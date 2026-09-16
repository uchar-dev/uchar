# typerange-dekking: de navondsten van de typerange-kaart krijgen hun getuigen

Aanleiding: `backlog/done/20260912-typerange-sluiting.md` boekte drie
lensronde-navondsten als vervolgwerk; de maintainer wil ze nu uitgevoerd.
Alle drie zijn dekkingsgaten, geen normveranderingen: de reparaties zelf
zijn per probe bevestigd geland, maar geen corpus-artefact of JUnit-getuige
pint ze.

1. **De AANTAL-arm** komt sinds de typerange-kaart uit de frontend zonder
   `bronRol` (type-paar-afleiding; de meerduidige variant valt luid op
   WF-034+WF-048) maar heeft geen golden en geen JUnit-getuige.
2. **De bronPad-tak, clingo's MIN/MAX-arm en het temporele
   vertaaltijd-hek** zijn gerepareerd en per probe bevestigd, maar geen
   corpus-golden dekt ze. Drools weigert bronPad per vorm, dus een
   driewegspeler kan daar niet — gate 5+6-dekking (ref ≡ clingo) kan wel.

## Acceptatie

- (a) Een JUnit-getuige in `frontends/regelspraak` voor de AANTAL-arm
  zonder bronRol (de type-paar-afleiding produceert de vouw), plus een
  getuige dat de meerduidige variant luid op WF-034+WF-048 valt — corpus-
  golden mét cases als de vorm het hele vierkant door kan, anders de
  JUnit-weg met de reden in het Result.
- (b) Nieuwe corpus-goldens (Jutterswet-domein, rechtenvrij, `bron.rgs`
  waar de frontend de vorm draagt) die dekken: de bronPad-tak, clingo's
  MIN- en MAX-arm, en het temporele vertaaltijd-hek. Elke verwachting
  HANDBEREKEND vóór het draaien, met de berekening in het Result. Vormen
  die een executor per vorm weigert, verschijnen reden-dragend in diens
  weigeringskanaal — nooit stil.
- (c) Alle veertien gates groen (`bash scripts/check-X.sh`, OCaml na
  `eval $(opam env)`). Gate 12: nieuwe goldens die spelen verhogen
  `EXPECTED_PLAYED` (gelijkheid), niet-spelende komen bij naam met reden
  in de kanaallijst — die scriptwijziging hoort bij deze kaart en valt
  onder het mergemandaat na de lensronde.
- (d) Geen wijziging aan bestaande goldens, negatiefs of tests; alles
  additief. `spec/CONSTRUCTS.md` en `spec/PLAN.md` NIET aanraken; een
  eventueel registervoorstel als letterlijke tekst in het Result.

## Result

**Alle drie de navondsten hebben nu een artefact, en het bouwen ervan legde
TWEE voorkomens van dezelfde defectklasse bloot, die in dezelfde ronde zijn
gedicht.** Vijf nieuwe goldens, één nieuw negatief, één nieuwe JUnit-klasse,
twee regels gewijzigd in `backends/clingo` (één per voorkomen). Alle veertien
gates groen.

*Na de lensronde is er één fixronde geweest; die bevestigde alle veertien
gates, alle vier de toen bestaande goldens en alle drie de rood-eerst-metingen
exact, en voegde vier punten toe: het tweede voorkomen van de defectklasse met
zijn eigen golden (`muntweging`), twee extra JUnit-getuigen, een te sterke
nieuwheidsclaim aangescherpt, en een stale commentaarzin bijgewerkt. Alles
hieronder is de eindstand.*

### Wat er landde

*(1) De AANTAL-arm zonder bronRol.*

- `frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/TelbereikTest.java`
  — acht tests, nieuwe klasse, geen bestaande test aangeraakt. Ze pinnen: het
  geschreven typemeervoud bindt het objecttype; de vouw gaat bindingloos de
  wire op (geen `bronRol`, geen `bronPad`, geen `Onopgelost`); de geschreven
  ROLnaam bindt wél, op hetzelfde model; de meerduidige variant gokt géén
  relatie; twee grenzen van de opzoeking — een gedeeld typemeervoud bindt geen
  van beide, een woord dat niets benoemt levert geen elementtype; en de twee
  flanken die de fixronde erbij zette — een woord dat TEGELIJK objecttype- én
  rolmeervoud is bindt de RELATIE (de opzoeking is een fallback en de volgorde
  stond nergens vast), en een onverbonden typepaar levert dezelfde opgeloste,
  bindingloze vouw als het meerduidige geval.
