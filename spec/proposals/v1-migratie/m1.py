#!/usr/bin/env python3
"""M1 of the v1 wire migration: regelspraak-ir-v0 -> uchar-ir-v1 in its M1 shape.

    m1.py FILE...             migrate the named ir.json documents in place
    m1.py --all               migrate every corpus/**/ir.json and toka/**/ir.json
    m1.py --check [FILE...]   list every document that still carries "formaat"
                              (default: the --all set); exit 1 if there is one

The target shape is the one the M1 card (20260923-m1-schema-en-container) fixes
under "De M1-vorm van de wire", and spec/schema/uchar-ir-v1.schema.json types it.
M1 restructures the container and nothing else: the root nests
{format, model, rules, diagnostics}, the six v0 registries become lists, the
inert `regels` stubs leave the wire, the eight Objecttype side-maps fold into
Attribute/Characteristic/TimelineFacet nodes, and a domain becomes a Domain
wrapper whose datatype drops its self-name. Every other node is carried verbatim.

Loud by construction: a v0 value the M1 shape cannot carry (a null or a
disagreeing self-name, a null datatype, a side-map key naming no member, a facet
entry without a granularity key, a value outside its enum) fails the document
with its file name and the JSON pointer into the v0 input; nothing is repaired.
A failing document is left untouched. A document already in uchar-ir-v1 is left
untouched as well, byte for byte, so a second run changes nothing.

Output is written with gate 2's own serialiser, executed from
scripts/check-ir-schema.sh exactly as gates 9 and 14 do, so a migrated document
round-trips byte-identically under gate 2.
"""
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

V0 = 'regelspraak-ir-v0'
V1 = 'uchar-ir-v1'

V0_ROOT = ['formaat', 'objecttypen', 'domeinen', 'parameters', 'eenheidstelsels', 'feittypen',
           'dimensies', 'tijdlijnen', 'dagsoorten', 'dagsoortPerEenheid', 'regels',
           'resolvedRegels', 'resolvedDagsoorten', 'objectCreaties', 'verdelingen',
           'consistenties', 'uniciteiten', 'uniciteitUnies', 'feitCreaties',
           'resolvedStartpunten', 'regelGroepen', 'diagnostics']
V0_ROOT_OPTIONAL = ['prioritizedDefaults', 'functionDefs']
RULE_KEYS = ['resolvedRegels', 'resolvedDagsoorten', 'objectCreaties', 'verdelingen',
             'consistenties', 'uniciteiten', 'uniciteitUnies', 'feitCreaties',
             'resolvedStartpunten', 'regelGroepen']

ATTR_MAPS = ['attribuutMeervoud', 'attribuutDimensies', 'attribuutGranulariteit',
             'attribuutTijdlijnAantal', 'attribuutTijdlijnNaam']
CHAR_MAPS = ['kenmerkSoorten', 'kenmerkGranulariteit', 'kenmerkTijdlijnAantal']
V0_OBJECTTYPE = ['node', 'naam', 'attributen'] + ATTR_MAPS + ['kenmerken'] + CHAR_MAPS + \
                ['bezield', 'meervoud']

GRANULARITIES = ('DAG', 'WEEK', 'MAAND', 'KWARTAAL', 'JAAR')
KINDS = ('BIJVOEGLIJK', 'BEZITTELIJK', 'ZELFSTANDIG')
# the self-name field a domain datatype carries at the declaration position in v0
SELF_NAME = {'Enumeratie': 'domein', 'SumType': 'domain', 'StructType': 'domain'}
NAMELESS_DATATYPES = ('Numeriek', 'Percentage', 'Tekst', 'Booleaan', 'DatumInDagen',
                      'DatumTijd', 'Onbekend', 'ListType')
_INTEGER = re.compile(r'-?(0|[1-9][0-9]*)')


class Refusal(Exception):
    pass


def ptr(*parts):
    return ''.join('/' + str(p).replace('~', '~0').replace('/', '~1') for p in parts)


def _pairs(pairs):
    # a duplicate key would vanish silently in a dict, taking a declaration with it
    keys = [k for k, _ in pairs]
    dup = sorted({k for k in keys if keys.count(k) > 1})
    if dup:
        raise Refusal('duplicate object key(s) %s' % ', '.join(json.dumps(k) for k in dup))
    return dict(pairs)


def parse(raw):
    return json.loads(raw.decode('utf-8'), parse_int=RawNum, parse_float=RawNum,
                      object_pairs_hook=_pairs)


def is_int(v):
    return isinstance(v, RawNum) and _INTEGER.fullmatch(v.raw) is not None


