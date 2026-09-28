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

## Result (2026-09-28, bouwer)

Branch `card/opgave-markering-datum`, basis `31be1777`, kop vóór deze
Result-commit `cbc1bdfa`. Werkboom schoon; niets gepusht of gemerged.
Proeven en metingen buiten de repo staan in `/tmp/v2/bouwer-datum/`
(probe, tegencases, sweep, gatelogs `gates-basis/` en `gates-kop/`).

### Wat er landde

- `RuleChecker.opgaveDeclaratieFout` laat de markering toe op een
  attribuut van datatype `Datum in dagen`, naast Numeriek zonder eenheid.
  `Datum en tijd in millisecondes`, Tekst, Percentage, Boolean, een
  enumeratie en Numeriek met eenheid blijven geweigerd, met de zin van K3
  letterlijk. De lowering is niet gewijzigd (K2). Javadoc van
  `opgaveDeclaraties` en het commentaar bij de weigering bijgewerkt.
- `OpgaveMarkeringTest`: twee nieuwe getuigen
  (`op_een_datum_in_dagen_bouwt_de_markering_dezelfde_vijf_stukken`,
  `op_een_datum_in_dagen_draagt_de_wire_het_datumtype_en_de_datumliteral`);
  de weigertabel zonder de rij `Datum in dagen`, met de rij `Datum en tijd
  in millisecondes | Datum-tijd`, en de nieuwe zin letterlijk.
- Golden `corpus/golden/jutterswet-opgave-datum/` (bron.rgs, scenario.rgs,
  ir.json, zeven cases, NOTE.md met de casustekst "Art. 3f (dag van
  aanmelding)" en de handberekening), toegelaten via `scripts/admit.sh`.
- `scripts/check-drools.sh` (**voorstel**, eigen commit): pin, redenblok,
  klasse (A) en de kop van klasse (A).
- `frontends/regelspraak/grammar/DIVERGENCES.md` §7: de weigerbullet over
  het datatype, de eerste bullet van "Scope / limitations" en de
  golden-alinea.
- `scripts/check-noop.sh` niet geraakt: geen overlever.

### Acceptatie, met echte uitvoer

**(a)** Frontend vóór = build van `d116ac55` (kopie buiten de repo),
na = kop.

- R1 vóór: `ERROR (line 3): attribuut 'meldingsdatum' van Vondst: '(mag
  opgegeven worden)' staat alleen op een Numeriek attribuut zonder
  eenheid, en 'meldingsdatum' is Datum in dagen — …`, exit 1. Na:
  `WARNING (line 3): attribuut 'meldingsdatum' van Vondst: '(mag opgegeven
  worden)' staat op een attribuut dat geen regel berekent of vaststelt —
  'meldingsdatum' is al invoer, en de markering genereert niets (dialect
  §7)`, exit 0.
- R2 vóór: dezelfde ERROR, exit 1. Na: exit 0, `ir: objecttypen=1
  feittypen=0 resolvedRegels=2`, `verifier: PASS`, `diagnostics: []`. De
  vijf stukken op de wire: `opgegeven meldingsdatum` en `berekende
  meldingsdatum` direct na `meldingsdatum`, elk `{"node": "Date",
  "precision": "day"}`; `melding op vinddag` schrijft `berekende
  meldingsdatum`; groep `opgave boven berekening meldingsdatum` (opgave,
  berekening); kenmerk `afwijkende opgave meldingsdatum` (possessive) met
  regel `afwijking opgave meldingsdatum`; consistentie `controle opgave
  meldingsdatum` met source `de meldingsdatum Datum in dagen (mag
  opgegeven worden)`, line 3. De gegenereerde regel `afwijking opgave
  meldingsdatum` is als JSON-object gelijk aan dezelfde regel met de hand
  geschreven op twee datums (eigen proef).
- R3: de emissie vóór en na is byte-gelijk (`cmp`); de R3-variant van de
  hoofdsessie (met `// Bron:`) ook, en na is hij gelijk aan haar gepinde
  `ir.json`.

