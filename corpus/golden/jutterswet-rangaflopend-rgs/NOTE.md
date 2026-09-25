# jutterswet-rangaflopend-rgs — an allocation in order, written in RegelSpraak

The RegelSpraak-source twin of `jutterswet-rangaflopend`: a `Sluitkas`
distributes its `sluitpremie` over its `sluitjutters` in descending order of
`strandrang`, in equal shares within a tie, each share floored at 0
decimals, and the undistributed rest goes to its `sluitrest`. The source is
`bron.rgs` (§13.4.10 verdeling, `op volgorde van`), the document is the
RegelSpraak frontend's emission, and gate 10 re-emits it byte for byte, so an
`Allocation` with `method: "ordered"` (IR-SPEC ch. 3.2) is pinned as
frontend output for the first time. `case.json` is the carrier's case file,
copied byte for byte, with the same absolute expectations.

## Source (`bron.rgs`)

```
De sluitpremie van een sluitkas wordt verdeeld over het sluitdeel van alle
sluitjutters van de sluitkas, waarbij wordt verdeeld:
- op volgorde van afnemende strandrang,
- bij een even groot criterium in gelijke delen,
- afgerond op 0 decimalen naar beneden.
Als onverdeelde rest blijft de sluitrest van de sluitkas over.
```

The grammar's ordered form requires the tie-break line;
`in gelijke delen` is the unweighted tie-break, so the emission carries no
`ratioAttribute`. `afnemende` becomes `descending: true`, the rounding line
`roundingDecimals: 0`, and the rest line `remainderAttribute`. On an
allocation the frontend does write the `connectingRole`: it derives the
binding from the pair `Sluitkas`/`Jutter` and refuses to emit an allocation
whose pair has no unique relation (`IrEmitter.allocationVersion`).

## Structural diff against `jutterswet-rangaflopend/ir.json`

None. With every `source` stripped and the keys sorted the two documents are
equal, and they are equal byte for byte as well: neither carries a `source`
record, because the frontend writes none on an allocation and the carrier
has none. The carrier could therefore have carried this source itself.

## Hand computation

The ordered method per RUNTIME-SEMANTICS ch. 4.2, the edge rows per ch. 4.4.
The groups are the equal-key classes of `strandrang`, descending, with an
empty key last and no error for it:

| group | members | pot at entry | share each (floor, 0 dec.) | pot after |
|---|---|---|---|---|
| G1 | J1 (22) | 1200 | 1200 / 1 = **1200** | 0 |
| G2 | J2, J3 (20) | 0 | 0 / 2 = **0** | 0 |
| G3 | J4 (empty key) | 0 | **0** | 0 |

`sluitrest` of S1 = 1200 − 1200 = **0**. The shares of G2 and G3 are real
zeros, not empty (ch. 4.4, a pot exhausted before a group). The case expects
these five values and `consistent`. The input carries no `sluitdeel` and no
`sluitrest`, so a no-op run fails the case and it needs no survivor entry.

## Executors

ref and clingo replay the case with the pinned values; the Drools backend
plays it in both branches, exec-model and DRL text, `PLAYED 1` each
(measured 2026-09-25).
