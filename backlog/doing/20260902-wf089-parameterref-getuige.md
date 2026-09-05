# WF-089's eerlijkheidsnotitie is achterhaald: de `ParameterRef`-arm heeft een getuige

Goal: De WF-089-indexrij in `spec/IR-SPEC.md` (ch. 9, de regeltabel)
draagt een eerlijkheidsnotitie met TWEE meetclaims die de
meldplichtspoor-golden nu falsificeert. Letterlijk: "The `ParameterRef`
and `DimensieCel` arms have **no positive witness**: no golden, rood or
parity document carries a keuzetype-typed `ParameterReference` or
`DimensionCellRead`, so OVER-rejection in those two arms is unpinned
(measured mutants that make either arm fire unconditionally leave gate 3
green); a keuzetype-typed parameter cannot be added to a golden today
because the ASP carrier refuses the form ahead of translation."

Gemeten in fixronde 1 van `20260902-meldplichtspoor-niveau0` (in een
`git archive`-kopie, de werkkopie ongewijzigd):

- `corpus/golden/jutterswet-meldplichtspoor` draagt een lijstparameter
  `meldreeks` van `ListType`-van-`SumType`, en `keuzetype_domain` in
  `spec/verifier/lib/checks.ml` kijkt door een `List_type` heen naar het
  element. De `ParameterRef` van de foldbron valt dus BINNEN de arm.
- De arm onvoorwaardelijk laten vuren en alle 169 golden + rood
  ir.json-documenten door de verifier halen maakt **precies één** document
  rood: die golden. De arm heeft dus een positieve getuige, en
  over-afwijzing erin is niet langer ongepind.
- De ASP-weigeringsclaim is eveneens onwaar: clingo speelt alle acht
  casussen van die golden exact (gate 6 corpusbreed groen).

De `DimensieCel`-arm blijft wél zonder getuige — het casusformaat draagt
geen celinvoer — dus de notitie moet gesplitst, niet geschrapt.

`spec/IR-SPEC.md` is protected en viel buiten de scope van de
meldplichtspoor-kaart; deze kaart is de voorgestelde tekstwijziging plus
de hermeting die haar draagt.

Acceptance:
- De WF-089-indexrij herschreven als PROPOSAL-diff: de
  `ParameterRef`-helft van de eerlijkheidsnotitie vervangen door de
  getuige (`jutterswet-meldplichtspoor`, de lijstelement-doorkijk in
  `keuzetype_domain`), de `DimensieCel`-helft ongewijzigd als nog-open
  meetgat, en de weerlegde ASP-weigeringszin geschrapt.
- De getuigenkolom van de rij noemt de golden expliciet, in de vorm die
  de andere rijen gebruiken.
- De mutantmeting hermeten en in de bouwnotitie opgetekend met echte
  uitvoer: onvoorwaardelijke arm → exact één rood document, arm hersteld
  → gate 3 corpusbreed groen (169 golden clean, 206 negatief rejected).
- Controleer bij het schrijven of een tweede WF-rij dezelfde
  "geen-getuige"-claim voert over een vorm die deze golden inmiddels wél
  draagt (WF-065's lijstelement-`SumType`, WF-070's somtype-accumulator);
  zo ja, in dezelfde diff meenemen.
- Alle twaalf gates groen.

May touch: `spec/IR-SPEC.md` (proposal — maintainer merget),
`backlog/`.

Out of scope: elke verifier-, schema- of corpuswijziging (de meting
vraagt er geen: de arm is al correct, alleen haar notitie is verouderd);
de `DimensieCel`-getuige (die vraagt celinvoer in het casusformaat — een
eigen kaart); `scripts/**`.
