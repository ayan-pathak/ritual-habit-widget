import CoreGraphics
import UIKit

/// What Mochi is doing, derived from the streak — never chosen for decoration.
enum Mood {
    case awake, pleased, resting, letDown
}

/**
 Mochi, four drawn portraits.

 The four moods are one illustration with a different face, exported at a
 single registration: the head sits on the same pixels in every one, so the
 silhouette never shifts and he never looks redrawn between moods. That was
 the rule when he was a 20x18 grid and it is still the rule now.

 The art lives once, in the Android resources, and `bootstrap.sh` stages it
 into `Resources/` — the same arrangement Archivo has, and for the same
 reason: a second byte-identical copy is a copy that can drift. Both this
 target and the widget extension get their own bundle copy at build time.

 They are bitmaps rather than a vector because of the Android side of the
 port: a widget there can only be handed a `Bitmap`, so the app and the widget
 can only draw the same pixels if the source *is* pixels. Keeping iOS on the
 same four files is what keeps the two platforms from drifting.
 */
enum Cat {

    /// The exported art, in pixels. Every mood is this size and registered alike.
    static let artWidth: CGFloat = 480
    static let artHeight: CGFloat = 496

    /// Width over height, so a caller can size him from either one.
    static let aspect = artWidth / artHeight

    /// Width Mochi occupies when he is drawn `height` tall.
    static func widthFor(_ height: CGFloat) -> CGFloat { height * aspect }

    private static var cache: [String: UIImage] = [:]

    private static func name(_ mood: Mood) -> String {
        switch mood {
        case .awake: return "mochi_awake"
        case .pleased: return "mochi_pleased"
        case .resting: return "mochi_resting"
        case .letDown: return "mochi_let_down"
        }
    }

    /// The portrait for a mood, or nil if it did not ship — never a crash.
    static func image(_ mood: Mood) -> UIImage? {
        let key = name(mood)
        if let hit = cache[key] { return hit }
        guard let img = UIImage(named: key) else { return nil }
        cache[key] = img
        return img
    }

    /**
     Draws Mochi into the box whose top-left is (`left`, `top`) and whose
     height is `height`.
     */
    static func draw(in ctx: CGContext, left: CGFloat, top: CGFloat, height: CGFloat, mood: Mood) {
        guard let cg = image(mood)?.cgImage else { return }
        let rect = CGRect(x: left, y: top, width: widthFor(height), height: height)
        // The context is y-down here, as CoreGraphics images are not, so flip
        // about the rect rather than drawing him upside down.
        ctx.saveGState()
        ctx.translateBy(x: 0, y: rect.midY)
        ctx.scaleBy(x: 1, y: -1)
        ctx.translateBy(x: 0, y: -rect.midY)
        ctx.draw(cg, in: rect)
        ctx.restoreGState()
    }

    /// A standalone image of Mochi, for the places SwiftUI wants one.
    static func image(height: CGFloat, mood: Mood) -> UIImage {
        let size = CGSize(width: widthFor(height), height: height)
        return UIGraphicsImageRenderer(size: size).image { ctx in
            draw(in: ctx.cgContext, left: 0, top: 0, height: height, mood: mood)
        }
    }

    /// The mood the app should show given today's state and the running streak.
    static func moodFor(doneToday: Bool, streak: Int, missedYesterday: Bool) -> Mood {
        if doneToday { return .pleased }
        if missedYesterday && streak == 0 { return .letDown }
        return .awake
    }
}
