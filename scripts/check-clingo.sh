#!/bin/sh
# Gate 6: the differential — clingo replays every corpus case exactly
# (PLAN section 6, Phase 3: "differential testing becomes a square;
# every divergence is by definition a spec bug"). The backend translates
# ir.json + case.json to ASP-Core-2, clingo computes the stable model,
# and the model is checked against the same absolute expectations gate 5
# pins for ref/ — so a green gate 5 plus a green gate 6 is
# ref ≡ Clingo over the corpus, anchored to hand-written values, never
# parity alone.
#
# Checks:
#   1. backends/clingo builds (the OCaml workspace, scripts/lib/ocaml-build.sh);
#   2. clingo is available;
#   3. the backend's own suite runs green in shards, every unit of its work
#      once, with at least the floored number of checks;
#   4. every corpus/golden/**/case*.json replays exactly through the
#      translate → clingo → check pipeline, except the cases of a golden
#      that EXPECTED_CHANNEL below names: those refuse, every one of them,
#      by name with the register code the list pins; an empty case set is
#      a failure, never silently green.
#
# The shards and the replays run side by side, on one process per core
# (scripts/lib/parallel.sh; the measurement is below). The judge reads the
# shards first, and then each case's output and exit status in the order of
# the case list, so the report is the one a replay case by case would give.
#
# The gate takes no arguments. One pair plays through the runner itself,
# as backends/clingo/README.md shows:
#   _build/default/backends/clingo/bin/clingo_runner.exe <ir.json> <case.json>
set -u
[ "$#" -eq 0 ] || { echo "usage: scripts/check-clingo.sh (no arguments)" >&2; exit 2; }
cd "$(git rev-parse --show-toplevel)" || exit 2

if [ ! -d backends/clingo ]; then
    echo 'FAIL: backends/clingo not found' >&2
    exit 2
fi

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
    echo 'FAIL: need clingo on PATH (e.g. pip install clingo)' >&2
    exit 2
fi

. scripts/lib/ocaml-build.sh
if ! ensure_ocaml; then
    echo 'check-clingo: FAIL (build)' >&2
    exit 1
fi

# --- monotone floor (maintainer decision 2026-09-09) ---------------------------------------------
# A lower bound, never an equality: it catches SHRINK — the suite that quietly runs half — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
# The suite is floored because nothing else counts it — it is not corpus files, so no other gate
# would notice it halving. NO floor on the replayed-case count: that is byte-for-byte the file set
# gate 5 already floors (both scan corpus/golden for case*.json), and one quantity wants one floor.
EXPECTED_MIN_SUITE_CHECKS=4797   # 2026-11-22: measured 4797, so that a check missing from the shards fails here

# The backend's own unit suite carries the fault-mode proof obligations of the
# normalisation pass (no finest unit faults; 1/24 stays exact) that no corpus witness
# can exercise — an un-run suite is a silent green, so the gate runs it and
# demands a floored check count (the gate-9 zero-checks discipline).
#
# It runs in shards (backends/clingo/test/test_clingo.ml, "THE SUITE IN SHARDS"): its units of work are numbered, and
# UCHAR_SUITE_SHARD=k/n does those whose number is k modulo n and logs which. Silence is excluded by judge_suite below:
# every shard ran and ended green, all of them met the same number of units, their logs together hold every number
# exactly once, and their checks together reach the floor. It first passes rows of its own (SUITE_SELF_TEST).
SUITE_BIN=_build/default/backends/clingo/test/test_clingo.exe
BIN=_build/default/backends/clingo/bin/clingo_runner.exe

# One process per core, for the shards and the replays together, and two shards per process. A shard, clingo_runner and
# the clingo either starts are single-threaded. Measured 2026-11-22 on 16 cores:
#   - the suite takes 157 s in one process, four fifths of it in six of its 272 units (13 to 35 s each, one clingo
#     program), and a process without units 5 ms. The longest shard, the shards alone and all at once: 59 s of 4, 72 s of
#     6, 43 s of 8, 39 s of 12, 42 s of 16, 43 s of 32. With more shards than processes two of the six seldom stand in
#     one shard, whatever units come before them;
#   - the 1211 replays take 215 s one after the other, of which ten take 198 s (the longest 37 s,
#     jutterswet-opvolgerversies/case3-tweede-versie.json, whose clingo holds 10 GB), and 40 s on 8 processes;
#   - the gate takes 64 s on 8 processes and 47 s on 16.
# In `make gates` a process gets a fraction of a core, so the number of processes decides how long this gate takes there.
. scripts/lib/parallel.sh
if ! procs=$(parallel_jobs 1); then
    echo 'check-clingo: FAIL (no number of processes)' >&2
    exit 1
