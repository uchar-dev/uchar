Violates WF-050 (`spec/IR-SPEC.md` ch. 6.1; `RUNTIME-SEMANTICS.md` ch. 7.10):
an allocation writes the target of a defaults group through a type chain. The
defaults group `bergingsaandeel van een hoofdberger` decides the
`bergingsaandeel` of a `Hoofdberger` (7, and 9 for a hoofdberger whose `rang`
is above 1); `verdeel de opbrengst van een berging` distributes the
`opbrengst` of a `Berging` over the `bergingsaandeel` of all its bergers
(`targetType` `Berger`). `Hoofdberger` is a `Berger`.

    WF-050 FAIL /rules/prioritizedDefaults/0: an allocation also writes
    defaults target 'Hoofdberger.bergingsaandeel' through its type chain — the
    group declared on Hoofdberger, the allocation's shares on Berger — and an
    allocation is never folded into a stack, so an instance of Hoofdberger has
    both writers

The report sits at the defaults group, as the flat lift's does. The mirror
placement — the group on `Berger`, the allocation over the hoofdbergers only
— is refused by the same arm, naming the group on `Berger` and the
allocation's shares on `Hoofdberger`; there the reference interpreter and
the exec-model branch find no fixpoint either and the ASP backend derives
`H1.bergingsaandeel = [7;1000]` (measured 2026-09-25).

**Why a `Hoofdberger` has both writers.** `targetType` is the population an
allocation ranges over, read extensionally (ch. 2.2), so the allocation
writes the `bergingsaandeel` of every `Berger` linked to the berging, a
`Hoofdberger` included, while the group decides that of every `Hoofdberger`.
The fold of ch. 7.10 takes ordinary rules only; an allocation never enters a
stack. On a `Hoofdberger` the allocation's share and the group's decision
both write one attribute, the form the single-writer lift of WF-050 refuses
when the allocation ranges over the group's own subject.

**The executors disagree.** Measured 2026-09-25 with one `Berging`
(`opbrengst` 1000) linked to a `Hoofdberger` H1 (`rang` 1) and a plain
`Berger` G1: the reference interpreter faults (`EVALUATION FAULT: no fixpoint
within 100 passes — divergence is a fault (ch. 5.7)`); the ASP backend
derives two values (`H1.bergingsaandeel = [7;500]`); the Drools backend's
exec-model branch finds no fixpoint (`geen fixpunt (kringverwijzing?)`); its
DRL-text branch excludes the document, as it excludes every defaults group
(`the DRL-text branch has no defaults lowering`).

**The expressible form.** The group and the allocation write different
attributes. With the group moved to `toeslag`, an attribute `Hoofdberger`
declares itself, the document is verifier-clean and the reference
interpreter, the ASP backend and the exec-model branch agree (H1
`bergingsaandeel` 500 and `toeslag` 7, G1 `bergingsaandeel` 500; measured
2026-09-25).

**The document.** Emitted by the RegelSpraak frontend from this source:

    Eenheidsysteem Munt
        de euro (mv: euros) EUR €

    Objecttype de Berging (mv: Bergingen)
        de opbrengst Numeriek (geheel getal) met eenheid €;
        de premie Numeriek (geheel getal) met eenheid €;
        de onverdeelde rest Numeriek (geheel getal) met eenheid €;
        de onverdeelde premierest Numeriek (geheel getal) met eenheid €;

    Objecttype de Berger (mv: Bergers) (bezield)
        het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal) met eenheid €;

    Objecttype de Hoofdberger is een Berger (mv: Hoofdbergers) (bezield)
        de rang Numeriek (niet-negatief geheel getal);

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

    Regel bergingsaandeel van een hoofdberger
        geldig altijd
            Het bergingsaandeel van een Hoofdberger moet gesteld worden op 7 €.

    Uitzondering op bergingsaandeel van een hoofdberger: bergingsaandeel van een hoge hoofdberger
        geldig altijd
            Het bergingsaandeel van een Hoofdberger moet gesteld worden op 9 €
            indien zijn rang groter is dan 1.

**The arm.** WF-050's flat lift keys on the group's declared subject, where
`Hoofdberger.bergingsaandeel` and `Berger.bergingsaandeel` are two keys;
before the chain arm the verifier passed this document (`verifier: PASS`).
The chain arm reads the two writes an allocation makes — its shares on
`targetType`, its remainder on `sourceType` — against the group's subject,
and reports when the two types lie on one chain, the one a strict ancestor
of the other (`chain_lower` in `spec/verifier/lib/checks.ml`, the predicate
WF-079's and WF-026's chain arms read as well). It has no gate: an
allocation is never folded into the group's stack. `expect.txt` pins the
chain arm on the shares half; `corpus/negatief/wf050-keten-restant-naast-groep`
pins the remainder half.

Falsifiable, arm-exact: dropping the shares write from the chain arm's list
(in `wf050`, `[ ("shares", vd.doel_type, vd.doel_attribuut); ("remainder",
vd.bron_type, vd.restant_attribuut) ]` reduced to its remainder entry) turns
this case clean and nothing else — 729 documents compared over golden, rood
and negatief, one verdict moved (measured 2026-09-25).
