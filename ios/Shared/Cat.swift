import CoreGraphics
import UIKit

/// What Mochi is doing, derived from the streak — never chosen for decoration.
enum Mood {
    case awake, pleased, resting, letDown
}

/**
 Mochi, a 20x18 bitmap.

 Only the eye and mouth rows change between moods, so the silhouette never
 shifts — that is what keeps a pixel mascot from looking redrawn each time.
 */
enum Cat {

    static let cols = 20
    static let rows = 18

    private static let base = [
        "....KK........KK....",
        "...KDGK......KGDK...",
        "...KGPK......KPGK...",
        "..KGGPGKKKKKKGPGGK..",
        "..KGGGGGGGGGGGGGGK..",
        ".KGGGGGGGGGGGGGGGGK.",
        ".KGGGGGGGGGGGGGGGGK.",
        ".KGEEEGGGGGGGGEEEGK.",
        ".KGEBEGGGGGGGGEBEGK.",
        ".KGEEEGGGGGGGGEEEGK.",
        ".KGGGGGGGPPGGGGGGGK.",
        ".KGGGGGGKPPKGGGGGGK.",
        "..KGGGGGGKKGGGGGGK..",
        "..KKGGGGGGGGGGGGKK..",
        "....KGGGGGGGGGGK....",
        "....KGGGGGGGGGGK....",
        "....KGGGGGGGGGGK....",
        "....KKKKKKKKKKKK...."
    ]

    private static func rowsFor(_ mood: Mood) -> [String] {
        var r = base
        switch mood {
        case .awake:
            break
        case .pleased:
            r[7] = ".KGGKGGGGGGGGGGKGGK."
            r[8] = ".KGKGKGGGGGGGGKGKGK."
            r[9] = ".KGGGGGGGGGGGGGGGGK."
            r[11] = ".KGGGGGKKPPKKGGGGGK."
        case .resting:
            r[7] = ".KGGGGGGGGGGGGGGGGK."
            r[8] = ".KGKKKGGGGGGGGKKKGK."
            r[9] = ".KGGGGGGGGGGGGGGGGK."
        case .letDown:
            r[7] = ".KGKGKGGGGGGGGKGKGK."
            r[8] = ".KGGKGGGGGGGGGGKGGK."
            r[9] = ".KGGGGGGGGGGGGGGGGK."
            r[12] = "..KGGGGKGGGGKGGGGK.."
        }
        return r
    }

    private static func colorOf(_ ch: Character) -> ARGB {
        switch ch {
        case "K": return 0xFF12120F   // outline
        case "D": return 0xFF56564E   // ear shadow
        case "G": return 0xFF8A8A80   // coat
        case "P": return 0xFFE0A3A3   // ear pink, nose
        case "E": return 0xFFCFE85F   // eye — carries the brand colour
        case "B": return 0xFF12120F   // pupil
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
