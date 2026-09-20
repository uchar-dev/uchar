# v8-enum-emitter: de frontend emittert enumeratieliteralen welgevormd

Uit `spec/proposals/V1-WIREMIGRATIE.md` V8, **BESLIST (2026-09-19,
maintainer): route (a)** — nu, als eigen v0-kaart naar het model van de
tekstlexeem-kaart (`backlog/done/20260918-tekstlexeem-canonicalisatie.md`).

De stand (V8, hermeten bij oppakken): de wire is dubbel gehekt (WF-039
op het label, WF-034 (r) op het sentineltype) en nul van de documenten
draagt de gedelimiteerde vorm — er is geen levende divergentie en geen
stil fout antwoord. Wat er wél is: de RegelSpraak-frontend kan een
enumeratieliteral niet welgevormd emitteren — hij schrijft de snippet
mét delimiters én laat het type op het onbekend-sentinel staan — en
daardoor draagt geen enkele `bron.rgs`-golden er een.

Eerlijk prijskaartje (uit het besluit): dit is niet één emitterregel.
Naast het strippen van de delimiters moet de frontend de literal ook
TYPEREN, en dat is contextafhankelijk werk — welke `Enumeratie` bedoeld
is volgt uit het schrijfdoel of de vergelijkingszijde. Het precedent
(tekstlexeem) deed alleen het strippen.

## Acceptatie (aangescherpt bij oppakken, 2026-09-20)

Stand hermeten vóór de wijziging, machinaal over de hele boom (688
`ir.json`): NUL gedelimiteerde literalwaarden, NUL `Onbekend`-getypeerde
literalen, en 16 `Enumeratie`-getypeerde literalen in 13 bestanden — alle
handgeschreven of uit de Catala-frontend, geen enkele uit een `bron.rgs`.
De kaartzin klopt: geen levende divergentie, wel een dekkingsgat. Gate 10
basislijn: PASS, 322 tests, 63 rgs-goldens.

- (a) `ExpressieBouwer` schrijft voor een `ENUM_LITERAL` het KALE label
  (uit de tokentekst, niet uit de whitespace-gevouwen bronsnippet) met
  het `Enumeratie`-datatype van de context; `bron` houdt de
  gedelimiteerde snippet. De afleidingsregel: het domein komt van het
  SCHRIJFDOEL (het gedeclareerde datatype van het doelattribuut) of van
  de ANDERE VERGELIJKINGSZIJDE. Waar geen van beide een `Enumeratie`
  aanwijst blijft de emissie een luide `Onopgelost`-marker met reden —
  het bestaande markerpatroon, dat sinds 2026-09-19 een WARNING krijgt —
  nooit een gok. Emittergetuigen in `IrEmitterTest`, rood-eerst: eerst
  de huidige gedelimiteerde+sentinelemissie vastgelegd, daarna omgezet.
- (b) Eén `bron.rgs`-golden met een enumeratieliteral in beide
  contexten, handberekende verwachtingen VOORAF genoteerd, gespeeld
  door ref en clingo; Drools gespeeld of benoemde EXCLUDE in beide
  kanaallijsten van `scripts/check-drools.sh` (voorstel, zuiver
  additief — de parallelle kaarten raken hetzelfde script).
- (c) De `Literal`-rij in `spec/IR-SPEC.md` ch. 4.1 zegt vandaag "an
  enumeratiewaarde keeps its `'…'` delimiters, which readers strip
  (`Literalen.enumLabel`)". Die zin beschrijft de FRONTEND, niet de
  wire: WF-039 kruist het kale label byte-exact tegen het gedeclareerde
  domein, dus de gedelimiteerde vorm is op de wire ill-formed. De zin
  wordt gecorrigeerd plus één alinea in de rij WF-081/084/095/tekstlexeem
  (voorstel, beschermd pad); WF-039 blijft byte-exact zoals hij is.
- (d) De toka-meting: de zone draagt 30 documenten met
  enumeratiedeclaraties (alle 30 `ir.json`, één gedeclareerd domein).
  Meet of de emitterwijziging daar bytes beweegt en of een herpin nodig
  is; meld de uitkomst en welke fixtures raken in het Result, maar
  herschrijf geen toka-fixture — dat oordeel is aan de maintainer.
