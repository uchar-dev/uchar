#!/bin/sh
# The reference-type derivation harness (RUNTIME-SEMANTICS ch. 2.10, "the consumer clause").
#
# A v0 IR node repeated its type on the wire at positions where that type is a COPY of a
# declaration — an attribute reference, a parameter reference, a dimension-cell read, a binder
# read — or a DERIVATION over its operands — an arithmetic node, a Let, a value aggregation. Since L2
# (2026-09-24, B13) the reference positions write no type at all: a copy that cannot be written
# cannot drift, and every consumer takes the type there from the declaration. What stays on the
# wire is the DERIVED annotation, and the clause says a consumer must read the SOURCE — the
# operands' declarations and the ch. 2.3/2.6 rules — not that repetition. This script is what
# holds all three executors to it:
#
#   1. every corpus/golden/**/ir.json is copied into a temporary directory with a systematic
#      LIE grafted onto every twistable type annotation (below);
#   2. ref, clingo and BOTH Drools transpiler branches — the exec-model back-end and the DRL text
#      compiled to a KieBase — replay the golden's ORIGINAL case files against the LYING copy;
#   3. each executor must produce EXACTLY the outcome it produces on the untouched ir.json —
#      the same pinned values, or the same per-form refusal. A twist that changes an answer is
#      an executor reading the repetition instead of the declaration, and the gate goes red
#      with the diverging executor and golden named.
#   4. the goldens that legitimately survive with a DIFFERENT outcome are pinned in the
#      explicit survivor list below, each with the reason its outcome may move. A survivor
#      outside the list is a failure; a listed survivor that no longer diverges, or no longer
#      exists, is a stale entry and also a failure (the check-noop pattern).
#   5. an empty twist set is a failure, never silently green.
#   6. every unit-bearing annotation the twister MEETS must sit either in the twist table below
#      or in the explicit, reason-carrying exception set (where the annotation IS the source).
#      Table and clamp key on the POSITION CLASS, which for a Binary is (node, op) and for an
#      Aggregation or a BuiltinCall (node, kind) — ch. 2.10 splits those node kinds by OPERATOR,
#      not by node.
#      plus/minus/reducedBy/times and the six value aggregations derive their result unit;
#      dividedBy, power, percentageOf, an absolute value, a root and the date/time extractors are
#      ch. 2.10's FOURTH class, where v0 derives nothing. Keying on the node alone would twist a
#      unit-bearing dividedBy as though it sat at a derived position, and then report whatever
#      came out as a consumer defect — a wrong diagnosis where the honest answer
#      is "v0 states no join rule here" (or, when nothing observable moved, silence where the
#      fourth-class witness should have been announced). A position in neither set is one this
#      harness cannot probe, so nothing holds a consumer to the declaration there — the gate
#      fails with the class and the golden named, and says WHICH of the two arrivals it is.
#      That clamp is what keeps a new wire form, and the first fourth-class witness, from
#      entering the corpus with its consumer side silently unmeasured; measured 2026-09-04 it
#      costs nothing (zero of either).
#
# --- what a PASS means, and what it does not ------------------------------------------------
# This harness measures INDIFFERENCE: with every declaration held byte-identical, no executor's
# answer moves when the repetition lies. It does NOT measure NECESSITY — it cannot separate a
# consumer that derives from one that ignores units altogether, because both are indifferent to
# a unit lie. Necessity is pinned elsewhere and must stay there: gates 5 and 6 hold ref and
# clingo to the corpus's ABSOLUTE hand-written expectations, and gate 12 holds the Drools
# backend to exactly those same values, so an executor that dropped unit restatement is red
# there on the UNTOUCHED corpus long before it is green here. Indifference is the half no other
# gate can see, precisely because a lying document is one no other gate may hold.
#
# With ONE exception, and this script carries it: the DRL-TEXT branch has no value gate at all —
# gate 12 replays the exec-model path only. Comparing that branch against its own untouched status
# line would therefore be satisfied by a branch that had stopped restating units altogether, whose
# baseline is simply wrong in both runs. So its BASELINE is pinned here too, for exactly the goldens
# this harness probes (the DRL_BASIS_UIT list below).
#
# Two twist classes, Rounding and Clamp, are for BOTH DROOLS legs a no-op by construction: their Java
# IR records (in memory still Afgerond and Begrensd) DERIVE their type() from the basis and hold no
# wire annotation at all, so the graft cannot reach either of them — it still probes ref and clingo,
# which do read the wire there. The Drools side of those two rests on the EffectieveEenheidWitnessTest
# witnesses, which build the nodes directly. Said out loud so a green line is not read as coverage it
# has not got. (Until L2 the parameter reference and the dimension-cell read were two more such
# classes; since L2 they carry no annotation to twist.)
#
# And the same honesty about HOW MUCH of the two Drools legs a green line covers. On the UNTOUCHED
# corpus the derivation never disagreed with the annotation — WF-091 made the repetition faithful,
# so effType(e) equalled e.type() at every call (measured 2026-09-04 with a StackWalker probe on
# EffectieveEenheid.van, attributing each call to its transpiler frame: zero divergences over every
# replayable golden, in BOTH branches). Since L2 a reference carries no annotation, so there the
# derivation has nothing to disagree with, and WF-091 holds the derived annotations that remain. On
# the LYING copies the same probe showed the derivation overriding the annotation at a handful of
# call sites (measured 2026-09-04 over the 11 twistable replayable goldens, BEFORE L2: the reference
# classes were still in the twist table then, and the two comparison sites below were reached only
# through a twisted reference annotation, which no twist reaches since L2). The list has not been
# re-probed since, so read it as that dated measurement:
#   exec-model branch — 5 sites
#     - the WRITE boundary (a consequence value restated into its doel's declared unit) — 20 hits;
#     - the ADDITIVE OPERAND boundary (the rhs of a +/- restated into the lhs unit) — 14 hits;
#     - the AGGREGATION restatement (a fold's per-element and total unit into the doel's) — 2 hits;
#     - the ATTRIBUTE-vs-ATTRIBUTE comparison (§5.1 Tabel 32 operand restatement) — 2 hits;
#     - the ATTRIBUTE-vs-PARAMETER comparison (the same Tabel 32 restatement against a global) — 1 hit.
#   DRL-text branch — 3 sites
#     - the WRITE boundary (regel, omrekenWaarde over value and doel) — 20 hits;
#     - the ADDITIVE OPERAND boundary (binairTerm) — 10 hits;
#     - the §5.1 Tabel 32 comparison operand restatement (conditie) — 4 hits.
# Every other effType call in either file is a pass-through on this corpus, so its derivation is
# STATED but not measured here. For the DRL branch, measured TRANSPILE-ONLY over every golden the
# loader accepts (118; a replay-based sweep misses the no-case goldens): 12 of its 26 converted call
# sites are REACHED at all, 3 of those ever override. The OTHER 23 rest on the backend's JUnit
# witnesses (gate 12's obligation), not on a green line here, and they are two different facts:
# 9 ARE reached but on this corpus always UNITLESS — the temporal §4.1/§5.1.4 rescalings, the Begrensd
# value arm, the §8.2 timeline comparison and its geheelPeriodeCall sibling (reached only through a
# no-case golden), the two concatenation-aggregation sites — while the remaining 14 no golden
# reaches at all. A leg that stops PLAYING entirely fails the floor below; a leg that merely shrinks
# stays green and only the printed count moves.
#
# THE BEGRENSD ITEM OF THAT LIST LAPSED ON 2026-09-11 and is corrected rather than left to rot. The
# corpus gained jutterswet-vangstklem, its first UNIT-BEARING clamp (a `t` bound over a `kg` base, one
# declared system, factor 1000), so DrlTranspiler's clamp arm —
# `omrekenWaarde(waarde(bound), effType(bound), effType(basis))` — now restates where it used to no-op.
# Measured on that golden and not read off the code: the untouched document PLAYS in the DRL-text
# branch, and relabelling its minimum bound `t` -> `kg` in the IR moves the DRL-text answer from
# 1000,00 to 250,00. So the always-UNITLESS group is one smaller and that arm rests on a golden now,
# not on the JUnit witnesses alone. The four counts of the paragraph above (12 reached, 3 ever
# overriding, 9 unitless, 14 unreached) are NOT restated here: they came from a StackWalker probe run
# on 2026-09-04 over an 11-golden twistable set, and which bucket the clamp arm moved INTO — an
# overriding site, or a reached-but-non-overriding one — was not re-probed. Read them as that dated
# measurement, with this correction on top, and re-probe before quoting them as current.
#
# The DRL leg is NOT a repetition of the exec one. It reaches a golden the exec branch cannot play
# at all (jutterswet-vondsttarief, which the exec transpiler SKIPs as "waarde buiten scope" while the
# text branch lowers and plays it), and its three live sites are different code. Measured by mutation
# on 2026-09-04 — EffectieveEenheid.van() put back on the node's own annotation, the state before the
# derivation existed — the DRL leg alone goes red on NINE of the eleven twistable goldens, including
# that one. The two that stay green under the mutant are the two the text branch markers (the ch. 4.10
# Let: lichterloon, vondstrecht), which is the honest reading of a branch that does not lower them.
#
# The verifier does NOT run here. This harness asks an EXECUTION question — is the outcome a
# function of the declarations? — outside the validity question, exactly as gate 7's --noop
# mode asks its question outside evaluation. The lying copies are deliberately documents the
# verifier would reject under WF-091; that is what makes them a probe.
#
# --- the twist ------------------------------------------------------------------------------
# Facet: the UNIT, and only the unit. Measured (2026-09-04), and the reason each other facet is
# out:
#   - SOORT: a soort lie is a different document — ref routes its ch. 1.2d empty-equality and
#     its date arithmetic on the static soort, so twisting it would measure ref's routing, not
#     a declaration reading. WF-091 crosses it in the verifier instead, at the derived positions
#     that still carry an annotation. Until L2 WF-078 crossed it at a binder read; a BinderRef
#     carries no type since then (B13), so there is nothing to cross there.
#   - PRECISION: the ASP translator sizes its integer encoding off the annotation's decimals
#     (value_scale in backends/clingo/lib/translate.ml), so a precision lie makes clingo
#     overflow or refuse rather than answer — measured: 21 of 402 cases. And it would probe
#     nothing here: the Drools back-ends never read Datatype.decimalen or Datatype.geheel at
#     all, only Afgerond.decimalen, which is a NODE field and not a type annotation.
# So the unit is both the facet all three consumers can be indifferent to and the only one this
# backend reads. The lie: every twistable annotation carrying a unit with a canonical and a
# factor gets a SISTER unit — same canonical, abbreviation suffixed, factorToCanonical
# multiplied by a power of ten fixed PER POSITION CLASS (the exact rational {num, den} with num
# times 10^k, in lowest terms). Same canonical keeps the units
# inter-convertible, so a consumer that trusts the annotation happily restates by the wrong
# factor (a loud wrong number) instead of refusing; distinct per-class factors keep two
# annotations from cancelling each other at a restatement boundary. Nothing else in the
# document moves — every DECLARATION (an object type's attributes, a parameter's datatype, an
# unit system) is left byte-identical, which is precisely what makes the derived answer the
# right answer.
#
# Twisted positions (the class factor in brackets): the composite positions —
#   Binary/{plus,minus,reducedBy,times} [1e5], Let [1e6],
#   Aggregation/{sum,total,minimum,maximum,first,last} [1e7], Rounding [1e8], Clamp [1e9]
#   (the ch. 4.10 Let node reaches this backend as a carrier, and its wire annotation is what the
#   carrier holds). Until L2 the reference positions were twisted too — the attribute reference
#   [10], the parameter reference [100], the dimension-cell read [1000], BinderRef [10000]; since L2
#   they write no type, so the table no longer names them (the L2 card) and the factors above keep
#   their v0 exponents, which leaves every composite twist byte-identical to what it was. Since the
#   L5 landing the table keys on the L5 names of the wire (Binary, Rounding, Clamp, the operators and
#   the aggregation kinds in their one spelling); the classes and their factors did not move.
# The factor stays per NODE KIND even though membership is per (node, op): a restatement boundary
# sits between two node kinds, never between two operators of one kind, so splitting the factor as
# well would buy nothing and would move the twist set the corpus already pins.
#
# NOT twisted, each because the annotation IS the source there (ch. 2.10's named exceptions), and
# each therefore EXEMPT from the clamp of step 6:
#   - a Literal: its type is its own value's carrier ({"number": "3.5"} in EUR), a copy of
#     nothing; the value itself is never twisted. All three executors read it, and must.
#   - the DURATION operand of a date +/- duration Binary: ref (eval.ml) and the ASP translator
#     both route the calendar unit off that operand's static type (WF-035). Twisting it would
#     measure that routing, not a declaration reading. Which operand is the DATE is read the way
#     the executors read it: off the node's own annotation, and — since a reference writes no
#     type (L2) — off the declaration a reference names (attribute, parameter, dimension cell,
#     binder in scope).
#   - a duration / absoluteDuration BuiltinCall's own type: its unit is the OPERATOR's parameter
#     ("de tijdsduur van A tot B in jaren", WF-038), not a derived result type.
#
# Neither twisted nor exempt is NOT silently skipped any more (step 6), and the clamp separates
# two different arrivals:
#   - a ch. 2.10 FOURTH-CLASS branch of a node kind the table otherwise carries — a unit-bearing
#     Binary/dividedBy, /power, /percentageOf, or a BuiltinCall absolute value, root or
#     date/time extractor. v0 derives no result unit there, so a twist would probe a rule that
#     does not exist; the gate reports the witness instead. What that witness must buy is a
#     derivation rule (or a fence) — never a wider licence to read the wire.
#   - a node kind outside the table altogether: every v1 node the corpus does not yet carry with a
#     unit (Fold, Match, Construct, ...) trips on its first unit-bearing witness, and must then be
#     given a twist class or an exception with its reason. NB the ASP translator already reads
#     Fold's and Match's own type for its integer SCALE (value_scale, ASP-TRANSLATION.md S2/S4) —
#     that is the reading the clamp will surface first.
#
# --- the vocabulary leg (IR-SPEC ch. 2.12) ---------------------------------------------------
# The same indifference question for the vocabulary side table: `vocabularies` on the document and
# `designations` / `valueDesignations` on the declarations carry no semantics, so no executor may give
# another answer when they change. Every golden that declares a vocabulary and has case files is
# replayed three more times, each against the golden's own untouched outcome, with the executors and
# the comparison of the unit legs above (ref and clingo exit 0; the two Drools branches print their
# own untouched status line):
#   - STRIPPED: every vocabulary and every designation taken out — the document an executor that reads
#     nothing of the side table sees anyway;
#   - EXCHANGED: the names of the vocabularies rotated one place (two vocabularies change places), in
#     the declarations and in every designation that names them alike. Each set of designations keeps
#     its vocabulary's default mark and language under the other name, so the document stays
#     verifier-clean — a rotation of the designations ALONE would put the default's designations under
#     a vocabulary that may not name every declaration (WF-119). This is the minimal twist, and it is
#     blind to an executor that reads the side table through a vocabulary's role (the default one)
#     rather than its name; the third twist is not;
#   - ROTATED: within each vocabulary and each scope of WF-118 (the kinds without an owner each on
#     their own, the own attributes and the own characteristics of an object type, the roles of a fact
#     type, the values of a domain), the designations move one declaration on, among the declarations
#     that carry one. The multiset of effective names of every scope is unchanged, so WF-118 and WF-119
#     still hold; a rotation that would give a declaration its own canonical name in a vocabulary that is
#     not the default (a redundancy WF-120 wants accounted for) tries the next offset, and a scope with
#     no such offset stays as it is and is not counted. An executor that resolved a name through a
#     designation would now reach another declaration.
# Measured 2026-09-27 on jutterswet-vocabulaires: the three twisted copies are verifier-clean, and
# ref, clingo and both Drools branches reproduce the untouched outcome on each. Measured by mutation the
# same day — the shared OCaml loader made to take an attribute's first designation for its name, an
# executor that reads the side table — the leg goes red on the exchanged and the rotated copies in ref
# and clingo (8 failures over the two cases); the stripped copy stays green, as it must, since it has
# nothing to read (and gates 5 and 6 are red on the untouched golden under that mutant). Unlike the unit twist,
# a divergence here has no survivor list: no reading of the side table is a legitimate one, so every
# divergence fails. A golden without vocabularies is not probed; no golden with them is a failure.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

