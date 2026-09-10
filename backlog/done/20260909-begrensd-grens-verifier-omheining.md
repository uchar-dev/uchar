# Krijgt de Begrensd-grens een WF-omheining, zoals WF-035/036/023 de andere posities omheinen?

Goal: beslissen — en de beslissing als spec-delta of als expliciet
niet-doen vastleggen — of een `Begrensd`-grens (de `minimum`/`maximum` van een
begrenzing) een WF-regel krijgt die eist dat haar eenheid met die van de basis
verenigbaar is. Vandaag is dat de enige eenheidspositie in de gemeten set
waar de verifier een document doorlaat dat ref én clingo weigeren.

## De meting die de vraag stelt (2026-09-09, lens 1 op de kaart
`20260906-drools-eenheid-uit-declaratie`)

Mutant **N**: een `Begrensd` over twee `EUR`-attributen, met een `Literal`
ondergrens `10` geannoteerd in `kg` — en een bovengrens `100`, ook in `kg`.
Alleen de grens-annotaties dragen die vreemde eenheid; de basis en het doel zijn
`EUR`.

| consument | uitkomst |
|---|---|
| **verifier** | **PASS** — geen enkele WF-regel omheint deze positie |
| ref | `EVALUATION FAULT: incompatible units: EUR vs kg` |
| clingo | `TRANSLATION FAULT: non-identical units under a Begrensd bound are not translated` |
| Drools (vóór de eenheidsgrens) | **PLAYED** — stil, met de basiswaarde ongewijzigd |
| Drools (ná de eenheidsgrens) | `ERROR Eenheidsconflict: … 'kg' … herschaalt niet naar 'EUR' …` |

De andere drie posities zijn wél omheind, en de mutanten laten dat zien:
`WF-035` op de additieve join, `WF-036` op de vergelijkingsoperanden, `WF-023`
op de toekenning. De grens van een begrenzing valt tussen wal en schip.

Merk op waarom WF-091 hier niet redt: de grens is een `Literal`, en een
`Literal` is juist één van de drie ch. 2.10-posities waar de annotatie de
**bron** is. Er valt dus niets te kruisen tegen een declaratie — de vraag is
niet of de annotatie een declaratie correct herhaalt, maar of twee eenheden op
één plek bij elkaar passen. Dat is precies wat WF-035/036/023 elders doen.

## Wat deze kaart moet opleveren

Eén van twee, expliciet:

- **een nieuwe WF-regel** (voorstel: de begrenzing-grens moet een eenheid
  dragen die verenigbaar is met die van haar basis), met de gebruikelijke
  volledige uitrusting: ID, verifier-check, een geslaagde golden en een
  `corpus/negatief/<wf###-slug>/` die om de juiste reden faalt; of
- **een vastgelegd niet-doen**, met de reden waarom deze positie anders is dan
  de drie die wél omheind zijn, opgeschreven in `spec/IR-SPEC.md` naast de
  bestaande regels zodat de asymmetrie een uitspraak is en geen gat.

Neem in beide gevallen de N-meting hierboven op als getuige.

## Waarom dit een eigen kaart is

De Drools-backend heeft de vorm sinds 2026-09-09 dicht (de eenheidsgrens van
`Eenheidsconflict` faalt op deze positie, in beide takken), dus het
differentiële vierkant is hier al sluitend: alle drie de consumenten weigeren.
Wat open blijft is een **spec**-vraag — hoort de verifier dit document te
weigeren? — en die raakt `spec/IR-SPEC.md` en `spec/verifier/`, allebei
beschermde paden waar alleen de beheerder merget. Ze hoort dus niet in een
backend-kaart thuis.

May touch: `spec/IR-SPEC.md`, `spec/verifier/`, `corpus/golden/`,
`corpus/negatief/` — alle als voorstel, nooit als merge.

## Out of scope

- De Drools-backend: die weigert al, met reden en in beide takken.
- Ref en clingo: die weigeren al.
- De andere drie posities: die zijn omheind en blijven zoals ze zijn.

