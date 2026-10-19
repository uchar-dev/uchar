# jutterswet-voorwaardelijke-initialisatie — an initialisation with a condition

The witness of an initialisation that carries a condition
(RUNTIME-SEMANTICS ch. 5.4): it writes only where its target is empty
AND its condition holds, and a target the case input fills is left
standing. One `Vondst` with a `gewicht` and a `bergloon`, both with two
decimals, and one rule:

    Het bergloon van een Vondst moet geïnitialiseerd worden op 2 maal zijn gewicht
    indien zijn gewicht groter is dan 10.

The ch. 7.7 desugaring does not take this form: a conditional
initialisation's justification would need the input-emptiness guard
conjoined, which no condition can write. `ref` therefore replays it
natively (ch. 5.4), and so does the ASP backend: its `val` head carries
the condition and `not inputfilled(S,"bergloon")`, a case fact beside the
untouched input value.

## Hand-computed absolutes

| case | input | the three branches | `bergloon` |
|---|---|---|---|
| case1-geschreven | V1 `gewicht` 12,00 | empty, 12,00 > 10 holds: written, 2 × 12,00 | V1 **24,00** |
| case2-voorwaarde-onwaar | V1 `gewicht` 10,00 | empty, 10,00 > 10 fails: nothing written | V1 **leeg** |
| case3-invoer-blijft | V1 `gewicht` 12,00, `bergloon` 40,00 | filled by the input: the input stands | V1 **40,00** |
| case4-per-vondst | V1 12,00; V2 8,00; V3 30,00 with `bergloon` 15,00 | each Vondst on its own | V1 **24,00**, V2 **leeg**, V3 **15,00** |

Every case is consistent. case2 sits on the boundary, so an executor that
read the comparison as "at least" would write 20,00 there; in case3 and
for V3 the condition holds, so only the emptiness guard keeps the input.

## Measured (2026-09-25)

- verifier: `verifier: PASS`.
- `ref`: `oracle: PASS (2 expectations)` for case1 to case3,
  `oracle: PASS (4 expectations)` for case4.
- ASP backend: `clingo: PASS (2 expectations)` for case1 to case3,
  `clingo: PASS (4 expectations)` for case4. Before its initialisation arm
  every case ended in `TRANSLATION FAULT: rule kind not translated`.
- Drools: `PLAYED 4` in the exec-model branch and `PLAYED 4` in the
  DRL-text branch.
- The do-nothing run (`--noop`): case1 and case4 answer
  `MISMATCH: V1.bergloon = leeg, expected 24`; case2 and case3 answer
  `oracle: PASS (2 expectations, NO-OP)`. Those two are gate-7 survivors
  by construction: an absence and a standing input are what they assert.

## What it pins

The three branches of the guard, separately and side by side:
condition and emptiness both hold (written), the condition fails on an
empty target (leeg), the input fills the target while the condition holds
(the input stands). Mutating the ASP arm confirms it: without the
emptiness guard case3 answers `MISMATCH: V1.bergloon = [4000;2400],
expected 4000`, and without the condition case2 answers
`MISMATCH: V1.bergloon = [2000], expected leeg`.

The source is `source.rgs`; gate 10 re-emits this document from it
byte-identically. An initialisation beside a computing rule on the same
target is pinned in the ASP backend's own suite, not here: case input on
a computed target stays refused there, so the third branch could not
stand in one golden with it.
