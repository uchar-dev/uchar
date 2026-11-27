#!/bin/sh
# Gate 4: corpus case files (spec/schema/uchar-case-v0.schema.json).
#
# Six checks:
#   1. the case schema is itself a valid draft 2020-12 schema (metaschema);
#  1b. the schema's patterns are held against a fixed lexeme table, in both
#      polarities and through the validator the corpus check uses, with no
#      corpus document involved (the gate-2 model, 2026-09-23). A clamp
#      fails when a pattern appears at a position the table does not carry;
#   2. every corpus/golden/**/case*.json and corpus/red/**/case*.json
#      validates against it;
#   3. every case file sits beside an ir.json;
#   4. zone discipline: a golden case carries `expected` (never
#      `expectedFault`), a red case carries `expectedFault` — a red
#      case can never hide inside the green corpus, or vice versa;
#   5. canonical key order: every object in every case file follows the
#      `properties` order of its schema definition (`rolesClosed` directly
#      after `rolesEmpty` and before `characteristics`, and the role
#      expectations `roleEmpty`, `roleCount`, `roleTarget`, `roleOpen`),
#      held by self-test rows in both polarities.
# A zone without a case file fails: an empty input set is never silently green.
#
# NO monotone floor here (maintainer decision 2026-09-09, the counting-floors
# regime): this gate's count is the disjoint union of the two zone counts that
# gates 5 and 8 each floor separately, and a floor on the SUM is the weaker
# instrument — a golden case traded for a red one keeps the total equal while
# the floor of whichever zone LOST it fires. (Only that one: the gaining zone's
# floor is a lower bound and says nothing about a zone that grew.)
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

SCHEMA='spec/schema/uchar-case-v0.schema.json'
if [ ! -f "$SCHEMA" ]; then
    echo "FAIL: schema not found at $SCHEMA" >&2
    exit 2
fi

if python3 -c 'import jsonschema' 2>/dev/null; then
    RUN='python3'
elif command -v uv >/dev/null 2>&1; then
    RUN='uv run --quiet --with jsonschema python3'
else
    echo 'FAIL: need python3 with the "jsonschema" package.' >&2
    exit 2
fi

list=$(mktemp) || exit 2
trap 'rm -f "$list"' EXIT
find corpus/golden corpus/red -name 'case*.json' 2>/dev/null | sort > "$list"

status=0
for zone in corpus/golden corpus/red; do
    if ! grep -q "^$zone/" "$list"; then
        echo "FAIL: $zone holds no case file (an empty input set is a failure, never silently green)" >&2
        status=1
    fi
done
n=0
while IFS= read -r f; do
    n=$((n + 1))
    if [ ! -f "$(dirname "$f")/ir.json" ]; then
        echo "FAIL: $f has no ir.json beside it (a case belongs to a document)" >&2
        status=1
    fi
done < "$list"

