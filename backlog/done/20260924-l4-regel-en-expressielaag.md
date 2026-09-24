# L4 — de regel- en expressielaag (M5 + M6: B19, B20, B21, B27, B22 a/b/c, B23; plus N3): de vierde klapkaart van v1

Vierde kaart van fase 2 van `spec/proposals/V1-WIREMIGRATIE.md` (§7, de
landingstabel: L4 = M5 + M6), na L1 (`backlog/done/20260923-m1-schema-en-container.md`),
L2 (`backlog/done/20260923-l2-referentielaag.md`) en L3
(`backlog/done/20260924-l3-typesysteem.md`, geland `4439d802`, Result
`cd256d00`; deze kaart bouwt op main `d0dab83d`). L4 zet de
REGELLAAG en de EXPRESSIELAAG van de wire om en niets anders:

- **B19** — de zes benoemde vormen worden `{node, name, versions}`: de
  `" #i"`-afvlakking van v0 keert om, elke versie draagt haar eigen
  `validity` (altijd geschreven), en `bronAnnotaties` wordt
  `statuteAnnotations` op de vorm.
- **B20** — `soort` valt uiteen: `initialisation` (boolean, altijd
  geschreven) en `presentation` (alleen bij `GELIJKSTELLING` en
  `BEREKENING`, v0-spelling); de kenmerktoekenning is een regel waarvan het
  doel een kenmerk is — dat volgt uit de declaratie, niet uit een veld.
- **B21** — `Functie` splitst in `Aggregation` (de acht vouwsoorten) en
  `BuiltinCall` (de zeventien overige), elk met alleen de eigen velden.
- **B27** — `ResolvedUniciteit` en `ResolvedUniciteitUnie` worden één
  `Uniqueness` met een lijst `sources` (één bron = enkelvoudig); de lijst
  `uniciteitUnies` verdwijnt.
- **B22 a/b/c** — `Literal.waarde` (een lexeem) wordt `Literal.value`, een
  getypeerde waarde in de varianten van het casusschema; een decimale komma
  wordt een punt, de juridische spelling blijft in `bron` (B22a en B22b
  landden al als P5 en P4; L4 voert B22c uit).
- **B23** — `Constante` splitst: `de dag` wordt `BoundDay`, `Rekendatum` en
  `Rekenjaar` worden `ContextValue`.
- **N3** — een regeldoel waarvan de naam op het subjecttype zowel attribuut
  als kenmerk is, wordt ill-formed (nieuwe arm onder WF-022).

Wat NIET meegaat, met de reden:

- **B28** blijft een regellaagvorm: `ResolvedStartpunt` blijft byte-voor-byte
  L3, want IR-SPEC 3.8 beschrijft de registratie als eigen vorm en WF-032
  staat per (objecttype, tijdlijn) een bepaling toe die één `anchor`-veld op
  de tijdlijn niet kan dragen; L5 hernoemt haar (D40).
- **B24** gaat niet mee: er ligt geen kaart die de taxonomie van de
  `Onopgelost`-reden vastlegt, dus de reden landt later additief en
  `Onopgelost` blijft in L4 byte-voor-byte L3 (D38).
- **B25** gaat niet mee: er is geen golden met volledige ref ≡ clingo ≡
  Drools-pariteit op de semantiek van `Regelversie` (0 dragers), dus de
  v1-knoopvorm wacht op die golden; alleen de IR-SPEC-zin over "is gevuurd"
  wordt naar V13 herschreven (D39).

