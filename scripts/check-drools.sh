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
# Runs JAR-ONLY (decision V2-B, the gate-10 pattern): the shaded runtime uber-jar is built once
#   mvn -o -f backends/drools/pom.xml package        # -> target/uchar-drools-backend-*-shaded.jar
# and the gate invokes `java -jar` per golden. Absent the jar the gate builds it once (offline).
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-drools: FAIL — $*" >&2; exit 1; }

command -v java >/dev/null 2>&1 || fail "java not found (the Drools runtime needs a JDK/JRE 21+)"

JAR=$(ls backends/drools/target/uchar-drools-backend-*-shaded.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
  command -v mvn >/dev/null 2>&1 || fail "no shaded jar and mvn not found — build it: mvn -o -f backends/drools/pom.xml package"
  echo "check-drools: shaded jar absent — building it once (offline)…" >&2
  mvn -o -q -f backends/drools/pom.xml package >/dev/null 2>&1 || fail "the shaded-jar build is red — run: mvn -o -f backends/drools/pom.xml package"
  JAR=$(ls backends/drools/target/uchar-drools-backend-*-shaded.jar 2>/dev/null | head -1)
  [ -n "$JAR" ] || fail "the build produced no shaded jar"
fi

# --- the value-divergence exclusion list (reason-carrying; fails when stale) -------------------
# Each entry: "<golden-relative-path>|<reason>". A golden here MUST diverge (FAIL/ERROR); if it
# plays+passes the list is stale. Grouped by the divergence class the differential exposed.
EXCLUDE_KNOWN=(
  # leeg-semantics: the exec-model writes/asserts a value where ref/clingo pin the uchar leeg
  # rule (leeg-valt-door: a gelijkstelling with a leeg source overwrites the target rather than
  # preserving it — traced to the unconditional werkBij write; leeg-in-condition: a comparison/
  # kenmerk over a leeg operand reads true rather than dropping the rule).
  "anchor/jutterswet-waardestelling|leeg-valt-door: exec-model overwrites a leeg-sourced gelijkstelling; ref/clingo preserve the input"
  "anchor/jutterswet-waardering-v0vorm|leeg-doorval: the leeg fall-through recompute does not reach a fixpoint in the exec-model"
  "anchor/jutterswet-kustwacht|leeg-in-condition: a kenmerk over a leeg operand reads present where ref/clingo pin absent"
  "jutterswet-borgvlag|leeg-in-condition: a kenmerk over a leeg operand reads present where ref/clingo pin absent"
  "jutterswet-vondstetiket|leeg-in-condition: a kenmerk over a leeg operand reads present where ref/clingo pin absent"
  "jutterswet-gelijkdeling|leeg in a verdeling restant: exec-model writes a value where ref/clingo pin leeg"
  "jutterswet-voorwaardedeling|leeg under a conditional verdeling: exec-model writes a value where ref/clingo pin leeg"
  # NB objectcreatie (§13.4.6) and wederkerig self-relations (§13.3.9.2) are NOT listed here: they do not
  # execute in the v0 backend at all (a navigated created-object read yields leeg / an intValue() NPE; a self-pair
  # role has no derived bean field), so they are LOUD PER-FORM auto-exclusions in CaseRunner, not value-divergences.
  # A crash-at-fire is a not-carried form, never a "genuine exec-model semantic difference" — see the ERROR arm below.
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
