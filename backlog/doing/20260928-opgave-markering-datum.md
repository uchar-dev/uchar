# opgave-markering-datum: `(mag opgegeven worden)` op een attribuut Datum in dagen

*Voorstel van de werkbank-hoofdsessie, 2026-09-28, alleen lezend op
`68eed2fb`; aangemaakt met akkoord van de maintainer, te bouwen door een
bouwagent en geland door de hoofdsessie. Reproducties en metingen op
Jutterswet-materiaal; eigen casustekst.*

## Aanleiding

De markering `(mag opgegeven worden)` (`DIVERGENCES.md` §7) staat alleen op
een Numeriek attribuut zonder eenheid. Op een Datum in dagen weigert de
resolver: "… een defaults-groep op een attribuut van een ander datatype of
met een eenheid is nog niet in alle drie de uitvoerders gemeten" (R1, R2).
Die weigering is een meetgrens, geen ontwerpkeuze: elk defaultsdoel van het
corpus was een getal zonder eenheid (§7, "Fail-fast refusals"), en §7
"Scope / limitations" zet een datum op de wachtlijst tot een golden per
familie in de drie uitvoerders speelt.

Een model dat datums afleidt (einddata, termijnen) en die datums ook door
de casus wil laten opgeven, schrijft vandaag het hardheidspatroon met de
hand (R3): een eigen attribuut `opgegeven X`, de berekening als regel en de
opgave als `Uitzondering op` erbovenop. Dat is precies wat de markering
genereert. De meting hieronder laat zien dat die handgeschreven vorm op een
datum in alle drie de uitvoerders speelt.

## Meting (2026-09-28, distributiepin op `68eed2fb`)

R3 resolvet zonder melding en emitteert één `PrioritizedDefaults` op
`meldingsdatum` (opgave boven berekening). Twee cases, handgerekend: zonder
opgave is `meldingsdatum` de `vinddatum` (10-03-2026); met opgave
12-03-2026 wint de opgave. Replay: `ref` PASS (2 verwachtingen) op beide,
clingo PASS op beide, Drools-exectak `PLAYED 2`. De drie uitvoerders spelen
dus een defaults-groep op een Datum in dagen; alleen de frontendweigering
staat de markering in de weg.

## Besluiten (voorstel; de maintainer beslist)

- K1: de markering is toegelaten op een attribuut van datatype `Datum in
  dagen`, naast Numeriek zonder eenheid. `Datum en tijd in millisecondes`,
  Tekst, Percentage, Boolean, een enumeratie en Numeriek met eenheid
  blijven geweigerd, elk tot een eigen golden.
- K2: de lowering (§7, de vijf stukken) verandert niet: `opgegeven X` en
  `berekende X` krijgen het datatype van X; de groep, de regel `afwijking
  opgave X` (`ongelijk aan` op twee datums) en de consistentie `controle
  opgave X` zijn zoals bij een getal. Een geschreven initialisatie van X
  met een datumliteral wordt de laagste default, zoals bij een getal.
- K3: de weigerzin noemt de twee toegelaten families: "'(mag opgegeven
  worden)' staat alleen op een Numeriek attribuut zonder eenheid of een
  Datum in dagen, en 'X' is Tekst — een defaults-groep op een attribuut
  van een ander datatype of met een eenheid is nog niet in alle drie de
  uitvoerders gemeten (dialect §7)". De overige weigeringen en de
  waarschuwing blijven letterlijk.
- K4: een golden `jutterswet-opgave-datum` met eigen casustekst in de NOTE
  ("Art. 3f (dag van aanmelding)"; de nummers 3d en 3e zijn al in gebruik):
  één gemarkeerd Datum-attribuut met een berekening uit een andere datum,
  een initialisatie met een datumliteral, een regel die `afwijkende opgave
  X` leest, en een berekening die X leest (de lezer leest de opgeloste
  waarde). Cases handgerekend, in de trant van
  `jutterswet-opgave-boven-berekening`: geen opgave; opgave gelijk; opgave
  wijkt af (het §9.5-feit); opgave zonder feiten; niets (de initialisatie
  beslist); X zelf in de invoer met feiten (Reading A: de berekening
  verdringt de invoer) en zonder feiten (de invoer blijft staan, met een
  verwachting op de lezer zodat de case de niets-doende uitvoerder
  weerlegt). Het datumrekenen moet de initialisatie ruimte laten: meet of
  `plus` op een lege datum beslist en schrijf dan `indien zijn vinddag
  gevuld is` op de berekening (de les van §7 bij `maal`).
- K5: speelt de initialisatiedefault met datumliteral in een van de drie
  uitvoerders niet, dan weigert de resolver een initialisatie van een
  gemarkeerd Datum-attribuut met een eigen zin, gemeten en gemeld, en
  krijgt de golden geen initialisatie. Geen wijziging aan `backends/`,
  `ref/` of `spec/` in deze kaart.

## Acceptatie

- (a) R1 en R2 resolven zonder ERROR; R2 emitteert de vijf stukken van §7
  op `meldingsdatum`, met Datum in dagen op `opgegeven meldingsdatum` en
  `berekende meldingsdatum`; R3 emitteert byte-gelijk aan vóór de kaart.