Byte-voor-byte L3 blijven: de hele modellaag (behalve de expressies in de
lichamen van `model.functionDefs`, die de expressiedelta's volgen);
`ResolvedDagsoort`, `ResolvedStartpunt`, `ResolvedRegelGroep`; de sleutels
van de defaultslaag (`prioritizedDefaults`, `Default`, `ExceptionGroup` —
de expressies die daarin staan volgen wel B21/B22/B23); de kindknopen
`Geldigheid`, `RolBinding`, `Toekenning`, `DimensieSelectie`; alle
expressie- en conditieknopen behalve `Functie`, `Literal` en `Constante`
(ook `Onopgelost` en `Regelversie`); alle enumwaarden; `bron`, `line`,
`diagnostics` en `format`; de sleutels onder `rules` behalve dat
`uniciteitUnies` verdwijnt.

Goal: één ff-landing waarna alle veertien gates groen zijn op de L4-vorm,
alle documenten in de vier corpuszones en de pariteitszone in die vorm staan
(stand main `cd256d00`, L3 geland, gemeten 2026-09-24: golden 242 (anchor 40, overig
202), negatief 407, rood 47, toka 30 = 726; elk document verandert, want elk
draagt de sleutel `uniciteitUnies`), de drie emitters hem schrijven, de vier
consumenten hem lezen, en de toka-zone meeverhuist (gate 11 in
regressievorm; README-divergentieregel L4). Gates 5, 6, 7, 8 en 12 mogen
geen uitkomst bewegen: L4 verandert geen betekenis, en elke afwijking is een
nulmeting.

## Besluiten die deze kaart uitvoert (niet heropenen)

Uit `research/v1-klap/BESLUITENREGISTER-L2-L5.md` §3 (D26–D40), de
kruisende besluiten van §1 en de normvraag N3. Eén regel per besluit, als
opdracht; bij twijfel wint de registertekst, die per besluit zijn grond
noemt.

Kruisend (zie de L2-kaart voor de volledige tekst):

- **D1** Herstructureren = IR-SPEC-naam voor knoop en eigen sleutels. L4
  hernoemt precies: de zes benoemde vormen (`Rule`, `Allocation`,
  `ObjectCreation`, `FactCreation`, `Consistency`, `Uniqueness`) met hun
  versierecords, `Source`, `Aggregation`, `BuiltinCall`, `Literal` (de
  sleutel `waarde` → `value`), `ContextValue` en `BoundDay`. Kindknopen die
  niet geherstructureerd worden blijven v0 (`Geldigheid`, `RolBinding`,
  `Toekenning`, `DimensieSelectie`).
- **D2** `bron` blijft `bron` (string of `null`) op elke knoop tot L5, ook op
  de nieuwe knopen.
- **D3** Elke knoop draagt `node`, behalve de versierecords van de zes
  benoemde vormen en de getypeerde literaalwaarde (`Source` draagt `node`).
- **D4** Enumwaarden houden hun v0-spelling (`method: "NAAR_RATO"`,
  `kind: "SOM"`, `presentation: "BEREKENING"`, `granularity: "MAAND"`).
- **D5** (a) een veld dat IR-SPEC optioneel noemt: v0-`null` → afwezig;
  (b) een veld waarvan een WF-regel de aanwezigheid bewaakt blijft nullable
  — de naam van een benoemde vorm (WF-107), referentienamen, de
  allocatievelden, het onderwerp van een objectcreatie, `Literal.type`; onder
  hetzelfde criterium ook het onderwerp van een consistentie (WF-029,
  getuige `wf029-no-subject`); (d) elke v0-waarde die de L4-vorm niet kan dragen laat het
  script luid falen met bestand en JSON-pointer.
- **D6** De Java-records van `frontends/regelspraak` en de OCaml-AST houden
  hun v0-vorm; alleen de wirelaag (schema, laders, emitters) en de logica die
  verdwenen wire-informatie las, veranderen.
- **D16** `COMPLEX` is een laadfout (`method` is een gesloten enum van drie
  waarden); WF-024 vervalt uit IR-SPEC en verifier; `wf024-complex` wordt
  verwijderd; de casus-dump draagt de waarde twee keer en krijgt in L4 zijn
  regel in de divergentielijst.
- **D20** `EXPECTED_MIN_NEGATIEF` blijft 350; de stand wordt gemeld.
- **D21** Gate 13 volgt: het script sleutelt op `Functie`/`kind` en moet in
  L4 op `Aggregation`/`BuiltinCall`; `EXPECTED_MIN_TWISTS` blijft tenzij de
  gemeten stand eronder komt.

Voor L4:

- **D26** Alle sleutels onder `rules` blijven v0; de samengevoegde
  uniciteiten staan onder `uniciteiten` (eerst de v0-enkelvoudige, dan de
  v0-unies, elk in v0-volgorde); `uniciteitUnies` vervalt; de lijstnamen
  komen in L5.
- **D27** Een benoemde vorm is `{node, name, versions}` met versierecords
  zonder `node`, in de sleutelvolgorde van de IR-SPEC-tabellen; v0-invoeren
  met gelijke naam na het strippen van `" #i"` worden versies in de volgorde
  van `i`; `statuteAnnotations` staat op de vorm, overgenomen uit de eerste
  versie, en het script faalt luid als de versies verschillen.
- **D28** `validity` staat op elk versierecord; v0-`null` migreert naar
  `{"node": "Geldigheid", "vanaf": null, "tot": null}`; de 26
  consistentiegetuigen met `geldigheid: null` gaan mee.
- **D29** `presentation` is optioneel en staat er alleen bij
  `GELIJKSTELLING` en `BEREKENING` (v0-spelling tot L5); de tabel van
  IR-SPEC 3.1 wordt `presentation?`.
- **D30** Het doel blijft een attribuutreferentie; of het lid een attribuut
  of een kenmerk is volgt uit de declaratie; de vorm `{kind, name}` komt er
  niet.
- **D31** `FactCreation` verliest `doel` (het doeltype is de vuller van
  `targetRole`) en de omhullende `Tussenstap`: een stap van `path` is een
  `RolBinding`.
- **D32** Op de objectcreatie heet het veld `connectingRole`; de tabel van
  IR-SPEC 3.3 wordt gecorrigeerd.
- **D33** `groepeerAttribuut` wordt `orderingAttribute`; de landing toetst
  tegen `RUNTIME-SEMANTICS.md` ch. 4 dat het veld de sorteersleutel is, en
  stopt met de meting als dat niet zo is.
- **D34** `bron` en `line` staan op het versierecord van `Consistency`
  (`bron`, `line`) en `Uniqueness` (`bron`), onder hun v0-naam tot L5.
- **D35** `Aggregation` = `{node, kind, arguments, elementType?, filter?,
  emptyFallback?, dimensionSelection?, sourceRole?, sourcePath?, type, bron}`;
  `BuiltinCall` = `{node, kind, arguments, filter?, granularity?, type,
  bron}`; `granularity` alleen op een builtin, de aggregatievelden niet op een
  builtin; de drie negatief-documenten met een facet op de verkeerde knoop
  worden onschrijfbaar en verwijderd.
- **D36** `Literal.value` neemt de `value`-varianten van `uchar-case-v0`
  letterlijk over (`number`, `boolean`, `text`, `date`, `dateTime`,
  `enumeration`, `constructor`); migratie en RegelSpraak-emitter zetten `,`
  om in `.`; `bron` houdt de juridische spelling; WF-081, WF-084 en WF-095
  blijven verifierregels over de string in de variant; een nieuwe regel
  kruist variant tegen `type`; nieuwe patronen in het schema vragen
  zelftestrijen in gate 2.
- **D37** `de dag` wordt `BoundDay {node, type, bron}`, eerst opgenomen in
  IR-SPEC 4.1; `Rekendatum` en `Rekenjaar` worden `ContextValue {node, name,
  type, bron}` met hun v0-spelling tot L5; `ContextValue.name` blijft in het
  schema een vrije string, zodat WF-098 niet-toegelaten namen blijft
  weigeren; beide WF-098-getuigen worden herschreven.
- **D38** B24 gaat niet mee; `Onopgelost` blijft in L4 ongemoeid.
- **D39** B25 gaat niet mee; de zin «"is gevuurd" is a v1 non-goal» in
  IR-SPEC 4.5 wordt naar V13 herschreven (beide statussen blijven; de
  knoopvorm wacht op de paritygolden).
- **D40** B28: de startpuntregistratie blijft een regellaagvorm; L5 hernoemt
  haar.
- **N3** (normvraag, standaardantwoord JA geldt): een regeldoel waarvan de
  naam op het subjecttype zowel een attribuut als een kenmerk aanduidt, is
  ill-formed — een nieuwe arm onder WF-022, met een negatief-getuige met
  `expect.txt`. De arm raakt 0 documenten (gemeten over alle zones, toka
  inbegrepen: geen regeldoel draagt zo'n naam; in de corpuszones declareert
  alleen `anchor/jutterswet-toelagegronden` er een, `Jutter.toelage`, en in
  de toka-zone één objecttype in 30 documenten, zonder regel die hem
  schrijft).
- **Naamprincipe voor de defaultslaag** (standaardantwoord van de
  hoofdsessie): geen L4-namen op `prioritizedDefaults`, `Default` en
  `ExceptionGroup`.

## Uitvoeringsbesluiten van de hoofdsessie (U1–U27)

Genomen op 2026-09-24 uit de beslispunten van het snijverslag, met dezelfde
nummering (het verslag zelf blijft buiten de repo; de meting die elk besluit
draagt staat hier in één zin). Waar de kaarttekst "Un" zegt, is dit de plek.

- **U1 — de twee `COMPLEX`-verdelingen in de casus-dump.** `IrEmitter`
  schrijft een verdeling die de frontend niet kan dragen niet, en zet in
  `diagnostics` een WARNING die haar naam noemt (WF-113 (a): het verslag van
  wat onopgelost bleef); C meet de opnieuw geëmitteerde dump als
  byte-identiek aan m4.py's migratie van een meetkopie van de L3-dump zonder
  die twee verdelingen (`/rules/verdelingen/3` en `/5` in de L3-dump) plus de
  twee diagnostics; `toka/README.md` krijgt de divergentieregel L4 met die
  twee pointers. De slices onder `toka/cases/` dragen geen `COMPLEX`
  (gemeten) en migreren mechanisch (A1). Niet gekozen: het hele model
  weigeren (verliest de fragmenten) of `COMPLEX` schrijfbaar houden
  (strijdig met D16).
- **U2 — versiegroepering.** m4.py en de emitter groeperen alleen invoeren
  van één lijst waarvan de naam op `" #<i>"` eindigt en na strippen bytegelijk
  is; de `i` van een groep zijn precies 1..n, de invoeren staan
  aaneengesloten, een groep telt minstens twee versies en hun
  `bronAnnotaties` zijn gelijk — anders luid falen; een invoer zonder
  achtervoegsel is een vorm met één versie, ook naast een tweede invoer met
  dezelfde kale naam (WF-020 vuurt dan zoals nu). Gemeten: 2 groepen van 2
  (`anchor/jutterswet-jutheffing`, `negatief/wf021-version-overlap`), geen
  afwijker. Geen rgs-golden en geen toka-document draagt twee versies: C
  schrijft een `IrEmitterTest` voor de groepering.
- **U3 — een vormnaam op `" #<cijfers>"` is in L4 onschrijfbaar**:
  schemapatroon op `name` plus laderweigering met dezelfde zin in alle vier
  de lezers (de in-memory records spellen versies zo). 0 dragers.
- **U4 — `RuleVersion.target` is in het schema `$ref AttribuutRef`**, en de
  laders weigeren een andere knoop op die plek (1041 van 1041 v0-doelen zijn
  een `AttribuutRef`; `doel: null` en `waarde: null` 0).
- **U5 — sleutelvolgorde** waar IR-SPEC geen volledige tabel geeft: `node`
  eerst, dan de IR-SPEC-tabelvolgorde, de v0-velden van D34 achteraan —
  precies de volgorde in "De L4-vorm van de wire".
- **U6 — lege lijsten en de gescoopte unie**: `versions`, `sources`, `keys`
  en `path` krijgen `minItems: 1` in schema en laders; een
  `UniquenessVersion` met twee of meer bronnen draagt geen `scope`
  (schemavoorwaarde en laderweigering; 0 gescoopte unies en 0 lege lijsten
  gemeten).
- **U7 — `BuiltinCall.granularity`** houdt de vijf v0-waarden (geen
  verscherping buiten het register; gemeten alleen `MAAND` en `JAAR`).
- **U8 — de literaalwaarde**: de patronen van `number`, `date` en
  `dateTime` uit `uchar-case-v0` letterlijk; `constructor` zonder `payload`;
  `value` verplicht met precies één variant en een niet-lege string of een
  boolean.
- **U9 — `ContextValue.name` is een verplichte string en `type` een
  verplicht datatype** op beide contextknopen; het open punt «`naam: null`»
  van WF-098 wordt geschiedenis (0 dragers).
- **U10 — de regelsoort als structurele afbeelding** in elke lader en
  emitter (checklistpunt 16), zonder het model te raadplegen;
  `initialisation: true` met `presentation` is een schemavoorwaarde en een
  laderweigering. Gemeten exact op het hele corpus (590/208/4/239).
- **U11 — de ledensoortarm onder WF-022**: de verifier kruist de afbeelding
  tegen de declaratie (een versie zonder `presentation` en zonder
  `initialisation` MOET een kenmerk schrijven; een versie met `presentation`
  of `initialisation: true` MOET een attribuut schrijven); zonder de arm zet
  ref voor een Booleaan-attribuut zonder tag stil een kenmerk. Twee getuigen
  (`wf022-kenmerkdoel-met-presentatie`,
  `wf022-attribuutdoel-zonder-presentatie`); 0 corpusdocumenten geraakt. De
  Drools-lader (die het model heeft) weigert met dezelfde zinnen; de
  gedeelde OCaml-lader raadpleegt het model niet en zwijgt, zoals bij WF-009
  in L3.
- **U12 — wirepointers in het geheugen**: elk record van de zes vormen
  krijgt één `string`-veld met de wirepointer van zijn versie
  (`/rules/<lijst>/<f>/versions/<v>`), gevuld door de lader; elke melding van
  verifier, ref en clingo die nu `"/resolvedRegels/%d"` uit een in-memory
  index bouwt, leest dat veld; pointers volgen de L4-wire (ook `value`,
  `condition`, `target`, `arguments`), de lijstsleutels blijven v0 (D26).
  302 negatief-getuigen melden op zo'n pointer; 203 negatief- en 7
  golden-NOTE's citeren er een en gaan mee (A2); 0 `expect.txt` draagt een
  pointer.
- **U13 — de literaal in het geheugen**: het `Literal`-record houdt het
  v0-lexeem (`waar`/`onwaar`; het getal met punt) en krijgt een gesloten
  variant-type met zeven constructoren (één per casusschema-variant) erbij;
  WF-114 vergelijkt variant en type; de gedeelde OCaml-lader en de
  Drools-lader weigeren een variant die niet bij het type past met de kernzin
  van WF-114 (zonder regelnummer), zodat geen uitvoerder `{"text": "5"}` bij
  een `Number` stil als getal leest.
- **U14 — de gebonden dag in het geheugen**: een eigen expressieconstructor
  voor `BoundDay` met `type` en `bron` (de compiler vindt elke lezer die nu
  `Constante "de dag"` matcht: ref, clingo, WF-031, WF-098); `Constante`
  blijft voor de contextwaarde met naam. De Java-records blijven (D6): de
  Drools-lader maakt van `BoundDay` `Constante("de dag")` en weigert een
  `ContextValue` met de naam `de dag`.
- **U15 — een contextwaarde buiten de twee namen** krijgt in ref, clingo en
  de Drools-lader een benoemde arm met de kernzin van WF-098 (geen bodemzin
  meer; gate 8); `Rekenjaar` blijft toegelaten-en-onuitgevoerd (ref faalt
  erop bij naam).
- **U16 — de doelsoort van een feitcreatie in het geheugen** is de
  gedeclareerde vuller van `targetRole` in `factType` (de laders vullen
  `doel`); lost dat niet op, dan laat de verifier-lader het leeg (WF-028 en
  WF-034 melden) en weigert de Drools-lader; m4.py eist gelijkheid met het
  v0-`doel` (25 van 25 gemeten).
- **U17 — één lijst `uniciteiten` op de wire, twee lijsten in het
  geheugen**: de laders splitsen op het aantal bronnen; de emitters schrijven
  eerst de enkelvoudige, dan de unies (D26); een andere volgorde laadt gewoon
  (de volgorde draagt geen betekenis; geen weigering).
- **U18 — signatuur vóór de bouw**: A2's EERSTE commit op `card/l4-a2` is de
  AST- en laderwijziging van U12, U13 en U14, met een gebouwde verifier; A2
  sluit die stap af met een Result en de hash; de hoofdsessie start B vanaf
  die commit (branch `card/l4-b` vanaf A2's eerste commit, niet vanaf A1's
  kop); C start vanaf A1's kop.
- **U19 — WF-081 versmalt tot het `number`-patroon van het casusschema**:
  geen komma, geen `.5` zonder leidend cijfer, een noemer zonder voorloopnul;
  migratie en emitter schrijven een punt; de lexeemlezers van ref, clingo en
  Drools weigeren een komma (één grammatica voor één variant); getuige
  `wf081-decimale-komma`. 0 dragers na migratie.
- **U20 — WF-081, WF-084 en WF-095 sleutelen op de variant**, niet op het
  type; WF-114 is de enige kruising van variant en type.
- **U21 — WF-114** krijgt het vrije nummer 114 (WF-113 is het hoogste in
  IR-SPEC en verifier), de zin uit "Regels die veranderen" en twee getuigen;
  de `constructor`-variant heeft 0 golden-dragers (alleen
  `negatief/wf036-gelijkheid-enumeratie-tegen-somtype`), de indexrij noemt
  dat; geen nieuwe golden.
- **U22 — de N3-getuige** `wf022-doel-attribuut-en-kenmerk`, gebroken uit
  `anchor/jutterswet-toelagegronden` (daar is `Jutter.toelage` attribuut én
  kenmerk, zonder regel met dat doel) door een regel met doel `toelage` toe
  te voegen; de golden van de arm is diezelfde anchor; de RegelSpraak-emitter
  weigert zo'n doel luid (U25).
- **U23 — nieuwe getuige `wf098-de-dag-als-contextwaarde`**: `ContextValue
  {name: "de dag"}` valt op WF-098.
- **U24 — de rood-fragmenten "the Rekendatum constant"** worden "the
  Rekendatum context value" in ref en clingo, en beide fragmenten van
  `corpus/rood/tijdsduur-reads-van-before-tot/case1.json` worden opnieuw
  gepind, per regel in het Result.
- **U25 — weigeringen aan de producentkant**: `IrEmitter` weigert luid een
  in-memory literaal zonder type, een `Constante`-naam buiten `de dag`,
  `Rekendatum` en `Rekenjaar`, en een regeldoel met een dubbele naam (N3);
  de `COMPLEX`-verdeling volgt U1.
- **U26 — namen**: deze kaart `backlog/doing/20260924-l4-regel-en-expressielaag.md`;
  branch `card/l4-regel-en-expressielaag`, worktree `wt/l4`; bouwers
  `card/l4-a2`, `card/l4-b`, `card/l4-c`; `spec/proposals/v1-migratie/m4.py`;
  naamtabel `geland: "M5"` (B19, B20, B21, B27) en `"M6"` (B22, B23).
- **U27 — registerfouten uit het snijden**: (a) de dubbele "L3
  landed"-alinea in PLAN §9 is recht (`d0dab83d`); (b) de WF-082-indexrij
  noemt `jutterswet-hardheidsoverschot` (A2); (c)
  `wf030-single-source-union` heeft geen `NOTE.md` — hij wordt verwijderd en
  de README-reden noemt de meting (A2).

Nog te meten door de bouwers: per getuige de arm (A2); welke
verifiermeldingen in tekst bewegen (A2; gemeten zijn 13 met een
v0-veldnaam); of de lexeemlezers van ref, clingo en Drools één plek delen
(B en C).

## Lens-checklist — vooraf, per bouwer (de twaalf punten van L3, plus vijf van L4)

Elke bouwer loopt deze lijst vóór zijn Result na en noemt per punt wat hij
mat; de lens toetst op de unie strikt deze lijst plus de kaart, in één ronde.

1. **Elke nieuwe of veranderde wirepositie door alle vier de lezers gelezen
   én gebruikt** (verifier-lader; de gedeelde OCaml-lader van ref en clingo;
   de Drools-lader; catala, flogic en `IrEmitter` als schrijvers):
   `Rule.name`/`versions`/`statuteAnnotations`; `RuleVersion.initialisation`
   en `presentation` (samen de reconstructie van de v0-`soort`, zie punt 16);
   `validity` op elke versie; de hernoemde velden van de vijf andere
   versierecords (`method`, `orderingAttribute`, `connectingRole`,
   `createdType`, `initialisations`, `assertion`, `scope`, …);
   `FactCreation.path` en de uit `targetRole` gereconstrueerde doelsoort;
   `Uniqueness.sources` en `Source`; `Aggregation` en `BuiltinCall` met hun
   velden; `Literal.value` in elke variant; `ContextValue` en `BoundDay`.
   Niet "geladen en genegeerd".
2. **Onbekende en vervallen velden bij naam geweigerd in elke lader**: de
   knopen `ResolvedRegel`, `ResolvedVerdeling`, `ResolvedObjectCreatie`,
   `ResolvedFeitCreatie`, `Tussenstap`, `ResolvedConsistentie`,
   `ResolvedUniciteit`, `ResolvedUniciteitUnie`, `Bron` (de unietak),
   `Functie`, `Constante`; de lijst `uniciteitUnies`; op de L4-knopen en
   hun versierecords de v0-sleutels `naam`,
   `soort`, `doel`, `waarde`, `voorwaarde`, `geldigheid`, `bronAnnotaties`,
   `bronType`, `bronAttribuut`, `doelType`, `doelAttribuut`, `methode`,
   `ratoAttribuut`, `maximumAttribuut`, `afrondingDecimalen`,
   `restantAttribuut`, `groepeerAttribuut`, `afnemend`, `verbindendeRol`,
   `nieuwType`, `toekenningen`, `anker`, `tussenstappen`, `feittype`,
   `ankerRol`, `doelRol`, `joinRol`, `assertie`, `sleutels`, `scopeRol`,
   `bronnen`, `argumenten`, `elementObjecttype`, `leegwaarde`,
   `dimensieSelectie`, `bronRol`, `bronPad`, `granulariteit`, en `subject`
   op een uniciteitsversie; verder `COMPLEX` als methode, een
   aggregatieveld op een `BuiltinCall`, `granularity` op een `Aggregation`,
   een `kind` die niet bij de knoop hoort, een `Literal.value` die een string
   is, twee sleutels draagt of een `payload` heeft, een lege `versions`,
   `sources`, `keys` of `path`, een unie (≥ 2 bronnen) met `scope`, en een
   vormnaam op `" #<cijfers>"`. Een onbekende enumwaarde (`method`,
   `presentation`, `kind`, `granularity`) krijgt in elke lader de zin van de
   OCaml-lader (`unknown <veld> value "<v>"`; L3-vondst B4).
3. **Geen v0-namen in meldingen, commentaren, NOTE-citaten en javadoc** voor
   wat L4 herstructureert (geen `Functie`, `leegwaarde`, `soort`,
   `KENMERKTOEKENNING` als wireveld, `ResolvedUniciteitUnie`, "the Rekendatum
   constant" in een melding over de L4-wire), geen vervallen wirevorm die in
   IR-SPEC of RUNTIME-SEMANTICS als geldend beschreven blijft (L3-vondst B5),
   en geen D-/U-nummers in IR-SPEC, RUNTIME-SEMANTICS, schema of
   golden-NOTE's.
4. **Tests die een verboden vorm pinnen herschrijven, niet verwijderen**
   (tenzij de vorm onschrijfbaar is: dan een telling in het Result):
   `IrEmitterTest`, de Drools-getuigentests met ingebedde documenten, de
   frontendtests met ingebedde documenten, `test_ref.ml`, `test_clingo.ml`,
   de catala- en flogic-suites.
5. **Sleutelvolgorde = schema-`properties`-volgorde** voor de nieuwe knopen
   en de zes versierecords; L3-volgorde voor de rest; m4.py en alle drie de
   emitters schrijven exact die volgorde (gate 2 round-trip; gates 9/10/11/14
   byte-identiek).
6. **Elke lezer dezelfde defaults**: `condition` afwezig = onvoorwaardelijk;
   `validity` bestaat altijd; `presentation` afwezig = geen oppervlaktetag;
   `elementType`, `emptyFallback`, `dimensionSelection`, `filter`,
   `granularity` afwezig = geen; de optionele allocatievelden afwezig = geen;
   `connectingRole` afwezig op een objectcreatie = de afgeleide terugkoppeling
   (RUNTIME-SEMANTICS ch. 5.5); `scope` afwezig = ongescoopt; een vorm met één
   versie heet in het geheugen zonder achtervoegsel.
7. **Per getuige de verwachte arm in `expect.txt`** (U1 van L2) — elke
   herschreven getuige met een L4-delta op zijn defect en elke nieuwe; per
   verwijderde getuige de meting dat m4.py erop faalt of dat de verifier de
   vertaling aanvaardt.
8. **Vloeren gemotiveerd**: `EXPECTED_MIN_NEGATIEF` 350 blijft;
   `EXPECTED_MIN_EXPECT` naar de gemeten stand; de andere vloeren alleen
   omlaag bij een gemeten stand eronder, in dezelfde commit, met de reden.
9. **Gate-12-pins hermeten** (verwacht 188/167, geen kanaalwissel); elke
   afwijking is een nulmeting en stopt de bouwer op dat punt.
10. **Gate 13 op de L4-namen**: de twister sleutelt op
    `Aggregation`/`BuiltinCall` en `kind` (de afgeleide soorten, de
    `TIJDSDUUR`-uitzondering), op `arguments`, en laat de literaalwaarde
    (`value`) buiten de twist zoals het lexeem nu; `EXPECTED_MIN_TWISTS` 38
    blijft.
11. **Geen stille terugval**: geen lezer leidt een regelsoort af uit het
    model of uit een ontbrekend veld buiten de afbeelding van punt 16; geen
    lezer leest een variant die niet bij het type hoort; geen lezer leidt
    de doelsoort van een feitcreatie anders af dan uit de gedeclareerde vuller
    van `targetRole`; geen lezer beeldt een `ContextValue` met de naam
    `de dag` af op de gebonden dag.
12. **Diagnostics, `bron` en `line` ongemoeid** (WF-113 (b) zoekt de `bron`
    van elk `Onopgelost` letterlijk; D2, D34).
13. **Komma → punt met behoud van `bron`**: de 17 kommaliteralen dragen in
    `value.number` een punt en hun `bron` byte-gelijk; drie ervan (in
    `meldloon`, `vangstklem`, `wrakrecht`) komen ook uit de emitter.
14. **Versiegroepering luid**: m4.py en de emitter weigeren ongelijke
    `bronAnnotaties` binnen een groep en achtervoegsels die niet 1..n zijn;
    de lezers vouwen een vorm met n ≥ 2 versies uit tot `naam #1..#n` en een
    vorm met één versie tot de kale naam; elke melding noemt de wirepointer
    `/rules/<lijst>/<f>/versions/<v>/…`, nooit een in-memory index.
15. **Builtin- en aggregatiefacetten op de juiste knoop**: `granularity`
    alleen op `BuiltinCall`, de vijf aggregatievelden alleen op
    `Aggregation`, `kind` in de verzameling van de knoop — in schema, m4.py,
    alle laders en alle emitters.
16. **De regelsoort als afbeelding**: `initialisation: false` + `presentation`
    ↔ `GELIJKSTELLING`/`BEREKENING`; `initialisation: true` zonder
    `presentation` ↔ `INITIALISATIE`; `initialisation: false` zonder
    `presentation` ↔ `KENMERKTOEKENNING`; de combinatie `initialisation: true`
    met `presentation` bestaat niet (schema en laders); de verifier kruist de
    afbeelding tegen de ledensoort van het doel (zie "Regels die
    veranderen"; U10, U11).
17. **De verifier aanvaardt de eigen emissie van elke frontend** (L3-vondst
    B1): C en B leveren voor elke nieuwe emittervorm (versiegroep,
    `Aggregation`/`BuiltinCall`, elke literaalvariant, `BoundDay`,
    `ContextValue`, samengevoegde uniciteit, `path`) een proefdocument, en
    de verifier en de vier lezers laden het schoon.

## De L4-vorm van de wire (normatief voor deze kaart)

Canonieke sleutelvolgorde: voor de nieuwe knopen en versierecords de
`properties`-volgorde van het schema, die de volgorde hieronder is (de
IR-SPEC-tabellen, met `node` vooraan en de v0-velden van D34 achteraan);
voor elke andere knoop de L3-volgorde. Serialisatie ongewijzigd (gate 2's
serialiser). Alles wat hieronder niet genoemd wordt is byte-voor-byte L3.

**De `rules`-container (D26):**

```
"rules": {
  "resolvedRegels":      [ Rule ],               // v0-sleutel tot L5
  "resolvedDagsoorten":  [ ResolvedDagsoort ],   // ongewijzigd
  "objectCreaties":      [ ObjectCreation ],
  "verdelingen":         [ Allocation ],
  "consistenties":       [ Consistency ],
  "uniciteiten":         [ Uniqueness ],         // v0 uniciteiten, daarna v0 uniciteitUnies, elk in v0-volgorde
  "feitCreaties":        [ FactCreation ],
  "resolvedStartpunten": [ ResolvedStartpunt ],  // ongewijzigd (D40)
  "regelGroepen":        [ ResolvedRegelGroep ], // ongewijzigd
  "prioritizedDefaults"?: [ … ]                   // sleutels ongewijzigd; de expressies erin volgen B21/B22/B23
}                                                 // "uniciteitUnies" vervalt
```

**De benoemde vormen (B19, D27, D3).** Elke vorm is

```
{ "node": <Vorm>, "name": string|null, "versions": [ <versierecord zonder node>, … ] }   // versions minItems 1
```

met `name` = de v0-`naam` zonder achtervoegsel (nullable, WF-107, D5 (b)).
Groepering: invoeren van één v0-lijst waarvan de naam op `" #<i>"` eindigt
(`i` ≥ 1) en die na het strippen gelijk zijn (bytegelijk), worden één vorm
met de versies in de volgorde van `i`; een invoer zonder achtervoegsel is een
vorm met één versie; de vorm staat op de plaats van zijn eerste versie. Het
script faalt luid als de `i` van een groep niet precies 1..n zijn, als een
groep uit één versie bestaat, of als de versies van een regel verschillende
`bronAnnotaties` dragen (gemeten: 0 van elk). Een vormnaam die zelf op
`" #<cijfers>"` eindigt is in L4 onschrijfbaar (U3: de in-memory
records spellen versies zo).

```
Rule            { "node": "Rule", "name", "versions": [ RuleVersion ], "statuteAnnotations"?: [ string, … ] }
RuleVersion     { "validity": Geldigheid, "target": AttribuutRef, "value": expr,   // target alleen AttribuutRef (U4)
                  "condition"?: conditie, "initialisation": boolean,
                  "presentation"?: "GELIJKSTELLING"|"BEREKENING" }       // niet naast initialisation: true (U10)
Allocation      { "node": "Allocation", "name", "versions": [ AllocationVersion ] }
AllocationVersion { "sourceType": string|null, "sourceAttribute": string|null,
                  "targetType": string|null, "targetAttribute": string|null,
                  "method": "GELIJKE_DELEN"|"NAAR_RATO"|"OP_VOLGORDE",
                  "ratioAttribute"?: string, "maximumAttribute"?: string,
                  "roundingDecimals"?: integer, "remainderAttribute"?: string,
                  "orderingAttribute"?: string, "descending": boolean,
                  "condition"?: conditie, "validity": Geldigheid, "connectingRole": RolBinding }
ObjectCreation  { "node": "ObjectCreation", "name", "versions": [ ObjectCreationVersion ] }
ObjectCreationVersion { "createdType": string|null, "subject": string|null,
                  "initialisations": [ Toekenning ], "condition"?: conditie,
                  "validity": Geldigheid, "connectingRole"?: RolBinding }
FactCreation    { "node": "FactCreation", "name", "versions": [ FactCreationVersion ] }
FactCreationVersion { "factType": string, "anchorRole": string, "targetRole": string,
                  "anchor": string|null, "path": [ RolBinding ],          // minItems 1
                  "condition"?: conditie, "validity": Geldigheid }
Consistency     { "node": "Consistency", "name", "versions": [ ConsistencyVersion ] }
ConsistencyVersion { "subject": string|null, "assertion": conditie,
                  "validity": Geldigheid, "bron": string|null, "line": integer }
Uniqueness      { "node": "Uniqueness", "name", "versions": [ UniquenessVersion ] }
UniquenessVersion { "sources": [ Source ], "scope"?: RolBinding,         // sources minItems 1; scope alleen bij één bron (U6)
                  "validity": Geldigheid, "bron": string|null }
Source          { "node": "Source", "objectType": string|null, "keys": [ string|null, … ] }  // keys minItems 1
```

Migratie per v0-vorm (alle velden die hier niet staan gaan verbatim mee,
onder de nieuwe naam uit de tabel):

| v0 | L4 | regel |
|---|---|---|
| `ResolvedRegel.naam` | `Rule.name` | achtervoegsel gestript; groepering hierboven |
| `ResolvedRegel.soort` | `initialisation` + `presentation?` | `GELIJKSTELLING`/`BEREKENING` → `initialisation: false` + `presentation` met die waarde; `INITIALISATIE` → `initialisation: true`, geen `presentation`; `KENMERKTOEKENNING` → `initialisation: false`, geen `presentation` (het doel is een kenmerk, D30); `soort: null` → luid falen |
| `doel`, `waarde` | `target`, `value` | een `null`, of een doel dat geen `AttribuutRef` is, → luid falen (IR-SPEC: verplicht; U4) |
| `voorwaarde` (alle vormen) | `condition?` | `null` → afwezig |
| `geldigheid` (alle vormen) | `validity` | `null` → `{"node": "Geldigheid", "vanaf": null, "tot": null}` (D28) |
| `bronAnnotaties` | `Rule.statuteAnnotations` | van de eerste versie; verbatim, ook een ill-formed lijst (de drie WF-082-getuigen) |
| `bronType`, `bronAttribuut`, `doelType`, `doelAttribuut` | `sourceType`, `sourceAttribute`, `targetType`, `targetAttribute` | nullable (D5 (b)) |
| `methode` | `method` | `COMPLEX` of `null` → luid falen (D16) |
| `ratoAttribuut`, `maximumAttribuut`, `afrondingDecimalen`, `restantAttribuut`, `groepeerAttribuut` | `ratioAttribute`, `maximumAttribute`, `roundingDecimals`, `remainderAttribute`, `orderingAttribute` | `null` → afwezig; een lege string blijft (WF-034-getuigen) |
| `afnemend` | `descending` | verbatim |
| `verbindendeRol` (verdeling, objectcreatie) | `connectingRole` | verbatim (D32) |
| `nieuwType`, `toekenningen` | `createdType`, `initialisations` | `Toekenning` blijft v0 |
| `feittype`, `ankerRol`, `doelRol`, `anker` | `factType`, `anchorRole`, `targetRole`, `anchor` | verbatim |
| `tussenstappen` + `joinRol` | `path` | de `rolBinding` van elke `Tussenstap` in volgorde, daarna `joinRol` |
| `ResolvedFeitCreatie.doel` | — | vervalt (D31); het script eist dat het gelijk is aan de gedeclareerde vuller van `doelRol` in `feittype` en faalt anders luid (gemeten: 25 van 25 gelijk) |
| `assertie` | `assertion` | `null` → luid falen |
| `ResolvedConsistentie.bron`, `.line`; `ResolvedUniciteit(Unie).bron` | `bron`, `line` op het versierecord | verbatim (D34) |
| `ResolvedUniciteit {subject, sleutels, scopeRol?}` | één versie met `sources: [{objectType: subject, keys: sleutels}]`, `scope` ← `scopeRol` | |
| `ResolvedUniciteitUnie {bronnen}` | één versie met `sources` ← elke `Bron {type, sleutels}` als `{objectType: type, keys: sleutels}` | een lege `sleutels` of `bronnen` → luid falen |

**Aggregatie en builtin (B21, D35).** `kind` beslist de knoop: `SOM`,
`TOTAAL`, `AANTAL`, `MINIMUM`, `MAXIMUM`, `EERSTE`, `LAATSTE`,
`AANTAL_GEVULDE` → `Aggregation`; de zeventien andere → `BuiltinCall`.

```
Aggregation { "node": "Aggregation", "kind": <acht, v0-spelling>, "arguments": [ expr ],
              "elementType"?: string, "filter"?: conditie, "emptyFallback"?: expr,
              "dimensionSelection"?: DimensieSelectie, "sourceRole"?: RolBinding,
              "sourcePath"?: [ RolBinding ], "type": datatype|null, "bron": string|null }
BuiltinCall { "node": "BuiltinCall", "kind": <zeventien, v0-spelling>, "arguments": [ expr ],
              "filter"?: conditie, "granularity"?: "DAG"|"WEEK"|"MAAND"|"KWARTAAL"|"JAAR",   // v0-waarden (U7)
              "type": datatype|null, "bron": string|null }
```

`argumenten` → `arguments`; `elementObjecttype` → `elementType`,
`leegwaarde` → `emptyFallback`, `dimensieSelectie` → `dimensionSelection`,
`bronRol` → `sourceRole`, `bronPad` → `sourcePath`, `granulariteit` →
`granularity`; `null` → afwezig. De schemavoorwaarde van L2 op de vouwbinding
verhuist mee: bij `elementType` aanwezig, precies één argument en `kind` ≠
`TOTAAL` precies één van `sourceRole`/`sourcePath`; nooit beide. Een builtin
die een van de vijf aggregatievelden draagt, of een aggregatie met
`granulariteit`, laat het script luid falen (gemeten: 3 documenten, D35).

**Literalen (B22, D36).** `Literal { "node": "Literal", "value": <variant
zonder node>, "type": datatype|null, "bron": string|null }`, met de
variant gekozen uit `type`:

| `type` | variant | lexeem (L3) → waarde (L4) |
|---|---|---|
| `Number` (met of zonder eenheid, `%` inbegrepen) | `{"number": string}` | elke `,` → `.`; verder verbatim |
| `Booleaan` | `{"boolean": true\|false}` | `waar` → `true`, `onwaar` → `false`; elk ander lexeem → luid falen |
| `Tekst` | `{"text": string}` | verbatim |
| `Date` met `day` | `{"date": string}` | verbatim |
| `Date` met `millisecond` | `{"dateTime": string}` | verbatim |
| `Enumeratie` | `{"enumeration": string}` | verbatim |
| `SumType` | `{"constructor": string}` | verbatim (zonder `payload`, U8) |
| `null`, of een ander datatype | — | luid falen (1 drager: `wf034-typeloos-literal`, met de hand) |

Een `waarde: null` laat het script luid falen (gemeten: 0 dragers). De
patronen van de varianten zijn die van `uchar-case-v0` (`number`, `date`,
`dateTime`); de negatief-getuigen van WF-081/084/095 dragen hun ill-formed
string in de variant (de negatief-zone valideert niet tegen het schema) en
de verifier weigert ze.

**Contextwaarden (B23, D37).**

```
BoundDay     { "node": "BoundDay", "type": datatype, "bron": string|null }                 // v0 Constante "de dag"
ContextValue { "node": "ContextValue", "name": string, "type": datatype, "bron": string|null }  // elke andere naam, verbatim
```

Het script beslist bytegelijk: `naam` = `de dag` → `BoundDay`; elke andere
string → `ContextValue` met die naam (zo dragen de twee WF-098-getuigen hun
naam `de dagX` en `rekendatum` mee); `naam: null` of `type: null` → luid
falen (gemeten: 0 dragers).

**Een v0-waarde die de L4-vorm niet kan dragen** laat `m4.py` LUID falen met
bestandsnaam en JSON-pointer; het script repareert niets. De verwachte
weigeringen zijn per constructie de verwijderde getuigen van de stempellijst,
`wf034-typeloos-literal` (met de hand herschreven) en `toka/casus/ir.json`
(2× `COMPLEX`, U1); elke andere weigering is een bevinding.

**Gemeten dragers** (main `cd256d00`; knopen a/g/r/n/t = totaal |
documenten a/g/r/n/t = totaal; a = anchor, g = golden zonder anchor):

| L4-knoop | v0-knopen | documenten |
|---|---|---|
| `Rule` (v0-invoeren; 1039 vormen na groepering) | 83/419/74/419/46 = 1041 | 32/181/40/303/21 = 577 |
| `Allocation` | 1/13/0/21/12 = 47 | 1/13/0/20/7 = 41 |
| `ObjectCreation` | 5/8/3/30/2 = 48 | 4/8/3/27/2 = 44 |
| `FactCreation` | 3/8/2/11/1 = 25 | 2/6/2/8/1 = 19 |
| `Consistency` | 3/4/0/32/2 = 41 | 2/3/0/30/2 = 37 |
| `Uniqueness` (15 enkelvoudig + 9 unie) | 8/4/7/5/0 = 24 | 3/4/5/5/0 = 17 |
| `Aggregation` | 19/101/15/112/10 = 257 | 10/60/10/86/6 = 172 |
| `BuiltinCall` | 8/25/14/26/17 = 90 | 4/10/8/19/5 = 46 |
| `Literal` | 72/460/57/507/89 = 1185 | 27/136/27/225/12 = 427 |
| `BoundDay` | 0/10/10/32/3 = 55 | 0/4/6/9/1 = 20 |
| `ContextValue` | 1/0/2/5/0 = 8 | 1/0/2/5/0 = 8 |

Verder gemeten: de achtervoegselgroepen zijn er twee, elk van twee versies
(`anchor/jutterswet-jutheffing`, `negatief/wf021-version-overlap`); `soort`:
`BEREKENING` 590, `GELIJKSTELLING` 208, `KENMERKTOEKENNING` 239,
`INITIALISATIE` 4 (a 1, n 1, t 2), `null` 0; `geldigheid: null` 97 (regel 71
in 17 documenten, consistentie 26 in negatief); `bronAnnotaties` 5 regels in
4 documenten (golden `jutterswet-hardheidsoverschot` 2, negatief 3 WF-082);
`voorwaarde: null` → afwezig op 696 regels, 23 objectcreaties, 46
verdelingen, 6 feitcreaties; `path` van lengte 1/2/3: 5/19/1; `methode`
`COMPLEX` 3 (negatief 1, `toka/casus` 2); literalen per variant: `number`
735 (waarvan 9 met `%`), `boolean` 247 (`waar` 245, `onwaar` 2), `date` 87,
`dateTime` 12, `enumeration` 23, `text` 79 (toka 48), `constructor` 1,
typeloos 1; komma's 17 in 12 documenten (golden 10 in 6: `ketenkwantor` 2,
`meldloon`, `sleepbaan`, `vangstklem`, `vervaltermijn`, `wrakrecht` 4 —
waarvan `meldloon`, `vangstklem`, `wrakrecht` een `bron.rgs` hebben;
negatief 7 in 6); een getallexeem zonder leidend cijfer of als breuk: 0;
builtins met een aggregatieveld 2 documenten, een aggregatie met
`granulariteit` 1; builtin-`granulariteit` alleen `MAAND` (20) en `JAAR` (12);
`Functie.type: null` 1 (`wf038-tijdsduur-zonder-node-type`, blijft `null`).
Elk van de 726 documenten verandert (elk draagt `uniciteitUnies`); 37 alleen
daarin (a 1, g 1, n 34, t 1).

## Regels die veranderen (IR-SPEC + verifier, bouwer A2)

A2 schrijft de spec-tekst ZELF (U10 (b) van L3). Elke nieuwe of veranderde
weigerzin staat letterlijk gelijk in verifier, ref, clingo en Drools
(U10 (e)); een zin hieronder tussen «» is een voorstel, A2 mag hem
aanscherpen maar zet dan dezelfde zin in alle vier.

**Vervalt geheel** (geschiedenisregel "until L4 …", zoals M1 dat voor WF-094
deed):

- **WF-024** (D16). IR-SPEC 3.2: de regel, de delta-alinea «what v0 marked
  COMPLEX is ill-formed in v1 (WF-024)» en de mirror-noot worden
  geschiedenis: sinds L4 is `method` een gesloten enum van drie waarden in
  schema en elke lader, en `COMPLEX` een laadfout. Verifierarm weg; de
  WF-indexrij wordt een geschiedenisrij; `wf024-complex` verwijderd.
- **WF-030, de arm «a uniqueness union carries at least two sources»**
  (B27): een uniciteit met één bron is de enkelvoudige vorm.
- **WF-037, de facetarmen die op de andere knoop vielen** (D35):
  `granularity` op een aggregatiesoort en `emptyFallback`/
  `dimensionSelection` op een builtin zijn onschrijfbaar. De armen binnen de
  knoop blijven (`emptyFallback` alleen bij `SOM`, `dimensionSelection`
  alleen bij `SOM`/`MINIMUM`/`MAXIMUM`, `granularity` alleen en verplicht
  bij `TIJDSEVENREDIG_DEEL`/`AANTAL_DAGEN_IN` — checks.ml r. 4000–4027).
- **WF-047, de bereikarm op een builtin** (D35): een `BuiltinCall` draagt
  geen bereik; de zin in IR-SPEC 4.3 die `wf047-bereik-op-builtin` noemt
  wordt geschiedenis.
- **WF-095, het open punt «`waarde: null` is the empty half»** en **WF-098,
  het open punt «`naam: null`»**: `value` is een verplichte variant met een
  string (of boolean), `ContextValue.name` een verplichte string; beide lege
  helften zijn sinds L4 onschrijfbaar (0 dragers, gemeten) — de open-punt-
  alinea's worden geschiedenis.

**Verandert:**

- **WF-020, WF-021, WF-107** lezen de gegroepeerde vorm (`name`,
  `versions[*].validity`); de in-memory uitvouwing (`naam #i`) houdt de
  checks zelf gelijk, maar elke melding noemt de wirepointer
  `/rules/<lijst>/<f>/versions/<v>/…`. De v0-mirrornoten over het
  achtervoegselschema (ch. 3 "Versioning", 3.1 Delta en Migration, WF-020's
  noot) worden "landed L4".
- **WF-022** krijgt de **N3-arm**: een versie waarvan het doel op het
  subjecttype (inclusief supertypen, WF-001-identiteit) zowel een attribuut
  als een kenmerk aanduidt, is ill-formed — voorstel
  «target '<naam>' names both an attribute and a characteristic of
  '<type>': which member a rule writes decides its kind, and this name
  decides nothing». De waardeclausule leest `{"boolean": true}`. Voorstel
  (U11): een **ledensoortarm** die de afbeelding van checklistpunt 16
  tegen de declaratie kruist («a version without `presentation` and without
  `initialisation` attributes a characteristic, so its target MUST name one»
  en omgekeerd).
- **WF-079**: de code leest via de bijectie dezelfde soort en verandert
  niet; de IR-SPEC-tekst leest "computing rule" als een versie met
  `initialisation: false` waarvan het doel een attribuut is, en
  "version-free" als een vorm met precies één versie met altijd-geldigheid,
  en noemt `GELIJKSTELLING`/`BEREKENING` als de `presentation`-tag, niet als
  soort. Hetzelfde geldt voor de soortlezingen in WF-023 en WF-050 en in
  `desugar.ml`.
- **WF-082** leest `statuteAnnotations` op de `Rule`; de melding noemt dat
  veld. Gemeten: de golden die de WF-index noemt (`jutterswet-hardheid`,
  anchor) draagt geen annotaties meer; de drager is
  `jutterswet-hardheidsoverschot` — A2 zet de indexrij recht.
- **WF-023, WF-027** noemen hun schrijfposities met de L4-namen (`target`
  van een `RuleVersion`; een lid van `initialisations`); de uitzondering
  voor de kenmerktoekenning volgt de ledensoort, niet `soort`.
- **WF-028**: de doelsoort is de gedeclareerde vuller van `targetRole`; «the
  chain MUST reach the target type» leest `path` tot die vuller.
- **WF-029, WF-054**: `subject` van de `ConsistencyVersion`; `scope` van de
  `UniquenessVersion`.
- **WF-030**: de overige armen lezen `sources` (sleutels lossen op; ariteit
  en soort per positie gelijk over de bronnen).
- **WF-031**: de positiearm noemt de twee sessiewaarden als `ContextValue`
  en laat `BoundDay` toe («the ONE context value it may carry is `de dag`»
  wordt «the bound day (`BoundDay`) is admitted; a `ContextValue` naming a
  session value is refused»).
- **WF-034**: de armen over de allocatievelden, het onderwerp van de
  objectcreatie en de vouwbron noemen de L4-veldnamen; de arm van de
  typeloze waardeknoop (`Literal.type: null`) ongewijzigd.
- **WF-037, WF-038, WF-083, WF-093, WF-102, WF-110, WF-055, WF-056**: lezen
  `Aggregation`/`BuiltinCall` met hun L4-velden (`arguments`,
  `elementType`, `emptyFallback`, `dimensionSelection`, `sourceRole`,
  `sourcePath`, `granularity`); meldingen die `elementObjecttype`, `bronRol`,
  `bronPad` of `argumenten` noemen (13 getuigen, gemeten) noemen de
  L4-velden.
- **WF-081, WF-084, WF-095** lezen de string in hun variant (`number`,
  `date`, `dateTime`) en sleutelen op de VARIANT, niet op het type — de
  kruising variant ↔ type is de nieuwe regel hieronder. WF-081 versmalt
  (U19, standaard ja) tot het `number`-patroon van het casusschema:
  geen komma meer, geen `.5` zonder leidend cijfer, een noemer zonder
  voorloopnul (gemeten: 0 dragers na migratie).
- **WF-039** leest `{"enumeration": …}` (het label tegen het gedeclareerde
  domein) en `{"number": …}` (het teken). De andere regels die een literaal
  raken (WF-036, WF-061, WF-062) lezen zijn `type`, niet zijn waarde
  (gemeten: WF-061 leest de literaalwaarde niet); hun getuigen bewegen
  alleen mechanisch.
- **WF-098**: de gesloten verzameling op `ContextValue.name` is
  `Rekendatum`, `Rekenjaar`, bytegelijk; `de dag` is `BoundDay` en een
  `ContextValue` met die naam valt als elke andere naam buiten de
  verzameling — voorstel «context value '<n>' is not one of the two
  admissible names Rekendatum, Rekenjaar: the context-value name is a closed
  set, spelled byte for byte».
- **Nieuw: WF-114** (D36; V9 (a) voorwaarde 2) — voorstel: «A `Literal`'s
  value variant MUST be the one its `type` names: `number` for a `Number`
  (a percentage included), `boolean` for a `Booleaan`, `text` for a `Tekst`,
  `date` for a day-precision `Date`, `dateTime` for a millisecond-precision
  `Date`, `enumeration` for an `Enumeratie`, `constructor` for a `SumType`.
  A literal whose `type` is null is WF-034's typeless value node and this
  rule does not reach it; a list or struct type names no variant.»
  Verifierzin: «literal value is a <variant> value but its type is <type>:
  the variant is the one the type names». Golden: elke golden-literaal
  (gemeten 532 literalen in 163 goldens, zes van de zeven varianten; de
  `constructor`-variant heeft 0 golden-dragers en 1 negatief-drager).
  Negatief: `wf114-literaalvariant-tegen-type`,
  `wf114-datumvariant-tegen-precisie`.

**Spec-tekst die mee moet** (A2, Engels; de lens leest elke gewijzigde
regel):

- IR-SPEC ch. 3 "Versioning": gelande vorm `{node, name, versions}`, de
  versierecords zonder `node`, de groepering als geschiedenis.
- 3.1: tabel `presentation?` met de v0-waarden tot L5 (de tabelwaarden
  `calculation`/`equation` zijn de L5-spelling van `BEREKENING`/
  `GELIJKSTELLING`); `statuteAnnotations` op de vorm (mirror-noot →
  geschiedenis); de Delta- en Migration-zinnen → geland; de zin over
  `jutterswet-penningdrempel` "in its v0 mirror form" herschreven.
- 3.2: `COMPLEX`-delta en WF-024 → geschiedenis. 3.3: `verbindendeRol` →
  `connectingRole` in de tabel (D32); `MemberInit` blijft op de wire
  `Toekenning` tot L5. 3.4: `FactCreation` met `path` geland, `doel`
  vervallen (D31), de alinea «The node keeps its v0 names … until the path
  regrouping of L4» → geland. 3.5 en 3.6: `bron`/`line` op de versie (D34);
  3.6: `Uniqueness` en `Source` geland (B27), en een uniciteit met twee of
  meer bronnen draagt geen `scope` (de gescoopte unie blijft een open
  ontwerpvraag). 3.10: elke versie schrijft haar `validity`; de
  v0-spelling `null` is geschiedenis (D28).
- 4.1: de `Literal`-rij (de varianten; komma → punt; `bron` houdt de
  juridische spelling); de `ContextValue`-rij (twee namen) en een nieuwe
  `BoundDay`-rij (D37 — «whether v1 gives it one is an open point» wordt
  besloten); de alinea's van WF-081/084/095/098.
- 4.3: de `BuiltinCall`-veldtabel (D35); de mirror-noten en de
  Migration-zin → geland; de `wf047-bereik-op-builtin`-zin → geschiedenis.
- 4.5: «"is gevuurd" is a v1 non-goal» → naar V13 (D39): beide statussen
  blijven, de v1-knoopvorm wacht op de paritygolden.
- 4.6: de WF-034-armen die L4-velden noemen. De WF-index: elke veranderde
  rij, de WF-114-rij, de WF-024-geschiedenisrij, de WF-082-golden.
- `RUNTIME-SEMANTICS.md`: elke plek die `Functie`, `leegwaarde`,
  `elementObjecttype`, `argumenten`, `bronRol`/`bronPad`, `Constante`,
  een literaal-`waarde`, `soort` of `verbindendeRol` als levende wirevorm
  noemt (grep-tellingen in RS op main `cd256d00`: `leegwaarde` 11,
  `elementObjecttype` 9, `Functie` 4, `argumenten` 4, `verbindendeRol` 4,
  `Constante` 3, `de dag` 6, `Rekendatum` 5; `waarde` en `soort` 76 en 23,
  grotendeels proza of andere knopen — per regel beoordelen). D33 is in de
  snijsessie getoetst: RS ch. 4.2 «groups are the equal-key classes of the
  ordering attribute, ascending or descending as declared» — het veld is de
  sorteersleutel; A2 bevestigt dat ref en clingo het zo lezen.

## Stempellijst (V14 (a), D17, D18)

Bron: `research/v1-klap/stempelmeting-negatief-l2-l5.md` §4 (de L4-tabel), §7
en §9b, bijgesteld door het register (D16, D35, D36, D37) en door de meting
van deze snijsessie: de L3-verifier (gebouwd in de L3-worktree; het
negatief-corpus is byte-gelijk aan main `cd256d00`) over alle 407
negatief-documenten, alleen gelezen — 302 getuigen melden hun eigen regel op een pointer in een
benoemde vorm (`resolvedRegels` 219, `consistenties` 28, `objectCreaties`
25, `verdelingen` 18, `feitCreaties` 8, `uniciteiten` 2, `uniciteitUnies`
2); hun pointer krijgt in L4 `versions/<v>/` en de L4-veldnamen, en hun
NOTE-citaten gaan mee. De stempelmeting van §4 noemde 13 getuigen (1
verwijderd, 12 herschreven, waarvan de twee WF-098-getuigen (i)); de meting
hieronder vindt 5 verwijderingen en 1 handmatige herschrijving, en zet de
twee WF-098-getuigen op (m): beide dragen hun defect al in de L4-vorm (de
namen `de dagX` en `rekendatum`).

**Verwijderd-met-reden (5)** — de meting dat m4.py erop faalt of dat de
verifier de vertaling aanvaardt, staat per getuige in het Result van A2:

| map | regel | delta | stempel | reden |
|---|---|---|---|---|
| `wf024-complex` | WF-024 | B21, D16 | verwijderd-met-reden | `COMPLEX` staat niet in de methode-enum: laadfout; WF-024 vervalt uit IR-SPEC en verifier (geschiedenisregel); m4.py faalt erop |
| `wf037-aantalgevulde-granulariteit` | WF-037 | B21, D35 | verwijderd-met-reden | een `Aggregation` draagt geen `granularity`: onschrijfbaar (m4.py faalt); de arm "granularity facet on a non-bucketing kind" verliest zijn object op de aggregatiesoorten |
| `wf047-bereik-op-builtin` | WF-047 | B21, D35 | verwijderd-met-reden | een `BuiltinCall` draagt geen `elementType` en geen bereik: onschrijfbaar (m4.py faalt); IR-SPEC 4.3 noemt de getuige nu bij naam en krijgt een geschiedenisregel |
| `wf034-sentinel-builtin-wratoperand` | WF-034 | B21, D35 | verwijderd-met-reden | idem: de `ABSOLUTE_WAARDE` draagt `elementObjecttype` en `bronRol`; de armwissel die L2 vastlegde (D9) verliest zijn drager |
| `wf030-single-source-union` | WF-030 | B27 | verwijderd-met-reden | een uniciteit met één bron IS de enkelvoudige vorm, dus de arm "a uniqueness union carries at least two sources" verliest zijn object; te meten: de verifier aanvaardt de vertaling (de getuige heeft geen `NOTE.md`, gemeten) |

**Herschreven (i) (1):**

| map | regel | delta | stempel | L4-vorm / reden |
|---|---|---|---|---|
| `wf034-typeloos-literal` | WF-034 | B22, D36 | herschreven (i) | het script kan zonder `type` geen variant kiezen en faalt; met de hand `"value": {"number": "2"}` met `"type": null`; de arm blijft die van de typeloze waardeknoop; de nieuwe kruisregel zwijgt (niets om tegen te kruisen) |

**Herschreven (m), met een L4-delta op het defect** — `expect.txt` per
getuige (U1 van L2); A2 meet per getuige de arm (L3-verifier op de L3-vorm
tegen de L4-verifier op de L4-vorm):

| map(pen) | regel | delta | L4-vorm |
|---|---|---|---|
| `wf021-version-overlap` | WF-021 | B19 | één `Rule` met twee versies waarvan de `validity` overlapt |
| `wf020-crossform-collision`, `wf020-defaultsgroep-botst-met-regel` | WF-020 | B19 | de naam is de `name` van de gegroepeerde vorm (`wf020-defaultsgroep-duplicaat` draagt geen L4-delta op het defect) |
| `wf107-naamloze-regel`, `wf107-blanco-regelnaam` | WF-107 | B19 | `Rule.name` `null` resp. blanco (`wf107-naamloze-defaultsgroep`: defaultslaag, geen delta) |
| `wf082-empty-bron-annotaties`, `wf082-blank-entry`, `wf082-padded-entry` | WF-082 | B19 | `statuteAnnotations` op de vorm, verbatim (`[]`, `[""]`, een opgevulde string); de melding noemt het L4-veld |
| `wf022-target-unresolvable`, `wf022-kenmerk-onwaar` | WF-022 | B20 | het doel van een `RuleVersion` zonder `presentation`; de waarde `{"boolean": false}` op een kenmerkdoel |
| `wf079-two-unconditional-writers` | WF-079 | B20 | twee vormen met elk één versie, `presentation: "BEREKENING"`, zonder `condition` |
| `wf023-initialisatie-domein` | WF-023 | B20 | `initialisation: true` |
| `wf028-create-along-final-hop` | WF-028 | B19, D31 | `path` = de stappen gevolgd door de laatste hop |
| de 13 `wf034-verdeling-*`, `wf025-max-without-ratio`, `wf026-two-allocations-one-target` | WF-034, WF-025, WF-026 | B19, D33 | de `AllocationVersion`-veldnamen; de melding noemt `sourceAttribute`, `orderingAttribute`, … in plaats van de v0-namen |
| `wf034-objectcreatie-loos-subject`, `wf034-objectcreatie-onbekend-subject`, de drie `wf027-*`, `wf047-creation-binding-wrong-filler` | WF-034, WF-027, WF-047 | B19, D32 | `ObjectCreationVersion` (`subject`, `initialisations`, `connectingRole`) |
| `wf029-no-subject` | WF-029 | B19 | `ConsistencyVersion.subject: null` |
| `wf030-arity-mismatch`, `wf054-dangling-scope-binding`, `wf054-to-many-scope-role` | WF-030, WF-054 | B27 | `sources` met twee `Source`-knopen; `scope` |
| de 12 overige `wf037-*`, de 11 `wf038-*`, de 5 `wf083-*`, de 5 `wf093-*`, de 6 `wf102-*`, de 6 `wf110-*`, de 4 `wf056-*`, de 5 `wf055-*`, de 4 overige `wf047-*` voor zover hun defect op een vouw of verdeling staat, de 3 `wf034-foldbron-*` | diverse | B21 | `Aggregation`/`BuiltinCall`; meldingen die `elementObjecttype`, `bronRol`, `bronPad` of `argumenten` noemen (gemeten: 13 getuigen) noemen de L4-velden |
| `wf081-unit-in-lexeme` | WF-081 | B22 | `{"number": "5 €"}` |
| `wf084-bare-non-iso`, `wf084-source-snippet` | WF-084 | B22 | `{"date": "31-01-2024"}`, `{"date": "dd. 31-01-2024"}` |
| de zes `wf095-*` | WF-095 | B22 | `{"dateTime": …}`, elk zijn eigen spellingsfout |
| `wf039-negative-under-nonneg` | WF-039 | B22 | `{"number": "-3"}` onder `NIET_NEGATIEF` |
| `wf098-onbekende-contextwaarde-naam` | WF-098 | B23 | `ContextValue {name: "de dagX"}`; de gesloten verzameling telt nu twee namen |
| `wf098-contextwaarde-naam-kapitalisatie` | WF-098 | B23 | `ContextValue {name: "rekendatum"}` — al een herkapitalisering van `Rekendatum`, zoals D37 vraagt |
| `wf031-sessiewaarde-in-dagsoort` | WF-031 | B23 | een `ContextValue` `Rekendatum` in een dagsoortvoorwaarde |

Mechanisch, zonder L4-delta op het defect (geen stempelrij; A2 meldt de
telling): de 26 consistentiegetuigen met `geldigheid: null` en de 11
negatief-documenten met een regel met `geldigheid: null` (46 knopen); de 6
negatief-documenten met een kommaliteraal; `wf036-gelijkheid-enumeratie-
tegen-somtype`, de enige drager van de `constructor`-variant (`{"constructor":
"Wad"}`; WF-036 leest het type, niet de waarde); alle andere documenten
waarvan alleen de pointer beweegt.

**Nieuw (met `NOTE.md` en `expect.txt`)** — de mapnamen zijn voorstellen
(U-besluiten); de eerste drie volgen uit het register, de andere uit
U11, U19 en U23:

| map | regel | wat het pint | grond |
|---|---|---|---|
| `wf022-doel-attribuut-en-kenmerk` | WF-022 (N3-arm) | een regeldoel waarvan de naam op het subjecttype attribuut én kenmerk is (gebroken uit `anchor/jutterswet-toelagegronden`, met een regel die `toelage` schrijft) | N3 |
| `wf114-literaalvariant-tegen-type` | WF-114 | een `text`-variant bij een `Number`-type | D36 |
| `wf114-datumvariant-tegen-precisie` | WF-114 | een `date`-variant bij een `Date` met `millisecond` | D36 |
| `wf022-kenmerkdoel-met-presentatie` | WF-022 (ledensoortarm) | een versie met `presentation` die een kenmerk schrijft | U11 |
| `wf022-attribuutdoel-zonder-presentatie` | WF-022 (ledensoortarm) | een versie zonder `presentation` en zonder `initialisation` die een attribuut schrijft | U11 |
| `wf098-de-dag-als-contextwaarde` | WF-098 | `ContextValue {name: "de dag"}`: de gebonden dag is geen contextwaarde meer | U23 |
| `wf081-decimale-komma` | WF-081 | `{"number": "12,50"}`: de komma is sinds L4 geen getalspelling meer | U19 |

Verwachte negatief-stand: met alle standaardantwoorden 407 − 5 + 7 = 409;
zonder U11, U19 en U23 407 − 5 + 3 = 405; de vloer 350 blijft.
`EXPECTED_MIN_EXPECT` stijgt van 165 naar de gemeten stand (elke herschreven
getuige hierboven zonder `expect.txt` krijgt er een, elke nieuwe getuige
ook). `corpus/negatief/README.md` krijgt een sectie "L4 — the rule and
expression layer" naar het model van L3 (Engels; kolommen directory, rule,
delta, stamp, arm, reason), met de verwijderde getuigen en hun meting, de
herschreven getuigen met hun arm in woorden, de nieuwe getuigen, en de zin
over de pointers die alleen verschoven.

## Acceptatie

Elke bouwer sluit met de genoemde gates groen op zijn eigen branch; de kaart
sluit met alle veertien groen op de samengevoegde branch (staat die op de
main-tip, dan is dat de main-run). Stand op main `cd256d00` (de
L3-landingsrun, alle veertien groen) tussen haakjes; bij L4 mogen alleen de gate-2-zelftestrijen, de
gate-3-stand, `EXPECTED_MIN_EXPECT` en de sleutels van de gate-13-twister
bewegen — elke andere beweging is een nulmeting.

- **(a)** `python3 spec/proposals/v1-migratie/m4.py --check` meldt 0
  L3-documenten in de vier zones en de pariteitszone (de dump na de
  her-emissie van C); idempotent (een tweede `--all` wijzigt geen byte); de
  tellingen per zone in het Result. De weigeringen van `--all` zijn precies:
  `wf024-complex`, `wf037-aantalgevulde-granulariteit`,
  `wf047-bereik-op-builtin`, `wf034-sentinel-builtin-wratoperand` (de vier
  verwijderingen die het script raken), `wf034-typeloos-literal` (met de
  hand) en `toka/casus/ir.json` (2× `COMPLEX`; U1); elke andere
  weigering is een bevinding.
- **(b)** Gate 2: metaschema-valide; alle golden- en rood-documenten (289)
  valideren en round-trippen; `EXPECTED_MIN_DOCS` 192 blijft; zelftestrijen
  (nu 26 patroon- en 123 vormrijen) erbij voor elk vervallen veld en elke
  nieuwe vorm van checklistpunt 2 — minimaal: `uniciteitUnies` in `rules`;
  een `ResolvedRegel`-, `Functie`- en `Constante`-knoop; een `Rule` zonder
  `versions` en met `versions: []`; een versierecord met `node`; een
  `RuleVersion` met `soort`, zonder `initialisation`, met `validity: null`,
  met `presentation: "KENMERKTOEKENNING"`, en (U10) met
  `initialisation: true` plus `presentation`; `statuteAnnotations: []`; een
  vormnaam `"x #2"` (U3); `method: "COMPLEX"`; `groepeerAttribuut`
  op een verdeling; `verbindendeRol` op een objectcreatie; `doel` en
  `tussenstappen` op een feitcreatie en `path: []`; `sources: []`,
  `keys: []`, en (U6) twee bronnen met `scope`; `granularity` op een
  `Aggregation`, `elementType` op een `BuiltinCall`, een `kind` van de andere
  knoop; de L2-rijen van de vouwbinding met de L4-namen (`SOM` met
  `elementType` zonder bereik geweigerd, `TOTAAL` zonder bereik aanvaard,
  beide bereiken geweigerd); `Literal` met `waarde`, met `value` als string,
  als JSON-getal in `number`, met twee varianten, met `"12,50"`, `"5 €"`,
  `"31-01-2024"` en `"2026-03-15T12:00"` in hun variant, met
  `{"boolean": "waar"}`, en (U8) een `constructor` met `payload`;
  een `ContextValue` zonder `type` en met `name: null`; een `BoundDay` met
  `name`; en als aanvaarde rij een `ContextValue` met `name: "de dag"` (het
  schema laat de naam vrij, WF-098 weigert hem).
- **(c)** Gate 3: alle golden- en rood-documenten verifier-schoon (289);
  negatief 405–409 (409 met U11, U19 en U23),
  elk op zijn regel én, waar `expect.txt` staat, op zijn arm;
  `EXPECTED_MIN_NEGATIEF` 350 blijft; `EXPECTED_MIN_EXPECT` van 165 naar de
  gemeten stand in dezelfde commit (de enige regel van `check-verifier.sh`
  die beweegt).
- **(d)** Gate 4 ongewijzigd (667 casusbestanden; het casusformaat verandert
  niet, D56).
- **(e)** Gates 5, 6, 7, 8: GEEN uitkomst beweegt. Ref-suite (955, vloer 489)
  en clingo-suite (1129, vloer 83) herschreven op de L4-wire met hun
  tellingen; 619 replays elk; noop 582 + 37 overlevenden, lijst
  ongewijzigd; rood 48 cases, 96 fragmenten, Drools-poot 11/37 ongewijzigd.
  Gemeten: twee rood-fragmenten noemen "the Rekendatum constant"
  (`tijdsduur-reads-van-before-tot`, ref en clingo); verandert die zin
  (U24), dan worden beide fragmenten opnieuw gepind, per regel in het
  Result; `contextwaarde-de-dag-buiten-dagsoort` pint al "the bound day of a
  day-kind definition" en blijft staan.
- **(f)** Gates 9, 10, 14: alle emittergoldens byte-identiek na her-emissie
  (12 catala; 73 rgs; 2 flogic) én aan de mechanische migratie van
  diezelfde documenten (een verschil is een bevinding over script of
  emitter); de drie rgs-goldens met een kommaliteraal (`meldloon`,
  `vangstklem`, `wrakrecht`) schrijven een punt in `value.number` en de
  komma in `bron`; vloeren 201/12, 173/38, 81/1 blijven; suites (207, 559,
  135) gemeld.
- **(g)** Gate 11: `toka/casus/ir.json` opnieuw geëmitteerd door de frontend
  (de twee `COMPLEX`-verdelingen volgens U1) en byte-identiek aan m4.py's
  migratie van een meetkopie van de L3-dump zonder die twee verdelingen,
  plus de twee diagnostics van U1 — elk ander verschil is een bevinding; 29 slices via
  m4.py; replay 29/29/29; suite ≥ 27; 45 fragmenten, holdouts `toka-23` en
  `toka-45` ongewijzigd tenzij gemeten; `toka/README.md` krijgt de
  divergentieregel L4.
- **(h)** Gate 12: `EXPECTED_PLAYED` 188 en `EXPECTED_DRL_PLAYED` 167 opnieuw
  gemeten en gelijk (geen kanaalwissel verwacht: L4 verandert geen
  betekenis); JUnit (717) zonder skips; de Drools-lader leest de zes vormen,
  `Aggregation`/`BuiltinCall`, `Literal.value`, `ContextValue`, `BoundDay`
  en weigert elk vervallen veld bij naam (het `OnbekendVeldWitnessTest`-
  model, een getuige per veld van checklistpunt 2).
- **(i)** Gate 13: de twister op de L4-namen; 40 twists over 22 goldens
  (vloer 38) en de DRL-tak 21 (vloer 20) blijven; alle legs op hun eigen
  ongetwiste regel.
- **(j)** Naamtabeltoets rc 0: de L4-namen als identiteit of met `geland:
  "M5"` (regellaag: B19, B20, B21, B27) of `"M6"` (expressielaag: B22,
  B23); de vervallen v0-namen geland-vervallen; de twee `onzeker`-rijen
  `groepeerAttribuut@*` en `verbindendeRol@ResolvedObjectCreatie` beslist
  (D33, D32); de `Constante`-rij met twee doelgroepen (`ContextValue`,
  `BoundDay`), zoals de `Functie`-rij er al twee heeft (`Aggregation`,
  `BuiltinCall`); `geland_telling` bijgewerkt.
- **(k)** Vervalt: geen herpin van `regelspraak-java` (maintainerbesluit
  2026-09-24: geen tussentijdse herpins; één pin-bump als v1 klaar is). De
  toolchain-impact van A.7 (7 zekere en 1 mogelijke `IrEmitterTest`-kanteling,
  21 bestanden in de recordlaag die bij ongewijzigde records niet bewegen)
  landt bij die ene pin-bump.

## May touch / NIET / Out of scope

May touch (per bouwer disjunct, zie de werkverdeling): `spec/schema/**`,
`spec/proposals/v1-migratie/**`, `spec/proposals/v1-naamtabel.json` (+
toets), `spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md` (alleen de genoemde
plekken), `spec/verifier/**`, `corpus/**`, `toka/**`, `ref/**`,
`backends/clingo/**`, `backends/drools/**`, `frontends/**`, en van
`scripts/`: `check-ir-schema.sh` (zelftestrijen),
`check-verifier.sh` (alleen `EXPECTED_MIN_EXPECT`),
`check-reftype-derivation.sh` (de twister op de L4-namen),
`check-drools.sh` (pins en kanaallijsten, alleen bij een gemeten wissel),
`check-rood.sh` (alleen bodemzinnen waarvan de tekst beweegt) — elke
gate-scriptwijziging per regel in het Result. NIET: `spec/CONSTRUCTS.md`,
`spec/PLAN.md`, `CLAUDE.md`, `WAY-OF-WORKING.md`, `.github/**`,
`spec/proposals/V1-WIREMIGRATIE.md` (behalve de L4-rij van de
landingstabel, A2), en geen herpin of wijziging in `regelspraak-java`.

Out of scope: B24, B25 (behalve de IR-SPEC-zin van D39), B28, B26, B6, B10
(L5 of later); de lijstsleutels onder `rules` (D26); de enumwaardespellingen
(`GELIJKSTELLING` blijft, D4); de hernoeming van `Geldigheid`, `Toekenning`,
`DimensieSelectie`, `RolBinding`, `ResolvedDagsoort`, `ResolvedStartpunt`,
`ResolvedRegelGroep`, `Onopgelost`, `Regelversie` (L5); de sleutels van de
defaultslaag en een `statuteAnnotations` daarop (D44); de gescoopte unie
(B27, ontwerpvraag); een canonicalisatiepas (D56); het OPEN-punt
percentage-van; de Java-records en de toolchain; alles wat betekenis
wijzigt. Een bouwer die een betekenisverschil meet (een golden die ná L4
anders uitkomt; een golden die van gate-12-kanaal wisselt) STOPT op dat
punt, legt de meting in het Result en gaat verder met de rest — dat is de
nulmeting.

## Werkverdeling

Vier bouwers, kaartbranch `card/l4-regel-en-expressielaag` vanaf main
(worktree `wt/l4`); A1 eerst; daarna A2, B en C parallel op `card/l4-a2`,
`card/l4-b`, `card/l4-c` vanaf A1's kop (worktrees `wt/l4-a2`, `wt/l4-b`,
`wt/l4-c`); de hoofdsessie voegt serieel samen (ff/rebase), draait de
veertien gates, zet één lens op de unie (checklist + kaart + elke gewijzigde
spec-regel), fixronde op eigen branches vanaf de unie met het
vondstenbestand van de lens als werklijst, herkeuring strikt op de gemelde
punten, adviesronde in dezelfde kaart zonder derde lens. Bestanden per
bouwer disjunct; een bestand dat twee bouwers nodig hebben staat hieronder
bij één van hen, en de ander vraagt erom via de hoofdsessie.

