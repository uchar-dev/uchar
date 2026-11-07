#!/usr/bin/env python3
"""The symbol of a unit (card unit-symbol, 2026-11-07): a declared unit's symbol moves from its first spelling to
its own field, DeclaredUnit.symbol.

    m8.py FILE...            migrate the named ir.json documents in place
    m8.py --all              migrate every ir.json of the zones below
    m8.py --dry-run [FILE...]  count what a run would rewrite, per zone (default: the --all set), and
                             write nothing; exit 1 if a document would be refused
    m8.py --check [FILE...]  list every document that still carries a symbol among its spellings
                             (default: the --all set); exit 1 if there is one

The zones are corpus/, toka/, ref/test/fixtures/ and backends/drools/src/test/resources/.

Until this card the RegelSpraak frontend wrote the token after a unit's abbreviation as its first spelling, and the
L3 migration made a v0 map key that differs from the abbreviation the first spelling of its unit; in both cases the
token is the euro sign, the unit's symbol (IR-SPEC ch. 2.5). The script moves it, and changes nothing else:

- A DeclaredUnit (an element of a UnitSystem's 'units') whose first spelling is a symbol of SYMBOLS gets that
  symbol as 'symbol', after 'factorToCanonical' and before 'spellings' (the key order of the schema), and loses it
  from 'spellings'; a list left empty is dropped, since absent is the one spelling of no further spelling.
- Every other key, value and key order is carried verbatim.

Loud by construction: a shape this script does not know fails the document with its file name and the JSON pointer
into the input, and a failing document is left untouched. That covers a symbol of SYMBOLS at another place than the
first spelling, a first spelling that is no word and no symbol of SYMBOLS (a symbol this script was not told of), a
unit that already carries a 'symbol' and a symbol among its spellings, a 'spellings' that is no list of strings, and
a UnitSystem whose 'units' is no list of objects. A document without a symbol among its spellings is left untouched
byte for byte, so a second run changes nothing. Every migrated document gets one line on stderr counting the symbols
it moved.

Output is written with gate 2's own serialiser, executed from scripts/check-ir-schema.sh exactly as m1.py to m7.py
do, so a migrated document round-trips byte-identically under gate 2. A document another writer laid out (a negative
that writes the euro sign as itself) keeps its layout: in its bytes the first spelling moves into a 'symbol' line
before 'spellings', with the indentation of that key, and the result must read as exactly the migrated document, key
order included, or the document is refused.
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

ZONES = (('corpus', '**', 'ir.json'), ('toka', '**', 'ir.json'),
         ('ref', 'test', 'fixtures', '**', 'ir.json'),
         ('backends', 'drools', 'src', 'test', 'resources', '**', 'ir.json'))

# the symbols the measurement of 2026-11-07 found as a first spelling: the euro sign, and nothing else
SYMBOLS = ('€',)
COUNT_NAMES = ('symbolen',)


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


class Migration:
    def __init__(self):
        self.errors = []
        self.counts = collections.Counter()

    def fail(self, path, msg):
        self.errors.append((path, msg))

    def walk(self, v, p):
        if isinstance(v, list):
            return [self.walk(x, p + ptr(i)) for i, x in enumerate(v)]
        if not isinstance(v, dict):
            return v
        if v.get('node') == 'UnitSystem':
            units = v.get('units')
            if not isinstance(units, list) or not all(isinstance(u, dict) for u in units):
                self.fail(p + ptr('units'), "a UnitSystem whose 'units' is no list of objects")
                return v
            return {k: ([self.unit(u, p + ptr(k, i)) for i, u in enumerate(x)] if k == 'units'
                        else self.walk(x, p + ptr(k)))
                    for k, x in v.items()}
        return {k: self.walk(x, p + ptr(k)) for k, x in v.items()}

    def unit(self, u, p):
        sp = u.get('spellings')
        if sp is None:
            return u
        if not isinstance(sp, list) or not all(isinstance(s, str) for s in sp):
            self.fail(p + ptr('spellings'), "'spellings' that is no list of strings")
            return u
        for i, s in enumerate(sp):
            if s in SYMBOLS and i > 0:
                self.fail(p + ptr('spellings', i), 'the symbol %s at another place than the first spelling'
                          % json.dumps(s))
                return u
        if not sp or (sp[0] not in SYMBOLS and sp[0].isalpha()):
            return u
        if sp[0] not in SYMBOLS:
            self.fail(p + ptr('spellings', 0), 'the first spelling %s is no word and no symbol this script '
                      'knows' % json.dumps(sp[0]))
            return u
        if 'symbol' in u:
            self.fail(p + ptr('symbol'), 'a unit that carries a symbol and a symbol among its spellings')
            return u
        out = {}
        for k, x in u.items():
            if k == 'spellings':
                out['symbol'] = sp[0]
                if len(sp) > 1:
                    out['spellings'] = sp[1:]
            else:
                out[k] = x
        self.counts['symbolen'] += 1
        return out


def plain(v):
    if isinstance(v, RawNum):
        return ('num', v.raw)
    if isinstance(v, dict):
        return [(k, plain(x)) for k, x in v.items()]
    if isinstance(v, list):
        return [plain(x) for x in v]
    return v


# the first spelling in another writer's layout: '"spellings": [', the whitespace before the symbol, the symbol
# (escaped or as itself), its comma and the whitespace before the next spelling
_FIRST = re.compile(rb'(?P<ind>\n[ \t]*)"spellings": \[(?P<ws>\s*)(?P<sym>"(?:\\u20ac|\xe2\x82\xac)"),\s*')


def migrate(raw):
    """(new bytes | None when no symbol stands among the spellings, [(json pointer, message)], migration)."""
    try:
        doc = parse(raw)
    except (UnicodeDecodeError, ValueError, Refusal) as exc:
        return None, [('', 'cannot parse: %s' % exc)], None
    if not isinstance(doc, dict):
        return None, [('', 'the document is not a JSON object')], None
    if doc.get('format') != FORMAT:
        return None, [(ptr('format'), 'format %s is not %s' % (json.dumps(doc.get('format')),
                                                               json.dumps(FORMAT)))], None
    m = Migration()
    new = m.walk(doc, '')
    if m.errors:
        return None, m.errors, None
    if m.counts['symbolen'] == 0:
        return None, [], m
    if (ser(doc, 0) + '\n').encode('utf-8') != raw:
        moved = _FIRST.sub(lambda g: g.group('ind') + b'"symbol": ' + g.group('sym') + b',' + g.group('ind')
                           + b'"spellings": [' + g.group('ws'), raw)
        if plain(parse(moved)) != plain(new):
            return None, [('', "not in gate 2's serialisation, and a move of the first spelling in its bytes "
                               'does not read as the migrated document')], None
        return moved, [], m
    out = (ser(new, 0) + '\n').encode('utf-8')
    if (ser(parse(out), 0) + '\n').encode('utf-8') != out:
        return None, [('', 'internal: the serialised document does not read back as built')], None
    return out, [], m


def default_files():
    files = []
    for zone in ZONES:
        files += sorted(glob.glob(os.path.join(ROOT, *zone), recursive=True))
    return files


def show(path):
    rel = os.path.relpath(path, ROOT)
    return path if rel.startswith('..') else rel


def zone_of(path):
    parts = os.path.relpath(path, ROOT).split(os.sep)
    for zone in ZONES:
        fixed = [z for z in zone if z != '**' and z != 'ir.json']
        if parts[:len(fixed)] == fixed:
            # corpus is counted per zone of its own: golden, negative, red
            return '/'.join(parts[:2]) if fixed == ['corpus'] else '/'.join(fixed)
    return os.path.dirname(show(path))


def read_all(files):
    out = []
    for f in files:
        try:
            out.append((f, open(f, 'rb').read()))
        except OSError as exc:
            sys.stderr.write('FAIL: cannot read %s: %s\n' % (show(f), exc))
            return None
    return out


def dry_run(files):
    docs = read_all(files)
    if docs is None:
        return 2
    per_zone = collections.OrderedDict()
    refused = 0
    for f, raw in docs:
        z = per_zone.setdefault(zone_of(f), collections.Counter())
        z['documenten'] += 1
        out, errors, m = migrate(raw)
        if errors:
            refused += 1
            z['geweigerd'] += 1
            for path, msg in errors:
                print('refused: %s at %s: %s' % (show(f), path or '<root>', msg))
            continue
        if out is None:
            continue
        z['bestanden'] += 1
        z.update(m.counts)
    total = collections.Counter()
    print('m8 --dry-run: per zone, the documents read, the files a run rewrites and the symbols it moves '
          'from the spellings to the symbol')
    for zone, c in per_zone.items():
        total.update(c)
        print('  %-40s documenten %4d  bestanden %3d  symbolen %3d  geweigerd %d'
              % (zone, c['documenten'], c['bestanden'], c['symbolen'], c['geweigerd']))
    print('  %-40s documenten %4d  bestanden %3d  symbolen %3d  geweigerd %d'
          % ('totaal', total['documenten'], total['bestanden'], total['symbolen'], total['geweigerd']))
    return 1 if refused else 0


def check(files):
    docs = read_all(files)
    if docs is None:
        return 2
    left = 0
    for f, raw in docs:
        out, errors, _ = migrate(raw)
        if errors:
            left += 1
            path, msg = errors[0]
            print('refused: %s at %s: %s' % (show(f), path or '<root>', msg))
        elif out is not None:
            left += 1
            print('symbol among the spellings: %s' % show(f))
    print('m8 --check: %d of %d document(s) carry a symbol among their spellings' % (left, len(files)))
    return 1 if left else 0


def main(argv):
    if not argv or argv[0] in ('-h', '--help'):
        print(__doc__.split('\n\n')[1])
        return 0 if argv else 2
    if argv[0] in ('--check', '--dry-run'):
        files = argv[1:] or default_files()
        if not files:
            sys.stderr.write('FAIL: no ir.json documents to read\n')
            return 2
        return check(files) if argv[0] == '--check' else dry_run(files)
    if argv[0] == '--all':
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
    totals = collections.Counter()
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
        tmp = f + '.m8tmp'
        with open(tmp, 'wb') as fh:
            fh.write(out)
        os.replace(tmp, f)
        migrated += 1
        totals.update(m.counts)
        sys.stderr.write('m8: %s: %s\n' % (show(f), ', '.join('%s %d' % (k, m.counts[k])
                                                              for k in COUNT_NAMES)))
    for f, errors in failed:
        sys.stderr.write('FAIL: %s\n' % show(f))
        for path, msg in errors:
            sys.stderr.write('  at %s: %s\n' % (path or '<root>', msg))
    print('m8: %d migrated, %d without a symbol among their spellings and left untouched, %d refused (left as '
          'they were); %s' % (migrated, kept, len(failed),
                              ', '.join('%s %d' % (k, totals[k]) for k in COUNT_NAMES)))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
