# omgekeerde-eenheid: een eenheidloze teller gedeeld door E heeft de eenheid E⁻¹

*Vastgelegd op 2026-10-11 door de hoofdsessie van de werkbank, op verzoek van de maintainer, naar aanleiding van de
demovideo (beat h04b08: de resolvermelding `gedeeld door: een eenheidloze teller gedeeld door de eenheid 'dg' geeft een
omgekeerde eenheid (1/dg) die het Eenheid-model niet kan uitdrukken`). Gelezen op main `e2144945`; niets nagemeten
met ref, clingo of Drools.*

Goal: het `Unit`-model kan een omgekeerde eenheid uitdrukken, en de typering geeft een eenheidloze teller gedeeld door
een eenheid E de eenheid E⁻¹ in plaats van een resolverfout. Daarmee zijn een frequentie (s⁻¹), een aantal per dag
zonder teller-eenheid en het omgekeerde van een duur te declareren en uit te rekenen, op elke route gelijk.

## Aanleiding

- **Besluit 17b (2026-08-29)** maakte eenheidloos ÷ E een luide resolverfout. Het week daarmee bewust af van Tabel 14
  rij 4 van de arbiter, die eenheidloos ÷ E de eenheid E geeft; dat is dimensioneel onjuist. De reden voor de fout was
  dat het model 1/E niet kan spellen (`backlog/done/20260829-eenheid-identiteit-typerfix.md`, Result; CONSTRUCTS rij 26;
  `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Typing.java` r. 106–111). Die fout was juist
  zolang het model de eenheid niet kon uitdrukken. Deze kaart neemt de volgende stap: het model leert de eenheid
  uitdrukken.
- **Maintainer (2026-10-11):** geen eenheid gedeeld door E betekent E⁻¹. Afwijken van Tabel 14 rij 4 blijft dus nodig,
  maar nu met het dimensioneel juiste type in plaats van een fout.
- **Het model nu.** `Unit` heeft een afkorting, een stelsel, een canonieke eenheid, een factor en hoogstens één
  `noemer` (een rate zoals `€/jr`). Daarnaast bestaat een opake producteenheid zonder ontbinding
  (`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/model/Unit.java` r. 25–55; IR-SPEC ch. 2.5;
  `spec/schema/uchar-ir-v1.schema.json`, `Unit.denominator`). Een teller zonder eenheid en exponenten bestaan niet.
  RUNTIME-SEMANTICS ch. 2.6 noemt exponenten en samengestelde eenheden van meer niveaus als v0-grens en open v1-vraag
  (CONSTRUCTS rij 26).
- **Twee stille degradaties in dezelfde hoek**, in strijd met fail-fast. `DefinitionResolver.unitOf` (r. 700–712)
  leest de `exponent` van een `eenheidMacht` nooit, zodat `met eenheid m^2` als `m` getypeerd wordt. Bij meer dan twee
  machten houdt hij alleen de teller over, zodat `€/u/s` als `€` getypeerd wordt. De grammatica accepteert beide vormen
  (`frontends/regelspraak/grammar/RegelSpraak.g4` r. 518–520).
- **Spelling in de spec.** De afgeleide spec-grammatica in de distributie
  (`regelspraak-toolchain/specification/v2.3.0/RegelSpraak-spec-for-ANTLR-v2.3.0.md` r. 498–505, §13.3.3.2/3 en
  §13.3.5.5/6) kent `'1'` als eenheidexpressie en `eenheidMacht ( CARET exponent )?` met een gehele exponent. De
  uchar-grammatica kent `'1'` niet. Of de normatieve tekst een negatieve exponent of `1/E` toestaat, is niet nagelezen.

## Acceptatie (bij oppakken aanscherpen)

- (a) **Representatie.** Een eenheid is een product van factoren met een gehele exponent (negatief toegestaan), met
  dezelfde exacte omrekening naar de canonieke eenheid van elk stelsel (`factorToCanonical`, WF-092) als nu. `€/dg` en
  `€ · dg⁻¹` zijn dezelfde eenheid. Een rate wordt een bijzonder geval van deze vorm of wordt er eenduidig op
  afgebeeld; de bouwer beslist dat tegen IR-SPEC ch. 2.5 en de genormaliseerde vorm van Q9
  (`spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md`) en meldt het expliciet,
  inclusief wat er met `Unit.denominator` op de wire gebeurt (schemawijziging of afbeelding).
