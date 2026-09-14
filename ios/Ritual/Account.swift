import AuthenticationServices
import CryptoKit
import Foundation
import FirebaseCore
import FirebaseAuth
import GoogleSignIn
import UIKit

/**
 Who this device is signed in as, if anyone.

 Three ways in — Apple, Google, and an email and password — and they all land
 on the same uid, which is the only thing the rest of the app knows about a
 person. What a Ritual account *is* does not change with how you opened it.

 Email stays as the floor, because it is the one that behaves identically on
 iOS and Android.

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

    /// Whether a Google button can do anything: the client id comes out of
    /// GoogleService-Info.plist, which a checkout does not carry.
    var googleAvailable: Bool { FirebaseApp.app()?.options.clientID != nil }

    private var handle: AuthStateDidChangeListenerHandle?

    /// Held between the Apple request being built and its result coming back.
    /// Apple signs the hash; Firebase checks the original against it, which is
    /// what stops a token from one session being replayed into another.
    private var appleNonce: String?

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

    // ── Google ──────────────────────────────────────────────────────────────

    func signInWithGoogle() async -> Bool {
        guard available, let clientID = FirebaseApp.app()?.options.clientID else {
            error = "This build has no Firebase configuration."
            return false
        }
        guard let presenter = Self.topViewController() else { return false }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        busy = true
        error = nil
        defer { busy = false }
        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
            guard let token = result.user.idToken?.tokenString else {
                error = "Google did not return a token."
                return false
            }
            let credential = GoogleAuthProvider.credential(
                withIDToken: token, accessToken: result.user.accessToken.tokenString)
            _ = try await Auth.auth().signIn(with: credential)
            return true
        } catch let failure as NSError where failure.code == GIDSignInError.canceled.rawValue {
            // Dismissing the sheet is a choice, not a fault.
            return false
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    // ── Apple ───────────────────────────────────────────────────────────────

    /// Fills in the request the SignInWithAppleButton is about to make.
    func prepare(_ request: ASAuthorizationAppleIDRequest) {
        let nonce = Self.randomNonce()
        appleNonce = nonce
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(nonce)
    }

    /// Takes what the button came back with. Apple sends a name exactly once,
    /// on the very first authorisation, so it is passed straight through to
    /// Firebase while it is here.
    func finish(_ result: Result<ASAuthorization, Error>) async -> Bool {
        guard available else {
            error = "This build has no Firebase configuration."
            return false
        }
        switch result {
        case .failure(let failure):
            if (failure as? ASAuthorizationError)?.code != .canceled {
                error = failure.localizedDescription
            }
            return false
        case .success(let authorization):
            guard let apple = authorization.credential as? ASAuthorizationAppleIDCredential,
                  let data = apple.identityToken,
                  let token = String(data: data, encoding: .utf8),
                  let nonce = appleNonce else {
                error = "Apple did not return a usable token."
                return false
            }
            busy = true
            error = nil
            defer { busy = false; appleNonce = nil }
            do {
                let credential = OAuthProvider.appleCredential(
                    withIDToken: token, rawNonce: nonce, fullName: apple.fullName)
                _ = try await Auth.auth().signIn(with: credential)
                return true
            } catch {
                self.error = error.localizedDescription
                return false
            }
        }
    }

    func signOut() {
        guard available else { return }
        try? Auth.auth().signOut()
        GIDSignIn.sharedInstance.signOut()
        error = nil
    }

    // ── Odds and ends ───────────────────────────────────────────────────────

    /// Where a Google sheet gets presented from. SwiftUI has no view
    /// controller to hand over, so the key window's root stands in.
    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var top = scene?.keyWindow?.rootViewController
        while let next = top?.presentedViewController { top = next }
        return top
    }

    private static func randomNonce(length: Int = 32) -> String {
        var bytes = [UInt8](repeating: 0, count: length)
        _ = SecRandomCopyBytes(kSecRandomDefault, length, &bytes)
        let alphabet = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        return String(bytes.map { alphabet[Int($0) % alphabet.count] })
    }

    private static func sha256(_ input: String) -> String {
        SHA256.hash(data: Data(input.utf8)).map { String(format: "%02x", $0) }.joined()
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
