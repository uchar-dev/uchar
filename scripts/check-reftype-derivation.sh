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
#   2. ref, clingo and BOTH Drools transpiler branches — the exec-model branch (since card
#      tms-omschakeling the lowering with truth maintenance) and the DRL text compiled to a
#      KieBase — replay the golden's ORIGINAL case files against the LYING copy;
#   3. each executor must produce EXACTLY the outcome it produces on the untouched ir.json —
#      the same pinned values, or the same per-form refusal. A twist that changes an answer is
#      an executor reading the repetition instead of the declaration, and the gate goes red
#      with the diverging executor and golden named. A Drools run on the untouched ir.json that
#      ends in no status line of the runner fails the gate by golden, leg and executor, before
#      any comparison (drools_status below).
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
#      plus/minus/reducedBy/times/dividedBy/dividedByAbs/percentageOf and the six value aggregations
#      derive their result unit; power, an absolute value, a root and the date/time extractors are
#      ch. 2.10's FOURTH class, where v0 derives nothing. Keying on the node alone would twist a
#      unit-bearing power as though it sat at a derived position, and then report whatever
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
# The DRL-TEXT branch is the second rendering of the analysis the exec-model branch fires (card
# drl-teksttak-op-truth-maintenance), and gate 12 holds its every status line to the exec-model
# branch's over the whole corpus. Here its UNTOUCHED line must be the exec-model branch's untouched
# line as well (so a text branch that refused everything, or that stopped restating units, is red
# here on the untouched golden), and its twisted line its own untouched one.
#
# Two twist classes, Rounding and Clamp, are for BOTH DROOLS legs a no-op by construction: their Java
# IR records (in memory still Afgerond and Begrensd) DERIVE their type() from the basis and hold no
# wire annotation at all, so the graft cannot reach either of them — it still probes ref and clingo,
# which do read the wire there. The Drools side of those two rests on the EffectiveUnitWitnessTest
# witnesses, which build the nodes directly. Said out loud so a green line is not read as coverage it
# has not got. (Until L2 the parameter reference and the dimension-cell read were two more such
# classes; since L2 they carry no annotation to twist.)
#
# And the same honesty about HOW MUCH of the two Drools legs a green line covers. On the UNTOUCHED
# corpus the derivation never disagreed with the annotation — WF-091 made the repetition faithful,
# so effType(e) equalled e.type() at every call (measured 2026-09-04 with a StackWalker probe on
# EffectiveUnit.of, attributing each call to its transpiler frame: zero divergences over every
# replayable golden, in BOTH branches). Since L2 a reference carries no annotation, so there the
# derivation has nothing to disagree with, and WF-091 holds the derived annotations that remain. On
# the LYING copies the same probe showed the derivation overriding the annotation at a handful of
# call sites (measured 2026-09-04 over the 11 twistable replayable goldens, BEFORE L2: the reference
# classes were still in the twist table then, and the two comparison sites below were reached only
# through a twisted reference annotation, which no twist reaches since L2). The list has not been
# re-probed since, so read it as that dated measurement (its exec-model sites are those of the destructive
# lowering, which card tms-omschakeling removed; the lowering with truth maintenance was not probed):
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
# The DRL leg is NOT a repetition of the exec one: its three live sites are different code. Until card
# tms-omschakeling it also reached a golden the exec branch could not play at all
# (jutterswet-vondsttarief, which the destructive exec transpiler SKIPped as "waarde buiten scope" while
# the text branch lowers and plays it); the exec branch with truth maintenance plays it too. Measured
# by mutation on 2026-09-04 — EffectiveUnit.of() put back on the node's own annotation, the state
# before the derivation existed — the DRL leg alone goes red on NINE of the eleven twistable goldens,
# including that one. The two that stay green under the mutant are the two the text branch markers (the ch. 4.10
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
#     nothing here: the Drools back-ends never read Datatype.decimal_places or Datatype.whole_part at
#     all, only Rounding.decimal_places, which is a NODE field and not a type annotation.
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
#     Binary/power, or a BuiltinCall absolute value, root or
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
#
# --- the origin leg (IR-SPEC ch. 2.13) -------------------------------------------------------
# The same question for the origin side table: `origin` on a declaration a producer made itself
# carries no semantics either. Every golden that carries an origin and has case files is replayed
# once more, STRIPPED: every `origin` taken out, the document an executor that reads nothing of the
# field sees anyway, with the executors and the comparison of the vocabulary leg (ref and clingo
# exit 0 against the pinned expectations; the two Drools branches print their own untouched status
# line). A reader of the field in ref or clingo makes gates 5 and 6 red on the untouched golden and
# leaves the stripped copy green, as it must, since it has nothing to read; a reader in the Drools
# loader or transpilers is caught here, by the line comparison. Measured 2026-10-15 (card
# herkomst-op-de-draad) by mutation: the Drools loader made to skip a rule that carries an origin of
# the form decisionTableRow, an executor that reads the field, turns the leg red on the goldens with
# such rows on both branches; without it the leg is green. No survivor list: no reading of the
# field is a legitimate one. A golden without an origin is not probed; no golden with one is a
# failure.
#
# --- the name leg (IR-SPEC ch. 6.1) ----------------------------------------------------------
# The same question for `Default.name`, the name the source gives the block a default comes from:
# an extension field without semantics, like `source`. Every golden whose defaults carry a name
# and that has case files is replayed once more, STRIPPED: every `name` taken out of every Default
# node, at any depth of nesting, with the executors and the comparison of the origin leg. A reader
# of the field in ref or clingo makes gates 5 and 6 red on the untouched golden; a reader in the
# Drools loader or transpilers is caught here, by the line comparison. Measured 2026-10-21 (card
# defaults-decider) by mutation: the Drools loader made to skip a root default whose name begins
# with "melding", an executor that reads the field, turns the leg red on three goldens
# (jutterswet-opgave-datum, -opgave-subtype, -premiestaffel) on both branches; without it the leg
# is green. No survivor list: no reading of the field is a legitimate one. A golden without a named
# default is not probed; no golden with one is a failure.
# Since 2026-10-21 (card default-statute-annotations) the same stripped copy also loses every
# `statuteAnnotations` of a Default (IR-SPEC ch. 6.1, WF-082), the `// Bron:` annotations of the
# block the default comes from: an extension field without semantics like `name`, on the same
# nodes, so it rides this leg rather than a leg of its own, with its own count and floor. Every
# annotated default the frontend writes also carries the name of its block, so the probed set is
# the same. Measured 2026-10-21 by mutation: the Drools loader made to drop a root default by the
# text of one of its annotations, an executor that reads the field, turns the leg red on the two
# goldens that carry that text (jutterswet-schorsing, -tweeledige-grondslag) on both branches;
# without it the leg is green.
#
# --- the presentation leg (IR-SPEC ch. 10) ---------------------------------------------------
# The same question for the presentation fields of the model and rule layers: how a source spelled
# something, which no executor's answer may depend on. A field the schema leaves optional is STRIPPED; a
# field the schema requires, or whose presence is core, keeps its place and has its value EXCHANGED:
#   - kind: every `Characteristic.kind` rotated adjectival -> possessive -> nominal -> adjectival, so
#     every characteristic carries another valid kind (required in the schema);
#   - rule-presentation: the `presentation` of every rule version that carries one exchanged, equation
#     <-> calculation; the tag stays, because its presence is what makes a version a computation
#     (IR-SPEC ch. 3.1), which is core;
#   - preposition: every `Dimension.preposition` stripped;
#   - percentage: every percentage tag (`presentation: "percentage"` on a Number) stripped;
#   - rule-group: every `RuleGroup` with `recursive: false` taken out of `ruleGroups`; a recursive group
#     stays, because membership of one admits a creation cycle (WF-045), which is core;
#   - rule-annotations: every `statuteAnnotations` of a Rule stripped (those of a Default ride the name leg);
#   - diagnostics: every entry of the root list `diagnostics` (required in the schema) keeps its place, its
#     `message` gets the prefix "(exchanged) " and its `line` grows by one; the old message stays inside the
#     new one, so every Unresolved snippet it accounted for it still accounts for (WF-113 (b));
#   - source: every source record stripped, except on an Unresolved, where the schema requires it: there the
#     snippet loses its last character, which stays inside every diagnostic message that accounted for the
#     marker (WF-113 (b)). The Drools backend quotes a node's snippet in many refusal sentences ('{snippet}'),
#     and the clingo backend in the refusal CLINGO-035 ("{snippet}", OCaml's %S, "" without one), so a Drools
#     status line or a clingo refusal on a copy without the snippet is compared after each quoted old snippet of
#     the untouched one is rendered as the copy has it (same_drools_line, clingo_refused_alike); no other
#     difference passes, and the report counts the lines that differed only there.
# Every golden with at least one of these fields and with case files is replayed ONCE, against one
# combined copy with every field changed at once, with the executors and the comparison of the origin
# leg (ref and clingo per case against the pinned expectations; the two Drools branches print their own
# untouched status line). One copy per golden instead of one per field: the fields sit on nearly every
# golden (the rule presentation on 342 of 419), so a replay per field would multiply the leg's JVMs by
# up to three. A difference on the combined copy is then assigned to a field: the executor that
# differed runs again on the copies of that golden with one field changed each, and the report names
# the fields whose own copy moves its outcome away from the untouched one, or says that only the
# combination does; where ref or clingo give the combined copy's outcome on the untouched golden as well,
# they miss the pinned outcome there too, and the report says so instead of naming a field. The leg
# replays its calls through scripts/lib/parallel.sh, one process per two cores, and judges them in corpus
# order. A reader of a stripped field in ref or clingo makes gates 5 and 6 red on the untouched golden; a
# reader in the Drools loader or transpilers, and a reader of an exchanged value in any executor, is
# caught here. Measured 2026-11-11 (card core-and-presentation): every executor reproduces the untouched
# outcome on every combined copy (394 goldens). Measured by mutation the same day, five readers at once,
# each in one executor: ref faulting on a possessive characteristic, clingo faulting on a calculation
# version, the Drools loader refusing a dimension without a preposition, a percentage number without its
# tag, and a non-recursive rule group. The leg went red and named the field: kind for ref on 191 goldens,
# rule-presentation for clingo on 73, preposition on 10, percentage on 7 and rule-group on 1 for both
# Drools branches (13 goldens for ref and 54 for clingo carried the read value untouched, and were
# reported as missing the pinned outcome there too). Measured by mutation the same day for the two fields that
# joined the leg after: ref failing on a diagnostic with an odd line and the Drools loader refusing a rule with
# statute annotations made the leg red, naming diagnostics for ref on 5 cases of 3 goldens and rule-annotations
# for both Drools branches on 11 goldens. For the source record: the Drools loader refusing a comparison without
# one made the leg red on 189 goldens for both branches, naming source. No survivor list: no reading of a presentation field
# is a legitimate one. A field that no golden carries fails its floor.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

# --- the survivor list (reason-carrying; fails when stale) -----------------------------------
# Entries are "<golden-dir>|<executor>|<reason>". EMPTY today: measured 2026-09-04, all three
# executors are indifferent over the whole twistable set. An entry here is an admission that one
# executor's answer depends on a repetition rather than a declaration, and must say why that is
# the right reading — not merely that it is the current one.
SURVIVORS=''