selftest=$($RUN - "$SCHEMA" "$list" <<'PY'
import sys, json, re
from jsonschema import Draft202012Validator, validators
from jsonschema.exceptions import best_match, SchemaError, ValidationError

# JSON Schema's `pattern` is an ECMA-262 regular expression, and there `$`
# matches only at the end of the input. The stock validator runs Python's
# re.search, whose `$` also matches before a final newline, so it admitted
# "2026-03-10\n" for the date pattern, "5\n" for the number pattern and an
# asOfDate with a trailing newline, all of which every case reader refuses
# (measured 2026-09-23). This validator takes a FULL match instead. For the
# three patterns the case schema carries (anchored at both ends on every
# alternative, digits written [0-9]) a full match and ECMA-262's search agree:
# measured 2026-09-23 against node on 1437 lexeme-pattern pairs (both
# self-test tables and every corpus lexeme of these kinds), no difference.
# That is a property of these patterns, not of full matching: `^a$\n?` on
# "a\n", `^\d+$` on "\u0663" and `^\w+$` on "\u00e9" match fully in Python
# and fail in ECMA-262. The self-test below carries no non-ASCII digit, so it
# would not see a pattern changed from [0-9] to \d.
def ecma_pattern(validator, pattern, instance, schema):
    if validator.is_type(instance, "string") and re.fullmatch(pattern, instance) is None:
        yield ValidationError("%r does not match %r" % (instance, pattern))

CaseValidator = validators.extend(Draft202012Validator, {"pattern": ecma_pattern})

schema_path, list_path = sys.argv[1], sys.argv[2]
with open(list_path, encoding="utf-8") as fh:
    files = [line.rstrip("\n") for line in fh if line.strip()]
with open(schema_path, encoding="utf-8") as fh:
    schema = json.load(fh)
try:
    Draft202012Validator.check_schema(schema)
except SchemaError as exc:
    sys.stderr.write("FAIL: %s is not a valid draft 2020-12 schema:\n  %s\n"
                     % (schema_path, exc.message))
    sys.exit(1)
validator = CaseValidator(schema)

# --- check 1b: the pattern self-test (the gate-2 model, 2026-09-23) --------------------------------
# A schema pattern is a MUST with no failing witness of its own: the corpus carries only lexemes the
# patterns admit, so loosening a pattern, or matching it Python's way again (then `$` also matches
# before a final newline), leaves every corpus file valid and this gate green. So the witness is held
# HERE, against the schema itself: every pattern position, every lexeme of its kind's table, through
# the SAME validator the corpus check below uses. SCOPE is the schema's whole pattern surface; the
# clamp fails when a pattern turns up at a position the table does not carry, or a carried one goes.
DATE, DATETIME, NUMBER, FAULT_CODE = "date", "dateTime", "number", "faultCode"
PATTERN_POSITIONS = {
    "/properties/expectedFault/properties/code": FAULT_CODE,
    # the EF code on an `inconsistent` expectation, a §9.5 kind (card channel-fact-codes, 2026-10-19)
    "/$defs/expectation/oneOf/6/properties/code": FAULT_CODE,
    "/properties/asOfDate/oneOf/0": DATE,
    "/$defs/instance/properties/characteristicTimelines/additionalProperties/items/properties/from": DATE,
    "/$defs/instance/properties/characteristicTimelines/additionalProperties/items/properties/to": DATE,
    "/$defs/value/oneOf/0/properties/number": NUMBER,
    "/$defs/value/oneOf/1/properties/date": DATE,
    "/$defs/value/oneOf/2/properties/dateTime": DATETIME,
    "/$defs/timelineValue/properties/timeline/items/properties/from": DATE,
    "/$defs/timelineValue/properties/timeline/items/properties/to": DATE,
    "/$defs/expectation/oneOf/2/properties/timeline/items/properties/from": DATE,
    "/$defs/expectation/oneOf/2/properties/timeline/items/properties/to": DATE,
}

# (lexeme, must the schema accept it, why this row is here)
LEXEMES = {
    DATE: [
        ("2026-06-01",          True,  "the strict lexeme itself"),
        ("2026-02-30",          True,  "THE LAYER BOUNDARY, pinned deliberately: a pattern cannot check "
                                       "a calendar, so this one is admitted here and refused by the case "
                                       "readers (corpus/red/input-date-calendar pins that refusal)"),
        ("2026-06-01\n",        False, "a trailing newline: Python's `$` also matches before a final "
                                       "newline, ECMA-262's only at the end"),
        ("2026-06-01 ",         False, "a trailing space"),
        ("01-06-2026",          False, "day-first, the source snippet's order"),
        ("2026-6-1",            False, "variable field widths"),
        ("+012026-06-01",       False, "a sign-prefixed extended year"),
        ("2026-06-01T00:00:00", False, "a dateTime where a day is called for"),
        ("",                    False, "the empty string"),
    ],
    DATETIME: [
        ("2026-03-15T12:00:00",     True,  "the canonical lexeme without a fraction"),
        ("2026-03-15T12:00:00.250", True,  "a fraction of exactly three digits"),
        ("2026-03-15T24:00:00",     True,  "THE LAYER BOUNDARY: the pattern checks field widths, not the "
                                           "clock, so hour 24 is admitted here and refused by the case "
                                           "readers"),
        ("2026-03-15T12:00:00\n",   False, "a trailing newline"),
        ("2026-03-15 12:00:00",     False, "a space where 'T' belongs"),
        ("2026-03-15T12:00",        False, "no seconds field"),
        ("2026-03-15T12:00:00.1",   False, "a fraction of one digit"),
        ("2026-03-15T12:00:00Z",    False, "a zone suffix"),
    ],
    NUMBER: [
        ("5",      True,  "an integer"),
        ("-12.50", True,  "a negative decimal"),
        ("3/4",    True,  "a rational"),
        ("5\n",    False, "a trailing newline"),
        ("1,000",  False, "a comma: the case wire has no grouping and no decimal comma"),
        (".5",     False, "a bare leading point"),
        ("1E3",    False, "an exponent"),
        ("+5",     False, "a plus sign"),
        ("1/0",    False, "a zero denominator"),
        ("3/04",   False, "a denominator with a leading zero"),
        ("1.5/2",  False, "a decimal numerator"),
        ("",       False, "the empty string"),
    ],
    # the EF code of a fault kind (RUNTIME-SEMANTICS ch. 5.7b), 2026-10-18 (card meldingscodes)
    FAULT_CODE: [
        ("EF-005",   True,  "the code itself"),
        ("EF-000",   True,  "the pattern checks the form, not the table; gate 8 holds every pinned code "
                            "to a row of the RUNTIME-SEMANTICS table"),
        ("EF-005\n", False, "a trailing newline"),
        ("EF-05",    False, "two digits"),
        ("EF-0005",  False, "four digits"),
        ("ef-005",   False, "lower case"),
        ("WF-005",   False, "a verifier rule, not a fault kind"),
        ("EF005",    False, "no hyphen"),
        ("",         False, "the empty string"),
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
    sys.stderr.write("FAIL: the schema's pattern surface moved and the self-test did not follow.\n")
    for p in sorted(set(_positions) - set(PATTERN_POSITIONS)):
        sys.stderr.write("  pattern with no self-test row: %s\n" % p)
    for p in sorted(set(PATTERN_POSITIONS) - set(_positions)):
        sys.stderr.write("  self-test row with no pattern:  %s\n" % p)
    sys.exit(1)

# One probe per position: the position's own subschema, reached by $ref from a root that carries
# the schema's `properties` and `$defs` (neither asserts anything about a string instance).
selftest_bad = 0
n_selftest = 0
for pos, kind in sorted(PATTERN_POSITIONS.items()):
    probe = CaseValidator({"properties": schema["properties"], "$defs": schema["$defs"],
                           "$ref": "#" + pos})
    for lexeme, want_ok, why in LEXEMES[kind]:
        n_selftest += 1
        got_ok = not list(probe.iter_errors(lexeme))
        if got_ok != want_ok:
            selftest_bad += 1
            sys.stderr.write(
                "FAIL: pattern self-test, %s = %r: schema %s it, expected %s\n  %s\n"
                % (pos, lexeme, "accepts" if got_ok else "refuses",
                   "accept" if want_ok else "refuse", why))
# The shape of the pin of a red case (card fragment-cleanup, 2026-10-19): `expectedFault` is a code and its params,
# an object of strings, and nothing else, so a fragment of message text (`ref`, `clingo`, `drools`) left in a case is
# refused here and not only by the case reader. No case of the corpus can fail these rows; they are held HERE,
# against the schema itself, through the same validator.
EXPECTED_FAULT_ROWS = [
    ({"code": "EF-005", "params": {}},                              True,  "a kind without parameters pins the empty object"),
    ({"code": "EF-016", "params": {"label": "V", "rules": "a, b"}}, True,  "parameters are an object of strings"),
    ({"code": "EF-005"},                                            False, "params is required: a pin states its parameters, or states that it has none"),
    ({"params": {}},                                                False, "the code is required"),
    ({"code": "EF-005", "params": {"rule": 5}},                     False, "a parameter value is a string, a lexeme"),
    ({"code": "EF-005", "params": {"rule": None}},                  False, "no null parameter value"),
    ({"code": "EF-005", "params": ["rule"]},                        False, "params is an object, not a list"),
    ({"code": "EF-005", "params": "rule"},                          False, "params is an object, not a string"),
    ({"code": "EF-005", "params": {}, "ref": "division by zero"},   False, "a ref fragment is refused: no pin is a piece of message text"),
    ({"code": "EF-005", "params": {}, "clingo": "division"},        False, "a clingo fragment is refused"),
    ({"code": "EF-005", "params": {}, "drools": "deling"},          False, "a drools fragment is refused"),
]
probe = CaseValidator({"properties": schema["properties"], "$defs": schema["$defs"],
                       "$ref": "#/properties/expectedFault"})
for doc, want_ok, why in EXPECTED_FAULT_ROWS:
    n_selftest += 1
    got_ok = not list(probe.iter_errors(doc))
    if got_ok != want_ok:
        selftest_bad += 1
        sys.stderr.write("FAIL: expectedFault self-test, %r: schema %s it, expected %s\n  %s\n"
                         % (doc, "accepts" if got_ok else "refuses", "accept" if want_ok else "refuse", why))
# The shape of a cell (card dimensions-executed, 2026-11-02): the cells input of a dimensioned attribute and the two
# expectations on one cell. No golden case carries a cell yet, so the shape is held HERE, through the same validator;
# that the labels name a cell of the attribute, positionally, is the case readers' (EF-034), which see the IR.
CELL_ROWS = [
    ("#/$defs/instance/properties/attributes/additionalProperties",
     {"cells": [{"labels": ["voorjaar"], "value": {"number": "3.00"}}]}, True, "a cell: its labels and its value"),
    ("#/$defs/instance/properties/attributes/additionalProperties",
     {"cells": [{"labels": ["voorjaar", "zuid"], "value": {"number": "5"}},
                {"labels": ["najaar", "zuid"], "value": {"number": "2"}}]}, True,
     "two cells of an attribute with two dimensions"),
    ("#/$defs/instance/properties/attributes/additionalProperties",
     {"cells": []}, False, "no cell: absence is the one spelling of an attribute whose cells are all empty"),
    ("#/$defs/instance/properties/attributes/additionalProperties",
     {"cells": [{"labels": [], "value": {"number": "1"}}]}, False, "a cell names at least one label"),
    ("#/$defs/instance/properties/attributes/additionalProperties",
     {"cells": [{"labels": ["voorjaar"]}]}, False, "a cell of the input carries its value"),
    ("#/$defs/instance/properties/attributes/additionalProperties",
     {"cells": [{"labels": ["voorjaar"], "value": None}]}, False, "no explicit empty in the input"),
    ("#/$defs/instance/properties/attributes/additionalProperties",
     {"cells": [{"labels": {"seizoen": "voorjaar"}, "value": {"number": "1"}}]}, False,
     "the labels are positional, never an object keyed by dimension"),
    ("#/$defs/instance/properties/attributes/additionalProperties",
     {"cells": [{"labels": ["voorjaar"], "value": {"number": "1"}}], "number": "1"}, False,
     "the cells variant beside a scalar is no value"),
    ("#/$defs/expectation",
     {"kind": "cellValue", "label": "j1", "attribute": "vondstgewicht", "labels": ["najaar"],
      "value": {"number": "4.50"}}, True, "the value of one cell"),
    ("#/$defs/expectation",
     {"kind": "cellEmpty", "label": "j1", "attribute": "vondstgewicht", "labels": ["zomer"]}, True,
     "one cell empty"),
    ("#/$defs/expectation",
     {"kind": "cellValue", "label": "j1", "attribute": "vondstgewicht", "value": {"number": "4.50"}}, False,
     "a cell expectation names its cell"),
    ("#/$defs/expectation",
     {"kind": "cellEmpty", "label": "j1", "attribute": "vondstgewicht", "labels": ["zomer"],
      "value": {"number": "1"}}, False, "an empty cell carries no value"),
    ("#/$defs/expectation",
     {"kind": "attributeValue", "label": "j1", "attribute": "vondstgewicht", "labels": ["najaar"],
      "value": {"number": "4.50"}}, False, "labels belong to the cell kinds, never to attributeValue"),
]
for ref_, doc, want_ok, why in CELL_ROWS:
    n_selftest += 1
    probe = CaseValidator({"properties": schema["properties"], "$defs": schema["$defs"], "$ref": ref_})
    got_ok = not list(probe.iter_errors(doc))
    if got_ok != want_ok:
        selftest_bad += 1
        sys.stderr.write("FAIL: cell self-test, %r: schema %s it, expected %s\n  %s\n"
                         % (doc, "accepts" if got_ok else "refuses", "accept" if want_ok else "refuse", why))
# The states of a member in a document with inferFromEmpty false (RUNTIME-SEMANTICS ch. 1.5; card open-values-in-ref,
# 2026-11-21; card settled-empty-input, 2026-11-25): the input fields that state an attribute has no value or a
# characteristic does not hold, and the two expectations of an open member. Only the strict goldens carry them, so the
# shape is held HERE as well, through the same validator; that the names are declared and that no member stands in both
# input fields are the case readers' (EF-040, EF-047, EF-048).
def _v1(**extra):
    inst = {"label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": []}
    inst.update(extra)
    return inst
STATE_ROWS = [
    ("#/$defs/instance", _v1(attributesEmpty=["aanvangsdatum"]), True,
     "an attribute the case states has no value"),
    ("#/$defs/instance", _v1(attributesEmpty=[]), True, "an empty list states nothing"),
    ("#/$defs/instance", _v1(attributesEmpty=[""]), False, "an attribute is named"),
    ("#/$defs/instance", _v1(attributesEmpty="aanvangsdatum"), False, "a list, never a single name"),
    ("#/$defs/instance", _v1(attributesEmpty={"aanvangsdatum": None}), False,
     "a list of names, never an object of nulls"),
    ("#/$defs/instance", _v1(rolesEmpty=["vinder"]), True,
     "a non-reciprocal to-one role the case states has no link"),
    ("#/$defs/instance", _v1(rolesEmpty=[]), True, "an empty list states nothing, as 'rolesEmpty' may"),
    ("#/$defs/instance", _v1(rolesEmpty=[""]), False, "a role is named"),
    ("#/$defs/instance", _v1(rolesEmpty="vinder"), False, "a list, never a single name"),
    ("#/$defs/instance", _v1(rolesEmpty={"vinder": None}), False,
     "a list of names, never an object of nulls"),
    ("#/$defs/instance", _v1(rolesClosed=["vondsten"]), True,
     "a non-reciprocal to-many role the case states is closed"),
    ("#/$defs/instance", _v1(roles={"vondsten": ["V2"]}, rolesClosed=["vondsten"]), True,
     "rolesClosed alongside linked labels closes the collection at those labels"),
    ("#/$defs/instance", _v1(rolesClosed=[]), True, "an empty list states nothing, as 'rolesClosed' may"),
    ("#/$defs/instance", _v1(rolesClosed=[""]), False, "a closed role is named"),
    ("#/$defs/instance", _v1(rolesClosed=[None]), False, "a closed role is a non-empty string"),
    ("#/$defs/instance", _v1(rolesClosed="vondsten"), False, "a list, never a single name"),
    ("#/$defs/instance", _v1(rolesClosed={"vondsten": []}), False,
     "a list of names, never an object of arrays"),
    ("#/$defs/instance", _v1(characteristicsAbsent=["gemeld"]), True,
     "a characteristic the case states does not hold"),
    ("#/$defs/instance", _v1(characteristicsAbsent=[]), True, "an empty list states nothing, as 'characteristics' may"),
    ("#/$defs/instance", _v1(), True, "the field is optional"),
    ("#/$defs/instance", _v1(characteristicsAbsent=[""]), False, "a characteristic is named"),
    ("#/$defs/instance", _v1(characteristicsAbsent="gemeld"), False, "a list, never a single name"),
    ("#/$defs/instance", _v1(characteristicsAbsent={"gemeld": False}), False,
     "a list of names, never an object of truths"),
    ("#/$defs/expectation", {"kind": "attributeOpen", "label": "V1", "attribute": "vindloon"}, True,
     "an open attribute"),
    ("#/$defs/expectation", {"kind": "attributeOpen", "label": "V1", "attribute": "vindloon",
                             "value": {"number": "10"}}, False, "an open attribute carries no value"),
    ("#/$defs/expectation", {"kind": "attributeOpen", "label": "V1"}, False, "an attributeOpen names its attribute"),
    ("#/$defs/expectation", {"kind": "characteristicOpen", "label": "V1", "characteristic": "kostbaar"}, True,
     "an open characteristic"),
    ("#/$defs/expectation", {"kind": "characteristicOpen", "label": "V1", "attribute": "kostbaar"}, False,
     "a characteristicOpen names a characteristic, never an attribute"),
    ("#/$defs/expectation", {"kind": "characteristicOpen", "characteristic": "kostbaar"}, False,
     "a characteristicOpen names its instance"),
    ("#/$defs/expectation", {"kind": "roleEmpty", "label": "J1", "role": "vondsten"}, True,
     "a closed empty role slot"),
    ("#/$defs/expectation", {"kind": "roleEmpty", "label": "J1"}, False,
     "a roleEmpty names its role"),
    ("#/$defs/expectation", {"kind": "roleEmpty", "role": "vondsten"}, False,
     "a roleEmpty names its instance"),
    ("#/$defs/expectation", {"kind": "roleEmpty", "label": "", "role": "vondsten"}, False,
     "a roleEmpty label is non-empty"),
    ("#/$defs/expectation", {"kind": "roleEmpty", "label": "J1", "role": ""}, False,
     "a roleEmpty role is non-empty"),
    ("#/$defs/expectation", {"kind": "roleEmpty", "label": "J1", "role": "vondsten", "count": 0}, False,
     "a roleEmpty carries no count"),
    ("#/$defs/expectation", {"kind": "roleEmpty", "label": "J1", "role": "vinder", "target": "V1"}, False,
     "a roleEmpty carries no target"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "role": "vondsten", "count": 0}, True,
     "a roleCount of 0"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "role": "vondsten", "count": 3}, True,
     "a positive roleCount"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "role": "vondsten"}, False,
     "a roleCount carries its count"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "count": 2}, False,
     "a roleCount names its role"),
    ("#/$defs/expectation", {"kind": "roleCount", "role": "vondsten", "count": 2}, False,
     "a roleCount names its instance"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "role": "", "count": 2}, False,
     "a roleCount role is non-empty"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "role": "vondsten", "count": -1}, False,
     "a roleCount count is non-negative"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "role": "vondsten", "count": 1.5}, False,
     "a roleCount count is an integer, never a fraction"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "role": "vondsten", "count": "2"}, False,
     "a roleCount count is a JSON integer, never a string"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "role": "vondsten", "count": True}, False,
     "a roleCount count is an integer, never a boolean"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "role": "vondsten", "count": False}, False,
     "a roleCount count is an integer, never false"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "role": "vondsten", "count": None}, False,
     "a roleCount count is an integer, never null"),
    ("#/$defs/expectation", {"kind": "roleCount", "label": "J1", "role": "vondsten", "count": 1, "target": "V1"}, False,
     "a roleCount carries no target"),
    ("#/$defs/expectation", {"kind": "roleTarget", "label": "V1", "role": "vinder", "target": "J1"}, True,
     "a roleTarget naming its target label"),
    ("#/$defs/expectation", {"kind": "roleTarget", "label": "V1", "role": "vinder"}, False,
     "a roleTarget carries its target"),
    ("#/$defs/expectation", {"kind": "roleTarget", "label": "V1", "target": "J1"}, False,
     "a roleTarget names its role"),
    ("#/$defs/expectation", {"kind": "roleTarget", "role": "vinder", "target": "J1"}, False,
     "a roleTarget names its instance"),
    ("#/$defs/expectation", {"kind": "roleTarget", "label": "", "role": "vinder", "target": "J1"}, False,
     "a roleTarget label is non-empty"),
    ("#/$defs/expectation", {"kind": "roleTarget", "label": "V1", "role": "", "target": "J1"}, False,
     "a roleTarget role is non-empty"),
    ("#/$defs/expectation", {"kind": "roleTarget", "label": "V1", "role": "vinder", "target": ""}, False,
     "a roleTarget target is non-empty"),
    ("#/$defs/expectation", {"kind": "roleTarget", "label": "V1", "role": "vinder", "target": ["J1"]}, False,
     "a roleTarget target is one label string, never a list"),
    ("#/$defs/expectation", {"kind": "roleTarget", "label": "V1", "role": "vinder", "target": 1}, False,
     "a roleTarget target is a string"),
    ("#/$defs/expectation", {"kind": "roleTarget", "label": "V1", "role": "vinder", "target": "J1", "count": 1}, False,
     "a roleTarget carries no count"),
    ("#/$defs/expectation", {"kind": "roleOpen", "label": "J1", "role": "vondsten"}, True,
     "an open role slot"),
    ("#/$defs/expectation", {"kind": "roleOpen", "label": "J1"}, False,
     "a roleOpen names its role"),
    ("#/$defs/expectation", {"kind": "roleOpen", "role": "vondsten"}, False,
     "a roleOpen names its instance"),
    ("#/$defs/expectation", {"kind": "roleOpen", "label": "J1", "role": ""}, False,
     "a roleOpen role is non-empty"),
    ("#/$defs/expectation", {"kind": "roleOpen", "label": "J1", "role": "vondsten", "count": 0}, False,
     "a roleOpen carries no count"),
    ("#/$defs/expectation", {"kind": "roleOpen", "label": "V1", "role": "vinder", "target": "J1"}, False,
     "a roleOpen carries no target"),
]
for ref_, doc, want_ok, why in STATE_ROWS:
    n_selftest += 1
    probe = CaseValidator({"properties": schema["properties"], "$defs": schema["$defs"], "$ref": ref_})
    got_ok = not list(probe.iter_errors(doc))
    if got_ok != want_ok:
        selftest_bad += 1
        sys.stderr.write("FAIL: state self-test, %r: schema %s it, expected %s\n  %s\n"
                         % (doc, "accepts" if got_ok else "refuses", "accept" if want_ok else "refuse", why))
