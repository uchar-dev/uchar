# Verklaart de genoemde eigenaar het kenmerk ook?

Goal: beslissen — met een machinaal toetsbare uitkomst — of een
`Kenmerk`-node met een expliciete `eigenaar` óók moet worden gekruist op de
vraag of dát type het genoemde kenmerk **verklaart**, en, onlosmakelijk
daarmee, **welke eigenaar gezaghebbend is: het veld of het onderwerp**. WF-034
sluit vandaag alleen de eerste helft (de naam moet een verklaard objecttype
zijn); de tweede helft staat als een uitdrukkelijke stilte in de verifier
opgeschreven en is nooit beslist.

Deze kaart is afgesplitst van `20260906-gedurende-kwalificatie-stiltes` (geland
2026-09-10). Die kaart sloot de eigenaar op één soort positie — het lid van een
`gedurende`-clip, waar de lezer helemaal geen eigenaar raadpleegt (WF-097, dat
in de fixronde van 2026-09-10 alle drie de clips ging dekken) — en liet deze
helft uitdrukkelijk liggen, omdat hij een *semantische* vraag is en geen
resolutievraag.

## Wat er vandaag staat

`spec/verifier/lib/checks.ml`, de `Kenmerk_check`-wandeling, noteert de stilte
zelf. De eigenaar-arm kruist of de naam een verklaard objecttype is; de
declaratie-kruising eronder is gekeyd op **`subject`**, niet op `eigenaar`. Het
commentaar zegt met zoveel woorden wat dat niet sluit: of het genoemde type het
kenmerk verklaart — en dat het beantwoorden ervan meteen beslist welke eigenaar
gezaghebbend is.

De asymmetrie is echt: bij een expliciete `eigenaar` die het kenmerk *niet*
verklaart, kruist de verifier vandaag het kenmerk tegen het **onderwerp** en zegt
niets over het veld. Een bean-model-uitvoerder die de eigenaar wél als
aanhechtingspunt leest, leest daar dus een ander document dan de verifier.

## De meting (2026-09-10, op de eindkop van de moederkaart)

Over de drie zones (`corpus/golden`, `corpus/rood`, `corpus/negatief`):

    Kenmerk-nodes totaal                       213
    daarvan met een expliciete eigenaar          6   (alle zes in corpus/negatief)

De zes, met per stuk de stand van de open vraag:

| document | positie | eigenaar | kenmerk | type verklaard | verklaart het kenmerk |
| --- | --- | --- | --- | --- | --- |
| `wf031-dagsoort-leest-kenmerk` | `/resolvedDagsoorten/0/voorwaarde` | `Jutter` | `penninggerechtigd` | ja | **ja** |
| `wf034-kenmerkcheck-onbekende-eigenaar` | `/resolvedRegels/0/voorwaarde` | `Bestaatniet` | `gemeld` | nee | n.v.t. (WF-034 vangt hem) |
| `wf046-negated-read-in-recursive-cycle` | `/objectCreaties/0/voorwaarde` | `Persoon` | `volwassen` | ja | **ja** |
| `wf097-clip-kenmerk-eigenaar` | `/resolvedRegels/0/waarde/filter/onderdelen/0` | `Jutter` | `borgplichtig` | ja | **ja** (positie nu WF-097-verboden) |
| `wf097-prorata-kenmerk-eigenaar` | `/resolvedRegels/0/waarde/filter` | `Jutter` | `borgplichtig` | ja | **ja** (positie nu WF-097-verboden) |
| `wf097-dagentelling-kenmerk-eigenaar` | `/resolvedRegels/0/waarde/filter` | `Jutter` | `borgplichtig` | ja | **ja** (positie nu WF-097-verboden) |

De onderste drie zijn getuigen van de moederkaart zelf en staan op posities die
WF-097 inmiddels verbiedt; voor DEZE kaart tellen ze niet mee als vindplaats,
alleen als verklaring waarom het getal van 4 naar 6 liep. De scalaire posities
die er werkelijk toe doen zijn de bovenste drie, ongewijzigd sinds 2026-09-09.

