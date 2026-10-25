#!/bin/sh
# Gate 8: the red corpus — expected-fault cases (corpus/red). Each
# directory holds a verifier-clean ir.json whose case*.json declares
# `expectedFault {code, params}`: LOADING OR evaluation MUST fail
# observably in BOTH executors, as the fault kind the pin names
# (RUNTIME-SEMANTICS ch. 5.7b): the same EF code, and the pinned parameters among
# the ones the executor emits. No piece of message text is pinned (card
# fragment-cleanup, 2026-10-19; before it a case pinned a fragment of the report
# of each executor, and this gate kept a list of bottom sentences to hold the pins
# honest). Both phases count: the runners read expectedFault on its own, BEFORE
# the world, so a refusal by the case reader — an ambiguous role key, a link to an
# undeclared label — is pinnable exactly like a fault at evaluation.
# This is the standing regression fence for red paths — the WF-044
# unsolved state among them: Unresolved surfaces, never vanishes.
#
#   1. ref/ and backends/ build; clingo must be available;
#   2. every corpus/red/**/case*.json is replayed by both runners, which print
#      one `CODE <code> <params as JSON>` line per code they observed and the
#      case's own `CODE-PINNED <code> <params>`, decoded by the runner's own case
#      reader (never a shell scrape: a JSON string may spell any character as an
#      escape);
#   3. an empty red set is a failure, never silently green;
#   4. every red case pins `expectedFault.code`, the EF code of its fault
#      kind, and ref, clingo, and Drools where the case is not listed in the Drools
#      leg's exclusion list, each REPORT that code with the pinned parameters among
#      its own — or the executor stands, with the code of its own refusal and its
#      reason, in the divergence list below, which fails when stale in either
#      direction;
#   5. the parameters an executor emits are keys of the parameter column of the
#      table of fault kinds (an EF code) or of the line of its register (a REF or
#      CLINGO code); two cases of one document that differ in a parameter only are
#      told apart by their pinned parameters (the list of pairs below);
#   6. the fault-kind table of RUNTIME-SEMANTICS ch. 5.7b is read here,
#      strictly: every row parses, every witness it names exists and pins
#      the row's code, and every code a red case pins is a row;
#   7. every code a golden `inconsistent` expectation pins is a row whose
#      channel is §9.5, the codes a channel fact carries beside its
#      identity rule × subject (ch. 5.6; card channel-fact-codes,
#      2026-10-19). Gates 5, 6 and 12 replay those expectations; this gate
#      holds their codes to the table;
#   8. the registers of the two executors hold (scripts/check-exec-codes.sh),
#      and no list of this gate or of gate 12 names a `fallback` code as what a
#      witness proves (the fence on codes, below).
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

. scripts/lib/ocaml-build.sh
if ! ensure_ocaml; then
    echo 'check-red: FAIL (build)' >&2
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
EXPECTED_MIN_RED_CASES=44   # 2026-10-19: measured 44 (corpus/red/**/case*.json; 26 measured 2026-09-10, 50 before the card
                            # zero-division-channel moved six witnesses of a division by a numeric zero to the golden corpus)
# The code checks (card meldingscodes, 2026-10-18). RED_CODES counts the ref and clingo checks in
# which the executor reported the case's code: 44 cases × 2 executors minus the 6 entries of the
# divergence list, measured 82 (93 on 50 cases and 7 entries before the card zero-division-channel). EF_CODES counts the rows of the RUNTIME-SEMANTICS ch. 5.7b table,
# measured 24. The Drools leg's code checks get no floor of their own, for the reason its fault count
# has none (below): each is a case with a `drools` fragment, held by that pin and by the name-carrying
# exclusion list.
EXPECTED_MIN_RED_CODES=82   # 2026-10-19: measured 82 (ref 44 + clingo 38; 93 on 2026-10-18, lowered with the six moved cases)
# The parameter checks (card fragment-cleanup, 2026-10-19). RED_PARAMS counts, over the code checks of ref and clingo that
# did not stand in the divergence list, the pinned parameter pairs the executor confirmed among its own, an empty pin
# counting once: it is the one count that says the pins carry parameters at all, so a corpus whose params quietly emptied
# would keep every other check green. DROOLS_PARAMS is the same count over the Drools leg, kept apart because that leg
# may be skipped and the floor of the two OCaml executors must not move with it.
EXPECTED_MIN_RED_PARAMS=120   # 2026-10-19: measured 120 (ref 63 + clingo 57: the pinned pairs of the 44 cases, an empty pin once, clingo
                              # on 38 of them; 131 before the card zero-division-channel moved six cases, each an empty pin)
EXPECTED_MIN_DROOLS_PARAMS=28 # 2026-10-19: measured 28 (the 21 cases that FAULT on Drools; 33 on 26 before the card
                              # zero-division-channel)
EXPECTED_MIN_EF_CODES=31    # 2026-10-19: measured 31 (RUNTIME-SEMANTICS ch. 5.7b; 24 at card meldingscodes, 2026-10-18,
                            # raised with the seven §9.5 codes of card channel-fact-codes)
# GOLDEN_CODES counts the golden `inconsistent` expectations that pin a code (card channel-fact-codes): the codes
# gates 5, 6 and 12 hold the executors to on the §9.5 channel, so a count that quietly halved would halve that
# comparison and no other gate counts it.
EXPECTED_MIN_GOLDEN_CODES=80   # 2026-10-19: measured 80 (corpus/golden/**/case*.json; 73 at card channel-fact-codes, raised
                               # with the seven EF-005 pins of the six witnesses card zero-division-channel moved here)

if ! command -v python3 >/dev/null 2>&1; then
    echo 'FAIL: need python3 — the code leg reads the fault-kind table of RUNTIME-SEMANTICS ch. 5.7b and decodes the golden witnesses it names' >&2
    exit 2
fi

