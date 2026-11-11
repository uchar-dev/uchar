# The meldplicht in three source languages

Goal: one rule, authored in RegelSpraak, Catala and F-logic as three goldens with byte-identical case files:
`jutterswet-meldplicht`, `jutterswet-meldplicht-catala` and `jutterswet-meldplicht-flogic`. The rule: a Strandvondst
is meldplichtig when its vondstwaarde is greater than 100. The website's languages section shows this rule in all
three source languages and links to these sources; the goldens keep that example running. The rule is chosen to be
the smallest one all three frontends carry the same way: a characteristic assigned under a comparison, no
arithmetic (a product with more decimals than its target needs an explicit rounding, which F-logic does not have),
no defaults and no inheritance (Catala has no subtypes).

Acceptance:
- each `ir.json` is what its frontend emits: gates 9, 10 and 14 regenerate it byte-identically;
- the case files are byte-identical across the three goldens and replay in ref, clingo and both Drools branches
  (gates 5, 6 and 12);
- the `Rule` in the three documents is identical apart from its name and the source snippets; each golden's
  `NOTE.md` accounts for every other difference between the documents;
- `make gates` green. Gate 12's `EXPECTED_PLAYED` rises by three: a protected script, landed on the maintainer's
  instruction for this card (2026-11-11).

May touch: `corpus/golden/jutterswet-meldplicht/`, `corpus/golden/jutterswet-meldplicht-catala/`,
`corpus/golden/jutterswet-meldplicht-flogic/`, their three DRL text pins under
`backends/drools/src/test/resources/drl-text/` (`DrlTextPinWitnessTest`: a new golden writes its pin), `scripts/check-drools.sh` (`EXPECTED_PLAYED` and its comment only),
`backlog/`.

Out of scope: frontends, backends, ref, spec. regelspraak-toolchain: no pin bump, the distribution does not read the
corpus.

Result: drie goldens met één case, `case1-drempel.json`, byte-gelijk in alle drie: `jutterswet-meldplicht`
(RegelSpraak), `jutterswet-meldplicht-catala` en `jutterswet-meldplicht-flogic`. Elk `ir.json` is de uitvoer van zijn
frontend (de Catala- en F-logic-documenten gecanoniseerd met de serialiser van gate 2). De drie documenten dragen
dezelfde `Rule` (doel het kenmerk `meldplichtig`, waarde `true`, voorwaarde één `Comparison` `vondstwaarde > 100`); de
NOTE van de RegelSpraak-golden legt de vier categorieën verschil uit (bron, namen die een taal niet kent, de regelnaam,
een lege `prioritizedDefaults`), die van de twee andere tellen hun regels. De drie gegenereerde DRL-teksten verschillen
alleen in het commentaar boven de toets en, bij Catala, de regelnaam.

Tijdens het werk is "May touch" verruimd met de drie DRL-pins onder `backends/drools/src/test/resources/drl-text/`:
`DrlTextPinWitnessTest` eist een pin voor elke nieuwe golden, wat de kaart niet voorzag. `EXPECTED_PLAYED` in
`scripts/check-drools.sh` gaat van 362 naar 365; de kaart `review-candidates-triage` verhoogt dezelfde regel naar 363
en moet bij haar rebase 366 schrijven (de eigenaar koos ervoor daar niet op te wachten).

Gezien en verworpen als voorbeeld: een bergloon van een kwart van een vondstwaarde met twee decimalen. RegelSpraak
weigert de regel zonder expliciete afronding, de Catala-frontend rondt `money` zelf af, en F-logic heeft geen afronding,
zodat de verifier het F-logic-document weigert (`WF-023.precision-exceeds`).

Controle: `make gates` op de werktak; de eerste run was rood op gate 12 alleen door de ontbrekende pins. In de tweede
run faalde in gate 12 `DrlPropertyWitnessTest` met "invalid header field" bij het compileren van fact-beans in het
geheugen, ook voor goldens buiten deze kaart; los gedraaid was gate 12 groen (365 gespeeld op beide takken, 1898
JUnit-tests).

Vóór het landen: `make gates` op de tak zoals hij landt, op deze alinea na (`out/gates/20261111-105654/`): de 17 gates
en after-run groen, `red=0`.
