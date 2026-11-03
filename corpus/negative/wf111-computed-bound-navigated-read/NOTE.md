# wf111-computed-bound-navigated-read

Violates WF-111 (`spec/IR-SPEC.md` ch. 4.6): a time window's bound is a
day-precise date expression over the subject, the parameters and literals.
Here the window of the time-dependent characteristic `rentedragend` runs
`van de vervaldatum van zijn aangifte plus 1 dg tot en met zijn betaaldatum`:
the bound computes, as a bound may, but it reads an attribute of the object
the role `aangifte` reaches, which the rule does not admit.

Expected verifier report (`expect.txt` pins key and parameters):

    WF-111.bound-form FAIL /rules/rules/0/versions/0/condition/from: the from
    bound of a time window is a navigated read of 'vervaldatum' — …

The document is the RegelSpraak frontend's emission of a clean twin whose
rule `peildatum` computes the same expression as a value, with that
expression moved into the bound by hand: the frontend refuses the bound
itself (RGS-202).
