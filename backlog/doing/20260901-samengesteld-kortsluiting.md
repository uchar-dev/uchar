# Samengesteld ALLE/DE sluit links-naar-rechts kort (§11.1's echte drager)

Goal: Het §11.1-deling-door-nul-voorbeeld van de officiële RegelSpraak-
spec (read-only referentie; §-verwijzingen mogen, excerpten nooit) wordt
niet door een Let-semantiek gered maar door het VOORWAARDENDEEL: de
genormeerde evaluatievolgorde interleavet variabelen en enkele
voorwaarden links-naar-rechts, en een eerdere voorwaarde (`A ongelijk
aan 0`) poort de latere die zou faulten (`B > 0` met `B = b/A`).
Vandaag evalueert `Samengesteld ALLE/DE` (ch. 8.3.2) in ref álle leden
(`List.filter`, eval.ml ~1177) en clingo's integriteitsconstraint vuurt
volgorde-onafhankelijk — gemeten (verkenning 2026-09-01, probe H):
het voorbeeld fault in beide. Deze kaart maakt de kortsluiting
normatief: ALLE stopt bij het eerste onware lid, links-naar-rechts in
wire-volgorde; de leden ná de kortsluiting worden niet geëvalueerd
(geen fault, geen §9.5-kanaalfeit). Gemeten: in ref één regel
(`List.for_all`), 0/399 corpus-verschillen; clingo en Drools vergen
echt ontwerp (de deel-door-nul-constraint moet conjunct-volgorde-bewust
worden resp. de pattern-conjunctie kent geen volgorde-semantiek —
mogelijk een guard-lowering per conjunct-prefix).

Acceptance:
- Rood-eerst: de probe-H-vorm (voorwaarde `ALLE[deler ≠ 0; (Let q =
  100,00 ÷ deler in q) > 0,00]`, deler=0) fault vóór de kaart in ref én
  clingo; ná de kaart: geen fault, regel vuurt niet, doel ongeschreven —
  als getuige-golden met absolute pins (eerst door ref).
- RUNTIME-SEMANTICS ch. 8.3.2 draagt de volgorde-normatieve
  kortsluiting met gewerkt voorbeeld; ASP-TRANSLATION documenteert de
  vertaalvorm; Drools draagt de vorm of weigert reden-dragend per de
  gate-12-discipline.
- Alle twaalf gates groen; geen bestaande pin beweegt (gemeten
  verwacht: 0/399).

