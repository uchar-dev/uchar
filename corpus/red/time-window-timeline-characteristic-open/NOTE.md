# time-window-timeline-characteristic-open

The twin of `corpus/golden/time-window-timeline-characteristic` with the
window open at its end: "Een Vondst is geborgen vanaf 1 januari 2024". A
time-dependent characteristic holds in the periods of its condition
(`RUNTIME-SEMANTICS.md` ch. 3.10, 3.11), and here that period is
[2024-01-01, +∞), with an open bound. A characteristic's true periods carry
no open bound (ch. 3.1): every reader of its buckets, the day count of
`bergdagen` here among them, would walk a period without an end. The
executors decline the form (EF-023), naming the characteristic.

    verifier            PASS
    ref                 EVALUATION FAULT: EF-023: the periods in which the
                        time-dependent characteristic "geborgen" of "V1" holds
                        have an open bound … Declined at evaluation
    clingo              viol: the periods in which the time-dependent
                        characteristic geborgen holds have an open bound …
                        Declined in translation
    Drools, both        FAULT EF-023 {characteristic: geborgen}
    branches

Gate 8 pins the code and the parameter in both OCaml executors and on both
Drools branches.
