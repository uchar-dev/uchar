Violates WF-079 (`spec/IR-SPEC.md` ch. 3.1; `RUNTIME-SEMANTICS.md` ch. 7.10):
two unconditional computing rules write one attribute through a type chain
that declares no defaults group. `uitkering van een strandrecht` sets the
`uitkering` of a `Strandrecht` to its `grondslag`; `uitkering van een
wrakrecht` sets the `uitkering` of a `Wrakrecht` to its `grondslag` times 2;
`Wrakrecht` is a `Strandrecht`. The supertype's rule comes first in the
document.

**Why both fire on a `Wrakrecht`.** The fold of ch. 7.10 — an ordinary rule
read as the base of its level, the subtype's base replacing the inherited
one — is gated: a rule is absorbed only on a target whose chain declares a
defaults group somewhere. This chain declares none, so neither rule enters a
stack. The supertype's rule quantifies over every `Strandrecht`, a `Wrakrecht`
included, and the subtype's rule over every `Wrakrecht`: a `Wrakrecht`
instance has two writers on `uitkering` with no explicit priority, the form
WF-079 refuses on one declared type.

**The executors part.** Measured 2026-09-25 with a `Wrakrecht` and a
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
inherited one, and a `Wrakrecht` has one writer. `corpus/golden/jutterswet-wrakbasis` (a group on
the supertype beside a rule on the subtype) and
`corpus/golden/jutterswet-wrakerfbasis` (a rule on the supertype beside a
group on the subtype) carry that shape, and
`corpus/golden/anchor/jutterswet-strandrechtketen` carries a group on each
level.

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

Verifier without a chain arm in WF-079: `verifier: PASS` — the rule keyed on
the declared type, and `Strandrecht.uitkering` and `Wrakrecht.uitkering` are
two keys.
