# clingo-tarief-decimalen: geld × tarief met minder dan vier decimalen, en de WW-tweeling

Nulmeting van de kaart dialect-uitzondering-op-uitzondering (2026-09-25):
de RegelSpraak-tweeling van `anchor/ww-herleving-inkomen` — boom en
rechtvaardigingen gelijk aan het anker, `ref` 10/10 en de Drools-exec-tak
PLAYED 10 — is niet toegelaten, omdat clingo haar frontend-emissie op alle
tien cases weigert: `UNSATISFIABLE — viol("arithmetic range exceeded
(clingo grounds in machine integers)")`. Oorzaak, gemeten: de frontend
typeert de literal `0,8750` op haar kleinste schaal (drie decimalen; het
product op vijf), zoals `ExpressieBouwer.decimalenVanLiteral` gedocumenteerd
doet en zoals de toka-pariteit vereist; het anker schreef vier en zes. In
`backends/clingo/lib/translate.ml` eist `money_mult_shape` een tarief met
precies vier decimalen (`dec m = Some 2 && dec r = Some 4`), en het gewone
productpad hekt elke operand op 46340 — een maandloon van 2400,00 (240000
op schaal 2) valt daarbuiten. Met alleen die literal op vier decimalen
slaagt clingo 10/10. Het is een beperking van de ASP-backend, geen
frontendfout: RUNTIME-SEMANTICS §2.10 maakt het literaltype de bron, `ref`
en de exec-tak rekenen gelijk, en clingo weigert luid zonder fout antwoord.
De bron van de tweeling staat reproduceerbaar in
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §8.2.

## Acceptatie (bij oppakken aanscherpen)

- (a) De gesplitste geld-×-tariefcodering van clingo draagt een tarief met
  ten hoogste vier decimalen (het tarief exact naar schaal vier gebracht),
  met een check in de backendsuite (`backends/clingo/test/test_clingo.ml`)
  en een rood-eerst-meting op de WW-emissie.
- (b) De tweeling `corpus/golden/ww-herleving-inkomen-rgs/` toegelaten:
  `bron.rgs`, de emissie als `ir.json` (gate 10 byte-identiek), de tien
  ankercases byte-gelijk, NOTE met de structurele diff en triage — daarin
  ook het naamverschil van de groep (`toekenning art 15` tegen `recht op
  uitkering bij herleving`; `name` is geen `source`); ref ≡ clingo ≡ exec.
- (c) Pins (**voorstel**): gate 7 de overlever `case7-invoer-staat-tegen-
  blokkade` met de reden van het anker; gate 12 `EXPECTED_PLAYED` +1,
  `DRL_CHANNEL` klasse (A) +1 en de koptelling; CONSTRUCTS rij 8 (de
  hoofdsessie) meldt de toelating.
- (d) Alle veertien gates groen.

May touch: `backends/clingo/**`, `corpus/golden/ww-herleving-inkomen-rgs/**`
(nieuw), `scripts/check-noop.sh` en `scripts/check-drools.sh`
(**voorstel**), kaartverplaatsing. Niet in deze kaart: de literaltypering
van de frontend (die draagt de toka-pariteit) en de ankers.

## U-besluiten (hoofdsessie, 2026-09-25, bij de claim)

