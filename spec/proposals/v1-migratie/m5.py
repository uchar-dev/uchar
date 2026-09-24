#!/usr/bin/env python3
"""L5 of the v1 wire migration: uchar-ir-v1 from its L4 shape to its L5 shape, by stage.

    m5.py --stage m7 FILE...             migrate the named ir.json documents in place
    m5.py --stage m7 --all               migrate every corpus/**/ir.json and toka/**/ir.json
    m5.py --stage m7 --check [FILE...]   list every document that is not yet in the M7 shape
                                         (default: the --all set); exit 1 if there is one

The target shape is the one the L5 card (20260924-l5-bronrecord-en-hernoeming) fixes under
"De L5-vorm van de wire", and spec/schema/uchar-ir-v1.schema.json types it. L5 is two steps, the
source record (M7) and the renaming (M8). The stage is always named: --stage m7 is the step this
script carries.

The source record (M7, delta B26) moves every source text into the record 'source' and changes
nothing else: every other key, value and key order is carried verbatim.

- A 'bron' string becomes "source": {"snippet": <the string>}, byte for byte (whitespace
  included, never trimmed), at the place 'bron' stood. The record carries no node.
- A null 'bron' is left out: a node without source text carries no source.
- On a consistency version (rules.consistenties[i].versions[j]) and on a ResolvedRegelGroep, 'line'
  goes into the record after 'snippet' where a snippet is written, and is dropped where 'bron' was
  null; the count line reports both. A Diagnostic keeps its own 'line'.

Loud by construction: an L4 value the M7 shape cannot carry fails the document with its file name
and the JSON pointer into the L4 input; nothing is repaired. That covers an empty 'bron' (a snippet
is never empty, so the document has no M7 form), a null or absent 'bron' on an Onopgelost marker
(the marker requires a source), a 'bron' that is neither a string nor null, a 'line' beside a
string that is not an integer of at least 1, a 'line' without a 'bron' beside it, and a 'line' on
any other object than the two carriers and a Diagnostic. A failing document is left untouched. A
document already in the M7 shape (no 'bron' anywhere, no 'line' on a carrier) is left untouched as
well, byte for byte, so a second run changes nothing; a document that carries 'bron' beside a
source record, or a carrier's 'line' beside one, fails as mixed. Every migrated document gets one
line on stderr counting what was rewritten.

This script reads the L4 shape, so it names the L4 fields it removes; no L5 artifact carries those
names.

Output is written with gate 2's own serialiser, executed from scripts/check-ir-schema.sh exactly as
m1.py to m4.py do, so a migrated document round-trips byte-identically under gate 2.
"""
import collections
import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

_gate2 = open(os.path.join(ROOT, 'scripts', 'check-ir-schema.sh'), encoding='utf-8').read()
_ns = {'json': json}
exec(_gate2[_gate2.index('class RawNum'):_gate2.index('def roundtrip')], _ns)
RawNum, ser = _ns['RawNum'], _ns['ser']

FORMAT = 'uchar-ir-v1'
STAGES = ('m7',)

# the two carriers of a v0 'line' beside 'bron': a consistency version, found by its position
# (a version record carries no node), and the rule-group node
CONSISTENCY_VERSION = re.compile(r'/rules/consistenties/[0-9]+/versions/[0-9]+')
RULE_GROUP = 'ResolvedRegelGroep'
MARKER = 'Onopgelost'
INTEGER = re.compile(r'-?[0-9]+')

COUNT_NAMES = ('snippets', 'null weg', 'line in source (consistentie)',
               'line in source (regelgroep)', 'line vervallen (consistentie)',
               'line vervallen (regelgroep)')


class Refusal(Exception):
    pass


def ptr(*parts):
    return ''.join('/' + str(p).replace('~', '~0').replace('/', '~1') for p in parts)


def _pairs(pairs):
    # a duplicate key would vanish silently in a dict, taking a field with it
    keys = [k for k, _ in pairs]
    dup = sorted({k for k in keys if keys.count(k) > 1})
    if dup:
        raise Refusal('duplicate object key(s) %s' % ', '.join(json.dumps(k) for k in dup))
    return dict(pairs)


def parse(raw):
    return json.loads(raw.decode('utf-8'), parse_int=RawNum, parse_float=RawNum,
                      object_pairs_hook=_pairs)


