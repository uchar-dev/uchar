# runtime-tijdconditie-leeg: `rsTijdConditie` leest een lege kant van een vergelijkingsknip als geen periode

*Gevonden op 2026-10-09 door de bouwer van stap 2 van `20261008-tms-verdeling-en-tijdlijnen` en door een lens op die
stap, en door de hoofdsessie vastgelegd volgens de afspraak dat een bestaande bug in de gedeelde runtime een kaart
krijgt. Nagemeten op `95947ae0f` (2026-11-03), met ref, clingo en de verifier uit `_build` van de worktree
`wt/drools-clip-equality` en de shaded jar van die worktree.*

## Aanleiding

RUNTIME-SEMANTICS ch. 3.6 laat een totaal knippen op "de tijd dat" een vergelijking van een eigen tijdsafhankelijk
attribuut met een grens geldt, en leest die vergelijking per periode met de regels van ch. 1.2. Bij een getal is twee
keer leeg gelijk en precies één keer leeg ongelijk, en een gat in de getoetste tijdlijn leest leeg (ch. 3.1). Het
voorbeeld van 500,00 in ch. 3.6 pint dat voor ongelijkheid.

De oude exec-model- en DRL-teksttak zijn weg. De lowering met truth maintenance (`transpiler/tms`: exec-model en
DRL-tekst, twee weergaven van één analyse) rekent de vergelijkingsknip met de runtimehulp `rsTijdConditie`
(`backends/drools/.../runtime/RegelSpraakRuntime.java`, via `StaticFunction.RS_TIME_CONDITION`), die er op
`95947ae0f` nog staat. Die hulp loopt alleen de segmenten van de getoetste tijdlijn langs, zodat een gat geen periode
is, en geeft bij een lege grens geen enkele periode. Voor de ordeningen klopt dat met ch. 1.2; voor "gelijk aan" en
"ongelijk aan" niet, en daarom weigert de tak die twee met DROOLS-222.

Document (de golden `jutterswet-peilwacht` van deze kaart): een Jutter met een wachtvergoeding per maand van 10 van
januari tot en met juni 2024, een peilstand van 5 alleen in januari, en "het totaal van zijn wachtvergoeding per maand
gedurende de tijd dat zijn peilstand (on)gelijk is aan zijn drempelstand". Verifier PASS.

| Geval | met de hand (ch. 3.6, 1.2) | ref | tak met truth maintenance | die tak zonder de weigering | clingo |
|---|---|---|---|---|---|
| gelijk aan, grens leeg | 50 | 50 | `EXCLUDE DROOLS-222` | 0 | `TRANSLATION FAULT` |
| gelijk aan, grens 5 | 10 | 10 | `EXCLUDE DROOLS-222` | 10 | `TRANSLATION FAULT` |
| ongelijk aan, grens 5 | 50 | 50 | `EXCLUDE DROOLS-222` | 0 | `TRANSLATION FAULT` |
| ongelijk aan, grens leeg | 10 | 10 | `EXCLUDE DROOLS-222` | 0 | `TRANSLATION FAULT` |

Letterlijk, op beide branches gelijk: `EXCLUDE DROOLS-222: transpiler SKIP: regel: peilsom van een jutter — a total
over its own timeline ('het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn peilstand gelijk is
aan zijn drempelstand') with 'equal to' in the clip ('gedurende de tijd dat zijn peilstand gelijk is aan zijn
drempelstand'): this branch does not carry this form yet; the calculation helper rsTijdConditie reads an empty side, a
gap in the compared timeline, or an empty boundary as no period, whereas RUNTIME-SEMANTICS ch. 3.6 and the reference
interpreter apply the comparison of ch. 1.2 per period: two empty sides are equal, and exactly one empty side is
unequal; no golden with cases has it`. Clingo: `TRANSLATION FAULT: equality in a per-segment clip comparison is not
translated (no corpus case exercises it)`, exit 1. Ref: `oracle: PASS` op alle drie case-bestanden, met de waarden
die met de hand uit ch. 3.6 en ch. 1.2 volgen. De kolom "zonder de weigering" is de tak met alleen de weigering
weggehaald en de oude hulp: `FAIL case1-grens-leeg.json: attribuut peilsom: verwacht 50, werkelijk 0; attribuut
afwijksom: verwacht 10, werkelijk 0; …`, op beide branches gelijk.

## Acceptatie

- (a) Een golden met de vier gevallen hierboven, eigen Jutterswet-casus, met absolute verwachtingen volgens ch. 3.6 en
  ch. 1.2, met de hand nagerekend (ref is niet de maat).
- (b) De tak met truth maintenance draagt "gelijk aan" en "ongelijk aan" in een knip op beide branches, rood eerst;
  DROOLS-222 `RETIRED` als geen plek haar meer noemt; de rijen `knip-gelijk` en `knip-ongelijk` van
  `TmsTimelineRefusalWitnessTest` spelende getuigen of naar de golden.
- (c) Clingo vertaalt gelijkheid in een knip, of weigert haar met naam zoals nu.
- (d) Drools-suite groen zonder skips, gates 5, 6, 12 en 17 groen op de golden, `make gates` groen.

## Omvang

Klein.

May touch: `backends/drools/**` (de runtimehulp en de weigering in `transpiler/tms/`), `backends/clingo/`,
`corpus/golden/<nieuwe golden>/`, `scripts/check-drools.sh` (pins, voorstel), kaartverplaatsing.
