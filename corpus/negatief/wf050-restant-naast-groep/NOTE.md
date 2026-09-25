Violates WF-050 (`spec/IR-SPEC.md` ch. 6.1): an allocation writes its
remainder into the target of a defaults group on the same type. The defaults
group `onverdeelde rest van een berging` decides the `onverdeelde rest` of a
`Berging` (7, and 9 for a berging whose `opbrengst` is above 5000);
`verdeel de opbrengst van een berging` distributes the `opbrengst` of a
`Berging` over its bergers and writes what is left into the `onverdeelde
rest` of the `Berging` (`sourceType` `Berging`, `remainderAttribute`
`onverdeelde rest`).

    WF-050 FAIL /rules/prioritizedDefaults/0: an allocation also writes
    defaults target 'Berging.onverdeelde rest'

This is the remainder half of the lift's allocation clause;
`corpus/negatief/wf050-allocation-beside-defaults` is the share half. The
document shares its model with `corpus/negatief/wf050-keten-restant-naast-groep`
— `Wrakberging` is a `Berging` in both — and differs from it in one
declaration: here the group's subject is `Berging`, the allocation's own
source type, so the flat clause speaks and the chain clause does not.

**The executors disagree.** Measured 2026-09-25 with one `Berging` B1
(`opbrengst` 1001) linked to two plain `Berger`s, each of which receives 500:
the reference interpreter faults (`EVALUATION FAULT: no fixpoint within 100
passes — divergence is a fault (ch. 5.7)`); the ASP backend derives two values
(`B1.onverdeelde rest = [7;1]`); the Drools backend's exec-model branch gives
7 (`verwacht 1, werkelijk 7`); its DRL-text branch excludes the document, as
it excludes every defaults group.

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

    Regel onverdeelde rest van een berging
        geldig altijd
            De onverdeelde rest van een Berging moet gesteld worden op 7 €.

    Uitzondering op onverdeelde rest van een berging: onverdeelde rest van een grote berging
        geldig altijd
            De onverdeelde rest van een Berging moet gesteld worden op 9 €
            indien zijn opbrengst groter is dan 5000 €.

**The arm.** `expect.txt` pins the flat clause's allocation sentence. The
chain clause's sentence begins with the same words; on this document it
cannot fire, because the group's subject and the allocation's source type are
one type.

Falsifiable: with the remainder condition of the flat clause made false (in
`wf050`, the second disjunct of the allocation test changed to `false && match
(vd.bron_type, vd.restant_attribuut) with …`) the verifier passes this
document (`verifier: PASS`), and before this witness that mutant changed no
document of golden, rood, negatief or the parity zone at all (measured
2026-09-25).