---

## Result (2026-09-10) — een nieuwe WF-regel: **WF-099**

De kaart bood twee uitwegen; het is de eerste geworden. Grond: drie
uitvoerders weigeren een vorm die de verifier doorlaat, en dat is precies de
klasse waarvoor de drie zusterposities hun omheining kregen. Een inhoudelijke
reden waarom juist deze positie anders zou zijn, is er niet — de reden dat ze
open stond is dat geen van de drie zusterregels er hoort te komen (zie
"waarom de zusters het niet halen" hieronder).

### 1. Wat "verenigbaar" bij de zusters betekent — uitgemeten, niet aangenomen

Alle drie de zusters roepen **hetzelfde predicaat** aan,
`units_compatible_adopting` in `spec/verifier/lib/checks.ml`:

- `units_convertible` — gelijke canonieke naam (of, bij ontbreken, gelijke
  afkorting) en gelijke vorm (plat vs tarief, recursief over de noemer).
  Onbekende structuur telt als verenigbaar, dus het predicaat onder-rapporteert
  liever dan dat het misvuurt.
- `units_compatible` — allebei eenheidloos = verenigbaar; allebei aanwezig =
  convertibel; **eenzijdig = onverenigbaar**.
- de adoptieclausule — een **eenheidloze numerieke `Literal`** neemt eerst de
  eenheid van haar buurman over.

Dus: **convertibiliteit, niet identiteit.** Dat staat ook al zwart op wit in de
corpus zelf: `corpus/negatief/wf091-begrensd-eenheid/NOTE.md` schrijft
"WF-035 crosses operand *convertibility* (and `kg`/`t` are convertible by
construction)".

WF-099 roept datzelfde predicaat aan. De regel voegt dus een **positie** toe,
geen vierde begrip van verenigbaarheid.

### 2. Hermeting van mutant N op deze HEAD

Opnieuw gebouwd (een `Begrensd` over `EUR`-attributen met `kg`-grenzen, 10 en
100), niet overgenomen van de kaart:

| consument | uitkomst, gemeten 2026-09-10 |
|---|---|
| verifier (vóór de check) | `verifier: PASS` |
| ref | `EVALUATION FAULT: incompatible units: EUR vs kg` |
| clingo | `TRANSLATION FAULT: non-identical units under a Begrensd bound are not translated` |
| Drools, jar van vóór de omheining | `PLAYED 1` |
| Drools, jar herbouwd (post-omheining) | `ERROR Eenheidsconflict: onverenigbare eenheden bij een §6.1.4 begrenzing-ondergrens: 'kg' (canoniek kg) herschaalt niet naar 'EUR' (canoniek EUR) — er bestaat geen omrekenfactor` |

**Alle drie de uitvoerders weigeren de getuige.** De eerste Drools-rij was een
artefact van de jar-datering: de aanvankelijk beschikbare jar was gebouwd op
2026-09-09 18:11:20, terwijl de commit die de begrenzing-grens omheint
(`2841f4e`) van diezelfde dag 20:27:57 is. Lens 1 heeft de jar herbouwd en de
weigering gemeten; in deze fixronde is dat hier nagedaan met de uitvoer
hierboven. De dateringsslag om de arm vervalt dus. Gate 12 herbouwt de jar
onvoorwaardelijk en is groen op deze branch (lens 1).

**De twee Drools-takken zijn niet één omheining; dat gladstrijken zou
onnauwkeurig zijn.** De exec-model-tak heeft een **klem-specifieke** poort per
grens — `herschaald(effType(grens), effType(basis), "een §6.1.4
begrenzing-{onder,boven}grens")` — en die reden staat letterlijk in de
foutboodschap hierboven. De DRL-tekst-tak heeft zo'n klem-specifieke poort
niet en vangt hetzelfde defect via de **generieke §4-waardeherschaling**
(`omrekenWaarde`). Ze komen wel op dezelfde plek uit:
`Eenheidsconflict.eisHerschaalbaar`, waarvan de eigen getuigetest `kg`/`t` als
herschaalbaar en `jr`/`kg` als fout vastpint. Beide takken toetsen dus
**herschaalbaarheid = convertibiliteit**, langs twee verschillende routes.

