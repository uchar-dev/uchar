#!/usr/bin/env bash
# Gate 12 — the Drools differential leg of the square (maintainer decision 2026-08-27).
#
# The Drools backend CONSUMES the uchar IR: backends/drools loads every golden's ir.json off
# the wire (never the RegelSpraak source), builds a Drools KieBase through the ported exec-model
# back-end, and replays every case*.json. A golden whose forms the loader+transpiler carry MUST
# replay through Drools with EXACTLY the pinned expectations gates 5 (ref) and 6 (clingo) verify
# — so a played pass IS the differential square (ref ≡ clingo ≡ Drools over the corpus, anchored
# to hand-written values).
#
# NOT-CARRIED forms never vanish silently. Two reason-carrying channels, both loud:
#   1. PER-FORM auto-exclusions — the CLI itself refuses a form the v0 backend does not carry
#      (a loader Refusal on a v1 node/section, a transpiler MARKER/SKIP, a contract violation, a
#      temporal/dimensioned/rational/list/struct input, an OBJECTCREATIE (§13.4.6) that does not
#      execute, a WEDERKERIG self-relation (§13.3.9.2), an attributeTimeline read-back). The reason
#      is printed; these self-document per form. A form that lowers but faults / yields the wrong
#      value at fire — objectcreatie among them — is not carried and lands HERE, not in channel 2.
#   2. VALUE-DIVERGENCE exclusions — a golden that loads, lowers, fires WITHOUT faulting, and
#      produces a WRONG VALUE (a FAIL: verwacht X, werkelijk Y). The exec-model (the production
#      RegelSpraak semantics) genuinely differs from ref/clingo here; each such golden is listed
#      below WITH a reason. This channel is FAIL-ONLY: a crash at fire (ERROR) is never a value-
#      divergence — it is a not-carried form and MUST be a channel-1 auto-exclusion, so an ERROR
#      always fails the gate. The list is the check-noop survivor pattern: it FAILS WHEN STALE — a
#      listed golden that now plays+passes, or a listed golden that is gone, trips the gate.
#
# An empty played set is a FAILURE, never silently green. Report: played N, auto-excluded M,
# value-divergence K (per reason), no-case L.
#
# The backend's own JUnit suite must ALSO pass with a NON-ZERO test count (maintainer decision
# 2026-08-30, the gate-10 model): a zero-test or skipped suite is a failure, never silently green.
#
# The REPLAY runs JAR-ONLY (decision V2-B, the gate-10 pattern): the gate invokes `java -jar` per
# golden against target/uchar-drools-backend-*-shaded.jar. Since 2026-08-30 the gate itself rebuilds
# that jar unconditionally (the one `mvn package` below, which also runs the suite), so the replay
# can never run a jar that predates the source under review.
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-drools: FAIL — $*" >&2; exit 1; }

command -v java >/dev/null 2>&1 || fail "java not found (the Drools runtime needs a JDK/JRE 21+)"

# --- build + the backend's own JUnit suite, with a NON-ZERO test count (maintainer decision 2026-08-30) ----
# The gate-10 obligation, mirrored: the witness tests carry what the corpus replay cannot see — the
# shape a case input became, a write-guard branch no golden exercises — so a suite that ran NOTHING
# would be a silent greenlight for exactly the checks the corpus does not duplicate.
# DESIGN: ONE `mvn package` runs the suite AND refreshes the shaded jar, unconditionally. Building
# only-when-absent let the replay run a stale jar that predates the source under review (lens finding
# 2026-08-30), and a suite-only `mvn test` compiled fresh source while the replay stayed stale — one
# command closes both flanks, incrementally a few seconds against a multi-minute replay. Trusting a
# surefire report on disk instead would buy nothing but a way to be wrong (a report can be "fresh"
# yet predate an edit). It runs FIRST so a red or empty suite fails in seconds. The summary line is
# matched WITHOUT its log-level prefix (surefire demotes it to [WARNING] when anything is skipped)
# and skipped tests fail the gate: a skipped witness is a green subset, and a green subset is not
# green. CI already builds through mvn; locally mvn is a hard requirement of the gate, deliberately.
command -v mvn >/dev/null 2>&1 || fail "mvn not found — gate 12 builds the jar and runs the backend's JUnit suite (a skipped suite is a silent greenlight)"
suite_out=$(mvn -o -f backends/drools/pom.xml package 2>&1)
suite_status=$?
if [ $suite_status -ne 0 ]; then
  echo "$suite_out" | tail -25 >&2
  fail "the backend's build+JUnit suite is red"
