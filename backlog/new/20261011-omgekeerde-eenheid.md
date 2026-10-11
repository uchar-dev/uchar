# omgekeerde-eenheid: een eenheidloze teller gedeeld door E heeft de eenheid E⁻¹

*Vastgelegd op 2026-10-11 door de hoofdsessie van de werkbank, op verzoek van de maintainer, naar aanleiding van de
demovideo (beat h04b08: de resolvermelding `gedeeld door: een eenheidloze teller gedeeld door de eenheid 'dg' geeft een
omgekeerde eenheid (1/dg) die het Eenheid-model niet kan uitdrukken`). Gelezen op main `e2144945`; niets nagemeten
met ref, clingo of Drools.*

Goal: het Eenheid-model kan een omgekeerde eenheid uitdrukken, en de typering geeft een eenheidloze teller gedeeld door
een eenheid E de eenheid E⁻¹ in plaats van een resolverfout. Daarmee zijn een frequentie (s⁻¹), een aantal per dag
zonder teller-eenheid en het omgekeerde van een duur te declareren en uit te rekenen, op elke route gelijk.

## Aanleiding

- **Besluit 17b (2026-08-29)** maakte eenheidloos ÷ E een luide resolverfout. Het week daarmee bewust af van Tabel 14
  rij 4 van de arbiter, die eenheidloos ÷ E de eenheid E geeft; dat is dimensioneel onjuist. De reden voor de fout was
  dat het model 1/E niet kan spellen (`backlog/done/20260829-eenheid-identiteit-typerfix.md`, Result; CONSTRUCTS rij 26;
  `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Typering.java` r. 106–111). Die fout was juist
  zolang het model de eenheid niet kon uitdrukken. Deze kaart neemt de volgende stap: het model leert de eenheid
  uitdrukken.
- **Maintainer (2026-10-11):** geen eenheid gedeeld door E betekent E⁻¹. Afwijken van Tabel 14 rij 4 blijft dus nodig,
  maar nu met het dimensioneel juiste type in plaats van een fout.
- **Het model nu.** `Eenheid` heeft een afkorting, een stelsel, een canonieke eenheid, een factor en hoogstens één
  `noemer` (een rate zoals `€/jr`). Daarnaast bestaat een opake producteenheid zonder ontbinding
  (`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/model/Eenheid.java` r. 25–55; IR-SPEC ch. 2.5;
  `spec/schema/uchar-ir-v1.schema.json`, `Unit.denominator`). Een teller zonder eenheid en exponenten bestaan niet.
  RUNTIME-SEMANTICS ch. 2.6 noemt exponenten en samengestelde eenheden van meer niveaus als v0-grens en open v1-vraag
  (CONSTRUCTS rij 26).
- **Twee stille degradaties in dezelfde hoek**, in strijd met fail-fast. `DefinitionResolver.eenheidVan` (r. 700–712)
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
  `EenheidIdentiteitTest` die de luide fout pint, wordt een test op het type E⁻¹. Een eenheid die het model ook dan niet
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

May touch: `frontends/regelspraak/**` (grammatica, `Eenheid`, `DefinitionResolver`, `Typering`, emitter, tests),
`spec/IR-SPEC.md` ch. 2.5, `spec/RUNTIME-SEMANTICS.md` ch. 2.6, `spec/schema/uchar-ir-v1.schema.json` (voorstel),
`ref/lib/**`, `backends/clingo/**`, `backends/drools/**` (loader en de tak met truth maintenance),
`corpus/golden/<nieuw golden>/`, `frontends/regelspraak/grammar/DIVERGENCES.md`, kaartverplaatsing.

Out of scope: een benoemde afgeleide eenheid met eigen naam (`de hertz Hz = 1/s`); de spec-grammatica kent als
omrekening alleen `= NUMBER eenheidAfkorting`, en dit wordt eventueel een vervolgkaart. Verder buiten deze kaart: de
canonieke-eenheid-wire van Q9 als geheel, de DRL-teksttak en de toolchain (re-vendor en herpin door de hoofdsessie van
de distributie).