if selftest_bad:
    sys.stderr.write("check-case-schema: FAIL (%d self-test row(s) bad)\n" % selftest_bad)
    sys.exit(1)


def _deref(s):
    while isinstance(s, dict) and "$ref" in s:
        cur = schema
        for seg in s["$ref"][2:].split("/"):
            cur = cur[seg]
        s = cur
    return s


def _candidates(inst, s):
    s = _deref(s)
    alts = s.get("oneOf") or s.get("anyOf")
    out = [s] if alts is None or "properties" in s else [c for alt in alts for c in _candidates(inst, alt)]
    if isinstance(inst, dict):
        keep = []
        for c in out:
            props = c.get("properties")
            if props is None:
                if alts is None and isinstance(c.get("additionalProperties"), dict):
                    keep.append(c)
                continue
            kind = props.get("kind")
            if isinstance(kind, dict) and "const" in kind and inst.get("kind") != kind["const"]:
                continue
            if any(r not in inst for r in c.get("required", [])):
                continue
            if c.get("additionalProperties") is False and any(k not in props for k in inst):
                continue
            keep.append(c)
        return keep
    if isinstance(inst, list):
        return [c for c in out if "items" in c]
    return []


def _ptr(path, key):
    return "%s/%s" % (path, str(key).replace("~", "~0").replace("/", "~1"))


