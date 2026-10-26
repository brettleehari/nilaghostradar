import XCTest
@testable import GhostCore

final class HauntPolicyTests: XCTestCase {
    private func policy(_ edit: (inout HauntPolicy.Config) -> Void, random: Float) -> HauntPolicy {
        var c = HauntPolicy.Config(); edit(&c); return HauntPolicy(config: c, random: { random })
    }

    func testWarmUp() {
        let p = policy({ $0.warmUpMs = 10_000; $0.chance = 1 }, random: 0)
        p.startSession(nowMs: 0)
        XCTAssertFalse(p.decide(nowMs: 5_000)); XCTAssertTrue(p.decide(nowMs: 10_000))
    }

    func testMinGap() {
        let p = policy({ $0.warmUpMs = 0; $0.minGapMs = 30_000; $0.chance = 1 }, random: 0)
        p.startSession(nowMs: 0)
        XCTAssertTrue(p.decide(nowMs: 1_000)); XCTAssertFalse(p.decide(nowMs: 20_000)); XCTAssertTrue(p.decide(nowMs: 31_000))
    }

    func testGuaranteeAfterSkips() {
        let p = policy({ $0.warmUpMs = 0; $0.minGapMs = 0; $0.chance = 0; $0.guaranteeAfterSkips = 3 }, random: 0.999)
        p.startSession(nowMs: 0)
        XCTAssertFalse(p.decide(nowMs: 1)); XCTAssertFalse(p.decide(nowMs: 2)); XCTAssertFalse(p.decide(nowMs: 3))
        XCTAssertTrue(p.decide(nowMs: 4)); XCTAssertFalse(p.decide(nowMs: 5))
    }

    func testMaxPerSession() {
        let p = policy({ $0.warmUpMs = 0; $0.minGapMs = 0; $0.chance = 1; $0.maxPerSession = 1 }, random: 0)
        p.startSession(nowMs: 0)
        XCTAssertTrue(p.decide(nowMs: 1)); for i in 0..<10 { XCTAssertFalse(p.decide(nowMs: Int64(10 + i))) }
    }

    func testForce() {
        let p = policy({ $0.warmUpMs = 60_000; $0.minGapMs = 60_000; $0.chance = 0 }, random: 0.999)
        p.startSession(nowMs: 0)
        p.forceNextHaunt(); XCTAssertTrue(p.decide(nowMs: 1)); XCTAssertFalse(p.decide(nowMs: 2))
    }

    func testPauseResume() {
        let p = policy({ $0.warmUpMs = 10_000; $0.minGapMs = 0; $0.chance = 1; $0.sessionResetAfterMs = 60_000 }, random: 0)
        p.startSession(nowMs: 0)
        p.onPaused(nowMs: 20_000); XCTAssertFalse(p.onResumed(nowMs: 50_000)); XCTAssertTrue(p.decide(nowMs: 50_001))
        p.onPaused(nowMs: 60_000); XCTAssertTrue(p.onResumed(nowMs: 120_000))
        XCTAssertFalse(p.decide(nowMs: 125_000)); XCTAssertTrue(p.decide(nowMs: 130_000))
    }
}

final class SoulScannerTests: XCTestCase {
    private func obj(_ l: String, _ s: Float) -> DetectedObject { DetectedObject(label: l, score: s, box: Box(left: 0, top: 0, right: 10, bottom: 10)) }

    func testBestEverydayObject() {
        XCTAssertEqual(SoulScanner().scan([obj("cup", 0.6), obj("chair", 0.8)], nowMs: 0), "chair")
    }
    func testIgnoresWeakTriggerSkipped() {
        let s = SoulScanner()
        XCTAssertNil(s.scan([obj("cup", 0.5)], nowMs: 0))
        XCTAssertNil(s.scan([obj("teddy bear", 0.9)], nowMs: 0))
        XCTAssertNil(s.scan([obj("person", 0.9)], nowMs: 0))
        XCTAssertEqual(s.scan([obj("teddy bear", 0.9), obj("cup", 0.6)], nowMs: 0), "cup")
    }
    func testGapAndRepeat() {
        var c = SoulScanner.Config(); c.minGapMs = 4_000; c.repeatLabelMs = 20_000
        let s = SoulScanner(config: c)
        XCTAssertEqual(s.scan([obj("cup", 0.9)], nowMs: 0), "cup")
        XCTAssertNil(s.scan([obj("bed", 0.9)], nowMs: 3_999))
        XCTAssertNil(s.scan([obj("cup", 0.9)], nowMs: 10_000))
        XCTAssertEqual(s.scan([obj("cup", 0.9), obj("bed", 0.6)], nowMs: 10_001), "bed")
        XCTAssertEqual(s.scan([obj("cup", 0.9)], nowMs: 20_000), "cup")
    }
}

