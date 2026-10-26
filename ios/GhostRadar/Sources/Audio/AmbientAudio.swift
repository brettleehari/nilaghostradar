import AVFoundation
import os

/// Background loops for the radar phase. One is picked at random each time the hunt (re)starts,
/// never the same twice running. Pauses the instant a haunted lock happens so the stinger hits
/// silence. `setTempo` speeds the loop up (pitch unchanged); `setVolume` follows the same signal.
final class AmbientAudio {
    private let log = Logger(subsystem: "GhostRadar", category: "ambience")
    private var players: [AVAudioPlayer] = []
    private var current: AVAudioPlayer?
    private var lastIndex = -1
    private var tempo: Float = 1
    private var volume: Float

    init(resources: [String], volume: Float = 0.6) {
        self.volume = volume
        for name in resources {
            guard let url = Bundle.main.url(forResource: name, withExtension: "wav") ?? Bundle.main.url(forResource: name, withExtension: "mp3") ?? Bundle.main.url(forResource: name, withExtension: "m4a") else {
                log.error("ambience \(name, privacy: .public) missing"); continue
            }
            if let p = try? AVAudioPlayer(contentsOf: url) {
                p.numberOfLoops = -1
                p.enableRate = true
                p.volume = volume
                p.prepareToPlay()
                players.append(p)
            }
        }
    }

    func play() {
        if let c = current, c.isPlaying { return }
        guard !players.isEmpty else { return }
        var choices = Array(players.indices).filter { $0 != lastIndex }
        if choices.isEmpty { choices = Array(players.indices) }
        let i = choices.randomElement()!
        lastIndex = i
        let p = players[i]
        current = p
        p.currentTime = 0
        p.rate = tempo
        p.volume = volume
        p.play()
        log.info("ambience loop #\(i + 1) of \(self.players.count)")
    }

    func pause() {
        current?.pause()
        current = nil
    }

    /// 1.0 = as recorded; quantised to 0.05 so the stretcher is not poked every frame.
    func setTempo(_ speed: Float) {
        let q = min(max((speed / 0.05).rounded() * 0.05, 0.5), 2.5)
        if q == tempo { return }
        tempo = q
        current?.rate = q
    }

    func setVolume(_ v: Float) {
        let q = (min(max(v, 0), 1) * 20).rounded() / 20
        if q == volume { return }
        volume = q
        for p in players { p.volume = q }
    }

    func release() { for p in players { p.stop() }; current = nil }
}
