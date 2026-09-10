package com.ayan.ritual.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.billing.Unlock
import com.ayan.ritual.render.Mood

private val INCLUDED = listOf(
    "As many rituals as you keep",
    "A widget for each one",
    "Every year you have kept, in the archive",
    "Story cards without a watermark"
)

/**
 * The wall, and the only one in the app.
 *
 * It shows up when someone reaches for a second ritual — the moment the app
 * has already proved itself — and never on launch, never over a day waiting to
 * be marked. Everything already kept stays free and stays visible.
 */
@Composable
fun PaywallScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val unlocked by Unlock.unlockedState
    val price = Unlock.price ?: "$4.99"

    if (unlocked) onClose()

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp),
            horizontalArrangement = Arrangement.End
        ) {
            RoundButton(onClick = onClose, size = 40.dp) {
                androidx.compose.foundation.Canvas(Modifier.size(18.dp)) {
                    drawLine(Ink, Offset(size.width * .22f, size.height * .22f),
                        Offset(size.width * .78f, size.height * .78f), size.width * .13f, StrokeCap.Round)
                    drawLine(Ink, Offset(size.width * .78f, size.height * .22f),
                        Offset(size.width * .22f, size.height * .78f), size.width * .13f, StrokeCap.Round)
                }
            }
        }

        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Lime)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MochiTile(Mood.PLEASED, Paper, pixel = 2.dp, corner = 18.dp, inset = 14.dp)
            Spacer(Modifier.height(18.dp))
            Text(
                "Keep more than one.",
                style = Display.copy(fontSize = 28.sp, lineHeight = 30.sp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Your first ritual is free forever. Unlock the rest once, and they are yours for good.",
                style = Body.copy(color = androidx.compose.ui.graphics.Color(0xB312120F)),
                textAlign = TextAlign.Center
            )
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)) {
            INCLUDED.forEach { line ->
                Row(Modifier.padding(bottom = 14.dp)) {
                    Box(Modifier.size(15.dp)) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            drawLine(Ink, Offset(size.width * .18f, size.height * .53f),
                                Offset(size.width * .4f, size.height * .74f), size.width * .15f, StrokeCap.Round)
                            drawLine(Ink, Offset(size.width * .4f, size.height * .74f),
                                Offset(size.width * .82f, size.height * .29f), size.width * .15f, StrokeCap.Round)
                        }
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(line, style = Body.copy(color = Ink))
                }
            }
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
            InkPill(
                label = "Unlock forever · $price",
                onClick = { context.findActivity()?.let { Unlock.purchase(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Restore a previous purchase",
                style = Body.copy(fontSize = 13.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { Unlock.refresh() }
                    .padding(vertical = 8.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "One payment. No subscription, no account, nothing to cancel.",
                style = Body.copy(fontSize = 12.sp, color = InkFaint),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(34.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/** Play needs the Activity, and Compose only hands us a Context. */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
