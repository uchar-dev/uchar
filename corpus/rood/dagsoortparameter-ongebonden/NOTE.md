# dagsoortparameter-ongebonden

A day-kind voorwaarde that reads a **parameter the case does not bind**.
Fail-fast, in both executors, in the same words.

## The shape

`jutterswet-havenrooster`'s document verbatim — a `havendag` is a day later
than the `openingsdatum` parameter whose day-of-month is 1 — replayed with a
case whose `parameters` object is **empty**.

## Why a RED case, and what it caught

WF-031 admits a parameter in a day-kind voorwaarde: a parameter is model data,
not derived state, so a day kind may be bound to one and this document is
verifier-clean. What a case may NOT do is leave it unbound: binding is
fail-fast (`RUNTIME-SEMANTICS.md` ch. 5.7), and an unbound parameter gives the
predicate no truth value at all — so the day kind has no extension, and every
duration reckoned against it has no answer.

That is the defect this case pins, found by review (F1, 2026-09-19).
`ref` faulted; the ASP backend, which EVALUATES the predicate at translation
time to build the day kind's extension, read the missing binding as **empty**
and carried on — shipping a confident table, and with it confident answers,
built on a comparison that had silently become false. Measured before the fix:
`ref` `parameter "openingsdatum" is unbound in this case`, clingo a PASS with
a full set of values. Both now fault, and the fragment is one sentence shared
by both.

## The green twin

`corpus/golden/jutterswet-havenrooster` is the same document with the
parameter bound, over two cases that bind it **differently** — and the two
bindings move the answers, which is what makes the parameter observable rather
than merely present. The Drools backend does not reach fire on either: its
day-kind predicate compiler carries no parameter operand at all (it compiles a
voorwaarde once into the KieBase, while parameter values arrive per case as
session globals), so it is listed by name with that reason in gate 8's and
gate 12's channels.
