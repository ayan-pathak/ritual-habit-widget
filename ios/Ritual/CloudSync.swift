import Foundation
import FirebaseCore
import FirebaseFirestore
import WidgetKit

/**
 The cloud copy: one document per ritual under `users/{uid}/habits/{id}`.

 Local first, always. `HabitStore` stays the source of truth for the app and
 the widget — Firestore is a mirror that catches up. Nothing here may block
 marking a day, and a device with no network keeps working as it always did.

 A whole ritual is one document: a decade of epoch-days is a few kilobytes,
 well inside Firestore's limit, and it keeps a full sync to one read per ritual
 rather than one per day kept.

 Conflicts resolve the same way they do on Android, deliberately:

 - **On first link**, when a signed-out device carrying rituals meets an
   account that already has some, the days are unioned. That is the one moment
   where picking a winner costs someone squares they really kept.
 - **After that**, last write wins per ritual. Two devices editing the same
   ritual within the minute is rare; losing a year to a clever merge is not
   recoverable.
 */
@MainActor
final class CloudSync {

    static let shared = CloudSync()

    private var listener: ListenerRegistration?
    private var linkedUID: String?
    private var pendingUnion = true

    private init() {}

    private var database: Firestore? {
        FirebaseApp.app() == nil ? nil : Firestore.firestore()
    }

    /// Starts mirroring for `uid`. Calling twice for the same account does nothing.
    func start(uid: String) {
        guard linkedUID != uid || listener == nil else { return }
        stop()
        guard let database else { return }
        linkedUID = uid
        pendingUnion = true

        listener = database.collection("users").document(uid).collection("habits")
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let self, let snapshot else { return }
                Task { @MainActor in
                    self.apply(snapshot.documents)
                }
            }
    }

    func stop() {
        listener?.remove()
        listener = nil
        linkedUID = nil
    }

    private func apply(_ documents: [QueryDocumentSnapshot]) {
        let store = HabitStore.shared
        store.ensureLoaded()

        var remote: [Habit] = []
        for doc in documents {
            let data = doc.data()
            if data["deleted"] as? Bool == true { continue }
            let days = (data["done"] as? [Int]) ?? (data["done"] as? [NSNumber])?.map(\.intValue) ?? []
            remote.append(Habit(
                id: data["id"] as? String ?? doc.documentID,
                name: data["name"] as? String ?? "Untitled",
                slot: data["slot"] as? String ?? "Daily",
                accentIndex: (data["accent"] as? NSNumber)?.intValue ?? 0,
                createdEpochDay: (data["created"] as? NSNumber)?.intValue ?? 0,
                done: Set(days)
            ))
        }

        let merged = CloudSync.merge(local: store.habits, remote: remote, union: pendingUnion)
        pendingUnion = false
        if merged != store.habits {
            store.replaceAll(merged)
            WidgetCenter.shared.reloadAllTimelines()
        }
        pushAll()
    }

    /// Writes every local ritual up. Cheap: one small document each.
    func pushAll() {
        guard let database, let uid = linkedUID else { return }
        let store = HabitStore.shared
        store.ensureLoaded()
        let collection = database.collection("users").document(uid).collection("habits")
        let stamp = Int(Date().timeIntervalSince1970 * 1000)
        for habit in store.habits {
            collection.document(habit.id).setData([
                "id": habit.id,
                "name": habit.name,
                "slot": habit.slot,
                "accent": habit.accentIndex,
                "created": habit.createdEpochDay,
                "done": habit.done.sorted(),
                "updatedAt": stamp,
                "deleted": false
            ])
        }
    }

    /// A deleted ritual is tombstoned, so the other device agrees.
    func markDeleted(id: String) {
        guard let database, let uid = linkedUID else { return }
        database.collection("users").document(uid).collection("habits").document(id)
            .setData([
                "id": id,
                "deleted": true,
                "updatedAt": Int(Date().timeIntervalSince1970 * 1000)
            ])
    }

    static func merge(local: [Habit], remote: [Habit], union: Bool) -> [Habit] {
        var byID = Dictionary(uniqueKeysWithValues: local.map { ($0.id, $0) })
        for habit in remote {
            if let mine = byID[habit.id] {
                if union {
                    var merged = habit
                    merged.done = mine.done.union(habit.done)
                    byID[habit.id] = merged
                } else {
                    byID[habit.id] = habit
                }
            } else {
                byID[habit.id] = habit
            }
        }
        return byID.values.sorted { $0.createdEpochDay < $1.createdEpochDay }
    }
}
