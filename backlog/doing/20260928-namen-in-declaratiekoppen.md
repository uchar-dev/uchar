# namen-in-declaratiekoppen: voorzetsels in objecttype- en meervoudsnamen, werkwoorden in feittypenamen, en geen stille herlezing van een declaratie

*Voorstel uit het naamonderzoek van 2026-09-28 (werkbank-hoofdsessie, alleen
lezend op `43caed71`; rapport bij de maintainer). Aangemaakt op verzoek van de
maintainer, die de kaart zelf bouwt; nog niet geclaimd. Reproducties op
Jutterswet-materiaal. K7, kleingeschreven 'van' in objecttypenamen met
voorrang voor de gedeclareerde naam in de navigatie, is op 2026-09-28 door de
maintainer geschrapt: CGM-namen worden benamingen in een vocabulaire (kaart
`vocabulaires-benamingen`, geland in 43caed71) en vragen dit niet.*

## Aanleiding

De frontend bouwt namen woord voor woord op en gebruikt de hoofdletter als
grens tussen naam en type (`RegelSpraakLexer.g4:16-24`, noten 1 en 2). Een
objecttypenaam met een kleingeschreven voorzetsel kapt af bij het voorzetsel;
de rest wordt als attribuutdeclaratie gelezen, en zonder `(mv:`-groep gebeurt
dat zonder enige melding (R2). Een feittypenaam met 'heeft' kapt af (R4). Een
lid dat na een afgekapte naam begint, levert een tweede declaratie op (R3,
R5); een rolvuller die geen objecttype is, passeert de frontend (R6; alleen de
verifier meldt WF-003, en `check` van de distributie draait die niet).
Regeltekst leest voorzetsels in objecttypenamen al, hoofdletterongevoelig (R7,
gemeten tegen een Title-Case-kop): de beperking zit alleen in de koppen.

De specificatie begrenst deze namen met regeleinde en tab (§13.3.1.1,
§13.3.2.5, §13.3.9.1; §13.1.8 noemt regeleinde en tab controletekens) en kent
geen hoofdletterregel (nul treffers op 'hoofdlet', 'kleine letter', 'kapitaal'
in de vier documenten); §3.11, §9.3 en §9.4 tonen feittypenamen met
voorzetsels en een objecttype met wisselende hoofdletter. De afkapping is dus
een keuze van deze frontend, geen reconstructie van een gebrekkige
specproductie, en `DIVERGENCES.md` registreert haar niet (wel de versmalling
door elke nieuwe token, `:611-620`). Gemeten 2026-09-28 op `43caed71`;
telling over 643 verschillende `.rgs`-bronnen (corpus, `toka/`, distributie,
bronnen buiten de repository): 0 objecttypekoppen met tekst achter de
naamgroep, 0 koppen zonder `(mv:` gevolgd door een lid zonder lidwoord, 0
rolregels zonder lidwoord, 0 feittypekoppen met een lidwoord, 448 van 448
`(mv:`-groepen op hun eigen regel gesloten.

## Besluiten (voorstel; de maintainer beslist)

- K1: een lid (attribuut, kenmerk) of rol die begint op de regel waar de kop,
  het vorige lid of de vorige rol eindigde, is een ERROR op die regel. De
  melding noemt het woord en het token waar de vorige naam eindigde en, bij
  een sleutelwoord, de schrijfwijze die wel werkt (een koppelteken). Een lid
  dat op de volgende regel doorloopt, blijft toegelaten (specvoorbeeld §3.9).
- K2: de vuller van een rol is een gedeclareerd objecttype, anders ERROR in
  de frontend (WF-003; nu meldt alleen de verifier het).
- K3: `objectTypeNaam` (`RegelSpraak.g4:365`) laat na het eerste `UPPER_ID`
  ook op, bij, in, voor, over, tot, 'tot en met', per en met toe, op elke
  plaats waar de productie staat (kop, extensie, `is een`, rolvuller,
  scenario-instantie); 'van', de lidwoorden en de overige sleutelwoorden
  blijven buiten.
- K4: `meervoudsvorm` (`:367`) laat elk woord van `kaleNaamWoord` toe tot het
  sluithaakje.
- K5: `feitTypeNaamWoord` (`:586-589`) laat de werkwoorden heeft, hebben, is
  en zijn toe, en de overige woordtokens behalve de lidwoorden de en het (die
  openen de rolregel) en één en meerdere; de bouwer legt de set vast met de
  meting van (c).
- K6: een gedeclareerde attribuut-, kenmerk- of rolnaam die regeltekst niet
  als naam leest (zoals een naam met 'als'), geeft een WARNING die het woord
  noemt; de naam wordt daarvoor opnieuw gelext en moet als `kaleNaam` te
  lezen zijn. Geen ERROR: één bestaande bron draagt zo'n naam.

## Acceptatie

- (a) R1, R2, R4 en R7 lossen op met de namen zoals geschreven; R3, R5 en
  R6 geven een ERROR op de genoemde regel.