final class SoulHeatTests: XCTestCase {
    private func bear(_ score: Float, _ hf: Float) -> DetectedObject { DetectedObject(label: "teddy bear", score: score, box: Box(left: 0, top: 0, right: 100, bottom: 640 * hf)) }

    func testRaw() {
        let h = SoulHeat()
        XCTAssertEqual(h.raw(score: 0.3, boxHeightFraction: 0), 0, accuracy: 1e-4)
        XCTAssertGreaterThan(h.raw(score: 0.6, boxHeightFraction: 0.1), h.raw(score: 0.4, boxHeightFraction: 0.1))
        XCTAssertEqual(h.raw(score: 0.99, boxHeightFraction: 0.9), 1, accuracy: 1e-4)
    }
    func testSmoothing() {
        var c = SoulHeat.Config(); c.riseFraction = 0.5; c.fallFraction = 0.2
        let h = SoulHeat(config: c)
        XCTAssertEqual(h.onFrame([bear(0.9, 0.5)], imageHeight: 640), 0.5, accuracy: 1e-4)
        XCTAssertEqual(h.onFrame([bear(0.9, 0.5)], imageHeight: 640), 0.75, accuracy: 1e-4)
        XCTAssertEqual(h.onFrame([], imageHeight: 640), 0.6, accuracy: 1e-4)
        for _ in 0..<60 { h.onFrame([], imageHeight: 640) }
        XCTAssertEqual(h.value, 0)
    }
    func testBands() {
        XCTAssertEqual(SoulHeat.band(0), .cold); XCTAssertEqual(SoulHeat.band(0.3), .warm); XCTAssertEqual(SoulHeat.band(0.9), .hot)
    }
}

final class GeometryTests: XCTestCase {
    func testFitted() {
        XCTAssertEqual(fittedBoxSize(container: FitSize(width: 1000, height: 1000), box: FitSize(width: 480, height: 640)), FitSize(width: 750, height: 1000))
    }
    func testToPreviewAndMirror() {
        let g = FrameGeometry(imageWidth: 480, imageHeight: 640, previewWidth: 960, previewHeight: 1280)
        XCTAssertEqual(Box(left: 10, top: 20, right: 30, bottom: 40).toPreview(g), Box(left: 20, top: 40, right: 60, bottom: 80))
        let m = FrameGeometry(imageWidth: 480, imageHeight: 640, previewWidth: 480, previewHeight: 640, mirrored: true)
        XCTAssertEqual(Box(left: 10, top: 0, right: 30, bottom: 10).toPreview(m), Box(left: 450, top: 0, right: 470, bottom: 10))
    }
    func testRotate90() {
        XCTAssertEqual(Box(left: 0, top: 0, right: 10, bottom: 20).rotated(degrees: 90, srcWidth: 640, srcHeight: 480), Box(left: 460, top: 0, right: 480, bottom: 10))
    }
    func testBlipBrightness() {
        XCTAssertEqual(Blips.brightness(blipAngle: 90, sweepAngle: 90), 1, accuracy: 1e-4)
        XCTAssertEqual(Blips.brightness(blipAngle: 90, sweepAngle: 240), 0.5, accuracy: 1e-4)
        XCTAssertEqual(Blips.brightness(blipAngle: 90, sweepAngle: 30), 0, accuracy: 1e-4)
    }
    func testCutoutClamped() {
        let r = cutoutRect(box: Box(left: -5, top: 10, right: 100, bottom: 700), margin: 0.1, imageWidth: 480, imageHeight: 640)!
        XCTAssertEqual(r.x, 0); XCTAssertEqual(r.y, 0); XCTAssertEqual(r.w, 110); XCTAssertEqual(r.h, 640)
    }
    func testHeatColour() {
        func close(_ a: RGB, _ b: RGB) { XCTAssertEqual(a.r, b.r, accuracy: 1e-5); XCTAssertEqual(a.g, b.g, accuracy: 1e-5); XCTAssertEqual(a.b, b.b, accuracy: 1e-5) }
        close(heatColour(0), radarGreen); close(heatColour(0.5), radarAmber); close(heatColour(1), radarRed)
    }
}