ko_devs = []


def _key_order(inst, s, path, doc):
    if isinstance(inst, dict):
        cs = _candidates(inst, s)
        if not cs:
            return
        if "properties" not in cs[0]:
            for k, v in inst.items():
                _key_order(v, cs[0]["additionalProperties"], _ptr(path, k), doc)
            return
        follows = [c for c in cs if [k for k in c["properties"] if k in inst]
                   == [k for k in inst if k in c["properties"]]]
        if not follows:
            ko_devs.append((doc, path or "<root>", list(inst),
                            [k for k in cs[0]["properties"] if k in inst]))
        c = (follows or cs)[0]
        for k, v in inst.items():
            if k in c["properties"]:
                _key_order(v, c["properties"][k], _ptr(path, k), doc)
            elif isinstance(c.get("additionalProperties"), dict):
                _key_order(v, c["additionalProperties"], _ptr(path, k), doc)
    elif isinstance(inst, list):
        cs = _candidates(inst, s)
        if cs:
            for i, x in enumerate(inst):
                _key_order(x, cs[0]["items"], _ptr(path, i), doc)


def _case_with_instance_keys(pairs):
    return {
        "format": "uchar-case-v0",
        "asOfDate": None,
        "input": {"instances": [dict(pairs)], "parameters": {}},
        "expected": [{"kind": "consistent"}],
    }


