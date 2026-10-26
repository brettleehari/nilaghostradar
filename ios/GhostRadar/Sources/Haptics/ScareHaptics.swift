import CoreHaptics
import UIKit
import os

/// Core Haptics patterns mirroring the Android waveforms: hunting ticks, a heartbeat, a jolt at
/// the lock, the scare rumble and the aftershock thump. Falls back to UIKit impact feedback on
/// phones without a haptic engine.
final class ScareHaptics {
    private let log = Logger(subsystem: "GhostRadar", category: "haptics")
    private var engine: CHHapticEngine?
    private let impact = UIImpactFeedbackGenerator(style: .heavy)
    private var scarePlayer: CHHapticPatternPlayer?

    init() {
        guard CHHapticEngine.capabilitiesForHardware().supportsHaptics else { return }
        do {
            let e = try CHHapticEngine()
            e.playsHapticsOnly = true
            e.resetHandler = { [weak self] in try? self?.engine?.start() }
            e.stoppedHandler = { _ in }
            try e.start()
            engine = e
        } catch { log.error("haptic engine: \(error.localizedDescription)") }
        impact.prepare()
    }

    /// One short Find My-style tap; `strength` 0..1.
    func tick(_ strength: Float, durationMs: Int64 = 18) {
        play([transient(at: 0, intensity: strength, sharpness: 0.4)])
    }

    /// Lub (60 ms) – gap – dub (45 ms).
    func heartbeat(_ strength: Float) {
        play([continuous(at: 0, ms: 60, intensity: 0.9 * strength, sharpness: 0.2),
              continuous(at: 0.17, ms: 45, intensity: 0.6 * strength, sharpness: 0.2)])
    }

    /// One hard hit the instant the radar goes red.
    func jolt() {
        play([continuous(at: 0, ms: 130, intensity: 1, sharpness: 0.8)])
    }

    /// The scare pattern (~2.1 s): hard jolt, stutter, then a long growing rumble.
    func scare() {
        let timings: [Double] = [0, 180, 60, 90, 50, 90, 50, 400, 80, 250, 60, 700]
        let amps: [Float] = [0, 255, 0, 200, 0, 200, 0, 160, 0, 220, 0, 255]
        var t = 0.0
        var events: [CHHapticEvent] = []
        for (i, ms) in timings.enumerated() {
            if amps[i] > 0 { events.append(continuous(at: t, ms: ms, intensity: amps[i] / 255, sharpness: i >= 7 ? 0.3 : 0.9)) }
            t += ms / 1000
        }
        play(events)
    }

    func aftershock(ms: Int64) { play([continuous(at: 0, ms: Double(ms), intensity: 1, sharpness: 0.7)]) }

    func stop() { try? scarePlayer?.stop(atTime: 0); scarePlayer = nil }

    private func transient(at t: Double, intensity: Float, sharpness: Float) -> CHHapticEvent {
        CHHapticEvent(eventType: .hapticTransient, parameters: [
            CHHapticEventParameter(parameterID: .hapticIntensity, value: min(max(intensity, 0.05), 1)),
            CHHapticEventParameter(parameterID: .hapticSharpness, value: sharpness)], relativeTime: t)
    }

    private func continuous(at t: Double, ms: Double, intensity: Float, sharpness: Float) -> CHHapticEvent {
        CHHapticEvent(eventType: .hapticContinuous, parameters: [
            CHHapticEventParameter(parameterID: .hapticIntensity, value: min(max(intensity, 0.05), 1)),
            CHHapticEventParameter(parameterID: .hapticSharpness, value: sharpness)], relativeTime: t, duration: ms / 1000)
    }

    private func play(_ events: [CHHapticEvent]) {
        guard let engine else { impact.impactOccurred(); return }
        do {
            let pattern = try CHHapticPattern(events: events, parameters: [])
            let player = try engine.makePlayer(with: pattern)
            try engine.start()
            try player.start(atTime: CHHapticTimeImmediate)
            if events.count > 2 { scarePlayer = player }
        } catch { log.error("haptic play: \(error.localizedDescription)") }
    }
}