**Er is geen enkele tegenvoorbeeld-getuige in het corpus** — geen document
waarin de genoemde eigenaar het kenmerk niet verklaart. Wie deze kaart doet,
maakt zijn eerste getuige zelf, en de kaart is daarmee expliciet een
*ontwerp*beslissing op een lege verzameling, niet het opruimen van gemeten
schade. Vaststellen dat de verzameling nog steeds leeg is, is het eerste werk.

De `wf031`-positie is de scherpste van de drie: in een dagsoort-voorwaarde kent
de wandeling **geen onderwerp**, dus daar is het veld de enige eigenaar die er
is. Elke regel die "het onderwerp is gezaghebbend" zou zeggen, moet die positie
apart beantwoorden.

## De vraag die beslist moet worden

Eén genummerde vraag, met drie mogelijke antwoorden, elk met een prijs:

1. **Het veld is gezaghebbend.** De declaratie-kruising verhuist van `subject`
   naar `eigenaar` waar die aanwezig is, en een eigenaar die het kenmerk niet
   verklaart is een fout. Prijs: dit verandert de *lezing* van bestaande
   documenten (het `wf031`-geval leest dan tegen `Jutter` in plaats van tegen
   niets), en de uitvoerders moeten mee — anders is het een verifier-only regel
   met een uitvoerder die iets anders doet.
2. **Het onderwerp is gezaghebbend, het veld is presentatie.** Dan wordt de
   eigenaar-arm van WF-034 een pure resolutiecontrole en moet ergens staan dat
   het veld verder *niets* betekent. Prijs: de `wf031`-positie heeft geen
   onderwerp, dus die lezing moet daar iets anders zeggen.
3. **Beide moeten het verklaren.** De strengste, en de enige die geen keuze
   maakt tussen twee autoriteiten. Prijs: over-afwijzing als er ooit een
   legitiem document komt waarin ze verschillen.

## Kernwaarschuwingen

1. **Dit is geen opruimklus.** Er is nul gemeten schade; er is een gemeten
   *stilte*. De beslissing moet op de semantiek rusten, niet op een
   corpustelling.
2. **Beweeg de uitvoerders mee of beweeg niets.** `ref/lib/eval.ml` en
   `backends/clingo/lib/translate.ml` matchen `eigenaar` vandaag weg met `_` op
   de scalaire kenmerkcheck (`Kenmerk_check { … eigenaar = _; … }`); de
   Drools-transpiler leest hem wél. Een verifier-regel zonder de uitvoerders
   erbij maakt precies de stilte die de moederkaart sloot.
3. **Het clip-lid is klaar en valt hierbuiten.** WF-097 weigert de eigenaar daar
   al; deze kaart gaat over de scalaire posities ernaast.

## Acceptance

- De genummerde vraag beantwoord, met de reden, en de prijs van het gekozen
  antwoord uitgeschreven.
- De meting hierboven **opnieuw geteld** (niet overgeschreven) en genoteerd.
- Per beslissing een artefact dat hem toetst: bij weigeren een
  `corpus/negatief/wf###-<slug>/`; bij een gewijzigde lezing een golden met
  absolute verwachtingen die **beide** uitvoerders halen.
- Rood eerst: de getuige faalt aantoonbaar vóór de wijziging.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (**voorstel** — beschermd pad), `spec/IR-SPEC.md`
(**voorstel**), `ref/**`, `backends/clingo/**`, `backends/drools/**`,
`corpus/negatief/**`, `corpus/golden/**`, `backlog/`.

## Out of scope

- Het clip-lid: WF-097 (geland 2026-09-09 op de totaal-clip, 2026-09-10
  verbreed tot alle drie de clips) weigert daar zowel de eigenaar als de
  periode.
- De `periode` op de scalaire kenmerkcheck. Dat is de §8.2-familie, een eigen
  vraag met een eigen prijs (de RegelSpraak-frontend produceert die vorm uit de
  bron), uitdrukkelijk buiten de snit van de moederkaart gehouden.
