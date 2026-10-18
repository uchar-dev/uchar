#!/usr/bin/env bash
# Gate 11 — the TOKA parity zone (maintainer decision 2026-08-27; a regular
# gate of the set since 2026-10-13, when the maintainer decided that the
# `toka/` zone stays).
#
# Unconditional and blocking. NOTE: it is NOT "exactly like gate 10"
# any more — obligation 5 (the differential replay, added by decision V6-A)
# needs the FULL toolchain of gates 6 + 12 combined: OCaml/dune (ref + clingo,
# and since obligation 4 the verifier of gate 3),
# the clingo binary, AND the Drools shaded jar, on top of gate 10's JDK 21. The
# CI job (`toka-parity` in .github/workflows/gates.yml) provisions exactly that;
# on a runner missing any of them this gate fails LOUDLY (never silently green).
#
# Six obligations, each loud (the sister-gate contract):
#   1. the frontend builds (ANTLR parser + resolver + IR emitter, JDK 21+) and
#      the TOKA test suite under toka/ compiles + passes against it with a
#      NON-ZERO test count — a suite that ran nothing is a silent greenlight;
#   2. every pinned parity dump (`toka/**/ir.json`) re-emits through uchar's
#      OWN frontend byte-identically — the feature-parity proof (frontend ==
#      toolchain on the TOKA casus, which the corpus does not carry), MINUS
#      the recorded divergences. The dump was emitted once with
#      the toolchain at the pin; where uchar's emitter has since changed ON
#      PURPOSE the pin was re-taken in that card and listed in toka/README.md
#      ("Deliberate divergences from the toolchain emission"), and for those
#      constructs this leg proves "frontend == the recorded state", not
#      toolchain agreement. That list carries every re-pin with its date and
#      its card, one entry per landing since the first (maintainer decision
#      2026-09-18: a Tekst literal's value is the PARSED string, not the
#      source snippet with its own outer '"'), so this comment does not count
#      them. Divergence by holdout (toka-45, below) and divergence by re-pin
#      are both recorded; a silent one is not available;
#   3. every replay slice (`toka/cases/**/ir.json`) obeys the SAME metaschema
#      + byte-round-trip discipline gate 2 enforces on the corpus, run
#      here in explicit mode via scripts/check-ir-schema.sh — one serialiser,
#      one source of truth. Gate 2 scans only corpus/, so without this leg the
#      newly-committed IR wire artifacts would ride only on the more-permissive
#      ref/clingo/Drools loaders (defense-in-depth for the zone);
#   4. every replay slice is WELL-FORMED (the card toka-welgevormd, 2026-09-25):
#      verifier-clean under gate 3's OCaml verifier, built as gate 3 builds it, so a
#      slice is held to the rules the corpus is held to and not only to
#      schema, round-trip and replay. A slice carries beside its scenario whatever
#      anchors its model (toka/README.md). The reason-carrying VERIFIER_HOLDOUTS list
#      names each slice held out with the one rule and pointer it is rejected at; a
#      holdout that turns clean, or is rejected anywhere else, fails the gate. The
#      casus dump stays OUTSIDE this leg (see VERIFIER_HOLDOUTS for the reason);
#   5. the DIFFERENTIAL replay (decision V6-A, 2026-08-27): every toka case under
#      toka/cases/<scenario>/{ir.json,case.json} replays through ref AND clingo
#      AND the uchar-IR-loading Drools backend, and its observed outcome in each
#      executor MUST match the class the parity manifest pins — exact agreement
#      where all three carry, a LOUD refusal (clingo translation fault / Drools
#      per-form EXCLUDE) where one does not. ZERO silent divergences is the hard
#      rule: a value difference WITHOUT a fault (a ref non-match, a Drools FAIL or
#      ERROR at fire) reddens the gate. Per-class counts land in the report;
#   6. an empty fixture set, zero tests, a stale holdout, or a toka case dir the
#      manifest does not cover is a FAILURE, never silently green; a missing JDK,
#      dune (the verifier, ref/clingo), clingo, or the Drools jar fails loudly.
#
# The reason-carrying holdout LIST mirrors gate 7's survivor list: the full casus
# raises exactly these resolver ERRORs, so `--ir` writes nothing for the whole set.
# The resolvable casus dump is therefore emitted from every fragment EXCEPT these,
# each present-asserted so the list fails when stale (and CasusTest pins the same
# two as its tolerated diagnostics, failing if either stops erroring):
#   toka-23 — the §4.1 datum+datum casus-quirk (bevestigingsinterval, a Datum-tijd
#             parameter added to a Datum-tijd, forbidden by Tabel 2).
#   toka-45 — dimensionally inconsistent AS WRITTEN: a unitless Bedrag minus a
#             km-typed product. The eenheid-identiteit typering (besluit 16Y) types
#             the product (unitloos × km = km) and the min then fails the unit rule
#             — uchar-v1 diverges from the toolchain here. The .rgs itself STAYS in
#             original form; only its parity coverage moved to this holdout list.
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

