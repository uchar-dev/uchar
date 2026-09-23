# regelgroep-uitzondering-afsluiting: een Regelgroep sluit stil af bij de eerste Uitzondering op

*Status: concept, 23 september 2026 — bijvangst van de onderzoekskaart
dialect-defaults-niet-en-nesting (voorstel §9.1, meting M5). Onafhankelijk
van de besluiten 1–8; raakt dezelfde RuleChecker als
dialect-niet-nesting-frontend, dus serieel daarmee.*

Gemeten 2026-09-23 op de frontend van main 8dd05e25. Een `Regelgroep
berging` gevolgd door een basisregel, een `Uitzondering op` die basis en
een derde regel: de frontend emitteert `leden: ["bergingsbevoegd bij
quotum"]` met `diagnostics: []`. De uitzondering en de regel erna vallen
buiten de groep (`groepLid : regel | beslistabel`, `RegelSpraak.g4:77-78`),
en omdat de basis in een defaults-groep wordt geheven, noemt de groep een
lid dat geen regel meer is. De verifier weigert het document:
`WF-033 FAIL /regelGroepen/0: group member 'bergingsbevoegd bij quotum'
does not resolve to a rule form`. Frontend en verifier verschillen dus van
oordeel over een document dat de frontend zonder melding schrijft.

## Opties

- (a) Een uitbreidingsblok binnen een `Regelgroep` luid weigeren, en een
  groepslid dat in een defaults-groep wordt geheven ook — de frontend kent
  beide. De voorgestelde richting.
- (b) Uitbreidingsblokken als groepsleden toelaten: WF-033 moet dan een
  defaults-groep als lid aanvaarden, en er moet een uitspraak komen over
  wat een defaults-groep in een recursieve groep betekent (een
  spec-besluit, beschermd pad).

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood eerst met de proef hierboven; daarna een ERROR die het blok en
  de groep noemt, en geen bestand op schijf.
- (b) Alle poorten groen; de goldens van gate 10 byte-identiek.

May touch: `frontends/regelspraak/grammar/**`,
`frontends/regelspraak/src/main/java/**/resolver/**`, nieuwe testklassen,
kaartverplaatsing (bij optie (b) ook `spec/IR-SPEC.md` WF-033 als
voorstel).