fi
shards=$((2 * procs))

# --- the refusal channel: which goldens clingo does not play, and why (fails in both directions) ---------
# Entry: "<golden-relative-path>|CLINGO-###[, CLINGO-###…]|<reason it is not played>", one per line. PLAYED IS THE
# DEFAULT: a golden that no line names plays every case exactly. A golden a line names refuses on EVERY case, by name,
# with the codes the line pins: the codes at the head of the runner's report (`TRANSLATION FAULT: CLINGO-032: …`,
# or `CLINGO-031: clingo: UNSATISFIABLE — …` for a guard that fired at solving, `CLINGO-033, CLINGO-034: clingo: …`
# where two guards fired), each a code of backends/clingo/codes.tsv of the kind refusal or limit. The line names
# every code at the head, in any order, and no other: a head with two codes is two limits of the encoding, and a line
# that named one of them would hide the other. A fallback code is the bottom arm of a dispatch and no pin names it
# (the fence of gate 8). Only the codes are compared; the reason is documentation and may be reworded without
# tripping the gate.
#
# Silence is excluded in every direction, each by golden name:
#   - a golden that refuses with a code and stands in no line fails;
#   - a line whose golden plays every case, or refuses with other codes, fails — the list is stale;
#   - a line whose golden has no case file under corpus/golden (gone, renamed) fails — likewise stale;
#   - a golden whose cases partly play and partly refuse fails: a line covers every case of its golden;
#   - a case that fails without a register code at its head, with an EF code among the codes there, a fallback
#     code or a code the register does not hold, or with a mismatch or a load error, fails whether or not a
#     line names its golden: it is no refusal of this backend, and only a refusal is listable.
# The list's own guards: a golden named twice fails (the first line would silently win), and so does a line
# without three fields, without a reason, with a code named twice, or with a code that is no refusal or limit of
# the register.
# The rules above are a function of the judge below; it first plays rows of its own through them (SELF_TEST,
# one per guard, both directions of the stale guard among them), and a row that does not give its verdict
# fails the gate.
#
# No floor on the list (maintainer decision 2026-09-09 asks a sentence where there is none): it is
# name-carrying and held in both directions, so it cannot shrink or grow unseen, and its length is a count of
# refusals, whose shrinking is progress; the count of replayed cases is the case set gate 5 floors.
EXPECTED_CHANNEL='
anchor/jutterswet-verzuimrente-dagen|CLINGO-032|the temporal total by the calendar outside a rounding (README [V6]): a day weighs 1/365 or 1/366 of its year, so the exact verzuimrente, 10160/73 and 4500/61, has a denominator that no decimal scale of this encoding holds, and the backend translates the total only as the base of a rounding. ref plays both cases; the rounded form, jutterswet-verzuimrente-dagen-afgerond, plays here
anchor/jutterswet-verzuimrente-30-360|CLINGO-033, CLINGO-034|two guards of the encoding fire on both cases: the exact maandrente, 50/3 in November, is written into the unbounded form, which this encoding holds as a whole number (CLINGO-033), and the rate times the afdrachtbedrag, the first factor of the product in the order of IMPLICIT-TIME §4, passes the bound 46 340 per factor of the machine-integer products at the scales of their decimals (CLINGO-034). ref plays both cases; the rounded form, jutterswet-verzuimrente-30-360-afgerond, plays here
fractional-power-negative-base|CLINGO-041|the encoding translates only a whole non-negative literal exponent, "fractional exponent 0.5 is not translated"; a negative base under it is leeg with an EF-044 deposit in ref (RUNTIME-SEMANTICS ch. 2.2), and ref and both Drools branches play both cases
jutterswet-keurquotient|CLINGO-039|a bare division in a condition whose quotient is not whole (13 / 2, 25 / 4): this encoding compares the quotient at the scale of its operands, an integer that 6.5 and 6.25 are not, so it refuses by name where ref compares the exact quotient; ref and both Drools branches play both cases (card clingo-nonexact-division-in-condition, 2026-11-06)
jutterswet-macht-van-nul|CLINGO-041|the encoding translates only a whole non-negative literal exponent: a negative one inverts its base, a division, which the scaled integers do not hold (0 tot de macht -1 is leeg with an EF-005 deposit in ref); ref and both Drools branches play the golden (card ref-macht-van-nul, 2026-11-06)
jutterswet-registratienummer|CLINGO-046|the exact-digit test ("numeriek met exact 9 cijfers") reads the characters of a text, which needs string functions that ASP-Core-2 does not have and this encoding, running no script, does not add; ref and both Drools branches play all eight cases
jutterswet-registratienummer-gehele-jaar|CLINGO-046|the exact-digit test read per day under the whole-period qualifier (RUNTIME-SEMANTICS ch. 3.9 on ch. 1.2) reads the characters of a text on every day, which needs the string functions this encoding does not have; ref and both Drools branches play all six cases
jutterswet-saldotoezicht|CLINGO-047|a read of the inconsistency channel ("regelversie controle saldo is inconsistent", RUNTIME-SEMANTICS ch. 5.6) has no translation in this encoding; ref and both Drools branches play both cases
jutterswet-tekst-van-een-waarde|CLINGO-042|the text of a value in a text composition (RUNTIME-SEMANTICS ch. 1.2, "Text coercion": 5,5, waar, 21-06-2026) needs string functions, which ASP-Core-2 does not have and this encoding, running no script, does not add; ref and both Drools branches play both cases (card tekst-van-een-waarde-in-een-tekstsamenstelling, 2026-11-06)
jutterswet-werfkeur|CLINGO-035|a quantifier fold over a relation that creations write, whose matrix reads the evaluation date the case does not carry: whether the fold reaches that read depends on the member order of RUNTIME-SEMANTICS ch. 5.7a (the links the case wrote, then instance order), which this encoding does not carry, so it refuses at translation where ref stops before the fault; the Drools backend refuses the same fold with DROOLS-259 (card created-member-order, 2026-11-11)
'

