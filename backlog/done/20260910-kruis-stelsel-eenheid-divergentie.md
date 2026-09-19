# Een kruis-stelsel-eenheid met gelijke canonieke naam splitst de uitvoerders — op ELKE eenheidspositie

*(Bestandsnaam draagt nog "kruis-stelsel-eenheid-divergentie"; de kaart is
gemeten breder dan haar naam suggereert. Dit is een **eenheden-kaart**, geen
klem-kaart — zie §"Reikwijdte".)*

Goal: beslissen — en als spec-delta of als expliciet niet-doen vastleggen — wat
er gebeurt met een eenheid die convertibel is op **canonieke naam** maar in een
**ander gedeclareerd eenheidstelsel** zit dan de eenheid waartegen ze wordt
gekruist. Vandaag laat de verifier zo'n document door, rekenen `ref` en Drools
het uit, en weigert de ASP-backend het te vertalen. Dat is een levende
divergentie op een **verifier-schoon** document, en ze zit **niet** op één
positie maar op alle vier de eenheidsposities die het IR kent.

## De vorm en de meting (2026-09-10)

Twee stelsels, allebei volledig gedeclareerd in `eenheidstelsels`:

- `Gewicht` met `kg` (canoniek `kg`, factor 1);
- `Massa` met `kg` (canoniek `kg`, factor 1) en `t` (canoniek `kg`, factor 1000).

Een `Begrensd` met de basis in `Gewicht`-`kg` en de grenzen in `Massa`-`t`
(`minimaal 10 t`, `maximaal 100 t` over een `kg`-basis):

| consument | uitkomst |
|---|---|
| verifier | **PASS** — de canonieke namen zijn gelijk, dus WF-099 en `units_convertible` laten het door |
| `ref` | herschaalt: 10 t = **10000** kg |
| Drools, beide takken | **10000** |
| ASP-backend (clingo) | `TRANSLATION FAULT: non-identical units under a Begrensd bound are not translated` |

**"Twee tegen één" is te grof gezegd, en de nuance telt.** `ref` deelt het
convertibiliteitspredicaat **letterlijk** met de verifier: `ref/lib/arith.ml`
regel 80 is `let convertible a b = Ir_v0.Checks.units_convertible a b`. Dat
`ref` de verifier volgt is dus geen onafhankelijke bevestiging maar dezelfde
code die tweemaal spreekt. De onafhankelijke stem is **Drools** — een eigen
implementatie (`Eenheidsconflict.eisHerschaalbaar`) die op de canonieke naam
kijkt en het stelsel negeert. De stand is dus preciezer: `ref` (zelfde
predicaatcode als de verifier) **plus** Drools (onafhankelijk) tegenover de
ASP-backend.

## Reikwijdte: dit is een eenheden-kaart, niet een klem-kaart

Nagemeten 2026-09-10 op alle vier de posities waar een van de drie eenheden
kruist. Dezelfde kruis-stelsel-vorm splitst **identiek** op elke positie:

| positie | verifier | `ref` | ASP-backend |
|---|---|---|---|
| WF-099 klem-grens | PASS | 10000 | `… under a Begrensd bound …` |
| WF-035 additieve join (`kg`-attribuut plus `10 t`) | PASS | **10005** | `… under an additive join …` |
| WF-036 vergelijking (`kg`-attribuut groter dan `10 t`) | PASS | evalueert (leeg) | `… under a comparison …` |
| WF-023 toekenning (doel in `Massa`-`kg`, waarde uit `Gewicht`-`kg`) | PASS | 5 | `… under the write to "bandbreedte" …` |

**En het splitst zelfs ZONDER omrekenfactor.** `Massa`-`kg` (factor 1, canoniek
`kg`) als grens op een `Gewicht`-`kg`-basis: verifier PASS, `ref` `oracle: PASS
(5 expectations)` — dus `ref` ziet de twee eenheden als identiek en herschaalt
niets — terwijl de ASP-backend nog steeds `TRANSLATION FAULT` geeft. De
divergentie hangt dus **niet** aan een factor maar aan het stelsel-veld zelf.