- (b) **Typering.** eenheidloos ÷ E → E⁻¹; E⁻¹ × E → dimensieloos; € ÷ dg → €/dg zoals nu; vergelijken en optellen
  vereisen dezelfde dimensie, met herrekening (`1/wk` tegen `1/dg`: factor 1/7). De rood-eerst-test in
  `UnitIdentityTest` die de luide fout pint, wordt een test op het type E⁻¹. Een eenheid die het model ook dan niet
  kan uitdrukken, blijft een luide fout en wordt nooit een wire-type.
- (c) **Declaratie.** `met eenheid` kan een omgekeerde eenheid spellen, in de vorm die de spec draagt (`1/dg`, of
  `dg^-1` als de normatieve tekst dat toelaat; de bouwer kiest tegen de bron en noteert de keuze in DIVERGENCES.md als
  het een uitbreiding is). De twee stille degradaties hierboven verdwijnen: een exponent wordt gelezen en een tweede
  noemer wordt gedragen, of geeft een luide fout.
- (d) **Alle routes.** Ref, clingo en de tak met truth maintenance rekenen en vergelijken een omgekeerde eenheid gelijk.
  Een publiek golden laat zien: een aantal (eenheidloos) gedeeld door een duur in dagen is een attribuut
  `met eenheid 1/dg`, vergeleken met een waarde in `1/wk`, en vermenigvuldigd met een duur weer eenheidloos. Ref ≡ clingo
  ≡ Drools; de waarden met de hand nagerekend.
- (e) **Het demogeval.** `regelspraak-werkbank/presentaties/demo-govtech4all/90b-eenheid-deling-fout.rgs` (een bedrag
  zonder eenheid gedeeld door `borgdagen` in `dg`, toegekend aan een `dagbedrag` zonder eenheid) blijft geweigerd, maar
  nu met een melding over de eenheid van het doel, namelijk dat `1/dg` niet past op een attribuut zonder eenheid. Een
  melding dat het model de eenheid niet kan uitdrukken, mag het niet meer zijn.
- (f) Alle gates groen; gate 10/11 byte-identiek, of de bewegende goldens expliciet genoemd. CONSTRUCTS rij 26 en het
  spoor van besluit 17b bijgewerkt (hoofdsessie, serieel bij de landing).

## Omvang

Middel tot groot: de representatie raakt het IR-schema, het Java-model, de typering, de emitter en loaders, ref, clingo
en Drools. Na `20261008-tms-omschakeling`, zodat alleen de tak met truth maintenance mee hoeft; de DRL-teksttak volgt
met `20261008-drl-teksttak-op-truth-maintenance`. Raakt `20261010-clingo-let-binder-vergelijking-over-eenheden`
(herrekening over eenheden in clingo): niet tegelijk bouwen.

May touch: `frontends/regelspraak/**` (grammatica, `Unit`, `DefinitionResolver`, `Typing`, emitter, tests),
`spec/IR-SPEC.md` ch. 2.5, `spec/RUNTIME-SEMANTICS.md` ch. 2.6, `spec/schema/uchar-ir-v1.schema.json` (voorstel),
`ref/lib/**`, `backends/clingo/**`, `backends/drools/**` (loader en de tak met truth maintenance),
`corpus/golden/<nieuw golden>/`, `frontends/regelspraak/grammar/DIVERGENCES.md`, kaartverplaatsing.

Out of scope: een benoemde afgeleide eenheid met eigen naam (`de hertz Hz = 1/s`); de spec-grammatica kent als
omrekening alleen `= NUMBER eenheidAfkorting`, en dit wordt eventueel een vervolgkaart. Verder buiten deze kaart: de
canonieke-eenheid-wire van Q9 als geheel, de DRL-teksttak en de toolchain (re-vendor en herpin door de hoofdsessie van
de distributie).

Result: de eenheid is een product van factoren in één normaalvorm, op de bestaande wire; eenheidloos ÷ E is E⁻¹ op
alle routes. Vier uitvoerders spelen de golden. De kaart blijft in `doing/`; de hoofdsessie landt.