# --- the survivor list (reason-carrying; fails when stale) -----------------------------------
# Entries are "<golden-dir>|<executor>|<reason>". EMPTY today: measured 2026-09-04, all three
# executors are indifferent over the whole twistable set. An entry here is an admission that one
# executor's answer depends on a repetition rather than a declaration, and must say why that is
# the right reading — not merely that it is the current one.
SURVIVORS=''

# --- the DRL-branch baseline list (reason-carrying; fails when stale) ------------------------
# A twistable golden's DRL-TEXT baseline must be PLAYED (the branch ran and matched the corpus's
# ABSOLUTE expectations — the same ones gates 5, 6 and 12 pin) or EXCLUDE (the branch does not carry
# the form, and says which). Anything else is a value the corpus does not pin anywhere, and must be
# listed here with the reason. Entries are "<golden-dir>|<reason>"; an entry whose golden is gone, or
# whose baseline is back in line, is stale and fails. EMPTY today, measured 2026-09-04: of the 11
# twistable replayable goldens the text branch PLAYS 9 and markers 2 (the ch. 4.10 Let, which it does
# not lower). NB this pins the branch on the twistable subset only — over the WHOLE corpus it has 7
# value divergences and 2 fire-time errors, which is gate-12 territory and deliberately not re-fought
# here.
DRL_BASIS_UIT=''

