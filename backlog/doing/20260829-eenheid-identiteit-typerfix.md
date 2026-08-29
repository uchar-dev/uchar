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