**De in-memory afbeelding is het koppelvlak tussen A2 en B** (D6, U3 van
L3): de OCaml-AST houdt zijn v0-constructoren waar dat kan, met drie
aanvullingen die de wire afdwingt (U12–U14, U18): per afgevlakte
versie de wirepointer; de variant van een literaal naast het lexeem; een
eigen constructor voor de gebonden dag. De hoofdsessie legt die signatuur
vast als U-besluit vóór A2 en B starten, zodat beide ertegen bouwen; A2
schrijft haar in `spec/verifier/lib/ast.ml` en de lader als eerste commit
en meldt de kop, B rebaset daarop.

- **A1 — de wire.** `spec/schema/uchar-ir-v1.schema.json` (de elf knopen en
  zes versierecords; de `expr`-unie zonder `Functie`/`Constante`, met
  `Aggregation`, `BuiltinCall`, `ContextValue`, `BoundDay`; `rules` zonder
  `uniciteitUnies`; de voorwaarden: vouwbinding met de L4-namen, geen
  `presentation` naast `initialisation: true`, geen `scope` bij twee of meer
  bronnen, de `kind`-verzameling per knoop, de literaalvarianten met de
  patronen van `uchar-case-v0`) en `spec/schema/README.md`;
  `scripts/check-ir-schema.sh` (`_l4_base`, `L4_SHAPE_ROWS`, de
  patroonrijen voor `number`/`date`/`dateTime` naast de 26 van `Geldigheid`;
  de M1/L2/L3-bases op de L4-vorm); `spec/proposals/v1-migratie/m4.py`; alle
  `corpus/**/ir.json` en `toka/cases/**/ir.json` mechanisch (de
  weigeringen van acceptatie (a) blijven L3 en worden gemeld;
  `toka/casus/ir.json` is voor C); `spec/proposals/v1-naamtabel.json` +
  toets (j). Sluit met gates 2 en 4 groen; rapporteert per zone de
  tellingen en elke weigering.
