# jutterswet-afleiden-uit-leeg — a document that infers nothing from an empty value

The RegelSpraak line `Afleiden uit leeg: nee` (grammar/DIVERGENCES.md, "Afleiden uit leeg") on a small Jutterswet
model. The frontend writes the choice as `inferFromEmpty: false` on the root of `ir.json`, directly after `format`, and
gate 10 re-emits `ir.json` from `source.rgs` byte for byte. The document is read as RUNTIME-SEMANTICS ch. 1.5 lays down:
a member is filled, settled leeg or open.

A Vondst with a taxatie, a gewicht, a vindloon, a bewakingstoeslag and a meldloon; the characteristics kostbaar and
ongetaxeerd, which rules attribute, and gemeld, which only the case gives. Five rules:

- `kostbaarheid`: kostbaar indien zijn taxatie groter is dan 1000 (a comparison);
- `vindloon`: zijn gewicht plus 10 (a calculation);
- `bewakingstoeslag`: 5 indien hij niet kostbaar is (a negated characteristic check);
- `ongetaxeerd`: ongetaxeerd indien zijn taxatie leeg is (the leeg test);
- `meldloon`: 3 indien hij gemeld is (a check of an input characteristic).

The expectations, computed by hand. "absent" in the input is `characteristicsAbsent`; "settled leeg" is an
`attributeEmpty` expectation, "stands not" a `characteristicAbsent`, "open" an `attributeOpen` or `characteristicOpen`.

| case | input | kostbaar | vindloon | bewakingstoeslag | ongetaxeerd | gemeld | meldloon |
|---|---|---|---|---|---|---|---|
| case1, V1 | taxatie 2000, gewicht 5, gemeld | holds | 15 | settled leeg | stands not | holds | 3 |
| case1, V2 | taxatie 50, gewicht 0, gemeld absent | stands not | 10 | 5 | stands not | stands not | settled leeg |
| case2 | gewicht 5, gemeld | open | 15 | open | holds | holds | 3 |
| case3 | taxatie 50, gemeld absent | stands not | open | 5 | stands not | stands not | settled leeg |
| case4 | nothing | open | open | open | holds | open | open |

Where everything is given (case1) every member stands, as in the reading of ch. 1.2–1.4. Without a taxatie the
comparison is undetermined, so kostbaar is open, and so is the bewakingstoeslag, whose "niet kostbaar" is undetermined
as well; ongetaxeerd holds, since a leeg test over an open value is true for the form that writes it (a closure). With
kostbaar holding, "niet kostbaar" stands false and the bewakingstoeslag is settled leeg. Without a gewicht the vindloon,
which reads it, is open. A gemeld that the case states absent stands not to hold, so the meldloon is settled leeg; a
gemeld the case names nowhere is open, and so is the meldloon. No case deposits a §9.5 fact. In the reading of ch.
1.2–1.4 a Vondst without a taxatie is not kostbaar and gets the bewakingstoeslag 5, one without a gewicht has the
vindloon 10, and one that does not name gemeld has no meldloon.

The reference interpreter plays the four cases (gate 5). The ASP backend refuses the document with CLINGO-049 (gate 6),
both Drools branches with DROOLS-370 (gate 12), and the readings at the fixpoint (`runner --trace`) are refused with
REF-014, `form` `reading` (gate 17); each gate lists the golden by name. The red case
`corpus/red/jutterswet-kenmerk-wel-en-niet` carries the same document.
