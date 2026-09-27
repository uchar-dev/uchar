# wf117-twee-benamingen-een-vocabulaire

Violates WF-117 (b) (`spec/IR-SPEC.md` ch. 2.12): at most one designation
per vocabulary on one declaration.

    WF-117 FAIL /model/objectTypes/0/designations/1: a second designation in
    the vocabulary 'handboek' on the object type 'Jutter': 'strandjutter'
    and 'jutter op het strand'

The document is `corpus/golden/jutterswet-vocabulaires` with a second
`handboek` designation, `jutter op het strand`, on the object type `Jutter`:
the way from the declaration to its name in `handboek` would have two
answers. The verifier reports it in one line, the arm `expect.txt` pins.