# --- the DRL-text branch and ch. 5.10: the divergence class is EMPTY (measured 2026-09-07) ---
# This gate owns the DRL-TEXT branch, so it records the branch's relationship to
# RUNTIME-SEMANTICS ch. 5.10. Until 2026-09-07 that relationship was a divergence, and this file
# carried a watchlist over every at-most carrier in the corpus.
#
# THE MAINTAINER DECISION OF 2026-09-07 CLOSED IT. Including at-most's TRUE trigger (`at most d`
# stops once h + r <= d, not only once h > d) made ch. 5.10 exactly the dual of at-least — which is
# what the text branch computes: it renders `at most d` as `!(minstens(members, d+1))`, and
# at-least's own two triggers map one-to-one onto at-most's two sides. The branch had been
# implementing the dual all along; the norm now says the same thing.
#
# REMEASURED over the whole read-set model, both directions, all five quantifiers, every threshold
# (including d > n) and every truth assignment, n = 1..7 — 6646 combinations:
#     verdict divergences vs ch. 5.10 ................. 0
#     executor reads a member ch. 5.10 skips .......... 0   (would be a fault the norm does not have)
#     executor skips a member ch. 5.10 reads .......... 0   (would be an answer where the norm faults)
# for the DRL-text branch AND for clingo. Confirmed end-to-end on the very probe that used to be
# the smoking gun (at most 1 over one member that divides by zero): ref, clingo, the exec-model
# branch and the DRL-text branch now all write 1 — they agree, where three of them used to fault
# and the fourth answered.
#
# So there is no shape left to watch, and the watchlist that stood here is gone rather than left to
# rot. The live check on this branch is the DRL_BASIS_UIT baseline above, which still requires the
# text branch to PLAY or EXCLUDE per twistable golden.
#
# WHAT "0 divergences" DOES AND DOES NOT BUY: the reservations against reading this as a proof — and
# what a second, independent model did and did not confirm — are recorded with the measurement in
# backlog/done/20260901-samengesteld-kortsluiting.md (card Results, the decision round). They live
# there rather than here so they survive the follow-up card being withdrawn.

RUNNER_REF=_build/default/ref/bin/runner.exe
RUNNER_CLINGO=_build/default/backends/clingo/bin/clingo_runner.exe

if ! command -v dune >/dev/null 2>&1; then
    if command -v opam >/dev/null 2>&1; then
        eval "$(opam env)" || exit 2
    fi
fi
command -v dune >/dev/null 2>&1 || {
    echo 'FAIL: need dune (OCaml). Install via opam: opam install dune yojson zarith' >&2; exit 2; }
command -v clingo >/dev/null 2>&1 || { echo 'FAIL: need clingo on PATH' >&2; exit 2; }
command -v java >/dev/null 2>&1 || { echo 'FAIL: need a JDK/JRE 21+' >&2; exit 2; }
command -v mvn >/dev/null 2>&1 || { echo 'FAIL: need mvn (the Drools backend jar is taken fresh or rebuilt here, and only mvn can rebuild a stale one)' >&2; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo 'FAIL: need python3 (the twister)' >&2; exit 2; }

if ! dune build ref backends 2>&1; then
    echo 'check-reftype-derivation: FAIL (ref/clingo build)' >&2
    exit 1
fi
# The jar is taken FRESH OR REBUILT (scripts/lib/drools-jar.sh, 2026-09-23), so the harness can
# never probe a jar that predates the source under review (the gate-12 lesson): it is rebuilt when
# it is absent or older than any path it is built from. The stale-jar flank — a
# build-only-when-absent jar can silently predate the source — stays closed, because the test is
# "older than the source", not "absent". Until then this step rebuilt the jar on every run, with
# -DskipTests as the helper still does: the backend's own JUnit suite is gate 12's obligation, not
# this one's — this harness asks only whether the OUTCOME follows the declarations. The helper also
# demands exactly one shaded jar, where `ls | head -1` used to take the lexicographic first, and
# shows the tail of a red build, which used to go to /dev/null.
. scripts/lib/drools-jar.sh
JAR=$(ensure_drools_jar) || { echo 'check-reftype-derivation: FAIL (no fresh Drools backend jar — the drools-jar line above says why)' >&2; exit 1; }

TWISTER=$(mktemp) || exit 2
VTWISTER=$(mktemp) || exit 2
WORK=$(mktemp -d) || exit 2
LIST=$(mktemp) || exit 2
CASES=$(mktemp) || exit 2
ONGEDEKT=$(mktemp) || exit 2   # "<golden> <node>" per unit-bearing annotation outside table + exceptions
RAKERS=$(mktemp) || exit 2     # "<golden>|<executor>" per survivor entry that ACTUALLY diverged
DRLRAKERS=$(mktemp) || exit 2  # "<golden>" per DRL-baseline entry whose baseline is ACTUALLY off
trap 'rm -rf "$TWISTER" "$VTWISTER" "$WORK" "$LIST" "$CASES" "$ONGEDEKT" "$RAKERS" "$DRLRAKERS"' EXIT

