# Eenheid-identiteit: de typerfix — Onbekend eruit, "geen eenheid" eerste-klas

Goal: maintainer-besluit 15c (2026-08-29), met het geratificeerde
principe uit de kwantiteitenrekening (ISO 80000; F# units of measure):
eenheden vormen een groep met **dimensieloos als identiteitselement "1"**
— "bewust geen eenheid" is eerste-klas kennis en absorbeert onder
vermenigvuldiging (1 × EUR = EUR); "eenheid onbekend" is géén waarde maar
een inferentie-mislukking → fail-fast error, nooit een wire-type. Concreet:
(1) de frontend-Typering typeert een product/quotiënt met een eenheidloze
operand met zijn echte type — Numeriek, de eenheid van de eenheid-dragende
operand, decimalen per de officiële typeringentabel (§6, source-verified
tegen de v2.3.0-arbiter) — en de
Onbekend("samengestelde eenheid")-degradatie voor deze hoek verdwijnt;
een genuinely samengestelde eenheid (EUR×EUR) die het Eenheid-model niet
kan spellen wordt een luide resolver-error (tenzij IR-SPEC ch. 2.5's opake
producteenheid de wire-vorm blijkt te dragen — bouwer beslist tegen de
spec en meldt expliciet); (2) clingo's numeric_ok-Binair-rekenarm
pensioneert (zijn enige bestaansreden was deze emitter-lacune), refs
tolerantie idem voor zover aanwezig; (3) de geraakte toka-slices
hergenereren door de eigen frontend en de gepinde ir.json's bewegen mee
(het f35d541/WF-081-precedent — gate-11-byte-identiteit blijft de eis, de
replaywaarden veranderen niet); (4) een publiek golden in de
stukopbrengst-vorm (basisbedrag + aantal × stukprijs), nu met correct
getypeerd product, handberekend, ref ≡ clingo en gate-12-classificatie
eerlijk gemeld; (5) rood-eerst-tests: de oude degradatie aantoonbaar weg,
de onspelbare samengestelde eenheid aantoonbaar luid.

Acceptance: alle twaalf gates groen; gates 10/11 byte-identiek op de
NIEUWE emissies (een emitterwijziging beweegt gepinde goldens mee —
expliciet getoond welke); geen Onbekend-getypeerde Binair meer op enige
wire in corpus of toka; de clingo-arm weg mét test; geen verwachting
verschoven (de typering verandert waarden niet). CONSTRUCTS-rij-27-notitie
en LADDER-EVOLUTIE-statusnoot (Q13/flip-punt verdampt): hoofdsessie
serieel bij de landing; toolchain re-vendor + herpin idem.

May touch: frontends/regelspraak/ (Typering, emitter, tests),
backends/clingo/ (numeric_ok-krimp + tests), ref/ (tolerantie-krimp
indien aanwezig), corpus/golden/ (nieuw golden + hergenereerde
rgs-goldens), toka/ (hergenereerde ir.json's + manifest, onder dit
kaartmandaat), backlog/.

Out of scope: de count-positie-Onbekend (tien publieke goldens —
geregistreerd vervolgpunt, lost op bij de v1-WF-003-promotie);
CONSTRUCTS/PLAN; de toolchain-checkout (re-vendor door de hoofdsessie).

Result: besluit 15c/16Y/17b volledig geland. Typering: MAAL-identiteit
onvoorwaardelijk in beide richtingen (arbiter Tabel 11 rij 4, decimalen =
som per Tabel 13; de literal/attribuut-splitsing en de 4-args
reken-overload gepensioneerd); GEDEELD eenheidloos/E en de onspelbare
rest-composieten (rate-factor/omgekeerde-noemer) zijn luide
resolver-errors — de bewuste, dimensioneel zuivere divergentie van de
onzuivere Tabel-14-rij-4-cel (17b), CONSTRUCTS-delta serieel bij de
landing. Clingo's numeric_ok-Onbekend-Binair-arm gepensioneerd
(rood-naar-groen bewezen); ref had geen tweeling (static_numeric is
vergelijking-only en draagt de count-positie — onaangeraakt). Toka-45
(16Y): tweede reden-dragende holdout naast toka-23 — de casus is
dimensioneel inconsistent zoals geschreven (eenheidloos Bedrag min
km-getypeerd product), door de identiteitstypering blootgelegd waar v0
haar maskeerde met de Onbekend-degradatie; scenario-dir en manifestregel
weg (30→29), casus-emissie hergenereerd zonder de regel (21→20, geen
andere regel hergetypeerd, geen replaywaarde bewogen), de .rgs-bestanden
in originele vorm, holdouts en JUnit-quirklijst falen-bij-veroudering.
Getuige jutterswet-stukopbrengst terug via bron.rgs (stuktal — de
aantal-functie-botsing vermeden), product getypeerd EUR, 190/112
handberekend, volle vierkant (ref ≡ clingo ≡ Drools PLAYED). Geen
Onbekend-getypeerde rekenkunde meer op enige committed wire; de
count-positie (tien goldens + toka-44-datum-MIN) expliciet buiten scope
tot de v1-WF-003-promotie. Drie rood-eerst-JUnit-getuigen
(EenheidIdentiteitTest) aantoonbaar rood op de oude frontend. Alle
twaalf gates groen (gate 11: 29 slices, 45 rgs byte-identiek, nul stille
divergenties; gate 12: played 71). Lens-MINORs: de CONSTRUCTS-delta's
(17b/17c) en de CLAUDE.md-gate-11-zin zijn seriële landingsplicht
(gedaan bij deze landing); de getuige is integer-only en discrimineert
som-vs-max-decimalen niet op de wire (de JUnit- en probegetuigen doen
dat wél — geregistreerd jacht-punt); één rapportzin over de reststring
was te breed (wire-claim klopt). Toolchain-nawerk (re-vendor + herpin +
scenariosuite-holdout toka-45): hoofdsessie, direct hierna.
