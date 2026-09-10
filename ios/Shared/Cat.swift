import CoreGraphics
import UIKit

/// What Mochi is doing, derived from the streak - never chosen for decoration.
enum Mood {
    case awake, pleased, resting, letDown
}

/**
 Mochi, a 40x32 bitmap.

 Only the brow, eye and mouth rows change between moods, so the silhouette
 never shifts - that is what keeps a pixel mascot from looking redrawn each
 time. The 20x18 original is three parts, and they are the character: ears, a
 wide rounded head, and a narrower neck under it. Nothing here changes that.

 The head is an ellipse biased low with a gaussian bulge at cheek level, so the
 face is widest where the muzzle is and the line curves *under* it into the
 chin. A rounded rectangle chamfers there; this does not, which is the whole
 difference between a puffy cheek and a hard jaw. The ears taper to a single
 pixel at the tip.

 The extra pixels go into detail and never into changing the shape: an iris
 with a dark rim, a lit arc along its bottom and a catchlight; blush that falls
 off into the coat rather than ending on a hard edge; a fan of three whiskers
 either side, drawn in mid grey so they read on the accent block and on ink
 alike; ears with a pink inner held a pixel off the outer rim. The rule the
 original was built on is unchanged, and it is the rule that matters: one
 silhouette, and only the face moves.
 */
enum Cat {

    static let cols = 40
    static let rows = 32

    private static let base = [
        "...........K................K...........",
        "..........KLK..............KLK..........",
        "..........KPK.....KKKK.....KPK..........",
        ".........KLPLKKKKKLLLLKKKKKGPGK.........",
        ".........KPPPLLLLLGGGGLLLGGPPPK.........",
        "........KPPPPPGGGGGGGGGGGGPPPPPK........",
        ".......KLLLLLLGGGGGGGGGGGGLLLLLSK.......",
        ".......KKLLLLLLGGGGGGGGGGLLLLLLKK.......",
        ".........KLGGGGGGGGGGGGGGGGGGGK.........",
        ".........KLGGGGGGGGGGGGGGGGGGGK.........",
        ".........KLGGGGGGGGGGGGGGGGGGGK.........",
        "........KLGGGGGGGGGGGGGGGGGGGGSK........",
        "........KLGGGGGGGGGGGGGGGGGGGGGK........",
        ".......KLRRRGGGGGGGGGGGGGGGGRRRSK.......",
        ".......KHHHHRGGGGGGGGGGGGGGRHHHHK.......",
        "..S...KRHHHHHGGGGGGGGGGGGGGHHHHHRK....S.",
        "..SSS.KRHHHHRGGGGGKKKKGGGGGRHHHHRK.SSS..",
        "....SSKGGRRRGGGGGHPPPPHGGGGGRRRGGKS.....",
        "...SSSKGGGGGGGGGMMMPPMMMGGGGGGGGSKSSS...",
        ".SSS...KGGGGGGGMMMMMMMMMMGGGGGGSK...SSS.",
        ".....SS.KGGGGGGGMMMMMMMMGGGGGGSKSSS.....",
        "...SS....KSGGGGGGMMMMMMGGGGGGSK....SS...",
        "..S.......KKSGGGGGGGGGGGGGGSKK.......S..",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KKKKKKKKKKKKKKKK............"
    ]

    private static func rowsFor(_ mood: Mood) -> [String] {
        var r = base
        switch mood {
        case .awake:
            r[8] = ".........KLGKKKKKGGGGGGKKKKKGGK........."
            r[9] = ".........KLKDEEEDKGGGGKDEEEDKGK........."
            r[10] = ".........KLKEWBBEKGGGGKEWBBEKGK........."
            r[11] = "........KLGKEBBBEKGGGGKEBBBEKGSK........"
            r[12] = "........KLGKDAAADKGGGGKDAAADKGGK........"
            r[13] = ".......KLRRRKKKKKGGGGGGKKKKKRRRSK......."
            r[19] = ".SSS...KGGGGGGGMMMKMMKMMMGGGGGGSK...SSS."
            r[20] = ".....SS.KGGGGGGGMMMKKMMMGGGGGGSKSSS....."
        case .pleased:
            r[10] = ".........KLGGKKKGGGGGGGGKKKGGGK........."
            r[11] = "........KLGGKGGGKGGGGGGKGGGKGGSK........"
            r[12] = "........KLGKKGGGKKGGGGKKGGGKKGGK........"
            r[19] = ".SSS...KGGGGGGGMMKKMMKKMMGGGGGGSK...SSS."
            r[20] = ".....SS.KGGGGGGGMMMKKMMMGGGGGGSKSSS....."
        case .resting:
            r[11] = "........KLGKKKKKKKGGGGKKKKKKKGSK........"
            r[12] = "........KLGGKKKKKGGGGGGKKKKKGGGK........"
        case .letDown:
            r[5] = "........KPPPPPGKKKGGGGGKKKPPPPPK........"
            r[6] = ".......KLLLLLKKKGGGGGGGGGKKKLLLSK......."
            r[7] = ".......KKLLLKKKGGGGGGGGGGLKKKLLKK......."
            r[8] = ".........KLGKKKKKGGGGGGKKKKKGGK........."
            r[9] = ".........KLKKKKKKKGGGGKKKKKKKGK........."
            r[10] = ".........KLKEBBBEKGGGGKEBBBEKGK........."
            r[11] = "........KLGKEBBBEKGGGGKEBBBEKGSK........"
            r[12] = "........KLGKDAAADKGGGGKDAAADKGGK........"
            r[13] = ".......KLRRRKKKKKGGGGGGKKKKKRRRSK......."
            r[19] = ".SSS...KGGGGGGGMMMMKKMMMMGGGGGGSK...SSS."
            r[20] = ".....SS.KGGGGGGGMKKMMKKMGGGGGGSKSSS....."
        }
        return r
    }

    private static func colorOf(_ ch: Character) -> ARGB {
        switch ch {
        case "K": return 0xFF12120F   // outline
        case "L": return 0xFFA8A89E   // coat, lit rim
        case "G": return 0xFF8A8A80   // coat
        case "S": return 0xFF6E6E66   // coat, shaded rim - and the whiskers
        case "P": return 0xFFE0A3A3   // ear pink, nose
        case "H": return 0xFFD69B96   // blushed cheek
        case "R": return 0xFFAE968C   // blush, falling off into the coat
        case "M": return 0xFFE8E3D2   // muzzle
        case "E": return 0xFFC9F73F   // iris - carries the brand colour
        case "A": return 0xFFE6FC9C   // iris, lit along the bottom
        case "D": return 0xFF84A828   // iris, rim
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
