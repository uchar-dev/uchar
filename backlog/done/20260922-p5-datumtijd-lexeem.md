# p5-datumtijd-lexeem: WF-095 versmalt tot canoniek ISO

V1-preconditie P5 (spec/proposals/V1-WIREMIGRATIE.md §7, B22a) — de
goedkoopste stap op de lijst. WF-095 (2026-09-06) laat vandaag twee
spellingen toe voor het `DatumTijd`-lexeem: canoniek
`yyyy-mm-ddThh:mm:ss[.fff]` én de dag-eerst-bronsnippet, omdat de
RegelSpraak-emitter (`ExpressieBouwer`, de dedato-tak met
tijdcomponent) de snippet nog in `waarde` schrijft — één regel onder de
kale-datumtak die wél ISO schrijft. Gemeten (B22a): de snippetvorm
heeft VIJF corpusdragers, alle vijf in `corpus/negatief/wf095-*` — die
zijn er om afgewezen te worden; de enige golden met een
`DatumTijd`-literal (`jutterswet-meldstip`) draagt de canonieke vorm.
Nul goldenregeneratie.

## Acceptatie

- (a) Spec-first: de WF-095-tekst in IR-SPEC versmalt tot canoniek ISO
  (voorstel); de verifier-arm die de snippet toelaat vervalt; de vijf
  `wf095-*`-negatief-getuigen worden herschreven zodat elk één
  spellingsfout tegen de canonieke vorm pint (rood-eerst: elke getuige
  eerst zien falen tegen de oude, tolerante arm).
- (b) De emitterregel: de dedato-tak met tijdcomponent schrijft de
  canonieke ISO-vorm in `waarde` (de bronsnippet blijft in `bron`),
  symmetrisch aan de kale-datumtak. Rood-eerst een emittergetuige in de
  JUnit-suite — de emitterkant heeft vandaag nul goldengetuigen; weeg
  een kleine rgs-golden met `DatumTijd`-literal als blijvende getuige.
- (c) Mutatieproef op de verifier-arm (snippet weer toelaten ⇒ minstens
  één negatief-getuige valt stil ⇒ gate 3 rood), uitgevoerd en
  teruggedraaid met build-rc in één keten.
- (d) Alle veertien gates groen; geen versieklap, v0-schema ongewijzigd.

May touch: `spec/IR-SPEC.md` (**voorstel**), `spec/verifier/`
(**voorstel**), `corpus/negatief/wf095-*`, `frontends/regelspraak/**`,
`corpus/golden/**` (nieuwe getuige), kaartverplaatsing. NB: raakt de
frontend-emitter — serieel met andere emitterkaarten.

## Result:

Alle vier acceptatiepunten gehaald; alle veertien gates groen, elk los
gedraaid.

**Meetcorrectie op B22a.** De vijf `wf095-*`-negatiefs dragen de
snippetvorm NIET: elk pint al één eigen spellingsfout tegen de canonieke
vorm (zone-suffix, spatie voor `T`, fractie `.1`, ontbrekende seconden,
omringende tekst) en werd ook door de oude tweespellingsarm geweigerd.
Gescand over de corpuszones en `toka/`: nul WIRE-documenten met de
snippet in `waarde`. De bekende, bedoelde uitzondering zijn de
lezertolerantie-getuigen in de testsuites (`ref/test/test_ref.ml`,
`DatumTijdLexeemWitnessTest`, `CaseWireDatumTijdWitnessTest`,
`LiteralenTest`), die de snippet dragen omdat de lezers hem achter het
hek nog lezen. Herschrijven had dus niets te doen; wat ontbrak was een
getuige die onder de oude arm verifier-schoon was. Die is er nu:
`wf095-bronsnippet` (`15-03-2026 12:00:00.000` in `waarde`) — rood-eerst
gezien (`verifier: PASS`, rc 0, onder de oude arm). De NOTE's van
`spatie-scheider` en `omringende-tekst` spraken nog van twee toegelaten
spellingen en zijn bijgewerkt. Voorstel voor `V1-WIREMIGRATIE.md` B22a
(niet aangeraakt): "vijf corpusdragers, alle vijf in wf095-*" wordt "nul
corpusdragers; de vijf wf095-negatiefs pinnen elk een andere fout".

