# De stille kwalificaties op de kenmerkcheck in de `gedurende`-clip

Goal: beslissen — per flank, met een machinaal toetsbare uitkomst — wat de vier
gemeten *stille* kwalificaties op een `Kenmerk`-node binnen een `gedurende`-clip
betekenen. Alle vier zijn vandaag verifier-PASS en worden door `ref` én
`backends/clingo` zonder één woord genegeerd of stil naar 0 gerekend. Ze zijn
geen bugs in één uitvoerder: de twee uitvoerders zijn het *eens* over de stilte,
en juist daarom kan geen enkele bestaande gate ze zien. De kaart die de
temporaliteitspoort op de clip landde (2026-09-06) mat ze op en liet ze
uitdrukkelijk liggen, omdat elke flank tegelijk in `ref`, in de ASP-backend, in
de verifier en soms in het corpus moet bewegen — eenzijdig sluiten *maakt* een
gate-6-divergentie waar er nu geen is.

## De vier flanken (alle gemeten 2026-09-06)

Meetopstelling voor alle vier: `corpus/golden/anchor/jutterswet-dubbelwacht`,
getwist in een `/tmp`-uitpak van de commit, alle cases door de verifier, `ref`
en de ASP-backend. `case1-overlap` pint 30, `case3-een-leeg` pint 0 — een stil
verkeerd antwoord is dus zichtbaar.

**Flank 1 — `eigenaar` verdampt.** `Kenmerk.eigenaar` op een clip-lid gezet
op een *verklaarde* typenaam: verifier **PASS**, `ref` PASS (2 expectations),
ASP-backend PASS (2 expectations). Beide uitvoerders matchen het veld weg.
(Zet je er een niet-verklaarde naam neer, dan vangt WF-034 het document —
maar dát is de resolutievraag, niet de semantische.)

**Flank 2 — `periode` verdampt, ook de ontkenning.** `Kenmerk.periode` op een
clip-lid gezet op elk van de vier `GeheelPeriode`-vormen (`jaar` × `niet`):
**20 metingen, 20× identiek aan `periode: null`** — 4 vormen × (verifier +
`ref`/ASP op twee cases). Ook `niet: true` verandert niets: een
*ontkende* volledige-periode-eis verdwijnt even geruisloos als de bevestigende.

**Flank 3 — het statisch geleverde temporele kenmerk.** De case-lader neemt
een kenmerk in de statische lijst aan zodra het *verklaard* is
(`ref/lib/case.ml` r. 285-289 toetst alleen verklaardheid), terwijl de clip
uitsluitend de kenmerk-*tijdlijnen* leest. Lever een temporeel kenmerk
statisch en de doorsnede is leeg:

    ref     MISMATCH: J1.dubbelwachtsom = 0, expected 30
    clingo  MISMATCH: J1.dubbelwachtsom = [0], expected 30

Dit is de enige flank met een **corpusgetuige**: het IR-document is
ongewijzigd en welgevormd, alleen het case-bestand levert het kenmerk in de
verkeerde vorm. Beide uitvoerders zwijgen en antwoorden 0.

**Flank 4 — de null-granulariteit, drie tegen één.** `kenmerkGranulariteit:
{ "k": null }` is schema-legaal (de enum draagt `null` expliciet) en
**verifier PASS** — gemeten. Drie lezers beslissen "temporeel" op
**sleutelbestaan**, de vierde op de **waarde**:

| lezer | plek | beslist op |
| --- | --- | --- |
| verifier, WF-055 | `spec/verifier/lib/checks.ml` r. 4057-4068 | sleutelbestaan |
| `ref` | `ref/lib/eval.ml` r. 80-81, gebruikt r. 1001-1003 | sleutelbestaan |
| ASP-backend | `backends/clingo/lib/translate.ml` r. 1914-1936 | sleutelbestaan |
| de case-lader | `ref/lib/case.ml` r. 317-331, `Some (_, Some g) -> g` | de **waarde** |

Gevolg: met een `null`-waarde laten de drie de clip door, weigert de lader een
kenmerktijdlijn, en levert een case zonder die tijdlijn een lege doorsnede —
**stil 0 in beide uitvoerders**. Dit is de helft van de stille-0-klasse die de
temporaliteitspoort níet sluit; de poort sluit de helft waar de sleutel
*ontbreekt*.