# --- monotone floors (maintainer decision 2026-09-09) --------------------------------------------
# Lower bounds, never equalities: they catch SHRINK — the suite that quietly runs half — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
# The three counts are unseen elsewhere: this zone is outside corpus/, so no other gate reads a
# single file of it. The casus-dump count is deliberately NOT floored — it is 1, and the existing
# "-gt 0" is already that floor.
EXPECTED_MIN_TESTS=27       # 2026-09-10: measured 27 (the zone's own JUnit suite)
EXPECTED_MIN_FRAGMENTS=45   # 2026-09-10: measured 45 (.rgs fragments re-emitted byte-identical)
EXPECTED_MIN_CASES=29       # 2026-09-10: measured 29 (manifest-pinned replay cases)
EXPECTED_MIN_CLEAN=28       # 2026-09-25: measured 28 (verifier-clean replay slices; the verifier holdouts not counted)

HOLDOUTS=(
  'toka-23-regel-bevestigingstijdstip-vlucht.rgs'   # §4.1 datum+datum casus-quirk
  'toka-45-regel-belasting-op-basis-van-afstand.rgs' # dimensioneel inconsistent (16Y): unitless Bedrag min km-product
)
is_holdout() { local base=$1 h; for h in "${HOLDOUTS[@]}"; do [ "$base" = "$h" ] && return 0; done; return 1; }

# The replay slices the verifier leg (obligation 4) holds out: '<scenario key> <rule> <pointer>', the
# key resolving to exactly one directory toka/cases/scenario-<key>-*, and the rule and pointer being
# the slice's WHOLE verifier report. Stale in either direction: a holdout that turns clean, that is
# rejected at another rule or pointer as well, or whose key resolves to no slice fails the gate.
#   toka-24 — a property of the casus, not an emitter defect: the clamp's minimum is a literal written
#             WITH a unit over a base whose operands are declared without one — the one-sided pair
#             WF-099 refuses and IR-SPEC ch. 4.1 leaves open as one decision for WF-023, WF-035,
#             WF-036 and WF-099 together. The frontend writes the unit the source writes and leaves a
#             unitless bound unitless for the verifier to adopt; every executor replays the slice.
# The casus dump (toka/casus/ir.json) is outside the leg altogether. As one document it is
# overdetermined by nature: its scenarios each write the same targets in their own way, legitimate per
# scenario and not as one program (the competing writes of WF-026 and WF-079). It also declares less
# than the slices, which carry the scenario test model: every name-carrying WF-034 in it, and its one
# WF-022, names an attribute or a characteristic the slices declare and the casus uses without
# declaring. Measured 2026-09-25: 15 reports, one per pointer and sentence (WF-026 drops its repeats
# since the card verifier-allocatie-keten; 18 lines before). 14 since 2026-10-06: the WF-045 cycle
# through the contingent's object creation and fact creation was the fact creation's population
# proxy (RUNTIME-SEMANTICS ch. 6.1), which went that day. Its role is the parity object of
# obligation 2, not a well-formed program.
VERIFIER_HOLDOUTS=(
  'toka-24 WF-099 /rules/rules/0/versions/0/value/base/minimum'
)

# 1. build the frontend (main classes) and ensure both tool jars.
build_out=$(make -C "$FE" compile 2>&1) || { printf '%s\n' "$build_out" | tail -20 >&2; fail "the frontend build is red"; }
make -C "$FE" junit-jar >/dev/null 2>&1 || fail "could not fetch the JUnit console jar"
JAR=$(ls "$FE"/lib/antlr-*-complete.jar 2>/dev/null | head -1)
JUNIT=$(ls "$FE"/lib/junit-platform-console-standalone-*.jar 2>/dev/null | head -1)
[ -n "$JAR" ]    || fail "no ANTLR jar under $FE/lib after the build"
[ -n "$JUNIT" ]  || fail "no JUnit jar under $FE/lib after the build"
[ -d "$FE/out" ] || fail "no compiled frontend classes under $FE/out after the build"

