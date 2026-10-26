import AVFoundation

/// One shared playback session: the prank must be heard with the mute switch on, like a game.
enum AudioSession {
    static func activate() {
        let s = AVAudioSession.sharedInstance()
        try? s.setCategory(.playback, mode: .default, options: [])
        try? s.setActive(true)
    }
}
