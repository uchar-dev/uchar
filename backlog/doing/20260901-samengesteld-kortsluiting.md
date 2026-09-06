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

**Alle dertien gates groen** (2026-09-06, op de kaartkop):
1 PASS · 2 181 bestanden · 3 181 golden / 337 negatief · 4 436 cases ·
5 414 · 6 414 · 7 378 (36 survivors, ongewijzigd) · 8 22 rood ·
9 201 checks / 12 catala-goldens · 10 173 tests / 33 rgs-goldens ·
11 PASS (29 cases, 0 divergenties) · 12 played 113 / JUnit 210 /
43 auto-refusals / 1 value-divergence (de bestaande) / vloer 46 ·
13 74 twists over 15 goldens, 70 replays, 0 survivors.

**Bewust NIET gedaan / open voor de lensronde.**

1. **De DRL-tekst-tak** (de slapende tweede transpilertak, alleen door
   gate 13 aangeraakt) is ongewijzigd. Zijn telkwantoren zijn een
   tekstuele k-deelverzameling-OR-vouw; Java's `||`/`&&` maken die
   *luier dan of gelijk aan* ch. 5.10 — hij kan een lid overslaan dat de
   normatieve volgorde wél leest (TEN HOOGSTE 1 over drie leden waarvan
   het eerste faalt), nooit andersom. Een getrouwe vorm vergt een
   runtime-helper met suppliers in plaats van een tekstexpansie: een
   eigen kaart. Corpusbreed meet gate 13 vandaag geen verschil.
2. **`at-most`'s waar-richting** (*h* + *r* ≤ *d*) is géén stoptrigger —
   het besluit noemt hem niet, en met *d* ≥ *n* zou hij vóór het eerste
   lid stoppen. Vastgelegd in ch. 5.10 als grens, niet stilzwijgend.
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
