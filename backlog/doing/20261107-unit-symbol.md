# unit-symbol: het symbool van een eenheid op de draad, in een tekstdeel en in de bron

*Besluit van de eigenaar, 2026-11-07, in de hoofdsessie besproken en goedgekeurd (spectekst hieronder). Vervolg op
`backlog/done/20261009-tekst-van-een-waarde-in-een-tekstsamenstelling.md` ("Vraag voor de eigenaar", punt 2).*

Goal: een eenheidsdeclaratie draagt haar symbool in een eigen veld `DeclaredUnit.symbol`. Een tekstdeel `«X (eenheid)»`
schrijft elke factor met het symbool van zijn declaratie, waar die er een heeft, en anders met zijn afkorting
(`5,50 (€)`, `1200,00 (€/jr)`, `1,25 (kg)`, `12,50 (%)`). In de bron is het token na de afkorting op een eenheidsregel het
symbool. Twee eenheden mogen één symbool dragen; een verwijzing met zo'n symbool is dan dubbelzinnig en een fout, een
verwijzing met de afkorting (`met eenheid USD`) werkt altijd.

## Aanleiding en normbron

- RegelSpraak-specificatie §3.7 (lopende tekst en voorbeeld `de euro (mv: euros) EUR €`): een eenheid heeft een
  naamwoord en eventueel een afkorting en/of een symbool. De EBNF van §13.3.5.1 kent één `<eenheidsafkorting>`; onze
  grammatica neemt er al willekeurig veel (`eenheidAfkorting*`). De specificatie verwijst zelf met het symbool naar de
  eenheid (`met eenheid €`, `1000 €`); het corpus doet dat 87 keer in `met eenheid`.
- ALEF (gemeten 2026-11-07 op 872b859, alleen als achtergrond; de IR-SPEC noemt ALEF niet): `BasisEenheid` heeft de
  aparte optionele eigenschappen `afkorting` en `symbool`; het standaardstelsel `Valuta ISO-4217` geeft AUD, CAD en USD
  alle drie `$`. Een symbool is dus geen identiteit. ALEF's runtime-tekstomzetting schrijft geen eenheid.
- Nu schrijft de frontend het tweede token als eerste spelling (`"spellings": ["€", "euro", "euros"]`), en de
  tekstomzetting schrijft de afkorting (`5,50 (EUR)`).
- Gemeten over alle `.rgs` van de repository: 150 eenheidsregels, 116 met één token na het naamwoord, 34 met twee
  (33 × `EUR €`, 1 × `kg kgm` in `jutterswet-stelselweging`), geen met drie of meer.

## Ontwerp (goedgekeurd)

### Draad en IR-SPEC ch. 2.5

`DeclaredUnit` krijgt het optionele veld `symbol` (string, `minLength: 1`), in de sleutelvolgorde tussen
`factorToCanonical` en `spellings`. Alleen op de declaratie; op een voorkomen (`Unit`) weigert het schema het al
(`additionalProperties: false`) en weigert elke lezer het, zoals `spellings`. Het symbool staat niet ook in `spellings`.
Geen WF-regel: geen uniciteit, en geen executor zoekt een eenheid op haar symbool.

Rij in de tabel van `Unit` (ch. 2.5):

> | `symbol` | string? | the symbol of the unit: a written form beside its abbreviation, in which a text part writes the unit (RUNTIME-SEMANTICS ch. 1.2); on its declaration in `units` only, never empty |

Alinea na de tabel (de bestaande zin "a factor of a unit that occurs on a `Number` is the same node without `spellings`"
wordt "without `symbol` and `spellings`"):

> *The symbol.* A unit is identified by its system and its abbreviation. Its symbol is only the form in which a value
> of the unit is written for a reader. Two units may carry one symbol, so no rule holds a symbol unique and no reader
> identifies a unit by it. A factor on a `Number` carries neither `symbol` nor `spellings`. The RegelSpraak frontend
> takes as the symbol the token after the abbreviation on a unit line (`de euro (mv: euros) EUR €`; RegelSpraak
> specification §3.7).

