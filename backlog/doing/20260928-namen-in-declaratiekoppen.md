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

## Result

*Bouwer, 2026-09-28, branch `card/namen-in-declaratiekoppen` op basis `16131b9f`. De
kop van de branch staat onder "Commits". De commit met de gate-12-pinnen raakt een
beschermd pad (`scripts/`); de hoofdsessie landt haar.*

### Wat er landde

K3, K4 en K5 in `RegelSpraak.g4` (drie producties verbreed, geen nieuw token, de lexer
onaangeroerd), K1, K2 en K6 in `DefinitionResolver.java`, zeventien JUnit-getuigen in
`DeclaratiekopNaamTest`, de golden `corpus/golden/jutterswet-premierecht/` (toegelaten via
`scripts/admit.sh`, daarna met `scenario.rgs`), de sectie "Namen in declaratiekoppen" in
`frontends/regelspraak/grammar/DIVERGENCES.md`, en de gate-12-pinnen 217 → 218 en
187 → 188. `RegelSpraakModel.java` en `ScenarioResolver.java` hoefden niet te veranderen:
de scenario-instantie `een Recht op premie R1` leest via de verbrede `objectTypeNaam`, en
`ScenarioResolver` zoekt de typenaam al hoofdletterongevoelig op. De IR veranderde niet;
`spec/`, `backends/`, `ref/`, de pariteitszone en de lexer zijn niet geraakt.

### Woordensets per positie, met de reden van elk uitgesloten token

- **Objecttypenaam** (`objectTypeNaam`, K3): `UPPER_ID ( UPPER_ID | LOWER_ID | op | bij | in
  | voor | over | tot | tot en met | per | met )*`. Dat zijn precies de voorzetsels van de
  kaart, en het zijn woorden die `kaleNaamWoord` al leest, dus een gedeclareerde naam leest
  in regeltekst terug. Buiten blijven: `van` (het navigatiescheidingsteken van regeltekst;
  een typenaam met `van` leest daar niet terug), de lidwoorden en alle overige
  sleutelwoorden (kaart K3). De productie staat in kop, extensiekop, `is een`, rolvuller en
  scenario-instantie (en in de twee harnasproducties van `corpusEntry`); de woorden gelden
  op elk van die plaatsen.
- **Meervoud** (`meervoudsvorm`, K4): `kaleNaamWoord+`, ook voor het eerste woord (de kaart
  zegt "elk woord"; het sluithaakje begrenst, dus geen enkele lezing erna verandert). Niet
  `van` en niet `als`: die staan niet in `kaleNaamWoord`, want regeltekst leest ze niet in
  een naam.
- **Feittypenaam** (`feitTypeNaamWoord`, K5): `LOWER_ID`, `UPPER_ID`, `NUMBER`, `tot en met`
  en elk token waarvan elke spelling één woord is, behalve de hieronder uitgesloten. Dus
  onder meer `heeft`, `hebben`, `is`, `zijn`, `een`, `van`, `vanaf`, `uit`, `en`, `of`, `er`,
  `dat`, `die`, `niet`, `als`, `alle`, `twee`/`drie`/`vier`, `aantal`, `kenmerk`, de
  vergelijkings-, reken- en afrondingswoorden, de tijdeenheden, de datatypewoorden
  (`Numeriek`, `Tekst`, …) en `Rekendatum`/`Rekenjaar`/`Geldig`. Uitgesloten, elk met zijn
  reden:
  - `de`, `het` (DE, HET, ook met hoofdletter): openen de rolregel (kaart K5; §13.3.9.1).
  - `één`, `meerdere` (EEN_TELWOORD, MEERDERE): openen de kardinaliteitsregel (kaart K5).
  - De zestien blokopeners `Objecttype`, `Domein`, `Dimensie`, `Eenheidsysteem`,
    `Parameter`, `Feittype`, `Dagsoort`, `Vocabulaire`, `Tijdlijn`, `Regelgroep`,
    `Uitzonderingen`, `Regel`, `Beslistabel`, `Situatie`, `Parameterset`, `Scenario`: een
    feittype zonder rollen moet op het volgende blok falen, niet diens sleutelwoord in zijn
    naam opnemen. Getuige: `Feittype vondstrelatie` / `Objecttype de Jutter …` geeft de
    eerste syntaxfout op regel 2, `syntaxfout: extraneous input 'Objecttype' …`
    (`de_rolregel_en_een_volgend_blok_beeindigen_de_feittypenaam`).
  - De frasetokens (meer woorden in één token, ook `GEHEEL_GETAL` en `TEN_MINSTE`, die
    daarnaast een aaneengeschreven spelling hebben): geen woord, en hun frasen nemen
    lidwoorden en woorden mee. `tot en met` is de ene uitzondering: het is een naamwoord in
    elke andere naamproductie en in K3.
  - Tekens en merktekens (`(mv:`, `(bezield)`, `(bezittelijk)`, `(bijvoeglijk)`,
    `(standaard)`, leestekens, `%`, `€`): geen woord.
  - De meting van (c): geen van de 184 feittypekoppen in de 643 bronnen draagt een token
    buiten de oude set, en de verbreding verandert in de meting alleen de
    'expecting'-verzameling van twee negatieve snippets (hieronder).
