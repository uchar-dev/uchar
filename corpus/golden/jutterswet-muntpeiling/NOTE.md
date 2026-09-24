# jutterswet-muntpeiling — a count whose element word names a POPULATION

Two counts over one relation, written two ways in the source, pinned to the
same numbers.

`het aantal gevonden munten van de Jutter` names the ROLE the fact type
declares, by its declared plural. `het aantal Munten van de Jutter` names the
OBJECT TYPE's own declared plural; no role is called that. Until L2 only the
first went out with a `bronRol`; the second went out as a bindingless
instance fold of `IR-SPEC.md` ch. 4.3, and every consumer derived its
relation from the type pair (`Jutter`/`Munt` is uniquely connected, which
WF-048 demanded). Since L2 a fold's range is written and never derived
(`spec/proposals/V1-WIREMIGRATIE.md` B12): the wire carries the one binding
`muntvondst` / `gevonden munt` on BOTH counts, and the second count's source
names the bound role by its declared name, `gevonden munt`, while the written
`Munten van de Jutter` stays in `bron`.

*The population name.* Until 2026-09-16 the count's element word was looked up
as the SINGULAR of an object type or as a role name, so a written plural that
denotes a declared type resolved to nothing: the fold went out with
`elementObjecttype: null` and the document was WF-034-ill-formed
(`'Munten' is neither an attribute nor a characteristic of 'Jutter'`). The
population-plural lookup closed that; this golden is the emitted witness.

*What the golden pins since L2.* Until L2 the two spellings sat side by side
so that the derived relation could be checked against the bound one. Since
L2 nothing derives, so that check is gone: both rules read the same written
binding, and the golden pins that a count spelled with the population plural
and a count spelled with the role plural reach one relation and answer the
same number for every jutter.

The ambiguous flank has no wire form since L2. With a second fact type
between the same pair, the population plural names no single relation, so no
binding can be written, and a single-argument count with an
`elementType` but no range is refused at load. Until L2 it loaded and
failed WF-034 and WF-048 at once; its witness `wf048-telling-meerduidig` left
the corpus with WF-048 (`corpus/negatief/README.md`).

## Case derivation (hand-computed, absolute)

The count ranges over the instances the relation reaches whose type is
`Munt`-or-below (ch. 2.2). Every jutter here links only `Munt` instances, so
reach and population coincide and the two rules must agree per jutter.

| jutter | `gevonden munten` | `muntaantal` (population plural) | `gecontroleerde muntaantal` (role plural) |
|---|---|---|---|
| J1 | M1, M2, M3 | **3** | **3** |
| J2 | none | **0** | **0** |
| J3 | M4 | **1** | **1** |

J2 is the empty-relation flank: a count over no members is 0, not empty, so
both counts write 0 where the relation reaches nothing (ch. 1.2e).
