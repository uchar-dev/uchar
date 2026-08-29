Violates WF-031 (`spec/IR-SPEC.md` par. 5 / ch. 3.7, the purity clause):
a day-kind voorwaarde is evaluated against a bare date before the rule
graph, with no fact base (the stratum-0 framing of par. 5), so it may
reference only dates, date-components, eerste-paasdag, literals,
parameters and other day-kind names — never derived object state. Here
the day-kind `feestdag`'s condition is a characteristic check reading
`penninggerechtigd` of a `Jutter`, which the verifier refuses.

Rood-eerst: before the purity clause landed the pre-card verifier
accepted this document (a characteristic read in a day-kind condition was
silently admitted); the WF-031 walk now rejects it, citing the read.

A minimal hand-built document; the day-kind reads a characteristic of the
one declared object type.
