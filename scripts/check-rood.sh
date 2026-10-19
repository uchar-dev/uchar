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
# unsolved state among them: Unresolved surfaces, never vanishes.
#
#   1. ref/ and backends/ build; clingo must be available;
#   2. every corpus/rood/**/case*.json passes both runners in
#      expected-fault mode (exit 0);
#   3. an empty red set is a failure, never silently green;
#   4. no pinned fragment cites a BOTTOM SENTENCE of ref or clingo — the
#      refusal a node-kind match falls through to, which names no form
#      (the bottom-sentence fence, 2026-09-19);
#   5. every red case pins `expectedFault.code`, the EF code of its fault
#      kind (RUNTIME-SEMANTICS ch. 5.7b), and ref, clingo, and Drools where
#      the case pins a `drools` fragment, each REPORT that code — or the
#      executor stands, with its reason, in the divergence list below,
#      which fails when stale in either direction (card meldingscodes,
#      2026-10-18);
#   6. the fault-kind table of RUNTIME-SEMANTICS ch. 5.7b is read here,
#      strictly: every row parses, every witness it names exists and pins
#      the row's code, and every code a red case pins is a row.
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
EXPECTED_MIN_RED_CASES=26   # 2026-09-10: measured 26 (corpus/rood/**/case*.json)
# The code checks (card meldingscodes, 2026-10-18). RED_CODES counts the ref and clingo checks in
# which the executor reported the case's code: 50 cases × 2 executors minus the 7 entries of the
# divergence list, measured 93. EF_CODES counts the rows of the RUNTIME-SEMANTICS ch. 5.7b table,
# measured 24. The Drools leg's code checks get no floor of their own, for the reason its fault count
# has none (below): each is a case with a `drools` fragment, held by that pin and by the name-carrying
# exclusion list.
EXPECTED_MIN_RED_CODES=93   # 2026-10-18: measured 93 (ref 50 + clingo 43)
EXPECTED_MIN_EF_CODES=24    # 2026-10-18: measured 24 (RUNTIME-SEMANTICS ch. 5.7b)

if ! command -v python3 >/dev/null 2>&1; then
    echo 'FAIL: need python3 — the code leg reads the fault-kind table of RUNTIME-SEMANTICS ch. 5.7b and decodes the golden witnesses it names' >&2
    exit 2
fi

# --- the code divergence list (card meldingscodes, 2026-10-18) -----------------------------------
# A red case states the ONE fault kind the semantics demands (`expectedFault.code`); what an executor
# does instead is executor conduct, kept here beside the other conduct lists (the Drools leg's
# exclusion list below, gate 12's EXPECTED_CHANNEL), never in the case: the corpus does not move when
# an executor moves. An entry admits that the executor reports NO code on the case, because it fails
# for a reason of its own; it never admits ANOTHER EF code, which would be a disagreement about the
# kind, not conduct. The first word of the reason is its category: REFUSAL (a form the executor does
# not carry) or LIMIT (a limit of its own representation). FALLBACK (a bottom sentence, fenced above)
# and CONTRACT (an ill-formed document; a red document is verifier-clean) are not admitted. The
# entries carry no code of their own: the fragment pins keep covering the actual reason, and a code
# column joins when executor codes land (spec/proposals/MESSAGE-CODES.md §7, card 5); until then the
# category is checked against this vocabulary only.
#
# Stale-guarded both ways: an entry whose executor now reports a code — the case's or another — fails,
# and so does an entry that no check consulted (its case is gone, or its executor makes no check on
# it); an unlisted check whose executor reports no code, or another one, fails as well.
#
# <case, relative to corpus/rood, without .json>|<executor>|<CATEGORY> <reason>
divergence=$(mktemp) || exit 2
cat > "$divergence" <<'EOF'
begrensd-reads-bounds-in-order/case1|clingo|REFUSAL measured 2026-10-18: the backend refuses the Clamp whose minimum bound is not a ground literal, "a Clamp minimum bound that is not a ground literal could be empty; ref reads an empty bound as no bound, which the bare join cannot express", at translation, so the division by numeric zero that ref and Drools fault on (EF-005) is never reached
som-fallback-always-evaluated/case1|clingo|REFUSAL measured 2026-10-18: the backend does not translate a sum over listed operands, "a sum over listed operands is not translated: …", so the stand-in whose division by numeric zero ref faults on (EF-005) is never evaluated
dagsoortstap-buiten-bereik/case1|clingo|LIMIT measured 2026-10-18: the step's millisecond value 53375975860 "exceeds clingo's integer range (grounding would wrap silently)" at translation, where ref and Drools fault on the datum-tijd range of ch. 3.8 (EF-007)
datumtijdstap-buiten-bereik/case1-plus-uren|clingo|LIMIT measured 2026-10-18: the step's millisecond value 2000000000000 "exceeds clingo's integer range" at translation, where ref and Drools fault on the datum-tijd range of ch. 3.8 (EF-007)
datumtijdstap-buiten-bereik/case2-min-uren|clingo|LIMIT measured 2026-10-18: the step's millisecond value 2000000000000 "exceeds clingo's integer range" at translation, where ref and Drools fault on the datum-tijd range of ch. 3.8 (EF-007)
datumtijdstap-buiten-bereik/case3-plus-dagen|clingo|LIMIT measured 2026-10-18: the step's millisecond value 60000000000 "exceeds clingo's integer range" at translation, where ref and Drools fault on the datum-tijd range of ch. 3.8 (EF-007)
datumtijdstap-buiten-bereik/case4-telling-buiten-bereik|clingo|LIMIT measured 2026-10-18: the duration count 18446744073709551617 "exceeds clingo's integer range" at translation, where ref and Drools refuse the count itself as out of range (EF-008)
EOF
div_used=$(mktemp) || exit 2
pinned_map=$(mktemp) || exit 2
n_red_codes=0
n_div=0

