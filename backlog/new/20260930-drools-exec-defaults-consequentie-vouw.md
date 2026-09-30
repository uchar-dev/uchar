# drools-exec-defaults-consequentie-vouw: een vouw als consequentie van een default speelt niet in de exec-tak

*Vondst van een regelsetbouwer, 2026-09-30 (skelet-vervolg: de laatste brug
K1 die bleef), bevestigd door de hoofdsessie op uchar `b0aab696` via de
distributie (`bin/regelspraak check|scenario|cases`) en de eigen
uitvoerders; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/defaults-consequentie/` (bron.rgs, bron-gewoon.rgs,
scenario.rgs, cases/; alleen lezen). Luid (exit 3), geen fout antwoord; wel
het laatste gat dat een regelset dwingt tot een hulplid voor een datum die
een `Afwijking van` of de opgave-markering boven zich heeft. Op verzoek van
de eigenaar geschreven (2026-09-30), tweeling van
drools-exec-defaults-justificatie-vouw.*

## Aanleiding

De kaart drools-exec-defaults-justificatie-vouw liet de rechtvaardiging
van een default tellen en ver lezen; de consequentie van een default bleef
daarbuiten. Zodra de basisregel van een `Afwijking van`-boom (of van de
groep die `(mag opgegeven worden)` genereert) haar waarde als vouw over een
rol berekent — "de laatste van de bergdag van alle vondsten van de Berging"
— markeert de exec-tak de hele groep: "defaults (Berging.sluitdag):
niet-reduceerbare consequentie (far-side/aggregatie): <default>". Dezelfde
vouw in een gewone regel speelt sinds drools-exec-datumvouw-over-rol (exit
0). Een regelset die een datum als de vroegste of laatste over haar gronden
afleidt en daar een uitzondering of een opgave boven zet, schrijft daarom
de vouw in een gewone regel op een hulplid en laat de default dat lid
lezen.

## Minimale reproductie

`b0aab696`, distributie `bin/regelspraak check|scenario|cases`; ref via
`_build/default/ref/bin/runner.exe`, clingo via
`_build/default/backends/clingo/bin/clingo_runner.exe`.

    Objecttype de Berging (mv: Bergingen)
        de gemelde sluitdag Datum in dagen;
        de sluitdag Datum in dagen;
    Objecttype de Vondst (mv: Vondsten)
        de bergdag Datum in dagen;
    Feittype BergingBetreftVondst
        de berging Berging
        de vondst (mv: vondsten) Vondst
    Eén berging betreft meerdere vondsten

    Regel sluitdag
        geldig altijd
            De sluitdag van een Berging moet berekend worden als de laatste van de bergdag van alle vondsten van de Berging.

    Afwijking van sluitdag: gemelde sluitdag gaat voor
        geldig altijd
            De sluitdag van een Berging moet gesteld worden op zijn gemelde sluitdag
            indien zijn gemelde sluitdag gevuld is.

    Scenario 1: Berging B met vondsten van 03-03-2026 en 09-03-2026, geen gemelde sluitdag;
    verwacht sluitdag 09-03-2026. Scenario 2: B met gemelde sluitdag 12-03-2026 en één vondst;
    verwacht 12-03-2026.

    R1  bron.rgs, zoals hierboven.
        nu: check "niet uitvoerbaar in uchar (exit 3): transpiler SKIP: defaults: defaults
        A:Berging.sluitdag — defaults (Berging.sluitdag): niet-reduceerbare consequentie
        (far-side/aggregatie): <default>" (resolved=0); scenario MARKER voor beide scenario's; ref
        `oracle: PASS (1 expectations)` en clingo `clingo: PASS (1 expectations)` voor beide cases.
    R2  bron-gewoon.rgs: alleen `Regel sluitdag`, zonder de afwijking.
        nu: check exit 0 zonder meldingen (de vouw in een gewone regel speelt).
    R3  dezelfde vorm met `(mag opgegeven worden)` op de sluitdag in plaats van de afwijking (meting
        van de regelsetbouwer, `/tmp/v2/probe/skelet-2/`, A5): dezelfde weigering op
        `berekende sluitdag`, met één vouw en met twee vouwen als operanden.

## Acceptatie

- (a) Meet de vormruimte in beide Drools-takken en in ref en clingo: de
  consequentie van de basisregel, van een uitzondering en van de gegenereerde
  groep van de markering als vouw over een rol (eerste, laatste, minimum,
  maximum, som, aantal), als vouw binnen een berekening (vouw plus een duur),
  en twee vouwen als operanden van één minimum of maximum; met een
  rechtvaardiging die telt of ver leest erbij (de vorm van
  drools-exec-defaults-justificatie-vouw); één en twee rollen ver.
- (b) De exec-tak speelt een default waarvan de consequentie een vouw over
  een rol is, met dezelfde lowering als de gewone regel (de vouw als eigen
  accumulate, de consequentie leest haar uitkomst) en met dezelfde uitkomst
  als ref en clingo, ook in de gegenereerde groep van de markering; wat niet
  meegaat blijft een benoemde weigering met de vorm in de zin; rood-eerst
  JUnit-getuige met R1 en R3.
- (c) Golden (werknaam `jutterswet-sluitdag-gemeld`), handgerekend, in ref ≡
  clingo ≡ Drools-exec; `EXPECTED_PLAYED` (230) en `EXPECTED_DRL_PLAYED`
  (191) in `scripts/check-drools.sh` als **voorstel**; de DRL-teksttak volgens
  haar kanaal (zij weigert defaultsgroepen).
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). `backends/drools/README.md`: de
  alinea over de rechtvaardiging noemt nu ook de consequentie;
  `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` krijgt de registertekst
  van de hoofdsessie.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: serieel met de andere
Drools-kaarten (zelfde transpiler); na de landing kan het laatste K1-lid van
de regelset weg.