Daarmee is acceptance-punt 2 hieronder ("meet eerst of de zusters dezelfde
scheefstand hebben") **al beantwoord**: dat doen ze, alle drie, met hun eigen
foutboodschap. De bouwkaart hoeft die meting niet over te doen, alleen te
bevestigen op de dan geldende HEAD.

**Oorzaakduiding, verfijnd.** Twee mechanismen samen, en de verfijning verklaart
precies waarom ook de factorloze vorm splitst:

1. `units_identical_ok` in de ASP-backend vergelijkt **hele eenheid-records
   structureel** (`Some a, Some b when a <> b`), en `stelsel` is gewoon één van
   de velden van dat record. Twee eenheden met dezelfde afkorting, dezelfde
   canonieke naam en dezelfde factor zijn dus **ongelijk** zodra hun stelsel
   verschilt.
2. `Normalise.document`, dat vóór de vertaling draait, herschaalt alleen binnen
   het stelsel van het **schrijfdoel**. Een kruis-stelsel-literal wordt daardoor
   nooit aangeraakt en houdt zijn eigen stelselveld.

Binnen één stelsel maakt (2) de records gelijk, en daar valt de identiteitstoets
samen met convertibiliteit — precies waarom WF-099 op convertibiliteit kon
landen. Stelsel-overschrijdend doet (2) niets, dus (1) vuurt, met of zonder
omrekenfactor. `ref` (`units_convertible`) en Drools
(`Eenheidsconflict.eisHerschaalbaar`) kijken allebei alleen naar de **canonieke
naam** en negeren het stelsel, dus die twee rekenen door.

## De vraag die deze kaart moet beslissen

Twee uitwegen, en het kader is de zuster-WF's:

1. **De wire sluiten** — een verifier-regel (of een arm van een bestaande
   eenhedenregel) die kruist-stelsel-paren afwijst, zodat de ASP-backend nooit
   een document ziet dat ze zou weigeren. Dan wordt de vraag: is "verenigbaar"
   canonieke naam **plus** stelsel? Dat is een echte verscherping van
   `units_convertible`, en ze raakt WF-023/035/036/099 tegelijk — die roepen
   allemaal hetzelfde predicaat aan. Kosten meten vóór het besluit.
2. **De vertaling verruimen** — de ASP-backend een exacte omrekenfactor laten
   dragen over stelselgrenzen heen, zodat hij `ref` en Drools volgt. Let op wat
   de foutboodschap zelf zegt over de reden dat hij dat niet doet: "ref restates
   with an exact conversion factor this integer encoding does not carry" — dus
   dit is werk aan de integer-codering/normalisatie, geen vlag.

Wat **niet** mag: het laten staan als stilzwijgende divergentie. Twee
uitvoerders die hetzelfde antwoord geven en een derde die weigert, op een
document dat alle gates passeert, is precies wat het differentiële vierkant
hoort te vangen.

## Prioriteitssignaal — levend, maar (voor zover gemeten) alleen handgebouwd

De divergentie is echt en reproduceerbaar, maar de **voorwaarde** ervoor komt in
geen enkel bestaand document voor. Gemeten 2026-09-10 over corpus én
pariteitszone: **67** documenten declareren twee of meer eenheidstelsels, en in
**0** daarvan delen twee stelsels een canonieke eenheidsnaam. Er is dus vandaag
geen gegatet document dat de vorm draagt, en geen bekende producentenroute.

Voorzichtig geformuleerd, want dit is een **niet-uitputtende** meting: er is
niet ge-audit of de RegelSpraak-frontend twee stelsels met een gedeelde
canonieke naam *kan* emitteren — alleen dat geen bestaand document het doet. Een
korte frontend-meting hoort bij stap 1 van deze kaart en kan het signaal beide
kanten op bijstellen.

## Acceptance

- de driehoeksmeting hierboven is gereproduceerd op de dan geldende HEAD, met
  echte uitvoer van alle drie de uitvoerders;
- de reikwijdtemeting is **bevestigd** op de dan geldende HEAD in plaats van
  overgedaan: dezelfde scheefstand staat al gemeten op WF-035, WF-036 en WF-023
  (zie "Reikwijdte"), dus wat de bouwkaart moet doen is nagaan dat die vier
  splitsingen er nog zijn — niet ze opnieuw ontdekken;
- kort gemeten is of de frontend de vorm kan produceren;
- de uitkomst is één van twee, expliciet: een verifier-regel met de volledige
  uitrusting (ID, verifier-check, geslaagde golden, reden-dragende
  `corpus/negatief/<wf###-slug>/`) — of een verruimde ASP-vertaling met een
  golden die de kruis-stelsel-vorm in het volle vierkant speelt;
- in beide gevallen wordt de "open"-passage in WF-099's clausule én registerrij
  bijgewerkt, want die verwijst nu naar deze kaart;
- gates 1, 2, 3, 5, 6 groen, en 12 als de gekozen weg Drools raakt.

May touch: `spec/IR-SPEC.md` en `spec/verifier/` (voorstel, beschermd),
`corpus/negatief/`, `corpus/golden/`, en bij uitweg 2 `backends/clingo/`.
`backlog/`.

Out of scope:
- WF-099 zelf: de regel staat, en haar keuze voor convertibiliteit is los van
  deze vraag gemeten en verdedigd.
- De eenzijdige-eenheidsparen-klasse (ch. 4.1) — andere open klasse, één besluit
  voor vier regels samen.
- De niet-gedeclareerd-stelsel-vorm: die is al dicht, WF-034 weigert hem
  (`unit 't' declares unit system 'Massa', which no declaration carries`).

---

## Result (2026-09-19)

### Het besluit: UITWEG 2 — de ASP-backend conformeert, de norm beweegt niet

Van de twee uitwegen is **de vertaling verruimd**, niet de wire gesloten. De
motivering is drieledig en geen van de drie is een smaakkwestie:

1. **De spec-keuze was al gemaakt.** WF-099 landde op *convertibiliteit op
   canonieke naam*, uitdrukkelijk en gemeten — de clausule verdedigt die keuze
   tegen identiteit met de constatering dat identiteit "een vorm zou weigeren
   die alle drie de uitvoerders spelen". `units_convertible` is een canonieke-
   NAAM-wet en zegt niets over het declarerende stelsel. Uitweg 1 zou dat
   predicaat verscherpen tot *naam + stelsel*, en dat raakt WF-023, WF-035,
   WF-036 en WF-099 tegelijk — vier regels die allemaal hetzelfde predicaat
   aanroepen. Een verscherping van de norm om één backend zijn gelijk te geven
   is de staart die met de hond kwispelt.
2. **Generiek boven special, méér functionaliteit.** Uitweg 1 verkleint de
   verzameling uitdrukbare documenten; uitweg 2 vergroot de verzameling
   uitvoerbare. Twee stelsels die een canonieke eenheid delen is een normale
   modelleervorm, en de frontend produceert hem (zie de frontend-meting
   hieronder).
3. **Twee onafhankelijke stemmen stonden al aan die kant.** `ref` deelt zijn
   predicaat letterlijk met de verifier, dus telt als één stem met de norm —
   maar Drools implementeert het onafhankelijk (`Eenheidsconflict.eisHerschaalbaar`,
   op canonieke naam, stelsel genegeerd) en rekent hetzelfde. De ASP-backend
   stond alleen, en wat hij handhaafde was geen normregel maar een eigenschap
   van zijn integer-codering die als hek was blijven staan.

**Dit is dus executor-conformiteit, geen normwijziging.** Geen verifier-, ref-
of spec-code is aangeraakt. Het besluit is genomen namens de gedocumenteerde
maintainerslijn; de spec-bijwerking is als registervoorstel geformuleerd en
NIET zelf geland (beschermde paden).

### Hermeting van de driehoek op deze HEAD (2026-09-19)

Vijf probes, elk uit `.rgs` door de eigen frontend geëmitteerd, telkens
`ruwgewicht = 5,00 kg` in stelsel `Gewicht`, en `10 t` / `Massa`-`kg` als de
kruisende eenheid. `VOOR` = HEAD 4a1e524e; `NA` = deze tak.

| # | positie | verifier | `ref` | Drools exec | Drools drl | clingo VOOR | clingo NA |
|---|---|---|---|---|---|---|---|
| P1 | WF-099 klemgrens (`min 10 t` over `kg`) | PASS | PASS — 10000,00 | PLAYED 1 | PLAYED 1 | `TRANSLATION FAULT: non-identical units under a Begrensd bound …` | PASS (2) |
| P2 | WF-035 additieve join (`ruwgewicht plus 10 t`) | PASS | PASS — 10005,00 | PLAYED 1 | PLAYED 1 | `… under an additive join …` | PASS (2) |
| P3 | WF-036 vergelijking (`ruwgewicht groter dan 10 t`, in een voorwaarde) | PASS | PASS — leeg | PLAYED 1 | PLAYED 1 | `… under a comparison …` | `… under a comparison …` |
| P4 | WF-023 toekenning (doel `Massa`-`kg`, waarde `Gewicht`-`kg`) | PASS | PASS — 5,00 | PLAYED 1 | PLAYED 1 | `… under the write to "bandbreedte" …` | PASS (2) |
| P5 | de FACTORLOZE vorm (`Massa`-`kg` tegen `Gewicht`-`kg`, factor 1) | PASS | PASS — 5,00 / 10,00 | PLAYED 1 | PLAYED 1 | `… under the write to "spiegelgewicht" …` | PASS (3) |

De vier splitsingen die de kaart opschreef staan er nog, met dezelfde
boodschappen, en de factorloze vorm ook. `ref` en beide Drools-takken bewegen
nergens — voor noch na.

**P3 is niet blijven staan om een stelselreden, en dat is gemeten.** Twee extra
probes:

- **P6** — *hetzelfde document binnen ÉÉN stelsel*: `Gewicht` met `kg` en
  `t`, een `kg`-attribuut tegen een `10 t`-literal in een **voorwaarde**.
  clingo weigert hem met exact dezelfde boodschap, **vóór én na**. De
  normalisatiepas herschrijft regelWAARDEN en geen voorwaarden; dat bereik is
  onveranderd en geldt binnen één stelsel precies zo.
- **P7** — *een kruis-stelselvergelijking die GEEN factor nodig heeft*
  (`Massa`-`kg` attribuut tegen een `1000 kg`-literal in `Gewicht`): VOOR
  `TRANSLATION FAULT: … under a comparison …`, NA `clingo: PASS`.

Daarmee is de conformiteitsclaim precies: **clingo behandelt de stelselgrens nu
niet anders dan de grens tussen twee eenheden binnen één stelsel** — niet
beter, niet slechter.

### De frontend-meting: ja, de vorm is produceerbaar

De kaart noemde dit niet-geaudit. Gemeten:
`RegelSpraakModel.resolveEenheid` loopt de stelsels in declaratievolgorde af en
neemt de eerste die de spelling draagt; `DefinitionResolver.resolveFactor`
kent elke basiseenheid haar eigen stelsel toe met zichzelf als canoniek. Twee
stelsels die allebei `kg` declareren zijn dus gewoon toegestaan, en dit
compileert probleemloos:

```
Eenheidsysteem Gewicht
    het kilogram (mv: kilogrammen) kg

Eenheidsysteem Massa
    het kilogram (mv: kilogrammen) kg kgm
    de ton (mv: tonnen) t = 1000 kg
```

`kg` en `kilogram` bereiken **Gewicht**, `t` bereikt **Massa**, en de tweede
afkorting `kgm` (de grammatica staat `eenheidAfkorting*` toe) bereikt
**Massa**'s kilogram — een record dat in élk veld gelijk is aan dat van
Gewicht behalve `stelsel`. Dat is letterlijk de factorloze vorm uit de kaart.
Het prioriteitssignaal gaat dus omhoog, niet omlaag: er was geen *bekende*
producentenroute, maar er is er wel degelijk een, en ze vraagt geen enkele
kunstgreep.

### De implementatie: de omrekentabel is de canonieke naam

Twee plekken in `backends/clingo`, samen +161/-72 regels (commentaar
meegeteld; de codewijziging zelf is klein), geen regel buiten de backend:

1. **`normalise.ml` — de tabel kruist stelsels.** De pas verzamelde haar
   eenheden uit `plain_units` van het stelsel van het schrijfdoel. Nu
   verzamelt `table_for_target` elke vlakke gedeclareerde eenheid van het hele
   document die volgens `Checks.units_convertible` in de doeleenheid
   herschaalt, ongeacht welk stelsel haar declareert, plus de doeleenheid
   zelf. `finest_of_stelsel` is daarmee `finest_of_units ~where` geworden: een
   vouw over één omrekentabel in plaats van over "een heel stelsel". Dat is
   niet alleen de verruiming maar ook een correctie — een stelsel mag
   eenheden met verschillende canonieke namen dragen, en hun factoren tellen
   verschillende canonieke eenheden, dus in één fijnste-vouw zijn ze
   onvergelijkbaar. Die worden nu uitgesloten in plaats van meegevouwen.
2. **`translate.ml` — het hek test dezelfde grootheid, niet hetzelfde record.**
   `units_identical_ok` vergeleek `eenheid`-records structureel, en `stelsel`
   is gewoon een veld. Nu is er één ontsnapping: `Normalise.same_quantity a b`
   — convertibel én `Arith.conv_factor a b = 1`. Die dekt precies de paren
   waarvoor de codering niets hoeft te dragen. Alles wat een vermenigvuldiging
   vergt blijft weigeren.

**Hoe de kruis-stelselfactor in de integer-codering past.** Hij past op
*exact dezelfde voorwaarden* als een factor binnen één stelsel, en krijgt geen
vrijstelling. De codering werkt met geschaalde gehelen; herschalen naar de
**fijnste** eenheid is altijd een vermenigvuldiging, dus `10 t → 10000 kg` is
`×1000` en het grondgetal `1000·10^d` blijft exact. De twee bestaande
bewijsverplichtingen gelden onverkort over de bredere tabel: (1) elke factor
moet een exacte korte decimale lezing hebben, (2) elke eenheid van de tabel
moet in een **positief geheel getal** naar de fijnste herschalen — anders een
luide weigering met reden, nu met de tabel benoemd
(`the canonical-kg conversion table (unit systems Gewicht, Massa) admits no
finest base unit: …`). Er is één verplichting **bij**gekomen: (3) beide zijden
moeten de canonieke eenheid daadwerkelijk NOEMEN. `units_convertible` is
bewust conservatief — een eenheid zonder canoniek én zonder afkorting (ch.
2.5's opake product) heet compatibel met alles — en zonder die eis zou de pas
die toestemming lezen als vergunning om een opake literal om te LABELEN naar
de doeleenheid. Onbekende convertibiliteit is geen omrekenfactor.

### De golden: `corpus/golden/jutterswet-stelselweging`

Vierbenig gespeeld: verifier PASS, `ref` PASS (37 verwachtingen), clingo PASS
(37), Drools `PLAYED 1` in **beide** takken. Handberekende absoluten eerst; de
uitvoerders hebben ze niet bijgesteld — alle vier groen bij de eerste run.

Twee stelsels, `Gewicht` (`kg`) en `Massa` (`kg`, `kgm`, `t`), allebei
canoniek `kg`. Zes regels kruisen de grens op alle vier de posities, met én
zonder omrekenfactor:

| regel | positie | paar | vraagt |
|---|---|---|---|
| `ondergewicht` | WF-099 klemgrens | `1 t` (Massa) op een `kg`-basis (Gewicht) | factor ×1000 |
| `spiegelgewicht` | WF-023 toekenning | `kg` (Gewicht) naar een `kgm`-doel (Massa) | factor 1 |
| `somgewicht` | WF-035 additieve join | `kg` (Gewicht) plus `kgm` (Massa) | factor 1 |
| `toeslaggewicht` | WF-035 additieve join | `kg` (Gewicht) plus `1 t` (Massa) | factor ×1000 |
| `lastgewicht` | WF-099 grens **én** WF-023 toekenning | `2 t`-grens (Massa) op een `kg`-basis (Gewicht), geschreven naar een `kgm`-doel (Massa) | allebei tegelijk |
| `keurgewicht` | WF-036 vergelijking | `kgm` (Massa) groter dan `1000 kg` (Gewicht) | factor 1 |

V4/V5 (1000,00 en 1000,01) klemmen de herschaalde drempel op de cent; V6 pint
de leeg-voortplanting (klem leeg, additief coalescerend: `somgewicht` 0,00 en
`toeslaggewicht` 1000,00). De NOTE noemt wat de waarden falsifiëren: wie niet
herschaalt antwoordt 250,00 waar 1000,00 staat, wie ×1000 toepast waar geen
factor is antwoordt 250000,00.

### Getuigen en mutatieproeven

`backends/clingo/test/test_clingo.ml`: **362 → 378 checks**, floor
(`EXPECTED_MIN_SUITE_CHECKS=83`) onaangeroerd. Toegevoegd:

- **het predicaat** — `same_quantity` zegt ja over de stelselgrens bij factor
  1 (en symmetrisch), nee bij factor 1000, nee bij een andere canonieke naam,
  en nee bij een opake eenheid aan één of aan beide zijden (verplichting 3);
- **de tabel** — de fijnste over `{kg@Gewicht, kg@Massa, t@Massa}` is een
  factor-1-`kg`, `t@Massa → kg@Gewicht` is ×1000 en `kg@Massa → kg@Gewicht` is
  ×1; een kruis-stelselverhouding van 1,5 faalt luid en de boodschap noemt
  **beide** stelsels;
- **de vier posities end-to-end** — één document met de WF-099-grens, de
  WF-023-toekenning, de WF-035-join en de WF-036-vergelijking over de
  stelselgrens vertaalt, `ref` pint 1000,00, en de uitgezonden klemregel
  draagt de herschaalde `(1000*100)` en niet de omgelabelde `(1*100)`.

De vier bestaande oproepplekken van de fijnste-vouw (Afstand, Termijn, Broken,
Inexact) draaien mee op de nieuwe tabelvorm: `finest_of_units ~where
(plain_units s)` in plaats van `finest_of_stelsel s`. Zelfde eigenschappen,
zelfde needles, geen enkele verzwakking — de reden voor de vormverandering
staat hierboven onder *De implementatie*, punt 1.

**Mutatieproef 1 — de stelselgrens weer dicht.** In `restates_into` één
conjunct erbij (`u.stelsel = tu.stelsel`), herbouwen:

```
FAIL kg@Gewicht and kg@Massa are one quantity (only the system differs)
FAIL same_quantity is symmetric across the system boundary
FAIL cross-system document translates (unexpected fault: non-identical units under a Begrensd bound …)
FAIL cross-system: the 1 t bound reaches the program restated to 1000, not relabelled to 1
FAIL mutant: a 500-kg ton still translates (unexpected fault: non-identical units under a Begrensd bound …)
test_clingo: 378 checks, 5 FAILURES
```
en de golden zelf: `TRANSLATION FAULT: non-identical units under a Begrensd
bound are not translated`.

**Mutatieproef 2 — omlabelen in plaats van herschalen** (`to_finest` geeft
altijd `Q.one`):

```
FAIL km -> m is x1000 (multiply-only integer)
FAIL jaar -> maand is x12 (non-power-of-ten multiply-only integer)
FAIL no-finest stelsel must fault, not silently pick
FAIL t@Massa -> kg@Gewicht is x1000 across the system boundary
FAIL a non-integer CROSS-SYSTEM ratio must fault, not silently pick
FAIL cross-system: the 1 t bound reaches the program restated to 1000, not relabelled to 1
FAIL mutant: the emitted bound follows the declared factor (500, not 1000)
test_clingo: 378 checks, 7 FAILURES
```
en de golden: `MISMATCH: V5.lastgewicht = [200], expected 100001` /
`MISMATCH: V6.toeslaggewicht = [100], expected 100000`.

Beide mutanten teruggedraaid; suite weer `378 checks, 0 failures`.

Bovendien draagt de suite de omgekeerde mutant permanent, als document in
plaats van als code: hetzelfde stuk IR met `Massa`'s canonieke naam op `kgm`
weigert luid (`non-identical units`), en met een tonfactor van 500 beweegt
zowel `ref` (500,00) als het uitgezonden grondgetal (`(500*100)`). Een
toekomstige verruiming die canonieke namen niet meer zou lezen, of die zou
omlabelen in plaats van herschalen, loopt daar op stuk.

### Corpussweep, beide Drools-takken

`java -jar <shaded> [--drl] <ir.json> <case*.json>` over elke golden, vóór
(golden weggenomen) en ná:

```
exec: 153 -> 154      drl: 126 -> 127
diff (beide takken):  132a133
                      > jutterswet-stelselweging	PLAYED 1
```

Precies één rapportregel beweegt, in beide takken. Geen enkele andere golden
verandert van kanaal of reden. (De Drools-backend leest de clingo-code niet;
de beweging is volledig de nieuwe golden.)

`bash scripts/check-clingo.sh`: `PASS (backend suite 362 → 378 checks, floor
83; 526 → 527 cases replay exactly through clingo — ref = clingo over the
corpus)`.

### Poortbewegingen (alle veertien groen vóór en ná)

| poort | vóór | ná |
|---|---|---|
| 2 ir-schema | 230 files | 231 files (floor 192) |
| 3 verifier | 230 golden clean | 231 golden clean |
| 4 case-schema | 554 case files | 555 case files |
| 5 oracle | 526 cases replayed | 527 cases replayed |
| 6 clingo | suite 362; 526 cases | suite 378; 527 cases |
| 7 noop | 490 cases refute | 491 cases refute |
| 10 regelspraak | 54 rgs goldens | 55 rgs goldens (byte-identiek) |
| 12 drools | played 153 | played 154; 49 non-PLAYED gepind; value-divergence 1 |
| 13 reftype | 108 twists / 22 goldens | 126 twists / 23 goldens (floor 74 ongewijzigd; 0 survivors; 0 buiten de twisttabel) |

### De veertien poorten, met exitcodes (eindstand, 2026-09-19)

```
1   scripts/check-self-contained.sh       exit 0   PASS
2   scripts/check-ir-schema.sh            exit 0   PASS (schema valid; 24 pattern self-test row(s); 231 file(s) validate and round-trip, floor 192)
3   scripts/check-verifier.sh             exit 0   PASS (231 golden clean; 395 negatief rejected for their declared rule, floor 350)
4   scripts/check-case-schema.sh          exit 0   PASS (schema valid; 555 case file(s) validate)
5   scripts/check-oracle.sh               exit 0   PASS (ref suite 643 worked examples, floor 489; 527 cases replayed exactly, floor 427)
6   scripts/check-clingo.sh               exit 0   PASS (backend suite 378 checks, floor 83; 527 cases replay exactly through clingo)
7   scripts/check-noop.sh                 exit 0   PASS (491 cases refute a do-nothing executor; 36 pinned survivors)
8   scripts/check-rood.sh                 exit 0   PASS (28 red cases fault observably in ref and clingo, floor 26)
9   scripts/check-catala.sh               exit 0   PASS (frontend suite 201 checks, floor 201; 12 catala goldens regenerate byte-identically)
10  scripts/check-regelspraak.sh          exit 0   PASS (JUnit 283 tests, floor 173; 55 rgs goldens re-emit byte-identically, floor 38)
11  scripts/check-toka-parity.sh          exit 0   PASS
12  scripts/check-drools.sh               exit 0   PASS (played 154 = EXPECTED_PLAYED; 49 non-PLAYED pinned by name; JUnit 455 tests, floor 239)
13  scripts/check-reftype-derivation.sh   exit 0   PASS (126 unit annotations twisted over 23 goldens, floor 74; 104 executor replays, 22 DRL-text PLAYED, floor 13; 0 survivors)
14  scripts/check-flogic.sh               exit 0   PASS (frontend suite 130 checks, floor 81; 2 flogic goldens re-emit byte-identically, floor 1)
```

Alle veertien waren ook groen op de uitgangs-HEAD (4a1e524e), zodat de
vergelijking betekenis heeft.

### De scriptdiff (`scripts/check-drools.sh`, gemandateerd, alleen de kanaaltelling)

```diff
@@ -539,7 +539,15 @@ EXPECTED_CHANNEL=(
 # and a NON-numeric one. Measured before and after over ALL goldens in BOTH branches: exec 151 -> 153,
 # DRL-text 126 -> 126 with its whole report byte-identical (this card does not touch DrlTranspiler), and
 # exactly these two report lines move — no other golden changes channel or reason.
-EXPECTED_PLAYED=153
+# 2026-09-19 (card kruis-stelsel-eenheid), 153 -> 154: `jutterswet-stelselweging` arrives as a NEW
+# GOLDEN and plays immediately. It carries the cross-system unit form — two declared unit systems
+# sharing a canonical name, crossed at all four unit positions (WF-023 write, WF-035 additive join,
+# WF-036 comparison, WF-099 clamp bound), with and without a conversion factor. The Drools backend
+# already answered that form on BOTH branches before the card; what the card changed is the clingo
+# translation, which used to refuse it. So the golden joins the square complete rather than red, and
+# this is the one report line that moves — measured before and after over ALL goldens in BOTH branches:
+# exec 153 -> 154, DRL-text 126 -> 127, no other golden changes channel or reason.
+EXPECTED_PLAYED=154
 
 # --- the list's own two guards (fixronde 2026-09-06) -------------------------------------------
 # The floor below compares each MEASURED golden against ITS pin, so a malformed list can weaken the
```

De reden-dragende `EXPECTED_CHANNEL`-lijst is niet aangeraakt: de nieuwe
golden speelt, dus hij hoort per definitie niet in de lijst van niet-spelers.

### May touch — gerespecteerd

`backends/clingo/**` (lib + test), `corpus/golden/jutterswet-stelselweging/`
(nieuw), `scripts/check-drools.sh` **uitsluitend** de kanaaltelling
(gemandateerd), en dit kaartbestand. Niets anders is aangeraakt — in het
bijzonder geen `spec/verifier/`, geen `ref/`, geen `spec/*.md`.

### Registervoorstellen (NIET geland — beschermde paden)

**(1) `spec/IR-SPEC.md`, WF-099-clausule.** Vervang de passage die met
`What IS open is the CROSS-SYSTEM bound whose system is fully declared:`
begint en eindigt op
`` `backlog/new/20260910-kruis-stelsel-eenheid-divergentie.md`. `` door:

> *Closed (2026-09-19), and it was the ASP BACKEND that moved, not the rule:*
> the CROSS-SYSTEM bound whose system is fully declared — with `Massa`
> declared beside `Gewicht` (`kg` factor 1, `t` factor 1000, both canonical
> `kg`), a `t` bound over a `kg` base in `Gewicht` — is convertible by
> canonical name, so the verifier and this rule admit it, and on 2026-09-10
> `ref` restated to 10000 and the Drools backend answered 10000 on both
> branches while the ASP backend refused to translate, its per-system
> normalisation finding no identity ACROSS two systems. That was executor
> divergence and not an open normative question: this rule had already chosen
> convertibility-on-canonical-name, and the backend was enforcing a fence the
> norm does not carry. It now gathers its conversion table by canonical name
> across the declared systems and lets a pair whose exact factor is 1 through
> its identity fence. The integer encoding is untouched: a cross-system factor
> that is not a positive integer into the finest unit stays the same loud
> refusal a same-system one gets. Re-measured 2026-09-19, one probe per
> position, `ref` and both Drools branches unmoved throughout: the WF-099
> bound, the WF-035 additive join and the WF-023 write all go from
> `TRANSLATION FAULT: non-identical units under …` to the pinned values, and
> so does the factorless pair (`kg` in `Massa` against `kg` in `Gewicht`).
> The WF-036 comparison moves for the factorless pair and NOT for the
> factor-1000 one, and that remainder is not a system fence: the normalisation
> pass rewrites rule VALUES and not conditions, so the IDENTICAL same-system
> document (a `kg` attribute against a `10 t` literal in a condition, one
> declared system) is refused by the same message before and after. Golden
> `jutterswet-stelselweging` carries the four crossed positions in the full
> square, with and without a factor;
> `backlog/done/20260910-kruis-stelsel-eenheid-divergentie.md`.

**(2) `spec/IR-SPEC.md`, WF-099-registerrij (de rij die met `| WF-099 |`
begint).** Vervang de zin die begint met `The open shape is the CROSS-SYSTEM
bound whose system is fully DECLARED` tot en met
`` `backlog/new/20260910-kruis-stelsel-eenheid-divergentie.md`. `` door:

> **Closed (2026-09-19) by the EXECUTOR, not by the rule:** the CROSS-SYSTEM
> bound whose system is fully DECLARED — `Massa` declared beside `Gewicht`
> (`kg` factor 1, `t` factor 1000, both canonical `kg`), a `t` bound over a
> `kg` base in `Gewicht`: convertible by canonical name, so verifier and rule
> admit it, while on 2026-09-10 `ref` restated to 10000 and Drools answered
> 10000 on both branches and the ASP backend refused to translate, its
> per-system normalisation finding no identity ACROSS systems. The rule's own
> choice settled it — convertibility is a canonical-NAME law, so the backend
> was enforcing a fence the norm does not carry, and the backend conformed
> rather than the spec narrowing: it now gathers its conversion table by
> canonical name across declared systems and admits an exact-factor-1 pair at
> the identity fence, with the integer encoding untouched (a cross-system
> ratio that is not a positive integer into the finest unit stays the same
> loud refusal a same-system one gets). Re-measured 2026-09-19 on all four
> positions: the WF-099 bound, the WF-035 join and the WF-023 write move to
> the pinned values, and so does the factorless `kg`@Massa / `kg`@Gewicht
> pair; the WF-036 comparison moves only for the factorless pair, because the
> normalisation pass rewrites rule VALUES and not conditions — and the
> identical SAME-system document is refused by the same message before and
> after, so what remains is the pass's scope and not a system fence.
> Cost: zero elsewhere — the Drools sweep over both branches moves exactly the
> new golden's line (exec 153 → 154, DRL-text 126 → 127) and the corpus replay
> counts grow only by it. Golden `jutterswet-stelselweging` is the
> cross-system carrier: two declared systems sharing canonical `kg`, crossed
> at all four positions with and without a factor, `lastgewicht` crossing
> twice in one rule (a `Massa` bound over a `Gewicht` base written to a
> `Massa` target), hand-computed absolutes replayed by ref ≡ clingo ≡ Drools
> on both branches; gate 12's `EXPECTED_PLAYED` 153 → 154, gate 13's twist set
> 108 → 126 annotations over 22 → 23 goldens with the floor unchanged.
> `backlog/done/20260910-kruis-stelsel-eenheid-divergentie.md`.

**(3) `spec/CONSTRUCTS.md`, rij 1** (de WF-099-alinea van 2026-09-10).
Vervang de passage die begint met `open blijft de KRUIS-STELSEL-grens met een
volledig gedeclareerd zusterstelsel` tot en met
`waar ze gemeten op alle VIER de eenheidsposities identiek splitst.` door:

> **Gesloten (2026-09-19), door de UITVOERDER en niet door de regel:** de
> KRUIS-STELSEL-grens met een volledig gedeclareerd zusterstelsel (allebei
> canoniek `kg`), waar `ref` en Drools 10000 rekenden en de ASP-backend
> weigerde te vertalen. De regel had haar keuze al gemaakt — convertibiliteit
> op canonieke naam — dus was dit geen open normvraag maar een backend die een
> hek ophief dat de norm niet draagt; clingo conformeert. De vertaling
> verzamelt haar omrekentabel nu op canonieke naam over de gedeclareerde
> stelsels heen en laat een paar met exacte factor 1 door het identiteitshek;
> de integer-codering verandert niet, en een kruis-stelselverhouding die geen
> positief geheel getal naar de fijnste eenheid is blijft dezelfde luide
> weigering als binnen één stelsel. Hermeten op alle vier de posities: de
> WF-099-grens, de WF-035-join en de WF-023-toekenning bewegen naar de gepinde
> waarden, evenals de factorloze vorm; de WF-036-vergelijking beweegt alleen
> factorloos, omdat de normalisatiepas regelWAARDEN herschrijft en geen
> voorwaarden — en hetzelfde document binnen één stelsel weigert met dezelfde
> boodschap, vóór en ná, dus wat overblijft is het bereik van de pas en geen
> stelselhek. Golden `jutterswet-stelselweging` draagt de vier gekruiste
> posities in het volle vierkant (poorten 5, 6, 12 en 13);
> `EXPECTED_PLAYED` 153 → 154, gate 13 108 → 126 twists over 22 → 23 goldens.
> `backlog/done/20260910-kruis-stelsel-eenheid-divergentie.md`.

### Wat bewust NIET is gedaan

- **Geen verifier-regel, geen `corpus/negatief/`-getuige.** Uitweg 1 is
  afgewezen; er is geen nieuwe verboden vorm om rood van te maken.
- **De WF-036-vergelijking met een factor blijft weigeren** — en dat is het
  bereik van de normalisatiepas (regelwaarden, geen voorwaarden), niet een
  stelselgrens. P6 bewijst dat binnen één stelsel identiek. Dat verruimen is
  een eigen kaart: de pas moet dan ook `voorwaarde`-bomen herschrijven, wat
  raakt aan de vraag welke eenheid een voorwaarde "in" staat (er is geen
  schrijfdoel om naartoe te normaliseren).
- **`spec/ASP-TRANSLATION.md` S6** beschrijft de normalisatiepas; de
  stelselverruiming hoort daar op termijn ook in. Niet aangeraakt — beschermd
  noch gemandateerd door deze kaart; genoemd zodat het niet wegvalt.

## Lensronde (2026-09-19, naschrift hoofdsessie)

Alle vijftien lenspunten CONFIRMED, advies MERGE; gemerged en de drie
registervoorstellen geland op `ead29076` — de registerrij kreeg daarbij
een tijdslaag-aanhef op de oude kostenclausule van 2026-09-10, zodat de
twee Cost-zinnen elk hun eigen landing dragen. De lens voegde eigen
bewijs toe: alle 526 bestaande ASP-programma's zijn vóór/ná
byte-identiek, dus de tabelverruiming is aantoonbaar de identiteit over
het bestaande corpus. Drie staande aantekeningen, niet blokkerend:

- de opmaakstring van de omrekentabel-fout (enkelvoud/meervoud "unit
  system(s)" plus de stelsellijst) wordt nergens rechtstreeks
  geasserteerd; formaatdrift zou onopgemerkt blijven;
- `restate_lit` berekent `to_finest` voor een literaleenheid die niet
  in de tabel hoeft te zitten, dus de positief-geheel-verplichting dekt
  een ongedeclareerde literaleenheid niet — pre-existent en door deze
  kaart VERKLEIND (`unit_in_stelsel` eiste alleen stelselgelijkheid,
  `restates_into` eist óók canonieke-naamconvertibiliteit);
- het Result citeert bij mutatieproef 2 twee van de twaalf
  MISMATCH-regels (selectie, geen onjuistheid).
