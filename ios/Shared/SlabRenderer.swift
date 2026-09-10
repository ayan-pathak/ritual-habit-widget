import CoreGraphics
import CoreText
import UIKit

/// Everything the renderer needs, with no dependency on how it was stored.
struct SlabModel: Equatable {
    var title: String
    var slot: String
    var accent: Accent
    var year: Int
    var today: DayDate
    var doneDaysOfYear: Set<Int>
    var streak: Int
    var totalDone: Int
    var remaining: Int
    var doneToday: Bool
    var mood: Mood = .awake
}

/// One text style: a face at a size, a colour, and tracking measured in ems.
struct TypeSpec {
    let font: UIFont
    let color: ARGB
    let kern: CGFloat

    init(_ size: CGFloat, _ color: ARGB, _ font: UIFont, _ tracking: CGFloat) {
        self.font = font
        self.color = color
        self.kern = tracking * size
    }
}

/**
 Draws the card: a flat colour block carrying one year of days.

 The same routine backs the home-screen widget and every card inside the app,
 so the two can never drift apart. Marking today inverts the block to ink and
 the grid to the ritual's colour — that flip is the whole visual reward.

 Sizes arrive in points rather than pixels, so `density` is 1 on iOS where the
 Android build passes the screen's dp scale; the numbers below are the same
 numbers either way.
 */
enum SlabRenderer {

    struct Config {
        var density: CGFloat = 1
        var header = true
        var footer = true
        var action = false
        var cat = true
        var quarterRuler = false
        var corner: CGFloat = 24
        var pad: CGFloat = 17
        var fillBackground = true
    }

    static let actionHeight: CGFloat = 34

    static func render(size: CGSize, model: SlabModel, config: Config = Config()) -> UIImage {
        let w = max(size.width, 1)
        let h = max(size.height, 1)
        return UIGraphicsImageRenderer(size: CGSize(width: w, height: h)).image { context in
            draw(in: context.cgContext, width: w, height: h, model: model, config: config)
        }
    }

