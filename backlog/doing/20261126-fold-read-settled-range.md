# FoldRead laat bij vaststaand bereik de uitkomst en bijdragers intact

Priority: high
Size: S
Status: doing
May touch: `backends/drools/`
Out of scope: `spec/`, `ref/`, `backends/clingo/`, `frontends/regelspraak/`, `corpus/`

## Achtergrond

In `ComputeHelpers.beforeFoldOutcome` (ingevoerd voor `Term.FoldRead` wanneer een
vouwbereik een openbare naar-één-stap bevat) werd bij `rangeOpen == null`
(`roSel = Integer.MAX_VALUE`) alsnog `openSel < roSel` waar zodra `openMembers`
niet leeg was (`0 < Integer.MAX_VALUE`). Daardoor brak `beforeFoldOutcome` een
kwantorvouw met een ingevulde naar-één-stap in haar `sourcePath` voortijdig af:

1. Bij een onbepaalde kwantorvouw (`V1` uitgesloten door het filter, `V2` met open
   matrix) werd `v.outcome()` in `RecordingRendering` niet bereikt en bleef
   `ConditionReading.NodeReading.contributors()` leeg in plaats van `[V2]`.
2. Bij een besliste kwantorvouw naast een open lid (`V2` maakt `geen van de` onwaar,
   terwijl `V3` een open matrix heeft) brak `beforeFoldOutcome` ten onrechte af met
   `OpenRead` vóór `quantifierVerdictOverOpen` het vaststaande oordeel kon geven.

## Oplossing

1. Wanneer `rangeOpen == null` is geen enkele stap van het vouwbereik open: voer in
   `ComputeHelpers.beforeFoldOutcome` exact dezelfde controles uit als bij een vouw
   zonder openbaar bereik (`reportFilterDeposits`, `filterFault`, `memberFault`) en
   keer terug zodat `outcome` gewoon evalueert.
2. Voeg een getuige `open-settled-range-quantifier` toe aan `OpenReadingsWitnessTest`
   tegen `ref --trace` op beide Drools-takken (`EXEC_MODEL` en `DRL_TEXT`), voor zowel
   het onbepaalde geval met bijdrager `[V2]` als het besliste geval naast een open
   lid `[V2, V3]`.
