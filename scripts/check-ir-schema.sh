#!/bin/sh
# Gate 2: IR JSON schema (spec/schema/uchar-ir-v1.schema.json).
#
# Five checks:
#   1. the schema is itself a valid draft 2020-12 schema (metaschema);
#  1b. the schema's patterns are held against a fixed lexeme table, in both
#      polarities, with no corpus document involved — without it, deleting a
#      pattern is a silent green (measured 2026-09-11). A clamp fails when a
#      pattern appears at a position the table does not carry;
#  1c. the positions the M1, L2, L3 and L4 shapes and the source record of L5
#      close by schema alone are held the same way: one small valid document per
#      shape, one mutation per row, in both polarities;
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
# FULL match instead. For the patterns this schema carries a full match and
# ECMA-262's search agree. The date pattern at both validity bounds is
# anchored at both ends with digits written [0-9]: measured 2026-09-23
# against node, together with the case schema's patterns, on 1437
# lexeme-pattern pairs, no difference. Since L4 a literal's number, date and
# dateTime values carry the case schema's own three patterns, and a form name
# carries a negative lookahead written with [\s\S] and closed on (?![\s\S])
# instead of `$`, because inside a lookahead Python's `$` also matches before
# a final newline: measured 2026-09-24 against node, with and without the u
# flag, on 787 lexeme-pattern pairs (the tables below, every literal lexeme
# of the three kinds and every form name of the four zones and the parity
# zone), no difference. That is a property of the patterns, not of full
# matching: `^a$\n?` on "a\n", `^\d+$` on "\u0663" and `^\w+$` on "\u00e9"
# match fully in Python and fail in ECMA-262. The self-test below runs the
# same validator as the corpus check; its trailing-newline rows fail the day
# a pattern is matched Python's way again, and it carries no non-ASCII digit
# except in one form-name row, so it would not see a date or number pattern
# changed from [0-9] to \d.
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
# SCOPE is this schema's WHOLE pattern surface, which is these six positions and nothing else: the
# two validity bounds, and since L4 the three patterned variants of a literal's value (taken from the
# case schema) and the form name. The clamp below fails when a pattern turns up at a position the
# table does not carry, so a new pattern arrives with its own rows or not at all. The ten patterns
# of uchar-case-v0 stood in exactly the same position; since 2026-09-23 gate 4 holds them against a
# table of its own, on this model.
BOUND, NUMBER, DATE, DATETIME, FORMNAME = "bound", "number", "date", "dateTime", "formName"
PATTERN_POSITIONS = {
    "/$defs/Geldigheid/properties/vanaf": BOUND,
    "/$defs/Geldigheid/properties/tot": BOUND,
    "/$defs/LiteralValue/oneOf/0/properties/number": NUMBER,
    "/$defs/LiteralValue/oneOf/3/properties/date": DATE,
    "/$defs/LiteralValue/oneOf/4/properties/dateTime": DATETIME,
    "/$defs/formName": FORMNAME,
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

# (lexeme, must the schema accept it, why this row is here), per kind; the value patterns of a
# literal are the case schema's (gate 4 holds the same three there), so its rows are carried over,
# with the spellings the verifier's lexeme rules have negatief witnesses for beside them
LEXEMES = {
    NUMBER: [
        ("5",      True,  "an integer"),
        ("-12.50", True,  "a negative decimal"),
        ("3/4",    True,  "a rational"),
        ("12,50",  False, "a decimal comma: the literal writes a point, and the comma stays in the "
                          "source snippet (B22)"),
        ("5 \u20ac", False, "a unit in the lexeme: the unit is the literal's type (the WF-081 family)"),
        ("5\n",    False, "a trailing newline"),
        ("1,000",  False, "a grouping comma"),
        (".5",     False, "a bare leading point"),
        ("5.",     False, "a point without a fraction"),
        ("1E3",    False, "an exponent"),
        ("+5",     False, "a plus sign"),
        ("1/0",    False, "a zero denominator"),
        ("3/04",   False, "a denominator with a leading zero"),
        ("1.5/2",  False, "a decimal numerator"),
        ("",       False, "the empty string"),
    ],
    DATE: [
        ("2026-06-01",          True,  "the strict lexeme itself"),
        ("2026-02-30",          True,  "THE LAYER BOUNDARY, pinned deliberately: a pattern cannot check a "
                                       "calendar, so this one is admitted here and refused by WF-084 in the "
                                       "verifier"),
        ("31-01-2024",          False, "day-first, the source snippet's order (the WF-084 family)"),
        ("dd. 31-01-2024",      False, "the source snippet itself"),
        ("2026-06-01\n",        False, "a trailing newline"),
        ("2026-06-01 ",         False, "a trailing space"),
        ("2026-6-1",            False, "variable field widths"),
        ("+012026-06-01",       False, "a sign-prefixed extended year"),
        ("2026-06-01T00:00:00", False, "a dateTime where a day is called for"),
        ("",                    False, "the empty string"),
    ],
    DATETIME: [
        ("2026-03-15T12:00:00",          True,  "the canonical lexeme without a fraction"),
        ("2026-03-15T12:00:00.250",      True,  "a fraction of exactly three digits"),
        ("2026-03-15T24:00:00",          True,  "THE LAYER BOUNDARY: the pattern checks field widths, "
                                                "not the clock, so hour 24 is admitted here and refused "
                                                "by WF-095 in the verifier"),
        ("2026-03-15T12:00",             False, "no seconds field (the WF-095 family)"),
        ("2026-03-15 12:00:00",          False, "a space where 'T' belongs"),
        ("2026-03-15T12:00:00.1",        False, "a fraction of one digit"),
        ("2026-03-15T12:00:00Z",         False, "a zone suffix"),
        ("15-03-2026 12:00:00.000",      False, "the day-first source snippet"),
        ("rommel 2026-03-15T12:00:00 x", False, "surrounding text"),
        ("2026-03-15T12:00:00\n",        False, "a trailing newline"),
    ],
    FORMNAME: [
        (None,                             True,  "a nameless form: WF-107's, with its own witness"),
        ("",                               True,  "a blank name: WF-107's, with its own witness"),
        ("jutheffing naar jaartarief",     True,  "a plain name"),
        ("jutheffing naar jaartarief #2",  False, "the spelling of the second version of a form with "
                                                  "several: on the v0 wire and in the readers' "
                                                  "in-memory records, so a form named so would read as "
                                                  "a version of another (B19)"),
        ("x #1",                           False, "the first version's spelling"),
        ("x #10",                          False, "any number of digits"),
        ("x #0",                           False, "no version is numbered 0, but the spelling is still "
                                                  "a version's"),
        (" #1",                            False, "a name that is nothing but the suffix"),
        ("x#2",                            True,  "no space before '#': not the version spelling"),
        ("x # 2",                          True,  "a space after '#'"),
        ("x #2 ",                          True,  "a trailing space: the suffix does not end the name"),
        ("x #2a",                          True,  "a letter after the digits"),
        ("x #",                            True,  "no digits"),
        ("regel #1 van 2",                 True,  "the suffix inside the name"),
        ("x #2\n",                         True,  "a trailing newline: the suffix does not end the name. "
                                                  "Python's `$` would match before that newline, "
                                                  "ECMA-262's does not; this row fails the day the "
                                                  "lookahead is closed on `$`"),
        ("x #\u0663",                      True,  "a non-ASCII digit: the in-memory spelling is written "
                                                  "with ASCII digits, so this is not a version's; the "
                                                  "row fails the day [0-9] becomes \\d"),
    ],
}

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
if set(_positions) != set(PATTERN_POSITIONS):
    stray = sorted(set(_positions) - set(PATTERN_POSITIONS))
    gone = sorted(set(PATTERN_POSITIONS) - set(_positions))
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
n_selftest = 2 * len(BOUND_LEXEMES)
# every other position: its own subschema, reached by $ref from a root that carries the schema's
# $defs (which assert nothing about a string instance), probed with the bare lexeme
for pos, kind in sorted(PATTERN_POSITIONS.items()):
    if kind == BOUND:
        continue
    probe_v = IrValidator({"$defs": schema["$defs"], "$ref": "#" + pos})
    for lexeme, want_ok, why in LEXEMES[kind]:
        n_selftest += 1
        got_ok = not list(probe_v.iter_errors(lexeme))
        if got_ok != want_ok:
            selftest_bad += 1
            sys.stderr.write(
                "FAIL: pattern self-test, %s = %r: schema %s it, expected %s\n  %s\n"
                % (pos, lexeme, "accepts" if got_ok else "refuses",
                   "accept" if want_ok else "refuse", why))
if selftest_bad:
    sys.stderr.write("check-ir-schema: FAIL (%d pattern self-test row(s) bad)\n" % selftest_bad)
    sys.exit(1)

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
    # written in the L4 shape (since L3 the number node, the parameter and the day kinds; since L4
    # the rules container without uniciteitUnies, taken from the schema): the rows below pin what M1
    # closed, and the schema they run against is the L4 one
    num = {"node": "Number", "whole": True, "decimals": 0, "sign": "ONBEPAALD"}
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
            "parameters": [{"node": "Parameter", "name": "vondstdrempel", "datatype": dict(num)}],
            "domains": [{"node": "Domain", "name": "Vondstsoort",
                         "datatype": {"node": "Enumeratie", "waarden": ["hout", "touw"]}}],
            "unitSystems": [],
            "dimensions": [],
            "timelines": [{"node": "Tijdlijn", "naam": "penningjaar", "variabelStartpunt": False,
                           "granulariteit": "JAAR", "aantal": 1}],
            "dayKinds": [],
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
    ("Parameter.name = null", lambda d: d["model"]["parameters"][0].update(name=None), False,
     "B2: with the map key gone, the name is the declaration's only name (written 'name' "
     "since L3)"),
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
# the listed fold, whose element type does not make it a ranged fold. In the reasons, a
# B-number is a delta of spec/proposals/V1-WIREMIGRATIE.md, and a D- or U-number a decision listed
# in the L2 card (backlog/done/20260923-l2-referentielaag.md). Since L4 the base is written in the
# L4 shape (a rule, allocation, uniqueness and fact creation are forms with versions, a fold is an
# Aggregation, a fact creation's steps are its path), and each row names the position it mutates
# by its L4 name: the closure it pins is L2's, at the place the L4 shape keeps it.
_ALWAYS = {"node": "Geldigheid", "vanaf": None, "tot": None}


def _l2_base():
    d = _m1_base()
    num = {"node": "Number", "whole": True, "decimals": 0, "sign": "ONBEPAALD"}
    rb = lambda rol: {"node": "RolBinding", "feittype": "vondstregistratie", "rol": rol}
    ref = lambda attr, ot: {"node": "AttribuutRef", "attribuut": attr, "objecttype": ot}
    def fold(kind, args, el, rng=None):
        f = {"node": "Aggregation", "kind": kind, "arguments": args}
        if el is not None:
            f["elementType"] = el
        if rng is not None:
            f["sourceRole"] = rng
        f.update(type=dict(num))
        return f

    def rule(name, value, condition):
        version = {"validity": dict(_ALWAYS), "target": ref("vondstwaarde", "Jutter"), "value": value}
        if condition is not None:
            version["condition"] = condition
        version.update(initialisation=False, presentation="BEREKENING")
        return {"node": "Rule", "name": name, "versions": [version]}
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
    som = fold("SOM", [ref("gewicht", "Vondst")], "Vondst", rb("vondst"))
    count = fold("AANTAL", [{"node": "InstanceSetUnion", "branches": [{
        "node": "InstanceSetBranch", "elementType": "Vondst", "sourceRole": rb("vondst"),
        "filter": None}]}], None)
    check = {"node": "Kenmerk", "kenmerk": "aangespoeld", "negatie": False, "eigenaar": None,
             "periode": None}
    condition = {"node": "Samengesteld", "kwantor": "ALLE", "drempel": 0,
                 "onderdelen": [
                     {"node": "RoleMembership", "factType": "vondstregistratie", "role": "vinder",
                      "negated": False},
                     {"node": "Kwantorvouw", "kwantor": "ALLE", "elementObjecttype": "Vondst",
                      "filter": None, "toets": check, "bronRol": rb("vondst")}]}
    d["rules"]["resolvedRegels"] = [
        rule("vondstwaarde", som, condition),
        rule("vondstaantal", count, None),
        rule("vondstdrempel", {"node": "ParameterRef", "parameter": "vondstdrempel"}, None),
        rule("vondstonbekend", {"node": "Onopgelost", "type": dict(num),
                                "source": {"snippet": "de onbekende vondstwaarde"}}, None),
        rule("vondstcel", {"node": "DimensieCel", "attribuut": ref("vondstwaarde", "Jutter"),
                           "labels": ["hout"]}, None),
        rule("vondstbinder", {"node": "Let", "type": dict(num), "binder": "w",
                              "value": ref("vondstwaarde", "Jutter"),
                              "body": {"node": "BinderRef", "name": "w"}}, None),
        rule("vondstvariabele", {"node": "VariabeleRef", "naam": "w"}, None),
    ]
    d["rules"]["verdelingen"] = [{
        "node": "Allocation", "name": "vondstverdeling", "versions": [{
            "sourceType": "Jutter", "sourceAttribute": "vondstwaarde", "targetType": "Vondst",
            "targetAttribute": "gewicht", "method": "GELIJKE_DELEN", "descending": False,
            "validity": dict(_ALWAYS), "connectingRole": rb("vondst")}]}]
    d["rules"]["uniciteiten"] = [{
        "node": "Uniqueness", "name": "uniek gewicht", "versions": [{
            "sources": [{"node": "Source", "objectType": "Vondst", "keys": ["gewicht"]}],
            "scope": rb("vinder"), "validity": dict(_ALWAYS)}]}]
    d["rules"]["feitCreaties"] = [{
        "node": "FactCreation", "name": "registratie", "versions": [{
            "factType": "vondstregistratie", "anchorRole": "vinder", "targetRole": "vondst",
            "anchor": "Jutter", "path": [rb("vondst"), rb("vondst")],
            "validity": dict(_ALWAYS)}]}]
    return d


def _rule(d, i):
    # the one version of the i-th rule: every position these rows mutate is a version's
    return d["rules"]["resolvedRegels"][i]["versions"][0]


def _som(d):
    return _rule(d, 0)["value"]


def _part(d, i):
    return _rule(d, 0)["condition"]["onderdelen"][i]


def _alloc(d):
    return d["rules"]["verdelingen"][0]["versions"][0]


def _uniq(d):
    return d["rules"]["uniciteiten"][0]["versions"][0]


def _fc(d):
    return d["rules"]["feitCreaties"][0]["versions"][0]


def _listed(d):
    # the listed value fold: two operands and an element type, as the producer writes it
    _rule(d, 2)["value"] = {
        "node": "Aggregation", "kind": "EERSTE",
        "arguments": [{"node": "AttribuutRef", "attribuut": "vondstwaarde",
                       "objecttype": "Jutter"}, _rule(d, 2)["value"]],
        "elementType": "Jutter", "type": None}


def _both(d, kind):
    _som(d).update(kind=kind, sourcePath=[dict(_som(d)["sourceRole"]),
                                          dict(_som(d)["sourceRole"])])


_NUM = {"node": "Number", "whole": True, "decimals": 0, "sign": "ONBEPAALD"}

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
     lambda d: _rule(d, 0)["condition"]["onderdelen"].__setitem__(0, {
         "node": "Rollidmaatschap", "rol": "vinder", "anderTeFiller": "Vondst",
         "negatie": False, "bron": None}), False,
     "B9: RoleMembership is the one role-check node, in both directions"),
    ("the L1 far-side role check where the role check stands",
     lambda d: _rule(d, 0)["condition"]["onderdelen"].__setitem__(0, {
         "node": "VerreRollidmaatschap", "filler": "Vondst", "backref": "vinder",
         "negatie": False, "bron": None}), False,
     "B9: RoleMembership is the one role-check node, in both directions"),
    ("AttribuutRef carries set", lambda d: _rule(d, 0)["target"].update(set=False), False,
     "B11, D8: the set reading is the argument of a ranged fold, not a field"),
    ("AttribuutRef carries navigatieRol",
     lambda d: _rule(d, 0)["target"].update(navigatieRol="vinder"), False,
     "B11: a navigation is its role bindings; a written name beside them could drift"),
    ("AttribuutRef carries navigatiePad",
     lambda d: _rule(d, 0)["target"].update(navigatiePad=["vinder"]), False,
     "B11: a navigation is its role bindings; a written path beside them could drift"),
    ("AttribuutRef carries onderwerpBron",
     lambda d: _rule(d, 0)["target"].update(onderwerpBron="Jutter"), False,
     "B11: the context fixes the subject; the written subject spelling left the wire"),
    ("AttribuutRef carries type", lambda d: _rule(d, 0)["target"].update(type=dict(_NUM)), False,
     "B13: the declaration is the one source of a reference's type"),
    ("ParameterRef carries type", lambda d: _rule(d, 2)["value"].update(type=dict(_NUM)), False,
     "B13: the declaration is the one source of a reference's type"),
    ("DimensieCel carries type", lambda d: _rule(d, 4)["value"].update(type=dict(_NUM)), False,
     "B13: the declaration is the one source of a reference's type"),
    ("BinderRef carries type",
     lambda d: _rule(d, 5)["value"]["body"].update(type=dict(_NUM)), False,
     "B13: a binder's type is its Let's, written once"),
    ("VariabeleRef carries type", lambda d: _rule(d, 6)["value"].update(type=dict(_NUM)),
     False, "B13, D13: the variable reference loses its type with the other four classes"),
    ("Aggregation SOM with an elementType and no range", lambda d: _som(d).pop("sourceRole"),
     False, "U2, D7: an instance fold's range is written, never derived"),
    ("Aggregation TOTAAL with an elementType and no range",
     lambda d: _som(d).update(kind="TOTAAL") or _som(d).pop("sourceRole"), True,
     "U2, D8 (iv): the bindingless TOTAAL is the subject's own read (the verifier holds it)"),
    ("Aggregation SOM with both sourceRole and sourcePath", lambda d: _both(d, "SOM"), False,
     "U2: one range, one spelling"),
    ("Aggregation TOTAAL with both sourceRole and sourcePath", lambda d: _both(d, "TOTAAL"), False,
     "U2: never both, under TOTAAL too"),
    ("Aggregation EERSTE over two listed operands with an elementType and no range",
     _listed, True,
     "U2 is scoped to one argument: a listed value fold reads its operands and has no range"),
    ("Kwantorvouw without a range", lambda d: _part(d, 1).pop("bronRol"), False,
     "U2: a quantifier fold's range is written, always"),
    ("Kwantorvouw with both bronRol and bronPad",
     lambda d: _part(d, 1).update(bronPad=[dict(_part(d, 1)["bronRol"]),
                                           dict(_part(d, 1)["bronRol"])]), False,
     "U2: one range, one spelling"),
    ("AllocationVersion without connectingRole",
     lambda d: _alloc(d).pop("connectingRole"), False,
     "U2, D7: the allocation's relation is written, never derived"),
    ("InstanceSetBranch.sourceRole = null",
     lambda d: _rule(d, 1)["value"]["arguments"][0]["branches"][0].update(sourceRole=None),
     False, "U2: the selection's relation is required and non-null"),
    ("InstanceSetBranch without sourceRole",
     lambda d: _rule(d, 1)["value"]["arguments"][0]["branches"][0].pop("sourceRole"), False,
     "U2: the selection's relation is written, never derived"),
    ("FactCreationVersion carries zetVeld",
     lambda d: _fc(d).update(zetVeld="vondst"), False,
     "B7: a fact creation carries its symbolic fields alone"),
    ("FactCreationVersion carries joinVeld",
     lambda d: _fc(d).update(joinVeld="vinder"), False,
     "B7: a fact creation carries its symbolic fields alone"),
    ("FactCreationVersion without path, which carries the joining hop",
     lambda d: _fc(d).pop("path"), False,
     "B7, D5 (c): the symbolic block is the whole creation"),
    ("FactCreationVersion without factType",
     lambda d: _fc(d).pop("factType"), False,
     "B7, D5 (c): the symbolic block is the whole creation"),
    ("FactCreationVersion without anchorRole",
     lambda d: _fc(d).pop("anchorRole"), False,
     "B7, D5 (c): the symbolic block is the whole creation"),
    ("FactCreationVersion without targetRole",
     lambda d: _fc(d).pop("targetRole"), False,
     "B7, D5 (c): the symbolic block is the whole creation"),
    ("the L2 step node where a path step stands",
     lambda d: _fc(d)["path"].__setitem__(0, {"node": "Tussenstap",
                                              "rolBinding": _fc(d)["path"][0]}), False,
     "B7, D12: a creation step is its binding"),
    ("a path step carries type",
     lambda d: _fc(d)["path"][0].update(type="Vondst"), False,
     "B7, D12: a creation step is its binding; the bean fields left the step"),
    ("a path step carries veld",
     lambda d: _fc(d)["path"][0].update(veld="vondst"), False,
     "B7, D12: a creation step is its binding; the bean fields left the step"),
    ("a path step carries viaOuderVeld",
     lambda d: _fc(d)["path"][0].update(viaOuderVeld=False),
     False, "B7, D12: a creation step is its binding; the bean fields left the step"),
    ("UniquenessVersion carries scopeVeld",
     lambda d: _uniq(d).update(scopeVeld="vinder"), False,
     "B8: scopeRol alone is the scoping"),
    ("Kenmerk carries navigatie", lambda d: _part(d, 1)["toets"].update(navigatie="vondst"), False,
     "D53: the hop is navigatieBinding alone"),
    ("Onbekend as a datatype",
     lambda d: d["model"]["parameters"][0].update(datatype={"node": "Onbekend", "reden": None}),
     False, "B16, D15: Onbekend is no datatype"),
    ("Onbekend as Onopgelost.type",
     lambda d: _rule(d, 3)["value"].update(type={"node": "Onbekend", "reden": None}), False,
     "B16, D14, U10: absent is the one spelling of an unknown type; Onbekend is no datatype"),
    ("Onopgelost without type", lambda d: _rule(d, 3)["value"].pop("type"), True,
     "D14: absent is the one spelling of an unknown type"),
    ("Onopgelost with type = null", lambda d: _rule(d, 3)["value"].update(type=None), False,
     "D14, U10: absent, never null"),
]


