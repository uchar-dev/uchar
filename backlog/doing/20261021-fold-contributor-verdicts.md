# fold-contributor-verdicts: de lezing van een kwantorvouw legt het oordeel van elk lid vast, met de stop als positie

*Vastgelegd door de hoofdsessie op 2026-10-21 uit Fable-advies 2
(`werk/defaults-decider/fable-advies-2.md` in `uchar-dev`, §3 weg A, §4). Besluit van de eigenaar 2026-10-21: weg A (de
lezing legt het oordeel van elk lid vast), niet B (de lezing blijft bij de stopregel en de uitleg beperkt zich daartoe);
het advies van Fable wordt meegenomen.*

## Aanleiding

RUNTIME-SEMANTICS ch. 5.12 legt de bijdragers van een kwantorvouw na de stop vast als `unreached`, zonder oordeel. De
Drools-backend evalueert de matrix per lid in een eigen regel en geeft elk lid een oordeel; waar de matrix falen noch
deponeren kan, houdt zijn sessie geen volgorde en schrijft hij `unordered` (211 van de 264 vouwen in de vergelijking van
gate 17, 80 %). De canonieke tekst van 5.12 is daardoor alleen canoniek via de afwijkende vergelijkingsregel van gate 17.
De toolchain-kaart `explanation-fold-members` (besluit van de eigenaar: ook leden na de stop) leest die oordelen; onder
de huidige 5.12 is dat informatie die alleen Drools levert.

Daarnaast verschillen ref en Drools al op twee plaatsen die gate 17 niet bereikt: na een lid met een fout (ref schrijft
niets, Drools `unreached`) en bij een filter met een fout (ref geen bijdragers, Drools alle `unreached`).

## Doel (weg A)

- **Spec, ch. 5.12** (voorstel; beschermd pad): bij "Folds over a set" per bijdrager het oordeel van de matrix of zijn
  fout, de tabel van ch. 3.9a's `test(m)` over de bijdragers, op de fixpuntwereld zoals een lezing evalueert, zonder
  deponering, plus of de evaluatie het lid bereikte; een oordeel of fout na de stop is van de lezing, niet van de
  sessie, en verandert de status niet; bij een filter met een fout geen bijdragers. De canonieke tekst
  `contributors [<object> <verdict|fault> reached|unreached; …]` (`- unreached` vervalt) en één zin over `unordered`
  als erkende "geen positie" (naar ch. 5.11). De definitiezin krijgt "and, for a fold over a set, the table its
  definition in ch. 3.9a fixes". Twee grenzen als zin in "The artifacts": `FAULTS` blijft buiten het differentieel (een
  golden faalt niet aan het fixpunt); een oordeel van een niet-bereikt lid kan rusten op een deponering die de lezing
  wegliet.
- **ref**: in de leesmodus (alleen met waarnemer; gate 5 bytegelijk) na de stop en vóór een fout elk overgebleven lid
  evalueren en met `reached = false` bijdragen; `Member_unreached` vervalt. Een ref-test `WV-5.12-<golden>` die de tabel
  uit een golden leest.
- **Drools**: het filter met een fout geeft geen bijdragers (één regel, een getuige in `ReadingsWitnessTest`); de
  javadoc van `Contributor.reached`: de positie, `null` waar de vouw er geen heeft.
- **Gate 17**: de vergelijkingsregel per bijdrager letterlijk op lid en oordeel of fout, de positie waar beide er een
  schrijven; de rij "unreached without a verdict agrees" keert om; een struikeldraad: schrijft Drools `unordered`, dan
  draagt geen bijdrager van ref een fout; een vloer `EXPECTED_MIN_UNREACHED_CONTRIBUTORS` (bij de landing gemeten).
- **Een getuige-golden**: een vouw `all` waarvan lid 1 niet geldt (stop) en waarvan de matrix op lid 2 de rekendatum
  leest in een sessie zonder datum (EF-011); het kanaal blijft `consistent`; de lezing toont `fault EF-011 unreached` op
  beide kanten.

## Acceptatie

- (a) Rood eerst: de nieuwe ref-test en de getuige-golden rood op de stand vóór de kaart.
- (b) Mutant: ref die na de stop de leden als `reached` schrijft maakt gate 17 rood.
- (c) De zeventien gates groen; gate 5 bytegelijk.

May touch: `spec/RUNTIME-SEMANTICS.md` ch. 5.12 (voorstel), `ref/lib/eval.ml`, `ref/lib/reading.ml`, `ref/test/`,
`backends/drools` (het filter, de javadoc, een getuige), `scripts/check-readings.sh` (voorstel), één golden, deze kaart.

Out of scope: de toolchain (geen API-wijziging; de pin-bump van `fixpoint-inconsistency-order` brengt de spec mee);
deponeringen naast het oordeel in de tabel (pas als de vorm in een regelset voorkomt).
