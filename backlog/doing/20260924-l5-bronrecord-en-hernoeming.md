# L5 — bronrecord en hernoeming (M7 + M8: B26, B6, B10): de vijfde en laatste klapkaart van v1

Vijfde en laatste kaart van fase 2 van `spec/proposals/V1-WIREMIGRATIE.md` (§7, de
landingstabel: L5 = M7 + M8), na L1 (`backlog/done/20260923-m1-schema-en-container.md`,
`32185b7b`), L2 (`backlog/done/20260923-l2-referentielaag.md`, `fd3f0a73`), L3
(`backlog/done/20260924-l3-typesysteem.md`, `4439d802`) en L4
(`backlog/done/20260924-l4-regel-en-expressielaag.md`, geland `1cdcd9fc`, registerwerk
`1edd17e4`; deze kaart bouwt op main `1edd17e4`). L5 doet drie dingen, in deze volgorde:

- **M7 — het bronrecord (B26), als nulmeting en EERSTE inhoudelijke commit.** Elke
  `bron`-string wordt mechanisch `"source": {"snippet": …}` (D44, ook op de
  defaultslaag); `bron: null` wordt afwezig (D42); het v0-veld `line` op de
  consistentieversie en de regelgroep gaat op in `source.line` waar een `source` wordt
  geschreven en vervalt waar `bron` null was (D43); `source` is verplicht op een
  `Onopgelost` en WF-113 (b) zoekt `source.snippet` (D46). Verder beweegt in M7 niets. M7
  krijgt een eigen gate-run waarin gates 5, 6 en 12 niet mogen bewegen: zo wordt de
  aanname "geen uitvoerder leest `bron`" gemeten op een commit waarop niets anders
  tegelijk verandert (V1-WIREMIGRATIE §7, "M7 is opzettelijk een eigen kaart").
- **M8 — de hernoeming (B6, B10).** Elke knoopnaam en elke sleutel die L1–L4 in
  v0-spelling lieten staan, krijgt zijn IR-SPEC-naam (D1): de modellaag (`Text`,
  `Boolean`, `Enumeration`, `FactType`, `Role`, `Dimension`, `Timeline`,
  `RoleBinding`), de regellaag (`Validity`, `MemberInit`, `DayKindDefinition`,
  `StartPointDetermination`, `RuleGroup`, de sleutels van de defaultslaag), de
  expressie- en conditielaag (`Binary`, `Rounding`, `Clamp`, `TextComposition`,
  `LiteralChunk`, `EmbeddedExpression`, `AttributeReference`, `DimensionCellRead`,
  `ParameterReference`, `VariableReference`, `Unresolved`, `DimensionSelection`,
  `Comparison`, `Predicate`, `CharacteristicCheck`, `TimeWindow`, `Compound`,
  `QuantifierFold`, `WholePeriod`); de lijsten onder `rules` (D51); `eigenaar` wordt
  `host` (B10, D48), `objecttype` op de attribuutreferentie `subjectType` (D49), de
  twee velden van `TemporalRead` `base` en `date` (D50); en ELKE enumwaarde gaat naar
  de ene spellingsregel van N2 (Engels, lowerCamel), via een expliciete vertaaltabel in
  emitters en laders die als sectie `waarden` in de naamtabel staat (D47).
- **null → afwezig op de hernoemde knopen (D5 (a)).** Een veld dat IR-SPEC optioneel
  noemt, gaat van v0-`null` naar afwezig (18 posities, 4575 waarden, gemeten); een veld
  waarvan een WF-regel de aanwezigheid bewaakt, blijft nullable (D5 (b)).

Wat na L5 klaar is: de wire `uchar-ir-v1` is compleet. Op de wire staat geen
v0-knoopnaam, geen v0-sleutel en geen v0-enumwaarde meer, met één uitzondering bij
besluit: de knoop `Regelversie` met zijn velden `regel` en `inconsistent` houdt zijn
v0-vorm (D39; 0 dragers, want de paritygolden van V13 bestaat niet). Elke brontekst
staat in het bronrecord. Fase 2 van V1-WIREMIGRATIE is daarmee afgesloten; de ene
pin-bump van de toolchain (fase 3, maintainerbesluit 2026-09-24) wordt opeisbaar, maar
hoort niet bij deze kaart.

Wat NIET meegaat, met de reden:

- **B24** (de reden bij `Unresolved`): er ligt geen kaart die de taxonomie vastlegt;
  `Onopgelost` wordt `Unresolved` zonder `reason`, de reden landt later additief (D38).
- **B25** (`RuleStatusCheck`): de paritygolden van V13 bestaat niet, dus `Regelversie`
  houdt zijn v0-vorm; alleen zijn `bron` wordt `source`, omdat B26 dwars door alle lagen
  gaat (D39; zie U12).
- **`file` en `column` in het bronrecord, en de positieconventies**: geen emitter
  schrijft ze in L5; de conventies (basis van `line` en `column`, vorm van `file`) en de
  definitie van `snippet` naast een positie horen bij de kaart die als eerste posities
  schrijft (D45). Het schema laat de twee velden tot dan niet toe.
- **`statuteAnnotations` op de defaultslaag**: de wetsverwijzingen in `Default.bron`,
  `PrioritizedDefaults.bron` en `ExceptionGroup.bron` worden mechanisch `snippet`; een
  eigen veld landt later additief (D44).
- **De in-memory hernoeming**: de Java-records van `frontends/regelspraak` en de
  OCaml-AST houden hun v0-vorm; alleen de wirelaag verandert (D52, D6).
- **Een canonicalisatiepas** (D56): gate 4 en het casusformaat bewegen niet.
- **De herpin van `regelspraak-java`**: geen tussentijdse herpins; één pin-bump als v1
  compleet is (maintainerbesluit 2026-09-24). Acceptatie (k) vervalt, zoals in L4.
- **P11**: de `toka/`-zone blijft tot de publieke flip (PLAN §9 (10), (11)).
- **Aanscherping van v0-nullable velden die IR-SPEC verplicht noemt** (0 null-dragers)
  en een voorwaardelijke aanwezigheid van `Compound.threshold` en `Predicate.digits`: die
  velden worden hernoemd maar houden hun L4-nullability en hun L4-aanwezigheid; open punt
  (U18, U19).
- **De open punten 3, 4, 6, 7 en 11–14 van L4**: gedrag of getuigendekking, geen naam;
  L5 verandert geen betekenis.

Byte-voor-byte L4 blijven (per knoop: zijn eigen sleutels en waarden; een kindknoop volgt
zijn eigen rij in de hernoemingstabel), op de sleutels `bron` en `line` na (die in M7
opgaan in `source`): de L1–L4-knopen van de modellaag (`ObjectType`, `Attribute`,
`Characteristic`, `TimelineFacet`, `Domain`, `Parameter`, `UnitSystem`, `Unit`, `DayKind`,
`Number`, `Date`, `SumType`, `ListType`, `StructType`) op hun enumwaarden na; de zes
benoemde vormen en hun versierecords, `Source`, `Aggregation`, `BuiltinCall`, `Literal`,
`ContextValue`, `BoundDay` en `RoleMembership` op hun enumwaarden na; de kernelknopen van
5b/5c (`Construct`, `Match`, `BinderRef`, `StructLiteral`, `FieldAccess`, `Lambda`,
`ListLiteral`, `IfThenElse`, `Invoke`, `Fold`, `MapList`, `FilterList`, `ListLength`,
`Let`, `InstanceSetBranch`, `InstanceSetUnion`, `FunctionDef`) inclusief hun nullability;
`format`; `diagnostics` op de waarde van `severity` na; de varianten van `Literal.value`;
`Date.precision` en `Number.presentation` (al Engels); en alle casusbestanden (gate 4).

Goal: één ff-landing waarna alle veertien gates groen zijn op de L5-vorm, alle
documenten in de vier corpuszones en de pariteitszone in die vorm staan (stand main
`1edd17e4`, L4 geland, gemeten 2026-09-24: golden 242 (anchor 40, overig 202),
negatief 417, rood 47, toka 30 = 736; in M7 veranderen 678 documenten — 58 dragen geen
`bron`-sleutel, a/g/r/n/t = 0/10/1/40/7 —, in M8 verandert elk document, want elk
draagt de v0-lijstsleutels onder `rules`), de drie emitters de L5-vorm schrijven, de
vier consumenten hem lezen, de toka-zone meeverhuist (gate 11 in regressievorm;
README-divergentieregel L5), en de eerste inhoudelijke commit van de kaart M7 is,
met een vastgelegde nulmeting. Gates 5, 6, 7, 8, 12 en 13 mogen geen uitkomst bewegen:
L5 verandert geen betekenis, en elke afwijking is een nulmeting.

## Besluiten die deze kaart uitvoert (niet heropenen)

Uit `research/v1-klap/BESLUITENREGISTER-L2-L5.md` §4 (D41–D52), de kruisende besluiten van
§1, de L4-besluiten die naar L5 verwijzen (D38–D40), §5 (D53–D56, alle vier eerder
uitgevoerd) en de normvraag N2. Eén regel per besluit, als opdracht; bij twijfel wint de
registertekst, die per besluit zijn grond noemt.

Kruisend (zie de L2-kaart voor de volledige tekst):

- **D1** Herstructureren = IR-SPEC-naam voor knoop en eigen sleutels. L5 is "al het
  overige": elke knoop en elke sleutel die L1–L4 in v0-spelling lieten, krijgt zijn
  IR-SPEC-naam, met de lijsten onder `rules` (D51). De tabel in "De L5-vorm van de wire"
  is de volledige lijst.
- **D2** `bron` bleef `bron` tot L5; in M7 wordt het op ELKE knoop `source`, ook op de
  knopen die L2–L4 al een IR-SPEC-naam gaven.
- **D3** Elke knoop draagt `node`, behalve de versierecords van de zes benoemde vormen, de
  getypeerde literaalwaarde, de exacte factor en het bronrecord (D41). De nieuwe
  L5-knopen dragen `node`: `WholePeriod`, `LiteralChunk`, `EmbeddedExpression`,
  `DimensionSelection`, `DayKindDefinition`, `StartPointDetermination`, `MemberInit`,
  `Validity` en de rest van de hernoemingstabel.
- **D4** Enumwaarden hielden hun v0-spelling tot L5; L5 spelt ze allemaal volgens N2
  (D47).
- **D5** (a) Een veld dat IR-SPEC optioneel noemt: v0-`null` → afwezig, op elke knoop die
  L5 hernoemt; (b) een veld waarvan een WF-regel de aanwezigheid bewaakt, blijft
  nullable; (d) elke v0-waarde die de L5-vorm niet kan dragen, laat `m5.py` luid falen
  met bestand en JSON-pointer.
- **D6 / D52** De Java-records en de OCaml-AST houden hun v0-vorm; alleen de wirelaag
  (schema, laders, emitters) en de meldingen die de wire beschrijven, veranderen. De
  Drools-weigeringsteksten die in-memory `bron` citeren, veranderen niet (de snippet is
  de v0-string letterlijk), zodat de nulmeting van M7 zuiver blijft.
- **D20** `EXPECTED_MIN_NEGATIEF` blijft 350; de stand wordt gemeld.
- **D21** Gate 13 volgt: de twister sleutelt op v0-knoopnamen en v0-waarden
  (`Binair`, `Afgerond`, `Begrensd`, `PLUS`/`MIN`/`VERMINDERD_MET`/`MAAL`, de
  aggregatiesoorten, `TIJDSDUUR`) en moet in L5 op de L5-namen;
  `EXPECTED_MIN_TWISTS` blijft tenzij de gemeten stand eronder komt.

Uit L4, voor L5:

- **D38** `Onopgelost` wordt in L5 `Unresolved`, zonder `reason`; B24 landt later
  additief.
- **D39** De v1-knoopvorm `RuleStatusCheck` wacht op de paritygolden; die ligt er niet,
  dus `Regelversie` houdt in L5 zijn v0-vorm (0 dragers).
- **D40** De startpuntregistratie blijft een regellaagvorm en wordt in L5 hernoemd naar
  de vorm van IR-SPEC 3.8: `StartPointDetermination {timeline, objectType, member}`.

Voor L5 (register §4):

- **D41** Het record staat onder de sleutel `source`, draagt geen `node` en krijgt geen
  knoopnaam op de wire; IR-SPEC noemt het "the structured source reference" (ch. 4).
- **D42** `source` is optioneel: bij v0-`null` afwezig; is het aanwezig, dan is `snippet`
  aanwezig en niet leeg. De zin "Every node carries a source reference" in IR-SPEC ch. 4
  wordt "may carry", met `Unresolved` als uitzondering (D46).
- **D43** `line` gaat op in `source.line` waar een `source` wordt geschreven; waar `bron`
  null was, vervalt `line`, en de landing meldt het aantal.
- **D44** Elke knoop met een `bron`-string, de drie knopen van de defaultslaag inbegrepen,
  krijgt mechanisch `{"snippet": …}`; `statuteAnnotations` op de defaultslaag kan later
  additief landen.
- **D45** `snippet` is de v0-`bron`-string letterlijk; geen emitter schrijft `file` of
  `column`, en `line` alleen volgens D43; de positieconventies horen bij de kaart die als
  eerste posities schrijft.
- **D46** WF-113 (b) zoekt `source.snippet`; `source` is verplicht op `Unresolved`; een
  literaal draagt zijn juridische spelling in `source` waar v0 een `bron` had, en de
  literalen zonder `bron` blijven zonder `source` (het register telde 123, gemeten op
  `1edd17e4`: 125, a/g/r/n/t = 7/4/14/100/0).
- **D47** Elke enumwaarde gaat naar haar IR-SPEC-spelling via een expliciete vertaaltabel
  in emitters en laders, vastgelegd als sectie `waarden` van de naamtabel met een toets;
  welke spelling IR-SPEC draagt, beslist N2.
- **D48** `eigenaar` wordt `host?`, eerst opgenomen in de tabel van `CharacteristicCheck`
  in IR-SPEC 4.5; de drie verplichtingen WF-034 (q), WF-100 en WF-109 gaan mee.
- **D49** `objecttype` op de attribuutreferentie wordt `subjectType`, met de betekenis die
  IR-SPEC 4.1a geeft: het eindpunt waar het attribuut woont.
- **D50** `TemporalRead` krijgt `base` en `date`; tabel 4.3a wordt gecorrigeerd.
- **D51** `WholePeriod {granularity, negated}` met `granularity` ∈ `year`/`month`,
  `LiteralChunk {text}`, `EmbeddedExpression {expr}`, `DimensionSelection {dimension,
  labels}`, `DayKindDefinition` en `StartPointDetermination`; de lijsten onder `rules`
  worden `rules`, `dayKindDefinitions`, `objectCreations`, `allocations`,
  `consistencies`, `uniquenesses`, `factCreations`, `startPointDeterminations` en
  `ruleGroups` (het pad `rules.rules` inbegrepen), met `prioritizedDefaults` ongewijzigd;
  alles eerst in IR-SPEC.
- **D52** L5 hernoemt de wire en niet het in-memory model; een hernoeming van de typen is,
  als ze gewenst is, een eigen kaart na de klap.

Register §5 — vier herstructureringen zonder B-post, alle vier eerder uitgevoerd en hier
niet heropend: **D53** (`Kenmerk.navigatie` verviel in L2; `navigatieBinding` draagt de
hop en wordt in L5 `navigation`), **D54** (`UnitSystem` sinds L3), **D55** (`DayKind` sinds
L3), **D56** (geen canonicalisatiepas; gate 4 en het casusformaat bewegen niet). De zes
namen zonder IR-SPEC-naam die de opdracht bij "§5" noemde, staan in **D51**, niet in §5.

Normvraag:

