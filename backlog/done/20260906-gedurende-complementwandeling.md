# De ontkende `gedurende`-clip: een levende `ref ≢ clingo`

Goal: de negatie-tak van de `gedurende`-clip uit de divergentie halen. Op een
**welgevormd, verifier-PASS** document rekent de referentie-interpreter de
complementklip gewoon uit, terwijl de ASP-backend hem met reden weigert. Dat is
geen dichte deur en geen stilte: het is een levende `ref ≢ clingo` op een vorm
die vandaag door elke poort komt. Deze kaart kiest één van twee antwoorden —
de backend leert de complementwandeling, óf de vorm wordt (tijdelijk, met een
WF-regel) op de wire geweigerd — en maakt de keuze machinaal toetsbaar.

## De meting (2026-09-06, eigen meting)

`corpus/golden/anchor/jutterswet-dubbelwacht`, uitgepakt naar `/tmp`, met
`negatie: true` op één kenmerk in de clip. Twee vormen gemeten: het
`ALLE`-samenstel met één ontkend lid, en een kale ontkende `Kenmerk`-clip.
Beide zijn **verifier PASS** — geen enkele WF-regel raakt ze.

| case | positieve vorm | `ref`, ontkend | ASP-backend, ontkend |
| --- | --- | --- | --- |
| `case1-overlap` | 30 | **30** (PASS) | weigert |
| `case3-een-leeg` | 0 | **120** | weigert |

De weigering luidt:

    TRANSLATION FAULT: the negated gedurende clip (the complement walk) is not
      translated (no corpus case exercises it)

`case3-een-leeg` is de scherpste: het kenmerk heeft daar géén tijdlijn, de
positieve clip levert dus 0, en de **complementwandeling dekt het hele jaar** —
`ref` antwoordt 120 (twaalf maanden × 10). Er is dus echte, betekenisvolle
semantiek aan de ref-kant; de backend heeft haar niet.

Beide vormen — het samenstel met een ontkend lid en de kale ontkende clip —
geven exact hetzelfde beeld: ref rekent, de backend weigert.

## Waarom dit nu een kaart is

De weigeringstekst zegt *"no corpus case exercises it"*, en dat klopt: het
corpus draagt de vorm niet (zie de regressiegrens hieronder). Maar
"onbereikbaar in het corpus" is iets anders dan "ill-formed". De vorm is
**welgevormd**: de verifier laat hem door, de referentie-interpreter geeft er
een antwoord op, en dat antwoord is niet triviaal. Zolang dat zo is, staat er
een `ref ≢ clingo` in de gate-set die alleen niet vuurt omdat niemand hem
schrijft — precies het patroon dat het huis elders (het WF-092-eerlijkheids-
patroon) niet accepteert.

De weigering zelf is overigens **niet** de fout: luid weigeren is beter dan
stil verkeerd rekenen, en dat deed de backend hier goed. De vraag is welke van
de twee artefacten moet bewegen.

## De twee antwoorden

**A. De backend leert de complementwandeling.** De ASP-vertaling krijgt het
complement van een kenmerktijdlijn binnen de klip-horizon, zodat de doorsnede
op de segmenten *buiten* de gedekte periodes valt. Toetsbaar: de anker-twists
hierboven worden goldens (of unittests) en `ref ≡ clingo` op alle vier de
anker-cases, met de absolute waarden gepind — 30 en 120 zijn al gemeten en
kunnen letterlijk als verwachting mee.

**B. De vorm wordt op de wire geweigerd.** Een WF-regel weigert een ontkende
kenmerkcheck binnen een `gedurende`-clip, met een `corpus/negatief/`-getuige
die zijn mapnaam citeert. Dan verandert `ref` mee — hij moet de vorm dan óók
weigeren in plaats van 120 te antwoorden — anders verplaatst de divergentie
zich alleen maar van de backend naar de verifier.

Antwoord A is de eerlijke uitbreiding en B de eerlijke inperking; beide zijn
te toetsen. Wat **niet** kan is de huidige toestand met een comment
goedpraten: één uitvoerder die rekent en één die weigert, is per definitie
geen `ref ≡ clingo`.

## Regressiegrens (gemeten 2026-09-06)

497 `ir.json`-bestanden in `corpus/golden`, `corpus/rood` en
`corpus/negatief`; 34 `Functie`-nodes met een filter (`gedurende`-clips) over
22 bestanden; 33 `Kenmerk`-nodes bínnen zo'n clip, waarvan **0 met
`negatie: true`**. Antwoord B kost dus vandaag geen enkele corpusregressie, en
antwoord A voegt de eerste dragers toe. Bij het landen opnieuw tellen, niet
overschrijven.