# mention() and the stale check at the end read only the first two fields of an entry, so the reason is checked here,
# before the run: an entry without a third field, or with a blank one, fails.
reasonless=$(printf '%s\n' "$SURVIVORS" | while IFS= read -r s; do
    [ -n "$s" ] || continue
    rest=${s#*|}
    reason=${rest#*|}
    if [ "$rest" = "$s" ] || [ "$reason" = "$rest" ] || [ -z "$(printf '%s' "$reason" | tr -d '[:space:]')" ]; then
        echo "  $s"
    fi
done)
if [ -n "$reasonless" ]; then
    printf 'FAIL: survivor entry without a reason (each entry is <golden-dir>|<executor>|<reason>):\n%s\n' "$reasonless" >&2
    exit 1
fi

# --- the DRL-text branch and ch. 5.10: the divergence class is EMPTY (measured 2026-09-07) ---
# This gate measured the DRL-TEXT branch's relationship to RUNTIME-SEMANTICS ch. 5.10 when no other
# gate held that branch (since card drl-teksttak-op-truth-maintenance gate 12 holds it line for line). Until 2026-09-07 that relationship was a divergence, and this file
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
# rot. The live check on this branch is gate 12, and the line equality with the exec-model branch below.
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

. scripts/lib/ocaml-build.sh
if ! ensure_ocaml; then
    echo 'check-reftype-derivation: FAIL (build)' >&2
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
# PARALLEL_JVM_FLAGS, the flags of a replay JVM
. scripts/lib/parallel.sh

TWISTER=$(mktemp) || exit 2
VOCABULARY_TWISTER=$(mktemp) || exit 2
HSTRIPPER=$(mktemp) || exit 2
NSTRIPPER=$(mktemp) || exit 2
PRESENTER=$(mktemp) || exit 2
WORK=$(mktemp -d) || exit 2
LIST=$(mktemp) || exit 2
UNCOVERED=$(mktemp) || exit 2   # "<golden> <node>" per unit-bearing annotation outside table + exceptions
HITS=$(mktemp) || exit 2     # "<golden>|<executor>" per survivor entry that ACTUALLY diverged
trap 'rm -rf "$TWISTER" "$VOCABULARY_TWISTER" "$HSTRIPPER" "$NSTRIPPER" "$PRESENTER" "$WORK" "$LIST" "$UNCOVERED" "$HITS"' EXIT

cat > "$TWISTER" <<'PY'
import json, os, sys
from fractions import Fraction

# the position classes and their restatement-factor exponents (see the script header). The exponent
# is per NODE KIND; MEMBERSHIP is per position class, which for Binary/Aggregation is finer (below).
EXPONENT = {"Binary": 5, "Let": 6, "Aggregation": 7, "Rounding": 8, "Clamp": 9}
# ch. 2.10 clause 2 splits Binary by OPERATOR and an Aggregation by KIND, not by node: only these
# branches DERIVE a result unit (an additive result carries the left unit / a unitless side adopts
# the other; a times and a quotient add and subtract the exponents of their factors; a percentage-of
# carries its right operand's, ch. 2.6 rule 7; §4.6 Eenheid1 hands a value aggregation
# its first operand's). Every OTHER op/kind of those two node kinds is ch. 2.10's FOURTH class — v0
# derives nothing there, so it is neither twistable nor exempt, and the clamp reports it.
DERIVED_OP = {"plus", "minus", "reducedBy", "times", "dividedBy", "dividedByAbs", "percentageOf"}
DERIVED_KIND = {"sum", "total", "minimum", "maximum", "first", "last"}
# A BuiltinCall is a node kind the table carries with no derived branch at all: an absolute value,
# a root and the date/time extractors are the fourth class, and the two duration kinds are exempt
# (below). Since L4 it is the other half of what v0 wrote as one Functie node (delta B21).
CARRIED = set(EXPONENT) | {"BuiltinCall"}
# The reason-carrying exception set of the step-6 clamp: node kinds OUTSIDE the twist table whose
# annotation IS the source, so leaving them unprobed is a statement and not a gap.
EXEMPT_NODES = {"Literal": "the literal's type carries its own value's unit ({\"number\": \"3.5\"} in EUR), a copy of no declaration"}
# WF-038: a duration's own unit is the operator's parameter, not a derived result type
EXEMPT_BUILTINS = {"duration", "absoluteDuration"}
DATE = "Date"
SUFFIX = "-twisted"


def position_class(x):
    """The key table and clamp agree on: (node, op) for a Binary, (node, kind) for an Aggregation
       or a BuiltinCall, the bare node kind otherwise. Printed as the clamp's witness, so a report
       names the BRANCH."""
    node = x.get("node")
    if node == "Binary":
        return "Binary/%s" % x.get("op")
    if node in ("Aggregation", "BuiltinCall"):
        return "%s/%s" % (node, x.get("kind"))
    return node


def derived(x):
    """True iff this position's result unit is one ch. 2.10 clause 2 DERIVES — the only positions a
       twist may probe, because a twist asks whether a consumer follows a rule that exists."""
    node = x.get("node")
    if node == "Binary":
        return x.get("op") in DERIVED_OP
    if node == "Aggregation":
        return x.get("kind") in DERIVED_KIND
    return node in EXPONENT


class RawNum:
    """A JSON number with its original lexeme, so untouched numbers re-emit unchanged."""
    __slots__ = ("raw",)

    def __init__(self, s):
        self.raw = s


def rational(f):
    """The exact factor {num, den} as a Fraction; anything else is no factor to twist."""
    if not (isinstance(f, dict) and isinstance(f.get("num"), RawNum)
            and isinstance(f.get("den"), RawNum)):
        return None
    return Fraction(int(f["num"].raw), int(f["den"].raw))


def sister(unit, exp):
    """A convertible SISTER of the unit: the first factor with a conversion pair gets the same
       canonical, a distinct abbreviation and its factor x 10^exp, the rational written {num, den}
       in lowest terms, as the wire writes every factor. Its exponent e moves the factor of the
       whole unit by 10^(exp*e), which is never 1."""
    if not isinstance(unit, list):
        return None
    for i, e in enumerate(unit):
        if not isinstance(e, dict):
            return None
        a, c, f = e.get("abbreviation"), e.get("canonical"), rational(e.get("factorToCanonical"))
        if a is None or c is None or f is None:
            continue
        if SUFFIX in a:
            return None
        z = f * (10 ** exp)
        out = dict(e)
        out["abbreviation"] = a + SUFFIX + str(exp)
        out["factorToCanonical"] = {"num": RawNum(str(z.numerator)),
                                    "den": RawNum(str(z.denominator))}
        return unit[:i] + [out] + unit[i + 1:]
    return None


def twist(dt, exp):
    if not isinstance(dt, dict) or dt.get("node") != "Number":
        return dt, None
    u = sister(dt.get("unit"), exp)
    if u is None:
        return dt, None
    out = dict(dt)
    out["unit"] = u
    return out, "%s %s->%s" % (dt.get("node"), dt["unit"][0].get("abbreviation"),
                               u[0]["abbreviation"])


# A reference writes no type since L2 (B13): whether an operand is a DATE is read off the
# declaration it names, exactly as the executors read it (RUNTIME-SEMANTICS ch. 2.10). The other
# nodes still carry their own annotation.
DECL = {"attr": {}, "super": {}, "param": {}}


def declarations(doc):
    model = doc.get("model", {})
    for ot in model.get("objectTypes", []):
        n = ot.get("name")
        DECL["super"][n.lower()] = ot.get("supertype")
        for a in ot.get("attributes", []):
            DECL["attr"][(n.lower(), a.get("name", "").lower())] = a.get("datatype")
    for p in model.get("parameters", []):
        DECL["param"][(p.get("name") or "").lower()] = p.get("datatype")


def declared(e, scope):
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
        return declared(e.get("attribute") or {}, scope)
    if node == "BinderRef":
        return scope.get((e.get("name") or "").lower())
    return e.get("type")


def is_date_typed(e, scope):
    if not isinstance(e, dict):
        return False
    t = declared(e, scope)
    return isinstance(t, dict) and t.get("node") == DATE


def unit_bearing(dt):
    return isinstance(dt, dict) and dt.get("node") == "Number" and dt.get("unit") is not None


def loop(x, tally, skip_own=False, scope=None):
    scope = {} if scope is None else scope
    if isinstance(x, list):
        for v in x:
            loop(v, tally, scope=scope)
        return
    if not isinstance(x, dict):
        return
    node = x.get("node")
    # WF-035: the duration operand of a date +/- duration carries the calendar unit ref routes on
    duration = None
    if node == "Binary" and x.get("op") in ("plus", "minus", "reducedBy"):
        l, r = x.get("left"), x.get("right")
        if is_date_typed(l, scope) and not is_date_typed(r, scope):
            duration = r
        elif is_date_typed(r, scope) and not is_date_typed(l, scope):
            duration = l
    exempt = skip_own or node in EXEMPT_NODES \
        or (node == "BuiltinCall" and x.get("kind") in EXEMPT_BUILTINS)
    if node is not None and unit_bearing(x.get("type")):
        # step 6, the clamp: account for EVERY unit-bearing annotation, so a position that is
        # neither twisted nor exempt cannot pass unnoticed. The two arrivals are reported apart:
        # a FOURTH-CLASS branch of a carried node kind (v0 derives no result unit there) and a
        # node kind the table does not carry at all (a v1 form reaching the corpus).
        tally["unit"] += 1
        if exempt:
            tally["exempt"] += 1
        elif not derived(x):
            tally["fourth" if node in CARRIED else "uncovered"].append(position_class(x))
        elif sister(x["type"].get("unit"), EXPONENT[node]) is None:
            # in the table, but no convertible SISTER exists (a unit with no canonical/factor —
            # a unit system declares no conversion for it). A non-convertible lie would measure
            # refusal routing, not a declaration reading, so it is counted and not twisted.
            tally["withoutSister"] += 1
    if derived(x) and not exempt:
        twisted, how = twist(x.get("type"), EXPONENT[node])
        if how:
            x["type"] = twisted
            tally["hits"].append(how)
    # the binders a node opens are in scope of its body, with the type their binding declares
    inner = scope
    if node == "Let":
        inner = dict(scope)
        inner[(x.get("binder") or "").lower()] = declared(x.get("value") or {}, scope)
    elif node in ("Lambda", "FunctionDef"):
        inner = {(p.get("name") or "").lower(): p.get("datatype") for p in x.get("parameters", [])}
    elif node == "FilterList":
        # the filter binder reads each element of the list, so its type is the list's element type
        inner = dict(scope)
        list_type = declared(x.get("list") or {}, scope)
        inner[(x.get("binder") or "").lower()] = \
            list_type.get("element") if isinstance(list_type, dict) else None
    for k, v in list(x.items()):
        if k == "type":        # a DECLARATION never sits under an expression's own type
            continue
        if node == "Match" and k == "arms":
            # an arm binder carries its constructor's declared payload, off the scrutinee's type
            st = declared(x.get("scrutinee") or {}, scope)
            ctors = {(c.get("name") or "").lower(): c.get("payload")
                     for c in (st.get("constructors", []) if isinstance(st, dict) else [])}
            for arm in v if isinstance(v, list) else []:
                b = arm.get("binder") if isinstance(arm, dict) else None
                armscope = dict(scope)
                if b:
                    armscope[b.lower()] = ctors.get((arm.get("constructor") or "").lower())
                loop(arm, tally, scope=armscope)
            continue
        loop(v, tally, skip_own=(v is duration),
             scope=inner if (node == "Let" and k == "body")
             or (node == "FilterList" and k == "condition") or node in ("Lambda", "FunctionDef")
             else scope)


def pieces(o):
    if isinstance(o, RawNum):
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
        first = True
        for k, v in o.items():
            if not first:
                yield ","
            first = False
            yield json.dumps(k, ensure_ascii=False)
            yield ":"
            for c in pieces(v):
                yield c
        yield "}"
    elif isinstance(o, list):
        yield "["
        first = True
        for v in o:
            if not first:
                yield ","
            first = False
            for c in pieces(v):
                yield c
        yield "]"
    else:
        raise TypeError(repr(o))


source, target, name = sys.argv[1], sys.argv[2], sys.argv[3]
with open(source, encoding="utf-8") as fh:
    doc = json.load(fh, parse_int=RawNum, parse_float=RawNum)
declarations(doc)
tally = {"hits": [], "uncovered": [], "fourth": [], "unit": 0, "exempt": 0,
           "withoutSister": 0}
loop(doc, tally)
map_ = os.path.dirname(target)
if map_:
    os.makedirs(map_, exist_ok=True)
with open(target, "w", encoding="utf-8") as fh:
    for c in pieces(doc):
        fh.write(c)
# line 1: the four counters the shell sums; then one TAGGED line per uncovered occurrence (the
# clamp), the tag separating ch. 2.10's fourth class from a node kind the table does not carry.
print("%d %d %d %d" % (len(tally["hits"]), tally["unit"],
                       tally["exempt"], tally["withoutSister"]))
for position in tally["fourth"]:
    print("fourth %s %s" % (name, position))
for position in tally["uncovered"]:
    print("unknown %s %s" % (name, position))
PY

cat > "$VOCABULARY_TWISTER" <<'PY'
import json, os, sys

# The three copies of the vocabulary leg (see the script header). Numbers keep their lexemes: the
# copies differ from the golden in the side table and in nothing else.


class RawNum:
    __slots__ = ("raw",)

    def __init__(self, s):
        self.raw = s


def write_json(o, path):
    def piece(o):
        if isinstance(o, RawNum):
            return o.raw
        if isinstance(o, dict):
            return "{" + ",".join(json.dumps(k, ensure_ascii=False) + ":" + piece(v) for k, v in o.items()) + "}"
        if isinstance(o, list):
            return "[" + ",".join(piece(v) for v in o) + "]"
        return json.dumps(o, ensure_ascii=False)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(piece(o))


def read_json(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh, parse_int=RawNum, parse_float=RawNum)


def carriers(doc):
    """Every node that may carry designations, with its WF-118 scope and its canonical name."""
    out = []
    m, r = doc["model"], doc["rules"]
    for ot in m.get("objectTypes", []):
        out.append((("objectTypes",), ot.get("name"), ot))
        for a in ot.get("attributes", []):
            out.append((("attributes", ot.get("name")), a.get("name"), a))
        for c in ot.get("characteristics", []):
            out.append((("characteristics", ot.get("name")), c.get("name"), c))
    for ft in m.get("factTypes", []):
        out.append((("factTypes",), ft.get("name"), ft))
        for role in ft.get("roles", []):
            out.append((("roles", ft.get("name")), role.get("name"), role))
    for p in m.get("parameters", []):
        out.append((("parameters",), p.get("name"), p))
    for section in ("rules", "objectCreations", "allocations", "consistencies", "uniquenesses",
                  "factCreations", "prioritizedDefaults"):
        for f in r.get(section, []):
            out.append((("forms",), f.get("name"), f))
    for g in r.get("ruleGroups", []):
        out.append((("ruleGroups",), g.get("name"), g))
    return out


def value_tables(doc):
    """The enumeration declarations that carry value designations, by domain."""
    for d in doc["model"].get("domains", []):
        dt = d.get("datatype") or {}
        if dt.get("node") == "Enumeration" and "valueDesignations" in dt:
            yield d.get("name"), dt


source, target = sys.argv[1], sys.argv[2]
if not (read_json(source).get("vocabularies") or []):
    print("0 0 0 0")
    sys.exit(0)
os.makedirs(target, exist_ok=True)

# STRIPPED
bare = read_json(source)
del bare["vocabularies"]
stripped = 0
for _, _, n in carriers(bare):
    stripped += len(n.pop("designations", []))
for _, dt in value_tables(bare):
    stripped += len(dt.pop("valueDesignations"))
write_json(bare, os.path.join(target, "stripped.json"))

# EXCHANGED: vocabulary i takes the name of vocabulary i+1, in the declarations and the designations
exchanged = read_json(source)
names = [v["name"] for v in exchanged["vocabularies"]]
renamed = {n.lower(): names[(i + 1) % len(names)] for i, n in enumerate(names)}
swapped = 0
for v in exchanged["vocabularies"]:
    v["name"] = renamed[v["name"].lower()]
for _, _, n in carriers(exchanged):
    for b in n.get("designations", []):
        b["vocabulary"] = renamed.get(b["vocabulary"].lower(), b["vocabulary"])
        swapped += 1
for _, dt in value_tables(exchanged):
    for b in dt["valueDesignations"]:
        b["vocabulary"] = renamed.get(b["vocabulary"].lower(), b["vocabulary"])
        swapped += 1
write_json(exchanged, os.path.join(target, "swapped.json"))

# ROTATED: per vocabulary and scope, the designations one declaration on
rotated_doc = read_json(source)
defaults = {v["name"].lower() for v in rotated_doc["vocabularies"] if v.get("default")}
rotated = 0


def rotate(voc, places):
    """plekken: (canonical name, designation record) of one scope and vocabulary, in document order.
       The first offset that gives no declaration its own canonical name outside the default wins."""
    global rotated
    if len(places) < 2:
        return
    texts = [b["designation"] for _, b in places]
    for k in range(1, len(places)):
        shifted = texts[-k:] + texts[:-k]
        if voc in defaults or all(t != c for (c, _), t in zip(places, shifted)):
            for (_, b), t in zip(places, shifted):
                if b["designation"] != t:
                    b["designation"] = t
                    rotated += 1
            return


scopes = {}
for scope, canon, n in carriers(rotated_doc):
    for b in n.get("designations", []):
        scopes.setdefault((b["vocabulary"].lower(), scope), []).append((canon, b))
for dom, dt in value_tables(rotated_doc):
    for b in dt["valueDesignations"]:
        scopes.setdefault((b["vocabulary"].lower(), ("values", dom)), []).append((b["value"], b))
for (voc, _), places in scopes.items():
    rotate(voc, places)
write_json(rotated_doc, os.path.join(target, "rotated.json"))
print("1 %d %d %d" % (stripped, swapped, rotated))
PY

cat > "$HSTRIPPER" <<'PY'
import json, os, sys

# The one copy of the origin leg (see the script header): every `origin` taken out of the nodes that
# may carry one, the nodes that may carry `designations` (IR-SPEC ch. 2.13). Numbers keep their
# lexemes, so the copy differs from the golden in the field and in nothing else.


class RawNum:
    __slots__ = ("raw",)

    def __init__(self, s):
        self.raw = s


def write_json(o, path):
    def piece(o):
        if isinstance(o, RawNum):
            return o.raw
        if isinstance(o, dict):
            return "{" + ",".join(json.dumps(k, ensure_ascii=False) + ":" + piece(v) for k, v in o.items()) + "}"
        if isinstance(o, list):
            return "[" + ",".join(piece(v) for v in o) + "]"
        return json.dumps(o, ensure_ascii=False)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(piece(o))


def carriers(doc):
    m, r = doc["model"], doc["rules"]
    for ot in m.get("objectTypes", []):
        yield ot
        yield from ot.get("attributes", [])
        yield from ot.get("characteristics", [])
    for ft in m.get("factTypes", []):
        yield ft
        yield from ft.get("roles", [])
    yield from m.get("parameters", [])
    for section in ("rules", "objectCreations", "allocations", "consistencies", "uniquenesses",
                  "factCreations", "prioritizedDefaults", "ruleGroups"):
        yield from r.get(section, [])


source, target = sys.argv[1], sys.argv[2]
with open(source, encoding="utf-8") as fh:
    doc = json.load(fh, parse_int=RawNum, parse_float=RawNum)
stripped = 0
for n in carriers(doc):
    if n.pop("origin", None) is not None:
        stripped += 1
if stripped:
    os.makedirs(os.path.dirname(target), exist_ok=True)
    write_json(doc, target)
print(stripped)
PY

cat > "$NSTRIPPER" <<'PY'
import json, os, sys

# The one copy of the name leg (see the script header): every `name` and every `statuteAnnotations`
# taken out of every Default node (IR-SPEC ch. 6.1), the defaults of a group and those of its exception
# groups at any depth. Numbers keep their lexemes, so the copy differs from the golden in those two
# fields and in nothing else. Prints the names and the annotation lists it took out.


class RawNum:
    __slots__ = ("raw",)

    def __init__(self, s):
        self.raw = s


def write_json(o, path):
    def piece(o):
        if isinstance(o, RawNum):
            return o.raw
        if isinstance(o, dict):
            return "{" + ",".join(json.dumps(k, ensure_ascii=False) + ":" + piece(v) for k, v in o.items()) + "}"
        if isinstance(o, list):
            return "[" + ",".join(piece(v) for v in o) + "]"
        return json.dumps(o, ensure_ascii=False)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(piece(o))


def defaults(chain):
    for d in chain:
        yield d
        for member in d.get("exceptions", {}).get("members", []):
            yield from defaults(member["defaults"])


source, target = sys.argv[1], sys.argv[2]
with open(source, encoding="utf-8") as fh:
    doc = json.load(fh, parse_int=RawNum, parse_float=RawNum)
stripped = 0
annotations = 0
for group in doc["rules"].get("prioritizedDefaults", []):
    for d in defaults(group["defaults"]):
        if d.pop("name", None) is not None:
            stripped += 1
        if d.pop("statuteAnnotations", None) is not None:
            annotations += 1
if stripped or annotations:
    os.makedirs(os.path.dirname(target), exist_ok=True)
    write_json(doc, target)
print(stripped, annotations)
PY

cat > "$PRESENTER" <<'PY'
import json, os, sys

# The copies of the presentation leg (see the script header): one copy per field, the field alone
# stripped or exchanged, and one combined copy with every field changed at once. Numbers keep their
# lexemes, so a copy differs from the golden in its field(s) and in nothing else. Prints the number of
# places each field changes, in the order of FIELDS; source prints two, the records stripped and the
# Unresolved snippets exchanged. With --same-line it compares two Drools status lines instead (same_line).


class RawNum:
    __slots__ = ("raw",)

    def __init__(self, s):
        self.raw = s


def write_json(o, path):
    def piece(o):
        if isinstance(o, RawNum):
            return o.raw
        if isinstance(o, dict):
            return "{" + ",".join(json.dumps(k, ensure_ascii=False) + ":" + piece(v) for k, v in o.items()) + "}"
        if isinstance(o, list):
            return "[" + ",".join(piece(v) for v in o) + "]"
        return json.dumps(o, ensure_ascii=False)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(piece(o))


def nodes(j, kind):
    if isinstance(j, dict):
        if j.get("node") == kind:
            yield j
        for v in j.values():
            yield from nodes(v, kind)
    elif isinstance(j, list):
        for v in j:
            yield from nodes(v, kind)


# The rotation moves every kind to another valid one, so a reader of any single value sees it change.
KIND_ROTATION = {"adjectival": "possessive", "possessive": "nominal", "nominal": "adjectival"}
PRESENTATION_EXCHANGE = {"equation": "calculation", "calculation": "equation"}


def kind(doc):
    n = 0
    for c in nodes(doc, "Characteristic"):
        c["kind"] = KIND_ROTATION[c["kind"]]
        n += 1
    return n


def rule_presentation(doc):
    # Only the value moves: the tag's presence decides the rule kind (IR-SPEC ch. 3.1), which is core.
    n = 0
    for rule in doc["rules"].get("rules", []):
        for version in rule["versions"]:
            if "presentation" in version:
                version["presentation"] = PRESENTATION_EXCHANGE[version["presentation"]]
                n += 1
    return n


def preposition(doc):
    n = 0
    for d in nodes(doc, "Dimension"):
        if d.pop("preposition", None) is not None:
            n += 1
    return n


def percentage(doc):
    n = 0
    for d in nodes(doc, "Number"):
        if d.pop("presentation", None) is not None:
            n += 1
    return n


def rule_group(doc):
    # A recursive group stays: membership of one admits a creation cycle (WF-045), which is core.
    groups = doc["rules"].get("ruleGroups")
    if not groups:
        return 0
    kept = [g for g in groups if g["recursive"]]
    doc["rules"]["ruleGroups"] = kept
    return len(groups) - len(kept)


def rule_annotations(doc):
    n = 0
    for r in nodes(doc, "Rule"):
        if r.pop("statuteAnnotations", None) is not None:
            n += 1
    return n


def diagnostics(doc):
    # Required at the root, so every entry keeps its place and changes its message and line; the message keeps
    # the old text after a prefix, so every marker snippet it contained it still contains (WF-113 (b)) and the
    # copy stays a document.
    n = 0
    for d in doc["diagnostics"]:
        d["message"] = "(exchanged) " + d["message"]
        d["line"] = RawNum(str(int(d["line"].raw) + 1))
        n += 1
    return n


# The snippets the source change moved, as (old, new): "" where the record was stripped, the shorter snippet where
# an Unresolved kept it. The Drools comparison below renders them in the untouched status line.
rendered = set()


def source(doc):
    # Optional on every node but an Unresolved, where the schema requires it (IR-SPEC ch. 4.4): there the snippet
    # loses its last character, which stays inside every diagnostic message that contained the whole snippet, so
    # WF-113 (b) still accounts for the marker, on this copy alone and beside the diagnostics exchange.
    counts = [0, 0]

    def walk(j):
        if isinstance(j, dict):
            if "source" in j:
                record = j["source"]
                if not (isinstance(record, dict) and isinstance(record.get("snippet"), str)):
                    sys.exit("a source field that is no source record in %s: %r" % (source_path, record))
                old = record["snippet"]
                if j.get("node") == "Unresolved":
                    if len(old) < 2:
                        sys.exit("an Unresolved snippet too short to exchange in %s: %r" % (source_path, old))
                    record["snippet"] = old[:-1]
                    counts[1] += 1
                else:
                    del j["source"]
                    counts[0] += 1
                rendered.add((old, record["snippet"] if "source" in j else ""))
            for v in j.values():
                walk(v)
        elif isinstance(j, list):
            for v in j:
                walk(v)

    walk(doc)
    return tuple(counts)


# rule-group comes last: the groups it takes out carry source records, which the source change must count on the
# combined copy as on its own.
FIELDS = [("kind", kind), ("rule-presentation", rule_presentation), ("preposition", preposition),
          ("percentage", percentage), ("rule-annotations", rule_annotations), ("diagnostics", diagnostics),
          ("source", source), ("rule-group", rule_group)]


def ocaml_quoted(s):
    # OCaml's %S: the UTF-8 bytes between double quotes, with String.escaped's escapes (a byte outside ' '..'~'
    # as \ddd in decimal).
    named = {'"': '\\"', "\\": "\\\\", "\n": "\\n", "\t": "\\t", "\r": "\\r", "\b": "\\b"}
    out = []
    for b in s.encode("utf-8"):
        c = chr(b)
        out.append(named[c] if c in named else c if 32 <= b <= 126 else "\\%03d" % b)
    return '"' + "".join(out) + '"'


def same_line(quote, snippets, untouched, changed):
    # The Drools backend quotes the snippet of a node in many refusal sentences ('{snippet}', '' without one), the
    # clingo backend in the refusal CLINGO-035 ("{snippet}" as OCaml's %S writes it, "" without one); [quote] says
    # which. A line that differs only there is the same answer: each quoted old snippet of the untouched line is
    # rendered as the copy has it, in one pass, and nothing else is touched. A snippet the copy moved to two
    # different values is left alone, so such a line stays different.
    import re
    render = (lambda t: "'" + t + "'") if quote == "'" else ocaml_quoted
    with open(snippets, encoding="utf-8") as fh:
        pairs = json.load(fh)
    to = {}
    for old, new in pairs:
        to.setdefault(old, set()).add(new)
    to = {render(old): render(news.pop()) for old, news in to.items() if len(news) == 1}
    if not to:
        return untouched == changed
    pattern = re.compile("|".join(re.escape(old) for old in sorted(to, key=len, reverse=True)))
    return pattern.sub(lambda m: to[m.group(0)], untouched) == changed


if sys.argv[1] == "--same-line":
    if sys.argv[2] not in ("'", '"'):
        sys.exit("--same-line takes the quote ' (Drools) or \" (clingo), not %r" % sys.argv[2])
    sys.exit(0 if same_line(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5]) else 1)

source_path, target = sys.argv[1], sys.argv[2]


def load():
    with open(source_path, encoding="utf-8") as fh:
        return json.load(fh, parse_int=RawNum, parse_float=RawNum)


combined = load()
counts = []
for name, change in FIELDS:
    single = load()
    n = change(single)
    if change(combined) != n:
        sys.exit("the %s change counts differently on the combined copy of %s" % (name, source_path))
    n = n if isinstance(n, tuple) else (n,)
    if any(n):
        os.makedirs(target, exist_ok=True)
        write_json(single, os.path.join(target, name + ".json"))
    counts.extend(n)
if any(counts):
    write_json(combined, os.path.join(target, "combined.json"))
if rendered:
    with open(os.path.join(target, "snippets.json"), "w", encoding="utf-8") as fh:
        json.dump(sorted(rendered), fh, ensure_ascii=False)
print(" ".join(str(n) for n in counts))
PY

# Is <golden> x <executor> a pinned survivor? Read LINE by line, never word by word: a
# reason carries spaces, so splitting $SURVIVORS on whitespace would shred every entry the
# moment the list stops being empty.
mention() {
    matched=$(printf '%s\n' "$SURVIVORS" | while IFS= read -r s; do
        [ -n "$s" ] || continue
        [ "${s%%|*}" = "$1" ] || continue
        rest=${s#*|}
        [ "${rest%%|*}" = "$2" ] && echo hit
    done)
    [ -n "$matched" ]
}

# The status line DroolsBackendMain prints last: `PLAYED <n>`, `FAIL <detail>`, `EXCLUDE DROOLS-###: <reason>`
# or `ERROR <detail>`. Each leg compares the line of a run on a changed copy with the line of the run on the
# untouched ir.json, so two runs whose JVMs both crashed, were killed or did not start would compare equal,
# empty against empty. An untouched run that ends in any other line therefore fails the gate by golden, leg
# and executor, and the leg compares nothing with it. ref needs no such check: the legs judge it by its exit
# status, and a process that crashed, was killed or did not start does not end with 0. clingo is judged the
# same way, except on a golden in gate 6's refusal channel (clingo_same below).
drools_status() {
    case $1 in
        "PLAYED "*)
            case ${1#PLAYED } in ''|*[!0-9]*) return 1 ;; esac ;;
        "FAIL "*|"ERROR "*|"EXCLUDE DROOLS-"[0-9][0-9][0-9]": "*) ;;
        *) return 1 ;;
    esac
}
no_status() {   # <golden> <leg> <executor> <line>
    printf "FAIL: %s — the untouched run of %s in the %s leg ends in '%s', no status line of DroolsBackendMain (PLAYED, FAIL, EXCLUDE, ERROR): its JVM crashed, was killed or did not start\n" \
        "$1" "$3" "$2" "$4" >&2
    status=1
}

