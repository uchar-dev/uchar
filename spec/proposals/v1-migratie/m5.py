#!/usr/bin/env python3
"""L5 of the v1 wire migration: uchar-ir-v1 from its L4 shape to its L5 shape (M7 + M8).

    m5.py FILE...                    migrate the named ir.json documents in place, M7 then M8
    m5.py --all                      migrate every corpus/**/ir.json and toka/**/ir.json
    m5.py --check [FILE...]          list every document that is not yet in the L5 shape, in the
                                     L4 or the M7 shape (default: the --all set); exit 1 if one is
    m5.py --stage m7|m8 [...]        the same, with one step only

The target shape is the one the L5 card (20260924-l5-bronrecord-en-hernoeming) fixes under
"De L5-vorm van de wire", and spec/schema/uchar-ir-v1.schema.json types it. L5 is two steps, the
source record (M7) and the renaming (M8). The default run takes a document in the L4 shape
through both, one in the M7 shape through M8 alone, and leaves one in the L5 shape untouched.

The source record (M7, delta B26) moves every source text into the record 'source' and changes
nothing else: every other key, value and key order is carried verbatim.

- A 'bron' string becomes "source": {"snippet": <the string>}, byte for byte (whitespace
  included, never trimmed), at the place 'bron' stood. The record carries no node.
- A null 'bron' is left out: a node without source text carries no source.
- On a consistency version (rules.consistenties[i].versions[j]) and on a ResolvedRegelGroep, 'line'
  goes into the record after 'snippet' where a snippet is written, and is dropped where 'bron' was
  null; the count line reports both. A Diagnostic keeps its own 'line'.

The renaming (M8, deltas B6 and B10) gives every node, key and enum value its IR-SPEC name and
changes nothing else: every other key, value and key order is carried verbatim.

- The nine lists under 'rules' take their L5 keys (rules, dayKindDefinitions, objectCreations,
  allocations, consistencies, uniquenesses, factCreations, startPointDeterminations, ruleGroups),
  in place; prioritizedDefaults keeps its name.
- Every node of the table RENAME takes its L5 name and its keys their L5 names, in place, so the
  key order is the L4 one; PrioritizedDefaults, Default (with its exceptions record) and
  TemporalRead keep their node name and rename their keys. Regelversie keeps its v0 shape until the
  v1 form of the rule-status check lands (B25).
- Every enum value goes through the table VALUES, one family per field; the values a verifier rule
  refuses by name (severity ERROR and INFO, the quantifier DE of a quantifier fold) get their L5
  spelling too. A context-value name outside the table is carried verbatim (WF-098 refuses it),
  and a null severity or timeline granularity stays null. The boolean jaar of the whole-period
  qualifier becomes its granularity: true is year, false is month.
- On a renamed node, a field IR-SPEC names optional is left out where v0 wrote null (NULL_ABSENT);
  every other null is carried verbatim.

Loud by construction: a value the target shape cannot carry fails the document with its file name
and the JSON pointer into the input; nothing is repaired. For M7 that covers an empty 'bron' (a
snippet is never empty, so the document has no M7 form), a null or absent 'bron' on an Onopgelost
marker (the marker requires a source), a 'bron' that is neither a string nor null, a 'line' beside
a string that is not an integer of at least 1, a 'line' without a 'bron' beside it, and a 'line' on
any other object than the two carriers and a Diagnostic. For M8 it covers an enum value outside
the table of its family, a key the L4 node or form or version does not carry, a node the L4 shape
does not have, and a document that mixes the shapes: an L5 node in a document in the M7 shape, a
rules container that is neither the M7 nor the L5 one, a v0 name in a document with the L5
container. A failing document is left untouched. A document already in the target shape is left
untouched as well, byte for byte, so a second run changes nothing. Every migrated document gets one
line on stderr per step counting what was rewritten.

This script reads the L4 and the M7 shape, so it names the fields and values it removes; no L5
artifact carries those names except the rule-status check Regelversie. The table VALUES is the
section 'waarden' of spec/proposals/v1-naamtabel.json, and that file's test holds the two equal.

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
STAGES = ('m7', 'm8')

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


class M7Migration:
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


def migrate_m7(raw):
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
    m = M7Migration()
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


# --- M8: the renaming -----------------------------------------------------------------------------

# the rules container: the v0 list keys in their order, and their L5 keys
RULES_V0 = ('resolvedRegels', 'resolvedDagsoorten', 'objectCreaties', 'verdelingen',
            'consistenties', 'uniciteiten', 'feitCreaties', 'resolvedStartpunten', 'regelGroepen')
RULES_L5 = ('rules', 'dayKindDefinitions', 'objectCreations', 'allocations', 'consistencies',
            'uniquenesses', 'factCreations', 'startPointDeterminations', 'ruleGroups')
RULES_OPTIONAL = ('prioritizedDefaults',)

# every node M8 renames or whose keys it renames: v0 node -> (L5 node, {L4 key: L5 key}); the map
# lists every key the L4 node carries (in the M7 shape, with 'source'), in its L4 order, so a key
# outside it is one the L4 node does not carry
RENAME = {
    'Tekst': ('Text', {'node': 'node'}),
    'Booleaan': ('Boolean', {'node': 'node'}),
    'Enumeratie': ('Enumeration', {'node': 'node', 'domein': 'domain', 'waarden': 'values'}),
    'Feittype': ('FactType', {'node': 'node', 'naam': 'name', 'wederkerig': 'reciprocal',
                              'rollen': 'roles'}),
    'Rol': ('Role', {'node': 'node', 'naam': 'name', 'objecttypeNaam': 'objectType',
                     'meervoud': 'plural', 'meervoudig': 'many'}),
    'Dimensie': ('Dimension', {'node': 'node', 'naam': 'name', 'naamMeervoud': 'plural',
                               'voorzetsel': 'preposition', 'waarden': 'labels'}),
    'Tijdlijn': ('Timeline', {'node': 'node', 'naam': 'name', 'variabelStartpunt': 'variableStart',
                              'granulariteit': 'granularity', 'aantal': 'count'}),
    'RolBinding': ('RoleBinding', {'node': 'node', 'feittype': 'factType', 'rol': 'role'}),
    'Geldigheid': ('Validity', {'node': 'node', 'vanaf': 'from', 'tot': 'to'}),
    'Toekenning': ('MemberInit', {'node': 'node', 'attribuut': 'member', 'waarde': 'value',
                                  'kenmerk': 'characteristic'}),
    'ResolvedDagsoort': ('DayKindDefinition', {'node': 'node', 'naam': 'dayKind',
                                               'voorwaarde': 'condition',
                                               'geldigheid': 'validity'}),
    'ResolvedStartpunt': ('StartPointDetermination', {'node': 'node', 'tijdlijn': 'timeline',
                                                      'objecttype': 'objectType', 'lid': 'member'}),
    'ResolvedRegelGroep': ('RuleGroup', {'node': 'node', 'naam': 'name', 'recursief': 'recursive',
                                         'leden': 'members', 'source': 'source'}),
    'PrioritizedDefaults': ('PrioritizedDefaults', {
        'node': 'node', 'naam': 'name', 'onderwerp': 'subject', 'kenmerk': 'characteristic',
        'attribuut': 'attribute', 'ongeordend': 'unordered', 'defaults': 'defaults',
        'source': 'source', 'geldigheid': 'validity'}),
    'Default': ('Default', {'node': 'node', 'besluit': 'decision', 'voorwaarde': 'justification',
                            'consequentie': 'consequence', 'initialisatie': 'initialisation',
                            'excepties': 'exceptions', 'source': 'source'}),
    'Binair': ('Binary', {'node': 'node', 'op': 'op', 'links': 'left', 'rechts': 'right',
                          'type': 'type', 'source': 'source'}),
    'Afgerond': ('Rounding', {'node': 'node', 'basis': 'base', 'modus': 'mode',
                              'decimalen': 'decimals', 'type': 'type', 'source': 'source'}),
    'Begrensd': ('Clamp', {'node': 'node', 'basis': 'base', 'minimum': 'minimum',
                           'maximum': 'maximum', 'type': 'type', 'source': 'source'}),
    'TekstReeks': ('TextComposition', {'node': 'node', 'delen': 'parts', 'type': 'type',
                                       'source': 'source'}),
    'Letterlijk': ('LiteralChunk', {'node': 'node', 'tekst': 'text'}),
    'Ingevoegd': ('EmbeddedExpression', {'node': 'node', 'expr': 'expr'}),
    'AttribuutRef': ('AttributeReference', {'node': 'node', 'attribuut': 'attribute',
                                            'objecttype': 'subjectType', 'source': 'source',
                                            'navigatieBindingen': 'path'}),
    'DimensieCel': ('DimensionCellRead', {'node': 'node', 'attribuut': 'attribute',
                                          'labels': 'labels', 'source': 'source'}),
    'ParameterRef': ('ParameterReference', {'node': 'node', 'parameter': 'parameter',
                                            'source': 'source'}),
    'VariabeleRef': ('VariableReference', {'node': 'node', 'naam': 'name', 'source': 'source'}),
    'Onopgelost': ('Unresolved', {'node': 'node', 'type': 'type', 'source': 'source'}),
    'TemporalRead': ('TemporalRead', {'node': 'node', 'basis': 'base', 'datum': 'date',
                                      'type': 'type', 'source': 'source'}),
    'DimensieSelectie': ('DimensionSelection', {'node': 'node', 'dimensie': 'dimension',
                                                'labels': 'labels', 'source': 'source'}),
    'Vergelijking': ('Comparison', {'node': 'node', 'op': 'op', 'links': 'left', 'rechts': 'right',
                                    'periode': 'wholePeriod', 'source': 'source'}),
    'Predicaat': ('Predicate', {'node': 'node', 'soort': 'kind', 'operand': 'operand',
                                'dagsoort': 'dayKind', 'negatie': 'negated', 'cijfers': 'digits',
                                'periode': 'wholePeriod', 'source': 'source'}),
    'Kenmerk': ('CharacteristicCheck', {'node': 'node', 'kenmerk': 'characteristic',
                                        'negatie': 'negated', 'eigenaar': 'host',
                                        'periode': 'wholePeriod', 'source': 'source',
                                        'navigatieBinding': 'navigation'}),
    'Periode': ('TimeWindow', {'node': 'node', 'vanaf': 'from', 'tot': 'to',
                               'totInclusief': 'toInclusive', 'source': 'source'}),
    'Samengesteld': ('Compound', {'node': 'node', 'kwantor': 'quantifier', 'drempel': 'threshold',
                                  'onderdelen': 'parts', 'source': 'source'}),
    'Kwantorvouw': ('QuantifierFold', {'node': 'node', 'kwantor': 'quantifier',
                                       'drempel': 'threshold', 'elementObjecttype': 'elementType',
                                       'filter': 'filter', 'toets': 'test', 'source': 'source',
                                       'bronRol': 'sourceRole', 'bronPad': 'sourcePath'}),
    'GeheelPeriode': ('WholePeriod', {'node': 'node', 'jaar': 'granularity', 'niet': 'negated'}),
}
# the L4 nodes M8 carries under their own name and with their own keys
KEPT = ('Aggregation', 'Allocation', 'Attribute', 'BinderRef', 'BoundDay', 'BuiltinCall',
        'Characteristic', 'Consistency', 'Construct', 'ContextValue', 'Date', 'DayKind',
        'Diagnostic', 'Domain', 'ExceptionGroup', 'FactCreation', 'FieldAccess', 'FilterList',
        'Fold', 'FunctionDef', 'IfThenElse', 'InstanceSetBranch', 'InstanceSetUnion', 'Invoke',
        'Lambda', 'Let', 'ListLength', 'ListLiteral', 'ListType', 'Literal', 'MapList', 'Match',
        'Number', 'ObjectCreation', 'ObjectType', 'Parameter', 'Regelversie', 'RoleMembership',
        'Rule', 'Source', 'StructLiteral', 'StructType', 'SumType', 'TimelineFacet', 'Uniqueness',
        'Unit', 'UnitSystem')
L5_NODES = {l5: set(keys.values()) for l5, keys in RENAME.values()}
V0_NODES = tuple(n for n, (l5, _) in RENAME.items() if l5 != n)

# the default record of a Default: its v0 keys and their L5 keys
EXCEPTIONS = {'ongeordend': 'unordered', 'leden': 'members'}

# the six named forms: v0 list -> (form node, the keys of the form, the keys of a version, the
# enum place on the version or None); the forms carry their L4 shape, which is the L5 one
FORMS = {
    'resolvedRegels': ('Rule', ('node', 'name', 'versions', 'statuteAnnotations'),
                       ('validity', 'target', 'value', 'condition', 'initialisation',
                        'presentation'), 'RuleVersion.presentation'),
    'objectCreaties': ('ObjectCreation', ('node', 'name', 'versions'),
                       ('createdType', 'subject', 'initialisations', 'condition', 'validity',
                        'connectingRole'), None),
    'verdelingen': ('Allocation', ('node', 'name', 'versions'),
                    ('sourceType', 'sourceAttribute', 'targetType', 'targetAttribute', 'method',
                     'ratioAttribute', 'maximumAttribute', 'roundingDecimals',
                     'remainderAttribute', 'orderingAttribute', 'descending', 'condition',
                     'validity', 'connectingRole'), 'AllocationVersion.method'),
    'consistenties': ('Consistency', ('node', 'name', 'versions'),
                      ('subject', 'assertion', 'validity', 'source'), None),
    'uniciteiten': ('Uniqueness', ('node', 'name', 'versions'),
                    ('sources', 'scope', 'validity', 'source'), None),
    'feitCreaties': ('FactCreation', ('node', 'name', 'versions'),
                     ('factType', 'anchorRole', 'targetRole', 'anchor', 'path', 'condition',
                      'validity'), None),
}

# null -> absent: the optional fields of the renamed nodes, by L5 place, where v0 wrote null
NULL_ABSENT = ('Validity.from', 'Validity.to', 'Role.plural', 'Comparison.wholePeriod',
               'CharacteristicCheck.wholePeriod', 'CharacteristicCheck.host', 'Default.decision',
               'QuantifierFold.filter', 'PrioritizedDefaults.characteristic',
               'Predicate.wholePeriod', 'Predicate.dayKind', 'Default.justification',
               'TimeWindow.to', 'Clamp.maximum', 'Clamp.minimum', 'Dimension.plural',
               'Dimension.preposition', 'TimeWindow.from')

# the enum families: family -> (its L5 places, {v0 value: L5 value}). The section 'waarden' of
# spec/proposals/v1-naamtabel.json carries the same pairs, and its test holds the two equal. The
# values a verifier rule refuses by name (severity error and info, the quantifier the on a
# quantifier fold) are in the table: they get the L5 spelling like every other value.
VALUES = {
    'Number.sign': (('Number.sign',), {
        'ONBEPAALD': 'unspecified', 'NEGATIEF': 'negative', 'NIET_NEGATIEF': 'nonNegative',
        'POSITIEF': 'positive'}),
    'Characteristic.kind': (('Characteristic.kind',), {
        'BIJVOEGLIJK': 'adjectival', 'BEZITTELIJK': 'possessive', 'ZELFSTANDIG': 'nominal'}),
    'granularity': (('TimelineFacet.granularity', 'Timeline.granularity',
                     'BuiltinCall.granularity'), {
        'DAG': 'day', 'WEEK': 'week', 'MAAND': 'month', 'KWARTAAL': 'quarter', 'JAAR': 'year'}),
    'RuleVersion.presentation': (('RuleVersion.presentation',), {
        'GELIJKSTELLING': 'equation', 'BEREKENING': 'calculation'}),
    'AllocationVersion.method': (('AllocationVersion.method',), {
        'GELIJKE_DELEN': 'equalShares', 'NAAR_RATO': 'proportional', 'OP_VOLGORDE': 'ordered'}),
    'Comparison.op': (('Comparison.op',), {
        'KLEINER': 'less', 'KLEINER_OF_GELIJK': 'lessOrEqual', 'GROTER': 'greater',
        'GROTER_OF_GELIJK': 'greaterOrEqual', 'GELIJK': 'equal', 'ONGELIJK': 'notEqual',
        'EERDER': 'earlier', 'EERDER_OF_GELIJK': 'earlierOrEqual', 'LATER': 'later',
        'LATER_OF_GELIJK': 'laterOrEqual'}),
    'Binary.op': (('Binary.op',), {
        'PLUS': 'plus', 'MIN': 'minus', 'VERMINDERD_MET': 'reducedBy', 'MAAL': 'times',
        'GEDEELD_DOOR': 'dividedBy', 'GEDEELD_DOOR_ABS': 'dividedByAbs', 'TOT_DE_MACHT': 'power',
        'PERCENTAGE_VAN': 'percentageOf'}),
    'Rounding.mode': (('Rounding.mode',), {
        'NAAR_BENEDEN': 'floor', 'NAAR_BOVEN': 'ceiling', 'RICHTING_NUL': 'towardZero',
        'WEG_VAN_NUL': 'awayFromZero', 'REKENKUNDIG': 'halfAwayFromZero'}),
    'Aggregation.kind': (('Aggregation.kind',), {
        'SOM': 'sum', 'TOTAAL': 'total', 'AANTAL': 'count', 'MINIMUM': 'minimum',
        'MAXIMUM': 'maximum', 'EERSTE': 'first', 'LAATSTE': 'last',
        'AANTAL_GEVULDE': 'filledCount'}),
    'BuiltinCall.kind': (('BuiltinCall.kind',), {
        'WORTEL': 'squareRoot', 'ABSOLUTE_WAARDE': 'absoluteValue',
        'CONCATENATIE': 'concatenation', 'TIJDSDUUR': 'duration',
        'ABSOLUTE_TIJDSDUUR': 'absoluteDuration', 'EERSTE_PAASDAG': 'easterSunday',
        'TIJDSEVENREDIG_DEEL': 'timeProportionalPart', 'AANTAL_DAGEN_IN': 'dayCount',
        'JAAR_UIT': 'yearOf', 'MAAND_UIT': 'monthOf', 'DAG_UIT': 'dayOf', 'UUR_UIT': 'hourOf',
        'MINUUT_UIT': 'minuteOf', 'SECONDE_UIT': 'secondOf',
        'MILLISECONDE_UIT': 'millisecondOf', 'DATUM_MET': 'dateWith',
        'DATUMTIJD_MET': 'dateTimeWith'}),
    'Predicate.kind': (('Predicate.kind',), {
        'LEEG': 'empty', 'GEVULD': 'filled', 'ELFPROEF': 'elevenTest',
        'NUMERIEK_MET_EXACT': 'exactDigits', 'DAGSOORT': 'dayKind'}),
    'quantifier': (('Compound.quantifier', 'QuantifierFold.quantifier'), {
        'DE': 'the', 'ALLE': 'all', 'GEEN': 'none', 'TEN_MINSTE': 'atLeast',
        'TEN_HOOGSTE': 'atMost', 'PRECIES': 'exactly'}),
    'ContextValue.name': (('ContextValue.name',), {
        'Rekendatum': 'evaluationDate', 'Rekenjaar': 'evaluationYear'}),
    'WholePeriod.granularity': (('WholePeriod.granularity',), {True: 'year', False: 'month'}),
    'Diagnostic.severity': (('Diagnostic.severity',), {
        'WARNING': 'warning', 'ERROR': 'error', 'INFO': 'info'}),
}
# a family whose value is a free string: a name outside its table is carried verbatim (WF-098
# refuses it, with its own witness)
FREE = ('ContextValue.name',)
# the places where a null is carried as it stands: the schema admits it on a timeline, and on a
# diagnostic it is a value the verifier refuses by name (WF-113 (a))
NULL_KEPT = ('Timeline.granularity', 'Diagnostic.severity')
PLACE = {place: fam for fam, (places, _) in VALUES.items() for place in places}

COUNT_NAMES_M8 = ('knopen', 'sleutels', 'lijstsleutels', 'waarden', 'null weg',
                  'namen verbatim')


def value_of(table, x):
    """The L5 value of [x] in [table], or the sentinel MISSING: a boolean never matches a string."""
    for v0, l5 in table.items():
        if type(v0) is type(x) and v0 == x:
            return l5
    return MISSING


MISSING = object()


def show_value(x):
    return json.dumps(plain(x)) if not isinstance(x, RawNum) else x.raw


class M8Migration:
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
        if n is None:
            return {k: self.walk(x, p + ptr(k)) for k, x in v.items()}
        if n in RENAME:
            return self.renamed(v, p)
        if n in KEPT:
            return self.kept(v, p)
        if n in L5_NODES:
            self.fail(p + ptr('node'), 'the L5 node %s in a document in the M7 shape: the two '
                      'shapes are mixed' % n)
        else:
            self.fail(p + ptr('node'), 'the node %s: the L4 shape has no such node'
                      % json.dumps(n))
        return v

    def value(self, place, x, p):
        fam = PLACE[place]
        _, table = VALUES[fam]
        if x is None and place in NULL_KEPT:
            return None
        l5 = value_of(table, x)
        if l5 is not MISSING:
            self.counts['waarden'] += 1
            return l5
        if fam in FREE:
            self.counts['namen verbatim'] += 1
            return x
        if any(type(l5) is type(x) and l5 == x for l5 in table.values()):
            self.fail(p, 'the L5 value %s at %s in a document in the M7 shape: the two shapes are '
                      'mixed' % (show_value(x), place))
        else:
            self.fail(p, 'the value %s at %s is not in the table of the family %s: the L5 shape '
                      'has no spelling for it' % (show_value(x), place, fam))
        return x

    def kept(self, v, p):
        n = v['node']
        out = {}
        for k, x in v.items():
            place = '%s.%s' % (n, k)
            q = p + ptr(k)
            out[k] = self.value(place, x, q) if place in PLACE else self.walk(x, q)
        return out

    def renamed(self, v, p):
        n = v['node']
        l5, keys = RENAME[n]
        out = {}
        for k, x in v.items():
            q = p + ptr(k)
            if k not in keys:
                self.fail(q, 'the key %s on %s: the L4 node carries no such key'
                          % (json.dumps(k), n))
                continue
            nk = keys[k]
            if nk != k:
                self.counts['sleutels'] += 1
            if k == 'node':
                out['node'] = l5
                continue
            place = '%s.%s' % (l5, nk)
            if x is None and place in NULL_ABSENT:
                self.counts['null weg'] += 1
                continue
            if place in PLACE:
                out[nk] = self.value(place, x, q)
            elif place == 'Default.exceptions':
                out[nk] = self.exceptions(x, q)
            else:
                out[nk] = self.walk(x, q)
        if l5 != n:
            self.counts['knopen'] += 1
        return out

    def exceptions(self, x, p):
        if not isinstance(x, dict):
            self.fail(p, 'the exceptions of a default that are not a record')
            return x
        out = {}
        for k, y in x.items():
            if k not in EXCEPTIONS:
                self.fail(p + ptr(k), 'the key %s on the exceptions of a default: the L4 record '
                          'carries no such key' % json.dumps(k))
                continue
            self.counts['sleutels'] += 1
            out[EXCEPTIONS[k]] = self.walk(y, p + ptr(k))
        return out

    def forms(self, lst, p, v0_list):
        form, form_keys, version_keys, place = FORMS[v0_list]
        if not isinstance(lst, list):
            self.fail(p, 'the list %s is not a list' % v0_list)
            return lst
        out = []
        for i, f in enumerate(lst):
            q = p + ptr(i)
            if not isinstance(f, dict) or f.get('node') != form:
                self.fail(q, 'an entry that is not a %s in the list %s' % (form, v0_list))
                continue
            nf = {}
            for k, x in f.items():
                if k not in form_keys:
                    self.fail(q + ptr(k), 'the key %s on %s: the L4 form carries no such key'
                              % (json.dumps(k), form))
                elif k == 'versions' and isinstance(x, list):
                    nf[k] = [self.version(y, q + ptr(k, j), form, version_keys, place)
                             for j, y in enumerate(x)]
                else:
                    nf[k] = self.walk(x, q + ptr(k))
            out.append(nf)
        return out

    def version(self, v, p, form, keys, place):
        if not isinstance(v, dict):
            self.fail(p, 'a version of a %s that is not a record' % form)
            return v
        out = {}
        for k, x in v.items():
            if k not in keys:
                self.fail(p + ptr(k), 'the key %s on a version of a %s: the L4 version carries no '
                          'such key' % (json.dumps(k), form))
                continue
            if place is not None and place.endswith('.' + k):
                out[k] = self.value(place, x, p + ptr(k))
            else:
                out[k] = self.walk(x, p + ptr(k))
        return out

    def run(self, doc):
        out = {}
        for k, v in doc.items():
            if k != 'rules':
                out[k] = self.walk(v, ptr(k))
                continue
            rules = {}
            for lk, lst in v.items():
                q = ptr('rules', lk)
                if lk in FORMS:
                    rules[RULES_L5[RULES_V0.index(lk)]] = self.forms(lst, q, lk)
                elif lk in RULES_V0:
                    rules[RULES_L5[RULES_V0.index(lk)]] = self.walk(lst, q)
                elif lk in RULES_OPTIONAL:
                    rules[lk] = self.walk(lst, q)
                else:
                    self.fail(q, 'the key %s under rules: the M7 shape has no such list'
                              % json.dumps(lk))
                    continue
                if lk in RULES_V0:
                    self.counts['lijstsleutels'] += 1
            out[k] = rules
        return out


def container(doc):
    """'v0', 'l5' or None (neither: the keys under rules mix the two or match neither)."""
    rules = doc.get('rules')
    keys = set(rules) - set(RULES_OPTIONAL) if isinstance(rules, dict) else None
    if keys == set(RULES_V0):
        return 'v0'
    if keys == set(RULES_L5):
        return 'l5'
    return None


def left_m8(v, p=''):
    """Every v0 name the L5 shape does not carry, as (pointer, what): the walk's own completeness
       check, and the test that a document with the L5 container is L5 throughout."""
    out = []
    if isinstance(v, list):
        for i, x in enumerate(v):
            out += left_m8(x, p + ptr(i))
        return out
    if not isinstance(v, dict):
        return out
    n = v.get('node')
    if n in V0_NODES:
        out.append((p, 'the v0 node %s' % n))
    elif n in L5_NODES:
        for k in v:
            if k not in L5_NODES[n] and k != 'bron':
                out.append((p + ptr(k), 'the key %s, which the L5 node %s does not carry'
                            % (json.dumps(k), n)))
    if 'bron' in v:
        out.append((p + ptr('bron'), 'the v0 field bron'))
    if n == 'Default' and isinstance(v.get('exceptions'), dict):
        for k in v['exceptions']:
            if k not in EXCEPTIONS.values():
                out.append((p + ptr('exceptions', k), 'the key %s on the exceptions of a default'
                            % json.dumps(k)))
    for k, x in v.items():
        place = '%s.%s' % (n, k)
        if place in PLACE and PLACE[place] not in FREE:
            _, table = VALUES[PLACE[place]]
            if value_of(table, x) is not MISSING:
                out.append((p + ptr(k), 'the v0 value %s' % show_value(x)))
        out += left_m8(x, p + ptr(k))
    return out


def left_rules(doc):
    """The v0 list keys under rules and the v0 values on the version records."""
    out = []
    rules = doc.get('rules') if isinstance(doc.get('rules'), dict) else {}
    for k in rules:
        if k in RULES_V0:
            out.append((ptr('rules', k), 'the v0 list %s' % k))
    for lst, place in (('rules', 'RuleVersion.presentation'),
                       ('allocations', 'AllocationVersion.method')):
        field = place.split('.')[1]
        _, table = VALUES[PLACE[place]]
        for i, f in enumerate(rules.get(lst) or []):
            for j, ver in enumerate((f.get('versions') or []) if isinstance(f, dict) else []):
                if isinstance(ver, dict) and value_of(table, ver.get(field)) is not MISSING:
                    out.append((ptr('rules', lst, i, 'versions', j, field),
                                'the v0 value %s' % show_value(ver.get(field))))
    return out


def migrate_m8(raw):
    """(new bytes | None when already in the L5 shape, [(json pointer, message)], migration)."""
    try:
        doc = parse(raw)
    except (UnicodeDecodeError, ValueError, Refusal) as exc:
        return None, [('', 'cannot parse: %s' % exc)], None
    if not isinstance(doc, dict):
        return None, [('', 'the document is not a JSON object')], None
    if doc.get('format') != FORMAT:
        return None, [(ptr('format'), 'format %s is not %s (m1.py migrates the v0 shape first)'
                       % (json.dumps(doc.get('format')), json.dumps(FORMAT)))], None
    bron, _, line = shape(doc)
    shape_of = container(doc)
    if shape_of == 'l5':
        rest = left_m8(doc) + left_rules(doc)
        if rest:
            return None, [(q, '%s in a document with the L5 rules container: the shapes are '
                           'mixed' % what) for q, what in rest], None
        return None, [], None
    if shape_of is None:
        return None, [(ptr('rules'), 'the lists under rules are neither the nine of the M7 shape '
                       'nor the nine of the L5 shape')], None
    if bron or line:
        return None, [(q, 'the document is in the L4 shape: M7 comes first')
                      for q in bron + line], None
    m = M8Migration()
    doc = m.run(doc)
    if m.errors:
        return None, m.errors, None
    rest = left_m8(doc) + left_rules(doc)
    if rest:
        return None, [(q, 'internal: %s survived the walk' % what) for q, what in rest], None
    out = (ser(doc, 0) + '\n').encode('utf-8')
    # the written bytes must read back as exactly the document built, in gate 2's canonical form
    back = parse(out)
    if plain(back) != plain(doc) or (ser(back, 0) + '\n').encode('utf-8') != out:
        return None, [('', 'internal: the serialised document does not read back as built')], None
    return out, [], m


# --- the run --------------------------------------------------------------------------------------

def migrate(raw, stages=STAGES):
    """(new bytes | None when unchanged, [(json pointer, message)], {stage: migration}).

    The default run is M7 then M8: a document in the L4 shape gets both, one in the M7 shape only
    M8, one in the L5 shape nothing. A document whose rules container is the L5 one is checked
    for a v0 name before M7 runs, so M7 never writes into a document that is L5 elsewhere."""
    done = {}
    cur = raw
    if 'm7' in stages:
        try:
            doc = parse(raw)
        except (UnicodeDecodeError, ValueError, Refusal) as exc:
            return None, [('', 'cannot parse: %s' % exc)], None
        if isinstance(doc, dict) and container(doc) == 'l5' and 'm8' in stages:
            out, errors, _ = migrate_m8(raw)
            return None, errors, done
        out, errors, m = migrate_m7(cur)
        if errors:
            return None, errors, None
        if out is not None:
            cur, done['m7'] = out, m
    if 'm8' in stages:
        out, errors, m = migrate_m8(cur)
        if errors:
            return None, errors, None
        if out is not None:
            cur, done['m8'] = out, m
    return (cur if cur is not raw else None), [], done


def migrate_bytes(raw):
    """(new bytes | None when already in the L5 shape, [(json pointer, message)]): the default run,
       for the documents embedded in the test suites."""
    out, errors, _ = migrate(raw)
    return out, errors


def default_files():
    files = sorted(glob.glob(os.path.join(ROOT, 'corpus', '**', 'ir.json'), recursive=True))
    files += sorted(glob.glob(os.path.join(ROOT, 'toka', '**', 'ir.json'), recursive=True))
    return files


def show(path):
    rel = os.path.relpath(path, ROOT)
    return path if rel.startswith('..') else rel


def count_line(stage, m):
    names = COUNT_NAMES if stage == 'm7' else COUNT_NAMES_M8
    return ', '.join('%s %d' % (k, m.counts[k]) for k in names)


def label(stages):
    return 'm5' if stages == STAGES else 'm5 --stage %s' % stages[0]


def target(stages):
    return 'the %s shape' % ('M7' if stages == ('m7',) else 'L5')


def check(files, stages):
    left = 0
    for f in files:
        try:
            raw = open(f, 'rb').read()
        except OSError as exc:
            sys.stderr.write('FAIL: cannot read %s: %s\n' % (show(f), exc))
            return 2
        out, errors, done = migrate(raw, stages)
        if errors:
            left += 1
            path, msg = errors[0]
            print('refused: %s at %s: %s' % (show(f), path or '<root>', msg))
        elif out is not None:
            left += 1
            print('%s: %s' % ('L4' if 'm7' in done else 'M7', show(f)))
    print('%s --check: %d of %d document(s) not in %s' % (label(stages), left, len(files),
                                                          target(stages)))
    return 1 if left else 0


def main(argv):
    if argv and argv[0] in ('-h', '--help'):
        print(__doc__.split('\n\n')[1])
        return 0
    stages = STAGES
    if argv and argv[0] == '--stage':
        if len(argv) < 2 or argv[1] not in STAGES:
            sys.stderr.write('FAIL: --stage names one step: %s\n' % '|'.join(STAGES))
            return 2
        stages, argv = (argv[1],), argv[2:]
    if not argv:
        print(__doc__.split('\n\n')[1])
        return 2
    if argv[0] == '--check':
        files = argv[1:] or default_files()
        if not files:
            sys.stderr.write('FAIL: no ir.json documents to check\n')
            return 2
        return check(files, stages)
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
        out, errors, done = migrate(raw, stages)
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
        for stage in STAGES:
            if stage in done:
                sys.stderr.write('m5 %s: %s: %s\n'
                                 % (stage, show(f), count_line(stage, done[stage])))
    for f, errors in failed:
        sys.stderr.write('FAIL: %s\n' % show(f))
        for path, msg in errors:
            sys.stderr.write('  at %s: %s\n' % (path or '<root>', msg))
    print('%s: %d migrated, %d already in %s and left untouched, %d refused (left as they were)'
          % (label(stages), migrated, kept, target(stages), len(failed)))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
