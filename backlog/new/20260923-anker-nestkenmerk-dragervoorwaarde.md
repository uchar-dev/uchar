# anker-nestkenmerk-dragervoorwaarde: de ongeordende broers van het nestkenmerk-anker dragen de voorwaarde van hun drager

Vervolg op anker-ww-herleving-dragervoorwaarde (geland 2026-09-23) en
maintainersbesluit 2026-09-23 (tweede besluitvraag): de leden van
`Uitzonderingen ongeordend op X:` erven de rechtvaardiging van X, net als
een enkele `Uitzondering op X`. Het handanker `anchor/jutterswet-nestkenmerk`
(RUNTIME-SEMANTICS ch. 7.9, het kenmerkdoel `Jutter.vrijgesteld`) draagt
zijn ongeordende uitzonderingsgroep vandaag zonder die voorwaarde: de
wortel beslist `waar` op `nx`, de broers beslissen `onwaar` op `ny` en
`waar` op `nz`, elk alleen op het eigen kenmerk. De sessie van het
WW-anker mat: met `nz` en zonder `nx` zeggen ref, clingo en de
Drools-exec-tak "aanwezig", want de broer beslist buiten zijn drager om;
een RegelSpraak-tweeling die `nx` erft zou "afwezig" geven en kon dus
niet op dit anker uitkomen. De drie bestaande cases (`case1-y-wint`:
nx, ny; `case2-conflict`: nx, ny, nz; `case3-terugval`: nx) hebben `nx`
alle drie en veranderen niet van verwachting.

## Ontwerp

- De twee broers krijgen de voorwaarde `ALLE[nx, ny]` respectievelijk
  `ALLE[nx, nz]` (plat, drager eerst, zoals het WW-anker), de wortel blijft
  `nx`; de rose tree en de `ongeordend: true`-markering blijven staan.
- Eén nieuwe case, `case4-nz-zonder-nx` (alleen `nz` → `vrijgesteld`
  afwezig, consistent): pint het erven. Rood-eerst: op het huidige anker
  geven alle drie de uitvoerders "aanwezig".
- De NOTE (er is er nog geen naast de cases; de spec-tabel in ch. 7.9
  r. 5028–5037 beschrijft de drie cases) legt de dragervoorwaarde uit met
  dezelfde alinea-opbouw als het WW-anker. De tabel in ch. 7.9 is
  beschermd; een vierde rij daar gaat als **voorstel** mee, of de NOTE
  draagt haar.
- Niet in deze kaart, wel te noteren: de attribuutankers
  `jutterswet-nestuitzondering` en `jutterswet-nestdiepte` hebben dezelfde
  vorm (ongeordende groepen onder `grondX` zonder `grondX` in de leden) en
  geen case zonder de drager; er is geen tweeling voor gepland. Of zij mee
  moeten, beslist de maintainer als zo'n tweeling er komt.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: `case4-nz-zonder-nx` faalt op het huidige `ir.json` in
  ref, clingo en de Drools-exec-tak; groen na de conjunctie in alle drie;
  case1–case3 ongewijzigd in verwachting.
- (b) Gate 3 (verifier) groen: WF-050/WF-075 op de sub-defaults; gates 5,
  6, 12 groen; gate 12 pins ongewijzigd (zelfde golden); gate 7: case4
  overleeft geen no-op-uitvoerder (verwachting afwezig, plus `consistent`).
- (c) Alle veertien gates groen.

May touch: `corpus/golden/anchor/jutterswet-nestkenmerk/**` (**voorstel**,
handanker), `spec/RUNTIME-SEMANTICS.md` ch. 7.9 tabel (**voorstel**, één
rij), kaartverplaatsing.

Niet in deze kaart: het dialect; de attribuutankers.
