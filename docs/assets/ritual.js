/**
 * Ritual, drawn in a browser.
 *
 * This is a port of the app's own renderers — Palette.kt, GridGeo.kt,
 * Habit.kt, Cat.kt, SlabRenderer.kt and ShareCardRenderer.kt — line for line,
 * so the cards on this site are the cards the app draws rather than pictures
 * of them. Every number here is the number in the Kotlin.
 *
 * Mochi is the real path data out of MochiArt.kt, replayed with the same
 * one-device-pixel seam stroke: the art is built from shapes that abut rather
 * than overlap, so without that stroke the paper shows through every boundary
 * as a hairline.
 */
/* ══ Palette.kt ══════════════════════════════════════════════════════════ */
// The card renderer's palette, fixed in both themes.
const INK = "#12120F", PAPER = "#F4F2EA", CREAM = "#E7E3D4", LIME = "#C9F73F";
// The app's own chrome, which flips. Anything printed on a fixed colour — a
// Lime card, a swatch — keeps the ink that was chosen against it.
const C = { ink: "var(--ink)", onInk: "var(--onink)", soft: "var(--inksoft)",
            faint: "var(--inkfaint)", paper: "var(--paper)" };
const ON_LIME = "#12120F", ON_LIME_SOFT = "rgba(18,18,15,.70)";
const ACCENTS = [
  { name: "Lime",   block: "#C9F73F", onBlock: INK },
  { name: "Red",    block: "#E5331C", onBlock: PAPER },
  { name: "Butter", block: "#EDE55C", onBlock: INK },
  { name: "Orange", block: "#F26A1B", onBlock: INK },
];
const accentAt = i => ACCENTS[((i % ACCENTS.length) + ACCENTS.length) % ACCENTS.length];
const alpha = (hex, a) => {
  const n = parseInt(hex.slice(1), 16);
  return `rgba(${n >> 16 & 255},${n >> 8 & 255},${n & 255},${a / 255})`;
};

