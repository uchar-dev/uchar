Violates WF-113 (a) (`spec/IR-SPEC.md` ch. 2): a `warning` whose `message` is
three spaces.

    WF-113 FAIL /diagnostics/0/message: diagnostic with an empty or blank message: an entry that says nothing accounts for nothing

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
the golden unchanged, plus one entry with severity `warning`, line 5, and
`"message": "   "`. A blank message counts as empty: it reports nothing, and
under the literal containment of WF-113 (b) it could only account for a blank
marker, which (b) refuses. A check that compares the message with the empty
string admits this entry, so it falls independently of `wf113-empty-message`.