- `corpus/golden/jutterswet-muntpeiling` (bron.rgs + case + NOTE) — de vorm
  kan het hele vierkant door, dus het is een golden geworden en niet alleen
  de JUnit-weg. Het document schrijft DEZELFDE telling twee keer: bindingloos
  (`het aantal Munten van de Jutter`) en op de gedeclareerde rolnaam
  (`het aantal gevonden munten van de Jutter`), zodat de type-paar-afleiding
  tegen de binding wordt gecontroleerd en niet tegen zichzelf. Speelt in ref,
  clingo en Drools.
- `corpus/negatief/wf048-telling-meerduidig` (ir.json + NOTE) — dezelfde bron
  met een tweede `Jutter`–`Munt`-feittype, door de frontend geëmitteerd. De
  verifier weigert hem op WF-034 (`'Munten' is neither a member of 'Jutter'
  nor a declared role, and the type pair 'Jutter'/'Munt' does not derive a
  unique connecting role (2 candidates)`) én WF-048 (`bindingless instance
  fold: 2 fact types relate 'Jutter' to 'Munt'`). De directory declareert
  WF-048. Precies wat dat toevoegt, na de lens aangescherpt: de arm was niet
  ONgeraakt — `wf034-ambigu-typenpaar` trapt hem ook, en zegt dat — maar gate 3
  controleert alleen de regel die een directorynaam declareert, dus een
  incidentele overtreding is een regel in een rapport en geen gepinde getuige;
  ze kan verdwijnen zonder dat een gate iets merkt. Dit is de eerste case die
  WF-048's `site:"instance fold"`-arm DECLAREERT.

*(2) De bronPad-tak, de MIN/MAX-arm en het temporele vertaaltijd-hek.* Alle
drie zijn armen van dezelfde uitsluitingsfilter (ch. 2.2), dus alle drie de
goldens zijn `jutterswet-munttelling` met de vouw op een andere arm gezet.

- `corpus/golden/jutterswet-muntpiek` — MINIMUM en MAXIMUM met
  `elementObjecttype: Munt` over een rol die `Vondst` vult. Handgeschreven
  IR (zie afwijking (b)), cases, NOTE. Speelt in alle drie.
- `corpus/golden/jutterswet-muntketen` — een tweestaps `bronPad`
  (Jutter → Boot → Vondst) met dezelfde versmalling, in SOM én MINIMUM, mét
  een diamant. Tweewegsgetuige: Drools weigert `Functie.bronPad` per vorm en
  de golden staat reden-dragend in gate 12's kanaallijst.
- `corpus/golden/jutterswet-muntbewaring` — de per-element temporele vouw met
  een geërfd maandelijks lid, en twee gewone `Vondst`-instanties met een
  asOfDate-emmerwaarde ernaast. Speelt in alle drie.

*(3, uit de fixronde) Het tweede voorkomen van de defectklasse.*

- `corpus/golden/jutterswet-muntweging` (bron.rgs + case + NOTE) — een schrijf-
  regel wiens DOELSLEUF op de voorouder is gedeclareerd (`Munt ⊑ Vondst`,
  `geborgen gewicht` staat op `Vondst`, het regelonderwerp is `Munt`) met een
  `t`-literaal opgeteld bij een `kg`-grondslag. De eerste subtype-golden die de
  overerving op de SCHRIJFpositie zet in plaats van op de elementverzameling of
  het regelonderwerp. Speelt in alle drie.

### De handberekeningen, elk vóór het draaien

Volledig uitgeschreven in de NOTE.md van elke golden; hier de kern.

**muntpeiling.** De telling loopt over de instanties die de relatie bereikt
en waarvan het type `Munt`-of-lager is. J1 → M1, M2, M3 = **3**; J2 → geen
links = **0**; J3 → M4 = **1**. Beide regels tellen dezelfde relatie, dus
beide kolommen moeten per jutter gelijk zijn. Gemeten: ref PASS (7),
clingo PASS (7), Drools PLAYED 1, noop weerlegd op zes verwachtingen.

