# fout-op-tussenstand: een deling door een tijdelijke nul geeft afhankelijk van de regelvolgorde een fout of een waarde

*Gevonden op 2026-10-08 door de eindlens van de kaart
`20261007-drools-logische-afleiding` en door de hoofdsessie vastgelegd volgens
de afspraak dat een bestaande bug een kaart krijgt. Gemeten op uchar main
`a0661940` (ref en clingo uit `_build` van de hoofdcheckout, de shaded jar van
`backends/drools` uit main `d2021534`; daartussen veranderde alleen een kaart).
De proefbestanden stonden onder `/tmp/eindlens-probe-oB65/` (`a`,
`a-omgekeerd`, `b`, `b-omgekeerd`, elk met `ir.json` en cases); de documenten
staan hieronder, zodat de meting te herhalen is.*

## Aanleiding

Ch. 5.2 van `spec/RUNTIME-SEMANTICS.md` belooft confluentie: de volgorde van
de regels, de activeringen en de invoegingen doet er niet toe, met drie
genoemde uitzonderingen; het uitgewerkte voorbeeld daar zegt dat een
tussenwaarde in de eindtoestand niet waarneembaar is. Een deling door een
numerieke nul is een fout, geen leeg (ch. 1.4, punt 4). Een initialisatie is de
laagste default (ch. 5.4, 7.3, 7.7), en alleen de beslissende default wordt
uitgerekend (ch. 7.1; ch. 7.5, de rij "speculative default evaluation"). Op
twee kleine documenten maakt een fout een tussenstand toch waarneembaar, en
hangt de uitkomst van ref en van beide Drools-takken af van de volgorde van de
regels in het document.

Model voor beide: objecttype Vondst met `taxatie`, `opbrengst`, `deelnemers`,
`bergloon` en `aandeel` (gehele getallen) en de kenmerken `gemeld` en `erkend`.

Document A:

    Regel erkenning: Een Vondst is erkend indien hij gemeld is.
    Regel bergloon: Het bergloon van een Vondst moet gesteld worden op zijn
        taxatie indien hij erkend is.
    Regel standaardbergloon: Het bergloon van een Vondst moet geïnitialiseerd
        worden op zijn opbrengst gedeeld door zijn deelnemers.

Case: V1 gemeld, taxatie 30, opbrengst 100, deelnemers 0. Volgens ch. 5.4,
7.1 en 7.7 beslist de regel bergloon; de initialisatie beslist niet en wordt
niet uitgerekend: bergloon 30, consistent.

Document B: erkenning en bergloon als in A, plus

    Regel standaardbergloon: Het bergloon van een Vondst moet geïnitialiseerd
        worden op 0.
    Regel aandeel: Het aandeel van een Vondst moet berekend worden als zijn
        opbrengst gedeeld door zijn bergloon.

Case: V1 gemeld, taxatie 30, opbrengst 90. Het fixpunt: bergloon 30, aandeel
3, consistent; de 0 van de initialisatie is een tussenstand.

## Gemeten (2026-10-08)

"Voorwaarts" is de volgorde hierboven, "omgekeerd" de omgekeerde volgorde van
de regels in het document.

| Uitvoerder | A voorwaarts | A omgekeerd | B voorwaarts | B omgekeerd |
|---|---|---|---|---|
| ref | `PASS` | `EVALUATION FAULT: division by numeric zero` | `PASS` | `EVALUATION FAULT: division by numeric zero` |
| clingo | `UNSATISFIABLE — a fail-fast guard fired: viol("division by numeric zero","V1")` | idem | `PASS` | `PASS` |
| Drools, exec-modeltak | `PLAYED` | `ERROR DelingDoorNul … "standaardbergloon": 100 gedeeld door 0` | `PLAYED` | `ERROR DelingDoorNul … "aandeel": 90 gedeeld door 0` |
| Drools, DRL-teksttak | `PLAYED` | idem exec | `PLAYED` | idem exec |
| Drools, truth-maintenance-proeftak (branch) | `ERROR` | `ERROR` | `PLAYED` | `ERROR` |

Bij B rekenen ref en de Drools-takken de lezer uit tegen de tijdelijke 0 als
die lezer eerder aan de beurt is; clingo geeft het fixpunt. Bij A rekent ref de
initialisatie uit als die vóór de rekenende regel staat, en clingo altijd: zijn
fail-fast op een deling door nul is niet bewaakt door het besluit van de
default. Geen golden heeft een van beide vormen, dus de gates zien het niet.
De proeftak weigert vorm A sinds de fixronde van zijn eindlens; vorm B geeft
hij dezelfde volgordeafhankelijkheid als ref.

## Aanvulling (2026-10-09): document C en de tak met truth maintenance

Gemeten op 2026-10-08 tijdens `20261008-tms-aggregaties-creatie-en-fouten`
(waarneming 4 van haar `Result:`), op haar branch `361ca2ec`, met ref en clingo
uit `_build` van de worktree en de shaded jar van die commit.

