# defaults-negation-at-bottom-without-evaluation-date

Een besluit tot niet toekennen onderaan een defaults-groep, waarvan de voorwaarde het rekenjaar leest, in een sessie
zonder rekendatum. Gevonden bij de kaart `defaults-decider` (2026-10-21). `ir.json` komt uit de RegelSpraak-frontend,
van deze bron:

    Objecttype de Vondst (mv: Vondsten)
        het vondstjaar Numeriek (geheel getal);
        het gewicht Numeriek (geheel getal);
        is oud kenmerk (bijvoeglijk);

    Regel ouderdom
        geldig altijd
            Een Vondst is niet oud
            indien zijn vondstjaar groter is dan het Rekenjaar.

    Afwijking van ouderdom: zeer zware vondst
        geldig altijd
            Een Vondst is oud
            indien zijn gewicht groter is dan 100.

De groep op `oud` heeft twee defaults: bovenaan `zeer zware vondst` (toekennen, `gewicht > 100`), onderaan `ouderdom`
(niet toekennen, `vondstjaar > evaluationYear`). De case heeft één Vondst V1 (vondstjaar 2026, gewicht 50) en
`asOfDate: null`.

## Handberekening

`RUNTIME-SEMANTICS.md` ch. 7.1: de groep evalueert van boven naar beneden tot een default beslist. De bovenste beslist
niet (50 is niet groter dan 100). Dan komt de onderste aan de beurt, en haar voorwaarde leest het rekenjaar. Ch. 8.2
leidt het rekenjaar af uit de rekendatum van de sessie, en die ontbreekt: de evaluatie faalt met EF-011 (ch. 5.7b),
nooit met een standaardjaar en nooit met leeg.

## Wat elke uitvoerder zegt (gemeten 2026-10-21)

- `ref`: `EVALUATION FAULT: EF-011: the evaluationYear context value needs an evaluation date; the case carries
  asOfDate null`;
- de ASP-backend: `TRANSLATION FAULT: EF-011: …`, dezelfde zin;
- de Drools-backend, in de rode run: `FAULT EF-011 {}: ComputeFault: error in the rule "defaults K:Vondst.oud": the
  evaluationYear context value needs an evaluation date; the case carries asOfDate null`.

## Waarom deze case er is

Vóór de kaart `defaults-decider` gaf de TMS-lowering van de Drools-backend een besluit tot niet toekennen onderaan een
eenvoudige stapel geen regel: het verdringt niets, dus de lowering sloeg het over. Daarmee werd de voorwaarde nooit
geëvalueerd, en de Drools-backend speelde deze case stil uit (`NOFAULT ran to a model without faulting`, gemeten met
de jar op uchar `d9c0b9a5`), terwijl `ref` en de ASP-backend faalden. Sinds die kaart krijgt elk besluit een regel,
omdat `DroolsBackend.decisionsOf` moet kunnen zeggen welke default besliste; de fout komt nu ook in Drools. Gate 8 houdt
de drie uitvoerders aan deze uitkomst.

## Falsifieerbaar

Op `d9c0b9a5` (de lowering zonder die regel) geeft de Drools-run `NOFAULT`, wat gate 8 weigert voor een case die niet
in de lijst van de Drools-leg staat.
