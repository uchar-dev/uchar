# division-abs-by-numeric-zero

The abs-division variant of `division-by-numeric-zero`: `stukprijs` divides
`opbrengst` by `partijen` through gedeeld-door-abs (`RUNTIME-SEMANTICS.md`
ch. 2.2 — the quotient truncated toward zero at 5 decimals), and `partijen`
is a FILLED numeric zero. The variant shares `arith.div_gen` in ref, the
rounded-division path in the clingo translation and `ComputeHelpers.dividedAbs`
in the Drools backend, so it carries the same edge: since 2026-10-19 (card
`zero-division-channel`, ch. 1.4 point 4) the quotient is leeg and the rule
deposits a §9.5 fact of kind EF-005 on K1. This witness pins that the abs arm
carries the edge *itself* — a translation of the variant that kept the old
fault, or gave 0, while the bare division deposits would split the executors.

Until 2026-10-19 this was a red case, the v0-mirror hard fault in both OCaml
executors (`ASP-TRANSLATION.md` S6/S19).
