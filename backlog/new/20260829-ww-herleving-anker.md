# WW-herleving: het realisme-anker (eerste casus, inkomenspad)

Goal: de eerste WW-realisme-case van LADDER-EVOLUTIE §3.4 / Q14
(geratificeerd 2026-08-29) als handgeschreven anker:
`corpus/golden/anchor/ww-herleving-inkomen/` — het pad art. 20-1-c →
art. 21-1 eerste volzin (geen termijn). Eén genest kenmerk-ladder-doel
`recht op uitkering` (grondtoekenning art. 15 → do-not-attribute op de
inkomensgrens 20-1-c → herleving 21-1 bij vervallen omstandigheid →
do-not-attribute op de blokkade 21-2-a), plus gewone datumrekenregels
(inkomensgrens 0,875 × maandloon; `tijdsduur` eindiging→herleving; nieuwe
maxdatum = maxdatum + verstreken periode). Scalaire invoerfeiten:
maandloon, maandinkomen, eindigingsdatum, vervaldatum/aanvraagdatum,
maxdatum — het maandvariabele inkomen en de tijdlijn-lifecycle zijn
expliciet uitgesteld (§3.3). Drie case-bestanden met handberekende,
absolute verwachtingen: het basisgeval (herleving, nieuwe maxdatum), een
grens-niet-overschreden-variant (geen eindiging) en een
blokkade-variant (21-2-a: geen herleving). De artikelteksten (rechtenvrij,
art. 11 Auteurswet) staan geciteerd in een NOTE.md in de ankermap;
handboekmateriaal en verwijzingen naar privérepo's of machinepaden komen
in geen enkel artefact.

Acceptance: gates 1 t/m 8 groen met het nieuwe anker: schema-valid en
byte-roundtrip (gate 2), verifier-schoon (gate 3), case-schema (gate 4),
ref replay exact (gate 5), clingo exact (gate 6), noop faalt op alle drie
de cases (gate 7 — anders gemotiveerde survivor-entry), rood onaangeroerd
(gate 8); gate 12 sluit het anker luid uit via het bestaande
defaultLadders-weigerkanaal (geen scriptwijziging nodig); handberekening
van elke verwachting in de NOTE.md naast de artikelcitaten.

May touch: corpus/golden/anchor/ww-herleving-inkomen/ (nieuw), backlog/.

Out of scope: verdere WW-scenario's (detentie/termijn, AOW-onderlaag,
samenloop — latere kaarten); tijdlijn-lifecycle; frontend- of
backendwijzigingen; spec-tekst.
