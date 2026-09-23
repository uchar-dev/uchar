#!/bin/sh
# Gate 2: IR JSON schema (spec/schema/regelspraak-ir-v0.schema.json).
#
# Four checks:
#   1. the schema is itself a valid draft 2020-12 schema (metaschema);
#  1b. the schema's patterns are held against a fixed lexeme table, in both
#      polarities, with no corpus document involved — without it, deleting a
#      pattern is a silent green (measured 2026-09-11). A clamp fails when a
#      pattern appears at a position the table does not carry;
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

SCHEMA='spec/schema/regelspraak-ir-v0.schema.json'
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
# (measured 2026-09-23, the gate-4 twin of this fix). Both patterns of this
# schema are anchored at both ends, so a FULL match is what ECMA-262 asks of
# them; on a pattern that is not anchored that way a full match is only ever
# stricter than ECMA-262's search, never laxer. The self-test below runs the
# same validator as the corpus check, and its trailing-newline row fails the
# day the pattern is matched Python's way again.
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
# arrives with its own rows or not at all. Named rather than silently omitted: the ten patterns of
# uchar-case-v0 stand in exactly the same position and are gate 4's to close, on a card of its own.
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
    print("check-ir-schema: schema valid; %d pattern self-test row(s); "
          "0 corpus files (corpus not yet populated)" % n_selftest)
    sys.exit(0)

print("check-ir-schema: PASS (schema valid; %d pattern self-test row(s); "
      "%d file(s) validate and round-trip, floor %d)" % (n_selftest, n, floor))
sys.exit(0)
PY
