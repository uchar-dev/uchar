# subtype-frontend-keten — de ketensluiting van de dialect-frontend

Goal: de reparatiekaart die blok A van `backlog/done/20260912-subtype-verdeling-keten.md`
uitvoert. `RegelSpraakModel.Objecttype` draagt de ketenbewuste accessors sinds de
subtype-kernel (`keten`, `attribuut`, `dimensiesVan`, `heeftLid`, `kenmerkSoort`,
`declarantVan`), maar de resolverlaag van `frontends/regelspraak` leest op vijf gemeten
plekken nog de rauwe declaratiekaarten van het EIGEN type. Een lid van een VOOROUDER
bindt daar niet — of, in de temporaliteitsfamilie, wordt het verkeerd BEOORDEELD. Het
gevolg is bijna overal LUID: de wire is goed omheind, dus een correct document wordt
onbruikbaar geweigerd in plaats van dat er stil een verkeerd getal uit komt. Op één
plek niet, en die is daarmee de ernstigste. Deze kaart sluit de vijf, voegt de
ontbrekende keten-accessors generiek toe aan het model, en geeft elke plek een getuige
— rood-eerst in JUnit, plus één nieuwe rgs-golden die een geërfd lid werkelijk door een
regel gebruikt vanaf een subtype-subject.

De vijf plekken (SUBTYPING §3.2: opwaarts, dichtstbijzijnde declaratie wint):

1. `MeervoudMatcher.vindAttribuut` — leest `attributen`/`attribuutMeervoud` van het
   eigen type. Oorzaak van de p18-weigering waarmee de vorige kaart begon.
2. `KenmerkMatcher.vind` — dezelfde vraag voor kenmerken; elf aanroepplaatsen
   (gemeten: vijf in `ExpressieBouwer`, zes in `RuleChecker`).
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

**De zes plekken, elk met zijn getuige.** Achttien getuigen, eerst op de basis
geschreven en gemeten, daarna groen gemaakt: **17 rood op de basis, 245 groen
op de kop, 0 rood**. Eén van de achttien is niet rood op de onaangeraakte
basis; welke en waarom staat hieronder bij plek 4.

| plek | op de basis gemeten | fix |
|---|---|---|
| 1 `MeervoudMatcher.vindAttribuut` | `bergingsaandelen` bond niets; het meervoud bleef als `doelAttribuut` op de wire — **WF-034 FAIL** | `ot.keten()`, exact-enkelvoud via `attribuutNaam`, `(mv: …)` bij de declarant, ambiguïteitseis ketenbreed |
| 2 `KenmerkMatcher.vind` | een geërfd kenmerk bond in geen van de elf aanroepplaatsen; de voorwaarde werd `Onopgelost` — **WF-044 bij evaluatie** | idem, exacte basisvorm via `kenmerkNaam`; binding en §3.5-soort zien nu dezelfde declarant |
| 3 objectcreatie-kenmerktak | **frontend-ERROR** `'betwist' is geen attribuut of kenmerk van Hoofdberger` | `nieuwType.kenmerkNaam(naam)`, symmetrisch met de attribuut-tak ernaast |
| 4 temporaliteitsfamilie | **WF-055 FAIL** ×2 en drie verzonnen ERRORs, zie hieronder | vijf nieuwe keten-accessors; alle twaalf lookups lopen erdoorheen |
| 5 `ExpressieBouwer.dimensieCel` | een cellezing van een geërfd gedimensioneerd attribuut bleef `Onopgelost` | `ot.gedimensioneerdeLeden()` |
| 6 declaratiespelling in de intervalvouw | **de enige stille**: de GESCHREVEN spelling op de wire, 0 diagnostics, **verifier PASS** | `ot.attribuutNaam(kop)` |

**Waar de schade zit, tegen mijn eerste lezing in.** Ik schreef dit verslag
eerst alsof plek 4 stille verkeerde getallen produceerde. Nagemeten op de basis
klopt dat niet, en het onderscheid doet ertoe, want het bepaalt waarvóór de
reparatie nodig is. De wire is goed omheind:

- `ExpressieBouwer` zet bij een niet-oplosbare gedurende-conditie ALTIJD een
  `Onopgelost` plus een WARNING — `filter: null` is voor die vorm onbereikbaar
  — en de verifier weigert die: `WF-055 FAIL /resolvedRegels/0/waarde/filter`.
  De klem verdween dus niet stil, ze degradeerde tot een weigering.
