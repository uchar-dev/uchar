# ref-trace-and-readings-gate: ref als getuige van de lezingen, en de leesclausule

*Vastgelegd door de hoofdsessie op 2026-10-21, kaart U3 uit het Fable-advies over de waarden in de uitleg
(`research/uitleg-lezingen-2026-10-21/fable-advies-2026-10-21.md` in `uchar-dev/research`, §3.5, §5, §6). Besluit van de
eigenaar 2026-10-21: de lezing krijgt een plaats in de spec, met ref als tweede uitvoerder en een gate. Volgt op
`fixpoint-readings`.*

## Doel

`ref --trace <ir> <case>` print per (vormpointer, onderwerplabel, knoopnummer) hetzelfde record als de lezingen van
de Drools-backend, in canonieke tekst (getallen als exacte breuk of als decimaal zoals de case-vergelijking ze
canoniseert, datums als in de cases), op de eindwereld. RUNTIME-SEMANTICS ch. 5 krijgt een paragraaf "The reading of a
condition at the fixpoint" (per vorm met een voorwaarde en per onderwerp: de evaluatie op de fixpunt-wereld in de
volgorde van ch. 5.10 en 5.11, per knoop bereikt of niet, oordeel of waarde, fout; een vorm buiten haar geldigheid en
een verdrongen default hebben geen lezing; een lezing deponeert niets), met een uitgewerkt voorbeeld (een golden met een
samenstelling die stopt) dat een ref-test letterlijk toetst. `scripts/check-readings.sh` (gate 17) vergelijkt per
golden-case-bestand de trace van ref met de lezingen van Drools (beide takken), met `EXPECTED_MIN_*`-vloeren en een
redendragende afwijkingenlijst die leeg begint en in beide richtingen op veroudering faalt.

## Acceptatie

- (a) Rood met een mutant op beide kanten (een `Compound` die niet stopt, of een lid dat wordt bereikt terwijl de
  stopregel al vaststond), elk één keer.
- (b) Gate 17 groen over het corpus, met vloeren; gate 1 groen (geen deling van code tussen `ref/` en
  `backends/drools`).
- (c) Alle gates groen; voorgestelde `CLAUDE.md`-tekst voor gate 17 in de Result.

May touch: `ref/`, `spec/RUNTIME-SEMANTICS.md`, `scripts/check-readings.sh`, `.github/workflows/gates.yml` (de gate in
de volgorde), `spec/verifier/` alleen als de gedeelde lader iets nodig heeft; beschermde paden als voorstel, de eigenaar
merget; de kaart.
