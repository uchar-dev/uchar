# subtype-frontend-keten — de ketensluiting van de dialect-frontend

Goal: de reparatiekaart die blok A van `backlog/done/20260912-subtype-verdeling-keten.md`
uitvoert. `RegelSpraakModel.Objecttype` draagt de ketenbewuste accessors sinds de
subtype-kernel (`keten`, `attribuut`, `dimensiesVan`, `heeftLid`, `kenmerkSoort`,
`declarantVan`), maar de resolverlaag van `frontends/regelspraak` leest op vijf gemeten
plekken nog de rauwe declaratiekaarten van het EIGEN type. Een lid van een VOOROUDER
bindt daar niet — of, in de temporaliteitsfamilie, wordt het verkeerd BEOORDEELD, wat
erger is dan niet binden: een tijdreekswaarde ontsnapt dan aan een scalaire wacht. Deze
kaart sluit de vijf, voegt de ontbrekende keten-accessors generiek toe aan het model,
en geeft elke plek een getuige — rood-eerst in JUnit, plus één nieuwe rgs-golden die
een geërfd lid werkelijk door een regel gebruikt vanaf een subtype-subject.

De vijf plekken (SUBTYPING §3.2: opwaarts, dichtstbijzijnde declaratie wint):

1. `MeervoudMatcher.vindAttribuut` — leest `attributen`/`attribuutMeervoud` van het
   eigen type. Oorzaak van de p18-weigering waarmee de vorige kaart begon.
2. `KenmerkMatcher.vind` — dezelfde vraag voor kenmerken; acht aanroepplaatsen.
3. De objectcreatie-kenmerktak in `RuleChecker` — ERROR waar een binding hoort.
4. De temporaliteitsfamilie — twaalf rauwe `containsKey`-lookups zonder keten-accessor.
5. `ExpressieBouwer.dimensieCel` — leest `attribuutDimensies` rauw.

Acceptance:
- per plek een rood-eerst JUnit-getuige: falend op de basis, groen na de fix;
- een nieuwe rgs-golden (bron.rgs + geëmitte ir.json + case met handberekende pins)
  waarin een verdeling naar een subtype met een geërfd doelattribuut door de hele
  keten loopt;
- alle dertien gates groen, gate 10 met byte-identieke re-emissie van élke rgs-golden
  (de ketenbrede ambiguïteitseis mag geen bestaand document verstoren) en gate 11 met
  een byte-identieke ©-zone;
- elke plek die bewust RAUW blijft draagt zijn reden.

May touch: `frontends/regelspraak/` (resolverlaag, model-accessors, tests),
`corpus/golden/<nieuwe golden>`, `scripts/check-drools.sh` (alleen de
`EXPECTED_PLAYED`-gelijkheid), dit kaartbestand.

Out of scope: `spec/**` (ook `CONSTRUCTS.md` en `PLAN.md` niet — registervoorstel als
letterlijke tekst in het Result), `spec/verifier/`, `ref/`, `backends/`, bestaande
corpusbestanden, `.github/`.

Result:

**De vijf plekken, elk met zijn rood-eerst getuige.** De twaalf getuigen zijn
eerst op de basis geschreven en gemeten (12 rood, 227 groen), daarna groen
gemaakt (239 groen, 0 rood).

| plek | rood-eerst gemeten | fix |
|---|---|---|
| 1 `MeervoudMatcher.vindAttribuut` | `bergingsaandelen` bond niets; het meervoud bleef als `doelAttribuut` op de wire staan | `ot.keten()`, exact-enkelvoud via `attribuutNaam`, `(mv: …)` bij de declarant, ambiguïteitseis ketenbreed |
| 2 `KenmerkMatcher.vind` | een geërfd kenmerk bond in geen enkele predicaatvorm | idem, exacte basisvorm via `kenmerkNaam`; binding en §3.5-soort zien nu dezelfde declarant |
| 3 objectcreatie-kenmerktak | `ERROR: 'betwist' is geen attribuut of kenmerk van Hoofdberger` | `nieuwType.kenmerkNaam(naam)`, symmetrisch met de attribuut-tak ernaast |
| 4 temporaliteitsfamilie | drie verschillende uitkomsten, zie hieronder | vijf nieuwe keten-accessors; alle twaalf lookups lopen erdoorheen |
| 5 `ExpressieBouwer.dimensieCel` | een cellezing van een geërfd gedimensioneerd attribuut bleef marker | `ot.gedimensioneerdeLeden()` |

