#!/usr/bin/env python3
"""L3 of the v1 wire migration: uchar-ir-v1 from its L2 shape to its L3 shape (M4).

    m3.py FILE...             migrate the named ir.json documents in place
    m3.py --all               migrate every corpus/**/ir.json and toka/**/ir.json
    m3.py --check [FILE...]   list every document that is not yet in the L3 shape
                              (default: the --all set); exit 1 if there is one

The target shape is the one the L3 card (20260924-l3-typesysteem) fixes under
"De L3-vorm van de wire", and spec/schema/uchar-ir-v1.schema.json types it. L3 rewrites
the TYPE SYSTEM and nothing else; every other node, field and value spelling is carried
verbatim, in its own key order. The six L3 nodes are written in the schema's
'properties' order.

- Numeriek becomes Number {node, whole, decimals, sign, unit?}. A null teken is written
  ONBEPAALD, the v0 spelling of an unspecified sign; a null eenheid is left out.
- Percentage becomes a Number with the unit '%' and the sign ONBEPAALD (IR-SPEC ch. 2.4,
  the migration line of the percentage delta). A Percentage without a unit gets the
  built-in '%' {abbreviation '%', canonical '%', factor 1/1}, without a system; one
  whose unit is '%' keeps that unit, renamed; any other unit fails the document. The
  presentation tag 'percentage' is written only where the Percentage stood at a
  declaration position: the datatype of an attribute, a parameter or a domain, and
  everything inside such a datatype (a sum-type payload, a list element, a struct
  field), a function or lambda parameter, a function result. At a type annotation (the
  'type' of any expression node, Literal, Let and Fold included) it becomes the same
  Number without the tag.
- DatumInDagen becomes Date {precision: day}, DatumTijd Date {precision: millisecond}.
- Every Eenheid, at every position and in a noemer recursively, becomes Unit {node,
  abbreviation, system?, canonical?, factorToCanonical?, denominator?}; a null field is
  left out. The factor becomes the exact rational {num, den}: a factor written as a whole
  number is carried verbatim over 1; any other factor becomes the fraction with the
  smallest denominator whose value lies within 10^-14 relative of the L2 double, with no
  bound on the denominator, and a factor for which that fraction has a denominator above
  10^9 fails the document. The rule finds the intended fraction p/q with certainty only
  where no other fraction with a denominator of at most q fits in the tolerance interval:
  such a fraction lies at least 1/q^2 from p/q and the interval is 2 * 10^-14 * p/q wide,
  so the guarantee holds while p*q stays below 5 * 10^13, for a factor around 1 roughly a
  denominator below 10^7. Beyond that the rule may choose a fraction with a smaller
  denominator; 10^9 is only the bound at which the script fails loudly. On the migrated
  data every case was measured again: the 542 reconstructed factors (540 in the parity
  zone, 2 in negatief; 8 distinct lexemes) all lie within the tolerance and inside the
  guarantee, the largest p*q being 86400000. The tolerance is there because the v0
  doubles are products of ratios rounded to 16 digits per step, so exact double equality
  does not find the intended fraction (the parity zone's time units: 1/24, 1/1440,
  1/86400, 1/86400000). What checks the result against the truth is the frontend's
  re-emission of the casus dump, which knows the declared 1/N forms.
- Eenheidstelsel becomes UnitSystem {node, name, units}. The map of units becomes a
  list in map order, one Unit per abbreviation; a map key that differs from its unit's
  abbreviation becomes the first spelling of that unit, and every alias becomes a
  spelling of the unit with the same abbreviation, in alias-map order ('spellings',
  written after the unit's other fields, and only on these declarations). A spelling is
  never the abbreviation itself: an alias equal to its unit's abbreviation is left out,
  and a unit with no spelling left carries no 'spellings'.
- Parameter becomes {node, name, datatype, timeline?}: a granulariteit becomes the
  timeline facet {node: TimelineFacet, granularity, count: 1} without a timeline name;
  a null granulariteit is left out.
- dayKinds and the day-kind registry dagsoortPerEenheid become one list of DayKind
  {node, name, plural?} in the order of dayKinds; the registry key leaves the model.
  Each registry entry is resolved to the day kind its value names, under the identity
  of the verifier's WF-017 (checks.ml norm: whitespace collapsed, ASCII lower case). A
  key equal to that day kind's name is the name itself and is dropped; one further key
  becomes the plural; a second further key fails the document.

Loud by construction: an L2 value the L3 shape cannot carry fails the document with
its file name and the JSON pointer into the L2 input; nothing is repaired. That covers a
factor <= 0 or one without a fraction as above, a third day-kind spelling, a registry
entry that resolves to no declared day kind (the L3 shape has no registry to carry it),
a null in a unit-system map, an alias of a unit the system does not declare or whose
unit differs from the declared one, two map entries that spell one abbreviation with
different units, a percentage whose unit is not '%', a parameter without a datatype,
and an unknown or missing key on a node this script rewrites. A failing document is
left untouched. A document already in the L3 shape is left untouched as well, byte for
byte, so a second run changes nothing. Every migrated document gets one line on stderr
counting what was rewritten.

This script reads the L2 shape, so it names the L2 fields it removes; no L3 artifact
carries those names.

Output is written with gate 2's own serialiser, executed from
scripts/check-ir-schema.sh exactly as m1.py and m2.py do, so a migrated document
round-trips byte-identically under gate 2.
"""
import collections
import glob
import json
import math
import os
import re
import sys
from fractions import Fraction

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

