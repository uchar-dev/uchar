# IMPLICIT-TIME — rekenen en voorwaarden per periode over tijdlijnen

*Status: VOORSTEL · 2026-11-03 · werkproza, niet normatief. Fase 0 van de kaart
`backlog/doing/20261010-impliciete-tijd.md`. Dit document verandert geen spec, schema, verifier,
uitvoerder of corpus; het staat buiten elke zone van gate 1. Gemeten op main `b3feb3882`
(regelnummers van uchar op die commit; de meting in §5 draaide op dezelfde code; eerdere runs op
`95947ae0f` en `27cfd3dfa` staan in §5).*

*Bronnen, alleen met nummer en in eigen woorden aangehaald. (1) De RegelSpraak-specificatie
v2.3.0 van de Belastingdienst: het hoofddocument als "spec §…", het document met de typeringen als
"typ. §…". (2) ALEF, de EUPL-1.2-uitgave van de Belastingdienst, op `872b859`; Java-paden relatief
aan `AlefJava/alefRuntime/src/main/java/nl/belastingdienst/alef_runtime/`, MPS-modellen relatief
aan `languages/`. (3) De presentatie "Implicit Temporal Reasoning for Executable Legal Rules"
(Rules as Code Europe 2026), met dianummers, zoals de kaart haar aanhaalt. Er staat geen tekst uit
een van de drie in dit document en geen tekst uit dit document in een zone van gate 1.*

*Gelezen en gedraaid. "Gelezen" betekent: uit de broncode afgeleid. ALEF's runtime is daarnaast
gedraaid, zonder downloads: de 63 bronbestanden van `alefRuntime` met `javac` gecompileerd naar
een eigen map buiten ALEF, en een eigen proefprogramma dat de runtime aanroept zoals de vertaler van
`regelspraak.tijd` dat doet (gelezen, niet gegenereerd). Waar een ALEF-waarde hieronder "gedraaid"
heet, komt ze uit dat programma; de commando's staan in het `Result:` van de kaart.*

## 0. Hoe de besluitpunten zijn ingedeeld

Besluit van de eigenaar (2026-11-03): ook voor tijd volgt uchar ALEF waar de
RegelSpraak-specificatie zwijgt. Elk besluitpunt krijgt daarom één van drie markeringen:

- **spec spreekt** — de specificatie beslist de vraag, en noch ALEF noch de gelande semantiek van
  uchar (RUNTIME-SEMANTICS of een golden) zegt er iets anders over. Besloten: de specificatie.
- **spec zwijgt (ALEF)** — de specificatie beslist de vraag niet; de nagekeken paragrafen staan
  erbij. Besloten: de lezing van ALEF.
- **tegenspraak** — de specificatie of de gelande semantiek van uchar spreekt ALEF tegen. Dat
  blijft een **normvraag** voor de eigenaar, met een aanbeveling.

Binnen een besloten lezing blijven keuzes over hoe uchar haar uitdrukt (wire, case-formaat, codes,
de ASP-codering). Dat zijn **technische vragen**; ik beslis ze hier als **voorlopig besluit** (VB).

## 1. Overzicht

| # | Besluitpunt | Markering | Soort | Aanbeveling in één regel |
|---|---|---|---|---|
| 1 | Rekenen per periode (maal, gedeeld door, percentage van, verminderd met) | spec spreekt (§5.1.1, §5.1.4) | besloten; technisch VB 1.1–1.4 | Per dag lezen met de leeg-regels van ch. 1.2; eenheden per segment; de §9.5-melding per regel × onderwerp. |
| 2 | Het tijddomein (een scalair, een tijdsafhankelijke parameter als operand) | tegenspraak (spec §5.1.1, §5.1.4.1 en ALEF tegen RS ch. 3.4) | **normvraag; besloten door de eigenaar: de aanbeveling** | Alle tijd: een tijdlijn deelt de hele tijd in, leeg buiten haar perioden; een scalair geldt altijd. |
| 3 | Een voorwaarde op een regel met een tijdsafhankelijk doel | spec spreekt (§10.3, §8.4.1) | besloten; technisch VB 3.1–3.3 | Per periode: de regel schrijft alleen waar de voorwaarde geldt; dat veralgemeent de knip van ch. 3.10. |
| 4 | De gehele-periodekwalificator op zo'n regel | tegenspraak (RS ch. 3.9 vouwt; ALEF per emmer) | **normvraag; besloten door de eigenaar: de aanbeveling** | Per emmer op een tijdsafhankelijk doel; de vouw blijft voor een scalair doel. Geen golden verandert. |
| 4b | De lege dag in een vergelijking onder de kwalificator | tegenspraak (spec §8.2 met typ. §5.3, §5.6, en ALEF, tegen RS ch. 3.9) | **normvraag; besloten door de eigenaar: de aanbeveling** | Ch. 1.2 per dag, zoals de knip van ch. 3.6: een gatdag voldoet aan "ongelijk aan", een lege grens maakt de toets niet onwaar. Geen golden verandert. |
| 5 | Twee schrijvers van één tijdlijndoel | spec zwijgt (ALEF) | besloten; technisch VB 5.1–5.2 | Per dag: fout alleen op een dag waar beide schrijven en verschillen; RS ch. 5.7 per dag gelezen. |
| 6 | Van datum tot datum; een datumexpressie als grens | spec spreekt (§10.3, §8.4.3, §9.2) | besloten; technisch VB 6.1–6.2 | Een tijdsafhankelijk kenmerk binnen een venster schrijven; WF-111 verruimen tot elke dagprecieze datumexpressie. |
| 7 | De dagentelling: 0 of niets, een dagsoorttoets, de eenheid | tegenspraak voor 7a (typ. §4.21 en ALEF tegen RS ch. 3.8 en het anker `jutterswet-dagentelling`) | **normvraag (7a); besloten door de eigenaar: de aanbeveling**; 7b besloten (spec zwijgt), 7c besloten (spec spreekt) | 7a: 0 in een emmer zonder ware dag; 7b: een dagsoorttoets op de getelde dag toelaten; 7c: de telling draagt dagen per rastereenheid. |
| 8 | Een fijnere operand in een grover doel | spec spreekt (typ. §3, §5.1.1, §10.3) | besloten; technisch VB 8.1 | Statisch weigeren, met een nieuwe WF-regel; 8b (een fijnere basis van het tijdsevenredig deel) volgt ALEF, zonder bouwkaart. |
| 9 | Minimum, maximum, eerste en laatste over tijdlijnen | spec spreekt (§5.1.1, §5.1.4) | besloten; geen bouwkaart | Per periode over de operanden; een vorm "over de tijd van één tijdlijn" heeft geen bron en komt er niet. |
| 10 | Verschuiven en eenmalig | spec zwijgt (ALEF) | besloten; geen bouwkaart | ALEF's afronding van grenzen op het raster vastleggen; bouwen pas als een brontaal van uchar de vorm schrijft. |
| 11 | De dagtelconventie bij integratie | tegenspraak (ALEF tegen RS ch. 3.6) | **normvraag; besloten door de eigenaar: de aanbeveling** | De kalender: tussen een dageenheid en een maandeenheid de werkelijke lengte, geen gedeclareerde factor. |
| 12 | Het totaal over een gedeeltelijke eenheid | tegenspraak (ALEF tegen RS ch. 3.6 en golden `jutterswet-vast-startpunt`) | **normvraag (12a, 12b); besloten door de eigenaar: de aanbeveling**; 12c besloten (spec spreekt) | 12a: naar rato van de werkelijke lengte; 12b: de gewone operand houden als "per rastereenheid"; 12c: een afwezige tijdlijn telt 0. |
| 13 | Tijdsafhankelijke feittypen | spec zwijgt (ALEF) | besloten; geen bouwkaart | Vastleggen als later. |

De normvragen voor de eigenaar zijn dus 2, 4, 4b, 7a, 11, 12a en 12b. Bij 2, 4b, 7a en 12a staat de
specificatie (2, 4b, 7a) of haar stilte (12a) aan de kant van ALEF, en is het de gelande tekst van
uchar die afwijkt.

**Besluiten van de eigenaar (2026-11-03).** Alle zeven normvragen zijn beslist volgens de aanbeveling:
2 alle tijd; 4 per emmer op een tijdsafhankelijk doel, de vouw blijft voor een scalair doel; 4b ch. 1.2
per dag ook onder de kwalificator; 7a 0 in een emmer zonder ware dag; 11 de kalender tussen dag- en
maandeenheden, een factor tussen de twee families ill-formed; 12a naar rato van de werkelijke lengte,
als gevolgde afwijking; 12b de gewone operand houden, gelezen als "per rastereenheid", als gevolgde
afwijking. Waar hieronder "normvraag" en "aanbeveling" staat, is de aanbeveling het besluit; de
voorlopige besluiten die van een keuze afhingen, gelden voor de gekozen lezing.

## 2. Het principe: de lezing per dag

Voor elke operator en elke voorwaarde zonder eigen tijdsemantiek is de betekenis over tijdlijnen
de scalaire betekenis per dag: de leeg-regels van ch. 1.2 per dag, en de vergelijking die een regel
is (ch. 5.1) per dag. De bewerkingen met een eigen tijdsemantiek houden hun eigen definitie: het
totaal (ch. 3.6), het tijdsevenredig deel (ch. 3.7), de dagentelling (ch. 3.8), de
gehele-periodekwalificator (ch. 3.9), het venster (ch. 3.10) en, als ze er komen, verschuiven en
eenmalig (punt 10).

- **De specificatie** zegt dit zelf. Een expressie over tijdsafhankelijke gegevens wordt toegepast
  op elke periode tussen twee knips van de samengevoegde tijdlijn (spec §5.1.4); een expressie met
  een tijdsafhankelijk element is tijdsafhankelijk, met als enige uitzondering de aggregatie in de
  tijd van §7.1 (spec §5.1.1); een conditie met een tijdsafhankelijk gegeven is tijdsafhankelijk
  (spec §8.4.1); en een tijdsafhankelijke voorwaarde op een regel met een tijdsafhankelijk doel
  beperkt het resultaat tot de perioden waarin ze geldt (spec §10.3).
