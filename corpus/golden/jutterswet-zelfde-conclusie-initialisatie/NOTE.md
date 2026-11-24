# jutterswet-zelfde-conclusie-initialisatie — an initialisation default below an open default with its value

A stack whose lowest default is its initialisation, and a rule beside an initialisation, in a document with
`Afleiden uit leeg: nee` (`inferFromEmpty: false`), read as RUNTIME-SEMANTICS ch. 1.5 lays down ("The initialisation of
a stack", "An initialisation", "An open default with a constant"). Own Jutterswet-flavoured casus, rights-free.

- `strandloon`: one stack, "op 5 indien hij betwist is" above the initialisation default "op 5";
- `meldloon`: no stack: the rule "op 5 indien hij betwist is" and the initialisation rule "op 5".

`betwist` is an input characteristic, not declared complete. Both members are derived, so a value the case gives them
is no open input.

`ir.json` is written by hand. The frontend writes an initialisation default only under the opgave marker (dialect §7),
whose highest default reads the opgave and so is never open with a constant; the document was emitted from a source
with the base "op 5" in place of the initialisation default, which was then made the initialisation default (the
justification removed, `initialisation: true`), and the stack renamed `strandloon`. The case files are written by hand.

The reading. strandloon: where betwist is open the highest default is open with 5; the initialisation default, read
provisionally, decides 5 where the case leaves the strandloon empty: the strandloon is 5. Where the case gives it a
value, the input stands above the initialisation, which gives way: the stack is open with 5, and on an attribute that
leaves the target open — also where the case value is 5 (case7), since the case value is no default of the stack.
meldloon: beside a rule the initialisation is no default of a stack (ch. 1.5, "An initialisation"): where the rule
leaves the meldloon open the initialisation is not at its turn, and the meldloon is open, also where the rule can decide
only the initialisation's 5.

## Cases (hand-computed, absolute)

| case | strandloon in the case | betwist | strandloon | meldloon |
|---|---|---|---|---|
| case1-betwist-open | — | open | 5 | open |
| case2-betwist-waar | — | holds | 5 | 5 |
| case3-betwist-niet | — | not | 5 | 5 |
| case4-invoer-betwist-open | 7 | open | open | open |
| case5-invoer-betwist-waar | 7 | holds | 5 | 5 |
| case6-invoer-betwist-niet | 7 | not | 7 | 5 |
| case7-zelfde-invoer-betwist-open | 5 | open | open | open |

case1: the strandloon is 5 by the initialisation default below the open default; the old reading, which stopped at the
first open default, left it open. case2: the highest default decides 5; case3: it gives way and the initialisation
decides 5. case5: the highest default decides 5 over the input 7; case6: it gives way, the initialisation gives way to
the input, and the strandloon keeps 7.

No case deposits a §9.5 fact. Gate 5 holds every case against the run with each given input left out (monotonicity):
case2 and case3 without betwist are case1; case5 and case6 without betwist are case4.