# clingo on a changed copy gives the answer it gives on the untouched ir.json. That answer is exit 0 where
# gate 6 pins the golden as played. For a golden in gate 6's refusal channel it is a refusal with register
# codes at the head of the report (`TRANSLATION FAULT: CLINGO-032: …`, `CLINGO-031: clingo: …`,
# `CLINGO-033, CLINGO-034: clingo: …`), and the copy must give that report byte for byte. An untouched run that ends in anything else is no answer to
# reproduce (two runs that both crashed would compare equal), so the copy then fails as a non-zero exit.
clingo_same() {   # <untouched ir.json> <changed copy> <case>; the copy's report in $report_line
    report_line=$("$RUNNER_CLINGO" "$2" "$3" 2>&1) && return 0
    clingo_refused_alike "$1" "$3"
}
# The second half of clingo_same, for a copy that exited non-zero with its report in $report_line (the
# presentation leg runs the copy as a task of its own). With the copies' directory of the presentation leg, a
# refusal that differs from the untouched one only in a quoted snippet the source change moved is the same answer
# (same_line in the presenter, as for a Drools status line), and clingo_rendered says so.
clingo_refused_alike() {   # <untouched ir.json> <case> [<copies' directory>]
    clingo_rendered=0
    untouched=$("$RUNNER_CLINGO" "$1" "$2" 2>&1) && return 1
    case $untouched in
        "TRANSLATION FAULT: CLINGO-"[0-9][0-9][0-9]": "*|"CLINGO-"[0-9][0-9][0-9]": clingo: "*|"CLINGO-"[0-9][0-9][0-9]", CLINGO-"*": clingo: "*) ;;
        *) return 1 ;;
    esac
    [ "$untouched" = "$report_line" ] && return 0
    [ $# -ge 3 ] && [ -f "$3/snippets.json" ] || return 1
    python3 "$PRESENTER" --same-line '"' "$3/snippets.json" "$untouched" "$report_line" || return 1
    clingo_rendered=1
}