list=$(mktemp) || exit 2
tasks=$(mktemp -d) || exit 2
trap 'rm -f "$list"; rm -rf "$tasks"' EXIT

find corpus/golden -name 'case*.json' 2>/dev/null | sort > "$list"
# collect: a task per shard of the suite, keyed suite-<k>; and a task per case with a sibling ir.json, keyed by the
# case's line in the list
k=0
while [ "$k" -lt "$shards" ]; do
    parallel_task "$tasks" "suite-$k" out env UCHAR_SUITE_SHARD="$k/$shards" UCHAR_SUITE_SHARD_LOG="$tasks/suite-$k.log" "$SUITE_BIN"
    k=$((k + 1))
done
n_cases=0
while IFS= read -r case; do
    n_cases=$((n_cases + 1))
    ir=$(dirname "$case")/ir.json
    if [ -f "$ir" ]; then
        parallel_task "$tasks" "$n_cases" out "$BIN" "$ir" "$case"
    fi
done < "$list"
echo "check-clingo: the suite in $shards shards and the replays, on $procs processes (one per core)"
if ! parallel_run "$tasks" "$procs"; then
    echo 'check-clingo: FAIL (parallel_run failed over the shards and the replays; the parallel line above says why)' >&2
    exit 1
fi

# judge: the shards, then the cases in the order of the list
python3 -I - "$list" "$tasks" backends/clingo/codes.tsv "$EXPECTED_CHANNEL" "$shards" "$EXPECTED_MIN_SUITE_CHECKS" <<'PYEOF'
import os, re, sys

list_path, tasks, codes_path, channel_text, n_shards, suite_floor = sys.argv[1:7]
PLAYED = "PLAYED"


