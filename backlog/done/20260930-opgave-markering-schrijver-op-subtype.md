# opgave-markering-schrijver-op-subtype: de opgave-markering weigert een schrijver op een subtype

*Vondst van de regelsetbouwers, 2026-09-28 (skelet: de handverlaging van een
datum die het subtype schrijft), gemeten door de hoofdsessie op uchar
`de809cf7` via de distributie (`bin/regelspraak check|scenario`); reproductie
op Jutterswet-materiaal in `/tmp/v2/probe/subtype-schrijver/` (a-supertype.rgs,
b-subtype.rgs, c-beide.rgs, d-eigen-attribuut.rgs, e-ander-lid.rgs,
scenario-b.rgs; alleen lezen). Luid (ERROR), geen fout antwoord. Op akkoord
van de eigenaar geschreven (2026-09-30).*

## Aanleiding

De opgave-markering `(mag opgegeven worden)` (DIVERGENCES §7) lowert op het
declarerende objecttype T één defaultsgroep "opgave boven berekening X" met
de opgave bovenaan en de verplaatste schrijvers eronder. Zodra een regel X
schrijft voor een subtype S van T, weigert de resolver de markering:
"de gegenereerde defaults-groep op Vondst zou met de schrijvers van
Strandvondst samengaan tot één stapel per type (RUNTIME-SEMANTICS ch. 7.10),
en die samenstelling bouwt de markering niet (dialect §7)". §7 noemt het als
grens: "no composition with writers elsewhere in the inheritance line".

Dat is in een regelset een gewone vorm: een datum staat op het supertype,
omdat de bron hem daar declareert en elk subtype hem heeft, en de regel die
hem afleidt staat op het subtype, omdat alleen dat subtype de operanden kent.
Zo'n regelset verlaagt de markering nu met de hand: een eigen attribuut
`opgegeven X`, een `Afwijking van` op de berekening die de opgave voor laat
gaan, en geen `berekende X`, geen `afwijkende opgave X` en geen controle.
De betekenis is dezelfde als die van de markering, maar het dialect zegt het
niet en de lint ziet een afwijkende naam.

Wat ch. 7.10 vraagt, is duidelijk: per (concreet type, doel) één
effectieve stapel, waarin de opgave bovenaan blijft staan, ook boven de
eigen schrijvers van het subtype; de initialisatie en de virtuele
invoerdefault worden nooit vervangen. De kaart bouwt de samenstelling die
de markering nu weigert.

## Minimale reproductie

`de809cf7`, distributie `bin/regelspraak check|scenario`.

    Objecttype de Vondst (mv: Vondsten)
        de vinddatum Datum in dagen;
        de meldingsdatum Datum in dagen (mag opgegeven worden);
    Objecttype de Strandvondst is een Vondst (mv: Strandvondsten)
        de aanspoeldatum Datum in dagen;

    Regel melding op aanspoeldag
        geldig altijd
            De meldingsdatum van een Strandvondst moet gesteld worden op zijn aanspoeldatum
            indien zijn aanspoeldatum gevuld is.

    Scenario: Strandvondst S1 (aanspoeldatum 01-03-2026, opgegeven meldingsdatum 05-03-2026) en
    S2 (aanspoeldatum 02-03-2026); verwacht: meldingsdatum S1 05-03-2026, S2 02-03-2026.

    R1  b-subtype.rgs, zoals hierboven.
        nu: check ERROR "attribuut 'meldingsdatum' van Vondst: '(mag opgegeven worden)', maar regel
        'melding op aanspoeldag' schrijft 'meldingsdatum' voor Strandvondst, een subtype van Vondst —
        de gegenereerde defaults-groep op Vondst zou met de schrijvers van Strandvondst samengaan
        tot één stapel per type (RUNTIME-SEMANTICS ch. 7.10), en die samenstelling bouwt de
        markering niet (dialect §7)"; geen IR, dus geen meting in de uitvoerders.
    R2  c-beide.rgs: dezelfde regel op de Vondst ("op zijn vinddatum") én de regel op de
        Strandvondst. nu: dezelfde ERROR.
    R3  a-supertype.rgs: alleen de regel op de Vondst. nu: exit 0, resolved=2 (de groep gelowerd).
    R4  d-eigen-attribuut.rgs: de markering en de regel beide op de Strandvondst. nu: exit 0;
        scenario PASS "opgave gaat voor op het subtype".
    R5  e-ander-lid.rgs: de markering op de Vondst met een schrijver op de Vondst, en een regel op
        de Strandvondst die een ander lid schrijft. nu: exit 0.

## Acceptatie

- (a) Meet de vormruimte in ref, clingo en de Drools-exec-tak (de
  DRL-teksttak volgens haar kanaal): de schrijver op het subtype als
  gelijkstelling, als berekening en als `Uitzondering op`/`Afwijking van`-boom;
  schrijvers op het supertype en het subtype tegelijk (met en zonder een
  `Afwijking van` tussen beide); twee niveaus (kleinkind); met en zonder
  initialisatie op X; een regel die `afwijkende opgave X` en `berekende X`
  leest vanaf het subtype; de opgave gevuld, leeg, en gelijk aan de
  berekening (de controle van piece 5).
