# jutterswet-schorsing — a negated exception (`heeft geen`) on a positive base

Exercises form 1 of the uchar-dialect defaults extension
(`frontends/regelspraak/grammar/DIVERGENCES.md` §4): a negated
characteristic decision in an `Uitzondering op` block, lowered by the
RegelSpraak frontend to a default with `decision: false` on the base's own
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

RUNTIME-SEMANTICS ch. 7.1 takes art. 4bis ("evenmin") as its example of
parallel grounds: separate defaults groups, each on its own target, that a
consumer combines. This golden writes the article's own sentence instead, as
an exception on the art. 3 target, because that sentence denies the right
itself. With one ground on each side the two models give the same answer:
the right holds iff the count reaches 12 and the vergunning is not geschorst.

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

1. **exception default** — `decision false`, justified by one flat `all`
   of `aantal aangemelde vondsten >= 12` and `vergunning geschorst`, the
   base's condition first; `source` "Jutterswet art. 4bis".
2. **base default** — `decision true`, justified by `aantal aangemelde
   vondsten >= 12`; `source` "Jutterswet art. 3".

The target is the declared characteristic `recht op de penning`: the
frontend reads `geen recht op de penning` verbatim first, finds no
characteristic of that whole name, and binds the rest. Both rules leave
`rules` (WF-050: one writer per target), and `diagnostics` is
empty.

A `false` decision writes nothing (RUNTIME-SEMANTICS ch. 7.1; ch. 5.3's
monotone writes hold), so it leaves a characteristic the input sets in
place. None of the cases supplies `recht op de penning` as input: whether
input should rank below the rules is the question of a card of its own,
and this golden does not pin an answer to it.

## Case derivations (hand-computed, absolute)

Walk top-down; the highest deciding default wins. The exception is
justified iff the count is at least 12 and the jutter's vergunning is
geschorst, and then decides `onwaar`, so nothing is written although the
base would decide `waar`. Otherwise the base decides `waar` iff the count
is at least 12. Without a deciding default the characteristic is absent
(closed world, §3.5).

| case | label | vondsten | vergunning geschorst | exception | base (>= 12) | recht op de penning |
|------|-------|----------|----------------------|-----------|--------------|---------------------|
| case1-drempel                 | J | 12 | no  | not justified     | **waar**  | **present** |
| case2-geschorst               | J | 14 | yes | **onwaar**        | (waar)    | **absent**  |
|                               | K | 14 | no  | not justified     | **waar**  | **present** |
| case3-geschorst-onder-drempel | J | 5  | yes | not justified     | none      | **absent**  |
|                               | K | 12 | no  | not justified     | **waar**  | **present** |
| case4-onder-drempel           | J | 11 | no  | not justified     | none      | **absent**  |
|                               | K | 12 | no  | not justified     | **waar**  | **present** |

A value in parentheses is what the base would decide; the exception
decides first. case2 is the negated exception displacing the base on the
same count as a sibling the base grants; in case3 the count is below the
threshold, so neither default is justified and the characteristic is
absent; case1 and case4 put the threshold's two sides next to each other.
All four are `consistent`: one ordered group never conflicts.

Every case carries an instance the base grants, so every case fails a
no-op run and none needs a survivor entry.

## Executors

ref and clingo replay all four cases. The Drools exec-model branch lowers a
defaults group and plays them; the DRL-text branch refuses every document
with `prioritizedDefaults` (no defaults lowering in that branch).

## The carrier's condition

Since the card dialect-uitzondering-op-uitzondering an `Uitzondering op`
is restrictive (`frontends/regelspraak/grammar/DIVERGENCES.md` §4): the
frontend conjoins the base's condition into the exception's
justification. The source is unchanged; the `ir.json` was re-emitted, and
its only change is the exception default's justification, which was
`vergunning geschorst` alone. Every expected value stays, and all four
cases pass in ref, clingo and the Drools exec branch on the new `ir.json`:
the two readings differ only where the count is below 12 and the
vergunning is geschorst (case3), and there the exception decided `onwaar`
before and nothing decides now, which leaves the characteristic absent
either way. Art. 4bis does not widen art. 3, so the block keeps its
`Uitzondering op` header; a widening block is written `Afwijking van`.