**(b)** Rood op `84dc4c52` (toetsen erbij, resolver nog de basis):
`[ 969 tests found ]`, `[ 962 tests successful ]`, `[ 7 tests failed ]`:
de vijf rijen van de tabel op de zin, de twee nieuwe getuigen op de
ERROR. Groen op `440bdcec`: `[ 969 tests successful ]`, `[ 0 tests
failed ]`.

**(c)** `admit: ADMITTED corpus/golden/jutterswet-opgave-datum (7 case(s);
verifier-clean, ref = clingo, no-op refuted, gates green)` (met het
voorstel voor gate 12 in de werkboom). De golden-tabel staat hieronder.
`scenario.rgs` erbij: de scenariolaag draagt elke verwachting (een
datumwaarde, een leeg attribuut, een kenmerk aanwezig en afwezig,
consistent, en `inconsistent volgens "controle opgave meldingsdatum"
voor V`); `--cases` schrijft de zeven casebestanden en een `ir.json`
byte-gelijk aan de productieroute. Gate 7: alle zeven geven onder
`--noop` exit 1, geen nieuwe overlever.

**(d)** Gate 12: `played 219`, `played matches the pinned 219`, `DRL-text
branch played 188 (… pin 188)`, `DRL-text floor 87 non-PLAYED goldens
pinned by name`. De regels staan onder "Gewijzigde scriptregels".

**(e)** DIVERGENCES §7, drie plekken, geen kaart- of K-nummers: de
weigerbullet noemt beide families, de zin, de telling van de
defaults-groepen op een attribuut (40 groepen in 25 goldens, 39 op een
getal zonder eenheid in 24 goldens, 1 op een datum; gemeten over alle
`corpus/golden/**/ir.json`, het doeltype langs de supertypes gevolgd) en
de eenheidsclausule na een datum; Scope noemt getal en datum; de
golden-alinea noemt `jutterswet-opgave-datum` met de meting in de drie
uitvoerders en de reden dat haar berekening geen voorwaarde heeft.

**(f)** Zie "Inertietelling".

**(g)** Zie "Registerzin".

**(h)** Alle zestien gates groen op `cbc1bdfa`, zie "Gates".

### Afwijkingen van de kaart en keuzes, met meting

1. **Geen voorwaarde op de berekening (K4).** Gemeten vóór de golden, op
   de berekening `vinddag plus 3 dagen` alleen, zonder markering, een
   Vondst zonder vinddag, verwacht `meldingsdatum` leeg: ref `oracle:
   PASS (2 expectations)`, clingo `clingo: PASS (2 expectations)`,
   Drools-exec `PLAYED 1`. `plus` op een lege datum beslist dus niet
   (RUNTIME-SEMANTICS ch. 1.2, "leeg + 5 dagen = leeg"), de initialisatie
   houdt haar ruimte, en `indien zijn vinddag gevuld is` is niet nodig.
   case4, case5 en case7 pinnen hetzelfde onder de markering.
2. **K5 niet nodig.** De initialisatiedefault met datumliteral speelt in
   de drie uitvoerders: case5 (`meldingsdatum` 2026-03-01) is PASS in ref
   en clingo en PLAYED in de exectak, en de tegencases t11 (leeg), t14
   (2026-03-01 over de invoer) en t15 falen in alle drie.
3. **Eenheidsclausule na een datum.** `Datum in dagen met eenheid €` met
   markering gaf vóór twee ERRORs (`attribuut 'meldingsdatum' met eenheid
   '€': een Datum in dagen draagt geen eenheid` en de datatypeweigering),
   na alleen de eerste; exit 1 beide keren, geen IR. Keuze: de
   datumfamilie hangt niet af van de clausule, want DefinitionResolver
   weigert haar al; een zin van de markering erbij zou "… en 'X' is
   Datum in dagen" zeggen over een toegelaten datatype.
4. **Datumliteral in het model.** `Expr.Literal.waarde()` is
   `2026-03-01`, de brontekst blijft `01-03-2026`; de getuige pint beide
   (het model en de wire).