- (b) De resolver lowert de markering ook waar een schrijver van X op een
  subtype staat, zó dat per concreet type de opgave bovenaan de effectieve
  stapel staat (ch. 7.10, blokvolgorde 1–4) en `berekende X` de berekende
  waarde van dat type draagt; waar de samenstelling niet in het IR past,
  weigert de resolver benoemd, met de vorm en de reden. Geen wijziging aan
  `spec/` (7.10 is normatief); rood-eerst JUnit-getuige met deze reproductie.
- (c) Golden (werknaam `jutterswet-opgave-subtype`), handgerekend, als
  scenariodocument zoals `jutterswet-opgave-datum`, in ref ≡ clingo ≡
  Drools-exec; de pins van gate 10 (rgs-goldens), gate 12 (`EXPECTED_PLAYED`
  224, `EXPECTED_DRL_PLAYED` 190) en gate 16 (scenario-goldens) als
  **voorstel** waar de golden telt.
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). DIVERGENCES §7 "Scope / limitations":
  de grens "no composition with writers elsewhere in the inheritance line"
  vervangen door wat gebouwd is en wat nog weigert; "Fail-fast refusals"
  bijgewerkt.

May touch: `frontends/regelspraak/**` (resolver, tests, DIVERGENCES §7),
`corpus/golden/<nieuwe golden>/`, `scripts/check-*.sh` (pins, **voorstel**),
kaartverplaatsing. Niet: `ref/`, `backends/clingo/`, `backends/drools/`,
`spec/`. NB: serieel na de kaart drools-exec-kwantorvoorwaarde-eigen-operand;
werk op de kop van main na haar landing.

## Result (2026-09-30, bouwer)

Tak `card/opgave-markering-schrijver-op-subtype`, basis `6f8b9d1f`, kop vóór
deze Result-commit `8d65149a`. Werkboom schoon; niets gepusht, gemerged of
gerebased. De proeven staan buiten de repo: per vorm een map met `bron.rgs`,
`scenario.rgs` en de uitvoer van frontend, verifier, ref, clingo, Drools-exec
en DRL-teksttak.

### Wat er landde

- `RuleChecker.opgaveLowering`: een rekenende schrijver van X op een subtype
  van T, op elke diepte (een regel, een tabelrij, een defaults-groep uit een
  boom of een ongeordend blok), verhuist zoals een schrijver op T en houdt zijn
  type: hij schrijft `berekende X` voor de instanties van dat subtype. De
  gegenereerde leden, regel, groep en consistentie blijven op T. Een
  initialisatie op een subtype blijft op X. Nieuw:
  `subtypeInitialisatieFout` (twee initialisaties in één erflijn; een
  initialisatie met venster of versies op een subtype). Weg: de weigering "een
  schrijver van X op een ander type van de erflijn". Javadoc en
  sectiecommentaar bijgewerkt.
- `OpgaveMarkeringTest`: `een_schrijver_op_een_subtype_weigert` vervangen door
  tien getuigen: zes voor de lowering (subtype, beide niveaus, afwijkingsboom
  op het subtype, kleinkind, initialisatie op T, voorwaardelijke initialisatie
  op het subtype), drie weigeringen met hun zin letterlijk, en één die pint dat
  een schrijver op een supertype alleen de doelweigering (WF-022) krijgt. De
  klasse telt 42 toetsen.
- Golden `corpus/golden/jutterswet-opgave-subtype/` (bron.rgs, scenario.rgs,
  ir.json, twaalf cases, NOTE.md met de casustekst "Art. 3g (aanmelding van een
  strandvondst)", de stapels per concreet type en de handberekening),
  toegelaten via `scripts/admit.sh`.
- `scripts/check-drools.sh` (**voorstel**, beschermd pad, eigen commit): pin,
  redenblok, klasse (A) en de koptellingen.
- `frontends/regelspraak/grammar/DIVERGENCES.md` §7: tabelrijen 2 en 3; drie
  alinea's (schrijvers op een subtype, de gemeten vormen, een initialisatie op
  een subtype); de weigerlijst (de erflijnbullet vervangen door twee bullets);
  Scope; de kringbullet; de golden-alinea.

### (a) Metingen per vorm

Kleine modellen op Jutterswet-materiaal: Vondst (met `meldingsdatum (mag
opgegeven worden)`), Strandvondst is een Vondst, Barnsteenvondst is een
Strandvondst, zuster Wrakvondst. Eerst de handverlaagde vorm van R1; dan elke
vorm met een proefbouw van de lowering zonder de twee nieuwe weigeringen (niet
gecommit); daarna alles opnieuw met de gebouwde resolver, met dezelfde
uitkomst behalve de drie vormen die nu weigeren. Waar de samenstelling het punt
is, staat het document zonder markering ernaast (achtervoegsel u). "PASS (n)"
is per case de letterlijke laatste regel `oracle: PASS (n expectations)` van
ref en `clingo: PASS (n expectations)` van clingo; Drools-exec geeft één regel
over de hele proef. De DRL-teksttak geeft bij elke proef met markering
`EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults
lowering`; dat staat hieronder niet telkens.

