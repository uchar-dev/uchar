# p3-drools-verbindenderol-verdeling: de verdeling leest haar verbindende rol

V1-preconditie P3 (spec/proposals/V1-WIREMIGRATIE.md §2.2 en §7,
blokkeert B12). `ResolvedVerdeling.verbindendeRol` is de ENIGE nog
her-afgeleide binding van de oorspronkelijke drie: de vouw-`bronRol`
en de objectcreatie-`verbindendeRol` worden al van de wire gelezen
(zijkanaal `Loaded#rolBindingen`; de DRL-tekst-tak leest het
vouwbereik sinds `drl-som-over-bronrol` ook van de wire). De loader
leidt de verdelingsrol vandaag af uit het typenpaar; die afleiding
vervangt deze kaart door wire-lezing.

## Acceptatie

- (a) Beide Drools-takken lezen `verbindendeRol` op de verdeling van
  de wire; de typenpaar-afleiding vervalt in de beslissende rol
  (kruiscontrole die hardop faalt bij verschil mag — kies en
  motiveer).
- (b) De leesproef rood-eerst: een IR-mutatieproef die de
  wire-rol twist en aantoont dat het gedrag (of een benoemde fout)
  de wire volgt waar de twist vóór de port genegeerd werd.
  Uitgevoerd en teruggedraaid met build-rc in één keten.
- (c) Gelijkheidsbewijs: de volledige corpussweep over beide takken
  speelt identiek; gate 12-pins bewegen NIET.
- (d) Alle veertien gates groen; wire en schema ongewijzigd.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten (P1, P2, unie-telling); kleinste van de drie
ports.

Result: (a) en (b) gehaald. (c) gehaald voor de gelijkheid, maar de
pins bewegen wel: de port laat één golden spelen. Daardoor staat
(d) op gate 12 rood zolang het voorgestelde pin-diff niet landt.
`scripts/` is beschermd pad en valt buiten "May touch"; het diff staat
hieronder en wacht op mandaat.

(a) gehaald. `FactModelGenerator.verdelingsjoin` is de ene afleiding
waarop beide takken de ontvangersjoin beslissen (exec `verdeling`,
DRL-tekst `verdeling`). De loader zet de wire-binding van de verdeling
in het zijkanaal `Loaded#rolBindingen`, net als die van de
objectcreatie. De join is de zuster van de gebonden ontvangersrol, dus
de rol die de bron speelt en daarmee het referentieveld op de
ontvanger. Bij een wederkerig feittype is dat de gebonden rol zelf.
Een binding die geen gedeclareerde rol noemt, wordt geweigerd als
`verdeling (verbindende rol onopgelost: ...)`, in beide takken met
dezelfde zin. Alleen een bindingloze verdeling, die WF-048 toelaat
zolang het paar uniek is, vraagt nog het typenpaar
(`verbindendeRol(doel, bron)`). Rest voor B12.

Gekozen: GEEN kruiscontrole, op grond van drie metingen. ref beslist
symbolisch: `run_verdeling` geeft `verbindende_rol` door aan `linked`,
en daar wint de binding zonder vergelijking; `connecting_role` loopt
alleen zonder binding. De verifier kruist alleen de vullers: WF-047
eist dat de binding oplost en dat de vullers ⊑-passen bij doel- en
brontype; WF-048 geldt alleen voor de bindingloze vorm. Niets eist dat
de binding gelijk is aan de typenpaar-afleiding. Op een herhaald paar
heeft de afleiding bovendien geen antwoord om tegen te kruisen. Daar
is `verbindendeRol(Berger, Berging)` null, en een kruiscontrole zou
precies het document weigeren dat ref speelt. Bewijs dat het gat
echt is: de twist naar `(bergingstoezicht, toezichthouder)` op
bergingstoezicht en de twist naar `(reservedeelname, reserveberger)`
zijn allebei verifier-PASS.

