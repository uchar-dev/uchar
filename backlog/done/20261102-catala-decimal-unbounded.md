# catala-decimal-unbounded: een Catala-decimal is de onbegrensde vorm

*Vastgelegd door de hoofdsessie op 2026-11-02 bij de landing van `20261102-static-precision-gate`. Besluit Yeb van die dag: optie
(a), een Catala-`decimal` wordt de onbegrensde vorm, zodat de statische precisiepoort voor elke frontend gelijk geldt. De bouwer van de
poort mat dat het zo nog niet kan; Fable-advies: `/Users/yeb/Developer/uchar-dev/werk/static-precision-gate/fable/advies-catala.md`.*

Goal: de Catala-frontend zet een `decimal` op de onbegrensde vorm, en ref, clingo en beide Drools-branches spelen elke Catala-golden
daarmee op dezelfde absolute verwachtingen; RUNTIME-SEMANTICS ch. 2.9 zegt dat (de vaste schaal S = 4 vervalt).

## Waarom het nu niet kan (advies §1)

- Clingo houdt de onbegrensde vorm als geheel getal (`backends/clingo/lib/translate.ml:208-220`): `tarief` = 0,20 raakt de resttoets in
  `fitted` (`:10200-10215`), en het geldproduct brengt de rente naar 10^4 en overschrijdt het rekenbereik (`:3142-3153`). Gemeten:
  `jutterswet-tariefrecht-catala` met `decimals` 4 → 0 is in clingo UNSAT op 4 van 4 cases, ref en verifier PASS.
- De plugin weigert `jutterswet-kamerhuishouden-catala` omdat zij S = 4 zelf vastpint (`rat_str` rond 312-324, de fence `dec r = Some 4`
  rond 2266-2283), geen clingo-grens.
- Bevinding: de defaults-schrijfactie in clingo roept `fitted` zonder `~unbounded` (`translate.ml:11403`), tegen
  `spec/ASP-TRANSLATION.md` 2612-2617.

## Richting (advies §2, uitvoering A)

De clingo-backend leidt voor elk onbegrensd attribuut een schaal af uit de ch. 2.3-grens van zijn schrijfacties en de decimalen van zijn
case-invoer (`Ir_v0.Checks.precision`, `spec/verifier/lib/checks.ml` rond 9680, al linkbaar vanuit `backends/clingo/lib/dune`); is die
eindig, dan rekent clingo op die schaal, anders blijft de bestaande benoemde limiet. Een variant met `tarief` op 2 decimalen speelt 4 van 4
in clingo. Geen rationalen in clingo (B); afleiding in de frontend (C) alleen als terugval. De plugin laat de pin S = 4 los.

Acceptance:
- (a) Rood eerst: de twee Catala-goldens met `decimal` onbegrensd falen nu in clingo of in de plugin; daarna spelen ze in het vierkant.
- (b) De Catala-fixture die `static-precision-gate` naar een deling door 4 zette (3.5000 → 1.7500), keert terug als getuige van een
  quotiënt door een runtime-waarde (`passing_ref_only`-fixture, `frontends/catala/.../suite.ml` rond 247-251).
- (c) De defaults-schrijfactie in clingo met `~unbounded`, met een getuige, eerst rood.
- (d) Ch. 2.9 en `spec/ASP-TRANSLATION.md` (voorstel) zeggen de nieuwe stand; de OPEN-zin in ch. 2.9 en de zin in CONSTRUCTS rij 2 vervallen.
- (e) `make gates` groen.

May touch: `backends/clingo/`, `frontends/catala/`, `corpus/golden/*-catala/`, `spec/RUNTIME-SEMANTICS.md`, `spec/ASP-TRANSLATION.md`
(voorstel), kaartverplaatsing.

## Result

Bouwer, 2026-11-03, gerebased op main `f3d4cff41` (de branch begon op `698a5543f`; geen conflict). Niet gemerged, niet
gepusht; de kaart blijft in `doing/`.

