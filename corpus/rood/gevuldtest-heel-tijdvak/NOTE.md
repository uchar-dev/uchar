# gevuldtest-heel-tijdvak

The unnegated **gevuld** test under the ch. 3.9 whole-period qualifier:
"de lengte van de Schuit is gedurende het gehele jaar gevuld". All three
executors refuse it by name. It is the gevuld twin of
`leegtest-heel-tijdvak`. With `leegnegatie-heel-tijdvak` and
`gevuldnegatie-heel-tijdvak`, the four red cases close the square `kind` ×
`negated`: leeg and gevuld, each negated and unnegated.

## The shape

`leegtest-heel-tijdvak` with two fields changed: `Predicate.kind` is
`filled`, and the source snippet says "gevuld" where it said "leeg". The
case file is the same byte for byte. The lengte is month-granular, the
qualifier is `WholePeriod` (`year`, `negated: false`), and the case
supplies S1's lengte as a timeline, so the scalar-input refusal cannot
fire first and hide the one under test.

## What ch. 3.9 answers, by hand

The gevuld test takes the days(B) threshold: every day of each touched
bucket must be filled. The grid is the qualifier's own, so the buckets
are years.

- S1: lengte 8 on [2025-01-01, 2026-01-01). The timeline touches one
  bucket, 2025, on all 365 of its days, and all 365 are filled. 365 of
  365 satisfy the test, so the fold is **waar** and meldpremie is
  **40**.
- S2: no lengte at all. A timeline with no segments touches no bucket
  and passes vacuously: **waar**, so meldpremie is **40**.

Both rows are 40. A timeline that covers the whole of its only bucket
meets the touched-days threshold and days(B) alike, so this case does
not separate the two thresholds. It is here for the refusal, not for a
value.

## Why each executor fails

- `ref` reaches the same named arm as its twins. That arm matches a
  leeg or gevuld predicate under a whole-period qualifier, whatever
  `negated` holds, and refuses at evaluation: "whole-period leeg/gevuld
  test is ch. 3.9 scope, not executed here".
- The ASP backend refuses the same node in the same words when it
  translates the condition.
- The Drools loader refuses the form before either transpiler branch
  sees it, under the key and sentence of the leeg twin:
  `Predicaat.periode`, "a leeg/gevuld test under a whole-period
  qualifier (ch. 3.9): ref executes neither the leeg fold (touched days)
  nor the gevuld fold (days(B)), so this backend does not answer the form
  either". The red run reports that loader refusal as an EXCLUDE, by its
  exception type, and gate 8's Drools leg pins a fragment only on a
  FAULT, so the leg lists this case with that reason.
  The backend's own witness test asserts the key and the ground for
  leeg and gevuld alike, and gate 8 runs it when it rebuilds the jar.

## Why a RED case

Until 2026-09-23 both Drools branches played this form. Measured with a
jar built from before the loader refusal: the red runner reported
`NOFAULT ran to a model without faulting` on this document, and a copy
of the case with the hand values above as `expected` came out `PLAYED 1`
in the exec branch and in the DRL-text branch. No second executor could
check that answer.

For the two OCaml executors this document holds the reach of their
named arms over the soort, which the two leeg documents cannot. Measured
on a mutant with both arms narrowed to `soort = Leeg`: the two leeg
documents still pass, and this one fails in both executors. `ref` drops
the qualifier, reads the operand as a scalar and faults on ch. 3.3
(`temporal attribute "lengte" in a scalar context is refused`). The ASP
backend falls through to its catch-all (`condition kind not translated
(no corpus case exercises it)`), which gate 8 lists as a bottom sentence
that no pin may cite. Neither is the pinned sentence, so gate 8 fails
on both executors.

If `ref` comes to execute the ch. 3.9 gevuld fold, its pin here fails,
and the Drools refusal loses the ground its sentence states; the ASP
backend's pin fails the day it follows. The document is verifier-clean:
no WF rule forbids the form, because ch. 3.9 norms it.
