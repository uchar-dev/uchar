# aantal-bronarm-en-drools-lader: een telling met bereik en een argument dat geen bron is

Bijvangst van de kaart aantal-bronargument-welgevormd (2026-09-25, open
punten 1 en 2 en lensvondsten A15/A17.2). Sinds die kaart weigert WF-037 een
telling met `elementType` zonder argument en met een `ListLiteral`; de
tussenvormen niet: een `count`/`filledCount` met `elementType`, een bereik
(`sourceRole` of `sourcePath`) én een argument dat geen instantiebron is (een
`Literal`, een `Binary`, een `ParameterReference`, …) krijgt vandaag alleen
de bindingszin van WF-047, `ref` de bodemzin `unsupported instance-fold
source shape` en clingo een bodemzin uit de lijst van 36. Gemeten: de
Drools-lader voert zulke documenten uit — `count` + `elementType` +
`sourceRole` zonder argument, met een `Literal` of met een `Binary` speelt
in beide takken (PLAYED 1; hij telt het bereik en negeert het argument), en
`filledCount` zonder argument speelt in de DRL-tak. Geen gate speelt
ongeldige documenten, dus het is geen kloof in het vierkant; het is wel een
uitvoerder die een ill-formed document stil uitvoert.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een bronarm onder WF-037 (**voorstel**): een telling met `elementType`
  draagt als enig argument een instantiebron (de vormen van WF-096/WF-102);
  elke andere vorm weigert bij naam, WF-047 treedt terug (één melding per
  gebrek), gespiegeld aan de setbronarm van de gevulde telling. Negatiefs
  per vorm, rood-eerst, mutatieproef.
- (b) `ref` en clingo weigeren in dezelfde zin vanuit benoemde armen (de
  bodemzinnen vervallen voor deze vormen); suitegetuigen in beide suites.
- (c) De Drools-lader weigert de vormen die WF-037 weigert (deze kaart én
  de vorige) in de zin van de verifier, met een getuige in de eigen JUnit.
