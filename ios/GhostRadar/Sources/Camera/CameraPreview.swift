import SwiftUI
import AVFoundation

/// The live camera, letterboxed to the analysed frame's 3:4 so boxes map 1:1 onto it.
struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }

    func makeUIView(context: Context) -> PreviewView {
        let v = PreviewView()
        v.previewLayer.session = session
        v.previewLayer.videoGravity = .resizeAspectFill
        v.backgroundColor = .black
        if let c = v.previewLayer.connection, c.isVideoRotationAngleSupported(90) { c.videoRotationAngle = 90 }
        return v
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {
        if let c = uiView.previewLayer.connection, c.isVideoRotationAngleSupported(90), c.videoRotationAngle != 90 { c.videoRotationAngle = 90 }
    }
}
