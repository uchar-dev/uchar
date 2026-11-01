# unit-factors: een eenheid is een lijst factoren, zodat elke spelling die een eenheid noemt een eenheid is

*Op verzoek van de eigenaar op 2026-11-01: "bouw zodat RGS-360 weg kan". De maintainer vraagt hiermee dat de uitvoerders de
vormen dragen die het voorlopige besluit van die dag nog weigerde. Ontwerp: het Fable-advies hieronder, dat de bron van deze
kaart is. Elk punt van §6 is een voorlopig besluit dat landt, tenzij de eigenaar het terugdraait.*

Goal: `Number.unit` wordt een niet-lege lijst factoren met een gehele exponent, de algebra is die van een product van
factoren in ref, clingo en beide Drools-takken, en de frontend leest `m^2`, `m^-2`, `€/u/s`, `1/dg/m`, `€/dg^2` en `m^2/s`.
RGS-360, RGS-282, RGS-293 en RGS-294 verdwijnen; de spellingen die geen eenheid noemen (`m^0`, `m^1/2`, `2/dg`) krijgen RGS-361.

Acceptance: §10 hieronder. May touch: §11. Out of scope: §11. Stappen: §9, elk een eigen bouwer.

## Fable-advies

Gelezen: CLAUDE.md, WAY-OF-WORKING.md, `backlog/done/20261011-omgekeerde-eenheid.md`, IR-SPEC ch. 2.5 (punten 1–5,
WF-092, WF-123), RUNTIME-SEMANTICS ch. 2.4/2.6/2.10, CONSTRUCTS rij 26, PRIORITIZED-DEFAULTS-EVOLUTIE §8/Q9, het schema
(`Unit`, `DeclaredUnit`), `model/Unit.java`, `resolver/Typing.java`, `DefinitionResolver.unitOf`, `ref/lib/arith.ml`, de
eenheidsdelen van `translate.ml` en `normalise.ml`, `EffectiveUnit.java`, `UnitConflict.java`, `checks.ml` (WF-091, WF-092,
WF-123, `times_unit`, `quotient_unit`, `units_convertible`, `unit_identical`), `check-reftype-derivation.sh`, en de
spec-grammatica `regelspraak-toolchain/specification/v2.3.0/RegelSpraak-spec-for-ANTLR-v2.3.0.md` (r. 299–303, 392,
498–517). Alles hieronder is zelf gemeten op `e92fcb1f8`; de proef staat in §7, de bestanden in deze map
(`proof.diff`, `build-baseline.log`, `jar-build.log`).

## 0. Gekozen weg in één alinea

Een eenheid wordt op de wire een niet-lege lijst factoren: `"unit": [ {node: Unit, abbreviation, system?, canonical?,
factorToCanonical?, exponent?} , ... ]`. Een factor is de gedeclareerde eenheid zoals zij nu al voorkomt (dezelfde vier
velden, dezelfde knoop `Unit`), plus een geheel `exponent` ≠ 0 dat afwezig is als hij 1 is. `denominator` en de teller
`"1"` verdwijnen; `DeclaredUnit` (de declaratie in `UnitSystem.units`, met `spellings`) verandert niet. De algebra is
die van een product van factoren: vermenigvuldigen telt exponenten op, delen trekt ze af, omrekenbaar is gelijke
dimensie (dezelfde canonieke klassen met dezelfde exponenten), de factor is het product van de machten van de exacte
`factorToCanonical`. Het opake product (ch. 2.5 punt 4) vervalt. De frontend leest elke spelling die de grammatica nu al
parseert; de drie spellingen die geen eenheid zijn (`m^0`, `m^1/2`, `2/dg`) houden één code met een andere naam. RGS-360
("kan het Eenheid-model niet uitdrukken") verdwijnt, net als RGS-282, RGS-293 en RGS-294. Alle vier uitvoerders dragen
`m^2`, `m^-2`, `€/u/s`, `1/dg/m`, `€/dg^2` en `m^2/s`; clingo zonder factor ≠ 1 bij × en ÷, zoals nu.

## 1. Representatie en wire

### 1.1 Meting van de stand (`e92fcb1f8`)

| zone | bestanden met eenheden | gedeclareerde `Unit` | voorkomens (`Number.unit` + noemers) | rates (`denominator`) | teller `"1"` | product (`·`) |
|---|---|---|---|---|---|---|
| corpus/golden | 67 van 346 `ir.json` | 69 | 278 | 14 | 5 | 0 |
| corpus/negative | 155 | 235 | 768 | 87 | 31 | 1 (`dg·wk`, wf123-reciprocal-of-product) |
| corpus/red | 12 | 4 | 35 | 0 | 0 | 0 |
| toka/ | 30 (29 slices + dump) | 420 | 460 | 0 | 0 | 0 |
| ref/test/fixtures | 1 (`product-into-rate`) | 2 | 8 | 3 | 0 | 3 |

Geen `case*.json` draagt een eenheid (0). Geen golden draagt een product; de ene is een negatief geval. Eén
voorkomen heeft een noemer met een noemer (`wf123-reciprocal-of-rate`), diepte 3. De drooglooptest van de migratie
(§7.3): 261 bestanden met een voorkomen, 1446 `Number.unit`-posities, 103 noemers die in de lijst opgaan, 35 tellers
`"1"` die vervallen, 4 producten die in factoren uiteenvallen; 730 declaraties blijven byte-gelijk.

Lezers en schrijvers van de eenheidswire (grep op `abbreviation`/`denominator`, buiten corpus en toka):
`spec/verifier/lib/{ast,loader,checks}.ml` (ref leest via `Ir_v0`), `ref/lib/{arith,value,timeline,reading,world}.ml`,
`backends/clingo/lib/{normalise,translate}.ml`, `backends/drools/.../loader/IrLoader.java`,
`frontends/regelspraak/.../{model/Unit,emit/IrEmitter,resolver/DefinitionResolver,resolver/ScenarioResolver}.java`,
`frontends/catala/lib/emit.ml`; scripts: `check-ir-schema.sh` (zelftestrijen 712–726, 848–857, 914),
`check-reftype-derivation.sh` (`sister`, r. 415–435), `spec/proposals/v1-migratie/m3.py` (geschiedenis),
`spec/proposals/v1-naamtabel.json` (rijen `denominator`, `noemer`, `unit`). `frontends/flogic` schrijft geen eenheid.
De toolchain roept geen methode van `model.Unit` aan (grep op `.abbreviation()`, `rendering()`, `denominator()`,
`numerator()`, `Unit.compound`, `Unit.product` buiten de submodule: 0 treffers); zij volgt via de pin-bump.

Aantal plaatsen dat de vorm (`denominator`, `isReciprocal`, `isProduct`, `numerator`, `compound`, `product`,
`reciprocal`) leest: `checks.ml` 48, `loader.ml` 2, `arith.ml` 15, overige ref 12, `translate.ml` 16, `normalise.ml` 8,
`catala/emit.ml` 2, Java frontend 42, Java Drools 27, Java-tests 39, `test_ref.ml` 45, `test_clingo.ml` 12.

### 1.2 De opties

**(a) Een nieuw veld met factoren en exponenten, de wire migreert.** Twee subvormen:

- (a1) wikkelknoop: `"unit": {node: Unit, factors: [ {node: Factor, ...} ]}`. Dan is een gedeclareerde eenheid een andere
  knoop dan een factor, of `DeclaredUnit` moet ook mee (730 declaraties bewegen, 420 in toka).
- (a2) **lijst van de bestaande knoop**: `"unit": [ Unit, ... ]`, `Unit` krijgt `exponent`, verliest `denominator`. Een
  factor ís de gedeclareerde eenheid zoals zij nu al op een voorkomen staat (de vier velden gekopieerd), met een
  exponent; de declaratie blijft byte-gelijk. De migratie is: wikkel het voorkomen in een lijst, zet de noemer als tweede
  element met `exponent: -1` (recursief met wisselend teken), laat de teller `"1"` weg, splits een `·`-product in zijn
  factoren. Geen informatie wordt gereconstrueerd; het script faalt luid op een vorm die het niet kent.

**(b) De bestaande velden uitbreiden, geen migratie.** Houd `abbreviation`/`denominator`/`"1"` voor wat ze nu dragen en
voeg toe: `exponent` op een atomaire knoop, en voor een positie met twee verschillende factoren een `factors`-lijst naast
of in plaats van de `·`-string. Nul bestanden bewegen. Prijs: drie deelvormen (atoom, product, reciprook) en de
structuur twee keer gespeld (de `·`-string én `factors`), of `abbreviation` wordt optioneel; WF-123 blijft met zijn vier
armen; elke lezer moet alle deelvormen kennen; `product_factors`/`split("·")` blijven stringparsers van structuur. De
algebra moet hoe dan ook in alle uitvoerders gebouwd worden, dus (b) bespaart alleen het migratiescript en de
vormwijziging van de loaders, niet het werk aan de uitvoerders.

**(c) Alleen `exponent` toevoegen, één noemer houden.** Dekt `m^2`, `m^-2`, `€/dg^2`, `m^2/s`, niet `€/u/s` en `1/dg/m`,
die de eigenaar noemt. Afgevallen.

### 1.3 Keuze: (a2), de lijst van `Unit`-knopen

Redenen, in volgorde van gewicht:

1. Eén concept, één vorm. `m·s` en `s·m`, `€/dg` en `€·dg⁻¹`, `1/dg` en `dg^-1` zijn één lijst. WF-091 vergelijkt de
   lijst structureel (geen stringparser), WF-092 geldt per factor zonder wijziging, WF-123 vervalt met zijn vijf getuigen.
2. De declaraties bewegen niet (730 knopen, waarvan 420 in toka), en elk voorkomen van een gewone eenheid wordt
   alleen in een lijst gewikkeld: de L6-migratie is mechanisch en controleerbaar (§7.3), met de werkwijze van L1–L5
   (één script `m6.py` met gate 2's serialiser, `--all`/`--check`, idempotent; de casusdump her-emissie door de
   frontend is de toets van script ≡ emitter, gate 11 regressievorm; `v1-naamtabel.json` krijgt de rijen).
3. Exactheid: de factor van een lijst is ∏ `factorToCanonical`^exponent, een exacte rationaal; geen enkele rekenweg
   verandert, alleen de recursie over `denominator` wordt een vouw over de lijst.
4. Q9: de factorlijst is precies de eenheidshelft van optie A (een canonieke eenheid is een lijst over canonieke
   atomen zonder omrekenpaar). Deze kaart raakt Q9 niet (bron-eenheden blijven op de wire, geen waarde beweegt), en
   maakt de latere stap kleiner. Geen besluit over Q9 nodig.
