# jutterswet-hoofdberging — an allocation whose target type is a subtype

The allocation counterpart of `corpus/golden/jutterswet-wrakrecht`: the same
chain notion, but on the ch. 3.2 form instead of on rules and defaults. One
allocation carries the two things this golden exists to pin, and neither of
them is expressible without a supertype edge:

1. **The target attribute is INHERITED.** `targetType` is `Hoofdberger`;
   `targetAttribute` is `bergingsaandeel`, which `Hoofdberger` does not declare
   — `Berger`, its supertype, does. Resolution walks the chain upward
   (`IR-SPEC.md` ch. 2.2), so the allocation's write target resolves at the
   supertype and the share lands on a subtype instance.

2. **The connecting relation hangs on the SUPERTYPE.** `bergingsdeelname`
   relates `Berging` to `Berger`, so the bound role reaches every berger of
   the berging — including ones that are not hoofdbergers. The recipient set
   is nevertheless every **targetType** instance the relation reaches
   (`RUNTIME-SEMANTICS.md` ch. 4.1): the role supplies the relation, the
   target type supplies the population. `G1`, a plain `Berger`, is linked and
   receives nothing.

Before the supertype edge existed the two sets always coincided — WF-047
demanded one and the same name on the bound role's filler and on `targetType`
— so no golden could tell them apart. Since ch. 2.6 widened "agree" to
⊑-compatibility they can differ, and this golden is the witness that all
three executors keep them apart the same way.

`Hoofdberger` also declares one own attribute, `rang`, which the case
supplies and the expectations echo: the type is a real subtype with its own
member, not a renaming, and the Drools bean that `extends Berger` carries
both members.

A third thing rides along, on the same two declarations and at no extra
cost. The scoped uniqueness *rang per berging* has `Hoofdberger` for its
subject and binds its scope hop explicitly (`scope` on `bergingsdeelname`,
role `berging`), so the hop's near side is filled by `Berger` while the
subject is `Hoofdberger`. The BOUND spelling of a hop used to compare those
two names literally and refuse; its bindingless twin resolved through ⊑ and
passed — the same document admitted or refused depending on which spelling
it used, with ch. 2.6 recommending the refused one. This golden is the
standing witness that the bound branch joins through ⊑ (WF-054; the same
correction landed on WF-053's two hops, WF-102's `sourcePath` step and
WF-028's creation chain). `G1` is not in the uniqueness population either,
for the same extensional reason it receives no share.

## Case derivation (hand-computed, absolute)

`case1-gemengd`: one `Berging` with `opbrengst = 1001`, linked to `H1` and
`H2` (both `Hoofdberger`) and to `G1` (a plain `Berger`).

Equal shares over the recipient set, floored at 0 decimals, with the
remainder written back on the source:

| recipient | type | in the recipient set? | share |
|---|---|---|---|
| H1 | Hoofdberger | yes | ⌊1001/2⌋ = **500** |
| H2 | Hoofdberger | yes | ⌊1001/2⌋ = **500** |
| G1 | Berger | **no** — not a `Hoofdberger` | **leeg** (never 0) |

`onverdeelde rest = 1001 − (500 + 500) = **1**`. Had the recipient set been
the role's links unfiltered, the three numbers would read 333 / 333 / 333
with a remainder of 2 — the divergence this golden pins shut.
