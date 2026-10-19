# jutterswet-dagentelling-rgs — the day count written in RegelSpraak

The RegelSpraak-source twin of `anchor/jutterswet-dagentelling`: the same type,
the same two temporal attributes, the same temporal characteristic and the
same two day counts, written in `source.rgs` and emitted by the RegelSpraak
frontend. Gate 10 re-emits `ir.json` from the source byte for byte, so this is
the first golden in which the frontend's emission of the day count
(`BuiltinCall` of kind `dayCount`, `RUNTIME-SEMANTICS.md` ch. 3.8) is pinned.
The four `case*.json` are the anchor's, copied byte for byte, with the same
absolute expectations.

## Source (`source.rgs`)

- `is borgplichtig kenmerk (bijvoeglijk) voor elke maand`: the temporal
  characteristic the counts read.
- `borgdagen` (`voor elke maand`) and `jaardagen` (`voor elk jaar`), both
  `niet-negatief geheel getal`.
- `De borgdagen van een Jutter moet berekend worden als het aantal dagen in
  de maand dat hij borgplichtig is.` and the same with `het jaar` into
  `jaardagen`: each count's stated bucket equals its target's granularity
  (WF-058).

## The type of the count

Until the card frontend-emitterrestanten the frontend wrote the day count
with `"type": null`: the resolver makes no type claim for this builtin, and
WF-034 refuses such a node ("value node without a static type"). The emitter
now writes the type the spec fixes for it: a count of days is typed as every
count is (`RUNTIME-SEMANTICS.md` ch. 2.3 and ch. 3.8), a whole, non-negative
number without a unit — `{Number, whole true, decimals 0, sign nonNegative}`,
the type every `dayCount` in the corpus carries, the anchor's two included.

## Structural diff against `anchor/jutterswet-dagentelling/ir.json`

Read with every `source` stripped, the two documents are equal. At the byte
level they differ in three `source` records only: the anchor's two filters
read `gedurende de tijd dat hij borgplichtig is` where the emitted filters
carry the snippet of the whole count, and the anchor's second count reads
`in de jaar` where the source writes `in het jaar`.

## Executors

ref and clingo replay the four cases with the pinned values. The Drools
backend plays them in both branches, exec-model and DRL text, `PLAYED 4` each,
as it plays the anchor (measured 2026-09-25).

Two of the four cases survive a no-op run for the anchor's reasons, and gate 7
lists them beside the anchor's: `case3-leeg` (a count over no condition
periods writes nothing, and that absence is the assertion) and
`case4-input-blijft` (a count that decides nothing leaves the case-supplied
input timeline standing whole, ch. 7.8).