### Commits

- `342fb20dd` kaart naar doing
- `fefd4fb7b` clingo: de onbegrensde vorm op de schaal van haar precisiegrens
- `e67ec7ae7` catala: een decimal is de onbegrensde vorm
- `6bd9eb319` spec: een Catala-decimal is de onbegrensde vorm (voorstel)
- `427cd819f` clingo: de rest van een verdeling hoort bij de bron
- `fee3b6065` clingo: een deling als defaults-consequentie noemt de onbegrensde vorm
- en de commit van dit Result

Buiten de "May touch": `ref/test/test_ref.ml` en `ref/test/dune` (het uitgewerkte voorbeeld van ch. 2.9, zoals de opdracht
vraagt). `ref/lib/`, `spec/verifier/`, `backends/drools/` en de RegelSpraak-frontend zijn niet aangeraakt.

### Clingo (uitvoering A)

- Nieuwe voorstap `backends/clingo/lib/scale.ml` (`Scale.document doc case`), aangeroepen bovenaan `Translate.translate` én
  `Model.check`, zodat programma en modellezing dezelfde declaratie zien, ook in de suite en via de runner in de suites van
  Catala en F-logic. Per plaats van de onbegrensde vorm (attribuut op zijn declarerende type, of parameter): de ruimste van de
  decimalen van de case-waarden (scalair en tijdlijnsegmenten; een cel telt voor zijn attribuut) en de grens van elke
  schrijfactie (regelversie, creatielid, defaults-consequentie op elke diepte, verdeling), omgerekend naar de eenheid van het
  doel met `Ir_v0.Checks.precision`/`in_unit`. Kleinste fixpunt vanaf de case-waarden: een lezing van een plaats van de vorm
  leest de grens tot dan toe (tijdens het zoeken wordt grens 0 als geheel gedeclareerd). Een grens boven 9 is geen eindige
  grens (`pow10`). Uitkomst: eindige grens d > 0 → `decimals = d`; grens 0 en geen eindige grens → declaratie ongewijzigd
  (de benoemde limiet blijft). De draad blijft onaangeroerd.
- Defaults (bevinding c): `emit_own_dec` roept `fitted ~unbounded:target_unbounded` (de vlag door `translate_chain` en
  `translate_group`); een niet-hele waarde op de onbegrensde vorm meldt de zin van die vorm, zonder EF-006. Vervolgbevinding,
  meegenomen in `fee3b6065`: een kale deling als consequentie meldde op dezelfde vorm de begrensde zin (EF-006), want
  `write_unbounded` stond in een defaults-groep niet aan; nu wel, zoals in de schrijfactie van een regel.
- `Translate.unbounded_datatype` is `Scale.unbounded_datatype` (één definitie).
- Bevinding tijdens de bouw: de rest van een verdeling staat op de bron, niet op het doeltype; de eerste versie zocht hem op het
  doeltype (rood op de nieuwe getuige, opgelost in `427cd819f`).

### Catala-plugin

- `num_rat` is de onbegrensde vorm (`whole false, decimals 0`); `rat_scale` weg.
- `rat_str` schrijft het kortste exacte lexeem (`0.2`, `0.15`, `0`, `0.12345`) en weigert alleen een breuk zonder eindige
  decimale vorm (vanuit een Catala-literal onbereikbaar).
- Het hek van de gesloten body eist geld van 2 decimalen maal een rente met een precisiegrens van 1 tot 4 decimalen (ch. 2.3,
  gelezen uit literals, declaraties en de factorregel voor een deling door een literal) in plaats van `dec r = Some 4`.