# --- the code divergence list (card meldingscodes, 2026-10-18; code column since card fragment-cleanup) -----------
# A red case states the ONE fault kind the semantics demands (`expectedFault.code`, with its parameters); what an
# executor does instead is executor conduct, kept here beside the other conduct lists (the Drools leg's exclusion
# list below, gate 12's EXPECTED_CHANNEL), never in the case: the corpus does not move when an executor moves. An
# entry admits that the executor fails the case for a reason of its own and names the code of that reason, a
# `REF-###` or `CLINGO-###` of its register (ref/codes.tsv, backends/clingo/codes.tsv): the executor must report
# exactly that code, never another EF code, which would be a disagreement about the kind, not conduct. The first word of
# the reason is its category, and it must be the kind of the code in the register: REFUSAL (a form the executor does
# not carry) for `refusal`, LIMIT (a limit of its own representation) for `limit`. A `fallback` code, the bottom arm of
# a dispatch, is never admitted: it is the same sentence for every form its arm catches, so a witness that rests on it
# witnesses nothing of its own (see the fence on codes below). CONTRACT (an ill-formed document; a red document is
# verifier-clean) is not admitted either.
#
# Stale-guarded both ways: an entry whose executor now reports the case's code, or reports another code than the
# entry's, fails, and so does an entry that no check consulted (its case is gone, or its executor makes no check on
# it); an unlisted check whose executor reports no code, or another one than the case's, fails as well.
#
# <case, relative to corpus/red, without .json>|<executor>|<CODE>|<CATEGORY> <reason>
divergence=$(mktemp) || exit 2
cat > "$divergence" <<'EOF2'
sum-fallback-always-evaluated/case1|clingo|CLINGO-007|REFUSAL measured 2026-10-18, case reworked 2026-10-19 (card zero-division-channel): the backend does not translate a sum over listed operands, "a sum over listed operands is not translated: …", so the stand-in whose read of the evaluation year without an evaluation date ref faults on (EF-011) is never evaluated; until that card the stand-in divided by a numeric zero (EF-005), which deposits since
day-kind-step-outside-range/case1|clingo|CLINGO-001|LIMIT measured 2026-10-18: the step's millisecond value 53375975860 "exceeds clingo's integer range (grounding would wrap silently)" at translation, where ref and Drools fault on the datum-tijd range of ch. 3.8 (EF-007)
date-time-step-outside-range/case1-plus-uren|clingo|CLINGO-001|LIMIT measured 2026-10-18: the step's millisecond value 2000000000000 "exceeds clingo's integer range" at translation, where ref and Drools fault on the datum-tijd range of ch. 3.8 (EF-007)
date-time-step-outside-range/case2-min-uren|clingo|CLINGO-001|LIMIT measured 2026-10-18: the step's millisecond value 2000000000000 "exceeds clingo's integer range" at translation, where ref and Drools fault on the datum-tijd range of ch. 3.8 (EF-007)
date-time-step-outside-range/case3-plus-dagen|clingo|CLINGO-001|LIMIT measured 2026-10-18: the step's millisecond value 60000000000 "exceeds clingo's integer range" at translation, where ref and Drools fault on the datum-tijd range of ch. 3.8 (EF-007)
date-time-step-outside-range/case4-telling-buiten-bereik|clingo|CLINGO-001|LIMIT measured 2026-10-18: the duration count 18446744073709551617 "exceeds clingo's integer range" at translation, where ref and Drools refuse the count itself as out of range (EF-008)
date-time-with-outside-range/case1-jaar-200-miljoen|clingo|CLINGO-029|LIMIT measured 2026-10-22 (card date-with-in-ref-and-clingo): the date construction's year fence, "dateTimeWith of a year after 5000000 is not translated (the civil day number would leave clingo's integers)", where ref and Drools fault on the datum-tijd range of ch. 3.8 (EF-007): every instant outside that range lies beyond the years this backend's day numbers hold
date-time-with-outside-range/case2-een-milliseconde-na-het-bereik|clingo|CLINGO-029|LIMIT measured 2026-10-22 (card date-with-in-ref-and-clingo): the same year fence, at year 146140482, where ref and Drools fault one millisecond past the top of the range (EF-007)
date-time-with-outside-range/case3-een-milliseconde-voor-het-bereik|clingo|CLINGO-029|LIMIT measured 2026-10-22 (card date-with-in-ref-and-clingo): the fence of a year before 1, "dateTimeWith of a year before 1 is not translated", where ref and Drools fault one millisecond before the bottom of the range (EF-007)
EOF2
div_used=$(mktemp) || exit 2
pinned_map=$(mktemp) || exit 2
n_red_codes=0
n_red_params=0
n_drools_params=0
n_div=0

# The pairs of cases of one document that differ in a parameter of their fault ONLY (card fragment-cleanup, acceptance
# (c)): their code is the same, so before the fragments left the text told them apart, and now their pinned `params`
# must. Gate 8 fails when the two cases of a pair pin the same params, and when a pair names a case that is gone.
# <case>|<case>|<reason>
pairs=$(mktemp) || exit 2
cat > "$pairs" <<'EOF2'
aanvoergeld-parameter-unbound/case1-tarief-ongebonden|aanvoergeld-parameter-unbound/case2-drempel-ongebonden|one document, two parameters the case leaves unbound: EF-014 differs in `parameter` only (aanvoertarief, aanvoerdrempel)
jutterswet-dubbele-melding/case1-beide-schrijvers-gelden|jutterswet-dubbele-melding/case2-een-van-twee-vondsten|one document, two instances whose writers disagree: EF-016 differs in `label` only (V, V2)
EOF2

# The comparison of one executor's observation with the pin, in python (the gate requires python3 already): the lines
# the runner printed (or the status line of the Drools red run) on stdin; arguments: executor, case key, the pinned code,
# the pinned params as JSON. Prints `OK <code seen: 0|1> <pinned parameter pairs confirmed, or 1 for an empty pin>`, or
# `DIV`, on stdout; a failure explains itself on stderr and exits 1. Holds, besides the pin, that every parameter the
# executor emits is a key of the parameter column of its code (the table of fault kinds for an EF code, the register of
# the executor for its own), and the fence on codes: a `fallback` code is never what the divergence list admits.
cmp=$(mktemp) || exit 2
cat > "$cmp" <<'PYEOF'
import json, sys

executor, key, pin_code, pin_params_json, divergence, params_file, ref_reg, clingo_reg = sys.argv[1:9]
pin_params = json.loads(pin_params_json)
text = sys.stdin.read()


def fail(msg):
    sys.stderr.write("FAIL: %s: %s\n" % (key, msg))
    sys.exit(1)


columns, kinds = {}, {}
for line in open(params_file, encoding="utf-8"):
    f = line.rstrip("\n").split(" ")
    if f[0] == "PARAMS":
        columns[f[1]] = [p for p in (f[2] if len(f) > 2 else "").split(",") if p]
for reg in (ref_reg, clingo_reg):
    for n, line in enumerate(open(reg, encoding="utf-8").read().split("\n")[1:]):
        c = line.split("\t")
        if len(c) == 5:
            columns[c[0]] = [p for p in c[3].split(",") if p]
            kinds[c[0]] = c[1]

# the observation: a list of (code, params), in report order
observed = []
if executor == "drools":
    line = text.strip().splitlines()[-1] if text.strip() else ""
    if not line.startswith("FAULT "):
        fail("Drools did not FAULT: %s" % line[:200])
    rest, dec = line[len("FAULT "):], json.JSONDecoder()
    while rest.startswith("EF-"):
        code, rest = rest[:6], rest[6:]
        if not rest.startswith(" {"):
            fail("the Drools status line gives %s no parameters (`FAULT EF-### {…}: …`): %s" % (code, line[:200]))
        params, end = dec.raw_decode(rest[1:])
        observed.append((code, params))
        rest = rest[1 + end:]
        if rest.startswith(", "):
            rest = rest[2:]
else:
    for l in text.splitlines():
        if l.startswith("CODE ") and not l.startswith("CODE-PINNED"):
            f = l.split(" ", 2)
            observed.append((f[1], json.loads(f[2])))
seen = [c for c, _ in observed]
for code, params in observed:
    if code not in columns:
        fail("%s reports the code %s, which is in no register or table (RUNTIME-SEMANTICS ch. 5.7b; the registers of the executors)" % (executor, code))
    for k in params:
        if k not in columns[code]:
            fail("%s emits the parameter %s on %s, which is no key of the parameter column of that code (%s)" % (executor, k, code, ",".join(columns[code]) or "—"))

entry = None
for line in open(divergence, encoding="utf-8"):
    f = line.rstrip("\n").split("|", 3)
    if len(f) == 4 and f[0] == key and f[1] == executor:
        entry = f
