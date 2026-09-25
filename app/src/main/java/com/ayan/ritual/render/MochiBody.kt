package com.ayan.ritual.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/**
 * One full-body Mochi per moment in the app. Each answers the screen it is on:
 * he waves on the way in, peeks over the button on the first question, holds
 * up a key on the unlock, parties on day thirty, and dozes on an empty home.
 */
enum class Pose(val mood: Mood) {
    WAVE(Mood.PLEASED), HELLO(Mood.RESTING), PEEK(Mood.RESTING), SIT(Mood.RESTING),
    CHEER(Mood.PLEASED), PERCH(Mood.PLEASED), KEY(Mood.PLEASED), CARD(Mood.PLEASED),
    SELFIE(Mood.PLEASED), CLOUD(Mood.RESTING), PARTY(Mood.PLEASED), SLEEP(Mood.RESTING),

    /** A waddle, facing you. The caller moves him; this is only the gait. */
    WALK(Mood.PLEASED),

    /**
     * Once, from behind whatever he is anchored to: up, a look around, back
     * down. With [MochiBody.Extras.cheer] he comes up grinning and hops with
     * hearts. Lasts [MochiBody.popSeconds].
     */
    POP(Mood.RESTING)
}

/**
 * Mochi with his whole body.
 *
 * His head and chest are the real portrait, drawn by [Cat.draw] exactly as
 * everywhere else. Below it is a sitting body drawn the way the face is drawn:
 * flat fills in the portrait's four greys, a 24-unit ink outline on the
 * silhouette only, and tabby stripes as tapered blades rather than lines. The
 * chest's own colour bands carry on below its bottom edge and end in a tufted
 * fur edge, so there is no seam.
 *
 * Every arm, leg and foot is one closed outline: a spine that bends softly at
 * the elbow and wrist, a width that swells and narrows along it the way a
 * cat's leg does, fur tufts at the elbow like the ones on his cheeks, and
 * three toes scalloped into the paw's edge. Nothing in him is a straight line.
 *
 * Coordinates are the portrait's own units, with the body continuing below
 * its bottom edge to a ground line at [GROUND]. The frame is a pure function
 * of the clock, so the composable only has to hand it the time. Only the head
 * is rasterised, once per size and face; the body is cheap enough to draw.
 */
object MochiBody {

    const val CW = 1400f
    const val CH = 1650f
    private const val OX = 260f
    private const val OY = 310f
    private const val GROUND = 1290f

    /** Width over height of the frame he is drawn in. */
    const val RATIO = CW / CH

    /**
     * What a caller can tell a pose beyond the clock: a name to say back, a
     * pop that celebrates, which way a walk is heading and how high off the
     * ground it walks.
     */
    class Extras(
        val say: String? = null,
        val cheer: Boolean = false,
        val dir: Float = -1f,
        val walkLift: Float = 0f
    )

    /** How long a [Pose.POP] lasts, after which the caller removes him. */
    fun popSeconds(cheer: Boolean) = if (cheer) 3.4f else 3.0f

    /** How long a change of pose takes to blend from one to the next. */
    const val BLEND = 0.45f

    /**
     * Where the ground line sits, as a fraction of the frame's height, so a
     * caller can stand him on the edge of something. When peeking, his paws
     * grip the frame's own bottom edge.
     */
    fun groundAt(pose: Pose): Float = when (pose) {
        Pose.PEEK, Pose.POP -> 1f
        Pose.PERCH -> (OY + GROUND - 300f) / CH
        else -> (OY + GROUND) / CH
    }

    // ── Palette: the portrait's own greys, and the app's four colours ──────

    private const val K = 0xFF000000.toInt()
    private const val BASE = 0xFF646464.toInt()
    private const val MID = 0xFF8F8E8B.toInt()
    private const val DARK = 0xFF4A4A4A.toInt()
    private const val LIGHT = 0xFFABAAA9.toInt()
    private const val PALE = 0xFFC7C6C6.toInt()
    private const val PINK = 0xFFD1837A.toInt()
    private const val PINK_LIGHT = 0xFFE3A39B.toInt()
    private const val CREAM = 0xFFF4F2EA.toInt()
    private const val CLOUD_LINE = 0xFFDDD9CB.toInt()
    private const val INK = 0xFF12120F.toInt()
    private const val LIME = 0xFFC9F73F.toInt()
    private const val RED = 0xFFE5331C.toInt()
    private const val BUTTER = 0xFFEDE55C.toInt()
    private const val ORANGE = 0xFFF26A1B.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val OL = 24f

    private const val PI = 3.1415927f
    private const val TAU = PI * 2f
    private fun deg(r: Float) = r * 57.29578f
    private fun clamp(v: Float, a: Float, b: Float) = max(a, min(b, v))
    private fun smooth(v: Float): Float { val u = clamp(v, 0f, 1f); return u * u * (3f - 2f * u) }
    private fun pop(v: Float): Float {
        val u = clamp(v, 0f, 1f); val c = 1.9f
        return 1f + (c + 1f) * (u - 1f).pow(3) + c * (u - 1f).pow(2)
    }
    private fun cyc(t: Float, period: Float, off: Float = 0f): Float {
        val v = (t + off) / period
        return v - floor(v)
    }

    private class Pt(val x: Float, val y: Float)

    // ── What a pose changes ────────────────────────────────────────────────

    private class Tail(
        val a0: Float = 0.3f, val curl: Float = -0.215f, val sway: Float, val f: Float,
        val bx: Float = 706f, val by: Float = 1236f, val front: Boolean = false
    )

    private enum class Prop { KEY, CARD, PHONE, CLOUD }

    private class P(t: Float) {
        var hop = 0f; var sq = 0f; var land = -1f; var tilt = 0f; var bob = 0f
        var armL = Float.NaN; var armR = Float.NaN; var wristL = 0f; var wristR = 0f
        var padL = false; var padR = false
        var tail = Tail(sway = 0.25f, f = 2f)
        var dy = 0f; var peek = false; var prop: Prop? = null; var dangle = false
        var breathe = sin(t * 1.8f) * 0.014f; var flash = -1f; var cardY = 0f; var wig = 0f
        var liftL = 0f; var liftR = 0f; var sway = 0f; var pawUp = 1f
        var tailFrom: Tail? = null; var tailMix = 1f
    }

    /**
     * A hop with weight: squash on landing and before take-off, stretch on
     * the way up and down. Every part starts and ends at zero, so nothing
     * snaps between frames: the stretch eases in after take-off and out
     * before the apex, and each squash is one smooth dip on the ground.
     */
    private fun P.jump(t: Float, period: Float, height: Float, air: Float) {
        val u = cyc(t, period)
        if (u < air) {
            val k = u / air
            hop = sin(PI * k) * height; sq = 0.7f * sin(PI * k) * abs(cos(PI * k)); land = -1f
        } else {
            val v = (u - air) / (1f - air)
            val l = min(0.5f, 0.32f / ((1f - air) * period))
            var s = 0f
            if (v < l) s -= 0.6f * sin(PI * v / l)
            if (v > 1f - l) s -= 0.5f * sin(PI * (v - (1f - l)) / l)
            hop = 0f; sq = s; land = v
        }
    }

