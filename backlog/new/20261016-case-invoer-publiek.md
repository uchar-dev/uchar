# case-invoer-publiek: de invoerstap van een case als publieke ingang van de Drools-backend

*Op verzoek van de eigenaar (2026-10-16). Kaart F van de reeks "beslisservice via dienst" (voorgangers
`waarnemer-vuur` en `vergelijking-publiek`): de werkbank laat scenario's rekenen door de beslisservice van de
distributie, en hoe een invoer beans wordt, moet dan van uchar komen. Regelnummers op `a3ac41fe`.*

Goal: de invoerstap van een case is privé in `CaseRunner`
(`backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseRunner.java`): `beanVoor` (r. 576),
`zetAttributenEnKenmerken` (r. 624), `zetRollen` (r. 679), `spiegelWederkerigeRollen` (r. 925), de lijstvolgorde
(`Lijst`, `lijst`, `voegInvoerIn`, r. 777–816) en de tijdlijninvoer (`temporeleInvoer`, `kenmerkTijdreeks`). Elke
ingang van de replay roept die vier stappen achter elkaar aan (`invoerBeans`, `naFire`, `metSessie`, `fireRood`,
`runCase`). Een afnemer kan ze niet aanroepen. De distributie heeft daarom een kopie van het rolschrijven
(`RolSchrijver`, gebruikt door `CaseSessie` en door de dienst via `VerzoekMapper`): het zusterveld, het verre veld van
een 1:1-relatie, de spiegel van een wederkerige relatie en de lijstvolgorde van RUNTIME-SEMANTICS ch. 5.7a, buiten
uchar. Kaart G (distributie) schrapt die kopie; deze kaart maakt de ingang waarmee dat kan.

De kaart maakt publiek, in de nieuwe klasse `backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseInvoer.java`:

1. `CaseInvoer.van(DroolsBackend.Build build, JsonNode c)`: leest `asOfDate` en `input.instances` van een
   `uchar-case-v0`-case op verse beans van het beanmodel van de build, zoals de replay dat nu doet.
2. `beans()` (label → bean, in de volgorde van de case), `typen()` (label → objecttype) en `invoer()` (een
   `Invoer` voor `DroolsBackend.voegIn`: de beans in die volgorde, de emmerscalairs en elke to-many-lijst in de volgorde
   van de replay).
3. Weigeringen van een rollink met de instantie (`label`), de rol zoals de case haar schrijft (`rol`) en het genoemde
   doel (`doel`), zodat een afnemer met een eigen verzoek er een veldpad van kan maken.

De code van de invoerstap verhuist uit `CaseRunner` naar `CaseInvoer`; de waardelezer die de vergelijking ermee deelt
(`waardeNaarArg`, `tijdreeks`, `rooster`, …) blijft in `CaseRunner`. Elke ingang van de replay roept `CaseInvoer.van`
aan. Geen nieuwe vormen, geen wijziging van spec, schema of corpus.

Keuze over de invoer van de ingang: het invoerdeel van een `uchar-case-v0`-case, ook voor de dienst. De dienst
valideert zijn verzoek zelf (namen, typen, eenheden; een 400 met veldpad) en zet het gevalideerde verzoek om naar
`instances` met de ids als labels. Het alternatief, een ingang die beans aanneemt die de aanroeper maakte, laat het
maken van beans en het zetten van attributen in de distributie en geeft uchar twee invoercontracten.

Acceptance:
- (a) De publieke ingang: `public static CaseInvoer van(DroolsBackend.Build, JsonNode)`, `Map<String, Object> beans()`,
  `Map<String, Objecttype> typen()`, `Invoer invoer()`. Weigeringen, elk door `van`, dus vóór er iets in een sessie
  gaat:
  - een tweede, ander doel op een to-one referentieveld (WF-013): voorwaarts, op het verre veld van een 1:1-relatie, op
    het zusterveld van een to-many rol, en bij de spiegel van een wederkerige rol: `CaseInvoer.Rolbotsing`, een
    `IllegalArgumentException`, met `label()`, `rol()` en `doel()` van de link die de case schreef;
  - een link die het beanmodel niet kan dragen (een onbekend doellabel, geen to-one referentieveld, de
    naamdelingsgrens): `CaseInvoer.Rolweigering`, een `CaseRunner.Excluded`, met dezelfde drie velden;
  - beide met de zin van nu, zodat het rapport en de replay dezelfde reden geven; ze delen de interface
    `CaseInvoer.Rolfout`.
- (b) `CaseRunner` leest elke case via `CaseInvoer.van`; de vier stappen en de lijstvolgorde staan niet meer in
  `CaseRunner`. Meting buiten de gates: het harnas `RapportDump` (één JVM, `CaseRunner.rapport` per golden met
  case-bestanden, één regel `ScenarioResultaat.toString()` per case) met de jar van `a3ac41fe` en met die van de
  eindstand. Nulmeting op `a3ac41fe`: 300 goldens, 823 rapporten (695 PASS, 128 MARKER), twee runs bytegelijk (sha1
  `405dd7c691b6a3db576c697501930023631a1b99`). De eindstand moet bytegelijk zijn.
- (c) Gate 12 groen met ongewijzigde pins en lijsten (het script blijft ongewijzigd); `ZusterrolInvoerWitnessTest`,
  `OpslagrichtingWitnessTest` en `WederkerigZelfpaarWitnessTest` ongewijzigd groen.
- (d) Nieuwe testklasse `backends/drools/src/test/java/dev/uchar/regelspraak/cases/CaseInvoerWitnessTest.java`, die
  alleen de publieke ingang en `DroolsBackend` gebruikt: de gevallen van ch. 5.7a (één poot, beide poten, de to-one
  botsing op elk van de vier plaatsen, wederkerig, de lijstvolgorde), elke weigering met haar velden, labels die ids van
  een verzoek zijn, en `invoer()` tegen `voegIn` en de replay. Eerst rood waar het kan: elke rij valt tegen een
  tijdelijke mutatie (gemeten, niet bewaard). Geen bestaand testbestand gewijzigd.
- (e) `backends/drools/README.md`, "A session in four calls", stap 2, noemt `CaseInvoer`.
- (f) Alle 16 gates van `CLAUDE.md` groen op de eindstand van de branch; gate 12 met de nieuwe tests in de
  JUnit-telling.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseRunner.java`, de nieuwe klasse
`backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseInvoer.java`,
`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java` (alleen Javadoc), de nieuwe testklasse
`backends/drools/src/test/java/dev/uchar/regelspraak/cases/CaseInvoerWitnessTest.java`, `backends/drools/README.md`,
deze kaart.

Out of scope:
- De parameters en de rekendatum als globals (`zetGlobals`): die zet de aanroeper op de sessie; `CaseInvoer` leest
  alleen de instanties.
- Een andere rolresolutie of lijstvolgorde: de kaart verhuist, ze verandert niets.
- De distributie (`RolSchrijver`, `CaseSessie`, `VerzoekMapper`, `Wereld`): kaart G, na een pin-bump.
- `spec/`, het schema, het corpus, de gate-scripts.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.