- (e) Alle veertien gates groen, elk los gedraaid; frontend-wijziging
  betekent een herpin van de toolchain door de maintainer na de landing.

May touch: `frontends/regelspraak/**`, nieuwe goldenmap onder
`corpus/golden/`, `scripts/check-drools.sh` uitsluitend de kanaallijst
(voorstel), `spec/IR-SPEC.md` ch. 4.1 (voorstel), kaartverplaatsing.
Out of scope: de wire-vorm zelf (WF-039/WF-034 bewegen niet), B22, en
de consumentenkant — wat een backend met een kaal label DOET is niet
deze kaart.

Result: geland. De enumeratiewaarde is de vierde in de rij kale lexemen
(WF-081 getal, WF-084 datum, tekstlexeem 2026-09-18), maar met een
tweede helft die de eerste drie niet hebben: een enumeratieliteral noemt
een LABEL en geen domein, dus welk `Enumeratie`-type hij draagt is een
eigenschap van zijn PLAATS. `ExpressieBouwer` bouwt hem daarom als een
marker en twee plaatsen promoveren hem — het schrijfdoel en de andere
vergelijkingszijde, precies de twee die de §13.3.4.2-controle al leest.
Waar geen van beide een domein noemt blijft de marker staan en meldt de
markerdiscipline hem luid. Eén plek in de frontend bouwt zo'n literal
(`ExpressieBouwer:428`, machinaal gecontroleerd), dus dit is de hele
emissiewijziging.

METING (machinaal, hele boom, vóór de wijziging). 688 `ir.json`: NUL
gedelimiteerde literalwaarden, NUL `Onbekend`-getypeerde literalen, 16
`Enumeratie`-getypeerde literalen in 13 bestanden — alle handgeschreven
of uit de Catala-frontend, geen enkele uit een `bron.rgs`. De kaartzin
klopt dus letterlijk: geen levende divergentie, een dekkingsgat van de
producent. Na de wijziging: 689 documenten, nog steeds 0 en 0, en 20
`Enumeratie`-getypeerde literalen in 14 bestanden — de vier nieuwe zijn
die van de nieuwe golden. Basislijn gate 10 vóór: PASS, 322 tests, 63
rgs-goldens.

HET DUBBELE HEK, gemeten op de kaart haar eigen document. De golden
`jutterswet-vondstklasse` met één literal teruggedraaid naar de oude
emissievorm: `WF-034 FAIL /resolvedRegels/0/waarde/type: unknown-type
sentinel inside a structured node`. Alleen de delimiters terug, het type
intact: `WF-039 FAIL /resolvedRegels/0/waarde: enumeration label
''wrakgoed'' is not in the declared domain`. Beide helften van het hek
weigeren dus precies wat de emitter tot nu toe schreef.

ROOD-EERST, feitelijk gedraaid. Vier getuigen eerst in hun doelvorm
gecommit (84d802c5): 4 rood van 20, met de huidige emissie in de
faalmelding — `"waarde":"'gaaf'","type":{"node":"Onbekend","reden":"niet
opgelost"},"bron":"'gaaf'"`, in zowel de schrijf- als de
vergelijkingspositie. Na de fix 21 van 21 groen, hele suite 327 groen.