- (b) JUnit-getuigen in `OpgaveMarkeringTest`: de lowering op een Datum in
  dagen (namen, datatype van de gegenereerde attributen, de initialisatie
  met datumliteral als laagste default); de weigertabel zonder de rij
  `Datum in dagen` en met de rij `Datum en tijd in millisecondes |
  Datum-tijd`; de nieuwe weigerzin letterlijk.
- (c) De golden `jutterswet-opgave-datum` (`bron.rgs`, `ir.json` als
  frontendemissie met `diagnostics: []`, cases, `NOTE.md` met casustekst
  en handberekening), toegelaten via `scripts/admit.sh`; ref ≡ clingo ≡
  Drools-exec; elke case weerlegt de niets-doende uitvoerder (gate 7)
  zonder nieuwe overlever. Een `scenario.rgs` erbij als de scenariolaag
  elke verwachting draagt (gate 16); anders handgeschreven cases zoals bij
  `jutterswet-opgave-boven-berekening`, met de reden in het Result.
- (d) Gate 12: `EXPECTED_PLAYED` +1 met redenblok, de golden in klasse (A)
  van `DRL_CHANNEL` (`EXCLUDE|ch. 7 prioritizedDefaults: no defaults
  lowering in this branch`) en de koptellingen van klasse (A) bijgewerkt.
  `scripts/` is beschermd: als **voorstel** in een eigen commit op de
  branch.
- (e) `DIVERGENCES.md` §7: de weigerbullet over het datatype, de eerste
  bullet van "Scope / limitations" en de golden-alinea bijgewerkt, met de
  meting van deze kaart (datum, de drie uitvoerders, de tellingen). Geen
  kaartnummers of K-nummers in DIVERGENCES, NOTE, javadoc en scripts.
- (f) Inertie: de grammatica verandert niet; het enige gedrag dat
  verandert is dat van een document met de markering op een Datum in
  dagen, dat vóór de kaart een ERROR was. Gate 10 (alle rgs-goldens
  byte-gelijk) en gate 11 (toka-pariteit) zijn de meting; het Result telt
  daarnaast de bronnen van corpus en pariteitszone die de markering dragen
  (verwacht: alleen de twee opgave-goldens).
- (g) Een registerzin voor `spec/CONSTRUCTS.md` rij 8 in het Result (een
  aanvulling op de Landed-alinea van 2026-09-25: sinds deze kaart ook een
  Datum in dagen); de hoofdsessie landt hem.
- (h) Alle gates groen, in het bijzonder 3, 5, 6, 7, 10, 11, 12 en 16.

## Mag raken

`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/RuleChecker.java`,
`frontends/regelspraak/src/test/java/dev/uchar/regelspraak/OpgaveMarkeringTest.java`,
`frontends/regelspraak/grammar/DIVERGENCES.md`,
`corpus/golden/jutterswet-opgave-datum/`, `scripts/check-drools.sh` en,
alleen als een overlever onvermijdelijk is, `scripts/check-noop.sh`
(**voorstel**), kaartverplaatsing. Niet: de grammatica, `spec/`,
`backends/`, `ref/`, `toka/`, de andere goldens.

## Niet in deze kaart

`Datum en tijd in millisecondes`, Tekst, een enumeratie, Percentage,
Boolean en Numeriek met eenheid (elk een eigen golden); de
kenmerkmarkering (§7 "Scope / limitations"); een typenaam in de
gegenereerde namen; de uitleg van de defaults in de werkbank.

## Minimale reproductie

`68eed2fb`, distributie `bin/regelspraak check`; `/` scheidt regels.

    R1  Objecttype de Vondst (mv: Vondsten)
            de vinddatum Datum in dagen;
            de meldingsdatum Datum in dagen (mag opgegeven worden);
        nu: ERROR regel 3 "attribuut 'meldingsdatum' van Vondst: '(mag opgegeven worden)' staat alleen
        op een Numeriek attribuut zonder eenheid, en 'meldingsdatum' is Datum in dagen — een
        defaults-groep op een attribuut van een ander datatype of met een eenheid is nog niet in alle
        drie de uitvoerders gemeten (dialect §7)"; exit 1.
        Na de kaart: de WARNING van §7 (geen rekenende schrijver), exit 0.
    R2  R1 + Regel melding op vinddag / geldig altijd /
        De meldingsdatum van een Vondst moet gesteld worden op de vinddatum van de Vondst.
        nu: dezelfde ERROR; exit 1. Na de kaart: exit 0 en de vijf stukken.
    R3  Objecttype de Vondst (mv: Vondsten)
            de vinddatum Datum in dagen;
            de opgegeven meldingsdatum Datum in dagen;
            de meldingsdatum Datum in dagen;
        Regel melding op vinddag / geldig altijd /
        De meldingsdatum van een Vondst moet gesteld worden op de vinddatum van de Vondst.
        Uitzondering op melding op vinddag: opgegeven melding / geldig altijd /
        De meldingsdatum van een Vondst moet gesteld worden op de opgegeven meldingsdatum van de Vondst
        indien zijn opgegeven meldingsdatum gevuld is.
        nu: exit 0, één PrioritizedDefaults; ref PASS, clingo PASS, Drools-exec PLAYED 2 op de twee
        cases van de meting. Na de kaart: ongewijzigd.