**muntpiek.** De extremen lopen over dezelfde populatie en nemen min/max van
de waarden die de leden dragen.

| jutter | gelinkt | daarvan `Munt` | waarden | laagste | hoogste |
|---|---|---|---|---|---|
| J1 | V1 1,00 · M1 7,00 · M2 5,00 · V2 9,00 | M1, M2 | 7,00 · 5,00 | **5,00** | **7,00** |
| J2 | V3 4,00 | geen | — | **leeg** | **leeg** |
| J3 | M3 zonder gewicht | M3 | — | **leeg** | **leeg** |
| J4 | M4 3,00 | M4 | 3,00 | **3,00** | **3,00** |

Gemeten: ref PASS (10), clingo PASS (10), Drools PLAYED 1, noop weerlegd.

**muntketen.** Het bereik is de platte unie over het pad, ontdubbeld per
instantie; de bijdragers zijn daarvan de `Munt`-of-lagere.

| jutter | boten | bereikt | daarvan `Munt` | laagste | totaal |
|---|---|---|---|---|---|
| J1 | B1, B2 | V1 1,00 · M1 7,00 · M2 5,00 · V2 9,00 | M1, M2 | **5,00** | 7,00 + 5,00 = **12,00** |
| J2 | B3 | V3 4,00 | geen | **leeg** | **0** (leegwaarde) |
| J3 | B4, B5 | M3 3,00 (via beide boten) · V4 2,00 | M3 | **3,00** | **3,00** |

Gemeten: ref PASS (7), clingo PASS (7), noop weerlegd, Drools EXCLUDE met de
`Functie.bronPad`-reden.

**muntbewaring.** Sessiedatum 2026-03-15. Een segment draagt zijn waarde maal
de HELE maanden van zijn eigen span bij; die ledentotalen tellen over de set
op. J1: M1 = 12 €/mnd × 3 mnd (jan, feb, mrt) = 36 €, M2 = 7 × 1 + 5 × 1 =
12 €, samen **48 €** (V1 draagt een emmerwaarde 99 en is geen lid). J2: alleen
V2 met emmerwaarde 50, populatie leeg, TOTAAL is onvoorwaardelijk **0 €**.
J3: M3 is lid en volledig AFWEZIG, draagt niets bij, **0 €**. De 48 is
opzettelijk hetzelfde getal als `jutterswet-tempovouw`, zodat het verschil
tussen de twee goldens de versmalling is en verder niets. Gemeten: ref PASS
(4), clingo PASS (4) na de fix hieronder, Drools PLAYED 1, noop weerlegd.

**muntweging.** `1 t = 1000 kg` volgens de gedeclareerde factor, en het doel is
`kg` — de fijnste basiseenheid van het stelsel `Gewicht` — dus elke som wordt
in `kg` gedaan nadat de literaal precies één keer is herschreven.

| instantie | type | brutogewicht | `aangegeven gewicht` (regel op Vondst) | `geborgen gewicht` (regel op Munt) |
|---|---|---|---|---|
| V1 | Vondst | 3,00 kg | 3,00 + 1000,00 = **1003,00** | **leeg** |
| M1 | Munt | 2,00 kg | 2,00 + 1000,00 = **1002,00** | 2,00 + 1000,00 = **1002,00** |
| M2 | Munt | 0,50 kg | 0,50 + 1000,00 = **1000,50** | 0,50 + 1000,00 = **1000,50** |

V1 draagt de insluitingsrichting en haar tegenhanger op één rij: de
voorouderregel vuurt voor hem, de SUBTYPEregel niet, dus `geborgen gewicht`
blijft leeg op een gewone vondst — een regelonderwerp wordt niet omhoog
verbreed. M1 en M2 zijn de kruiscontrole: de twee kolommen dragen per
constructie hetzelfde getal, dus een herschrijving die maar één pad raakt komt
binnen één instantie aan het licht in plaats van als een getal dat niemand kan
narekenen. M2's 0,50 houdt de tweede decimaal dragend: wie `1 t` als een hele
1000 herschrijft en daarna de decimalen van de andere operand laat vallen,
antwoordt daar 1000,00 en is op M1 nog steeds goed. Gemeten: ref PASS (8),
clingo PASS (8) na de fix, Drools PLAYED 1, noop weerlegd op vijf
verwachtingen.

