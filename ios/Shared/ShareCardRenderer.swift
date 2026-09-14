import CoreGraphics
import UIKit

/**
 The 1080x1920 image that goes to an Instagram story.

 It has to survive being seen for two seconds at thumb size, so it carries one
 number, one grid, and the cat — nothing else. Everything is sized off the
 canvas width, so the same code makes a square post if we ever need one.
 */
enum ShareCardRenderer {

    static let storyW: CGFloat = 1080
    static let storyH: CGFloat = 1920

    static func render(model: SlabModel,
                       width: CGFloat = storyW,
                       height: CGFloat = storyH) -> UIImage {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1              // the card is already at its final pixel size
        format.opaque = true
        return UIGraphicsImageRenderer(size: CGSize(width: width, height: height), format: format)
            .image { ctx in
                draw(in: ctx.cgContext, w: width, h: height, model: model)
            }
    }

    private static func draw(in ctx: CGContext, w: CGFloat, h: CGFloat, model: SlabModel) {
        let u = w / 1080
        func s(_ v: CGFloat) -> CGFloat { v * u }

        ctx.textMatrix = CGAffineTransform(scaleX: 1, y: -1)

        let accent = model.accent

        ctx.setFillColor(Palette.cream.cgColor)
        ctx.fill(CGRect(x: 0, y: 0, width: w, height: h))

        // A single colour block anchors the composition; the story's safe area
        // keeps clear of Instagram's own chrome top and bottom.
        let margin = s(84)
        let blockTop = h * 0.235
        let block = CGRect(x: margin, y: blockTop, width: w - margin * 2, height: s(990))
        fill(ctx, block, s(64), accent.block)

        let onBlock = accent.onBlock
        let pad = s(64)

        // ── Ritual name ─────────────────────────────────────────────────────
        let slot = TypeSpec(s(30), onBlock.withAlpha(160), Fonts.semiBold(s(30)), 0.10)
        SlabRenderer.text(ctx, model.slot.uppercased(),
                          at: CGPoint(x: block.minX + pad, y: block.minY + pad + s(26)), spec: slot)

        let nameSpec = TypeSpec(s(76), onBlock, Fonts.extraBold(s(76)), -0.03)
        let name = SlabRenderer.fit(model.title, spec: nameSpec, maxWidth: block.width - pad * 2)
        SlabRenderer.text(ctx, name,
                          at: CGPoint(x: block.minX + pad, y: block.minY + pad + s(110)), spec: nameSpec)

        // ── The number that matters ─────────────────────────────────────────
        let big = TypeSpec(s(300), onBlock, Fonts.extraBold(s(300)), -0.05)
        SlabRenderer.text(ctx, "\(model.streak)",
                          at: CGPoint(x: block.minX + pad - s(8), y: block.minY + s(430)), spec: big)

        let unit = TypeSpec(s(38), onBlock.withAlpha(190), Fonts.extraBold(s(38)), 0.02)
        SlabRenderer.text(ctx, model.streak == 1 ? "DAY IN A ROW" : "DAYS IN A ROW",
                          at: CGPoint(x: block.minX + pad, y: block.minY + s(492)), spec: unit)

        // ── Mochi ───────────────────────────────────────────────────────────
        let catH = s(176)
        let catW = Cat.widthFor(catH)
        let catBox = CGRect(
            x: block.maxX - pad - catW - s(38),
            y: block.minY + s(250),
            width: catW + s(38) + s(2),
            height: catH + s(38)
        )
        fill(ctx, catBox, s(34), model.doneToday ? Palette.ink : Palette.paper)
        Cat.draw(in: ctx, left: catBox.midX - catW / 2, top: catBox.midY - catH / 2, height: catH, mood: model.mood)

        // ── The year ────────────────────────────────────────────────────────
        let cols = GridGeo.colsFor(model.year)
        let gapRatio: CGFloat = 0.30
        let gridW = block.width - pad * 2
        let cell = gridW / (CGFloat(cols) + gapRatio * CGFloat(cols - 1))
        let gap = cell * gapRatio
        let gridTop = block.minY + s(620)
        let startOff = GridGeo.startOffset(model.year)
        let yearLen = GridGeo.lengthOf(model.year)
        let todayDoy = model.today.year == model.year ? model.today.dayOfYear : -1

        let r = cell * 0.30
        for col in 0..<cols {
            for row in 0..<GridGeo.rows {
                let doy = col * GridGeo.rows + row - startOff + 1
                if doy < 1 || doy > yearLen { continue }
                let rect = CGRect(
                    x: block.minX + pad + CGFloat(col) * (cell + gap),
                    y: gridTop + CGFloat(row) * (cell + gap),
                    width: cell,
                    height: cell
                )
                let colour: ARGB
                if model.doneDaysOfYear.contains(doy) {
                    colour = Palette.ink
                } else if todayDoy > 0 && doy > todayDoy {
                    colour = onBlock.withAlpha(23)
                } else {
                    colour = onBlock.withAlpha(52)
                }
                fill(ctx, rect, r, colour)
            }
        }

        let gridBottom = gridTop + CGFloat(GridGeo.rows) * cell + CGFloat(GridGeo.rows - 1) * gap

        // ── Tally ───────────────────────────────────────────────────────────
        ctx.setFillColor(onBlock.withAlpha(46).cgColor)
        ctx.fill(CGRect(x: block.minX + pad, y: gridBottom + s(56),
                        width: block.width - pad * 2, height: s(4)))

        let tallyY = gridBottom + s(150)
        let figure = TypeSpec(s(64), onBlock, Fonts.extraBold(s(64)), -0.02)
        let caption = TypeSpec(s(26), onBlock.withAlpha(160), Fonts.semiBold(s(26)), 0.10)

        SlabRenderer.text(ctx, "\(model.totalDone)", at: CGPoint(x: block.minX + pad, y: tallyY), spec: figure)
        SlabRenderer.text(ctx, "LIT IN \(model.year)",
                          at: CGPoint(x: block.minX + pad, y: tallyY + s(42)), spec: caption)

        let midX = block.minX + block.width * 0.5
        SlabRenderer.text(ctx, "\(model.remaining)", at: CGPoint(x: midX, y: tallyY), spec: figure)
        SlabRenderer.text(ctx, "SQUARES LEFT", at: CGPoint(x: midX, y: tallyY + s(42)), spec: caption)

        // ── Wordmark ────────────────────────────────────────────────────────
        let mark = TypeSpec(s(34), Palette.ink, Fonts.extraBold(s(34)), -0.01)
        SlabRenderer.text(ctx, "RITUAL", at: CGPoint(x: w / 2, y: block.maxY + s(110)),
                          spec: mark, align: .center)

        let sub = TypeSpec(s(24), Palette.ink.withAlpha(130), Fonts.semiBold(s(24)), 0.14)
        SlabRenderer.text(ctx, "ONE SQUARE A DAY", at: CGPoint(x: w / 2, y: block.maxY + s(152)),
                          spec: sub, align: .center)
    }

    private static func fill(_ ctx: CGContext, _ rect: CGRect, _ radius: CGFloat, _ color: ARGB) {
        ctx.setFillColor(color.cgColor)
        ctx.addPath(CGPath(roundedRect: rect, cornerWidth: radius, cornerHeight: radius, transform: nil))
        ctx.fillPath()
    }
}
