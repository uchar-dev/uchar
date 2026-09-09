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

## Result (geland 2026-09-09)

Alle vier de flanken beslist en uitgevoerd volgens de vier
maintainerbesluiten van 2026-09-09. **Alle dertien gates groen.**

### Per flank

**Flank 1+2 — WF-097, één regel met twee armen: WEIGEREN.** Een
`Kenmerk`-node die als (of in) de conditie van een `gedurende`-clip staat,
mag geen `eigenaar` en geen `periode` dragen, de **ontkende** periode
inbegrepen. *Rood eerst*, op de pre-arm binary (md5
`5111932e2d1e51de144290ab16abab00`): drie één-veld-verdraaiingen van
`jutterswet-tegenwacht` waren verifier-PASS én gaven in **36 van de 36**
replays (3 verdraaiingen × 6 cases × {ref, clingo}) het ONVERDRAAIDE
antwoord — de kaartmeting van 20, bevestigd en verbreed. Groen erna: één
falende regel per document, en beide uitvoerders spiegelen de weigering
**woordelijk**, zodat gate 6 er niet op kan splitsen. Negatiefs:
`wf097-clip-kenmerk-eigenaar` (composiet-lid), `wf097-clip-kenmerk-periode`
en `wf097-clip-kenmerk-periode-ontkend` (vlak clip-lid, beide polariteiten).
De scalaire kenmerk-periode buiten de clip blijft **buiten de snit** — de
frontend produceert die vorm uit de bron — en de §8.2-`Vergelijking`-vorm
eveneens (7 corpusnodes nu, waarvan 5 bestaand: hij leeft). WF-043's
kenmerk-arm is niet verbreed.

**Flank 3 — de lader weigert luid, in alle drie de lezers.** *Rood eerst*
(binary van commit `1907380`), de kaartmeting exact gereproduceerd:

    ref     MISMATCH: J1.dubbelwachtsom = 0, expected 30
    clingo  MISMATCH: J1.dubbelwachtsom = [0], expected 30

plus de zuster op `jutterswet-tegenwacht` (`tegenwachtsom = 0, expected
30`). De Drools-CaseRunner gaf `EXCLUDE` — erger dan het klinkt: dat
archiveert een kapotte case onder "een vorm die de backend niet draagt".
Groen erna: `CASE ERROR` in ref en in de gedeelde clingo-caselezer (het is
één lezer — `backends/clingo` hangt aan `uchar-ref` en roept
`Uchar_ref.Case.load_file` aan), en een harde
`IllegalArgumentException` in de Drools-CaseRunner, zoals zijn spiegelarm
er al een had. Elke melding noemt het kenmerk én de reden. Kosten: **0 van
482** cases (golden + rood + toka) leverde een temporeel kenmerk statisch.

Dit is case-formaat-semantiek, geen WF-regel. Het case-schema **kan** de
vorm niet uitsluiten — het valideert het case-document alleen en ziet de
IR-declaraties nooit, dus welke namen temporeel zijn is daar onkenbaar; dat
staat nu bij het veld `characteristics` in
`spec/schema/uchar-case-v0.schema.json` en in `RUNTIME-SEMANTICS.md` naast
de alinea over de twee case-conventies.

**Flank 4 — schema én verifier, dubbel dicht.** Eerst gemeten, zoals
gevraagd: **0** corpusdocumenten droegen een `kenmerkGranulariteit` met
waarde `null`, de RegelSpraak-emitter bewaakt zijn `put` met een expliciete
`if (gk != null)` en de Catala-emitter zou `Null` schrijven maar zijn map is
door constructie leeg (`kenmerk_granulariteit = []`). Er bewoog dus niets.
`null` is uit de enum verdwenen én WF-059 kreeg een tweede arm.