Bij `spellings` erbij: de emitters schrijven geen spelling gelijk aan het symbool (zoals nu al: geen gelijk aan de
afkorting). De passage in ch. 2.1 over de v0-sleutel `€` ("so `€` is a spelling of `EUR` (ch. 2.5)") naar de huidige
stand: `€` is het `symbol` van `EUR`. De IR-SPEC staat op zichzelf: geen ALEF-term, RegelSpraak alleen als verwijzing
naar de herkomst van de bronvorm. Normatieve tekst beschrijft de huidige stand, zonder kaartnamen of
uitvoerdersstatus.

### RUNTIME-SEMANTICS ch. 1.2

Bij een deel met `unit`: elke factor geschreven met het symbool van zijn declaratie (het `symbol` van de `DeclaredUnit`
met dezelfde `system` en `abbreviation`), waar die er geen heeft met zijn afkorting; `%` en de kale kalendereenheden
(zonder `system`) met hun afkorting; verder zoals nu (positieve factoren met `·`, dan `/` en elke negatieve, een macht
waar die niet 1 is). Voorbeelden en tabelrijen: `5,50 (€)` (EUR met symbool €), `5,50 (EUR)` bij een EUR zonder
symbool, `1,25 (kg)`, `12,50 (%)`, een samengestelde eenheid met een gemengde schrijfwijze (`€/kg` of `€/jr`). De
cursieve noot: "that unit is its abbreviation ... no role that marks one of them as its symbol" vervalt; de eenheid
schrijft haar symbool waar haar declaratie er een heeft, besluit van de eigenaar van 2026-11-07. Meldingen, de trace van
ch. 5.12, dumps en de case-uitvoer houden de afkorting (`Units.render`, `Unit.rendering()` blijven zoals ze zijn): alleen
het tekstdeel schrijft het symbool.

### Frontend (RegelSpraak)

- Een eenheidsregel is naamwoord, (mv), afkorting en eventueel symbool: het tweede token is het symbool. Een regel met
  één token heeft alleen een afkorting, ook als dat token `€` is. Een derde of verder token heeft geen rol en is een
  fout met een eigen RGS-code (gemeten: geen bron schrijft er een).
- Verwijzen (`resolveUnit`, `declaresUnit`): de volgorde stelsel voor stelsel blijft; binnen een stelsel eerst de
  afkorting, dan het symbool, dan woord en meervoud. Een symbool dat in het stelsel waar de verwijzing uitkomt op twee
  of meer eenheden staat, is dubbelzinnig: een fout met een eigen RGS-code die het symbool en de afkortingen van de
  kandidaten noemt, op de plek van de verwijzing (`met eenheid $`, `100 $`, ook in een samengestelde eenheid
  `$/jr` en in een omrekenspecificatie). Nooit stil de eerste kiezen.
- De declaratie met een gedeeld symbool (`USD $` en `CAD $` in één stelsel) is toegestaan. WF-009 (RGS-016) telt
  alleen de afkortingen. Een symbool gelijk aan een afkorting van zijn eigen stelsel (ook zijn eigen, `kg kg`) blijft een
  fout: het kan nooit naar zijn eenheid verwijzen; hergebruik RGS-016 als de zin past, anders een eigen code.
- De emitter schrijft `symbol` en haalt het symbool uit `spellings`. `IrEmitter` controleert al dat een spelling niet
  gelijk is aan de afkorting; idem voor het symbool.
- `grammar/DIVERGENCES.md`: de eenheidsregel (de EBNF kent één afkorting; het symbool volgt §3.7) en een gedeeld
  symbool; de sectie "De eenheid van een tekstdeel" naar het symbool.
- Register en beide catalogi voor de nieuwe codes (gate 10, `scripts/check-rgs-codes.sh`), tests in de frontend.

### Executors

- OCaml-loader (verifier, ref, clingo): `symbol` op een declaratie lezen (een lege string, `null` of een niet-string is
  een laadfout in de zin die de loader voor `spellings` schrijft); op een voorkomen geweigerd. Geen controle in
  `checks.ml`.
- Ref: `text_of` schrijft per factor het symbool van zijn declaratie, anders de afkorting. Suite-rijen
  (`WV-1.2c-text-*`) voor symbool, zonder symbool, `%`, samengesteld gemengd.
