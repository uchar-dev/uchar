# leegtest-heel-tijdvak

The **unnegated** leeg test under the ch. 3.9 whole-period qualifier: "de
lengte van de Schuit is gedurende het gehele jaar leeg". All three
executors refuse it by name. It is the twin of `leegnegatie-heel-tijdvak`,
which carries the negated form; `gevuldtest-heel-tijdvak` is its gevuld
twin. With `gevuldnegatie-heel-tijdvak` the four close the square soort ×
negatie.

## The shape

`leegnegatie-heel-tijdvak` with two fields changed: `Predicaat.negatie`
is `false`, and the `bron` text drops its "niet". Everything else is the
twin's, and the case file is the same byte for byte. The lengte is
month-granular, the qualifier is `GeheelPeriode` (jaar, `niet: false`),
and the case supplies S1's lengte as a timeline, so the scalar-input
refusal cannot fire first and hide the one under test.

## What ch. 3.9 answers, by hand

Unlike the negated twin, this form is normed: ch. 3.9 gives the leeg test
the touched-days threshold. The grid is the qualifier's own, so the
buckets are years even though the attribute is month-granular.

- S1: lengte 8 on [2025-01-01, 2026-01-01). The timeline touches one
  bucket, 2025, on all 365 of its days. None of those days is leeg, so 0
  of 365 satisfy the test and the fold is **onwaar**. The condition
  fails and meldpremie stays **leeg**.
- S2: no lengte at all. A timeline with no segments touches no bucket
  and passes vacuously: **waar**, so meldpremie is **40**.

An executor that carried the form would answer S1.meldpremie leeg and
S2.meldpremie 40.

## Why each executor fails

- `ref` reaches a named arm for a leeg/gevuld predicate that carries a
  whole-period qualifier. The arm sits before the plain leeg and gevuld
  arms and matches whatever `negatie` holds, so this twin and the negated
  one meet the same refusal at evaluation: "whole-period leeg/gevuld test
  is ch. 3.9 scope, not executed here".
- The ASP backend refuses the same node in the same words, from a named
  arm that comes before its leeg/gevuld arm, when it translates the
  condition.
- The Drools loader refuses the form before either transpiler branch sees
  it, as `Predicaat.periode`: "a leeg/gevuld test under a whole-period
  qualifier (ch. 3.9): ref executes neither the leeg fold (touched days)
  nor the gevuld fold (days(B)), so this backend does not answer the form
  either". The red run reports that loader refusal as an EXCLUDE, by its
  exception type, and gate 8's Drools leg pins a fragment only on a
  FAULT, so the leg lists this case with that reason.

## Why a RED case

Until 2026-09-23 both Drools branches played this form. Measured with a
jar built from before the loader refusal: the red runner reported
`NOFAULT ran to a model without faulting` on this document, and a green
copy of the case, with the hand values above as `expected`, came out
`PLAYED 1` in the exec branch and in the DRL-text branch. The replay
runner skips an `expectedFault` case, so on this document itself it
reported `PLAYED 0`. The fold lives in the Drools runtime alone, and `ref`
does not execute it, so no second executor could check that answer. The
refusal brought Drools into line with the other two.

For the two OCaml executors this is not a second copy of the twin. It
holds the REACH of their named arms: an arm narrowed to the negated form
still passes the twin and fails here. Measured on such a mutant, both
arms given `negatie = true`: `ref` drops the qualifier, reads the
operand as a scalar and faults on ch. 3.3 (`temporal attribute "lengte"
in a scalar context is refused`), and the ASP backend falls through to
its catch-all (`condition kind not translated`). Neither is the pinned
sentence, so gate 8 fails on both executors.

This document keeps all three refusals under the gate. If `ref` comes to
execute the ch. 3.9 leeg/gevuld fold, its pin here fails and the Drools
refusal loses the ground its sentence states; the ASP backend's pin fails
the day it follows. The document is verifier-clean: no WF rule forbids
the form, because ch. 3.9 norms it.