- **N2** (standaardantwoord JA geldt): IR-SPEC legt vóór de hernoeming één spellingsregel
  voor alle enumwaarden vast — Engels, lowerCamel (`nonNegative`, `equalShares`,
  `atLeast`, `exactDigits`, `filledCount`) — en trekt zijn bestaande spellingen daarop
  gelijk, ook de Nederlandse van `Binary.op` en de builtins. In deze kaart betekent
  "vóór L5" dat A2 de regel en alle L5-spellingen in zijn eerste fase in IR-SPEC schrijft,
  vóór de naamtabelcitaten van A1 en vóór de meldingen van A2's tweede fase (volgorde in
  de werkverdeling). De tabel per familie staat in "De L5-vorm van de wire".
- **Naamprincipe voor de defaultslaag** (L4, standaardantwoord van de hoofdsessie): in L5
  krijgen `PrioritizedDefaults`, `Default` en `ExceptionGroup` hun namen uit IR-SPEC 6.1
  (`name`, `subject`, `characteristic`, `attribute`, `unordered`, `validity`,
  `decision`, `justification`, `consequence`, `initialisation`, `exceptions`,
  `members`); de knoopnamen zijn al Engels.

## Uitvoeringsbesluiten van de hoofdsessie (U1–U40)

Genomen op 2026-09-24 uit de beslispunten van het snijverslag, met dezelfde
nummering (BP-nn → Unn; het verslag zelf blijft buiten de repo; de meting die
een besluit draagt staat hier in één zin). Waar de kaarttekst "Un" zegt, is dit
de plek; waar de werkverdeling of de acceptatie van een U-besluit afwijkt, geldt
het U-besluit. Drie normvragen zijn met hun standaardantwoord aan de maintainer
voorgelegd en gelden zo tot hij anders zegt: NV-1 de Engelse woorden van U24–U28
en U30; NV-2 null → afwezig (U17); NV-3 `Regelversie` met `source` (U12). Het
mandaat van U38 (Part I van `spec/ASP-TRANSLATION.md`) is hem op dezelfde
manier voorgelegd.

Werkvorm:

- **U1 — M7 is een commitreeks, geen samengeperste commit.** M7 is alles wat
  M7-O en M7-J tussen de claimcommit en de M7-unie committen; de nulmeting en de
  kop "M7 — de nulmeting" noemen de hash van die unie. Niet gekozen: samenpersen
  tot één commit — dat verliest de bouwcommits en wint niets, want de meting
  hangt aan de unie-hash. "Eerste commit" in V1-WIREMIGRATIE §7 en in deze kaart
  betekent: de eerste inhoudelijke stap van de kaart, vóór elke M8-wijziging op
  de kaartbranch.
- **U2 — de nulmeting meet de hoofdsessie zelf**, met een script buiten de
  repository (`/tmp/l5/nulmeting.sh <checkout> <uitvoermap>`): één keer vóór M7
  op main `1edd17e4` (parallel met M7-O), één keer na M7 op de M7-unie; de
  vergelijking is `diff -r` over padonafhankelijke uitvoer; toegestane
  verschillen zijn alleen verifiermeldingen die `bron` noemden, per regel
  genoemd. Bouwers meten de nulmeting niet zelf; zij melden wel elke beweging
  in hun eigen gates (les (d)).
- **U3 — volgorde, met meer parallellie dan de werkverdeling noemt.** M7-O
  werkt in twee fasen: fase 1 = schema, `m5.py --stage m7`, alle documenten van
  de vier zones én de toka-zone (dump en slices, mechanisch), de twee
  handgetuigen, de M7-rijen van de naamtabel en van gate 2, met gates 2 en 4
  groen; fase 2 = de OCaml-lader, `checks.ml`, de clingo- en ref-tests, de
  catala- en flogic-emitters. M7-J start vanaf de fase-1-commit, parallel met
  fase 2, en MEET de toka-dump (de her-emissie door de frontend moet bytegelijk
  zijn aan de mechanische migratie; alleen bij een verschil schrijft hij en
  meldt het). A2 fase 1 (alleen `spec/IR-SPEC.md`) loopt parallel met M7 op een
  eigen branch vanaf de claimcommit en wordt op de M7-unie gerebaset. A1 start
  vanaf de M7-unie plus A2 fase 1 zodra gates 2, 3 en 4 groen zijn en het
  OCaml-deel van de nulmeting (ref, clingo, `--noop`, verifier, rood) bytegelijk
  is; het Drools-deel mag naast A1 aflopen, en een verschil daar stopt de
  M8-unie tot de hoofdsessie beslist. Stap 4–6 zoals de werkverdeling.
- **U4 — N vanaf de `checks.ml`-commit van A2 fase 2**, met de armmeting van A2
  als werklijst; mag splitsen in N1 (negatief) en N2 (golden en rood), disjunct
  per zone; in de fixronde komt er een aparte NOTE-fixer.
- **U5 — één vertaaltabel per taal.** OCaml: één module in de gedeelde
  bibliotheek (`spec/verifier/lib/spelling.ml`), gebruikt door de lader (dus
  verifier, ref en clingo) en door de catala- en flogic-emitter; Java: één
  klasse in de gedeelde recordmap van de frontend
  (`frontends/regelspraak/src/main/java/.../ir/`), gebruikt door `IrEmitter` en
  de Drools-lader; beide getoetst tegen de sectie `waarden` van de naamtabel.
  Gemeten: 16 `.name()`-regels in `IrEmitter`, `enumOf`/`valueOf` in
  `IrLoader`, tabellen per enum in `loader.ml`.
- **U6 — de sectie `waarden` van de naamtabel**: per familie één rij
  `{familie, plaatsen, veld, sectie, bewijs, rijen: [[v0, L5], …], geweigerd:
  [[v0, L5], …]}`, en toetspunt 9: elke `enum` van het schema hoort bij precies
  één familie; de L5-waarden van een familie zijn exact de schema-enum, plus de
  bij naam geweigerde waarden apart; elke L5-waarde staat in backticks in de
  genoemde IR-SPEC-sectie; geen v0-waarde staat nog in het schema; de afbeelding
  is bijectief.
- **U7 — M7 begint direct na de claimcommit** (de kaart in `backlog/doing` is
  de eerste commit van de kaartbranch; WAY-OF-WORKING §2), zie U1.

Het bronrecord:

- **U8 — de schemavorm**: `$defs/sourceReference` = `{snippet: string
  (minLength 1), line?: integer (minimum 1)}` met `additionalProperties:
  false`; `file` en `column` laat het schema niet toe tot de kaart die als
  eerste posities schrijft (een later optioneel veld is additief).
- **U9 — plaats**: `source` staat waar `bron` stond; in het record `snippet`
  vóór `line`.
- **U10 — de lege snippet** is een laadweigering in elke lezer, ook de verifier,
  zonder WF-nummer (onschrijfbaar, M1-precedent): «a source snippet is never
  empty: a node without source text carries no source»;
  `wf113-marker-bron-leeg` wordt met de hand een snippet van alleen witruimte,
  zodat de blanco-arm van WF-113 (b) zijn getuige houdt. Gemeten: 1 lege `bron`
  in 736 documenten.
- **U11 — de marker zonder `source`**: `source` is schemaverplicht op
  `Unresolved`; de uitvoerders weigeren een marker zonder `source` bij het
  laden; de verifier-lader laadt hem in zijn leesstand `Verifier` en WF-113 (b)
  weigert hem met de zin van de null-arm; `wf113-marker-bron-null` blijft, met
  de hand geschreven.
- **U12 — `Regelversie`** (NV-3): de knoop en zijn velden `regel` en
  `inconsistent` blijven v0 (D39); `bron` wordt ook daar `source`, zodat na L5
  nergens een `bron`-sleutel op de wire staat. 0 dragers.
- **U13 — catala schrijft geen `line`**: zijn `cons_line` is de volgorde van de
  assertie in de scope, geen regel van een bronbestand; 0 catala-goldens dragen
  een consistentie, gate 9 beweegt niet.
- **U14 — `source.line` ≥ 1**, alleen op de consistentieversie en de regelgroep;
  een 0 in het geheugen (de Java-`Bron` van een synthetische knoop) is afwezig
  op de wire. Gemeten: geen v0-`line` op een drager is 0.
- **U15 — diagnostics**: `message` en `line` blijven zoals ze zijn (ook de
  Nederlandse meldingtekst die de RegelSpraak-frontend in een WARNING schrijft:
  producentproza); alleen `severity` krijgt de L5-spelling.

De hernoeming:

- **U16 — sleutelvolgorde**: voor elke hernoemde knoop de L4-volgorde met de
  sleutels ter plaatse hernoemd, ook waar een IR-SPEC-tabel anders ordent (zoals
  `attribute, subjectType, path, source` in 4.1): een herschikking is een
  wijziging zonder betekenis die elke bytevergelijking laat bewegen.
- **U17 — null → afwezig** (NV-2): op de 18 posities van de tabel (4575
  waarden), met `Predicate.dayKind` en `Default.justification` erbij; het schema
  eist bij `TimeWindow` geen van beide grenzen, omdat `wf042-empty-window`
  anders onschrijfbaar wordt.
- **U18 — geen aanscherping**: een v0-nullable veld dat IR-SPEC verplicht noemt
  en dat geen WF-regel bewaakt, blijft nullable (0 null-dragers), ook `null` in
  de enum van `Timeline.granularity`; de lijst gaat als open punt naar het
  Result.
- **U19 — `Compound.threshold` en `Predicate.digits`** blijven altijd
  geschreven: `jutterswet-tegenwacht` draagt `drempel: 1` onder `ALLE`; een
  voorwaardelijke aanwezigheid is een herstructurering met een eigen meting.
- **U20 — nieuwe getuige** `wf098-v0-naam-rekendatum` met `ContextValue {name:
  "Rekendatum"}` (precedent: `wf098-de-dag-als-contextwaarde` in L4).
- **U21 — `wf098-contextwaarde-naam-kapitalisatie`** draagt na L5 met de hand
  `EvaluationDate`: de herkapitalisering van de L5-naam, zodat de getuige blijft
  pinnen dat WF-098 bytegelijk kruist.
- **U22 — bij naam geweigerde waarden** krijgen ook de L5-spelling en staan als
  "geweigerd" in de tabel: `error` en `info` als severity, `the` als kwantor van
  een `QuantifierFold`; `m5.py` beeldt `ERROR`, `INFO` en `DE` erop af; de
  verifier-lader leest ze in zijn leesstand, de uitvoerders weigeren ze bij het
  laden.
- **U23 — namen**: de kaart `backlog/doing/20260924-l5-bronrecord-en-hernoeming.md`,
  kaartbranch `card/l5-bronrecord-en-hernoeming` in `wt/l5`; bouwers
  `card/l5-m7-o`, `card/l5-m7-j`, `card/l5-a1`, `card/l5-a2`, `card/l5-b1`,
  `card/l5-b2`, `card/l5-c1`, `card/l5-c2`, `card/l5-n`, elk in `wt/l5-<bouwer>`;
  `spec/proposals/v1-migratie/m5.py` met `--stage m7`; de naamtabel `geland:
  "M7"` (B26) en `"M8"` (B6, B10, D47–D51).