def plain(v):
    if isinstance(v, RawNum):
        return ('num', v.raw)
    if isinstance(v, dict):
        return {k: plain(x) for k, x in v.items()}
    if isinstance(v, list):
        return [plain(x) for x in v]
    return v


def is_carrier(v, p):
    return v.get('node') == RULE_GROUP or CONSISTENCY_VERSION.fullmatch(p) is not None


def is_diagnostic(v, p):
    return v.get('node') == 'Diagnostic' and re.fullmatch(r'/diagnostics/[0-9]+', p) is not None


def objects(v, p=''):
    """Every object of [v] with its JSON pointer, parents before children."""
    if isinstance(v, list):
        for i, x in enumerate(v):
            yield from objects(x, p + ptr(i))
    elif isinstance(v, dict):
        yield v, p
        for k, x in v.items():
            yield from objects(x, p + ptr(k))


def shape(doc):
    """(bron pointers, source pointers, carrier-line pointers) of a document."""
    bron, source, line = [], [], []
    for v, p in objects(doc):
        if 'bron' in v:
            bron.append(p + ptr('bron'))
        if 'source' in v:
            source.append(p + ptr('source'))
        if 'line' in v and is_carrier(v, p):
            line.append(p + ptr('line'))
    return bron, source, line


class Migration:
    def __init__(self):
        self.errors = []
        self.counts = collections.Counter()

    def fail(self, path, msg):
        self.errors.append((path, msg))

    def walk(self, v, p):
        if isinstance(v, list):
            for i, x in enumerate(v):
                v[i] = self.walk(x, p + ptr(i))
            return v
        if not isinstance(v, dict):
            return v
        out = self.node(v, p)
        for k in list(out.keys()):
            if k != 'source':
                out[k] = self.walk(out[k], p + ptr(k))
        return out

    def node(self, v, p):
        """[v] with its source text in the record, keys in their own order."""
        carrier = is_carrier(v, p)
        if 'line' in v and not carrier and not is_diagnostic(v, p):
            self.fail(p + ptr('line'), 'a line on an object that is neither a consistency version '
                      'nor a %s: the source record takes the line of those two alone' % RULE_GROUP)
            return v
        if 'bron' not in v:
            if v.get('node') == MARKER:
                self.fail(p, 'an %s marker without bron: the M7 shape requires a source on the '
                          'marker' % MARKER)
            elif carrier and 'line' in v:
                self.fail(p + ptr('line'), 'a line without a bron beside it: the source record '
                          'writes a line only after its snippet')
            return v
        b = v['bron']
        if b is None:
            if v.get('node') == MARKER:
                self.fail(p + ptr('bron'), 'an %s marker whose bron is null: the M7 shape requires '
                          'a source on the marker, and a null has no snippet to write' % MARKER)
                return v
        elif not isinstance(b, str):
            self.fail(p + ptr('bron'), 'a bron that is neither a string nor null')
            return v
        elif b == '':
            self.fail(p + ptr('bron'), 'an empty bron: a source snippet is never empty, and a '
                      'node without source text carries no source')
            return v
        record = None
        if isinstance(b, str):
            record = {'snippet': b}
            self.counts['snippets'] += 1
        else:
            self.counts['null weg'] += 1
        what = 'regelgroep' if v.get('node') == RULE_GROUP else 'consistentie'
        if carrier and 'line' in v:
            line = v['line']
            if record is None:
                self.counts['line vervallen (%s)' % what] += 1
            elif not (isinstance(line, RawNum) and INTEGER.fullmatch(line.raw)):
                self.fail(p + ptr('line'), 'a line that is not an integer')
                return v
            elif int(line.raw) < 1:
                self.fail(p + ptr('line'), 'the line %s: a source line is at least 1' % line.raw)
                return v
            else:
                record['line'] = line
                self.counts['line in source (%s)' % what] += 1
        out = {}
        for k, x in v.items():
            if k == 'bron':
                if record is not None:
                    out['source'] = record
            elif k == 'line' and carrier:
                continue
            else:
                out[k] = x
        return out


