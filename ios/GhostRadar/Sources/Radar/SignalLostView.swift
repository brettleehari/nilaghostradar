import SwiftUI

/// The cut after the lunge: hard black, a burst of static, SIGNAL LOST flickering. PLACEHOLDER copy.
struct SignalLostView: View {
    var body: some View {
        TimelineView(.animation) { timeline in
            let seed = UInt64(timeline.date.timeIntervalSinceReferenceDate * 60)
            ZStack {
                Color.black
                Canvas { ctx, size in
                    var rng = SeededRandom(seed: seed)
                    for _ in 0..<260 {
                        let y = CGFloat(rng.next01()) * size.height, x = CGFloat(rng.next01()) * size.width
                        let len = 2 + CGFloat(rng.next01()) * 28, a = 0.05 + rng.next01() * 0.35
                        var p = Path(); p.move(to: CGPoint(x: x, y: y)); p.addLine(to: CGPoint(x: x + len, y: y))
                        ctx.stroke(p, with: .color(.white.opacity(a)), lineWidth: 1.5)
                    }
                    for _ in 0..<2 {
                        let y = CGFloat(rng.next01()) * size.height
                        var p = Path(); p.move(to: CGPoint(x: 0, y: y)); p.addLine(to: CGPoint(x: size.width, y: y))
                        ctx.stroke(p, with: .color(.white.opacity(0.12)), lineWidth: 3)
                    }
                }
                Text("SIGNAL LOST").font(.system(size: 30, weight: .bold, design: .monospaced)).tracking(6)
                    .foregroundStyle(Theme.red).opacity((seed / 3) % 4 == 0 ? 0.15 : 1)
            }
            .ignoresSafeArea()
        }
    }
}

/// The aftershock: the face, huge, for a couple of frames when the kid has just breathed out.
struct AftershockFlashView: View {
    var body: some View {
        ZStack {
            Color.black.opacity(0.85)
            EvilFace.image.resizable().scaledToFit().padding(.horizontal, 28)
        }
        .ignoresSafeArea()
    }
}

/// Tiny xorshift so the static can be re-rolled deterministically per frame.
struct SeededRandom {
    private var s: UInt64
    init(seed: UInt64) { s = seed &* 0x9E3779B97F4A7C15 | 1 }
    mutating func next() -> UInt64 { s ^= s << 13; s ^= s >> 7; s ^= s << 17; return s }
    mutating func next01() -> Double { Double(next() % 10_000) / 10_000 }
}