# --- check 1c, the L3 rows (card L3, 2026-09-24) -------------------------------------------------
# L3 moves the type system to its v1 nodes and closes by schema what they no longer carry: the L2
# number, percentage, date, unit and unit-system nodes and their fields, a factor that is not an
# exact rational {num, den}, the day-kind registry beside the day kinds, a null where a field is
# written whole or not at all, and a spelling on a unit occurrence (only the declaration in a
# UnitSystem carries spellings). Same mechanism as the M1 and L2 rows: one small valid L3
# document — the L2 base plus one node of every class L3 writes, in canonical key order — and one
# mutation per row. The rows that must PASS pin the deliberate admissions: the presentation tag on
# a type annotation and a factor not in lowest terms, both refused by the verifier (WF-007,
# WF-092) with a negatief witness of their own, because one datatype union serves declarations and
# annotations alike and a schema cannot state a greatest common divisor. In the reasons, a
# B-number is a delta of spec/proposals/V1-WIREMIGRATIE.md. Since L4 the rule the base adds is a
# Rule with one version and its literal a typed value, as the L4 schema writes them.
def _l3_base():
    d = _l2_base()
    m = d["model"]
    ratio = lambda num, den: {"num": num, "den": den}
    eur = {"node": "Unit", "abbreviation": "EUR", "system": "Munt", "canonical": "EUR",
           "factorToCanonical": ratio(1, 1)}
    mnd = {"node": "Unit", "abbreviation": "mnd", "system": "Tijd", "canonical": "mnd",
           "factorToCanonical": ratio(1, 1)}
    jr = {"node": "Unit", "abbreviation": "jr", "system": "Tijd", "canonical": "mnd",
          "factorToCanonical": ratio(12, 1)}
    pct = {"node": "Unit", "abbreviation": "%", "canonical": "%", "factorToCanonical": ratio(1, 1)}
    m["unitSystems"] = [
        {"node": "UnitSystem", "name": "Munt", "units": [dict(eur, spellings=["euro", "euros"])]},
        {"node": "UnitSystem", "name": "Tijd", "units": [
            dict(mnd, spellings=["maand", "maanden"]), dict(jr, spellings=["jaar", "jaren"])]}]
    _ot(d)["attributes"] += [
        {"node": "Attribute", "name": "vondsttarief", "datatype": {
            "node": "Number", "whole": False, "decimals": 2, "sign": "NIET_NEGATIEF",
            "unit": dict(eur, denominator=dict(jr))}},
        {"node": "Attribute", "name": "vindersdeel", "datatype": {
            "node": "Number", "whole": False, "decimals": 2, "sign": "ONBEPAALD",
            "unit": dict(pct), "presentation": "percentage"}},
        {"node": "Attribute", "name": "aangemeldOp", "datatype": {"node": "Date",
                                                                  "precision": "day"}},
        {"node": "Attribute", "name": "gemeldOp", "datatype": {"node": "Date",
                                                               "precision": "millisecond"}}]
    m["parameters"].append({
        "node": "Parameter", "name": "vondsttarief",
        "datatype": {"node": "Number", "whole": False, "decimals": 2, "sign": "NIET_NEGATIEF",
                     "unit": dict(eur)},
        "timeline": {"node": "TimelineFacet", "granularity": "JAAR", "count": 1}})
    m["dayKinds"] = [{"node": "DayKind", "name": "schouwdag", "plural": "schouwdagen"}]
    d["rules"]["resolvedRegels"].append({"node": "Rule", "name": "vindersdeel", "versions": [{
        "validity": dict(_ALWAYS),
        "target": {"node": "AttribuutRef", "attribuut": "vindersdeel", "objecttype": "Jutter"},
        "value": {"node": "Literal", "value": {"number": "5"},
                  "type": {"node": "Number", "whole": True, "decimals": 0, "sign": "ONBEPAALD",
                           "unit": dict(pct)}},
        "initialisation": False, "presentation": "BEREKENING"}]})
    return d


