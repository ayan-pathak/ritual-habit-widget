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

    /**
     * The stamp this device last wrote for each ritual.
     *
     * Firestore replays a write to its own listener twice, once locally and
     * again when the server acks it, and those echoes can arrive after the
     * next edit has already been made. An echo older than what we have
     * written is our own past, and applying it drags the ritual backwards.
     */
    private val lastPushed = mutableMapOf<String, Long>()

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
                val stamp = doc.getLong("updatedAt") ?: 0L
                if (stamp < (lastPushed[id] ?: 0L)) return@mapNotNull null
                val days = (doc.get("done") as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toLong() }
                    ?.toSet() ?: emptySet()
                if (doc.getBoolean("deleted") == true) return@mapNotNull null
                val created = doc.getLong("created") ?: 0L
                Habit(
                    id = id,
                    name = doc.getString("name") ?: "Untitled",
                    slot = doc.getString("slot") ?: "Daily",
                    accentIndex = (doc.getLong("accent") ?: 0L).toInt(),
                    createdEpochDay = created,
                    done = days,
                    // A document written by an older build carries none of
                    // these, and must read back as a habit with no identity
                    // rather than as one whose identity was cleared.
                    identity = doc.getString("identity") ?: "",
                    goalStartEpochDay = doc.getLong("goalStart") ?: created,
                    builtEpochDay = doc.getLong("built")
                ) to stamp
            }

            val linking = pendingUnion
            val merged = merge(HabitStore.habits, remote, union = linking)
            pendingUnion = false
            if (merged != HabitStore.habits) {
                HabitStore.replaceAll(context, merged)
                RitualWidgetProvider.refreshAll(context)
            }

            // Only the first look pushes the whole device up, because only the
            // first look is the link. After that a push is what a local edit
            // does, through HabitStore.onChanged.
            //
            // Pushing here on every snapshot is a loop with no floor: the push
            // makes Firestore call this listener, which pushes, as fast as the
            // network allows for as long as the app is open. It also meant a
            // day someone had just marked could be overwritten by an echo of
            // the write before it, which is the one thing this file is not
            // allowed to do.
            if (linking) {
                pushAll(context, uid)
            } else {
                val known = snapshot.documents.map { it.getString("id") ?: it.id }.toSet()
                val strangers = HabitStore.habits.filter { it.id !in known }
                if (strangers.isNotEmpty()) write(uid, strangers)
            }
        }
    }

    fun stop() {
        registration?.remove()
        registration = null
        linkedUid = null
        lastPushed.clear()
    }

    /** Writes every local ritual up. Cheap: one small document each. */
    fun pushAll(context: Context, uid: String? = linkedUid) {
        val target = uid ?: return
        HabitStore.ensureLoaded(context)
        write(target, HabitStore.habits)
    }

    private fun write(uid: String, habits: List<Habit>) {
        if (habits.isEmpty()) return
        val store = db() ?: return
        val collection = store.collection(USERS).document(uid).collection(HABITS)
        val stamp = System.currentTimeMillis()
        habits.forEach { habit ->
            lastPushed[habit.id] = stamp
            collection.document(habit.id).set(
                mapOf(
                    "id" to habit.id,
                    "name" to habit.name,
                    "slot" to habit.slot,
                    "accent" to habit.accentIndex,
                    "created" to habit.createdEpochDay,
                    "identity" to habit.identity,
                    "goalStart" to habit.goalStartEpochDay,
                    "built" to habit.builtEpochDay,
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
        val stamp = System.currentTimeMillis()
        lastPushed[id] = stamp
        store.collection(USERS).document(uid).collection(HABITS).document(id)
            .set(mapOf("id" to id, "deleted" to true, "updatedAt" to stamp))
    }

    /**
     * Removes the whole cloud copy of an account: every ritual document under
     * `users/{uid}/habits`, and the user document above them.
     *
     * It runs while that account is still signed in, because the rules only
     * ever let an account near its own path — delete the user first and what
     * is left is unreachable rather than gone, which is the one outcome a
     * deletion promise cannot survive.
     *
     * The listener is stopped first so that nothing pushes the device's own
     * rituals back up between the read and the delete.
     */
    fun deleteEverything(uid: String, onDone: (Boolean) -> Unit) {
        val store = db() ?: return onDone(false)
        stop()
        val user = store.collection(USERS).document(uid)
        user.collection(HABITS).get()
            .addOnSuccessListener { snapshot ->
                val refs = snapshot.documents.map { it.reference } + user
                // Firestore caps a batch at 500 writes. Nobody has 500
                // rituals, but a promise to delete everything should not have
                // a number in it.
                val batches = refs.chunked(400)
                var left = batches.size
                var ok = true
                batches.forEach { chunk ->
                    val batch = store.batch()
                    chunk.forEach { batch.delete(it) }
                    batch.commit().addOnCompleteListener { task ->
                        if (!task.isSuccessful) ok = false
                        if (--left == 0) onDone(ok)
                    }
                }
            }
            .addOnFailureListener { onDone(false) }
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
