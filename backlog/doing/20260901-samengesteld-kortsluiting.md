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
     faalt" divergeert zelf niet.
   **En de klassebeschrijving die de eerste fixronde ervoor in de plaats
   zette was óók fout** (gecorrigeerd in fixronde 2): ik las 636 als "het
   aantal gemeten combinaties waarvan er 258 met d < n het eens zijn".
   Het is omgekeerd: **636 is het aantal DIVERGERENDE combinaties**
   (n ≤ 6), en de 258 met **d < n zijn de gevaarlijke**, niet de veilige.
   De klasse is dus niet "d ≥ n" maar **heel TEN_HOOGSTE**; kleinste
   end-to-end tegenvoorbeeld: TEN HOOGSTE 1 over twee leden, lid 1 onwaar,
   lid 2 deelt door nul — ref, clingo en de exec-tak faulten, de
   tekst-tak schrijft 1. De registratie in
   `scripts/check-reftype-derivation.sh` is navenant herbouwd als
   reden-dragende naamlijst over álle TEN_HOOGSTE-dragers in
   `corpus/golden` én `corpus/rood` (vandaag drie), die faalt bij een
   ongelijste drager én bij een stale regel. Getrouwe vorm:
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
(b) de suite-formulering "549 geslaagd + 1 pre-existente omgevings-abort",
met de naam van die ene abortende test erbij: uchar heeft geen top-level
`Makefile`, en de genoemde testnaam bestaat in deze repo niet — dat is
gecontroleerd met een grep op de letterlijke naam, die hier bewust niet
wordt overgeschreven (een zin die een naam spelt om te zeggen dat hij
nergens voorkomt, weerlegt zichzelf).

**Alle dertien gates groen** (fixronde, 2026-09-07): 1 PASS ·
2 183 bestanden · 3 183 golden / 337 negatief · 4 438 cases · 5 415 ·
6 415 · 7 379 (36 survivors, ongewijzigd) · 8 23 rood (Drools-been
6 fault-pinned) · 9 201 checks / 12 catala-goldens · 10 173 tests /
34 rgs-goldens · 11 PASS · 12 played 114 / JUnit 210 / 1 value-divergence
(de bestaande) / vloer 46 · 13 74 twists over 15 goldens, 70 replays,
0 survivors, plus de nieuwe ch.-5.10-registratie. Suites: ref 461
gewerkte voorbeelden (was 456), clingo 80 checks, backend-JUnit 210.
`EXPECTED_PLAYED` 113 → 114.

## Fixronde 2 (2026-09-07) — lens 2

Lens 2 bevestigde fixronde 1 en weerlegde de kop op twee blokkerende en
drie ernstige punten. Alle metingen opnieuw in deze worktree
gereproduceerd.

**BLOKKEREND 1 — de gedurende-clip splitste ref ≢ clingo,
frontend-bereikbaar.** De clip is een EXPRESSIE (een getotaliseerde
`Functie` met filter), dus hij kan als OPERAND in een lid van een ánder
samengesteld staan. ch. 5.10 grens 2 dekt de EIGEN leden van de clip;
S7 rekende de hele clip buiten scope en dat was te grof.

| probe | ref | clingo VOOR | clingo NA |
|-------|-----|-------------|-----------|
| P_clip_in_lid | PASS | UNSAT `viol("arithmetic range exceeded …","J1")` | PASS |
| Q_clip_rule_guard (clip in het resultaat, onware regelconditie) | PASS | UNSAT idem | PASS |
| R_clip_frontend (uit gewone bron) | PASS | UNSAT idem | PASS |
| **jutterswet-klipwacht** (mijn eigen getuige) | PASS | UNSAT `…,"K1"` | PASS |

**TWEE** fences waren fout, niet één. De lens vond de
vergelijkingsfence; de tweede — de segmentfence `|W| > 46340` — kwam pas
boven toen ik het programma van mijn eigen getuige dumpte en de
resterende ongeguarde `viol`-regels las. Na de eerste fix bleef
klipwacht namelijk gewoon divergeren. Beide waren gesleuteld op de
helper-eigen vrije `S` in plaats van op het regelsubject en droegen noch
de `is_a`-basis noch `ctx.rule_guard`; de eerste grondde daardoor over
een kruisproduct van elk clipsubject met elk grenssubject. Beide dragen
nu de basis, de prefix én het juiste subject. ch. 5.10 grens 2 en S7
onderscheiden nu expliciet *de eigen leden van de clip* van *de clip als
operand*.

