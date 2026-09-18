# tekstlexeem-canonicalisatie: één vorm voor het v0-tekstliteral

Uit de tekstlexeem-kaart (2026-09-18): v0 draagt twee geldige
lexeemfamilies voor dezelfde tekstwaarde — rauw (`Flevoland`) en met
eigen buitenste aanhalingstekens (`"Flevoland"`) — omdat `waarde` per
IR-SPEC ch. 4.1 de onontlede bronstring is. De splitsing is
emittergedrag, geen corpus-toeval: `ExpressieBouwer.java:381` schrijft
de bronsnippet inclusief buitenste aanhalingstekens, waar
handgeschreven goldens rauwe lexemen dragen. Sinds de tekstlexeem-kaart
vergelijken alle vier consumenten verbatim, dus beide families spelen —
maar twee documenten met dezelfde bedoeling zijn niet byte-gelijk, en
een rgs-afgeleide golden met tekstliteral speelt alleen als zijn
case-invoer de quotes óók draagt.

Maintainersbesluit 2026-09-18 (genummerde vraag): **canonicaliseren
bij emissie** — de emitter schrijft voortaan de geparste waarde, niet
de bronsnippet met buitenste aanhalingstekens. Uitvoering direct na
het lopende zes-kaarten-pariteitsprogramma; gemandateerde kaart, geen
optie meer.

## Acceptatie (bij oppakken aanscherpen)

- (a) `ExpressieBouwer` schrijft voor `Tekst`-literalen de geparste
  waarde (buitenste delimiters weg, ingebedde tekens verbatim); de
  bronsnippet blijft via de `bron`-annotatie beschikbaar.
- (b) De geraakte `ir.json`-bestanden regenereren met verantwoorde
  byte-diff. Meting kaart tekstlexeem-lexeem (2026-09-18): geen
  publieke rgs-golden draagt een tekstliteral; de quote-omsloten
  vormen zitten in de `toka/`-zone. Bijbehorende quote-dragende
  casewaarden bewegen mee — meten bij oppakken.
- (c) Aandachtspunt gate 11: canonicaliseren is een bewuste
  emissie-divergentie van de toolchain (zoals besluit 16Y bij
  toka-45); de gepinde `toka/**/ir.json` regenereren in dezelfde
  kaart en de divergentie documenteren in het gate-script-commentaar
  dan wel de holdout-verantwoording (beschermd-padvoorstel indien
  scripttekst wijzigt).
- (d) `spec/CONSTRUCTS.md`-regel (registervoorstel in het Result) en
  de IR-SPEC ch. 4.1-consequentie meenemen.
- (e) Alle veertien gates groen; de emitter is vendored slice, dus
  toolchain-herpin na de merge — te combineren met de
  programma-eindherpin.

Result: geland. `ExpressieBouwer` bouwt de `Tekst`-literal nu uit de
`STRING_TEXT`-chunks die de lexer al oplevert in plaats van uit
`bron.snippet()`; `bron` houdt de onontlede snippet. Eén plek in de
frontend maakt een `Datatype.Tekst()`-literal, en geen enkele andere plek
in de frontend leest de waarde van zo'n literal (alleen
`Literalen.tekstOfEnumLabel`, ongewijzigd), dus dit is de hele
emissiewijziging.

METING (machinaal, hele boom, vóór de wijziging; script ad hoc, niet
gecommit). Quote-omsloten `Tekst`-literalen: 48 stuks in 4 bestanden —
`toka/casus/ir.json` en de drie
`toka/cases/scenario-toka-46-woonregio-factor-{1,2,3}/ir.json`, elk 12,
en de drie slices dragen exact dezelfde twaalf waarden als de casus. De
meting van de vorige kaart klopt en is compleet: geen publieke
corpus-golden draagt er een — wél dragen 15 publieke bestanden
(`corpus/golden/jutterswet-*`, `corpus/negatief/wf*`) in totaal 27
`Tekst`-literalen, alle rauw. Quote-dragende case-tekstwaarden: 3, één
per `scenario-toka-46-woonregio-factor-*/case.json`, elk matchend tegen
een literal in zijn eigen slice. Na de wijziging: 0 en 0 in de hele boom.
Basismeting vóór de wijziging: gate 10 PASS (280 tests, 54 rgs-goldens),
gate 11 PASS (29 cases, ref 29 / clingo 29 / Drools 29 PLAYED).

