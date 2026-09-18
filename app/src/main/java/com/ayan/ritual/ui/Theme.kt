package com.ayan.ritual.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ayan.ritual.R

/** Which way round the app's own surfaces go. */
enum class Appearance { LIGHT, DARK }

/**
 * The app's surfaces, and the one switch that flips them.
 *
 * **Only the chrome flips.** The page, the inset panels and the type change;
 * the cards do not. A ritual card is a printed object — a colour block with a
 * year stamped into it — and it is the same object on a dark table as on a
 * light one. It is also the thing that gets screenshotted to a story and
 * pinned to a home screen, where it has no idea what is behind it. So
 * [com.ayan.ritual.render.Palette] stays fixed and this only governs what the
 * card is lying on.
 *
 * The colours are read through a snapshot-backed [appearance], so a plain
 * `val Cream get() = …` works in a composable and inside a `Canvas` draw
 * lambda alike, and every use site in the app keeps reading the same names it
 * always did.
 */
object Look {

    private const val PREFS = "ritual_look"
    private const val KEY = "appearance_v1"

    private val _appearance = mutableStateOf(Appearance.DARK)
    val appearance: Appearance get() = _appearance.value

    private var prefs: android.content.SharedPreferences? = null

    /** Safe to call on every launch; the first call wins. */
    fun load(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        // Dark unless someone has said otherwise. The window background and
        // the splash are dark too, so a light default meant every cold start
        // flashed cream over ink before the first frame landed.
        _appearance.value =
            if (p.getString(KEY, "DARK") == "LIGHT") Appearance.LIGHT else Appearance.DARK
    }

    fun set(value: Appearance) {
        _appearance.value = value
        prefs?.edit()?.putString(KEY, value.name)?.apply()
    }

    private val dark get() = appearance == Appearance.DARK

    /** The page. */
    val page: Color get() = if (dark) Color(0xFF16150F) else Color(0xFFE7E3D4)

    /** Inset surfaces: fields, the odd panel. */
    val surface: Color get() = if (dark) Color(0xFF232117) else Color(0xFFF4F2EA)

    /** Type, filled squares, primary pills. */
    val ink: Color get() = if (dark) Color(0xFFEDE9DA) else Color(0xFF12120F)

    /** What sits on a primary pill, which is always the opposite of [ink]. */
    val onInk: Color get() = if (dark) Color(0xFF16150F) else Color(0xFFF4F2EA)
}

// The names every screen already uses. They are getters rather than constants
// so that flipping Look repaints the app without a single call site changing.
val Cream: Color get() = Look.page
val Paper: Color get() = Look.surface
val Ink: Color get() = Look.ink
val OnInk: Color get() = Look.onInk
val InkSoft: Color get() = Look.ink.copy(alpha = 0.56f)
val InkFaint: Color get() = Look.ink.copy(alpha = 0.20f)

// Fixed, both ways round: these are the brand, not the surface.
val Lime = Color(0xFFC9F73F)
val Red = Color(0xFFE5331C)

// What goes on a fixed colour. A Lime card is the same Lime in both themes,
// so anything printed on it has to be the ink that was chosen against Lime —
// following [Ink] would put cream type on a highlighter in dark mode.
val OnLime = Color(0xFF12120F)
val OnLimeSoft = Color(0xB312120F)

val Archivo = FontFamily(
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_extrabold, FontWeight.ExtraBold)
)

/** The statement type: set tight, always ExtraBold. */
val Display: TextStyle
    get() = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 41.sp,
        letterSpacing = (-1.2).sp,
        color = Ink
    )

/** Small caps used for every label and category. */
val Caps: TextStyle
    get() = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 1.1.sp,
        color = InkSoft
    )

val Body: TextStyle
    get() = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = InkSoft
    )

@Composable
fun RitualTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink,
            onPrimary = OnInk,
            background = Cream,
            onBackground = Ink,
            surface = Paper,
            onSurface = Ink,
            outline = InkFaint
        ),
        typography = Typography(),
        content = content
    )
}