if entry is not None:
    _, _, ecode, reason = entry
    if executor not in ("ref", "clingo"):
        fail("the divergence list names the executor '%s' (ref or clingo; the Drools leg has its own exclusion list)" % executor)
    kind = kinds.get(ecode)
    category = reason.split(" ", 1)[0]
    if kind is None:
        fail("the divergence entry names %s, which is no code of the registers of ref or clingo" % ecode)
    if kind == "fallback":
        fail("the divergence entry names %s, a fallback code (the bottom arm of a dispatch): a fallback code is never what a witness rests on — give the form a named arm with a code of its own" % ecode)
    if {"refusal": "REFUSAL", "limit": "LIMIT"}.get(kind) != category:
        fail("the divergence entry %s|%s says %s, but %s is a code of the kind %s in its register" % (key, executor, category, ecode, kind))
    if not seen:
        fail("%s is listed as failing for a reason of its own (%s), but reports no code: every refusal that stands in the list carries one" % (executor, ecode))
    if pin_code in seen:
        fail("stale divergence — %s now reports the case's code %s; drop the entry (%s)" % (executor, pin_code, reason))
    if ecode not in seen:
        fail("%s is listed as reporting %s, but reports %s — a divergence entry names the code the executor reports (%s)" % (executor, ecode, ", ".join(seen), reason))
    print("DIV")
    sys.exit(0)

if pin_code not in seen:
    note = ""
    if seen and all(kinds.get(c) == "fallback" for c in seen):
        note = " (a fallback code, the bottom arm of a dispatch, is no witness of any kind: give the form a named arm)"
    fail("%s reports %s, but the case pins %s (expectedFault.code). Correct the executor, or list it in the divergence list with the code of its own refusal and its reason%s"
          % (executor, ", ".join(seen) or "no code", pin_code, note))
emitted = [p for c, p in observed if c == pin_code]
best = None
for params in emitted:
    miss = [(k, v, params.get(k)) for k, v in pin_params.items() if params.get(k) != v]
    if not miss:
        best = params
        break
    best = best or params
    first_miss = miss
if not any(all(p.get(k) == v for k, v in pin_params.items()) for p in emitted):
    k, v, got = first_miss[0]
    fail("%s emits %s for %s %s, but the case pins %s %s: the pinned parameters must be among the emitted ones (a parameter this executor cannot name is left out of the pin)"
          % (executor, json.dumps(best, ensure_ascii=False), pin_code, "without that key" if got is None else "with another value", k, json.dumps(v, ensure_ascii=False)))
print("OK 1 %d" % (len(pin_params) or 1))
PYEOF

status=0
n_cases=0
list=$(mktemp) || exit 2
find corpus/red -name 'case*.json' 2>/dev/null | sort > "$list"
# the params file the python comparison reads: the parameter column of every row of the table of fault kinds
# (`PARAMS EF-### a,b`), written by the table block below; the loop needs it first, so the table is parsed here and
# the strict row checks stay with the block below
params_file=$(mktemp) || exit 2
trap 'rm -f "$list" "$cmp" "$pairs" "$params_file"' EXIT
python3 -I - spec/RUNTIME-SEMANTICS.md > "$params_file" <<'PYEOF'
import re, sys
for line in open(sys.argv[1], encoding="utf-8").read().split("\n"):
    m = re.match(r"^\| (EF-[0-9]{3}) \| [^|]+ \| [^|]+ \| (—|`[^|]*?`) \| [^|]* \| [^|]* \|$", line)
    if m:
        print("PARAMS %s %s" % (m.group(1), "" if m.group(2) == "—" else ",".join(re.findall(r"`([^`]+)`", m.group(2)))))
PYEOF
if ! grep -q '^PARAMS ' "$params_file"; then
    echo 'FAIL: the parameter column of the table of fault kinds (RUNTIME-SEMANTICS ch. 5.7b) reads no row — the parameters cannot be checked' >&2
    exit 1
fi

# One code check: <case key> <executor> <runner output> <pinned code> <pinned params as JSON>.
# Returns 0 when the executor reports the case's code with the pinned parameters among its own, or when it is listed in
# the divergence list and reports the code of that entry; counts into n_red_codes, n_red_params and n_div.
code_check() {
    ck_key=$1; ck_exec=$2
    ck_res=$(printf '%s\n' "$3" | python3 -I "$cmp" "$ck_exec" "$ck_key" "$4" "$5" "$divergence" "$params_file" ref/codes.tsv backends/clingo/codes.tsv) || return 1
    if [ "$ck_res" = DIV ]; then
        printf '%s|%s\n' "$ck_key" "$ck_exec" >> "$div_used"
        n_div=$((n_div + 1))
    else
        set -- $ck_res
        # the Drools leg counts its own: it may be skipped, and the floors of the two OCaml executors must not move with it
        if [ "$ck_exec" = drools ]; then
            n_drools_params=$((n_drools_params + $3))
        else
            n_red_codes=$((n_red_codes + $2))
            n_red_params=$((n_red_params + $3))
        fi
    fi
    return 0
}

while IFS= read -r case; do
    n_cases=$((n_cases + 1))
    ir=$(dirname "$case")/ir.json
    if [ ! -f "$ir" ]; then
        echo "FAIL: $case has no sibling ir.json" >&2
        status=1
        continue
    fi
    key=${case#corpus/red/}
    key=${key%.json}
    out=$("$REF" "$ir" "$case" 2>&1)
    # the case's own pin, as the runner's case reader decoded it (never a shell scrape: a JSON string
    # may spell any character as an escape)
    pin=$(printf '%s\n' "$out" | awk '$1 == "CODE-PINNED" { print $2; exit }')
    pinparams=$(printf '%s\n' "$out" | awk '$1 == "CODE-PINNED" { $1 = ""; $2 = ""; sub(/^  /, ""); print; exit }')
    if [ -z "$pin" ] || ! printf '%s\n' "$out" | grep -q '^CODE '; then
        printf 'FAIL: %s — ref did not observe the expected fault:\n%s\n' "$case" "$out" >&2
        status=1
        continue
    fi
    case "$pin" in
        EF-[0-9][0-9][0-9]) ;;
        *)
            printf 'FAIL: %s pins the code %s — expectedFault.code is the EF code of a fault kind (RUNTIME-SEMANTICS ch. 5.7b), never the code of an executor (the fence on codes)\n' "$case" "$pin" >&2
            status=1
            continue ;;
    esac
    printf '%s|%s|%s\n' "$key" "$pin" "$pinparams" >> "$pinned_map"
    code_check "$key" ref "$out" "$pin" "$pinparams" || status=1
    out=$("$CLINGO" "$ir" "$case" 2>&1)
    if ! printf '%s\n' "$out" | grep -q '^CODE '; then
        printf 'FAIL: %s — clingo did not observe the expected fault:\n%s\n' "$case" "$out" >&2
        status=1
        continue
    fi
    code_check "$key" clingo "$out" "$pin" "$pinparams" || status=1
done < "$list"

if [ "$n_cases" -eq 0 ]; then
    echo 'FAIL: no case files under corpus/red — the red fence is empty, never silently green' >&2
    exit 1
fi
if [ "$n_cases" -lt "$EXPECTED_MIN_RED_CASES" ]; then
    echo "FAIL: $n_cases red case(s) replayed, but EXPECTED_MIN_RED_CASES pins at least $EXPECTED_MIN_RED_CASES — the red fence shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi
if [ "$n_red_codes" -lt "$EXPECTED_MIN_RED_CODES" ]; then
    echo "FAIL: $n_red_codes ref and clingo code check(s) reported the case's code, but EXPECTED_MIN_RED_CODES pins at least $EXPECTED_MIN_RED_CODES — the code checks shrank (a deliberate removal lowers the floor in the same commit)" >&2
    status=1
fi
if [ "$n_red_params" -lt "$EXPECTED_MIN_RED_PARAMS" ]; then
    echo "FAIL: ref and clingo confirmed $n_red_params pinned parameter(s) (an empty pin counts once per check), but EXPECTED_MIN_RED_PARAMS pins at least $EXPECTED_MIN_RED_PARAMS — the parameter checks shrank (a deliberate removal lowers the floor in the same commit)" >&2
    status=1
fi