def _l3_attr(d, name):
    return next(a for a in _ot(d)["attributes"] if a["name"] == name)


def _tarief(d):
    # a Number at a declaration, with a rate unit: EUR per jr
    return _l3_attr(d, "vondsttarief")["datatype"]


def _pct(d):
    return _l3_attr(d, "vindersdeel")["datatype"]


def _date(d):
    return _l3_attr(d, "aangemeldOp")["datatype"]


def _tijd(d):
    return d["model"]["unitSystems"][1]


def _jr(d):
    # the declared jr, in the Tijd system
    return _tijd(d)["units"][1]


def _factor(d):
    return _jr(d)["factorToCanonical"]


def _lit(d):
    # a Literal whose type annotation is a Number with the unit '%'
    return d["rules"]["resolvedRegels"][-1]["versions"][0]["value"]


def _param(d):
    return d["model"]["parameters"][1]


def _daykind(d):
    return d["model"]["dayKinds"][0]


L3_SHAPE_ROWS = [
    ("the L3 base document", lambda d: None, True,
     "the L3 shape itself: Number, Date, Unit, UnitSystem, Parameter and DayKind in canonical "
     "form, a percentage tagged at its declaration, exact rational factors, spellings on the "
     "declared units"),
    ("the presentation tag on a type annotation",
     lambda d: _lit(d)["type"].update(presentation="percentage"), True,
     "one datatype union serves declarations and annotations, so the schema cannot tell them "
     "apart; the verifier refuses the tag on an annotation (WF-007)"),
    ("a percentage Number without the tag", lambda d: _pct(d).pop("presentation"), True,
     "B14: the tag is presentation only; a Number with the unit '%' is a percentage either way"),
    ("factorToCanonical {num: 24, den: 2}", lambda d: _factor(d).update(num=24, den=2), True,
     "lowest terms is WF-092, a verifier check: a schema cannot state a greatest common divisor"),
    ("a declared unit without spellings", lambda d: _jr(d).pop("spellings"), True,
     "absent is the one spelling of a unit with no further spelling"),
    ("the bare unit: no system, canonical or factor",
     lambda d: [_jr(d).pop(k) for k in ("system", "canonical", "factorToCanonical")], True,
     "the bare form (IR-SPEC ch. 2.5); either half of the conversion pair alone is WF-092's"),
    ("Parameter without timeline", lambda d: _param(d).pop("timeline"), True,
     "absent: the parameter is not time-dependent"),
    ("DayKind without plural", lambda d: _daykind(d).pop("plural"), True,
     "absent is the one spelling of a day kind without a plural"),
    ("the L2 number node where a Number stands",
     lambda d: _l3_attr(d, "vondsttarief").update(datatype={
         "node": "Numeriek", "geheel": False, "decimalen": 2, "teken": None, "eenheid": None}),
     False, "B14: Number is the one number node"),
    ("the L2 percentage node where a Number stands",
     lambda d: _l3_attr(d, "vindersdeel").update(datatype={
         "node": "Percentage", "geheel": False, "decimalen": 2, "eenheid": None}),
     False, "B14: a percentage is a Number with the unit '%'"),
    ("the L2 day-precision date node where a Date stands",
     lambda d: _l3_attr(d, "aangemeldOp").update(datatype={"node": "DatumInDagen"}), False,
     "B15: Date is the one date node, parameterised by its precision"),
    ("the L2 millisecond-precision date node where a Date stands",
     lambda d: _l3_attr(d, "gemeldOp").update(datatype={"node": "DatumTijd"}), False,
     "B15: Date is the one date node, parameterised by its precision"),
    ("the L2 unit node where a Unit stands",
     lambda d: _tarief(d).update(unit={"node": "Eenheid", "afkorting": "EUR", "stelsel": "Munt",
                                       "canoniek": "EUR", "factorNaarCanoniek": 1,
                                       "noemer": None}), False,
     "Unit is the one unit node, its factor a rational"),
    ("the L2 unit-system node where a UnitSystem stands",
     lambda d: d["model"]["unitSystems"].__setitem__(0, {
         "node": "Eenheidstelsel", "naam": "Munt", "eenheden": {}, "aliassen": {}}), False,
     "a unit system is its name and a list of units carrying their spellings"),
    ("Number carries geheel", lambda d: _tarief(d).update(geheel=False), False,
     "B14: whole is the one spelling of the field"),
    ("Number carries decimalen", lambda d: _tarief(d).update(decimalen=2), False,
     "B14: decimals is the one spelling of the field"),
    ("Number carries teken", lambda d: _tarief(d).update(teken="NIET_NEGATIEF"), False,
     "B14: sign is the one spelling of the field"),
    ("Number carries eenheid", lambda d: _tarief(d).update(eenheid=None), False,
     "B14: unit is the one spelling of the field, and absent the one spelling of no unit"),
    ("Unit carries afkorting", lambda d: _tarief(d)["unit"].update(afkorting="EUR"), False,
     "abbreviation is the one spelling of the field"),
    ("Unit carries stelsel", lambda d: _tarief(d)["unit"].update(stelsel="Munt"), False,
     "system is the one spelling of the field"),
    ("Unit carries canoniek", lambda d: _tarief(d)["unit"].update(canoniek="EUR"), False,
     "canonical is the one spelling of the field"),
    ("Unit carries factorNaarCanoniek",
     lambda d: _tarief(d)["unit"].update(factorNaarCanoniek=1), False,
     "B17 (i): factorToCanonical is the one spelling of the factor, as a rational"),
    ("Unit carries noemer", lambda d: _tarief(d)["unit"].update(noemer=None), False,
     "denominator is the one spelling of a rate's denominator"),
    ("Parameter carries naam", lambda d: _param(d).update(naam="vondsttarief"), False,
     "B2: name is the declaration's one name"),
    ("Parameter carries granulariteit", lambda d: _param(d).update(granulariteit="JAAR"), False,
     "B18: the timeline facet is the one spelling of a parameter's time-dependence"),
    ("UnitSystem carries naam", lambda d: _tijd(d).update(naam="Tijd"), False,
     "B2: name is the declaration's one name"),
    ("UnitSystem carries eenheden", lambda d: _tijd(d).update(eenheden={}), False,
     "units is the one list of a system's units"),
    ("UnitSystem carries aliassen", lambda d: _tijd(d).update(aliassen={}), False,
     "a unit's further spellings are on the declared unit (spellings), not in a map beside it"),
    ("the model carries dagsoortPerEenheid",
     lambda d: d["model"].update(dagsoortPerEenheid={"schouwdagen": "schouwdag"}), False,
     "a day kind carries its own spellings (name, plural); there is no registry beside it"),
    ("dayKinds as a list of strings", lambda d: d["model"].update(dayKinds=["schouwdag"]), False,
     "a day kind is a DayKind node"),
    ("DayKind.plural = null", lambda d: _daykind(d).update(plural=None), False,
     "absent is the one spelling of no plural"),
    ("factorToCanonical as a JSON number", lambda d: _jr(d).update(factorToCanonical=12), False,
     "B17 (i): the factor is an exact rational {num, den}, never a number that may be rounded"),
    ("factorToCanonical as a string", lambda d: _jr(d).update(factorToCanonical="12/1"), False,
     "B17 (i): the factor is the object {num, den}"),
    ("factorToCanonical without den", lambda d: _factor(d).pop("den"), False,
     "B17 (i): both halves of the rational are always written"),
    ("factorToCanonical without num", lambda d: _factor(d).pop("num"), False,
     "B17 (i): both halves of the rational are always written"),
    ("factorToCanonical with den = 0", lambda d: _factor(d).update(den=0), False,
     "a conversion factor is a positive rational: den is at least 1"),
    ("factorToCanonical with num = 0", lambda d: _factor(d).update(num=0), False,
     "a conversion factor is a positive rational: num is at least 1"),
    ("factorToCanonical with a fractional num", lambda d: _factor(d).update(num=1.5), False,
     "num and den are integers"),
    ("factorToCanonical {num: 2^62 - 1, den: 2^62 - 1}",
     lambda d: _factor(d).update(num=2 ** 62 - 1, den=2 ** 62 - 1), True,
     "the largest integer every loader reads: the bound itself is admitted"),
    ("factorToCanonical with num = 2^62", lambda d: _factor(d).update(num=2 ** 62), False,
     "every loader reads num and den as integers of at most 2^62 - 1 and refuses a larger one"),
    ("factorToCanonical with den = 2^62", lambda d: _factor(d).update(den=2 ** 62), False,
     "every loader reads num and den as integers of at most 2^62 - 1 and refuses a larger one"),
    ("factorToCanonical carries node", lambda d: _factor(d).update(node="Rational"), False,
     "the factor is a value inside the Unit, not a node: it carries no discriminator"),
    ("presentation = 'PERCENTAGE'", lambda d: _pct(d).update(presentation="PERCENTAGE"), False,
     "B14: 'percentage' is the one value of the tag"),
    ("presentation = null", lambda d: _pct(d).update(presentation=None), False,
     "absent is the one spelling of an untagged Number"),
    ("sign = null", lambda d: _tarief(d).update(sign=None), False,
     "a number declared without a sign is ONBEPAALD; null is not a sign"),
    ("Number without sign", lambda d: _tarief(d).pop("sign"), False,
     "the sign is always written"),
    ("Number.unit = null", lambda d: _tarief(d).update(unit=None), False,
     "absent is the one spelling of a number without a unit"),
    ("Date without precision", lambda d: _date(d).pop("precision"), False,
     "B15: day and millisecond share no order (WF-036), so the precision is always written"),
    ("Date.precision = 'DAG'", lambda d: _date(d).update(precision="DAG"), False,
     "B15: the precision has the spellings day and millisecond, and no other"),
    ("Unit.system = null", lambda d: _tarief(d)["unit"].update(system=None), False,
     "no unit field is ever null: absent is the one spelling"),
    ("Unit.denominator = null", lambda d: _tarief(d)["unit"].update(denominator=None), False,
     "no unit field is ever null: absent is the one spelling of a plain unit"),
    ("a unit occurrence carries spellings",
     lambda d: _tarief(d)["unit"].update(spellings=["euro"]), False,
     "only the declaration in a UnitSystem carries spellings; on an occurrence they would be "
     "accepted and dropped"),
    ("a declared unit with spellings = []", lambda d: _jr(d).update(spellings=[]), False,
     "absent is the one spelling of no further spelling"),
    ("Parameter's facet carries timelineName",
     lambda d: _param(d)["timeline"].update(timelineName="penningjaar"), False,
     "B18: no parameter records a timeline name, so the position is closed"),
    ("Parameter.timeline = null", lambda d: _param(d).update(timeline=None), False,
     "absent is the one spelling of a parameter that is not time-dependent"),
    ("Parameter.datatype = null", lambda d: _param(d).update(datatype=None), False,
     "a parameter has a datatype (IR-SPEC ch. 2.10)"),
]