_gate2 = open(os.path.join(ROOT, 'scripts', 'check-ir-schema.sh'), encoding='utf-8').read()
_ns = {'json': json}
exec(_gate2[_gate2.index('class RawNum'):_gate2.index('def roundtrip')], _ns)
RawNum, ser = _ns['RawNum'], _ns['ser']

FORMAT = 'uchar-ir-v1'

# the keys of the L2 nodes this script rewrites, in their L2 order
NUMERIEK_KEYS = ('node', 'geheel', 'decimalen', 'teken', 'eenheid')
PERCENTAGE_KEYS = ('node', 'geheel', 'decimalen', 'eenheid')
EENHEID_KEYS = ('node', 'afkorting', 'stelsel', 'canoniek', 'factorNaarCanoniek', 'noemer')
STELSEL_KEYS = ('node', 'naam', 'eenheden', 'aliassen')
PARAMETER_KEYS = ('node', 'naam', 'datatype', 'granulariteit')
DATE_PRECISION = {'DatumInDagen': 'day', 'DatumTijd': 'millisecond'}
L2_NODES = ('Numeriek', 'Percentage', 'DatumInDagen', 'DatumTijd', 'Eenheid', 'Eenheidstelsel')

# the reconstruction of a factor that is not a whole number (the L3 card): the fraction with the
# smallest denominator within TOLERANCE, relative, of the L2 double; above MAX_DEN it is refused
TOLERANCE = Fraction(1, 10 ** 14)
MAX_DEN = 10 ** 9

COUNT_NAMES = ('getallen', 'tekens aangevuld', 'percentages declaratie', 'percentages annotatie',
               'ingebouwde %', 'datums dag', 'datums milliseconde', 'eenheden', 'factoren geheel',
               'factoren gereconstrueerd', 'stelsels', 'stelseleenheden',
               'kaartsleutels als spelling', 'aliassen als spelling',
               'aliassen gelijk aan afkorting vervallen',
               'parameters', 'parameterfacetten', 'dagsoorten', 'registrysleutels gelijk aan naam',
               'dagsoortmeervouden')

_ASCII_LOWER = {c: c + 32 for c in range(ord('A'), ord('Z') + 1)}

# lexeme -> (num, den) or None; the search runs once per distinct lexeme
_RATIONAL = {}


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


def norm(s):
    # checks.ml norm: collapse_ws (split on space and tab, drop the empties), then lowercase_ascii
    return ' '.join(x for chunk in s.split(' ') for x in chunk.split('\t') if x).translate(_ASCII_LOWER)


def is_node(v, name):
    return isinstance(v, dict) and v.get('node') == name


def plain(v):
    if isinstance(v, RawNum):
        return ('num', v.raw)
    if isinstance(v, dict):
        return {k: plain(x) for k, x in v.items()}
    if isinstance(v, list):
        return [plain(x) for x in v]
    return v


def simplest_between(lo, hi):
    """The fraction with the smallest denominator in the closed interval [lo, hi], 0 < lo < hi:
       an integer if one lies in it, else the reciprocal step of the continued fraction."""
    fl = lo.numerator // lo.denominator
    if fl == lo:
        return Fraction(fl)
    if fl + 1 <= hi:
        return Fraction(fl + 1)
    return fl + 1 / simplest_between(1 / (hi - fl), 1 / (lo - fl))


