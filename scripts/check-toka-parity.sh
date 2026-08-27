#!/usr/bin/env bash
# Gate 11 — the TOKA parity zone (TEMPORARY, maintainer decision 2026-08-27).
#
# This gate exists ONLY while the isolated `toka/` zone exists. It is the one
# documented exception to gate-set monotonicity (CLAUDE.md): it leaves the set
# when the ©-Belastingdienst zone is removed ahead of any public flip. Until
# then it is unconditional and blocking, exactly like gate 10.
#
# Three obligations, each loud (the sister-gate contract):
#   1. the frontend builds (ANTLR parser + resolver + IR emitter, JDK 21+) and
#      the TOKA test suite under toka/ compiles + passes against it with a
#      NON-ZERO test count — a suite that ran nothing is a silent greenlight;
#   2. every pinned parity dump (`toka/**/ir.json`, emitted once with the
#      toolchain at the pin) re-emits through uchar's OWN frontend
#      byte-identically — the feature-parity proof (frontend == toolchain on
#      the ©-Belastingdienst casus the public corpus may never carry);
#   3. an empty fixture set, zero tests, or a stale holdout is a FAILURE,
#      never silently green; a missing JDK fails loudly.
#
# The reason-carrying holdout mirrors gate 7's survivor list: the full casus
# raises exactly ONE resolver ERROR — the §4.1 datum+datum casus-quirk in
# toka-23 (CasusTest pins it as the single tolerated diagnostic) — so `--ir`
# writes nothing for the whole set. The resolvable casus dump is therefore
# emitted from every fragment EXCEPT that one; the holdout is asserted present
# so the list fails when stale.
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-toka-parity: FAIL — $*" >&2; exit 1; }

command -v java  >/dev/null 2>&1 || fail "java not found (the frontend needs JDK 21+)"
command -v javac >/dev/null 2>&1 || fail "javac not found (the frontend needs JDK 21+, not a bare JRE)"
command -v make  >/dev/null 2>&1 || fail "make not found"

FE=frontends/regelspraak
TOKA=toka
[ -d "$FE" ]   || fail "$FE is missing"
[ -d "$TOKA" ] || fail "$TOKA is missing (the parity zone)"

HOLDOUT='toka-23-regel-bevestigingstijdstip-vlucht.rgs'

# 1. build the frontend (main classes) and ensure both tool jars.
build_out=$(make -C "$FE" compile 2>&1) || { printf '%s\n' "$build_out" | tail -20 >&2; fail "the frontend build is red"; }
make -C "$FE" junit-jar >/dev/null 2>&1 || fail "could not fetch the JUnit console jar"
JAR=$(ls "$FE"/lib/antlr-*-complete.jar 2>/dev/null | head -1)
JUNIT=$(ls "$FE"/lib/junit-platform-console-standalone-*.jar 2>/dev/null | head -1)
[ -n "$JAR" ]    || fail "no ANTLR jar under $FE/lib after the build"
[ -n "$JUNIT" ]  || fail "no JUnit jar under $FE/lib after the build"
[ -d "$FE/out" ] || fail "no compiled frontend classes under $FE/out after the build"

# 2. compile + run the TOKA test suite against the frontend classes. The tests
#    carry unswept ©-material, so they live under toka/ (not frontends/, which
#    gate 1 keeps TOKA-free) and compile into a scratch dir scanned in
#    isolation — java runs with cwd = repo root, so CasusTest's Path.of(
#    "toka/casus") resolves.
TEST_SRC=$(find "$TOKA" -name '*.java')
[ -n "$TEST_SRC" ] || fail "no TOKA test sources under $TOKA (an empty test set is a failure)"
TOUT=$(mktemp -d -t toka-tests-XXXXXX)
trap 'rm -rf "$TOUT"' EXIT
if ! javac -cp "$JAR:$JUNIT:$FE/out" -d "$TOUT" $TEST_SRC 2>"$TOUT/javac.err"; then
  cat "$TOUT/javac.err" >&2
  fail "the TOKA test sources do not compile against the frontend"
fi
suite_out=$(java -jar "$JUNIT" execute --class-path "$JAR:$FE/out:$TOUT" \
  --scan-class-path "$TOUT" --details=summary --disable-banner 2>&1)
suite_status=$?
if [ $suite_status -ne 0 ]; then
  printf '%s\n' "$suite_out" | tail -30 >&2
  fail "the TOKA test suite is red"
fi
tests=$(printf '%s\n' "$suite_out" | sed -n 's/^\[ *\([0-9][0-9]*\) tests successful *\]$/\1/p' | tail -1)
[ -n "$tests" ] || fail "the suite produced no '[ N tests successful ]' line — it did not run"
[ "$tests" -gt 0 ] || fail "the suite ran zero tests (an empty test set is a failure)"

# 3. re-emit every pinned casus dump and require byte-identity.
casus_n=0; frag_n=0
while IFS= read -r dump; do
  d=$(dirname "$dump"); name=${d#"$TOKA"/}
  args=(); n=0
  while IFS= read -r rgs; do
    if [ "$(basename "$rgs")" = "$HOLDOUT" ]; then continue; fi
    args+=("$rgs"); n=$((n + 1))
  done < <(find "$d" -maxdepth 1 -name '*.rgs' | sort)
  [ "$n" -gt 0 ] || fail "$name carries an ir.json but no re-emittable .rgs beside it"
  out=$(mktemp -t toka-parity-XXXXXX.json)
  if ! java -cp "$JAR:$FE/out" \
      io.github.yhavinga.regelspraak.resolver.Resolver --ir "$out" "${args[@]}" \
      >/dev/null 2>&1; then
    rm -f "$out"; fail "$name: the frontend refuses or errors re-emitting the casus"
  fi
  if ! cmp -s "$out" "$dump"; then
    echo "DIFF: $name (re-emitted IR diverges from the pinned toolchain dump)" >&2
    rm -f "$out"; fail "$name: the frontend output diverges from the pinned dump"
  fi
  rm -f "$out"; casus_n=$((casus_n + 1)); frag_n=$((frag_n + n))
done < <(find "$TOKA" -name ir.json | sort)

[ "$casus_n" -gt 0 ] || fail "no toka/**/ir.json parity dumps found (an empty fixture set is a failure)"
# stale-holdout guard: the reason-carrying holdout must still be present.
find "$TOKA" -name "$HOLDOUT" | grep -q . || fail "holdout '$HOLDOUT' not found under $TOKA — the reason-carrying list is stale"

echo "check-toka-parity: PASS — TOKA suite green ($tests tests); $casus_n casus dump(s), $frag_n .rgs re-emitted byte-identical (holdout: $HOLDOUT)"
