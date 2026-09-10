import SwiftUI

struct DetailScreen: View {
    let habit: Habit
    @ObservedObject var store: HabitStore
    let onBack: () -> Void

    @State private var year: Int = DayDate.today().year
    @State private var confirmingDelete = false

    var body: some View {
        let today = DayDate.today()
        let accent = accentAt(habit.accentIndex)
        let model = habit.model(today: today, year: year)
        let viewingNow = year == today.year

        ScrollView {
            VStack(alignment: .leading, spacing: 0) {

                // ── Bar ─────────────────────────────────────────────────────
                HStack(spacing: 10) {
                    RoundButton(size: 40, action: onBack) { Chevron() }
                    Spacer()
                    YearStep(pointsLeft: true, enabled: year > habit.firstYear) {
                        if year > habit.firstYear { year -= 1 }
                    }
                    Text("\(year)").displayStyle(15)
                    YearStep(pointsLeft: false, enabled: year < today.year) {
                        if year < today.year { year += 1 }
                    }
                }
                .padding(.top, 14)

                // ── Title ───────────────────────────────────────────────────
                HStack(alignment: .bottom) {
                    VStack(alignment: .leading, spacing: 5) {
                        CapsLabel(text: habit.slot)
                        Text(habit.name).displayStyle(34)
                    }
                    Spacer()
                    MochiTile(mood: model.mood, tile: Color(accent.block), pixel: 3, corner: 18, inset: 11)
                }
                .padding(.top, 18)

                // ── The field ───────────────────────────────────────────────
                RitualCard(
                    model: model,
                    height: 132,
                    header: false,
                    footer: false,
                    cat: false,
                    quarterRuler: true,
                    pad: 18
                )
                .padding(.vertical, 20)

                // ── Tally ───────────────────────────────────────────────────
                HStack {
                    StatCell(value: "\(habit.streak(today))", label: "Streak")
                    Spacer()
                    StatCell(value: "\(habit.bestStreak())", label: "Longest")
                    Spacer()
                    StatCell(value: "\(habit.totalIn(year))", label: "Lit in \(year)")
                    Spacer()
                    StatCell(value: "\(habit.totalAllTime)", label: "All time", color: Theme.inkSoft)
                }

                // A streak is counted in days, not in years — it carries
                // straight over New Year's, and the archive keeps every year
                // it crossed.
                if habit.streak(today) > DayDate.lengthOfYear(today.year) {
                    Text("That streak has run longer than a year.")
                        .bodyStyle(12, color: Theme.inkFaint)
                        .padding(.top, 10)
                }

                // ── Cadence ─────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 12) {
                    CapsLabel(text: "By month")
                    MonthBars(habit: habit, year: year)
                }
                .padding(.top, 28)

                // ── Share ───────────────────────────────────────────────────
                VStack(spacing: 8) {
                    if StoryShare.isInstagramInstalled {
                        InkPill(
                            label: "Share streak",
                            background: Color(accent.block),
                            content: Theme.ink,
                            border: Theme.ink,
                            action: { StoryShare.shareStreak(model: model) }
                        ) {
                            InstagramGlyph()
                        }
                        Text("Opens Instagram Stories")
                            .bodyStyle(12, color: Theme.inkFaint)
                    } else {
                        let card = Image(uiImage: StoryShare.card(for: model))
                        ShareLink(item: card, preview: SharePreview("Your streak", image: card)) {
                            Text("Share streak")
                                .font(Theme.display(15))
                                .foregroundStyle(Theme.ink)
                                .frame(maxWidth: .infinity)
                                .frame(height: 58)
                                .background(Color(accent.block))
                                .clipShape(Capsule())
                                .overlay { Capsule().strokeBorder(Theme.ink, lineWidth: 2) }
                        }
                        Text("Instagram isn't installed — you'll get the share sheet")
                            .bodyStyle(12, color: Theme.inkFaint)
                    }
                }
                .padding(.top, 28)

                // ── The act ─────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 0) {
                    if viewingNow {
                        HStack(spacing: 10) {
                            InkPill(
                                label: model.doneToday ? "Marked today" : "Mark today",
                                background: model.doneToday ? Theme.cream : Theme.ink,
                                content: model.doneToday ? Theme.ink : Theme.paper,
                                border: model.doneToday ? Theme.ink : nil,
                                action: { store.toggle(id: habit.id, date: today) }
                            ) {
                                if model.doneToday { Checkmark() }
                            }
                            RoundButton(
                                background: confirmingDelete ? Theme.red : .clear,
                                border: confirmingDelete ? Theme.red : Theme.ink,
                                action: { confirmingDelete.toggle() }
                            ) {
                                Cross(color: confirmingDelete ? Theme.paper : Theme.ink)
                            }
                        }
                    } else {
                        Text("\(year) is closed. Come back to this year to mark a day.")
                            .bodyStyle(13)
                            .frame(maxWidth: .infinity)
                            .multilineTextAlignment(.center)
                    }

                    if confirmingDelete {
                        Text("Delete “\(habit.name)”? Every square it holds goes with it.")
                            .bodyStyle(13)
                            .padding(.top, 14)
                        InkPill(
                            label: "Delete forever",
                            background: Theme.red,
                            content: Theme.paper,
                            action: {
                                store.delete(id: habit.id)
                                onBack()
                            }
                        )
                        .padding(.top, 10)
                    }
                }
                .padding(.top, 22)

                Spacer(minLength: 40)
            }
            .padding(.horizontal, 20)
        }
        .background(Theme.cream)
        .onAppear { year = DayDate.today().year }
    }
}