cat > "$TWISTER" <<'PY'
import json, os, sys
from fractions import Fraction

# the position classes and their restatement-factor exponents (see the script header). The exponent
# is per NODE KIND; MEMBERSHIP is per position class, which for Binary/Aggregation is finer (below).
KLASSE = {"Binary": 5, "Let": 6, "Aggregation": 7, "Rounding": 8, "Clamp": 9}
# ch. 2.10 clause 2 splits Binary by OPERATOR and an Aggregation by KIND, not by node: only these
# branches DERIVE a result unit (an additive result carries the left unit / a unitless side adopts
# the other; a times cancels or forms the opaque product; §4.6 Eenheid1 hands a value aggregation
# its first operand's). Every OTHER op/kind of those two node kinds is ch. 2.10's FOURTH class — v0
# derives nothing there, so it is neither twistable nor exempt, and the clamp reports it.
AFGELEIDE_OP = {"plus", "minus", "reducedBy", "times"}
AFGELEIDE_KIND = {"sum", "total", "minimum", "maximum", "first", "last"}
# A BuiltinCall is a node kind the table carries with no derived branch at all: an absolute value,
# a root and the date/time extractors are the fourth class, and the two duration kinds are exempt
# (below). Since L4 it is the other half of what v0 wrote as one Functie node (delta B21).
GEDRAGEN = set(KLASSE) | {"BuiltinCall"}
# The reason-carrying exception set of the step-6 clamp: node kinds OUTSIDE the twist table whose
# annotation IS the source, so leaving them unprobed is a statement and not a gap.
UITZONDERING = {"Literal": "the literal's type carries its own value's unit ({\"number\": \"3.5\"} in EUR), a copy of no declaration"}
# WF-038: a duration's own unit is the operator's parameter, not a derived result type
GEEN_FUNCTIE = {"duration", "absoluteDuration"}
DATUM = "Date"
ACHTERVOEGSEL = "-verdraaid"


def positieklasse(x):
    """The key table and clamp agree on: (node, op) for a Binary, (node, kind) for an Aggregation
       or a BuiltinCall, the bare node kind otherwise. Printed as the clamp's witness, so a report
       names the BRANCH."""
    node = x.get("node")
    if node == "Binary":
        return "Binary/%s" % x.get("op")
    if node in ("Aggregation", "BuiltinCall"):
        return "%s/%s" % (node, x.get("kind"))
    return node


def afgeleid(x):
    """True iff this position's result unit is one ch. 2.10 clause 2 DERIVES — the only positions a
       twist may probe, because a twist asks whether a consumer follows a rule that exists."""
    node = x.get("node")
    if node == "Binary":
        return x.get("op") in AFGELEIDE_OP
    if node == "Aggregation":
        return x.get("kind") in AFGELEIDE_KIND
    return node in KLASSE


class RuweNum:
    """A JSON number with its original lexeme, so untouched numbers re-emit unchanged."""
    __slots__ = ("raw",)

    def __init__(self, s):
        self.raw = s


def rationaal(f):
    """The exact factor {num, den} as a Fraction; anything else is no factor to twist."""
    if not (isinstance(f, dict) and isinstance(f.get("num"), RuweNum)
            and isinstance(f.get("den"), RuweNum)):
        return None
    return Fraction(int(f["num"].raw), int(f["den"].raw))


def zusje(e, exp):
    """A convertible SISTER of unit e: same canonical, distinct abbreviation, factor x 10^exp —
       the rational written {num, den} in lowest terms, as the wire writes every factor."""
    if not isinstance(e, dict):
        return None
    a, c, f = e.get("abbreviation"), e.get("canonical"), rationaal(e.get("factorToCanonical"))
    if a is None or c is None or f is None or ACHTERVOEGSEL in a:
        return None
    z = f * (10 ** exp)
    uit = dict(e)
    uit["abbreviation"] = a + ACHTERVOEGSEL + str(exp)
    uit["factorToCanonical"] = {"num": RuweNum(str(z.numerator)), "den": RuweNum(str(z.denominator))}
    return uit


def verdraai(dt, exp):
    if not isinstance(dt, dict) or dt.get("node") != "Number":
        return dt, None
    u = zusje(dt.get("unit"), exp)
    if u is None:
        return dt, None
    uit = dict(dt)
    uit["unit"] = u
    return uit, "%s %s->%s" % (dt.get("node"), dt["unit"].get("abbreviation"), u["abbreviation"])


# A reference writes no type since L2 (B13): whether an operand is a DATE is read off the
# declaration it names, exactly as the executors read it (RUNTIME-SEMANTICS ch. 2.10). The other
# nodes still carry their own annotation.
DECL = {"attr": {}, "super": {}, "param": {}}


def declaraties(doc):
    model = doc.get("model", {})
    for ot in model.get("objectTypes", []):
        n = ot.get("name")
        DECL["super"][n.lower()] = ot.get("supertype")
        for a in ot.get("attributes", []):
            DECL["attr"][(n.lower(), a.get("name", "").lower())] = a.get("datatype")
    for p in model.get("parameters", []):
        DECL["param"][(p.get("name") or "").lower()] = p.get("datatype")


def gedeclareerd(e, scope):
    node = e.get("node")
    if node == "AttributeReference":
        ot, a = (e.get("subjectType") or "").lower(), (e.get("attribute") or "").lower()
        while ot:
            if (ot, a) in DECL["attr"]:
                return DECL["attr"][(ot, a)]
            ot = (DECL["super"].get(ot) or "").lower()
        return None
    if node == "ParameterReference":
        return DECL["param"].get((e.get("parameter") or "").lower())
    if node == "DimensionCellRead":
        return gedeclareerd(e.get("attribute") or {}, scope)
    if node == "BinderRef":
        return scope.get((e.get("name") or "").lower())
    return e.get("type")


def datum_getypeerd(e, scope):
    if not isinstance(e, dict):
        return False
    t = gedeclareerd(e, scope)
    return isinstance(t, dict) and t.get("node") == DATUM


def eenheiddragend(dt):
    return isinstance(dt, dict) and dt.get("node") == "Number" and dt.get("unit") is not None