# --- check 1c, the L4 rows (card L4, 2026-09-24) -------------------------------------------------
# L4 moves the rule layer and the expression layer to their v1 forms and closes by schema what they
# no longer carry: the v0 rule-form nodes and their fields, the rule kind as a field, the
# placeholder allocation method, the fact creation's target type and step node, the union list and
# its source node, the overloaded function node, the context-value grab-bag, and a literal's
# lexeme. It closes as well what the new forms cannot carry: an empty list of versions, sources,
# keys or path steps, a scoped union, the presentation tag beside the initialisation flag, a form
# name spelled as a version, a fold field on a builtin call or a bucket on an aggregation, a kind of
# the other node, a literal value that is not exactly one variant, and a context value without a
# name or type. Same mechanism as the M1, L2 and L3 rows: one small valid L4 document — the L3 base
# plus a form of every named kind with its versions, an aggregation and a builtin call with their
# optional fields, a literal of every variant, a context value and a bound day, in canonical key
# order — and one mutation per row. The rows that must PASS pin the deliberate admissions, each a
# verifier rule with a negatief witness of its own: a nameless or blank form (WF-107), a null
# reference name (WF-029, WF-034), an empty allocation attribute (WF-034), a padded statute
# annotation (WF-082), a builtin call without a type (WF-038), a typeless literal (WF-034), a value
# variant its literal's type does not name (WF-114), and a context value named outside the closed
# set (WF-098); the other accepted rows pin the absences that have a meaning of their own. In the
# reasons, a B-number is a delta of spec/proposals/V1-WIREMIGRATIE.md.
def _l4_base():
    d = _l3_base()
    m, r = d["model"], d["rules"]
    num = {"node": "Number", "whole": True, "decimals": 0, "sign": "ONBEPAALD"}
    euro = {"node": "Number", "whole": False, "decimals": 2, "sign": "NIET_NEGATIEF"}
    rb = lambda rol: {"node": "RolBinding", "feittype": "vondstregistratie", "rol": rol}
    ref = lambda attr, ot: {"node": "AttribuutRef", "attribuut": attr, "objecttype": ot}
    lit = lambda value, typ: {"node": "Literal", "value": value, "type": typ}
    kenmerk = lambda naam: {"node": "Kenmerk", "kenmerk": naam, "negatie": False, "eigenaar": None,
                            "periode": None}
    soort = {"node": "Enumeratie", "domein": "Vondstsoort", "waarden": ["hout", "touw"]}
    plaats = [{"name": "Wad", "payload": None}, {"name": "Strand", "payload": None}]

    def version(target, value, condition=None, initialisation=False, presentation=None,
                validity=None):
        v = {"validity": validity or dict(_ALWAYS), "target": target, "value": value}
        if condition is not None:
            v["condition"] = condition
        v["initialisation"] = initialisation
        if presentation is not None:
            v["presentation"] = presentation
        return v

    def rule(name, *versions):
        return {"node": "Rule", "name": name, "versions": list(versions)}

    m["domains"].append({"node": "Domain", "name": "Vindplaats",
                         "datatype": {"node": "SumType", "constructors": plaats}})
    _ot(d)["attributes"] += [
        {"node": "Attribute", "name": "bijnaam", "datatype": {"node": "Tekst"}},
        {"node": "Attribute", "name": "vondstsoort", "datatype": dict(soort)},
        {"node": "Attribute", "name": "vindplaats",
         "datatype": {"node": "SumType", "domain": "Vindplaats", "constructors": plaats}}]
    r["resolvedRegels"] += [
        # two versions under one name, with adjacent windows and one list of annotations
        dict(rule("jaartarief",
                  version(ref("vondsttarief", "Jutter"), lit({"number": "12.50"}, dict(euro)),
                          presentation="BEREKENING",
                          validity={"node": "Geldigheid", "vanaf": "2025-01-01",
                                    "tot": "2025-12-31"}),
                  version(ref("vondsttarief", "Jutter"), lit({"number": "3/4"}, dict(euro)),
                          condition=kenmerk("aangemeld"), presentation="GELIJKSTELLING",
                          validity={"node": "Geldigheid", "vanaf": "2026-01-01", "tot": None})),
             statuteAnnotations=["art. 3 Jutterswet"]),
        # a characteristic write carries neither the flag nor the tag; an initialisation the flag
        rule("aanmelding", version(ref("aangemeld", "Jutter"),
                                   lit({"boolean": True}, {"node": "Booleaan"}))),
        rule("beginwaarde", version(ref("vondstwaarde", "Jutter"), lit({"number": "0"}, dict(num)),
                                    initialisation=True)),
        rule("bijnaam", version(ref("bijnaam", "Jutter"), lit({"text": "de jutter"}, {"node": "Tekst"}),
                                presentation="GELIJKSTELLING")),
        rule("aanmelddatum", version(ref("aangemeldOp", "Jutter"),
                                     lit({"date": "2026-01-01"}, {"node": "Date", "precision": "day"}),
                                     presentation="GELIJKSTELLING")),
        rule("meldtijdstip", version(ref("gemeldOp", "Jutter"),
                                     lit({"dateTime": "2026-03-15T12:00:00.250"},
                                         {"node": "Date", "precision": "millisecond"}),
                                     presentation="GELIJKSTELLING")),
        rule("vondstsoort", version(ref("vondstsoort", "Jutter"),
                                    lit({"enumeration": "hout"}, dict(soort)),
                                    presentation="GELIJKSTELLING")),
        rule("vindplaats", version(ref("vindplaats", "Jutter"),
                                   lit({"constructor": "Wad"},
                                       {"node": "SumType", "domain": "Vindplaats",
                                        "constructors": plaats}),
                                   presentation="GELIJKSTELLING")),
        # an aggregation with every field a fold carries, over a two-step range
        rule("vondstsom", version(ref("vondstwaarde", "Jutter"), {
            "node": "Aggregation", "kind": "SOM", "arguments": [ref("gewicht", "Vondst")],
            "elementType": "Vondst", "filter": kenmerk("aangespoeld"),
            "emptyFallback": lit({"number": "0"}, dict(num)),
            "sourcePath": [rb("vondst"), rb("vondst")], "type": dict(num)},
            presentation="BEREKENING")),
        # a dimension fold: no element type, so no range
        rule("vakkensom", version(ref("vondstwaarde", "Jutter"), {
            "node": "Aggregation", "kind": "SOM", "arguments": [ref("vondstwaarde", "Jutter")],
            "dimensionSelection": {"node": "DimensieSelectie", "dimensie": "kustvak",
                                   "labels": ["noord", "zuid"]},
            "type": dict(num)}, presentation="BEREKENING")),
        # a builtin call with its bucket and its gedurende condition, and one over a session value
        rule("tariefdeel", version(ref("vondsttarief", "Jutter"), {
            "node": "BuiltinCall", "kind": "TIJDSEVENREDIG_DEEL",
            "arguments": [ref("vondsttarief", "Jutter")], "filter": kenmerk("aangemeld"),
            "granularity": "MAAND", "type": dict(euro)}, presentation="BEREKENING")),
        rule("rekendag", version(ref("vondstwaarde", "Jutter"), {
            "node": "BuiltinCall", "kind": "DAG_UIT", "arguments": [
                {"node": "ContextValue", "name": "Rekendatum",
                 "type": {"node": "Date", "precision": "day"}}],
            "type": dict(num)}, presentation="BEREKENING")),
    ]
    r["resolvedDagsoorten"] = [{
        "node": "ResolvedDagsoort", "naam": "schouwdag",
        "voorwaarde": {"node": "Vergelijking", "op": "GELIJK", "links": {
            "node": "BuiltinCall", "kind": "DAG_UIT", "arguments": [
                {"node": "BoundDay", "type": {"node": "Date", "precision": "day"},
                 "source": {"snippet": "de dag"}}],
            "type": dict(num)},
            "rechts": lit({"number": "1"}, dict(num)), "periode": None},
        "geldigheid": None}]
    r["objectCreaties"] = [{"node": "ObjectCreation", "name": "muntregistratie", "versions": [{
        "createdType": "Muntvondst", "subject": "Jutter", "initialisations": [
            {"node": "Toekenning", "attribuut": "gewicht", "waarde": lit({"number": "1"}, dict(num)),
             "kenmerk": False},
            {"node": "Toekenning", "attribuut": "aangespoeld",
             "waarde": lit({"boolean": True}, {"node": "Booleaan"}), "kenmerk": True}],
        "condition": kenmerk("aangemeld"), "validity": dict(_ALWAYS),
        "connectingRole": rb("vondst")}]}]
    # every optional field of an allocation at once: which method admits which is WF-025's
    r["verdelingen"].append({"node": "Allocation", "name": "vondstverdeling naar rato",
                             "versions": [{
        "sourceType": "Jutter", "sourceAttribute": "vondstwaarde", "targetType": "Vondst",
        "targetAttribute": "gewicht", "method": "NAAR_RATO", "ratioAttribute": "gewicht",
        "maximumAttribute": "gewicht", "roundingDecimals": 2, "remainderAttribute": "vondstwaarde",
        "orderingAttribute": "gewicht", "descending": True, "condition": kenmerk("aangemeld"),
        "validity": dict(_ALWAYS), "connectingRole": rb("vondst")}]})
    r["consistenties"] = [{"node": "Consistency", "name": "gewicht positief", "versions": [{
        "subject": "Vondst",
        "assertion": {"node": "Vergelijking", "op": "GROTER", "links": ref("gewicht", "Vondst"),
                      "rechts": lit({"number": "0"}, dict(num)), "periode": None},
        "validity": dict(_ALWAYS),
        "source": {"snippet": "Het gewicht van een vondst is groter dan 0.", "line": 12}}]}]
    r["uniciteiten"].append({"node": "Uniqueness", "name": "unieke registratie", "versions": [{
        "sources": [{"node": "Source", "objectType": "Jutter", "keys": ["vondstwaarde"]},
                    {"node": "Source", "objectType": "Vondst", "keys": ["gewicht"]}],
        "validity": dict(_ALWAYS)}]})
    return d


