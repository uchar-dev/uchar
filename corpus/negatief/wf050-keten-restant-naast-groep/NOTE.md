Violates WF-050 (`spec/IR-SPEC.md` ch. 6.1; `RUNTIME-SEMANTICS.md` ch. 7.10):
an allocation writes its remainder into the target of a defaults group
through a type chain. The defaults group `onverdeelde rest van een
wrakberging` decides the `onverdeelde rest` of a `Wrakberging` (7, and 9 for
a wrakberging whose `diepte` is above 10); `verdeel de opbrengst van een
berging` distributes the `opbrengst` of a `Berging` over its bergers and
writes what is left into the `onverdeelde rest` of the `Berging`
(`sourceType` `Berging`, `remainderAttribute` `onverdeelde rest`).
`Wrakberging` is a `Berging`.

**Why a `Wrakberging` has both writers.** The allocation runs for every
`Berging`, a `Wrakberging` included, and writes the remainder on it; the
group decides the `onverdeelde rest` of every `Wrakberging`. The fold of
ch. 7.10 takes ordinary rules only; an allocation never enters a stack. On a
`Wrakberging` the remainder and the group's decision both write one
attribute, the form the single-writer lift of WF-050 refuses when the
allocation's source is the group's own subject. This is the remainder half
of that lift; `corpus/negatief/wf050-keten-toewijzing-naast-groep` is the
share half.

**The executors disagree.** Measured 2026-09-25 with one `Wrakberging`
(`opbrengst` 1001, `diepte` 3) linked to two plain `Berger`s, each of which
receives 500: the reference interpreter faults (`EVALUATION FAULT: no
fixpoint within 100 passes — divergence is a fault (ch. 5.7)`); the ASP
backend derives two values (`B1.onverdeelde rest = [7;1]`). The Drools
backend declines the document before it runs: the exec-model branch on a
bean limit of its own (`name-sharing bean limit`), which a plain `Berging`
in the same case does not reach, and the DRL-text branch because it carries
no defaults lowering.

**The document.** Emitted by the RegelSpraak frontend from this source:

    Eenheidsysteem Munt
        de euro (mv: euros) EUR €

    Objecttype de Berging (mv: Bergingen)
        de opbrengst Numeriek (geheel getal) met eenheid €;
        de onverdeelde rest Numeriek (geheel getal) met eenheid €;

    Objecttype de Wrakberging is een Berging (mv: Wrakbergingen)
        de diepte Numeriek (niet-negatief geheel getal);

    Objecttype de Berger (mv: Bergers) (bezield)
        het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal) met eenheid €;

    Feittype bergingsdeelname
        de berging Berging
        de berger (mv: bergers) Berger
    Eén berging heeft meerdere bergers

    Regel verdeel de opbrengst van een berging
        geldig altijd
            De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld:
            - in gelijke delen,
            - afgerond op 0 decimalen naar beneden.
            Als onverdeelde rest blijft de onverdeelde rest van de berging over.

    Regel onverdeelde rest van een wrakberging
        geldig altijd
            De onverdeelde rest van een Wrakberging moet gesteld worden op 7 €.

    Uitzondering op onverdeelde rest van een wrakberging: onverdeelde rest van een diepe wrakberging
        geldig altijd
            De onverdeelde rest van een Wrakberging moet gesteld worden op 9 €
            indien zijn diepte groter is dan 10.

**The arm.** WF-050's flat lift keys on the group's declared subject, where
`Wrakberging.onverdeelde rest` and `Berging.onverdeelde rest` are two keys;
before the chain arm the verifier passed this document (`verifier: PASS`).
