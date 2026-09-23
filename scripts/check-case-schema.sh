#!/bin/sh
# Gate 4: corpus case files (spec/schema/uchar-case-v0.schema.json).
#
# Four checks:
#   1. the case schema is itself a valid draft 2020-12 schema (metaschema);
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

$RUN - "$SCHEMA" "$list" <<'PY' || status=1
import sys, json, re
from jsonschema import Draft202012Validator, validators
from jsonschema.exceptions import best_match, SchemaError, ValidationError

# JSON Schema's `pattern` is an ECMA-262 regular expression, and there `$`
# matches only at the end of the input. The stock validator runs Python's
# re.search, whose `$` also matches before a final newline, so it admitted
# "2026-03-10\n" for the date pattern, "5\n" for the number pattern and an
# asOfDate with a trailing newline, all of which every case reader refuses
# (measured 2026-09-23). Every pattern in the case schema is anchored at both
# ends on every alternative, so a FULL match is what ECMA-262 asks of it; on
# a pattern that is not anchored that way a full match is only ever stricter
# than ECMA-262's search, never laxer, so this gate can fail loudly on such a
# pattern but can never pass a value the schema refuses.
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
PY

if [ "$n" -eq 0 ]; then
    echo 'check-case-schema: schema valid; 0 case files (cases not yet populated)'
    exit "$status"
fi
if [ "$status" -eq 0 ]; then
    echo "check-case-schema: PASS (schema valid; $n case file(s) validate)"
fi
exit "$status"
