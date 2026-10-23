# anchor-forms — the anchored forms beside the identity check, one rule each

The model of `jutterswet-registervoorrang`: a Register holds Vergunningen
through the fact type `registerinschrijving` (roles `register`, to-one, and
`vergunning`, plural `vergunningen`, to-many). A Vergunning has an
`ingangsdag`, a `tarief`, a `teller`, a `korting` and the characteristic
`geschorst`. Every rule on the Vergunning ranges over the Vergunningen of its
register (`sourcePath [registerinschrijving/register,
registerinschrijving/vergunning]`), a range that reaches the Vergunning
itself; "de Vergunning zelf" is the form subject (IR-SPEC ch. 4.1a, WF-122;
RUNTIME-SEMANTICS ch. 3.4a).

Written in fix round A of the card `subselection-reads-subject` (2026-10-23)
for the forms review A found without a positive witness, and as the corpus
witness of the clingo corrections of that round.

| rule | form | why |
|---|---|---|
| tarief naast geschorste | maximum; filter: not itself, and the Vergunning zelf is geschorst | an anchored `CharacteristicCheck` in a filter |
| registerrust (characteristic) | `all` over the others: the Vergunning zelf is not geschorst | an anchored, negated `CharacteristicCheck` in a test |
| tarief boven registernummer | maximum; filter: tarief above the nummer of the register of the Vergunning zelf | an anchored `AttributeReference` with a `path` |
| tarief naast duur register | maximum; filter: not itself, and at least one Vergunning of the register of the Vergunning zelf has a tarief of at least 20 | an anchored `QuantifierFold` behind the identity check |
| eigen tarief | maximum; filter: hij is de Vergunning zelf | the `IdentityCheck` with `negated: false` |
| deelbaar tarief | maximum; filter: not itself, and tarief / teller above 1 | a division of the member's own attributes behind the identity check |
| gelijk aan later tarief | maximum; filter: not itself, and the tarief equals the highest tarief of the register of the Vergunning zelf later than the Vergunning zelf | an anchored `Aggregation` with an anchored read in its own filter, behind the identity check |
| later tarief zonder korting | as above, behind "zijn korting is leeg" | the same anchored fold behind a leeg test of the member |
| hoogste tarief naast zichzelf (on Register) | maximum over the Register's Vergunningen; filter: die niet het Register zelf zijn | the `IdentityCheck` between types that share no instance: waar at every member |
| hoogste tarief dat zichzelf is (on Register) | the same with `negated: false` | onwaar at every member |
| deelbaar later tarief | condition: hij is niet geschorst; maximum; filter: later than the Vergunning zelf, and tarief / teller above 1 | a rule condition, an anchored comparison and a division of the member's own attributes |
| deelbaar tarief van ongeschorste | condition: hij is niet geschorst; maximum; filter: tarief / teller above 1 | no anchor, in a document that holds anchored rules |

The last five rules on the Vergunning before the Register's are the forms
whose ASP translation review A found refused or unsafe: a helper over a member
whose guard reads the form subject (the identity check, the anchored
comparison, the rule's condition) and an anchored fold behind a prefix that
reads the member. Before fix round A the ASP backend refused both case files
by name ("a helper rule over a member reads the form subject"), and refused
the rule `deelbaar tarief van ongeschorste` even beside one anchored rule
alone; the anchored fold behind a prefix was a grounding error there (review
A, proofs p13, p13b, p3).

No `source.rgs`: the RegelSpraak frontend writes no anchored quantifier fold
(RGS-358, `frontends/regelspraak/grammar/DIVERGENCES.md`, "The rule subject in
a subselection — zelf"), so the document is written by hand. The frontend
route of the anchor is carried by `jutterswet-registervoorrang` and
`fold-filter-subject-two-spellings`.

## Hand computation

`case1-two-registers.json`: R1 (nummer 12) holds V1 (2026-01-01, tarief 10,
teller 2), V2 (2026-03-01, 20, 5, geschorst) and V3 (2026-06-01, 15, 3); R2
(nummer 7) holds V4 (2026-01-01, 5, 1) and V5 (2026-01-01, 7, 1); V8
(2026-01-01, 1, 1) is in no register. No korting.

- tarief naast geschorste: only V2 is geschorst: V2 folds V1 and V3, **15**;
  every other Vergunning leeg.
- registerrust: V2's others fail the test (V2 is geschorst), so V2 has none;
  V1, V3, V4 and V5 have it, and V8, without members, has it (`all` over
  none).
- tarief boven registernummer: in R1 the tarieven above 12 are 20 and 15:
  **20** for V1, V2 and V3; in R2 neither 5 nor 7 is above 7: leeg; V8 leeg.
- tarief naast duur register: R1 holds a tarief of at least 20 (V2), so the
  quantifier holds for V1, V2 and V3: V1 **20**, V2 **15**, V3 **20**; R2
  holds none: V4, V5 leeg; V8 leeg.
- eigen tarief: V1 **10**, V2 **20**, V3 **15**, V4 **5**, V5 **7**; V8 leeg.
- deelbaar tarief: every quotient is above 1 (5, 4, 5, 5, 7): V1 **20**, V2
  **15**, V3 **20**, V4 **7**, V5 **5**; V8 leeg.
- gelijk aan later tarief: the anchored fold gives V1 20 (V2, V3 later), V2
  15 (V3), V3 leeg, V4 and V5 leeg (the same day is not later), V8 leeg: V1
  **20** (V2), V2 **15** (V3), the rest leeg.
- later tarief zonder korting: no korting, so every member passes the leeg
  test; no Vergunning has the tarief of its own later maximum, so the missing
  identity check changes nothing: V1 **20**, V2 **15**, the rest leeg.
- hoogste tarief naast zichzelf: R1 **20**, R2 **7**; hoogste tarief dat
  zichzelf is: R1 and R2 leeg.
- deelbaar later tarief: V2 is geschorst (leeg); V1 folds V2 and V3, **20**;
  no member is later than V3, V4 or V5: leeg; V8 leeg.
- deelbaar tarief van ongeschorste: V2 is geschorst (leeg); V1 and V3 **20**,
  V4 and V5 **7**; V8 leeg.

`case2-other-suspended.json`: R1 (12) holds V1 (2026-01-01, 10, 2,
geschorst), V2 (2026-03-01, 20, 5) and V3 (2026-06-01, 15, 3, korting 1).

- tarief naast geschorste: V1 **20**; V2, V3 leeg. registerrust: V1 none, V2
  and V3 have it.
- tarief boven registernummer: **20** for all three. tarief naast duur
  register: V1 **20**, V2 **15**, V3 **20**. eigen tarief: **10**, **20**,
  **15**. deelbaar tarief: V1 **20**, V2 **15**, V3 **20**.
- gelijk aan later tarief: V1 **20**, V2 **15**, V3 leeg.
- later tarief zonder korting: V1 **20** (V2 has no korting); V2 **leeg**:
  the one member with the tarief 15 of its anchored maximum is V3, which has a
  korting; V3 leeg.
- hoogste tarief naast zichzelf: R1 **20**; hoogste tarief dat zichzelf is:
  leeg.
- deelbaar later tarief: V1 is geschorst (leeg); V2 folds V3, **15**; V3 leeg.
- deelbaar tarief van ongeschorste: V1 leeg; V2 and V3 **20**.

Both cases are consistent. Every case pins a value or a characteristic the
input does not carry (gate 7).
