#!/usr/bin/env python3
"""L4 of the v1 wire migration: uchar-ir-v1 from its L3 shape to its L4 shape (M5 + M6).

    m4.py FILE...             migrate the named ir.json documents in place
    m4.py --all               migrate every corpus/**/ir.json and toka/**/ir.json
    m4.py --check [FILE...]   list every document that is not yet in the L4 shape
                              (default: the --all set); exit 1 if there is one

The target shape is the one the L4 card (20260924-l4-regel-en-expressielaag) fixes under
"De L4-vorm van de wire", and spec/schema/uchar-ir-v1.schema.json types it. L4 rewrites the
RULE LAYER and the EXPRESSION LAYER and nothing else; every other node, field and value
spelling is carried verbatim, in its own key order. The L4 nodes and version records are
written in the schema's 'properties' order.

The rule layer (M5):

- The six named forms become {node, name, versions}: ResolvedRegel a Rule, ResolvedVerdeling
  an Allocation, ResolvedObjectCreatie an ObjectCreation, ResolvedFeitCreatie a FactCreation,
  ResolvedConsistentie a Consistency, and ResolvedUniciteit and ResolvedUniciteitUnie a
  Uniqueness. A version record carries no node. The lists keep their v0 keys; the uniqueness
  forms of the v0 lists uniciteiten and uniciteitUnies share the one list uniciteiten, the
  single-source forms first, then the unions, each in v0 order, and uniciteitUnies is gone.
- Versions are grouped back. Entries of one v0 list whose name ends in " #<i>" and whose names
  are byte-equal after stripping that suffix are one form with the versions in the order of i;
  an entry without the suffix is a form with one version, also beside an entry with the same
  bare name (WF-020 then fires as before). The document fails when the suffixes of a group are
  not exactly " #1" to " #n", when its entries are not contiguous, when a group has one entry,
  when a stripped name still ends in " #<digits>" (a form name the L4 shape cannot write), or
  when the versions of a rule carry different bronAnnotaties. A form stands where its first
  version stood; its name is the stripped name, or the entry's name (null included, WF-107).
- The rule kind soort becomes the flag and the tag: GELIJKSTELLING and BEREKENING are
  initialisation false with that presentation; INITIALISATIE is initialisation true without
  one; KENMERKTOEKENNING is initialisation false without one (the target is a characteristic,
  which the declaration says). A null or unknown kind fails the document.
- A rule's doel and waarde become target and value; a target that is not an AttribuutRef, and a
  null value, fail the document. bronAnnotaties becomes the rule's statuteAnnotations, verbatim
  from the first version (also an ill-formed list: WF-082 is the verifier's).
- On every version: voorwaarde becomes condition, a null one left out; geldigheid becomes
  validity, and a null one is written {"node": "Geldigheid", "vanaf": null, "tot": null}.
- An allocation's fields take their IR-SPEC names (sourceType, sourceAttribute, targetType,
  targetAttribute, method, ratioAttribute, maximumAttribute, roundingDecimals,
  remainderAttribute, orderingAttribute, descending, connectingRole); a null optional field is
  left out and an empty string stays (WF-034's). A method COMPLEX or null fails the document.
- An object creation's nieuwType, toekenningen and verbindendeRol become createdType,
  initialisations and connectingRole; each Toekenning stays its v0 node.
- A fact creation's feittype, ankerRol, doelRol and anker become factType, anchorRole,
  targetRole and anchor; its tussenstappen and joinRol become one path, the rolBinding of each
  Tussenstap in order and then the joinRol. Its doel is dropped: the document fails unless it
  equals the declared filler of doelRol in the fact type feittype names.
- A consistency's assertie becomes assertion (null fails); bron and line stay on the version.
- A single uniqueness becomes one version with the one source {objectType: subject, keys:
  sleutels} and its scopeRol as scope; a union becomes one version with a source per Bron {type,
  sleutels}. An empty key list or source list fails the document.

The expression layer (M6), everywhere an expression stands (the rule layer, the defaults layer
and the bodies of model.functionDefs):

- Functie splits by kind: SOM, TOTAAL, AANTAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE and
  AANTAL_GEVULDE become an Aggregation {kind, arguments, elementType?, filter?, emptyFallback?,
  dimensionSelection?, sourceRole?, sourcePath?, type, bron}, the seventeen others a BuiltinCall
  {kind, arguments, filter?, granularity?, type, bron}; a null field is left out. An aggregation
  with a granulariteit, and a builtin with an element type, empty value, dimension selection or
  range, fail the document: the L4 node has no place for the field.
- A Literal's lexeme waarde becomes its typed value, the variant chosen by its type: a Number
  (with or without a unit) {"number"} with every ',' written '.', a Booleaan {"boolean"} from
  waar/onwaar, a Tekst {"text"}, a Date of day precision {"date"} and of millisecond precision
  {"dateTime"}, an Enumeratie {"enumeration"}, a SumType {"constructor"}, each string carried
  verbatim otherwise; bron keeps the author's spelling. A null lexeme, a null type (no variant
  to choose), any other type, and a boolean lexeme other than waar/onwaar fail the document.
- A Constante named "de dag" becomes a BoundDay {type, bron}; any other name a ContextValue
  {name, type, bron} with that name verbatim. A null name or type fails the document.

Loud by construction: an L3 value the L4 shape cannot carry fails the document with its file
name and the JSON pointer into the L3 input; nothing is repaired. That covers every case named
above and an unknown or missing key on a node this script rewrites. A failing document is left
untouched. A document already in the L4 shape is left untouched as well, byte for byte, so a
second run changes nothing; a document mixing the two shapes fails. Every migrated document
gets one line on stderr counting what was rewritten.

This script reads the L3 shape, so it names the L3 fields it removes; no L4 artifact carries
those names.

Output is written with gate 2's own serialiser, executed from scripts/check-ir-schema.sh
exactly as m1.py, m2.py and m3.py do, so a migrated document round-trips byte-identically under
gate 2.
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

# the keys of the L3 nodes this script rewrites, in their L3 order; OPTIONAL are the extension
# fields a node may carry beside them
REGEL_KEYS = ('node', 'naam', 'soort', 'doel', 'waarde', 'voorwaarde', 'geldigheid')
REGEL_OPTIONAL = ('bronAnnotaties',)
VERDELING_KEYS = ('node', 'naam', 'bronType', 'bronAttribuut', 'doelType', 'doelAttribuut',
                  'methode', 'ratoAttribuut', 'maximumAttribuut', 'afrondingDecimalen',
                  'restantAttribuut', 'groepeerAttribuut', 'afnemend', 'voorwaarde', 'geldigheid',
                  'verbindendeRol')
OBJECTCREATIE_KEYS = ('node', 'naam', 'nieuwType', 'subject', 'toekenningen', 'voorwaarde',
                      'geldigheid')
OBJECTCREATIE_OPTIONAL = ('verbindendeRol',)
FEITCREATIE_KEYS = ('node', 'naam', 'anker', 'doel', 'tussenstappen', 'voorwaarde', 'geldigheid',
                    'feittype', 'ankerRol', 'doelRol', 'joinRol')
TUSSENSTAP_KEYS = ('node', 'rolBinding')
CONSISTENTIE_KEYS = ('node', 'naam', 'subject', 'assertie', 'bron', 'line', 'geldigheid')
UNICITEIT_KEYS = ('node', 'naam', 'subject', 'sleutels', 'bron', 'geldigheid')
UNICITEIT_OPTIONAL = ('scopeRol',)
UNIE_KEYS = ('node', 'naam', 'bronnen', 'bron', 'geldigheid')
BRON_KEYS = ('node', 'type', 'sleutels')
TOEKENNING_KEYS = ('node', 'attribuut', 'waarde', 'kenmerk')
FUNCTIE_KEYS = ('node', 'kind', 'argumenten', 'elementObjecttype', 'leegwaarde', 'filter',
                'dimensieSelectie', 'type', 'bron', 'granulariteit')
FUNCTIE_OPTIONAL = ('bronRol', 'bronPad')
LITERAL_KEYS = ('node', 'waarde', 'type', 'bron')
CONSTANTE_KEYS = ('node', 'naam', 'type', 'bron')

# the L3 rules container, in its order, and the L4 one
L3_RULES = ('resolvedRegels', 'resolvedDagsoorten', 'objectCreaties', 'verdelingen',
            'consistenties', 'uniciteiten', 'uniciteitUnies', 'feitCreaties',
            'resolvedStartpunten', 'regelGroepen')
L4_RULES = tuple(k for k in L3_RULES if k != 'uniciteitUnies')
RULES_OPTIONAL = ('prioritizedDefaults',)

AGGREGATION_KINDS = ('SOM', 'TOTAAL', 'AANTAL', 'MINIMUM', 'MAXIMUM', 'EERSTE', 'LAATSTE',
                     'AANTAL_GEVULDE')
BUILTIN_KINDS = ('WORTEL', 'ABSOLUTE_WAARDE', 'CONCATENATIE', 'TIJDSDUUR', 'ABSOLUTE_TIJDSDUUR',
                 'EERSTE_PAASDAG', 'TIJDSEVENREDIG_DEEL', 'AANTAL_DAGEN_IN', 'JAAR_UIT',
                 'MAAND_UIT', 'DAG_UIT', 'UUR_UIT', 'MINUUT_UIT', 'SECONDE_UIT',
                 'MILLISECONDE_UIT', 'DATUM_MET', 'DATUMTIJD_MET')
GRANULARITIES = ('DAG', 'WEEK', 'MAAND', 'KWARTAAL', 'JAAR')
METHODS = ('GELIJKE_DELEN', 'NAAR_RATO', 'OP_VOLGORDE')
# soort -> (initialisation, presentation or None)
SOORT = {'GELIJKSTELLING': (False, 'GELIJKSTELLING'), 'BEREKENING': (False, 'BEREKENING'),
         'INITIALISATIE': (True, None), 'KENMERKTOEKENNING': (False, None)}
BOOLEAN = {'waar': True, 'onwaar': False}

# every L3 node the L4 shape does not carry; a Literal carrying 'waarde' is the seventh form
L3_NODES = ('ResolvedRegel', 'ResolvedVerdeling', 'ResolvedObjectCreatie', 'ResolvedFeitCreatie',
            'Tussenstap', 'ResolvedConsistentie', 'ResolvedUniciteit', 'ResolvedUniciteitUnie',
            'Bron', 'Functie', 'Constante')

# a version's name on the v0 wire: the form name, a space, '#' and the version number
SUFFIX = re.compile(r'([\s\S]*) #([0-9]+)')

COUNT_NAMES = ('regelinvoeren', 'regels', 'versiegroepen', 'versies in groepen',
               'berekening', 'gelijkstelling', 'initialisatie', 'kenmerktoekenning',
               'annotaties', 'geldigheid null', 'voorwaarde null weg',
               'verdelingen', 'verdelingvelden null weg', 'objectcreaties', 'feitcreaties',
               'padstappen', 'consistenties', 'uniciteiten enkelvoudig', 'uniciteiten unie',
               'bronnen', 'aggregaties', 'builtins', 'number', 'boolean', 'text', 'date',
               'dateTime', 'enumeration', 'constructor', 'komma naar punt', 'gebonden dagen',
               'contextwaarden')


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


def always():
    return {'node': 'Geldigheid', 'vanaf': None, 'tot': None}


class Migration:
    def __init__(self):
        self.errors = []
        self.counts = collections.Counter()
        self.changed = False
        self.fact_types = {}

    def fail(self, path, msg):
        self.errors.append((path, msg))

    def keys(self, v, p, expected, what, optional=()):
        """True iff [v] carries exactly the L3 keys of [what]; each deviation is reported."""
        ok = True
        for k in v:
            if k not in expected and k not in optional:
                self.fail(p + ptr(k), 'unknown key %s on %s: the L4 shape has no place for it'
                          % (json.dumps(k), what))
                ok = False
        for k in expected:
            if k not in v:
                self.fail(p, '%s without its key %s' % (what, json.dumps(k)))
                ok = False
        return ok

    # --- the expression walk ------------------------------------------------------------------
    def walk(self, v, p):
        if isinstance(v, list):
            for i, x in enumerate(v):
                v[i] = self.walk(x, p + ptr(i))
            return v
        if not isinstance(v, dict):
            return v
        node = v.get('node')
        if node == 'Functie':
            return self.functie(v, p)
        if node == 'Literal':
            return self.literal(v, p)
        if node == 'Constante':
            return self.constante(v, p)
        if node in L3_NODES:
            self.fail(p, 'the rule-layer node %s where no named form stands: the L4 shape writes '
                      'it only in its own rules list' % node)
            return v
        for k in list(v.keys()):
            v[k] = self.walk(v[k], p + ptr(k))
        return v

    def optional_expr(self, v, p):
        return None if v is None else self.walk(v, p)

    def functie(self, v, p):
        if not self.keys(v, p, FUNCTIE_KEYS, 'a function node', FUNCTIE_OPTIONAL):
            return v
        kind = v['kind']
        for k in FUNCTIE_OPTIONAL:
            if k in v and v[k] is None:
                self.fail(p + ptr(k), 'a null %s: a range is written whole or not at all' % k)
                return v
        args = v['argumenten']
        if not isinstance(args, list):
            self.fail(p + ptr('argumenten'), 'the arguments are not a list')
            return v
        if kind in AGGREGATION_KINDS:
            if v['granulariteit'] is not None:
                self.fail(p + ptr('granulariteit'), 'an aggregation (%s) with the bucket %s: the L4 '
                          'shape writes granularity on a builtin call only' % (kind, json.dumps(
                              v['granulariteit'])))
                return v
            el = v['elementObjecttype']
            if el is not None and not isinstance(el, str):
                self.fail(p + ptr('elementObjecttype'), 'an element type that is not a string')
                return v
            self.changed = True
            self.counts['aggregaties'] += 1
            out = {'node': 'Aggregation', 'kind': kind,
                   'arguments': self.walk(args, p + ptr('argumenten'))}
            if el is not None:
                out['elementType'] = el
            for old, new in (('filter', 'filter'), ('leegwaarde', 'emptyFallback'),
                             ('dimensieSelectie', 'dimensionSelection')):
                if v[old] is not None:
                    out[new] = self.walk(v[old], p + ptr(old))
            if 'bronRol' in v:
                out['sourceRole'] = self.walk(v['bronRol'], p + ptr('bronRol'))
            if 'bronPad' in v:
                out['sourcePath'] = self.walk(v['bronPad'], p + ptr('bronPad'))
            out['type'] = v['type']
            out['bron'] = v['bron']
            return out
        if kind in BUILTIN_KINDS:
            wrong = [k for k in ('elementObjecttype', 'leegwaarde', 'dimensieSelectie')
                     if v[k] is not None] + [k for k in FUNCTIE_OPTIONAL if k in v]
            for k in wrong:
                self.fail(p + ptr(k), 'a builtin call (%s) with %s: the L4 shape writes that '
                          'field on an aggregation only' % (kind, k))
            if wrong:
                return v
            g = v['granulariteit']
            if g is not None and g not in GRANULARITIES:
                self.fail(p + ptr('granulariteit'), 'unknown granularity %s' % json.dumps(g))
                return v
            self.changed = True
            self.counts['builtins'] += 1
            out = {'node': 'BuiltinCall', 'kind': kind,
                   'arguments': self.walk(args, p + ptr('argumenten'))}
            if v['filter'] is not None:
                out['filter'] = self.walk(v['filter'], p + ptr('filter'))
            if g is not None:
                out['granularity'] = g
            out['type'] = v['type']
            out['bron'] = v['bron']
            return out
        self.fail(p + ptr('kind'), 'unknown function kind %s: neither an aggregation nor a '
                  'builtin call' % json.dumps(kind))
        return v

    def literal(self, v, p):
        if not self.keys(v, p, LITERAL_KEYS, 'a literal'):
            return v
        lexeme, t = v['waarde'], v['type']
        if lexeme is None:
            self.fail(p + ptr('waarde'), 'a literal without a lexeme: the L4 value is always one '
                      'variant with a string or a boolean')
            return v
        if not isinstance(lexeme, str):
            self.fail(p + ptr('waarde'), 'a lexeme that is not a string')
            return v
        if t is None:
            self.fail(p + ptr('type'), 'a typeless literal: the L4 shape chooses the value '
                      'variant by the type, and there is none')
            return v
        tn = t.get('node') if isinstance(t, dict) else None
        if tn == 'Number':
            if ',' in lexeme:
                self.counts['komma naar punt'] += 1
            variant, value = 'number', lexeme.replace(',', '.')
        elif tn == 'Booleaan':
            if lexeme not in BOOLEAN:
                self.fail(p + ptr('waarde'), 'the boolean lexeme %s: only waar and onwaar are a '
                          'boolean value' % json.dumps(lexeme))
                return v
            variant, value = 'boolean', BOOLEAN[lexeme]
        elif tn == 'Tekst':
            variant, value = 'text', lexeme
        elif tn == 'Date' and t.get('precision') == 'day':
            variant, value = 'date', lexeme
        elif tn == 'Date' and t.get('precision') == 'millisecond':
            variant, value = 'dateTime', lexeme
        elif tn == 'Enumeratie':
            variant, value = 'enumeration', lexeme
        elif tn == 'SumType':
            variant, value = 'constructor', lexeme
        else:
            self.fail(p + ptr('type'), 'a literal of type %s: no value variant of the L4 shape '
                      'carries it' % (json.dumps(tn) if tn else 'without a datatype node'))
            return v
        self.changed = True
        self.counts[variant] += 1
        return {'node': 'Literal', 'value': {variant: value}, 'type': t, 'bron': v['bron']}

    def constante(self, v, p):
        if not self.keys(v, p, CONSTANTE_KEYS, 'a context constant'):
            return v
        name, t = v['naam'], v['type']
        if not isinstance(name, str):
            self.fail(p + ptr('naam'), 'a context constant without a name: the L4 shape writes '
                      'the bound day or a named context value')
            return v
        if not isinstance(t, dict):
            self.fail(p + ptr('type'), 'a context constant without a type: the L4 shape requires '
                      'one')
            return v
        self.changed = True
        if name == 'de dag':
            self.counts['gebonden dagen'] += 1
            return {'node': 'BoundDay', 'type': t, 'bron': v['bron']}
        self.counts['contextwaarden'] += 1
        return {'node': 'ContextValue', 'name': name, 'type': t, 'bron': v['bron']}

    # --- the shared version fields --------------------------------------------------------------
    def validity(self, g, p):
        if g is None:
            self.counts['geldigheid null'] += 1
            return always()
        if not is_node(g, 'Geldigheid'):
            self.fail(p, 'a validity that is not a Geldigheid node')
            return None
        return g

    def condition(self, out, c, p):
        if c is None:
            self.counts['voorwaarde null weg'] += 1
        else:
            out['condition'] = self.walk(c, p)

    def string(self, v, p, what):
        if not isinstance(v, str):
            self.fail(p, '%s that is not a string: the L4 shape requires one' % what)
            return False
        return True

    def binding(self, v, p, what):
        if not is_node(v, 'RolBinding'):
            self.fail(p, '%s that is not a RolBinding' % what)
            return False
        return True

    # --- the six named forms: each returns (name, version, annotations) or None -----------------
    def regel(self, v, p):
        if not self.keys(v, p, REGEL_KEYS, 'a rule entry', REGEL_OPTIONAL):
            return None
        soort = v['soort']
        if soort not in SOORT:
            self.fail(p + ptr('soort'), 'the rule kind %s: the L4 shape writes only the four v0 '
                      'kinds as initialisation and presentation' % json.dumps(soort))
            return None
        if not is_node(v['doel'], 'AttribuutRef'):
            self.fail(p + ptr('doel'), 'a rule target that is %s: the L4 target is an attribute '
                      'reference' % ('null' if v['doel'] is None else 'no AttribuutRef'))
            return None
        if v['waarde'] is None:
            self.fail(p + ptr('waarde'), 'a rule without a value: the L4 value is required')
            return None
        validity = self.validity(v['geldigheid'], p + ptr('geldigheid'))
        if validity is None:
            return None
        initialisation, presentation = SOORT[soort]
        self.counts[soort.lower()] += 1
        out = {'validity': validity, 'target': self.walk(v['doel'], p + ptr('doel')),
               'value': self.walk(v['waarde'], p + ptr('waarde'))}
        self.condition(out, v['voorwaarde'], p + ptr('voorwaarde'))
        out['initialisation'] = initialisation
        if presentation is not None:
            out['presentation'] = presentation
        self.counts['regelinvoeren'] += 1
        return v['naam'], out, v.get('bronAnnotaties')

    def verdeling(self, v, p):
        if not self.keys(v, p, VERDELING_KEYS, 'an allocation entry'):
            return None
        method = v['methode']
        if method not in METHODS:
            self.fail(p + ptr('methode'), 'the method %s: the L4 method is one of the three real '
                      'methods, so the v0 placeholder of an unhandled form cannot be written'
                      % json.dumps(method))
            return None
        if not isinstance(v['afnemend'], bool):
            self.fail(p + ptr('afnemend'), 'descending that is not a boolean')
            return None
        d = v['afrondingDecimalen']
        if d is not None and not (isinstance(d, RawNum) and re.fullmatch(r'-?[0-9]+', d.raw)):
            self.fail(p + ptr('afrondingDecimalen'), 'rounding decimals that are not an integer')
            return None
        if not self.binding(v['verbindendeRol'], p + ptr('verbindendeRol'), 'a connecting role'):
            return None
        validity = self.validity(v['geldigheid'], p + ptr('geldigheid'))
        if validity is None:
            return None
        out = {'sourceType': v['bronType'], 'sourceAttribute': v['bronAttribuut'],
               'targetType': v['doelType'], 'targetAttribute': v['doelAttribuut'],
               'method': method}
        for old, new in (('ratoAttribuut', 'ratioAttribute'), ('maximumAttribuut', 'maximumAttribute'),
                         ('afrondingDecimalen', 'roundingDecimals'),
                         ('restantAttribuut', 'remainderAttribute'),
                         ('groepeerAttribuut', 'orderingAttribute')):
            if v[old] is None:
                self.counts['verdelingvelden null weg'] += 1
            else:
                out[new] = v[old]
        out['descending'] = v['afnemend']
        self.condition(out, v['voorwaarde'], p + ptr('voorwaarde'))
        out['validity'] = validity
        out['connectingRole'] = self.walk(v['verbindendeRol'], p + ptr('verbindendeRol'))
        self.counts['verdelingen'] += 1
        return v['naam'], out, None

    def objectcreatie(self, v, p):
        if not self.keys(v, p, OBJECTCREATIE_KEYS, 'an object-creation entry',
                         OBJECTCREATIE_OPTIONAL):
            return None
        inits = v['toekenningen']
        if not isinstance(inits, list):
            self.fail(p + ptr('toekenningen'), 'the initialisations are not a list')
            return None
        for i, t in enumerate(inits):
            q = p + ptr('toekenningen', i)
            if not is_node(t, 'Toekenning'):
                self.fail(q, 'an initialisation that is not a Toekenning')
                return None
            if not self.keys(t, q, TOEKENNING_KEYS, 'an initialisation'):
                return None
        if 'verbindendeRol' in v and not self.binding(v['verbindendeRol'],
                                                      p + ptr('verbindendeRol'),
                                                      'a connecting role'):
            return None
        validity = self.validity(v['geldigheid'], p + ptr('geldigheid'))
        if validity is None:
            return None
        out = {'createdType': v['nieuwType'], 'subject': v['subject'],
               'initialisations': self.walk(inits, p + ptr('toekenningen'))}
        self.condition(out, v['voorwaarde'], p + ptr('voorwaarde'))
        out['validity'] = validity
        if 'verbindendeRol' in v:
            out['connectingRole'] = self.walk(v['verbindendeRol'], p + ptr('verbindendeRol'))
        self.counts['objectcreaties'] += 1
        return v['naam'], out, None

    def feitcreatie(self, v, p):
        if not self.keys(v, p, FEITCREATIE_KEYS, 'a fact-creation entry'):
            return None
        for k in ('feittype', 'ankerRol', 'doelRol'):
            if not self.string(v[k], p + ptr(k), 'the %s' % k):
                return None
        steps = v['tussenstappen']
        if not isinstance(steps, list):
            self.fail(p + ptr('tussenstappen'), 'the steps are not a list')
            return None
        path = []
        for i, s in enumerate(steps):
            q = p + ptr('tussenstappen', i)
            if not is_node(s, 'Tussenstap'):
                self.fail(q, 'a step that is not a Tussenstap')
                return None
            if not self.keys(s, q, TUSSENSTAP_KEYS, 'a step'):
                return None
            if not self.binding(s['rolBinding'], q + ptr('rolBinding'), 'a step binding'):
                return None
            path.append(s['rolBinding'])
        if not self.binding(v['joinRol'], p + ptr('joinRol'), 'the joining hop'):
            return None
        path.append(v['joinRol'])
        # the dropped target type must be the declared filler of the target role
        fts = self.fact_types.get(v['feittype'], [])
        fillers = [r.get('objecttypeNaam') for ft in fts for r in (ft.get('rollen') or [])
                   if isinstance(r, dict) and r.get('naam') == v['doelRol']]
        if len(fts) != 1 or len(fillers) != 1:
            self.fail(p + ptr('doel'), 'the target type cannot be checked against the fact type: '
                      '%d fact type(s) named %s, %d role(s) named %s in it'
                      % (len(fts), json.dumps(v['feittype']), len(fillers),
                         json.dumps(v['doelRol'])))
            return None
        if fillers[0] != v['doel']:
            self.fail(p + ptr('doel'), 'the target type %s is not the declared filler %s of the '
                      'target role %s: the L4 shape drops it and reads the filler'
                      % (json.dumps(v['doel']), json.dumps(fillers[0]), json.dumps(v['doelRol'])))
            return None
        validity = self.validity(v['geldigheid'], p + ptr('geldigheid'))
        if validity is None:
            return None
        out = {'factType': v['feittype'], 'anchorRole': v['ankerRol'], 'targetRole': v['doelRol'],
               'anchor': v['anker'], 'path': self.walk(path, p + ptr('tussenstappen'))}
        self.condition(out, v['voorwaarde'], p + ptr('voorwaarde'))
        out['validity'] = validity
        self.counts['feitcreaties'] += 1
        self.counts['padstappen'] += len(path)
        self.counts['pad %d' % len(path)] += 1
        return v['naam'], out, None

    def consistentie(self, v, p):
        if not self.keys(v, p, CONSISTENTIE_KEYS, 'a consistency entry'):
            return None
        if v['assertie'] is None:
            self.fail(p + ptr('assertie'), 'a consistency without an assertion: the L4 assertion '
                      'is required')
            return None
        validity = self.validity(v['geldigheid'], p + ptr('geldigheid'))
        if validity is None:
            return None
        out = {'subject': v['subject'], 'assertion': self.walk(v['assertie'], p + ptr('assertie')),
               'validity': validity, 'bron': v['bron'], 'line': v['line']}
        self.counts['consistenties'] += 1
        return v['naam'], out, None

    def keys_list(self, keys, p):
        if not isinstance(keys, list) or not keys:
            self.fail(p, 'an empty key list: a source has at least one key')
            return False
        return True

    def uniciteit(self, v, p):
        if not self.keys(v, p, UNICITEIT_KEYS, 'a uniqueness entry', UNICITEIT_OPTIONAL):
            return None
        if not self.keys_list(v['sleutels'], p + ptr('sleutels')):
            return None
        if 'scopeRol' in v and not self.binding(v['scopeRol'], p + ptr('scopeRol'), 'a scope'):
            return None
        validity = self.validity(v['geldigheid'], p + ptr('geldigheid'))
        if validity is None:
            return None
        out = {'sources': [{'node': 'Source', 'objectType': v['subject'], 'keys': v['sleutels']}]}
        if 'scopeRol' in v:
            out['scope'] = self.walk(v['scopeRol'], p + ptr('scopeRol'))
        out['validity'] = validity
        out['bron'] = v['bron']
        self.counts['uniciteiten enkelvoudig'] += 1
        self.counts['bronnen'] += 1
        return v['naam'], out, None

    def unie(self, v, p):
        if not self.keys(v, p, UNIE_KEYS, 'a uniqueness-union entry'):
            return None
        sources = v['bronnen']
        if not isinstance(sources, list) or not sources:
            self.fail(p + ptr('bronnen'), 'a union without sources: a uniqueness has at least '
                      'one')
            return None
        out_sources = []
        for i, b in enumerate(sources):
            q = p + ptr('bronnen', i)
            if not is_node(b, 'Bron'):
                self.fail(q, 'a union source that is not a Bron')
                return None
            if not self.keys(b, q, BRON_KEYS, 'a union source'):
                return None
            if not self.keys_list(b['sleutels'], q + ptr('sleutels')):
                return None
            out_sources.append({'node': 'Source', 'objectType': b['type'], 'keys': b['sleutels']})
        validity = self.validity(v['geldigheid'], p + ptr('geldigheid'))
        if validity is None:
            return None
        self.counts['uniciteiten unie'] += 1
        self.counts['bronnen'] += len(out_sources)
        return v['naam'], {'sources': out_sources, 'validity': validity, 'bron': v['bron']}, None

    # --- grouping the flattened versions back into forms ----------------------------------------
    def forms(self, lst, p, v0_node, convert, l4_node):
        """The L4 forms of one v0 list, in list order, or None when the list fails."""
        if not isinstance(lst, list):
            self.fail(p, 'not a list where the L3 shape carries one')
            return None
        entries = []
        ok = True
        for i, v in enumerate(lst):
            q = p + ptr(i)
            if not is_node(v, v0_node):
                self.fail(q, 'an entry that is not a %s node in its v0 list' % v0_node)
                ok = False
                continue
            name = v.get('naam')
            if name is not None and not isinstance(name, str):
                self.fail(q + ptr('naam'), 'a name that is not a string')
                ok = False
                continue
            r = convert(v, q)
            if r is None:
                ok = False
                continue
            m = SUFFIX.fullmatch(name) if isinstance(name, str) else None
            entries.append((i, name, m.group(1) if m else None, m.group(2) if m else None, r))
        if not ok:
            return None
        # the runs of entries: an entry without a suffix is a run of its own, and the suffixed
        # entries of one stripped name are one run, which must be the only one for that name
        runs = []
        k = 0
        while k < len(entries):
            base = entries[k][2]
            j = k + 1
            while base is not None and j < len(entries) and entries[j][2] == base:
                j += 1
            runs.append(entries[k:j])
            k = j
        ends = {}
        for run in runs:
            base = run[0][2]
            if base is None:
                continue
            if base in ends:
                self.fail(p + ptr(run[0][0], 'naam'), 'the versions of %s do not stand together: '
                          'another entry interrupts them after %s' % (json.dumps(base), ends[base]))
                return None
            ends[base] = p + ptr(run[-1][0])
        out = []
        for run in runs:
            i, name, base, _, (_, version, ann) = run[0]
            if base is None:
                out.append(self.form(l4_node, name, [version], ann))
                continue
            q = p + ptr(i, 'naam')
            if len(run) < 2:
                self.fail(q, 'the version suffix of %s stands on one entry: a form with one '
                          'version carries its bare name, so the L4 shape cannot write it'
                          % json.dumps(name))
                return None
            if sorted(e[3] for e in run) != sorted(str(n) for n in range(1, len(run) + 1)):
                self.fail(q, 'the version suffixes of %s are %s, not exactly 1 to %d'
                          % (json.dumps(base), ', '.join(e[3] for e in run), len(run)))
                return None
            if SUFFIX.fullmatch(base):
                self.fail(q, 'the form name %s itself ends in a version suffix: the L4 shape '
                          'cannot write it' % json.dumps(base))
                return None
            anns = [e[4][2] for e in run]
            for e, a in zip(run[1:], anns[1:]):
                if plain(a) != plain(anns[0]):
                    self.fail(p + ptr(e[0], 'bronAnnotaties'), 'the versions of %s carry different '
                              'statute annotations: the L4 shape writes one list on the form'
                              % json.dumps(base))
                    return None
            versions = [e[4][1] for e in sorted(run, key=lambda e: int(e[3]))]
            out.append(self.form(l4_node, base, versions, anns[0]))
            self.counts['versiegroepen'] += 1
            self.counts['versies in groepen'] += len(run)
        return out

    def form(self, node, name, versions, annotations):
        self.changed = True
        if node == 'Rule':
            self.counts['regels'] += 1
        f = {'node': node, 'name': name, 'versions': versions}
        if annotations is not None:
            f['statuteAnnotations'] = annotations
            self.counts['annotaties'] += 1
        return f

    # --- the document ---------------------------------------------------------------------------
    def run(self, doc):
        for k in ('model', 'rules', 'diagnostics'):
            if k not in doc:
                self.fail(ptr(k), 'root key of the L3 shape absent')
        if self.errors:
            return
        model, rules = doc['model'], doc['rules']
        if not isinstance(model, dict) or not isinstance(rules, dict):
            self.fail('', 'model or rules is not an object')
            return
        if 'uniciteitUnies' not in rules:
            # the L4 container: nothing to do, provided nothing of the L3 shape is left in it
            return
        self.keys(rules, ptr('rules'), L3_RULES, 'the rules container', RULES_OPTIONAL)
        if self.errors:
            return
        for ft in model.get('factTypes') or []:
            if isinstance(ft, dict) and isinstance(ft.get('naam'), str):
                self.fact_types.setdefault(ft['naam'], []).append(ft)
        self.walk(model, ptr('model'))
        r = ptr('rules')
        named = {
            'resolvedRegels': self.forms(rules['resolvedRegels'], r + ptr('resolvedRegels'),
                                         'ResolvedRegel', self.regel, 'Rule'),
            'objectCreaties': self.forms(rules['objectCreaties'], r + ptr('objectCreaties'),
                                         'ResolvedObjectCreatie', self.objectcreatie,
                                         'ObjectCreation'),
            'verdelingen': self.forms(rules['verdelingen'], r + ptr('verdelingen'),
                                      'ResolvedVerdeling', self.verdeling, 'Allocation'),
            'consistenties': self.forms(rules['consistenties'], r + ptr('consistenties'),
                                        'ResolvedConsistentie', self.consistentie, 'Consistency'),
            'feitCreaties': self.forms(rules['feitCreaties'], r + ptr('feitCreaties'),
                                       'ResolvedFeitCreatie', self.feitcreatie, 'FactCreation'),
        }
        single = self.forms(rules['uniciteiten'], r + ptr('uniciteiten'), 'ResolvedUniciteit',
                            self.uniciteit, 'Uniqueness')
        union = self.forms(rules['uniciteitUnies'], r + ptr('uniciteitUnies'),
                           'ResolvedUniciteitUnie', self.unie, 'Uniqueness')
        named['uniciteiten'] = None if single is None or union is None else single + union
        rest = {k: self.walk(rules[k], r + ptr(k))
                for k in ('resolvedDagsoorten', 'resolvedStartpunten', 'regelGroepen')}
        if 'prioritizedDefaults' in rules:
            rest['prioritizedDefaults'] = self.walk(rules['prioritizedDefaults'],
                                                    r + ptr('prioritizedDefaults'))
        if self.errors:
            return
        out = {}
        for k in L4_RULES:
            out[k] = named[k] if k in named else rest[k]
        if 'prioritizedDefaults' in rest:
            out['prioritizedDefaults'] = rest['prioritizedDefaults']
        doc['rules'] = out
        self.changed = True


def l3_forms(v, p=''):
    """Every L3 form left in [v], as (pointer, what): the walk's own completeness check, so a
       position the walk did not reach fails loudly instead of passing through."""
    out = []
    if isinstance(v, list):
        for i, x in enumerate(v):
            out += l3_forms(x, p + ptr(i))
    elif isinstance(v, dict):
        node = v.get('node')
        if node in L3_NODES:
            out.append((p, 'node %s' % node))
        elif node == 'Literal' and 'waarde' in v:
            out.append((p + ptr('waarde'), 'a literal lexeme'))
        for k, x in v.items():
            out += l3_forms(x, p + ptr(k))
    return out


def container_forms(doc):
    rules = doc.get('rules') if isinstance(doc.get('rules'), dict) else {}
    return [(ptr('rules', 'uniciteitUnies'), 'the union list')] if 'uniciteitUnies' in rules else []


def migrate(raw):
    """(new bytes | None when already in the L4 shape, [(json pointer, message)], migration)."""
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
    left = l3_forms(doc) + container_forms(doc)
    if left:
        msg = ('internal: %s survived the walk' if m.changed else
               '%s in a document without the L3 union list: the two shapes are mixed')
        return None, [(q, msg % what) for q, what in left], None
    if not m.changed:
        return None, [], m
    out = (ser(doc, 0) + '\n').encode('utf-8')
    # the written bytes must read back as exactly the document built, in gate 2's canonical form
    back = parse(out)
    if plain(back) != plain(doc) or (ser(back, 0) + '\n').encode('utf-8') != out:
        return None, [('', 'internal: the serialised document does not read back as built')], None
    return out, [], m


def migrate_bytes(raw):
    """(new bytes | None when already in the L4 shape, [(json pointer, message)]), as m3.py."""
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
    paths = sorted((k for k in m.counts if k.startswith('pad ')), key=lambda k: int(k[4:]))
    if paths:
        line += ', paden %s' % ' '.join('%s:%d' % (k[4:], m.counts[k]) for k in paths)
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
            print('L3 (refused): %s at %s: %s' % (show(f), path or '<root>', msg))
        elif out is not None:
            left += 1
            print('L3: %s' % show(f))
    print('m4 --check: %d of %d document(s) not in the L4 shape' % (left, len(files)))
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
        tmp = f + '.m4tmp'
        with open(tmp, 'wb') as fh:
            fh.write(out)
        os.replace(tmp, f)
        migrated += 1
        sys.stderr.write('m4: %s: %s\n' % (show(f), count_line(m)))
    for f, errors in failed:
        sys.stderr.write('FAIL: %s\n' % show(f))
        for path, msg in errors:
            sys.stderr.write('  at %s: %s\n' % (path or '<root>', msg))
    print('m4: %d migrated, %d already in the L4 shape and left untouched, %d refused (left in '
          'the L3 shape)' % (migrated, kept, len(failed)))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
