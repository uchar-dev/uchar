# p1-drools-navigatiebindingen: de loader leest de navigatiebindingen van de wire

V1-preconditie P1 (spec/proposals/V1-WIREMIGRATIE.md §2.2 en §7,
blokkeert B11). `backends/drools`' `IrLoader` NEGEERT vandaag
`AttribuutRef.navigatieBindingen` en `Kenmerk.navigatieBinding` en
leidt de navigatie her-af uit de v0-velden `navigatieRol`/
`navigatiePad` — precies de velden die bij B11 van de wire verdwijnen.
Zonder deze port is B11 een amputatie. Het bestaande zijkanaal-patroon
(`Loaded#rolBindingen`, dat de vouw-`bronRol` en de
objectcreatie-`verbindendeRol` al draagt) is de gebaande weg; de
recordtypen zijn gedeeld met de frontend (build-helper), dus de velden
zijn er al.

## Acceptatie

- (a) Beide Drools-takken lezen de navigatiebinding van de wire; de
  her-afleiding uit `navigatieRol`/`navigatiePad` vervalt in de
  beslissende rol (mag hooguit als kruiscontrole blijven die hardop
  faalt bij verschil — kies en motiveer).
- (b) De leesproef rood-eerst: een IR-mutatieproef die de
  wire-binding twist en aantoont dat het GEDRAG (of een benoemde
  fout) de wire volgt — vóór de port wordt dezelfde twist genegeerd.
  Uitgevoerd en teruggedraaid met build-rc in één keten.
- (c) Gelijkheidsbewijs: de volledige corpussweep over beide takken
  speelt identiek; gate 12-pins (EXPECTED_PLAYED /
  EXPECTED_DRL_PLAYED) bewegen NIET.
- (d) Alle veertien gates groen; wire en schema ongewijzigd (dit is
  een consumer-port, geen wire-wijziging).

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten (P2, P3, unie-telling).

Result: (a) gehaald. `IrLoader` leest `AttribuutRef.navigatieBindingen`
en `Kenmerk.navigatieBinding` in een nieuw zijkanaal
`Loaded#navigatieBindingen` (identiteit-gesleuteld, zoals `bronPaden`),
`CaseRunner` geeft het aan beide takken, en
`FactModelGenerator.navigatiewandeling` is de ene afleiding waarop beide
beslissen: exec in `navReader`, `navKetenReader`, `navReactiviteit`,
`navComparisonRole` en de drie kenmerkplekken; DRL-tekst in `naviRol`,
`naviResolve` (de keten, met het hoppad nu in `NaviRef.pad`), de
reactiviteits- en ketenwaardelezing en dezelfde drie kenmerkplekken.
Gekozen: GEEN kruiscontrole. WF-053 eist in gate 3 al dat elke
geschreven stap de gebonden rol aanwijst, de geschreven velden
verdwijnen bij B11, en een controle hier zou de binding alleen een veto
geven. Past een binding niet op het onderwerp (onbekende rol, de
wandeling begint niet bij het onderwerp, het eindpunt past niet), dan
weigert de tak met zijn bestaande reden voor een onopgeloste navigatie.
Terugvallen op de naam doet hij dan niet. Alleen een BINDINGLOZE
verwijzing, de vorm die WF-053 ook toelaat, wordt nog uit de geschreven
stap afgeleid. Een lege lijst en een binding op een niet-navigerende
kenmerkcheck weigert de loader met naam. Rest voor B11, gemeten: de
vormpredikaten (eigen of genavigeerd, keten of enkele stap) lezen nog
`navigatieRol`/`navigatiePad`, en daar is een `navigatiePad` van lengte
1 een KETEN en geen enkele stap (vier goldens, één rood document).

Meting vooraf: dragers op de wire (AttribuutRef / Kenmerk). golden: 22
verwijzingen in 14 documenten (10 enkele stap op naam, 11 keten
waarvan 9 met pad van lengte 1, 1 op paar), 5 kenmerkchecks in 4
documenten. Bindingloos met geschreven stap: 2 verwijzingen en 3
kenmerkchecks. rood: 3 verwijzingen in 2 documenten, 1 kenmerkcheck in 1 document.
negatief: 22 + 9. toka: 8 + 4. Tijdelijke kruiscontrole (niet
gecommit) over alle vier zones, beide takken: 125 vergeleken
resoluties in de goldens, nul verschillen. Het ENIGE verschil zat in
`corpus/rood/ambiguous-case-role-key`: de geschreven keten `ligger` is
op naam dubbelzinnig (pontonligging en vlotligging), de her-afleiding
gaf niets, de wire gaf `pontonligging.ligger`, net als ref.

(b) gehaald, rood eerst. Zeven varianten van
`jutterswet-dubbelverbinding` (vier feittypen over Schuit/Haven),
elke twist wijst de vlagprijs-lezing naar `winterberging.winterhaven`.
Jar van main 10b1d74a (voor de port): keten-, naam- en kenmerktwist
PLAYED 4 in beide takken (twist genegeerd), terwijl ref de twist volgt
(`MISMATCH: S1.vlagprijs = leeg, expected 40`). De stap op paar
(verifier-schoon, alleen de wire noemt de relatie) werd door beide
takken geweigerd, terwijl ref hem speelt. Na de port volgen beide
takken elke twist met dezelfde uitkomst als ref, en de paarvariant
speelt. Pin: `NavigatiebindingWitnessTest`, 7 rijen via de replay-API
in beide takken. Uitgevoerd en teruggedraaid in één keten (stash van
de port, build, test, pop, build, test): zonder port test-rc=1 met 6
failures en 1 error (7/7 rood), met port test-rc=0 en 7/7 groen.