- Het `eigenaar`-veld schrappen. Dat is de v1-delta die `CONSTRUCTS.md` ch. 2.2
  al draagt; deze kaart gaat over wat hij betekent zolang hij op de wire staat.

---

## Result (2026-09-10)

**Het besluit, met zijn prijs uitgeschreven.** Kaartoptie **1 — het veld is
gezaghebbend** (maintainerbesluit 2026-09-10). De declaratie-kruising verhuist
van `subject` naar `eigenaar` waar het veld aanwezig is; een eigenaar die het
genoemde kenmerk niet verklaart is **WF-100**. De prijs, in drie delen en geen
ervan verzwegen:

1. *De lezing van bestaande documenten verandert.* De onderwerploze
   dagsoortpositie kruiste vóór dit besluit tegen niets — daar kent de
   wandeling geen onderwerp, dus liep de kruising er eenvoudig niet — en kruist
   nu tegen het genoemde type. Dat vergt geen aparte uitspraak: onder de
   eigenaar-sleutel is het veld daar de enige sleutel die er is, en de positie
   wordt gewoon in plaats van uitzonderlijk. WF-031 weigert dat document
   sowieso omdat een dagsoortvoorwaarde geen objecttoestand mag lezen, dus geen
   uitvoerder bereikt hem.
2. *De uitvoerders moesten mee, en het antwoord dat zij konden geven is een
   WEIGERING, geen nieuwe waarde.* `ref` en de ASP-backend lezen precies ÉÉN
   object per conditie: het onderwerp van de omvattende vorm. Waar de eigenaar
   een ander type noemt is er hier geen object waar de check over kan gaan, en
   tóch het onderwerp lezen is precies de lezing die het besluit afwijst — dan
   was het onderwerp alsnog gezaghebbend. Ze weigeren nu
   (`RUNTIME-SEMANTICS.md` ch. 5.8b). Een weigering is een kleiner antwoord dan
   een implementatie, en dat is als restant genoteerd en gekaart, niet
   weggeschreven.
3. *Er is geen tegenvoorbeeld in de corpus.* Dit is een ONTWERPbesluit op een
   lege CORPUS-verzameling; de eerste getuige is voor de regel geschreven. De
   kaart waarschuwde daarvoor en de meting bevestigt het — maar "geen gemeten
   schade" geldt alleen binnen de corpus. Buiten de corpus wees de verifier de
   enige emissievorm van de frontend af; zie de reparatiemeting verderop.

**Hertelling op deze HEAD (opnieuw geteld, niet overgeschreven).** Over
`corpus/golden` (166), `corpus/rood` (26) en `corpus/negatief` (359):

    Kenmerk-nodes totaal                       216   (kaart 2026-09-10: 213)
    daarvan met een expliciete eigenaar           6   (alle zes in corpus/negatief)

De zes zijn dezelfde zes die de kaart-tabel noemt, en alle zes nog steeds
consistent: `wf031-dagsoort-leest-kenmerk` (Jutter/penninggerechtigd, verklaart),
`wf046-negated-read-in-recursive-cycle` (Persoon/volwassen, verklaart),
`wf097-clip-kenmerk-eigenaar`, `wf097-prorata-kenmerk-eigenaar`,
`wf097-dagentelling-kenmerk-eigenaar` (Jutter/borgplichtig, verklaart) en
`wf034-kenmerkcheck-onbekende-eigenaar` (Bestaatniet — geen gedeclareerd type,
WF-034 vangt hem). **De tegenvoorbeeld-verzameling is nog steeds leeg.** Het
totaal liep van 213 naar 216 door corpuswerk sindsdien, niet door nieuwe
eigenaar-dragers. De pariteitszone erbij gemeten: 30 documenten, 9
kenmerkchecks, **0** met een eigenaar — ook de toolchain-gepinde ©-casus draagt
de vorm niet.

**De pre-divergentiemeting (vóór elke wijziging, op deze HEAD).** Handgebouwd
document: onderwerp `Vondst` (verklaart `gekeurd`), eigenaar `Jutter` (verklaart
`gekeurd` óók), case V1 = gekeurd, J1 = niet gekeurd, zodat de twee lezingen
verschillende waarden geven.

