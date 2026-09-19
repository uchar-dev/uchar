#!/bin/sh
# Gate 8: the red corpus — expected-fault cases (corpus/rood). Each
# directory holds a verifier-clean ir.json whose case*.json declares
# `expectedFault {ref, clingo}`: LOADING OR evaluation MUST fail
# observably in BOTH executors, with the pinned fragment in each report
# (the runners verify the fragment themselves and exit 0 only on the
# expected fault). Both phases count, since 2026-09-19: the runners read
# expectedFault on its own, BEFORE the world, so a refusal by the case
# reader — an ambiguous role key, a link to an undeclared label — is
# pinnable exactly like a fault at evaluation. Read the other way round
# that whole class could only crash on exit 2, and no zone held it.
# This is the standing regression fence for red paths — the WF-044
# unsolved state among them: Onopgelost surfaces, never vanishes.
#
#   1. ref/ and backends/ build; clingo must be available;
#   2. every corpus/rood/**/case*.json passes both runners in
#      expected-fault mode (exit 0);
#   3. an empty red set is a failure, never silently green.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

if ! command -v dune >/dev/null 2>&1; then
    if command -v opam >/dev/null 2>&1; then
        eval "$(opam env)" || exit 2
    fi
fi
if ! command -v dune >/dev/null 2>&1; then
    echo 'FAIL: need dune (OCaml). Install via opam: opam install dune yojson zarith' >&2
    exit 2
fi
if ! command -v clingo >/dev/null 2>&1; then
    echo 'FAIL: need clingo on PATH (brew install clingo / apt install gringo)' >&2
    exit 2
fi

if ! dune build ref backends 2>&1; then
    echo 'check-rood: FAIL (build)' >&2
    exit 1
fi
REF=_build/default/ref/bin/runner.exe
CLINGO=_build/default/backends/clingo/bin/clingo_runner.exe

# --- monotone floor (maintainer decision 2026-09-09) ---------------------------------------------
# A lower bound, never an equality: it catches SHRINK — the red fence that quietly halves — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
# This is the red zone's own floor; gate 5 carries the green zone's. Gate 4 sees the sum of the two
# and therefore floors neither: a golden case traded for a red one keeps a sum equal.
# NO second floor on the Drools leg's fault count: its not-carried cases are each pinned by name in
# the reason-carrying exclusion list below, so the leg's fault total is the case count minus a
# name-pinned set, both of which are already held.
EXPECTED_MIN_ROOD_CASES=26   # 2026-09-10: measured 26 (corpus/rood/**/case*.json)

status=0
n_cases=0
list=$(mktemp) || exit 2
trap 'rm -f "$list"' EXIT
find corpus/rood -name 'case*.json' 2>/dev/null | sort > "$list"
while IFS= read -r case; do
    n_cases=$((n_cases + 1))
    ir=$(dirname "$case")/ir.json
    if [ ! -f "$ir" ]; then
        echo "FAIL: $case has no sibling ir.json" >&2
        status=1
        continue
    fi
    if ! out=$("$REF" "$ir" "$case" 2>&1); then
        printf 'FAIL: %s — ref did not observe the expected fault:\n%s\n' "$case" "$out" >&2
        status=1
    fi
    if ! out=$("$CLINGO" "$ir" "$case" 2>&1); then
        printf 'FAIL: %s — clingo did not observe the expected fault:\n%s\n' "$case" "$out" >&2
        status=1
    fi
done < "$list"

if [ "$n_cases" -eq 0 ]; then
    echo 'FAIL: no case files under corpus/rood — the red fence is empty, never silently green' >&2
    exit 1
fi
if [ "$n_cases" -lt "$EXPECTED_MIN_ROOD_CASES" ]; then
    echo "FAIL: $n_cases red case(s) replayed, but EXPECTED_MIN_ROOD_CASES pins at least $EXPECTED_MIN_ROOD_CASES — the red fence shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi

