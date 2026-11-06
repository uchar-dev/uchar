#!/usr/bin/env python3
"""L7 of the v1 wire migration: uchar-ir-v1 from its L6 shape to its L7 shape, the member assignments.

    m7.py FILE...            migrate the named ir.json documents in place
    m7.py --all              migrate every ir.json of the zones below
    m7.py --dry-run [FILE...]  count what a run would rewrite, per zone (default: the --all set), and
                             write nothing; exit 1 if a document would be refused
    m7.py --check [FILE...]  list every document that is not in the L7 shape (default: the --all set);
                             exit 1 if there is one

The zones are corpus/, toka/, ref/test/fixtures/ and backends/drools/src/test/resources/.

The members of an object creation are its assignments (IR-SPEC ch. 3.3), and nothing else changes:
every other key, value and key order is carried verbatim.

- The key 'initialisations' of a version of an ObjectCreation becomes 'assignments', at the place it
  stood, so the key order of the version stays the canonical one.
- The node of each member, "MemberInit", becomes "MemberAssignment". The fields of the member
  (member, labels, value, characteristic) stay, in their order.

Loud by construction: a shape this script does not know fails the document with its file name and the
JSON pointer into the input, and a failing document is left untouched. That covers 'initialisations'
on any other object than a version of an ObjectCreation, a MemberInit node anywhere else than in that
list, a version that carries both 'initialisations' and 'assignments', an 'initialisations' that is no
list, a member that is not a MemberInit node, and an 'assignments' list that still carries a
MemberInit. A document already in the L7 shape is left untouched
byte for byte, so a second run changes nothing. Every migrated document gets one line on stderr
counting what was rewritten.

Output is written with gate 2's own serialiser, executed from scripts/check-ir-schema.sh exactly as
m1.py to m6.py do, so a migrated document round-trips byte-identically under gate 2; the script also
holds that the written bytes are the input with the two names replaced and nothing else. A test
resource another writer pretty-printed (Jackson's '"key" : value') keeps its layout: its bytes get the
two names replaced, and the result must read as exactly the migrated document, key order included, or
the document is refused.
"""
import collections
import glob
import json
import os
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

OLD_KEY, NEW_KEY = 'initialisations', 'assignments'
OLD_NODE, NEW_NODE = 'MemberInit', 'MemberAssignment'
COUNT_NAMES = ('versies', 'leden')


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
        return [(k, plain(x)) for k, x in v.items()]
    if isinstance(v, list):
        return [plain(x) for x in v]
    return v


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
        if v.get('node') == OLD_NODE:
            self.fail(p, 'a %s node outside the members of an object creation' % OLD_NODE)
            return v
        if OLD_KEY in v:
            self.fail(p + ptr(OLD_KEY), "'%s' on an object that is no version of an ObjectCreation"
                      % OLD_KEY)
            return v
        if v.get('node') == 'ObjectCreation' and isinstance(v.get('versions'), list):
            return {k: ([self.version(x, p + ptr(k, i)) for i, x in enumerate(w)] if k == 'versions'
                        else self.walk(w, p + ptr(k)))
                    for k, w in v.items()}
        return {k: self.walk(x, p + ptr(k)) for k, x in v.items()}

    def version(self, v, p):
        if not isinstance(v, dict):
            self.fail(p, 'a version of an ObjectCreation that is not an object')
            return v
        if OLD_KEY in v and NEW_KEY in v:
            self.fail(p, "a version that carries both '%s' and '%s'" % (OLD_KEY, NEW_KEY))
            return v
        out = {}
        for k, x in v.items():
            if k == OLD_KEY:
                out[NEW_KEY] = self.members(x, p + ptr(k))
                self.counts['versies'] += 1
            elif k == NEW_KEY:
                if isinstance(x, list):
                    for i, m in enumerate(x):
                        if isinstance(m, dict) and m.get('node') == OLD_NODE:
                            self.fail(p + ptr(k, i), "a %s in '%s': the two shapes are mixed"
                                      % (OLD_NODE, NEW_KEY))
                out[k] = self.walk(x, p + ptr(k))
            else:
                out[k] = self.walk(x, p + ptr(k))
        return out

    def members(self, ms, p):
        if not isinstance(ms, list):
            self.fail(p, "'%s' that is not a list" % OLD_KEY)
            return ms
        out = []
        for i, m in enumerate(ms):
            q = p + ptr(i)
            if not isinstance(m, dict) or m.get('node') != OLD_NODE:
                self.fail(q, 'a member that is not a %s node' % OLD_NODE)
                out.append(m)
                continue
            self.counts['leden'] += 1
            out.append({k: (NEW_NODE if k == 'node' else self.walk(x, q + ptr(k)))
                        for k, x in m.items()})
        return out