- **Attribuut-, kenmerk- en rolnaam** (`lowerNaam`): ongewijzigd.
- **Domeinnaam** (`domeinNaam`): ongewijzigd (kaart: niet in deze kaart).

### Overige technische keuzes, met hun grond

- **Taal van de meldingen.** De opdracht noemt diagnosezinnen in het Engels én "in de stijl
  van de bestaande diagnosezinnen". Alle meldingen van deze frontend zijn Nederlands
  producentproza (L5-kaart, U15: `message` blijft zoals hij is), ook die van de jongste
  kaarten (vocabulaires, opgave); de bouwer van de opgavekaart las de L5-regel ook als
  regel voor wire-meldingen (zijn Result, punt 9). Ik schreef de nieuwe zinnen dus in het
  Nederlands, met `WF-003` in L5-spelling. Grammaticacommentaar, Javadoc en
  toetscommentaar zijn Engels; de toetsnamen volgen de bestaande dialecttoetsen
  (Nederlandse methodenamen).
- **Vorm van de K1-melding.** Eén zin voor elke situatie, met de onderdelen die de kaart
  vraagt: het woord waarmee het lid of de rol begint, het token waarop de vorige declaratie
  eindigt, en de vorige declaratie zelf (`de kop`, `het vorige lid '<naam>'`, `de vorige
  rol '<naam>'`), zodat de auteur ziet wat als aparte declaratie gelezen is. De
  koppeltekenhint staat er alleen waar hij klopt: waar de vorige declaratie op een naam
  eindigt (een kop zonder annotaties, of een rol, die op haar vuller eindigt) en het woord
  een sleutelwoord is, met het voorbeeld `'<token>-<woord>'`; en waar het vorige lid een
  kenmerk is dat op `kenmerk` eindigt, met `'kenmerk-<woord>'`. Na een attribuut dat op
  zijn type eindigt (`BK`, `Tekst`) zou een koppelteken aan het type niets oplossen, dus
  daar staat geen hint. De regelnummers van de tokens beslissen; een `---`-koptekst loopt
  tot het regeleinde en telt niet als lid.
- **Herstelde definities.** Waar de parser binnen een objecttype, extensie of feittype
  moest herstellen (een `ErrorNode` of een regel met `exception`), slaan K1, K2 en K6 die
  definitie over: de grenzen zijn die van het herstel. Gemeten op een feittype zonder rollen
  gevolgd door een objecttype: zonder deze regel kwamen er drie onzinmeldingen naast de
  syntaxfout (`de rol 'de' begint met 'de' …`, `('Jutter-(mv:')`).
- **K6 naast K1.** Een lid of rol die K1 weigert, krijgt geen K6-waarschuwing: het is een
  misgelezen declaratie, niet de naam die de auteur schreef.