| lezer | uitkomst |
| --- | --- |
| verifier | **PASS** |
| `ref` | `V1.afslag = "veiling \"Zee\""` — het ONDERWERP gelezen |
| ASP-backend | idem, dezelfde waarde |
| Drools-backend | `EXCLUDE transpiler SKIP: regel (voorwaarde buiten Phase-B scope)` — geen regel gedragen |

Eén verifier-schoon document, drie uitvoerders, twee lezingen en een stille.
Met `eigenaar: null` of `eigenaar: "Vondst"` speelt Drools hetzelfde document
wél en geeft dezelfde waarde als `ref` en de ASP-backend — het veld is dus geen
decoratie voor die uitvoerder, het is een aanhechtingspunt.

**De operationele semantiek die gekozen is, en waarom.** De producent zegt zelf
wat het veld betekent: de RegelSpraak-frontend zet hem op precies één vorm — de
voorwaarde van een §13.4.7 feitcreatie die een kenmerk van het DOEL noemt — en
tagt hem met `eigenaar = doel` "zodat de transpiler hem op het `$doel`-patroon
plaatst (en nooit op de anker, ook niet onder een gelijknamig anker-kenmerk)".
Beide Drools-takken lezen hem zo: `eigenaar != null && eigenaar != subject`
levert een marker (DRL-tak) of een skip (exec-model-tak), en in de
feitcreatie-route valt de vertaler terug op het doel, waar de constraint dan op
een ANDER object landt. Daaruit volgt de tweeledige wet die nu in de spec staat:

- **de eigenaar is de declaratiesleutel.** Élke declaratie die een lezer voor
  het kenmerk raadpleegt is die van het GENOEMDE type: verklaart het type het
  kenmerk (WF-100), is het tijdsafhankelijk (`kenmerkGranulariteit` — dat
  beslist tussen de scalaire lezing en de tijdlijnlezing van ch. 3.3), en de
  knoopsleutel `char:<eigenaar>:<naam>` van de afhankelijkheidsrand. Die derde
  was al zo gesleuteld in alle vier de lezers vóór de regel; wat de regel
  toevoegt is dat de twee kruisingen daar nu mee overeenstemmen. Eén sleutel,
  één keer berekend, zodat de bestaanskruising en WF-043's
  tijdsafhankelijkheidskruising niet uit elkaar kunnen lopen.
- **de eigenaar is óók een gastheer-uitspraak, en een lezer die die niet kan
  honoreren weigert.** Het OBJECT blijft in `ref` en de ASP-backend het
  onderwerp; is de eigenaar niet het type van dat object, dan is er hier geen
  object en volgt een fout. `ref` lui (bij evaluatie), de ASP-backend gretig
  (bij vertaling) — dezelfde splitsing die WF-055 al heeft, geërfd en niet
  nieuw. Beide zeggen dezelfde zin.

**Rood eerst, en arm-exact.** Getuige
`corpus/negatief/wf100-eigenaar-verklaart-kenmerk-niet/` (uit de golden gebroken
door één veld: `eigenaar` van `Vondst` naar `Jutter`, dat `penninggerechtigd`
verklaart en geen `gekeurd`):

    vóór de wijziging:  verifier: PASS
    ná  de wijziging:  WF-100 FAIL /resolvedRegels/0/voorwaarde/eigenaar:
                       'gekeurd' is not a declared characteristic of 'Jutter'

Eén regel, geen bijvangst. Volle sweep over de verifier-gegatete zones (golden
167, negatief 360, rood 26 — 553 documenten), verifier-rapport voor en na
letterlijk gediff'd: **precies één verdict beweegt** — dat van de nieuwe getuige
— en geen enkele andere rapportregel. Nul over-afwijzing dus, en de 193
golden+rood-documenten blijven schoon. Falsifieerbaar in twee richtingen, beide
gemeten: geef `Jutter` een eigen `gekeurd` en de case wordt schoon (en wordt dan
door de UITVOERDERS geweigerd — andere regel, ander rapport); zet `Vondst` terug
en het is de golden.

