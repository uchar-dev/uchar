# AANTAL_GEVULDE, kaart 2: clingo, beide Drools-takken en de goldens

Uitvoeringskaart 2 van 2 onder het ontwerp
`backlog/doing/20260919-aantal-gevulde-gemiddelde.md`. Het besluitenblok
daar (G1–G9, maintainer 2026-09-19) is bindend en wordt hier niet
heropend; §5 "Uitvoeringsschets" verdeelt het werk over twee kaarten en
dit is de tweede. Kaart 1 (`backlog/done/20260920-aantal-gevulde-wire.md`)
landde de wire, de verifierhekken en `ref`; haar §9 is de
OVERDRACHTSNOTITIE die deze kaart uitvoert. De ontwerpkaart gaat met deze
kaart mee naar `done/`.

Goal: de waardentelling `AANTAL_GEVULDE` wordt door ALLE VIER de
uitvoerders beantwoord — `ref` (kaart 1), clingo, de Drools-exec-tak en de
Drools-DRL-tekst-tak — en het corpus draagt de eerste goldens waarop dat
vierkant sluit, plus één rode casus voor de temporele setbron die beide
OCaml-uitvoerders luid weigeren.

## Acceptatie

- (a) **clingo.** De benoemde weigerarm in
  `backends/clingo/lib/translate.ml` wordt een vertaling: een `#count`
  over dezelfde aanwezigheidsliteral waarmee de som/min/max hun
  presence-guard bouwen, met dezelfde `bronRol`/`bronPad`/filter-vorm als
  de SOM/TOTAAL-setarm. De temporele setbron houdt een EIGEN benoemde
  weigerzin (geen bodemzin), want `ref` weigert de vorm ook.
- (b) **Drools, in deze volgorde.** (1) `AANTAL_GEVULDE` in
  `Expr.FunctieKind` — een frontend-BESTAND, geen emissiewijziging, maar
  het record compileert in de backend mee; (2) `ExecModelTranspiler` (de
  accumulate-CE naast de count-tak); (3) `DrlTranspiler` (de
  `case AANTAL -> "count"`-tak). De klasse-brede `IrLoader`-Refusal laat
  de soort daarna door; de getuige uit kaart 1
  (`OnbekendeFunctieKindWitnessTest`) moet op een ECHT onbekende soort
  blijven slaan.
- (c) **Goldens, met handberekende verwachtingen VOORAF in het dossier.**
  Minstens (i) een basisgolden die de vouw dragend maakt: gemengd
  gevuld/leeg, met de instantietelling ernaast zodat
  `AANTAL_GEVULDE ≠ AANTAL` zichtbaar is; (ii) de `bronPad`-golden met
  een element dat langs twee tussenknopen bereikbaar is (de unie telt hem
  één keer); (iii) een niet-numerieke getuige (besluit G8), met de
  booleaanse `onwaar` als scherpste rij.
- (d) **Rood.** Minstens één rode casus voor de temporele setbron, met
  gepinde fragmenten in `ref` én clingo die GEEN bodemzin zijn.
- (e) **Poortpinnen.** `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` in
  `scripts/check-drools.sh` zijn GELIJKHEDEN en bewegen per spelende
  golden, met de reden per golden bij de pin. Zuiver additief — een
  parallelle kaart raakt hetzelfde script.
- (f) Alle veertien poorten groen, elk los gedraaid.

May touch: `backends/clingo/lib/translate.ml`,
`backends/clingo/test/test_clingo.ml`,
`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/ir/Expr.java`
(alleen de `FunctieKind`-enum),
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
een JUnit-getuige onder `backends/drools/src/test/`,
`corpus/golden/**` (nieuwe zones), `corpus/rood/**` (nieuwe zone),
`scripts/check-drools.sh` (alleen de twee gelijkheidspinnen en hun
redenblokken — beschermd pad, gemandateerd voorstel), deze kaart en de
ontwerpkaart.

