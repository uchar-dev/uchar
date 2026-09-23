# jutterswet-opvolgerquorum

The monotone half of the quantifier-fold stratification rule (`IR-SPEC.md`
ch. 5.2, maintainer decision 2026-09-23). The RANGE of a quantifier fold is a
negative edge under ALLE, GEEN, TEN_HOOGSTE and PRECIES and positive under
TEN_MINSTE; the MATRIX is negative under GEEN, TEN_HOOGSTE and PRECIES and
positive under TEN_MINSTE and ALLE. This document puts both positive parts
on a cycle through a recursive creation, and it is verifier-clean. The
negative parts are pinned by the `corpus/negatief/wf046-kwantorvouw-*`
cases.

One recursive creation and one rule, over Wachters in two relations:
`opvolging` (a voorganger and its opvolgers, which the creation fills) and
`ploeg` (a ploegleider and its ploegleden, filled only by the case input).

- "werf een opvolger" (recursive group "estafette") mints an opvolger with
  rang + 1 while the rang of the Wachter is kleiner dan 5, **ten minste 1**
  opvolger of the Wachter has a rang groter dan 3, and **alle** ploegleden
  of the Wachter have a merk groter dan 1.
- "merk van een wachter": merk := het aantal opvolgers van de Wachter.

The creation reads the `opvolging` range and the opvolgers' rang of its own
product (the TEN_MINSTE fold), and the merk that the rule derives from that
product (the ALLE matrix). Both run through the recursive creation and carry
no negative edge, so WF-045 admits the cycle and WF-046 finds nothing.

## Hand computation

W1 always has rang 1. Every opvolger the creation mints has no opvolgers of
its own, so its TEN_MINSTE fold fails and it mints nothing further.

- **case1** — W1 (rang 1) and W2 (rang 4), no links. Neither has an
  opvolger, so neither TEN_MINSTE fold holds and nothing is minted.
  merk(W1) = 0, merk(W2) = 0. This is the counter-witness that decided the
  rule: a reading that walked the matrix negatively under every quantifier
  refused this document, on which the reference interpreter and the ASP
  backend agree.
- **case2** — W1 with opvolger W2 (rang 4), no ploeg. For W1, 1 < 5, W2's
  rang 4 > 3 so TEN_MINSTE 1 holds, and ALLE over no ploegleden holds, so W1
  mints an opvolger of rang 2. The range of W1's fold grows by that opvolger
  and the fold still holds: under TEN_MINSTE a growing range cannot lose the
  verdict. merk(W1) = 2 (W2 and the minted one), merk(W2) = 0.
- **case3** — as case2, and W1 leads W3 (rang 2), which has no opvolgers.
  merk(W3) = 0, so the ALLE fold over W1's ploegleden fails and W1 mints
  nothing. W3 has no opvolger, so its TEN_MINSTE fails. merk(W1) = 1,
  merk(W2) = 0, merk(W3) = 0.
- **case4** — as case3, and W3 has opvolger W4 (rang 4). At the start
  merk(W3) = 1, which is not groter dan 1, so W1's ALLE fold fails. W3's own
  folds hold (W4's rang 4 > 3; W3 leads nobody), so W3 mints an opvolger of
  rang 3, and merk(W3) becomes 2. Now W1's ALLE fold holds, and W1 mints.
  The matrix only ever turned true: under ALLE the verdict is monotone in
  the matrix. merk(W1) = 2, merk(W2) = 0, merk(W3) = 2, merk(W4) = 0.

Two, two, three and four value rows, each case plus `consistent`: 15
expectations over four cases.

## What this golden pins

Each positive clause has a mutation that refuses this document: the range
read negatively under TEN_MINSTE, the matrix read negatively under
TEN_MINSTE, and the matrix read negatively under ALLE (each measured
2026-09-23 as a gate-3 failure on this document). The reference interpreter
and the ASP backend agree on all four cases, and the ASP program has exactly
one stable model in each. Both Drools branches play all four.

No case lets two Wachters lead each other. On such an input the ASP
backend's encoding of the ALLE fold, a double negation, has a second,
self-supporting model beside the least fixpoint (`IR-SPEC.md` ch. 5.5,
recorded flank). A case like that would pin a backend defect rather than
the rule this golden is about.

No `bron.rgs`: the document is hand-written IR, and the rules illustrate the
form; no Jutterswet article renders them.
