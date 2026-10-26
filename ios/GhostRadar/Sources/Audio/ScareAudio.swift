import AVFoundation
import os

/// Preloaded stingers; one is picked at random per scare, never the same twice running.
/// `onFinished` fires when the playing track ends (cooldown ends at song end or 8 s).
final class ScareAudio: NSObject, AVAudioPlayerDelegate {
    private let log = Logger(subsystem: "GhostRadar", category: "scare")
    private var players: [AVAudioPlayer] = []
    private var current: AVAudioPlayer?
    private var lastIndex = -1
    private let onFinished: () -> Void

    init(resources: [String], onFinished: @escaping () -> Void) {
        self.onFinished = onFinished
        super.init()
        for name in resources {
            guard let url = Bundle.main.url(forResource: name, withExtension: "wav") ?? Bundle.main.url(forResource: name, withExtension: "mp3") ?? Bundle.main.url(forResource: name, withExtension: "m4a") else {
                log.error("stinger \(name, privacy: .public) missing"); continue
            }
            if let p = try? AVAudioPlayer(contentsOf: url) {
                p.volume = 1
                p.delegate = self
                p.prepareToPlay()
                players.append(p)
            }
        }
    }

    func play() {
        stop()
        guard !players.isEmpty else { log.warning("no scare track ready"); return }
        var choices = Array(players.indices).filter { $0 != lastIndex }
        if choices.isEmpty { choices = Array(players.indices) }
        let i = choices.randomElement()!
        lastIndex = i
        let p = players[i]
        current = p
        log.info("scare track #\(i + 1) of \(self.players.count)")
        p.currentTime = 0
        p.play()
    }

    func stop() {
        if let c = current, c.isPlaying { c.stop(); c.currentTime = 0 }
        current = nil
    }

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        if player === current { current = nil; onFinished() }
    }

    func release() { for p in players { p.stop() }; current = nil }
}