# --- monotone floors (maintainer decision 2026-09-09) --------------------------------------------
# Lower bounds, never equalities: they catch SHRINK — the harness that quietly probes half — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
# Both counts are this harness's discriminating power, not a file count: the twist total is what
# the twist TABLE meets in the corpus, so a narrowed table would shrink it with every corpus file
# still in place, and no other gate would see it. The DRL leg has no floor of its own since the
# switch-over of card drl-teksttak-op-truth-maintenance: it must PLAY exactly the twistable goldens the
# exec-model branch plays (the line equality below).
EXPECTED_MIN_TWISTS=38      # 2026-09-24: measured 38 annotations twisted over 21 goldens. LOWERED
                            # from 74 (2026-09-10) in the L2 commit that took the four reference
                            # classes out of the twist table (the L2 card,
                            # backlog/done/20260923-l2-referentielaag.md): a reference writes no type since
                            # B13, so there is nothing left to twist there; the composite twists
                            # are untouched (same 38 before and after the table change)

# The vocabulary leg's floors (see the script header), measured 2026-09-27 at the card that brought the
# side table, on its one golden jutterswet-vocabulaires: a golden count, so a leg that stops finding
# the goldens it probes fails above zero, and the designations the stripped and the rotated copies
# move, so a twister that quietly moves nothing fails too (an exchange moves every designation, so its
# count is the stripped one and needs no floor of its own).
EXPECTED_MIN_VOCABULARY_GOLDENS=1     # 2026-09-27: measured 1 golden with vocabularies and case files
EXPECTED_MIN_VOCABULARY_STRIPPED=46   # 2026-09-27: measured 46 designations stripped (23 declarations x 2 vocabularies)
EXPECTED_MIN_VOCABULARY_ROTATED=36  # 2026-09-27: measured 36 designations rotated to another declaration

# The origin leg's floors (see the script header), measured 2026-10-15 at the card herkomst-op-de-draad: a
# golden count, so a leg that stops finding the goldens it probes fails above zero, and the origins the
# stripped copies lose, so a stripper that quietly takes out less fails too.
EXPECTED_MIN_ORIGIN_GOLDENS=9        # 2026-10-15: measured 9 goldens with an origin and case files
EXPECTED_MIN_ORIGIN_STRIPPED=71      # 2026-10-15: measured 71 origins stripped over those 9

# The name leg's floors (see the script header), measured 2026-10-21 at the card defaults-decider: a golden
# count, so a leg that stops finding the goldens it probes fails above zero, and the names the stripped
# copies lose, so a stripper that quietly takes out less fails too.
EXPECTED_MIN_NAME_GOLDENS=20         # 2026-10-21: measured 20 goldens with a named default and case files
EXPECTED_MIN_NAME_STRIPPED=64        # 2026-10-21: measured 64 names stripped over those 20
# The annotation lists the same copies lose (card default-statute-annotations, measured 2026-10-21): 29
# lists over 12 goldens, the 27 the frontend wrote into Default.source until then and the two of the
# golden jutterswet-tweeledige-grondslag.
EXPECTED_MIN_ANNOTATIONS_STRIPPED=29 # 2026-10-21: measured 29 statuteAnnotations lists stripped

# The presentation leg's floors (see the script header), measured 2026-11-11 at the card core-and-presentation:
# the goldens it replays, and per field the goldens that carry it and the places its copy changes, so a copier
# that quietly changes less, or a field that leaves the corpus, fails by the name of the field.
EXPECTED_MIN_PRESENTATION_GOLDENS=407       # 2026-11-11: measured 407 goldens with a presentation field and case files;
                                            # RAISED from 394 when the source record joined the leg (11 goldens carry
                                            # no other presentation field)
