import AppIntents
import SwiftUI
import WidgetKit

/**
 The slab on the home screen.

 The whole face is a bitmap drawn by `SlabRenderer` — the same code the app
 uses — with a transparent button laid over the mark pill, so a day can be kept
 without leaving the home screen. Tapping anywhere else opens the ritual.
 */

// ── Which ritual this widget shows ──────────────────────────────────────────

struct HabitEntity: AppEntity {
    let id: String
    let name: String

    static var typeDisplayRepresentation: TypeDisplayRepresentation { "Ritual" }
    static var defaultQuery = HabitQuery()

    var displayRepresentation: DisplayRepresentation { DisplayRepresentation(title: "\(name)") }
}

struct HabitQuery: EntityQuery {
    func entities(for identifiers: [String]) async throws -> [HabitEntity] {
        HabitStore.snapshot()
            .filter { identifiers.contains($0.id) }
            .map { HabitEntity(id: $0.id, name: $0.name) }
    }

    func suggestedEntities() async throws -> [HabitEntity] {
        HabitStore.snapshot().map { HabitEntity(id: $0.id, name: $0.name) }
    }
}

struct SelectRitualIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource { "Choose a ritual" }
    static var description: IntentDescription { "Which practice this widget keeps." }

    @Parameter(title: "Ritual")
    var habit: HabitEntity?

    func perform() async throws -> some IntentResult { .result() }
}

// ── Marking today from the home screen ──────────────────────────────────────

struct ToggleTodayIntent: AppIntent {
    static var title: LocalizedStringResource { "Mark today" }

    @Parameter(title: "Ritual")
    var habitID: String

    init() {}

    init(habitID: String) {
        self.habitID = habitID
    }

    func perform() async throws -> some IntentResult {
        HabitStore.toggleDetached(id: habitID, date: DayDate.today())
        return .result()
    }
}

// ── Timeline ────────────────────────────────────────────────────────────────

struct RitualEntry: TimelineEntry {
    let date: Date
    let habit: Habit?
}

struct RitualProvider: AppIntentTimelineProvider {

    func placeholder(in context: Context) -> RitualEntry {
        RitualEntry(date: Date(), habit: HabitStore.snapshot().first)
    }

    func snapshot(for configuration: SelectRitualIntent, in context: Context) async -> RitualEntry {
        RitualEntry(date: Date(), habit: HabitStore.habitForWidget(id: configuration.habit?.id))
    }

    func timeline(for configuration: SelectRitualIntent, in context: Context) async -> Timeline<RitualEntry> {
        let entry = RitualEntry(
            date: Date(),
            habit: HabitStore.habitForWidget(id: configuration.habit?.id)
        )
        // The grid moves at midnight and never in between.
        let midnight = Calendar.current.startOfDay(for: Date().addingTimeInterval(24 * 60 * 60))
        return Timeline(entries: [entry], policy: .after(midnight))
    }
}

// ── The face ────────────────────────────────────────────────────────────────

struct RitualWidgetView: View {
    let entry: RitualEntry

    var body: some View {
        GeometryReader { geo in
            if let habit = entry.habit {
                let today = DayDate.today()
                let model = habit.model(today: today, year: today.year)
                let config = SlabRenderer.Config(
                    action: true,
                    corner: 0,
                    fillBackground: true
                )
                let action = SlabRenderer.actionRect(
                    width: geo.size.width, height: geo.size.height,
                    model: model, config: config
                )

                ZStack(alignment: .topLeading) {
                    Image(uiImage: SlabRenderer.render(size: geo.size, model: model, config: config))
                        .resizable()
                        .frame(width: geo.size.width, height: geo.size.height)

                    // The pill is part of the bitmap; this is only its hit
                    // target, in exactly the rect the renderer drew it in.
                    Button(intent: ToggleTodayIntent(habitID: habit.id)) {
                        Color.clear
                    }
                    .buttonStyle(.plain)
                    .frame(width: action.width, height: action.height)
                    .offset(x: action.minX, y: action.minY)
                }
                .widgetURL(URL(string: "ritual://\(habit.id)"))
            } else {
                VStack(spacing: 10) {
                    MochiTileImage(mood: .awake)
                    Text("Name one practice")
                        .font(Theme.display(15))
                        .foregroundStyle(Theme.ink)
                }
                .frame(width: geo.size.width, height: geo.size.height)
                .background(Theme.lime)
            }
        }
    }
}

/// The widget's own copy of the mascot tile — it can't reach the app's views.
private struct MochiTileImage: View {
    let mood: Mood

    var body: some View {
        Image(uiImage: Cat.image(px: 1.52, mood: mood))
            .resizable()
            .frame(width: Cat.widthFor(1.52), height: Cat.heightFor(1.52))
    }
}

struct RitualWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: "RitualWidget",
            intent: SelectRitualIntent.self,
            provider: RitualProvider()
        ) { entry in
            RitualWidgetView(entry: entry)
                .containerBackground(for: .widget) { Theme.cream }
        }
        .configurationDisplayName("Ritual")
        .description("A year of squares. Tap the pill to keep today.")
        .supportedFamilies([.systemMedium, .systemLarge])
        .contentMarginsDisabled()
    }
}

@main
struct RitualWidgetBundle: WidgetBundle {
    var body: some Widget {
        RitualWidget()
    }
}
