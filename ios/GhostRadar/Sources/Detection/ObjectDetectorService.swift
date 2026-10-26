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
    private var inFlightStart: [Int: CFAbsoluteTime] = [:]
    private var lastLogMs: Int64 = 0
    private var imageWidth = 480
    private var imageHeight = 640
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

    /// Upright 480x640 sample buffer (the camera connection is rotated to portrait).
    func detect(sampleBuffer: CMSampleBuffer, timestampMs: Int) {
        guard let detector, let image = try? MPImage(sampleBuffer: sampleBuffer, orientation: .up) else { return }
        imageWidth = Int(image.width); imageHeight = Int(image.height)
        inFlightStart[timestampMs] = CFAbsoluteTimeGetCurrent()
        do { try detector.detectAsync(image: image, timestampInMilliseconds: timestampMs) } catch {
            inFlightStart[timestampMs] = nil
            log.error("detectAsync: \(error.localizedDescription)")
        }
    }

    func objectDetector(_ objectDetector: ObjectDetector, didFinishDetection result: ObjectDetectorResult?, timestampInMilliseconds: Int, error: Error?) {
        let start = inFlightStart.removeValue(forKey: timestampInMilliseconds)
        let ms = start.map { Int((CFAbsoluteTimeGetCurrent() - $0) * 1000) } ?? 0
        if let error { log.error("detection error: \(error.localizedDescription)"); return }
        let objects: [DetectedObject] = (result?.detections ?? []).compactMap { d in
            guard let c = d.categories.first, let name = c.categoryName else { return nil }
            let r = d.boundingBox
            return DetectedObject(label: name, score: c.score,
                                  box: Box(left: Float(r.minX), top: Float(r.minY), right: Float(r.maxX), bottom: Float(r.maxY)))
        }.sorted { $0.score > $1.score }
        let frame = DetectionFrame(objects: objects, inferenceTimeMs: ms, imageWidth: imageWidth, imageHeight: imageHeight,
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
