import Foundation

/// Axis-aligned box in image pixels (upright image, origin top-left).
public struct Box: Equatable {
    public var left: Float, top: Float, right: Float, bottom: Float
    public init(left: Float, top: Float, right: Float, bottom: Float) {
        self.left = left; self.top = top; self.right = right; self.bottom = bottom
    }
    public var width: Float { right - left }
    public var height: Float { bottom - top }
    public var centerX: Float { (left + right) / 2 }
    public var centerY: Float { (top + bottom) / 2 }
}

/// One detection from the model, decoupled from MediaPipe types.
public struct DetectedObject: Equatable {
    public let label: String
    public let score: Float
    public let box: Box
    public init(label: String, score: Float, box: Box) { self.label = label; self.score = score; self.box = box }
}

/// Everything the detector reports for one analysed camera frame.
public struct DetectionFrame {
    /// Sorted by score, highest first.
    public let objects: [DetectedObject]
    public let inferenceTimeMs: Int
    /// Size of the upright image the boxes refer to.
    public let imageWidth: Int
    public let imageHeight: Int
    public let timestampMs: Int64
    public init(objects: [DetectedObject], inferenceTimeMs: Int, imageWidth: Int, imageHeight: Int, timestampMs: Int64) {
        self.objects = objects; self.inferenceTimeMs = inferenceTimeMs
        self.imageWidth = imageWidth; self.imageHeight = imageHeight; self.timestampMs = timestampMs
    }
}

/// COCO labels that count as "a doll or toy". There is no "doll" class in COCO; "teddy bear" is
/// the v1 trigger, confirmed on Nila's doll in the day-one test (see NOTES.md).
public let triggerLabels: Set<String> = ["teddy bear"]
