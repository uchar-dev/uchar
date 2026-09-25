Violates WF-079 (`spec/IR-SPEC.md` ch. 3.1, the initialisation arm through a
type chain; `RUNTIME-SEMANTICS.md` ch. 5.4 and ch. 7.10): an initialisation
on `Vondst` (`25,00` where the `gewicht` is above 10) and one on
`Wrakvondst` (`30,00` where the `gewicht` is above 20), with
`Wrakvondst` a `Vondst` and no defaults group anywhere on the chain.

    WF-079 FAIL /rules/rules/1/versions/0: two initialisations write target
    (Wrakvondst, bergloon) through its type chain — one declared on Vondst,
    one on Wrakvondst — and no defaults group folds them into one stack, so
    both write a Wrakvondst instance with nothing to rank them (ch. 5.4,
    ch. 7.10)

The report cites the version on the lower type, `Wrakvondst`, the first
concrete type both write. With the two rules swapped in the document the
same sentence cites `/rules/rules/0/versions/0`.

**Why both write a `Wrakvondst`.** The fold of ch. 7.10 takes an ordinary
rule into a stack only where the chain declares a defaults group on the
target. This chain declares none, so the initialisation on `Vondst` fires on
every `Vondst`, a `Wrakvondst` included, beside the one on `Wrakvondst`.

**The executors disagree.** Measured 2026-09-25 with a `Vondst` and a
`Wrakvondst`, both of `gewicht` 30,00, in both orders of the two rules.
Before the initialisation's guard read the input, the reference interpreter
and both Drools branches answered by document order: the `Wrakvondst` got 25
with this order (`MISMATCH: W1.bergloon = 25, expected 30`) and 30 with the
rules swapped. Since then the reference interpreter and the Drools
exec-model branch find no fixpoint, while the DRL-text branch still answers
by order. The ASP backend refuses the shape (`2 initialisations write
"bergloon"`). The verifier passed the document before this arm.

**Inside the gate.** With a defaults group on a type above both levels, the
fold takes the two initialisations into one stack on every instance they
share, the inherited one above the own one, and the arm does not report the
pair; the ASP backend's own suite pins that shape (two initialisations on a
three-level chain whose top declares a group).

**The document.** Emitted by the RegelSpraak frontend from this source:

    Objecttype de Vondst (mv: Vondsten)
        het gewicht Numeriek (getal met 2 decimalen);
        het bergloon Numeriek (getal met 2 decimalen);

    Objecttype de Wrakvondst is een Vondst (mv: Wrakvondsten)

    Regel standaardbergloon
        geldig altijd
            Het bergloon van een Vondst moet geïnitialiseerd worden op 25,00
            indien zijn gewicht groter is dan 10.

    Regel wrakbergloon
        geldig altijd
            Het bergloon van een Wrakvondst moet geïnitialiseerd worden op 30,00
            indien zijn gewicht groter is dan 20.

`expect.txt` pins the chain initialisation arm.