- **ALEF** doet het zo. De vertaler leest een binaire expressie over tijdsafhankelijke operanden
  met `Time.lift` over de scalaire afbeelding van dezelfde expressie, met een scalaire operand
  verpakt als `Always` (`regelspraak.tijd/models/regelspraak.tijd.translator.mps` r. 5990–6080,
  methode `texpressie`); `Time.lift` geeft per moment de doorsnede van de vakken en past de functie
  toe op de waarden daar (`time/Time.java` r. 287–308).
- **uchar** doet het al voor plus en min: `cut_merge` past de scalaire operator toe per deelperiode
  (`ref/lib/timeline.ml` r. 408–419), en de leeg-regel van ch. 3.4 is die van ch. 1.2 voor plus en
  min (RS r. 1231–1235). Het verschil met ALEF zit in het domein, niet in de lezing: punt 2.

## 3. De besluitpunten

Elk punt: de vraag; de lezingen (uchar nu, ALEF, de specificatie); de aanbeveling met reden; de
voorlopige besluiten; een uitgewerkt voorbeeld met absolute waarden, dat `ref/test/` letterlijk kan
overnemen (de voorgestelde testnaam staat erbij). "RS" is `spec/RUNTIME-SEMANTICS.md` op `b3feb3882`.
De voorbeelden gebruiken, waar ze over de ankercasus gaan, de getallen van §4.

### Punt 1 — Rekenen per periode

**Vraag.** Wat betekenen maal, gedeeld door (beide varianten), percentage van en verminderd met
over tijdlijnen, en welke periode noemt de §9.5-melding bij een deling door leeg?

**Lezingen.**
- *uchar nu.* Geweigerd en OPEN: RS ch. 3.4 r. 1259–1270 (alleen plus en min bestaan over
  tijdlijnen; de lifting per periode is een kandidaat) en ch. 3.12 r. 3516. Gemeten (§5,
  `p01-rekenen-per-periode`, `p01b-rekenen-zonder-parameter`): frontend en verifier laten de vorm
  door; ref weigert elk tijdsafhankelijk attribuutdoel buiten de pro rata en de dagentelling
  (`ref/lib/eval.ml` r. 3233–3240), clingo en beide Drools-takken ook.
- *ALEF.* Per moment de scalaire afbeelding (zie §2); `verminderd met` heeft een eigen tak die
  hetzelfde doet (translator r. 6180–6260). Gedraaid: `BigRational.multiply` per moment over `a` en
  `b` van het voorbeeld hieronder geeft 0 vóór 2024-04-01, 3000 op [2024-04-01, 2024-07-01) en 0
  daarna.
- *Specificatie.* Spreekt: spec §5.1.1 en §5.1.4 voor elke expressie; de rekentabellen per operator
  (typ. §4.1–§4.8) zijn die van ch. 1.2, die uchar spiegelt.

**Aanbeveling.** Aannemen: elke operator per dag met de leeg-regels van ch. 1.2. Reden: de
specificatie zegt het voor elke expressie, ALEF doet het, en het vraagt geen nieuwe leeg-regel —
ch. 1.2 heeft er al een per operator.

**Voorlopige besluiten (technisch).**
- VB 1.1 — De cut set is die van ch. 3.4 over alle tijdlijnoperanden; een scalaire operand of
  parameter zonder tijdlijn levert geen knip.
- VB 1.2 — Eenheden per ch. 2.6, per segment; het resultaat eenmaal herrekend naar het doel (zoals
  ch. 3.4 r. 1237–1239 voor plus).
- VB 1.3 — Een deling door leeg of door 0 in een periode geeft in die periode leeg en legt één
  §9.5-feit neer per regel × onderwerp × code (EF-031 of EF-005, RS ch. 5.6 en 5.7b), hoeveel
  perioden het ook raakt. De melding krijgt geen periode als parameter: het register van ch. 5.7b
  verandert niet, en een periode zou de identiteit van het feit (regel × onderwerp) breken.
- VB 1.4 — De precisiepoort van WF-023 (RS ch. 2.4 punt 4) geldt ook voor een tijdlijndoel waarvan
  de waarde zo'n berekening is, met de grens van ch. 2.3 per segment; de uitzondering van r. 495–497
  blijft alleen voor de pro rata. De fout per segment (EF-006) blijft het vangnet.

**Uitgewerkt voorbeeld** (`WV-3.4-per-dag`). `a` = 100 op [2024-01-01, 2024-07-01), `b` = 30 op
[2024-04-01, 2024-10-01), beide zonder eenheid, domein alle tijd (punt 2). Per periode:

| periode | a maal b | a verminderd met b | a gedeeld door b | 10 % van a |
|---|---|---|---|---|
| tot 2024-01-01 | 0 | leeg | 0 | 0 |
| [2024-01-01, 2024-04-01) | 0 | 100 | leeg, en één §9.5-feit EF-031 | 10 |
| [2024-04-01, 2024-07-01) | 3000 | 70 | 10/3 | 10 |
| [2024-07-01, 2024-10-01) | 0 | leeg | 0 | 0 |
| vanaf 2024-10-01 | 0 | leeg | 0 | 0 |

Na samenvoegen van gelijke buren: `a maal b` = 0 tot 2024-04-01, 3000 op [2024-04-01, 2024-07-01),
0 vanaf 2024-07-01 (gelijk aan de gedraaide ALEF-uitkomst). Met het domein "de span" (punt 2) zijn
de eerste en de laatste rij een gat.

### Punt 2 — Het tijddomein

**Vraag.** Over welke tijd loopt een tijdlijnuitkomst: de span van de tijdlijnoperanden (zoals
ch. 3.4 nu), of alle tijd (zoals ALEF)? Wat is een scalair of een tijdsafhankelijke parameter als
operand?

**Lezingen.**
- *uchar nu.* Een tijdlijn is een eindige verzameling segmenten; buiten de segmenten leest ze leeg
  (RS ch. 3.1 r. 993–1003). Het resultaat van ch. 3.4 bestaat alleen tussen de eerste en de laatste
  knip; daarbuiten is het een gat (r. 1231–1235, met het uitgewerkte voorbeeld r. 1254–1257). Een
  scalair in tijdlijnrekenen en een onvoorwaardelijk tijdsafhankelijk kenmerk zijn geweigerd omdat
  ze een tijddomein vragen (r. 1264–1270, ch. 3.11 r. 3456–3466, ch. 3.12 r. 3517). Een
  tijdsafhankelijke parameter is als tijdlijnoperand alleen bereikbaar via `TemporalRead` (ch. 3.3,
  8.3); ref en clingo weigeren hem op een scalaire positie (`backends/clingo/lib/translate.ml`
  r. 4493), en het Drools-laadprogramma weigert elk document met een tijdsafhankelijke parameter
  (gemeten: DROOLS-030, §5). Geen golden schrijft ch. 3.4 of 3.5 via een regel.
- *ALEF.* Een tijdsafhankelijke waarde deelt de hele tijd in aaneengesloten vakken in, van een open
  begin tot een open einde; leeg is een vak met waarde null (`time/ITimed.java` r. 15–20). Een
  scalair wordt `Always.of(waarde)` (r. 165–170). Gedraaid: `a + b` van punt 1 met
  `BigRational.add` per moment geeft 0 vóór 2024-01-01 en 0 vanaf 2024-10-01, op open vakken.
- *Specificatie.* Spreekt. Een expressie zonder tijdsafhankelijke gegevens geldt altijd (spec
  §5.1.1, §5.1.4). Het eerste uitgewerkte voorbeeld van §5.1.4 (§5.1.4.1) geeft de uitkomst van een
  optelling ook een waarde, 0, op de open periode vóór de eerste knip en na de laatste. Een totaal
  zonder begrenzende conditie kan onbegrensd veel waarden opleveren (§7.1, de eerste
  waarschuwing). Een kenmerktoekenning zonder voorwaarde maakt het kenmerk altijd van toepassing
  (§9.2). Een parameter kan tijdsafhankelijk zijn (§3.8) en is dan tijdsafhankelijk gegeven als
  elk ander (§5.1.1).

**Normvraag.** De specificatie en ALEF lezen alle tijd; RS ch. 3.4 leest de span. **Aanbeveling:
alle tijd.** Reden: zo staat het in §5.1.4.1, zo rekent ALEF, en het lost drie open punten van
ch. 3.12 tegelijk op (een scalair naast een tijdlijn, het onvoorwaardelijke tijdsafhankelijke
kenmerk, en eenmalig, punt 10), zonder een tweede regel voor "binnen de span". De prijs: de
tijdlijnwaarde krijgt open segmenten, het case-formaat ook, en de ASP-codering moet een open grens
dragen.

**Voorlopige besluiten (technisch, alleen als de eigenaar alle tijd kiest).**
- VB 2.1 — Een tijdlijnwaarde is een opdeling van alle tijd; een segment mag een open begin of een
  open einde hebben. `read` (ch. 3.1) blijft gelijk; een gat is een leeg segment.
- VB 2.2 — In `uchar-case-v0` mag een segment `from` of `to` weglaten (open); de laders lezen een
  ontbrekende grens als open. De verwachting van een case vergelijkt de hele opdeling, de open
  segmenten met waarde 0 inbegrepen.
- VB 2.3 — Een tijdsafhankelijke parameter is een tijdlijnoperand als elk tijdsafhankelijk attribuut
  (WF-043 laat dat al toe); de vintage-lezing van ch. 8.3 blijft de `TemporalRead` met de rekendatum,
  en de dekkingseis van ch. 8.4 blijft alleen voor die lezing.
- VB 2.4 — Het totaal (ch. 3.6) van een tijdlijn met een open segment met waarde 0 telt dat segment
  als 0; met een open segment met een andere waarde is het een fout met een nieuwe EF-code. ALEF doet
  hetzelfde: `timesDuration` vermenigvuldigt een vak met waarde 0 niet (`time/TimeLine.java`
  r. 313) en gooit `InfinitePeriodException` op de lengte van een open vak (r. 330).

**Uitgewerkt voorbeeld** (`WV-3.4-domein`). `a` als in punt 1; `5 plus a`:
- alle tijd: 5 tot 2024-01-01, 105 op [2024-01-01, 2024-07-01), 5 vanaf 2024-07-01;
- de span: 105 op [2024-01-01, 2024-07-01), daarbuiten een gat.

### Punt 3 — Een voorwaarde op een regel met een tijdsafhankelijk doel

