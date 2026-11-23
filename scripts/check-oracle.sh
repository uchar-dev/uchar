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
#      below;
#   6. monotonicity (the same section): per golden that check 5 carries or
#      that carries the property itself, per case and per open input member
#      the case gives, the end state without that input keeps every outcome
#      of the end state with it, without exception — see below.
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
EXPECTED_MIN_PRESERVED_GOLDENS=279  # 2026-11-22: measured 279 (check 5: goldens whose forms ref carries strictly, the folds over a set, the quantifier fold, the defaults group, the creations off a cycle and the IfThenElse among them; 280 before jutterswet-grens-per-jutter, whose Lets are read in the filter and the matrix of a quantifier fold, was refused as let-in-composite)
EXPECTED_MIN_PRESERVED_CASES=782    # 2026-11-22: measured 782 (check 5: their case files; 784 before that golden's two)

# check 5's own red row: a golden on which the copy with the property and WITHOUT --closed-inputs gives another
# outcome than the case expects. Measured 2026-11-21: jutterswet-leeg-deeltal case1-leeg-deeltal, an open bonus as the
# dividend, "V1.loon = leeg, expected 0". Where that copy plays the golden's every case, the leg could not tell the
# strict reading from the ordinary one, and the gate fails.
PRESERVATION_MUTANT=jutterswet-leeg-deeltal

EXPECTED_MIN_MONOTONE_RUNS=2652     # 2026-11-23: measured 2652 (check 6: runs without one input compared with the case), raised from 2226 with the card one-empty, part 2: a test on leeg or gevuld, the filled-value count, a fold filter on gevuld and an initialisation read over an open value as every other read is, so the 396 runs in which a closure decided, until then counted apart, are compared; 2648 until the member left out was read off the document, which compares the 4 runs of jutterswet-keurunie case2, whose case spells the characteristics otherwise than their declaration

# check 6's own red row: a run whose member left out is not open without its value, compared all the same, must lose
# an outcome. Measured 2026-11-23: jutterswet-volledig-gegeven case2 without the toeslag of V1, which is declared
# complete: without its value it is settled leeg, and the uitbetaling 55 becomes 50. Where it keeps every outcome, the
# comparison could not see a change, and the gate fails.
MONOTONE_MUTANT='jutterswet-volledig-gegeven/case2-bijslag-open.json|attribute|V1|toeslag'

# check 6: the runs whose difference is a gap of RUNTIME-SEMANTICS ch. 1.5 rather than a fault of ref, one per line,
# '<golden>/<case file>|<attribute or characteristic>|<object>|<member>|<reason>', the input "-" for a case whose run
# with every input it gives already faults. Empty: every run the leg compares keeps every outcome. Stale-failing in
# both directions: a listed run that keeps every outcome, or that is not compared, fails.
MONOTONE_GAPS=''

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

# check 6: monotonicity (RUNTIME-SEMANTICS ch. 1.5, "The two requirements"): giving an open input member a value adds
# outcomes and changes none, without exception. Per golden that check 5 carries (every case of its strict copy plays)
# and per golden that carries the property itself, per case file and per input the case gives (an attribute value, a
# characteristic in `characteristics` or in `characteristicsAbsent`), the case without that input runs beside the case
# as it is, both with `runner --end-state`: the end state of ch. 5.12a with its open members. The run without the input
# keeps every outcome of its end state in the other: each of its lines that is not open stands there; a member it has
# no line for (settled leeg, or standing not to hold) has none there either; a link or an object the other adds lies on
# a relation it has open. What the member left out is without its value is read off the document, not off ref's end
# state, so that ref closing an open input member cannot take its run out of the comparison (ch. 1.5), in four
# standings: derived, where a writer other than an initialisation writes it on the object's type (a rule other than an
# initialisation, or a defaults group with a default other than its initialisation default or with an exception
# group); an open input member, where no such writer writes it and its declaration does not say its input is complete;
# complete, declared so with no writer, settled leeg; and initialised, declared complete with an initialisation as its
# one writer, settled leeg below the initialisation, which then gives its value. A run that leaves out an open input
# member must have it open in ref's end state, and a run that leaves out a complete one must not; each fails by name
# where it does not. A run that leaves out a derived or an initialised member is compared where ref names the member
# open, and else counted apart, and so is one that leaves out a complete member. Every other difference fails by name
# unless MONOTONE_GAPS lists it; the comparison,
# the reading of the document and the judgement of a run pass rows of their own, and the run of MONOTONE_MUTANT,
# compared all the same, must lose an outcome. The runs go in one batch, one runner per core, and are judged in corpus
# order.
if ! python3 -I - "$list" "$work" <<'PYEOF'
import collections, copy, json, os, sys