    /** The same hop, once: a dip, a jump, a dip, zero at both ends. */
    private fun hopOnce(u: Float, height: Float): Pair<Float, Float> = when {
        u < 0f || u > 1f -> 0f to 0f
        u < 0.24f -> 0f to -0.5f * sin(PI * u / 0.24f)
        u < 0.76f -> { val k = (u - 0.24f) / 0.52f; sin(PI * k) * height to 0.7f * sin(PI * k) * abs(cos(PI * k)) }
        else -> 0f to -0.55f * sin(PI * (u - 0.76f) / 0.24f)
    }

    private fun tail(f: Float, sway: Float) = Tail(sway = sway, f = f)

    private fun params(pose: Pose, t: Float, x: Extras): P {
        val p = P(t)
        when (pose) {
            Pose.WAVE -> {
                p.armR = -2.55f + sin(t * 7f) * 0.2f; p.wristR = sin(t * 7f - 1f) * 0.5f; p.padR = true
                p.tilt = 0.04f + sin(t * 1.7f) * 0.05f; p.tail = tail(2.4f, 0.3f); p.jump(t, 3.4f, 26f, 0.18f)
            }
            Pose.HELLO -> {
                // One envelope rises and falls around the greeting, so the wave
                // eases into and out of his resting pose instead of switching on.
                val u = cyc(t, 4.4f); val e = smooth(u / 0.08f) * (1f - smooth((u - 0.5f) / 0.1f))
                p.tilt = e * 0.12f + (1f - e) * sin(t * 1.3f) * 0.03f
                p.armL = 2.0f + e * (0.45f + sin(t * 14f) * 0.1f)
                p.wristL = 0.2f + e * (sin(t * 14f - 0.8f) * 0.4f - 0.2f); p.padL = true
                p.tail = tail(2f, 0.34f)
                val (h, q) = hopOnce(u * 4.4f / 1.1f, 40f); p.hop = h; p.sq = q
            }
            Pose.PEEK -> {
                val u = cyc(t, 6.5f)
                val up = when {
                    u < 0.2f -> pop(u / 0.2f)
                    u < 0.8f -> 1f
                    u < 0.92f -> 1f - smooth((u - 0.8f) / 0.12f)
                    else -> 0f
                }
                p.peek = true; p.dy = 1010f - 500f * up; p.wig = sin(t * 8f) * 6f
                p.tilt = if (u > 0.18f && u < 0.78f) sin((u - 0.18f) / 0.6f * TAU * 1.5f) * 0.14f else 0f
            }
            Pose.SIT -> {
                val u = cyc(t, 5.5f)
                p.tilt = sin(t * 1.1f) * 0.03f + if (u > 0.6f && u < 0.85f) 0.12f * sin((u - 0.6f) / 0.25f * PI) else 0f
                p.tail = tail(1.6f, 0.3f)
            }
            Pose.CHEER -> {
                p.jump(t, 0.95f, 130f, 0.5f); val up = p.hop / 130f
                p.armL = 2.55f + up * 0.35f; p.armR = -2.55f - up * 0.35f
                p.wristL = sin(t * 13f) * 0.3f; p.wristR = -sin(t * 13f) * 0.3f
                p.padL = true; p.padR = true; p.tail = tail(5f, 0.45f)
            }
            Pose.PERCH -> {
                p.dangle = true; p.dy = -300f; p.tilt = sin(t * 1.2f) * 0.07f
                p.tail = Tail(a0 = 1.35f, curl = 0.04f, sway = 0.5f, f = 2.2f, bx = 760f, by = 1250f)
            }
            Pose.KEY -> {
                p.armR = -2.5f + sin(t * 3f) * 0.1f; p.wristR = sin(t * 3f - 0.7f) * 0.22f; p.prop = Prop.KEY
                p.tilt = -0.05f + sin(t * 1.8f) * 0.04f; p.tail = tail(3f, 0.32f); p.jump(t, 1.8f, 36f, 0.32f)
            }
            Pose.CARD -> {
                p.prop = Prop.CARD; p.cardY = -abs(sin(t * 2.2f)) * 46f
                p.tilt = sin(t * 2.2f) * 0.04f; p.tail = tail(2.5f, 0.3f)
            }
            Pose.SELFIE -> {
                val u = cyc(t, 2.8f)
                p.armR = -2.42f + sin(t * 1.4f) * 0.05f; p.wristR = -0.15f; p.prop = Prop.PHONE
                p.tilt = 0.11f + if (u < 0.12f) sin(u / 0.12f * PI) * 0.05f else 0f
                p.armL = 2.05f + sin(t * 6f) * 0.1f; p.wristL = -0.3f; p.padL = true
                p.flash = u; p.tail = tail(2f, 0.3f)
            }
            Pose.CLOUD -> {
                p.prop = Prop.CLOUD; p.dy = -210f + sin(t * 1.8f) * 22f; p.tilt = sin(t * 1.8f - 0.8f) * 0.05f
                p.tail = Tail(a0 = 1.2f, curl = 0.03f, sway = 0.4f, f = 1.6f, bx = 760f, by = 1250f)
            }
            Pose.PARTY -> {
                p.jump(t, 0.92f, 200f, 0.55f); val up = p.hop / 200f
                p.armL = 2.45f + up * 0.45f; p.armR = -2.45f - up * 0.45f
                p.wristL = sin(t * 15f) * 0.35f; p.wristR = -sin(t * 15f) * 0.35f
                p.padL = true; p.padR = true; p.tail = tail(6f, 0.5f); p.tilt = sin(t * 4f) * 0.05f
            }
            Pose.SLEEP -> {
                val u = cyc(t, 5.2f)
                val nod = if (u < 0.86f) smooth(u / 0.86f) else 1f - smooth((u - 0.86f) / 0.05f)
                p.tilt = 0.04f + 0.2f * nod; p.bob = 8f + nod * 26f; p.breathe = sin(t * 1.5f) * 0.035f
                if (u > 0.86f && u < 0.96f) { val b = sin((u - 0.86f) / 0.1f * PI); p.hop = b * 26f; p.sq = 0.3f * b }
                p.tail = Tail(a0 = PI - 0.05f, curl = 0.035f, sway = 0.12f, f = 0.9f, bx = 700f, by = 1262f, front = true)
            }
            Pose.WALK -> {
                // Weight rocks from paw to paw, each front paw lifts in turn,
                // and he leans a little into the way he is going.
                val s = sin(t * 7.2f)
                p.liftL = max(0f, s) * 50f; p.liftR = max(0f, -s) * 50f; p.sway = s * 0.06f
                p.hop = abs(s) * 16f; p.tilt = -s * 0.05f + x.dir * 0.05f; p.dy = x.walkLift
                p.tail = tail(3.6f, 0.45f)
            }
            Pose.POP -> {
                val u = clamp(t / popSeconds(x.cheer), 0f, 1f)
                val up = when {
                    u < 0.16f -> pop(u / 0.16f)
                    u < 0.78f -> 1f
                    else -> 1f - smooth((u - 0.78f) / 0.17f)
                }
                p.peek = true; p.dy = 1340f - 640f * up; p.wig = sin(t * 8f) * 6f
                p.pawUp = smooth(u / 0.1f) * (1f - smooth((u - 0.86f) / 0.1f))
                p.tilt = if (x.cheer) sin(t * 3f) * 0.05f
                         else if (u > 0.2f && u < 0.74f) sin((u - 0.2f) / 0.54f * TAU) * 0.15f else 0f
                if (x.cheer) { val (h, q) = hopOnce((u - 0.2f) / 0.42f, 110f); p.hop = h; p.sq = q }
            }
        }
        return p
    }