DE ONTQUOTING-REGEL, aan de grammatica gemeten en in de javadoc gezet.
`tekstWaarde : STRING_START tekstReeksDeel* STRING_END` met
`STRING_TEXT : ~["«»\r\n]+` (`RegelSpraakLexer.g4`). De grammatica kent
GEEN escape-vorm: een aanhalingsteken sluit altijd de string. Daaruit
volgt dat (1) de delimiters aparte tokens zijn, dus de chunks nemen ÍS
het strippen ervan — totaal en verliesloos, het kan nooit een teken
opeten dat de auteur schreef; (2) er niets te ontsnappen valt, dus een
backslash blijft een gewoon teken; (3) een door deze emitter geschreven
waarde nooit zelf een `"` kan bevatten. Feitelijk gemeten aan de parser,
niet afgeleid: `"Flevoland"` → `Flevoland`; `""` → de lege string;
`"pad\naar"` → `pad\naar`; `"het 'merk'"` → `het 'merk'`;
`"5 % korting"` → `5 % korting`. De chunks nemen in plaats van
`snippet` minus eerste en laatste teken maakt bovendien één stille
normalisatie ongedaan: `DefinitionResolver.src` vouwt elke witruimte-run
tot één spatie, dus `"a  b"` werd als `"a b"` weggeschreven; nu is de
waarde `a  b` en toont alleen `bron` nog de vouw.

AFWIJKING VAN DE KAART, expliciet. De gevraagde getuige "`merk "AA"` komt
als `merk "AA"` op de wire" is als BRONLEXEEM niet te schrijven: bij
`"merk "AA""` sluit het tweede aanhalingsteken de string en de rest is
syntaxfout (feitelijk gemeten: `ERROR (line 6): syntaxfout: extraneous
input 'AA'`). De ingebedde-quotefamilie is per constructie
HANDGESCHREVEN — `corpus/golden/jutterswet-keurmerk` is precies zo'n
document — en blijft geldig: consumenten vergelijken verbatim, dat is de
vorige kaart en die is ongewijzigd groen. De getuige pint daarom het
grammaticafeit zelf, wat sterker is: hij legt vast WAAROM de ontquoting
totaal mag zijn, en wordt rood zodra de stringgrammatica ooit een escape
krijgt — precies het moment waarop de regel zou moeten wijzigen.

