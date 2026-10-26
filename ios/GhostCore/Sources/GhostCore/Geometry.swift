import Foundation

/// Width/height pair without UIKit.
public struct FitSize: Equatable {
    public let width: Float, height: Float
    public init(width: Float, height: Float) { self.width = width; self.height = height }
}

/// Scales `box` to fit inside `container` preserving aspect, so the whole analysed frame is visible.
public func fittedBoxSize(container: FitSize, box: FitSize) -> FitSize {
    let boxAspect = box.width / box.height
    let containerAspect = container.width / container.height
    return boxAspect > containerAspect
        ? FitSize(width: container.width, height: container.width / boxAspect)
        : FitSize(width: container.height * boxAspect, height: container.height)
}

/// How an upright analysis image is shown in a preview of the same aspect ratio.
public struct FrameGeometry {
    public let imageWidth: Int, imageHeight: Int
    public let previewWidth: Float, previewHeight: Float
    public let mirrored: Bool
    public init(imageWidth: Int, imageHeight: Int, previewWidth: Float, previewHeight: Float, mirrored: Bool = false) {
        self.imageWidth = imageWidth; self.imageHeight = imageHeight
        self.previewWidth = previewWidth; self.previewHeight = previewHeight; self.mirrored = mirrored
    }
}

public extension Box {
    /// Maps a box in image pixels to preview coordinates.
    func toPreview(_ g: FrameGeometry) -> Box {
        let sx = g.previewWidth / Float(g.imageWidth)
        let sy = g.previewHeight / Float(g.imageHeight)
        let l = left * sx, r = right * sx
        return g.mirrored
            ? Box(left: g.previewWidth - r, top: top * sy, right: g.previewWidth - l, bottom: bottom * sy)
            : Box(left: l, top: top * sy, right: r, bottom: bottom * sy)
    }

    /// Rotates a box in a raw sensor frame by `degrees` clockwise (0/90/180/270) into the upright frame.
    func rotated(degrees: Int, srcWidth: Int, srcHeight: Int) -> Box {
        let w = Float(srcWidth), h = Float(srcHeight)
        switch ((degrees % 360) + 360) % 360 {
        case 0: return self
        case 90: return Box(left: h - bottom, top: left, right: h - top, bottom: right)
        case 180: return Box(left: w - right, top: h - bottom, right: w - left, bottom: h - top)
        case 270: return Box(left: top, top: w - right, right: bottom, bottom: w - left)
        default: preconditionFailure("rotation must be a multiple of 90")
        }
    }

    func horizontalOffset(_ g: FrameGeometry) -> Float {
        let p = toPreview(g)
        return min(max((p.centerX / g.previewWidth) * 2 - 1, -1), 1)
    }
}

/// Crop rectangle for the doll cutout: the box plus a margin, clamped to the image.
public func cutoutRect(box: Box, margin: Float, imageWidth: Int, imageHeight: Int) -> (x: Int, y: Int, w: Int, h: Int)? {
    let mx = box.width * margin, my = box.height * margin
    let left = min(max(Int(box.left - mx), 0), imageWidth - 1)
    let top = min(max(Int(box.top - my), 0), imageHeight - 1)
    let right = min(max(Int(box.right + mx), left + 1), imageWidth)
    let bottom = min(max(Int(box.bottom + my), top + 1), imageHeight)
    guard right > left, bottom > top else { return nil }
    return (left, top, right - left, bottom - top)
}
