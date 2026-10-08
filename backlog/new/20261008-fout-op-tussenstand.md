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

## Vraag aan de eigenaar (spec)

Telt een fout op een tussenstand? Voorgesteld antwoord: nee. Een fout is, net
als een waarde, een eigenschap van het fixpunt (ch. 5.2); een initialisatie die
niet beslist, wordt niet uitgerekend en geeft dus geen fout (ch. 7.1). Dan is
het antwoord bij A bergloon 30 en bij B aandeel 3, in elke volgorde, en wijken
ref (beide), clingo (A) en de Drools-takken (beide) af. Het alternatief is een
voorgeschreven evaluatievolgorde waarin een lezer pas na al zijn schrijvers
rekent; dat lost B op, maar A niet.

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
