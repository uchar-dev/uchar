# division-abs-by-numeric-zero

The abs-division variant of `division-by-numeric-zero` (see that NOTE
for the full rationale): `stukprijs` divides `opbrengst` by `partijen`
through gedeeld-door-abs (RUNTIME-SEMANTICS ch. 2.2 — the quotient
truncated toward zero at 5 decimals), and `partijen` is a FILLED
numeric zero. The variant shares `arith.div_gen` in ref and the
rounded-division path in the clingo translation, so it carries the
same edge: a numeric zero divisor is the v0-mirror hard fault in both
executors, with the v1 §9.5-channel route a decided, tracked delta
(RUNTIME-SEMANTICS ch. 1.4 point 4; `ASP-TRANSLATION.md` S6/S15).
This witness pins that the abs arm carries the edge *itself* — a
translation of the variant that lost the zero-divisor fault while the
bare division kept it would split the executors silently.
