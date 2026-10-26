// swift-tools-version:5.9
import PackageDescription

// Pure logic shared by nothing but the app, kept as a package so it unit-tests on the Mac in
// seconds (`swift test`) with no simulator. No UIKit, no AVFoundation in here.
let package = Package(
    name: "GhostCore",
    platforms: [.iOS(.v15), .macOS(.v12)],
    products: [.library(name: "GhostCore", targets: ["GhostCore"])],
    targets: [
        .target(name: "GhostCore"),
        .testTarget(name: "GhostCoreTests", dependencies: ["GhostCore"]),
    ]
)
