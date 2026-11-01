#!/usr/bin/env python3
"""L6 of the v1 wire migration: uchar-ir-v1 from its L5 shape to its L6 shape, the factor list.

    m6.py FILE...          migrate the named ir.json documents in place
    m6.py --all            migrate every ir.json of the zones below
    m6.py --check [FILE...]  list every document that is not in the L6 shape (default: the --all
                           set) and every declaration that moved against the base commit; exit 1
                           if there is one

The zones are corpus/, toka/, ref/test/fixtures/ and backends/drools/src/test/resources/.

Number.unit becomes a non-empty list of Unit nodes, IR-SPEC ch. 2.5, and nothing else changes: every
other key, value and key order is carried verbatim.

- A unit occurrence is wrapped in a list. Its fields stay in their order.
- A denominator becomes a further factor with the opposite exponent (a denominator of a denominator
  turns the sign back). An exponent of 1 is written by no key: 'exponent' is the last key of a factor
  and appears where the exponent is not 1.
- The numerator "1" of a reciprocal is left out: it names no factor.
- A product, an abbreviation with '·' in it, falls apart into its atomic factors. Each keeps the
  product's system and carries no conversion pair, as the product did not. A factor named twice is one
  factor with its exponents added ("m·m" is m with exponent 2).
- The factors stand in the canonical order of WF-124: positive exponents before negative ones, within
  a sign by abbreviation in code point order, then by system with an absent system first.
- A declaration in UnitSystem.units is not touched: it stays a single Unit, and --check compares its
  bytes with the base commit.

Loud by construction: a shape this script does not know fails the document with its file name and the
JSON pointer into the input, and a failing document is left untouched. That covers a Unit node at any
other place than Number.unit and UnitSystem.units, a unit that is not an object, a "1" that stands
without a denominator or carries a system or a conversion pair, a product that carries a conversion
pair, an empty factor name, two occurrences of one factor whose records differ, a factor whose exponents
add up to zero, a denominator on a declaration, and a list that still carries a denominator or the
numerator "1". A list in any other respect is the L6 shape and stays as it is, whatever WF-124 says of
it: the witnesses of that rule are written in the target shape, and judging them is the verifier's work.
A document already in the L6 shape is left untouched byte for byte, so a second run changes nothing.
Every migrated document gets one line on stderr counting what was rewritten.

Output is written with gate 2's own serialiser, executed from scripts/check-ir-schema.sh exactly as
m1.py to m5.py do, so a migrated document round-trips byte-identically under gate 2.
"""
import collections
import glob
import json
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

_gate2 = open(os.path.join(ROOT, 'scripts', 'check-ir-schema.sh'), encoding='utf-8').read()
_ns = {'json': json}
exec(_gate2[_gate2.index('class RawNum'):_gate2.index('def roundtrip')], _ns)
RawNum, ser = _ns['RawNum'], _ns['ser']

FORMAT = 'uchar-ir-v1'

# the commit the declarations are compared with: the last one in the L5 shape
BASE = 'e92fcb1f8'

ZONES = (('corpus', '**', 'ir.json'), ('toka', '**', 'ir.json'),
         ('ref', 'test', 'fixtures', '**', 'ir.json'),
         ('backends', 'drools', 'src', 'test', 'resources', '**', 'ir.json'))

PRODUCT = '·'
ONE = '1'
COUNT_NAMES = ('posities', 'noemers', 'tellers weg', 'producten', 'exponenten')
FACTOR_KEYS = ('node', 'abbreviation', 'system', 'canonical', 'factorToCanonical', 'exponent')


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


