# jutterswet-munttelling — an instance fold whose element type is a subtype

The fold counterpart of `corpus/golden/jutterswet-hoofdberging`. That golden
pins the allocation's recipient set; this one pins the same distinction one
construct over, on the instance fold's `elementObjecttype` — the seventh of
the type-ranging positions `IR-SPEC.md` ch. 2.2 quantifies extensionally, and
the last one to get a witness for the direction that EXCLUDES.

`vondstaanmelding` relates `Jutter` to `Vondst`, so the fold's source role
reaches every vondst a jutter aanmeldde. Both folds declare
`elementObjecttype: Munt`, and `Munt ⊑ Vondst` is a strict subtype, so the
population is a proper subset of what the role yields: the role supplies the
REACH, the declared element type supplies the POPULATION. A plain `Vondst`
linked beside the munten is reached and not folded.

Before the supertype edge existed the two always coincided — WF-047 demanded
one and the same name on the bound role's filler and on `elementObjecttype` —
so no golden could tell them apart. Since ch. 2.6 widened "agree" to
⊑-compatibility they can differ. Until 2026-09-16 `ref` and `clingo` folded
the reached set whole and both counted the plain vondst in; the Drools backend
did not, because its exec-model branch matches the element type as a Java
CLASS pattern over beans wired with native `extends`. The two answers were a
live three-way divergence on a verifier-clean document, and `RUNTIME-SEMANTICS`
ch. 2.2 says Drools had it right. This golden is the standing witness that all
three now keep reach and population apart the same way.

`Munt` also declares one own attribute, `jaartal`, which the case supplies and
the expectations echo: the type is a real subtype with its own member, not a
renaming, and the bean that `extends Vondst` carries both. `gewicht`, the
attribute the SOM folds, is declared on `Vondst` and read off `Munt` elements —
the inclusion direction of the same clause, riding along on the same two
declarations.

## Case derivation (hand-computed, absolute)

Three jutters, so the fold's element set is emptied three different ways.

| jutter | linked | of those, `Munt` | `aantal munten` | `muntgewicht` |
|---|---|---|---|---|
| J1 | V1 (Vondst, 3.00), M1 (Munt, 7.00), M2 (Munt, 5.00) | M1, M2 | **2** | 7.00 + 5.00 = **12.00** |
| J2 | V2 (Vondst, 4.00) | none | **0** | **0** (leegwaarde) |
| J3 | M3 (Munt, no gewicht) | M3 | **1** | **0** (leegwaarde) |

J1 is the plain exclusion: one of three links is not a `Munt`, so it neither
counts nor weighs. Unfiltered the two numbers would read 3 and 15.00 — the
divergence this golden pins shut.

J2 is the sharper half of the same point: the role links exactly one instance,
so the reached set is NOT empty, and the fold is empty all the same because the
type filter removes its only member. An executor that answered from the links
alone would report 1 and 4.00 here; the empty-fold `leegwaarde` is what a fold
over no members is defined to give.

J3 separates the two ways a sum reaches zero: its member IS a `Munt` and is
counted, but carries no `gewicht`, so the sum has a member and no value and
falls to the same `leegwaarde` J2 reaches with no member at all. Only the count
tells them apart (1 against 0), which is why both folds are pinned per jutter.
