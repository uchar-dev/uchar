#!/bin/sh
# Gate 5: the oracle — the reference interpreter (ref/) replays every
# corpus case (PLAN section 8: "an interpreter replay of each corpus case
# must pass").
#
# Four checks:
#   1. ref/ builds (the OCaml workspace, scripts/lib/ocaml-build.sh, which
#      also builds the ir_v0 loader it links against);
#   2. ref/'s OWN test suite (ref/test/test_ref.ml) runs green with a
#      non-zero, floored example count — see below;
#   3. every corpus/golden/**/case*.json replays exactly: the runner loads
#      the sibling ir.json and the case file, evaluates to the fixpoint,
#      and compares every expectation exactly (exit 0); any mismatch or
#      load error turns the gate red with the runner's report;
#   4. goldens that have no case file yet are listed explicitly — they are
#      not replayed and must never look green. An empty case set is a
#      failure, never silently green;
#   5. preservation with complete knowledge (RUNTIME-SEMANTICS ch. 1.5, "The
#      two requirements"): every golden whose document does not carry
#      inferFromEmpty, with the property added in a copy and every input
#      member complete (`runner --closed-inputs`), plays its own cases — see
#      below.
#
# With explicit arguments (ir.json case.json) it replays just that pair.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

if [ ! -d ref ]; then
    echo 'FAIL: ref/ not found' >&2
    exit 2
fi

# Locate dune; fall back to the opam environment when it is not on PATH.
if ! command -v dune >/dev/null 2>&1; then
    if command -v opam >/dev/null 2>&1; then
        eval "$(opam env)" || exit 2
    fi
fi
if ! command -v dune >/dev/null 2>&1; then
    echo 'FAIL: need dune (OCaml). Install via opam: opam install dune yojson zarith' >&2
    exit 2
fi

# check 1: build (the OCaml workspace: ref/ plus the ir_v0 library).
. scripts/lib/ocaml-build.sh
if ! ensure_ocaml; then
    echo 'check-oracle: FAIL (build)' >&2
    exit 1
fi
BIN=_build/default/ref/bin/runner.exe

# --- monotone floors (maintainer decision 2026-09-09) --------------------------------------------
# Lower bounds, never equalities: they catch SHRINK — the suite that quietly runs half — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
EXPECTED_MIN_EXAMPLES=489   # 2026-09-10: measured 489 (ref/test/test_ref.ml worked examples)
EXPECTED_MIN_CASES=427      # 2026-09-10: measured 427 (corpus/golden/**/case*.json)
EXPECTED_MIN_PRESERVED_GOLDENS=134  # 2026-11-21: measured 134 (check 5: goldens whose forms ref carries strictly)
EXPECTED_MIN_PRESERVED_CASES=339    # 2026-11-21: measured 339 (check 5: their case files)

# check 5's own red row: a golden on which the copy with the property and WITHOUT --closed-inputs gives another
# outcome than the case expects. Measured 2026-11-21: jutterswet-leeg-deeltal case1-leeg-deeltal, an open bonus as the
# dividend, "V1.loon = leeg, expected 0". Where that copy plays the golden's every case, the leg could not tell the
# strict reading from the ordinary one, and the gate fails.
PRESERVATION_MUTANT=jutterswet-leeg-deeltal

if [ "$#" -eq 2 ]; then
    exec "$BIN" "$1" "$2"
fi

# check 2: ref/'s own test suite. Until 2026-09-10 NO gate ran it, while it carries the worked
# examples RUNTIME-SEMANTICS.md owes each of its clauses (CLAUDE.md, Spec discipline) — spec
# artifacts whose executor was unguarded. It is run here, in the gate that already owns ref/ and
# already builds it, exactly the way gate 6 runs the clingo backend's own suite: run it, demand a
# count line, floor the count. The workspace build above already produces the exe.
# It sits BELOW the two-argument branch on purpose: that form replays one pair for a person
# debugging it, and making that pay for the whole suite is a cost with no gate value.
SUITE_BIN=_build/default/ref/test/test_ref.exe
[ -x "$SUITE_BIN" ] || { echo "FAIL: ref suite binary missing at $SUITE_BIN after the build" >&2; exit 1; }
if ! SUITE_OUT=$("$SUITE_BIN" 2>&1); then
    echo "$SUITE_OUT" >&2
    echo 'check-oracle: FAIL (ref test suite)' >&2
    exit 1
fi
SUITE_N=$(printf '%s' "$SUITE_OUT" | sed -n 's/^test_ref: all \([0-9][0-9]*\) worked examples pass$/\1/p' | tail -1)
if [ -z "$SUITE_N" ]; then
    echo "$SUITE_OUT" >&2
    echo 'FAIL: the ref suite printed no "all N worked examples pass" line — it did not run (a silent green is not green)' >&2
    exit 1
fi
if [ "$SUITE_N" -lt "$EXPECTED_MIN_EXAMPLES" ]; then
    echo "FAIL: the ref suite ran $SUITE_N worked examples, but EXPECTED_MIN_EXAMPLES pins at least $EXPECTED_MIN_EXAMPLES — the suite shrank (a deliberately dropped example lowers the floor in the same commit)" >&2
    exit 1
