import SwiftUI
import GhostCore

/// The hunting screen: black, three rings, sweeping wedge, fading blips, a big arrow and a one-word
/// readout. Colour and breathing rate follow `heat`, Find My style. Shakes after a scare.
struct RadarView: View {
    let state: GhostState
    let notice: Notice?
    let heat: Float
    let proximity: ProximityConfig

    @State private var blips: [Blip] = Blips.random(7)
    @State private var wander: Float = 0
    @State private var phase: Double = 0
    @State private var lastTick: TimeInterval = 0

    private var tracking: Bool { state.isLocked || state.isScaring }
    private var haunted: Bool { state.isHauntedLock || state.isScaring }
    private var gone: Bool { state.isCooldown }

    var body: some View {
        let colour = heatColor(heat)
        TimelineView(.animation) { timeline in
            let t = timeline.date.timeIntervalSinceReferenceDate
            let sweep = Float((t * 180).truncatingRemainder(dividingBy: 360))   // 2 s per revolution
            let pulse = 1 + proximity.pulseAmplitude * Float(sin(breathingPhase(at: t)))
            let shake = gone ? heat * proximity.shakeMaxPx : 0
            let jitter = shake > 0 ? CGSize(width: CGFloat.random(in: -1...1) * CGFloat(shake), height: CGFloat.random(in: -1...1) * CGFloat(shake)) : .zero

            VStack(spacing: 24) {
                StatusLine(state: state, colour: colour, t: t)
                ZStack {
                    SweepCanvas(sweepAngle: sweep, blips: blips, lockedBlip: state.target.map(blipFor), colour: colour, t: t)
                    DirectionArrow(angle: arrowAngle, lit: tracking, colour: colour)
                        .frame(width: 120, height: 120)
                }
                .scaleEffect(CGFloat(pulse))
                .offset(jitter)
                HeatReadout(heat: heat, here: haunted, gone: gone, colour: colour)
                RadarNotice(notice: notice, colour: colour)
            }
            .padding(.horizontal, 24)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.black)
            .animation(.easeInOut(duration: 0.35), value: heat)
        }
        .task { await wanderLoop() }
        .task { await blipLoop() }
    }

    private var arrowAngle: Float { state.target.map { $0.horizontalOffset * maxArrowDegrees } ?? wander }

    /// Phase advances at a rate set by heat so a tempo change never jumps.
    private func breathingPhase(at t: TimeInterval) -> Double {
        // Stored in @State through a tiny side channel: SwiftUI forbids mutation during body, so
        // the increment is dispatched; the visual lag is one frame.
        let dtMs = lastTick == 0 ? 0 : (t - lastTick) * 1000
        let next = (phase + dtMs / Double(proximity.pulsePeriodMs(heat)) * 2 * .pi).truncatingRemainder(dividingBy: 2 * .pi)
        DispatchQueue.main.async { phase = next; lastTick = t }
        return phase
    }

    private func wanderLoop() async {
        while !Task.isCancelled {
            if state.target == nil { withAnimation(.spring(response: 1.2, dampingFraction: 0.55)) { wander = Float.random(in: -45...45) } }
            try? await Task.sleep(nanoseconds: UInt64(1_600 + Int.random(in: 0..<900)) * 1_000_000)
        }
    }

    private func blipLoop() async {
        while !Task.isCancelled {
            try? await Task.sleep(nanoseconds: 2_000 * 1_000_000)
            blips = Blips.shuffle(blips, howMany: 2)
        }
    }
}

private let maxArrowDegrees: Float = 55

private func blipFor(_ t: Target) -> Blip {
    Blip(angle: t.horizontalOffset * maxArrowDegrees, radius: min(max(1 - t.proximity * 1.2, 0.1), 0.95), size: 1.6)
}

private struct StatusLine: View {
    let state: GhostState; let colour: Color; let t: TimeInterval
    var body: some View {
        let blink = 0.25 + 0.75 * (0.5 + 0.5 * sin(t * .pi / 0.7))
        let (text, alpha): (String, Double) = {
            switch state {
            case .searching: return ("SEARCHING…", blink)
            case .locked(_, let h): return h ? ("SIGNAL LOCKED", 1) : ("SEARCHING…", blink)
            case .scaring: return ("IT SEES YOU", 1)
            case .cooldown: return ("SIGNAL LOST", blink)
            }
        }()
        Text(text).font(.system(size: 22, weight: .bold, design: .monospaced)).tracking(4)
            .foregroundStyle(colour).opacity(alpha)
    }
}

private struct HeatReadout: View {
    let heat: Float; let here: Bool; let gone: Bool; let colour: Color
    var body: some View {
        let word: String = {
            if here { return "HERE" }
            if gone { return "GONE" }
            switch SoulHeat.band(heat) { case .cold: return "COLD"; case .warm: return "WARM"; case .hot: return "HOT" }
        }()
        VStack(spacing: 4) {
            Text(word).font(.system(size: 56, weight: .bold, design: .monospaced)).tracking(6).foregroundStyle(colour)
            Text("SOUL PROXIMITY").font(.system(size: 11, design: .monospaced)).tracking(3).foregroundStyle(colour.opacity(0.6))
        }
    }
}

/// A small pill under the readout: "FAINT TRACE · NO ANSWER", "CHAIR · NO SOUL". PLACEHOLDER copy.
private struct RadarNotice: View {
    let notice: Notice?; let colour: Color
    var body: some View {
        ZStack {
            if let notice {
                Text(text(notice)).font(.system(size: 13, design: .monospaced)).tracking(2).foregroundStyle(colour)
                    .padding(.horizontal, 14).padding(.vertical, 6)
                    .overlay(Capsule().stroke(colour.opacity(0.5), lineWidth: 1))
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .id(notice.id)
            }
        }
        .frame(height: 36)
        .animation(.easeInOut(duration: 0.3), value: notice)
    }
    private func text(_ n: Notice) -> String {
        switch n {
        case .faintTrace: return "FAINT TRACE · NO ANSWER"
        case .noSoul(let label, _): return "\(label.uppercased()) · NO SOUL"
        }
    }
}