fi
summary=$(printf '%s\n' "$suite_out" | sed -n 's/^\[[A-Z]*\] Tests run: \([0-9][0-9]*\), Failures: [0-9]*, Errors: [0-9]*, Skipped: \([0-9][0-9]*\).*/\1 \2/p' | tail -1)
[ -n "$summary" ] || fail "the suite produced no 'Tests run: N' summary — it did not run"
tests=${summary%% *}; skipped=${summary##* }
[ "$tests" -gt 0 ] || fail "the suite ran zero tests (an empty test set is a failure, never silently green)"
[ "$skipped" -eq 0 ] || fail "the suite skipped $skipped test(s) — a skipped witness is a silent greenlight"

JAR=$(ls backends/drools/target/uchar-drools-backend-*-shaded.jar 2>/dev/null | head -1)
[ -n "$JAR" ] || fail "the build produced no shaded jar"

# --- the value-divergence exclusion list (reason-carrying; fails when stale) -------------------
# Each entry: "<golden-relative-path>|<reason>". A golden here MUST diverge (FAIL/ERROR); if it
# plays+passes the list is stale. Grouped by the divergence class the differential exposed.
EXCLUDE_KNOWN=(
  # Triaged 2026-08-28 (card leeg-divergentie-triage) by REPLAY through the original regelspraak-java
  # toolchain (its DrlTranspiler oracle AND ExecModelTranspiler, fired over the equivalent minimal
  # .rgs): in every case the original toolchain agrees with OUR PORT and NOT with ref/clingo — so each
  # is a GENUINE v0 production-semantics divergence (class b), never a port bug. The uchar spec
  # (ref/clingo) implements principled leeg semantics; production RegelSpraak's Drools lowering does
  # not. Each is a named v1-delta candidate. NB one reason line was CORRECTED here: the kustwacht
  # mechanism is NOT a leeg operand (see below).
  # Originally SEVEN; borgvlag and vondstetiket LEFT the list 2026-08-28 (card ongelijk-leeg-uniform):
  # the ch. 1.2d inequality became the uniform Tabel-44 rule (leeg ONGELIJK gevuld → WAAR for every
  # datatype), so ref/clingo now fire betwist/omstreden exactly as Drools does — the square closed and
  # both PLAY+PASS. This was NOT production-Drools moving toward the spec: the spec moved toward the
  # Tabel-44 source and thereby toward the value production Drools already produced on this one cell.
  # gelijkdeling and voorwaardedeling ALSO LEFT the list 2026-08-28 (card verdeling-fixes): the two were
  # not principled v0 semantics but two toolchain ALLOCATION BUGS (the dropped verdeling voorwaarde; the
  # no-recipient rest overwrite). Both were FIXED at the regelspraak-java source (DrlTranspiler +
  # ExecModelTranspiler) and the identical diffs synced into backends/drools here, so ref ≡ clingo ≡
  # Drools now on both — they PLAY+PASS. The official RegelSpraak spec (§9.7.5) is silent on the
  # zero-recipient case, so the no-recipient rest becoming leeg is a bugfix, not a v1 delta.
  "anchor/jutterswet-waardestelling|v0-divergence: a gelijkstelling with a LEEG source overwrites the pre-filled doel with null; ref/clingo preserve it (leeg valt door). Original toolchain agrees with our port: DRL-oracle AND exec-model both write null (prijs:=basistarief[leeg] over prijs=40 -> null). v1-delta candidate: a leeg-source assignment is a no-op."
  "anchor/jutterswet-waardering-v0vorm|v0-EXEC-MODEL NON-TERMINATION, not a semantic divergence (PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q11, ratified 2026-08-29): the leeg-source gelijkstelling (writes null) oscillates against the INITIALISATIE (refills 25), so the exec-model back-end never reaches a fixpoint (fireAllRules hits its 100k cap) and returns the wrong value at fire. Unlike the other entries here this is NOT a genuine v0 production-semantics difference: the original toolchain's DRL-oracle DOES settle at 25 (= ref = clingo) — only the exec-model's reactivity of a leeg-source assignment diverges. It stays in this value-divergence list solely because the gate classifies a non-terminating fire as a wrong-value FAIL rather than an auto-exclusion; reclassifying it out of this list waits for a CaseRunner-taxonomy revisit (per the ratified decision), not a spec change."
  "anchor/jutterswet-kustwacht|v0-divergence (CORRECTED: NOT a leeg operand): NEGATION-as-failure is not stratified — a geen-<kenmerk> rule fires before the positive kenmerk is derived and is never retracted, so rustwacht reads present though its post IS a risicopost. Original toolchain agrees with our port: DRL-oracle AND exec-model both fire the negated kenmerk true. v1-delta candidate: stratify negated-kenmerk reads."
  # jutterswet-keurmerk LEFT the list 2026-08-29 (card triage-sluiting, PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q11): the
  # embedded-double-quote divergence was a backends/drools string-encoding defect, not a v0 semantic
  # difference. ExecModelTranspiler.tekstOfEnumLabel now strips only the paired Tekst DELIMITERS (as
  # Literalen.enumLabel does for single quotes) instead of every quote, so an embedded quote survives
  # the equality AND the computed-text write — ref ≡ clingo ≡ Drools on the quote-bearing golden, which
  # now PLAYS+PASSES. Removing this entry is mandatory: a listed golden that plays trips the stale guard.
  # NB objectcreatie (§13.4.6) and wederkerig self-relations (§13.3.9.2) are NOT listed here: they do not
  # execute in the v0 backend at all (a navigated created-object read yields leeg / an intValue() NPE; a self-pair
  # role has no derived bean field), so they are LOUD PER-FORM auto-exclusions in CaseRunner, not value-divergences.
  # A crash-at-fire is a not-carried form, never a "genuine exec-model semantic difference" — see the ERROR arm below.
  #
  # The three goldens the temporal case-input port surfaced 2026-08-30 (jutterswet-borgdeel,
  # synth2-prorata-breed, jutterswet-dagentelling) LEFT this list 2026-08-30 (card drools-fixpunt-en-ch78).
  # They were never the class the banner above describes — neither carried a v0 production-semantics
  # divergence — and both underlying defects were BUILT, so all three PLAY+PASS and removing them is
  # mandatory (a listed golden that plays trips the stale guard):
  #   (c) TERMINATION: ExecModelTranspiler's rsTijdreeksGelijk idempotency guard was scoped to ANCHORED
  #       temporal writes, while the §9.9 freshness expr leaves EVERY temporal write whose value reads an
  #       own attribute class-reactive — so unanchored siblings on one subject re-activated each other past
  #       the 100k cap. The guard now covers every timeline write (werkBijTijdlijn), which is
  #       semantics-neutral: it skips a write only when the recomputed timeline is segment-for-segment equal.
  #   (d) The ch. 7.8 timeline arm (settled spec: ref's Desugar.standing_temporal_targets + eval.ml's
  #       pro-rata/day-count arms, and clingo) is now implemented in the same helper — on a standing temporal
  #       target an empty fold result writes nothing and the case-supplied input timeline stands whole.
  # jutterswet-waardering-v0vorm above is a DIFFERENT oscillation (a scalar leeg-source gelijkstelling
  # against an INITIALISATIE) and stays listed, unchanged and witness-pinned.
)

known_reason() { local n="$1"; for e in "${EXCLUDE_KNOWN[@]}"; do [ "${e%%|*}" = "$n" ] && { echo "${e#*|}"; return 0; }; done; return 1; }

declare -a KNOWN_HIT=()
played=0; auto_excl=0; div_excl=0; nocase=0
declare -a AUTO_REASONS=()
declare -a FAILED=()

while IFS= read -r ir; do
  dir=$(dirname "$ir"); name=${dir#corpus/golden/}
  cases=$(find "$dir" -maxdepth 1 -name 'case*.json' | sort)
  if [ -z "$cases" ]; then
    nocase=$((nocase + 1)); echo "  no-case  $name (a golden without a case file — not playable)"; continue
  fi
  out=$(java -jar "$JAR" "$ir" $cases 2>/dev/null | tail -1)
  tok=${out%% *}
  case "$tok" in
    PLAYED)
      if reason=$(known_reason "$name"); then
        fail "stale exclusion: '$name' is in EXCLUDE_KNOWN ($reason) but now PLAYS+PASSES — remove it"
      fi
      played=$((played + 1)) ;;
    EXCLUDE)
      if known_reason "$name" >/dev/null; then
        fail "double-listed: '$name' auto-excludes per form AND sits in EXCLUDE_KNOWN — drop the static entry"
      fi
      auto_excl=$((auto_excl + 1)); AUTO_REASONS+=("${out#EXCLUDE }") ;;
    FAIL)
      # A value-divergence is a VALUE difference (FAIL: verwacht X, werkelijk Y) — the exec-model genuinely
      # differs from ref/clingo. It must be listed with a reason; an unlisted FAIL breaks the square.
      if reason=$(known_reason "$name"); then
        div_excl=$((div_excl + 1)); KNOWN_HIT+=("$name")
      else
        FAILED+=("$name :: $out")
      fi ;;
    ERROR)
      # A crash at fire is NOT a value-divergence — it is a form the backend does not execute, which belongs in
      # the per-form auto-exclusion channel (a CaseRunner Excluded), never excused as a semantic ref/clingo delta.
      # So an ERROR always fails the gate, whether or not the golden is listed: it must be reclassified, not listed.
      FAILED+=("$name :: $out  (a fault at fire is not a value-divergence — make it a per-form auto-exclusion)") ;;
    *) FAILED+=("$name :: unexpected output: $out") ;;
  esac