def _form(d, lst, name):
    return next(f for f in d["rules"][lst] if f["name"] == name)


def _ver(d, lst, name, i=0):
    return _form(d, lst, name)["versions"][i]


def _rv(d, name, i=0):
    return _ver(d, "resolvedRegels", name, i)


def _val(d, name):
    return _rv(d, name)["value"]


# the one form of each named kind the rows mutate, and its first version
_FORMS = [("Rule", "resolvedRegels", "jaartarief"),
          ("Allocation", "verdelingen", "vondstverdeling naar rato"),
          ("ObjectCreation", "objectCreaties", "muntregistratie"),
          ("FactCreation", "feitCreaties", "registratie"),
          ("Consistency", "consistenties", "gewicht positief"),
          ("Uniqueness", "uniciteiten", "unieke registratie")]


def _set(getter, key, value):
    return lambda d: getter(d).update({key: value})


def _pop(getter, key):
    return lambda d: getter(d).pop(key)


def _v0_keys(where, getter, keys, why):
    # one row per v0 key the L4 node or version record no longer carries
    return [("%s carries the v0 key %s" % (where, k), _set(getter, k, None), False, why)
            for k in keys]


_SOM = lambda d: _val(d, "vondstsom")
_DAG_UIT = lambda d: _val(d, "rekendag")
_CTX = lambda d: _val(d, "rekendag")["arguments"][0]
_BOUND = lambda d: d["rules"]["resolvedDagsoorten"][0]["voorwaarde"]["links"]["arguments"][0]
_DEEL = lambda d: _val(d, "tariefdeel")
_NUMLIT = lambda d: _val(d, "beginwaarde")
_ALLOC = lambda d: _ver(d, "verdelingen", "vondstverdeling naar rato")
_OC = lambda d: _ver(d, "objectCreaties", "muntregistratie")
_FC = lambda d: _ver(d, "feitCreaties", "registratie")
_CONS = lambda d: _ver(d, "consistenties", "gewicht positief")
_UNION = lambda d: _ver(d, "uniciteiten", "unieke registratie")
_SINGLE = lambda d: _ver(d, "uniciteiten", "uniek gewicht")