| Vorm | Frontend | ref | clingo | Drools-exec |
|---|---|---|---|---|
| R1 kaart (b-subtype, scenario-b) | basis: `ERROR (line 3): … maar regel 'melding op aanspoeldag' schrijft 'meldingsdatum' voor Strandvondst, een subtype van Vondst — … die samenstelling bouwt de markering niet (dialect §7)`, exit 1; kop: exit 0, `ir: objecttypen=2 feittypen=0 resolvedRegels=2` | PASS (2) | PASS (2) | `PLAYED 1` |
| R2 kaart (c-beide) | basis: dezelfde ERROR; kop: exit 0, zie p05 | | | |
| R3, R4, R5 (a-supertype, d-eigen-attribuut, e-ander-lid) | exit 0 vóór en na, IR byte-gelijk (`cmp`) | R4 met scenario-b: PASS (2) | PASS (2) | `PLAYED 1` |
| h1: R1 met de hand verlaagd (vijf stukken zonder markering, de groep als `Uitzondering op`) | exit 0 | PASS (8) | PASS (8) | `PLAYED 1`; de IR van R1 via de lowering is op de bronteksten na gelijk (regel van het subtype, afwijkingsregel, defaults) |
| p01 gelijkstelling op S; opgave wijkt af, geen, gelijk, zonder berekening; Vondst met en zonder opgave; doel in de invoer | exit 0 | PASS (4), (7), (7), (4) | idem | `PLAYED 4` |
| p02 berekening op S (`plus 2 dagen`) | exit 0 | PASS (4), (7), (7), (4) | idem | `PLAYED 4` |
| p03 `Uitzondering op`-boom op S | exit 0 | PASS (4), (9) | idem | `PLAYED 2` |
| p04 `Afwijking van`-boom op S | exit 0 | PASS (4), (7) | idem | `PLAYED 2` |
| p05 vlakke voorwaardelijke schrijvers op T en S (R2) | exit 0 | case1 PASS (9); case2 (op één Strandvondst gelden beide) `EVALUATION FAULT: no fixpoint within 100 passes — divergence is a fault (ch. 5.7)`; case3 (idem met opgave) dezelfde regel | case1 PASS (9); case2 `MISMATCH: S3.meldingsdatum = [20516;20513], expected 20513`; case3 PASS (1) | `FAIL case2-beide-feiten-op-het-subtype.json: geen fixpunt (kringverwijzing?) \| case3-…: geen fixpunt (kringverwijzing?)` |
| p05u idem zonder markering | exit 0 | case1 PASS (4); case2 dezelfde FAULT | case2 `MISMATCH: S3.meldingsdatum = [20513;20516], expected 20513` | `FAIL case2-…: geen fixpunt (kringverwijzing?)`; DRL-teksttak `PLAYED 2` |
| p06a `Afwijking van` op de regel van T met lichaam op S, met en zonder markering | ERROR (§4): `Afwijking van 'melding op vinddag': afwijking 'melding op aanspoeldag' schrijft een ander doel (Strandvondst.meldingsdatum (attribuut)) dan de basisregel (Vondst.meldingsdatum (attribuut)) — …` | | | |
| p06b vlakke regel op T naast een `Afwijking van`-boom op S | exit 0 | PASS (6), (7) | idem | `PLAYED 2` |
| p06bu idem zonder markering | exit 0 | PASS (6) | idem | `PLAYED 1` (een Strandvondst met alleen een vinddatum blijft leeg in beide: de eigen basis van S vervangt de geërfde regel) |
| p07 schrijver alleen op het kleinkind | exit 0 | PASS (4), (7) | idem | `PLAYED 2` |
| p08 boom op S, eigen basis op het kleinkind | exit 0 | PASS (6), (4) | idem | `PLAYED 2` |
| p08u idem zonder markering | exit 0 | PASS (6) | idem | `PLAYED 1` |
| p09 initialisatie op T, schrijver op S | exit 0 | PASS (8) | idem | `PLAYED 1` |
| p10a initialisatie op S (blijft op X) | exit 0 | PASS (6) | idem | `PLAYED 1` |
| p10au idem zonder markering | exit 0 | PASS (5) | idem | `PLAYED 1` |
| p10c initialisatie op S "indien hij beschadigd is" | exit 0 | PASS (4) | idem | `PLAYED 1` |
| p10cu idem zonder markering | exit 0 | PASS (4) | idem | `PLAYED 1` |
| p10f initialisaties op twee zusters | exit 0 | PASS (5) | idem | `PLAYED 1` |
| p10b initialisaties op T en S, proefbouw | exit 0, `verifier: PASS` | `MISMATCH: S2.meldingsdatum = 2026-03-01, expected 2026-03-15` | `MISMATCH: S2.meldingsdatum = [20513], expected 20527` | `FAIL case1-twee-initialisaties.json: attribuut meldingsdatum: verwacht 2026-03-15, werkelijk 2026-03-01` |
| p10bu idem zonder markering | exit 0; verifier `WF-079 FAIL /rules/rules/2/versions/0: two initialisations write target (Strandvondst, meldingsdatum) through its type chain — one declared on Vondst, one on Strandvondst — …` | | | |
| p10g initialisaties op S en kleinkind, proefbouw | exit 0 | `MISMATCH: B1.meldingsdatum = 2026-03-01, expected 2026-03-15` | `MISMATCH: B1.meldingsdatum = [20513], expected 20527` | `FAIL …: attribuut meldingsdatum: verwacht 2026-03-15, werkelijk 2026-03-01` |
| p10gu idem zonder markering | verifier `WF-079 FAIL /rules/rules/2/versions/0: two initialisations write target (Barnsteenvondst, meldingsdatum) through its type chain — one declared on Strandvondst, one on Barnsteenvondst — …` | | | |
| p10e initialisatie op S `geldig vanaf 01-01-2026`, proefbouw | exit 0, `verifier: PASS` | `EVALUATION FAULT: target A:meldingsdatum on 'Strandvondst' is written by a rule with a VALIDITY WINDOW beside an inherited defaults stack: …` | `TRANSLATION FAULT:` met dezelfde zin | `EXCLUDE transpiler SKIP: defaults: effectieve stapels — defaults: target A:meldingsdatum op 'Strandvondst' is written by a rule with a VALIDITY WINDOW beside an inherited defaults stack: …` |
| p10eu idem zonder markering | exit 0 | PASS (1) | PASS (1) | `PLAYED 1`; DRL-teksttak `PLAYED 1` |
| p10b, p10g, p10e met de kop | ERROR, exit 1, de twee nieuwe zinnen (zie (b)) | | | |
| p11 regels op S die `afwijkende opgave X` en `berekende X` lezen | exit 0 | PASS (5), (5) | idem | `PLAYED 2` |
| p12 zusters, elk een eigen berekening | exit 0 | PASS (7), (4) | idem | `PLAYED 2` |
| p13 twee onvoorwaardelijke schrijvers op T en S | exit 0; verifier `WF-079 FAIL /rules/rules/1/versions/0: two unconditional computing rules write target (Strandvondst, berekende meldingsdatum) through its type chain — …` | | | |
| p13u idem zonder markering | verifier dezelfde WF-079 op `(Strandvondst, meldingsdatum)` | | | |
| p14 `Uitzonderingen ongeordend` op S | exit 0 | PASS (6) | idem | `PLAYED 1` |
| p14u idem zonder markering | exit 0 | PASS (4) | idem | `PLAYED 1` |
| p15 beslistabel op S | exit 0 | PASS (6) | idem | `PLAYED 1` |
| p16 getal (Numeriek) met schrijver op S, initialisatie op T | exit 0 | PASS (7), (4) | idem | `PLAYED 2` |
| x1 regel op S die de `berekende X` leest die hij schrijft | `WARNING (line 0): kringverwijzing tussen berekeningen: Strandvondst.berekende meldingsdatum -> Strandvondst.berekende meldingsdatum — …`; verifier WF-045 | | | |
| x2 regel op S "indien hij afwijkende opgave meldingsdatum heeft" | geen WARNING; verifier `WF-045 FAIL …` en `WF-046 FAIL …` (dezelfde regel op de Vondst: `WARNING … Vondst.berekende meldingsdatum -> Vondst.afwijkende opgave meldingsdatum -> …`) | | | |
| x3 schrijver op een supertype, X op het subtype gemarkeerd | basis: twee ERRORs (WF-022 en de erflijnzin); kop: één, `ERROR (line 9): regeldoel 'De meldingsdatum van een Vondst': 'meldingsdatum' is geen gedeclareerd attribuut van Vondst (WF-022, IR-SPEC ch. 3.1)` | | | |