5. **Toetsnaam.** `de_markering_op_iets_anders_dan_een_getal_zonder_eenheid_weigert`
   heet nu `…_zonder_eenheid_of_een_datum_weigert`, zodat de naam zegt wat
   de tabel toetst.
6. **Namen in de golden.** De andere datum heet `vinddag` (zoals K4
   schrijft), de lezer `uiterste afhaaldatum` (lid 5), de kenmerklezer
   `ter controle` (lid 4).
7. **Volgorde van de commits.** De golden (`981709d9`) staat vóór het
   voorstel voor gate 12 (`8195d1d2`): op `981709d9` alleen is gate 12
   rood (219 spelers tegen pin 218). De toelating draaide met het voorstel
   in de werkboom; beide commits landen samen.
8. **Inertie, verwachting van de kaart.** "Alleen de twee opgave-goldens"
   klopt voor de bronnen vóór deze kaart; met de nieuwe golden dragen
   drie corpusbronnen de markering (zie Inertietelling).
9. **Hertelling in gate 12.** Behalve de gevraagde koptellingen van klasse
   (A) ook `eighty-one of the eighty-six` → `eighty-two of the
   eighty-seven` in de kop van `DRL_CHANNEL`, gemeten over de lijst: 87
   regels, 34 met de (A)-zin, 37 laderweigeringen, 11 MARKERs (82).

### Golden `jutterswet-opgave-datum` (handgerekend, absoluut)

Rekenen: 10-03 + 3 = 13-03-2026; 13-03 + 30 = 12-04-2026; 12-03 + 30 =
11-04-2026; 01-03 + 30 = 31-03-2026; 20-03 + 30 = 19-04-2026.

| Case | Invoer | Verwacht | ref | clingo | Drools-exec | no-op |
|---|---|---|---|---|---|---|
| case1-geen-opgave | vinddag 10-03-2026 | meldingsdatum 13-03-2026; berekende 13-03-2026; kenmerk afwezig; ter controle afwezig; afhaaldatum 12-04-2026; consistent | PASS (6) | PASS (6) | PLAYED 1 | exit 1 |
| case2-opgave-gelijk | vinddag 10-03-2026, opgegeven 13-03-2026 | meldingsdatum 13-03-2026; berekende 13-03-2026; kenmerk afwezig; ter controle afwezig; afhaaldatum 12-04-2026; consistent | PASS (6) | PASS (6) | PLAYED 1 | exit 1 |
| case3-opgave-wijkt-af | vinddag 10-03-2026, opgegeven 12-03-2026 | meldingsdatum 12-03-2026; berekende 13-03-2026; kenmerk aanwezig; ter controle aanwezig; afhaaldatum 11-04-2026; inconsistent `controle opgave meldingsdatum` × V | PASS (6) | PASS (6) | PLAYED 1 | exit 1 |
| case4-opgave-zonder-feiten | opgegeven 12-03-2026 | meldingsdatum 12-03-2026; berekende leeg; kenmerk afwezig; ter controle afwezig; afhaaldatum 11-04-2026; consistent | PASS (6) | PASS (6) | PLAYED 1 | exit 1 |
| case5-niets | — | meldingsdatum 01-03-2026 (initialisatie); berekende leeg; kenmerk afwezig; afhaaldatum 31-03-2026; consistent | PASS (5) | PASS (5) | PLAYED 1 | exit 1 |
| case6-doel-zelf-in-invoer | vinddag 10-03-2026, meldingsdatum 20-03-2026 | meldingsdatum 13-03-2026 (Reading A); berekende 13-03-2026; kenmerk afwezig; afhaaldatum 12-04-2026; consistent | PASS (5) | PASS (5) | PLAYED 1 | exit 1 |
| case7-doel-zelf-zonder-feiten | meldingsdatum 20-03-2026 | meldingsdatum 20-03-2026 (de invoer blijft); berekende leeg; kenmerk afwezig; afhaaldatum 19-04-2026; consistent | PASS (5) | PASS (5) | PLAYED 1 | exit 1 |