def _v0_rule(d):
    v = _rv(d, "beginwaarde")
    d["rules"]["resolvedRegels"][0] = {
        "node": "ResolvedRegel", "naam": "beginwaarde", "soort": "INITIALISATIE",
        "doel": v["target"], "waarde": v["value"], "voorwaarde": None, "geldigheid": None}


L4_SHAPE_ROWS = [
    ("the L4 base document", lambda d: None, True,
     "the L4 shape itself: the six named forms with their versions, a two-version rule with its "
     "statute annotations, an aggregation and a builtin call with their optional fields, a literal "
     "of every variant, a context value and a bound day, in canonical key order"),
    # --- the container and the v0 nodes -----------------------------------------------------------
    ("rules carries uniciteitUnies", lambda d: d["rules"].update(uniciteitUnies=[]), False,
     "B27: one list of uniqueness forms; the v0 union list left the wire, as a key and as an empty "
     "list"),
    ("the v0 rule node where a Rule stands", _v0_rule, False,
     "B19, B20: a rule is {node, name, versions}, and its kind is no field"),
    ("the v0 allocation node where an Allocation stands",
     lambda d: d["rules"]["verdelingen"].__setitem__(0, {
         "node": "ResolvedVerdeling", "naam": "vondstverdeling", "bronType": "Jutter",
         "bronAttribuut": "vondstwaarde", "doelType": "Vondst", "doelAttribuut": "gewicht",
         "methode": "GELIJKE_DELEN", "ratoAttribuut": None, "maximumAttribuut": None,
         "afrondingDecimalen": None, "restantAttribuut": None, "groepeerAttribuut": None,
         "afnemend": False, "voorwaarde": None, "geldigheid": None,
         "verbindendeRol": _ALLOC(d)["connectingRole"]}), False,
     "B19: an allocation is {node, name, versions}"),
    ("the v0 object-creation node where an ObjectCreation stands",
     lambda d: d["rules"]["objectCreaties"].__setitem__(0, {
         "node": "ResolvedObjectCreatie", "naam": "muntregistratie", "nieuwType": "Muntvondst",
         "subject": "Jutter", "toekenningen": [], "voorwaarde": None, "geldigheid": None}), False,
     "B19: an object creation is {node, name, versions}"),
    ("the v0 fact-creation node where a FactCreation stands",
     lambda d: d["rules"]["feitCreaties"].__setitem__(0, {
         "node": "ResolvedFeitCreatie", "naam": "registratie", "anker": "Jutter", "doel": "Vondst",
         "tussenstappen": [], "voorwaarde": None, "geldigheid": None,
         "feittype": "vondstregistratie", "ankerRol": "vinder", "doelRol": "vondst",
         "joinRol": _FC(d)["path"][-1]}), False,
     "B19: a fact creation is {node, name, versions}"),
    ("the v0 consistency node where a Consistency stands",
     lambda d: d["rules"]["consistenties"].__setitem__(0, {
         "node": "ResolvedConsistentie", "naam": "gewicht positief", "subject": "Vondst",
         "assertie": _CONS(d)["assertion"], "bron": None, "line": 12, "geldigheid": None}), False,
     "B19: a consistency is {node, name, versions}"),
    ("the v0 single uniqueness node where a Uniqueness stands",
     lambda d: d["rules"]["uniciteiten"].__setitem__(0, {
         "node": "ResolvedUniciteit", "naam": "uniek gewicht", "subject": "Vondst",
         "sleutels": ["gewicht"], "bron": None, "geldigheid": None}), False,
     "B19, B27: a uniqueness is {node, name, versions}, its sources named"),
    ("the v0 union node where a Uniqueness stands",
     lambda d: d["rules"]["uniciteiten"].__setitem__(1, {
         "node": "ResolvedUniciteitUnie", "naam": "unieke registratie", "bronnen": [],
         "bron": None, "geldigheid": None}), False,
     "B27: the union is the uniqueness with two or more sources"),
    ("the v0 union source node where a Source stands",
     lambda d: _UNION(d)["sources"].__setitem__(0, {"node": "Bron", "type": "Jutter",
                                                    "sleutels": ["vondstwaarde"]}), False,
     "B27: a source is {node: Source, objectType, keys}"),
    ("the v0 function node where an aggregation stands",
     lambda d: _rv(d, "vondstsom").update(value={
         "node": "Functie", "kind": "SOM", "argumenten": [], "elementObjecttype": None,
         "leegwaarde": None, "filter": None, "dimensieSelectie": None, "type": None,
         "bron": None, "granulariteit": None}), False,
     "B21: an aggregation and a builtin call are two nodes"),
    ("the v0 function node where a builtin call stands",
     lambda d: _rv(d, "rekendag").update(value={
         "node": "Functie", "kind": "DAG_UIT", "argumenten": [], "elementObjecttype": None,
         "leegwaarde": None, "filter": None, "dimensieSelectie": None, "type": None,
         "bron": None, "granulariteit": None}), False,
     "B21: an aggregation and a builtin call are two nodes"),
    ("the v0 constant node where a context value stands",
     lambda d: _DAG_UIT(d)["arguments"].__setitem__(0, {
         "node": "Constante", "naam": "Rekendatum", "type": {"node": "Date", "precision": "day"},
         "bron": None}), False,
     "B23: a session value is a ContextValue"),
    ("the v0 constant node where the bound day stands",
     lambda d: d["rules"]["resolvedDagsoorten"][0]["voorwaarde"]["links"]["arguments"].__setitem__(
         0, {"node": "Constante", "naam": "de dag", "type": {"node": "Date", "precision": "day"},
             "bron": "de dag"}), False,
     "B23: the bound day of a day-kind definition is a BoundDay"),
    # --- the named forms and their versions -------------------------------------------------------
] + [
    ("%s without versions" % node, _pop(lambda d, l=lst, n=name: _form(d, l, n), "versions"), False,
     "B19: a form carries its versions (IR-SPEC ch. 3, Versioning)")
    for node, lst, name in _FORMS
] + [
    ("%s with versions = []" % node, _set(lambda d, l=lst, n=name: _form(d, l, n), "versions", []),
     False, "B19: a form has at least one version (IR-SPEC ch. 3: versions >= 1)")
    for node, lst, name in _FORMS
] + [
    ("%sVersion carries node" % node,
     _set(lambda d, l=lst, n=name: _ver(d, l, n), "node", node + "Version"), False,
     "B19: a version record is part of its form and carries no discriminator")
    for node, lst, name in _FORMS
] + [
    ("%s named 'x #2'" % node, _set(lambda d, l=lst, n=name: _form(d, l, n), "name", "x #2"),
     False, "B19: that is the spelling of the second version of a form with several, on the v0 "
     "wire and in the readers' in-memory records")
    for node, lst, name in _FORMS
] + [
    ("Rule.name = null", _set(lambda d: _form(d, "resolvedRegels", "jaartarief"), "name", None),
     True, "a nameless form is WF-107's, a verifier rule with its own witness"),
    ("Rule.name = ''", _set(lambda d: _form(d, "resolvedRegels", "jaartarief"), "name", ""),
     True, "a blank name is WF-107's, a verifier rule with its own witness"),
    ("Rule without name", _pop(lambda d: _form(d, "resolvedRegels", "jaartarief"), "name"),
     False, "the name is always written; a form without one writes null"),
    ("Rule without statuteAnnotations",
     _pop(lambda d: _form(d, "resolvedRegels", "jaartarief"), "statuteAnnotations"), True,
     "absent is the one spelling of a rule without annotations"),
    ("Rule.statuteAnnotations = []",
     _set(lambda d: _form(d, "resolvedRegels", "jaartarief"), "statuteAnnotations", []), False,
     "absent is the one spelling of no annotation (IR-SPEC ch. 3.1)"),
    ("Rule.statuteAnnotations = ['']",
     _set(lambda d: _form(d, "resolvedRegels", "jaartarief"), "statuteAnnotations", [""]), False,
     "an annotation is a non-empty string"),
    ("Rule.statuteAnnotations with a padded entry",
     _set(lambda d: _form(d, "resolvedRegels", "jaartarief"), "statuteAnnotations",
          [" art. 3 Jutterswet "]), True,
     "the trimming is WF-082's, a verifier rule with its own witness"),
    ("a RuleVersion carries statuteAnnotations",
     _set(lambda d: _rv(d, "jaartarief"), "statuteAnnotations", ["art. 3 Jutterswet"]), False,
     "B19: the annotations are the form's, one list for all its versions"),
] + _v0_keys("Rule", lambda d: _form(d, "resolvedRegels", "jaartarief"),
             ["naam", "soort", "bronAnnotaties"],
             "B19: a rule carries name, versions and statuteAnnotations") + [
    # --- the rule version -------------------------------------------------------------------------
    ("RuleVersion without initialisation", _pop(lambda d: _rv(d, "jaartarief"), "initialisation"),
     False, "B20: the initialisation flag is always written"),
    ("RuleVersion.initialisation = null",
     _set(lambda d: _rv(d, "jaartarief"), "initialisation", None), False,
     "B20: the flag is a boolean"),
    ("RuleVersion.validity = null", _set(lambda d: _rv(d, "jaartarief"), "validity", None), False,
     "every version writes its validity; always is a Geldigheid with both bounds null"),
    ("RuleVersion without validity", _pop(lambda d: _rv(d, "jaartarief"), "validity"), False,
     "every version writes its validity (IR-SPEC ch. 3.10)"),
    ("RuleVersion.presentation = 'KENMERKTOEKENNING'",
     _set(lambda d: _rv(d, "jaartarief"), "presentation", "KENMERKTOEKENNING"), False,
     "B20: the tag has the two values of a computing version; a characteristic write is a "
     "version without it"),
    ("RuleVersion.presentation = 'INITIALISATIE'",
     _set(lambda d: _rv(d, "jaartarief"), "presentation", "INITIALISATIE"), False,
     "B20: the initialisation is the flag, not a tag"),
    ("RuleVersion.presentation = null",
     _set(lambda d: _rv(d, "jaartarief"), "presentation", None), False,
     "absent is the one spelling of no tag"),
    ("RuleVersion with initialisation true and a presentation",
     _set(lambda d: _rv(d, "jaartarief"), "initialisation", True), False,
     "the initialisation flag and the presentation tag never stand together: an initialisation "
     "has no surface form of its own"),
    ("RuleVersion with initialisation true and no presentation",
     lambda d: _rv(d, "jaartarief").update(initialisation=True) or
     _rv(d, "jaartarief").pop("presentation"), True, "the initialisation"),
    ("RuleVersion with initialisation false and no presentation",
     _pop(lambda d: _rv(d, "jaartarief"), "presentation"), True,
     "a characteristic write; that its target names one is WF-022's"),
    ("RuleVersion without condition", _pop(lambda d: _rv(d, "jaartarief", 1), "condition"), True,
     "absent is unconditional"),
    ("RuleVersion.condition = null", _set(lambda d: _rv(d, "jaartarief", 1), "condition", None),
     False, "absent is the one spelling of unconditional"),
    ("RuleVersion.target = null", _set(lambda d: _rv(d, "jaartarief"), "target", None), False,
     "the target is always written (IR-SPEC ch. 3.1)"),
    ("RuleVersion whose target is a ParameterRef",
     _set(lambda d: _rv(d, "jaartarief"), "target",
          {"node": "ParameterRef", "parameter": "vondsttarief"}), False,
     "the target is an attribute reference and nothing else"),
    ("RuleVersion.value = null", _set(lambda d: _rv(d, "jaartarief"), "value", None), False,
     "the value is always written (IR-SPEC ch. 3.1)"),
] + _v0_keys("RuleVersion", lambda d: _rv(d, "jaartarief"),
             ["naam", "soort", "doel", "waarde", "voorwaarde", "geldigheid"],
             "B19, B20: a version writes validity, target, value, condition, initialisation and "
             "presentation") + [
    # --- allocation, object creation, fact creation, consistency ----------------------------------
    ("AllocationVersion.method = 'COMPLEX'", _set(_ALLOC, "method", "COMPLEX"), False,
     "the v0 placeholder of an unhandled form is no method (IR-SPEC ch. 3.2)"),
    ("AllocationVersion.method = null", _set(_ALLOC, "method", None), False,
     "the method is always one of the three"),
    ("AllocationVersion with its optional fields absent",
     lambda d: [_ALLOC(d).pop(k) for k in ("ratioAttribute", "maximumAttribute",
                                           "roundingDecimals", "remainderAttribute",
                                           "orderingAttribute", "condition")], True,
     "absent is none"),
    ("AllocationVersion.ratioAttribute = ''", _set(_ALLOC, "ratioAttribute", ""), True,
     "an empty attribute name is WF-034's, a verifier rule with its own witness"),
    ("AllocationVersion.sourceType = null", _set(_ALLOC, "sourceType", None), True,
     "WF-034 holds the presence of the four type and attribute fields"),
] + [
    ("AllocationVersion.%s = null" % k, _set(_ALLOC, k, None), False,
     "absent is the one spelling of none")
    for k in ("ratioAttribute", "maximumAttribute", "roundingDecimals", "remainderAttribute",
              "orderingAttribute")
] + _v0_keys("AllocationVersion", _ALLOC,
             ["naam", "bronType", "bronAttribuut", "doelType", "doelAttribuut", "methode",
              "ratoAttribuut", "maximumAttribuut", "afrondingDecimalen", "restantAttribuut",
              "groepeerAttribuut", "afnemend", "voorwaarde", "geldigheid", "verbindendeRol"],
             "B19: an allocation version writes the fields of IR-SPEC ch. 3.2") + [
    ("ObjectCreationVersion without connectingRole", _pop(_OC, "connectingRole"), True,
     "absent: the back-link is derived (RUNTIME-SEMANTICS ch. 5.5)"),
    ("ObjectCreationVersion.connectingRole = null", _set(_OC, "connectingRole", None), False,
     "written whole or not at all"),
    ("ObjectCreationVersion.subject = null", _set(_OC, "subject", None), True,
     "WF-034 holds the subject's presence, with its own witness"),
] + _v0_keys("ObjectCreationVersion", _OC,
             ["naam", "nieuwType", "toekenningen", "voorwaarde", "geldigheid", "verbindendeRol"],
             "B19: an object creation version writes the fields of IR-SPEC ch. 3.3") + [
    ("FactCreationVersion.path = []", _set(_FC, "path", []), False,
     "a fact creation reaches its target in at least one hop (IR-SPEC ch. 3.4)"),
    ("FactCreationVersion.anchor = null", _set(_FC, "anchor", None), True,
     "a reference name of the rule layer stays nullable; that it resolves is WF-028's"),
] + _v0_keys("FactCreationVersion", _FC,
             ["naam", "anker", "doel", "tussenstappen", "voorwaarde", "geldigheid", "feittype",
              "ankerRol", "doelRol", "joinRol"],
             "B19: a fact creation version writes the fields of IR-SPEC ch. 3.4; the target type "
             "is the declared filler of targetRole") + [
    ("ConsistencyVersion.assertion = null", _set(_CONS, "assertion", None), False,
     "the assertion is always written (IR-SPEC ch. 3.5)"),
    ("ConsistencyVersion.subject = null", _set(_CONS, "subject", None), True,
     "WF-029 holds the subject's presence, with its own witness"),
] + _v0_keys("ConsistencyVersion", _CONS, ["naam", "assertie", "geldigheid"],
             "B19: a consistency version writes subject, assertion, validity and source") + [
    # --- uniqueness -------------------------------------------------------------------------------
    ("UniquenessVersion.sources = []", _set(_UNION, "sources", []), False,
     "B27: a uniqueness has at least one source (IR-SPEC ch. 3.6)"),
    ("Source.keys = []", lambda d: _UNION(d)["sources"][0].update(keys=[]), False,
     "a source has at least one key (IR-SPEC ch. 3.6)"),
    ("a union (two sources) with scope",
     _set(_UNION, "scope", {"node": "RolBinding", "feittype": "vondstregistratie",
                            "rol": "vinder"}), False,
     "a scoped union is an open design question (IR-SPEC ch. 3.6), so the position is closed"),
    ("a single-source uniqueness without scope", _pop(_SINGLE, "scope"), True,
     "absent is unscoped"),
    ("Source.objectType = null", lambda d: _UNION(d)["sources"][0].update(objectType=None), True,
     "a reference name of the rule layer stays nullable; that it resolves is WF-030's"),
] + _v0_keys("UniquenessVersion", _SINGLE,
             ["naam", "subject", "sleutels", "scopeRol", "bronnen", "geldigheid"],
             "B27: a uniqueness version writes sources, scope, validity and source") + _v0_keys(
    "Source", lambda d: _UNION(d)["sources"][0], ["type", "sleutels"],
    "B27: a source writes objectType and keys") + [
    # --- aggregation and builtin call -------------------------------------------------------------
    ("Aggregation carries granularity", _set(_SOM, "granularity", "MAAND"), False,
     "B21: the bucket is a builtin call's field"),
    ("Aggregation with kind WORTEL", _set(_SOM, "kind", "WORTEL"), False,
     "B21: a kind of the other node"),
    ("BuiltinCall with kind SOM", _set(_DEEL, "kind", "SOM"), False,
     "B21: a kind of the other node"),
    ("Aggregation.filter = null", _set(_SOM, "filter", None), False,
     "absent is the one spelling of no filter"),
    ("Aggregation.emptyFallback = null", _set(_SOM, "emptyFallback", None), False,
     "absent is the one spelling of no fallback"),
    ("Aggregation.elementType = null",
     lambda d: _val(d, "vakkensom").update(elementType=None), False,
     "absent is the one spelling of no element type"),
    ("BuiltinCall.granularity = null", _set(_DEEL, "granularity", None), False,
     "absent is the one spelling of no bucket"),
    ("BuiltinCall.granularity = 'DECENNIUM'", _set(_DEEL, "granularity", "DECENNIUM"), False,
     "the five buckets of the v0 wire, in their v0 spelling"),
    ("BuiltinCall.type = null", _set(_DEEL, "type", None), True,
     "a builtin call without a type is WF-038's, a verifier rule with its own witness"),
] + [
    ("BuiltinCall carries %s" % k, _set(_DEEL, k, v), False,
     "B21: %s is an aggregation's field" % k)
    for k, v in (("elementType", "Jutter"),
                 ("sourceRole", {"node": "RolBinding", "feittype": "vondstregistratie",
                                 "rol": "vondst"}),
                 ("sourcePath", []),
                 ("emptyFallback", {"node": "Literal", "value": {"number": "0"}, "type": None}),
                 ("dimensionSelection", {"node": "DimensieSelectie", "dimensie": "kustvak",
                                         "labels": ["noord"]}))
] + _v0_keys("Aggregation", _SOM,
             ["argumenten", "elementObjecttype", "leegwaarde", "dimensieSelectie", "bronRol",
              "bronPad", "granulariteit"],
             "B21: an aggregation writes the fields of IR-SPEC ch. 4.3") + _v0_keys(
    "BuiltinCall", _DEEL, ["argumenten", "elementObjecttype", "granulariteit"],
    "B21: a builtin call writes kind, arguments, filter, granularity, type and source") + [
    # --- literal ----------------------------------------------------------------------------------
    ("Literal carries waarde beside value", _set(_NUMLIT, "waarde", "0"), False,
     "B22: a literal carries a typed value, not a lexeme"),
    ("Literal with the v0 lexeme and no value",
     lambda d: _NUMLIT(d).pop("value") and _NUMLIT(d).update(waarde="0"), False,
     "B22: a literal carries a typed value, not a lexeme"),
    ("Literal.value = null", _set(_NUMLIT, "value", None), False,
     "the value is always written: exactly one variant"),
    ("Literal.value as a string", _set(_NUMLIT, "value", "0"), False,
     "B22: the value is a variant object"),
    ("Literal.value.number as a JSON number", _set(_NUMLIT, "value", {"number": 0}), False,
     "B22: a number is an exact string, never a JSON number that may be rounded"),
    ("Literal.value with two variants", _set(_NUMLIT, "value", {"number": "0", "text": "0"}),
     False, "exactly one variant"),
    ("Literal.value = {}", _set(_NUMLIT, "value", {}), False, "exactly one variant"),
    ("Literal.value.number = '12,50'", _set(_NUMLIT, "value", {"number": "12,50"}), False,
     "B22: a decimal point; the source's comma stays in the source snippet"),
    ("Literal.value.number = '5 \u20ac'", _set(_NUMLIT, "value", {"number": "5 \u20ac"}), False,
     "the unit is the literal's type, never part of its value"),
    ("Literal.value.date = '31-01-2024'",
     lambda d: _val(d, "aanmelddatum").update(value={"date": "31-01-2024"}), False,
     "the ISO date; the source spelling stays in the source snippet"),
    ("Literal.value.dateTime = '2026-03-15T12:00'",
     lambda d: _val(d, "meldtijdstip").update(value={"dateTime": "2026-03-15T12:00"}), False,
     "the seconds field is always written"),
    ("Literal.value = {'boolean': 'waar'}",
     lambda d: _val(d, "aanmelding").update(value={"boolean": "waar"}), False,
     "a boolean is a JSON boolean; the v0 lexeme is gone"),
    ("Literal.value constructor with a payload",
     lambda d: _val(d, "vindplaats").update(value={"constructor": "Wad",
                                                   "payload": {"number": "1"}}), False,
     "a literal names a constructor and applies none"),
    ("Literal.value = {'list': []}", _set(_NUMLIT, "value", {"list": []}), False,
     "no literal carries a list: the case format's list variant is not a literal's"),
    ("Literal.value = {'enumeration': ''}",
     lambda d: _val(d, "vondstsoort").update(value={"enumeration": ""}), False,
     "a label is a non-empty string"),
    ("Literal.value = {'text': ''}", lambda d: _val(d, "bijnaam").update(value={"text": ""}),
     True, "the case format's text variant admits the empty string, and an empty text is one"),
    ("Literal.type = null", _set(_NUMLIT, "type", None), True,
     "a typeless value node is WF-034's, a verifier rule with its own witness"),
    ("Literal whose variant is not the one its type names",
     _set(_NUMLIT, "value", {"text": "0"}), True,
     "variant against type is WF-114's: one datatype union serves every literal, so the schema "
     "cannot cross the two"),
    ("Literal with a date variant under a millisecond-precision type",
     lambda d: _val(d, "meldtijdstip").update(value={"date": "2026-03-15"}), True,
     "variant against type is WF-114's"),
    # --- context value and bound day --------------------------------------------------------------
    ("ContextValue without type", _pop(_CTX, "type"), False, "the type is always written"),
    ("ContextValue.type = null", _set(_CTX, "type", None), False, "the type is a datatype"),
    ("ContextValue.name = null", _set(_CTX, "name", None), False,
     "the name is always a string: a context value without one names nothing"),
    ("ContextValue without name", _pop(_CTX, "name"), False, "the name is always written"),
    ("ContextValue named 'de dag'", _set(_CTX, "name", "de dag"), True,
     "the schema leaves the name free: WF-098 refuses a name outside the closed set, the bound "
     "day's included, with its own witness"),
    ("BoundDay carries name", _set(_BOUND, "name", "de dag"), False,
     "B23: the bound day carries no name"),
    ("BoundDay without type", _pop(_BOUND, "type"), False, "the type is always written"),
    ("BoundDay.type = null", _set(_BOUND, "type", None), False, "the type is a datatype"),
] + _v0_keys("ContextValue", _CTX, ["naam"], "B23: a context value writes name, type and source") + \
    _v0_keys("BoundDay", _BOUND, ["naam"], "B23: the bound day writes type and source")


