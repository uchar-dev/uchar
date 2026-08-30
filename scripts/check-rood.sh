#!/bin/sh
# Gate 8: the red corpus — expected-fault cases (corpus/rood). Each
# directory holds a verifier-clean ir.json whose case*.json declares
# `expectedFault {ref, clingo}`: evaluation MUST fail observably in
# BOTH executors, with the pinned fragment in each report (the runners
# verify the fragment themselves and exit 0 only on the expected
# fault). This is the standing regression fence for red paths — the
# WF-044 unsolved state among them: Onopgelost surfaces, never
# vanishes.
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

# --- the Drools leg (decision V3-b, 2026-08-27) -----------------------------------------------
# The Drools backend CONSUMES the uchar IR (loads ir.json off the wire). For each rood case it
# either observes a genuine runtime fault AT FIRE — the FAULT this leg pins, its
# `expectedFault.drools` fragment in the report — or it does not carry the red-path form (an
# EXCLUDE from the loader/transpiler, or a NOFAULT where the backend does not enforce the
# constraint and runs to a model). A carried form pins a fragment; a not-carried form is listed
# below with a reason. The exclusion list is stale-guarded: a listed case that now FAULTS, or is
# gone, fails the gate; a case that is neither pinned nor listed fails the gate (completeness).
#
# Conditional on a JDK + python3 + the shaded jar, the gate-9 (catala) pattern: run in FULL where
# they exist (every failure inside is hard), skip LOUDLY where they do not — a warning, never a
# silent green, never a blocker. .github/ is a protected path this card may not touch, and the
# hosted gate-8 runner carries no JDK; so the leg is the reviewer's pre-merge duty on a machine
# that has java. The ref/clingo legs above remain the CI blocker.
JAR=$(ls backends/drools/target/uchar-drools-backend-*-shaded.jar 2>/dev/null | head -1)
if command -v java >/dev/null 2>&1 && command -v python3 >/dev/null 2>&1 \
   && { [ -n "$JAR" ] || command -v mvn >/dev/null 2>&1; }; then
    if [ -z "$JAR" ]; then
        echo 'check-rood: Drools leg — shaded jar absent, building it once (offline)…' >&2
        mvn -o -q -f backends/drools/pom.xml package >/dev/null 2>&1
        JAR=$(ls backends/drools/target/uchar-drools-backend-*-shaded.jar 2>/dev/null | head -1)
    fi
    if [ -z "$JAR" ]; then
        echo 'check-rood: FAIL — Drools leg: java present but no shaded jar (build it: mvn -o -f backends/drools/pom.xml package)' >&2
        status=1
    else
        MAIN=io.github.yhavinga.regelspraak.cases.DroolsRoodMain
        excl=$(mktemp) || exit 2
        # <rood-case-dir>|<reason the v0 Drools backend does not fault-pin this red form>
        cat > "$excl" <<'EOF'
ambiguous-backlink|EXCLUDE objectcreatie (§13.4.6) refused up front — the creation consequence does not execute in the v0 backend, so the ambiguous back-link is never reached
ambiguous-fold-role-name|NOFAULT the v0 backend resolves the fold role without detecting the cross-type ambiguity ref/clingo reject; it runs to a model
closed-nonexact-division|EXCLUDE the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
closed-runtime-division-by-zero|EXCLUDE the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
closed-runtime-nonexact-division|EXCLUDE the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
decimal-nonterminating|NOFAULT the v0 backend has no §6.1.2 write-precision guard — a non-terminating quotient collapses to DECIMAL64 (Breuk.naarDecimal) instead of faulting; it runs to a model
division-by-numeric-zero|EXCLUDE the reaching rule (vindersloon van een vondst) does not lower — a transpiler SKIP (waarde buiten scope); fire is never reached
finite-chain-over-budget|EXCLUDE objectcreatie (§13.4.6) refused up front — the generative chain does not execute in the v0 backend
input-overprecision|NOFAULT the v0 case runner does not enforce declared attribute input precision — it accepts the over-precise value and runs to a model
input-overprecision-param|NOFAULT the v0 case runner does not enforce declared parameter input precision — it accepts the over-precise value and runs to a model
list-element-overprecision|EXCLUDE the v0 loader refuses the model's record datatype (StructType) before any list-element precision is reached — the record/struct form is not carried
near-side-rolcheck-to-many|EXCLUDE the rolcheck rule (bemand met jutter) does not lower — a transpiler SKIP (voorwaarde buiten Phase-B scope); fire is never reached
runaway-recursion|EXCLUDE objectcreatie (§13.4.6) refused up front — the recursive generator does not execute in the v0 backend
struct-field-overprecision|EXCLUDE the v0 loader refuses the model's record datatype (StructType) — the record/struct form is not carried
to-one-second-filler|EXCLUDE the case's role (aangemelde vondsten) has no uniquely derivable reference field in the v0 bean model — the role input is refused before the second-filler violation is reached
uni-temporal-key-scalar|EXCLUDE the case sets a temporal attribute (kavelnummer) as a scalar while carrying NO asOfDate — the bucket convention (corpus/golden/README.md) gives that scalar no evaluation date to sit in, so the input is not carried and the ever-unique key is never reached
uni-two-temporal-keys|NOFAULT the v0 backend enforces no ARITY restriction on temporal key components — ExecModelTranspiler.uniciteit lowers an rsOoitBotst ever-collision predicate per component, where ref/clingo reject the key SHAPE itself; the case's single instance collides with nothing, so it runs to a model
uu-temporal-key-scalar|EXCLUDE the case sets a temporal attribute (vondstnummer) as a scalar while carrying NO asOfDate — the bucket convention (corpus/golden/README.md) gives that scalar no evaluation date to sit in, so the input is not carried and the ever-unique key is never reached
uu-two-temporal-keys|NOFAULT the v0 backend enforces no ARITY restriction on temporal key components — ExecModelTranspiler.uniciteitUnie lowers an rsOoitBotst ever-collision predicate per component, where ref/clingo reject the key SHAPE itself; the union's single instance collides with nothing, so it runs to a model
vintage-uncovered-date|EXCLUDE the v0 loader refuses the TemporalRead expression node (the vintage read) — the temporal-read form is not carried
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
    echo 'check-rood: Drools leg SKIPPED — needs java + python3 + (the shaded jar or mvn). Not a blocker (the hosted gate-8 runner carries no JDK; the leg is the reviewer pre-merge duty). The ref/clingo legs above are the CI blocker.' >&2
fi

if [ "$status" -eq 0 ]; then
    printf 'check-rood: PASS (%s red cases fault observably in ref and clingo)\n' "$n_cases"
fi
exit "$status"
