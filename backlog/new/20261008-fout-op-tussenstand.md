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