De enumwaarden (NV-1, standaardantwoord; de volledige tabel staat in "De
L5-vorm van de wire"):

- **U24 — `Comparison.op`**: `less`, `lessOrEqual`, `greater`, `greaterOrEqual`,
  `equal`, `notEqual`, `earlier`, `earlierOrEqual`, `later`, `laterOrEqual`
  (de datumordening houdt eigen woorden, zoals v0).
- **U25 — `Binary.op`**: `plus`, `minus`, `reducedBy`, `times`, `dividedBy`,
  `dividedByAbs`, `power`, `percentageOf` (`Abs` blijft de naam van de
  dialectvariant).
- **U26 — `Rounding.mode`**: `floor`, `ceiling`, `towardZero`, `awayFromZero`,
  `halfAwayFromZero` (naar de betekenissen van RUNTIME-SEMANTICS 2.5; "down" en
  "up" laten open of het om −∞ of om nul gaat).
- **U27 — `BuiltinCall.kind`**: `squareRoot`, `absoluteValue`, `concatenation`,
  `duration`, `absoluteDuration`, `easterSunday`, `timeProportionalPart`,
  `dayCount`, `yearOf`, `monthOf`, `dayOf`, `hourOf`, `minuteOf`, `secondOf`,
  `millisecondOf`, `dateWith`, `dateTimeWith`.
- **U28 — `Predicate.kind`**: `empty`, `filled`, `elevenTest`, `exactDigits`,
  `dayKind`. Wil de maintainer de vakterm houden, dan wordt alleen die rij
  `elfproef` (één woord, al lowerCamel); 0 dragers.
- **U29 — `ContextValue.name`**: `evaluationDate`, `evaluationYear`.
- **U30 — `Diagnostic.severity`**: `warning`; `error` en `info` bij naam
  geweigerd (U22); `null` blijft `null` (1 getuige).
- **U31 — de regeltekst in IR-SPEC** (Conventions, bindend): «Enumeration
  values on the wire are English identifiers in lowerCamel case: one word in
  lower case (`sum`, `year`), several words joined with each further word
  capitalised (`nonNegative`, `equalShares`, `filledCount`); no upper-case
  start, no hyphen, no space, no Dutch. The case format spells its kinds the
  same way (`attributeValue`, `characteristicPresent`). Each family's table
  lists its v0 values.» — en elke familietabel (2.2, 2.4, 2.8, 3.1, 3.2, 4.1,
  4.2, 4.3, 4.5, ch. 2 voor severity) op die spelling, met de L5-waarden in
  backticks (de naamtabeltoets leest ze, U6).

Meldingen, pointers en gates:

- **U32 — de meldingsregel**: een melding die een wireconstructie noemt, noemt
  haar in haar L5-spelling (knoopnaam, sleutel, enumwaarde); Nederlandse woorden
  van het dialect in kleine letters ("som and totaal", "gedurende", "verminderd
  met") blijven; in-memory namen alleen waar de tekst het geheugen beschrijft.
- **U33 — "soort mismatch"**: het woord "soort" in de WF-091-meldingen blijft;
  het is de term van IR-SPEC voor de datatypefamilie (ch. 4.1), geen
  wiresleutel.
- **U34 — gate 8**: de 11 fragmenten die een v0-naam pinnen worden opnieuw
  gepind uit de echte uitvoer (B1); in `check-rood.sh` verandert alleen de
  regel van een bodemzin waarvan B1 de tekst verandert (verwacht één: de
  `dt_term`-zin van clingo noemt `VariabeleRef`), per regel in het Result.
- **U35 — gate 12**: `scripts/check-drools.sh` verandert niet tenzij een
  kanaalwissel gemeten wordt; de redenteksten in `EXPECTED_CHANNEL` noemen
  in-memory recordnamen en blijven (D52).
- **U36 — gate 13**: de twister sleutelt op de L5-namen van acceptatie (i);
  `EXPECTED_MIN_TWISTS` 38 en `EXPECTED_MIN_DRL_PLAYED` 20 blijven.
- **U37 — vloeren**: `EXPECTED_MIN_NEGATIEF` 350 blijft (stand 417 → 418);
  `EXPECTED_MIN_EXPECT` 228 → de gemeten stand (verwacht 260); alle andere
  vloeren blijven.

Buiten L5 en registerwerk:

- **U38 — `spec/ASP-TRANSLATION.md`**: B1 heeft mandaat voor Part I (S1–S19):
  elke wirenaam die als huidige wire beschreven staat, naar de L5-spelling;
  Part II (de historische handoefeningen) blijft ongemoeid. Het document zegt
  van zichzelf «The text normates what the code proves»; zonder de hernoeming
  zou Part I een vervallen wirevorm als geldend beschrijven. Gemeten: 65
  v0-namen in backticks in Part I, 3 in Part II.
- **U39 — CLAUDE.md**: de hoofdsessie zet bij de landing de gateteksten om zoals
  "Registerwerk" hieronder zegt.
- **U40 — de open punten van L4**: L5 neemt er geen mee; L5 verandert geen
  betekenis; een bouwer die er een raakt, zet het als genummerd open punt in
  zijn Result.

## Lens-checklist — vooraf, per bouwer (de zeventien punten van L4, aangepast op L5)

Elke bouwer loopt deze lijst vóór zijn Result na en noemt per punt wat hij mat; de lens
toetst op de unie strikt deze lijst plus de kaart, in één ronde.

1. **Elke nieuwe of hernoemde wirepositie door alle vier de lezers gelezen én gebruikt**
   (de gedeelde OCaml-lader van verifier, ref en clingo; de Drools-lader; catala,
   flogic en `IrEmitter` als schrijvers): `source` (`snippet`, `line`) op elke knoop,
   elke hernoemde sleutel uit de hernoemingstabel, elke enumwaarde via de vertaaltabel,
   de negen lijstsleutels onder `rules`, `WholePeriod.granularity`, `host`, `base` en
   `date`, `subjectType`, `path`, `navigation`, `DayKindDefinition`,
   `StartPointDetermination`, `RuleGroup`, `Validity` met afwezige grenzen, `MemberInit`,
   de sleutels van de defaultslaag, `Diagnostic.severity`. Niet "geladen en genegeerd".
2. **Onbekende en vervallen namen bij naam geweigerd in elke lader**: elke v0-knoopnaam
   van de hernoemingstabel (32) en elke v0-sleutel op zijn L5-knoop, de v0-lijstsleutels
   onder `rules`, `bron` op elke knoop, `line` naast `source` op een consistentieversie of
   een regelgroep, `source` als string, als `null`, zonder `snippet`, met
   `"snippet": ""`, met `file` of `column`, of met `line` < 1; een `Unresolved` zonder
   `source` (de uitvoerders; de verifier volgt zijn leesstand, punt 12); elke
   v0-enumwaarde (de zin van de OCaml-lader: `unknown <veld> value "<v>"`); een `null` op
   een veld dat D5 (a) afwezig maakte (`from`, `to`, `host`, `wholePeriod`, `plural`,
   `preposition`, `decision`, `justification`, `characteristic` op een defaultsgroep,
   `dayKind` op een predicaat, `minimum`/`maximum` op een `Clamp`, `filter` op een
   `QuantifierFold`); `WholePeriod` met `jaar`. `Regelversie` met `regel`
   en `inconsistent` blijft laden (D39).
3. **Geen v0-namen in meldingen, commentaren, NOTE-citaten, READMEs en javadoc voor wat de
   wire draagt**: een melding die een wireconstructie noemt, noemt haar met haar
   L5-spelling (knoopnaam, sleutel, enumwaarde: `Unresolved`, `subjectType`, `sum`,
   `evaluationDate`); Nederlandse brontaalwoorden in kleine letters (`gedurende`,
   `verminderd met`, "som and totaal" als woorden van het dialect) blijven; in-memory
   namen (OCaml-constructoren, Java-records) alleen waar de tekst het geheugen beschrijft
   (D52). Geen vervallen wirevorm die in IR-SPEC, RUNTIME-SEMANTICS, Part I van
   ASP-TRANSLATION of een README als geldend beschreven blijft; de `v0 name:`-regels van
   IR-SPEC blijven staan (geschiedenis, en de naamtabeltoets leest ze). Geen D-/U-nummers
   in IR-SPEC, RUNTIME-SEMANTICS, schema, scripts of NOTE's.
4. **Tests die een verboden vorm pinnen herschrijven, niet verwijderen** (tenzij de vorm
   onschrijfbaar is: dan een telling in het Result): `IrEmitterTest` en de dertien andere
   frontendtestbestanden met ingebedde documenten, de 60 Drools-testbestanden met
   ingebedde documenten, `test_ref.ml` (voor zover het meldingen of pointers pint),
   `test_clingo.ml` (973 ingebedde documenten), de catala- en flogic-suites.
5. **Sleutelvolgorde = schema-`properties`-volgorde**, en die is voor elke hernoemde knoop
   de L4-volgorde met de sleutels ter plaatse hernoemd; `source` staat waar `bron` stond,
   `line` staat in `source` na `snippet`; een door D5 (a) afwezig veld laat geen gat.
   `m5.py` en alle drie de emitters schrijven exact die volgorde (gate 2 round-trip;
   gates 9/10/11/14 byte-identiek).
6. **Elke lezer dezelfde defaults**: `source` afwezig = geen bron; `host` afwezig = het
   subject (RUNTIME-SEMANTICS ch. 5.8b); `wholePeriod` afwezig = geen kwalificator;
   `from`/`to` afwezig = open grens, beide afwezig = altijd; `plural`/`preposition`
   afwezig = geen; `decision`/`justification`/`characteristic` afwezig precies zoals
   hun v0-`null`; `dayKind` afwezig op een predicaat van een andere soort;
   `minimum`/`maximum` afwezig = geen grens aan die kant; `filter` afwezig op een
   `QuantifierFold` = geen subselectie.
7. **Per getuige de verwachte arm in `expect.txt`** (U1 van L2) — elke gestempelde getuige
   van de stempellijst en elke nieuwe; elke bestaande `expect.txt` waarvan de melding een
   v0-naam noemde (38 gemeten) opnieuw gepind uit de echte verifieruitvoer.
8. **Vloeren gemotiveerd**: `EXPECTED_MIN_NEGATIEF` 350 blijft; `EXPECTED_MIN_EXPECT` van
   228 naar de gemeten stand; de andere vloeren alleen omlaag bij een gemeten stand
   eronder, in dezelfde commit, met de reden.
9. **Gate-12-pins hermeten** (verwacht 188/167, geen kanaalwissel), zowel na M7 als na
   M8; elke afwijking is een nulmeting en stopt de bouwer op dat punt.
10. **Gate 13 en gate 8 op de L5-namen**: de twister sleutelt op `Binary`/`Rounding`/`Clamp`,
    op `plus`/`minus`/`reducedBy`/`times`, op `sum`/`total`/`minimum`/`maximum`/`first`/
    `last`, op `duration`/`absoluteDuration`, op `left`/`right` en op
    `AttributeReference`/`subjectType`/`attribute`, `ParameterReference`,
    `DimensionCellRead`; `EXPECTED_MIN_TWISTS` 38 blijft. De rood-fragmenten die een
    v0-naam pinnen (11 in 8 cases) worden opnieuw gepind uit de echte uitvoer, per regel
    in het Result; een bodemzin van `check-rood.sh` beweegt alleen als zijn tekst in ref of
    clingo beweegt (verwacht: één regel).
11. **Geen stille terugval**: geen lezer leest een v0-naam naast de L5-naam (een tweede
    spelling van één naam bestaat niet); geen lezer beeldt een onbekende enumwaarde af op
    een standaard; geen lezer leest een afwezige `source` op een `Unresolved` als lege
    tekst; geen `.name()`, `valueOf` of stringvergelijking op een wirewaarde buiten de
    vertaaltabel.
12. **WF-113 en de diagnostics**: WF-113 (b) zoekt `source.snippet` letterlijk; de
    verifier-lader laadt in zijn leesstand `Verifier` een `Unresolved` zonder `source`, zodat
    WF-113 (b) hem noemt, en de uitvoerders (leesstand `Executor`, de Drools-lader)
    weigeren hem bij het laden met dezelfde kernzin; `message` en `line` van een
    diagnostic onveranderd, `severity` in L5-spelling; de meldingtekst die de frontend in
    een WARNING schrijft (producentproza) verandert niet.
13. **Het bronrecord**: `snippet` is bytegelijk de L4-`bron`-string (witruimte inbegrepen,
    nooit getrimd, nooit opnieuw afgeleid); `line` alleen op de consistentieversie en de
    regelgroep, alleen naast een `snippet`, en alleen ≥ 1; geen `file`, geen `column`; een
    lege snippet is in elke lezer een laadweigering.
14. **Pointers op de L5-wire**: elke melding van verifier, ref, clingo en Drools noemt de
    L5-wirepointer (`/rules/rules/<f>/versions/<v>/…`, `/rules/dayKindDefinitions/<i>/condition`,
    `…/wholePeriod`, `…/host`, `…/left`); geen v0-segment blijft over; elk NOTE-citaat
    opnieuw uit de echte verifieruitvoer.
15. **Eén vertaaltabel per taal**: OCaml één tabel in de gedeelde bibliotheek
    (`spec/verifier/lib/`), gebruikt door de lader en door de catala- en flogic-emitter;
    Java één tabel in de gedeelde recordmap van de frontend, gebruikt door `IrEmitter` en
    de Drools-lader; elke tabel volledig en bijectief per familie, getoetst tegen de
    sectie `waarden` van de naamtabel.
16. **De grep-toets "geen v0-naam op de wire"**: `python3 spec/proposals/v1-migratie/m5.py
    --check` meldt 0 documenten in L4- of M7-vorm over alle zones; de naamtabeltoets geeft
    rc 0 met elke v0-rij `geland` (M7 of M8) behalve de rijen van `Regelversie` en `regel`
    (D39); een grep van gequote v0-namen in laders en emitters geeft 0 buiten `m5.py` en
    buiten geschiedeniscommentaar — de telling per bestand in het Result.
17. **De verifier aanvaardt de eigen emissie van elke frontend** (L3-vondst B1): C1 en B2
    leveren voor elke nieuwe emittervorm (`source` met en zonder `line`, `host`,
    `wholePeriod`, elke enumfamilie die de emitter schrijft, `DayKindDefinition`,
    `StartPointDetermination`, `RuleGroup`, de defaultslaag die de frontend schrijft) een
    proefdocument, en de verifier en de vier lezers laden het schoon.

## De L5-vorm van de wire (normatief voor deze kaart)

Canonieke sleutelvolgorde: voor elke hernoemde knoop de L4-volgorde met de sleutels ter
plaatse hernoemd (U16); `source` staat waar `bron` stond; in het bronrecord `snippet`
vóór `line`; een veld dat D5 (a) afwezig maakt, laat de volgorde van de rest ongemoeid.
De `properties`-volgorde van het schema is die volgorde. Serialisatie ongewijzigd (gate
2's serialiser). Alles wat hieronder niet genoemd wordt, is byte-voor-byte L4.

### Fase M7 — het bronrecord (B26; D41–D46)

```
source?: { "snippet": string,        // minLength 1: de L4-bron-string, bytegelijk (D45)
           "line"?: integer }         // ≥ 1; alleen consistentieversie en regelgroep (D43)
```

Het record is de schemadefinitie `$defs/sourceReference` (een schema-interne naam in
kleine beginletter, zoals `formName` en `datatype`; op de wire draagt het record geen
`node` en geen knoopnaam, D41). Het staat als optioneel veld `source` op elke definitie
die in L4 `bron` droeg — 48 schemadefinities, 44 knoopsoorten met dragers (geen dragers:
`TekstReeks`, `VariabeleRef`, `Regelversie`) — op de plaats van `bron`, en is VERPLICHT
op beide varianten van `Onopgelost` (D46). `file` en `column` laat het schema niet toe
tot de positiekaart (D45, U8). Het veld `line` verdwijnt van `ConsistencyVersion` en
`ResolvedRegelGroep`. De sleutel `bron` bestaat na M7 nergens meer op de wire.

Migratie (`m5.py --stage m7`):

| v0 (L4) | M7 | regel |
|---|---|---|
| `"bron": "<s>"` | `"source": {"snippet": "<s>"}` | bytegelijk, ook witruimte |
| `"bron": null` | afwezig | D42 |
| `"bron": ""` | luid falen | een lege snippet bestaat niet (D42); 1 drager: `negatief/wf113-marker-bron-leeg` (met de hand, stempellijst) |
| `Onopgelost` met `"bron": null` | luid falen | `source` is verplicht op de marker (D46); 1 drager: `negatief/wf113-marker-bron-null` (met de hand) |
| `line` naast een `bron`-string | `"source": {"snippet": …, "line": <n>}` | D43; `line` < 1 → luid falen (0 dragers) |
| `line` naast `"bron": null` | vervalt | D43; de landing meldt het aantal |

Gemeten dragers (main `1edd17e4`, a/g/r/n/t = anchor / golden zonder anchor / rood /
negatief / toka):

| wat | aantal a/g/r/n/t = totaal | documenten a/g/r/n/t = totaal |
|---|---|---|
| `bron` als string → `source.snippet` | 531/2610/337/2217/412 = 6107 | 39/189/44/346/23 = 641 |
| `bron: null` → afwezig | 43/148/84/499/0 = 774 | 3/24/4/72/0 = 103 |
| `line` → `source.line` (consistentie 16, regelgroep 1) | 3/4/0/8/2 = 17 | — |
| `line` vervalt (consistentie 27 in negatief, regelgroep 22: a 2, g 2, r 2, n 16) | 2/2/2/43/0 = 49 | — |
| documenten zonder `bron`-sleutel (veranderen in M7 niet) | — | 0/10/1/40/7 = 58 |
| `Onopgelost` (8 met string, 1 met null, van de 8 is er 1 leeg) | 0/0/2/6/1 = 9 | 0/0/2/4/1 = 7 |
| `Literal` zonder `bron` (blijft zonder `source`, D46) | 7/4/14/100/0 = 125 | — |

Per producent in de goldens: RegelSpraak 73 documenten (890 strings, 15 null; 71 met een
string), Catala 12 (255 / 78), F-logic 2 (43 / 12), met de hand 155 (1953 / 86). Geen
`bron` is een witruimtestring; één is de lege string (hierboven). De v0-waarden van `line`
op dragers met een string: 1 (6×, waarvan één op een regelgroep), 2 (3×), 12 (4×, waarvan
één uit de RegelSpraak-golden `jutterswet-vondstnummer`), 30 en 34 (de RegelSpraak-golden
`jutterswet-keurspelling`), en twee in de toka-zone; geen enkele is 0.

### Fase M8 — de hernoeming (B6, B10; D47–D51) en null → afwezig (D5 (a))

**De `rules`-container (D51):**

```
"rules": {
  "rules":                    [ Rule ],                    // v0 resolvedRegels
  "dayKindDefinitions":       [ DayKindDefinition ],       // v0 resolvedDagsoorten
  "objectCreations":          [ ObjectCreation ],          // v0 objectCreaties
  "allocations":              [ Allocation ],              // v0 verdelingen
  "consistencies":            [ Consistency ],             // v0 consistenties
  "uniquenesses":             [ Uniqueness ],              // v0 uniciteiten
  "factCreations":            [ FactCreation ],            // v0 feitCreaties
  "startPointDeterminations": [ StartPointDetermination ], // v0 resolvedStartpunten
  "ruleGroups":               [ RuleGroup ],               // v0 regelGroepen
  "prioritizedDefaults"?:     [ PrioritizedDefaults ]      // ongewijzigd
}
```

Alle 736 documenten dragen de negen v0-sleutels; `prioritizedDefaults` staat in 60
(9/25/0/26/0). De volgorde blijft die van L4. Een wirepointer in een benoemde vorm wordt
`/rules/rules/<f>/versions/<v>/…` (van `/rules/resolvedRegels/…`), enzovoort.

**De hernoemingstabel (B6, B10, D48–D51).** Knopen: aantal knopen en aantal documenten,
a/g/r/n/t. Een sleutel met `?` is optioneel; een sleutel die D5 (a) optioneel maakt, staat
met zijn nulltelling in de volgende tabel. Elke knoop hieronder draagt na M7 `source?`
(op `Unresolved` verplicht).

| v0-knoop | L5-knoop | sleutels v0 → L5, in volgorde | knopen | documenten |
|---|---|---|---|---|
| `Tekst` | `Text` | — | 0/28/2/29/108 = 167 | 0/7/1/16/30 = 54 |
| `Booleaan` | `Boolean` | — | 12/100/8/123/76 = 319 | 7/49/6/98/30 = 190 |
| `Enumeratie` (gebruiks- en declaratiepositie) | `Enumeration` | `domein` → `domain` (alleen op de gebruikspositie), `waarden` → `values` | 0/27/0/107/90 = 224 | 0/6/0/33/30 = 69 |
| `Feittype` | `FactType` | `naam` → `name`, `wederkerig` → `reciprocal`, `rollen` → `roles` | 21/108/23/173/120 = 445 | 13/79/16/119/30 = 257 |
| `Rol` | `Role` | `naam` → `name`, `objecttypeNaam` → `objectType`, `meervoud` → `plural?`, `meervoudig` → `many` | 41/215/46/342/210 = 854 | 13/79/16/119/30 = 257 |
| `Dimensie` | `Dimension` | `naam` → `name`, `naamMeervoud` → `plural?`, `voorzetsel` → `preposition?`, `waarden` → `labels` | 0/2/0/8/0 = 10 | 0/2/0/8/0 = 10 |
| `Tijdlijn` | `Timeline` | `naam` → `name`, `variabelStartpunt` → `variableStart`, `granulariteit` → `granularity`, `aantal` → `count` | 0/2/0/4/30 = 36 | 0/2/0/3/30 = 35 |
| `RolBinding` | `RoleBinding` | `feittype` → `factType`, `rol` → `role` | 43/196/25/181/32 = 477 | 13/75/11/100/15 = 214 |
| `Geldigheid` | `Validity` | `vanaf` → `from?`, `tot` → `to?` (beide afwezig = altijd) | 118/486/92/605/62 = 1363 | 40/202/47/391/29 = 709 |
| `Toekenning` | `MemberInit` | `attribuut` → `member`, `waarde` → `value`, `kenmerk` → `characteristic` | 7/11/2/25/2 = 47 | 4/8/2/23/2 = 39 |
| `ResolvedDagsoort` | `DayKindDefinition` | `naam` → `dayKind`, `voorwaarde` → `condition`, `geldigheid` → `validity` | 0/7/6/27/1 = 41 | 0/5/5/16/1 = 27 |
| `ResolvedStartpunt` | `StartPointDetermination` | `tijdlijn` → `timeline`, `objecttype` → `objectType`, `lid` → `member` (D40) | 0/1/0/1/3 = 5 | 0/1/0/1/3 = 5 |
| `ResolvedRegelGroep` | `RuleGroup` | `naam` → `name`, `recursief` → `recursive`, `leden` → `members` | 2/2/2/17/0 = 23 | 2/2/2/17/0 = 23 |
| `PrioritizedDefaults` (naam blijft) | — | `naam` → `name`, `onderwerp` → `subject`, `kenmerk` → `characteristic?`, `attribuut` → `attribute?`, `ongeordend` → `unordered?`, `defaults`, `geldigheid` → `validity` | 15/23/0/32/0 = 70 | 9/17/0/26/0 = 52 |
| `Default` (naam blijft) | — | `besluit` → `decision?`, `voorwaarde` → `justification?`, `consequentie` → `consequence?`, `initialisatie` → `initialisation?`, `excepties` → `exceptions?` = `{ongeordend → unordered, leden → members}` | 37/48/0/61/0 = 146 | 9/17/0/25/0 = 51 |
| `Binair` | `Binary` | `op`, `links` → `left`, `rechts` → `right` | 36/188/37/89/8 = 358 | 14/84/21/70/5 = 194 |
| `Afgerond` | `Rounding` | `basis` → `base`, `modus` → `mode`, `decimalen` → `decimals` | 4/32/2/2/2 = 42 | 3/14/2/2/2 = 23 |
| `Begrensd` | `Clamp` | `basis` → `base`, `minimum?`, `maximum?` | 0/8/1/8/2 = 19 | 0/3/1/4/2 = 10 |
| `TekstReeks` | `TextComposition` | `delen` → `parts` | 0 | 0 |
| `Letterlijk` | `LiteralChunk` | `tekst` → `text` (D51) | 0 | 0 |
| `Ingevoegd` | `EmbeddedExpression` | `expr` (D51) | 0 | 0 |
| `AttribuutRef` | `AttributeReference` | `attribuut` → `attribute`, `objecttype` → `subjectType` (D49), `navigatieBindingen` → `path?` | 203/987/176/949/150 = 2465 | 34/189/42/340/23 = 628 |
| `DimensieCel` | `DimensionCellRead` | `attribuut` → `attribute`, `labels` | 0/2/0/5/0 = 7 | 0/2/0/5/0 = 7 |
| `ParameterRef` | `ParameterReference` | `parameter` | 6/51/12/73/18 = 160 | 5/28/9/65/9 = 116 |
| `VariabeleRef` | `VariableReference` | `naam` → `name` | 0 | 0 |
| `Onopgelost` (expressie en conditie) | `Unresolved` | `type?` (expressie); geen `reason` (D38) | 0/0/2/6/1 = 9 | 0/0/2/4/1 = 7 |
| `TemporalRead` (naam blijft) | — | `basis` → `base`, `datum` → `date` (D50) | 3/0/1/6/0 = 10 | 2/0/1/6/0 = 9 |
| `DimensieSelectie` | `DimensionSelection` | `dimensie` → `dimension`, `labels` (D51) | 0/0/0/1/0 = 1 | 0/0/0/1/0 = 1 |
| `Vergelijking` | `Comparison` | `op`, `links` → `left`, `rechts` → `right`, `periode` → `wholePeriod?` | 20/193/22/182/75 = 492 | 12/90/13/123/12 = 250 |
| `Predicaat` | `Predicate` | `soort` → `kind`, `operand`, `dagsoort` → `dayKind?`, `negatie` → `negated`, `cijfers` → `digits`, `periode` → `wholePeriod?` | 1/4/7/30/0 = 42 | 1/2/7/27/0 = 37 |
| `Kenmerk` | `CharacteristicCheck` | `kenmerk` → `characteristic`, `negatie` → `negated`, `eigenaar` → `host?` (B10, D48), `periode` → `wholePeriod?`, `navigatieBinding` → `navigation?` | 57/98/5/131/9 = 300 | 18/58/5/64/3 = 148 |
| `Periode` | `TimeWindow` | `vanaf` → `from?`, `tot` → `to?`, `totInclusief` → `toInclusive` | 0/13/1/7/0 = 21 | 0/4/1/7/0 = 12 |
| `Samengesteld` | `Compound` | `kwantor` → `quantifier`, `drempel` → `threshold` (altijd geschreven, U19), `onderdelen` → `parts` | 11/60/4/55/24 = 154 | 6/37/4/38/8 = 93 |
| `Kwantorvouw` | `QuantifierFold` | `kwantor` → `quantifier`, `drempel` → `threshold?`, `elementObjecttype` → `elementType`, `filter?`, `toets` → `test`, `bronRol` → `sourceRole?`, `bronPad` → `sourcePath?` | 0/38/0/21/0 = 59 | 0/12/0/18/0 = 30 |
| `GeheelPeriode` | `WholePeriod` | `jaar` → `granularity` (`true` → `"year"`, `false` → `"month"`), `niet` → `negated` (D51) | 0/15/4/12/0 = 31 | 0/6/4/12/0 = 22 |
| `Regelversie` | blijft (D39) | `regel`, `inconsistent` blijven; alleen `bron` → `source` (M7) | 0 | 0 |

Daarmee verdwijnen 32 v0-knoopnamen van de wire (`Onopgelost` telt één keer; `Regelversie`
blijft). Elke andere knoop verandert in M8 alleen in zijn enumwaarden (tabel hieronder).

**De enumwaarden (N2, D47).** Eén regel: Engels, lowerCamel (U31 voor de IR-SPEC-tekst).
Dragers: aantal knopen a/g/r/n/t = totaal. "IR-SPEC nu" is de spelling die IR-SPEC vandaag
draagt; A2 trekt haar gelijk.

| familie (L5-positie) | v0 → L5 (dragers) | IR-SPEC nu |
|---|---|---|
| `Number.sign` | `ONBEPAALD` → `unspecified` (232/1275/204/1225/967 = 3903), `NEGATIEF` → `negative` (0), `NIET_NEGATIEF` → `nonNegative` (99/329/102/683/107 = 1320), `POSITIEF` → `positive` (0/1/0/3/150 = 154) | 2.4, al zo |
| `Characteristic.kind` | `BIJVOEGLIJK` → `adjectival` (54/170/17/282/149 = 672), `BEZITTELIJK` → `possessive` (1/1/0/0/90 = 92), `ZELFSTANDIG` → `nominal` (0/0/0/1/239 = 240) | 2.2, al zo |
| granularity: `TimelineFacet.granularity`, `Timeline.granularity`, `BuiltinCall.granularity` | `DAG` → `day` (2), `WEEK` → `week` (0), `MAAND` → `month` (111 + 1 + 20), `KWARTAAL` → `quarter` (0 + 30 + 0), `JAAR` → `year` (66 + 5 + 13) | 2.2/2.8 al zo; 4.3 "month/year" en "(v0 spelling)" |
| `RuleVersion.presentation` | `GELIJKSTELLING` → `equation` (23/80/8/93/18 = 222), `BEREKENING` → `calculation` (49/245/58/232/12 = 596) | 3.1 noemt beide |
| `AllocationVersion.method` | `GELIJKE_DELEN` → `equalShares` (0/5/0/8/2 = 15), `NAAR_RATO` → `proportional` (1/5/0/10/5 = 21), `OP_VOLGORDE` → `ordered` (0/3/0/2/3 = 8) | 3.2, al zo |
| `Comparison.op` | `KLEINER` → `less` (32), `KLEINER_OF_GELIJK` → `lessOrEqual` (8), `GROTER` → `greater` (120), `GROTER_OF_GELIJK` → `greaterOrEqual` (87), `GELIJK` → `equal` (148), `ONGELIJK` → `notEqual` (31), `EERDER` → `earlier` (36), `EERDER_OF_GELIJK` → `earlierOrEqual` (0), `LATER` → `later` (25), `LATER_OF_GELIJK` → `laterOrEqual` (5) | geen (4.5: "ordering, equality, date ordering"); B6 toont `greater` |
| `Binary.op` | `PLUS` → `plus` (200), `MIN` → `minus` (24), `VERMINDERD_MET` → `reducedBy` (12), `MAAL` → `times` (49), `GEDEELD_DOOR` → `dividedBy` (55), `GEDEELD_DOOR_ABS` → `dividedByAbs` (3), `TOT_DE_MACHT` → `power` (8), `PERCENTAGE_VAN` → `percentageOf` (7) | 4.2, Nederlands met koppeltekens |
| `Rounding.mode` | `NAAR_BENEDEN` → `floor` (18), `NAAR_BOVEN` → `ceiling` (1), `RICHTING_NUL` → `towardZero` (1), `WEG_VAN_NUL` → `awayFromZero` (1), `REKENKUNDIG` → `halfAwayFromZero` (21) | geen; RUNTIME-SEMANTICS 2.5 geeft de betekenissen |
| `Aggregation.kind` | `SOM` → `sum` (39), `TOTAAL` → `total` (39), `AANTAL` → `count` (114), `MINIMUM` → `minimum` (18), `MAXIMUM` → `maximum` (10), `EERSTE` → `first` (14), `LAATSTE` → `last` (9), `AANTAL_GEVULDE` → `filledCount` (16) | 4.3 in woorden; `filledCount` staat er al |
| `BuiltinCall.kind` | `WORTEL` → `squareRoot` (0), `ABSOLUTE_WAARDE` → `absoluteValue` (1), `CONCATENATIE` → `concatenation` (0), `TIJDSDUUR` → `duration` (20), `ABSOLUTE_TIJDSDUUR` → `absoluteDuration` (4), `EERSTE_PAASDAG` → `easterSunday` (6), `TIJDSEVENREDIG_DEEL` → `timeProportionalPart` (20), `AANTAL_DAGEN_IN` → `dayCount` (13), `JAAR_UIT` → `yearOf` (10), `MAAND_UIT` → `monthOf` (16), `DAG_UIT` → `dayOf` (17), `UUR_UIT` → `hourOf` (0), `MINUUT_UIT` → `minuteOf` (0), `SECONDE_UIT` → `secondOf` (0), `MILLISECONDE_UIT` → `millisecondOf` (0), `DATUM_MET` → `dateWith` (0), `DATUMTIJD_MET` → `dateTimeWith` (0) | 4.3, Nederlands met koppeltekens; extractors zonder spelling |
| `Predicate.kind` | `LEEG` → `empty` (7), `GEVULD` → `filled` (30), `ELFPROEF` → `elevenTest` (0), `NUMERIEK_MET_EXACT` → `exactDigits` (0), `DAGSOORT` → `dayKind` (5) | 4.5: empty, filled, elfproef, exact-digits, day-kind |
| quantifier: `Compound.quantifier`, `QuantifierFold.quantifier` | `DE` → `the` (0 + 1, het laatste een WF-108-getuige), `ALLE` → `all` (104 + 20), `GEEN` → `none` (11 + 11), `TEN_MINSTE` → `atLeast` (30 + 14), `TEN_HOOGSTE` → `atMost` (6 + 7), `PRECIES` → `exactly` (3 + 6) | 4.5: the, all, none, at-least, at-most, exactly |
| `ContextValue.name` (vrije string, gesloten door WF-098) | `Rekendatum` → `evaluationDate` (1/0/2/3/0 = 6), `Rekenjaar` → `evaluationYear` (0); een naam buiten de verzameling gaat verbatim mee (`de dag`, `de dagX`, `rekendatum`: elk 1, negatief) | 4.1: evaluation-date, evaluation-year |
| `WholePeriod.granularity` (v0 boolean `jaar`) | `true` → `year` (0/15/4/12/0 = 31), `false` → `month` (0) | 4.5: `year-or-month` |
| `Diagnostic.severity` | `WARNING` → `warning` (0/1/2/6/5 = 14); bij naam geweigerd, alleen in getuigen: `ERROR` → `error` (1), `INFO` → `info` (1); `null` blijft (1) | ch. 2: `WARNING` |

Onveranderd: `Date.precision` (`day`, `millisecond`), `Number.presentation`
(`percentage`), `format` (`uchar-ir-v1`), de sleutels van de literaalvarianten. Een
enumwaarde op een verkeerde positie (`the` op een `QuantifierFold`; `error`, `info` en
`null` als severity) blijft een gelezen waarde die de verifier bij naam weigert (WF-108 (a),
WF-113 (a)); elke andere onbekende waarde is een laadfout.

**null → afwezig (D5 (a)).** Op de hernoemde knopen, waar IR-SPEC het veld optioneel noemt:

| L5-positie (v0) | v0-`null` → afwezig, a/g/r/n/t = totaal |
|---|---|
| `Validity.from` (`Geldigheid.vanaf`) | 116/484/91/603/62 = 1356 |
| `Validity.to` (`Geldigheid.tot`) | 116/484/90/604/62 = 1356 |
| `Role.plural` (`Rol.meervoud`) | 18/110/19/144/210 = 501 |
| `Comparison.wholePeriod` (`Vergelijking.periode`) | 20/181/22/175/75 = 473 |
| `CharacteristicCheck.wholePeriod` (`Kenmerk.periode`) | 57/95/5/126/9 = 292 |
| `CharacteristicCheck.host` (`Kenmerk.eigenaar`) | 57/92/5/109/9 = 272 |
| `Default.decision` (`besluit`) | 22/41/0/39/0 = 102 |
| `QuantifierFold.filter` | 0/31/0/20/0 = 51 |
| `PrioritizedDefaults.characteristic` (`kenmerk`) | 8/20/0/21/0 = 49 |
| `Predicate.wholePeriod` (`periode`) | 1/4/3/30/0 = 38 |
| `Predicate.dayKind` (`dagsoort`) | 1/3/6/27/0 = 37 |
| `Default.justification` (`voorwaarde`) | 2/1/0/14/0 = 17 |
| `TimeWindow.to` (`Periode.tot`) | 0/7/0/3/0 = 10 |
| `Clamp.maximum` | 0/3/0/3/2 = 8 |
| `Clamp.minimum` | 0/3/0/2/0 = 5 |
| `Dimension.plural` (`naamMeervoud`) | 0/0/0/3/0 = 3 |
| `Dimension.preposition` (`voorzetsel`) | 0/0/0/3/0 = 3 |
| `TimeWindow.from` (`Periode.vanaf`) | 0/1/0/1/0 = 2 |
| **totaal, 18 posities** | **4575** |

Na de migratie: `Validity` zonder grenzen 1353 keer (altijd), met een grens 10; `host`
aanwezig 28 keer (0/6/0/22/0); `wholePeriod` aanwezig 31 keer; een `TimeWindow` zonder
grenzen 1 keer (`negatief/wf042-empty-window`: WF-042's getuige; het schema eist
daarom geen van beide grenzen, U17).

Blijft nullable (D5 (b), een WF-regel bewaakt de aanwezigheid; null-dragers):
`AttributeReference.attribute` en `.subjectType` (2 en 2, WF-034), `DimensionCellRead.attribute`
(1, WF-034 (g)), `ParameterReference.parameter` (1, WF-034),
`CharacteristicCheck.characteristic` (1, WF-034), `QuantifierFold.elementType` en `.test`
(1 en 1, WF-108), `DayKindDefinition.dayKind` en `.condition` (1 en 1, WF-031),
`PrioritizedDefaults.name` (1, WF-107). Elk ander v0-nullable veld van een hernoemde knoop
(0 null-dragers) blijft nullable zoals in L4: geen aanscherping in deze kaart (U18).

**Migratie per v0-vorm (`m5.py`, standaard M7 + M8; een document in M7-vorm krijgt alleen
M8).** Elke hernoeming uit de tabellen hierboven, ter plaatse; elke enumwaarde via de
tabel; elke `null` van de D5 (a)-tabel → afwezig; `GeheelPeriode.jaar` → `granularity`
(`true` → `"year"`, `false` → `"month"`); een naam van een contextwaarde buiten de tabel
verbatim. Luid falen, met bestand en JSON-pointer in de invoer: een enumwaarde die niet in
de tabel staat (ook niet als bij-naam-geweigerde waarde), een sleutel die niet bij de
L4-knoop hoort, een document dat L4-, M7- en L5-sleutels mengt. Verwacht: 0 weigeringen in
M8; de enige weigeringen van een volledige run over L4 zijn de twee van M7.

**Gemeten omvang van M8** (main `1edd17e4`): 32 knoopnamen; 72 propertyrijen van de
naamtabel met een levende groep die geen identiteit is, plus de identiteitsrij `datum`
die D50 beslist (`bron` en een groep van `line` gaan in M7, `regel` blijft, D39); 15
enumfamilies op 18 posities; 4575 nullen; alle 736 documenten.

## Regels die veranderen (IR-SPEC + verifier, bouwer A2)

A2 schrijft de spec-tekst ZELF (les (b)). Elke nieuwe of veranderde weigerzin staat
letterlijk gelijk in verifier, gedeelde OCaml-lader, ref, clingo en Drools, zonder
regelnummerstaart (les (e)); een zin hieronder tussen «» is een voorstel, A2 mag hem
aanscherpen maar zet dan dezelfde zin in alle lezers. L5 verandert geen betekenis: geen
regel komt erbij, geen regel vervalt; wat verandert zijn de namen in de regeltekst, in de
meldingen en in de pointers, en de plaats waar WF-113 (b) de brontekst leest.

**Vervalt geheel:** niets. B6 is een hernoeming; de armen van WF-113 (b) blijven bestaan
via de leesstand van de verifier-lader (les (i)).

**Verandert in M7 (de verifiercode door M7-O, vóór A2; de IR-SPEC-tekst van B26 schrijft
A2 in fase 1):**

- **WF-113 (b)** zoekt `source.snippet` letterlijk in de `message` van een diagnostic
  (D46). `source` is verplicht op `Unresolved` (schema; de uitvoerders weigeren een marker
  zonder `source` bij het laden); de verifier-lader laadt hem in de stand `Verifier`, en
  WF-113 (b) weigert hem met de zin van de null-arm: «Unresolved marker without a source:
  no diagnostic message can contain it, so it cannot be accounted for». De blanco-arm
  blijft voor een snippet van alleen witruimte: «Unresolved marker whose source snippet
  is blank: blank counts as absent, since nearly every message contains it and a message
  about another marker would account for this one». Een LEGE snippet kan niet geschreven
  worden (schema `minLength: 1`) en elke lezer weigert haar bij het laden, ook de verifier:
  «a source snippet is never empty: a node without source text carries no source» (geen
  WF-nummer; onschrijfbaar, M1-precedent). In M7 heet de knoop op de wire nog `Onopgelost`;
  de zinnen noemen vanaf M7 `Unresolved`, zodat zij in M8 niet opnieuw bewegen.
- **Meldingen die `bron` noemen** noemen `source`. Gemeten: naast de WF-113-zinnen één
  gedeelde weigerzin, die van WF-095 over een datumtijdlexeem buiten de grammatica ("…
  the day-first source snippet belongs in `bron` — ch. 4.1)"), één functie in
  `checks.ml` die ook ref en clingo gebruiken, dezelfde zin in de Drools-lader, gepind in
  `DatumTijdLexeemWitnessTest`, `RegelEnExpressielaagWeigeringWitnessTest`,
  `test_clingo.ml` en de NOTE van `negatief/wf084-source-snippet`. Geen corpuscase van
  golden, rood of toka bereikt die zin, dus de nulmeting van de uitvoerders ziet hem niet.

**Verandert in M8:**

- **WF-113 (a)**: de ene wireseverity is `warning`; `error` en `info` worden bij naam
  geweigerd («severity error is not a wire severity: …»), een ontbrekende severity met
  «diagnostic without a severity: the one wire severity is warning».
- **WF-098**: de gesloten verzameling is `evaluationDate`, `evaluationYear`, bytegelijk —
  «context value '<n>' is not one of the two admissible names evaluationDate,
  evaluationYear: the context-value name is a closed set, spelled byte for byte». De
  v0-spelling `Rekendatum` valt er nu buiten, zoals `de dag` sinds L4.
- **WF-031**: de sessiewaarde-arm noemt `evaluationDate`; de velden van de
  dagsoortdefinitie heten `dayKind`, `condition`, `validity`.
- **WF-034 (q), WF-100, WF-109** (B10, D48): het veld heet `host`; afwezig is het subject;
  de pointers eindigen op `…/host`. **WF-097**: de eigenaar-arm noemt `host`, de
  kwalificator-arm `wholePeriod`.
- **WF-049** (D50): `base` en `date`.
- **WF-053, WF-083, WF-034 (de attribuutreferentie)** (D49): `subjectType` en `path` —
  «an attribute reference without a path is the subject's own read, so its subjectType is
  the subject type '<T>'»; «fold-source subjectType '<T>' is not the fold's anchor …».
- **WF-020, WF-021, WF-107**: de lijstnamen in de melding zijn de L5-namen («collides
  with '<n>' in allocations», «form in rules carries no name …»).
- **WF-021, WF-101** (`Validity`) en **WF-042, WF-111, WF-112** (`TimeWindow`): `from` en
  `to`; een afwezige grens is open, en WF-042 weigert een venster zonder beide grenzen.
- **WF-108**: de velden van `QuantifierFold` (`quantifier`, `threshold`, `elementType`,
  `filter`, `test`, `sourceRole`, `sourcePath`); arm (a) weigert `the` bij naam.
- **WF-037, WF-038, WF-055, WF-056, WF-083, WF-089, WF-090, WF-091, WF-093, WF-102,
  WF-110**: de soorten en operatoren in hun L5-spelling (`sum`, `total`, `count`, `first`,
  `last`, `minimum`, `maximum`, `filledCount`, `plus`, `times`, `timeProportionalPart`,
  `dayCount`, …); het woord "soort" in de WF-091-zinnen is de term van IR-SPEC voor de
  datatypefamilie (ch. 4.1) en geen wiresleutel: het blijft (U33).
- **WF-032, WF-033**: `StartPointDetermination {timeline, objectType, member}` en
  `RuleGroup {name, recursive, members}`.
- **WF-011, WF-012, WF-013, WF-014, WF-015, WF-016, WF-040**: `FactType`/`Role`
  (`reciprocal`, `roles`, `many`, `plural`), `Dimension` (`labels`), `Timeline`
  (`variableStart`, `granularity`, `count`), `DimensionCellRead`/`DimensionSelection`.
- **WF-041, WF-043**: `threshold`; `WholePeriod`.
- **WF-050, WF-051, WF-075, WF-107** (defaultslaag): `name`, `subject`, `characteristic`,
  `attribute`, `unordered`, `decision`, `justification`, `consequence`, `exceptions`,
  `members`, `validity`.
- **WF-022, WF-079**: waar een melding een presentatietag noemt, `equation`/`calculation`.
- **WF-044**: `Unresolved` (gedragsregel; de rood-fragmenten, gate 8).
- **Elke pointer** volgt de L5-wire, ook de pointers die `checks.ml` zelf bouwt (gemeten:
  212 pointerliteralen, waarvan 26 met een v0-segment: `resolvedDagsoorten` 5,
  `voorwaarde` 5, `consequentie` 3, `regelGroepen` 2, `excepties` 2, en `naam`,
  `geldigheid`, `resolvedStartpunten`, `links`, `rechts`, `vanaf`, `tot`, `waarde`,
  `line` elk 1), en de pointers die de lader per versie opslaat (`/rules/rules/…` enz.).

**Nieuw:** geen WF-regel. Wel een getuige (U20): `wf098-v0-naam-rekendatum` —
`ContextValue {name: "Rekendatum"}`: de v0-spelling valt sinds L5 buiten de verzameling.

**Spec-tekst die mee moet** (A2, Engels, de lens leest elke gewijzigde regel). Gemeten in
backticks op `1edd17e4`: IR-SPEC 101 v0-knoopnamen, 500 v0-sleutels, 245 v0-enumwaarden;
RUNTIME-SEMANTICS 52 / 138 / 69; per regel beoordelen: een `v0 name:`-regel en een
geschiedenisalinea blijven, een zin die de huidige wire beschrijft gaat naar de L5-naam.

- **Conventions**: de spellingsregel van N2 als bindende regel (voorstel in U31), met de
  familietabellen die naar hun v0-spelling verwijzen.
- **Method**: de lijst van extensievelden (`navigatieBindingen` → `path`,
  `navigatieBinding` → `navigation`, "the quantifier fold … keeps the v0 names `bronRol`
  and `bronPad` until its own landing" → geland).
- **ch. 2**: de tabel **Rules** met de L5-lijstsleutels (de zin "under their v0 keys,
  verbatim until landings L4 … and L5" wordt geschiedenis); `Diagnostic.severity`
  `warning`. **2.2** `kind`- en granularity-waarden ("keep their v0 spelling until M8"
  wordt geland). **2.4** `Text`, `Boolean`, `Enumeration` op de wire; `sign`-waarden.
  **2.6** `FactType`, `Role`, `RoleBinding` op de wire ("wire node `RolBinding`", "v0 wire
  `feittype`/`rol`" wordt geschiedenis). **2.7**, **2.8** idem voor `Dimension`,
  `Timeline`.
- **ch. 3**: Versioning en **3.10** (`Validity`, afwezige grenzen; "a `Geldigheid` node
  until its own landing" en "always is the node with both bounds null" worden geland);
  **3.1** `presentation` ∈ `equation`, `calculation` (geland); **3.2** `method` op de wire
  in IR-SPEC-spelling; **3.3** `MemberInit` op de wire ("still the v0 node `Toekenning`"
  wordt geland); **3.5**, **3.6** `source` (met `line`) op het versierecord; **3.7**
  `DayKindDefinition` als knoopnaam (D51); **3.8** `StartPointDetermination` (D40, D51);
  **3.9** `RuleGroup` met `source` en het opgegane `line`.
- **ch. 4 (preambule)**: het bronrecord geland (D41–D46): "Every node carries a source
  reference" wordt "may carry", `Unresolved` verplicht, `snippet` + `line?`, `file` en
  `column` wachten op de positiekaart, de "pending delta" wordt geland.
- **4.1**: `ContextValue.name` ∈ `evaluationDate`, `evaluationYear`; elke zin "(`bron` on
  the wire)" wordt `source`; de `Literal`-rij (juridische spelling in `source.snippet`).
  **4.1a** `subjectType`, `path`. **4.2** `Binary.op`, `Rounding.mode`, `Clamp`,
  `TextComposition` met `LiteralChunk {text}` en `EmbeddedExpression {expr}` (D51).
  **4.3** `Aggregation.kind` en `BuiltinCall.kind`/`granularity` in L5-spelling ("in their
  v0 spelling until their own landing" wordt geland); `DimensionSelection {dimension,
  labels}` (D51). **4.3a** de veldtabel `base`, `date` (D50). **4.4** `Unresolved` met
  verplichte `source`. **4.5** `host?` in de tabel van `CharacteristicCheck` (D48);
  `Comparison.op`, `Predicate.kind`, de kwantoren in L5-spelling; `QuantifierFold` op de
  wire ("wire `Kwantorvouw`" wordt geland); `WholePeriod {granularity, negated}` (D51);
  `RuleStatusCheck`: de wire houdt `Regelversie` (D39), met `source`. **4.6** de WF-teksten
  hierboven. **4.7–4.10** alleen `bron` → `source`.
- **ch. 6.1**: de wire-namen van de defaultslaag ("v0 wire `naam`" enz. wordt
  geschiedenis).
- **De WF-index**: elke rij met een veranderde naam of melding, de WF-098-rij
  (`wf098-v0-naam-rekendatum`), de WF-113-rij (`source.snippet`, de lege snippet als
  laadweigering).
- **`RUNTIME-SEMANTICS.md`**: elke plek die een v0-knoop, -sleutel of -waarde als levende
  wirevorm noemt (per regel; de RegelSpraak-woorden van het dialect in kleine letters
  blijven, U32).

## Stempellijst (V14 (a), D17, D18)

Bron: `research/v1-klap/stempelmeting-negatief-l2-l5.md` §5 (de L5-tabel: elf getuigen,
alle herschreven (m) op de hernoeming `eigenaar` → `host`), §7 (de stand) en §8 (S3 over
null, S8 over de arm), bijgesteld door het register (D42, D46, D48–D51, N2) en door de
meting van deze snijsessie: een statische wandeling over de 417 negatief-documenten op
`1edd17e4` (welke getuige draagt een construct dat L5 hernoemt of herspelt, en op welke
plek staat zijn defect). De verifier is in deze snijsessie niet gebouwd en niet gedraaid:
per getuige de arm meten is werk van A2 (de L4-verifier op de L4-vorm tegen de L5-verifier
op de L5-vorm, zoals in L4). Stempelmeting §5 zei: "Geen van deze getuigen hangt af van
`bron` (B26)"; dat klopt voor de elf, maar vier WF-113-getuigen hangen er wel van af
(hieronder).

**Verwijderd-met-reden: 0.** Geen v0-vorm wordt in L5 onschrijfbaar zonder dat een
WF-regel haar defect nog noemt: de lege snippet en de marker zonder `source` zijn
getuigen van WF-113 (b), die via de leesstand blijft oordelen.

**Herschreven (i) (2):**

| map | regel | delta | stempel | L5-vorm / reden |
|---|---|---|---|---|
| `wf113-marker-bron-leeg` | WF-113 (b) | B26, D42 | herschreven (i), in M7 | `m5.py` faalt op `"bron": ""` (een lege snippet bestaat niet); met de hand een snippet van alleen witruimte (`"source": {"snippet": " "}`), zodat de blanco-arm haar defect houdt; de lege snippet is sinds L5 een laadweigering in elke lezer (U10) |
| `wf098-contextwaarde-naam-kapitalisatie` | WF-098 | N2, D47 | herschreven (i), in M8 | `m5.py` draagt `rekendatum` verbatim mee (een naam buiten de tabel); met de hand `EvaluationDate`, de herkapitalisering van de L5-naam `evaluationDate`, zodat de getuige blijft pinnen dat de verzameling bytegelijk kruist |

**Herschreven (m), met een L5-delta op het defect** — `expect.txt` per getuige (U1 van
L2); A2 meet per getuige de arm. De eerste elf zijn de rijen van stempelmeting §5, de
overige de meting van deze snijsessie.

| map(pen) | regel | delta | L5-vorm |
|---|---|---|---|
| `wf034-kenmerkcheck-onbekende-eigenaar` | WF-034 (q) | B10 | `host: "Bestaatniet"` |
| `wf100-eigenaar-verklaart-kenmerk-niet` | WF-100 | B10 | `host` (`Vondst`, `Jutter`); de bijvangst WF-109 blijft |
| `wf109-doelgastheer-genavigeerd`, `wf109-doelgastheer-periode`, `wf109-doelgastheer-samengesteld`, `wf109-subtype-overlap` | WF-109 | B10 | `host: "Sein"` (samengesteld ook `Kustvak`); `-periode` draagt ook een `WholePeriod` |
| `wf109-gastheer-ambigu-supertype` | WF-109 | B10 | `host: "Baken"` |
| `wf109-gastheer-ambigu-zelfgetypeerd` | WF-109 | B10 | `host: "Wachter"` |
| `wf109-gastheer-buiten-bereik`, `wf109-gastheer-deelpopulatie`, `wf109-gastheer-derde-type` | WF-109 | B10 | `host` (`Vondst`/`Jutter`; `Vondst`/`Strandvondst`; `Sein`/`Post`) |
| `wf097-clip-kenmerk-eigenaar`, `wf097-dagentelling-kenmerk-eigenaar`, `wf097-prorata-kenmerk-eigenaar` | WF-097 | B10 | `host: "Jutter"` op het lid van de clip; de melding noemt `host` |
| `wf097-clip-kenmerk-periode`, `wf097-clip-kenmerk-periode-ontkend`, `wf097-dagentelling-kenmerk-periode`, `wf097-prorata-kenmerk-periode` | WF-097 | D51 | `wholePeriod: {node: "WholePeriod", granularity: "year", negated: …}` op het lid van de clip |
| `wf043-wholeperiod-nontemporal` | WF-043 | D51 | `WholePeriod` op een niet-temporeel operand |
| `wf113-marker-bron-null` | WF-113 (b) | B26, D46 | een `Unresolved` zonder `source`; `m5.py` faalt erop (verplicht veld), de hand schrijft hem, de leesstand laadt hem, de null-arm weigert hem |
| `wf113-marker-zonder-verslag`, `wf113-verslag-noemt-marker-niet` | WF-113 (b) | B26 | `source.snippet` op de marker; de melding noemt de snippet |
| `wf113-severity-error`, `wf113-severity-info`, `wf113-severity-null` | WF-113 (a) | N2 | `severity` `error`, `info`, `null`; de melding noemt `warning` als de ene wireseverity |
| `wf108-kwantorvouw-kwantor-de` | WF-108 (a) | N2 | `QuantifierFold.quantifier: "the"`, bij naam geweigerd |
| `wf098-onbekende-contextwaarde-naam`, `wf098-de-dag-als-contextwaarde` | WF-098 | N2 | `de dagX` en `de dag` verbatim; de melding noemt `evaluationDate`, `evaluationYear` |
| `wf031-sessiewaarde-in-dagsoort` | WF-031 | N2 | `ContextValue {name: "evaluationDate"}` in een `DayKindDefinition.condition` |
| `wf049-read-at-datetime`, `wf049-read-of-nontemporal`, `wf049-type-not-element` | WF-049 | D50 | `TemporalRead {base, date}` |
| `wf053-eigen-lezing-supertype`, `wf034-attribuutref-zonder-objecttype` | WF-053, WF-034 | D49 | `subjectType` (een supertype van het subject; `null`) |
| `wf083-vouwbron-anker-op-tussentype`, `wf083-vouwbron-vreemd-elementtype`, `wf083-vouwbron-vreemd-objecttype` | WF-083 | D49 | `subjectType` van de vouwbron; de melding noemt `subjectType` |
| `wf021-version-overlap`, `wf101-geldigheidsgrens-kalenderdag`, `wf101-geldigheidsgrens-tekenjaar` | WF-021, WF-101 | B6, D5 (a) | `Validity {from?, to?}`; de ongeldige grens staat in `from` of `to` |
| `wf042-empty-window` | WF-042 | B6, D5 (a) | `TimeWindow` zonder `from` en zonder `to` (v0: beide `null`) |
| `wf111-datumtijdgrens-attribuut`, `wf111-datumtijdgrens-literal`, `wf111-periodegrens-parameterdatum` | WF-111 | B6 | `TimeWindow.from`; de melding noemt `from` |

Mechanisch, zonder L5-delta op het defect (geen stempelrij; A2 meldt de telling): alle
andere getuigen — hun namen, pointers en enumwaarden bewegen, hun defect niet. Daaronder
de getuigen waarvan alleen de meldingstekst een soort of operator noemt (WF-037, WF-038,
WF-055, WF-056, WF-083 `totaal-vreemd-elementtype`, WF-089, WF-090, WF-091, WF-093,
WF-102, WF-110) en de getuigen van WF-020 en WF-107 waarvan de melding een lijstnaam
noemt: hun `expect.txt` wordt opnieuw gepind waar hij een v0-naam draagt. Gemeten: 38 van
de 228 `expect.txt` noemen een v0-naam (39 treffers, waarvan één de gedeclareerde
attribuutnaam `aantal aangemelde vondsten`): `EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM` 11,
`PLUS`/`MAAL` 7, `SOM`/`TOTAAL`/`AANTAL`/`AANTAL_GEVULDE` 5, `objecttype` 4,
`Rekendatum`/`Rekenjaar` 4, een lijstsleutel 5, `vanaf` 2.

**Nieuw (met `NOTE.md` en `expect.txt`)** — de mapnaam is een voorstel:

| map | regel | wat het pint | grond |
|---|---|---|---|
| `wf098-v0-naam-rekendatum` | WF-098 | `ContextValue {name: "Rekendatum"}`: de v0-spelling valt sinds L5 buiten de gesloten verzameling | N2; het precedent van `wf098-de-dag-als-contextwaarde` in L4 (U20) |

Verwachte negatief-stand: met het standaardantwoord 417 − 0 + 1 = 418; zonder de nieuwe
getuige 417; de vloer 350 blijft (D20). `EXPECTED_MIN_EXPECT` stijgt van 228 naar de
gemeten stand: elke herschreven getuige hierboven zonder `expect.txt` krijgt er een (31
getuigen in de twee tabellen hebben er nog geen) en de nieuwe getuige ook, verwacht 228 +
31 + 1 = 260; de 38 opnieuw gepinde bestanden veranderen de telling niet.
`corpus/negatief/README.md` krijgt een sectie "L5 — the source record and the renaming"
naar het model van L4 (Engels; kolommen directory, rule, delta, stamp, arm, reason): de
nulmeting van M7 in één zin, de twee met de hand herschreven getuigen, de gestempelde
getuigen met hun arm, de nieuwe getuige, en de telling van de getuigen waarvan alleen
naam, pointer of meldingstekst bewoog. De M1–L4-secties blijven geschiedenis; hun pointers
(`/rules/verdelingen/3` enz.) blijven zoals ze toen waren.

## M7 — de nulmeting (ingevuld door de hoofdsessie, 2026-09-25)

**Commitreeks.** Van de claimcommit `1690aa05` tot de M7-unie `14f1bfdb` (U1):
dertien commits van M7-O (`6dac7367` … `c48b13df` de wire, fase 1; `7b5a993e`
… `98845f49` de gedeelde lader, `checks.ml`, de clingo-suite en de catala- en
flogic-emitter, fase 2) en vier van M7-J (de Java-emitter en frontendtests,
de Drools-lader en Drools-tests, de getuige `BronrecordWeigeringWitnessTest`),
gerebaset op M7-O's kop. Geen document en geen lezer draagt nog `bron`.

**Tellingen van `m5.py --stage m7`** (a/g/r/n/t): 6098 strings naar
`source.snippet` in 639 documenten plus 9 in de twee handgetuigen = 6107 in
641; 773 + 1 nullen weg = 774 in 103; `line` 17 opgegaan (consistentie 16,
regelgroep 1) en 49 vervallen (consistentie 27, regelgroep 22); 676
documenten gemigreerd, 58 zonder `bron`-sleutel ongemoeid; precies twee
weigeringen, beide op `/rules/resolvedRegels/0/versions/0/condition/links/bron`
(`wf113-marker-bron-leeg`: met de hand een snippet van alleen witruimte;
`wf113-marker-bron-null`: met de hand de marker zonder `source`); de toka-dump
168 snippets en 1 `line`, door de frontend byte-gelijk opnieuw geëmitteerd
(104888 bytes); `--check` 0 van 736; een tweede `--all` wijzigt niets.

**De veertien gates op de unie** (elk als losse aanroep; L4-stand tussen
haakjes waar hij anders is): 1 PASS; 2 PASS 289, 77 patroon- en 327 vormrijen
(311); 3 PASS 289 schoon, 417 negatief, 228 armfragmenten; 4 PASS 667; 5 PASS
ref-suite 976, 619 replays; 6 PASS clingo-suite 1325 (1284: de M7-laadtabel),
619; 7 PASS 582 + 37; 8 PASS 48 / 96, Drools-poot 11 / 37; 9 PASS 216 / 12; 10
PASS JUnit 573 (570) / 73; 11 PASS 45 fragmenten, 29 slices, replay 29/29/29;
12 PASS played 188, DRL 167, JUnit 756 (748) zonder skips; 13 PASS 40 twists /
22 goldens, DRL 21; 14 PASS 135 / 2.

**De bytevergelijking** (U2; `/tmp/l5/nulmeting.sh`, vóór op main `1690aa05`,
ná op `14f1bfdb`, 3308 bestanden elk): ref 619 van 619 byte-gelijk, clingo
619/619, `ref --noop` 619/619, de replay-API exec-model 242/242 en DRL-tekst
242/242 (statusregel, waarden en redenen), rood: ref 48/48, clingo 48/48,
Drools 48/48, toka: ref 29/29, clingo 29/29, Drools 29/29, verifier 726 van
736. De tien verschillen zijn precies de toegestane: de zes WF-095-getuigen
(`wf095-bronsnippet`, `-fractiebreedte`, `-omringende-tekst`,
`-ontbrekende-seconden`, `-spatie-scheider`, `-zone-suffix`: de zin eindigt
op "belongs in `source`") en de vier WF-113-getuigen
(`wf113-marker-bron-leeg` en `-bron-null`: "Unresolved marker whose source
snippet is blank …" en "Unresolved marker without a source …" in plaats van
"Onopgelost marker whose bron is blank/null …"; `wf113-marker-zonder-verslag`
en `wf113-verslag-noemt-marker-niet`: "… contains its source snippet
verbatim"). De aanname "geen uitvoerder leest `bron`" houdt: geen enkele
uitvoer van ref, clingo, Drools, rood of toka bewoog.

## Acceptatie

Elke bouwer sluit met de genoemde gates groen op zijn eigen branch; de kaart sluit met alle
veertien groen op de samengevoegde branch (staat die op de main-tip, dan is dat de
main-run). Stand op main `1edd17e4` (de L4-registerwerkcommit; alle veertien groen op de
L4-landing) tussen haakjes. Bij L5 mogen alleen bewegen: de zelftestrijen van gate 2, de
negatief-stand en `EXPECTED_MIN_EXPECT` van gate 3, de tekst van de 11 rood-fragmenten die
een v0-naam pinnen en van de bodemzinnen waarvan de tekst in ref of clingo beweegt (gate
8), en de sleutels van de twister van gate 13. Elke andere beweging is een nulmeting.

**(0) M7 — de nulmeting, de eerste inhoudelijke commit.**

*Wat M7 is.* Een commitreeks op de kaartbranch, direct na de claimcommit, gemaakt door M7-O
en M7-J en door de hoofdsessie samengevoegd tot de M7-unie (U1): het schema met het
bronrecord (en verder de L4-vorm), `m5.py` met de stap `--stage m7`, alle vier de zones
en de 29 toka-slices door die stap, de gedeelde OCaml-lader en `checks.ml` (WF-113 (b),
de WF-095-zin), de Drools-lader, de drie emitters (`source` in plaats van `bron`, `line`
in `source`), de testsuites met ingebedde documenten door dezelfde stap, de twee
WF-113-getuigen met de hand, de M7-rijen van de naamtabel en van gate 2, en de toka-dump
die de frontend opnieuw schrijft. Geen her-emissie als bron: de emittergoldens en de
toka-dump komen uit `m5.py --stage m7`, en gates 9, 10, 11 en 14 bewijzen dat de
aangepaste emitters dezelfde bytes schrijven. Geen herpin van `regelspraak-java`.

*Wat M7 meet.* De aanname "geen uitvoerder leest `bron`": verandert een uitvoer van ref,
clingo of Drools als alleen `bron` van vorm verandert, dan leest er toch iemand.
Werkwijze (de hoofdsessie, met het script van U2):

1. Vóór M7, op de basis `1edd17e4`, elk uitvoerderartefact één keer gebouwd (ref, clingo,
   de Drools-jar; de jar één keer per run), en buiten de repository opgeslagen
   (`/tmp/l5/results/m7-voor/`): per golden case (619) de volledige uitvoer van de
   ref-runner, van clingo en van `ref --noop`; per golden de statusregel en de waarden van
   de replay-API in beide Drools-takken (exec-model en DRL-tekst), met de redenen van elke
   weigering per vorm; per rood case (48) het foutverslag van ref en clingo en, waar
   gepind (11), van Drools; per toka-slice (29) de replay door ref, clingo en Drools; de
   uitvoer van de verifier over alle 736 documenten.
2. Na M7 hetzelfde in `/tmp/l5/results/m7-na/`.
3. Bytevergelijking (`diff -r`). Toegestane verschillen: alleen meldingen van de verifier
   die `bron` noemden (WF-113 (b) op vier negatief-getuigen, de WF-095-zin op zijn
   negatief-getuigen), elk per regel genoemd. Elk verschil in ref, clingo, `--noop`,
   Drools (een van beide takken), rood of toka weerlegt de aanname: de hoofdsessie STOPT,
   legt het verschil als nulmeting in de kaart en beslist vóór M8.

*Wat M7 vastlegt.* In de kaart, onder een kop "M7 — de nulmeting" die de hoofdsessie na de
gate-run invult: de commithash; de tellingen van `m5.py --stage m7` per zone (verwacht
6107 strings in 641 documenten, 774 nullen in 103, `line` 17 opgegaan en 49 vervallen,
2 weigeringen met de hand); de veertien gates met hun getallen, waarbij gate 5 (619
replays), gate 6 (619) en gate 12 (188/167) gelijk blijven aan de L4-stand; per uitvoerder
"n van n byte-gelijk"; de toegestane verifierverschillen per regel. De meetbestanden
blijven buiten de repository.

**(a)** `python3 spec/proposals/v1-migratie/m5.py --check` meldt 0 documenten in L4- of
M7-vorm in de vier zones en de pariteitszone (de dump na de her-emissie van C1);
idempotent (een tweede `--all` wijzigt geen byte, ook niet over een M7-document); de
tellingen per zone in het Result. De weigeringen van een volledige run over de L4-basis
zijn precies `wf113-marker-bron-null` en `wf113-marker-bron-leeg` (beide in M7 met de hand
geschreven); elke andere weigering is een bevinding.

**(b)** Gate 2: metaschema-valide; alle golden- en rood-documenten (289) valideren en
round-trippen; `EXPECTED_MIN_DOCS` 192 blijft; de patroonposities verhuizen naar
`/$defs/Validity/properties/from` en `…/to` (de rij `null` wordt een weigering: een open
grens is afwezig; een rij voor de afwezige grens erbij), de patroonrijen (nu 77) naar de
gemeten stand; alle bases (`_m1_base` … `_l4_base`) op de L5-vorm en een `_l5_base` met
`L5_SHAPE_ROWS` (vormrijen nu 311): minimaal een v0-lijstsleutel onder `rules`; per
familie van de hernoemingstabel de v0-knoopnaam en een v0-sleutel op de L5-knoop; `bron`
op een knoop; `source` als string, `null`, `{}`, `{"snippet": ""}`, met `file`, met
`column`, met `line: 0`; `line` naast `source` op een consistentieversie; een
`Unresolved` (expressie en conditie) zonder `source`; per enumfamilie een v0-waarde
geweigerd; `from: null`, `host: null`, `wholePeriod: null`, `plural: null` op een `Role`,
`decision: null`; `WholePeriod` met `jaar` en met `granularity: "week"`; `the` als
kwantor van een `QuantifierFold`; en als aanvaarde rijen een `Validity` zonder grenzen,
een `TimeWindow` zonder grenzen (WF-042's), een `ContextValue` met `name: "Rekendatum"`
(het schema laat de naam vrij, WF-098 weigert hem), `source` met `line: 12`, en
`Regelversie` met `regel` en `source` (D39).

**(c)** Gate 3: alle golden- en rood-documenten verifier-schoon (289); negatief 417–418
(418 met de nieuwe getuige), elk op zijn regel én, waar `expect.txt` staat, op zijn arm;
`EXPECTED_MIN_NEGATIEF` 350 blijft; `EXPECTED_MIN_EXPECT` van 228 naar de gemeten stand
(verwacht 260) in dezelfde commit (de enige regel van `check-verifier.sh` die beweegt).

**(d)** Gate 4 ongewijzigd (667 casusbestanden; het casusformaat verandert niet, D56;
geen casusbestand noemt een wirepointer of een wirenaam, gemeten).

**(e)** Gates 5, 6, 7, 8: GEEN uitkomst beweegt. Ref-suite (976, vloer 489) en
clingo-suite (1284, vloer 83) herschreven waar zij meldingen pinnen of L4-documenten
laden, met hun tellingen; 619 replays elk; noop 582 + 37 overlevenden, lijst
ongewijzigd; rood 48 cases, 96 fragmenten, Drools-poot 11/37. De 11 fragmenten die een
v0-naam pinnen worden opnieuw gepind, per regel in het Result: `Onopgelost` 4
(`wf044-onopgelost-guard`, `wf044-onopgelost-reached`, ref en clingo), `TOTAAL` 3 en
`SOM` 1 (clingo: `datumsetbron-totaalvouw`, `datumtijdlijn-klipvouw`,
`datumtijdlijn-totaalvouw`, `datumsetbron-somvouw`), `Begrensd` 1 (clingo:
`begrensd-reads-bounds-in-order`), `Rekendatum` 2 (`tijdsduur-reads-van-before-tot`, ref
en clingo). Een bodemzin van `check-rood.sh` verandert alleen als zijn tekst in ref of
clingo verandert, per regel in het Result (verwacht één: de `dt_term`-zin van clingo
noemt `VariabeleRef`). Nulmeting van M8 zoals in L4: de uitvoer van ref, clingo en `ref
--noop` over 619 golden-, 48 rood- en 29 toka-cases byte-gelijk tussen M7 en de unie, op
de opnieuw gepinde zinnen na.

**(f)** Gates 9, 10, 14: alle emittergoldens byte-identiek na her-emissie (12 catala; 73
rgs; 2 flogic) én aan de mechanische migratie van diezelfde documenten (een verschil is
een bevinding over script of emitter); vloeren 201/12, 173/38, 81/1 blijven; suites (216,
570, 135) gemeld.

**(g)** Gate 11: `toka/casus/ir.json` opnieuw geëmitteerd door de frontend (in M7 en in
M8) en byte-identiek aan `m5.py`'s migratie van een meetkopie van de L4-dump — geen
uitzondering verwacht, elk verschil is een bevinding; 29 slices via `m5.py`; replay
29/29/29; suite ≥ 27; 45 fragmenten; holdouts `toka-23` en `toka-45` ongewijzigd tenzij
gemeten; `toka/README.md` krijgt de divergentieregel L5 (alleen paden, pointers en
tellingen).

**(h)** Gate 12: `EXPECTED_PLAYED` 188 en `EXPECTED_DRL_PLAYED` 167 na M7 en na M8
gemeten en gelijk (geen kanaalwissel verwacht); JUnit (748) zonder skips; de Drools-lader
leest de L5-vorm via de Java-vertaaltabel en weigert elke v0-naam bij naam (een getuige
per familie van checklistpunt 2, naar het model van
`RegelEnExpressielaagWeigeringWitnessTest`); de knoopsoortvloer van
`OnbekendVeldWitnessTest` (64 op L4) opnieuw gemeten; `scripts/check-drools.sh`
ongewijzigd tenzij een kanaalwissel gemeten wordt (de redenteksten in `EXPECTED_CHANNEL`
noemen in-memory recordnamen en blijven, D52).

**(i)** Gate 13: de twister op de L5-namen (`KLASSE` met `Binary`, `Let`, `Aggregation`,
`Rounding`, `Clamp`; `AFGELEIDE_OP` = `plus`, `minus`, `reducedBy`, `times`;
`AFGELEIDE_KIND` = `sum`, `total`, `minimum`, `maximum`, `first`, `last`; `GEEN_FUNCTIE`
= `duration`, `absoluteDuration`; de datumduur-arm op `Binary` met `left`/`right`; de
declaratielezing op `AttributeReference` met `subjectType`/`attribute`,
`ParameterReference`, `DimensionCellRead`); 40 twists over 22 goldens (vloer 38) en de
DRL-tak 21 (vloer 20) blijven; alle legs op hun eigen ongetwiste regel.

**(j)** Naamtabeltoets rc 0: elke levende v0-rij `geland: "M7"` (`bron`, en `line` op de
consistentieversie en de regelgroep) of `"M8"` (de rest), behalve de rijen `Regelversie` en
`regel` (D39, met een noot); de L5-namen als identiteitsrij met de noot "geland M7"
(`source`, `snippet`, `line` in het bronrecord) of "geland M8"; de drie `onzeker`-rijen
beslist (`objecttype@AttribuutRef` D49, `basis@TemporalRead` en `datum@*` D50) en de lijst
`onzeker` leeg; de gat-rijen beslist (D48, D51) met een IR-SPEC-citaat na de eerste fase
van A2; `botsingen` bijgewerkt; de nieuwe sectie `waarden` (D47) met toetspunt 9 groen;
`geland_telling` met M7 en M8.

**(k)** Vervalt: geen herpin van `regelspraak-java` (maintainerbesluit 2026-09-24: één
pin-bump als v1 compleet is). De toolchain-impact van L5 (A.8 van het toolchainrapport:
alle 12 asserties van de toolchain-`IrEmitterTest` dragen Nederlandse sleutels, de
conformancegate breekt alleen als een emitter `source.file` schrijft, wat L5 niet doet)
landt bij die ene pin-bump.

## May touch / NIET / Out of scope

May touch (per bouwer disjunct, zie de werkverdeling): `spec/schema/**`,
`spec/proposals/v1-migratie/m5.py`, `spec/proposals/v1-naamtabel.json` en
`spec/proposals/v1-naamtabel-toets.py`, `spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md`
(de genoemde plekken), `spec/verifier/**`, `corpus/**`, `toka/**` (alleen `ir.json`, de
dump en `README.md`), `ref/**`, `backends/clingo/**`, `backends/drools/**`,
`frontends/**`, en van `scripts/`: `check-ir-schema.sh` (bases en zelftestrijen),
`check-verifier.sh` (alleen `EXPECTED_MIN_EXPECT`), `check-reftype-derivation.sh` (de
twister op de L5-namen), `check-rood.sh` (alleen bodemzinnen waarvan de tekst beweegt),
`check-drools.sh` (pins en kanaallijsten, alleen bij een gemeten wissel) — elke
gate-scriptwijziging per regel in het Result; en `spec/ASP-TRANSLATION.md`, alleen Part I
en alleen de wirenamen (mandaat in deze kaart, B1, U38). NIET: `spec/CONSTRUCTS.md`,
`spec/PLAN.md`, `CLAUDE.md`, `WAY-OF-WORKING.md`, `.github/**`,
`spec/proposals/V1-WIREMIGRATIE.md` (behalve de L5-rij van de landingstabel, A2), Part II
van `spec/ASP-TRANSLATION.md`, `spec/IR-REVIEW.md`, de andere proposals, `backlog/**` (de
kaart zelf alleen door de hoofdsessie), en geen herpin of wijziging in `regelspraak-java`.

Out of scope: B24 (D38) en B25 (D39); `file` en `column` in het bronrecord en de
positieconventies (D45); `statuteAnnotations` op de defaultslaag (D44); de hernoeming van
de Java-records en de OCaml-typen (D52); een canonicalisatiepas (D56); de pin-bump van
de toolchain (fase 3); P11; het aanscherpen van v0-nullable velden zonder null-dragers en
een voorwaardelijke `threshold` of `digits` (U18, U19); de open punten 3, 4, 6, 7 en
11–14 van L4; alles wat betekenis wijzigt. Een bouwer die een
betekenisverschil meet (een golden die ná M7 of ná M8 anders uitkomt; een golden die van
gate-12-kanaal wisselt; een case waarvan de uitvoer van ref, clingo of Drools in bytes
verandert buiten een opnieuw gepinde zin) STOPT op dat punt, legt de meting in zijn
Result en gaat verder met de rest — dat is de nulmeting.

## Werkverdeling

Kaartbranch `card/l5-bronrecord-en-hernoeming` vanaf main `1edd17e4` (worktree `wt/l5`;
de kaart `backlog/doing/20260924-l5-bronrecord-en-hernoeming.md`). Zes stappen, serieel
van stap tot stap, parallel binnen stap 1 en stap 4. Elke bouwer werkt op
`card/l5-<bouwer>` in `wt/l5-<bouwer>`; de hoofdsessie voegt serieel samen (ff of rebase),
draait de gates, zet één lens op de unie (checklist + kaart + elke gewijzigde spec-regel),
fixronde op eigen branches vanaf de unie met het vondstenbestand van de lens als
werklijst, herkeuring strikt op de gemelde punten, adviesronde in dezelfde kaart zonder
derde lens. Bestanden per bouwer disjunct; een bestand dat twee bouwers nodig hebben staat
hieronder bij één van hen, en de ander vraagt erom via de hoofdsessie.

**Stap 1 — M7 (twee bouwers, één commitreeks; U1, U3).** M7-O werkt in twee fasen en M7-J
start vanaf de fase-1-commit; de hoofdsessie voegt M7-O en M7-J samen, draait de veertien
gates en de nulmeting van acceptatie (0) op die unie en vult de kop "M7 — de nulmeting" in.
A2 fase 1 (stap 2) loopt hiermee parallel op een eigen branch; A1 (stap 3) start pas na de
M7-unie (U3).

- **M7-O — het bronrecord aan de OCaml- en wirekant.** `spec/schema/uchar-ir-v1.schema.json`
  (`$defs/sourceReference`; `source` op de 48 definities met `bron`, verplicht op beide
  `Onopgelost`-definities; `line` weg van `ConsistencyVersion` en `ResolvedRegelGroep`),
  `spec/schema/README.md` (een alinea over het bronrecord), `scripts/check-ir-schema.sh`
  (de bases met `source`; de M7-rijen van acceptatie (b)),
  `spec/proposals/v1-migratie/m5.py` (de stap `--stage m7`, het contract van `m4.py`:
  `FILE...`, `--all`, `--check`, idempotent, luid met JSON-pointer, gate 2's serialiser),
  alle `corpus/**/ir.json` en `toka/cases/**/ir.json` door die stap, de twee
  WF-113-getuigen met de hand (alleen `ir.json`), de M7-rijen van
  `spec/proposals/v1-naamtabel.json`, `spec/verifier/lib/{loader,checks}.ml` (`source` in
  de ongewijzigde AST, de leesstand voor de marker zonder `source`, de lege snippet als
  laadweigering, WF-113 (b) op `snippet`, de WF-095-zin),
  `backends/clingo/test/test_clingo.ml` (de ingebedde documenten door dezelfde stap;
  laadrijen voor `bron`, de lege snippet en de marker zonder `source`),
  `ref/test/test_ref.ml` (alleen waar het een veranderde zin pint),
  `frontends/catala/lib/emit.ml` en `frontends/flogic/lib/emit.ml` met hun suites
  (`source` in plaats van `bron`; catala schrijft geen `line`, U13). Sluit met gates 1–7,
  9 en 14 op de eigen branch; gates 8, 10–13 (die een Java-poot hebben) op de M7-unie.
- **M7-J — het bronrecord aan de Java-kant**, vanaf de corpuscommit van M7-O.
  `frontends/regelspraak/src/main/java/.../emit/IrEmitter.java` (`source`, `line` in
  `source` alleen op de consistentieversie en de regelgroep, nooit `file` of `column`),
  de frontendtests met ingebedde documenten (9 bestanden dragen `bron`, 33 keer),
  `backends/drools/src/main/java/.../loader/IrLoader.java` (`source`, de marker zonder
  `source` en de lege snippet geweigerd met de zinnen van de OCaml-lader, de
  WF-095-zin), de Drools-tests met ingebedde documenten (45 bestanden dragen `bron`, 361
  keer), `toka/casus/ir.json` (door de frontend opnieuw geschreven, byte-gelijk aan
  `m5.py --stage m7` over de L4-dump). Sluit met gate 10 op de eigen branch; gates 8,
  11, 12 en 13 op de M7-unie.

**Stap 2 — A2 fase 1: de namen eerst in IR-SPEC** (spec-first: D48 "eerst opgenomen in die
tabel", D51 "alles eerst in IR-SPEC", N2 "vóór L5"), vanaf de M7-commit. In
`spec/IR-SPEC.md` de spellingsregel van N2 in de Conventions (U31) en in elke tabel de
L5-naam van knoop, sleutel en enumwaarde: `host?` in 4.5, `base`/`date` in 4.3a, de
namen van D51 (3.7, 3.8, 4.2, 4.3, 4.5, de Rules-tabel van ch. 2), de familietabellen van
N2, en de tekst van het bronrecord dat in M7 al op de wire staat (de preambule van ch. 4,
WF-113 (b), D41–D46). Een tekstcommit; sluit met gate 1. A1 start vanaf deze commit.

**Stap 3 — A1, de L5-wire**, vanaf de fase-1-commit van A2.
`spec/schema/uchar-ir-v1.schema.json` (de hernoemingstabel, de enumtabellen, D5 (a), de
`rules`-container; de schemavoorwaarden van L2–L4 op hun L5-namen),
`spec/schema/README.md` (sectie "The source record and the renaming (L5)" en de inleiding
"carries the L5 shape"), `scripts/check-ir-schema.sh` (alle bases op de L5-vorm,
`_l5_base`, `L5_SHAPE_ROWS`, de patroonposities van `Validity`), `m5.py` (de stap M8 en de
standaardrun M7 + M8), alle `corpus/**/ir.json` en `toka/cases/**/ir.json` (de dump is van
C1), `spec/proposals/v1-naamtabel.json` en `v1-naamtabel-toets.py` (elke rij geland met
een citaat uit de IR-SPEC van stap 2, de sectie `waarden` met toetspunt 9; de citaten die
stap 4 nog laat bewegen in de laatste fase, les (j)). Sluit met gates 2 en 4 en de
naamtabeltoets; rapporteert per zone de tellingen en elke weigering.

**Stap 4 — parallel vanaf de kop van A1**, met twee afhankelijkheden:

- **A2 fase 2 — verifier en de rest van de spec.** Als EERSTE commit
  `spec/verifier/lib/loader.ml` op de L5-namen met de OCaml-vertaaltabel als eigen module
  in de gedeelde bibliotheek (voorstel `spec/verifier/lib/spelling.ml`, U5), gebouwd,
  met hash in een tussentijds Result — B1 en B2 starten vanaf die commit. Daarna
  `spec/verifier/lib/{checks,desugar}.ml` (meldingen, pointers),
  `scripts/check-verifier.sh` (alleen `EXPECTED_MIN_EXPECT`), de rest van
  `spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md` (de genoemde plekken, Engels, ZELF
  geschreven), `spec/verifier/README.md`, van `corpus/negatief/`: de herschrijving van
  `wf098-contextwaarde-naam-kapitalisatie/ir.json` en de nieuwe map
  `wf098-v0-naam-rekendatum/` (met `NOTE.md` en `expect.txt`), de armmeting per getuige
  als lijst voor N (L4-verifier op de L4-vorm tegen de L5-verifier op de L5-vorm), en de
  L5-rij van de landingstabel in `V1-WIREMIGRATIE.md` ("loopt"). Sluit met gate 3 (en 2,
  4).
- **B1 — ref en clingo**, vanaf de eerste commit van A2 fase 2. `ref/lib/*.ml` en
  `backends/clingo/lib/*.ml` (elke melding die een wireconstructie noemt, in L5-spelling:
  `bronRol`/`bronPad` → `sourceRole`/`sourcePath`, `Onopgelost` → `Unresolved`, "the
  Rekendatum context value" → "the evaluationDate context value", `Begrensd` → `Clamp`,
  `SOM`/`TOTAAL` → `sum`/`total`, …), `ref/test/test_ref.ml`,
  `backends/clingo/test/test_clingo.ml` (973 ingebedde documenten door `m5.py` over de
  tekstblokken waar ze heel zijn; laadrijen voor de v0-namen), `backends/clingo/README.md`,
  Part I van `spec/ASP-TRANSLATION.md` (de wirenamen, 65 v0-namen in backticks; Part II
  blijft), de 11 fragmenten in `corpus/rood/**/case*.json`, `scripts/check-rood.sh` (alleen
  bodemzinnen waarvan B1 de tekst verandert). Sluit met gates 5, 6, 7, 8 (gate 8 met de
  Drools-poot op een proefunie).
- **B2 — catala, flogic en de twister**, vanaf de eerste commit van A2 fase 2.
  `frontends/catala/lib/{emit,translate}.ml` en `frontends/catala/test/**`,
  `frontends/flogic/lib/{emit,lower}.ml`, `frontends/flogic/test/**` en
  `frontends/flogic/README.md` (de L5-vorm in canonieke volgorde via de OCaml-vertaaltabel;
  gemeten in `emit.ml`: catala 21 gequote v0-knoopnamen, 76 v0-sleutels, 39 v0-waarden;
  flogic 13 / 64 / 35), `scripts/check-reftype-derivation.sh` (de twister van acceptatie
  (i)). Sluit met gates 9 en 14; gate 13 op de unie.
- **C1 — de Java-emitter en de toka-dump**, vanaf de kop van A1. Als EERSTE commit de
  Java-vertaaltabel als eigen klasse in de gedeelde recordmap van de frontend
  (`frontends/regelspraak/src/main/java/.../ir/`, U5), met hash in een tussentijds
  Result — C2 start vanaf die commit —; daarna `emit/IrEmitter.java` (de L5-vorm; de tabel
  in plaats van de 16 `.name()`-aanroepen op een wirewaarde; D5 (a); de `rules`-sleutels),
  `frontends/regelspraak/src/test/**` (`IrEmitterTest` en de dertien andere bestanden met
  ingebedde documenten), `toka/casus/ir.json` (opnieuw geëmitteerd), `toka/README.md`
  (divergentieregel L5). Sluit met gate 10; gate 11 op een proefunie.
- **C2 — de Drools-lader**, vanaf de eerste commit van C1.
  `backends/drools/src/main/java/.../loader/IrLoader.java` (strikt op de L5-vorm, de
  Java-vertaaltabel in plaats van `enumOf`/`valueOf` op een wirewaarde, elke v0-naam bij
  naam geweigerd, afwezig = het v0-`null`), `schema/ContractValidator.java` alleen als het
  een wirenaam noemt, de transpilers alleen waar zij een wirenaam lazen (verwacht: geen,
  D52), `backends/drools/src/test/**` (60 bestanden met ingebedde documenten; een
  weigeringsgetuige per familie), `backends/drools/README.md`, `scripts/check-drools.sh`
  alleen bij een gemeten wissel. Sluit met gate 12.

**Stap 5 — N, de corpusteksten**, vanaf de `checks.ml`-commit van A2 fase 2 (de
L5-meldingen staan). `corpus/negatief/*/NOTE.md` en `corpus/negatief/*/expect.txt`
(behalve de nieuwe map van A2): de NOTE-citaten opnieuw uit de echte L5-verifieruitvoer
(224 negatief-NOTE's citeren een pointer met een v0-segment), het NOTE-proza in L5-namen
of met een geschiedeniskader (203 negatief-NOTE's noemen een v0-naam in backticks, 666
keer), de 38 `expect.txt` die een v0-naam noemen opnieuw gepind, een `expect.txt` voor
elke gestempelde getuige zonder (31), `corpus/negatief/README.md` (sectie "L5");
`corpus/golden/**/NOTE.md` (98 noemen een v0-naam in backticks, 501 keer; 7 citeren een
pointer met een v0-segment) en `corpus/rood/**/NOTE.md` (25, 93 keer). Mag splitsen in N1
(negatief) en N2 (golden en rood), disjunct per zone (U4). Sluit met gates 1 en 3.

**Stap 6 — de unie**: gates 1–14, de nulmeting van M8 (acceptatie (e)), de lens, de
fixronde (de fixers per disjuncte bestandsgroep, plus een aparte NOTE-fixer voor het
NOTE-proza), herkeuring, adviesronde.

Elke bouwer volgt het regime in `/tmp/l5/regime.md` (door de hoofdsessie klaargezet); hij
commit klein en vaak met een bouwbare branch; schrijft zijn Result tussentijds naar een
bestand buiten de repository (`/tmp/l5/results/<bouwer>.md`) en werkt het bij na elke
afgeronde stap; geen `git add -A`; geen kaartjes in `backlog/new` (restpunten als
genummerd open punt in het Result); nooit pushen of mergen; CONSTRUCTS/PLAN/CLAUDE.md
nooit; loopt de lens-checklist na en noemt per punt wat hij mat; eindigt met een
Result-blok (stand, tellingen, gate-uitvoer, scriptregels, open punten) — ook op zijn
contextgrens, na een commit van wat staat.

**Lessen van L2, L3 en L4 (U10 van L3 en de lessen van L4, letterlijk overgenomen, bindend
voor deze kaart):**

- **(a)** Tijdens de bouw landt de hoofdsessie GEEN andere kaart op main.
- **(b)** A2 schrijft de spec-tekst ZELF, zonder fork — de lens leest elke gewijzigde
  spec-regel.
- **(c)** Elke bouwer krijgt bij een fixronde het vondstenbestand van de lens als
  werklijst en werkt op een eigen branch vanaf de unie (disjuncte bestanden; de
  hoofdsessie rebaset serieel), plus een aparte NOTE-fixer voor het NOTE-proza (in L4 aan
  niemand toegewezen).
- **(d)** Een bouwer die een betekenisverschil meet STOPT op dat punt en meldt het (de
  nulmeting).
- **(e)** Elke nieuwe zin die een uitvoerder weigert, staat letterlijk gelijk in verifier,
  gedeelde OCaml-lader, ref, clingo en Drools, ZONDER regelnummerstaart in de
  uitvoerders.
- **(f)** Geen besluitnummers (D/U) in IR-SPEC, RUNTIME-SEMANTICS, schema, scripts of
  NOTE's — verwijs naar B-delta's, CONSTRUCTS-rijen en deze kaart (de negatief-README mag);
  het regime voegt code-commentaar toe.
- **(g)** Bouwers schrijven hun Result tussentijds naar een bestand en committen klein —
  bouwer C van L4 liep op zijn contextgrens; daarom per uitvoerder een eigen bouwer (A1
  wire; A2 verifier/spec in twee fasen; B1 ref + clingo; B2 catala + flogic + twister; C1
  IrEmitter + frontendtests + toka-dump; C2 Drools-lader + Drools-tests), en in L5 ook M7-O,
  M7-J en N, met de disjuncte bestanden hierboven.
- **(h)** Bij de landing eerst `git add` van de kaart, dan `git mv`.
- **(i)** De verifier-lader heeft een leesstand (`Verifier` laadt wat de verifier moet
  beoordelen, `Executor` weigert met de kernzin); nieuwe lexeem- of vormregels volgen dat
  patroon — in L5 de marker zonder `source` (WF-113 (b)), `the` op een `QuantifierFold`
  (WF-108 (a)) en `error`/`info`/`null` als severity (WF-113 (a)).
- **(j)** De naamtabel (A1) beweegt mee met elke spec-zin die A2 herschrijft: A1 zet de
  citaten in de laatste fase, of de hoofdsessie op de unie. Dat geldt ook voor de
  `v0 name:`-regels van IR-SPEC, die de toets leest (toetspunt 3).

## Registerwerk bij de landing (hoofdsessie)

`spec/CONSTRUCTS.md`, scopematrix: rij 3 (het bronrecord geland: `source {snippet,
line?}` op elke knoop die een brontekst had, verplicht op `Unresolved`; `file`, `column`
en de positieconventies wachten op de positiekaart; `statuteAnnotations` op de
defaultslaag en de machineleesbare wetsbron-id blijven open), rij 16 (`Unresolved` met
verplichte `source`, WF-113 (b) op de snippet; de gestructureerde reden blijft open, B24),
rij 1 (soorten en operatoren in L5-spelling: `Aggregation.kind`, `BuiltinCall.kind`,
`Binary.op`), rij 2 (`presentation` ∈ `equation`, `calculation`), rij 4
(`AttributeReference {attribute, subjectType, path}`, `RoleBinding {factType, role}`,
`CharacteristicCheck.navigation`), rij 11 (`Timeline`; `StartPointDetermination
{timeline, objectType, member}`, B28 niet genomen), rij 14 (`RuleGroup`, `line` in
`source`), rij 17 (`method` ∈ `equalShares`, `proportional`, `ordered`), rij 18
(`VERMINDERD_MET` in de notities wordt `reducedBy`), rij 19 en 20 (`source` op het
versierecord, bij de consistentie met `line`), rij 23 (`DayKindDefinition {dayKind,
condition, validity}`), rij 25 (`evaluationDate`, `evaluationYear`), rij 27 (`Text`,
`Boolean`, `Enumeration {domain, values}`), rij 28 (`Dimension`, `DimensionCellRead`,
`DimensionSelection`), rij 29 (`WholePeriod {granularity, negated}`), rij 30
(`Regelversie` houdt zijn v0-vorm tot de paritygolden, B25), rij 31 (`TextComposition`,
`LiteralChunk`, `EmbeddedExpression`), rij 34 (`Rounding` en zijn vijf modi), rij 40
(`QuantifierFold` op de wire, de kwantoren in L5-spelling). Knoopinventaris: 2.1
`TekstReeks` (+ `Deel`), `Binair` met de twee `BinOp`-rijen, de drie `FunctieKind`-rijen
(L5-spelling), `DimensieSelectie`, `Afgerond` + `AfrondModus`, `Begrensd`,
`AttribuutRef`, `DimensieCel`, `ParameterRef`, `VariabeleRef`, `Constante` (de namen van
`ContextValue`), `Onopgelost` (Expr); 2.2 `Vergelijking` + `Operator`, `Predicaat` met
de twee `Predicaatsoort`-rijen, `Kenmerk` (`host`), `Periode`, `Samengesteld` met de twee
`Kwantor`-rijen, `Regelversie` (ongewijzigd, D39), `Onopgelost` (Conditie),
`GeheelPeriode`; 2.3 `Soort`, `Methode`, `ResolvedObjectCreatie` (+ `Toekenning` →
`MemberInit`), `ResolvedConsistentie` en `ResolvedUniciteit(Unie)` (`source`),
`ResolvedDagsoort`, `ResolvedStartpunt`, `ResolvedRegelGroep`, `Geldigheid`; 2.4
`KenmerkSoort`, `Feittype`/`Rol`, `Granulariteit`, `Tijdlijn`, `Dimensie`, `Numeriek` +
`Teken`, `Tekst`/`Booleaan`, `Enumeratie`, `Severity`/`Diagnostic`; §4 (de
productie-inventaris van `Onopgelost`): één zin dat de wire de marker sinds L5
`Unresolved` noemt.

`spec/PLAN.md` §9 (11): L5 geland met datum, hash en kaart — het bronrecord (B26) als
nulmeting in de eerste commit (met haar uitkomst), de hernoeming (B6, B10) met de
spellingsregel van N2 en de namen van D48–D51, null → afwezig op de hernoemde knopen; de
v1-wiremigratie is daarmee compleet; de ene pin-bump van de toolchain (fase 3) is
opeisbaar; de `toka/`-zone blijft tot de publieke flip.

`spec/proposals/V1-WIREMIGRATIE.md`: de L5-rij van de landingstabel (A2 zet "loopt", de
hoofdsessie GELAND met hash en kaart, met B24, B25 en de posities als "niet mee" en hun
reden); in de tabel van fase 2 M7 en M8 als geland; een afsluitende alinea van fase 2: de
wire `uchar-ir-v1` draagt geen v0-naam meer, op `Regelversie` na (V13, 0 dragers); de
additieve open punten (de reden van `Unresolved`, de posities in het bronrecord,
`statuteAnnotations` op de defaultslaag, de aanscherping van nullable velden); fase 3
(T1–T4) als één pin-bump; P11 bij de publieke flip.

`CLAUDE.md` (beschermd; gemeten, voorstel per gate): gate 8 noemt "`Onopgelost` reached in
evaluation" → `Unresolved`; gate 12 noemt "an objectcreatie" en "the wederkerig
self-relation" (`wederkerig` is de v0-sleutel van `FactType.reciprocal`) → "an object
creation", "the reciprocal self-relation"; gate 13 noemt in de L2-geschiedenis
"`AttribuutRef`, `ParameterRef`, `DimensieCel` and `BinderRef`" → de L5-namen
(`AttributeReference`, `ParameterReference`, `DimensionCellRead`, `BinderRef`) of "(v0
names)" erachter, en "TIJDSDUUR's own unit" → "the `duration` builtin's own unit"; gate 2
("`regelspraak-ir-v0` until then") is geschiedenis en blijft; de bestandsnamen
`bron.rgs`, `bron.catala_en` en `bron.flogic` in gates 9, 10 en 14 zijn namen van
bronbestanden in het corpus, geen wiresleutels, en blijven. Daarna de memory.

