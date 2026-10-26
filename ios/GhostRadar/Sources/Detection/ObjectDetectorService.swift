import Foundation
import AVFoundation
import MediaPipeTasksVision
import GhostCore
import os

/// MediaPipe EfficientDet-Lite0 int8 in live-stream mode, the same model file as the Android
/// build. Frames go in on the camera queue; results come back on MediaPipe's thread and are
/// handed to `onFrame` on the main queue. Threshold 0.3, three results, CPU delegate.
final class ObjectDetectorService: NSObject, ObjectDetectorLiveStreamDelegate {
    private let log = Logger(subsystem: "GhostRadar", category: "detect")
    private var detector: ObjectDetector?
    private struct InFlight { let start: CFAbsoluteTime; let rawWidth: Int; let rawHeight: Int; let landscape: Bool }
    private var inFlight: [Int: InFlight] = [:]
    private var lastLogMs: Int64 = 0
    private var loggedGeometry = false
    var onFrame: ((DetectionFrame) -> Void)?

    override init() {
        super.init()
        guard let path = Bundle.main.path(forResource: "efficientdet_lite0_int8", ofType: "tflite") else {
            log.error("model missing from bundle"); return
        }
        let options = ObjectDetectorOptions()
        options.baseOptions.modelAssetPath = path
        options.runningMode = .liveStream
        options.scoreThreshold = 0.3
        options.maxResults = 3
        options.objectDetectorLiveStreamDelegate = self
        do { detector = try ObjectDetector(options: options) } catch { log.error("detector init failed: \(error.localizedDescription)") }
    }

    /// Portrait (480x640) buffers go in as-is. If the connection could not rotate and the buffer
    /// is the raw landscape sensor frame (640x480), MediaPipe is told it is `.right` (90° clockwise
    /// to upright); it infers on the rotated image but reports boxes in the raw frame, so they are
    /// rotated back into upright coordinates in the callback (same rule as the MediaPipe iOS sample).
    func detect(sampleBuffer: CMSampleBuffer, timestampMs: Int) {
        guard let detector, let pb = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        let w = CVPixelBufferGetWidth(pb), h = CVPixelBufferGetHeight(pb)
        let landscape = w > h
        if !loggedGeometry { loggedGeometry = true; log.info("camera buffer \(w)x\(h) \(landscape ? "landscape, rotating boxes" : "portrait", privacy: .public)") }
        guard let image = try? MPImage(sampleBuffer: sampleBuffer, orientation: landscape ? .right : .up) else { return }
        inFlight[timestampMs] = InFlight(start: CFAbsoluteTimeGetCurrent(), rawWidth: w, rawHeight: h, landscape: landscape)
        do { try detector.detectAsync(image: image, timestampInMilliseconds: timestampMs) } catch {
            inFlight[timestampMs] = nil
            log.error("detectAsync: \(error.localizedDescription)")
        }
    }

    func objectDetector(_ objectDetector: ObjectDetector, didFinishDetection result: ObjectDetectorResult?, timestampInMilliseconds: Int, error: Error?) {
        guard let f = inFlight.removeValue(forKey: timestampInMilliseconds) else { return }
        let ms = Int((CFAbsoluteTimeGetCurrent() - f.start) * 1000)
        if let error { log.error("detection error: \(error.localizedDescription)"); return }
        let objects: [DetectedObject] = (result?.detections ?? []).compactMap { d in
            guard let c = d.categories.first, let name = c.categoryName else { return nil }
            let r = d.boundingBox
            var box = Box(left: Float(r.minX), top: Float(r.minY), right: Float(r.maxX), bottom: Float(r.maxY))
            if f.landscape { box = box.rotated(degrees: 90, srcWidth: f.rawWidth, srcHeight: f.rawHeight) }
            return DetectedObject(label: name, score: c.score, box: box)
        }.sorted { $0.score > $1.score }
        let (iw, ih) = f.landscape ? (f.rawHeight, f.rawWidth) : (f.rawWidth, f.rawHeight)
        let frame = DetectionFrame(objects: objects, inferenceTimeMs: ms, imageWidth: iw, imageHeight: ih,
                                   timestampMs: Int64(timestampInMilliseconds))
        logThrottled(frame)
        DispatchQueue.main.async { [onFrame] in onFrame?(frame) }
    }

    /// Top-3 labels and scores, at most every 250 ms, like the Android DetectionLogger.
    private func logThrottled(_ f: DetectionFrame) {
        if f.timestampMs - lastLogMs < 250 { return }
        lastLogMs = f.timestampMs
        let labels = f.objects.isEmpty ? "(none)" : f.objects.prefix(3).map {
            String(format: "%@ %.2f [%d,%d %dx%d]", $0.label, $0.score, Int($0.box.left), Int($0.box.top), Int($0.box.width), Int($0.box.height))
        }.joined(separator: " | ")
        log.info("labels: \(labels, privacy: .public)  (\(f.inferenceTimeMs) ms, \(f.imageWidth)x\(f.imageHeight))")
    }
}
