import CoreGraphics
import UIKit

/// What Mochi is doing, derived from the streak — never chosen for decoration.
enum Mood {
    case awake, pleased, resting, letDown
}

/**
 Mochi, four drawn portraits, as vectors.

 The four moods are one drawing with a different face. They come off a 2x2
 sheet, and `tools/mochi.py` windows each quadrant out of it and registers
 them against each other, so the head sits on the same coordinates in every
 mood: the silhouette never shifts and he never looks redrawn between them.
 That was the rule when he was a 20x18 grid and it is still the rule.

 The paths live in `MochiArt.swift`, generated from `art/mochi/*.svg` by that
 same script — so the shapes iOS draws and the shapes Android draws come out
 of one conversion and cannot drift.

 They are replayed rather than rasterised because he is drawn at sizes an
 order of magnitude apart — 30pt in the widget header, 176px on the story
 card — and a raster picked for one is wrong for the other.
 */
enum Cat {

    /// Width over height, so a caller can size him from either one.
    static let aspect = MochiArt.viewWidth / MochiArt.viewHeight

    /// Width Mochi occupies when he is drawn `height` tall.
    static func widthFor(_ height: CGFloat) -> CGFloat { height * aspect }

    // ── The art, parsed once ────────────────────────────────────────────────

    private struct Item {
        let path: CGPath
        let alpha: CGFloat
        let solid: CGColor?
        let gradient: CGGradient?
        let start: CGPoint
        let end: CGPoint
    }

    private static var cache: [String: [Item]] = [:]
    private static let space = CGColorSpaceCreateDeviceRGB()

    private static func color(_ hex: Substring) -> CGColor {
        let v = UInt32(hex, radix: 16) ?? 0
        return CGColor(colorSpace: space, components: [
            CGFloat((v >> 16) & 0xFF) / 255, CGFloat((v >> 8) & 0xFF) / 255,
            CGFloat(v & 0xFF) / 255, CGFloat((v >> 24) & 0xFF) / 255
        ])!
    }

    private static func source(_ mood: Mood) -> (String, String) {
        switch mood {
        case .awake: return ("awake", MochiArt.awake)
        case .pleased: return ("pleased", MochiArt.pleased)
        case .resting: return ("resting", MochiArt.resting)
        case .letDown: return ("letDown", MochiArt.letDown)
        }
    }

    private static func items(_ mood: Mood) -> [Item] {
        let (key, body) = source(mood)
        if let hit = cache[key] { return hit }
        var out: [Item] = []
        for line in body.split(separator: "\n", omittingEmptySubsequences: true) {
            let f = line.split(separator: "\t", omittingEmptySubsequences: false)
            guard let kind = f.first else { continue }
            if kind == "S", f.count >= 4 {
                out.append(Item(path: parse(f[3]), alpha: CGFloat(Double(f[2]) ?? 1),
                                solid: color(f[1]), gradient: nil,
                                start: .zero, end: .zero))
            } else if kind == "G", f.count >= 9 {
                let stops = [color(f[1]), color(f[2])] as CFArray
                out.append(Item(
                    path: parse(f[8]),
                    alpha: CGFloat(Double(f[7]) ?? 1),
                    solid: nil,
                    gradient: CGGradient(colorsSpace: space, colors: stops, locations: [0, 1]),
                    start: CGPoint(x: Double(f[3]) ?? 0, y: Double(f[4]) ?? 0),
                    end: CGPoint(x: Double(f[5]) ?? 0, y: Double(f[6]) ?? 0)))
            }
        }
        cache[key] = out
        return out
    }

    /**
     Absolute `M`/`L`/`C`/`Z` only, which is all the converter ever emits.
     Scanned over UTF-8 rather than through `String.Index`, because this runs
     over tens of thousands of numbers the first time a mood is drawn.
     */
    private static func parse(_ d: Substring) -> CGPath {
        let path = CGMutablePath()
        let b = Array(d.utf8)
        var i = 0
        var start = CGPoint.zero
        var pen = CGPoint.zero

        func number() -> CGFloat {
            while i < b.count, b[i] == 0x20 || b[i] == 0x2C { i += 1 }   // space, comma
            var sign: Double = 1
            if i < b.count, b[i] == 0x2D { sign = -1; i += 1 }           // '-'
            var whole: Double = 0
            while i < b.count, b[i] >= 0x30, b[i] <= 0x39 {
                whole = whole * 10 + Double(b[i] - 0x30); i += 1
            }
            if i < b.count, b[i] == 0x2E {                               // '.'
                i += 1
                var scale = 0.1
                while i < b.count, b[i] >= 0x30, b[i] <= 0x39 {
                    whole += Double(b[i] - 0x30) * scale
                    scale /= 10
                    i += 1
                }
            }
            return CGFloat(sign * whole)
        }

        while i < b.count {
            switch b[i] {
            case 0x4D:                                                   // 'M'
                i += 1
                pen = CGPoint(x: number(), y: number())
                start = pen
                path.move(to: pen)
            case 0x4C:                                                   // 'L'
                i += 1
                pen = CGPoint(x: number(), y: number())
                path.addLine(to: pen)
            case 0x43:                                                   // 'C'
                i += 1
                let c1 = CGPoint(x: number(), y: number())
                let c2 = CGPoint(x: number(), y: number())
                pen = CGPoint(x: number(), y: number())
                path.addCurve(to: pen, control1: c1, control2: c2)
            case 0x5A, 0x7A:                                             // 'Z', 'z'
                i += 1
                path.closeSubpath()
                pen = start
            default:
                i += 1
            }
        }
        return path.copy()!
    }

    // ── Drawing ─────────────────────────────────────────────────────────────

    /**
     Draws Mochi into the box whose top-left is (`left`, `top`) and whose
     height is `height`. The art's own coordinates are y-down, as a UIKit
     context is, so the whole thing is drawn under one transform and no flip.
     */
    static func draw(in ctx: CGContext, left: CGFloat, top: CGFloat, height: CGFloat, mood: Mood) {
        ctx.saveGState()
        ctx.translateBy(x: left, y: top)
        ctx.scaleBy(x: widthFor(height) / MochiArt.viewWidth, y: height / MochiArt.viewHeight)
        for item in items(mood) {
            ctx.saveGState()
            if item.alpha < 1 { ctx.setAlpha(item.alpha) }
            if let solid = item.solid {
                ctx.addPath(item.path)
                ctx.setFillColor(solid)
                ctx.fillPath()
            } else if let gradient = item.gradient {
                ctx.addPath(item.path)
                ctx.clip()
                ctx.drawLinearGradient(gradient, start: item.start, end: item.end,
                                       options: [.drawsBeforeStartLocation,
                                                 .drawsAfterEndLocation])
            }
            ctx.restoreGState()
        }
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
