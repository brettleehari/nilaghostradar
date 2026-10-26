import Foundation

/// A fake echo on the radar: lights up when the sweep passes it, then fades until the next pass.
public struct Blip: Equatable {
    /// Degrees clockwise from the top.
    public var angle: Float
    /// 0 (centre) .. 1 (outer ring).
    public var radius: Float
    public var size: Float
    public init(angle: Float, radius: Float, size: Float) { self.angle = angle; self.radius = radius; self.size = size }
}

public enum Blips {
    public static func random(_ count: Int, rng: inout some RandomNumberGenerator) -> [Blip] {
        (0..<count).map { _ in random(rng: &rng) }
    }
    public static func random(rng: inout some RandomNumberGenerator) -> Blip {
        Blip(angle: Float.random(in: 0..<360, using: &rng),
             radius: 0.15 + Float.random(in: 0..<0.8, using: &rng),
             size: 0.6 + Float.random(in: 0..<0.8, using: &rng))
    }
    public static func random(_ count: Int) -> [Blip] { var g = SystemRandomNumberGenerator(); return random(count, rng: &g) }

    /// Replaces `howMany` random blips so the picture slowly changes between sweeps.
    public static func shuffle(_ blips: [Blip], howMany: Int, rng: inout some RandomNumberGenerator) -> [Blip] {
        if blips.isEmpty { return blips }
        var out = blips
        for _ in 0..<howMany { out[Int.random(in: 0..<out.count, using: &rng)] = random(rng: &rng) }
        return out
    }
    public static func shuffle(_ blips: [Blip], howMany: Int) -> [Blip] { var g = SystemRandomNumberGenerator(); return shuffle(blips, howMany: howMany, rng: &g) }

    /// Brightness: 1 just behind the sweep line, fading to 0 `fadeDegrees` later.
    public static func brightness(blipAngle: Float, sweepAngle: Float, fadeDegrees: Float = 300) -> Float {
        let behind = ((sweepAngle - blipAngle).truncatingRemainder(dividingBy: 360) + 360).truncatingRemainder(dividingBy: 360)
        return min(max(1 - behind / fadeDegrees, 0), 1)
    }
}