**BLOKKEREND 2 — de DRL-tekst-scan was te smal en zijn klasse stond op
z'n kop.** Ik had 636 gelezen als "gemeten combinaties waarvan 258 het
eens zijn"; het is **636 DIVERGERENDE** combinaties (n ≤ 6) en de **258
met d < n zijn de gevaarlijke meerderheid**. De klasse is dus heel
`TEN_HOOGSTE`, niet de rand `d ≥ n`. Mijn scan matchte bovendien
`drempel >= len(onderdelen)` (dekte 378/636), globde alleen
`corpus/golden` en zag de twee bestaande `d<n`-dragers structureel niet.
Herbouwd als **reden-dragende naamlijst** (`ATMOST_WACHT`, het
check-noop-survivorpatroon) over elke `TEN_HOOGSTE`-drager in
`corpus/golden` **én** `corpus/rood` — vandaag drie, elk met de reden.
Een golden kán de divergentie zelf niet dragen (de divergerende gevallen
zijn precies die waar de norm FAULT, en dat is geen golden), dus de lijst
is een waakhond over de VORM. Beide faalrichtingen aantoonbaar getest:

```
ongelijste drager : FAIL … carries a TEN HOOGSTE composite that the ch. 5.10 watchlist
                    above does not name → corpus/golden/jutterswet-sluimerwacht/ir.json
stale regel       : FAIL … a stale entry hides the next real one. Drop it:
                    → corpus/golden/verzonnen-drager/ir.json
```

Klassebeschrijving gecorrigeerd in het script, in openpunt 1 hierboven en
in de vervolgkaart.

**ERNSTIG 3 — vijf mutanten hadden nul corpusdekking.** Nieuwe
artefacten: `jutterswet-randwacht` (gelijkheids- én orderingsarm met een
range-gevoelige vergelijking ná een stoppend lid — doodt C2 en C3),
`jutterswet-vouwrand` (de eerste/laatste-vouw; **IR-geauthord**, want de
frontend weigert `eerste van` over numeriek en de fence vuurt alleen bij
gemengde schalen — doodt C4) en een vierde regel `randteken` op
`jutterswet-seinwacht` (PRECIES drempel 3 over 2 leden, de `d = n+1`-rand
— doodt C5c in clingo én D2 in de Drools-exec-tak, en pint daarmee de
"exactly, out of reach"-rij over alle drie executors in plaats van alleen
ref).

**Volledige mutanttabel (18 recepten, alle gedood, baseline leeft):**

| set | recepten | uitkomst |
|-----|----------|----------|
| ref (M1–M6, kill: ref-suite of gate 5) | 6 | **6/6 GEDOOD** (M4 via de ref-suite; zijn corpusartefact is de rode case, gate 8) |
| clingo (C1–C7 incl. C5a/b/c, kill: gate 6) | 9 | **9/9 GEDOOD** |
| Drools exec-model (D1–D3, kill: gate 12) | 3 | **3/3 GEDOOD** |
| baseline | 1 | leeft |

**KLEIN 6 / OBSERVATIE 7.** De zin die een testnaam spelde om te zeggen
dat hij nergens voorkomt is herschreven zonder die naam. Het bronblok in
`corpus/rood/atmost-no-vacuous-true/NOTE.md` is expliciet als illustratie
gemarkeerd; de case zelf is niet aangeraakt.

**Bevroren tot het maintainerbesluit.** Grens 1, de rode case
`corpus/rood/atmost-no-vacuous-true` en de levendheidsmotivering zijn
NIET aangeraakt (uitstaande vraag over het duale paar
`ten hoogste d` ≡ `niet ten minste d+1`). De nieuwe artefacten zijn
besluit-onafhankelijk gekozen: de PRECIES/TEN_MINSTE-getuigen raken de
at-most-vraag niet, en het enige nieuwe TEN_HOOGSTE-materiaal
(`hoogsteteken` op seinwacht) zit in de OVERSCHRIJDINGSrichting, die
onder beide uitkomsten een trigger blijft.