Meting vooraf. Leesplekken: één per tak, `model.verbindendeRol(doel,
bron)` bovenaan `verdeling()` (exec r. 6120, DRL-tekst r. 1123). De
join gaat van daaruit als parameter naar alle deelregels (aandeel,
rest, volgorde, Fout-companions), dus er waren geen verdere
afleidplekken. De wire-binding werd geladen noch gelezen. Dragers:
golden 14 verdelingen in 14 documenten, alle 14 met binding; rood 0;
negatief 24 in 23 documenten, 17 met binding; toka 12 in 7 documenten,
alle met binding. Afleiding (typenpaar) tegen wire (zuster van de
gebonden rol), gemeten met de echte loader en `gebondenRolpaar`: in
golden 13 gelijk en 1 verschil, `anchor/jutterswet-bergingstoezicht`
(afleiding null, wire `berging`); in toka 12 gelijk. De verschillen in
negatief zijn de getuigen zelf: WF-047 (bronrol gebonden, rol hangt
los), WF-034 (zonder bron- of doeltype, en `onbekend-ratoattribuut`,
waar het typenpaar geen unieke relatie geeft). Het herhaalde typenpaar
dat de kaart vroeg te bouwen, staat al in het corpus: bergingstoezicht
relateert Berging en Berger via `bergingsdeelname` en
`bergingstoezicht`. Vandaag wordt het niet gedragen. Beide takken
weigerden het als `onopgelost naar_rato`, terwijl ref en clingo het
spelen (gates 5 en 6).

(b) gehaald, rood eerst. `VerdelingsrolWitnessTest`, 8 rijen via de
replay-API in beide takken (`BeideTakken`):
- De afleidingsassertie: het typenpaar zwijgt, `verdelingsjoin` geeft
  `berging`.
- bergingstoezicht speelt.
- Met een derde relatie `reservedeelname` speelt de ongetwiste
  binding.
- De twist naar `reservedeelname` verplaatst het antwoord naar
  "verwacht 60, werkelijk leeg" / "verwacht 40, werkelijk 100". Dat
  zijn precies de MISMATCH-cellen van ref.
- De twist naar `toezichthouder` wordt geweigerd op de opslagrichting
  (de zuster 'bewaakte berging' is meervoudig). ref faalt daar op
  "no fixpoint".
- Op `jutterswet-berging` (uniek paar) wordt de binding op de bronrol
  `berging` geweigerd ("rol 'berger' op Berger geen referentieveld").
  De verifier geeft WF-047 FAIL, ref MISMATCH leeg.
- Een losse rol `bewaker` wordt geweigerd als "verbindende rol
  onopgelost". De verifier geeft WF-047 FAIL, ref EVALUATION FAULT.
- Zonder binding speelt het document op het typenpaar.

Matrix met de jar van main 788d355b tegen de jar met port, beide
takken. Vóór de port werden de vier herhaald-paar-rijen geweigerd als
`onopgelost naar_rato`, wat de binding ook zei. De bronrol- en de
losse twist speelden het gepinde antwoord, dus de twist bleef ongelezen.
Na de port volgt elke rij de wire. De leesproef is in één keten
uitgevoerd en teruggedraaid: stash van IrLoader en beide transpilers,
test, pop, test. Zonder port was test-rc=1 (8 rijen: 4 failures, 3
errors; alleen de bindingloze rij groen), met port test-rc=0 (8/8).

(c) gelijkheid gehaald, pins bewegen. De sweep liep over alle
goldens met casus en alle 41 rood-documenten, samen 271 documenten
en 542 volledige stdout-bestanden, beide takken, vóór en na. `diff -r`
toont precies één document: bergingstoezicht gaat van EXCLUDE naar
PLAYED 1, in exec en in DRL-tekst. De andere 270 documenten zijn
byte-identiek. Exec gaat van 179 naar 180, DRL-tekst van 156 naar 157.
Dat volgt uit de port zelf, niet uit een bijwerking: zolang een
herhaald paar in het corpus staat, kan wire-lezing het niet laten
liggen. De kaart nam aan dat de pins niet zouden bewegen; die aanname
klopt voor de gelijkheid over de rest, niet voor het herhaalde paar.