- **A2 — verifier en spec.** `spec/verifier/lib/{ast,loader,checks,desugar}.ml`
  (de in-memory afbeelding als eerste commit; de laderweigeringen van
  checklistpunt 2 met de zinnen die ook B en C schrijven; de regels van
  "Regels die veranderen"; WF-114; de pointers naar de L4-wire),
  `scripts/check-verifier.sh` (alleen `EXPECTED_MIN_EXPECT`),
  `spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md` (de genoemde plekken,
  Engels, ZELF geschreven), `corpus/negatief/**` behalve de mechanische
  `ir.json`-migratie van A1 (de vijf verwijderingen, `wf034-typeloos-literal`
  met de hand, de nieuwe getuigen, elke `NOTE.md` en `expect.txt`, de 203
  NOTE-citaten met een pointer in een benoemde vorm opnieuw uit de echte
  verifieruitvoer), de zeven `corpus/golden/**/NOTE.md` met zo'n pointer,
  `corpus/negatief/README.md` sectie "L4", en de L4-rij van de
  landingstabel in `V1-WIREMIGRATIE.md` ("loopt"). Sluit met gate 3 groen
  (en 2, 4).
- **B — de OCaml-kant.** `ref/lib/{eval,case,world,alloc}.ml` voor zover
  de AST-aanvullingen, de pointers (`evaluate` koppelt `"/resolvedRegels/%d"`
  aan de stratumsleutel van `Checks.dep_forms`) en de nieuwe weigerzinnen
  (variant tegen type, een contextwaarde met de naam `de dag`) ze raken;
  `ref/test/test_ref.ml` (bouwt de AST direct: 83 `litv`, 28 `rr`, 23
  `Functie`-records); `backends/clingo/lib/{normalise,translate,model}.ml`
  idem; `backends/clingo/test/test_clingo.ml` (39 ingebedde IR-documenten
  op de L4-wire, met m4.py over de tekstblokken waar ze heel zijn, en
  laadweigeringsrijen voor de vervallen L4-velden naast de 35 van L3);
  `frontends/catala/lib/{emit,translate}.ml` en `frontends/flogic/lib/{emit,
  lower}.ml` (de L4-vorm in canonieke volgorde: catala schrijft regels,
  consistenties, objectcreaties en `Constante` — die wordt `ContextValue` of
  `BoundDay` — en weigert al een `Functie`; flogic schrijft `SOM`/`AANTAL`
  als `Aggregation`, `emit.ml` r. 95–114; geen van beide schrijft
  `bronAnnotaties`, verdelingen of uniciteiten, en beide schrijven de lege
  lijst `uniciteitUnies`, die verdwijnt; beide suites hebben een
  regelsjabloon met `soort: null` en `geldigheid: null` dat onschrijfbaar
  wordt) met hun suites; `scripts/check-reftype-derivation.sh` (de twister:
  `KLASSE` "Functie", `AFGELEIDE_KIND`, `UITZONDERING` Literal,
  `GEEN_FUNCTIE` TIJDSDUUR/ABSOLUTE_TIJDSDUUR, `positieklasse`, `afgeleid`
  sleutelen op `Functie`/`kind`); `scripts/check-rood.sh` (alleen
  bodemzinnen waarvan B de tekst verandert: 10 noemen een functiearm, 7 een
  "literal", 4 regel/soort/doel) en de twee rood-fragmenten van
  `tijdsduur-reads-van-before-tot` als hun zin verandert. Sluit met gates
  5, 6, 7, 8, 9, 14 groen; gate 13 op de unie.
