import XCTest
@testable import GhostCore

final class StateMachineTests: XCTestCase {
    private let target = Target(label: "teddy bear", score: 0.9, box: Box(left: 0, top: 0, right: 10, bottom: 10), horizontalOffset: 0.2, proximity: 0.3)
    private let locked = DetectionGate.Output(locked: true, target: nil, justLocked: false, justLost: false)
    private let unlocked = DetectionGate.Output(locked: false, target: nil, justLocked: false, justLost: false)

    func testLockThenNextFrameScares() {
        let m = GhostStateMachine()
        XCTAssertEqual(m.onGate(locked, target: target), .locked(target, haunted: true))
        XCTAssertEqual(m.onGate(locked, target: target), .scaring(target))
    }

    func testHoldKeepsLocked() {
        let m = GhostStateMachine()
        _ = m.onGate(locked, target: target)
        XCTAssertEqual(m.onGate(locked, target: target, holdScare: true), .locked(target, haunted: true))
        XCTAssertEqual(m.onGate(locked, target: target), .scaring(target))
    }

    func testLostDuringScaringGoesToCooldown() {
        let m = GhostStateMachine()
        _ = m.onGate(locked, target: target); _ = m.onGate(locked, target: target)
        XCTAssertEqual(m.onGate(unlocked, target: nil), .cooldown(target))
    }

    func testCooldownNeedsTimerAndRelease() {
        let m = GhostStateMachine()
        _ = m.onGate(locked, target: target); _ = m.onGate(locked, target: target); _ = m.onScareFinished()
        XCTAssertTrue(m.onGate(locked, target: target).isCooldown)
        XCTAssertTrue(m.onCooldownFinished().isCooldown)   // still locked
        XCTAssertEqual(m.onGate(unlocked, target: nil), .searching)
    }

    func testUnhauntedNeverScaresAndCanWakeUp() {
        let m = GhostStateMachine(decideHaunted: { false })
        XCTAssertEqual(m.onGate(locked, target: target), .locked(target, haunted: false))
        for _ in 0..<5 { XCTAssertEqual(m.onGate(locked, target: target), .locked(target, haunted: false)) }
        XCTAssertEqual(m.wakeUp(), .locked(target, haunted: true))
        XCTAssertTrue(m.onGate(locked, target: target).isScaring)
    }

    func testDecisionAskedOncePerLock() {
        var asked = 0
        let m = GhostStateMachine(decideHaunted: { asked += 1; return false })
        _ = m.onGate(locked, target: target); _ = m.onGate(locked, target: target)
        _ = m.onGate(unlocked, target: nil); _ = m.onGate(locked, target: target)
        XCTAssertEqual(asked, 2)
    }
}