class Migration:
    def __init__(self):
        self.errors = []

    def fail(self, path, msg):
        self.errors.append((path, msg))

    def obj(self, v, path, what):
        if not isinstance(v, dict):
            self.fail(path, '%s is not an object' % what)
            return None
        return v

    # --- the five tables whose node stays v0: the map key goes, the node keeps `naam` ---------
    def named_nodes(self, doc, table):
        out = []
        m = self.obj(doc[table], ptr(table), table)
        for key, node in (m or {}).items():
            p = ptr(table, key)
            if self.obj(node, p, 'the declaration') is None:
                continue
            if 'naam' not in node or node['naam'] is None:
                self.fail(p + '/naam', 'self-name is null or absent: dropping the map key would '
                          'leave the declaration without its name %s' % json.dumps(key))
            elif node['naam'] != key:
                self.fail(p + '/naam', 'self-name %s differs from its map key %s: the list form '
                          'keeps one spelling and there is no rule for choosing'
                          % (json.dumps(node['naam']), json.dumps(key)))
            out.append(node)
        return out

    def domains(self, doc):
        out = []
        m = self.obj(doc['domeinen'], ptr('domeinen'), 'domeinen')
        for key, dt in (m or {}).items():
            p = ptr('domeinen', key)
            if dt is None:
                self.fail(p, 'null datatype: a Domain carries a datatype')
                continue
            if self.obj(dt, p, 'the domain datatype') is None:
                continue
            node = dt.get('node')
            if node in SELF_NAME:
                field = SELF_NAME[node]
                if dt.get(field) is None:
                    self.fail(p + '/' + field, 'self-name is null or absent at the declaration '
                              'position of domain %s' % json.dumps(key))
                elif dt[field] != key:
                    self.fail(p + '/' + field, 'self-name %s differs from its map key %s: the '
                              'list form keeps one spelling and there is no rule for choosing'
                              % (json.dumps(dt[field]), json.dumps(key)))
                dt = {k: v for k, v in dt.items() if k != field}
            elif node not in NAMELESS_DATATYPES:
                self.fail(p + '/node', 'not a datatype node: %s' % json.dumps(node))
            out.append({'node': 'Domain', 'name': key, 'datatype': dt})
        return out

    # --- Objecttype -> ObjectType with member nodes ---------------------------------------------
    def facet(self, gmap, cmap, nmap, member, p, mapnames):
        g = gmap[member]
        if g not in GRANULARITIES:
            self.fail(p(mapnames[0], member), 'granularity %s is not one of %s'
                      % (json.dumps(g), '/'.join(GRANULARITIES)))
        facet = {'node': 'TimelineFacet', 'granularity': g}
        if member in cmap:
            c = cmap[member]
            if not is_int(c):
                self.fail(p(mapnames[1], member), 'count %s is not an integer'
                          % (json.dumps(c) if not isinstance(c, RawNum) else c.raw))
            facet['count'] = c
        else:
            facet['count'] = RawNum('1')
        if nmap is not None and member in nmap:
            n = nmap[member]
            if not isinstance(n, str):
                self.fail(p(mapnames[2], member), 'timeline name %s is not a string'
                          % json.dumps(n))
            facet['timelineName'] = n
        return facet

    def object_type(self, key, ot):
        base = ptr('objecttypen', key)
        p = lambda *parts: base + ptr(*parts)
        if self.obj(ot, base, 'the object type') is None:
            return None
        if ot.get('node') != 'Objecttype':
            self.fail(p('node'), 'node is %s, not "Objecttype"' % json.dumps(ot.get('node')))
            return None
        for k in ot:
            if k not in V0_OBJECTTYPE and k != 'supertype':
                self.fail(p(k), 'key unknown to the v0 Objecttype')
        missing = [k for k in V0_OBJECTTYPE if k not in ot]
        for k in missing:
            self.fail(p(k), 'v0 Objecttype key absent')
        if missing:
            return None
        if ot['naam'] is None:
            self.fail(p('naam'), 'self-name is null: dropping the map key would leave the object '
                      'type without its name %s' % json.dumps(key))
        elif ot['naam'] != key:
            self.fail(p('naam'), 'self-name %s differs from its map key %s: the list form keeps '
                      'one spelling and there is no rule for choosing'
                      % (json.dumps(ot['naam']), json.dumps(key)))
        new = {'node': 'ObjectType', 'name': key}
        if 'supertype' in ot:
            if not isinstance(ot['supertype'], str):
                self.fail(p('supertype'), 'supertype is not a string')
            new['supertype'] = ot['supertype']
        if not isinstance(ot['bezield'], bool):
            self.fail(p('bezield'), 'bezield is not a boolean')
        new['animate'] = ot['bezield']
        if ot['meervoud'] is not None:
            if not isinstance(ot['meervoud'], str):
                self.fail(p('meervoud'), 'meervoud is not a string')
            new['plural'] = ot['meervoud']

        maps = {}
        for name in ATTR_MAPS + CHAR_MAPS:
            maps[name] = self.obj(ot[name], p(name), name) or {}
        attrs = self.obj(ot['attributen'], p('attributen'), 'attributen') or {}
        chars = ot['kenmerken']
        if not isinstance(chars, list):
            self.fail(p('kenmerken'), 'kenmerken is not a list')
            chars = []

        # a side-map key that names no member is a facet the member-node form cannot hold
        for name in ATTR_MAPS:
            for k in maps[name]:
                if k not in attrs:
                    self.fail(p(name, k), 'key names no declared attribute of %s'
                              % json.dumps(key))
        for name in CHAR_MAPS:
            for k in maps[name]:
                if k not in chars:
                    self.fail(p(name, k), 'key names no declared characteristic of %s'
                              % json.dumps(key))
        # count and name hang on the facet, and the facet exists only with a granularity key
        for name, gname, members in (('attribuutTijdlijnAantal', 'attribuutGranulariteit', attrs),
                                     ('attribuutTijdlijnNaam', 'attribuutGranulariteit', attrs),
                                     ('kenmerkTijdlijnAantal', 'kenmerkGranulariteit', chars)):
            for k in maps[name]:
                if k in members and k not in maps[gname]:
                    self.fail(p(name, k), 'facet entry for a member with no %s key: the member '
                              'has no timeline to carry it' % gname)

        attributes = []
        for a, dt in attrs.items():
            if dt is None:
                self.fail(p('attributen', a), 'null datatype: the migration does not carry '
                          'a datatype-less attribute (WF-059) across the epoch')
            node = {'node': 'Attribute', 'name': a, 'datatype': dt}
            if a in maps['attribuutMeervoud'] and maps['attribuutMeervoud'][a] is not None:
                pl = maps['attribuutMeervoud'][a]
                if not isinstance(pl, str):
                    self.fail(p('attribuutMeervoud', a), 'plural is not a string')
                node['plural'] = pl
            if a in maps['attribuutDimensies']:
                dims = maps['attribuutDimensies'][a]
                if not isinstance(dims, list) or not all(isinstance(x, str) for x in dims):
                    self.fail(p('attribuutDimensies', a), 'dimensions are not a list of strings')
                elif dims:
                    node['dimensions'] = dims
            if a in maps['attribuutGranulariteit']:
                node['timeline'] = self.facet(
                    maps['attribuutGranulariteit'], maps['attribuutTijdlijnAantal'],
                    maps['attribuutTijdlijnNaam'], a, p,
                    ('attribuutGranulariteit', 'attribuutTijdlijnAantal', 'attribuutTijdlijnNaam'))
            attributes.append(node)
        new['attributes'] = attributes

        characteristics = []
        for i, c in enumerate(chars):
            if not isinstance(c, str):
                self.fail(p('kenmerken', i), 'characteristic name %s is not a string'
                          % json.dumps(c))
                continue
            kind = maps['kenmerkSoorten'].get(c)
            if kind is None:
                kind = 'ZELFSTANDIG'
            elif kind not in KINDS:
                self.fail(p('kenmerkSoorten', c), 'kind %s is not one of %s'
                          % (json.dumps(kind), '/'.join(KINDS)))
            node = {'node': 'Characteristic', 'name': c, 'kind': kind}
            if c in maps['kenmerkGranulariteit']:
                node['timeline'] = self.facet(
                    maps['kenmerkGranulariteit'], maps['kenmerkTijdlijnAantal'], None, c, p,
                    ('kenmerkGranulariteit', 'kenmerkTijdlijnAantal', None))
            characteristics.append(node)
        new['characteristics'] = characteristics
        return new

    # --- the root ---------------------------------------------------------------------------------
    def run(self, doc):
        for k in doc:
            if k not in V0_ROOT and k not in V0_ROOT_OPTIONAL:
                self.fail(ptr(k), 'root key unknown to v0')
        missing = [k for k in V0_ROOT if k not in doc]
        for k in missing:
            self.fail(ptr(k), 'v0 root key absent')
        if missing:
            return None
        ots = []
        for key, ot in (self.obj(doc['objecttypen'], ptr('objecttypen'), 'objecttypen') or {}).items():
            new = self.object_type(key, ot)
            if new is not None:
                ots.append(new)
        model = {
            'objectTypes': ots,
            'factTypes': self.named_nodes(doc, 'feittypen'),
            'parameters': self.named_nodes(doc, 'parameters'),
            'domains': self.domains(doc),
            'unitSystems': self.named_nodes(doc, 'eenheidstelsels'),
            'dimensions': self.named_nodes(doc, 'dimensies'),
            'timelines': doc['tijdlijnen'],
            'dayKinds': doc['dagsoorten'],
            'dagsoortPerEenheid': doc['dagsoortPerEenheid'],
        }
        if 'functionDefs' in doc:
            model['functionDefs'] = doc['functionDefs']
        rules = {k: doc[k] for k in RULE_KEYS}
        if 'prioritizedDefaults' in doc:
            rules['prioritizedDefaults'] = doc['prioritizedDefaults']
        diags = doc['diagnostics']
        if not isinstance(diags, list):
            self.fail(ptr('diagnostics'), 'diagnostics is not a list')
        else:
            for i, d in enumerate(diags):
                if self.obj(d, ptr('diagnostics', i), 'the diagnostic') is None:
                    continue
                if d.get('severity') != 'WARNING':
                    self.fail(ptr('diagnostics', i, 'severity'), 'severity %s: the wire carries '
                              'WARNING only (WF-113 (a))' % json.dumps(d.get('severity')))
                if not isinstance(d.get('message'), str):
                    self.fail(ptr('diagnostics', i, 'message'), 'message is not a string')
        return {'format': V1, 'model': model, 'rules': rules, 'diagnostics': diags}


