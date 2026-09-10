import UIKit

/**
 Flat, warm, high-contrast. No gradients, no glow, no shadow.

 A kept day is solid ink on a colour block; a missed one is a wash of that same
 ink. Nothing emits, so a dense year stays legible where a glowing grid smears
 into blobs.
 */
enum Palette {
    static let cream: ARGB = 0xFFE7E3D4
    static let paper: ARGB = 0xFFF4F2EA
    static let white: ARGB = 0xFFFFFFFF
    static let ink: ARGB   = 0xFF12120F

    /// Ink at the weights the grid uses.
    static let inkMissed: ARGB = 0x3312120F
    static let inkFuture: ARGB = 0x1712120F
    static let inkRule: ARGB   = 0x2E12120F
    static let inkSoft: ARGB   = 0x8F12120F
}

/// A packed 32-bit colour, alpha first — the same literals the Kotlin uses.
typealias ARGB = UInt32

extension ARGB {

    var uiColor: UIColor {
        UIColor(
            red: CGFloat((self >> 16) & 0xFF) / 255.0,
            green: CGFloat((self >> 8) & 0xFF) / 255.0,
            blue: CGFloat(self & 0xFF) / 255.0,
            alpha: CGFloat((self >> 24) & 0xFF) / 255.0
        )
    }

    var cgColor: CGColor { uiColor.cgColor }

    /// The same colour at a new alpha (0...255), preserving its RGB.
    func withAlpha(_ alpha: Int) -> ARGB {
        let a = UInt32(min(max(alpha, 0), 255))
        return (self & 0x00FFFFFF) | (a << 24)
    }
}

/**
 A ritual's colour. `block` is the card, `onBlock` everything drawn on it.
 Once today is marked the card flips to ink and `block` becomes the grid's ink.
 */
struct Accent: Equatable {
    let name: String
    let block: ARGB
    let onBlock: ARGB

    init(_ name: String, _ block: ARGB, _ onBlock: ARGB = Palette.ink) {
        self.name = name
        self.block = block
        self.onBlock = onBlock
    }
}

let ACCENTS: [Accent] = [
    Accent("Lime", 0xFFC9F73F),
    Accent("Red", 0xFFE5331C, Palette.paper),
    Accent("Sage", 0xFF9DB8A4),
    Accent("Camel", 0xFFBC9F76),
    Accent("Butter", 0xFFEDE55C),
    Accent("Orange", 0xFFF26A1B)
]

func accentAt(_ index: Int) -> Accent {
    ACCENTS[((index % ACCENTS.count) + ACCENTS.count) % ACCENTS.count]
}