Document C: objecttype Vondst met `opbrengst`, `deelnemers` en `toeslag`
(gehele getallen) en de kenmerken `gemeld` en `erkend`.

    Regel erkenning: Een Vondst is erkend indien hij gemeld is.
    Regel standaardtoeslag: De toeslag van een Vondst moet geïnitialiseerd
        worden op 5 indien zijn opbrengst gedeeld door zijn deelnemers groter
        is dan 10.

Case: V2 gemeld, opbrengst 100, deelnemers 0, en toeslag 7 in de invoer. De
initialisatie schrijft niet, want haar doel is in de invoer gevuld (ch. 5.4);
onder het voorgestelde antwoord hieronder is de verwachting toeslag 7,
consistent.

| Uitvoerder | C |
|---|---|
| ref | `EVALUATION FAULT: division by numeric zero` |
| clingo | `UNSATISFIABLE — a fail-fast guard fired: viol("division by numeric zero","V2")` |
| Drools, exec-modeltak | `ERROR DelingDoorNul: deling door nul in de regel "standaardtoeslag": 100 gedeeld door 0 …` |
| Drools, DRL-teksttak | `PLAYED 1` |
| Drools, truth-maintenance-tak (vóór en na de kaart) | `PLAYED 1` |

Ref rekent de voorwaarde van een initialisatie uit vóór de blokkade op het doel
(`ref/lib/eval.ml`, de tak van de initialisatie na `eval_cond`); de tak met
truth maintenance toetst eerst op het onderwerppatroon dat het doel in de invoer
leeg is en rekent de voorwaarde daar niet uit. C verschilt van A: bij A rekent
een initialisatie haar waarde naast een rekenende regel die beslist, bij C haar
voorwaarde terwijl de invoer haar blokkeert. De vraag is dezelfde: mag een
initialisatie die niet beslist een fout geven? `20261008-deling-door-leeg-onder-initialisatie`
stelt die vraag voor een deponering (deling door leeg), waar de spec al nee
zegt (ch. 7.1); C is haar tegenhanger voor een fout. Neem C mee in het besluit, in de
goldens van (b) en in (c).

Wat de tak met truth maintenance sinds die kaart op A en B antwoordt, staat in
haar `Result:` onder "Meting voor `20261008-fout-op-tussenstand`": A weigert hij
in beide volgordes (een deling in een initialisatie naast een andere schrijver;
met die weigering uitgezet speelt hij bergloon 30), B speelt hij in beide
volgordes met aandeel 3. Met fouten als feit van het fixpunt geeft de tak dus al
wat het voorgestelde antwoord vraagt.

## Vraag aan de eigenaar (spec)

Telt een fout op een tussenstand? Voorgesteld antwoord: nee. Een fout is, net
als een waarde, een eigenschap van het fixpunt (ch. 5.2); een initialisatie die
niet beslist, wordt niet uitgerekend en geeft dus geen fout (ch. 7.1). Dan is
het antwoord bij A bergloon 30 en bij B aandeel 3, in elke volgorde, en wijken
ref (beide), clingo (A) en de Drools-takken (beide) af. Het alternatief is een
voorgeschreven evaluatievolgorde waarin een lezer pas na al zijn schrijvers
rekent; dat lost B op, maar A niet.

**Besluit van de eigenaar (2026-10-09), op advies van een Fable-adviseur
(`/tmp/lens-bugs/advies-fable.md`, vraag 3):** nee. Een evaluatiefout en een
§9.5-deponering zijn uitkomsten van de evaluatie van één vorm op één onderwerp.
Een her-evaluatie onder versheid (ch. 5.1) vervangt de eerdere uitkomst, een vorm
die niet beslist wordt niet uitgerekend (ch. 7.1, 7.3), en de sessie meldt de
uitkomsten die aan het fixpunt van het stratum van de vorm staan. Uitkomsten, in
elke volgorde: A bergloon 30; B bergloon 30 en aandeel 3; C toeslag 7; alle
consistent. Voor een deponering op een tussenstand geldt hetzelfde, dus de zinnen
van ch. 6.1 en 7.1 dat een tijdelijke deponering blijft staan, vervallen. Het
besluit staat los van de vraag of de numerieke nul een harde fout blijft of het
besloten v1-kanaalfeit wordt (PLAN §9 (7)).

