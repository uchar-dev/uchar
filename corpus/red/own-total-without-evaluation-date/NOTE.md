# own-total-without-evaluation-date — the subject's own total in a session without an evaluation date

The document of `own-total-full-timeline`: the jaarloon of a Jutter is the
total of his wachtloon without a clip, which reads the asOfDate-bucket
scalar (RUNTIME-SEMANTICS ch. 3.6). The case carries no evaluation date and
gives J1 no wachtloon. Without an evaluation date there is no bucket, so the
total faults also where the timeline is absent, which would otherwise total 0
(ch. 3.6, "The evaluation date is tested before the absence of the
timeline"): EF-043, attribute `wachtloon`, in the reference interpreter, the
ASP backend and both branches of the Drools backend. A scalar wachtloon here
would be refused by the Drools backend before the session (DROOLS-051), so
the case leaves it out.
