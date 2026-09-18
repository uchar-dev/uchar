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

## Result (2026-09-18)

Beide Drools-takken consumeren `Functie.bronPad`. Het loaderhek is weg in
dezelfde wijziging die de wandeling bouwt, en van de drie goldens speelt er
ÉÉN — niet drie. Dat is de afwijking van deze kaart en ze staat vooraan,
niet achteraan.

### Rood-eerst, letterlijk (jar van de merge-base, 3 × 2 regels)

Alle zes regels zijn woordelijk gelijk:

    anchor/jutterswet-bootberging  (exec)  EXCLUDE loader: form not carried by the v0 Drools backend: Functie.bronPad (a multi-step fold-source range (WF-102); the accumulate source carries one join role)
    anchor/jutterswet-bootberging  (--drl) EXCLUDE loader: form not carried by the v0 Drools backend: Functie.bronPad (a multi-step fold-source range (WF-102); the accumulate source carries one join role)
    jutterswet-vondstketen         (exec)  EXCLUDE loader: form not carried by the v0 Drools backend: Functie.bronPad (a multi-step fold-source range (WF-102); the accumulate source carries one join role)
    jutterswet-vondstketen         (--drl) EXCLUDE loader: form not carried by the v0 Drools backend: Functie.bronPad (a multi-step fold-source range (WF-102); the accumulate source carries one join role)
    jutterswet-muntketen           (exec)  EXCLUDE loader: form not carried by the v0 Drools backend: Functie.bronPad (a multi-step fold-source range (WF-102); the accumulate source carries one join role)
    jutterswet-muntketen           (--drl) EXCLUDE loader: form not carried by the v0 Drools backend: Functie.bronPad (a multi-step fold-source range (WF-102); the accumulate source carries one join role)

### Groen ná — en waarom het er één is en geen drie

    jutterswet-vondstketen         (exec)  PLAYED 1
    jutterswet-vondstketen         (--drl) PLAYED 1
    anchor/jutterswet-bootberging  (exec)  EXCLUDE transpiler SKIP: regel: laagste vondstgewicht van een jutter — regel (bronPad-stap 1 (Boot -> Vondst) heeft geen beanveld — de rol 'geborgen vondst' op Boot is meervoudig, dus haar opslag is het veld aan de overzijde, en ook daar leidt het beanmodel er geen af (beide zijden meervoudig) (§13.3.9 opslagrichting)): laagste vondstgewicht van een jutter
    anchor/jutterswet-bootberging  (--drl) EXCLUDE drl-transpiler MARKER: regel: laagste vondstgewicht van een jutter — bronPad-stap 1 (Boot -> Vondst) heeft geen beanveld — [zelfde zin]
    jutterswet-muntketen           (exec)  EXCLUDE transpiler SKIP: regel: laagste muntgewicht — regel (bronPad-stap 1 (Boot -> Munt) heeft geen beanveld — [zelfde zin]): laagste muntgewicht
    jutterswet-muntketen           (--drl) EXCLUDE drl-transpiler MARKER: regel: laagste muntgewicht — bronPad-stap 1 (Boot -> Munt) heeft geen beanveld — [zelfde zin]

De kaart nam aan dat de drie goldens verschillen in hun PAD. Ze verschillen
ook in hun MODEL, en dat is doorslaggevend: `bootberging` en `muntketen`
declareren hun middelste feittype `berging` met BEIDE rollen meervoudig,
`vondstketen` met `bergende boot` enkelvoudig. §13.3.9 bergt een relatie in
precies één scalair referentieveld, aan de ENE kant; een relatie zonder
ene-kant heeft aan geen van beide kanten een veld. Dat is exact de
`anchor/jutterswet-erflijn`-vorm, die om dezelfde reden al weigerde. De
drie goldens zijn dus niet drie padvormen maar twee modelvormen, en de
tweede is voor deze backend onbereikbaar zolang het beanmodel geen
collectie-representatie voor veel-op-veel draagt.

