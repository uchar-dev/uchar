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

## Lensronde (2026-08-25)

Twee opus-lensbevindingen verwerkt; beide zelf gereproduceerd (rood-eerst,
letterlijke uitvoer).

- **BEVINDING 1 [MAJOR/CONFIRMED] — gevallen schaalhek in `closed_division`.**
  WAAR. `closed_division` berekende `out_scale = max sl sr` en liet
  `ctx.write_scale` vallen; het regel-niveau-`translate_division` gebruikt
  `max (max sl sr) ctx.write_scale`. Gevolg: een gesloten-body constante-deler-
  deling waarvan het exacte quotiënt méér decimalen vergt dan de operand-schalen
  bieden maar wél in de fold-/schrijfschaal past, vuurde ten onrechte de
  niet-exact-deling-viol → clingo UNSAT waar ref de waarde aanvaardt. De
  tutorial-deler `2,0000` (sr=10000 ≥ write_scale) maskeerde het; alleen een
  bare-integer-deler legt write_scale bloot. Rood-eerst gereproduceerd op een
  wegwerp-IR (`acc + x/4` in een 2-dec accumulator, kindertallen=[1]): verifier
  `PASS`, ref `oracle: PASS` (0,25), clingo `UNSATISFIABLE — ... viol("non-exact
  division ...",plist("kindertallen"))`. Fix: `out_scale = max (max sl sr)
  ctx.write_scale` (r. 2293, spiegelt r. 1371). Na de fix beide `PASS` (0,25).
  Vaste getuige: `corpus/golden/jutterswet-kinderkwart` (`Fold(+, 0.00)` van
  `acc + kindertal / 4` in het 2-dec attribuut `kwartheffing`; case1 [1,2,3] →
  1,50, case2 [] → 0,00; hand berekend, ref ≡ clingo). S15 bijgewerkt: de
  out-scale is `max(s_L, s_R, write_scale)`, gelijk aan het regel-niveau.

- **BEVINDING 2 [MINOR/CONFIRMED, buiten scope] — omsluitende Let-binder in een
  gesloten fold-body.** WAAR (pre-existing; `closed_term`'s Binder_ref r. 2055
  laat de bind-atomen vallen — de Plus/Min-tak deed dit al vóór deze kaart, de
  nieuwe Maal-tak is enkel een nieuwe route). De verplichte compositie
  `Let(f=<parameter>) in Fold(+,0, λacc x. acc + x*f)` gereproduceerd: verifier
  `PASS` (WF-064 weigert het niet), ref `EVALUATION FAULT: unbound binder "f"
  (WF-064 should preclude this)`, clingo grond-crash `error: unsafe variables
  ... 'LB3' is unsafe`, clingo exit 65. Beide executors falen (geen fout
  antwoord); WF-064-ill-gevormd, dus nooit een golden. De echte leemte is
  WF-064-volledigheid (spec/verifier) óf een nette `closed_term`-fault i.p.v.
  een onveilige regel — beide buiten deze kaart (May touch sluit spec/verifier
  en ref uit; een nette-fault-ontwerpkeuze verbreedt de kaart). Getrackt:
  `backlog/new/20260825-closed-body-enclosing-binder.md`.

Alle negen gates opnieuw groen (slotregels):
1. `check-self-contained: PASS`
2. `check-ir-schema: PASS (schema valid; 103 file(s) validate and round-trip)`
3. `check-verifier: PASS (103 golden clean; 142 negatief rejected for their declared rule)`
4. `check-case-schema: PASS (schema valid; 277 case file(s) validate)`
5. `check-oracle: PASS (259 cases replayed exactly; NOT replayed, no case file yet: corpus/golden/jutterswet-vormenkabinet)`
6. `check-clingo: PASS (259 cases replay exactly through clingo — ref = clingo over the corpus)`
7. `check-noop: PASS (226 cases refute a do-nothing executor; 33 pinned survivors pin defeasible standing)`
8. `check-rood: PASS (18 red cases fault observably in both executors)`
9. `check-catala: PASS — frontend suite green (155 checks); 10 catala golden(s) regenerate byte-identically`

Result: de gesloten-body-fence is verbreed met de multiplicatieve
vormen van tutorialpagina 2-4 — algemene Maal, geld×rate via de
gedeelde gesplitste cent-encodering, de decimal-of-harmonisatie en
de gcd-gereduceerde constante-deler-deling — met werkelijk één
codering (het regel-niveau emitteert nu via dezelfde helpers,
byte-identiek geverifieerd) en met alle hekken meebewogen én vurend
getoond (46340-ranges, niet-exact-viol als rood-getuige,
cent-afronding). ref ≡ clingo over de volle corpus. De lensronde
ving een echte MAJOR — de gesloten deling liet ctx.write_scale uit
de exactheidstoets vallen, waardoor clingo spurieus weigerde waar
ref rekende — gefixt in spiegeling met het regel-niveau, met vaste
regressiegetuige; en een pre-existing MINOR: een buiten-Let-binder
gelezen in een fold-body passeert de verifier terwijl beide
executors er verschillend op stuklopen (ref: unbound-binder-fault
met de eigen notitie dat WF-064 dit hoort te weren; clingo: onveilig
grondingsfout) — een verifier-gat op beschermd pad, als voorstel
geregistreerd, niet hier gedicht. Blijvend luid, gedocumenteerd in
S19: runtime-deler, Percentage_van, Gedeeld_door_abs, Tot_de_macht
en Let binnen gesloten bodies.
