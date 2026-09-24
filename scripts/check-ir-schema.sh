#!/bin/sh
# Gate 2: IR JSON schema (spec/schema/uchar-ir-v1.schema.json).
#
# Five checks:
#   1. the schema is itself a valid draft 2020-12 schema (metaschema);
#  1b. the schema's patterns are held against a fixed lexeme table, in both
#      polarities, with no corpus document involved — without it, deleting a
#      pattern is a silent green (measured 2026-09-11). A clamp fails when a
#      pattern appears at a position the table does not carry;
#  1c. the positions the M1 and L2 shapes close by schema alone are held the same
#      way: one small valid document per shape, one mutation per row, in both
#      polarities;
#   2. every corpus/golden/**/ir.json validates against it;
#   3. every such file round-trips: parsed with raw number lexemes preserved and
#      re-emitted canonically, the output is byte-identical to the input (this is
#      the guarantee that the schema-blessed form and the emitter's form coincide).
#
# With explicit file arguments it runs 1-3 against those files instead of the
# corpus glob (handy for checking a dump outside the repo). An empty corpus
# passes and says so.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

SCHEMA='spec/schema/uchar-ir-v1.schema.json'
if [ ! -f "$SCHEMA" ]; then
    echo "FAIL: schema not found at $SCHEMA" >&2
    exit 2
fi

# (a) locate a python3 that has jsonschema, else fall back to uv, else bail.
if python3 -c 'import jsonschema' 2>/dev/null; then
    RUN='python3'
elif command -v uv >/dev/null 2>&1; then
    RUN='uv run --quiet --with jsonschema python3'
else
    echo 'FAIL: need python3 with the "jsonschema" package.' >&2
    echo '  install it with:  python3 -m pip install jsonschema' >&2
    echo '  or install uv (https://docs.astral.sh/uv/) so this script can run it isolated.' >&2
    exit 2
fi

# --- monotone floor (maintainer decision 2026-09-09) ---------------------------------------------
# A lower bound, never an equality: it catches SHRINK — the corpus that quietly halves — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit. Corpus mode only — explicit mode checks a dump.
# This is the document floor for BOTH artifact zones; gate 3 scans the same set and therefore
# floors only its own, unshared quantity (the negatief cases).
EXPECTED_MIN_DOCS=192   # 2026-09-10: measured 192 (corpus/golden + corpus/rood ir.json)

if [ "$#" -gt 0 ]; then
    MODE='explicit'
    set -- "$SCHEMA" "$MODE" "$EXPECTED_MIN_DOCS" "$@"
else
    MODE='corpus'
    files=$(find corpus/golden corpus/rood -name ir.json 2>/dev/null | sort)
    # shellcheck disable=SC2086
    set -- "$SCHEMA" "$MODE" "$EXPECTED_MIN_DOCS" $files
fi

# shellcheck disable=SC2086
$RUN - "$@" <<'PY'
import sys, json, re

try:
    from jsonschema import Draft202012Validator, validators
    from jsonschema.exceptions import best_match, SchemaError, ValidationError
except Exception as exc:  # pragma: no cover
    sys.stderr.write("FAIL: could not import jsonschema: %s\n" % exc)
    sys.exit(2)

# JSON Schema's `pattern` is an ECMA-262 regular expression, and there `$`
# matches only at the end of the input. The stock validator runs Python's
# re.search, whose `$` also matches before a final newline, so it admitted a
# validity bound "2026-01-01\n" that only WF-101 in the verifier then refused
# (measured 2026-09-23, the gate-4 twin of this fix). This validator takes a
# FULL match instead. For the pattern this schema carries (the date pattern
# at both bounds, anchored at both ends, digits written [0-9]) a full match
# and ECMA-262's search agree: measured 2026-09-23 against node, together with
# the case schema's patterns, on 1437 lexeme-pattern pairs, no difference.
# That is a property of the pattern, not of full matching: `^a$\n?` on "a\n",
# `^\d+$` on "\u0663" and `^\w+$` on "\u00e9" match fully in Python and fail
# in ECMA-262. The self-test below runs the same validator as the corpus
# check; its trailing-newline row fails the day the pattern is matched
# Python's way again, and it carries no non-ASCII digit, so it would not see
# the pattern changed from [0-9] to \d.
def ecma_pattern(validator, pattern, instance, schema):
    if validator.is_type(instance, "string") and re.fullmatch(pattern, instance) is None:
        yield ValidationError("%r does not match %r" % (instance, pattern))

IrValidator = validators.extend(Draft202012Validator, {"pattern": ecma_pattern})

schema_path = sys.argv[1]
mode = sys.argv[2]
floor = int(sys.argv[3])
files = sys.argv[4:]

with open(schema_path, encoding="utf-8") as fh:
    schema = json.load(fh)

# check 1: the schema is a valid draft 2020-12 schema.
try:
    Draft202012Validator.check_schema(schema)