**De golden, met de handberekening.** `corpus/golden/jutterswet-keurhouder/` is
de EERSTE golden van de corpus die überhaupt een `eigenaar` draagt; vóór hem
stond het veld alleen in de negatief-zone, dus pinde geen enkele gegatete,
herspeelde case wat een lezer ermee doet. Twee `Vondst`-regels lezen hetzelfde
kenmerk `gekeurd` in beide polariteiten, elk met `Vondst` — het eigen
onderwerptype — als eigenaar; ernaast staat een tweede gedeclareerd type
`Jutter` met een eigen kenmerk, dat de eigenaar tot een KEUZE maakt in plaats
van de enige beschikbare naam.

| instantie | type | kenmerk uit de case | afslag | keurstatus |
| --- | --- | --- | --- | --- |
| V1 | Vondst | `gekeurd` | `zeeveiling` | leeg |
| V2 | Vondst | (geen) | leeg | `onbekeurd` |
| J1 | Jutter | `penninggerechtigd` | n.v.t. | n.v.t. |

`afslag` wordt precies daar geschreven waar `gekeurd` geldt, `keurstatus`
precies daar waar het niet geldt, dus de twee regels delen de twee Vondsten en
geen van beide krijgt allebei. J1 wordt door geen enkele regel geraakt en komt
ongewijzigd terug. Zes absolute verwachtingen; `ref` PASS (6), clingo PASS (6),
Drools PLAYED — de vierkantszijde sluit (ref ≡ clingo ≡ Drools). NOOP-weerleggend:
V1's `afslag` en V2's `keurstatus` staan niet in de ruwe invoer.

**De gewijzigde lezing, en waarom die geen golden KAN zijn.** Het document dat
de weigering toont — eigenaar is een gedeclareerd type dat het kenmerk verklaart
maar niet het onderwerptype is — past in geen enkele corpuszone: een golden moet
herspelen, een rood-case moet in ELKE uitvoerder die de vorm draagt observeerbaar
falen, en de Drools-backend laat zo'n regel vallen als transpiler-skip in plaats
van te falen (dat zou een reden-dragende regel in het beschermde
`scripts/check-rood.sh` vergen, dat niet in de may-touch-set staat). De getuigen
staan daarom in de eigen, gegatete suites van de twee uitvoerders.
`ref/test/test_ref.ml` krijgt ch. 5.8b met zeven voorbeelden — eigenaar
afwezig, eigenaar = onderwerptype, eigenaar in andere kapitalisatie, het
onwaar-geval, twee weigeringen in beide richtingen en de GENAVIGEERDE weigering
— gate 5, suite **489 -> 496**.
`backends/clingo/test/test_clingo.ml` krijgt zes checks — twee keer ref ≡
clingo op de dragende vorm en twee weigeringen met gepinde zin — gate 6, suite
**86 -> 92**. Let op het verschil tussen gemeten stand en vloer: 489 is bij
`ref` toevallig allebei, bij clingo is 86 de gemeten stand vóór de kaart en 83
de vloer, en die vloer beweegt niet. De 86 is gemeten door de suite van vóór de
kaart terug te halen en te draaien, niet uit het gate-script afgelezen. Beide
vloeren zijn ondergrenzen, dus groei vergt geen scriptwijziging.

**Het artefactgat op de GENAVIGEERDE vorm, gedicht.** WF-100's kruising raakt
de genavigeerde check bewust niet — daar leeft het kenmerk op het BEREIKTE type,
dat WF-053 al kruist — maar de HOSTING-weigering van de uitvoerders loopt er
wél door: de eigenaar noemt het patroon waar de constraint aan hangt, en dat is
het onderwerp, of de check daarna hopt of niet. De consequentie is scherp en is
op de postwacht-vorm gemeten (`risicopost` via `post`, eigenaar `Post`,
onderwerp `Wachter`):

