# wf116-language-with-whitespace

Violates WF-116 (d) (`spec/IR-SPEC.md` ch. 2.12): a language code is not
empty and carries no whitespace.

    WF-116 FAIL /vocabularies/0/language: language 'nl NL' of vocabulary
    'handboek' is not a code: it is empty or carries whitespace

The document is `corpus/golden/jutterswet-vocabulaires` with the language of
`handboek` written `nl NL`; what a code denotes is not checked, only this
form. The verifier reports it in one line, the arm `expect.txt` pins.
