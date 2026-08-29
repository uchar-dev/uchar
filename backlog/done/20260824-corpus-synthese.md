# Corpus-synthese, eerste golf: synth3 over de 5b/5c-constructen

Goal: de PLAN §8-synthesepijplijn in het klein maar echt: een eerste
golf synthetische goldens (synth3-*) gericht op de constructen die
nog dun gedekt zijn — sumtypes/match, folds over lijsten, structs
(incl. geneste), geneste defaults-groepen, strikte Let, subscope-creatie
met kenmerk-init — elk geval met 2-3 cases met ABSOLUTE verwachtingen.
Admissie drievoudig per PLAN §8: verifier-clean, ref ≡ clingo exact,
en noop-refuter (of reden-dragende survivor); plus de coverage-eis:
elk geval oefent zijn geclaimde construct aantoonbaar (verwijder het
construct en de verwachting verandert — toon dat steekproefsgewijs).
Kwaliteit boven kwantiteit: 10-20 goldens. Generators zijn
/tmp-throwaway conform het bestaande synth-patroon; de canonieke
serialisatie via de gate-2-slice. KRITIEK: een gegenereerd geval
waarop ref en clingo VERSCHILLEN wordt NOOIT toegelaten — het gaat
als geminimaliseerde divergentievondst in het kaartrapport (de
adversariële route), nooit in het corpus.

Acceptance: alle negen gates groen; de admissie- en coverage-toetsen
per golden gedocumenteerd in de kaart; rood-eerst
(noop/foute-verwachting-proeven getoond); twee opus-lenzen verwerkt.

May touch: corpus/golden/synth3-*/, scripts/check-noop.sh (alleen
survivor-pins), backlog/. Out of scope: al het overige — GEEN
executor-, spec- of frontend-wijziging; een gevonden divergentie is
een rapport, geen fix.

---

## Voortgang — synth3-golf geleverd (2026-08-24)

**11 goldens, 42 cases, absolute verwachtingen; alle negen gates groen.**
Generatoren waren /tmp-wegwerp (het bestaande synth-patroon); canonieke
serialisatie via de gate-2-slice (`ser`/`escape`/`RawNum` letterlijk uit
`scripts/check-ir-schema.sh`), dus elke `ir.json` round-trip't byte-identiek
(gate 2 bevestigt: 95 files valideren én round-trippen). Admissie drievoudig
per golden per case: verifier-clean, ref ≡ clingo exact op de HAND-berekende
verwachting (executors alleen als bevestiging), noop-refuter of
reden-dragende survivor. Elke waarde met de hand nagerekend en pas daarna
door beide executors bevestigd.

### De goldens en hun geclaimde constructen

| golden | constructcombinatie (5b/5c) | cases |
|---|---|---|
| `synth3-standtarief` | Match over 3-constructor SumType; payload-binder-arm, nullary-arm, arm met deling+Afgerond (direct), leeg scrutinee → leeg | 4 |
| `synth3-vangstsom` | Fold over lijst van SumType; inline Match in de lambda; IfThenElse-plafondkap | 4 |
| `synth3-metingtal` | MapList + Fold + IfThenElse (voorwaardelijke telling); MapList + Fold (verschoven som) | 4 |
| `synth3-keuringtal` | FilterList (sum-typed = én ≠) + ListLength; kale ListLength | 4 |
| `synth3-vondststaat` | vlakke struct: StructLiteral-bouw + FieldAccess + struct-gelijkheid-kenmerk | 3 |
| `synth3-nestwaarde` | geneste struct: geneste StructLiteral-bouw + FieldAccess-over-FieldAccess (diepste scalair) | 3 |
| `synth3-drempelheffing` | strikte Let ×3 genest; binder tweemaal gelezen; WF-077 conditie-operand; leeg-barrière; Afgerond load-bearing | 4 |
| `synth3-nestkorting` | geneste defaults-groep (Default.excepties, ongeordend) met BEREKENDE exceptie-consequenties (Binair over invoer `grondslag`, geen kale Literal): berekend-enkel/waarde-gelijk-via-verschillende-expr/verschillend-conflict/berekende-terugval/init — zie lensronde 1 | 5 |
| `synth3-kamerheffing` | subscope-lowering: ResolvedObjectCreatie + boolean-getypeerde-expressie kenmerk-init (WF-027-relaxatie) + genavigeerde teruglezing | 3 |
| `synth3-besluitschakel` | Construct-in-regel (twee disjuncte writers) + downstream Match over berekend sum-attribuut | 4 |
| `synth3-letbeslag` | strikte Let met Match-body; binder tweemaal gelezen; direct-afgeronde productarm; Match-leeg vs Let-leeg-barrière | 4 |

