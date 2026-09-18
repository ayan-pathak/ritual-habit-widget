import CoreGraphics
import Foundation

/// A thing Mochi does. Each one answers a change in state, never decoration.
enum Beat {
    case mark, miss, unlock, settle
}

/**
 Mochi's motion.

 There is no new art here and no rig. The nine paths that carry his head also
 carry his chest, so a head turn or an independent ear flick is not available
 without redrawing him. What is available is the whole portrait as one body:
 squash, stretch, a hop, and a face swapped underneath. That is enough,
 because squash and stretch is where the weight lives, and weight is what
 reads as alive.

 Scale runs from the bottom centre, so a squash presses him down onto the tile
 rather than shrinking him toward the middle of it.

 This is the same beat table and the same timings as `MochiMotion.kt` on the
 Android side, and it holds no SwiftUI and no CoreGraphics drawing for that
 reason. Call `tick(at:)` once a frame, then read `scaleX`, `scaleY`,
 `offsetY` and `mood`.
 */
final class MochiMotion {

    /// The face to draw this frame. A blink shows through as `.resting`.
    private(set) var mood: Mood

    /// Horizontal scale about the bottom centre.
    private(set) var scaleX: CGFloat = 1

    /// Vertical scale about the bottom centre.
    private(set) var scaleY: CGFloat = 1

    /// Rise and fall, in the art's own units: the `MochiArt.viewHeight` viewport.
    private(set) var offsetY: CGFloat = 0

    /// Breathing and blinking. Off for a still frame, or for reduced motion.
    var idle = true

    init(_ mood: Mood = .awake) {
        self.mood = mood
        self.resting = mood
        self.faceUnderBlink = mood
    }

    // ── The beats ───────────────────────────────────────────────────────────
    //
    // A segment is a duration, the state it lands on, and how it gets there.
    // `takesFace` marks the one segment in a beat that swaps the face, so the
    // caller supplies which face and the timing stays here.

    private struct Segment {
        let ms: CGFloat
        let sx: CGFloat
        let sy: CGFloat
        let ty: CGFloat
        let takesFace: Bool
        let ease: (CGFloat) -> CGFloat
    }

    private static func segments(_ which: Beat) -> [Segment] {
        switch which {
        // Load, leave, land, and wobble out. He is committed before he moves.
        case .mark:
            return [
                Segment(ms: 90, sx: 1.10, sy: 0.88, ty: 0, takesFace: false, ease: easeIn),
                Segment(ms: 140, sx: 0.93, sy: 1.13, ty: -34, takesFace: true, ease: easeOut),
                Segment(ms: 130, sx: 1.06, sy: 0.94, ty: 0, takesFace: false, ease: easeIn),
                Segment(ms: 290, sx: 1, sy: 1, ty: 0, takesFace: false, ease: settle)
            ]
        // No pop. Weight goes out of him and he sinks.
        case .miss:
            return [
                Segment(ms: 220, sx: 1.02, sy: 0.99, ty: 2, takesFace: true, ease: easeBoth),
                Segment(ms: 480, sx: 1.015, sy: 0.985, ty: 9, takesFace: false, ease: easeOut)
            ]
        // Mark, twice, the second bounce half the height of the first.
        case .unlock:
            return [
                Segment(ms: 110, sx: 1.09, sy: 0.90, ty: 0, takesFace: false, ease: easeIn),
                Segment(ms: 150, sx: 0.92, sy: 1.15, ty: -46, takesFace: true, ease: easeOut),
                Segment(ms: 130, sx: 1.05, sy: 0.95, ty: 0, takesFace: false, ease: easeIn),
                Segment(ms: 120, sx: 0.95, sy: 1.08, ty: -22, takesFace: false, ease: easeOut),
                Segment(ms: 110, sx: 1.03, sy: 0.97, ty: 0, takesFace: false, ease: easeIn),
                Segment(ms: 280, sx: 1, sy: 1, ty: 0, takesFace: false, ease: settle)
            ]
        case .settle:
            return [
                Segment(ms: 300, sx: 1, sy: 1, ty: 0, takesFace: true, ease: easeOut)
            ]
        }
    }

    // ── State ───────────────────────────────────────────────────────────────

