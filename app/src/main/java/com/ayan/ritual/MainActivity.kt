package com.ayan.ritual

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.ayan.ritual.billing.Unlock
import com.ayan.ritual.cloud.Account
import com.ayan.ritual.cloud.CloudSync
import com.ayan.ritual.data.HabitStore
import com.ayan.ritual.data.Onboarding
import com.ayan.ritual.render.Fonts
import com.ayan.ritual.ui.CreateScreen
import com.ayan.ritual.ui.DetailScreen
import com.ayan.ritual.ui.AccountScreen
import com.ayan.ritual.ui.BuiltScreen
import com.ayan.ritual.ui.HomeScreen
import com.ayan.ritual.ui.OnboardingFlow
import com.ayan.ritual.ui.PaywallReason
import com.ayan.ritual.ui.PaywallScreen
import com.ayan.ritual.ui.RitualTheme
import com.ayan.ritual.ui.ShelfScreen
import com.ayan.ritual.ui.WelcomeScreen
import com.ayan.ritual.widget.RitualWidgetProvider
import java.time.LocalDate

sealed interface Route {
    data object Welcome : Route
    /** The one way in: look, name, sentence, bet, ritual, widget, story. */
    data object Onboarding : Route
    data object Home : Route
    data class Detail(val habitId: String) : Route
    /** Creating the ritual that builds [identity], which is blank when a
        later one is added from Home. */
    data class Create(val identity: String = "") : Route
    data class Paywall(val reason: PaywallReason = PaywallReason.ANOTHER) : Route
    data object Account : Route
    /** Thirty days cleared, waiting to be claimed. */
    data class Built(val habitId: String) : Route
    /** Every sentence that is now true, with its evidence. */
    data object Shelf : Route
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
        // local write is mirrored after it has already landed on disk. Whether
        // there is a cloud copy at all is decided in RitualApp: backup is part
        // of the unlock, so it runs only for someone signed in who has it.
        HabitStore.onChanged = { CloudSync.pushAll(applicationContext) }
        pendingHabitId.value = intent?.getStringExtra(RitualWidgetProvider.EXTRA_HABIT_ID)

        setContent {
            // The bars are transparent, so their icons have to follow the
            // appearance rather than the theme file, which only ever sees the
            // dark default.
            val lightBars = com.ayan.ritual.ui.Look.appearance == com.ayan.ritual.ui.Appearance.LIGHT
            LaunchedEffect(lightBars) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
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
 * Three gates, in the order they were passed. Sign-in is the first thing
 * offered, because an account is what carries the squares to the next phone
 * and the moment to say so is before there are any. It is an offer and not a
 * wall: a launch stops there when there is a Firebase project, nobody is
 * signed in, and nobody has waved it off. Without a project — a checkout with
 * no google-services.json — there is nothing to sign in to and it falls
 * through; waved off once, it falls through the same way and lives in Account
 * from then on. Then the onboarding flow, resumed wherever it was left. Then the unlock, but only on a launch
 * that finds a streak already worth keeping, which is the whole point of not
 * asking sooner.
 */
private fun firstRoute(): Route = when {
    Account.available && !Account.signedIn && !Onboarding.skippedSignIn -> Route.Welcome
    // One flow, resumed wherever it was left: nothing is asked twice.
    Onboarding.needsFlow(HabitStore.habits.isNotEmpty()) -> Route.Onboarding
    Onboarding.unlockIsWorthMentioning(LocalDate.now()) && !Unlock.unlocked -> {
        Onboarding.markSawPaywall()
        Route.Paywall()
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
    val context = LocalContext.current

    // Backup: signed in *and* unlocked. Signing in alone is an account, not a
    // backup; the squares stay on this phone until the unlock turns it on.
    val uid by Account.uidState
    val unlocked by Unlock.unlockedState
    LaunchedEffect(uid, unlocked) {
        val id = uid
        if (id != null && unlocked) CloudSync.start(context, id) else CloudSync.stop()
    }

    // Thirty days that were cleared and never claimed. Checked here rather
    // than inside the screen that marked the day, so it also catches someone
    // who was away on day thirty and comes back on day thirty five.
    LaunchedEffect(habits) {
        val today = LocalDate.now()
        val ready = habits.firstOrNull { !it.isBuilt && it.goalMet(today) }
        // Never over the way in, or over a purchase someone is in the middle of.
        val busy = route is Route.Built || route is Route.Onboarding || route is Route.Paywall
        if (ready != null && !busy) route = Route.Built(ready.id)
    }

    when (val r = route) {
        is Route.Welcome -> WelcomeScreen(
            onSignedIn = { route = firstRoute() },
            onSkip = {
                Onboarding.markSkippedSignIn()
                route = firstRoute()
            }
        )

        is Route.Onboarding -> OnboardingFlow(
            habits = habits,
            onFinished = { id ->
                Onboarding.finishFlow()
                route = id?.let { Route.Detail(it) } ?: Route.Home
            }
        )

        is Route.Home -> HomeScreen(
            habits = habits,
            onOpen = { route = Route.Detail(it.id) },
            onCreate = { route = Route.Create() },
            onPaywall = { route = Route.Paywall() },
            onAccount = { route = Route.Account },
            onShelf = { route = Route.Shelf }
        )

        is Route.Paywall -> PaywallScreen(reason = r.reason, onClose = { route = Route.Home })

        is Route.Account -> AccountScreen(
            onBack = { route = Route.Home },
            onBackup = { route = Route.Paywall(PaywallReason.BACKUP) }
        )

        is Route.Built -> {
            val habit = habits.firstOrNull { it.id == r.habitId }
            if (habit == null) {
                route = Route.Home
            } else {
                BuiltScreen(
                    habit = habit,
                    onClaim = {
                        HabitStore.markBuilt(context, habit.id)
                        route = Route.Shelf
                    },
                    // Not now is not never: it stays unclaimed and the check
                    // above will offer it again on the next launch.
                    onLater = { route = Route.Home }
                )
            }
        }

        is Route.Shelf -> ShelfScreen(
            habits = habits,
            onBack = { route = Route.Home },
            onShare = { }
        )

        is Route.Create -> CreateScreen(
            identity = r.identity,
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
                    onBack = { route = Route.Home },
                    onPaywall = { reason -> route = Route.Paywall(reason) }
                )
            }
        }
    }
}