### Coverage-toets (construct eruit → verwachting verandert) — 5 getoond

1. `synth3-standtarief/case3-betwist`: Afgerond uit de Betwist-arm → ref-fout
   `value 100/3 does not fit the declared 2 decimals` (i.p.v. 33.33).
2. `synth3-vangstsom/case1-gevuld`: IfThenElse-kap geneutraliseerd → 49.75
   (alles opgeteld) i.p.v. 19.75.
3. `synth3-nestwaarde/case1-gevuld`: diepe FieldAccess ingekort naar één niveau
   → kernprijs 3 i.p.v. 12.5.
4. `synth3-nestkorting/case2-conflict-pq`: excepties-subgroep verwijderd → de
   conflict-fact `Jutter.korting/0` bestaat niet meer (default 0 beslist 100.00).
5. `synth3-drempelheffing/case2-onder`: IfThenElse-guard weg → 26.00 (belasting
   altijd toegepast) i.p.v. 20.00.

### Rood-eerst (foute-verwachting-proeven, letterlijke uitvoer)

Off-by-a-cent / off-by-one op een toegelaten verwachting → beide executors
verwerpen letterlijk (de verwachting is load-bearing, niet vacuüm-groen), o.a.:
`synth3-vangstsom` totaal 19.75→19.76 → ref `MISMATCH: J1.totaal = 19.75`,
clingo `MISMATCH: J1.totaal = [1975], expected 1976`; `synth3-drempelheffing`
heffing 37.48→37.47 → beide MISMATCH; `synth3-kamerheffing` heffing 200→199 →
beide MISMATCH. Plus gate 7 (noop) zelf: 213 cases refuteren een niets-doende
executor.

### check-noop.sh — survivor-pins (expliciet gemeld)

Negen nieuwe survivors toegevoegd (leeg/afwezigheid die de defeasible standing
zélf is), elk met reden: `standtarief/case4-leeg` (leeg scrutinee door Match),
`vangstsom/case4-afwezig` + `metingtal/case4-afwezig` + `keuringtal/case4-afwezig`
(leeg-lijst-propagatie door Fold/Map/Filter), `vondststaat/case3-afwezig`
(record no-hole), `drempelheffing/case4-leeg-grondslag` (strikte Let
leeg-barrière), `besluitschakel/case4-afwezig` (onbesliste sum → Match-leeg),
`letbeslag/case3-leeg-regime` (Match-leeg met gevulde Let-binding),
`letbeslag/case4-leeg-grondslag` (Let leeg-barrière onder een Match-body).
Gate 7: 33 survivors (24 bestaand + 9 nieuw).

### Divergentievondsten (ref↔clingo) — NIET toegelaten, geminimaliseerd

Geen enkele *stille* ref≢clingo-waardeafwijking gevonden — de differentieel
faalde luid waar hij niet overeenkwam. Drie shapes waar clingo fail-fast en ref
slaagt (door de admissie correct geweigerd, dus buiten het corpus gehouden):

1. **Niet-terminerende deling die een binder/arm kruist vóór de afronding**
   (semantisch, de interessantste). Minimale getuigen:
   `Afgerond(Let g = a÷b; g, 2)` en `Afgerond(Match{K x → a÷b}, 2)` met a÷b
   niet-terminerend → **ref** rondt exact-rationeel af naar 33.33; **clingo**
   `UNSATISFIABLE — viol("non-exact division (an explicit rounding is mandated
   before precision is lost)")`. De *directe* vorm `Afgerond(a÷b, 2)` komt in
   beide overeen (33.33). Duiding: clingo dwingt de ch. 2.4-regel ("rond af vóór
   precisieverlies") af op elk binder-/arm-materialisatiepunt (waar het naar
   geschaalde integers moet), ref pas op de schrijfgrens (het draagt de exacte
   rational als thunk door). Golden 1 is daarom bewust zó gebouwd dat de
   Afgerond de deling *direct* omsluit.