- **De bestaande rolnaamheuristiek** (`DefinitionResolver` rond de oude regels 911-924)
  blijft staan, naast K1 ("aanvult"): K1 noemt regel en woord, de heuristiek de
  waarschijnlijke oorzaak, en alleen de heuristiek ziet een rol die over de volgende regel
  doorloopt. De modeltoets van de pariteitszone
  (`ModelTest.feittype_naam_met_lidwoord_waarschuwt_voor_de_aangetaste_rol`) pint die
  WARNING; toen ik haar in een eerste versie wegliet waar K1 weigerde, werd gate 11 rood.
- **Hoe K6 opnieuw lext.** De gedeclareerde naam (het bronfragment met ingeklapte
  witruimte) gaat alleen door `RegelSpraakLexer`, en de parser leest haar met de regel
  `kaleNaam` onder `BailErrorStrategy`. Het woord is het token waarop de parser faalt, of
  het eerste token na wat `kaleNaam` nam als dat niet EOF is. Het voorbeeld verbindt dat
  woord met een koppelteken aan zijn linkerbuur (aan de rechterbuur als het vooraan staat).
  Zo is de grammatica de enige bron van de woordenset; er is geen tweede lijst.
- **K2 als ERROR of WARNING**: zie "Normvragen".
- **De golden draagt `scenario.rgs`.** Buiten de lijst van (d), bewust: zo getuigt het corpus
  zelf van `een Recht op premie R1`, en gate 16 regenereert de twee casebestanden uit de
  handgeschreven verwachtingen. `scripts/admit.sh` kopieert `scenario.rgs` niet; ik voegde
  het na de toelating toe en draaide gate 16 en 10 opnieuw (zie open punten).

### De meldingen uit echte uitvoer, met hun reproductie

Uitvoer van `Resolver --ir` van de frontend op de kop van de branch.

K1, lid op de regel van het vorige lid (R3):

    Domein BK is van het type Numeriek (geheel getal)
    Objecttype de Jutter (mv: Jutters) (bezield)
        de datum einde seizoen BK bij eb Datum in dagen;

    ERROR (line 3): objecttype 'Jutter': het attribuut 'bij eb' begint met 'bij' op de regel waar het vorige lid 'datum einde seizoen' eindigt, direct na 'BK'; een lid begint op een eigen regel

K1, rol op de regel van de kop, met koppeltekenhint (R5; de heuristiek staat ernaast):

    ERROR (line 5): feittype 'vondst voor': de rol 'jutter de vinder' begint met 'de' op de regel waar de kop eindigt, direct na 'voor'; een rol begint op een eigen regel — hoort 'de' bij de naam, verbind het dan met een koppelteken aan het woord ervoor ('voor-de')
    WARNING (line 5): feittype 'vondst voor': de rolnaam 'jutter de vinder' bevat een lidwoord (de/het) na een zelfstandig woord — waarschijnlijk bevat de feittypenaam een 'de'/'het' die de naam afkapt en in de rol wordt geabsorbeerd (§13.3.9.3); hernoem het feittype zonder lidwoord

K1, lid op de kopregel (`Objecttype de Vondst bij eb de waarde Numeriek (geheel getal);`):

    ERROR (line 1): objecttype 'Vondst bij eb': het attribuut 'waarde' begint met 'de' op de regel waar de kop eindigt, direct na 'eb'; een lid begint op een eigen regel — hoort 'de' bij de naam, verbind het dan met een koppelteken aan het woord ervoor ('eb-de')

K1, `van` in de kop (`Objecttype de Vondst van eb` / `de waarde Numeriek (geheel getal);`):

    ERROR (line 1): objecttype 'Vondst': het attribuut 'van eb de waarde' begint met 'van' op de regel waar de kop eindigt, direct na 'Vondst'; een lid begint op een eigen regel — hoort 'van' bij de naam, verbind het dan met een koppelteken aan het woord ervoor ('Vondst-van')

