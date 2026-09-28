# drools-beannaamruimte-botsing: attribuut en kenmerk delen één getterruimte

Gemeten door de naamnorm-lens (2026-09-23, bestond al): een attribuut
`Gehesen` naast een kenmerk `gehesen` op één objecttype is
verifier-schoon en ref speelt het, maar de Drools-beanmodel-generatie
geeft beide dezelfde getter — exec faalt met `ERROR … setter
setGehesen(List)` en de DRL-tak weigert het hele bestand bij het
compileren. Luid, geen stil fout antwoord, maar een pakketbrede of
kale fout in plaats van een benoemde per-vorm-weigering. De
naamnorm-kaart maakte het geval bereikbaar via afwijkende spelling;
de botsing zelf is een naamruimtekwestie van het beanmodel.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer; meet welke declaratieparen botsen (attribuut ×
  kenmerk, en zijn er meer ruimtes — rollen?).
- (b) Benoemde weigering bij het laden (huispatroon: loader-Refusal
  die beide declaraties noemt) óf gescheiden naamruimtes in het
  beanmodel; kies de kleinste en motiveer; rood-eerst beide takken.
- (c) Corpussweep byte-identiek; alle veertien gates groen.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten.

## Aanvulling (hoofdsessie, 2026-09-28): twee typenamen op één Java-klasse

Dezelfde naamruimtekwestie, nu tussen objecttypen. Gemeten op uchar
`ea6695a2` via de distributie (`bin/regelspraak check`), reproductie op
Jutterswet-materiaal; `/` scheidt regels.

    R1  Objecttype het RechtOpPremie (mv: RechtenOpPremie)
            de grondslag Numeriek (geheel getal);
        Objecttype het Recht op premie is een RechtOpPremie (mv: Rechten op premie)
            de premie Numeriek (geheel getal);
        Regel premie / geldig altijd /
        De premie van een Recht op premie moet berekend worden als de grondslag van het Recht op premie plus 1.
        nu: het frontend onderscheidt de twee typen (objecttypen=2, resolved=1); de
        beancompiler schrijft 'Recht op premie' en 'RechtOpPremie' naar één klassenaam
        en stopt met `java.lang.IllegalStateException: gegenereerde fact-beans
        compileren niet: …/facts/RechtOpPremie.java:4: error: cyclic inheritance
        involving dev.uchar.regelspraak.facts.RechtOpPremie`
        (`BeanCompiler.compileer` ← `DroolsBackend.build`); exit 1 met stacktrace,
        geen melding die de twee declaraties noemt.
    R2  R1 zonder `is een` en zonder de regel (twee losse typen).
        nu: exit 3, `contract: RechtOpPremie.grondslag: getter getGrondslag()
        ontbreekt op dev.uchar.regelspraak.facts.RechtOpPremie` — de tweede klasse
        overschrijft de eerste; de melding noemt het gevolg, niet de botsing.

Acceptatie erbij:

- (d) R1 en R2 geven één benoemde weigering die beide objecttypen en de
  gedeelde klassenaam noemt (het huispatroon van (b)), óf botsingsvrije
  klassenamen zodat beide typen spelen. De bouwer kiest de kleinste vorm die
  (b) en (d) samen dekt en motiveert; botsingsvrije namen alleen als elke
  bestaande golden zijn beans byte-gelijk houdt. Meet welke naamparen botsen:
  objecttype × objecttype (met en zonder `is een`), attribuut × kenmerk (de
  aanleiding), rollen, feittypen.
- (e) Een getuige per gekozen vorm: een negatief- of rood-getuige voor een
  weigering; bij botsingsvrije namen een golden met beide typen, in ref ≡
  clingo ≡ Drools-exec.
- (f) Alle zestien gates groen (de scripts met bash waar de shebang dat
  zegt).

## Result (2026-09-28, bouwer)

### Wat er landde