2. **FilterList met numeriek predicaat** (dekkingsgat, luide translation fault).
   `ListLength(FilterList(l, m, m > drempel))` → ref telt; clingo
   `TRANSLATION FAULT: a FilterList condition here is a sum-typed (in)equality
   over the bound element (no corpus case for more)`. clingo's FilterList dekt
   alleen sum-getypeerde (on)gelijkheid (de S19-inventaris). Golden 3 telt
   daarom via Map+Fold+IfThenElse; golden 4 filtert over een sum-lijst.
3. **Afronding van een negatieve operand** (dekkingsgat, luide translation
   fault). `Afgerond(x, 2)` met x<0 → ref rondt; clingo
   `viol("negative operand under rounding is not translated")`. Golden 7 is
   daarom herbouwd zonder negatieve tussenwaarde onder de afronding.

Alle drie zijn "ref slaagt, clingo faalt luid" — de admissie-protocol (admit.sh
regel 4) weigert ze als ref≢clingo. Geen ervan is een spec-fix binnen deze kaart
(out of scope); ze staan hier als adversariële route / rapport.

### Open flanken

- **Twee opus-lenzen**: nog niet verwerkt — als subagent heb ik geen mechanisme
  om onafhankelijke opus-review-lenzen te spawnen; dit blijft de review-stap voor
  de orchestrator/maintainer (het lens-patroon uit de git-log). Eerlijk gemeld
  als gat, geen groen subset dat groen heet.
- Divergentievondst 1 (binder-kruisende niet-terminerende deling) is een
  scherpe, reproduceerbare ref≢clingo-grens die een eigen kaart verdient
  (spec-besluit: waar dwingt v1 de ch. 2.4-afronding af — op de schrijfgrens
  zoals ref, of op elk materialisatiepunt zoals clingo?). Buiten deze kaart.

---

## Lensronde 1 verwerkt (2026-08-24)