K1, `kenmerk` in een attribuutnaam (`de premie kenmerk strand Numeriek (geheel getal);`):

    ERROR (line 2): objecttype 'Vondst': het attribuut 'strand' begint met 'strand' op de regel waar het vorige lid 'premie' eindigt, direct na 'kenmerk'; een lid begint op een eigen regel — hoort 'kenmerk' bij de naam, verbind het dan met een koppelteken aan het woord erna ('kenmerk-strand')

K2, ERROR waar het document de misslag toont (R6):

    ERROR (line 6): feittype 'vondstrelatie': de rol 'vinder' heeft als objecttype 'BK Jutter', en dat is nergens als objecttype gedeclareerd — WF-003: de vuller van een rol is een gedeclareerd objecttype; 'Jutter' is dat wel: een rolnaam eindigt bij het eerste woord met een hoofdletter, dus 'BK' is als deel van het objecttype gelezen

K2, ERROR voor een vuller die als ander soort gedeclareerd is (rol `de waardering Bedrag`,
`Bedrag` een domein):

    ERROR (line 6): feittype 'vondstwaardering': de rol 'waardering' heeft als objecttype 'Bedrag', en dat is een domein, geen objecttype — WF-003: de vuller van een rol is een gedeclareerd objecttype

K2, WARNING voor een vuller die het document nergens declareert (alleen het feittype
`vondstaanmelding` met de rollen `aanmelder Jutter` en `vondst Vondst`):

    WARNING (line 2): feittype 'vondstaanmelding': de rol 'aanmelder' heeft als objecttype 'Jutter', en dat is nergens als objecttype gedeclareerd — WF-003: de vuller van een rol is een gedeclareerd objecttype; staat het in een ander bestand van het model, lees de bestanden dan samen

K6 (`de premie als strand Numeriek (geheel getal);` en `is van belang kenmerk
(bijvoeglijk);`):

    WARNING (line 2): objecttype 'Vondst': regeltekst leest het attribuut 'premie als strand' niet als één naam, want 'als' is daar geen woord van een naam; verbind het met een koppelteken aan een buurwoord ('premie-als strand')
    WARNING (line 3): objecttype 'Vondst': regeltekst leest het kenmerk 'van belang' niet als één naam, want 'van' is daar geen woord van een naam; verbind het met een koppelteken aan een buurwoord ('van-belang')

### R1–R7: nu en na

"Nu": `bin/regelspraak check` van de distributie (gepind op uchar `43caed71`), zoals de kaart;
de frontend van de basiscommit `16131b9f` gaf dezelfde meldingen. "Na": `Resolver --ir` op de
kop van de branch.

| | nu | na |
|---|---|---|
| R1 | exit 1, `ERROR (line 1): syntaxfout: mismatched input 'op' expecting {RPAREN, UPPER_ID, LOWER_ID}` | exit 0; objecttype `Recht op premie`, meervoud `Rechten op premie`, attribuut `premie` |
| R2 | exit 0; objecttype `Vondst` met één attribuut `bij eb de waarde` | exit 0; objecttype `Vondst bij eb` met attribuut `waarde` |
| R3 | exit 0; attributen `datum einde seizoen` (BK) en `bij eb` | exit 1, ERROR regel 3 (K1, zin hierboven) |
| R4 | exit 1, `syntaxfout: extraneous input 'heeft' …` en WARNING rolnaam `vondst de vinder` | exit 0; feittype `jutter heeft vondst`, rollen `vinder` (Jutter) en `vondst` (Vondst) |
| R5 | exit 0 met WARNING; feittype `vondst voor`, rol `jutter de vinder` | exit 1, ERROR regel 5 (K1) naast de bestaande WARNING |
| R6 | exit 0; rolvuller `BK Jutter` | exit 1, ERROR regel 6 (K2) |
| R7 | exit 1 (de fout van R1); met kop `Recht Op Premie` exit 0 | exit 0 met kop `Recht op premie`; regel `premie` schrijft `Recht op premie.premie` |

### JUnit-getuigen

`frontends/regelspraak/src/test/java/dev/uchar/regelspraak/DeclaratiekopNaamTest.java`, 17
toetsen; gate 10 telde 950 toetsen vóór de kaart en 967 erna.

