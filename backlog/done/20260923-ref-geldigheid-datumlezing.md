# ref-geldigheid-datumlezing: asOfDate en geldigheid als opake strings vergeleken

Gemeten door de lezerszone-lens (2026-09-23): `Eval.applies` in ref
vergelijkt `asOfDate` en `geldigheid.vanaf`/`tot` LEXICOGRAFISCH als
ondoorzichtige strings, zonder de datumlezer. Gevolg, gemeten op
vensterheffing met `asOfDate "01-06-2026"` (dag-eerst): ref en clingo
antwoorden `overgangsheffing = 140` waar `leeg` hoort, zonder melding;
Drools geeft daar een ERROR. Een misvormde `geldigheid.vanaf` passeert
ref en clingo eveneens. Het hek is vandaag het schemapatroon (gates 2
en 4) — de corpuszones dragen alleen canoniek — maar de norm van de
lezerszone-kaarten ("een lezer die aanvaardt wat geen verifier
toelaat is ongetuigd gedrag") geldt hier onverkort, en de
stringvergelijking geeft op een niet-canonieke waarde een STIL ander
antwoord in twee van de drie uitvoerders.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer beide metingen (asOfDate dag-eerst; misvormde
  geldigheidsgrens) op alle uitvoerders, letterlijke uitvoer in het
  dossier.
- (b) ref leest de drie posities door de strikte ISO-lezer en weigert
  misvormd benoemd (clingo volgt via refs casemodule); rood-eerst per
  positie. Weeg of het schemapatroon dan volstaat als tweede hek of
  dat een casusgetuige blijvend hoort te pinnen.
- (c) Corpussweep: geen bestaande replay beweegt (alle corpuswaarden
  zijn canoniek); alle veertien gates groen.

May touch: `ref/**`, `backends/clingo/**` (volgt), kaartverplaatsing.
NB: raakt de ref-caselaag — serieel met kaarten die ref/lib raken.

## Acceptatie, aangescherpt bij de claim (2026-09-23)

- (a) Beide metingen gereproduceerd op ref, clingo en beide Drools-takken,
  letterlijk hieronder; inventaris van elke datumlezing in de caselaag
  van ref en clingo.
- (b) De drie posities (asOfDate, `geldigheid.vanaf`, `.tot`) door de
  strikte lezer (`Value.date_of_iso`), benoemde weigering met de positie
  in de zin, geen bodemzin; clingo gemeten en meegenomen. Rood-eerst per
  positie in `ref/test/test_ref.ml` (en `test_clingo.ml` omdat clingo
  een eigen lezing bleek te hebben); mutatieproef per positie met `cmp`.
  Afweging schemapatroon versus casusgetuige vastgelegd.
- (c) Sweep ref+clingo over alle golden- en roodcases vóór/na
  byte-identiek; alle veertien gates groen, elk los; pins 181/160
  onaangeroerd. `backends/drools/**`, `spec/**`, `corpus/**`,
  `scripts/**`, `frontends/**` onaangeraakt.

## Result:

Alle acceptatiepunten gehaald; alle veertien gates groen, elk los
gedraaid. Fix en getuigen zitten in één commit tussen claim en
afsluiting (`ref: de geldigheidspoort leest datums in plaats van
strings`).

**Inventaris.** Stringvergelijking van een datum stond op precies TWEE
plekken, beide de geldigheidspoort: `ref/lib/eval.ml` `applies` en
clingo's eigen spiegel `backends/clingo/lib/translate.ml` `applies`
(`String.compare` op asOfDate en beide grenzen). Clingo had dus WEL een
eigen lezing; de casemodule deelt hij wel. De caselader nam `asOfDate`
als opake string zonder lezing. Alle andere datumlezingen lopen al door
de strikte lezer: in ref `Rekendatum` (eval), het temporele totaal,
`Periode_conditie`, `parameterset_check` (gedeeld met clingo) — alle
`Value.date_of_iso` op `as_of`; clingo's `Rekendatum`, `dk_seen_days`
en zijn periodeconditie idem; in de caselader `date`-waarden
(`date_of_iso`), `dateTime`-waarden (`read_iso_datetime`) en de
periodegrenzen van attribuut- en kenmerktijdlijnen (`date_of_iso`).
`World` en `Timeline` rekenen op dagnummers en lezen geen lexeem. De
verifier (`spec/verifier`, buiten deze kaart) vergelijkt grenzen nog als
string (WF-021-overlap) — daar maakt WF-101 dat correct.

**Meting vooraf** (golden `jutterswet-vensterheffing`, één positie
vervangen; case1 = asOfDate 2026-06-01 pint vensterheffing 125 +
overgangsheffing leeg, case2 = 2025-06-01 pint leeg + 140):

- asOfDate `01-06-2026` op case2 — ref `oracle: PASS (3 expectations)`,
  clingo `clingo: PASS (3 expectations)`: beide antwoorden 140 voor
  overgangsheffing, waar de datum 2026-06-01 leeg vraagt. Drools exec en
  DRL: `ERROR IllegalArgumentException: malformed date '01-06-2026' on
  'asOfDate': the uchar-case-v0 lexeme is yyyy-mm-dd — fixed field
  widths, digits only, no sign and no extended year`.
- asOfDate `2026-13-01` (het schemapatroon laat het DOOR) — ref
  `MISMATCH: M1.overgangsheffing = leeg, expected 140`, clingo
  `MISMATCH: M1.overgangsheffing is underived (leeg), expected 140`:
  geen melding, de string sorteert na elke 2026-grens. Drools beide
  takken `ERROR … malformed date '2026-13-01' on 'asOfDate': the field
  widths are right but the calendar is not …`.
- `vanaf` `01-01-2026` — case1 ref/clingo PASS (toeval), case2 ref
  `MISMATCH: M1.vensterheffing = 125, expected leeg`, clingo
  `MISMATCH: M1.vensterheffing = [125], expected leeg`: het venster
  opende. Drools beide takken `ERROR IllegalArgumentException: malformed
  validity date '01-01-2026' on 'vensterheffing in
  2026.geldigheid.vanaf': the regelspraak-ir-v0 lexeme is yyyy-mm-dd …`.
- `tot` `31-12-2025` (overgangsregel) — case1 ref `MISMATCH:
  M1.overgangsheffing = 140, expected leeg`, clingo idem `= [140]`;
  case2 PASS (toeval). Drools beide takken `ERROR … malformed validity
  date '31-12-2025' on 'overgangsheffing tot en met
  2025.geldigheid.tot' …`.

**De fix, en waarom deze.** `Eval.validity_day ~what` leest een lexeem
door `Value.date_of_iso` en weigert met `validity gate: <positie>
"<lexeem>" is not the ISO date lexeme yyyy-mm-dd (<reden van de
lezer>)`; `Eval.window_admits ~as_of` leest alle drie de posities
daarmee en vergelijkt DAGNUMMERS; `applies` roept hem aan, en clingo's
`applies` is nu `Eval.window_admits ~as_of:ctx.case.as_of g`. Parseren
naar een dagnummer in plaats van valideren-en-dan-stringvergelijken:
even groot, maar dan is de vergeleken waarde de gelezen waarde en kan
een latere wijziging die twee niet uit elkaar trekken. Beide grenzen
worden gelezen vóór de vergelijking, dus een misvormde `tot` weigert
ook als `vanaf` de datum al uitsluit (gepind). Clingo roept de ref-poort
aan in plaats van haar opnieuw te spiegelen: de poort wordt bij hem op
vertaaltijd beslist (per case een constante), zijn spiegel was
beslissing-voor-beslissing gelijk en droeg hetzelfde gat; één lezer
betekent één weigerzin. De no-share-regel geldt de productieruntime,
niet ref↔clingo (clingo deelt al casemodule en `parameterset_check`).
Daarnaast weigert de caselader `asOfDate` bij het laden:
`asOfDate "<lexeem>" is not the uchar-case-v0 date lexeme yyyy-mm-dd
(<reden>)` als `CASE ERROR` (fase: laden, exit 2), zodat geen lezer
verderop een niet-datum ziet; het veld blijft een string (kleinste
wijziging, alle afnemers lezen hem al via `date_of_iso`). Geen van de
nieuwe zinnen staat in gate 8's bodemzinlijst; ze noemen positie en
lexeem.

**Na** (zelfde fixtures): asOfDate — ref en clingo `CASE ERROR:
asOfDate "01-06-2026" is not the uchar-case-v0 date lexeme yyyy-mm-dd
(malformed date lexeme "01-06-2026" (expected ISO yyyy-mm-dd))` rc 2;
`2026-13-01` idem met `(month out of range in date lexeme
"2026-13-01")`. vanaf — ref `EVALUATION FAULT: validity gate:
geldigheid.vanaf "01-01-2026" is not the ISO date lexeme yyyy-mm-dd
(malformed date lexeme "01-01-2026" (expected ISO yyyy-mm-dd))`, clingo
hetzelfde als `TRANSLATION FAULT`, op beide cases. tot — idem met
`geldigheid.tot "31-12-2025"`. Drools onaangeraakt (zijn ERROR was al
de goede kant).

**Rood-eerst** (getuigen eerst geschreven, lib-wijziging gestasht):
`test_ref.exe`:
`FAIL WV-3.10-geldigheid-vanaf-dag-eerst-weigert (no fault at all)`,
`FAIL WV-3.10-geldigheid-vanaf-kalender-weigert (no fault at all)`,
`FAIL WV-3.10-geldigheid-tot-dag-eerst-weigert (no fault at all)`,
`FAIL WV-3.10-geldigheid-tot-kalender-weigert (no fault at all)`,
`FAIL WV-3.10-geldigheid-tot-weigert-ook-buiten-vanaf (no fault at
all)`, `FAIL WV-3.10-geldigheid-asofdate-dag-eerst-weigert (no fault at
all)`, `FAIL WV-3.10-caselader-weigert-dag-eerst-asofdate (loaded)`,
`FAIL WV-3.10-caselader-weigert-maand-13-asofdate (loaded)`, `8
failure(s)`; de vier controles (binnen, voor, tot-inclusief, na het
venster, en de canonieke caselading) groen. `test_clingo.exe`: `FAIL
geldigheid.vanaf dag-eerst: expected a loud fault, got a model`, `FAIL
geldigheid.tot dag-eerst: expected a loud fault, got a model`,
`test_clingo: 523 checks, 2 FAILURES`. Na de fix: `test_ref: all 801
worked examples pass`, `test_clingo: 523 checks, 0 failures`.

**Mutatieproeven** (elk één keten: mutant → bouw → suites → herstel →
bouw → `cmp identiek` → 801/523 groen):

- M1 caselader leest asOfDate niet meer: ref `FAIL
  WV-3.10-caselader-weigert-dag-eerst-asofdate (loaded)`, `FAIL
  …-maand-13-asofdate (loaded)`, `2 failure(s)`.
- M2 `vanaf` terug naar `String.compare`: ref `FAIL
  …-vanaf-dag-eerst-weigert`, `FAIL …-vanaf-kalender-weigert`, `2
  failure(s)`; clingo `FAIL geldigheid.vanaf dag-eerst …`, `1 FAILURES`.
- M3 `tot` terug naar `String.compare`: ref drie `tot`-pins, `3
  failure(s)`; clingo `FAIL geldigheid.tot dag-eerst …`, `1 FAILURES`.
- M4 asOfDate aan de poort stil op dag 0 bij een leesfout: ref `FAIL
  WV-3.10-geldigheid-asofdate-dag-eerst-weigert (no fault at all)`.
- M5 clingo's oude eigen stringspiegel terug: clingo beide
  geldigheid-pins, `523 checks, 2 FAILURES`; ref 801 groen.

(Een eerste M2/M3-poging bouwde niet — ongebruikte variabele — en draaide
dus de oude binaries; opnieuw gedaan met een bouwcontrole in de keten.)

**Schemapatroon of casusgetuige.** Het case-schema zet op `asOfDate`
`^[0-9]{4}-[0-9]{2}-[0-9]{2}$` en gate 4 valideert zonder
format-checker: dag-eerst `01-06-2026` weigert het, maar `2026-13-01`
en `2026-02-30` laat het DOOR — en de eerste gaf vóór deze kaart stil
een ander antwoord (hierboven). Het schema is voor asOfDate dus een
half hek (vorm, geen kalender). Een ROOD document kan die kalenderhelft
WEL pinnen: gate 4 laat de waarde door, en de laadarm van beide runners
matcht nu `CASE ERROR: asOfDate "2026-13-01" is not the uchar-case-v0
date lexeme`. Dat valt buiten deze kaart (`corpus/**`); voorgesteld als
vervolg (zie Open). De geldigheidsgrenzen kunnen GEEN casusgetuige
krijgen: gate 3 eist dat elk rood `ir.json` verifier-schoon is en
WF-101 weigert elke misvormde grens (vorm én kalender); ook het
IR-schemapatroon (gate 2) weigert de vormhelft. Die twee posities zijn
dus uitsluitend in de suites gepind (ref én clingo).

**Niets bewoog.** Sweep van ref- en clingo-runner over alle 638
golden- en roodcases, volledige uitvoer, vóór (oude lib) en na:
identiek na normalisatie van clingo's tijdelijke `.lp`-bestandsnamen
in grounding-meldingen (1986 regels). Gates 5/6 replayen 595 cases
exact.

**Gates** (elk los, na `eval $(opam env)`): 1 PASS; 2 PASS (278
bestanden, floor 192); 3 PASS (278 golden / 440 negatief); 4 PASS (638
cases); 5 PASS (ref-suite 801, 595 cases); 6 PASS (523 checks, 595
cases); 7 PASS (559 + 36 overlevers); 8 PASS (43 rood, 86 fragmenten,
geen bodemzin; Drools-been 9/34); 9 PASS (201 checks, 12 goldens); 10
PASS (409 tests, 70 rgs-goldens); 11 PASS (29/29/29); 12 PASS (played
181, DRL 160, JUnit 613 — serieel herdraaid omdat de eerste run parallel
liep met gate 8's jarbouw; beide PASS); 13 PASS (131 twists, 128
replays, 25 DRL-played, 0 overlevers); 14 PASS (130 checks, 2 goldens).

**Beschermd pad, voorstellen** (niet aangeraakt):

- `spec/IR-SPEC.md` ch. 4.1, alinea "What the rule makes true …": "the
  ch. 8 selection in all three opaque-string readers are correct **as
  written**, and none of them has to start parsing a bound" en de
  residuzin "still meets no fence in those three readers — of the four
  readers in the house only the Drools backend parses the lexeme" →
  sinds 2026-09-23 lezen de reference interpreter en de ASP-backend
  (één gedeelde poort) asOfDate en beide grenzen door de strikte
  datumlezer en weigeren een niet-datum met positie en lexeem; alleen de
  verifier houdt de grens als opake string (WF-021), daar gedekt door
  WF-101 zelf. Idem de "Three readers hold a bound as an opaque
  string"-zin (nu: de verifier; de executors deden dat tot 2026-09-23)
  en de WF-101-registerrij ("each hold a bound as an OPAQUE STRING …";
  "Residue … meets no fence in those three").
- `spec/IR-SPEC.md` ch. 3.10 (Geldigheid, "the rule that makes the
  raw-string comparison this document's consumers perform sound") →
  "… the verifier's raw-string comparison sound; the executors parse".
- `spec/ASP-TRANSLATION.md` (validity-gate-alinea): "(`applies`, the
  literal mirror of ref's `applies` …)" en "Window bounds compare as ISO
  strings, both ends inclusive, exactly as ref compares them" → de
  vertaler roept ref's `Eval.window_admits` aan; asOfDate en grenzen
  worden als dagnummer vergeleken, beide grenzen inclusief, en een
  niet-datum faalt met `validity gate: geldigheid.<vanaf|tot> "<lexeem>"
  is not the ISO date lexeme`; een niet-datum-asOfDate weigert de
  gedeelde caselader al.

**Registerrijtekst `spec/CONSTRUCTS.md`, `Geldigheid`-rij (§2.3)** —
achter "…; the frontend's source reading moved to its own reader; no
document moved" toevoegen:
"; **the executors' validity gate reads dates 2026-09-23 (card
`ref-geldigheid-datumlezing`)**: `ref` and the ASP backend compared
`asOfDate` and both bounds as raw strings, so a non-canonical value took
part silently — on `jutterswet-vensterheffing` asOfDate `01-06-2026`
answered overgangsheffing 140 where leeg is right, `2026-13-01` (which
the case-schema pattern admits) closed the 2026 window, a day-first
`vanaf` opened it and a day-first `tot` kept the transition version on,
in both executors alike while Drools already raised an ERROR; the gate
(`Eval.window_admits`, which the ASP translator now calls instead of
mirroring) reads all three through the strict date reader, compares day
numbers and refuses a non-date naming position and lexeme (`validity
gate: geldigheid.vanaf "…" is not the ISO date lexeme`), and the shared
case loader refuses a non-date `asOfDate` at load; witnessed in both
suites with a mutation per position; the verifier alone still orders
bounds as strings, sound under WF-101; no replay moved (ref+clingo sweep
over 638 cases identical)".

**Open.**

- Rood-getuige voor de kalenderhelft van asOfDate (`2026-13-01` of
  `2026-02-30`): passeert gate 4, weigert nu bij het laden in ref en
  clingo, ERROR in Drools. Een corpuskaart; eerst meten hoe gate 8's
  Drools-been een Drools-ERROR op de case telt.
- Faselabel van de overige caseladerdatums: een misvormde `date`-waarde
  in de case-input weigert luid maar als `EVALUATION FAULT: malformed
  date lexeme "10-03-2026" (expected ISO yyyy-mm-dd)` (clingo
  `TRANSLATION FAULT`), gemeten op `jutterswet-bergingsklok` case2 met
  één waarde dag-eerst: `date_of_iso`'s `Value.Fault` ontsnapt de lader
  zonder attribuut en zonder laadfase, en de `fail "malformed %s
  date"`-armen in `timeline_of_json` en de kenmerktijdlijnlezer zijn
  daardoor onbereikbaar. Geen stil antwoord, dus niet in deze kaart;
  kleine eigen kaart (ref-caselaag).
