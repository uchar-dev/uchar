# jutterswet-stukopbrengst

The **Onbekend arithmetic-recursion** witness (the public flip-witness for
LADDER-EVOLUTIE §9 Q13, ratified 2026-08-29). It is the one public golden that
exercises clingo's `numeric_ok` Binair-recursion admission
(`backends/clingo/lib/translate.ml`): the `Datatype_onbekend` sentinel is
admitted into arithmetic **only** when the node is provably numeric — an
arithmetic `Binair` whose operands are themselves numeric or such Binairs,
recursively. Before this golden, that arm's only executing witness lived in the
gate-11 parity zone and would have left the repository with it; this golden
makes the arm publicly reachable, verifier-clean, ahead of any public flip.

## The sentinel here

`opbrengst := basisbedrag + aantal stukken × stukprijs`. The inner product
`aantal stukken (unitless) × stukprijs (EUR)` is the frontend's genuine
"samengestelde eenheid" degradation: `Typering.reken` types a unitless
**attribute** × a unit-carrying operand as `Onbekend("samengestelde eenheid")`
because the Eenheid model cannot NAME the composite — it is a typing INABILITY,
not a non-numeric value. The outer `+` then carries `Onbekend("operand")`
(an Onbekend operand poisons the sum's type). Both nodes are provably numeric:
clingo's `numeric_ok` recurses through them to the Numeriek leaves, and the
verifier's WF-034 twin (`onbekend_numeriek`, spec/verifier) admits it too —
strictly a safe subset of clingo's admission (a None-typed operand is
numeric-ok for clingo, not admitted by the verifier), so nothing
verifier-clean can fault at execution and the document is well-formed.

`eff_unit` reads the OPERAND units (unitless × EUR adopts EUR), independent of
the declared `Onbekend` type, so the product lands in the EUR `opbrengst`
target without a unit fault.

## Hand-computed absolutes

| Vondst | basisbedrag | aantal stukken | stukprijs | opbrengst          |
|--------|-------------|----------------|-----------|--------------------|
| V1     | 40          | 3              | 50        | 40 + 3·50 = **190** |
| V2     | 12          | 5              | 20        | 12 + 5·20 = **112** |

ref = clingo over every expectation (gate 5 ≡ gate 6); Drools plays it too, so
the full square agrees (ref ≡ clingo ≡ Drools). NOOP-REFUTING: `opbrengst` is
computed, absent from the raw input, so a do-nothing run mismatches it.

Retired together with the `numeric_ok` arm and the sentinel itself at the v1
WF-003 fail-fast promotion (IR-SPEC ch. 2.4), which removes `Onbekend` entirely.