**Commits.** `c98ccf1a3` claim; `891987a18` frontend; `d93ffa8e5` uitvoerders; `523331016` voorstel (spec, schema,
WF-123, golden, negatieve gevallen, gate-scripts). De laatste commit op de branch zet dit Result. Beschermde paden in
`523331016` en `d93ffa8e5`: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/`, `spec/verifier/`, `scripts/`;
`spec/ASP-TRANSLATION.md` valt onder het CODEOWNERS-net (S6, twee aanvullingen).

**(a) Representatie en wire: afbeelding, geen schemawijziging (voorlopig besluit).** Normaalvorm (IR-SPEC ch. 2.5, punt 5):
de factoren met een positieve exponent zijn de teller (`abbreviation`, vanaf twee factoren het bestaande opake product,
dus `m²` is `m·m`), de ene factor met exponent -1 is `denominator`. Een teller zonder factor is `"1"`:
`{"node":"Unit","abbreviation":"1","denominator":{…dg…}}`. Geen factor is geen eenheid: geen `unit`.
Afweging: een veld met factoren en exponenten is de letterlijke vorm van (a), maar verschuift elke eenheid in alle
goldens en `toka/`, elke loader en WF-091's bytevergelijking, en loopt vooruit op de canonieke-eenheid-wire die Q9 als
latere v1-vorm vastlegt. De afbeelding laat geen golden of `toka/`-bestand bewegen en geeft de bestaande rate-regels
(omrekenen met omgekeerde noemer, ch. 2.5 punt 3) zonder meer aan `1/dg`. De prijs: wat twee negatieve factoren nodig
heeft (`€/u/s`, `dg⁻²`) heeft geen wire-type en blijft een luide fout. `factorToCanonical` blijft de bron van de
omrekening; de teller `"1"` draagt er geen en telt als factor 1. WF-123 houdt `"1"` op die ene plaats, met vier armen en
vier negatieve gevallen.

**(b) Typering.** eenheidloos ÷ E → E⁻¹ (ook ÷ `1/n` → n, ÷ `a/n` → `n/a`); E⁻¹ × E' (omrekenbaar) → geen eenheid;
E⁻¹ × F → `F/E`, zodat `€ ÷ dg`, `€ × 1/dg` en `1/dg × €` hetzelfde record geven; vergelijken en optellen via de bestaande
`convertibleTo`. Inverse van een product: nog steeds RGS-282, met een nieuwe tekst.

**(c) Spelling in `met eenheid`.** Gelezen in de afgeleide spec-grammatica van de distributie (r. 498–505): `<exponent>` is
daar een `<geheelgetal>` en dat token laat een minteken toe, dus `dg^-1` is normatief. `1/E` staat er niet: daar staat `'1'`
alleen. (d) vraagt `met eenheid 1/dg`, dus die vorm is een uitbreiding, vermeld in `grammar/DIVERGENCES.md` ("An inverse
unit"), en de twee spellen één eenheid. Tweede degradatie: de grammatica van de spec kent één slash, de onze `*`. Lezing:
`m`, `m^1`; `m^n` (n ≥ 2) is het product, dus `m^2` is de eenheid van `m maal m`; `dg^-1`, `1/dg`; `€/dg`. RGS-360 (fout, geen
eenheid): `€/u/s`, `1/dg/m`, `m^0`, `m^-2`, `€/dg^2`, `m^2/s`, `2/dg`, `m^1/2`. Voorlopig besluit: `m^2` wordt gedragen omdat de
wire het al kan spellen, `€/u/s` weigert omdat ref en de typering een product in de noemer niet doorrekenen.

**(e) Demogeval.** Nieuwe code RGS-359 (`toekenning: de waarde heeft de eenheid '1/dg', en die past niet op een attribuut
zonder eenheid`). Voorlopig besluit: geldt voor elke waarde met een eenheid op een doel zonder, niet alleen voor een
reciprook; die zaak gaf de verifierzin RGS-335, die geen eenheid noemt. Getuige
`a_reciprocal_does_not_fit_a_target_without_a_unit`. Register achteraan: RGS-359 en RGS-360.

**Per uitvoerder.**
- *Verifier.* `times_unit` geeft `Some None` bij E⁻¹ × E en de rate bij E⁻¹ × F; nieuw `quotient_unit`. Een `dividedBy` kruist nu
  ook zijn eenheid in WF-091 (getuige `wf091-quotient-unit`); er zat in het corpus geen enkel quotiënt met een andere
  annotatie. `dividedByAbs` kruist niets, zoals daarvoor.
- *Ref.* `Arith.mul`, `div_gen`; `restate_into` kent het product tegen een gedeclareerde `m·m`.
- *Clingo.* Gemeten: de golden faalde op `a unit-carrying multiplicative operand is not translated`, en had daarna de
  vergelijking en het product nodig. Nodig en gebouwd, alleen voor deze vorm: een bewezen eenheidloos deeltal over een
  eenheid (`quotient_units_ok`); E⁻¹ × E' met een hele factor (`reciprocal_factor`, `1/dg × wk`: ×7); twee omrekenbare
  reciproken vergelijken door beide kanten te vermenigvuldigen (`comparison_factors`, `1/dg` tegen `1/wk`: `x·7` tegen `y`),
  met een bereikhek op de kale term. Een vergelijking deelt niet, dus de gehele codering kan dit, anders dan bij optellen.
- *Drools.* De weigering "dimensioned" in gate 12 gaat over dimensies, niet over eenheden; ze raakte de golden niet.
  Gemeten op de oude jar: de deling speelde, maar `seizoensoogst` gaf 7,50 in plaats van 52,50 en de exacte gelijkheid
  J2 faalde. Reden: `exactRestateFactors` gaf null op een teller zonder factor en viel terug op de afgeronde factor, en
  het product herrekende niet. Dat laatste was een bestaand stil verschil met ref, ook bij `€/dg × wk` (30 tegen 210) en `dg ÷ wk`
  (3,5 tegen 0,50): `intoDenominator` en de quotiënt-arm in `TmsTranspiler.binary` lossen het op, met twee getuigen in
  `like-ref/`. `EffectiveUnit` leidt het quotiënt af. Beide takken gelijk (één analyse).

**(d) De golden `corpus/golden/jutterswet-vondsttempo`** (`source.rgs`, twee cases, handberekening in `NOTE.md`). Met `wk = 7 dg`
en drempel 14,00 per week:
J1 10 vondsten / 4 dg = 2,50 per dag = 17,50 per week: ijverig; 2,50 × 4 dg = 10,00; 2,50 × 3 wk = 2,50 × 21 = 52,50.
J2 4 / 2 dg = 2,00 per dag = 14 per week: niet groter, wel gelijk. J4 15 / 3 dg = 5,00 = 35 per week: boven `30 1/wk`.
J5 1 / 3 dg, naar beneden 0,33; × 3 dg = 0,99; × 1 wk = 2,31. Tweede case: drempel 1,00, J1 1 / 1 dg = 7 per week, ijverig.
Ref: `oracle: PASS` (30 en 12 verwachtingen); clingo: `PASS` (30 en 12); Drools exec-model en DRL-tekst: `PLAYED 2`.

**Rood op `7c0220d11`** (de bouwsels van main, met de golden en de tests van deze branch):
- frontend op `source.rgs`: `syntaxfout` op regels 5, 10 en 40, RGS-282 op regel 20 en drie eenheidsfouten.
- verifier op `ir.json`: `WF-091.facet-mismatch … typed no unit, but the type derived from its operands is '1'` (twee regels).
- ref: `EVALUATION FAULT: unit quotient not modelled: (unitless) / dg`.
- clingo: `TRANSLATION FAULT: a unit-carrying multiplicative operand is not translated …`.
- Drools (beide takken): `FAIL case.json: attribuut seizoensoogst: verwacht 52.50, werkelijk 7.50; … kenmerk drempelhoudend: verwacht
  aanwezig, werkelijk afwezig; …`. `like-ref`: `loon: verwacht 210.00, werkelijk 30.00`; `deel: verwacht 0.50, werkelijk 3.5`.
- `UnitIdentityTest`: 10 van de nieuwe tests rood (1327 gevonden, 10 gefaald).

**Mutaties** (in een losse worktree, niet gecommit, opgeruimd):
- Herrekening clingo: `comparison_factors` geeft `(1, 1)`: `MISMATCH: J2 lacks characteristic "drempelhoudend"`, ook J4 `ijverig` en
  `onvermoeibaar`.
- Herrekening ref: `conv_factor` laat de noemer weg: `MISMATCH: J1 lacks characteristic "ijverig"`, J2, J4.
  `Arith.mul` zonder factor: `MISMATCH: J4.seizoensoogst = 5, expected 35`.
- Herrekening Drools: `intoDenominator` geeft de term terug: `FAIL … seizoensoogst: verwacht 52.50, werkelijk 7.50`, beide takken.
- Degradatie `m^2`: de exponent ongelezen (`return first`): 3 rood, `a_power_is_the_product_of_its_unit_and_not_the_unit`,
  `a_spelling_without_a_normal_form_is_refused_by_name` (`m^0`), `a_negative_exponent_spells_the_reciprocal`.
- Degradatie `€/u/s`: `powers.size() > 2` geeft de teller terug: 1 rood, `a_spelling_without_a_normal_form_is_refused_by_name` (`€/dg/m`).

**Aangepaste verwachtingen en vloeren** (geen enkele verlaagd):
- `UnitIdentityTest.unitless_divided_by_unit_is_refused_loudly` (oud: fout met "omgekeerde eenheid") wordt
  `unitless_divided_by_unit_has_the_reciprocal_type` (nieuw: schoon, en op de wire `1` over `mnd`). Het doelattribuut van die test
  kreeg `met eenheid 1/mnd`: zonder eenheid is het RGS-359.
- `TypingTest.times_unitless_operand_takes_the_other_unit_over`: het laatste deel (oud: `!ok`, "omgekeerde eenheid"; nieuw: `ok`, `1/mnd`).
- `scripts/check-drools.sh` `EXPECTED_PLAYED` 281 → 282 (de golden). `TmsLikeRefWitnessTest.COUNT` 26 → 28.
- `scripts/check-reftype-derivation.sh`: `dividedBy` en `dividedByAbs` naar `DERIVED_OP`, `sister` twist de noemer van een reciprook.
  `RUNTIME-SEMANTICS` ch. 2.10: het quotiënt uit de vierde klasse, en `dividedBy` als voorbeeld daarvan vervangen door `power`.
- `Datatype.name()` toont `rendering()` (`Numeriek (€/jr)`, was `Numeriek (€)`); geen test las het.

**Geen golden of `toka/`-bestand bewogen** (gates 10, 11 en 16 byte-identiek).

**Gates, elk apart groen in de worktree.** 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16, 17. Gate 9 niet gedraaid: de
Catala-frontend is niet geraakt. Volledige set niet gedraaid. Nieuw in `ref/test/`: de `WV-2.6-*`-rijen van ch. 2.6, letterlijk. Na
rebase op `fout-op-tussenstand`: `EXPECTED_PLAYED` opnieuw zetten. Het nummer WF-123 stond op die branch nog vrij (daar alleen de
claim op het moment van kijken).

**Voorgestelde tekst CONSTRUCTS rij 26**, achter "(rood-eerst: `UnitIdentityTest`)" in de kolom delta:

> **Landed (2026-11, card `omgekeerde-eenheid`; supersedes the reciprocal half of besluit 17b, maintainer 2026-10-11):**
> a unit is a product of factors with a whole exponent in one normal form on the existing wire (IR-SPEC ch. 2.5, point
> 5): the positive factors as the numerator, the one factor with exponent -1 as `denominator`, a numerator without a factor as
> the abbreviation `"1"` (WF-123), so eenheidloos ÷ E is E⁻¹ rather than a loud error, still diverging from Tabel 14 rij 4,
> and `€/dg` and `€ · dg⁻¹` are one unit. No schema shape moved and the canonical-unit wire of Q9 stays open. A second negative
> factor (`€/u/s`, `dg⁻²`) has no normal form and stays a loud error, never a wire type (17c). `dividedBy` derives its unit
> (ch. 2.6, 2.10), so it left the fourth class and WF-091 crosses it. Clingo carries the reciprocal in a quotient, in a
> cancelling product with a whole factor and in a comparison (ASP-TRANSLATION S6).

Spoor besluit 17b: "eenheidloos ÷ E is een luide fout, want 1/E is niet te spellen" vervalt; de afwijking van Tabel 14 rij 4 blijft,
nu met het type E⁻¹ (maintainer 2026-10-11). 17c (`€/jr × m`) blijft staan. In de zin van rij 26 "(the reciprocal 1/E is a unit
the model cannot spell)" hoort de verwijzing naar deze landing. RUNTIME-SEMANTICS ch. 2.6 "OPEN … row 26" noemt nu alleen nog de
tweede negatieve factor en het opnieuw ontbinden van een product.

**Bleef liggen, met de gemeten stand.**
- Clingo weigert met naam: `1/wk × dg` (factor 1/7, `no whole number`), `1/dg + 1/wk` en een schrijven van `1/dg` naar `1/wk`
  (`non-identical units`; getuigen in `test_clingo.ml`), `m·m` (`non-identical units under the write`), `€/dg × wk` en `dg ÷ wk`. Een
  deling en een product in een gesloten body (`closed_division`, de closed `Times`) blijven `unit-carrying` weigeren. De route via een
  `BinderRef` is de kaart `clingo-let-binder-vergelijking-over-eenheden`.
- `€ ÷ 1/n` (spelbaar als `€·n`) en `€ ÷ €/jr` blijven RGS-294 in de typering; `divided_by_a_rate_is_a_loud_error` is ongemoeid.
  `(€·m) ÷ m` wordt niet opnieuw ontbonden, zoals daarvoor.
- Een benoemde afgeleide eenheid en de toolchain: buiten de kaart. De toolchain roept `numerator()` niet aan (grep), dus de nieuwe
  null voor een reciprook raakt haar niet.
- De ANTLR- en JUnit-jars haalde de Makefile van de frontend online op in de gitignored `lib/` van de verse worktree (geen Maven).
