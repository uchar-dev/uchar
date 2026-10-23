# jutterswet-registervoorrang — the form subject in a filter and in a quantifier test

A Register holds Vergunningen through the fact type `registerinschrijving`
(roles `register`, filled by Register, to-one, and `vergunning`, plural
`vergunningen`, filled by Vergunning, to-many). A Vergunning has an
`ingangsdag`, a `tarief` and the derived `hoogste latere tarief`, and the
characteristic `voorrang`. Both rules range over the Vergunningen of the
Vergunning's register (`sourcePath [registerinschrijving/register,
registerinschrijving/vergunning]`), a range that reaches the Vergunning
itself: the element of the fold has the type of the rule's subject, and
`zijn ingangsdag` in the filter is the member's.

| rule | form |
|---|---|
| hoogste latere tarief | de maximale waarde van het tarief van alle vergunningen van het register van de Vergunning die aan alle volgende voorwaarden voldoen: hij is niet de Vergunning zelf; zijn ingangsdag is later dan de ingangsdag van de Vergunning zelf |
| voorrang | een Vergunning heeft de voorrang indien de ingangsdag van alle vergunningen van het register van de Vergunning die niet de Vergunning zelf zijn later is dan de ingangsdag van de Vergunning zelf |

"de Vergunning zelf" is the form subject (IR-SPEC ch. 4.1a, WF-122;
RUNTIME-SEMANTICS ch. 3.4a): an `IdentityCheck` with `negated: true` (the
exclusion) and an `AttributeReference` with `anchor: "form"` (the
comparison), in the filter of the maximum and in the filter and the test of
the quantifier `all`. This is the positive witness of WF-122.

The RegelSpraak source is `source.rgs`, with the word `zelf`
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "The rule subject in a
subselection — zelf"), and the frontend writes the `ir.json` from it
byte-identically (gate 10). Until the frontend of the card
`subselection-reads-subject` the `ir.json` was written by hand; the emitted
one differs from it in five source snippets only: the snippet of the fold
argument and of the maximum carry the whole written source with the
subselection, the identity check of the quantifier carries its predicate
without the `die` (as every predicate of a subselection), and the left
operand and the test of the quantifier carry the written quantified operand.

## Hand computation

The cases are the worked example of RUNTIME-SEMANTICS ch. 3.4a, row by row
(`WV-3.4a-anchor-*`, also ref's own tests).

`case1-drie-vergunningen.json` (three): R1 holds V1 (01-01-2026, 10), V2
(01-03-2026, 20), V3 (01-06-2026, 15). V1 folds V2 and V3: **20**, and both
start later: **voorrang**. V2 folds V3: **15**; V1 starts earlier: no
voorrang. V3 has no later member: **leeg**, no voorrang.

`case2-een-vergunning.json` (alone): R1 holds V4 alone. The identity check
drops V4 from its own range: the maximum has no contributor (**leeg**) and
`all` over none is **waar**.

`case3-twee-registers.json` (two registers): R1 holds V1 (01-01-2026, 10)
and V2 (01-03-2026, 20), R2 holds V3 (01-02-2026, 30). V3 starts later than
V1 but is no member of V1's range: V1 **20**, voorrang; V2 leeg, no
voorrang; V3, alone in R2, **leeg** and **voorrang**.

`case4-lege-ingangsdag-onderwerp.json` (empty subject): V1 (leeg, 10), V2
(01-03-2026, 20), V3 (01-06-2026, 15). V1's ingangsdag is leeg, so every
comparison with it fails: V1 **leeg**, no voorrang (V2 and V3 are
contributors whose test fails). V1 drops out of V2's maximum (its ingangsdag
is leeg): V2 folds V3, **15**, and fails its test on V1: no voorrang. V3
**leeg**, no voorrang.

`case5-lege-ingangsdag-lid.json` (empty member): V1 (01-01-2026, 10), V2
(leeg, 20), V3 (01-03-2026, 15). V2 drops out of V1's maximum: **15**; in
V1's quantifier V2 passes the filter and fails the test: no voorrang. V2
leeg, V3 leeg, no voorrang.

`case6-zelfde-ingangsdag.json` (same day): V1 (01-01-2026, 10), V2
(01-01-2026, 20), V3 (01-02-2026, 5). A strict `later` counts no member of
the same day: V1 and V2 each fold V3 alone, **5** (with the same day
admitted, 20 and 10), and fail their test on each other: no voorrang. V3
leeg, no voorrang.

`case7-vroeger-in-ander-register.json` (earlier elsewhere): R1 holds V1
(01-03-2026, 10) and V2 (01-02-2026, 20), R2 holds V3 (01-12-2025, 30). V1
has V2 as its one member, which starts earlier: **leeg**, no voorrang. V2
folds V1, which starts later: **10**, **voorrang**. V3, alone in R2,
**leeg**, **voorrang**. An executor that compared a member with ANY
Vergunning instead of with the form subject would give V1 the voorrang, since
V2 starts later than V3: the ASP backend's helper for the matrix of an `all`
must carry the form subject in its head, and this case is the one that tells.

Every case pins a value or a voorrang the input does not carry, so a run
that evaluates nothing passes none of them (gate 7); cases 4 and 6 have
their third Vergunning for that reason.
