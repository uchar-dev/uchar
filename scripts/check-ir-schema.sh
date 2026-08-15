#!/bin/sh
# Gate 2: IR JSON schema (spec/schema/regelspraak-ir-v0.schema.json).
#
# Three checks:
#   1. the schema is itself a valid draft 2020-12 schema (metaschema);
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

if [ "$#" -gt 0 ]; then
    MODE='explicit'
    set -- "$SCHEMA" "$MODE" "$@"
else
    MODE='corpus'
    files=$(find corpus/golden -name ir.json 2>/dev/null | sort)
    # shellcheck disable=SC2086
    set -- "$SCHEMA" "$MODE" $files
fi

# shellcheck disable=SC2086
$RUN - "$@" <<'PY'
import sys, json

try:
    from jsonschema import Draft202012Validator
    from jsonschema.exceptions import best_match, SchemaError
except Exception as exc:  # pragma: no cover
    sys.stderr.write("FAIL: could not import jsonschema: %s\n" % exc)
    sys.exit(2)

schema_path = sys.argv[1]
mode = sys.argv[2]
files = sys.argv[3:]

with open(schema_path, encoding="utf-8") as fh:
    schema = json.load(fh)

# check 1: the schema is a valid draft 2020-12 schema.
try:
    Draft202012Validator.check_schema(schema)
except SchemaError as exc:
    sys.stderr.write("FAIL: %s is not a valid draft 2020-12 schema:\n  %s\n"
                     % (schema_path, exc.message))
    sys.exit(1)
validator = Draft202012Validator(schema)


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

if mode == "corpus" and n == 0:
    print("check-ir-schema: schema valid; 0 corpus files (corpus not yet populated)")
    sys.exit(0)

if failed:
    sys.stderr.write("check-ir-schema: FAIL (%d of %d file(s) bad)\n" % (failed, n))
    sys.exit(1)

print("check-ir-schema: PASS (schema valid; %d file(s) validate and round-trip)" % n)
sys.exit(0)
PY
