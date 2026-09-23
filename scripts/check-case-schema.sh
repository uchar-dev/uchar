#!/bin/sh
# Gate 4: corpus case files (spec/schema/uchar-case-v0.schema.json).
#
# Five checks:
#   1. the case schema is itself a valid draft 2020-12 schema (metaschema);
#  1b. the schema's patterns are held against a fixed lexeme table, in both
#      polarities and through the validator the corpus check uses, with no
#      corpus document involved (the gate-2 model, 2026-09-23). A clamp
#      fails when a pattern appears at a position the table does not carry;
#   2. every corpus/golden/**/case*.json and corpus/rood/**/case*.json
#      validates against it;
#   3. every case file sits beside an ir.json;
#   4. zone discipline: a golden case carries `expected` (never
#      `expectedFault`), a rood case carries `expectedFault` — a red
#      case can never hide inside the green corpus, or vice versa.
# An empty case set is reported explicitly, never silently green.
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
find corpus/golden corpus/rood -name 'case*.json' 2>/dev/null | sort > "$list"

status=0
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
DATE, DATETIME, NUMBER = "date", "dateTime", "number"
PATTERN_POSITIONS = {
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
                                       "readers (corpus/rood/inputdatum-kalender pins that refusal)"),
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
if selftest_bad:
    sys.stderr.write("check-case-schema: FAIL (%d pattern self-test row(s) bad)\n" % selftest_bad)
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
    # zone discipline, checked structurally on the parsed keys: a red
    # case can never hide inside the green corpus, or vice versa.
    if path.startswith("corpus/golden/") and "expectedFault" in doc:
        sys.stderr.write("FAIL: %s carries expectedFault inside corpus/golden — "
                         "red cases live in corpus/rood\n" % path)
        failed += 1
    elif path.startswith("corpus/rood/") and "expectedFault" not in doc:
        sys.stderr.write("FAIL: %s carries no expectedFault inside corpus/rood — "
                         "green cases live in corpus/golden\n" % path)
        failed += 1
if failed:
    sys.exit(1)
print(n_selftest)
PY
) || status=1

if [ "$n" -eq 0 ]; then
    echo "check-case-schema: schema valid; ${selftest:-no} pattern self-test row(s); 0 case files (cases not yet populated)"
    exit "$status"
fi
if [ "$status" -eq 0 ]; then
    echo "check-case-schema: PASS (schema valid; $selftest pattern self-test row(s); $n case file(s) validate)"
fi
exit "$status"
