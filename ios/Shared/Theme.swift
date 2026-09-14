import SwiftUI

extension Color {
    init(_ argb: ARGB) { self.init(uiColor: argb.uiColor) }
}

/// The design tokens, in the one place SwiftUI reads them from.
enum Theme {
    static let cream = Color(Palette.cream)
    static let paper = Color(Palette.paper)
    static let ink = Color(Palette.ink)
    static let inkSoft = Color(0x8F12120F as ARGB)
    static let inkFaint = Color(0x3312120F as ARGB)
    static let lime = Color(0xFFC9F73F as ARGB)
    static let red = Color(0xFFE5331C as ARGB)

    /// The statement type: always ExtraBold, always set tight.
    static func display(_ size: CGFloat) -> Font { Font(Fonts.extraBold(size) as CTFont) }
    static func caps(_ size: CGFloat) -> Font { Font(Fonts.semiBold(size) as CTFont) }
    static func body(_ size: CGFloat) -> Font { Font(Fonts.medium(size) as CTFont) }
}

extension View {
    /// Headings are set at -0.03em, the way the design system asks for.
    func displayStyle(_ size: CGFloat, color: Color = Theme.ink) -> some View {
        self.font(Theme.display(size))
            .tracking(size * -0.03)
            .foregroundStyle(color)
    }

    func capsStyle(_ size: CGFloat = 10, color: Color = Theme.inkSoft) -> some View {
        self.font(Theme.caps(size))
            .tracking(1.1)
            .foregroundStyle(color)
    }

    func bodyStyle(_ size: CGFloat = 14, color: Color = Theme.inkSoft) -> some View {
        self.font(Theme.body(size)).foregroundStyle(color)
    }
}