## Acceptance

- Een genoteerde keuze tussen A en B, met de reden.
- **Rood eerst**: de gekozen getuige faalt aantoonbaar vóór de wijziging.
- Bij A: `ref ≡ clingo` op de vier anker-afgeleide cases met absolute,
  handgeschreven verwachtingen (30 en 120 zijn gemeten en bruikbaar), en de
  weigeringstekst verdwijnt uit `backends/clingo/lib/translate.ml`.
- Bij B: een WF-regel met verifier-check, minstens één `corpus/negatief/`-geval
  dat de regel in zijn mapnaam noemt, én `ref` die dezelfde vorm weigert — de
  twee uitvoerders zeggen hetzelfde.
- De regressiegrens hierboven opnieuw geteld en in het `Result:` genoteerd.
- Alle dertien gates groen.

May touch: `backends/clingo/**`, `ref/**`, `spec/verifier/**` (als
**voorstel** — beschermd pad), `corpus/golden/**`, `corpus/negatief/**`,
`backlog/`.

## Out of scope

- De stille kwalificaties op dezelfde `Kenmerk`-node (`eigenaar`, `periode`,
  de null-granulariteit, het statisch geleverde temporele kenmerk). Eigen
  kaart: `20260906-gedurende-kwalificatie-stiltes.md`.
- De temporaliteitspoort op de positieve arm: die staat en spiegelt `ref`
  exact.

## Result (2026-09-09)

**Keuze: A** — de backend leert de complementwandeling (maintainerbesluit
2026-09-09, genummerde vraag 4). Reden: de vorm is welgevormd, de verifier
laat hem door, ref geeft er een niet-triviaal antwoord op, en de frontend
draagt hem uit gewone RegelSpraak-bron. Inperken zou een vorm weigeren die
de toolchain gewoon schrijft.

**Rood eerst.** `corpus/golden/jutterswet-tegenwacht` (nieuw, eigen
Jutterswet-materiaal naast het gepinde anker) draagt beide vormen die de
kaart mat: het `ALLE`-samenstel met één ontkend lid (`tegenwachtsom`) en de
kale ontkende `Kenmerk`-clip (`stilwachtsom`). Op het definitieve artefact,
vóór de wijziging: verifier PASS, ref PASS op alle cases, clingo
`TRANSLATION FAULT: the negated gedurende clip (the complement walk) is not
translated` op alle cases. Na de wijziging: alles PASS.

**De vertaling.** `backends/clingo/lib/translate.ml`: de weigeringstekst is
weg. De ontkende arm leidt per waardesegment het complement af met twee
kandidaatpredicaten (`clN_not_lo`/`clN_not_hi`: een segmentrand of de rand
van een gedekte periode), een dekkingspredicaat `clN_not_cov` en een regel
die een niet-gedekt paar binnen één segment aanneemt. Maximaliteit volgt uit
de kandidaatregels, niet uit een aggregaat. De complementranden zaten al in
de `clN_wu`-cutset, dus de kalenderfeiten veranderen niet. De vorm staat in
`spec/ASP-TRANSLATION.md` S8; de weigering is uit de S19-inventaris.

**Pins — eerst met de hand, daarna door de uitvoerders bevestigd.** Zes
cases, twee attributen per case, twaalf absolute waarden. De 30 (case1) en
120 (case3) uit de kaart zijn nagerekend en kloppen.

| case | tegenwachtsom (ALLE, één lid ontkend) | stilwachtsom (kale ontkende clip) |
| --- | --- | --- |
| `case1-overlap` | 30 | 30 |
| `case2-disjunct` | 30 | 90 |
| `case3-een-leeg` | 120 | 120 |
| `case4-gespleten` | 20 | 60 |
| `case5-gaten` | 70 | 100 |
| `case6-ch36-voorbeeld` | 115 | 115 |

`case5-gaten` is nieuw ten opzichte van de vier anker-cases: een
waardetijdlijn met een gat en twee verschillende waarden, die de wandeling
per segment vastlegt. `case6-ch36-voorbeeld` is het uitgewerkte voorbeeld
van `RUNTIME-SEMANTICS.md` ch. 3.6 zelf (de ontkende vorm, 115): dat stond
alleen in `ref/` en is nu ook door clingo en Drools gepind.

