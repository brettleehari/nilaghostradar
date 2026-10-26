import SwiftUI
import GhostCore

/// PLACEHOLDER palette, same values as the Android build. Radar colour is Nila's open question.
enum Theme {
    static let green = Color(rgb: radarGreen)
    static let amber = Color(rgb: radarAmber)
    static let red = Color(rgb: radarRed)
    static let bone = Color(red: 0xE8 / 255, green: 0xE6 / 255, blue: 0xDF / 255)
}

extension Color {
    init(rgb: RGB) { self.init(red: Double(rgb.r), green: Double(rgb.g), blue: Double(rgb.b)) }
}

func heatColor(_ heat: Float) -> Color { Color(rgb: heatColour(heat)) }