# 2. compile + run the TOKA test suite against the frontend classes. The tests
#    carry TOKA material, so they live under toka/ (not frontends/, which
#    gate 1 keeps TOKA-free) and compile into a scratch dir scanned in
#    isolation — java runs with cwd = repo root, so CasusTest's Path.of(
#    "toka/casus") resolves.
TEST_SRC=$(find "$TOKA" -name '*.java')
[ -n "$TEST_SRC" ] || fail "no TOKA test sources under $TOKA (an empty test set is a failure)"
TOUT=$(mktemp -d -t toka-tests-XXXXXX)
REPLAY_TMP=""
cleanup() { rm -rf "${TOUT:-}" "${REPLAY_TMP:-}"; }
trap cleanup EXIT
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
[ "$tests" -ge "$EXPECTED_MIN_TESTS" ] \
  || fail "the suite ran $tests tests, but EXPECTED_MIN_TESTS pins at least $EXPECTED_MIN_TESTS — the suite shrank (a deliberately dropped test lowers the floor in the same commit)"

# 3. re-emit every pinned casus dump and require byte-identity. The per-scenario
#    slice dumps under toka/cases/ (part 4's replay fixtures) carry no sibling
#    .rgs and are NOT re-emittable, so they are excluded here — only casus dumps
#    that sit beside their .rgs sources are the frontend==toolchain parity proof.
#    A slice therefore does NOT follow the emitter: when an emitter change
#    moves a construct, the slices move by hand in the same card
#    (toka/README.md), or the zone carries two spellings of one construct and
#    this leg cannot see it.
case_n=0; frag_n=0
while IFS= read -r dump; do
  d=$(dirname "$dump"); name=${d#"$TOKA"/}
  args=(); n=0
  while IFS= read -r rgs; do
    if is_holdout "$(basename "$rgs")"; then continue; fi
    args+=("$rgs"); n=$((n + 1))
  done < <(find "$d" -maxdepth 1 -name '*.rgs' | sort)
  [ "$n" -gt 0 ] || fail "$name carries an ir.json but no re-emittable .rgs beside it"
  out=$(mktemp -t toka-parity-XXXXXX.json)
  if ! java -cp "$JAR:$FE/out" \
      dev.uchar.regelspraak.resolver.Resolver --ir "$out" "${args[@]}" \
      >/dev/null 2>&1; then
    rm -f "$out"; fail "$name: the frontend refuses or errors re-emitting the casus"
  fi
  if ! cmp -s "$out" "$dump"; then
    echo "DIFF: $name (re-emitted IR diverges from the pinned toolchain dump)" >&2
    rm -f "$out"; fail "$name: the frontend output diverges from the pinned dump"
  fi
  rm -f "$out"; case_n=$((case_n + 1)); frag_n=$((frag_n + n))
done < <(find "$TOKA" -name ir.json -not -path "$TOKA/cases/*" | sort)

[ "$case_n" -gt 0 ] || fail "no toka/**/ir.json parity dumps found (an empty fixture set is a failure)"
[ "$frag_n" -ge "$EXPECTED_MIN_FRAGMENTS" ] \
  || fail "$frag_n .rgs fragment(s) re-emitted, but EXPECTED_MIN_FRAGMENTS pins at least $EXPECTED_MIN_FRAGMENTS — the parity coverage shrank (a deliberate removal, or a new holdout, lowers the floor in the same commit)"
# stale-holdout guard: every reason-carrying holdout must still be present.
for h in "${HOLDOUTS[@]}"; do
  find "$TOKA" -name "$h" | grep -q . || fail "holdout '$h' not found under $TOKA — the reason-carrying list is stale"
done

# 3b. schema + round-trip discipline over the replay slices. Gate 2 scans only
#     corpus/; these newly-committed IR wire artifacts are otherwise unguarded.
#     Reuse the gate-2 script in explicit mode — same metaschema, same canonical
#     serialiser, one source of truth — so a slice editing into a schema-invalid
#     or non-byte-round-tripping shape reddens the gate here.
slice_irs=$(find "$TOKA/cases" -name ir.json | sort)
[ -n "$slice_irs" ] || fail "no $TOKA/cases/**/ir.json slices to schema-check"
# shellcheck disable=SC2086
if ! schema_out=$(sh scripts/check-ir-schema.sh $slice_irs 2>&1); then
  printf '%s\n' "$schema_out" >&2
  fail "a toka replay slice is schema-invalid or does not round-trip byte-identically (gate-2 discipline)"
fi
slice_n=$(printf '%s\n' "$slice_irs" | grep -c .)

# 3c. well-formedness (obligation 4): every replay slice verifier-clean, bar the VERIFIER_HOLDOUTS.
#     Only a failure prints the verifier's report: its messages carry the zone's own names.
if ! command -v dune >/dev/null 2>&1; then
  command -v opam >/dev/null 2>&1 && eval "$(opam env)"
fi
command -v dune >/dev/null 2>&1 || fail "dune not found (the verifier, ref and clingo need OCaml/dune)"
dune build --root spec/verifier >/dev/null 2>&1 || fail "the verifier build is red (dune build --root spec/verifier, as gate 3 builds it)"
VERIFIER=spec/verifier/_build/default/bin/verifier.exe
[ -x "$VERIFIER" ] || fail "no verifier binary at $VERIFIER after the build"
VH_DIRS=(); VH_PINS=()
# The list may be empty: bash 3.2 under set -u calls "${a[@]}" on an empty array unbound, so every
# expansion of the list goes through the ${a[@]+...} form.
for e in ${VERIFIER_HOLDOUTS[@]+"${VERIFIER_HOLDOUTS[@]}"}; do
  key=${e%% *}
  dirs=$(find "$TOKA/cases" -mindepth 1 -maxdepth 1 -type d -name "scenario-$key-*" | sort)
  nd=$(printf '%s\n' "$dirs" | grep -c .)
  [ "$nd" -eq 1 ] || fail "verifier holdout '$key' resolves to $nd slice directories, not one — the reason-carrying list is stale"
  VH_DIRS+=("$(basename "$dirs")"); VH_PINS+=("${e#* }")
done
vh_pin_of() { local i; for i in "${!VH_DIRS[@]}"; do [ "${VH_DIRS[$i]}" = "$1" ] && { printf '%s' "${VH_PINS[$i]}"; return 0; }; done; return 1; }
declare -a VERIFY_FAIL=()
clean_n=0; vheld_n=0
while IFS= read -r ir; do
  scen=$(basename "$(dirname "$ir")")
  vout=$("$VERIFIER" "$ir" 2>&1); vrc=$?
  if pin=$(vh_pin_of "$scen"); then
    got=$(printf '%s\n' "$vout" | sed -n 's/^\(WF-[0-9][0-9]*\) FAIL \([^:]*\):.*/\1 \2/p')
    if [ $vrc -eq 0 ]; then
      VERIFY_FAIL+=("$scen: a verifier holdout, but verifier-clean now — the reason-carrying list is stale")
    elif [ $vrc -ne 1 ] || [ "$got" != "$pin" ]; then
      printf '%s\n' "$vout" >&2
      VERIFY_FAIL+=("$scen: a verifier holdout pinned to '$pin', but its report differs (exit $vrc) — a holdout hides no second defect")
    else
      vheld_n=$((vheld_n + 1))
    fi
  elif [ $vrc -eq 0 ]; then
    clean_n=$((clean_n + 1))
  else
    printf '%s\n' "$vout" >&2
    VERIFY_FAIL+=("$scen: not verifier-clean (exit $vrc)")
  fi
done <<<"$slice_irs"
if [ ${#VERIFY_FAIL[@]} -gt 0 ]; then
  echo "check-toka-parity: FAIL — ${#VERIFY_FAIL[@]} replay slice(s) break the well-formedness leg:" >&2
  for f in "${VERIFY_FAIL[@]}"; do echo "    $f" >&2; done
  fail "the well-formedness leg found ${#VERIFY_FAIL[@]} slice(s) out of line (every slice verifier-clean, bar the pinned holdouts)"
fi
[ "$clean_n" -gt 0 ] || fail "no verifier-clean replay slice (an empty set is a failure)"
[ "$clean_n" -ge "$EXPECTED_MIN_CLEAN" ] \
  || fail "$clean_n replay slice(s) verifier-clean, but EXPECTED_MIN_CLEAN pins at least $EXPECTED_MIN_CLEAN — the well-formed set shrank (a new holdout, or a deliberate removal, lowers the floor in the same commit)"
[ "$vheld_n" -eq "${#VERIFIER_HOLDOUTS[@]}" ] \
  || fail "$vheld_n of ${#VERIFIER_HOLDOUTS[@]} verifier holdout(s) matched their pinned report — the reason-carrying list is stale"

# 4. the differential replay (decision V6-A): every toka case through ref, clingo, Drools.
MANIFEST="$TOKA/cases/parity.manifest"
[ -f "$MANIFEST" ] || fail "no parity manifest at $MANIFEST (the replay leg has nothing pinned)"

# ref + clingo (OCaml) — build at the workspace root (dune was located in 3c).
command -v clingo >/dev/null 2>&1 || fail "clingo not found on PATH (the differential needs it)"
dune build ref backends >/dev/null 2>&1 || fail "the ref/clingo build is red"
REF=_build/default/ref/bin/runner.exe
CLINGO=_build/default/backends/clingo/bin/clingo_runner.exe
[ -x "$REF" ] && [ -x "$CLINGO" ] || fail "ref/clingo binaries missing after the build"

# the Drools backend jar — consumes the uchar IR off the wire, taken FRESH OR REBUILT
# (scripts/lib/drools-jar.sh, 2026-09-23): rebuilt when it is absent or older than any path it is
# built from, and exactly one shaded jar is demanded. The lens-proven stale-jar flank — a
# build-only-when-absent jar can silently predate the source under review — stays closed, because
# the test is "older than the source", not "absent". From 2026-08-30 until then this step rebuilt
# the jar unconditionally with `mvn package`, the backend's JUnit suite included; that suite is gate
# 12's obligation (its one `mvn package` runs it and refreshes the jar), so it deliberately no
# longer runs here. mvn is a hard requirement, exactly as it is of gate 12: only mvn can rebuild a
# stale jar.
command -v mvn >/dev/null 2>&1 || fail "mvn not found — the Drools leg takes the shaded jar fresh or rebuilt, and only mvn can rebuild a stale one (stale-jar guard)"
. scripts/lib/drools-jar.sh || fail "could not read scripts/lib/drools-jar.sh"
DJAR=$(ensure_drools_jar) || fail "no fresh Drools shaded jar (the drools-jar line above says why)"

REPLAY_TMP=$(mktemp -d -t toka-replay-XXXXXX)
CFRAGS="$REPLAY_TMP/clingo-faults"; DFRAGS="$REPLAY_TMP/drools-excludes"; : >"$CFRAGS"; : >"$DFRAGS"
declare -a REPLAY_FAIL=()
seen="$REPLAY_TMP/seen"; : >"$seen"
cases_n=0
ref_pass=0
clingo_pass=0; clingo_fault=0
drools_play=0; drools_excl=0

while IFS='|' read -r scen ref clingo cfrag drools dfrag; do
  case "$scen" in ''|\#*) continue;; esac
  dir="$TOKA/cases/$scen"; ir="$dir/ir.json"; cs="$dir/case.json"
  [ -f "$ir" ] && [ -f "$cs" ] || fail "manifest names '$scen' but $dir lacks ir.json/case.json"
  printf '%s\n' "$scen" >>"$seen"
  cases_n=$((cases_n + 1))

  # --- ref: the ref-100 oracle carries every toka case; a non-match is a silent divergence.
  if [ "$ref" = PASS ]; then
    if "$REF" "$ir" "$cs" >/dev/null 2>&1; then ref_pass=$((ref_pass + 1))
    else REPLAY_FAIL+=("$scen: ref pinned PASS but refused/diverged (a value difference without a fault fails the gate)"); fi
  else fail "manifest: unexpected ref token '$ref' for $scen (ref is the 100% oracle over this set)"; fi

  # --- clingo: PASS matches the same pinned expectations, else a LOUD translation fault with the fragment.
  cout=$("$CLINGO" "$ir" "$cs" 2>&1); cc=$?
  if [ "$clingo" = PASS ]; then
    if [ $cc -eq 0 ]; then clingo_pass=$((clingo_pass + 1))
    else REPLAY_FAIL+=("$scen: clingo pinned PASS but did not replay: $(printf '%s' "$cout" | head -1)"); fi
  elif [ "$clingo" = FAULT ]; then
    if [ $cc -ne 0 ] && printf '%s' "$cout" | grep -qF "$cfrag"; then
      clingo_fault=$((clingo_fault + 1)); printf '%s\n' "$cfrag" >>"$CFRAGS"
    else REPLAY_FAIL+=("$scen: clingo pinned loud FAULT '$cfrag' but got exit=$cc: $(printf '%s' "$cout" | head -1)"); fi
  else fail "manifest: unexpected clingo token '$clingo' for $scen"; fi

  # --- Drools: PLAYED (lowered + matched) or a LOUD per-form EXCLUDE with the fragment. A FAIL/ERROR at
  #     fire is a value difference without a fault (or a crash) and always reddens the gate — the hard rule.
  dout=$(java -jar "$DJAR" "$ir" "$cs" 2>/dev/null | tail -1); dtok=${dout%% *}
  if [ "$drools" = PLAYED ]; then
    if [ "$dtok" = PLAYED ]; then drools_play=$((drools_play + 1))
    else REPLAY_FAIL+=("$scen: Drools pinned PLAYED but got: $dout"); fi
  elif [ "$drools" = EXCLUDE ]; then
    if [ "$dtok" = EXCLUDE ] && printf '%s' "$dout" | grep -qF "$dfrag"; then
      drools_excl=$((drools_excl + 1)); printf '%s\n' "$dfrag" >>"$DFRAGS"
    else REPLAY_FAIL+=("$scen: Drools pinned loud EXCLUDE '$dfrag' but got: $dout"); fi
  else fail "manifest: unexpected drools token '$drools' for $scen"; fi
done < "$MANIFEST"

[ "$cases_n" -gt 0 ] || fail "the parity manifest pins zero toka cases (an empty replay set is a failure)"
[ "$cases_n" -ge "$EXPECTED_MIN_CASES" ] \
  || fail "$cases_n replay case(s) pinned, but EXPECTED_MIN_CASES pins at least $EXPECTED_MIN_CASES — the manifest shrank (a deliberate removal lowers the floor in the same commit)"

# every toka case dir must be covered by the manifest — no silent skip.
while IFS= read -r cdir; do
  s=$(basename "$(dirname "$cdir")")
  grep -qxF "$s" "$seen" || fail "toka case '$s' has an ir.json but no manifest line — it would be silently skipped"
done < <(find "$TOKA/cases" -name ir.json | sort)

# the hard rule: any unpinned outcome — a silent value divergence among them — reddens the gate.
if [ ${#REPLAY_FAIL[@]} -gt 0 ]; then
  echo "check-toka-parity: FAIL — ${#REPLAY_FAIL[@]} toka case(s) diverged from the pinned parity class:" >&2
  for f in "${REPLAY_FAIL[@]}"; do echo "    $f" >&2; done
  fail "the differential replay found ${#REPLAY_FAIL[@]} divergence(s) (zero silent divergences is the hard rule)"
fi

echo ""
echo "check-toka-parity: PASS"
echo "  re-emission: $case_n casus dump(s), $frag_n .rgs re-emitted byte-identical, floor $EXPECTED_MIN_FRAGMENTS (holdouts: ${HOLDOUTS[*]}); TOKA suite $tests tests, floor $EXPECTED_MIN_TESTS"
echo "  schema: $slice_n replay slice(s) metaschema-valid + byte-round-trip (gate-2 discipline, explicit mode)"
vh_list=$(printf '%s; ' ${VERIFIER_HOLDOUTS[@]+"${VERIFIER_HOLDOUTS[@]}"} | sed 's/; $//')
echo "  well-formedness: $clean_n replay slice(s) verifier-clean, floor $EXPECTED_MIN_CLEAN (verifier holdouts: ${vh_list:-none}); the casus dump is outside this leg (the parity object, not a well-formed program)"
echo "  differential replay: $cases_n toka cases (floor $EXPECTED_MIN_CASES) through ref, clingo, Drools (slices under $TOKA/cases/), ZERO silent divergences"
echo "    ref    : $ref_pass PASS / 0 diverge  (the 100% oracle carries every toka case)"
echo "    clingo : $clingo_pass PASS / $clingo_fault loud translation-fault / 0 silent diverge"
echo "    Drools : $drools_play PLAYED / $drools_excl loud per-form EXCLUDE / 0 FAIL-or-ERROR"
echo "    clingo faults by class:"
sort "$CFRAGS" | uniq -c | sed 's/^/     /'
echo "    Drools excludes by form:"
sort "$DFRAGS" | uniq -c | sed 's/^/     /'