# --- check 1c, the L5 rows (card L5, 2026-09-24) -------------------------------------------------
# The first step of L5 moves every source text into the source record (delta B26): the optional
# field 'source' {snippet, line?} stands where 'bron' stood, and the schema closes by itself what
# the record cannot carry: 'bron' on a node, a record that is not an object with a non-empty
# snippet, the positions file and column (they wait for the card that first writes positions), a
# line below 1, a line beside the record on a consistency version or a rule group, and an
# Onopgelost marker without a source. Same mechanism as the M1 to L4 rows: one small valid document
# — the L4 base plus an Onopgelost condition and a rule group with its source line — and one
# mutation per row. Since this step the bases above write every source text in the record: a null
# source text is absent, a string one a snippet (the bound day's, both markers', and with its line
# the consistency version's). The rows that must PASS pin the absences the record allows. In the
# reasons, a B-number is a delta of spec/proposals/V1-WIREMIGRATIE.md.
def _l5_base():
    d = _l4_base()
    r = d["rules"]
    num = {"node": "Number", "whole": True, "decimals": 0, "sign": "ONBEPAALD"}
    r["resolvedRegels"].append({"node": "Rule", "name": "verjaring", "versions": [{
        "validity": dict(_ALWAYS),
        "target": {"node": "AttribuutRef", "attribuut": "vondstwaarde", "objecttype": "Jutter"},
        "value": {"node": "Literal", "value": {"number": "0"}, "type": dict(num)},
        "condition": {"node": "Onopgelost", "source": {"snippet": "de vondst is verjaard"}},
        "initialisation": False, "presentation": "BEREKENING"}]})
    r["regelGroepen"] = [{"node": "ResolvedRegelGroep", "naam": "vondstregels", "recursief": False,
                          "leden": ["vondstwaarde", "verjaring"],
                          "source": {"snippet": "Regelgroep vondstregels", "line": 3}}]
    return d


