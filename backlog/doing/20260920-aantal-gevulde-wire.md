# AANTAL_GEVULDE, kaart 1: de wire, de poort en `ref`

Uitvoeringskaart 1 van 2 onder het ontwerp
`backlog/doing/20260919-aantal-gevulde-gemiddelde.md`. Het besluitenblok
daar (G1–G9, maintainer 2026-09-19) is bindend en wordt hier niet
heropend; §5 "Uitvoeringsschets" verdeelt het werk over twee kaarten en
dit is de eerste. De ontwerpkaart blijft in `doing/` tot kaart 2 landt.

Goal: de waardentelling `AANTAL_GEVULDE` bestaat op de wire, wordt door
de verifier per arm behekt en door `ref` uitgevoerd, met een
worked-example-clausule in `spec/RUNTIME-SEMANTICS.md` die letterlijk
`ref/test/` haalt. Geen golden en geen rode casus — die wachten op
kaart 2, die clingo en beide Drools-takken de vorm leert; een golden
zonder clingo-drager breekt poort 6 en een rode casus eist twee
weigerende uitvoerders met benoemde fragmenten.

## Acceptatie

- (a) **Wire.** `spec/schema/regelspraak-ir-v0.schema.json` draagt de
  ene nieuwe `kind`-enumwaarde, additief; geen veld erbij, geen veld
  met nieuwe betekenis. Poort 2 groen, round-trip byte-identiek.
- (b) **Spec.** `spec/IR-SPEC.md` ch. 4.3 draagt de soort (kindrij +
  vormbeschrijving), WF-037 twee arms (precies één argument; dat
  argument MOET een setlezing zijn), WF-038 de scope-zin (de soort is
  geen numerieke vouw; besluit G8), WF-096 de exemptiezin zes → zeven.
  Geen nieuw WF-nummer (besluit G5). Beschermd pad: voorstel in de
  branch, merge is aan de hoofdsessie.
- (c) **Rood-eerst.** Eén negatief per arm in `corpus/negatief/`, elk
  eerst gemeten tegen de HUIDIGE verifier (md5 erbij) en daarna tegen
  de nieuwe, plus een arm-exacte mutantsweep over golden + rood +
  negatief + pariteit met beide uitkomsten in het dossier.
- (d) **`ref`.** De vouw wordt uitgevoerd; `spec/RUNTIME-SEMANTICS.md`
  krijgt ch. 3.4c met absolute worked examples en de ch. 1.2e-rij. De
  voorbeelden staan verbatim in `ref/test/` en dekken expliciet: lege
  bron → 0, gemengde gevuld/ongevuld-tellingen, en elk waardetype
  (getal, tekst, datum, enumeratie, booleaan).
- (e) **Benoemde weigering bij de andere uitvoerders.** clingo en de
  Drools-loader kennen de vorm nog niet — dat is correct voor kaart 1 —
  maar zij weigeren hem BENOEMD, niet met een bodemzin en niet met een
  onbeheerde uitzondering, elk met een getuige in de eigen suite. Zo
  blijft poort 8's bodemzinhek toekomstvast wanneer kaart 2 een rode
  casus zou willen pinnen.
- (f) Alle veertien poorten groen, elk los gedraaid.

May touch: `spec/schema/regelspraak-ir-v0.schema.json`,
`spec/IR-SPEC.md` (alleen ch. 4.3 en de WF-037/WF-038/WF-096-bullets
van ch. 4.6), `spec/RUNTIME-SEMANTICS.md` (alleen ch. 1.2e, ch. 3.4b's
verwijzing en het nieuwe ch. 3.4c), `spec/verifier/lib/**`,
`corpus/negatief/**`, `ref/lib/eval.ml`, `ref/test/test_ref.ml`,
`backends/clingo/lib/translate.ml`, `backends/clingo/test/test_clingo.ml`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/loader/IrLoader.java`,
een JUnit-getuige onder `backends/drools/src/test/`, en deze kaart.

Out of scope: `corpus/golden/**` en `corpus/rood/**` (kaart 2);
clingo-vertaling en beide Drools-transpilers (kaart 2); `scripts/**`
(geen vloer beweegt: alle vloeren zijn ondergrenzen en poort 12's
gelijkheden lezen niets zolang er geen golden is); `frontends/**` (de
dialectzin is besluit G9 en kaart 3); `spec/CONSTRUCTS.md` en
`spec/PLAN.md` (registervoorstel gaat als rijtekst in het `Result:`).