- K3: `een_typenaam_draagt_de_voorzetsels_van_regeltekst` (alle negen voorzetsels),
  `van_blijft_buiten_de_typenaam_en_een_koppelteken_leest_wel`,
  `recht_op_premie_werkt_op_elke_plaats` (kop, extensiekop, `is een`, rolvuller, regeldoel,
  lezing, voorwaarde en scenario-instantie `een Recht op premie R1`, tot in het casebestand).
- K4: `een_meervoud_loopt_tot_zijn_sluithaakje` (objecttype, attribuut `premies per dag`,
  rol `rechten op premie` met haar `sourceRole` in regeltekst).
- K5: `een_feittypenaam_draagt_werkwoorden` (`heeft`, `is`, `hebben`, `zijn`, `die`),
  `de_rolregel_en_een_volgend_blok_beeindigen_de_feittypenaam`.
- K1: `een_lid_op_de_regel_van_de_kop_is_een_fout`,
  `een_lid_op_de_regel_van_het_vorige_lid_is_een_fout` (R3 en twee leden met `;`),
  `een_kenmerk_dat_een_naam_afkapt_noemt_het_koppelteken`,
  `een_rol_op_de_regel_van_de_vorige_rol_is_een_fout`,
  `een_lid_dat_doorloopt_of_op_een_eigen_regel_begint_blijft_toegelaten` (§3.9, en een lid
  zonder lidwoord op de regel na de kop).
- K2: `een_rolvuller_die_geen_objecttype_is_is_een_fout` (R6 en een domein als vuller),
  `een_rolvuller_die_nergens_gedeclareerd_is_is_een_waarschuwing` (en met de typen erna:
  geen melding).
- K6: `een_naam_die_regeltekst_niet_leest_krijgt_een_waarschuwing` (attribuut, kenmerk en
  rol), `de_koppeltekenspelling_leest_in_regeltekst`.
- Wat bleef: `een_kop_in_titelhoofdletters_leest_als_voorheen`, en
  `de_golden_leest_zonder_diagnose`.

Elke meldingstoets vergelijkt de hele zin en haar regel.

### De golden `jutterswet-premierecht`

Declaraties: parameter `premietarief` (geheel getal); objecttype `Jutter` (bezield) met
`premiebedrag`, `aantal toegekende rechten` en kenmerk `premiegerechtigd`; objecttype
`Recht op premie` (mv `Rechten op premie`) met `vondstgewicht`, `premie` en kenmerk
`toegekend`; feittype `jutter heeft premierecht` met de rollen `rechthebbende` (Jutter) en
`recht op premie` (mv `rechten op premie`, Recht op premie), één rechthebbende heeft meerdere
rechten op premie. Regels: `Een Recht op premie is toegekend indien het vondstgewicht van het
Recht op premie groter is dan 2`; `De premie van een Recht op premie moet berekend worden als
het vondstgewicht van het Recht op premie maal het premietarief indien het Recht op premie
toegekend is`; het premiebedrag als `de som van de premie van alle rechten op premie van de
Jutter of 0 als die er niet zijn`; het aantal toegekende rechten als `het aantal rechten op
premie van de Jutter die toegekend zijn`; `Een Jutter is premiegerechtigd indien zijn
premiebedrag groter is dan 20`. De som en de telling dragen `sourceRole` {`jutter heeft
premierecht`, `recht op premie`}.

Handberekening (ook in `NOTE.md`):

- `case1-tarief-vier.json`, premietarief 4. R1 (5): toegekend, premie 5 × 4 = **20**. R2
  (2): niet toegekend (2 is niet groter dan 2), premie **leeg**. R3 (3): toegekend, premie
  **12**. R4 (1): niet toegekend, premie **leeg**. J1 (R1, R2, R3): premiebedrag 20 + 12 =
  **32** (de lege premie telt niet), **2** toegekende rechten, premiegerechtigd (32 > 20).
  J2 (R4): premiebedrag **0** (de enige premie is leeg: de terugval), **0**, niet
  premiegerechtigd. J3 (geen rechten): **0**, **0**, niet premiegerechtigd. Consistent.
  18 verwachtingen.
