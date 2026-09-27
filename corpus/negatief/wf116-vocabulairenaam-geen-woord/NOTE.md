# wf116-vocabulairenaam-geen-woord

Violates WF-116 (a) (`spec/IR-SPEC.md` ch. 2.12): a vocabulary's name is a
word.

    WF-116 FAIL /vocabularies/0/name: vocabulary name 'hand boek' is not a
    word: it is empty, or carries whitespace, a parenthesis or a colon

The document is `corpus/golden/jutterswet-vocabulaires` with the vocabulary
`handboek` renamed `hand boek`, in its declaration and in every designation
that names it, so that the name's form is the one defect: whitespace in a
name the dialect writes as `(<vocabulary>:`. The verifier reports it in one
line, the arm `expect.txt` pins.
