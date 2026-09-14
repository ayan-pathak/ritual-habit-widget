import Foundation

/**
 The two answers the first launch needs, remembered so it never asks twice.

 Both are decisions to *stop* asking. Ritual keeps every square on the device
 whether or not anyone ever signs in, so the sign-in step has to be one a
 person can walk past — and once they have walked past it, walking past it
 again on every launch would be nagging rather than offering.

 App-only, so plain `UserDefaults`: the widget never asks either question.
 */
@MainActor
final class Onboarding: ObservableObject {

    static let shared = Onboarding()

    private static let skippedKey = "skipped_sign_in_v1"
    private static let sawPaywallKey = "saw_paywall_v1"

    /// Whether someone has said they would rather not have an account.
    @Published private(set) var skippedSignIn: Bool

    /// Whether the unlock has been offered once, unprompted.
    private(set) var sawPaywall: Bool

    private let defaults = UserDefaults.standard

    private init() {
        skippedSignIn = defaults.bool(forKey: Self.skippedKey)
        sawPaywall = defaults.bool(forKey: Self.sawPaywallKey)
    }

    func skipSignIn() {
        skippedSignIn = true
        defaults.set(true, forKey: Self.skippedKey)
    }

    func markSawPaywall() {
        sawPaywall = true
        defaults.set(true, forKey: Self.sawPaywallKey)
    }
}