### Rood-eerst, per reparatie gemeten

De drie reparaties waren al geland, dus rood-eerst kon alleen door de
reparatie tijdelijk terug te draaien. Dat is per reparatie gedaan en daarna
teruggezet; geen van die patches staat in de geschiedenis.

1. **De populatie-opzoeking van de telling.** `typeViaMeervoud` uit
   `aantalElement` gehaald: `het aantal Munten van de Jutter` emitteert dan
   `"elementObjecttype": null` en de verifier weigert het document
   (`WF-034 … 'Munten' is neither an attribute nor a characteristic of
   'Jutter'`). Met de reparatie: `"elementObjecttype": "Munt"`, bindingloos,
   verifier PASS.
2. **De uitsluitingsfilter op de MIN/MAX-arm en op het pad.** Het
   `isa`-atoom uit clingo's `source_join` en het `List.filter` uit ref's
   `fold_members` verwijderd. `muntpiek` antwoordt dan in BEIDE uitvoerders
   J1 = 1,00 / 9,00 in plaats van 5,00 / 7,00 en J2 = 4,00 / 4,00 in plaats
   van leeg — precies de ongefilterde getallen die met de hand waren
   uitgerekend.
3. **Het temporele vertaaltijd-hek.** De ledenwandeling van het hek terug op
   het ruwe bereik gezet (`Eval.linked` / `Eval.linked_path` in plaats van
   `Eval.fold_members`). `muntbewaring` faalt dan met
   `TRANSLATION FAULT: the per-element temporal fold of "bewaartarief" over
   "V2": the case supplies an asOfDate-bucket scalar …` — een weigering bij
   naam op een instantie die de vouw niet vouwt.

4. **Het schrijfdoel van de eenheidspas** (fixronde). Hier kon rood-eerst
   echt: de golden is eerst gebouwd en gemeten, daarna de fix. Vóór de fix
   antwoordt ref alle acht verwachtingen (1002,00 incluis) en geeft Drools
   PLAYED 1, terwijl clingo `TRANSLATION FAULT: non-identical units under an
   additive join are not translated` zegt. Na de fix: clingo PASS (8),
   driewegspeler.

### De eigen vondst: een geërfd tijdlijnlid las "niet temporeel"

`muntbewaring` vertaalde aanvankelijk niet, en niet om het hek:
`clingo: TRANSLATION FAULT: non-identical units under the write to
"bewaarlast"`, terwijl ref 48 antwoordde en Drools PLAYED 1 gaf. Oorzaak:
`backends/clingo/lib/translate.ml`, `attr_temporal_decl` — de
temporaliteitstoets waar de §4.20-noemerstrip aan hangt — las de RUWE
declaratie (`ctx.doc.objecttypen`) waar elke andere lezing in die vertaler
door `objecttype` (`effective_ot`, ketensluitend) gaat. Verankerd op `Munt`,
dat zelf geen tijdlijn declareert, antwoordde hij "niet temporeel", bleef de
strip uit en weigerde het schrijfhek `EUR/mnd` in een `EUR`-doel.

Dat is dezelfde correctie die de facet-kaart in `backends/drools` maakte (de
facet-zijkaarten per GEDECLAREERD type gelezen). Alleen de FOUT was van deze
helper — het commentaar erboven zegt waarom hij niet mag falen — de keten
nooit; hij leest nu `effective_ot` en behoudt zijn faalvrije gedrag. Eén
regel plus commentaar; geen andere ruwe lezing staat er nog in
(`grep ctx.doc.objecttypen` geeft nog alleen de `isa_sub`-emissie, die
terecht de ruwe declaraties afloopt).

Geen aparte unittest in `backends/clingo/test` toegevoegd: de golden is
corpus-getuige en loopt door gates 5, 6 en 12, en de vuistregel "geen
testbestanden aanraken zonder te vragen" staat boven de gewoonte. De
VURENDE richting van het hek (een echt lid met een emmerwaarde) houdt haar
bestaande getuige in diezelfde suite.