"PASS (n)" is `oracle: PASS (n expectations)` en `clingo: PASS (n
expectations)`; de golden als geheel: Drools-exec `PLAYED 7`, DRL-tak
`EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no
defaults lowering`. De no-op weerlegging van case7 is de lezer:
`MISMATCH: V.uiterste afhaaldatum = leeg, expected 2026-04-19`. Vijftien
tegencases (buiten de repo, elk één verwachting omgedraaid) falen in alle
drie, bv. t13 (case6 met `meldingsdatum` 20-03-2026): ref en clingo exit
1, Drools `FAIL …: attribuut meldingsdatum: verwacht 2026-03-20,
werkelijk 2026-03-13`.

### Gewijzigde scriptregels (`scripts/check-drools.sh`, VOORSTEL, `8195d1d2`)

- `EXPECTED_PLAYED=218` → `EXPECTED_PLAYED=219`, met redenblok `# 218 ->
  219 (2026-09-28, card opgave-markering-datum): jutterswet-opgave-datum,
  new, from bron.rgs and scenario.rgs and hand-computed — …` (de
  markering op een Datum in dagen; de exectak speelt de zeven cases,
  PLAYED 7, de datumliteral van de initialisatiedefault erbij; de
  DRL-teksttak weigert op de defaults-zin, `EXPECTED_DRL_PLAYED` blijft
  188).
- `DRL_CHANNEL` klasse (A), in naamvolgorde tussen
  `jutterswet-opgave-boven-berekening` en `jutterswet-opgave-vormen`:
  `"jutterswet-opgave-datum|EXCLUDE|ch. 7 prioritizedDefaults: no
  defaults lowering in this branch"`.
- Kop klasse (A): `34 lines` → `35 lines`; `sitting 30 below` → `sitting
  31 below`; `33 of the 34 lines` → `34 of the 35 lines`; `the 34th,
  jutterswet-havenrooster` → `the 35th, …`; `32 of those 33 ARE` → `33
  of those 34 ARE`; `read as 33 exec-only players` → `read as 34 …`;
  `The 30 is 34 exec-only players — those 32,` → `The 31 is 35 exec-only
  players — those 33,`; hertelling erbij: `Recounted 2026-09-28 (card
  opgave-markering-datum): jutterswet-jaarvaststelling,
  jutterswet-vocabulaires and jutterswet-premierecht play in both
  branches and moved neither the gap nor a class head (218 - 188);
  jutterswet-opgave-datum joins the class, 34 -> 35 lines and 30 -> 31
  (218 -> 219 at EXPECTED_PLAYED), and the five class heads (A 35, B 37,
  C 10, D 2, E 3 = 87 lines) were counted against the list.`
- Kop `DRL_CHANNEL`: `eighty-one of the eighty-six` → `eighty-two of the
  eighty-seven`.

### Registerzin voor `spec/CONSTRUCTS.md` rij 8 (aanvulling op de Landed-alinea van 2026-09-25; de hoofdsessie landt hem)

> **Landed (2026-09-28, card `opgave-markering-datum`):** the opgave
> marker of form 3 stands on a `Datum in dagen` as well as on a number
> without a unit. The refusal of a date was a measurement boundary, not a
> design choice: the hand-written hardship pattern on a date played in
> ref, clingo and the Drools exec branch before the frontend admitted the
> marker. The lowering is unchanged: `opgegeven X` and `berekende X`
> carry X's datatype, the generated rule compares two dates with
> `notEqual`, and an authored initialisation with a date literal is the
> group's lowest default. `Datum en tijd in millisecondes`, a text, a
> percentage, a boolean, an enumeration and a number with a unit stay
> refused at the declaration, each until a golden of its own. Witness
> `jutterswet-opgave-datum` (one marked date computed from another date by
> `plus`, whose empty operand propagates so that the initialisation
> decides; a computation reading the marked date and a rule reading the
> generated characteristic; seven cases from a scenario document), in ref
> ≡ clingo ≡ Drools exec; the DRL-text branch refuses it on its defaults
> sentence. No wire, spec, verifier or executor change.

### Inertietelling