Tegencase op p01 (S1 verwacht 01-03-2026): ref `MISMATCH: S1.meldingsdatum =
2026-03-05, expected 2026-03-01`, clingo `MISMATCH: S1.meldingsdatum = [20517],
expected 20513`, Drools `FAIL …: attribuut meldingsdatum: verwacht 2026-03-01,
werkelijk 2026-03-05`.

### (b) De gekozen lowering

Elke rekenende schrijver van X op T of op een subtype van T verhuist naar
`berekende X` en houdt zijn type; de ene gegenereerde groep blijft op T. Een
initialisatie op T gaat de groep in zoals voorheen; een op een subtype blijft
op X.

Reden. (1) RUNTIME-SEMANTICS ch. 7.10: een subtype zonder eigen declaratie op
X erft de stapel van T heel. Na de verhuizing schrijft geen subtype X, dus is
voor elk concreet type de opgave de hoogste default van de stapel van X (blok
2), de berekeningsdefault de basis (blok 3), en de initialisatie en de
virtuele invoerdefault staan eronder (blok 4). (2) `berekende X` draagt per
concreet type de samenstelling die X zonder markering had, omdat de schrijvers
op dezelfde niveaus in dezelfde vorm staan; gemeten op p06b/p06bu, p08/p08u,
p14/p14u, p10a/p10au, p10c/p10cu en de tweeling van de golden. De markering
blijft suiker: ze zet alleen de opgave boven de berekening. (3) Geen wire-,
spec-, verifier- of uitvoerderwijziging; de IR van R1 is op de bronteksten na
gelijk aan de handverlaagde vorm h1.

Verworpen:

1. De schrijvers van het subtype op X laten staan en de vouw van ch. 7.10 laten
   werken (de gewone regel van S wordt de basis van S onder de geërfde opgave).
   `berekende X` zou voor een instantie van S leeg blijven of de berekening van
   T dragen, zodat kenmerk en consistentie niet kloppen; en een eigen
   uitzondering van S kwam boven de geërfde opgave (blok 1 boven blok 2), waar
   de kaart de opgave bovenaan vraagt.
2. Een gegenereerde groep per subtype: vormnamen zijn uniek in het model
   (WF-020), en de erfenis van de groep op T geeft hetzelfde zonder extra wire.
3. Bij vlakke schrijvers op T en S een groep op `berekende X` genereren zodat de
   regel van S die van T vervangt. Dat verandert de berekening tegenover het
   document zonder markering (p05u: een Strandvondst met alleen een vinddatum
   krijgt daar 04-03-2026 van de regel van T) en kiest tussen twee schrijvers
   die de auteur niet rangschikte (ch. 7.7).
4. Een initialisatie op een subtype in de gegenereerde groep opnemen: de groep
   staat op T en haar initialisatiedefault geldt voor elk type, en een tweede
   groep botst op de naam (zie 2). Haar weigeren is niet nodig: de vouw van ch.
   7.10 neemt haar met haar voorwaarde op (p10a, p10c, p10f spelen in de drie,
   gelijk aan zonder markering).
5. De supertype-richting van de oude erflijnweigering houden: een supertype
   heeft X niet (WF-105), dus de regel weigert al op zijn doel (WF-022); de
   tweede zin sprak over een samenstelling die er niet is (x3).

Benoemde weigeringen, waar de samenstelling niet in het IR past:

- Twee initialisaties in één erflijn, waarvan minstens één op een subtype
  (p10b, p10g): de stapel van het lagere type zou twee initialisatiedefaults
  dragen. Met de weigering eruit gaven de drie uitvoerders zonder melding de
  hogere; zonder markering weigert de verifier het paar onder WF-079, waarvan de
  initialisatie-arm alleen een doel zonder defaults leest. Zin: `attribuut
  'meldingsdatum' van Vondst: '(mag opgegeven worden)', en 'meldingsdatum' heeft
  twee initialisaties in één erflijn ('melding bij onbekende dag' voor Vondst en
  'melding bij onbekende aanspoeldag' voor Strandvondst) — de effectieve stapel
  van Strandvondst zou twee initialisatiedefaults dragen, en RUNTIME-SEMANTICS
  ch. 7.10 kent er één; zonder de markering weigert WF-079 dit paar (dialect
  §7)`.
- Een initialisatie op een subtype met een ander venster dan altijd of met meer
  dan één versie (p10e): de vouw neemt geen regel met venster op; met de
  weigering eruit weigerden de drie uitvoerders, zonder markering speelt het.
  Zin: `attribuut 'meldingsdatum' van Vondst: '(mag opgegeven worden)', en de
  initialisatie 'melding bij onbekende aanspoeldag' van 'meldingsdatum' voor
  Strandvondst heeft een ander geldigheidsvenster dan altijd — naast de
  gegenereerde defaults-groep op Vondst wordt zij een default in de effectieve
  stapel van Strandvondst (RUNTIME-SEMANTICS ch. 7.10), en die neemt geen regel
  met een venster op (dialect §7)`.

Niet geweigerd, wel gemeten: vlakke schrijvers op T en S die op één instantie
beide gelden (p05) falen luid in de uitvoerders, met en zonder markering; dat
is de agendavolgorde van het document zelf (ch. 7.7), niet iets wat de
markering toevoegt, en het frontend meldt het ook zonder markering niet.

Rood-eerst: op `aa136980` (toetsen erbij, resolver de basis) `[ 1008 tests
found ]`, `[ 998 tests successful ]`, `[ 10 tests failed ]`: de tien nieuwe
getuigen; zes op de oude ERROR, één op twee ERRORs waar één verwacht werd,
drie op de oude zin waar de nieuwe verwacht werd. Groen op
`ca0dd3ab`: `[ 1008 tests successful ]`, `[ 0 tests failed ]`.

### (c) Golden `jutterswet-opgave-subtype`

`admit: ADMITTED corpus/golden/jutterswet-opgave-subtype (12 case(s);
verifier-clean, ref = clingo, no-op refuted, gates green)`, gedraaid met het
voorstel voor gate 12 in de werkboom. De IR uit `bron.rgs` alleen is
byte-gelijk aan die uit `bron.rgs` met `scenario.rgs`.

Stapels van `berekende meldingsdatum`: Vondst geen schrijver; Strandvondst lid
2 (aanspoeldag indien beschadigd) boven lid 1 (aanspoeldag plus 2 dagen);
Barnsteenvondst lid 2 (geërfd) boven lid 3 (keurdag), dat lid 1 vervangt.
`meldingsdatum` bij elk type: opgave, berekende, initialisatie 01-03-2026,
invoer.