Dat opheffen raakt `FactModelGenerator` (de veldafleiding), `CaseRunner`
(de case-invoerkant weigert zo'n relatie al bij het schrijven) en beide
transpilers. Het valt bovendien woordelijk onder de Out of scope van deze
kaart ("andere opslagrichtingweigeringen"). Ik heb het daarom niet gedaan
en het niet stil gelaten: de twee kanaalregels dragen nu hun werkelijke,
één laag diepere reden, precies zoals `anchor/jutterswet-bergingstoezicht`
die bij de rolvelden-kaart kreeg. Voorstel voor een vervolgkaart:
"veel-op-veel in het Drools-beanmodel", met `bootberging`, `muntketen`,
`erflijn`, `gemeenteberging` en `penninguitgifte` als vijf gemeten
kandidaten.

### Wat er gebouwd is

**Loader.** `Functie.bronPad` wordt VERBATIM gedecodeerd — het hele
geordende rijtje `RolBinding`s, onderwerp-uitwaarts — en rijdt naast het
model mee in `IrLoader.Loaded.bronPaden`, om de reden waarom `bronRol` daar
al reed plus een scherpere: een pad is de enige uitbreiding die het model
NIET kan herafleiden. Het typepaar antwoordt over de eindpunten; de
tussentypen zijn de bewering van het document. De drie VORM-armen van
WF-102 blijven benoemde weigeringen in de loader — (a) naast een `bronRol`,
(b) minder dan twee stappen, (c) op iets anders dan de setbron van een
eenargumentsvouw — met (a) eerst, zodat een document dat beide spellingen
draagt daarvoor benoemd wordt. De MODELRELATIEVE helft van (d) — een stap
die niet aansluit, een landing naast het elementtype, een hop zonder
beanveld — zit in de transpilers, die als enige het vouwonderwerp kennen.

**`transpiler/kern/Bronpad`.** De ontleding, één keer voor beide takken:
per stap de gebonden rol, de tegenoverliggende rol, het bereikte type, de
opslagrichting en het beanveld, of één zin waarom niet (dezelfde zin in
beide takken, via `FactModelGenerator.geenJoinveldReden`). De vorm van de
lowering volgt uit het beanmodel en niet uit een keuze: een naar-veel-stap
is functioneel VANAF het element, een naar-een-stap vanaf het onderwerp.
Een pad dat een voorwaartse reeks gevolgd door een achterwaartse is, is
daarom één gelijkheid tussen twee gettersketens, en dan kan geen element
twee keer bereikt worden. Alleen waar een ACHTERWAARTSE stap door een
VOORWAARTSE wordt gevolgd bestaan er echt twee routes naar hetzelfde
element; daar wordt de tussenknoop EXISTENTIEEL gekwantificeerd. Dat is de
deduplicatie die de vlakke unie vraagt, vóór de vouw, en zij zit in de
BRON en niet in de vouw — de padcode kent geen enkele soortspecifieke arm.

**Beide transpilers.** De exec-tak bouwt het elementpatroon plus, waar
nodig, `DSL.exists` over de tussenpatronen; de DRL-tak schrijft dezelfde
wandeling als null-bewaakte gettersketen in de patroonconstraint, plus
`and exists( … )`. De vouw zelf is uit `aggregatie` gelicht naar `vouw`,
zodat filter, lege-SOM-terugval, de `!= null`-bronwacht en de
leegCount-companion letterlijk dezelfde code zijn voor beide bereikvormen.

**Eén gemeten Drools-detail.** Elke accumulate-TELLING draagt nu haar
elementvariabele. Een bronloze telling is legaal boven een gewoon patroon,
maar boven het subnetwerk dat een pad bouwt lokaliseert de model-compiler
het bronpatroon via de bronvariabele van de EERSTE functie en laat een
bronloze functie elke binding onopgelost: gemeten viel het vuren om met
`SubnetworkTuple cannot be cast to BigDecimal`. Over een gewoon patroon is
het semantiekvrij (de functie telt treffers en leest de waarde nooit), en
de corpusbrede veegproef bevestigt dat: geen enkele padloze golden beweegt.

### Getuigen

`backends/drools/src/test/.../cases/BronpadKetenWitnessTest` (7 rijen,
beide takken, eigen vocabulaire — Tolhuis / Sluis / Praam / Zegelpraam /
Ankersteen / Vat — met handberekende pins vóór er iets gedraaid is):

- `de_vlakke_unie_over_het_pad_in_beide_takken` draagt de kaartpunten 1
  t/m 5 in één document: de MIN en de SOM inclusief de bijdrager wiens
  eigen waarde leeg is (3,00 / 15,00), een tussenexemplaar dat niets
  bereikt en een onderwerp dat geen tussenexemplaar bereikt (leeg / 0 / 0),
  dezelfde eindinstantie via twee routes (12,00 en niet 24,00), de directe
  onderwerp-elementtyperelatie die ernaast ligt en nooit gevouwen mag
  worden, en de landing op een strikt SUBTYPE (5,00 en niet 3,00).
- `dezelfde_eindinstantie_via_twee_routes_telt_niet_dubbel` speelt hetzelfde
  document tegen 24,00 en eist dat beide takken het verwerpen.
- `de_directe_relatie_geeft_een_ander_antwoord_dan_het_pad` verruilt het
  pad voor de directe eenstapsbinding en meet wat een consument die het
  veld laat vallen antwoordt: `laagste vracht 1,00` (gepind 3,00),
  `totale vracht 1,00` (gepind 15,00), `laagste zegelvracht leeg` (gepind
  5,00).
- vier weigerrijen, beide takken, elk met hun eigen zin:
  `een_verwijderde_stap_breekt_de_wandeling`,
  `twee_verwisselde_stappen_breken_de_wandeling`,
  `een_pad_dat_naast_het_elementtype_landt_wordt_geweigerd`,
  `een_cardinaliteitsstrijdige_stap_wordt_geweigerd`.

`backends/drools/src/test/.../loader/BronPadWeigeringWitnessTest` (5 rijen)
is omgebouwd van "de loader weigert elk pad" naar wat de loader nu doet:
het hele geordende pad wordt bewaard (`bemanning/gevoerde boot`,
`berging/geborgen vondst`, in wire-volgorde), en de drie vormarmen (a),
(b) — leeg én eenstaps — en (c) weigeren bij naam.

**Mutatieproeven, feitelijk uitgevoerd en hersteld.** Twee zijn
DOCUMENTmutaties en staan permanent in de suite (de twee wandelrijen
hierboven): een stap verwijderd en twee stappen verwisseld maken het pad
in beide takken tot een benoemde weigering, niet tot toevallig hetzelfde
antwoord. Twee zijn CODEmutaties, gedraaid en teruggedraaid:

- deduplicatie uit (de tussenpatronen in de accumulate-BRON gevoegd in
  plaats van existentieel gekwantificeerd, in beide takken): rood, met
  `attribuut steenlast: verwacht 12.00, werkelijk 24.00`. Twee rijen vielen
  om.
- terugval op de directe relatie geforceerd (`pad = null` in beide takken):
  alle zeven rijen rood.

### Corpusbrede veegproef, vóór en ná, beide takken

199 `ir.json` (glob twee niveaus diep), elke golden met haar casebestanden
door beide CLI-takken, regel voor regel gedift:

    exec-model : 149 -> 150 PLAYED
    DRL-tekst  : 122 -> 123 PLAYED

Er bewegen EXACT drie regels per tak, en het zijn dezelfde drie: de twee
loaderweigeringen die transpilerweigeringen werden, en `vondstketen` die
gaat spelen. Geen enkele andere golden verandert van status of van reden —
dat is tegelijk het bewijs dat het padloze `bronRol`-geval byte- en
gedragsgelijk blijft, inclusief de telling-met-elementvariabele.

### Gates

Alle veertien, exitcodes letterlijk:

    self-contained: 0        rood: 0
    ir-schema: 0             catala: 0
    verifier: 0              regelspraak: 0
    case-schema: 0           toka-parity: 0
    oracle: 0                drools: 0
    clingo: 0                reftype-derivation: 0
    noop: 0                  flogic: 0

Gate 12 letterlijk:

    check-drools: PASS
      played 150  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
      auto-excluded 46  (per-form refusals: loader / transpiler marker / contract / unsupported input)
      value-divergence 1  (listed with reason — the exec-model differs from ref/clingo)
      no-case 2  (goldens without a case file)
      channel floor 49 non-PLAYED goldens pinned by name; played matches the pinned 150
      JUnit 437 tests, floor 239  (the backend's own suite, green with a non-zero count)

Gate 13 vóór (op de merge-base gemeten, in een aparte basis-werkboom) en ná
zijn TEKEN VOOR TEKEN gelijk:

    check-reftype-derivation: PASS (108 unit annotations twisted over 22 goldens, floor 74; 100 executor replays reproduce the untouched outcome, 21 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)
      corpus-wide accounting: 147 unit-bearing annotations = 111 twistable + 30 exempt (the annotation IS the source) + 6 without a convertible sister + 0 at a §2.10 fourth-class branch (v0 derives no result unit) + 0 outside the twist table

Dat is geen toeval maar de meting van een voorspelling: de twistverzameling
komt uit het corpus en daar is niets aan veranderd, `vondstketen` en
`muntketen` dragen geen eenheid, en `bootberging` is wél
eenheidsdragend maar weigert vóór én ná in beide takken. Geen vloer is
aangeraakt.

### De scriptwijziging, verantwoord

`scripts/check-drools.sh`, gemandateerd beschermd pad: één
`EXPECTED_CHANNEL`-regel weg (`jutterswet-vondstketen`), `EXPECTED_PLAYED`
149 -> 150 met datum en reden, en de REDENEN van de twee overblijvende
regels bijgewerkt naar de weigering die zij werkelijk dragen. Dat laatste is
geen vrijheid die ik nam: de oude redenen noemden een loaderweigering die
niet meer bestaat, en het script zegt zelf dat de reden documentatie is en
herschreven mag worden. De kaart vroeg drie regels en +3; het zijn er één
en +1, om de reden hierboven. Niets anders in het script.

### Registervoorstel voor `spec/CONSTRUCTS.md` (niet geland)

Rij 1, in het blok **Landed (2026-09-11)** over `bronPad`, is de slotzin
over Drools achterhaald. Voorstel, ter vervanging van "de Drools-backend
WEIGERT de vorm bij naam (`Functie.bronPad`) in de kanaalvloer van gate 12
— expliciet en niet geërfd, … waar de zin 3,00 zegt.":

> de Drools-backend WANDELT het pad sinds 2026-09-18 in BEIDE
> transpilertakken (kaart `20260918-drools-bronpad`): de loader bewaart de
> stappen verbatim — het pad is de enige uitbreiding die het model niet kan
> herafleiden — en elke tak bouwt per stap een expliciete join op het
> §13.3.9-beanveld van die stap. De deduplicatie volgt uit die richtingen:
> een naar-veel-stap is functioneel vanaf het element en een naar-een-stap
> vanaf het onderwerp, dus een voorwaartse reeks gevolgd door een
> achterwaartse is één gelijkheid tussen twee gettersketens waarlangs geen
> element twee keer bereikbaar is, en alleen waar een achterwaartse stap
> door een voorwaartse wordt gevolgd wordt de tussenknoop existentieel
> gekwantificeerd. `jutterswet-vondstketen` speelt daarmee
> (`EXPECTED_PLAYED` 149 → 150) — de golden waarop het verschil een WAARDE
> is: zonder het veld blijft hetzelfde document `verifier: PASS` en
> antwoorden ref, clingo én Drools 5,00 waar de zin 3,00 zegt.
> `anchor/jutterswet-bootberging` en `jutterswet-muntketen` weigeren nog,
> maar één laag dieper en om een reden die niets met het pad te maken
> heeft: hun middelste feittype declareert beide rollen meervoudig, en een
> relatie zonder ene-kant heeft in dit beanmodel aan geen van beide kanten
> een veld (de `erflijn`-vorm).

### Bewuste restscope

- Veel-op-veel in het Drools-beanmodel — de vervolgkaart hierboven.
- `Kwantorvouw.bronPad` blijft een loaderweigering (Out of scope). De
  ontleding in `kern` is er klaar voor: die knoop draagt zijn pad al in het
  record en heeft geen zijkanaal nodig.
- De conservatieve weigering van een naar-een-elementrol met meervoudige
  zusterrol is NIET gespiegeld op padstappen. Een padstap eist alleen dat
  het veld bestaat. Dat is bewust: die weigering is een eigen gemeten kaart
  en valt onder "andere opslagrichtingweigeringen".
- Een subtype-instantie die een naar-een-rol vult wiens gedeclareerde
  vuller haar SUPERtype is, wordt door de case-invoerkant geweigerd
  (`heeftReferentieVeld` vraagt naar het exacte doeltype terwijl het
  beanveld op de voorouder staat). Gevonden bij het bouwen van de getuige
  en omzeild door die link op een gewone Praam te leggen; het raakt het pad
  niet en hoort bij dezelfde beanmodel-kaart.