- **C — de Java-kant.** `frontends/regelspraak` — `emit/IrEmitter.java`
  (de L4-vorm; versiegroepering uit de in-memory namen met dezelfde regels
  als m4.py; de afbeelding van `soort`; `Functie` naar `Aggregation`/
  `BuiltinCall`; de literaalvariant uit het type, `,` → `.`; `Constante`
  naar `BoundDay`/`ContextValue` en een luide weigering buiten de drie
  namen; `uniciteiten` + `uniciteitUnies` samen; `path`; `connectingRole`;
  de `COMPLEX`-verdeling volgens U1; de weigeringen van U25),
  `IrEmitterTest` (43 tests; L4-relevant: 9 op een literaal-`waarde`, 2 op
  `bronRol` van een `Functie`, 1 op `dimensieSelectie` als volgordeanker, 1
  op `bronAnnotaties`; nieuw: de versiegroepering en de samengevoegde
  uniciteit, want geen rgs-golden en geen toka-document draagt twee versies
  of een unie — gemeten) en de acht andere testbestanden die emitterfragmenten
  pinnen (`AantalGevuldeDialectTest`, `TelbereikTest`, `GenesteVouwbronTest`,
  `SubtypeDialectTest`, `AantalUnieTest`, `VariabeledeelLoweringTest`,
  `DefaultsExtensieTest`, `MarkerVerslagTest`); de records bewegen niet
  (D6). `backends/drools` — `loader/IrLoader.java` (strikt op de L4-vorm,
  bijectie naar de records, weigering bij naam van elk vervallen veld, de
  versie-uitvouwing, `doel` uit de vuller van `targetRole`, variant tegen
  type, een contextwaarde `de dag`), `schema/ContractValidator.java`
  (`uniciteitUnies`), de transpilers alleen waar zij verdwenen
  wire-informatie lazen (de lezers van `ResolvedRegel.Soort` —
  `kern/StaandeDoelen`, `Stratificatie`, `graaf/ProducentConsument`, de
  `switch r.soort()` van de exec-tak — en `CaseRunner.zonderVersie` blijven
  door de bijectie ongewijzigd; de exec-tak speelt sinds L3 geen
  vergelijking over percentages en dat blijft zo, L3-open punt 7); de tests: 20 ingebedde IR-documenten in 17
  bestanden, 55 JSON-pointers in 12 testbestanden die in een benoemde vorm
  wijzen, de 11 laderweigeringsgetuigen (`OnbekendVeldWitnessTest`: de
  knoopsoortvloer 59 blijft gehaald — elf soorten vallen weg, elf nieuwe
  komen in de goldens), en een nieuwe `L4`-weigeringsgetuige naar het
  model van `TypesysteemWeigeringWitnessTest`. `toka/casus/ir.json` opnieuw
  geëmitteerd, `toka/README.md` (divergentieregel L4, alleen paden,
  pointers en tellingen), `scripts/check-drools.sh` alleen bij een gemeten
  wissel. Toolchain-impact A.7 als input: de recordlaag (21 bestanden in de
  toolchain) beweegt niet omdat de records blijven; de draadlaag kantelt
  bij de ene pin-bump na v1. Sluit met gates 10, 12 en 11 groen.

