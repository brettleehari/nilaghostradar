import Foundation

/// Every tunable number for the scare in one place. Same values as the Android build.
public struct ScareConfig {
    public var cutoutMarginFraction: Float = 0.12
    public var faceCenterXFraction: Float = 0.50
    public var faceCenterYFraction: Float = 0.24
    public var faceWidthFraction: Float = 0.52
    public var faceCrossfadeMs: Int = 250
    public var floatUpFraction: Float = 0.06
    public var floatUpMs: Int = 250
    public var approachScale: Float = 3.2
    public var approachMs: Int = 800
    public var holdMs: Int = 700
    public var jitterDegrees: Float = 3
    public var fadeOutMs: Int = 400
    public var cooldownMaxMs: Int64 = 8_000
    public var blackoutMs: Int64 = 1_200
    public var aftershockAtMs: Int64 = 2_000
    public var aftershockFlashMs: Int64 = 90
    public var aftershockRumbleMs: Int64 = 90
    public var voiceLine: String = "Bloody Mary"
    public var voiceMaxHoldMs: Int64 = 1_500
    public var noticeMs: Int64 = 2_500
    public init() {}
    public var scareDurationMs: Int64 { Int64(floatUpMs + approachMs + holdMs) }
}

/// Every number for the warmer/colder feel.
public struct ProximityConfig {
    public var pulseAmplitude: Float = 0.025
    public var pulsePeriodColdMs: Float = 2_400
    public var pulsePeriodHotMs: Float = 450
    public var tickFromHeat: Float = 0.15
    public var tickPeriodWarmMs: Float = 1_100
    public var tickPeriodHotMs: Float = 220
    public var tickDurationMs: Int64 = 18
    public var tickStrengthMin: Float = 0.25
    public var tickStrengthMax: Float = 0.9
    public var heartbeatFromHeat: Float = 0.55
    public var heartbeatPeriodSlowMs: Float = 950
    public var heartbeatPeriodFastMs: Float = 480
    /// While Searching, the first time the readout turns HOT the phone whispers "I can see you"; not again within the gap.
    public var nearWhisper: Bool = true
    public var nearWhisperGapMs: Int64 = 6_000
    public var traceHeatCap: Float = 0.4
    public var shakeMaxPx: Float = 14
    public var joltAtLock: Bool = true
    public var tempoMaxSpeed: Float = 1.6
    public var ambienceVolumeCold: Float = 0.6
    public var ambienceVolumeHot: Float = 0.9
    public init() {}

    public func pulsePeriodMs(_ heat: Float) -> Float { lerp(pulsePeriodColdMs, pulsePeriodHotMs, heat) }
    public func tickPeriodMs(_ heat: Float) -> Int64 { Int64(lerp(tickPeriodWarmMs, tickPeriodHotMs, warmth(heat))) }
    public func tickStrength(_ heat: Float) -> Float { lerp(tickStrengthMin, tickStrengthMax, warmth(heat)) }
    public func tempo(_ heat: Float) -> Float { lerp(1, tempoMaxSpeed, heat) }
    public func ambienceVolume(_ heat: Float) -> Float { lerp(ambienceVolumeCold, ambienceVolumeHot, heat) }
    public func heartbeat(_ heat: Float) -> Bool { heat >= heartbeatFromHeat }
    public func heartbeatPeriodMs(_ heat: Float) -> Int64 {
        let t = min(max((heat - heartbeatFromHeat) / (1 - heartbeatFromHeat), 0), 1)
        return Int64(lerp(heartbeatPeriodSlowMs, heartbeatPeriodFastMs, t))
    }
    private func warmth(_ heat: Float) -> Float { min(max((heat - tickFromHeat) / (1 - tickFromHeat), 0), 1) }
    private func lerp(_ a: Float, _ b: Float, _ t: Float) -> Float { a + (b - a) * min(max(t, 0), 1) }
}

/// Radar colour for a heat value as RGB 0..1: green when cold, amber when warm, red when hot.
public struct RGB: Equatable { public let r: Float, g: Float, b: Float }
public let radarGreen = RGB(r: 0x39 / 255.0, g: 0xFF / 255.0, b: 0x14 / 255.0)
public let radarAmber = RGB(r: 0xFF / 255.0, g: 0xB3 / 255.0, b: 0x00 / 255.0)
public let radarRed = RGB(r: 0xFF / 255.0, g: 0x1E / 255.0, b: 0x3C / 255.0)

public func heatColour(_ heat: Float) -> RGB {
    let h = min(max(heat, 0), 1)
    func mix(_ a: RGB, _ b: RGB, _ t: Float) -> RGB { RGB(r: a.r + (b.r - a.r) * t, g: a.g + (b.g - a.g) * t, b: a.b + (b.b - a.b) * t) }
    return h < 0.5 ? mix(radarGreen, radarAmber, h / 0.5) : mix(radarAmber, radarRed, (h - 0.5) / 0.5)
}
