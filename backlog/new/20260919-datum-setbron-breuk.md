# datum-setbron-breuk: SOM telt dagnummers op waar ref een typefout werpt

Gemeten (kaart eerste-laatste-setvouw-kloof, 2026-09-19, §1; door de
lens onafhankelijk gereproduceerd): op een verifier-SCHOON document met
een DATUM-setbron splijten de niet-ordenende vouwsoorten het vierkant,
en de splijting is per uitvoerder anders:

- **SOM / TOTAAL**: `ref` werpt `plus: expected a number, got
  2024-01-15` (datums sommeren is een typefout), de ASP-backend
  **antwoordt 39520** — de som van de dagnummers 19737+19783 — en de
  Drools exec-tak gooit een `ClassCastException` (`LocalDate` naar
  `BigDecimal`); de DRL-tak weigert op compile. Drie uitvoerders, drie
  gedragingen, waarvan één een stellig getal.
- **MINIMUM / MAXIMUM**: `ref` en clingo zijn het eens (2024-01-15 /
  2024-03-01 — de ordenende lezing is welgedefinieerd op datums), maar
  de Drools exec-tak gooit (`kan accumulate-resultaat niet naar
  BigDecimal omzetten`) en de DRL-tak weigert op compile — een crash is
  geen benoemde weigering.

Er is geen producentroute: de RegelSpraak-frontend typt de zin weg
(`som van vereist numerieke operanden, kreeg Datum in dagen`). De vorm
is dus alleen met de hand op de wire te schrijven — maar hij is
verifier-schoon, en het gate-8-principe zegt dat een stellig fout
antwoord (clingo's dagnummersom) erger is dan een fout.

Kandidaatrichting (te wegen bij oppakken, `ref` is de arbiter): de
ASP-vertaling weigert of fault SOM/TOTAAL over een datum-setbron zoals
`ref` dat doet (rood-getuige mogelijk zodra beide OCaml-uitvoerders
observably falen); de Drools-takken weigeren per construct met reden in
plaats van te crashen; MIN/MAX over datums blijft de gedragen ordenende
lezing (ref ≡ clingo staat er al — een golden kan die helft dichten).

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst per soort en per uitvoerder; de meting van §1 hermeten
  op de kop van de kaart.
- (b) Geen stellig antwoord meer op een vorm waar de arbiter faalt;
  crashes worden benoemde weigeringen; elk gedicht deel krijgt zijn
  getuige (golden waar twee uitvoerders dragen, rood waar beide OCaml-
  uitvoerders falen).
- (c) Corpussweep beide Drools-takken; kanaal-/roodgevolgen als
  gemandateerd beschermd-padvoorstel; alle veertien gates groen.

May touch: `backends/clingo/**`, `backends/drools/**` (weigeringen),
`corpus/golden/**` en `corpus/rood/**`, `scripts/check-drools.sh` en
`scripts/check-rood.sh` uitsluitend telling/lijst (voorstel),
kaartverplaatsing. Out of scope: `ref/` (arbiter), `frontends/**`.
NB: serieel met andere `translate.ml`- en DrlTranspiler-kaarten.
