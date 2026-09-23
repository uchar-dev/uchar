# bron-annotaties-defaults: // Bron:-annotaties verdwijnen als een regel in een defaults-groep wordt opgenomen

*Status: concept, 23 september 2026 — bijvangst van de onderzoekskaart
dialect-defaults-niet-en-nesting (voorstel §9.2, meting M6). Onafhankelijk
van de besluiten 1–8; dezelfde RuleChecker-code als
dialect-niet-nesting-frontend, dus erna of erin, nooit ernaast.*

Gemeten 2026-09-23 op de frontend van main 8dd05e25: een basisregel en haar
`Uitzondering op`, elk met een `// Bron:`-regel erboven, geven een IR
waarin geen van beide annotatieteksten ergens voorkomt; de verifier geeft
PASS. `defaultsVan` en `defaultVan` zetten `bron: null`
(`RuleChecker.java:331-351`, "no per-group bron", "No per-default bron").
Op een gewone regel komt de annotatie wel mee (`bronAnnotaties`, WF-082).
De werkbank hangt de herleidbaarheid per wetslid aan deze annotaties, en
juist in de defaults-families (art. 15, 20, 21 WW) verdwijnt ze.

## Opties

- (a) De annotatietekst van elk blok in zijn `Default.bron`, die van de
  basis ook in de `bron` van de groep — de velden die de handankers al voor
  wetsverwijzingen gebruiken ("art. 15: …"); geen draadwijziging;
  meerdere annotaties op één blok samengevoegd. `PRIORITY-RELATION.md`
  §4.5 plaatst de herkomst van een voorrang al in `Default.bron`. De
  voorgestelde richting.
- (b) Een `bronAnnotaties`-lijst op `Default` en `PrioritizedDefaults`, een
  extensieveld onder WF-082 — verliesvrij, maar een draadwijziging
  (beschermd pad).

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood eerst met de proef hierboven; daarna staan beide teksten in de
  IR op de plek van de gekozen optie.
- (b) De goldens van gate 10 blijven byte-identiek: geen van de drie
  defaults-goldens met een `bron.rgs` (`jutterswet-bergingsdrempel`,
  `-wachtgeld`, `-wrakrecht`) draagt een `// Bron:`-annotatie (gemeten
  2026-09-23). Alle poorten groen.

May touch: `frontends/regelspraak/src/main/java/**/resolver/**`, nieuwe
testklassen, kaartverplaatsing; bij optie (b) ook schema, verifier en
`spec/IR-SPEC.md` als voorstel.