except SchemaError as exc:
    sys.stderr.write("FAIL: %s is not a valid draft 2020-12 schema:\n  %s\n"
                     % (schema_path, exc.message))
    sys.exit(1)
validator = IrValidator(schema)

# --- check 1b: the pattern self-test (maintainer decision 2026-09-11) -----------------------------
# A schema pattern is a MUST with no failing witness of its own: taking it out leaves every gate
# green. Measured 2026-09-11 with both Geldigheid patterns deleted and the schema re-serialised —
# this gate reported PASS at 194 files and the Drools witness stayed 18/18. No CORPUS document can
# close that: this gate validates corpus/golden and corpus/rood only, and a document that VIOLATES a
# pattern cannot sit in either zone without turning the gate permanently red, while one in
# corpus/negatief is never validated here at all. So the witness is held HERE, against the schema
# itself, with no corpus document involved — the mechanism decided on the geldigheidsgrens card.
#
# SCOPE is this schema's WHOLE pattern surface, which is these two positions and nothing else. The
# clamp below fails when a pattern turns up at a position the table does not carry, so a new pattern
# arrives with its own rows or not at all. The ten patterns of uchar-case-v0 stood in exactly the
# same position; since 2026-09-23 gate 4 holds them against a table of its own, on this model.
PATTERN_POSITIONS = {
    "/$defs/Geldigheid/properties/vanaf",
    "/$defs/Geldigheid/properties/tot",
}

# (lexeme, must the schema accept it, why this row is here)
BOUND_LEXEMES = [
    (None,                  True,  "the open bound: null is the declared way to leave a side open"),
    ("2026-01-01",          True,  "the strict lexeme itself"),
    ("2028-02-29",          True,  "a leap day: the pattern is a shape test and admits it"),
    ("2026-02-30",          True,  "THE LAYER BOUNDARY, pinned deliberately: a pattern cannot check "
                                   "a calendar, so this one is admitted here and refused by WF-101 "
                                   "in the verifier. Making the pattern reject it is a change to "
                                   "the split between the two layers; this row is where you say so"),
    ("+012026-01-01",       False, "sign-prefixed extended year: sorts below every four-digit year, "
                                   "so an opaque-string reader silently opens the window"),
    ("-0500-01-01",         False, "the same defect with the other sign"),
    ("niet-een-datum",      False, "not a date at all: sorts above every date, silently closing "
                                   "the window"),
    ("2026-1-1",            False, "variable field widths: orders wrongly against a padded lexeme"),
    ("26-01-01",            False, "a two-digit year"),
    ("2026-01-01T00:00:00", False, "a datetime where a day is called for"),
    ("2026-01-01 ",         False, "a trailing space: the anchors are what keep it out"),
    ("2026-01-01\n",        False, "a trailing newline: Python's `$` also matches before a final "
                                   "newline, ECMA-262's only at the end, so this row fails the day "
                                   "the pattern is matched Python's way again"),
    ("",                    False, "the empty string"),
]

_positions = []


def _scan(node, path):
    if isinstance(node, dict):
        if isinstance(node.get("pattern"), str):
            _positions.append(path)
        for k, val in node.items():
            _scan(val, path + "/" + str(k))
    elif isinstance(node, list):
        for i, val in enumerate(node):
            _scan(val, path + "/" + str(i))


_scan(schema, "")
if set(_positions) != PATTERN_POSITIONS:
    stray = sorted(set(_positions) - PATTERN_POSITIONS)
    gone = sorted(PATTERN_POSITIONS - set(_positions))
    sys.stderr.write("FAIL: the schema's pattern surface moved and the self-test did not follow.\n")
    for p in stray:
        sys.stderr.write("  pattern with no self-test row: %s\n" % p)
    for p in gone:
        sys.stderr.write("  self-test row with no pattern:  %s\n" % p)
    sys.exit(1)

_bound = IrValidator({"$defs": schema["$defs"], "$ref": "#/$defs/Geldigheid"})
selftest_bad = 0
for field in ("vanaf", "tot"):
    for lexeme, want_ok, why in BOUND_LEXEMES:
        probe = {"node": "Geldigheid", "vanaf": None, "tot": None}
        probe[field] = lexeme
        got_ok = not list(_bound.iter_errors(probe))
        if got_ok != want_ok:
            selftest_bad += 1
            sys.stderr.write(
                "FAIL: pattern self-test, Geldigheid.%s = %r: schema %s it, expected %s\n  %s\n"
                % (field, lexeme, "accepts" if got_ok else "refuses",
                   "accept" if want_ok else "refuse", why))
if selftest_bad:
    sys.stderr.write("check-ir-schema: FAIL (%d pattern self-test row(s) bad)\n" % selftest_bad)
    sys.exit(1)
n_selftest = 2 * len(BOUND_LEXEMES)