**Alle dertien gates groen** (fixronde 2, 2026-09-07): 1 PASS ·
2 186 bestanden · 3 186 golden / 337 negatief · 4 441 cases · 5 418 ·
6 418 · 7 382 (36 survivors, ongewijzigd) · 8 23 rood · 9 201 checks /
12 catala-goldens · 10 173 tests / 36 rgs-goldens · 11 PASS ·
12 played 116 / JUnit 210 / 1 value-divergence (de bestaande) / vloer 47 ·
13 74 twists, 3 waakhonddragers. Suites: ref 461, clingo 80/0,
backend-JUnit 210. `EXPECTED_PLAYED` 114 → 116; `EXPECTED_CHANNEL` +1:
`jutterswet-klipwacht` speelt NIET in Drools (transpiler SKIP — de clip
als samengesteld LID valt buiten de Phase-B-conditiescope), reden-dragend
gepind; zijn ch. 5.10-inhoud is een clingo-eigenschap die gates 5+6
pinnen.

## Besluitronde (maintainerbesluit 2026-09-07; gemeten 2026-09-09)

**Het besluit** (genummerde vraag 3, bindend): de at-most-WAAR-trigger
wordt OPGENOMEN. Eén regel zonder uitzondering — de evaluatie stopt zodra
de uitkomst vaststaat. `ten hoogste d` stopt dus óók zodra
hits + resterend ≤ drempel. **Grens 1 vervalt**, en met haar de
levendheidsmotivering uit fixronde 1.

