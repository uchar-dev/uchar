> **Herkomst.** Dit review is op 2026-08-14/15 geschreven in de
> `regelspraak-java`-repo (HEAD `b85dff3`) en hier ongewijzigd overgenomen als
> founding document van het uchar-programma. Alle bestandspaden en
> regelnummers verwijzen naar die repo op die commit. Het bijbehorende
> vervolgplan is `PLAN.md` in deze map.

# IR-review — RegelSpraak-java als kandidaat voor een engine-neutrale semantische kern

*Beoordeeld: de IR onder `src/main/java/io/github/yhavinga/regelspraak/ir/` (14 klassen, ±800
regels), het declaratiemodel `model/RegelSpraakModel.java` + `model/Datatype.java`, en de
consumenten (`transpiler/`, `runtime/`, `schema/`). Peildatum van dit review: 2026-08-14,
HEAD `b85dff3`.*

## (a) Samenvatting

1. **Semantiek vs. syntax — goed.** De IR modelleert betekenis: getypeerde expressiebomen,
   gebonden objecttypen, geëxpandeerde dimensieselecties, gedesugarde beslistabellen. Restanten
   van oppervlaktesyntax (naam-strings, meervoud-heuristiek) zitten in de bindlaag, niet in de kern.
2. **Engine-neutraliteit — matig.** De IR is aantoonbaar tweemaal consumeerbaar, maar beide
   back-ends zijn Drools; het §9.4 referentieveld-factmodel, de agenda-semantiek en de
   Java-runtime-als-betekenisdrager zitten in de IR gebakken. Een Clingo-backend stuit op alle drie.
3. **Uitzonderingen en prioriteit — matig.** Regelversies met geldigheid zijn declaratief;
   negatie-stratificatie, het initialisatie-default en de fixpoint-semantiek zijn procedureel
   (salience, leeg-guard) en staan buiten de IR.
4. **Tijd — goed, binnen één as.** Regelversie-geldigheid, tijdsafhankelijke attributen met
   granulariteit/startpunt en parametervintages zijn gemodelleerd en fail-fast. Bitemporaliteit
   (kennisdatum naast geldigheidsdatum) ontbreekt; temporaliteit zit niet in het typesysteem.
5. **Volledigheid en traceerbaarheid — matig.** Elke expressie draagt haar bronsnede, en gaten zijn
   eerlijk gemarkeerd (`Onopgelost`, Dekking-manifest). Maar regelnummer/bestand ontbreken op
   vrijwel elke IR-node, en wetsartikel-annotaties worden weggegooid.
6. **Spec-kwaliteit — ontbreekt.** Er is geen normatieve IR-specificatie los van de Java-code; de
   (rijke, §-geciteerde) javadoc plus `docs/grammar-divergences.md` ís de spec. De IR heeft geen
   serialisatievorm en dus geen taalonafhankelijk koppelvlak.

## (b) Bevindingen per punt

### 1. Semantiek vs. syntax

De IR is een geresolvede, getypeerde AST — geen parse-tree-spiegel:

- Elke `Expr`-node draagt zijn resultaat-`Datatype` volgens de typeringen-matrix plus de bronsnede
  (`ir/Expr.java:25-29`). Referenties zijn gebonden: `AttribuutRef` kent het objecttype, het
  navigatiepad in **gedeclareerde** rolnamen en de set/scalar-status (`ir/Expr.java:159-181`);
  `ParameterRef` wijst naar de gedeclareerde parameter (`ir/Expr.java:193-195`).
- Oppervlaktevormen worden genormaliseerd vóór de IR: een dimensie-interval "vanaf a t/m b" is al
  geëxpandeerd tegen de gedeclareerde labelvolgorde tot een concrete labellijst
  (`ir/Expr.java:98-104`); beslistabellen (§12) worden gedesugard tot gewone regels
  (`resolver/BeslistabelDesugar.java`); regelversies worden per versie een eigen `ResolvedRegel`
  met eigen `Geldigheid` (`ir/ResolvedRegel.java:22-25`).
- De regelsoorten zijn semantische categorieën, geen zinsvormen: gelijkstelling/berekening/
  initialisatie/kenmerktoekenning (`ir/ResolvedRegel.java:24`), aparte IR-vormen voor
  objectcreatie, feitcreatie, verdeling, consistentie, uniciteit, dagsoortdefinitie en
  startpuntbepaling (`model/RegelSpraakModel.java:33-55`).

Waar de oppervlakte wél doorsijpelt:

