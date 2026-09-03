Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `AttributeReference`
arm, DECIMALS facet: `Jutter.gewicht` is declared with two decimals and
the reference that reads it is typed with none. No unit is involved on
either side, so the unit facet is silent and the decimals facet carries
the refusal alone.

    WF-091 FAIL /resolvedRegels/0/waarde/type: decimals mismatch:
    attribute reference 'gewicht' is typed 0 decimals, but the declaration
    it reads from is 2 decimals

Why it has to exist: precision is the other half of what WF-078 named as
deliberately uncrossed, and it is the facet whose absence is quietest — a
consumer that rounds to the annotated precision loses cents on every read
while ref and the ASP backend, which compute from the declaration, keep
them. Note the scope: this is the READ side. The assignment precision gate
of WF-023 (a computed VALUE against a target's declared precision) needs
expression-shape analysis and stays OPEN; it is a different question.

Falsifiable: drop the decimals facet from the engine and this case alone
turns clean.