def plain(v):
    if isinstance(v, RawNum):
        return ('num', v.raw)
    if isinstance(v, dict):
        return {k: plain(x) for k, x in v.items()}
    if isinstance(v, list):
        return [plain(x) for x in v]
    return v


def migrate_bytes(raw):
    """(new bytes | None when already v1, [(json pointer, message)]) for one document."""
    try:
        doc = parse(raw)
    except (UnicodeDecodeError, ValueError, Refusal) as exc:
        return None, [('', 'cannot parse: %s' % exc)]
    if not isinstance(doc, dict):
        return None, [('', 'the document is not a JSON object')]
    if 'formaat' not in doc and doc.get('format') == V1:
        return None, []
    if 'format' in doc:
        return None, [(ptr('format'), 'a document carrying both "formaat" and "format", or a '
                       '"format" other than %s' % json.dumps(V1))]
    if doc.get('formaat') != V0:
        return None, [(ptr('formaat'), 'format %s is not %s'
                       % (json.dumps(doc.get('formaat')), json.dumps(V0)))]
    m = Migration()
    new = m.run(doc)
    if m.errors:
        return None, m.errors
    out = (ser(new, 0) + '\n').encode('utf-8')
    # the written bytes must read back as exactly the document built, in gate 2's canonical form
    back = parse(out)
    if plain(back) != plain(new) or (ser(back, 0) + '\n').encode('utf-8') != out:
        return None, [('', 'internal: the serialised document does not read back as built')]
    return out, []


