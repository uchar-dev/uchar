# gate12-drl-takteller: de DRL-tekst-tak telbaar maken

Gemeten spanning (asp-ch102-bouwer, 2026-09-19): gate 12's
`EXPECTED_PLAYED`-gelijkheid en kanaallijst tellen uitsluitend het
exec-model-kanaal. De vijf nieuwe ch.-10.2/10.6-goldens spelen alleen
daar; dat de DRL-tekst-tak ze om drie pre-existente redenen niet
speelt, staat alleen in het reden-commentaarblok van de bump — en
commentaar is documentatie, geen check. Het vierkant is op die vormen
groen door de takkeuze van de gate, niet door takdekking. De
DRL-PLAYED-stand (nu 132, na de twee Let-kaarten van 2026-09-19) werd in het pariteitsprogramma handmatig in
Results bijgehouden; niets hekt hem tegen stille krimp behalve gate
13's twist-vloer (DRL-PLAYED ≥ 13, alleen over twist-goldens).

Voorstel om te wegen: een eigen `EXPECTED_DRL_PLAYED` in
`scripts/check-drools.sh` (gelijkheid, zoals de exec-pin, met
reden-blok bij elke beweging) plus een reden-dragende
niet-spelerslijst voor de DRL-tak, of — lichter — alleen de
gelijkheidspin. Kosten: elke kaart die een golden in de DRL-tak laat
spelen of verliest raakt voortaan het beschermde script (dat is bij de
exec-pin al zo en bleek werkbaar).

## Acceptatie (bij oppakken aanscherpen)

- (a) Besluit gelijkheidspin / vloer / beide, met de reden erbij.
- (b) De gate draait de DRL-sweep zelf (nu draait hij de replay alleen
  exec) — meet de looptijdkosten en weeg ze.
- (c) Beschermd-padvoorstel voor `scripts/check-drools.sh`; alle
  veertien gates groen; de bestaande exec-pins bewegen niet.

May touch: `scripts/check-drools.sh` (voorstel — beschermd pad),
kaartverplaatsing. Besluit aan de maintainer.

## Maintainersbesluit (2026-09-19): de volle vorm

Besluit (a): een `EXPECTED_DRL_PLAYED`-gelijkheidspin mét een
reden-dragende niet-spelerslijst voor de DRL-tak; de gate draait de
DRL-sweep zelf en de looptijdkosten worden gemeten en gemeld in het
Result. Uitvoerbaar met dit mandaat (beschermd pad via de
gebruikelijke lens- en maintainersmerge-route).

## Result (2026-09-19, branch `card/drl-takteller`, van 2d7f0399)

**De stand is met de hand nagemeten en komt uit op 132.** Een losse sweep
over alle 209 goldens via de eigen vlag van de replay-runner, gedraaid vóór
er één regel aan de gate veranderde, gaf: 132 PLAYED, 70 EXCLUDE, 3 FAIL,
2 ERROR, 2 zonder casusbestand. Dat is precies de 132 die de twee
Let-kaarten van 2026-09-19 achterlieten, dus er viel niets uit te zoeken —
de verwachte stand en de gemeten stand vielen samen en dát is wat gepind
staat. Ter vergelijking het exec-kanaal ernaast: 159 van dezelfde 209.

### Wat er in `scripts/check-drools.sh` bij is gekomen

`EXPECTED_DRL_PLAYED=132`, een gelijkheid, met een redenblok naar het model
van de exec-pin: datum, eerste meting, waar het getal vandaan kwam, en de
regel dat een golden dat gaat spelen de lijst verlaat én dit getal met één
verhoogt.

`DRL_CHANNEL`, 77 regels `"<golden>|<kanaal>|<reden>"`, dezelfde vorm als
`EXPECTED_CHANNEL`: spelen is de default, alleen niet-spelers staan er, en
alleen het kanaal wordt vergeleken. De kanalen zijn de vier statustokens die
de tak zelf drukt, zodat wat gepind is precies is wat gemeten wordt. Per
redenklasse, elk met een groepskop die de klasse uitlegt en met elke golden
bij naam eronder:

| klasse | n | reden |
| --- | --- | --- |
| A | 24 | `EXCLUDE` — ch. 7 prioritizedDefaults; deze tak heeft die lowering niet |
| B | 34 | `EXCLUDE` — loader-weigering op een v1-knoop; gedeeld met exec |
| C | 12 | `EXCLUDE` — een MARKER van de DRL-transpiler, per vorm |
| D | 4 | `FAIL` (2) + `ERROR` (2) — het objectcreatie-defect, zie hieronder |
| E | 1 | `FAIL` — de negatie-stratificatie, identiek in beide takken |
| F | 2 | `NOCASE` — geen casusbestand |

De sweep zelf draait als laatste blok van de gate, ná het exec-vierkant, met
de twee guards die de exec-lijst ook heeft (kanaaldrift bij naam; een pin
waarvan het golden weg is) plus de holle-token-guard (`PLAYED 0` telt niet
als speler).

**De groepskoppen van A en C zijn na de lensronde gecorrigeerd, met een
eigen exec-sweep over alle 209 goldens ernaast gelegd.** Wat er nu staat is
gemeten en niet geschat:

- Klasse C is níét tien-om-twee gedeeld. **Negen** van de twaalf weigert
  exec ook (`jutterswet-keurstaat` hoort daarbij — exec: `transpiler SKIP:
  voorwaarde buiten Phase-B scope` — net als `jutterswet-klipwacht`).
  **Drie** zijn tak-alleen, en exec speelt ze: `anchor/jutterswet-wachtersketen`
  (PLAYED 2), `synth2-budgetgrens-onder` (PLAYED 1) en `jutterswet-vouwrand`
  (PLAYED 1). De eerste twee dragen een objectcreatie-MARKER op dezelfde
  onopgeloste terugkoppeling — dat is de **luide** helft van het
  objectcreatie-gat, waar klasse D de stille helft is. Beide koppen wijzen
  nu naar elkaar, zodat het gat compleet in het bestand staat.
- Klasse A framede `anchor/jutterswet-toelagegronden` ten onrechte als deel
  van de asymmetrie tussen de takken. Exec weigert dat golden óók, op een
  andere zin (kenmerk-doel deelt zijn naam met een attribuut, een
  v0-beanmodel-limiet), dus het draagt 0 bij aan het gat van 27. De kop
  noemt dat nu expliciet en rekent het gat voor: 30 exec-only-spelers min 3
  goldens die deze tak speelt en exec niet (`anchor/jutterswet-vindersloon`,
  `jutterswet-vondsttarief`, `synth2-afrondingscombinaties`). De pinregel
  van toelagegronden zelf was en blijft correct.

**Eén verschil is bewust en staat als zodanig in het bestand:** hier is een
fout bij het vuren *lijstbaar*. In het exec-kanaal laat een `ERROR` de gate
vallen, want dat kanaal ís het vierkant; deze tak is een smallere meting die
tot nu toe aan geen enkele corpuswaarde gehouden werd, en een bestaand
defect tot rode gate promoveren was deze kaart niet.

### Wat de teller meteen opleverde (klasse D)

Vier goldens dragen elk precies één `objectCreatie`; het exec-kanaal speelt
ze alle vier, deze tak speelt ze niet én weigert ze niet bij naam. Twee
antwoorden leeg waar ref, clingo en exec een getal geven
(`anchor/jutterswet-rekenkamer`: 70 / 160 / -15 / 60 →
leeg; `jutterswet-rekenkamerheffing-catala` idem), twee vallen bij het vuren
om met een reflectieve `NullPointerException` in de consequence van de
creatieregel (`anchor/jutterswet-rekenkamer-overzee`, `synth3-kamerheffing`).
Dat is geen semantisch verschil en geen niet-gedragen vorm, maar een gat in
de creatie-lowering van deze tak. Het staat nu bij naam gepind, zodat
repareren de lijst *moet* bewegen. Niet gerepareerd — dat is backend-werk
met een eigen kaart: `backlog/new/20260919-drl-objectcreatie-defect.md`, in
deze branch aangemaakt (zie onderaan).

### Looptijd, wall-clock gemeten op deze branch

| run | totaal |
| --- | --- |
| gate 12 zoals op `main` (`git show main:` — gedraaid in deze worktree) | 1:34,7 |
| gate 12 met de sweep erin | 3:20,0 |

De sweep meet zichzelf ook en drukt dat in het slotrapport. Drie metingen
gaven 96s, 97s en (door de lens) 100s, voor 207 java-aanroepen (de twee
goldens zonder casus kosten er geen). Het getal staat daarom als **~100s**
in het script én hier: het is een orde van grootte en geen constante, en
het verse getal van de run zelf is wat je moet geloven. De prijs is ruwweg
een verdubbeling van de gate, en die wordt alleen betaald door een gate die
verder groen is: bij een gebroken vierkant faalt hij nog steeds in de tijd
die hij altijd had — de lens mat dat na op 1m54 zonder de DRL-regels te
bereiken.

### Mutatieproeven

| proef | uitkomst |
| --- | --- |
| pin 132 → 131 | `FAIL — the DRL-text branch played 132, but EXPECTED_DRL_PLAYED pins 131 — …`; exit 1 |
| spelend golden als `EXCLUDE` in de lijst (`anchor/jutterswet-borgdeel`) | `FAIL — 1 golden(s) changed channel in the DRL-TEXT branch:` / `anchor/jutterswet-borgdeel :: pinned EXCLUDE, measured PLAYED — PLAYED 6`; exit 1 |
| verzonnen naam in de lijst (`jutterswet-verzonnen-golden`) | `FAIL — stale DRL channel pin: 'jutterswet-verzonnen-golden' is pinned in DRL_CHANNEL but no such golden was measured (gone, or renamed) — remove it`; exit 1 |

Alle drie hersteld; de gate draait daarna weer groen.

### Wijziging aan een bestaande check, expliciet gemeld