- `case2-tarief-vijf-op-de-grens.json`, premietarief 5. R5 (4), R6 (3), R7 (3): alle drie
  toegekend, premie **20**, **15**, **15**. J4 (R5): **20**, **1**, niet premiegerechtigd (20
  is niet groter dan 20). J5 (R6, R7): **30**, **2**, premiegerechtigd. Consistent. 13
  verwachtingen.

De basiscommit weigerde deze bron op haar koppen (`ERROR (line 8): syntaxfout: mismatched input
'op' expecting {RPAREN, UPPER_ID, LOWER_ID}`, `ERROR (line 13): syntaxfout: extraneous input
'heeft' …`, `ERROR (line 15): …'op'…`, gevolgd door onopgeloste regels). Nu: lege
diagnostieklijst, `verifier: PASS`. Gespeeld door: gate 2 (schema en sleutelvolgorde), 3
(verifier-schoon), 4 (caseschema), 5 (`oracle: PASS (18 expectations)` en 13), 6
(`clingo: PASS (18 expectations)` en 13), 7 (beide cases weerleggen de no-op), 10
(heremissie bytegelijk), 12 (`PLAYED 2` in de exec-model-tak én de DRL-tekst-tak), 15 (de
vier ch.-5-kanten van de verifier, twee over het kenmerk `toegekend` en twee over een
attribuut, gelijk aan de regelgraaf; routeleg bytegelijk) en 16 (het scenariodocument
regenereert beide casebestanden). Gate 13 twist hem niet: hij draagt geen eenheden en geen
vocabulaires. Het is de eerste corpusgolden met een objecttypenaam met spaties.

### Inertiemeting (methode van DIVERGENCES §6)

643 verschillende bronnen (de lijst van het naamonderzoek), elk door de frontend van de
basiscommit `16131b9f` (wegwerp-worktree, inmiddels verwijderd) en door die van de kop van
de branch, in-process via `Resolver.irModus` (de productieroute) en, voor de 217 bronnen
met een scenario, ook via `Resolver.casesModus`; vergeleken: exitstatus, stdout en stderr
van de CLI, de IR en de casebestanden. Normalisatie: alleen de ANTLR-tekst `expecting …` en
`missing … at`.

| verzameling | bronnen | IR (basis) | IR bytegelijk | IR gelijk op de diagnostieklijst na | ERROR (basis) | ERROR, diagnostiek gelijk | ERROR, gelijk op 'expecting' na | emitterweigering gelijk |
|---|---|---|---|---|---|---|---|---|
| corpus (100 `bron.rgs`, 7 `scenario.rgs`) | 107 | 100 | 100 | 0 | 7 | 7 | 0 | – |
| pariteitszone | 47 | 20 | 16 | 4 | 24 | 24 | 0 | 3 |
| distributie | 59 | 13 | 13 | 0 | 46 | 46 | 0 | – |
| werkbank | 201 | 17 | 12 | 5 | 184 | 184 | 0 | – |
| operationele regelcollectie | 36 | 5 | 4 | 1 | 31 | 31 | 0 | – |
| oudere toolchain | 193 | 37 | 33 | 4 | 154 | 152 | 2 | 2 |
| **totaal** | **643** | **192** | **178** | **14** | **446** | **444** | **2** | **5** |

Geen exitstatus veranderde. De scenarioroute schreef dezelfde 12 casebestanden (distributie);
de overige scenariobestanden falen in beide builds gelijk (ze hebben hun modelbestand nodig;
de zeven corpusparen dekt gate 16). Restverschillen, alle verklaard:

- **13 bronnen, K2-WARNING**: één bestand van een model dat zijn objecttypen in een ander
  bestand declareert (vier losse fragmenten van de pariteitszone, vier specvoorbeelden van de
  oudere toolchain, vier regelbestanden van de werkbank, één van de regelcollectie). Model en
  regels bytegelijk, de diagnostieklijst van de IR krijgt de WARNINGs (1 tot 16 per bron,
  samen 59). In een model uit meer bestanden, samen gelezen, verdwijnen ze: de casus van de
  pariteitszone (alle fragmenten samen) heremitteert in gate 11 bytegelijk.
