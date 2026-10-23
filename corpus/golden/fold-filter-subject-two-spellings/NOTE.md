# fold-filter-subject-two-spellings — one reading of the rule subject in a filter, spelled twice

A Schuit has an afmeerdag, ladingen (each in a maand) and klachten (each with
an einddag); a lading has one schuit. Four rules fold over the ladingen of the
Schuit with a filter that reads the Schuit, two by two the same reading:

| rule | the filter reads | spelling on the wire |
|---|---|---|
| eerste klachtvrije maand | de laatste van de einddag van alle klachten **van de Schuit** | the inner fold with `anchor: "form"`, `sourceRole schuitklacht.klacht` from the Schuit |
| eerste klachtvrije maand via de lading | de laatste van de einddag van alle klachten **van de schuit van de Lading in-een-maand** | the inner fold from the lading, `sourcePath [lading in-een-maand.schuit, schuitklacht.klacht]` |
| eerste maand na het afmeren | de afmeerdag **van de Schuit** | an own read of the Schuit with `anchor: "form"` |
| eerste maand na het afmeren via de lading | de afmeerdag **van de schuit van de Lading in-een-maand** | a read from the lading, `path [lading in-een-maand.schuit]` |

The first sentence of each pair names the rule subject, and the frontend
builds it at the Schuit with the form anchor (IR-SPEC ch. 4.1a, WF-122;
`frontends/regelspraak/grammar/DIVERGENCES.md`, "The rule subject in a
subselection — zelf"); the second navigates from the element, over the
lading's one schuit, and keeps the navigation. Both are valid IR, and since
the way back from a lading is to-one and leads to the Schuit whose ladingen
are folded, they read the same object (RUNTIME-SEMANTICS ch. 3.4a, "two
spellings"). Every case pins both rules of a pair on one value, so the claim
is measured in ref, clingo and both Drools branches, not asserted. The
dependency graphs of the two spellings differ in their relation edges (the
navigation consumes the relation `lading in-een-maand`, the anchored read
does not), as IR-SPEC ch. 5.1 says; gate 15 counts those edges. Witness of
B2 of the card `subselection-reads-subject` (2026-10-23). Own
Jutterswet-flavoured casus, own values, rights-free.

## Hand computation

A month step clamps to the month's end (ch. 3.8); de laatste over no
contributor is leeg (ch. 1.2e), and an ordering comparison with a leeg
operand is false (ch. 1.2).

`case1-een-schuit.json`: S1, afmeerdag 15-04-2026, ladingen L1 (01-02-2026),
L2 (01-04-2026), L3 (01-05-2026), klachten K1 (einddag 10-03-2026) and K2
(20-04-2026). De laatste einddag is 20-04-2026: L1 plus 1 mnd is 01-03, not
later; L2 01-05 and L3 01-06 are. Eerste klachtvrije maand, both spellings,
**01-04-2026**. Only L3 (01-05) is later than the afmeerdag: eerste maand na
het afmeren, both spellings, **01-05-2026**.

`case2-twee-schuiten.json`: S1 as in case 1, and S2 with afmeerdag
01-01-2026, lading L4 (01-02-2026) and klacht K3 (einddag 15-01-2026). S1 as
in case 1. S2: L4 plus 1 mnd is 01-03, later than 15-01: **01-02-2026**; L4
is later than 01-01: **01-02-2026**. A fold over every klacht instead of the
Schuit's own would give S2 the einddag 20-04-2026 and leave it leeg.

`case3-geen-klachten.json`: S1 with afmeerdag 01-01-2026, ladingen L1
(01-02-2026) and L2 (01-03-2026), no klachten. De laatste einddag over no
klacht is leeg, so no lading passes: eerste klachtvrije maand, both
spellings, **leeg**. L1 is later than 01-01: eerste maand na het afmeren,
both spellings, **01-02-2026**.
