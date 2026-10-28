import Foundation
import CoreGraphics
import GhostCore
import os

/// Owns the gate, the state machine, the `HuntSequence` clock, the heat meter and the timers.
/// Feed it every DetectionFrame from the camera; render from its published state.
///
/// A lock starts unhaunted: the hunt screen plays "I can see you" and calls `onNearLineDone`; if the
/// toy is still in view then, the lock wakes up (haunted), the screen says "Bloody Mary", and the
/// scare fires `bangAfterLineMs` later. Nothing can lock in the first `noLockAfterOpenMs` of a hunt
/// or for `noLockAfterScareMs` after a scare.
@MainActor
final class GhostViewModel: ObservableObject {
    @Published private(set) var state: GhostState = .searching
    @Published private(set) var lastFrame: DetectionFrame?
    @Published private(set) var snapshot: CGImage?
    @Published private(set) var notice: Notice?
    @Published private(set) var heat: Float = 0

    let config = ScareConfig()
    let proximity = ProximityConfig()
    let sequence = HuntSequence()

    private let gate = DetectionGate()
    private let scanner = SoulScanner()
    private let heatMeter = SoulHeat()
    // Every lock starts unhaunted; onNearLineDone wakes it up.
    private let machine = GhostStateMachine(decideHaunted: { false })
    private let log = Logger(subsystem: "GhostRadar", category: "ghost")

    private var scareTimer: Task<Void, Never>?
    private var cooldownTimer: Task<Void, Never>?
    private var noticeTimer: Task<Void, Never>?
    private var noticeCount: Int64 = 0
    private var fakeTargetUntilMs: Int64 = 0
    private var voiceHoldUntilMs: Int64 = 0
    private var cooldownStartMs: Int64 = 0
    /// The gate is fed nothing until this uptime: step 1 after opening, step 4 after a scare.
    private var noLockUntilMs: Int64 = 0
    private var pausedAtMs: Int64?

    /// Cuts the doll out of the latest camera frame; injected by the hunt screen.
    var cutOut: ((_ x: Int, _ y: Int, _ w: Int, _ h: Int) -> CGImage?)?

    init() { startHunt(Self.now()) }

    static func now() -> Int64 { Int64(ProcessInfo.processInfo.systemUptime * 1000) }

    private func startHunt(_ now: Int64) {
        noLockUntilMs = now + sequence.noLockAfterOpenMs
        log.info("hunt started: no lock for \(self.sequence.noLockAfterOpenMs) ms")
    }

    func onHuntPaused() { pausedAtMs = Self.now() }
    /// Away for `sessionResetAfterMs` or longer restarts the hunt (step 1 again).
    func onHuntResumed() {
        let now = Self.now()
        let away = pausedAtMs.map { now - $0 } ?? 0
        pausedAtMs = nil
        guard away >= sequence.sessionResetAfterMs else { return }
        scanner.reset()
        startHunt(now)
        log.info("hunt restarted after a long absence")
    }
    /// "Bloody Mary" has finished (or could not play).
    func onVoiceLineDone() { voiceHoldUntilMs = 0 }

    /// "I can see you" has finished (or could not play). If the toy is still locked, it wakes up:
    /// the lock turns haunted and the scare follows.
    func onNearLineDone() {
        let before = state
        guard before.isFaintTrace else { return }
        let after = machine.wakeUp()
        state = after
        onTransition(before, after)
        log.info("still in view at the end of the line: haunted")
    }

    /// Debug: a teddy bear in the middle of the frame for `durationMs`, ignoring the no-lock windows.
    func injectFakeTarget(durationMs: Int64 = 10_000) {
        noLockUntilMs = 0
        fakeTargetUntilMs = Self.now() + durationMs
        log.info("debug: fake target injected for \(durationMs) ms")
    }

    func onFrame(_ raw: DetectionFrame) {
        let now = Self.now()
        let frame = now < fakeTargetUntilMs ? raw.withFakeBear() : raw
        lastFrame = frame
        // Steps 1 and 4: the radar still warms, but nothing can lock.
        let canLock = now >= noLockUntilMs
        let out = gate.onFrame(canLock ? frame.objects : [], nowMs: frame.timestampMs)
        let target = out.target.map { Target($0, imageWidth: frame.imageWidth, imageHeight: frame.imageHeight) }
        let before = state
        let after = machine.onGate(out, target: target, holdScare: now < voiceHoldUntilMs)
        state = after
        onTransition(before, after)

        let measured = heatMeter.onFrame(frame.objects, imageHeight: frame.imageHeight)
        switch after {
        case .locked(_, true), .scaring: heat = 1
        case .locked(_, false): heat = measured
        case .cooldown: heat = min(max(1 - Float(now - cooldownStartMs) / Float(config.cooldownMaxMs), 0), 1)
        case .searching: heat = measured
        }
        if after.isSearching, let label = scanner.scan(frame.objects, nowMs: frame.timestampMs) {
            noticeCount += 1; show(.noSoul(label: label, id: noticeCount))
        }
    }