## De meetlijst: wie ziet de kwalificatie, en wat doet hij ermee

| # | artefact | plek | wat er met de kwalificatie gebeurt |
| --- | --- | --- | --- |
| 1 | ASP-backend | `translate.ml` r. 1914 (clip-arm) / r. 3440 (`Predicaat`, gevuld) | clip-arm: `eigenaar` en `periode` weggematcht met `_` → **stil**. `Predicaat`: het patroon eist `periode = None` → **luide weigering**. Twee naburige nodes, twee verschillende antwoorden. |
| 2 | `ref` | `eval.ml` r. 1000 (clip-arm), r. 1267 (scalaire kenmerkcheck), r. 1258-1266 (`Leeg`/`Gevuld`) | overal `_` → **stil**; alleen op de `Vergelijking` weigert ref luid (r. 1257, *"whole-period comparison is ch. 3 scope"*). `Predicaat.periode` is dus een **levende asymmetrie** met rij 1. |
| 3 | verifier | `checks.ml` r. 5171-5177 (WF-043, kenmerk-arm) | de regel bestáát — *whole-period qualifier on a non-time-dependent characteristic* — maar is **binnen de clip onvervulbaar**: WF-055 eist daar al dat het kenmerk temporeel is, dus WF-043's niet-temporele tak kan er per constructie niet vuren. De clip heeft geen periode-regel. |
| 4 | Drools-backend | `RegelSpraakRuntime.java` r. 1078/1102 (`rsGeheelPeriodeAlle`, `rsGeheelPeriode`), `DrlTranspiler.java` r. 525 | de §8.2-vorm is **geïmplementeerd**, en wel op de **`Vergelijking`** — niet op het kenmerk. Er is dus echte semantiek voor "gedurende de gehele periode", op een andere node. |
| 5 | emitters + corpus | `IrEmitter.java` r. 623, `frontends/catala/lib/emit.ml` r. 119-120 | beide emitters schrijven de `GeheelPeriode`-node uit. Het corpus draagt er **5** (gemeten 2026-09-06), alle vijf op een `Vergelijking`: 2 in `corpus/golden/jutterswet-vormenkabinet` (`resolvedRegels` 4 en 5), 3 in de negatief-zone. **Geen enkele op een `Kenmerk`.** |
| 6 | RegelSpraak-frontend | `RuleChecker.java` r. 1985-1993 (`kenmerkPredicaat`), r. 1898-1905 (`keurGeheelPeriode`) | de frontend **produceert de kenmerk-variant wel degelijk**: de §8.2-kwalificatie wordt op een `Conditie.Kenmerk` gezet zodra de onderwerp-toets hem temporeel vindt. De vorm is dus bereikbaar vanuit de brontaal; alleen het corpus draagt hem nog niet. |

Plus de case-lader-splitsing van flank 3 en 4: `case.ml` r. 285-289 (statische
lijst, toetst alleen verklaardheid) tegenover r. 317-331 (tijdlijnen, toetst de
granulariteits*waarde*).

## Kernwaarschuwingen — lees deze vóór je een regel schrijft

1. **De periode-vormen zijn ononderscheidbaar van `null`, ontkenning
   inbegrepen.** 20/20 gemeten. Wie hier "gewoon even implementeert" moet
   eerst beslissen wat `niet: true` op een kenmerk-clip *betekent*; er is geen
   bestaand gedrag om op terug te vallen, in geen enkele uitvoerder.
2. **Een brede WF-043-verbreding is over-afwijzing.** De §8.2-vorm op de
   `Vergelijking` **leeft** — 5 corpusnodes, twee executors, een frontend die
   hem produceert. Alleen de **kenmerk-arm** is smal genoeg om te weigeren, en
   die weigering kost de RegelSpraak-frontend een marker op een vorm die zij
   vandaag zonder klacht uitzendt (`RuleChecker.java` r. 1985-1993). Dat is een
   ontwerpbeslissing, geen opruimklus.
