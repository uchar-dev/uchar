# drools-temporele-naamnorm: temporele lookups vergelijken op exacte spelling

Gemeten door de periode-zink-kaart (2026-09-23, route R9): een
heelperiode-guard met `Gehesen` tegen een declaratie `gehesen` is voor
de verifier, ref en clingo hetzelfde kenmerk (naamnorm: trim +
witruimtevouw + lowercase, RUNTIME-SEMANTICS ch. 5.6), maar de
Drools-backend zoekt temporele namen op EXACTE spelling: 22×
`kenmerkGranulariteitLeden().containsKey` en 50×
`granulariteitLeden().containsKey`. Gevolg vóór de zink-kaart: de
ankerpoging miste het kenmerk en exec las stil het DOEL (antwoord 1
waar ref/clingo 0 geven); sinds die kaart wordt de doelpoging benoemd
geweigerd, dus de flank is dicht — maar de lookup-asymmetrie zelf
blijft en kan elke temporele vorm met afwijkende spelling naar een
weigering sturen waar de andere uitvoerders spelen. De golden
`jutterswet-seinjaar` pint de huidige stand bij naam en moet bij het
oppakken van kanaal wisselen (de NOTE zegt dat expliciet).

Zelfde familie, erbij belegd (consistentie-label-kaart, 2026-09-23):
`CaseRunner` vergelijkt de REGELNAAM van een inconsistent-sleutel
byte-exact waar ref `norm` plus versiestrip toepast. Geen
corpusdrager vandaag; hier meenemen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Inventariseer de 72 lookup-plekken en leid ze door één
  genormeerde vergelijking (het huispatroon van de
  enumnaam-identiteit: `Namen.norm`), of motiveer per klasse waarom
  exacte spelling daar juist is.
- (b) Rood-eerst getuigen beide takken voor de R9-vorm (guard-spelling
  wijkt af van de declaratie): na de fix speelt hij met refs antwoord.
- (c) `jutterswet-seinjaar` wisselt van kanaal (weigering → PLAYED in
  de ankerpoging): pins en kanaallijsten bewegen — benoemd
  beschermd-pad-voorstel; corpussweep verder byte-identiek.
- (d) Alle veertien gates groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh`
(**voorstel**, pins/kanaallijsten), kaartverplaatsing. NB: serieel met
andere Drools-kaarten.

## Result (2026-09-23)

Geland in beide takken. Alleen `jutterswet-seinjaar` wisselt van kanaal;
alle veertien gates groen.

### (a) Inventaris op de huidige stand (basis f2638a49)

Gehercount: 72 `containsKey`-plekken (22 kenmerk, 50 attribuut) en
daarnaast 17 exacte `granulariteitLeden().get(naam)`-aanroepen die de kaart
niet noemde — 16 echte LEZINGEN van de granulariteit en één aanwezigheidsvraag
in vermomming (ExecModelTranspiler r. 368, `get(…) != null`; de lens telde
17 en had gelijk, hier eerst als 16 lezingen geteld), dus 73
aanwezigheidsvragen en 16 lezingen, plus `Tijdlijnen.meerPeriodiek` (het periodeaantal, exact via
`tijdlijnAantal.get`). Per bestand: ExecModelTranspiler 55, DrlTranspiler 33
(waarvan 4 al via `bevatNaam`, de uniciteitssleutels), CaseRunner 5,
EffectieveEenheid 1, StaandeDoelen 1 (al `bevatNaam`).

Door de norm (één contract, geen dubbele normalisatie: `gedeclareerdeNaam`
doet eerst de exacte treffer, pas daarna `norm`):
- 72 aanwezigheidsvragen → `Namen.bevatNaam` (bestond al, het
  huispatroon van de sleutelconventie- en enumnaamkaarten);
- de 16 lezingen en de `get(…) != null`-vraag → de nieuwe tweeling
  `Namen.opNaam(map, naam)`
  (`map.get(gedeclareerdeNaam(map.keySet(), naam))`); zonder die tweeling
  zou een lid dat onder de norm temporeel is een granulariteit null lezen;
- `Tijdlijnen.meerPeriodiek` → `Namen.opNaam`; anders slaagt de temporele
  toets en leest een anders gespeld raster als enkel-periodiek.
De map zelf normeren bij het bouwen kon niet: `granulariteitLeden()` e.d.
leven in het frontendmodel (`frontends/`, buiten deze kaart).

Bewust exact gelaten:
- `FactModelGenerator` r. 73/79 (`ot.attribuutGranulariteit.containsKey`,
  `ot.kenmerkGranulariteit.containsKey`): beide kanten zijn DECLARATIES op
  hetzelfde objecttype (de attribuut-/kenmerklijst tegen de facetmap van
  dezelfde node), geen geschreven verwijzing.
- `IrLoader` (vullen van de maps): schrijft de declaratie zoals ze op de
  draad staat.
- Gevonden, niet aangeraakt: het frontendmodel leest
  `attribuutTijdlijnNaam` exact in `heeftVariabelStartpunt`,
  `startpuntAnkerLid` en `isStartpuntLid` (7 aanroepen in de twee
  transpilers). Dat is de §9.9-startpuntfamilie; `ref` voert die niet uit
  (geen treffer op startpunt in `ref/lib/`), de enige golden met een
  variabel startpunt (`jutterswet-vormenkabinet`) heeft geen casus. Er is
  dus geen uitvoerder om tegen te meten, en de methode ligt in
  `frontends/`. Open punt, geen fix zonder getuige.

Regelnaam van de runner: `CaseRunner.consistentie` vergeleek
`rule.equals(getRegel)`; nu `zelfdeRegel` = `norm(zonderVersie a) =
norm(zonderVersie b)`, ref's `Inconsistent`-arm (`ref/lib/case.ml`).
`zonderVersie` is `Checks.strip_version` van de verifier, teken voor teken
nagebouwd: laatste `#`, index ≥ 2, precies een spatie ervoor, minstens één
cijfer erna en alleen cijfers; strip vóór norm. Bewust NIET
`ProducentConsument.basisNaam`, die ook een groepsprefix en een
beslistabelsuffix stript en dus ruimer is dan ref. Gemeten met ref's runner
op `jutterswet-keurspelling` (V1, regel "geen wrakhout ter keuring"):
"Geen  Wrakhout ter keuring", "geen wrakhout ter keuring #2" en
"Geen wrakhout ter keuring  #17" → `oracle: PASS (1 expectations)`;
"…keuring#2", "…keuring #2a", "…keuring #" → ref `CASE ERROR: inconsistent
expectation references unknown rule` (bij het laden; de runner heeft die
laadcontrole niet en meldt de ontbrekende fact).

