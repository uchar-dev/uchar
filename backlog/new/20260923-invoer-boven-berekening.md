# invoer-boven-berekening: een oppervlak voor "deze afleiding mag door invoer worden overschreven"

*Status: concept (onderzoeks- en ontwerpkaart), 23 september 2026 — wacht
op de besluiten 1–8 van spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md;
besluit 7 splitst deze kaart af. Levert een voorstel, geen code.*

Aanleiding. De werkbank wil afgeleide waarden ook als invoer kunnen
aannemen en een afleiding door invoer laten overrulen (vorm 3 van de
onderzoekskaart dialect-defaults-niet-en-nesting). Wat er al is (voorstel
§6):

- Casusinvoer volgt Reading A: de virtuele invoer-default staat onder elke
  gewone default (RUNTIME-SEMANTICS ch. 7.8); Reading B voor casusinvoer
  is op 2026-08-20 afgewezen (PHASE4-DEFAULTS §10 besluit 1).
- Reading B bestaat als programmastructuur: de opgave als eigen feit plus
  een hoogste default die haar leest (PHASE4-DEFAULTS §4.1, het
  hardheidspatroon); voor Catala-`context`-variabelen gegenereerd
  (PHASE5-CATALA besluit 6). In het dialect is dat vandaag schrijfbaar voor
  een attribuutdoel, als laatst geschreven `Uitzondering op`.
- Bij een kenmerkdoel geldt de positieve richting al: een invoerkenmerk
  blijft staan onder elke negatieve beslissing (gemeten, voorstel M9). De
  negatieve richting vraagt vorm 1 plus een expliciet negatief
  invoerkenmerk (het 'nonsmoker'-patroon van SBVR 1.5 §24.2.1.3).

## Te beslissen in het voorstel van deze kaart

- suiker (een markering die het hardheidspatroon genereert; geen
  draadwijziging — de voorgestelde start) of semantiek (een invoer-default
  boven de gewone defaults per doel: RUNTIME-SEMANTICS ch. 7.8, de
  `inputval/3`-codering van clingo, de invoer in Drools, mogelijk het
  caseformaat);
- de plaats van de markering: per attribuut (declaratie) of per regel;
- de uitleg "opgegeven, niet afgeleid": de beslissende default draagt de
  naam van het blok; de uitleg in regelspraak-java moet defaults kunnen
  lezen (bijvangst testdocument-uitzonderingsvormen);
- tegenspraak tussen feiten en opgave: de berekende waarde op een eigen
  attribuut plus een consistentieregel, één §9.5-feit bij verschil
  (PHASE4-DEFAULTS §4.1);
- wat niet heropend wordt: casusinvoer als geheel blijft Reading A.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een voorstel in `spec/proposals/` met de opties, de metingen die ze
  onderscheiden en voorgestelde besluiten.
- (b) Een goldenplan met ten minste een attribuut- en een kenmerkgeval.

May touch: `spec/proposals/**`, `backlog/**`.