| Case | Invoer | Verwacht (meldingsdatum; berekende; kenmerk / ter controle / controledag; afhaaldatum; kanaal) | ref | clingo |
|---|---|---|---|---|
| case1-strandvondst-berekend | S, aanspoeldag 10-03 | 12-03; 12-03; afwezig / niet / leeg; 11-04; consistent | PASS (7) | PASS (7) |
| case2-strandvondst-beschadigd | S, aanspoeldag 10-03, beschadigd | 10-03; 10-03; afwezig; 09-04; consistent | PASS (5) | PASS (5) |
| case3-strandvondst-opgave-wijkt-af | S, aanspoeldag 10-03, opgegeven 15-03 | 15-03; 12-03; aanwezig / ter controle / 12-03; 14-04; `controle opgave meldingsdatum` × S | PASS (7) | PASS (7) |
| case4-strandvondst-opgave-gelijk | S, aanspoeldag 10-03, opgegeven 12-03 | 12-03; 12-03; afwezig / niet / leeg; 11-04; consistent | PASS (7) | PASS (7) |
| case5-barnsteenvondst-eigen-basis | B, aanspoeldag 10-03, keurdag 20-03 | 20-03; 20-03; afwezig; 19-04; consistent | PASS (5) | PASS (5) |
| case6-barnsteenvondst-geerfde-uitzondering | B, idem, beschadigd | 10-03; 10-03; afwezig; 09-04; consistent | PASS (5) | PASS (5) |
| case7-barnsteenvondst-zonder-keurdag | B, aanspoeldag 10-03 | 01-03 (initialisatie); leeg; afwezig; 31-03; consistent | PASS (5) | PASS (5) |
| case8-barnsteenvondst-opgave-wijkt-af | B, aanspoeldag 10-03, keurdag 20-03, opgegeven 18-03 | 18-03; 20-03; aanwezig / ter controle / 20-03; 17-04; `controle opgave meldingsdatum` × B | PASS (7) | PASS (7) |
| case9-vondst-zonder-berekening | V, niets | 01-03; leeg; afwezig; 31-03; consistent | PASS (5) | PASS (5) |
| case10-vondst-met-opgave | V, opgegeven 07-03 | 07-03; leeg; afwezig; 06-04; consistent | PASS (5) | PASS (5) |
| case11-doel-zelf-in-invoer | S, aanspoeldag 10-03, meldingsdatum 20-03 | 12-03 (Reading A); 12-03; afwezig; 11-04; consistent | PASS (5) | PASS (5) |
| case12-doel-zelf-zonder-feiten | S, meldingsdatum 20-03 | 20-03 (invoer blijft); leeg; afwezig; 19-04; consistent | PASS (5) | PASS (5) |

Alle data in 2026. De golden als geheel: Drools-exec `PLAYED 12`, DRL-teksttak
`EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults
lowering`. Onder `--noop` geven alle twaalf exit 1. Vijftien tegencases (buiten
de repo, elk één verwachting omgedraaid) falen in alle drie, bijvoorbeeld case6
met 20-03-2026 (de eigen basis boven de geërfde uitzondering): ref `MISMATCH:
B.meldingsdatum = 2026-03-10, expected 2026-03-20`, clingo `MISMATCH:
B.meldingsdatum = [20522], expected 20532`, Drools `FAIL …: attribuut
meldingsdatum: verwacht 2026-03-20, werkelijk 2026-03-10`. De tweeling zonder
markering (de twee lid-6-regels eruit, acht cases zonder opgave): PASS in ref
en clingo, `PLAYED 8`, dezelfde meldingsdatum en afhaaldatum.

Pinvoorstellen:

- Gate 12 (`scripts/check-drools.sh`, beschermd pad, `f62909da`):
  `EXPECTED_PLAYED=225` → `226` met redenblok `# 225 -> 226 (2026-09-30, card
  opgave-markering-schrijver-op-subtype): jutterswet-opgave-subtype, new, from
  bron.rgs and scenario.rgs and hand-computed — …` (de exectak speelt de twaalf
  cases zonder bronwijziging in de backend; de DRL-teksttak weigert op haar
  defaults-zin, `EXPECTED_DRL_PLAYED` blijft 191). DRL_CHANNEL klasse (A), in
  naamvolgorde tussen `jutterswet-opgave-datum` en `jutterswet-opgave-vormen`:
  `"jutterswet-opgave-subtype|EXCLUDE|ch. 7 prioritizedDefaults: no defaults
  lowering in this branch"`. Kop (A): `36 lines` → `37 lines`, `sitting 34
  below` → `sitting 35 below`, `35 of the 36 lines` → `36 of the 37 lines`,
  `the 36th, jutterswet-havenrooster` → `the 37th, …`, `All 35 ARE` → `All 36
  ARE`, `The 34 is 38 exec-only players — those 35,` → `The 35 is 39 exec-only
  players — those 36,`, en een hertelzin (A 37, B 37, C 10, D 2, E 3, F 3 = 92
  regels, tegen de lijst geteld; kloof 35 = 226 - 191). Kop DRL_CHANNEL:
  `eighty-six of the ninety-one` → `eighty-seven of the ninety-two`. Gemeten:
  `played 226`, `played matches the pinned 226`, `DRL-text branch played 191
  (… pin 191)`, `DRL-text floor 92 non-PLAYED goldens pinned by name`.
- Gate 10: `EXPECTED_MIN_GOLDENS=80` is een vloer; de golden telt mee (110
  rgs-goldens gemeten), er is geen pin te verzetten. Redenregel als de
  maintainer de vloer ophoogt: `110 (2026-09-30, +1 jutterswet-opgave-subtype)`.