### (b) Rood-eerst en mutatie

Nieuwe getuige `TemporeleNaamnormWitnessTest` (4 tests): de R9-vorm op de
seinjaar-golden in beide takken (2 en 0 spelen, de doellezing 1 wordt als
waarde afgewezen), het periodeaantal (inclusief de twee grenzen die ref's
norm NIET vouwt: niet-ASCII, newline), de regelsleutel over de zes
spellingen hierboven, en de versiestrip als eenheid. Bestaande test
aangepast (met melding): `HeelperiodeDoelpogingWitnessTest` test 2 pinde
de R9-weigering van de zink-kaart en heet nu
`de_naamnorm_route_speelt_op_de_anker_in_beide_takken` (`groen`, 0); de
weigering zelf blijft gepind door test 1 en 3 van die klasse.

Rood-eerst: pre-fix main-bronnen (stash), alleen de twee statische helpers
in CaseRunner bijgezet zodat de test compileert:

    Tests run: 8, Failures: 2, Errors: 2
    TemporeleNaamnorm…de_heelperiode_guard_…:46 » Excluded transpiler SKIP: feitcreatie:
      vakbeseining door het kustvak — feitcreatie (voorwaarde niet naar anker of doel te
      lowereren — gehele-periodekwalificatie: alleen de anker is gebonden (WF-109): het kustvak
      gedurende het gehele jaar Gehesen is): vakbeseining door het kustvak
    TemporeleNaamnorm…het_periodeaantal_…:61 andere hoofdletter ==> expected: <true> but was: <false>
    TemporeleNaamnorm…de_regelnaam_…:77 exec-model — pin 'Geen  Wrakhout ter keuring' ==> expected:
      <null> but was: <case: verwacht inconsistent (Geen  Wrakhout ter keuring @ V1), niet gevonden
      onder herkeuring volgt de staat @ V2, geen wrakhout ter keuring @ V1>
    HeelperiodeDoelpoging…de_naamnorm_route_…:123 » Excluded transpiler SKIP: … (WF-109): het sein
      gedurende het gehele jaar gehesen is) …

(de versiestrip-eenheidstest was groen, want de helpers waren bijgezet).
Na herstel alle bestanden `cmp`-gelijk aan de post-fix-kopie.

Mutaties (telkens teruggezet, `cmp` bevestigd):
1. `Namen.bevatNaam`/`opNaam` weer exact: 18 tests, 3 failures + 2 errors —
   de R9-getuige, het periodeaantal, de omgebouwde HeelperiodeDoelpoging-test,
   en twee bestaande `NaamNormWitnessTest`-sleuteltests (die leunen al op
   `bevatNaam`); seinjaar exec en drl weer EXCLUDE met de WF-109-zin.
2. `zelfdeRegel` → `a.equals(b)` plus `meerPeriodiek` → `get(lid)`: 3 van 4
   rood (regelsleutel, versiestrip-eenheid, periodeaantal).