def loop(x, telling, sla_eigen_over=False, scope=None):
    scope = {} if scope is None else scope
    if isinstance(x, list):
        for v in x:
            loop(v, telling, scope=scope)
        return
    if not isinstance(x, dict):
        return
    node = x.get("node")
    # WF-035: the duration operand of a date +/- duration carries the calendar unit ref routes on
    duur = None
    if node == "Binary" and x.get("op") in ("plus", "minus", "reducedBy"):
        l, r = x.get("left"), x.get("right")
        if datum_getypeerd(l, scope) and not datum_getypeerd(r, scope):
            duur = r
        elif datum_getypeerd(r, scope) and not datum_getypeerd(l, scope):
            duur = l
    vrijgesteld = sla_eigen_over or node in UITZONDERING \
        or (node == "BuiltinCall" and x.get("kind") in GEEN_FUNCTIE)
    if node is not None and eenheiddragend(x.get("type")):
        # step 6, the clamp: account for EVERY unit-bearing annotation, so a position that is
        # neither twisted nor exempt cannot pass unnoticed. The two arrivals are reported apart:
        # a FOURTH-CLASS branch of a carried node kind (v0 derives no result unit there) and a
        # node kind the table does not carry at all (a v1 form reaching the corpus).
        telling["eenheid"] += 1
        if vrijgesteld:
            telling["vrijgesteld"] += 1
        elif not afgeleid(x):
            telling["vierde" if node in GEDRAGEN else "ongedekt"].append(positieklasse(x))
        elif zusje(x["type"].get("unit"), KLASSE[node]) is None:
            # in the table, but no convertible SISTER exists (a unit with no canonical/factor —
            # a unit system declares no conversion for it). A non-convertible lie would measure
            # refusal routing, not a declaration reading, so it is counted and not twisted.
            telling["zonderzusje"] += 1
    if afgeleid(x) and not vrijgesteld:
        nieuw, hoe = verdraai(x.get("type"), KLASSE[node])
        if hoe:
            x["type"] = nieuw
            telling["treffers"].append(hoe)
    # the binders a node opens are in scope of its body, with the type their binding declares
    binnen = scope
    if node == "Let":
        binnen = dict(scope)
        binnen[(x.get("binder") or "").lower()] = gedeclareerd(x.get("value") or {}, scope)
    elif node in ("Lambda", "FunctionDef"):
        binnen = {(p.get("name") or "").lower(): p.get("datatype") for p in x.get("parameters", [])}
    elif node == "FilterList":
        # the filter binder reads each element of the list, so its type is the list's element type
        binnen = dict(scope)
        lijst = gedeclareerd(x.get("list") or {}, scope)
        binnen[(x.get("binder") or "").lower()] = \
            lijst.get("element") if isinstance(lijst, dict) else None
    for k, v in list(x.items()):
        if k == "type":        # a DECLARATION never sits under an expression's own type
            continue
        if node == "Match" and k == "arms":
            # an arm binder carries its constructor's declared payload, off the scrutinee's type
            st = gedeclareerd(x.get("scrutinee") or {}, scope)
            ctors = {(c.get("name") or "").lower(): c.get("payload")
                     for c in (st.get("constructors", []) if isinstance(st, dict) else [])}
            for arm in v if isinstance(v, list) else []:
                b = arm.get("binder") if isinstance(arm, dict) else None
                armscope = dict(scope)
                if b:
                    armscope[b.lower()] = ctors.get((arm.get("constructor") or "").lower())
                loop(arm, telling, scope=armscope)
            continue
        loop(v, telling, sla_eigen_over=(v is duur),
             scope=binnen if (node == "Let" and k == "body")
             or (node == "FilterList" and k == "condition") or node in ("Lambda", "FunctionDef")
             else scope)


def stukken(o):
    if isinstance(o, RuweNum):
        yield o.raw
    elif o is None:
        yield "null"
    elif o is True:
        yield "true"
    elif o is False:
        yield "false"
    elif isinstance(o, str):
        yield json.dumps(o, ensure_ascii=False)
    elif isinstance(o, dict):
        yield "{"
        eerste = True
        for k, v in o.items():
            if not eerste:
                yield ","
            eerste = False
            yield json.dumps(k, ensure_ascii=False)
            yield ":"
            for c in stukken(v):
                yield c
        yield "}"
    elif isinstance(o, list):
        yield "["
        eerste = True
        for v in o:
            if not eerste:
                yield ","
            eerste = False
            for c in stukken(v):
                yield c
        yield "]"
    else:
        raise TypeError(repr(o))


bron, doel, naam = sys.argv[1], sys.argv[2], sys.argv[3]
with open(bron, encoding="utf-8") as fh:
    doc = json.load(fh, parse_int=RuweNum, parse_float=RuweNum)
declaraties(doc)
telling = {"treffers": [], "ongedekt": [], "vierde": [], "eenheid": 0, "vrijgesteld": 0,
           "zonderzusje": 0}
loop(doc, telling)
map_ = os.path.dirname(doel)
if map_:
    os.makedirs(map_, exist_ok=True)
with open(doel, "w", encoding="utf-8") as fh:
    for c in stukken(doc):
        fh.write(c)
# line 1: the four counters the shell sums; then one TAGGED line per uncovered occurrence (the
# clamp), the tag separating ch. 2.10's fourth class from a node kind the table does not carry.
print("%d %d %d %d" % (len(telling["treffers"]), telling["eenheid"],
                       telling["vrijgesteld"], telling["zonderzusje"]))
for klasse in telling["vierde"]:
    print("vierde %s %s" % (naam, klasse))
for klasse in telling["ongedekt"]:
    print("onbekend %s %s" % (naam, klasse))
PY

cat > "$VTWISTER" <<'PY'
import json, os, sys

# The three copies of the vocabulary leg (see the script header). Numbers keep their lexemes: the
# copies differ from the golden in the side table and in nothing else.


class RuweNum:
    __slots__ = ("raw",)

    def __init__(self, s):
        self.raw = s


def schrijf(o, pad):
    def stuk(o):
        if isinstance(o, RuweNum):
            return o.raw
        if isinstance(o, dict):
            return "{" + ",".join(json.dumps(k, ensure_ascii=False) + ":" + stuk(v) for k, v in o.items()) + "}"
        if isinstance(o, list):
            return "[" + ",".join(stuk(v) for v in o) + "]"
        return json.dumps(o, ensure_ascii=False)
    with open(pad, "w", encoding="utf-8") as fh:
        fh.write(stuk(o))


def lees(pad):
    with open(pad, encoding="utf-8") as fh:
        return json.load(fh, parse_int=RuweNum, parse_float=RuweNum)


def dragers(doc):
    """Every node that may carry designations, with its WF-118 scope and its canonical name."""
    uit = []
    m, r = doc["model"], doc["rules"]
    for ot in m.get("objectTypes", []):
        uit.append((("objectTypes",), ot.get("name"), ot))
        for a in ot.get("attributes", []):
            uit.append((("attributes", ot.get("name")), a.get("name"), a))
        for c in ot.get("characteristics", []):
            uit.append((("characteristics", ot.get("name")), c.get("name"), c))
    for ft in m.get("factTypes", []):
        uit.append((("factTypes",), ft.get("name"), ft))
        for rol in ft.get("roles", []):
            uit.append((("roles", ft.get("name")), rol.get("name"), rol))
    for p in m.get("parameters", []):
        uit.append((("parameters",), p.get("name"), p))
    for lijst in ("rules", "objectCreations", "allocations", "consistencies", "uniquenesses",
                  "factCreations", "prioritizedDefaults"):
        for f in r.get(lijst, []):
            uit.append((("forms",), f.get("name"), f))
    for g in r.get("ruleGroups", []):
        uit.append((("ruleGroups",), g.get("name"), g))
    return uit


def waardeTabellen(doc):
    """The enumeration declarations that carry value designations, by domain."""
    for d in doc["model"].get("domains", []):
        dt = d.get("datatype") or {}
        if dt.get("node") == "Enumeration" and "valueDesignations" in dt:
            yield d.get("name"), dt


bron, doel = sys.argv[1], sys.argv[2]
if not (lees(bron).get("vocabularies") or []):
    print("0 0 0 0")
    sys.exit(0)
os.makedirs(doel, exist_ok=True)

# STRIPPED
kaal = lees(bron)
del kaal["vocabularies"]
gestript = 0
for _, _, n in dragers(kaal):
    gestript += len(n.pop("designations", []))
for _, dt in waardeTabellen(kaal):
    gestript += len(dt.pop("valueDesignations"))
schrijf(kaal, os.path.join(doel, "gestript.json"))

# EXCHANGED: vocabulary i takes the name of vocabulary i+1, in the declarations and the designations
wissel = lees(bron)
namen = [v["name"] for v in wissel["vocabularies"]]
nieuw = {n.lower(): namen[(i + 1) % len(namen)] for i, n in enumerate(namen)}
verwisseld = 0
for v in wissel["vocabularies"]:
    v["name"] = nieuw[v["name"].lower()]
