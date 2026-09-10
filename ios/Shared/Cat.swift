import CoreGraphics
import UIKit

/// What Mochi is doing, derived from the streak — never chosen for decoration.
enum Mood {
    case awake, pleased, resting, letDown
}

/**
 Mochi, a 32x28 bitmap.

 Only the eye and mouth rows change between moods, so the silhouette never
 shifts — that is what keeps a pixel mascot from looking redrawn each time.
 The extra resolution over the 20x18 original buys him a lit side and a shaded
 one, pink inside the ears, a cream muzzle and feet; the rule that the outline
 never moves is unchanged, and it is the rule that matters.
 */
enum Cat {

    static let cols = 32
    static let rows = 28

    private static let base = [
        "................................",
        ".......K................K.......",
        "......KGK..............KGK......",
        "......KGGKKKKKKKKKKKKKKGGK......",
        ".....KGPLGGGGGGGGGGGGGGGPGK.....",
        "....KGPPPLLLLLLLLGGGGGGPPPGK....",
        "....KPPPPLLLLLLLGGGGGGGPPPPK....",
        ".....KGLLLLLLLGGGGGGGGGGGGK.....",
        "....KGLLLLLLLGGGGGGGGGGGGGSK....",
        "....KGLLLLLLGGGGGGGGGGGGGGGK....",
        "....KGLLLLGGGGGGGGGGGGGGGGGK....",
        "....KGLLLGGGGGGGGGGGGGGGGGGK....",
        "....KGLLGGGGGGGGGGGGGGGGGGGK....",
        "....KGGGGGGGGGGGGGGGGGGGGGGK....",
        "....KGGGGGGGGGGGGGGGGGGGGGSK....",
        ".....KGGGGGGMMPPPPMMGGGGGGK.....",
        ".....KGGGGGGMMMPPMMMGGGGGSK.....",
        "......KGGGGMMMMMMMMMMGGGGK......",
        "......KGGGGGMMMMMMMMGGGGGK......",
        "......KGGGGGGGMMMMGGGGGGGK......",
        "......KGGGGGGGGGGGGGGGGGGK......",
        "......KGGGGGGGGGGGGGGGGGGK......",
        "......KGGGGGGGGGGGGGGGGGGK......",
        "......KGGGGGGGGGGGGGGGGGGK......",
        "......KGGSKKSGGGGGGSKKSGGK......",
        "......KGGK..KGGGGGGK..KGGK......",
        "......KSGK..KGGGGGGK..KGSK......",
        ".......KKK..KKKKKKKK..KKK......."
    ]

    private static func rowsFor(_ mood: Mood) -> [String] {
        var r = base
        switch mood {
        case .awake:
            r[11] = "....KGLLLWEEGGGGGGGEEWGGGGGK...."
            r[12] = "....KGLLGEBEGGGGGGGEBEGGGGGK...."
            r[13] = "....KGGGGEEEGGGGGGGEEEGGGGGK...."
        case .pleased:
            r[11] = "....KGLLLKGKGGGGGGGKGKGGGGGK...."
            r[12] = "....KGLLGGKGGGGGGGGGKGGGGGGK...."
            r[19] = "......KGGGGGGKKKKKKGGGGGGK......"
            r[20] = "......KGGGGGKGGGGGGKGGGGGK......"
        case .resting:
            r[12] = "....KGLLGKKKGGGGGGGKKKGGGGGK...."
        case .letDown:
            r[10] = "....KGLLLKKKGGGGGGGKKKGGGGGK...."
            r[11] = "....KGLLLWEEGGGGGGGEEWGGGGGK...."
            r[12] = "....KGLLGEBEGGGGGGGEBEGGGGGK...."
            r[13] = "....KGGGGEEEGGGGGGGEEEGGGGGK...."
            r[18] = "......KGGGGGKMMMMMMKGGGGGK......"
            r[19] = "......KGGGGGGKKKKKKGGGGGGK......"
        }
        return r
    }

    private static func colorOf(_ ch: Character) -> ARGB {
        switch ch {
        case "K": return 0xFF12120F   // outline
        case "L": return 0xFFA8A89E   // coat, lit side
        case "G": return 0xFF8A8A80   // coat
        case "S": return 0xFF6E6E66   // coat, shaded rim
        case "M": return 0xFFE4E1D4   // muzzle
        case "P": return 0xFFE0A3A3   // ear pink, nose
        case "E": return 0xFFCFE85F   // eye — carries the brand colour
        case "B": return 0xFF12120F   // pupil
        case "W": return 0xFFF4F2EA   // catchlight
        default: return 0
        }
    }

    /// Width this cat occupies when each pixel is `px` wide.
    static func widthFor(_ px: CGFloat) -> CGFloat { CGFloat(cols) * px }

    /// Height this cat occupies when each pixel is `px` wide.
    static func heightFor(_ px: CGFloat) -> CGFloat { CGFloat(rows) * px }

    /**
     Draws Mochi with his top-left at (`left`, `top`), one bitmap pixel per
     `px` points. Pixels are drawn a hair oversized so no seams show between
     them at fractional scales.
     */
    static func draw(in ctx: CGContext, left: CGFloat, top: CGFloat, px: CGFloat, mood: Mood) {
        let bleed: CGFloat = 0.5
        let grid = rowsFor(mood)
        for y in 0..<rows {
            let row = Array(grid[y])
            for x in 0..<cols {
                let c = colorOf(row[x])
                if c == 0 { continue }
                ctx.setFillColor(c.cgColor)
                ctx.fill(CGRect(
                    x: left + CGFloat(x) * px,
                    y: top + CGFloat(y) * px,
                    width: px + bleed,
                    height: px + bleed
                ))
            }
        }
    }

    /// A standalone image of Mochi, for the places SwiftUI wants one.
    static func image(px: CGFloat, mood: Mood) -> UIImage {
        let size = CGSize(width: widthFor(px), height: heightFor(px))
        return UIGraphicsImageRenderer(size: size).image { ctx in
            draw(in: ctx.cgContext, left: 0, top: 0, px: px, mood: mood)
        }
    }

    /// The mood the app should show given today's state and the running streak.
    static func moodFor(doneToday: Bool, streak: Int, missedYesterday: Bool) -> Mood {
        if doneToday { return .pleased }
        if missedYesterday && streak == 0 { return .letDown }
        return .awake
    }
}
