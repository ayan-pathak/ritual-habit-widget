import Foundation
import FirebaseCore
import FirebaseAuth

/**
 Who this device is signed in as, if anyone.

 Email and password rather than Sign in with Apple, because the point of an
 account here is moving a practice from one phone to the next — and email is
 the only sign-in that behaves identically on iOS and Android. Apple and Google
 can be added later without changing what a Ritual account is.

 Signing in is optional and stays optional. Ritual works with no account, on a
 device that has never seen a network, exactly as it always did; nothing here
 may become load-bearing for marking a day.
 */
@MainActor
final class Account: ObservableObject {

    static let shared = Account()

    @Published private(set) var uid: String?
    @Published private(set) var email: String?
    @Published private(set) var busy = false
    @Published private(set) var error: String?

    /// False when the app has no Firebase configuration, which is the normal
    /// state of a checkout: the account screen says so rather than failing.
    let available: Bool

    private var handle: AuthStateDidChangeListenerHandle?

    private init() {
        available = FirebaseApp.app() != nil
        guard available else { return }
        let auth = Auth.auth()
        uid = auth.currentUser?.uid
        email = auth.currentUser?.email
        handle = auth.addStateDidChangeListener { [weak self] _, user in
            Task { @MainActor in
                self?.uid = user?.uid
                self?.email = user?.email
            }
        }
    }

    func signIn(email address: String, password: String) async -> Bool {
        await run { try await Auth.auth().signIn(withEmail: address.trimmed, password: password) }
    }

    func createAccount(email address: String, password: String) async -> Bool {
        await run { try await Auth.auth().createUser(withEmail: address.trimmed, password: password) }
    }

    func signOut() {
        guard available else { return }
        try? Auth.auth().signOut()
        error = nil
    }

    private func run(_ work: () async throws -> AuthDataResult) async -> Bool {
        guard available else {
            error = "This build has no Firebase configuration."
            return false
        }
        busy = true
        error = nil
        defer { busy = false }
        do {
            _ = try await work()
            return true
        } catch let failure {
            error = failure.localizedDescription
            return false
        }
    }
}

private extension String {
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
}
