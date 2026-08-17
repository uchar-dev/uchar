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
import sys, json
from jsonschema import Draft202012Validator
from jsonschema.exceptions import best_match, SchemaError

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
validator = Draft202012Validator(schema)

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