- **1 bron, K6-WARNING**: een gegenereerd gegevensmodelbestand van de werkbank met een
  attribuut waarvan de naam `als` draagt; de ene bron die de kaart al telde.
- **2 bronnen, alleen 'expecting'-tekst**: twee negatieve snippets van de oudere toolchain
  waarvan het feittype zijn kardinaliteitsregel mist; de verwachtingsverzameling bij EOF noemt
  nu ook de nieuwe naamwoorden. Na normalisatie gelijk.
- **K1 vuurt op geen van de 643.**

Tekstuele telling over dezelfde 643 (commentaar en literals weggefilterd): 322
objecttypekoppen, geen met een kleingeschreven voorzetsel of sleutelwoord in de naam of tekst
achter de naamgroep op de kopregel, vijf zonder `(mv:` en geen daarvan gevolgd door een lid
zonder lidwoord; 361 rolregels, geen zonder lidwoord; 184 feittypekoppen, geen met een
lidwoord of een woord buiten de oude set; 1748 ledenregels, geen met twee `;`; 448
`(mv:`-groepen, alle op hun eigen regel gesloten.

### Gates

Alle zestien groen op de kop van de branch vóór de Result-commit (daarna alleen de kaart
gewijzigd), elk apart gedraaid met eigen log; `scripts/admit.sh` draaide ze bovendien alle
zestien groen bij de toelating.

- 1 PASS. 2 PASS: 313 bestanden (was 312), 41252 objecten in 814 documenten in schemavolgorde.
- **3** PASS: 313 goldens schoon, 501 negatief op hun regel (vloer 350), 344 op hun arm (vloer
  260), 605 pointers (vloer 547).
- 4 PASS: 774 casebestanden (was 772).
- **5** PASS: ref-suite 1152 voorbeelden, 734 cases exact (was 732).
- **6** PASS: backendsuite 1988 checks, 734 cases exact door clingo.
- 7 PASS: 690 cases weerleggen de no-op (was 688), 44 overlevers. 8 PASS: 40 rode cases.
  9 PASS: 241 checks, 12 catala-goldens.
- **10** PASS: 967 JUnit-toetsen (was 950), 101 rgs-goldens bytegelijk (was 100).
- **11** PASS: 45 `.rgs` bytegelijk in de casus, de toetssuite van de zone 27 toetsen, 29 cases door ref,
  clingo en Drools, 28 slices verifier-schoon.
- **12** PASS: gespeeld 218 = pin `EXPECTED_PLAYED` 218 (was 217), DRL-tekst-tak 188 = pin
  `EXPECTED_DRL_PLAYED` 188 (was 187), JUnit 846.
- **13** PASS: 41 eenheidsannotaties getwist over 23 goldens (vloer 38), 106 replays;
  vocabulaireleg 1 golden, 46/46/36.
- 14 PASS: 150 checks, 2 goldens.
- **15** PASS: specleg 237 van 274 goldens vergeleken, 126 afhankelijkheden gelijk (vloeren
  234 / 116), 3 gelijste verschillen; routeleg 101 goldens met `bron.rgs` (vloer 98), 23
  eenheidstoetsen.
- **16** PASS: 8 scenariogoldens (vloer 7), 33 casebestanden (vloer 31).

Geen vloer aangepast; alleen de twee gelijkheidspinnen van gate 12, in hun eigen commit
(beschermd pad).

### Normvragen (met het gebouwde standaardantwoord)

