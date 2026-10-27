# subtype-characteristic-in-fold-filter: een kenmerk van een subtype in het filter van een vouw over een rol van het supertype

*Vastgelegd door de werkbank (2026-10-27), uit de bouw van de regelset-opdracht `wekeneis-nieuw-recht` (rules, tak
`wekeneis-nieuw-recht`, 9590119). Gemeten op toolchain b4f3ab94 (uchar c6a45d89). Niet gecommit tot de bouw.*

## Aanleiding

In v2/herleving-ww vouwt de uitzondering van art. 21 lid 2 onder a over "alle rechten van de samenloop" (rol `rechten`,
gevuld met het supertype `RechtOpUitkering`). Sinds een besluit van de eigenaar (2026-10-27) telt een recht daar alleen
mee als het de wekeneis heeft. Het kenmerk `wekeneis` staat op het subtype `RechtOpUitkeringWwBijOntslag`, waar het CGM
het legt.

In het filter van die vouw bindt `.. hij heeft de wekeneis` niet. `check` geeft, exit 0:

    WARNING (rules/hww-06-samenloop.rgs:42): onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het
    onderwerp (§8.1.7/§8.1.8), dus er is geen lidmaatschap om te toetsen — 'hij heeft de wekeneis' blijft als marker
    op de wire staan

Hetzelfde gebeurt met "het RechtOpUitkering heeft de wekeneis", plat en in een geneste groep. De werkbank maakt van een
WARNING een rode `make check`.

De regelset omzeilt het nu met brug K14: het filter leest de toets van de wekeneis zelf (de kalenderweken van de
referteperiode tegen de drempel). Daardoor staat die toets op twee plekken. Een eerdere brug op hetzelfde patroon is K9:
"de twee maanden" staan op het supertype, omdat de inkomens aan `Recht op uitkering WW` hangen, dat de grond niet ziet.

## Reproductie op neutraal materiaal

```
Objecttype het Strandrecht (mv: Strandrechten)
    het loon Numeriek (getal met 2 decimalen);
Objecttype het Jutrecht is een Strandrecht (mv: Jutrechten)
    de grens Numeriek (getal met 2 decimalen);
    de blokkade kenmerk (bezittelijk);
Objecttype het Wrakrecht is een Jutrecht (mv: Wrakrechten)
Extensie van objecttype het Wrakrecht
    de vaartijd kenmerk (bezittelijk);
Objecttype de Ploeg (mv: Ploegen)
Objecttype het Logboek (mv: Logboeken)
    de zeeweken Numeriek (geheel getal);
Objecttype de Melding (mv: Meldingen)
    de soortcode Numeriek (geheel getal);
Feittype PloegBetreftStrandrecht
    de ploeg Ploeg
    het recht (mv: rechten) Strandrecht
Eén ploeg betreft meerdere rechten
Feittype StrandrechtHeeftLogboek
    het recht Strandrecht
    het logboek Logboek
Eén recht heeft één logboek
Feittype MeldingLeidtTotStrandrecht
    de melding Melding
    het recht Strandrecht
Eén melding leidt tot één recht
Parameter de vaartijddrempel : Numeriek (geheel getal)

Regel vaartijd
    geldig altijd
        Een Wrakrecht heeft de vaartijd
        indien de zeeweken van het logboek van het Wrakrecht groter of gelijk is aan de vaartijddrempel.

Regel blokkade
    geldig altijd
        Een Jutrecht heeft de blokkade
        indien hij aan alle volgende voorwaarden voldoet:
        - zijn grens is gevuld
        - (de maximale waarde van het loon van alle rechten van de ploeg van het Jutrecht die aan alle volgende voorwaarden voldoen:
        .. hij is niet het Jutrecht zelf
        .. de soortcode van de melding van het Strandrecht is gelijk aan 1
        .. hij heeft de vaartijd) is groter dan zijn grens.
```

