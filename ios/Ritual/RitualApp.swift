import FirebaseCore
import SwiftUI
import WidgetKit

enum Route: Equatable {
    case welcome
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
    @StateObject private var onboarding = Onboarding.shared
    @State private var route: Route = RitualApp.firstRoute()
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

    /**
     Where a launch lands.

     The sign-in step is only in the way while all three are true: there is a
     Firebase configuration to sign in to, nobody is signed in on this device,
     and nobody has said they would rather not be. Any one of those failing and
     the app opens on the grid, which is what it is for.
     */
    private static func firstRoute() -> Route {
        let account = Account.shared
        return account.available && account.uid == nil && !Onboarding.shared.skippedSignIn
            ? .welcome : .home
    }

    /// The unlock is offered once, on the way out of the welcome, and never
    /// unprompted again — a second ritual asks for itself when it is wanted.
    private func leaveWelcome() {
        route = .home
        if !unlock.isUnlocked && !onboarding.sawPaywall {
            onboarding.markSawPaywall()
            showingPaywall = true
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
        case .welcome:
            WelcomeView(
                account: account,
                onSignedIn: { leaveWelcome() },
                onSkip: {
                    onboarding.skipSignIn()
                    leaveWelcome()
                }
            )

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