Wat de oude PLAYED-rij liet zien is trouwens erger dan "stil": de jar van vóór
de omheining paste de `kg`-grenzen gewoon toe op een `EUR`-basis en klemde door.

### 3. De convertibel-meting — er is géén divergentie, en dat is bepalend

Zelfde document, grenzen verplaatst naar `t` (stelsel `Gewicht`, factor 1000
naar canoniek `kg`) over een `kg`-basis:

| consument | uitkomst |
|---|---|
| verifier | PASS |
| ref | herschaalt: 10 t = 10000 kg |
| clingo | herschaalt: dezelfde waarde (1000000 op schaal 100) |

**Ref en clingo zijn het eens.** De hypothese uit de opdracht — ref accepteert
convertibel, clingo weigert het — is gemeten en weerlegd. De verklaring zit in
`Clingo_backend.Translate.translate`: die draait eerst `Normalise.document`,
dat elk gedeclareerd stelsel naar zijn fijnste basiseenheid canonicaliseert.
Clingo's `units_identical_ok` toetst dus identiteit van **al genormaliseerde**
eenheden, en binnen één stelsel ís die identiteit convertibiliteit.

Bewijs door de factor te variëren: met factor 1 op de `t`-grens gaan ref én
clingo PASS; met factor 1000 herschalen ze allebei, met dezelfde waarde.

**Regelkeuze: convertibel.** Ze is (a) zuster-consistent en (b) precies de
lijn waar alle drie de uitvoerders het eens zijn. Identiteit zou een vorm
weigeren die alle drie spelen — dat is exact het spiegelbeeld van het gat dat
deze kaart dicht, en dus geen verdedigbare strengheid.

### 4. Twee ref/clingo-divergenties die de keuze wél dicht, en één die open blijft

Doorgemeten voorbij de kaartvraag, want de keuze raakt ze:

| vorm | ref | clingo | WF-099 |
|---|---|---|---|
| grens `t` met canoniek `ton` op een `kg`-basis | FAULT | **geeft een model** | **FAIL** — dicht |
| grens `kg` omgelabeld naar stelsel `Munt`, op een `EUR`-basis | FAULT | **geeft een model, met de ongewijzigde waarden** | **FAIL** — dicht |
| grens `t` in een VOLLEDIG gedeclareerd zusterstelsel `Massa`, op een `kg`-basis in `Gewicht` | **10000** | **FAULT** | PASS — **blijft open** |

De tweede rij is de scherpste van de twee gedichte: clingo rekende daar
kilogrammen als euro's en gaf de vastgepinde antwoorden terug.
Convertibiliteit weigert beide (de canonieke namen verschillen),
identiteit-per-stelsel niet.

**De derde rij is in de fixronde gecorrigeerd, en de correctie is inhoudelijk.**
Aanvankelijk stond hier de vorm "grens `t` in een NIET-gedeclareerd stelsel
`Massa`". Die is nagemeten en blijkt al dicht: WF-034 weigert hem, één melding
per grens — `unit 't' declares unit system 'Massa', which no declaration
carries: the unit resolves against no conversion table`. Op die vorm bestaat
dus geen conform document en valt er niets open te laten.

De wérkelijk open vorm is het **kruis-stelsel**-geval met een volledig
gedeclareerd zusterstelsel: `Massa` naast `Gewicht`, met `kg` (factor 1) en `t`
(factor 1000), allebei canoniek `kg`; de basis in `Gewicht`-`kg`, de grenzen in
`Massa`-`t`. Zelf nagebouwd en gemeten 2026-09-10: **verifier PASS**, `ref`
10000, **Drools 10000 op beide takken**, clingo `TRANSLATION FAULT`. Een levende
twee-tegen-één-divergentie op een verifier-schoon document. Oorzaak: clingo's
`Normalise.document` canonicaliseert **per stelsel**, dus twee stelsels
normaliseren onafhankelijk en `units_identical_ok` ziet stelsel-overschrijdend
geen identiteit; `ref` en Drools kijken naar de canonieke naam en negeren het
stelsel. Gekaart als `backlog/new/20260910-kruis-stelsel-eenheid-divergentie.md`
— gemeld én gekaart, niet alleen gemeld.

