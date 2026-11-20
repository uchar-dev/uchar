# role-article-in-explanation: de uitleg schrijft "de recht" voor een rol die als "het recht" is gedeclareerd

*Opgesteld door een werkbanksessie op 2026-11-20, op verzoek van de eigenaar, als vondst uit de ronde van de marker
`niet van toepassing` op de herlevingsdatum (regelset v2/herleving-ww). Verwant aan de toolchain-kaart
`initialisation-reason-reads-zijn` (2026-11-20), die "omdat de bergloon …" in "omdat zijn bergloon …" veranderde.*

## Aanleiding

De regelset v2 declareert de rol in `v2/fundament/rules/fnd-06-feittypen.rgs` (regelspraak-rules, main 229d744):

    het recht (mv: rechten) RechtOpUitkering

Een voorwaarde over die rol, in `v2/herleving-ww/rules/hww-07-herlevingsdatum.rgs`:

    - het recht van de RechtOpUitkeringPeriode heeft de indicatie herleving recht op uitkering

De uitleg (toolchain 17739877, uchar 2c883ab74; `bin/regelspraak scenario --explanation`) maakt van de niet-vervulde
voorwaarde:

    P1 heeft geen berekende herlevingsdatum, want geen regel gold; de regel 'herlevingsdatum art. 21 lid 1' gold niet,
    want niet geldt: de recht de indicatie herleving recht op uitkering heeft

"de recht" is fout: de declaratie zegt "het recht". Gemeten 2026-11-20 op main van de regelset: 23 keer in de uitleg
van een inventaris van scenario's (werkbank, `werknotities/20261120-herleving-marker/meting/meet-gronden-basis.log`,
o.a. r. 44, 89, 134); het komt in elke uitleg van een kenmerkcontrole via die rol. De uitleg is invoer voor een
taalmodel en voor de werkbank, die haar letterlijk toont.

De zin ontstaat in de toolchain (`toolchain/src/main/java/dev/uchar/regelspraak/toolchain/engine/Verbaliser.java`).
Niet vastgesteld: of de IR van uchar het gedeclareerde lidwoord van een rol meegeeft (dan is het een toolchainfout),
of niet (dan moet de IR het eerst dragen).

## Goal

Een zin van de uitleg die een rol noemt, gebruikt een lidwoord dat met de declaratie klopt, of een vorm zonder
lidwoord zoals de initialisatie ("zijn recht"). De bouwer meet waar het lidwoord verloren gaat, stelt de vorm voor
(gedeclareerd lidwoord, of "zijn"/"haar" zoals `initialisation-reason-reads-zijn`), de hoofdsessie besluit.

## Acceptance

- Een golden of negatieve proef met een rol die als het-woord is gedeclareerd, en een kenmerkcontrole via die rol in
  een voorwaarde die niet geldt: de uitleg schrijft geen "de <het-woord>".
- Het geval hierboven, op de regelset v2 van regelspraak-rules: de 23 zinnen zijn goed; geen andere zin van de uitleg
  verandert, behalve dezelfde fout elders (die noemt de Result).
- Gates groen; WERKBANK.md voor de werkbank met de nieuwe zin, zodat de werkbank haar verwachtingen en opnames kan
  volgen.

## Omvang

Klein: één vorm in de verbalisering, eventueel een veld in de IR. Geen gedrag van de regels.

## Result

*Hoofdsessie, 2026-11-20.*

- **Gemeten: het IR draagt het lidwoord van een rol niet.** `Role` in `spec/schema/uchar-ir-v1.schema.json` heeft
  `name`, `objectType`, `plural`, `many`, `origin` en `designations`. Het frontend bewaart het gedeclareerde lidwoord
  wel (`FactType.roleArticles`, gevuld in `DefinitionResolver`), maar alleen op de route vanaf de brontekst en voor zijn
  eigen meldingen (`definiteRole`); `IrLoader.factType` vult het niet. De uitleg van de toolchain leest het model van de
  lader (`Verbaliser.Document`), dus het lidwoord gaat op de draad verloren. `Verbaliser.who` schreef daarom een vast
  "de " vóór elke rol.
