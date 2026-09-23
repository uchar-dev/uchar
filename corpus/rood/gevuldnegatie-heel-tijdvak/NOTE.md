# gevuldnegatie-heel-tijdvak

A **negated** gevuld test under the ch. 3.9 whole-period qualifier: "de
lengte van de Schuit is gedurende het gehele jaar niet gevuld". All
three executors refuse it by name. It is the gevuld twin of
`leegnegatie-heel-tijdvak`. With `leegtest-heel-tijdvak` and
`gevuldtest-heel-tijdvak`, the four red cases close the square soort ×
negatie: leeg and gevuld, each negated and unnegated.

## The shape

`leegnegatie-heel-tijdvak` with two fields changed: `Predicaat.soort` is
`GEVULD`, and the `bron` text says "niet gevuld" where it said "niet
leeg". The same document follows from `gevuldtest-heel-tijdvak` by
setting `negatie` to `true` and adding the "niet"; both derivations
give the same bytes. The case file is the one all four twins share,
byte for byte.

## What ch. 3.9 answers, by hand

`negatie` is the inner, per-value negation: a negated gevuld test holds
on a day exactly when the lengte is leeg on that day. Which threshold it
then takes is the OPEN point its twin `leegnegatie-heel-tijdvak`
describes: ch. 3.9 gives the leeg test touched days and every other test
days(B), and it does not say which of the two a negated test takes. On
this case both candidates give the same answer.

- S1: lengte 8 on [2025-01-01, 2026-01-01). The timeline touches one
  bucket, 2025, on all 365 of its days, and on none of them is the
  lengte leeg. 0 days satisfy the test, below 365 under either
  threshold, so the fold is **onwaar** and meldpremie stays **leeg**.
- S2: no lengte at all. A timeline with no segments touches no bucket
  and passes vacuously: **waar**, so meldpremie is **40**.

The case cannot tell the two thresholds apart. It is here for the
refusal, not for a value.

## Why each executor fails

- `ref` reaches the named arm its three twins reach: a leeg or gevuld
  predicate under a whole-period qualifier, whatever `negatie` holds. It
  refuses at evaluation: "whole-period leeg/gevuld test is ch. 3.9
  scope, not executed here".
- The ASP backend refuses the same node in the same words when it
  translates the condition.
- The Drools loader refuses the form before either transpiler branch
  sees it, under the key and sentence of `leegnegatie-heel-tijdvak`:
  `Predicaat.negatie`, "a negated leeg/gevuld test under a whole-period
  qualifier (ch. 3.9): the threshold of the negated test is not
  normed". The red run reports that loader refusal as an EXCLUDE, by its
  exception type, and gate 8's Drools leg pins a fragment only on a
  FAULT, so the leg lists this case with that reason.

## Why a RED case

Before the Drools loader read `negatie` on leeg and gevuld, both branches
answered this form UNNEGATED. Measured with a jar built from 1ef9d818,
the commit before that change: the red runner reported `NOFAULT ran to a
model without faulting` on this document. A green copy with the hand
values above came out `FAIL case1.json: verwacht leeg, werkelijk 40` in
the exec branch and in the DRL-text branch, and a copy that expects the
unnegated answer (40 and 40) came out `PLAYED 1` in both. That is the
gevuld test answered with its negation dropped. From that change on the
loader refuses the form (measured on a jar built from 8b685402: `EXCLUDE
loader: … Predicaat.negatie …`).

This document holds the one corner of the square that its three twins
do not reach. Measured on a mutant that exempts exactly (GEVULD,
negated): in `ref` and in the ASP backend the named arm matches every
leeg or gevuld predicate under the qualifier except that one, and in the
Drools loader the negated refusal applies to LEEG only. The three twins
still pass in all three executors, and this one fails in each:

- `ref` drops the qualifier, reads the operand as a scalar and faults on
  ch. 3.3 (`temporal attribute "lengte" in a scalar context is
  refused`).
- The ASP backend falls through to its catch-all (`condition kind not
  translated (no corpus case exercises it)`), which gate 8 lists as a
  bottom sentence that no pin may cite.
- The Drools loader swaps the test to an unnegated leeg test, which it
  then does not refuse, and the red runner reports `NOFAULT ran to a
  model without faulting`. Gate 8's Drools leg lists this case as an
  EXCLUDE, so it fails on the drift.

The backend's own suite stayed green on that mutant (620 tests): its
witness for the negated refusal covers the leeg test only. For this
corner, this document is the only guard on the Drools refusal.

The day the norm picks a threshold for a negated test, the Drools
refusal loses its stated ground, as the negated leeg twin's does. The
document is verifier-clean: no WF rule forbids the form, because the
norm has not decided it.
