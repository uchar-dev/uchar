# jutterswet-initialisatievolgorde — an initialisation whose value a later rule computes

The witness that an initialisation's guard reads the input and not the
initialisation's own write (RUNTIME-SEMANTICS ch. 5.4, ch. 7.3). One
`Vondst` with four attributes of two decimals and three rules, in this
order:

    Het bergloon van een Vondst moet geïnitialiseerd worden op 2 maal zijn berekend gewicht
    indien zijn gewicht groter is dan 10.

    Het vindloon van een Vondst moet geïnitialiseerd worden op zijn berekend gewicht plus 5.

    Het berekend gewicht van een Vondst moet berekend worden als het gewicht van de Vondst plus 1.

Both initialisations read `berekend gewicht`, which the third rule
computes, and both are written before it. An executor that evaluates an
initialisation before `berekend gewicht` exists writes a value computed
against a leeg read (0 for `bergloon`, 5 for `vindloon`); if its guard then
counts that write as a filled target, the value stays. The guard reads
the input facts instead, so the initialisation is re-evaluated when
`berekend gewicht` arrives, and the answer does not depend on the order of
the rules.

The conditional `bergloon` lies outside the ch. 7.7 desugaring, so `ref`
and the ASP backend both replay it natively; the unconditional `vindloon`
is inside it, so `ref` replays it natively while the ASP backend
translates the desugared defaults group. Both paths are pinned.

## Hand-computed absolutes

| case | input | `berekend gewicht` | `bergloon` | `vindloon` |
|---|---|---|---|---|
| case1-beide-geschreven | V1 `gewicht` 12,00 | **13,00** | 12,00 > 10: 2 × 13,00 = **26,00** | 13,00 + 5 = **18,00** |
| case2-voorwaarde-onwaar | V1 `gewicht` 8,00 | **9,00** | 8,00 > 10 fails: **leeg** | 9,00 + 5 = **14,00** |
| case3-invoer-blijft | V1 `gewicht` 12,00, `bergloon` 40,00, `vindloon` 3,00 | **13,00** | the input stands: **40,00** | the input stands: **3,00** |
| case4-per-vondst | V1 12,00; V2 10,00; V3 30,00 with `vindloon` 7,00 | — | V1 **26,00**, V2 **leeg** (10,00 > 10 fails), V3 **62,00** | V1 **18,00**, V2 **16,00**, V3 **7,00** |

Every case is consistent.

## Measured (2026-09-25)

- verifier: `verifier: PASS`.
- `ref`: `oracle: PASS (4 expectations)` for case1 to case3,
  `oracle: PASS (7 expectations)` for case4.
- ASP backend: `clingo: PASS (4 expectations)` for case1 to case3,
  `clingo: PASS (7 expectations)` for case4.
- Drools: `PLAYED 4` in the exec-model branch. The DRL-text branch keeps the
  transient values: `FAIL case1-beide-geschreven.json: attribuut bergloon:
  verwacht 26.00, werkelijk 0; attribuut vindloon: verwacht 18.00,
  werkelijk 5`, and so on for case2 and case4.
- The do-nothing run (`--noop`) fails every case, case3 with
  `MISMATCH: V1.berekend gewicht = leeg, expected 13`.

Before the guard read the input, `ref` answered this document with
`MISMATCH: V1.bergloon = 0, expected 26 MISMATCH: V1.vindloon = 5,
expected 18` on case1, and the Drools exec-model branch with `FAIL
case1-beide-geschreven.json: attribuut bergloon: verwacht 26.00, werkelijk
0; attribuut vindloon: verwacht 18.00, werkelijk 5`; the ASP backend
passed every case.

## What it pins

The mutant is the document order. With `berekend gewicht` written first,
the same cases pass in all four executors before the change and after it:
`oracle: PASS`, `clingo: PASS`, `PLAYED 4` in both Drools branches. With
the order of this source, only the guard that reads the input passes them.

The source is `bron.rgs`; gate 10 re-emits this document from it
byte-identically.
