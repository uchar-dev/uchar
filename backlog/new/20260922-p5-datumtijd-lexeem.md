# p5-datumtijd-lexeem: WF-095 versmalt tot canoniek ISO

V1-preconditie P5 (spec/proposals/V1-WIREMIGRATIE.md §7, B22a) — de
goedkoopste stap op de lijst. WF-095 (2026-09-06) laat vandaag twee
spellingen toe voor het `DatumTijd`-lexeem: canoniek
`yyyy-mm-ddThh:mm:ss[.fff]` én de dag-eerst-bronsnippet, omdat de
RegelSpraak-emitter (`ExpressieBouwer`, de dedato-tak met
tijdcomponent) de snippet nog in `waarde` schrijft — één regel onder de
kale-datumtak die wél ISO schrijft. Gemeten (B22a): de snippetvorm
heeft VIJF corpusdragers, alle vijf in `corpus/negatief/wf095-*` — die
zijn er om afgewezen te worden; de enige golden met een
`DatumTijd`-literal (`jutterswet-meldstip`) draagt de canonieke vorm.
Nul goldenregeneratie.

## Acceptatie

- (a) Spec-first: de WF-095-tekst in IR-SPEC versmalt tot canoniek ISO
  (voorstel); de verifier-arm die de snippet toelaat vervalt; de vijf
  `wf095-*`-negatief-getuigen worden herschreven zodat elk één
  spellingsfout tegen de canonieke vorm pint (rood-eerst: elke getuige
  eerst zien falen tegen de oude, tolerante arm).
- (b) De emitterregel: de dedato-tak met tijdcomponent schrijft de
  canonieke ISO-vorm in `waarde` (de bronsnippet blijft in `bron`),
  symmetrisch aan de kale-datumtak. Rood-eerst een emittergetuige in de
  JUnit-suite — de emitterkant heeft vandaag nul goldengetuigen; weeg
  een kleine rgs-golden met `DatumTijd`-literal als blijvende getuige.
- (c) Mutatieproef op de verifier-arm (snippet weer toelaten ⇒ minstens
  één negatief-getuige valt stil ⇒ gate 3 rood), uitgevoerd en
  teruggedraaid met build-rc in één keten.
- (d) Alle veertien gates groen; geen versieklap, v0-schema ongewijzigd.

May touch: `spec/IR-SPEC.md` (**voorstel**), `spec/verifier/`
(**voorstel**), `corpus/negatief/wf095-*`, `frontends/regelspraak/**`,
`corpus/golden/**` (nieuwe getuige), kaartverplaatsing. NB: raakt de
frontend-emitter — serieel met andere emitterkaarten.