**Waarom naast het anker en niet erin.** `corpus/golden/anchor/` is een
beschermd pad: een sessie stelt er hooguit een diff voor en merget hem
nooit zelf. Daarom staat `jutterswet-tegenwacht` in `corpus/golden/` en niet
in de anker-deelverzameling. (De "May touch" van de kaart sluit
`corpus/golden/**` in en dus ook `anchor/`; het is de beschermde-padenregel
die de plaatsing bepaalt, niet de kaarttekst.) Dat heeft één gevolg dat
benoemd hoort te worden: het uitgewerkte ch. 3.6-voorbeeld (115) is nu in
een gewone golden gepind, niet in de anker-deelverzameling met haar
permanente pin. Daar staat tegenover dat deze golden een `bron.rgs` draagt
en het anker niet: gate 10 bewaakt hem byte-identiek vanaf de bron, wat
anker-goldens juist niet krijgen. Als de maintainer het ch. 3.6-voorbeeld
liever in `anker`-status heeft, is dat een aparte, bewuste verplaatsing —
niet iets wat deze kaart eenzijdig moest doen.

**Stratificatie — onderzocht en vastgelegd.** Een ontkende kenmerkcheck in
een clip is wel degelijk een **negatieve rand** in de WF-045/046-wandeling:
de verifier loopt het `Functie`-filter in (`dep_cond ... ~neg:false`) en een
lid met `negatie: true` levert `Dep_neg`. Een cyclus erdoorheen wordt
geweigerd — nieuwe getuige
`corpus/negatief/wf046-negated-clip-kenmerk-cycle`, die zowel WF-046 als
WF-045 laat vuren (de cyclus loopt niet door een recursieve creatie; de
bestaande `wf046-negated-read-in-recursive-cycle` isoleert de regel zelf).
Die getuige pint de **route**, niet een nieuwe regel: de verifier is door
deze kaart niet aangeraakt en wees hetzelfde document op `main` al identiek
af. Wat vastligt is dat een filterconditie de afhankelijkheidswandeling
bereikt en dat `negatie: true` er als negatieve rand in gaat — het feit
waarop de stratificatie-uitspraak hieronder steunt.
Het geëmitteerde programma blijft daarmee gestratificeerd: `clN_not_cov`
leest alleen `ktl/4`-casusfeiten en `tl/5` en leest deze helper niet terug.
Los nagemeten op het nagebouwde fragment: clingo geeft precies één stabiel
model (`Models : 1` onder `--models=0`).

**Gate 12.** De nieuwe getuige **speelt** in Drools: loader en transpiler
droegen de vorm al, en de gepinde waarden klopten meteen. `EXPECTED_PLAYED`
117 → 118, geen kanaalregel nodig. Daarmee staat het volle vierkant op deze
vorm: ref ≡ clingo ≡ Drools.

**Regressiegrens, hermeten 2026-09-09** (de meting van 2026-09-06 was
verlopen): op de gerebasede kop 527 `ir.json` in `corpus/golden` +
`corpus/rood` + `corpus/negatief` (was 497; 526 vóór de rebase — het
verschil is het negatief dat met `main` meekwam); 39 `Functie`-nodes met
een filter over 25
bestanden (was 34 over 22); 37 `Kenmerk`-nodes bínnen zo'n clip (was 33),
waarvan **3 met `negatie: true`** (was 0) — de eerste dragers: twee in de
nieuwe golden, één in de nieuwe negatief-getuige.

**Dertien gates groen — gemeten op de gerebasede kop** (2026-09-09, branch
op `main` = `76c11e2`, waarop de caselezer- en dagsoortkaart al geland
waren; het tweede-merge-regime): 1 PASS; 2 PASS (188 bestanden); 3 PASS
(188 golden schoon, **339** negatief); 4 PASS (448 cases); 5 PASS (425
replays); 6 PASS (425 replays, ref = clingo); 7 PASS (389 cases, 36
survivors); 8 PASS (23 rode cases); 9 PASS (201 checks, 12 catala-goldens);
10 PASS (173 JUnit, 38 rgs-goldens); 11 PASS (29 PLAYED); 12 PASS (played
118, kanaalvloer 47, **JUnit 223**); 13 PASS (74 twists over 15 goldens).

De rebase verliep zonder conflict. Twee tellingen verschoven ten opzichte
van de meting vóór de rebase, beide door `main` en niet door deze kaart:
gate 3 van 338 naar 339 (het negatief van de dagsoortkaart komt bovenop de
337 van vóór en de mijne), en gate 12 van JUnit 210 naar 223 (de suite van
de Drools-backend groeide op `main`). Verder week geen enkele telling af.