### 5. Rood eerst, dan groen — met echte uitvoer

`corpus/negatief/wf099-begrensd-grens-eenheid/` (de N-vorm als getuige).

Vóór de check, op deze HEAD:

    $ verifier.exe corpus/negatief/wf099-begrensd-grens-eenheid/ir.json
    verifier: PASS
    rc=0

Na de check:

    $ verifier.exe corpus/negatief/wf099-begrensd-grens-eenheid/ir.json
    WF-099 FAIL /resolvedRegels/0/waarde/minimum: clamp minimum and base have incompatible units
    WF-099 FAIL /resolvedRegels/0/waarde/maximum: clamp maximum and base have incompatible units
    rc=1

Vier falsifieerbaarheidsclaims uit de `NOTE.md` zijn allemaal nagemeten en
kloppen: grenzen naar `t` op een `kg`-basis → PASS; grenzen zonder eenheid →
PASS (adoptie); maximum weg → alleen de minimum-melding; minimum weg → alleen
de maximum-melding. De twee grensposities vallen dus onafhankelijk.

### 6. Arm-exactheid

De check verwijderd via een edit, herbouwd, en de **volle sweep over alle
verifier-gegatete zones — golden 166, negatief 359, rood 26, samen 551
`ir.json`** — vergeleken met de sweep mét check. Precies één document verandert
van verdict:

    < corpus/negatief/wf099-begrensd-grens-eenheid/ir.json  rc=0  verifier: PASS|
    > corpus/negatief/wf099-begrensd-grens-eenheid/ir.json  rc=1  WF-099 FAIL …

Geen enkele andere regelmelding beweegt, in geen enkele zone.

### 7. Over-afwijzing

Gate 3 groen: **192 golden clean**, 359 negatief afgewezen om hun eigen regel.
Eigen sweep over álle 581 `ir.json` — de verifier-gegatete zones (golden 166,
negatief 359, rood 26 = 551) plus de 30 documenten van de pariteitszone — met
de nieuwe binary: WF-099 meldt op **drie** documenten.

- `corpus/negatief/wf099-begrensd-grens-eenheid` — de getuige.
- Twee documenten in de **ongegatete** pariteitszone: de casusbundel en één
  scenariodocument. Gate 11 draait de verifier niet (gemeten: het woord
  "verifier" komt in `scripts/check-toka-parity.sh` niet voor), en allebei
  waren ze al niet-conform vóór deze regel — de casusbundel met 34 andere
  meldingen, het scenariodocument met één eerdere WF-016-bevinding.