- Leden blijven **naam-strings**: `AttribuutRef.attribuut`, `Kenmerk.kenmerk`, de sleutels van
  `ResolvedUniciteit` zijn Strings die consumenten case-insensitief opnieuw opzoeken
  (`model/RegelSpraakModel.java:505-511`). Er is geen symbolische identiteit (een verwijzing naar
  het gedeclareerde lid zelf); binding is dus half — het objecttype is gebonden, het lid niet.
- Naamresolutie met Nederlandse morfologie (meervoud-heuristiek `+s/+en`, lidwoord-strippen,
  kop-zelfstandig-naamwoord zoeken) leeft ín het model en wordt ook op IR-consumptietijd nog
  aangeroepen (`model/RegelSpraakModel.java:205-223`, `399-420`) — taalverwerking in de
  semantische laag.
- `AttribuutRef.navigatieRol` is per javadoc "the role name the author WROTE"
  (`ir/Expr.java:150-157`) en `onderwerpBron` is rauwe brontekst — bewust, maar het koppelt de
  IR aan de geschreven vorm.

### 2. Engine-neutraliteit

Sterk: er zijn al twee back-ends over exact dezelfde IR, met een gedeelde, "pure, Drools-free"
kern en een dekking-manifest dat per construct LOWERED/MARKER/SKIP vastlegt
(`docs/TRANSPILER-ARCHITECTURE.md:10-17`, `transpiler/kern/Dekking.java:17-19`). Dat bewijst
her-consumeerbaarheid. Maar beide back-ends zijn Drools, en de Drools-aannames zijn tot in de
IR-definities zichtbaar:

- De IR beschrijft zichzelf in Drools-termen: `Expr` is "the input the Drools transpiler will
  consume" (`ir/Expr.java:11-12`), `Conditie` is "the 'when' side of the rule the Drools
  transpiler will emit" (`ir/Conditie.java:8-10`), `Functie.elementObjecttype` is "what the
  transpiler binds the accumulate source pattern to" (`ir/Expr.java:82-84`).
- Het **§9.4 referentieveld-factmodel** (to-one-referentie op de meerdere-zijde) is
  IR-semantiek geworden: `Rollidmaatschap` "lowers to a bare null-test on that reference field"
  (`ir/Conditie.java:75-85`), `VerreRollidmaatschap` is per definitie een exists-join die alleen
  als top-level-conditie gestructureerd wordt — een beperking die uit de Drools-patroonmachinerie
  komt, niet uit RegelSpraak (`ir/Conditie.java:87-93`). Feitcreatie draagt veldnamen
  (`zetVeld`/`joinVeld`, `ir/ResolvedFeitCreatie.java:31-32`), uniciteit een `scopeVeld`
  (`ir/ResolvedUniciteit.java:26`). Een ASP-backend wil hier feittype + rolbinding zien, geen
  bean-veldnaam.
- De **runtime is de betekenisdrager**: leegwaarde-discipline, §6-rekenkunde, tijdlijn-algebra en
  de verdeling leven uitsluitend als Java in `runtime/RegelSpraakRuntime.java`
  (`docs/TRANSPILER-ARCHITECTURE.md:27-30`); de op-volgorde-verdeling is een imperatieve helper
  die "the whole allocation in one shot" berekent (`ir/ResolvedVerdeling.java:18`). Beide huidige
  back-ends *delen* die runtime; een niet-JVM-backend kan dat niet, en er bestaat geen
  declaratieve beschrijving van deze semantiek om tegen te herimplementeren.
- **Evaluatiemodel**: forward-chaining tot fixpoint met mutabele facts (`modify`-sets,
  `ir/ResolvedFeitCreatie.java:13`), negatie-stratificatie via agenda-salience
  (`transpiler/DrlTranspiler.java:233-236`, `1045-1047`). Voor Clingo is stratificatie juist
  triviaal declaratief — maar de IR zégt nergens dat negatie over afgeleide kenmerken
  gestratificeerd moet zijn; dat weet alleen de back-end.
- **Leegwaarde/gesloten wereld**: "leeg = 0" in rekenkunde en de initialisatie-leeg-guard zijn
  vastgelegd in `docs/grammar-divergences.md:1671` en `:1778` en gecodeerd in de runtime — een
  drie-waardige semantiek die een ASP-encoding expliciet nodig heeft, maar die nergens als
  IR-eigenschap is gedefinieerd.
- Mild: `Geldigheid` en literalen gebruiken `java.time.LocalDate`/`BigDecimal` — voor een
  in-memory-IR onvermijdelijk, maar zonder serialisatievorm is zelfs dát Java-gebonden.

