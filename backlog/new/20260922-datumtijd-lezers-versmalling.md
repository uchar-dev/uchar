# datumtijd-lezers-versmalling: de snippet-tolerantie achter het hek

Nalatenschap van kaart `20260922-p5-datumtijd-lexeem` (WF-095 versmald
tot canoniek ISO in `waarde`; de emitter schrijft ISO, de snippet
blijft in `bron`). De LEZERS tolereren de dag-eerst-snippet nog: `ref`
(en via zijn casemodule clingo) en de Drools-lezer
`Literalen.parseDatumTijd`. IR-SPEC benoemt die tolerantie expliciet
als gedrag achter het hek (ch. 4.1, de lezerszin) — de vorm bereikt de
schijf niet meer, dus de tolerantie is dood spoor maar geen fout.

Normvraag bij oppakken (afweging in de kaart beslissen, langs de
plan-lijn): versmallen de lezers mee tot canoniek — dan worden de
tolerantie-getuigen weigeringsgetuigen en verdwijnt de
maand-eerst/dag-eerst-vraag ook uit de lezers — of blijft de
tolerantie als gedocumenteerde leesbreedte? Aanbeveling: versmallen;
een lezer die vormen aanvaardt die geen producent meer schrijft en
geen verifier meer toelaat is ongetuigd gedrag.

Los van die keuze, en hoe dan ook te doen:

- `ref/lib/value.ml:110-117` zegt "which a v0 emitter still writes
  into `waarde`" — sinds de P5-kaart onwaar voor deze repo; commentaar
  actualiseren.
- `spec/ASP-TRANSLATION.md` en IR-SPEC houden de lezerszin consistent
  met de gekozen uitkomst.

## Acceptatie (bij oppakken aanscherpen)

- (a) De normkeuze vastgelegd in de kaart met motivering; bij
  versmallen: alle drie de lezers weigeren de snippet benoemd,
  rood-eerst per lezer (de bestaande tolerantie-getuigen — o.a.
  `ref/test/test_ref.ml` ~4082, `DatumTijdLexeemWitnessTest`,
  `CaseWireDatumTijdWitnessTest`, `LiteralenTest` — kantelen
  gemotiveerd mee).
- (b) Het value.ml-commentaar en de spec-lezerszinnen kloppen met de
  werkelijkheid.
- (c) Alle veertien gates groen.

May touch: `ref/**`, `backends/**`, `frontends/regelspraak/**`,
`spec/IR-SPEC.md` (**voorstel**), `spec/ASP-TRANSLATION.md`
(**voorstel**), kaartverplaatsing. NB: serieel met andere
Drools-kaarten.