DE AFLEIDINGSREGEL, gemeten en in de getuigen vastgelegd. (1) Het
SCHRIJFDOEL: het gedeclareerde datatype van het doelattribuut — de
gelijkstelling/initialisatie (`RuleChecker.registreer`) en het
objectcreatie-lid, twee van de drie WF-023-schrijfposities; de derde
(de defaults-consequentie) kopieert de waarde van de `ResolvedRegel` en
erft de promotie dus gratis. (2) De andere VERGELIJKINGSZIJDE: het type
van de zusteroperand, symmetrisch, dus de literal promoveert links zowel
als rechts. Alle zes bouwplaatsen van een `Conditie.Vergelijking` lopen
nu door één fabriek, zodat de regel geldt van vergelijkingen als zodanig
en niet van de vormen die deze kaart toevallig aanraakte — met één
uitzondering, die de fabriek zelf benoemt: `elementVergelijking` (de
§5.6 die/dat-subselectie op een aggregatie-operand) weigert vóór het
bouwen elke rechterzijde die geen Literal of ParameterRef is, dus daar
bereikt een marker de fabriek nooit en wordt het hele predicaat
geweigerd in plaats van stil ongetypeerd vergeleken. Dat is de
bestaande versmalling van die positie en geen gat dat deze regel sloeg:
zij vouwt een GEAGGREGEERD attribuut, en geen aggregatie loopt over een
`Enumeratie`, dus een welgetypeerde enumvergelijking kan er niet staan.
De promotie
daalt ook af door de ch.-4.10-letketen van een variabelendeel, want die
DENOTEERT haar waarde: zonder dat zou `Daarbij geldt: de tint is 'gaaf'`
het onbekend-sentinel op DRIE posities dragen (Let, BinderRef, waarde)
in plaats van op één. Hij stopt waar `RuleChecker.afgepeld` zijn
bindingenkaart nodig heeft — een body die langs een binnenste Let een
buitenste binder leest blijft marker, luid, in plaats van half
gepromoveerd.

EEN FOUT ANTWOORD DAT MEEVERDWEEN, niet gezocht maar gemeten. Het label
komt uit de ENUM_LITERAL-TOKENTEKST, niet uit `DefinitionResolver.src`.
Die helper vouwt elke witruimte-run tot één spatie; de DECLARATIE doet
dat niet (`enumeratie()` neemt de tokentekst). Een gedeclareerde waarde
met een dubbele spatie was daardoor niet toe te kennen: de
lidmaatschapscontrole las bij het schrijfdoel de gevouwen snippet en
weigerde een correct document — feitelijk gemeten vóór de fix, `ERROR
(line 8): 'licht blauw' is geen waarde van Enumeratie Kleur [licht
blauw, rood]`. Nu lopen declaratie, controle en wire door dezelfde
tokentekst. De getuige
`enum_literal_label_komt_uit_de_tokentekst_niet_uit_de_gevouwen_snippet`
pint dat, inclusief de eis dat de gevouwen vorm NERGENS in het document
voorkomt.

GETUIGEN. Vijf in `IrEmitterTest`, naast de rij WF-081/084 en de drie
tekstlexeemgetuigen: `enum_literal_krijgt_zijn_domein_van_het_schrijfdoel`,
`enum_literal_krijgt_zijn_domein_van_de_andere_vergelijkingszijde` (beide
richtingen), `enum_literal_door_de_letketen_van_een_variabelendeel`,
`enum_literal_zonder_domeincontext_blijft_een_luide_marker` (Tekst-doel:
markernode én WARNING met reden) en de tokentekstgetuige. Eén hulpje
erbij, `enumBron`, plus de constante `GAAF_GETYPEERD` die de enige vorm
pint die de wire toelaat. Geen bestaande check gewijzigd — ook
`VariabeledeelLoweringTest.de_enumdomeincontrole_ziet_door_de_letketen_heen`
niet, die ongewijzigd groen blijft omdat de promotie door de keten daalt.

DE GOLDEN. `corpus/golden/jutterswet-vondstklasse`, de eerste
`bron.rgs` die een enumeratiewaarde draagt. Domein `Vondstklasse`
('drijfgoed' 'strandgoed' 'wrakgoed'), objecttype Vondst met `klasse`
(invoer), `bestemming` (afgeleid) en het kenmerk `bergingsplichtig`.
Drie regels zetten de twee afleidingsplaatsen naast elkaar: R1 schrijft
de literal 'wrakgoed' onder een voorwaarde die de literal 'drijfgoed'
als vergelijkingszijde draagt; R2 schrijft de eigen klasse door onder de
ongelijkheid; R3 leest de AFGELEIDE bestemming tegen 'wrakgoed', zodat
een gepromoveerde literal zelf weer een vergelijkingszijde is.
Handberekend VOORAF, per casus: drijfgoed → bestemming wrakgoed +
bergingsplichtig; strandgoed → bestemming strandgoed, kenmerk afwezig;
wrakgoed → bestemming wrakgoed + bergingsplichtig. Alle drie spelen
exact zo door ref en clingo (gates 5 en 6, corpus 552 → 555 casussen).