- `DroolsBackend.build` weigert vóór de beangeneratie, in beide takken
  en met één zin die beide declaraties en de gedeelde Java-naam noemt
  (sleutelwoord `beannaambotsing`):
  - een objecttype waarvan de feitklasse al bij een ander objecttype
    hoort (met en zonder `is een`), bij een hulpklasse die het
    beanmodel zelf genereert (`Segment`, `Inconsistentie`) of bij een
    publiek `java.lang`-type;
  - twee rollen op één referentieveld van hetzelfde type;
  - een attribuut of kenmerk zonder eigen beanveld, maar alleen waar
    een vorm van de regellaag (de vormen van `ProducentConsument`) of
    een startpuntbepaling het leest of schrijft.
- `CaseRunner` weigert met dezelfde zin een case die zo'n lid zet
  (`attributes`, `characteristics`, `characteristicTimelines`) of leest
  (`attributeValue`/`attributeEmpty`/`attributeTimeline`,
  `characteristicPresent`/`characteristicAbsent`).
- `FactModelGenerator`: de ledenafleiding draagt de herkomst van elk
  lid (`Herkomst`, intern `kandidaten()`); nieuw zijn `verdringingen()`,
  `verdringingReden()`, `verdrongenLid()`, `rolbotsing()` en
  `klassebotsing()`. `leden()` trekt nu de leden van elke voorouder af,
  niet alleen die van de directe ouder.
- Getuigen: `BeannaamBotsingWitnessTest` (16 rijen), de goldens
  `jutterswet-premiespelling` en `jutterswet-halfstokspelling`, en een
  voorstel voor gate 12 (`scripts/check-drools.sh`).

### (a) Gemeten: welke paren op één Java-naam vallen

`Namen.ident` laat spaties en koppeltekens weg en zet de woorden in
camel case; WF-001 en WF-002 vergelijken hoofdletterongevoelig en WF-002
laat een attribuut en een kenmerk dezelfde naam dragen. Elke probe
hieronder is eigen Jutterswet-tekst via de frontend, verifier-schoon en
door ref en clingo gespeeld (`oracle: PASS`, `clingo: PASS`); "vóór" is
de jar van de claim (`982735b5`), in beide takken gelijk tenzij anders
vermeld.

