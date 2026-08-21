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

Result: the rule pin refuses at load when no form carries the pinned
name — symmetric with the unknown-label refusal, through the shared
loader, so both executors refuse identically (exit 2, same message
naming the unknown pin AND a non-exhaustive sorted candidate list).
The valid-name set is exactly what the channel can carry: every form
name and the unordered ladder group's target-form key; ladder MEMBER
names are deliberately absent (they never reach the channel — the
lens's significant find: accepting them re-created the vague mismatch
this card exists to kill; a member-name pin now refuses at load naming
the two target-form candidates). The version-strip asymmetry the lens
found is harmonised to the spec side: BOTH checkers (ref and clingo)
now match version-stripped on both sides, per ch. 5.6's
version-stripped channel key, so a pin "X" matches a deposit from
version "X #2" — no existing pin changes meaning (strip is identity on
unversioned names). Red-first: the typo pin loaded and failed as a
vague mismatch; the name-set lens hunted every insert_error key and
found no false refusal. All eight gates green.
