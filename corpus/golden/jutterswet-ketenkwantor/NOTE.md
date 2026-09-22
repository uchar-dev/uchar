# jutterswet-ketenkwantor

A COUNTING quantifier over a multi-step range: the quantifier fold
(`IR-SPEC.md` ch. 4.5, WF-108 (c)) with a two-step `bronPad`, read as the
FLAT UNION of `RUNTIME-SEMANTICS.md` ch. 3.4b (ch. 3.9a shares the range of
ch. 3.4a/3.4b). A Jutter voert Boten, a Boot bergt Vondsten, and one Vondst
may be geborgen by more than one Boot (the `berging` relation is
many-to-many, which is what lets an element be shared).

```
Een Jutter is vondstrijk indien het gewicht van ten minste 4 van de
geborgen vondsten van alle gevoerde boten van de Jutter groter is dan 1,00.
Een Jutter is vondstarm indien het gewicht van ten hoogste 3 van de
geborgen vondsten van alle gevoerde boten van de Jutter groter is dan 1,00.
```

Both rules read ONE count, from either side: `vondstarm` is the
complement of `vondstrijk` on every cell, so each case pins the count
twice. The second rule is also what makes every case refute a
do-nothing executor (gate 7): a case whose only verdict is an absent
kenmerk is satisfied by the raw input.

Under `AANTAL` a count over this range has no wire form (WF-096 keeps the
instance count off a set source, WF-102 admits a range only on one; see
`wf096-aantal-setbron-bronpad`). The quantifier fold is a different node
under its own rule, and this document is the witness that it counts over a
multi-step range, flat.

## Producer route, measured 2026-09-22

No `bron.rgs`. Each sentence above, fed through the RegelSpraak frontend
(`make -C frontends/regelspraak ir FILE=…`), is refused with the WF-096
error "setlezing buiten een vouwbron": the frontend's quantifier lowering
declines a written range that resolved to a multi-step path rather than
bind its first step (the `bereikPad` guard in `RuleChecker.kwantorvouw`),
and the same holds with "van de gevoerde boten" instead of "van alle".
The document is therefore hand-written: the one-step sentences "… van de
geborgen vondsten van de Boot …" were emitted by the frontend and their
`Kwantorvouw`s carried over with the subject moved to the Jutter and
`bronRol` replaced by the two-step `bronPad` (bemanning/gevoerde boot,
berging/geborgen vondst).

## The cells, computed by hand before any executor ran

Every Vondst weighs more than 1,00 unless stated, so the matrix holds of
every reached element except where the table says otherwise. `distinct` is
the number of elements in the flat union that satisfy the matrix — the
pinned reading. `voorkomens` counts join tuples (boot, vondst) instead: the
reading that folds per intermediate and adds up, which this document must
tell apart.

| case | J1's boten → vondsten | flat union | distinct | voorkomens | vondstrijk (≥ 4) | vondstarm (≤ 3) |
|---|---|---|---|---|---|---|
| `case1-gedeelde-vondst-onder-de-drempel` | B1→{V1,V2}, B2→{V2,V3} | {V1,V2,V3} | 3 | 4 | **afwezig** | **aanwezig** |
| `case2-gedeelde-vondst-op-de-drempel` | B1→{V1,V2}, B2→{V2,V3,V4} | {V1,V2,V3,V4} | 4 | 5 | **aanwezig** | **afwezig** |
| `case3-toets-beslist-mee` | B1→{V1,V2}, B2→{V2,V3,V4}; V4 weighs 0,50 | {V1,V2,V3,V4} | 3 | 4 | **afwezig** | **aanwezig** |

Case 1 is the separating cell: V2 is reached along B1 and along B2 and
counts once, so the count is 3: the threshold of 4 is not met and the
ceiling of 3 is. An executor that counted occurrences would answer 4 and
turn BOTH verdicts over.

Case 2 is the other side of the threshold with the same shared element:
the flat count reaches exactly 4. Case 3 keeps case 2's graph and makes
the matrix fail on V4 — the count is over the elements that SATISFY the
test, so it drops to 3 (occurrences would still reach 4, again turning both
verdicts over).