for _, _, n in dragers(wissel):
    for b in n.get("designations", []):
        b["vocabulary"] = nieuw.get(b["vocabulary"].lower(), b["vocabulary"])
        verwisseld += 1
for _, dt in waardeTabellen(wissel):
    for b in dt["valueDesignations"]:
        b["vocabulary"] = nieuw.get(b["vocabulary"].lower(), b["vocabulary"])
        verwisseld += 1
schrijf(wissel, os.path.join(doel, "verwisseld.json"))

# ROTATED: per vocabulary and scope, the designations one declaration on
rot = lees(bron)
standaard = {v["name"].lower() for v in rot["vocabularies"] if v.get("default")}
geroteerd = 0


def roteer(voc, plekken):
    """plekken: (canonical name, designation record) of one scope and vocabulary, in document order.
       The first offset that gives no declaration its own canonical name outside the default wins."""
    global geroteerd
    if len(plekken) < 2:
        return
    teksten = [b["designation"] for _, b in plekken]
    for k in range(1, len(plekken)):
        verschoven = teksten[-k:] + teksten[:-k]
        if voc in standaard or all(t != c for (c, _), t in zip(plekken, verschoven)):
            for (_, b), t in zip(plekken, verschoven):
                if b["designation"] != t:
                    b["designation"] = t
                    geroteerd += 1
            return


scopes = {}
for scope, canon, n in dragers(rot):
    for b in n.get("designations", []):
        scopes.setdefault((b["vocabulary"].lower(), scope), []).append((canon, b))
for dom, dt in waardeTabellen(rot):
    for b in dt["valueDesignations"]:
        scopes.setdefault((b["vocabulary"].lower(), ("values", dom)), []).append((b["value"], b))
for (voc, _), plekken in scopes.items():
    roteer(voc, plekken)
schrijf(rot, os.path.join(doel, "geroteerd.json"))
print("1 %d %d %d" % (gestript, verwisseld, geroteerd))
PY