# --- check 1c: the M1 shape self-test (card M1, 2026-09-23) -------------------------------------
# M1 closes a set of positions by SCHEMA alone: the v0 root keys and format name, the two refused
# severities, a facet without a grid or a count, an untagged characteristic, a self-name at a
# domain's declaration position, a null name on a model-list declaration, a null where an optional
# field is written whole or not at all, a v0 side-map on an ObjectType, a timeline name on a
# characteristic's facet, an empty dimension list. As with the patterns, no corpus document can
# witness such a refusal (golden and rood must validate; negatief is never validated here), so each
# is held against the schema itself: one small valid M1 document, one mutation per row. The rows
# that must PASS pin the deliberate admissions of spec/schema/README.md: where a value's refusal is
# a verifier rule with its own negatief witness, the schema admits the value so that the witness
# stays a document the schema admits.
def _m1_base():
    num = {"node": "Numeriek", "geheel": True, "decimalen": 0, "teken": "ONBEPAALD",
           "eenheid": None}
    return {
        "format": "uchar-ir-v1",
        "model": {
            "objectTypes": [{
                "node": "ObjectType", "name": "Jutter", "animate": True, "plural": "Jutters",
                "attributes": [{
                    "node": "Attribute", "name": "vondstwaarde", "datatype": dict(num),
                    "timeline": {"node": "TimelineFacet", "granularity": "JAAR", "count": 1,
                                 "timelineName": "penningjaar"}}],
                "characteristics": [{
                    "node": "Characteristic", "name": "aangemeld", "kind": "BIJVOEGLIJK",
                    "timeline": {"node": "TimelineFacet", "granularity": "MAAND", "count": 1}}]}],
            "factTypes": [],
            "parameters": [{"node": "Parameter", "naam": "vondstdrempel", "datatype": dict(num),
                            "granulariteit": None}],
            "domains": [{"node": "Domain", "name": "Vondstsoort",
                         "datatype": {"node": "Enumeratie", "waarden": ["hout", "touw"]}}],
            "unitSystems": [],
            "dimensions": [],
            "timelines": [{"node": "Tijdlijn", "naam": "penningjaar", "variabelStartpunt": False,
                           "granulariteit": "JAAR", "aantal": 1}],
            "dayKinds": [],
            "dagsoortPerEenheid": {},
        },
        "rules": {k: [] for k in schema["properties"]["rules"]["required"]},
        "diagnostics": [{"node": "Diagnostic", "severity": "WARNING",
                         "message": "beslistabel 'Vondstsoorten' heeft geen tabelinhoud "
                                    "(alleen een kop)",
                         "line": 3}],
    }


def _ot(d):
    return d["model"]["objectTypes"][0]


def _attr(d):
    return _ot(d)["attributes"][0]


def _char(d):
    return _ot(d)["characteristics"][0]


def _diag(d):
    return d["diagnostics"][0]


# (row, mutation of the base document, must the schema accept it, why this row is here)
M1_SHAPE_ROWS = [
    ("the base document", lambda d: None, True,
     "the M1 shape itself, with a WARNING and a timeline name on an ATTRIBUTE's facet"),
    ("TimelineFacet.count = 0", lambda d: _attr(d)["timeline"].update(count=0), True,
     "count >= 1 is WF-005, a verifier check with its own negatief witness"),
    ("Diagnostic.line = -1", lambda d: _diag(d).update(line=-1), True,
     "line >= 0 is WF-113 (a), a verifier check with its own negatief witness"),
    ("Attribute.datatype = null", lambda d: _attr(d).update(datatype=None), True,
     "a datatype-less attribute is WF-059's, a verifier check with its own negatief witness"),
    ("root carries 'regels'", lambda d: d.update(regels=[]), False,
     "B3: the inert v0 stubs left the wire, as a key and as an empty list"),
    ("root carries 'formaat'", lambda d: d.update(formaat="regelspraak-ir-v0"), False,
     "the v0 format key: the root is {format, model, rules, diagnostics}"),
    ("format = 'regelspraak-ir-v0'", lambda d: d.update(format="regelspraak-ir-v0"), False,
     "B1: the format has one name"),
    ("Diagnostic.severity = 'ERROR'", lambda d: _diag(d).update(severity="ERROR"), False,
     "WF-113 (a): an ERROR means the producer refused to write the document"),
    ("Diagnostic.severity = 'INFO'", lambda d: _diag(d).update(severity="INFO"), False,
     "WF-113 (a): INFO is not a wire severity"),
    ("Diagnostic.severity = null", lambda d: _diag(d).update(severity=None), False,
     "WF-113 (a): WARNING is the one wire severity"),
    ("attribute facet granularity = null",
     lambda d: _attr(d)["timeline"].update(granularity=None), False,
     "a facet without a grid cannot be written (WF-059 arms 2 and 3 left with the side-maps)"),
    ("characteristic facet without granularity",
     lambda d: _char(d)["timeline"].pop("granularity"), False,
     "a facet without a grid cannot be written, absent or null"),
    ("attribute facet without count", lambda d: _attr(d)["timeline"].pop("count"), False,
     "count is always written; a single-period facet writes 1"),
    ("Characteristic without kind", lambda d: _char(d).pop("kind"), False,
     "kind is always written; an untagged v0 characteristic migrates as ZELFSTANDIG"),
    ("Domain's Enumeratie carries 'domein'",
     lambda d: d["model"]["domains"][0]["datatype"].update(domein="Vondstsoort"), False,
     "B2: at the declaration position the Domain wrapper's name is the one spelling"),
    ("Parameter.naam = null", lambda d: d["model"]["parameters"][0].update(naam=None), False,
     "B2: with the map key gone, naam is the declaration's only name"),
    ("ObjectType.plural = null", lambda d: _ot(d).update(plural=None), False,
     "absent is the one spelling of 'no plural declared'"),
    ("Attribute.plural = null", lambda d: _attr(d).update(plural=None), False,
     "absent is the one spelling of 'no plural declared'"),
    ("ObjectType.supertype = null", lambda d: _ot(d).update(supertype=None), False,
     "an extension field is written whole or not at all"),
    ("ObjectType carries the v0 side-map 'attribuutMeervoud'",
     lambda d: _ot(d).update(attribuutMeervoud={}), False,
     "B5: the eight side-maps are member facets, so a facet without a member cannot be written"),
    ("characteristic facet carries timelineName",
     lambda d: _char(d)["timeline"].update(timelineName="penningjaar"), False,
     "v0 recorded no timeline name for a characteristic, and M1 keeps the position closed"),
    ("Attribute.dimensions = []", lambda d: _attr(d).update(dimensions=[]), False,
     "absent is the one spelling of a scalar attribute (WF-004)"),
]