EXPECTED_MIN_KIND_GOLDENS=211               # 2026-11-11: measured 211 goldens with a characteristic
EXPECTED_MIN_KIND_ROTATED=438               # 2026-11-11: measured 438 characteristic kinds rotated
EXPECTED_MIN_RULE_PRESENTATION_GOLDENS=342  # 2026-11-11: measured 342 goldens with a tagged rule version
EXPECTED_MIN_RULE_PRESENTATION_EXCHANGED=868  # 2026-11-11: measured 868 presentation values exchanged
EXPECTED_MIN_PREPOSITION_GOLDENS=10         # 2026-11-11: measured 10 goldens with a dimension preposition
EXPECTED_MIN_PREPOSITION_STRIPPED=13        # 2026-11-11: measured 13 prepositions stripped
EXPECTED_MIN_PERCENTAGE_GOLDENS=7           # 2026-11-11: measured 7 goldens with a percentage tag
EXPECTED_MIN_PERCENTAGE_STRIPPED=13         # 2026-11-11: measured 13 percentage tags stripped
EXPECTED_MIN_RULE_GROUP_GOLDENS=1           # 2026-11-11: measured 1 golden with a non-recursive rule group (jutterswet-vocabulaires)
EXPECTED_MIN_RULE_GROUP_STRIPPED=1          # 2026-11-11: measured 1 non-recursive rule group stripped
# Two provenance fields of IR-SPEC ch. 10, measured 2026-11-11 when they joined the leg.
EXPECTED_MIN_RULE_ANNOTATIONS_GOLDENS=11    # 2026-11-11: measured 11 goldens with a rule that carries statuteAnnotations
EXPECTED_MIN_RULE_ANNOTATIONS_STRIPPED=34   # 2026-11-11: measured 34 statuteAnnotations lists of rules stripped
EXPECTED_MIN_DIAGNOSTICS_GOLDENS=3          # 2026-11-11: measured 3 goldens with a diagnostic
EXPECTED_MIN_DIAGNOSTICS_EXCHANGED=5        # 2026-11-11: measured 5 diagnostics exchanged (message and line)
# The source record, measured 2026-11-11 when it joined the leg: stripped where optional, the snippet exchanged on an
# Unresolved, where the schema requires the record.
EXPECTED_MIN_SOURCE_GOLDENS=402             # 2026-11-11: measured 402 goldens with a source record
EXPECTED_MIN_SOURCE_STRIPPED=7021           # 2026-11-11: measured 7021 source records stripped
EXPECTED_MIN_SOURCE_EXCHANGED=3             # 2026-11-11: measured 3 Unresolved snippets exchanged (unresolved-behind-a-failing-condition)

status=0
find corpus/golden -name ir.json 2>/dev/null | sort > "$LIST"