done < <(find corpus/golden -name ir.json | sort)

# unclassified divergences fail the gate — a golden must play+pass or carry a reason
if [ ${#FAILED[@]} -gt 0 ]; then
  echo "check-drools: FAIL — ${#FAILED[@]} golden(s) neither play nor carry an exclusion reason:" >&2
  for f in "${FAILED[@]}"; do echo "    $f" >&2; done
  exit 1
fi

# stale guard: every listed exclusion must have been hit as a divergence
for e in "${EXCLUDE_KNOWN[@]}"; do
  n="${e%%|*}"; hit=0
  for h in "${KNOWN_HIT[@]:-}"; do [ "$h" = "$n" ] && hit=1; done
  [ $hit -eq 1 ] || fail "stale exclusion: '$n' is listed but did not diverge (gone, or now carried) — remove it"
done

[ "$played" -gt 0 ] || fail "empty played set — no golden replayed through Drools (an empty played set is a failure)"

echo ""
echo "check-drools: PASS"
echo "  played $played  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)"
echo "  auto-excluded $auto_excl  (per-form refusals: loader / transpiler marker / contract / unsupported input)"
echo "  value-divergence $div_excl  (listed with reason — the exec-model differs from ref/clingo)"
echo "  no-case $nocase  (goldens without a case file)"
echo "  JUnit $tests tests  (the backend's own suite, green with a non-zero count)"
