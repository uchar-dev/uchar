#!/usr/bin/env bash
# Gate 14 — the F-logic frontend conformance (frontends/flogic, the second
# independent source language for the kernel).
#
# Three obligations, each loud, on the model of gate 10:
#   1. the frontend builds and its own suite passes with a NON-ZERO check
#      count — the suite emits each test program, asserts it verifier-clean,
#      replays its cases through ref AND clingo, and drives one fixture per
#      refusal; a suite that ran nothing is a silent greenlight;
#   2. every corpus golden that carries a bron.flogic re-emits through the
#      frontend and, after canonicalisation (the same serialiser shape gate 2
#      enforces, executed from that script — one source of truth), is
#      byte-identical to the checked-in ir.json;
#   3. an empty flogic-golden set is a FAILURE, never silently green, and a
#      missing dune fails loudly (a gate that skips when its tool is absent
#      is a silent greenlight).
#
# Unlike gate 9 this gate needs no external compiler: the frontend is plain
# OCaml in the workspace, so its CI leg is unconditional.
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-flogic: FAIL — $*" >&2; exit 1; }

# --- monotone floors (maintainer decision 2026-09-09) --------------------------------------------
# Lower bounds, never equalities: they catch SHRINK — the suite that quietly runs half — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
# Both counts are unseen elsewhere: the suite is not corpus files at all, and the golden count is
# the number of bron.flogic sources, which no other gate reads (gate 2 floors the ir.json beside
# them, so a deleted source would otherwise cost frontend coverage in silence).
EXPECTED_MIN_CHECKS=81   # 2026-09-12: measured 81 (frontends/flogic/test, the runtest alias)
EXPECTED_MIN_GOLDENS=1   # 2026-09-12: measured 1 (corpus/golden/**/bron.flogic)

command -v dune >/dev/null 2>&1 || fail "dune not found (run: eval \$(opam env))"
command -v python3 >/dev/null 2>&1 || fail "python3 not found (the canonicaliser needs it)"

FE=frontends/flogic
[ -d "$FE" ] || fail "$FE is missing"

dune build "$FE" 2>/dev/null || dune build "$FE" || fail "the frontend does not build"
# --force so the suite actually RUNS every time (a cached run prints
# nothing), and the check count is verified: the bare runtest alias is a
# dune default that is empty-but-valid, so "the alias built" proves
# nothing — a hunted silent green. Zero checks is a failure.
suite_out=$(dune build --force "@$FE/runtest" 2>&1)
suite_status=$?
if [ $suite_status -ne 0 ]; then
  echo "$suite_out" | tail -20 >&2
  fail "the frontend test suite is red"
fi
checks=$(printf '%s\n' "$suite_out" | sed -n 's/^\([0-9][0-9]*\) checks, 0 failures$/\1/p' | tail -1)
[ -n "$checks" ] || fail "the suite produced no '<N> checks, 0 failures' line — it did not run (an empty runtest alias is a silent greenlight)"
[ "$checks" -gt 0 ] || fail "the suite ran zero checks (an empty program list is a failure)"
[ "$checks" -ge "$EXPECTED_MIN_CHECKS" ] \
  || fail "the suite ran $checks checks, but EXPECTED_MIN_CHECKS pins at least $EXPECTED_MIN_CHECKS — the suite shrank (a deliberately dropped check lowers the floor in the same commit)"

BIN=_build/default/$FE/bin/flogic_frontend.exe
[ -x "$BIN" ] || fail "frontend binary missing at $BIN after build"

# canonicalise exactly as gate 2's round-trip serialiser does, by executing
# that script's own embedded serialiser — one shape, one source of truth,
# no drift between the gates
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

pass=0; diffcount=0; emitfail=0
while IFS= read -r src; do
  d=$(dirname "$src"); n=${d#corpus/golden/}
  [ -f "$d/ir.json" ] || fail "$n carries bron.flogic but no ir.json"
  raw=$(mktemp -t flogic-gate-XXXXXX.json)
  out=$(mktemp -t flogic-gate-canon-XXXXXX.json)
  if ! "$BIN" "$src" "$raw" >/dev/null 2>&1; then
    echo "EMIT FAIL: $n (the frontend refuses or errors on its own golden source)"
    emitfail=$((emitfail + 1)); rm -f "$raw" "$out"; continue
  fi
  canon "$raw" "$out" || fail "canonicalisation failed for $n"
  if cmp -s "$out" "$d/ir.json"; then
    pass=$((pass + 1))
  else
    echo "DIFF: $n (frontend output diverges from the checked-in golden)"
    diffcount=$((diffcount + 1))
  fi
  rm -f "$raw" "$out"
done < <(find corpus/golden -name source_name.flogic | sort)

[ $((pass + diffcount + emitfail)) -gt 0 ] \
  || fail "no flogic goldens found under corpus/golden (an empty set is a failure)"
if [ $diffcount -gt 0 ] || [ $emitfail -gt 0 ]; then
  fail "$pass identical, $diffcount diverging, $emitfail emit failures"
fi
[ "$pass" -ge "$EXPECTED_MIN_GOLDENS" ] \
  || fail "$pass flogic golden(s) re-emitted, but EXPECTED_MIN_GOLDENS pins at least $EXPECTED_MIN_GOLDENS — the frontend's golden coverage shrank (a deliberate removal lowers the floor in the same commit)"
echo "check-flogic: PASS — frontend suite green ($checks checks, floor $EXPECTED_MIN_CHECKS); $pass flogic golden(s) re-emit byte-identically (floor $EXPECTED_MIN_GOLDENS)"
