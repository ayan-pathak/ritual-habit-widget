import SwiftUI

struct HomeScreen: View {
    @ObservedObject var store: HabitStore
    @ObservedObject var unlock: Unlock
    let onOpen: (Habit) -> Void
    let onCreate: () -> Void
    let onPaywall: () -> Void
    let onAccount: () -> Void

    private var today: DayDate { DayDate.today() }

    var body: some View {
        let today = self.today
        let year = today.year
        let yearLen = DayDate.lengthOfYear(year)
        let remaining = Habit.remainingIn(year, today: today)

        // Mochi in the corner speaks for the whole app: pleased once every
        // ritual is marked, awake while any is still open.
        let allDone = !store.habits.isEmpty && store.habits.allSatisfy { $0.isDone(today) }

        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Text("Ritual").displayStyle(19)
                    Spacer()
                    Button(action: onAccount) {
                        MochiTile(
                            mood: allDone ? .pleased : .awake,
                            tile: allDone ? Theme.lime : Theme.paper,
                            pixel: 0.95,
                            corner: 999,
                            inset: 7
                        )
                    }
                    .buttonStyle(.plain)
                }
                .padding(.top, 18)

                VStack(alignment: .leading, spacing: 14) {
                    Text("\(remaining) squares\nleft this year")
                        .displayStyle(40)
                        .lineSpacing(1)
                    HStack(spacing: 7) {
                        RoundedRectangle(cornerRadius: 3, style: .continuous)
                            .fill(Theme.ink)
                            .frame(width: 9, height: 9)
                        Text("Day \(today.dayOfYear) of \(yearLen)").bodyStyle(13)
                    }
                }
                .padding(.top, 22)

                if store.habits.isEmpty {
                    EmptyState(onCreate: onCreate).padding(.top, 28)
                } else {
                    ForEach(store.habits) { habit in
                        RitualCard(model: habit.model(today: today, year: year), height: 172)
                            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                            .padding(.top, 16)
                            .onTapGesture { onOpen(habit) }
                    }

                    InkPill(label: "New ritual", action: {
                        if unlock.canCreate(existing: store.habits.count) { onCreate() } else { onPaywall() }
                    })
                    .padding(.top, 24)

                    if !unlock.canCreate(existing: store.habits.count) {
                        Text("One ritual is free. Unlock the rest for \(unlock.displayPrice), once.")
                            .bodyStyle(12, color: Theme.inkFaint)
                            .frame(maxWidth: .infinity)
                            .multilineTextAlignment(.center)
                            .padding(.top, 10)
                    }

                    Spacer(minLength: 28)
                }
            }
            .padding(.horizontal, 20)
        }
        .background(Theme.cream)
    }
}

private struct EmptyState: View {
    let onCreate: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            VStack(spacing: 0) {
                MochiTile(mood: .awake, tile: Theme.paper, pixel: 2.5, corner: 18, inset: 14)
                Text("Nothing to keep yet.")
                    .displayStyle(24)
                    .multilineTextAlignment(.center)
                    .padding(.top, 20)
                Text("Name one practice. Every day you keep it fills a square.")
                    .bodyStyle(14, color: Color(0xB312120F as ARGB))
                    .multilineTextAlignment(.center)
                    .padding(.top, 10)
            }
            .frame(maxWidth: .infinity)
            .padding(24)
            .background(Theme.lime)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))

            InkPill(label: "Start a ritual", action: onCreate)
        }
    }
}