# the pairs: two cases of one document that differ in a parameter only must pin different params
while IFS='|' read -r pa pb preason; do
    [ -z "$pa" ] && continue
    ja=$(awk -F'|' -v c="$pa" '$1 == c { i = index($0, "|"); r = substr($0, i + 1); j = index(r, "|"); print substr(r, j + 1); exit }' "$pinned_map")
    jb=$(awk -F'|' -v c="$pb" '$1 == c { i = index($0, "|"); r = substr($0, i + 1); j = index(r, "|"); print substr(r, j + 1); exit }' "$pinned_map")
    if [ -z "$ja" ] || [ -z "$jb" ]; then
        echo "FAIL: stale pair — $pa and $pb are listed as a pair (parameters only tell them apart), but one of them is no red case any more" >&2
        status=1
    elif [ "$ja" = "$jb" ]; then
        printf 'FAIL: %s and %s pin the same params %s: the pair differs in a parameter only (%s), and a code alone does not tell them apart\n' "$pa" "$pb" "$ja" "$preason" >&2
        status=1
    fi
done < "$pairs"

# --- the fault-kind table (RUNTIME-SEMANTICS ch. 5.7b), read strictly (card meldingscodes) --------
# A sidecar registry would duplicate the normative table, so this gate parses the table itself — the
# first gate to parse a spec table. The grammar is strict and a line it cannot read FAILS, never
# passes as prose: every line of the document that starts with "| EF-" is a row, must stand inside
# ch. 5.7b, and must read `| EF-### | kind | channel[; decision] | — or `name`, … | spec home |
# `corpus/red|golden/<dir>`, … |`; codes are unique and ascending. The python side checks what needs
# decoding — a golden witness, whose `inconsistent` expectation must carry the code — and prints the
# rows and the red witnesses; the shell side holds a red witness to the codes the runners decoded
# above. So a stale row is a gate failure, never a spec lie. The python side also reads every golden case
# (card channel-fact-codes, 2026-10-19): a code an `inconsistent` expectation pins names a §9.5 kind, so it must
# be a row on the §9.5 channel, and their number is floored.
table=$(mktemp) || exit 2
if ! python3 - spec/RUNTIME-SEMANTICS.md > "$table" <<'PY'
import glob, json, os, re, sys
path = sys.argv[1]
lines = open(path, encoding="utf-8").read().split("\n")
head = "### 5.7b Fault kinds and their codes"
if head not in lines:
    sys.stderr.write("FAIL: %s has no section '%s' — the fault-kind table is gone\n" % (path, head))
    sys.exit(1)
start = lines.index(head)
end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith("## ") or lines[i].startswith("### ")), len(lines))
WIT = r"`corpus/(?:red|golden)/[a-z0-9][a-z0-9-]*`"
ROW = re.compile(r"^\| (EF-[0-9]{3}) \| ([^|]+?) \| (case input|fault|declined|§9\.5)(; [^|]+?)? \| "
                 r"(—|`[a-z][A-Za-z]*`(?:, `[a-z][A-Za-z]*`)*) \| ([^|]+?) \| (" + WIT + r"(?:, " + WIT + r")*) \|$")
bad, last, rows, channel = 0, None, 0, {}
for n, line in enumerate(lines, 1):
    if not line.startswith("| EF-"):
        continue
    if not (start < n - 1 < end):
        sys.stderr.write("FAIL: %s line %d is a fault-kind row outside ch. 5.7b: %s\n" % (path, n, line[:120]))
        bad += 1
        continue
    m = ROW.match(line)
    if not m:
        sys.stderr.write("FAIL: %s line %d does not read as a row of the fault-kind table (code | kind | channel | parameters | spec home | witnesses): %s\n" % (path, n, line[:160]))
        bad += 1
        continue
    code = m.group(1)
    if last is not None and code <= last:
        sys.stderr.write("FAIL: %s line %d: %s follows %s — codes are unique and ascending\n" % (path, n, code, last))
        bad += 1
    last = code
    rows += 1
    channel[code] = m.group(3)
    print("ROW %s" % code)
    for zone, d in re.findall(r"`corpus/(red|golden)/([a-z0-9][a-z0-9-]*)`", m.group(7)):
        root = "corpus/%s/%s" % (zone, d)
        if not os.path.isdir(root):
            sys.stderr.write("FAIL: %s names the witness %s, which does not exist\n" % (code, root))
            bad += 1
        elif zone == "red":
            print("WITNESS %s %s" % (code, d))
        else:
            hit = False
            for f in sorted(glob.glob(root + "/case*.json")):
                for e in json.load(open(f, encoding="utf-8")).get("expected", []):
                    if isinstance(e, dict) and e.get("kind") == "inconsistent" and e.get("code") == code:
                        hit = True
            if not hit:
                sys.stderr.write("FAIL: %s names the golden witness %s, but no case there carries an inconsistent expectation with that code\n" % (code, root))
                bad += 1
print("ROWS %d" % rows)
golden_codes = 0
for f in sorted(glob.glob("corpus/golden/**/case*.json", recursive=True)):
    for e in json.load(open(f, encoding="utf-8")).get("expected", []):
        if not (isinstance(e, dict) and e.get("kind") == "inconsistent" and "code" in e):
            continue
        golden_codes += 1
        c = e["code"]
        if c not in channel:
            sys.stderr.write("FAIL: %s pins the code %s on an inconsistent expectation, which is no row of the fault-kind table (RUNTIME-SEMANTICS ch. 5.7b)\n" % (f, c))
            bad += 1
        elif channel[c] != "§9.5":
            sys.stderr.write("FAIL: %s pins %s on an inconsistent expectation, but its row is on the channel '%s': a channel fact carries only the codes of §9.5 kinds (ch. 5.6)\n" % (f, c, channel[c]))
            bad += 1
print("GOLDEN-CODES %d" % golden_codes)
sys.exit(1 if bad else 0)
PY
then
    status=1
fi
n_ef=$(awk '$1 == "ROWS" { print $2 }' "$table")
n_ef=${n_ef:-0}
if [ "$n_ef" -lt "$EXPECTED_MIN_EF_CODES" ]; then
    echo "FAIL: the fault-kind table of RUNTIME-SEMANTICS ch. 5.7b has $n_ef row(s), but EXPECTED_MIN_EF_CODES pins at least $EXPECTED_MIN_EF_CODES (a retired code keeps its row, marked retired)" >&2
    status=1
fi
n_golden_codes=$(awk '$1 == "GOLDEN-CODES" { print $2 }' "$table")
n_golden_codes=${n_golden_codes:-0}
if [ "$n_golden_codes" -lt "$EXPECTED_MIN_GOLDEN_CODES" ]; then
    echo "FAIL: $n_golden_codes golden inconsistent expectation(s) pin a code, but EXPECTED_MIN_GOLDEN_CODES pins at least $EXPECTED_MIN_GOLDEN_CODES — the §9.5 code pins shrank (a deliberate removal lowers the floor in the same commit)" >&2
    status=1
fi
# a red witness pins its row's code in at least one of its cases
while read -r tok code dir; do
    [ "$tok" = "WITNESS" ] || continue
    if ! grep -q "^$dir/[^|]*|$code|" "$pinned_map"; then
        echo "FAIL: $code names the red witness corpus/red/$dir, but no case there pins expectedFault.code $code" >&2
        status=1
    fi
done < "$table"
# every pinned code is a row
for code in $(cut -d'|' -f2 "$pinned_map" | sort -u); do
    if ! grep -qx "ROW $code" "$table"; then
        echo "FAIL: a red case pins $code, which is no row of the fault-kind table (RUNTIME-SEMANTICS ch. 5.7b)" >&2
        status=1
    fi