_EXPR_MARKER = lambda d: _val(d, "vondstonbekend")
_COND_MARKER = lambda d: _rv(d, "verjaring")["condition"]
_GROUP = lambda d: d["rules"]["regelGroepen"][0]

L5_SHAPE_ROWS = [
    ("the L5 base document", lambda d: None, True,
     "the L4 shape with every source text in the source record: a snippet on the bound day and on "
     "both markers, and with its line on the consistency version and the rule group"),
    ("BoundDay carries bron instead of source",
     lambda d: _BOUND(d).update(bron=_BOUND(d).pop("source")["snippet"]), False,
     "B26: the source text is the snippet of the source record; the v0 field bron left the wire"),
    ("Literal carries bron = null", _set(_NUMLIT, "bron", None), False,
     "B26: absent is the one spelling of a node without source text"),
    ("source as a string", _set(_BOUND, "source", "de dag"), False,
     "B26: the source text is the snippet of the record, never the field itself"),
    ("source = null", _set(_BOUND, "source", None), False,
     "absent is the one spelling of a node without source text"),
    ("source = {}", _set(_BOUND, "source", {}), False,
     "the snippet is always written: a record without it has no source text"),
    ("source.snippet = ''", _set(_BOUND, "source", {"snippet": ""}), False,
     "a snippet is never empty: a node without source text carries no source"),
    ("source with file", lambda d: _BOUND(d)["source"].update(file="bron.rgs"), False,
     "file and column wait for the card that first writes positions and fixes their conventions"),
    ("source with column", lambda d: _BOUND(d)["source"].update(column=1), False,
     "file and column wait for the card that first writes positions and fixes their conventions"),
    ("source with line = 0", lambda d: _GROUP(d)["source"].update(line=0), False,
     "a source line is at least 1"),
    ("source with line = 12", lambda d: _GROUP(d)["source"].update(line=12), True,
     "a line of at least 1, after the snippet"),
    ("ConsistencyVersion carries line beside source", _set(_CONS, "line", 12), False,
     "B26: the line of a consistency version is part of its source record"),
    ("ResolvedRegelGroep carries line beside source", _set(_GROUP, "line", 3), False,
     "B26: the line of a rule group is part of its source record"),
    ("Onopgelost expression without source", _pop(_EXPR_MARKER, "source"), False,
     "WF-113 (b) accounts for a marker by its snippet, so a marker always carries a source"),
    ("Onopgelost condition without source", _pop(_COND_MARKER, "source"), False,
     "WF-113 (b) accounts for a marker by its snippet, so a marker always carries a source"),
    ("ConsistencyVersion without source", _pop(_CONS, "source"), True,
     "absent: a version without source text, and so without a line"),
    ("ConsistencyVersion.source without line", lambda d: _CONS(d)["source"].pop("line"), True,
     "the line is optional in the record"),
]


shape_bad = 0
for base, rows in ((_m1_base, M1_SHAPE_ROWS), (_l2_base, L2_SHAPE_ROWS),
                   (_l3_base, L3_SHAPE_ROWS), (_l4_base, L4_SHAPE_ROWS),
                   (_l5_base, L5_SHAPE_ROWS)):
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
n_shape = (len(M1_SHAPE_ROWS) + len(L2_SHAPE_ROWS) + len(L3_SHAPE_ROWS) + len(L4_SHAPE_ROWS)
           + len(L5_SHAPE_ROWS))


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
