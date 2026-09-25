Violates WF-050 (`spec/IR-SPEC.md` ch. 6.1; `RUNTIME-SEMANTICS.md` ch. 7.10):
an object creation initialises the target of a defaults group through a type
chain. The defaults group `uitkering van een strandrecht` decides the
`uitkering` of a `Strandrecht` (5.00, and 9.00 for a strandrecht whose
`grondslag` is above 100); `maak een wrakrecht bij elke melding` creates a
`Wrakrecht` for every `Melding` and initialises its `uitkering` to the
melding's `omvang` (`createdType` `Wrakrecht`). `Wrakrecht` is a
`Strandrecht`. `lees de gemelde uitkering` reads the created instance's
`uitkering` back into the melding, so a case can observe it.

**Why the created `Wrakrecht` has both writers.** A creation mints an
instance of exactly its `createdType`; it is not polymorphic. That instance is
a `Wrakrecht`, and a `Wrakrecht` inherits the whole defaults stack of
`Strandrecht` on `uitkering` (ch. 7.10), so the group decides its `uitkering`.
The fold of ch. 7.10 takes ordinary rules only; a creation-time write never
enters a stack. On the created instance the creation's value and the group's
decision both write one attribute, the form the single-writer lift of WF-050
refuses when the creation mints the group's own subject.

**The other direction is clean.** With the group on `Wrakrecht` and the
creation minting a `Strandrecht`, the created instance is a plain
`Strandrecht`, which the group never runs on. Measured 2026-09-25 with a
`Melding` M1 (`omvang` 50.00) and a `Wrakrecht` input W1 (`grondslag` 10.00):
the reference interpreter, the ASP backend and the exec-model branch agree
(M1 `gemelde uitkering` 50.00 from the creation, W1 `uitkering` 5.00 from the
group), and the verifier passes that document.

**The executors disagree.** Measured 2026-09-25 with one `Melding` M1
(`omvang` 50.00): the reference interpreter lets the group's decision stand
(`MISMATCH: M1.gemelde uitkering = 5, expected 50`); the ASP backend derives
two values (`M1.gemelde uitkering = [500;5000]`, that is 5.00 and 50.00); the
Drools backend's exec-model branch gives 5; its DRL-text branch excludes the
document, as it excludes every defaults group (`the DRL-text branch has no
defaults lowering`). The flat form — the same creation minting a `Strandrecht`
— is refused by WF-050 already and gives the same three answers.

**The expressible form.** The creation initialises what the group reads, and
the group writes the target. With the creation setting the `grondslag` of the
new `Wrakrecht` to the melding's `omvang`, the document is verifier-clean and
the reference interpreter, the ASP backend and the exec-model branch agree
(`gemelde uitkering` 5.00 for `omvang` 50.00, and 9.00 for `omvang` 150.00;
measured 2026-09-25).

**The document.** Emitted by the RegelSpraak frontend from this source:

    Objecttype het Strandrecht (mv: Strandrechten)
        de grondslag Numeriek (niet-negatief getal met 2 decimalen);
        de uitkering Numeriek (niet-negatief getal met 2 decimalen);

    Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)

    Objecttype de Melding (mv: Meldingen)
        de omvang Numeriek (niet-negatief getal met 2 decimalen);
        de gemelde uitkering Numeriek (niet-negatief getal met 2 decimalen);

    Feittype wrakmelding
        de melding Melding
        het gemeld wrakrecht Wrakrecht
    Eén melding meldt één gemeld wrakrecht

    Regel maak een wrakrecht bij elke melding
        geldig altijd
            Een Melding heeft een gemeld wrakrecht met uitkering gelijk aan zijn omvang.

    Regel uitkering van een strandrecht
        geldig altijd
            De uitkering van een Strandrecht moet gesteld worden op 5,00.

    Uitzondering op uitkering van een strandrecht: uitkering van een groot strandrecht
        geldig altijd
            De uitkering van een Strandrecht moet gesteld worden op 9,00
            indien zijn grondslag groter is dan 100.

    Regel lees de gemelde uitkering
        geldig altijd
            De gemelde uitkering van een Melding moet gesteld worden op de uitkering van zijn gemeld wrakrecht.

**The arm.** WF-050's flat creation clause keys on the group's declared
subject, where `Strandrecht.uitkering` and `Wrakrecht.uitkering` are two keys;
before the chain arm the verifier passed this document (`verifier: PASS`).