5. Kosten van (a2) tegenover (b): dezelfde ±35 bronbestanden, plus één script en 261 datavormen die een script
   schrijft. Het verschil is klein in werk en groot in de spec.

### 1.4 De L6-vorm precies

Schema (voorstel; beschermd pad):

- `Number.unit`: `{"type": "array", "minItems": 1, "items": {"$ref": "#/$defs/Unit"}}`; afwezig is "geen eenheid".
- `Unit.properties` in deze volgorde: `node`, `abbreviation`, `system`, `canonical`, `factorToCanonical`, `exponent`
  (`{"type": "integer", "not": {"enum": [0, 1]}}`). `denominator` vervalt. `additionalProperties: false` blijft.
- `DeclaredUnit`: ongewijzigd (geen `exponent`; `spellings` na `factorToCanonical`).
- Voorbeelden: `€` → `[{€}]`; `€/jr` → `[{€}, {jr, "exponent": -1}]`; `1/dg` → `[{dg, "exponent": -1}]`; `m^2` →
  `[{m, "exponent": 2}]`; `€/u/s` → `[{€}, {s, -1}, {u, -1}]`; `m^2/s` → `[{m, 2}, {s, -1}]`; `%` → `[{%}]`.

**WF-124** (nieuw, vormwet op één eenheid, op elke positie die WF-092 bereikt): (a) de lijst is niet leeg; (b) een
`exponent` is nooit 0 en nooit 1 (1 is afwezig); (c) geen twee factoren met dezelfde `abbreviation` én hetzelfde `system`
(één factor per atoom; hun exponenten horen opgeteld); (d) de factoren staan in de canonieke volgorde: eerst de
factoren met een positieve exponent, dan die met een negatieve; binnen een teken op `abbreviation` in oplopende
Unicode-codepuntvolgorde, bij gelijke afkorting op `system` (afwezig vóór aanwezig); (e) een declaratie in
`UnitSystem.units` draagt geen `exponent`. Getuigen: `wf124-empty-unit`, `wf124-exponent-zero`,
`wf124-exponent-one-written`, `wf124-duplicate-factor`, `wf124-factors-unsorted`, `wf124-declared-exponent`; passend:
de nieuwe goldens (§9, C5) en elke bestaande.
Codepuntvolgorde is in de drie talen hetzelfde resultaat: OCaml `compare` op UTF-8 is bytevolgorde = codepuntvolgorde,
Python `sorted` is codepuntvolgorde, Java vergelijkt met `codePoints()` (niet `compareTo`, dat UTF-16-eenheden
vergelijkt; het verschil zit alleen bij supplementaire tekens, maar de regel moet exact zijn).

**WF-123**: retired, rij in de WF-index als geschiedenis (precedent WF-024, "Retired at L4"); de vijf `wf123-*`
getuigen gaan weg met reden in `corpus/negative/README.md` (precedent L4). Vloeren gate 3: `expect.txt` 390 ≥ 354, na
−5 +6 ≥ 391; parameterregels 642 ≥ 572; armen en sleutels groeien.

**WF-091**: `unit_identical` wordt lijstgelijkheid van factoren (afkorting, stelsel, canoniek, factor, exponent, in
volgorde); de parametertekst `render_unit` blijft voor exponent ±1 byte-gelijk ("… per 'jr' of system …") en schrijft
alleen bij |exponent| ≠ 1 een `^n`, zodat geen bestaande `expect.txt` beweegt (`wf091-times-unit` pint die tekst).

**WF-092, WF-034 (r)**: per factor, ongewijzigd.

**Rendering** in meldingen, parameters (`valueUnit`, `targetUnit`), ch. 5.12-tekst en `SessionDump`: de positieve
factoren in wire-volgorde met `·` verbonden, `^n` bij n ≠ 1, `1` als er geen is; dan per negatieve factor `/f` met
`^|n|` bij |n| ≠ 1. Bestaande vormen blijven byte-gelijk: `€/jr`, `1/dg`, `m`, `%`. Nieuw: `m^2`, `m^2/s`, `€/u/s`,
`€/dg^2`, `1/m^2`, `€·m/jr`.

## 2. Algebra

Definities. Een eenheid U is een eindige, niet-lege afbeelding atoom → exponent (geheel, ≠ 0), geschreven als de
gesorteerde lijst van §1.4. Een atoom is het record (abbreviation, system, canonical, factorToCanonical). De **klasse**
van een atoom is zijn `canonical` onder de ch. 2.1-identiteit (`norm`), of zijn `abbreviation` als het kaal is; de
**factor** f(a) is `factorToCanonical` als rationaal, 1 als het kaal is (de bestaande lezing: een kale eenheid is het
canonieke doel van elke eenheid die haar noemt, gemeten op `jutterswet-vergunningsduur`). Een half omrekenpaar is
WF-092 en in ref een fout, zoals nu. De **dimensie** D(U) is de afbeelding klasse → Σ exponent, zonder nullen. De
**totale factor** F(U) = ∏ f(a)^e(a).

Regels (ch. 2.6, vervangt punten 3–5 van ch. 2.5 en de maal/gedeeld-door-regels):

1. **Omrekenbaar**: U ~ V ⇔ D(U) = D(V). Factor U→V = F(U)/F(V), exact. Niet-omrekenbaar: fout (WF-010, `incompatible
   units`), zoals nu.
2. **Vermenigvuldigen** U × V (waarde v_U·v_V): begin met U; voor elke factor (b, e) van V: is b al een atoom van U, tel
   e op; is er anders een atoom a van U met dezelfde klasse als b, herreken de factor van V in a: waarde ×
   (f(b)/f(a))^e, en tel e op bij a; anders voeg (b, e) toe. Exponent 0 verdwijnt; een lege lijst is geen eenheid.
   Eenheidloos × U = U, waarde ongewijzigd.
3. **Delen** U ÷ V = U × V⁻¹ (alle exponenten van V genegeerd). Eenheidloos ÷ U = U⁻¹.
4. **Optellen/aftrekken**: U ~ V vereist; rechts herrekend in links; resultaat draagt links. Ongewijzigd.
5. **Vergelijken**: U ~ V vereist; beide kanten naar links herrekend. Ongewijzigd.
6. **Schrijven**: de waarde-eenheid ~ de gedeclareerde eenheid, herrekend; anders een fout. Ongewijzigd; de uitzondering
   voor een afgeleid product (ch. 2.10, Drools) vervalt.
7. **Afronden, begrenzen**: eenheid van de basis. **Percentage van**: de rechter eenheid. **Macht**: beide operanden
   eenheidloos (WF-035), ongewijzigd en buiten de kaart. **Decimalen**: Tabel 13 ongewijzigd.
8. **Tijdvouw** (§4.20, ch. 3.6): de integratie over de grid vermenigvuldigt met de grid-eenheid (regel 2), zodat `€/mnd`
   over maanden `€` is en `€/dg^2` over dagen `€/dg`; ref houdt de toets dat de negatieve factor de grid-eenheid is.

Canonieke volgorde: §1.4 (d). Daardoor zijn `m·s` en `s·m` één lijst, en de regel "rechts in links herrekenen" maakt de
spelling van het resultaat onafhankelijk van wat de spec bij "links" al voorschrijft (additief resultaat draagt links).

Absolute voorbeelden (worden `ref/test` rijen `WV-2.6-*`; `wk = 7 dg`, `cm = 1/100 m`, `jr = 12 mnd`):

| uitdrukking | waarde | eenheid |
|---|---|---|
| 2 m × 3 m | 6 | `m^2` |
| 6 `m^2` in een `m^2`-attribuut | 6 | `m^2` |
| 6 `m^2` in een `m^2/s`-attribuut | fout | `incompatible units: m^2 vs m^2/s` |
| 2 cm × 3 m | 600 | `cm^2` (3 m = 300 cm, regel 2) |
| 600 `cm^2` in een `m^2`-attribuut | 3/50 | `m^2` (factor 1/10000) |
| 6 `m^2` ÷ 2 m | 3 | `m` |
| 6 `m^2` ÷ 2 cm | 300 | `m` (1/cm = 100/m) |
| 1 ÷ 4 `m^2` | 1/4 | `1/m^2` |
| 100 `€/jr` × 6 mnd | 50 | `€` (6 mnd = 1/2 jr) |
| 30 `€/dg` × 1 wk | 210 | `€` |
| 2 `1/dg` × 3 wk | 42 | geen (bestaande rij) |
| 7 `1/wk` × 3 dg | 3 | geen (bestaande rij) |
| 3 dg ÷ 1 wk | 3/7 | geen |
| 5 `€/dg/m` × 2 dg × 3 m | 30 | `€` |
| 1 `€/jr` × 1 `kg/jr` | 1 | `€·kg/jr^2` |
| 1 `€/jr` × 1 m | 1 | `€·m/jr` (17c vervalt) |
| 6 € ÷ 2 `1/n` | 3 | `€·n` (RGS-294 vervalt) |
| 1 ÷ 2 `€/dg` | 1/2 | `dg/€` (bestaande rij) |
| 2 `m^2` = 20000 `cm^2` | waar | — |

Het opake product is niet meer nodig: elke factor draagt zijn omrekenpaar, dus een product is omrekenbaar of niet, nooit
"alleen identiek". Weg: ch. 2.5 punt 4, `Value.Product` (ref), `Unit.product` en `isProduct` (Java), `is_product`,
`product_factors` (verifier), `DERIVED_PRODUCTS`/`isDerivedProduct`/`countDerivedProducts` (EffectiveUnit), de
vrijstelling in `UnitConflict`, de alinea "fourth MAAL outcome" van ch. 2.10, RGS-282/293/294, en de restpost `passend`
(`m × m` in een gedeclareerde `m·m`: wordt `m^2` in `m^2`, identiek, speelt in alle vier; `m × m` in `m^2/s` is overal
een fout, §7).

Besluit 17c (`€/jr × m` is een luide fout) wordt vervangen: de vorm `€·m/jr` is een eenheid. RGS-294 (`€ ÷ 1/n`, `€ ÷
€/jr` → `€·jr/€`… = `jr` na kanselering van de klasse €): afgeleid, geen fout. Het spoor in CONSTRUCTS rij 26 vermeldt
beide.

## 3. Clingo

Wat de gehele codering vandaag doet (`translate.ml` 2839–3041, ASP-TRANSLATION S6): multiplicatieve operanden
eenheidsvrij, behalve eenheidloos × E (adopteren), E⁻¹ × E' met een gehele factor (vermenigvuldig de term), eenheidloos ÷
E (reciprook); additieve joins en schrijfgrenzen alleen bij `same_quantity` (factor exact 1); vergelijkingen identiek
of twee reciproken met p/q (beide kanten vermenigvuldigd, bereikhek). Een niet-gehele factor is een weigering met naam.

