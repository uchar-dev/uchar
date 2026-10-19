#!/usr/bin/env bash
# Gate 10 — the RegelSpraak frontend conformance (frontends/regelspraak,
# the toolchain's frontend half on the copy route).
#
# Four obligations, each loud:
#   1. the frontend builds jar-only (ANTLR-generated parser + resolver +
#      IR emitter, JDK 21+) and its JUnit suite passes with a NON-ZERO
#      test count — a suite that ran nothing is a silent greenlight;
#   2. every corpus golden that carries a source.rgs re-emits through the
#      frontend's --ir mode byte-identically to the checked-in ir.json —
#      the emitter-golden conformance the toolchain ran against this
#      repo's pin, now self-hosted;
#   3. an empty rgs-golden set is a FAILURE, never silently green, and a
#      missing JDK fails loudly (a gate that skips when its tool is
#      absent is a silent greenlight);
#   4. every message the frontend raises carries a code from one register,
#      held in both directions by scripts/check-rgs-codes.sh (card
#      frontend-codes, 2026-10-19), which carries its own floor; since the
#      card message-catalogues (2026-10-19) the same script holds the Dutch and
#      the English catalogue of the message texts to the register, and the
#      sites to passing no text.
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-regelspraak: FAIL — $*" >&2; exit 1; }

# --- monotone floors (maintainer decision 2026-09-09) --------------------------------------------
# Lower bounds, never equalities: they catch SHRINK — the suite that quietly runs half — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
# Both counts are unseen elsewhere: the JUnit suite is not corpus files at all, and the golden count
# is the number of source.rgs sources, which no other gate reads (gate 2 floors the ir.json beside
# them, so a deleted source would otherwise cost frontend coverage in silence).
EXPECTED_MIN_TESTS=173    # 2026-09-10: measured 173 (frontends/regelspraak, `make test`)
EXPECTED_MIN_GOLDENS=80   # 2026-09-25: measured 80 (corpus/golden/**/source.rgs). RAISED from 38
                          # (measured 2026-09-10): five sources give object creation, the power, a
                          # rounding other than floor, the ordered allocation and a decision table
                          # with rows their first RegelSpraak golden (the card
                          # rgs-getuigen-bronloze-constructen), and two dialect twins arrived beside
                          # them; at 38, 42 sources could go without this gate failing

command -v java  >/dev/null 2>&1 || fail "java not found (the frontend needs JDK 21+)"
command -v javac >/dev/null 2>&1 || fail "javac not found (the frontend needs JDK 21+, not a bare JRE)"
command -v make  >/dev/null 2>&1 || fail "make not found"

FE=frontends/regelspraak
[ -d "$FE" ] || fail "$FE is missing"

# --- the carrier check of the message register (card frontend-codes, 2026-10-19) ---------------------------------
# Every message the frontend raises carries a code `RGS-###` and named parameters, from one register
# (diagnostic-codes.tsv); its text is the entry of the code in the catalogue of the language (card message-catalogues).
# The script checks register and sources in both directions (every code a site passes is registered, every
# registered code is passed, the parameter names agree, no site without a code or with a text), the catalogues against
# the register, and holds floors on the register's and the catalogues' size. It runs first: it reads sources and needs
# no build.
bash scripts/check-rgs-codes.sh || fail "the register of message codes does not hold (scripts/check-rgs-codes.sh)"

# Build + run the JUnit suite via the standalone console launcher. The
# launcher exits non-zero on any failing test; the count is verified
# separately because an empty class-path scan exits ZERO having run
# nothing — the hunted silent green.
suite_out=$(make -C "$FE" test 2>&1)
suite_status=$?
if [ $suite_status -ne 0 ]; then
  echo "$suite_out" | tail -25 >&2
  fail "the frontend build or its JUnit suite is red"
fi
tests=$(printf '%s\n' "$suite_out" | sed -n 's/^\[ *\([0-9][0-9]*\) tests successful *\]$/\1/p' | tail -1)
[ -n "$tests" ] || fail "the suite produced no '[ N tests successful ]' line — it did not run"
[ "$tests" -gt 0 ] || fail "the suite ran zero tests (an empty test set is a failure)"
[ "$tests" -ge "$EXPECTED_MIN_TESTS" ] \
  || fail "the suite ran $tests tests, but EXPECTED_MIN_TESTS pins at least $EXPECTED_MIN_TESTS — the suite shrank (a deliberately dropped test lowers the floor in the same commit)"

JAR=$(ls "$FE"/lib/antlr-*-complete.jar 2>/dev/null | head -1)
[ -n "$JAR" ] || fail "no ANTLR jar under $FE/lib after the build"
[ -d "$FE/out" ] || fail "no compiled classes under $FE/out after the build"

# Re-emit every emitter golden (a source.rgs beside its ir.json) and
# require byte-identity — byte-identity implies schema-validity and
# verifier-cleanliness, because gates 2 and 3 pin those on the same
# checked-in files.
pass=0; diffcount=0; emitfail=0
while IFS= read -r src; do
  d=$(dirname "$src"); n=${d#corpus/golden/}
  [ -f "$d/ir.json" ] || fail "$n carries source.rgs but no ir.json"
  out=$(mktemp -t regelspraak-gate-XXXXXX.json)
  if ! java -cp "$JAR:$FE/out" \
      dev.uchar.regelspraak.resolver.Resolver --ir "$out" "$src" \
      >/dev/null 2>&1; then
    echo "EMIT FAIL: $n (the frontend refuses or errors on its own golden source)"
    emitfail=$((emitfail + 1)); rm -f "$out"; continue
  fi
  if cmp -s "$out" "$d/ir.json"; then
    pass=$((pass + 1))
  else
    echo "DIFF: $n (frontend output diverges from the checked-in golden)"
    diffcount=$((diffcount + 1))
  fi
  rm -f "$out"
done < <(find corpus/golden -name source.rgs | sort)

[ $((pass + diffcount + emitfail)) -gt 0 ] \
  || fail "no rgs goldens found under corpus/golden (an empty set is a failure)"
if [ $diffcount -gt 0 ] || [ $emitfail -gt 0 ]; then
  fail "$pass identical, $diffcount diverging, $emitfail emit failures"
fi
[ "$pass" -ge "$EXPECTED_MIN_GOLDENS" ] \
  || fail "$pass rgs golden(s) re-emitted, but EXPECTED_MIN_GOLDENS pins at least $EXPECTED_MIN_GOLDENS — the frontend's golden coverage shrank (a deliberate removal lowers the floor in the same commit)"
echo "check-regelspraak: PASS — JUnit suite green ($tests tests, floor $EXPECTED_MIN_TESTS); $pass rgs golden(s) re-emit byte-identically (floor $EXPECTED_MIN_GOLDENS)"