def judge_suite(shards, floor):
    """shards: for k of 0..n-1 the exit status of shard k/n (None where it did not run), its output and its log (None
    where it wrote none). The failures, and the units and checks of the shards together."""
    failures = []
    names = ["%d/%d" % (k, len(shards)) for k in range(len(shards))]
    totals, ran, checks = {}, {}, 0
    for name, (rc, out, log) in zip(names, shards):
        if rc is None:
            failures.append("shard %s of the suite did not run" % name)
            continue
        if rc != "0":
            failures.append("shard %s of the suite ends with status %s:\n%s" % (name, rc, out.rstrip("\n")))
        lines = (log or "").split("\n")
        tail = re.fullmatch(r"TOTAL ([0-9]+)\nCHECKS ([0-9]+)\n", "\n".join(lines[-3:]))
        runs = [re.fullmatch(r"RUN ([0-9]+)", line) for line in lines[:-3]]
        if not tail or not all(runs):
            failures.append("shard %s of the suite logs no lines RUN <unit> with TOTAL <units> and CHECKS <checks> at the end" % name)
            continue
        totals.setdefault(int(tail.group(1)), []).append(name)
        checks += int(tail.group(2))
        for run in runs:
            ran.setdefault(int(run.group(1)), []).append(name)
    if len(totals) > 1:
        failures.append("the shards of the suite met different numbers of units: %s"
                        % "; ".join("%d (%s)" % (total, ", ".join(totals[total])) for total in sorted(totals)))
    units = max(totals, default=0)
    for what, numbers in (("ran more than once", sorted(u for u in ran if len(ran[u]) > 1)),
                          ("ran in no shard", sorted(set(range(units)) - set(ran))),
                          ("ran beyond the %d units the shards met" % units, sorted(u for u in ran if u >= units))):
        if numbers:
            failures.append("%d unit(s) of the suite %s: %s" % (len(numbers), what, ", ".join(
                "%d%s" % (u, " (%s)" % ", ".join(ran[u]) if u in ran else "") for u in numbers[:8]) + (", …" if len(numbers) > 8 else "")))
    if checks < floor:
        failures.append("the backend unit suite ran %d checks, but EXPECTED_MIN_SUITE_CHECKS pins at least %d — the suite shrank (a deliberately dropped check lowers the floor in the same commit)" % (checks, floor))
    return failures, units, checks


def log_of(runs, total=7, checks=4):
    return "".join("RUN %d\n" % u for u in runs) + "TOTAL %d\nCHECKS %d\n" % (total, checks)


S0, S1, S2 = ("0", "", log_of([0, 3, 6])), ("0", "", log_of([1, 4])), ("0", "", log_of([2, 5]))
SUITE_SELF_TEST = [
    # (row, the shards, the fragment of the first failure they must give, or None for a pass)
    ("every unit once", [S0, S1, S2], None),
    ("a shard left out", [S0, S1, (None, "", None)], "shard 2/3 of the suite did not run"),
    ("a shard that ran twice, in the place of another", [S0, S1, S1], "2 unit(s) of the suite ran more than once: 1 (1/3, 2/3), 4 (1/3, 2/3)"),
    ("a number that is missing", [S0, ("0", "", log_of([1])), S2], "1 unit(s) of the suite ran in no shard: 4"),
    ("the last number missing", [("0", "", log_of([0, 3])), S1, S2], "1 unit(s) of the suite ran in no shard: 6"),
    ("a total that differs", [S0, S1, ("0", "", log_of([2, 5], total=8))], "met different numbers of units: 7 (0/3, 1/3); 8 (2/3)"),
    ("a number beyond the total", [S0, S1, ("0", "", log_of([2, 5, 8]))], "1 unit(s) of the suite ran beyond the 7 units the shards met: 8 (2/3)"),
    ("a shard that fails", [S0, ("1", "FAIL a check", log_of([1, 4])), S2], "shard 1/3 of the suite ends with status 1:\nFAIL a check"),
    ("a shard that ends before its last unit", [S0, ("0", "", "RUN 1\n"), S2], "shard 1/3 of the suite logs no lines RUN <unit>"),
    ("a shard without a log", [S0, ("0", "", None), S2], "shard 1/3 of the suite logs no lines RUN <unit>"),
    ("a log with another line", [S0, ("0", "", "RUN 1\nRAN 4\nTOTAL 7\nCHECKS 4\n"), S2], "shard 1/3 of the suite logs no lines RUN <unit>"),
    ("a check that is missing", [S0, ("0", "", log_of([1, 4], checks=3)), S2], "ran 11 checks, but EXPECTED_MIN_SUITE_CHECKS pins at least 12"),
]
for row, shards, want in SUITE_SELF_TEST:
    got = judge_suite(shards, 12)[0]
    if not (got == [] if want is None else len(got) >= 1 and want in got[0]):
        sys.stderr.write("check-clingo: FAIL (self-test row '%s' of the shards: wanted %s, got %s)\n"
                         % (row, "a pass" if want is None else "a failure with '%s' first" % want, got or "a pass"))
        sys.exit(1)


