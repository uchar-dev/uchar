# jutterswet-bergingsdrempel — ordered kenmerk deviation (Afwijking van)

Exercises the uchar-dialect **deviation** form
(`frontends/regelspraak/grammar/DIVERGENCES.md` §4, form 2): a base rule
with one `Afwijking van` block, lowered by the RegelSpraak frontend to ONE
`PrioritizedDefaults` on the characteristic `bergingsbevoegd`, the block a
chain position above the base (the ordered form of
PRIORITIZED-DEFAULTS-EVOLUTIE §4.2). Own Jutterswet-flavoured domain
(berging of wrecks), own numbers.

The block widens its base — eight wrecks under noodweer against twenty —
and therefore carries its own condition only. An `Uitzondering op`
conjoins its carrier's condition with its own: written with that header,
the block's justification would carry the base's `>= 20` first, the block
would never decide below twenty wrecks, and case2 would come out absent
(measured 2026-09-25 in ref and clingo). Until the card
`dialect-uitzondering-op-uitzondering` the block was written `Uitzondering
op`, which carried its own condition only at the time; the `ir.json` of
the `Afwijking van` header is byte-identical to the one the old header
gave.

## Source (`bron.rgs`)

- Base rule **bergingsbevoegd bij quotum**: a Jutter is `bergingsbevoegd`
  when `aantal geborgen wrakken >= 20`.
- Deviation **bergingsbevoegd bij noodweer** (`Afwijking van bergingsbevoegd
  bij quotum`): lowers the threshold to 8 when the emergency ground
  `noodweer verklaard` holds.

## Lowering

One `PrioritizedDefaults` (characteristic `bergingsbevoegd`, subject `Jutter`),
defaults in descending priority:

1. **deviation default** — `decision true`, justified by `noodweer verklaard
   AND aantal geborgen wrakken >= 8`.
2. **base default** — `decision true`, justified by
   `aantal geborgen wrakken >= 20`.

Both the base rule and the deviation leave `rules` (WF-050
single-writer: their semantics live in the defaults). Until L1 each also kept
a `Regel` stub in the `regels` register, which left the wire with the M1
card (B3). The base rule's `geldig altijd` is carried onto the defaults as
a `validity` with neither `from` nor `to`.

## Case derivations (hand-computed, absolute)

Walk top-down; the highest deciding default wins. Both defaults decide
`true`, so the defaults attribute `bergingsbevoegd` iff the deviation OR the
base is justified.

| case | wrakken | noodweer verklaard | deviation (>=8 & noodweer) | base (>=20) | bergingsbevoegd |
|------|---------|--------------------|----------------------------|-------------|-----------------|
| case1-quotum        | 20 | no  | no (no noodweer) | **yes** | **present** |
| case2-noodweer      | 8  | yes | **yes**          | no      | **present** |
| case3-onder-quotum  | 12 | no  | no (no noodweer) | no (12<20) | **absent** |
| case4-onder-noodweer| 5  | yes | no (5<8)         | no      | **absent** |

All four are `consistent` — a single set of characteristic defaults never
conflicts.
case2 shows the deviation lowering the threshold; case3 shows the deviation is
inert without the ground; case4 shows the lowered threshold still has a floor.
