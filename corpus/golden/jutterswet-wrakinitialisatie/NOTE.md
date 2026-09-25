# jutterswet-wrakinitialisatie — an initialisation with a condition in a subtype's stack

The witness of an initialisation that the inheritance fold takes into a
defaults stack (RUNTIME-SEMANTICS ch. 7.10). `Vondst` carries `gewicht`
and `bergloon`, both with two decimals; `Wrakvondst is een Vondst`. One
rule on the supertype and one defaults group on the subtype:

    Het bergloon van een Vondst moet geïnitialiseerd worden op 2 maal zijn gewicht
    indien zijn gewicht groter is dan 10.

    Het bergloon van een Wrakvondst moet gesteld worden op 5,00
    indien zijn gewicht groter is dan 50.
    (exception) ... op 9,00 indien zijn gewicht groter is dan 100.

On a `Wrakvondst` the group's chain declares a defaults group on
`bergloon`, so the initialisation is folded into that stack as its
initialisation default and does not fire as a rule there. On a plain
`Vondst` nothing on the chain declares a group, and the rule fires as the
ordinary conditional initialisation of ch. 5.4.

Two things are pinned at once. The folded initialisation keeps its
condition: its default is justified where the target is empty in the
input facts and `gewicht > 10` holds. And the initialisation's condition
and value read `gewicht` as an own read of `Vondst`, the level that
declared them, while the stack belongs to `Wrakvondst`: every executor
evaluates a default on its declaring level.

## Hand-computed absolutes

The exception's justification is the conjunction the frontend writes for
an `Uitzondering op` block: `gewicht > 50` and `gewicht > 100`.

| case | instance | `gewicht` | input `bergloon` | who decides | `bergloon` |
|---|---|---|---|---|---|
| case1-wrakvondst-voorwaarde | W1 | 8,00 | — | nothing: 8,00 > 10 fails | **leeg** |
| | W2 | 12,00 | — | the initialisation default, 2 × 12,00 | **24,00** |
| case2-wrakvondst-stapel | W3 | 60,00 | — | the base | **5,00** |
| | W4 | 120,00 | — | the exception | **9,00** |
| | W5 | 12,00 | 40,00 | the input stands; the initialisation is not justified | **40,00** |
| case3-vondst | V1 (`Vondst`) | 12,00 | — | the rule, 2 × 12,00 | **24,00** |
| | V2 (`Vondst`) | 8,00 | — | nothing | **leeg** |
| case4-grens-naast-elkaar | V1 (`Vondst`) | 10,00 | — | nothing: 10,00 > 10 fails | **leeg** |
| | W1 | 10,00 | — | nothing, in the stack | **leeg** |
| | W2 | 30,00 | — | the initialisation default, 2 × 30,00 | **60,00** |

Every case is consistent. case4 sits on the boundary on both sides, so an
executor that read the comparison as "at least" would write 20,00 twice.

## Measured (2026-09-25)

- verifier: `verifier: PASS`.
- `ref`: `oracle: PASS (3 expectations)` for case1 and case3,
  `oracle: PASS (4 expectations)` for case2 and case4.
- ASP backend: `clingo: PASS (3 expectations)` for case1 and case3,
  `clingo: PASS (4 expectations)` for case2 and case4.
- Drools: `PLAYED 4` in the exec-model branch; the DRL-text branch answers
  `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no
  defaults lowering`.
- The do-nothing run (`--noop`) fails every case: case1 with
  `MISMATCH: W2.bergloon = leeg, expected 24`, case3 with
  `MISMATCH: V1.bergloon = leeg, expected 24`.

Before the fold kept the condition and read each default on its declaring
level, the three executors answered this document three ways: `ref` gave
`EVALUATION FAULT: an attribute reference without a path is the subject's
own read, so its subjectType is the subject type 'Wrakvondst' — 'Vondst'
names another type, which only a navigation reaches` on case1 and case4;
the ASP backend refused every case with `TRANSLATION FAULT: the
initialisation of "bergloon" shares its target with a defaults group, an
allocation or an object creation: ch. 5.4 places an initialisation beside
ordinary rules only (no corpus case)`; and the Drools exec-model branch
refused the stack with `EXCLUDE transpiler SKIP: defaults: defaults
A:Wrakvondst.bergloon — defaults (Vondst.bergloon): niet-reduceerbare
consequentie (far-side/aggregatie): standaardbergloon`.

## What it pins

Folding the initialisation without its condition again gives
`MISMATCH: W1.bergloon = 16, expected leeg` in `ref`,
`MISMATCH: W1.bergloon = [1600], expected leeg` in the ASP backend and
`FAIL case1-wrakvondst-voorwaarde.json: verwacht leeg, werkelijk 16.00` in
the Drools exec-model branch, and case4 answers 20 for W1 in all three.
Reading every default of the composed stack on the owner's level again
brings back the own-read fault in `ref` and the ASP backend and the
far-side refusal in the Drools exec-model branch.

The source is `bron.rgs`; gate 10 re-emits this document from it
byte-identically.