De arm zit onder **WF-059**, niet onder WF-097 — gemotiveerd: WF-059's
eerste arm is *exact dezelfde vorm* één zijtabel verderop ("de wire laat
`null` toe in de attribuutmap, maar een datatype-loze declaratie splitst de
uitvoerders op verder schoon invoerdocument"), inclusief dezelfde dubbele
structuur van regel + caselezer-als-vangnet. WF-097's onderwerp is de stille
kwalificatie op een clip-lid; die twee bundelen zou van WF-097 twee
onverwante regels maken.

De meting is **scherper dan de kaart aannam**: het is geen stille 0. Met de
tijdlijn geleverd sterft de case bij het laden met een melding die zijn
eigen document tegenspreekt; zonder de tijdlijn loopt hij dóór en antwoordt
**60 en 120 waar 30 en 30 gepind staat** — identiek fout in beide
uitvoerders (één van de twee clips is ontkend; zonder perioden om te
complementeren degenereert hij naar de hele span). Negatief:
`wf059-kenmerk-zonder-granulariteit`.

**Flank-overstijgend.** `backlog/new/20260909-eigenaar-verklaart-kenmerk.md`
geschreven, met de hertelde meting als basis en de vraag veld-of-onderwerp
als genummerde open vraag met drie antwoorden en hun prijs.

### Arm-exacte mutanten (edit-recept in de tekst, elk één treffer)

| # | recept | gevolg |
| --- | --- | --- |
| M1 | `checks.ml`: `@ (match eigenaar with` → `@ (match (if true then None else eigenaar) with` | **alleen** `wf097-clip-kenmerk-eigenaar` wordt PASS |
| M2 | `checks.ml`: `@ (match periode with` → `@ (match (if true then None else periode) with` | **alleen** de twee periode-negatiefs worden PASS |
| M3 | `checks.ml`: de arm-2-bron `ot.kenmerk_granulariteit)` → `[])` | **alleen** `wf059-kenmerk-zonder-granulariteit` wordt PASS |
| M4 | `case.ml`: `if List.exists (fun (k, _) -> norm k = norm c) ot.kenmerk_granulariteit then` → `if false then` | de flank-3-getuige valt terug op `= 0, expected 30`; de clingo-suite faalt **1** check |

Elk mutant liet de andere drie getuigen ongemoeid; na herstel md5
`14fc9dfc56e3a067f586a48d08d36604` en `git status` leeg. (M1 en M2
compileerden in eerste opzet niet — `unused variable` — waardoor de meting
op de oude binary viel; opnieuw gedaan met een recept dat wél bouwt. De
eerste, mislukte poging is hierboven niet als bewijs geteld.)

### Hertelde regressiegrens (2026-09-09, bij het landen)

Bij aftakking → na het landen, over `corpus/golden` + `corpus/rood` +
`corpus/negatief`:

    ir.json-documenten            534 → 538   (+4 negatiefs)
    Functie met filter (clips)     39 →  47   in 25 → 29 bestanden
    Kenmerk BINNEN een clip        37 →  49   eigenaar 0 → 1, periode 0 → 2
    Kenmerk corpusbreed           193 → 205   eigenaar 3 → 4, periode 0 → 2
    Predicaat met periode           0 →   0
    GeheelPeriode-nodes             5 →   7
    kenmerkGranulariteit null       0 →   1

Elk nieuw exemplaar is een **eigen getuige**: er is geen bestaand document
dat een van de vier vormen droeg, dus **nul over-afwijzing** over de vier
zones. De kaart telde 2026-09-06 nog 497/34/33/189; de clip-telling is sinds
toen door `jutterswet-tegenwacht` gegroeid, zoals de opdracht voorzag.

### Gates (aanvang → eindkop)

1 PASS · 2 **192** → **192** (de schema-aanscherping kost geen document) ·
3 192 golden / **342 → 346** negatief · 4 **453** → 453 · 5 **427** → 427
(2 zonder case) · 6 **427** → 427, suite 83 checks (3 nieuwe) · 7 391 / 36
survivors — onbewogen · 8 **26** rood — onbewogen · 9 201 checks / 12
catala-goldens — onbewogen · 10 173 tests / 38 rgs-goldens — onbewogen ·
11 29 PLAYED / 0 EXCLUDE — onbewogen · 12 played **119 → 119**, channel
floor 47, JUnit **237 → 239** · 13 74 twists / 15 goldens / 0 survivors —
onbewogen. Daarnaast `test_ref` 489 gewerkte voorbeelden groen.

**Geen enkele gate-script is gewijzigd.** Dat was de aangekondigde
mogelijkheid en hij deed zich niet voor: er is geen nieuwe golden (dus
`EXPECTED_PLAYED` blijft 119 en de kanaallijst onaangeroerd) en geen nieuwe
rode case (dus de Drools-uitsluitingslijst in `scripts/check-rood.sh` blijft
zoals hij was).

### Wat niet gedaan is, expliciet

- **De scalaire kenmerk-`periode` buiten de clip** — de §8.2-familie. Buiten
  de snit per besluit; de RegelSpraak-frontend produceert die vorm uit de
  bron, dus weigeren is een producent-snijdende beslissing met een eigen
  kaart. Gemeten: 2 corpusnodes, allebei mijn eigen clip-getuigen; buiten de
  clip nul.
- **`attribuutGranulariteit` met waarde `null`.** Dezelfde naad één zijtabel
  verderop, en die naad is er structureel écht (`attr_temporal` leest de
  SLEUTEL, de attribuut-arm van de caselezer leest de WAARDE). Gemeten: 0 van
  568 documenten (incl. `toka/`) draagt er een. Wat ontbreekt is de
  **uitgevoerde** meting; op het bewijs van een zustertabel weigeren zou
  over-afwijzing zijn. Benoemd in de spec-clausule, de registerrij en het
  verifier-commentaar als klaarliggende volgende stap.
- **De `negatie: true`-tak van de clip** — eigen kaart, ongemoeid.
- **`ref/test/test_ref.ml` draait door geen enkele gate.** Opgemerkt, niet
  opgelost (buiten deze snit): de suite is groen (489), maar een regressie
  erin zou geen gate rood maken. De flank-3-getuige is daarom in de
  **clingo**-suite gezet, die gate 6 wél draait — en dat is geen omweg: de
  lezer is gedeeld, dus het is even goed de bewijslast van die backend.

`git status --short` leeg; de branch blijft **ONGEMERGED**. Beschermde
paden aangeraakt als **voorstel**: `spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md`, `spec/verifier/`, `spec/schema/`. May
touch is niet verbreed: `backends/drools` bleef beperkt tot de
CaseRunner-leesklem (transpiler en loader onaangeroerd, zoals de parallelle
kaart `card/onopgelost-zijkanaal` vereist), plus één additief JUnit-bestand.
