# deling-door-leeg-onder-initialisatie: een initialisatie die niet geldt, deponeert geen deling door leeg

*Gevonden op 2026-10-08 tijdens de kaart `20261007-drools-logische-afleiding`
(stap 3, de getuige voor de initialisatie) en door de hoofdsessie vastgelegd
op verzoek van de eigenaar. Gemeten op uchar main `d2021534`, met de shaded jar
van `backends/drools` die gate 12 die ochtend uit main bouwde en met ref en
clingo uit `_build` van de hoofdcheckout. De proefbestanden stonden onder
`/tmp/waarneming-Gfsk/out/` (`ir.json`, `case1-geldt.json`,
`case2-invoer.json`); het document staat hieronder, zodat de meting te
herhalen is.*

## Aanleiding

Een initialisatie schrijft alleen als haar doel leeg is, en die blokkade leest
de invoer (`spec/RUNTIME-SEMANTICS.md` ch. 5.4). Ch. 7.3 en 7.7 lezen de
initialisatie als de laagste default, gerechtvaardigd als het doel leeg is in
de invoerfeiten, en sinds ch. 7.8 draagt ook een initialisatie alleen die
lezing ("initialisation alone and initialisation-beside-computation through
the desugaring"). Ch. 7.1 beperkt de deponeringen in een defaults-groep:
"only the deciding default's evaluation deposits on the §9.5 channel". Een
initialisatie waarvan het doel in de invoer gevuld is, beslist niet, en mag
dus ook geen deling door leeg deponeren (ch. 5.6).

Het document: objecttype Vondst met de attributen `opbrengst`, `deelnemers` en
`aandeel` (alle drie een geheel getal), en één regel:

    Regel standaardaandeel
        geldig altijd
            Het aandeel van een Vondst moet geïnitialiseerd worden op zijn
            opbrengst gedeeld door zijn deelnemers.

Case 1: V1 met opbrengst 100 en verder niets; verwacht `aandeel` leeg en
inconsistent onder `standaardaandeel` op V1. Case 2: V2 met opbrengst 100 en
`aandeel` 5 als invoer; verwacht `aandeel` 5 en consistent.

## Gemeten (2026-10-08)

| Uitvoerder | Case 1 (doel leeg) | Case 2 (doel gevuld in de invoer) |
|---|---|---|
| ref | `oracle: PASS (2 expectations)` | `oracle: PASS (2 expectations)` |
| clingo | `MISMATCH: expected inconsistent (an inconsistent fact from rule "standaardaandeel" on "V1"), but the model carries: ? × V1` | `MISMATCH: expected consistent, but the model carries 1 inconsistent fact(s)` |
| Drools, exec-modeltak | `PLAYED 1` | `FAIL case2-invoer.json: verwacht consistent, werkelijk inconsistent (standaardaandeel @ V2)` |
| Drools, DRL-teksttak | `PLAYED 1` | `PLAYED 1` |
| Drools, truth-maintenance-proeftak (branch `card/drools-logische-afleiding`) | `PLAYED 1` | `PLAYED 1` |

Twee executors deponeren dus onder een initialisatie die niet geldt: de
exec-modeltak en clingo. Clingo noemt bovendien bij case 1 de producent niet
(`?`). Geen golden in `corpus/golden` heeft een initialisatie met een deling
waarvan de deler leeg kan zijn naast invoer op haar doel; daarom zien de
gates 5, 6 en 12 het niet.

Vermoedelijke plaatsen, niet nagelezen: in de exec-modeltak de aparte
deponeerregel voor een deling door leeg (`delingCompanions`,
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/model/ExecModelTranspiler.java`
rond regel 7633), die de blokkade van de initialisatie niet lijkt te dragen; in
clingo de deponeerregel van een initialisatie zonder de bewaking die ch. 7.5
voor een speculatieve default noemt (`decides_n` in de body), en de naam van de
producent.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met deze vorm, eigen Jutterswet-casus, rechtenvrij, met
  `bron.rgs`, `ir.json` en cases voor beide gevallen hierboven. Overweeg een
  derde case met een voorwaardelijke initialisatie waarvan de voorwaarde niet
  geldt.
- (b) De exec-modeltak en clingo gerepareerd, rood eerst: ref ≡ clingo ≡
  Drools (beide takken) op de nieuwe golden, tegen absolute verwachtingen.
- (c) Nagaan of dezelfde lacune bestaat voor de andere producenten van het
  kanaal onder een initialisatie (de negatieve radicand van §6.6, een leeg
  startanker van ch. 3.2), en in de DRL-teksttak; wat zo gevonden wordt,
  krijgt een rij in de golden of een getuige.
- (d) Gates 5, 6 en 12 groen; in `scripts/check-drools.sh` gaan
  `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` elk één omhoog (beschermd pad:
  voorstel); alle gates van `CLAUDE.md` groen.

## Omvang

Klein. In de exec-modeltak vermoedelijk één bewaking op de deponeerregel, in
clingo één bewaking en de naam van de producent, en één golden.

May touch: `corpus/golden/<nieuwe golden>/`,
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/model/ExecModelTranspiler.java`
(en `DrlTranspiler.java` alleen als (c) daar iets vindt), `backends/clingo/**`,
getuigen onder `backends/drools/src/test/java` en `backends/clingo/test`,
`scripts/check-drools.sh` (de twee pins; voorstel), kaartverplaatsing.
Niet in deze kaart: de spec (de tekst beslist het al; vindt de bouwer een
tegenspraak, dan een voorstel), ref (geeft al het juiste antwoord) en de
truth-maintenance-proeftak.

## Raakvlakken

De kaart `20261007-drools-logische-afleiding` legt deze meting ook vast in
haar `Result:`. Haar getuige
`TmsInitialisatieWitnessTest.een_deling_door_leeg_deponeert_alleen_waar_de_initialisatie_geldt`
vraagt van de exec-modeltak alleen case 1; landt die kaart vóór deze, dan zet
deze kaart daar ook case 2 aan. Geen andere kaart raakt
`delingCompanions` of de deponeerregels van clingo.

Na het besluit van 2026-10-08 (PLAN §9) vervangt `20261008-tms-omschakeling`
de exec-modeltak door de verlaging met truth maintenance. Deze kaart is klein
en kan daarvóór landen zoals ze staat. Landt ze erna, dan vervalt (b) voor de
exec-modeltak, want de nieuwe verlaging geeft hier al het goede antwoord.