- Goldens: `jutterswet-tariefrecht-catala/ir.json` en `jutterswet-kamerhuishouden-catala/ir.json` geregenereerd (decimals
  4 → 0 op declaraties en annotaties, lexemen `0.2000` → `0.2`, `1.0000` → `1`, `4.0000` → `4` enz.); hun case-bestanden
  zijn niet veranderd. `source.catala_en` van tariefrecht: de modelleringsnotitie zegt de onbegrensde vorm in plaats van S = 4
  (niet in de IR). De andere tien Catala-goldens regenereren onveranderd.
- Ref, clingo: beide goldens spelen op alle cases. Drools: tariefrecht `PLAYED 4` op beide branches; kamerhuishouden blijft
  `EXCLUDE DROOLS-002` (functionDefs), zoals vóór de kaart (`EXPECTED_CHANNEL` ongewijzigd).

### Fixtures en tests (oud → nieuw)

- `frontends/catala/test/refuse_nonterminating.catala_en` (weigering "terminate") → `decimal_literal.catala_en`, spelend
  door ref en clingo, case `cases/decimal_literal.json` (r = 0.12345; clingo op 5 decimalen).
- (b) Nieuw `division_runtime.catala_en` (`vangst / jutters`), `passing_ref_only` met `division_runtime_seven.json` (3.5) en
  `division_runtime_nine.json` (2.25), plus een check dat clingo 7/2 met naam weigert ("a division into a number declared
  without decimals is not whole …"). `division.catala_en` (deling door 4, 1.7500 en 2.2500) blijft de ref+clingo-fixture.
- `backends/clingo/test/test_clingo.ml`, het blok THE UNBOUNDED FORM: "unbounded form, a non-exact division into it"
  (`translates_carrying` op de benoemde zin, case 6 → 12, 3/2) wordt `both_answer` "at the scale of its bound, a quotient
  by 4" (speelt); "a non-whole product into it" (3 → 9/2) wordt `both_answer` "a product by 3/2". De benoemde zinnen van de
  limiet verhuizen naar een document met een deling door een attribuut (geen eindige grens). Nieuw: invoer 1,5 (3 en 3/8),
  een grens die een grens leest, een grens boven 9 decimalen (geheel gehouden), een parameter op de decimalen van zijn
  case-waarde, een creatielid, een verdeling (aandeel 33.3, rest 0.15), defaults-consequentie (geheel, 9/2 en 7/2 met de
  zin van de vorm). Suite 2320 checks.
- `ref/test`: `WV-2.9-tarief-onbegrensd`, `-tarief-een-kind` (1/5), `-aanslag-een-kind` (3750.00), `-tarief-vier-kinderen`
  (1/10), `-aanslag-vier-kinderen` (1000.00), `-quotient-by-input-well-formed`, `-quotient-by-input` (7/2); 2123 voorbeelden.
- Proza van `decimals.catala_en` en `division.catala_en` en commentaar in `suite.ml` zeggen de onbegrensde vorm.

### Spec (voorstel)

RUNTIME-SEMANTICS ch. 2.9 heet "Catala decimals — the unbounded form": `TRat` is de onbegrensde vorm, de poort zet daar geen
plafond, de ASP-backend houdt een decimal op de schaal van zijn grens of als geheel getal; de vaste schaal S = 4 en de
OPEN-zin vervallen; uitgewerkt voorbeeld met absolute uitkomsten (1/5 en 3750,00; 1/10 en 1000,00; 7/2), letterlijk in
`ref/test`. Ch. 2.4 (alinea over de ASP-backend), ch. 2.9 "scale is a boundary" en "money × decimal" bijgewerkt.
ASP-TRANSLATION S2 (de voorstap en waar de limiet blijft), S18(c) (decimals, en de alinea over de berekende rente, nu over
`jutterswet-kinderweging`), S19 (de zinnen van de vorm gelden zonder eindige grens, voor regel, creatielid en
defaults-consequentie).

### Voorgestelde tekst voor `spec/CONSTRUCTS.md`

- Rij 2: de zin "The Catala route (a Catala `decimal` as the unbounded form, owner's decision 2026-11-02) is OPEN, card
  `20261102-catala-decimal-unbounded`." vervalt.
- Rij 10: "the pinned Catala-decimal scale S=4 in RS ch. 2.9" wordt "the Catala decimal as the unbounded form in RS ch. 2.9".
- `spec/PLAN.md`: geen scopewijziging.

### Rood en mutaties

Rood op `698a5543f` (een losse worktree, gebouwd; logs `werk/catala-decimal-unbounded/logs/stap3-*`, `stap4-*`, `stap5-*`,
`stap6-*`):

- (a) De nieuwe IR van tariefrecht met de clingo van `698a5543f`: UNSAT op 4 van 4 cases, `viol("result of
  penningplichtige.tarief does not fit its declared decimals","J1")` (gerapporteerd als EF-006) en `viol("arithmetic range
  exceeded …")`; ref en verifier PASS. Met de nieuwe clingo 4 van 4 PASS. Kamerhuishouden speelt ook met de oude clingo; zijn
  rood zit in de plugin: met het hek op een rente van precies 4 decimalen (mutatie P1) geeft de regeneratie `GENERATE FAIL`.
- (b) en de literal: de nieuwe fixtures in de suite van `698a5543f`: `decimal_literal.catala_en : translate` exit 3,
  `division_runtime.catala_en : verifier-clean` `WF-023.precision-unbounded` (doel 4 decimalen), de weigering door clingo
  FAIL (clingo PASS op S = 4); 247 checks, 3 failures.
- (c) en de clingo-getuigen: de nieuwe suite op de code van `698a5543f`: 8 failures (quotiënt door 4, invoer 1,5, product
  door 3/2, grens die een grens leest, defaults-consequentie met de zin van de vorm, parameter, creatielid, verdeling). De
  getuige van de deling als consequentie was rood op `427cd819f` vóór `fee3b6065`.
- `ref/test`: `WV-2.9-tarief-onbegrensd` is rood op `698a5543f` (decimals 4); de waarden 1/5, 3750,00, 1/10, 1000,00 en 7/2
  gelden daar ook, want de semantiek van ref veranderde niet (ref draagt de exacte waarde in beide declaraties).

Mutaties in een losse worktree van `427cd819f`, elk één vervanging, niet gecommit (`werk/catala-decimal-unbounded/scratch/mutate.py`,
`logs/stap7-mutations.log`):

| | mutatie | uitkomst |
|---|---|---|
| M1 | Scale: één ronde in plaats van het fixpunt | 4 rood (grens die een grens leest, product zonder eindige grens, defaults geheel, creatielid) |
| M2 | Scale: geen kap op 9 | 1 rood ("a scale of 10^10 exceeds clingo's integer range") |
| M3 | Scale: case-waarden genegeerd | 3 rood (invoer 1,5, grens die een grens leest, parameter) |
| M4 | `Model.check` zonder Scale | 8 rood |
| M5 | defaults `fitted` zonder `~unbounded` | 1 rood |
| M6 | Scale leest geen creatieleden | 1 rood |
| M7 | Scale: rest op het doeltype | 1 rood |
| M8 | Scale leest geen defaults-consequenties | golden tariefrecht case1 en case2 UNSAT |
| P1 | plugin: rente van precies 4 decimalen | `tutorial_end_2_4.catala_en` translate rood; kamerhuishouden `GENERATE FAIL` |
| P2 | plugin: lexeem altijd op 4 decimalen | `decimal_literal` ref en clingo rood; tariefrecht en kamerhuishouden DIFF |
| P3 | plugin: `num_rat` op 4 decimalen | 5 rood (o.a. `division_runtime` verifier-clean, de weigering door clingo); beide goldens DIFF |

### Meting: een RegelSpraak-golden die nu in clingo speelt

Geen. In de corpus heeft alleen `jutterswet-precisiepoort` een attribuut van de onbegrensde vorm (`het ruwe aandeel`, een
deling door een attribuut: geen eindige grens); het speelde al met hele waarden. De documenten van de Drools-suite
`rational/one-third` en `rational/factor-at-the-boundary` (uit `drools-schrijfgrens`/`drools-exact-arithmetic`) delen door een
attribuut en blijven in clingo UNSAT met de zin van de onbegrensde vorm, voor en na. Geen pins veranderd.

### Aangepaste verwachtingen, pins en vloeren

Geen verwachting van een golden veranderd; geen pin of vloer veranderd (gate 6 suite 2320 checks, vloer 83; gate 9 250 checks,
was 241, vloer 201, 12 goldens; gate 5 2123 voorbeelden, vloer 489; gate 12 `EXPECTED_PLAYED` en `EXPECTED_CHANNEL` gelijk).

### Voorlopige besluiten

1. De voorstap draait in `Translate.translate` en in `Model.check` (niet in de runner), zodat elke aanroeper dezelfde
   declaratie ziet; de afleiding loopt daardoor twee keer per case, en is de identiteit in een document zonder de vorm.
2. Een grens 0 laat de declaratie staan (geheel gehouden, gelijkwaardig); alleen d > 0 wordt herschreven.
3. Een aandeel van een verdeling zonder afronding heeft geen eindige grens (de deling door de ontvangers is niet getypeerd);
   met afronding n heeft het aandeel n en de rest de ruimste van n en de pot.
4. Plaatsen zijn attributen en parameters; een structveld, payload of lijstelement van de vorm blijft geheel gehouden.
5. Het plugin-hek leest de grens van de rente met de regels van ch. 2.3 (`Ir_v0.Checks`), niet de term-decimalen van clingo.

### Wat niet lukte of open blijft

- Clingo rekent een deling in een gesloten body op max(sl, sr, schrijfschaal); een rente met grens 3 of 4 uit een deling
  (k/8) gaat door het plugin-hek en kan in clingo bij uitvoering met naam weigeren. Geen bron bereikt het.
- Een kale deling in een defaults-consequentie naar een begrensd doel rekent clingo nog op schrijfschaal 1 (ongewijzigd).
- `spec/IR-SPEC.md` r. 3783 (een meting van 2026-09-04) noemt nog "the pinned ch. 2.9 decimal scale"; buiten de
  "May touch", niet aangeraakt.

### make gates

Eén run op `427cd819f` groen (`out/gates/20261103-001916`); daarna `fee3b6065` en een tweede run, groen:

```
start 2026-11-03 00:27:20 fee3b6065 card/catala-decimal-unbounded
ocaml rc=0 1s ocaml-build: rebuilt (3 source path(s) newer than the stamp, e.g. backends/clingo/lib; 1s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 547 source files)
01-self-contained rc=0 1s
02-ir-schema rc=0 124s
04-case-schema rc=0 6s
03-verifier rc=0 37s
05-oracle rc=0 21s
06-clingo rc=0 258s
07-noop rc=0 12s
08-red rc=0 140s
09-catala rc=0 4s
10-regelspraak rc=0 223s
11-toka-parity rc=0 211s
12-drools rc=0 312s
13-reftype rc=0 270s
14-flogic rc=0 1s
15-graph rc=0 45s
16-scenario rc=0 40s
17-readings rc=0 236s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-03 00:32:33 313s red=0
```

**Landing (hoofdsessie, 2026-11-03).** Zelf nagemeten: `jutterswet-tariefrecht-catala` geeft met de clingo van `3d4676b88` op alle vier cases `TRANSLATION FAULT: default value scale …` en nu `clingo: PASS`, ref PASS. Ff op main `f3d4cff41`; `make gates` groen op `fee3b6065`, daarna alleen het Result. In dezelfde landing: CONSTRUCTS rij 2 (de Catala-route is uitgevoerd) en rij 10, en de verouderde zin in IR-SPEC (rond regel 3783) over de vaste Catala-schaal.
