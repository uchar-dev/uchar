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

## Rood-eerst, gemeten vóór elke wijziging (2026-09-20, HEAD c4f12743)

De twee goldens door de replay-API, beide takken, met de jar van deze
HEAD:

    jutterswet-keurstaat    exec: EXCLUDE transpiler SKIP: regel:
                                  veilbaarheid naar staat — regel
                                  (voorwaarde buiten Phase-B scope)
                            drl : EXCLUDE drl-transpiler MARKER: regel:
                                  veilbaarheid naar staat — TODO
                                  voorwaarde: haar staat gelijk is aan 'gaaf'
    jutterswet-vondstklasse exec: EXCLUDE transpiler SKIP: regel:
                                  bestemming van drijfgoed — regel
                                  (voorwaarde buiten Phase-B scope)
                            drl : EXCLUDE drl-transpiler MARKER: regel:
                                  bestemming van drijfgoed — TODO
                                  voorwaarde: zijn klasse gelijk is aan
                                  'drijfgoed'

De WAARDE-arm apart, want de conditie-arm ketst als eerste af: hetzelfde
document met de voorwaarde van R1 weggenomen geeft exec `regel (waarde
buiten scope)` en DRL-tekst `TODO literal: wrakgoed`. Beide helften van
de kaartzin kloppen dus letterlijk.

Actuele stand van de pins op deze HEAD: `EXPECTED_PLAYED` 169,
`EXPECTED_DRL_PLAYED` 146 (het kaartje schreef 165/142; er zijn
sindsdien kaarten geland).

## Toolchain-consequentie (herpin 2026-09-20, regelspraak-java 1e53206)

De pin-bump naar 3e97f181 legde dezelfde lezer in de toolchain bloot:
de twee beslistabel-enum-fixtures van `ExecModelDifferentialTest`
(OR-lijst én waarde-kant) loweren daar niet meer — drie
reden-dragende allowlist-entries en de diffvloer 6 → 4, elk met een
terugzet-verwijzing naar déze kaart. De herpin ná deze kaart draait
die drie entries en de vloer terug; een achtergebleven entry faalt de
stale-check daar vanzelf.