fi

status=0
list=$(mktemp) || exit 2
trap 'rm -f "$list"' EXIT

# check 3: every case replays exactly.
n_cases=0
find corpus/golden -name 'case*.json' 2>/dev/null | sort > "$list"
while IFS= read -r case; do
    n_cases=$((n_cases + 1))
    ir=$(dirname "$case")/ir.json
    if [ ! -f "$ir" ]; then
        echo "FAIL: $case has no sibling ir.json" >&2
        status=1
        continue
    fi
    if ! out=$("$BIN" "$ir" "$case" 2>&1); then
        printf 'FAIL: %s does not replay:\n%s\n' "$case" "$out" >&2
        status=1
    fi
done < "$list"

if [ "$n_cases" -eq 0 ]; then
    echo 'FAIL: no case files under corpus/golden — the oracle has nothing to replay' >&2
    exit 1
fi
# The floor on the green zone's case files. Gate 4 sees the same files plus the red zone's, but its
# total is the sum of two zones, so a golden case traded for a red one keeps it equal — the floor
# per zone (here, and gate 8 for corpus/red) is strictly stronger and is where they live.
if [ "$n_cases" -lt "$EXPECTED_MIN_CASES" ]; then
    echo "FAIL: $n_cases golden case(s) replayed, but EXPECTED_MIN_CASES pins at least $EXPECTED_MIN_CASES — the green corpus shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi

# check 4: goldens without any case file are listed, never silently green.
# Every directory holding an ir.json, at any depth: the anchor goldens sit one level below the rest.
uncovered=''
for f in $(find corpus/golden -name ir.json | sort); do
    d=${f%ir.json}
    if ! ls "${d}"case*.json >/dev/null 2>&1; then
        uncovered="$uncovered ${d%/}"
    fi
done

# check 5: preservation with complete knowledge (RUNTIME-SEMANTICS ch. 1.5). Where nothing is open, the reading
# that infers nothing from an empty value is the reading of ch. 1.2-1.4: ref builds it so (one evaluator, the
# property read only where it decides which absent input is open), and this check holds it on the corpus. Per golden
# with case files whose document does not carry inferFromEmpty, a copy with `"inferFromEmpty": false` directly after
# `format` replays every case file of the golden with `runner --closed-inputs` (every input member complete), against
# the case's own expectations. A golden whose copy ref refuses (REF-014, a form whose strict reading it does not carry
# yet) is counted per form, the first the refusal names, and does not fail; every other case must play, and one that
# does not fails by name with the runner's report, and so does a golden whose cases partly play and partly refuse. A
# golden that carries the property already is not part of the check: its cases pin the strict reading, and check 3
# plays them. The red row: the copy of PRESERVATION_MUTANT without --closed-inputs must miss at least one of its cases.
# The copies and the replays run in a directory of their own, one runner per core (scripts/lib/parallel.sh), and are
# judged in corpus order.
. scripts/lib/parallel.sh
work=$(mktemp -d) || exit 2
trap 'rm -f "$list"; rm -rf "$work"' EXIT
if ! procs=$(parallel_jobs 1); then
    echo 'check-oracle: FAIL (no number of processes for the preservation replays; the parallel line above says why)' >&2
    exit 1
fi
find corpus/golden -name 'case*.json' 2>/dev/null | sort > "$list"
if ! python3 -I - "$list" "$work" "$PRESERVATION_MUTANT" <<'PYEOF'
import collections, json, os, sys

list_path, work, mutant = sys.argv[1:4]
by_golden = collections.OrderedDict()
for line in open(list_path, encoding="utf-8"):
    case = line.rstrip("\n")
    if case:
        by_golden.setdefault(os.path.dirname(case), []).append(case)
