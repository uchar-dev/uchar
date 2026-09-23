# wf037-aantal-opgesomd-element

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in the count's ARITY clause,
in the spelling that ALSO claims an instance source: an `AANTAL` carrying
`elementObjecttype: "Vondst"` and, beside it, an operand list of two
ordinary reads.

    WF-037 FAIL /rules/resolvedRegels/0/waarde: a count folds INSTANCES and has
    no LISTED form, and this node carries 2 operands: the source language
    spells a count over a subject expression alone (ch. 4.3), so there is
    no articulated operand list to admit — the only reading an operand
    list leaves a count is the number of FILLED operands, and that meaning
    belongs to the filled count over its set source, never to this kind

Why a third witness. The arm deliberately does NOT read
`elementObjecttype`, and this document is what that decision costs and
buys. The neighbouring clause — the one that defines the listed value fold
— keys on `elementObjecttype = null`, so an arm written in its image would
have let this spelling through. It must not: an element type beside an
operand list is not a milder defect but a second claim on one node, "fold
the Vondst instances" and "fold this list", and the wire may not say two
things at once (the same reason the set-source decision of 2026-09-19
gave).

The document. `wf037-aantal-opgesomd` with `Vondst` and the
`vondstaanmelding` fact type restored and `elementObjecttype` set to
`Vondst`. There is no `bronRol`: the element type is reachable from the
subject through exactly one fact type, so WF-047 and WF-048 are silent and
the verifier reports one fault.

Executors, measured on this document before the arm landed:

    verifier    : PASS
    ref         : EVALUATION FAULT: unsupported instance-fold source shape
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : EXCLUDE transpiler SKIP (aggregatie buiten Phase-C scope);
                  DRL-text branch EXCLUDE drl-transpiler MARKER (TODO
                  aggregatie AANTAL)

This spelling is therefore not the one-answers-two-refuse gap its two
siblings pin — here ALL THREE executors refuse. What it pins is the wire
itself: a verifier-clean document no executor can evaluate is a
well-formedness rule that is missing, not a runtime that is incomplete.
Both classes are spec faults under `spec/PLAN.md` §6, and one arm closes
them together.

**Read the `ref` line above as LOUD, not as NAMED.** `unsupported
instance-fold source shape` is the residual arm over fold-source
combinations and it states the position rather than the combination
rejected — it is one of the thirty-six bottom sentences
`scripts/check-rood.sh` forbids a red case to pin, for exactly that reason.
It is quoted here as the measured behaviour of this document and as nothing
more: it witnesses that `ref` refuses, never WHAT it refused. That is also
why this case belongs in `corpus/negatief` and could not be a red one — the
refusal that identifies the form is the verifier's, and it is the sentence
at the top of this note.

And it had a producer route. The RegelSpraak frontend's grammar carries an
operand list at the count, so "het aantal vondsten van de Jutter en kavels
van de Jutter" bound `Vondst` as its element from the first operand's
written word and emitted this shape — arity 2 WITH an element type — under a
warning that does not stop emission. Since 2026-09-20 the frontend refuses
it at the node with the same ERROR its two siblings get, which is the point
of the arm being blind to `elementObjecttype` on both sides of the wire.

Rood-eerst: pre-refusal verifier (binary md5
`c7356ed0f3002e933a550b899e888a1a`) says `verifier: PASS`; with the arm,
the single rejection above. Falsifiable, arm-exact: the shared mutant of
`wf037-aantal-opgesomd` turns this document clean too. A narrower mutant
falsifies the element-type decision on its own — add
`&& f.element_objecttype = None` to the arm and this document alone turns
clean, while its two siblings stay red.