def _case_with_expectation_keys(pairs):
    return {
        "format": "uchar-case-v0",
        "asOfDate": None,
        "input": {"instances": [], "parameters": {}},
        "expected": [dict(pairs)],
    }


ORDER_ROWS = [
    ("rolesClosed directly after rolesEmpty and before characteristics",
     _case_with_instance_keys([
         ("label", "J1"), ("objectType", "Jutter"), ("attributes", {}),
         ("roles", {}), ("rolesEmpty", ["mentor"]), ("rolesClosed", ["vondsten"]),
         ("characteristics", []),
     ]), True),
    ("rolesClosed directly after roles when rolesEmpty is absent",
     _case_with_instance_keys([
         ("label", "J1"), ("objectType", "Jutter"), ("attributes", {}),
         ("roles", {"vondsten": ["V1"]}), ("rolesClosed", ["vondsten"]),
         ("characteristics", []),
     ]), True),
    ("rolesClosed before rolesEmpty",
     _case_with_instance_keys([
         ("label", "J1"), ("objectType", "Jutter"), ("attributes", {}),
         ("roles", {}), ("rolesClosed", ["vondsten"]), ("rolesEmpty", ["mentor"]),
         ("characteristics", []),
     ]), False),
    ("rolesClosed after characteristics",
     _case_with_instance_keys([
         ("label", "J1"), ("objectType", "Jutter"), ("attributes", {}),
         ("roles", {}), ("characteristics", []), ("rolesClosed", ["vondsten"]),
     ]), False),
    ("roleEmpty in canonical order kind, label, role",
     _case_with_expectation_keys([("kind", "roleEmpty"), ("label", "J1"), ("role", "vondsten")]), True),
    ("roleEmpty with role before label",
     _case_with_expectation_keys([("kind", "roleEmpty"), ("role", "vondsten"), ("label", "J1")]), False),
    ("roleEmpty with kind after label",
     _case_with_expectation_keys([("label", "J1"), ("kind", "roleEmpty"), ("role", "vondsten")]), False),
    ("roleCount in canonical order kind, label, role, count",
     _case_with_expectation_keys([("kind", "roleCount"), ("label", "J1"), ("role", "vondsten"), ("count", 2)]), True),
    ("roleCount with count before role",
     _case_with_expectation_keys([("kind", "roleCount"), ("label", "J1"), ("count", 2), ("role", "vondsten")]), False),
    ("roleCount with role before label",
     _case_with_expectation_keys([("kind", "roleCount"), ("role", "vondsten"), ("label", "J1"), ("count", 2)]), False),
    ("roleTarget in canonical order kind, label, role, target",
     _case_with_expectation_keys([("kind", "roleTarget"), ("label", "V1"), ("role", "vinder"), ("target", "J1")]), True),
    ("roleTarget with target before role",
     _case_with_expectation_keys([("kind", "roleTarget"), ("label", "V1"), ("target", "J1"), ("role", "vinder")]), False),
    ("roleTarget with role before label",
     _case_with_expectation_keys([("kind", "roleTarget"), ("role", "vinder"), ("label", "V1"), ("target", "J1")]), False),
    ("roleOpen in canonical order kind, label, role",
     _case_with_expectation_keys([("kind", "roleOpen"), ("label", "J1"), ("role", "vondsten")]), True),
    ("roleOpen with role before label",
     _case_with_expectation_keys([("kind", "roleOpen"), ("role", "vondsten"), ("label", "J1")]), False),
]
for row, doc, want_in_order in ORDER_ROWS:
    n_selftest += 1
    seen = len(ko_devs)
    _key_order(doc, schema, "", "<self-test>")
    got_in_order = len(ko_devs) == seen
    del ko_devs[seen:]
    if got_in_order != want_in_order:
        selftest_bad += 1
        sys.stderr.write("FAIL: key-order self-test, %s: case %s the schema's order, expected it %s\n"
                         % (row, "follows" if got_in_order else "breaks",
                            "to follow" if want_in_order else "to break"))
