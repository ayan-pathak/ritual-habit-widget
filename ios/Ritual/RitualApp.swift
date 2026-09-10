import FirebaseCore
import SwiftUI
import WidgetKit

enum Route: Equatable {
    case home
    case detail(String)
    case create
    case account
}

@main
struct RitualApp: App {

    @StateObject private var store = HabitStore.shared
    @StateObject private var unlock = Unlock.shared
    @StateObject private var account = Account.shared
    @State private var route: Route = .home
    @State private var showingPaywall = false
    @Environment(\.scenePhase) private var scenePhase

    init() {
        Fonts.register()
        // GoogleService-Info.plist is per-project configuration and is not in
        // the repository. Without it there is simply no cloud to reach, which
        // is the same path a signed-out device takes.
        if Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil {
            FirebaseApp.configure()
        }
        HabitStore.shared.ensureLoaded()

        // The mirror follows the device, never the other way round: a local
        // write lands on disk first and is pushed after.
        HabitStore.shared.onChanged = {
            Task { @MainActor in CloudSync.shared.pushAll() }
        }
    }

    var body: some Scene {
        WindowGroup {
            content
                .task {
                    await unlock.load()
                    if let uid = account.uid { CloudSync.shared.start(uid: uid) }
                }
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
                onPaywall: { showingPaywall = true },
                onAccount: { route = .account }
            )

        case .account:
            AccountView(account: account, onBack: { route = .home })

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
                    onPaywall: { showingPaywall = true },
                    onAccount: { route = .account }
                )
            }
        }
    }
}