# One code check: <case key> <executor> <observed codes, comma-separated, or -> <pinned code>.
# Returns 0 when the executor reports the case's code, or when it reports none and is listed;
# the caller counts the first kind (it alone has a code to count).
code_check() {
    ck_case=$1; ck_exec=$2; ck_seen=$3; ck_pin=$4
    ck_entry=$(awk -F'|' -v c="$ck_case" -v e="$ck_exec" '$1 == c && $2 == e { print $3; exit }' "$divergence")
    case ",$ck_seen," in
        *",$ck_pin,"*) ck_has=1 ;;
        *) ck_has=0 ;;
    esac
    if [ -n "$ck_entry" ]; then
        printf '%s|%s\n' "$ck_case" "$ck_exec" >> "$div_used"
        if [ "$ck_seen" = "-" ]; then
            n_div=$((n_div + 1))
            return 0
        fi
        if [ "$ck_has" -eq 1 ]; then
            printf 'FAIL: stale divergence — %s: %s now reports the case'"'"'s code %s; drop the entry (%s)\n' \
                "$ck_case" "$ck_exec" "$ck_pin" "$ck_entry" >&2
        else
            printf 'FAIL: %s: %s is listed as failing for a reason of its own, but reports the code %s where the case pins %s — another kind is a disagreement, not conduct (%s)\n' \
                "$ck_case" "$ck_exec" "$ck_seen" "$ck_pin" "$ck_entry" >&2
        fi
        return 1
    fi
    if [ "$ck_has" -eq 1 ]; then
        return 0
    fi
    printf 'FAIL: %s: %s reports the code %s, but the case pins %s (expectedFault.code). Correct the executor, or list it in the divergence list with its reason where it fails for a reason of its own\n' \
        "$ck_case" "$ck_exec" "$ck_seen" "$ck_pin" >&2
    return 1
}

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
    key=${case#corpus/rood/}
    key=${key%.json}
    if ! out=$("$REF" "$ir" "$case" 2>&1); then
        printf 'FAIL: %s — ref did not observe the expected fault:\n%s\n' "$case" "$out" >&2
        status=1
    fi
    # the case's own code, as the runner's case reader decoded it (never a shell scrape: a JSON string
    # may spell any character as an escape)
    pin=$(printf '%s\n' "$out" | awk '$1 == "CODE-PINNED" { print $2; exit }')
    seen=$(printf '%s\n' "$out" | awk '$1 == "CODE" { print $2; exit }')
    if [ -z "$pin" ] || [ -z "$seen" ]; then
        printf 'FAIL: %s — ref printed no CODE and CODE-PINNED line (runner/gate contract broken)\n' "$case" >&2
        status=1
    elif [ "$pin" = "-" ]; then
        printf 'FAIL: %s carries no expectedFault.code — every red case pins the EF code of its fault kind (RUNTIME-SEMANTICS ch. 5.7b)\n' "$case" >&2
        status=1
    else
        printf '%s|%s\n' "$key" "$pin" >> "$pinned_map"
        if code_check "$key" ref "$seen" "$pin"; then
            [ "$seen" = "-" ] || n_red_codes=$((n_red_codes + 1))
        else
            status=1
        fi
    fi
    if ! out=$("$CLINGO" "$ir" "$case" 2>&1); then
        printf 'FAIL: %s — clingo did not observe the expected fault:\n%s\n' "$case" "$out" >&2
        status=1
    fi
    seen=$(printf '%s\n' "$out" | awk '$1 == "CODE" { print $2; exit }')
    if [ -z "$seen" ]; then
        printf 'FAIL: %s — clingo printed no CODE line (runner/gate contract broken)\n' "$case" >&2
        status=1
    elif [ -n "$pin" ] && [ "$pin" != "-" ]; then
        if code_check "$key" clingo "$seen" "$pin"; then
            [ "$seen" = "-" ] || n_red_codes=$((n_red_codes + 1))
        else
            status=1
        fi
    fi
done < "$list"

if [ "$n_cases" -eq 0 ]; then
    echo 'FAIL: no case files under corpus/rood — the red fence is empty, never silently green' >&2
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

# --- the fault-kind table (RUNTIME-SEMANTICS ch. 5.7b), read strictly (card meldingscodes) --------
# A sidecar registry would duplicate the normative table, so this gate parses the table itself — the
# first gate to parse a spec table. The grammar is strict and a line it cannot read FAILS, never
# passes as prose: every line of the document that starts with "| EF-" is a row, must stand inside
# ch. 5.7b, and must read `| EF-### | kind | channel[; decision] | — or `name`, … | spec home |
# `corpus/rood|golden/<dir>`, … |`; codes are unique and ascending. The python side checks what needs
# decoding — a golden witness, whose `inconsistent` expectation must carry the code — and prints the
# rows and the red witnesses; the shell side holds a red witness to the codes the runners decoded
# above. So a stale row is a gate failure, never a spec lie.
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
WIT = r"`corpus/(?:rood|golden)/[a-z0-9][a-z0-9-]*`"
ROW = re.compile(r"^\| (EF-[0-9]{3}) \| ([^|]+?) \| (case input|fault|declined|§9\.5)(; [^|]+?)? \| "
                 r"(—|`[a-z][A-Za-z]*`(?:, `[a-z][A-Za-z]*`)*) \| ([^|]+?) \| (" + WIT + r"(?:, " + WIT + r")*) \|$")
bad, last, rows = 0, None, 0
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
    print("ROW %s" % code)
    for zone, d in re.findall(r"`corpus/(rood|golden)/([a-z0-9][a-z0-9-]*)`", m.group(7)):
        root = "corpus/%s/%s" % (zone, d)
        if not os.path.isdir(root):
            sys.stderr.write("FAIL: %s names the witness %s, which does not exist\n" % (code, root))
            bad += 1
        elif zone == "rood":
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
# a red witness pins its row's code in at least one of its cases
while read -r tok code dir; do
    [ "$tok" = "WITNESS" ] || continue
    if ! grep -q "^$dir/[^|]*|$code\$" "$pinned_map"; then
        echo "FAIL: $code names the red witness corpus/rood/$dir, but no case there pins expectedFault.code $code" >&2
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

# --- the bottom-sentence fence: a pin may not cite a bottom sentence (2026-09-19) ---------------------
# Both executors match on node KIND and end those matches in a fall-through arm. A BOTTOM SENTENCE
# is such an arm's refusal, and its defining property is that it is INVARIANT over everything the
# arm catches: it describes the position and what the backend does carry there — "expression kind
# not translated", "a list source here is an own list-typed attribute, a list parameter, a MapList
# or a FilterList", "a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape"
# — and never the construct actually rejected. A red case that pins one witnesses nothing of its
# own: every other uncarried form at that position satisfies the same fragment, so the witness
# stops witnessing its form, and where the sentence also claims no corpus case reaches it, the pin
# makes that claim live-false. Not hypothetical — som-fallback-always-evaluated pinned "expression
# kind not translated" until 2026-09-19, when the listed som finally got the named arm it deserved,
# and only a reader's eye caught it. The cure is always that one: give the form its own arm with
# its own sentence, then pin THAT. This fence makes the recidive fail by name.
#
# THE BOUNDARY, and it is the whole difficulty of the list. A refusal that identifies the REJECTED
# construct is not a bottom sentence and stays freely pinnable, whether it names it outright ("a
# ListLiteral list source is not translated", "allocation validity windows are not translated"),
# interpolates it ("the fold source %S is not a list-typed own attribute"), or states the one
# property that defines it ("non-numeric arithmetic operand", "negative exponent", "sum fallback
# other than 0"). Listed instead is every refusal that delimits only against the set of forms the
# implementation happens to carry TODAY — the "X here is A, B or C", "takes only", "supports only",
# "Carried: …", "other than <the carried list>" formulations — because that set moves with the
# code, so the sentence tells a reader nothing stable about the witness. Well-formedness
# diagnostics are outside the question entirely ("duration expects two arguments"): they name a
# violated expectation, not an unreached capacity, and their arm catches one thing.
#
# Two entries carry the INTERPOLATION-FREE part of their sentence (ref's literal_value and
# run_rule): the runtime tail is a lexeme or a rule name and differs per document, so no pin can
# ever match it.
#
# BOTH match directions count: the fragment inside the sentence (the som defect's shape) and the
# sentence inside the fragment (a pin that quotes a bottom sentence whole and adds a tail).
# Accepted consequence of the first direction, and intended: a pin short and generic enough to sit
# inside a bottom sentence — "not translated" — is refused even where it would describe a
# form-naming refusal at replay. That is a pin-quality rule, not an accident: a fragment that short
# does not identify its witness either, and the red contract is that a pin names what it witnesses.
#
# Scope is ref + clingo, the two executors that carry fragments. The Drools leg below pins its
# not-carried forms by NAME in its exclusion list rather than by fragment, so it needs no fence.
#
# Known weakness, recorded rather than hidden — the weakness of every pinned list in this gate set:
# a bottom sentence reworded, moved or newly added in ref/clingo without this list moving with it
# leaves the fence blind to exactly that sentence.
#
# The list SHRINKS when a sentence loses its last carrier, and that is a deliberate, dated act with
# its reason in the commit — never a quiet delete. A listed sentence no arm can produce is an
# untruth of the same kind the fence exists to catch: it tells a reader that a residual arm is
# waiting there when none is. ONE removal so far. 2026-09-20 (card ref-periodeconditie-arm):
# `ref/lib/eval.ml eval_cond|condition kind not implemented in this card` left, because that match
# is exhaustive over the condition constructors and its last catch, Time_window, became the
# ch. 3.10 evaluation-date GATE (golden `jutterswet-premievenster`, six clauses, four executors),
# with its own named refusals for the two halves ch. 3.10 defers. Its clingo sister `cond_atoms` is
# a genuine wildcard and still catches the uncarried Predicaat kinds and Regelversie, so that entry
# STAYS. Since the same day the shrink has a MACHINE fence below: a listed sentence its named
# source no longer contains fails the gate by itself.
#
# <source file> <function>|<bottom sentence, verbatim>|<what it delimits against instead of naming>
bottom=$(mktemp) || exit 2
cat > "$bottom" <<'EOF'
ref/lib/eval.ml literal_value|literal of this datatype not implemented|the residual arm over declared datatypes: it says "this datatype" and interpolates the LEXEME, never which datatype has no reader
ref/lib/eval.ml eval_expr|expression kind not implemented in this card|the residual arm over expr node kinds — it names the node CLASS, not the kind it rejected
ref/lib/eval.ml eval_expr|unsupported temporal-read base shape|the wildcard arm under Temporal_read: it says "shape" and names none
ref/lib/eval.ml eval_functie|unsupported gedurende condition shape (an own temporal characteristic, a per-segment comparison of an own temporal attribute, or an all composite of such shapes)|the wildcard arm of the gedurende period reader: the parenthetical enumerates the three CARRIED shapes, so the rejected one stays unnamed
ref/lib/eval.ml eval_functie|unsupported instance-fold source shape|the residual arm over fold-source combinations — the rejected combination is not named
ref/lib/eval.ml eval_functie|of a set has no defined moment to fold (ch. 3.6: only sum and total carry the per-element timeline fold)|the fall-through of the per-element timeline fold, whose match carries sum and total and sends every other kind here: it names the position and the two kinds that ARE carried, never the fold rejected, so it is one sentence for minimum, maximum, first, last and any kind added later. Listed 2026-09-20 after a reader measured it: the filled count's red case pinned this sentence, and the identical document with kind minimum matched that pin word for word. The interpolation-free tail is listed because the attribute name differs per document. The cure is the one this fence always names — a named arm per kind — and the filled count now has one (ch. 3.4c: "no moment at which a filled count can ask a timeline whether it is filled"); the four ranking folds still share this sentence and will need theirs when a red case wants to pin one
ref/lib/eval.ml eval_functie|function kind not implemented in this card|the wildcard arm over function kinds
ref/lib/eval.ml eval_cond|predicate kind not implemented in this card|the residual Predicaat arm, after the carried predicate kinds
ref/lib/eval.ml run_pro_rata|unsupported pro-rata condition shape (an own temporal characteristic; no corpus case for more)|the wildcard arm over pro-rata conditions: it names the one carried shape and claims no corpus case for the rest
ref/lib/eval.ml run_pro_rata|unsupported day-count condition shape (an own temporal characteristic; no corpus case for more)|the wildcard arm over day-count conditions, the mirror of the pro-rata one above
ref/lib/eval.ml run_pro_rata|a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape (a gedurende condition over an own temporal characteristic); no corpus case for more|the wildcard arm over temporal targets: "takes only" enumerates the carried shapes, the rejected target stays unnamed
ref/lib/eval.ml run_rule|unsupported target shape|the wildcard arm over rule targets; the sentence prefixes the RULE name, never the shape it refuses
backends/clingo/lib/translate.ml symbolic_term|a text/enum/boolean operand here is an own-subject attribute read or a literal (no corpus case for more)|the wildcard arm over text/enum/boolean operands: "here is A or B" is the carried list
backends/clingo/lib/translate.ml expr_term|unsupported gedurende condition shape (an own temporal characteristic, a per-segment comparison of an own temporal attribute, or an all composite of such shapes)|the wildcard arm of the gedurende period reader, word for word ref's
backends/clingo/lib/translate.ml expr_term|unsupported temporal-read base shape|the wildcard arm under the temporal read, the mirror of ref's
backends/clingo/lib/translate.ml expr_term|a Construct payload other than a literal or an own attribute read is not translated at rule level (a parameter read, a Match or a composite payload has no corpus case)|the residual Construct arm: "other than a literal or an own attribute read" delimits against the carried pair; the parenthetical lists three candidates and settles none
backends/clingo/lib/translate.ml expr_term|expression kind not translated (no corpus case exercises it)|the wildcard arm over expr node kinds; its parenthetical is the claim a red pin falsifies — the 2026-09-19 defect itself
backends/clingo/lib/translate.ml dt_term|datum-tijd here is not the pair term's arithmetic. Carried: a datum-tijd literal, an own-subject read, datetime ± duration, and first/last over listed operands. A loud flank, no corpus case: a datum-tijd PARAMETER read, a Let/BinderRef-bound datum-tijd (and the residual VariableReference), a set-typed fold, a dateWith or dateTimeWith constructor, and a navigated datum-tijd read|the wildcard arm over datum-tijd terms: it spells out what is "Carried:" and what is a flank, never which of them arrived
backends/clingo/lib/translate.ml whole_period_fails|a whole-period comparison against a bound other than a literal or a scalar parameter is not translated: the bound rides inside the per-segment rule, which binds only the timeline's own subject (no corpus case)|the wildcard arm over whole-period bounds: "other than a literal or a scalar parameter" is the carried pair in complement form
backends/clingo/lib/translate.ml cond_atoms|condition kind not translated (no corpus case exercises it)|the wildcard arm over condition kinds, carrying the same no-corpus-case claim as the expression one
backends/clingo/lib/translate.ml construct_ground_term|a Construct payload other than a literal is not a ground term (a payload that must be bound needs a body atom this position cannot carry; no corpus case)|the residual arm over Construct payloads in ground position: "other than a literal" delimits against the one carried form
backends/clingo/lib/translate.ml sum_term|a sum-typed operand here is an own-subject attribute read or a Construct (no corpus case for more)|the wildcard arm over sum-typed operands: "here is A or B" is the carried list
backends/clingo/lib/translate.ml struct_base|a struct field-access base here is an own-subject attribute read, a bound binder or a struct-typed field access (no corpus case for more)|the wildcard arm over struct field-access bases: "here is A, B or C" is the carried list
backends/clingo/lib/translate.ml scrutinee_pattern|a match scrutinee here is an own-subject sum-typed attribute read (no corpus case for more)|the wildcard arm over match scrutinees: "here is A" is the one carried form
backends/clingo/lib/translate.ml list_source|a list source here is an own list-typed attribute, a list parameter, a MapList or a FilterList (no corpus case for more)|the wildcard arm over list sources: "here is A, B, C or D" is the carried list
backends/clingo/lib/translate.ml body_condition|an IfThenElse condition in a closed body is a comparison over bound terms (no corpus case for more)|the wildcard arm over closed-body IfThenElse conditions: it states the carried shape, not the rejected one
backends/clingo/lib/translate.ml closed_term|a closed-body field access reads a bound struct binder (no corpus case for more)|the wildcard arm over closed-body field accesses: it states the one carried base
backends/clingo/lib/translate.ml closed_term|a closed-body term here is a bound binder, a literal, a parameter, a +/-/×/percentage-of combination, a division (bare or abs), a power by a constant integer exponent, a Construct or a Let (no corpus case for more)|the wildcard arm over closed-body terms: "here is" followed by the whole carried inventory
backends/clingo/lib/translate.ml sum_operand|a sum-typed operand in a closed body is a bound binder or a Construct (no corpus case for more)|the wildcard arm over sum-typed operands in a closed body: the carried pair, not the rejection
backends/clingo/lib/translate.ml filter_condition|a FilterList condition here is a sum-typed (in)equality over the bound element (no corpus case for more)|the wildcard arm over FilterList conditions: "here is" the one carried shape
backends/clingo/lib/translate.ml closed_division|a closed-body rounded division supports only the towardZero abs-division collapse (no corpus case for another rounding mode)|the residual arm over closed-body rounding modes: "supports only" names the carried collapse, and "another rounding mode" is the complement of it
backends/clingo/lib/translate.ml value_condition|a value-position IfThenElse condition is a comparison over bound operands (no corpus case for more)|the wildcard arm over value-position IfThenElse conditions: the carried shape, not the rejected one
backends/clingo/lib/translate.ml translate_regel|a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape (a gedurende condition over an own temporal characteristic); no corpus case for more|the wildcard arm over temporal targets, word for word ref's
backends/clingo/lib/translate.ml translate_regel|rule kind not translated|the wildcard arm over rule kinds
backends/clingo/lib/translate.ml translate_regel|unsupported rule target|the wildcard arm over rule targets
backends/clingo/lib/translate.ml bool_atoms|a characteristic-initialisation expression here is an own-subject boolean attribute read (no corpus case for more)|the wildcard arm over characteristic-initialisation expressions: "here is" the one carried read
EOF
n_bottom=$(grep -c '|' "$bottom")

# --- the CARRIER fence: a listed sentence must still exist in its named source (2026-09-20) -----
# Until this fence the list shrank by hand and nothing checked it; a carrier-less entry waited for a
# reader's eye. Its FIRST version passed three ways it should not have (found by a reader the same
# day, and each is now a rule): it read the source column and ignored it, so an invented file name
# passed; it searched the CONCATENATION of both sources, so the six sentences both files carry were
# unguarded in either; and it counted a COMMENT as a carrier, so a historical measurement quoted in
# prose kept the fence green after the arm itself was gone. Hence: normalise and search PER FILE,
# only the file the source column names, and only in CODE.
#
# Four things stand between a sentence in this list and its spelling in OCaml, and all four are
# normalised away before a plain substring search: COMMENTS (nesting-aware, depth-tracked across
# lines), the WRAP (a long literal ends in a backslash, and the compiler elides that plus the
# newline and the next line's leading blanks), the escaped QUOTES, and the escaped BYTES —
# `translate.ml` writes ± as \xc2\xb1 rather than as the character, so hex escapes go on the source
# side and every non-ASCII byte on BOTH sides, which makes the two spellings meet.
#
# Accepted limits, recorded rather than hidden: two sentences differing only in non-ASCII characters
# are one sentence to this fence; a source line ending in an escaped backslash would be joined
# wrongly (neither file has one); and a literal containing "(*" would open a comment here (neither
# file has one, checked).
strip_ocaml() {
    LC_ALL=C awk '
      function strip(s,   i, n, c2, o) {
        n = length(s); o = ""
        for (i = 1; i <= n; i++) {
          c2 = substr(s, i, 2)
          if (c2 == "(*") { depth++; i++; continue }
          if (c2 == "*)" && depth > 0) { depth--; i++; continue }
          if (depth == 0) o = o substr(s, i, 1)
        }
        return o
      }
      { l = strip($0)
        if (cont) { sub(/^[ \t]*/, "", l); cont = 0 } else { out = out "\n" }
        if (l ~ /\\$/) { sub(/\\$/, "", l); cont = 1 }
        out = out l }
      END { gsub(/\\x[0-9a-fA-F][0-9a-fA-F]/, "", out); gsub(/\\"/, "\"", out); print out }' "$1" \
    | LC_ALL=C tr -d '\200-\377'
}
carries() {  # <normalised file> <sentence> -> 0 when the file's CODE contains it
    printf '%s' "$2" | LC_ALL=C tr -d '\200-\377' | grep -Ff - "$1" >/dev/null 2>&1
}

# Standing self-test of the MECHANISM, in the \u probe's style beside it: one synthetic source
# carrying three sentences — one wrapped across lines, one written with hex escapes, one that exists
# ONLY inside a comment — and the fence must find the first two and miss the third. It tests the
# normaliser, never the inventory, so rewording a real sentence never touches it.
probe_src=$(mktemp) || exit 2
probe_norm=$(mktemp) || exit 2
cat > "$probe_src" <<'PROBE'
(* a probe sentence that lives only in a comment *)
let a () =
  fault
    "a probe sentence that is wrapped across \
     two source lines"
let b () = fault "a probe sentence with an escaped \xc2\xb1 byte"
PROBE
strip_ocaml "$probe_src" > "$probe_norm"
carries "$probe_norm" "a probe sentence that is wrapped across two source lines" \
    || { echo 'FAIL: the carrier fence does not rejoin a wrapped OCaml literal — every long listed sentence would be reported carrier-less' >&2; status=1; }
carries "$probe_norm" "a probe sentence with an escaped ± byte" \
    || { echo 'FAIL: the carrier fence does not see through an escaped byte (\xc2\xb1) — the sentences that spell one would be reported carrier-less' >&2; status=1; }
if carries "$probe_norm" "a probe sentence that lives only in a comment"; then
    echo 'FAIL: the carrier fence counts a COMMENT as a carrier — a historical measurement quoted in prose would keep it green after the arm is gone' >&2
    status=1
fi
rm -f "$probe_src" "$probe_norm"

norm=$(mktemp) || exit 2
last=''
while IFS='|' read -r src sentence _reason; do
    [ -z "$sentence" ] && continue
    file=${src%% *}
    if [ ! -f "$file" ]; then
        printf 'FAIL: the bottom-sentence list names the source %s, which does not exist — the entry cannot be checked at all\n' "$file" >&2
        status=1
        continue
    fi
    if [ "$file" != "$last" ]; then strip_ocaml "$file" > "$norm"; last=$file; fi
    carries "$norm" "$sentence" || {
        printf 'FAIL: the bottom-sentence list carries "%s" (%s), but that file produces it no longer. A listed sentence without a carrier tells a reader a residual arm is waiting where none is: remove the entry, deliberately and with its reason in the commit.\n' \
            "$sentence" "$src" >&2
        status=1
    }
done < "$bottom"
rm -f "$norm"

# The fragments are DECODED, not scraped. A pinned fragment is a JSON string and may spell any
# character as an escape; a shell-side text scrape compares the escape and misses the match, which
# is the one failure mode a fence like this may not have. So the reading and the comparing happen
# where the red path already does both: `ref --bottom-sentence` reads expectedFault with the same reader
# the runner above uses, and compares with Case.contains, the same substring matcher it runs a
# fault report through — so the fence tests exactly the relation the pin has at replay. No new
# build or install requirement: ref is already built and required by this gate, and the shell side
# adds only POSIX text handling (a read loop, an awk line lookup).
sent=$(mktemp) || exit 2
while IFS='|' read -r _src sentence _reason; do printf '%s\n' "$sentence"; done < "$bottom" > "$sent"

# Standing proof that the DECODING path is the live one (the \u mutation, permanent). A synthetic
# pin spells its first character as an escape and must still be caught; a text scrape cannot catch
# it. The pair is self-contained on purpose — it tests the mechanism, not the inventory, so
# rewording a real bottom sentence never touches it.
probe_s=$(mktemp) || exit 2
probe_c=$(mktemp) || exit 2
printf 'expression kind not translated (no corpus case exercises it)\n' > "$probe_s"
printf '{ "expectedFault": { "ref": "\\u0065xpression kind not translated", "clingo": "a pin that cites nothing" } }\n' > "$probe_c"
probe_out=$("$REF" --bottom-sentence "$probe_s" "$probe_c" 2>&1)
probe_hits=$(printf '%s\n' "$probe_out" | grep -c '^HIT ')
if [ "$probe_hits" != "1" ] || ! printf '%s\n' "$probe_out" | grep -q '^HIT ref '; then
    printf 'FAIL: the bottom-sentence self-test did not catch an escaped citation (expected exactly one HIT on ref, got %s) — the fence is comparing escapes instead of the characters they stand for, so every pin it clears is cleared for the wrong reason:\n%s\n' \
        "$probe_hits" "$probe_out" >&2
    status=1
fi
rm -f "$probe_s" "$probe_c"

n_frag=0
hits=$(mktemp) || exit 2
while IFS= read -r case; do
    name=$(basename "$(dirname "$case")")
    if ! out=$("$REF" --bottom-sentence "$sent" "$case" 2>&1); then
        printf 'FAIL: %s — the pinned fragments could not be read; the red contract pins both executors, and a pin this fence cannot read is a fence that holds nothing:\n%s\n' \
            "$name" "$out" >&2
        status=1
        continue
    fi
    printf '%s\n' "$out" > "$hits"
    pins=$(awk '$1 == "PINS" { print $2; exit }' "$hits")
    if [ -z "$pins" ]; then
        printf 'FAIL: %s — the fence reported no PINS count (script/runner contract broken):\n%s\n' "$name" "$out" >&2
        status=1
        continue
    fi
    n_frag=$((n_frag + pins))
    while read -r tok key line frag; do
        [ "$tok" = "HIT" ] || continue
        entry=$(awk -v n="$line" 'NR == n' "$bottom")
        printf 'FAIL: %s pins expectedFault.%s = %s, which cites the bottom sentence on line %s of the list (%s). A bottom sentence is the same refusal for every form its arm catches, so the pin witnesses nothing of its own: give the form its own arm with its own sentence, then pin that.\n' \
            "$name" "$key" "$frag" "$line" "$entry" >&2
        status=1
    done < "$hits"
done < "$list"
rm -f "$bottom" "$sent" "$hits"
if [ "$n_frag" -eq 0 ]; then
    echo 'FAIL: the bottom-sentence fence read no fragment at all — an unmeasured fence is never silently green' >&2
    status=1
fi

# --- the Drools leg (decision V3-b, 2026-08-27) -----------------------------------------------
# The Drools backend CONSUMES the uchar IR (loads ir.json off the wire). For each rood case it
# either reports a FAULT — the report this leg matches the case's `expectedFault.drools` fragment
# against — or it does not carry the red-path form (an EXCLUDE from the loader, a transpiler or
# the case reader, or a NOFAULT where the backend does not enforce the constraint and runs to a
# model). A carried form pins a fragment; a not-carried form is listed below with a reason. An EXCLUDE
# entry also pins the register code of the refusal (`name|EXCLUDE DROOLS-014 <reason>`, card
# drools-refusal-codes, 2026-10-19), which the status line carries at its head (`EXCLUDE DROOLS-014:
# <sentence>`); the leg compares the two, so a refusal that moves to another code fails by case name
# with both codes. A NOFAULT entry refuses nothing and carries no code. The
# exclusion list is stale-guarded: a listed case that now FAULTS, or is gone, fails the gate; a
# case that is neither pinned nor listed fails the gate (completeness).
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
# unknown-role-target-label, uni-temporal-key-scalar, uu-temporal-key-scalar — six when measured
# 2026-09-23, until the card ref-klipvouw-nul moved two to corpus/negatief), and on the
# vensterheffing document (measured 2026-09-23) an
# undeclared attribute gives "EXCLUDE case sets undeclared attribute", while meldwaarde written as
# {"boolean": true} gives "FAULT IllegalArgumentException: argument type mismatch" and an
# undeclared characteristic gives "FAULT RuntimeException: kon setter setVerzonnen(boolean) niet
# aanroepen …". Hence the PIN RULE: a `drools` fragment cites the backend's OWN sentence and never
# platform text — a JDK exception message, or the parse message the backend appends in
# parentheses. Its own sentence may be a runtime message ("Breuk deling door 0", "creation
# budget exhausted: …"); for a refusal while the case is read it is a sentence that names position
# and lexeme (asofdate-kalender: "malformed date '2026-02-30' on 'asOfDate': the field widths are
# right but the calendar is not"). A pin on platform text pins a crash: this leg accepted a trial
# red case pinned on "argument type mismatch" (lens, 2026-09-23). The rule is a review rule;
# nothing below checks it.
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
    # package`, the backend's JUnit suite included; that suite is gate 12's obligation (its one `mvn
    # package` runs it and refreshes the jar), so it deliberately no longer runs here. legfail is
    # leg-LOCAL so the leg always leaves a verdict of its own, regardless of earlier red-case failures
    # elsewhere in the gate (lens finding: keying on the global status let the leg evaporate without
    # one).
    legfail=0
    . scripts/lib/drools-jar.sh
    if ! command -v mvn >/dev/null 2>&1; then
        echo 'check-rood: FAIL — Drools leg: java present but mvn missing; the leg takes the jar fresh or rebuilt, and only mvn can rebuild a stale one (stale-jar guard)' >&2
        status=1; legfail=1
        JAR=''
    elif ! JAR=$(ensure_drools_jar); then
        echo 'check-rood: FAIL — Drools leg: no fresh shaded jar (the drools-jar line above says why)' >&2
        status=1; legfail=1
        JAR=''
    fi
    if [ -z "$JAR" ]; then
        # every empty-JAR path above set legfail and printed its own verdict; this arm only guards
        # against a future edit that forgets to — belt and braces, never the primary reporter
        [ $legfail -eq 1 ] || { echo 'check-rood: FAIL — Drools leg: no shaded jar and no verdict printed (script defect)' >&2; status=1; }
    else
        MAIN=dev.uchar.regelspraak.cases.DroolsRedMain
        excl=$(mktemp) || exit 2
        # <rood-case-dir>|<reason the v0 Drools backend does not fault-pin this red form>
        cat > "$excl" <<'EOF'
aantalgevulde-temporele-setbron|EXCLUDE DROOLS-210 measured 2026-10-10 (card tms-omschakeling): the exec-model branch this leg drives, which lowers with truth maintenance since the switch-over, refuses the reaching rule when it builds the KieBase, "transpiler SKIP: regel: aantal gewogen vondsten — een Aggregation AANTAL_GEVULDE over het tijdsafhankelijke attribuut 'gewicht' in een waarde (…): zij heeft geen moment waarop zij een tijdlijn rangschikt, telt of toetst". The refusal is the SAME judgement ref and clingo pin: a count has no defined MOMENT at which to ask a timeline whether it is filled (RUNTIME-SEMANTICS ch. 3.4c), so all three executors decline and only the two that can carry a fragment do. Measured first 2026-09-20 on the destructive lowering, whose exec branch reported "regel (aggregatie buiten Phase-C scope)" and whose DRL-text branch its marker. Listed rather than fragment-pinned because the transpiler raises its refusal as an Excluded, which the red run reports as EXCLUDE, and this leg pins a fragment only on a FAULT; the day ch. 3.4c supplies a moment rule, this entry goes stale and the gate says so
aanvoergeld-parameter-ongebonden|NOFAULT measured 2026-09-23 (card clingo-parameter-failfast): both cases run to a model. The backend answers a parameter the case does not bind the way the ASP backend did before that card — case1 aanvoergeld 310.00 / 87.40 / 0.00, the absent tarief coalesced to 0; case2 aanvoerplichtig absent on J1 and J3 — where ref and clingo fault fail-fast in one sentence (ch. 5.7). Listed rather than fragment-pinned because this leg does not fault here at all; the day the backend binds parameters fail-fast, this entry goes stale and the gate says so
ambiguous-backlink|EXCLUDE DROOLS-080 measured 2026-10-10 (card tms-omschakeling): since the switch-over the exec-model branch this leg drives lowers with truth maintenance, and it refuses the creation when it builds the KieBase, "transpiler SKIP: objectcreatie: insigne voor een jutter — more than one fact type relates "Jutter" to "Insigne"; the created object's back-link is ambiguous (ch. 5.5); deze tak draagt deze vorm nog niet". It names the ambiguity ref and clingo fault on, but as a form the branch does not carry, an EXCLUDE, and this leg pins a fragment only on a FAULT. Until the switch-over the destructive lowering faulted at fire in the same words and the case pinned them
ambiguous-case-role-key|EXCLUDE DROOLS-050 measured 2026-09-22 (P1 port): the case-input arm refuses at the role key `ligger` on the bean-field limit — two same-named roles contend for one field on Schuit. Same phase as ref's and clingo's load fault, different reason: they refuse the ambiguity itself, this backend a field it cannot carry; it raises that as a form it does not carry, so the refusal is an EXCLUDE
closed-nonexact-division|EXCLUDE DROOLS-008 the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
closed-runtime-division-by-zero|EXCLUDE DROOLS-008 the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
closed-runtime-nonexact-division|EXCLUDE DROOLS-008 the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
dagsoortparameter-ongebonden|EXCLUDE DROOLS-179 measured 2026-10-10 (card tms-omschakeling): the exec-model branch this leg drives, which lowers with truth maintenance since the switch-over, refuses the reaching rule when it builds the KieBase, "transpiler SKIP: regel: streefdatum van een havenmeester — een duur in dagsoorten (…): de definitie van de dagsoort 'havendag' leest de parameter 'openingsdatum', die deze tak in een dagsoortdefinitie niet leest (WF-031 laat haar toe)", so the unbound parameter is never reached. The refusal is about the operand FORM and not about the binding, which is why it is listed rather than fragment-pinned. Measured first 2026-09-19 on the destructive lowering, whose day-kind predicate compiler (kern.DayKinds) carried no PARAMETER operand at all and SKIPped the rule (waarde buiten scope)
dagsoortverwijzing-andere-dag|EXCLUDE DROOLS-180 measured 2026-10-10 (card tms-omschakeling): the exec-model branch this leg drives, which lowers with truth maintenance since the switch-over, refuses the reaching rule when it builds the KieBase, "transpiler SKIP: regel: uiterste bergingsdatum van een bergingsploeg — een duur in dagsoorten (…): de definitie van de dagsoort 'springtijdag' toetst de dagsoort 'ankerdag' op een andere dag dan de gebonden dag (…), een vorm die geen executor uitvoert (RUNTIME-SEMANTICS ch. 3.8)", and fire is never reached. All three executors refuse the form; only the two that can carry a fragment pin one. Measured first 2026-09-19 on the destructive lowering, whose day-kind predicate compiler (kern.DayKinds) left the §9.8 composition UNCOMPILED for an operand other than the bound day and SKIPped the rule (waarde buiten scope)
decimal-nonterminating|NOFAULT the v0 backend has no §6.1.2 write-precision guard — a non-terminating quotient collapses to DECIMAL64 (Breuk.naarDecimal) instead of faulting; it runs to a model
input-overprecision|NOFAULT the v0 case runner does not enforce declared attribute input precision — it accepts the over-precise value and runs to a model
input-overprecision-param|NOFAULT the v0 case runner does not enforce declared parameter input precision — it accepts the over-precise value and runs to a model
list-element-overprecision|EXCLUDE DROOLS-009 the v0 loader refuses the model's record datatype (StructType) before any list-element precision is reached — the record/struct form is not carried
periodevenster-tijdlijnkenmerk|EXCLUDE DROOLS-102 measured 2026-09-20, reason updated 2026-09-23 and 2026-10-10 (card tms-omschakeling): a period condition on a TEMPORAL target is ch. 3.10's clip, not the evaluation-date gate. The clip is built for ATTRIBUTE targets in ref and clingo since 2026-09-23 (golden jutterswet-pachtvenster, which both branches of this backend decline by name); this document's target is a CHARACTERISTIC, whose timeline neither OCaml executor writes (ch. 3.11), so all four refuse. The exec-model branch this leg drives, which lowers with truth maintenance since the switch-over, refuses one step earlier, at the target: "transpiler SKIP: regel: berging van een vondst — het tijdsafhankelijke kenmerk 'geborgen' van Vondst als doel: deze tak draagt deze vorm nog niet; geen golden met cases schrijft er een (RUNTIME-SEMANTICS ch. 3.11)". The destructive lowering refused at the clip (exec "periodevergelijking op tijdsafhankelijk doel — §8.4.3 clip", DRL-text "TODO periodevergelijking op een tijdsafhankelijk doel (§8.4.3)"), while the ASP backend once PLAYED the scalar gate here because its target test read only the ATTRIBUTE granularity table. Listed rather than fragment-pinned: the transpiler raises its refusal as an Excluded, which the red run reports as EXCLUDE, and this leg pins a fragment only on a FAULT
som-fallback-always-evaluated|EXCLUDE DROOLS-207 the reaching rule (vangstsom van een vangst) does not lower — a transpiler SKIP, since the card tms-omschakeling (2026-10-10) in the words of the exec-model branch this leg drives, "een Aggregation SOM zonder elementtype (…)", before it in those of the destructive lowering, "aggregatie buiten Phase-C scope"; fire is never reached, so the sum's stand-in is not Drools-observable here
struct-field-overprecision|EXCLUDE DROOLS-009 the v0 loader refuses the model's record datatype (StructType) — the record/struct form is not carried
to-one-second-filler|EXCLUDE DROOLS-161 measured 2026-10-10 (card tms-omschakeling): the exec-model branch this leg drives, which lowers with truth maintenance since the switch-over, refuses a fact creation of the document when it builds the KieBase, "transpiler SKIP: feitcreatie: berging door de kustgemeente — het pad van de feitcreatie 'berging door de kustgemeente': de stap over 'aanmelding' heeft twee rollen naar veel, waarvoor het beanmodel geen veld heeft (§13.3.9 opslagrichting)", so the second-filler violation is never reached. Until the switch-over the destructive lowering refused the reaching rule instead (aantal bergende gemeenten van een vondst, "aggregatie over de enkelvoudige elementrol 'bergende gemeente' wiens zusterrol 'geborgen vondst' meervoudig is …"; measured 2026-09-23, card rood-heeltijdvak-ongenegeerd). The transpiler raises its refusal as an Excluded, which the red run reports as EXCLUDE, and this leg pins a fragment only on a FAULT
uni-temporal-key-scalar|EXCLUDE DROOLS-051 the case sets a temporal attribute (kavelnummer) as a scalar while carrying NO asOfDate — the bucket convention (corpus/golden/README.md) gives that scalar no evaluation date to sit in, so the input is not carried and the ever-unique key is never reached
uni-two-temporal-keys|EXCLUDE DROOLS-127 measured 2026-10-10 (card tms-omschakeling): since the switch-over the exec-model branch this leg drives lowers with truth maintenance, and it refuses the uniqueness when it builds the KieBase, "transpiler SKIP: uniciteit: dubbel temporeel enkelvoudig — een uniciteitssleutel 'kavelnummer' van Strandvondst op een tijdlijn naast een tweede: een sleutel heeft hoogstens één tijdsafhankelijk deel, …". That is the judgement ref and clingo fault on (at most one temporal key component, ch. 5.9), but as a form the branch does not carry, an EXCLUDE, and this leg pins a fragment only on a FAULT. Until the switch-over the destructive lowering faulted on the key list in ref's words and the case pinned them
unknown-role-target-label|EXCLUDE DROOLS-048 measured 2026-09-19: the backend's own loader refuses the same input and names the same dangling label (role 'liggers' names unknown label S9), but the backend raises it as a form it does not carry, an EXCLUDE, and this leg pins a fragment only on a FAULT. The agreement is real and recorded here rather than pinned: all three executors reject this case, and only the two that can carry a fragment do
uu-temporal-key-scalar|EXCLUDE DROOLS-051 the case sets a temporal attribute (vondstnummer) as a scalar while carrying NO asOfDate — the bucket convention (corpus/golden/README.md) gives that scalar no evaluation date to sit in, so the input is not carried and the ever-unique key is never reached
uu-two-temporal-keys|EXCLUDE DROOLS-127 measured 2026-10-10 (card tms-omschakeling): the refusal of uni-two-temporal-keys for the uniqueness over more sources, "transpiler SKIP: uniciteit-unie: dubbel temporeel — een uniciteitssleutel 'volgnummer' van Strandvondst op een tijdlijn naast een tweede: een sleutel heeft hoogstens één tijdsafhankelijk deel, …", an EXCLUDE where ref and clingo fault; until the switch-over the destructive lowering faulted in ref's words and the case pinned them
vintage-uncovered-date|EXCLUDE DROOLS-023 the v0 loader refuses the TemporalRead expression node (the vintage read) — the temporal-read form is not carried
EOF
        d_fault=0
        d_excl=0
        d_excl_codes=0
        d_codes=0
        while IFS= read -r case; do
            name=$(basename "$(dirname "$case")")
            ir=$(dirname "$case")/ir.json
            frag=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get("expectedFault",{}).get("drools",""))' "$case")
            reason=$(awk -F'|' -v n="$name" '$1==n {i=index($0,"|"); print substr($0,i+1); exit}' "$excl")
            # DroolsRedMain prints exactly one status line (FAULT|NOFAULT|EXCLUDE|ERROR <detail>) last on
            # stdout; the leg classifies on that STATUS TOKEN, never the bare exit code, so an ERROR (an I/O
            # failure, or a crash while the IR is loaded and the KieBase built) can never be swallowed as a
            # not-carried outcome and a listed reason's category is checked. A crash AFTER the build is a FAULT
            # (head of this leg): a listed case that shows one fails as stale, and a pin may not match it.
            out=$(java -cp "$JAR" "$MAIN" "$ir" "$case" 2>/dev/null | tail -1)
            tok=${out%% *}
            if [ -n "$frag" ]; then
                if [ -n "$reason" ]; then
                    echo "FAIL: $name carries a drools fragment AND sits in the Drools exclusion list — drop the static entry" >&2
                    status=1
                fi
                # a carried case MUST be a FAULT whose report contains the pinned fragment, the backend's own
                # sentence and never platform text (head of this leg); any other token (NOFAULT / EXCLUDE /
                # ERROR) is a regression, never a pass.
                case "$tok" in
                    FAULT)
                        case "$out" in
                            *"$frag"*) d_fault=$((d_fault + 1)) ;;
                            *) printf 'FAIL: %s — Drools faulted but the pinned fragment is absent (drools="%s"):\n%s\n' "$name" "$frag" "$out" >&2
                               status=1 ;;
                        esac
                        # the code at the head of the status line, `FAULT EF-005: …` (several: `EF-005, EF-016: …`)
                        key=${case#corpus/rood/}
                        key=${key%.json}
                        pin=$(awk -F'|' -v c="$key" '$1 == c { print $2; exit }' "$pinned_map")
                        seen=$(printf '%s\n' "$out" | sed -n 's/^FAULT \(EF-[0-9][0-9][0-9]\(, EF-[0-9][0-9][0-9]\)*\): .*/\1/p' | tr -d ' ')
                        seen=${seen:--}
                        if [ -z "$pin" ]; then
                            printf 'FAIL: %s — no pinned code was decoded for the Drools code check (see the ref leg above)\n' "$name" >&2
                            status=1
                        elif code_check "$key" drools "$seen" "$pin"; then
                            [ "$seen" = "-" ] || d_codes=$((d_codes + 1))
                        else
                            status=1
                        fi ;;
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
        # monotone floor (maintainer decision 2026-09-09) on the EXCLUDE entries that pin a register code: a list that
        # quietly lost its code pins would keep passing on the channel alone. Measured 2026-10-19 (card
        # drools-refusal-codes): 19 cases, the 19 EXCLUDE entries of the list.
        EXPECTED_MIN_DROOLS_EXCLUDE_CODES=19
        if [ "$d_excl_codes" -lt "$EXPECTED_MIN_DROOLS_EXCLUDE_CODES" ]; then
            printf 'check-rood: FAIL — Drools leg: %s EXCLUDE cases compared on their DROOLS code, but EXPECTED_MIN_DROOLS_EXCLUDE_CODES pins at least %s — the list shrank or lost its code pins\n' "$d_excl_codes" "$EXPECTED_MIN_DROOLS_EXCLUDE_CODES" >&2
            status=1
        fi
        if [ "$status" -eq 0 ]; then
            printf 'check-rood: Drools leg PASS (%s fault-pinned, %s of them reporting the case'"'"'s code; %s not-carried with reason, %s of them EXCLUDE pinned by their DROOLS code)\n' "$d_fault" "$d_codes" "$d_excl" "$d_excl_codes"
        fi
        drools_ran=1
    fi
else
    echo 'check-rood: Drools leg SKIPPED — needs java + python3 (and, once running, mvn: the leg takes the jar fresh or rebuilt). Not a blocker on a machine without java; the ref/clingo legs above are the CI blocker. NB hosted ubuntu runners DO carry java, so there the leg runs (and needs a warm ~/.m2).' >&2
fi

# the divergence list's gone-guard: every entry was consulted by a check. A Drools entry is consulted
# only when the Drools leg ran; on a machine without java it is skipped with the leg, loudly above.
while IFS='|' read -r c e reason; do
    [ -z "$c" ] && continue
    case "$e" in
        ref|clingo|drools) ;;
        *) echo "FAIL: the divergence entry $c names the executor '$e' (ref, clingo or drools)" >&2; status=1; continue ;;
    esac
    case "$reason" in
        "REFUSAL "*|"LIMIT "*) ;;
        *) echo "FAIL: the divergence entry $c|$e has no category REFUSAL or LIMIT as the first word of its reason" >&2; status=1 ;;
    esac
    if [ "$e" = drools ] && [ "${drools_ran:-0}" -eq 0 ]; then continue; fi
    if ! grep -qx "$c|$e" "$div_used"; then
        echo "FAIL: stale divergence — the entry $c|$e was consulted by no code check (its case is gone, or the executor makes no check on it)" >&2
        status=1
    fi
done < "$divergence"
rm -f "$divergence" "$div_used" "$pinned_map"

if [ "$status" -eq 0 ]; then
    printf 'check-rood: PASS (%s red cases fault observably in ref and clingo, floor %s; %s pinned fragments cite none of the %s listed bottom sentences; %s ref and clingo code checks report the case'"'"'s code, floor %s, %s listed as divergence; %s fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor %s)\n' \
        "$n_cases" "$EXPECTED_MIN_RED_CASES" "$n_frag" "$n_bottom" "$n_red_codes" "$EXPECTED_MIN_RED_CODES" "$n_div" "$n_ef" "$EXPECTED_MIN_EF_CODES"
fi
exit "$status"