def read_codes(path):
    """code -> kind, from the register of this backend (check-exec-codes.sh holds it to the sources)."""
    rows = open(path, encoding="utf-8").read().split("\n")
    kinds = {}
    for row in rows[1:]:
        if row:
            cols = row.split("\t")
            kinds[cols[0]] = cols[1]
    return kinds


def parse_channel(text, kinds):
    """The lines of EXPECTED_CHANNEL: golden -> its codes in ascending order, and the failures of the list's own
    guards."""
    pins, failures = {}, []
    for line in text.split("\n"):
        if not line.strip():
            continue
        fields = line.split("|", 2)
        if len(fields) != 3:
            failures.append("malformed channel line '%s': want '<golden>|CLINGO-###[, CLINGO-###…]|<reason>'" % line)
            continue
        golden, field, reason = fields
        if not reason.strip():
            failures.append("channel line without a reason: '%s' — every line is '<golden>|CLINGO-###|<why it is not played>'" % golden)
        if golden in pins:
            failures.append("duplicate channel line: '%s' is named more than once in EXPECTED_CHANNEL — the first line would silently win" % golden)
            continue
        codes = field.split(", ")
        for code in codes:
            if not re.fullmatch(r"CLINGO-[0-9]{3}", code):
                failures.append("channel line '%s' pins '%s', which is no code CLINGO-### of this backend" % (golden, code))
            elif code not in kinds:
                failures.append("channel line '%s' pins %s, which %s does not hold" % (golden, code, codes_path))
            elif kinds[code] not in ("refusal", "limit"):
                failures.append("channel line '%s' pins %s of the kind %s: a fallback code is the bottom arm of a dispatch, and only a refusal or a limit is listable" % (golden, code, kinds[code]))
        if len(set(codes)) != len(codes):
            failures.append("channel line '%s' names a code twice: '%s'" % (golden, field))
        pins[golden] = tuple(sorted(set(codes)))
    return pins, failures


HEAD_FAULT = re.compile(r"TRANSLATION FAULT: ([A-Z]+-[0-9]{3}): ")
HEAD_SOLVE = re.compile(r"((?:[A-Z]+-[0-9]{3})(?:, [A-Z]+-[0-9]{3})*): clingo: ")


def outcome(rc, out, kinds):
    """PLAYED, the codes of a refusal in ascending order, or None with the reason this is no refusal."""
    if rc == "0":
        return PLAYED, None
    if rc != "1":
        return None, "exit status %s, a load error or no clingo" % rc
    first = out.split("\n", 1)[0]
    m = HEAD_FAULT.match(first) or HEAD_SOLVE.match(first)
    if not m:
        return None, "no register code at the head of the report"
    codes = m.group(1).split(", ")
    for code in codes:
        if not code.startswith("CLINGO-"):
            return None, "%s at the head of the report, a fault where ref plays" % code
        if code not in kinds:
            return None, "%s, which %s does not hold" % (code, codes_path)
        if kinds[code] not in ("refusal", "limit"):
            return None, "%s, a %s code" % (code, kinds[code])
    return tuple(sorted(set(codes))), None


def named(codes):
    """PLAYED, or the codes of a refusal as a line of EXPECTED_CHANNEL writes them."""
    return codes if codes == PLAYED else ", ".join(codes)