1. **Een rolvuller die het document nergens declareert: ERROR of WARNING?** De kaart (K2)
   zegt ERROR. Gate 11 draait de oorspronkelijke modeltoets van de pariteitszone
   (`ModelTest.parse_zonder_fouten`), die een plak met een feittype resolveert waarvan de
   vuller `Vlucht` in die plak niet gedeclareerd is, en eist dat er geen ERROR is; de zone is
   voor deze kaart alleen-lezen en gate 11 moet groen blijven. Gemeten: met K2 overal als
   ERROR faalde die toets en werd gate 11 rood. Gebouwd standaardantwoord, dat het
   vastgelegde gedrag van de pariteitszone en de praktijk van modellen uit meer bestanden
   volgt: ERROR waar het document de misslag laat zien (een staart van de vuller is een
   gedeclareerd objecttype, zoals `BK Jutter` in R6, of de naam is als ander soort
   gedeclareerd); WARNING waar de naam nergens in het document staat. De verifier weigert
   zo'n los bestand nog steeds onder WF-003. Wil de maintainer overal een ERROR, dan moet de
   plak in de pariteitszone haar objecttype declareren (een wijziging in die zone, aan hem).

### Open punten

- De normvraag hierboven.
- Een ongedeclareerde dimensie passeert de frontend nog stil: `de waarde Numeriek (geheel
  getal) gedimensioneerd met jaartal;` zonder `Dimensie` geeft exit 0 en een IR, die de
  verifier onder WF-004 weigert (gemeten 2026-09-28). Dezelfde klasse als de rolvuller vóór
  deze kaart, buiten haar opdracht. Een ongedeclareerd domein is wel een ERROR, maar op
  regel 0 (`ongedefinieerd domein Bedrag (gebruikt door 'waarde')`).
- `scripts/admit.sh` kopieert `scenario.rgs` niet mee (het kent `bron.rgs`,
  `bron.catala_en`, `bron.flogic` en `NOTE.md`), en de Layout-rij van `corpus/README.md`
  noemt het niet, terwijl gate 16 het leest en acht goldens het dragen. Ik voegde het na de
  toelating toe en draaide gate 16 en 10 opnieuw. Beide bestanden liggen buiten "mag raken".
- Buiten deze kaart, zoals de kaart ze noemt: attribuutnamen met een hoofdletterwoord (een
  eigen kaart zonder nieuwe syntaxis), sleutelwoorden in namen (koppelteken), kleingeschreven
  `van` in typenamen (koppelteken of `Van`), domeinnamen met voorzetsels.

### Voorgestelde PLAN §9-alinea

**Names in declaration heads landed (2026-09-28, card `namen-in-declaratiekoppen`):** the
RegelSpraak frontend reads an object-type name with the prepositions rule text already reads
(`Recht op premie`) in every position of the production, a plural up to its closing
parenthesis, and a fact-type name with its verbs (`jutter heeft premierecht`); a member or
role that begins on the line where the previous declaration ended is an ERROR on that line,
a role's filler that is no object type is reported before the emission, and a declared name
rule text cannot read back gets a WARNING naming the word
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "Namen in declaratiekoppen"; inertness
measured over 643 sources, no exit status and no model changed; golden
`jutterswet-premierecht`, gate 12 plays 218 and 188).
Open: the filler check is an ERROR only where the document shows the misreading and a
WARNING where the type is declared nowhere, because the parity zone's own model test
resolves such a slice without an error — an ERROR everywhere needs that slice completed, the
maintainer's call; an undeclared dimension still passes the frontend and only the verifier
refuses it (WF-004); `scripts/admit.sh` and `corpus/README.md` do not know the
`scenario.rgs` of a golden.

### Commits op de branch

- `dc3eac12` Grammatica: voorzetsels in typenamen, werkwoorden in feittypenamen
- `663ab7ea` Resolver: geen stille herlezing van een declaratie
- `fef2864a` Resolver: de naamcontroles laten een herstelde declaratie met rust
- `853594f1` Frontendtoetsen voor namen in declaratiekoppen
- `dde7381c` Golden jutterswet-premierecht: namen met voorzetsel en werkwoord
- `3a356a51` Gate 12: de nieuwe golden speelt, beide pinnen één omhoog (beschermd pad,
  landt via de hoofdsessie)
- `74cf2f41` DIVERGENCES: sectie Namen in declaratiekoppen
- de commit die deze Result-sectie in de kaart zet (de kop van de branch)