def migrate(raw):
    """(new bytes | None when already in the M7 shape, [(json pointer, message)], migration)."""
    try:
        doc = parse(raw)
    except (UnicodeDecodeError, ValueError, Refusal) as exc:
        return None, [('', 'cannot parse: %s' % exc)], None
    if not isinstance(doc, dict):
        return None, [('', 'the document is not a JSON object')], None
    if doc.get('format') != FORMAT:
        return None, [(ptr('format'), 'format %s is not %s (m1.py migrates the v0 shape first)'
                       % (json.dumps(doc.get('format')), json.dumps(FORMAT)))], None
    bron, source, line = shape(doc)
    if not bron and not line:
        return None, [], None
    if source:
        return None, [(q, 'a source record in a document that still carries %s: the two shapes '
                       'are mixed' % ('bron' if bron else 'a line on a consistency version or a '
                                      'rule group')) for q in source], None
    m = Migration()
    doc = m.walk(doc, '')
    if m.errors:
        return None, m.errors, None
    bron, _, line = shape(doc)
    if bron or line:
        return None, [(q, 'internal: this survived the walk') for q in bron + line], None
    out = (ser(doc, 0) + '\n').encode('utf-8')
    # the written bytes must read back as exactly the document built, in gate 2's canonical form
    back = parse(out)
    if plain(back) != plain(doc) or (ser(back, 0) + '\n').encode('utf-8') != out:
        return None, [('', 'internal: the serialised document does not read back as built')], None
    return out, [], m


def migrate_bytes(raw):
    """(new bytes | None when already in the M7 shape, [(json pointer, message)]), as m4.py."""
    out, errors, _ = migrate(raw)
    return out, errors


def default_files():
    files = sorted(glob.glob(os.path.join(ROOT, 'corpus', '**', 'ir.json'), recursive=True))
    files += sorted(glob.glob(os.path.join(ROOT, 'toka', '**', 'ir.json'), recursive=True))
    return files


def show(path):
    rel = os.path.relpath(path, ROOT)
    return path if rel.startswith('..') else rel


def count_line(m):
    return ', '.join('%s %d' % (k, m.counts[k]) for k in COUNT_NAMES)


def check(files):
    left = 0
    for f in files:
        try:
            raw = open(f, 'rb').read()
        except OSError as exc:
            sys.stderr.write('FAIL: cannot read %s: %s\n' % (show(f), exc))
            return 2
        out, errors, _ = migrate(raw)
        if errors:
            left += 1
            path, msg = errors[0]
            print('L4 (refused): %s at %s: %s' % (show(f), path or '<root>', msg))
        elif out is not None:
            left += 1
            print('L4: %s' % show(f))
    print('m5 --stage m7 --check: %d of %d document(s) not in the M7 shape' % (left, len(files)))
    return 1 if left else 0


def main(argv):
    if not argv or argv[0] in ('-h', '--help'):
        print(__doc__.split('\n\n')[1])
        return 0 if argv else 2
    if argv[0] != '--stage' or len(argv) < 2 or argv[1] not in STAGES:
        sys.stderr.write('FAIL: name the stage first: --stage %s (the stage this script carries)\n'
                         % '|'.join(STAGES))
        return 2
    argv = argv[2:]
    if argv and argv[0] == '--check':
        files = argv[1:] or default_files()
        if not files:
            sys.stderr.write('FAIL: no ir.json documents to check\n')
            return 2
        return check(files)
    if argv and argv[0] == '--all':
        if len(argv) > 1:
            sys.stderr.write('FAIL: --all takes no file arguments\n')
            return 2
        files = default_files()
    else:
        files = argv
    if not files:
        sys.stderr.write('FAIL: no ir.json documents found\n')
        return 2
    migrated = kept = 0
    failed = []
    for f in files:
        try:
            raw = open(f, 'rb').read()
        except OSError as exc:
            failed.append((f, [('', 'cannot read: %s' % exc)]))
            continue
        out, errors, m = migrate(raw)
        if errors:
            failed.append((f, errors))
            continue
        if out is None:
            kept += 1
            continue
        tmp = f + '.m5tmp'
        with open(tmp, 'wb') as fh:
            fh.write(out)
        os.replace(tmp, f)
        migrated += 1
        sys.stderr.write('m5 m7: %s: %s\n' % (show(f), count_line(m)))
    for f, errors in failed:
        sys.stderr.write('FAIL: %s\n' % show(f))
        for path, msg in errors:
            sys.stderr.write('  at %s: %s\n' % (path or '<root>', msg))
    print('m5 --stage m7: %d migrated, %d already in the M7 shape and left untouched, %d refused '
          '(left in the L4 shape)' % (migrated, kept, len(failed)))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
