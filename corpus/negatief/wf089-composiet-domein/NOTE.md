Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) on its one-sided arm at a
**composite** position: a `PLUS` over two `Number` operands is annotated
with the keuzetype domain `Vaarwater` and written to the enumeration
attribute it claims to produce. The derived type of an additive node is a
number (ch. 2.3 / ch. 2.6), which names no keuzetype domain, so the rule
fires exactly as it does when only the declaration names one.

    WF-089 FAIL /rules/resolvedRegels/0/waarde/type: the PLUS composite is typed
    with keuzetype domain 'Vaarwater', but the type derived from its
    operands is number and names no keuzetype domain

Why it has to exist. WF-091's DERIVED arms (2026-09-04, ch. 4.1) carry the
same facet order and the same number-follows-the-facet rule as the
reference arms, so the domain facet at a composite reports under WF-089 —
not under WF-091. Until this case that firing path had no artifact at all:
WF-089's own aggregation arm reaches an element-passing fold and nothing
else, so a `PLUS`, a `MAAL`, a rounding, a clamp, a `SOM`, a `TOTAAL` or a
count could name any domain it liked. The witness is arm-exact and cheap
because the path already existed; what it pins is that the derived half
keeps WF-089's number and WF-089's wording instead of absorbing them.

The soort facet stands aside here by design (`domeinen` is ONE namespace,
so domain-versus-no-domain is the domain facet's own clause), which is why
the message is WF-089's one-sided arm and not a soort mismatch. WF-023
stays quiet for the same reason the reference one-sided case documents: it
compares the target's declared type with the value's SERIALIZED type, and
both spell `Vaarwater`.

Falsifiable, measured: switch the whole derived cross off
(`composite_identity` returns `[]`) and this case turns clean together with
the thirteen WF-091 derived cases and no others; switch the ADDITIVE arm
off and it turns clean with the four `wf091-binair-*` cases and no others;
add `domain` to that arm's skip list and this case — and **only** this case
of all 257 negatiefs — turns clean.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the composite's annotation, a
derived position, keeps its (V5 (a)), so the cross now runs against the type
the operands' declarations derive. The rule still fires on the same arm, and
`expect.txt` pins it (U1).