def order_key(factor):
    """WF-124 (d): the sign, then the abbreviation in code point order, then the system with an absent
       system first. Python compares strings by code point, which is the order of the rule."""
    e = factor.get('exponent')
    exponent = 1 if e is None else int(e.raw)
    return (0 if exponent > 0 else 1, factor['abbreviation'],
            0 if 'system' not in factor else 1, factor.get('system', ''))


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
        n = v.get('node')
        if n == 'Unit':
            self.fail(p, 'a Unit node outside Number.unit and UnitSystem.units')
            return v
        if n == 'UnitSystem' and isinstance(v.get('units'), list):
            return {k: (self.declared(x, p + ptr(k)) if k == 'units' else self.walk(x, p + ptr(k)))
                    for k, x in v.items()}
        if n == 'Number' and 'unit' in v:
            return {k: (self.occurrence(x, p + ptr(k)) if k == 'unit' else self.walk(x, p + ptr(k)))
                    for k, x in v.items()}
        return {k: self.walk(x, p + ptr(k)) for k, x in v.items()}

    def declared(self, units, p):
        for i, u in enumerate(units):
            q = p + ptr(i)
            if not isinstance(u, dict) or u.get('node') != 'Unit':
                self.fail(q, 'a declaration in a unit system that is not a Unit node')
            elif 'denominator' in u:
                self.fail(q + ptr('denominator'), 'a denominator on a declaration: a declaration '
                          'is one unit, and the L6 shape has no spelling for this one')
        return units

    def occurrence(self, u, p):
        if isinstance(u, list):
            for i, f in enumerate(u):
                self.factor_in_list(f, p + ptr(i))
            return u
        if not isinstance(u, dict) or u.get('node') != 'Unit':
            self.fail(p, 'Number.unit that is neither a Unit node nor a list of them')
            return u
        self.counts['posities'] += 1
        merged = {}
        for f, q in self.factors(u, 1, p):
            key = (f['abbreviation'], f.get('system'))
            if key not in merged:
                merged[key] = (dict(f), q)
                continue
            first, _ = merged[key]
            if plain({k: x for k, x in first.items() if k != 'exponent'}) != \
                    plain({k: x for k, x in f.items() if k != 'exponent'}):
                self.fail(q, 'the factor %s stands twice with different records: the L6 shape has '
                          'one factor per abbreviation and system' % json.dumps(f['abbreviation']))
                return u
            first['exponent'] = exponent_of(first) + exponent_of(f)
        out = []
        for f, q in merged.values():
            e = exponent_of(f)
            if e == 0:
                self.fail(q, 'the exponents of the factor %s add up to zero: the L6 shape has no '
                          'such factor, and the migration does not decide what the unit then is'
                          % json.dumps(f['abbreviation']))
                return u
            f.pop('exponent', None)
            if e != 1:
                f['exponent'] = RawNum(str(e))
                self.counts['exponenten'] += 1
            out.append(f)
        if not out:
            self.fail(p, 'a unit without a factor')
            return u
        return sorted(out, key=order_key)

    def factor_in_list(self, f, p):
        if not isinstance(f, dict) or f.get('node') != 'Unit':
            self.fail(p, 'an entry of Number.unit that is not a Unit node')
        elif 'denominator' in f:
            self.fail(p + ptr('denominator'), 'a denominator on a factor of a list: the two shapes '
                      'are mixed')
        elif f.get('abbreviation') == ONE:
            self.fail(p + ptr('abbreviation'), 'the numerator "1" in a list: the L6 shape writes no '
                      'such factor')

    def factors(self, u, sign, p):
        """(factor, pointer) of the record [u] under the exponent sign [sign], numerator first."""
        abbr = u.get('abbreviation')
        if not isinstance(abbr, str):
            self.fail(p + ptr('abbreviation'), 'a unit without an abbreviation string')
            return []
        for k in u:
            if k not in FACTOR_KEYS[:-1] + ('denominator',):
                self.fail(p + ptr(k), 'the key %s on a Unit occurrence: the L5 shape has no such key'
                          % json.dumps(k))
                return []
        denominator = u.get('denominator')
        out = []
        if abbr == ONE:
            if denominator is None:
                self.fail(p, 'the unit "1" without a denominator: it names no factor, and the '
                          'L6 shape has no empty unit')
                return []
            if any(k in u for k in ('system', 'canonical', 'factorToCanonical')):
                self.fail(p, 'the numerator "1" with a system or a conversion pair: it names no '
                          'factor, so the fields have no place to go')
                return []
            self.counts['tellers weg'] += 1
        elif PRODUCT in abbr:
            if 'canonical' in u or 'factorToCanonical' in u:
                self.fail(p, 'a product with a conversion pair: its factors have no record to '
                          'carry it')
                return []
            names = abbr.split(PRODUCT)
            if '' in names:
                self.fail(p + ptr('abbreviation'), 'an empty factor name in the product %s'
                          % json.dumps(abbr))
                return []
            self.counts['producten'] += 1
            for name in names:
                f = {'node': 'Unit', 'abbreviation': name}
                if 'system' in u:
                    f['system'] = u['system']
                f['exponent'] = sign
                out.append((f, p))
        else:
            f = {k: x for k, x in u.items() if k != 'denominator'}
            f['exponent'] = sign
            out.append((f, p))
        if denominator is not None:
            self.counts['noemers'] += 1
            if not isinstance(denominator, dict) or denominator.get('node') != 'Unit':
                self.fail(p + ptr('denominator'), 'a denominator that is not a Unit node')
                return []
            out += self.factors(denominator, -sign, p + ptr('denominator'))
        return out


