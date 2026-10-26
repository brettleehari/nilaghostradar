import SwiftUI
import AVFoundation

/// Root of the app: rationale → permission → hunt. ⓘ top-right opens the open-source notices.
struct RootView: View {
    @State private var granted = AVCaptureDevice.authorizationStatus(for: .video) == .authorized
    @State private var deniedOnce = AVCaptureDevice.authorizationStatus(for: .video) == .denied
    @State private var showLicenses = false

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            if granted {
                HuntView()
            } else {
                CameraRationaleView(deniedOnce: deniedOnce) {
                    if AVCaptureDevice.authorizationStatus(for: .video) == .denied {
                        if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                        return
                    }
                    AVCaptureDevice.requestAccess(for: .video) { ok in
                        DispatchQueue.main.async { granted = ok; if !ok { deniedOnce = true } }
                    }
                }
            }
            Button { showLicenses = true } label: {
                Text("ⓘ").font(.system(size: 22)).foregroundStyle(Theme.green.opacity(0.6)).padding(12)
            }
            .accessibilityLabel("Open source licences")
        }
        .sheet(isPresented: $showLicenses) { LicensesView() }
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
    }
}

/// Plain-language explanation shown before the camera permission request. PLACEHOLDER copy for Nila.
struct CameraRationaleView: View {
    let deniedOnce: Bool
    let onContinue: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Ghost Radar needs your camera")
                .font(.title2.weight(.semibold)).foregroundStyle(Theme.green).multilineTextAlignment(.center)
            Text("The camera looks for ghosts hiding in toys. Everything happens on this phone. Nothing is recorded, saved, or sent anywhere.")
                .font(.body).foregroundStyle(Theme.bone).multilineTextAlignment(.center)
            if deniedOnce {
                Text("Without the camera there is nothing to hunt. You can allow it in Settings › Nila › Camera.")
                    .font(.callout).foregroundStyle(Theme.bone).multilineTextAlignment(.center)
            }
            Button(action: onContinue) {
                Text(deniedOnce ? "Open Settings" : "Start hunting")
                    .font(.headline).foregroundStyle(.black)
                    .padding(.horizontal, 28).padding(.vertical, 12)
                    .background(Theme.green, in: Capsule())
            }
            .padding(.top, 16)
        }
        .padding(.horizontal, 32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black)
    }
}

/// Open-source notices, read from third_party_notices.txt in the bundle.
struct LicensesView: View {
    @Environment(\.dismiss) private var dismiss
    private var text: String {
        guard let url = Bundle.main.url(forResource: "third_party_notices", withExtension: "txt"),
              let s = try? String(contentsOf: url, encoding: .utf8) else { return "Notices missing from bundle." }
        return s
    }
    var body: some View {
        NavigationStack {
            ScrollView {
                Text(text).font(.system(size: 11, design: .monospaced)).foregroundStyle(Theme.bone)
                    .frame(maxWidth: .infinity, alignment: .leading).padding(16)
            }
            .background(Color.black)
            .navigationTitle("Open source licences")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Back") { dismiss() } } }
        }
        .preferredColorScheme(.dark)
    }
}
