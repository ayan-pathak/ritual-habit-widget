package com.ayan.ritual.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/**
 * The single source of truth, shared by the UI and the widget receiver.
 *
 * Both live in the same process but the receiver can start it cold, so every
 * public entry point calls [ensureLoaded] first rather than trusting an init.
 */
object HabitStore {

    private const val PREFS = "ritual_store"
    private const val KEY_HABITS = "habits_v1"
    private const val KEY_WIDGETS = "widget_bindings_v1"

    private val _habits = mutableStateOf<List<Habit>>(emptyList())
    val habits: List<Habit> get() = _habits.value

    /** Compose reads this to recompose on change. */
    val habitsState get() = _habits

    private var loaded = false
    private var bindings: MutableMap<Int, String> = mutableMapOf()

    /**
     * Called after every local change, so a mirror can follow.
     *
     * Nothing set here may block or fail a write: the device's own store is
     * the source of truth, and marking a day has to work with no network and
     * no account.
     */
    var onChanged: (() -> Unit)? = null

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (loaded) return
        val p = prefs(context)
        _habits.value = decodeHabits(p.getString(KEY_HABITS, null))
        bindings = decodeBindings(p.getString(KEY_WIDGETS, null))
        loaded = true
    }

    fun get(context: Context, id: String?): Habit? {
        ensureLoaded(context)
        if (id == null) return null
        return _habits.value.firstOrNull { it.id == id }
    }

    fun create(context: Context, name: String, slot: String, accentIndex: Int): Habit {
        ensureLoaded(context)
        val habit = Habit(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "Untitled" },
            slot = slot.trim().ifEmpty { "Daily" },
            accentIndex = accentIndex,
            createdEpochDay = LocalDate.now().toEpochDay(),
            done = emptySet()
        )
        _habits.value = _habits.value + habit
        persist(context)
        return habit
    }

    /** Replaces everything, for a merge arriving from the cloud. */
    fun replaceAll(context: Context, habits: List<Habit>) {
        ensureLoaded(context)
        _habits.value = habits
        prefs(context).edit()
            .putString(KEY_HABITS, encodeHabits(habits))
            .putString(KEY_WIDGETS, encodeBindings(bindings))
            .apply()
    }

    fun update(context: Context, habit: Habit) {
        ensureLoaded(context)
        _habits.value = _habits.value.map { if (it.id == habit.id) habit else it }
        persist(context)
    }

    fun delete(context: Context, id: String) {
        ensureLoaded(context)
        _habits.value = _habits.value.filterNot { it.id == id }
        bindings.entries.removeAll { it.value == id }
        persist(context)
    }

    /** Flips [date] for [id]. Returns the habit's new state, or null if it's gone. */
    fun toggle(context: Context, id: String, date: LocalDate): Habit? {
        ensureLoaded(context)
        val habit = _habits.value.firstOrNull { it.id == id } ?: return null
        val day = date.toEpochDay()
        val next = habit.copy(
            done = if (habit.done.contains(day)) habit.done - day else habit.done + day
        )
        update(context, next)
        return next
    }

    // ── Widget bindings ─────────────────────────────────────────────────────

    fun habitForWidget(context: Context, widgetId: Int): Habit? {
        ensureLoaded(context)
        val bound = bindings[widgetId]?.let { id -> _habits.value.firstOrNull { it.id == id } }
        // An unbound (or orphaned) widget falls back to the first habit so it is
        // never a blank rectangle on someone's home screen.
        return bound ?: _habits.value.firstOrNull()
    }

    fun bindWidget(context: Context, widgetId: Int, habitId: String) {
        ensureLoaded(context)
        bindings[widgetId] = habitId
        persist(context)
    }

    fun unbindWidgets(context: Context, widgetIds: IntArray) {
        ensureLoaded(context)
        widgetIds.forEach { bindings.remove(it) }
        persist(context)
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    private fun persist(context: Context) {
        prefs(context).edit()
            .putString(KEY_HABITS, encodeHabits(_habits.value))
            .putString(KEY_WIDGETS, encodeBindings(bindings))
            .apply()
        onChanged?.invoke()
    }

    private fun encodeHabits(list: List<Habit>): String {
        val arr = JSONArray()
        for (h in list) {
            val days = JSONArray()
            h.done.sorted().forEach { days.put(it) }
            arr.put(
                JSONObject()
                    .put("id", h.id)
                    .put("name", h.name)
                    .put("slot", h.slot)
                    .put("accent", h.accentIndex)
                    .put("created", h.createdEpochDay)
                    .put("done", days)
            )
        }
        return arr.toString()
    }

    private fun decodeHabits(raw: String?): List<Habit> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val daysArr = o.optJSONArray("done") ?: JSONArray()
                val days = HashSet<Long>(daysArr.length())
                for (j in 0 until daysArr.length()) days.add(daysArr.getLong(j))
                Habit(
                    id = o.getString("id"),
                    name = o.optString("name", "Untitled"),
                    slot = o.optString("slot", "Daily"),
                    accentIndex = o.optInt("accent", 0),
                    createdEpochDay = o.optLong("created", LocalDate.now().toEpochDay()),
                    done = days
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeBindings(map: Map<Int, String>): String {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k.toString(), v) }
        return o.toString()
    }

    private fun decodeBindings(raw: String?): MutableMap<Int, String> {
        if (raw.isNullOrBlank()) return mutableMapOf()
        return runCatching {
            val o = JSONObject(raw)
            val out = mutableMapOf<Int, String>()
            o.keys().forEach { k -> out[k.toInt()] = o.getString(k) }
            out
        }.getOrDefault(mutableMapOf())
    }
}