- **Zo geeft het de WARNING.**
- **Wat wel zonder melding speelt** (gemeten door de bouwer, met vier scenario's: boven, onder, zonder logboek, zonder
  aantal):
  - de toets zelf in het filter ("… de zeeweken van het logboek van het Strandrecht is leeg" of "… groter of gelijk aan
    de vaartijddrempel");
  - het kenmerk op het supertype gedeclareerd, met de regel op het subtype.

## Doel

`hij heeft <kenmerk>` (en `het <Supertype> heeft <kenmerk>`) bindt in het filter van een vouw over een rol die een
supertype vult, ook als alleen een subtype het kenmerk declareert.

Wat zo'n kenmerk betekent voor een lid dat niet van dat subtype is, is een ontwerpvraag. Het standaardantwoord van de
werkbank: het kenmerk geldt voor een lid van het subtype zoals daar afgeleid, en geldt niet voor een lid van een ander
type. Dat is gelijk aan "hij is een <Subtype> en hij heeft <kenmerk>".

Twee dingen beslist de bouwer, met één Fable-advies als het een echte ontwerpvraag is:
- geldt hetzelfde voor een attribuut van een subtype in zo'n filter;
- wat zegt de verifier tegen een naam die alleen een subtype van een ander type declareert.

De keuze komt in de spec (RUNTIME-SEMANTICS en de paragraaf van de binding) en in het Result.

## Acceptatie

- (a) Rood eerst: een golden in de vorm van de reproductie, met een lid van het subtype dat het kenmerk heeft, een dat
  het niet heeft, en een lid van een ander type in de vouw. Ref, clingo en beide Drools-takken spelen hem met dezelfde
  uitkomst.
- (b) De uitleg van de toolchain noemt het kenmerk in het filter (`NAME` met `declaration: KENMERK`).
- (c) Goldens zonder deze vorm blijven bytegelijk; `make gates` groen.
- (d) v2 bouwt nog. In het Result staat hww-06 met "... hij heeft de wekeneis" in plaats van brug K14, gemeten met
  `check` en `scenario-test` van v2/herleving-ww: geen WARNING, en dezelfde uitkomsten.

## Niet

De regelset zelf: het weghalen van brug K14 doet de werkbank na de pin-bump.

## Voorlopige besluiten (Fable-advies, 2026-10-27)

*Uit `research/subtype-characteristic-in-fold-filter-2026-10-27/fable-advies.md` (gelezen op `088659f0`), door de
hoofdsessie als voorlopig besluit overgenomen. Het advies noemt per uitvoerder de plaatsen in de code, de golden
met haar waarden en de bouwvolgorde. Acceptatie (b) (de uitleg van de toolchain) en (d) (hww-06 met `check` en
`scenario-test`) doet de hoofdsessie bij de pin-bump van de toolchain.*

- **D1 Weergave.** `CharacteristicCheck {characteristic, host: S}` met S een strikt subtype van het type van het
  ene gebonden object van de positie: de versmallende host. Geen nieuw knooppunt, geen nieuw veld, geen
  schemawijziging. De frontend emitteert hem alleen waar de keten van het onderwerp K niet draagt.
- **D2 Lezing** (RUNTIME-SEMANTICS ch. 5.8b regel 4). Waar iff het object een instantie van S is (extensioneel,
  ch. 2.2) en K heeft zoals S het declareert; `negated` is het complement. Declaratiesleutel en graafknoop: S.
- **D3 Ontkenning.** Een lid buiten S: positief onwaar, ontkend waar — het gewone complement (ch. 5.8a regel 4,
  WF-122 (c)); niet de hop-lezing van ch. 5.8 regel 3. Gepind door `hoogste rustloon` (40, niet 30).
- **D4 WF-109.** De flank "nul voldoend, één overlappend" is op een positie met één gebonden object een lezing;
  op de feitcreatie-guard blijft hij `only-some-objects`. Getuige herschreven naar de guard.
- **D5 Positie.** Elke positie met één gebonden object (regel, consistentie, verdeling, creatie, default,
  vouwfilter, kwantorfilter en -test, unieselectie). Niet: de genavigeerde check, de verankerde check, de guard.
- **D6 Siblings.** Twee onvergelijkbare declaranten: frontend weigert met een benoemde diagnose die ze noemt;
  geen vereniging geëmitteerd (de wire kan haar wel dragen; verwijding later). Eén declarant op welke diepte ook
  onder het onderwerp: die host.
- **D7 Attribuut.** Niet in deze kaart. Richting voor de volgende: versmallende eigen lezing (`subjectType` S ⊏
  onderwerp) leest leeg buiten S, ch. 1.2-discipline, geen typetoets als conjunct.
- **D8 Verifier.** WF-100 en WF-034 ongewijzigd: de host moet K declareren; zonder host moet het onderwerp K
  declareren (nieuwe negatieve getuige).
- **D9 Frontend-diagnose.** Geen declarant onder het onderwerp: RGS-092 `no-characteristic-no-role` als nu.
- **D10 ref.** `owner_hosts` kent drie uitkomsten; het statische type is `env.subj`; de foutzin voor een vreemde
  host blijft (suite r. 6126).
- **D11 clingo.** `isa(X,S), has(X,K)`; ontkend via helper onder NAF.
- **D12 Drools.** Klassetoets + lezing op beide takken; DROOLS-240 alleen nog voor een onvergelijkbare host; een
  ontbrekende stand leest als "buiten S", nooit als een patroon dat het lid uit de regel laat vallen.
- **D13 Lezing ch. 5.12.** Knoop `CharacteristicCheck <oordeel>`, geen `on`-deel; geen nieuwe regelvorm.
- **D14 Uitleg.** Niets extra op de wire; de distributie leest `host` als holder (`Verbaliser` r. 1815), eigen
  kaart bij de pin-bump.
- **D15 Getuigen.** Golden `jutterswet-ploegblokkade` (bron + scenario, 3 cases, 6 regels); negatief herschreven +
  nieuw; werkvoorbeeld C in ref- en clingo-suite; frontend-JUnit; `EXPECTED_PLAYED` 281.

## Result (2026-10-27)

Tak `card/subtype-characteristic-in-fold-filter`, op `af5fd045`. Geraakt: de frontend (`ExpressionBuilder`,
`RuleChecker`, beide catalogi, `NarrowingHostTest`, `grammar/DIVERGENCES.md`), ref (`eval.ml`, `test_ref.ml`), clingo
(`translate.ml`, `test_clingo.ml`), de Drools-backend (`Host`, `TmsTranspiler`, `DroolsCode`, beide catalogi), de
nieuwe golden `corpus/golden/jutterswet-ploegblokkade`, vier negatieven en `corpus/negative/README.md`, en als voorstel
(beschermde paden; de tak is het voorstel) de verifier (`spec/verifier/lib/checks.ml`), `spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md` en `scripts/check-drools.sh`. Niet geraakt: `CLAUDE.md`, `spec/PLAN.md`,
`spec/CONSTRUCTS.md`, het schema (D1: geen nieuw veld), de toka-zone, de productieruntime.

- `f437ce06` frontend: de versmallende host, de sibling-reden, `NarrowingHostTest`, DIVERGENCES; de golden.
- `5762fb9f` verifier (voorstel): de arm van WF-109; de getuige van `only-some-objects` herschreven naar de guard;
  drie nieuwe negatieven; README.
- `22cc9139` ref en clingo: `owner_hosts` met drie uitkomsten, de lezing, werkvoorbeeld C in beide suites.
- `7201d7ca` Drools: de onwaar-stand buiten de host, DROOLS-295; gate 12 `EXPECTED_PLAYED` 280 → 281 (voorstel).
- `117abe69` spec (voorstel): IR-SPEC ch. 2.2, 4.5, 4.6 (q2), WF-index (WF-109, WF-034); RUNTIME-SEMANTICS ch. 5.8b
  regel 4 met werkvoorbeeld C, ch. 5.12 één zin.
- `190f0d83` DIVERGENCES en één OCaml-commentaar in de huidige stand, zonder verhaal over vroeger.
- dit Result.

### Wat er veranderde, per uitvoerder

**Frontend.** Waar een naam geen kenmerk van de keten van het onderwerp bindt en geen rol die het onderwerp vult, zoekt
`ExpressionBuilder.narrowingHost` onder het onderwerp: elk type strikt eronder bindt de naam zoals het onderwerp hem zou
binden (de hele naam eerst, dan `niet`/`geen`, het lidwoord eraf, de verbuiging; na `een` de soort die het werkwoord
kiest). Precies één declarant: de check met die declarant als `owner`, op de wire `host` (D1). Meer dan één: een marker
met de reden `RGS-092.characteristic-on-sibling-subtypes` (`name`, `subject`, `declarants`; D6). Geen: de marker en
reden van voorheen, `RGS-092.no-characteristic-no-role` (D9). De zoektocht komt na keten en rol, dus wat vóór de kaart
bond, bindt hetzelfde. Waar: `characteristicCheckOn` (regelvoorwaarde, consistentie, verdeling, creatie, default, het
samengestelde filter "die aan … voldoen", beide werkwoorden en de EEN-vormen), `elementCharacteristicCheck` (de toets
van een kwantor) en `elementCharacteristicCheckFilter` ("die zeevast zijn"). Niet: de genavigeerde check (een eigen
pad), de verankerde check (`!anchored`), en de hele voorwaarde van een feitcreatie (`narrowingHosts(false)` rond de
bouw). `collectOwnReads` sleutelt een check met een host onder het onderwerp op die host (de kringcontrole ziet de
lezing). De Tabel 16-controle van een ontkenning leest de soort op de host.

**Verifier.** `wf109_host_arm` kreeg de arm `[ _ ], [], [ _ ] when narrowing -> None`; de aanroeper zet `narrowing`
op een check zonder navigatie en zonder kwalificatie over de gehele periode. Op de guard (twee gebonden objecten), de
genavigeerde en de gekwalificeerde check blijft het `only-some-objects`. WF-100 en WF-034 zijn ongewijzigd (D8): de host
moet het kenmerk declareren; zonder host moet het onderwerp het declareren.

**ref.** `owner_hosts` geeft `Hosted` of `Narrowing e` of faalt in de zin van voorheen (D10). `Narrowing e` geldt waar
de host strikt onder `env.subj` ligt, het statische type van de positie. De scalaire check leest dan
`isa self.otype e && has_char self k`, ontkend het complement (D2, D3); de tijdsafhankelijkheid wordt op de host gelezen
(regel 1). De genavigeerde en de gekwalificeerde check met een versmallende host falen (`narrowing_refused`).
`guard_on_target` faalt nu zelf, in de zin van de verifier, waar de host van een guard strikt onder één van de twee
objecten ligt (anders las de anker hem als versmallende host).

**clingo.** Dezelfde `owner_hosts` op het statische `subject_ot`. Positief `isa(X,"S"), has(X,"K")`; ontkend
`not nhN(X)` met `nhN(X) :- isa(X,"T"), isa(X,"S"), has(X,"K").` (D11). De genavigeerde en de gekwalificeerde vorm
falen bij de vertaling, in ref's zin.

**Drools, beide takken.** `Host.narrows`: een host strikt onder het onderwerp van de regel. Gemeten vóór de bouw (het
risico van het advies): met de check gelezen als een gewone stand (`reads.own(member(host, …))`) speelden beide takken
de golden met hoogste rustloon 30, 30 en leeg in case 1 en zonder `landvast` op J1 en S1 — de stand van `vaartijd`
bestaat alleen op Wrakrechten, dus een lid buiten de host viel uit de regel en uit de vouw. Nu leest
`Reads.narrowed` altijd de `CharacteristicState` (ook voor een kenmerk dat alleen invoer is), en zet
`outsideStateRule` per (kenmerk, onderwerptype) een onwaar-stand op elk object van het onderwerptype waarvan de klasse
niet die van de host of een subtype ervan is (`CLASS_NOT_IN`). De stand is zo totaal over de onderwerpen van de regel;
een lid buiten de host leest onwaar (D12). DROOLS-240 blijft voor een host die het onderwerp nooit kan zijn. Nieuw:
DROOLS-295 (`NARROWING_HOST_NOT_CARRIED`, `LIMIT`) voor een versmallende host op een genavigeerde of gekwalificeerde
check, en voor een host onder de declarant (de standen zijn die van de declarant; ref en clingo dragen die vorm, de
frontend schrijft hem niet).

**Lezingen (gate 17, D13).** Geen nieuwe regelvorm: de knoop is `CharacteristicCheck <oordeel>` zonder `on`-deel, ook op
een object buiten de host. Op de golden geven `runner --trace` en `--readings` elk 122 leesregels; de twee takken zijn
gelijk, en ref verschilt van hen alleen in de positie van de bijdragers van de kwantorvouw (`reached`/`unreached` tegen
`unordered`, zes regels), wat gate 17 per bijdrager toestaat.

### Getuigen

- Golden `jutterswet-ploegblokkade`: bron, scenario, NOTE, drie cases met 23, 9 en 14 verwachtingen. Zeven regels: het
  kenmerk in het filter van een vouw (positief, ontkend, met de typenaam, genest in een samengestelde voorwaarde), als
  voorwaarde van een regel op het supertype (positief en ontkend) en in de toets van een kwantorvouw. Ref, clingo en
  beide Drools-takken: PASS, PLAYED 3.
- Negatieven: `wf109-host-partial-population` herschreven naar de guard (host `Seinmast` onder het doel `Sein`, uit
  `jutterswet-seinhijsing`; sleutel `only-some-objects` blijft gedekt); nieuw `wf034-characteristic-of-subtype-without-host`,
  `wf109-narrowing-host-navigated`, `wf109-narrowing-host-period` (uit de golden). README: sectie
  "`subtype-characteristic-in-fold-filter` (2026-10-27)" onder "Vervallen bij een normwijziging" en een verwijzing in de
  L5-rij.
- ref-suite: WV-5.8b-C, 20 controles (de tabel van werkvoorbeeld C op vier objecten in beide polariteiten, de vreemde
  host in de zin van voorheen, de genavigeerde weigering, case 1 van de golden met zijn absolute waarden); 1434 → 1454.
- clingo-suite: de spiegel, 3 controles (ref en clingo op case 1, de genavigeerde weigering); 2235 → 2238.
- Frontend: `NarrowingHostTest`, 9 tests (filter, ontkend en met typenaam, regelvoorwaarde, kwantortoets, die/dat, een
  kenmerk van de keten zonder host, siblings met code en parameters, geen declarant blijft RGS-092, een type buiten de
  keten bindt niet); suite 1302 → 1311.
- Drools: de golden speelt via de corpusgetuigen (`BranchDumpsCorpusWitnessTest`, de readings-getuigen); suite 1713.

### Rood

Op `af5fd045` met de bestanden van de golden (rode worktree `werk/…/red`): de frontend gaf zes `RGS-092`-WARNINGs en
één `RGS-213` (de kwantorvouw) en liet zeven markers staan. Op het IR met hosts gaf de verifier van `af5fd045`
`WF-109.only-some-objects` op elk van de zes checks; ref faalde (`… is hosted on "Wrakrecht" while the subject is
"Strandrecht"`, in case 3 "… "Jutrecht"": het concrete type, dus per case verschillend), clingo bij de vertaling, en
beide Drools-takken `EXCLUDE DROOLS-240`.

### Mutaties (in een kopie, niet gecommit)

| mutatie | waar | uitkomst |
|---|---|---|
| F1 `narrowingHost` geeft altijd null | frontend | 6 van 9 tests in `NarrowingHostTest` rood; de golden re-emitteert anders (gate 10) |
| F2 siblings: de eerste declarant wint | frontend | `two_declarants_…` rood; golden ongewijzigd |
| V1 `narrowing = true` (geen navigatie- en periodegrens) | verifier | `wf109-narrowing-host-navigated` en `-period` schoon (gate 3) |
| V2 de arm `when narrowing` uit | verifier | de golden zes keer `WF-109.only-some-objects` (gate 3) |
| V3 WF-034 zoekt ook onder het onderwerp | verifier | `wf034-characteristic-of-subtype-without-host` schoon (gate 3) |
| R1 buiten de host onwaar in beide polariteiten (de hop-lezing) | ref | 7 controles van WV-5.8b-C rood (rustloon 30, 30, leeg; landvast J1, S1 afwezig); runner op case 1: MISMATCH |
| C1 ontkend `isa, not has` (idem) | clingo | de spiegel rood: hoogste rustloon [3000] waar 4000 hoort |
| D1 `outsideStateRule` niet toegevoegd | Drools | beide takken FAIL op de golden: rustloon 30, 30, leeg; landvast afwezig op J1, S1 |

### DRL-vergelijking

`DrlDump` (de productiebuild, exec-model, per golden) met de jar van `af5fd045` en van `7201d7ca` over het corpus van de
tak: 283 → 284 gebouwd, 48 → 47 geweigerd. Het enige verschil is `jutterswet-ploegblokkade` (geweigerd met DROOLS-240,
nu gebouwd); de DRL-tekst van elke andere golden is bytegelijk, de weigeringen ook.

### v2

- IR van v2 (fundament + herleving-ww, resolve-volgorde) met de frontend van `af5fd045` en van de tak: bytegelijk.
- `V2BuildProbe` op de jar van de tak: beide takken bouwen, 339 regels zonder en 493 met lezingen (gelijk aan voorheen).
- Kopie met `rules/hww-06-samenloop.rgs` r. 46–48 (brug K14) vervangen door `.. hij heeft de wekeneis`: de frontend geeft
  geen WARNING, het IR geen diagnostics; beide takken bouwen (343 / 497 regels: de standen van `wekeneis` op het
  supertype en de onwaar-standregel). Scenario's via `--cases` en `DroolsBackendMain`, beide takken gelijk: de
  ongewijzigde bronnen PLAYED 175 van 175; de kopie 166 van 175. De negen andere cases (toets-kern 14, 16; g12-g14 8,
  10; g15 3; g4-g5 17; g7-g9 8, 9, 15) hebben een nieuw recht zonder aantal kalenderweken in zijn referteperiode: brug
  K14 laat dat recht meetellen (de tak "… is leeg", open punt 25), "heeft de wekeneis" niet (de regel van hww-17 geeft
  zonder aantal geen wekeneis), dus de uitzondering van lid 2 onder a vuurt niet en het recht herleeft. ref geeft
  op toets-kern 14 dezelfde uitkomst als Drools (clingo vertaalt v2 niet: de eenheidsalgebra). Dat is de betekenis van
  de vervanging, geen fout van de uitvoerders.
- Tweede kopie die de leeg-tak houdt en alleen de drempeltoets vervangt (`... de kalenderweken … is leeg` /
  `... hij heeft de wekeneis`, de versmallende host genest in een ten-minste-één-groep): geen WARNING, geen diagnostics,
  beide takken bouwen (343 / 497), 175 van 175 PLAYED op beide takken.
- De hoofdsessie meet het met `check` en `scenario-test` van de toolchain; voor de werkbank: de vervanging van K14 is
  alleen uitkomstgelijk met de leeg-tak erbij.

### Gates en suites

Elk los gedraaid, één tegelijk, op de code van de eindstand (`190f0d83` wijzigt daarna alleen DIVERGENCES-proza en één
OCaml-commentaar). Logs in `werk/subtype-characteristic-in-fold-filter/logs/`.

| gate | uitkomst |
|---|---|
| 2 | PASS: 375 bestanden valideren en round-trippen; 916 documenten in de sleutelvolgorde van het schema |
| 3 | PASS: 375 goldens schoon; 541 negatieven geweigerd op hun regel, 384 op hun arm, 220 armsleutels; 668 pointers |
| 4 | PASS: 961 casebestanden |
| 5 | PASS: ref-suite 1454 werkvoorbeelden; 904 cases exact |
| 6 | PASS: clingo-suite 2238 controles, 0 fouten; 904 cases exact |
| 7 | PASS: 859 cases weerleggen de no-op, 45 vastgepinde overlevers |
| 8 | PASS: 57 rode cases |
| 10 | PASS: JUnit 1311 tests; 142 rgs-goldens bytegelijk; `check-rgs-codes` 355 codes |
| 11 | PASS: 45 toka-fragmenten bytegelijk, TOKA-suite 28 tests; 29 toka-cases in ref, clingo en Drools |
| 12 | PASS: PLAYED 281 op beide takken (gelijke statusregels), JUnit 1713 tests, nul skips |
| 13 | PASS: 44 eenheidsannotaties gedraaid over 25 goldens, 130 herhalingen |
| 15 | PASS: 294 van 331 goldens vergeleken, 243 afhankelijkheden gelijk aan de verifier |
| 16 | PASS: 36 scenario-goldens, 126 casebestanden bytegelijk |
| 17 | PASS: 281 goldens, 774 casebestanden, 6389 lezingen, 0 afwijkingen |

Suites: frontend 1311 tests (gate 10), `ref/test` 1454, clingo 2238 controles, `backends/drools` 1713 tests, nul skips.

### Voorlopige besluiten van de bouwer

- **B1 Volgorde.** Keten, dan rol, dan de zoektocht onder het onderwerp. Een rol van het onderwerp wint dus van een
  gelijknamig kenmerk van een subtype; zo verandert geen binding die vóór de kaart bestond (gemeten: elke golden en de
  toka-casus bytegelijk).
- **B2 De feitcreatie.** De frontend schrijft in de hele voorwaarde van een feitcreatie geen versmallende host, ook
  niet genest, al bindt een geneste positie één object: de herbouw naar het doel (`targetCondition`) begint alleen waar
  de bouw op de anker een marker gaf, en een versmallende host op de anker zou die herbouw wegnemen. De verifier laat de
  geneste vorm toe (één gebonden object), ref en clingo lezen hem; geen producent schrijft hem.
- **B3 Posities in de frontend.** Ook de EEN-vormen ("heeft een <k>", "is een <k>", met de soort van de declarant), de
  kwantortoets en de die/dat-subselectie. De golden kreeg daarom een zevende regel (`ploegvarend`, de kwantortoets;
  afwijking van D15's zes), zodat die positie in alle vier uitvoerders speelt; die/dat staat alleen in de JUnit.
- **B4 Sibling-weigering (afwijking van D6 in de vorm).** Een marker met WARNING en de reden
  `RGS-092.characteristic-on-sibling-subtypes` (parameters `name`, `subject`, `declarants`, in beide catalogi), geen
  ERROR en geen eigen code. Een marker is niet af waar hij ontstaat (de aanroeper leest de zin nog als kwantorvouw); de
  reden reist mee en de sweep meldt hem alleen als hij blijft. De werkbank maakt van de WARNING een rode `check`. Het
  register houdt RGS-092 (`expression`, `reason`); de parameters staan op het deel, dat `check-rgs-codes.sh` in beide
  catalogi houdt.
- **B5 Twee grenzen in de verifier.** De versmallende host alleen op een check zonder navigatie en zonder kwalificatie
  over de gehele periode, met een negatief per grens (`wf109-narrowing-host-navigated`, `-period`; meer dan D15 noemt).
  Zonder die grens liet de verifier een vorm toe die geen uitvoerder leest (mutatie V1).
- **B6 De guard in ref.** `guard_on_target` faalt zelf met de zin van de verifier waar de host van een hele guard strikt
  onder één van de twee objecten ligt; anders las de anker die host als versmallende host.
- **B7 Drools (afwijking van D12 in het mechanisme).** Geen "ontbrekende stand leest als buiten S" in de toets, maar een
  expliciete onwaar-stand op elk object buiten de host (`outsideStateRule`, `CLASS_NOT_IN`): het standpatroon matcht dan
  voor elk lid, op beide takken met hetzelfde plan, en het lid valt niet uit de regel. Een kenmerk dat alleen invoer is
  gaat langs dezelfde stand (geen getuige met alleen invoer: `vaartijd` wordt afgeleid; de invoerregel van de stand is
  dezelfde als bij een afgeleid kenmerk). DROOLS-295 als `LIMIT`, omdat één van zijn armen (host onder de declarant)
  verifier-schoon is.
- **B8 Het kenmerk in de sibling-zin** is de geschreven naam zonder lidwoord (`name`), niet een kenmerk: de twee
  declaranten kunnen de naam via verschillende verbuigingen binden.

### Voor `CLAUDE.md`, gate 12

Als eigen zin na de zin van `test-beyond-thirteen-reads` ("… calls its test function with all of them."):

> Since the card `subtype-characteristic-in-fold-filter` (2026-10-27) a characteristic check whose host lies strictly
> below the subject of its rule (the narrowing host, RUNTIME-SEMANTICS ch. 5.8b rule 4) reads the standing of the
> host's characteristic, and a rule of its own puts an onwaar standing on every object of the subject's type outside the
> host (`TmsTranspiler.outsideStateRule`), so such an object reads onwaar instead of leaving the rule.

Geen vloer en geen lijst van gates 3, 10, 12, 16 of 17 hoeft te veranderen: de vloeren groeien alleen
(`EXPECTED_PLAYED` staat in het script, 281, voorstel in `7201d7ca`), DROOLS-295 is een nieuwe code boven de vloer 289.

### PLAN §9

> **A characteristic of a subtype (2026-10-27, card `subtype-characteristic-in-fold-filter`):** a
> `CharacteristicCheck` may name as its `host` a type strictly below the one object its position binds — the narrowing
> host (IR-SPEC ch. 4.6 (q2), RUNTIME-SEMANTICS ch. 5.8b rule 4): true for an instance of the host that has the
> characteristic, false for every other object, the negation the complement. WF-109 admits it at a one-object position
> on a check without navigation and whole-period qualifier; the guard of a fact creation keeps `only-some-objects`. The
> RegelSpraak frontend writes it where exactly one type below the subject declares the written name (siblings: a marker
> that names them); ref, clingo and both Drools branches read it (Drools through an onwaar standing outside the host).
> New golden `jutterswet-ploegblokkade`; gate 12 `EXPECTED_PLAYED` 281. Open: an attribute of a subtype (D7, leeg
> outside the declaring type, a card of its own); the union over siblings; the navigated check with a narrowing host;
> the explanation of the distribution reads `host` as holder (D14). Provisional decisions D1–D15 and B1–B8 stand in the
> card.

### CONSTRUCTS

Toe te voegen aan de rij `Kenmerk` (ch. 2.2), na "**Bijgesteld 2026-09-20 (WF-109 …)**":

> **Bijgesteld 2026-10-27 (de versmallende host, kaart `subtype-characteristic-in-fold-filter`).** De host mag een type
> strikt onder het ene gebonden object van de positie noemen: de check is dan waar voor een instantie van dat type met
> het kenmerk en onwaar voor elk ander object, de ontkenning het complement (`IR-SPEC.md` ch. 4.6 (q2),
> `RUNTIME-SEMANTICS.md` ch. 5.8b regel 4). Niet op de guard van een feitcreatie, de genavigeerde en de gekwalificeerde
> check. Geen nieuw veld; de frontend schrijft hem voor "hij heeft <kenmerk van een subtype>".

### Voor de toolchain

- D14: `Verbaliser` (r. 1813–1816) neemt `holder = subject` en leest de host niet; met `holder = k.owner() != null ?
  k.owner() : subject` vindt `predicate` de soort en `Naming.characteristicOf` de declaratie `KENMERK` op het subtype
  (acceptatie (b)). Of de uitleg voor een lid buiten de host "is geen <host>" zegt, is een keuze van de distributie.
- Acceptatie (d) en de pin-bump: zie v2 hierboven; "hij heeft de wekeneis" alleen is niet uitkomstgelijk met brug K14,
  de variant met de leeg-tak wel.
- DROOLS-295 is nieuw in het register (`LIMIT`); een catalogus van de distributie die de Drools-codes spiegelt, krijgt
  hem mee.
