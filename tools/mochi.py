"""Turns the four Mochi drawings into vector art both platforms can draw.

`art/mochi/*.svg` is the source. This emits, from the same parse so the two
cannot drift:

    app/src/main/res/drawable/mochi_*.xml   Android VectorDrawable
    ios/Shared/MochiArt.swift               the same paths, for CoreGraphics

    python3 tools/mochi.py            # write both
    python3 tools/mochi.py --check    # re-emit and compare, for CI

The four files are one 2x2 sheet. They carry identical geometry and differ
only in their viewBox, which windows onto a quadrant — so each mood's origin
comes from its own file, and paths that fall outside that window are dropped
rather than shipped four times over. NUDGE is the last bit of registration on
top of that: the art sits a little differently inside each quadrant, and
lining it up is what puts the head on the same pixels in all four. That is
the one rule Mochi has always been drawn to.

The exports only ever use absolute M/L/C/z and translate(0,0), which is why
the path data survives being carried across once it is shifted. Anything else
in a future export will raise here rather than being silently dropped.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART = os.path.join(ROOT, 'art', 'mochi')

# What each quadrant is cropped to, inside its own viewBox, and how far the
# art sits from that viewBox's corner. Measured off a render of each file.
VIEW_W, VIEW_H = 883.12, 913.06
NUDGE = {
    'awake':    (27.60, 33.14),
    'pleased':  (47.84, 33.14),
    'resting':  (29.44, 38.66),
    'let_down': (47.84, 38.66),
}
MOODS = ('awake', 'pleased', 'resting', 'let_down')

NUM = re.compile(r'-?\d*\.?\d+(?:[eE][-+]?\d+)?')


def rgb_of(text):
    r, g, b = (int(v) for v in NUM.findall(text)[:3])
    return r, g, b


def argb(text, alpha=1.0):
    """'rgb(1,2,3)' plus an alpha, as #AARRGGBB."""
    r, g, b = rgb_of(text)
    return '#%02X%02X%02X%02X' % (round(max(0.0, min(1.0, alpha)) * 255), r, g, b)


def fmt(v):
    return ('%.3f' % v).rstrip('0').rstrip('.') or '0'


def shift(d, ox, oy):
    """Moves absolute path data by (-ox, -oy). M, L and C take x,y pairs."""
    out, i, n = [], 0, len(d)
    while i < n:
        c = d[i]
        if c in 'MLC':
            coords = {'M': 2, 'L': 2, 'C': 6}[c]
            vals = []
            i += 1
            for k in range(coords):
                m = NUM.search(d, i)
                if m is None:
                    raise ValueError('%s wants %d numbers' % (c, coords))
                vals.append(float(m.group(0)) - (ox if k % 2 == 0 else oy))
                i = m.end()
            out.append(c + ' '.join(fmt(v) for v in vals))
        elif c in 'zZ':
            out.append('Z')
            i += 1
        elif c in ' ,\t\r\n':
            i += 1
        else:
            raise ValueError('unsupported path command %r — this converter only '
                             'handles absolute M/L/C/z' % c)
    return ''.join(out)


def bbox(d):
    """A conservative box for absolute M/L/C data — control points included."""
    xs = [float(v) for v in NUM.findall(d)]
    if not xs:
        return None
    return min(xs[0::2]), min(xs[1::2]), max(xs[0::2]), max(xs[1::2])


def parse(mood):
    """One mood as (paths, gradients), windowed onto its quadrant and registered."""
    src = open(os.path.join(ART, mood + '.svg'), encoding='utf-8-sig').read()
    vb = [float(v) for v in NUM.findall(
        re.search(r'viewBox="([^"]+)"', src).group(1))]
    nx, ny = NUDGE[mood]
    ox, oy = vb[0] + nx, vb[1] + ny

    for t in set(re.findall(r'transform="([^"]*)"', src)):
        if re.sub(r'[\s]', '', t) != 'translate(0,0)':
            raise ValueError('unsupported transform %r — bake it into the path '
                             'data before exporting' % t)

    grads = {}
    for m in re.finditer(r'<linearGradient\b([^>]*)>(.*?)</linearGradient>', src, re.S):
        head, body = m.group(1), m.group(2)
        gid = re.search(r'id="([^"]+)"', head).group(1)
        x1, y1, x2, y2 = (float(re.search(r'\b%s="([^"]+)"' % k, head).group(1))
                          for k in ('x1', 'y1', 'x2', 'y2'))
        stops = []
        for st in re.finditer(r'<stop\b([^>]*)/>', body):
            a = st.group(1)
            op = re.search(r'stop-opacity="([^"]+)"', a)
            stops.append((float(re.search(r'offset="([^"]+)"', a).group(1)),
                          re.search(r'stop-color="([^"]+)"', a).group(1),
                          float(op.group(1)) if op else 1.0))
        grads[gid] = (x1 - ox, y1 - oy, x2 - ox, y2 - oy, sorted(stops))

    paths = []
    for m in re.finditer(r'<path\b([^>]*?)/?>', src):
        a = m.group(1)
        dm = re.search(r'\bd="([^"]*)"', a)
        fm = re.search(r'\bfill="([^"]*)"', a)
        if not dm or not fm or fm.group(1) == 'none':
            continue
        om = re.search(r'\bopacity="([^"]*)"', a)
        # The sheet holds all four cats; keep only what this window shows.
        box = bbox(dm.group(1))
        if box and (box[2] < ox or box[0] > ox + VIEW_W
                    or box[3] < oy or box[1] > oy + VIEW_H):
            continue
        paths.append((shift(dm.group(1), ox, oy), fm.group(1),
                      float(om.group(1)) if om else 1.0))
    return paths, grads