| paar | probe | Drools vóór | Drools na |
|---|---|---|---|
| objecttype × objecttype, met `is een` (R1 van de hoofdsessie) | `RechtOpPremie`; `Recht op premie is een RechtOpPremie` | `ERROR IllegalStateException: gegenereerde fact-beans compileren niet: …/RechtOpPremie.java:4: error: cyclic inheritance involving dev.uchar.regelspraak.facts.RechtOpPremie` | geweigerd (klasse) |
| idem, zonder `is een` (R2) | twee losse typen | `EXCLUDE contract: RechtOpPremie.grondslag: getter getGrondslag() ontbreekt op dev.uchar.regelspraak.facts.RechtOpPremie` | geweigerd (klasse) |
| idem, gelijke leden | beide `grondslag`, regel op `Recht op premie` | `FAIL …: verwacht leeg, werkelijk 3` (de regel raakte de instantie van het andere type) | geweigerd (klasse) |
| objecttype × hulpklasse | objecttype `Segment` | `EXCLUDE contract: Segment.lengte: getter getLengte() ontbreekt op dev.uchar.regelspraak.facts.Segment` | geweigerd (hulpklasse) |
| objecttype × `java.lang` | objecttype `String` | `ERROR IllegalStateException: … Inconsistentie.java:10: error: toString() in dev.uchar.regelspraak.facts.Inconsistentie cannot override toString() in java.lang.Object` | geweigerd (`java.lang`) |
| attribuut × kenmerk, kenmerk door een regel geschreven | `top-stand` Numeriek, kenmerk `top stand` | exec `ERROR ConsequenceException: … java.lang.ClassCastException: Cannot cast java.lang.Boolean to java.math.BigDecimal`; DRL `EXCLUDE drl compile: … Unable to Analyse Expression …` | geweigerd (regel) |
| attribuut (Boolean) × kenmerk | `half-stok`, kenmerk `half stok` (golden halfstokspelling) | `FAIL …: verwacht leeg, werkelijk 1 \| …: kenmerk half stok: verwacht afwezig, werkelijk aanwezig` | geweigerd (regel) |
| attribuut × kenmerk, kenmerk door niemand gebruikt | bron3 van de hoofdsessie; de vorm van de TOKA-casus | bouwt (distributie `check`: exit 0) | bouwt; premierecht met zo'n kenmerk speelt beide cases (JUnit-rij 16), de TOKA-cases spelen (gate 11) |
| attribuut × attribuut | `vondst-gewicht`, `vondst gewicht` | `FAIL …: attribuut waarde: verwacht 5, werkelijk 10` | geweigerd (de case zet het) |
| kenmerk × kenmerk | `top-stand`, `top stand` | `FAIL …: verwacht leeg, werkelijk 1` | geweigerd (regel) |
| eigen lid × geërfd lid | `grondslag-waarde` op Strandrecht, `grondslag waarde` op Wrakrecht | `FAIL …: attribuut premie: verwacht 4, werkelijk 11; attribuut grondslag waarde: verwacht 3, werkelijk 10`; met de invoer in omgekeerde volgorde `PLAYED 1` | geweigerd (regel) |
| eigen lid van een kleinkind × lid van de grootouder, ander type | `vondst-gewicht` Numeriek op Vondst, `vondst gewicht` Tekst op Diepe wrakvondst | `ERROR IllegalStateException: … getVondstGewicht() in dev.uchar.regelspraak.facts.DiepeWrakvondst cannot override getVondstGewicht(…` | bouwt (`PLAYED` op een lege case; het lid wordt niet gebruikt, `leden()` over de hele keten) |
| rol × rol, zelfde veldtype | `vaste-ligger` (pontonligging) en `vaste ligger` (reserveligging), beide naar Ponton | `FAIL …: verwacht leeg, werkelijk 9`; met beide rollen op één schuit `ERROR IllegalArgumentException: to-one rol 'vaste ligger' krijgt een tweede, ander doel — ref weigert die case bij het laden (WF-013)`, terwijl ref speelt | geweigerd (rol) |
| rol × rol, ander veldtype | `rood/ambiguous-case-role-key` (`ligger` naar Ponton en naar Vlot) | per gebruik benoemd geweigerd (bestaande poort op naam en type) | ongewijzigd |
| rol × lid | — | per gebruik benoemd geweigerd door de bestaande poort ("name-sharing bean limit"); niet opnieuw gemeten | ongewijzigd |
| feittypen | — | geen Java-naam: op de claim gaat in `backends/drools/src/main` geen feittypenaam door `ident`, `pascal`, `getter` of `setter` (`git grep`, 0 treffers), alleen rolnamen | — |
| parameter × parameter (buiten het beanmodel) | `grens-waarde`, `grens waarde` | `FAIL …: attribuut premie: verwacht 5, werkelijk 10` | ongewijzigd, open punt 1 |
| Java-sleutelwoord als eigenschap (geen paar) | attribuut `default` | `ERROR IllegalStateException: … error: <identifier> expected private java.math.BigDecimal default;` | ongewijzigd, open punt 3 |

Voorkomen in het corpus, gemeten over alle 845 `ir.json` van
`corpus/golden`, `corpus/rood`, `corpus/negatief` en de TOKA-zone met
de afleiding van de generator: nul objecttypen op één klasse, nul
hulpklasse- of `java.lang`-namen, nul parameters of feittypen op één
naam. Leden: `anchor/jutterswet-toelagegronden` (attribuut en kenmerk
`toelage` op Jutter; een defaultsgroep schrijft het kenmerk), de
TOKA-casus en alle 29 TOKA-cases (een attribuut en een kenmerk met één
naam op één objecttype; alleen het attribuut wordt gelezen en gezet),
`rood/ambiguous-case-role-key` (rol × rol, ander type) en
`negatief/wf105-hershadowd-attribuut` (verifier weigert). Drie
afleidingen van één rol via een subtype van haar vuller
(`jutterswet-strandmelding`, `negatief/wf050-*`, `negatief/wf109-*`)
zijn één declaratie en geen botsing.

### Gekozen vorm en waarom

