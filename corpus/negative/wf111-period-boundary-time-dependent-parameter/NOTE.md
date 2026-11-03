# wf111-period-boundary-time-dependent-parameter

Violates WF-111 (`spec/IR-SPEC.md` ch. 4.6): a time window's bound is a
day-precise date expression over the subject, the parameters and literals,
and it reads one value per subject. Here the window is `vanaf de peildatum`,
where `peildatum` is a date parameter for every month: a time-dependent
parameter, whose timeline gives no single date.

Expected verifier report (`expect.txt` pins key and parameters):

    WF-111.bound-form FAIL /rules/rules/0/versions/0/condition/from: the from
    bound of a time window is the time-dependent parameter 'peildatum' — …

The same document with a scalar `peildatum` is a well-formed window: a
parameter is one of the three sources a bound may read (IMPLICIT-TIME
VB 6.1).
