# AANTAL_GEVULDE, kaart 3: de dialectzin in de RegelSpraak-frontend

Derde en laatste kaart onder het AANTAL_GEVULDE-ontwerp
(`backlog/done/20260919-aantal-gevulde-gemiddelde.md`, besluiten G1–G9 van
de maintainer, 2026-09-19). De wire, de verifierhekken en `ref` landden in
kaart 1 (`backlog/done/20260920-aantal-gevulde-wire.md`); clingo en beide
Drools-takken in kaart 2
(`backlog/done/20260920-aantal-gevulde-backends.md`). Wat rest is
**besluit G9**: de soort heeft in geen enkele frontend een producentroute,
en dat was opzet — de eerste twee kaarten zijn wire-first. Deze kaart geeft
haar er een.

Dit kaartje bestaat omdat de ontwerpkaart met kaart 2 naar `done/` ging en
daarmee de enige drager was van het voorstel hieronder; zonder deze kaart
zou het werk verweesd achterblijven in een afgesloten dossier.

Goal: een RegelSpraak-document kan de waardentelling SCHRIJVEN, de
frontend emitteert er de bestaande wire-vorm voor, en een corpusgolden
draagt een `bron.rgs` die byte-identiek heremitteert.

## Het voorstel, overgenomen uit de ontwerpkaart §4 (G9)

De woordgroep:

    het aantal gevulde waarden van <attribuut> van alle <rol> van de <onderwerp>

Langer dan `het aantal`, met het bestaande langste-match-precedent van
`het aantal dagen in`.

**De risico-aantekening hoort erbij en is de kern van de kaart.**
`het aantal gevulde …` alléén is NIET veilig: een officieel document mag
een attribuut of rol hebben wier naam met "gevulde" begint, en dan
verschuift maximal munch een bestaande zin. Het derde woord (`waarden`)
maakt dat onwaarschijnlijk maar niet onmogelijk. De frontend-kaart moet de
inertheid daarom METEN over het vlakke v2.3.0-corpus en het resultaat
noteren in `frontends/regelspraak/grammar/DIVERGENCES.md`, zoals de
`is een`-analyse dat voor `IS_EEN` deed — onder hetzelfde
inert-op-officiële-documenten-argument dat de `Uitzondering op`-uitbreiding
en de `is een`-uitbreiding van `spec/proposals/SUBTYPING.md` §9.2 gebruiken.
Meten, niet aannemen.

## De tweede taak, ook uit de ontwerpkaart

`VOUWSOORTEN` in `RuleChecker.java` gelijktrekken met de verifier-grens
`set_source_exempt` (`spec/verifier/lib/checks.ml`). Die twee grenzen horen
gelijk te blijven en lopen sinds kaart 1 uiteen: de verifier kent ZEVEN
waardevouwen sinds `AANTAL_GEVULDE` erbij kwam, de frontend-checker nog
niet.

## Acceptatie (bij oppakken aanscherpen)

- (a) Grammatica-uitbreiding plus de inertheidsmeting in
  `frontends/regelspraak/grammar/DIVERGENCES.md`.
- (b) Weigeringsfixtures per refusal, zoals de frontend die elders draagt.
- (c) Een `bron.rgs` op een van de goldens van kaart 2 — de voor de hand
  liggende is `corpus/golden/jutterswet-gevuldetelling` — die byte-identiek
  heremitteert. NB: die goldens zijn met de hand geschreven IR, dus de
  emissie moet naar de BESTAANDE `ir.json` toe, niet andersom.
- (d) `VOUWSOORTEN` ≡ `set_source_exempt`, met een getuige die uiteenlopen
  betrapt.
- (e) Poort 10: JUnit-vloer en rgs-goldenvloer bewegen vanzelf omhoog;
  vloeren hoeven niet opgehoogd te worden. Alle veertien poorten groen.

May touch: `frontends/regelspraak/**`, `corpus/golden/**` (alleen een
`bron.rgs` toevoegen), deze kaart.

Out of scope: `spec/**`, `ref/**`, `backends/**` (alle drie dragen de soort
al), `spec/CONSTRUCTS.md` en `spec/PLAN.md` (rijvoorstel in het `Result:`).
