# FLAVOURS-OF-EMPTY — welke soorten afwezigheid de IR onderscheidt

*Status: VOORSTEL · 2026-11-04 · werkproza, niet normatief. Het resultaat van de kaart
`backlog/doing/20261104-flavours-of-empty.md`, na het besluit van de eigenaar van 2026-11-04 dat een schrijver die met
een lege waarde vuurt niets beslist (RUNTIME-SEMANTICS ch. 1.2 Assignment, ch. 3.11 "Leeg in a write", ch. 7.8). Dit
document verandert geen spec, schema, verifier, uitvoerder, frontend, gate-script of corpusbestand; het staat buiten elke
zone van gate 1. De RegelSpraak-specificatie v2.3.0 (© Belastingdienst, alle rechten voorbehouden) wordt alleen met
paragraafnummer en in eigen woorden aangehaald. Verwijzingen naar uchar gelden voor main `474732d3c`. De metingen [M] zijn
op 2026-11-04 gedaan: uchar's frontend, ref en clingo-runner gebouwd op die commit, de Drools-jar van dezelfde dag; ALEF
op commit `872b859` in alef-bridge (kaart `backlog/doing/20261104-empty-kinds-in-alef.md` op de branch
`card/empty-kinds-in-alef`, commit `c980d68`); Catala 1.2.1; clingo 5.8.2; sqlite 3.43.2. De programma's, uitvoer en
bronnotities staan in het onderzoeksrapport buiten de repo (`research/flavours-of-empty/`); de programma's van §6 staan
hier letterlijk.*

## 0. Markeringen

- **[Q]** een letterlijk citaat — van uchar's eigen tekst of van een externe bron die geen RegelSpraak-specificatie is;
- **[P]** een parafrase van een bron (de RegelSpraak-specificatie wordt alleen geparafraseerd);
- **[I]** een gevolgtrekking van dit voorstel, in geen bron gevonden;
- **[M]** een meting voor dit voorstel.

`RS` is `spec/RUNTIME-SEMANTICS.md`; `§` zonder bronnaam is een paragraaf van de RegelSpraak-specificatie v2.3.0, `§`
met een bronnaam ervoor (XACML, SEP, een artikel) een paragraaf van die bron.

## 1. Het antwoord in het kort

1. **Tien soorten afwezigheid** komen in regelgeving, uitvoeringspraktijk en de onderzochte talen voor (§3): nog niet
   afgeleid, niet aangeleverd, bekend onbekend, niet van toepassing, bekend onwaar, onbepaald door een fout,
   tegenstrijdig, bewust gewist, een lege verzameling en niet uitdrukbaar. Afgeschermd (privacy) en tijdelijk onbekend
   zijn varianten die de bronnen noemen; ze vallen onder dezelfde rijen.
