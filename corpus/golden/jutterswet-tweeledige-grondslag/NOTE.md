# jutterswet-tweeledige-grondslag — a default with two statute annotations

The witness of `Default.statuteAnnotations` (IR-SPEC ch. 6.1, WF-082; card
`default-statute-annotations`, 2026-10-21): the `// Bron:` annotations of a
lifted block stand on the default the block becomes as a list, the field a
rule carries (ch. 3.1), and not joined into the default's `source`. One of
the two annotations of the base contains `"; "`, the separator the frontend
used to join with, so the joined form could not tell this block from one
with three annotations. The law is the Jutterswet (`corpus/jutterswet/wet-v4.md`):

- **Art. 3 (penningrecht)** grants the right to the penning to the erkende
  jutter who registered at least as many finds as the vondstdrempel; the
  base rule takes the threshold of twelve, as `jutterswet-schorsing` does.
- **Art. 3b (telling)** says how the finds are counted; the base rule reads
  the count as input, and the annotation records the article it rests on,
  with a remark after a `;`.
- **Art. 4bis (schorsing)** and **art. 4 (uitsluiting)**: a geschorste or a
  geweigerde raapvergunning takes the right away. Art. 4 is cut to its first
  layer here; `jutterswet-uitsluiting-rgs` writes all three.

## Source (`source.rgs`)

- Base rule **penningrecht art 3**, with two annotations:
  `// Bron: Jutterswet art. 3` and
  `// Bron: Jutterswet art. 3b; een vondst op een stormvloeddag telt tweemaal`.
- Exception **schorsing art 4bis**, annotated `// Bron: Jutterswet art. 4bis`:
  `Een Jutter heeft geen recht op de penning indien hij vergunning geschorst is.`
- Exception **weigering art 4**, without an annotation:
  `Een Jutter heeft geen recht op de penning indien hij vergunning geweigerd is.`

## Lowering

One `PrioritizedDefaults` `penningrecht art 3` (characteristic `recht op de
penning`, subject `Jutter`), defaults in descending priority — the later
written exception stacks higher:

1. `weigering art 4` — `decision false`, justified by the `all` of the base's
   comparison and `vergunning geweigerd`; no `statuteAnnotations` (the block
   has none) and no `source`.
2. `schorsing art 4bis` — `decision false`, the same with `vergunning
   geschorst`; `"statuteAnnotations": ["Jutterswet art. 4bis"]`.
3. `penningrecht art 3` — `decision true`, justified by the comparison;
   `"statuteAnnotations": ["Jutterswet art. 3", "Jutterswet art. 3b; een
   vondst op een stormvloeddag telt tweemaal"]` — two entries, the second
   with its `"; "` inside it.

No default carries a `source`: the frontend has no source text for a block
as a whole. The group's own `source` keeps the base's annotations joined
with `"; "` (`grammar/DIVERGENCES.md`, "`// Bron:` annotations of lifted
rules"): that field is a single source record, and the card moved only the
defaults' annotations to a list.

## Cases

From `scenario.rgs` (gate 16 regenerates them byte for byte):

| case | input | expected |
|---|---|---|
| `case1-boven-de-drempel` | K, 14 finds | K has the right |
| `case2-geschorst` | J, 14 finds, vergunning geschorst; K | J has no right, K has it |
| `case3-geweigerd` | L, 14 finds, vergunning geweigerd; K | L has no right, K has it |
| `case4-onder-de-drempel` | M, 5 finds; K | M has no right, K has it |

Every case is consistent. K, with 14 finds and no exception, stands in every
case: an expectation that holds on the raw input alone (no right, and
consistent) would let the identity transpiler pass (gate 7), and K's right
is derived. The annotations carry no semantics, so the
answers are those of the same document without them; gate 13 replays the
golden with every `statuteAnnotations` of a default stripped and demands
the same answer from ref, clingo and both Drools branches.

## Red first

Measured before the frontend change, on the frontend of `4f283d9d`: the
frontend wrote `"source": {"snippet": "Jutterswet art. 3; Jutterswet art.
3b; een vondst op een stormvloeddag telt tweemaal"}` on the base default,
three pieces when split on `"; "` for two annotations; the hand-written
`ir.json` with the two lists was refused by the schema (`statuteAnnotations`
was unexpected on a Default) and by the verifier's loader (`unknown field
'statuteAnnotations'`).
