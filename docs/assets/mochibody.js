/* ══ Mochi, full body ════════════════════════════════════════════════════
   His head and chest are the real portrait, drawn by Cat.frame exactly as
   everywhere else. Below it, a sitting body drawn the way the face is drawn:
   flat fills in the portrait's four greys, a 24-unit ink outline on the
   silhouette only, and tabby stripes as tapered blades rather than lines.
   The chest's own colour bands carry on below its bottom edge and end in a
   tufted fur edge, so there is no seam.

   Coordinates are the portrait's own units (883 x 913). The body continues
   below its bottom edge to a ground line at y = 1290. */
const MochiBody = (() => {
  const CW = 1400, CH = 1650, OX = 260, OY = 310, GROUND = 1290, LEDGE = 1500;
  const K = "#000000", BASE = "#646464", MID = "#8F8E8B", DARK = "#4A4A4A",
        LIGHT = "#ABAAA9", PALE = "#C7C6C6", PINK = "#D1837A",
        CREAMY = "#F4F2EA", INKY = "#12120F";
  const LIMEC = "#C9F73F", RED = "#E5331C", BUTTER = "#EDE55C", ORANGE = "#F26A1B";
  const OL = 24;
  const still = typeof matchMedia === "function" && matchMedia("(prefers-reduced-motion: reduce)").matches;
  const live = [];

  const S = Math.sin, C = Math.cos, PI = Math.PI, TAU = PI * 2;
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
  const smooth = u => { u = clamp(u, 0, 1); return u * u * (3 - 2 * u); };
  const pop = u => { u = clamp(u, 0, 1); const c = 1.9; return 1 + (c + 1) * (u - 1) ** 3 + c * (u - 1) ** 2; };
  const cyc = (t, period, off) => ((((t + (off || 0)) / period) % 1) + 1) % 1;

  /* A hop with weight: squash on landing and before take-off, stretch on
     the way up and down. `sq` is negative for squash, positive for stretch.
     Every part of it starts and ends at zero, so nothing ever snaps from one
     frame to the next: the stretch eases in after take-off and out before
     the apex, and the two squashes are each one smooth dip on the ground. */
  function jump(t, period, height, air) {
    const u = cyc(t, period);
    if (u < air) { const k = u / air; return { hop: S(PI * k) * height, sq: 0.7 * S(PI * k) * Math.abs(C(PI * k)), land: -1 }; }
    const v = (u - air) / (1 - air), L = Math.min(0.5, 0.32 / ((1 - air) * period));
    let sq = 0;
    if (v < L) sq -= 0.6 * S(PI * v / L);
    if (v > 1 - L) sq -= 0.5 * S(PI * (v - (1 - L)) / L);
    return { hop: 0, sq, land: v };
  }

  /* The same hop, once, for a kick: a dip, a jump, a dip. Zero at both ends. */
  function hopOnce(u, height) {
    if (u < 0 || u > 1) return { hop: 0, sq: 0 };
    if (u < 0.24) return { hop: 0, sq: -0.5 * S(PI * u / 0.24) };
    if (u < 0.76) { const k = (u - 0.24) / 0.52; return { hop: S(PI * k) * height, sq: 0.7 * S(PI * k) * Math.abs(C(PI * k)) }; }
    return { hop: 0, sq: -0.55 * S(PI * (u - 0.76) / 0.24) };
  }

  /* ── drawing helpers ───────────────────────────────────────────────── */
  function qpts(a, c, b, n) {
    const out = [];
    for (let i = 0; i <= n; i++) {
      const u = i / n, v = 1 - u;
      out.push([v * v * a[0] + 2 * v * u * c[0] + u * u * b[0], v * v * a[1] + 2 * v * u * c[1] + u * u * b[1]]);
    }
    return out;
  }
  /* Offsets a centre line both ways by a width that varies along it. */
  function ribbon(pts, wAt) {
    const L = [], R = [], n = pts.length;
    for (let i = 0; i < n; i++) {
      const a = pts[Math.max(0, i - 1)], b = pts[Math.min(n - 1, i + 1)];
      let tx = b[0] - a[0], ty = b[1] - a[1]; const m = Math.hypot(tx, ty) || 1; tx /= m; ty /= m;
      const w = wAt(i / (n - 1)) / 2;
      L.push([pts[i][0] - ty * w, pts[i][1] + tx * w]); R.push([pts[i][0] + ty * w, pts[i][1] - tx * w]);
    }
    return { L, R };
  }
  function ribbonPath(x, r) {
    x.beginPath(); x.moveTo(...r.L[0]);
    for (const q of r.L.slice(1)) x.lineTo(...q);
    for (const q of r.R.slice().reverse()) x.lineTo(...q);
    x.closePath();
  }
  /* A tabby stripe: a curved blade, pointed at both ends or only the far one. */
  function blade(x, a, c, b, w, col, oneEnd) {
    const r = ribbon(qpts(a, c, b, 14), u => w * (oneEnd ? Math.pow(1 - u, 0.75) : Math.pow(S(PI * u), 0.7)));
    ribbonPath(x, r); x.fillStyle = col; x.fill();
  }
  function outline(x, w) { x.lineWidth = w || OL; x.strokeStyle = K; x.lineJoin = "round"; x.lineCap = "round"; x.stroke(); }
  function inked(x, fill) { x.fillStyle = fill; x.fill(); outline(x); }
  function line(x, a, b, c, d, w, col) {
    x.beginPath(); x.moveTo(a, b); x.lineTo(c, d);
    x.lineWidth = w; x.strokeStyle = col; x.lineCap = "round"; x.stroke();
  }
  function curve(x, a, b, cx, cy, c, d, w, col) {
    x.beginPath(); x.moveTo(a, b); x.quadraticCurveTo(cx, cy, c, d);
    x.lineWidth = w; x.strokeStyle = col; x.lineCap = "round"; x.stroke();
  }

  /* ── body ──────────────────────────────────────────────────────────── */
  /* The silhouette. It starts well up inside the portrait's chest so a head
     tilt never opens a gap at the corners, and meets the portrait's own
     outline at its bottom edge (x 95 and 773 at y 905). */
  function torsoPath(x) {
    x.beginPath();
    x.moveTo(140, 760);
    x.quadraticCurveTo(100, 840, 95, 905);
    x.bezierCurveTo(84, 1010, 52, 1120, 76, 1200);
    x.bezierCurveTo(100, 1282, 190, 1290, 270, 1290);
    x.lineTo(598, 1290);
    x.bezierCurveTo(678, 1290, 768, 1282, 792, 1200);
    x.bezierCurveTo(816, 1120, 784, 1010, 773, 905);
    x.quadraticCurveTo(768, 840, 728, 760);
  }

  function drawTorso(x) {
    torsoPath(x);
    x.save(); x.clip();
    x.fillStyle = BASE; x.fillRect(0, 700, 900, 650);
    // The portrait's dark side bands, carried down the flanks.
    torsoPath(x); x.lineWidth = 150; x.strokeStyle = DARK; x.stroke();
    // Flank stripes: blades from the edge, pointing in and down.
    for (const m of [0, 1]) {
      const X = v => m ? 868 - v : v;
      blade(x, [X(40), 975], [X(150), 985], [X(250), 1030], 44, DARK, true);
      blade(x, [X(30), 1060], [X(140), 1070], [X(230), 1120], 40, DARK, true);
      blade(x, [X(40), 1140], [X(120), 1150], [X(190), 1190], 34, DARK, true);
    }
    // Belly, a lighter oval with small fur tufts.
    x.beginPath(); x.ellipse(434, 1150, 175, 150, 0, 0, TAU); x.fillStyle = MID; x.fill();
    x.beginPath(); x.ellipse(434, 1170, 120, 110, 0, 0, TAU); x.fillStyle = LIGHT; x.fill();
    for (const [a, b, s] of [[380, 1095, 1], [488, 1095, 1], [434, 1140, 1.2], [392, 1195, 0.9], [476, 1195, 0.9]]) {
      blade(x, [a - 26 * s, b - 14 * s], [a - 8 * s, b + 6 * s], [a, b + 22 * s], 14 * s, PALE);
      blade(x, [a + 26 * s, b - 14 * s], [a + 8 * s, b + 6 * s], [a, b + 22 * s], 14 * s, PALE);
    }
    x.restore();
    torsoPath(x); outline(x);
  }

  /* The chest's light bib, continued below the portrait's edge and ending
     in a row of tufts, with the centre stripe running into it to a point.
     Drawn over the tops of the front legs so fur hangs over them. */
  function drawBib(x) {
    const tufts = [[232, 930], [262, 1004], [300, 962], [340, 1024], [382, 970], [434, 1036],
                   [486, 970], [528, 1024], [568, 962], [606, 1004], [634, 930]];
    const bib = dy => {
      x.beginPath(); x.moveTo(232, 880 + dy);
      for (const [a, b] of tufts) x.lineTo(a, b + dy);
      x.lineTo(634, 880 + dy); x.closePath();
    };
    bib(16); x.fillStyle = DARK; x.fill();
    bib(0); x.fillStyle = MID; x.fill();
    blade(x, [440, 880], [440, 940], [436, 1000], 60, BASE, true);
    for (const a of [300, 568]) blade(x, [a, 920], [a + (a < 434 ? 10 : -10), 950], [a, 985], 22, LIGHT);
    // The two base-grey bands either side of the bib, tapering into the flank.
    blade(x, [200, 880], [205, 930], [215, 975], 60, BASE, true);
    blade(x, [660, 880], [655, 930], [645, 975], 60, BASE, true);
  }

  /* A sitting cat's thigh: a round haunch with a tabby swirl, and the hind
     foot poking out in front of it. */
  function drawHaunch(x, m, noFoot) {
    const cx = m ? 668 : 200, rot = m ? -0.22 : 0.22, X = v => m ? 868 - v : v;
    const path = () => { x.beginPath(); x.ellipse(cx, 1196, 134, 98, rot, 0, TAU); };
    path(); x.save(); x.clip();
    x.fillStyle = BASE; x.fill();
    path(); x.lineWidth = 70; x.strokeStyle = DARK; x.stroke();
    blade(x, [X(110), 1150], [X(200), 1105], [X(290), 1160], 40, DARK);
    blade(x, [X(125), 1225], [X(200), 1180], [X(270), 1225], 32, DARK);
    blade(x, [X(150), 1130], [X(210), 1122], [X(250), 1140], 14, MID);
    x.restore();
    path(); outline(x);
    if (!noFoot) footFront(x, X(290), 1266, m ? 0.12 : -0.12, 0.82);
  }

  /* ── limbs ─────────────────────────────────────────────────────────── */
  /* Every arm, leg and foot is one closed outline, the way the face is
     drawn: a spine that bends softly at the elbow and wrist, a width that
     swells at the upper arm, narrows at the wrist and swells again into the
     paw, a couple of fur tufts at the elbow like the ones on his cheeks, and
     three toes scalloped into the paw's edge. No straight edges anywhere. */
  const lerpK = (keys, u) => {
    for (let i = 1; i < keys.length; i++) if (u <= keys[i][0]) {
      const [u0, a] = keys[i - 1], [u1, b] = keys[i];
      return a + (b - a) * smooth((u - u0) / (u1 - u0));
    }
    return keys[keys.length - 1][1];
  };
  const ARM_W = [[0, 134], [0.22, 140], [0.56, 112], [0.68, 116], [0.86, 176], [1, 164]];
  const LEG_W = [[0, 132], [0.3, 118], [0.66, 98], [0.8, 124], [1, 164]];
  const FOOT_W = [[0, 138], [1, 158]];

  /* The centre line: short links whose heading drifts by `bend` through the
     middle and by `wrist` near the end. Heading 0 is straight down. */
  function spine(sx, sy, th, L, bend, wrist, n) {
    const pts = [[sx, sy]], heads = [th];
    for (let i = 1; i <= n; i++) {
      const u = i / n, a = th + bend * smooth((u - 0.28) / 0.34) + wrist * smooth((u - 0.66) / 0.2);
      const [px, py] = pts[i - 1];
      pts.push([px - (L / n) * S(a), py + (L / n) * C(a)]); heads.push(a);
    }
    return { pts, heads };
  }

  function limb(x, o) {
    const n = 48, sp = spine(o.sx, o.sy, o.th, o.L, o.bend || 0, o.wrist || 0, n), P = sp.pts;
    const Lp = [], Rp = [], W = o.W || ARM_W;
    for (let i = 0; i <= n; i++) {
      const u = i / n, a = sp.heads[i], tx = -S(a), ty = C(a), w = lerpK(W, u) / 2;
      const spike = o.tuft ? (Math.max(0, 1 - Math.abs(u - 0.4) / 0.04) + 0.8 * Math.max(0, 1 - Math.abs(u - 0.49) / 0.04)) * 0.2 : 0;
      const wl = w * (1 + (o.tuft > 0 ? spike : 0)), wr = w * (1 + (o.tuft < 0 ? spike : 0));
      Lp.push([P[i][0] - ty * wl, P[i][1] + tx * wl]); Rp.push([P[i][0] + ty * wr, P[i][1] - tx * wr]);
    }
    // The paw's end, round, with two notches between three toes.
    const aE = sp.heads[n], d = [-S(aE), C(aE)], nl = [-d[1], d[0]], E = P[n], wE = lerpK(W, 1) / 2, depth = o.depth || 0.72;
    const cap = [], notches = [];
    for (let k = 1; k < 30; k++) {
      const ang = PI * k / 30, g = z => Math.exp(-((z / 0.085) ** 2));
      const r = 1 - (o.toes === false ? 0 : 0.11 * (g(ang - PI * 0.34) + g(ang - PI * 0.66)));
      const q = [E[0] + (nl[0] * C(ang) + d[0] * depth * S(ang)) * wE * r, E[1] + (nl[1] * C(ang) + d[1] * depth * S(ang)) * wE * r];
      cap.push(q);
      if (k === 10 || k === 20) notches.push(q);
    }
    // A rounded start, where the limb shows its shoulder.
    const start = [];
    if (o.startCap) {
      const a0 = sp.heads[0], d0 = [-S(a0), C(a0)], n0 = [-d0[1], d0[0]], w0 = lerpK(W, 0) / 2;
      for (let k = 1; k < 20; k++) {
        const ang = PI + PI * k / 20;
        start.push([P[0][0] + (n0[0] * C(ang) + d0[0] * 0.8 * S(ang)) * w0, P[0][1] + (n0[1] * C(ang) + d0[1] * 0.8 * S(ang)) * w0]);
      }
    }
    const path = () => {
      x.beginPath(); x.moveTo(...Lp[0]);
      for (const q of Lp) x.lineTo(...q);
      for (const q of cap) x.lineTo(...q);
      for (let i = n; i >= 0; i--) x.lineTo(...Rp[i]);
      for (const q of start) x.lineTo(...q);
      x.closePath();
    };
    const at = u => Math.round(u * n);
    const Pc = [E[0] - d[0] * wE * 0.3, E[1] - d[1] * wE * 0.3];

    path(); x.save(); x.clip();
    x.fillStyle = BASE; x.fillRect(-3000, -3000, 6000, 6000);
    // Shade down one side, as the portrait shades its cheeks and chest.
    x.beginPath(); x.moveTo(...Rp[0]); for (let i = 1; i <= at(0.8); i++) x.lineTo(...Rp[i]);
    x.lineWidth = 46; x.strokeStyle = DARK; x.lineJoin = "round"; x.stroke();
    // Tabby bands across the limb.
    for (const u of o.stripes || [0.3, 0.5]) {
      const i = at(u), a = sp.heads[i], dd = [-S(a) * 22, C(a) * 22];
      const ext = (p, q) => [p[0] + (p[0] - q[0]) * 0.3, p[1] + (p[1] - q[1]) * 0.3];
      blade(x, ext(Lp[i], P[i]), [P[i][0] + dd[0], P[i][1] + dd[1]], ext(Rp[i], P[i]), 30, DARK);
    }
    // A soft highlight down the other side.
    const mixL = u => { const i = at(u); return [(Lp[i][0] * 3 + P[i][0] * 2) / 5, (Lp[i][1] * 3 + P[i][1] * 2) / 5]; };
    blade(x, mixL(0.06), mixL(0.3), mixL(0.6), 18, MID);
    // The paw, lighter, a mitten inside the same outline.
    x.save(); x.translate(...Pc); x.rotate(aE);
    x.beginPath(); x.ellipse(0, 10, wE * 1.12, wE * 0.95, 0, 0, TAU); x.fillStyle = o.pad ? PALE : LIGHT; x.fill();
    if (!o.pad) { x.beginPath(); x.ellipse(-wE * 0.3, -wE * 0.15, wE * 0.42, wE * 0.26, 0.2, 0, TAU); x.fillStyle = PALE; x.fill(); }
    x.restore();
    x.restore();

    path(); outline(x);
    if (o.pad) beans(x, Pc, aE, wE);
    else for (const q of notches) {
      const v = [Pc[0] - q[0], Pc[1] - q[1]], m = Math.hypot(...v);
      curve(x, q[0], q[1], q[0] + v[0] / m * wE * 0.2 + d[1] * 4, q[1] + v[1] / m * wE * 0.2 - d[0] * 4,
            q[0] + v[0] / m * wE * 0.42, q[1] + v[1] / m * wE * 0.42, 11, K);
    }
    return { paw: Pc, end: E, head: aE };
  }

  /* Palm out: a heart-shaped main pad and four toe beans, pointing along
     the paw toward its toes. */
  function beans(x, Pc, a, wE) {
    x.save(); x.translate(...Pc); x.rotate(a); const s = wE / 74; x.scale(s, s);
    x.beginPath(); x.moveTo(0, -46);
    x.bezierCurveTo(-48, -46, -48, 0, -22, 4); x.quadraticCurveTo(0, 10, 22, 4);
    x.bezierCurveTo(48, 0, 48, -46, 0, -46); x.fillStyle = PINK; x.fill();
    x.beginPath(); x.ellipse(-10, -26, 12, 7, 0.3, 0, TAU); x.fillStyle = "#E3A39B"; x.fill();
    for (const [bx, by, r] of [[-52, 18, -0.5], [-20, 44, -0.18], [20, 44, 0.18], [52, 18, 0.5]]) {
      x.beginPath(); x.ellipse(bx, by, 15, 19, r, 0, TAU); x.fillStyle = PINK; x.fill();
    }
    x.restore();
  }

  /* A paw on the ground seen from the front: a short foot with a rounded
     top and its toes along the bottom edge. */
  function footFront(x, cx, cy, rot, s) {
    x.save(); x.translate(cx, cy); x.rotate(rot || 0); x.scale(s || 1, s || 1);
    limb(x, { sx: 0, sy: -34, th: 0, L: 30, W: FOOT_W, depth: 0.55, startCap: true, stripes: [], tuft: 0 });
    x.restore();
  }

  /* A front leg standing: chubby at the top, narrowing, then the paw. */
  function drawLeg(x, cx, lift) {
    const m = cx < 434 ? -1 : 1;
    limb(x, { sx: cx, sy: 925, th: 0, L: 318 - (lift || 0), bend: 0.05 * m, W: LEG_W, depth: 0.5, stripes: [0.46, 0.62] });
  }

  /* A raised front leg from the shoulder, bent softly toward his head. With
     `pad`, the palm faces out; otherwise the paw is curled and toes show. */
  const ARM_LEN = 272;
  function armSpine(sx, sy, th, wrist, len) {
    const L = len || ARM_LEN, bend = 0.2 * Math.sign(th), sp = spine(sx, sy, th, L, bend, wrist || 0, 48);
    const a = sp.heads[48], E = sp.pts[48], wE = lerpK(ARM_W, 1) / 2;
    return { bend, L, paw: [E[0] + S(a) * wE * 0.3, E[1] - C(a) * wE * 0.3], head: a };
  }
  function drawArm(x, sx, sy, th, wrist, pad, len) {
    const g = armSpine(sx, sy, th, wrist, len);
    return limb(x, { sx, sy, th, L: g.L, bend: g.bend, wrist: wrist || 0, W: ARM_W, pad, startCap: true,
                     tuft: th > 0 ? -1 : 1, stripes: [0.28, 0.46], depth: 0.78 }).paw;
  }

  /* The tail as a chain whose links each lag the one before, so a swish
     travels from the base out to the tip. Tapered, ringed, dark-tipped. */
  function drawTail(x, tl, t) {
    const n = 13, seg = tl.seg || 29;
    const chain = c => {
      const q = [[c.bx || 706, c.by || 1236]];
      for (let i = 1; i <= n; i++) {
        const a = c.a0 + c.curl * i + c.sway * S(t * c.f - i * 0.42) * (0.25 + i / n);
        const [px, py] = q[i - 1]; q.push([px + seg * C(a), py + seg * S(a)]);
      }
      return q;
    };
    let pts = chain(tl);
    if (tl.from) { const a = chain(tl.from); pts = pts.map((q, i) => [a[i][0] + (q[0] - a[i][0]) * tl.e, a[i][1] + (q[1] - a[i][1]) * tl.e]); }
    const w = u => 76 - 22 * u;
    const r = ribbon(pts, w), tip = pts[n], tr = w(1) / 2;
    ribbonPath(x, r); x.lineWidth = OL * 2; x.strokeStyle = K; x.lineJoin = "round"; x.stroke();
    x.beginPath(); x.arc(tip[0], tip[1], tr, 0, TAU); x.lineWidth = OL * 2; x.stroke();
    ribbonPath(x, r); x.fillStyle = BASE; x.fill();
    const band = (i, j, col) => {
      x.beginPath(); x.moveTo(...r.L[i]); for (let k = i + 1; k <= j; k++) x.lineTo(...r.L[k]);
      for (let k = j; k >= i; k--) x.lineTo(...r.R[k]); x.closePath(); x.fillStyle = col; x.fill();
    };
    band(3, 4, DARK); band(6, 7, DARK); band(9, 10, DARK); band(11, n, DARK);
    x.beginPath(); x.arc(tip[0], tip[1], tr, 0, TAU); x.fillStyle = DARK; x.fill();
    x.beginPath();
    for (let i = 1; i < 9; i++) {
      const q = [(r.L[i][0] * 3 + pts[i][0]) / 4, (r.L[i][1] * 3 + pts[i][1]) / 4];
      i === 1 ? x.moveTo(...q) : x.lineTo(...q);
    }
    x.lineWidth = 9; x.strokeStyle = MID; x.lineCap = "round"; x.stroke();
  }

  /* Hind legs hanging over the edge he is sitting on, kicking in turn. */
  function drawDangle(x, t) {
    for (const m of [0, 1]) {
      const sw = S(t * 3.2 + m * PI) * 0.32;
      limb(x, { sx: m ? 680 : 188, sy: 1200, th: sw, L: 150, bend: sw * 0.4, W: [[0, 120], [0.55, 92], [1, 140]],
                depth: 0.55, stripes: [0.45], tuft: 0 });
    }
  }

  /* ── props ─────────────────────────────────────────────────────────── */
  /* Held the way you hold up a key: the bow in the paw, the blade pointing
     up, the teeth on the outside. */
  function drawKey(x) {
    x.save(); x.translate(0, -30); x.scale(1.8, 1.8);
    x.beginPath(); x.moveTo(-19, -20); x.lineTo(-19, -300); x.quadraticCurveTo(-19, -322, 0, -330);
    x.quadraticCurveTo(19, -322, 19, -300); x.lineTo(19, -20); x.closePath();
    x.rect(19, -292, 50, 26); x.rect(19, -248, 36, 24); inked(x, INKY);
    x.beginPath(); x.arc(0, 20, 72, 0, TAU); inked(x, INKY);
    x.beginPath(); x.arc(0, 20, 28, 0, TAU); x.fillStyle = LIMEC; x.fill();
    x.beginPath(); x.arc(-8, -200, 7, 0, TAU); x.arc(-8, -150, 7, 0, TAU); x.fillStyle = "#3A3A33"; x.fill();
    x.restore();
  }
  function drawMiniCard(x, t) {
    x.save(); x.rotate(S(t * 2.2) * 0.05);
    x.beginPath(); x.roundRect(-200, -128, 400, 256, 44); inked(x, RED);
    const lit = Math.min(32, Math.floor(cyc(t, 3.2) * 34) + 16);
    for (let c = 0; c < 8; c++) for (let r = 0; r < 4; r++) {
      x.beginPath(); x.roundRect(-158 + c * 40, -40 + r * 38, 28, 28, 7);
      x.fillStyle = (c * 4 + r) < lit ? INKY : "rgba(244,242,234,.38)"; x.fill();
    }
    x.fillStyle = CREAMY; x.font = "800 56px Archivo, system-ui, sans-serif"; x.fillText("+1", -158, -64);
    x.restore();
  }
  function drawPhone(x) {
    x.save(); x.translate(0, -120); x.rotate(-0.12);
    x.beginPath(); x.roundRect(-100, -175, 200, 350, 36); inked(x, INKY);
    x.beginPath(); x.roundRect(-76, -150, 96, 96, 24); x.fillStyle = "#2B2B26"; x.fill();
    for (const [a, b] of [[-50, -124], [-6, -124], [-50, -80]]) { x.beginPath(); x.arc(a, b, 17, 0, TAU); x.fillStyle = LIMEC; x.fill(); }
    x.restore();
  }
  function drawCloud(x, t) {
    const puffs = [[-360, 20, 92], [-200, -22, 124], [20, -52, 138], [240, -22, 118], [390, 24, 88]];
    const shape = () => {
      x.beginPath();
      puffs.forEach(([a, b, r], i) => { const rr = r + S(t * 2 + i) * 6; x.moveTo(a + rr, b); x.arc(a, b, rr, 0, TAU); });
      x.roundRect(-460, 10, 920, 120, 60);
    };
    x.save(); x.translate(434, 1390);
    shape(); x.lineWidth = OL * 2; x.strokeStyle = K; x.lineJoin = "round"; x.stroke();
    shape(); x.fillStyle = CREAMY; x.fill();
    for (const [a, b, r] of puffs.slice(1, 4)) curve(x, a - r * 0.55, b + r * 0.15, a, b + r * 0.55, a + r * 0.55, b + r * 0.15, 12, "#DDD9CB");
    x.restore();
    x.save(); x.translate(434, 1450);
    line(x, 0, 60, 0, -44, 30, INKY); line(x, -46, 0, 0, -48, 30, INKY); line(x, 46, 0, 0, -48, 30, INKY);
    x.restore();
  }

  /* ── effects ───────────────────────────────────────────────────────── */
  function heart(x, cx, cy, s, a, col) {
    if (s <= 0 || a <= 0) return;
    x.save(); x.globalAlpha = clamp(a, 0, 1); x.translate(cx, cy); x.scale(s, s);
    x.beginPath(); x.moveTo(0, 40);
    x.bezierCurveTo(-70, -8, -46, -62, 0, -28);
    x.bezierCurveTo(46, -62, 70, -8, 0, 40); x.closePath();
    x.fillStyle = col || RED; x.fill(); x.lineWidth = 12; x.strokeStyle = K; x.lineJoin = "round"; x.stroke();
    x.beginPath(); x.ellipse(-22, -18, 9, 6, -0.6, 0, TAU); x.fillStyle = "rgba(255,255,255,.55)"; x.fill();
    x.restore();
  }
  function sparkle(x, cx, cy, r, a, col, rot) {
    if (r <= 0 || a <= 0) return;
    x.save(); x.globalAlpha = clamp(a, 0, 1); x.translate(cx, cy); x.rotate(rot || 0);
    x.beginPath(); x.moveTo(r, 0);
    for (let i = 0; i < 4; i++) {
      const q = i * PI / 2;
      x.quadraticCurveTo(C(q + PI / 4) * r * 0.14, S(q + PI / 4) * r * 0.14, C(q + PI / 2) * r, S(q + PI / 2) * r);
    }
    x.closePath(); x.fillStyle = col || BUTTER; x.fill(); x.lineWidth = 10; x.strokeStyle = K; x.lineJoin = "round"; x.stroke();
    x.restore();
  }
  function puff(x, cx, cy, v) {
    if (v < 0 || v > 0.6) return;
    const k = v / 0.6;
    x.save(); x.globalAlpha = 1 - k;
    for (const d of [-1, 1]) for (const [o, r] of [[0, 1], [70, 0.7]]) {
      x.beginPath(); x.arc(cx + d * (300 + o + k * 120), cy - 20 - k * 40, (30 + k * 22) * r, 0, TAU);
      x.fillStyle = CREAMY; x.fill(); x.lineWidth = 9; x.strokeStyle = K; x.stroke();
    }
    x.restore();
  }
  /* A speech bubble as wide as what it says, its tail pointing down-left
     toward his face. */
  function bubble(x, cx, cy, s, text) {
    if (s <= 0.02) return;
    x.save(); x.translate(cx, cy); x.scale(s * 1.45, s * 1.45);
    let size = 88; x.font = `800 ${size}px Archivo, system-ui, sans-serif`;
    let tw = x.measureText(text).width;
    if (tw > 420) { size = Math.floor(size * 420 / tw); x.font = `800 ${size}px Archivo, system-ui, sans-serif`; tw = 420; }
    const hw = Math.max(130, tw / 2 + 60);
    x.beginPath(); x.roundRect(-hw, -95, hw * 2, 150, 60);
    x.moveTo(-hw + 60, 50); x.lineTo(-hw + 20, 115); x.lineTo(-hw + 130, 52);
    x.fillStyle = CREAMY; x.fill(); x.lineWidth = 16; x.strokeStyle = K; x.lineJoin = "round"; x.stroke();
    x.beginPath(); x.rect(-hw + 50, 40, 90, 20); x.fillStyle = CREAMY; x.fill();
    x.fillStyle = INKY; x.textAlign = "center"; x.textBaseline = "middle";
    x.fillText(text, 0, -18); x.restore();
  }
  function confetti(x, t, seed) {
    for (let i = 0; i < 34; i++) {
      const r = n => { const v = S(seed + i * 12.9898 + n * 78.233) * 43758.5453; return v - Math.floor(v); };
      const speed = 0.22 + r(1) * 0.3, y = ((t * speed + r(2)) % 1) * (CH + 160) - 120;
      const px = r(3) * CW + S(t * 2 + i) * 40;
      x.save(); x.translate(px, y); x.rotate(t * (1 + r(4) * 3) + i);
      x.scale(1, C(t * (3 + r(5) * 4) + i));
      x.fillStyle = [LIMEC, RED, BUTTER, ORANGE, CREAMY, INKY][i % 6];
      if (i % 3 === 0) x.fillRect(-10, -34, 20, 68); else x.fillRect(-22, -22, 44, 44);
      x.restore();
    }
  }
  function zzz(x, t) {
    x.save(); x.fillStyle = INKY; x.textAlign = "center";
    for (let i = 0; i < 3; i++) {
      const k = cyc(t * 0.33, 1, i / 3);
      x.globalAlpha = k < 0.15 ? k / 0.15 : 1 - (k - 0.15) / 0.85;
      x.font = `800 ${Math.round(70 + k * 70)}px Archivo, system-ui, sans-serif`;
      x.fillText("z", 830 + k * 190 + S(k * 7) * 30, 260 - k * 380);
    }
    x.restore();
  }

  /* ── poses ─────────────────────────────────────────────────────────── */
  /* Each pose returns what differs from sitting still. Arms are angles in
     radians from hanging straight down: negative swings the right paw up
     and out, positive the left. `front` draws effects over him, in the
     hopped space; `ground` draws on the ground, before him. */
  const TAIL = (f, sway, extra) => Object.assign({ a0: 0.3, curl: -0.215, sway, f }, extra || {});
  const POSES = {
    wave: { mood: "pleased", label: "Welcome, sign in",
      f: t => ({ armR: -2.55 + S(t * 7) * 0.2, wristR: S(t * 7 - 1) * 0.5, padR: true,
                 tilt: 0.04 + S(t * 1.7) * 0.05, tail: TAIL(2.4, 0.3), ...jump(t, 3.4, 26, 0.18) }),
      front(x, t, p, paw) {
        for (let i = 0; i < 2; i++) {
          const u = cyc(t, 2.6, i * 1.3);
          heart(x, paw.R[0] + 60 + S(u * 9) * 26 + u * 70, paw.R[1] - 70 - u * 300, pop(u * 5) * (1 - u ** 4) * 0.95, 1 - u ** 3);
        }
      } },
    hello: { mood: "resting", label: "What should we call you",
      f: t => { const u = cyc(t, 4.4), e = smooth(u / 0.08) * (1 - smooth((u - 0.5) / 0.1));
        // One envelope rises and falls around the whole greeting, so the wave
        // eases into and out of his resting pose instead of switching on.
        return { tilt: e * 0.12 + (1 - e) * S(t * 1.3) * 0.03,
                 armL: 2.0 + e * (0.45 + S(t * 14) * 0.1), wristL: 0.2 + e * (S(t * 14 - 0.8) * 0.4 - 0.2), padL: true,
                 tail: TAIL(2, 0.34), ...hopOnce(u * 4.4 / 1.1, 40) }; },
      front(x, t, p, paw, st) { const u = cyc(t, 4.4);
        bubble(x, 640, -150, u < 0.6 ? pop(u / 0.1) * (1 - smooth((u - 0.52) / 0.08)) : 0, st.say ? `hi, ${st.say}!` : "hi!"); } },
    peek: { mood: "resting", label: "How should it look",
      f: t => { const u = cyc(t, 6.5);
        const up = u < 0.2 ? pop(u / 0.2) : u < 0.8 ? 1 : u < 0.92 ? 1 - smooth((u - 0.8) / 0.12) : 0;
        const look = u > 0.18 && u < 0.78 ? S((u - 0.18) / 0.6 * TAU * 1.5) * 0.14 : 0;
        return { peek: true, dy: 1010 - 500 * up, tilt: look, wig: S(t * 8) * 6 }; } },
    sit: { mood: "resting", label: "Finish the sentence",
      f: t => { const u = cyc(t, 5.5);
        return { tilt: S(t * 1.1) * 0.03 + (u > 0.6 && u < 0.85 ? 0.12 * S((u - 0.6) / 0.25 * PI) : 0),
                 tail: TAIL(1.6, 0.3) }; } },
    cheer: { mood: "pleased", label: "Thirty days from today",
      f: t => { const j = jump(t, 0.95, 130, 0.5), up = j.hop / 130;
        return { ...j, armL: 2.55 + up * 0.35, armR: -2.55 - up * 0.35, wristL: S(t * 13) * 0.3, wristR: -S(t * 13) * 0.3,
                 padL: true, padR: true, tail: TAIL(5, 0.45) }; },
      ground(x, t, p) { puff(x, 434, GROUND, p.land); },
      front(x, t, p, paw) { if (p.hop > 90) { const k = (p.hop - 90) / 40;
        sparkle(x, paw.L[0] - 60, paw.L[1] - 90, 44 * k, k, BUTTER, t * 3);
        sparkle(x, paw.R[0] + 60, paw.R[1] - 90, 44 * k, k, LIMEC, -t * 3); } } },
    perch: { mood: "pleased", label: "On your home screen",
      f: t => ({ dangle: true, dy: -300, tilt: S(t * 1.2) * 0.07,
                 tail: TAIL(2.2, 0.5, { a0: 1.35, curl: 0.04, bx: 760, by: 1250 }) }),
      front(x, t) { const u = cyc(t, 3.2); x.save(); x.globalAlpha = 1 - u; x.fillStyle = INKY;
        x.font = "800 110px Archivo, system-ui, sans-serif"; x.fillText("♪", 860 + S(u * 8) * 30, 200 - u * 260); x.restore(); } },
    key: { mood: "pleased", label: "Your first ritual is free",
      f: t => ({ armR: -2.5 + S(t * 3) * 0.1, wristR: S(t * 3 - 0.7) * 0.22, prop: "key",
                 tilt: -0.05 + S(t * 1.8) * 0.04, tail: TAIL(3, 0.32), ...jump(t, 1.8, 36, 0.32) }),
      front(x, t, p, paw) { const u = cyc(t, 1.6);
        sparkle(x, paw.R[0] - 10, paw.R[1] - 260, 60 * S(clamp(u / 0.4, 0, 1) * PI), 1, BUTTER, u * 2);
        sparkle(x, paw.R[0] + 120, paw.R[1] - 120, 30 * S(clamp((u - 0.3) / 0.4, 0, 1) * PI), 1, CREAMY, 0); } },
    card: { mood: "pleased", label: "Build the next one",
      f: t => ({ prop: "card", cardY: -Math.abs(S(t * 2.2)) * 46, tilt: S(t * 2.2) * 0.04, tail: TAIL(2.5, 0.3) }),
      front(x, t, p) { for (let i = 0; i < 2; i++) { const u = cyc(t, 1.9, i * 0.95);
        x.save(); x.globalAlpha = 1 - u ** 2; x.fillStyle = INKY;
        x.font = `800 ${Math.max(1, Math.round(110 * pop(u * 4)))}px Archivo, system-ui, sans-serif`;
        x.textAlign = "center"; x.fillText("+1", 434 + (i ? 330 : -330) + S(u * 6) * 20, 1010 + p.cardY - u * 260); x.restore(); } } },
    selfie: { mood: "pleased", label: "Share it on Instagram",
      f: t => { const u = cyc(t, 2.8);
        return { armR: -2.42 + S(t * 1.4) * 0.05, wristR: -0.15, prop: "phone", tilt: 0.11 + (u < 0.12 ? S(u / 0.12 * PI) * 0.05 : 0),
                 armL: 2.05 + S(t * 6) * 0.1, wristL: -0.3, padL: true, flash: u, tail: TAIL(2, 0.3) }; },
      front(x, t, p, paw) { const u = p.flash;
        if (u < 0.16) { const k = u / 0.16; x.save(); x.globalAlpha = 1 - k;
          x.beginPath(); x.arc(paw.R[0] - 30, paw.R[1] - 260, 50 + k * 240, 0, TAU); x.fillStyle = "#FFFFFF"; x.fill(); x.restore(); }
        if (u > 0.2) { const k = (u - 0.2) / 0.8;
          heart(x, paw.R[0] + 90 + k * 60, paw.R[1] - 300 - k * 200, pop(k * 5) * 0.9 * (1 - k ** 4), 1 - k ** 3);
          heart(x, paw.R[0] + 180 + k * 30, paw.R[1] - 200 - k * 150, pop(k * 4 - 0.4) * 0.6 * (1 - k ** 4), 1 - k ** 3, PINK); } } },
    cloud: { mood: "resting", label: "Back it up",
      f: t => ({ prop: "cloud", dy: -210 + S(t * 1.8) * 22, tilt: S(t * 1.8 - 0.8) * 0.05,
                 tail: TAIL(1.6, 0.4, { a0: 1.2, curl: 0.03, bx: 760, by: 1250 }) }),
      front(x, t) { for (let i = 0; i < 3; i++) { const u = cyc(t, 2.1, i * 0.7), ax = [-300, 0, 300][i] + 434;
        x.save(); x.globalAlpha = u < 0.2 ? u / 0.2 : 1 - (u - 0.2) / 0.8; x.translate(ax, 1300 - u * 520); x.scale(0.8, 0.8);
        line(x, 0, 50, 0, -40, 26, INKY); line(x, -38, 0, 0, -42, 26, INKY); line(x, 38, 0, 0, -42, 26, INKY); x.restore(); } } },
    party: { mood: "pleased", label: "Day thirty",
      f: t => { const j = jump(t, 0.92, 200, 0.55), up = j.hop / 200;
        return { ...j, armL: 2.45 + up * 0.45, armR: -2.45 - up * 0.45, wristL: S(t * 15) * 0.35, wristR: -S(t * 15) * 0.35,
                 padL: true, padR: true, tail: TAIL(6, 0.5), tilt: S(t * 4) * 0.05 }; },
      ground(x, t, p) { puff(x, 434, GROUND, p.land); },
      front(x, t, p) { if (p.hop > 140) { const k = (p.hop - 140) / 60;
        sparkle(x, 434, -80, 70 * k, k, BUTTER, t * 2); sparkle(x, 90, 120, 40 * k, k, CREAMY, 0); sparkle(x, 790, 110, 46 * k, k, LIMEC, 0); } },
      canvas(x, t, st) { confetti(x, t, st.seed); } },
    /* A waddle, facing you: weight rocks from paw to paw, each front paw
       lifts in turn, and he leans a little into the way he is going. The
       caller moves him across the screen; this is only the gait. */
    walk: { mood: "pleased", label: "Walking in",
      f: (t, st) => { const s = S(t * 7.2), dir = st.dir || -1;
        return { liftL: Math.max(0, s) * 50, liftR: Math.max(0, -s) * 50, sway: s * 0.06,
                 hop: Math.abs(s) * 16, tilt: -s * 0.05 + dir * 0.05, dy: st.walkDy || 0,
                 tail: TAIL(3.6, 0.45) }; } },
    /* Once, from behind whatever he is anchored to: up, a look around, and
       back down. With `cheer` he comes up grinning and hops with hearts. */
    pop: { mood: "resting", label: "Popping up", once: st => st.cheer ? 3.4 : 3.0, ledge: LEDGE,
      f: (t, st) => { const u = clamp(t / (st.cheer ? 3.4 : 3.0), 0, 1);
        const up = u < 0.16 ? pop(u / 0.16) : u < 0.78 ? 1 : 1 - smooth((u - 0.78) / 0.17);
        const look = st.cheer ? S(t * 3) * 0.05 : (u > 0.2 && u < 0.74 ? S((u - 0.2) / 0.54 * TAU) * 0.15 : 0);
        const h = st.cheer ? hopOnce((u - 0.2) / 0.42, 110) : { hop: 0, sq: 0 };
        return { peek: true, dy: LEDGE - OY + 50 - 670 * up, pawUp: smooth(u / 0.1) * (1 - smooth((u - 0.86) / 0.1)),
                 tilt: look, wig: S(t * 8) * 6, hop: h.hop, sq: h.sq }; },
      front(x, t, p, paw, st) {
        if (st.say) { const u = t / (st.once || 3.4);
          bubble(x, 660, -170, u > 0.2 && u < 0.76 ? pop((u - 0.2) / 0.1) * (1 - smooth((u - 0.68) / 0.08)) : 0, st.say); }
        if (!st.cheer) return;
        for (let i = 0; i < 5; i++) {
          const k = clamp((t / 3.4 - 0.26 - i * 0.035) / 0.45, 0, 1), side = i - 2;
          heart(x, 434 + side * 80 + side * 170 * k, 140 - 420 * k + Math.abs(side) * 60 * k,
                pop(k * 4) * (1 - k ** 4) * (1.25 - Math.abs(side) * 0.12), 1 - k ** 3, i % 2 ? PINK : RED);
        }
      } },
    sleep: { mood: "resting", label: "Nothing to keep yet",
      f: t => { const u = cyc(t, 5.2), nod = u < 0.86 ? smooth(u / 0.86) : 1 - smooth((u - 0.86) / 0.05);
        return { tilt: 0.04 + 0.2 * nod, bob: 8 + nod * 26, breathe: S(t * 1.5) * 0.035,
                 ...(u > 0.86 && u < 0.96 ? { hop: S((u - 0.86) / 0.1 * PI) * 26, sq: 0.3 * S((u - 0.86) / 0.1 * PI) } : {}),
                 tail: TAIL(0.9, 0.12, { a0: PI - 0.05, curl: 0.035, bx: 700, by: 1262, front: true }) }; },
      front(x, t) { zzz(x, t); } }
  };

  function defaults(t) {
    return { hop: 0, sq: 0, land: -1, tilt: 0, bob: 0, armL: null, armR: null, wristL: 0, wristR: 0,
             padL: false, padR: false, tail: TAIL(2, 0.25), dy: 0, peek: false, prop: null, dangle: false,
             breathe: S(t * 1.8) * 0.014, flash: -1, cardY: 0, wig: 0, liftL: 0, liftR: 0, sway: 0, pawUp: 1 };
  }

  /* ── one frame ─────────────────────────────────────────────────────── */
  function draw(x, st, t, h) {
    const pose = POSES[st.pose];
    let p = Object.assign(defaults(t), pose.f(t, st));
    if (st.prev) {
      const e = smooth((st.now - st.prev.at) / BLEND);
      if (e >= 1) st.prev = null;
      else p = blend(Object.assign(defaults(t), POSES[st.prev.pose].f(t, st)), p, e);
    }
    const kick = st.now - st.kickAt;
    const k = hopOnce(kick / 1.1, 190);
    p.hop += k.hop; p.sq += k.sq;
    const mood = st.mood || pose.mood;
    const sx = 1 - 0.07 * p.sq - p.breathe * 0.5, sy = 1 + 0.09 * p.sq + p.breathe;
    const toHop = ([a, b]) => [434 + (a - 434) * sx, GROUND + (b - GROUND) * sy];

    if (pose.ledge) { x.save(); x.beginPath(); x.rect(0, 0, CW, pose.ledge); x.clip(); }
    x.save(); x.translate(OX, OY + p.dy);
    if (pose.ground && !p.peek) pose.ground(x, t, p);
    x.save(); x.translate(0, -p.hop);
    if (p.sway) { x.translate(434, GROUND); x.rotate(p.sway); x.translate(-434, -GROUND); }
    x.save(); x.translate(434, GROUND); x.scale(sx, sy); x.translate(-434, -GROUND);

    let pawL = [330, 900], pawR = [538, 900];
    if (!p.peek) {
      if (!p.tail.front) drawTail(x, p.tail, t);
      drawTorso(x);
      if (p.dangle) drawDangle(x, t);
      drawHaunch(x, 0, p.dangle); drawHaunch(x, 1, p.dangle);
      if (p.armL === null) drawLeg(x, 348, p.liftL);
      if (p.armR === null) drawLeg(x, 520, p.liftR);
      if (p.tail.front) drawTail(x, p.tail, t);
      drawBib(x);
    }

    x.save();
    x.translate(434, 905 + p.bob - p.sq * 14); x.rotate(p.tilt); x.translate(-434, -905);
    x.drawImage(Cat.frame(913.06 * h / CH, mood), 0, 0, 883.12, 913.06);
    x.restore();

    if (!p.peek) {
      if (p.prop === "card") {
        x.save(); x.translate(434, 1070 + p.cardY); x.scale(1.12, 1.12); drawMiniCard(x, t); x.restore();
        const reach = (sx, tx, ty) => { const dx = tx - sx, dy = ty - 968; return [Math.atan2(-dx, dy), Math.hypot(dx, dy) - 50]; };
        const [aL, lL] = reach(322, 250, 1080 + p.cardY), [aR, lR] = reach(546, 618, 1080 + p.cardY);
        drawArm(x, 322, 968, aL, 0.5, false, lL); drawArm(x, 546, 968, aR, -0.5, false, lR);
      }
      if (p.armL !== null) pawL = drawArm(x, 322, 968, p.armL, p.wristL, p.padL);
      if (p.armR !== null) {
        if (p.prop === "key" || p.prop === "phone") {
          const g = armSpine(546, 968, p.armR, p.wristR);
          x.save(); x.translate(...g.paw); x.rotate(p.prop === "key" ? 0.22 + p.wristR * 0.5 : g.head + PI);
          if (p.prop === "key") drawKey(x); else drawPhone(x);
          x.restore();
        }
        pawR = drawArm(x, 546, 968, p.armR, p.wristR, p.padR);
      }
    }
    x.restore();
    if (p.prop === "cloud") drawCloud(x, t);
    if (pose.front) pose.front(x, t, p, { L: toHop(pawL), R: toHop(pawR) }, st);
    x.restore();
    x.restore();

    if (pose.ledge) x.restore();
    if (p.peek && p.pawUp > 0.01 && st.paws !== false) {
      // Over a ledge the paws hook its edge, toes down the far side, and
      // slide back behind it when he goes.
      const at = pose.ledge ? pose.ledge + 34 : CH - 36, drop = (1 - p.pawUp) * 140;
      if (pose.ledge) { x.save(); x.beginPath(); x.rect(0, 0, CW, pose.ledge + 60); x.clip(); }
      footFront(x, OX + 300, at + drop, p.wig * 0.012, 1.05);
      footFront(x, OX + 568, at + drop, -p.wig * 0.012, 1.05);
      if (pose.ledge) x.restore();
    }
    if (pose.canvas) pose.canvas(x, t, st);
  }

  function paint(st) {
    const c = st.canvas, dpr = window.devicePixelRatio || 1, h = c.clientHeight;
    if (!h) return;
    const w = h * CW / CH;
    c.style.width = w + "px";
    if (c.width !== Math.round(w * dpr) || c.height !== Math.round(h * dpr)) {
      c.width = Math.round(w * dpr); c.height = Math.round(h * dpr);
    }
    const x = c.getContext("2d");
    x.setTransform(dpr * h / CH, 0, 0, dpr * h / CH, 0, 0);
    x.clearRect(0, 0, CW, CH);
    draw(x, st, still ? 0.9 : st.now - st.t0, h * dpr);
  }

  let running = false;
  function loop(ms) {
    const now = ms / 1000;
    for (let i = live.length - 1; i >= 0; i--) {
      const st = live[i];
      if (!st.canvas.isConnected) { live.splice(i, 1); continue; }
      st.now = now; paint(st);
      if (st.once && now - st.t0 > st.once) { live.splice(i, 1); if (st.onDone) st.onDone(); }
    }
    if (live.length && !still) requestAnimationFrame(loop); else running = false;
  }

  function mount(canvas, opts) {
    const now = performance.now() / 1000;
    const st = Object.assign({ canvas, pose: opts.pose, mood: opts.mood, t0: now - Math.random() * 2,
                 kickAt: -10, seed: Math.random() * 100, now }, opts.extra || {});
    const once = POSES[opts.pose].once;
    if (once) { st.t0 = now; st.once = once(st); st.onDone = opts.onDone; }
    st.kick = () => { st.kickAt = performance.now() / 1000; if (still) paint(st); };
    /* Moves to another pose over BLEND seconds rather than cutting to it. */
    st.setPose = (pose, mood) => {
      if (pose === st.pose) return;
      st.prev = { pose: st.pose, at: performance.now() / 1000 };
      st.pose = pose; st.mood = mood;
      if (still) paint(st);
    };
    live.push(st);
    paint(st);
    if (!running && !still) { running = true; requestAnimationFrame(loop); }
    return st;
  }

  /* Blends two frames' parameters. An arm that is down in one of them is
     treated as hanging straight, so a paw can rise out of a sitting pose. */
  const BLEND = 0.45;
  const NUM = ["hop", "sq", "tilt", "bob", "dy", "breathe", "cardY", "liftL", "liftR", "sway", "wristL", "wristR", "pawUp", "wig"];
  function blend(a, b, e) {
    const o = Object.assign({}, b), mix = (u, v) => u + (v - u) * e;
    for (const k of NUM) o[k] = mix(a[k] || 0, b[k] || 0);
    for (const [k, hang] of [["armL", 0.04], ["armR", -0.04]]) {
      if (a[k] === null && b[k] === null) o[k] = null;
      else o[k] = mix(a[k] ?? hang, b[k] ?? hang);
    }
    o.padL = e < 0.5 ? a.padL : b.padL; o.padR = e < 0.5 ? a.padR : b.padR;
    // Each tail keeps its own rhythm; the drawn shape moves from one to the other.
    o.tail = Object.assign({}, b.tail, { from: a.tail, e });
    return o;
  }

  /* Where the ground line sits, as a fraction of the canvas height, so a
     caller can stand him on the edge of something. */
  const groundAt = pose => pose === "peek" || pose === "pop" ? 1 : (OY + GROUND + (pose === "perch" ? -300 : 0)) / CH;
  /* Where the edge he pops up from sits, as a fraction of the height. */
  const ledgeAt = () => LEDGE / CH;

  return { mount, POSES, groundAt, ledgeAt, ratio: CW / CH, hopOnce };
})();