- Clingo: alleen de AST-patronen volgen; het tekstdeel blijft CLINGO-042.
- Drools: de loader leest `symbol` op een declaratie en weigert het op een voorkomen (zoals `spellings`,
  `IrLoader` rond r. 1181–1218, met rijen in `TypeSystemRefusalWitnessTest`); de analyse (`TmsTranspiler.textPart`)
  geeft `rsTekst` de spelling met symbolen als constante. `Unit.rendering()` blijft voor meldingen.

### Corpus

- `jutterswet-stelselweging`: het kilogram van Massa wordt via zijn naamwoord bereikt in plaats van via de tweede
  afkorting `kgm`, bijvoorbeeld `het massakilogram (mv: massakilogrammen) kg` en `met eenheid massakilogram`. Wat de
  golden toetst blijft: twee records met canonieke naam `kg` in twee stelsels, factor 1, dezelfde verwachtingen. NOTE.md
  mee.
- `jutterswet-tekst-van-een-waarde`: `(EUR)` → `(€)` in de verwachtingen; een deel over een eenheid zonder symbool
  (bijvoorbeeld een tweede stelsel met `kg`) en een samengestelde eenheid met gemengde schrijfwijze erbij, zodat beide
  regels in ref en beide Drools-takken spelen. DRL-pin opnieuw (`DrlTextPinWitnessTest`, zie hieronder).
- Migratie van de rest: het symbool verhuist van `spellings` naar `symbol`. Gemeten (2026-11-07, `ir.json` met `€` of
  `kgm` in een eenheidsstelsel): 28 goldens, 34 negatieven, 1 red-case, 30 bestanden in `toka/`, 15 ref-fixtures,
  2 bestanden onder `backends/drools/src/test/resources`. Waar een `source.rgs` bij staat, schrijft de frontend het
  opnieuw (gates 10, 11 en 16 houden dat byte-gelijk); de rest met een script `spec/proposals/v1-migratie/m8.py`
  (precedent m1–m7), dat telt wat het verplaatst en faalt bij iets onverwachts. `toka/README.md` krijgt de landing
  onder zijn bewuste afwijkingen, met de tellingen, zoals de eerdere landingen.
- Negatieven waarvan een pointer of `expect.txt` naar een spellingsindex wijst: nalopen (gate 3).

## Acceptance

- Rood eerst: de nieuwe verwachtingen van `jutterswet-tekst-van-een-waarde` op de code van main `a5b857a47` falen in ref
  en beide Drools-takken (log).
- `make gates` in de worktree: alle zeventien gates en `after-run` rc=0 (logs onder
  `uchar-dev/werk/unit-symbol/logs/`). Gate 9 (catala) draait mee waar catala staat.
- Geen vloer daalt; waar een telling bewust daalt (spellingen die symbool worden), staat dat met reden in het Result.
- Frontendtests voor: symbool gelezen en geschreven, derde token geweigerd, gedeeld symbool gedeclareerd, verwijzing met
  een gedeeld symbool geweigerd (ook in een samengestelde eenheid), `met eenheid USD` bij een gedeeld symbool werkt,
  symbool gelijk aan een afkorting geweigerd.

May touch: `spec/IR-SPEC.md` (ch. 2.1, 2.5), `spec/RUNTIME-SEMANTICS.md` (ch. 1.2), `spec/schema/uchar-ir-v1.schema.json`
(`DeclaredUnit`), `spec/verifier/`, `spec/proposals/v1-migratie/m8.py`, `scripts/check-ir-schema.sh` (vorm-zelftestrijen
voor `symbol`), `frontends/regelspraak/`, `ref/`, `backends/clingo/`, `backends/drools/`, `corpus/golden/`,
`corpus/negative/`, `corpus/red/`, `toka/` (de `ir.json`-bestanden en `toka/README.md`; de fragmenten
`toka/casus/toka-*.rgs` blijven letterlijk), deze kaart.

Out of scope: `spec/CONSTRUCTS.md`, `spec/PLAN.md` en `CLAUDE.md` (voorstellen in het Result; de hoofdsessie landt ze);
de distributie `regelspraak-toolchain` (pin-bump na de landing); andere frontends (Catala, F-logic schrijven geen
symbool); het symbool in meldingen, trace, dumps of case-uitvoer.