### 3. Uitzonderingen en prioriteit

- **Declaratief geregeld**: regelversies. Elke versie is een eigen `ResolvedRegel` met een
  `Geldigheid`-venster; selectie gebeurt tegen de Rekendatum (`ir/Geldigheid.java:6-13`);
  overlappende versies geven een diagnostiek (`resolver/RuleChecker.java:182`).
- **Procedureel geregeld**: al het andere. RegelSpraak v2.3.0 kent geen expliciete
  uitzonderings-/tenzij-constructie, maar de facto defeasibility zit er wel in en staat buiten de
  IR: (i) een initialisatie schrijft alleen bij leeg doel — een default die door elke andere
  regel verslagen wordt — als leeg-guard in beide back-ends (`docs/grammar-divergences.md:1778`);
  (ii) een regel die een genegeerd kenmerk leest zakt naar salience −1 zodat alle producenten
  eerst vuren (`transpiler/DrlTranspiler.java:1045-1047`) — stratificatie als agenda-truc;
  (iii) de fixpoint-/freshness-semantiek van een berekening staat alleen in
  `docs/grammar-divergences.md:1865`.
- **Conflicten zijn niet gerepresenteerd**: twee regels die hetzelfde attribuut schrijven hebben
  in de IR geen onderlinge ordening, prioriteit of conflictmarkering. De producent/consument-graaf
  bestaat wél als aparte analyse (`graaf/ProducentConsument.java`) maar voedt de IR niet.

### 4. Tijd

Drie lagen, elk aanwezig:

- **Regelversie-geldigheid**: `Geldigheid` met concrete, inclusieve datumgrenzen (jaar-vormen
  verruimd tot 1-1/31-12, `ir/Geldigheid.java:11-13`).
- **Tijdsafhankelijke waarden**: granulariteit, tijdvak-aantal en tijdlijnnaam per attribuut en
  per kenmerk (`model/RegelSpraakModel.java:470-493`), benoemde tijdlijnen met variabel startpunt
  plus de §9.9-ankersystematiek (`ir/ResolvedStartpunt.java`,
  `model/RegelSpraakModel.java:111-140`); runtime-representatie `List<Segment>` met
  knip-merge-algebra (`schema/FactModelGenerator.java:50,110-128`, `runtime/RegelSpraakRuntime.java:639-786`).
- **Parametervintages**: geldigheids-geversioneerde waarden, per verzoek geselecteerd tegen één
  peildatum, fail-fast op overlap én op gaten — nooit een stille default
  (`model/Parameterset.java:12-27`, `95-107`).

Grenzen:

- **Eén tijd-as.** De peildatum is tegelijk de rekendatum en de vintage-selector
  (`model/Parameterset.java:14-16`). Er is geen kennisdatum-as: "wat wisten we op datum K over
  de toestand op datum G" (terugwerkende kracht, herzieningen) is niet uitdrukbaar. Voor
  wetgeving die met terugwerkende kracht wijzigt is één as op termijn te weinig; niets in het
  repo documenteert dit als bewuste scope-grens (de genoemde backlog-card is git-ignored en dus
  niet controleerbaar).
- **Temporaliteit zit niet in het typesysteem.** `Datatype` (`model/Datatype.java:14-16`) kent
  geen tijdlijn-vorm: een tijdsafhankelijk `Bedrag` heeft type Numeriek, identiek aan een scalair
  bedrag; dat het een tijdlijn is hangt in zijtabellen (`attribuutGranulariteit` e.d.). Elke
  consument moet die tabellen er zelf bij raadplegen; een `Expr` over een tijdlijn is aan zijn
  type niet te herkennen. Het gevolg is zichtbaar: een tijdsafhankelijke read op een
  scalar-positie kan alleen als marker afgevangen worden (`docs/grammar-divergences.md:914`).

### 5. Volledigheid en traceerbaarheid

- **Bronsnede: ja.** Elke `Expr`- en `Conditie`-node draagt `bron`, de whitespace-genormaliseerde
  bronsnede (`ir/Expr.java:28-29`, `ir/Conditie.java:23-24`); de uitleg-verbalisator bouwt daarop.