DE TOKA-METING (kaartpunt d), de uitkomst: GEEN herpin nodig, geen
fixture geraakt. De 30 documenten met enumeratiedeclaraties zijn alle 30
`toka/**/ir.json`; zij dragen één gedeclareerd domein (uit
`toka-06-domein-luchthavens-is-van-het-type.rgs`) en GEEN ENKELE
enumeratiewaarde in een expressiepositie — statisch gemeten (0
gedelimiteerde waarden, 0 `Onbekend`-getypeerde literalen in de hele
zone) en daarna feitelijk: gate 11 PASS ná de wijziging, 45 fragmenten
byte-identiek her-geëmitteerd, 29 slices, ref 29 / clingo 29 / Drools 29
PLAYED, nul divergenties. De emitterwijziging beweegt dus nul
toka-bytes. Anders dan bij het tekstlexeem is dit geen geboekte
divergentie: er is niets om te boeken.

CHECK-DROOLS, exact wat ik wijzigde en waarom. Twee regels toegevoegd,
niets herordend of hernummerd, geen telling verplaatst:
`EXPECTED_CHANNEL` krijgt `jutterswet-vondstklasse|EXCLUDE|transpiler
SKIP: regel — voorwaarde buiten Phase-B scope` en `DRL_CHANNEL` krijgt
`jutterswet-vondstklasse|EXCLUDE|MARKER regel: TODO voorwaarde — an
enum-label equality in a condition`, beide met redenblok. `EXPECTED_PLAYED`
blijft 164 en `EXPECTED_DRL_PLAYED` blijft 141: de gate zegt het zelf —
een nieuwe golden die niet speelt is nooit meegeteld. De kanaalvloeren
lopen van 52 naar 53 en van 75 naar 76, wat de lengte van de lijsten is
en geen pin. Tussendoor at een eerste poging de buurregel
`jutterswet-vondsttarief` op; de gate noemde de speler die zijn pin
kwijtraakte bij naam en de regel is in 984f288a teruggezet — precies
waarvoor die vloer er staat.

DE WEIGERING ZELF, gemeten met de replay-API op de nieuwe golden:
exec-model `EXCLUDE transpiler SKIP: regel: bestemming van drijfgoed —
regel (voorwaarde buiten Phase-B scope)`, DRL-tekst `EXCLUDE
drl-transpiler MARKER: regel: bestemming van drijfgoed — TODO
voorwaarde`. Dezelfde vorm als `jutterswet-keurstaat`, dat al sinds
lang dezelfde twee weigeringen draagt. De golden is dus een ref ≡
clingo-paar, geen volledig vierkant, en dat staat per naam in beide
lijsten.

DE OORZAAK VAN DIE WEIGERING, gemeten en BUITEN SCOPE gehouden. De
gedeelde lezer `Literalen.tekstOfEnumLabel` geeft voor een niet-Tekst
literal `enumLabel(waarde)` terug, en die verlangt DELIMITERS: op een
WF-039-conforme kale waarde geeft hij null. VIJF plekken lezen door hem
heen: drie vergelijkingsarms in `ExecModelTranspiler` (3913 eigen
attribuut, 3928 genavigeerd, 5422), één in `DrlTranspiler` (5114), en —
de plek die de SCHRIJFzijde draagt — de waarde-arm
`ExecModelTranspiler:4185` (`String tekstLabel = … tekstOfEnumLabel(tlit)`),
die een enumliteral als geschreven waarde in een String-constante
omzet. De backend weigert dus niet alleen elke enumGELIJKHEID van een
conforme wire maar ook elke enumTOEKENNING. Apart gemeten op een
variant van deze golden zonder voorwaarde
(`De bestemming van een Vondst moet gesteld worden op 'wrakgoed'.`):
exec-model `transpiler SKIP: regel (waarde buiten scope)`, DRL-tekst
`MARKER: TODO literal: wrakgoed` — de schrijfhelft van de golden zou
dus ook zonder haar voorwaarde geweigerd zijn. De lezer implementeert
de ch.-4.1-zin die deze kaart juist corrigeert. Dat is de consumentenkant
en die staat in deze kaart
uitdrukkelijk buiten scope; hij is ook niet gratis, want het repareren
verschuift `jutterswet-keurstaat` én `jutterswet-vondstklasse` van
EXCLUDE naar PLAYED in beide takken en dus `EXPECTED_PLAYED` 164 → 166
en `EXPECTED_DRL_PLAYED` 141 → 143 — een gelijkheidspin en twee
lijstverwijderingen, wat met parallelle kaarten op hetzelfde script
botst. Kaartvoorstel hieronder.