- De §8.4-**ontsnapping is echt**, maar hij bereikt geen uitvoerder: WF-055
  laat in een ALLE-composite alleen kenmerkchecks toe, dus basis én kop worden
  geweigerd — met een andere reden. De ontsnapping zit in de resolverlaag, niet
  in de keten die eruit komt.
- Een `Onopgelost` die bij evaluatie wordt bereikt faalt met **WF-044**. Een
  regel die niet bindt vuurt dus niet "stil nooit"; hij neemt het document mee.
- Drie plekken geven een ronduit verzonnen frontend-ERROR: `totaal van vereist
  een eenheid per tijdseenheid (§4.20 Tabel 30)` en tweemaal `de check op
  volledige periode … kan alleen op een tijdsafhankelijk onderwerp worden
  toegepast (§8.2)`.

Van de zes plekken faalt de basis er dus op **vijf LUID** (WF-034 ×2, WF-055
×2, WF-044, één ERROR, plus drie valse ERRORs). De rechtvaardiging van deze
kaart is daarmee: een correct document werd onbruikbaar geweigerd — niet: er
gingen stil getallen fout. **Behalve op plek 6.** Die emitteert de geschreven
spelling van een geërfd attribuut (`"Betaalde belasting"` waar `betaalde
belasting` is gedeclareerd), met nul diagnostics, en de verifier zegt PASS:
geen enkele WF-regel houdt een `AttribuutRef`-naam tegen de declaraties. Dat is
de enige werkelijk stille fout van de zes, en ze is nu de zesde getuige.

**De vijf ongetuigde lookups binnen plek 4 hebben nu elk hun eigen test.**
`Tijdrekening.periodeGrensLowerbaar` (gedeeld door BEIDE Drools-takken, dus
één fout antwoord is twee uitvoerders tegelijk) staat als directe unittest in
`TijdrekeningTest`; `telTemporeel`, de genavigeerde tijdsevenredig-basis, de
genavigeerde gedurende-kenmerkklem en `setLezing` als brontests. Vier ervan
zijn rood op de basis. `telTemporeel` niet, en de reden staat bij de test: daar
stopt plek 1 de zin één stap eerder, zodat de telling nooit wordt bereikt —
plek 1 repareren is wat de telling dragend maakt. Die test pint daarom beide
richtingen tegelijk: met één geërfde tijdlijncomponent moet de uniciteit
RESOLVEN, met twee moet ze WEIGEREN; een ketenblinde telling zou beide
toelaten.

**Generiek boven speciaal.** De drie bestaande accessors droegen elk hun eigen
kopie van dezelfde ketenlus; er is nu één `opKeten`, en de vijf nieuwe
(`granulariteitVan`, `tijdlijnAantalVan`, `kenmerkGranulariteitVan`,
`kenmerkTijdlijnAantalVan`, plus `gedimensioneerdeLeden`, `attribuutNaam` en
`kenmerkNaam`) zijn er varianten van. `heeftLid` liep al ketenbewust maar met
een derde kopie van de lus; die is nu `attribuut` plus `kenmerkNaam`.

**De zesde plek, meegerepareerd en achteraf de belangrijkste.**
`ExpressieBouwer` r.742 zocht de GEDECLAREERDE spelling van een gedimensioneerd
attribuut in de eigen attribuutkaart, twee regels nadat `dimensiesVan` de keten
al had gelezen. Bij een geërfd attribuut viel hij terug op de GESCHREVEN vorm —
dezelfde fout als plek 1, via de andere deur, maar met een heel ander gevolg:
plek 1 zet de naam op een positie die WF-034 kruist en wordt geweigerd, plek 6
zet hem in een `AttribuutRef`, en die kruist geen enkele regel. Gemeten op de
basis, met de lens-probe:

```
attribuut-namen in de waarde: ['Betaalde belasting']
diagnostics: []
verifier: PASS
```

Op de kop: `['betaalde belasting']`, de gedeclareerde spelling. Dit is de enige
van de zes die de omheining passeert, en tegelijk het bewijs dat die omheining
een gat heeft — een eigen vraag voor de spec-kant, hieronder als vervolgwerk D.

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

