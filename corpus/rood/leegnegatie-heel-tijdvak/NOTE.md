# leegnegatie-heel-tijdvak

A **negated** leeg test under the ch. 3.9 whole-period qualifier: "de
lengte van de Schuit is gedurende het gehele jaar niet leeg". All three
executors refuse it by name.

## The shape

`jutterswet-waardemelding`'s first rule on its own (meldpremie := 40 if
the lengte is niet leeg, `Predicaat LEEG` with `negatie: true`). The
lengte is made month-granular and the qualifier `GeheelPeriode` (jaar,
not negated) is added. The case supplies the lengte as a timeline, so
the scalar-input refusal cannot fire first and hide the one under test.

## Why a RED case

`negatie` is the inner, per-value negation of the test. The qualifier's
own negation is `GeheelPeriode.niet`, which is `false` here. Under the
qualifier the meaning of the inner negation is an OPEN point of the norm.
Ch. 3.9 gives the leeg test the touched-days threshold and every other
test days(B), and it does not say which of the two a negated leeg test
takes. Under days(B) the swap to the gevuld test would be exact; under
touched days it would not.

`ref` refuses every whole-period leeg/gevuld test ("whole-period
leeg/gevuld test is ch. 3.9 scope, not executed here"). The ASP backend
refuses the same form in the same words, from a named arm that comes
before its leeg/gevuld arm. The Drools loader refuses the negated form by
name (`Predicaat.negatie`, "the threshold of the negated test is not
normed"). The Drools leg lists it as an EXCLUDE, because that refusal
comes one phase before fire.

The document is verifier-clean: no WF rule forbids the form, because the
norm has not decided it. Until this case existed only the backends' own
suites pinned the refusal. If the norm picks a threshold, all three
refusals go stale together.