- (b) JUnit-getuigen per K; 'Recht op premie' werkt als kop, als rolvuller,
  in een regeldoel, een lezing, een voorwaarde en een scenario-instantie
  (`een Recht op premie R1`).
- (c) Inertie met de methode van `DIVERGENCES.md` §6 over de corpusbronnen,
  `toka/` en de bronnen buiten de repository: IR bytegelijk, diagnostiek
  gelijk op de 'expecting {…}'-tekst na; het Result noemt de tellingen.
- (d) Een rgs-golden (bijvoorbeeld `jutterswet-premierecht`) met 'Recht op
  premie', rolmeervoud 'rechten op premie' en feittype 'jutter heeft
  premierecht', toegelaten via `scripts/admit.sh`.
- (e) Een sectie "Namen in declaratiekoppen" in `DIVERGENCES.md`: de
  woordensets per positie, de resterende beperking (hoofdlettergrens in
  attribuut- en rolregels, sleutelwoorden, kleingeschreven 'van' in
  typenamen) en de meting.
- (f) Alle zestien gates groen, in het bijzonder 3, 5, 6, 10, 11, 12 en 16.

## Mag raken

`frontends/regelspraak/grammar/RegelSpraak.g4`,
`frontends/regelspraak/grammar/DIVERGENCES.md`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/DefinitionResolver.java`
(en `model/RegelSpraakModel.java` en `resolver/ScenarioResolver.java` als
dat nodig is), `frontends/regelspraak/src/test/`,
`corpus/golden/<nieuwe golden>/`. Niet: `RegelSpraakLexer.g4`, `spec/`,
`backends/`, `ref/`, `toka/`.

## Niet in deze kaart

Attribuutnamen met een hoofdletterwoord (maintainersbesluit 2026-09-28:
later een eigen kaart zonder nieuwe syntaxis, tot dan kleinschrijven);
namen met een sleutelwoord (besluit: saneren met koppeltekens, 'premie-en-toeslag'
werkt in declaratie en regeltekst); kleingeschreven 'van' in
objecttypenamen (maintainersbesluit 2026-09-28: blijft buiten, een
koppelteken werkt; de CGM-namen worden benamingen in een vocabulaire en
vragen dit niet); domeinnamen met voorzetsels; een
lexermode voor koppen; gequoteerde namen; regeltekst lexen met de
gedeclareerde namen als tokens.

## Minimale reproductie

`43caed71`, `bin/regelspraak check`; basis B = de eerste vier regels van R4;
`/` scheidt regels.

    R1  Objecttype het Recht op premie (mv: Rechten op premie)
            de premie Numeriek (geheel getal);
        nu: ERROR regel 1 "mismatched input 'op' expecting {RPAREN, UPPER_ID, LOWER_ID}";
        naam 'Recht', 'op premie' gelezen als attribuut
    R2  Objecttype de Vondst bij eb
            de waarde Numeriek (geheel getal);
        nu: exit 0; objecttype 'Vondst' met één attribuut 'bij eb de waarde'. Na de kaart: kop 'Vondst bij eb'
    R3  Domein BK is van het type Numeriek (geheel getal)
        Objecttype de Jutter (mv: Jutters) (bezield)
            de datum einde seizoen BK bij eb Datum in dagen;
        nu: exit 0; attributen 'datum einde seizoen' (BK) en 'bij eb' (Datum in dagen). Na de kaart: ERROR regel 3 (K1)
    R4  Objecttype de Jutter (mv: Jutters) (bezield)
            de naam Tekst;
        Objecttype de Vondst (mv: Vondsten)
            de waarde Numeriek (geheel getal);
        Feittype jutter heeft vondst
            de vinder Jutter
            de vondst (mv: vondsten) Vondst
        Eén vinder heeft meerdere vondsten
        nu: ERROR "extraneous input 'heeft'"; WARNING rolnaam 'vondst de vinder'. Na de kaart: feittype 'jutter heeft vondst'
    R5  B + Feittype vondst voor de jutter / de vinder Jutter / de vondst (mv: vondsten) Vondst / Eén vinder heeft meerdere vondsten
        nu: exit 0 met WARNING; feittype 'vondst voor', rol 'jutter de vinder'. Na de kaart: ERROR (K1)
    R6  B + Feittype vondstrelatie / de vinder BK Jutter / de vondst (mv: vondsten) Vondst / Eén vinder heeft meerdere vondsten
        nu: exit 0; rolvuller 'BK Jutter' (verifier: WF-003 FAIL). Na de kaart: ERROR (K2)
    R7  R1 met extra lid 'de waarde Numeriek (geheel getal);' en de regel
        Regel premie / geldig altijd / De premie van een Recht op premie moet berekend worden als de waarde van het Recht op premie plus 1.
        nu gemeten met kop 'Recht Op Premie': lost op. Na de kaart: lost op met kop 'Recht op premie'