Wat de algebra van §2 daaraan toevoegt, zonder de codering te veranderen:

- `eff_unit` (Times, Divided_by): de eenheid van regel 2/3, en daarnaast de **factor** van de herrekening als rationaal
  (`Ir_v0.Units.times`/`divide` geven `(unit option, Q.t)`). Factor 1 betekent: het geheeltallige product of quotiënt is
  al juist, alleen de eenheid verandert. Dit dekt `m × m` (`m^2`), `m^2 ÷ m`, `€ ÷ jr`, `€ ÷ €`, `€/dg/m × dg × m`.
- `mul_units_ok` → factor 1: toegelaten; gehele factor k: de operand van de rechterkant maal k, zoals `reciprocal_factor`
  nu (`€/dg × wk`: ×7), hernoemd tot `product_factor ctx l r : (side, int) option`; niet-gehele factor: weigering met
  naam, ongewijzigd. `provably_unitless` leest de Times-arm via dezelfde functie (geen eenheid over = bewezen vrij).
- `quotient_units_ok` → factor 1 toegelaten (de nieuwe vormen hebben er geen andere nodig); een gehele factor op de deler
  (`dg ÷ wk`: deler ×7) is codeerbaar maar niet nodig: buiten de kaart, met naam in S19.
- `comparison_factors` → elke omrekenbare paar met factor p/q: x·p tegen y·q (algemeen in plaats van alleen reciproken);
  dekt `m^2` tegen `cm^2` (×10000, bereikhek `max_int31 / k` blijft).
- `units_identical_ok`/`Normalise.same_quantity`: D gelijk en F(U)/F(V) = 1; `Normalise.plain_units` en `is_pure_pct`:
  één factor met exponent 1; `restates_into`/`unit_named` per factor.
- De grid-strip (`translate.ml` 1057, 2433): de negatieve factor van de grid-klasse, één macht.

Niet in de kaart: `1/wk × dg` (factor 1/7) blijft "the factor 1/7 … is no whole number" (getuige in `test_clingo.ml`
r. 16165); een additieve join of schrijfgrens met factor ≠ 1 blijft `non-identical units`. De kaart kan zonder: de
negen vormen van RGS-360 zijn declaraties en vragen in een golden alleen factor 1 (identieke atomen). Gate 6 heeft geen
weigerlijst, dus de goldens van C5 worden zo geschreven (`m × m` in `m^2`, `€/dg/m × dg × m` in `€`, vergelijking in
dezelfde eenheid of met gehele factor). Onhaalbaar in de gehele codering blijft wat een deling door een constante
vraagt die niet opgaat; dat is de bestaande S19-regel en verandert niet.

Concreet bewezen (§7): met twee armen (eenheid van het product in `eff_unit`, toelating in `mul_units_ok`) speelt clingo
`vierkant.ir.json` en `passend.ir.json` (`PASS (1 expectations)`) en weigert `passend-per-s.ir.json` bij de schrijfgrens.

## 4. Drools en ref

**ref** (`ref/lib`): `Value.unit` wordt `Unitless | Unit of measure_unit` (de lijst), `Product` weg. `Arith.conv_factor`
= F(a)/F(b) na de dimensietoets; `Arith.mul`/`div_gen` via `Ir_v0.Units.times`/`divide` (eenheid + factor op de
waarde); `restate_into`, `equal`, `compare_op`, `join_units`, `world.ml:116` ongewijzigd in vorm. `timeline.ml:598`
(grid-kanselering) en `:611` (Product-arm) via regel 8; `reading.ml:808` (`unit_text`) de rendering van §1.4.
`ref/test`: de rijen `WV-2.6-*` naar de tabel van §2 (de twee rijen van de vorige kaart over `m·m` keren om: `2 m × 3 m`
in een gedeclareerde `m^2` is 6, in `m^2/s` een fout; die wijziging wordt in de Result genoemd), de fixture
`product-into-rate` migreert mee (de `m·m`-afkortingen worden `[{m, 2}]`).

**Verifier** (`spec/verifier/lib`): nieuwe module `units.ml` (`Ir_v0.Units`: `times`, `divide`, `convertible`,
`factor`, `render`, `canonical_order`) die `checks.ml`, ref en clingo delen, zoals `units_convertible` nu al;
`check_unit_form` per factor + WF-124 op de lijst; `wf092` de declaratiearm (e); `times_unit`/`quotient_unit` claimen
altijd (de verifier kruist nu ook een product, de restpost van de vorige kaart sluit); `unit_identical`, `render_unit`,
`unit_spelling` op de lijst; `is_reciprocal`, `is_product`, `product_factors`, `numerator_unit`, `reciprocal_unit` weg.
Gate 1: ref en verifier delen al code met elkaar; met de Drools-runtime delen ze niets, ongewijzigd.

**Drools** (`backends/drools`): `IrLoader.unit` leest de lijst (per factor het exacte paar in `UnitSystem.exactFactors`
zoals nu per declaratie); `EffectiveUnit.timesUnit`/`quotientUnit` → `Unit.times`/`divide` uit het frontendmodel (één
auteur voor frontend en Drools, waar nu `timesUnit` "de Java-tweeling van `Typing.compoundUnit`" is), `afterTimeFolding`
via regel 8 (de negatieve factor van de grid één macht omhoog; Drools kent geen grid→eenheid-tabel en neemt de ene
negatieve factor; bij geen of meerdere ongewijzigd, waarna de schrijfgrens faalt); `DERIVED_PRODUCTS` en
`UnitConflict`'s vrijstelling weg: `requireRescalable` is "omrekenbaar of fout". `TmsTranspiler.intoDenominator`
(r. 4414) wordt "de factor van `Unit.times` als `Term.Restate` op de rechter operand"; `restated` (r. 4559)
ongewijzigd in vorm; `Unit.exactRestateFactors` levert ∏ num^e / ∏ den^e als `BigInteger`-machten (exact; de
voorafgeronde `conversionFactorTo` blijft de restpost van rij 26 waar geen exact paar is). `TEMPORAL_TOTAL_RATE_UNIT`
en `RATE_DENOMINATOR_NOT_GRID` (r. 5458–5489) lezen "de ene factor met exponent −1"; `TimeCalculation.chronoUnit(Unit)`
antwoordt alleen op één factor met exponent 1. Beide takken renderen dezelfde analyse: één plan, geen eigen lijst (de
regel van `drl-teksttak-op-truth-maintenance`). `SessionDump:519` rendert §1.4, byte-gelijk aan ref's `unit_text`
(gate 17 vergelijkt de regels letterlijk). Getuigen: `a_derived_product_against_a_rate_is_a_fault` blijft (nu op
omrekenbaarheid), nieuw `a_product_into_its_declared_square_plays`, `a_rate_times_a_convertible_unit_applies_the_factor`
(`€/dg × wk` = 210, `like-ref/`), `TmsLikeRefWitnessTest.COUNT` 28 → 30.

**Restpost `passend`**: `m × m` in een gedeclareerde `m·m` wordt `m^2` in `m^2`, identiek: speelt in alle vier (§7:
ref PASS, clingo PASS, Drools PLAYED op beide takken). `passend-per-s` (`m^2` in `m^2/s`): fout in alle vier (§7, op een
verse jar; de jar in `backends/drools/target` van main is ouder dan `f3a480135` en speelde het nog).

**Gate 13** (`check-reftype-derivation.sh`): de twist blijft "een zustereenheid op elke afgeleide annotatie". `sister`
twist de eerste factor met een omrekenpaar (afkorting met achtervoegsel, factor ×10^k); de `"1"`-arm vervalt. Een
exponent e vermenigvuldigt F met 10^(k·e), nooit 1. De twistklassen, `DERIVED_OP`, de vierde klasse en de vloeren
(`EXPECTED_MIN_TWISTS=38`) veranderen niet; de nieuwe goldens tellen erbij. De vocabulaire-, herkomst- en naamlegs
raken geen eenheid.

## 5. Wat RGS-360 overhoudt

De grammatica van de frontend (`RegelSpraak.g4` r. 518–523) parseert nu al elke vorm uit de lijst van de eigenaar; alleen
`DefinitionResolver.normalForm` weigert. Na deze kaart leest het de spelling `A^a/B^b/…` en `1/B^b/…` in de lijst, met
`^1` toegestaan en genormaliseerd.

| spelling | uitkomst | reden |
|---|---|---|
| `m^2`, `m^-2`, `€/u/s`, `1/dg/m`, `€/dg^2`, `m^2/s`, `1/dg^2` | een eenheid | §1.4 |
| `m^0` | fout met naam | een exponent 0 noemt geen factor; stil "geen eenheid" is een tweede spelling van eenheidloos (fail fast) |
| `m^1/2` | fout met naam | de exponent is geen geheel getal (de spec: `<exponent>` is een `<geheelgetal>`; het NUMBER-token lext ook `1/2`) |
| `2/dg` | fout met naam | alleen `1` staat over een noemer; `2` is een getal, geen factor |

Eén code blijft, met een andere naam en zin, want deze drie spellingen zijn geen eenheid in welk model dan ook:
**`RGS-361 unit-spelling-is-no-unit`**, parameters `spelling`, `part` (drie varianttemplates `.exponent-zero`,
`.exponent-not-whole`, `.numerator-not-one`, het patroon van `Coded.variant`). RGS-360 vervalt (de zin "kan het
Eenheid-model niet uitdrukken" bestaat niet meer; het register eist geen aaneengesloten reeks, RGS-359 ontbreekt al;
356 codes tegen vloer 347, na −4 (282, 293, 294, 360) +1 = 353). **"RGS-360 weg" is haalbaar** in de betekenis van de
eigenaar: er is geen eenheid meer die het model niet kan uitdrukken; wat overblijft is een spelling die geen eenheid
noemt. Als de eigenaar ook die als syntaxfout wil, kan dat niet via de grammatica zonder het NUMBER-token te splitsen
(een impliciet `'1'`-literal-token in ANTLR krijgt voorrang boven NUMBER voor elke `1` in de taal); de resolvercode is
de juiste plaats.

## 6. Normvragen met voorlopig besluit

Elk hieronder is een voorlopig besluit dat landt, tenzij de eigenaar het terugdraait.