| lezer | vóór | ná |
| --- | --- | --- |
| verifier | PASS | **PASS** (de kruising is terecht buiten bereik) |
| `ref` | `PASS (10)` | **fault** |
| ASP-backend | `PASS (10)` | **fault** |
| Drools exec-model | `EXCLUDE transpiler SKIP … voorwaarde buiten Phase-B scope` | ongewijzigd |
| Drools DRL-tekst | `EXCLUDE drl-transpiler MARKER: TODO kenmerk 'risicopost' hoort bij Post` | ongewijzigd |

Ná deze kaart heeft die vorm dus **nul antwoordende uitvoerders**. Dat is
bedoeld, maar het stond alleen in proza. Nu draagt elke uitvoerder er een
machinaal gecontroleerde getuige voor, en beide asserteren de ZIN en niet enkel
de fout — de weigering gaat aan de hop vooraf, dus een kale fout-check zou ook
slagen op een onoplosbare rol. De bijzin staat nu ook in (q1)'s Scope-alinea en
in de WF-100-registerrij, zodat het niet alleen in ch. 5.8b te vinden is.

**Wat de golden wel en niet pint (eerlijk afgebakend).** De acceptance vraagt
"bij een gewijzigde lezing een golden met absolute verwachtingen die beide
uitvoerders halen". `jutterswet-keurhouder` levert dat, maar het pint het
BESLUIT niet: op de PRE-binaries geeft hij identiek `PASS (6)`, omdat de
declaratiesleutel daar samenvalt — de eigenaar IS het onderwerptype. Wat de
golden pint is dat de vorm laadt, lowert en in alle drie de uitvoerders
identiek herspeelt, en dat het vierkant erop sluit. Het besluit zelf wordt
gepind door de negatief-getuige (de verifier-helft) en door de twee
uitvoerderssuites (de uitvoerdershelft) — precies zoals de NOTE bij de golden
het zegt.

**De gemeten reparatie buiten de corpus, en haar keerzijde.** De zin "geen
gemeten schade" geldt voor de corpuszones en NIET voor de producent. De
frontend emitteert dit veld op precies één vorm — de §13.4.7 doel-guard met
`eigenaar = doel` en doel ≠ anker — en juist die vorm wees de verifier vóór
deze kaart af, omdat de kruising op het ONDERWERP (de anker) gesleuteld was.
Gemeten 2026-09-10 op de feitcreatie van `jutterswet-postwacht` (anker
`Kustvak`, doel `Sein`, kenmerk `gehesen` op het doel, guard
`eigenaar: "Sein"`, case S1 gehesen / S2 niet):

| lezer | vóór | ná |
| --- | --- | --- |
| verifier | `WF-034 FAIL /feitCreaties/0/voorwaarde: 'gehesen' is not a declared characteristic of 'Kustvak'` | **PASS** |
| `ref` | `vakseinaantal = 0` (leest de anker, die het kenmerk nooit draagt) | **fault** |
| ASP-backend | idem, `0` | **fault** (bij vertaling) |
| Drools exec-model | speelt, `vakseinaantal = 1` | ongewijzigd |
| Drools DRL-tekst | speelt, `vakseinaantal = 1` | ongewijzigd |

Er wás dus schade, buiten de corpus, en die is weg: de enige emissievorm van de
producent is nu conform. De keerzijde staat er in dezelfde meting: die vorm is
nu conform **én** onspeelbaar in `ref` en de ASP-backend, terwijl beide
Drools-takken hem spelen en de uitkomst van de DOEL-instanties laten afhangen
(1 van de 2 Seinen). Dat is exact optie 3 van het restkaartje, ongekozen; de
meting staat nu ook ín dat kaartje als uitgangspositie. En het corrigeert een
aanname: de Drools-backend leest de eigenaar-instanties werkelijk, dus de
weigering in `ref`/ASP is een capaciteitsgrens van die twee lezers (één object
per conditie) en geen verkapte "de eigenaar moet het onderwerptype zijn"-regel
— de verifier zegt daar niets over en laat zo'n document toe.

