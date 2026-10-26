import Foundation
import CoreGraphics
import GhostCore
import os

/// Owns the gate, the state machine, the haunt policy, the heat meter and the timers.
/// Feed it every DetectionFrame from the camera; render from its published state.
@MainActor
final class GhostViewModel: ObservableObject {
    @Published private(set) var state: GhostState = .searching
    @Published private(set) var lastFrame: DetectionFrame?
    @Published private(set) var snapshot: CGImage?
    @Published private(set) var notice: Notice?
    @Published private(set) var heat: Float = 0

    let config = ScareConfig()
    let proximity = ProximityConfig()
    let policyConfig = HauntPolicy.Config()

    private let gate = DetectionGate()
    private let policy: HauntPolicy
    private let scanner = SoulScanner()
    private let heatMeter = SoulHeat()
    private lazy var machine = GhostStateMachine(decideHaunted: { [unowned self] in policy.decide(nowMs: Self.now()) })
    private let log = Logger(subsystem: "GhostRadar", category: "ghost")

    private var scareTimer: Task<Void, Never>?
    private var cooldownTimer: Task<Void, Never>?
    private var noticeTimer: Task<Void, Never>?
    private var noticeCount: Int64 = 0
    private var fakeTargetUntilMs: Int64 = 0
    private var voiceHoldUntilMs: Int64 = 0
    private var cooldownStartMs: Int64 = 0
    private var lastDecisionMs: Int64 = 0

    /// Cuts the doll out of the latest camera frame; injected by the hunt screen.
    var cutOut: ((_ x: Int, _ y: Int, _ w: Int, _ h: Int) -> CGImage?)?

    init() {
        policy = HauntPolicy(config: policyConfig)
        policy.startSession(nowMs: Self.now())
    }

    static func now() -> Int64 { Int64(ProcessInfo.processInfo.systemUptime * 1000) }

    func onHuntPaused() { policy.onPaused(nowMs: Self.now()) }
    func onHuntResumed() {
        if policy.onResumed(nowMs: Self.now()) { scanner.reset(); log.info("session restarted after a long absence") }
    }
    func onVoiceLineDone() { voiceHoldUntilMs = 0 }

    /// Debug: a teddy bear in the middle of the frame for `durationMs`, forced haunted.
    func injectFakeTarget(durationMs: Int64 = 3_000) {
        policy.forceNextHaunt()
        fakeTargetUntilMs = Self.now() + durationMs
        log.info("debug: fake target injected for \(durationMs) ms")
    }

    func onFrame(_ raw: DetectionFrame) {
        let now = Self.now()
        let frame = now < fakeTargetUntilMs ? raw.withFakeBear() : raw
        lastFrame = frame
        let out = gate.onFrame(frame.objects, nowMs: frame.timestampMs)
        let target = out.target.map { Target($0, imageWidth: frame.imageWidth, imageHeight: frame.imageHeight) }
        let before = state
        var after = machine.onGate(out, target: target, holdScare: now < voiceHoldUntilMs)
        if !before.isLocked && after.isLocked { lastDecisionMs = now }
        if after.isFaintTrace && now - lastDecisionMs >= policyConfig.rerollMs {
            lastDecisionMs = now
            if policy.decide(nowMs: now) { after = machine.wakeUp(); log.info("faint trace re-rolled: haunted") }
            else { log.info("faint trace re-rolled: still faint") }
        }
        state = after
        onTransition(before, after)

        let measured = heatMeter.onFrame(frame.objects, imageHeight: frame.imageHeight)
        switch after {
        case .locked(_, true), .scaring: heat = 1
        case .locked(_, false): heat = min(measured, proximity.traceHeatCap)
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
        if newLock || wokeUp {
            let haunted = after.isHauntedLock
            voiceHoldUntilMs = (!haunted || config.voiceLine.isEmpty) ? 0 : Self.now() + config.voiceMaxHoldMs
            if !haunted { noticeCount += 1; show(.faintTrace(id: noticeCount)) }
            else if notice?.isFaintTrace == true { notice = nil }
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
        case .locked(_, let h): return h ? "Locked" : "Locked(trace)"
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