done
rm -f "$table"

# --- the fence on codes (card fragment-cleanup, 2026-10-19; it replaces the bottom-sentence fence, 2026-09-19) -----
# Both executors match on node KIND and end those matches in a fall-through arm. The refusal of such an arm, a BOTTOM
# SENTENCE, is invariant over everything the arm catches: it describes the position and what the backend does carry
# there ("expression kind not translated", "a list source here is an own list-typed attribute, a list parameter, a
# MapList or a FilterList", "a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape") and never the
# construct actually rejected. A red case that pins one witnesses nothing of its own: every other uncarried form at that
# position satisfies the same pin, and where the sentence also claims that no corpus case reaches it, the pin makes the
# claim false. That happened (sum-fallback-always-evaluated pinned "expression kind not translated" until 2026-09-19)
# and only a reader's eye caught it, so a list of 36 such sentences and a fence that normalised the sources to find
# them stood here, with a mechanism to decode the fragments of the pins (`ref --bottom-sentence`) because a JSON string
# may spell any character as an escape.
#
# A pin is now a fault kind and its parameters, never text, so a pin cannot cite a sentence at all; what the fence
# still has to stop is a case resting on a bottom arm's CODE. The bottom arms carry codes of the kind `fallback` in the
# registers of the executors (ref/codes.tsv, backends/clingo/codes.tsv), and this gate holds them out of every place that
# says what a witness proves: `expectedFault.code` is an EF code (the schema's pattern, and the check in the loop above),
# the divergence list may not name a `fallback` code (the check of the comparison above), and neither the exclusion list
# of the Drools leg nor gate 12's EXPECTED_CHANNEL may name a `fallback` code of the Drools register (the leg below). A
# fault in a red case of which an executor reports only a `fallback` code is a code other than the pinned one, and fails
# as such. The cure for a bottom arm is the one the old fence named, a named arm for the form with a code of its own,
# never another wording of the bottom; the carrier check (scripts/check-exec-codes.sh) holds each register to its source
# in both directions, as the carrier fence held the sentence list.
bash scripts/check-exec-codes.sh || status=1

# --- the Drools leg (decision V3-b, 2026-08-27) -----------------------------------------------
# The Drools backend CONSUMES the uchar IR (loads ir.json off the wire). For each red case it
# either reports a FAULT — the status line this leg reads the code and the parameters of — or it does not
# carry the red-path form (an EXCLUDE from the loader, a transpiler or the case reader, or a NOFAULT
# where the backend does not enforce the constraint and runs to a model). A not-carried form is listed
# below with a reason; every other case MUST FAULT with the pinned code and the pinned parameters among
# the ones the status line carries (`FAULT EF-016 {"rules":"…"}: <report>`, one `<code> <json>` per code, in
# report order). Until the card fragment-cleanup (2026-10-19) a case carried a `drools` fragment of the
# backend's own sentence, which told this leg whether the backend carried the case; the exclusion list
# says it now, and a case that is neither listed nor FAULTs fails the gate (completeness). An EXCLUDE entry also pins the register code of the refusal (`name|EXCLUDE DROOLS-014 <reason>`, card
# drools-refusal-codes, 2026-10-19), which the status line carries at its head (`EXCLUDE DROOLS-014:
# <sentence>`); the leg compares the two, so a refusal that moves to another code fails by case name
# with both codes. A NOFAULT entry refuses nothing and carries no code. The
# exclusion list is stale-guarded: a listed case that now FAULTS, or is gone, fails the gate. Neither
# this list nor gate 12's EXPECTED_CHANNEL may name a DROOLS code of the kind FALLBACK (the fence on
# codes above).
#
# HOW THE RED RUN CLASSIFIES: by exception TYPE, never by phase, and so since the leg landed
# (e1f779aa, 2026-08-27). A refusal of a form the backend does not carry — a loader refusal, or an
# `Excluded` from the build or from the case reader — is EXCLUDE; an I/O failure, or any other
# exception while the IR is loaded and the KieBase built, is ERROR; ANY other runtime exception
# after the build is FAULT — at fire, and equally while the case is read, a crash included; and a
# run without a fixpoint is a FAULT that raises no exception: on the exec-model branch this leg
# drives since the card tms-omschakeling (2026-10-10), "creation budget exhausted …" at the
# creation budget and "geen fixpunt: het vuurplafond van … vuringen is bereikt, …" at the fire
# cap; on the destructive lowering before it, "geen fixpunt (kringverwijzing?)" at its cap of
# 100 000. So FAULT does not separate a fault at fire from a refusal
# of the case, and a case defect lands on either side: four listed cases get their EXCLUDE from
# the case reader after the build (measured 2026-09-25: ambiguous-case-role-key,
# unknown-role-target-label, uniqueness-temporal-key-scalar, ever-unique-temporal-key-scalar — six when measured
# 2026-09-23, until the card ref-klipvouw-nul moved two to corpus/negative), and on the
# vensterheffing document (measured 2026-09-23) an
# undeclared attribute gives "EXCLUDE case sets undeclared attribute", while meldwaarde written as
# {"boolean": true} gives "FAULT IllegalArgumentException: argument type mismatch" and an
# undeclared characteristic gives "FAULT RuntimeException: kon setter setVerzonnen(boolean) niet
# aanroepen …". A crash after the build is a FAULT without a code: such a case cannot be pinned, and
# the leg fails it as a case that reports no code.
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
    # When the leg runs, the jar is taken FRESH OR REBUILT (scripts/lib/drools-jar.sh, 2026-09-23):
    # rebuilt when it is absent or older than any path it is built from, and exactly one shaded jar
    # is demanded. The lens-proven stale-jar flank — a build-only-when-absent jar can silently
    # predate the source under review — stays closed, because the test is "older than the source",
    # not "absent". From 2026-08-30 until then the leg rebuilt the jar unconditionally with `mvn
    # package`, the backend's JUnit suite included; that suite is gate 12's obligation (it runs it
    # with `mvn -o test` on the same target/), so it deliberately no longer runs here. legfail is
    # leg-LOCAL so the leg always leaves a verdict of its own, regardless of earlier red-case failures
    # elsewhere in the gate (lens finding: keying on the global status let the leg evaporate without
    # one).
    legfail=0
    . scripts/lib/drools-jar.sh
    if ! command -v mvn >/dev/null 2>&1; then
        echo 'check-red: FAIL — Drools leg: java present but mvn missing; the leg takes the jar fresh or rebuilt, and only mvn can rebuild a stale one (stale-jar guard)' >&2
        status=1; legfail=1
        JAR=''
    elif ! JAR=$(ensure_drools_jar); then
        echo 'check-red: FAIL — Drools leg: no fresh shaded jar (the drools-jar line above says why)' >&2
        status=1; legfail=1
        JAR=''
    fi
    if [ -z "$JAR" ]; then
        # every empty-JAR path above set legfail and printed its own verdict; this arm only guards
        # against a future edit that forgets to — belt and braces, never the primary reporter
        [ $legfail -eq 1 ] || { echo 'check-red: FAIL — Drools leg: no shaded jar and no verdict printed (script defect)' >&2; status=1; }
    else
        MAIN=dev.uchar.regelspraak.cases.DroolsRedMain
        excl=$(mktemp) || exit 2
        # <red-case-dir>|<reason the v0 Drools backend does not fault-pin this red form>
        cat > "$excl" <<'EOF'