**Plek 4 is de gevaarlijke, en ze faalt in drie richtingen tegelijk** — alle
drie gemeten voordat er iets veranderde:

1. **stil fout.** `het totaal van zijn wachtvergoeding per maand gedurende de
   tijd dat hij wachtplichtig is` verloor zijn klem: `filter: null`, dus het
   totaal telt de hele tijdlijn mee en het getal is fout zonder markering.
2. **ontsnapping.** §8.4 laat een NIET-temporele vergelijking als statisch lid
   in een samengestelde gedurende-conditie toe. Een geërfd tijdlijnattribuut
   werd als statisch beoordeeld en glipte erdoor — een `List<Segment>` in een
   scalaire term. Nu blijft de hele conditie een eerlijke `Onopgelost`, exact
   zoals het vlakke equivalent (de getuige vergelijkt de twee `resolvedRegels`
   letterlijk).
3. **verzonnen fouten.** `totaal van vereist een eenheid per tijdseenheid
   (§4.20 Tabel 30)` en tweemaal `de check op volledige periode … kan alleen op
   een tijdsafhankelijk onderwerp worden toegepast (§8.2)`, alle drie op
   documenten die niets mankeert.

**Generiek boven speciaal.** De drie bestaande accessors droegen elk hun eigen
kopie van dezelfde ketenlus; er is nu één `opKeten`, en de vijf nieuwe
(`granulariteitVan`, `tijdlijnAantalVan`, `kenmerkGranulariteitVan`,
`kenmerkTijdlijnAantalVan`, plus `gedimensioneerdeLeden`, `attribuutNaam` en
`kenmerkNaam`) zijn er varianten van. `heeftLid` liep al ketenbewust maar met
een derde kopie van de lus; die is nu `attribuut` plus `kenmerkNaam`.

**Een zesde plek, meegerepareerd.** `ExpressieBouwer` r.742 zocht de
GEDECLAREERDE spelling van een gedimensioneerd attribuut in de eigen
attribuutkaart, twee regels nadat `dimensiesVan` de keten al had gelezen. Bij
een geërfd attribuut viel hij terug op de GESCHREVEN vorm en zette daarmee een
naam op de wire die geen declaratie draagt — dezelfde fout als plek 1, alleen
via de andere deur. Nu `attribuutNaam`.

**Terecht rauw, elk met zijn reden ter plekke.** `DefinitionResolver`'s
WF-105-schaduwcheck en de declaratieplaats zelf (die moeten eigen en geërfd
juist scheiden), `IrEmitter`'s ledenemissie (de wire draagt eigen leden plus
`supertype`), en twee die de kaart niet noemde: `RuleChecker.domeinReferenties`
ITEREERT over declaraties in plaats van er een op te zoeken — ketengesloten zou
ze één geërfd attribuut één keer per afstammeling melden — en de
startpunt-reservering, waarover hieronder.

**De §9.9-ankerfamilie: gevonden, gemeten, NIET gerepareerd.**
`heeftVariabelStartpunt` leest `attribuutTijdlijnNaam` rauw, dus een subtype
met een geërfd attribuut op een tijdlijn met variabel startpunt geldt als
niet-variabel. Ik heb hem verbreed en weer teruggedraaid: het is geen
ledenlookup maar een protocol. Een startpuntbepaling DECLAREERT een synthetisch
attribuut op een genoemd subject; `startpuntLid`, `isStartpuntLid` en de
naamreservering in `RuleChecker` sleutelen alle drie op die subjectnaam. Eén van
de vier door de keten laten lopen verschuift de naad alleen. De hele familie
sluiten is een eigen, gemeten kaart — al haar consumenten zitten in
`backends/drools`, buiten may-touch, en geen corpusgetuige raakt haar. De reden
staat nu bij `heeftVariabelStartpunt` en bij de reservering.

