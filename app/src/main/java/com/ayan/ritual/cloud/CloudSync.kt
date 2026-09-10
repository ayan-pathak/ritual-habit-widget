package com.ayan.ritual.cloud

import android.content.Context
import com.ayan.ritual.data.Habit
import com.ayan.ritual.data.HabitStore
import com.ayan.ritual.widget.RitualWidgetProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * The cloud copy: one document per ritual under `users/{uid}/habits/{id}`.
 *
 * Local first, always. The device's own store stays the source of truth for
 * the UI and the widget — Firestore is a mirror that catches up. Nothing in
 * here is allowed to block marking a day, and a device with no network keeps
 * working exactly as it always did.
 *
 * A whole ritual is one document: a decade of epoch-days is a few kilobytes,
 * well inside Firestore's limit, and it keeps a full sync to one read per
 * ritual rather than one per day kept.
 *
 * Two rules decide conflicts:
 *
 * - **On first link**, when a signed-out device with rituals meets an account
 *   that already has some, the two are unioned. That is the moment where
 *   clobbering costs someone real days, so it is the one moment we never
 *   resolve by picking a winner.
 * - **After that**, last write wins per ritual, on `updatedAt`. Two phones
 *   editing the same ritual in the same minute is rare; losing a year to a
 *   merge bug is not recoverable.
 */
object CloudSync {

    private const val USERS = "users"
    private const val HABITS = "habits"

    private var registration: ListenerRegistration? = null
    private var linkedUid: String? = null

    private fun db(): FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    /**
     * Starts mirroring for [uid]. Call on sign-in and on launch while signed
     * in; calling twice for the same account does nothing.
     */
    fun start(context: Context, uid: String) {
        if (linkedUid == uid && registration != null) return
        stop()
        linkedUid = uid
        val store = db() ?: return
        HabitStore.ensureLoaded(context)

        val collection = store.collection(USERS).document(uid).collection(HABITS)

        // First read of this account decides whether we are linking or merely
        // catching up. `pendingUnion` is only true for that first snapshot.
        var pendingUnion = true

        registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val remote = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val days = (doc.get("done") as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toLong() }
                    ?.toSet() ?: emptySet()
                if (doc.getBoolean("deleted") == true) return@mapNotNull null
                Habit(
                    id = id,
                    name = doc.getString("name") ?: "Untitled",
                    slot = doc.getString("slot") ?: "Daily",
                    accentIndex = (doc.getLong("accent") ?: 0L).toInt(),
                    createdEpochDay = doc.getLong("created") ?: 0L,
                    done = days
                ) to (doc.getLong("updatedAt") ?: 0L)
            }

            val merged = merge(HabitStore.habits, remote, union = pendingUnion)
            pendingUnion = false
            if (merged != HabitStore.habits) {
                HabitStore.replaceAll(context, merged)
                RitualWidgetProvider.refreshAll(context)
            }
            // Anything the device knows that the cloud doesn't, push up.
            pushAll(context, uid)
        }
    }

    fun stop() {
        registration?.remove()
        registration = null
        linkedUid = null
    }

    /** Writes every local ritual up. Cheap: one small document each. */
    fun pushAll(context: Context, uid: String? = linkedUid) {
        val target = uid ?: return
        val store = db() ?: return
        HabitStore.ensureLoaded(context)
        val collection = store.collection(USERS).document(target).collection(HABITS)
        val stamp = System.currentTimeMillis()
        HabitStore.habits.forEach { habit ->
            collection.document(habit.id).set(
                mapOf(
                    "id" to habit.id,
                    "name" to habit.name,
                    "slot" to habit.slot,
                    "accent" to habit.accentIndex,
                    "created" to habit.createdEpochDay,
                    "done" to habit.done.sorted(),
                    "updatedAt" to stamp,
                    "deleted" to false
                )
            )
        }
    }

    /** A deleted ritual is tombstoned, so the other device stops showing it. */
    fun markDeleted(id: String) {
        val uid = linkedUid ?: return
        val store = db() ?: return
        store.collection(USERS).document(uid).collection(HABITS).document(id)
            .set(mapOf("id" to id, "deleted" to true, "updatedAt" to System.currentTimeMillis()))
    }

    internal fun merge(
        local: List<Habit>,
        remote: List<Pair<Habit, Long>>,
        union: Boolean
    ): List<Habit> {
        val byId = local.associateBy { it.id }.toMutableMap()
        for ((habit, _) in remote) {
            val mine = byId[habit.id]
            byId[habit.id] = when {
                mine == null -> habit
                union -> habit.copy(done = mine.done + habit.done)
                else -> habit
            }
        }
        return byId.values.sortedBy { it.createdEpochDay }
    }
}
