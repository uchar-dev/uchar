#!/usr/bin/env bash
# Gate 9 — the Catala frontend conformance (PLAN §6 Phase 5a).
#
# Three obligations, each loud:
#   1. the frontend builds and its full test suite passes
#      (frontends/catala/test: E2E through verifier+ref+clingo, the
#      refusal fixtures, the third-verdict cases);
#   2. every corpus golden that carries a bron.catala_en regenerates
#      through the plugin and, after canonicalisation (the same
#      serialiser shape gate 2 enforces), is byte-identical to the
#      checked-in ir.json — the toolchain's emitter-golden conformance
#      mirrored to the frontend side;
#   3. an empty catala-golden set is a FAILURE, never silently green,
#      and a missing catala installation fails loudly (a gate that
#      skips when its tool is absent is a silent greenlight).
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-catala: FAIL — $*" >&2; exit 1; }

command -v dune >/dev/null 2>&1 || fail "dune not found (run: eval \$(opam env))"
command -v catala >/dev/null 2>&1 \
  || fail "the catala compiler is not installed — the frontend gate cannot run (never silently green)"
command -v python3 >/dev/null 2>&1 || fail "python3 not found (the canonicaliser needs it)"

dune build frontends/catala 2>/dev/null || dune build frontends/catala \
  || fail "the frontend does not build"
# --force so the suite actually RUNS every time (a cached run prints
# nothing), and the check count is verified: the bare runtest alias is
# a dune default that is empty-but-valid, so "the alias built" proves
# nothing — a hunted silent green. Zero checks is a failure.
suite_out=$(dune build --force @frontends/catala/runtest 2>&1)
suite_status=$?
if [ $suite_status -ne 0 ]; then
  echo "$suite_out" | tail -15 >&2
  fail "the frontend test suite is red"
fi
checks=$(printf '%s\n' "$suite_out" | sed -n 's/^\([0-9][0-9]*\) checks, 0 failures$/\1/p' | tail -1)
[ -n "$checks" ] || fail "the suite produced no '<N> checks, 0 failures' line — it did not run (an empty runtest alias is a silent greenlight)"
[ "$checks" -gt 0 ] || fail "the suite ran zero checks (an empty program list is a failure)"

BIN=_build/default/frontends/catala/bin/catala_frontend.exe
[ -x "$BIN" ] || fail "plugin binary missing at $BIN after build"

# canonicalise exactly as gate 2's round-trip serialiser does, by
# executing that script's own embedded serialiser — one shape, one
# source of truth, no drift between the two gates
canon() {
  python3 - "$1" "$2" <<'PYEOF'
import json, sys
sh = open('scripts/check-ir-schema.sh').read()
ns = {'json': json}
exec(sh[sh.index('class RawNum'):sh.index('def roundtrip')], ns)
data = json.loads(open(sys.argv[1], 'rb').read().decode('utf-8'),
                  parse_int=ns['RawNum'], parse_float=ns['RawNum'])
open(sys.argv[2], 'w').write(ns['ser'](data, 0) + '\n')
PYEOF
}

pass=0; diffcount=0; genfail=0
while IFS= read -r src; do
  d=$(dirname "$src"); n=${d#corpus/golden/}
  [ -f "$d/ir.json" ] || fail "$n carries bron.catala_en but no ir.json"
  raw=$(mktemp -t catala-gate-XXXXXX.json)
  out=$(mktemp -t catala-gate-canon-XXXXXX.json)
  if ! "$BIN" "$src" "$raw" >/dev/null 2>&1; then
    echo "GENERATE FAIL: $n (the plugin refuses or errors on its own golden source)"
    genfail=$((genfail + 1)); rm -f "$raw" "$out"; continue
  fi
  canon "$raw" "$out" || fail "canonicalisation failed for $n"
  if cmp -s "$out" "$d/ir.json"; then
    pass=$((pass + 1))
  else
    echo "DIFF: $n (plugin output diverges from the checked-in golden)"
    diffcount=$((diffcount + 1))
  fi
  rm -f "$raw" "$out"
done < <(find corpus/golden -name bron.catala_en | sort)

[ $((pass + diffcount + genfail)) -gt 0 ] \
  || fail "no catala goldens found under corpus/golden (an empty set is a failure)"
if [ $diffcount -gt 0 ] || [ $genfail -gt 0 ]; then
  fail "$pass identical, $diffcount diverging, $genfail generate failures"
fi
echo "check-catala: PASS — frontend suite green ($checks checks); $pass catala golden(s) regenerate byte-identically"
