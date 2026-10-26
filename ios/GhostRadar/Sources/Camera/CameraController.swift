import AVFoundation
import CoreImage
import UIKit
import os

/// Back camera at 640x480, portrait, BGRA frames to the detector, latest frame kept for the doll
/// cutout. Mirrors the Android CameraView: analysis only, keep-only-latest, never records.
final class CameraController: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    let session = AVCaptureSession()
    private let queue = DispatchQueue(label: "ghostradar.camera", qos: .userInitiated)
    private let output = AVCaptureVideoDataOutput()
    private let log = Logger(subsystem: "GhostRadar", category: "camera")
    private let context = CIContext()
    private var latestPixelBuffer: CVPixelBuffer?
    private let latestLock = NSLock()
    private var configured = false

    /// Called on the camera queue with an upright sample buffer and a monotonic timestamp in ms.
    var onSampleBuffer: ((CMSampleBuffer, Int) -> Void)?

    func start() {
        queue.async { [self] in
            if !configured { configure() }
            if !session.isRunning { session.startRunning() }
        }
    }

    func stop() {
        queue.async { [self] in if session.isRunning { session.stopRunning() } }
    }

    private func configure() {
        configured = true
        session.beginConfiguration()
        session.sessionPreset = .vga640x480
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: device), session.canAddInput(input) else {
            log.error("no back camera"); session.commitConfiguration(); return
        }
        session.addInput(input)
        output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        output.alwaysDiscardsLateVideoFrames = true
        output.setSampleBufferDelegate(self, queue: queue)
        guard session.canAddOutput(output) else { log.error("cannot add output"); session.commitConfiguration(); return }
        session.addOutput(output)
        if let c = output.connection(with: .video) {
            if c.isVideoRotationAngleSupported(90) { c.videoRotationAngle = 90; log.info("data output rotated to portrait") }
            else { log.info("data output cannot rotate; detector will rotate boxes") }
            c.isVideoMirrored = false
        }
        session.commitConfiguration()
    }

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        if let pb = CMSampleBufferGetImageBuffer(sampleBuffer) {
            latestLock.lock(); latestPixelBuffer = pb; latestLock.unlock()
        }
        let ms = Int(CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sampleBuffer)) * 1000)
        onSampleBuffer?(sampleBuffer, ms)
    }

    /// Crops the latest frame, in upright image pixels. A raw landscape buffer is turned upright
    /// first (90° clockwise), matching what the detector did with its boxes.
    func cutOut(x: Int, y: Int, w: Int, h: Int) -> CGImage? {
        latestLock.lock(); let pb = latestPixelBuffer; latestLock.unlock()
        guard let pb else { return nil }
        var ci = CIImage(cvPixelBuffer: pb)
        if CVPixelBufferGetWidth(pb) > CVPixelBufferGetHeight(pb) { ci = ci.oriented(.right) }
        let extent = ci.extent
        // CIImage origin is bottom-left; flip y.
        let crop = CGRect(x: extent.minX + CGFloat(x), y: extent.minY + extent.height - CGFloat(y + h), width: CGFloat(w), height: CGFloat(h))
        return context.createCGImage(ci, from: crop)
    }
}
