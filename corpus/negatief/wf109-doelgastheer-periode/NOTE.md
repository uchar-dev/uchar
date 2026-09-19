Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)) at its **third scope fence**:
the doel is a bound object only for a check that carries no whole-period
qualifier. This guard carries one ("gedurende het gehele jaar gehesen"), so the
anker is the only object bound and `Sein` names none.

    WF-109 FAIL /feitCreaties/0/voorwaarde/eigenaar: the characteristic check
    is hosted on 'Sein', which no object bound here can be: this position binds
    the subject 'Kustvak'

Broken from `corpus/golden/jutterswet-seinhijsing` by two moves: `gehesen` on
`Sein` becomes time-dependent (`kenmerkGranulariteit: JAAR`) and the guard gains
a `GeheelPeriode` qualifier. The host field is untouched.

Why the fence exists, measured rather than assumed (2026-09-19, on this exact
document with a whole-year S1 and a half-year S2):

    verifier  : PASS      (before the fence)
    ref       : PASS 1    (the doel reading)
    clingo    : PASS 1    (the doel reading)
    exec      : PLAYED 1  (the same number, reached by the fold's own subject search)
    drl-tekst : EXCLUDE — Unable to Analyse Expression
                rsGeheelPeriode($subject.getGehesen(), "JAAR", 7, null)

In BOTH bean-model branches the ch. 3.9 whole-period arm runs BEFORE the host
test, so under a qualifier the host field decides nothing and the fold picks a
subject by its own rules. One branch happened to land on the doel; the other
lowered the fold onto the ANKER — where `gehesen` is an ordinary boolean and not
a timeline — and sank the whole package at compile. A verifier-clean document
that one of the four executors cannot carry at all is the split WF-109 exists to
close, so the shape is refused until someone norms it.

**This fence is temporary by construction.** The §8.2 whole-period family is
another card's, and is out of scope for the card that wrote this rule. The day
that family norms a qualified host, this fence falls and this witness turns into
a golden — which is why the refusal is positional (the doel is simply not bound
here) and not a claim that the form is meaningless.

Falsifiable in three directions, each turning this case clean:

- drop the qualifier — the bare doel host, which is the golden;
- host it on `Kustvak` — the anker reading, qualifier and all;
- empty the field — the guard gates the anker.
