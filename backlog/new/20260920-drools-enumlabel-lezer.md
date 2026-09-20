# drools-enumlabel-lezer: de consumer eist delimiters die de wire nooit draagt

Uit kaart `20260919-v8-enum-emitter` (2026-09-20, lens + mini-fixronde):
`Literalen.tekstOfEnumLabel` leest een enumeratiewaarde door `enumLabel`,
dat delimiters verlangt, terwijl de wire per WF-039 het kale label
draagt. Vijf plekken lezen door die lezer heen, en het zijn niet alleen
vergelijkingen: `ExecModelTranspiler` 3913 (eigen attribuut), 3928
(genavigeerd) en 5422, `DrlTranspiler` 5114, en de WAARDE-arm
`ExecModelTranspiler:4185`, die een enumliteral als geschreven waarde
omzet. Beide Drools-takken weigeren daardoor zowel elke enumgelijkheid
als elke enumtoekenning — apart gemeten op een voorwaardeloze variant:
exec-model `waarde buiten scope`, DRL-tekst `TODO literal: wrakgoed`. De
twee geraakte goldens zijn `jutterswet-keurstaat` en
`jutterswet-vondstklasse`, beide per naam gepind in beide kanaallijsten
van gate 12.

De reparatie is klein en zit op ÉÉN plek: de lezer geeft het label van
een `Enumeratie`-getypeerde literal verbatim terug, precies zoals de
Tekst-arm doet, waarmee alle vijf plekken gratis meekomen — geen van de
call sites hoeft te wijzigen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de twee EXCLUDEs op de twee goldens vastgelegd, dan de
  lezer-fix; dual-branchgetuige met mutatieproef (delimiters-eis terug →
  getuige faalt bij naam in beide takken).
- (b) Kanaalgevolgen: beide goldens worden spelers — de
  gelijkheidspins elk +2 op de dan geldende stand (bij het schrijven van
  dit kaartje: `EXPECTED_PLAYED` 165 → 167, `EXPECTED_DRL_PLAYED` 142 →
  144) en twee regels uit élke kanaallijst; SERIEEL plannen tegen andere
  kaarten op `scripts/check-drools.sh`.
- (c) Corpussweep beide takken; alle veertien gates groen. Het levert
  twee goldens erbij aan het volledige vierkant.

May touch: `backends/drools/**`, `scripts/check-drools.sh` (pins +
kanaallijsten, **voorstel**), kaartverplaatsing. NB: serieel met andere
DrlTranspiler/check-drools-kaarten.
