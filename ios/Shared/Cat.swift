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
 The 20x18 original is three parts, and they are the character: ears, a wide
 rounded head, and a narrower neck under it. 32x28 is 1.6x of that, so every
 landmark lands where it always did.

 Below the cheeks the silhouette's width eases into the neck on a cosine, which
 has zero slope at both ends — so the jaw leaves the cheek and meets the neck
 as one curve, rather than the flat shelf and square corner a union of two
 rectangles gives you. The ears come to a point, one pixel at the tip.

 The extra pixels go into shading, the inside of the ears, blushed cheeks, and
 eyes big enough to be cute — set below the midline of the head, which is the
 whole trick — and never into changing the shape. The rule the original was
 built on is unchanged, and it is the rule that matters: one silhouette, and
 only the eye and mouth rows move.
 */
enum Cat {

    static let cols = 32
    static let rows = 28

    private static let base = [
        "................................",
        ".......K................K.......",
        "......KPK..............KPK......",
        ".....KPPPK............KPPPK.....",
        ".....KPPPPKKKKKKKKKKKKPPPPK.....",
        ".....KPPPPGGGGGGGGGGGGPPPPK.....",
        "....KGGLLLLLLLLLLLLLGGGGGGGK....",
        "...KGGLLLLLLLLLLLLLGGGGGGGGSK...",
        "..KGGLLLLLLLLLLLLGGGGGGGGGGGSK..",
        "..KGLLLLLLLLLLLLGGGGGGGGGGGGGK..",
        "..KGLLLLLLLLLLLGGGGGGGGGGGGGGK..",
        "..KGLLLLLLLLLLGGGGGGGGGGGGGGGK..",
        "..KGLLLLLLLLGGGGGGGGGGGGGGGGGK..",
        "..KGLLLLLLLGGGGGGGGGGGGGGGGGGK..",
        "..KGLLLLLLGGGGGGGGGGGGGGGGGGGK..",
        "..KGLLLLGGGGGGGGGGGGGGGGGGGGGK..",
        "..KGGHHHGGGGGGGGGGGGGGGGHHHGSK..",
        "...KGHHHGGGGGGGGGGGGGGGGHHHSK...",
        "....KGGGGGGGGGPPPPGGGGGGGGSK....",
        ".....KKGGGGGGGGPPGGGGGGGSKK.....",
        ".......KGGGGGGGGGGGGGGGSK.......",
        "........KSGGGGGGGGGGGGSK........",
        ".........KGGGGGGGGGGGGK.........",
        ".........KGGGGGGGGGGGGK.........",
        ".........KGGGGGGGGGGGGK.........",
        ".........KGGGGGGGGGGGGK.........",
        ".........KGGGGGGGGGGGGK.........",
        ".........KKKKKKKKKKKKKK........."
    ]

    private static func rowsFor(_ mood: Mood) -> [String] {
        var r = base
        switch mood {
        case .awake:
            r[12] = "..KGLLLLEEEEEGGGGGGEEEEEGGGGGK.."
            r[13] = "..KGLLLLEBBEEGGGGGGEBBEEGGGGGK.."
            r[14] = "..KGLLLLEBBEEGGGGGGEBBEEGGGGGK.."
            r[15] = "..KGLLLLWEEEEGGGGGGWEEEEGGGGGK.."
            r[16] = "..KGGHHHGEEEGGGGGGGGEEEGHHHGSK.."
        case .pleased:
            r[13] = "..KGLLLLLKKKGGGGGGGGKKKGGGGGGK.."
            r[14] = "..KGLLLKKLGGKKGGGGKKGGGKKGGGGK.."
            r[20] = ".......KGGGGGKGGGGKGGGGSK......."
            r[21] = "........KSGGGGKKKKGGGGSK........"
        case .resting:
            r[14] = "..KGLLLLKKKKKGGGGGGKKKKKGGGGGK.."
            r[15] = "..KGLLLLGKKGGGGGGGGGKKGGGGGGGK.."
        case .letDown:
            r[10] = "..KGLLKKLLLLLLLGGGGGGGGGKKGGGK.."
            r[11] = "..KGLLLLLKKKKLGGGGGKKKKGGGGGGK.."
            r[13] = "..KGLLLLEBBEEGGGGGGEBBEEGGGGGK.."
            r[14] = "..KGLLLLEBBEEGGGGGGEBBEEGGGGGK.."
            r[15] = "..KGLLLLEEEEEGGGGGGEEEEEGGGGGK.."
            r[20] = ".......KGGGGGGKKKKGGGGGSK......."
            r[21] = "........KSGGGKGGGGKGGGSK........"
        }
        return r
    }

    private static func colorOf(_ ch: Character) -> ARGB {
        switch ch {
        case "K": return 0xFF12120F   // outline
        case "L": return 0xFFA8A89E   // coat, lit side
        case "G": return 0xFF8A8A80   // coat
        case "S": return 0xFF6E6E66   // coat, shaded rim
        case "P": return 0xFFE0A3A3   // ear pink, nose
        case "H": return 0xFFD69B96   // blushed cheek
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