- (d) Alle veertien gates groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` (**voorstel**),
`corpus/negatief/**`, `ref/**`, `backends/clingo/**`, `backends/drools/**`,
kaartverplaatsing.

## U-besluiten (hoofdsessie, 2026-09-25, bij de claim)

- U1 Bronarm onder WF-037 (geen nieuw nummer): een telling (`count`/`filledCount`) met `elementType` draagt als enig argument een instantiebron (de vormen van WF-096/WF-102: een `AttributeReference` langs een to-many rol of pad, met of zonder `sourceRole`/`sourcePath`); elke andere vorm (Literal, Binary, ParameterReference, VariableReference, BuiltinCall, …) is ill-formed bij naam; WF-047 treedt terug (één melding per gebrek), gespiegeld aan de setbronarm van de gevulde telling en aan de omgekeerde arm van 2026-09-25. Negatiefs per vorm (`wf037-aantal-elementtype-literaal`, `-binair`, `-parameter`, …), rood-eerst, `expect.txt`, NOTE uit echte uitvoer, mutatieproef.
- U2 `ref` en clingo weigeren in dezelfde zin vanuit benoemde armen (de bodemzin `unsupported instance-fold source shape` en de clingo-bodemzin vervallen voor deze vormen); suitegetuigen in `ref/test/test_ref.ml` en `backends/clingo/test/test_clingo.ml` (mandaat); rood-eerst met een mutant.
- U3 De Drools-lader (`IrLoader.java`) weigert de vormen die WF-037 weigert — elementType zonder argument, een `ListLiteral`, en de niet-bronargumenten van U1 — met de zin van de verifier (`Refusal`), zodat geen ill-formed telling meer stil speelt; JUnit-getuige (nieuwe klasse); de zinnen letterlijk gelijk in vier lezers.
- U4 Tweede taak, direct fixen waar het kan: de exec-tak sluit een consistentie uit waarvan de assertie een `Compound` met een andere kwantor dan `all` is (`ExecModelTranspiler.java` r. ~5577, "schending buiten Phase-D scope"). Meet eerst; hergebruikt de lowering de bestaande conditie-evaluatie van regels (die `atLeast`/`atMost`/`exactly`/`none` al draagt), bouw hem dan, met een golden (handgerekend, ref ≡ clingo ≡ exec, `bron.rgs` als de frontend de vorm schrijft) en gate-12-pins (**voorstel**); anders meld je precies wat ontbreekt.
- U5 Gates 1, 2, 3, 5, 6, 8, 12 (13 als een golden een type-annotatie op een afgeleide positie draagt).

## Result (2026-09-25, bouwer)

## Ronde 3 (lensfix) — 2026-09-25

### Stand

Branch `fix/lens-bronarm` in wt/bronarm, vanaf de unie `card/k7-unie` = `36750511`. **Kop `3bd9bac3`.** Zes
commits, niets gepusht of gemerged; geen kaartje, kaart, CONSTRUCTS, PLAN, CLAUDE.md, WAY-OF-WORKING, .github of
scripts aangeraakt. Beschermde paden alleen als voorstel: `spec/IR-SPEC.md` en `spec/verifier/lib/checks.ml`
(81971a17). Buiten de regio's van fix/lens-verifier (typeringsweigeringen in IrLoader, datumzinnen) en
fix/lens-clingo (translate.ml): ik raakte in IrLoader alleen `aggregation()`.

    53e416f4 Drools-lader: een niet-gedragen vouwbron in de zin van de verifier            (B2)
    d1157be7 Drools-lader: het bereik vóór de bron, zoals ref en clingo                   (A2)
    81971a17 WF-110 (a) treedt terug waar de vouwbronarm de unie noemt (voorstel)         (A1)
    5999acba DrlTranspiler: het venstercommentaar volgt de arm erboven                    (A6)
    06335bbd exec: een geneste samengestelde assertie onder een tellende kwantor          (A7)
    3bd9bac3 exec: de geneste samenstelling ook in een tellende regelconditie             (A7, regelconditie)

Getuigen buiten de repo (hergebruikt en uitgebreid): lensproeven (buiten de repo) (de 145 bronarmdocumenten
+ 5 unie-documenten van de lens), (buiten de repo) (de tien kwantorvarianten × tien instanties, plus eigen
q12–q15), eigen randgevallen (buiten de repo), regelconditieproef (buiten de repo).
Meetgereedschap (buiten de repo),summ}.py`; uitvoer vóór `out-voor`, `out-extra-voor`, na `out-na`.

### Per vondst: meting vóór/na

**B2 — de Drools-lader sprak de WF-037-zin niet bij niet-gedragen knopen.** Vóór (unie): op de 145 lensdocumenten
gaven beide Drools-takken 27 keer `EXCLUDE loader: form not carried by the v0 Drools backend: Expr:<knoop> (unknown
expression node)`. Na: 0 keer; 19 geven de vouwbronzin, 8 (met een pad) WF-102 (c), gelijk aan ref en clingo. Op
alle 145: de eerste zin van ref, clingo, exec en DRL gelijk vóór 116, na 145. Zin (Drools, letterlijk, bv.):

    malformed uchar-ir-v1 document at '/rules/rules/0/versions/0/value': a count aggregation carries elementType "Vondst" and a Lambda operand: the source of an instance fold is an AttributeReference

Gebouwd (technische keuze, geregistreerd): geen lijst van niet-gedragen soorten vóór het lezen, maar uitstel van
de weigering. `aggregation()` leest het ene argument van een vouw met elementType zoals altijd; gooit dat lezen een
`Refusal` (een knoop die de backend niet draagt, ook dieper in het argument), dan wacht die tot het bereik
(BEREIK_TWEEMAAL, WF-102 (c), BEREIK_ONTBREEKT, pad) en de bron (`bronWeigering`, `unieBronWeigering`) gevraagd
zijn, en wordt alleen gegooid als geen daarvan weigert. Een `Documentfout` bij het lezen (WF-034, WF-113) wordt
niet uitgesteld, zoals de gedeelde lader die eerst weigert. Redenen: één bron van waarheid (de dispatch in
`expr()`), geen tweede lijst die kan afdrijven; een niet-gedragen knoop ónder een gedragen argument valt er ook
onder (x1: `Binary(Lambda, Literal)` vóór EXCLUDE, na de vouwbronzin, gelijk aan ref/clingo). Voor de elf soorten
van de lens is het effect gelijk aan een voorcontrole op de ruwe knoop: van de knoop wordt alleen de discriminator
gelezen (bij TemporalRead ook de sleutels).
Getuige, rood-eerst: `TellingBronarmWitnessTest` drie rijen (alle elf soorten onder count, ListLength onder sum,
Lambda onder een Binary), vóór 3 van 24 rood (Refusal i.p.v. Documentfout), na 24/24.
De vier teksten zijn nu waar, gecontroleerd: IR-SPEC WF-037-alinea (nu r. 7507–7511: "the Drools loader refuses at
load, in the verifier's sentences, … a node that is no `AttributeReference`. With a `sourcePath` beside the node …
the executors report that first"), de WF-037-indexrij (r. 8758), de javadoc van `bronWeigering` en de klassedoc van
`TellingBronarmWitnessTest` r. 17–21. Geen tekst gewijzigd.

**A2 — ListLiteral + sourcePath: Drools noemde eerst de omgekeerde arm.** Vóór: `pad2-count-lijstliteraal` en
`pad2-filledCount-lijstliteraal` Drools WF-037 (`… and a ListLiteral operand: a listed value is no source of
instances`), ref/clingo WF-102 (c). Na: Drools ook WF-102 (c), op `…/value/sourcePath`:

    a sourcePath is admissible only as the range of a single-argument VALUE fold's set source — a count's range is one step, spelled sourceRole

De voorcontrole op de ListLiteral vervalt (de ListLiteral wacht nu als elke niet-gedragen knoop tot na het bereik);
het commentaar bij `bronWeigering` in `aggregation()` belooft nu wat het doet. Zelfde klasse, meegenomen en
gemeten: een waardevouw met een pad van één stap en een niet-bron (x2 Literal, x9 Lambda) gaf in Drools eerst de
stappenweigering (`a sourcePath carries at least TWO steps, found 1 …`, een Refusal), ref/clingo WF-102 (c) → de
lader vraagt nu eerst de positie, dan het aantal stappen, zoals ref; een telling met ListLiteral zonder bereik (x3)
gaf de omgekeerde arm, ref/clingo de ladingzin van de gedeelde lader → nu die zin (`a single-argument fold with an
elementType carries its range (sourceRole or sourcePath) under every kind but total: the range is written, never
derived`). Getuige, rood-eerst: vier rijen in `TellingBronarmWitnessTest` (sectie 4), vóór 4 van 28 rood, na 28/28.

**A1 — een unie onder een niet-tellende vouw kreeg WF-037 én WF-110 (a).** Besluit: **WF-037 (de vouwbronarm) is de
ene melding.** Grond: IR-SPEC kende de unie al aan die arm toe ("Every other node there is ill-formed … and any
other … a union under a count is WF-110 (c)'s"); de verifier meldt een unie-positiefout al op de consumerende knoop
waar die een eigen regel heeft (clausule (c) onder een telling, `union_ok` door de ouder bepaald); en ref, clingo en
beide Drools-takken gaven al alle vier de WF-037-zin (gemeten). Eén wijziging dus: `union_ok` is ook waar waar
`non_source` vuurt. Vóór (lensdocumenten filledCount/sum/total/maximum + unie): twee regels, o.a.

    WF-110 FAIL /rules/rules/0/versions/0/value/arguments/0: an instance-set union stands where nothing consumes it: …

Na: één regel, bv. `WF-037 FAIL /rules/rules/0/versions/0/value: a sum aggregation carries elementType "Vondst" and
an InstanceSetUnion operand: the source of an instance fold is an AttributeReference`; ref, clingo, exec, DRL
ongewijzigd die zin. Sweep verifier over 793 corpus- en toka-documenten: 0 veranderd. Nieuwe negatief
`wf037-som-elementtype-unie` (expect.txt `a sum aggregation carries elementType "Vondst" and an InstanceSetUnion
operand`; NOTE met de echte uitvoer en de meting vóór). IR-SPEC (voorstel): WF-110 (a) registertekst, de
WF-037-vouwbronalinea, de WF-037-indexrij (met de getuige) en de WF-110-indexrij; naamtabel-toets rc 0.

**A6 — commentaar DrlTranspiler.** Het commentaar bij de venster-arm van `schendingConstraint` zegt nu dat de arm
erboven een samengestelde assertie onder geen/tellende kwantor als regelconditie verlaagt, waar een venster (op
elke diepte) de WF-112-marker is, zodat die arm null geeft en de aanroeper WF-112 noemt. Gecontroleerd tegen
`conditieInline` (Vensterpositie.weigering → marker) en de aanroeper (r. ~714).

**A7 — geneste samenstelling onder een tellende kwantor in de exec-tak.** Vóór: lensproef q9 (`exactly 1 [cmp,
atLeast 1 [cmp, cmp]]`) exec `EXCLUDE transpiler SKIP: … (schending buiten Phase-D scope)`, DRL PLAYED 10, ref/clingo
10/10. Na: exec PLAYED 10. Eigen varianten (IR met de hand, handgerekend): q12 `atLeast 2 [cmp, none[cmp,cmp], cmp]`,
q13 `none [cmp, all[cmp,cmp]]`, q14 diepte 2 `exactly 1 [cmp, atLeast 1 [cmp, none[cmp]]]`: ref/clingo 0 fouten, DRL
PLAYED 10 vóór en na, exec SKIP → PLAYED 10; q15 (`all` boven, genest tellend) speelde al en blijft PLAYED 10. De
negen andere lensvarianten blijven PLAYED 10 in beide takken. Gebouwd: de schending is de negatie van
`subjectConditie` (de bestaande vouw op willekeurige diepte, met de ch. 5.10-kortsluiting) i.p.v. `leafCompound`
(één niveau); voor een niet-geneste samenstelling zijn beide gelijk. Golden `jutterswet-keurcriteria` + K5
(`precies één van: lengte > 20, ten minste één van: gewicht > 50, jaartal < 1860`), uit `bron.rgs` geëmitteerd,
handgerekend (V1/V3 via het geneste criterium, V6 twee, V4/V5 nul); rood-eerst exec EXCLUDE → PLAYED 2; mutant
(`ten minste` streng in de vouw) → FAIL case 1 op K5 @ V1. **Geen pinverschuiving**: played 204, DRL 177.
Meegenomen, dezelfde klasse (niet in de lens): dezelfde vorm als **regelconditie** sloeg exec ook over (proef r1:
exec `regel (voorwaarde buiten Phase-B scope)`, DRL PLAYED 7, ref/clingo 7/7) — de tellende vouw van `voorwaarde`
las alleen bladcriteria. Nu leest die een geneste samenstelling via `subjectConditie`; r1 exec PLAYED 7. Golden:
kenmerk `keurig` + regel `keurigheid van een vondst` met de conditie van K5 woord voor woord (keurig precies waar K5
geldt); rood-eerst exec EXCLUDE → PLAYED 2; dezelfde mutant faalt keurig op V1 (case 1) en V7 (case 2). In corpus,
rood en toka staat geen regelconditie met die vorm (gezocht), dus ook hier geen kanaalwissel.

**A8 — alleen gemeld.** De DRL-tekst-tak geeft een stil antwoord op het WF-026-ketenpaar (lens: a1 → 100, a2 → 500);
valt onder U1 van de allocatiekaart (de verifier is de poort). Niets gebouwd.

### Poorten (elk los na `eval $(opam env)`, op 50410ae7; jar één keer, door gate 12; gate 8 daarna "fresh")

De kop `3bd9bac3` is 50410ae7 met alleen de javadoc van `subjectConditie` bijgewerkt (drie gebruikers i.p.v. één;
`git diff 50410ae7 3bd9bac3` = die ene javadoc; `mvn compile` groen); de poorten zijn daarom niet herhaald.

     1 check-self-contained: PASS
     2 check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 301 file(s) validate and round-trip, floor 192)
     3 check-verifier: PASS (301 golden clean; 463 negatief rejected for their declared rule, floor 350; 305 on their declared arm (expect.txt), floor 260; 565 cited pointers resolve in their documents, floor 547)
     4 check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 720 case file(s) validate)
     5 check-oracle: PASS (ref suite 1019 worked examples, floor 489; 677 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
     6 check-clingo: PASS (backend suite 1782 checks, floor 83; 677 cases replay exactly through clingo — ref = clingo over the corpus)
     7 check-noop: PASS (635 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)
     8 check-rood: PASS (43 red cases fault observably in ref and clingo, floor 26; 86 pinned fragments cite none of the 36 listed bottom sentences); Drools leg PASS (11 fault-pinned, 32 not-carried with reason); drools-jar: fresh (jar newer than 160 source files)
    10 check-regelspraak: PASS — JUnit suite green (721 tests, floor 173); 89 rgs golden(s) re-emit byte-identically (floor 80)
    12 check-drools: PASS — played 204 (pin 204), auto-excluded 51, value-divergence 1, no-case 3; channel floor 55; DRL-text branch played 177 (pin 177), DRL-text floor 82; JUnit 807 tests, floor 239

Gate 8 gedraaid omdat de kaart hem noemt (U5); 11 en 13 niet (geen toka-, geen eenheidsraking). Extra metingen:
Drools-lader vóór/na over alle 463 negatiefs (met een lege case) 0 verschillen; toka 29 documenten met cases,
beide takken vóór/na 0 verschillen. Tellingen t.o.v. de unie: negatief 462 → 463, expect.txt 304 → 305, Drools-JUnit
800 → 807; keurcriteria case 1 4 → 7 en case 2 12 → 19 verwachtingen; geen vloer of pin aangeraakt, geen scriptregel.

### Definitieve zinnen

Geen nieuwe of gewijzigde weigerzin: de bestaande zinnen staan nu op meer plaatsen in de Drools-lader (de
vouwbronzin bij niet-gedragen knopen; WF-102 (c) en de ladingzin van het ontbrekende bereik in de volgorde van
ref/clingo) en de verifier meldt bij de unie onder een niet-tellende vouw alleen de vouwbronzin. Letterlijk:

    a %s aggregation carries elementType "%s" and %s operand: the source of an instance fold is an AttributeReference
    a sourcePath is admissible only as the range of a single-argument VALUE fold's set source — a count's range is one step, spelled sourceRole
    a single-argument fold with an elementType carries its range (sourceRole or sourcePath) under every kind but total: the range is written, never derived

### Wat aan de opdracht/lens niet klopte (gemeten)

1. B2's mechanisme "vóór het lezen" is gebouwd als uitstel na het lezen (zie B2); gelijk effect voor de elf soorten,
   ruimer voor een niet-gedragen knoop dieper in het argument.
2. A2 reikte verder dan ListLiteral + sourcePath: ook een waardevouw met pad van één stap (x2, x9) en een
   ListLiteral zonder bereik (x3) volgden in Drools een andere volgorde; meegenomen.
3. A7 gold ook voor de regelconditie (r1), niet alleen de assertie; meegenomen, met golden-regel.
4. De telling "74 documenten met dezelfde WF-037-zin in vijf lezers" (lens) meet ik als 76 vóór (mijn criterium:
   de vouwbronzin in alle vijf lezers, over dezelfde 145); na 95.

### Registervoorstel spec/CONSTRUCTS.md (aanvulling op de zinnen van ronde 1–2 hieronder)

Rij 1, achter de alinea "Landed … the fold-source arm":

> **Fix round (2026-09-25):** the Drools loader refuses a node it does not carry at a fold source — the argument
> or a node under it — in the arm's sentence, where before both branches excluded the rule as a form not carried,
> and asks the range before the source as `ref` and the ASP backend ask it (a path first under WF-102 (c), a
> missing range first as such); a union at the source of any fold but a count is reported once, under this arm,
> WF-110 (a) stepping aside. Measured on 145 out-of-corpus documents: one first sentence in the four executors on
> 116 before, 145 after; Drools exclusions 27 → 0. Witness `wf037-som-elementtype-unie`.

Rij 19, achter de alinea van ronde 1–2:

> **Fix round (2026-09-25):** a composite nested among the criteria of a counting or negating composite plays in
> the exec-model branch too — in a consistency's assertion and in a rule condition, one criterion that holds as its
> own fold does, at any depth — where before that branch excluded the rule while the DRL-text branch, `ref` and
> clingo answered. Golden `jutterswet-keurcriteria` (K5 and the rule `keurigheid van een vondst`, from `bron.rgs`).

### Restpunten

1. Een weigering bij het laden binnen een knoop die de Drools-lader niet leest: x4 (telling + sourceRole +
   `ListLiteral[VariableReference]`) en x6 (telling + `Lambda` met een VariableReference als lichaam). ref en clingo
   weigeren bij het laden (WF-034), Drools geeft de WF-037-zin (x4: vóór ook al; x6: vóór EXCLUDE). De verifier meldt
   bij x4 WF-037 én WF-034, bij x6 alleen WF-037 en WF-069 (hij loopt het Lambda-lichaam niet af). De lader kan die
   knopen niet lezen; zo gelaten.
2. x5: een `total` zonder bereik met een vreemd elementType en een Literal: ref/clingo eerst WF-083 (c), Drools eerst
   WF-037 (WF-083 (c) toetst de lader pas in de onderwerpronde, na de knoop). De verifier meldt beide. Niet veranderd:
   dat vergt de vouwbronzin voor dit geval uitstellen tot de onderwerpronde.
3. x10: een telling zónder elementType over een Lambda: verifier WF-037 (de elementType-arm van de telling) + WF-069;
   ref een eigen WF-069-zin, clingo een bodemzin (`expression kind not translated (no corpus case exercises it)`),
   Drools EXCLUDE. Een andere arm dan deze kaart; geen uitvoerder spreekt daar de verifierzin.
4. Gate 3 ziet de één-melding-eigenschap van A1 niet (extra meldingen zijn toegestaan); alleen de NOTE van
   `wf037-som-elementtype-unie` en de verifiersweep leggen haar vast.
5. A8 (zie boven): voor de hoofdsessie, onder U1 van de allocatiekaart.
6. Open punten 1, 3–5 van ronde 1–2 (hieronder) staan nog; punt 2 (pad naast een niet-bron: uitvoerders eerst
   WF-102 (c)) geldt nu ook voor de Drools-lader bij ListLiteral en niet-gedragen knopen.

---

## Result aantal-bronarm-en-drools-lader (2026-09-25, bouwer; bijgewerkt na de vervolgronde)

### Stand

Branch `card/aantal-bronarm-en-drools-lader`, basis main `20e508d5`, claim `96d2b042`, **kop `f8226e97`**. Elf
commits, niets gepusht of gemerged; kaart, CONSTRUCTS, PLAN, CLAUDE.md, WAY-OF-WORKING, .github niet aangeraakt.

    8d0564f8 negatief: zes getuigen (count) rood-eerst + verhuisde pin wf037-aantalgevulde-niet-setbron
    bab223da verifier: bronarm onder WF-037 (voorstel), IR-SPEC, NOTE's
    70ce8ac7 ref en clingo: benoemde armen + suitegetuigen
    94d23296 Drools-lader: Documentfout in de verifierzin + TellingBronarmWitnessTest
    aceb1911 U4: consistentie met tellende/ontkennende samengestelde assertie in beide Drools-takken + golden + pins (voorstel)
    186fed4c golden jutterswet-keurcriteria: canonieke serialisatie
    cbc58941 (5) the-kwantor in een consistentie: beide Drools-takken via de all-arm + ConsistentieDeKwantorWitnessTest
    1de840ca (1) WF-110 (c) in de verifierzin in ref, clingo en Drools-lader + suitegetuigen
    2de33860 (2) drie negatiefs voor waardevouwen, rood-eerst
    dc3df151 (2) vouwbronarm over alle acht soorten (voorstel), zin ingekort, ref/clingo/Drools + getuigen
    f8226e97 (6) frontend: samengestelde consistentie onder elke kwantor naar de wire; bron.rgs + her-emissie golden

### De definitieve weigerzinnen (letterlijk gelijk in verifier, ref, clingo en Drools-lader, zonder regelnummerstaart)

WF-037, de vouwbronarm (nieuw; sinds dc3df151 over alle acht aggregatiesoorten):

    a %s aggregation carries elementType "%s" and %s operand: the source of an instance fold is an AttributeReference

1e %s: soort in L5-spelling; 2e: elementType-naam tussen aanhalingstekens; 3e: wire-knoopnaam met lidwoord ("an"
vóór A/E/I/O/U; OCaml `Spelling.a_node`, Java op het ruwe `node`-veld). Echte verifieruitvoer:

    WF-037 FAIL /rules/rules/0/versions/0/value: a count aggregation carries elementType "Vondst" and a Literal operand: the source of an instance fold is an AttributeReference
    WF-037 FAIL /rules/rules/0/versions/0/value: a sum aggregation carries elementType "Vondst" and a ParameterReference operand: the source of an instance fold is an AttributeReference
    WF-037 FAIL /rules/rules/0/versions/0/value: a total aggregation carries elementType "Jutter" and a Literal operand: the source of an instance fold is an AttributeReference

(De eerste versie eindigde "… over its range"; ingekort omdat een `total` zonder bereik geen bereik heeft.)

WF-110 (c) (bestaande verifierzin, nu ook in ref, clingo en Drools-lader; velden in de volgorde elementType,
sourceRole, sourcePath, filter):

    a count over an instance-set union names no source of its own, and this node carries %s: the union's selections carry the population, the relation and the subselection, one set per selection, so a source here is a second claim on one node (ch. 4.3)

De Drools-lader spreekt daarnaast de twee zinnen van de omgekeerde arm ongewijzigd (`… but no source operand: an
instance fold ranges over a written source`, `… and a ListLiteral operand: a listed value is no source of
instances`). Gelijkheidstoets (eerste ronde, 42 documenten): 40 letterlijk gelijk in de vier lezers, de 2 overige
dragen een sourcePath (twee gebreken; de drie uitvoerders melden eensgezind eerst WF-102 (c)).

### Wat gebouwd is

- Verifier (voorstel): `non_source` naast `sourceless` in `checks.ml`: elke aggregatie met elementType en één
  argument dat geen AttributeReference is (uitgezonderd: ListLiteral — onder de tellingen de omgekeerde arm, onder de
  waardevouwen WF-091 —, en een unie onder count: WF-110 (c)); de setbronarm van de gevulde telling gaat erin op; de
  WF-047-bereikarm treedt terug. `spelling.ml`: `expr_node`, `a_node`.
- ref: benoemde armen voor de vouwbronarm en WF-110 (c) (de eigen filterzin bij de unie vervalt; een telling over een
  unie met alleen een sourceRole werd eerder stil beantwoord). clingo: `non_source`/`non_source_fold` in `expr_term`,
  `symbolic_term` en `dt_term`; de WF-110 (c)-arm spreekt de verifierzin, een pad valt door naar WF-102 (c).
- Drools-lader: `bronWeigering` (drie vormen) en `unieBronWeigering` (WF-110 (c)), als Documentfout.
- Drools-transpilers: (U4) de schending van een consistentie onder geen/ten minste/ten hoogste/precies is de negatie
  van de vouw van de regelcondities (exec `leafCompound`, DRL `conditie`/`minstens`); (5) `the` neemt de `all`-arm.
- Frontend (mandaat (6)): `RuleChecker.transpileerbaar` laat een samengestelde assertie onder elke kwantor door;
  `subjectVan` geeft de subject-geleide samengestelde consistentie haar onderwerp, zodat "zijn …" oplost. Alleen
  consistentiepaden; IrEmitter en de grammatica ongemoeid.
- IR-SPEC (voorstel): cel `arguments`, de alinea over de gevulde telling in ch. 4.3, de WF-037-registertekst
  (samenvattende zin, setbronalinea, alinea "The fold-source arm"), de WF-037-indexrij, een zin in WF-110.
  Naamtabel-toets rc 0.

### Meting en rood-eerst (per punt)

- Eerste ronde: zie de tabel vóór/na hieronder; gate 3 rood-eerst 13 FAIL-regels op 7 negatiefs; sweep 762 docs,
  7 bewegen, 0 verdictwissels; zes mutanten (NOTE wf037-aantal-elementtype-literaal).
- (1) Gemeten: ref bodemzin (elementType+sourceRole), stil antwoord (alleen sourceRole), eigen zin (filter); clingo eigen
  zin; Drools PLAYED in beide takken. Rood-eerst: ref-suite 3 failures, clingo-suite 4, JUnit 3.
- (2) Gemeten: waardevouw + elementType + sourceRole + Literal/Parameter/Binary: verifier alleen WF-047; ref/clingo
  bodemzinnen; Drools exec SKIP, DRL ClassCastException (sum/total/min/max) of MARKER (first/last); `total` zonder
  bereik + Literal: verifier PASS, niemand voert uit. Kosten: alle 30 totalen zonder bereik en alle waardevouwen met
  één argument dragen een AttributeReference. Rood-eerst: gate 3 6 FAIL-regels op de 3 nieuwe negatiefs; ref 8; clingo
  5 (incl. symbolic_term, dt_term); JUnit 11. Sweep 766 corpusdocs: 10 bewegen (7 oude getuigen alleen de zin, 3
  nieuwe), 1 verdictwissel (de nieuwe totaal-getuige); verifier checks.ml 1763a801…, binary cc6adade….
- (5) Gemeten: ref ≡ clingo lezen `the` als conjunctie (RUNTIME-SEMANTICS ch. 5.10: all/the vragen h = n); beide
  Drools-takken SKIP/MARKER. RegelSpraak spelt `the` in een consistentie niet (geen "de" in consistentieKwantificatie;
  "het volgende criterium" wordt `all`) → geen golden-case, JUnit-getuige; rood-eerst 2 errors → 0.
- (6) Gemeten: onder `alle` tilde de frontend alleen criteria met expliciet subject; onder de andere kwantoren nooit.
  Frontend-JUnit SamengesteldeConsistentieTest rood-eerst 6 van 7 faalden (WARNING), daarna 7/7. Drie eerder getilde
  proefconsistenties emitteren byte-gelijk. Golden: **her-geëmitteerd** uit de nieuwe `bron.rgs` — de emissie wijkt
  van de handgeschreven IR alleen af in `source`-fragmenten (zonder source-records gelijk); beide casebestanden
  byte-ongewijzigd; verifier PASS, ref/clingo PASS, exec en DRL PLAYED 2.

Tabel vóór/na (eerste ronde; kolommen verifier | ref | clingo | exec | DRL):

    count + Literal/Binary/Parameter/Variable/BuiltinCall/Unresolved/Rounding/Aggregation (sourceRole)
        vóór: WF-047 binding | bodem | bodem | PLAYED 1 | PLAYED 1     na: WF-037 | zin | zin | Documentfout-zin x2
    filledCount + dezelfde vormen     vóór: WF-037 setbron | bodem | bodem | SKIP | PLAYED 1     na: idem als count
    sum/total/min/max/first/last + Literal/Parameter (sourceRole)
        vóór: WF-047 binding | bodem | bodem | SKIP | ClassCastException of MARKER     na: WF-037 | zin | zin | zin x2
    total zonder bereik + Literal     vóór: PASS | bodem | bodem | SKIP | MARKER       na: WF-037 | zin | zin | zin x2
    count + unie + eigen bron (WF-110 (c))  vóór: WF-110 | bodem of stil antwoord | eigen zin | PLAYED x2   na: WF-110-zin x4
    vorige kaart: zonder argument / ListLiteral onder count   vóór Drools: SKIP, MARKER, PLAYED 1 of Refusal ListLiteral
        na: Documentfout-zin in beide takken

### Poorten (elk los na `eval $(opam env)`, eindstand f8226e97; 12 vóór 8, jar één keer gebouwd)

     1 check-self-contained: PASS
     2 check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 294 file(s) validate and round-trip, floor 192)
     3 check-verifier: PASS (294 golden clean; 442 negatief rejected for their declared rule, floor 350; 284 on their declared arm (expect.txt), floor 260)
     5 check-oracle: PASS (ref suite 1015 worked examples, floor 489; 645 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
     6 check-clingo: PASS (backend suite 1749 checks, floor 83; 645 cases replay exactly through clingo — ref = clingo over the corpus)
    12 check-drools: PASS — played 198, auto-excluded 51, no-case 2; channel floor 54, played matches the pinned 198; DRL-text branch played 174 (pin 174), DRL-text floor 78; JUnit 799 tests, floor 239
     8 check-rood: PASS (43 red cases fault observably in ref and clingo, floor 26; 86 pinned fragments cite none of the 36 listed bottom sentences); Drools leg PASS (11 fault-pinned, 32 not-carried with reason); drools-jar: fresh
    10 check-regelspraak: PASS — JUnit suite green (625 tests, floor 173); 82 rgs golden(s) re-emit byte-identically (floor 80)
    11 check-toka-parity: PASS — re-emission: 1 casus dump, 45 .rgs byte-identical (floor 45, 2 holdouts), TOKA suite 27 tests; ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED

Gate 11 gedraaid voor de gevraagde byte-identiteit van de rest (0 van de 47 toka-bronnen draagt een samengestelde
consistentie). Gate 13 niet (geen type-annotatie op een afgeleide positie in de nieuwe golden).

Tellingen t.o.v. de basis: negatief 433 → 442; expect.txt 275 → 284; golden+rood schoon 293 → 294; cases 643 → 645;
rgs-goldens 81 → 82; ref-suite 994 → 1015; clingo-suite 1733 → 1749; Drools JUnit → 799 (+15 TellingBronarm, later 21;
+2 ConsistentieDeKwantor); frontend JUnit 618 → 625. Geen vloer aangeraakt.

### Pins en gewijzigde scriptregels (beschermd pad, voorstel)

`scripts/check-drools.sh`: `EXPECTED_PLAYED=197` → `198`, `EXPECTED_DRL_PLAYED=173` → `174`, elk met redenblok
(jutterswet-keurcriteria). Sinds de vervolgronde ongewijzigd (de her-emissie speelt nog steeds in beide takken).
Geen andere scriptregel.

### Wat aan de kaarttekst niet klopte (gemeten)

1. "alleen de bindingszin van WF-047" gold alleen onder `count`; onder `filledCount` weigerde de setbronarm al.
2. Drools speelde meer vormen dan de kaart mat (onder count ook Parameter/Variable/BuiltinCall/Unresolved/Rounding/
   Aggregation; onder filledCount alle in de DRL-tak).
3. WF-096 is sinds L2 geschiedenis; de instantiebron is de ene AttributeReference naast een bereik (ch. 4.3).
4. U3 "(`Refusal`)": gebouwd als Documentfout (door de coördinator akkoord bevonden).
5. U1 "gespiegeld aan de setbronarm": de arm neemt die setbronarm over (één zin, knoop genoemd).
6. U4 "de exec-tak": ook de DRL-tak gebouwd; "bron.rgs als de frontend de vorm schrijft": deed hij niet — sinds (6) wel.
7. U1 "per vorm": zes onder count; in de vervolgronde drie onder de waardevouwen.

### Registervoorstel spec/CONSTRUCTS.md

Rij 1, na de alinea "**Landed (2026-09-25, card `aantal-bronargument-welgevormd`): the converse arm …**":

> **Landed (2026-09-25, card `aantal-bronarm-en-drools-lader`): the fold-source arm.** The one argument of an
> aggregation that carries `elementType` is an `AttributeReference`, under every kind; every other node there is
> ill-formed under WF-037 and named in one sentence (no new number; a `ListLiteral` under a value fold stays WF-091's,
> a union under a count WF-110 (c)'s). Measured before: with a range WF-047 refused the form on its binding sentence
> alone, `ref` and clingo in bottom sentences, both Drools branches PLAYED the counts by counting the range and crashed
> on or excluded the value folds, and a `total` without a range over a literal was verifier-clean while no executor
> ran it. The arm takes over the filled count's set-source arm, WF-047 steps aside, `ref` and clingo refuse from named
> arms, and the Drools loader refuses at load, in the verifier's sentences, every shape of WF-037 that names a
> population and hands no source, and WF-110 (c). Nine negatiefs; one verdict change, a new witness.

Rij 19, na de alinea "**Drools backend (2026-09-23, card `drools-consistentie-label`)** …":

> **Landed (2026-09-25, card `aantal-bronarm-en-drools-lader`):** a consistency whose assertion is a composite under
> `none`, `atLeast`, `atMost`, `exactly` or `the` plays in both Drools branches — the violation is the negation of the
> fold their rule conditions carry, `the` through the `all` arm — where before both excluded it while `ref` and clingo
> answered; and the RegelSpraak frontend lifts the composite consistency under every quantifier, reading "zijn …"
> against the onderwerp, where before it lifted it only under `alle` with the subject written in full. Golden
> `jutterswet-keurcriteria`, emitted from its `bron.rgs`; `the` has no RegelSpraak spelling there and is witnessed in
> the Drools backend's own suite.

### Open punten

1. Een `ListLiteral` onder een waardevouw met elementType en bereik: verifier WF-091 + de WF-047-bindingszin, `ref` en
   clingo bodemzinnen, Drools Refusal `Expr:ListLiteral`. Bewust gelaten: de vorige kaart besliste dat vorm (b) bij de
   tellingen blijft.
2. Met een sourcePath naast een niet-bronargument meldt de verifier WF-037 én WF-102 (c) (en (d)); onder de gevulde
   telling beschrijven ze hetzelfde gebrek (bestond al met de setbronarm). De uitvoerders melden eensgezind eerst
   WF-102 (c).
3. De één-melding-eigenschap ziet gate 3 niet (extra meldingen zijn toegestaan); alleen in de NOTE.
4. De vijfregelige alinea over de bodemzinnen staat nog in de NOTE's van vier getuigen van de omgekeerde arm (vorige
   kaart).
5. Frontend-bouwsels (`frontends/regelspraak/lib`, `gen`, `out`, gitignored) staan in de worktree.

## Lens en fixronde (2026-09-25, hoofdsessie)

Lens (uitvoerders-trio, unie 36750511, eigen meting in kopieën): EERST FIXEN. Goedgekeurd op eigen meting: 145 proefdocumenten met negentien knoopsoorten onder de vijf vouwen; 74 documenten met dezelfde WF-037-zin in verifier, ref, clingo en beide Drools-takken; WF-110 (c) in vier lezers gelijk per veldcombinatie; de DRL-crash weg (Documentfout in beide takken); consistenties: tien kwantorvarianten × tien instanties handgerekend, ref en clingo zonder fout, exec en DRL spelen ze (op de geneste vorm na, A7); venster onder een kwantor in alle lezers WF-112; de frontend-emissie byte-gelijk voor `jutterswet-keurcriteria`; suites rood-eerst herhaald met eigen mutanten (ref 17 en 3 failures, clingo 12, Drools 16 van 21, keurcriteria FAIL in beide takken zonder de negatie, frontend 5 van 7). Documentfout is de juiste route (gate 12 laat een ill-formed golden hard falen, conform de verifier).

Blokkerend: B2 — de Drools-lader sprak de WF-037-zin niet bij elf knoopsoorten (Lambda, ListLength, Match, Construct, StructLiteral, FieldAccess, Invoke, Fold, MapList, FilterList, TemporalRead): daar viel hij terug op een Refusal "form not carried", terwijl IR-SPEC, de javadoc en de JUnit-klasse het tegendeel beweerden. Advies: A1 (een unie onder een niet-tellende vouw kreeg WF-037 én WF-110 (a)), A2 (de volgorde van de armen bij ListLiteral met sourcePath verschilde van ref/clingo), A6 (verouderd commentaar in DrlTranspiler), A7 (een geneste samengestelde voorwaarde onder een tellende kwantor werd in de exec-tak overgeslagen terwijl de registerzin beide takken belooft), A8 (het stille DRL-antwoord op het WF-026-ketenpaar valt onder U1 van de allocatiekaart — alleen gemeld), A9 (kopregels van commits tot 89 tekens — voor de toekomst).

Fixronde (branch fix/lens-bronarm, verse fixer): zes commits op de unie. B2: de lader laat de Refusal van een niet-gedragen knoop wachten tot het bereik en de bron gevraagd zijn (één lijst van niet-gedragen knopen, ook dieper in het argument; een Documentfout van WF-034/WF-113 gaat voor): op de 145 lensdocumenten 27 → 0 exclusies, 19 in de WF-037-zin en 8 (met een pad) eerst WF-102 (c); één eerste zin in ref, clingo, exec en DRL op 116 → 145; JUnit rood-eerst 3 van 24; de vier teksten zijn nu waar. A2: bij een ListLiteral met een pad noemt Drools eerst WF-102 (c), zoals ref en clingo; een waardevouw met een pad van één stap eerst de positie, een ontbrekend bereik vóór de bron; commentaar klopt; JUnit 4 van 28 rood-eerst. A1: WF-037 is de ene melding voor een unie onder een niet-tellende vouw en WF-110 (a) treedt terug (IR-SPEC-tekst en -rijen, negatief `wf037-som-elementtype-unie`; sweep 793 documenten 0 bewogen; naamtabel rc 0). A6: commentaar bijgewerkt. A7: de geneste samengestelde assertie onder een tellende kwantor speelt in de exec-tak (lensproef q9 en eigen varianten tot diepte 2: SKIP → PLAYED 10; `jutterswet-keurcriteria` K5 uit bron.rgs, rood-eerst EXCLUDE → PLAYED 2; mutant met strenge `ten minste` laat case 1 falen), en dezelfde vorm als regelconditie (regel `keurigheid van een vondst`). Pins ongewijzigd (204/177 op de branch). Gates 1–8, 10, 12 groen. Bewust open (hoofdsessie): de volgorde van de eerste melding op dubbel misvormde documenten — een VariableReference in een ListLiteral of Lambda (ref/clingo WF-034, Drools WF-037), een `total` zonder bereik met vreemd elementType (ref/clingo eerst WF-083 (c), Drools eerst WF-037) — en een telling zonder elementType over een Lambda, waar geen uitvoerder de verifierzin spreekt (een andere arm dan deze kaart).
