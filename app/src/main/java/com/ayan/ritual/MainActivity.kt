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
import com.ayan.ritual.render.Cat
import com.ayan.ritual.render.Fonts
import com.ayan.ritual.ui.CreateScreen
import com.ayan.ritual.ui.DetailScreen
import com.ayan.ritual.ui.AccountScreen
import com.ayan.ritual.ui.HomeScreen
import com.ayan.ritual.ui.PaywallScreen
import com.ayan.ritual.ui.RitualTheme
import com.ayan.ritual.ui.WelcomeScreen
import com.ayan.ritual.widget.RitualWidgetProvider

sealed interface Route {
    data object Welcome : Route
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
        Fonts.load(this)
        Cat.load(this)
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
 * The sign-in step is only in the way while all three are true: there is a
 * Firebase project to sign in to, nobody is signed in on this device, and
 * nobody has said they would rather not be. Any one of those failing and the
 * app opens on the grid, which is what it is for.
 */
private fun firstRoute(): Route =
    if (Account.available && !Account.signedIn && !Onboarding.skippedSignIn) Route.Welcome
    else Route.Home

@Composable
private fun RitualApp(openHabitId: String?, onConsumed: () -> Unit) {
    var route by remember { mutableStateOf(firstRoute()) }

    // The unlock is offered once, on the way out of the welcome, and never
    // unprompted again — a second ritual asks for itself when it is wanted.
    fun leaveWelcome() {
        route = if (!Unlock.unlocked && !Onboarding.sawPaywall) {
            Onboarding.markSawPaywall()
            Route.Paywall
        } else {
            Route.Home
        }
    }

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
            onSignedIn = { leaveWelcome() },
            onSkip = { leaveWelcome() }
        )

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