def rational(lexeme):
    """(num, den) in lowest terms for a positive factor lexeme that is not a whole number, or
       None: the fraction with the smallest denominator within TOLERANCE, relative, of the
       lexeme's double, when that denominator is at most MAX_DEN."""
    if lexeme not in _RATIONAL:
        x = Fraction(float(lexeme))
        r = simplest_between(x * (1 - TOLERANCE), x * (1 + TOLERANCE))
        _RATIONAL[lexeme] = (r.numerator, r.denominator) if r.denominator <= MAX_DEN else None
    return _RATIONAL[lexeme]


def builtin_percent():
    return {'node': 'Unit', 'abbreviation': '%', 'canonical': '%',
            'factorToCanonical': {'num': RawNum('1'), 'den': RawNum('1')}}


class Migration:
    def __init__(self):
        self.errors = []
        self.counts = collections.Counter()
        self.denominators = collections.Counter()
        self.changed = False

    def fail(self, path, msg):
        self.errors.append((path, msg))

    def keys(self, v, p, expected, what):
        """True iff [v] carries exactly the L2 keys of [what]; each deviation is reported."""
        ok = True
        for k in v:
            if k not in expected:
                self.fail(p + ptr(k), 'unknown key %s on %s: the L3 shape has no place for it'
                          % (json.dumps(k), what))
                ok = False
        for k in expected:
            if k not in v:
                self.fail(p, '%s without its key %s' % (what, json.dumps(k)))
                ok = False
        return ok

    # --- the walk -------------------------------------------------------------------------------
    def walk(self, v, p, ctx):
        """[ctx] is None outside a datatype, 'decl' inside a datatype entered from a declaration
           position, 'annot' inside a datatype entered from a type annotation."""
        if isinstance(v, list):
            for i, x in enumerate(v):
                v[i] = self.walk(x, p + ptr(i), ctx)
            return v
        if not isinstance(v, dict):
            return v
        node = v.get('node')
        if node == 'Numeriek':
            return self.number(v, p, ctx)
        if node == 'Percentage':
            return self.percentage(v, p, ctx)
        if node in DATE_PRECISION:
            return self.date(v, p, ctx)
        if node == 'Eenheid':
            return self.unit(v, p)
        if node == 'Eenheidstelsel':
            return self.unit_system(v, p)
        if node == 'Parameter' and ('naam' in v or 'granulariteit' in v):
            return self.parameter(v, p)
        for k in list(v.keys()):
            v[k] = self.walk(v[k], p + ptr(k), self.child_ctx(ctx, k))
        return v

    @staticmethod
    def child_ctx(ctx, key):
        # inside a datatype every nested datatype shares its position; outside, the key decides
        if ctx is not None:
            return ctx
        if key == 'type':
            return 'annot'
        if key in ('datatype', 'result'):
            return 'decl'
        return None

    def position(self, p, ctx, what):
        if ctx is None:
            self.fail(p, '%s outside a declaration and outside a type annotation: no position '
                      'decides whether it carries the presentation tag' % what)
            return False
        return True

    # --- Number, Date ---------------------------------------------------------------------------
    def number(self, v, p, ctx):
        if not self.keys(v, p, NUMERIEK_KEYS, 'a number datatype'):
            return v
        self.changed = True
        self.counts['getallen'] += 1
        sign = v['teken']
        if sign is None:
            sign = 'ONBEPAALD'
            self.counts['tekens aangevuld'] += 1
        out = {'node': 'Number', 'whole': v['geheel'], 'decimals': v['decimalen'], 'sign': sign}
        if v['eenheid'] is not None:
            unit = self.unit(v['eenheid'], p + ptr('eenheid'))
            if unit is not None:
                out['unit'] = unit
        return out

    def percentage(self, v, p, ctx):
        if not self.keys(v, p, PERCENTAGE_KEYS, 'a percentage datatype'):
            return v
        if not self.position(p, ctx, 'a percentage datatype'):
            return v
        self.changed = True
        e = v['eenheid']
        if e is None:
            unit = builtin_percent()
            self.counts['ingebouwde %'] += 1
        elif is_node(e, 'Eenheid') and e.get('afkorting') == '%':
            unit = self.unit(e, p + ptr('eenheid'))
        else:
            self.fail(p + ptr('eenheid'), "a percentage whose unit is %s, not '%%': the L3 shape "
                      "writes a percentage as a Number with the unit '%%'"
                      % (json.dumps(e.get('afkorting')) if isinstance(e, dict) else 'not a unit'))
            return v
        out = {'node': 'Number', 'whole': v['geheel'], 'decimals': v['decimalen'],
               'sign': 'ONBEPAALD'}
        if unit is not None:
            out['unit'] = unit
        if ctx == 'decl':
            out['presentation'] = 'percentage'
            self.counts['percentages declaratie'] += 1
        else:
            self.counts['percentages annotatie'] += 1
        return out

    def date(self, v, p, ctx):
        if not self.keys(v, p, ('node',), 'a date datatype'):
            return v
        self.changed = True
        precision = DATE_PRECISION[v['node']]
        self.counts['datums dag' if precision == 'day' else 'datums milliseconde'] += 1
        return {'node': 'Date', 'precision': precision}

    # --- Unit and its factor --------------------------------------------------------------------
    def unit(self, e, p):
        if not is_node(e, 'Eenheid'):
            self.fail(p, 'not a unit node where the L2 shape carries one')
            return None
        if not self.keys(e, p, EENHEID_KEYS, 'a unit'):
            return None
        self.changed = True
        self.counts['eenheden'] += 1
        a = e['afkorting']
        if not isinstance(a, str):
            self.fail(p + ptr('afkorting'), 'a unit without an abbreviation: the L3 shape requires '
                      'one')
            return None
        out = {'node': 'Unit', 'abbreviation': a}
        for old, new in (('stelsel', 'system'), ('canoniek', 'canonical')):
            if e[old] is None:
                continue
            if not isinstance(e[old], str):
                self.fail(p + ptr(old), 'not a string: the L3 shape writes %s as a string' % new)
                return None
            out[new] = e[old]
        if e['factorNaarCanoniek'] is not None:
            factor = self.factor(e['factorNaarCanoniek'], p + ptr('factorNaarCanoniek'))
            if factor is None:
                return None
            out['factorToCanonical'] = factor
        if e['noemer'] is not None:
            den = self.unit(e['noemer'], p + ptr('noemer'))
            if den is None:
                return None
            out['denominator'] = den
        return out

    def factor(self, f, p):
        if not isinstance(f, RawNum):
            self.fail(p, 'a conversion factor that is not a JSON number')
            return None
        s = f.raw
        if re.fullmatch(r'[0-9]+', s):
            # a whole number: carried verbatim over 1
            if int(s) == 0:
                self.fail(p, 'the factor 0: a conversion factor is a positive rational')
                return None
            self.counts['factoren geheel'] += 1
            return {'num': RawNum(s), 'den': RawNum('1')}
        x = float(s)
        if not (math.isfinite(x) and x > 0):
            self.fail(p, 'the factor %s: a conversion factor is a positive rational' % s)
            return None
        r = rational(s)
        if r is None:
            self.fail(p, 'the factor %s: no fraction with a denominator of at most %d lies within '
                      '%s, relative, of its double, so the L3 shape cannot write it as an exact '
                      'rational' % (s, MAX_DEN, TOLERANCE))
            return None
        n, d = r
        self.counts['factoren gereconstrueerd'] += 1
        self.denominators[d] += 1
        return {'num': RawNum(str(n)), 'den': RawNum(str(d))}

    # --- UnitSystem -----------------------------------------------------------------------------
    def unit_system(self, v, p):
        if not self.keys(v, p, STELSEL_KEYS, 'a unit system'):
            return v
        name, units_map, aliases = v['naam'], v['eenheden'], v['aliassen']
        if not isinstance(name, str):
            self.fail(p + ptr('naam'), 'a unit system without a name')
            return v
        ok = True
        for k, m in (('eenheden', units_map), ('aliassen', aliases)):
            if not isinstance(m, dict):
                self.fail(p + ptr(k), 'not a map where the L2 shape carries one')
                ok = False
        if not ok:
            return v
        self.changed = True
        self.counts['stelsels'] += 1
        # abbreviation -> [unit, map keys that differ from it, alias keys], in map order
        units = collections.OrderedDict()
        for key, u in units_map.items():
            q = p + ptr('eenheden', key)
            if u is None:
                self.fail(q, 'a null unit in the map of a unit system: the L3 list has no place '
                          'for it')
                continue
            cu = self.unit(u, q)
            if cu is None:
                continue
            a = cu['abbreviation']
            if a in units:
                if plain(units[a][0]) != plain(cu):
                    self.fail(q, 'a second entry for the abbreviation %s with a different unit: '
                              'the L3 list writes each unit once' % json.dumps(a))
                    continue
            else:
                units[a] = [cu, [], []]
            if key != a:
                units[a][1].append(key)
                self.counts['kaartsleutels als spelling'] += 1
        for key, u in aliases.items():
            q = p + ptr('aliassen', key)
            if u is None:
                self.fail(q, 'a null alias: it names no unit to be a spelling of')
                continue
            cu = self.unit(u, q)
            if cu is None:
                continue
            a = cu['abbreviation']
            if a not in units:
                self.fail(q, 'an alias of the unit %s, which the system does not declare: the '
                          'spelling has no unit to go on' % json.dumps(a))
                continue
            if plain(units[a][0]) != plain(cu):
                self.fail(q, 'an alias whose unit differs from the declared unit %s: as a '
                          'spelling it would lose that difference' % json.dumps(a))
                continue
            if key == a:
                # a spelling is never the abbreviation itself: that is the unit's primary spelling
                self.counts['aliassen gelijk aan afkorting vervallen'] += 1
                continue
            units[a][2].append(key)
            self.counts['aliassen als spelling'] += 1
        out_units = []
        for a, (cu, keys, alias_keys) in units.items():
            spellings = keys + alias_keys
            dup = sorted({s for s in spellings if spellings.count(s) > 1})
            if dup:
                self.fail(p, 'the unit %s would carry the spelling(s) %s twice, from the map and '
                          'from the aliases' % (json.dumps(a), ', '.join(json.dumps(s) for s in dup)))
                continue
            if spellings:
                cu['spellings'] = spellings
            out_units.append(cu)
            self.counts['stelseleenheden'] += 1
        return {'node': 'UnitSystem', 'name': name, 'units': out_units}

    # --- Parameter ------------------------------------------------------------------------------
    def parameter(self, v, p):
        if not self.keys(v, p, PARAMETER_KEYS, 'a parameter'):
            return v
        if not isinstance(v['naam'], str):
            self.fail(p + ptr('naam'), 'a parameter without a name')
            return v
        if v['datatype'] is None:
            self.fail(p + ptr('datatype'), 'a parameter without a datatype: the L3 shape '
                      'requires one')
            return v
        self.changed = True
        self.counts['parameters'] += 1
        out = {'node': 'Parameter', 'name': v['naam'],
               'datatype': self.walk(v['datatype'], p + ptr('datatype'), 'decl')}
        if v['granulariteit'] is not None:
            out['timeline'] = {'node': 'TimelineFacet', 'granularity': v['granulariteit'],
                               'count': RawNum('1')}
            self.counts['parameterfacetten'] += 1
        return out

    # --- DayKind --------------------------------------------------------------------------------
    def day_kinds(self, model, p):
        names, registry = model.get('dayKinds'), model['dagsoortPerEenheid']
        if not isinstance(names, list):
            self.fail(p + ptr('dayKinds'), 'no list of day kinds beside the day-kind registry')
            return
        if not isinstance(registry, dict):
            self.fail(p + ptr('dagsoortPerEenheid'), 'the day-kind registry is not a map')
            return
        ok = True
        for i, n in enumerate(names):
            if not isinstance(n, str):
                self.fail(p + ptr('dayKinds', i), 'a day kind without a name')
                ok = False
        if not ok:
            return
        further = [[] for _ in names]
        for key, val in registry.items():
            q = p + ptr('dagsoortPerEenheid', key)
            if not isinstance(val, str):
                self.fail(q, 'a registry entry that names no day kind: the L3 shape has no '
                          'registry to carry it')
                continue
            cands = [i for i, n in enumerate(names) if norm(n) == norm(val)]
            if len(cands) != 1:
                self.fail(q, 'the registry entry names the day kind %s, which %s: the L3 shape has '
                          'no registry, so a spelling is written on its own day kind or not at all'
                          % (json.dumps(val), 'no declaration carries' if not cands
                             else '%d declarations carry' % len(cands)))
                continue
            i = cands[0]
            if norm(key) == norm(names[i]):
                self.counts['registrysleutels gelijk aan naam'] += 1
            else:
                further[i].append((key, q))
        out = []
        for i, n in enumerate(names):
            node = {'node': 'DayKind', 'name': n}
            if len(further[i]) > 1:
                self.fail(further[i][1][1], 'a third spelling of the day kind %s beside its name '
                          'and the plural %s: a DayKind carries its name and one plural'
                          % (json.dumps(n), json.dumps(further[i][0][0])))
            elif further[i]:
                node['plural'] = further[i][0][0]
                self.counts['dagsoortmeervouden'] += 1
            out.append(node)
            self.counts['dagsoorten'] += 1
        model['dayKinds'] = out
        del model['dagsoortPerEenheid']
        self.changed = True

    def run(self, doc):
        for k in ('model', 'rules', 'diagnostics'):
            if k not in doc:
                self.fail(ptr(k), 'root key of the L2 shape absent')
        if self.errors:
            return
        model = doc['model']
        if not isinstance(model, dict):
            self.fail(ptr('model'), 'the model is not an object')
            return
        if 'dagsoortPerEenheid' in model:
            self.day_kinds(model, ptr('model'))
        self.walk(model, ptr('model'), None)
        self.walk(doc['rules'], ptr('rules'), None)


