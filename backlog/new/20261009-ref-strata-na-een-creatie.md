# ref-strata-na-een-creatie: ref laat na een creatie de lagere strata niet eerst tot rust komen

*Gevonden op 2026-10-09 door de bouwer van stap 1 van
`20261009-creatie-initialisatie-leest-groeiende-waarde` (bijvangst) en door de
hoofdsessie vastgelegd volgens de afspraak dat een bestaande bug een kaart
krijgt. Gemeten op uchar main `8beff67a` met ref, clingo en de verifier uit
`_build` van de hoofdcheckout en de shaded jar van `backends/drools` uit een
bevroren kopie van `54ba99f7` (de code is gelijk); de nieuwe lus van die kaart
(branch `card/creatie-initialisatie-leest-groeiende-waarde`, `f0d8c5fe`) geeft
hetzelfde. Het document wordt gemaakt door
`/tmp/lens-bugs/w1/bijvangst-strata.py`; de proefbestanden stonden onder
`/tmp/strata-bug-gXRm/uit/`.*

## Aanleiding

Ch. 6.3 van `spec/RUNTIME-SEMANTICS.md`: de vormen van een lager stratum komen
tot hun fixpunt voordat een vorm van een hoger stratum vuurt. Ref draait voor
stratum k alle vormen van stratum ≤ k samen in één pas in documentvolgorde
(`ref/lib/eval.ml`, de strata-lus rond regel 3997–4004 op main; op de branch
van W1 de generatielus). Maakt een creatie van stratum k een nieuw object, dan
rekent een vorm van stratum 1 op dat object in dezelfde pas, vóór een vorm van
stratum 0 er zijn waarde heeft geschreven. Leest die vorm een voorwaarde die op
afwezigheid waar is, dan schrijft hij op een tussenstand, en ref trekt niets
in.

Model: objecttype Haven met `y` (geheel getal) en de kenmerken `open` en
`bewaakt`; objecttype Post met `a` (geheel getal) en het kenmerk
`gemarkeerd`; feittype havenpost: één haven (Haven) heeft één post (Post).

    Regel gemarkeerde post: Een Post is gemarkeerd indien zijn a ongelijk is
        aan 5.
    Regel y van een haven: De y van een Haven moet gesteld worden op 1.
    Regel open haven: Een Haven is open indien zijn y groter is dan 0.
    Regel a van een post: De a van een Post moet gesteld worden op 5.
    Regel bewaakte haven: Een Haven is bewaakt indien zijn post gemarkeerd is.
    Objectcreatie post voor een open haven: een Haven die open is, krijgt een
        Post (over havenpost, zonder initialisaties).

Strata: `y` en `a` 0, `open` en `gemarkeerd` 1 (een kenmerktoekenning leest
strikt), de creatie 2, `bewaakt` 2. Case: H1 zonder invoer. Verwacht: H1 open,
één Post met a = 5, die niet gemarkeerd is, dus H1 niet bewaakt.

## Gemeten (2026-10-09)

| Uitvoerder | gemarkeerde post eerst (zoals hierboven) | a van een post eerst |
|---|---|---|
| verifier | `PASS` | `PASS` |
| ref (main en de branch van W1) | `MISMATCH: H1 carries characteristic "bewaakt", expected absent` | `PASS` |
| clingo | `PASS` | `PASS` |
| Drools, exec-modeltak en DRL-teksttak | `PLAYED 1` | `PLAYED 1` |
| Drools, truth-maintenance-tak | weigert (een objectcreatie zonder `connectingRole`, waarvan de terugkoppeling uit het typenpaar volgt) | idem |

Met de lezer eerst leest "gemarkeerde post" op de nieuwe Post a nog als leeg;
"leeg ongelijk aan 5" is waar (ch. 1.2), dus de Post wordt gemarkeerd en H1
bewaakt. Geen golden heeft deze vorm; daarom zien de gates het niet. Het is niet
de restpost "conditions on attribute-writing forms read positively" van ch.
6.1: daar staan lezer en schrijver in één stratum, hier ordent de stratificatie
ze al, en ref houdt zich er niet aan.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met deze vorm, de lezer eerst, eigen Jutterswet-casus,
  rechtenvrij, met `bron.rgs`, `ir.json` en de case hierboven; ref, clingo en de
  Drools-takken die de vorm dragen spelen hem. Voorbeelden in `ref/test/` voor
  beide volgordes.
- (b) Ref laat na elke ronde van creaties de vormen die niets maken stratum voor
  stratum tot rust komen (0, 1, …, k), rood eerst. Geen bestaande verwachting
  in `corpus/golden` of `corpus/rood` verandert.
- (c) Gates 5, 6, 8 en 12 groen (in `scripts/check-drools.sh` gaan de pins
  `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` elk één omhoog; voorstel); de
  corpusgetuige van de tak met truth maintenance noemt haar weigering; alle gates
  van `CLAUDE.md` groen.

## Omvang

Klein. Eén plaats in de evaluatielus van ref en één golden.

May touch: `ref/lib/eval.ml`, `ref/test/`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (pins; voorstel), kaartverplaatsing. Niet: clingo en
de Drools-backend (geven al het goede antwoord of weigeren), de spec (ch. 6.3
zegt het al).

## Raakvlakken

Bouwt op de generatielus van `20261009-creatie-initialisatie-leest-groeiende-waarde`
en het budget van `20261009-creatiebudget-ref-en-clingo`; serieel, daarna.
