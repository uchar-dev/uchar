# vast-startpunt-tijdlijn: een tijdlijn met een vast startpunt buiten de kalenderperiode krijgt een veld op de wire

IR-SPEC ch. 2.8 (`Timeline`: `granularity`, `count`, `variableStart`) zegt
uitdrukkelijk dat de wire geen vast startpunt draagt: een tijdlijn met
`variableStart: false` ligt op het kalenderraster (RS ch. 3.2, "Fixed
starting points"). RegelSpraak kent wél tijdlijnen met een vast startpunt
buiten dat raster (§13.3.6.3–.7, "startend op …"); de frontend resolvet ze
(`RegelSpraakModel.vasteStartpunten`) en de emitter weigert ze met een
ERROR ("tijdlijn: het vaste startpunt '…' wijkt af van de kalenderperiode
en de wire heeft er geen veld voor — de zin wordt geweigerd in plaats van
het startpunt stil te laten vallen", `IrEmitter.wireWeigeringen`). De oude
toolchain droeg de vorm in het geheugen; de maintainer besloot op
2026-09-26 dat de wire haar nu draagt. Spec-first: dit is een deltakaart
(`spec/CONSTRUCTS.md`-rij door de hoofdsessie bij de landing).

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 De bouwer meet eerst welke startpuntvormen §13.3.6.3–.7 kent (een
  absolute datum, een terugkerende dag-maand, iets anders; de
  specificatie is read-only en wordt niet geciteerd) en welke de frontend
  vandaag in `vasteStartpunten` zet; de kaart draagt ze alle op de wire.
- U2 IR-SPEC ch. 2.8: één optioneel veld op `Timeline` (naam **voorstel**
  `fixedStart`, vorm door de bouwer: een strikte ISO-datum voor een
  absolute start, een `--MM-DD`-achtige dag-maand voor een terugkerende
  start — de bouwer kiest één spelling en verantwoordt haar), met een
  nieuwe MUST (**voorstel** WF-1xx, nummer door de bouwer als eerstvolgende
  vrije): `fixedStart` alleen bij `variableStart: false`, welgevormd,
  passend bij de granulariteit (een dag-maand bij jaar/maand, geen dag bij
  een dagtijdlijn); verifier-arm met één zin, negatief rood-eerst,
  `expect.txt`, NOTE uit echte uitvoer; schema-bijwerking in canonieke
  sleutelvolgorde (gate 2).
- U3 RS ch. 3.2: het raster van zo'n tijdlijn is de reeks perioden van
  `count` granulariteitseenheden vanaf het startpunt (en dezelfde reeks
  terug in de tijd), worked example → `ref/test`. Alle drie de uitvoerders
  bucketen erop (ref, clingo, beide Drools-takken); de frontend emitteert
  het veld en `wireWeigeringen` verliest deze arm.
- U4 Golden `jutterswet-vast-startpunt` (**voorstel**): een tijdlijn
  "startend op" een datum buiten het kalenderraster met een attribuut
  erop, regels die per periode schrijven en lezen, cases rond de
  periodegrens; ref ≡ clingo ≡ Drools. Gate 13 (twist) en gate 7 per de
  vaste patronen; gate 12 +1 op beide takken.
- U5 Pariteitszone: alleen tellen; als de casusdump de vorm draagt, is de
  herpin in deze kaart (README-regel zonder inhoud uit de zone).
- U6 Klaar: alle veertien gates groen; naamtabel-toets rc 0; gelijke
  zinnen in verifier, ref, clingo en de Drools-lader zonder
  regelnummerstaart; L5-spelling.

## Acceptatie

- (a) U1–U6; (b) een document met het veld is verifier-schoon en speelt in
  de drie uitvoerders; een document met het veld op een `variableStart`-
  tijdlijn wordt geweigerd met de WF-zin; (c) `git grep "wire heeft er geen
  veld"` leeg.

May touch: `spec/IR-SPEC.md` ch. 2.8, `spec/RUNTIME-SEMANTICS.md` ch. 3.2,
`spec/schema/`, `spec/verifier/` (alle **voorstel**), `ref/**`,
`backends/clingo/**`, `backends/drools/**`, `frontends/regelspraak/**`,
`corpus/golden/jutterswet-vast-startpunt/`, `corpus/negatief/wf1xx-*`,
gatepins (**voorstel**), `toka/` alleen bij een herpin. Niet in deze kaart:
`spec/CONSTRUCTS.md` en `spec/PLAN.md` (hoofdsessie bij de landing).