def exponent_of(f):
    e = f.get('exponent', 1)
    return int(e.raw) if isinstance(e, RawNum) else e


def declarations(v, p=''):
    """(pointer, serialised bytes) of every declaration in [v]: what must not move."""
    out = []
    if isinstance(v, list):
        for i, x in enumerate(v):
            out += declarations(x, p + ptr(i))
    elif isinstance(v, dict):
        if v.get('node') == 'UnitSystem' and isinstance(v.get('units'), list):
            out += [(p + ptr('units', i), ser(u, 0)) for i, u in enumerate(v['units'])]
        for k, x in v.items():
            out += declarations(x, p + ptr(k))
    return out


def migrate(raw):
    """(new bytes | None when already in the L6 shape, [(json pointer, message)], migration)."""
    try:
        doc = parse(raw)
    except (UnicodeDecodeError, ValueError, Refusal) as exc:
        return None, [('', 'cannot parse: %s' % exc)], None
    if not isinstance(doc, dict):
        return None, [('', 'the document is not a JSON object')], None
    if doc.get('format') != FORMAT:
        return None, [(ptr('format'), 'format %s is not %s (m1.py migrates the v0 shape first)'
                       % (json.dumps(doc.get('format')), json.dumps(FORMAT)))], None
    m = Migration()
    new = m.walk(doc, '')
    if m.errors:
        return None, m.errors, None
    if m.counts['posities'] == 0:
        return None, [], None
    out = (ser(new, 0) + '\n').encode('utf-8')
    # the written bytes must read back as exactly the document built, in gate 2's canonical form
    back = parse(out)
    if plain(back) != plain(new) or (ser(back, 0) + '\n').encode('utf-8') != out:
        return None, [('', 'internal: the serialised document does not read back as built')], None
    if declarations(new) != declarations(doc):
        return None, [('', 'internal: a declaration moved')], None
    return out, [], m


def migrate_bytes(raw):
    """(new bytes | None when already in the L6 shape, [(json pointer, message)]): the run, for a
       document embedded in a test suite."""
    out, errors, _ = migrate(raw)
    return out, errors


def default_files():
    files = []
    for zone in ZONES:
        files += sorted(glob.glob(os.path.join(ROOT, *zone), recursive=True))
    return files


def show(path):
    rel = os.path.relpath(path, ROOT)
    return path if rel.startswith('..') else rel


def base_bytes(path):
    """The document at the base commit, or None where it did not exist there."""
    rel = os.path.relpath(path, ROOT)
    run = subprocess.run(['git', '-C', ROOT, 'show', '%s:%s' % (BASE, rel)], capture_output=True)
    return run.stdout if run.returncode == 0 else None


def check(files):
    left = moved = compared = 0
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
            print('refused: %s at %s: %s' % (show(f), path or '<root>', msg))
            continue
        if out is not None:
            left += 1
            print('L5: %s' % show(f))
            continue
        base = base_bytes(f)
        if base is None:
            continue
        now, then = declarations(parse(raw)), declarations(parse(base))
        compared += len(then)
        if now != then:
            moved += 1
            print('a declaration moved against %s: %s' % (BASE, show(f)))
    print('m6 --check: %d of %d document(s) not in the L6 shape, %d with a declaration that moved '
          '(%d declarations compared with %s)' % (left, len(files), moved, compared, BASE))
    return 1 if left or moved else 0


def main(argv):
    if not argv or argv[0] in ('-h', '--help'):
        print(__doc__.split('\n\n')[1])
        return 0 if argv else 2
    if argv[0] == '--check':
        files = argv[1:] or default_files()
        if not files:
            sys.stderr.write('FAIL: no ir.json documents to check\n')
            return 2
        return check(files)
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
        tmp = f + '.m6tmp'
        with open(tmp, 'wb') as fh:
            fh.write(out)
        os.replace(tmp, f)
        migrated += 1
        totals.update(m.counts)
        sys.stderr.write('m6: %s: %s\n' % (show(f), ', '.join('%s %d' % (k, m.counts[k])
                                                              for k in COUNT_NAMES)))
    for f, errors in failed:
        sys.stderr.write('FAIL: %s\n' % show(f))
        for path, msg in errors:
            sys.stderr.write('  at %s: %s\n' % (path or '<root>', msg))
    print('m6: %d migrated, %d already in the L6 shape and left untouched, %d refused (left as they '
          'were); %s' % (migrated, kept, len(failed),
                         ', '.join('%s %d' % (k, totals[k]) for k in COUNT_NAMES)))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