if selftest_bad:
    sys.stderr.write("check-case-schema: FAIL (%d self-test row(s) bad)\n" % selftest_bad)
    sys.exit(1)

failed = 0
for path in files:
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
    _key_order(doc, schema, "", path)
    # zone discipline, checked structurally on the parsed keys: a red
    # case can never hide inside the green corpus, or vice versa.
    if path.startswith("corpus/golden/") and "expectedFault" in doc:
        sys.stderr.write("FAIL: %s carries expectedFault inside corpus/golden — "
                         "red cases live in corpus/red\n" % path)
        failed += 1
    elif path.startswith("corpus/red/") and "expectedFault" not in doc:
        sys.stderr.write("FAIL: %s carries no expectedFault inside corpus/red — "
                         "green cases live in corpus/golden\n" % path)
        failed += 1
for doc_path, ptr, got, want in ko_devs:
    sys.stderr.write("FAIL: %s at %s: the keys %s are not in the schema's order %s\n"
                     % (doc_path, ptr, ", ".join(got), ", ".join(want)))
    failed += 1
if failed:
    sys.exit(1)
print(n_selftest)
PY
) || status=1

if [ "$status" -eq 0 ]; then
    echo "check-case-schema: PASS (schema valid; $selftest self-test row(s); $n case file(s) validate)"
fi
exit "$status"
