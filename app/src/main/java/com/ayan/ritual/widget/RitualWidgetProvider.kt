package com.ayan.ritual.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.ayan.ritual.MainActivity
import com.ayan.ritual.R
import com.ayan.ritual.data.Habit
import com.ayan.ritual.data.HabitStore
import com.ayan.ritual.data.Moments
import com.ayan.ritual.render.SlabModel
import com.ayan.ritual.render.SlabRenderer
import com.ayan.ritual.render.Cat
import com.ayan.ritual.render.Fonts
import com.ayan.ritual.render.Mood
import com.ayan.ritual.render.accentAt
import java.time.LocalDate

/**
 * The slab on the home screen.
 *
 * The whole face is a bitmap drawn by [SlabRenderer] — the same code the app
 * uses — with two transparent hit targets laid over it: the field opens the
 * app, the pill marks today without leaving the home screen.
 */
class RitualWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // The periodic update is the widget's only clock, so it is when he
        // drops by: now and then a word about where the ritual stands, left
        // up until the next one. Unmarked, a nudge; kept, the count.
        val today = LocalDate.now()
        val until = System.currentTimeMillis() + NUDGE_MILLIS
        ids.forEach { id ->
            val habit = HabitStore.habitForWidget(context, id)
            val say = if (habit == null || Math.random() >= NUDGE_CHANCE) null
                else if (habit.isDone(today)) listOf(
                    Moments.keptLine(habit, today), "see you tomorrow!", "${habit.totalDone} lit!"
                ).random()
                else listOf("today’s square?", "psst… today?", "ready for today?").random()
            Moments.setWidgetSay(context, id, say, until)
            render(context, manager, id)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        newOptions: android.os.Bundle
    ) {
        render(context, manager, widgetId)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        HabitStore.unbindWidgets(context, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_TODAY -> {
                val widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                val today = LocalDate.now()
                val next = HabitStore.habitForWidget(context, widgetId)?.let { habit ->
                    HabitStore.toggle(context, habit.id, today)
                }
                // Kept from the widget: he says the count beside his box for a
                // few seconds. A tenth day past the thirty is worth a post,
                // which the app asks about when it next opens.
                if (next != null && next.isDone(today)) {
                    val brag = Moments.bragDue(context, next, today)
                    if (brag > 0) Moments.setPendingBrag(context, next.id, brag)
                    val say = if (brag > 0) "$brag! post it?" else Moments.keptLine(next, today)
                    Moments.setWidgetSay(context, widgetId, say, System.currentTimeMillis() + SAY_MILLIS)
                    val pending = goAsync()
                    Handler(Looper.getMainLooper()).postDelayed({
                        render(context, AppWidgetManager.getInstance(context), widgetId)
                        pending.finish()
                    }, SAY_MILLIS + 100L)
                } else {
                    Moments.setWidgetSay(context, widgetId, null, 0L)
                }
                refreshAll(context)
            }

            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> refreshAll(context)
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_TOGGLE_TODAY = "com.ayan.ritual.ACTION_TOGGLE_TODAY"
        const val EXTRA_HABIT_ID = "com.ayan.ritual.HABIT_ID"

        /** How long he says the count after a day is kept from the widget. */
        private const val SAY_MILLIS = 6000L
        /** How often a periodic update brings him by, and how long he stays. */
        private const val NUDGE_CHANCE = 0.4
        private const val NUDGE_MILLIS = 25L * 60L * 1000L

        /** Largest bitmap we hand to RemoteViews; comfortably inside the transaction limit. */
        private const val MAX_PIXELS = 1_400_000

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, RitualWidgetProvider::class.java)
            )
            ids.forEach { render(context, manager, it) }
        }

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            HabitStore.ensureLoaded(context)
            Fonts.load(context)
            val habit = HabitStore.habitForWidget(context, widgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_ritual)

            val density = context.resources.displayMetrics.density
            val options = manager.getAppWidgetOptions(widgetId)
            val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                .takeIf { it > 0 } ?: 300
            val hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
                .takeIf { it > 0 } ?: 140

            var wPx = (wDp * density).toInt().coerceAtLeast(160)
            var hPx = (hDp * density).toInt().coerceAtLeast(90)
            val pixels = wPx.toLong() * hPx.toLong()
            if (pixels > MAX_PIXELS) {
                val scale = Math.sqrt(MAX_PIXELS.toDouble() / pixels).toFloat()
                wPx = (wPx * scale).toInt()
                hPx = (hPx * scale).toInt()
            }
            // The bitmap may be downscaled, but it keeps the widget's aspect ratio and
            // is drawn with fitXY, so the pill still sits under its hit target.
            val renderDensity = density * (wPx.toFloat() / (wDp * density))

            val today = LocalDate.now()
            val year = today.year
            val model = if (habit != null) {
                SlabModel(
                    title = habit.name,
                    slot = habit.slot,
                    accent = accentAt(habit.accentIndex),
                    year = year,
                    today = today,
                    doneDaysOfYear = habit.daysOfYear(year),
                    streak = habit.streak(today),
                    totalDone = habit.totalIn(year),
                    remaining = Habit.remainingIn(year, today),
                    doneToday = habit.isDone(today),
                    mood = Cat.moodFor(habit.isDone(today), habit.streak(today), habit.missedYesterday(today))
                )
            } else {
                SlabModel(
                    title = "Open Ritual",
                    slot = "Not bound",
                    accent = accentAt(0),
                    year = year,
                    today = today,
                    doneDaysOfYear = emptySet(),
                    streak = 0,
                    totalDone = 0,
                    remaining = Habit.remainingIn(year, today),
                    doneToday = false,
                    mood = Mood.RESTING
                )
            }

            val bitmap = SlabRenderer.render(
                wPx, hPx, model,
                SlabRenderer.Config(
                    density = renderDensity,
                    action = habit != null,
                    cornerDp = 24f,
                    padDp = 15f,
                    say = if (habit != null) Moments.widgetSay(context, widgetId, System.currentTimeMillis()) else null
                )
            )
            views.setImageViewBitmap(R.id.slab, bitmap)
            views.setContentDescription(
                R.id.tap_grid,
                "${model.title}. ${model.totalDone} days lit, ${model.remaining} squares left in $year."
            )

            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_HABIT_ID, habit?.id)
            }
            views.setOnClickPendingIntent(
                R.id.tap_grid,
                PendingIntent.getActivity(
                    context, widgetId, open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            if (habit != null) {
                val toggle = Intent(context, RitualWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_TODAY
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                views.setOnClickPendingIntent(
                    R.id.tap_action,
                    PendingIntent.getBroadcast(
                        context, widgetId, toggle,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

            manager.updateAppWidget(widgetId, views)
        }
    }
}