Een benoemde weigering, geen botsingsvrije namen:

1. De kaart staat botsingsvrije namen alleen toe als elke golden zijn
   beans byte-gelijk houdt. `anchor/jutterswet-toelagegronden` heeft een
   botsing; een eigen veld voor zijn kenmerk verandert zijn bean. Ook de
   TOKA-casus heeft er een.
2. Een naam per botsing moet door elke plek die een naam tot Java-naam
   maakt: op de claim roepen 124 regels in 8 bestanden van
   `backends/drools/src/main` `ident`, `Namen.getter`, `Namen.setter` of
   `RegelSpraakRuntime.pascal` aan, in beide transpilers, de caselezer,
   het contract en de runtime, die leden bij het vuren op naam oplost
   zonder model. De weigering is één controle vóór de beangeneratie.

Waar en hoe precies:

3. In `DroolsBackend.build` als `CaseRunner.Excluded`, niet als
   loader-`Refusal` (afwijking 1). De regelgraaf van gate 15 leest via
   `IrLoader.loadAll` en houdt de twee namen uit elkaar (volgens de
   README van de backend is het ook de lezing die de scopeanalyse van de
   distributie gebruikt); een loader-weigering had die documenten voor
   de graaf onleesbaar gemaakt. Gemeten: gate 15 vergelijkt
   nu 240 goldens (was 238), de twee nieuwe inbegrepen. Gelezen, niet
   gemeten: `GraafBronrouteTest` roept `IrLoader.loadAll` zonder vangnet
   aan op elke golden met een `bron.rgs`, dus een loader-weigering had de
   routeleg van gate 15 laten falen. De nabije precedent is de
   naamdelingsgrens van de wederkerige zelfrol, ook een `Excluded` in
   `DroolsBackend.build`.
4. Objecttypen en rollen (zelfde veldtype) weigeren altijd: elke klasse
   wordt gecompileerd, en rolgebruik zit verspreid over navigatie,
   vouwbereik, creatie, verdeling en case-invoer. Rollen met een ander
   veldtype laat de controle aan de bestaande poort per gebruik
   (`heeftReferentieVeld`, naam én type), zodat
   `rood/ambiguous-case-role-key` ongewijzigd blijft.
5. Leden weigeren alleen bij gebruik. Een weigering op declaratie had de
   29 TOKA-cases geweigerd die gate 11 als `PLAYED` vastpint: elk heeft
   een attribuut en een kenmerk met één naam, en het kenmerk gebruikt
   niemand. Wie een lid zonder eigen veld gebruikt, bereikt het veld van
   de andere declaratie: een crash bij het vuren als de typen verschillen,
   een fout antwoord als ze samenvallen (tabel hierboven). Een ongebruikt
   lid zonder veld raakt niemand. Welk lid het veld houdt, volgt de
   generator: een voorouder vóór een afstammeling, binnen een type
   attributen, kenmerken, referentievelden, elk in declaratievolgorde.
6. `leden()` over de hele keten (afwijking 4): de aftrek van alleen de
   directe ouder liet een kleinkind een eigenschap van de grootouder
   opnieuw declareren; met een ander type crashte de beancompilatie
   (tabel). Zonder deze aftrek hoort in zo'n keten niet één declaratie
   bij één eigenschap, en klopt "wie houdt het veld" niet.

### Weigerzinnen, letterlijk uit echte uitvoer