De twee guards op `EXPECTED_CHANNEL` (dubbele pin, pin zonder reden, kanaal
dat geen kanaal is) zijn van een inline-lus een functie geworden,
`keur_kanaallijst <lijstnaam> <toegestane kanalen> <entry>...`, en worden nu
over béide lijsten aangeroepen. Reden: de DRL-lijst arriveerde met dezelfde
twee gaten, en het enige dat tussen de twee lijsten verschilt is de
verzameling geldige kanalen. Gedrag op `EXPECTED_CHANNEL` ongewijzigd; de
foutteksten noemen nu de lijstnaam erbij. Verder zijn twee bestaande
commentaarblokken bijgewerkt (de kopnotitie en de vloer-notitie) zodat ze de
tweede pin vermelden. Geen enkele bestaande *waarde* is aangeraakt.

**Na de lensronde is die toets aangescherpt.** De eerste versie testte
kanaallidmaatschap als substring over de samengevoegde string
(`case " $toegestaan " in *" $ch "*`), en dat is strikt zwakker dan de
exacte `case` die hij verving: een samengesteld kanaal wordt geaccepteerd.
Nagemeten, los van de gate:

```
OUD (substring): ACCEPTEERT "EXCLUDE DIVERGE"
NIEUW (token): WIJST AF "EXCLUDE DIVERGE"
```

De toets loopt nu per token met stringgelijkheid. Onschadelijk was het wel
— zo'n pin sneuvelt later alsnog op de driftvergelijking — maar een guard
die zwakker is dan zijn voorganger is geen guard. Herbewijs, één
mutatieproef per lijst, beide door de volle gate:

| lijst | proef | uitkomst |
| --- | --- | --- |
| `EXPECTED_CHANNEL` | `jutterswet-seizoenvondst` op `EXCLUDE DIVERGE` | `FAIL — malformed channel pin: … is pinned 'EXCLUDE DIVERGE' in EXPECTED_CHANNEL …`; exit 1 |
| `DRL_CHANNEL` | `jutterswet-vormenkabinet` op `EXCLUDE FAIL` | `FAIL — malformed channel pin: … is pinned 'EXCLUDE FAIL' in DRL_CHANNEL …`; exit 1 |

### De exec-pins staan onbewogen

Uit de slotrun, ná de wijziging:

```
played 159  (…matched the pinned ref≡clingo expectations)
channel floor 50 non-PLAYED goldens pinned by name; played matches the pinned 159
DRL-text branch played 132  (its OWN count — the narrower branch, not the square; pin 132)
DRL-text floor 77 non-PLAYED goldens pinned by name; sweep cost 96s wall clock
JUnit 478 tests, floor 239
```

### Veertien poorten

1 PASS · 2 PASS (238 bestanden) · 3 PASS (238 schoon, 395 afgewezen) ·
4 PASS (568 casusbestanden) · 5 PASS (643 voorbeelden, 539 casussen) ·
6 PASS (419 checks, 539 casussen) · 7 PASS (503 weerleggingen, 36
overlevers) · 8 PASS (29 rood) · 9 PASS (201 checks, 12 goldens) ·
10 PASS (298 tests, 58 goldens) · 11 PASS (29/29/29) · 12 PASS (zie boven) ·
13 PASS (126 verdraaiingen, 22 door de DRL-tak gespeeld) · 14 PASS (130
checks, 2 goldens).

Ná de lens-fixronde opnieuw gedraaid: **1** (de scriptproza veranderde),
**12** (PASS, slotgetallen hierboven, 3:20,7 totaal) en **13** (PASS,
onveranderd — hij deelt de jar en dezelfde vlag van de replay-runner). De
overige elf zijn niet herhaald en dat is een bewuste afweging, geen
vergetelheid: die ronde raakte alleen commentaar en één guard in
`scripts/check-drools.sh` plus twee backlog-bestanden, en geen van die elf
leest een van die bestanden. Aan `backends/drools/` is in de hele kaart
geen regel veranderd.

### Bewust niet

Het objectcreatie-defect van klasse D niet gerepareerd (backend-bron, buiten
de may-touch). De gate-12-beschrijving in `CLAUDE.md` niet aangeraakt — die
is van de maintainer. `spec/CONSTRUCTS.md` en `spec/PLAN.md` niet aangeraakt.
Geen poging de DRL-tak dichter bij 159 te brengen: dit is een teller, geen
takverbreding.

### Vervolgkaart, aangemaakt in deze branch

`backlog/new/20260919-drl-objectcreatie-defect.md` staat er nu als bestand
in plaats van als voorstel in dit Result. Hij draagt de tekst die hier
eerder stond, plus de luide helft uit klasse C, plus de regel die de
lensronde vroeg:

> NB serieel met `20260919-drl-creatie-koppelloos.md` (beide raken
> `DrlTranspiler.objectCreatie`).

Niet samengevoegd met die koppelloos-kaart, en de reden staat in het
bestand: disjuncte codepaden, disjuncte goldens, disjuncte symptomen. De
vier klasse-D-goldens nemen de `rolNaarSubject == null`-tak niet — hun
terugkoppeling lóst op en het misgaan zit bij het vuren; de goldens die die
tak wél nemen staan in klasse C als `EXCLUDE`. De koppelloos-kaart zelf is
ongemoeid gelaten; de hermeet-notitie daar is werk voor de hoofdsessie.
