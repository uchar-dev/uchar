# date-part-not-whole — a part of a date construction that is no whole number

The vervaldag of a Vondst is the date with year 2025, month its vervalmaand
and day 1; the vervalmaand is a number of 1 decimal. V1's vervalmaand is 2,5.
Each part of a date construction is a whole unitless number, and another one
is a fault, never a truncation (RUNTIME-SEMANTICS ch. 3.8): the session faults
with EF-046 in the reference interpreter and both branches of the Drools
backend. The ASP backend refuses at translation every part whose type admits
a fraction, CLINGO-048 (gate 8's divergence list). Truncated, the vervaldag
would be 2025-02-01.