Gevolgen volgens het advies: ref toetst bij een initialisatie de blokkade op het
doel vóór de voorwaarde (lost C) en houdt fout en deponering per (vorm,
onderwerp) bij, vervangt ze bij her-evaluatie en meldt ze pas aan het fixpunt
(lost A en B); clingo krijgt op de `viol` van de nuldeling en op de deponering
onder een initialisatie de guards die de `val`-kop al heeft (lost A en C); de tak
met truth maintenance doet het al, en zijn twee weigeringen die op dit besluit
wachten (een deling in een default, een deling in een initialisatie naast een
andere schrijver) kunnen weg. Voorgestelde spectekst (ch. 5.7, na "Divergence by
two writers"): "**Faults and deposits are read at the fixpoint.** An evaluation
fault (ch. 1.4) and a §9.5 deposit are outcomes of one form's evaluation on one
subject. Re-evaluation under freshness (ch. 5.1) replaces the earlier outcome,
and a form that does not decide (ch. 7.1, 7.3) is not evaluated; the session
reports the outcomes standing at the fixpoint of the form's stratum."

## Aanvulling (2026-10-09, avond): document D, een deponering onder een initialisatie die niet beslist

Gevonden door een herkeuring tijdens `20261008-tms-expressievormen-en-rekendatum` en door de hoofdsessie nagemeten,
met ref, clingo en de verifier uit `_build` van die worktree en de shaded jar van haar tip (generator
`/tmp/lens-tms-k2/proef/herkeuring1.py`).

Document D: objecttype Jutter met `b` en `heffing` (getallen met 2 decimalen) en één regel:

    Regel heffing van een jutter: De heffing van een Jutter moet
        geïnitialiseerd worden op 7 indien 5 gedeeld door zijn b leeg is.

Case: J1 met heffing 9 in de invoer, b leeg. De initialisatie schrijft niet, want haar doel is in de invoer gevuld;
onder het besluit hierboven beslist ze niet en wordt ze niet uitgerekend. Verwachting: heffing 9, consistent.

| Uitvoerder | D |
|---|---|
| ref | `MISMATCH: expected consistent, but the channel carries: heffing van een jutter × J1` |
| clingo | `MISMATCH: expected consistent, but the model carries 1 inconsistent fact(s)` |
| Drools, exec-modeltak | `EXCLUDE … (voorwaarde buiten Phase-B scope)` |
| Drools, DRL-teksttak | `PLAYED 1` |
| Drools, tak met truth maintenance | `PLAYED 1` |

D is de deponeringsvariant van C: ref rekent de voorwaarde uit vóór de blokkade op het doel. De fix die het besluit
voor C noemt (in ref de blokkade vóór de voorwaarde, in clingo de guard van de `val`-kop) lost D mee op. Neem D mee in
(b) en (c).

## Aanvulling (2026-10-09, nacht): volgordeafhankelijkheid van ref op getuigen van `20261008-tms-verdeling-en-tijdlijnen`

Ref houdt in een deel van de regelvolgordes een tijdlijn of fout van een tussenstand vast, waar de tak met truth
maintenance (en clingo, waar die de vorm vertaalt) de uitkomst aan het fixpunt geeft. De getuigen meten het per volgorde:
`TmsTimelineWitnessTest` deel B2 (een vergelijking met een waarde die een regel later vervangt: in de volgorde jaardeel
vóór teller "MISMATCH … 306 … 59, expected … 4"), `TmsInputFormWitnessTest` deel E en `TmsWholePeriodWitnessTest`
delen A en A3 ("J1.gelijkpremie = 20" in 3 van de 6 volgordes). Dezelfde klasse als de documenten A–D hierboven.

## Acceptatie (bij oppakken aanscherpen)

- (a) Het besluit staat in de spec (beschermd pad: voorstel aan de
  eigenaar), met de documenten A en B als uitgewerkt voorbeeld met absolute
  uitkomsten in beide volgordes.
- (b) Goldens voor A en B, met de regels in beide volgordes (twee documenten
  per vorm, of een andere vorm die de volgorde vastlegt), en de voorbeelden als
  test in `ref/test/`.
- (c) Ref, clingo en beide Drools-takken geven het besloten antwoord in beide
  volgordes.
- (d) Alle gates van `CLAUDE.md` groen; de pins van gate 12 (beschermd:
  voorstel).

## Omvang

Middel: een normbesluit en drie executors. Waar het besluit "een fout telt
alleen aan het fixpunt" wordt, raakt het ook de manier waarop ref een fout
meldt (nu meteen bij de evaluatie).

May touch: `spec/RUNTIME-SEMANTICS.md` (voorstel), `ref/**`,
`backends/clingo/**`, `backends/drools/**` (exec-modeltak en DRL-teksttak),
`corpus/golden/<nieuwe goldens>/`, `scripts/check-drools.sh` (pins;
voorstel), kaartverplaatsing.

## Raakvlakken

`backlog/new/20261008-deling-door-leeg-onder-initialisatie.md` gaat over
hetzelfde gebied (wat een initialisatie die niet beslist, mag doen) en kan in
dezelfde sessie worden gewerkt. De kaart `20261007-drools-logische-afleiding`
legt deze meting vast in haar `Result:`.

Na het besluit van 2026-10-08 (PLAN §9) gaan beide Drools-takken op truth
maintenance:

- `20261008-tms-aggregaties-creatie-en-fouten` voert een fout als logisch feit
  dat pas aan het fixpunt telt, omdat die tak tussenstanden bezoekt die ref niet
  ziet. Zijn `Result:` geeft wat de tak op de documenten A en B antwoordt, in
  beide volgordes. Twee weigeringen van die tak hangen van het besluit hier af:
  een deling in een default, en een deling in een initialisatie naast een
  andere schrijver.
- Voor (c) zijn de Drools-takken na `20261008-tms-omschakeling` en
  `20261008-drl-teksttak-op-truth-maintenance` de takken met truth maintenance.
  Landt deze kaart eerder, dan geldt (c) voor de takken die er dan zijn.
- Dezelfde vraag geldt voor een deponering op een tussenstand: de tak met truth
  maintenance trekt haar in, ref houdt haar. RUNTIME-SEMANTICS ch. 6.1
  ("Recorded residuals") legt dat laatste al vast als residu dat op het
  defaultsmodel van ch. 7 wacht. Neem het mee in het besluit.

## Na `20261008-tms-omschakeling` (2026-10-11)

Na `20261008-tms-omschakeling` (2026-10-10) is de exec-model-tak de tak met truth maintenance; de rijen 'Drools,
exec-modeltak' in de tabellen beschrijven de verwijderde destructieve verlaging. De twee weigeringen die op dit
besluit wachten (een deling in een default, een deling in een initialisatie naast een andere schrijver) staan nu in de
exec-model-tak en zijn dekkingsverlies (punt 2 van de lijst in het `Result:` van die kaart); (c) geldt voor de
exec-model-tak en de DRL-teksttak.

## Result:

Gebouwd op main `7c0220d11` in de worktree `wt/fout-op-tussenstand`. De kaart blijft in `doing/`; de hoofdsessie landt.

### Commits

- `e1e7c507f` claim
- `9d05d1dd6` ref: fout en deponering per (vorm, onderwerp), vervangen bij her-evaluatie, gemeld aan het fixpunt van het stratum; de blokkade van een initialisatie vóór haar voorwaarde; `WV-5.7-fixpoint`
- `57a33a48a` ref: een andere schrijver die de waarde van de initialisatie schrijft, verdringt haar ook (gevonden door gate 17)
- `aae4466a1` clingo: fail-fasts en deponeringen krijgen de bewaking van de `val`-kop; README [S3], [S4] weg
- `2c1f05cdc` Drools: DROOLS-120 en DROOLS-165 `RETIRED`, de vormen spelen; getuigen aangepast
- `1b4b30610` spec (voorstel): `RUNTIME-SEMANTICS.md` ch. 5.4, 5.6, 5.7, 5.7b, 6.1, 7.1, 7.5, 7.8; `ASP-TRANSLATION.md`
- `dff96768f` goldens en rode cases; `EXPECTED_PLAYED` en de lijst van gate 8 (voorstel)
- de commit met dit Result

### Meettabel

De proefdocumenten staan onder `werk/fout-op-tussenstand/probe/`; D heeft naast de regel van de kaart een tweede regel (`grondslag`), zodat het document een volgorde heeft. E is toegevoegd: B met initialisatie 7, zodat de tussenstand een harde fout geeft (90/7 past niet, EF-006).

Op main `7c0220d11` (case zoals in de kaart; `v` = voorwaarts, `o` = omgekeerd):

| | ref | clingo | Drools exec en DRL (gelijk) |
|---|---|---|---|
| A v | PASS | `expected consistent, but the model carries 1 inconsistent fact(s)` | `EXCLUDE DROOLS-120` |
| A o | `channel carries: standaardbergloon × V1` | idem | `EXCLUDE DROOLS-120` |
| B v | PASS | PASS | PLAYED 1 |
| B o | `channel carries: aandeel × V1` | PASS | PLAYED 1 |
| C v, o | `channel carries: standaardtoeslag × V2` | `model carries 1 inconsistent fact(s)` | PLAYED 1 |
| D v, o | `channel carries: heffing van een jutter × J1` | `model carries 1 inconsistent fact(s)` | PLAYED 1 |
| E v | PASS | PASS | PLAYED 1 |
| E o | `EVALUATION FAULT: EF-006: V1.aandeel: value 90/7 …` | PASS | PLAYED 1 |

Sinds de kaart `zero-division-channel` zijn de uitkomsten "EVALUATION FAULT" en `viol(...)` van de kaart bij A, B en C een kanaalfeit EF-005 geworden; `DelingDoorNul` bestaat niet meer. Bij clingo droeg het feit bij A de verkeerde naam (`erkenning × V5`: `ctx.rule_name` bleef staan van de vorige regel).

Op de eindstand: elke cel van A, B, C, D en E in beide volgordes en op alle vier de uitvoerders `PASS` of `PLAYED 1`, met A bergloon 30; B bergloon 30 en aandeel 3; C toeslag 7; D heffing 9; E bergloon 30 en aandeel 3; alle consistent (`werk/fout-op-tussenstand/logs/measure-final.log`).

### Wat er veranderde

**Spec (voorstel).** De alinea "Faults and deposits are read at the fixpoint" in ch. 5.7 na "Divergence by two writers", met de voorgestelde tekst letterlijk, drie zinnen eraan (een fout staat in plaats van de write; meerdere fouten noemt het rapport alle; de blokkade komt vóór voorwaarde en waarde) en uitgewerkte voorbeelden A, B, C, D met absolute uitkomsten. Vervallen: de zin van ch. 6.1 over deponeringen (residu (b)) en de zin van ch. 7.1 dat tijdelijke deponeringen blijven staan. Ch. 5.4 krijgt de zin dat een andere schrijver met dezelfde waarde de initialisatie ook verdringt, ch. 5.6 één zin, ch. 7.5 en 7.8 de verwijzing, en de tabel van ch. 5.7b twee getuigen bij EF-005 en één bij EF-006.

**Ref.** `World.t` draagt `outcomes`: per (form, at) de deponeringen of de fout van de laatste evaluatie. `channel` is daaruit afgeleid. `Eval.over_subjects` vangt `Value.Fault` per onderwerp, zet de wereld terug op die van vóór de evaluatie en bewaart de fout; `raise_standing_fault` meldt aan het einde van elk stratum, na generaties en checks. Een geblokkeerde initialisatie rekent voorwaarde en waarde niet uit. Een uniciteit heeft haar uitkomst onder `at = ""`.

**Clingo.** `translate_rule`: de voorwaarde van een initialisatie krijgt `init_guard` als `rule_guard`. `translate_chain`: voorwaarde en gevolg van een default krijgen `reached n`, de rechtvaardiging en bij een initialisatiedefault `not hi(S)`. Nieuw veld `deposit_guard`, alleen op `deposit`: een gedeclareerde default deponeert onder `decN(S,_)`; in `rule_guard` zou dat zijn eigen premisse zijn, omdat de waardehelpers `rule_guard` dragen. Het kanaal heet nu de doelvorm, of bij een uit v0 ontsuikerde vorm de regelnaam.

**Drools.** `Consequence.Decision` draagt `deposit` (twee weergaven: `PatternDslRendering`, `DrlRendering`): waar het besluit gevuld is, deponeert het wat de sonde hoorde; bij leeg niets. Een deling in de rechtvaardiging gaat via `depositRule` met `decidedBy`: de deponering staat zolang `Decides` of `Outcome` van die default staat. De rol `CONDITION_DEPOSIT` komt nu ook bij `Kind.DEFAULT` voor. De twee refusal-plaatsen en `division(Condition, Expr)` zijn weg. De wortel in een default blijft geweigerd.

### Goldens

Veertien goldens, elk document in beide volgordes (achtervoegsel `-omgekeerd`), met `source.rgs`, `ir.json`, `NOTE.md`:

| golden | cases per volgorde |
|---|---|
| `jutterswet-initialisatie-naast-regel` (A) | 5 |
| `jutterswet-lezer-van-initialisatie` (B) | 3 |
| `jutterswet-voorwaarde-onder-invoer` (C) | 4 |
| `jutterswet-deponering-onder-invoer` (D) | 4 |
| `jutterswet-fout-op-tussenstand` (E) | 2 |
| `jutterswet-deling-in-afwijking` (declared groups, DROOLS-165) | 10 |
| `jutterswet-lege-initialisatie-naast-regel` (een initialisatie die niets opslaat) | 1 |

Zes rode cases, elk één case: `corpus/red/jutterswet-fout-aan-fixpunt`, `jutterswet-initialisatie-past-niet`, `jutterswet-afwijking-past-niet`, elk met `-omgekeerd`, EF-006.

### Rood eerst

Op de binaries van `7c0220d11` (`logs/red-final.log`), per golden het aantal rode cases:

| golden | ref | clingo | Drools |
|---|---|---|---|
| initialisatie-naast-regel | 0 van 5 | 4 van 5 | EXCLUDE DROOLS-120 |
| -omgekeerd | 2 van 5 | 4 van 5 | EXCLUDE DROOLS-120 |
| lezer-van-initialisatie | 0 van 3 | 0 | PLAYED 3 |
| -omgekeerd | 1 van 3 | 0 | PLAYED 3 |
| voorwaarde-onder-invoer, beide | 1 van 4 | 2 van 4 | PLAYED 4 |
| deponering-onder-invoer, beide | 1 van 4 | 1 van 4 | PLAYED 4 |
| fout-op-tussenstand | 0 van 2 | 0 | PLAYED 2 |
| -omgekeerd | 2 van 2 | 0 | PLAYED 2 |
| deling-in-afwijking, beide | 0 van 10 | 8 van 10 | EXCLUDE DROOLS-165 |

Twee goldens zijn op de basis dus op geen enkele uitvoerder rood (`lezer-van-initialisatie` en `fout-op-tussenstand` voorwaarts): dat zijn de controles van hun omgekeerde tweeling. De flankcases en de rode cases van `jutterswet-fout-aan-fixpunt` waren op ref en clingo al groen, omdat ze vastleggen wat moet blijven; zij gaan rood onder M4 hieronder. De rode cases `initialisatie-past-niet` en `afwijking-past-niet` waren op Drools een weigering.

`ref/test` op de basis met de nieuwe tests: 54 keer FAIL, onder meer `WV-5.7-fixpoint-a [erkenning, standaardbergloon, bergloon] (expected consistent, but the channel carries: standaardbergloon × V1)` en `WV-5.7-fixpoint-fault-between … (fault: V1.aandeel: value 90/7 does not fit …)`. Getuigen Drools op de basis: de oude tekst pinde de weigering.

### Mutaties (niet gecommit; `werk/fout-op-tussenstand/probe/mutate_ref.sh`, `mutate_rest.py`, logs `mut-*.log`)

| | recept | uitkomst |
|---|---|---|
| M1 | ref: `blocked` altijd onwaar | onder meer `WV-5.7-fixpoint-c`, `-d` rood |
| M2 | `set_outcome`: lege uitkomst verwijdert niet | `-a` 5, `-b` 20, `-fault-between` 20 |
| M3 | `over_subjects`: fout niet vangen (`when false`) | `-a-nonexact` 5, `-fault-between` 12 |
| M4 | `raise_standing_fault` niet aanroepen | `-fault-stands`, en de WF-rijen die een fout verwachten |
| M5 | `write_attr` laat `init_written` staan | `-a-equal` 5, `-a` 5, `-b` 20 |
| M6 | eigendomsverlies telt niet als verandering | `-a-equal` 1 |
| C1 | clingo: `deposit_guard` leeg | deling-in-afwijking case 2, 3, 9 |
| C2 | `rule_guard` van het gevolg leeg | A case 1, 2; deling case 5, 6 |
| C3 | `rule_guard` van de rechtvaardiging leeg | deling case 5, 6 |
| C4 | `init_guard` niet op de voorwaarde | C case 1, 2; D case 1 |
| C5 | kanaalsleutel altijd de doelvorm | A case 5 |
| D1 | Drools: `Decision` zonder `deposit` | exec en DRL: deling case 4 |
| D2 | `depositRule` zonder `decidedBy` | exec en DRL: deling case 9 |
| D3 | exec: deponering vóór de leeg-toets | exec: deling case 2, 3; DRL `PLAYED 10` |
| D4 | DRL: `decisionOf` zonder deponering | DRL: deling case 4; exec `PLAYED 10` |

### Aangepaste verwachtingen en getuigen

Geen vloer verlaagd. `EXPECTED_PLAYED` 281 → 295. In `scripts/check-red.sh` zes `NOFAULT`-regels erbij (reden hieronder).

Suite van clingo (`test_clingo.ml`), omdat ref op de tussenstand niet meer faalt:
- 15 rijen `named_refusal ~ref_plays:false` → `~ref_plays:true`: tr-cr-1, tr-w-1, tr-pm-1, tr-pm2-1, tr-pm2-ctl, tr-kv-1, tr-kv-listed, r6-dmd-1, r6-ebar, r6-alle-fold, r6-pres, tr-of-1, r6-of-alle, r6-agg-comp, r6-nav. Oud: ref faalt op de pass vóór de creatie. Nieuw: ref speelt, clingo weigert nog in de genoemde zin.
- tr-pm2-ctl: de rijen `hb` (bonus leeg) → bonus 4, consistent. De oude rijen zijn nooit gelezen, omdat ref faalde; 4 volgt met de hand uit restpost 1 en de gemaakte schuit.
- 4 rijen `pinned_silent` → `both_answer`: t1 ([S3]), r6-alle-nul, r6-agg-ite, r6-regelcond-nul ([S4]). De README-afwijkingen [S3] en [S4] zijn weg; [S3] noemde dit "a norm question for the maintainer".

Drools:
- `DefaultsDivisionWitnessTest`: vijf rijen `refused` → `played` met dezelfde waarden; `a_division_under_another_operation_stays_refused` → `…_decides_with_its_deposit` (V3 5,00 en `Vondst.verhouding` EF-031); nieuw `a_default_that_a_higher_one_displaces_does_not_deposit`. De wortelrij blijft geweigerd.
- `ZeroDivisorWitnessTest.a_zero_divisor_in_the_consequence_of_a_default`: weigering → V1 0,75, V3 1,00, consistent, beide takken.
- `TmsInitialisationWitnessTest.a_division_in_an_initialisation_beside_another_writer_is_refused` → `…_counts_at_the_fixpoint`: elke volgorde, beide takken, V1 30 consistent en V2 leeg met EF-005.
- `TmsCreationInputWitnessTest.a_division_in_an_initialisation_beside_a_writer_on_a_sister_subtype`: weigering met de supertype-regel → `gespeeld`.
- `LetCarrierWalkWitnessTest.the_defaults_division_fence_fires_through_a_let` → `a_division_behind_a_let_in_a_default_plays`: toeslag 5,0000.
- `CompanionPathWitnessTest.the_defaults_division_fence_stays_standing_for_a_division_in_a_letbody` → `a_division_in_a_let_body_in_a_default_falls_through`: toeslag 0,0000, consistent.
- `FilterFaultWitnessTest.a_defaults_group_with_a_fold_that_can_fail_is_refused` → `…_plays_the_higher_default`: J1 1, consistent.
- `StackCompositionWitnessTest.a_refused_stack_…`: het apparaat voor de geweigerde stack is nu een wortel in plaats van een deling.
- `DefaultsplanWitnessTest`: 22 plannen `, facts=[` → `, deposit=null, facts=[`, het nieuwe recordveld in de dump.
- `DepositFlagSlotWitnessTest`: een `Decision` met `deposit` vult haar sonde (nieuwe tak in de switch).

### Getuigen uit de aanvulling (ref, elke volgorde, `logs/witness-1.log`)

Documenten uitgeschreven met de `main` van de testklassen.

- **`TmsInputFormWitnessTest` deel E: dezelfde klasse, gerepareerd.** case2: basis 5 van 6 volgordes PASS, 1 keer fout over V4; nu 6 van 6. case en case3 faalden en falen in elke volgorde: die fout staat aan het fixpunt, zoals de getuige beschrijft.
- **`TmsTimelineWitnessTest` B2: andere oorzaak, blijft staan.** 1 van 2 volgordes `J1.jaardeel = … 306 … 59, expected … 4`, op de basis en nu gelijk. Ref houdt een geschreven tijdlijn vast waarvan de voorwaarde later onwaar wordt.
- **`TmsWholePeriodWitnessTest` A: andere oorzaak, blijft staan.** 8 van 24 volgordes PASS, op de basis en nu gelijk (`J1.stilpremie = 30`, `J2.trouwpremie = 10`): hetzelfde.
- **`TmsWholePeriodWitnessTest` A3: andere oorzaak, blijft staan.** 0 van 6 PASS, gelijk: `J5.ongelijkpremie = 10` in elke volgorde (de vergelijking tegen leeg, ch. 1.2), `J1.gelijkpremie = 20` in 3 volgordes (geschreven waarde).

De gedeelde oorzaak bij B2, A en het tweede deel van A3 is de geschreven waarde die blijft staan: het andere residu van ch. 6.1 ("a condition-guarded attribute write whose condition later turns false keeps its written value"). Het besluit van 2026-10-09 noemt fout en deponering, niet de waarde, dus dit is niet gerepareerd.

### `deling-door-leeg-onder-initialisatie`

Op de eindstand geven beide cases op ref, clingo, exec en DRL `PASS` of `PLAYED 1`: case 1 `aandeel` leeg en inconsistent onder `standaardaandeel`, case 2 `aandeel` 5, consistent. De twee gemeten afwijkingen van die kaart (clingo's deponering en de naam `?`) zijn daarmee weg. Van haar acceptatie blijft: een eigen golden met die vorm (A case 3 en 5 dekken hem op één initialisatie met andere schrijver) en (c), de andere kanaalproducenten onder een initialisatie, niet nagelopen. De hoofdsessie kan haar sluiten of tot die twee punten terugbrengen.

### Voorlopige besluiten

1. **Twee leesregels voor een default.** Een gedeclareerde default deponeert alleen waar hij beslist (ch. 7.1). Een uit v0 ontsuikerde vorm deponeert waar hij wordt uitgerekend, ook bij een lege waarde, onder de regelnaam, want ch. 7.8 en `deling-door-leeg-onder-initialisatie` case 1 eisen dat, en ref en Drools doen het zo. Alternatief: ook de initialisatie speculatief, wat ch. 7.3 letterlijk zou kunnen dragen, maar dat verandert case 1 en de flank van A.
2. **Meerdere staande fouten.** Ref rapporteert code en parameters van de eerste in sorteervolgorde (onafhankelijk van de documentvolgorde), de teksten van alle in de tekst. `Value.Fault` draagt één code; gate 8 leest één `CODE`-regel van ref. Geen rode case raakt dit.
3. **Alle fouten uit een evaluatie op een onderwerp zijn uitgesteld**, ook EF-015 en EF-017. Fouten buiten een onderwerp (een geldigheid zonder datum, een statische weigering) blijven meteen vallen; EF-016 ook, want dat is geen uitkomst van één vorm.
4. **Eigenaarschap telt als verandering.** Schrijft een andere schrijver de waarde die de initialisatie al schreef, dan geeft `write_attr` `changed = true`, omdat de blokkade dat leest. Zonder dat bleef de deponering van de initialisatie staan (M6). Gevonden doordat gate 17 ref's trace (`HOLDS`) tegen Drools (`DISPLACED`) hield op `jutterswet-lezer-van-initialisatie-omgekeerd` case 3. Een initialisatie die niets opslaat, neemt geen eigenaarschap: anders meldt `write_attr` elke pas een verandering op leeg-over-leeg en bereikt ref geen fixpunt (`jutterswet-lege-initialisatie-naast-regel`, rood op `4e00dd02d`: `no fixpoint within 100 passes`; `WV-5.7-fixpoint-a-empty`).
5. **Rode EF-006-flanken staan op Drools als `NOFAULT`.** Die tak kent geen schrijfgrens (de bestaande regel `decimal-nonterminating`). De flank "een fout aan het fixpunt blijft gemeld" is voor Drools gedekt door `TmsFaultWitnessTest` (wortel) en voor de deponeringen door de goldens.
6. **Golden E en de golden voor gedeclareerde groepen zijn extra** ten opzichte van de kaart, omdat EF-005 geen harde fout meer is en anders niets de harde fout en DROOLS-165 in de corpus vastlegt.

Fable-advies van 2026-11-01 (`/Users/yeb/Developer/uchar-dev/werk/fout-op-tussenstand/fable/advies.md`): besluiten 1, 2 en 3 behouden, 4 met de aanvulling hierboven. De zin in ch. 5.7 zegt daarom "a default a higher default displaces (ch. 7.1), or an initialisation whose guard blocks it (ch. 5.4, 7.3), is not evaluated", in plaats van de voorgestelde zin "a form that does not decide (ch. 7.1, 7.3) is not evaluated" uit het besluit van 2026-10-09: een regel die leeg uitrekent, beslist niet en wordt wel uitgerekend (ch. 7.8). Dat is een voorlopig besluit. Vastgelegde zwakte bij besluit 2: de tekst noemt alle fouten, de machinelijn de eerste (`runner.ml`, één `CODE`-regel); geen rode case raakt dit.

### Voorstel `spec/CONSTRUCTS.md`

Rij 8, achteraan: "**Faults and deposits at the fixpoint (2026-11-01, card `fout-op-tussenstand`, RUNTIME-SEMANTICS ch. 5.7):** an evaluation fault and a §9.5 deposit are outcomes of one form's evaluation on one subject; re-evaluation replaces them, a form that does not decide is not evaluated, and the session reports what stands at the fixpoint of the form's stratum. Tracked delta against v0's persist-always deposits and immediate faults. A declared default deposits only where it decides; a v0 form the desugaring reads as a default deposits where it is evaluated. Goldens `jutterswet-initialisatie-naast-regel`, `jutterswet-lezer-van-initialisatie`, `jutterswet-voorwaarde-onder-invoer`, `jutterswet-deponering-onder-invoer`, `jutterswet-fout-op-tussenstand`, `jutterswet-deling-in-afwijking`, `jutterswet-lege-initialisatie-naast-regel`, each in both rule orders."

### Voorstel `spec/PLAN.md`

In de alinea "Faults and deposits count at the fixpoint (2026-10-09 …)": "to be built" vervalt, en erachter: "Built 2026-11-01. A write whose condition later turns false keeps its value in the reference interpreter: the residual of ch. 6.1 stays, and `TmsTimelineWitnessTest` B2 and `TmsWholePeriodWitnessTest` A and A3 still measure it." In de alinea rond regel 1065 vervalt "Recorded, not opened: this branch withdraws a deposit made at an intermediate state where ref keeps it …".

### Wat bleef liggen

- De geschreven waarde die blijft staan (B2, A, A3 hierboven): een bug tegen ch. 5.2, kaart `backlog/new/20261101-written-value-at-fixpoint.md`.
- Clingo weigert de 15 `named_refusal`-rijen nog in de genoemde zin, ook waar ref nu speelt. De README zegt het; de weigering is te ruim geworden en niet onjuist. Een kaart kan haar versmallen.
- `IR-SPEC.md` regel 8899 en `RUNTIME-SEMANTICS.md` ch. 6.1 ("channel facts are never retracted") gaan over consistentie en uniciteit, die aan het fixpunt van hun stratum vuren. Ze spreken het besluit niet tegen, maar lezen stug naast ch. 5.7. Ik heb ze niet aangeraakt (IR-SPEC viel buiten de opdracht).
- Ref's `run_consistency` vuurt nog steeds pas na de laatste generatie; dat hoeft niet meer voor de juistheid, omdat de uitkomst nu vervangt.
- `vouwfilter-resterende-vormen` niet aangeraakt. Een deling in een vouwfilter onder een gedeclareerde default is wel gemeten: het proefdocument (`jutterswet`-model van `fold-filter-divides-by-zero`, `zware vondsten` als default) geeft op ref, clingo, exec en DRL 1 met EF-005 onder `Jutter.zware vondsten`; er is geen golden voor.

### Gates

`make gates` op `a3c5c9dcc`: alle 17 groen, 339 s, `red=0`, `out/gates/20261101-123755/`. Gate 12 speelt 295 op beide takken.