    /**
     * Blends two frames' parameters. An arm that is down in one of them is
     * treated as hanging straight, so a paw can rise out of a sitting pose;
     * each tail keeps its own rhythm and the drawn shape moves between them.
     */
    private fun blend(a: P, b: P, e: Float): P {
        fun mix(u: Float, v: Float) = u + (v - u) * e
        b.hop = mix(a.hop, b.hop); b.sq = mix(a.sq, b.sq); b.tilt = mix(a.tilt, b.tilt); b.bob = mix(a.bob, b.bob)
        b.dy = mix(a.dy, b.dy); b.breathe = mix(a.breathe, b.breathe); b.cardY = mix(a.cardY, b.cardY)
        b.liftL = mix(a.liftL, b.liftL); b.liftR = mix(a.liftR, b.liftR); b.sway = mix(a.sway, b.sway)
        b.wristL = mix(a.wristL, b.wristL); b.wristR = mix(a.wristR, b.wristR); b.pawUp = mix(a.pawUp, b.pawUp); b.wig = mix(a.wig, b.wig)
        b.armL = if (a.armL.isNaN() && b.armL.isNaN()) Float.NaN
                 else mix(if (a.armL.isNaN()) 0.04f else a.armL, if (b.armL.isNaN()) 0.04f else b.armL)
        b.armR = if (a.armR.isNaN() && b.armR.isNaN()) Float.NaN
                 else mix(if (a.armR.isNaN()) -0.04f else a.armR, if (b.armR.isNaN()) -0.04f else b.armR)
        if (e < 0.5f) { b.padL = a.padL; b.padR = a.padR }
        b.tailFrom = a.tail; b.tailMix = e
        return b
    }

