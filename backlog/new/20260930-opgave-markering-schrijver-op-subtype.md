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
