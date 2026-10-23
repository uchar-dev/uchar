# fold-filter-nested-aggregate — a listed fold in the subselection of five folds

A Schuit has an afmeerdag and ladingen, each in a maand and with a gewicht.
A lading is late when its maand is later than "de laatste van (de afmeerdag
van de Schuit min 1 mnd) en (de afmeerdag van de Schuit min 2 mnd)": a
listed fold (RUNTIME-SEMANTICS ch. 1.2e) in the subselection ("die aan de
volgende voorwaarde voldoen: …", IR-SPEC ch. 4.3 and 4.5, the `filter`
field) of a fold over the ladingen. Five rules fold over the late ladingen:

| rule | fold over the late ladingen |
|---|---|
| eerste maand na de afmeerdag | de eerste van de maand (the witness of the card clingo-nested-aggregate-in-fold-filter) |
| aantal late ladingen | het aantal |
| late gewicht | de som van het gewicht |
| zwaarste late gewicht | de maximale waarde van het gewicht |
| laat beladen | a quantifier fold: het gewicht van ten minste één late lading groter dan 100 |

The listed fold is an aggregate inside the condition of each element of
another aggregate. The ASP backend moves it into a helper predicate per
element, since clingo admits no aggregate inside an aggregate element
(until the card it stopped with a parse error on every rule here).

"de afmeerdag van de Schuit" in the subselection reads the rule subject: since
the card `subselection-reads-subject` (2026-10-23) each of its ten reads is
an own read of the Schuit with `anchor: "form"` (IR-SPEC ch. 4.1a, WF-122),
where the frontend wrote the way back from the lading before (`path
[lading in-een-maand.schuit]`). The cases did not change.

## Hand computation

The ladingen are the same in every case: L1 maand 01-03-2026 gewicht 50, L2
maand 01-04-2026 gewicht 150, L3 maand 01-05-2026 gewicht 80. The month
step clamps to the month's end (ch. 3.8), and min 1 mnd is always the later
of the two operands.

`case1-afmeerdag-half-april.json`: afmeerdag 15-04-2026. Min 1 mnd =
15-03-2026, min 2 mnd = 15-02-2026, de laatste = 15-03-2026. L1 (01-03) is
not later; L2 (01-04) and L3 (01-05) are. Eerste maand **01-04-2026**;
aantal **2**; late gewicht 150 + 80 = **230**; zwaarste **150**; L2 weighs
more than 100, so the Schuit **is laat beladen**.

`case2-geen-late-lading.json`: afmeerdag 15-06-2026. De laatste =
15-05-2026; no maand is later. Eerste maand **leeg** (an ordering fold over
no contributor, ch. 1.2e); aantal **0**; late gewicht **leeg** (a som
without fallback over no contributor); zwaarste **leeg**; **not** laat
beladen.

`case3-maandeinde.json`: afmeerdag 31-03-2026. Min 1 mnd = 28-02-2026 (the
clamp: February 2026 has 28 days), min 2 mnd = 31-01-2026, de laatste =
28-02-2026. All three maanden are later. Eerste maand **01-03-2026**;
aantal **3**; late gewicht 50 + 150 + 80 = **280**; zwaarste **150**;
**laat beladen**.
