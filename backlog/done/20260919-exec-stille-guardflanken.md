# exec-stille-guardflanken: twee gemeten stille routes in de servicetak

Uit de gastheer/triage-meetsessie (2026-09-19), besluitvrij te dichten
— beide zijn "stil waar de zustertak luid is" en raken geen normvraag:

1. **De Onopgelost-feitcreatie-guard verdwijnt in exec.** De frontend
   levert bij `doel == anker` een feitcreatie met een letterlijke
   `Onopgelost`-guard af; verifier PASS, ref WF-044 FAULT, clingo
   FAULT, drl EXCLUDE — en exec speelt STIL 0. Daarmee laat de exec-tak
   een `Onopgelost` verdwijnen, precies wat het gate-8-principe
   bewaakt; de familie heeft op de guard-positie geen getuige.
2. **De derde-type-eigenaar maakt exec inert zonder skip.** Een
   kenmerkcheck-eigenaar die anker noch doel is, is verifier-schoon;
   drl EXCLUDEt met reden, exec emitteert geen regel, registreert geen
   skip en telt als gespeeld met waarde 0 — een taksplijting die
   gate 12 (exec-only) niet kan zien.

Beide fixes: exec weigert per construct met reden (het drl-patroon),
nooit inert. NIET de gastheervraag beantwoorden — de guard-semantiek
blijft zoals hij is; alleen stil → luid.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: beide vormen met de huidige stille exec-uitkomst
  (PLAYED 0 / inert-lowered) letterlijk vastgelegd.
- (b) Exec weigert beide per construct met reden; dual-branchgetuigen
  pinnen dat beide takken dezelfde weigering dragen; mutatieproef.
- (c) Overweeg een rood-getuige voor de Onopgelost-guard-familie
  (expectedFault ref/clingo + reden-dragende Drools-regel — beschermd
  scriptvoorstel).