    static func draw(in ctx: CGContext, width w: CGFloat, height h: CGFloat,
                     model: SlabModel, config cfg: Config) {

        let d = cfg.density
        func dp(_ v: CGFloat) -> CGFloat { v * d }

        ctx.textMatrix = CGAffineTransform(scaleX: 1, y: -1)

        let pad = dp(cfg.pad)
        let corner = dp(cfg.corner)
        let card = CGRect(x: 0, y: 0, width: w, height: h)

        // Marked days flip the card to ink and light the grid in the ritual's
        // colour, so a glance across the home screen says what is still open.
        let lit = model.doneToday
        let block = lit ? Palette.ink : model.accent.block
        let onBlock = lit ? Palette.paper : model.accent.onBlock
        let markInk = lit ? model.accent.block : Palette.ink
        let missed = onBlock.withAlpha(52)
        let future = onBlock.withAlpha(23)

        if cfg.fillBackground {
            fill(ctx, rect: card, radius: corner, color: block)
        }

        // ── Layout ──────────────────────────────────────────────────────────
        let headerH = cfg.header ? dp(38) : 0
        let footerH = cfg.footer ? dp(22) : 0
        let actionH = cfg.action ? dp(actionHeight) : 0

        var bottom = h - pad
        if cfg.action || cfg.footer {
            bottom -= max(actionH, footerH)
        }

        let gridTopBound = pad + headerH + dp(6)
        let gridBottomBound = max(bottom - dp(10), gridTopBound + dp(14))

        let cols = GridGeo.colsFor(model.year)
        let gapRatio: CGFloat = 0.30
        let innerW = w - 2 * pad
        var cell = innerW / (CGFloat(cols) + gapRatio * CGFloat(cols - 1))
        var gap = cell * gapRatio
        var gridH = CGFloat(GridGeo.rows) * cell + CGFloat(GridGeo.rows - 1) * gap

        let zoneH = gridBottomBound - gridTopBound
        if gridH > zoneH {
            cell = zoneH / (CGFloat(GridGeo.rows) + gapRatio * CGFloat(GridGeo.rows - 1))
            gap = cell * gapRatio
            gridH = CGFloat(GridGeo.rows) * cell + CGFloat(GridGeo.rows - 1) * gap
        }
        let gridLeft = pad
        let gridTop = gridTopBound + max((zoneH - gridH) / 2, 0)

        let yearLen = GridGeo.lengthOf(model.year)
        let startOff = GridGeo.startOffset(model.year)
        let isCurrentYear = model.today.year == model.year
        let todayDoy = isCurrentYear ? model.today.dayOfYear : -1

        // ── Header ──────────────────────────────────────────────────────────
        if cfg.header {
            var textRight = w - pad

            if cfg.cat {
                let px = dp(0.96)
                let catW = Cat.widthFor(px)
                let catH = Cat.heightFor(px)
                let boxW = catW + dp(11)
                let boxH = catH + dp(9)
                let boxL = w - pad - boxW
                let box = CGRect(x: boxL, y: pad - dp(2), width: boxW, height: boxH)
                fill(ctx, rect: box, radius: dp(10), color: lit ? model.accent.block : Palette.ink)
                Cat.draw(
                    in: ctx,
                    left: box.midX - catW / 2,
                    top: box.midY - catH / 2,
                    px: px,
                    mood: model.mood
                )
                textRight = boxL - dp(10)
            } else {
                // No cat: the streak takes the corner instead.
                let num = TypeSpec(dp(17), onBlock, Fonts.extraBold(dp(17)), -0.02)
                text(ctx, "\(model.streak)", at: CGPoint(x: w - pad, y: pad + dp(15)), spec: num, align: .right)
                let cap = TypeSpec(dp(8), onBlock.withAlpha(150), Fonts.semiBold(dp(8)), 0.10)
                text(ctx, "DAY STREAK", at: CGPoint(x: w - pad, y: pad + dp(26)), spec: cap, align: .right)
                textRight = w - pad - dp(58)
            }

            let slotSpec = TypeSpec(dp(8.5), onBlock.withAlpha(150), Fonts.semiBold(dp(8.5)), 0.10)
            text(ctx, model.slot.uppercased(), at: CGPoint(x: pad, y: pad + dp(8)), spec: slotSpec)

            let nameSpec = TypeSpec(dp(17), onBlock, Fonts.extraBold(dp(17)), -0.02)
            let name = fit(model.title, spec: nameSpec, maxWidth: max(textRight - pad, dp(30)))
            text(ctx, name, at: CGPoint(x: pad, y: pad + dp(27)), spec: nameSpec)

            if cfg.cat {
                let streakSpec = TypeSpec(dp(8.5), onBlock.withAlpha(170), Fonts.semiBold(dp(8.5)), 0.10)
                text(ctx, "\(model.streak) DAY STREAK",
                     at: CGPoint(x: pad, y: pad + dp(37)), spec: streakSpec)
            }
        }

        // ── Quarter ruler ───────────────────────────────────────────────────
        if cfg.quarterRuler {
            let spec = TypeSpec(dp(8), onBlock.withAlpha(140), Fonts.semiBold(dp(8)), 0.10)
            for (month, label) in [(1, "JAN"), (4, "APR"), (7, "JUL"), (10, "OCT")] {
                let col = GridGeo.colOf(DayDate(year: model.year, month: month, day: 1))
                text(ctx, label,
                     at: CGPoint(x: gridLeft + CGFloat(col) * (cell + gap), y: gridTop - dp(5)),
                     spec: spec)
            }
        }

        // ── The field ───────────────────────────────────────────────────────
        let r = cell * 0.30
        for col in 0..<cols {
            for row in 0..<GridGeo.rows {
                let doy = col * GridGeo.rows + row - startOff + 1
                if doy < 1 || doy > yearLen { continue }
                let rect = CGRect(
                    x: gridLeft + CGFloat(col) * (cell + gap),
                    y: gridTop + CGFloat(row) * (cell + gap),
                    width: cell,
                    height: cell
                )

                let colour: ARGB
                if model.doneDaysOfYear.contains(doy) {
                    colour = markInk
                } else if isCurrentYear && doy > todayDoy {
                    colour = future
                } else {
                    colour = missed
                }
                fill(ctx, rect: rect, radius: r, color: colour)

                if doy == todayDoy {
                    // Today wears a ring whether or not it is filled.
                    let ring = rect.insetBy(dx: -dp(1.3), dy: -dp(1.3))
                    ctx.setStrokeColor(markInk.cgColor)
                    ctx.setLineWidth(max(dp(1.2), 1.3))
                    ctx.addPath(CGPath(roundedRect: ring, cornerWidth: r * 1.7, cornerHeight: r * 1.7, transform: nil))
                    ctx.strokePath()
                }
            }
        }

        // ── Footer ──────────────────────────────────────────────────────────
        if cfg.footer || cfg.action {
            let baseY = h - pad - dp(4)

            let litSpec = TypeSpec(dp(12), onBlock, Fonts.extraBold(dp(12)), 0)
            let litText = "\(model.totalDone) lit"
            text(ctx, litText, at: CGPoint(x: pad, y: baseY), spec: litSpec)
            var x = pad + width(litText, spec: litSpec) + dp(9)

            ctx.setFillColor(onBlock.withAlpha(90).cgColor)
            ctx.fillEllipse(in: CGRect(
                x: x + dp(2) - dp(2), y: baseY - dp(3.5) - dp(2),
                width: dp(4), height: dp(4)
            ))
            x += dp(13)

            let leftSpec = TypeSpec(dp(12), onBlock.withAlpha(160), Fonts.semiBold(dp(12)), 0)
            text(ctx, "\(model.remaining) to go", at: CGPoint(x: x, y: baseY), spec: leftSpec)

            if cfg.action {
                drawAction(ctx, w: w, h: h, pad: pad, model: model, lit: lit, d: d)
            }
        }
    }