/* ══ Dates, as java.time sees them ═══════════════════════════════════════ */
const DAY = 86400000;
const epochDay = d => Math.floor(Date.UTC(d.y, d.m - 1, d.d) / DAY);
const fromEpochDay = n => { const t = new Date(n * DAY); return { y: t.getUTCFullYear(), m: t.getUTCMonth() + 1, d: t.getUTCDate() }; };
const isLeap = y => (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
const yearLen = y => isLeap(y) ? 366 : 365;
const dayOfYear = d => epochDay(d) - epochDay({ y: d.y, m: 1, d: 1 }) + 1;
// GridGeo: weeks left to right, weekdays top to bottom, Sunday first.
const ROWS = 7;
const startOffset = y => new Date(Date.UTC(y, 0, 1)).getUTCDay();
const colsFor = y => Math.floor((startOffset(y) + yearLen(y) - 1) / ROWS) + 1;
const colOf = d => Math.floor((startOffset(d.y) + dayOfYear(d) - 1) / ROWS);
const MONTH_INITIALS = ["J","F","M","A","M","J","J","A","S","O","N","D"];
const MONTH_LENS = y => [31, isLeap(y) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

/* ══ Habit.kt ════════════════════════════════════════════════════════════ */
function daysOfYear(h, year) {
  const first = epochDay({ y: year, m: 1, d: 1 }), last = epochDay({ y: year, m: 12, d: 31 });
  const out = new Set();
  for (const day of h.done) if (day >= first && day <= last) out.add(day - first + 1);
  return out;
}
const isDone = (h, day) => h.done.has(day);
function streak(h, today) {
  let day = today;
  if (!h.done.has(day)) day -= 1;
  let n = 0;
  while (h.done.has(day)) { n++; day--; }
  return n;
}
const missedYesterday = (h, today) => !h.done.has(today - 1) && !isDone(h, today);
function bestStreak(h) {
  if (!h.done.size) return 0;
  const s = [...h.done].sort((a, b) => a - b);
  let best = 1, run = 1;
  for (let i = 1; i < s.length; i++) { run = s[i] === s[i - 1] + 1 ? run + 1 : 1; if (run > best) best = run; }
  return best;
}
const remainingIn = (year, today) => {
  const t = fromEpochDay(today);
  if (t.y !== year) return t.y < year ? yearLen(year) : 0;
  return yearLen(year) - dayOfYear(t);
};
// Cat.moodFor. Only two of the four faces ship: awake's wide green eyes read
// as a stare at the sizes he is drawn at, and a sad face punishes a missed day
// the empty square has already recorded. Both of these smile, eyes shut.
const moodFor = (done, st, missed) => done ? "pleased" : "resting";
const opensEyes = m => m === "awake" || m === "letDown";

function toModel(h, today, year) {
  return {
    title: h.name, slot: h.slot, accent: accentAt(h.accentIndex), year,
    today, doneDaysOfYear: daysOfYear(h, year), streak: streak(h, today),
    totalDone: daysOfYear(h, year).size, remaining: remainingIn(year, today),
    doneToday: isDone(h, today), mood: moodFor(isDone(h, today), streak(h, today), missedYesterday(h, today)),
  };
}

/* ══ Cat.kt ══════════════════════════════════════════════════════════════
   The same rows the app parses, replayed the same way, down to the seam:
   one device pixel of stroke in each shape's own colour, because abutting
   shapes each antialias their own edge and the paper shows through. */
const Cat = (() => {
  const VW = MOCHI.w, VH = MOCHI.h;
  const parsed = {};
  const argb = hex => {
    const v = parseInt(hex, 16);
    return `rgb(${v >> 16 & 255} ${v >> 8 & 255} ${v & 255})`;
  };
  function items(mood) {
    if (parsed[mood]) return parsed[mood];
    const out = [];
    for (const line of MOCHI.moods[mood]) {
      const f = line.split("\t");
      if (f[0] === "S") out.push({ path: new Path2D(f[3]), a: +f[2], fill: argb(f[1]) });
      else if (f[0] === "G") out.push({
        path: new Path2D(f[8]), a: +f[7],
        grad: [argb(f[1]), argb(f[2]), +f[3], +f[4], +f[5], +f[6]],
      });
    }
    return (parsed[mood] = out);
  }
  const widthFor = h => h * (VW / VH);

  function draw(ctx, left, top, height, mood, dpr) {
    if (height <= 0) return;
    const s = height / VH;
    ctx.save();
    ctx.translate(left, top);
    ctx.scale(s, s);
    // One device pixel expressed in the coordinates now in force. Half of it
    // lands outside each shape, which is exactly the seam.
    ctx.lineWidth = 1 / (s * dpr);
    ctx.lineJoin = "round";
    for (const it of items(mood)) {
      ctx.globalAlpha = it.a;
      let paint;
      if (it.grad) {
        const g = ctx.createLinearGradient(it.grad[2], it.grad[3], it.grad[4], it.grad[5]);
        g.addColorStop(0, it.grad[0]); g.addColorStop(1, it.grad[1]);
        paint = g;
      } else paint = it.fill;
      ctx.fillStyle = paint; ctx.strokeStyle = paint;
      ctx.fill(it.path); ctx.stroke(it.path);
    }
    ctx.globalAlpha = 1;
    ctx.restore();
  }

  // Each mood rasterised once per size, then moved. Seventy-five paths a
  // frame is not worth it for a squash.
  const frames = new Map();
  function frame(height, mood) {
    const dpr = window.devicePixelRatio || 1;
    const key = mood + "@" + Math.round(height) + "@" + dpr;
    if (frames.has(key)) return frames.get(key);
    const w = widthFor(height);
    const c = document.createElement("canvas");
    c.width = Math.max(1, Math.round(w * dpr));
    c.height = Math.max(1, Math.round(height * dpr));
    const ctx = c.getContext("2d");
    ctx.scale(dpr, dpr);
    draw(ctx, 0, 0, height, mood, dpr);
    frames.set(key, c);
    return c;
  }
  return { VW, VH, widthFor, draw, frame };
})();

/** Truncates to a width, adding an ellipsis only when it actually overflows. */
function fitText(ctx, text, maxWidth) {
  if (ctx.measureText(text).width <= maxWidth) return text;
  let end = text.length;
  while (end > 1 && ctx.measureText(text.slice(0, end) + "…").width > maxWidth) end--;
  return text.slice(0, Math.max(1, end)) + "…";
}

/* ══ SlabRenderer.kt ═════════════════════════════════════════════════════ */
const Slab = (() => {
  function setFont(ctx, size, color, weight, tracking) {
    ctx.font = `${weight} ${size}px Archivo, system-ui, sans-serif`;
    ctx.fillStyle = color;
    ctx.letterSpacing = (tracking * size).toFixed(2) + "px";
    ctx.textAlign = "left";
  }
  function fit(ctx, text, maxWidth) {
    if (ctx.measureText(text).width <= maxWidth) return text;
    let end = text.length;
    while (end > 1 && ctx.measureText(text.slice(0, end) + "…").width > maxWidth) end--;
    return text.slice(0, Math.max(1, end)) + "…";
  }
  function rrect(ctx, x, y, w, h, r) {
    ctx.beginPath(); ctx.roundRect(x, y, w, h, r);
  }

  function draw(ctx, w, h, model, cfg) {
    const d = cfg.scale ?? 1;          // 1 CSS px = 1 dp, scaled for a miniature
    const dp = v => v * d;
    const pad = dp(cfg.padDp ?? 17), corner = (cfg.corner ?? 24) * (cfg.cornerScale ?? 1);
    const header = cfg.header !== false, footer = cfg.footer !== false;
    const cat = cfg.cat !== false, action = !!cfg.action, ruler = !!cfg.quarterRuler;

    // Marked days flip the card to ink and light the grid in the ritual's
    // colour, so a glance across the home screen says what is still open.
    const lit = model.doneToday;
    const block = lit ? INK : model.accent.block;
    const onBlock = lit ? PAPER : model.accent.onBlock;
    const markInk = lit ? model.accent.block : INK;
    const missed = alpha(onBlock, 52), future = alpha(onBlock, 23);

    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = block; rrect(ctx, 0, 0, w, h, corner); ctx.fill();

    const headerH = header ? dp(42) : 0;
    const footerH = footer ? dp(22) : 0;
    const actionH = action ? dp(34) : 0;
    let bottom = h - pad;
    if (action || footer) bottom -= Math.max(actionH, footerH);

    const gridTopBound = pad + headerH + dp(6);
    const gridBottomBound = Math.max(bottom - dp(10), gridTopBound + dp(14));
    const cols = colsFor(model.year), gapRatio = .30, innerW = w - 2 * pad;
    let cell = innerW / (cols + gapRatio * (cols - 1));
    let gap = cell * gapRatio;
    let gridH = ROWS * cell + (ROWS - 1) * gap;
    const zoneH = gridBottomBound - gridTopBound;
    if (gridH > zoneH) {
      cell = zoneH / (ROWS + gapRatio * (ROWS - 1));
      gap = cell * gapRatio;
      gridH = ROWS * cell + (ROWS - 1) * gap;
    }
    const gridLeft = pad;
    const gridTop = gridTopBound + Math.max((zoneH - gridH) / 2, 0);

    const len = yearLen(model.year), startOff = startOffset(model.year);
    const t = fromEpochDay(model.today);
    const isCurrentYear = t.y === model.year;
    const todayDoy = isCurrentYear ? dayOfYear(t) : -1;

    if (header) {
      let textRight = w - pad;
      if (cat) {
        const catH = dp(30), catW = Cat.widthFor(catH);
        const boxW = catW + dp(11), boxH = catH + dp(9);
        const boxL = w - pad - boxW, boxT = pad - dp(2);
        ctx.fillStyle = lit ? model.accent.block : INK;
        rrect(ctx, boxL, boxT, boxW, boxH, dp(10)); ctx.fill();
        // Standing on the bottom edge of the box, not floating in the middle
        // of it: the same rule the app's MochiTile follows.
        Cat.draw(ctx, boxL + boxW / 2 - catW / 2, boxT + boxH - catH, catH,
                 model.mood, window.devicePixelRatio || 1);
        textRight = boxL - dp(10);
      } else {
        setFont(ctx, dp(17), onBlock, 800, -.02); ctx.textAlign = "right";
        ctx.fillText(String(model.streak), w - pad, pad + dp(15));
        setFont(ctx, dp(8), alpha(onBlock, 150), 600, .10); ctx.textAlign = "right";
        ctx.fillText("DAY STREAK", w - pad, pad + dp(26));
        ctx.textAlign = "left";
        textRight = w - pad - dp(58);
      }
      setFont(ctx, dp(8.5), alpha(onBlock, 150), 600, .10);
      ctx.fillText(model.slot.toUpperCase(), pad, pad + dp(8));
      setFont(ctx, dp(17), onBlock, 800, -.02);
      ctx.fillText(fit(ctx, model.title, Math.max(textRight - pad, dp(30))), pad, pad + dp(27));
      if (cat) {
        setFont(ctx, dp(8.5), alpha(onBlock, 170), 600, .10);
        // 14dp below the name's baseline rather than 10: at 8.5dp the caps
        // line was sitting in the name's descenders.
        ctx.fillText(`${model.streak} DAY STREAK`, pad, pad + dp(41));
      }
    }

    if (ruler) {
      setFont(ctx, dp(8), alpha(onBlock, 140), 600, .10);
      for (const [month, label] of [[1, "JAN"], [4, "APR"], [7, "JUL"], [10, "OCT"]]) {
        const col = colOf({ y: model.year, m: month, d: 1 });
        ctx.fillText(label, gridLeft + col * (cell + gap), gridTop - dp(5));
      }
    }

    const r = cell * .30;
    ctx.letterSpacing = "0px";
    for (let col = 0; col < cols; col++) {
      for (let row = 0; row < ROWS; row++) {
        const doy = col * ROWS + row - startOff + 1;
        if (doy < 1 || doy > len) continue;
        const l = gridLeft + col * (cell + gap), tp = gridTop + row * (cell + gap);
        ctx.fillStyle = model.doneDaysOfYear.has(doy) ? markInk
          : (isCurrentYear && doy > todayDoy ? future : missed);
        rrect(ctx, l, tp, cell, cell, r); ctx.fill();
        if (doy === todayDoy) {
          // Today wears a ring whether or not it is filled.
          ctx.strokeStyle = markInk;
          ctx.lineWidth = Math.max(dp(1.2), 1.3);
          rrect(ctx, l - dp(1.3), tp - dp(1.3), cell + dp(2.6), cell + dp(2.6), r * 1.7);
          ctx.stroke();
        }
      }
    }

    if (footer || action) {
      const baseY = h - pad - dp(4);
      setFont(ctx, dp(12), onBlock, 800, 0);
      const litText = `${model.totalDone} lit`;
      ctx.fillText(litText, pad, baseY);
      let x = pad + ctx.measureText(litText).width + dp(9);
      ctx.fillStyle = alpha(onBlock, 90);
      ctx.beginPath(); ctx.arc(x + dp(2), baseY - dp(3.5), dp(2), 0, 7); ctx.fill();
      x += dp(13);
      setFont(ctx, dp(12), alpha(onBlock, 160), 600, 0);
      ctx.fillText(`${model.remaining} to go`, x, baseY);
      if (action) drawAction(ctx, w, h, pad, model, lit, d);
    }
    ctx.letterSpacing = "0px";
  }

  /** The mark-today control. The widget lays a transparent hit target over it. */
  function drawAction(ctx, w, h, pad, model, lit, d) {
    const dp = v => v * d;
    const hgt = dp(34), label = lit ? "Done" : "Mark";
    setFont(ctx, dp(11.5), lit ? INK : PAPER, 800, 0);
    const checkW = lit ? dp(15) : 0;
    const wide = ctx.measureText(label).width + dp(28) + checkW;
    const right = w - pad, left = right - wide, top = h - pad - hgt;
    ctx.fillStyle = lit ? model.accent.block : INK;
    rrect(ctx, left, top, wide, hgt, hgt / 2); ctx.fill();
    setFont(ctx, dp(11.5), lit ? INK : PAPER, 800, 0);
    const textX = left + wide / 2 - ctx.measureText(label).width / 2 + checkW / 2;
    ctx.fillText(label, textX, top + hgt / 2 + dp(4));
    if (lit) {
      // Hand-drawn check: no font dependency, crisp at any size.
      const cx = textX - dp(9), cy = top + hgt / 2, s = dp(3.6);
      ctx.strokeStyle = INK; ctx.lineWidth = Math.max(dp(1.9), 2);
      ctx.lineCap = "round"; ctx.lineJoin = "round";
      ctx.beginPath();
      ctx.moveTo(cx - s, cy + s * .05);
      ctx.lineTo(cx - s * .15, cy + s * .72);
      ctx.lineTo(cx + s, cy - s * .72);
      ctx.stroke();
      ctx.lineCap = "butt";
    }
  }
  return { draw };
})();

/* ══ ShareCardRenderer.kt ════════════════════════════════════════════════
   The 1080x1920 image that goes to an Instagram story. Everything is sized
   off the canvas width, so drawing it small is the same composition rather
   than a sketch of it. */
const Story = (() => {
  const W = 1080, H = 1920;

  function draw(ctx, w, h, model) {
    const u = w / 1080, s = v => v * u;
    const accent = model.accent, onBlock = accent.onBlock;

    ctx.fillStyle = CREAM; ctx.fillRect(0, 0, w, h);

    // A single colour block anchors the composition; the story's safe area
    // keeps clear of Instagram's own chrome top and bottom.
    const margin = s(84), blockTop = h * 0.235;
    const bx = margin, by = blockTop, bw = w - margin * 2, bh = s(990);
    ctx.fillStyle = accent.block;
    ctx.beginPath(); ctx.roundRect(bx, by, bw, bh, s(64)); ctx.fill();

    const pad = s(64);
    const font = (size, color, weight, tracking) => {
      ctx.font = `${weight} ${size}px Archivo, system-ui, sans-serif`;
      ctx.fillStyle = color;
      ctx.letterSpacing = (tracking * size).toFixed(2) + "px";
      ctx.textAlign = "left";
    };

    font(s(30), alpha(onBlock, 160), 600, .10);
    ctx.fillText(model.slot.toUpperCase(), bx + pad, by + pad + s(26));

    font(s(76), onBlock, 800, -.03);
    ctx.fillText(fitText(ctx, model.title, bw - pad * 2), bx + pad, by + pad + s(110));

    // The number that matters.
    font(s(300), onBlock, 800, -.05);
    ctx.fillText(String(model.streak), bx + pad - s(8), by + s(430));
    font(s(38), alpha(onBlock, 190), 800, .02);
    ctx.fillText(model.streak === 1 ? "DAY IN A ROW" : "DAYS IN A ROW", bx + pad, by + s(492));

    // Mochi: ink, and standing on the bottom edge of his box.
    const catH = s(176), catW = Cat.widthFor(catH);
    const cbL = bx + bw - pad - catW - s(38), cbT = by + s(250);
    const cbW = catW + s(40), cbH = catH + s(38);
    ctx.fillStyle = INK;
    ctx.beginPath(); ctx.roundRect(cbL, cbT, cbW, cbH, s(34)); ctx.fill();
    Cat.draw(ctx, cbL + cbW / 2 - catW / 2, cbT + cbH - catH, catH, model.mood,
             window.devicePixelRatio || 1);

    // The year.
    const cols = colsFor(model.year), gapRatio = .30;
    const gridW = bw - pad * 2;
    const cell = gridW / (cols + gapRatio * (cols - 1)), gap = cell * gapRatio;
    const gridTop = by + s(620), startOff = startOffset(model.year), len = yearLen(model.year);
    const t = fromEpochDay(model.today);
    const todayDoy = t.y === model.year ? dayOfYear(t) : -1;
    const r = cell * .30;
    ctx.letterSpacing = "0px";
    for (let col = 0; col < cols; col++) {
      for (let row = 0; row < ROWS; row++) {
        const doy = col * ROWS + row - startOff + 1;
        if (doy < 1 || doy > len) continue;
        ctx.fillStyle = model.doneDaysOfYear.has(doy) ? INK
          : (todayDoy > 0 && doy > todayDoy ? alpha(onBlock, 23) : alpha(onBlock, 52));
        ctx.beginPath();
        ctx.roundRect(bx + pad + col * (cell + gap), gridTop + row * (cell + gap), cell, cell, r);
        ctx.fill();
      }
    }
    const gridBottom = gridTop + ROWS * cell + (ROWS - 1) * gap;

    ctx.fillStyle = alpha(onBlock, 46);
    ctx.fillRect(bx + pad, gridBottom + s(56), bw - pad * 2, s(4));

    const tallyY = gridBottom + s(150);
    font(s(64), onBlock, 800, -.02);
    ctx.fillText(String(model.totalDone), bx + pad, tallyY);
    font(s(26), alpha(onBlock, 160), 600, .10);
    ctx.fillText(`LIT IN ${model.year}`, bx + pad, tallyY + s(42));
    const midX = bx + bw * .5;
    font(s(64), onBlock, 800, -.02);
    ctx.fillText(String(model.remaining), midX, tallyY);
    font(s(26), alpha(onBlock, 160), 600, .10);
    ctx.fillText("SQUARES LEFT", midX, tallyY + s(42));

    font(s(34), INK, 800, -.01); ctx.textAlign = "center";
    ctx.fillText("RITUAL", w / 2, by + bh + s(110));
    font(s(24), alpha(INK, 130), 600, .14); ctx.textAlign = "center";
    ctx.fillText("ONE SQUARE A DAY", w / 2, by + bh + s(152));
    ctx.textAlign = "left"; ctx.letterSpacing = "0px";
  }

  function paint(canvas, height, model) {
    const w = height * (W / H), dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(height * dpr);
    canvas.style.width = w + "px";
    canvas.style.height = height + "px";
    const ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    draw(ctx, w, height, model);
    return w;
  }

  return { W, H, paint };
})();


/* ══ MochiMotion.kt ══════════════════════════════════════════════════════ */
const easeIn = t => t * t * t;
const easeOut = t => 1 - Math.pow(1 - t, 3);
const easeBoth = t => t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
const settleEase = t => 1 - Math.pow(2, -9 * t) * Math.cos(t * 13.5);

const BEATS = {
  // Load, leave, land, and wobble out. He is committed before he moves.
  mark: [[90, 1.10, .88, 0, 0, easeIn], [140, .93, 1.13, -34, 1, easeOut],
         [130, 1.06, .94, 0, 0, easeIn], [290, 1, 1, 0, 0, settleEase]],
  // No pop. Weight goes out of him and he sinks.
  miss: [[220, 1.02, .99, 2, 1, easeBoth], [480, 1.015, .985, 9, 0, easeOut]],
  // Mark, twice, the second bounce half the height of the first.
  unlock: [[110, 1.09, .90, 0, 0, easeIn], [150, .92, 1.15, -46, 1, easeOut],
           [130, 1.05, .95, 0, 0, easeIn], [120, .95, 1.08, -22, 0, easeOut],
           [110, 1.03, .97, 0, 0, easeIn], [280, 1, 1, 0, 0, settleEase]],
  settle: [[300, 1, 1, 0, 1, easeOut]],
};
const BREATH = 3.4, BLINK = .11;

class MochiMotion {
  constructor(mood = "resting") {
    this.mood = mood; this.scaleX = 1; this.scaleY = 1; this.offsetY = 0; this.idle = true;
    this.seg = null; this.i = 0; this.elapsed = 0; this.lands = mood;
    this.fromX = 1; this.fromY = 1; this.fromT = 0;
    this.poseX = 1; this.poseY = 1; this.poseT = 0;
    this.clock = 0; this.blinkAt = 2.4; this.blinkUntil = -1;
    this.resting = mood; this.face = mood;
  }
  play(which, face) {
    this.seg = BEATS[which]; this.i = 0; this.elapsed = 0;
    this.lands = face; this.resting = face;
    this.fromX = this.poseX; this.fromY = this.poseY; this.fromT = this.poseT;
  }
  snapTo(face) {
    this.seg = null; this.resting = face; this.face = face; this.mood = face;
    this.poseX = this.poseY = 1; this.poseT = 0;
    this.scaleX = this.scaleY = 1; this.offsetY = 0;
  }
  advance(dt) {
    this.clock += dt;
    this.step(dt);
    let bx = 1, by = 1;
    if (this.idle) {
      // Slow enough to read as breathing rather than as a pulse, and Y and X
      // move against each other so his volume stays about constant.
      const s = Math.sin(this.clock * (2 * Math.PI / BREATH));
      by = 1 + s * .012; bx = 1 - s * .006;
      // Only a face with open eyes can blink, and neither of the two the app
      // shows has any. He never blinks mid-beat either: a shut eye during a
      // hop reads as a flinch.
      if (opensEyes(this.face) && !this.seg && this.clock >= this.blinkAt) {
        this.blinkUntil = this.clock + BLINK;
        this.blinkAt = this.clock + 2.8 + Math.random() * 3.7;
      }
    }
    this.scaleX = this.poseX * bx; this.scaleY = this.poseY * by;
    this.offsetY = this.poseT;
    this.mood = this.clock < this.blinkUntil ? "resting" : this.face;
  }
  step(dt) {
    if (!this.seg) { this.face = this.resting; return; }
    const s = this.seg[this.i];
    this.elapsed += dt;
    const k = Math.min(Math.max(this.elapsed * 1000 / s[0], 0), 1);
    const e = s[5](k);
    this.poseX = this.fromX + (s[1] - this.fromX) * e;
    this.poseY = this.fromY + (s[2] - this.fromY) * e;
    this.poseT = this.fromT + (s[3] - this.fromT) * e;
    if (s[4]) this.face = this.lands;
    if (k >= 1) {
      // settle overshoots its way back and arrives a thousandth short.
      this.poseX = s[1]; this.poseY = s[2]; this.poseT = s[3];
      this.fromX = this.poseX; this.fromY = this.poseY; this.fromT = this.poseT;
      this.elapsed = 0; this.i++;
      if (this.i >= this.seg.length) this.seg = null;
    }
  }
}


/* ── Mochi, alive ────────────────────────────────────────────────────────
   One rig per canvas and one frame loop for all of them. He breathes, and a
   shut-eyed face cannot blink so the calm one does the opposite: it opens its
   eyes now and then and looks at you. */
const catsLive = [];

function mochi(canvas, opts) {
  const o = opts || {};
  const rig = new MochiMotion(o.mood || "resting");
  const still = matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (still) rig.idle = false;

  const state = {
    rig,
    height: 0,
    paint() {
      const dpr = window.devicePixelRatio || 1;
      const h = canvas.clientHeight;
      if (!h) return;
      const w = Cat.widthFor(h);
      canvas.style.width = w + "px";
      if (canvas.width !== Math.round(w * dpr) || canvas.height !== Math.round(h * dpr)) {
        canvas.width = Math.round(w * dpr);
        canvas.height = Math.round(h * dpr);
      }
      const ctx = canvas.getContext("2d");
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.clearRect(0, 0, w, h);
      const img = Cat.frame(h, rig.mood);
      ctx.save();
      // From the bottom centre: a squash presses him down rather than
      // shrinking him toward the middle of the frame.
      ctx.translate(w / 2, h);
      ctx.scale(rig.scaleX, rig.scaleY);
      ctx.translate(-w / 2, -h);
      ctx.translate(0, rig.offsetY * h / Cat.VH);
      ctx.drawImage(img, 0, 0, w, h);
      ctx.restore();
    },
    play(beat, face) {
      if (still) { rig.snapTo(face || rig.mood); state.paint(); return; }
      rig.play(beat, face || rig.mood);
    },
    set(face) { still ? rig.snapTo(face) : rig.play("settle", face); },
  };
  catsLive.push(state);
  return state;
}

let lastFrame = 0;
function tick(now) {
  const dt = lastFrame ? Math.min((now - lastFrame) / 1000, 0.05) : 0;
  lastFrame = now;
  for (const c of catsLive) { c.rig.advance(dt); c.paint(); }
  requestAnimationFrame(tick);
}
requestAnimationFrame(tick);

/* ══ What the page uses ══════════════════════════════════════════════════ */

/**
 * A year that looks lived in rather than sampled: kept from January with the
 * gaps a real one has, and a run at the end worth being proud of. The break
 * before that run is what makes the streak number true.
 */
function demoModel(opts) {
  const o = opts || {};
  const now = new Date();
  const today = epochDay({ y: now.getFullYear(), m: now.getMonth() + 1, d: now.getDate() });
  const t = fromEpochDay(today), doy = dayOfYear(t), done = new Set();
  const run = o.run == null ? 9 : o.run;
  for (let d = 1; d < doy; d++) if ((d * 7) % 9 !== 0 && (d * 5) % 14 !== 0) done.add(d);
  for (let d = Math.max(1, doy - run); d < doy; d++) done.add(d);
  done.delete(Math.max(1, doy - run - 1));
  if (o.marked) done.add(doy);
  return {
    title: o.title || "Read before sleep",
    slot: o.slot || "Evening",
    accent: accentAt(o.accent || 0),
    year: t.y, today,
    doneDaysOfYear: done,
    streak: o.marked ? run + 1 : run,
    totalDone: done.size,
    remaining: remainingIn(t.y, today),
    doneToday: !!o.marked,
    mood: o.marked ? "pleased" : "resting",
  };
}

/** Paints a ritual card into a canvas at its element size. */
function paintCard(canvas, model, cfg) {
  const dpr = window.devicePixelRatio || 1;
  const w = canvas.clientWidth;
  if (!w) return;
  const ratio = (cfg && cfg.ratio) || 353 / 172;
  const h = Math.round(w / ratio);
  canvas.width = Math.round(w * dpr);
  canvas.height = Math.round(h * dpr);
  canvas.style.height = h + "px";
  const ctx = canvas.getContext("2d");
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  // One scale on the density and every dp inside the card comes down with the
  // frame, so a smaller card is a photograph of the real one, not a redraw.
  Slab.draw(ctx, w, h, model, Object.assign({ scale: w / 353 }, cfg || {}));
}

window.Ritual = { ACCENTS, accentAt, Cat, Slab, Story, demoModel, paintCard,
                  mochi, MochiMotion, INK, PAPER, CREAM, LIME };
