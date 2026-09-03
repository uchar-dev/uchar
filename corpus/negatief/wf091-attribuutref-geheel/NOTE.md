Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `AttributeReference`
arm, WHOLENESS facet: `Jutter.gewicht` is declared fractional (`geheel:
false`, zero decimals — one of WF-007's three precision forms) and the
reference that reads it is typed whole. Decimals agree, so the decimals
facet is silent and wholeness carries the refusal alone.

    WF-091 FAIL /resolvedRegels/0/waarde/type: wholeness mismatch:
    attribute reference 'gewicht' is typed whole, but the declaration it
    reads from is fractional

Why it has to exist beside `wf091-attribuutref-decimalen`: WF-007 admits
three precision forms, and two of them share a decimal count — whole with
zero decimals and fractional with zero decimals. A rule that crossed only
the decimal count would call those two identical, which is exactly the
weakening this case refutes.

Falsifiable: drop the wholeness facet from the engine and this case alone
turns clean; `wf091-attribuutref-decimalen` stays red.