# --- the Drools leg (decision V3-b, 2026-08-27) -----------------------------------------------
# The Drools backend CONSUMES the uchar IR (loads ir.json off the wire). For each rood case it
# either observes a genuine runtime fault AT FIRE — the FAULT this leg pins, its
# `expectedFault.drools` fragment in the report — or it does not carry the red-path form (an
# EXCLUDE from the loader/transpiler, or a NOFAULT where the backend does not enforce the
# constraint and runs to a model). A carried form pins a fragment; a not-carried form is listed
# below with a reason. The exclusion list is stale-guarded: a listed case that now FAULTS, or is
# gone, fails the gate; a case that is neither pinned nor listed fails the gate (completeness).
#
# Conditional on a JDK + python3 (gate-9 pattern for the RUNNER: skip loudly where java is
# absent; the ref/clingo legs above remain the CI blocker). WITHIN the leg, mvn is a hard
# requirement since 2026-08-30 (the stale-jar guard): a missing tool there is a FAILURE, no
# longer a skip — a machine with java but no mvn cannot vouch for jar freshness. NB the hosted
# ubuntu runner DOES carry a JDK and Maven (lens finding 2026-08-30 — the earlier "carries no
# JDK" premise was untrue), so on hosted CI this leg RUNS and needs a warm ~/.m2; the gate-8
# workflow job provides none today, so hosted gate 8 goes red the day CI revives until that job
# gains a maven cache/build step (a .github card, out of this card's reach). Not hidden: red,
# never a silent skip.
if command -v java >/dev/null 2>&1 && command -v python3 >/dev/null 2>&1; then
    # When the leg runs, the jar is rebuilt UNCONDITIONALLY (the gate-12 model, 2026-08-30): a
    # build-only-when-absent jar can silently predate the source under review — the lens-proven
    # stale-jar flank. legfail is leg-LOCAL so the leg always leaves a verdict of its own,
    # regardless of earlier red-case failures elsewhere in the gate (lens finding: keying on the
    # global status let the leg evaporate without one).
    legfail=0
    if ! command -v mvn >/dev/null 2>&1; then
        echo 'check-rood: FAIL — Drools leg: java present but mvn missing; the leg rebuilds the jar unconditionally (stale-jar guard)' >&2
        status=1; legfail=1
        JAR=''
    else
        echo 'check-rood: Drools leg — rebuilding the shaded jar (unconditional, offline)…' >&2
        build_out=$(mvn -o -f backends/drools/pom.xml package 2>&1)
        if [ $? -ne 0 ]; then
            printf '%s\n' "$build_out" | tail -25 >&2
            echo 'check-rood: FAIL — Drools leg: the shaded-jar build is red — run: mvn -o -f backends/drools/pom.xml package' >&2
            status=1; legfail=1
            JAR=''
        else
            # exactly ONE shaded jar, or the measurement is ambiguous: ls|head-1 picks the
            # lexicographic first, which after an artifact-version bump is the STALE one (lens).
            JAR=$(ls backends/drools/target/uchar-drools-backend-*-shaded.jar 2>/dev/null)
            n=$(printf '%s\n' "$JAR" | grep -c .)
            if [ "$n" -eq 0 ]; then
                echo 'check-rood: FAIL — Drools leg: the build produced no shaded jar' >&2
                status=1; legfail=1
                JAR=''
            elif [ "$n" -gt 1 ]; then
                echo "check-rood: FAIL — Drools leg: expected exactly one shaded jar, found: $JAR (a leftover jar from an earlier artifact version would be measured instead of the fresh build — clean backends/drools/target)" >&2
                status=1; legfail=1
                JAR=''
            fi
        fi
    fi
    if [ -z "$JAR" ]; then
        # every empty-JAR path above set legfail and printed its own verdict; this arm only guards
        # against a future edit that forgets to — belt and braces, never the primary reporter
        [ $legfail -eq 1 ] || { echo 'check-rood: FAIL — Drools leg: no shaded jar and no verdict printed (script defect)' >&2; status=1; }
    else
        MAIN=io.github.yhavinga.regelspraak.cases.DroolsRoodMain
        excl=$(mktemp) || exit 2
        # <rood-case-dir>|<reason the v0 Drools backend does not fault-pin this red form>
        cat > "$excl" <<'EOF'
