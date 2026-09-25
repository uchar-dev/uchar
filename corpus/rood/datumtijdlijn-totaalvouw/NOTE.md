# datumtijdlijn-totaalvouw

The §7.1 own-subject `total` over a TIMELINE whose segment values are
dates, as a red case. The document is `golden/jutterswet-tempototaal`
reduced to its third rule — `het totaal van zijn bewaarvergoeding per
maand` — with the totalled attribute (granularity `month`) and the target
retyped to `Datum in dagen`, and the unused `Vondst` type and its fact
type dropped. The verifier passes it, so the refusal has to come from
the executors.

The reason is arithmetic. A temporal total (ch. 3.6) multiplies each
segment value by the whole grid units it spans, and a date is a calendar
POSITION: scaling one has no meaning. `ref` says so at the
multiplication itself — `maal: expected a number, got 2024-01-15` — and
that is this case's arbiter reading.

What the case guards against is a DEFINITE WRONG NUMBER. Measured
2026-09-23 on this document: the ASP backend answered **19737** — the day
number of 2024-01-15 — because the case supplies one asOfDate-bucket
scalar for a `month` timeline, the whole-units factor is 1, and the day
number came out unchanged, looking like an answer. The translation now
declines the form by name, asking the DECLARATION on the subject type,
before it grounds anything.

The refusal is about the DOCUMENT, not about the case, and that costs
one measured asymmetry: a case that supplies NO value for the attribute
gets leeg from `ref` (no multiplication is ever performed) while the ASP
backend still declines. The direction is the harmless one — a refusal
against an answer no chapter defines — and it is the same stance the
set-source sister (`datumsetbron-totaalvouw`) takes.

The Drools backend declines too, in two phases. The exec branch's case
loader refuses the non-numeric segment before fire (a generated Segment
holds a `BigDecimal`); the DRL-text branch markers the form through the
timeline half of the reason the set-source arm shares. Both are listed
with their reason in gate 8's exclusion list rather than fragment-pinned,
this leg's standing convention — FAULT there means a genuine fault at
fire.