# --- the unit leg (the twist; see the script header) -------------------------------------------
leg_twist() {
    CASES=$WORK/twist.cases
    n_goldens=0
    n_twists=0
    n_checked=0
    n_survivors=0
    n_drl=0
    n_exec_played=0
    n_unit=0
    n_exempt=0
    n_without_sister=0
    while IFS= read -r ir; do
        dir=$(dirname "$ir")
        ident=${dir#corpus/golden/}
        tw="$WORK/$dir/ir.json"
        out=$(python3 "$TWISTER" "$ir" "$tw" "$ident") || { echo "FAIL: the twister could not read $ir" >&2; exit 2; }
        first_line=$(printf '%s\n' "$out" | head -1)
        # the clamp's accounting runs over EVERY golden, replayable or not: an unprobed node kind is a
        # gap wherever it sits.
        rest_lines=$(printf '%s\n' "$out" | tail -n +2)
        [ -z "$rest_lines" ] || printf '%s\n' "$rest_lines" >> "$UNCOVERED"
        set -- $first_line
        hits_list=$1
        n_unit=$((n_unit + $2))
        n_exempt=$((n_exempt + $3))
        n_without_sister=$((n_without_sister + $4))
        [ "$hits_list" -gt 0 ] || continue          # no twistable annotation: nothing to probe here
        find "$dir" -maxdepth 1 -name 'case*.json' 2>/dev/null | sort > "$CASES"
        [ -s "$CASES" ] || continue                # no case file: not replayable (gate 5 lists those)
        n_goldens=$((n_goldens + 1))
        n_twists=$((n_twists + hits_list))

        # ref and clingo: exit 0 on the untouched ir.json is what gates 5 and 6 pin, so the twisted
        # run must exit 0 too, or, for a golden in gate 6's refusal channel, give clingo's refusal of
        # the untouched run (clingo_same). Any other outcome is the executor reading the repetition.
        while IFS= read -r case; do
            n_checked=$((n_checked + 2))
            if ! report_line=$("$RUNNER_REF" "$tw" "$case" 2>&1); then
                if mention "$ident" ref; then
                    n_survivors=$((n_survivors + 1)); echo "$ident|ref" >> "$HITS"
                else
                    printf 'FAIL: %s — ref answers differently on the lying copy:\n%s\n' "$case" "$report_line" >&2
                    status=1
                fi
            fi
            if ! clingo_same "$ir" "$tw" "$case"; then
                if mention "$ident" clingo; then
                    n_survivors=$((n_survivors + 1)); echo "$ident|clingo" >> "$HITS"
                else
                    printf 'FAIL: %s — clingo answers differently on the lying copy:\n%s\n' "$case" "$report_line" >&2
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
        n_checked=$((n_checked + 1))
        base_expr=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" "$ir" "$@" 2>/dev/null | tail -1)
        twisted=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" "$tw" "$@" 2>/dev/null | tail -1)
        [ "${base_expr%% *}" = PLAYED ] && n_exec_played=$((n_exec_played + 1))
        if ! drools_status "$base_expr"; then
            no_status "$ident" unit Drools "$base_expr"
        elif [ "$base_expr" != "$twisted" ]; then
            if mention "$ident" drools; then
                n_survivors=$((n_survivors + 1)); echo "$ident|drools" >> "$HITS"
            else
                printf 'FAIL: %s — Drools answers differently on the lying copy:\n  untouched: %s\n  lying:     %s\n' \
                    "$ident" "$base_expr" "$twisted" >&2
                status=1
            fi
        fi

        # The SECOND rendering of the same analysis, through the same twist: the DRL text behind the runner's
        # --drl entry, compiled by the classic DRL build and replayed against the SAME pinned expectations. Its
        # untouched line must be the exec-model branch's untouched line (the two render one analysis; gate 12
        # holds this over the whole corpus), and its twisted line its own untouched one.
        n_checked=$((n_checked + 1))
        drl_baseline=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" --drl "$ir" "$@" 2>/dev/null | tail -1)
        d_twisted=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" --drl "$tw" "$@" 2>/dev/null | tail -1)
        if ! drools_status "$drl_baseline"; then
            no_status "$ident" unit "the DRL-text branch" "$drl_baseline"
        elif drools_status "$base_expr" && [ "$drl_baseline" != "$base_expr" ]; then
            printf 'FAIL: %s — the DRL-text branch, the second rendering of the analysis, gives another status line than the exec-model branch on the UNTOUCHED golden:\n  exec-model: %s\n  drl-text:   %s\n' \
                "$ident" "$base_expr" "$drl_baseline" >&2
            status=1
        fi
        [ "${drl_baseline%% *}" = PLAYED ] && n_drl=$((n_drl + 1))
        if drools_status "$drl_baseline" && [ "$drl_baseline" != "$d_twisted" ]; then
            if mention "$ident" drl; then
                n_survivors=$((n_survivors + 1)); echo "$ident|drl" >> "$HITS"
            else
                printf 'FAIL: %s — the DRL-text branch answers differently on the lying copy:\n  untouched: %s\n  lying:     %s\n' \
                    "$ident" "$drl_baseline" "$d_twisted" >&2
                status=1
            fi
        fi
    done < "$LIST"
    printf 'leg_status=%s n_goldens=%s n_twists=%s n_checked=%s n_survivors=%s n_drl=%s n_exec_played=%s n_unit=%s n_exempt=%s n_without_sister=%s\n' \
        "$status" "$n_goldens" "$n_twists" "$n_checked" "$n_survivors" "$n_drl" "$n_exec_played" "$n_unit" "$n_exempt" "$n_without_sister" > "$WORK/twist.counts"
}

# --- the vocabulary leg (see the script header) ---------------------------------------------------
leg_vocabulary() {
    CASES=$WORK/vocabulary.cases
    n_vgoldens=0
    n_v_stripped=0
    n_v_swapped=0
    n_v_rotated=0
    n_vreplays=0
    while IFS= read -r ir; do
        dir=$(dirname "$ir")
        ident=${dir#corpus/golden/}
        vdir="$WORK/vocabulaire/$ident"
        first_line=$(python3 "$VOCABULARY_TWISTER" "$ir" "$vdir") || { echo "FAIL: the vocabulary twister could not read $ir" >&2; exit 2; }
        set -- $first_line
        [ "$1" -eq 1 ] || continue                 # no vocabulary: nothing to probe here
        find "$dir" -maxdepth 1 -name 'case*.json' 2>/dev/null | sort > "$CASES"
        [ -s "$CASES" ] || continue                # no case file: not replayable
        n_vgoldens=$((n_vgoldens + 1))
        n_v_stripped=$((n_v_stripped + $2))
        n_v_swapped=$((n_v_swapped + $3))
        n_v_rotated=$((n_v_rotated + $4))
        set --
        while IFS= read -r case; do set -- "$@" "$case"; done < "$CASES"
        base_expr=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" "$ir" "$@" 2>/dev/null | tail -1)
        drl_baseline=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" --drl "$ir" "$@" 2>/dev/null | tail -1)
        drools_status "$base_expr" || no_status "$ident" vocabulary Drools "$base_expr"
        drools_status "$drl_baseline" || no_status "$ident" vocabulary "the DRL-text branch" "$drl_baseline"
        for variant in stripped swapped rotated; do
            vir="$vdir/$variant.json"
            while IFS= read -r case; do
                n_vreplays=$((n_vreplays + 2))
                if ! report_line=$("$RUNNER_REF" "$vir" "$case" 2>&1); then
                    printf 'FAIL: %s — ref answers differently on the %s copy (vocabulary leg):\n%s\n' "$case" "$variant" "$report_line" >&2
                    status=1
                fi
                if ! clingo_same "$ir" "$vir" "$case"; then
                    printf 'FAIL: %s — clingo answers differently on the %s copy (vocabulary leg):\n%s\n' "$case" "$variant" "$report_line" >&2
                    status=1
                fi
            done < "$CASES"
            n_vreplays=$((n_vreplays + 2))
            v=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" "$vir" "$@" 2>/dev/null | tail -1)
            if drools_status "$base_expr" && [ "$v" != "$base_expr" ]; then
                printf 'FAIL: %s — Drools answers differently on the %s copy (vocabulary leg):\n  untouched: %s\n  %s: %s\n' \
                    "$ident" "$variant" "$base_expr" "$variant" "$v" >&2
                status=1
            fi
            dv=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" --drl "$vir" "$@" 2>/dev/null | tail -1)
            if drools_status "$drl_baseline" && [ "$dv" != "$drl_baseline" ]; then
                printf 'FAIL: %s — the DRL-text branch answers differently on the %s copy (vocabulary leg):\n  untouched: %s\n  %s: %s\n' \
                    "$ident" "$variant" "$drl_baseline" "$variant" "$dv" >&2
                status=1
            fi
        done
    done < "$LIST"
    printf 'leg_status=%s n_vgoldens=%s n_v_stripped=%s n_v_swapped=%s n_v_rotated=%s n_vreplays=%s\n' \
        "$status" "$n_vgoldens" "$n_v_stripped" "$n_v_swapped" "$n_v_rotated" "$n_vreplays" > "$WORK/vocabulary.counts"
}

# --- the origin leg (see the script header) --------------------------------------------------------
leg_origin() {
    CASES=$WORK/origin.cases
    n_hgoldens=0
    n_h_stripped=0
    n_hreplays=0
    while IFS= read -r ir; do
        dir=$(dirname "$ir")
        ident=${dir#corpus/golden/}
        hir="$WORK/herkomst/$ident/gestript.json"
        stripped=$(python3 "$HSTRIPPER" "$ir" "$hir") || { echo "FAIL: the origin stripper could not read $ir" >&2; exit 2; }
        [ "$stripped" -gt 0 ] || continue          # no origin: nothing to probe here
        find "$dir" -maxdepth 1 -name 'case*.json' 2>/dev/null | sort > "$CASES"
        [ -s "$CASES" ] || continue                # no case file: not replayable
        n_hgoldens=$((n_hgoldens + 1))
        n_h_stripped=$((n_h_stripped + stripped))
        while IFS= read -r case; do
            n_hreplays=$((n_hreplays + 2))
            if ! report_line=$("$RUNNER_REF" "$hir" "$case" 2>&1); then
                printf 'FAIL: %s — ref answers differently on the stripped copy (origin leg):\n%s\n' "$case" "$report_line" >&2
                status=1
            fi
            if ! clingo_same "$ir" "$hir" "$case"; then
                printf 'FAIL: %s — clingo answers differently on the stripped copy (origin leg):\n%s\n' "$case" "$report_line" >&2
                status=1
            fi
        done < "$CASES"
        set --
        while IFS= read -r case; do set -- "$@" "$case"; done < "$CASES"
        n_hreplays=$((n_hreplays + 2))
        base_expr=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" "$ir" "$@" 2>/dev/null | tail -1)
        h=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" "$hir" "$@" 2>/dev/null | tail -1)
        if ! drools_status "$base_expr"; then
            no_status "$ident" origin Drools "$base_expr"
        elif [ "$h" != "$base_expr" ]; then
            printf 'FAIL: %s — Drools answers differently on the stripped copy (origin leg):\n  untouched: %s\n  stripped:  %s\n' \
                "$ident" "$base_expr" "$h" >&2
            status=1
        fi
        drl_baseline=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" --drl "$ir" "$@" 2>/dev/null | tail -1)
        dh=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" --drl "$hir" "$@" 2>/dev/null | tail -1)
        if ! drools_status "$drl_baseline"; then
            no_status "$ident" origin "the DRL-text branch" "$drl_baseline"
        elif [ "$dh" != "$drl_baseline" ]; then
            printf 'FAIL: %s — the DRL-text branch answers differently on the stripped copy (origin leg):\n  untouched: %s\n  stripped:  %s\n' \
                "$ident" "$drl_baseline" "$dh" >&2
            status=1
        fi
    done < "$LIST"
    printf 'leg_status=%s n_hgoldens=%s n_h_stripped=%s n_hreplays=%s\n' \
        "$status" "$n_hgoldens" "$n_h_stripped" "$n_hreplays" > "$WORK/origin.counts"
}

# --- the name leg (see the script header) ----------------------------------------------------------
leg_name() {
    CASES=$WORK/name.cases
    n_ngoldens=0
    n_n_stripped=0
    n_a_stripped=0
    n_nreplays=0
    while IFS= read -r ir; do
        dir=$(dirname "$ir")
        ident=${dir#corpus/golden/}
        nir="$WORK/naam/$ident/gestript.json"
        counts=$(python3 "$NSTRIPPER" "$ir" "$nir") || { echo "FAIL: the name stripper could not read $ir" >&2; exit 2; }
        stripped=${counts% *}
        annotations=${counts#* }
        [ $((stripped + annotations)) -gt 0 ] || continue   # no named or annotated default: nothing to probe here
        find "$dir" -maxdepth 1 -name 'case*.json' 2>/dev/null | sort > "$CASES"
        [ -s "$CASES" ] || continue                # no case file: not replayable
        n_ngoldens=$((n_ngoldens + 1))
        n_n_stripped=$((n_n_stripped + stripped))
        n_a_stripped=$((n_a_stripped + annotations))
        while IFS= read -r case; do
            n_nreplays=$((n_nreplays + 2))
            if ! report_line=$("$RUNNER_REF" "$nir" "$case" 2>&1); then
                printf 'FAIL: %s — ref answers differently on the stripped copy (name leg):\n%s\n' "$case" "$report_line" >&2
                status=1
            fi
            if ! clingo_same "$ir" "$nir" "$case"; then
                printf 'FAIL: %s — clingo answers differently on the stripped copy (name leg):\n%s\n' "$case" "$report_line" >&2
                status=1
            fi
        done < "$CASES"
        set --
        while IFS= read -r case; do set -- "$@" "$case"; done < "$CASES"
        n_nreplays=$((n_nreplays + 2))
        base_expr=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" "$ir" "$@" 2>/dev/null | tail -1)
        nm=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" "$nir" "$@" 2>/dev/null | tail -1)
        if ! drools_status "$base_expr"; then
            no_status "$ident" name Drools "$base_expr"
        elif [ "$nm" != "$base_expr" ]; then
            printf 'FAIL: %s — Drools answers differently on the stripped copy (name leg):\n  untouched: %s\n  stripped:  %s\n' \
                "$ident" "$base_expr" "$nm" >&2
            status=1
        fi
        drl_baseline=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" --drl "$ir" "$@" 2>/dev/null | tail -1)
        dn=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" --drl "$nir" "$@" 2>/dev/null | tail -1)
        if ! drools_status "$drl_baseline"; then
            no_status "$ident" name "the DRL-text branch" "$drl_baseline"
        elif [ "$dn" != "$drl_baseline" ]; then
            printf 'FAIL: %s — the DRL-text branch answers differently on the stripped copy (name leg):\n  untouched: %s\n  stripped:  %s\n' \
                "$ident" "$drl_baseline" "$dn" >&2
            status=1
        fi
    done < "$LIST"
    printf 'leg_status=%s n_ngoldens=%s n_n_stripped=%s n_a_stripped=%s n_nreplays=%s\n' \
        "$status" "$n_ngoldens" "$n_n_stripped" "$n_a_stripped" "$n_nreplays" > "$WORK/name.counts"
}

# --- the presentation leg (see the script header) --------------------------------------------------
# The outcome of ref or clingo on one case: its report and its exit status, with the name of clingo's
# temporary file made constant (clingo prints it in some info lines).
outcome() {   # <ref|clingo> <ir.json> <case>
    if [ "$1" = ref ]; then
        { "$RUNNER_REF" "$2" "$3" 2>&1; echo "exit $?"; }
    else
        { "$RUNNER_CLINGO" "$2" "$3" 2>&1; echo "exit $?"; } | sed -E 's|[^ ]*uchar-clingo[0-9a-f]+[.]lp|uchar-clingo.lp|g'
    fi
}
# A difference on the combined copy is assigned to a field by the copies with that field alone changed: a
# field moves the answer when the executor's outcome on its copy differs from the outcome on the untouched
# golden (for ref and clingo the outcome of the case, for a Drools branch the status line). ref and clingo
# are judged above against the pinned expectations, so the combined copy can fail where the untouched golden
# fails too; then no field is assigned, and gates 5 and 6 are the ones that hold that case.
# A Drools status line on a copy against the untouched one: equal, or equal once every snippet the source change
# moved is rendered in the untouched line as the copy has it, inside the quotes Drools writes around a snippet
# ('{snippet}' in its refusal sentences). Only those quoted snippets move; any other difference stays one.
same_drools_line() {   # <copies' directory> <untouched line> <copy's line>
    [ "$2" = "$3" ] && return 0
    [ -f "$1/snippets.json" ] || return 1
    python3 "$PRESENTER" --same-line "'" "$1/snippets.json" "$2" "$3"
}
fields_alone() {   # <executor> <untouched ir.json> <copies' directory> <file of case paths> <exec-model line> <DRL-text line>
    fa_moved=
    case $1 in
        ref|clingo)
            fa_case=$(cat "$4")
            fa_base=$(outcome "$1" "$2" "$fa_case")
            if [ "$(outcome "$1" "$3/combined.json" "$fa_case")" = "$fa_base" ]; then
                echo "  $1 gives the same outcome on the untouched golden: it misses the pinned outcome there too (gates 5 and 6), so no field is assigned"
                return
            fi
            for fa_field in kind rule-presentation preposition percentage rule-annotations diagnostics source rule-group; do
                [ -f "$3/$fa_field.json" ] || continue
                fa_out=$(outcome "$1" "$3/$fa_field.json" "$fa_case")
                [ "$fa_out" = "$fa_base" ] && continue
                # a clingo refusal that differs only in a quoted snippet, as in clingo_refused_alike
                [ "$1" = clingo ] && [ "$fa_field" = source ] \
                    && python3 "$PRESENTER" --same-line '"' "$3/snippets.json" "$fa_base" "$fa_out" && continue
                fa_moved="$fa_moved $fa_field"
            done ;;
        drools|drl)
            fa_flag= fa_line=$5
            [ "$1" = drl ] && fa_flag=--drl fa_line=$6
            fa_dir=$3 fa_cases=$4
            set --
            while IFS= read -r fa_case; do set -- "$@" "$fa_case"; done < "$fa_cases"
            for fa_field in kind rule-presentation preposition percentage rule-annotations diagnostics source rule-group; do
                [ -f "$fa_dir/$fa_field.json" ] || continue
                fa_copy=$(java $PARALLEL_JVM_FLAGS -jar "$JAR" $fa_flag "$fa_dir/$fa_field.json" "$@" 2>/dev/null | tail -1)
                if [ "$fa_field" = source ]; then
                    same_drools_line "$fa_dir" "$fa_line" "$fa_copy" || fa_moved="$fa_moved $fa_field"
                else
                    [ "$fa_copy" = "$fa_line" ] || fa_moved="$fa_moved $fa_field"
                fi
            done ;;
    esac
    if [ -n "$fa_moved" ]; then
        echo "  the field(s) that move the answer on their own:$fa_moved"
    else
        echo "  no field moves the answer on its own: only the combination does"
    fi
}
leg_presentation() {
    CASES=$WORK/presentation.cases
    TASKS=$WORK/presentation.tasks
    ONE=$WORK/presentation.one
    mkdir -p "$TASKS" || exit 2
    jobs=$(parallel_jobs 2) || exit 2
    : > "$WORK/presentation.list"
    i=0
    while IFS= read -r ir; do
        dir=$(dirname "$ir")
        ident=${dir#corpus/golden/}
        find "$dir" -maxdepth 1 -name 'case*.json' 2>/dev/null | sort > "$CASES"
        [ -s "$CASES" ] || continue                # no case file: not replayable
        pdir="$WORK/presentation/$ident"
        counts=$(python3 "$PRESENTER" "$ir" "$pdir") || { echo "FAIL: the presentation copier could not read $ir" >&2; exit 2; }
        [ -f "$pdir/combined.json" ] || continue   # no presentation field: nothing to probe here
        i=$((i + 1))
        printf '%s %s %s\n' "$i" "$dir" "$counts" >> "$WORK/presentation.list"
        cp "$CASES" "$pdir/cases" || exit 2
        c=0
        while IFS= read -r case; do
            c=$((c + 1))
            parallel_task "$TASKS" "$i.ref.$c" out "$RUNNER_REF" "$pdir/combined.json" "$case"
            parallel_task "$TASKS" "$i.clingo.$c" out "$RUNNER_CLINGO" "$pdir/combined.json" "$case"
        done < "$CASES"
        set --
        while IFS= read -r case; do set -- "$@" "$case"; done < "$CASES"
        parallel_task "$TASKS" "$i.exec" null java $PARALLEL_JVM_FLAGS -jar "$JAR" "$ir" "$@"
        parallel_task "$TASKS" "$i.exec.copy" null java $PARALLEL_JVM_FLAGS -jar "$JAR" "$pdir/combined.json" "$@"
        parallel_task "$TASKS" "$i.drl" null java $PARALLEL_JVM_FLAGS -jar "$JAR" --drl "$ir" "$@"
        parallel_task "$TASKS" "$i.drl.copy" null java $PARALLEL_JVM_FLAGS -jar "$JAR" --drl "$pdir/combined.json" "$@"
    done < "$LIST"
    parallel_run "$TASKS" "$jobs" || exit 2

    n_pgoldens=0
    n_kind_goldens=0 n_kind=0
    n_rp_goldens=0 n_rp=0
    n_prep_goldens=0 n_prep=0
    n_pct_goldens=0 n_pct=0
    n_rg_goldens=0 n_rg=0
    n_ra_goldens=0 n_ra=0
    n_diag_goldens=0 n_diag=0
    n_src_goldens=0 n_src=0 n_srcx=0 n_rendered=0 n_rendered_clingo=0
    n_preplays=0
    while read -r i dir nk nrp nprep npct nra ndiag nsrc nsrcx nrg; do
        ident=${dir#corpus/golden/}
        ir=$dir/ir.json
        pdir="$WORK/presentation/$ident"
        n_pgoldens=$((n_pgoldens + 1))
        [ "$nk" -eq 0 ] || { n_kind_goldens=$((n_kind_goldens + 1)); n_kind=$((n_kind + nk)); }
        [ "$nrp" -eq 0 ] || { n_rp_goldens=$((n_rp_goldens + 1)); n_rp=$((n_rp + nrp)); }
        [ "$nprep" -eq 0 ] || { n_prep_goldens=$((n_prep_goldens + 1)); n_prep=$((n_prep + nprep)); }
        [ "$npct" -eq 0 ] || { n_pct_goldens=$((n_pct_goldens + 1)); n_pct=$((n_pct + npct)); }
        [ "$nrg" -eq 0 ] || { n_rg_goldens=$((n_rg_goldens + 1)); n_rg=$((n_rg + nrg)); }
        [ "$nra" -eq 0 ] || { n_ra_goldens=$((n_ra_goldens + 1)); n_ra=$((n_ra + nra)); }
        [ "$ndiag" -eq 0 ] || { n_diag_goldens=$((n_diag_goldens + 1)); n_diag=$((n_diag + ndiag)); }
        [ $((nsrc + nsrcx)) -eq 0 ] || { n_src_goldens=$((n_src_goldens + 1)); n_src=$((n_src + nsrc)); n_srcx=$((n_srcx + nsrcx)); }
        c=0
        while IFS= read -r case; do
            c=$((c + 1))
            n_preplays=$((n_preplays + 2))
            printf '%s\n' "$case" > "$ONE"
            if [ "$(cat "$TASKS/$i.ref.$c.rc")" != 0 ]; then
                printf 'FAIL: %s — ref answers differently on the presentation copy:\n%s\n' "$case" "$(cat "$TASKS/$i.ref.$c.out")" >&2
                fields_alone ref "$ir" "$pdir" "$ONE" "" "" >&2
                status=1
            fi
            report_line=$(cat "$TASKS/$i.clingo.$c.out")
            if [ "$(cat "$TASKS/$i.clingo.$c.rc")" != 0 ]; then
                if ! clingo_refused_alike "$ir" "$case" "$pdir"; then
                    printf 'FAIL: %s — clingo answers differently on the presentation copy:\n%s\n' "$case" "$(cat "$TASKS/$i.clingo.$c.out")" >&2
                    fields_alone clingo "$ir" "$pdir" "$ONE" "" "" >&2
                    status=1
                elif [ "$clingo_rendered" = 1 ]; then
                    n_rendered_clingo=$((n_rendered_clingo + 1))
                fi
            fi
        done < "$pdir/cases"
        n_preplays=$((n_preplays + 2))
        base_expr=$(tail -1 "$TASKS/$i.exec.out")
        p=$(tail -1 "$TASKS/$i.exec.copy.out")
        drl_baseline=$(tail -1 "$TASKS/$i.drl.out")
        dp=$(tail -1 "$TASKS/$i.drl.copy.out")
        if ! drools_status "$base_expr"; then
            no_status "$ident" presentation Drools "$base_expr"
        elif [ "$p" != "$base_expr" ]; then
            if same_drools_line "$pdir" "$base_expr" "$p"; then
                n_rendered=$((n_rendered + 1))
            else
                printf 'FAIL: %s — Drools answers differently on the presentation copy:\n  untouched: %s\n  changed:   %s\n' \
                    "$ident" "$base_expr" "$p" >&2
                fields_alone drools "$ir" "$pdir" "$pdir/cases" "$base_expr" "$drl_baseline" >&2
                status=1
            fi
        fi
        if ! drools_status "$drl_baseline"; then
            no_status "$ident" presentation "the DRL-text branch" "$drl_baseline"
        elif [ "$dp" != "$drl_baseline" ]; then
            if same_drools_line "$pdir" "$drl_baseline" "$dp"; then
                n_rendered=$((n_rendered + 1))
            else
                printf 'FAIL: %s — the DRL-text branch answers differently on the presentation copy:\n  untouched: %s\n  changed:   %s\n' \
                    "$ident" "$drl_baseline" "$dp" >&2
                fields_alone drl "$ir" "$pdir" "$pdir/cases" "$base_expr" "$drl_baseline" >&2
                status=1
            fi
        fi
    done < "$WORK/presentation.list"
    printf 'leg_status=%s n_pgoldens=%s n_kind_goldens=%s n_kind=%s n_rp_goldens=%s n_rp=%s n_prep_goldens=%s n_prep=%s n_pct_goldens=%s n_pct=%s n_rg_goldens=%s n_rg=%s n_ra_goldens=%s n_ra=%s n_diag_goldens=%s n_diag=%s n_src_goldens=%s n_src=%s n_srcx=%s n_rendered=%s n_rendered_clingo=%s n_preplays=%s\n' \
        "$status" "$n_pgoldens" "$n_kind_goldens" "$n_kind" "$n_rp_goldens" "$n_rp" "$n_prep_goldens" "$n_prep" \
        "$n_pct_goldens" "$n_pct" "$n_rg_goldens" "$n_rg" "$n_ra_goldens" "$n_ra" \
        "$n_diag_goldens" "$n_diag" "$n_src_goldens" "$n_src" "$n_srcx" "$n_rendered" "$n_rendered_clingo" "$n_preplays" > "$WORK/presentation.counts"
}

# --- the five legs, side by side --------------------------------------------------------------------
# The unit, vocabulary, origin, name and presentation legs share nothing but the list of goldens and the jar,
# so they run as five processes at once, with its report into $WORK/<leg>.log, its exit status into
# $WORK/<leg>.rc and its counts into $WORK/<leg>.counts. The gate then reads them in the order of the legs,
# each report and the checks after it in the place they have, so the report and the first failure are those
# of the five legs one after the other. The first four go one golden after the other; a replay JVM with
# PARALLEL_JVM_FLAGS uses 1.5 cores (scripts/lib/parallel.sh), so they keep about 6 cores busy. The
# presentation leg replays nearly every golden, too many for one process, so it runs its calls through
# scripts/lib/parallel.sh on one process per two cores, as gates 12 and 17 do, and judges them in corpus order.
echo "  legs: 5 processes (unit, vocabulary, origin, name, presentation)"
for leg in twist vocabulary origin name presentation; do
    { ( "leg_$leg" ) > "$WORK/$leg.log" 2>&1; echo $? > "$WORK/$leg.rc"; } &
done
wait

# The report of a leg, in its place. A leg that stopped early (a twister or a stripper could not read a
# golden: its FAIL line, then status 2) stops the gate there with status 2; a leg whose process ended in
# any other way without its counts fails the gate by the name of the leg.
leg_report() {
    cat "$WORK/$1.log" >&2
    leg_rc=$(cat "$WORK/$1.rc" 2>/dev/null)
    if [ "$leg_rc" = 2 ]; then
        exit 2
    fi
    if [ "$leg_rc" != 0 ] || [ ! -f "$WORK/$1.counts" ]; then
        echo "FAIL: the $1 leg ended with status ${leg_rc:-none} and without its counts — its process crashed or was killed" >&2
        exit 1
    fi
    . "$WORK/$1.counts"
    [ "$leg_status" -eq 0 ] || status=1
}
leg_report twist
leg_report vocabulary
if [ "$n_vgoldens" -eq 0 ]; then
    echo 'FAIL: no golden declares a vocabulary — the vocabulary leg probed nothing (an empty set is never green)' >&2
    exit 1
fi
if [ "$n_vgoldens" -lt "$EXPECTED_MIN_VOCABULARY_GOLDENS" ]; then
    echo "FAIL: the vocabulary leg probed $n_vgoldens golden(s), but EXPECTED_MIN_VOCABULARY_GOLDENS pins at least $EXPECTED_MIN_VOCABULARY_GOLDENS — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi
if [ "$n_v_stripped" -lt "$EXPECTED_MIN_VOCABULARY_STRIPPED" ]; then
    echo "FAIL: the vocabulary leg stripped $n_v_stripped designation(s), but EXPECTED_MIN_VOCABULARY_STRIPPED pins at least $EXPECTED_MIN_VOCABULARY_STRIPPED — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi
if [ "$n_v_rotated" -lt "$EXPECTED_MIN_VOCABULARY_ROTATED" ]; then
    echo "FAIL: the vocabulary leg rotated $n_v_rotated designation(s), but EXPECTED_MIN_VOCABULARY_ROTATED pins at least $EXPECTED_MIN_VOCABULARY_ROTATED — the rotation moves less than it did (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi

leg_report origin
if [ "$n_hgoldens" -eq 0 ]; then
    echo 'FAIL: no golden carries an origin — the origin leg probed nothing (an empty set is never green)' >&2
    exit 1
fi
if [ "$n_hgoldens" -lt "$EXPECTED_MIN_ORIGIN_GOLDENS" ]; then
    echo "FAIL: the origin leg probed $n_hgoldens golden(s), but EXPECTED_MIN_ORIGIN_GOLDENS pins at least $EXPECTED_MIN_ORIGIN_GOLDENS — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi
if [ "$n_h_stripped" -lt "$EXPECTED_MIN_ORIGIN_STRIPPED" ]; then
    echo "FAIL: the origin leg stripped $n_h_stripped origin(s), but EXPECTED_MIN_ORIGIN_STRIPPED pins at least $EXPECTED_MIN_ORIGIN_STRIPPED — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi

leg_report name
if [ "$n_ngoldens" -eq 0 ]; then
    echo 'FAIL: no golden carries a named default — the name leg probed nothing (an empty set is never green)' >&2
    exit 1
fi
if [ "$n_ngoldens" -lt "$EXPECTED_MIN_NAME_GOLDENS" ]; then
    echo "FAIL: the name leg probed $n_ngoldens golden(s), but EXPECTED_MIN_NAME_GOLDENS pins at least $EXPECTED_MIN_NAME_GOLDENS — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi
if [ "$n_n_stripped" -lt "$EXPECTED_MIN_NAME_STRIPPED" ]; then
    echo "FAIL: the name leg stripped $n_n_stripped name(s), but EXPECTED_MIN_NAME_STRIPPED pins at least $EXPECTED_MIN_NAME_STRIPPED — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi
if [ "$n_a_stripped" -lt "$EXPECTED_MIN_ANNOTATIONS_STRIPPED" ]; then
    echo "FAIL: the name leg stripped $n_a_stripped statuteAnnotations list(s) of defaults, but EXPECTED_MIN_ANNOTATIONS_STRIPPED pins at least $EXPECTED_MIN_ANNOTATIONS_STRIPPED — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi

leg_report presentation
# A field the leg changed in no golden, or in fewer goldens or places than its floor, fails by its name.
presentation_floor() {   # <count> <floor> <name of the floor> <what was counted>
    if [ "$1" -eq 0 ]; then
        echo "FAIL: the presentation leg found no $4 — it probed nothing there (an empty set is never green)" >&2
        exit 1
    fi
    if [ "$1" -lt "$2" ]; then
        echo "FAIL: the presentation leg counted $1 $4, but $3 pins at least $2 — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
        exit 1
    fi
}
presentation_floor "$n_pgoldens" "$EXPECTED_MIN_PRESENTATION_GOLDENS" EXPECTED_MIN_PRESENTATION_GOLDENS 'golden(s) with a presentation field and case files'
presentation_floor "$n_kind_goldens" "$EXPECTED_MIN_KIND_GOLDENS" EXPECTED_MIN_KIND_GOLDENS 'golden(s) with a characteristic'
presentation_floor "$n_kind" "$EXPECTED_MIN_KIND_ROTATED" EXPECTED_MIN_KIND_ROTATED 'characteristic kind(s) rotated'
presentation_floor "$n_rp_goldens" "$EXPECTED_MIN_RULE_PRESENTATION_GOLDENS" EXPECTED_MIN_RULE_PRESENTATION_GOLDENS 'golden(s) with a tagged rule version'
presentation_floor "$n_rp" "$EXPECTED_MIN_RULE_PRESENTATION_EXCHANGED" EXPECTED_MIN_RULE_PRESENTATION_EXCHANGED 'presentation value(s) of a rule version exchanged'
presentation_floor "$n_prep_goldens" "$EXPECTED_MIN_PREPOSITION_GOLDENS" EXPECTED_MIN_PREPOSITION_GOLDENS 'golden(s) with a dimension preposition'
presentation_floor "$n_prep" "$EXPECTED_MIN_PREPOSITION_STRIPPED" EXPECTED_MIN_PREPOSITION_STRIPPED 'preposition(s) stripped'
presentation_floor "$n_pct_goldens" "$EXPECTED_MIN_PERCENTAGE_GOLDENS" EXPECTED_MIN_PERCENTAGE_GOLDENS 'golden(s) with a percentage tag'
presentation_floor "$n_pct" "$EXPECTED_MIN_PERCENTAGE_STRIPPED" EXPECTED_MIN_PERCENTAGE_STRIPPED 'percentage tag(s) stripped'
presentation_floor "$n_rg_goldens" "$EXPECTED_MIN_RULE_GROUP_GOLDENS" EXPECTED_MIN_RULE_GROUP_GOLDENS 'golden(s) with a non-recursive rule group'
presentation_floor "$n_rg" "$EXPECTED_MIN_RULE_GROUP_STRIPPED" EXPECTED_MIN_RULE_GROUP_STRIPPED 'non-recursive rule group(s) stripped'
presentation_floor "$n_ra_goldens" "$EXPECTED_MIN_RULE_ANNOTATIONS_GOLDENS" EXPECTED_MIN_RULE_ANNOTATIONS_GOLDENS 'golden(s) with an annotated rule'
presentation_floor "$n_ra" "$EXPECTED_MIN_RULE_ANNOTATIONS_STRIPPED" EXPECTED_MIN_RULE_ANNOTATIONS_STRIPPED 'statuteAnnotations list(s) of rules stripped'
presentation_floor "$n_diag_goldens" "$EXPECTED_MIN_DIAGNOSTICS_GOLDENS" EXPECTED_MIN_DIAGNOSTICS_GOLDENS 'golden(s) with a diagnostic'
presentation_floor "$n_diag" "$EXPECTED_MIN_DIAGNOSTICS_EXCHANGED" EXPECTED_MIN_DIAGNOSTICS_EXCHANGED 'diagnostic(s) exchanged'
presentation_floor "$n_src_goldens" "$EXPECTED_MIN_SOURCE_GOLDENS" EXPECTED_MIN_SOURCE_GOLDENS 'golden(s) with a source record'
presentation_floor "$n_src" "$EXPECTED_MIN_SOURCE_STRIPPED" EXPECTED_MIN_SOURCE_STRIPPED 'source record(s) stripped'
presentation_floor "$n_srcx" "$EXPECTED_MIN_SOURCE_EXCHANGED" EXPECTED_MIN_SOURCE_EXCHANGED 'Unresolved snippet(s) exchanged'

# The survivor list, BOTH halves (the check-noop pattern): an entry whose golden is gone is stale,
# and so is an entry that no longer diverges — it then admits an exception nothing needs any more,
# which is exactly how a list stops describing the system it pins.
dropped=$(printf '%s\n' "$SURVIVORS" | while IFS= read -r s; do
    [ -n "$s" ] || continue
    g=${s%%|*}
    rest=${s#*|}
    ex=${rest%%|*}
    # (no `case` here: /bin/sh on macOS is bash 3.2, whose parser cannot read one inside $(...))
    if [ "$ex" != ref ] && [ "$ex" != clingo ] && [ "$ex" != drools ] && [ "$ex" != drl ]; then
        echo "  $g|$ex — unknown executor (expected ref, clingo, drools or drl)"
    elif [ ! -d "corpus/golden/$g" ]; then
        echo "  $g|$ex — no such golden"
    elif ! grep -qxF "$g|$ex" "$HITS" 2>/dev/null; then
        echo "  $g|$ex — no longer diverges under the twist; drop the entry"
    fi
done)
if [ -n "$dropped" ]; then
    printf 'FAIL: stale survivor entry:\n%s\n' "$dropped" >&2
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

# The same bottom for the DRL leg, kept beside the line equality above (which already makes a text branch
# that refused every golden red on every untouched golden the exec-model branch plays): an empty played set
# is never green. Measured 2026-09-04 with exactly that mutant — a refusal at the front of CaseRunner's DRL
# branch: the leg fell from 9 played goldens to 0 and the gate still said PASS. NB the count is the STATUS
# HEAD, so a golden whose every case file carried expectedFault would replay nothing and still print
# "PLAYED 0", counting here — the corpus zone discipline gate 4 pins (golden carries expected, red carries
# expectedFault) makes that shape unreachable today, and this bottom must not come to lean on it.
if [ "$n_drl" -eq 0 ]; then
    echo 'FAIL: the DRL-text branch PLAYED no golden — the leg measured nothing (an empty played set is never green)' >&2
    exit 1
fi
if [ "$n_drl" -ne "$n_exec_played" ]; then
    echo "FAIL: the DRL-text branch PLAYED $n_drl twistable golden(s) where the exec-model branch PLAYED $n_exec_played — the two render one analysis, so the counts are equal or a line above already failed" >&2
    exit 1
fi

n_fourth=$(awk '$1 == "fourth" {n++} END {print n+0}' "$UNCOVERED")
n_unknown=$(awk '$1 == "unknown" {n++} END {print n+0}' "$UNCOVERED")
n_uncovered=$((n_fourth + n_unknown))
if [ "$n_fourth" -gt 0 ]; then
    echo 'FAIL: a unit-bearing annotation sits at a node kind whose result unit v0 does not derive —' >&2
    echo '      the §2.10 fourth-class witness has arrived. The node kind is one the twist table carries,' >&2
    echo '      but THIS operator/kind branch has no join rule (power, an' >&2
    echo '      absolute value, a root, a date/time extractor), so a twist here would' >&2
    echo '      probe a rule that does not exist and report its absence as a consumer defect. What the' >&2
    echo '      witness must buy is a derivation rule (or a fence) — never a wider licence to read the' >&2
    echo '      wire. ref/clingo and the Drools backend answer such a position DIFFERENTLY today (ch.' >&2
    echo '      2.10), so the divergence is now observable and must be settled:' >&2
    awk '$1 == "fourth" {print "  " $2 " " $3}' "$UNCOVERED" | sort | uniq -c >&2
    status=1
fi
if [ "$n_unknown" -gt 0 ]; then
    echo 'FAIL: a unit-bearing annotation sits at a node kind the twist table does not carry, and that' >&2
    echo '      the exception set does not exempt — this harness probes nothing there, so no consumer is' >&2
    echo '      held to the declaration at that position. Give the kind a twist class (with its own' >&2
    echo '      factor exponent) or an exception with the reason its annotation IS the source:' >&2
    awk '$1 == "unknown" {print "  " $2 " " $3}' "$UNCOVERED" | sort | uniq -c >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    printf '  NOTE (ch. 5.10): the DRL-TEXT branch no longer diverges from ch. 5.10. The maintainer\n'
    printf '       decision of 2026-09-07 added at-most'"'"'s TRUE trigger, which makes the norm the exact dual\n'
    printf '       of at-least — and that dual is what the branch already computed. Remeasured over the full\n'
    printf '       read-set model (n = 1..7, 6646 combinations, both directions, all five quantifiers):\n'
    printf '       0 verdict divergences, 0 extra reads, 0 skipped reads, for the text branch and for clingo.\n'
    printf '       The former watchlist is retired; see the comment above.\n'
    printf 'check-reftype-derivation: PASS (%s unit annotations twisted over %s goldens, floor %s; %s executor replays reproduce the untouched outcome, %s of them PLAYED by the DRL-text branch, equal to the exec-model branch; %s pinned survivors)\n' \
        "$n_twists" "$n_goldens" "$EXPECTED_MIN_TWISTS" "$n_checked" "$n_drl" "$n_survivors"
    printf '  vocabulary leg: %s golden(s) with vocabularies, floor %s; %s designations stripped (floor %s), %s exchanged, %s rotated (floor %s); %s executor replays reproduce the untouched outcome\n' \
        "$n_vgoldens" "$EXPECTED_MIN_VOCABULARY_GOLDENS" "$n_v_stripped" "$EXPECTED_MIN_VOCABULARY_STRIPPED" \
        "$n_v_swapped" "$n_v_rotated" "$EXPECTED_MIN_VOCABULARY_ROTATED" "$n_vreplays"
    printf '  origin leg: %s golden(s) with origins, floor %s; %s origins stripped (floor %s); %s executor replays reproduce the untouched outcome\n' \
        "$n_hgoldens" "$EXPECTED_MIN_ORIGIN_GOLDENS" "$n_h_stripped" "$EXPECTED_MIN_ORIGIN_STRIPPED" "$n_hreplays"
    printf '  name leg: %s golden(s) with named or annotated defaults, floor %s; %s names stripped (floor %s), %s statuteAnnotations lists (floor %s); %s executor replays reproduce the untouched outcome\n' \
        "$n_ngoldens" "$EXPECTED_MIN_NAME_GOLDENS" "$n_n_stripped" "$EXPECTED_MIN_NAME_STRIPPED" \
        "$n_a_stripped" "$EXPECTED_MIN_ANNOTATIONS_STRIPPED" "$n_nreplays"
    printf '  presentation leg: %s golden(s) on one combined copy each, floor %s; %s characteristic kinds rotated over %s goldens (floors %s, %s), %s rule presentation values exchanged over %s (floors %s, %s), %s prepositions stripped over %s (floors %s, %s), %s percentage tags stripped over %s (floors %s, %s), %s non-recursive rule groups stripped over %s (floors %s, %s), %s statuteAnnotations lists of rules stripped over %s (floors %s, %s), %s diagnostics exchanged over %s (floors %s, %s), %s source records stripped and %s Unresolved snippets exchanged over %s (floors %s, %s, %s; %s Drools status lines and %s clingo refusals differ only in a quoted snippet); %s executor replays reproduce the untouched outcome\n' \
        "$n_pgoldens" "$EXPECTED_MIN_PRESENTATION_GOLDENS" \
        "$n_kind" "$n_kind_goldens" "$EXPECTED_MIN_KIND_ROTATED" "$EXPECTED_MIN_KIND_GOLDENS" \
        "$n_rp" "$n_rp_goldens" "$EXPECTED_MIN_RULE_PRESENTATION_EXCHANGED" "$EXPECTED_MIN_RULE_PRESENTATION_GOLDENS" \
        "$n_prep" "$n_prep_goldens" "$EXPECTED_MIN_PREPOSITION_STRIPPED" "$EXPECTED_MIN_PREPOSITION_GOLDENS" \
        "$n_pct" "$n_pct_goldens" "$EXPECTED_MIN_PERCENTAGE_STRIPPED" "$EXPECTED_MIN_PERCENTAGE_GOLDENS" \
        "$n_rg" "$n_rg_goldens" "$EXPECTED_MIN_RULE_GROUP_STRIPPED" "$EXPECTED_MIN_RULE_GROUP_GOLDENS" \
        "$n_ra" "$n_ra_goldens" "$EXPECTED_MIN_RULE_ANNOTATIONS_STRIPPED" "$EXPECTED_MIN_RULE_ANNOTATIONS_GOLDENS" \
        "$n_diag" "$n_diag_goldens" "$EXPECTED_MIN_DIAGNOSTICS_EXCHANGED" "$EXPECTED_MIN_DIAGNOSTICS_GOLDENS" \
        "$n_src" "$n_srcx" "$n_src_goldens" "$EXPECTED_MIN_SOURCE_STRIPPED" "$EXPECTED_MIN_SOURCE_EXCHANGED" \
        "$EXPECTED_MIN_SOURCE_GOLDENS" "$n_rendered" "$n_rendered_clingo" "$n_preplays"
    printf '  corpus-wide accounting: %s unit-bearing annotations = %s twistable + %s exempt (the annotation IS the source) + %s without a convertible sister + %s at a §2.10 fourth-class branch (v0 derives no result unit) + %s outside the twist table\n' \
        "$n_unit" "$((n_unit - n_exempt - n_without_sister - n_uncovered))" \
        "$n_exempt" "$n_without_sister" "$n_fourth" "$n_unknown"
fi
exit "$status"