Elke bouwer: commit klein en vaak met een bouwbare branch; geen
`git add -A`; geen kaartjes in `backlog/new` (restpunten als genummerd open
punt in het Result); nooit pushen of mergen; CONSTRUCTS/PLAN/CLAUDE.md
nooit; loopt de lens-checklist na en noemt per punt wat hij mat; eindigt met
een Result-blok (stand, tellingen, gate-uitvoer, scriptregels, open punten)
— ook op zijn contextgrens, na een commit van wat staat.

**Lessen van L2 en L3 (U10 van L3, letterlijk overgenomen, bindend voor
deze kaart):** (a) Tijdens de bouw landt de hoofdsessie GEEN andere kaart
op main (de clingo-kaart midden in L2 kostte een bouwer zijn context aan
een conflict); (b) A2 schrijft de spec-tekst ZELF, zonder fork — de lens
leest elke gewijzigde spec-regel; (c) elke bouwer krijgt bij een fixronde
het vondstenbestand van de lens als werklijst en werkt op een eigen branch
vanaf de unie (disjuncte bestanden; de hoofdsessie rebaset serieel); (d)
een bouwer die een betekenisverschil meet STOPT op dat punt en meldt het
(de nulmeting); (e) elke nieuwe zin die een uitvoerder weigert, staat
letterlijk gelijk in verifier, ref, clingo en Drools (L2 kostte twee rondes
aan ongelijke zinnen); (f) geen besluitnummers (D/U) in IR-SPEC, RS, schema
of golden-NOTE's — verwijs naar B-delta's, CONSTRUCTS-rijen en deze kaart.

