# Multiplicatieve gesloten-body-termen in clingo (2-4-muur D)

Goal: de gesloten-body-termvertaling (de S15-fence "a closed-body
term here is a bound binder, a literal, a parameter, a +/-
combination or a Construct") verbreedt met de multiplicatieve vormen
die tutorialpagina 2-4 vergt: Maal — inclusief geld×rate via de
bestaande gesplitste Mult_mon_rat-encodering — plus de
decimal-of-conversie en de constante-deler-deling (de
gcd-gereduceerde vorm), alles door HERGEBRUIK van de bestaande
regel-niveau-encoderingen in de body-term-context: één codering,
dezelfde schaaldiscipline, dezelfde range- en afrondingshekken (de
lens-les van de vorige golf: een gevallen hek is een stille
overloop). Rood-eerst op de huidige fence-tekst; eigen
corpus-getuige(n) met een multiplicatieve fold-/functie-body met
absolute verwachtingen door beide executors; ASP-TRANSLATION S15
bijgewerkt (S19 blijft de hoogste sectie; schrap wat gedicht is).

Acceptance: alle negen gates groen; rood-eerst per vorm; ref ≡
clingo over de volle corpus; twee opus-lenzen verwerkt vóór de
merge.

May touch: backends/clingo/, corpus/golden/ (nieuwe getuigen),
spec/ASP-TRANSLATION.md, scripts/check-noop.sh (alleen indien
nodig), backlog/. GEEN spec/CONSTRUCTS.md; de frontend-fence blijft
in deze kaart ONaangeraakt (kaart 2 versmalt hem). Out of scope:
ref/, frontends/, normatieve spec-documenten, spec/verifier/.

## Resultaat (2026-08-25)

`closed_term` verbreed met de multiplicatieve vormen; één codering per
vorm, gedeeld met het regel-niveau:

- **Maal** (algemeen) — het geschaalde product `(tl*tr)` op sl·sr met de
  46340-range-hekken, geen coalesce (een gesloten-body-binder wordt nooit
  leeg).
- **geld×rate** (`Afgerond{REKENKUNDIG,2}` over 2-dec×4-dec, `money_mult_shape`)
  — de gesplitste cent-encodering via de nieuwe gedeelde helpers
  `money_mult_fences`/`money_mult_body`; regel-niveau houdt zijn
  subject-gekeyde `mm%d`-helper (byte-identiek), de gesloten body zet de
  A/C-toewijzingen INLINE (verse variabelen) zodat de fold-recurrentie de
  per-element-index bindt.
- **constante-deler-deling** (`x / 2.0000`) — de gcd-gereduceerde vorm via
  de nieuwe gedeelde `const_div_reduce`; numerator-range-hek + niet-exact-viol
  op de list-id, waarde exact-gegate (`numt \ dent == 0`). De
  decimal-of-conversie (ToRat) is de out-scale-harmonisatie — geen aparte node.

Alle hekken bewegen mee naar de body-context (geverifieerd dat ze VUREN,
gekeyd op de list-id-witness): algemeen-Maal-46340 (x=50000 → UNSAT),
geld×rate-range (grondslag=$300000 → UNSAT), niet-exact-deling (rood), en de
cent-afronding engageert (case1 x=1: 185,175 → 185,18). ref blijft
exact-rationeel op de overloop-gevallen — daarom is de niet-exact-deling het
beide-executor-hek (differentieel als `decimal-nonterminating`).

Getuigen:
- `corpus/golden/jutterswet-kinderfoldheffing` — fold-body met geld×rate
  (cent-afronding), constante-deler-deling+ToRat, én een algemene Maal;
  case1 heffing=432,08 / puntentotaal=6, case2 (lege lijst) 0,00 / 0. Hand
  berekend, ref ≡ clingo.
- `corpus/rood/closed-nonexact-division` — fold-body `x/3` niet-exact;
  clingo niet-exact-viol, ref "does not fit the declared 0 decimals" — beide.

ROOD-EERST vastgelegd: vóór de verbreding gaf clingo op beide getuigen
letterlijk `TRANSLATION FAULT: a closed-body term here is a bound binder, a
literal, a parameter, a +/- combination or a Construct (no corpus case for
more)` — de fence rond r. 2059.

S15 bijgewerkt (de gesloten-body-termregel plus een sub-lijst per vorm); uit
S19 geschrapt dat multiplicatieve fold-bodies het "no corpus case"-hek rijden
(nu vervangen door: ze vertalen; rest — runtime-deler, Percentage_van,
Gedeeld_door_abs, Tot_de_macht, Let — rijdt het hek nog).

Alle negen gates groen (slotregels):
1. `check-self-contained: PASS`
2. `check-ir-schema: PASS (schema valid; 102 file(s) validate and round-trip)`
3. `check-verifier: PASS (102 golden clean; 142 negatief rejected for their declared rule)`
4. `check-case-schema: PASS (schema valid; 275 case file(s) validate)`
5. `check-oracle: PASS (257 cases replayed exactly; NOT replayed, no case file yet: corpus/golden/jutterswet-vormenkabinet)`
6. `check-clingo: PASS (257 cases replay exactly through clingo — ref = clingo over the corpus)`
7. `check-noop: PASS (224 cases refute a do-nothing executor; 33 pinned survivors pin defeasible standing)`
8. `check-rood: PASS (18 red cases fault observably in both executors)`
9. `check-catala: PASS — frontend suite green (155 checks); 10 catala golden(s) regenerate byte-identically`

Openstaand: twee opus-lenzen vóór de merge (acceptance).
