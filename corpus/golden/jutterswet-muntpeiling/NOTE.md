# jutterswet-muntpeiling — a count whose element word names a POPULATION

Two counts over one relation, written two ways, pinned to the same numbers.

`het aantal gevonden munten van de Jutter` names the ROLE the fact type
declares, so the frontend binds it and the wire carries `bronRol`.
`het aantal Munten van de Jutter` names the OBJECT TYPE's own declared plural.
No role is called that, so nothing binds and the wire carries
`elementObjecttype: Munt` with **no** `bronRol` — the bindingless instance
fold of `IR-SPEC.md` ch. 4.3, whose source every consumer derives from the
type pair (`Jutter`/`Munt` is uniquely connected, which is exactly what WF-048
demands before the document is well-formed).

Both halves are new coverage, and they are new for different reasons.

*The population name.* Until 2026-09-16 the count's element word was looked up
as the SINGULAR of an object type or as a role name, so a written plural that
denotes a declared type resolved to nothing: the fold went out with
`elementObjecttype: null` and the document was WF-034-ill-formed
(`'Munten' is neither an attribute nor a characteristic of 'Jutter'`). The
population-plural lookup closed that; this golden is the emitted witness.

*The bindingless count.* `corpus/golden/jutterswet-kaveltelling` already shows
a count going out bound-less, but for the opposite reason — there the sentence
names a ROLE whose plural the model never declared. Here the sentence names no
role at all, and the two spellings sit side by side in one document so the
derivation can be checked against the binding instead of only against itself:
whatever the type pair derives must be the relation the bound count names, or
the two attributes disagree.

The ambiguous flank is refused rather than guessed: with a second fact type
between the same pair, nothing binds and nothing derives, and the document
fails WF-034 and WF-048 at once (`corpus/negatief/wf048-telling-meerduidig`).

## Case derivation (hand-computed, absolute)

The count ranges over the instances the relation reaches whose type is
`Munt`-or-below (ch. 2.2). Every jutter here links only `Munt` instances, so
reach and population coincide and the two rules must agree per jutter.

| jutter | `gevonden munten` | `muntaantal` (bindingless) | `gecontroleerde muntaantal` (bound) |
|---|---|---|---|
| J1 | M1, M2, M3 | **3** | **3** |
| J2 | none | **0** | **0** |
| J3 | M4 | **1** | **1** |

J2 is the empty-relation flank: a count over no members is 0, not empty, so
the bindingless fold has to derive its relation even where that relation
delivers nothing — a consumer that derived no role at all would leave the
attribute empty instead of writing 0.
