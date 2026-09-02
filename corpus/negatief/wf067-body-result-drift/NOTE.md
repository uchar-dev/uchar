Violates WF-067 (`spec/IR-SPEC.md` ch. 2.11) on its body/result clause:
the `FunctionDef` `Kies` declares the result `Enumeratie` `Vaarwater` and
its body is a `Literal` `Noord` typed `Enumeratie` `Kust` — a second,
internally consistent domain. The `Invoke` is truthful (typed `Vaarwater`,
so WF-068 is clean) and feeds a `Match` whose arms cover `Vaarwater`.

    WF-067 FAIL /functionDefs/0/body/type: the body is enumeration 'Kust',
    but the declared result is enumeration 'Vaarwater'

Why it has to exist: found by probing for a route to ref's "no arm" fault
AFTER the read side (WF-089) and the attribute write side (WF-023) were
closed. WF-068 crosses an `Invoke`'s type and arguments against the
declared result and parameters — the READ of a function — but nothing
crossed the body, the WRITE into that result. Verifier-clean before; ref
faulted `match has no arm for enumeration value "Noord"`; clingo faulted
the translation (the pre-existing carrier fence on an enumeration-typed
`Invoke` operand, which the faithful twin — body `Wad` typed `Vaarwater`,
ref PASS — hits identically). The cross is full datatype equality, soort
and domain alike, like WF-068/069/070: a number body under an enumeration
result is refused by the same line.

Falsifiable: make the body/result arm of `wf067` (`Some bt, r when bt <>
r`) return `[]` and this case passes while every other `wf067-*` case
stays red.