    // ── Paint ──────────────────────────────────────────────────────────────

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)
    private val image = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** The fade every effect draws under; 1 for the cat himself. */
    private var fade = 1f

    private fun tint(color: Int): Int {
        val a = ((color ushr 24) * clamp(fade, 0f, 1f)).roundToInt()
        return (a shl 24) or (color and 0xFFFFFF)
    }

    private fun Canvas.fillPath(path: Path, color: Int) {
        fill.color = tint(color); drawPath(path, fill)
    }
    private fun Canvas.strokePath(path: Path, color: Int, width: Float, cap: Paint.Cap = Paint.Cap.ROUND) {
        line.color = tint(color); line.strokeWidth = width; line.strokeCap = cap; drawPath(path, line)
    }
    private fun Canvas.outline(path: Path, width: Float = OL) = strokePath(path, K, width)
    private fun Canvas.inked(path: Path, color: Int) { fillPath(path, color); outline(path) }
    private fun Canvas.segment(ax: Float, ay: Float, bx: Float, by: Float, width: Float, color: Int) {
        line.color = tint(color); line.strokeWidth = width; line.strokeCap = Paint.Cap.ROUND
        drawLine(ax, ay, bx, by, line)
    }
    private fun Canvas.curve(ax: Float, ay: Float, cx: Float, cy: Float, bx: Float, by: Float, width: Float, color: Int) {
        strokePath(Path().apply { moveTo(ax, ay); quadTo(cx, cy, bx, by) }, color, width)
    }

    private fun oval(cx: Float, cy: Float, rx: Float, ry: Float, rot: Float = 0f) = Path().apply {
        addOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), Path.Direction.CW)
        if (rot != 0f) transform(Matrix().apply { setRotate(deg(rot), cx, cy) })
    }
    private fun circle(cx: Float, cy: Float, r: Float) = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
    private fun round(l: Float, t: Float, w: Float, h: Float, r: Float) = Path().apply {
        addRoundRect(RectF(l, t, l + w, t + h), r, r, Path.Direction.CW)
    }

    // ── Shapes along a curve ───────────────────────────────────────────────

    private fun qpts(a: Pt, c: Pt, b: Pt, n: Int) = List(n + 1) { i ->
        val u = i / n.toFloat(); val v = 1f - u
        Pt(v * v * a.x + 2 * v * u * c.x + u * u * b.x, v * v * a.y + 2 * v * u * c.y + u * u * b.y)
    }

    /** Offsets a centre line both ways by a width that varies along it. */
    private fun ribbon(pts: List<Pt>, wAt: (Float) -> Float): Pair<List<Pt>, List<Pt>> {
        val n = pts.size
        val l = ArrayList<Pt>(n); val r = ArrayList<Pt>(n)
        for (i in 0 until n) {
            val a = pts[max(0, i - 1)]; val b = pts[min(n - 1, i + 1)]
            var tx = b.x - a.x; var ty = b.y - a.y
            val m = hypot(tx, ty).takeIf { it > 0f } ?: 1f
            tx /= m; ty /= m
            val w = wAt(i / (n - 1f)) / 2f
            l.add(Pt(pts[i].x - ty * w, pts[i].y + tx * w)); r.add(Pt(pts[i].x + ty * w, pts[i].y - tx * w))
        }
        return l to r
    }

    private fun ribbonPath(l: List<Pt>, r: List<Pt>) = Path().apply {
        moveTo(l[0].x, l[0].y)
        for (q in l) lineTo(q.x, q.y)
        for (i in r.indices.reversed()) lineTo(r[i].x, r[i].y)
        close()
    }

    /** A tabby stripe: a curved blade, pointed at both ends or only the far one. */
    private fun Canvas.blade(a: Pt, c: Pt, b: Pt, w: Float, color: Int, oneEnd: Boolean = false) {
        val (l, r) = ribbon(qpts(a, c, b, 14)) { u ->
            w * if (oneEnd) (1f - u).coerceAtLeast(0f).pow(0.75f) else sin(PI * u).coerceAtLeast(0f).pow(0.7f)
        }
        fillPath(ribbonPath(l, r), color)
    }
    private fun Canvas.blade(ax: Float, ay: Float, cx: Float, cy: Float, bx: Float, by: Float, w: Float, color: Int, oneEnd: Boolean = false) =
        blade(Pt(ax, ay), Pt(cx, cy), Pt(bx, by), w, color, oneEnd)

    // ── Body ───────────────────────────────────────────────────────────────

    /**
     * The silhouette. It starts well up inside the portrait's chest so a head
     * tilt never opens a gap at the corners, and meets the portrait's own
     * outline at its bottom edge (x 95 and 773 at y 905).
     */
    private fun torsoPath() = Path().apply {
        moveTo(140f, 760f)
        quadTo(100f, 840f, 95f, 905f)
        cubicTo(84f, 1010f, 52f, 1120f, 76f, 1200f)
        cubicTo(100f, 1282f, 190f, 1290f, 270f, 1290f)
        lineTo(598f, 1290f)
        cubicTo(678f, 1290f, 768f, 1282f, 792f, 1200f)
        cubicTo(816f, 1120f, 784f, 1010f, 773f, 905f)
        quadTo(768f, 840f, 728f, 760f)
    }

    private fun Canvas.torso() {
        val tp = torsoPath()
        save(); clipPath(tp)
        fill.color = BASE; drawPaint(fill)
        // The portrait's dark side bands, carried down the flanks.
        strokePath(tp, DARK, 150f)
        for (m in 0..1) {
            val x = { v: Float -> if (m == 1) 868f - v else v }
            blade(x(40f), 975f, x(150f), 985f, x(250f), 1030f, 44f, DARK, true)
            blade(x(30f), 1060f, x(140f), 1070f, x(230f), 1120f, 40f, DARK, true)
            blade(x(40f), 1140f, x(120f), 1150f, x(190f), 1190f, 34f, DARK, true)
        }
        // Belly, lighter, with small fur tufts.
        fillPath(oval(434f, 1150f, 175f, 150f), MID)
        fillPath(oval(434f, 1170f, 120f, 110f), LIGHT)
        for ((a, b, s) in listOf(Triple(380f, 1095f, 1f), Triple(488f, 1095f, 1f), Triple(434f, 1140f, 1.2f),
                                 Triple(392f, 1195f, 0.9f), Triple(476f, 1195f, 0.9f))) {
            blade(a - 26 * s, b - 14 * s, a - 8 * s, b + 6 * s, a, b + 22 * s, 14 * s, PALE)
            blade(a + 26 * s, b - 14 * s, a + 8 * s, b + 6 * s, a, b + 22 * s, 14 * s, PALE)
        }
        restore()
        outline(tp)
    }

    /**
     * The chest's light bib, continued below the portrait's edge and ending in
     * a row of tufts, with the centre stripe running into it to a point. Drawn
     * over the tops of the front legs so fur hangs over them.
     */
    private val TUFTS = floatArrayOf(232f, 930f, 262f, 1004f, 300f, 962f, 340f, 1024f, 382f, 970f, 434f, 1036f,
        486f, 970f, 528f, 1024f, 568f, 962f, 606f, 1004f, 634f, 930f)

    private fun Canvas.bib() {
        fun shape(dy: Float) = Path().apply {
            moveTo(232f, 880f + dy)
            for (i in TUFTS.indices step 2) lineTo(TUFTS[i], TUFTS[i + 1] + dy)
            lineTo(634f, 880f + dy); close()
        }
        fillPath(shape(16f), DARK)
        fillPath(shape(0f), MID)
        blade(440f, 880f, 440f, 940f, 436f, 1000f, 60f, BASE, true)
        for (a in listOf(300f, 568f)) blade(a, 920f, a + if (a < 434f) 10f else -10f, 950f, a, 985f, 22f, LIGHT)
        blade(200f, 880f, 205f, 930f, 215f, 975f, 60f, BASE, true)
        blade(660f, 880f, 655f, 930f, 645f, 975f, 60f, BASE, true)
    }

    /** A sitting cat's thigh: a round haunch with a tabby swirl, and the hind foot in front. */
    private fun Canvas.haunch(m: Int, noFoot: Boolean) {
        val cx = if (m == 1) 668f else 200f
        val rot = if (m == 1) -0.22f else 0.22f
        val x = { v: Float -> if (m == 1) 868f - v else v }
        val shape = oval(cx, 1196f, 134f, 98f, rot)
        save(); clipPath(shape)
        fillPath(shape, BASE)
        strokePath(shape, DARK, 70f)
        blade(x(110f), 1150f, x(200f), 1105f, x(290f), 1160f, 40f, DARK)
        blade(x(125f), 1225f, x(200f), 1180f, x(270f), 1225f, 32f, DARK)
        blade(x(150f), 1130f, x(210f), 1122f, x(250f), 1140f, 14f, MID)
        restore()
        outline(shape)
        if (!noFoot) foot(x(290f), 1266f, if (m == 1) 0.12f else -0.12f, 0.82f)
    }

    // ── Limbs ──────────────────────────────────────────────────────────────

    private val ARM_W = floatArrayOf(0f, 134f, 0.22f, 140f, 0.56f, 112f, 0.68f, 116f, 0.86f, 176f, 1f, 164f)
    private val LEG_W = floatArrayOf(0f, 132f, 0.3f, 118f, 0.66f, 98f, 0.8f, 124f, 1f, 164f)
    private val FOOT_W = floatArrayOf(0f, 138f, 1f, 158f)
    private val DANGLE_W = floatArrayOf(0f, 120f, 0.55f, 92f, 1f, 140f)
    private const val ARM_LEN = 272f

    /** Width along a limb, eased between keyframes given as (u, width) pairs. */
    private fun widthAt(keys: FloatArray, u: Float): Float {
        var i = 2
        while (i < keys.size) {
            if (u <= keys[i]) {
                val u0 = keys[i - 2]; val a = keys[i - 1]; val u1 = keys[i]; val b = keys[i + 1]
                return a + (b - a) * smooth((u - u0) / (u1 - u0))
            }
            i += 2
        }
        return keys[keys.size - 1]
    }

    private class Spine(val pts: List<Pt>, val heads: FloatArray)

    /**
     * The centre line: short links whose heading drifts by [bend] through the
     * middle and by [wrist] near the end. Heading 0 is straight down.
     */
    private fun spine(sx: Float, sy: Float, th: Float, length: Float, bend: Float, wrist: Float, n: Int): Spine {
        val pts = ArrayList<Pt>(n + 1); val heads = FloatArray(n + 1)
        pts.add(Pt(sx, sy)); heads[0] = th
        for (i in 1..n) {
            val u = i / n.toFloat()
            val a = th + bend * smooth((u - 0.28f) / 0.34f) + wrist * smooth((u - 0.66f) / 0.2f)
            val prev = pts[i - 1]
            pts.add(Pt(prev.x - (length / n) * sin(a), prev.y + (length / n) * cos(a))); heads[i] = a
        }
        return Spine(pts, heads)
    }

    private class Limb(
        val sx: Float, val sy: Float, val th: Float, val length: Float,
        val bend: Float = 0f, val wrist: Float = 0f, val widths: FloatArray = ARM_W,
        val depth: Float = 0.72f, val pad: Boolean = false, val tuft: Int = 0,
        val stripes: FloatArray = floatArrayOf(0.3f, 0.5f), val startCap: Boolean = false
    )

    /** Draws one limb and returns the centre of its paw. */
    private fun Canvas.limb(o: Limb): Pt {
        val n = 48
        val sp = spine(o.sx, o.sy, o.th, o.length, o.bend, o.wrist, n); val pts = sp.pts
        val lp = ArrayList<Pt>(n + 1); val rp = ArrayList<Pt>(n + 1)
        for (i in 0..n) {
            val u = i / n.toFloat(); val a = sp.heads[i]
            val tx = -sin(a); val ty = cos(a); val w = widthAt(o.widths, u) / 2f
            val spike = if (o.tuft != 0)
                (max(0f, 1f - abs(u - 0.4f) / 0.04f) + 0.8f * max(0f, 1f - abs(u - 0.49f) / 0.04f)) * 0.2f else 0f
            val wl = w * (1f + if (o.tuft > 0) spike else 0f)
            val wr = w * (1f + if (o.tuft < 0) spike else 0f)
            lp.add(Pt(pts[i].x - ty * wl, pts[i].y + tx * wl)); rp.add(Pt(pts[i].x + ty * wr, pts[i].y - tx * wr))
        }
        // The paw's end: round, with two notches between three toes.
        val aE = sp.heads[n]; val dx = -sin(aE); val dy = cos(aE); val nx = -dy; val ny = dx
        val e = pts[n]; val wE = widthAt(o.widths, 1f) / 2f
        val cap = ArrayList<Pt>(30); val notches = ArrayList<Pt>(2)
        for (k in 1 until 30) {
            val ang = PI * k / 30f
            fun g(z: Float) = exp(-((z / 0.085f) * (z / 0.085f)))
            val r = 1f - 0.11f * (g(ang - PI * 0.34f) + g(ang - PI * 0.66f))
            val q = Pt(e.x + (nx * cos(ang) + dx * o.depth * sin(ang)) * wE * r,
                       e.y + (ny * cos(ang) + dy * o.depth * sin(ang)) * wE * r)
            cap.add(q)
            if (k == 10 || k == 20) notches.add(q)
        }
        // A rounded start, where the limb shows its shoulder.
        val start = ArrayList<Pt>(20)
        if (o.startCap) {
            val a0 = sp.heads[0]; val d0x = -sin(a0); val d0y = cos(a0); val n0x = -d0y; val n0y = d0x
            val w0 = widthAt(o.widths, 0f) / 2f
            for (k in 1 until 20) {
                val ang = PI + PI * k / 20f
                start.add(Pt(pts[0].x + (n0x * cos(ang) + d0x * 0.8f * sin(ang)) * w0,
                             pts[0].y + (n0y * cos(ang) + d0y * 0.8f * sin(ang)) * w0))
            }
        }
        val shape = Path().apply {
            moveTo(lp[0].x, lp[0].y)
            for (q in lp) lineTo(q.x, q.y)
            for (q in cap) lineTo(q.x, q.y)
            for (i in n downTo 0) lineTo(rp[i].x, rp[i].y)
            for (q in start) lineTo(q.x, q.y)
            close()
        }
        fun at(u: Float) = (u * n).roundToInt()
        val pc = Pt(e.x - dx * wE * 0.3f, e.y - dy * wE * 0.3f)

        save(); clipPath(shape)
        fill.color = tint(BASE); drawPaint(fill)
        // Shade down one side, as the portrait shades its cheeks and chest.
        strokePath(Path().apply {
            moveTo(rp[0].x, rp[0].y); for (i in 1..at(0.8f)) lineTo(rp[i].x, rp[i].y)
        }, DARK, 46f)
        // Tabby bands across the limb.
        for (u in o.stripes) {
            val i = at(u); val a = sp.heads[i]
            fun ext(p: Pt, q: Pt) = Pt(p.x + (p.x - q.x) * 0.3f, p.y + (p.y - q.y) * 0.3f)
            blade(ext(lp[i], pts[i]), Pt(pts[i].x - sin(a) * 22f, pts[i].y + cos(a) * 22f), ext(rp[i], pts[i]), 30f, DARK)
        }
        // A soft highlight down the other side.
        fun mixL(u: Float): Pt { val i = at(u); return Pt((lp[i].x * 3 + pts[i].x * 2) / 5, (lp[i].y * 3 + pts[i].y * 2) / 5) }
        blade(mixL(0.06f), mixL(0.3f), mixL(0.6f), 18f, MID)
        // The paw, lighter: a mitten inside the same outline.
        save(); translate(pc.x, pc.y); rotate(deg(aE))
        fillPath(oval(0f, 10f, wE * 1.12f, wE * 0.95f), if (o.pad) PALE else LIGHT)
        if (!o.pad) fillPath(oval(-wE * 0.3f, -wE * 0.15f, wE * 0.42f, wE * 0.26f, 0.2f), PALE)
        restore()
        restore()

        outline(shape)
        if (o.pad) beans(pc, aE, wE)
        else for (q in notches) {
            val vx = pc.x - q.x; val vy = pc.y - q.y; val m = hypot(vx, vy)
            curve(q.x, q.y, q.x + vx / m * wE * 0.2f + dy * 4f, q.y + vy / m * wE * 0.2f - dx * 4f,
                  q.x + vx / m * wE * 0.42f, q.y + vy / m * wE * 0.42f, 11f, K)
        }
        return pc
    }

    /** Palm out: a heart-shaped main pad and four toe beans, pointing toward the toes. */
    private fun Canvas.beans(pc: Pt, a: Float, wE: Float) {
        save(); translate(pc.x, pc.y); rotate(deg(a)); val s = wE / 74f; scale(s, s)
        fillPath(Path().apply {
            moveTo(0f, -46f)
            cubicTo(-48f, -46f, -48f, 0f, -22f, 4f); quadTo(0f, 10f, 22f, 4f)
            cubicTo(48f, 0f, 48f, -46f, 0f, -46f); close()
        }, PINK)
        fillPath(oval(-10f, -26f, 12f, 7f, 0.3f), PINK_LIGHT)
        for ((bx, by, r) in listOf(Triple(-52f, 18f, -0.5f), Triple(-20f, 44f, -0.18f), Triple(20f, 44f, 0.18f), Triple(52f, 18f, 0.5f)))
            fillPath(oval(bx, by, 15f, 19f, r), PINK)
        restore()
    }

    /** A paw on the ground seen from the front: rounded top, toes along the bottom. */
    private fun Canvas.foot(cx: Float, cy: Float, rot: Float, s: Float) {
        save(); translate(cx, cy); rotate(deg(rot)); scale(s, s)
        limb(Limb(0f, -34f, 0f, 30f, widths = FOOT_W, depth = 0.55f, startCap = true, stripes = floatArrayOf()))
        restore()
    }

    /** A front leg standing: chubby at the top, narrowing, then the paw. */
    private fun Canvas.leg(cx: Float, lift: Float) {
        val m = if (cx < 434f) -1f else 1f
        limb(Limb(cx, 925f, 0f, 318f - lift, bend = 0.05f * m, widths = LEG_W, depth = 0.5f, stripes = floatArrayOf(0.46f, 0.62f)))
    }

    private class ArmPlan(val bend: Float, val length: Float, val paw: Pt, val head: Float)

    private fun armPlan(sx: Float, sy: Float, th: Float, wrist: Float, length: Float = ARM_LEN): ArmPlan {
        val bend = 0.2f * th.sign
        val sp = spine(sx, sy, th, length, bend, wrist, 48)
        val a = sp.heads[48]; val e = sp.pts[48]; val wE = widthAt(ARM_W, 1f) / 2f
        return ArmPlan(bend, length, Pt(e.x + sin(a) * wE * 0.3f, e.y - cos(a) * wE * 0.3f), a)
    }

    /** A raised front leg, bent softly toward his head. Palm out with [pad]. */
    private fun Canvas.arm(sx: Float, sy: Float, th: Float, wrist: Float, pad: Boolean, length: Float = ARM_LEN): Pt {
        val plan = armPlan(sx, sy, th, wrist, length)
        return limb(Limb(sx, sy, th, plan.length, bend = plan.bend, wrist = wrist, widths = ARM_W, pad = pad,
            startCap = true, tuft = if (th > 0f) -1 else 1, stripes = floatArrayOf(0.28f, 0.46f), depth = 0.78f))
    }

    /** Hind legs hanging over the edge he is sitting on, kicking in turn. */
    private fun Canvas.dangle(t: Float) {
        for (m in 0..1) {
            val sw = sin(t * 3.2f + m * PI) * 0.32f
            limb(Limb(if (m == 1) 680f else 188f, 1200f, sw, 150f, bend = sw * 0.4f, widths = DANGLE_W,
                depth = 0.55f, stripes = floatArrayOf(0.45f)))
        }
    }

    /**
     * The tail as a chain whose links each lag the one before, so a swish
     * travels from the base out to the tip. Tapered, ringed, dark-tipped.
     */
    private fun Canvas.tail(tl: Tail, t: Float, from: Tail? = null, e: Float = 1f) {
        val n = 13; val seg = 29f
        fun chain(c: Tail): List<Pt> {
            val q = ArrayList<Pt>(n + 1); q.add(Pt(c.bx, c.by))
            for (i in 1..n) {
                val a = c.a0 + c.curl * i + c.sway * sin(t * c.f - i * 0.42f) * (0.25f + i / n.toFloat())
                val prev = q[i - 1]; q.add(Pt(prev.x + seg * cos(a), prev.y + seg * sin(a)))
            }
            return q
        }
        var pts = chain(tl)
        if (from != null && e < 1f) {
            val a = chain(from)
            pts = pts.mapIndexed { i, q -> Pt(a[i].x + (q.x - a[i].x) * e, a[i].y + (q.y - a[i].y) * e) }
        }
        val w = { u: Float -> 76f - 22f * u }
        val (l, r) = ribbon(pts, w)
        val tip = pts[n]; val tr = w(1f) / 2f
        val body = ribbonPath(l, r)
        strokePath(body, K, OL * 2); strokePath(circle(tip.x, tip.y, tr), K, OL * 2)
        fillPath(body, BASE)
        fun band(i: Int, j: Int) = fillPath(Path().apply {
            moveTo(l[i].x, l[i].y); for (k in i + 1..j) lineTo(l[k].x, l[k].y)
            for (k in j downTo i) lineTo(r[k].x, r[k].y); close()
        }, DARK)
        band(3, 4); band(6, 7); band(9, 10); band(11, n)
        fillPath(circle(tip.x, tip.y, tr), DARK)
        strokePath(Path().apply {
            for (i in 1 until 9) {
                val qx = (l[i].x * 3 + pts[i].x) / 4; val qy = (l[i].y * 3 + pts[i].y) / 4
                if (i == 1) moveTo(qx, qy) else lineTo(qx, qy)
            }
        }, MID, 9f)
    }

    // ── Props ──────────────────────────────────────────────────────────────

    private fun Canvas.key() {
        save(); translate(10f, -60f); rotate(deg(-0.3f)); scale(1.7f, 1.7f)
        inked(round(-20f, -40f, 40f, 250f, 12f), INK)
        inked(Path().apply {
            addRect(RectF(20f, 140f, 74f, 168f), Path.Direction.CW)
            addRect(RectF(20f, 186f, 60f, 212f), Path.Direction.CW)
        }, INK)
        inked(circle(0f, -110f, 76f), INK)
        fillPath(circle(0f, -110f, 30f), LIME)
        fillPath(circle(-30f, -140f, 12f), 0xFF3A3A33.toInt())
        restore()
    }

    private fun Canvas.miniCard(t: Float) {
        save(); rotate(deg(sin(t * 2.2f) * 0.05f))
        inked(round(-200f, -128f, 400f, 256f, 44f), RED)
        val lit = min(32, floor(cyc(t, 3.2f) * 34f).toInt() + 16)
        for (c in 0 until 8) for (r in 0 until 4) {
            fillPath(round(-158f + c * 40f, -40f + r * 38f, 28f, 28f, 7f), if (c * 4 + r < lit) INK else 0x61F4F2EA)
        }
        label("+1", -158f, -64f, 56f, CREAM, Paint.Align.LEFT)
        restore()
    }

    private fun Canvas.phone() {
        save(); translate(0f, -120f); rotate(deg(-0.12f))
        inked(round(-100f, -175f, 200f, 350f, 36f), INK)
        fillPath(round(-76f, -150f, 96f, 96f, 24f), 0xFF2B2B26.toInt())
        for ((a, b) in listOf(-50f to -124f, -6f to -124f, -50f to -80f)) fillPath(circle(a, b, 17f), LIME)
        restore()
    }

    private fun Canvas.cloud(t: Float) {
        val puffs = listOf(Triple(-360f, 20f, 92f), Triple(-200f, -22f, 124f), Triple(20f, -52f, 138f),
                           Triple(240f, -22f, 118f), Triple(390f, 24f, 88f))
        val shape = Path().apply {
            puffs.forEachIndexed { i, (a, b, r) -> addCircle(a, b, r + sin(t * 2f + i) * 6f, Path.Direction.CW) }
            addRoundRect(RectF(-460f, 10f, 460f, 130f), 60f, 60f, Path.Direction.CW)
        }
        save(); translate(434f, 1390f)
        strokePath(shape, K, OL * 2)
        fillPath(shape, CREAM)
        for ((a, b, r) in puffs.subList(1, 4)) curve(a - r * 0.55f, b + r * 0.15f, a, b + r * 0.55f, a + r * 0.55f, b + r * 0.15f, 12f, CLOUD_LINE)
        restore()
        save(); translate(434f, 1450f)
        arrow(30f)
        restore()
    }

    private fun Canvas.arrow(width: Float) {
        segment(0f, 60f, 0f, -44f, width, INK)
        segment(-46f, 0f, 0f, -48f, width, INK)
        segment(46f, 0f, 0f, -48f, width, INK)
    }

    // ── Effects ────────────────────────────────────────────────────────────

    private fun Canvas.label(s: String, x: Float, y: Float, size: Float, color: Int, align: Paint.Align, middle: Boolean = false) {
        text.typeface = Fonts.extraBold(); text.textSize = size; text.textAlign = align; text.color = tint(color)
        val base = if (middle) y - (text.ascent() + text.descent()) / 2f else y
        drawText(s, x, base, text)
    }

    private inline fun faded(a: Float, block: () -> Unit) {
        val was = fade; fade = was * clamp(a, 0f, 1f)
        if (fade > 0f) block()
        fade = was
    }

    private fun Canvas.heart(cx: Float, cy: Float, s: Float, a: Float, color: Int = RED) {
        if (s <= 0f || a <= 0f) return
        faded(a) {
            save(); translate(cx, cy); scale(s, s)
            val shape = Path().apply {
                moveTo(0f, 40f); cubicTo(-70f, -8f, -46f, -62f, 0f, -28f); cubicTo(46f, -62f, 70f, -8f, 0f, 40f); close()
            }
            fillPath(shape, color); strokePath(shape, K, 12f)
            fillPath(oval(-22f, -18f, 9f, 6f, -0.6f), 0x8CFFFFFF.toInt())
            restore()
        }
    }

    private fun Canvas.sparkle(cx: Float, cy: Float, r: Float, a: Float, color: Int, rot: Float) {
        if (r <= 0f || a <= 0f) return
        faded(a) {
            save(); translate(cx, cy); rotate(deg(rot))
            val shape = Path().apply {
                moveTo(r, 0f)
                for (i in 0 until 4) {
                    val q = i * PI / 2f
                    quadTo(cos(q + PI / 4f) * r * 0.14f, sin(q + PI / 4f) * r * 0.14f, cos(q + PI / 2f) * r, sin(q + PI / 2f) * r)
                }
                close()
            }
            fillPath(shape, color); strokePath(shape, K, 10f)
            restore()
        }
    }

    private fun Canvas.puff(cx: Float, cy: Float, v: Float) {
        if (v < 0f || v > 0.6f) return
        val k = v / 0.6f
        faded(1f - k) {
            for (d in listOf(-1f, 1f)) for ((o, r) in listOf(0f to 1f, 70f to 0.7f)) {
                val c = circle(cx + d * (300f + o + k * 120f), cy - 20f - k * 40f, (30f + k * 22f) * r)
                fillPath(c, CREAM); strokePath(c, K, 9f)
            }
        }
    }

    /** A speech bubble as wide as what it says, its tail toward his face. */
    private fun Canvas.bubble(cx: Float, cy: Float, s: Float, say: String) {
        if (s <= 0.02f) return
        save(); translate(cx, cy); scale(s * 1.45f, s * 1.45f)
        text.typeface = Fonts.extraBold(); text.textSize = 88f
        var size = 88f; var tw = text.measureText(say)
        if (tw > 420f) { size = 88f * 420f / tw; tw = 420f }
        val hw = max(130f, tw / 2f + 60f)
        val shape = Path().apply {
            addRoundRect(RectF(-hw, -95f, hw, 55f), 60f, 60f, Path.Direction.CW)
            moveTo(-hw + 60f, 50f); lineTo(-hw + 20f, 115f); lineTo(-hw + 130f, 52f); close()
        }
        fillPath(shape, CREAM); strokePath(shape, K, 16f)
        fillPath(Path().apply { addRect(RectF(-hw + 50f, 40f, -hw + 140f, 60f), Path.Direction.CW) }, CREAM)
        label(say, 0f, -18f, size, INK, Paint.Align.CENTER, middle = true)
        restore()
    }

    private val CONFETTI = intArrayOf(LIME, RED, BUTTER, ORANGE, CREAM, INK)

    private fun Canvas.confetti(t: Float, seed: Float) {
        for (i in 0 until 34) {
            fun r(n: Int): Float { val v = sin(seed + i * 12.9898f + n * 78.233f) * 43758.5453f; return v - floor(v) }
            val speed = 0.22f + r(1) * 0.3f
            val y = ((t * speed + r(2)) % 1f) * (CH + 160f) - 120f
            val px = r(3) * CW + sin(t * 2f + i) * 40f
            save(); translate(px, y); rotate(deg(t * (1f + r(4) * 3f) + i))
            scale(1f, cos(t * (3f + r(5) * 4f) + i))
            fill.color = tint(CONFETTI[i % 6])
            if (i % 3 == 0) drawRect(-10f, -34f, 10f, 34f, fill) else drawRect(-22f, -22f, 22f, 22f, fill)
            restore()
        }
    }

    private fun Canvas.zzz(t: Float) {
        for (i in 0 until 3) {
            val k = cyc(t * 0.33f, 1f, i / 3f)
            faded(if (k < 0.15f) k / 0.15f else 1f - (k - 0.15f) / 0.85f) {
                label("z", 830f + k * 190f + sin(k * 7f) * 30f, 260f - k * 380f, 70f + k * 70f, INK, Paint.Align.CENTER)
            }
        }
    }

    /** Effects drawn over him, in the space he hops in. */
    private fun Canvas.front(pose: Pose, t: Float, p: P, pawL: Pt, pawR: Pt, x: Extras) {
        when (pose) {
            Pose.WAVE -> for (i in 0 until 2) {
                val u = cyc(t, 2.6f, i * 1.3f)
                heart(pawR.x + 60f + sin(u * 9f) * 26f + u * 70f, pawR.y - 70f - u * 300f,
                    pop(u * 5f) * (1f - u.pow(4)) * 0.95f, 1f - u.pow(3))
            }
            Pose.HELLO -> {
                val u = cyc(t, 4.4f)
                bubble(640f, -150f, if (u < 0.6f) pop(u / 0.1f) * (1f - smooth((u - 0.52f) / 0.08f)) else 0f,
                    if (x.say.isNullOrBlank()) "hi!" else "hi, ${x.say}!")
            }
            Pose.CHEER -> if (p.hop > 90f) {
                val k = (p.hop - 90f) / 40f
                sparkle(pawL.x - 60f, pawL.y - 90f, 44f * k, k, BUTTER, t * 3f)
                sparkle(pawR.x + 60f, pawR.y - 90f, 44f * k, k, LIME, -t * 3f)
            }
            Pose.PERCH -> {
                val u = cyc(t, 3.2f)
                faded(1f - u) { label("♪", 860f + sin(u * 8f) * 30f, 200f - u * 260f, 110f, INK, Paint.Align.LEFT) }
            }
            Pose.KEY -> {
                val u = cyc(t, 1.6f)
                sparkle(pawR.x - 10f, pawR.y - 260f, 60f * sin(clamp(u / 0.4f, 0f, 1f) * PI), 1f, BUTTER, u * 2f)
                sparkle(pawR.x + 120f, pawR.y - 120f, 30f * sin(clamp((u - 0.3f) / 0.4f, 0f, 1f) * PI), 1f, CREAM, 0f)
            }
            Pose.CARD -> for (i in 0 until 2) {
                val u = cyc(t, 1.9f, i * 0.95f)
                faded(1f - u * u) {
                    label("+1", 434f + (if (i == 1) 330f else -330f) + sin(u * 6f) * 20f, 1010f + p.cardY - u * 260f,
                        max(1f, 110f * pop(u * 4f)), INK, Paint.Align.CENTER)
                }
            }
            Pose.SELFIE -> {
                val u = p.flash
                if (u < 0.16f) {
                    val k = u / 0.16f
                    faded(1f - k) { fillPath(circle(pawR.x - 30f, pawR.y - 260f, 50f + k * 240f), WHITE) }
                }
                if (u > 0.2f) {
                    val k = (u - 0.2f) / 0.8f
                    heart(pawR.x + 90f + k * 60f, pawR.y - 300f - k * 200f, pop(k * 5f) * 0.9f * (1f - k.pow(4)), 1f - k.pow(3))
                    heart(pawR.x + 180f + k * 30f, pawR.y - 200f - k * 150f, pop(k * 4f - 0.4f) * 0.6f * (1f - k.pow(4)), 1f - k.pow(3), PINK)
                }
            }
            Pose.CLOUD -> for (i in 0 until 3) {
                val u = cyc(t, 2.1f, i * 0.7f); val ax = listOf(-300f, 0f, 300f)[i] + 434f
                faded(if (u < 0.2f) u / 0.2f else 1f - (u - 0.2f) / 0.8f) {
                    save(); translate(ax, 1300f - u * 520f); scale(0.8f, 0.8f); arrow(26f); restore()
                }
            }
            Pose.PARTY -> if (p.hop > 140f) {
                val k = (p.hop - 140f) / 60f
                sparkle(434f, -80f, 70f * k, k, BUTTER, t * 2f)
                sparkle(90f, 120f, 40f * k, k, CREAM, 0f)
                sparkle(790f, 110f, 46f * k, k, LIME, 0f)
            }
            Pose.SLEEP -> zzz(t)
            Pose.POP -> if (x.cheer) for (i in 0 until 5) {
                val k = clamp((t / popSeconds(true) - 0.26f - i * 0.035f) / 0.45f, 0f, 1f); val side = i - 2
                heart(434f + side * 80f + side * 170f * k, 140f - 420f * k + abs(side) * 60f * k,
                    pop(k * 4f) * (1f - k.pow(4)) * (1.25f - abs(side) * 0.12f), 1f - k.pow(3), if (i % 2 == 1) PINK else RED)
            }
            else -> Unit
        }
    }

    // ── The head ───────────────────────────────────────────────────────────

    private val heads = HashMap<Long, Bitmap>()

    /** The portrait, rasterised once per face and pixel height. */
    private fun head(mood: Mood, px: Int): Bitmap {
        val key = (mood.ordinal.toLong() shl 32) or px.toLong()
        heads[key]?.let { return it }
        if (heads.size > 12) heads.clear()
        val h = px.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(Cat.widthFor(h.toFloat()).roundToInt().coerceAtLeast(1), h, Bitmap.Config.ARGB_8888)
        Cat.draw(Canvas(bitmap), 0f, 0f, h.toFloat(), mood)
        heads[key] = bitmap
        return bitmap
    }

    // ── One frame ──────────────────────────────────────────────────────────

    /**
     * Draws [pose] at clock [t], seconds, into a frame [height] pixels tall
     * whose top-left is the canvas origin. [kickAge] is seconds since the
     * last kick, a hop answering something just tapped; negative for none.
     * [from] is the pose he is leaving, [sinceSwitch] seconds ago; the two
     * blend over [BLEND] seconds rather than cutting.
     */
    fun draw(
        canvas: Canvas, height: Float, pose: Pose, t: Float, kickAge: Float = -1f, seed: Float = 0f,
        mood: Mood = pose.mood, extras: Extras = Extras(), from: Pose? = null, sinceSwitch: Float = BLEND
    ) {
        if (height <= 0f) return
        var p = params(pose, t, extras)
        if (from != null && from != pose && sinceSwitch < BLEND) {
            p = blend(params(from, t, extras), p, smooth(sinceSwitch / BLEND))
        }
        val (kh, kq) = hopOnce(kickAge / 1.1f, 190f)
        p.hop += kh; p.sq += kq
        val sx = 1f - 0.07f * p.sq - p.breathe * 0.5f
        val sy = 1f + 0.09f * p.sq + p.breathe
        fun toHop(q: Pt) = Pt(434f + (q.x - 434f) * sx, GROUND + (q.y - GROUND) * sy)
        val scale = height / CH
        fade = 1f

        with(canvas) {
            save(); scale(scale, scale)
            save(); translate(OX, OY + p.dy)
            if (!p.peek && (pose == Pose.CHEER || pose == Pose.PARTY)) puff(434f, GROUND, p.land)
            save(); translate(0f, -p.hop)
            if (p.sway != 0f) { translate(434f, GROUND); rotate(deg(p.sway)); translate(-434f, -GROUND) }
            save(); translate(434f, GROUND); scale(sx, sy); translate(-434f, -GROUND)

            var pawL = Pt(330f, 900f); var pawR = Pt(538f, 900f)
            if (!p.peek) {
                if (!p.tail.front) tail(p.tail, t, p.tailFrom, p.tailMix)
                torso()
                if (p.dangle) dangle(t)
                haunch(0, p.dangle); haunch(1, p.dangle)
                if (p.armL.isNaN()) leg(348f, p.liftL)
                if (p.armR.isNaN()) leg(520f, p.liftR)
                if (p.tail.front) tail(p.tail, t, p.tailFrom, p.tailMix)
                bib()
            }

            save()
            translate(434f, 905f + p.bob - p.sq * 14f); rotate(deg(p.tilt)); translate(-434f, -905f)
            drawBitmap(head(mood, (MochiArt.VIEW_H * height / CH).roundToInt()), null,
                RectF(0f, 0f, MochiArt.VIEW_W, MochiArt.VIEW_H), image)
            restore()

            if (!p.peek) {
                if (p.prop == Prop.CARD) {
                    save(); translate(434f, 1070f + p.cardY); scale(1.12f, 1.12f); miniCard(t); restore()
                    fun reach(sxs: Float, tx: Float, ty: Float): Pair<Float, Float> {
                        val ddx = tx - sxs; val ddy = ty - 968f
                        return atan2(-ddx, ddy) to (hypot(ddx, ddy) - 50f)
                    }
                    val (aL, lL) = reach(322f, 250f, 1080f + p.cardY)
                    val (aR, lR) = reach(546f, 618f, 1080f + p.cardY)
                    arm(322f, 968f, aL, 0.5f, false, lL); arm(546f, 968f, aR, -0.5f, false, lR)
                }
                if (!p.armL.isNaN()) pawL = arm(322f, 968f, p.armL, p.wristL, p.padL)
                if (!p.armR.isNaN()) {
                    if (p.prop == Prop.KEY || p.prop == Prop.PHONE) {
                        val plan = armPlan(546f, 968f, p.armR, p.wristR)
                        save(); translate(plan.paw.x, plan.paw.y)
                        rotate(deg(plan.head + PI + if (p.prop == Prop.KEY) 0.45f else 0f))
                        if (p.prop == Prop.KEY) key() else phone()
                        restore()
                    }
                    pawR = arm(546f, 968f, p.armR, p.wristR, p.padR)
                }
            }
            restore()
            if (p.prop == Prop.CLOUD) cloud(t)
            front(pose, t, p, toHop(pawL), toHop(pawR), extras)
            restore()
            restore()

            if (p.peek && p.pawUp > 0.01f) {
                // The paws slide up onto the edge as he rises and back off it as he goes.
                val drop = (1f - p.pawUp) * 140f
                foot(OX + 300f, CH - 36f + drop, p.wig * 0.012f, 1.05f)
                foot(OX + 568f, CH - 36f + drop, -p.wig * 0.012f, 1.05f)
            }
            if (pose == Pose.PARTY) confetti(t, seed)
            restore()
        }
    }
}