(c) gehaald. De volledige sweep over 231 goldens geeft per golden
byte-identieke uitkomstregels in beide takken, voor en na (exec 179,
DRL-tekst 156). Gate 12 meldt 179/156, dus de pins bewegen niet.

(d) gehaald. Alle veertien gates zijn groen. `spec/schema`, `corpus`,
de frontends, `toka` en `scripts` zijn niet gewijzigd.

Bijvangst: door de port bereikt de Drools-tak in
`rood/ambiguous-case-role-key` nu de rolsleutel van de casus. Daar
weigerde hij (EXCLUDE, categorie ongewijzigd) met een zin die een
gedeclareerd lid de schuld gaf. `CaseRunner.naamdelingsPoort` noemt nu
de werkelijke oorzaak: twee gelijknamige rollen om één veld. De
redentekst in `scripts/check-rood.sh` (regel 399) beschrijft nog de
oude weigerplek ("the transpiler SKIPs … waarde buiten scope").
Voorstel aan de maintainer: vervang die door "EXCLUDE: the case-input
arm refuses at the role key `ligger` on the bean-field limit — two
same-named roles contend for one field on Schuit. Same phase as ref's
and clingo's load fault, different reason: they refuse the ambiguity
itself, this backend a field it cannot carry". De gate toetst alleen
het categoriewoord en blijft dus groen.

Voorgestelde registerbijwerking (spec/CONSTRUCTS.md, hoofdsessie).
Rij 4: in "Measured authority relation (2026-09-19, v1-inventory)"
wordt "three symbolic extensions are ignored" één (de `verbindendeRol`
van de verdeling, P3). Daarachter: "**Landed (2026-09-22, kaart
p1-drools-navigatiebindingen):** both Drools branches decide a bound
navigation by `navigatieBindingen`/`navigatieBinding`
(`FactModelGenerator.navigatiewandeling`, side channel
`Loaded#navigatieBindingen`), never falling back to the written name;
only a bindingless reference is derived from it. Corpus sweep
byte-identical in both branches (179/156); witness
`NavigatiebindingWitnessTest`. Residue for B11: the shape predicates
still read `navigatieRol`/`navigatiePad`." Rij `AttribuutRef` en rij
`Kenmerk` krijgen elk: "executed by both Drools branches off the
binding since 2026-09-22 (P1)". PLAN.md: geen scopewijziging.

Fixronde (lens, na rebase op main 3573d18a). B1: de eigen-lezingsarmen
beslisten op `navigatieRol() == null` alleen, en een gebonden lezing
op paar voldoet daar ook aan. Bij een gelijknamig subjectattribuut las
de exec-tak dat attribuut. Nagebouwde probe: dubbelverbinding plus
`Schuit.kadegeld` en de voorwaarde `kadegeld@Haven > 30` gebonden aan
vlagvoering.vlaghaven. verifier PASS, ref PASS, DRL PLAYED 4, exec FAIL
"attribuut vlagprijs: verwacht 40, werkelijk leeg". Fix: één predicaat
`ongenavigeerd(a)` (geen geschreven stap EN geen binding) per tak. Het
vervangt de 15 (DRL) en 30 (exec) losse `== null`-guards, en ook de
`!= null`-armen, zodat de gebonden lezing de genavigeerde arm krijgt.
De laatste plek die nog alleen op `navigatieRol` afmat, de meldtekst
van `operandAs` bij een matrixweigering, gebruikt sinds de slotronde
ook `ongenavigeerd` (de methode is daarvoor niet-statisch gemaakt).
Er blijft geen uitzondering over. Na de fix speelt de probe in beide
takken. A3: het eindpunt wordt nu getoetst als vuller ⊑ eindpunt, de
richting van WF-053 en ref. Gemeten: van 55 gebonden verwijzingen over
de vier zones wijkt de vuller alleen af in twee negatief-documenten
(`wf053-chain-near-side-mismatch`, `wf053-dangling-navigation-binding`),
dus op golden, rood en toka is dit gedragsneutraal. De getuige toont
dat een supertype-eindpunt in de WANDELING wordt geaccepteerd. De
keten speelt end-to-end. Enkele stap en paar met een
supertype-eindpunt weigeren nog stroomafwaarts op het beanveld: een
eerlijke weigering, geen fout antwoord. Rest voor B11:
`heeftReferentieVeld` en `naarVeelRol` toetsen daar op het eindpunt
(`a.objecttype()`) in plaats van op het door de wandeling bereikte
type, en de weigerreden legt de schuld daardoor bij het beanmodel. A4: een misvormde
hop (null, geen RolBinding, `feittype` of `rol` ontbreekt of is geen
tekst) en een lege of null-lijst weigert de loader bij naam, vóór de
niet-navigerend-toets. A5: de rij voor de niet-passende binding pint
per tak de reden ("regel (waarde buiten scope)" en "TODO navigatie:
…"). A2: het voorstel voor check-rood.sh is hierboven herschreven.
Getuige nu 10 rijen. Zonder de fix, met de getuige: test-rc=1, 3 rood
(B1, A3, A4). Met de fix: 10/10. Sweep over 232 goldens byte-identiek
met een jar van main 3573d18a (exec 179, DRL 156).