### (c) Kanaalwissel `jutterswet-seinjaar`

Beide takken `PLAYED 3`. Gemeten waarden (verwachting op 99 gezet, zodat de
runner de werkelijke waarde noemt), identiek in exec en drl:
`case1 werkelijk 2 | case2 werkelijk 0 | case3 werkelijk 2` = de gepinde
ref/clingo-waarden 2 / 0 / 2.

### (d) Sweeps

Corpus, beide takken, pre-jar tegen post-jar, 233 goldens met casussen:
precies twee regels verschillen, `exec jutterswet-seinjaar` en
`drl jutterswet-seinjaar`, EXCLUDE → PLAYED 3. De rest byte-identiek.
Played: exec 181 → 182, drl 160 → 161.

Extra meting (hoofdlettertwist): in 24 goldens met casussen en een
temporele verwijzing is elke verwijzing naar een temporeel lid in de
regels in ASCII-hoofdletters gezet (declaraties en casussen ongemoeid;
de getter blijft zo gelijk). Verifier, ref en clingo: PASS op alle 24.
Pre-fix jar: 0 van 24 gespeeld in beide takken (weigeringen, en 3× ERROR
— `ClassCastException ArrayList → BigDecimal` in exec, een
eenheidsconflict in drl — op keurlast, muntbewaring, tempovouw). Post-fix
jar: 21 gespeeld per tak, en alle 48 statusregels gelijk aan de
ongetwiste stand van dezelfde golden.

Gates, elk los na `eval $(opam env)`:
1 self-contained PASS; 2 ir-schema 278 bestanden; 3 verifier 278 golden
schoon, 440 negatief; 4 case-schema 638; 5 oracle 788 voorbeelden, 595
casussen; 6 clingo 518 checks, 595 casussen; 7 noop 559 + 36 overlevers;
8 rood 43 (Drools-been 9 + 34); 9 catala 201 checks, 12 goldens;
10 regelspraak 409 tests, 70 rgs-goldens; 11 toka-parity 29
ref/clingo/Drools, 45 re-emissies, 27 TOKA-tests; 12 drools played 182 =
pin, DRL 161 = pin, JUnit 617; 13 reftype 131 twists, 128 replays;
14 flogic 130 checks.

### Beschermd-pad-voorstel

`scripts/check-drools.sh` (commit "gate 12: seinjaar speelt…"):
`EXPECTED_PLAYED` 181 → 182 en `EXPECTED_DRL_PLAYED` 160 → 161, elk met
redenblok; de seinjaar-regel uit `EXPECTED_CHANNEL` en uit `DRL_CHANNEL`
klasse (C); kop van klasse (C) 11 → 10 met regel, en "TEN of the eleven"
→ "NINE of the ten" (de gedeelde doelpoging-weigering valt uit de
opsomming). Niets anders aan het script.

Corpus onder het in de NOTE voorziene mandaat:
`corpus/golden/jutterswet-seinjaar/NOTE.md`, sectie "What it pins in the
Drools backend", in de verleden tijd gezet met de kanaalwissel erbij;
`ir.json` en casussen ongemoeid.

### Registerzinnen voor `spec/CONSTRUCTS.md`

Rij 29, VERVANGING (anders spreekt de rij zichzelf tegen). Oude substring,
exact zoals op main c15ca860:

> Witness: `corpus/golden/jutterswet-seinjaar` (ref = clingo 2 / 0 / 2; both Drools branches refuse, pinned by name in gate 12) and `HeelperiodeDoelpogingWitnessTest`.

Nieuwe tekst:

> Witness: `HeelperiodeDoelpogingWitnessTest`. The case-variant route stopped reaching the doel attempt with card `drools-temporele-naamnorm` (2026-09-23): both Drools branches now look temporal names up under the name identity, so the guard's `Gehesen` finds the anker's declared `gehesen`, the anker attempt lowers it, and `corpus/golden/jutterswet-seinjaar` plays 2 / 0 / 2 in the full square (`EXPECTED_PLAYED` 181 → 182, `EXPECTED_DRL_PLAYED` 160 → 161). The refusal of the doel attempt stands and is pinned by that test's displaced-role route and the WF-109 witness.