**Vraag.** Is zo'n voorwaarde een poort voor de hele schrijfactie (zoals nu), of geldt ze per
periode, zodat de regel schrijft waar ze geldt? Wordt een tijdsafhankelijke operand in zo'n
voorwaarde dan toelaatbaar?

**Lezingen.**
- *uchar nu.* ref evalueert de voorwaarde één keer per onderwerp en schrijft de hele tijdlijn of
  niets (`ref/lib/eval.ml` r. 3266–3274). Een tijdsafhankelijk kenmerk of attribuut in de voorwaarde
  wordt buiten de collapses geweigerd (RS ch. 3.3, r. 1182–1192). Het venster van ch. 3.10 knipt al
  per periode op een tijdsafhankelijk doel (r. 3366–3412). Geen tekst in RS stelt dat een andere
  voorwaarde de hele schrijfactie poort, en geen golden pint het: van de 17 regelversies met een
  tijdsafhankelijk doel in `corpus/golden` hebben er 4 een voorwaarde, 3 een `TimeWindow` en 1 een
  scalaire vergelijking (`synth2-prorata-breed`), die per periode hetzelfde geeft. Gemeten (§5,
  `p03b`): ref weigert een tijdsafhankelijk kenmerk in de voorwaarde; **clingo weigert niet en
  schrijft een lege tijdlijn**; beide Drools-takken weigeren (DROOLS-271).
- *ALEF.* `ITimed.when` maakt de waarde leeg waar de geldigheid niet geldt (`time/ITimed.java`
  r. 113–115); `IValidity` heeft `and`, `or`, `negate`, `ifThen` en de tellende vormen, alle per
  periode (`time/IValidity.java` r. 57, 159–220). Het typesysteem weigert een scalair doel onder
  een tijdsafhankelijke voorwaarde, en een doel waarvan het raster de knips van de voorwaarde niet
  kan dragen (`regelspraak.tijd/models/regelspraak.tijd.typesystem.mps`, `check_ActieIndienVoorwaarde`
  r. 1052–1570); het laat zowel "indien" als "gedurende de tijd dat" toe voor een tijdsafhankelijke
  voorwaarde (`check_ActieIndienVoorwaarde_tijd`, r. 3784–4006).
- *Specificatie.* Spreekt: een tijdsafhankelijke conditie in het voorwaardendeel kan alleen bij een
  tijdsafhankelijk resultaat, en bepaalt het resultaat alleen voor de perioden waarin ze geldt
  (spec §10.3); het doel moet de knips van de voorwaarde kunnen dragen (§10.3, typ. §3); "gedurende
  de tijd dat" vervangt dan "indien" (§8.4.2, §10.3).

**Aanbeveling.** Per periode: de knip van ch. 3.10 veralgemeend tot elke voorwaarde op een
tijdsafhankelijk doel. Voor een scalair doel blijft ch. 3.3. Reden: de specificatie zegt het, ALEF
doet het, en uchar knipt al zo met het venster.

**Voorlopige besluiten (technisch).**
- VB 3.1 — De lezing hangt aan het doel, niet aan het woord: de wire krijgt geen veld voor
  "gedurende de tijd dat". Gemeten: de frontend schrijft beide vormen als dezelfde voorwaarde, en
  ALEF laat beide toe (zie boven).
- VB 3.2 — De ware perioden van de voorwaarde volgen uit punt 1 en ch. 3.9 per dag (een
  samenstelling per dag met `and`/`or`/`not` en de tellende kwantoren); de knip snijdt segmenten en
  houdt waarden (ch. 3.10 r. 3401–3406).
- VB 3.3 — Een voorwaarde waarvan het raster fijner is dan dat van het doel is ill-formed (punt 8);
  een vergelijking leest per periode ch. 1.2, zoals de knip van ch. 3.6 sinds de kaart
  `20261009-runtime-tijdconditie-leeg` (geland op `27cfd3dfa`, golden `jutterswet-peilwacht`) in ref,
  clingo en beide Drools-takken doet: een gat in de getoetste tijdlijn en een lege grens zijn leeg,
  dus een gat voldoet aan "ongelijk aan 5" en "gelijk aan" houdt op een gat tegen een lege grens. De
  voorwaarde per periode leest een lege dag daarmee zoals die knip; dat ch. 3.9 hem anders leest, is
  punt 4b.

**Uitgewerkt voorbeeld** (`WV-3.10-voorwaarde-per-periode`). Een Afdracht met `rentedragend` (een
kenmerk voor elke dag) op [2025-11-16, 2026-03-13) en `verzuimd` (een kenmerk voor elke maand) op
[2025-12-01, 2026-02-01); de regel "de minimumdagen (geheel getal, voor elke maand) moet berekend
worden als (het aantal dagen in de maand dat hij rentedragend is) gedurende de tijd dat hij
verzuimd is". Uitkomst: 31 op [2025-12-01, 2026-01-01) en 31 op [2026-01-01, 2026-02-01); in elke
andere maand schrijft de regel niets. Voor formulering B (§4): de dagrente (€/jr, voor elke dag) =
de verzuimrentevoet maal het afdrachtbedrag gedurende de tijd dat hij rentedragend is, geval 1:
400 op [2025-11-16, 2026-02-01) en 500 op [2026-02-01, 2026-03-13), daarbuiten niets.

### Punt 4 — De gehele-periodekwalificator op een regel met een tijdsafhankelijk doel

**Vraag.** Leest "gedurende de gehele maand / het gehele jaar" in een voorwaarde op zo'n regel per
emmer (een waarheidstijdlijn op de emmers van de kwalificator), of vouwt hij tot één waarheidswaarde
(ch. 3.9)?

**Lezingen.**
- *uchar nu.* RS ch. 3.9 r. 2811–2817 vouwt altijd tot één waarheidswaarde, voor elk doel. Gemeten
  (§5, `p07b`, `p07c`): **alle vier de uitvoerders** (ref, clingo, beide Drools-takken) voeren die
  vouw uit op een dagentelling onder "gedurende de tijd dat hij niet gedurende de gehele maand
  rentedragend is" en schrijven daarmee alle vijf de maanden, waar de specificatie en ALEF er twee
  geven. Geen golden zet de kwalificator op een regel met een tijdsafhankelijk doel (gemeten over
  `corpus/golden`: 0 van 17 versies). Voor een scalair doel is de vouw gepind door
  `synth-gehelejaarkenmerk` en `jutterswet-ploegpremie`.
- *ALEF.* Per emmer: `TimeLine.wholePeriods`, `partialPeriods` en `notWholePeriods`
  (`time/TimeLine.java` r. 228–251) geven een waarheidstijdlijn op het raster; de vertaler van
  `HeleTijdvak` gebruikt `notWholePeriods` en `wholePeriods` (translator r. 11600 en 11624, methode
  `tpredicaat`). `notWholePeriods` is waar in elke maand die niet geheel geldt, ook in een maand
  zonder één ware dag (r. 248–251; gedraaid op geval 2: waar overal behalve in maart 2028).
- *Specificatie.* De controle is alleen waar als ze op elk moment van de opgegeven periode waar is
  (spec §8.2). Hoe dat samengaat met een tijdsafhankelijk doel zegt §8.2 niet; met §10.3 (de
  voorwaarde per periode) ligt "de opgegeven periode" per emmer voor de hand, maar de tekst beslist
  het niet. Nagekeken: §8.2, §8.4.1–§8.4.3, §10.3.

**Normvraag** (omdat RS ch. 3.9 de vouw voor elk doel uitspreekt en de uitvoerders haar zo
uitvoeren). **Aanbeveling: per emmer op een tijdsafhankelijk doel; de vouw blijft voor een scalair
doel.** Reden: ALEF leest het zo, het volgt uit punt 3, en een vouw zou de schrijfactie van een
tijdlijn poorten met één waarheidswaarde over haar hele span — precies de poort die punt 3 opheft.
Geen golden verandert.

**Uitgewerkt voorbeeld** (`WV-3.9-per-emmer`). `rentedragend` op [2025-11-16, 2026-03-13).
"Gedurende de gehele maand rentedragend" per emmer: waar in december 2025, januari en februari 2026,
onwaar in elke andere maand. De regel "de minimumdagen moet berekend worden als (het aantal dagen in
de maand dat hij rentedragend is) gedurende de tijd dat hij niet gedurende de gehele maand
rentedragend is":
- per emmer, met 7a = 0 en alle tijd: 0 tot 2025-11-01, 15 op [2025-11-01, 2025-12-01), niets op
  [2025-12-01, 2026-03-01), 12 op [2026-03-01, 2026-04-01), 0 vanaf 2026-04-01;
- per emmer, met de dagentelling van nu en de span: 15 en 12, in november en maart;
- gemeten nu (de vouw): 15, 31, 31, 28 en 12 in november tot en met maart.

#### Punt 4b — De lege dag in een vergelijking onder de kwalificator

**Vraag** (gevonden door de bouwer van `20261009-runtime-tijdconditie-leeg`, open vraag 1 van zijn
`Result:`). De knip van ch. 3.6 en de kwalificator van ch. 3.9 lezen een lege dag van dezelfde
vergelijking tegengesteld. Is dat bedoeld, en zo nee, welke lezing geldt?

**Lezingen.**
- *uchar nu.* De knip leest ch. 1.2 per periode: een gat voldoet aan "ongelijk aan" tegen een gevulde
  grens (RS ch. 3.6 r. 2150–2154, het voorbeeld van 500,00), en tegen een lege grens geldt "ongelijk
  aan" op elke gevulde dag (sinds `27cfd3dfa`, golden `jutterswet-peilwacht`). Onder de kwalificator
  maakt een lege grens de toets onwaar (RS ch. 3.9 r. 2833–2834), en een gatdag of een dag onder een
  expliciet leeg segment voldoet aan geen enkele vergelijking, ongelijkheid inbegrepen, hoewel ch. 1.2
  een scalaire ongelijkheid tegen leeg waar maakt (r. 2938–2948, besloten 2026-09-26; ref
  `ref/lib/timeline.ml` r. 284–288). Gemeten door die bouwer op een eigen document
  (`werk/drools-clip-equality/scratch/vraag/`, verifier PASS; wachtvergoeding per maand 10 over 2024):
  **K1**, peilstand 7 alleen in januari 2024 en drempelstand 5: de knip "ongelijk aan" geeft 120 in ref
  en Drools, "gedurende het gehele jaar ongelijk aan" is onwaar in ref en Drools. **K2**, peilstand 7
  over heel 2024 en drempelstand leeg: de knip "ongelijk aan" 120, de knip "gelijk aan" 0; "gedurende
  het gehele jaar ongelijk aan" is in ref waar (tegen ch. 3.9 in) en in beide Drools-takken onwaar
  (naar ch. 3.9); clingo weigert het document (CLINGO-010). Geen golden pint een vergelijking met
  "gelijk aan" of "ongelijk aan" onder de kwalificator: gemeten over `corpus/golden`, elke vergelijking
  onder de kwalificator (in `jutterswet-keurploeg`, `-ploegpremie`, `-reddingsvloot`, `-sleepvloot`
  en `-vast-startpunt`) is "groter dan" een literal, en een ordening tegen leeg is in beide lezingen
  onwaar.
