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
#   3. an empty red set is a failure, never silently green;
#   4. no pinned fragment cites a BOTTOM SENTENCE of ref or clingo — the
#      refusal a node-kind match falls through to, which names no form
#      (the bodemzin fence, 2026-09-19).
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

# --- the bodemzin fence: a pin may not cite a bottom sentence (2026-09-19) ---------------------
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
# property that defines it ("non-numeric arithmetic operand", "negative exponent", "som fallback
# other than 0"). Listed instead is every refusal that delimits only against the set of forms the
# implementation happens to carry TODAY — the "X here is A, B or C", "takes only", "supports only",
# "Carried: …", "other than <the carried list>" formulations — because that set moves with the
# code, so the sentence tells a reader nothing stable about the witness. Well-formedness
# diagnostics are outside the question entirely ("tijdsduur expects two arguments"): they name a
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
# is exhaustive over the condition constructors and its last catch, Periode_conditie, became the
# ch. 3.10 evaluation-date GATE (golden `jutterswet-premievenster`, six clauses, four executors),
# with its own named refusals for the two halves ch. 3.10 defers. Its clingo sister `cond_atoms` is
# a genuine wildcard and still catches the uncarried Predicaat kinds and Regelversie, so that entry
# STAYS. Since the same day the shrink has a MACHINE fence below: a listed sentence its named
# source no longer contains fails the gate by itself.
#
# <source file> <function>|<bottom sentence, verbatim>|<what it delimits against instead of naming>
bodem=$(mktemp) || exit 2
cat > "$bodem" <<'EOF'
ref/lib/eval.ml literal_value|literal of this datatype not implemented|the residual arm over declared datatypes: it says "this datatype" and interpolates the LEXEME, never which datatype has no reader
ref/lib/eval.ml eval_expr|expression kind not implemented in this card|the residual arm over expr node kinds — it names the node CLASS, not the kind it rejected
ref/lib/eval.ml eval_expr|unsupported temporal-read basis shape|the wildcard arm under Temporal_read: it says "shape" and names none
ref/lib/eval.ml eval_functie|unsupported gedurende condition shape (WF-055: an own temporal characteristic, a per-segment comparison of an own temporal attribute, or an ALLE composite of such shapes)|the wildcard arm of the gedurende period reader: the parenthetical enumerates the three CARRIED shapes, so the rejected one stays unnamed
ref/lib/eval.ml eval_functie|unsupported instance-fold source shape|the residual arm over fold-source combinations — the rejected combination is not named
ref/lib/eval.ml eval_functie|of a set has no defined moment to fold (ch. 3.6: only som and totaal carry the per-element timeline fold)|the fall-through of the per-element timeline fold, whose match carries som and totaal and sends every other kind here: it names the position and the two kinds that ARE carried, never the fold rejected, so it is one sentence for minimum, maximum, eerste, laatste and any kind added later. Listed 2026-09-20 after a reader measured it: the filled count's red case pinned this sentence, and the identical document with kind MINIMUM matched that pin word for word. The interpolation-free tail is listed because the attribute name differs per document. The cure is the one this fence always names — a named arm per kind — and the filled count now has one (ch. 3.4c: "no moment at which a filled count can ask a timeline whether it is filled"); the four ranking folds still share this sentence and will need theirs when a red case wants to pin one
ref/lib/eval.ml eval_functie|function kind not implemented in this card|the wildcard arm over function kinds
ref/lib/eval.ml eval_cond|predicate kind not implemented in this card|the residual Predicaat arm, after the carried predicate kinds
ref/lib/eval.ml run_pro_rata|unsupported pro-rata condition shape (an own temporal characteristic; no corpus case for more)|the wildcard arm over pro-rata conditions: it names the one carried shape and claims no corpus case for the rest
ref/lib/eval.ml run_pro_rata|unsupported day-count condition shape (an own temporal characteristic; no corpus case for more)|the wildcard arm over day-count conditions, the mirror of the pro-rata one above
ref/lib/eval.ml run_pro_rata|a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape (a gedurende condition over an own temporal characteristic); no corpus case for more|the wildcard arm over temporal targets: "takes only" enumerates the carried shapes, the rejected target stays unnamed
ref/lib/eval.ml run_rule|unsupported target shape|the wildcard arm over rule targets; the sentence prefixes the RULE name, never the shape it refuses
backends/clingo/lib/translate.ml symbolic_term|a text/enum/boolean operand here is an own-subject attribute read or a literal (no corpus case for more)|the wildcard arm over text/enum/boolean operands: "here is A or B" is the carried list
backends/clingo/lib/translate.ml expr_term|unsupported gedurende condition shape (WF-055: an own temporal characteristic, a per-segment comparison of an own temporal attribute, or an ALLE composite of such shapes)|the wildcard arm of the gedurende period reader, word for word ref's
backends/clingo/lib/translate.ml expr_term|unsupported temporal-read basis shape|the wildcard arm under the temporal read, the mirror of ref's
backends/clingo/lib/translate.ml expr_term|a Construct payload other than a literal or an own attribute read is not translated at rule level (a parameter read, a Match or a composite payload has no corpus case)|the residual Construct arm: "other than a literal or an own attribute read" delimits against the carried pair; the parenthetical lists three candidates and settles none
backends/clingo/lib/translate.ml expr_term|expression kind not translated (no corpus case exercises it)|the wildcard arm over expr node kinds; its parenthetical is the claim a red pin falsifies — the 2026-09-19 defect itself
backends/clingo/lib/translate.ml dt_term|datum-tijd here is not the pair term's arithmetic. Carried: a datum-tijd literal, an own-subject read, datetime ± duration, and eerste/laatste over listed operands. A loud flank, no corpus case: a datum-tijd PARAMETER read, a Let/BinderRef-bound datum-tijd (and the residual VariabeleRef), a set-typed fold, a datum-met or datumtijd-met constructor, and a navigated datum-tijd read|the wildcard arm over datum-tijd terms: it spells out what is "Carried:" and what is a flank, never which of them arrived
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
backends/clingo/lib/translate.ml closed_division|a closed-body rounded division supports only the richting-nul abs-division collapse (no corpus case for another rounding mode)|the residual arm over closed-body rounding modes: "supports only" names the carried collapse, and "another rounding mode" is the complement of it
backends/clingo/lib/translate.ml value_condition|a value-position IfThenElse condition is a comparison over bound operands (WF-077; no corpus case for more)|the wildcard arm over value-position IfThenElse conditions: the carried shape, not the rejected one
backends/clingo/lib/translate.ml translate_regel|a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape (a gedurende condition over an own temporal characteristic); no corpus case for more|the wildcard arm over temporal targets, word for word ref's
backends/clingo/lib/translate.ml translate_regel|rule kind not translated|the wildcard arm over rule kinds
backends/clingo/lib/translate.ml translate_regel|unsupported rule target|the wildcard arm over rule targets
backends/clingo/lib/translate.ml bool_atoms|a characteristic-initialisation expression here is an own-subject boolean attribute read (no corpus case for more)|the wildcard arm over characteristic-initialisation expressions: "here is" the one carried read
EOF
n_bodem=$(grep -c '|' "$bodem")

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
done < "$bodem"
rm -f "$norm"

