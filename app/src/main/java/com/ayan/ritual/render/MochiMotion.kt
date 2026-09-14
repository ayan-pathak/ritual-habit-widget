package com.ayan.ritual.render

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** A thing Mochi does. Each one answers a change in state, never decoration. */
enum class Beat { MARK, MISS, UNLOCK, SETTLE }

/**
 * Mochi's motion.
 *
 * There is no new art here and no rig. The nine paths that carry his head also
 * carry his chest, so a head turn or an independent ear flick is not available
 * without redrawing him. What is available is the whole portrait as one body:
 * squash, stretch, a hop, and a face swapped underneath. That is enough,
 * because squash and stretch is where the weight lives, and weight is what
 * reads as alive.
 *
 * Scale runs from the bottom centre, so a squash presses him down onto the
 * tile rather than shrinking him toward the middle of it.
 *
 * This holds no Compose and no `android.graphics`: it is the beat table and a
 * clock, so the iOS side can run the same timings against its own drawing.
 * Call [advance] once a frame, then read [scaleX], [scaleY], [offsetY] and
 * [mood].
 */
class MochiMotion(mood: Mood = Mood.RESTING) {

    /** The face to draw this frame. A blink shows through as [Mood.RESTING]. */
    var mood: Mood = mood
        private set

    /** Horizontal scale about the bottom centre. */
    var scaleX: Float = 1f
        private set

    /** Vertical scale about the bottom centre. */
    var scaleY: Float = 1f
        private set

    /** Rise and fall, in the art's own units: the [MochiArt.VIEW_H] viewport. */
    var offsetY: Float = 0f
        private set

    /** Breathing and blinking. Off for a still frame, or for reduced motion. */
    var idle: Boolean = true

    // ── The beats ───────────────────────────────────────────────────────
    //
    // A segment is a duration, the state it lands on, and how it gets there.
    // `takesFace` marks the one segment in a beat that swaps the face, so the
    // caller supplies which face and the timing stays here.

    private class Segment(
        val ms: Int,
        val sx: Float,
        val sy: Float,
        val ty: Float,
        val takesFace: Boolean,
        val ease: (Float) -> Float
    )

    private fun beat(which: Beat): List<Segment> = when (which) {
        // Load, leave, land, and wobble out. He is committed before he moves.
        Beat.MARK -> listOf(
            Segment(90, 1.10f, 0.88f, 0f, false, easeIn),
            Segment(140, 0.93f, 1.13f, -34f, true, easeOut),
            Segment(130, 1.06f, 0.94f, 0f, false, easeIn),
            Segment(290, 1f, 1f, 0f, false, settle)
        )
        // No pop. Weight goes out of him and he sinks.
        Beat.MISS -> listOf(
            Segment(220, 1.02f, 0.99f, 2f, true, easeBoth),
            Segment(480, 1.015f, 0.985f, 9f, false, easeOut)
        )
        // Mark, twice, the second bounce half the height of the first.
        Beat.UNLOCK -> listOf(
            Segment(110, 1.09f, 0.90f, 0f, false, easeIn),
            Segment(150, 0.92f, 1.15f, -46f, true, easeOut),
            Segment(130, 1.05f, 0.95f, 0f, false, easeIn),
            Segment(120, 0.95f, 1.08f, -22f, false, easeOut),
            Segment(110, 1.03f, 0.97f, 0f, false, easeIn),
            Segment(280, 1f, 1f, 0f, false, settle)
        )
        Beat.SETTLE -> listOf(
            Segment(300, 1f, 1f, 0f, true, easeOut)
        )
    }

    // ── State ───────────────────────────────────────────────────────────

    private var segments: List<Segment>? = null
    private var index = 0
    private var elapsed = 0f              // seconds into the current segment
    private var lands = Mood.RESTING      // the face the running beat swaps to

    // Where the beat started from, so an interrupted beat carries its pose
    // into the next one instead of snapping back to rest first.
    private var fromX = 1f
    private var fromY = 1f
    private var fromT = 0f

    // The pose the beats produce, before breathing is laid over it.
    private var poseX = 1f
    private var poseY = 1f
    private var poseT = 0f

    private var clock = 0f
    private var blinkAt = 2.4f
    private var blinkUntil = -1f
    private var resting = mood
    private var faceUnderBlink = mood

    /** Starts [which], landing on [face]. Any beat already running is cut. */
    fun play(which: Beat, face: Mood) {
        segments = beat(which)
        index = 0
        elapsed = 0f
        lands = face
        resting = face
        fromX = poseX
        fromY = poseY
        fromT = poseT
    }

    /** Drops straight to [face] with no motion, for a still or reduced frame. */
    fun snapTo(face: Mood) {
        segments = null
        resting = face
        faceUnderBlink = face
        mood = face
        poseX = 1f; poseY = 1f; poseT = 0f
        scaleX = 1f; scaleY = 1f; offsetY = 0f
    }

    /** Advances the clock by [dt] seconds and recomputes the pose. */
    fun advance(dt: Float) {
        clock += dt
        step(dt)

        var breatheX = 1f
        var breatheY = 1f
        if (idle) {
            // Slow enough to read as breathing rather than as a pulse. Y and X
            // move against each other, so his volume stays about constant.
            val s = sin(clock * (2f * PI.toFloat() / BREATH_SECONDS))
            breatheY = 1f + s * 0.012f
            breatheX = 1f - s * 0.006f
            blink()
        }

        scaleX = poseX * breatheX
        scaleY = poseY * breatheY
        offsetY = poseT
        mood = if (clock < blinkUntil) Mood.RESTING else faceUnderBlink
    }

    private fun step(dt: Float) {
        val list = segments ?: run {
            faceUnderBlink = resting
            return
        }
        val segment = list[index]
        elapsed += dt
        val k = (elapsed * 1000f / segment.ms).coerceIn(0f, 1f)
        val e = segment.ease(k)
        poseX = fromX + (segment.sx - fromX) * e
        poseY = fromY + (segment.sy - fromY) * e
        poseT = fromT + (segment.ty - fromT) * e
        if (segment.takesFace) faceUnderBlink = lands
        if (k >= 1f) {
            // Land on the target exactly: `settle` overshoots its way back and
            // arrives a thousandth short, which would otherwise accumulate.
            poseX = segment.sx; poseY = segment.sy; poseT = segment.ty
            fromX = poseX; fromY = poseY; fromT = poseT
            elapsed = 0f
            index++
            if (index >= list.size) segments = null
        }
    }

    private fun blink() {
        // Only a face with open eyes can blink, and the two the app shows do
        // not have any. He never blinks mid-beat either: a shut eye during a
        // hop reads as a flinch.
        if (!faceUnderBlink.opensEyes || segments != null || clock < blinkAt) return
        blinkUntil = clock + BLINK_SECONDS
        blinkAt = clock + 2.8f + Random.nextFloat() * 3.7f
    }

    private companion object {
        const val BREATH_SECONDS = 3.4f
        const val BLINK_SECONDS = 0.11f

        val easeIn: (Float) -> Float = { t -> t * t * t }
        val easeOut: (Float) -> Float = { t -> 1f - (1f - t).pow(3) }
        val easeBoth: (Float) -> Float = { t ->
            if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
        }

        /** Overshoot, come back, overshoot less: a landing, not a stop. */
        val settle: (Float) -> Float = { t -> 1f - 2f.pow(-9f * t) * cos(t * 13.5f) }
    }
}