3. **Eenzijdig sluiten maakt een gate-6-divergentie.** Elke flank waar één
   uitvoerder gaat weigeren terwijl de ander doorrekent, is per definitie
   `ref ≢ clingo`. De artefacten bewegen hier samen of niet.
4. **De eigenaar-flank heeft een tweede helft die al opgeschreven staat.**
   `checks.ml` r. 5136-5142 noteert expliciet wat WF-034 *niet* sluit: of het
   genoemde type het kenmerk **verklaart**. Wie flank 1 beslist, beslist die
   helft mee — welke eigenaar gezaghebbend is, het veld of het onderwerp.

## Regressiegrens (gemeten 2026-09-06)

497 `ir.json`-bestanden in `corpus/golden`, `corpus/rood` en
`corpus/negatief`; 34 `Functie`-nodes met een filter (`gedurende`-clips),
verdeeld over 22 bestanden; 33 `Kenmerk`-nodes bínnen zo'n clip. Daarvan:

    met eigenaar : 0
    met periode  : 0
    met negatie  : 0

Corpusbreed: 189 `Kenmerk`-nodes, 3 met een `eigenaar` (alle drie in de
negatief-zone), 0 met een `periode`; 0 `Predicaat`-nodes met een `periode`.
**Elke flank kan dus vandaag zonder één corpusregressie worden gesloten** — en
dat getal moet bij het landen opnieuw geteld worden, niet overgeschreven.

## Acceptance

- **Per flank een genoteerde beslissing** — weigeren, implementeren, of
  uitdrukkelijk toestaan-met-reden — en per beslissing een artefact dat hem
  toetst. Een flank die "duidelijk is" maar niets toetst, is niet gedaan.
- **Rood eerst**: elke nieuwe weigering of berekening eerst aantoonbaar rood
  (de getuige faalt vóór de wijziging), daarna groen.
- Voor elke geweigerde vorm een `corpus/negatief/<wf###-slug>/` met de
  WF-regel die zijn mapnaam noemt; voor elke geïmplementeerde vorm een golden
  met absolute verwachtingen die **beide** uitvoerders halen.
- De regressiegrens hierboven opnieuw geteld en in het `Result:` genoteerd.
- Alle dertien gates groen.

May touch: `ref/**`, `backends/clingo/**`, `spec/verifier/**` (als
**voorstel** — beschermd pad), `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md` (als **voorstel** — beschermde paden),
`corpus/golden/**`, `corpus/negatief/**`, `backlog/`.

## Out of scope

- De `negatie: true`-tak van de clip. Dat is een eigen, al gemeten
  `ref ≢ clingo`-divergentie met een eigen kaart
  (`20260906-gedurende-complementwandeling.md`).
- De temporaliteitspoort zelf: die staat en spiegelt `ref` exact.
- De §8.2-`Vergelijking`-vorm herbouwen. Die leeft en werkt; deze kaart gaat
  over de **kenmerk**-arm ernaast.


---

## Result (geland 2026-09-09; fixronde 2026-09-10)

Alle vier de flanken beslist en uitgevoerd volgens de vier
maintainerbesluiten van 2026-09-09. De fixronde van 2026-09-10 sloot één
ernstig scope-gat (E1) en corrigeerde drie tekstpunten en vier observaties.
**Alle dertien gates groen.**

### Per flank

**Flank 1+2 — WF-097, één regel met twee armen, over ALLE DRIE de clips:
WEIGEREN.** Een `Kenmerk`-node die als (of in) de conditie van een
`gedurende`-clip staat, mag geen `eigenaar` en geen `periode` dragen, de
**ontkende** periode inbegrepen.

*Het scope-gat en zijn herstel (E1).* De regel landde eerst op de **totaal**-clip
alleen. De pro-rata-vouw (ch. 3.7, WF-056) en de dagentelling (ch. 3.8, WF-058)
dragen dezelfde node in hetzelfde veld en matchten beide velden nog met `_` weg;
zij bleven een dag lang stil doorrekenen. De regel dekt nu de drie clips als een
**gesloten lijst**, en de **vierde** lezing van `Functie.filter` — de
die/dat-subselectie — is uitdrukkelijk **geen** clip en blijft ongemoeid (daar
zijn eigenaar en periode gewone conditievelden).