**De golden: `corpus/golden/jutterswet-bergingsdeling`.** De brontweeling van
`jutterswet-hoofdberging`, dat dezelfde verdeling met de hand pint omdat er
geen bron voor bestond. Twee posities in één document, beide een lid van de
voorouder gelezen vanaf een subtype: het doelattribuut van de verdeling
(schrijfkant) en de kenmerkvoorwaarde van de tweede regel (leeskant). De
handberekende pins: ⌊1001/2⌋ = 500 elk, `onverdeelde rest` 1 — dezelfde drie
getallen die hoofdberging met de hand pint voor dezelfde rekensom, daarom
hergebruikt in plaats van verzonnen; plus `toeslag` 25 voor de gemachtigde
hoofdberger en leeg voor de andere. Speelt in ref, clingo én Drools.
`scripts/check-drools.sh` **EXPECTED_PLAYED 131 -> 132**, met reden en datum in
het script.

**De geërfd-temporele golden kon klein, maar niet landen.**
`jutterswet-erfwacht` is gebouwd en gemeten: schema-geldig, verifier-schoon,
ref = clingo exact, weerlegt de no-op. De bron, zodat de vervolgkaart hem niet
opnieuw hoeft te bedenken:

```
Objecttype de Jutter (mv: Jutters) (bezield)
    de wachtvergoeding per maand Numeriek (geheel getal) voor elke maand;
    is wachtplichtig kenmerk (bijvoeglijk) voor elke maand;

Objecttype de Klipjutter is een Jutter (mv: Klipjutters) (bezield)
    de wachtsom Numeriek (geheel getal);

Regel wachtsom van een klipjutter
    geldig altijd
        De wachtsom van een Klipjutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij wachtplichtig is).
```

De case is die van `jutterswet-getijdenwacht` met `Jutter` vervangen door
`Klipjutter` en alleen haar `J1` en `J3` (hier `K1` en `K2`), dus met dezelfde
handberekening: 10 €/maand over 2023, 11 over het eerste halfjaar 2024,
wachtplichtig van 2023-07-01 tot 2024-02-01 = 6 × 10 + 1 × 11 = **71**; `K2` is
nooit wachtplichtig en krijgt **0**. Maar Drools weigert hem per vorm:

> `EXCLUDE loader: Objecttype.supertype (a supertype chain carrying a temporal
> or dimensioned member (the facet side maps are read per declared type))`

Dat is de backend die zijn EIGEN ketenblindheid kent en er luid voor weigert;
het commentaar bij `IrLoader.bindSupertypes` zegt letterlijk "until those reads
are chain-aware". Gemeten: 122 rauwe facet-lezingen in `backends/drools` buiten
de loader. Hem toch admitteren vraagt een `EXPECTED_CHANNEL`-pin, en may-touch
geeft me in dat script alleen de `EXPECTED_PLAYED`-gelijkheid. De golden is dus
weer verwijderd; de dekking van plek 4 rust op de vier JUnit-getuigen, en de
backendkant staat hieronder als vervolgwerk.

**Gate 10 expliciet gemeten.** Precies één bestaande rgs-golden draagt een
supertype (`jutterswet-wrakrecht`); voor alle andere is `keten()` gelijk aan
`[self]`, dus is de ketenverbreding daar bewijsbaar een no-op. De
ambiguïteitsverbreding raakt geen enkel bestaand document: 44 rgs-goldens
re-emitteren byte-identiek (43 + de nieuwe). Gate 11: de ©-zone ongewijzigd,
45 re-emissies byte-identiek, vloer 45.

**Dertien gates groen** (slotregels in het sessieverslag).

**Registervoorstel** (letterlijke tekst, hoofdsessie landt het),
`spec/CONSTRUCTS.md` rij 39, aan de bestaande cel toe te voegen:

