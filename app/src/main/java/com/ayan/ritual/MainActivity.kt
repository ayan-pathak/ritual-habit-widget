package com.ayan.ritual

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ayan.ritual.billing.Unlock
import com.ayan.ritual.cloud.Account
import com.ayan.ritual.cloud.CloudSync
import com.ayan.ritual.data.HabitStore
import com.ayan.ritual.data.Onboarding
import com.ayan.ritual.render.Fonts
import com.ayan.ritual.ui.CreateScreen
import com.ayan.ritual.ui.DetailScreen
import com.ayan.ritual.ui.AccountScreen
import com.ayan.ritual.ui.HomeScreen
import com.ayan.ritual.ui.PaywallScreen
import com.ayan.ritual.ui.RitualTheme
import com.ayan.ritual.ui.TourScreen
import com.ayan.ritual.ui.WelcomeScreen
import com.ayan.ritual.widget.RitualWidgetProvider
import java.time.LocalDate

sealed interface Route {
    data object Welcome : Route
    data object Tour : Route
    data object Home : Route
    data class Detail(val habitId: String) : Route
    data object Create : Route
    data object Paywall : Route
    data object Account : Route
}

class MainActivity : ComponentActivity() {

    private var pendingHabitId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        HabitStore.ensureLoaded(this)
        Onboarding.load(this)
        com.ayan.ritual.ui.Look.load(this)
        Fonts.load(this)
        Unlock.start(this)
        Account.start()

        // The cloud copy follows the device, never the other way round: every
        // local write is mirrored after it has already landed on disk.
        HabitStore.onChanged = { CloudSync.pushAll(applicationContext) }
        Account.uid?.let { CloudSync.start(this, it) }
        pendingHabitId.value = intent?.getStringExtra(RitualWidgetProvider.EXTRA_HABIT_ID)

        setContent {
            RitualTheme {
                RitualApp(openHabitId = pendingHabitId.value, onConsumed = { pendingHabitId.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingHabitId.value = intent.getStringExtra(RitualWidgetProvider.EXTRA_HABIT_ID)
    }

    override fun onResume() {
        super.onResume()
        // A purchase can land outside the app — on another device, or in Play
        // itself. Ask again every time we come forward.
        Unlock.refresh()
    }

    override fun onStop() {
        super.onStop()
        // Whatever changed in here, the home screen should agree with it.
        RitualWidgetProvider.refreshAll(this)
    }
}

/**
 * Where a launch lands.
 *
 * Three gates, in the order they were passed. Sign-in is the way in rather
 * than an offer on the way past, so a launch stops there whenever there is a
 * Firebase project and nobody is signed in; without one — a checkout with no
 * google-services.json — there is nothing to sign in to and it falls through.
 * Then the tour, once. Then the unlock, but only on a launch that finds a
 * streak already worth keeping, which is the whole point of not asking
 * sooner.
 */
private fun firstRoute(): Route = when {
    Account.available && !Account.signedIn -> Route.Welcome
    !Onboarding.sawTour -> Route.Tour
    Onboarding.unlockIsWorthMentioning(LocalDate.now()) && !Unlock.unlocked -> {
        Onboarding.markSawPaywall()
        Route.Paywall
    }
    else -> Route.Home
}

@Composable
private fun RitualApp(openHabitId: String?, onConsumed: () -> Unit) {
    var route by remember { mutableStateOf(firstRoute()) }

    // A tap on the widget lands straight on that habit.
    remember(openHabitId) {
        if (openHabitId != null) {
            route = Route.Detail(openHabitId)
            onConsumed()
        }
        openHabitId
    }

    val habits by HabitStore.habitsState

    when (val r = route) {
        is Route.Welcome -> WelcomeScreen(
            onSignedIn = { route = if (!Onboarding.sawTour) Route.Tour else Route.Home }
        )

        is Route.Tour -> TourScreen(onDone = { route = Route.Home })

        is Route.Home -> HomeScreen(
            habits = habits,
            onOpen = { route = Route.Detail(it.id) },
            onCreate = { route = Route.Create },
            onPaywall = { route = Route.Paywall },
            onAccount = { route = Route.Account }
        )

        is Route.Paywall -> PaywallScreen(onClose = { route = Route.Home })

        is Route.Account -> AccountScreen(onBack = { route = Route.Home })

        is Route.Create -> CreateScreen(
            onDone = { route = Route.Detail(it.id) },
            onBack = { route = Route.Home }
        )

        is Route.Detail -> {
            val habit = habits.firstOrNull { it.id == r.habitId }
            if (habit == null) {
                route = Route.Home
            } else {
                DetailScreen(
                    habit = habit,
                    onBack = { route = Route.Home }
                )
            }
        }
    }
}