Die twee zijn geen toevalstreffer maar een **eenzijdig eenheidspaar**: een
eenheidloze `Binair`-basis met een `0 €`-literal als ondergrens. Alle drie de
uitvoerders laten zo'n paar door; het zusterpredicaat weigert het tenzij de
eenheidloze kant een literal is. Dat is nagemeten in plaats van afgeleid: exact
hetzelfde eenzijdige paar geeft `WF-035 FAIL: additive operands have
incompatible units` op een additieve join en `WF-099 FAIL` op een klem. De
regel erft de houding van haar zusters dus **letterlijk**, inclusief de open
klasse die `spec/IR-SPEC.md` ch. 4.1 al beschrijft ("a one-sided pair whose
unitless side is not an adopting literal"). Die klasse verbreden is één besluit
voor WF-023, WF-035, WF-036 en WF-099 samen — geen divergentie om hier bij één
van de vier in te voeren. Ch. 4.1 noteert nu dat WF-099 zich erbij voegt.

In de verifier-gegatete zones (golden 166, negatief 359, rood 26) is de
over-afwijzing dus **nul**.

### 8. De aangewezen golden

`corpus/golden/jutterswet-grensbedrag` — de **enige** `Begrensd`-drager in
`corpus/golden` (gemeten over alle **166** golden-`ir.json`; de andere dragers
zijn `corpus/negatief/wf091-begrensd-eenheid`,
`corpus/rood/begrensd-reads-bounds-in-order` en twee documenten in de
pariteitszone). Er is dus een bestaande drager en er is geen nieuwe golden
gebouwd, precies zoals de kaart voorschrijft.

**Eerlijk erbij, want het is een zwakte:** die golden is op basis én op beide
grenzen eenheidloos, dus hij pint de toegelaten allebei-eenheidloos-lezing en
niet de convertibele helft. Géén golden draagt een eenheid-dragende klem. Dat
is niet stiekem gelaten: zo'n golden erbij zetten verplaatst **gate 12's
`EXPECTED_PLAYED=119` (een gelijkheid)** en **gate 13's twist-tabel-boekhouding**
(die in zoveel woorden vastlegt dat de `Begrensd` "on this corpus always
UNITLESS" is), en die twee staan in `scripts/`, een beschermd pad buiten deze
kaart. Aangemaakt als `backlog/new/20260910-begrensd-eenheid-golden.md`, met
allebei die scriptbewegingen expliciet als voorstel benoemd; de clausule en de
registerrij verwijzen er bij bestandsnaam naar, zodat het gat gekaart is en
niet alleen gemeld.

### 9. Wat er landde

- `spec/verifier/lib/checks.ml` — de WF-099-arm op de `Begrensd`-tak, direct
  onder de bestaande WF-035-arm, met het zusterpredicaat.
- `spec/IR-SPEC.md` — de WF-099-clausule in ch. 4.6, direct achter WF-035 en
  WF-036 (plaatsing op onderwerp, zoals WF-097 en WF-098 ook doen); de
  registerrij (6 pipes, gelijk aan 98 van de 99 WF-rijen); en één alinea in
  ch. 4.1's open-klasse-passage die vastlegt dat WF-099 zich bij die klasse
  voegt in plaats van haar te verbreden.
- `corpus/negatief/wf099-begrensd-grens-eenheid/` — `ir.json` + `NOTE.md`.
- `backlog/new/20260910-geneste-aggregatie-rolketen.md` en
  `backlog/new/20260910-reverse-rol-via-naam.md` — hun kandidaat-nummer
  schuift van WF-099 naar **WF-100**, zelfde patroon en zelfde formulering als
  de vorige schuif toen WF-098 bezet raakte.

### 10. Gates

    check-self-contained: PASS
    check-ir-schema: PASS (schema valid; 192 file(s) validate and round-trip, floor 192)
    check-verifier: PASS (192 golden clean; 359 negatief rejected for their declared rule, floor 350)
    check-oracle: PASS (ref suite 489 worked examples, floor 489; 427 cases replayed exactly, floor 427)
    check-clingo: PASS (backend suite 86 checks, floor 83; 427 cases replay exactly through clingo — ref = clingo over the corpus)

### 11. Voorgestelde CONSTRUCTS-zin (spec/CONSTRUCTS.md NIET aangeraakt)

Het construct hangt aan **rij 1** ("Typed expression trees (arithmetic,
comparison, functions, aggregation)") — de knoopcatalogus zet `Begrensd`
expliciet op rij 1 (`| `Begrensd` | clamp to min/max | §13.4.17.27–.30 | sugar
| 1 | min(max(x, lo), hi) |`). Voorgestelde toevoeging aan rij 1, in de
`**Landed (…):**`-vorm van haar buren:

> **Landed (2026-09-10):** WF-099 (IR-SPEC ch. 4.6) sluit de laatste
> onomheinde eenheidspositie van de gemeten set — de eenheid van een
> `Begrensd`-grens wordt tegen die van haar BASIS gekruist, door hetzelfde
> predicaat dat WF-035 op een additief paar en WF-036 op een vergelijking
> gebruikt, zodat de regel een positie toevoegt en geen vierde begrip van
> verenigbaarheid; elke grens kruist de basis en niet haar zustergrens, omdat
> de basis is waar een grens IN wordt herschaald — RUNTIME-SEMANTICS ch. 2.7
> is enkel de teruggeefwet (basis, minimum of maximum, minimum eerst,
> vergelijkingen exact) en draagt geen eenhedenzin, ch. 2.10 geeft de klem als
> composiete positie het effectieve type van haar BASIS, en ch. 2.6 draagt de
> herschaling zelf (een operand herschaald in de eenheid van de ander met
> exacte factoren, een vergelijking over eenheden heen die BEIDE zijden
> herformuleert) — en omdat elke vergelijking van een klem
> basis-tegen-grens is, volgt min-tegen-max daaruit vanzelf. Rood eerst op deze
> HEAD: een klem over een `EUR`-basis met beide grenzen in `kg` was
> verifier-PASS terwijl `ref` faalde (`incompatible units: EUR vs kg`), de
> ASP-backend de vertaling weigerde en de Drools-backend
> `Eenheidsconflict: … 'kg' … herschaalt niet naar 'EUR'` gaf — de verifier
> liet door wat alle drie de uitvoerders weigeren; de twee Drools-takken komen
> daar langs verschillende routes (een klem-specifieke §6.1.4-poort in de
> exec-model-tak, de generieke §4-waardeherschaling in de DRL-tak). De lijn is
> CONVERTIBILITEIT en is gemeten in plaats van uit de boodschappen afgelezen,
> want die spreken elkaar tegen (`ref` zegt incompatible en toetst
> convertibiliteit; de ASP-backend zegt non-identical maar toetst identiteit
> van eenheden die per gedeclareerd stelsel al genormaliseerd zijn; de
> Drools-backend toetst herschaalbaarheid): een `t`-grens op een `kg`-basis
> wordt door `ref` en de ASP-backend naar dezelfde waarde herschaald zonder
> fout op één van beide benen, dus identiteit zou een vorm geweigerd hebben
> die alle drie spelen. De keuze sluit bovendien twee gemeten
> ref/ASP-divergenties (een grens waarvan de `canoniek` het stelsel dat ze
> noemt tegenspreekt; een grens omgelabeld naar het stelsel van de basis,
> waar de ASP-backend stilzwijgend de ongewijzigde waarden teruggaf). Wat ze
> NIET sluit is niet de vorm die er eerst voor werd aangezien: een grens die
> een NIET-gedeclareerd stelsel noemt weigert WF-034 al. Open blijft de
> KRUIS-STELSEL-grens met een volledig gedeclareerd zusterstelsel — `Massa`
> naast `Gewicht`, allebei canoniek `kg` — waar `ref` en Drools 10000 rekenen
> en de ASP-backend weigert te vertalen: een levende twee-tegen-één-divergentie
> op een verifier-schoon document, gekaart als
> `backlog/new/20260910-kruis-stelsel-eenheid-divergentie.md`. Arm-exact en
> kosteloos: de check weghalen verplaatst precies één verdict over de
> verifier-gegatete zones (golden 166, negatief 359, rood 26 — 551 documenten).
> Getuige `wf099-begrensd-grens-eenheid`; positieve kant
> `jutterswet-grensbedrag`, de enige klem-drager van de corpus en overal
> eenheidloos — géén golden draagt een eenheid-dragende klem, gekaart als
> `backlog/new/20260910-begrensd-eenheid-golden.md` in plaats van
> binnengesmokkeld, omdat er één gate 12's `EXPECTED_PLAYED`-gelijkheid en
> gate 13's twist-tabel-boekhouding verplaatst.

### 12. Wat bewust niet bewoog

- **`ref/`, `backends/clingo/`, `backends/drools/`** — geen bronbestand
  aangeraakt. Ze weigeren de vorm alle drie al; dat is de kaarttekst zelf. De
  Drools-jar is wél herbouwd (alleen build-uitvoer in `target/`, geen
  getrackt bestand) om de post-omheining-weigering zelf te kunnen meten in
  plaats van hem uit de bron af te leiden — zie §2.
- **`scripts/`** — onaangeraakt. Geen enkele teller of gepinde lijst hoefde
  te bewegen: de negatief-vloer is 350 en staat nu op 359, gate 2's
  golden-telling is ongewijzigd op 192, en gate 12/13 zien geen nieuwe golden.
- **`spec/CONSTRUCTS.md` en `spec/PLAN.md`** — onaangeraakt; de rij-zin staat
  in §11 zodat de hoofdsessie hem serieel landt.
- **`corpus/golden/`** — onaangeraakt; er is een bestaande drager (§8).
- **De open klasse van het eenzijdige eenheidspaar** — niet hier beslist; ze
  is één besluit voor vier regels samen en staat nu in ch. 4.1 genoteerd.
- **De kruis-stelsel-divergentie** (§4, derde rij) — gemeten, opgeschreven in
  clausule én registerrij, en gekaart als
  `backlog/new/20260910-kruis-stelsel-eenheid-divergentie.md`; niet hier
  gedicht, want de keuze raakt WF-023/035/036/099 tegelijk.
- **De twee pariteitszone-documenten** — niet gerepareerd; die zone gaat niet
  door de verifier en beide documenten waren al niet-conform.

### 13. Fixronde (2026-09-10)

Verwerkt na de terugkoppeling van lens 1, die alle metingen reproduceerde en
vier correcties vond:

1. **Grondslagcitatie hersteld** in clausule én registerrij. RUNTIME-SEMANTICS
   ch. 2.7 draagt geen eenhedenzin — het is enkel de teruggeefwet. De
   herschaling staat in ch. 2.6 en het effectieve type van de klem in ch. 2.10.
   Alle drie worden nu apart en juist geciteerd; de dubbele bepaler in "the
   restating the runtime performs" is weg.
2. **De open-passage gecorrigeerd** — inhoudelijk, niet redactioneel: de
   niet-gedeclareerd-stelsel-vorm is al dicht (WF-034, zelf nagemeten), en de
   werkelijke open klasse is de kruis-stelsel-grens met een volledig
   gedeclareerd zusterstelsel. Zelf nagebouwd en gemeten (§4).
3. **Twee kaartjes aangemaakt**, zodat "gemeld maar niet gekaart" dicht is:
   `20260910-begrensd-eenheid-golden.md` en
   `20260910-kruis-stelsel-eenheid-divergentie.md`. Clausule, registerrij en
   Result verwijzen er bij bestandsnaam naar.
4. **Tellingen uitgeschreven** in plaats van als "de vier zones" gelabeld
   (golden 166 + negatief 359 + rood 26 = 551, pariteitszone 30 apart), de
   praktijk die ch. 4.1 voorschrijft; en "130 goldens" in §8 gecorrigeerd naar
   166.
5. **Jar-datering vervallen**: de jar is herbouwd en Drools weigert de getuige
   met `Eenheidsconflict`. Alle drie de uitvoerders weigeren hem dus. Gate 12
   herbouwt onvoorwaardelijk en is groen op deze branch (lens 1).
6. **De twee Drools-takken niet langer gladgestreken**: klem-specifieke
   §6.1.4-poort in de exec-model-tak, generieke §4-waardeherschaling in de
   DRL-tak, allebei uitkomend in `Eenheidsconflict.eisHerschaalbaar`.
7. **Het pariteitszone-scenario-ID uit de Result gehaald** en vervangen door
   een klasse-aanduiding (beheerdersafspraak; oudere done-kaarten die het
   patroon dragen blijven staan als historie).