> De dialect-frontend liep bij de kernel op vijf plekken achter op zijn eigen
> model: `MeervoudMatcher`, `KenmerkMatcher`, de objectcreatie-kenmerktak, de
> hele temporaliteitsfamilie (die geen keten-accessor hád) en `dimensieCel`
> lazen de declaratiekaarten van het eigen type, zodat een lid van een
> voorouder niet bond — en bij de temporele facetten niet ontbrak maar VERKEERD
> werd beoordeeld: een tijdlijn die als scalair doorgaat verliest stil haar
> §7.1-klem, ontsnapt aan de §8.4-wacht en trekt tegelijk een verzonnen
> §4.20- en §8.2-fout. Hersteld 2026-09-12; getuige `golden/jutterswet-bergingsdeling`,
> de brontweeling van `golden/jutterswet-hoofdberging`, die de vorm pas nu uit
> een `bron.rgs` laat komen. Nog open, met reden: de §9.9-ankerfamilie
> (`attribuutTijdlijnNaam`) sleutelt op de gedeclareerde subjectnaam en wordt
> als geheel verbreed of niet, en de Drools-backend leest zijn facetkaarten nog
> per gedeclareerd type — hij weigert de combinatie subtype × temporeel/
> gedimensioneerd daarom luid in de loader.

**Vervolgwerk — gemeten, niet in deze ronde gerepareerd.**

*A. De Drools-backend leest zijn facetkaarten per gedeclareerd type.* 122 rauwe
lezingen buiten de loader; de loader weigert de combinatie subtype ×
temporeel/gedimensioneerd expliciet om dat af te dekken. Dezelfde reparatie als
deze kaart, één laag lager, en zij maakt `jutterswet-erfwacht` alsnog
admitteerbaar — bron en caseafleiding staan hierboven. Die kaart heft de
weigering op en zet `EXPECTED_PLAYED` een stap hoger.

*B. De §9.9-ankerfamilie sluiten.* Vier plekken tegelijk
(`heeftVariabelStartpunt`, `startpuntLid`, `isStartpuntLid`, de reservering in
`RuleChecker`), met een eigen meting; consumenten in `backends/drools`.

*C. Het onderwerp van een type-rangende positie resolveert niet door ⊑.*
Gemeten: met de relatie op de VOOROUDER (`bergingsdeelname` naar `Berger`) en
`alle Hoofdbergers` als doelverzameling emitteert de frontend `doelType: null`
— de onderwerpresolutie zoekt een relatie van `Berging` naar `Hoofdberger` en
vindt er geen, terwijl de rol die `Berger` vult elke `Hoofdberger` bereikt.
Daardoor is de vorm die `jutterswet-hoofdberging` met de hand pint (waar
ontvangersverzameling en rolbereik uit elkaar lopen) nog steeds niet uit een
bron schrijfbaar. Andere klasse dan deze kaart — een navigatie/rolbinding-vraag,
geen ledenlookup — dus alleen gemeld.

**Eerlijke afwijkingen.** (a) De kaart vroeg per plek één JUnit-getuige; plek 4
kreeg er vier, omdat één test maar één van de drie faalrichtingen laat zien.
(b) De kaart noemde een zesde plek als mogelijkheid; ik vond er drie, en heb er
één gerepareerd (dezelfde klasse), één beargumenteerd rauw gelaten met de reden
ter plekke (§9.9) en één alleen gemeld (C). (c) De geërfd-temporele golden is
gebouwd en gemeten maar niet geland — de reden staat hierboven en is geen
grootte maar een may-touch-grens. (d) `kern/Tijdrekening.java` en
`model/RegelSpraakModel.java` worden meegecompileerd door `backends/drools`;
hun gedrag verandert daar dus mee. Bewust en gewenst (het is één en dezelfde
ketenvraag), en gate 12 en 13 staan stil: geen corpusgolden combineert vandaag
een supertype met een temporeel of gedimensioneerd lid — de loader weigert die
combinatie juist.