    func onAudioFinished() {
        log.info("scare track finished")
        if state.isCooldown { finishCooldown() }
    }

    private func show(_ n: Notice) {
        log.info("notice: \(String(describing: n), privacy: .public)")
        notice = n
        noticeTimer?.cancel()
        if n.isFaintTrace { return }
        let ms = config.noticeMs
        noticeTimer = Task { [weak self] in
            try? await Task.sleep(nanoseconds: UInt64(ms) * 1_000_000)
            guard let self, !Task.isCancelled, self.notice?.id == n.id else { return }
            self.notice = nil
        }
    }

    private func onTransition(_ before: GhostState, _ after: GhostState) {
        if before.isLocked && !after.isLocked, notice?.isFaintTrace == true { notice = nil }
        let newLock = !before.isLocked && after.isLocked
        let wokeUp = before.isFaintTrace && after.isHauntedLock
        if newLock { noticeCount += 1; show(.faintTrace(id: noticeCount)) }
        if wokeUp {
            notice = nil
            voiceHoldUntilMs = config.voiceLine.isEmpty ? 0 : Self.now() + sequence.bangAfterLineMs
        }
        if !before.isScaring, case .scaring(let t) = after {
            if let r = cutoutRect(box: t.box, margin: config.cutoutMarginFraction, imageWidth: t.imageWidth, imageHeight: t.imageHeight) {
                snapshot = cutOut?(r.x, r.y, r.w, r.h)
            }
            scareTimer?.cancel()
            let ms = config.scareDurationMs
            scareTimer = Task { [weak self] in
                try? await Task.sleep(nanoseconds: UInt64(ms) * 1_000_000)
                guard let self, !Task.isCancelled else { return }
                let b = self.state
                self.state = self.machine.onScareFinished()
                self.onTransition(b, self.state)
            }
        }
        if !before.isCooldown && after.isCooldown {
            cooldownStartMs = Self.now()
            heat = 1
            scareTimer?.cancel()
            cooldownTimer?.cancel()
            let ms = config.cooldownMaxMs
            cooldownTimer = Task { [weak self] in
                try? await Task.sleep(nanoseconds: UInt64(ms) * 1_000_000)
                guard let self, !Task.isCancelled else { return }
                self.finishCooldown()
            }
        }
        if before.isCooldown && after.isSearching {
            // Step 4: cool off.
            noLockUntilMs = Self.now() + sequence.noLockAfterScareMs
            log.info("cooling off: no lock for \(self.sequence.noLockAfterScareMs) ms")
        }
        if before.kind != after.kind { log.info("state: \(before.kind, privacy: .public) -> \(after.kind, privacy: .public)") }
    }

    private func finishCooldown() {
        cooldownTimer?.cancel()
        let before = state
        state = machine.onCooldownFinished()
        onTransition(before, state)
        if state.isSearching { snapshot = nil }
    }
}

extension GhostState {
    var kind: String {
        switch self {
        case .searching: return "Searching"
        case .locked(_, let h): return h ? "Locked" : "Locked(found)"
        case .scaring: return "Scaring"
        case .cooldown: return "Cooldown"
        }
    }
}

private extension DetectionFrame {
    func withFakeBear() -> DetectionFrame {
        let w = Float(imageWidth) * 0.28, h = Float(imageHeight) * 0.26
        let cx = Float(imageWidth) / 2, cy = Float(imageHeight) * 0.58
        let fake = DetectedObject(label: "teddy bear", score: 0.99, box: Box(left: cx - w / 2, top: cy - h / 2, right: cx + w / 2, bottom: cy + h / 2))
        return DetectionFrame(objects: [fake] + objects, inferenceTimeMs: inferenceTimeMs, imageWidth: imageWidth, imageHeight: imageHeight, timestampMs: timestampMs)
    }
}