# The fragments are DECODED, not scraped. A pinned fragment is a JSON string and may spell any
# character as an escape; a shell-side text scrape compares the escape and misses the match, which
# is the one failure mode a fence like this may not have. So the reading and the comparing happen
# where the red path already does both: `ref --bodemzin` reads expectedFault with the same reader
# the runner above uses, and compares with Case.contains, the same substring matcher it runs a
# fault report through — so the fence tests exactly the relation the pin has at replay. No new
# build or install requirement: ref is already built and required by this gate, and the shell side
# adds only POSIX text handling (a read loop, an awk line lookup).
sent=$(mktemp) || exit 2
while IFS='|' read -r _src sentence _reason; do printf '%s\n' "$sentence"; done < "$bodem" > "$sent"

# Standing proof that the DECODING path is the live one (the \u mutation, permanent). A synthetic
# pin spells its first character as an escape and must still be caught; a text scrape cannot catch
# it. The pair is self-contained on purpose — it tests the mechanism, not the inventory, so
# rewording a real bottom sentence never touches it.
probe_s=$(mktemp) || exit 2
probe_c=$(mktemp) || exit 2
printf 'expression kind not translated (no corpus case exercises it)\n' > "$probe_s"
printf '{ "expectedFault": { "ref": "\\u0065xpression kind not translated", "clingo": "a pin that cites nothing" } }\n' > "$probe_c"
probe_out=$("$REF" --bodemzin "$probe_s" "$probe_c" 2>&1)
probe_hits=$(printf '%s\n' "$probe_out" | grep -c '^HIT ')
if [ "$probe_hits" != "1" ] || ! printf '%s\n' "$probe_out" | grep -q '^HIT ref '; then
    printf 'FAIL: the bodemzin self-test did not catch an escaped citation (expected exactly one HIT on ref, got %s) — the fence is comparing escapes instead of the characters they stand for, so every pin it clears is cleared for the wrong reason:\n%s\n' \
        "$probe_hits" "$probe_out" >&2
    status=1