filled-count-temporal-set-source|EXCLUDE DROOLS-210 measured 2026-10-10 (card tms-omschakeling): the exec-model branch this leg drives, which lowers with truth maintenance since the switch-over, refuses the reaching rule when it builds the KieBase, "transpiler SKIP: regel: aantal gewogen vondsten — een Aggregation AANTAL_GEVULDE over het tijdsafhankelijke attribuut 'gewicht' in een waarde (…): zij heeft geen moment waarop zij een tijdlijn rangschikt, telt of toetst". The refusal is the SAME judgement ref and clingo pin: a count has no defined MOMENT at which to ask a timeline whether it is filled (RUNTIME-SEMANTICS ch. 3.4c), so all three executors decline and only the two that can carry a fragment do. Measured first 2026-09-20 on the destructive lowering, whose exec branch reported "regel (aggregatie buiten Phase-C scope)" and whose DRL-text branch its marker. Listed rather than fragment-pinned because the transpiler raises its refusal as an Excluded, which the red run reports as EXCLUDE, and this leg pins a fragment only on a FAULT; the day ch. 3.4c supplies a moment rule, this entry goes stale and the gate says so
aanvoergeld-parameter-unbound|NOFAULT measured 2026-09-23 (card clingo-parameter-failfast): both cases run to a model. The backend answers a parameter the case does not bind the way the ASP backend did before that card — case1 aanvoergeld 310.00 / 87.40 / 0.00, the absent tarief coalesced to 0; case2 aanvoerplichtig absent on J1 and J3 — where ref and clingo fault fail-fast in one sentence (ch. 5.7). Listed rather than fragment-pinned because this leg does not fault here at all; the day the backend binds parameters fail-fast, this entry goes stale and the gate says so
ambiguous-backlink|EXCLUDE DROOLS-080 measured 2026-10-10 (card tms-omschakeling): since the switch-over the exec-model branch this leg drives lowers with truth maintenance, and it refuses the creation when it builds the KieBase, "transpiler SKIP: objectcreatie: insigne voor een jutter — more than one fact type relates "Jutter" to "Insigne"; the created object's back-link is ambiguous (ch. 5.5); deze tak draagt deze vorm nog niet". It names the ambiguity ref and clingo fault on, but as a form the branch does not carry, an EXCLUDE, and this leg pins a fragment only on a FAULT. Until the switch-over the destructive lowering faulted at fire in the same words and the case pinned them
ambiguous-case-role-key|EXCLUDE DROOLS-050 measured 2026-09-22 (P1 port): the case-input arm refuses at the role key `ligger` on the bean-field limit — two same-named roles contend for one field on Schuit. Same phase as ref's and clingo's load fault, different reason: they refuse the ambiguity itself, this backend a field it cannot carry; it raises that as a form it does not carry, so the refusal is an EXCLUDE
closed-nonexact-division|EXCLUDE DROOLS-008 the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
closed-runtime-nonexact-division|EXCLUDE DROOLS-008 the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
day-kind-parameter-unbound|EXCLUDE DROOLS-179 measured 2026-10-10 (card tms-omschakeling): the exec-model branch this leg drives, which lowers with truth maintenance since the switch-over, refuses the reaching rule when it builds the KieBase, "transpiler SKIP: regel: streefdatum van een havenmeester — een duur in dagsoorten (…): de definitie van de dagsoort 'havendag' leest de parameter 'openingsdatum', die deze tak in een dagsoortdefinitie niet leest (WF-031 laat haar toe)", so the unbound parameter is never reached. The refusal is about the operand FORM and not about the binding, which is why it is listed rather than fragment-pinned. Measured first 2026-09-19 on the destructive lowering, whose day-kind predicate compiler (kern.DayKinds) carried no PARAMETER operand at all and SKIPped the rule (waarde buiten scope)
day-kind-reference-other-day|EXCLUDE DROOLS-180 measured 2026-10-10 (card tms-omschakeling): the exec-model branch this leg drives, which lowers with truth maintenance since the switch-over, refuses the reaching rule when it builds the KieBase, "transpiler SKIP: regel: uiterste bergingsdatum van een bergingsploeg — een duur in dagsoorten (…): de definitie van de dagsoort 'springtijdag' toetst de dagsoort 'ankerdag' op een andere dag dan de gebonden dag (…), een vorm die geen executor uitvoert (RUNTIME-SEMANTICS ch. 3.8)", and fire is never reached. All three executors refuse the form; only the two that can carry a fragment pin one. Measured first 2026-09-19 on the destructive lowering, whose day-kind predicate compiler (kern.DayKinds) left the §9.8 composition UNCOMPILED for an operand other than the bound day and SKIPped the rule (waarde buiten scope)
decimal-nonterminating|NOFAULT the v0 backend has no §6.1.2 write-precision guard — a non-terminating quotient collapses to DECIMAL64 (Breuk.naarDecimal) instead of faulting; it runs to a model
input-overprecision|NOFAULT the v0 case runner does not enforce declared attribute input precision — it accepts the over-precise value and runs to a model
input-overprecision-param|NOFAULT the v0 case runner does not enforce declared parameter input precision — it accepts the over-precise value and runs to a model
list-element-overprecision|EXCLUDE DROOLS-009 the v0 loader refuses the model's record datatype (StructType) before any list-element precision is reached — the record/struct form is not carried
time-window-timeline-characteristic|EXCLUDE DROOLS-102 measured 2026-09-20, reason updated 2026-09-23 and 2026-10-10 (card tms-omschakeling): a period condition on a TEMPORAL target is ch. 3.10's clip, not the evaluation-date gate. The clip is built for ATTRIBUTE targets in ref and clingo since 2026-09-23 (golden jutterswet-pachtvenster, which both branches of this backend decline by name); this document's target is a CHARACTERISTIC, whose timeline neither OCaml executor writes (ch. 3.11), so all four refuse. The exec-model branch this leg drives, which lowers with truth maintenance since the switch-over, refuses one step earlier, at the target: "transpiler SKIP: regel: berging van een vondst — het tijdsafhankelijke kenmerk 'geborgen' van Vondst als doel: deze tak draagt deze vorm nog niet; geen golden met cases schrijft er een (RUNTIME-SEMANTICS ch. 3.11)". The destructive lowering refused at the clip (exec "periodevergelijking op tijdsafhankelijk doel — §8.4.3 clip", DRL-text "TODO periodevergelijking op een tijdsafhankelijk doel (§8.4.3)"), while the ASP backend once PLAYED the scalar gate here because its target test read only the ATTRIBUTE granularity table. Listed rather than fragment-pinned: the transpiler raises its refusal as an Excluded, which the red run reports as EXCLUDE, and this leg pins a fragment only on a FAULT
sum-fallback-always-evaluated|EXCLUDE DROOLS-207 the reaching rule (vangstsom van een vangst) does not lower — a transpiler SKIP, since the card tms-omschakeling (2026-10-10) in the words of the exec-model branch this leg drives, "een Aggregation SOM zonder elementtype (…)", before it in those of the destructive lowering, "aggregatie buiten Phase-C scope"; fire is never reached, so the sum's stand-in is not Drools-observable here
struct-field-overprecision|EXCLUDE DROOLS-009 the v0 loader refuses the model's record datatype (StructType) — the record/struct form is not carried
to-one-second-filler|EXCLUDE DROOLS-161 measured 2026-10-10 (card tms-omschakeling): the exec-model branch this leg drives, which lowers with truth maintenance since the switch-over, refuses a fact creation of the document when it builds the KieBase, "transpiler SKIP: feitcreatie: berging door de kustgemeente — het pad van de feitcreatie 'berging door de kustgemeente': de stap over 'aanmelding' heeft twee rollen naar veel, waarvoor het beanmodel geen veld heeft (§13.3.9 opslagrichting)", so the second-filler violation is never reached. Until the switch-over the destructive lowering refused the reaching rule instead (aantal bergende gemeenten van een vondst, "aggregatie over de enkelvoudige elementrol 'bergende gemeente' wiens zusterrol 'geborgen vondst' meervoudig is …"; measured 2026-09-23, card rood-heeltijdvak-ongenegeerd). The transpiler raises its refusal as an Excluded, which the red run reports as EXCLUDE, and this leg pins a fragment only on a FAULT
uniqueness-temporal-key-scalar|EXCLUDE DROOLS-051 the case sets a temporal attribute (kavelnummer) as a scalar while carrying NO asOfDate — the bucket convention (corpus/golden/README.md) gives that scalar no evaluation date to sit in, so the input is not carried and the ever-unique key is never reached
uniqueness-two-temporal-keys|EXCLUDE DROOLS-127 measured 2026-10-10 (card tms-omschakeling): since the switch-over the exec-model branch this leg drives lowers with truth maintenance, and it refuses the uniqueness when it builds the KieBase, "transpiler SKIP: uniciteit: dubbel temporeel enkelvoudig — een uniciteitssleutel 'kavelnummer' van Strandvondst op een tijdlijn naast een tweede: een sleutel heeft hoogstens één tijdsafhankelijk deel, …". That is the judgement ref and clingo fault on (at most one temporal key component, ch. 5.9), but as a form the branch does not carry, an EXCLUDE, and this leg pins a fragment only on a FAULT. Until the switch-over the destructive lowering faulted on the key list in ref's words and the case pinned them
unknown-role-target-label|EXCLUDE DROOLS-048 measured 2026-09-19: the backend's own loader refuses the same input and names the same dangling label (role 'liggers' names unknown label S9), but the backend raises it as a form it does not carry, an EXCLUDE, and this leg pins a fragment only on a FAULT. The agreement is real and recorded here rather than pinned: all three executors reject this case, and only the two that can carry a fragment do
ever-unique-temporal-key-scalar|EXCLUDE DROOLS-051 the case sets a temporal attribute (vondstnummer) as a scalar while carrying NO asOfDate — the bucket convention (corpus/golden/README.md) gives that scalar no evaluation date to sit in, so the input is not carried and the ever-unique key is never reached
ever-unique-two-temporal-keys|EXCLUDE DROOLS-127 measured 2026-10-10 (card tms-omschakeling): the refusal of uniqueness-two-temporal-keys for the uniqueness over more sources, "transpiler SKIP: uniciteit-unie: dubbel temporeel — een uniciteitssleutel 'volgnummer' van Strandvondst op een tijdlijn naast een tweede: een sleutel heeft hoogstens één tijdsafhankelijk deel, …", an EXCLUDE where ref and clingo fault; until the switch-over the destructive lowering faulted in ref's words and the case pinned them
vintage-uncovered-date|EXCLUDE DROOLS-023 the v0 loader refuses the TemporalRead expression node (the vintage read) — the temporal-read form is not carried
EOF
        # the fence on codes: neither this list nor gate 12's EXPECTED_CHANNEL names a DROOLS code of the kind FALLBACK
        # (the bottom arm of a dispatch), as a code a witness rests on; read from the register itself, in the one form
        # scripts/check-drools-codes.sh reads it by
        if ! python3 -I - "$excl" scripts/check-drools.sh backends/drools/src/main/java/dev/uchar/regelspraak/refusal/DroolsCode.java <<'PYEOF'
