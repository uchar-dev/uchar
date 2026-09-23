# jutterswet-bergersdeling — the source twin of `jutterswet-hoofdberging`

Same model, same case, same three hand-computed numbers as
`corpus/golden/jutterswet-hoofdberging`. The one thing that is new is that
this document comes out of a `bron.rgs` instead of being written by hand.

`jutterswet-hoofdberging` pins an allocation whose recipient set is narrower
than the relation that reaches it: `bergingsdeelname` relates `Berging` to
`Berger`, the allocation targets `Hoofdberger`, and the plain `Berger` linked
beside the hoofdbergers receives nothing. That golden carries a note saying it
has no source twin, and why: the frontend resolved an onderwerp naming a
population only through the declared SINGULAR of an object type or through a
ROLE of that name. A population named by a SUBTYPE has neither by
construction — the relation is declared once, on the ancestor, so no role is
named after the subtype — and "alle hoofdbergers" therefore resolved to
nothing at all. The allocation went out with `doelType: null` and
`doelAttribuut` stuck on the written plural, which `WF-034` refuses:

```
WF-034 FAIL /rules/verdelingen/0/doelType: allocation without a target object type:
it resolves against no declaration
```

Since 2026-09-16 the written PLURAL of a declared object type resolves at the
positions that name a population, so `alle hoofdbergers` binds `Hoofdberger`
and the emitter writes the connecting role that reaches it — `berger`, whose
filler is the ANCESTOR. That is the ch. 3.2 reading the allocation round put
in the spec: the bound role's filler is ⊑-compatible with `targetType`, not
equal to it; the role supplies the relation and the target type supplies the
population.

`corpus/golden/jutterswet-bergingsdeling` is a source twin too, and a
different one: there the fact type hangs on `Hoofdberger` itself, so the role
plural `hoofdbergers` bound the role and the old resolution reached the type
through it. The two differ in exactly the declaration this golden exists for.

## Case derivation (hand-computed, absolute)

The case is `jutterswet-hoofdberging`'s `case1-gemengd` unchanged, so the
derivation is that golden's and is not restated here: one `Berging` with
`opbrengst = 1001` linked to `H1`, `H2` (both `Hoofdberger`) and `G1` (a plain
`Berger`); equal shares floored at 0 decimals give **500** each, `G1` gets
**leeg** because it is not in the recipient set, and `onverdeelde rest` is
**1**. Reusing the arithmetic rather than inventing a second sum is
deliberate: the two goldens must answer identically, because the only
difference between them is which hand wrote the IR.