Uit de jar van deze branch, met de code van `2703e598` (de statusregel
van `DroolsBackendMain`, exec en `--drl` gelijk):

    EXCLUDE beannaambotsing: de objecttypen 'RechtOpPremie' en 'Recht op premie' vallen op één feitklasse, dev.uchar.regelspraak.facts.RechtOpPremie, en het beanmodel maakt één klasse per naam
    EXCLUDE beannaambotsing: het objecttype 'Segment' valt op de feitklasse dev.uchar.regelspraak.facts.Segment, de hulpklasse die het beanmodel zelf genereert voor de segmenten van een tijdlijn
    EXCLUDE beannaambotsing: het objecttype 'String' valt op de feitklasse dev.uchar.regelspraak.facts.String, en die verbergt java.lang.String voor de gegenereerde beans en de DRL-tekst
    EXCLUDE beannaambotsing: het kenmerk 'half stok' van Sein valt op de beaneigenschap Sein.halfStok van het attribuut 'half-stok' van Sein en heeft geen eigen veld; regel 'halfstok van een sein' leest of schrijft het
    EXCLUDE beannaambotsing: het kenmerk 'toelage' van Jutter valt op de beaneigenschap Jutter.toelage van het attribuut 'toelage' van Jutter en heeft geen eigen veld; defaultsgroep 'toelage-kenmerk voor vissers' leest of schrijft het
    EXCLUDE beannaambotsing: het attribuut 'grondslag waarde' van Wrakrecht valt op de beaneigenschap Wrakrecht.grondslagWaarde van het attribuut 'grondslag-waarde' van Strandrecht en heeft geen eigen veld; regel 'premie van een wrakrecht' leest of schrijft het
    EXCLUDE beannaambotsing: het attribuut 'vondst gewicht' van Vondst valt op de beaneigenschap Vondst.vondstGewicht van het attribuut 'vondst-gewicht' van Vondst en heeft geen eigen veld; de case zet het
    EXCLUDE beannaambotsing: het kenmerk 'vondstgewicht' van Recht op premie valt op de beaneigenschap RechtOpPremie.vondstgewicht van het attribuut 'vondstgewicht' van Recht op premie en heeft geen eigen veld; de case leest het
    EXCLUDE beannaambotsing: de rol 'vaste ligger' (feittype 'reserveligging') valt op het referentieveld Schuit.vasteLigger van de rol 'vaste-ligger' (feittype 'pontonligging'), een veld van hetzelfde type, en de bean draagt per veld één relatie

De laatste-maar-één is premierecht met een ongebruikt kenmerk
`vondstgewicht` en een case die `characteristicAbsent` op dat kenmerk
verwacht: vóór `PLAYED 1` (de verwachting las het getal 5 van het
attribuut als "niet waar", juist bij toeval), nu geweigerd.

### (e) Getuigen

- JUnit, `backends/drools/src/test/java/dev/uchar/regelspraak/cases/BeannaamBotsingWitnessTest.java`,
  16 rijen op varianten in het geheugen van `jutterswet-premierecht`,
  `jutterswet-rekenjaar` en `anchor/jutterswet-toelagegronden`; elke
  weigerrij eist in beide takken een `Excluded` met alle fragmenten.
  Rood op `78100938` (de code van de claim): `Tests run: 16, Failures:
  10, Errors: 5`, onder meer `IllegalStateException … cyclic
  inheritance`, `kon setter setVondstgewicht(List) niet aanroepen op
  RechtOpPremie`, `kon setter setToegekend(boolean) niet aanroepen`,
  `refuses nothing and answers case: attribuut aantal-toegekende
  rechten: verwacht 9, werkelijk 0`, `… kenmerk laat-gemeld: verwacht
  afwezig, werkelijk aanwezig`, `… kenmerk toegekend: verwacht afwezig,
  werkelijk aanwezig; verwacht leeg, werkelijk 20`, `… attribuut
  aantal-toegekende rechten: verwacht 7, werkelijk 0`, en voor het
  kleinkind `[Lid[eigenschap=rechthebbende, javaType=Jutter,
  isVorm=false]] ==> expected: <false> but was: <true>`. De zestiende
  rij (een ongebruikt lid zonder veld laat premierecht spelen) is groen
  vóór en na. Groen na `2703e598`: `Tests run: 16, Failures: 0,
  Errors: 0, Skipped: 0`.
- Corpus: `corpus/golden/jutterswet-premiespelling` (R1-vorm) en
  `corpus/golden/jutterswet-halfstokspelling` (attribuut × kenmerk, de
  vorm met het fout antwoord), elk `bron.rgs` + `scenario.rgs` + twee
  cases + `NOTE.md` met de handberekening; verifier PASS, ref en clingo
  PASS op alle vier de cases, no-op weerlegd, `--ir` en `--cases` geven
  dezelfde `ir.json`. Drools weigert beide in beide takken; gate 12
  pint ze in `EXPECTED_CHANNEL` en `DRL_CHANNEL` (voorstelcommit
  `927b93dc`). Een negatief- of roodgetuige kan niet (afwijking 2).

