# wf022-waardeclausule-getuige: de tweede clausule van WF-022 heeft geen eigen negatief

*Status: concept, 23 september 2026 — wacht op de besluiten 1–8 van
spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md (inhoudelijk alleen op
het oordeel over §7.3). De "eventueel verifier"-kaart van dat voorstel:
alleen een getuige, geen verifiercode.*

Aanleiding. WF-022 heeft twee clausules: het doel lost op, en de waarde van
een kenmerkdoel is de constante `waar` — de draadvorm van RegelSpraak
§9.2. De verifier toetst beide (`spec/verifier/lib/checks.ml:1400-1416`),
maar de WF-index noemt één negatief, `wf022-target-unresolvable`, dat
alleen de eerste raakt (`spec/IR-SPEC.md:7806`). Gemeten 2026-09-23
(voorstel M10): een vlakke `KENMERKTOEKENNING` met waarde `onwaar` laat de
uitvoerders vier kanten op gaan — ref laat het kenmerk afwezig, clingo
stopt met `TRANSLATION FAULT: characteristic attribution with a non-waar
value`, de Drools-exectak zet het aanwezig, de DRL-teksttak laat het
afwezig. De verifier weigert zo'n document wel; alleen staat die weigering
nergens vast.

## Werk

- `corpus/negatief/wf022-kenmerk-onwaar/ir.json` naar de tekst van
  voorstel §7.3 (één objecttype `Recht`, één vlakke regel op `herleefd`
  met waarde `onwaar`);
- de WF-022-rij van de WF-index noemt het nieuwe negatief (beschermd
  pad: voorstel).

## Acceptatie (bij oppakken aanscherpen)

- (a) Gate 3 weigert het document met `WF-022 FAIL
  /resolvedRegels/0/waarde: characteristic target's value must be the
  boolean constant true`.
- (b) Alle poorten groen; de negatief-telling van gate 3 stijgt met één.

May touch: `corpus/negatief/wf022-kenmerk-onwaar/`, de WF-022-rij in
`spec/IR-SPEC.md` (als voorstel), kaartverplaatsing.

## Acceptatie (aangescherpt bij de claim, 2026-09-23)

Opgepakt in branch `card/rood-duo` (fixronde 3 van de hoofdsessie),
na de rebase op main `3a140fe4`.

- (a) `corpus/negatief/wf022-kenmerk-onwaar/ir.json` naar de tekst van
  voorstel §7.3: objecttype `Recht` met de kenmerken `herleefd` en
  `nieuw recht ontstaan`, en de vlakke regel `geen herleving` met
  waarde `onwaar`. De verifier weigert het document alleen op WF-022,
  met `WF-022 FAIL /resolvedRegels/0/waarde: characteristic target's
  value must be the boolean constant true`. Een NOTE met de zelf
  gemeten uitkomst van de vier uitvoerders (ref, clingo, exec-tak,
  DRL-tekst-tak), via een case die alleen onder /tmp bestaat.
- (b) Rood-eerst: een mutant die de waardeclausule van `wf022` in
  `spec/verifier/lib/checks.ml` uitschakelt, maakt gate 3 rood op
  precies dit negatief. Daarna herstel, met `cmp` tegen HEAD.
- (c) De WF-022-rij van de WF-index in `spec/IR-SPEC.md` noemt het
  nieuwe negatief (**voorstel**, alleen die ene rij).
- (d) Gates 1, 2, 3, 4, 8 en 12 groen, elk los. Gate 3 telt 441
  negatief. Registerzin voor de WF-022-rij van CONSTRUCTS in het
  Result.
