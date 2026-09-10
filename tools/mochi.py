"""Mochi, generated.

The silhouette is computed - an ellipse biased low plus a gaussian bulge at
cheek level, so the face is widest where the muzzle is and the line curves
*under* it into the chin. A rounded rectangle chamfers there; this does not,
and that is the whole difference between a puffy cheek and a hard jaw. Coat
shading, the ears, the blush and the whiskers fall out of the same geometry.

The face is placed by hand. Procedural shading can carry a coat; it cannot
draw an eye worth looking at.

    python3 tools/mochi.py --swift    # the grid section of ios/Shared/Cat.swift
    python3 tools/mochi.py --png OUT  # a sheet of all four moods, to look at

Paste the --swift output between `static let cols` and `colorOf` in
ios/Shared/Cat.swift. Nothing reads this at build time: it is the source the
bitmap was drawn from, kept so the next change is an edit and not a re-pixel.
"""
import math, struct, sys, zlib

W, H = 40, 36
CX = 19.5

SKULL_Y, SKULL_R, HEAD_RX = 17.5, 11.6, 11.4
CHEEK_Y, CHEEK_BULGE, CHEEK_SPREAD = 21.0, 2.8, 4.2
NECK_HW, NECK_T, NECK_B = 7.5, 27, 35
EAR_CENTRES = (11, 28)
EAR_T, EAR_B = 4, 11


def head_half(y):
    t = (y - SKULL_Y) / SKULL_R
    if abs(t) > 1:
        return 0.0
    hw = HEAD_RX * (1 - abs(t) ** 2.5) ** (1 / 2.5)
    return hw + CHEEK_BULGE * math.exp(-((y - CHEEK_Y) / CHEEK_SPREAD) ** 2)


def ear_half(y):
    if not (EAR_T <= y <= EAR_B):
        return None
    return 0.45 + (y - EAR_T) * 0.62


def filled(x, y):
    if not (0 <= x < W and 0 <= y < H):
        return False
    if abs(x - CX) <= head_half(y):
        return True
    if NECK_T <= y <= NECK_B and abs(x - CX) <= NECK_HW:
        return True
    h = ear_half(y)
    return h is not None and any(abs(x - c) <= h for c in EAR_CENTRES)


def build():
    g = [[' '] * W for _ in range(H)]
    for y in range(H):
        for x in range(W):
            if filled(x, y):
                g[y][x] = 'G'

    # Coat shading: light from the upper left, a shaded rim opposite it.
    for y in range(H):
        for x in range(W):
            if g[y][x] != 'G':
                continue
            d = 9.0
            for dy in range(-4, 5):
                for dx in range(-4, 5):
                    if not filled(x + dx, y + dy):
                        d = min(d, math.hypot(dx, dy))
            nx, ny = (x - CX) / 17.0, (y - 17.0) / 15.0
            light = -nx * 0.64 - ny * 0.8
            if d <= 1.7 and light < -0.05:
                g[y][x] = 'S'
            elif d <= 2.2 and light > 0.30:
                g[y][x] = 'L'

    # Ears: a pink inner that tapers with the point, held off the outer edge by
    # a pixel of coat so the ear keeps a rim all the way up.
    for c in EAR_CENTRES:
        for y in range(EAR_T, EAR_B + 1):
            h = ear_half(y)
            if h is None:
                continue
            inner = h - 1.35
            if inner < 0.2:
                continue
            for x in range(int(math.ceil(c - inner)), int(c + inner) + 1):
                if g[y][x] in ('G', 'L', 'S'):
                    g[y][x] = 'P' if y > EAR_T + 1 else 'H'
        # A tuft of pale fur at the mouth of the ear.
        for y in (EAR_B - 1, EAR_B):
            for x in range(c - 5, c + 6):
                if g[y][x] in ('P', 'H'):
                    g[y][x] = 'L'

    def paint(y, xs, ch, over=('G', 'L', 'S', 'M', 'H', 'P')):
        for x in xs:
            if 0 <= x < W and g[y][x] in over:
                g[y][x] = ch

    # Muzzle: a cream pad under the nose, wider than tall.
    for y in range(H):
        for x in range(W):
            if g[y][x] not in ('G', 'L', 'S'):
                continue
            dx, dy = (x - CX) / 4.6, (y - 23.0) / 2.6
            if dx * dx + dy * dy <= 1.0:
                g[y][x] = 'M'

    # Cheeks: blush out where the face is widest, falling off into the coat.
    for cx in (9.8, 29.2):
        for y in range(16, 23):
            for x in range(W):
                if g[y][x] not in ('G', 'L', 'S'):
                    continue
                r = ((x - cx) / 2.4) ** 2 + ((y - 19.0) / 1.7) ** 2
                if r <= 1.0:
                    g[y][x] = 'H'
                elif r <= 1.75:
                    g[y][x] = 'R'

    # Nose, and the philtrum under it.
    paint(20, range(18, 22), 'K')
    paint(21, range(18, 22), 'P')
    paint(22, range(19, 21), 'P')
    for x in (17, 22):
        if g[21][x] == 'M':
            g[21][x] = 'H'

    # Whiskers fan out past the cheek, so they never break the outline they
      # cross. Mid grey, which reads on cream and on ink alike.
    WHISKERS = (
        ((5.5, 21.0), (1.5, 19.4)),
        ((5.5, 22.4), (1.2, 22.6)),
        ((6.5, 23.8), (2.2, 25.6)),
    )
    for (x0, y0), (x1, y1) in WHISKERS:
        n = int(abs(x0 - x1) * 2)
        for i in range(n + 1):
            t = i / n
            px_, py_ = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            for sx in (px_, 2 * CX - px_):
                x, y = int(round(sx)), int(round(py_))
                if 0 <= x < W and 0 <= y < H and g[y][x] == ' ':
                    g[y][x] = 'S'

    out = [row[:] for row in g]
    for y in range(H):
        for x in range(W):
            if g[y][x] in (' ', 'S') and not (g[y][x] == 'S' and filled(x, y)):
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                ax, ay = x + dx, y + dy
                if not (0 <= ax < W and 0 <= ay < H) or not filled(ax, ay):
                    out[y][x] = 'K'
                    break
    return ["".join(r).replace(' ', '.') for r in out]