- (d) Corpussweep beide takken: nul kanaalwisselingen (geen corpusdoc
  draagt de vormen); alle veertien gates groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`,
`backends/drools/src/test/**`, evt. `corpus/rood/**` +
`scripts/check-rood.sh`-regel (voorstel), kaartverplaatsing.
NB: serieel ná de exec-bladvertaling-kaart (zelfde bestand).

## Result (2026-09-19, branch `card/exec-stille-guardflanken`, van 4a1e524e)

**Eén plek was de bron van beide stiltes.** `ExecModelTranspiler.feitCreatie`
probeert de voorwaarde eerst op het anker, dan op het doel, en zette bij
"geen van beide" de vlag `inert`, waarna hij geen regel emitteerde én niets
registreerde. `meet()` leest een emitter die niets aan de skiplijst toevoegde
als LOWERED, dus het document speelde gewoon mee — met de creatietelling op
nul. Beide gemelde vormen komen dáár uit, en daarom is er één fix.

### (a) Rood-eerst, gemeten op de pre-kaart-jar

Beide vormen zijn als eigen verifier-schone invoeren herbouwd uit
`corpus/golden/jutterswet-postwacht` (anker Kustvak, tussenstap Post, doel
Sein), teruggebracht tot de creatie plus de regel die telt wát de creatie
maakt, zodat de uitkomst één getal op het anker is. De populatie: V1 met P1
met S1 en S2 — de creatie zou `vakseinaantal` op 2 zetten.

| vorm | verifier | ref | clingo | exec | drl-tekst |
| --- | --- | --- | --- | --- | --- |
| `Onopgelost`-guard | PASS | `EVALUATION FAULT: Onopgelost condition reached in evaluation (WF-044)` | `TRANSLATION FAULT: condition kind not translated (…)` | **PLAYED 1**, `vakseinaantal` 0 | `EXCLUDE drl-transpiler MARKER: feitcreatie: … — TODO voorwaarde niet naar anker of doel te lowereren — regel inert i.p.v. over-creatie` |
| kenmerkcheck `eigenaar: "Post"` (anker noch doel) | PASS | `EVALUATION FAULT: characteristic check "risicopost" is hosted on "Post" while the subject is "Kustvak" … (WF-100)` | `TRANSLATION FAULT` (dezelfde zin) | **PLAYED 1**, `vakseinaantal` 0 | dezelfde MARKER-regel |

De nul is een wáárde en geen token: een proefgeval dat 2 pint (wat de creatie
zou maken) faalt zichtbaar in beide vormen — `FAIL … attribuut vakseinaantal:
verwacht 2, werkelijk 0`. En de frontend-route van vorm 1 is bevestigd in de
bron: `RuleChecker.doelVoorwaarde` geeft bij `doel == anker` de anker-guard
onveranderd terug, en die is daar juist de `Onopgelost`.

### (b) De weigering, en wat ze niet raakt

`feitCreatie` weigert de inerte tak nu per construct, met de zin die de
DRL-tak al drukte plus de brontekst van de guard, zodat het poortrapport de
twee vormen uit elkaar houdt:

```
EXCLUDE transpiler SKIP: feitcreatie: vakbeseining door het kustvak
  — feitcreatie (voorwaarde niet naar anker of doel te lowereren: het sein het sein is): vakbeseining door het kustvak
EXCLUDE transpiler SKIP: feitcreatie: vakbeseining door het kustvak
  — feitcreatie (voorwaarde niet naar anker of doel te lowereren: de post risicopost is): vakbeseining door het kustvak
```

Daarmee is "geen regel en geen reden" in dit bestand niet meer bereikbaar:
van de zeven emitters die `meet()` aanroept was de inerte feitcreatie de enige
met een kale `return null`, geteld vóór en ná. De javadoc van `meet()` noemde
die tak als hét voorbeeld van een correcte, niet-registrerende lowering; die
zin is nu de omgekeerde invariant.

Getuige: `StilleGuardflankWitnessTest` (4 checks), dual-branch via
`BeideTakken`. Twee pinnen dat **beide** takken dezelfde weigering dragen met
hetzelfde fragment; twee flanken pinnen dat de guard-semantiek onaangeroerd
is en dat de weigering niet over-vuurt:

- doel-gehoste guard (`eigenaar` = het doel): beide takken PLAYED, `vakseinaantal`
  **1** — de doel-lezing, niet 2 (anker) en niet 0 (guard weggevallen); proeven op
  0 en op 2 falen zichtbaar in beide takken. ref/clingo blijven hier FAULT (WF-100):
  dat is de open gastheervraag van `20260910-eigenaar-gastheer-object.md`, en die
  is niet aangeraakt.
- anker-gehoste guard (`eigenaar` = het anker): verifier PASS, ref PASS (2
  expectations), clingo PASS (2), beide takken PLAYED met 2; proeven op 0 en 1 falen.

**Mutatieproef, feitelijk uitgevoerd.** De weigering weer terug naar
`if (inert) return null;`, suite gedraaid: `Tests run: 4, Failures: 2` — precies
de twee weigeringsgetuigen, en precies op de stilte
(`Expected CaseRunner.Excluded to be thrown, but nothing was thrown`); de twee
flanken bleven groen. Hersteld, opnieuw gedraaid: `Tests run: 4, Failures: 0`.
De werkboom is daarna schoon (`git diff` leeg).

### (c) De rood-getuige: NIET gebouwd, met de meting erbij

Het kaartpunt nam aan dat de guard-positie de klassieke vorm heeft — een
WF-044-evaluatie-FAULT in ref én clingo. Gemeten klopt de helft: `ref` heeft
een eigen arm (`Conditie_onopgelost` → "Onopgelost condition reached in
evaluation (WF-044)"), **clingo niet**. Daar valt de conditie in de generieke
bodem `condition kind not translated (no corpus case exercises it)` — de zin
die de conditie-`match` als laatste alternatief draagt.

Dat maakt de getuige vandaag onbouwbaar zonder iets te breken:

1. het `expectedFault.clingo`-fragment zou een bericht pinnen dat letterlijk
   beweert dat géén corpusgeval het uitlokt — terwijl de nieuwe corpuscase
   precies dat doet. Een artefact dat zijn eigen boodschap onwaar maakt hoort
   hier niet in de rode zone;
2. het zou aan clingo-kant een *capaciteitsgat* pinnen in plaats van de
   onopgelost-wacht. De rode zone is de staande regressiehek voor rode paden;
   dit been zou over de afwezigheid van een implementatie gaan.

De eerlijke volgorde is dus andersom, en dat is een eigen kaartje: geef
`backends/clingo/lib/translate.ml` een `Conditie_onopgelost`-arm die de WF-044-fout
werpt zoals zijn eigen EXPRESSIE-arm dat al doet (regel ~3484, "Onopgelost reached
in translation … (WF-044)") en zoals `ref/lib/eval.ml` dat doet, en bouw dán de
rood-getuige op de guard-positie. `backends/clingo` valt buiten de May-touch van
deze kaart, en het is bovendien de derde uitvoerder: dat verplaatsen naar hier zou
het disjunctheidscontract breken voor winst die één kaart later net zo groot is.

Voorstel voor dat kaartje, kant-en-klaar over te nemen:

> **`clingo-onopgelost-conditie`** — Goal: de ASP-backend citeert WF-044 ook op de
> CONDITIE-positie, zoals hij dat op de expressie-positie al doet, zodat de
> Onopgelost-wacht een rode getuige op de guard-positie kan dragen. Acceptance:
> `backends/clingo/lib/translate.ml` krijgt een `Conditie_onopgelost`-arm met de
> WF-044-fout (gemeten vóór: `condition kind not translated (no corpus case
> exercises it)`); daarna `corpus/rood/wf044-onopgelost-guard/` met
> `expectedFault {ref, clingo}` op de §13.4.7-guard bij `doel == anker`, plus de
> reden-dragende Drools-regel (categorie EXCLUDE — beide takken weigeren de vorm
> per construct sinds 2026-09-19). Gate 8 gaat 28 → 29, vloer 26 blijft; de
> `check-rood.sh`-regel is een beschermd-padvoorstel met datum + reden. May touch:
> `backends/clingo/**`, `corpus/rood/**`, `scripts/check-rood.sh` (voorstel).

Gate 8 blijft dus op 28 rode cases (vloer 26) en `scripts/check-rood.sh` is niet
aangeraakt — er is geen beschermd-padvoorstel op deze branch. *Opgeheven
2026-09-19 door `clingo-onopgelost-conditie`: clingo citeert WF-044 nu ook op
de conditiepositie, en `corpus/rood/wf044-onopgelost-guard` draagt de
guard-positie (gate 8 op 29); de bijbehorende `check-rood.sh`-lijstregel is
bij die merge geland.* Wat de familie
intussen wél heeft: `corpus/rood/wf044-onopgelost-reached` dekt de
EXPRESSIE-positie, en de guard-positie is vanaf nu in beide Drools-takken luid.

Voor vorm 2 is bewust géén rode getuige gemaakt: een `expectedFault` op de
WF-100-hosting zou de openstaande gastheervraag normatief beantwoorden, en die
staat expliciet buiten deze kaart.

### (d) Corpussweep en poorten

Sweep vóór en ná, over **alle 202** goldens in **beide** takken (één regel per
golden, status én reden): de twee rapporten zijn **regel voor regel identiek**.
exec PLAYED 153 → 153, DRL-tekst PLAYED 126 → 126, nul kanaalwisselingen, nul
gewijzigde redenen. Dat was de verwachting: geen corpusdocument draagt een guard
die naar anker noch doel lowert. `EXPECTED_PLAYED` en `EXPECTED_CHANNEL` in gate 12
hoefden daarom niet te bewegen, en gate 12 rapporteert zelf `played 153 … channel
floor 49 non-PLAYED goldens pinned by name; played matches the pinned 153`.

Alle veertien poorten groen (exitcode 0; 3–6 zijn in de repo `100644`, dus met `sh`
aangeroepen):

```
gate  1  check-self-contained       exit=0   check-self-contained: PASS
gate  2  check-ir-schema            exit=0   schema valid; 24 pattern self-test rows; 230 files validate and round-trip, floor 192
gate  3  check-verifier             exit=0   230 golden clean; 395 negatief rejected for their declared rule, floor 350
gate  4  check-case-schema          exit=0   schema valid; 554 case files validate
gate  5  check-oracle               exit=0   ref suite 643 worked examples, floor 489; 526 cases replayed exactly, floor 427
gate  6  check-clingo               exit=0   backend suite 362 checks, floor 83; 526 cases replay exactly through clingo
gate  7  check-noop                 exit=0   490 cases refute a do-nothing executor; 36 pinned survivors
gate  8  check-rood                 exit=0   28 red cases fault observably in ref and clingo, floor 26; Drools leg 6 fault-pinned, 22 not-carried
gate  9  check-catala               exit=0   frontend suite 201 checks, floor 201; 12 catala goldens regenerate byte-identically
gate 10  check-regelspraak          exit=0   JUnit suite 283 tests, floor 173; 54 rgs goldens re-emit byte-identically
gate 11  check-toka-parity          exit=0   check-toka-parity: PASS
gate 12  check-drools               exit=0   played 153, auto-excluded 46, value-divergence 1; suite groen
gate 13  check-reftype-derivation   exit=0   108 annotations twisted over 22 goldens, floor 74; 100 executor replays reproduce
gate 14  check-flogic               exit=0   frontend suite 130 checks, floor 81; 2 flogic goldens re-emit byte-identically
```

### May touch — werkelijk aangeraakt

- `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`
  (de inerte tak + twee javadoc-zinnen die hem beschreven)
- `backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/StilleGuardflankWitnessTest.java` (nieuw)
- deze kaart (`new/` → `doing/` → `done/`)

Níet aangeraakt: `corpus/rood/**`, `scripts/check-rood.sh` (zie (c)), en elk
beschermd pad. **Geen registervoorstel**: geen CONSTRUCTS-delta (de wire, de
verifier en de spec zijn ongemoeid — dit is een backend die luid wordt waar hij
stil was) en geen PLAN-scopewijziging.

### Open twijfel

- De exec-reden draagt de brontekst van de guard, de DRL-reden niet. Ze delen
  het fragment dat de getuige pint, dus de takken spreken elkaar niet tegen,
  maar het rapport is aan exec-zijde iets rijker. Dat is bewust: zonder die
  tekst zijn de twee vormen in het poortrapport niet te onderscheiden.
  `DrlTranspiler` viel buiten de May-touch, dus de symmetrie is niet afgemaakt.
- De geweigerde documenten hebben nergens een *corpus*-huis: ref en clingo
  werpen erop een fout, dus een golden kan de vorm niet dragen. Vorm 1 is
  sinds 2026-09-19 rood-gedekt (`corpus/rood/wf044-onopgelost-guard`); vorm 2
  is vandaag wél rood-bereikbaar — clingo werpt daar een echte WF-100-zin, de
  Drools-token is EXCLUDE — en is alleen op de normatieve grond van (c)'s
  slotalinea uitgesteld. Tot die tijd leeft de dekking in de eigen suite van
  de backend.
