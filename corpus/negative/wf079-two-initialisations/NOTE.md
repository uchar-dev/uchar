Violates WF-079 (`spec/IR-SPEC.md` ch. 3.1, the initialisation arm;
`RUNTIME-SEMANTICS.md` ch. 5.4): two initialisations, each a form with one
always-valid version, write one attribute target without prioritized
defaults — `Vondst.bergloon`. `standaardbergloon` writes `25,00` where the
`gewicht` is above 10, `lengtebergloon` writes `30,00` where the `lengte` is
above 3. Where both conditions hold, nothing ranks the two.

    WF-079 FAIL /rules/rules/0/versions/0: two initialisations write target
    (Vondst, bergloon) with no prioritized defaults — where both apply
    nothing ranks them, and a condition on either gives it no priority: the
    fixpoint does not define which value the target takes (ch. 5.4)

**Why conditions do not exempt the pair.** Two conditional computing rules
may be a guarded case split, and WF-079 leaves them alone. Two
initialisations have no such reading: the initialisation's guard turns an
overlap of the two conditions into an order, since in v0 the first to fire
fills the target and the other then finds it filled.

**The executors disagree.** Measured 2026-09-25 with one `Vondst` of
`gewicht` 12,00 and `lengte` 5,00, in both orders of the two rules. Before
the initialisation's guard read the input, the reference interpreter and
both Drools branches answered by document order: 25 with this order, 30 with
the rules swapped (`MISMATCH: V1.bergloon = 30, expected 25`). Since then the
reference interpreter finds no fixpoint (`EVALUATION FAULT: no fixpoint
within 100 passes — divergence is a fault (ch. 5.7)`), and so does the Drools
exec-model branch (`geen fixpunt (kringverwijzing?)`), while the DRL-text
branch still answers 25 or 30 by order. The ASP backend refuses the shape:
`TRANSLATION FAULT: 2 initialisations write "bergloon": the first to fire
writes and the others find the target filled, an order among writers that no
guard on the fact base reproduces (no corpus case)`. The verifier passed the
document before this arm (`verifier: PASS`).

**The expressible form.** One initialisation per target, or a defaults
group that orders the two defaults explicitly.

**The document.** Emitted by the RegelSpraak frontend from this source:

    Objecttype de Vondst (mv: Vondsten)
        het gewicht Numeriek (getal met 2 decimalen);
        de lengte Numeriek (getal met 2 decimalen);
        het bergloon Numeriek (getal met 2 decimalen);

    Regel standaardbergloon
        geldig altijd
            Het bergloon van een Vondst moet geïnitialiseerd worden op 25,00
            indien zijn gewicht groter is dan 10.

    Regel lengtebergloon
        geldig altijd
            Het bergloon van een Vondst moet geïnitialiseerd worden op 30,00
            indien zijn lengte groter is dan 3.

`expect.txt` pins the flat initialisation arm.