list_path, work = sys.argv[1:3]
by_golden = collections.OrderedDict()
for line in open(list_path, encoding="utf-8"):
    case = line.rstrip("\n")
    if case:
        by_golden.setdefault(os.path.dirname(case), []).append(case)
# the goldens check 5 carries: every case of the strict copy plays
closed = collections.OrderedDict()
for line in open(os.path.join(work, "tasks.tsv"), encoding="utf-8"):
    key, mode, name, copy_path, case = line.rstrip("\n").split("\t")
    if mode != "closed":
        continue
    run = os.path.join(work, "run", key)
    ok = open(run + ".rc").read().strip() == "0" and open(run + ".out").read().startswith("oracle: PASS")
    entry = closed.setdefault(name, [copy_path, True])
    entry[1] = entry[1] and ok
strict = [l.strip() for l in open(os.path.join(work, "strict.txt"), encoding="utf-8") if l.strip()]
os.makedirs(os.path.join(work, "mono", "cases"))
with open(os.path.join(work, "mono", "tasks.tsv"), "w", encoding="utf-8") as tasks:
    for n, (golden, cases) in enumerate(by_golden.items(), 1):
        name = os.path.relpath(golden, "corpus/golden")
        if name in strict:
            doc = os.path.join(golden, "ir.json")
        elif name in closed and closed[name][1]:
            doc = closed[name][0]
        else:
            continue
        for m, case_path in enumerate(cases, 1):
            case_file = os.path.basename(case_path)
            tasks.write("f%d.%d\t%s\t%s\t-\t%s\t%s\n" % (n, m, name, case_file, doc, case_path))
            with open(case_path, encoding="utf-8") as f:
                case = json.load(f, object_pairs_hook=collections.OrderedDict)
            j = 0
            for i, inst in enumerate(case["input"]["instances"]):
                left_out = [("attribute", a, lambda c, i=i, a=a: c["input"]["instances"][i]["attributes"].pop(a))
                            for a in (inst.get("attributes") or {})]
                for field in ("characteristics", "characteristicsAbsent"):
                    left_out += [("characteristic", k,
                                  lambda c, i=i, field=field, k=k: c["input"]["instances"][i][field].remove(k))
                                 for k in (inst.get(field) or [])]
                for kind, member, drop in left_out:
                    j += 1
                    reduced = copy.deepcopy(case)
                    drop(reduced)
                    path = os.path.join(work, "mono", "cases", "%d.%d.%d.json" % (n, m, j))
                    with open(path, "w", encoding="utf-8") as f:
                        json.dump(reduced, f, ensure_ascii=False)
                    tasks.write("v%d.%d.%d\t%s\t%s\t%s|%s|%s\t%s\t%s\n"
                                % (n, m, j, name, case_file, kind, inst["label"], member, doc, path))
PYEOF
then
    echo 'check-oracle: FAIL (monotonicity: the cases without one input could not be written)' >&2
    exit 1
