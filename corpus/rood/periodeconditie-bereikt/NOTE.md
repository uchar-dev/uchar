# periodeconditie-bereikt

The **TimeWindow condition** (`IR-SPEC.md` ch. 4.5, v0 wire node
`Periode`): a `van <date> tot <date>` window written as a rule's whole
condition, which gates the rule on the evaluation date. This document
is verifier-clean — the node is legal, well-formed wire content, and a
golden already carries one (`corpus/golden/jutterswet-vormenkabinet`,
inside an `ALLE` composite no case reaches) — but here the case
*reaches* it: one `Vondst`, an `asOfDate` inside the window, and the
window is the only thing standing between the rule and its value.

Both OCaml executors decline it **by name**, and that is what this
witness pins: `ref` at evaluation, the ASP backend at translation. The
refusal is not a missing evaluation date. Both executors have one
(they already gate `Geldigheid` with it), and the sentence is the same
at an `asOfDate` inside the window, outside it, and null. It is the
construct that has no reading here.

## Why a refusal and not a semantics

`RUNTIME-SEMANTICS.md` ch. 8.2 states in one sentence that the period
condition tests the evaluation date against a window, and ch. 5.11
already records that `ref` declines the form. There is no worked
example, and the clause calls it "the ch. 3.10 period condition" —
which is the **validity** window, a different window: ch. 3.10's two
bounds are both inclusive, while this node's are from-inclusive,
to-exclusive unless `toInclusive` (ch. 4.5). Reusing the validity gate
would therefore be a guess with a known off-by-one in it, and the
bound semantics are only half the question: what a time-free session
makes of the window, and what the window does to a temporal target,
are unstated too. So the two executors refuse, each naming the
construct and the missing clause.

## The square splits two-two

Measured 2026-09-20 on exactly this document, before any arm existed:

    verifier            PASS
    ref                 EVALUATION FAULT: condition kind not implemented
                        in this card
    clingo              TRANSLATION FAULT: condition kind not translated
                        (no corpus case exercises it)
    Drools exec-model   PLAYED 1 — vondstpremie 50
    Drools DRL-tekst    PLAYED 1 — vondstpremie 50

The two OCaml sentences were **bottom sentences**, both standing
verbatim in the forbidden list of `scripts/check-rood.sh`: they
reported the position their residual arm sits at, never the construct.
The named arms this witness pins replaced them.

The two bean-model branches genuinely execute the window — they lower
it to "rekendatum inside [vanaf, tot)" — and the measurement is
two-sided, so it is not a vacuous pass: with the expectation moved to
99 each branch reports `FAIL … attribuut vondstpremie: verwacht 99,
werkelijk 50`; at an `asOfDate` of 2026-09-01 each plays the attribute
empty; and at a null `asOfDate` each plays it empty as well, the
lowering putting `rekendatum != null` in front. They are therefore
listed in the gate's Drools exclusion list as `NOFAULT`, with that
reason: a red form the backend does not refuse because it carries it.

Two of four executors run this construct and two decline it, so no
golden of this shape can enter the corpus: the differential square has
no arbitrator for it. That is the standing argument for norming the
period family — and the day it is normed, this directory stops being
red and the gate's stale guards say so, in both the bodemzin list and
the Drools exclusion list.