import re, sys
excl, channel, register = sys.argv[1:4]
kinds = dict(re.findall(r'\("(DROOLS-[0-9]{3})", Kind\.([A-Z]+),', open(register, encoding="utf-8").read()))
if not kinds:
    sys.exit("FAIL: the register %s holds no code in the form the fence reads — the fence would hold nothing" % register)
bad, seen = 0, 0
for path, pattern in ((excl, r"^[^|]+\|EXCLUDE (DROOLS-[0-9]{3}) "), (channel, r'^\s*"[^"|]+\|EXCLUDE (DROOLS-[0-9]{3})\|')):
    for line in open(path, encoding="utf-8"):
        m = re.match(pattern, line)
        if not m:
            continue
        seen += 1
        if kinds.get(m.group(1)) is None:
            sys.stderr.write("FAIL: %s names %s, which the Drools register does not hold\n" % (path, m.group(1))); bad += 1
        elif kinds[m.group(1)] == "FALLBACK":
            sys.stderr.write("FAIL: %s names %s, a fallback code of the Drools register: the bottom arm of a dispatch is never what a witness rests on — give the form a named arm with a code of its own\n" % (path, m.group(1))); bad += 1
if seen == 0:
    sys.stderr.write("FAIL: the fence on codes read no EXCLUDE entry in the Drools exclusion list or in EXPECTED_CHANNEL — an empty scan is never silently green\n"); bad += 1