# --- check 1c, the L2 rows (card L2, 2026-09-23) -------------------------------------------------
# L2 closes the reference layer by schema: the fields a reference no longer carries, the ranges
# that are written instead of derived (the conditional obligations of spec/schema/README.md), the
# one role-check node, the symbolic fact creation and scoped uniqueness, the unknown datatype.
# Same mechanism as the M1 rows: one small valid L2 document — the M1 base plus a fact type and
# one node of every class L2 reshapes — and one mutation per row. The rows that must PASS pin the
# deliberate admissions: the bindingless TOTAAL (the subject's own read, held by the verifier) and
# the listed fold, whose elementObjecttype does not make it a ranged fold. In the reasons, a
# B-number is a delta of spec/proposals/V1-WIREMIGRATIE.md, and a D- or U-number a decision listed
# in the L2 card (backlog/done/20260923-l2-referentielaag.md).
def _l2_base():
    d = _m1_base()
    num = {"node": "Numeriek", "geheel": True, "decimalen": 0, "teken": "ONBEPAALD",
           "eenheid": None}
    rb = lambda rol: {"node": "RolBinding", "feittype": "vondstregistratie", "rol": rol}
    ref = lambda attr, ot: {"node": "AttribuutRef", "attribuut": attr, "objecttype": ot,
                            "bron": None}
    fold = lambda kind, args, el: {
        "node": "Functie", "kind": kind, "argumenten": args, "elementObjecttype": el,
        "leegwaarde": None, "filter": None, "dimensieSelectie": None, "type": dict(num),
        "bron": None, "granulariteit": None}
    rule = lambda naam, waarde, voorwaarde: {
        "node": "ResolvedRegel", "naam": naam, "soort": "BEREKENING",
        "doel": ref("vondstwaarde", "Jutter"), "waarde": waarde, "voorwaarde": voorwaarde,
        "geldigheid": None}
    d["model"]["objectTypes"].append({
        "node": "ObjectType", "name": "Vondst", "animate": False, "plural": "Vondsten",
        "attributes": [{"node": "Attribute", "name": "gewicht", "datatype": dict(num)}],
        "characteristics": [{"node": "Characteristic", "name": "aangespoeld",
                             "kind": "BIJVOEGLIJK"}]})
    d["model"]["objectTypes"].append({
        "node": "ObjectType", "name": "Muntvondst", "supertype": "Vondst", "animate": False,
        "attributes": [], "characteristics": []})
    d["model"]["factTypes"].append({
        "node": "Feittype", "naam": "vondstregistratie", "wederkerig": False,
        "rollen": [{"node": "Rol", "naam": "vinder", "objecttypeNaam": "Jutter",
                    "meervoud": "vinders", "meervoudig": False},
                   {"node": "Rol", "naam": "vondst", "objecttypeNaam": "Vondst",
                    "meervoud": "vondsten", "meervoudig": True}]})
    som = fold("SOM", [ref("gewicht", "Vondst")], "Vondst")
    som["bronRol"] = rb("vondst")
    count = fold("AANTAL", [{"node": "InstanceSetUnion", "bron": None, "branches": [{
        "node": "InstanceSetBranch", "elementType": "Vondst", "sourceRole": rb("vondst"),
        "filter": None, "bron": None}]}], None)
    check = {"node": "Kenmerk", "kenmerk": "aangespoeld", "negatie": False, "eigenaar": None,
             "periode": None, "bron": None}
    condition = {"node": "Samengesteld", "kwantor": "ALLE", "drempel": 0, "bron": None,
                 "onderdelen": [
                     {"node": "RoleMembership", "factType": "vondstregistratie", "role": "vinder",
                      "negated": False, "bron": None},
                     {"node": "Kwantorvouw", "kwantor": "ALLE", "elementObjecttype": "Vondst",
                      "filter": None, "toets": check, "bron": None, "bronRol": rb("vondst")}]}
    d["rules"]["resolvedRegels"] = [
        rule("vondstwaarde", som, condition),
        rule("vondstaantal", count, None),
        rule("vondstdrempel", {"node": "ParameterRef", "parameter": "vondstdrempel",
                               "bron": None}, None),
        rule("vondstonbekend", {"node": "Onopgelost", "type": dict(num), "bron": None}, None),
        rule("vondstcel", {"node": "DimensieCel", "attribuut": ref("vondstwaarde", "Jutter"),
                           "labels": ["hout"], "bron": None}, None),
        rule("vondstbinder", {"node": "Let", "type": dict(num), "binder": "w",
                              "value": ref("vondstwaarde", "Jutter"),
                              "body": {"node": "BinderRef", "name": "w", "bron": None},
                              "bron": None}, None),
        rule("vondstvariabele", {"node": "VariabeleRef", "naam": "w", "bron": None}, None),
    ]
    d["rules"]["verdelingen"] = [{
        "node": "ResolvedVerdeling", "naam": "vondstverdeling", "bronType": "Jutter",
        "bronAttribuut": "vondstwaarde", "doelType": "Vondst", "doelAttribuut": "gewicht",
        "methode": "GELIJKE_DELEN", "ratoAttribuut": None, "maximumAttribuut": None,
        "afrondingDecimalen": None, "restantAttribuut": None, "groepeerAttribuut": None,
        "afnemend": False, "voorwaarde": None, "geldigheid": None,
        "verbindendeRol": rb("vondst")}]
    d["rules"]["uniciteiten"] = [{
        "node": "ResolvedUniciteit", "naam": "uniek gewicht", "subject": "Vondst",
        "sleutels": ["gewicht"], "bron": None, "geldigheid": None, "scopeRol": rb("vinder")}]
    d["rules"]["feitCreaties"] = [{
        "node": "ResolvedFeitCreatie", "naam": "registratie", "anker": "Jutter", "doel": "Vondst",
        "tussenstappen": [{"node": "Tussenstap", "rolBinding": rb("vondst")}],
        "voorwaarde": None, "geldigheid": None, "feittype": "vondstregistratie",
        "ankerRol": "vinder", "doelRol": "vondst", "joinRol": rb("vondst")}]
    return d


