package com.ayan.ritual.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.MainActivity
import com.ayan.ritual.data.Habit
import com.ayan.ritual.data.HabitStore
import com.ayan.ritual.ui.Display
import com.ayan.ritual.ui.InkPill
import com.ayan.ritual.ui.Caps
import com.ayan.ritual.ui.CapsLabel
import com.ayan.ritual.ui.InkSoft
import com.ayan.ritual.ui.InkFaint
import com.ayan.ritual.ui.RitualTheme
import com.ayan.ritual.ui.toModel
import com.ayan.ritual.ui.RitualCard
import com.ayan.ritual.ui.Cream
import java.time.LocalDate

/**
 * Shown while a widget is being placed: pick which ritual this slab will carry.
 *
 * The activity starts with RESULT_CANCELED, so backing out leaves no orphan
 * widget on the home screen.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        HabitStore.ensureLoaded(this)
        com.ayan.ritual.render.Fonts.load(this)

        setContent {
            RitualTheme {
                ConfigScreen(
                    habits = HabitStore.habitsState.value,
                    onPick = ::bind,
                    onCreate = {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Coming back from creating a habit should show it immediately.
        HabitStore.ensureLoaded(this)
    }

    private fun bind(habit: Habit) {
        HabitStore.bindWidget(this, widgetId, habit.id)
        RitualWidgetProvider.render(this, AppWidgetManager.getInstance(this), widgetId)
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        )
        finish()
    }
}

@Composable
private fun ConfigScreen(
    habits: List<Habit>,
    onPick: (Habit) -> Unit,
    onCreate: () -> Unit
) {
    val today = LocalDate.now()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 18.dp)) {
                Text(
                    "Bind the widget",
                    style = Display.copy(fontSize = 28.sp, lineHeight = 30.sp)
                )
                Spacer(Modifier.height(10.dp))
                CapsLabel(
                    if (habits.isEmpty()) "Nothing to bind yet" else "Choose what it will track",
                    color = InkSoft
                )
            }
        }

        if (habits.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Create a ritual first, then place the widget again.",
                        style = com.ayan.ritual.ui.Body,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    InkPill("Open Ritual", onCreate, Modifier.fillMaxWidth())
                }
            }
        } else {
            items(habits, key = { it.id }) { habit ->
                val year = today.year
                Box(
                    Modifier
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .clickable { onPick(habit) }
                ) {
                    RitualCard(
                        model = habit.toModel(today, year),
                        height = 172.dp
                    )
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tap a card to bind it",
                    style = Caps.copy(color = InkFaint, fontSize = 10.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