def judge(cases, channel_text, kinds):
    """cases: (case path, exit status or None without an ir.json, output) in the order of the list. The
    failures of the channel rule, and the counts of the report."""
    pins, failures = parse_channel(channel_text, kinds)
    goldens, order = {}, []
    for path, rc, out in cases:
        if rc is None:
            failures.append("%s has no sibling ir.json" % path)
            continue
        golden = os.path.dirname(path)[len("corpus/golden/"):]
        if golden not in goldens:
            goldens[golden] = []
            order.append(golden)
        code, why = outcome(rc, out, kinds)
        goldens[golden].append((path, code, why, out))
    played_cases = refused_cases = 0
    for golden in order:
        rows = goldens[golden]
        bad = [r for r in rows if r[1] is None]
        for path, _, why, out in bad:
            failures.append("%s does not replay through clingo (%s):\n%s" % (path, why, out.rstrip("\n")))
        if bad:
            continue
        measured = set(r[1] for r in rows)
        pinned = pins.get(golden, PLAYED)
        if len(measured) > 1:
            failures.append("%s: its cases play and refuse, or refuse with other codes (%s) — a line of EXPECTED_CHANNEL covers every case of its golden" % (golden, "; ".join("%s %s" % (os.path.basename(r[0]), named(r[1])) for r in rows)))
            continue
        measured = measured.pop()
        if measured == pinned:
            if measured == PLAYED:
                played_cases += len(rows)
            else:
                refused_cases += len(rows)
        elif pinned == PLAYED:
            failures.append("%s refuses with %s on every case but stands in no line of EXPECTED_CHANNEL — make the backend carry the form, or pin it with its codes and the reason:\n%s" % (golden, named(measured), rows[0][3].rstrip("\n")))
        elif measured == PLAYED:
            failures.append("stale channel line: %s is pinned %s but plays every case — remove the line" % (golden, named(pinned)))
        else:
            failures.append("%s is pinned %s, but its cases refuse with %s:\n%s" % (golden, named(pinned), named(measured), rows[0][3].rstrip("\n")))
    for golden, codes in pins.items():
        if golden not in goldens:
            failures.append("stale channel line: %s is pinned %s, but no golden of that name has a case file under corpus/golden (gone, or renamed) — remove the line" % (golden, named(codes)))
    return failures, played_cases, refused_cases, len(pins)


SELF_KINDS = {"CLINGO-001": "limit", "CLINGO-002": "fallback", "CLINGO-005": "refusal", "CLINGO-031": "limit"}
G = "corpus/golden/g/case1.json"
G2 = "corpus/golden/g/case2.json"
FAULT1 = "TRANSLATION FAULT: CLINGO-001: a scaled value out of range"
FAULT5 = "TRANSLATION FAULT: CLINGO-005: a clamp bound that is no literal"
SELF_TEST = [
    # (row, channel text, cases, the fragment of the one failure it must give, or None for a pass)
    ("a pinned golden that refuses with its code passes", "g|CLINGO-001|r", [(G, "1", FAULT1), (G2, "1", FAULT1)], None),
    ("a refusal at solving is read off its head", "g|CLINGO-031|r", [(G, "1", "CLINGO-031: clingo: UNSATISFIABLE — a fail-fast guard fired: viol(...)")], None),
    ("an unpinned golden that plays passes", "", [(G, "0", "clingo: PASS (2 expectations)")], None),
    ("stale: a pinned golden that plays every case", "g|CLINGO-001|r", [(G, "0", "clingo: PASS (1 expectations)")], "stale channel line: g is pinned CLINGO-001 but plays"),
    ("stale: a pinned golden that is gone", "h|CLINGO-001|r", [(G, "0", "clingo: PASS (1 expectations)")], "stale channel line: h is pinned CLINGO-001, but no golden"),
    ("unpinned: a golden that refuses with a code and stands in no line", "", [(G, "1", FAULT1)], "g refuses with CLINGO-001 on every case but stands in no line"),
    ("a pinned golden that refuses with another code", "g|CLINGO-001|r", [(G, "1", FAULT5)], "g is pinned CLINGO-001, but its cases refuse with CLINGO-005"),
    ("a golden whose cases play and refuse", "g|CLINGO-001|r", [(G, "1", FAULT1), (G2, "0", "clingo: PASS (1 expectations)")], "a line of EXPECTED_CHANNEL covers every case"),
    ("a report without a code at its head is no refusal", "g|CLINGO-001|r", [(G, "1", "TRANSLATION FAULT: an uncoded refusal")], "no register code at the head"),
    ("an EF code at the head is no refusal", "g|CLINGO-001|r", [(G, "1", "EF-006: clingo: UNSATISFIABLE — a fail-fast guard fired")], "EF-006 at the head of the report"),
    ("a line that pins both codes of a head with two passes", "g|CLINGO-031, CLINGO-001|r", [(G, "1", "CLINGO-001, CLINGO-031: clingo: UNSATISFIABLE — …")], None),
    ("a line that pins one code of a head with two", "g|CLINGO-001|r", [(G, "1", "CLINGO-001, CLINGO-031: clingo: UNSATISFIABLE — …")], "g is pinned CLINGO-001, but its cases refuse with CLINGO-001, CLINGO-031"),
    ("a line that pins two codes of a head with one", "g|CLINGO-001, CLINGO-031|r", [(G, "1", FAULT1)], "g is pinned CLINGO-001, CLINGO-031, but its cases refuse with CLINGO-001"),
    ("an EF code beside a register code at the head is no refusal", "g|CLINGO-001|r", [(G, "1", "CLINGO-001, EF-006: clingo: UNSATISFIABLE — …")], "EF-006 at the head of the report"),
    ("a line that names a code twice", "g|CLINGO-001, CLINGO-001|r", [(G, "1", FAULT1)], "names a code twice"),
    ("a fallback code at the head is no refusal", "", [(G, "1", "TRANSLATION FAULT: CLINGO-002: a text operand shape")], "CLINGO-002, a fallback code"),
    ("a mismatch is no refusal", "g|CLINGO-001|r", [(G, "1", "MISMATCH: attribuut x: verwacht 1, werkelijk 2")], "no register code at the head"),
    ("a load error is no refusal", "", [(G, "2", "CASE ERROR: a case without expectations")], "exit status 2"),
    ("a golden named twice", "g|CLINGO-001|r\ng|CLINGO-001|r", [(G, "1", FAULT1)], "duplicate channel line: 'g'"),
    ("a line without a reason", "g|CLINGO-001|", [(G, "1", FAULT1)], "channel line without a reason: 'g'"),
    ("a line that pins a fallback code", "g|CLINGO-002|r", [(G, "1", FAULT1)], "pins CLINGO-002 of the kind fallback"),
    ("a line that pins no code of this backend", "g|DROOLS-042|r", [(G, "1", FAULT1)], "pins 'DROOLS-042', which is no code CLINGO-###"),
]
for row, text, cases, want in SELF_TEST:
    got = judge(cases, text, SELF_KINDS)[0]
    if want is None:
        ok = got == []
    else:
        ok = len(got) >= 1 and want in got[0]
    if not ok:
        sys.stderr.write("check-clingo: FAIL (self-test row '%s' of the channel rule: wanted %s, got %s)\n"
                         % (row, "a pass" if want is None else "a failure with '%s' first" % want, got or "a pass"))
        sys.exit(1)