**Spec.** ch. 5.10 draagt de nieuwe rij; de motivering is nu de simpele
volledige ("elke rij markeert het moment waarop de uitkomst vaststaat; de
tabel is de uitputtende opsomming van die momenten per kwantor"). De
vacuüm-klasse staat naast de bestaande d > n-tekst: bij **at-most met
d ≥ n** staat de uitkomst al vóór het eerste lid vast en wordt er NIETS
gelezen — dezelfde clausule als ten-minste/precies boven het ledental,
alleen met uitkomst *waar*. S7 volgt met de tweezijdige at-most-guard.

**Executors.** ref: `settled` krijgt de waar-zijde. clingo: de
at-most-prefixguard wordt tweezijdig, `H ≤ d` **én** `H ≥ d − r + 1`; bij
d ≥ n faalt die tweede helft al op de lege prefix, dus emitteert een
vacuüm-ware at-most niets. Drools-exec: `vastgesteld()` idem.

**Artefact-omkering.** `corpus/rood/atmost-no-vacuous-true` pinde precies
het tegendeel en faultte niet meer:

```
ref    : EXPECTED FAULT DID NOT OCCUR: evaluation succeeded
clingo : EXPECTED FAULT DID NOT OCCUR: clingo found a model
```

Een rode case die niet meer fault is geen rode case. Vervangen door
`corpus/golden/jutterswet-vacuumwacht`, met beide interessante punten van
de nieuwe rij: `vacuumteken` (TEN HOOGSTE 2 van 2 — de vacuüm, leest
niets, ook het delende lid niet) en `stopteken` (TEN HOOGSTE 2 van 4 met
leden [onwaar, onwaar, faultend, x] — de waar-stop ONDER het ledental).
Handberekend, in één keer door ref en clingo bevestigd. Rood 23 → 22.

**Een tweede vondst van deze ronde.** De mutant die de clip-VERGELIJKINGS-
fence zijn guard afneemt (`C8`) OVERLEEFDE eerst alle dertien gates:
`jutterswet-klipwacht` raakte alleen de segmentfence. De
vergelijkingsfence vuurt namelijk pas bij VERSCHILLENDE schalen. Daarom
draagt klipwacht nu een tweede regel `randklipteken`, die de temporele
gehele attribuutwaarde tegen een 2-decimalen `drempelbedrag` vergelijkt:
de harmonisatie ×100 verlaagt de bound tot max_int31/100, K1's 30 000 000
gaat eroverheen, en dat alles achter een stoppend lid 1. Daarna sterft
`C8` — en zijn zusje `C9` (segmentfence) ook.

**Volledige mutanttabel (20 recepten, alle gedood, baseline leeft),
gemeten op de eindkop:**

| set | recept | kill-gate | uitkomst |
|-----|--------|-----------|----------|
| ref | baseline | — | **leeft** |
| ref | M1 stopcheck ná i.p.v. vóór | ref-suite + gate 5 | GEDOOD |
| ref | M2 ten-minste-onhaalbaarheid weg | ref-suite + gate 5 | GEDOOD |
| ref | M3 ten-minste-triggers weg | ref-suite + gate 5 | GEDOOD |
| ref | **M4′ at-most-WAAR weg** (de nieuwe rij) | ref-suite + gate 5 | GEDOOD |
| ref | M5 at-most-ONWAAR weg | ref-suite + gate 5 | GEDOOD |
| ref | M6 precies-triggers weg | ref-suite + gate 5 | GEDOOD |
| clingo | C1 i=0-uitzondering terug | gate 6 | GEDOOD |
| clingo | C2 gelijkheidsarm zonder guard | gate 6 | GEDOOD |
| clingo | C3 orderingsarm zonder guard | gate 6 | GEDOOD |
| clingo | C4 eerste/laatste-vouw zonder guard | gate 6 | GEDOOD |
| clingo | C5a ten-minste-guard één te los | gate 6 | GEDOOD |
| clingo | C5c precies-guard één te los | gate 6 | GEDOOD |
| clingo | C6 ALLE-prefix weg | gate 6 | GEDOOD |
| clingo | **C7′ at-most-guard eenzijdig** (de C-variant van M4′) | gate 6 | GEDOOD |
| clingo | C8 clip-vergelijkingsfence zonder guard | gate 6 | GEDOOD |
| clingo | C9 clip-segmentfence zonder guard | gate 6 | GEDOOD |
| Drools-exec | D1 at-most-ONWAAR weg | gate 12 | GEDOOD |
| Drools-exec | D2 precies-onhaalbaarheid weg | gate 12 | GEDOOD |
| Drools-exec | D3 stopcheck ná i.p.v. vóór | gate 12 | GEDOOD |
| Drools-exec | **D4 at-most-WAAR weg** (de D-variant van M4′) | gate 12 | GEDOOD |

**Waakhond-hermeting — de DRL-tekst-divergentie is VERDWENEN.** Het
leesverzamelingsmodel is aangepast aan de nieuwe norm en opnieuw
uitputtend gedraaid (beide richtingen, alle vijf kwantoren, elke drempel
inclusief d > n, elke waarheidstoekenning), **n = 1..7, 6646
combinaties**:

| meting | DRL-tekst | clingo |
|--------|-----------|--------|
| waarde-divergenties t.o.v. ch. 5.10 | **0** | **0** |
| leest een lid dat ch. 5.10 overslaat | **0** | **0** |
| slaat een lid over dat ch. 5.10 leest | **0** | **0** |

De reden is structureel: met de waar-trigger is `ten hoogste d` precies
`niet ten minste d+1`, en dát is wat de tekst-tak berekent — hij
implementeerde de duale al. End-to-end bevestigd op de getuige die vóór
het besluit de smoking gun was (TEN HOOGSTE 1 over één delend lid): ref
`1`, clingo `[1]`, Drools-exec `1`, Drools-DRL-tekst `1` — vier
executors, één antwoord, waar de eerste drie eerder faultten.
`jutterswet-vacuumwacht` speelt in **beide** Drools-takken.

Gevolg: de waakhond-naamlijst in `scripts/check-reftype-derivation.sh` is
**opgeheven** (er is geen vorm meer om te bewaken); de meting staat er
reden-dragend voor in de plaats. De vervolgkaart
`backlog/new/20260907-drl-tekst-kortsluiting.md` **stelt voor zichzelf te
laten vervallen** — met de drie voorbehouden die eerlijkheidshalve tegen
"0 divergenties" pleiten (het model abstraheert de tak; n ≤ 7 is de
gemeten grens; de tak blijft een tweede vertaling) en een smaller
alternatief. Niet zelf verwijderd: dat is een maintainerbesluit.

**Alle dertien gates groen** (besluitronde, gemeten 2026-09-09): 1 PASS ·
2 186 bestanden · 3 186 golden / 337 negatief · 4 441 cases · 5 419 ·
6 419 · 7 383 (36 survivors, ongewijzigd) · 8 **22 rood** · 9 201 checks /
12 catala-goldens · 10 173 tests / **37 rgs-goldens** · 11 PASS ·
12 **played 117** / JUnit 210 / 1 value-divergence (de bestaande) /
vloer 47 · 13 74 twists, 0 survivors. Suites: ref **462**, clingo 80/0,
backend-JUnit 210. `EXPECTED_PLAYED` 116 → 117.

## Slotronde (gemeten 2026-09-09) — eindverificatie: MERGEN, met twee reparaties

**F1 — de te-gretige richting kreeg haar wacht terug.** De
rood→golden-omkering van de besluitronde nam één bewaakte richting mee:
een at-most-stop die **één lid te vroeg** vuurt leest MINDER dan ch. 5.10
en antwoordt dus waar de norm fault. Structureel kan een groene golden
dat niet zien — de leden zijn gewone joins, dus een strengere guard haalt
niets uit de waarheidswaarde weg en raakt alleen het **faultoppervlak**.
Gemeten, en dat is meteen de vóór/ná in één tabel:

| mutant | gate 5 | gate 6 | gate 8 | uitkomst |
|--------|--------|--------|--------|----------|
| C10 at-most-guard één te gretig (`+1` → `+2`) | LEEFT | LEEFT | **DOOD** | GEDOOD |
| C11b guard met een extra `H >= 1` | LEEFT | LEEFT | **DOOD** | GEDOOD |

Beide overleven de groene gates en sterven op gate 8 — precies het bewijs
dat alleen een RODE case deze richting kan pinnen. Nieuwe rode case
`corpus/rood/atmost-reads-until-settled`: ten hoogste 2 van 4 met de
deling op **lid 2**. Na lid 1 is *h* = 0 en *r* = 3, dus 0 + 3 ≤ 2 houdt
niet en lid 2 hóórt gelezen te worden. De deling staat bewust op lid 2:
lid 1 wordt door geen enkele guard weggenomen en zou daar niets pinnen.
Rood 22 → 23.

**F2 — bungelende kruisverwijzing.** Sinds grens 1 verviel heet de
overgebleven grens "One boundary" zonder nummer; `ASP-TRANSLATION` S7 en
de clip-fence-comment in `translate.ml` noemden nog "boundary 2". Beide
vegen naar "ch. 5.10's one boundary".

**F3 — de voorbehouden hebben een vaste drager.** Het
waakhond-vervangende commentaar in `scripts/check-reftype-derivation.sh`
verwijst nu naar déze Results, zodat de voorbehouden het eventuele verval
van de vervolgkaart overleven. Ze staan hier:

Wat "0 divergenties" wél en niet koopt — met wat de eindlens ervan
ontkrachtte:

1. *"Het model abstraheert de tekst-tak."* — **grotendeels ontkracht.**
   De eindlens bouwde een onafhankelijk model, reproduceerde de 6646
   combinaties exact, en het duale argument (`ten hoogste d` ≡ `niet ten
   minste d+1`) is structureel sluitend voor **alle** n via de
   `minstens`/combineer-code, niet alleen voor de gemeten n.
2. *"n ≤ 7 is de gemeten grens."* — **grotendeels ontkracht:** het
   onafhankelijke model loopt schoon door tot n = 10, en punt 1 maakt de
   grens hoe dan ook niet het bindende bezwaar.
3. *"De tekst-tak blijft een tweede, structureel andere vertaling."* —
   **blijft staan.** Dat hij vandaag samenvalt is geen garantie dat een
   volgende ch.-5.10-wijziging hem meeneemt. Dit is het reële restrisico,
   en het smalle alternatief in de vervolgkaart (een eigen
   ch.-5.10-getuige in de backend-JUnit-suite) dekt precies dit af.

**Alle dertien gates groen** (slotronde, gemeten 2026-09-09): 1 PASS ·
2 **187 bestanden** · 3 **187 golden** / 337 negatief · 4 **442 cases** ·
5 419 · 6 419 · 7 383 (36 survivors, ongewijzigd) · 8 **23 rood**
(Drools-been 6 fault-pinned) · 9 201 checks / 12 catala-goldens ·
10 173 tests / 37 rgs-goldens · 11 PASS · 12 played 117 / JUnit 210 /
1 value-divergence (de bestaande) / vloer 47 · 13 74 twists, 0 survivors.
Suites: ref 462, clingo 80/0, backend-JUnit 210.

**Eindstand van de mutantdekking: 22 recepten, alle dood, baseline leeft**
— 6 ref, 12 clingo (incl. C10 en C11b), 4 Drools-exec.