## Registerwerk bij de landing (hoofdsessie)

`spec/CONSTRUCTS.md`, scopematrix: rij 1 (de `Functie`-splitsing geland:
`Aggregation`/`BuiltinCall`, B21), rij 2 (versies expliciet, `soort`
uiteengevallen in `initialisation`/`presentation` en de ledensoort van het
doel, B19/B20), rij 7 (`initialisation` als vlag), rij 17 (`COMPLEX` een
laadfout, WF-024 vervallen), rij 19 (`Consistency` met `bron`/`line` op de
versie), rij 20 (`Uniqueness` samengevoegd; de gescoopte unie blijft
buiten), rij 21 (`connectingRole`), rij 22 (`path`, `doel` vervallen), rij
23 (de gebonden dag van een dagsoortdefinitie als `BoundDay`), rij 25 (de
`Constante`-splitsing geland), rij 27 (de getypeerde literaal geland,
WF-114), rij 28 (`dimensionSelection` op `Aggregation`), rij 30 (de IR-SPEC-
zin naar V13; B25 wacht op de paritygolden), rij 11 (de startpuntregistratie
blijft tot L5, D40). Knoopinventaris: 2.1 `Literal`, `Functie` met de drie
`FunctieKind`-rijen, `DimensieSelectie` (veld hernoemd, knoop v0 tot L5),
`Constante`; 2.2 `Regelversie` (ongewijzigd, D39); 2.3 `ResolvedRegel` +
`Soort`, `ResolvedVerdeling` + `Methode` + `Methode.COMPLEX`,
`ResolvedObjectCreatie`, `ResolvedFeitCreatie`, `ResolvedConsistentie`,
`ResolvedUniciteit`, `ResolvedUniciteitUnie`, `Geldigheid` (de `null`-
spelling op versies weg). `spec/PLAN.md` §9 (11): L4 geland met datum en
hash, plus de zin «is followed by a toolchain re-pin» naar het
maintainerbesluit van 2026-09-24 (geen tussentijdse herpins). `spec/proposals/V1-WIREMIGRATIE.md`: de L4-rij (A2 zet "loopt",
de hoofdsessie GELAND, met B24/B25/B28 als "niet mee" en hun reden).
`CLAUDE.md`: geen gatetekst verandert verwacht — gate 13 noemt geen
`Functie` (gemeten) en zijn vrijstelling «the Literal lexeme» blijft
kloppen (de string in `value.number` is het lexeem, de eenheid staat in het
`type`); gate 2 en gate 3 noemen geen L4-vorm. Daarna de memory.

## Correcties op de kaarttekst (gemeten door de bouwers, aanvaard door de hoofdsessie)

- **U8** (A1): de tekstvariant volgt het casusschema letterlijk, dus `{"text": ""}` is
  toegelaten (0 dragers; een aanvaarde gate-2-rij pint het); "niet-lege string" geldt via
  de patronen voor `number`, `date` en `dateTime`, niet voor `text`.
- **U13 en U15** (A2): "de gedeelde OCaml-lader weigert" is dezelfde lader als die van de
  verifier; een laadweigering zou `verifier.exe` vóór elke regel laten stoppen en de
  WF-114- en WF-098-getuigen uit gate 3 halen. De lader kreeg daarom een leesstand
  (`Loader.reader = Verifier | Executor`, standaard `Executor`): de verifier laadt het
  document en laat zijn regel oordelen; ref, clingo en de emitter-suites weigeren bij het
  laden met de kernzin.
- **Laderzinnen** (A2): de kaart stelde bij U3, U4, U6, U8, U9 en U10 geen zin voor; A2 koos
  ze en C nam ze letterlijk over (de lijst staat in het Result).
- **Naamtabel** (A1): de rij `soort@ResolvedRegel` stond als gat; de Delta-alinea van IR-SPEC
  3.1 noemt de afbeelding, dus de rij heeft twee groepen met herkomst "vermelding".
  `BoundDay` is een gat tot A2's rij in IR-SPEC 4.1 er staat (dan identiteit + geland M6).
- **Schema-README** (A1): de L3-knooptelling 10802 was gemeten 10839; op L4 zijn het 11509
  knopen plus versierecords.
- **Negatief-zone** (A1, A2): na de migratie zijn 26 van 407 documenten schema-ongeldig (de
  vijf geweigerde plus 21 waarvan het schema het defect nu ook sluit); gate 2 valideert de
  zone niet en de verifier-lader laadt elke string in een variant, zodat de WF-regel oordeelt.
- **Clingo-suite** (B): geen "39 ingebedde documenten" — de suite laadt 973 IR-documenten uit
  hele tekstblokken en fragmenten; 935 zijn structureel gelijk aan m4.py's migratie van hun
  L3-tegenhanger, de 38 van de L2-laadtabel (zonder feittype, m4.py weigert hun basis) zijn met
  de hand omgezet; `model.ml` hoefde niet te veranderen; de catala-suite telt 216 door het
  proefdocument.
- **U15** (B): bij wire-documenten zit de benoemde arm al in de gedeelde lader; ref en clingo
  kregen er een voor handgebouwde knopen.
- **Gedeelde zinnen** (B, C, hoofdsessie): ref en clingo zetten eerst " (WF-0xx)" achter de drie
  met de verifier gedeelde zinnen; de hoofdsessie haalde die staart weg, zodat verifier, ref,
  clingo en Drools byte-gelijk zijn.
- **U11-zinnen** (C): het kaartvoorstel miste "a " en de staart die lid en type noemt; de
  definitieve zinnen zijn die van de verifier.
- **U19** (C): "één grammatica voor één variant" — de Drools-lader weigerde eerst alleen de komma
  en las `.5`, `5 €`, `-.5` en `3/04` stil verkeerd; sinds de landing weigert hij elk getal
  buiten de casusgrammatica, zoals ref en clingo.
- **Knoopsoortvloer** (C): elf oude soorten verdwijnen, maar tien nieuwe verschijnen in goldens
  die laden (`ContextValue` alleen in een golden die om een andere reden geweigerd wordt) plus
  zes versierecords; de meting ging van 59 naar 64. `ContractValidator` hoefde niet te
  veranderen (leest de in-memory lijsten).
- **Meldingen met een v0-veldnaam** (A2): 21 in plaats van 13; negatief-NOTE's met een pointer
  in een benoemde vorm: 199 in plaats van 203 (vier hoorden bij verwijderde getuigen).
- **Gate 11 en 8** (B, C): op een bouwersbranch alleen niet groen te krijgen (de Drools-poot
  is van C, de OCaml-lezers van B); C2 mat ze groen op een proefunie.
- **U13** (B-fix, gemeten in de fixronde): geen lezer las het v0-lexeem `Literal.waarde`, ook de
  verifier niet (die leest de variant); het veld is uit de AST gehaald — dode staat die uiteen kon
  lopen. `Loader.literal_lexeme` blijft als functie voor wie de string nodig heeft.
- **U19 en dezelfde klasse** (lens B6, B-fix, C-fix): niet alleen een getal, ook een datum- of
  datumtijdlexeem buiten de grammatica wordt door elke uitvoerder bij het laden geweigerd met de
  kernzin van de verifier (WF-081, WF-084, WF-095, één gedeelde functie per regel in checks.ml);
  de verifier laadt en laat zijn regel oordelen.
- **Kwantorvouw** (B-fix, gevonden bij het meten): de Kwantorvouw houdt tot L5 zijn v0-velden
  (`bronRol`/`bronPad`); clingo's gedeelde bereikcode noemde sinds L4 de Aggregation-velden, zodat
  vier lezers vier uitkomsten gaven — nu toetst een eigen functie de kwantorvouw met de
  WF-108-zinnen van de verifier.
- **A8** (C-fix): `Rekenjaar` is admitted-and-unexecuted; Drools voerde het uit en weigert het nu
  vóór het verlagen met dezelfde zin als ref en clingo; `rsRekenjaar` uit de geporteerde runtime.
- **Kaartpunt 3 en 4 van N**: het `regels`-register en `kenmerkSoorten`/`kenmerkGranulariteit`
  verlieten de wire bij L1 (M1-kaart), niet bij L2/L3 zoals de hoofdsessie schreef.

## Result

**Geland (L4 van de v1-klap, 2026-09-24; basis main `1014450f` — L1, L2 en
L3 eronder; de landingscommit is deze kaartverplaatsing zelf, unie
`2c38f4a0`).** Vier bouwers (A1 wire, A2 verifier/spec in twee fasen, B
OCaml, C Java — C liep op zijn contextgrens en C2 maakte zijn werk af),
27 uitvoeringsbesluiten van de hoofdsessie (U1–U27) bovenop D26–D40, de
kruisende besluiten en het standaardantwoord op N3; één lensronde op unie 1
(`e1f329ec`: EERST FIXEN — B1–B7 en A1–A12, alle in dezelfde kaart gefixt op
vier branches door A2-fix, B-fix, C-fix en een NOTE-fixer N, plus wat de
fixers zelf van dezelfde klasse vonden), de herkeuring door een verse lens op
unie 2 (`076d40a4`): EERST FIXEN op alleen tekst (H1–H3) — in de adviesronde door de hoofdsessie afgehandeld, zonder derde lens. Alle veertien gates groen op de unie, die op de main-tip
stond.

**De wire.** De zes benoemde vormen als `{node, name, versions}` met
versierecords zonder `node` (B19: de `" #i"`-afvlakking omgekeerd, `validity`
altijd geschreven, `statuteAnnotations` op de vorm); `RuleVersion
{validity, target, value, condition?, initialisation, presentation?}` (B20:
`soort` uiteengevallen; de kenmerktoekenning is een versie zonder tag en
zonder initialisatie waarvan het doel een kenmerk is); `Allocation`,
`ObjectCreation` (met `connectingRole`), `FactCreation` (met `path`, zonder
`doel`), `Consistency` en `Uniqueness` met `sources: [Source]` (B27; één
lijst `uniciteiten`, `uniciteitUnies` weg); `Aggregation` en `BuiltinCall`
elk met alleen eigen velden (B21); `Literal.value` in de zeven varianten van
het casusschema, gekozen door `type`, komma → punt met de juridische
spelling in `bron` (B22c); `BoundDay {type, bron}` en `ContextValue {name,
type, bron}` (B23). Enumwaarden en de sleutels onder `rules` v0 tot L5
(D26); B24, B25 en B28 niet mee (D38–D40). In-memory bleven de
v0-constructoren in elke lezer: de regelsoort is een structurele afbeelding
uit vlag en tag, de literaal houdt zijn lexeem naast de variant, de gebonden
dag kreeg een eigen constructor, elk record van de zes vormen draagt de
wirepointer van zijn versie; de gedeelde OCaml-lader kreeg een leesstand
(`Verifier` laadt wat de verifier moet beoordelen, `Executor` weigert het
bij het laden met de kernzin van de regel: de variant tegen het type, een
contextwaarde buiten de twee namen, en sinds de fixronde ook een getal-,
datum- of datumtijdlexeem buiten de grammatica — één gedeelde functie per
regel in `checks.ml`); `Literal.waarde` (het v0-lexeem) en `Complex` gingen
uit de AST, want geen lezer las ze meer.

**Migratie.** `spec/proposals/v1-migratie/m4.py` (`migrate_bytes`, `--all`,
`--check`, idempotent, luid met JSON-pointer): 720 van 726 documenten
mechanisch (anchor 40, golden 202, negatief 402, rood 47, toka-slices 29);
de zes weigeringen zijn precies de verwachte — de vier onschrijfbare
getuigen, `wf034-typeloos-literal` (met de hand) en de casus-dump (2×
`COMPLEX`, door C opnieuw geëmitteerd). Elk document veranderde
(`uniciteitUnies`), 37 alleen daarin. Tellingen: Rule-vormen 1014 uit 1016
v0-invoeren (2 versiegroepen van 2); soort BEREKENING/GELIJKSTELLING/
INITIALISATIE/KENMERKTOEKENNING 581/202/3/230; `validity` uit `null` 97 in
43 documenten; Allocation/ObjectCreation/FactCreation/Consistency
40/47/24/40; Uniqueness 15 enkelvoudig + 9 unie; Aggregation/BuiltinCall
251/78; literalen number/boolean/text/date/dateTime/enumeration/constructor
724/237/67/87/12/23/1, komma → punt 17 in 12 documenten; BoundDay/
ContextValue 52/8; padlengtes 1/2/3: 5/18/1. U1: de emitter laat de twee
`COMPLEX`-verdelingen van de dump weg en schrijft er een WARNING-diagnostic
voor; de nieuwe dump is byte-identiek aan m4.py's migratie van een
meetkopie van de L3-dump zonder die twee verdelingen plus de twee
diagnostics (`toka/README.md`, divergentieregel L4 met de pointers
`/rules/verdelingen/3` en `/5` van de L3-dump); de 29 slices via m4.py.