1. **Omrekenbaarheid van samengestelde eenheden is dimensiegelijkheid** (§2 regel 1): `cm·m` ~ `m^2` met factor 1/100;
   producten zijn niet langer opaak, ch. 2.5 punt 4 vervalt. Aanbeveling: ja. Het alternatief (alleen identieke
   producten) laat `cm × cm` in `m^2` een fout zijn terwijl elk atoom zijn factor draagt.
2. **Bij × en ÷ herrekent een rechter factor in de linker factor van dezelfde klasse** (§2 regel 2): `cm × m` = `cm^2`,
   waarde ×100; `€/jr × mnd` = `€`, waarde /12. Aanbeveling: ja; het is de bestaande regel "restated into the denominator
   first" en "het resultaat draagt links", nu uniform. Het alternatief (nooit herrekenen bij ×, alles bij de consument)
   geeft dezelfde eindwaarden maar laat `wk/dg` als eenheid van een eenheidloze grootheid staan en verandert de
   WF-091-afleiding van `jutterswet-vondsttempo`.
3. **Spellingen in `met eenheid`**, gelezen in de spec-grammatica (geen tekst overgenomen): de spec kent `E^n` met n een
   geheel getal (het token laat een minteken toe, dus ook negatief, en ook 0), `E^n/F^m` met één slash, en `1` alleen; zij
   kent niet `1/E`, niet meer dan één slash en geen productspelling (`€·m`). (a) uchar aanvaardt daarbovenop `1/E^n/…`
   (bestaat) en meer dan één slash (`€/u/s`, `1/dg/m`): uitbreiding in `grammar/DIVERGENCES.md` bij "An inverse unit".
   Aanbeveling: ja, de eigenaar noemt beide. (b) Geen productspelling in deze kaart: een product van twee verschillende
   atomen (`€·m`) is alleen een tussenresultaat; een attribuut erin is niet declareerbaar. Aanbeveling: niet nu, met
   naam in de Result. (c) `1` alleen blijft een syntaxfout in uchar. Aanbeveling: laten, noteren in DIVERGENCES.
4. **`m^0` is een fout, geen eenheidloos attribuut.** Aanbeveling: fout (§5).
5. **`m^1` is dezelfde eenheid als `m`** en wordt op de wire nooit met `exponent: 1` geschreven (WF-124 (b)).
6. **Besluit 17c vervalt; RGS-282, RGS-293, RGS-294 vervallen**: `€/jr × m`, `€/jr × kg/jr`, `€ ÷ 1/n`, `€ ÷ €/jr` en
   eenheidloos ÷ product zijn eenheden. Aanbeveling: ja; "cannot spell the unit is never a wire type" blijft als
   principe waar en heeft geen geval meer.
7. **WF-123 retired, WF-124 nieuw**, vijf getuigen verwijderd met reden. Aanbeveling: ja (precedent WF-024/WF-048).
8. **Rendering** (§1.4): één `/` per negatieve factor, `^n` bij |n| ≠ 1, bestaande vormen byte-gelijk. Geldt voor
   diagnostiekparameters, de verifierzin, ch. 5.12 en `SessionDump`. Aanbeveling: ja.
9. **Canonieke volgorde** (§1.4 (d)): tekenvolgorde, dan codepuntvolgorde op afkorting, dan stelsel. Aanbeveling: ja.
10. **Q9**: geen besluit; rij 26 krijgt de zin dat de factorlijst de eenheidshelft van optie A is en de bron-eenheden op
    de wire blijven.
11. **Clingo**: een niet-gehele factor bij ×, ÷, + of schrijven blijft een weigering met naam; geen golden met zo'n
    vorm; de gehele factor bij ÷ blijft buiten de kaart (S19). Aanbeveling: ja.
12. **Drools-exactheid**: machten op het exacte paar (`BigInteger.pow`); de voorafgeronde factor blijft de restpost van
    rij 26 waar hij al stond. Aanbeveling: ja.
13. **Tijdvouw van `€/dg^2` over dagen is `€/dg`** (regel 8). Aanbeveling: ja; ref houdt de grid-toets, Drools neemt de
    ene negatieve factor.

## 7. De proef

Kopie: `git worktree add --detach werk/unit-factors/fable/wt` op `e92fcb1f8`; OCaml gebouwd (`build-baseline.log`),
Drools-jar gebouwd met `mvn -o -q -f backends/drools/pom.xml package -DskipTests` (`jar-build.log`, exit 0). Documenten:
`werk/omgekeerde-eenheid/fable/{vierkant,passend,passend-per-s}.ir.json` met `case.json` (2 m × 3 m = 6; `vierkant`
en `passend` vergelijken het product met het attribuut in beide volgordes; `passend-per-s` declareert `m·m/s`).

**7.1 Nulmeting** (ongewijzigde bouwsels): verifier `PASS`; ref `EVALUATION FAULT: incompatible units: m·m vs m*m`;
clingo `TRANSLATION FAULT: non-identical units under the write to "oppervlakte"`; Drools op de verse jar `PLAYED 1`
op beide takken voor `vierkant` en `passend`, en `ERROR UnitConflict … 'm·m' … 'm·m/s'` op beide takken voor
`passend-per-s`.

**7.2 Mutatie** (`proof.diff`, 53 regels, twee bestanden): ref `Arith.mul` maakt van twee gewone eenheden een
`Unit`-record (gesorteerde `·`-join, kaal) in plaats van `Product`; clingo `eff_unit` (Times) geeft datzelfde record en
`mul_units_ok` laat twee gewone eenheden toe (factor 1). Gebouwd en opnieuw gespeeld:

| document | ref | clingo | Drools exec | Drools DRL |
|---|---|---|---|---|
| vierkant | `oracle: PASS (1 expectations)` | `clingo: PASS (1 expectations)` | `PLAYED 1` | `PLAYED 1` |
| passend | `oracle: PASS (1 expectations)` | `clingo: PASS (1 expectations)` | `PLAYED 1` | `PLAYED 1` |
| passend-per-s | `incompatible units: m·m/s vs m·m` | `non-identical units under the write` | `ERROR UnitConflict` | `ERROR UnitConflict` |

