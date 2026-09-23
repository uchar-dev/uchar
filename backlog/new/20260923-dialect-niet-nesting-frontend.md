# dialect-niet-nesting-frontend: "niet" en geneste uitzonderingen in de RegelSpraak-frontend

*Status: concept, 23 september 2026 — wacht op de besluiten 1–8 van
spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md. Gesneden door de
onderzoekskaart dialect-defaults-niet-en-nesting; de inhoud volgt de
voorgestelde besluiten en wordt bijgesteld waar de maintainer anders
beslist.*

Doel: de RegelSpraak-frontend schrijft vorm 1 (een negatieve
kenmerkbeslissing in een uitzondering → `besluit: false`) en vorm 2 (een
uitzondering op een uitzondering → geneste `Default.excepties`, en
`Uitzonderingen ongeordend op <naam>:`), met de spellingen, weigeringen en
meldingen van het voorstel (§4, §5, §7.4). Draad, verifier en uitvoerders
veranderen niet: ref, clingo en de Drools-exectak dragen beide vormen al
(voorstel §2.2, metingen M8–M12); de DRL-teksttak weigert elk
defaults-document, zoals nu.

## Werk

- **Grammatica** (`frontends/regelspraak/grammar/RegelSpraak.g4`): één
  alternatief `onderwerpKeten IS NIET EEN kaleNaam` in
  `kenmerkToekenning`; een optioneel `OP uitzonderingBasisNaam` vóór de
  dubbele punt in `uitzonderingGroep`; `uitzonderingRegel` en
  `uitzonderingGroep` erbij in `testDocument` (het grammaticadeel van
  bijvangst 3; het scenariodeel hoort bij regelspraak-java). Geen nieuwe
  token, geen gereserveerd naamwoord.
- **RuleChecker**: de ontkenning in `kenmerk()` via de matcher van de
  voorwaardekant (verbatim eerst, dan `niet `/`geen ` afpellen); de
  polariteit naar `besluit`; `ordeningsDefaults` wordt een boombouw (alle
  blokken eerst oplossen, ouderkaart, kringen met hun pad weigeren, doel,
  venster en versie per rand toetsen, van onder naar boven bouwen, de
  gelifte regels pas aan het eind uit `resolvedRegels` halen); het
  verlagingsschema van voorstel §5.2 (eerste niveau een ketenpositie,
  dieper `excepties`, ongeordend op een basis als groep-default direct
  boven de basis); de weigeringen F1–F6 en de waarschuwing F7 (bij besluit
  4(d) optie A); een geneste uitzondering onder een geërfde uitzondering
  geweigerd (besluit 5).
- **Meldingen**: die van voorstel §4.5 vervangen de misleidende meldingen
  van meting 1 en meting 2.
- **DIVERGENCES.md §4**: de zinnen "the frontend never emits `besluit:
  false`" (r. 91-92), "a genuine §9.5 conflict is only expressible on an
  attribute target" (r. 101-103) en "Exception-on-exception … is not
  authored from source" (r. 129-130) vervangen door de spellingstabel, de
  toegelaten posities, het verlagingsschema, de gekozen lezing van besluit
  4(d), de weigeringen en de inertiemeting.
- De javadoc van `ir/PrioritizedDefaults.java` ("the RegelSpraak resolver
  never builds it", onjuist sinds 2026-08-29) gecorrigeerd.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood eerst: de proeven M1–M4 van het voorstel (meting 1, meting 2,
  "niet" in een gewone regel, `is niet een`) geven de oude meldingen; na
  de kaart de nieuwe, of de verwachte verlaging.
- (b) Fixtures F1–F8 naast `DefaultsExtensieTest`, elk met een vastgepind
  meldingsfragment; positieve fixtures die de draadvorm van §5.2 vastleggen
  (eerste niveau keten, genest, ongeordend op een uitzondering, ongeordend
  op een basis, `besluit: false` op elk niveau).
- (c) Inertie gemeten zoals DIVERGENCES §6: alle vlakke documenten van de
  daar genoemde collecties emitteren vóór en na byte-identiek; het aantal
  en de collecties komen in DIVERGENCES.
- (d) Alle veertien poorten groen; de goldens van gate 10 byte-identiek;
  gate 11 ongewijzigd.

May touch: `frontends/regelspraak/grammar/**`,
`frontends/regelspraak/src/main/java/**/resolver/**`,
`frontends/regelspraak/src/main/java/**/ir/PrioritizedDefaults.java`,
nieuwe testklassen onder `frontends/regelspraak/src/test/java/**`,
kaartverplaatsing. Bestaande testbestanden alleen met toestemming van de
maintainer: `DefaultsExtensieTest.onbekende_basisregel_weigert_luid` pint
de melding "geen basisregel met die naam gevonden", die dit werk vervangt.
Niet in deze kaart: corpus, gatescripts, spec. Serieel met
bron-annotaties-defaults en regelgroep-uitzondering-afsluiting (zelfde
RuleChecker).