**BEVINDING 1 [MINOR/CONFIRMED] — synth3-nestkorting was een re-skin van anchor
jutterswet-nestuitzondering.** Zelf geverifieerd (beide IR's gelezen): de oude
nestkorting was structureel isomorf — één kenmerk-gated default (rechtA→100) met een
`ongeordend`-excepties-groep van drie kale-Literal ExceptionGroups (grondP=30,
grondQ=45, grondR=30, twee-gelijk-één-verschillend) plus een init-default; de vijf
cases vielen op een strikte deelverzameling van de negen van nestuitzondering.
Alleen kenmerk-namen, doel-attribuut en bedragen verschilden; de beslisprocedure
was identiek. **De bevinding is terecht.**

**Fix — geretarget, niet gedropt.** De bevinding noemt drie retarget-opties; twee
vielen af na verificatie: (a) *geordende* meerledige excepties bestaan niet —
WF-075 (spec-regel 1854) eist de `unordered`-markering zodra een groep ≥2 leden
heeft, dus `ongeordend:false` met meerdere leden is ill-formed; (c) *drielaags*
nesting is al geankerd door `jutterswet-nestdiepte`. Overblijft (b): **een
exceptie-consequentie die een berekende expressie is i.p.v. een kale Literal** —
in het hele corpus dragen álle exceptie-armen kale Literals (nestuitzondering,
nestdiepte, nestkenmerk, toelagegronden, tariefrecht-catala). Beide executors
evalueren de default-consequentie via hun algemene expressiepad (ref `eval_expr`,
clingo `expr_term`), dus een berekende arm is toelaatbaar en verifier-clean.

De geretargete golden: elke consequentie is nu een berekende `Binair` (PLUS) over
een invoer-attribuut `grondslag` — de eigen terugval van de hoogste default
(`grondslag+90`) en de drie armen P=`grondslag+20`, Q=`grondslag+35`,
R=`(grondslag+15)+5`. **R en P berekenen dezelfde waarde via een verschillende
expressie**, dus case2 oefent waarde-gelijkheid-reconciliatie over syntactisch
verschillende expressies — precies wat geen kale-Literal-anker kan raken (daar is
"gelijk" letterlijk dezelfde literal-string).

### Admissie (drievoudig, PLAN §8) — alle vijf cases

| case | kenmerken / grondslag | korting | construct |
|---|---|---|---|
| `case1-berekend-enkel` | rechtA,grondP / 12.00 | 32.00 | enkele berekende exceptie-arm; waarde volgt de invoer |
| `case2-gelijk-verschillende-expr` | rechtA,grondP,grondR / 10.00 | 30.00 | P=30 == R=30 via verschíllende expressie → geen conflict |
| `case3-conflict-berekend` | rechtA,grondP,grondQ / 10.00 | leeg + `Jutter.korting/0` | P=30 ≠ Q=45 → conflict op berekende waarden |
| `case4-terugval-berekend` | rechtA / 10.00 | 100.00 | terugval naar de berekende eigen consequentie van de hoogste default |
| `case5-init-basis` | (geen) / 10.00 | 5.00 | init-default beslist |

Verifier-clean (95 golden clean). ref ≡ clingo exact op elke handberekende
waarde (executors als bevestiging: alle vijf `oracle: PASS` / `clingo: PASS`).

### Coverage-toets (construct eruit → verwachting verandert) — 2 getoond

A. **Waarde-gelijkheid is load-bearing.** R's binnenste literal 15.00→16.00
   (R wordt 31 ≠ P=30) → case2's schone 30.00 klapt in beide executors om naar
   een conflict: ref `MISMATCH: M.korting = leeg, expected 30` +
   `channel carries: Jutter.korting/0 × M`; clingo `underived (leeg), expected
   3000` + `1 inconsistent fact(s)`. Dit is de exacte eigenschap die geen
   kale-Literal-anker oefent.
B. **De payload is berekend, niet een vaste literal.** case1 `grondslag`
   12.00→20.00 (verwachting nog 32.00) → korting volgt de som: ref
   `MISMATCH: M.korting = 40, expected 32`; clingo `= [4000], expected 3200`.

### Rood-eerst (foute-verwachting-proef, letterlijke uitvoer)

case1 32.00→32.01 → ref `MISMATCH: M.korting = 32, expected 32.01`; clingo
`MISMATCH: M.korting = [3200], expected 3201`. De verwachting is load-bearing.

### check-noop.sh — ongewijzigd

Geen survivor-pin toegevoegd of verwijderd: alle vijf nieuwe cases refuteren een
niets-doende executor (de korting is berekend/afgeleid, case3 verwacht bovendien
een `inconsistent`-feit dat noop niet produceert). Gate 7 blijft 33 survivors.

### Alle negen gates groen (slotregels)

1. `check-self-contained: PASS`
2. `check-ir-schema: PASS (schema valid; 95 file(s) validate and round-trip)`
3. `check-verifier: PASS (95 golden clean; 138 negatief rejected for their declared rule)`
4. `check-case-schema: PASS (schema valid; 263 case file(s) validate)`
5. `check-oracle: PASS (246 cases replayed exactly; NOT replayed, no case file yet: corpus/golden/jutterswet-vormenkabinet)`
6. `check-clingo: PASS (246 cases replay exactly through clingo — ref = clingo over the corpus)`
7. `check-noop: PASS (213 cases refute a do-nothing executor; 33 pinned survivors pin defeasible standing)`
8. `check-rood: PASS (17 red cases fault observably in both executors)`
9. `check-catala: PASS — frontend suite green (128 checks); 7 catala golden(s) regenerate byte-identically`

Result: de eerste synthesegolf staat: 11 synth3-goldens met 42 cases
over de dun gedekte 5b/5c-constructen (Match/SumType, de
fold-familie, vlakke en geneste structs, strikte Let, geneste
defaults-groepen met conflict en terugval, subscope-creatie met
boolean-getypeerde kenmerk-init), elk met hand-berekende absolute
verwachtingen en de drievoudige admissie van PLAN §8: verifier-clean,
ref ≡ clingo exact op de handwaarde, en noop-refutatie of een
reden-dragende survivor-pin. De coverage-toets is voor vijf goldens
getoond (construct eruit → verwachting verandert); de lensronde ving
één degeneraat (een re-skin van een bestaand anker), vervangen.
De belangrijkste opbrengst zijn drie ref↔clingo-divergentievondsten
die de admissie correct WEIGERDE en die geminimaliseerd in deze
kaart staan — waaronder één semantisch scherpe: een
niet-terminerende deling die een Let-binder of Match-arm kruist
vóórdat de Afgerond hem omsluit rondt in ref exact-rationeel af waar
clingo op de tussenwaarde faalt. Dat is een spec-besluit (waar
dwingt v1 de afronding af: op de schrijfgrens of op elk
materialisatiepunt) en gaat als genummerde vraag naar de maintainer;
tot dat besluit blijft de vorm buiten het corpus.
