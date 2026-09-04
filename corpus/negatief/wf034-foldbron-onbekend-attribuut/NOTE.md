Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **fold source** outside the
row-1 wart naming an attribute *no declaration carries*, admitted until now by
the ch. 4.3 type-pair fallback applied beyond its scoping.

    WF-034 FAIL /resolvedRegels/1/waarde/argumenten/0: 'bestaatniet' is not a
    declared attribute of 'Jutter': outside the v0 instance-fold source (the
    unknown-typed row-1 wart) a fold-source argument is an ordinary attribute
    reference — the ch. 4.3 role-name exception and its type-pair fallback do
    not reach here

WF-034 grants exactly one scoped exception: "the fold-source role name
(ch. 4.3)". Chapter 4.3 scopes it to the **v0 instance-fold source** — the
row-1 wart, which writes a *role* name in its argument's `attribuut` slot and
carries the unknown sentinel on its own `type` by construction. That sentinel
is how the wart is spelled on the wire, and it is how **both executors** key
their role-naming branch: the reference interpreter's contributor match and
the ASP backend's count arm each require it. The verifier did not: it offered
the exception — the declared-role route and the type-pair fallback behind it —
to *every* fold-source argument, including one reading a genuine attribute
value. This clause narrows the exception back to its scoping.

The document. `corpus/golden/anchor/jutterswet-maatschap` sums the
`vondstwaarde` of a Jutter's fellow partners; the fold's only fact type is a
**reciprocal self-relation** over `Jutter`, so the type pair `Jutter`/`Jutter`
derives exactly one connecting role — the fallback always succeeds there, for
any name whatsoever. Repointing the source at `bestaatniet` therefore left the
document verifier-clean. Measured against
`corpus/golden/anchor/jutterswet-maatschap/case1.json`:

    ref    : MISMATCH: J1.maatwaarde = 0, expected 12
             MISMATCH: J2.maatwaarde = 0, expected 30
    clingo : TRANSLATION FAULT: unknown attribute "bestaatniet" on "Jutter"

`ref` does not fault: the partners are found, each is asked for an attribute
it does not have, each answers *leeg*, and the sum of nothing is 0 — a number,
returned confidently, where 12 and 30 are pinned by hand on an **anchor**
golden. The ASP backend refuses the same document outright. Divergence again,
and again on input the verifier called well-formed.

The regression boundary, counted over all four zones before and after: of 89
named fold sources — *named* meaning the argument spells both its `attribuut`
and its `objecttype`, which is what makes it a position this arm can resolve;
count the one source that spells an attribute and no object type (a wave-1
shape, refused there) and the totals read 90 and 58 — 32 carry the wart, 24
resolving through a declared role name, 5 through the type pair, 3 already
ill-formed under this rule's older arms, and 57 do not; of those 57, 54 already
resolve as a declared attribute
of their object type and one names a characteristic (WF-080's business,
untouched), leaving only this witness and its sibling. The counting folds
that lean on the fallback keep both routes: `jutterswet-kaveltelling`'s
synthesized plural `duinkavels` over a role with no declared plural (the
corpus's one golden fallback user) and the declared-plural route of
`jutterswet-aanmeldertelling` and `jutterswet-vondsttelling` are warts, and
all three replay unchanged in `ref` and clingo after the narrowing
(5, 3 and 7 expectations respectively, and the untouched
`anchor/jutterswet-maatschap` 7).

*Measured breadth of the shape itself:* zero outside this witness and its
sibling `wf034-foldbron-rolnaam-waardevouw`. Over all 486 documents of golden
+ rood + negatief + parity the narrowing changes exactly those two verdicts
and nothing else.

A hand-broken variant of `corpus/golden/anchor/jutterswet-maatschap/ir.json` —
one name repointed, a one-line diff; schema-valid, since `AttribuutRef.attribuut`
is `["string","null"]` on the v0 wire.

Falsifiable, branch-exact: in `aggregation_source_check` — the fold source's
own arm, which replaces the generic `walk_expr` for this position — make the
type-pair fallback reachable outside the wart again (guard the new
"ordinary attribute reference" arm with `not (unique connecting role)` as well
as `not wart`). This case, alone in all four zones, turns clean; its role-name
sibling stays rejected, because the pair `Vondst`/`Vondst` it lives on derives
no connecting role at all. Measured: 486 documents compared, 1 report changes,
1 verdict. (Read the mutant more coarsely — delete the whole "ordinary
attribute reference" arm instead of guarding it — and the sibling's *message*
changes to the fallback's while its verdict holds: 2 reports, still the same
1 verdict, still this document.)