fi
rm -f "$probe_s" "$probe_c"

n_frag=0
hits=$(mktemp) || exit 2
while IFS= read -r case; do
    name=$(basename "$(dirname "$case")")
    if ! out=$("$REF" --bodemzin "$sent" "$case" 2>&1); then
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
        entry=$(awk -v n="$line" 'NR == n' "$bodem")
        printf 'FAIL: %s pins expectedFault.%s = %s, which cites the bottom sentence on line %s of the list (%s). A bottom sentence is the same refusal for every form its arm catches, so the pin witnesses nothing of its own: give the form its own arm with its own sentence, then pin that.\n' \
            "$name" "$key" "$frag" "$line" "$entry" >&2
        status=1
    done < "$hits"
done < "$list"
rm -f "$bodem" "$sent" "$hits"
if [ "$n_frag" -eq 0 ]; then
    echo 'FAIL: the bodemzin fence read no fragment at all — an unmeasured fence is never silently green' >&2
    status=1
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
aantalgevulde-temporele-setbron|EXCLUDE measured 2026-09-20: both transpiler branches carry the filled-value count from that day, but neither carries it over a TEMPORAL element attribute — the exec branch reports "regel (aggregatie buiten Phase-C scope)" and the DRL-text branch its marker, both one phase before fire. The refusal is the SAME judgement ref and clingo pin: a count has no defined MOMENT at which to ask a timeline whether it is filled (RUNTIME-SEMANTICS ch. 3.4c), so all three executors decline and only the two that can carry a fragment do. Listed rather than fragment-pinned because this leg's vocabulary reserves FAULT for a genuine fault at fire; the day ch. 3.4c supplies a moment rule, this entry goes stale and the gate says so
ambiguous-case-role-key|EXCLUDE measured 2026-09-22 (P1 port): the case-input arm refuses at the role key `ligger` on the bean-field limit — two same-named roles contend for one field on Schuit. Same phase as ref's and clingo's load fault, different reason: they refuse the ambiguity itself, this backend a field it cannot carry; this leg's vocabulary reserves FAULT for a genuine fault at fire, so the refusal stays an EXCLUDE
ambiguous-fold-role-name|NOFAULT the v0 backend resolves the fold role without detecting the cross-type ambiguity ref/clingo reject; it runs to a model
begrensd-reads-bounds-in-order|NOFAULT the v0 backend evaluates a Begrensd's MAXIMUM bound BEFORE its MINIMUM — the reverse of the ch. 5.11 wire order — so the ch. 10.6 barrier sitting in the maximum fires before the minimum's division is ever reached, and the fault never happens; it runs to a model. Measured both ways on this exact shape: with the division in the MAXIMUM instead, Drools DOES fault ("Breuk deling door 0") on the very document where ref and clingo do not. This is the operand-order divergence the witness exists to expose; the Drools-side correction is a card of its own
closed-nonexact-division|EXCLUDE the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
closed-runtime-division-by-zero|EXCLUDE the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
closed-runtime-nonexact-division|EXCLUDE the v0 loader refuses the model's list datatype (ListType) — the fold-over-a-list form is not carried
contextwaarde-de-dag-buiten-dagsoort|EXCLUDE measured 2026-09-19: the transpiler SKIPs the document's only rule (jaartal van een baken — waarde buiten scope), because the v0 backend reads `de dag` ONLY inside its day-kind predicate compiler (kern.Dagsoorten) and its ordinary constant reader carries no such name; fire is never reached. The two OCaml executors both refuse the same node by name, one phase later, and pin the fragment
dagsoortparameter-ongebonden|EXCLUDE measured 2026-09-19: the day-kind predicate compiler (kern.Dagsoorten) carries no PARAMETER operand at all — it compiles the voorwaarde once into the KieBase while parameter values arrive per case as session globals — so the reaching rule is a transpiler SKIP (waarde buiten scope) and the unbound parameter is never reached. The refusal is about the operand FORM and not about the binding, which is why it is listed rather than fragment-pinned
dagsoortverwijzing-andere-dag|EXCLUDE measured 2026-09-19: the day-kind predicate compiler (kern.Dagsoorten) leaves the §9.8 composition UNCOMPILED when its operand is a date other than the bound day — the narrow route the day-kind card took after measuring that ref and this backend both answered what the ASP backend refused — so the reaching rule is a transpiler SKIP (waarde buiten scope) and fire is never reached. All three executors now refuse the form; only the two that can carry a fragment pin one
datumsetbron-somvouw|EXCLUDE measured 2026-09-20: both transpiler branches refuse the ADDITIVE fold over a DATE-valued set source BY NAME, one phase before fire — the exec branch as a transpiler SKIP, the DRL-text branch as a MARKER, both carrying the one shared sentence ("vouw SOM over de setbron 'vondstdatum' … Optellen is rekenkunde en alleen bedragen tellen op"). That is the SAME judgement ref and clingo pin here: no executor carries the form, and only the two that can carry a fragment do. Listed rather than fragment-pinned for this leg's standing reason — FAULT here means a genuine fault at fire. The entry is NEW because the refusal is: until this card the backend CRASHED on the form (ClassCastException, LocalDate to BigDecimal), and a crash names no form at all
datumsetbron-totaalvouw|EXCLUDE measured 2026-09-20: the TOTAAL twin of the entry above. Both branches ask one shared question before either lowers anything, so the two kinds refuse on the same sentence with their own kind named in it; same phase, same reason
datumtijdlijn-klipvouw|EXCLUDE measured 2026-09-23: the gedurende-clip sibling of the entry below, the same refusal on the same ground — the case loader declines the date-valued segment ("non-numeric timeline value on 'wachtvergoeding per maand' is not carried by the v0 backend") before fire, and the DRL-text branch markers the clipped total through the shared timeline reason ("vouw TOTAAL over de tijdlijn 'wachtvergoeding per maand' van Klipjutter … niet getalachtig") where it used to fail the whole file on compile. No executor carries the form; only the two that can carry a fragment pin one
datumtijdlijn-totaalvouw|EXCLUDE measured 2026-09-23: the §7.1 own-subject TOTAAL over a DATE-valued timeline. The case loader declines the segment by name one phase before fire ("non-numeric timeline value on 'bewaarvergoeding per maand' is not carried by the v0 backend — the generated Segment holds a BigDecimal waarde"), and the DRL-text branch — which until this card failed the whole file on compile ("Unable to Analyse Expression if (rsTijdTotaal(…") — now markers the form through the timeline half of the reason the set-source arm shares. That is the SAME judgement ref and clingo pin: a temporal total multiplies each segment value, and a date is not an amount. Listed rather than fragment-pinned for this leg's standing reason: FAULT here means a genuine fault at fire
decimal-nonterminating|NOFAULT the v0 backend has no §6.1.2 write-precision guard — a non-terminating quotient collapses to DECIMAL64 (Breuk.naarDecimal) instead of faulting; it runs to a model
division-by-numeric-zero|EXCLUDE the reaching rule (vindersloon van een vondst) does not lower — a transpiler SKIP (waarde buiten scope); fire is never reached
finite-chain-over-budget|NOFAULT the self-typed generative chain now RUNS (2026-08-30: the creation's symbolic verbindendeRol binding rides beside the model, so the back-link resolves and both ends are written) — and it terminates: the chain is FINITE at rank < 105, so 104 generations are minted and the session reaches a fixpoint well inside the 100k fire cap. The v0 backend has no ch. 5.5 GENERATION budget, the boundary this case exists to pin; ref exhausts its pass budget and clingo its generation depth where Drools simply finishes. Its runaway twin (runaway-recursion) has no such end and IS fault-pinned, so the cap itself is fenced
input-overprecision|NOFAULT the v0 case runner does not enforce declared attribute input precision — it accepts the over-precise value and runs to a model
input-overprecision-param|NOFAULT the v0 case runner does not enforce declared parameter input precision — it accepts the over-precise value and runs to a model
list-element-overprecision|EXCLUDE the v0 loader refuses the model's record datatype (StructType) before any list-element precision is reached — the record/struct form is not carried
near-side-rolcheck-to-many|EXCLUDE the rolcheck rule (bemand met jutter) does not lower — a transpiler SKIP (voorwaarde buiten Phase-B scope); fire is never reached
periodevenster-tellende-kwantor|EXCLUDE measured 2026-09-20: the SECOND bound of RUNTIME-SEMANTICS ch. 3.10's v0 mirror note — "refuse the condition under counting quantifiers". The boundary is read off this backend rather than chosen: one verifier-clean document per quantifier showed both branches PLAYING under ALLE and DE and refusing under GEEN, TEN_MINSTE, TEN_HOOGSTE and PRECIES, the DRL-text branch naming the very limit ("TODO periodevergelijking onder een tellende kwantor (§8.4.3)") where the exec branch reports a transpiler SKIP (voorwaarde buiten Phase-B scope). ref and clingo now refuse the same four by name with the phase in the sentence; the carried ALLE is green in all four (golden jutterswet-premievenster). Listed rather than fragment-pinned for this leg's standing reason: FAULT here means a genuine fault at fire
periodevenster-tijdlijnkenmerk|EXCLUDE measured 2026-09-20: a period condition on a TEMPORAL target is ch. 3.10's clip, not the evaluation-date gate, and the clip is specified but built nowhere. This backend classified it correctly before either OCaml executor did — exec "periodevergelijking op tijdsafhankelijk doel — §8.4.3 clip", DRL-text "TODO periodevergelijking op een tijdsafhankelijk doel (§8.4.3)" — while the ASP backend PLAYED the scalar gate on the timeline, because its target test read the ATTRIBUTE granularity table and this target is a CHARACTERISTIC. Both OCaml executors now ask the characteristic table too, each through its own reader, and refuse by name with the phase in the sentence. Listed rather than fragment-pinned: a transpiler refusal is one phase before fire
periodegrens-parameterdatum|EXCLUDE measured 2026-09-20: the bound shape RUNTIME-SEMANTICS ch. 3.10 defers — its v0 mirror note carries a date literal or an own scalar date attribute and leaves the general date expression WF-042 admits to a later card. All four executors refuse this parameter bound and the agreement is unanimous: ref and clingo by name (the pins run to the phase tail, "Declined at evaluation" against "Declined in translation"), the exec branch with a transpiler SKIP (voorwaarde buiten Phase-B scope) and the DRL-text branch naming the very limit — "TODO periodevergelijking-grens 'de peildatum' (§8.4.3)". Listed rather than fragment-pinned for this leg's standing reason: FAULT here means a genuine fault at fire, and a transpiler refusal is one phase before it. The window's CARRIED shapes are green in all four (golden jutterswet-premievenster); the day the general expression lands, this entry goes stale and the gate says so
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
    printf 'check-rood: PASS (%s red cases fault observably in ref and clingo, floor %s; %s pinned fragments cite none of the %s listed bottom sentences)\n' \
        "$n_cases" "$EXPECTED_MIN_ROOD_CASES" "$n_frag" "$n_bodem"
fi
exit "$status"