    /// Where the mark-today control sits. The widget lays its button over this.
    static func actionRect(width w: CGFloat, height h: CGFloat,
                           model: SlabModel, config cfg: Config) -> CGRect {
        let d = cfg.density
        let pad = cfg.pad * d
        let hgt = actionHeight * d
        let lit = model.doneToday
        let label = lit ? "Done" : "Mark"
        let spec = TypeSpec(11.5 * d, lit ? Palette.ink : Palette.paper, Fonts.extraBold(11.5 * d), 0)
        let checkW = lit ? 15 * d : 0
        let wide = width(label, spec: spec) + 28 * d + checkW
        return CGRect(x: w - pad - wide, y: h - pad - hgt, width: wide, height: hgt)
    }

    private static func drawAction(_ ctx: CGContext, w: CGFloat, h: CGFloat, pad: CGFloat,
                                   model: SlabModel, lit: Bool, d: CGFloat) {
        func dp(_ v: CGFloat) -> CGFloat { v * d }
        let hgt = dp(actionHeight)
        let label = lit ? "Done" : "Mark"
        let spec = TypeSpec(dp(11.5), lit ? Palette.ink : Palette.paper, Fonts.extraBold(dp(11.5)), 0)
        let checkW = lit ? dp(15) : 0
        let labelW = width(label, spec: spec)
        let wide = labelW + dp(28) + checkW
        let right = w - pad
        let r = CGRect(x: right - wide, y: h - pad - hgt, width: wide, height: hgt)

        fill(ctx, rect: r, radius: hgt / 2, color: lit ? model.accent.block : Palette.ink)

        let textX = r.midX - labelW / 2 + checkW / 2
        text(ctx, label, at: CGPoint(x: textX, y: r.midY + dp(4)), spec: spec)

        if lit {
            // Hand-drawn check: no font dependency, crisp at any size.
            let cx = textX - dp(9)
            let cy = r.midY
            let s = dp(3.6)
            ctx.setStrokeColor(Palette.ink.cgColor)
            ctx.setLineWidth(max(dp(1.9), 2))
            ctx.setLineCap(.round)
            ctx.setLineJoin(.round)
            ctx.beginPath()
            ctx.move(to: CGPoint(x: cx - s, y: cy + s * 0.05))
            ctx.addLine(to: CGPoint(x: cx - s * 0.15, y: cy + s * 0.72))
            ctx.addLine(to: CGPoint(x: cx + s, y: cy - s * 0.72))
            ctx.strokePath()
        }
    }

    // ── Drawing primitives ──────────────────────────────────────────────────

    enum Align { case left, right, center }

    private static func fill(_ ctx: CGContext, rect: CGRect, radius: CGFloat, color: ARGB) {
        ctx.setFillColor(color.cgColor)
        ctx.addPath(CGPath(roundedRect: rect, cornerWidth: radius, cornerHeight: radius, transform: nil))
        ctx.fillPath()
    }

    private static func attributed(_ string: String, _ spec: TypeSpec) -> NSAttributedString {
        NSAttributedString(string: string, attributes: [
            .font: spec.font,
            .foregroundColor: spec.color.uiColor,
            .kern: spec.kern
        ])
    }

    static func width(_ string: String, spec: TypeSpec) -> CGFloat {
        let line = CTLineCreateWithAttributedString(attributed(string, spec))
        return CGFloat(CTLineGetTypographicBounds(line, nil, nil, nil))
    }

    /// Draws `string` with its baseline at `point`, the way Canvas.drawText does.
    static func text(_ ctx: CGContext, _ string: String, at point: CGPoint,
                     spec: TypeSpec, align: Align = .left) {
        let line = CTLineCreateWithAttributedString(attributed(string, spec))
        var x = point.x
        switch align {
        case .left: break
        case .right: x -= CGFloat(CTLineGetTypographicBounds(line, nil, nil, nil))
        case .center: x -= CGFloat(CTLineGetTypographicBounds(line, nil, nil, nil)) / 2
        }
        ctx.textPosition = CGPoint(x: x, y: point.y)
        CTLineDraw(line, ctx)
    }

    /// Truncates `string` to `maxWidth`, adding an ellipsis only when it overflows.
    static func fit(_ string: String, spec: TypeSpec, maxWidth: CGFloat) -> String {
        if width(string, spec: spec) <= maxWidth { return string }
        var end = string.count
        while end > 1 && width(String(string.prefix(end)) + "…", spec: spec) > maxWidth {
            end -= 1
        }
        return String(string.prefix(max(end, 1))) + "…"
    }
}
