import SwiftUI
import WidgetKit

enum Route: Equatable {
    case home
    case detail(String)
    case create
}

@main
struct RitualApp: App {

    @StateObject private var store = HabitStore.shared
    @StateObject private var unlock = Unlock.shared
    @State private var route: Route = .home
    @State private var showingPaywall = false
    @Environment(\.scenePhase) private var scenePhase

    init() {
        Fonts.register()
        HabitStore.shared.ensureLoaded()
    }

    var body: some Scene {
        WindowGroup {
            content
                .task { await unlock.load() }
                .onOpenURL { url in
                    // A tap on the widget lands straight on that ritual.
                    guard url.scheme == "ritual" else { return }
                    let id = url.host ?? url.lastPathComponent
                    if !id.isEmpty { route = .detail(id) }
                }
                .onChange(of: scenePhase) { _, phase in
                    switch phase {
                    case .active:
                        // The widget's process may have marked a day since.
                        store.reload()
                    case .background:
                        // Whatever changed in here, the home screen agrees.
                        WidgetCenter.shared.reloadAllTimelines()
                    default:
                        break
                    }
                }
                .sheet(isPresented: $showingPaywall) {
                    PaywallView(unlock: unlock) { showingPaywall = false }
                }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch route {
        case .home:
            HomeScreen(
                store: store,
                unlock: unlock,
                onOpen: { route = .detail($0.id) },
                onCreate: { route = .create },
                onPaywall: { showingPaywall = true }
            )

        case .create:
            CreateScreen(
                store: store,
                onDone: { route = .detail($0.id) },
                onBack: { route = .home }
            )

        case .detail(let id):
            if let habit = store.habits.first(where: { $0.id == id }) {
                DetailScreen(habit: habit, store: store, onBack: { route = .home })
            } else {
                HomeScreen(
                    store: store,
                    unlock: unlock,
                    onOpen: { route = .detail($0.id) },
                    onCreate: { route = .create },
                    onPaywall: { showingPaywall = true }
                )
            }
        }
    }
}