(a) IR-SPEC ch. 4.1 (WF-095-alinea, Literal-deltarij, WF-register-rij)
versmald tot één spelling — **voorstel**. Verifier-arm
`lexeme_datum_tijd` zonder snippettak, melding herschreven —
**voorstel**. Gate 3: 271 golden schoon, 431 negatief (was 430).

(b) `ExpressieBouwer`: de dedato-tak met tijd schrijft
`uuuu-MM-dd'T'HH:mm:ss.SSS` in `waarde`, snippet in `bron`; een
kalender-onmogelijk token (30-02) wordt een ERROR-diagnostic (emissie
stopt) in plaats van een lexeme dat WF-095 weigert — vroeger werd dat
stil geëmitteerd. JUnit rood-eerst gezien (`expected
<2026-03-15T12:00:00.000> but was <15-03-2026 12:00:00.000>`), plus een
getuige voor het foutpad (gate 10: 372 → 374 tests). Blijvende golden:
`jutterswet-sluitstip` (bron.rgs; drie bronspellingen van één tijdstip —
`dd. 1-7-2026`, `01-07-2026`, `1-07-2026` — in een schrijfpositie en
beide ordeningen, zeven gevallen waarvan V7 een maand-eerst-lezing zou
betrappen; handberekening in de NOTE). `jutterswet-meldstip` kon het
niet worden: zijn bronzinnen dragen `haar` (lensbesluit F11 van de
datumtijd-kaart) en de grammatica kent geen `haar`; zijn NOTE is
bijgewerkt.

(c) Mutatieproef: `checks.ml` teruggezet naar de tweespellingsarm ⇒ gate
3 rc 1, precies één melding (`wf095-bronsnippet passed the verifier`);
teruggedraaid, herbouw rc 0, `cmp` identiek — één Bash-keten.

(d) Gates: 1 PASS; 2 PASS (271 bestanden); 3 PASS (271/431); 4 PASS
(621 casussen); 5 PASS (ref 773 voorbeelden, 580 casussen); 6 PASS (479
checks, 580 casussen); 7 PASS (544 + 36 overlevers); 8 PASS (41 rood);
9 PASS (201 checks, 12 goldens); 10 PASS (374 tests, 69 rgs-goldens);
11 PASS (29/29/29); 12 PASS (played 178, DRL 155, JUnit 535); 13 PASS
(131 twists, 0 overlevers); 14 PASS (130 checks). Geen versieklap,
v0-schema ongewijzigd.

**Beschermd pad, voorstel:** `scripts/check-drools.sh`
`EXPECTED_PLAYED` 177 → 178 en `EXPECTED_DRL_PLAYED` 154 → 155, beide
+1 voor `jutterswet-sluitstip` (speelt in beide takken zonder
backendwijziging), met redenregels. Gate-2/3-tellingen +1 golden, gate
3 +1 negatief, gate 10 rgs-goldens 68 → 69 (alleen vloeren, geen
bijstelling nodig).

**Rest, benoemd:** `ref/lib/value.ml:116` zegt nog dat "a v0 emitter
still writes" de snippet; ref, clingo (via ref's casemodule) en de
Drools-lezer (`Literalen.parseDatumTijd`) lezen de snippet nog —
tolerantie achter het hek, in IR-SPEC zo benoemd. Versmallen van de
lezers is een eigen kaart.

**Voorgestelde CONSTRUCTS-rij-27-tekst** (vervangt de zin "unlike WF-084
the rule admits the snippet, because the emitter still writes it into
`waarde` and `bron` (canonicalisation is its own card)"): "until
2026-09-22 the rule admitted the snippet, because the emitter still
wrote it into `waarde` and `bron`; **narrowed 2026-09-22 (card
`p5-datumtijd-lexeem`)**: the emitter writes the ISO lexeme into `waarde`
and keeps the snippet in `bron`, symmetric with WF-084, and WF-095
admits the canonical ISO form alone — no document moved (the snippet had
zero carriers), new negatief `wf095-bronsnippet` (the one witness the
old arm admitted), emitter golden `jutterswet-sluitstip` (the first
`bron.rgs` golden with a `DatumTijd` literal, full square, gate-12 pins
178/155); the readers still read the snippet behind the fence".