**Restant dat hierbij hoort.** De enige eigenaar-vorm die overal herspeelt
(eigenaar = onderwerptype) schrijft de v0-producent nooit — `RuleChecker` zet
het veld alleen waar doel ≠ anker — en de vorm die de producent wél schrijft
kan onder dit besluit geen golden worden, want twee van de drie uitvoerders
faulten erop. Herspeelbare eigenaar-vorm en geproduceerde eigenaar-vorm zijn
voorlopig disjunct. Genoteerd in de golden-NOTE, in IR-SPEC (q1) en in het
restkaartje.

**Gate-uitkomsten.** Alle dertien groen, elk met zijn eigen exitcode gemeten
(niet die van een pipe). Gemeten getallen die bewogen: gate 2 192 -> 193
bestanden, gate 3 golden 192 -> 193 en negatief 359 -> 360, gate 4 453 -> 454
casebestanden, gate 5 427 -> 428 cases en suite 489 -> 496, gate 6 427 -> 428
cases en suite **86 -> 92** (vloer 83, ongewijzigd), gate 7 391 -> 392
weerleggende cases, gate 12 played
119 -> 120 en JUnit 244 (vloer 239). Gates 1, 8, 9, 10, 11 en 13 bewegen geen
enkel getal: gate 8 blijft 26 rode cases (geen rood document draagt een
eigenaar), gate 13 blijft 74 twists over 15 goldens met 0 annotaties buiten de
twist-tabel — de nieuwe golden draagt bewust geen enkele eenheid, dus levert hij
er geen.

**Scriptwijziging als VOORSTEL (beschermd pad).** Eén, en alleen als
kanaal-voorstel: `scripts/check-drools.sh`, `EXPECTED_PLAYED` 119 -> 120 met de
reden erbij. `jutterswet-keurhouder` arriveert SPELEND: beide checks noemen het
eigen onderwerptype als eigenaar, en op die vorm vuurt de eigenaar-test van de
transpiler (`eigenaar != null && eigenaar != subject`) niet, dus de twee regels
lowreren en vuren precies zoals een eigenaar-loos paar. Gemeten: `PLAYED 1`.
Geen enkele andere vloer beweegt — de andere gates dragen ondergrenzen en groei
vergt daar niets.

**Voorgestelde CONSTRUCTS-zin** (rij `Kenmerk`, ch. 2.2 — de rij die vandaag
"Drop the `eigenaar` pattern-steering field (Drools leak)" draagt):

> **Landed (2026-09-10):** zolang `eigenaar` op de v0-wire staat is hij
> GEZAGHEBBEND — WF-100 (`IR-SPEC.md` ch. 4.6 (q1)) kruist het genoemde type
> tegen het kenmerk dat het beweert te bezitten, en diezelfde sleutel draagt
> WF-034's bestaanskruising en WF-043's tijdsafhankelijkheidskruising: waar het
> veld er staat is het de sleutel, waar het ontbreekt is het onderwerp het.
> Daarmee is de tweede helft van WF-034 (q) gesloten, de helft die die regel
> uitdrukkelijk open liet, en het is een SEMANTISCH besluit — welke eigenaar
> gezaghebbend is — en geen resolutiebesluit. De uitvoerders bewegen mee, maar
> met een WEIGERING en niet met een nieuwe waarde: `ref` en de ASP-backend lezen
> één object per conditie, dus waar de eigenaar niet het type van dat object is
> falen ze (`RUNTIME-SEMANTICS.md` ch. 5.8b, `ref` lui bij evaluatie, de
> ASP-backend gretig bij vertaling) in plaats van het onderwerp te lezen — de
> lezing die vóór 2026-09-10 stilletjes gold, gemeten op een verifier-schoon
> document waar beide OCaml-uitvoerders de waarde van het onderwerp teruggaven
> onder de naam van een ander type en de Drools-backend de regel helemaal niet
> droeg. Een afwezige eigenaar en een eigenaar die het eigen onderwerptype
> noemt zijn dezelfde lezing en waren dat altijd, en daarom beweegt geen enkel
> document dat aan de regel voorafgaat. De schrap-delta van deze rij staat
> daarmee niet stil maar wordt scherper: wat er in v1 verdwijnt is nu een veld
> met een uitgeschreven betekenis, geen leak met een onbekende. De OBJECT-helft
> — dat een gastheer-voorwaarde over de INSTANTIE van het genoemde type gaat,
> wat het bonenmodel wél doet — blijft open en is gekaart als
> `backlog/new/20260910-eigenaar-gastheer-object.md`.