base = build()


def put(row, xs, ch):
    r = list(row)
    for x in xs:
        if 0 <= x < W and r[x] != '.':
            r[x] = ch
    return "".join(r)


EL, ER = 14, 25                    # eye centres
EY = 12                            # top row of the open eye


def stamp(rows, sprite, top, cx, flip=False):
    """Paints a sprite (a list of strings, '.' transparent) centred on cx."""
    half = (len(sprite[0]) - 1) // 2
    for j, line in enumerate(sprite):
        cells = line[::-1] if flip else line
        row = list(rows[top + j])
        for i, ch in enumerate(cells):
            x = cx - half + i
            if ch != '.' and 0 <= x < W and row[x] != '.':
                row[x] = ch
        rows[top + j] = "".join(row)


def both(rows, sprite, top):
    # Not mirrored: the coat is lit from the upper left, so both catchlights
    # belong on the same side.
    stamp(rows, sprite, top, EL)
    stamp(rows, sprite, top, ER)


# An open eye: lash line, a dark rim inside it, a lime iris ringing a wide
# pupil, a catchlight top left and a lit arc along the bottom of the iris.
EYE_OPEN = [
    ".KKKKK.",
    "KDEEEDK",
    "KEWBBEK",
    "KEBBBEK",
    "KDAAADK",
    ".KKKKK.",
]
# Half lidded: the same eye with the lid pulled down over the top of it.
EYE_TIRED = [
    ".KKKKK.",
    "KKKKKKK",
    "KEBBBEK",
    "KEBBBEK",
    "KDAAADK",
    ".KKKKK.",
]
EYE_HAPPY = ["..KKK..", ".K...K.", "KK...KK"]
EYE_SHUT = ["KKKKKKK", ".KKKKK."]
# The inner end rides up. That, not the mouth, is what reads as let down.
BROW_SAD = ["...KKK", ".KKK..", "KKK..."]

# A smile is a cup: the corners sit above the middle. Turn it over for a frown.
MOUTH_NEUTRAL = {23: [18, 21], 24: [19, 20]}
MOUTH_SMILE = {23: [17, 18, 21, 22], 24: [19, 20]}
MOUTH_FROWN = {23: [19, 20], 24: [17, 18, 21, 22]}


