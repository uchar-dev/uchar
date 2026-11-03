# jutterswet-emmerscalair-per-periode — a condition per period over the asOfDate-bucket scalar

The premie of a Jutter is 25 during the time that his strandloon is greater
than 120: a condition per period on a timeline target (RUNTIME-SEMANTICS
ch. 3.10), which reads the strandloon whole. The case gives J1's strandloon
as the asOfDate-bucket scalar 130 (asOfDate 2024-02-15), which carries no
period. A form that needs the full timeline and finds the bucket scalar
faults, never guesses (ch. 8.4): the session faults with EF-037, attribute
`strandloon`, label `J1`, in the reference interpreter, the ASP backend and
both branches of the Drools backend.
