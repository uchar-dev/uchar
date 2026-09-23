# L2 — de referentielaag (M2 + M3: B7, B8, B9, B11, B12, B13, B16, plus D53): de tweede klapkaart van v1

Tweede kaart van fase 2 van `spec/proposals/V1-WIREMIGRATIE.md` (§7, de
landingstabel van 2026-09-23: L2 = M2 + M3), na L1
(`backlog/done/20260923-m1-schema-en-container.md`, main `32185b7b`,
registerwerk `69436907`). L2 zet de REFERENTIELAAG van de wire om en niets
anders: elke verwijzing naar een gedeclareerd ding — attribuut, parameter,
dimensiecel, binder, variabele, rol, vouwbron, navigatiehop, creatiepad,
uniciteitsscope — draagt na L2 alleen nog haar symbolische vorm: geen
geschreven naamvelden naast de binding (B11, D53), geen bonenvelden naast het
rolpad (B7, B8), geen typeannotatie die een kopie van de declaratie is (B13),
geen bindingloze afleiding die elke lezer opnieuw uitrekent (B12), één
rollidmaatschapsknoop (B9), en geen `Onbekend` meer in de datatypesom (B16).
Alle andere knopen, velden en waardespellingen blijven byte-voor-byte L1 tot
hun eigen landing (L3–L5).

Goal: één ff-landing waarna alle veertien gates groen zijn op de L2-vorm van
de wire, alle documenten in de vier corpuszones en de pariteitszone in die
vorm staan (stand main `98d888ee`: 760 — golden 238, negatief 444, rood 48,
toka 30 — plus wat de kaarten `anker-nestkenmerk-dragervoorwaarde` en
`dialect-niet-in-uitzondering` vóór de rebase toevoegen), de drie emitters
hem schrijven, de vier consumenten hem lezen, gate 13 herzien is tot de
posities die na B13 een annotatie houden (V15 (b)), en de toka-zone
meeverhuist (gate 11 in regressievorm, README-divergentieregel L2). Deze
kaart raakt schema, verifier, ref, clingo, drools en alle drie de frontends
tegelijk; er lopen GEEN parallelle zonekaarten naast zodra bouwer C start.

## Besluiten die deze kaart uitvoert (niet heropenen)

De 23 besluiten van `research/v1-klap/BESLUITENREGISTER-L2-L5.md`
(sectie 1: D1–D21; sectie 5: D53, D56) gelden letterlijk. Ze staan hier in
één regel elk, zodat een bouwer het register niet hoeft te lezen; bij twijfel
wint de registertekst (die per besluit zijn grond noemt: IR-SPEC-sectie,
besluit V1–V17, naamprincipe of M1-precedent).

- **D1** Herstructureren = samenvoegen, splitsen, verplaatsen, sleutels
  hergroeperen, of een waarde van vorm/type veranderen → de knoop krijgt de
  IR-SPEC-naam voor zichzelf en zijn sleutels. Alleen sleutels verliezen of
  een verplichting krijgen → v0-naam en v0-sleutels blijven. L2 hernoemt
  precies één knoop: `RoleMembership`.
- **D2** `bron` (string | null) blijft onder die naam op elke knoop tot L5.
- **D3** Elke knoop draagt `node`, ook `RoleMembership`.
- **D4** Enumwaarden houden hun v0-spelling.
- **D5** `null`: (a) een veld dat IR-SPEC optioneel noemt gaat van null naar
  afwezig; (b) de door WF-034 bewaakte naamvelden (`attribuut`, `objecttype`,
  `parameter`, `kenmerk`, `subject`, de allocatievelden, `Literal.type`)
  blijven nullable; (c) wat een L2-delta verplicht maakt is schemaverplicht en
  non-null: `feittype`/`ankerRol`/`doelRol`/`joinRol` en
  `Tussenstap.rolBinding` (B7), `scopeRol` op een gescoopte uniciteit (B8),
  `factType`/`role` (B9), `verbindendeRol`, de `Kwantorvouw`-binding,
  `InstanceSetBranch.sourceRole`, en de vouwbinding behalve onder `TOTAAL`
  (B12, D7); (d) elke v0-waarde die de v1-vorm niet draagt laat het script
  luid falen met bestand en JSON-pointer.
- **D6** De Java-records van `frontends/regelspraak` (die `backends/drools`
  en de toolchain meecompileren) houden hun v0-vorm; alleen de wirelaag en de
  logica die verdwenen wire-informatie las veranderen. (OCaml-AST: U5.)
- **D7** Precies één van `bronRol`/`bronPad` op elke vouw met één argument
  die gekoppelde instanties leest; binding verplicht op `Kwantorvouw`,
  `ResolvedVerdeling.verbindendeRol` en `InstanceSetBranch.sourceRole`; een
  vouw met één argument zonder binding is de eigen lezing van het subject en
  alleen onder `TOTAAL` toegestaan; het script leidt ontbrekende bindingen af
  met de WF-048-afleiding en faalt luid bij dubbelzinnigheid.
- **D8** De setlezing van IR-SPEC 4.3 zonder `set`: (i) het enige argument
  van een instantievouw mét binding is de setbron; (ii) onder `AANTAL` telt
  die bron de bereikte instanties en is `attribuut` de geschreven rolnaam, die
  de gebonden rol MOET aanduiden (WF-047); (iii) onder de waardevouwen en
  `AANTAL_GEVULDE` wordt `attribuut` per bijdrager gelezen en resolveert het
  op `elementObjecttype`; (iv) zonder binding is het argument de eigen
  lezing. IR-SPEC 4.3 wordt daarop herschreven; WF-037's setarm leest
  "draagt een binding".
