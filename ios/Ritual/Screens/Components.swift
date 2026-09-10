import SwiftUI

/**
 A rendered card, sized to its box.

 The bitmap comes from `SlabRenderer` — the same function the widget draws
 with — so the app and the home screen cannot drift apart. It is cached
 against the model so scrolling a list of rituals doesn't redraw 365 cells a
 frame.
 */
struct RitualCard: View {
    let model: SlabModel
    var height: CGFloat
    var header = true
    var footer = true
    var cat = true
    var quarterRuler = false
    var corner: CGFloat = 24
    var pad: CGFloat = 17

    var body: some View {
        GeometryReader { geo in
            Image(uiImage: SlabRenderer.render(
                size: CGSize(width: geo.size.width, height: height),
                model: model,
                config: SlabRenderer.Config(
                    header: header,
                    footer: footer,
                    cat: cat,
                    quarterRuler: quarterRuler,
                    corner: corner,
                    pad: pad
                )
            ))
            .resizable()
            .frame(width: geo.size.width, height: height)
        }
        .frame(height: height)
    }
}

/// Mochi on a coloured tile — the app's only mascot surface.
struct MochiTile: View {
    let mood: Mood
    let tile: Color
    var height: CGFloat = 48
    var corner: CGFloat = 14
    var inset: CGFloat = 8

    var body: some View {
        Image(uiImage: Cat.image(height: height, mood: mood))
            .resizable()
            .frame(width: Cat.widthFor(height), height: height)
            .padding(.horizontal, inset)
            .padding(.vertical, inset * 0.8)
            .background(tile)
            .clipShape(RoundedRectangle(cornerRadius: corner, style: .continuous))
    }
}

struct CapsLabel: View {
    let text: String
    var color: Color = Theme.inkSoft

    var body: some View {
        Text(text.uppercased()).capsStyle(10, color: color)
    }
}

/// The primary control: a solid black pill. Nothing else in the app is this loud.
struct InkPill<Leading: View>: View {
    let label: String
    var height: CGFloat = 58
    var background: Color = Theme.ink
    var content: Color = Theme.paper
    var border: Color?
    let action: () -> Void
    @ViewBuilder var leading: Leading

    var body: some View {
        Button(action: action) {
            HStack(spacing: 9) {
                leading
                Text(label)
                    .font(Theme.display(15))
                    .foregroundStyle(content)
            }
            .frame(maxWidth: .infinity)
            .frame(height: height)
            .background(background)
            .clipShape(Capsule())
            .overlay {
                if let border {
                    Capsule().strokeBorder(border, lineWidth: 2)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

extension InkPill where Leading == EmptyView {
    init(label: String,
         height: CGFloat = 58,
         background: Color = Theme.ink,
         content: Color = Theme.paper,
         border: Color? = nil,
         action: @escaping () -> Void) {
        self.init(label: label, height: height, background: background,
                  content: content, border: border, action: action) { EmptyView() }
    }
}

/// A circular icon button with a 2pt outline — the app's secondary action.
struct RoundButton<Content: View>: View {
    var size: CGFloat = 58
    var background: Color = .clear
    var border: Color? = Theme.ink
    let action: () -> Void
    @ViewBuilder var content: Content

    var body: some View {
        Button(action: action) {
            content
                .frame(width: size, height: size)
                .background(background)
                .clipShape(Circle())
                .overlay {
                    if let border {
                        Circle().strokeBorder(border, lineWidth: 2)
                    }
                }
        }
        .buttonStyle(.plain)
    }
}

struct StatCell: View {
    let value: String
    let label: String
    var color: Color = Theme.ink

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(value).displayStyle(24, color: color)
            CapsLabel(text: label)
        }
    }
}

/// The colour chooser: six flat tiles, the chosen one ringed in ink.
struct ColourTiles: View {
    let selected: Int
    let onSelect: (Int) -> Void

    var body: some View {
        HStack(spacing: 10) {
            ForEach(Array(ACCENTS.enumerated()), id: \.offset) { index, accent in
                RoundedRectangle(cornerRadius: 15, style: .continuous)
                    .fill(Color(accent.block))
                    .frame(height: 46)
                    .overlay {
                        if index == selected {
                            RoundedRectangle(cornerRadius: 15, style: .continuous)
                                .strokeBorder(Theme.ink, lineWidth: 2.5)
                        }
                    }
                    .onTapGesture { onSelect(index) }
            }
        }
    }
}

/// A back chevron, drawn rather than shipped as an asset.
struct Chevron: View {
    var pointsLeft = true
    var color: Color = Theme.ink
    var size: CGFloat = 16

    var body: some View {
        Path { path in
            let tip = pointsLeft ? size * 0.3 : size * 0.7
            let base = pointsLeft ? size * 0.62 : size * 0.38
            path.move(to: CGPoint(x: base, y: size * 0.18))
            path.addLine(to: CGPoint(x: tip, y: size * 0.5))
            path.addLine(to: CGPoint(x: base, y: size * 0.82))
        }
        .stroke(color, style: StrokeStyle(lineWidth: size * 0.14, lineCap: .round, lineJoin: .round))
        .frame(width: size, height: size)
    }
}

struct Checkmark: View {
    var color: Color = Theme.ink
    var size: CGFloat = 16

    var body: some View {
        Path { path in
            path.move(to: CGPoint(x: size * 0.18, y: size * 0.53))
            path.addLine(to: CGPoint(x: size * 0.4, y: size * 0.74))
            path.addLine(to: CGPoint(x: size * 0.82, y: size * 0.29))
        }
        .stroke(color, style: StrokeStyle(lineWidth: size * 0.15, lineCap: .round, lineJoin: .round))
        .frame(width: size, height: size)
    }
}
