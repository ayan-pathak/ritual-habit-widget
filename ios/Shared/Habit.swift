import Foundation

/**
 One practice, and every day it was kept.

 Days are stored as epoch-days: a single `Int` per completion, timezone-free
 and trivially cheap to hold a decade of.
 */
struct Habit: Identifiable, Equatable {

    let id: String
    var name: String
    /// When in the day this belongs — shown above the name on every card.
    var slot: String
    var accentIndex: Int
    var createdEpochDay: Int
    var done: Set<Int>

    func isDone(_ date: DayDate) -> Bool { done.contains(date.epochDay) }

    /// Days completed in `year`, as day-of-year numbers, ready for the grid.
    func daysOfYear(_ year: Int) -> Set<Int> {
        let first = DayDate(year: year, month: 1, day: 1).epochDay
        let last = DayDate(year: year, month: 12, day: 31).epochDay
        var out = Set<Int>()
        for day in done where day >= first && day <= last {
            out.insert(day - first + 1)
        }
        return out
    }

    func totalIn(_ year: Int) -> Int { daysOfYear(year).count }

    /// Every square ever filled, across every year kept.
    var totalAllTime: Int { done.count }

    /// The first year this ritual has anything in it — where the archive starts.
    var firstYear: Int {
        DayDate(epochDay: min(createdEpochDay, done.min() ?? createdEpochDay)).year
    }

    /// Consecutive days ending today — or yesterday, so a day in progress
    /// doesn't read as broken.
    func streak(_ today: DayDate) -> Int {
        var day = today.epochDay
        if !done.contains(day) { day -= 1 }
        var n = 0
        while done.contains(day) {
            n += 1
            day -= 1
        }
        return n
    }

    /// True when yesterday was missed and today is not yet marked — Mochi's cue.
    func missedYesterday(_ today: DayDate) -> Bool {
        !done.contains(today.epochDay - 1) && !isDone(today)
    }

    func bestStreak() -> Int {
        if done.isEmpty { return 0 }
        let sorted = done.sorted()
        var best = 1
        var run = 1
        for i in 1..<sorted.count {
            run = sorted[i] == sorted[i - 1] + 1 ? run + 1 : 1
            if run > best { best = run }
        }
        return best
    }

    /// Squares still unwritten in `year` — the days after today.
    static func remainingIn(_ year: Int, today: DayDate) -> Int {
        if today.year != year {
            return today.year < year ? DayDate.lengthOfYear(year) : 0
        }
        return DayDate.lengthOfYear(year) - today.dayOfYear
    }

    /// Shared shape for every card, so the widget can't drift from the app.
    func model(today: DayDate, year: Int) -> SlabModel {
        SlabModel(
            title: name,
            slot: slot,
            accent: accentAt(accentIndex),
            year: year,
            today: today,
            doneDaysOfYear: daysOfYear(year),
            streak: streak(today),
            totalDone: totalIn(year),
            remaining: Habit.remainingIn(year, today: today),
            doneToday: isDone(today),
            mood: Cat.moodFor(
                doneToday: isDone(today),
                streak: streak(today),
                missedYesterday: missedYesterday(today)
            )
        )
    }
}