**De WF-101-schuif.** WF-100 was door twee wachtende kaarten als kandidaat
genoemd; deze landde eerder, dus beide zijn bijgewerkt naar WF-101:
`backlog/new/20260910-geneste-aggregatie-rolketen.md` (de padregel van een
meerstaps aggregatiebereik) en `backlog/new/20260910-reverse-rol-via-naam.md`
(de eenzijdige-leveringsregel). De dubbeling tussen die twee blijft staan: wie
het eerst landt gebruikt WF-101, de ander schuift opnieuw.

**Wat bewust NIET bewoog.**

- *De verifier zegt niets over "de eigenaar moet het onderwerptype zijn".* Dat
  zou strenger zijn dan optie 1 en dicht bij optie 3, en het is niet beslist.
  Het blijft een uitvoerderswet (ch. 5.8b), zichtbaar in hun suites.
- *De GENAVIGEERDE check.* WF-100 is expliciet tot de niet-genavigeerde check
  beperkt: daar leeft het kenmerk op het BEREIKTE type, dat WF-053's arm al
  kruist, dus een eigenaar is dat type niet en kruisen zou een vorm afwijzen
  waarover niets beslist is. Geen document in enige zone draagt eigenaar +
  navigatie. De uitvoerders passen de gastheer-weigering daar wél toe, met
  dezelfde helper en dezelfde reden als op de scalaire arm — dat is wat de twee
  Drools-takken ook doen, en het voorkomt dat er een nieuwe stilte ontstaat.
- *Het clip-lid.* WF-097 weigert de eigenaar daar al; niets aangeraakt.
- *De `periode` op de scalaire kenmerkcheck.* De §8.2-familie, eigen kaart.
- *Het veld schrappen.* De v1-delta van `CONSTRUCTS.md` rij `Kenmerk`; deze
  kaart gaat over wat hij betekent zolang hij op de wire staat.
- *`backends/drools/**`.* Niet aangeraakt — de transpiler leest het veld al zo,
  en de enige beweging daar is de `EXPECTED_PLAYED`-pin in het gate-script.
- *`spec/CONSTRUCTS.md` en `spec/PLAN.md`.* Niet aangeraakt; de rij staat
  hierboven als voorstel.

**Verbreding van de may-touch-set, geregistreerd.** De kaart noemde
`spec/RUNTIME-SEMANTICS.md` niet. De semantiekwijziging vergt hem: het besluit
verandert wat een lezer met het veld DOET, en dat hoort in het normatieve
runtime-document met een gewerkt voorbeeld dat verbatim de ref-suite in gaat
(`CLAUDE.md`, Spec discipline). Toegevoegd als **voorstel**: ch. 5.8b, drie
regels en een gewerkt voorbeeld. Regel 3 is uitdrukkelijk beperkt tot de twee
lezers die dat hoofdstuk normeert — `ref` en de ASP-backend lezen de
eigenaar-instanties nooit — en zegt met zoveel woorden dat een bonenmodel dat
op de producentvorm wél doet, met verwijzing naar de open objecthelft. Een
algemene "geen enkele lezer"-zin zou gemeten onwaar zijn en zou bovendien de
vraag dichttimmeren die IR-SPEC (q1) en het restkaartje juist open verklaren. Idem `scripts/check-drools.sh`, uitsluitend als
kanaal-voorstel.

**Beschermde paden — dit blijft een VOORSTEL.** `spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md`, `spec/verifier/**` en `scripts/check-drools.sh`
zijn beschermd; de tak blijft onverkort als voorstel staan tot de maintainer
hem samenvoegt.