### Het tweede voorkomen: het schrijfdoel van de eenheidspas

De lensronde vond hetzelfde defect één bestand verderop, en mijn eigen
`grep` had het gemist omdat ik alleen `translate.ml` had afgezocht.
`backends/clingo/lib/normalise.ml`, `target_unit`: de eenheids-herstelpas —
die vóór het additieve schrijfhek elke literaal van het stelsel van het doel
herschrijft naar de fijnste basiseenheid, zodat het hek één identiek
eenheidsrecord ziet in plaats van twee omrekenbare — zocht de eenheid van het
SCHRIJFDOEL in de ruwe declaratiekaart. Noemt het doel een subtype waarvan het
eenheidsdragende lid op de voorouder staat, dan is die sleuf daar afwezig, dus
antwoordde de pas "geen eenheid", liet de regel onherschreven, en weigerde het
hek daarna. Zelfde eenregelige correctie, zelfde commentaarpatroon.

*De correctie is over de rest van het corpus aantoonbaar een no-op, en dat is
gemeten en niet aangenomen.* Waar geen enkel supertype gedeclareerd is, is
`effective_ot` per definitie de ruwe declaratie. Van de 218 corpusdocumenten
(golden + rood) dragen er **17** een supertype-rand; over die 17 is per
schrijfregel het antwoord van `target_unit` vóór en na uitgerekend, en er is
**precies één** regel waar het verandert: `geborgen gewicht van een munt` in
de nieuwe golden zelf. Gates 5, 6, 8, 11 en 13 draaien daarnaast de volle
herspeling en bevestigen de waarden (481 cases exact, 29 toka-cases, 26 rode
cases, 96 herspelingen onder de annotatiedraai).

### De veertien gates, eindstand (basislijn → kop)

| gate | kerncijfer |
|---|---|
| 1 self-contained | PASS |
| 2 ir-schema | 213 → **218** bestanden valideren en round-trippen (vloer 192) |
| 3 verifier | 213 → **218** golden clean; 390 → **391** negatief op hun eigen rule (vloer 350) |
| 4 case-schema | 502 → **507** casebestanden |
| 5 oracle | ref-suite 620 worked examples (vloer 489); 476 → **481** cases exact herspeeld (vloer 427) |
| 6 clingo | backendsuite 188 checks (vloer 83); 476 → **481** cases ref = clingo |
| 7 noop | 440 → **445** cases weerleggen een niets-doende uitvoerder; 36 survivors ongewijzigd |
| 8 rood | 26 rode cases (vloer 26), ongewijzigd |
| 9 catala | 201 checks (vloer 201); 12 catala-goldens byte-identiek — ongewijzigd |
| 10 regelspraak | JUnit 255 → **263** tests (vloer 173); 47 → **49** rgs-goldens byte-identiek (vloer 38) |
| 11 toka-parity | 29 cases, 29 Drools PLAYED, 0 divergenties — ongewijzigd |
| 12 drools | played 138 → **142**; auto-excluded 46 → **47**; kanaalvloer 49 → **50**; value-divergence 1; no-case 2; JUnit 349 tests (vloer 239) |
| 13 reftype-derivation | 96 → **105** getwiste annotaties over 19 → **21** goldens (vloer 74); 88 → **96** herspelingen, 16 → **18** door de DRL-tekst-tak (vloer 13); 0 survivors; 0 buiten de twist-tabel |
| 14 flogic | 81 checks (vloer 81); 1 flogic-golden byte-identiek |

### De gate-12-scriptdelta

Twee soorten bewerking in `scripts/check-drools.sh`, in de bestaande stijl,
elk gedateerd en met reden:

1. `EXPECTED_PLAYED` 138 → 142, in vier afzonderlijk benoemde stappen
   (138 → 139 `muntpeiling`, 139 → 140 `muntpiek`, 140 → 141
   `muntbewaring`, 141 → 142 `muntweging`), elk met de reden waarom die
   golden een SPELER moet zijn en niet een weigering.
2. Eén regel erbij in `EXPECTED_CHANNEL`: `jutterswet-muntketen|EXCLUDE|
   loader Refusal: Functie.bronPad …`, die zegt dat dit de derde golden van
   die vorm is en de eerste met een versmalde landing, en die erbij vermeldt
   waar de eenstaps-versmalling wél speelt.
