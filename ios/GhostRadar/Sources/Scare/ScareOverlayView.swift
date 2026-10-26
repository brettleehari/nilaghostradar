import SwiftUI
import GhostCore

/// The possessed doll, drawn over the camera preview (must fill exactly the preview's bounds).
/// At Scaring: the cutout appears exactly where the doll is, its face crossfades to the evil
/// face, it lifts slightly and lunges toward the viewer, growing `approachScale`x toward the
/// centre over `approachMs` with jitter, holds, and fades on Cooldown.
struct ScareOverlayView: View {
    let state: GhostState
    let snapshot: CGImage?
    let config: ScareConfig

    @State private var overlayAlpha: Double = 0
    @State private var faceAlpha: Double = 0
    @State private var rise: Double = 0
    @State private var approach: Double = 0

    var body: some View {
        GeometryReader { geo in
            if let target = state.target, overlayAlpha > 0 {
                let pw = Float(geo.size.width), ph = Float(geo.size.height)
                let g = FrameGeometry(imageWidth: target.imageWidth, imageHeight: target.imageHeight, previewWidth: pw, previewHeight: ph)
                let b = target.box.toPreview(g)
                let mx = b.width * config.cutoutMarginFraction, my = b.height * config.cutoutMarginFraction
                let cw = CGFloat(b.width + 2 * mx), ch = CGFloat(b.height + 2 * my)
                let cLeft = CGFloat(b.left - mx), cTop = CGFloat(b.top - my)
                let a = approach
                let scale = 1 + (Double(config.approachScale) - 1) * a
                let cx0 = cLeft + cw / 2, cy0 = cTop + ch / 2 - CGFloat(rise) * CGFloat(config.floatUpFraction) * CGFloat(b.height)
                let cx = cx0 + (geo.size.width / 2 - cx0) * a, cy = cy0 + (geo.size.height / 2 - cy0) * a
                let fw = cw * CGFloat(config.faceWidthFraction)
                let fx = cw * CGFloat(config.faceCenterXFraction) - fw / 2, fy = ch * CGFloat(config.faceCenterYFraction) - fw / 2

                ZStack(alignment: .topLeading) {
                    Color.black.opacity(0.55 * overlayAlpha * (0.3 + 0.7 * a))
                    ZStack(alignment: .topLeading) {
                        if let snapshot { Image(decorative: snapshot, scale: 1).resizable().frame(width: cw, height: ch) }
                        Image("evil_face").resizable().scaledToFit().frame(width: fw, height: fw).offset(x: fx, y: fy).opacity(faceAlpha)
                    }
                    .frame(width: cw, height: ch)
                    .rotationEffect(.degrees(a > 0 && a < 1 ? jitter : 0))
                    .scaleEffect(scale)
                    .offset(x: cx - cw / 2, y: cy - ch / 2)
                    .opacity(overlayAlpha)
                }
            }
        }
        .onChange(of: state.kind) { _, _ in react() }
        .onAppear { react() }
    }

    private var jitter: Double { Double(config.jitterDegrees) * (Int(Date().timeIntervalSinceReferenceDate * 12.5) % 2 == 0 ? 1 : -1) }

    private func react() {
        switch state {
        case .scaring:
            rise = 0; approach = 0; faceAlpha = 0; overlayAlpha = 1
            withAnimation(.linear(duration: Double(config.faceCrossfadeMs) / 1000)) { faceAlpha = 1 }
            withAnimation(.easeInOut(duration: Double(config.floatUpMs) / 1000)) { rise = 1 }
            withAnimation(.easeInOut(duration: Double(config.approachMs) / 1000).delay(Double(config.floatUpMs) / 1000)) { approach = 1 }
        case .cooldown:
            withAnimation(.linear(duration: Double(config.fadeOutMs) / 1000)) { overlayAlpha = 0 }
        default:
            overlayAlpha = 0; faceAlpha = 0; rise = 0; approach = 0
        }
    }
}