2. **Geen gevestigde regeltaal draagt al die soorten als waarden.** De talen met een formele semantiek kiezen een van
   drie wegen (§4): één afwezige waarde met vaste regels per operator (SQL, FEEL, RegelSpraak, ALEF, uchar); afwezigheid
   als type dat de auteur zelf indeelt en verplicht afhandelt (Catala `optional` en eigen enumeraties, L4 `MAYBE`); of
   afwezigheid als waarheidswaarde van een uitspraak (answer set programming met `not p` tegen `-p`, de logica's van
   Kleene en Belnap, Oracle's `uncertain`). Systemen die meer soorten benoemen, doen dat naast de waarde: FHIR met een
   eigen element `dataAbsentReason` dat een waarde uitsluit, XACML met een uitkomst `NotApplicable` of `Indeterminate`
   op de beslissing. Codd's voorstel voor twee soorten null in SQL (1990) is niet overgenomen, en DMN beweegt sinds
   2023 de andere kant op: null alleen nog voor ontbrekend, niet voor een fout (DMN16-91).
3. **uchar draagt zes soorten al zonder nieuwe vorm** (§5): nog niet afgeleid en niet aangeleverd (leeg, RS ch. 1.1,
   5.7, 7.8), niet van toepassing door een conditie die niet geldt (leeg), bekend onwaar (een Boolean-attribuut met de
   waarde onwaar, §3.3.3/§3.5), onbepaald door een fout (leeg met een §9.5-feit of een evaluatiefout, RS ch. 1.4 punt
   4, ch. 5.6, ch. 5.7b), tegenstrijdig (EF-016, RS ch. 5.7; een ongeordende defaultsgroep, RS ch. 7.2) en de lege
   verzameling (`[]`, aantal 0, RS ch. 1.2 en 10.1).
4. **"Bekend onbekend" schrijf je als gegeven naast de waarde, niet als tweede leeg** (§7). In RegelSpraak vandaag: een
   attribuut met een enumeratiedomein (`'getaxeerd' | 'onbekend' | 'niet van toepassing'`) naast het bedrag; in de IR
   ook als constructor van een somtype (RS ch. 9, het anker `jutterswet-beschikkingsom`). Zo'n stand is een gewone
   gevulde waarde: een regel kan hem schrijven, hij beslist, hij staat tegen een andere schrijver (EF-016) en de
   rekenregels van ch. 1.2 veranderen niet. [M] Het voorbeeld van §6.1 speelt in ref en beide Drools-branches; clingo
   weigert de stand in de invoer van een berekend doel.
5. **Een regel schrijft geen leeg** (§8, N2): bewust wissen blijft onuitdrukbaar, zoals het besluit van 2026-11-04
   bepaalt. [M] ALEF ondersteunt dat in drie van zijn vier routes: een gelijkstelling aan `leeg` laat invoer 40 staan, en
   ALEF's editor waarschuwt "Gelijkstelling met leeg doet niets"; alleen ALEF's gegenereerde eager-code wist, en daar
   wint de laatste regel in de volgorde.
6. **Zes normvragen** staan in §8, elk met een aanbeveling. De kern: geen tweede afwezige waarde (N1), geen schrijfbaar
   leeg (N2), geen expliciet leeg in het caseformaat (N3), het standpatroon als schrijfwijze (N4), geen dialectsuiker
   voordat een statuut in het corpus erom vraagt (N5), en een kenmerk blijft tweewaardig (N6).

## 2. Wat uchar nu heeft

- **Eén leeg.** RS ch. 1.1 [Q]: "**empty** (leeg): the distinguished absence of a value, belonging to no datatype".
  Apart daarvan `Unresolved`, [Q] "an *unresolvable construct*, not an absent value" (CONSTRUCTS rij 16).
- **Regels per context.** RS ch. 1.2: plus, min, maal, percentage en het deeltal van een deling lezen leeg als 0;
  verminderd met, macht, wortel, absolute waarde, afronding en begrenzing propageren; een vergelijking met leeg is
  onwaar, de ongelijkheid is waar bij precies één lege kant (§8.1.1, Tabel 44); de som over alleen lege waarden is
  leeg, aantal en totaal zijn 0. RS ch. 1.4 [Q]: "Empty-as-unknown (FEEL-style null propagation) is a non-goal."
- **Tijdlijnen.** RS ch. 3.1: een gat en een leeg segment zijn twee representaties van "leeg hier", gelijk onder
  `read`.
- **Schrijven.** RS ch. 3.11, ch. 5.7 en ch. 7.8: een schrijver die met leeg vuurt beslist niets; op een tijdlijndoel
  beslist hij alleen de dagen waarop zijn tijdlijn een waarde leest; op een dag die niemand beslist houdt het doel zijn
  invoer of blijft het leeg; twee schrijvers met verschillende waarden geven EF-016.
- **Defaults.** RS ch. 7.1 [Q]: "A defaults group never writes leeg; consequently "deliberately assign leeg" is not
  expressible as a default", en de fall-through "does **not** distinguish "default inapplicable" from "default
  applicable with an empty value"". Catala's ∅ (EmptyError) is in uchar gewoon leeg, een gevolgde afwijking
  (`spec/proposals/PHASE5-CATALA.md`, tabel r. 75; CONSTRUCTS rij 8). De initialisatie is de laagste default (ch. 7.3),
  de case-invoer de virtual input default (ch. 7.8).
- **Het kanaal.** RS ch. 5.6: een deling door leeg of door 0, een negatieve wortel en andere vormen geven leeg plus een
  §9.5-feit met de EF-code van de soort; een evaluatiefout (ch. 5.7b) geeft de sessie geen antwoord.
- **Waarden die nooit leeg zijn.** Een constructorwaarde is gevuld en een `Construct` met een leeg argument is leeg
  (RS ch. 9.1); een `Match` op leeg is leeg, "an explicit leeg-arm is a recorded non-goal" (ch. 9.2); `[]` is een
  waarde (ch. 10.1).
- **Kenmerk en Boolean.** Een kenmerk is standaard onwaar (RS ch. 5.3, §3.5); een Boolean-attribuut is waar, onwaar of
  leeg (§3.3.3, §3.5).
- **Invoer.** Het caseformaat [Q]: "Absence means empty (leeg); an input never carries an explicit empty."
  (`spec/schema/uchar-case-v0.schema.json`, `attributes`). Een kenmerk in de invoer is aanwezig of niet
  (`KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md` §5.8).
- **Opgave boven berekening.** De markering `(mag opgegeven worden)` (geland; `KENMERKINVOER-…` §5) houdt een opgegeven
  en een berekende waarde als twee attributen naast elkaar, met een §9.5-feit als ze verschillen. Een opgave per
  periode gaat voor de berekening (besluit 2026-11-04).

## 3. De soorten afwezigheid

Elke soort met de bronnen die haar noemen. De namen tussen haakjes zijn die van de bron.

| # | soort | omschrijving (eigen woorden) | bronnen |
|---|---|---|---|
| K1 | **nog niet afgeleid** | geen regel heeft (nog) een waarde gegeven; tijdens de evaluatie een tussenstand, aan het fixpunt de uitkomst "geen schrijver beslist" | RS ch. 5.1, 7.2 (under-determination); Catala ∅ (Merigoux e.a. 2021, §4.1); Kleene's u voor een niet-gedefinieerde berekening (SEP "Many-Valued Logic" §2.2); Oracle `unknown`; §3.2, figuur 2 (een attribuut dat nog geen waarde kreeg) |
| K2 | **niet aangeleverd** | de invoer bevat het gegeven niet; niet gevraagd of niet doorgegeven | HL7 NASK (not asked), NP (not present); FHIR `not-asked`; XACML "missing attribute" (§7.3.5); ALEF verstekwaarde (`docs/services/service.md`); OpenFisca default value; Catala `context` |
| K3 | **bekend onbekend** | vastgesteld is dat het gegeven er niet is of niet te krijgen is: gevraagd, maar onbekend of geweigerd; ook tijdelijk onbekend | HL7 UNK, ASKU, NAV; FHIR `unknown`, `asked-unknown`, `temp-unknown`, `asked-declined`; Oracle `uncertain`; Blawx "unknown facts" (Morris 2023); Gelfond en Lifschitz 1991 (antwoord "unknown"); Marković e.a. 2022 situatie (iii); §3.5 (een leeg Boolean-attribuut heet daar ook onbekend) |
| K4 | **niet van toepassing** | het gegeven heeft voor dit object geen zinvolle waarde | HL7 NA; FHIR `not-applicable`; Codd's I-mark (1986, 1990); Marković e.a. 2022 situatie (i); XACML `NotApplicable` (op een beslissing); DMN streep `-` en RegelSpraak `n.v.t.` in een beslistabel (op een voorwaarde, §12) |
| K5 | **bekend onwaar** | voor een ja/nee-gegeven: vastgesteld nee, tegenover niet vastgesteld | ASP `-p` tegen `not p` (Gelfond en Lifschitz 1991); Wagner 2003 ("explicit falsity" tegen "non-truth"); RuleML `Neg` tegen `Naf`; §3.5 (Boolean tegen kenmerk) |
| K6 | **onbepaald door een fout** | de waarde had er moeten zijn, maar een berekening of bron faalde | FHIR `error`, `not-a-number`, `±infinity`; HL7 INV, NINF, PINF; XACML `Indeterminate`; FEEL null bij een typefout (DMN 1.5 §10.3.2.2; DMN16-91); Catala `DivisionByZero`, `NoValue`; §6.5 (deling door leeg is een fout); RS ch. 5.6, 5.7b |
| K7 | **tegenstrijdig** | twee bronnen of regels geven verschillende waarden | Belnap's "both" (SEP §2.3); Catala ⊛ (Conflict); ASP onvervulbaar; DMN Unique-tabel met overlap (Marković e.a. 2022 §3); RS ch. 5.7 (EF-016), ch. 7.2 (over-determination) |
| K8 | **bewust gewist** | een waarde die er was, wordt door een regel of besluit weggehaald | SQL `UPDATE … SET x = NULL`; §9.1 (gelijkstelling aan leeg geeft leeg; over invoer zwijgt de paragraaf); ALEF-waarschuwing "Gelijkstelling met leeg doet niets" |
| K9 | **lege verzameling** | er zijn geen leden; de verzameling zelf bestaat | RS ch. 10.1 (`[]` is een waarde), ch. 1.2 (aantal 0, som leeg); §5.8; SQL `COUNT` 0 en `SUM` NULL; FEEL `sum([]) = null` (DMN 1.5 §10.3.4); Catala `ListEmpty` |
| K10 | **niet uitdrukbaar** | de regeltaal kan het gevraagde niet zeggen; een eigenschap van de vorm, niet van de waarde | uchar `Unresolved` (CONSTRUCTS rij 16, WF-044); Prolog `existence_error` voor een ongedefinieerd predicaat (SWI-Prolog, vlag `unknown`); Catala `impossible`; Hart's open textuur (1961, h. VII) als de juridische kant |

Afgeschermd (HL7 MSK, FHIR `masked`) is een eigenschap van wat een systeem naar buiten geeft, niet van de evaluatie;
het blijft hier buiten beschouwing.

## 4. De talen

### 4.1 Welke soorten een taal kent, en hoe ze heten

Een cel noemt de vorm waarin de taal de soort uitdrukt; `—` betekent: geen eigen vorm.

**De omgeving van uchar**

| soort | RegelSpraak v2.3.0 | ALEF 872b859 | werkbank-regelsets | uchar |
|---|---|---|---|---|
| K1 nog niet afgeleid | leeg (§3.2) | leeg | leeg | leeg |
| K2 niet aangeleverd | leeg | leeg; verstekwaarde in een dienst | leeg | leeg (afwezig in de case) |
| K3 bekend onbekend | leeg (§3.5 noemt een leeg Boolean-attribuut ook onbekend) | — | — (datalacune als kenmerk, `dgl-18`) | — |
| K4 niet van toepassing | `n.v.t.` in een beslistabelcel (§12) | — | voorwaarde van een wetsbepaling | leeg (geen schrijver beslist) |
| K5 bekend onwaar | Boolean onwaar (§3.3.3); kenmerk kan het niet (§3.5) | Boolean onwaar [M] | kenmerk afwezig | Boolean onwaar; kenmerk afwezig (ch. 5.3) |
| K6 fout | een fout in de operatortabellen (§6.5, §8.1.1, §9.7.6) | — | — | leeg + §9.5-feit; evaluatiefout (ch. 5.6, 5.7b) |
| K7 tegenstrijdig | — | `RuntimeException` "Reassignment of a LazyCalculated property" (lazy) | — | EF-016; kanaalfeit van een ongeordende groep |
| K8 bewust gewist | gelijkstelling aan leeg (§9.1) | gewaarschuwd, geen effect; gegenereerde eager-code wist [M] | — | niet uitdrukbaar (sinds 2026-11-04) |
| K9 lege verzameling | aantal 0, som leeg of 0 (§5.8) | idem | idem | `[]`, aantal 0, som leeg (ch. 1.2, 10.1) |
| K10 niet uitdrukbaar | — | — | — | `Unresolved` |

**Gevestigde talen met een formele semantiek**

| soort | Catala 1.2.1 | DMN 1.5 / FEEL | ASP (clingo, s(CASP)) | Prolog / Datalog | SQL | Kleene / Bochvar / Belnap |
|---|---|---|---|---|---|---|
| K1 | ∅, eindigt in `NoValue` [M] | null (geen regel geraakt, Tabel 46) | `not p` (niet afleidbaar) | faalt (negation as failure) | — | Kleene's u |
| K2 | `context`-variabele met standaard | null | geen feit | geen feit (gesloten wereld) | NULL | u |
| K3 | eigen constructor [M] | null | geen van `p`, `-p`; abducible in s(CASP)/Blawx | — | NULL | u |
| K4 | `Absent` of eigen constructor | null; streep `-` op een voorwaarde | — | — | NULL (Codd's I-mark niet in de standaard) | — |
| K5 | `false` | `false` naast null | `-p` | — (alleen `\+`) | `FALSE` naast NULL | f naast u |
| K6 | `DivisionByZero`, `Impossible`, … (fout) | null (typefout); B-FEEL scheidt dit | — | `existence_error` | fout (deling door 0) | Bochvar's zinloze waarde |
| K7 | `Conflict` (⊛), ook bij gelijke berekende waarden [M] | Unique-schending; hit policy Any bij gelijke uitkomst | onvervulbaar [M] | — | — | Belnap's b |
| K8 | — | — | — | `retract/1` (buiten de logica) | `SET x = NULL` | — |
| K9 | `ListEmpty`; `optional` bij `content of … such that` | `sum([]) = null` | — | — | `COUNT` 0, `SUM` NULL [M] | — |
| K10 | `impossible` | — | — | ongedefinieerd predicaat | — | — |

**Standaarden en regelsystemen uit de praktijk**

| soort | LegalRuleML 1.0 | XACML 3.0 | L4 | OpenFisca | Oracle Intelligent Advisor | HL7 v3 / FHIR R4 |
|---|---|---|---|---|---|---|
| K1 | (defeasible: niet afgeleid) | `NotApplicable` | `NOTHING` | default value | `unknown` | NI |
| K2 | — | missing attribute: lege bag of `Indeterminate` | `NOTHING` | default value (0, onwaar, "", 1970-01-01) | `unknown` | NASK, NP; `not-asked` |
| K3 | — | — | eigen type | — | `uncertain` | UNK, ASKU, NAV; `unknown`, `asked-unknown`, `temp-unknown`, `asked-declined` |
| K4 | — | `NotApplicable` (beslissing) | eigen type | — | — | NA; `not-applicable` |
| K5 | `-head` (klassieke negatie) | `Deny` | `FALSE` | `False` | `false` | — |
| K6 | — | `Indeterminate`, `{P}`, `{D}`, `{DP}` | `EITHER` (fout als gegeven) | — | — | INV, NINF, PINF; `error`, `not-a-number` |
| K7 | defeater, `Override` | combining algorithms | — | — | — | — |
| K8 | — | — | — | — | — | — |
| K9 | — | lege bag | lege lijst | — | — | — |
| K10 | `Alternatives` (interpretaties) | — | — | — | — | `unsupported` |

### 4.2 Wat de afwezigheid van een taal doet in rekenen, vergelijken, schrijven en defaults

| taal | rekenen | vergelijken | schrijven | defaults |
|---|---|---|---|---|
| RegelSpraak | per operator: 0 bij plus, min, maal, percentage, deeltal; leeg bij verminderd met (links), wortel, macht, datum; fout bij deling door leeg (§6) | onwaar; ongelijk waar bij precies één lege kant; fout bij twee lege datums (§8.1.1) | gelijkstelling aan leeg geeft leeg; over invoer zwijgt §9.1 | initialisatie alleen op leeg (§9.6) |
| ALEF | als RegelSpraak | [M] Boolean: `= onwaar` onwaar op leeg, `≠ waar` waar op leeg, `is leeg` alleen op leeg | [M] leeg beslist niets, behalve in gegenereerde eager-code | initialisatie op leeg doet niets (test `ALEF3393`); verstekwaarde voor invoer |
| uchar | RS ch. 1.2 (de spiegel van RegelSpraak) | RS ch. 1.2 | beslist niets (ch. 3.11, 5.7, 7.8) | valt door (ch. 7.1), schrijft nooit leeg |
| Catala | geen lege waarde; `optional` vraagt een `match` (typefout bij `+`) [M] | idem | ∅ zonder regel: fout `NoValue` [M] | uitzonderingsboom; `context`; `condition` standaard onwaar |
| DMN / FEEL | propageert (`1 + null = null`, Tabel 56) | Kleene (Tabel 50); `null = null` waar (Tabel 49) | — (geen schrijfactie) | default output van een tabel, anders null (§8.2.12) |
| S-FEEL | niet gedefinieerd (§9.4) | niet gedefinieerd | — | — |
| ASP | — | `not p` tegen `-p` [M] | — (afleiden) | gesloten wereld per predicaat als regel |
| Prolog | — | `\+` faalt of slaagt | `assert`/`retract` | negation as failure |
| SQL | propageert [M] | Kleene; `WHERE` leest unknown als onwaar [M] | `SET x = NULL` | `DEFAULT` van een kolom |
| L4 | `MAYBE` vraagt patroonherkenning | idem | — | — |
| OpenFisca | geen afwezigheid; standaardwaarde per type | idem | — | default value per variabele |
| Oracle IA | `uncertain` en `unknown` stijgen op door de regels; een ware OF-tak beslist | operatoren `known`, `unknown`, `certain`, `uncertain`, `currently known` | — | `unknown` om een standaard te zetten |
| XACML | — | — | — | combining algorithms; `MustBePresent` kiest lege bag of fout |
| HL7 / FHIR | — (gegevensmodel) | — | `dataAbsentReason` alleen zonder waarde (obs-6) | — |

## 5. Per soort: wat ze met de semantiek van uchar zou doen

Per soort: draagt de IR haar nu, en wat zou het kosten om haar als **waarde** in te voeren, tegenover als **gegeven**
naast de waarde (een attribuut met een enumeratiedomein, een kenmerk, of een constructor van een somtype).

- **K1 nog niet afgeleid.** Gedragen. Het fixpunt (RS ch. 5.1) maakt een tussenstand onzichtbaar; aan het fixpunt is
  het doel leeg of houdt het zijn invoer (ch. 5.7). Geen wijziging.
- **K2 niet aangeleverd.** Gedragen als leeg; een standaard voor ontbrekende invoer is een initialisatie (ch. 5.4, 7.3)
  of een default. ALEF legt die standaard bij de dienst (verstekwaarde), OpenFisca in elke variabele, XACML per
  attribuut (`MustBePresent`). [I] De open/gesloten-markering per declaratie (RS ch. 1.4 punt 3) zou K2 per attribuut
  kunnen splitsen in "afwezig is onwaar of 0" en "afwezig is onbekend"; dat verandert ch. 1.2 per declaratie en is hier
  niet nodig, want een initialisatie geeft de gesloten lezing per attribuut al.
- **K3 bekend onbekend.** Niet gedragen als waarde. *Als waarde* (een tweede afwezigheid `onbekend` in elk datatype):
  ch. 1.2 krijgt per operator een kolom (propageert `onbekend` of telt het als 0? FEEL en SQL propageren; plus in
  RegelSpraak telt leeg als 0), elke vergelijking een driewaardige regel of een vaste keuze (ch. 1.4 heeft
  empty-as-unknown afgewezen), ch. 3.1 een segment met die waarde, ch. 3.11 en 5.7 een schrijver die `onbekend` schrijft
  en daarmee beslist (hij verdringt de invoer en botst met een schrijver van 40: EF-016), ch. 7.1 een nieuwe definitie
  van "gevuld" voor de fall-through, het caseformaat een spelling, en ref, clingo en beide Drools-branches een nieuwe
  waarde in elke operator. RegelSpraak en ALEF hebben er geen vorm voor. *Als gegeven*: geen wijziging van de
  semantiek; de stand is een gevulde waarde die een regel schrijft, die beslist en die botst zoals elke waarde (§6.1).
- **K4 niet van toepassing.** Gedragen als leeg wanneer de voorwaarde van de regel niet geldt (geen schrijver beslist),
  en als subtype wanneer het attribuut alleen op een subtype bestaat (RS ch. 5.8b). Niet te onderscheiden van K1 en K2
  in de waarde; RS ch. 7.1 kiest dat bewust ("does **not** distinguish"). Waar de uitkomst het onderscheid moet tonen,
  is "niet van toepassing" een stand (§6.1) of een kenmerk. De uitleg (RS ch. 5.12, de lezing aan het fixpunt) toont
  al welke voorwaarde niet gold.
- **K5 bekend onwaar.** Gedragen voor een Boolean-attribuut (drie standen); niet voor een kenmerk (gesloten, ch. 5.3).
  [M] ALEF leest de drie standen in alle vier de routes zoals RS ch. 1.2 (§6.2).
- **K6 onbepaald door een fout.** Gedragen: de waarde wordt leeg en het §9.5-kanaal draagt een feit met de EF-code
  (ch. 5.6), of de sessie faalt (ch. 5.7b). Zo scheidt uchar al wat DMN16-91 voor FEEL wil scheiden: leeg is de
  waarde, de fout staat ernaast. Geen wijziging.
- **K7 tegenstrijdig.** Gedragen: twee schrijvers met verschillende waarden geven EF-016 (ch. 5.7); een ongeordende
  defaultsgroep schrijft niet en geeft één kanaalfeit (ch. 7.2). Geen wijziging.
- **K8 bewust gewist.** Niet uitdrukbaar, sinds het besluit van 2026-11-04 ook niet voor een rekenende regel. *Als
  schrijfbaar leeg*: ch. 3.11 en 7.8 draaien terug (een lege waarde beslist dan wel), ch. 7.1's "never writes leeg"
  vervalt, en twee schrijvers van leeg en 50 botsen weer (EF-016) of moeten een rangorde krijgen. Op een tijdlijn zou
  een wis-schrijver de dagen van zijn periode beslissen met leeg, wat ch. 3.1 niet van een gat kan onderscheiden. [I]
  Een wetstekst die een waarde laat vervallen, beschrijft een nieuwe toestand, en die is een stand of kenmerk
  (`vervallen`), waar de lezers van de waarde op toetsen.
- **K9 lege verzameling.** Gedragen (`[]`, aantal 0, som leeg of met terugvalclausule 0). Geen wijziging.
- **K10 niet uitdrukbaar.** Gedragen door `Unresolved` (WF-044): een vorm, geen waarde. Geen wijziging.

## 6. Uitgewerkte voorbeelden

Eigen Jutterswet-tekst. Elk programma liep op 2026-11-04 door uchar's RegelSpraak-frontend (`--ir`) en daarna door ref,
de clingo-runner en de Drools-backend (exec-model en DRL-tekst) [M]; de case-bestanden staan in het onderzoeksrapport.

### 6.1 K3 en K4 als stand naast de waarde (RegelSpraak, vandaag)

```
Domein Waarderingsstand is van het type Enumeratie
    'getaxeerd'
    'onbekend'
    'niet van toepassing'

Objecttype de Vondst (mv: Vondsten)
    de taxatie Numeriek (getal met 2 decimalen);
    de waarderingsstand Waarderingsstand;
    het vindloon Numeriek (getal met 2 decimalen);
    is onleesbaar kenmerk (bijvoeglijk);
    is drijfhout kenmerk (bijvoeglijk);

Regel stand getaxeerd
    geldig altijd
        De waarderingsstand van een Vondst moet gesteld worden op 'getaxeerd'
        indien zijn taxatie gevuld is.

Regel stand onbekend
    geldig altijd
        De waarderingsstand van een Vondst moet gesteld worden op 'onbekend'
        indien hij onleesbaar is.

Regel stand niet van toepassing
    geldig altijd
        De waarderingsstand van een Vondst moet gesteld worden op 'niet van toepassing'
        indien hij drijfhout is.

Regel vindloon naar taxatie
    geldig altijd
        Het vindloon van een Vondst moet berekend worden als zijn taxatie gedeeld door 10 rekenkundig afgerond op 2 decimalen
        indien zijn waarderingsstand gelijk is aan 'getaxeerd'.

Regel forfaitair vindloon
    geldig altijd
        Het vindloon van een Vondst moet gesteld worden op 25,00
        indien zijn waarderingsstand gelijk is aan 'onbekend'.

Regel geen vindloon
    geldig altijd
        Het vindloon van een Vondst moet gesteld worden op 0,00
        indien zijn waarderingsstand gelijk is aan 'niet van toepassing'.
```

| case | invoer V1 | waarderingsstand | vindloon | soort | ref | clingo | Drools exec / DRL |
|---|---|---|---|---|---|---|---|
| 1 | taxatie 300,00 | **getaxeerd** | **30,00** | waarde | PASS | PASS | PLAYED |
| 2 | onleesbaar | **onbekend** | **25,00** | K3 | PASS | PASS | PLAYED |
| 3 | drijfhout | **niet van toepassing** | **0,00** | K4 | PASS | PASS | PLAYED |
| 4 | niets | **leeg** | **leeg** | K1/K2 | PASS | PASS | PLAYED |
| 5 | waarderingsstand `onbekend` | **onbekend** | **25,00** | K3 in de invoer | PASS | weigert ("a case-supplied text/enum computed target … has no encoding here (no corpus case)") | PLAYED |
| 6 | taxatie 300,00 en onleesbaar | — | — | K7 | **EF-016** op `waarderingsstand`, regels "stand getaxeerd" en "stand onbekend", waarden getaxeerd en onbekend | EF-016 (de botsing op `vindloon` genoemd) | EF-016 (twee fouten: `waarderingsstand` en `vindloon`) |

Wat het laat zien: case 2 en 3 schrijven een "bekend onbekend" en een "niet van toepassing" met een gewone regel; ze
beslissen en het vindloon volgt per soort. Case 4 toont dat "nog niets bekend" leeg blijft en van case 2 te
onderscheiden is. Case 5 toont dat een bekend onbekend ook als invoer kan komen. Case 6 toont dat twee bronnen die het
oneens zijn een fout geven, geen stille keuze. [I] De drie uitvoerders noemen in case 6 niet dezelfde attributen; gate 8
houdt een rode case alleen aan de gepinde code en parameters, dus een golden of rode case van deze vorm moet de
parameters pinnen die alle drie geven.

### 6.2 K5: bekend onwaar tegenover onbekend (Boolean-attribuut)

```
Objecttype de Vondst (mv: Vondsten)
    de bevestiging Boolean;
    de a Numeriek (geheel getal);
    de b Numeriek (geheel getal);
    de c Numeriek (geheel getal);

Regel a uit bevestiging
    geldig altijd
        De a van een Vondst moet gesteld worden op 1
        indien zijn bevestiging gelijk is aan onwaar.

Regel b uit bevestiging
    geldig altijd
        De b van een Vondst moet gesteld worden op 1
        indien zijn bevestiging ongelijk is aan waar.

Regel c uit bevestiging
    geldig altijd
        De c van een Vondst moet gesteld worden op 1
        indien zijn bevestiging leeg is.
```

| case | bevestiging | a | b | c | ref | clingo | Drools exec / DRL | ALEF (4 routes) |
|---|---|---|---|---|---|---|---|---|
| 1 | waar | **leeg** | **leeg** | **leeg** | PASS | weigert ("unscalable datatype in the ASP translation") | PLAYED | gelijk |
| 2 | onwaar | **1** | **1** | **leeg** | PASS | weigert | PLAYED | gelijk |
| 3 | niet gegeven | **leeg** | **1** | **1** | PASS | weigert | PLAYED | gelijk |

Wat het laat zien: "bekend onwaar" (case 2) en "onbekend" (case 3) zijn te onderscheiden met `gelijk is aan onwaar` en
`is leeg`; `ongelijk is aan waar` maakt ze gelijk, volgens §8.1.1 en RS ch. 1.2 (Tabel 44). ALEF (`e4` in alef-bridge,
dezelfde regels) geeft dezelfde negen waarden in gegenereerde code en interpreter, lazy en eager.

### 6.3 K6: onbepaald door een fout

```
Objecttype de Vondst (mv: Vondsten)
    de opbrengst Numeriek (getal met 2 decimalen);
    het aantal deelnemers Numeriek (geheel getal);
    het aandeel Numeriek (getal met 2 decimalen);

Regel aandeel van een vondst
    geldig altijd
        Het aandeel van een Vondst moet berekend worden als zijn opbrengst gedeeld door zijn aantal deelnemers
        rekenkundig afgerond op 2 decimalen.
```

| case | opbrengst | aantal deelnemers | aandeel | kanaal | ref | clingo | Drools exec / DRL |
|---|---|---|---|---|---|---|---|
| 1 | 100,00 | 4 | **25,00** | consistent | PASS | PASS | PLAYED |
| 2 | 100,00 | — | **leeg** | (aandeel van een vondst × V1) **EF-031** | PASS | PASS | PLAYED |
| 3 | 100,00 | 0 | **leeg** | (aandeel van een vondst × V1) **EF-005** | PASS | PASS | PLAYED |
| 4 | — | 4 | **0,00** | consistent | PASS | PASS | PLAYED |

Wat het laat zien: de waarde in case 2 en 3 is dezelfde leeg als in een case zonder gegevens; de soort staat op het
kanaal, met de code (EF-031 deling door leeg, EF-005 deling door 0). Case 4 is geen fout: het deeltal leeg telt als 0
(§6.5, RS ch. 1.2).

### 6.4 K3 en K4 als constructor van een somtype (de IR, ch. 9)

Het anker `corpus/golden/anchor/jutterswet-beschikkingsom`: `beschikking = Toegekend(bedrag) | Afgewezen |
Aangehouden`, met `uitkering` := match `beschikking`: `Toegekend(bedrag)` → bedrag + vaste voet 25,00; `Afgewezen` →
0,00; `Aangehouden` → 12,00 (RS ch. 9.2). [M] ref, 2026-11-04:

| case | beschikking | uitkering |
|---|---|---|
| 1 | `Toegekend(1250.50)` | **1275,50** |
| 2 | `Afgewezen` | **0,00** |
| 3 | `Aangehouden` | **12,00** |
| 4 | niet gegeven | **leeg** |

Wat het laat zien: een nullaire constructor is een bekende toestand zonder bedrag, te onderscheiden van leeg (case 4).
Een auteur die `Onbekend` en `NietVanToepassing` als constructoren declareert, krijgt de stand van §6.1 en het bedrag in
één attribuut; de `Match` moet elke constructor behandelen (WF-063), zoals Catala's `match` [M: de typefout "The
constructor Unknown of enum Taxatie is missing from this pattern matching"]. De RegelSpraak-frontend heeft geen bronvorm
voor een somtype (RS ch. 9, inleiding).

### 6.5 K8: een gelijkstelling aan leeg in ALEF

[M] alef-bridge, `EmptyKinds`, invoer taxatie 40 en de regel "De taxatie van een Vondst moet gesteld worden op leeg.":

| geval | regels | invoer | gegenereerd lazy | gegenereerd eager | interpreter lazy | interpreter eager |
|---|---|---|---|---|---|---|
| e1a | := leeg | taxatie 40 | 40 | leeg | 40 | 40 |
| e2b | := leeg; := opgave | taxatie 40, opgave 50 | 50 | 50 | 50 | 50 |
| e2rb | := opgave; := leeg | taxatie 40, opgave 50 | 50 | leeg | 50 | 50 |
| e3a | strandloon := leeg (maandtijdlijn) | strandloon 10 | 10 | 10 | 10 | 10 |

Wat het laat zien: ALEF's interpreter en lazy-code lezen de regel zoals uchar sinds 2026-11-04 (een lege waarde beslist
niets) en zoals ALEF's eigen waarschuwing; alleen de gegenereerde eager-code wist, afhankelijk van de volgorde. Met een
periode op een tijdlijndoel genereert ALEF Java die niet compileert (alef-bridge-kaart, Result).

## 7. Aanbeveling

1. **De IR houdt één afwezigheid als waarde: leeg** (RS ch. 1), met de regels van ch. 1.2. Geen tweede afwezige waarde
   en geen driewaardige logica in condities (ch. 1.4 blijft).
2. **Zes soorten zijn gedragen en houden hun vorm**: K1 en K2 (leeg), K4 door een voorwaarde die niet geldt (leeg),
   K5 (Boolean-attribuut onwaar), K6 (leeg met een §9.5-feit, of een evaluatiefout), K7 (EF-016; ch. 7.2), K9 (`[]`,
   aantal 0) en K10 (`Unresolved`).
3. **K3 (bekend onbekend, ook tijdelijk of geweigerd) en K4 als uitkomst zijn gegevens naast de waarde**: een attribuut
   met een enumeratiedomein (RegelSpraak, §6.1) of een constructor van een somtype (IR, §6.4). Een regel mag ze
   schrijven; het zijn gevulde waarden, dus ze beslissen, staan tegen invoer en botsen met een andere schrijver. Dit is
   de weg van Catala, L4 en FHIR (§4).
4. **K8 (bewust gewist) blijft onuitdrukbaar**: een regel schrijft geen leeg (besluit 2026-11-04). Een wetstekst die
   een waarde laat vervallen, wordt een stand of kenmerk waarop de lezers toetsen; een correctie van de invoer hoort bij
   de invoer.
5. **Geen nieuwe vorm in wire, schema of RegelSpraak-dialect** voor dit onderwerp. Wat wel volgt, zijn kaarten voor de
   uitvoerders die het patroon nog niet dragen (§9) en, als de eigenaar N4 aanneemt, een golden met het standpatroon.

## 8. Normvragen

**N1. Krijgt de IR naast leeg een tweede afwezigheid als waarde ("onbekend", of meer soorten zoals HL7's nullFlavor)?**
Aanbeveling: **nee**. Reden: een tweede waarde raakt elke operator van ch. 1.2, elke vergelijking, het schrijven
(ch. 3.11, 5.7), de fall-through van ch. 7.1 en vier uitvoerders, terwijl RegelSpraak en ALEF haar niet kennen; de
talen met een formele semantiek die meer soorten nodig hebben, laten de auteur ze als type declareren (Catala, L4), en
de standaarden die ze benoemen, zetten ze naast de waarde (FHIR obs-6, XACML). Codd's twee nulls zijn in SQL niet
overgenomen. Alternatief: een waarde `onbekend` die overal propageert zoals FEEL's null — afgewezen, omdat het ch. 1.4
heropent en `5 + leeg = 5` (RS ch. 1.2) laat botsen met `5 + onbekend = onbekend` in één document.

**N2. Mag een regel een attribuut bewust leegmaken?**
Aanbeveling: **nee**; het besluit van 2026-11-04 blijft. Reden: een schrijfbaar leeg draait ch. 3.11, 5.7 en 7.8 terug
en maakt een wis-schrijver op een tijdlijn ononderscheidbaar van een gat (ch. 3.1). §9.1 zegt alleen dat de uitkomst
leeg is; ALEF leest die uitkomst als "doet niets" en waarschuwt erbij. Alternatief: een eigen vorm "moet leeggemaakt
worden" die als enige leeg schrijft en beslist — niet aanbevolen zonder een statuut in het corpus dat het vraagt.

**N3. Moet het caseformaat een expliciet leeg (`null`) kunnen geven, apart van een ontbrekende sleutel?**
Aanbeveling: **nee**. Reden: zonder een tweede afwezige waarde (N1) heeft `null` geen andere betekenis dan de
ontbrekende sleutel, en "gegeven: onbekend" is een waarde van een standattribuut (§6.1 case 5). Het schema zegt nu
"an input never carries an explicit empty"; dat blijft.

**N4. Is het standpatroon de aanbevolen schrijfwijze van bekend onbekend en niet van toepassing, en in welke vorm?**
Aanbeveling: **ja; in RegelSpraak de enumeratie naast de waarde (§6.1), in de IR daarnaast het somtype (§6.4)**.
Reden: de enumeratie is vandaag uitdrukbaar en speelt in ref en beide Drools-branches; het somtype heeft geen bronvorm
in de RegelSpraak-frontend. Gevolg: twee kaarten voor de ASP-backend (§9), en een golden met het standpatroon zodra
die spelen.

**N5. Krijgt de RegelSpraak-dialect suiker voor het patroon, naar het voorbeeld van `(mag opgegeven worden)`?**
Aanbeveling: **nee, nu niet**. Reden: het patroon is drie regels en een domein in gewone RegelSpraak, en geen statuut in
het corpus vraagt het. Een dialectvorm (bijvoorbeeld een markering die een stand-attribuut en een domein genereert)
volgt pas als een golden met een wetstekst laat zien welke soorten die tekst onderscheidt.

**N6. Moet een kenmerk "bekend afwezig" kunnen krijgen (drie standen, zoals een Boolean-attribuut)?**
Aanbeveling: **nee**. Reden: §3.5 maakt het kenmerk tweewaardig en wijst voor drie standen naar het Boolean-attribuut;
ALEF raadt voor uitvoer boolean-attributen aan boven kenmerken (`docs/services/service.md`), en [M] leest de drie
standen van een Boolean zoals uchar. `KENMERKINVOER-…` §5.8 beschrijft dezelfde route voor een kenmerk dat een opgave
"niet aanwezig" moet kunnen krijgen.

## 9. Wat dit voorstel niet doet, en wat volgt

- **Geen wijziging** van RS, IR-SPEC, schema, verifier, uitvoerders, frontends of corpus.
- **ASP-backend** [M]: weigert een Boolean-attribuut in een conditie ("unscalable datatype in the ASP translation",
  `backends/clingo/lib/translate.ml` r. 164) en een door de case gegeven enumeratiewaarde op een berekend doel ("a
  case-supplied text/enum computed target … has no encoding here (no corpus case)"); beide meldingen hebben geen
  registercode aan de kop. Voorgestelde kaarten: `clingo-boolean-attribute-in-condition` en
  `clingo-enum-input-on-computed-target`.
- **Rapportage van EF-016** [M]: in §6.1 case 6 noemen ref, clingo en Drools niet hetzelfde attribuut (ref
  `waarderingsstand`; clingo `vindloon`; Drools beide). De code is gelijk; een rode case met die vorm moet alleen
  gedeelde parameters pinnen.
- **ALEF** [M]: de gegenereerde eager-code wist bij een gelijkstelling aan leeg, anders dan ALEF's interpreter; met een
  periode op een tijdlijndoel compileert de gegenereerde Java niet. Vervolgkaart in alef-bridge:
  `backlog/new/20261104-empty-in-a-period.md`. Geen gevolg voor uchar: uchar volgt de interpreter en de waarschuwing.
- **Catala** [M]: twee definities met dezelfde letterlijke consequentie (40 en 40) geven in Catala 1.2.1 geen conflict;
  met berekende gelijke waarden wel. RS ch. 7.2's "regardless of value equality" klopt voor de calculus (§4.1 van het
  artikel) en voor berekende waarden.

## Bronnen

Geraadpleegd op 2026-11-04. Bronnotities met de exacte plaats per bewering: `research/flavours-of-empty/notes/` buiten
de repo.

- RegelSpraak-specificatie v2.3.0 (4-12-2025), Belastingdienst: §3.2, §3.2.1, §3.3.3, §3.5, §5.8.2–§5.8.4, §6.2–§6.13,
  §8.1.1, §8.1.2, §9.1, §9.5, §9.6, §9.7.6, §12, §13.4.15, §13.4.17, Bijlage 1; en het document "typeringen" §5.
- ALEF, <https://github.com/belastingdienst/ALEF>, commit `872b859`: `languages/regelspraak/languageModels/structure.mps`
  (concept `Leeg`), `typesystem.mps` (`check_Gelijkstelling`), `AlefJava/merlin/src/main/java/nl/belastingdienst/merlin/lazy/MLazyProperty.java`,
  `solutions/ALEF_Testen/models/ALEF_Testen.ALEF3393.mps`, `solutions/Test_Spraken/models/Test_Spraken.RegelWarnings@tests.mps`,
  `docs/services/service.md`, `docs/regels/Actie_Initialisatie.md`.
- alef-bridge, kaart `20261104-empty-kinds-in-alef`, branch `card/empty-kinds-in-alef`, commit `c980d68`
  (`alef-models/EmptyKinds/expected.tsv`).
- Werkbank-regelsets (`regelspraak-rules`, `0aff7ed`): `dagloon-ww/rules/dgl-18-signaleringen-tijdvak.rgs`,
  `herleving-ww/rules/hww-20-startperiode.rgs`, `hww-11-grond-classificatie.rgs`.
- Merigoux, D., Chataing, N., Protzenko, J. (2021), "Catala: A Programming Language for the Law", *Proc. ACM Program.
  Lang.* 5 (ICFP), DOI 10.1145/3473582, arXiv 2103.03198v2, §4.1; Catala 1.2.1 (`runtimes/ocaml/catala_runtime.ml`,
  `compiler/shared_ast/definitions.ml`); het Catala-boek, <https://github.com/CatalaLang/catala-book> `e998b15`.
- OMG (2024), *Decision Model and Notation* 1.5, formal/24-01-01, <https://www.omg.org/spec/DMN/1.5/PDF>: §8.2.8,
  §8.2.12, §9.4, §10.3.2.2, §10.3.2.4, Tabellen 46, 49, 50, 51, 56; §10.3.4 (`sum`); p. 200.
- OMG-issue DMN16-91, "Binary operators should not return null", <https://issues.omg.org/issues/DMN16-91>.
- Marković, Đ., Vandevelde, S., Vennekens, J., Denecker, M. (2022), "On the Semantics of "null" in DMN: Undefined is
  not Unknown", RuleML+RR'22, CEUR-WS Vol. 3229, <https://ceur-ws.org/Vol-3229/paper61.pdf>.
- Gelfond, M., Lifschitz, V. (1991), "Classical Negation in Logic Programs and Disjunctive Databases", *New Generation
  Computing* 9:365–385, DOI 10.1007/BF03037169, <https://www.cs.utexas.edu/~vl/papers/clnegdd.pdf>.
- Wagner, G. (2003), "Web Rules Need Two Kinds of Negation", PPSWR 2003, LNCS 2901, p. 33–50, DOI
  10.1007/978-3-540-24572-8_3.
- Kowalski, R. (1989), "The treatment of negation in logic programs for representing legislation", ICAIL '89,
  p. 11–15, DOI 10.1145/74014.74016; Sergot, M. J. e.a. (1986), "The British Nationality Act as a Logic Program",
  *CACM* 29(5):370–386, DOI 10.1145/5689.5920 (niet zelf gelezen).
- Reiter, R. (1978), "On Closed World Data Bases", en Clark, K. L. (1978), "Negation as Failure", in Gallaire en Minker
  (red.), *Logic and Data Bases*, Plenum (niet zelf gelezen; geciteerd via Gelfond en Lifschitz 1991 en Kowalski 1989).
- Arias, J. e.a. (2018), "Constraint Answer Set Programming without Grounding", *TPLP* 18(3–4), arXiv 1804.11162;
  <https://github.com/SWI-Prolog/sCASP>.
- Morris, J. (2023), "Building Blawx", GDE 2023, CEUR-WS Vol. 3437, <https://ceur-ws.org/Vol-3437/paper5GDE.pdf>.
- SWI-Prolog manual, `\+/1` en de vlag `unknown`, <https://www.swi-prolog.org/pldoc/man?section=flags>.
- Codd, E. F. (1979), *ACM TODS* 4(4):397–434, DOI 10.1145/320107.320109; (1986), "Missing Information (Applicable and
  Inapplicable) in Relational Databases", *SIGMOD Record* 15(4), DOI 10.1145/16301.16303; (1990), *The Relational Model
  for Database Management, Version 2* (niet zelf gelezen; samengevat via <https://en.wikipedia.org/wiki/Null_(SQL)>).
- *Stanford Encyclopedia of Philosophy*, "Many-Valued Logic" (2026), <https://plato.stanford.edu/entries/logic-manyvalued/>,
  §2.2–§2.4 (Kleene, Bochvar, Belnap–Dunn, Łukasiewicz).
- OASIS (2021), *LegalRuleML Core Specification Version 1.0*,
  <https://docs.oasis-open.org/legalruleml/legalruleml-core-spec/v1.0/os/legalruleml-core-spec-v1.0-os.html>, §3.4, §4.1,
  §4.2.1, §4.2.4.
- Hoekstra, R. e.a. (2009), "LKIF Core", DOI 10.3233/978-1-58603-942-4-21; Gordon, T. F., Prakken, H., Walton, D.
  (2007), "The Carneades model of argument and burden of proof", *Artificial Intelligence* 171:875–896 (beide niet zelf
  gelezen); Hart, H. L. A. (1961), *The Concept of Law*, h. VII.
- OASIS (2013), *XACML Version 3.0*, <https://docs.oasis-open.org/xacml/3.0/xacml-3.0-core-spec-os-en.html>, §4.1.1,
  §5.29, §7.3.5, §7.10, §7.11, §7.19.3.
- Legalese, L4 Glossary, <https://legalese.com/l4/reference/GLOSSARY>.
- OpenFisca, "Variables", <https://openfisca.org/doc/key-concepts/variables.html>; `openfisca_core/variables/config.py`.
- Oracle Intelligent Advisor (Oracle Policy Modeling), "Certain and known operator rule examples" en "Decide whether
  to allow uncertainty in user answers", <https://docs.oracle.com/html/E79061_01/>.
- HL7 v3 NullFlavor, <http://terminology.hl7.org/CodeSystem/v3-NullFlavor>; FHIR R4 DataAbsentReason,
  <http://terminology.hl7.org/CodeSystem/data-absent-reason>; FHIR R4 Observation (obs-6),
  <https://hl7.org/fhir/R4/observation-definitions.html#Observation.dataAbsentReason>.