def l2_forms(v, p=''):
    """Every L2 form left in [v], as (pointer, what): the walk's own completeness check, so a
       position the walk did not reach fails loudly instead of passing through."""
    out = []
    if isinstance(v, list):
        for i, x in enumerate(v):
            out += l2_forms(x, p + ptr(i))
    elif isinstance(v, dict):
        node = v.get('node')
        if node in L2_NODES:
            out.append((p, 'node %s' % node))
        elif node == 'Parameter':
            for k in ('naam', 'granulariteit'):
                if k in v:
                    out.append((p + ptr(k), 'the L2 field %s' % k))
        elif node == 'Unit' and 'factorToCanonical' in v and not isinstance(v['factorToCanonical'],
                                                                              dict):
            out.append((p + ptr('factorToCanonical'), 'a factor that is not a rational'))
        for k, x in v.items():
            out += l2_forms(x, p + ptr(k))
    return out


def model_forms(doc):
    model = doc.get('model') if isinstance(doc.get('model'), dict) else {}
    out = []
    if 'dagsoortPerEenheid' in model:
        out.append((ptr('model', 'dagsoortPerEenheid'), 'the day-kind registry'))
    for i, d in enumerate(model.get('dayKinds') or []):
        if not is_node(d, 'DayKind'):
            out.append((ptr('model', 'dayKinds', i), 'a day kind that is not a DayKind node'))
    return out


