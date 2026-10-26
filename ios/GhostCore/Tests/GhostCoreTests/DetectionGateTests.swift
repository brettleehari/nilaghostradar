import XCTest
@testable import GhostCore

final class DetectionGateTests: XCTestCase {
    private func bear(_ score: Float) -> DetectedObject { DetectedObject(label: "teddy bear", score: score, box: Box(left: 0, top: 0, right: 10, bottom: 10)) }
    private func chair(_ score: Float) -> DetectedObject { DetectedObject(label: "chair", score: score, box: Box(left: 0, top: 0, right: 10, bottom: 10)) }

    func testThreeHitsLock() {
        let g = DetectionGate()
        XCTAssertFalse(g.onFrame([bear(0.9)], nowMs: 0).locked)
        XCTAssertFalse(g.onFrame([bear(0.9)], nowMs: 100).locked)
        let o = g.onFrame([bear(0.9)], nowMs: 200)
        XCTAssertTrue(o.locked); XCTAssertTrue(o.justLocked)
    }

    func testMissResetsCount() {
        let g = DetectionGate()
        _ = g.onFrame([bear(0.9)], nowMs: 0); _ = g.onFrame([bear(0.9)], nowMs: 100)
        _ = g.onFrame([], nowMs: 200)
        XCTAssertFalse(g.onFrame([bear(0.9)], nowMs: 300).locked)
    }

    func testWeakOrWrongLabelNeverLocks() {
        let g = DetectionGate()
        for i in 0..<10 { XCTAssertFalse(g.onFrame([bear(0.49), chair(0.99)], nowMs: Int64(i * 100)).locked) }
    }

    func testLostAfterTimeout() {
        let g = DetectionGate()
        for i in 0..<3 { _ = g.onFrame([bear(0.9)], nowMs: Int64(i * 100)) }
        XCTAssertTrue(g.onFrame([], nowMs: 600).locked)
        let o = g.onFrame([], nowMs: 700)
        XCTAssertFalse(o.locked); XCTAssertTrue(o.justLost)
    }

    func testTargetIsBestWhileLocked() {
        let g = DetectionGate()
        for i in 0..<3 { _ = g.onFrame([bear(0.6), bear(0.9)], nowMs: Int64(i * 100)) }
        XCTAssertEqual(g.onFrame([], nowMs: 300).target?.score, 0.9)
    }
}