- *ALEF.* De vergelijking per moment is de scalaire vergelijking met null als leeg:
  `Vergelijker.vergelijk` (`Vergelijker.java` r. 18–45) maakt "gelijk aan" waar op twee lege kanten,
  "ongelijk aan" waar op precies één, en een ordening onwaar op een lege kant; de vertaler van een
  tijdsafhankelijk predicaat roept haar aan (translator r. 11350, methode `tpredicaat`). De knip is
  `ITimed.when` op die geldigheid en de kwalificator `TimeLine.wholePeriods` erop (r. 228–230): beide
  lezen dezelfde geldigheid per moment, dus een lege dag op dezelfde manier. Gedraaid (eigen
  proefprogramma, de vergelijking per moment met `Vergelijker.vergelijk`, de geldigheid met
  `IValidity.whenTrue`): K1 "ongelijk aan": de knip 120, het gehele jaar 2024 **waar**; K2 "ongelijk
  aan": de knip 120, het gehele jaar **waar**; K2 "gelijk aan": de knip 0, het gehele jaar onwaar.
- *Specificatie.* Spreekt: de kwalificator is waar als de controle op elk moment van de periode waar
  is (spec §8.2), en de controle is het predicaat van §8.1 met zijn rekentabel voor lege waarden:
  "ongelijk aan" is waar bij precies één lege kant (typ. §5.6), "gelijk aan" bij een getal waar bij
  twee lege kanten (typ. §5.3), een ordening onwaar bij een lege kant (typ. §5.1, §5.2, §5.4, §5.5).
  Een gatdag voldoet dus aan "ongelijk aan 5", en tegen een lege grens geldt "ongelijk aan" op elke
  gevulde dag, onder de kwalificator net als in de knip.

**Normvraag** (de specificatie en ALEF tegen het besluit van 2026-09-26 in RS ch. 3.9).
**Aanbeveling: ch. 1.2 per dag, ook onder de kwalificator**, zodat beide collapses een lege dag van
dezelfde vergelijking gelijk lezen. Reden: zo staat het in §8.2 met typ. §5.3 en §5.6, zo rekent
ALEF, en het is de lezing die de knip sinds `27cfd3dfa` in alle uitvoerders heeft. Gevolg: geen golden
verandert; de clausules van ch. 3.9 over de lege grens en het expliciete lege segment bij een
vergelijking, hun uitgewerkte voorbeelden met "≠ 40" (r. 2941–2946) en de `ref`-tests die ze pinnen
gaan mee, en de Drools-hulp van de kwalificator en clingo's weigering van een attribuut als grens
(CLINGO-010) ook. Dat is precies de uitweg die de bugkaart `20261009-ref-gehele-periode-lege-grens`
in haar acceptatie (a) noemt: met deze lezing heeft ref op K2 gelijk en gaan ch. 3.9 en Drools mee.
De leeg- en gevuld-toetsen van ch. 3.9 veranderen niet: die zijn al per dag (r. 2983–3017). Welke
emmers de kwalificator raakt (de emmers van de getoetste tijdlijn, r. 2900–2911) is een andere vraag
en blijft.

**Uitgewerkt voorbeeld** (`WV-3.9-lege-dag`). Een maandattribuut `peilstand`, een scalair
`drempelstand`, "gedurende het gehele jaar":
- K1, peilstand 7 op [2024-01-01, 2024-02-01), drempelstand 5: "ongelijk aan" **waar** (31 dagen met
  7 ≠ 5 en 335 gatdagen met leeg ≠ 5, 366 van 366); nu onwaar (31 van 366). "Gelijk aan" onwaar in
  beide lezingen.
- K2, peilstand 7 op [2024-01-01, 2025-01-01), drempelstand leeg: "ongelijk aan" **waar** (366 van
  366 met 7 ≠ leeg); nu onwaar (de lege grens). "Gelijk aan" onwaar in beide lezingen.
- Ter vergelijking de knip, wachtvergoeding 10 per maand over 2024: K1 "ongelijk aan" 120, K2
  "ongelijk aan" 120 en "gelijk aan" 0 (zoals nu, in ref, clingo en Drools).

### Punt 5 — Twee schrijvers van één tijdlijndoel

**Vraag.** Leest de schrijversregel van ch. 5.7 per dag, met een fout alleen op een dag waar beide
schrijven en verschillen?

**Lezingen.**
- *uchar nu.* RS ch. 5.7 r. 4342–4388: twee schrijvers wier voorwaarden op één instantie beide
  gelden en die verschillende waarden schrijven, geven een fout (EF-016); besloten door de eigenaar
  op 2026-11-02: telt wie aan het fixpunt beslist. Gemeten (§5, `p05-twee-schrijvers`): clingo
  weigert twee schrijvers van een tijdlijndoel (`translate.ml` r. 10789), beide Drools-takken ook
  (DROOLS-103, `backends/drools/README.md` r. 819); ref komt niet zo ver (het doel zelf, r. 3239).
- *ALEF.* `ITimed.merge` per moment: is één kant null, dan telt de andere; zijn beide gevuld en
  verschillend, dan een fout (`time/ITimed.java` r. 99–106); de vertaler van een actie gebruikt dat
  voor schrijvers van één tijdsafhankelijk attribuut (translator r. 2184–2335). Gedraaid: de twee
  schrijvers van formulering A geven samen de rentedagen van §4.
- *Specificatie.* Zwijgt. Nagekeken: §4.1–§4.3 (regel, regelversie: versies van één regel
  overlappen niet), §9.1 (gelijkstelling), §10.3; geen paragraaf zegt iets over twee regels die één
  attribuut schrijven.

**Besloten: ALEF (spec zwijgt).** Per dag beslissen de schrijvers die op die dag schrijven; dat is
het besluit van 2026-11-02 per dag gelezen.

**Voorlopige besluiten (technisch).**
- VB 5.1 — Een schrijver wiens tijdlijn op dag d leeg leest, beslist dag d niet. Dat volgt uit
  ch. 3.1 (een gat en een leeg segment zijn per `read` gelijk) en is ALEF's `merge`; de zin "a writer
  that fires with a leeg value writes leeg" van ch. 5.7 heeft per dag geen tegenhanger.