def migrate(raw):
    """(new bytes | None when already in the L3 shape, [(json pointer, message)], migration)."""
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
    m.run(doc)
    if m.errors:
        return None, m.errors, None
    left = l2_forms(doc) + model_forms(doc)
    if left:
        return None, [(q, 'internal: %s survived the walk' % what) for q, what in left], None
    if not m.changed:
        return None, [], m
    out = (ser(doc, 0) + '\n').encode('utf-8')
    # the written bytes must read back as exactly the document built, in gate 2's canonical form
    back = parse(out)
    if plain(back) != plain(doc) or (ser(back, 0) + '\n').encode('utf-8') != out:
        return None, [('', 'internal: the serialised document does not read back as built')], None
    return out, [], m


def migrate_bytes(raw):
    """(new bytes | None when already in the L3 shape, [(json pointer, message)]), as m2.py."""
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
    line = ', '.join('%s %d' % (k, m.counts[k]) for k in COUNT_NAMES)
    if m.denominators:
        line += ', noemers %s' % ' '.join('%d:%d' % (d, n) for d, n in sorted(m.denominators.items()))
    return line


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
            print('L2 (refused): %s at %s: %s' % (show(f), path or '<root>', msg))
        elif out is not None:
            left += 1
            print('L2: %s' % show(f))
    print('m3 --check: %d of %d document(s) not in the L3 shape' % (left, len(files)))
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
        out, errors, m = migrate(raw)
        if errors:
            failed.append((f, errors))
            continue
        if out is None:
            kept += 1
            continue
        tmp = f + '.m3tmp'
        with open(tmp, 'wb') as fh:
            fh.write(out)
        os.replace(tmp, f)
        migrated += 1
        sys.stderr.write('m3: %s: %s\n' % (show(f), count_line(m)))
    for f, errors in failed:
        sys.stderr.write('FAIL: %s\n' % show(f))
        for path, msg in errors:
            sys.stderr.write('  at %s: %s\n' % (path or '<root>', msg))
    print('m3: %d migrated, %d already in the L3 shape and left untouched, %d refused (left in '
          'the L2 shape)' % (migrated, kept, len(failed)))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
