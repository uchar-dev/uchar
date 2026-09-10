Violates WF-099 (`spec/IR-SPEC.md` ch. 4.6) at both **clamp bound**
positions: a clamp over a `EUR` base carrying a `10` minimum and a `100`
maximum annotated `kg`. Only the two bounds carry the foreign unit — the
base, the clamp's own type and the rule target are `EUR`, so the document
is well-formed everywhere else and the defect is the unit pair alone.

    WF-099 FAIL /resolvedRegels/0/waarde/minimum: clamp minimum and base
    have incompatible units
    WF-099 FAIL /resolvedRegels/0/waarde/maximum: clamp maximum and base
    have incompatible units

Why it has to exist. A clamp restates each bound into the base's unit
before it compares magnitudes, so an unconvertible bound has no meaning
there — and every executor says so, while the verifier did not. Measured
on this exact document before the rule landed (2026-09-10): verifier
**PASS**, `ref` `EVALUATION FAULT: incompatible units: EUR vs kg`, the ASP
backend `TRANSLATION FAULT: non-identical units under a Begrensd bound are
not translated`. The three rules that come nearest all stop short: WF-035
crosses the operands of an ADDITIVE node and a clamp is not one (its own
clamp arm reaches the bounds for numeric-ness and for presence, never for
units), WF-036 crosses a COMPARISON's operands, and WF-023 crosses the
value written against its target — which here is `EUR` against `EUR`, and
agrees. WF-091 cannot reach it either: the bound is a `Literal`, one of the
three ch. 2.10 positions where the annotation IS the source, so there is no
declaration to cross it against.

Falsifiable: move both bounds to `t` — a different unit but convertible to
the base's, once the base is `kg` — and this case turns CLEAN, which is the
line the rule draws (convertible, not identical). Give the bounds no unit
at all and it turns clean too, by the unitless-literal adoption WF-035
carries. Drop either bound and exactly its own report goes, so the two
positions fail independently.
