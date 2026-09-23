# wf046-leegtest-aanwezigheid-recursieve-creatie

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3), through the both-polarities clause of the
maintainer decision of 2026-09-23 and through nothing else.

The recursive creation "werf de opvolger" (group "estafette") mints a
successor Wachter while "het merk van de Wachter is niet leeg": the test is
true only when merk is present. The rule "merk van een wachter" writes merk
as the rang of the successor, a plain read with no leeg/gevuld test. The only
leeg/gevuld test on the cycle is therefore one that holds on presence.

    WF-046 FAIL /objectCreaties/0: negative dependency edge on a cycle: werf de opvolger reads what merk van een wachter produces while merk van een wachter is reachable from it (no strata exist)

This document is not order-dependent. Its one fixpoint mints nothing: merk is
leeg, so the creation does not fire, so there is no successor, so merk stays
leeg. Measured 2026-09-23 with one input Wachter W1 of rang 1: the reference
interpreter leaves merk(W1) leeg and the ASP backend has exactly one stable
model with the same answer. It is refused because the decision counts a
leeg/gevuld test as negative whether it carries `negatie: false` or `true`:
the test depends on absence in both readings.

The case is what tells that decision apart from the narrower reading "only
the tests that hold on absence (leeg, gevuld negated) are negative". Under
the narrower reading this document passes; the other two
`wf046-leegtest-*` witnesses fail either way.

Minimal own-authored document on the shape of
`corpus/golden/anchor/jutterswet-wachtersketen`.
