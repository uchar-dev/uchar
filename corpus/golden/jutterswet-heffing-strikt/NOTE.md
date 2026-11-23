# jutterswet-heffing-strikt — one choice in three forms, in a document that infers nothing from leeg

One choice, "the heffing of a Vondst is 0 where its taxatie is gevuld and 25 where it is leeg", written in three forms
under the line `Afleiden uit leeg: nee` (grammar/DIVERGENCES.md, "Afleiden uit leeg"): the frontend writes
`inferFromEmpty: false` on the root of `ir.json`, and the document is read as RUNTIME-SEMANTICS ch. 1.5 lays down. A
member is filled, settled leeg or open. Own Jutterswet-flavoured casus, rights-free.

- the heffing: the rule `heffing`, "op 0 indien zijn taxatie gevuld is", with the initialisation `heffing aanvankelijk`,
  "op 25", below it;
- the stapelheffing: a stack, the base `stapelheffing`, "op 25", with the exception `getaxeerde stapelheffing`, "op 0
  indien zijn taxatie gevuld is";
- the codeheffing: a helper attribute, `heffingscode`, "op 1 indien zijn taxatie leeg is", read by `codeheffing`, "25
  maal zijn heffingscode" (a leeg heffingscode counts as 0 in the product, ch. 1.2).

The taxatie is an input member, not declared complete. Where the case gives it a value, the test on gevuld or leeg is
two-valued, and each form gives 0. Where the case gives it none, the taxatie is open and each test on it is
undetermined: the rule `heffing` leaves the heffing open, and the initialisation below it is not at its turn, since it
does not stand that the rule fills nothing; the exception of the stapelheffing is open, and so is the stack; the
heffingscode is open, and so is the codeheffing that reads it. Whether a form chooses with a rule above an
initialisation, in a stack or through a helper attribute, it does not decide over a value the case may still give. The author closes at the edge: with the
taxatie declared complete (`(volledig gegeven)`), a missing taxatie is settled leeg and each form gives 25; the suite
of the reference interpreter runs this document so (WV-1.5-three-forms).

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand. "settled leeg" is an `attributeEmpty` expectation, "open" an
`attributeOpen` (the scenario's "staat niet vast").

## Cases (hand-computed, absolute)

| case | input | heffing | stapelheffing | heffingscode | codeheffing |
|---|---|---|---|---|---|
| case1-getaxeerd | V1 taxatie 50 | 0 | 0 | settled leeg | 0 |
| case2-taxatie-open | V1 without a taxatie | open | open | open | open |
| case3-twee-vondsten, V1 | taxatie 2000 | 0 | 0 | — | 0 |
| case3-twee-vondsten, V2 | without a taxatie | open | open | — | open |

No case deposits a §9.5 fact. The reference interpreter plays the cases (gate 5); the ASP backend (gate 6) and both
branches of the Drools backend (gate 12) play them as well.
