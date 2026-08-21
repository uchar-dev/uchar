# Loader weigert een onbekende regelnaam in een inconsistent-verwachting

Besluit maintainer 2026-08-21: JA, symmetrisch met labels
(besluitenronde).

Goal: de case-loader weigert een `rule`-pin op een inconsistent-
verwachting wanneer geen regel/vorm in het document die (genormali-
seerde) naam draagt — dezelfde symmetrie als de bestaande weigering
van een onbekend `label`. De foutmelding noemt de onbekende naam en
de aanwezige kandidaten niet-limitatief. Let op de kanaalsleutels:
uniciteits-/consistentievormen en de doelvorm-sleutel van de
ongeordende groep tellen mee als geldige namen (de check volgt wat de
checker matcht, niet alleen resolvedRegels).

Acceptance: een case met een niet-bestaande regelnaam faalt bij LOAD
met een benoemde melding in beide executors (gedeelde loader); een
demonstratie-rood in het Result; geen bestaande case regresseert;
alle acht gates groen; twee lenzen gedraaid.

May touch: `ref/lib/case.ml` (en wat de gedeelde loader raakt),
`backlog/`; `corpus/` alleen als een getuige nodig blijkt.

Out of scope: het schema (blijft permissief; de check is semantisch).
