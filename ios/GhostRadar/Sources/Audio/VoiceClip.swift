import AVFoundation
import os

/// One short recorded line (Nila's own voice), preloaded so it starts the instant it is asked for.
/// `play` calls `onDone` when the clip ends or could not play, so a scare that waits on the line
/// is never delayed by a voice that is not coming. A `stop` is not a finish: its callback is dropped.
final class VoiceClip: NSObject, AVAudioPlayerDelegate {
    private let log = Logger(subsystem: "GhostRadar", category: "voice")
    private var player: AVAudioPlayer?
    private var pendingDone: (() -> Void)?
    private let says: String

    init(resource: String, says: String) {
        self.says = says
        super.init()
        guard let url = Bundle.main.url(forResource: resource, withExtension: "wav") ?? Bundle.main.url(forResource: resource, withExtension: "m4a") else {
            log.error("voice clip \(resource, privacy: .public) missing"); return
        }
        if let p = try? AVAudioPlayer(contentsOf: url) {
            p.volume = 1
            p.delegate = self
            p.prepareToPlay()
            player = p
        }
    }

    var isPlaying: Bool { player?.isPlaying ?? false }

    func play(onDone: @escaping () -> Void = {}) {
        guard let p = player else { onDone(); return }
        pendingDone?()
        pendingDone = onDone
        log.info("voice: \"\(self.says, privacy: .public)\"")
        p.currentTime = 0
        p.play()
    }

    /// Runs `cb` when the clip that is playing ends; at once if nothing is playing.
    func onFinished(_ cb: @escaping () -> Void) {
        if isPlaying { pendingDone = cb } else { cb() }
    }

    func stop() {
        pendingDone = nil
        if let p = player, p.isPlaying { p.stop(); p.currentTime = 0 }
    }

    private func finish() {
        let cb = pendingDone
        pendingDone = nil
        DispatchQueue.main.async { cb?() }
    }

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) { finish() }
    func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error: Error?) { finish() }
}
