Violates WF-026 (`spec/IR-SPEC.md` ch. 3.2; `RUNTIME-SEMANTICS.md` ch. 7.10):
two allocations write one attribute through a type chain. `verdeel de
opbrengst van een berging` distributes the `opbrengst` of a `Berging` over
the `bergingsaandeel` of all its bergers (`targetType` `Berger`); `verdeel de
premie van een berging` distributes its `premie` over the `bergingsaandeel`
of all its hoofdbergers (`targetType` `Hoofdberger`). `Hoofdberger` is a
`Berger`, and `bergingsaandeel` is declared on `Berger`. The allocation over
the supertype comes first in the document.

    WF-026 FAIL /rules/allocations/1/versions/0: two allocations write target
    (Hoofdberger, bergingsaandeel) through its type chain — one ranging over
    Berger, one over Hoofdberger — and an allocation is never folded into a
    stack, so an instance of Hoofdberger receives both shares; at most one
    allocation writes a target (ch. 3.2), and a second share needs an
    attribute Hoofdberger declares itself

The report cites the version on the lower type of the pair, `Hoofdberger`,
the first concrete type both allocations write. With the two allocations
swapped in the document the same sentence cites
`/rules/allocations/0/versions/0`.

**Why a `Hoofdberger` receives both shares.** `targetType` is the population
an allocation ranges over, read extensionally (ch. 2.2): the first
allocation writes the `bergingsaandeel` of every `Berger` linked to the
berging, a `Hoofdberger` included, and the second writes that of every
`Hoofdberger`. The fold of ch. 7.10 takes ordinary rules only. An allocation
never enters a stack, whatever defaults groups the chain declares, so
nothing ranks the two writes: a `Hoofdberger` has two writers on one
attribute, the form WF-026 refuses on one declared type. Unlike the chain
pair of WF-079 this one has no gate.

**The executors disagree.** Measured 2026-09-25 with one `Berging`
(`opbrengst` 1000, `premie` 100) linked to a `Hoofdberger` H1 and a plain
`Berger` G1, in both orders of the two allocations: the reference
interpreter faults (`EVALUATION FAULT: no fixpoint within 100 passes —
divergence is a fault (ch. 5.7)`); the ASP backend derives two values
(`H1.bergingsaandeel = [100;500]`, and `[500;100]` with the allocations
swapped); the Drools backend's exec-model branch finds no fixpoint (`geen
fixpunt (kringverwijzing?)`); its DRL-text branch answers by document order
(100 with this order, 500 with the allocations swapped). The exec-model
transpiler skips two allocations on one target by principle (`>1 verdeling
schrijft hetzelfde doel-attribuut`), and that skip keys on the declared type
as well: it fires when both allocations range over `Hoofdberger` and not on
this pair.

**The expressible form.** One allocation per target (ch. 3.2). Where the
hoofdbergers are to receive the premie as a share of their own, the second
allocation writes an attribute `Hoofdberger` declares itself. With
`premieaandeel` declared on `Hoofdberger` and written by the premie
allocation, the document is verifier-clean and the four executors agree (H1
`bergingsaandeel` 500 and `premieaandeel` 100, G1 `bergingsaandeel` 500;
measured 2026-09-25). A defaults group is no remedy: it folds rules, not
allocations, and an allocation beside a group is WF-050's.

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

    Regel verdeel de premie van een berging
        geldig altijd
            De premie van een berging wordt verdeeld over de bergingsaandelen van alle hoofdbergers van de berging, waarbij wordt verdeeld:
            - in gelijke delen,
            - afgerond op 0 decimalen naar beneden.
            Als onverdeelde rest blijft de onverdeelde premierest van de berging over.

**The arm.** WF-026's flat arm keys on the declared type, where
`Berger.bergingsaandeel` and `Hoofdberger.bergingsaandeel` are two keys;
before the chain arm the verifier passed this document (`verifier: PASS`).
The chain arm pairs two allocations of different forms on one attribute
whose target types lie on one chain, the one a strict ancestor of the other,
through the pairing WF-079's chain arm reads as well (`chain_pairs` in
`spec/verifier/lib/checks.ml`), and reports the pair at the version on the
lower type. It has no gate: the defaults groups a chain declares fold rules,
never an allocation. `expect.txt` pins the chain arm.

Falsifiable, arm-exact: making the chain arm report nothing (the last line of
`wf026` in `spec/verifier/lib/checks.ml` changed to `dedup_reports (pairs
named @ (if false then chain else []))`) turns this case clean and nothing
else — 729 documents compared over golden, red and negative, one verdict
moved (measured 2026-09-25).