- De grammatica is niet geraakt (`git diff 31be1777..HEAD` raakt geen
  `.g4`).
- Sweep, vóór (`d116ac55`) tegen na (kop), elke bron los door het
  frontend, IR plus uitvoer plus exitcode vergeleken: 158 bronnen (111 in
  `corpus`: 102 `bron.rgs` en 9 `scenario.rgs`; 47 in de pariteitszone),
  157 identiek, 1 verschillend: `corpus/golden/jutterswet-opgave-datum/bron.rgs`
  (vóór de ERROR op regel 3, exit 1; na geschreven, exit 0).
- Bronnen met de markering: in `corpus` drie
  (`jutterswet-opgave-boven-berekening`, `jutterswet-opgave-vormen`, beide
  op getallen, en de nieuwe golden), in de pariteitszone nul.
- Gate 10: 102 rgs-goldens byte-gelijk; gate 11: 45 `.rgs` byte-gelijk
  en 29 pariteitscases in ref, clingo en Drools.

### Gates (elk los, na `eval $(opam env)`; kop `cbc1bdfa`; basis `d116ac55` tussen haakjes waar het getal verschilt)

- 1: `check-self-contained: PASS`
- 2: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 314 file(s) validate and round-trip, floor 192; 41362 object(s) in 815 document(s) in the schema's key order, 1 in no subschema)` (basis 313, 41252, 814)
- 4: `check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 781 case file(s) validate)` (774)
- 3: `check-verifier: PASS (314 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)` (313)
- 5: `check-oracle: PASS (ref suite 1152 worked examples, floor 489; 741 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)` (734)
- 6: `check-clingo: PASS (backend suite 1988 checks, floor 83; 741 cases replay exactly through clingo — ref = clingo over the corpus)` (734)
- 7: `check-noop: PASS (697 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)` (690; 44)
- 8: `check-rood: Drools leg PASS (12 fault-pinned, 28 not-carried with reason)` / `check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)`
- 9: `check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)` (catala aanwezig, gedraaid)
- 10: `check-regelspraak: PASS — JUnit suite green (969 tests, floor 173); 102 rgs golden(s) re-emit byte-identically (floor 80)` (967; 101)
- 11: `check-toka-parity: PASS` — `re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (…); TOKA suite 27 tests, floor 27`; `well-formedness: 28 replay slice(s) verifier-clean, floor 28 (…)`; `ref : 29 PASS / 0 diverge`; `clingo : 29 PASS / 0 loud translation-fault / 0 silent diverge`; `Drools : 29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR`
- 12: `check-drools: PASS` — `played 219`; `channel floor 56 non-PLAYED goldens pinned by name; played matches the pinned 219`; `DRL-text branch played 188 (its OWN count — the narrower branch, not the square; pin 188)`; `DRL-text floor 87 non-PLAYED goldens pinned by name`; `JUnit 846 tests, floor 239` (218; 86)
- 13: `check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)`; accounting `85 unit-bearing annotations = 45 twistable + 40 exempt …` (83 = 45 + 38: de twee duurliteralen van de golden zijn vrijgesteld)
- 14: `check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)`
- 15: `check-graaf: PASS` — `spec leg — 238 of 275 goldens compared, 131 dependencies equal to the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)`; `route leg — the graph of the wire equals the graph of the source on all 102 goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)` (237 of 274, 126; 101)
- 16: `check-scenario: PASS — 9 scenario golden(s) (floor 7) regenerate 40 case file(s) (floor 31) and their ir.json byte-identically` (8; 33)

### Open punten

1. `scripts/check-drools.sh` (`8195d1d2`) is een voorstel op een
   beschermd pad: de hoofdsessie landt het, samen met de golden.
2. De registerzin voor `spec/CONSTRUCTS.md` rij 8 hierboven: hoofdsessie.
3. DIVERGENCES, "Scenariotoetsen" → "Witnesses", zegt "seven goldens
   carry a `scenario.rgs`": vóór deze kaart waren het er al acht
   (`jutterswet-premierecht` staat niet in die lijst), nu negen. Niet
   geraakt: buiten de drie plekken van §7 die de kaart noemt.