Rij 27, achter de enumnaam-landing ("De regelkant van dezelfde
identiteit is gedicht …"), als volgende zin:

> The temporal facets follow the same identity (2026-09-23, card
> `drools-temporele-naamnorm`): both Drools branches ask whether a member
> is time-dependent, and read its granularity and period count, by the
> declared name under `Namen.norm` instead of by exact spelling (73
> presence tests, 16 reads, the period-count predicate); the case runner
> names the setter of a timeline input from the declaration, and matches
> the rule of an `inconsistent` pin as ref does, by norm after the
> verifier's version strip. Witness: golden
> `jutterswet-seinjaar` and `TemporeleNaamnormWitnessTest`. Not covered:
> the §9.9 variable-startpunt lookups, which the frontend model reads by
> exact spelling and which ref does not carry.

(Rij 27 is de datatype-rij waar de enumnaam-landing staat; als de hoofdsessie
een aparte rij voor naamidentiteit verkiest, is de tweede zin los te plaatsen.)

### Open punten

- §9.9-startpunt-lookups exact in het frontendmodel (zie (a)); pas zinvol
  zodra ref de familie draagt.
- (Opgelost in de slotronde, A5: de setter van tijdlijninvoer.) De
  SCALAIRE casusinvoer (`attributes` zonder tijdlijn, `characteristics`)
  roept de setter nog op de geschreven naam aan; niet gemeten, niet
  aangeraakt — buiten de temporele familie van deze kaart.
- Lensvondst A2, bestond al en is door deze kaart alleen bereikbaar
  geworden: een attribuut `Gehesen` naast een kenmerk `gehesen` op één type
  is verifier-schoon en ref speelt het, maar Drools faalt luid op de
  botsing van de bean-getters (één naamruimte voor attribuut en kenmerk).
  Kaart: `20260923-drools-beannaamruimte-botsing` (maakt de hoofdsessie
  aan).

## Slotronde (2026-09-23, na de lens)

A1 — getuige voor de granulariteitsLEZING. Lens-mutant A2 (`opNaam` weer
exact, periodeaantal genormeerd) liet JUnit 617 groen. Nieuwe rij
`een_temporele_attribuutverwijzing_in_hoofdletters_speelt`: golden
`jutterswet-tempovouw` in het geheugen, elke regelverwijzing naar het
MAAND-temporele `bewaartarief` als `BEWAARTARIEF`; ref en clingo `PASS (3
expectations)`, beide takken spelen 48 / 0. Onder precies de A2-mutant
(`perNaam.get(naam)` in `opNaam`; `meerPeriodiek` via
`gedeclareerdeNaam`), 7 tests:

    Tests run: 7, Failures: 0, Errors: 2
    …een_temporele_attribuutverwijzing_in_hoofdletters_speelt:72 » NullPointer Cannot invoke
      "io.github.yhavinga.regelspraak.model.RegelSpraakModel$Granulariteit.name()" because the
      return value of "io.github.yhavinga.regelspraak.transpiler.kern.Namen.opNaam(java.util.Map,
      String)" is null
    …attribuuttijdlijninvoer_met_afwijkende_spelling_speelt:113 » NullPointer Cannot invoke
      "io.github.yhavinga.regelspraak.model.RegelSpraakModel$Granulariteit.ordinal()" because
      "gran" is null

(de tweede rij raakt de `opNaam`-lezing in CaseRunner r. 479). Hersteld,
`cmp` gelijk.

A5 — setter van tijdlijninvoer. Gemeten op de eerste fixcommit: casussen
met `characteristicTimelines: {"geHesen": …}` (seinjaar, heel en half
jaar) en `attributes: {"bewaarTarief": {timeline…}}` (tempovouw): ref en
clingo `PASS (3 expectations)`, beide takken `ERROR RuntimeException: kon
setter setGeHesen(List) niet aanroepen op Kustvak` resp. `…
setBewaarTarief(List) niet aanroepen op Vondst`. Fix: twee regels in
CaseRunner, `Namen.setter(Namen.gedeclareerdeNaam(<facetmap>.keySet(),
key))` voor de attribuut- en de kenmerktijdlijn. Twee rijen
(`kenmerktijdlijninvoer_…`, `attribuuttijdlijninvoer_…`) spelen 2 / 0 en
48 / 0 in beide takken. Mutant (beide setters terug op `e.getKey()`):

    Tests run: 7, Failures: 0, Errors: 2
    …attribuuttijdlijninvoer_met_afwijkende_spelling_speelt » RuntimeException: kon setter
      setBewaarTarief(List) niet aanroepen op Vondst
    …kenmerktijdlijninvoer_met_afwijkende_spelling_speelt » RuntimeException: kon setter
      setGeHesen(List) niet aanroepen op Kustvak

Hersteld, `cmp` gelijk.

A4 — de kop van klasse (C) in het scriptvoorstel staat nu chronologisch,
oudste hertelling eerst; inhoud ongewijzigd.

A3 — rij-29-zin herschreven als vervanging (zie boven).

Gates na de slotronde, los na `eval $(opam env)`: backend-JUnit 620
tests, 0 failures, 0 skipped; gate 12 PASS, played 182 = pin, DRL 161 =
pin, JUnit 620. De overige twaalf gates zijn niet opnieuw gedraaid: deze
ronde raakte alleen `backends/drools/**` en `scripts/check-drools.sh`.