3. Niets anders. Geen ander script onder `scripts/` aangeraakt; geen gate
   eiste het.

### Afwijkingen en twijfels, eerlijk

(a) **Twee bestanden buiten de letterlijke opdracht gewijzigd**:
`backends/clingo/lib/translate.ml` en `backends/clingo/lib/normalise.ml`, de
twee ketensluitingen hierboven. De kaart vroeg om getuigen, niet om
reparaties. Zonder de eerste was `muntbewaring` niet te landen en zonder de
tweede `muntweging`, en beide weigeringen waren echte gaten: een
verifier-schoon document dat twee uitvoerders beantwoorden en de derde luid
weigert. Het alternatief — de goldens zonder eenheden schrijven — had de
gaten laten staan en de getuigen zwakker gemaakt. Elke wijziging staat in
een eigen commit met haar meting erbij. Twijfel die ik eerlijk noteer: na
twee vondsten in dezelfde klasse is de vraag of er een DERDE ruwe lezing is
niet met een `grep` te beantwoorden — mijn eigen grep miste de tweede. Wat
ik nu wel heb nagelopen is de hele backend: `grep -rn objecttypen
backends/clingo/lib backends/clingo/bin` geeft na deze ronde nog exact TWEE
treffers, beide op regel 7075-7076 van `translate.ml`, en beide terecht — dat
is de `isa_sub`-emissie, die juist de RUWE supertype-randen moet aflopen om
de ketensluiting in de solver af te leiden. Elke andere declaratielezing in
deze backend gaat door `effective_ot`. Een systematische toets over de ándere
backends en de verifier is niet gedaan en is een eigen kaart.

(b) **Drie van de vijf goldens zijn handgeschreven IR zonder brontweeling**,
om precies de reden die de vorige kaart als vervolgwerk A vastlegde: de
vouwBRON van een LEDENvouw eist dat de geschreven naam een ROL noemt, dus
"alle munten van de Jutter" is niet schrijfbaar zolang de rol `vondst` heet.
Die weigering is bewust en van een eerdere ronde; ik heb haar niet verzwakt.
De TELvorm heeft die eis niet, en dat is precies waarom `muntpeiling` wél
een `bron.rgs` heeft.

(c) **De WF-034+WF-048-weigering is over twee artefacten verdeeld**, niet één.
De JUnit-getuige pint de frontendhelft (er komt geen gegokte binding op de
wire), het negatief pint het verdict. Eén artefact voor beide bestaat niet:
de JUnit-suite draait de OCaml-verifier niet, en een negatief zegt niets over
wat de frontend emitteert. Het negatief is daarom uit precies dezelfde bron
geëmitteerd, zodat de twee op hetzelfde document slaan.

(d) **Het negatief declareert WF-034 niet, alleen WF-048.** Een directory
declareert één rule, en `wf034-ambigu-typenpaar` dekt WF-034's
type-paar-fallback al (op een gesynthetiseerd meervoud). De NOTE noemt beide
regels en de reden voor de keuze. Een tweede directory met hetzelfde document
zou dubbel materiaal zijn.

(e) **Het residu van het temporele hek is niet aangeraakt.** Het commentaar
in de vertaler zegt dat een door de `die/dat`-FILTER weggevallen lid met een
emmerwaarde nog steeds wordt geweigerd waar ref antwoordt. Dat staat er nog,
luid en zonder corpuscase; het is een filtervraag en niet de
elementtype-vraag van deze kaart.

(f) **`muntpiek` heeft geen brontweeling én zou er wel één kunnen krijgen als
vervolgwerk A ooit wordt beslist.** Ik heb dat niet opgerekt.