*Rood eerst, twee keer.* Op de pre-arm binary van de totaal-clip (md5
`5111932e2d1e51de144290ab16abab00`): drie één-veld-verdraaiingen van
`jutterswet-tegenwacht`, alle drie verifier-PASS, **36 van 36** replays
onveranderd. Op de kop `99416a4`, die voor de andere twee clips de pre-arm
binary ís: zes één-veld-verdraaiingen van `anchor/jutterswet-borgdeel` en
`anchor/jutterswet-dagentelling`, alle zes verifier-PASS, **60 van 60** replays
onveranderd. Samen **96 replays, 96 onveranderde antwoorden**.

*Gedeelde helper, als eis en niet als notitie.* Elk van de drie artefacten
implementeert de afscherming **eenmaal** — verifier `wf097_clip_qualifications`,
ref en clingo `clip_member_qualifications` — en bereikt de drie clips vanaf
**twee of drie aanroepplekken**, al naar gelang hoe dat artefact ze zelf al
onderscheidt: de verifier heeft er **twee** (zijn pro-rata- en dagentellingarm
zijn één kind-gestuurde tak, dus één aanroep dekt beide; de totaal-clip, die op
VORM wordt herkend, roept dezelfde helper apart aan), ref en clingo elk
**drie**. Wat de regel eist is de gedéélde helper, niet een bepaald aantal
aanroepen: losse kopieën lopen uiteen, en dat is precies wat hier gebeurde.

*Negatiefs, zeven.* Drie op de totaal-clip (composiet-lid met eigenaar; vlak lid
met periode, beide polariteiten) en vier nieuwe: per resterende clip één per arm
(`wf097-prorata-kenmerk-{eigenaar,periode}`,
`wf097-dagentelling-kenmerk-{eigenaar,periode}`). De **ontkende** polariteit is
bewust niet per clip herhaald: dat is geen aparte arm maar een tak binnen de
gedeelde arm, al twee keer gepind op de totaal-clip; een derde en vierde kopie
zou samen met die twee vallen en niets nieuws toetsen. Wat de vier nieuwe wél
toetsen en de oude niet konden, is de **aanroepplek per clip**.

**Flank 3 — de lader weigert luid, in alle drie de lezers.** *Rood eerst*
(binary van commit `1907380`), de kaartmeting exact gereproduceerd:

    ref     MISMATCH: J1.dubbelwachtsom = 0, expected 30
    clingo  MISMATCH: J1.dubbelwachtsom = [0], expected 30

plus de zuster op `jutterswet-tegenwacht` (`tegenwachtsom = 0, expected 30`).
De Drools-CaseRunner gaf `EXCLUDE` — erger dan het klinkt: dat archiveert een
kapotte case onder "een vorm die de backend niet draagt". Na de klem weigeren
alle drie luid, met kenmerk én reden. ref en de ASP-backend delen de lezer
(`backends/clingo` hangt aan `uchar-ref` en roept `Uchar_ref.Case.load_file`
aan); Drools krijgt een harde `IllegalArgumentException` zoals zijn spiegelarm
er al een had. Kosten: **0 van 482** cases (golden + rood + toka) leverde een
temporeel kenmerk statisch.

Dit is case-formaat-semantiek, geen WF-regel. Het case-schema **kan** de vorm
niet uitsluiten — het valideert het case-document alleen en ziet de
IR-declaraties nooit; dat staat nu bij het veld `characteristics` in
`spec/schema/uchar-case-v0.schema.json` en in `RUNTIME-SEMANTICS.md` naast de
alinea over de twee case-conventies.

**Flank 4 — schema én verifier, dubbel dicht.** Eerst gemeten, zoals gevraagd:
**0** corpusdocumenten droegen een `kenmerkGranulariteit` met waarde `null`, de
RegelSpraak-emitter bewaakt zijn `put` met een expliciete `if (gk != null)` en
de Catala-emitter zou `Null` schrijven maar zijn map is door constructie leeg.
Er bewoog niets, dus: `null` uit de enum én een tweede arm onder WF-059.