BYTE-DIFF, verantwoord. 51 gewijzigde regels in 7 bestanden, 102
diff-regels, en niets anders: 48 `"waarde"`-velden (12 in
`toka/casus/ir.json`, 12 in elk van de drie slices) en 3 `"text"`-velden
(één per slice-`case.json`). Elke `"bron"`-annotatie staat er nog met
haar aanhalingstekens; geen enkel ander veld, geen sleutelvolgorde, geen
inspringing bewoog. Machinaal gecontroleerd door alle `+`/`-`-regels van
`git diff -U0 -- '*.json'` te tellen en te groeperen: 24 unieke
regelvormen, alle van de vorm `"waarde": …` of `"text": …`. De
casus-dump is her-geëmitteerd (`Resolver --ir` over de 45
niet-uitgehouden fragmenten, precies gate 11's aanroep); de slices dragen
geen `.rgs` en zijn met de hand meebewogen, waarde voor waarde tegen de
geparste JSON gecontroleerd. De 3 casewaarden moesten mee: literal en
invoer worden verbatim vergeleken, dus ze bewegen samen of de replay
breekt.

GETUIGEN. Drie nieuwe in
`frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/IrEmitterTest.java`,
naast de bestaande rij kale lexemen (WF-081 getal, WF-084 datum):
`tekst_literal_lexeme_is_de_geparste_waarde_snippet_in_bron` (het
gevraagde `"Flevoland"` → `Flevoland`, met de snippet in `bron`),
`tekst_literal_grammatica_kent_geen_escape_dus_nooit_een_ingebed_aanhalingsteken`
en `tekst_literal_normaliseert_verder_niets` (dubbele spatie, backslash,
enkele aanhalingstekens, lege string). Eén hulpje erbij, `emitRaw`, want
de bestaande `emitCompact` wist juist de witruimte die één getuige moet
zien. De dual-branchgetuigen van de vorige kaart zijn niet aangeraakt en
blijven groen: zij bouwen hun wire in geheugen, en de quote-dragende
familie blijft een geldig handgeschreven document.

MUTATIEPROEF, feitelijk uitgevoerd en hersteld. De ontquoting terug naar
`bron.snippet()`, hercompileren: frontend-suite 281 groen / 2 rood —
`tekst_literal_lexeme_is_de_geparste_waarde_snippet_in_bron` en
`tekst_literal_normaliseert_verder_niets`. De derde blijft groen, zoals
hoort: die pint de grammatica, niet de emitter. Gate 10 rc=1 (de suite).
Gate 11 rc=1 met `DIFF: casus (re-emitted IR diverges from the pinned
toolchain dump)` — de geregenereerde pin matcht niet meer. Daarna
`git checkout --` en hercompileren; werkboom schoon.

GATE 11, DE DIVERGENTIE. Gate 11 vergelijkt tegen de eigen gepinde
bestanden, dus hij is na regeneratie groen — maar voor tekstliteralen is
de dump daarmee geen toolchain-dump meer. Dat staat nu in
`toka/README.md` onder de nieuwe kop "Deliberate divergences from the
toolchain emission", met de precedentregel ernaast: `toka-45` (16Y)
divergeert de andere kant op, via een holdout in plaats van een herpin.
De bullet over `casus/ir.json` zegt nu "minus the recorded divergences"
en "any *unrecorded* emitter divergence", en er is een bullet bij over de
slices: zij dragen geen `.rgs`, gate 11 her-emitteert ze niet, dus zij
volgen de emitter niet vanzelf en moeten in dezelfde kaart met de hand
mee — anders draagt de zone twee spellingen van één constructie en kan
geen enkele gate dat zien. Geen enkele pin in
`scripts/check-toka-parity.sh` brak: `EXPECTED_MIN_FRAGMENTS=45`,
`EXPECTED_MIN_TESTS=27`, `EXPECTED_MIN_CASES=29` en beide holdouts staan
ongewijzigd en worden gehaald. Het script is NIET gewijzigd (beschermd
pad); hieronder het exacte voorstel.

VOORSTEL `scripts/check-toka-parity.sh` (beschermd pad, niet gewijzigd).
Vervang in de kopregels verplichting 2:

    #   2. every pinned parity dump (`toka/**/ir.json`, emitted once with the
    #      toolchain at the pin) re-emits through uchar's OWN frontend
    #      byte-identically — the feature-parity proof (frontend == toolchain on
    #      the ©-Belastingdienst casus the public corpus may never carry);

door:

    #   2. every pinned parity dump (`toka/**/ir.json`) re-emits through uchar's
    #      OWN frontend byte-identically — the feature-parity proof (frontend ==
    #      toolchain on the ©-Belastingdienst casus the public corpus may never
    #      carry), MINUS the recorded divergences. The dump was emitted once with
    #      the toolchain at the pin; where uchar's emitter has since changed ON
    #      PURPOSE the pin was re-taken in that card and listed in toka/README.md
    #      ("Deliberate divergences from the toolchain emission"), and for those
    #      constructs this leg proves "frontend == the recorded state", not
    #      toolchain agreement. Today that list holds one entry: a Tekst literal's
    #      waarde is the PARSED string, not the source snippet with its own outer
    #      '"' (maintainer decision 2026-09-18). Divergence by holdout (toka-45,
    #      below) and divergence by re-pin are both recorded; a silent one is not
    #      available;

en vul de commentaarregel boven leg 3 (`# 3. re-emit every pinned casus
dump …`) aan met één zin achter "…are the frontend==toolchain parity
proof.":

    #    A slice therefore does NOT follow the emitter: when an emitter change
    #    moves a construct, the slices move by hand in the same card
    #    (toka/README.md), or the zone carries two spellings of one construct and
    #    this leg cannot see it.

CHECKS. `bash scripts/check-regelspraak.sh`: PASS — 283 tests (vloer
173), 54 rgs-goldens byte-identiek (vloer 38). Geen publieke golden
bewoog: hun bronnen dragen geen tekstliteral, en de gate zou een
bytewijziging hard melden. `bash scripts/check-toka-parity.sh`: PASS — 1
casus-dump, 45 fragmenten byte-identiek, 27 TOKA-tests, 29 slices
schema-valide, 29 replays ref 29 PASS / clingo 29 PASS / Drools 29
PLAYED, nul divergenties. `mvn -o -f backends/drools/pom.xml package`
mét tests: BUILD SUCCESS, `Tests run: 437, Failures: 0, Errors: 0,
Skipped: 0`.

ALLE VEERTIEN GATES, los uitgevoerd, exitcode per gate:

    self-contained: 0        rood: 0
    ir-schema: 0             catala: 0
    verifier: 0              regelspraak: 0
    case-schema: 0           toka-parity: 0
    oracle: 0                drools: 0
    clingo: 0                reftype-derivation: 0
    noop: 0                  flogic: 0

TELLINGEN ONGEWIJZIGD. Gate 12: `played 150`, `channel floor 49
non-PLAYED goldens pinned by name; played matches the pinned 150`,
`value-divergence 1` — `EXPECTED_PLAYED` is een gelijkheidspin, dus een
groene gate 12 bewijst dat het kanaal niet bewoog. Gate 13: 108 twists
over 22 goldens (vloer 74), 100 replays waarvan 21 door de DRL-tekst-tak
(vloer 13), 0 survivors. Geen corpusbestand en geen backendcode is
aangeraakt, en gespeelde documenten spelen met dezelfde waarden omdat
literal en case-invoer samen bewogen. Geen script, geen spec-bestand,
geen publiek corpusbestand gewijzigd; `git diff --name-status
main...HEAD` geeft precies de kaartverplaatsing, twee frontend-bestanden
en acht `toka/`-bestanden.

BEWUST NIET AANGERAAKT, met reden. Twee javadoc-plekken herhalen de
IR-SPEC ch. 4.1-zin die met dit besluit verandert, en zij horen bij de
seriële spec-landing, niet bij deze kaart (beide buiten May touch):
`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/kern/Literalen.java`
(javadoc van `tekstOfEnumLabel`: "IR-SPEC ch. 4.1 puts the UNPARSED
source string in that field") en
`backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/TekstLexeemWitnessTest.java`
(klassedoc, dezelfde zin). Beide ASSERTIES blijven waar en groen — zij
gaan over de lezer, die niets strips — alleen de spec-motivering
verschuift. `BeslistabelDesugar.java` is gecontroleerd en blijft wél
juist: dat commentaar gaat over de CELSYNTAX in `.rgs`, niet over de
wire.

REGISTERVOORSTEL 1 — `spec/CONSTRUCTS.md`, rij 27 (`Datatype
refinements`), aan te hangen in de `v0→v1 delta`-kolom:

> **Tekstlexeem gecanonicaliseerd (2026-09-18, maintainersbesluit; kaart
> `20260918-tekstlexeem-canonicalisatie`):** het v0-`Tekst`-lexeem is de
> GEPARSTE string, niet de bronsnippet. De emitter schreef de snippet
> MÉT zijn eigen buitenste `"`-delimiters, zodat één tekstwaarde twee
> geldige v0-spellingen had; omdat alle vier consumenten `Tekst`
> byte-voor-byte vergelijken waren twee documenten met dezelfde
> bedoeling ongelijk, en speelde een geëmitteerde golden alleen als zijn
> case-invoer de delimiters óók droeg. Anders dan WF-081/084/095 is dit
> GEEN wire-hek: een `Tekst`-waarde is willekeurige tekst, dus geen
> verifier kan een spelling afwijzen — het is een PRODUCENTENregel,
> machinaal geborgd door de emittergetuige
> (`IrEmitterTest.tekst_literal_lexeme_is_de_geparste_waarde_snippet_in_bron`,
> gate 10) en door gate 11's byte-identieke her-emissie. De regel is aan
> de grammatica gemeten: de RegelSpraak-string kent geen escape-vorm, dus
> het strippen is totaal en verliesloos en een geëmitteerde waarde bevat
> nooit zelf een `"`. Dit is een bewuste, geboekte divergentie van de
> toolchain-spiegel (mirror-then-diverge): de productietoolchain schrijft
> nog de snippetvorm, dus de gepinde `toka/**/ir.json` zijn in dezelfde
> kaart herzet en de divergentie staat in `toka/README.md` — de tweede
> geboekte toolchain-divergentie naast toka-45 (16Y) en de eerste via
> herpin in plaats van via holdout. De handgeschreven quote-dragende
> familie blijft WELGEVORMD en wordt door alle vier consumenten verbatim
> gelezen (kaart `20260918-drools-tekstliteral-lexeem`); geen bestaande
> wire is ongeldig geworden.

REGISTERVOORSTEL 2 — `spec/IR-SPEC.md` ch. 4.1. De `Literal`-rij zegt nu
"v0 carries the unparsed source string as the value"; dat is na WF-081,
WF-084 en WF-095 al niet meer waar voor getal, datum en datum-tijd, en na
deze kaart ook niet meer voor `Tekst` — er is geen literalsoort meer waar
het geldt. Voorgestelde nieuwe notes-cel van de `Literal`-rij:

> **Delta (row 27):** v0 carries a LEXEME as the value — a string the
> executors parse — and v1 carries a typed value. The lexeme is the
> PARSED form, never the raw source slice: WF-081 pins the bare number,
> WF-084 the ISO date, WF-095 the ISO datetime, and (maintainer decision
> 2026-09-18) a `Tekst` literal's value is the string BETWEEN the source
> delimiters. The unparsed source slice lives in `source` (`bron` on the
> v0 wire), which is where a reader that wants the author's spelling
> looks.

en, in dezelfde paragrafenrij als WF-081/084/095, een nieuwe alinea —
bewust ZONDER WF-nummer, want de eigenschap is niet uit de wire alleen
beslisbaar en een MUST zonder verifiercheck mag deze spec niet dragen:

> **The text-literal lexeme (maintainer decision 2026-09-18).** A
> `Literal` whose `type` is `Tekst` carries as its value the PARSED
> string: what stands between the source form's delimiters, with the
> outer `"…"` removed and every character between them kept verbatim —
> no unescaping, no whitespace folding, no case folding. The RegelSpraak
> string grammar admits no escape sequence (a `"` always closes the
> string), so the removal is total and lossless and an emitted value can
> never itself contain a `"`. Unlike WF-081/084/095 this is NOT a
> well-formedness rule and carries no WF number: a `Tekst` literal
> legitimately carries arbitrary text, so no wire value can be rejected
> on its shape — a hand-authored document whose value carries its own
> outer quotes stays well-formed, and every executor compares it VERBATIM
> (ch. 4.1's raw-string comparison). The rule binds the PRODUCER: an
> emitter that writes the delimiter-carrying snippet puts a second
> spelling of one text value on the wire, which is what this decision
> removed. Its machine-checkable artifacts are the emitter witness
> (`IrEmitterTest.tekst_literal_lexeme_is_de_geparste_waarde_snippet_in_bron`,
> gate 10) and gate 11's byte-identical re-emission of the pinned dumps.

Terzijde bij WF-081: de scope-zin daar ("`Tekst` literals (which
legitimately carry arbitrary text) are governed by their own forms, not
by this rule") blijft precies kloppen en heeft nu een alinea om naar te
verwijzen.

BUITEN SCOPE GEHOUDEN. Verruiming van de stringgrammatica met een
escape-vorm (dan pas kan een bronlexeem een ingebed aanhalingsteken
dragen); case-inputnormalisatie (een casewaarde komt verbatim binnen en
blijft dat — de drie die bewogen zijn met de hand meebewogen omdat hun
literal bewoog, niet door een normaliserende lezer);
`TekstReeks`-interpolatie (ongewijzigd: die tak bouwt zijn delen al uit
dezelfde chunks); het herschrijven van publieke corpusbestanden (geen
enkel corpusbestand is aangeraakt); en de toolchain-herpin, die per
kaartpunt (e) na de merge met de programma-eindherpin meegaat.
