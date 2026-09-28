# drools-exec-voorwaarde-genavigeerde-afleiding: een voorwaarde op een afgeleide waarde van een genavigeerd object vuurt nooit

*Vondst van een regelsetbouwer, 2026-09-28, bevestigd door de hoofdsessie op
uchar `056df376` via de distributie (`bin/regelspraak check|scenario|cases`)
en de eigen uitvoerders; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/k4/` (alleen lezen). Een stil fout antwoord: gate 12 heeft
geen golden van deze vorm.*

## Aanleiding

Een gewone regel (gelijkstelling of berekening) op een object, met een
voorwaarde die via een to-one rol een AFGELEIDE waarde van het genavigeerde
object leest, vuurt in de Drools-exectak nooit: het doel blijft leeg en de
dekking zegt `NIET_GERAAKT`, terwijl `check` exit 0 geeft en ref en clingo de
verwachting halen. Zonder de voorwaarde vuurt de regel; met dezelfde lezing
in de WAARDE van de regel ook; met een voorwaarde op een INVOERwaarde van het
genavigeerde object ook (meting van de bouwer). De vorm is gewoon in een
regelset: een periode of grond die iets van "zijn recht" leest nadat een
andere regel dat op het recht heeft afgeleid.

## Minimale reproductie

`056df376`, distributie; `scenario` speelt de exectak.

    Objecttype de Jutter (mv: Jutters) (bezield)
        de meldingsdatum Datum in dagen;
        de afgeleide datum Datum in dagen;
    Objecttype de Vondst (mv: Vondsten)
        de uitkomst Datum in dagen;
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten

    Regel afgeleid
        geldig altijd
            De afgeleide datum van een Jutter moet gesteld worden op de meldingsdatum van de Jutter.

    Regel uitkomst
        geldig altijd
            De uitkomst van een Vondst moet gesteld worden op de meldingsdatum van de jutter van de Vondst
            indien de afgeleide datum van de jutter van de Vondst gevuld is.

    Scenario navigatie
    Gegeven is de volgende situatie:
    een Jutter J
    - de meldingsdatum is 01-03-2026
    een Vondst V
    - de jutter is J
    Met als verwacht resultaat:
    - de afgeleide datum van J is 01-03-2026
    - de uitkomst van V is 01-03-2026

    nu: check exit 0; scenario exit 1, "afgeleid : GEVUURD, uitkomst : NIET_GERAAKT", uitkomst leeg;
        cases + ref: `oracle: PASS (2 expectations)`; clingo: `clingo: PASS (2 expectations)`;
        zonder de indien-regel: scenario PASS.

## Acceptatie

- (a) Meet de vormruimte vóór de fix: voorwaarde op een afgeleid attribuut
  van een to-one genavigeerd object (deze reproductie); op een afgeleid
  kenmerk; twee rollen diep; via een to-many rol (kwantor of vouw); de
  afleiding uit een defaultsgroep in plaats van een gewone regel; en de
  DRL-teksttak op dezelfde documenten. Per vorm: ref, clingo, exec, DRL.
- (b) De oorzaak in de exec-tak (vermoedelijk: de voorwaarde op het
  gejoinde object wordt niet opnieuw geëvalueerd wanneer de afgeleide
  waarde later binnenkomt, of de join leest een kopie) en de fix, zodat de
  regel vuurt zodra de afleiding er is; rood-eerst JUnit-getuige met deze
  reproductie.
- (c) Een golden (werknaam `jutterswet-meldingsketen`) met deze vorm en de
  gemeten varianten die de fix dekt, handgerekend, in ref ≡ clingo ≡
  Drools-exec; `EXPECTED_PLAYED` +1 in `scripts/check-drools.sh` als
  **voorstel**. Een vorm die na de fix nog niet speelt, staat luid in het
  kanaal (EXCLUDE met reden), nooit als stil fout antwoord.
- (d) Corpussweep: alle goldens houden hun uitkomst (gate 12 played-telling
  en kanaal), beans byte-gelijk waar de fix de beangeneratie niet raakt;
  alle zestien gates groen (scripts met `#!/usr/bin/env bash` onder bash).
- (e) Een regel in `backends/drools/README.md` of de plek waar de exectak
  zijn beperkingen noemt, als er een beperking overblijft.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: parallel met de kaart
drools-subtype-in-supertype-rol (zelfde backend, andere bestanden: die raakt
het beanmodel en de rollen, deze de regelevaluatie); de hoofdsessie landt
serieel en rebaset.
