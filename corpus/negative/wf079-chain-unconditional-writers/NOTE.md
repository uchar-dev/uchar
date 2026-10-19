Violates WF-079 (`spec/IR-SPEC.md` ch. 3.1; `RUNTIME-SEMANTICS.md` ch. 7.10):
two unconditional computing rules write one attribute through a type chain
that declares no defaults group. `uitkering van een strandrecht` sets the
`uitkering` of a `Strandrecht` to its `grondslag`; `uitkering van een
wrakrecht` sets the `uitkering` of a `Wrakrecht` to its `grondslag` times 2;
`Wrakrecht` is a `Strandrecht`. The supertype's rule comes first in the
document.

    WF-079 FAIL /rules/rules/1/versions/0: two unconditional computing rules
    write target (Wrakrecht, uitkering) through its type chain — one declared
    on Strandrecht, one on Wrakrecht — and no defaults group folds them into
    one stack, so both fire on a Wrakrecht instance; priority is explicit (a
    defaults group, whose subtype base replaces the inherited base, ch. 7.10),
    never implicit agenda order (ch. 7.7)

The report cites the version on the lower type of the pair, `Wrakrecht`, the
first concrete type both rules fire on. With the two rules swapped in the
document the same sentence cites `/rules/rules/0/versions/0`.

**Why both fire on a `Wrakrecht`.** The fold of ch. 7.10 — an ordinary rule
read as the base of its level, the subtype's base replacing the inherited
one — is gated: a rule is absorbed only on a target whose chain declares a
defaults group somewhere. This chain declares none, so neither rule enters a
stack. The supertype's rule quantifies over every `Strandrecht`, a `Wrakrecht`
included, and the subtype's rule over every `Wrakrecht`: a `Wrakrecht`
instance has two writers on `uitkering` with no explicit priority, the form
WF-079 refuses on one declared type.

**The executors disagree.** Measured 2026-09-25 with a `Wrakrecht` and a
`Strandrecht`, both with `grondslag` 10.00, in both orders of the two rules:
the reference interpreter faults (`EVALUATION FAULT: no fixpoint within 100
passes — divergence is a fault (ch. 5.7)`); the ASP backend derives two values
(`W1.uitkering = [2000;1000]`, and `[1000;2000]` with the rules swapped); the
Drools backend's exec-model branch finds no fixpoint (`geen fixpunt
(kringverwijzing?)`); its DRL-text branch answers by document order (20.00
with this order, 10.00 with the rules swapped). A `Strandrecht` instance alone
gives 10.00 in all four.

**The expressible form.** A defaults group on the target somewhere in the
chain. The chain then passes the gate of ch. 7.10: an ordinary rule becomes
the base of the level that declares it, the subtype's base replaces the
inherited one, and a `Wrakrecht` has one writer.
`corpus/golden/jutterswet-wrakbasis` (a group on the supertype beside a rule
on the subtype) and `corpus/golden/jutterswet-wrakerfbasis` (a rule on the
supertype beside a group on the subtype) carry that shape, and
`corpus/golden/anchor/jutterswet-strandrechtketen` carries a group on each
level. The declared-unordered marking is no remedy here, so the report does
not offer it: on either level a declared-unordered set meets the other
level's rule in a composition ch. 7.10 refuses. Measured 2026-09-25 on
`corpus/golden/jutterswet-wrakconflict`, whose two unordered groups sit on
`Strandrecht.uitkering`: with this document's rule on `Wrakrecht` added, `ref`
and the ASP backend refuse "one own group over an inherited declared-unordered
set"; with the two groups moved to `Wrakrecht` and the rule put on
`Strandrecht`, both refuse "a fold between a declared-unordered set of its own
and an inherited stack has no defined reading". The verifier passes both
documents.

**The document.** Emitted by the RegelSpraak frontend from this source:

    Objecttype het Strandrecht (mv: Strandrechten)
        de grondslag Numeriek (niet-negatief getal met 2 decimalen);
        de uitkering Numeriek (niet-negatief getal met 2 decimalen);

    Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)

    Regel uitkering van een strandrecht
        geldig altijd
            De uitkering van een Strandrecht moet gesteld worden op zijn grondslag.

    Regel uitkering van een wrakrecht
        geldig altijd
            De uitkering van een Wrakrecht moet gesteld worden op zijn grondslag maal 2.

**The arm.** WF-079's flat arm keys on the declared type, where
`Strandrecht.uitkering` and `Wrakrecht.uitkering` are two keys; before the
chain arm the verifier passed this document (`verifier: PASS`). The chain arm
pairs a writer on a type with one on a strict ancestor and reports the pair
when the lower type's chain declares no defaults group on the target — the
gate the fold of ch. 7.10 reads (`chain_group_keys` in
`spec/verifier/lib/checks.ml`). `expect.txt` pins the chain arm.

Falsifiable, arm-exact: making the chain arm report nothing (the last line of
`wf079` in `spec/verifier/lib/checks.ml` changed to `pairs writers @ (if false
then chain_pairs computing else [])`) turns this case clean and nothing else —
708 documents compared over golden, red and negative, one verdict moved
(measured 2026-09-25).
