import SwiftUI
import GhostCore

/// Camera analysing underneath, the scare overlay on the preview, the radar on top. The radar is
/// opaque while Searching and Locked and drops the instant Scaring starts; after the lunge it is
/// the blackout, then the radar back, red and cooling, then one aftershock.
struct HuntView: View {
    @StateObject private var vm = GhostViewModel()
    @Environment(\.scenePhase) private var scenePhase
    @State private var camera = CameraController()
    @State private var detector = ObjectDetectorService()
    @State private var haptics = ScareHaptics()
    @State private var audio: ScareAudio?
    @State private var ambience: AmbientAudio?
    @State private var voice = VoiceLine()
    @State private var blackout = false
    @State private var aftershock = false

    var body: some View {
        GeometryReader { geo in
            let fit = fittedBoxSize(container: FitSize(width: Float(geo.size.width), height: Float(geo.size.height)), box: FitSize(width: 480, height: 640))
            let previewSize = CGSize(width: CGFloat(fit.width), height: CGFloat(fit.height))
            let showRadar = vm.state.isSearching || vm.state.isLocked || (vm.state.isCooldown && !blackout)
            ZStack {
                Color.black
                ZStack {
                    CameraPreview(session: camera.session)
                    ScareOverlayView(state: vm.state, snapshot: vm.snapshot, config: vm.config)
                }
                .frame(width: previewSize.width, height: previewSize.height)
                .clipped()
                RadarView(state: vm.state, notice: vm.notice, heat: vm.heat, proximity: vm.proximity)
                    .opacity(showRadar ? 1 : 0)
                    .animation(.easeInOut(duration: showRadar ? 0.6 : 0.15), value: showRadar)
                if blackout { SignalLostView() }
                if aftershock { AftershockFlashView() }
                #if DEBUG
                DebugPanel(frame: vm.lastFrame, state: vm.state)
                Color.clear.contentShape(Rectangle())
                    .onLongPressGesture(minimumDuration: 0.6) { vm.injectFakeTarget() }
                #endif
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .ignoresSafeArea()
        .onAppear(perform: start)
        .onDisappear(perform: stopAll)
        .onChange(of: scenePhase) { _, phase in
            switch phase {
            case .active: vm.onHuntResumed(); camera.start()
            case .background, .inactive: vm.onHuntPaused(); camera.stop()
            @unknown default: break
            }
        }
        .onChange(of: vm.state.kind) { _, _ in onStateChanged() }
        .onChange(of: vm.heat) { _, h in ambience?.setTempo(vm.proximity.tempo(h)); ambience?.setVolume(vm.proximity.ambienceVolume(h)) }
        .task { await tickLoop() }
    }

    private func start() {
        AudioSession.activate()
        audio = ScareAudio(resources: ["scare_track", "scare_track_2", "scare_track_3"]) { Task { @MainActor in vm.onAudioFinished() } }
        ambience = AmbientAudio(resources: ["radar_ambience", "radar_ambience_2", "radar_ambience_3"])
        detector.onFrame = { frame in vm.onFrame(frame) }
        camera.onSampleBuffer = { buffer, ms in detector.detect(sampleBuffer: buffer, timestampMs: ms) }
        vm.cutOut = { x, y, w, h in camera.cutOut(x: x, y: y, w: w, h: h) }
        camera.start()
        ambience?.play()
        #if DEBUG
        // Simulator / no-camera demo: GHOSTRADAR_DEMO=1 feeds empty frames and forces a haunted
        // bear 4 s in and every 25 s after, so the whole sequence can be seen with no camera.
        if ProcessInfo.processInfo.environment["GHOSTRADAR_DEMO"] != nil {
            Task { @MainActor in
                var n = 0
                while !Task.isCancelled {
                    if n % 125 == 20 { vm.injectFakeTarget() }
                    vm.onFrame(DetectionFrame(objects: [], inferenceTimeMs: 0, imageWidth: 480, imageHeight: 640, timestampMs: GhostViewModel.now()))
                    n += 1
                    try? await Task.sleep(nanoseconds: 200 * 1_000_000)
                }
            }
        }
        #endif
    }

    private func stopAll() {
        camera.stop(); audio?.release(); ambience?.release(); voice.stop(); haptics.stop()
    }

    /// Side effects on state entry, like the Android LaunchedEffect.
    private func onStateChanged() {
        switch vm.state {
        case .searching:
            haptics.stop(); audio?.stop(); voice.stop(); ambience?.play()
            blackout = false; aftershock = false
        case .locked(_, let haunted):
            if haunted && !vm.config.voiceLine.isEmpty {
                ambience?.pause()
                if vm.proximity.joltAtLock { haptics.jolt() }
                voice.speak(vm.config.voiceLine) { vm.onVoiceLineDone() }
            } else {
                vm.onVoiceLineDone()
            }
        case .scaring:
            ambience?.pause(); haptics.scare(); audio?.play()
        case .cooldown:
            haptics.stop(); voice.stop()
            Task { await aftermath() }
        }
    }

    private func aftermath() async {
        let cfg = vm.config
        blackout = true
        try? await Task.sleep(nanoseconds: UInt64(cfg.blackoutMs) * 1_000_000)
        blackout = false
        guard vm.state.isCooldown, cfg.aftershockAtMs > cfg.blackoutMs else { return }
        try? await Task.sleep(nanoseconds: UInt64(cfg.aftershockAtMs - cfg.blackoutMs) * 1_000_000)
        guard vm.state.isCooldown else { return }
        aftershock = true
        haptics.aftershock(ms: cfg.aftershockRumbleMs)
        try? await Task.sleep(nanoseconds: UInt64(cfg.aftershockFlashMs) * 1_000_000)
        aftershock = false
    }

    /// Warmer/colder ticks, a heartbeat once hot; only while hunting. The scare owns the motor otherwise.
    private func tickLoop() async {
        let prox = vm.proximity
        while !Task.isCancelled {
            let h = vm.heat, s = vm.state
            let hunting = s.isSearching || s.isFaintTrace || (s.isCooldown && !blackout)
            if hunting && prox.heartbeat(h) {
                haptics.heartbeat(prox.tickStrength(h))
                try? await Task.sleep(nanoseconds: UInt64(prox.heartbeatPeriodMs(h)) * 1_000_000)
            } else if hunting && h >= prox.tickFromHeat {
                haptics.tick(prox.tickStrength(h), durationMs: prox.tickDurationMs)
                try? await Task.sleep(nanoseconds: UInt64(prox.tickPeriodMs(h)) * 1_000_000)
            } else {
                try? await Task.sleep(nanoseconds: 120 * 1_000_000)
            }
        }
    }
}

#if DEBUG
private struct DebugPanel: View {
    let frame: DetectionFrame?; let state: GhostState
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(state.kind).foregroundStyle(state.isSearching ? .white : Theme.green)
            if let f = frame {
                Text("\(f.inferenceTimeMs) ms  \(f.imageWidth)x\(f.imageHeight)")
                ForEach(Array(f.objects.prefix(3).enumerated()), id: \.offset) { _, o in
                    Text(String(format: "%-14@ %.2f", o.label, o.score))
                }
            } else {
                Text("Waiting for first frame…")
            }
        }
        .font(.system(size: 11, design: .monospaced)).foregroundStyle(.white)
        .padding(8).background(Color.black.opacity(0.6))
        .padding(.top, 60).padding(.leading, 12)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .allowsHitTesting(false)
    }
}
#endif