/// The radar dish: rings, crosshair, sweep wedge, random fading blips, the real echo pulsing.
private struct SweepCanvas: View {
    let sweepAngle: Float; let blips: [Blip]; let lockedBlip: Blip?; let colour: Color; let t: TimeInterval
    var body: some View {
        Canvas { ctx, size in
            let centre = CGPoint(x: size.width / 2, y: size.height / 2)
            let outer = min(size.width, size.height) / 2 * 0.95
            ctx.fill(Path(ellipseIn: CGRect(x: centre.x - outer, y: centre.y - outer, width: outer * 2, height: outer * 2)), with: .color(colour.opacity(0.05)))
            for i in 1...3 {
                let r = outer * CGFloat(i) / 3
                let ring = Path(ellipseIn: CGRect(x: centre.x - r, y: centre.y - r, width: r * 2, height: r * 2))
                ctx.stroke(ring, with: .color(colour.opacity(0.12)), lineWidth: 6)
                ctx.stroke(ring, with: .color(colour.opacity(0.7)), lineWidth: 1.5)
            }
            var cross = Path()
            cross.move(to: CGPoint(x: centre.x - outer, y: centre.y)); cross.addLine(to: CGPoint(x: centre.x + outer, y: centre.y))
            cross.move(to: CGPoint(x: centre.x, y: centre.y - outer)); cross.addLine(to: CGPoint(x: centre.x, y: centre.y + outer))
            ctx.stroke(cross, with: .color(colour.opacity(0.35)), lineWidth: 1.5)

            // Sweep wedge: an angular gradient brightening toward the leading edge.
            let angle = Angle.degrees(Double(sweepAngle) - 90)
            let gradient = Gradient(stops: [.init(color: .clear, location: 0), .init(color: .clear, location: 0.75),
                                            .init(color: colour.opacity(0.35), location: 0.97), .init(color: colour.opacity(0.85), location: 1)])
            let disc = Path(ellipseIn: CGRect(x: centre.x - outer, y: centre.y - outer, width: outer * 2, height: outer * 2))
            ctx.fill(disc, with: .conicGradient(gradient, center: centre, angle: angle))
            var edge = Path()
            edge.move(to: centre)
            edge.addLine(to: CGPoint(x: centre.x + outer * cos(angle.radians), y: centre.y + outer * sin(angle.radians)))
            ctx.stroke(edge, with: .color(colour), lineWidth: 2.25)

            for b in blips {
                let a = Blips.brightness(blipAngle: b.angle, sweepAngle: sweepAngle)
                if a <= 0 { continue }
                drawBlip(ctx, centre, outer, b, colour.opacity(Double(a)), glow: Double(a) * 0.25)
            }
            if let b = lockedBlip {
                let pulse = 0.6 + 0.4 * (0.5 + 0.5 * sin(t * .pi / 0.5))
                drawBlip(ctx, centre, outer, Blip(angle: b.angle, radius: b.radius, size: b.size * Float(pulse)), colour, glow: 0.5 * pulse)
            }
            ctx.fill(Path(ellipseIn: CGRect(x: centre.x - 3, y: centre.y - 3, width: 6, height: 6)), with: .color(colour))
        }
        .aspectRatio(1, contentMode: .fit)
    }

    private func drawBlip(_ ctx: GraphicsContext, _ centre: CGPoint, _ outer: CGFloat, _ b: Blip, _ colour: Color, glow: Double) {
        let rad = Double(b.angle - 90) * .pi / 180
        let p = CGPoint(x: centre.x + outer * CGFloat(b.radius) * cos(rad), y: centre.y + outer * CGFloat(b.radius) * sin(rad))
        let r = 4 * CGFloat(b.size)
        ctx.fill(Path(ellipseIn: CGRect(x: p.x - r * 3, y: p.y - r * 3, width: r * 6, height: r * 6)), with: .color(colour.opacity(glow)))
        ctx.fill(Path(ellipseIn: CGRect(x: p.x - r, y: p.y - r, width: r * 2, height: r * 2)), with: .color(colour))
    }
}

/// Find My-style arrow; springs toward `angle` (0 = up, positive = clockwise).
private struct DirectionArrow: View {
    let angle: Float; let lit: Bool; let colour: Color
    var body: some View {
        GeometryReader { geo in
            let s = min(geo.size.width, geo.size.height)
            let c = CGPoint(x: geo.size.width / 2, y: geo.size.height / 2)
            let h = s * 0.42, w = s * 0.30
            let path = Path { p in
                p.move(to: CGPoint(x: c.x, y: c.y - h))
                p.addLine(to: CGPoint(x: c.x + w, y: c.y + h * 0.7))
                p.addLine(to: CGPoint(x: c.x, y: c.y + h * 0.35))
                p.addLine(to: CGPoint(x: c.x - w, y: c.y + h * 0.7))
                p.closeSubpath()
            }
            ZStack {
                if lit { path.stroke(colour.opacity(0.35), style: StrokeStyle(lineWidth: 14, lineJoin: .round)) }
                path.fill(lit ? colour : colour.opacity(0.9))
                path.stroke(Color.black.opacity(0.35), style: StrokeStyle(lineWidth: 1.5, lineJoin: .round))
            }
            .rotationEffect(.degrees(Double(angle)))
            .animation(.spring(response: 0.9, dampingFraction: 0.55), value: angle)
        }
    }
}