# Is <golden> x <executor> a pinned survivor? Read LINE by line, never word by word: a
# reason carries spaces, so splitting $SURVIVORS on whitespace would shred every entry the
# moment the list stops being empty.
vermeld() {
    treffer=$(printf '%s\n' "$SURVIVORS" | while IFS= read -r s; do
        [ -n "$s" ] || continue
        [ "${s%%|*}" = "$1" ] || continue
        rest=${s#*|}
        [ "${rest%%|*}" = "$2" ] && echo hit
    done)
    [ -n "$treffer" ]
}

# The same read for the DRL baseline list, which is keyed on the golden alone.
basisvrij() {
    treffer=$(printf '%s\n' "$DRL_BASIS_UIT" | while IFS= read -r s; do
        [ -n "$s" ] || continue
        [ "${s%%|*}" = "$1" ] && echo hit
    done)
    [ -n "$treffer" ]
}

# --- monotone floors (maintainer decision 2026-09-09) --------------------------------------------
# Lower bounds, never equalities: they catch SHRINK — the harness that quietly probes half — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
# Both counts are this harness's discriminating power, not a file count: the twist total is what
# the twist TABLE meets in the corpus, so a narrowed table would shrink it with every corpus file
# still in place, and no other gate would see it. The DRL floor extends the "-eq 0" bottom below,
# whose own comment records the measured mutant that dropped the leg from 9 played goldens to 0:
# that mutant would today be caught at 8, not only at 0.
EXPECTED_MIN_TWISTS=38      # 2026-09-24: measured 38 annotations twisted over 21 goldens. LOWERED
                            # from 74 (2026-09-10) in the L2 commit that took the four reference
                            # classes out of the twist table (the L2 card,
                            # backlog/done/20260923-l2-referentielaag.md): a reference writes no type since
                            # B13, so there is nothing left to twist there; the composite twists
                            # are untouched (same 38 before and after the table change)
EXPECTED_MIN_DRL_PLAYED=20  # 2026-09-24: measured 20 goldens PLAYED by the DRL-text branch, on the
                            # corpus and the jar of the L2 landing. RAISED from 13 (measured
                            # 2026-09-10): a floor catches shrink only below itself, and at 13 the
                            # leg could lose seven PLAYED goldens without this gate failing. The
                            # growth from 13 to 20 came with the goldens and lowerings that landed
                            # between the two dates; 20 is the count measured now, not a target

# The vocabulary leg's floors (see the script header), measured 2026-09-27 at the card that brought the
# side table, on its one golden jutterswet-vocabulaires: a golden count, so a leg that stops finding
# the goldens it probes fails above zero, and the designations the stripped and the rotated copies
# move, so a twister that quietly moves nothing fails too (an exchange moves every designation, so its
# count is the stripped one and needs no floor of its own).
EXPECTED_MIN_VOCABULAIRE_GOLDENS=1     # 2026-09-27: measured 1 golden with vocabularies and case files
EXPECTED_MIN_VOCABULAIRE_GESTRIPT=46   # 2026-09-27: measured 46 designations stripped (23 declarations x 2 vocabularies)
EXPECTED_MIN_VOCABULAIRE_GEROTEERD=36  # 2026-09-27: measured 36 designations rotated to another declaration

status=0
n_goldens=0
n_twists=0
n_gecontroleerd=0
n_survivors=0
n_drl=0
n_eenheid=0
n_vrijgesteld=0
n_zonderzusje=0
find corpus/golden -name ir.json 2>/dev/null | sort > "$LIST"
while IFS= read -r ir; do
    dir=$(dirname "$ir")
    naam=${dir#corpus/golden/}
    tw="$WORK/$dir/ir.json"
    uit=$(python3 "$TWISTER" "$ir" "$tw" "$naam") || { echo "FAIL: the twister could not read $ir" >&2; exit 2; }
    kop=$(printf '%s\n' "$uit" | head -1)
    # the clamp's accounting runs over EVERY golden, replayable or not: an unprobed node kind is a
    # gap wherever it sits.
    staart=$(printf '%s\n' "$uit" | tail -n +2)
    [ -z "$staart" ] || printf '%s\n' "$staart" >> "$ONGEDEKT"
    set -- $kop
    treffers=$1
    n_eenheid=$((n_eenheid + $2))
    n_vrijgesteld=$((n_vrijgesteld + $3))
    n_zonderzusje=$((n_zonderzusje + $4))
    [ "$treffers" -gt 0 ] || continue          # no twistable annotation: nothing to probe here
    find "$dir" -maxdepth 1 -name 'case*.json' 2>/dev/null | sort > "$CASES"
    [ -s "$CASES" ] || continue                # no case file: not replayable (gate 5 lists those)
    n_goldens=$((n_goldens + 1))
    n_twists=$((n_twists + treffers))

    # ref and clingo: exit 0 on the untouched ir.json is what gates 5 and 6 pin, so the twisted
    # run must exit 0 too. Any other outcome is the executor reading the repetition.
    while IFS= read -r case; do
        n_gecontroleerd=$((n_gecontroleerd + 2))
        if ! rapport=$("$RUNNER_REF" "$tw" "$case" 2>&1); then
            if vermeld "$naam" ref; then
                n_survivors=$((n_survivors + 1)); echo "$naam|ref" >> "$RAKERS"
            else
                printf 'FAIL: %s — ref answers differently on the lying copy:\n%s\n' "$case" "$rapport" >&2
                status=1
            fi
        fi
        if ! rapport=$("$RUNNER_CLINGO" "$tw" "$case" 2>&1); then
            if vermeld "$naam" clingo; then
                n_survivors=$((n_survivors + 1)); echo "$naam|clingo" >> "$RAKERS"
            else
                printf 'FAIL: %s — clingo answers differently on the lying copy:\n%s\n' "$case" "$rapport" >&2
                status=1
            fi
        fi
    done < "$CASES"

    # Drools: compare its own status line on the lying copy with the one it prints on the
    # untouched file. Comparing against the BASELINE rather than against PLAYED is deliberate —
    # a golden gate 12 auto-refuses per form, or carries in its value-divergence list, must stay
    # exactly as refusing/diverging under the twist, and this harness must not re-litigate that.
    set --
    while IFS= read -r case; do set -- "$@" "$case"; done < "$CASES"
    n_gecontroleerd=$((n_gecontroleerd + 1))
    basis=$(java -jar "$JAR" "$ir" "$@" 2>/dev/null | tail -1)
    verdraaid=$(java -jar "$JAR" "$tw" "$@" 2>/dev/null | tail -1)
    if [ "$basis" != "$verdraaid" ]; then
        if vermeld "$naam" drools; then
            n_survivors=$((n_survivors + 1)); echo "$naam|drools" >> "$RAKERS"
        else
            printf 'FAIL: %s — Drools answers differently on the lying copy:\n  untouched: %s\n  lying:     %s\n' \
                "$naam" "$basis" "$verdraaid" >&2
            status=1
        fi
    fi

    # The SECOND Drools transpiler branch, through the same twist: DrlTranspiler emits DRL TEXT, which the
    # runner's --drl entry compiles to a KieBase (the classic DRL build) and replays against the SAME pinned
    # expectations. Same baseline discipline as the exec leg — its OWN untwisted status line, never PLAYED —
    # and for the same reason, twice over: the text branch is NARROWER than the exec one (it has no ch. 7
    # prioritizedDefaults lowering, and it markers constructs the exec branch lowers, the ch. 4.10 Let among
    # them), and on a handful of goldens it also answers differently. Neither is this harness's question. What
    # IS its question is whether the answer it does give follows the declarations, and for that a refusal that
    # stays exactly as refusing is a perfectly good observation.
    n_gecontroleerd=$((n_gecontroleerd + 1))
    dbasis=$(java -jar "$JAR" --drl "$ir" "$@" 2>/dev/null | tail -1)
    dverdraaid=$(java -jar "$JAR" --drl "$tw" "$@" 2>/dev/null | tail -1)
    dkop=${dbasis%% *}
    if [ "$dkop" = PLAYED ]; then
        n_drl=$((n_drl + 1))
    elif [ "$dkop" != EXCLUDE ]; then
        # the baseline pin: nothing else holds this branch to the corpus's absolute values
        if basisvrij "$naam"; then
            echo "$naam" >> "$DRLRAKERS"
        else
            printf 'FAIL: %s — the DRL-text branch neither played nor refused the UNTOUCHED golden, so its\n      value is pinned by nothing:\n  %s\n' \
                "$naam" "$dbasis" >&2
            status=1
        fi
    fi
    if [ "$dbasis" != "$dverdraaid" ]; then
        if vermeld "$naam" drl; then
            n_survivors=$((n_survivors + 1)); echo "$naam|drl" >> "$RAKERS"
        else
            printf 'FAIL: %s — the DRL-text branch answers differently on the lying copy:\n  untouched: %s\n  lying:     %s\n' \
                "$naam" "$dbasis" "$dverdraaid" >&2
            status=1
        fi
    fi
done < "$LIST"

# --- the vocabulary leg (see the script header) ---------------------------------------------------
n_vgoldens=0
n_vgestript=0
n_vverwisseld=0
n_vgeroteerd=0
n_vreplays=0
while IFS= read -r ir; do
    dir=$(dirname "$ir")
    naam=${dir#corpus/golden/}
    vdir="$WORK/vocabulaire/$naam"
    kop=$(python3 "$VTWISTER" "$ir" "$vdir") || { echo "FAIL: the vocabulary twister could not read $ir" >&2; exit 2; }
    set -- $kop
    [ "$1" -eq 1 ] || continue                 # no vocabulary: nothing to probe here
    find "$dir" -maxdepth 1 -name 'case*.json' 2>/dev/null | sort > "$CASES"
    [ -s "$CASES" ] || continue                # no case file: not replayable
    n_vgoldens=$((n_vgoldens + 1))
    n_vgestript=$((n_vgestript + $2))
    n_vverwisseld=$((n_vverwisseld + $3))
    n_vgeroteerd=$((n_vgeroteerd + $4))
    set --
    while IFS= read -r case; do set -- "$@" "$case"; done < "$CASES"
    basis=$(java -jar "$JAR" "$ir" "$@" 2>/dev/null | tail -1)
    dbasis=$(java -jar "$JAR" --drl "$ir" "$@" 2>/dev/null | tail -1)
    for variant in gestript verwisseld geroteerd; do
        vir="$vdir/$variant.json"
        while IFS= read -r case; do
            n_vreplays=$((n_vreplays + 2))
            if ! rapport=$("$RUNNER_REF" "$vir" "$case" 2>&1); then
                printf 'FAIL: %s — ref answers differently on the %s copy (vocabulary leg):\n%s\n' "$case" "$variant" "$rapport" >&2
                status=1
            fi
            if ! rapport=$("$RUNNER_CLINGO" "$vir" "$case" 2>&1); then
                printf 'FAIL: %s — clingo answers differently on the %s copy (vocabulary leg):\n%s\n' "$case" "$variant" "$rapport" >&2
                status=1
            fi
        done < "$CASES"
        n_vreplays=$((n_vreplays + 2))
        v=$(java -jar "$JAR" "$vir" "$@" 2>/dev/null | tail -1)
        if [ "$v" != "$basis" ]; then
            printf 'FAIL: %s — Drools answers differently on the %s copy (vocabulary leg):\n  untouched: %s\n  %s: %s\n' \
                "$naam" "$variant" "$basis" "$variant" "$v" >&2
            status=1
        fi
        dv=$(java -jar "$JAR" --drl "$vir" "$@" 2>/dev/null | tail -1)
        if [ "$dv" != "$dbasis" ]; then
            printf 'FAIL: %s — the DRL-text branch answers differently on the %s copy (vocabulary leg):\n  untouched: %s\n  %s: %s\n' \
                "$naam" "$variant" "$dbasis" "$variant" "$dv" >&2
            status=1
        fi
    done
done < "$LIST"
if [ "$n_vgoldens" -eq 0 ]; then
    echo 'FAIL: no golden declares a vocabulary — the vocabulary leg probed nothing (an empty set is never green)' >&2
    exit 1
fi
if [ "$n_vgoldens" -lt "$EXPECTED_MIN_VOCABULAIRE_GOLDENS" ]; then
    echo "FAIL: the vocabulary leg probed $n_vgoldens golden(s), but EXPECTED_MIN_VOCABULAIRE_GOLDENS pins at least $EXPECTED_MIN_VOCABULAIRE_GOLDENS — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi
if [ "$n_vgestript" -lt "$EXPECTED_MIN_VOCABULAIRE_GESTRIPT" ]; then
    echo "FAIL: the vocabulary leg stripped $n_vgestript designation(s), but EXPECTED_MIN_VOCABULAIRE_GESTRIPT pins at least $EXPECTED_MIN_VOCABULAIRE_GESTRIPT — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi
if [ "$n_vgeroteerd" -lt "$EXPECTED_MIN_VOCABULAIRE_GEROTEERD" ]; then
    echo "FAIL: the vocabulary leg rotated $n_vgeroteerd designation(s), but EXPECTED_MIN_VOCABULAIRE_GEROTEERD pins at least $EXPECTED_MIN_VOCABULAIRE_GEROTEERD — the rotation moves less than it did (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi

# The survivor list, BOTH halves (the check-noop pattern): an entry whose golden is gone is stale,
# and so is an entry that no longer diverges — it then admits an exception nothing needs any more,
# which is exactly how a list stops describing the system it pins.
weg=$(printf '%s\n' "$SURVIVORS" | while IFS= read -r s; do
    [ -n "$s" ] || continue
    g=${s%%|*}
    rest=${s#*|}
    ex=${rest%%|*}
    # (no `case` here: /bin/sh on macOS is bash 3.2, whose parser cannot read one inside $(...))
    if [ "$ex" != ref ] && [ "$ex" != clingo ] && [ "$ex" != drools ] && [ "$ex" != drl ]; then
        echo "  $g|$ex — unknown executor (expected ref, clingo, drools or drl)"
    elif [ ! -d "corpus/golden/$g" ]; then
        echo "  $g|$ex — no such golden"
    elif ! grep -qxF "$g|$ex" "$RAKERS" 2>/dev/null; then
        echo "  $g|$ex — no longer diverges under the twist; drop the entry"
    fi
done)
if [ -n "$weg" ]; then
    printf 'FAIL: stale survivor entry:\n%s\n' "$weg" >&2
    status=1
fi

# The DRL baseline list, the same both halves.
drlweg=$(printf '%s\n' "$DRL_BASIS_UIT" | while IFS= read -r s; do
    [ -n "$s" ] || continue
    g=${s%%|*}
    if [ ! -d "corpus/golden/$g" ]; then
        echo "  $g — no such golden"
    elif ! grep -qxF "$g" "$DRLRAKERS" 2>/dev/null; then
        echo "  $g — the DRL-text branch plays or refuses it again; drop the entry"
    fi
done)
if [ -n "$drlweg" ]; then
    printf 'FAIL: stale DRL-baseline entry:\n%s\n' "$drlweg" >&2
    status=1
fi

if [ "$n_twists" -eq 0 ]; then
    echo 'FAIL: no annotation was twisted — the harness probed nothing (an empty twist set is never green)' >&2
    exit 1
fi
if [ "$n_twists" -lt "$EXPECTED_MIN_TWISTS" ]; then
    echo "FAIL: $n_twists annotation(s) twisted, but EXPECTED_MIN_TWISTS pins at least $EXPECTED_MIN_TWISTS — the probe shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi

# The same bottom for the DRL leg, and it is NOT implied by the baseline clamp above. That clamp demands
# PLAYED or EXCLUDE per golden, so a text branch that REFUSED EVERY golden would satisfy it for all of
# them, be indifferent by construction (both runs refuse identically), and leave the leg silently green
# while measuring nothing at all. Measured 2026-09-04 with exactly that mutant — a refusal at the front of
# CaseRunner's DRL branch: the leg fell from 9 played goldens to 0 and the gate still said PASS. NB the
# count is the STATUS HEAD, so a golden whose every case file carried expectedFault would replay nothing
# and still print "PLAYED 0", counting here — the corpus zone discipline gate 4 pins (golden carries
# expected, rood carries expectedFault) makes that shape unreachable today, and this bottom must not come
# to lean on it.
if [ "$n_drl" -eq 0 ]; then
    echo 'FAIL: the DRL-text branch PLAYED no golden — the leg measured nothing (an empty played set is never green)' >&2
    exit 1
fi
if [ "$n_drl" -lt "$EXPECTED_MIN_DRL_PLAYED" ]; then
    echo "FAIL: the DRL-text branch PLAYED $n_drl golden(s), but EXPECTED_MIN_DRL_PLAYED pins at least $EXPECTED_MIN_DRL_PLAYED — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi

n_vierde=$(awk '$1 == "vierde" {n++} END {print n+0}' "$ONGEDEKT")
n_onbekend=$(awk '$1 == "onbekend" {n++} END {print n+0}' "$ONGEDEKT")
n_ongedekt=$((n_vierde + n_onbekend))
if [ "$n_vierde" -gt 0 ]; then
    echo 'FAIL: a unit-bearing annotation sits at a node kind whose result unit v0 does not derive —' >&2
    echo '      the §2.10 fourth-class witness has arrived. The node kind is one the twist table carries,' >&2
    echo '      but THIS operator/kind branch has no join rule (dividedBy, power, percentageOf, an' >&2
    echo '      absolute value, a root, a date/time extractor), so a twist here would' >&2
    echo '      probe a rule that does not exist and report its absence as a consumer defect. What the' >&2
    echo '      witness must buy is a derivation rule (or a fence) — never a wider licence to read the' >&2
    echo '      wire. ref/clingo and the Drools backend answer such a position DIFFERENTLY today (ch.' >&2
    echo '      2.10), so the divergence is now observable and must be settled:' >&2
    awk '$1 == "vierde" {print "  " $2 " " $3}' "$ONGEDEKT" | sort | uniq -c >&2
    status=1
fi
if [ "$n_onbekend" -gt 0 ]; then
    echo 'FAIL: a unit-bearing annotation sits at a node kind the twist table does not carry, and that' >&2
    echo '      the exception set does not exempt — this harness probes nothing there, so no consumer is' >&2
    echo '      held to the declaration at that position. Give the kind a twist class (with its own' >&2
    echo '      factor exponent) or an exception with the reason its annotation IS the source:' >&2
    awk '$1 == "onbekend" {print "  " $2 " " $3}' "$ONGEDEKT" | sort | uniq -c >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    printf '  NOTE (ch. 5.10): the DRL-TEXT branch no longer diverges from ch. 5.10. The maintainer\n'
    printf '       decision of 2026-09-07 added at-most'"'"'s TRUE trigger, which makes the norm the exact dual\n'
    printf '       of at-least — and that dual is what the branch already computed. Remeasured over the full\n'
    printf '       read-set model (n = 1..7, 6646 combinations, both directions, all five quantifiers):\n'
    printf '       0 verdict divergences, 0 extra reads, 0 skipped reads, for the text branch and for clingo.\n'
    printf '       The former watchlist is retired; see the comment above.\n'
    printf 'check-reftype-derivation: PASS (%s unit annotations twisted over %s goldens, floor %s; %s executor replays reproduce the untouched outcome, %s of them PLAYED by the DRL-text branch, floor %s; %s pinned survivors)\n' \
        "$n_twists" "$n_goldens" "$EXPECTED_MIN_TWISTS" "$n_gecontroleerd" "$n_drl" \
        "$EXPECTED_MIN_DRL_PLAYED" "$n_survivors"
    printf '  vocabulary leg: %s golden(s) with vocabularies, floor %s; %s designations stripped (floor %s), %s exchanged, %s rotated (floor %s); %s executor replays reproduce the untouched outcome\n' \
        "$n_vgoldens" "$EXPECTED_MIN_VOCABULAIRE_GOLDENS" "$n_vgestript" "$EXPECTED_MIN_VOCABULAIRE_GESTRIPT" \
        "$n_vverwisseld" "$n_vgeroteerd" "$EXPECTED_MIN_VOCABULAIRE_GEROTEERD" "$n_vreplays"
    printf '  corpus-wide accounting: %s unit-bearing annotations = %s twistable + %s exempt (the annotation IS the source) + %s without a convertible sister + %s at a §2.10 fourth-class branch (v0 derives no result unit) + %s outside the twist table\n' \
        "$n_eenheid" "$((n_eenheid - n_vrijgesteld - n_zonderzusje - n_ongedekt))" \
        "$n_vrijgesteld" "$n_zonderzusje" "$n_vierde" "$n_onbekend"
fi
exit "$status"
