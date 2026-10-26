import AVFoundation

/// Says a short line in a low, slow voice with the phone's own speech synthesis. No network.
/// If speech is unavailable, `speak` calls `onDone` at once so the scare is never delayed.
final class VoiceLine: NSObject, AVSpeechSynthesizerDelegate {
    private let synth = AVSpeechSynthesizer()
    private var pendingDone: (() -> Void)?
    private let pitch: Float
    private let rate: Float

    init(pitch: Float = 0.5, rate: Float = AVSpeechUtteranceDefaultSpeechRate * 0.7) {
        self.pitch = pitch; self.rate = rate
        super.init()
        synth.delegate = self
    }

    func speak(_ text: String, onDone: @escaping () -> Void) {
        pendingDone = onDone
        let u = AVSpeechUtterance(string: text)
        u.voice = AVSpeechSynthesisVoice(language: "en-US")
        u.pitchMultiplier = pitch
        u.rate = rate
        synth.speak(u)
    }

    func stop() {
        pendingDone = nil
        if synth.isSpeaking { synth.stopSpeaking(at: .immediate) }
    }

    private func finish() {
        let cb = pendingDone
        pendingDone = nil
        DispatchQueue.main.async { cb?() }
    }

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) { finish() }
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) { finish() }
}
