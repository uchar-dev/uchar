# bound-day-behind-a-holding-condition

The document of `corpus/golden/bound-day-behind-a-failing-condition`, with B1
actief. The condition of "jaartal van een baken" holds, so its value reads the
bound day outside a day-kind evaluation; the first member of the condition of
"gemeld baken" does not hold, so its second member reads it too. Each is a
fault, EF-010 (`spec/RUNTIME-SEMANTICS.md` ch. 3.8, 5.7b), without parameters.

Every executor faults where it evaluates the read: `ref` in its evaluation,
the ASP backend at solving (a `viol/2` witness whose body is the reach of the
read), and the Drools backend in its red run.