May touch: spec/RUNTIME-SEMANTICS.md, spec/IR-SPEC.md (indien WF-tekst),
spec/ASP-TRANSLATION.md, ref/**, backends/clingo/**,
backends/drools/**, corpus/golden/** (nieuwe getuige),
corpus/negatief/** (indien WF), scripts/ (lijsten), backlog/.

Out of scope: de Let-lowering zelf (kaart
20260901-let-lowering-spec-first), OF/GEEN-VAN-kwantoren tenzij de
spec-tekst ze gratis meeneemt, toka/.

## Results (2026-09-06)

**Scope-uitbreiding, maintainerbesluit 2026-09-06 (genummerde vraag 1):**
de kortsluiting geldt voor ALLE VIJF kwantoren onder één regel, niet
alleen ALLE/DE. De kaarttekst hierboven is van vóór dat besluit.

**Rood-eerst, met echte output.** De getuige-vorm (lid 1 toetst de deler
op ongelijk-0, lid 2 deelt erdoor) fault op de ouder-commit in beide
executors — gemeten op de definitieve goldens zelf:

```
ref:    EVALUATION FAULT: division by numeric zero
clingo: UNSATISFIABLE — a fail-fast guard fired:
        viol("division by numeric zero","J1")   resp. …,"W1")
```

In Drools was de eerste gate-12-meting op `jutterswet-wachtdeling`
`ERROR ConstraintEvaluationException` — de telkwantorvouw las álle leden.
Na de kaart: geen fault, de regel vuurt niet, het doel blijft ongeschreven.

**Wat landde.**

* `ref/lib/eval.ml` — de samengestelde arm leest links-naar-rechts en
  stopt op `settled`; de vijf triggers uit het besluit letterlijk.
* `backends/clingo/lib/translate.ml` — prefix-bewust. ALLE/DE: de
  geaccumuleerde prefix-atomen gaan als `rule_guard` mee, zodat elke
  `viol`/`inconsistent`/fence die lid *i* uitzendt de eerdere leden in
  zijn body draagt. Telkwantoren: het ene `#count`-aggregaat is vervangen
  door per-lid-predicaten plus een geïndexeerde prefixketen `cqK_h<i>`,
  omdat een guard die over één gedeeld `cq(S,I)` telt via de negatieve
  schakel een predicaatlus zou sluiten. Getelde waarde identiek.
* `backends/drools/…/ExecModelTranspiler.java` — **de vorm wordt
  gedragen, niet geweigerd.** De conjunctieve tak sluit al kort uit
  zichzelf (alfaketen); de drie plekken die leden tot één lambda vouwen
  (telkwantoren van `voorwaarde`, `leafCompound`, de tellende
  `filterSource`-vouw) krijgen de expliciete stop via één gedeelde
  `vastgesteld()`-triggertabel.
* `spec/RUNTIME-SEMANTICS.md` ch. 5.10 — normatieve triggertabel, het
  stabiliteitsargument, recursie over geneste samengestelden, twee
  vastgelegde grenzen, twee gewerkte voorbeelden met absolute uitkomsten.
* `spec/ASP-TRANSLATION.md` S7 — de prefix-bewuste vertaalvorm met het
  acycliciteitsargument.
* Getuige-goldens `corpus/golden/jutterswet-ploegaandeel` (ALLE) en
  `corpus/golden/jutterswet-wachtdeling` (GEEN + TEN MINSTE 1, één stop,
  tegengestelde uitkomsten). Beide met `bron.rgs` (gate 10 draagt ze) en
  handberekende pins, in één keer door ref bevestigd.
* Tests: `ref/test` 445 → 456 (elf gewerkte voorbeelden, beide ch.-5.10-
  tabellen verbatim plus de fault-controle op het ongepoorte lid);
  backend-JUnit 207 → 210 (`KortsluitendeKwantorWitnessTest`, drie
  getuigen). Geen bestaande check gewijzigd.
* `scripts/check-drools.sh`: `EXPECTED_PLAYED` 111 → 113 (beide nieuwe
  goldens spelen; géén nieuwe vloerregel, géén nieuwe divergentie).

**Geen bestaande pin bewoog.** 0 corpusverschillen in ref én clingo,
precies zoals de kaart voorspelde (de kaart mat 0/399 op 2026-09-01; de
noemer is hier 412 replays over 434 cases vóór de getuigen, 414 over 436
erna).

**Geen nieuwe WF-regel.** Kortsluiting is evaluatiesemantiek: de
ledenvolgorde staat al op de wire (een JSON-array) en er is geen nieuwe
vorm om statisch af te keuren. WF-096 blijft dus vrij.

**Alle dertien gates groen** (bouwronde, 2026-09-06):
1 PASS · 2 181 bestanden · 3 181 golden / 337 negatief · 4 436 cases ·
5 414 · 6 414 · 7 378 (36 survivors, ongewijzigd) · 8 22 rood ·
9 201 checks / 12 catala-goldens · 10 173 tests / 33 rgs-goldens ·
11 PASS (29 cases, 0 divergenties) · 12 played 113 / JUnit 210 /
43 auto-refusals / 1 value-divergence (de bestaande) / vloer 46 ·
13 74 twists over 15 goldens, 70 replays, 0 survivors.

**Bewust NIET gedaan / open voor de lensronde.**

1. **De DRL-tekst-tak** (de slapende tweede transpilertak, alleen door
   gate 13 aangeraakt) is ongewijzigd. **Klassebeschrijving gecorrigeerd
   in de fixronde (2026-09-07)** — de bouwronde had hem twee keer mis:
   * *Fout gezegd:* "luier dan of gelijk aan ch. 5.10 — hij kan een lid
     overslaan dat de norm wél leest". *Werkelijk:* de tak is niet luier
     maar **onjuist waar**. `ten hoogste d` wordt
     `!(minstens(leden, d+1))`, en `minstens` met `k` boven het ledental
     expandeert naar het letterlijke `false` — dus komt er géén enkel lid
     in de DRL en ANTWOORDT de tak waar ref, clingo én de exec-tak
     FAULTEN. Hij past precies de vacuüm-waar-trigger toe die ch. 5.10
     grens 1 uitsluit.
   * *Fout voorbeeld:* "TEN HOOGSTE 1 over drie leden waarvan het eerste
     faalt" divergeert zelf niet. De echte klasse is exact
     **TEN_HOOGSTE met drempel ≥ ledental**, uitputtend gemeten over
     n ≤ 6 (636 combinaties, 258 met d < n — alle eens); de andere vier
     kwantoren divergeren nooit en de gevaarlijke richting is dicht.
   Vandaag corpus-onbereikbaar (0 goldens dragen die vorm); dat feit
   staat nu reden-dragend en falend-wanneer-stale geregistreerd in
   `scripts/check-reftype-derivation.sh`, en de getrouwe vorm is
   `backlog/new/20260907-drl-tekst-kortsluiting.md`.
2. **`at-most`'s waar-richting** (*h* + *r* ≤ *d*) is géén stoptrigger.
   **Motivering herschreven in de fixronde (2026-09-07):** de bouwronde
   gaf als reden "vuurt op bewijs dat niet gelezen is" en "zou vóór lid 1
   stoppen" — allebei onjuist als onderscheid, want ze gelden óók voor de
   WEL opgenomen onhaalbaarheidstriggers van at-least/exactly, die
   inderdaad vóór lid 1 stoppen (zie blokkerend 1). Het echte onderscheid
   is **levendheid**: een at-least-/exactly-drempel boven het ledental is
   een dode voorwaarde, een at-most-drempel die alle leden dekt is gewone
   praktijk. Nu ook gepind in plaats van alleen beweerd:
   `corpus/rood/atmost-no-vacuous-true`.
3. **De ch. 3.6 gedurende-clip** (`eval.ml` ~1029, `translate.ml` ~1967)
   blijft álle leden lezen: hij vouwt periodeverzamelingen, geen
   waarheidswaarde, dus er is geen moment waarop "de uitkomst vaststaat".
   Zo genoteerd in ch. 5.10.
4. **De Let in lid 2.** De kaart vroeg een Let-gebonden deling in lid 2.
   De gelande let-lowering wikkelt de Let-keten uitsluitend om de
   regel*waarde* (gedocumenteerde scope-grens in `RuleChecker`); een
   variabelelezing in een *voorwaarde* wordt geïnlined. De getuige draagt
   de deling daarom rechtstreeks in lid 2 — semantisch hetzelfde
   §11.1-patroon, maar zonder `Let`-knoop. Een getuige die de
   Let-in-lid-2-route zelf pint zou een handgeschreven `ir.json` vergen
   (geen `bron.rgs`, dus buiten gate 10).

## Fixronde (2026-09-07) — de adversariële lens

Twee blokkerende en drie ernstige vondsten, alle vijf verwerkt. Elke
meting hieronder is in deze worktree gereproduceerd, niet overgenomen.

**BLOKKEREND 1 — clingo liet lid 1 van elke telkwantor ongepoort.**
`if i = 0 then []` sloeg de prefix-guard voor lid 1 over. Maar bij
TEN_MINSTE/PRECIES met drempel > ledental vuurt de onhaalbaarheidstrigger
al op de LEGE prefix: ref leest niets, clingo las lid 1 wél.

| probe | vorm | ref (voor = na) | clingo VOOR | clingo NA |
|-------|------|-----------------|-------------|-----------|
| A | TEN MINSTE 5 over 2, deling in lid 1 | PASS | UNSAT `viol("division by numeric zero","W1")` | PASS |
| F | PRECIES 3 over 2 | PASS | UNSAT idem | PASS |
| K | idem, genest | PASS | UNSAT idem | PASS |
| N | idem, uit `bron.rgs` via de frontend | PASS | UNSAT idem | PASS |

De guard wordt nu voor ELKE i gebouwd; `cq*_h0(S,0)` — de eigen basis van
de prefixketen — laat de 0-ledenprefix door dezelfde vergelijking vallen
(H = 0, dus `H >= d − n` houdt precies als d ≤ n). Probe N is de
belangrijkste: de grammatica koppelt het telwoord niet aan het aantal
bullets, dus d > n komt uit gewone bron. Die vorm is nu een golden
(`jutterswet-seinwacht`, regel `minsteteken`).

**BLOKKEREND 2 — niet elke fence droeg de prefix.** Drie
`range_guard`-plekken bouwden hun body zonder `ctx.rule_guard`: de
numerieke gelijk/ongelijk-arm, de orderings-arm en de eerste/laatste-vouw.

| probe | vorm | ref | clingo VOOR | clingo NA |
|-------|------|-----|-------------|-----------|
| H_range_lek | ALLE [onwaar lid; range-gevoelige vergelijking] | PASS | UNSAT `viol("arithmetic range exceeded …","W1")` | PASS |
| I_range_geen | idem onder GEEN | PASS | UNSAT idem | PASS |
| H_range_klein | controle: dezelfde vorm, kleine getallen | PASS | PASS | PASS |

H_range_klein is wat bewijst dat het de fence was en niet de vorm. Dit
raakte dus óók de ALLE/DE-arm, niet alleen de telkwantoren.

**De sweep.** S7 claimde "elke fence draagt de prefix"; die zin is
gezwept en preciezer gemaakt in plaats van geloofd. Van de
emissieplekken die vanuit `cond_atoms` bereikbaar zijn:
*subject-gesleuteld en dragend* (na de fix) — de pre-epoch-fence en zijn
jaar-`viol`, de min/max-vouwen, de eerste/laatste-vouw, de
datumverschil- en datumrekenfences, de afrondingsfences en hun
negatief-operand-`viol`, de deling (rangefence + `inconsistent` + beide
`viol`s), de scalaire rangefence, en de gelijkheids- en orderings-arm;
*element-gesleuteld en bewust niet* — de gesloten-lichaam-familie
(`closed_term` onder `body_branches`), die per element op een `witness`
sleutelt en zijn eigen `guard_body`-keten draagt: een
subject-gesleutelde prefix daarin weven is een scoping-fout, geen fix.
Gemeten: 0 corpusgoldens dragen die vorm binnen een samengesteld lid, en
de enige bronroute die hem bereikt (een aggregatie over een berekende
expressie) wordt door clingo LUID geweigerd (`TRANSLATION FAULT:
expression kind not translated`), dus stille divergentie is er niet;
*buiten scope* — de `gedurende`-clipfence (ch. 3.6, grens 2).

**ERNSTIG 3 — de grens-1-motivering was niet sluitend.** De lens had
gelijk: "vuurt op bewijs dat niet gelezen is" en "zou vóór lid 1 stoppen"
gelden óók voor de WEL opgenomen onhaalbaarheidstriggers. Herschreven op
het echte onderscheid — **levendheid**: een at-least- of exactly-drempel
boven het ledental is een DEGENERATE, dode voorwaarde (hij kan nooit
houden, dus lezen levert alleen faults op namens een regel die niet kan
vuren), terwijl een at-most-drempel die alle leden dekt gewone, zinvol
geschreven praktijk is — daar zou de trigger een levende voorwaarde
structureel in een no-op veranderen. Plus de expliciete d > n-clausule
("er wordt NIETS gelezen"), die blokkerend 1 normatief maakt.

**ERNSTIG 4 — vier triggerrijen hadden nul artefact.** Mutanttabel
(arm-exacte ref-mutanten; kolommen ref-suite / gate 5):

| mutant | VOOR | NA |
|--------|------|-----|
| baseline | leeft | leeft |
| M1 stopcheck ná i.p.v. vóór | **overleeft alles** | DOOD / DOOD |
| M2 onhaalbaarheid weg | DOOD / leeft | DOOD / DOOD |
| M3 alles weg | DOOD / DOOD | DOOD / DOOD |
| M4 at-most-waar toegevoegd | **overleeft alles** | DOOD (ref-suite) + gate 8 |
| M5 at-most-trigger weg | **overleeft alles** | DOOD / DOOD |
| M6 precies-triggers weg | **overleeft alles** | DOOD / DOOD |

Nieuwe artefacten: golden `jutterswet-seinwacht` (TEN HOOGSTE- en
PRECIES-overschrijding mét een faultend lid ná de overschrijding, plus de
d > n-vorm) en rode case `corpus/rood/atmost-no-vacuous-true` (grens 1 in
de enige richting die een groene golden niet kan uitdrukken: de fault
moet er ZIJN). M4's gate-artefact is die rode case — mét M4 toegepast:
`EXPECTED FAULT DID NOT OCCUR: evaluation succeeded` (exit 1); zonder:
`oracle: expected fault observed (division by numeric zero)`. Plus vijf
ref-voorbeelden, één per triggerrij, en een triggerrij-naar-artefact-
tabel in ch. 5.10 zelf.

**ERNSTIG 5 — de DRL-tekst-tak.** Zelf gemeten op een `TEN HOOGSTE 1`
over ÉÉN faultend lid: ref `EVALUATION FAULT: division by numeric zero`,
clingo `UNSATISFIABLE … viol("division by numeric zero","W1")`,
Drools-exec `ERROR ConstraintEvaluationException`, Drools-DRL-tekst
schrijft `1`. Zie de gecorrigeerde openpunt-1 hierboven. Het feit staat
nu reden-dragend en falend-wanneer-stale in
`scripts/check-reftype-derivation.sh` — een scan die de gate laat falen
zodra een golden de vorm draagt — en de vervolgkaart is
`backlog/new/20260907-drl-tekst-kortsluiting.md`.

**KLEIN 6 — harnasrapportage.** `ref/test/test_ref.ml` noemt bij een
onafgevangen fault nu het laatst gerapporteerde voorbeeld plus de
faultreden (M2/M3 stierven eerder met nul rapportregels). Zuiver
additief: een write-only breadcrumb in `check` plus een
`Printexc`-handler; geen bestaande assertie of verwachting bewoog.

**Twee punten die NIET op deze kaart slaan.** Het bericht droeg twee
Result-tekstpunten mee die uit de let-port-fixronde komen en hier geen
betekenis hebben; ik heb ze niet verwerkt in plaats van er getallen bij
te verzinnen. (a) "jouw harnas bereikt 3 van de 15 Let-dragende goldens":
deze repo draagt inderdaad 15 Let-goldens, maar dit werk heeft geen
replay-harnas over Let-dragers — dat is het regelspraak-java-harnas.
(b) "`make test` is 549 geslaagd + 1 pre-existente omgevings-abort
(`corpus_drl_bevat_geen_enkele_function_definitie`)": uchar heeft geen
top-level `Makefile`, en dat testtoken komt in deze repo nergens voor
(gecontroleerd).

**Alle dertien gates groen** (fixronde, 2026-09-07): 1 PASS ·
2 183 bestanden · 3 183 golden / 337 negatief · 4 438 cases · 5 415 ·
6 415 · 7 379 (36 survivors, ongewijzigd) · 8 23 rood (Drools-been
6 fault-pinned) · 9 201 checks / 12 catala-goldens · 10 173 tests /
34 rgs-goldens · 11 PASS · 12 played 114 / JUnit 210 / 1 value-divergence
(de bestaande) / vloer 46 · 13 74 twists over 15 goldens, 70 replays,
0 survivors, plus de nieuwe ch.-5.10-registratie. Suites: ref 461
gewerkte voorbeelden (was 456), clingo 80 checks, backend-JUnit 210.
`EXPECTED_PLAYED` 113 → 114.
