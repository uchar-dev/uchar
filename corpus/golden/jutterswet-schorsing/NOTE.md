# jutterswet-schorsing — a negated exception (`heeft geen`) on a positive base

Exercises form 1 of the uchar-dialect defaults extension
(`frontends/regelspraak/grammar/DIVERGENCES.md` §4): a negated
characteristic decision in an `Uitzondering op` block, lowered by the
RegelSpraak frontend to a default with `besluit: false` on the base's own
target. The law is the Jutterswet (`corpus/jutterswet/wet-v4.md`):

- **Art. 3 (penningrecht)** grants the right to the penning to the erkende
  jutter who registered at least as many finds as the vondstdrempel. The
  base rule takes the threshold of twelve that the Derde
  wijzigingsverordening fixes for penningjaar 2025, and leaves out the two
  qualifications this golden does not test: the subject is the erkende
  jutter, and "onbesproken juttersgedrag" is an open concept.
- **Art. 4bis (schorsing)**: "Geen recht op de penning heeft evenmin degene
  wiens raapvergunning is geschorst." The statute states the exclusion as a
  negation of the right, and the exception block writes it that way:
  `Een Jutter heeft geen recht op de penning indien hij vergunning geschorst
  is.` The characteristic `recht op de penning` is bezittelijk, so the
  negation is `heeft geen` (Tabel 16).

## Source (`bron.rgs`)

- Base rule **penningrecht art 3**: `Een Jutter heeft recht op de penning
  indien zijn aantal aangemelde vondsten groter of gelijk is aan 12.`
- Exception **schorsing art 4bis** (`Uitzondering op penningrecht art 3`):
  `Een Jutter heeft geen recht op de penning indien hij vergunning
  geschorst is.`

Each block carries a `// Bron:` annotation; the lowering places each on the
default the block becomes, and the base's also on the group.

## Lowering

One `PrioritizedDefaults` (kenmerk `recht op de penning`, onderwerp
`Jutter`, named after the base), defaults in descending priority:

1. **exception default** — `besluit false`, justified by `vergunning
   geschorst`; `bron` "Jutterswet art. 4bis".
2. **base default** — `besluit true`, justified by `aantal aangemelde
   vondsten >= 12`; `bron` "Jutterswet art. 3".

The target is the declared characteristic `recht op de penning`: the
frontend reads `geen recht op de penning` verbatim first, finds no
characteristic of that whole name, and binds the rest. Both rules leave
`resolvedRegels` (WF-050: one writer per target), and `diagnostics` is
empty.

A `false` decision writes nothing (RUNTIME-SEMANTICS ch. 7.1; ch. 5.3's
monotone writes hold), so it leaves a characteristic the input sets in
place. None of the cases supplies `recht op de penning` as input: whether
input should rank below the rules is the question of a card of its own,
and this golden does not pin an answer to it.

## Case derivations (hand-computed, absolute)

Walk top-down; the highest deciding default wins. The exception is
justified iff the jutter's vergunning is geschorst, and then decides
`onwaar`, so nothing is written whatever the base would decide. Otherwise
the base decides `waar` iff the count is at least 12. Without a deciding
default the characteristic is absent (closed world, §3.5).

| case | label | vondsten | vergunning geschorst | exception | base (>= 12) | recht op de penning |
|------|-------|----------|----------------------|-----------|--------------|---------------------|
| case1-drempel                 | J | 12 | no  | not justified     | **waar**  | **present** |
| case2-geschorst               | J | 14 | yes | **onwaar**        | (waar)    | **absent**  |
|                               | K | 14 | no  | not justified     | **waar**  | **present** |
| case3-geschorst-onder-drempel | J | 5  | yes | **onwaar**        | (none)    | **absent**  |
|                               | K | 12 | no  | not justified     | **waar**  | **present** |
| case4-onder-drempel           | J | 11 | no  | not justified     | none      | **absent**  |
|                               | K | 12 | no  | not justified     | **waar**  | **present** |

A value in parentheses is what the base would decide; the exception
decides first. case2 is the negated exception displacing the base on the
same count as a sibling the base grants; case3 is the negated exception
deciding where the base would decide nothing, with the same absent outcome;
case1 and case4 put the threshold's two sides next to each other. All four
are `consistent`: one ordered group never conflicts.

Every case carries an instance the base grants, so every case fails a
no-op run and none needs a survivor entry.

## Executors

ref and clingo replay all four cases. The Drools exec-model branch lowers a
defaults group and plays them; the DRL-text branch refuses every document
with `prioritizedDefaults` (no defaults lowering in that branch).

Under the restrictive reading of `Uitzondering op` decided for the nesting
card (an exception's justification conjoined with its carrier's), the
exception default's justification becomes `aantal aangemelde vondsten >= 12`
and `vergunning geschorst`. Every expected value above stays: where the
count is below 12 neither default then decides, and the characteristic is
absent either way. Only the `ir.json` changes.