def task(key, ending):
    """The file KEY.ENDING of a task, or None where the task has none."""
    path = os.path.join(tasks, "%s.%s" % (key, ending))
    return open(path, encoding="utf-8", errors="replace").read() if os.path.exists(path) else None


shards = []
for k in range(int(n_shards)):
    rc = task("suite-%d" % k, "rc")
    shards.append((rc and rc.strip(), task("suite-%d" % k, "out"), task("suite-%d" % k, "log")))
suite_failures, suite_units, suite_checks = judge_suite(shards, int(suite_floor))
kinds = read_codes(codes_path)
cases = []
for n, path in enumerate(open(list_path, encoding="utf-8").read().split("\n")[:-1], 1):
    rc_file = os.path.join(tasks, "%d.rc" % n)
    if not os.path.exists(rc_file):
        cases.append((path, None, ""))
        continue
    rc = open(rc_file, encoding="utf-8").read().strip()
    out = open(os.path.join(tasks, "%d.out" % n), encoding="utf-8", errors="replace").read()
    cases.append((path, rc, out))
if not cases:
    sys.stderr.write("FAIL: no case files under corpus/golden — the differential has nothing to replay\n")
    sys.exit(1)
failures, played_cases, refused_cases, n_pins = judge(cases, channel_text, kinds)
failures = suite_failures + failures
for f in failures:
    sys.stderr.write("FAIL: %s\n" % f)
if failures:
    sys.exit(1)
print("check-clingo: PASS (backend suite %d checks, floor %s, its %d units each once in %s shards; %d cases replay exactly through clingo — ref = clingo over the corpus; "
      "%d golden(s) in the refusal channel, by name, %d cases refused with the pinned codes; the channel rule passes %d rows of its own, the rule of the shards %d)"
      % (suite_checks, suite_floor, suite_units, n_shards, played_cases, n_pins, refused_cases, len(SELF_TEST), len(SUITE_SELF_TEST)))
PYEOF