- **D9** De telbron zonder sentinel wordt herkend aan de bron zelf:
  `attribuut` noemt een rol van het vouwonderwerp en geen attribuut van het
  elementtype. WF-093 wordt tweezijdig ("zo'n bron eist `AANTAL`, en `AANTAL`
  eist zo'n bron"); WF-096 vervalt en zijn tellinghelft gaat op in WF-093;
  het sentinelhek van WF-034 arm (r) vervalt.
- **D10** `navigatieBindingen` is verplicht op elke lezing waarvan
  `objecttype` niet het subjecttype is; een lezing zonder binding is de eigen
  lezing; de nul-hop-zelfverwijzing wordt zo'n eigen lezing en de
  zelfaanduidingstoets van WF-053 vervalt met haar veld; het script leidt
  ontbrekende bindingen af met de WF-053-afleiding per typenpaar, faalt luid
  bij dubbelzinnigheid en meldt per document hoeveel het afleidde.
- **D11** `RoleMembership {node, factType, role, negated, bron}`: het script
  leidt `factType` en `role` af met WF-034 (o) (nabij) en (t) (ver), met het
  subjecttype uit de context; de geschreven `rol` moet de gevonden rol
  aanduiden op gedeclareerde naam of gedeclareerd meervoud (geen
  gesynthetiseerd meervoud); WF-034 krijgt een v1-arm die `factType`, `role`
  en de vulling door het subject kruist.
- **D12** B7/B8 in L2 alleen verwijderen: `zetVeld`, `joinVeld`,
  `Tussenstap.type`/`veld`/`viaOuderVeld` en `scopeVeld` weg, de symbolische
  velden verplicht; de knopen heten nog `ResolvedFeitCreatie`, `Tussenstap`,
  `ResolvedUniciteit` (hergroepering naar `path` en de namen `FactCreation`/
  `Uniqueness` in L4). De bindingloze scope van `anchor/jutterswet-kustregister`
  (tweede uniciteit) krijgt `scopeRol` afgeleid.
- **D13** Ook `VariabeleRef` verliest `type`.
- **D14** `type` wordt optioneel op het expressie-`Onopgelost`: afwezig =
  onbekend, aanwezig = een echt datatype; IR-SPEC 4.4 daarop herschreven;
  elke overblijvende `onbekend(`-aanroep in de frontend typeert of weigert
  luid. WF-044 ongewijzigd.
- **D15** `Onbekend` is in v1 een onbekende knoop en dus een laadfout; een
  onvindbaar domein valt op WF-003 (declaratie) of WF-018 (parameter).
- **D16** Effect pas in L4; in L2 blijft `COMPLEX` wat het is.
- **D17** Elke getuige waarvan de v1-vertaling gemeten op een andere regel
  valt wordt op die regel HERSCHREVEN (nieuwe mapnaam, nieuwe NOTE), ook als
  die vorm al een getuige heeft.
- **D18** Per herschreven of vervangen getuige een meldingsfragment van de
  verwachte arm, door gate 3 getoetst (mechanisme: U1).
- **D19/D56** Geen canonicalisatiepas in deze klap; de vijf `t`-tegen-`kg`-
  getuigen blijven herschreven (m).
- **D20** `EXPECTED_MIN_NEGATIEF` blijft 350; de stand wordt gemeld; alleen
  omlaag (gemotiveerd, zelfde commit) als de stand eronder komt.
- **D21** Gate 13 herzien: de twisttabel verliest `AttribuutRef`,
  `ParameterRef`, `DimensieCel`, `BinderRef`; `EXPECTED_MIN_TWISTS` in
  dezelfde commit naar de gemeten stand; `EXPECTED_MIN_DRL_PLAYED` opnieuw
  gemeten; intrekken alleen als de twistverzameling leeg is.
- **D53** `Kenmerk.navigatie` vervalt; `navigatieBinding` draagt de hop; de
  3 documenten zonder binding krijgen hem afgeleid; `Kenmerk` houdt zijn naam.

**Uitvoeringsbesluiten van de hoofdsessie (technisch, 2026-09-23):**

- **U1 — het armfragment van D18.** Elke herschreven of vervangen getuige
  van deze landing (elke rij van de stempellijst met een L2-delta en een
  stempel anders dan verwijderd) draagt naast `ir.json` en `NOTE.md` een
  bestand `expect.txt`: één regel, een letterlijk fragment van de
  verifiermelding van de verwachte arm, zonder JSON-pointer (een
  padverschuiving mag hem niet breken). Gate 3 (`scripts/check-verifier.sh`,
  onder kaartmandaat, per regel in het Result) toetst: staat `expect.txt` in
  de map, dan MOET het fragment letterlijk (`grep -F`) in de verifieruitvoer
  van dat document staan; een leeg of blanco `expect.txt` is een FAIL; een
  map zonder `expect.txt` wordt getoetst zoals vandaag (alleen het
  regelnummer). Het script meldt hoeveel fragmenten het toetste, met een
  vloer `EXPECTED_MIN_EXPECT` op de gemeten stand van deze landing. De
  stempellijst in `corpus/negatief/README.md` (sectie "L2", Engels, naar het
  M1-model met een kolom "arm") noemt per getuige de arm in woorden; het
  fragment staat alleen in het bestand (één bron).
- **U2 — schemaverplichting van de vouwbinding.** `Functie`: als
  `elementObjecttype` een string is en `kind` niet `TOTAAL`, dan precies één
  van `bronRol`/`bronPad` (JSON-schema `if`/`then` met `oneOf` op
  `required`), en nooit beide (ook niet onder `TOTAAL`); `Kwantorvouw`:
  precies één van beide, altijd; `ResolvedVerdeling.verbindendeRol` required;
  `InstanceSetBranch.sourceRole` required en non-null. De 15 groene eigen
  lezingen (alle `TOTAAL` met `elementObjecttype` gelijk aan het `objecttype`
  van het argument, DV L2-1) blijven zo schrijfbaar; de verifier bewaakt dat
  een bindingloze `TOTAAL` werkelijk de eigen lezing is (WF-083, D8 (iv)).
  Precedent voor een schemaconditie: het `drempel`-veld van `Kwantorvouw`
  (WF-108 (e)). Uitvoering (A1, door de hoofdsessie aanvaard): de voorwaarde
  op `Functie` geldt alleen bij precies één argument, want de opgesomde
  EERSTE/LAATSTE-vouwen dragen een vestigiaal `elementObjecttype` zonder
  bereik (5 in twee goldens, 2 in toka, 10 in negatief).
- **U3 — de ene `Onbekend` buiten de referentieposities in de
  pariteitszone.** `toka/casus/ir.json`, `/rules/resolvedRegels/16/waarde/type`
  is een `Binair.type` met `Onbekend` (toka-45, de 16Y-divergentie: datum +
  datum). Na B16 schrijft de emitter op een AFGELEIDE positie die de frontend
  niet kan typeren `"type": null` — het schema laat dat op `Binair.type` al
  toe; het is geen marker, want de expressie is opgelost, alleen niet
  typeerbaar — en géén `Onopgelost` en géén ERROR (die zou de hele dump
  weigeren). Op elke andere positie (declaratie, payload, `Literal.type`)
  weigert de emitter een in-memory `Onbekend` luid. De README-divergentieregel
  L2 noemt die ene knoop; toka-45 blijft holdout. De verifier verandert zijn
  houding tegenover een null-annotatie op een afgeleide positie in L2 niet
  (A2 meldt in het Result wat hij vandaag doet; L3/L4 beslissen erover).
  Uitvoering (A1, door de hoofdsessie aanvaard): `m2.py` schrijft een
  `Onbekend` als `type` van `Binair`, `Functie`, `Afgerond` of `Begrensd` als
  `null` en telt het (in het corpus alleen deze ene knoop), zodat de dump
  mechanisch migreert; op elke andere positie buiten een referentietype
  weigert het script een `Onbekend`.
- **U4 — de afleidingen van het migratiescript spiegelen de verifier.**
  `m2.py` leidt bindingen (D7, D10, D53), de scope (D12) en `factType`/`role`
  (D11) af met exact de resolutie die `spec/verifier/lib/checks.ml` vandaag
  onder WF-048, WF-053, WF-054 en WF-034 (o)/(t) uitvoert (supertype-⊑ van
  ch. 2.2 inbegrepen), met het subjecttype uit dezelfde context als de
  verifier (het regeldoel; `elementObjecttype` van vouw en kwantorvouw;
  `bronType` van de verdeling; `subject` van creatie, uniciteit en
  consistentie; de `Default`-drager; de functieparameter). Twee onafhankelijke
  bewijzen dat de afleiding klopt: gate 3 (de verifier kruist elke binding
  onder WF-047) en gates 10/11 (de frontend emitteert zijn eigen bindingen;
  de her-emissie moet byte-identiek zijn aan de mechanische migratie). Bij
  nul of meer dan één kandidaat faalt het script met bestand en pointer; die
  documenten zijn per constructie de gestempelde getuigen of een fout in de
  meting — beide in het Result.
- **U5 — de OCaml-AST verliest de velden.** `spec/verifier/lib/ast.ml`
  (gedeeld door verifier, ref, clingo, catala en flogic via de bibliotheek
  `uchar-ir-v0`) verliest `set`, `navigatieRol`, `navigatiePad`,
  `onderwerpBron`, het `type` op de vijf referentieklassen, `zetVeld`,
  `joinVeld`, `Tussenstap.type`/`veld`/`viaOuderVeld`, `scopeVeld`,
  `anderTeFiller`/`filler`/`backref`, `Kenmerk.navigatie` en de variant
  `Onbekend`, zodat de compiler elke lezer vindt (DV §2.4: checks.ml 82,
  eval.ml 56, translate.ml 82 treffers) en er geen stille terugval kan
  blijven staan. Dit is de fail-fast-lezing van D6: D6 beschermt de
  Java-records vanwege de toolchainkoppeling; de OCaml-AST heeft die
  koppeling niet.
- **U6 — de Java-records houden hun velden, de lezers niet.** De
  frontendrecords (`Expr.AttribuutRef` enz.) houden hun v0-velden (D6);
  `IrEmitter` schrijft ze niet; de Drools-`IrLoader` weigert ze bij naam als
  onbekend veld en vult in de records `null`/`false`/`[]`. Meetbare eis: na
  L2 leest geen regel in `backends/drools/src/main` buiten de lader nog
  `.set()`, `.navigatieRol()`, `.navigatiePad()`, `.onderwerpBron()`,
  `.zetVeld()`, `.joinVeld()`, `.veld()`, `.viaOuderVeld()`, `.scopeVeld()`,
  `.anderTeFiller()`, `.filler()`, `.backref()`, `.navigatie()` of `.type()`
  van een referentieknoop (grep-telling 0 in het Result); de vormpredikaten
  (eigen versus genavigeerd, keten versus enkele stap — de P1-rest) lezen de
  binding: geen `navigatieBindingen` = eigen lezing, lengte > 1 = keten.
- **U7 — de drie twistgetuigen van gate 12** (`BonenveldWitnessTest`,
  `NavigatiebindingWitnessTest`, `VerdelingsrolWitnessTest`) verliezen hun
  twistdoel (er is geen bonenveld of geschreven naam meer om te verdraaien)
  en worden herschreven tot laderweigeringsgetuigen: het verdraaide veld is
  nu een onbekend veld dat de lader bij naam weigert; per positie blijft één
  getuige dat de transpilers de BINDING volgen (een verkeerde binding geeft
  een luide, verkeerde uitkomst of een weigering, nooit stil het oude
  antwoord).
- **U8 — opvolgernamen van de vier vervangen getuigen** kiest A2 (mapnaam met
  de opvolgerregel als prefix); de README-lijst noemt oud → nieuw.
- **U9 — de ref-suite en de clingo-suite** (`ref/test/test_ref.ml`, 56
  treffers op geschrapte AST-velden; `backends/clingo/test/test_clingo.ml`,
  761 `"node"`-sleutels) worden op de L2-vorm gebracht; tests die uitsluitend
  een geschrapte vorm pinnen vervallen met een telling in het Result; de
  vloeren `EXPECTED_MIN_EXAMPLES` (489) en `EXPECTED_MIN_SUITE_CHECKS` (83)
  alleen omlaag als de stand eronder komt, gemotiveerd, zelfde commit.
- **U10 — `Onopgelost.type` in de migratie:** `Onbekend` of `null` → afwezig;
  een echt datatype blijft (`corpus/rood/wf044-onopgelost-reached` draagt
  `Numeriek` en houdt het).
- **U11 — de telbronnaam:** waar de geschreven naam op de `AANTAL`-bron de
  gebonden rol niet aanduidt op gedeclareerde naam of gedeclareerd meervoud,
  schrijven migratie en emitter de gedeclareerde rolnaam in `attribuut`;
  WF-047 blijft 'moet aanduiden op naam of meervoud'. Normvraag voor de
  maintainer met dit als standaardantwoord (hoofdsessie 2026-09-24; gemeten
  door A1: 2 goldens, 4 toka-plekken).

## Lens-checklist — vooraf, per bouwer (de terugkerende vondstklassen van L1)

Elke bouwer loopt deze lijst vóór zijn Result na en noemt per punt wat hij
mat; de lens toetst op de unie strikt deze lijst plus de kaart, in één ronde.

1. **Elke nieuwe of veranderde wirepositie door alle vier de lezers
   gecontroleerd** (verifier-lader; Drools-lader; de OCaml-consumenten via de
   gedeelde lader; catala/flogic als schrijvers): `RoleMembership.factType`/
   `role`, de verplichte bindingen, `scopeRol`, `Tussenstap.rolBinding`,
   `Onopgelost.type?`. Niet "geladen en genegeerd".
2. **Onbekende velden bij naam geweigerd in elke lader**, ook de vervallen
   velden: een document met `set`, `navigatieRol`, `type` op een referentie,
   `zetVeld`, `scopeVeld`, `anderTeFiller`, `Kenmerk.navigatie` of een
   `Onbekend`-knoop laadt nergens (gate-2-zelftestrijen; laderweigerings-
   getuigen in Drools en in de OCaml-suites).
3. **Geen v0-namen in meldingen, commentaren, NOTE-citaten, javadoc**: geen
   "set flag", geen `navigatieRol` in een verifiermelding, geen
   `anderTeFiller` in een Drools-weigering; NOTE.md-citaten opnieuw gemaakt
   uit de echte verifieruitvoer.
4. **Tests die een verboden vorm pinnen worden herschreven, niet verwijderd**
   (tenzij de vorm onschrijfbaar is: dan een telling in het Result):
   `IrEmitterTest`, de 27 Drools-getuigentests met ingebedde documenten, de
   8 frontendtests met ingebedde documenten, `test_ref.ml`, `test_clingo.ml`,
   de flogic-suite.
5. **Sleutelvolgorde = schema-`properties`-volgorde** voor `RoleMembership`;
   v0-volgorde zonder de vervallen sleutels voor de rest; script en alle
   drie de emitters schrijven exact die volgorde (gate 2 round-trip; gates
   9/10/11/14 byte-identiek).
6. **Elke lezer dezelfde defaults**: `Onopgelost` zonder `type` = onbekend
   (nooit `Numeriek`); vouw zonder binding = eigen lezing (nooit stil "uniek
   afleidbaar"); lezing zonder `navigatieBindingen` = eigen lezing, en
   `objecttype` MOET dan het subjecttype zijn; `negated` zoals geschreven.
7. **Per getuige de verwachte arm** (U1 `expect.txt`) en per verwijderde
   getuige de meting dat het script erop faalt of dat het onderwerp weg is.
8. **Vloeren gemotiveerd** (`EXPECTED_MIN_NEGATIEF`, `_TWISTS`, `_EXAMPLES`,
   `_SUITE_CHECKS`, `_TESTS` van gates 10/12, `_CHECKS` van gate 14): alleen
   omlaag bij een gemeten stand eronder, in dezelfde commit, met de reden in
   het script.
9. **Gate-12-pins hermeten**, elke kanaalwissel bij naam met reden (een
   golden die door zijn nieuwe binding gaat spelen is een verwachte wissel —
   noem hem; P3 liet dat bij `bergingstoezicht` al zien).
10. **V15/gate 13 herzien**: twisttabel, klemtoets (step 6) en scriptkop
    stemmen overeen met wat na B13 nog een annotatie draagt.
11. **Geen stille terugval**: waar v0-code "als het veld ontbreekt, leid af"
    deed, doet L2-code "als de binding ontbreekt, is het de eigen lezing of
    een weigering" — nooit een derde weg.
12. **Diagnostics en `bron` ongemoeid** (WF-113 (b) zoekt de `bron` van elk
    `Onopgelost` letterlijk; D2).

## De L2-vorm van de wire (normatief voor deze kaart)

Canonieke sleutelvolgorde: voor `RoleMembership` de `properties`-volgorde
van het schema; voor elke andere knoop de v0-volgorde zonder de vervallen
sleutels. Serialisatie ongewijzigd (twee spaties, `\uXXXX`-escapes, lege
containers inline). Alles wat hieronder niet genoemd wordt is byte-voor-byte
L1.

**Referentieknopen (B11, B13, D10, D13):**

```
AttribuutRef  { "node": "AttribuutRef", "attribuut": string|null, "objecttype": string|null,
                "bron": string|null,
                "navigatieBindingen"?: [ RolBinding, ... ] }   // minItems 1; aanwezig ⇔ objecttype ≠ subjecttype
ParameterRef  { "node": "ParameterRef", "parameter": string|null, "bron": string|null }
DimensieCel   { "node": "DimensieCel", "attribuut": expr|null, "labels": [ string|null ], "bron": string|null }
BinderRef     { "node": "BinderRef", "name": string, "bron": string|null }
VariabeleRef  { "node": "VariabeleRef", "naam": string|null, "bron": string|null }
```

Vervallen op `AttribuutRef`: `onderwerpBron`, `navigatieRol`, `navigatiePad`,
`set`, `type`; op de vier andere: `type`. Een lezing zonder
`navigatieBindingen` is de eigen lezing van het subject (`objecttype` =
subjecttype; verifier, WF-053); de nul-hop-zelfverwijzing
(`jutterswet-aanmelderkrediet`, het voorbeeld van IR-SPEC 4.1a) en de
subjectaanduidende `navigatieRol` op de telbron (`jutterswet-aanmeldertelling`)
worden zulke eigen lezingen.

**Vouwen en bereiken (B12, D7, D8, D9, U2):**

```
Functie            sleutels ongewijzigd; "bronRol"?: RolBinding / "bronPad"?: [ RolBinding ]:
                   precies één van beide als elementObjecttype ≠ null en kind ≠ TOTAAL (schema);
                   onder TOTAAL hoogstens één (bindingloos = eigen lezing over het eigen attribuut);
                   "type" blijft (afgeleide positie, V5 (a))
Kwantorvouw        sleutels ongewijzigd; precies één van bronRol/bronPad (schema)
ResolvedVerdeling  sleutels ongewijzigd; "verbindendeRol": RolBinding verplicht
InstanceSetBranch  sleutels ongewijzigd; "sourceRole": RolBinding verplicht, non-null
```

Lezing van de vouwbron: D8. De telbron wordt herkend aan `attribuut` (een rol
van het vouwonderwerp, geen attribuut van het elementtype), niet aan een
sentinel (D9).

**Rollidmaatschap (B9, D11)** — de ene hernoemde knoop, sleutelvolgorde =
deze:

```
RoleMembership { "node": "RoleMembership", "factType": string, "role": string,
                 "negated": boolean, "bron": string|null }
```

`factType` = een gedeclareerd feittype; `role` = de rol die het SUBJECT
daarin speelt, op gedeclareerde naam; betekenis: er bestaat een link van
`factType` waarin het subject `role` vult (`negated`: er bestaat er geen).
Vervangt `Rollidmaatschap {rol, anderTeFiller, negatie, bron}` en
`VerreRollidmaatschap {filler, backref, negatie, bron}`; de
topniveaubeperking van de verre vorm vervalt (IR-SPEC 4.5, rij
`RoleMembership`; CONSTRUCTS rij 5).

**Creatie en uniciteit (B7, B8, D12):**

```
ResolvedFeitCreatie { "node", "naam", "anker", "doel", "tussenstappen": [ Tussenstap ], "voorwaarde", "geldigheid",
                      "feittype": string, "ankerRol": string, "doelRol": string, "joinRol": RolBinding }  // alle vier verplicht, non-null
Tussenstap          { "node": "Tussenstap", "rolBinding": RolBinding }                                      // verplicht
ResolvedUniciteit   { "node", "naam", "subject", "sleutels", "bron", "geldigheid", "scopeRol"?: RolBinding }  // scopeRol ⇔ gescoopt
```

Vervallen: `zetVeld`, `joinVeld`, `Tussenstap.type`, `Tussenstap.veld`,
`Tussenstap.viaOuderVeld`, `scopeVeld`.

**Kenmerkcheck (D53):** `Kenmerk { "node", "kenmerk", "negatie", "eigenaar",
"periode", "bron", "navigatieBinding"?: RolBinding }`; `navigatie` vervalt.

**Onopgelost (D14, U10) en de datatypesom (B16, D15):** expressie-
`Onopgelost { "node": "Onopgelost", "type"?: datatype, "bron" }` (`type`
afwezig = onbekend; nooit `null`); de conditievariant ongewijzigd. De
`datatype`-unie verliest `Onbekend`; de `$def` verdwijnt.

Een v0-waarde die de L2-vorm niet kan dragen — een bindingloze vouw, lezing,
verdeling, kwantorvouw, uniciteitsscope of kenmerknavigatie waarvoor de
afleiding nul of meer dan één kandidaat geeft; een rollidmaatschap waarvan
`rol` de gevonden rol niet aanduidt; een feitcreatie zonder volledig
symbolisch blok; een `Onbekend` buiten een referentie-`type` of
`Onopgelost.type`; `InstanceSetBranch.sourceRole: null` — laat `m2.py` LUID
falen met bestandsnaam en JSON-pointer; het script repareert niets
stilzwijgend.

## Het migratiescript

`spec/proposals/v1-migratie/m2.py`, naar het model van `m1.py`:
`migrate_bytes(raw) -> bytes`, `--all` (de vier corpuszones + `toka/`),
`--check` (meldt elk document dat nog een L1-vorm draagt; rc ≠ 0),
idempotent (een tweede run wijzigt geen byte), en een L2-document laat het
staan. Het script leest het model van het document zelf (`model.factTypes`
met `Rol.naam`/`meervoud`/`objecttypeNaam`/`meervoudig`, `model.objectTypes`
met `supertype`) voor de afleidingen van U4 en schrijft per document één
regel naar stderr met wat het afleidde en verwijderde (vouwbindingen,
navigatiebindingen, rollidmaatschappen, scopes, kenmerkhops, referentietypes,
sentinels), zodat het Result de tellingen per zone draagt. Verwacht (DV §2.4,
gemeten op `16234880`): L2 raakt 705 documenten (a/g/r/n/t = 35/195/45/401/29);
6 groene documenten krijgen een vouwbinding (niet 16: de 15 tijdlijntotalen
zijn eigen lezingen); de 2 groene geschreven navigaties zonder binding worden
eigen lezingen; de bindingloze typenpaar-lezingen (niet geteld) krijgen een
binding, per document gemeld; 3 documenten een kenmerkhop; 8 + 2 documenten
een `RoleMembership` (12 + 6 knopen); 1 groen document
(`anchor/jutterswet-kustregister`) een `scopeRol`; 3010 referentietypes
verdwijnen en alle 141 `Onbekend`-knopen (134 met het referentietype, 6 in
gestempelde getuigen, 1 via U3). De weigeringen van het script zijn per
constructie de verwijderde getuigen van de stempellijst (voor zover hun
defect het script raakt) plus de bindingloze negatief-documenten waarvoor de
afleiding faalt (A2 herschrijft of stempelt ze); elke andere weigering is een
bevinding.

## Regels die veranderen (IR-SPEC + verifier, bouwer A2)

- **Vervalt geheel:** WF-048 (B12; «retires with the format bump», IR-SPEC
  2.6), WF-096 (D9; de tellinghelft gaat op in WF-093), WF-078 (B13),
  WF-034 arm (r) (het sentinelhek) en arm (m) (de bonenvelden van de
  feitcreatie); de zelfaanduidingstoets en de geschreven-naamarmen van
  WF-053; de bindingloze-terugvalarm van WF-054; de bonenveld-alleen-arm van
  WF-028; de referentiearmen van WF-091, WF-089 (leeshelft) en WF-090
  (leeshelft); de expressiewandelarm van WF-007. Elk krijgt in IR-SPEC een
  geschiedenisregel ("until L2 …"), zoals M1 dat voor WF-094 deed.
- **Verandert:** WF-093 tweezijdig (D9); WF-034 krijgt de v1-rolarm voor
  `RoleMembership` (D11); WF-053 krijgt de arm "een `AttribuutRef` zonder
  `navigatieBindingen` waarvan `objecttype` niet het subjecttype is" (D10) en
  houdt de gebonden armen (onbekend feittype, stap past niet, to-many in een
  scalaire positie); WF-037's setarm leest "draagt een binding"; WF-083 leest
  de vouwbron per positie (D8) in plaats van aan `set`; WF-003/WF-018 krijgen
  de onvindbaar-domein-lezing (D15); WF-047 blijft (de geschreven rolnaam op
  de telbron MOET de gebonden rol aanduiden); WF-110 verliest de
  `sourceRole`-null-arm (schemaverplicht, D5 (c)).
- **Spec-tekst die mee moet** (DV §2.1): IR-SPEC 4.1 (tabel: geen `type` op
  `AttributeReference`, `ParameterReference`, `VariableReference`), 4.1a (één
  expliciet pad; de nul-hop-zelfverwijzing als eigen lezing), 4.3 (de 41
  `set`-regels; WF-093; WF-096; de `AANTAL_GEVULDE`-tabel; WF-037's setarm;
  `DimensionCellRead`), 4.4 (`Unresolved` zonder declared-unknown type),
  4.5 (`RoleMembership`; `CharacteristicCheck.navigation`), 4.7
  (`BinderRef`), 3.4 (spiegelalinea bonenvelden → geschiedenis), 3.6
  (`scopeVeld`), 2.6 (WF-047/WF-048), 2.4 (`Onbekend`-delta → geland), 4.6
  (armen (m), (r), de rolarm), de WF-index; `RUNTIME-SEMANTICS.md` ch. 2.10
  (de consumentenclausule: op referentieposities bestaat de annotatie niet
  meer, de declaratie is de enige bron) en ch. 4.1 (de verdeling zonder
  afleiding).

## Stempellijst (V14 (a), D17, D18) — de getuigen van L2

Bron: `research/v1-klap/stempelmeting-negatief-l2-l5.md` (L2-tabel; de
controletabel-rijen met een L2-delta of zonder delta; §9b), per getuige
gemeten op `16234880`; de kolom "grond" noemt het registerbesluit waar het
register de meting overstemt. Alle 137 mappen bestaan op main (gecontroleerd
2026-09-23). Stempels: **verwijderd-met-reden 43**, **herschreven (m) 53**
(waarvan 22 zonder L2-delta: de S3-rijen van de controletabel, die alleen
mechanisch meegaan en geen werk vragen — ze staan hier als bewijs van D5 (b)),
**herschreven (i) 11**, **herschreven op een andere regel 24** (WF-063 7,
WF-023 5, WF-074 4, WF-076 2, WF-070 2, WF-036 2, WF-053 2 — nieuwe mapnaam
met die regel als prefix, nieuwe NOTE, `expect.txt`), **vervangen 4** (U8),
**armwissel binnen de eigen regel 2** (fragment in `expect.txt`). Daarnaast
meet A2 de gesignaleerde kandidaten van ST §9b en stempelt ze:
`wf083-fold-source-navigates-away`, `wf083-vouwbron-anker-op-operand`,
`wf083-vouwbron-navigatierol-buiten-de-wrat` (staan op `navigatieRol`:
verwacht verwijderd), `wf083-vouwbron-anker-op-tussentype`,
`wf083-vouwbron-vreemd-elementtype`, `wf083-vouwbron-vreemd-objecttype`
(verwacht herschreven), `wf037-aantalgevulde-niet-setbron` (verwacht
herschreven (i): één argument zonder binding en zonder `elementObjecttype`),
`wf110-unie-tak-zonder-bronrol` (verwacht verwijderd: `sourceRole` is
schemaverplicht). De 36 negatief-documenten met een bindingloze vouw en de 6
met een bindingloze verdeling die niet in deze lijst staan krijgen hun
binding van het script (of een stempel als de afleiding faalt). Verwachte
negatief-stand: 444 − 43 − (0 … 5) ≈ 396–401, boven de vloer; vervangen en
herschreven-op-andere-regel zijn netto nul.

Legenda: (m) = mechanisch, het script vertaalt en de regel vuurt; (i) = met de
hand herschreven op de L2-vorm; "op WF-xxx" = herschreven op de gemeten
opvolgerregel met nieuwe mapnaam.

| map | regel | delta | stempel | v1-vorm / reden (ST-meting, register-overstemming erbij) | grond |
|---|---|---|---|---|---|
| `wf091-aantal-decimalen` | WF-091 | B13 (+B21, L4) | **herschreven (i)** | Afgeleide arm AANTAL; de telling houdt haar annotatie (V5 (a)). v1: `count` met `type` 2 decimalen tegen afleiding 0. De tweede operand (bijvangst WF-037) moet eruit als de v1-telling geen operandenlijst draagt (afgeleid, B21). | — |
| `wf091-aantalgevulde-eenheid` | WF-091 | B13 | **herschreven (m)** | Afgeleide arm AANTAL_GEVULDE (v1 `filledCount`) getypeerd `kg` tegen eenheidloos. Geen referentieleugen in het document (gemeten: B13-simulatie verandert niets). | — |
| `wf091-afgerond-decimalen` | WF-091 | B13 | **herschreven (m)** | Afgeleide arm Rounding: 4 decimalen tegen afleiding 0. | — |
| `wf091-afgerond-eenheid` | WF-091 | B13 (+B17, L3) | **herschreven (m)** | Afgeleide arm Rounding: `t` tegen `kg` (converteerbare klasse). | S6 |
| `wf091-attribuutref-decimalen` | WF-091 | B13 | **verwijderd-met-reden** | Referentiearm; het `type` van de referentie verlaat de wire. Gemeten: met het gedeclareerde type verifier PASS. | — |
| `wf091-attribuutref-eenheid-converteerbaar` | WF-091 | B13 | **verwijderd-met-reden** | Referentiearm; gemeten PASS. | — |
| `wf091-attribuutref-eenheid-erbij` | WF-091 | B13 | **herschreven (op WF-023)** | Referentiearm vervalt. Gemeten: de vertaling valt op WF-023 `assignment across incompatible units` (eenheidloze declaratie in een `kg`-doel). WF-023 heeft vandaag geen getuige van die clausule. Nieuwe NOTE nodig. | S1 |
| `wf091-attribuutref-eenheid-kast` | WF-091 | B13 | **verwijderd-met-reden** | Referentiearm; gemeten PASS. | — |
| `wf091-attribuutref-eenheid-ontbreekt` | WF-091 | B13 | **herschreven (op WF-023)** | Als `-erbij`, de andere eenzijdige richting (`kg`-declaratie in een eenheidloos doel); gemeten WF-023. | S1 |
| `wf091-attribuutref-geheel` | WF-091 | B13 | **verwijderd-met-reden** | Referentiearm; gemeten PASS. | — |
| `wf091-attribuutref-reststructuur` | WF-091 | B13 | **herschreven (op WF-023)** | Referentiearm. Gemeten: vertaling valt op WF-023 `a list value is assigned to a sumtype target`; vorm gedekt door `wf023-schrijf-lijst-geen-domein-waarde`. | D17 |
| `wf091-attribuutref-soort` | WF-091 | B13 | **herschreven (op WF-023)** | Referentiearm. Gemeten: vertaling valt op WF-023 (getal in tekstdoel), de spiegel van `wf023-text-into-numeric`. | D17 |
| `wf091-attribuutref-teken` | WF-091 | B13 | **verwijderd-met-reden** | Referentiearm; gemeten PASS. | — |
| `wf091-begrensd-eenheid` | WF-091 | B13 (+B17, L3) | **herschreven (m)** | Afgeleide arm Clamp: `t` tegen `kg` (converteerbare klasse). | S6 |
| `wf091-binair-decimalen` | WF-091 | B13 | **herschreven (m)** | Afgeleide arm PLUS: 2 decimalen tegen 4. | — |
| `wf091-binair-eenheid` | WF-091 | B13 (+B17, L3) | **herschreven (m)** | Afgeleide arm PLUS: `t` tegen `kg` (converteerbare klasse). | S6 |
| `wf091-binair-geheel` | WF-091 | B13 | **herschreven (m)** | Afgeleide arm PLUS: geheel tegen fractioneel. | — |
| `wf091-binair-soort` | WF-091 | B13, B14 (L3) | **herschreven (i)** | Afgeleide arm PLUS, getypeerd `Percentage` over twee gewone getallen. Na B14 is een percentage `Number` met eenheid `%`: de melding verschuift van het soortfacet naar het eenheidsfacet (afgeleid; nog steeds WF-091). Om het soortfacet te blijven pinnen: annotatie vervangen door een niet-numerieke soort, bijv. `Boolean`. | D18 |
| `wf091-binderref-eenheid` | WF-091 | B13 | **herschreven (op WF-076)** | Referentiearm (`BinderRef`). Gemeten: vertaling valt op WF-076 (Let-type `t` tegen lichaam `kg`); die arm is gedekt door `wf076-body-type-mismatch`. | D17 |
| `wf091-dimensiecel-eenheid` | WF-091 | B13 | **verwijderd-met-reden** | Referentiearm (`DimensieCel`); gemeten PASS. | — |
| `wf091-fold-decimalen` | WF-091 | B13 | **herschreven (m)** | Afgeleide arm MINIMUM: 4 decimalen tegen 2. | — |
| `wf091-foldbron-eenheid` | WF-091 | B13 | **verwijderd-met-reden** | Vouwbronarm (`AttribuutRef`). De melding op de SOM-afleiding volgde uit de bronleugen; tegen de declaratie klopt de SOM-annotatie. Gemeten PASS. | — |
| `wf091-foldwortel-eenheid` | WF-091 | B13 (+B17, L3) | **herschreven (m)** | Afgeleide arm MAXIMUM: `t` tegen `kg` (converteerbare klasse). | S6 |
| `wf091-maal-eenheid` | WF-091 | B13 | **herschreven (m)** | Afgeleide arm MAAL: `EUR/jr` tegen afleiding `EUR`. | — |
| `wf091-parameterref-eenheid` | WF-091 | B13 | **verwijderd-met-reden** | Referentiearm (`ParameterRef`); gemeten PASS. | — |
| `wf091-schrijfdoel-eenheid` | WF-091 | B13 | **verwijderd-met-reden** | Doelarm: het regeldoel is een `AttribuutRef` en verliest zijn `type`. Gemeten PASS. | — |
| `wf091-som-eenheid` | WF-091 | B13 (+B17, L3) | **herschreven (m)** | Afgeleide arm SOM: `t` tegen `kg` (converteerbare klasse). | S6 |
| `wf091-totaal-eenheid` | WF-091 | B13 | **herschreven (m)** | Afgeleide arm TOTAAL: `EUR/jr` tegen afleiding `EUR`. | — |
| `wf089-aggregatie-doorgeefluik-enum` | WF-089 | B13 | **herschreven (m)** | Doorgeef-aggregatiearm: de EERSTE-annotatie (afgeleide positie, blijft) noemt `Vaarwater`, operand 0 `Kust`. Bijvangst WF-038 blijft. | — |
| `wf089-attribuutref-enum` | WF-089 | B13 | **herschreven (op WF-063)** | Referentiearm. Gemeten: vertaling valt op WF-063 (armen buiten het gedeclareerde domein); gedekt door `wf063-enum-alien-value` en `wf063-enum-missing-value`. | D17 |
| `wf089-attribuutref-geen-domein` | WF-089 | B13 | **herschreven (op WF-023)** | Referentiearm. Gemeten: vertaling valt op WF-023 (enumeratie in getaldoel), de spiegel van `wf023-defaults-soort`/`wf023-objectcreatie-soort`. | D17 |
| `wf089-attribuutref-geen-keuzetype-declaratie` | WF-089 | B13 | **herschreven (op WF-063)** | Referentiearm vervalt. Gemeten: vertaling valt op WF-063 `match scrutinee is number, which is no keuzetype`; die arm heeft vandaag geen getuige. | S1 |
| `wf089-attribuutref-lijst-sum` | WF-089 | B13 | **herschreven (op WF-070)** | Referentiearm vervalt. Gemeten: vertaling valt op WF-070 (lambdaparameter `oordeel`, verwacht `beschikking`); die arm heeft vandaag geen getuige. | S1 |
| `wf089-attribuutref-navigatie-enum` | WF-089 | B13, B11 | **herschreven (op WF-063)** | Referentiearm (genavigeerde lezing). Gemeten: vertaling valt op WF-063, gedekt. | D17 |
| `wf089-attribuutref-sum` | WF-089 | B13 | **herschreven (op WF-063)** | Referentiearm. Gemeten: vertaling valt op WF-063, gedekt. | D17 |
| `wf089-binderref-sum` | WF-089 | B13 | **herschreven (op WF-063)** | Referentiearm (`BinderRef`). Gemeten: vertaling valt op WF-063, gedekt. | D17 |
| `wf089-composiet-domein` | WF-089 | B13 | **herschreven (m)** | Afgeleide arm (PLUS met keuzetype-annotatie); de composiet houdt zijn annotatie. | — |
| `wf089-dimensiecel-enum` | WF-089 | B13 | **herschreven (op WF-063)** | Referentiearm (`DimensieCel`). Gemeten: vertaling valt op WF-063, gedekt. | D17 |
| `wf089-enum-over-som` | WF-089 | B13 | **herschreven (op WF-036)** | Referentiearm vervalt. Gemeten: vertaling valt op WF-036 `equality across families: sumtype vs enumeration`; WF-036 heeft vandaag geen getuige van die arm. | S1 |
| `wf089-foldbron-enum` | WF-089 | B13 | **verwijderd-met-reden** | Vouwbronarm vervalt. Gemeten: de vertaling blijft WF-089, maar via de doorgeef-aggregatiearm met exact de melding van `wf089-aggregatie-doorgeefluik-enum` — een armwissel die gate 3 niet ziet. | D18 |
| `wf089-minimum-doorgeefluik` | WF-089 | B13 | **herschreven (m)** | Doorgeef-aggregatiearm (MINIMUM). Bijvangst WF-038 blijft. | — |
| `wf089-parameterref-enum` | WF-089 | B13 | **herschreven (op WF-063)** | Referentiearm (`ParameterRef`). Gemeten: vertaling valt op WF-063, gedekt. | D17 |
| `wf089-som-over-enum` | WF-089 | B13 | **herschreven (op WF-036)** | Referentiearm. Gemeten: vertaling valt op WF-036, de spiegelrichting van `wf089-enum-over-som`; één getuige van die arm volstaat. | D17 |
| `wf090-attribuutref` | WF-090 | B13 | **herschreven (op WF-074)** | Referentiearm. Gemeten: vertaling valt op WF-074 (`bedrag` is geen veld van `dossier`), gedekt door `wf074-unknown-field`. | D17 |
| `wf090-attribuutref-lijst` | WF-090 | B13 | **herschreven (op WF-070)** | Referentiearm vervalt. Gemeten: vertaling valt op WF-070 (lambdaparameter `logboek`, verwacht `dossier`); de structzijde naast `wf089-attribuutref-lijst-sum`. | S1 |
| `wf090-binderref` | WF-090 | B13 | **herschreven (op WF-074)** | Referentiearm (`BinderRef`). Gemeten: vertaling valt op WF-074, gedekt. | D17 |
| `wf090-dimensiecel` | WF-090 | B13 | **herschreven (op WF-074)** | Referentiearm (`DimensieCel`). Gemeten: vertaling valt op WF-074, gedekt. | D17 |
| `wf090-doorgeefluik` | WF-090 | B13 | **herschreven (m)** | Doorgeef-aggregatiearm (EERSTE). Bijvangst WF-038 blijft. | — |
| `wf090-foldbron` | WF-090 | B13 | **verwijderd-met-reden** | Vouwbronarm vervalt. Gemeten: vertaling blijft WF-090 via de aggregatiearm, met de melding van `wf090-doorgeefluik` (armwissel, onzichtbaar voor gate 3). | D18 |
| `wf090-parameterref` | WF-090 | B13 | **herschreven (op WF-074)** | Referentiearm (`ParameterRef`). Gemeten: vertaling valt op WF-074, gedekt. | D17 |
| `wf090-schrijf-defaults` | WF-090 | — | **herschreven (m)** | Schrijfhelft: een `StructLiteral` (`logboek`; zijn `type` blijft) in een `dossier`-doel. Een echte typefout, ook in v1. | — |
| `wf090-schrijf-objectcreatie` | WF-090 | — | **herschreven (m)** | Als `wf090-schrijf-defaults`, in een objectcreatie-toekenning. | — |
| `wf090-schrijf-regeldoel` | WF-090 | — | **herschreven (m)** | Als `wf090-schrijf-defaults`, als regelwaarde. | — |
| `wf088-functiondef-drift` | WF-088 | — | **herschreven (m)** | De afwijkende waardenlijst staat op `FunctionDef`-parameter en -resultaat (declaratie). De v1-variant `Enumeration` houdt `values` (IR-SPEC ch. 2.4). Bijvangst WF-060 blijft. | — |
| `wf088-literal-type-drift` | WF-088 | B22 (L4) | **herschreven (m)** | Drift op het `type` van een `Literal`; dat type blijft (V9 (a), voorwaarde 2). | — |
| `wf088-use-site-value-drift` | WF-088 | — | **herschreven (m)** | Drift op de attribuutdeclaratie. | — |
| `wf088-use-site-value-order` | WF-088 | — | **herschreven (m)** | Volgordedrift op de attribuutdeclaratie. | — |
| `wf078-binderref-type-lie` | WF-078 | B13 | **herschreven (op WF-076)** | `BinderRef` verliest zijn `type`. Gemeten: vertaling valt op WF-076 (Let-type tekst tegen lichaam getal); gedekt door `wf076-body-type-mismatch`. | D17 |
| `wf034-sentinel-builtin-operand` | WF-034 (sentinelhek) | B13 | **verwijderd-met-reden** | Sentinel op de lezing van een gedeclareerd attribuut; zonder referentie-`type` is dat een gewone lezing (gemeten PASS). | — |
| `wf034-sentinel-builtin-wratoperand` | WF-034 (sentinelhek) | B13 (+V6) | **herschreven (m), armwissel** | Rolnaam `vondsten` als operand van een builtin. Gemeten zonder sentinel: WF-034 `'vondsten' is not a declared attribute of 'Jutter'` — armwissel naar de naamresolutie. Bewaakt in v1 de grens van de rolnaamuitzondering. | D9/D18 |
| `wf034-sentinel-gelest-vouwoperand` | WF-034 (sentinelhek) | B13 | **verwijderd-met-reden** | Als `-builtin-operand`, op een lijstvouwoperand (gemeten PASS). | — |
| `wf034-sentinel-gelest-wratoperand` | WF-034 (sentinelhek) | B13 (+V6) | **herschreven (m), armwissel** | Als `-builtin-wratoperand`, op een lijstvouwoperand (gemeten met een vervangend type: WF-034 naamresolutie). | D9/D18 |
| `wf034-sentinel-geleste-setlezing` | WF-034 (sentinelhek) | B13, B11 | **herschreven (op WF-053)** | Sentinel en `set` beide weg. Gemeten: wat overblijft is WF-053 (to-many-stap in een scalaire lijstvouwoperand), gelijk aan de opvolger van `wf096-setlezing-geleste-operand`. | D17 |
| `wf034-sentinel-kale-lezing` | WF-034 (sentinelhek) | B13 | **verwijderd-met-reden** | Sentinel op een kale attribuutlezing; gemeten PASS. | — |
| `wf034-sentinel-lijstelement-payload` | WF-034 (d) | B16 | **verwijderd-met-reden** | Sentinel in een payload van het `type` van een `Construct` (een positie die blijft). Onschrijfbaar zodra `Onbekend` de datatypesom verlaat. | S4 |
| `wf034-sentinel-null-vouwoperand` | WF-034 (sentinelhek) | B13 | **verwijderd-met-reden** | `type: null` op een referentie is in v1 de normale vorm (gemeten PASS). | — |
| `wf034-sentinel-payload-onder-lijst` | WF-034 (d) | B16 | **verwijderd-met-reden** | Als `-lijstelement-payload`. | S4 |
| `wf034-sentinel-structveld-payload` | WF-034 (d) | B16 | **verwijderd-met-reden** | Als `-lijstelement-payload`. | S4 |
| `wf034-sentinel-sumtype-payload` | WF-034 (d) | B16 | **verwijderd-met-reden** | Als `-lijstelement-payload`. | S4 |
| `wf093-aantalgevulde-wrat` | WF-093 | B13, B16 (+V6) | **herschreven (i)** | Kale spelling (rolnaam `vondsten`, `set: false`) onder AANTAL_GEVULDE. v1: dezelfde bron zonder `type`; WF-093 moet op de rolnaam sleutelen in plaats van op het sentinel. Gemeten zonder die hersleuteling: WF-034 (plus de v0-artefacten WF-037 en WF-047). | S2 |
| `wf093-wratvouw-kale-eerste` | WF-093 | B13, B16 (+V6) | **herschreven (i)** | Als `wf093-aantalgevulde-wrat`, onder EERSTE. Gemeten zonder hersleuteling: WF-034. | S2 |
| `wf093-wratvouw-kale-som` | WF-093 | B13, B16 (+V6) | **herschreven (i)** | Als `wf093-aantalgevulde-wrat`, onder SOM. Gemeten zonder hersleuteling: WF-034. | S2 |
| `wf093-wratvouw-set-maximum` | WF-093 | B13, B11 | **verwijderd-met-reden** | Set-spelling: zonder sentinel en zonder `set` is de bron de gewone waardevouwbron (MAXIMUM van `gewicht` over de vondsten), een welgevormde vouw. In de simulatie resten alleen v0-velden (`navigatieRol` onder WF-083). | — |
| `wf093-wratvouw-set-som` | WF-093 | B13, B11 | **verwijderd-met-reden** | Als `wf093-wratvouw-set-maximum`, onder SOM. | — |
| `wf034-feitcreatie-hoofdletterveld` | WF-034 (m) | B7 | **verwijderd-met-reden** | `zetVeld` verlaat de wire; de hoofdlettergevoeligheid van een bonenproperty heeft geen v1-drager. | — |
| `wf034-feitcreatie-loos-tussenstapveld` | WF-034 (m) | B7 | **verwijderd-met-reden** | `Tussenstap.veld` vervalt. | — |
| `wf034-feitcreatie-onbekend-joinveld` | WF-034 (m) | B7 | **verwijderd-met-reden** | `joinVeld` vervalt; de symbolische laatste stap (`path`) kruist WF-028. | — |
| `wf034-feitcreatie-onbekend-tussenstaptype` | WF-034 (m) | B7 | **verwijderd-met-reden** | `Tussenstap.type` vervalt. | — |
| `wf034-feitcreatie-onbekend-tussenstapveld` | WF-034 (m) | B7 | **verwijderd-met-reden** | `Tussenstap.veld` vervalt. | — |
| `wf034-feitcreatie-onbekend-zetveld` | WF-034 (m) | B7 | **verwijderd-met-reden** | `zetVeld` vervalt. | — |
| `wf034-feitcreatie-zusterrol` | WF-034 (m) | B7 | **verwijderd-met-reden** | De opslagzijde van het bonenmodel is geen v1-begrip. | — |
| `wf028-bean-only-creation` | WF-028 | B7 | **verwijderd-met-reden** | De bonenveld-alleen-arm vervalt (B7); een creatie zonder symbolische velden laadt in v1 niet. | S3 |
| `wf028-create-along-final-hop` | WF-028 | B7 | **herschreven (m)** | De clausule 'de gecreëerde relatie verschilt van die van de laatste stap' is symbolisch en blijft (IR-SPEC ch. 3.4: 'in v1 the ill-formed variants are verifier errors'). v1: `factType` gelijk aan het feittype van de laatste `path`-stap. | — |
| `wf028-half-symbolic-creation` | WF-028 | B7 | **verwijderd-met-reden** | `doelRol` en `joinRol` ontbreken als sleutel. In v1 zijn `targetRole` en `path` de enige vorm en schemaverplicht: laadfout. | S3 |
| `wf053-bindingless-ambiguous` | WF-053 | B11 | **verwijderd-met-reden** | De geschreven-naamarm (`post` meerduidig) vervalt. Gemeten zonder geschreven naam: beide regels vallen op de typenpaarafleiding met twee kandidaten (`Wachter`/`Post`, `Post`/`Kustvak`). | D10 |
| `wf053-chain-near-side-mismatch` | WF-053 | B11 | **herschreven (m)** | Bindingen in verkeerde volgorde; in v1 `path` [kustligging/kustvak, postbezetting/post] vanaf `Wachter`: stap 0 past niet. De twee geschreven-stapmeldingen vervallen (afgeleid; de v0-verifier telt stappen uit de geschreven velden en kan dit niet simuleren). | — |
| `wf053-dangling-navigation-binding` | WF-053 | B11 | **herschreven (m)** | Een `path`-stap noemt het niet-gedeclareerde feittype `bemanning`. | — |
| `wf053-geleste-vouwoperand-onverbonden-hop` | WF-053 | B11 (+B12) | **herschreven (i)** | `Baken.bakenloon` vanaf `Vondst` zonder verbindend feittype (typenpaar, nul kandidaten). Eist v1 een pad, dan valt hij op die arm (zelfde regel). Draagt een lijstvouw met `elementObjecttype` zonder binding (B12-telling). | D10 |
| `wf053-to-many-hop-in-scalar` | WF-053 | B11 | **herschreven (i)** | De geschreven meervoudsnaam `posten` vervalt. Gemeten: als typenpaar en met binding `postbezetting/post` valt hij op de to-many-stap in een scalaire positie. Blijft de WF-053-getuige van de positie 'hele regelwaarde' (zie `wf096-setlezing-regelwaarde`). | D10 |
| `wf053-written-role-mismatch` | WF-053 | B11 | **verwijderd-met-reden** | Geschreven stap tegen gebonden rol; v1 heeft geen geschreven stap. Gemeten met gelijke naam: PASS. | — |
| `wf053-zelfverwijzing-vreemde-rol` | WF-053 | B11 | **verwijderd-met-reden** | De nul-stap-zelfverwijzing bestaat alleen als geschreven stap; zonder naam is het een eigen lezing (gemeten PASS). | — |
| `wf054-binding-on-unscoped` | WF-054 | B8 | **verwijderd-met-reden** | In v1 is de aanwezigheid van `scope` de scoping; een binding op een ongescoopte uniciteit is onschrijfbaar. Gemeten als gescoopte uniciteit: PASS. | — |
| `wf054-dangling-scope-binding` | WF-054 | B8 | **herschreven (m)** | `scope` noemt het niet-gedeclareerde feittype `wijkindeling`. | — |
| `wf054-to-many-scope-role` | WF-054 | B8 | **herschreven (i)** | Nu bindingloos (`scopeVeld: "kavel"`, geen `scopeRol`). v1 eist de binding: `scope` met de to-many-rol `kavel` uitschrijven. | — |
| `wf096-aantal-setbron` | WF-096 | B11 (+V6) | **vervangen (WF-093 v1)** | Zonder `set` is een telling over een elementattribuut alleen fout als de v1-vouwsoortregel het verbiedt (telling alleen over een instantiebron). Nieuwe getuige: `count` met bron `AttributeReference{attribute: "gewicht"}`. Zonder die regel is het document schoon (afgeleid). | S2 |
| `wf096-aantal-setbron-bronpad` | WF-096 | B11 (+V6) | **verwijderd-met-reden** | In v1 dezelfde overtreding als `wf096-aantal-setbron`; de padlengte leest de opvolgerregel niet. | S2 |
| `wf096-aantal-setbron-wrat` | WF-096 | B11, B13 | **verwijderd-met-reden** | Het onderscheidende kenmerk (sentinel én `set`) heeft geen v1-spelling; de vertaling is een telling met rolnaambron op het elementtype, een duplicaat. | S2 |
| `wf096-setlezing-geleste-operand` | WF-096 | B11 | **vervangen (WF-053)** | 'De positieregel' (V14): gemeten zonder `set` valt de lezing op WF-053 (to-many-stap in een scalaire positie), hier een lijstvouwoperand. Nieuwe naam bijv. `wf053-to-many-lijstvouwoperand`. | — |
| `wf096-setlezing-operand` | WF-096 | B11 | **vervangen (WF-053)** | Als `wf096-setlezing-geleste-operand`, linkeroperand van een `PLUS` (gemeten). | — |
| `wf096-setlezing-regelwaarde` | WF-096 | B11 | **herschreven (op WF-053)** | Gemeten zonder `set`: WF-053 to-many in de hele regelwaarde — de positie die `wf053-to-many-hop-in-scalar` al pint. | D17 |
| `wf034-rolcheck-loze-andertefiller` | WF-034 (o) | B9 | **verwijderd-met-reden** | `anderTeFiller` vervalt; de v1-knoop noemt `factType` en `role`. Een v1-knoop zonder die velden laadt niet. | S3 |
| `wf034-rolcheck-onbekend-andertefiller` | WF-034 (o) | B9 | **vervangen (WF-034, v1-rolarm)** | De filler-afleiding vervalt. Opvolger: `RoleMembership` waarvan `factType` nergens resolveert. Vereist dat L2 WF-034 een v1-arm voor `RoleMembership` geeft (feittype resolveert, rol hoort erbij, het onderwerp vult de rol). | — |
| `wf034-verre-rolcheck-verkeerde-zijde` | WF-034 (t) | B9 | **herschreven (i)** | Dezelfde fout in de samengevoegde vorm: `RoleMembership{factType: "wachtindeling", role: "wachtjutter"}` op onderwerp `Wachtpost` — de rol van de andere zijde. Dit is het rood-eerst-bewijs dat V4 (b) vraagt. | — |
| `wf048-ambiguous-connecting-relation` | WF-048 | B12 | **verwijderd-met-reden** | WF-048 vervalt; de verdeling draagt verplicht haar binding, meerduidigheid is onschrijfbaar. | S3 |
| `wf048-telling-meerduidig` | WF-048 | B12 | **verwijderd-met-reden** | Als `wf048-ambiguous-connecting-relation`, voor de telling. De bijvangst WF-034 (typenpaarterugval) vervalt mee. | S3 |
| `wf003-unresolved-domain` | WF-003 | B16 | **herschreven (i)** | v1-vorm van een onvindbaar domein: de declaratie noemt het niet-gedeclareerde domein, bijv. `Enumeration{domain: "Bedragen"}`. Gemeten: WF-003 `enumeration references undeclared domain 'Bedragen'`. De regel beter op gelijkheid laten vergelijken (anders bijvangst WF-036). | S4 |
| `wf034-ambigu-typenpaar` | WF-034 | B12 (L2) | **verwijderd-met-reden** | Typenpaarterugval bij een bindingloze telling (twee kandidaten); B12 maakt de binding verplicht, de terugval bestaat niet meer. De onresolveerbare geschreven naam `duinkavels` blijft alleen een overtreding als v1 de geschreven rolnaam naast de binding houdt. | S2 |
| `wf034-attribuutref-zonder-attribuut` | WF-034 | — | **herschreven (m)** | `attribute: null`. | S3 |
| `wf034-attribuutref-zonder-objecttype` | WF-034 | — | **herschreven (m)** | `subjectType: null`. | S3 |
| `wf034-dangling-parameter` | WF-034 | — | **herschreven (m)** | Verwijzing naar niet-gedeclareerde parameter `geen`. | — |
| `wf034-dimensiecel-geen-referentie` | WF-034 | B13 (L2) | **herschreven (m)** | De cel verliest haar eigen `type`; de attribuutpositie (een `Literal`) blijft fout. | — |
| `wf034-dimensiecel-zonder-attribuut` | WF-034 | B13 (L2) | **herschreven (m)** | `attribute: null`. | S3 |
| `wf034-foldbron-onbekend-attribuut` | WF-034 | B12, B11 (L2) | **herschreven (i)** | Bindingloze vouw: in v1 een binding toevoegen; de onbekende attribuutnaam `bestaatniet` blijft. | — |
| `wf034-foldbron-zonder-attribuut` | WF-034 | B13 (L2) | **herschreven (m)** | `attribute: null`; de bijvangst WF-091 (SOM-afleiding) verdwijnt. | S3 |
| `wf034-foldbron-zonder-objecttype` | WF-034 | B13 (L2) | **herschreven (m)** | `subjectType: null`; de bijvangst WF-091 verdwijnt. | S3 |
| `wf034-kenmerkcheck-zonder-kenmerk` | WF-034 | — | **herschreven (m)** | `characteristic: null`. | S3 |
| `wf034-kwantorvouw-filter-onderwerpkenmerk` | WF-034 | — | **herschreven (m)** | Gebonden (`bronRol`); niets van L2–L5 raakt de overtreding. | — |
| `wf034-neerwaartse-lezing` | WF-034 | B11 (L2) | **herschreven (m)** | Lezing via een supertype-rol; `set` vervalt, de resolutie blijft. | — |
| `wf034-objectcreatie-loos-subject` | WF-034 | — | **herschreven (m)** | `subject: null`. | S3 |
| `wf034-objectcreatie-onbekend-subject` | WF-034 | — | **herschreven (m)** | Onderwerp `Bestaatniet`. | — |
| `wf034-onbekend-regeldoel` | WF-034 | B13 (L2) | **verwijderd-met-reden** | Sentinel op het `type` van het regeldoel; het doel verliest zijn `type` (gemeten PASS). | — |
| `wf034-parameterref-zonder-parameter` | WF-034 | — | **herschreven (m)** | `parameter: null`. | S3 |
| `wf034-regeldoel-zonder-type` | WF-034 | B13 (L2) | **verwijderd-met-reden** | Een doel zonder `type` is de v1-vorm (gemeten PASS). | — |
| `wf034-verdeling-lege-bronattribuut` | WF-034 | — | **herschreven (m)** | Lege string; v1 `Allocation` houdt de veldnamen (hernoemd in L5). | — |
| `wf034-verdeling-lege-groepeerattribuut` | WF-034 | — | **herschreven (m)** | Lege string. | — |
| `wf034-verdeling-lege-maximumattribuut` | WF-034 | — | **herschreven (m)** | Lege string. | — |
| `wf034-verdeling-onbekend-bronattribuut` | WF-034 | — | **herschreven (m)** | — | — |
| `wf034-verdeling-onbekend-doelattribuut` | WF-034 | — | **herschreven (m)** | — | — |
| `wf034-verdeling-onbekend-groepeerattribuut` | WF-034 | — | **herschreven (m)** | — | — |
| `wf034-verdeling-onbekend-maximumattribuut` | WF-034 | — | **herschreven (m)** | — | — |
| `wf034-verdeling-onbekend-ratoattribuut` | WF-034 | — | **herschreven (m)** | — | — |
| `wf034-verdeling-onbekend-restantattribuut` | WF-034 | — | **herschreven (m)** | — | — |
| `wf034-verdeling-zonder-bronattribuut` | WF-034 | — | **herschreven (m)** | `sourceAttribute: null`. | S3 |
| `wf034-verdeling-zonder-brontype` | WF-034 | — | **herschreven (m)** | `sourceType: null` (v1 `Allocation` houdt `sourceType`, IR-SPEC ch. 3.2). | S3 |
| `wf034-verdeling-zonder-doelattribuut` | WF-034 | — | **herschreven (m)** | `targetAttribute: null`. | S3 |
| `wf034-verdeling-zonder-doeltype` | WF-034 | — | **herschreven (m)** | `targetType: null`. | S3 |
| `wf034-zelftype-typenpaar` | WF-034 | B12 (L2) | **verwijderd-met-reden** | Als `wf034-ambigu-typenpaar`, zelfgetypeerd paar (twee kandidaatrollen). | S2 |

Verwijderd-met-reden vraagt per getuige de meting dat `m2.py` erop faalt (bij
de vervallen velden: het veld heeft geen L2-drager) of dat het onderwerp na
migratie weg is (verifier PASS op de vertaling); de klasse van de melding
staat in de README-lijst, zoals bij M1.

## Acceptatie

Elke bouwer sluit met de genoemde gates groen op zijn eigen branch; de kaart
sluit met alle veertien groen op de samengevoegde branch (staat die op de
main-tip, dan is dat de main-run), en:

- (a) `python3 spec/proposals/v1-migratie/m2.py --check` meldt 0 L1-documenten
  in de vier zones en de pariteitszone; idempotent; de afleidingstellingen
  per zone in het Result.
- (b) Gate 2: metaschema-valide; alle golden- en rood-documenten valideren en
  round-trippen; zelftestrijen erbij voor: `RoleMembership` zonder
  `factType`; `AttribuutRef` met `set`, met `navigatieRol`, met `type`;
  `ParameterRef` met `type`; `Functie` `SOM` met `elementObjecttype` zonder
  binding (weigering), `TOTAAL` zonder binding (aanvaard), met beide
  bindingen (weigering); `Kwantorvouw` zonder binding; `ResolvedVerdeling`
  zonder `verbindendeRol`; `InstanceSetBranch.sourceRole: null`;
  `ResolvedFeitCreatie` met `zetVeld`, en zonder `joinRol`; `Tussenstap`
  zonder `rolBinding`; `ResolvedUniciteit` met `scopeVeld`; `Kenmerk` met
  `navigatie`; `Onbekend` als datatype; `Onopgelost` zonder `type`
  (aanvaard) en met `type: null` (weigering). `EXPECTED_MIN_DOCS` 192
  blijft.
- (c) Gate 3: alle golden- en rood-documenten verifier-schoon; elke negatief
  afgewezen op zijn regel EN, waar `expect.txt` staat, met het fragment
  (U1); de stand gemeld; `EXPECTED_MIN_NEGATIEF` 350 blijft tenzij de stand
  eronder komt (D20); `EXPECTED_MIN_EXPECT` op de gemeten stand.
- (d) Gate 4 ongewijzigd (het casusformaat verandert niet; D56).
- (e) Gates 5, 6, 7, 8: GEEN uitkomst beweegt (L2 verandert geen betekenis);
  ref-suite en clingo-suite herschreven (U9) met hun tellingen; de
  noop-survivorlijst ongewijzigd; rood: elk fragment dat `set`, `Onbekend` of
  een geschreven naam noemt opnieuw gepind op de nieuwe meldtekst (per regel
  in het Result; DV: 4 fragmenten noemen `set`), de 36 bodemzinnen opnieuw
  vastgesteld waar hun tekst verandert; `EXPECTED_MIN_ROOD_CASES` 26 blijft.
- (f) Gates 9, 10, 14: alle emittergoldens byte-identiek na her-emissie (12
  catala; 71 rgs plus die van de dialectkaarten; 2 flogic) — en byte-identiek
  aan de mechanische migratie van diezelfde documenten (een verschil is een
  bevinding over script of emitter); vloeren 201/12, 173/38, 81/1 blijven;
  JUnit-tellingen gemeld.
- (g) Gate 11: casus-dump opnieuw geëmitteerd en byte-identiek aan de
  mechanische migratie behalve de ene knoop van U3 (gemeld); 29 slices via
  `m2.py`; replay 29/29/29; suite ≥ 27; `toka/README.md` krijgt onder
  "Deliberate divergences" de L2-regel (de zone staat in de L2-vorm; welke
  velden verdwenen; de afgeleide bindingen in de dump: 2 vouwen, 3
  navigaties, 1 rollidmaatschap; de U3-knoop); holdouts ongewijzigd tenzij
  gemeten.
- (h) Gate 12: `EXPECTED_PLAYED`/`EXPECTED_DRL_PLAYED` opnieuw gemeten
  (vandaag 185/164, plus wat de kaarten vóór de rebase toevoegen); elke
  kanaalwissel bij naam met reden; JUnit zonder skips, `EXPECTED_MIN_TESTS`
  239 gemotiveerd als hij beweegt; de U6-greptelling 0; laderweigerings-
  getuigen voor elk vervallen veld en voor `Onbekend`.
- (i) Gate 13: twisttabel zonder de vier referentieklassen (`KLASSE`, de
  scriptkop, de klem); `EXPECTED_MIN_TWISTS` naar de gemeten stand in
  dezelfde commit (schatting DV §2.3: ≈ 38 op 21 goldens);
  `EXPECTED_MIN_DRL_PLAYED` opnieuw gemeten; alle legs op hun eigen
  ongetwiste regel; de gatebeschrijving in `CLAUDE.md` doet de hoofdsessie bij
  de landing.
- (j) `python3 spec/proposals/v1-naamtabel-toets.py` rc 0: de bij L2 gelande
  namen als `identiteit` met `geland: "M2"` of `"M3"` (of een `geland M2`-
  noot; de toets kent M1..M8) — de vervallen properties (`onderwerpBron`,
  `navigatieRol`, `navigatiePad`, `set`, `type` op de vijf
  referentieklassen, `zetVeld`, `joinVeld`, `veld`, `viaOuderVeld`,
  `type@Tussenstap`, `scopeVeld`, `anderTeFiller`, `filler`, `backref`,
  `navigatie`, `reden`), de knopen `Onbekend`, `Rollidmaatschap`,
  `VerreRollidmaatschap`, en `RoleMembership` met `factType`, `role`,
  `negated` erbij; `geland_telling` bijgewerkt.
- (k) Herpin in `regelspraak-java` (hoofdsessie, na de landing): `SPEC_REV`
  naar de L2-kop; `rm -rf vendor/uchar-frontend/frontends` + `git archive`;
  `make clean`; `make uchar-conformance`; `make test` ≥ 553 (verwacht:
  `IrEmitterTest.feitcreatie_symbolisch_blok_is_heel_of_niet` kantelt op
  `viaOuderVeld`, TC A.5; de records bewegen niet, D6); `UCHAR_DIR=… mvn -o
  -B test`.

May touch (per bouwer disjunct, zie werkverdeling): `spec/schema/**`,
`spec/proposals/v1-migratie/**`, `spec/proposals/v1-naamtabel.json` (+ toets),
`spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md` (alleen de genoemde plekken),
`spec/verifier/**`, `corpus/**`, `toka/**`, `ref/**`, `backends/clingo/**`,
`backends/drools/**`, `frontends/**`, en van `scripts/`: `check-ir-schema.sh`
(zelftestrijen), `check-verifier.sh` (U1), `check-reftype-derivation.sh`
(D21), `check-drools.sh` (pins, kanaallijsten), `check-rood.sh` (fragmenten,
bodemzinnen), en `check-oracle.sh`/`check-clingo.sh`/`check-flogic.sh`/
`check-regelspraak.sh` ALLEEN een vloer die gemeten onder de stand komt —
elke gate-scriptwijziging per regel in het Result. NIET: `spec/CONSTRUCTS.md`,
`spec/PLAN.md`, `CLAUDE.md`, `WAY-OF-WORKING.md`, `.github/**`,
`spec/proposals/V1-WIREMIGRATIE.md` (behalve de ene L2-regel in de
landingstabel, A2).

Out of scope: B14–B28, B6, B10, B17, B26 (L3–L5); de enumwaardespellingen; de
`rules`-sleutels; de hergroepering `tussenstappen` + `joinRol` → `path` en de
namen `FactCreation`/`Uniqueness`/`AttributeReference`/`Aggregation`
(L4/L5); N1–N3; de verzamelingsverwijzing (V6 (c), latere kaart); een
canonicalisatiepas (D56); alles wat betekenis wijzigt. Een bouwer die een
betekenisverschil meet (een golden die ná L2 anders uitkomt; een golden die
van gate-12-kanaal wisselt om een andere reden dan zijn nieuwe binding)
STOPT op dat punt, legt de meting in het Result en gaat verder met de rest —
dat is de nulmeting die de landingen moeten leveren.

## Werkverdeling

Vier bouwers. Kaartbranch `card/l2-referentielaag` vanaf main NA de landing
van `anker-nestkenmerk-dragervoorwaarde`; A1 eerst op de kaartbranch
(worktree `wt/l2`); daarna A2, B en C parallel op eigen branches vanaf A1's
kop (`card/l2-a2`, `card/l2-b`, `card/l2-c` in `wt/l2-a2`, `wt/l2-b`,
`wt/l2-c`); C start pas als beide dialectkaarten geland zijn (zelfde
frontendbestanden) en rebaset A1's migratie over hun nieuwe goldens. De
hoofdsessie voegt serieel samen (ff/rebase), draait de veertien gates in
batches en zet één lens op de unie (checklist + kaart); herkeuring strikt
op de gemelde punten; fixronden door dezelfde bouwers.

- **A1 — de wire.** `spec/schema/uchar-ir-v1.schema.json` (+ README: de
  conditionele bindingen van U2 gemotiveerd), `scripts/check-ir-schema.sh`
  (zelftestrijen (b)), `spec/proposals/v1-migratie/m2.py` (U4), alle
  documenten gemigreerd (corpus + `toka/cases/**/ir.json` +
  `toka/casus/ir.json` mechanisch — C vervangt de dump daarna door de
  her-emissie en meet U3), `spec/proposals/v1-naamtabel.json` + toets (j).
  Sluit met gate 2 en 4 groen; gate 3 en de rest zijn dan rood en dat is
  verwacht. Rapporteert per zone de aantallen, de afleidingstellingen, en elk
  document waarop het script faalde met de reden.
- **A2 — verifier en spec.** `spec/verifier/**` (loader/ast/checks: U5; de
  regels van "Regels die veranderen"; de v1-rolarm; WF-093 tweezijdig),
  `scripts/check-verifier.sh` (U1), `spec/IR-SPEC.md` en
  `spec/RUNTIME-SEMANTICS.md` ch. 2.10/4.1 (de genoemde plekken; Engels), de
  137 stempels plus de gesignaleerde kandidaten (verwijderen; herschrijven
  met `expect.txt`; de 24 verhuizingen; de 4 vervangingen met U8-namen),
  `corpus/negatief/README.md` sectie "L2" naar het M1-model (kolom "arm"),
  de bindingloze negatief-documenten waarop het script faalde,
  `wf031-dagsoort-leest-rolstaat` met de hand, de ene L2-regel in de
  landingstabel van `V1-WIREMIGRATIE.md`. Sluit met gate 3 groen (en 2, 4).
- **B — de OCaml-kant.** `ref/lib/{eval,case}.ml`, `ref/test/test_ref.ml`;
  `backends/clingo/lib/{model,normalise,translate}.ml`,
  `backends/clingo/test/test_clingo.ml`; `frontends/catala/lib/{emit,
  translate}.ml`; `frontends/flogic/lib/{emit,lower}.ml`,
  `frontends/flogic/test/suite.ml`; `scripts/check-rood.sh` (fragmenten en
  bodemzinnen, (e)); `scripts/check-reftype-derivation.sh` (D21; de run zelf
  pas op de unie). Sluit met gates 5, 6, 7, 8, 9, 14 groen.
- **C — de Java-kant.** `frontends/regelspraak` (`IrEmitter` op de L2-vorm;
  `ExpressieBouwer`: de 14 `onbekend(`-routes typeren of luid weigeren
  (D14), de zelfverwijzing als eigen lezing; `RoleMembership` uit `Conditie`;
  `IrEmitterTest` en de 8 testbestanden met ingebedde documenten),
  `backends/drools` (`IrLoader` strikt op de L2-vorm met weigering bij naam
  van elk vervallen veld en van `Onbekend`; `ContractValidator`;
  `FactModelGenerator` en beide transpilers op de bindingen, U6;
  `CaseRunner`; de 27 getuigentests mechanisch met `m2.py` over de
  tekstblokken; U7), `toka/casus/ir.json` opnieuw geëmitteerd (U3),
  `toka/README.md`, `toka/src` waar hij een vervallen veld leest, alle
  rgs-goldens opnieuw geëmitteerd en byte-identiek aan A1's migratie
  gemeten, `scripts/check-drools.sh` (pins en kanaallijsten, (h)). Sluit met
  gates 10, 12 en de obligaties 1–3 van gate 11 groen; obligatie 4 en gate
  13 op de unie.

Elke bouwer: commit klein en vaak met de branch altijd bouwbaar; houdt zijn
context zuinig (geen hele corpusbestanden lezen; meten met python; de
stempeltabel hierboven is zijn werklijst); maakt GEEN kaartjes in
`backlog/new` (restpunten als genummerd open punt in het Result); pusht en
merget nooit; raakt CONSTRUCTS/PLAN/CLAUDE.md nooit; loopt vóór zijn Result
de lens-checklist na en noemt per punt wat hij mat; eindigt met een
Result-blok (stand, gemeten aantallen, gate-uitvoer, gate-scriptregels, open
punten) — ook op zijn contextgrens, zodat een vervolgbouwer verder kan.

## Registerwerk bij de landing (hoofdsessie)

`spec/CONSTRUCTS.md`: rij 1 (de typeannotatie — "a copy that cannot be
serialized cannot drift" — geland L2 op de vijf referentieklassen; gate 13
herzien), rij 4 (het vouwbereik: WF-048 vervallen, binding verplicht, eigen
lezing onder `TOTAAL`), rij 5 (`RoleMembership` geland; topniveaubeperking
weg), de rijen voor `AttribuutRef`/navigatie (B11 geland: één expliciet
pad), `ResolvedFeitCreatie`/`Tussenstap` (3.4: bonenvelden weg, de afleiding
in de backend — de "forcing move"), `ResolvedUniciteit` (3.6: `scopeVeld`
weg), `Kenmerk` (`navigatie` weg), `Onbekend` (2.4: geland), `VariabeleRef`
(rij 24: zonder `type`), WF-096/WF-093 waar het register ze noemt.
`spec/PLAN.md` §9 (11): L2 geland met datum en hash.
`spec/proposals/V1-WIREMIGRATIE.md`: de L2-rij (A2 zet hem). `CLAUDE.md`:
gate 13 «reference and composite positions» → de afgeleide posities die na
B13 een annotatie houden, met datum; gate 3: de fragmenttoets (U1). Daarna
de herpin van de toolchain (k) en de memory.

## Correcties op de kaarttekst (A1, gemeten)

Gemeten op de kaartbranch na de rebase op main `cf1090a5` (760 documenten:
golden 238, negatief 444, rood 48, toka 30).

- (g): de "3 navigaties" in de casus-dump zijn geen afgeleide bindingen maar
  drie geschreven navigaties die eigen lezingen worden (twee
  nul-hop-zelfverwijzingen en één telbron die het subject aanduidt). De dump
  krijgt 2 vouwbereiken, 1 `RoleMembership`, de U3-knoop en geen
  navigatiebinding.
- "L2 raakt 705 documenten": gemeten zijn 685 documenten geraakt, 653
  gemigreerd (golden 227, rood 45, negatief 358, toka 23) plus 32 geweigerd
  (allemaal negatief). 75 documenten zonder referentielaag blijven
  byte-gelijk (golden 11, rood 3, negatief 54, toka 7).
- "3010 referentietypes": gemeten 2866 verwijderd in de gemigreerde
  documenten.
- "141 `Onbekend`-knopen": gemeten 137, waarvan 130 op een referentietype, 6
  in declaraties of payloads van negatief-documenten en 1 via U3. Er
  verdwijnen er 124 (123 met het referentietype, 1 via U3); de overige 13
  staan in geweigerde documenten.
- "de 6 met een bindingloze verdeling": gemeten 7 bindingloze verdelingen in
  6 documenten. `wf050-allocation-beside-defaults` krijgt zijn rol afgeleid;
  `wf020-crossform-collision`, `wf024-complex`, `wf025-max-without-ratio` en
  `wf026-two-allocations-one-target` (twee verdelingen) hebben
  `bronType: null` en weigeren; `wf048-ambiguous-connecting-relation` is
  gestempeld.
- U11: 6 telbronnamen herschreven in 5 documenten: golden
  `jutterswet-kaveltelling` en `jutterswet-muntpeiling` (1 elk), toka 4
  plekken in 3 documenten (`toka/cases/scenario-toka-19-hoeveelheid-passagiers`,
  `toka/cases/scenario-toka-28-vastgestelde-contingent`, en `toka/casus` met
  2). Buiten U11 blijven de twee attribuutbronnen onder `AANTAL`
  (`wf096-aantal-setbron`, `wf096-aantal-setbron-bronpad`): hun naam is een
  attribuut van het elementtype en geen telbron; dat oordeel is aan WF-093.
