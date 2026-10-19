# jutterswet-erfketen — rule dependencies over a subtype chain, in both directions

A hand-computed Jutterswet golden whose rules depend on each other across the
subtype chain `Klipjutter` ⊑ `Jutter`. It exists for the rule graph of the
Drools backend and gate 15: until it, no golden had a rule → rule dependency
over a subtype chain, so the spec leg of `scripts/check-graph.sh` could not
see that the graph keys a member on its DECLARING type.

## Source (`source.rgs`)

`Jutter` declares `vondstwaarde`, `strandloon`, `kliptoeslag`, `jutterloon`,
`hoogtepremie` (whole, non-negative) and the characteristic `hoogwerker`;
`Klipjutter` is a `Jutter` and declares `klimhoogte` and `klipbonus`. Six
rules:

- `strandloon van een jutter`: strandloon = vondstwaarde maal 2 (written on
  the supertype);
- `klipbonus van een klipjutter`: klipbonus = strandloon plus 5, indien
  strandloon groter is dan 10 (read on the subtype);
- `kliptoeslag van een klipjutter`: kliptoeslag = klimhoogte maal 3
  (written on the subtype, declared on the supertype);
- `jutterloon van een jutter`: jutterloon = strandloon plus kliptoeslag
  (read on the supertype);
- `hoogwerker`: een Klipjutter is hoogwerker indien klimhoogte groter is
  dan 20 (a characteristic written on the subtype, declared on the
  supertype);
- `hoogtepremie van een jutter`: hoogtepremie = 7 indien hij hoogwerker is
  (read on the supertype).

## The dependencies (IR-SPEC ch. 5)

`verifier --dependency-edges` prints four positive edges, and the graph's
projection (`GraphProjection`) the same four dependencies:

    strandloon van een jutter      -> attr strandloon  -> jutterloon van een jutter
    strandloon van een jutter      -> attr strandloon  -> klipbonus van een klipjutter
    kliptoeslag van een klipjutter -> attr kliptoeslag -> jutterloon van een jutter
    hoogwerker                     -> char hoogwerker  -> hoogtepremie van een jutter

The last three cross the chain: the second from the supertype to the
subtype, the third and fourth from the subtype to the supertype. The
verifier keys a read on the type it spells and closes a write over the
comparable types; the graph keys both on the declaring type, `Jutter`.

Measured 2026-09-26 with the graph keyed on the holder type instead (in
`ProducentConsument.elementVan`, the declarant replaced by the holder):
gate 15 fails on this golden, `the rule graph disagrees with IR-SPEC ch. 5
on 3 dependency line(s)`, the three chain dependencies listed as drawn by
the verifier only. The same mutant with this golden set aside leaves the
spec leg green (232 of 269 goldens compared, 45 dependencies) and fails only
the route leg's unit tests.

## Hand computation

`maal` and `plus` coalesce an empty operand to 0 (RUNTIME-SEMANTICS ch. 1.2),
an ordering comparison with an empty operand is false, and a rule whose
condition is false writes nothing (leeg).

| case | label | type | vondstwaarde | klimhoogte | strandloon | klipbonus | kliptoeslag | jutterloon | hoogwerker | hoogtepremie |
|---|---|---|---|---|---|---|---|---|---|---|
| case1-klipjutters | K1 | Klipjutter | 8 | 25 | **16** | **21** | **75** | **91** | **present** | **7** |
| case1-klipjutters | K2 | Klipjutter | 4 | 20 | **8** | **leeg** | **60** | **68** | **absent** | **leeg** |
| case2-jutter-en-leeg | J1 | Jutter | 9 | — | **18** | — | **leeg** | **18** | **absent** | **leeg** |
| case2-jutter-en-leeg | K3 | Klipjutter | leeg | leeg | **0** | **leeg** | **0** | **0** | **absent** | **leeg** |

K2's strandloon of 8 is not greater than 10, so klipbonus stays leeg; its
klimhoogte of 20 is not greater than 20, so it is no hoogwerker. J1 is a
plain Jutter: no rule writes its kliptoeslag, which `plus` reads as 0. K3
has no input at all. Both cases also expect `consistent`.

## Executors (measured 2026-09-26)

- verifier: `verifier: PASS`.
- `ref`: `oracle: PASS (13 expectations)` on case1 and `oracle: PASS (12
  expectations)` on case2; the ASP backend `clingo: PASS` on the same
  counts.
- Drools: `PLAYED 2` in the exec-model branch and `PLAYED 2` in the DRL-text
  branch.
- gate 10: `source.rgs` re-emits this `ir.json` byte for byte.

## noop refutation (gate 7)

The `--noop` run fails both cases: case1 on 9 expectations (`MISMATCH:
K1.strandloon = leeg, expected 16` first) and case2 on 5 (`MISMATCH:
J1.strandloon = leeg, expected 18` first). Not a survivor.
