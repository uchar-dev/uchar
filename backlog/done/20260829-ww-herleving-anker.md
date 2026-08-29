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

Result: corpus/golden/anchor/ww-herleving-inkomen/ geland — het eerste
WW-realisme-anker (LADDER-EVOLUTIE §3.4/Q14): één genest kenmerk-ladder
(grant art. 15 → einde 20-1-c → herleving 21-1 → blokkade 21-2-a, rose
tree, WF-050/051/075-schoon) + drie scalaire rekenregels (inkomensgrens
0,875×maandloon; tijdsduur; datum+duur), VIER cases die elk op een andere
sport beslissen (case4-einde toegevoegd op lensvondst: de kale eindiging
was onbediend), alle verwachtingen handberekend in NOTE.md naast de
verbatim wetsartikelen (BWBR0004045, toestand 2026-07-01). Gates 1-8
groen (362 cases ref≡clingo; alle vier cases weerleggen noop); gate 12
sluit het anker luid uit via het bestaande defaultLadders-kanaal. Lenzen
(2 opus): juridisch-rekenkundig — elke pin op papier nagerekend,
wetscitaten byte-getrouw, WF-boobytraps in /tmp door de verifier
verworpen; rechten/gates — eigen casusgetallen bevestigd (géén
dossiertraject), gates 5/6/2 reddenen aantoonbaar op geflipte
pin/input/indentatie. De door lens 1 gemelde eenmalige,
niet-reproduceerbare gate-5-mismatch op case3 is verklaard en ontkracht:
beide lenzen deelden parallel dezelfde worktree en lens 2's
boobytrap (b) — input nieuw maandloon 2300→2000 — stond op dat moment in
de boom; de waargenomen uitkomst (recht aanwezig) is op die gemuteerde
input de CORRECTE semantiek, met exact dezelfde faalregel als lens 2
rapporteert. Na koude rebuild: 13+ gate-runs en ~1500 replays
deterministisch groen. Proceslessen vastgelegd: muterende
boobytrap-lenzen nooit parallel met gate-draaiende lenzen in één
worktree. Verwerkte lens-MINORs: case4, de C/D=1-vereenvoudiging
geëxpliciteerd, de case2-uitleg mechanisme-getrouw, leeg-propagatiezin
symmetrisch. Bewust buiten scope (in NOTE): maandvariabel inkomen,
lifecycle-tijdlijn, termijn-paden (latere kaarten).
