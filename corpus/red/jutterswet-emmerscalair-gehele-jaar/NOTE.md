# jutterswet-emmerscalair-gehele-jaar — the whole-period qualifier over the asOfDate-bucket scalar

The jaarpremie of a Jutter is 20 if his wachtloon is greater than 5 during
the whole year: the whole-period qualifier in a scalar rule, whose fold
reads the wachtloon whole, day by day in every calendar year it touches
(RUNTIME-SEMANTICS ch. 3.9). The case gives J1's wachtloon as the
asOfDate-bucket scalar 7 (asOfDate 2024-06-15), which carries no period. A
form that needs the full timeline and finds the bucket scalar faults
(ch. 8.4): the session faults with EF-037, attribute `wachtloon`, label
`J1`, in the reference interpreter, the ASP backend and both branches of
the Drools backend. Read as no timeline, the fold would touch no year and
the test would pass, a jaarpremie of 20.