SPEC-VOORSTEL (kaartpunt c, beschermd pad, in de branch als voorstel).
`spec/IR-SPEC.md` ch. 4.1. De `Literal`-rij zei "an enumeratiewaarde
keeps its `'…'` delimiters, which readers strip (`Literalen.enumLabel`)"
en noemde daarmee TWEE bronvormige lexemen. Die zin beschreef een
frontend-lezer en stond naast WF-039, die hetzelfde veld byte-exact
tegen het gedeclareerde domein kruist — twee zinnen over één veld
waarvan er maar één waar kon zijn. De rij noemt nu nog ÉÉN bronvormig
lexeem (WF-095, `DatumTijd`) en zegt expliciet dat de oude zin de lezer
beschreef, met een verwijzing naar de nieuwe alinea. Die alinea, "The
enumeration-value lexeme (maintainer decision 2026-09-19)", staat in de
rij naast de tekstlexeemalinea en draagt twee dingen: (1) het kale label
is de wire-vorm, en dat is GEEN nieuwe MUST — WF-039 en WF-034 (r)
zeggen het al, dus de alinea krijgt terecht geen WF-nummer; (2) welk
domein geldt is een eigenschap van de POSITIE, met de twee posities
genoemd, de WF-088-eis op het herhaalde waardenlijstje, en de
markerplicht waar geen positie er een aanwijst. De machinaal
controleerbare artefacten staan erbij: de emittergetuigen (gate 10) en
de golden (gates 3, 5, 6). WF-039 is niet aangeraakt — byte-exact zoals
hij was.

REGISTERVOORSTEL — `spec/CONSTRUCTS.md`, rij 27 (`Datatype
refinements`), aan te hangen in de `v0→v1 delta`-kolom, direct achter de
tekstlexeemzin en ter VERVANGING van de slotzin van "The enumeration
lexeme, measured (2026-09-19, v1-inventory)" die de reparatie nog
aankondigde:

> **Enumeratiewaarde geëmitteerd (2026-09-20, V8 (a); kaart
> `20260919-v8-enum-emitter`):** het gemeten dekkingsgat is gedicht. De
> emitter schrijft het KALE label met het `Enumeratie`-datatype van de
> positie; de gedelimiteerde snippet blijft in `bron`. Anders dan bij
> WF-081/084 en het tekstlexeem is dit niet alleen een lexeemregel: een
> enumeratieliteral noemt een label en geen domein, dus de bouwer bouwt
> een marker en twee POSITIES typeren hem — het schrijfdoel (WF-023's
> drie schrijfposities) en de andere vergelijkingszijde — waarna het
> `type` het gedeclareerde waardenlijstje voluit herhaalt zoals WF-088
> van elke gebruikssite eist. Waar geen positie een domein aanwijst
> blijft een `Onopgelost`-marker staan met een luide WARNING: weigeren
> is goedkoper dan raden, want een label onder een domein dat niemand
> voor die plaats declareerde is een stil fout antwoord. Geen nieuw
> wire-hek — WF-039 (label ∈ domein) en WF-034 (r) (geen sentinel)
> hekten dit al en zijn ongewijzigd; het is een PRODUCENTENregel,
> machinaal geborgd door vijf emittergetuigen (`IrEmitterTest`, gate 10)
> en door de eerste `bron.rgs`-golden die het construct draagt
> (`jutterswet-vondstklasse`, gates 3/5/6, ref ≡ clingo op
> handberekende waarden). Het label komt uit de ENUM_LITERAL-tokentekst
> en niet uit de whitespace-vouwende bronsnippet, want de declaratie
> doet dat ook niet — daardoor verdween een fout antwoord dat niemand
> had gezocht: een gedeclareerde waarde met een witruimte-run was tot nu
> toe niet toe te kennen. Geen toolchain-divergentie en geen herpin:
> geen toka-fragment draagt een enumeratiewaarde in een expressiepositie
> (gate 11 PASS, nul bewogen bytes). De Drools-backend draagt het
> construct NIET — zijn gedeelde lezer `Literalen.tekstOfEnumLabel`
> verlangt de gedelimiteerde vorm die de wire nooit draagt — dus de
> golden is per naam gepind in beide kanaallijsten van gate 12; dat is
> de consumentenkant en heeft een eigen kaart.