def vector_drawable(mood):
    paths, grads = parse(mood)
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           '<!-- Generated by tools/mochi.py from art/mochi/%s.svg. Do not edit. -->' % mood,
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    xmlns:aapt="http://schemas.android.com/aapt"',
           '    android:width="120dp"',
           '    android:height="124dp"',
           '    android:viewportWidth="%s"' % fmt(VIEW_W),
           '    android:viewportHeight="%s">' % fmt(VIEW_H)]
    for d, fill, alpha in paths:
        gid = re.match(r'url\(#(\w+)\)', fill)
        if gid is None:
            line = '    <path android:fillColor="%s" android:pathData="%s"' % (
                argb(fill), d)
            if alpha < 1.0:
                line += '\n        android:fillAlpha="%s"' % fmt(alpha)
            out.append(line + '/>')
            continue
        x1, y1, x2, y2, stops = grads[gid.group(1)]
        out.append('    <path android:pathData="%s">' % d)
        if alpha < 1.0:
            out[-1] = '    <path android:fillAlpha="%s" android:pathData="%s">' % (fmt(alpha), d)
        out += ['        <aapt:attr name="android:fillColor">',
                '            <gradient android:type="linear"',
                '                android:startX="%s" android:startY="%s"' % (fmt(x1), fmt(y1)),
                '                android:endX="%s" android:endY="%s">' % (fmt(x2), fmt(y2))]
        for off, col, op in stops:
            out.append('                <item android:offset="%s" android:color="%s"/>'
                       % (fmt(off), argb(col, op)))
        out += ['            </gradient>', '        </aapt:attr>', '    </path>']
    out.append('</vector>')
    return '\n'.join(out) + '\n'


def swift():
    """One string literal per mood, parsed on the device.

    A Swift array literal of a few hundred tuples is something the type
    checker takes minutes over; one string it does not look at twice.
    """
    blocks = []
    for mood in MOODS:
        paths, grads = parse(mood)
        rows = []
        for d, fill, alpha in paths:
            gid = re.match(r'url\(#(\w+)\)', fill)
            if gid is None:
                rows.append('S\t%s\t%s\t%s' % (argb(fill)[1:], fmt(alpha), d))
            else:
                x1, y1, x2, y2, stops = grads[gid.group(1)]
                first, last = stops[0], stops[-1]
                rows.append('G\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s' % (
                    argb(first[1], first[2])[1:], argb(last[1], last[2])[1:],
                    fmt(x1), fmt(y1), fmt(x2), fmt(y2), fmt(alpha), d))
        blocks.append((mood, '\n'.join(rows)))

    out = ['import CoreGraphics', '',
           '// Generated by tools/mochi.py from art/mochi/*.svg. Do not edit.',
           '//',
           '// One row per path: either',
           '//   S <argb> <alpha> <path data>',
           '// or',
           '//   G <argb0> <argb1> <x1> <y1> <x2> <y2> <alpha> <path data>',
           '// tab separated, one path per line. The path data is absolute M/L/C/Z,',
           '// already registered, in a %s x %s viewport.' % (fmt(VIEW_W), fmt(VIEW_H)),
           'enum MochiArt {', '',
           '    static let viewWidth: CGFloat = %s' % fmt(VIEW_W),
           '    static let viewHeight: CGFloat = %s' % fmt(VIEW_H), '']
    for mood, body in blocks:
        name = ''.join(p.capitalize() if i else p
                       for i, p in enumerate(mood.split('_')))
        out.append('    static let %s = """\n%s\n"""\n' % (name, body))
    out.append('}')
    return '\n'.join(out) + '\n'


def targets():
    t = {os.path.join(ROOT, 'app/src/main/res/drawable/mochi_%s.xml' % m): vector_drawable(m)
         for m in MOODS}
    t[os.path.join(ROOT, 'ios/Shared/MochiArt.swift')] = swift()
    return t


if __name__ == '__main__':
    check = '--check' in sys.argv
    bad = []
    for path, body in targets().items():
        rel = os.path.relpath(path, ROOT)
        # A branch may carry only one of the two platforms; write what is here
        # rather than conjuring the other one's tree.
        if not os.path.isdir(os.path.dirname(path)):
            print('%s  skipped, no such directory' % rel)
            continue
        if check:
            have = open(path).read() if os.path.exists(path) else None
            if have != body:
                bad.append(rel)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        open(path, 'w').write(body)
        print('%s  %d bytes' % (rel, len(body)))
    if check:
        if bad:
            print('stale, re-run tools/mochi.py: ' + ', '.join(bad))
            sys.exit(1)
        print('generated art is up to date')
