# jutterswet-totaal-open-segment — a total over an open segment that is not 0

The opgehoogde strandloon of `corpus/golden/jutterswet-maandloon`, 5 plus
the strandloon, and its total. J1 has a strandloon of 100 on the first half
of 2024, so the opgehoogde strandloon is 5 until 2024-01-01, 105 on the
first half of 2024 and 5 from 2024-07-01: its two segments with an open
bound carry 5. A total of 5 over unboundedly many months has no value
(RUNTIME-SEMANTICS ch. 3.6, ch. 5.7b): the session faults with EF-036,
attribute `opgehoogde strandloon`, label `J1`, in the reference interpreter
and the ASP backend. The Drools backend faults alike on both
branches.