KAARTVOORSTEL (nieuw, voor `backlog/new/`) —
`drools-enumlabel-lezer`: `Literalen.tekstOfEnumLabel` leest een
enumeratiewaarde door `enumLabel`, dat delimiters verlangt, terwijl de
wire per WF-039 het kale label draagt. Vijf plekken lezen door die lezer
heen, en het zijn niet alleen vergelijkingen: `ExecModelTranspiler` 3913
(eigen attribuut), 3928 (genavigeerd) en 5422, `DrlTranspiler` 5114, en
de WAARDE-arm `ExecModelTranspiler:4185`, die een enumliteral als
geschreven waarde omzet. Beide Drools-takken weigeren daardoor zowel
elke enumgelijkheid als elke enumtoekenning — apart gemeten op een
voorwaardeloze variant: exec-model `waarde buiten scope`, DRL-tekst
`TODO literal: wrakgoed`. De twee geraakte goldens zijn
`jutterswet-keurstaat` en `jutterswet-vondstklasse`. De reparatie is
klein en zit op ÉÉN plek: de lezer geeft het label van een
`Enumeratie`-getypeerde literal verbatim terug, precies zoals de
Tekst-arm doet, waarmee alle vijf plekken gratis meekomen — geen van de
call sites hoeft te wijzigen. De gevolgen zijn kanaalgevolgen:
`EXPECTED_PLAYED` 164 → 166,
`EXPECTED_DRL_PLAYED` 141 → 143 en twee regels uit elke kanaallijst, dus
serieel plannen tegen andere kaarten op `scripts/check-drools.sh`. Het
levert twee goldens erbij aan het volledige vierkant.

ALLE VEERTIEN GATES, los uitgevoerd, met kerngetallen:

    1  self-contained          PASS
    2  ir-schema               PASS  schema valide; 251 bestanden (vloer 192)
    3  verifier                PASS  251 golden schoon; 408 negatief (vloer 350)
    4  case-schema             PASS  589 casusbestanden
    5  oracle                  PASS  686 worked examples (vloer 489); 555 casussen
    6  clingo                  PASS  452 checks (vloer 83); 555 casussen
    7  noop                    PASS  519 casussen weerleggen; 36 survivors
    8  rood                    PASS  34 rode casussen (vloer 26); 68 fragmenten
    9  catala                  PASS  201 checks (vloer 201); 12 goldens
    10 regelspraak             PASS  327 tests (vloer 173); 64 rgs-goldens (vloer 38)
    11 toka-parity             PASS  45 fragmenten; 29 casussen; nul divergenties
    12 drools                  PASS  played 164; kanaalvloer 53; DRL played 141, vloer 76; JUnit 502
    13 reftype-derivation      PASS  129 twists over 25 goldens (vloer 74); 116 replays, 23 DRL
    14 flogic                  PASS  130 checks (vloer 81); 2 goldens

TOOLCHAIN-HERPIN NODIG (kaartpunt e). Dit is een
frontend-emissiewijziging, dus `regelspraak-java` moet na de landing op
de nieuwe `SPEC_REV` herpinnen. De frontend draagt nu 64
`bron.rgs`-goldens (was 63).

BUITEN SCOPE GEHOUDEN, met reden. De wire-vorm zelf (WF-039 en WF-034
zijn niet aangeraakt); B22/V9 (getypeerde JSON-waarden — een
enumeratiewaarde wordt daar `{"enumeration": "X"}`, wat deze kaart
voorbereidt maar niet doet); de consumentenkant (zie het kaartvoorstel);
`spec/proposals/V1-WIREMIGRATIE.md`, waarvan V8 en de P4-regel nu
uitgevoerd zijn maar dat buiten May touch valt; en de registerbestanden
`spec/CONSTRUCTS.md` en `spec/PLAN.md`, waarvoor de voorgestelde tekst
hierboven staat.
