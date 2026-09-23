# ref-klipvouw-nul: de arbiter antwoordt 0 waar niets geklipt is

Gemeten door de datumvouw-lens (2026-09-23): in de gedurende-klip-arm
van ref antwoordt de ARBITER zelf een betekenisloze 0 — een casus
zonder waardetijdlijn geeft `wachtsom = 0`, en een casus waarin de
hele klip wegvalt (kenmerk disjunct van de waarde) ook; met een
datumgetypeerd doel is die 0 zelfs 1970-01-01. Clingo volgde op de
oude basis (`[0]`); de Drools-exec CRASHT op de casus zonder waarde
met `ClassCastException BigDecimal→LocalDate`. De eigen-arm doet het
goed (leeg bij leeg). Sinds de datumvouw-kaart weigeren clingo en de
DRL-tak de datumgetypeerde vormen op documentniveau, maar refs
0-antwoord in de klip-arm en de exec-crash staan los van het
datumtype en blijven open.

Wortelvoorstel uit die kaart (beschermd pad, hier mee te nemen): een
WF-regel "de operand van TOTAAL/SOM/pro-rata is numeriek" — WF-091
vergelijkt vandaag alleen het aggregaattype met de operand, niet of
de operand een getal is.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer beide casussen op alle uitvoerders; leg vast wat de
  genormeerde uitkomst hoort te zijn (leeg, zoals de eigen-arm? — de
  spec beslist, RUNTIME-SEMANTICS ch. 3.5a/3.9 nalezen).
- (b) ref volgt de norm (vermoedelijk: leeg in plaats van 0);
  rood-eerst een ref-suitegetuige; clingo volgt; de exec-crash wordt
  een benoemde weigering of het juiste lege antwoord.
- (c) Weeg de WF-regel (spec/IR-SPEC + verifier, **voorstel**): als
  hij komt, worden de datumvouw-roodgetuigen negatiefs — benoem de
  verschuiving expliciet.
- (d) Corpussweep; alle veertien gates groen.

May touch: `ref/**`, `backends/clingo/**`, `backends/drools/**`,
`spec/IR-SPEC.md` en `spec/verifier/**` (**voorstel**), `corpus/**`,
`scripts/check-rood.sh` (**voorstel**), kaartverplaatsing. NB: raakt
de arbiter — extra zware lens.