- **Voorlopig besluit van de hoofdsessie: "zijn <rol>", geen veld op de draad.** De eigenaar koos dezelfde dag dezelfde
  vorm voor het attribuut van een initialisatie (toolchain-kaart `initialisation-reason-reads-zijn`: "zijn" klopt voor
  elk woord, en het gedeclareerde lidwoord tonen vraagt dat uchar het bewaart). De navigatie van een kenmerkcheck is
  op de draad één stap (IR-SPEC ch. 4.1a, `CharacteristicCheck.navigation`), dus "zijn <rol>" dekt elke check die het
  IR kan dragen. Een lidwoord op de draad is een nieuw presentatieveld: schema, IR-SPEC, emitter, lader, de
  presentatiepoot van gate 13 met haar vloeren, de re-emissie van elke golden met een rol en de migratie van `toka/`.
  Dat is niet de omvang van deze kaart. Wil de eigenaar "het recht" in de zin, dan vraagt dat dat veld, en dan voor
  rol, objecttype en attribuut samen.
- **In uchar verandert alleen deze kaart**: geen bron, geen golden, geen register, geen gate-script. De wijziging staat
  in de toolchain: commit `b99c9af14`, kaart `backlog/done/20261120-role-check-reads-zijn.md` daar, met de metingen
  in haar Result. De pin van de toolchain blijft `2c883ab74`.

Per punt van de Acceptance:

- **De proef met een het-woord.** Geen golden in uchar, want uchar heeft geen uitleg; de proef is de toolchaintest
  `ExplanationFollowsCharacteristicKindTest.a_role_declared_with_het_stands_after_zijn`: `het recht Recht`, en de
  regel "Een Uitkeringsperiode is herleefd indien het recht van de Uitkeringsperiode de indicatie herleving heeft",
  op een object waar de voorwaarde geldt en op een waar zij niet geldt. Eerst rood getoond op de onveranderde
  `Verbaliser` ("P2 is niet herleefd, want niet geldt: de recht de indicatie herleving heeft"), daarna groen ("…: zijn
  recht de indicatie herleving heeft").
- **De regelset v2** (regelspraak-rules 229d744, de meetcasus `meet-gronden.rgs` van de werkbank en de acht
  scenariobestanden van `v2/herleving-ww`; vóór en na in `werk/role-article-in-explanation/meting/`). De 23 van de
  kaart zijn regels die `grep "de recht "` telt: 21 zinnen met het foute lidwoord en 2 regels met het wetscitaat "het
  oude recht". De 21 zeggen nu "zijn recht de indicatie herleving recht op uitkering heeft"; de citaten zijn
  ongewijzigd. Over alle negen bestanden bevat geen zin meer "de recht" of "de eerdere recht" als woorden.
- **Afwijking van "geen andere zin verandert".** De gekozen vorm raakt elke kenmerkcheck over een rol, ook waar "de"
  goed was. Over de negen bestanden veranderen 397 zinnen, elk alleen in "de" → "zijn" direct vóór een rol, 614 keer:
  `recht` 321 en `eerdere recht` 74 (het-woorden, fout), `aanvraag uitkering` 186 en `uitkeringsduur` 33 (de-woorden,
  waren goed). Geen ander woord van enige zin verandert.
- **Dezelfde fout elders, meegenomen.** De reden waarom een initialisatie in een stapel niet besliste, schreef een vast
  "de" vóór het attribuut: "'bergloon zonder gewicht' gold niet, want de bergloon had een invoerwaarde" bij `het
  bergloon`. De toolchain-kaart van vanmiddag veranderde de twee zinnen ernaast en miste deze; geen test legde haar
  vast. Zij zegt nu "want zijn bergloon had een invoerwaarde" (en "… had al een waarde"), met de test
  `ExplanationDefaultsComputationTest.an_initialisation_that_waits_for_the_input_names_its_attribute_with_zijn`. In
  de meting op v2 komt deze zin niet voor.
- **WERKBANK.md**: `werk/role-article-in-explanation/WERKBANK.md`, met de oude en de nieuwe zin en de tellingen.

Gezien en gelaten:

- De terugval "de <attribuut>" en "de <parameter>" voor een lezing zonder brontekst (`Verbaliser.describe`) schrijft
  nog een vast "de". De kaart van vanmiddag hield haar buiten de omvang; "zijn" past daar niet zonder meer, want de
  lezing kan over een rol gaan en een parameter heeft geen bezitter.
- In de zin van een verdeling op een ontvanger leest de voorwaarde op de bron. "Zijn <rol>" verwijst daar naar de
  bron, zoals "zijn <attribuut>" in een geciteerde lezing op die plek al doet.
- `FactType.roleArticles` en `ObjectType.article` bestaan alleen op de route vanaf de brontekst; het model van de
  lader heeft ze niet. Geen uitvoerder leest ze.

Gates: de toolchain draait haar eigen set (`make gates`, de uitkomst staat in de Result van de kaart daar). In uchar
verandert alleen deze kaart, dus alleen gate 1 (`scripts/check-self-contained.sh`) is gedraaid, groen; de volle
`make gates` niet.