(g) **De nul-kandidaatflank blijft een JUnit-getuige en wordt geen tweede
negatief.** Gemeten gedraagt hij zich zoals verwacht: `WF-034 … does not
derive a unique connecting role (0 candidates)` naast `WF-048 … bindingless
instance fold: no fact type relates 'Jutter' to 'Munt'`. Een tweede
negatiefdirectory zou dezelfde twee clausules op een andere kandidatentelling
pinnen, terwijl de arm die zij declareert al door `wf048-telling-meerduidig`
wordt gedeclareerd. Wat géén getuige had is de FRONTENDhelft — dat een
onverbonden paar een opgeloste, bindingloze vouw oplevert en niet een
markering of een stilte — en dat is wat de JUnit-test pint. De verifiertekst
staat letterlijk in de javadoc, met de meetdatum, zodat een wijziging van die
boodschap zichtbaar wordt bij het lezen en niet alleen bij het draaien.

(h) **De fixronde raakte twee zinnen die door code onwaar waren geworden.**
Het hek-commentaar in `translate.ml` noemde nog de bereikwandeling
(`[linked]`) die het sinds de vorige ronde niet meer doet, en de NOTE van het
negatief claimde een arm die "no negatief case reached before" terwijl
`wf034-ambigu-typenpaar` hem incidenteel al raakt. Beide zijn bijgewerkt naar
wat er werkelijk staat; de tweede claim is teruggebracht tot het verschil dat
gate 3 werkelijk maakt (gedeclareerd versus incidenteel).

### Registervoorstel (letterlijke tekst, hoofdsessie landt het)

`spec/CONSTRUCTS.md` rij 39, aan de bestaande cel toe te voegen:

> De dekking van die uitsluitingsrichting is 2026-09-16 rondgemaakt: de
> reparatie zat op één plek (`fold_members` in ref, het sluitende
> `isa`-atoom van `source_join` in clingo), maar bereikte armen die geen
> enkele golden uitvoerde. `golden/jutterswet-muntpiek` zet haar op MINIMUM
> en MAXIMUM — waar een ten onrechte meegetelde instantie het antwoord niet
> verschuift maar vervangt — en `golden/jutterswet-muntketen` op een
> meerstaps `bronPad`, waar ch. 3.4b alleen de LANDING versmalt en niet de
> tussenstappen; die laatste is een tweewegsgetuige, want de Drools-backend
> weigert `bronPad` per vorm. `golden/jutterswet-muntbewaring` pint de derde
> plek: het vertaaltijd-hek van de per-element temporele vouw, dat de LEDEN
> afloopt en niet het ruwe bereik, en daarom niet meer struikelt over de
> ontbrekende tijdlijn van een instantie die niet meedoet. Diezelfde golden
> legde een tweede, eigen gat bloot: de §4.20-noemerstrip van de
> ASP-backend hing aan een temporaliteitstoets die de RUWE declaratie las,
> zodat een vouw verankerd op een subtype met een GEËRFD tijdlijnlid "niet
> temporeel" antwoordde en het schrijfhek `EUR/mnd` in een `EUR`-doel
> weigerde terwijl ref en Drools beide antwoordden — dezelfde correctie die
> de facet-ronde in de Drools-backend maakte, nu op de effectieve
> declaratie (ch. 2.2). Aan de frontendkant kreeg de TELvorm haar getuigen:
> een geschreven objecttype-MEERVOUD bindt de populatie en gaat
> bindingloos de wire op, waarna de bron uit het type-paar wordt afgeleid
> (`golden/jutterswet-muntpeiling`, dat dezelfde telling ook op de
> gedeclareerde rolnaam schrijft, zodat afleiding en binding elkaar
> controleren); de meerduidige variant gokt geen relatie en valt op WF-034
> en WF-048 tegelijk (`negatief/wf048-telling-meerduidig`, de eerste getuige
> die WF-048's instance-fold-arm ook DECLAREERT — gate 3 controleert alleen
> de gedeclareerde regel, en `wf034-ambigu-typenpaar` raakte de arm al
> incidenteel). Dezelfde ronde vond het defect van de ruwe declaratielezing
> een tweede keer, op de SCHRIJFpositie: de eenheids-herstelpas van de
> ASP-backend zocht de eenheid van het schrijfdoel eveneens in de ruwe kaart,
> zodat een regel die een lid van een subtype schrijft dat op de voorouder is
> gedeclareerd onherschreven bleef en het additieve schrijfhek haar weigerde
> terwijl ref en Drools 1002,00 gaven; getuige `golden/jutterswet-muntweging`,
> de eerste subtype-golden die de overerving op de schrijfpositie zet.