> De dialect-frontend liep bij de kernel op zes plekken achter op zijn eigen
> model: `MeervoudMatcher`, `KenmerkMatcher`, de objectcreatie-kenmerktak, de
> hele temporaliteitsfamilie (die geen keten-accessor hád), `dimensieCel` en de
> declaratiespelling in de intervalvouw lazen de declaratiekaarten van het eigen
> type, zodat een lid van een voorouder niet bond — en bij de temporele facetten
> niet ontbrak maar VERKEERD werd beoordeeld. De schade was op vijf van de zes
> LUID: de wire is omheind, dus een tijdlijn die als scalair doorgaat degradeert
> haar §7.1-klem tot een `Onopgelost` die WF-055 weigert, een niet-bindend
> kenmerk faalt bij evaluatie met WF-044, een niet-gedeclareerd meervoud kruist
> WF-034, en drie plekken geven een verzonnen §4.20- of §8.2-fout — telkens een
> correct document dat onbruikbaar wordt geweigerd, niet een getal dat stil
> fout gaat. De ENE uitzondering is de zesde plek: de geschreven spelling van
> een geërfd attribuut in een `AttribuutRef` gaat verifier-schoon en zonder
> diagnostics de wire op, want geen enkele WF-regel houdt een `AttribuutRef`-naam
> tegen de declaraties. Alle zes hersteld 2026-09-12; getuige
> `golden/jutterswet-bergingsdeling`, de brontweeling van
> `golden/jutterswet-hoofdberging`, die de vorm pas nu uit een `bron.rgs` laat
> komen. Nog open, met reden: die ongekruiste `AttribuutRef`-naam zelf, de
> §9.9-ankerfamilie (`attribuutTijdlijnNaam`), die op de gedeclareerde
> subjectnaam sleutelt en als geheel verbreed wordt of niet, en de
> Drools-backend, die zijn facetkaarten nog per gedeclareerd type leest en de
> combinatie subtype × temporeel/gedimensioneerd daarom luid weigert in de
> loader.

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

*D. Een `AttribuutRef`-naam wordt door geen enkele WF-regel tegen de
declaraties gehouden.* Dat is waarom plek 6 als enige stil kon falen: WF-034
kruist de naam-gesleutelde velden van verdeling, objectcreatie, uniciteit en
consistentie, maar de lidnaam ín een expressie kruist niets. Elke emitterfout
die daar een naam neerzet die geen declaratie draagt, komt er verifier-schoon
door. Een spec-vraag, buiten deze may-touch: verdient de expressiewandeling
haar eigen kruising (een verbreding van WF-034, of een eigen nummer)? De
frontend is nu op alle zes plekken dicht, maar de omheining is dat niet.

**Eerlijke afwijkingen.** (a) De kaart vroeg per plek één JUnit-getuige; plek 4
kreeg er negen, omdat de familie twaalf lookups telt en één test er maar één
raakt. (b) De kaart noemde een zesde plek als mogelijkheid; ik vond er drie, en
heb er één gerepareerd (dezelfde klasse, en achteraf de enige stille van de
zes), één beargumenteerd rauw gelaten met de reden ter plekke (§9.9) en één
alleen gemeld (C). (c) De geërfd-temporele golden is gebouwd en gemeten maar
niet geland — de reden staat hierboven en is geen grootte maar een
may-touch-grens. (d) `kern/Tijdrekening.java` en `model/RegelSpraakModel.java`
worden meegecompileerd door `backends/drools`; hun gedrag verandert daar dus
mee. Bewust en gewenst (het is één en dezelfde ketenvraag), en gate 12 en 13
staan stil: geen corpusgolden combineert vandaag een supertype met een
temporeel of gedimensioneerd lid — de loader weigert die combinatie juist.
(e) Mijn eerste versie van dit verslag beschreef plek 4 als een stille
rekenfout. Dat is nagemeten onjuist en hierboven rechtgezet: alleen plek 6
faalt stil, de rest faalt luid, en de rechtvaardiging van de reparatie is
daarmee een andere — een correct document dat geweigerd wordt in plaats van een
verkeerd antwoord dat doorglipt. De onjuiste formulering stond ook in het
registervoorstel, dus in tekst die voor een beschermd pad bestemd was; ze is
daar vervangen.