- **Positie: nee.** Regelnummer/kolom/bestandsnaam ontbreken op `Expr`, `Conditie`,
  `ResolvedRegel`, `ResolvedVerdeling` en de meeste andere IR-vormen; alleen
  `ResolvedConsistentie` en `ResolvedRegelGroep` dragen een `line`, en diagnostics dragen een
  regelnummer (`model/RegelSpraakModel.java:691`). Bij multi-file-invoer (`make scenario-test
  FILES="a.rgs b.rgs"`) is een IR-node niet naar zijn bronbestand terug te leiden. Voor audits is
  "de tekst van de snede" zwakker dan "bestand + positie": identieke snedes komen meermaals voor.
- **Wetsverwijzingen: weggegooid.** `// Bron:`-annotaties worden door de lexer geskipt, dus de
  uitleg kan geen wetsartikelen citeren — in het repo zelf erkend (`README.md:110-113`).
- **Uitdrukkingsgaten zijn expliciet — een sterk punt**: `Expr.Onopgelost`/`Conditie.Onopgelost`
  (`ir/Expr.java:203-204`, `ir/Conditie.java:159-160`) plus het Dekking-manifest met verplichte
  WARNING per niet-gelowerde constructie (`transpiler/kern/Dekking.java:38-46`). Wat de IR nu
  niet (volledig) kan uitdrukken, gedocumenteerd in `docs/grammar-divergences.md`: het
  "is gevuurd"-regelpredicaat (symmetrisch geweigerd, `:2158`), kenmerk-membership buiten het
  subject (subselectie-predicaten blijven `Onopgelost`, `:494` en `ir/Expr.java:94-97`), een
  scalaire read door een meerdere-rol (`:278`), concatenatie als losse waarde (`:1121`),
  meervoudige rangtelwoord-binding (`:1949`), de gehele-periode-check op een scalar-positie
  (`:914`), en de restvormen van verdeling (`Methode.COMPLEX`, `ir/ResolvedVerdeling.java:47-53`).

### 6. Spec-kwaliteit

- **De Java-code ís de spec.** Er bestaat geen normatief IR-document: `docs/
  TRANSPILER-ARCHITECTURE.md` (52 regels) beschrijft de lagenrolverdeling, niet de IR-inhoud;
  `docs/grammar-divergences.md` legt semantische béslissingen vast maar geen IR-structuur. Een
  schema (JSON Schema o.i.d.) ontbreekt; `schema/` betreft het fact-model, niet de IR.
- **Geen serialisatievorm.** De `Resolver`-CLI kent `--drl`, `--facts`, `--graaf`
  (`resolver/Resolver.java:125-129`) maar geen `--ir`: de IR bestaat uitsluitend in-memory tussen
  resolver en transpiler. Zonder tekstvorm is er geen golden-corpus van (bron → IR)-paren, geen
  diff-baar koppelvlak, en kan een niet-JVM-backend letterlijk nergens op aansluiten.
- Verzachtend: de javadoc is uitzonderlijk — vrijwel elke node citeert de spec-§ en definieert
  randgevallen (bijv. `ir/Conditie.java:95-102` over inclusieve/exclusieve grenzen). Als die
  discipline naar een taalonafhankelijk document wordt getild, is de afstand tot een echte
  IR-specificatie klein.

## (c) De vijf belangrijkste verbeteringen (in volgorde van impact)

1. **Geef de IR een normatieve specificatie en een serialisatievorm.**
   Nu is er niets waar een tweede DSL-frontend of een niet-JVM-backend op kan aansluiten.
   Doelstructuur: `docs/IR-SPEC.md` (per nodesoort: structuur, typering, semantiek — de bestaande
   javadoc als vertrekpunt) + een JSON-schema + `Resolver --ir out.json`, met een golden-corpus
   van (`.rgs` → IR-JSON)-paren in de gate-set zodat de Java-records een implementatie van de
   spec worden in plaats van de spec zelf. Dit is de voorwaarde voor vrijwel alles hieronder.

2. **Specificeer de runtime-semantiek declaratief, los van `RegelSpraakRuntime`.**
   Leegwaarde-discipline ("leeg = 0", de verminderd-met-uitzondering), §6-rekenkunde en
   -afronding, tijdlijn-algebra (knip-merge, tijdsevenredig), en de verdeling — inclusief
   op-volgorde als recurrente vergelijking over groepen in plaats van "één helper-aanroep".
   Doelstructuur: `docs/RUNTIME-SEMANTICS.md` met per operator de vergelijkingen en waarheids-
   tabellen (zoals de spec §6 zelf), gekoppeld aan de al bestaande absolute-waarde-pins van de
   differential. Neem hier óók het evaluatiemodel op: fixpoint-semantiek, de stratificatie-eis
   bij negatie over afgeleide kenmerken (nu een salience-truc in
   `DrlTranspiler.java:1045-1047`), en het initialisatie-default als expliciet defeasible
   default. Precies dit document is wat een Clingo-encoding nodig heeft.