Wat dit aantoont: (i) de gehele codering van clingo draagt een product van twee eenheden zonder factor en zonder
wijziging van de termen; alleen de eenheidsanalyse en de toelating veranderen, 6 regels; (ii) ref speelt zodra een
product een eenheidsrecord is en geen eigen constructor, 12 regels; (iii) de verifier was al schoon; (iv) het vierkant
sluit op alle drie documenten, ook op de fout. Dat is het riskantste stuk van de gekozen weg (de eigenaar: "clingo
weigert elk product van twee eenheden"). Het is een proef op de oude wire; de lijstvorm maakt dezelfde armen
eenvoudiger (geen `·`-join, de factor komt uit `Units.times`).

**7.3 Drooglooptest van de migratie** (Python, geen bestand geschreven): over corpus, toka en `ref/test/fixtures`: 261
bestanden met een voorkomen; 1446 `Number.unit`-posities gewikkeld; 103 noemers als factor met exponent −1 (één
geneste, diepte 3, correct met tekenwissel); 35 tellers `"1"` weggelaten; 4 `·`-producten gesplitst; 730 declaraties
onaangeroerd. De regel van het script is tien regels; niets wordt gereconstrueerd.

Opruiming: de worktree is verwijderd met `git worktree remove --force`; `proof.diff`, de twee logs en dit advies staan
in `werk/unit-factors/fable/`.

## 8. Bewaking van wat niet mag veranderen

1. Geen waarde beweegt: gates 5, 6, 12 en 17 geven op elk bestaand golden dezelfde uitkomst; `EXPECTED_PLAYED` blijft
   296 tot C5 en wordt dan met precies het aantal nieuwe goldens verhoogd; de `like-ref`-getuigen blijven.
2. De 730 declaraties (`UnitSystem.units`) zijn byte-gelijk vóór en na L6 (toets in `m6.py --check`).
3. De rendering van elke bestaande vorm is byte-gelijk (`€/jr`, `1/dg`, `%`, `m`): geen `expect.txt` van `corpus/negative`
   beweegt behalve de vijf verwijderde, geen `expectedFault.params` van `corpus/red` beweegt, geen regel van gate 17.
4. Gates 10, 11 en 16 blijven byte-identiek: elke golden met `source.rgs` en de casusdump worden her-geëmitteerd en zijn
   gelijk aan de migratie door `m6.py` (de L3-toets script ≡ emitter); `toka/README.md` krijgt de L6-regel.
5. Gate 13 blijft groen zonder overlevende: geen uitvoerder leest de annotatie; de vloeren veranderen niet.
6. Q9 blijft open; bron-eenheden blijven op de wire; geen case-bestand beweegt (0 dragen een eenheid).
7. WF-092 per factor ongewijzigd; `jutterswet-vondsttempo` blijft de passende golden van de reciprook.
8. Gate 1: `ref/` en `spec/verifier/` delen niets met de Drools-runtime; de gedeelde Java-algebra staat in
   `frontends/regelspraak/.../model/Unit.java`, niet in `backends/drools/runtime/`.
9. Geen gewijzigde test wordt verzwakt: de twee omgekeerde rijen (`2m-times-3m …`) en de omgekeerde
   `UnitIdentityTest`-gevallen worden in de Result met naam genoemd als gedragswijziging van deze kaart.
10. De gehele codering van clingo krijgt geen nieuwe deling: elke toelating is factor 1 of een gehele vermenigvuldiging.

## 9. Stappenplan in commits (elk groen; gates per stap)

C1 **claim**: kaart naar `doing/`.

C2 **L6: de factorlijst op de wire** — één commit, want loaders, emitters, schema en corpus kunnen niet los van elkaar
groen zijn (een nieuw corpus op oude loaders maakt gates 2–17 rood; nieuwe loaders op het oude corpus evenzo). Inhoud:
schema; `spec/proposals/v1-migratie/m6.py` (met `--all`/`--check`, gate 2's serialiser, luid op elke onbekende vorm);
corpus (golden 67, negative 155, red 12), toka-slices 29 + dump via her-emissie, `ref/test/fixtures/product-into-rate`;
OCaml: `ast.ml`, `loader.ml`, `units.ml` (nieuw, alleen vorm en rendering), `checks.ml` (WF-124, WF-123 retired,
`unit_identical`, `render_unit`, `unit_spelling`, de grid-strip), ref (`value.ml` houdt `Product` nog, `arith.ml`
vormwijziging, `timeline.ml`, `reading.ml`), clingo (`normalise.ml`, `translate.ml` vorm), catala `emit.ml`; Java:
`Unit.java` (atomen met exponent; dezelfde semantiek als nu, met hulpmethoden voor "de ene negatieve factor"),
`IrEmitter`, `IrLoader`, `DefinitionResolver`/`ExpressionBuilder`/`ScenarioResolver`/`RuleChecker`/`Datatype`/
`TimeCalculation`/`Typing`, `EffectiveUnit`/`UnitConflict`/`TmsTranspiler`/`SessionDump` (vorm); scripts:
`check-ir-schema.sh` (zelftestrijen), `check-reftype-derivation.sh` (`sister`); negatieven: −5 `wf123-*` (README),
+6 `wf124-*`; IR-SPEC ch. 2.4 (`unit: [Unit]`), 2.5 (veldtabel, "wire form", punt 5, WF-123/124), 4.6-lijst, WF-index;
`v1-naamtabel.json` (`denominator` geland in de nieuwe M-stap, `exponent` nieuw; `m5.py`-toets telt mee);
`toka/README.md`. Gates: alle 17 groen; geen waarde beweegt (§8.1); 10/11/16 byte-identiek. De builder mag op de branch
in kleinere WIP-commits werken en vóór de landing samenvoegen tot deze ene; de branch is tussen de WIP-commits niet
groen en dat staat erbij.

C3 **de algebra** — `Ir_v0.Units.times/divide/convertible/factor`; ref `mul`/`div_gen`/`conv_factor`, `Value.Product`
weg; verifier `times_unit`/`quotient_unit` claimen altijd; clingo `eff_unit`, `product_factor` (uit `reciprocal_factor`),
`mul_units_ok`, `quotient_units_ok`, `comparison_factors`, `same_quantity`, S6/S19-tekst; Java `Unit.times/divide/
convertibleTo/exactRestateFactors` (machten), `Typing.compoundUnit` → `Unit.times/divide`, RGS-282/293/294 uit register
en catalogi, `EffectiveUnit` via `Unit`, `DERIVED_PRODUCTS` en de `UnitConflict`-vrijstelling weg, `intoDenominator`
via de factor; tests: `WV-2.6-*` naar de tabel van §2, `test_clingo` (`m × m` in `m^2` vertaalt; `cm × m` in `m^2`
weigert met naam; `€/dg × wk` ×7), Drools-getuigen (§4), `TypingTest`/`UnitIdentityTest` op producten; RUNTIME-SEMANTICS
ch. 2.6 (regels + tabel), ch. 2.10 (vierde MAAL-uitkomst en "quotient zonder normaalvorm" weg). Gates: alle 17 groen;
corpuswaarden ongewijzigd (geen golden draagt een product); `jutterswet-vondsttempo` speelt als voorheen.

C4 **de spellingen in de frontend** — `DefinitionResolver.normalForm` leest `A^a/B^b/…` en `1/…` in de lijst; RGS-360 →
RGS-361 (§5) in register en beide catalogi; `UnitIdentityTest`: de negen spellingen schoon met hun wire-lijst, drie
geweigerd met `RGS-361`; `grammar/DIVERGENCES.md`. Gates 10, 11, 16 groen en byte-identiek (toka draagt geen van de
vormen); gate 3 ongewijzigd.

C5 **goldens en negatieven** — `corpus/golden/jutterswet-oppervlak` (`source.rgs`, `scenario.rgs` zodat gate 16 de cases
pint, `NOTE.md` met handberekening: `lengte maal breedte` in `m^2`, `oppervlakte gedeeld door lengte` in `m`, een
vergelijking met een drempel in `m^2`, een kenmerk) en `corpus/golden/jutterswet-sleeptarief` (`sleeptarief` in
`€/dg/m`, `sleepkosten = sleeptarief maal sleepdagen maal sleeplengte` in `€`, een `1/dg^2`- of `€/dg^2`-attribuut uit
`tarief gedeeld door dagen`); beide zonder factor ≠ 1 (§3); negatieven `wf091-product-exponent` (een `times` met een
verkeerde exponent in de annotatie), `wf023-square-into-length` (`m^2` in een `m`-doel). Gates: 2–8 groen; 12
`EXPECTED_PLAYED` 296 → 298; 13 twists ≥ 38 (meer); 16 vloeren +1/+n; 17 leest beide; alle 17 groen.

C6 **kaart naar `done/` met Result** (proposed tekst voor CONSTRUCTS rij 26 en het spoor 17b/17c erin); de hoofdsessie
landt rij 26 serieel. Daarna, buiten de kaart: pin-bump van de toolchain.

## 10. Acceptatie

- `frontends/regelspraak` `--ir` op een bron met `met eenheid m^2`, `m^-2`, `€/u/s`, `1/dg/m`, `€/dg^2`, `m^2/s`,
  `dg^-1`, `1/dg`, `€/jr`: schoon; de wire draagt de lijsten van §1.4 in canonieke volgorde; `m^1` schrijft geen
  `exponent`. `m^0`, `m^1/2`, `2/dg`: `RGS-361` met `spelling` en `part`. Geen `RGS-360`, `RGS-282`, `RGS-293`,
  `RGS-294` in het register of een catalogus.
- `python3 spec/proposals/v1-migratie/m6.py --check` meldt niets; een tweede `--all` verandert geen byte; de 730
  declaraties zijn byte-gelijk aan `e92fcb1f8`.
- `verifier` weigert elk `wf124-*` met de sleutel van zijn `expect.txt`; `wf123-*` bestaat niet meer en staat in
  `corpus/negative/README.md` met reden; `jutterswet-vondsttempo` en de twee nieuwe goldens zijn verifier-schoon.
- ref, clingo en beide Drools-takken: `jutterswet-oppervlak` en `jutterswet-sleeptarief` `PASS`/`PASS`/`PLAYED n`
  met de handwaarden van `NOTE.md`; `passend-per-s`-vorm (`m^2` in `m^2/s`) faalt in alle vier als eenheidsfout.
- `ref/test`: de tabel van §2 letterlijk als `WV-2.6-*`-rijen, alle groen; `test_clingo`: de drie getuigen van §3.
- `make gates`: 17 groen; `EXPECTED_PLAYED` = 296 + het aantal nieuwe goldens; gates 10, 11, 16 byte-identiek; gate 13
  zonder overlevende; gate 17 zonder afwijking; geen vloer verlaagd.
- Result noemt: de omgekeerde rijen en tests (§8.9), de verwijderde getuigen, de voorgestelde tekst voor rij 26, de
  open punten (§6.3b, §6.11, de voorafgeronde Drools-factor).

## 11. May touch

`spec/schema/uchar-ir-v1.schema.json`, `spec/IR-SPEC.md` (ch. 2.4, 2.5, 4.6, WF-index), `spec/RUNTIME-SEMANTICS.md`
(ch. 2.6, 2.10, 5.12 rendering), `spec/ASP-TRANSLATION.md` (S6, S19), `spec/verifier/**`, `spec/proposals/v1-migratie/m6.py`
(nieuw), `spec/proposals/v1-naamtabel.json`, `ref/**`, `backends/clingo/**`, `backends/drools/**` (loader, core, tms,
cases, tests; niet `runtime/`), `frontends/regelspraak/**` (model, resolver, emit, tests, `grammar/DIVERGENCES.md`,
register en catalogi; de grammatica zelf hoeft niet), `frontends/catala/lib/emit.ml`, `corpus/golden/**/ir.json`
(migratie), `corpus/negative/**` (migratie, −5, +8), `corpus/red/**/ir.json` (migratie), `corpus/golden/jutterswet-oppervlak/`,
`corpus/golden/jutterswet-sleeptarief/`, `corpus/negative/README.md`, `toka/**/ir.json` en `toka/README.md`,
`scripts/check-ir-schema.sh`, `scripts/check-reftype-derivation.sh`, `scripts/check-drools.sh` (`EXPECTED_PLAYED`),
kaartverplaatsing. Beschermde paden als voorstel op de branch; `spec/CONSTRUCTS.md` via de Result.

Out of scope: een productspelling in `met eenheid` (`€·m`), `1` alleen, een benoemde afgeleide eenheid (`Hz = 1/s`),
een niet-gehele factor in clingo bij ×/÷/+/schrijven en een gehele factor bij ÷, Q9 (canonieke-eenheid-wire), de
voorafgeronde Drools-factor (rij 26), `tot de macht` met een eenheid (WF-035), de toolchain-pin.

## 12. Omvang, gemeten

Data: 261 bestanden door het script (67 golden, 155 negative, 12 red, 30 toka, 1 ref-fixture); −5 +8 negatieven;
2 goldens nieuw. Bron: OCaml 9 bestanden (`ast`, `loader`, `checks`, `units` nieuw, `arith`, `value`, `timeline`,
`reading`, `normalise`, `translate`, `catala/emit`), Java frontend 10 (`Unit`, `Datatype`, `TimeCalculation`, `Typing`,
`DefinitionResolver`, `ExpressionBuilder`, `ScenarioResolver`, `RuleChecker`, `IrEmitter`, register+2 catalogi), Java
Drools 6 (`IrLoader`, `EffectiveUnit`, `UnitConflict`, `TmsTranspiler`, `SessionDump`, catalogi), tests 6
(`test_ref`, `test_clingo`, `UnitIdentityTest`, `TypingTest`, `TmsLikeRefWitnessTest`, `EffectiveUnitWitnessTest`).
Scripts 3 + 1 nieuw. Spec 4 documenten + schema + naamtabel + README's 2. Totaal ±45 bronbestanden, 1 script, 4
spec-documenten, 261 gemigreerde datadocumenten.

## Stand

### Stap 1 (C1 en C2), 2026-11-01

Commits op `card/unit-factors`: `756964241` claim, `7d3937601` `L6: de factorlijst op de wire`. De algebra (C3) en de
spellingen (C4) zijn niet gedaan: elke vorm die RGS-360 weigerde blijft geweigerd, RGS-282/293/294 bestaan nog, en
`Value.Product`, `Unit.product`, `DERIVED_PRODUCTS` en de `UnitConflict`-vrijstelling staan er nog.

**Gates.** `make gates`: 17 groen, `after-run` groen, `red=0`, 332 s (`werk/unit-factors/logs/stap1-gates-4.log`). `EXPECTED_PLAYED`
296 op beide takken. Geen `EXPECTED_`-regel in `scripts/` gewijzigd. Gate 3: 548 negatief geweigerd, `expect.txt` 391 (vloer 354).
Gate 10: 1334 tests, 157 goldens byte-gelijk. Gate 12: JUnit 1718. Gate 6: 2242 checks, 964 cases. Gate 5: 1723 voorbeelden.
Gate 13: 124 annotaties met eenheid, 50 twistbaar, 74 vrijgesteld, 0 zonder zuster.

**Migratie, gemeten** (`m6.py` over `e92fcb1f8`, per zone, zonder de vier `wf123-*` die het weigert):

| zone | bestanden | posities | noemers | tellers weg | producten | `exponent` geschreven | declaraties |
|---|---|---|---|---|---|---|---|
| corpus/golden | 67 van 346 | 264 | 14 | 5 | 0 | 14 | 69 |
| corpus/negative | 147 van 547 | 654 | 66 | 10 | 1 | 67 | 234 |
| corpus/red | 12 van 50 | 35 | 0 | 0 | 0 | 0 | 4 |
| toka | 30 van 30 | 460 | 0 | 0 | 0 | 0 | 420 |
| ref/test/fixtures | 1 | 5 | 3 | 0 | 3 | 6 | 2 |
| backends/drools/src/test/resources | 6 van 42 | 14 | 1 | 0 | 0 | 1 | 9 |

Na de verwijdering van de vijf `wf123-*`: 262 gemigreerd, 1425 posities, 79 noemers, 10 tellers, 3 producten, 82 `exponent`-velden.
`m6.py --check`: 0 van 1017 niet in L6, 0 declaraties bewogen, 727 declaraties vergeleken met `e92fcb1f8` (de 738 uit de tabel min de 11
van de verwijderde `wf123-*`). Een tweede `--all`: 0 gemigreerd, geen byte. `--check` leest de basis met `git show e92fcb1f8:<pad>`.

**§8, bewaking, gemeten.** Script ≡ emitter: gates 10, 11 en 16 groen op de gemigreerde bestanden. Rapporten: de verifier van
`e92fcb1f8` op het oude negatief corpus tegen die van deze commit op het nieuwe: alle 543 overblijvende getuigen regel voor regel gelijk,
na de vertaling `/unit` → `/unit/0` en `/unit/denominator` → `/unit/1` van de pointers. `expect.txt`: −5 `wf123-*`, +6 `wf124-*`, geen andere
wijziging. Geen `case*.json` in `corpus/` of `toka/` gewijzigd, dus geen `expectedFault.params`. Productdocumenten uit
`werk/omgekeerde-eenheid/fable` (`vierkant`, `passend`, `passend-per-s`), oude bouwsels op de oude wire tegen nieuwe op de gemigreerde: verifier
`PASS`, clingo `non-identical units`, Drools `PLAYED 1` op `vierkant` en `passend` en `UnitConflict` op `passend-per-s` op beide takken, zoals §7.1.
Enige verschil, zie afwijking 3: ref meldt `m^2 vs m*m` waar het `m·m vs m*m` meldde.

**Afwijkingen van het advies.**

1. De advieszone miste `backends/drools/src/test/resources`: zes `ir.json` met eenheden, die `m6.py --all` ook migreert. Daardoor en door de
   verwijderde getuigen wijken de aantallen af van §1.1 en §7.3 (261/1446/103/35/4). De 103 en 35 van het advies tellen de `wf123-*` mee.
2. De advieszin "730 declaraties": ik meet 738 over de zones inclusief Drools (9), dus 729 zonder; negatief 234 waar het advies 235 zegt. Niet verder
   uitgezocht: de toets is byte-gelijkheid per bestand, en die houdt.
3. De rendering van §1.4 is ingevoerd zoals geschreven, dus een product met een herhaald atoom heet nu `m^2` in meldingen, waar het `m·m` heette.
   Geen enkel corpusgeval of bestaande test pint die tekst; de bestaande vormen (`€/jr`, `1/dg`, `%`, `m`) zijn byte-gelijk.
4. Tekstfout in het advies, §1.4 en §2: de canonieke volgorde (codepunten) en "positieve factoren in wire-volgorde" geven `m·€/jr`, `kg·€/jr^2`
   en `€·n` (`€` is U+20AC, boven elke ASCII-letter), en `€/s/u`, niet `€·m/jr`, `€·kg/jr^2` en `€/u/s`. Ik volg de regel. `m·€/jr` en `€/s/u` staan zo
   in IR-SPEC ch. 2.5 en in `test_ref`. `€·n` is daarmee `n·€`. Stap 2 moet de tabel van §2 op dit punt lezen.
5. `DeclaredUnit` in het schema is niet "ongewijzigd": de eigenschap `denominator` ging eruit. Geen enkele declaratie droeg haar buiten `wf123-declared-one`.
6. `check-reftype-derivation.sh` en `DrlUnitSiteWitnessTest` twisten nu de eerste factor met een omrekenpaar, zoals §4 zegt, en dus ook een reciprook in de
   Drools-test. Gate 13: 46 annotaties getwist over 26 goldens (vloer 38), geen overlevende.
7. `spec/proposals/v1-naamtabel-toets.py` is aangeraakt (buiten §11): de M-stap M9 is toegelaten, en de rijen `denominator`, `noemer`, `exponent` en de
   telling zijn bijgewerkt. De toets is geen gate en stond al rood: 24 bevindingen over `IdentityCheck`, `anchor` en `Default.name`, geen enkele over een eenheid.
8. Tien `NOTE.md` van `wf092-*`/`wf034-unit-unknown-system` en de `NOTE.md` van `jutterswet-tempototaal` (buiten §11, die alleen `ir.json` noemt): de
   pointers in de geciteerde rapporten volgen de lijst.

**Voorlopige besluiten** (geen van deze stond open in het advies als normvraag).

- *Interne vorm.* OCaml: `Ast.unit_factor` en `measure_unit = unit_factor list`; `declared_unit.ge_unit` is een `unit_factor`; `exponent : int option`, zodat WF-124
  een geschreven 1 kan zien. Java: `Unit(List<Factor>)` met `Unit.Factor(abbreviation, system, canonical, factorToCanonical, exponent)`; de lijst staat op volgorde van de wire.
- *Oude lezing als projectie.* `Units.numerator`/`denominator`/`over` en in Java `abbreviation()`, `system()`, `canonical()`, `factorToCanonical()`, `denominator()`, `numerator()`,
  `compound`, `reciprocal`, `product` geven de lezing van de oude record, zodat alle aanroepers ongewijzigd blijven: `abbreviation()` is de teller (`1` als die leeg is, atomen met `·`
  bij een product). Daardoor raakt deze commit in Drools alleen de loader en één javadoc, en in `TmsTranspiler` niets.
- *Opake producten blijven opaak.* `Checks.units_convertible` en Java `numeratorConvertible` vergelijken een zijde van meer dan één atoom op de samengevoegde naam, niet op dimensie.
- *Loaders en WF-124.* De OCaml-loader laadt een lege lijst en een geschreven exponent 0 of 1, zodat de verifier de arm noemt. De Drools-loader weigert een lege lijst en exponent 0 met een
  `DocumentError` (nieuw getuige `a_unit_is_a_list_of_factors`), en leest de rest. Ref en clingo laden via de gedeelde loader en draaien de verifier niet: gemeten op de zes `wf124-*` met de case van `jutterswet-vergunningsduur` spelen beide `PASS`, omdat de getuigen de eenheid op een ongelezen attribuut of een declaratie zetten. Op een gelezen eenheid is `[]` in ref de teller `1` en telt exponent 0 als factor 1; dat is geen nieuwe zwakte (de oude bare `"1"` rekende ook stil door), maar een open punt voor C3: wie `Units.times` bouwt, kan hier een fout zetten.
- *`m6.py --check` weigert een afkorting `"1"` in een lijst*, als resttoets op de oude vorm; de verifier heeft daar geen regel meer voor. Staat C4 ooit een eenheid `1` toe, dan vervalt die regel in het script.
- *Sleutels van WF-124*: `empty-unit`, `exponent-zero`, `exponent-one-written`, `duplicate-factor`, `factors-unsorted`, `declared-exponent`; parameters `unit`, bij `declared-exponent` ook `unitSystem`, bij `factors-unsorted` `spelling`.

**Gewijzigde testen, alleen in vorm** (geen verwachte waarde verzwakt).

- Ingebedde wire-documenten in `test_ref`, `test_clingo` en de Java-testen dragen de lijst; `TypeSystemRefusalWitnessTest.UNIT` en de pointers in `TypingRefusalWitnessTest` eindigen op `/unit/0`.
- `UnitIdentityTest`: de wire-pin `"abbreviation":"1","denominator":{…}` is nu `"unit":[{… mnd …,"exponent":-1}]`, en `"abbreviation":"1","denominator"` is `"exponent":-1`.
- `ValueTermWitnessTest`: de pin op de `toString` van `Unit` (`Unit[abbreviation=km, …, denominator=null]`) is `Unit[factors=[Factor[abbreviation=km, …, exponent=1]]]`.
- `UnitConflictWitnessTest.a_namesake…`: de naamgenoot is `new Unit(derived.factors())`, waarde-gelijk en een ander exemplaar; de asserts zijn gelijk.
- `test_ref`: `WV-2.6-the-unit-m^2-has-no-inverse` bouwt `m` met exponent 2 in plaats van de afkorting `m·m`; `WV-2.6-1/dg-spelling` leest `render` en `Units.denominator`; `u.abbr_text = Some "€" && u.denominator = None` is de sterkere `u = eur`.
- Nieuw: `WV-2.5-spelling-*`, `WV-2.5-order-*`, `WV-2.5-total-factor-*` in `test_ref`; `a_unit_is_spelled_from_its_factors`, `the_factors_of_a_product_stand_in_code_point_order`, `a_self_product_is_one_factor_with_exponent_two` in `UnitFactorTest`; de rij `Unit.denominator` in `test_clingo`; zes `wf124-*`; in de schema-zelftest van gate 2 acht nieuwe rijen en één vervangen (`Unit.denominator = null` is `a factor carries a denominator`).

**Wat stap 2 moet weten.**

- Ingangen voor C3: `spec/verifier/lib/units.ml` kent alleen vorm en spelling en hangt niet aan `zarith`; `Ir_v0.Units.times`/`divide` met een `Q.t` vragen dus een keuze (het advies noemt `(unit option, Q.t)`; `ir_v0` linkt alleen `yojson`). `Arith.total_factor` is al F(U) = ∏ f^e. `Arith.conv_factor` is al F(a)/F(b), met de test rate/plain ervoor; `Checks.units_convertible` is de plek voor D(U) = D(V).
- De projecties die C3 weghaalt: `Checks.is_reciprocal`, `is_product`, `numerator_unit`, `reciprocal_unit`, `Units.numerator_text`, en in Java `compound`, `reciprocal`, `product`, `abbreviation()` als tellernaam, `system()`, `canonical()`, `factorToCanonical()`.
- Een `Number.unit` met twee negatieve factoren of een macht (`€/u/s`, `€/dg^2`) laadt nu in alle loaders en is verifier-schoon, maar geen enkele uitvoerder rekent er dimensie mee; `denominator` geeft dan het opake product van de negatieve factoren. De frontend schrijft deze vormen nog niet.
- `Unit.exactFactors` blijft per afkorting per stelsel; `IrEmitter.exactFactor` neemt nu een `Unit.Factor`.
- IR-SPEC: punt 4 (opaak product) en de voorbeeldregel `€·m` staan nog in ch. 2.5, net als RUNTIME-SEMANTICS ch. 2.6/2.10 en ASP-TRANSLATION S6/S19; punt 5 heet nu "The factor list". De zin in `grammar/DIVERGENCES.md` over `m^2` noemt de wirevorm `[{m, exponent: 2}]` en moet in C4 weg.
- Lange runs: een Bash-aanroep zonder `timeout` of achtergrondvlag stopt na 2 minuten en trekt een `&`-proces mee. Gebruik de achtergrondvlag van het hulpmiddel voor `make gates`. Bewerk geen bron tijdens de run: `after-run` faalt erop.

### Stap 2 (C3), 2026-11-01

Commits op `card/unit-factors`: `e1297eb0f` verifier, ref en clingo; `a5c1ce25c` Java en spec; `316c2b51b` `Arith.conv_factor` leest `Units.factor`; `dad99d20c` en `06d0df94d` testrijen en dode hulpfuncties; de commit `unit-factors: Stand van stap 2` deze tekst. De frontend leest de nieuwe spellingen nog niet: RGS-360 staat er nog, en `m^2` komt in deze stap alleen als tussenresultaat en via de wire voor (stap 3).

**Gates.** `make gates` op `06d0df94d`: 17 groen, `after-run` groen, `red=0`, 312 s (`werk/unit-factors/logs/stap2-gates-2.log`). `EXPECTED_PLAYED` 296 op beide takken. Geen `EXPECTED_`-regel in `scripts/` gewijzigd; in `scripts/` veranderde alleen een commentaarzin in `check-reftype-derivation.sh`. Gate 3: 396 golden schoon, 548 negatief geweigerd, `expect.txt` 391. Gate 5: 1764 voorbeelden, 964 cases. Gate 6: 2264 checks, 964 cases. Gate 10: 1342 tests, 157 goldens byte-gelijk, 353 codes (vloer 347). Gate 13: 46 annotaties getwist over 26 goldens, geen overlevende. Gate 17: 296 goldens, 834 case-bestanden.

**Wat niet bewoog (§8).** `git diff 55fee8d32 -- corpus toka` is leeg: geen waarde, geen declaratie, geen `expect.txt`, geen case. `jutterswet-vondsttempo` speelt in alle uitvoerders.

**Per uitvoerder.**

- *Gedeeld (`spec/verifier/lib/units.ml`):* `times`, `divide`, `convertible`, `factor`, `atom_class`, `dimension`, `is_atom`, `integrated`; `norm` en `collapse_ws` verhuisden hierheen, `Checks` verwijst ernaar.
- *Verifier:* `times_unit`, `quotient_unit`, `units_convertible`, `is_reciprocal`, `is_product`, `numerator_unit`, `reciprocal_unit` weg. `composite_expectation` claimt elke maal en elke deling van twee `Number`s. `time_unit_of_unit` antwoordt alleen op één factor met exponent 1.
- *Loader:* een `Executor` weigert `unit: []` en `exponent: 0` bij het laden, in de zin van WF-124; de `Verifier` laadt beide. Rijen `WV-2.5-an-executor-refuses-*` in `test_ref`.
- *Ref:* `Value.Product` weg, `mul` en `div_gen` via `Units` met `Arith.restatement`, `conv_factor` via `Units.factor` en met een fout op een niet-omrekenbaar paar, `Timeline.duration` via regel 8.
- *Clingo:* `eff_unit` via `Units`, `product_factor`, `mul_units_ok`, `quotient_units_ok`, `comparison_factors` en `grid_strip_ok` naar §3, `reciprocal_cancels` weg; `Normalise` raakt alleen een eenheid van één factor met exponent 1 aan. Geen nieuwe deling.
- *Java:* `Unit.times`, `divide`, `timesFactors`, `divideFactors`, `convertibleTo` op dimensie, `exactRestateFactors` met machten, `isAtom`, `negativeAtom`, `afterTimeFold`; `product`, `isProduct`, `isReciprocal`, `numerator` weg. `Typing.compoundUnit` weg. RGS-282, RGS-293, RGS-294 uit register en beide catalogi. `EffectiveUnit` via `Unit`, `DERIVED_PRODUCTS` en de vrijstelling in `UnitConflict` weg. `TmsTranspiler`: de factor van een product of deling als `Term.Restate` op de rechter operand, `intoDenominator` weg. `ScenarioResolver.exactRescaling` per factor met machten. `TimeCalculation.chronoUnit(Unit)` antwoordt alleen op één factor met exponent 1.
- *Spec:* RUNTIME-SEMANTICS ch. 2.6 (regels en tabel) en ch. 2.10 (vierde MAAL-uitkomst en quotient zonder normaalvorm weg), IR-SPEC ch. 2.5 (punten 1, 2, 3, 4, voorbeelden, het opake product weg) en ch. 4.1 (de `times`-alinea, de rij WF-091), ASP-TRANSLATION S6 en S19.

**De tabel van §2** staat in ch. 2.6 en als `WV-2.6-*` in `test_ref`, op waarde en eenheid, met de spelling van de regel van §1.4: `kg·€/jr^2`, `m·€/jr`, `n·€`. Rij 3 wijkt in één opzicht af, zie afwijking 3.

**Omgedraaide testen, oud en nieuw.** De kaart verandert dit gedrag expliciet; geen verwachting is losser gemaakt.

| test | oud | nieuw |
|---|---|---|
| `test_ref` `WV-2.6-c` | `3 € × 2 m` is `Value.Product` | `Unit`, gespeld `m·€` |
| `test_ref` `WV-2.6-the-unit-m^2-has-no-inverse` | `reciprocal_unit` is `None` | `WV-2.6-the-inverse-of-m^2-is-1/m^2`: `1/m^2` |
| `test_ref` `WV-2.6-2m-times-3m-does-not-restate-into-a-declared-m^2` | fout `incompatible units` | `WV-2.6-2m-times-3m-restates-into-a-declared-m^2`: 6 |
| `test_clingo` needle bij `1/wk × dg` | `the factor 1/7 from the quantity into the denominator` | `the factor 1/7 from the right operand into the left unit`: de zin geldt nu ook voor `m × cm` |
| `TypingTest.the_inverse_of_a_product_is_a_loud_error` | RGS-282 | `the_inverse_of_a_product_is_a_unit`: `1/EUR/m` |
| `TypingTest.times_unspellable_rate_factor_is_a_loud_error` | RGS-293 | `times_a_rate_and_another_unit_is_their_product`: `EUR·m/jr` |
| `TypingTest.divided_by_a_rate_is_a_loud_error` | RGS-294 | `divided_by_a_rate_cancels_what_the_two_share`: `jr` |
| `TypingTest.times_two_separate_units_gives_an_opaque_product_unit` | naam en commentaar opaak | `times_two_separate_units_gives_their_product`, zelfde asserts |
| `UnitIdentityTest.unspellable_rate_factor_is_refused_loudly` | RGS-293, "niet uitdrukken" | `a_rate_factor_into_another_dimension_is_refused_by_its_unit`: RGS-335 met `value unit 'EUR·m/jr', target unit 'EUR'` |
| `UnitFactorTest.a_self_product_is_one_factor_with_exponent_two` | `isProduct`, `abbreviation` `m·m`, `canonical` null | de factor `m`, exponent 2, met zijn eigen `canonical` en factor; omrekenbaar in zichzelf, niet in `m` |
| `UnitFactorTest.opaque_product_refuses_the_factor_question` | naam | `a_product_refuses_the_factor_question_into_its_own_factor`, zelfde asserts |
| `UnitConflictWitnessTest.a_DERIVED_opaque_product_stays_the_conservative_no_op` | geen fout | `a_product_against_another_dimension_is_a_fault`: `UnitConflict` |
| `UnitConflictWitnessTest.a_namesake_declared_on_the_wire_not_gets_the_vrijstelling` | herkomst maakt het verschil | `a_product_declared_on_the_wire_is_the_same_unit_as_a_derived_one`: beide `UnitConflict` tegen `EUR` |
| `UnitConflictWitnessTest.two_different_derived_products_also_set_each_other_free` | geen fout | `two_different_products_are_a_fault` |
| `UnitConflictWitnessTest.the_origin_table_grows_per_derivation_not_per_document` | groei van de tabel | weg, met de tabel |
| `EffectiveUnitWitnessTest.a_times_of_two_separate_units_gives_the_opaque_product` | `canonical` null, alleen identiek product omrekenbaar | `a_times_of_two_separate_units_gives_their_product`: dezelfde factoren zijn één eenheid, geen factor naar een andere dimensie |
| `TmsLikeRefWitnessTest.COUNT` | 28 | 30: `product-into-square`, `product-restated-into-left-factor` |

`UnitFactorTest.a_case_lie_refuses_the_factor_question` en de andere vijf weigervormen staan ongewijzigd.

**Nieuw.** In `test_ref` de tabel, regel 1 (`cm·m` tegen `m^2`), regel 8 (`€/mnd^2`, `€/m/mnd`, geen factor van het rooster, `Units.integrated`), een half paar in een product, de loader, en de kruising door de verifier op `product-into-rate`. In `test_clingo` 22 checks over `factors_doc`: `m × m` in `m^2`, `cm × m` en `m × m` in `m^2/s` geweigerd bij de schrijfgrens, `m × cm` geweigerd op de factor 1/100, `€/dg × wk` ×7, `€/dg/m × dg × m`, `€ × m`, `m^2 ÷ m`, `€ ÷ aantal`, `1 ÷ m^2`, `dg ÷ wk` geweigerd, en `m^2` tegen `cm^2` in beide uitkomsten. In Java `TypingTest.the_table_of_the_algebra_types_every_row`, zeven testen in `UnitFactorTest`, `UnitConflictWitnessTest.a_product_into_its_declared_square_plays`.

**Rode stand.** Op `55fee8d32` met de nieuwe testbestanden (`werk/unit-factors/logs/stap2-ref-red-2.log`, `stap2-red-tests-1.log`):

- `test_ref`: 21 `FAIL`-regels, onder andere `2m-times-3m-is-6-m^2`, `2cm-times-3m-is-600-cm^2`, `6-m^2-divided-by-2m-is-3-m`, `1-divided-by-4-m^2`, `5-eur-per-dg-per-m...`, `2-m^2-equals-20000-cm^2`. Elf rijen van de tabel zijn daarbij rood; de andere acht waren al groen (`100 €/jr × 6 mnd`, `30 €/dg × 1 wk`, `3 dg ÷ 1 wk`, `6 m^2` in `m^2` en in `m^2/s`, en de drie bestaande).
- `test_clingo`: de eerste nieuwe rij stopt de suite op `incompatible units: m^2 vs m*m`, en de oude zin bij `1/wk × dg` is rood.
- Drools, de oude jar, beide takken: `product-restated-into-left-factor` is `FAIL`, verwacht 600.0000, werkelijk 6.0000. `product-into-square` speelde al.
- De nieuwe Java-testen roepen `Unit.times` aan, dat op `55fee8d32` niet bestond; ik heb daar geen aparte run van. Het oude gedrag staat in de omgedraaide testen hierboven, die in stap 1 groen waren.

**Vierkant op het document** (`werk/unit-factors/stap2-proof/`): `m × m` in `m^2`: verifier `PASS`, ref en clingo `PASS (1 expectations)`, Drools `PLAYED 1` op beide takken. `m × m` in `m^2/s`: verifier `WF-023.incompatible-units`, ref `incompatible units: m^2/s vs m^2`, clingo `non-identical units under the write`, Drools `UnitConflict` op beide takken.

**Mutaties, niet gecommit** (`werk/unit-factors/logs/stap2-mutations-{1,2,3}.log`, script `werk/unit-factors/mutate.py`, op een kopie). Elke mutatie is door haar suite gevangen.

| regel | mutatie | gevangen door |
|---|---|---|
| 1, OCaml | dimensie zonder exponent | `WV-2.6-m^2-against-m-is-no-unit-pair` |
| 1, Java | idem | `a_dimension_is_a_class_with_its_exponent` en 5 andere |
| 2, OCaml | geen factor op de waarde | `WV-2.6-2cm-times-3m-is-600-cm^2` en 16 andere |
| 2, OCaml | zelfde atoom telt niet op | `WV-2.6-2m-times-3m-is-6-m^2` en 13 andere |
| 2, OCaml | klasse niet herrekend | `WV-2.6-2-per-dg-times-3wk-is-42` en 16 andere |
| 2, Java | geen factor op de waarde | `UnitFactorTest` (3 testen) |
| 3, OCaml | geen inverse | `WV-2.6-6-m^2-divided-by-2m-is-3-m` en 8 andere |
| 3, OCaml | eenheidloos gedeeld zonder inverse | `WV-2.6-1-divided-by-4-m^2-is-1/4-per-m^2` en 5 andere |
| 3, Java | geen inverse | `a_quotient_is_the_product_with_the_inverse` en 7 andere |
| 8, ref | geen roostertoets | `WV-2.6-rule-8-no-negative-factor-of-the-grid`, `WV-3.6-g-vreemde-noemer` |
| 8, gedeeld | `integrated` haalt de factor weg | `WV-2.6-rule-8-one-negative-factor-rises-one-power` (de eerste ronde overleefde; de rij is daarna toegevoegd) |
| 8, Java | de factor valt weg | `a_time_fold_raises_the_one_negative_factor` |
| machten, Java | exponent genegeerd | `a_power_restates_by_the_power_of_its_factor` |
| WF-091 | maal claimt de eenheid van links | `WV-2.6-the-verifier-derives-the-product-against-the-annotation` (idem) |
| loader | `[]` of exponent 0 niet geweigerd | `WV-2.5-an-executor-refuses-*` |
| clingo | gehele factor niet toegepast | `€/dg × wk`: 3000 tegen 21000 |
| clingo | niet-gehele factor stil afgekapt | `1/wk × dg`, `m × cm` |
| clingo | vergelijking zonder factor | `m^2 against cm^2` |
| clingo | deler met factor toegelaten | `dg ÷ wk` |
| clingo | `eff_unit` van maal is links | `m × m in m^2` en 5 andere |
| Drools | maal zonder factor | `TmsLikeRefWitnessTest`: `product-restated-into-left-factor` |
| Drools | factor van de deling omgedraaid | `TmsLikeRefWitnessTest`: `quotient-of-convertible-units` |
| Drools | `EffectiveUnit` van maal is links | `EffectiveUnitWitnessTest` (4 testen) |
| Drools | vrijstelling terug op een product | `UnitConflictWitnessTest` (6 testen) |

**Afwijkingen van het advies.**

1. `Units.times` en `divide` geven geen `Q.t`, maar de eenheid en een product van machten van gedeclareerde eenheden (`measure_unit`). Reden: `ir_v0` linkt alleen `yojson`, en een half omrekenpaar moet in ref bij naam fout gaan terwijl de verifier het getal niet nodig heeft. Ref neemt de waarde met `Arith.restatement`; `Units.factor a b` is `a` met `b` omgekeerd.
2. `product_factor` is `int option` en niet `(side, int) option`: de factor staat altijd op de rechter operand. De oude tak op de linker operand (`wk × 1/dg`) geeft dezelfde waarde; alleen het range-hek ligt op de andere term. Hetzelfde in Drools: `intoDenominator` is weg, de `Term.Restate` staat altijd rechts.
3. Tabelrij 3, `6 m^2 in een m^2/s-attribuut`. De tekst `incompatible units: m^2 vs m^2/s` komt uit `restate_into (m^2) 6 (m^2/s)`, en zo staat de rij in `test_ref`. Een schrijfregel kruist de waarde in de gedeclareerde eenheid en meldt `m^2/s vs m^2`; ch. 2.6 zegt dat in een zin onder de tabel.
4. De klasse in Java vergelijkt namen zoals geschreven, niet onder `norm` zoals §2 zegt; OCaml volgt §2. `UnitFactorTest.a_case_lie_refuses_the_factor_question` blijft daarmee staan. Een document waarin een `canonical` alleen in hoofdletters verschilt, herrekent in ref en is in Drools een `UnitConflict`: luid, en zoals vóór deze stap. Wel nieuw in Java: een kale eenheid is het canonieke doel van een eenheid die haar noemt, met factor 1, zoals §2 zegt.
5. `quotient_units_ok` laat ook `€ ÷ aantal` toe (factor 1, deler bewezen eenheidloos). Dat was geweigerd; §3 noemt alleen "factor 1 toegelaten". Geen test pinde de weigering.
6. De projecties die stap 1 noemde: ik heb `compound`, `reciprocal`, `abbreviation()`, `system()`, `canonical()`, `factorToCanonical()`, `denominator()` en in OCaml `Units.denominator`, `numerator_text`, `over` laten staan. Ze lezen een gedeclareerde eenheid of de spelling in een melding, en de grep op die namen geeft ongeveer tweehonderd regels, ook in testen. Geen van hen rekent nog met een eenheid. `Unit.reciprocal` geeft nu nooit `null`.
7. Buiten de lijst van §11: IR-SPEC ch. 4.1 (de `times`-alinea, die de claim van de verifier beschrijft) en de rij WF-091 in de index.
8. `like-ref`: `rate-times-converted-quantity` bestond al en dekt `€/dg × wk`. De twee nieuwe documenten zijn `product-into-square` en `product-restated-into-left-factor`. `a_rate_times_a_convertible_unit_applies_the_factor` is daarom geen aparte test.

**Voorlopige besluiten.**

- *Regel 8 bij meer dan één negatieve factor.* Ref heeft het rooster en rekent (`€/m/mnd` over maanden is `€/m`). Drools weigert bij naam (`RATE_DENOMINATOR_NOT_GRID`, `negativeAtom` is null), clingo komt bij de schrijfgrens uit op `non-identical units`, de frontend typeert het totaal ongewijzigd, en de verifier claimt daar niets. Geen golden heeft de vorm. Bij één negatieve factor is de uitkomst in alle vier gelijk, ook bij een macht (`€/mnd^2` is `€/mnd`).
- *`conv_factor` op een niet-omrekenbaar paar* is een fout met `incompatible units`, waar de oude toets alleen rate tegen plain ving. Elke aanroeper toetst eerst.
- *`convertible` met een factor zonder klasse* (geen `canonical`, geen afkorting; de wire kent de vorm niet, het AST wel) geeft waar, zoals de oude functie; `Normalise.unit_named` blijft de grens.
- *Een grote `exponent`:* de exacte machten rekenen zonder bovengrens door; ik heb er geen gezet.

**Open, buiten §11, voor de hoofdsessie.** `corpus/golden/jutterswet-aanvoergeld/NOTE.md` beschrijft twee vormen die clingo weigerde: `t` tegen `kg` in een vergelijking en `€/kg` maal gewicht in `€`. Beide vertalen nu. De andere NOTE's met `unit-carrying multiplicative operand` gaan over `percentageOf` en kloppen nog. `spec/CONSTRUCTS.md` rij 26 is nog niet bijgewerkt; dat is C6. `check-reftype-derivation.sh` is een beschermd pad (alleen commentaar).

**Wat stap 3 moet weten.**

- `DefinitionResolver.normalForm` is ongewijzigd en gebruikt `Unit.compound` en `Unit.reciprocal`. `compound` neemt de positieve factoren van links en keert rechts om, zonder atomen samen te voegen; voor `A^a/B^b/…` is een lijst met `Unit.Factor` en de canonieke volgorde de directe weg (`sorted` is nu privé).
- `IrEmitter` schrijft `exponent` al voor elke factor met een exponent anders dan 1 (r. 633), en de frontend houdt `Number.unit` als lijst factoren; daar is voor C4 geen wijziging nodig.
- Register: 353 codes, RGS-360 staat er nog; na C4 −1 +1. Catalogi idem. De vloer is 347.
- `grammar/DIVERGENCES.md` zegt bij `m^2` nu alleen dat deze lezer de spelling niet leest, en de tabel daarboven noemt RGS-360: beide gaan weg in C4.
- `UnitIdentityTest.a_power_is_refused_and_is_not_the_unit` en `a_spelling_without_a_normal_form_is_refused_by_name` pinnen RGS-360 en keren in C4 om.
- `UnitIdentityTest` en `TypingTest` gebruiken `EUR` als afkorting met `€` als alias: de rendering is `EUR·m/jr`. Let daarop bij een verwachting met `€`.
- Clingo-grenzen voor de goldens van C5: een product van drie operanden met decimalen raakt het range-hek van 46340 (`5,00 × 2,00 × 3,00`: `factors_doc` gebruikt daarom operanden zonder decimalen).
- Een Bash-aanroep die langer dan 180 s loopt, gaat vanzelf de achtergrond in; houd de wachtlus onder 170 s.

