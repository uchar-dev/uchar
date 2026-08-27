#!/usr/bin/env bash
# Gate 10 — the RegelSpraak frontend conformance (frontends/regelspraak,
# the toolchain's frontend half on the copy route).
#
# Three obligations, each loud:
#   1. the frontend builds jar-only (ANTLR-generated parser + resolver +
#      IR emitter, JDK 21+) and its JUnit suite passes with a NON-ZERO
#      test count — a suite that ran nothing is a silent greenlight;
#   2. every corpus golden that carries a bron.rgs re-emits through the
#      frontend's --ir mode byte-identically to the checked-in ir.json —
#      the emitter-golden conformance the toolchain ran against this
#      repo's pin, now self-hosted;
#   3. an empty rgs-golden set is a FAILURE, never silently green, and a
#      missing JDK fails loudly (a gate that skips when its tool is
#      absent is a silent greenlight).
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-regelspraak: FAIL — $*" >&2; exit 1; }

command -v java  >/dev/null 2>&1 || fail "java not found (the frontend needs JDK 21+)"
command -v javac >/dev/null 2>&1 || fail "javac not found (the frontend needs JDK 21+, not a bare JRE)"
command -v make  >/dev/null 2>&1 || fail "make not found"

FE=frontends/regelspraak
[ -d "$FE" ] || fail "$FE is missing"

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

JAR=$(ls "$FE"/lib/antlr-*-complete.jar 2>/dev/null | head -1)
[ -n "$JAR" ] || fail "no ANTLR jar under $FE/lib after the build"
[ -d "$FE/out" ] || fail "no compiled classes under $FE/out after the build"

# Re-emit every emitter golden (a bron.rgs beside its ir.json) and
# require byte-identity — byte-identity implies schema-validity and
# verifier-cleanliness, because gates 2 and 3 pin those on the same
# checked-in files.
pass=0; diffcount=0; emitfail=0
while IFS= read -r src; do
  d=$(dirname "$src"); n=${d#corpus/golden/}
  [ -f "$d/ir.json" ] || fail "$n carries bron.rgs but no ir.json"
  out=$(mktemp -t regelspraak-gate-XXXXXX.json)
  if ! java -cp "$JAR:$FE/out" \
      io.github.yhavinga.regelspraak.resolver.Resolver --ir "$out" "$src" \
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
done < <(find corpus/golden -name bron.rgs | sort)

[ $((pass + diffcount + emitfail)) -gt 0 ] \
  || fail "no rgs goldens found under corpus/golden (an empty set is a failure)"
if [ $diffcount -gt 0 ] || [ $emitfail -gt 0 ]; then
  fail "$pass identical, $diffcount diverging, $emitfail emit failures"
fi
echo "check-regelspraak: PASS — JUnit suite green ($tests tests); $pass rgs golden(s) re-emit byte-identically"
