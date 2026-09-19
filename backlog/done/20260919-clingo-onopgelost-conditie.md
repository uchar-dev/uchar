# clingo-onopgelost-conditie: WF-044 ook op de conditiepositie

De ASP-backend citeert WF-044 op de EXPRESSIE-positie (de
`Onopgelost`-arm in `backends/clingo/lib/translate.ml`, ~r. 3486) maar
laat een `Conditie_onopgelost` in de generieke bodem vallen:
`condition kind not translated (no corpus case exercises it)`. `ref`
heeft de arm wél (`ref/lib/eval.ml`: "Onopgelost condition reached in
evaluation (WF-044)"). Daardoor kan de Onopgelost-wacht geen rode
getuige op de guard-positie dragen: een `expectedFault.clingo` zou een
boodschap pinnen die beweert dat geen corpusgeval haar uitlokt, terwijl
de nieuwe case precies dat zou doen (gemeten,
`20260919-exec-stille-guardflanken` §c).

## Acceptatie (bij oppakken aanscherpen)

- (a) `backends/clingo/lib/translate.ml` krijgt een
  `Conditie_onopgelost`-arm die de WF-044-fout werpt zoals de
  expressie-arm dat doet; suite-getuige + mutatieproef.
- (b) Daarna `corpus/rood/wf044-onopgelost-guard/` met
  `expectedFault {ref, clingo}` op de §13.4.7-guard bij
  `doel == anker`, plus de reden-dragende Drools-regel (categorie
  EXCLUDE — beide takken weigeren de vorm per construct sinds
  2026-09-19). Gate 8 gaat 28 → 29, vloer 26 blijft; de
  `check-rood.sh`-regel is een beschermd-padvoorstel met datum + reden.
- (c) Alle veertien gates groen; corpussweep beide Drools-takken: nul
  kanaalwisselingen.

May touch: `backends/clingo/**`, `corpus/rood/**`,
`scripts/check-rood.sh` (voorstel), kaartverplaatsing.
NB: serieel met andere `translate.ml`-kaarten
(kruis-stelsel-eenheid, asp-ch102-vervolg — zelfde bestand).

## Result (2026-09-19, branch `card/clingo-onopgelost-conditie`, van b80ab4ab)

De arm zat er, maar één positie te hoog. `expr_term` weigert een
`Expr_onopgelost` al sinds de wacht bestaat; `cond_atoms` kende de vorm niet
en liet haar in zijn catch-all vallen. Die catch-all zegt *"no corpus case
exercises it"* — een capaciteitsmelding, geen weigering, en daarmee precies de
zin die onwaar wordt op het moment dat een rood corpusgeval haar uitlokt. Dat
is de reden dat de guard-getuige een kaart moest wachten en niet een uur.

### (a) Rood-eerst, gemeten vóór de arm

De §13.4.7-feitcreatie uit `corpus/golden/jutterswet-postwacht` (anker Kustvak,
tussenstap Post, doel Sein), met de creatie-`voorwaarde` op een letterlijke
`Onopgelost` — de vorm die de resolver aflevert als doel en anker hetzelfde
objecttype zijn: `RuleChecker.doelVoorwaarde` geeft bij `doel == anker` de
onopgeloste ankerguard onveranderd terug, want er is geen tweede subject om hem
tegen te herbouwen.

| uitvoerder | vóór de arm |
| --- | --- |
| verifier | `verifier: PASS` (exit 0) — een eerlijke marker is legale wire |
| ref | `EVALUATION FAULT: Onopgelost condition reached in evaluation (WF-044)` |
| clingo | `TRANSLATION FAULT: condition kind not translated (no corpus case exercises it)` |
| Drools exec | `EXCLUDE transpiler SKIP: feitcreatie: vakbeseining door het kustvak — feitcreatie (voorwaarde niet naar anker of doel te lowereren: het sein het sein is): vakbeseining door het kustvak` |
| Drools DRL-tekst | `EXCLUDE drl-transpiler MARKER: feitcreatie: vakbeseining door het kustvak — TODO voorwaarde niet naar anker of doel te lowereren — regel inert i.p.v. over-creatie` |

Drie van de vier noemen de vorm; clingo noemde de leemte.

### (b) De arm

`cond_atoms` krijgt een `Conditie_onopgelost`-arm direct vóór de catch-all, op
dezelfde plaats als de expressie-arm vóór de zijne. De zin is samengesteld uit
de twee die er al waren — `ref` levert de **positie** (`Onopgelost condition
reached in …`), de expressie-arm de **fase** (`… in translation: this rule
fails observably (WF-044)`):

```
TRANSLATION FAULT: Onopgelost condition reached in translation: this rule fails observably (WF-044)
```

Zo noemen de twee uitvoerders dezelfde plek in dezelfde woorden, en blijft het
fragment dat een rode case pint onderscheidbaar van de waardepositie
(`Onopgelost reached in translation`, zonder `condition`).

**Getuige.** `backends/clingo/test/test_clingo.ml` krijgt één check:
`WF-044 een Onopgeloste regelvoorwaarde wordt geciteerd, niet weggeslikt`,
via de bestaande `faults_with`-helper. Hij pint bewust de **andere**
conditiepositie — de voorwaarde van een gewone rekenregel — die geen
corpusdocument draagt: een golden zou erop falen, en de rode zone houdt één
getuige per vorm. Beide posities lopen door dezelfde arm. Suite: 419 checks,
0 failures (418 + deze).

**Mutatieproef, werkelijk uitgevoerd.** De arm uit `translate.ml` geknipt (741
bytes), opnieuw gebouwd, suite gedraaid:

```
FAIL WF-044 een Onopgeloste regelvoorwaarde wordt geciteerd, niet weggeslikt faults loudly (condition kind not translated (no corpus case exercises it))
test_clingo: 419 checks, 1 FAILURES
```

De getuige valt exact op de generieke bodemzin terug. Dezelfde mutatie op de
rode case:

```
EXPECTED FAULT MISMATCH: wanted "Onopgelost condition reached in translation" in:
TRANSLATION FAULT: condition kind not translated (no corpus case exercises it)
```

Hersteld, opnieuw gedraaid: `test_clingo: 419 checks, 0 failures`, rode case
`clingo: expected fault observed`. Werkboom daarna schoon.

### (c) De rode getuige

`corpus/rood/wf044-onopgelost-guard/` — `ir.json`, `case1.json`, `NOTE.md`.

Het document is `corpus/golden/jutterswet-postwacht/ir.json` met **precies één
veld gedraaid**: `feitCreaties[0].voorwaarde` van `null` naar de marker. De
case is die golden z'n eigen `case.json` met `expected` vervangen door
`expectedFault`; de populatie is ongewijzigd. Alles wat een groene run nodig
heeft is dus aanwezig en juist, en het enige wat de case rood maakt is de
marker zelf — de sterkst mogelijke isolatie voor een rood hek. Verifier PASS,
en het bestand valideert + rondloopt byte-identiek tegen gate 2's serialiser
(expliciete modus, apart gedraaid).

Gepinde fragmenten, geknipt uit de werkelijke uitvoer ná de arm:

```
ref    : "Onopgelost condition reached in evaluation"
clingo : "Onopgelost condition reached in translation"
```

Beide runners melden `expected fault observed` (exit 0). Geen
`expectedFault.drools`: beide Drools-takken weigeren de vorm per construct (zie
de tabel bij (a)), en dat is in de woordenschat van die leg een EXCLUDE, geen
fault-at-fire.

**Beschermd-padvoorstel — NIET geland.** Gate 8's uitsluitingslijst staat in
`scripts/check-rood.sh`, en de leg eist per rode case óf een gepind
Drools-fragment óf een reden-dragende regel. Zonder regel faalt de poort, met
precies deze zin (gemeten op deze branch):

```
FAIL: wf044-onopgelost-guard carries no drools fragment and is not in the Drools exclusion list — pin a fragment or list it with a reason:
EXCLUDE transpiler SKIP: feitcreatie: vakbeseining door het kustvak — feitcreatie (voorwaarde niet naar anker of doel te lowereren: het sein het sein is): vakbeseining door het kustvak
```

De ref- en clingo-legs zijn daar al groen op 29. Voorgestelde regel, in te
voegen tussen `vintage-uncovered-date` en `wf044-onopgelost-reached`:

```
wf044-onopgelost-guard|EXCLUDE measured 2026-09-19: BOTH transpiler branches refuse the §13.4.7 creation by construct — a guard that lowers to neither the anker's nor the doel's pattern cannot be dropped without firing the rule unconditionally and over-creating the relation (exec: "transpiler SKIP: feitcreatie … voorwaarde niet naar anker of doel te lowereren"; the DRL-text branch prints the same limit as a MARKER). The backend AGREES that the document is unreadable, one phase before fire; this leg's vocabulary reserves FAULT for a genuine fault at fire, so the agreement is listed here instead of fragment-pinned — exactly as its value-position sibling below is
```

Met die ene regel tijdelijk in de werkboom toegepast en gate 8 opnieuw
gedraaid:

```
check-rood: Drools leg PASS (6 fault-pinned, 23 not-carried with reason)
check-rood: PASS (29 red cases fault observably in ref and clingo, floor 26)
exit=0
```

Daarna teruggedraaid (`git checkout -- scripts/check-rood.sh`); `git status` is
schoon en geen beschermd pad staat op deze branch. **Geen tellingsaanpassing
nodig:** `EXPECTED_MIN_ROOD_CASES` is 26 en blijft 26 — een ondergrens, en 29
ligt erboven. De lijstregel is de enige vereiste wijziging.

### (d) Corpussweep en poorten

Sweep over alle **209** goldens in **beide** Drools-takken (één regel per
golden per tak, status én reden, 418 regels), gedraaid op de kaartclaim
(`8b2f9707`, Drools-gelijk aan `main`) en opnieuw op de tip: de twee rapporten
zijn **regel voor regel identiek** (`diff` leeg). exec PLAYED 159 → 159,
DRL-tekst PLAYED 130 → 130, nul kanaalwisselingen. Dat was de verwachting en
niet meer dan dat: deze kaart raakt geen bestand dat een Drools-tak leest, en
een rode case is geen golden.

Veertien poorten los gedraaid (`./scripts/check-<naam>.sh`):

```
gate  1  check-self-contained       exit=0   check-self-contained: PASS
gate  2  check-ir-schema            exit=0   schema valid; 24 pattern self-test rows; 238 files validate and round-trip, floor 192
gate  3  check-verifier             exit=0   238 golden clean; 395 negatief rejected for their declared rule, floor 350
gate  4  check-case-schema          exit=0   schema valid; 568 case files validate
gate  5  check-oracle               exit=0   ref suite 643 worked examples, floor 489; 539 cases replayed exactly, floor 427
gate  6  check-clingo               exit=0   backend suite 419 checks, floor 83; 539 cases replay exactly through clingo
gate  7  check-noop                 exit=0   503 cases refute a do-nothing executor; 36 pinned survivors
gate  8  check-rood                 exit=1   ref/clingo groen op 29; Drools-leg faalt op de ONTBREKENDE beschermde lijstregel (zie (c)); mét de voorgestelde regel exit=0
gate  9  check-catala               exit=0   frontend suite 201 checks, floor 201; 12 catala goldens regenerate byte-identically
gate 10  check-regelspraak          exit=0   JUnit suite 298 tests, floor 173; 58 rgs goldens re-emit byte-identically
gate 11  check-toka-parity          exit=0   check-toka-parity: PASS
gate 12  check-drools               exit=0   played 159, auto-excluded 47, value-divergence 1, no-case 2; channel floor 50; JUnit 471 tests, floor 239
gate 13  check-reftype-derivation   exit=0   126 annotations twisted over 23 goldens, floor 74; 104 executor replays reproduce, 22 PLAYED door de DRL-tak
gate 14  check-flogic               exit=0   frontend suite 130 checks, floor 81; 2 flogic goldens re-emit byte-identically
```

Acceptatiepunt (c) is dus **niet** helemaal gehaald, en dat is structureel:
zolang de beschermde lijstregel niet is geland kán gate 8 op deze branch niet
groen zijn. Dertien poorten groen, gate 8 groen op alles behalve de regel die
de maintainer moet landen.

### May touch — werkelijk aangeraakt

- `backends/clingo/lib/translate.ml` (de arm, 10 regels)
- `backends/clingo/test/test_clingo.ml` (de getuige)
- `corpus/rood/wf044-onopgelost-guard/` (nieuw: `ir.json`, `case1.json`,
  `NOTE.md`)
- deze kaart (`new/` → `doing/` → `done/`)

Níet aangeraakt: `scripts/check-rood.sh` (het voorstel hierboven staat alleen
in deze tekst), `backends/drools/**`, frontends, `ref/`, `spec/`. **Geen
registervoorstel** voor `spec/CONSTRUCTS.md` of `spec/PLAN.md`: er is geen
wire-, verifier- of spec-delta — een backend die luid wordt waar hij een
leemte meldde, en een rode case die een bestaande WF-regel op een tweede
positie vasthoudt.

### Backlog-nawerk voor de hoofdsessie (voorstel, niet geland)

`backlog/done/20260919-exec-stille-guardflanken.md` beschrijft de blokkade op
twee plekken die nu achterhaald zijn. Voorgestelde bijwerking:

- **§c**, na de slotalinea ("Gate 8 blijft dus op 28 rode cases …"), een regel:
  *"Opgeheven 2026-09-19 door `clingo-onopgelost-conditie`: clingo citeert
  WF-044 nu ook op de conditiepositie, en `corpus/rood/wf044-onopgelost-guard`
  draagt de guard-positie (gate 8 op 29). De `check-rood.sh`-lijstregel die
  daarbij hoort is een beschermd-padvoorstel in die kaart."*
- **Open twijfel, tweede bullet**, de zin "De rode zone kan vorm 1 pas dragen
  nadat clingo zijn WF-044-arm heeft (zie (c))" vervangen door: *"Vorm 1 is
  sinds 2026-09-19 rood-gedekt (`corpus/rood/wf044-onopgelost-guard`); vorm 2
  wacht nog op de normatieve grond van (c)'s slotalinea."*

Dat is andermans done-kaart, dus bookkeeping van de hoofdsessie, niet van deze
sessie.

### Open twijfel

- De arm faalt **statisch**, bij vertaling, waar `ref` **dynamisch** faalt, bij
  evaluatie. Voor deze case valt dat samen — de creatie wordt altijd vertaald
  en haar regel wordt altijd bereikt. Maar een `Onopgelost`-conditie onder een
  geldigheidsvenster dat nooit opengaat, of op een objecttype zonder instanties,
  zou in `ref` zwijgen en in clingo hard vallen. Dat verschil bestond al voor de
  expressie-arm en is met deze arm alleen verbreed; ik heb er geen getuige voor
  gebouwd, want welke van de twee gelijk heeft is een normvraag (mag een
  onbereikbare marker zwijgen?) en die stond niet op deze kaart.
- De rode zone draagt nu twee WF-044-getuigen die in hun clingo-fragment op één
  woord verschillen (`Onopgelost reached …` versus `Onopgelost condition
  reached …`). Dat is genoeg: geen van beide fragmenten is een deelreeks van de
  andere zin, dus de `contains`-toetsen kruisen elkaar niet. Maar het is krap,
  en een derde positie zou de fragmenten uit elkaar moeten trekken in plaats
  van er een woord bij te schrijven.
- De suite-getuige pint de regel-voorwaardepositie, het corpus de
  creatieguard. Beide lopen door dezelfde arm, dus de dekking is echt, maar
  geen van beide getuigt dat de arm de **enige** route is: als een derde
  conditiepositie ooit haar eigen `match` krijgt, valt die weer stil in een
  bodem, en niets in deze kaart merkt dat op.
