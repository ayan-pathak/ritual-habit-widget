import Foundation
import WidgetKit

/**
 The single source of truth, shared by the app and the widget extension.

 A JSON blob in the app group's `UserDefaults`, in the same shape the Android
 build writes: completions are epoch-days, one number each, so a decade of
 daily practice is a few kilobytes. The widget process can start cold at any
 moment, so every public entry point calls `ensureLoaded` first rather than
 trusting an initialiser to have run.
 */
final class HabitStore: ObservableObject {

    static let appGroup = "group.com.ayan.ritual"
    private static let key = "habits_v1"

    static let shared = HabitStore()

    @Published private(set) var habits: [Habit] = []

    /// Falls back to the app's own defaults when the group is unavailable —
    /// an unsigned simulator build, say. The app still works; the widget
    /// simply won't see the same rituals.
    static let defaults = UserDefaults(suiteName: HabitStore.appGroup) ?? .standard

    private var defaults: UserDefaults { HabitStore.defaults }
    private var loaded = false

    private init() {}

    func ensureLoaded() {
        if loaded { return }
        loaded = true
        habits = HabitStore.snapshot()
    }

    /// Re-reads from disk. The widget's process may have marked a day since.
    func reload() {
        loaded = true
        habits = HabitStore.snapshot()
    }

    // ── Detached access ─────────────────────────────────────────────────────
    //
    // The widget's process and its App Intents run outside the app and outside
    // the main actor, and they must not publish into a view tree that isn't
    // there. These read and write the same bytes without touching @Published.

    /// What is on disk right now. Safe from any thread.
    static func snapshot() -> [Habit] {
        decode(defaults.string(forKey: key))
    }

    /// The ritual a widget shows: the one it was configured with, or the first
    /// one, so an unconfigured widget is never a blank rectangle.
    static func habitForWidget(id: String?) -> Habit? {
        let all = snapshot()
        if let id, let match = all.first(where: { $0.id == id }) { return match }
        return all.first
    }

    /// Flips `date` for `id` straight on disk, for the widget's mark button.
    static func toggleDetached(id: String, date: DayDate) {
        var all = snapshot()
        guard let index = all.firstIndex(where: { $0.id == id }) else { return }
        if all[index].done.contains(date.epochDay) {
            all[index].done.remove(date.epochDay)
        } else {
            all[index].done.insert(date.epochDay)
        }
        defaults.set(encode(all), forKey: key)
    }

    func habit(id: String?) -> Habit? {
        ensureLoaded()
        guard let id else { return nil }
        return habits.first { $0.id == id }
    }

    @discardableResult
    func create(name: String, slot: String, accentIndex: Int) -> Habit {
        ensureLoaded()
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let habit = Habit(
            id: UUID().uuidString,
            name: trimmed.isEmpty ? "Untitled" : trimmed,
            slot: slot.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Daily" : slot,
            accentIndex: accentIndex,
            createdEpochDay: DayDate.today().epochDay,
            done: []
        )
        habits.append(habit)
        persist()
        return habit
    }

    func update(_ habit: Habit) {
        ensureLoaded()
        habits = habits.map { $0.id == habit.id ? habit : $0 }
        persist()
    }

    func delete(id: String) {
        ensureLoaded()
        habits.removeAll { $0.id == id }
        persist()
    }

    /// Flips `date` for `id`. Returns the habit's new state, or nil if it's gone.
    @discardableResult
    func toggle(id: String, date: DayDate) -> Habit? {
        ensureLoaded()
        guard var habit = habits.first(where: { $0.id == id }) else { return nil }
        if habit.done.contains(date.epochDay) {
            habit.done.remove(date.epochDay)
        } else {
            habit.done.insert(date.epochDay)
        }
        update(habit)
        return habit
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    private func persist() {
        defaults.set(HabitStore.encode(habits), forKey: HabitStore.key)
        WidgetCenter.shared.reloadAllTimelines()
    }

    private static func encode(_ list: [Habit]) -> String {
        let array: [[String: Any]] = list.map { h in
            [
                "id": h.id,
                "name": h.name,
                "slot": h.slot,
                "accent": h.accentIndex,
                "created": h.createdEpochDay,
                "done": h.done.sorted()
            ]
        }
        guard let data = try? JSONSerialization.data(withJSONObject: array),
              let json = String(data: data, encoding: .utf8) else { return "[]" }
        return json
    }

    private static func decode(_ raw: String?) -> [Habit] {
        guard let raw, !raw.isEmpty, let data = raw.data(using: .utf8),
              let array = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]]
        else { return [] }

        return array.compactMap { o in
            guard let id = o["id"] as? String else { return nil }
            let days = (o["done"] as? [Any])?.compactMap { ($0 as? NSNumber)?.intValue } ?? []
            return Habit(
                id: id,
                name: o["name"] as? String ?? "Untitled",
                slot: o["slot"] as? String ?? "Daily",
                accentIndex: (o["accent"] as? NSNumber)?.intValue ?? 0,
                createdEpochDay: (o["created"] as? NSNumber)?.intValue ?? DayDate.today().epochDay,
                done: Set(days)
            )
        }
    }
}
