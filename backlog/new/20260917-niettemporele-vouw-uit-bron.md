# niettemporele-vouw-uit-bron: de ongekwalificeerde kwantorvouw bereikt de frontend

Aanleiding: de RegelSpraak-frontend lowert een §8.2-zin alleen naar een
Kwantorvouw wanneer de conditie een heelperiodekwalificatie draagt
("gedurende het gehele jaar") — `RuleChecker.kwantorvouw` opent met
`if (periodeVan(c) == null) return c;`. De NIET-temporele vouw ("alle X
die Y <vergelijking>", zonder kwalificatie) speelt sinds de
asymmetrie-kaart drieweg, maar alleen vanuit handgeschreven IR; de
goldens `jutterswet-schouwploeg` en `-vuurtoren` hebben daarom geen
`bron.rgs`, en de ongekwalificeerde spelling valt vandaag op WF-096
(vergelijkingsvorm) of `Onopgelost` (kenmerkvorm). Maintainerslezing
2026-09-17: de ongekwalificeerde vorm is ook valide RegelSpraak; de
frontend moet hem dragen. Dit is een echte frontenduitbreiding — rij 40
noemt hem als "widening that is a frontend card of its own".

Eerste stap van de kaart: de spec-grond vaststellen. Toets tegen de
RegelSpraak-specificatie v2.3.0 (read-only; de ANTLR-mapping ligt in de
toolchain-repo onder `specification/`) welke productie de
ongekwalificeerde kwantificatie draagt en met welke semantiek, en leg
die vindplaats in het Result vast — géén spec-tekst citeren in
corpusmateriaal.

## Acceptatie (bij oppakken aanscherpen)

- (a) De spec-grond (v2.3.0-vindplaats + de gedragen spellingen) in het
  Result; elke spelling die v2.3.0 niet draagt blijft luid geweigerd.
- (b) De frontend lowert de gedragen ongekwalificeerde vormen naar de
  Kwantorvouw met niet-temporele matrix (alle vijf kwantoren, filter
  incluis), met JUnit-getuigen en rood-eerst-metingen van de huidige
  WF-096/Onopgelost-uitkomsten.
- (c) `jutterswet-schouwploeg` en `-vuurtoren` krijgen hun `bron.rgs`
  (byte-identieke heremissie door gate 10) — of, waar de zinsvorm net
  anders uitpakt, een nieuwe golden mét bron ernaast; niets bestaands
  wijzigt zonder gemelde reden.
- (d) De rij 40-clausules over de §8.2-poort en de handgeschreven
  getuigen versmallen — registervoorstel in het Result. De
  getalpredicaat-weigering van de diedat-kaart wordt herzien waar de
  matrix niet meer per constructie temporeel is (de reden vervalt op
  deze positie — meet en beslis).
- (e) Alle veertien gates groen; toolchain-naschok bij de herpin
  gemeten (de vendored resolver gaat de vorm loweren; de
  toolchain-armen dragen de niet-temporele matrix nog niet — luide
  weigering verwacht, meten).