with open(os.path.join(work, "tasks.tsv"), "w", encoding="utf-8") as tasks, \
        open(os.path.join(work, "strict.txt"), "w", encoding="utf-8") as strict:
    for n, (golden, cases) in enumerate(by_golden.items(), 1):
        name = os.path.relpath(golden, "corpus/golden")
        with open(os.path.join(golden, "ir.json"), encoding="utf-8") as f:
            doc = json.load(f, object_pairs_hook=collections.OrderedDict)
        if "inferFromEmpty" in doc:
            strict.write(name + "\n")
            continue
        copy = collections.OrderedDict()
        for k, v in doc.items():
            copy[k] = v
            if k == "format":
                copy["inferFromEmpty"] = False
        if "inferFromEmpty" not in copy:
            sys.exit("check-oracle: FAIL — %s/ir.json has no format key to put inferFromEmpty after" % golden)
        os.makedirs(os.path.join(work, "docs", str(n)))
        path = os.path.join(work, "docs", str(n), "ir.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(copy, f, ensure_ascii=False)
        for m, case in enumerate(cases, 1):
            tasks.write("p%d.%d\tclosed\t%s\t%s\t%s\n" % (n, m, name, path, case))
            if name == mutant:
                tasks.write("m%d.%d\topen\t%s\t%s\t%s\n" % (n, m, name, path, case))
PYEOF
then
    echo 'check-oracle: FAIL (preservation: the strict copies could not be written)' >&2
    exit 1
fi
tab=$(printf '\t')
mkdir "$work/run" || exit 2
while IFS="$tab" read -r key mode _name copy case; do
    if [ "$mode" = closed ]; then
        parallel_task "$work/run" "$key" out "$BIN" --closed-inputs "$copy" "$case"
    else
        parallel_task "$work/run" "$key" out "$BIN" "$copy" "$case"
    fi
done < "$work/tasks.tsv"
if ! parallel_run "$work/run" "$procs"; then
    echo 'check-oracle: FAIL (parallel_run failed over the preservation replays; the parallel line above says why)' >&2
    exit 1
fi
if ! preserved=$(python3 -I - "$work" "$PRESERVATION_MUTANT" "$EXPECTED_MIN_PRESERVED_GOLDENS" \
        "$EXPECTED_MIN_PRESERVED_CASES" <<'PYEOF'
import collections, os, re, sys

work, mutant = sys.argv[1], sys.argv[2]
min_goldens, min_cases = int(sys.argv[3]), int(sys.argv[4])
refusal = re.compile(r"^EVALUATION FAULT: REF-014: .* for the ([a-z-]+): it is refused")
problems = []
golden_cases = collections.OrderedDict()
mutant_runs = []
for line in open(os.path.join(work, "tasks.tsv"), encoding="utf-8"):
    key, mode, name, _copy, case = line.rstrip("\n").split("\t")
    run = os.path.join(work, "run", key)
    with open(run + ".rc", encoding="utf-8") as f:
        rc = f.read().strip()
    with open(run + ".out", encoding="utf-8") as f:
        out = f.read()
    if rc == "0" and out.startswith("oracle: PASS"):
        verdict = ("equal", "")
    else:
        m = refusal.match(out)
        verdict = ("refused", m.group(1)) if rc == "1" and m else ("other", out.strip().replace("\n", " | "))
    if mode == "closed":
        golden_cases.setdefault(name, []).append((case, verdict))
    else:
        mutant_runs.append((case, verdict))
carried, cases, forms = 0, 0, collections.Counter()
for name, results in golden_cases.items():
    kinds = {v[0] for _, v in results}
    if kinds == {"refused"}:
        forms[results[0][1][1]] += 1
        continue
    if "refused" in kinds:
        problems.append("%s: the copy with the property refuses some cases and plays others — the refusal of a "
                        "document is all its cases' or none" % name)
        continue
    carried += 1
    for case, (kind, detail) in results:
        cases += 1
        if kind != "equal":
            problems.append("%s does not keep its expectation with the property and every input member complete: %s"
                            % (case, detail))
if mutant not in golden_cases or {v[0] for _, v in golden_cases[mutant]} != {"equal"}:
    problems.append("PRESERVATION_MUTANT %s is no golden the check carries — its red row reads nothing" % mutant)
elif not mutant_runs:
    problems.append("PRESERVATION_MUTANT %s ran no case without --closed-inputs" % mutant)
elif all(kind == "equal" for _, (kind, _d) in mutant_runs):
    problems.append("the red row: the copy of %s with the property and without --closed-inputs plays all %d of its "
                    "cases, so the check cannot tell the strict reading from the ordinary one" % (mutant, len(mutant_runs)))
if carried < min_goldens:
    problems.append("%d golden(s) carried, but EXPECTED_MIN_PRESERVED_GOLDENS pins at least %d" % (carried, min_goldens))
if cases < min_cases:
    problems.append("%d case(s) carried, but EXPECTED_MIN_PRESERVED_CASES pins at least %d" % (cases, min_cases))
if problems:
    sys.stderr.write("".join("check-oracle: FAIL (preservation) — %s\n" % p for p in problems))
    sys.exit(1)
strict = [l.strip() for l in open(os.path.join(work, "strict.txt"), encoding="utf-8") if l.strip()]
missed = sum(1 for _, (kind, _d) in mutant_runs if kind != "equal")
print("preservation with complete knowledge: %d goldens and %d cases play, floors %d and %d; %d goldens refused per "
      "form (%s); %d carrying the property already, played by check 3 (%s); red row: %s misses %d of %d cases without "
      "--closed-inputs" % (carried, cases, min_goldens, min_cases, sum(forms.values()),
                           ", ".join("%s %d" % f for f in sorted(forms.items(), key=lambda x: (-x[1], x[0]))),
                           len(strict), ", ".join(strict) or "none", mutant, missed, len(mutant_runs)))
PYEOF
); then
    status=1
fi

if [ "$status" -eq 0 ]; then
    printf 'check-oracle: PASS (ref suite %s worked examples, floor %s; %s cases replayed exactly, floor %s' \
        "$SUITE_N" "$EXPECTED_MIN_EXAMPLES" "$n_cases" "$EXPECTED_MIN_CASES"
    if [ -n "$uncovered" ]; then
        printf '; NOT replayed, no case file yet:%s' "$uncovered"
    fi
    printf '; %s)\n' "$preserved"
fi
exit "$status"