- **Besluit van de eigenaar (2026-11-04), invoer naast schrijvers.** Op een tijdlijndoel beslist elke schrijver, ook een
  enige, alleen de dagen waarop zijn tijdlijn een waarde leest; op een dag die geen schrijver beslist, staat de invoer (of
  leeg); op een dag die een schrijver beslist, staat zijn waarde. Dat volgt ALEF (`merlin/lazy/MLazyTimedProperty.java`
  r. 18–31: elke schrijfactie wordt per moment samengevoegd met de huidige waarde, de invoer inbegrepen; de specificatie
  zwijgt, §9.3 zet invoer buiten haar bereik) en herziet voor het tijdlijndoel het besluit van 2026-08-21 (RS ch. 7.8, "the
  timeline arm": de hele tijdlijn, invoer en berekende segmenten mengen nooit). Meting: `werk/writers-and-input/verslag.md`
  (buiten de repo). Kaart `20261104-writers-per-day-beside-input`.
- VB 5.2 — Eén fout EF-016 per (attribuut, onderwerp), met de schrijvers en de waarden van de eerste
  dag waarop ze verschillen; het register van ch. 5.7b verandert niet. Waar op een dag geen schrijver
  beslist, staat de invoertijdlijn van die dag of leeg (ch. 5.7, ch. 7.8 per dag).

**Uitgewerkt voorbeeld** (`WV-5.7-per-dag`). Geval 1 van formulering A: schrijver 1 zet 30 dg/mnd
gedurende de tijd dat hij gedurende de gehele maand rentedragend is; schrijver 2 telt de dagen
gedurende de tijd dat hij niet gedurende de gehele maand rentedragend is. Samen: 0 tot 2025-11-01,
15 op [2025-11-01, 2025-12-01), 30 op [2025-12-01, 2026-03-01), 12 op [2026-03-01, 2026-04-01), 0
vanaf 2026-04-01 — gelijk aan de gedraaide ALEF-uitkomst. Laat schrijver 2 de voorwaarde weg, dan
schrijven beide in december 2025 (30 en 31): EF-016 op `A1.rentedagen` met de waarden 30 en 31.

### Punt 6 — Van datum tot datum

**Vraag.** Wordt een tijdsafhankelijk kenmerk binnen een venster een schrijfvorm (de rode casus
`time-window-timeline-characteristic` gaat dan over in een golden), en laat WF-111 een datum plus of
min een duur als grens toe ("de dag na de vervaldatum")?

**Lezingen.**
- *uchar nu.* De knip op een kenmerkdoel is geweigerd (RS ch. 3.10 r. 3425–3432; ch. 3.11
  r. 3460–3483); een grens is een datumliteraal of een eigen scalair datumattribuut van dagprecisie
  (WF-111, RS r. 3335–3344, besluit van de eigenaar 2026-09-23), en de algemene datumexpressie is een
  OPEN wirebesluit (ch. 3.12 r. 3524). Gemeten (§5): de frontend weigert "zijn vervaldatum plus 1 dg"
  als grens (RGS-202); met een eigen datumattribuut als grens weigeren ref en clingo het kenmerkdoel
  met EF-023 en beide Drools-takken met DROOLS-102.
- *ALEF.* Het typesysteem eist alleen dat begin en einde van een periode datums zijn
  (`check_Periode`, typesystem r. 1805–2093); een vorm wordt niet beperkt.
- *Specificatie.* Spreekt: een tijdsafhankelijke conditie kan bij een tijdsafhankelijk attribuut óf
  kenmerk (spec §10.3); de grenzen van "van … tot (en met) …" mogen elke expressie met een datum als
  resultaat zijn, berekeningen van datums inbegrepen (spec §8.4.3); de betekenis van de vijf vormen
  staat in §5.1.3. Een kenmerktoekenning zonder voorwaarde maakt het kenmerk altijd van toepassing
  (§9.2), wat met punt 2 een tijdlijn over alle tijd is.

**Besloten: de specificatie (spec spreekt).** WF-111's beperking is een omheining die ch. 3.12 al
OPEN noemt, geen andere betekenis; ze vervalt.

**Voorlopige besluiten (technisch).**
- VB 6.1 — Een grens is elke dagprecieze datumexpressie over het onderwerp, de parameters en
  literalen; een datum-tijd blijft ill-formed (WF-111's precisie-arm blijft). `from` wordt vóór `to`
  geëvalueerd (ch. 5.11). Een lege grens geeft een leeg venster (ch. 3.10 r. 3407–3412); ALEF's
  gedrag bij een lege grens is niet nagegaan.
- VB 6.2 — Een kenmerkdoel binnen een venster schrijft de ware perioden van het venster, gesneden
  met de ware perioden van de overige voorwaarde (punt 3); het niet-ontkende kenmerk alleen (de
  monotone lezing van ch. 3.11).

**Uitgewerkt voorbeeld** (`WV-3.10-kenmerk-venster`). "Een Afdracht is rentedragend van zijn
vervaldatum plus 1 dg tot en met zijn betaaldatum." Geval 1 (verval 2025-11-15, betaald
2026-03-12): rentedragend op [2025-11-16, 2026-03-13). Geval 2 (verval 2028-02-10, betaald
2028-04-04): op [2028-02-11, 2028-04-05). Een lege betaaldatum: rentedragend nergens.

### Punt 7 — De dagentelling

**Vragen.** (a) Geeft een emmer zonder ware dag 0 of niets? (b) Mag de voorwaarde een samenstelling
zijn met een dagsoorttoets op de getelde dag ("geen schrikkeldag")? (c) Welke eenheid draagt de
telling?

**Lezingen.**
- *uchar nu.* (a) Een emmer zonder ware dag geeft niets (RS ch. 3.8 r. 2261), gepind door het anker
  `corpus/golden/anchor/jutterswet-dagentelling` (twee segmenten voor `borgdagen`). (b) De voorwaarde
  is een eigen tijdsafhankelijk kenmerk (WF-058, RS r. 2266–2270); rijkere vormen worden geweigerd.
  Gemeten (§5, `p07a`): de frontend schrijft een samengestelde voorwaarde als `Unresolved`-filter met
  een waarschuwing (sinds de kaart `20261012-dagentelling-onbekende-voorwaarde`, geland op
  `b3feb3882`; op `27cfd3dfa` liet ze de voorwaarde nog stil weg); de verifier weigert met WF-058, ref
  met REF-010, clingo met CLINGO-024 en Drools met DROOLS-114. (c) Geen eenheid (`ref/lib/timeline.ml` r. 221);
  de verifier weigert een telling in een doel met eenheid dg/mnd (WF-023, gemeten), en clingo ook
  (gemeten: "day-count into a unit-carrying target is not translated").
- *ALEF.* (a) `getDuration` is de telling per periode aangevuld met 0 (`time/TimeLine.java`
  r. 371–375); `TestTimeOperations.testDuration` (test r. 15–52) eist 0 vóór en na de ware periode.
  Gedraaid op geval 2: 0 tot 2028-02-01, 18, 31, 4 en 0 vanaf 2028-05-01, op open vakken. (b) Het
  typesysteem laat een dagsoorttoets op de getelde dag toe voor ALEF's standaarddagsoorten en weigert
  een andere dagsoort daar (`check_TijdsduurDat`, typesystem r. 2587–2947); de schrikkeldag is een
  standaarddagsoort (`objecten/languageModels/gegevensspraak/behavior.mps` r. 49533–49683);
  `testDuration2` (test r. 55–67) telt "rentedragend en geen schrikkeldag" en geeft februari 2016
  vier dagen. (c) Niet nagegaan in het typesysteem.
- *Specificatie.* (a) Spreekt: is er geen dag waarop de voorwaarde geldt, dan is het resultaat 0
  dagen per tijdseenheid (typ. §4.21). Het voorbeeld van spec §7.2 noemt alleen de maanden met
  dagen; dat spreekt typ. §4.21 niet tegen. (b) Zwijgt: na "dat" staat een voorwaarde (spec §7.2),
  maar wat "de dag" daarin is, en of een dagsoorttoets er mag, zegt de tekst niet; de specificatie
  kent geen standaarddagsoorten (§3.12, §8.1.5). (c) Spreekt: het resultaat heeft als eenheid dagen
  per tijdseenheid, die van de telling (spec §7.2, typ. §4.21).

**7a — normvraag.** **Aanbeveling: 0 in elke emmer van het domein (punt 2) zonder ware dag.** Reden:
de typering zegt het, ALEF doet het, en de formulering A van de ankercasus gebruikt het (een maand
zonder rentedagen heeft 0 rente, geen lege rente). Gevolg: de verwachte tijdlijnen van de goldens
met een dagentelling veranderen: het anker `jutterswet-dagentelling` (beschermd pad), en
`jutterswet-dagentelling-rgs`, `jutterswet-vast-startpunt` en `jutterswet-pachtvenster`, dat zijn
telling knipt met een venster; wat in elk precies verandert, meet kaart 1c. Met alle tijd (punt 2)
krijgt elke emmer zonder ware dag 0; met de span alleen een emmer tussen de eerste en de laatste
ware dag.

**7b — besloten: ALEF (spec zwijgt).** Een dagsoorttoets op de getelde dag mag in de voorwaarde van
een dagentelling. VB 7.1: uchar heeft geen standaarddagsoorten, dus de toets noemt een
gedeclareerde dagsoort; dat kan omdat een dagsoort in uchar een zuivere functie van de dag is
(WF-031, RS ch. 3.8 r. 2446–2449). Binnen de voorwaarde van de telling is de gebonden dag
(`BoundDay`) de getelde dag. Dit is ruimer dan ALEF, dat alleen zijn standaarddagsoorten toelaat;
de reden voor die grens in ALEF is niet nagegaan.

**7c — besloten: de specificatie (spec spreekt).** VB 7.2: de telling draagt dagen per eenheid van
haar raster (dg/mnd, dg/jr) waar haar doel een eenheid van die dimensie declareert; in een doel
zonder eenheid schrijft ze het kale getal, zoals nu, zodat geen golden verandert. Een doel met een
andere eenheid is ill-formed (WF-058 uitgebreid).

**Uitgewerkt voorbeeld** (`WV-3.8-nul-en-dagsoort`). Geval 2: `rentedragend` op
[2028-02-11, 2028-04-05), een dagsoort `schrikkeldag` (maand 2, dag 29); "het aantal dagen in de
maand dat hij rentedragend is en de dag geen schrikkeldag is" in een doel met eenheid dg/mnd: 0 tot
2028-02-01, 18 op [2028-02-01, 2028-03-01), 31 op [2028-03-01, 2028-04-01), 4 op
[2028-04-01, 2028-05-01), 0 vanaf 2028-05-01 (gelijk aan de gedraaide ALEF-uitkomst). Zonder de
dagsoorttoets is februari 19. Met de dagentelling van nu: alleen 18, 31 en 4.

### Punt 8 — Een fijnere operand in een grover doel

**Vraag.** Een rentevoet per dag in een rente per maand: weigeren, constantheid per emmer eisen, of
naar rato versnijden zoals ALEF's `PROPORTIONAL`?

**Lezingen.**
- *uchar nu.* Elke operand moet even grof of grover zijn dan het doel (RS ch. 3.4 r. 1240–1243);
  CONSTRUCTS rij 11 noemt automatische granulariteitsconversie "later". Geen WF-regel houdt het
  vast: gemeten (§5, `p08`) laten frontend en verifier een parameter per dag in een maanddoel door;
  ref en clingo weigeren de case (hun lader kent geen dagraster, "v0 defines buckets for month and
  year only"), Drools weigert de waarde (DROOLS-105).
- *ALEF.* Het typesysteem weigert een toekenning waarvan het doel de knips van de expressie of de
  voorwaarde niet kan dragen (`check_ActieIndienVoorwaarde`, typesystem r. 1052–1570; de test
  `isToekenbaarVanuit` vergelijkt de lengte van de tijdvakken in dagen en maanden,
  `gegevensspraak.tijd/models/gegevensspraak.tijd.behavior.mps` r. 11223). `TimeMode.PROPORTIONAL`
  (`time/TimeMode.java` r. 47–95) is een interne modus van het totaal en van het tijdsevenredig
  deel, geen omzetting bij toekenning.
- *Specificatie.* Spreekt: de tijdlijn van het resultaat moet even fijn of fijner zijn dan de fijnste
  van de argumenten (typ. §3; spec §5.1.1; voor een voorwaarde §10.3); een gewone expressie kent dus
  geen omzetting naar een grover doel.

**Besloten: de specificatie (spec spreekt).** Statisch weigeren. VB 8.1: een nieuwe WF-regel
(werknaam WF-126) eist van elke tijdlijnoperand van de waarde en van de voorwaarde van een regel
met een tijdsafhankelijk doel een raster dat in dat van het doel past; de producenten met een eigen
raster (pro rata, dagentelling, gehele periode) volgen hun eigen regel.

**8b — besloten: ALEF (spec zwijgt).** Een basis van het tijdsevenredig deel die fijner is dan de
emmer: de specificatie zegt alleen dat de knips van de emmer op het doel moeten passen (spec §7.3.2,
typ. §4.22). ALEF's vertaling van het tijdsevenredig deel (`redistribute`, `time/TimeLine.java`
r. 383–411; translator r. 7790) neemt per emmer het naar dagen gewogen gemiddelde van de basis.
RS ch. 3.7 r. 2205–2207 eist een basis die even grof of grover is; dat blijft tot een bron de vorm
vraagt. Geen bouwkaart.

**Uitgewerkte voorbeelden** (`WV-3.4-fijner-geweigerd`, `WV-3.7-gemiddelde`). (1) Een parameter
`dagrentevoet` voor elke dag in "de maandrente (voor elke maand) moet berekend worden als de
dagrentevoet maal zijn afdrachtbedrag maal zijn rentedagen gedeeld door het rentejaar": ill-formed.
(2) 8b: een basis van 400 €/jr tot 2026-02-15 en 500 €/jr daarna, geheel februari 2026 ware dagen,
het tijdsevenredig deel per maand: februari 2026 = (14 × 400 + 14 × 500) / 28 = 450 in de eenheid
van de basis.

### Punt 9 — Minimum, maximum, eerste en laatste over tijdlijnen

**Vraag.** Per periode over de operanden, of over de tijd van één tijdlijn?

**Lezingen.**
- *uchar nu.* Geweigerd; OPEN met de eis dat de twee lezingen apart gedefinieerd worden (RS ch. 3.5
  r. 1955–1961, ch. 3.12 r. 3518). Een rangschikkende vouw over een tijdsafhankelijk lid van een
  verzameling is geweigerd omdat er geen moment is om te rangschikken (ch. 3.6 r. 2110–2113).
  Gemeten (§5, `p09`): frontend en verifier laten "de maximale waarde van zijn rentedagen en zijn
  minimumdagen" door; ref, clingo en Drools weigeren het doel.
- *ALEF.* Per moment, als elke expressie (§2); de vertaler tilt ook een meervoudige expressie per
  moment (translator r. 5228, `MultiExpressie`). Niet gedraaid.
- *Specificatie.* Spreekt: alle expressies behalve de aggregatie in de tijd van §7.1 zijn per
  periode (spec §5.1.1, §5.1.4); §5.8.3 en §5.8.4 noemen geen lezing over de tijd.

**Besloten: de specificatie (spec spreekt).** Per periode over de operanden, met ch. 1.2 per dag
(een lege operand wordt overgeslagen, alle operanden leeg geeft leeg). Een vorm over de tijd van één
tijdlijn heeft geen bron en komt er niet; de weigering van ch. 3.6 r. 2110 voor een rangschikkende
vouw over leden vervalt, want per periode is er wel een moment. Geen bouwkaart zonder een vraag uit
een brontaal: gemeten, de twee WW-goldens (`ww-herleving-inkomen`, `ww-herleving-inkomen-rgs`)
dragen geen tijdlijn.

**Uitgewerkt voorbeeld** (`WV-3.5-maximum-per-periode`). `rentedagen` = 15 op
[2025-11-01, 2025-12-01) en 30 op [2025-12-01, 2026-01-01); `minimumdagen` = 20 op
[2025-12-01, 2026-02-01). De maximale waarde van beide: 15, 30, 20 in november, december en januari;
daarbuiten leeg.

### Punt 10 — Verschuiven en eenmalig

**Vraag.** Krijgen verschuiven (naar het begin van de maand of van de volgende maand, dia 30) en
eenmalig (dia 31) een plaats?

**Lezingen.**
- *uchar nu.* Geen tegenhanger; de grammatica van `frontends/regelspraak` heeft er geen vorm voor.
- *ALEF.* `TijdlijnConversie` heeft een afronding `VANAF_BEGIN` of `VANAF_VOLGENDE` en een vlag
  `naEerste` (`regelspraak.tijd/models/regelspraak.tijd.structure.mps` r. 257–297); de vertaler
  rondt daarmee de grenzen van een geldigheid af op het raster met `TimeLine.round`
  (translator r. 4000–4110; `time/TimeLine.java` r. 102–107), als `EARLIER`, `LATER` of
  `NEXT_PERIOD`. In de editor staat de vlag voor een gebeurtenis die na de eerste dag plaatsvindt
  (`regelspraak.tijd.editor.mps` r. 1400–1500). Gedraaid op het maandraster: `EARLIER` maakt van
  [2025-11-16, 2026-03-13) [2025-11-01, 2026-03-01); `LATER` en `NEXT_PERIOD` maken er
  [2025-12-01, 2026-04-01) van; op [2025-12-01, 2026-03-13) laat `LATER` het begin staan en schuift
  `NEXT_PERIOD` het naar 2026-01-01. Een concept "eenmalig" staat niet in de structuur van
  `regelspraak.tijd` (de negentien concepten nagelopen); de dichtstbijzijnde vorm is een periode
  "vanaf …" zonder einde.
- *Specificatie.* Zwijgt. Nagekeken: §5.1.3, §8.4.1–§8.4.3, §10.3, §13.4.15 en §13.4.17: geen vorm
  voor verschuiven of eenmalig.

**Besloten: ALEF (spec zwijgt).** De betekenis is ALEF's afronding van grenzen op het raster;
eenmalig is met punt 2 een periode vanaf een datum zonder einde. Bouwen pas als een brontaal van
uchar de vorm schrijft (het beginsel van CONSTRUCTS rij 35, r. 71); geen bouwkaart.

**Uitgewerkt voorbeeld** (vastgelegd, geen test tot er een vorm is). Zie de gedraaide waarden
hierboven.

### Punt 11 — De dagtelconventie bij integratie

**Vraag.** Integreert het totaal een rente per jaar over een dagraster met de gedeclareerde factor
(jr = 365 dg, zoals uchar) of met de werkelijke lengte van het kalenderjaar (zoals ALEF)?

**Lezingen.**
- *uchar nu.* De gedeclareerde factor beslist, niet de kalender (RS ch. 3.6 r. 2070–2087, met het
  voorbeeld mnd = 30 dg → 3600 € op r. 2165–2169). Geen golden declareert een factor tussen een
  dageenheid en een maandeenheid (gemeten over de 82 omrekeningen in `corpus/golden`: alleen
  wk = 7 dg). Gemeten (§5, `p11`): ref weigert het totaal over een dagraster bij een
  emmerscalair (een dagraster heeft geen emmer), clingo weigert een factor anders dan 1, Drools
  weigert een noemer die niet de rastereenheid is (DROOLS-228).
- *ALEF.* Een gedeeltelijke eenheid telt als dagen gedeeld door de dagen van de omsluitende
  kalendereenheid (`partialLength`, `time/TimeLine.java` r. 338–344), dus 365 of 366 per jaar; de
  vertaler van het totaal gebruikt `timesDuration` (translator r. 4632, methode `sum`). Gedraaid:
  formulering B geeft 139 13/73 € in geval 1 en 73 47/61 € in geval 2.
- *Specificatie.* Zwijgt over de integratie van een rente over een ander raster: §7.1 vraagt een
  eenheid per tijdseenheid en rekent alleen hele maanden voor; §5.1.2 zegt dat de berekening de
  tijdlijn en de tijdseenheid van een gegeven niet verbindt, behalve bij het tijdsevenredig deel. Wel
  zegt §7.3.1 dat het eenheidssysteem Tijd geen omrekenfactor van jaren of maanden naar dagen kent,
  omdat een maand of jaar geen vast aantal dagen heeft.

**Normvraag.** **Aanbeveling: de kalender.** Tussen een eenheid die in dagen telt (dg, wk) en een
die in maanden telt (mnd, kw, jr) integreert het totaal met de werkelijke lengte van de omsluitende
kalendereenheid, en een gedeclareerde factor tussen die twee families is ill-formed. Binnen een
familie (jr = 12 mnd, wk = 7 dg) blijft de factor, die is kalenderexact; een eenheid buiten de
kalender (`bm`, RS r. 2078) blijft bij haar factor. Reden: ALEF rekent zo, de specificatie sluit een
vaste factor tussen de families uit (§7.3.1), en geen golden verandert. Wat verandert: de
voorbeelden van RS ch. 3.6 met mnd = 30 dg en de `ref`-tests die ze pinnen.

**Uitgewerkt voorbeeld** (`WV-3.6-kalender`). Formulering B, geval 2: 500 €/jr op 54 rentedragende
dagen in 2028 → met jr = 365 dg 500 × 54/365 = **73 71/73 €**; met de kalender 500 × 54/366 =
**73 47/61 €**.

### Punt 12 — Het totaal over een gedeeltelijke eenheid

**Vragen.** (a) Telt het totaal per geknipt fragment alleen hele eenheden (uchar), of naar rato van
de werkelijke lengte (ALEF)? (b) Eist het totaal een eenheid per tijdseenheid? (c) Wat is het totaal
van een afwezige tijdlijn?

**Lezingen.**
- *uchar nu.* (a) Per ruw geknipt fragment hele eenheden, nooit naar rato (RS ch. 3.6 r. 1965–1973);
  gepind door golden `jutterswet-vast-startpunt` case 1 (`toelagesom` 0, NOTE r. 82–83) en de
  `ref`-test `verify-clip-per-fragment-floor` (`ref/test/test_ref.ml` r. 3195–3204). (b) Een gewone
  waarde zonder noemer telt als waarde × aantal hele eenheden (RS r. 2055–2060), en veel goldens
  gebruiken dat (onder meer `jutterswet-getijdenwacht`, `jutterswet-vast-startpunt`). (c) Een
  afwezige tijdlijn telt leeg, een tijdlijn zonder segmenten 0 (RS r. 2044–2049); OPEN als residu
  (ch. 3.12 r. 3538).
- *ALEF.* (a) `timesDuration` vermenigvuldigt elk vak met zijn lengte in eenheden, een gedeeltelijke
  eenheid naar rato (`time/TimeLine.java` r. 295–344); `testRedistribute` (test r. 150–172) geeft
  140 waar de vloer van uchar bij een jaarrooster 0 geeft; gedraaid: 140. (b) Het typesysteem eist
  voor het totaal een expressie met een eenheid per tijdseenheid (`check_Totaal`, typesystem
  r. 3665–3783). (c) Niet nagegaan.
- *Specificatie.* (a) Zwijgt: het totaal is een sommatie van de waarden in de tijd, met een conditie
  alleen over de waarden in de opgegeven periode (spec §7.1); de voorbeelden hebben alleen hele
  maanden. Nagekeken: §7.1, typ. §4.20. (b) Spreekt: een eenheid per tijdseenheid is een voorwaarde
  (spec §7.1, typ. §4.20, tabel 30). (c) Spreekt: lege waarden tellen als 0, en zijn alle waarden
  leeg, dan is het resultaat 0 (typ. §4.20).

**12a — normvraag.** **Aanbeveling: naar rato van de werkelijke lengte (ALEF), als gevolgde
afwijking van de spiegel in `spec/CONSTRUCTS.md` rij 11.** Reden: de specificatie zwijgt, de
eigenaar volgt dan ALEF, en de vloer per fragment is een eigenschap van de gespiegelde runtime die
twee aangrenzende fragmenten van één eenheid allebei op 0 zet. Gevolg: `jutterswet-vast-startpunt`
case 1 krijgt `toelagesom` = 100 × 3/366 + 100 × 2/365 = **6090/4453**, dat niet past in het gehele
doel (EF-006): de golden krijgt een doel in de onbegrensde vorm of een afronding; de `ref`-test van
de vloer gaat mee.

**12b — normvraag.** De specificatie en ALEF eisen een eenheid per tijdseenheid; uchar laat een
gewone waarde toe. **Aanbeveling: de gewone operand houden, gelezen als "per eenheid van het
raster", als gevolgde afwijking.** Reden: met 12a geeft die lezing hetzelfde getal als ALEF met een
noemer gelijk aan de rastereenheid, en de goldens die haar gebruiken blijven geldig.

**12c — besloten: de specificatie (spec spreekt).** Een afwezige tijdlijn telt 0; het residu van
ch. 3.6 en de rij van ch. 3.12 vervallen. Met punt 2 (alle tijd) is een afwezige tijdlijn overal
leeg en telt ze ook langs die weg 0.

**Uitgewerkte voorbeelden** (`WV-3.6-naar-rato`). (1) ALEF's eigen geval: 400 €/jr tot 2021-05-15 en
500 €/jr daarna, rentedragend op [2021-04-01, 2021-07-21): 44 × 400/365 + 67 × 500/365 = **140 €**;
met de vloer op een jaarrooster **0**. (2) 120 €/jr op 2024, geknipt tot [2024-09-28, 2024-10-03):
naar rato 120 × 5/366 = **100/61 €**, met de vloer **0**. (3) 100 €/mnd in januari 2024, geknipt in
[2024-01-01, 2024-01-15) en [2024-01-15, 2024-02-01): naar rato 100 × 14/31 + 100 × 17/31 =
**100 €**, met de vloer **0** (de `ref`-test van nu).

### Punt 13 — Tijdsafhankelijke feittypen

**Vraag.** Krijgt een feittype een tijdlijn (dia 19)?

**Lezingen.**
- *uchar nu.* Nee: het schema geeft alleen `Attribute`, `Characteristic`, `Parameter` en
  `StartPointDetermination` een tijdlijn.
- *ALEF.* De vertaler werkt met tijdsafhankelijke objectverzamelingen (`RtTimedObjectSet`,
  24 verwijzingen in de translator van `regelspraak.tijd`); hoe een feittype zijn tijdlijn
  declareert, is niet nagegaan.
- *Specificatie.* Zwijgt: een tijdlijn hoort bij een attribuut, kenmerk of parameter (spec §3.8); een
  feittype heeft er geen (§3.11, §13.3.9).

**Besloten: ALEF (spec zwijgt), vastgelegd als later.** Geen bouwkaart zonder een vraag uit een
brontaal (de WW-goldens dragen geen tijdlijn). Geen uitgewerkt voorbeeld: zonder vorm is er niets
dat `ref` kan uitvoeren.

## 4. De ankercasus

Eigen woorden, in de Jutterswet: verzuimrente over een te late afdracht. Een Afdracht met een
afdrachtbedrag van 10 000 €, een vervaldatum en een betaaldatum; rentedragend van de dag na de
vervaldatum tot en met de betaaldatum (punt 6); een verzuimrentevoet als parameter voor elke maand
(punt 8: een parameter voor elke dag past niet in een maanddoel), 4 % per jaar tot 2026-02-01 en
5 % per jaar daarna, geschreven als 0,04 en 0,05 met eenheid 1/jr.

- **Formulering A (30/360)** met eenheden die sluiten: rentedagen in dg/mnd (de eenheid van de
  telling, punt 7c), het rentejaar 360 dg/jr, de maandrente = de verzuimrentevoet maal het
  afdrachtbedrag maal de rentedagen gedeeld door het rentejaar, in €/mnd (1/jr × € × dg/mnd ×
  jr/dg), en de verzuimrente = het totaal van de maandrente, in € (ch. 3.6, €/mnd over een
  maandraster). De rentedagen: 30 in een maand die geheel rentedragend is; in een andere maand de
  rentedragende dagen die geen schrikkeldag zijn (twee schrijvers, punt 5).
- **Formulering B (werkelijke dagen):** een dagrente voor elke dag in €/jr = de verzuimrentevoet
  maal het afdrachtbedrag gedurende de tijd dat hij rentedragend is (punt 3), en de verzuimrente =
  het totaal van de dagrente, met jr = 365 dg (uchar) of de kalender (ALEF, punt 11).

| Geval | A (30/360) | B, uchar (jr = 365 dg) | B, ALEF |
|---|---|---|---|
| 1: verval 2025-11-15, betaald 2026-03-12, 4 % tot 2026-02-01, daarna 5 % | nov 16 2/3, dec 33 1/3, jan 33 1/3, feb 41 2/3, mrt 16 2/3; totaal **141 2/3 €** | 400 × 77/365 + 500 × 40/365 = **139 13/73 €** | **139 13/73 €** (2025 en 2026 tellen 365 dagen) |
| 2: verval 2028-02-10, betaald 2028-04-04, 5 % | feb 18 rentedagen (19 min de schrikkeldag) → 25; mrt 30 → 41 2/3; apr 4 → 5 5/9; totaal **72 2/9 €** | 500 × 54/365 = **73 71/73 €** | 500 × 54/366 = **73 47/61 €** |

Alle vijf de waarden zijn opnieuw en onafhankelijk nagerekend in exacte breuken (Python
`fractions`, kalender uit `datetime`; geen afwijking) en ALEF's runtime geeft dezelfde vijf
(gedraaid, met de maandrente van geval 1 per maand en de rentedagen van geval 2 per maand). De presentatie geeft voor de eerste term van B
84 134/365 €; juist is 400 × 77/365 = 84 140/365 = **84 28/73 €**, en de termen van de dia zelf
tellen op tot 139 59/365 €. A en B verschillen in geval 1 **545/219 €** (≈ 2,49 €).

Geval 1 vraagt in A de punten 1–7, in B de punten 1, 2, 3 (of het knippende totaal van ch. 3.6),
6 en 11; geval 2 pint de schrikkeldagtoets (7b) en de conventie (11). Een lege betaaldatum is geen
ankerwaarde maar een vraag van punt 2 en 6.

## 5. De meting

Gemeten op main `b3feb3882` (de branch `card/implicit-time` stond er zonder codewijziging op). Twee
eerdere runs, op `95947ae0f` en op `27cfd3dfa`, gaven per document dezelfde regel, behalve bij `p07a`:
daar liet de frontend de samengestelde voorwaarde van de dagentelling tot `b3feb3882` stil weg (geen
filter, geen diagnose), gaf ref REF-011 en noemde Drools "(no condition)". Gemeten met de
frontend (`make -C frontends/regelspraak ir`), de verifier, ref (`runner.exe`), clingo
(`clingo_runner.exe`) en de Drools-jar in beide takken (zonder vlag en met `--drl`), gebouwd in de
worktree. De documenten staan buiten de repo onder `werk/implicit-time/meting/`; de commando's in het
`Result:`. Per document de eerste weigering per uitvoerder; de zinnen staan onder de tabel. In elke
rij geven beide Drools-takken dezelfde zin.

| Document | Vorm (punt) | frontend | verifier | ref | clingo | Drools (beide) |
|---|---|---|---|---|---|---|
| `a` (formulering A) | heel A | Z1 | — | — | — | — |
| `a-attribuutgrens` (grens: eigen datumattribuut) | heel A | Z2 (crash) | — | — | — | — |
| `a-invoer` (rentedragend uit de case) | heel A zonder 6 | IR | Z3 | Z4 | Z8 | D103 |
| `b` (formulering B) | heel B | Z1 | — | — | — | — |
| `b-attribuutgrens` | heel B | IR | PASS | Z5 | Z5 | D102 |
| `b-invoer` | heel B zonder 6 | IR | PASS | Z4 | Z7 | D105 |
| `p01-rekenen-per-periode` | maal, gedeeld door, parameter (1, 2) | IR | PASS | Z4 | Z7 | D105 |
| `p01b-rekenen-zonder-parameter` | maal en plus over twee tijdlijnen (1) | IR | PASS | Z4 | Z7 | D105 |
| `p03-voorwaarde-per-periode` | voorwaarde per periode, dagdoel (3) | IR | PASS | Z4 | Z7 | D105 |
| `p03b-voorwaarde-per-periode-op-dagentelling` | voorwaarde per periode, maanddoel (3) | IR | PASS | Z10 | Z11 (geen weigering) | D271 |
| `p04-gehele-maand-als-voorwaarde` | scalair in tijdlijn onder gehele maand (2, 4) | IR | PASS | Z4 | Z7 | D105 |
| `p05-twee-schrijvers` | twee schrijvers (5) | IR | Z3 | Z4 | Z8 | D103 |
| `p06a-kenmerk-van-datum-plus-duur-tot-en-met` | kenmerk binnen venster, berekende grens (6) | Z1 | — | — | — | — |
| `p06b-kenmerk-van-attribuut-tot-en-met` | kenmerk binnen venster (6) | IR | PASS | Z5 | Z5 | D102 |
| `p07a-dagentelling-met-dagsoorttoets` | samengestelde voorwaarde met dagsoort (7b) | IR met Z17 | Z3, Z12 | Z6 | Z7 | D114 |
| `p07b-dagentelling-onder-voorwaarde` | telling in dg/mnd onder niet-gehele maand (4, 7c) | IR | Z3 | Z13 | Z9 | Z13 |
| `p07c-…-zonder-eenheid` | dezelfde telling zonder eenheid (4) | IR | PASS | Z13 | Z13 | Z13 |
| `p08-fijnere-operand` | parameter per dag in maanddoel (8) | IR | PASS | Z14 | Z14 | D105 |
| `p09-maximum-over-tijdlijnen` | maximum per periode (9) | IR | PASS | Z4 | Z7 | D105 |
| `p11-totaal-per-dag` | €/jr over dagraster, jr = 365 dg (11) | IR | PASS | Z15 | Z16 | D228 |
| `p12-totaal-per-maand` | €/mnd over maandraster (12) | IR | PASS | PASS | PASS | PLAYED |

Zinnen (letterlijk zoals de uitvoerder ze schrijft; "…" vervangt alleen het regelnummer of de naam):

- **Z1** (frontend, RGS-202): `ERROR (line …): periodevergelijking (de vanaf-grens 'zijn vervaldatum plus 1 dg'): the from bound of a time window is a computed date expression — a bound is a date literal or an own scalar date attribute of the subject; the general date expression is an OPEN wire decision`
- **Z2** (frontend, geen diagnose maar een uitzondering, `IrEmitter.unknownType`, `IrEmitter.java:761`; teruggebracht tot de regel met de samengestelde dagentelling in een variabele): `Exception in thread "main" java.lang.IllegalStateException: IrEmitter: een onbekend datatype ('niet opgelost') heeft op deze positie geen spelling in uchar-ir-v1; alleen een afgeleide positie schrijft null en een Unresolved-marker laat zijn type weg`
- **Z3** (verifier): `WF-023.incompatible-units FAIL /rules/rules/…/versions/0: assignment across incompatible units: value unit 'geen', target unit 'dg/mnd'`
- **Z4** (ref, zonder code): `EVALUATION FAULT: temporal attribute "…" as a target: temporal writes are not implemented`
- **Z5** (ref en clingo, EF-023): `EF-023: a period condition on the temporal characteristic target "rentedragend" is ch. 3.10's CLIP over a characteristic timeline, and no temporal characteristic is written here (ch. 3.11), so there is no timeline to cut. Declined at evaluation` (clingo: `TRANSLATION FAULT: …` en `Declined in translation`)
- **Z6** (ref, REF-010): `EVALUATION FAULT: REF-010: unsupported day-count condition shape (an own temporal characteristic; no corpus case for more)`
- **Z7** (clingo, CLINGO-024): `TRANSLATION FAULT: CLINGO-024: a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape (a gedurende condition over an own temporal characteristic); no corpus case for more`
- **Z8** (clingo): `TRANSLATION FAULT: two computing rules write the temporal target "rentedagen", and a divergence between their timelines is not translated (no corpus case)`
- **Z9** (clingo): `TRANSLATION FAULT: day-count into a unit-carrying target is not translated (no corpus case)`
- **Z10** (ref): `EVALUATION FAULT: temporal characteristic "verzuimd" in a scalar condition is refused (ch. 3.3)`
- **Z11** (clingo, geen weigering): `MISMATCH: A1.minimumdagen timeline = (empty), expected [20423,20454)=31 [20454,20485)=31` — clingo leest het tijdsafhankelijke kenmerk in de voorwaarde als scalair en schrijft niets, waar ref weigert.
- **Z12** (verifier): `WF-058.condition-not-check FAIL /rules/rules/0/versions/0/value/filter: the day-count gedurende condition is an own temporal characteristic check`
- **Z13** (geen weigering; alle vier schrijven de vouw van ch. 3.9 als poort): ref `MISMATCH: A1.rentedagen = [2025-11-01,2025-12-01)=15 [2025-12-01,2026-01-01)=31 [2026-01-01,2026-02-01)=31 [2026-02-01,2026-03-01)=28 [2026-03-01,2026-04-01)=12, expected [2025-11-01,2025-12-01)=15 [2026-03-01,2026-04-01)=12`; clingo (in `p07c`) en Drools (`FAIL case.json: …`) dezelfde vijf maanden. In `p07b` schrijft ref een telling zonder eenheid in een doel met dg/mnd zonder fout (RS ch. 2.6 regel 6 vraagt er een).
- **Z14** (ref en clingo, de lader): `CASE ERROR: parameter dagrentevoet: the declared grid is day, which every day boundary sits on, yet this loader checks a timeline's grid through the calendar bucketing and v0 defines buckets for month and year only (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12): refused as a v0 limit, not as a defect of the case`
- **Z15** (ref): `EVALUATION FAULT: bucketing is defined for month and year only (ch. 3.2)`
- **Z17** (frontend, waarschuwing; het filter wordt een `Unresolved`): `WARNING (line …): onopgelost: een dagentelling telt alleen de dagen waarop het onderwerp zelf één tijdsafhankelijk kenmerk heeft, in de vorm 'dat hij <kenmerk> is' of 'dat hij <kenmerk> heeft'; deze voorwaarde heeft die vorm niet (§13.4.17.57, RUNTIME-SEMANTICS ch. 3.8) — 'er aan alle volgende voorwaarden wordt voldaan: - hij is rentedragend - de dag is geen schrikkeldag' blijft als marker op de wire staan`
- **Z16** (clingo): `TRANSLATION FAULT: the noemer strip of the temporal total of "dagrente" restates a EUR/jr value into a dg grid by a declared factor other than 1, which this integer encoding does not carry (ref applies the exact factor); only a denominator identical to the grid unit cancels here`
- **D102**: `EXCLUDE DROOLS-102: transpiler SKIP: regel: rentedragend — the time-dependent characteristic 'rentedragend' of Afdracht as target: this branch does not yet carry this form; no golden with cases writes one (RUNTIME-SEMANTICS ch. 3.11)`
- **D103**: `EXCLUDE DROOLS-103: transpiler SKIP: regel: rentedagen in een gehele maand — a time-dependent target with a second writer ('rentedagen' of Afdracht is also written by "rentedagen in een gedeeltelijke maand"): this branch does not yet carry this form; a target with more writers stays fenced (RUNTIME-SEMANTICS ch. 7.8), and no golden with cases has one`
- **D105**: `EXCLUDE DROOLS-105: transpiler SKIP: regel: … — a value of the time-dependent target '…' of Afdracht that is neither a time-proportional part nor a day count ('…'): this branch does not yet carry this form (RUNTIME-SEMANTICS ch. 3.4, 3.5, 3.11); no golden with cases writes one`
- **D114**: `EXCLUDE DROOLS-114: transpiler SKIP: regel: rentedagen — a day count ('het aantal dagen in de maand dat er aan alle volgende voorwaarden wordt voldaan: - hij is rentedragend - de dag is geen schrikkeldag') with a condition that is not a characteristic of the subject ('er aan alle volgende voorwaarden wordt voldaan: - hij is rentedragend - de dag is geen schrikkeldag'): this branch does not yet carry this form; the reference interpreter takes only an own time-dependent characteristic (RUNTIME-SEMANTICS ch. 3.7, 3.8)`
- **D228**: `EXCLUDE DROOLS-228: transpiler SKIP: regel: verzuimrente — a total over its own timeline ('het totaal van zijn dagrente') over 'dagrente' with the rate EUR/jr on a grid of DAG: the denominator is not the unit of the grid, and this branch does not recalculate it (RUNTIME-SEMANTICS ch. 3.6); no golden with cases has it`
- **D271**: `EXCLUDE DROOLS-271: transpiler SKIP: regel: minimumdagen in een verzuimde maand — the time-dependent characteristic 'verzuimd' of Afdracht at a place that reads or writes one value: this branch reads a timeline only in a total, the clip of it, a time-proportional part, a day count and a uniqueness key, and writes it only with a time-proportional part or a day count (RUNTIME-SEMANTICS ch. 3.3, 3.6–3.8, 3.11, 5.9)`

Bevindingen die buiten de besluitpunten om gaan; geen ervan is in deze kaart gerepareerd:

1. **De frontend crasht** (Z2) op een variabele die een dagentelling met een samengestelde voorwaarde
   bevat: een Java-uitzondering in plaats van een diagnose.
2. *Opgelost op `b3feb3882`:* tot die commit liet de frontend een samengestelde voorwaarde van een
   dagentelling stil weg (`p07a`: een `dayCount` zonder `filter`, zonder diagnose); de kaart
   `20261012-dagentelling-onbekende-voorwaarde` maakt er een waarschuwing en een `Unresolved` van (Z17).
3. **clingo weigert een tijdsafhankelijk kenmerk in de voorwaarde van een regel met een
   tijdsafhankelijk doel niet** (Z11): het schrijft een lege tijdlijn, waar ref met de zin van ch. 3.3
   weigert. Een stille afwijking tussen twee uitvoerders.
4. **De granulariteitsregel van ch. 3.4 heeft geen WF-controle** (`p08`): frontend en verifier laten
   een operand per dag in een maanddoel door.
5. **ref schrijft een telling zonder eenheid in een doel met eenheid** zonder fout (`p07b`), waar de
   verifier WF-023 geeft en ch. 2.6 regel 6 een fout vraagt.

Bevinding 3 en 5 zijn bestaande fouten in ref of clingo en horen volgens de afspraak een eigen kaart;
1 en 4 horen bij de frontend en de verifier. De hoofdsessie legt ze vast.

ALEF-kolom: gelezen én gedraaid (zie de kop). Formulering A en B volledig, met dezelfde vijf waarden
als §4.

## 6. Fasering

De volgorde, met per kaart de besluitpunten die ze nodig heeft; de kaartteksten staan in het
`Result:` van de kaart.

1. **1a** — ch. 3.4 en 3.5 via een regel in ref en clingo, met een golden (plus, min, de som per
   periode). Nodig: punt 2.
2. **2a** — Drools voor 1a, in de lowering met truth maintenance (beide weergaven), direct na 1a.
3. **1b** — punten 1, 2 en 11 met formulering B als anker (rentedragend uit de case, het totaal
   geknipt met "gedurende de tijd dat hij rentedragend is", ch. 3.6), het dagraster in de laders en
   het case-formaat. Nodig: 1, 2, 11.
4. **2b** — Drools voor 1b.
5. **1c** — punten 3–8 met formulering A als anker, en de frontend. Nodig: 3, 4, 5, 6, 7, 8.
6. **2c** — Drools voor 1c.
7. **12** — een eigen kaart als de eigenaar 12a of 12b als ALEF beslist; past na 1b (dezelfde
   integratie).
8. **4b** — geen eigen kaart: de bestaande kaart `20261009-ref-gehele-periode-lege-grens` draagt het
   besluit (haar acceptatie (a) noemt de lezing per dag al als uitweg); kiest de eigenaar de
   aanbeveling, dan gaan ch. 3.9, de Drools-hulp van de kwalificator en clingo mee, en ref niet. Ze
   staat los van 1a–2c en kan eerder.

De punten 9, 10 en 13 krijgen geen bouwkaart.