- Gate 16: vloeren `EXPECTED_MIN_SCENARIO_GOLDENS=7` en
  `EXPECTED_MIN_SCENARIO_CASES=31`; gemeten 17 goldens en 69 casebestanden. Geen
  pin te verzetten; redenregel bij ophogen: `17 / 69 (2026-09-30, +1 golden en
  +12 cases van jutterswet-opgave-subtype)`.

### (d) Gates

Op `8d65149a`, na `eval $(opam env)` en `dune build` (exit 0), in de volgorde
van `.github/workflows/gates.yml`, elk met de interpreter van zijn shebang (sh
voor 1–8 en 13, bash voor 9–12 en 14–16). Alle zestien exit 0. Per gate de
letterlijke laatste regel; waar die geen oordeel is, ook de PASS-regel.

- 1 (sh) exit 0: `check-self-contained: PASS`
- 2 (sh) exit 0: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 322 file(s) validate and round-trip, floor 192; 42339 object(s) in 823 document(s) in the schema's key order, 1 in no subschema)`
- 4 (sh) exit 0: `check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 810 case file(s) validate)`
- 3 (sh) exit 0: `check-verifier: PASS (322 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)`
- 5 (sh) exit 0: `check-oracle: PASS (ref suite 1152 worked examples, floor 489; 770 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)`
- 6 (sh) exit 0: `check-clingo: PASS (backend suite 1988 checks, floor 83; 770 cases replay exactly through clingo — ref = clingo over the corpus)`
- 7 (sh) exit 0: `check-noop: PASS (726 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)`
- 8 (sh) exit 0: `check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)`
- 9 (bash) exit 0: `check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)`
- 10 (bash) exit 0: `check-regelspraak: PASS — JUnit suite green (1008 tests, floor 173); 110 rgs golden(s) re-emit byte-identically (floor 80)`
- 11 (bash) exit 0: laatste regel `Drools excludes by form:` (kop van een lege lijst); oordeel `check-toka-parity: PASS`, `ref : 29 PASS / 0 diverge`, `clingo : 29 PASS / 0 loud translation-fault / 0 silent diverge`, `Drools : 29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR`
- 12 (bash) exit 0: laatste regel `JUnit 906 tests, floor 239  (the backend's own suite, green with a non-zero count)`; oordeel `check-drools: PASS`, `played 226`, `played matches the pinned 226`, `DRL-text branch played 191  (its OWN count — the narrower branch, not the square; pin 191)`, `DRL-text floor 92 non-PLAYED goldens pinned by name`
- 13 (sh) exit 0: laatste regel `corpus-wide accounting: 88 unit-bearing annotations = 45 twistable + 43 exempt (the annotation IS the source) + 0 without a convertible sister + 0 at a §2.10 fourth-class branch (v0 derives no result unit) + 0 outside the twist table`; oordeel `check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)`
- 14 (bash) exit 0: `check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)`
- 15 (bash) exit 0: `check-graaf: PASS`, met `spec leg — 246 of 283 goldens compared, 159 dependencies equal to the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)` en `route leg — the graph of the wire equals the graph of the source on all 110 goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)`
- 16 (bash) exit 0: `check-scenario: PASS — 17 scenario golden(s) (floor 7) regenerate 69 case file(s) (floor 31) and their ir.json byte-identically`

Inertie: elke RegelSpraak-bron van de repo (125 in `corpus`: 109 `bron.rgs`
en 16 `scenario.rgs`; 47 in de pariteitszone) door het frontend van de basis
en van `ca0dd3ab`, IR plus uitvoer plus exitcode vergeleken: 172 van 172
identiek. De grammatica is niet geraakt. Bronnen met de markering in `corpus`:
nu vier (`jutterswet-opgave-boven-berekening`, `jutterswet-opgave-vormen`,
`jutterswet-opgave-datum`, `jutterswet-opgave-subtype`).

### Commits op de tak

| Hash | Kop |
|---|---|
| `d4ecb302` | kaart opgave-markering-schrijver-op-subtype geclaimd |
| `aa136980` | OpgaveMarkeringTest: een schrijver op een subtype, rood |
| `ca0dd3ab` | RuleChecker: de markering met een schrijver op een subtype |
| `cbcd1d39` | golden jutterswet-opgave-subtype: de markering met schrijvers op subtypes |
| `f62909da` | voorstel gate 12: jutterswet-opgave-subtype speelt in de exectak |
| `8d65149a` | DIVERGENCES §7: de markering met schrijvers op een subtype |

Daarna één commit met dit Result, die de kaart naar `backlog/done` verplaatst.

### Open punten

1. Het voorstel voor gate 12 (`f62909da`) staat op een beschermd pad; de
   hoofdsessie landt het samen met de golden (zonder het is gate 12 rood: 226
   spelers tegen pin 225).
2. De registertekst voor `spec/CONSTRUCTS.md` rij 8 hieronder: hoofdsessie. De
   huidige Landed-alinea van 2026-09-25 noemt "beside a writer of X elsewhere
   in the inheritance line" nog als weigering.