Out of scope: `spec/**` (kaart 1 normeerde ch. 4.3 en ch. 3.4c; deze
kaart voegt er geen letter aan toe), `ref/**` (draagt de vorm al),
`spec/verifier/**`, `frontends/**` behalve de ene enumconstante (de
dialectzin is besluit G9 en kaart 3), `spec/CONSTRUCTS.md` en
`spec/PLAN.md` (rijvoorstel gaat als tekst in het `Result:`).

## Handberekeningen (VOORAF, ch. 3.4c)

De Jutterswet-populatie van de basisgolden, per jutter: J1 meldt V1
(gewicht 2,50), V2 (1,25) en V3 (geen gewicht); J2 meldt niets; J3 meldt
V4 (geen gewicht).

    jutter   AANTAL   AANTAL_GEVULDE   SOM (of 0)
    J1       3        2                3,75
    J2       0        0                0
    J3       1        0                0

De `bronPad`-golden: J1 voert B1 en B2; B1 bergt V1 (3,00) en V2 (geen
gewicht); B2 bergt V3 (5,00) én V1. De vlakke unie is {V1, V2, V3}, dus
`AANTAL` = 3 en `AANTAL_GEVULDE` = 2 — V1 telt één keer hoewel hij langs
twee boten bereikbaar is.

De niet-numerieke getuige: drie vondsten, per waardesoort twee gevuld en
één leeg, telkens 2. De booleaanse rij draagt `onwaar` als gevulde
waarde — leeg is de afwezigheid van een waarde, `onwaar` is er een.

## Voortgang

### Stap 1 — rood-eerst: het corpus vóór de uitvoerders

Vier zones toegevoegd, elk vóór enige uitvoerderswijziging gemeten:

    corpus/golden/jutterswet-gevuldetelling    verifier PASS, ref PASS (7)
                                               clingo: WEIGERT bij naam
    corpus/golden/jutterswet-gevuldepad        verifier PASS, ref PASS (2+2)
    corpus/golden/jutterswet-gevuldesoorten    verifier PASS, ref PASS (6)
    corpus/rood/aantalgevulde-temporele-setbron
                                               verifier PASS (de vorm is
                                               WELGEVORMD — de weigering hoort
                                               bij de uitvoerders, niet bij de
                                               poort)
                                               ref: verwachte fout gezien
                                               clingo: MISMATCH — hij weigert
                                               nog met kaart 1's naamzin in
                                               plaats van met de temporele

`ref` rekent alle drie de goldens meteen goed: hij draagt de vouw sinds
kaart 1, en de handberekende getallen hierboven zijn precies wat hij
antwoordt. Dat is de gewenste asymmetrie van deze kaart — het corpus pint
wat `ref` al kan en dwingt de andere drie uitvoerders daarheen.

De `bronPad`-golden is zo gebouwd dat de vlakke unie een ANDER getal geeft
dan de joinkoppels: in `case2-gedeelde-bergplaats` bereiken vier boten drie
bergplaatsen, waarvan P1 langs twee. Wie de koppels vouwt telt P1 twee keer
en antwoordt 3; de gepinde 2 is de unie. Zonder dat verschil zou de casus
niets onderscheiden.

De gedeelde bergplaats vroeg om een ANDERE relatievorm dan de
anker-golden `jutterswet-bootberging` gebruikt. Daar is de middenrelatie
veel-op-veel, waardoor het beanmodel aan geen van beide kanten een veld
afleidt en beide Drools-takken de golden bij naam weigeren. Hier lost een
boot in ÉÉN bergplaats en delen meerdere boten er één — een
veel-op-één-middenrelatie, waarvan het veld op de boot staat. Dat is
dezelfde deelbaarheid met een beanveld eronder, dus de vraag "telt de unie
of tellen de koppels" wordt in alle vier de uitvoerders gesteld in plaats
van in twee.