(d) twaalf van de veertien groen op de takbroncode, gate 12 rood zoals
ontworpen, en gate 1 rood op main. Wire, schema, corpus, frontends,
toka en scripts zijn ongewijzigd.

| gate | stand |
|---|---|
| 1 self-contained | PASS na commit dda98f66. Op main 788d355b FAIL: een P2-testcommentaar (58d32a6c, `BonenveldWitnessTest` r. 117) noemt de zone voluit; alleen het commentaar is herschreven |
| 2 ir-schema | PASS (273 bestanden, vloer 192) |
| 3 verifier | PASS (273 golden schoon, 431 negatief verworpen) |
| 4 case-schema | PASS (629 cases) |
| 5 oracle | PASS (775 voorbeelden, 588 cases) |
| 6 clingo | PASS (479 checks, 588 cases) |
| 7 noop | PASS (552 cases, 36 overlevers) |
| 8 rood | PASS (41 rood; Drools-been 9 met pin, 32 met reden) |
| 9 catala | PASS (201 checks, 12 goldens) |
| 10 regelspraak | PASS (380 tests, 70 goldens) |
| 11 toka-parity | PASS (29 cases, Drools 29 PLAYED) |
| 12 drools | FAIL met het script van main: "1 golden(s) changed channel: anchor/jutterswet-bergingstoezicht :: pinned EXCLUDE, measured PLAYED". PASS met het voorgestelde diff (tijdelijk toegepast, gedraaid, teruggezet): 180/157, vloeren 52/75, JUnit 572 |
| 13 reftype-derivation | PASS (131 twists over 27 goldens, 128 replays, 25 DRL) |
| 14 flogic | PASS (130 checks, 2 goldens) |

Voorgesteld diff voor `scripts/check-drools.sh` (beschermd pad,
maintainer): de regel `anchor/jutterswet-bergingstoezicht|EXCLUDE|...`
verdwijnt uit `EXPECTED_CHANNEL` en uit `DRL_CHANNEL`,
`EXPECTED_PLAYED` wordt 179 → 180 en `EXPECTED_DRL_PLAYED` 156 → 157.
Beide krijgen een redenblok in de stijl van de vorige verhogingen: het
herhaalde typenpaar, de typenpaar-afleiding die niets antwoordt, de
wire-lezing zoals in ref, en de sweep die verder niets veranderde.

Mandaten. `spec/proposals/V1-WIREMIGRATIE.md` (de §2.2-rij en de
P3-rij, met het doorhaalpatroon) is bijgewerkt onder het mandaat van
de kaartopdracht van de hoofdsessie, zoals bij P1 en P2. Het
testcommentaar in `BonenveldWitnessTest` is gewijzigd zonder mandaat
vooraf, maar met melding hier. Het is één zin commentaar, de test
verandert er niet door, en de wijziging is nodig voor gate 1.

Voorgestelde registerbijwerking (`spec/CONSTRUCTS.md`, hoofdsessie).
Rij 17 krijgt achter "the emitter produces it": "**Landed in
`backends/drools` (2026-09-22, kaart
`p3-drools-verbindenderol-verdeling`):** both Drools branches join the
recipients on the sibling of the bound recipient role
(`FactModelGenerator.verdelingsjoin`), no cross-check against the
type pair; a dangling binding is refused by name. The repeated-pair
golden `anchor/jutterswet-bergingstoezicht` plays in both branches
(180/157); the corpus sweep is otherwise byte-identical. Witness
`VerdelingsrolWitnessTest`. Residue for B12: the bindingless type-pair
arm." De knooprij `ResolvedVerdeling` krijgt: "recipient join read off
`verbindendeRol` by both Drools branches since 2026-09-22 (P3)". PLAN.md:
geen scopewijziging.
