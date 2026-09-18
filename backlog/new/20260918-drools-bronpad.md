# drools-bronpad: beide Drools-takken wandelen de geneste vouwbron

Goal: `backends/drools` consumeert `Functie.bronPad` in beide
transpilertakken. De loader bewaart iedere symbolische rolstap en de
accumulate-range wandelt het pad van regelonderwerp naar
`elementObjecttype`, zodat een geldige twee-hop to-many-aggregatie niet
meer wordt geweigerd en nooit terugvalt op een toevallige directe
relatie.

Evidence:

- `backlog/done/20260911-geneste-aggregatie-rolketen.md` heeft
  `bronPad` al normatief, in schema, verifier, frontend, ref en clingo
  geland. De bedoelde lezing is een vlakke unie van bereikbare
  eindinstanties; een eindinstantie die via meer dan één route
  bereikbaar is draagt één keer bij.
- De huidige loader weigert `Functie.bronPad` expliciet. Dat hek is
  correct zolang de backend geen pad draagt: het veld negeren laat de
  bestaande by-pair-afleiding een toevallige directe relatie kiezen en
  berekent aantoonbaar de verkeerde verzameling.
- De oude toolchain speelde vijf `PHASE_GENEST`-gevallen in beide
  Drools-armen: MIN over twee rechten en drie verrekeningen, lege
  range, lege bijdragers, een gevulde vergelijking en de verschillende
  leegvergelijkingen. De huidige goldens
  `anchor/jutterswet-bootberging`, `jutterswet-vondstketen` en
  `jutterswet-muntketen` worden in beide takken bij de loader geweigerd.
  De laatste getuigt bovendien dat het eindtype een subtype van de
  laatste rolvuller mag zijn.

Acceptance:

- Rood-eerst: leg voor alle drie goldens in beide CLI-takken de huidige
  `EXCLUDE loader: ... Functie.bronPad` vast.
- De loader decodeert en bewaart het volledige geordende pad van
  `RolBinding`-stappen; geen stap wordt uit vrije tekst of alleen uit
  het objecttypepaar herleid. Een leeg, niet-landend, verkeerd gericht
  of cardinaliteitsstrijdig pad blijft een luide, per-vormweigering
  wanneer het document ondanks de verifier aan de backend wordt
  aangeboden.
- Beide transpilers bouwen per padstap een expliciete join vanaf het
  onderwerp. Zij gebruiken de opslagdragende bean-kant van iedere rol,
  ondersteunen een to-one/tomany-mix en subtype-landing, slaan lege
  tussenstappen over en de-dupliceren dezelfde eindinstantie over
  meerdere routes vóór de fold. De foldsoort en leegwaarde blijven de
  bestaande `Functie`-semantiek; de padcode bevat geen MIN-specifieke
  uitzondering.
- Voeg dual-branch backendgetuigen toe voor:
  1. de absolute oude MIN-uitkomsten inclusief lege bijdrager;
  2. een lege tussenrange;
  3. dezelfde eindinstantie via twee routes (één bijdrage);
  4. een model met óók een directe onderwerp-eindtyperelatie, waarbij
     uitsluitend `bronPad` de range kiest;
  5. subtype-landing op de laatste stap.
- De bestaande cases van `anchor/jutterswet-bootberging`,
  `jutterswet-vondstketen` en `jutterswet-muntketen` geven zonder vlag
  én met `--drl` `PLAYED`, met ongewijzigde verwachtingen.
  `scripts/check-drools.sh` verwijdert precies deze drie
  `EXPECTED_CHANNEL`-regels en verhoogt `EXPECTED_PLAYED` met precies
  drie ten opzichte van de bij oppakken gemeten basis; datum en reden
  staan bij de beschermde scriptwijziging.
- Het padloze `bronRol`-geval blijft byte- en gedragsmatig gelijk.
  Mutatietests moeten bewijzen dat het verwijderen of verwisselen van
  één stap niet toevallig dezelfde uitkomst blijft geven.
- `mvn -q -o -f backends/drools/pom.xml package`,
  `bash scripts/check-drools.sh`,
  `bash scripts/check-reftype-derivation.sh` en alle veertien gates
  zijn groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/loader/IrLoader.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`,
uitsluitend werkelijk gedeelde pad-/joinhelpers onder
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/kern/`,
`backends/drools/src/test/**`,
`scripts/check-drools.sh` uitsluitend voor de drie genoemde
kanaalregels en de +3-pin (beschermd-padvoorstel), deze kaart en bij
afronding haar verplaatsing binnen `backlog/`.

Out of scope: `Kwantorvouw.bronPad`, de geneste AANTAL-vorm (WF-102
laat `bronPad` alleen op de SET-bron toe), een nieuwe wirevorm,
wijzigingen aan frontend/schema/verifier/ref/clingo, nieuwe
corpusverwachtingen en andere opslagrichtingweigeringen. Het expliciete
loaderhek mag pas verdwijnen in dezelfde wijziging die beide
transpilers en hun gedragsgetuigen groen maakt. Deze kaart deelt beide
transpilers met andere pariteitskaarten en wordt serieel gepland.
