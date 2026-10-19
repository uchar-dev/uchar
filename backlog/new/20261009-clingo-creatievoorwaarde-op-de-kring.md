# clingo-creatievoorwaarde-op-de-kring: clingo wijkt stil af op een creatievoorwaarde die leest wat haar kring nog verandert

*Vastgelegd op 2026-10-09 door de hoofdsessie, na het besluit van de eigenaar
over de generaties (kaart `20261009-creatie-initialisatie-leest-groeiende-waarde`),
volgens de afspraak dat een bestaande bug een kaart krijgt. Gemeten door de
bouwer van die kaart met ref, clingo en de verifier uit `_build` van haar
worktree; de documenten maakt `/tmp/lens-bugs/w1/restposten.py`, de uitkomsten
staan in `/tmp/lens-bugs/w1/restposten-meting-fix2.txt` (ref vóór en na de
generaties gelijk, alle 17 rijen).*

## Aanleiding

Sinds 2026-10-09 leest een creatie haar voorwaarde en haar initialisaties op
de uitgerekende wereld van de vorige generatie (`spec/RUNTIME-SEMANTICS.md`
ch. 5.5, Generations). Daarmee is de lezing van ref de norm voor twee restposten
van ch. 6.1: "conditions that hold on absence" en "a value that grows". Clingo
heeft geen moment van maken: zijn creatievoorwaarde is een regel over het
stabiele model. Voor een initialisatie weigert het sinds die kaart luid en bij
naam; voor een voorwaarde geeft het stil een andere uitkomst, of geen model
zonder benoemde weigering.

| Vorm (verifier `PASS`) | ref | clingo |
|---|---|---|
| model van `jutterswet-opvolgerquorum`, case 4, met de `alle`-matrix "merk kleiner dan 2" of "merk ongelijk aan 2" | merk(W1) = 2 | `MISMATCH: W1.merk = [1], expected 2` |
| `ten minste 1` met "merk kleiner dan 2", W3 met een ploeglid | merk(W1) = 2 | `MISMATCH: W1.merk = [1], expected 2` |
| zonder vouw: "het merk van de ploegleider van de Wachter is ongelijk aan 2" | merk(W3) = 2 | `MISMATCH: W3.merk = [1], expected 2` |
| twee vormen: "het merk is ongelijk aan 1" in de creatie en een regel die merk = 1 schrijft als "de rang van de opvolger is ongelijk aan 2" | merk(W1) = 1 | twee stabiele modellen; welk de runner kiest, ligt niet vast (op 2026-09-23 een met merk(W1) leeg, op 2026-10-09 dat van ref) |
| zes voorwaarden die op afwezigheid gelden ("de rang van de opvolger is ongelijk aan 2", "het aantal opvolgers is gelijk aan 0", …) | 5 Wachters | `UNSATISFIABLE — a fail-fast guard fired: no viol/2 witness (the input is not stratified?)` |

Geen golden heeft een van deze vormen; daarom zien de gates het niet. De golden
`jutterswet-opvolgerquorum` zelf leest "merk groter dan 1" onder `alle`: een
vergelijking die een groeiende waarde alleen waar kan maken, en die clingo goed
draagt.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een van twee routes, te kiezen bij het oppakken (de tweede is de normvraag
  die het besluit over de kwantorvouw van 2026-09-23 en het besluit over de
  generaties openlaten):
  - (a1) clingo weigert luid en bij naam (de tweede houding van
    `spec/IR-SPEC.md` ch. 5.5) een voorwaarde van een creatie op een door
    WF-045 toegelaten kring die haar waarheid kan verliezen als de kring verder
    groeit, en blijft een monotone lezing zoals die van
    `jutterswet-opvolgerquorum` dragen;
  - (a2) de verifier leest zo'n vergelijking als negatieve kant (WF-046), zodat
    de vormen hierboven niet meer welgevormd zijn; dan een negatief geval per
    vorm.
- (b) Checks in `backends/clingo/test` (bij a1) of negatieve gevallen (bij a2)
  voor elke rij hierboven, rood eerst, met het absolute antwoord van ref ernaast.
- (c) De twee restposten van ch. 6.1 zeggen wat er nu gebeurt (voorstel).
- (d) De recursieve goldens (`jutterswet-wachtersketen` en zijn rgs-tweeling,
  `jutterswet-opvolgerquorum`, `jutterswet-opvolgerversies`,
  `jutterswet-erflijn`, `synth2-budgetgrens-onder`,
  `jutterswet-budgetgrens-op`) blijven spelen; alle gates van `CLAUDE.md` groen.

## Omvang

Klein tot middel; de grens tussen een monotone en een niet-monotone lezing is
het werk.

May touch: `backends/clingo/**` (a1) of `spec/verifier/**` en
`corpus/negative/<nieuw>/` (a2), `spec/RUNTIME-SEMANTICS.md` en
`spec/IR-SPEC.md` (voorstel), kaartverplaatsing.

## Raakvlakken

Na `20261009-creatie-initialisatie-leest-groeiende-waarde` (de weigering van
de initialisaties, op dezelfde graaf) en `20261009-clingo-uitvoer-breekt-af-op-lengte`
(beide raken de vertaling van een recursieve creatie in clingo).
