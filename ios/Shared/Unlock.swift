import Foundation
import StoreKit

/**
 The lifetime unlock: one ritual is free, the rest are paid for once.

 The wall sits on *creating* a second ritual and nowhere else. Marking a day,
 the widget, the archive and the share card all keep working forever, for
 everyone, on whatever rituals they already have — a lapsed or refunded
 purchase must never leave someone unable to fill today's square, and it must
 never hide a year they already earned.

 There is no server behind any of this, so it is a one-time purchase rather
 than a subscription. StoreKit is the only source of truth; the cached flag in
 the app group is a convenience for a cold start with no network.
 */
@MainActor
final class Unlock: ObservableObject {

    static let productID = "com.ayan.ritual.unlimited"

    /// How many rituals someone can keep before the wall.
    static let freeLimit = 1

    static let shared = Unlock()

    @Published private(set) var isUnlocked: Bool
    @Published private(set) var product: Product?
    @Published private(set) var purchasing = false
    @Published private(set) var lastError: String?

    private static let cacheKey = "unlocked_v1"
    private let defaults: UserDefaults
    private var updates: Task<Void, Never>?

    private init() {
        defaults = UserDefaults(suiteName: HabitStore.appGroup) ?? .standard
        isUnlocked = defaults.bool(forKey: Unlock.cacheKey)
        updates = Task { [weak self] in
            // A purchase can land while the app is open, or from another device.
            for await update in Transaction.updates {
                if case .verified(let transaction) = update {
                    await transaction.finish()
                }
                await self?.refresh()
            }
        }
    }

    /// True when another ritual can be created without paying.
    func canCreate(existing: Int) -> Bool {
        isUnlocked || existing < Unlock.freeLimit
    }

    /// The price to show. Never hardcoded — the storefront decides.
    var displayPrice: String { product?.displayPrice ?? "$4.99" }

    func load() async {
        await refresh()
        do {
            let products = try await Product.products(for: [Unlock.productID])
            product = products.first
        } catch {
            // No network, or the product isn't configured yet. The paywall
            // still explains itself; only the button needs a price.
            lastError = nil
        }
    }

    func purchase() async {
        guard let product else {
            lastError = "The store isn't reachable right now."
            return
        }
        purchasing = true
        defer { purchasing = false }
        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                if case .verified(let transaction) = verification {
                    await transaction.finish()
                    await refresh()
                } else {
                    lastError = "That purchase could not be verified."
                }
            case .userCancelled:
                lastError = nil
            case .pending:
                lastError = "Waiting on approval for that purchase."
            @unknown default:
                lastError = nil
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    func restore() async {
        do {
            try await AppStore.sync()
        } catch {
            lastError = error.localizedDescription
        }
        await refresh()
    }

    private func refresh() async {
        var owned = false
        for await entitlement in Transaction.currentEntitlements {
            if case .verified(let transaction) = entitlement,
               transaction.productID == Unlock.productID,
               transaction.revocationDate == nil {
                owned = true
            }
        }
        isUnlocked = owned
        defaults.set(owned, forKey: Unlock.cacheKey)
    }
}