De arm zit onder **WF-059**, niet onder WF-097 — gemotiveerd: WF-059's eerste
arm is exact dezelfde vorm één zijtabel verderop ("de wire laat `null` toe in de
attribuutmap, maar een datatype-loze declaratie splitst de uitvoerders op verder
schoon invoerdocument"), inclusief dezelfde dubbele structuur van regel +
caselezer-als-vangnet. WF-097's onderwerp is de stille kwalificatie op een
clip-lid; die twee bundelen zou van WF-097 twee onverwante regels maken.

De meting is **scherper dan de kaart aannam**: geen stille 0. Met de tijdlijn
geleverd sterft de case bij het laden met een melding die zijn eigen document
tegenspreekt; zonder de tijdlijn loopt hij dóór en antwoordt **60 en 120 waar 30
en 30 gepind staat** — identiek fout in beide uitvoerders. Negatief:
`wf059-kenmerk-zonder-granulariteit`.

**Flank-overstijgend.** Twee kaartjes geschreven:
`backlog/new/20260909-eigenaar-verklaart-kenmerk.md` (de veld-of-onderwerp-vraag,
met de hertelde meting) en `backlog/new/20260909-attribuutgranulariteit-null.md`
(O1, lage prioriteit — zie hieronder).

### De zeven arm-exacte mutanten

Elk recept is één treffer in zijn bestand (machinaal gecontroleerd vóór gebruik).

| # | bestand | recept | wat er vrijkomt |
| --- | --- | --- | --- |
| M1 | `checks.ml` | `(match eigenaar with` → `(match (if true then None else eigenaar) with`, in de drieregelige aanhef `        (match eigenaar with / | Some e -> / [ v "WF-097" at` van de gedeelde helper | **precies de drie eigenaar-getuigen**, over alle drie de clips |
| M2 | `checks.ml` | idem voor `periode`, aanhef `        @ (match periode with / | Some p -> / [ v "WF-097" at` | **precies de vier periode-getuigen**, over alle drie de clips |
| M3 | `checks.ml` | het drieregelige anker `            | Some _ -> [])` + `          ot.kenmerk_granulariteit)` + `    ctx.doc.objecttypen` → middelste regel `          [])` | alleen `wf059-kenmerk-zonder-granulariteit` |
| M4 | `case.ml` | `if List.exists (fun (k, _) -> norm k = norm c) ot.kenmerk_granulariteit then` → `if false then` | de flank-3-getuige valt terug op `= 0, expected 30`; de clingo-suite faalt **1** check |
| M5 | `checks.ml` | in de aanroepplek `(Tijdsevenredig_deel \| Aantal_dagen_in), Some c` → `Tijdsevenredig_deel, Some c` | **precies de twee dagentelling-getuigen** |
| M6 | `checks.ml` | dezelfde aanroepplek → `Aantal_dagen_in, Some c` | **precies de twee pro-rata-getuigen** |
| M7 | `checks.ml` | `clip_cond_ok c @ wf097_clip_qualifications (sub "filter") c)` → `clip_cond_ok c)` | **precies de drie totaal-clip-getuigen** |

M1/M2 bewijzen dat de arm écht gedeeld is (één armbewerking beweegt alle drie de
clips); M5/M6/M7 bewijzen dat elke clip apart bereikbaar-en-gepind is — M5 en M6
door de kind-gestuurde tak te **versmallen** tot één van de twee folds (er valt
daar geen aanroep te schrappen zonder beide te raken), M7 door de aanroep van de
totaal-clip wél te schrappen. Na herstel is
`git status` leeg.

*Over de md5-pin (K3).* De pin `14fc9dfc56e3a067f586a48d08d36604` uit de eerste
Result hoorde bij commit `8c91c7e`, niet bij de kop: OCaml bakt bronposities in,
dus een commentaarregel erbij verandert de binary. De pins in dit verslag zijn
daarom gedateerd per commit in plaats van als één getal gepresenteerd. De
pre-arm binary van de totaal-clip (`5111932e…`) hoort bij aftakking `196ccea`;
de pre-arm binary van de andere twee clips is de kop `99416a4`.

*Over recept M3 (K2).* Het eerste verslag schreef M3 af als `ot.kenmerk_granulariteit)`,
en dat komt 3× voor. Het uitgevoerde recept gebruikte altijd het drieregelige
anker hierboven; alleen de opschrijving was onnauwkeurig. Alle zeven recepten
zijn nu machinaal op uniciteit getoetst vóór gebruik.

### Wat de twee uitvoerders NIET garanderen (K1)

De eerste Result zei "beide uitvoerders spiegelen de weigering woordelijk, dus
gate 6 kan er niet op splitsen". De tweede helft daarvan is **onjuist** en is
vervangen door de meting. Zelfde woorden, niet hetzelfde moment: ref is lui,
de ASP-vertaling gretig. Met de schending achter een onware regelvoorwaarde:

    verifier  WF-097 FAIL /resolvedRegels/1/waarde/filter: whole-period qualifier …
    ref       oracle: PASS (2 expectations)          <-- bereikt de regel nooit
    clingo    TRANSLATION FAULT: whole-period qualifier on the gedurende clip member …

Zelf nagemeten (2026-09-10), én nagemeten dat het **geërfd** is: dezelfde
opstelling met een WF-055-schending geeft exact dezelfde drie uitkomsten. Het is
een staande luiheid-versus-gretigheid van het paar, geen gevolg van deze regel.
Begrensd, niet opgelost: de verifier wijst zo'n document af, dus het kan geen
enkele gated zone in, en gate 6 vergelijkt alleen verifier-schone documenten.
Zo staat het nu in de clausule, de registerrij, het clingo-commentaar en
`RUNTIME-SEMANTICS.md`.

### Hertelde regressiegrens (2026-09-10, bij het landen)

Bij aftakking → na het landen, over `corpus/golden` + `corpus/rood` +
`corpus/negatief`:

    ir.json-documenten            534 → 542   (+8 negatiefs)
    Functie met filter (clips)     39 →  55   in 25 → 33 bestanden
    Kenmerk BINNEN een clip        37 →  57   eigenaar 0 → 3, periode 0 → 4
    Kenmerk corpusbreed           193 → 213   eigenaar 3 → 6, periode 0 → 4
    Predicaat met periode           0 →   0
    GeheelPeriode-nodes             5 →   9
    kenmerkGranulariteit null       0 →   1

Elk nieuw exemplaar is een **eigen getuige**: geen bestaand document droeg een
van de vier vormen, dus **nul over-afwijzing** over de vier zones. De
§8.2-familie is exact onbewogen — `GeheelPeriode` per drager-node geteld:
**`Vergelijking` 5 vóór en 5 ná**, en alle vier de nieuwe zitten op de nu
verboden `Kenmerk`-positie (O3; de eerste Result telde die vier abusievelijk mee
alsof de familie gegroeid was). De kaart telde 2026-09-06 nog 497/34/33/189.

### Gates (aanvang → eindkop)

1 PASS · 2 **192** → **192** (de schema-aanscherping kost geen document) ·
3 192 golden / **342 → 350** negatief · 4 **453** → 453 · 5 **427** → 427
(2 zonder case) · 6 **427** → 427, suite 83 checks (3 nieuwe) · 7 391 / 36
survivors — onbewogen · 8 **26** rood — onbewogen · 9 201 checks / 12
catala-goldens — onbewogen · 10 173 tests / 38 rgs-goldens — onbewogen ·
11 29 PLAYED / 0 EXCLUDE — onbewogen · 12 played **119 → 119**, channel floor
47, JUnit **237 → 239** · 13 74 twists / 15 goldens / 0 survivors — onbewogen.
Daarnaast `test_ref` 489 gewerkte voorbeelden groen.

**Geen enkel gate-script is gewijzigd.** Dat was de aangekondigde mogelijkheid
en hij deed zich niet voor: er is geen nieuwe golden (dus `EXPECTED_PLAYED`
blijft 119 en de kanaallijst onaangeroerd) en geen nieuwe rode case (dus de
Drools-uitsluitingslijst in `scripts/check-rood.sh` blijft zoals hij was).

### Wat niet gedaan is, expliciet

- **De scalaire kenmerk-`periode` buiten de clip** — de §8.2-familie. Buiten de
  snit per besluit; de RegelSpraak-frontend produceert die vorm uit de bron, dus
  weigeren is een producent-snijdende beslissing met een eigen kaart.
- **`Predicaat.periode`** (O4), gemeten 2026-09-10. Op een **niet-temporele**
  operand vangt de verifier hem (`WF-043 FAIL`), en dáár splitsen de uitvoerders:
  `ref oracle: PASS` — hij matcht de kwalificatie stil weg — tegen
  `clingo TRANSLATION FAULT`. Op een **temporele** operand, waar WF-043 niet
  reikt, is de verifier stil maar faalt ref alsnog, om een **andere** reden
  ("temporal attribute in a scalar condition is refused (ch. 3.3)"). Een
  verifier-schone getuige waar ref stil een antwoord geeft is dus **niet
  gevonden**; het corpus draagt 0 `Predicaat`-nodes met een periode. Eerlijk
  gezegd: hier is een echte stille tak in ref, maar geen document dat hem
  bereikt zonder door een andere regel te worden gevangen.
- **`attribuutGranulariteit` met waarde `null`** (O1) — eigen kaartje
  `backlog/new/20260909-attribuutgranulariteit-null.md`, met **lage prioriteit
  benoemd**, want de ontbrekende meting is nu gedaan en wijst uit dat dit
  **niet** flank 4's klasse is: er is geen stil verkeerd antwoord. Wel: een
  `CASE ERROR` die het document tegenspreekt, en — bij scalaire levering — twee
  uitvoerders met twee verschillende diagnoses (`ref` "without a granularity"
  tegen `clingo` "needs the full timeline"). 0 van de 572 documenten (vier zones
  plus de pariteitszone, geteld 2026-09-10 op de eindkop) draagt er een.
- **De `negatie: true`-tak van de clip** — eigen kaart, ongemoeid.
- **`ref/test/test_ref.ml` draait door geen enkele gate.** Opgemerkt, niet
  opgelost (buiten deze snit): de suite is groen (489), maar een regressie erin
  zou geen gate rood maken. De flank-3-getuige staat daarom in de **clingo**-suite,
  die gate 6 wél draait — geen omweg: de lezer is gedeeld, dus het is even goed
  de bewijslast van die backend.

`git status --short` leeg; de branch blijft **ONGEMERGED**. Beschermde paden
aangeraakt als **voorstel**: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/verifier/`, `spec/schema/`.

**May touch IS verbreed, op twee punten.** De kaart hierboven noemt
`ref/**`, `backends/clingo/**`, `spec/verifier/**`, `spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md`, `corpus/golden/**`, `corpus/negatief/**` en
`backlog/`. Daarbuiten geraakt, beide op instructie van de vier
maintainerbesluiten van 2026-09-09 die deze kaart uitvoerde:

- **`backends/drools/**`** — besluit 3 eist dat de lader luid weigert "in alle
  drie de lezers", en de derde lezer is de Drools-CaseRunner. Aangeraakt:
  `cases/CaseRunner.java` (uitsluitend de leesklem: één `Excluded` werd een
  harde fout) plus één additief testbestand. Transpiler en loader **niet**
  aangeraakt, zoals de parallel lopende kaart `card/onopgelost-zijkanaal`
  vereist — nagegaan op de diff, en de merge-boom is conflictvrij.
- **`spec/schema/**`** — besluit 4 eist "SCHEMA ÉN VERIFIER sluiten de null".
  Aangeraakt: `regelspraak-ir-v0.schema.json` (de enum) en, voor de
  vastlegging die besluit 3 vroeg, één `description` in
  `uchar-case-v0.schema.json`. Beide als **voorstel** — beschermd pad.

`corpus/rood/**` was in de opdracht eveneens vrijgegeven maar is **niet**
gebruikt: een rode case moet zijn case-bestand kunnen LADEN vóór de
expected-fault-tak, en flank 3's getuige faalt juist bij het laden, dus die
zone kon hem niet dragen. Geen enkel gate-script gewijzigd; geen golden- of
rood-bestand gewijzigd.