- U1 De gesplitste geld-×-tariefcodering in `backends/clingo/lib/translate.ml` (`money_mult_shape`, r. ~8130–8150) draagt een tarief met ten hoogste vier decimalen door het tarief exact naar schaal vier te brengen (0,875 op schaal 3 = 8750 op schaal 4); geen verandering voor tarieven die al vier decimalen dragen; een check in `backends/clingo/test/test_clingo.ml` (mandaat) met rood-eerst op de WW-emissie (de literal op drie decimalen).
- U2 De tweeling `corpus/golden/ww-herleving-inkomen-rgs/`: de bron uit `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §8.2 (sinds de fixronde van dialect-uitzondering-op-uitzondering draagt de emissie twee `source`-records meer op de uitzonderingsgroepen), emissie als `ir.json` (gate 10 byte-identiek), de tien ankercases byte-gelijk, NOTE met de structurele diff en triage (ook het naamverschil van de groep: `name` is geen `source`), ref ≡ clingo ≡ Drools-exec; gate 7: `case7-invoer-staat-tegen-blokkade` als overlever met de reden van het anker (**voorstel**); gate 12: `EXPECTED_PLAYED` +1 en klasse (A) van `DRL_CHANNEL` +1 met de koptelling (**voorstel**; de hoofdsessie telt de pins van parallelle kaarten op). De registerzin voor CONSTRUCTS rij 8 (de toelating) in het Result.
- U3 Gates 1–8, 10, 12, 13.

## Result (2026-09-25, bouwer)

## Result — clingo-tarief-decimalen (2026-09-25)

### Ronde 3 (lensfix)

**Stand.** Branch `fix/lens-clingo` vanaf de unie `card/k7-unie` (36750511), worktree `wt/clingo-tarief`.
**Nieuwe kop: 09cea5be** (na de aanvulling hieronder; na de eerste lensfix was het 69762c51). Niet gepusht, niet
gemerged, werkboom schoon. Beschermd/spec (voorstel): `scripts/check-drools.sh`, `spec/ASP-TRANSLATION.md`.

| commit | inhoud |
|---|---|
| 39000ffa | rood-eerst: de twee routes apart gepind, de nulvorm van de tekenrand, de getuige voor het tekenhek |
| 3b591d5c | A4: het tarieftekenhek en de bodywachter toetsen de term vóór de herschaling; B1-commentaar |
| 69762c51 | B1, A3, A5: ASP-TRANSLATION S18(c), S19, gesloten-bodybullet; NOTE; historieblok check-drools.sh |
| c709fcbc | aanvulling, rood-eerst: vijf getuigen voor tekenhekken die een overgelopen product toetsen |
| 1136c27c | aanvulling: een tekenhek leest het teken van een product uit de operanden (afronding, splitsing, deling) |
| 59ef8a26 | aanvulling: ASP-TRANSLATION S19, waar de tekenhekken het teken lezen (voorstel) |
| acb8217f | aanvulling, rood-eerst: twee getuigen voor het tekenhek op een overgelopen som |
| bf9c8a43 | aanvulling: een tekenhek leest de som alleen waar zij niet overliep, of uit operanden met één teken |
| 081a2108 | aanvulling: ASP-TRANSLATION S19, ook de som (voorstel) |
| 9ce2fb05 | aanvulling, rood-eerst: het tekenhek op een overgelopen som in een gesloten body |
| 09cea5be | aanvulling: ook een som in een gesloten body geeft haar teken zo door |

Regio's van de parallelle fixer (r. 1878/1941, 5009–5023, 5601–5606) niet aangeraakt. Mijn hunks in
`translate.ml`, in de nummering van de unie: 297 en 384 (type `sign_src`, ctx-velden), 446 (`sign_conds`,
`register_bound`), 1816 (`coalesce0`), 2311–2356 (splitsingshekken, `money_mult_rate`), 3052 (`let_binder`), 5092
(optelarm), 5126 (vermenigvuldigingsarm), 5199 (afrondingsarm), 5847 (afgeronde deling), 8134 en 8190 (optel- en
vermenigvuldigingsarm van `closed_term`), 8319–8421 (route en aanroepers van de splitsing), 10620 (ctx-constructie).
De dichtstbijzijnde bij de regio's van de fixer is 1816, 62 regels vóór 1878.

**B1 — de oorzaak van de toelating.** De tekst schrijft de toelating nu toe aan de route op de decimalen van de
termen: de frontend annoteert `0,8750` op drie, het lexeem houdt vier, de oude route las de annotatie en stuurde het
product naar S6; nu gaat het met k = 1 de splitsing in. De herschaling staat beschreven als bedoeld voor een tarief dat
op één tot drie decimalen geschreven of gedeclareerd is, in het corpus alleen doorlopen door `synth3-letbeslag`.
Aangepast: ASP-TRANSLATION S18(c) (de alinea en de getuigezin), NOTE `ww-herleving-inkomen-rgs` (alinea "De literal"),
het historieblok 201 -> 202 in `scripts/check-drools.sh` (dezelfde toeschrijving; voorstel), de kop van het testblok,
de commentaren van `money_mult_shape` en `money_mult_rate`, en de registerzin hieronder.
Pins op de programmatekst (`program_holds`): WW-route — `*((8750)/100)` aanwezig, `(8750*` en `|(8750)| >` afwezig
(k = 1, geen herschalingshek); herschalingsroute — `*(((875*10))/100)` en `|(875)| > 214748364` aanwezig.
Mutanten op de unie-code met de nieuwe suite (md5 unie `dc4f34b9…`, herstel gecontroleerd):
- M-a (recept: in `money_mult_roles` `| Some d -> false && d >= 1 && …`): `1790 checks, 11 FAILURES`, waaronder de
  herschalingspin; de WW-routepin blijft groen.
- M-b (recept: `dl`/`dr` uit `static_type`, binders via `term_decimals`): `4 FAILURES` (de drie annotatie- en
  lexeemframes, en de A4-getuige die op de unie al rood is); de WW-routepin blijft groen.
- M-a + M-b: `14 FAILURES`, nu ook de WW-routepin (`missing: [*((8750)/100)]`) en het WW-waardeframe. Gemeten: de
  tweeling hangt af van "termroute óf verbreding", zoals de lens zei.

**A3 — de tekenrand.** Tekst S18(c) en S19: "a negative operand beside a negative or zero other operand". Nieuwe pins
(`clingo_fence_fires`): −100,00 × 0,000 (tarief op drie decimalen) en 0,00 × −0,5 (één decimaal): ref 0,00, clingo
weigert met `negative operand under money multiplication is not translated`. Op M-a: `clingo answered the rows`.

**A4 — het tekenhek op de overgelopen term.** Fix: `money_mult_rate` geeft `(geschaalde term, eigen term)`;
`money_mult_fences` en `money_mult_body` lezen het teken op de eigen term (`~tr_sign`). Bij k = 1 byte-gelijk.
Getuige: 1,00 × 214748,4 (k = 1000). Vóór (unie): `viol("negative operand under money multiplication is not
translated","W1"); viol("arithmetic range exceeded …","W1")` — check rood. Na: alleen het bereikhek — groen.

**A5 — ook een geld met eenheid.** Tekst: de gesloten-bodybullet en S19 "with both operands unit-free"; S18(c) "one
whose operands are not both unit-free"; S19 "a product below four decimals whose money or rate carries a unit".

**Metingen vóór/na.**
- Suite: unie `1782 checks, 0 failures`; unie-code met de nieuwe checks `1790 checks, 1 FAILURES` (A4); na de fix
  `1790 checks, 0 failures`.
- Programmadiff over 749 paren (golden 677, rood 43, toka 29), `translate.ml` unie (dc4f34b9…) tegen de fix
  (c91daef4…): 4 veranderd, alle vier cases van `synth3-letbeslag` — het tekenhek en de bodywachter toetsen `(10)` in
  plaats van `((10*100))` — met identieke stabiele modellen; rood 0, toka 0.
- Lensproef p5 (15 varianten × 10 cases, ref / clingo unie / clingo fix): alleen `t7-attr1 L9` (214748,4) verandert,
  van NR naar R; alle andere 149 uitkomsten gelijk.

**Gates** (elk los na `eval $(opam env)`, op 69762c51):
```
check-self-contained: PASS
check-clingo: PASS (backend suite 1790 checks, floor 83; 677 cases replay exactly through clingo — ref = clingo over the corpus)
check-rood: Drools leg PASS (11 fault-pinned, 32 not-carried with reason)
check-rood: PASS (43 red cases fault observably in ref and clingo, floor 26; 86 pinned fragments cite none of the 36 listed bottom sentences)
```
Gate 5 niet gedraaid: ref niet geraakt. De jar bouwde gate 8 één keer opnieuw (`drools-jar: rebuilt (24 source path(s)
newer than the jar …; 4s)`), omdat de unie Drools-bronnen had veranderd. Naamtabeltoets: `OK`.

**Registerzin CONSTRUCTS rij 8, gecorrigeerd (B1):**

> **Landed (2026-09-25, card `clingo-tarief-decimalen`):** the RegelSpraak twin of `anchor/ww-herleving-inkomen` is admitted as `ww-herleving-inkomen-rgs`, the third form-2 witness and the first source whose three `Uitzondering op` blocks each name the preceding rule as carrier (einde on the base, herleving on einde, blokkade on herleving: the first level a chain position above the base, the two deeper levels nested). It differs from its anchor in the group's shape, the group's name (the frontend names a group after its base rule; `name` is no `source`), the type of the rate literal `0,8750` (three decimals, the product five, where the anchor writes four and six) and the conversion pair of the unit `dag` — in no justification or decision — and plays the anchor's ten cases, byte-identical, in ref, clingo and the Drools exec branch; the DRL-text branch refuses it on its defaults sentence, and its `case7-invoer-staat-tegen-blokkade` survives a do-nothing executor for the anchor's reason. What held it back was the ASP backend alone: its split money × rate encoding (ASP-TRANSLATION S18(c)) routed on the literal's annotation (three decimals) and took a four-decimal rate only; it now routes on the decimals the terms carry — the lexeme `0,8750` carries four, so the product takes the split unmultiplied — and finds the money on either side. Beside that, with both operands unit-free, a rate written or declared at one to three decimals is brought exactly to scale 10⁴ under a range fence, its sign read before the rescale; in the corpus only `synth3-letbeslag` takes that rescale. A four-decimal rate keeps its program byte for byte, and a whole-number rate or a unit-carrying operand keeps the S6 product. No wire, verifier or frontend change.

**Aanvulling — het tekenhek op een overgelopen product (algemene afronding, en hetzelfde patroon elders).**
Gemeten fout: in de algemene afrondingsarm toetste het tekenhek het product zelf; lensproef `t13-lit-012500 L1`
(2400,00 × 0,12500, S6-pad) gaf naast het bereikhek `negative operand under rounding is not translated`.
Hetzelfde patroon op drie andere plekken, alle gefixt:
- de afgeronde deling toetste de voorgeschaalde teller en noemer (`(tl)*knum`, `(tr)*kden`);
- de geld-×-tariefsplitsing toetste een geldterm die een product is achter een `coalesce0`-hulpregel;
- een `Let` rond een product (`let_binder`) gaf het overgelopen product door aan de afronding.
Het teken van het product was bovendien niet alleen onterecht negatief: een negatief product dat positief overliep,
kreeg geen getuige (−2400,00 × 0,12500: vóór alleen het bereikhek).
Fix (`translate.ml`): de vermenigvuldigingsarmen van `expr_term` en `closed_term` leggen hun product vast
(`ctx.signs`, `Product (tl, tr)`); `coalesce0` en `let_binder` leggen een variabele vast die zo'n product bindt
(`Bound`); `sign_conds` geeft de conjuncties waaronder een term negatief of positief is, voor een product uit de
operanden (negatief waar de tekens verschillen, een constante operand opgevouwen), voor een gebonden product via
hulpatomen `<pred>_neg`/`<pred>_pos` uit de body van de hulpregel, één keer uitgezonden als een hek erom vraagt; elke
andere term zoals vóór. De afronding, de splitsing en de deling (op `tl` en `tr`, vóór de voorschaling) lezen het teken
zo. Wat een tekenhek op een gewone term toetste, blijft tekst voor tekst gelijk. De regio's van de parallelle fixer
(r. 1878/1941, 5009–5023, 5601–5606) niet aangeraakt.
Rood-eerst (c709fcbc) op de `translate.ml` van 69762c51: `1800 checks, 5 FAILURES` — de afronding van 2400,00 × 0,12500
(onterechte negatiefgetuige), −2400,00 × 0,12500 (terechte negatiefgetuige ontbrak), (maandloon × tarief) × 0,12500 over
een gebonden product, het geldproduct in de splitsing ((maandloon × tarief) × 0,8750), en tarief / 3 afgerond op twee
decimalen met 21474837 × 100 overgelopen. Na de fix: `1800 checks, 0 failures`.
Programmadiff over 749 paren (golden 677, rood 43, toka 29), `translate.ml` van 69762c51 (c91daef4…) tegen de fix
(98d0ddb7…): **22 veranderd, alle golden, alle met identiek stabiel model** — `synth-afrondingswaaier` 5,
`synth-afrondingswaaier-rgs` 5, `synth2-afrondingscombinaties` 4, `synth3-standtarief` 4 (afgeronde deling: `(A) < 0`
in plaats van `((A)*10) < 0`), `synth3-drempelheffing` 4 (een `Let`-gebonden product: twee `let10_neg`-regels uit de
operanden, het hek leest `let10_neg(S)`); rood 0, toka 0.
Lensproef p5 (runner van 69762c51 tegen de fix): alleen `t13-lit-012500 L1` verandert, van NR naar R; de andere 149
uitkomsten gelijk.
Hetzelfde patroon bij een SOM, gemeten en gefixt (bf9c8a43): (maandloon + maandloon) afgerond op hele eenheden, met
20.000.000,00 — de som loopt over naar −294967296 en gaf een onterechte negatiefgetuige; met −20.000.000,00 loopt zij
positief over en ontbrak de terechte. Het teken van een som volgt niet uit de tekens van de operanden. De optelarm op
regelniveau legt zijn som vast (`Sum`); `sign_conds` leest haar waarde alleen waar geen bereikhek van het subject
vuurde (`not viol("arithmetic range exceeded …",S)` — een som loopt alleen over waar haar eigen bereikhek vuurt), en
daarnaast bepalen operanden met hetzelfde teken (vóór hun harmonisatie) het teken, hoe groot ze ook zijn. Rood-eerst
(acb8217f) op de `translate.ml` van 1136c27c: `1804 checks, 2 FAILURES`; na de fix `1804 checks, 0 failures`.
Tekst (voorstel): één zin in ASP-TRANSLATION S19 over waar de tekenhekken het teken lezen, met de som.

Ook een som in een GESLOTEN body loopt over waar haar bereikhek (per lijst-id) vuurt: het tarief x + 0,5 in een fold
met x = 300.000.000 (geharmoniseerd 3000000005, overgelopen naar −1294967291) gaf naast het bereikhek een onterechte
negatiefgetuige, gesleuteld op `plist("kindertallen")`. Getuige (9ce2fb05) rood op de code van bf9c8a43
(`1806 checks, 1 FAILURES`); de optelarm van `closed_term` legt zijn som nu vast met het lijst-id als sleutel
(09cea5be): `1806 checks, 0 failures`.

**Eindmetingen van de aanvulling** (op 09cea5be):
- Programmadiff over 749 paren, de `translate.ml` van 69762c51 tegen de eindstand (fd379fa5…): **26 veranderd, alle
  golden, alle met identiek stabiel model** — `synth-afrondingswaaier` 5, `synth-afrondingswaaier-rgs` 5,
  `synth2-afrondingscombinaties` 4, `synth3-standtarief` 4 (afgeronde deling: `(A) < 0` in plaats van `((A)*10) < 0`),
  `synth3-drempelheffing` 4 (een `Let`-gebonden product: `let10_neg` uit de operanden),
  `jutterswet-kamerhuishouden-catala` 2 (de somterm van het tarief via `co7_neg(S)`), `jutterswet-kinderfoldheffing` 2
  (de som van het tarief in de gesloten body, gelezen waar geen bereikhek van het lijst-id vuurde); rood 0, toka 0.
- Lensproef p5, volledige rapportregels, runner van 69762c51 tegen de eindstand: alleen `t13-lit-012500 L1` verandert
  (NR -> R); de andere 149 regels gelijk.
- Gates (elk los na `eval $(opam env)`, op 09cea5be):
```
check-self-contained: PASS
check-clingo: PASS (backend suite 1806 checks, floor 83; 677 cases replay exactly through clingo — ref = clingo over the corpus)
check-rood: Drools leg PASS (11 fault-pinned, 32 not-carried with reason)
check-rood: PASS (43 red cases fault observably in ref and clingo, floor 26; 86 pinned fragments cite none of the 36 listed bottom sentences)
```
  Jar: `drools-jar: fresh (jar newer than 160 source files)`. Naamtabeltoets `OK`. Gate 5 niet: ref niet geraakt.
- Wat de fix niet dekt, gelezen: een tekenhek dat een term leest na een andere hulpregel dan coalesce of `Let` (een
  `Match`-arm, een deling `qN`, een geld-×-tariefresultaat `mmN`) leest die term zoals vóór; de datumsom (datum ± duur)
  is geen getal en blijft buiten.

Overige open punten van ronde 2 blijven staan (nul decimalen ter bevestiging; tekenbestendige splitsing; `term_decimals`
naast `expr_term`; voorstel §8.2 eenheidsrij; gates 9, 11, 14 niet gedraaid).

### Stand

Branch `card/clingo-tarief-decimalen`, worktree `wt/clingo-tarief`, basis main 20e508d5, claim e73121bd.
**Nieuwe kop: ab99139f** (was 234848a3). Niet gepusht, niet gemerged, werkboom schoon. De tak raakt beschermde en
spec-paden (`scripts/check-noop.sh`, `scripts/check-drools.sh`, `spec/ASP-TRANSLATION.md`, het voorstel
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`) en is daarom als geheel een voorstel.

| commit | inhoud |
|---|---|
| ba26ee78 | rood-eerst: 15 checks in `backends/clingo/test/test_clingo.ml`, 7 rood op de oude code |
| f3bf9e98 | tarief op 1–3 decimalen exact naar schaal 10^4 (`money_mult_rate`); testblok naar 17 checks |
| cab4fb54 | tweeling `corpus/golden/ww-herleving-inkomen-rgs/` + pins gate 7 en gate 12 (**voorstel**) |
| 234848a3 | NOTE: de telmethode van de structurele diff expliciet |
| 25c7e843 | rood-eerst (fixronde): tarief links van het geld, afwijkende annotaties, lexeem voorbij vier decimalen — 6 rood |
| ba9d127a | fixronde: het geld aan beide kanten; de route op de afgeleide termschaal (`term_decimals`, `money_mult_roles`) |
| ab99139f | fixronde: `spec/ASP-TRANSLATION.md` S18(c), gesloten-bodybullet, S19; voorstel §5.2, §8.2, §8.4, §8.5 (**voorstel**) |

Tellingen: backendsuite 1733 -> 1761 checks (+28); golden cases 643 -> 653; rgs-goldens 81 -> 82; gate-7-overlevers
38 -> 39; gate 12 played 197 -> 198, DRL-tak 173 ongewijzigd, `DRL_CHANNEL` 78 -> 79 regels (klasse (A) 29 -> 30).

### Wat de codering nu doet (`backends/clingo/lib/translate.ml`)

- **Route** (`money_mult_roles`, `money_mult_shape` delegeert): een `Rounding{halfAwayFromZero,2}` over een `times`
  (eventueel onder `Let`s) neemt de gesplitste codering als één operand een geldterm op 2 decimalen is en de andere een
  tarief-term op 4 decimalen, of op 1–3 decimalen met beide operanden eenheidsloos (`eff_unit … = None`). **Het geld mag
  aan beide kanten staan**; bij twee operanden op 2 decimalen is de linker het geld. Een tarief op 0 decimalen en een
  tarief met eenheid onder vier decimalen blijven op het gewone productpad (S6).
- **De decimalen komen uit de termen, niet uit de annotatie** (`term_decimals`, nieuw, puur): de schaal die
  `expr_term`/`closed_term` de term geven, op dezelfde manier afgeleid — literal: lexeem; attribuut en parameter:
  declaratie; binder: de peeled `Let`-waarde of de schaal in `ctx.binders`; `FieldAccess`: het veld in de structdeclaratie
  van de basis (zoals `project_field`); `plus`/`min`/`verminderd met`: max; `maal`: som; `percentage van`: som + 2; deling:
  max(s_L, s_R, write_scale); abs-deling: 5; `Rounding`: haar decimalen; `Let`: de body. Elke andere knoopsoort: geen
  afleiding, dus het gewone pad. Er bestond geen losse pure afleiding in `translate.ml`; `Eval.static_type` en
  `Ir_v0.Checks.expr_type` geven bij een samengestelde knoop allebei de annotatie terug.
- **Rollen**: `translate_money_mult` en `closed_money_mult` zetten de twee termen in hun rol (geld, tarief) volgens de
  route, gelezen vóór de peel iets bindt.
- **Tarief naar schaal 10^4** (`money_mult_rate`, gedeeld): `(tr*k)` met k = 10000/sr onder `range_guard` op
  `max_int31/k`; k = 1 laat de term ongemoeid. De backstop blijft als verdediging staan, maar is onbereikbaar: de route
  leidt dezelfde schalen af die de backstop toetst.
- **Weigerzinnen, letterlijk** (alleen clingo; geen verifier- of laderzin, geen enumwaarde of type):
  - gewijzigd: `the split money multiplication expects a 2-decimal money and a rate of at most 4 decimals (%d,%d)`;
  - nieuw (onbereikbaar achter de route, beide aanroepers): `money multiplication base is not a money × rate product (money_mult_shape gated this)`.

### Metingen van de fixronde

- **Rood-eerst** (25c7e843) op de `translate.ml` van 234848a3: `test_clingo: 1761 checks, 6 FAILURES` — literal 0.8750
  links van het geld (UNSAT), attribuut op 3 decimalen links (UNSAT), gesloten body met het tarief links (catch-all),
  samengesteld tarief met annotatie 5 boven afleiding 3 (UNSAT: annotatieroute naar het gewone pad), samengesteld tarief
  met annotatie 3 onder afleiding 5 en lexeem `0.12500` op type 3 (beide: backstopfout `(100,100000)`).
  Op de fix: `1761 checks, 0 failures`.
- **Mutanten** (bouw en meting in één keten, md5 per binary, herstel gecontroleerd op md5 97f0916e…):
  - route via de annotatie (recept: in `money_mult_roles` `dl`/`dr` uit `static_type` in plaats van `term_decimals`,
    binders uitgezonderd): `3 FAILURES`, precies de drie afleidingsframes;
  - zonder symmetrie (recept: de regel `else if dr = Some 2 && rate ~money:r dl l then Some true` weg): `3 FAILURES`,
    precies de drie frames met het tarief links.
- **Programmadiff** over 725 paren (653 golden, 43 rood, 29 toka), de `translate.ml` van f3bf9e98 tegen de nieuwe:
  **0 veranderd**. Geen corpusproduct heeft het geld rechts, en in het corpus valt de afgeleide route overal samen met
  de oude. Gate 13 twist alleen annotaties met eenheid, dus zijn groen zegt niets over de route op een eenheidsloos
  samengesteld tarief; dat doen de twee annotatieframes en de annotatiemutant.
- Eén fout van mij tussendoor: de eerste versie van de twee gewone-padframes gebruikte 0,875 op schaal 10^5 (term
  87500), boven het S6-operandhek, dus UNSAT; aangepast naar 0,125 (term 12500, 100,00 × 0,125 = 12,50).

### Eerdere fasen (ongewijzigd)

- Reproductie: bron §8.2 letterlijk; emissie literal lexeem `0.8750` type 3, product 5; verifier PASS; oude code ref 10/10,
  exec `PLAYED 10`, DRL EXCLUDE, clingo 10/10 `UNSATISFIABLE … arithmetic range exceeded …` (W1..W10). Jar één keer
  (`mvn -o -B`, stap 1); gate 12 bouwt zelf, gates 8 en 13 vonden hem `fresh`.
- Programmadiff van de eerste fix (715 paren): 4 veranderd, `synth3-letbeslag`, identieke modellen; tarieven op vier
  decimalen byte-gelijk.
- Randen van de verbreding (gemeten): letterlijk 0–4 zonder eenheidstoets maakte 100,00 × −3, −100,00 × 3 en
  100,00 × 21,00 % (oud clingo PASS) tot weigeringen; daarom 1–3 eenheidsloos.
- Tweeling: `bron.rgs`, `ir.json` (gate 10 byte-identiek, gate-2-rondgang byte-identiek), tien cases `cmp`-gelijk, NOTE;
  verifier PASS, ref 10/10, clingo 10/10, exec `PLAYED 10`, DRL EXCLUDE; no-op negen weerlegd, case7 overleeft.
  Verwachtingen: `recht op uitkering` aanwezig in case1, 2, 5, 7, 9, 10, afwezig in case3, 4, 6, 8; case7 pint verder
  alleen `consistent`; de andere negen inkomensgrens 2100,00, verstreken periode 123 (case1, 8), 92 (case3, 10) of leeg,
  nieuwe maxdatum 2027-04-02 (case1, 8), 2026-11-30 (case2, 5, 6, 9), 2026-01-15 (case3, 10), 2026-08-31 (case4).
  Structurele diff: 140 regels, elf blokken, vier verschillen, alle (i).

### Gates

Fixronde (op ab99139f, elk los na `eval $(opam env)`):
```
check-self-contained: PASS
check-oracle: PASS (ref suite 994 worked examples, floor 489; 653 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
check-clingo: PASS (backend suite 1761 checks, floor 83; 653 cases replay exactly through clingo — ref = clingo over the corpus)
check-drools: PASS — played 198; channel floor 54 non-PLAYED goldens pinned by name; played matches the pinned 198; DRL-text branch played 173 (pin 173); DRL-text floor 79 non-PLAYED goldens pinned by name; JUnit 776 tests, floor 239
check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
```
Naamtabel `python3 spec/proposals/v1-naamtabel-toets.py`: `v1-naamtabel-toets: OK`.

Eerste ronde (op cab4fb54): gates 1–8, 10, 12, 13 PASS — ir-schema 294 files; verifier 294 golden clean, 433 negatief,
275 on arm; case-schema 696; oracle 653; clingo 1750 checks, 653 cases; noop 614 + 39 overlevers; rood 43 red cases,
Drools leg 11 + 32; regelspraak 618 tests, 82 rgs-goldens; drools played 198, DRL 173; reftype 41/23, 106 replays.

### Gewijzigde regels in beschermde paden en spec (**voorstel**)

- `scripts/check-noop.sh` r. 181–190 (reden van de overlever, die van het anker), r. 229 (het pad in `SURVIVORS`).
- `scripts/check-drools.sh` r. 891–900 (historieblok 197 -> 198), r. 901 `EXPECTED_PLAYED=198`, r. 914 "seventy-six of the
  seventy-nine", r. 919–928 kop klasse (A) (30 regels, 25 onder exec, 29/28/29, "The 25 is 29 … those 28"), r. 935–937
  hertelzin, r. 967 `"ww-herleving-inkomen-rgs|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"`.
- `spec/ASP-TRANSLATION.md`: de gesloten-bodybullet (geld × 4 of eenheidsloos 1–3, geld aan beide kanten,
  `money_mult_rate`, het vijfde hek); S18(c) nieuwe alinea "A rate of fewer decimals, and the money on the right"
  (route op de afgeleide termschaal, symmetrie, schaal 10^4 onder bereikhek, 0 decimalen en eenheid op S6, de tekenrand,
  getuige `ww-herleving-inkomen-rgs`); S19 (tekenrand, drie bereikhekken, nieuwe backstopzin, backstop onbereikbaar).
- Voorstel: §5.2, §8.2 (eerste rij en literalrij), §8.4 en §8.5 elk één gedateerde [M]-zin: de tweeling is sinds de kaart
  clingo-tarief-decimalen (dezelfde dag) toegelaten als `ww-herleving-inkomen-rgs`, 10/10 in ref, clingo en de Drools exec-tak.

### Registerzin voor `spec/CONSTRUCTS.md` rij 8 (de hoofdsessie landt haar)

> **Landed (2026-09-25, card `clingo-tarief-decimalen`):** the RegelSpraak twin of `anchor/ww-herleving-inkomen` is admitted as `ww-herleving-inkomen-rgs`, the third form-2 witness and the first source whose three `Uitzondering op` blocks each name the preceding rule as carrier (einde on the base, herleving on einde, blokkade on herleving: the first level a chain position above the base, the two deeper levels nested). It differs from its anchor in the group's shape, the group's name (the frontend names a group after its base rule; `name` is no `source`), the type of the rate literal `0,8750` (three decimals, the product five, where the anchor writes four and six) and the conversion pair of the unit `dag` — in no justification or decision — and plays the anchor's ten cases, byte-identical, in ref, clingo and the Drools exec branch; the DRL-text branch refuses it on its defaults sentence, and its `case7-invoer-staat-tegen-blokkade` survives a do-nothing executor for the anchor's reason. What held it back was the ASP backend alone: its split money × rate encoding (ASP-TRANSLATION S18(c)) now takes a unit-free rate of one to three decimals, brought exactly to scale 10⁴ under a range fence, where it took a four-decimal rate only, finds the money on either side of the product, and routes on the decimals its terms carry rather than on a composite's annotation; a four-decimal rate keeps its program byte for byte, and a whole-number or unit-carrying rate keeps the S6 product. No wire, verifier or frontend change.

De zin in rij 8 "The twin of `anchor/ww-herleving-inkomen` is not admitted: …" vervalt daarmee.

### Wat aan de kaart en de opdracht niet klopte (gemeten)

1. U1 "ten hoogste vier decimalen": letterlijk uitgevoerd maakt het drie gedragen vormen tot luide weigeringen; gebouwd is
   1–3 eenheidsloos, 4 ongewijzigd, 0 niet.
2. "de conventie `faults_with`/`passes`": `test_clingo.ml` kent geen `passes`; gebruikt zijn `ref_matches`,
   `clingo_matches`, `both_answer`.
3. "gate 6 volledig (1733 checks + 1 en 643 cases)": nu 1761 checks en 653 cases.
4. De WW-emissie vroeg geen schaalverhoging (haar lexeem komt op 10^4 binnen); het U1-voorbeeld geldt voor lexeem `0.875`
   of een attribuut op drie decimalen, beide in de suite.
5. Voorstel §8.2, eenheidsrij: `teken: "ONBEPAALD"` bestaat niet op de L5-`Unit`; het verschil is alleen `canonical` +
   `factorToCanonical` (niet aangepast: buiten de opdracht van de fixronde).
6. Fixronde punt 8 noemt "de bestaande afleiding in translate.ml die de termschaal geeft": die bestaat alleen binnen
   `expr_term`/`closed_term` (met bijwerkingen); `term_decimals` is een pure functie die dezelfde regels volgt.
7. Het regimebestand noemt basis 2f47d80e; deze worktree staat op 20e508d5.

### Open punten

1. **CONSTRUCTS rij 8**: registerzin hierboven; de oude "not admitted"-zin vervalt (hoofdsessie).
2. **Nul decimalen** (keuze ter bevestiging): `d >= 1` -> `d >= 0` in `money_mult_roles` neemt geheel-getaltarieven mee;
   gemeten effect in de eerste ronde: de vier programma's van `jutterswet-tariefrecht-catala` veranderen, identieke
   modellen; negatieve operanden worden dan geweigerd; het frame "a whole-number rate keeps a negative money" moet mee.
3. **Teken onder de splitsing**: rekenen op |M| en |R| en het teken terugzetten heft de rand op 1–3 decimalen en de
   bestaande weigering op 4 op, maar verandert de codering op vier decimalen.
4. **`term_decimals` naast `expr_term`**: twee plaatsen die dezelfde schaalregels volgen. Wijkt de afleiding ooit af, dan
   vangt `money_mult_rate` een geld- of tariefschaal die niet past luid op (de backstop); een tarief-term op kleinere
   schaal dan afgeleid krijgt een grotere factor en blijft exact. Een structurele koppeling (de schaalregels één keer
   schrijven) zou deze dubbeling opheffen; niet gebouwd.
5. **Voorstel §8.2, eenheidsrij** (punt 5 hierboven), voor de hoofdsessie.
6. Gates 9, 11 en 14 niet gedraaid (U3). De programmadiff dekt de 29 toka-cases (0 veranderd, beide rondes) — het
   clingo-been van gate 11; Catala-tarieven staan op vier decimalen, geld links, en `Mult_mon_int` houdt zijn route, dus
   gate 9 ziet geen veranderde vorm (beredeneerd, niet gemeten).

## Lens en fixronde (2026-09-25, hoofdsessie)

Lens (uitvoerders-trio, unie 36750511, eigen meting in kopieën): EERST FIXEN. Goedgekeurd op eigen meting: de gesplitste geld-×-tariefcodering op eigen proeven (tarief van één tot vier decimalen, geld links en rechts, heel getal en eenheidsdragend op S6, het bereikhek luid bij 216915,52, het herschalingshek bij k = 1000); de programmadiff over 749 paren (golden 14 veranderd: synth3-letbeslag met gelijke modellen en de WW-tweeling van UNSAT naar PASS; rood en de pariteitszone 0); de WW-tweeling (tien cases byte-gelijk aan het anker, ref en clingo 10/10, exec PLAYED 10, DRL-tak EXCLUDE op de defaults-zin, alleen case7 als no-op-overlever met de reden van het anker, structurele diff van vier verschillen); het nultarief op S6 als juist gewogen; `term_decimals` naast `expr_term` als dubbeling die luid of exact blijft.

Blokkerend: B1 — de teksten (ASP-TRANSLATION S18(c), de NOTE van de tweeling en de registerzin) schreven de toelating van de tweeling toe aan de verbreding naar één tot drie decimalen, terwijl de term van `0,8750` vier decimalen draagt (het lexeem) en met k = 1 gesplitst wordt: met de verbreding uit speelt de tweeling nog steeds 10/10; wat haar toeliet is de route op de decimalen van de termen; alleen synth3-letbeslag doorloopt de herschaling. Advies: A3 (de tekenrand onvolledig beschreven, de nulvorm niet gepind), A4 (een tarief voorbij het herschalingshek gaf ook een onjuiste "negative operand"-getuige omdat het tekenhek de overgelopen term toetste), A5 (ook een geld met eenheid blijft op S6, de tekst noemde alleen het tarief).

Besluit van de hoofdsessie: de registerzin voor rij 8 is herschreven naar de gemeten oorzaak; de backendsuite pint voortaan beide routes apart (de WW-route zonder herschaling en de herschalingsroute), elk rood-eerst tegen een mutant.

Fixronde (branch fix/lens-clingo, de clingo-tarief-bouwer): vijf commits op de unie. B1: de teksten (S18(c) met de getuigezin, de NOTE van de tweeling, het geschiedenisblok van gate 12, twee commentaren in translate.ml) schrijven de toelating nu toe aan de route op de decimalen van de termen: de frontend annoteert `0,8750` op drie decimalen, het lexeem draagt er vier, de oude route las de annotatie en stuurde het product naar het gewone pad, de nieuwe leest de termen en splitst met k = 1; de herschaling is bedoeld voor een tarief dat op één tot drie decimalen geschreven of gedeclareerd is (in het corpus alleen `synth3-letbeslag`). Beide routes apart gepind op de programmatekst; mutanten: verbreding uit → 11 failures (de WW-pin blijft groen), route via de annotatie → 4, beide samen → 14, nu ook de WW-pin. A3: "a negative operand beside a negative or zero other operand", de nulvorm gepind (−100,00 × 0,000 en 0,00 × −0,5: ref 0,00, clingo weigert met de negatief-operandgetuige). A4: het tekenhek en de bodywachter lezen het teken op de eigen term van het tarief, vóór de herschaling (1,00 × 214748,4 bij k = 1000: vóór twee getuigen, na alleen het bereikhek). A5: "with both operands unit-free" en "a product below four decimals whose money or rate carries a unit". Slotronde (gevonden door de bouwer, gefixt): hetzelfde patroon zat op het gewone productpad en drie plekken erbij — alle tekenhekken lezen het teken nu waar niets overgelopen kan zijn (product uit de twee operanden, ook achter een `coalesce0`- of `Let`-hulpregel via hulpatomen; som alleen waar geen bereikhek van het subject vuurde; afgeronde deling op teller en noemer vóór de voorschaling), acht getuigen rood-eerst; programmadiff 26 goldens veranderd met identiek stabiel model, rood en pariteitszone 0. Suite 1782 → 1806; gates 1, 6, 8 groen; naamtabel OK. Bewust open: een term die via een `Match`-arm, een deling `qN` of een splitsingsresultaat `mmN` binnenkomt, en de datumsom, lezen het teken als vóór.
