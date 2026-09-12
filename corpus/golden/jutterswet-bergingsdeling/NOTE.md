# jutterswet-bergingsdeling — an allocation to a subtype, authored in RegelSpraak

The **source twin** of `corpus/golden/jutterswet-hoofdberging`, which pins the
same allocation shape by hand: `doelType` is `Hoofdberger` and
`doelAttribuut` is `bergingsaandeel`, an attribute `Hoofdberger` does not
declare — `Berger`, its supertype, does. Resolution walks the chain upward
(`IR-SPEC.md` ch. 2.2), so the write target resolves at the supertype and the
share lands on a subtype instance.

What this golden adds over its hand-written counterpart is the **route in**.
The hand-written one proves the three executors agree on the IR; this one
proves the dialect frontend can PRODUCE that IR from a sentence. It could not
before: the sentence writes the target in the plural ("de bergingsaandelen van
alle hoofdbergers"), the declaration and its `(mv: …)` sit one hop up, and the
resolver read the subtype's own declaration maps only — so no written form of
an inherited attribute bound and the plural stayed on the wire, where no
declaration carries it and WF-034 refuses the document. An allocation to a
subtype with an inherited target attribute was therefore unreachable from a
`bron.rgs` at all, and `jutterswet-hoofdberging` has no source twin for that
reason.

Two positions, one document, both of them a member of an ancestor read from a
subtype:

1. **The allocation's target attribute is inherited** — the plural in the
   sentence binds `bergingsaandeel` as declared on `Berger`.
2. **The second rule's condition is an inherited kenmerk** — `gemachtigd` is
   declared on `Berger` and read in a predicaat whose subject is a
   `Hoofdberger`. That is the write side of the chain and the read side of it
   in one document.

The connecting relation `hoofdbergingsdeelname` hangs on the **subtype**, so
recipient set and role reach coincide here. The other arrangement — the
relation on the supertype, where they come apart — is what
`jutterswet-hoofdberging` pins; it is not expressible from a source yet
(the onderwerp of a type-ranging position does not resolve through `⊑`), and
that gap is recorded rather than hidden.

## Case derivation (hand-computed, absolute)

`case1-tweedeling`: one `Berging` with `opbrengst = 1001`, related to `H1` and
`H2`, both `Hoofdberger`. `H1` is `gemachtigd`, `H2` is not.

Equal shares over the recipient set, floored at 0 decimals, remainder written
back on the source:

| recipient | share |
|---|---|
| H1 | ⌊1001/2⌋ = **500** |
| H2 | ⌊1001/2⌋ = **500** |

`onverdeelde rest = 1001 − (500 + 500) = **1**` — the same three numbers
`jutterswet-hoofdberging` pins by hand for the same arithmetic, which is why
they are reused here rather than invented.

The conditional rule adds `toeslag = 25` for `H1`. `H2` fails the condition
and its `toeslag` stays **leeg** — never 0, since an unfired assignment writes
nothing.
