package com.ayan.ritual.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.render.Accent
import com.ayan.ritual.render.Cat
import com.ayan.ritual.render.Mood
import com.ayan.ritual.render.SlabModel
import com.ayan.ritual.render.SlabRenderer

/**
 * A rendered card, sized to its box. The bitmap is cached against the model so
 * scrolling a list of rituals doesn't redraw 365 cells a frame.
 */
@Composable
fun RitualCard(
    model: SlabModel,
    height: Dp,
    modifier: Modifier = Modifier,
    header: Boolean = true,
    footer: Boolean = true,
    cat: Boolean = true,
    quarterRuler: Boolean = false,
    cornerDp: Float = 24f,
    padDp: Float = 17f
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth().height(height)) {
        val wPx = constraints.maxWidth
        val hPx = with(density) { height.roundToPx() }
        val bitmap = remember(wPx, hPx, model, header, footer, cat, quarterRuler, padDp) {
            SlabRenderer.render(
                wPx, hPx, model,
                SlabRenderer.Config(
                    density = density.density,
                    header = header,
                    footer = footer,
                    cat = cat,
                    quarterRuler = quarterRuler,
                    cornerDp = cornerDp,
                    padDp = padDp
                )
            )
        }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Mochi on a coloured tile — the app's only mascot surface. */
@Composable
fun MochiTile(
    mood: Mood,
    tile: Color,
    height: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    inset: Dp = 8.dp
) {
    val density = LocalDensity.current
    val px = with(density) { height.toPx() }
    val w = with(density) { Cat.widthFor(px).toDp() }
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(tile)
            .padding(horizontal = inset, vertical = inset * 0.8f),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(Modifier.size(w, height)) {
            val bmp = remember(mood, px) {
                val b = android.graphics.Bitmap.createBitmap(
                    Cat.widthFor(px).toInt().coerceAtLeast(1),
                    px.toInt().coerceAtLeast(1),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                Cat.draw(android.graphics.Canvas(b), 0f, 0f, px, mood)
                b
            }
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier, color: Color = InkSoft) {
    Text(text.uppercase(), style = Caps.copy(color = color), modifier = modifier)
}

/** The primary control: a solid black pill. Nothing else in the app is this loud. */
@Composable
fun InkPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 58.dp,
    background: Color = Ink,
    content: Color = Paper,
    border: Color? = null,
    leading: @Composable (() -> Unit)? = null
) {
    Box(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(BorderStroke(2.dp, border), CircleShape) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = content),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            leading?.invoke()
            Text(
                label,
                style = Caps.copy(
                    color = content,
                    fontSize = 15.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** A circular icon button with a 2dp outline — the app's secondary action. */
@Composable
fun RoundButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
    background: Color = Color.Transparent,
    border: Color? = Ink,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(BorderStroke(2.dp, border), CircleShape) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun StatCell(value: String, label: String, modifier: Modifier = Modifier, color: Color = Ink) {
    Column(modifier) {
        Text(value, style = Display.copy(fontSize = 24.sp, lineHeight = 25.sp, color = color))
        CapsLabel(label, color = InkSoft, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The colour chooser: six flat tiles, the chosen one ringed in ink. */
@Composable
fun ColourTiles(
    accents: List<Accent>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        accents.forEachIndexed { index, accent ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(accent.block))
                    .then(
                        if (selected) Modifier.border(BorderStroke(2.5.dp, Ink), RoundedCornerShape(15.dp))
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = { onSelect(index) }
                    )
            )
        }
    }
}
