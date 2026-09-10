import SwiftUI

private let SLOTS = ["Morning", "Midday", "Evening", "Anytime"]

struct CreateScreen: View {
    @ObservedObject var store: HabitStore
    let onDone: (Habit) -> Void
    let onBack: () -> Void

    @State private var name = ""
    @State private var slotIndex = 0
    @State private var accentIndex = 0

    var body: some View {
        let today = DayDate.today()
        let accent = accentAt(accentIndex)

        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    RoundButton(size: 40, action: onBack) { Chevron() }
                    Spacer()
                }
                .padding(.top, 14)

                Text("New ritual").displayStyle(40).padding(.top, 18)

                // ── Name ────────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 10) {
                    CapsLabel(text: "What will you keep")
                    TextField("Read before sleep", text: $name)
                        .font(Theme.display(22))
                        .foregroundStyle(Theme.ink)
                        .tint(Theme.ink)
                        .autocorrectionDisabled()
                        .submitLabel(.done)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 18)
                        .background(Theme.paper)
                        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                        .onChange(of: name) { _, value in
                            if value.count > 26 { name = String(value.prefix(26)) }
                        }
                }
                .padding(.top, 26)

                // ── Slot ────────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 10) {
                    CapsLabel(text: "When")
                    HStack(spacing: 8) {
                        ForEach(Array(SLOTS.enumerated()), id: \.offset) { i, slot in
                            let on = i == slotIndex
                            Text(slot)
                                .font(Theme.caps(11))
                                .foregroundStyle(on ? Theme.paper : Theme.inkSoft)
                                .frame(maxWidth: .infinity)
                                .frame(height: 40)
                                .background(on ? Theme.ink : .clear)
                                .clipShape(Capsule())
                                .overlay {
                                    if !on { Capsule().strokeBorder(Theme.inkFaint, lineWidth: 1.5) }
                                }
                                .onTapGesture { slotIndex = i }
                        }
                    }
                }
                .padding(.top, 24)

                // ── Colour ──────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 10) {
                    CapsLabel(text: "Its colour")
                    ColourTiles(selected: accentIndex) { accentIndex = $0 }
                }
                .padding(.top, 24)

                // ── Preview ─────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 12) {
                    CapsLabel(text: "On your home screen")
                    RitualCard(
                        model: SlabModel(
                            title: name.isEmpty ? "Read before sleep" : name,
                            slot: SLOTS[slotIndex],
                            accent: accent,
                            year: today.year,
                            today: today,
                            doneDaysOfYear: [],
                            streak: 0,
                            totalDone: 0,
                            remaining: Habit.remainingIn(today.year, today: today),
                            doneToday: false,
                            mood: .awake
                        ),
                        height: 172
                    )
                }
                .padding(.top, 26)

                InkPill(label: "Start today", action: {
                    let habit = store.create(name: name, slot: SLOTS[slotIndex], accentIndex: accentIndex)
                    onDone(habit)
                })
                .padding(.top, 26)

                Spacer(minLength: 40)
            }
            .padding(.horizontal, 20)
        }
        .background(Theme.cream)
    }
}