3. **Ontkoppel de IR van het referentieveld-factmodel: relaties eerste-klas.**
   Vervang veldnaam-parameters (`zetVeld`/`joinVeld` in `ResolvedFeitCreatie`, `scopeVeld` in
   `ResolvedUniciteit`, `backref` in `VerreRollidmaatschap`) door een symbolische rolbinding.
   Doelstructuur: `record RolBinding(Feittype feittype, Rol rol)`; `Rollidmaatschap` en
   navigatie verwijzen naar rolbindingen, en de afleiding rolbinding → bean-veldnaam verhuist
   naar `schema/`/de back-ends. In ASP wordt een feittype dan direct een predicaat
   `feit(rol1, rol2)`; ook de top-level-only-beperking van `VerreRollidmaatschap` kan dan een
   back-end-beperking worden in plaats van een IR-beperking.

4. **Volwaardige bronkoppeling op elke IR-node.**
   Doelstructuur: vervang de kale `String bron` door
   `record Bron(String bestand, int regel, int kolom, String snede)` op `Expr`, `Conditie` en
   elke `Resolved*`-vorm, en laat de lexer `// Bron:`-annotaties op een hidden channel door zodat
   de uitleg wetsartikelen kan citeren (het in `README.md:110-113` al benoemde vervolg). Dit
   maakt audits knoopvast en de uitleg juridisch citeerbaar — de kernbelofte van een
   wetsuitvoerings-IR.

5. **Haal tijd het typesysteem in en benoem de tijd-assen.**
   Doelstructuur: een `Datatype.Tijdlijn(Datatype element, Granulariteit gran, int aantal,
   Startpunt start)` zodat een tijdsafhankelijke expressie aan zijn type herkenbaar is en de
   zijtabellen (`attribuutGranulariteit`, `attribuutTijdlijnNaam`, …) uit `Objecttype` kunnen
   verdwijnen; markers als "tijdsafhankelijke read op scalar-positie" worden dan typefouten in
   de resolver. Leg daarnaast expliciet vast (in de IR-spec van punt 1) dat het model één
   tijd-as heeft — peildatum = rekendatum = vintage-selector — en ontwerp de plek waar een
   tweede as (kennisdatum, voor terugwerkende kracht) later bij kan, zodat die uitbreiding geen
   herfundering wordt.

## (d) Open vragen

Bedoelingen die niet uit het repo af te leiden waren:

1. **Is de IR bedoeld als stabiel, publiek koppelvlak of als intern detail?** Niets in het repo
   claimt het een of het ander; het ontbreken van een serialisatievorm suggereert "intern", de
   review-opdracht ("universal characteristic") suggereert "publiek". De ambitie van meerdere
   DSL's/backends (Clingo) is nérgens in het repo terug te vinden — die komt uit de opdracht.
2. **Is de één-as-tijdsemantiek een bewuste scope-keuze of een gat?** `Parameterset.java:16`
   verwijst naar een backlog-card (`backlog/new/20260705-15:18-…`), maar `backlog/` is
   git-ignored en dus niet in te zien.
3. **Zijn de `Onopgelost`-nodes een permanente uitdrukkingsgrens of een bouwfase?** De javadoc
   zegt "cannot **yet** bind" (`ir/Expr.java:20-23`), maar het Dekking-manifest behandelt ze als
   eerste-klas verdict — tijdelijk of definitief maakt uit voor waar een tweede backend rekening
   mee moet houden.
4. **Wat is het beoogde semantische verschil tussen `BEREKENING` en `GELIJKSTELLING` in de IR?**
   Beide zijn doel := waarde (`resolver/RuleChecker.java:414`); alleen
   `docs/grammar-divergences.md:1865` ("een berekening is een equation") hint op een verschil,
   maar de IR zelf onderscheidt ze niet in gedrag.
5. **Moet de §9.10-regelgroep (een gedocumenteerde extensie buiten de spec,
   `ir/ResolvedRegelGroep.java`) tot de universele kern behoren, of blijft die
   ALEF-/implementatiespecifiek?**
6. **Blijft het Nederlands de canonieke naamruimte van de IR?** Voor dit repo is dat een bewuste
   keuze (`README.md:138-143`); voor een kern waarop meerdere DSL's aansluiten is de vraag of
   node- en rolnamen taalgebonden mogen blijven onbeantwoord.