ambiguous-case-role-key|EXCLUDE measured 2026-09-19: the transpiler SKIPs the document's only rule (ligdiepte van een schuit — a navigated read, waarde buiten scope), so the backend never builds the fact model the ambiguous case key would be read into; the case input is never reached. A LOAD-fault witness: ref and clingo both refuse while READING the case, and this leg's vocabulary reserves FAULT for a genuine fault at fire, so a loader/transpiler refusal is an EXCLUDE even where the backend would agree
ambiguous-fold-role-name|NOFAULT the v0 backend resolves the fold role without detecting the cross-type ambiguity ref/clingo reject; it runs to a model
begrensd-reads-bounds-in-order|NOFAULT the v0 backend evaluates a Begrensd's MAXIMUM bound BEFORE its MINIMUM — the reverse of the ch. 5.11 wire order — so the ch. 10.6 barrier sitting in the maximum fires before the minimum's division is ever reached, and the fault never happens; it runs to a model. Measured both ways on this exact shape: with the division in the MAXIMUM instead, Drools DOES fault ("Breuk deling door 0") on the very document where ref and clingo do not. This is the operand-order divergence the witness exists to expose; the Drools-side correction is a card of its own
closed-nonexact-division|EXCLUDE the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
closed-runtime-division-by-zero|EXCLUDE the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
closed-runtime-nonexact-division|EXCLUDE the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
decimal-nonterminating|NOFAULT the v0 backend has no §6.1.2 write-precision guard — a non-terminating quotient collapses to DECIMAL64 (Breuk.naarDecimal) instead of faulting; it runs to a model
division-by-numeric-zero|EXCLUDE the reaching rule (vindersloon van een vondst) does not lower — a transpiler SKIP (waarde buiten scope); fire is never reached
finite-chain-over-budget|NOFAULT the self-typed generative chain now RUNS (2026-08-30: the creation's symbolic verbindendeRol binding rides beside the model, so the back-link resolves and both ends are written) — and it terminates: the chain is FINITE at rank < 105, so 104 generations are minted and the session reaches a fixpoint well inside the 100k fire cap. The v0 backend has no ch. 5.5 GENERATION budget, the boundary this case exists to pin; ref exhausts its pass budget and clingo its generation depth where Drools simply finishes. Its runaway twin (runaway-recursion) has no such end and IS fault-pinned, so the cap itself is fenced
input-overprecision|NOFAULT the v0 case runner does not enforce declared attribute input precision — it accepts the over-precise value and runs to a model
input-overprecision-param|NOFAULT the v0 case runner does not enforce declared parameter input precision — it accepts the over-precise value and runs to a model
list-element-overprecision|EXCLUDE the v0 loader refuses the model's record datatype (StructType) before any list-element precision is reached — the record/struct form is not carried
near-side-rolcheck-to-many|EXCLUDE the rolcheck rule (bemand met jutter) does not lower — a transpiler SKIP (voorwaarde buiten Phase-B scope); fire is never reached
som-fallback-always-evaluated|EXCLUDE the reaching rule (vangstsom van een vangst) does not lower — a transpiler SKIP (aggregatie buiten Phase-C scope); fire is never reached, so the som's stand-in is not Drools-observable here
struct-field-overprecision|EXCLUDE the v0 loader refuses the model's record datatype (StructType) — the record/struct form is not carried
tijdsduur-reads-van-before-tot|NOFAULT the v0 backend enforces no van/tot ORDER on a TIJDSDUUR, so the ch. 10.6 barrier this witness plants never becomes a fault; it runs to a model — the mirror of begrensd-reads-bounds-in-order above, the same ch. 5.11 divergence. Reason category CORRECTED 2026-09-09 (card onopgelost-zijkanaal-opruiming), measured on both backends: the verdict was EXCLUDE while a Let was an Onopgelost carrier and the Rekendatum inside its body stayed a leaf to Expr.bevat, so the rule did not lower at all; with Let a real union node it lowers and fires. Both pinned legs are untouched — the case still pins expectedFault {ref, clingo}. Full account in the card and in the case's NOTE.md
to-one-second-filler|EXCLUDE the case's role (aangemelde vondsten) has no uniquely derivable reference field in the v0 bean model — the role input is refused before the second-filler violation is reached
uni-temporal-key-scalar|EXCLUDE the case sets a temporal attribute (kavelnummer) as a scalar while carrying NO asOfDate — the bucket convention (corpus/golden/README.md) gives that scalar no evaluation date to sit in, so the input is not carried and the ever-unique key is never reached
unknown-role-target-label|EXCLUDE measured 2026-09-19: the backend's own loader refuses the same input and names the same dangling label (role 'liggers' names unknown label S9), but a loader refusal is an EXCLUDE in this leg's vocabulary, where FAULT means a genuine fault at fire. The agreement is real and recorded here rather than pinned: all three executors reject this case, and only the two that can carry a fragment do
uu-temporal-key-scalar|EXCLUDE the case sets a temporal attribute (vondstnummer) as a scalar while carrying NO asOfDate — the bucket convention (corpus/golden/README.md) gives that scalar no evaluation date to sit in, so the input is not carried and the ever-unique key is never reached
vintage-uncovered-date|EXCLUDE the v0 loader refuses the TemporalRead expression node (the vintage read) — the temporal-read form is not carried
wf044-onopgelost-guard|EXCLUDE measured 2026-09-19: BOTH transpiler branches refuse the §13.4.7 creation by construct — a guard that lowers to neither the anker's nor the doel's pattern cannot be dropped without firing the rule unconditionally and over-creating the relation (exec: "transpiler SKIP: feitcreatie … voorwaarde niet naar anker of doel te lowereren"; the DRL-text branch prints the same limit as a MARKER). The backend AGREES that the document is unreadable, one phase before fire; this leg's vocabulary reserves FAULT for a genuine fault at fire, so the agreement is listed here instead of fragment-pinned — exactly as its value-position sibling below is
wf044-onopgelost-reached|EXCLUDE the reaching rule (waardering van een vondst) does not lower — a transpiler SKIP (waarde buiten scope); the Onopgelost marker never fires, so it is not Drools-carried (the WF-044 guard: listed, not fragment-pinned)
EOF
        d_fault=0
        d_excl=0
        while IFS= read -r case; do
            name=$(basename "$(dirname "$case")")
            ir=$(dirname "$case")/ir.json
            frag=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get("expectedFault",{}).get("drools",""))' "$case")
            reason=$(awk -F'|' -v n="$name" '$1==n {i=index($0,"|"); print substr($0,i+1); exit}' "$excl")
            # DroolsRoodMain prints exactly one status line (FAULT|NOFAULT|EXCLUDE|ERROR <detail>) last on
            # stdout; the leg classifies on that STATUS TOKEN, never the bare exit code, so an ERROR (a backend
            # crash) can never be swallowed as a not-carried outcome and a listed reason's category is checked.
            out=$(java -cp "$JAR" "$MAIN" "$ir" "$case" 2>/dev/null | tail -1)
            tok=${out%% *}
            if [ -n "$frag" ]; then
                if [ -n "$reason" ]; then
                    echo "FAIL: $name carries a drools fragment AND sits in the Drools exclusion list — drop the static entry" >&2
                    status=1
                fi
                # a carried case MUST be a genuine FAULT at fire whose report contains the pinned fragment; any
                # other token (NOFAULT / EXCLUDE / a crash ERROR) is a regression, never a pass.
                case "$tok" in
                    FAULT)
                        case "$out" in
                            *"$frag"*) d_fault=$((d_fault + 1)) ;;
                            *) printf 'FAIL: %s — Drools faulted but the pinned fragment is absent (drools="%s"):\n%s\n' "$name" "$frag" "$out" >&2
                               status=1 ;;
                        esac ;;
                    *) printf 'FAIL: %s — Drools did not FAULT (pinned drools="%s"), observed %s:\n%s\n' "$name" "$frag" "$tok" "$out" >&2
                       status=1 ;;
                esac
            else
                if [ -z "$reason" ]; then
                    printf 'FAIL: %s carries no drools fragment and is not in the Drools exclusion list — pin a fragment or list it with a reason:\n%s\n' "$name" "$out" >&2
                    status=1
                else
                    # the listed reason's first word is its declared not-carried category; it is machine-checked
                    # against the observed token, so a reason that drifts (EXCLUDE<->NOFAULT) or a case that now
                    # FAULTs or ERRORs fails the gate — the stale-guard is no longer exit-code-blind.
                    rcat=${reason%% *}
                    case "$tok" in
                        FAULT)
                            printf 'FAIL: stale exclusion — %s is listed (%s) but Drools now FAULTS; drop the entry and pin its fragment:\n%s\n' "$name" "$reason" "$out" >&2
                            status=1 ;;
                        ERROR)
                            printf 'FAIL: %s is listed (%s) but Drools ERRORED — a backend crash is never a not-carried outcome; fix the crash or reclassify:\n%s\n' "$name" "$reason" "$out" >&2
                            status=1 ;;
                        EXCLUDE|NOFAULT)
                            if [ "$tok" != "$rcat" ]; then
                                printf 'FAIL: reason drift — %s is listed as %s but Drools now reports %s; correct the reason category to match the observed status:\n%s\n' "$name" "$rcat" "$tok" "$out" >&2
                                status=1
                            else
                                d_excl=$((d_excl + 1))
                            fi ;;
                        *)
                            printf 'FAIL: %s — unexpected Drools status token "%s" (expected FAULT/EXCLUDE/NOFAULT/ERROR):\n%s\n' "$name" "$tok" "$out" >&2
                            status=1 ;;
                    esac
                fi
            fi
        done < "$list"
        # gone-guard: every exclusion entry must name an existing rood case (a double-list is caught above)
        while IFS='|' read -r n _rest; do
            [ -z "$n" ] && continue
            if [ -z "$(find "corpus/rood/$n" -maxdepth 1 -name 'case*.json' 2>/dev/null | head -1)" ]; then
                echo "FAIL: stale exclusion — '$n' is listed but no such rood case exists" >&2
                status=1
            fi
        done < "$excl"
        rm -f "$excl"
        # at-least-one-fault discipline: the leg must anchor on a genuine backend fault (the smoke-test
        # canary of a working jar) — an all-not-carried run is never silently green.
        if [ "$d_fault" -eq 0 ]; then
            echo 'check-rood: FAIL — Drools leg: no rood case fault-pinned; at least one genuine backend fault must anchor the leg (an empty fault set is a failure, never silently green)' >&2
            status=1
        fi
        if [ "$status" -eq 0 ]; then
            printf 'check-rood: Drools leg PASS (%s fault-pinned, %s not-carried with reason)\n' "$d_fault" "$d_excl"
        fi
    fi
else
    echo 'check-rood: Drools leg SKIPPED — needs java + python3 (and, once running, mvn: the leg rebuilds the jar unconditionally). Not a blocker on a machine without java; the ref/clingo legs above are the CI blocker. NB hosted ubuntu runners DO carry java, so there the leg runs (and needs a warm ~/.m2).' >&2
fi

if [ "$status" -eq 0 ]; then
    printf 'check-rood: PASS (%s red cases fault observably in ref and clingo, floor %s)\n' \
        "$n_cases" "$EXPECTED_MIN_ROOD_CASES"
fi
exit "$status"