**Regels.** WF-024 vervallen (geschiedenisregel; `COMPLEX` is een laadfout);
de WF-030-arm "at least two sources", de WF-037/047-facetarmen op de andere
knoop en de WF-020-arm "mixes a bare name with versioned entries" vervallen
(hun object is onschrijfbaar); WF-022 kreeg de N3-arm (een doelnaam die
attribuut én kenmerk is) en de ledensoortarm (U11: een versie zonder tag en
zonder initialisatie MOET een kenmerk schrijven, met tag of initialisatie
een attribuut — zonder die arm zette ref voor een Booleaan-attribuut zonder
tag stil een kenmerk); WF-114 nieuw (U21: de variant is die welke het type
noemt); WF-081 versmald tot de getalgrammatica van het casusschema (U19);
WF-081/084/095 sleutelen op de variant; WF-079's tekst herschreven naar wat
de check doet (twee onvoorwaardelijke rekenversies van verschillende vormen
met overlappende geldigheid); WF-098 gesloten op twee namen (`de
dag` als contextwaarde valt erbuiten); WF-020/021/107 op de gegroepeerde
vorm, WF-082 op `statuteAnnotations`, WF-028 op `path`, WF-029/054/030/034
op de L4-velden, WF-031 met `BoundDay`, WF-037/038/083/093/102/110/055/056
op `Aggregation`/`BuiltinCall`, WF-039 op `enumeration`/`number`; elke
melding op de L4-wirepointer (`/rules/<lijst>/<f>/versions/<v>/…`), 21
meldingen kregen L4-veldnamen. IR-SPEC ch. 3 en 3.1–3.6, 3.10, 4.1
(`Literal`, `ContextValue`, de nieuwe `BoundDay`-rij), 4.3, 4.5 (naar V13),
4.6 en de WF-index (WF-114-rij, WF-024-geschiedenisrij, WF-082-golden =
`jutterswet-hardheidsoverschot`); RUNTIME-SEMANTICS op de genoemde plekken;
D33 getoetst (RS 4.2: `orderingAttribute` is de sorteersleutel, ref en
clingo lezen het zo). De weigerzinnen letterlijk gelijk in verifier, ref,
clingo en Drools, ook de drie gedeelde (WF-083, WF-102 (b) en (c)) zonder
regelnummerstaart.

**Stempels (V14 (a)), lijst in `corpus/negatief/README.md` sectie "L4":**
5 verwijderd-met-reden (`wf024-complex`, `wf037-aantalgevulde-granulariteit`,
`wf047-bereik-op-builtin`, `wf034-sentinel-builtin-wratoperand` — m4.py
faalt erop —, `wf030-single-source-union` — de verifier aanvaardt de
vertaling), 1 met de hand (`wf034-typeloos-literal`), 112 herschreven (m)
met een L4-delta op het defect, 7 nieuw (`wf022-doel-attribuut-en-kenmerk`,
`wf022-kenmerkdoel-met-presentatie`, `wf022-attribuutdoel-zonder-
presentatie`, `wf114-literaalvariant-tegen-type`, `wf114-datumvariant-
tegen-precisie`, `wf098-de-dag-als-contextwaarde`, `wf081-decimale-komma`,
en uit de fixronde acht getuigen voor armen die er geen meer hadden:
`wf037-granulariteit-op-jaar-uit`, `wf037-tijdsevenredig-deel-zonder-
granulariteit`, `wf047-bereik-op-opgesomde-vouw`, `wf030-brontype-niet-
gedeclareerd`, `wf030-sleutel-geen-attribuut`, `wf037-jaar-uit-twee-
operanden`, `wf037-tijdsduur-een-operand`, `wf037-eerste-een-operand`; elk
arm-exact); negatief 407 → 417; `expect.txt` 165 → 228
(`EXPECTED_MIN_EXPECT` mee; 15 bestaande opnieuw gepind); vloer 350 blijft;
199 negatief-NOTE's en 3 golden-NOTE's opnieuw uit de echte
verifieruitvoer; L3-verifier op L3-corpus tegen L4-verifier op L4-corpus
(696 documenten): 388 byte-gelijk, 274 alleen de pointer, 29 melding in
tekst, 5 van regel gewisseld.

**Consumenten en emitters.** Gedeelde OCaml-lader op de L4-vorm met de
bijectie (uitvouwing tot `naam #i`; regelsoort uit vlag en tag; `fc_doel` uit
de vuller van `targetRole`; uniciteiten gesplitst op aantal bronnen) en elk
vervallen veld en elke vervallen knoop bij naam geweigerd (74 mutatieproeven;
92 laadrijen in de clingo-suite; `RegelEnExpressielaagWeigeringWitnessTest`
in Drools, 22 tests); ref en clingo lezen strata op de wirepointer, de
literaal uit zijn variant, `Bound_day`, en delen één getallezer met de
casusgrammatica; nulmeting: de uitvoer van ref, clingo en `ref --noop` over
619 golden-, 48 rood- en 29 toka-cases byte-gelijk tussen L3 en L4 op de
twee U24-regels na (`the Rekendatum context value`, opnieuw gepind in
`tijdsduur-reads-van-before-tot`); catala en flogic schrijven de L4-vorm
(proefdocument in de catala-suite); `IrEmitter` schrijft de L4-vorm
(versiegroepering met de regels van m4.py, de afbeelding van `soort`, de
variant uit het type, `BoundDay`/`ContextValue`, `path`, `uniciteiten`
samen, de U25-weigeringen, de `COMPLEX`-WARNING) en `IrEmitterTest` pint
wat geen golden draagt (43 → 54); de Drools-lader strikt op de L4-vorm met
de zinnen van de OCaml-lader, de ledensoortarm, de doelsoort uit
`targetRole`, de variant tegen het type en de volledige getalgrammatica
(vóór de landing las de Drools-lader `.5`, `5 €`, `-.5` en `3/04` stil
verkeerd), datum- en datumtijdlexemen en een granularity op een
niet-emmerende builtin (speelde stil), de onoplosbare `anchorRole` in de
WF-028-zinnen, en `Rekenjaar` geweigerd vóór het verlagen met dezelfde zin
als ref en clingo (Drools voerde het uit; `rsRekenjaar` uit de geporteerde
runtime); ref en clingo toetsen de Kwantorvouw (v0-velden tot L5) met de
WF-108-zinnen van de verifier in plaats van de Aggregation-zinnen die de
gedeelde bereikcode sinds L4 gaf, en delen de WF-028-zinnen; 73
rgs-goldens, 12 catala-, 2 flogic-goldens en de dump byte-identiek. Het
NOTE-proza van 110 goldens en getuigen beschrijft het eigen document in de
L4-namen, en de beslisnummers van de L2-kaart (146 in 78 getuigen-NOTE's)
zijn vervangen door wat ze aanduidden.

**Gate-scripts (per regel):** `check-ir-schema.sh` (`_l4_base`,
`L4_SHAPE_ROWS` 188 rijen; patroonrijen 26 → 77 voor `number`, `date`,
`dateTime` en het naampatroon; vormrijen 123 → 311); `check-verifier.sh`
(`EXPECTED_MIN_EXPECT` 165 → 228); `check-reftype-derivation.sh` (twister
op `Aggregation`/`BuiltinCall`/`kind`/`arguments`, `GEDRAGEN`, de
vrijstelling op een `BuiltinCall` met TIJDSDUUR; `EXPECTED_MIN_TWISTS` 38
blijft, 40 twists over 22 goldens gemeten); `check-drools.sh` en
`check-rood.sh` ongewijzigd (geen kanaalwissel; alleen de twee
U24-fragmenten in de case zelf).

**Lens:** eerste ronde op unie 1 (`e1f329ec`) EERST FIXEN met zeven blokkerende
vondsten en twaalf adviespunten: B1–B4 spec-tekst (een verkeerde
geschiedenisnoot, de WF-079-uitleg smaller dan de check, een onjuiste
WF-034-bewering, drie zinnen die een verdwenen wirevorm als geldend
beschreven), B5 twee bereikbare armen zonder getuige, B6 één weigerzin die
niet in alle lezers gelijk was (een getal buiten de grammatica: vier lezers,
vier reacties), B7 NOTE-proza in v0-namen zonder geschiedeniskader; de
fixronde op vier branches (`card/l4-fix-spec`, `card/l4-fix-ocaml`,
`card/l4-fix-java`, `card/l4-fix-notes`) sloot B1–B7 en A1–A9, A11 en A12
(A10, de oudere laderstijl van Drools, bewust als open punt), en de fixers
vonden zelf nog dezelfde klasse (drie WF-037-armen zonder getuige, datum- en
datumtijdlexemen, de Kwantorvouw die de Aggregation-zinnen kreeg, een
granularity die clingo en Drools stil negeerden, `Literal.waarde` dat geen
lezer las, `Rekenjaar` dat Drools uitvoerde, een onoplosbare `anchorRole`
die Drools laadde) en sloten dat in dezelfde ronde; de herkeuring door een
verse lens op unie 2 (`076d40a4`): EERST FIXEN op alleen tekst — alle
negentien vondsten in code en data verwerkt en nagemeten (acht armmutanten
over 736 documenten, elk raakt precies de eigen getuige; veertien
lexeemproeven met zin en pointer gelijk in ref, clingo en Drools; de
nulmeting over 696 cases byte-gelijk tussen unie 1 en unie 2; de
hermigratie van de basis byte-gelijk op de bekende uitzonderingen na), met
drie blokkerende tekstpunten (één NOTE en vier IR-SPEC-passages beschreven
het uitvoerdergedrag van vóór de fixronde; vier NOTE's idem) en drie
adviespunten, alle in de adviesronde door de hoofdsessie afgehandeld zonder
derde lens (gate 1 en 3 opnieuw groen); proeven in
`/tmp/lens-l4-meet/` en `/tmp/lens-l4-meet/herkeuring/`.

**Gates op de landing (alle veertien, elk los):** 1 PASS; 2 PASS 289 bestanden, 77 + 311 zelftestrijen; 3 PASS 289 schoon / 417 negatiefs / 228 armfragmenten; 4 PASS 667 case-files; 5 PASS ref-suite 976 / 619 replays; 6 PASS suite 1284 / 619; 7 PASS 582 + 37; 8 PASS 48 / 96 (Drools-poot 11 / 37); 9 PASS 216 / 12; 10 PASS 570 tests / 73 goldens; 11 PASS 45 fragmenten, 27 tests, 29 slices, replay 29/29/29; 12 PASS played 188, DRL 167, JUnit 748; 13 PASS 40 twists / 22 goldens, 100 replays, DRL 21; 14 PASS 135 / 2. Gemeten op `076d40a4`; de tekstcommit `2c38f4a0` erbovenop raakt alleen IR-SPEC en NOTE's, gate 1 en 3 daarop opnieuw groen.

**Open punten (genummerd, hoofdsessie weegt ze bij L5):**
1. U13/U15 zijn uitgevoerd als leesstand van de gedeelde lader (correctie op
   de kaart, zie "Correcties"): de verifier laadt en oordeelt, een uitvoerder
   weigert bij het laden met dezelfde kernzin.
2. B24 (de reden bij `Onopgelost`), B25 (de knoopvorm van `Regelversie`, tot
   de paritygolden) en B28 (de startpuntregistratie) landen later; de
   gescoopte unie blijft een ontwerpvraag (schema en laders weigeren haar).
3. `Rekenjaar`: Drools voert het uit (het jaar van de rekendatum), ref
   weigert het bij naam, clingo vertaalt het niet — bestond vóór L4, 0
   corpuscases; een eigen kaart of een normvraag.
4. Clingo weigert een uniciteit op `Tekst`-sleutels ("unscalable
   datatype") — bestond vóór L4 (gemeten op een proefdocument van C2).
5. Twee javadoc-regels in de recordlaag van de frontend (`ir/Expr.java`,
   `model/RegelSpraakModel.java`) noemen v0-wirenamen; de records bewegen
   niet tot de pin-bump van de toolchain (D6).
6. De NOTE van `jutterswet-vaarwaterspelling` en de leeshelft van
   `jutterswet-dossierspelling` beschrijven een referentietype dat bij L2 van
   de wire ging; of die goldens de ci-leesarm van WF-089/WF-090 nog pinnen,
   is een meting (sinds L2 open).
7. De negen getuigen `wf055-*` en `wf056-*` hebben nooit een NOTE gehad (wel
   `expect.txt`); WF-021 citeert versies bij hun in-memory naam (`… #1`).
8. `Ast.Literal.waarde` (het v0-lexeem) wordt door ref, clingo, catala en
   flogic niet meer gelezen, alleen door de verifier; het blijft de
   in-memory vorm (U13). Ref faalt op twee vormen met dezelfde wirepointer
   (alleen bereikbaar met handgebouwde documenten).
9. Testwijzigingen (gemeld, onder kaartmandaat): ref 955 → 976, clingo
   1129 → 1284, catala 207 → 216, flogic 135, frontend JUnit 559 → 570,
   Drools JUnit 717 → 748 (`OnbekendVeldWitnessTest` knoopsoortvloer 59,
   gemeten 64; herschreven, niet verwijderd: een onbekende `kind` is een
   documentfout, een onoplosbare `targetRole` wordt geweigerd, de zeven
   datumlexeemrijen pinnen de laadweigering).
11. Eigen toetsen van ref en clingo op handgebouwde of ongeverifieerde
    documenten, van vóór L4 en niet in deze kaart gelijkgetrokken: de
    zinnen van ref's `bound_role` voor andere rolbindingen dan WF-028; de
    volgorde van de toetsen in clingo's `source_join`; de vijfde WF-108-arm
    (`elementObjecttype`) met eigen zinnen; een drempel onder ALLE of GEEN
    die ref en clingo stil negeren waar de verifier WF-108 meldt; een
    emmerende builtin zonder granularity die op de bodemzin valt waar de
    verifier WF-037 meldt — kandidaatkaart: één gedeelde vormtoets in de
    lader.
12. De Drools-lader: de oudere stijlverschillen (`unknown field on <Knoop>`
    op de veldpointer, `null` op een optioneel veld, `expected a JSON
    object`; de WF-028-pointer op het veld waar de verifier de versie
    noemt), twee WF-028-armen (rollen verschillen; ankertype onder de vuller)
    en de WF-037-arm "bucketing builtin without a granularity" — van vóór L4,
    eigen kaart; wie de transpilers rechtstreeks aanroept krijgt in een
    consistentietoets nog de algemene scopezin (elke productie-ingang gaat
    langs de weigering vóór het verlagen).
13. `jutterswet-vaarwaterspelling` bewijst sinds L2 de hoofdletter-
    ongevoeligheid van WF-089 niet meer (`VAARWATER` staat alleen in `bron`);
    meting en eventueel een andere drager.
14. De Drools-lader draagt geen WF-030-arm: op een niet-gedeclareerd
    brontype crasht de DRL-tekst-tak en op een sleutel die geen attribuut is
    faalt hij bij het compileren (de exec-tak sluit uit of faalt bij het
    rekenen) — dezelfde klasse als punt 12. Een emmerende builtin binnen een
    expressie op een tijdsafhankelijk doel komt door de verifier, en ref,
    clingo en Drools weigeren dan met drie verschillende zinnen (bestond op
    unie 1 al; de benoemde clingo-arm uit de fixronde wordt op zo'n doel niet
    bereikt).
10. Stap (k), de herpin van `regelspraak-java`: uitgesteld tot de nieuwe
    toolchain onder uchar (maintainerbesluit 2026-09-24); A.7 noemt 7
    zekere en 1 mogelijke `IrEmitterTest`-kanteling en 21 recordbestanden
    die bij ongewijzigde records niet bewegen.