/// Twelve bars: how much of each month was kept.
private struct MonthBars: View {
    let habit: Habit
    let year: Int

    var body: some View {
        let done = habit.daysOfYear(year)
        let ratios: [CGFloat] = {
            var start = 1
            var out: [CGFloat] = []
            for m in 1...12 {
                let len = DayDate.lengthOfMonth(m, in: year)
                var hits = 0
                for d in start..<(start + len) where done.contains(d) { hits += 1 }
                start += len
                out.append(CGFloat(hits) / CGFloat(len))
            }
            return out
        }()

        VStack(spacing: 7) {
            GeometryReader { geo in
                let gap: CGFloat = 6
                let barW = (geo.size.width - gap * 11) / 12
                HStack(spacing: gap) {
                    ForEach(0..<12, id: \.self) { i in
                        ZStack(alignment: .bottom) {
                            RoundedRectangle(cornerRadius: barW * 0.22, style: .continuous)
                                .fill(Theme.inkFaint)
                            if ratios[i] > 0 {
                                RoundedRectangle(cornerRadius: barW * 0.22, style: .continuous)
                                    .fill(Theme.ink)
                                    .frame(height: geo.size.height * ratios[i])
                            }
                        }
                        .frame(width: barW)
                    }
                }
            }
            .frame(height: 64)

            HStack(spacing: 0) {
                ForEach(Array(MONTH_INITIALS.enumerated()), id: \.offset) { _, m in
                    Text(m)
                        .font(Theme.caps(9))
                        .foregroundStyle(Theme.inkFaint)
                        .frame(maxWidth: .infinity)
                }
            }
        }
    }
}

private struct YearStep: View {
    let pointsLeft: Bool
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Chevron(pointsLeft: pointsLeft, color: enabled ? Theme.ink : Theme.inkFaint, size: 11)
                .frame(width: 34, height: 34)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

private struct Cross: View {
    var color: Color = Theme.ink

    var body: some View {
        Path { path in
            path.move(to: CGPoint(x: 4, y: 4))
            path.addLine(to: CGPoint(x: 14, y: 14))
            path.move(to: CGPoint(x: 14, y: 4))
            path.addLine(to: CGPoint(x: 4, y: 14))
        }
        .stroke(color, style: StrokeStyle(lineWidth: 2.3, lineCap: .round))
        .frame(width: 18, height: 18)
    }
}

/// Instagram's rounded square with a ring and a corner dot.
private struct InstagramGlyph: View {
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 17 * 0.28, style: .continuous)
                .strokeBorder(Theme.ink, lineWidth: 17 * 0.12)
                .frame(width: 17 * 0.88, height: 17 * 0.88)
            Circle()
                .strokeBorder(Theme.ink, lineWidth: 17 * 0.12)
                .frame(width: 17 * 0.42, height: 17 * 0.42)
            Circle()
                .fill(Theme.ink)
                .frame(width: 17 * 0.12, height: 17 * 0.12)
                .offset(x: 17 * 0.22, y: -17 * 0.22)
        }
        .frame(width: 17, height: 17)
    }
}