def _rule(d, i):
    return d["rules"]["resolvedRegels"][i]


def _som(d):
    return _rule(d, 0)["waarde"]


def _part(d, i):
    return _rule(d, 0)["voorwaarde"]["onderdelen"][i]


def _listed(d):
    # the listed value fold: two operands and an elementObjecttype, as the producer writes it
    _rule(d, 2)["waarde"] = {
        "node": "Functie", "kind": "EERSTE", "elementObjecttype": "Jutter",
        "argumenten": [{"node": "AttribuutRef", "attribuut": "vondstwaarde",
                        "objecttype": "Jutter", "bron": None}, _rule(d, 2)["waarde"]],
        "leegwaarde": None, "filter": None, "dimensieSelectie": None, "type": None, "bron": None,
        "granulariteit": None}


def _both(d, kind):
    _som(d).update(kind=kind, bronPad=[dict(_som(d)["bronRol"]), dict(_som(d)["bronRol"])])


_NUM = {"node": "Numeriek", "geheel": True, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": None}

L2_SHAPE_ROWS = [
    ("the L2 base document", lambda d: None, True,
     "the L2 shape itself: ranges written, one RoleMembership, a typeless reference layer"),
    ("RoleMembership without factType", lambda d: _part(d, 0).pop("factType"), False,
     "B9, D11: the check names the fact type it reads, and nothing derives it"),
    ("RoleMembership without role", lambda d: _part(d, 0).pop("role"), False,
     "B9, D11: the check names the role the subject fills, and nothing derives it"),
    ("RoleMembership with counterpartType",
     lambda d: _part(d, 0).update(counterpartType="Muntvondst"), True,
     "U12: the other role's filler narrowed to a subtype of its declared filler type (the "
     "verifier crosses the subtype)"),
    ("RoleMembership with counterpartType = null",
     lambda d: _part(d, 0).update(counterpartType=None), False,
     "U12: absent is the one spelling of the declared filler type; the field is never null"),
    ("RoleMembership carries an unknown key", lambda d: _part(d, 0).update(filler="Muntvondst"),
     False, "B9, U12: the closed node admits counterpartType and nothing else beside its fields"),
    ("RoleMembership carries anderTeFiller",
     lambda d: _part(d, 0).update(anderTeFiller="Vondst"), False,
     "B9: the near-side check's filler field did not move onto the merged node"),
    ("RoleMembership carries backref", lambda d: _part(d, 0).update(backref="vinder"), False,
     "B9: the far-side check's back-reference did not move onto the merged node"),
    ("the L1 near-side role check where the role check stands",
     lambda d: _rule(d, 0)["voorwaarde"]["onderdelen"].__setitem__(0, {
         "node": "Rollidmaatschap", "rol": "vinder", "anderTeFiller": "Vondst",
         "negatie": False, "bron": None}), False,
     "B9: RoleMembership is the one role-check node, in both directions"),
    ("the L1 far-side role check where the role check stands",
     lambda d: _rule(d, 0)["voorwaarde"]["onderdelen"].__setitem__(0, {
         "node": "VerreRollidmaatschap", "filler": "Vondst", "backref": "vinder",
         "negatie": False, "bron": None}), False,
     "B9: RoleMembership is the one role-check node, in both directions"),
    ("AttribuutRef carries set", lambda d: _rule(d, 0)["doel"].update(set=False), False,
     "B11, D8: the set reading is the argument of a ranged fold, not a field"),
    ("AttribuutRef carries navigatieRol",
     lambda d: _rule(d, 0)["doel"].update(navigatieRol="vinder"), False,
     "B11: a navigation is its role bindings; a written name beside them could drift"),
    ("AttribuutRef carries navigatiePad",
     lambda d: _rule(d, 0)["doel"].update(navigatiePad=["vinder"]), False,
     "B11: a navigation is its role bindings; a written path beside them could drift"),
    ("AttribuutRef carries onderwerpBron",
     lambda d: _rule(d, 0)["doel"].update(onderwerpBron="Jutter"), False,
     "B11: the context fixes the subject; the written subject spelling left the wire"),
    ("AttribuutRef carries type", lambda d: _rule(d, 0)["doel"].update(type=dict(_NUM)), False,
     "B13: the declaration is the one source of a reference's type"),
    ("ParameterRef carries type", lambda d: _rule(d, 2)["waarde"].update(type=dict(_NUM)), False,
     "B13: the declaration is the one source of a reference's type"),
    ("DimensieCel carries type", lambda d: _rule(d, 4)["waarde"].update(type=dict(_NUM)), False,
     "B13: the declaration is the one source of a reference's type"),
    ("BinderRef carries type",
     lambda d: _rule(d, 5)["waarde"]["body"].update(type=dict(_NUM)), False,
     "B13: a binder's type is its Let's, written once"),
    ("VariabeleRef carries type", lambda d: _rule(d, 6)["waarde"].update(type=dict(_NUM)),
     False, "B13, D13: the variable reference loses its type with the other four classes"),
    ("Functie SOM with an elementObjecttype and no range", lambda d: _som(d).pop("bronRol"),
     False, "U2, D7: an instance fold's range is written, never derived"),
    ("Functie TOTAAL with an elementObjecttype and no range",
     lambda d: _som(d).update(kind="TOTAAL") or _som(d).pop("bronRol"), True,
     "U2, D8 (iv): the bindingless TOTAAL is the subject's own read (the verifier holds it)"),
    ("Functie SOM with both bronRol and bronPad", lambda d: _both(d, "SOM"), False,
     "U2: one range, one spelling"),
    ("Functie TOTAAL with both bronRol and bronPad", lambda d: _both(d, "TOTAAL"), False,
     "U2: never both, under TOTAAL too"),
    ("Functie EERSTE over two listed operands with an elementObjecttype and no range",
     _listed, True,
     "U2 is scoped to one argument: a listed value fold reads its operands and has no range"),
    ("Kwantorvouw without a range", lambda d: _part(d, 1).pop("bronRol"), False,
     "U2: a quantifier fold's range is written, always"),
    ("Kwantorvouw with both bronRol and bronPad",
     lambda d: _part(d, 1).update(bronPad=[dict(_part(d, 1)["bronRol"]),
                                           dict(_part(d, 1)["bronRol"])]), False,
     "U2: one range, one spelling"),
    ("ResolvedVerdeling without verbindendeRol",
     lambda d: d["rules"]["verdelingen"][0].pop("verbindendeRol"), False,
     "U2, D7: the allocation's relation is written, never derived"),
    ("InstanceSetBranch.sourceRole = null",
     lambda d: _rule(d, 1)["waarde"]["argumenten"][0]["branches"][0].update(sourceRole=None),
     False, "U2: the selection's relation is required and non-null"),
    ("InstanceSetBranch without sourceRole",
     lambda d: _rule(d, 1)["waarde"]["argumenten"][0]["branches"][0].pop("sourceRole"), False,
     "U2: the selection's relation is written, never derived"),
    ("ResolvedFeitCreatie carries zetVeld",
     lambda d: d["rules"]["feitCreaties"][0].update(zetVeld="vondst"), False,
     "B7: a fact creation carries its symbolic fields alone"),
    ("ResolvedFeitCreatie carries joinVeld",
     lambda d: d["rules"]["feitCreaties"][0].update(joinVeld="vinder"), False,
     "B7: a fact creation carries its symbolic fields alone"),
    ("ResolvedFeitCreatie without joinRol",
     lambda d: d["rules"]["feitCreaties"][0].pop("joinRol"), False,
     "B7, D5 (c): the symbolic block is the whole creation"),
    ("ResolvedFeitCreatie without feittype",
     lambda d: d["rules"]["feitCreaties"][0].pop("feittype"), False,
     "B7, D5 (c): the symbolic block is the whole creation"),
    ("ResolvedFeitCreatie without ankerRol",
     lambda d: d["rules"]["feitCreaties"][0].pop("ankerRol"), False,
     "B7, D5 (c): the symbolic block is the whole creation"),
    ("ResolvedFeitCreatie without doelRol",
     lambda d: d["rules"]["feitCreaties"][0].pop("doelRol"), False,
     "B7, D5 (c): the symbolic block is the whole creation"),
    ("Tussenstap without rolBinding",
     lambda d: d["rules"]["feitCreaties"][0]["tussenstappen"][0].pop("rolBinding"), False,
     "B7, D12: a creation step is its binding"),
    ("Tussenstap carries type",
     lambda d: d["rules"]["feitCreaties"][0]["tussenstappen"][0].update(type="Vondst"), False,
     "B7, D12: a creation step is its binding; the bean fields left the step"),
    ("Tussenstap carries veld",
     lambda d: d["rules"]["feitCreaties"][0]["tussenstappen"][0].update(veld="vondst"), False,
     "B7, D12: a creation step is its binding; the bean fields left the step"),
    ("Tussenstap carries viaOuderVeld",
     lambda d: d["rules"]["feitCreaties"][0]["tussenstappen"][0].update(viaOuderVeld=False),
     False, "B7, D12: a creation step is its binding; the bean fields left the step"),
    ("ResolvedUniciteit carries scopeVeld",
     lambda d: d["rules"]["uniciteiten"][0].update(scopeVeld="vinder"), False,
     "B8: scopeRol alone is the scoping"),
    ("Kenmerk carries navigatie", lambda d: _part(d, 1)["toets"].update(navigatie="vondst"), False,
     "D53: the hop is navigatieBinding alone"),
    ("Onbekend as a datatype",
     lambda d: d["model"]["parameters"][0].update(datatype={"node": "Onbekend", "reden": None}),
     False, "B16, D15: Onbekend is no datatype"),
    ("Onbekend as Onopgelost.type",
     lambda d: _rule(d, 3)["waarde"].update(type={"node": "Onbekend", "reden": None}), False,
     "B16, D14, U10: absent is the one spelling of an unknown type; Onbekend is no datatype"),
    ("Onopgelost without type", lambda d: _rule(d, 3)["waarde"].pop("type"), True,
     "D14: absent is the one spelling of an unknown type"),
    ("Onopgelost with type = null", lambda d: _rule(d, 3)["waarde"].update(type=None), False,
     "D14, U10: absent, never null"),
]

shape_bad = 0
for base, rows in ((_m1_base, M1_SHAPE_ROWS), (_l2_base, L2_SHAPE_ROWS)):
    for row, mutate, want_ok, why in rows:
        probe = base()
        mutate(probe)
        got_ok = not list(validator.iter_errors(probe))
        if got_ok != want_ok:
            shape_bad += 1
            sys.stderr.write("FAIL: shape self-test, %s: schema %s it, expected %s\n  %s\n"
                             % (row, "accepts" if got_ok else "refuses",
                                "accept" if want_ok else "refuse", why))
if shape_bad:
    sys.stderr.write("check-ir-schema: FAIL (%d shape self-test row(s) bad)\n" % shape_bad)
    sys.exit(1)
n_shape = len(M1_SHAPE_ROWS) + len(L2_SHAPE_ROWS)


# NOTE: gate 9 (scripts/check-catala.sh) executes the slice of this
# script from the RawNum class definition up to the roundtrip
# definition as its canonicaliser — one serialiser shape, one source
# of truth. Keep RawNum, escape and ser contiguous in that span, and
# keep this comment free of those literal anchor strings.
class RawNum:
    """A JSON number carrying its original lexeme, so round-trip re-emission is exact."""
    __slots__ = ("raw",)
    def __init__(self, s):
        self.raw = s


def escape(s):
    # Matches the reference emitter: named escapes for the seven controls, then
    # \uXXXX (lowercase hex) for every other control char and every non-ASCII char.
    out = ['"']
    for ch in s:
        o = ord(ch)
        if ch == '"':
            out.append('\\"')
        elif ch == '\\':
            out.append('\\\\')
        elif ch == '\n':
            out.append('\\n')
        elif ch == '\r':
            out.append('\\r')
        elif ch == '\t':
            out.append('\\t')
        elif ch == '\b':
            out.append('\\b')
        elif ch == '\f':
            out.append('\\f')
        elif o < 0x20 or o > 0x7e:
            out.append('\\u%04x' % o)
        else:
            out.append(ch)
    out.append('"')
    return ''.join(out)


def ser(v, depth):
    # Two-space indent, key order preserved, empty containers inline ({}/[]).
    if v is None:
        return 'null'
    if isinstance(v, RawNum):
        return v.raw
    if isinstance(v, bool):
        return 'true' if v else 'false'
    if isinstance(v, str):
        return escape(v)
    if isinstance(v, dict):
        if not v:
            return '{}'
        ind = '  ' * (depth + 1)
        parts = [ind + escape(k) + ': ' + ser(val, depth + 1) for k, val in v.items()]
        return '{\n' + ',\n'.join(parts) + '\n' + '  ' * depth + '}'
    if isinstance(v, list):
        if not v:
            return '[]'
        ind = '  ' * (depth + 1)
        parts = [ind + ser(x, depth + 1) for x in v]
        return '[\n' + ',\n'.join(parts) + '\n' + '  ' * depth + ']'
    raise TypeError("unserialisable %r" % type(v))


def roundtrip(path):
    with open(path, "rb") as fh:
        raw = fh.read()
    data = json.loads(raw.decode("utf-8"), parse_int=RawNum, parse_float=RawNum)
    out = (ser(data, 0) + '\n').encode("utf-8")
    if out == raw:
        return None
    a = raw.split(b'\n')
    b = out.split(b'\n')
    for i in range(max(len(a), len(b))):
        la = a[i] if i < len(a) else b'<EOF>'
        lb = b[i] if i < len(b) else b'<EOF>'
        if la != lb:
            return (i + 1,
                    la.decode("utf-8", "replace"),
                    lb.decode("utf-8", "replace"))
    return (0, "", "")


failed = 0
n = 0
for path in files:
    n += 1
    try:
        with open(path, encoding="utf-8") as fh:
            doc = json.load(fh)
    except (OSError, ValueError) as exc:
        sys.stderr.write("FAIL: cannot read/parse %s: %s\n" % (path, exc))
        failed += 1
        continue
    errs = list(validator.iter_errors(doc))
    if errs:
        be = best_match(errs)
        loc = "/".join(str(p) for p in be.absolute_path) or "<root>"
        sys.stderr.write("FAIL: %s\n  at: %s\n  %s\n" % (path, loc, be.message))
        failed += 1
        continue
    rt = roundtrip(path)
    if rt is not None:
        line, want, got = rt
        sys.stderr.write("FAIL: %s round-trip differs at line %d\n  expected: %s\n  got:      %s\n"
                         % (path, line, want, got))
        failed += 1

if failed:
    sys.stderr.write("check-ir-schema: FAIL (%d of %d file(s) bad)\n" % (failed, n))
    sys.exit(1)

# The floor is evaluated BEFORE the unpopulated-corpus exit below, or that exit
# would answer the very case the floor exists for: with the zones moved aside n
# is 0, and a floor that sits after an "n == 0 -> exit 0" branch never runs.
# Measured 2026-09-10, before this order: both zones aside gave PASS at 0.
if mode == "corpus" and n < floor:
    sys.stderr.write("FAIL: %d document(s) validated, but EXPECTED_MIN_DOCS pins at least %d — "
                     "the corpus shrank (a deliberate removal lowers the floor in the same "
                     "commit)\n" % (n, floor))
    sys.exit(1)

# Only reachable while no floor is pinned yet (floor 0) — the genuinely empty corpus.
if mode == "corpus" and n == 0:
    print("check-ir-schema: schema valid; %d pattern self-test row(s); %d shape self-test "
          "row(s); 0 corpus files (corpus not yet populated)" % (n_selftest, n_shape))
    sys.exit(0)

print("check-ir-schema: PASS (schema valid; %d pattern self-test row(s); %d shape self-test "
      "row(s); %d file(s) validate and round-trip, floor %d)" % (n_selftest, n_shape, n, floor))
sys.exit(0)
PY
