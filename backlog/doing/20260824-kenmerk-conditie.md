# De genavigeerde kenmerk-lezing in conditie-positie

Goal: een Catala-boolean over een relatie heen gelezen in
CONDITIE-positie ("is de eigenaar overzees" als voorwaarde) vertaalt
de plugin naar de al gelande genavigeerde Kenmerk-check (IR-SPEC ch.
4.1a/5, WF-053; beide executors dragen hem — anker
jutterswet-kustwacht). Geen nieuwe knoop; cond_expr verbreedt met de
genavigeerde blad-lezing, symmetrisch aan de bestaande vormen. De
WAARDE-positie-lezing blijft per maintainersbesluit 4-A (2026-08-24)
luid geweigerd — die refusal blijft exact staan. Fixture(s) door
verifier+ref+clingo plus een eigen catala-golden onder gate 9.

Acceptance: rood-eerst (de huidige refusal letterlijk); alle negen
gates groen; de waarde-positie-refusal aantoonbaar intact; twee
opus-lenzen verwerkt vóór de merge.

May touch: frontends/catala/, corpus/golden/ (de nieuwe
catala-golden), scripts/check-noop.sh (alleen indien nodig),
backlog/. GEEN spec/CONSTRUCTS.md. Out of scope: ref/, backends/,
normatieve spec-documenten, spec/verifier/.

## Geland — de genavigeerde kenmerk-lezing in conditie-positie (2026-08-24)

Route A, besluit-vrij: `cond_expr` verbreed met de genavigeerde
blad-lezing; geen nieuwe knoop, de al gelande wire-knoop `Kenmerk`
(navigatie + navigatieBinding, ch. 4.1a/5, WF-053) draagt hem. Beide
executoren dragen hem al (anker jutterswet-kustwacht) — de fixtures
lopen drievoudig groen zonder ref/backends aan te raken.

**ROOD-EERST (letterlijk, HEAD vóór de wijziging, op de conditie-lezing
van een boolean subscope-uitvoer):**
`catala_frontend: refused: condition shape not supported in the 5a
skeleton` (exit 3). Ná de verbreding vertaalt dezelfde bron schoon
(exit 0) naar de genavigeerde `Kenmerk`-check.

**De wijziging (`frontends/catala/`, smalste vorm).**
- `translate.ml`: `make_exprs` krijgt een `check_subscope_output`
  (spiegel van `read_subscope_output`, maar `I.conditie option`).
  `cond_expr` krijgt één `EStructAccess`-tak: een boolean uitvoer-veld
  van een gedeclareerde subscope-instantie → een genavigeerde
  `Kenmerk_check` (`navigatie` = callee-rol, `navigatie_binding` = de
  gesynthetiseerde terugkoppeling), symmetrisch aan het bare
  `check_var`. Een niet-boolean uitvoer-veld en de multi-state-flank
  weigeren luid, exact als de waarde-positie-lezing. De drie
  `make_exprs`-aanroepplaatsen (scope-eigen, override, topdef) krijgen
  de nieuwe parameter.
- `emit.ml` (bijvangst, noodzakelijk): de `Kenmerk`-emit liet
  `navigatie_binding` vallen (`= _`). Nu geschreven als
  extensie-veld `navigatieBinding` (heel of niet, nooit null — de
  schema-discipline), precies zoals `AttribuutRef.navigatieBindingen`.
  Dit corrigeerde een latente drop-bug: de besluit-6
  context-override-kenmerk-check emitteerde zijn `navigatieBinding`
  niet. `corpus/golden/jutterswet-hardheidsaftrek-catala/ir.json`
  is hergegenereerd naar de gecorrigeerde wire-vorm (ref ≡ clingo
  ongewijzigd — de navigatie werd voordien via de rolnaam alleen
  opgelost; de binding maakt de hop expliciet).

**WAARDE-positie-refusal intact (besluit 4-A, "toon dat hij nog
vuurt").** `refuse_bool_value` vuurt onveranderd:
`boolean variable "q" read in a value position … (read it as a
condition instead)` (exit 3). De cond-verbreding raakt de
waarde-positie-tak niet.

**Fixture + eigen catala-golden.**
- Suite-fixture `frontends/catala/test/overzeelezing.catala_en`
  (+ `cases/overzee_hoog.json`, `cases/binnengaats.json`) door
  verifier + ref + clingo in de suite; +6 checks.
- Eigen catala-golden `corpus/golden/jutterswet-overzeelezing-catala/`
  (art. 13, overzeetoeslag; eigen auteurschap, rechtenvrij):
  `bron.catala_en` + `ir.json` (byte-identiek geregenereerd onder gate
  9) + `case1-overzees.json` ($6000) + `case2-binnengaats.json`
  ($3000), geijkt tegen `catala interpret`. De uitzonderingssport
  (overzee → verdubbeling) leest `overzees` van de gemunte eigenaar
  over de terugkoppeling; ref ≡ clingo op beide cases.

**Alle negen gates groen (slotregels):**
1. `check-self-contained: PASS`
2. `check-ir-schema: PASS (schema valid; 99 file(s) validate and round-trip)`
3. `check-verifier: PASS (99 golden clean; 138 negatief rejected for their declared rule)`
4. `check-case-schema: PASS (schema valid; 270 case file(s) validate)`
5. `check-oracle: PASS (253 cases replayed exactly; NOT replayed, no case file yet: corpus/golden/jutterswet-vormenkabinet)`
6. `check-clingo: PASS (253 cases replay exactly through clingo — ref = clingo over the corpus)`
7. `check-noop: PASS (220 cases refute a do-nothing executor; 33 pinned survivors pin defeasible standing)`
8. `check-rood: PASS (17 red cases fault observably in both executors)`
9. `check-catala: PASS — frontend suite green (144 checks); 9 catala golden(s) regenerate byte-identically`

**Open flank (buiten kaart, besluit-vrij gehouden).** Een boolean
subscope-uitvoer in echte WAARDE-positie (als gelijkheids-operand,
bv. `x.b = y.b`) wordt vandaag níét geweigerd: `read_subscope_output`
levert een genavigeerde boolean `AttribuutRef` (dangling t.o.v. de
callee-karakteristiek). Dit is een bestaand gat in de waarde-positie
(niet geraakt door deze cond-verbreding). Besluit 4-A houdt de
waarde-positie uit scope; gemeld voor de maintainer. Nog open:
twee opus-lenzen vóór de merge.