def migrate(raw):
    """(new bytes | None when already in the L7 shape, [(json pointer, message)], migration)."""
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
    if m.counts['versies'] == 0:
        return None, [], m
    # the two names are the only difference: the bytes of the input with the key and the node
    # replaced where they stand as a key and as a node, in gate 2's spelling ('"k": ') or in the
    # spelling of a test resource another writer pretty-printed ('"k" : ')
    renamed = raw
    for sep in (': ', ' : '):
        renamed = renamed.replace(('"%s"%s' % (OLD_KEY, sep)).encode(),
                                  ('"%s"%s' % (NEW_KEY, sep)).encode()) \
                         .replace(('"node"%s"%s"' % (sep, OLD_NODE)).encode(),
                                  ('"node"%s"%s"' % (sep, NEW_NODE)).encode())
    if (ser(doc, 0) + '\n').encode('utf-8') == raw:
        out = (ser(new, 0) + '\n').encode('utf-8')
        if out != renamed:
            return None, [('', 'internal: the written document differs from the input in more '
                               'than the two names')], None
        if (ser(parse(out), 0) + '\n').encode('utf-8') != out:
            return None, [('', 'internal: the serialised document does not read back as built')], None
        return out, [], m
    # not gate 2's serialisation (a test resource): the rename keeps the writer's layout, and it
    # stands only if the renamed bytes read as exactly the migrated document
    if plain(parse(renamed)) != plain(new):
        return None, [('', "not in gate 2's serialisation, and the two names do not stand where a "
                           'rename of the bytes reaches every one of them')], None
    return renamed, [], m


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
    print('m7 --dry-run: per zone, the documents read, the files a run rewrites, the ObjectCreation '
          'versions whose key it renames and the members whose node it renames')
    for zone, c in per_zone.items():
        total.update(c)
        print('  %-40s documenten %4d  bestanden %3d  versies %3d  leden %3d  geweigerd %d'
              % (zone, c['documenten'], c['bestanden'], c['versies'], c['leden'], c['geweigerd']))
    print('  %-40s documenten %4d  bestanden %3d  versies %3d  leden %3d  geweigerd %d'
          % ('totaal', total['documenten'], total['bestanden'], total['versies'], total['leden'],
             total['geweigerd']))
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
            print('L6: %s' % show(f))
    print('m7 --check: %d of %d document(s) not in the L7 shape' % (left, len(files)))
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
        tmp = f + '.m7tmp'
        with open(tmp, 'wb') as fh:
            fh.write(out)
        os.replace(tmp, f)
        migrated += 1
        totals.update(m.counts)
        sys.stderr.write('m7: %s: %s\n' % (show(f), ', '.join('%s %d' % (k, m.counts[k])
                                                              for k in COUNT_NAMES)))
    for f, errors in failed:
        sys.stderr.write('FAIL: %s\n' % show(f))
        for path, msg in errors:
            sys.stderr.write('  at %s: %s\n' % (path or '<root>', msg))
    print('m7: %d migrated, %d already in the L7 shape and left untouched, %d refused (left as they '
          'were); %s' % (migrated, kept, len(failed),
                         ', '.join('%s %d' % (k, totals[k]) for k in COUNT_NAMES)))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