    private var running: [Segment]?
    private var index = 0
    private var elapsed: CGFloat = 0          // seconds into the current segment
    private var lands: Mood = .awake          // the face the running beat swaps to

    // Where the beat started from, so an interrupted beat carries its pose into
    // the next one instead of snapping back to rest first.
    private var fromX: CGFloat = 1
    private var fromY: CGFloat = 1
    private var fromT: CGFloat = 0

    // The pose the beats produce, before breathing is laid over it.
    private var poseX: CGFloat = 1
    private var poseY: CGFloat = 1
    private var poseT: CGFloat = 0

    private var clock: CGFloat = 0
    private var blinkAt: CGFloat = 2.4
    private var blinkUntil: CGFloat = -1
    private var resting: Mood
    private var faceUnderBlink: Mood
    private var previous: Date?

    /// Starts `which`, landing on `face`. Any beat already running is cut.
    func play(_ which: Beat, face: Mood) {
        running = Self.segments(which)
        index = 0
        elapsed = 0
        lands = face
        resting = face
        fromX = poseX
        fromY = poseY
        fromT = poseT
    }

    /// Drops straight to `face` with no motion, for a still or reduced frame.
    func snap(to face: Mood) {
        running = nil
        resting = face
        faceUnderBlink = face
        mood = face
        previous = nil
        poseX = 1; poseY = 1; poseT = 0
        scaleX = 1; scaleY = 1; offsetY = 0
    }

    /// Advances to `date`, which is what `TimelineView(.animation)` hands over.
    func tick(at date: Date) {
        let dt = previous.map { min(CGFloat(date.timeIntervalSince($0)), 0.05) } ?? 0
        previous = date
        advance(dt)
    }

    /// Advances the clock by `dt` seconds and recomputes the pose.
    func advance(_ dt: CGFloat) {
        clock += dt
        step(dt)

        var breatheX: CGFloat = 1
        var breatheY: CGFloat = 1
        if idle {
            // Slow enough to read as breathing rather than as a pulse. Y and X
            // move against each other, so his volume stays about constant.
            let s = sin(clock * (2 * .pi / Self.breathSeconds))
            breatheY = 1 + s * 0.012
            breatheX = 1 - s * 0.006
            blink()
        }

        scaleX = poseX * breatheX
        scaleY = poseY * breatheY
        offsetY = poseT
        mood = clock < blinkUntil ? .resting : faceUnderBlink
    }

    private func step(_ dt: CGFloat) {
        guard let list = running else {
            faceUnderBlink = resting
            return
        }
        let segment = list[index]
        elapsed += dt
        let k = min(max(elapsed * 1000 / segment.ms, 0), 1)
        let e = segment.ease(k)
        poseX = fromX + (segment.sx - fromX) * e
        poseY = fromY + (segment.sy - fromY) * e
        poseT = fromT + (segment.ty - fromT) * e
        if segment.takesFace { faceUnderBlink = lands }
        if k >= 1 {
            // Land on the target exactly: `settle` overshoots its way back and
            // arrives a thousandth short, which would otherwise accumulate.
            poseX = segment.sx; poseY = segment.sy; poseT = segment.ty
            fromX = poseX; fromY = poseY; fromT = poseT
            elapsed = 0
            index += 1
            if index >= list.count { running = nil }
        }
    }

    private func blink() {
        // He never blinks mid-beat: a shut eye during a hop reads as a flinch.
        guard running == nil, clock >= blinkAt else { return }
        blinkUntil = clock + Self.blinkSeconds
        blinkAt = clock + 2.8 + CGFloat.random(in: 0...3.7)
    }

    private static let breathSeconds: CGFloat = 3.4
    private static let blinkSeconds: CGFloat = 0.11

    private static let easeIn: (CGFloat) -> CGFloat = { t in t * t * t }
    private static let easeOut: (CGFloat) -> CGFloat = { t in 1 - pow(1 - t, 3) }
    private static let easeBoth: (CGFloat) -> CGFloat = { t in
        t < 0.5 ? 4 * t * t * t : 1 - pow(-2 * t + 2, 3) / 2
    }

    /// Overshoot, come back, overshoot less: a landing, not a stop.
    private static let settle: (CGFloat) -> CGFloat = { t in
        1 - pow(2, -9 * t) * cos(t * 13.5)
    }
}