4. De vloeren van gate 16 (`EXPECTED_MIN_SCENARIO_GOLDENS=7`,
   `EXPECTED_MIN_SCENARIO_CASES=31`) staan onder de meting (9 en 40);
   ophogen is een besluit van de maintainer, niet nodig voor groen.
5. De distributie (`bin/regelspraak`) draait op de oude submodulepin en
   weigert de markering op een datum tot die pin deze kop bevat.
6. `Datum en tijd in millisecondes`, Tekst, Percentage, Boolean, een
   enumeratie en Numeriek met eenheid: elk een eigen golden (niet in deze
   kaart).

### Commits op de branch

| Hash | Kop |
|---|---|
| `d116ac55` | kaart opgave-markering-datum geclaimd |
| `84dc4c52` | OpgaveMarkeringTest: de markering op een Datum in dagen, rood |
| `440bdcec` | RuleChecker: de markering ook op een Datum in dagen |
| `981709d9` | golden jutterswet-opgave-datum: de markering op een datum |
| `8195d1d2` | voorstel gate 12: jutterswet-opgave-datum speelt in de exectak |
| `cbc1bdfa` | DIVERGENCES §7: de markering op een Datum in dagen |

Daarna één commit met dit Result.

## Keuring (hoofdsessie, 2026-09-28)

Geland op main na een eigen run van alle zestien gates op de kop van de
branch, `83cc99a8` (logs `/tmp/v2/uchar-gates/`): groen. Gemeten daar:
gate 3 314 goldens schoon, 501 negatief; gate 5 en 6 741 cases exact;
gate 7 697 weerleggingen, 44 overlevers (ongewijzigd); gate 9 gedraaid,
241 checks; gate 10 969 toetsen, 102 rgs-goldens byte-gelijk; gate 11 45
bronnen byte-gelijk, 29 pariteitscases in de drie uitvoerders; gate 12
speelt 219 en 188 (de exec-pin één omhoog, eigen commit van de bouwer op
het beschermde pad, hier geland); gate 16 9 goldens, 40 casebestanden.
Vijf gates gaven in mijn eerste run exit 2: ze waren onder `sh` gestart
en gebruiken bash-procesvervanging; onder bash groen — geen bevinding
over de kaart.

Gelezen: de verbrede toelating in `opgaveDeclaraties` (één voorwaarde,
de zin van K3 letterlijk, de lowering ongeraakt); de twee getuigen en de
weigertabel met de rij Datum-tijd; de golden met eigen casustekst, zeven
handgerekende cases en een `scenario.rgs` die ze regenereert; het
gate-12-voorstel met de hertelling van klasse (A) en de kop van
`DRL_CHANNEL`; de drie plekken in DIVERGENCES §7 met de telling van de
defaultsgroepen op een attribuut (40 in 25 goldens, één op een datum).
De metingen die de kaart eenvoudiger maakten, aanvaard: `plus` op een
lege datum blijft leeg in de drie uitvoerders, dus de berekening draagt
geen voorwaarde; de initialisatiedefault met datumliteral speelt, dus
K5 vervalt; de eenheidsclausule na een datum blijft de ERROR van de
datatypelezer, zonder eigen zin van de markering.

Registerwerk in de landingscommit: de aanvulling op de Landed-alinea
van rij 8 in `spec/CONSTRUCTS.md`; in DIVERGENCES onder Scenariotoetsen
→ Witnesses de telling "seven goldens carry a scenario.rgs" naar negen,
met `jutterswet-premierecht` en `jutterswet-opgave-datum` erbij (open
punt 3 van de bouwer). De vloeren van gate 16 blijven staan (7 en 31
onder 9 en 40; geen krimp te bewaken). Open, niet als kaart: Datum-tijd,
Tekst, Percentage, Boolean, enumeratie en Numeriek met eenheid, elk een
eigen golden. Daarna: pin-bump naar de distributie (de dienst-replay-pin
gaat mee als de golden door de dienst speelt) en lokaal naar de
werkbank.