def default_files():
    files = sorted(glob.glob(os.path.join(ROOT, 'corpus', '**', 'ir.json'), recursive=True))
    files += sorted(glob.glob(os.path.join(ROOT, 'toka', '**', 'ir.json'), recursive=True))
    return files


def show(path):
    rel = os.path.relpath(path, ROOT)
    return path if rel.startswith('..') else rel


def check(files):
    left = []
    for f in files:
        try:
            doc = json.loads(open(f, 'rb').read().decode('utf-8'))
        except (OSError, UnicodeDecodeError, ValueError) as exc:
            sys.stderr.write('FAIL: cannot read/parse %s: %s\n' % (show(f), exc))
            return 2
        if not isinstance(doc, dict):
            sys.stderr.write('FAIL: %s is not a JSON object\n' % show(f))
            return 2
        if 'formaat' in doc:
            left.append(f)
    for f in left:
        print('v0: %s' % show(f))
    print('m1 --check: %d of %d document(s) still carry "formaat"' % (len(left), len(files)))
    return 1 if left else 0


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
    for f in files:
        try:
            raw = open(f, 'rb').read()
        except OSError as exc:
            failed.append((f, [('', 'cannot read: %s' % exc)]))
            continue
        out, errors = migrate_bytes(raw)
        if errors:
            failed.append((f, errors))
            continue
        if out is None:
            kept += 1
            continue
        tmp = f + '.m1tmp'
        with open(tmp, 'wb') as fh:
            fh.write(out)
        os.replace(tmp, f)
        migrated += 1
    for f, errors in failed:
        sys.stderr.write('FAIL: %s\n' % show(f))
        for path, msg in errors:
            sys.stderr.write('  at %s: %s\n' % (path or '<root>', msg))
    print('m1: %d migrated, %d already %s and left untouched, %d refused (left on %s)'
          % (migrated, kept, V1, len(failed), V0))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