### (c) Sweep

Jar van de claim tegen de jar van `2703e598`, over het corpus zoals het
vóór de twee nieuwe goldens was:

- gegenereerde beans (SHA-256 per document over alle klassen) van alle
  845 documenten (601 genereren beans): byte-gelijk;
- statusregels van 275 goldens in beide takken, 40 roodcases
  (`DroolsRoodMain`) en 29 TOKA-cases in beide takken: byte-gelijk op
  één na, `anchor/jutterswet-toelagegronden`. Vóór: exec `EXCLUDE
  transpiler SKIP: defaults: defaults K:Jutter.toelage — defaults
  (kenmerktarget Jutter.toelage deelt zijn naam met een attribuut — …)`,
  DRL `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has
  no defaults lowering`; na: beide de toelagezin hierboven. Kanaal
  blijft EXCLUDE; played 219 en DRL 188 ongewijzigd.

### (f) Gates

Alle zestien op `927b93dc`, in de volgorde van `gates.yml`, de bash-
scripts met bash, na `eval $(opam env)`, alle met exit 0:

    check-self-contained: PASS
    check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 316 file(s) validate and round-trip, floor 192; 41430 object(s) in 817 document(s) in the schema's key order, 1 in no subschema)
    check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 785 case file(s) validate)
    check-verifier: PASS (316 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
    check-oracle: PASS (ref suite 1152 worked examples, floor 489; 745 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
    check-clingo: PASS (backend suite 1988 checks, floor 83; 745 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop: PASS (701 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
    check-rood: Drools leg PASS (12 fault-pinned, 28 not-carried with reason)
    check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
    check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    check-regelspraak: PASS — JUnit suite green (969 tests, floor 173); 104 rgs golden(s) re-emit byte-identically (floor 80)
    check-toka-parity: PASS
        Drools : 29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR
    check-drools: PASS
      played 219  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
      channel floor 58 non-PLAYED goldens pinned by name; played matches the pinned 219
      DRL-text branch played 188  (its OWN count — the narrower branch, not the square; pin 188)
      DRL-text floor 89 non-PLAYED goldens pinned by name; sweep cost 134s wall clock
      JUnit 862 tests, floor 239  (the backend's own suite, green with a non-zero count)
    check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    check-graaf: spec leg — 240 of 277 goldens compared, 131 dependencies equal to the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)
    check-graaf: route leg — the graph of the wire equals the graph of the source on all 104 goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)
    check-graaf: PASS
    check-scenario: PASS — 11 scenario golden(s) (floor 7) regenerate 44 case file(s) (floor 31) and their ir.json byte-identically

Basisrun op de claim, vóór elke wijziging: ook alle zestien groen
(played 219, DRL 188, JUnit 846, graaf 238 van 275).

### Afwijkingen van de kaart

1. Geen loader-`Refusal` maar een `Excluded` in `DroolsBackend.build`;
   reden onder punt 3 van de keuze.
2. Geen negatief- of roodgetuige: de documenten zijn verifier-schoon
   (negatief eist een WF-weigering) en ref en clingo spelen ze (rood
   eist een fout in beide). De corpusgetuige is daarom een golden met
   een ref = clingo-paar en een benoemde Drools-weigering, vastgepind in
   gate 12. Dat vraagt een wijziging aan `scripts/check-drools.sh`, een
   beschermd pad: eigen commit met "voorstel" in de kop (`927b93dc`).
   Daarin ook de redentekst van toelagegronden en de verhuizing van zijn
   DRL-regel van klasse (A) naar een nieuwe klasse (F), met de
   koptellingen (A 34, F 3, samen 89).
3. Breder dan (b) en (d): ook attribuut × attribuut, kenmerk × kenmerk,
   eigen × geërfd, rol × rol (zelfde type), hulpklasse en `java.lang`.
   Zelfde mechanisme, zelfde controle, gemeten fout antwoord of kale
   crash (tabel onder (a)).
4. `leden()` over de hele keten, nodig voor punt 6 van de keuze; nul
   gewijzigde beans in het corpus.
5. (c) zegt veertien gates, (f) zestien; alle zestien gedraaid.

### Open punten

1. Parameters: twee parameters waarvan de namen alleen in spaties of
   koppeltekens verschillen, delen één global (`global` in de DRL-tekst,
   de sessieglobal in de exectak). Verifier, ref en clingo PASS, beide
   takken `FAIL …: attribuut premie: verwacht 5, werkelijk 10`, ook na
   deze kaart. Buiten het beanmodel en daarom niet gebouwd; dezelfde
   controle op parameterglobals zou het vangen.
2. Dagsoorten: de DRL-tekst noemt een dagsoortglobal
   `rsDagsoort_` + `Namen.ident(naam, true)`. Niet gemeten: de frontend
   leest een dagsoort als eenheid alleen als één woord, dus een botsing
   vraagt een handgeschreven IR.
3. Een Java-sleutelwoord als eigenschap (attribuut `default`) laat de
   beancompilatie kaal crashen (tabel). Geen naampaar; de javadoc van
   `Namen.ident` noemt het al.
4. De ledenweigering bij gebruik weigert ook het geval waarin alleen het
   lid zonder veld gebruikt wordt en het veld van de ander toevallig
   past (bijvoorbeeld een ongebruikt Booleaans attribuut dat de
   eigenschap houdt en een gebruikt kenmerk); dat speelde vroeger bij
   toeval. Zo weigert de bestaande poort ook een rol die door een lid
   verdrongen is. Geen voorkomen in corpus of TOKA-zone.
5. De distributie zet feiten uit een verzoek zelf, via `Namen.setter`
   in `VerzoekMapper` (gelezen, niet gemeten), niet via `CaseRunner`. Een
   verzoek dat een lid zonder eigen veld zet dat de regels niet
   gebruiken, wordt daar niet geweigerd; de bouwweigering geldt wel voor
   elke gebruiker van `DroolsBackend.build`. `verdringingen()` en
   `verdrongenLid()` zijn publiek, zodat de distributie dezelfde vraag
   kan stellen.
6. Nevenvondst, bestond al, ongewijzigd: een subtype dat via case-invoer
   een rol vult waarvan het supertype de vuller is (premierecht met
   `Strandjutter is een Jutter`, `rechten op premie` op S1), weigert
   Drools met `role 'rechten op premie' resolves to the to-one role
   'rechthebbende' on Recht op premie, whose field a same-named role
   toward another type already holds — …`, terwijl er geen rol met die
   naam naar een ander type bestaat; ref en clingo PASS. Luid, maar de
   zin noemt de verkeerde oorzaak.
7. `bin/regelspraak check` van de distributie draait op uchar
   `ea6695a2` en toont R1 en R2 nog als crash en contractmelding; de
   benoemde weigering bereikt de distributie pas na een pin-verplaatsing
   (niet gemeten).
8. Geen rij voorgesteld voor `spec/CONSTRUCTS.md` of `spec/PLAN.md`: er
   verandert geen IR-construct en geen spec-tekst, alleen wat de
   Drools-backend draagt.

### Commits (branch `card/drools-beannaamruimte-botsing`)

- `982735b5` kaart drools-beannaamruimte-botsing opgepakt
- `78100938` BeannaamBotsingWitnessTest: twee declaraties op één Java-naam, rood
- `d84004ff` FactModelGenerator.leden: de hele keten eraf, niet alleen de ouder
- `2703e598` Drools-backend weigert twee declaraties op één Java-naam, benoemd
- `312435c3` BeannaamBotsingWitnessTest: commentaar zonder herkomst
- `68fe0e31` goldens premiespelling en halfstokspelling: twee namen, één Java-naam
- `927b93dc` voorstel gate 12: de beannaambotsing in beide weigerlijsten
- en de commit met dit Result.