def mouth(rows, spec):
    for y, xs in spec.items():
        rows[y] = put(rows[y], xs, 'K')


def mood(name):
    r = base[:]
    if name == 'awake':
        both(r, EYE_OPEN, EY)
        mouth(r, MOUTH_NEUTRAL)
    elif name == 'pleased':
        both(r, EYE_HAPPY, EY + 2)
        mouth(r, MOUTH_SMILE)
    elif name == 'resting':
        both(r, EYE_SHUT, EY + 3)
    elif name == 'letDown':
        stamp(r, BROW_SAD, EY - 3, EL)
        stamp(r, BROW_SAD, EY - 3, ER, flip=True)
        both(r, EYE_TIRED, EY)
        mouth(r, MOUTH_FROWN)
    return r


MOODS = {n: mood(n) for n in ('awake', 'pleased', 'resting', 'letDown')}

COLOURS = {
    'K': (0x12, 0x12, 0x0F),
    'L': (0xA8, 0xA8, 0x9E),
    'G': (0x8A, 0x8A, 0x80),
    'S': (0x6E, 0x6E, 0x66),
    'P': (0xE0, 0xA3, 0xA3),
    'H': (0xD6, 0x9B, 0x96),
    'R': (0xAE, 0x96, 0x8C),
    'M': (0xE8, 0xE3, 0xD2),
    'E': (0xC9, 0xF7, 0x3F),
    'A': (0xE6, 0xFC, 0x9C),
    'D': (0x84, 0xA8, 0x28),
    'B': (0x12, 0x12, 0x0F),
    'V': (0xC6, 0xC3, 0xB7),
    'W': (0xF4, 0xF2, 0xEA),
}


def png(path, grids, scale=9, bg=(0xE7, 0xE3, 0xD4), gapx=3):
    gw = len(grids[0][0]) * scale
    tw = len(grids) * gw + (len(grids) - 1) * gapx * scale
    th = len(grids[0]) * scale
    rows = []
    for y in range(th):
        line = bytearray([0])
        for x in range(tw):
            col = bg
            for gi, g in enumerate(grids):
                left = gi * (gw + gapx * scale)
                if left <= x < left + gw:
                    ch = g[y // scale][(x - left) // scale]
                    if ch != '.':
                        col = COLOURS.get(ch, bg)
            line += bytes(col)
        rows.append(bytes(line))
    raw = b"".join(rows)

    def chunk(tag, data):
        c = tag + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c))

    out = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", tw, th, 8, 2, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))
    open(path, "wb").write(out)


CH = {'K': '#', 'L': '+', 'G': '.', 'S': ',', 'P': 'p', 'H': 'h', 'R': 'r',
      'M': 'o', 'E': 'O', 'A': 'a', 'D': 'd', 'B': '@', 'W': '*', '.': ' '}
ORDER = ('awake', 'pleased', 'resting', 'letDown')


def trimmed():
    """The grids with the empty rows above the ears dropped."""
    top = next(i for i, r in enumerate(base) if set(r) != {'.'})
    bot = max(i for i, r in enumerate(base) if set(r) != {'.'})
    return base[top:bot + 1], {k: v[top:bot + 1] for k, v in MOODS.items()}


def swift():
    b, m = trimmed()
    out = ['    static let cols = %d' % len(b[0]),
           '    static let rows = %d' % len(b), '',
           '    private static let base = [']
    out += ['        "%s"%s' % (r, ',' if i < len(b) - 1 else '')
            for i, r in enumerate(b)]
    out += ['    ]', '',
            '    private static func rowsFor(_ mood: Mood) -> [String] {',
            '        var r = base', '        switch mood {']
    for name in ORDER:
        out.append('        case .%s:' % name)
        out += ['            r[%d] = "%s"' % (i, m[name][i])
                for i in range(len(b)) if m[name][i] != b[i]]
    out += ['        }', '        return r', '    }']
    return "\n".join(out)


if __name__ == '__main__':
    if '--swift' in sys.argv:
        print(swift())
    elif '--png' in sys.argv:
        b, m = trimmed()
        png(sys.argv[sys.argv.index('--png') + 1], [m[n] for n in ORDER])
    else:
        _, m = trimmed()
        for name in ORDER:
            print(name)
            for r in m[name]:
                print("  " + "".join(CH.get(c, c) for c in r))
            print()