fi
mkdir "$work/mono/run" || exit 2
while IFS="$tab" read -r key _name _case _what doc file; do
    parallel_task "$work/mono/run" "$key" out "$BIN" --end-state "$doc" "$file"
done < "$work/mono/tasks.tsv"
if ! parallel_run "$work/mono/run" "$procs"; then
    echo 'check-oracle: FAIL (parallel_run failed over the monotonicity runs; the parallel line above says why)' >&2
    exit 1
fi
if ! monotone=$(python3 -I - "$work" "$MONOTONE_MUTANT" "$MONOTONE_GAPS" "$EXPECTED_MIN_MONOTONE_RUNS" <<'PYEOF'
import collections, json, os, re, sys

work, mutant, gaps_text = sys.argv[1], sys.argv[2], sys.argv[3]
min_runs = int(sys.argv[4])
problems = []

# --- the end state, read back (RUNTIME-SEMANTICS ch. 5.12a) ---
QUOTED = r'"(?:[^"\\]|\\.)*"'
NAME = r'(?:%s|«%s»\((?:[^()]|\([^()]*\))*\)#\d+)' % (QUOTED, QUOTED)
MEMBER = re.compile(r'^(attribute|characteristic) (%s) (%s)(?: (\[[^\]]*\]))?' % (NAME, QUOTED))
LINK = re.compile(r'^link (%s) (%s) (%s|open) (%s) (%s|open)$' % (QUOTED, QUOTED, NAME, QUOTED, NAME))
OBJECT = re.compile(r'^object (%s) ' % NAME)
MADE = re.compile(r'^«%s»\((.*)\)#\d+$' % QUOTED)


def member_key(line):
    # (kind, object, member, cell) of an attribute or characteristic line, else None
    m = MEMBER.match(line)
    return (m.group(1), m.group(2), m.group(3), m.group(4)) if m else None


def is_open(line):
    m = LINK.match(line)
    return (m is not None and "open" in (m.group(3), m.group(5))) or (line.endswith(" open") and member_key(line) is not None)


def open_on(reduced, fact_type, objects):
    # whether the end state [reduced] has the relation of [fact_type] open on one of [objects]
    for line in reduced:
        m = LINK.match(line)
        if m and m.group(1) == fact_type and "open" in (m.group(3), m.group(5)) \
                and ({m.group(3), m.group(5)} - {"open"}) & set(objects):
            return True
    return False


def lost(reduced, full):
    # the outcomes of the end state [reduced] that [full] does not keep
    out = []
    full_set = set(full)
    reduced_keys = {member_key(l): l for l in reduced if member_key(l)}
    objects = {OBJECT.match(l).group(1) for l in reduced if OBJECT.match(l)}

    def made_on_open(name):
        # a made object the reduced run lacks: made for an object of it on a relation open there, or for such a
        # made object in turn
        made = MADE.match(name)
        if not made:
            return False
        subject = made.group(1)
        if subject in objects:
            return any(is_open(l) and l.startswith("link ") and subject in l for l in reduced)
        return made_on_open(subject)

    for line in reduced:
        if not is_open(line) and line not in full_set:
            out.append("lost: " + line)
    for line in full:
        key = member_key(line)
        if key is not None and key[1] in objects:
            before = reduced_keys.get(key)
            if before is None:
                out.append("settled without the input, now: " + line)
            elif before != line and not is_open(before):
                out.append("changed: %s -> %s" % (before, line))
        m = LINK.match(line)
        if m and line not in reduced and not open_on(reduced, m.group(1), [m.group(3), m.group(5)]) \
                and not any(o not in objects and made_on_open(o) for o in (m.group(3), m.group(5))):
            out.append("a link on no open relation: " + line)
        o = OBJECT.match(line)
        if o and o.group(1) not in objects and not made_on_open(o.group(1)):
            out.append("an object on no open relation: " + line)
    return out


# --- the comparison's own rows ---
V = 'attribute "V1" '
rows = [
    ("equal states keep everything", ['object "V1" "Vondst"', V + '"vindloon" 10'], ['object "V1" "Vondst"', V + '"vindloon" 10'], True),
    ("an open member may become a value", ['object "V1" "Vondst"', V + '"vindloon" open'], ['object "V1" "Vondst"', V + '"vindloon" 10'], True),
    ("a value lost", ['object "V1" "Vondst"', V + '"vindloon" 10'], ['object "V1" "Vondst"', V + '"vindloon" open'], False),
    ("a value changed", ['object "V1" "Vondst"', V + '"vindloon" 10'], ['object "V1" "Vondst"', V + '"vindloon" 5'], False),
    ("settled leeg, then a value", ['object "V1" "Vondst"'], ['object "V1" "Vondst"', V + '"vindloon" 10'], False),
    ("stands not to hold, then holds", ['object "V1" "Vondst"'], ['object "V1" "Vondst"', 'characteristic "V1" "kostbaar"'], False),
    ("an open characteristic may hold", ['object "V1" "Vondst"', 'characteristic "V1" "kostbaar" open'],
     ['object "V1" "Vondst"', 'characteristic "V1" "kostbaar"'], True),
    ("a fact lost", ['channel "r" "V1" EF-005'], [], False),
    ("a fact added", [], ['channel "r" "V1" EF-005'], True),
    ("a link on an open relation", ['object "J1" "Jutter"', 'link "vangst" "jutter" "J1" "vondst" open'],
     ['object "J1" "Jutter"', 'link "vangst" "jutter" "J1" "vondst" open', 'link "vangst" "jutter" "J1" "vondst" "V1"'], True),
    ("a link on no open relation", ['object "J1" "Jutter"'], ['object "J1" "Jutter"', 'link "vangst" "jutter" "J1" "vondst" "V1"'], False),
    ("a made object on an open relation",
     ['object "J1" "Jutter"', 'link "proefvangst" "jutter" "J1" "proefvondst" open'],
     ['object "J1" "Jutter"', 'link "proefvangst" "jutter" "J1" "proefvondst" open', 'object «"maak"»("J1")#1 "Vondst"',
      'attribute «"maak"»("J1")#1 "gewicht" 3'], True),
    ("a made object on no open relation", ['object "J1" "Jutter"'], ['object "J1" "Jutter"', 'object «"maak"»("J1")#1 "Vondst"'], False),
    ("a made object made for a made object on an open relation",
     ['object "J1" "Jutter"', 'link "proefvangst" "jutter" "J1" "proefvondst" open'],
     ['object "J1" "Jutter"', 'object «"maak"»("J1")#1 "Vondst"', 'object «"meld"»(«"maak"»("J1")#1)#1 "Melding"',
      'link "melding" "vondst" «"maak"»("J1")#1 "melding" «"meld"»(«"maak"»("J1")#1)#1'], True),
]
for name, reduced, full, keeps in rows:
    if (not lost(reduced, full)) != keeps:
        problems.append('the comparison fails its row "%s": it says %s' % (name, "keeps" if not keeps else "loses"))


# --- whether a member left out is an open input member, read off the document (RUNTIME-SEMANTICS ch. 1.5) ---
def norm(s):
    return " ".join(s.split()).lower()


def chain(doc, otype):
    # the object type [otype] and its supertypes, nearest first
    types = {norm(o["name"]): o for o in doc["model"]["objectTypes"]}
    out, t = [], norm(otype)
    while t is not None:
        if t not in types or types[t] in out:
            raise ValueError("no object type %r, or a cycle of supertypes through it" % t)
        out.append(types[t])
        t = norm(types[t]["supertype"]) if "supertype" in types[t] else None
    return out


def writers(doc):
    # (subject type, kind, member, derives) of every writer: a rule version (one that is neither an initialisation nor
    # carries a presentation assigns a characteristic, WF-022) and a defaults group. Every writer derives the member
    # but an initialisation: a rule version marked so, or a defaults group whose defaults are its initialisation
    # default alone, without an exception group (an exception group decides above the input)
    for rule in doc["rules"]["rules"]:
        for v in rule["versions"]:
            t = v["target"]["attribute"] if v["target"]["node"] == "DimensionCellRead" else v["target"]
            init = v.get("initialisation") is True
            yield (t["subjectType"], "attribute" if init or "presentation" in v else "characteristic", t["attribute"],
                   not init)
    for group in doc["rules"].get("prioritizedDefaults", []):
        kind = "characteristic" if "characteristic" in group else "attribute"
        yield (group["subject"], kind, group[kind],
               any(d.get("initialisation") is not True or "exceptions" in d for d in group["defaults"]))


def standing(doc, otype, kind, member):
    # what the member [member] of an object of [otype] is without a value — "derived"; "input"; "complete", declared
    # complete with no writer, settled leeg; or "initialised", declared complete with an initialisation as its one
    # writer, settled leeg below the initialisation, which then gives its value — and the name its declaration gives
    # it, by which the end state names it (a case may spell it otherwise)
    declared = [m for o in chain(doc, otype) for m in o["attributes" if kind == "attribute" else "characteristics"]
                if norm(m["name"]) == norm(member)]
    if not declared:
        raise ValueError("%s has no %s %r" % (otype, kind, member))
    names = {norm(o["name"]) for o in chain(doc, otype)}
    on = [derives for t, k, m, derives in writers(doc) if k == kind and norm(m) == norm(member) and norm(t) in names]
    if any(on):
        return "derived", declared[0]["name"]
    if declared[0].get("complete") is not True:
        return "input", declared[0]["name"]
    return ("initialised" if on else "complete"), declared[0]["name"]


def number(n):
    return {"node": "Literal", "value": {"number": n}}


def ot(name, attributes, characteristics=(), supertype=None):
    o = {"name": name, "attributes": attributes, "characteristics": [{"name": c} for c in characteristics]}
    if supertype:
        o["supertype"] = supertype
    return o


def target(subject, member):
    return {"node": "AttributeReference", "attribute": member, "subjectType": subject}


row_doc = {
    "model": {"objectTypes": [
        ot("Vondst", [{"name": "taxatie"}, {"name": "toeslag", "complete": True}, {"name": "strandloon"},
                      {"name": "meldloon"}, {"name": "vindloon"}, {"name": "bergloon"},
                      {"name": "premie", "complete": True}, {"name": "keurloon"}], ["kostbaar", "gemeld", "Gekeurd"]),
        ot("Strandvondst", [], [], "Vondst")]},
    "rules": {
        "rules": [
            {"versions": [{"target": target("Vondst", "strandloon"), "initialisation": True}]},
            {"versions": [{"target": target("Vondst", "vindloon"), "presentation": "calculation"}]},
            {"versions": [{"target": target("Vondst", "kostbaar")}]},
            {"versions": [{"target": target("Strandvondst", "bergloon"), "presentation": "calculation"}]},
            {"versions": [{"target": target("Vondst", "premie"), "initialisation": True}]}],
        "prioritizedDefaults": [
            {"subject": "Vondst", "attribute": "meldloon", "defaults": [{"consequence": number("3"), "initialisation": True}]},
            {"subject": "Vondst", "attribute": "taxatie", "defaults": [{"consequence": number("5")},
                                                                      {"consequence": number("3"), "initialisation": True}]},
            {"subject": "Vondst", "attribute": "keurloon",
             "defaults": [{"consequence": number("3"), "initialisation": True,
                           "exceptions": {"unordered": True, "members": [{"defaults": [{"consequence": number("8")}]}]}}]}]}}
standing_rows = [
    ("an input member", "Strandvondst", "characteristic", "gemeld", "input", "gemeld"),
    ("an input member the case spells otherwise", "Vondst", "characteristic", " GEMELD", "input", "gemeld"),
    ("a declaration with capitals, the case spelling it otherwise", "Vondst", "characteristic", "gekeurd", "input",
     "Gekeurd"),
    ("a member declared complete, on a subtype", "Strandvondst", "attribute", "toeslag", "complete", "toeslag"),
    ("a member declared complete that only an initialisation writes", "Vondst", "attribute", "premie", "initialised",
     "premie"),
    ("a member that only an initialisation rule writes", "Vondst", "attribute", "strandloon", "input", "strandloon"),
    ("a member that only the initialisation default of a stack writes", "Vondst", "attribute", "meldloon", "input",
     "meldloon"),
    ("a member a stack with another default writes", "Vondst", "attribute", "taxatie", "derived", "taxatie"),
    ("a member a stack writes whose initialisation default carries an exception group", "Vondst", "attribute",
     "keurloon", "derived", "keurloon"),
    ("a member a calculation writes", "Strandvondst", "attribute", "vindloon", "derived", "vindloon"),
    ("a characteristic an attribution writes", "Vondst", "characteristic", "kostbaar", "derived", "kostbaar"),
    ("a member a rule on a subtype writes, on the supertype", "Vondst", "attribute", "bergloon", "input", "bergloon"),
    ("a member a rule on a subtype writes, on the subtype", "Strandvondst", "attribute", "bergloon", "derived",
     "bergloon"),
]
for name, otype, kind, member, want, want_declared in standing_rows:
    got, declared = standing(row_doc, otype, kind, member)
    if got != want or declared != want_declared:
        problems.append('the reading of the document fails its row "%s": it says %s, declared as %r' % (name, got, declared))


def judge(standing_of, state, kind, label, member):
    # what a run without the input does in the comparison: "compare"; "apart" (a member that is not open without its
    # value); or a failure, "closed" (an open input member ref does not name open) or "opened" (a member declared
    # complete that no writer writes, which ref names open). A derived member, and a complete member below an
    # initialisation, which reads what it reads, that ref names open is compared all the same: its run can only add to
    # the comparison
    named_open = "%s %s %s open" % (kind, quote(label), quote(member)) in state
    if standing_of == "input":
        return "compare" if named_open else "closed"
    if standing_of == "complete":
        return "opened" if named_open else "apart"
    return "compare" if named_open else "apart"


def quote(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


judge_rows = [
    ("an open input member ref names open", "input", ['attribute "V1" "strandloon" open'], "compare"),
    ("an open input member ref closes (red)", "input", ['attribute "V1" "strandloon" 5'], "closed"),
    ("a member declared complete", "complete", [], "apart"),
    ("a member declared complete ref names open (red)", "complete", ['attribute "V1" "strandloon" open'], "opened"),
    ("a complete member below an initialisation ref names open", "initialised", ['attribute "V1" "strandloon" open'],
     "compare"),
    ("a complete member below an initialisation ref does not name open", "initialised",
     ['attribute "V1" "strandloon" 5'], "apart"),
    ("a derived member ref names open", "derived", ['attribute "V1" "strandloon" open'], "compare"),
    ("a derived member ref does not name open", "derived", ['attribute "V1" "strandloon" 5'], "apart"),
]
for name, standing_of, state, want in judge_rows:
    got = judge(standing_of, state, "attribute", "V1", "strandloon")
    if got != want:
        problems.append('the judgement of a run fails its row "%s": it says %s' % (name, got))

gaps = {}
for line in gaps_text.splitlines():
    if not line.strip():
        continue
    parts = line.split("|")
    if len(parts) != 5 or not parts[4].strip():
        problems.append("malformed MONOTONE_GAPS entry %r: '<golden>/<case file>|<kind>|<object>|<member>|<reason>'" % line)
        continue
    gaps["|".join(parts[:4])] = parts[4]
seen_gaps = set()


def read_run(key):
    run = os.path.join(work, "mono", "run", key)
    with open(run + ".rc", encoding="utf-8") as f:
        rc = f.read().strip()
    with open(run + ".out", encoding="utf-8") as f:
        out = f.read().splitlines()
    state = [l[len("state "):] for l in out if l.startswith("state ")]
    return rc, state


mutant_case, mutant_what = mutant.split("|", 1)
full = {}
counts = collections.Counter()
mutant_lost = None
documents = {}
for line in open(os.path.join(work, "mono", "tasks.tsv"), encoding="utf-8"):
    key, name, case_file, what, doc_path, file = line.rstrip("\n").split("\t")
    case = name + "/" + case_file
    rc, state = read_run(key)
    if what == "-":
        gap = case + "|-|-|-"
        if rc == "0":
            full[case] = state
            counts["cases"] += 1
            if gap in gaps:
                problems.append("stale MONOTONE_GAPS entry: %s runs now — remove it" % gap)
                seen_gaps.add(gap)
        elif gap in gaps:
            seen_gaps.add(gap)
        else:
            problems.append("%s does not run with the inputs it gives (exit %s)" % (case, rc))
        continue
    if case not in full:
        continue
    gap = case + "|" + what
    kind, label, member = what.split("|", 2)
    if rc == "0":
        if case == mutant_case and what == mutant_what:
            mutant_lost = lost(state, full[case])
        if doc_path not in documents:
            with open(doc_path, encoding="utf-8") as f:
                documents[doc_path] = json.load(f)
        with open(file, encoding="utf-8") as f:
            otype = next(i["objectType"] for i in json.load(f)["input"]["instances"] if i["label"] == label)
        standing_of, declared = standing(documents[doc_path], otype, kind, member)
        verdict = judge(standing_of, state, kind, label, declared)
        if verdict == "apart":
            counts["derived"] += 1
            continue
        found = {"closed": ["ref does not name the open input member open"],
                 "opened": ["ref names the member open, which is declared complete and has no writer"]}.get(verdict) or lost(state, full[case])
    else:
        found = ["the run without the input faults (exit %s)" % rc]
    counts["runs"] += 1
    if found and gap in gaps:
        seen_gaps.add(gap)
    elif found:
        problems.append("%s without the %s %s of %s: %s" % (case, kind, member, label, "; ".join(found[:4])))
    elif gap in gaps:
        problems.append("stale MONOTONE_GAPS entry: %s keeps every outcome now — remove it" % gap)
        seen_gaps.add(gap)
for gap in gaps:
    if gap not in seen_gaps:
        problems.append("stale MONOTONE_GAPS entry: %s is not compared — remove it" % gap)
if mutant_lost is None:
    problems.append("MONOTONE_MUTANT %s is no run the leg reads" % mutant)
elif not mutant_lost:
    problems.append("the red row: the run of MONOTONE_MUTANT %s keeps every outcome, so the comparison cannot see a change" % mutant)
if counts["runs"] < min_runs:
    problems.append("%d run(s) compared, but EXPECTED_MIN_MONOTONE_RUNS pins at least %d" % (counts["runs"], min_runs))
if problems:
    sys.stderr.write("".join("check-oracle: FAIL (monotonicity) — %s\n" % p for p in problems))
    sys.exit(1)
print("monotonicity: %d cases, %d runs without one input compared, floor %d, %d of a member not open without its "
      "value; %d listed gap(s) of ch. 1.5; %d comparison rows, %d rows of the reading of the document and %d of the "
      "judgement of a run; red row: %s loses %d outcome(s)"
      % (counts["cases"], counts["runs"], min_runs, counts["derived"], len(gaps), len(rows), len(standing_rows),
         len(judge_rows), mutant, len(mutant_lost)))
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
    printf '; %s; %s)\n' "$preserved" "$monotone"
fi
exit "$status"