sys.exit(1 if bad else 0)
PYEOF
        then
            status=1
        fi
        d_fault=0
        d_excl=0
        d_excl_codes=0
        d_codes=0
        while IFS= read -r case; do
            name=$(basename "$(dirname "$case")")
            ir=$(dirname "$case")/ir.json
            reason=$(awk -F'|' -v n="$name" '$1==n {i=index($0,"|"); print substr($0,i+1); exit}' "$excl")
            # DroolsRedMain prints exactly one status line (FAULT|NOFAULT|EXCLUDE|ERROR <detail>) last on
            # stdout; the leg classifies on that STATUS TOKEN, never the bare exit code, so an ERROR (an I/O
            # failure, or a crash while the IR is loaded and the KieBase built) can never be swallowed as a
            # not-carried outcome and a listed reason's category is checked. A crash AFTER the build is a FAULT
            # (head of this leg): a listed case that shows one fails as stale, and a pin may not match it.
            out=$(java -cp "$JAR" "$MAIN" "$ir" "$case" 2>/dev/null | tail -1)
            tok=${out%% *}
            if [ -z "$reason" ]; then
                # a carried case MUST be a FAULT with the pinned code and parameters; any other token
                # (NOFAULT / EXCLUDE / ERROR) is a regression, never a pass
                case "$tok" in
                    FAULT)
                        d_fault=$((d_fault + 1))
                        key=${case#corpus/red/}
                        key=${key%.json}
                        pin=$(awk -F'|' -v c="$key" '$1 == c { print $2; exit }' "$pinned_map")
                        pinparams=$(awk -F'|' -v c="$key" '$1 == c { i = index($0, "|"); r = substr($0, i + 1); j = index(r, "|"); print substr(r, j + 1); exit }' "$pinned_map")
                        if [ -z "$pin" ]; then
                            printf 'FAIL: %s — no pinned code was decoded for the Drools code check (see the ref leg above)\n' "$name" >&2
                            status=1
                        elif code_check "$key" drools "$out" "$pin" "$pinparams"; then
                            d_codes=$((d_codes + 1))
                        else
                            status=1
                        fi ;;
                    *) printf 'FAIL: %s — Drools did not FAULT (the case is not in the exclusion list, so the backend must carry it), observed %s:\n%s\n' "$name" "$tok" "$out" >&2
                       status=1 ;;
                esac
            else
                # the listed reason's first word is its declared not-carried category; it is machine-checked
                # against the observed token, so a reason that drifts (EXCLUDE<->NOFAULT) or a case that now
                # FAULTs or ERRORs fails the gate — the stale-guard is no longer exit-code-blind.
                rcat=${reason%% *}
                case "$tok" in
                    FAULT)
                        printf 'FAIL: stale exclusion — %s is listed (%s) but Drools now FAULTS; drop the entry (the case is then pinned on Drools too):\n%s\n' "$name" "$reason" "$out" >&2
                        status=1 ;;
                    ERROR)
                        printf 'FAIL: %s is listed (%s) but Drools ERRORED — a backend crash is never a not-carried outcome; fix the crash or reclassify:\n%s\n' "$name" "$reason" "$out" >&2
                        status=1 ;;
                    EXCLUDE|NOFAULT)
                        if [ "$tok" != "$rcat" ]; then
                            printf 'FAIL: reason drift — %s is listed as %s but Drools now reports %s; correct the reason category to match the observed status:\n%s\n' "$name" "$rcat" "$tok" "$out" >&2
                            status=1
                        elif [ "$tok" = EXCLUDE ]; then
                            # an EXCLUDE entry pins the register code of the refusal (`EXCLUDE DROOLS-014 <reason>`)
                            # and the report carries it at its head (`EXCLUDE DROOLS-014: <sentence>`); the two must
                            # be the same code, so a refusal that moves to another code, or a report without one,
                            # fails by case name with both codes
                            rcode=$(printf '%s\n' "$reason" | sed -n 's/^EXCLUDE \(DROOLS-[0-9][0-9][0-9]\) .*/\1/p')
                            scode=$(printf '%s\n' "$out" | sed -n 's/^EXCLUDE \(DROOLS-[0-9][0-9][0-9]\): .*/\1/p')
                            if [ -z "$rcode" ]; then
                                printf 'FAIL: %s is listed as EXCLUDE without a register code (EXCLUDE DROOLS-### <reason>):\n%s\n' "$name" "$reason" >&2
                                status=1
                            elif [ "$rcode" != "$scode" ]; then
                                printf 'FAIL: code drift — %s is pinned %s but Drools reports %s:\n%s\n' "$name" "$rcode" "${scode:-no code}" "$out" >&2
                                status=1
                            else
                                d_excl=$((d_excl + 1)); d_excl_codes=$((d_excl_codes + 1))
                            fi
                        else
                            d_excl=$((d_excl + 1))
                        fi ;;
                    *)
                        printf 'FAIL: %s — unexpected Drools status token "%s" (expected FAULT/EXCLUDE/NOFAULT/ERROR):\n%s\n' "$name" "$tok" "$out" >&2
                        status=1 ;;
                esac
            fi
        done < "$list"
        # gone-guard: every exclusion entry must name an existing red case (a double-list is caught above)
        while IFS='|' read -r n _rest; do
            [ -z "$n" ] && continue
            if [ -z "$(find "corpus/red/$n" -maxdepth 1 -name 'case*.json' 2>/dev/null | head -1)" ]; then
                echo "FAIL: stale exclusion — '$n' is listed but no such red case exists" >&2
                status=1
            fi
        done < "$excl"
        rm -f "$excl"
        # at-least-one-fault discipline: the leg must anchor on a genuine backend fault (the smoke-test
        # canary of a working jar) — an all-not-carried run is never silently green.
        if [ "$d_fault" -eq 0 ]; then
            echo 'check-red: FAIL — Drools leg: no red case fault-pinned; at least one genuine backend fault must anchor the leg (an empty fault set is a failure, never silently green)' >&2
            status=1
        fi
        # monotone floor (maintainer decision 2026-09-09) on the EXCLUDE entries that pin a register code: a list that
        # quietly lost its code pins would keep passing on the channel alone. Measured 2026-10-19 (card
        # drools-refusal-codes): 19 cases, the 19 EXCLUDE entries of the list; 18 the same day, when the card
        # zero-division-channel moved closed-runtime-division-by-zero to the golden corpus.
        EXPECTED_MIN_DROOLS_EXCLUDE_CODES=18
        if [ "$d_excl_codes" -lt "$EXPECTED_MIN_DROOLS_EXCLUDE_CODES" ]; then
            printf 'check-red: FAIL — Drools leg: %s EXCLUDE cases compared on their DROOLS code, but EXPECTED_MIN_DROOLS_EXCLUDE_CODES pins at least %s — the list shrank or lost its code pins\n' "$d_excl_codes" "$EXPECTED_MIN_DROOLS_EXCLUDE_CODES" >&2
            status=1
        fi
        if [ "$n_drools_params" -lt "$EXPECTED_MIN_DROOLS_PARAMS" ]; then
            printf 'check-red: FAIL — Drools leg: %s pinned parameter(s) confirmed, but EXPECTED_MIN_DROOLS_PARAMS pins at least %s — the parameter checks of the leg shrank\n' "$n_drools_params" "$EXPECTED_MIN_DROOLS_PARAMS" >&2
            status=1
        fi
        if [ "$status" -eq 0 ]; then
            printf 'check-red: Drools leg PASS (%s fault-pinned, %s of them reporting the case'"'"'s code, %s pinned parameter checks confirmed, floor %s; %s not-carried with reason, %s of them EXCLUDE pinned by their DROOLS code)\n' "$d_fault" "$d_codes" "$n_drools_params" "$EXPECTED_MIN_DROOLS_PARAMS" "$d_excl" "$d_excl_codes"
        fi
        drools_ran=1
    fi
else
    echo 'check-red: Drools leg SKIPPED — needs java + python3 (and, once running, mvn: the leg takes the jar fresh or rebuilt). Not a blocker on a machine without java; the ref/clingo legs above are the CI blocker. NB hosted ubuntu runners DO carry java, so there the leg runs (and needs a warm ~/.m2).' >&2
fi

# the divergence list's gone-guard: every entry was consulted by a check, and names an executor that carries a
# fragment-free register (ref or clingo; the Drools backend lists its not-carried cases in its own exclusion list above)
while IFS='|' read -r c e ecode reason; do
    [ -z "$c" ] && continue
    case "$e" in
        ref|clingo) ;;
        *) echo "FAIL: the divergence entry $c names the executor '$e' (ref or clingo)" >&2; status=1; continue ;;
    esac
    case "$ecode" in
        REF-[0-9][0-9][0-9]|CLINGO-[0-9][0-9][0-9]) ;;
        *) echo "FAIL: the divergence entry $c|$e names '$ecode' as its code (a REF-### or CLINGO-### of the register of the executor)" >&2; status=1 ;;
    esac
    case "$reason" in
        "REFUSAL "*|"LIMIT "*) ;;
        *) echo "FAIL: the divergence entry $c|$e has no category REFUSAL or LIMIT as the first word of its reason" >&2; status=1 ;;
    esac
    if ! grep -qx "$c|$e" "$div_used"; then
        echo "FAIL: stale divergence — the entry $c|$e was consulted by no code check (its case is gone, or the executor makes no check on it)" >&2
        status=1
    fi
done < "$divergence"
rm -f "$divergence" "$div_used" "$pinned_map"

if [ "$status" -eq 0 ]; then
    printf 'check-red: %s golden inconsistent expectations pin a §9.5 code, each a row on that channel, floor %s\n' \
        "$n_golden_codes" "$EXPECTED_MIN_GOLDEN_CODES"
    printf 'check-red: PASS (%s red cases fault observably in ref and clingo, floor %s; %s ref and clingo code checks report the case'"'"'s code, floor %s, %s listed as divergence; %s pinned parameter checks confirmed, floor %s; %s fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor %s)\n' \
        "$n_cases" "$EXPECTED_MIN_RED_CASES" "$n_red_codes" "$EXPECTED_MIN_RED_CODES" "$n_div" "$n_red_params" "$EXPECTED_MIN_RED_PARAMS" "$n_ef" "$EXPECTED_MIN_EF_CODES"
fi
exit "$status"