**Bewust buiten scope gelaten:** de vier stille kwalificaties op dezelfde
`Kenmerk`-node (eigen kaart `20260906-gedurende-kwalificatie-stiltes`), en
de temporaliteitspoort op de positieve arm. Eén nevenwerking, niet
teruggedraaid: een ontkende kenmerkcheck mét navigatie viel vroeger in de
weigering van deze arm en valt nu in de algemene
"unsupported gedurende condition shape"-weigering — dezelfde tekst die ref
er al voor gaf, dus de twee uitvoerders lopen daar nu gelijk.

**Gemeten nevenklasse, niet door deze kaart geïntroduceerd.** Op een
document waar het ontkende kenmerk door een regel wordt afgeleid (een
temporele KENMERKTOEKENNING, zónder cyclus, dus verifier PASS) faalt `ref`
met `temporal writes are not implemented` terwijl clingo nu rekent; die
divergentie bestond op `main` al precies zo op de **positieve** arm, die
deze kaart niet raakt (gemeten: ontkend 90, positief 30, ref faalt in beide
gevallen), dus de ontkende arm gedraagt zich voortaan als de positieve in
plaats van een eigen klasse te openen. De flank hoort bij
`20260906-gedurende-kwalificatie-stiltes`; reproductie:
`/tmp/probe-strat/no-cycle*.json`.

**Beschermde paden — alle drie als VOORSTEL, alleen de maintainer merget.**
Boekhouding vooraf: **alle drie vallen buiten de "May touch" van de kaart.**
Die noemt van `spec/` alleen `spec/verifier/**`, dus zowel
`spec/RUNTIME-SEMANTICS.md` als `spec/ASP-TRANSLATION.md` staat erbuiten, en
`scripts/` wordt er helemaal niet genoemd. Alle drie zijn door de opdracht
aangewezen (de ASP-vorm hoort in `ASP-TRANSLATION.md`, RUNTIME-SEMANTICS
alleen als klein voorstel, de speelvloer moest meebewegen); geen ervan is
een eigen uitbreiding van de scope.

- `spec/RUNTIME-SEMANTICS.md`: de executed-note bij het ontkende
  samenstel-lid beweerde dat de ASP-kant luid faalt en dat geen anker het
  differentieel kan pinnen. Dat is nu onwaar; de zin noemt de getuige. Eén
  hunk, 6 regels uit en 6 in, waarvan 3 pure herafbreking van ongewijzigd
  proza (de ten-minste-1-zin) om binnen de regelbreedte van de alinea te
  blijven.
- `spec/ASP-TRANSLATION.md`: de S8-vertaalvorm van de ontkende clip en het
  schrappen van de weigering uit de S19-inventaris. De opdracht wees dit
  document expliciet aan als de plek voor de ASP-vorm. Het staat níét in de
  beschermde-padenlijst van `CLAUDE.md`, maar wél binnen de `/spec/`-claim
  van `.github/CODEOWNERS` — die twee lopen uiteen. Het wordt hier daarom
  onder dezelfde voorstel-kwalificatie gemeld; het verschil zelf is
  opgeschreven als `backlog/new/20260909-beschermde-paden-lijsten-gelijk.md`.
- `scripts/check-drools.sh`: `EXPECTED_PLAYED` 117 → 118 met
  reden-commentaar. `scripts/` is een beschermd pad én viel buiten de
  "May touch" van de oorspronkelijke kaart; de bump was onvermijdelijk
  omdat gate 12 de speelvloer bij naam en getal pint en een nieuwe golden
  die meespeelt de gate anders rood laat lopen — precies zoals de gate is
  bedoeld. De opdracht vroeg de teller mee te bewegen op deze branch. Geen
  kanaalregel toegevoegd; de getuige speelt.

**Slotronde (2026-09-09).** Branch gerebased op `main` = `76c11e2`
(caselezer- en dagsoortkaart), zonder conflict; de volledige dertien gates
zijn daarna op de gerebasede kop gedraaid, met de twee verschuivingen
hierboven en geen andere afwijking. Twee Results-correcties verwerkt: de
plaatsing van de golden buiten `anchor/` steunt op de beschermde-padenregel
en niet op de kaarttekst (de "May touch" sluit `anchor/` juist in), en de
may-touch-boekhouding geldt voor alle drie de geraakte paden, niet alleen
voor `scripts/`. Eén nieuw kaartje geschreven:
`backlog/new/20260909-beschermde-paden-lijsten-gelijk.md`, over de
uiteenlopende dekking van de prozalijst in `CLAUDE.md` en de `/spec/`-claim
in `.github/CODEOWNERS`.