3. DIVERGENCES, Scenariotoetsen → Witnesses, zegt "nine goldens carry a
   `scenario.rgs`": vóór deze kaart waren het er zestien, nu zeventien (niet
   genoemd: `jutterswet-meldingsketen`, `jutterswet-premiestaffel`,
   `jutterswet-zinkende-jutter`, `jutterswet-halfstokspelling`,
   `jutterswet-sluitdag`, `jutterswet-premiespelling`, `jutterswet-wrakmelding`
   en `jutterswet-opgave-subtype`). Buiten §7; niet geraakt.
4. Een rekenende regel op een subtype die `afwijkende opgave X` leest, sluit een
   kring via de gegenereerde regel op T: de verifier weigert (WF-045, WF-046),
   het frontend waarschuwt niet, omdat zijn graaf knopen per geschreven type
   heeft (x2). In §7 vastgelegd onder de bestaande grens van de graaf; een
   waarschuwing vraagt knopen die de gegenereerde leden over de erflijn delen.
5. Vlakke voorwaardelijke schrijvers op T en een subtype die op één instantie
   beide gelden (p05): ref en de exectak melden geen fixpunt, clingo geeft twee
   waarden, met en zonder markering; de verifier laat het door (WF-079 kijkt
   naar onvoorwaardelijke paren) en het frontend meldt niets.
6. WF-079 over twee onvoorwaardelijke schrijvers op T en een subtype (p13) noemt
   met de markering het doel `berekende meldingsdatum`, een naam die de auteur
   niet schreef; hetzelfde gold al voor twee zulke schrijvers op T.
7. Een initialisatie met voorwaarde blijft op T geweigerd (de initialisatie-
   default van de groep draagt geen voorwaarde) en is op een subtype
   toegestaan (de vouw houdt haar voorwaarde): gemeten en in §7 vastgelegd; de
   kant van T is in deze kaart niet veranderd.
8. De distributie (`bin/regelspraak`) draait op haar eigen submodulepin en
   weigert R1 tot die pin deze kop bevat.

### Registertekst voor `spec/CONSTRUCTS.md` rij 8 (aanvulling op de Landed-alinea's; de hoofdsessie landt hem)

> **Landed (2026-09-30, card `opgave-markering-schrijver-op-subtype`):** the
> opgave marker stands on an attribute whose computing writers stand on
> subtypes of its declaring type T as well. Every computing writer of X on T or
> on a subtype of T, at any depth, moves to `berekende X` and keeps its type, so
> the writers of `berekende X` compose per concrete type by
> `RUNTIME-SEMANTICS.md` ch. 7.10 as those of X compose without the marker; the
> one generated group stays on T, no subtype writes X after the lowering, and
> every subtype inherits the group whole, so the opgave is the highest default
> of every type's stack. An initialisation on a subtype stays on X, where the
> ch. 7.10 fold makes it the initialisation default of that type's stack with
> its condition. The refusal "beside a writer of X elsewhere in the inheritance
> line" is replaced by two: two initialisations of X in one inheritance line
> (with the refusal left out, ref, clingo and the Drools exec branch gave the
> higher one without a diagnostic, where the verifier refuses the unmarked
> document under WF-079), and an initialisation on a subtype with a window or
> with versions (the fold takes no windowed rule; the three executors refused
> it). Witness `jutterswet-opgave-subtype` (an exception tree on the
> Strandvondst, an own base on the Barnsteenvondst below it, the initialisation
> on the Vondst, subtype rules reading the generated characteristic and
> `berekende X`; twelve cases from a scenario document), in ref ≡ clingo ≡
> Drools exec; the DRL-text branch refuses it on its defaults sentence. No
> wire, spec, verifier or executor change.

## Keuring (hoofdsessie, 2026-09-30)

Eigen run van de zestien gates op `2475ead1` in de volgorde van `gates.yml`
(bash waar de shebang dat zegt), `dune build` vooraf; logs in
`/tmp/v2/uchar-gates9/`. Alle zestien exit 0: gate 3 322 goldens; gate 5 en 6
770 cases (ref = clingo); gate 7 726 cases, 44 pinned; gate 10 JUnit 1008,
110 rgs-goldens; gate 12 `check-drools: PASS`, played 226 (pin 226), DRL 191
(pin 191), JUnit 906; gate 16 17 scenario-goldens, 69 casebestanden. De
lowering gelezen: een schrijver op een subtype verhuist op zijn eigen type
naar `berekende X`, de groep blijft op T en wordt heel geërfd (ch. 7.10);
twee benoemde weigeringen erbij, beide eerst zonder weigering gemeten. De
registertekst is in dit landingscommit gezet in `spec/CONSTRUCTS.md` rij 8.
Open punten 1–8 gelezen: geen kaart; 3 (de telling van de scenario-goldens
in DIVERGENCES) en 6 (WF-079 noemt `berekende X`) zijn tekstpunten voor een
volgende frontend-kaart; 5 (twee vlakke schrijvers op T en S, geen fixpunt
in ref en exec, twee waarden in clingo) staat los van de markering en is een
eigen vondst; 8 volgt nu (distributie- en werkbankpin). Landing: ff van
`card/opgave-markering-schrijver-op-subtype` op main.

