# jutterswet-schouwploeg — the quantifier fold over a NON-temporal matrix

The sibling of `jutterswet-ploegpremie`. There the matrix is the ch. 3.9
whole-period test over a member's own timeline; here it is an ORDINARY
condition of the member — a scalar comparison and a characteristic check —
with no period qualifier anywhere in the document:

```
Een Schouwploeg is ervaren    indien de dienstjaren van ALLE wachten
                                     van de Schouwploeg groter zijn dan 5.
Een Schouwploeg is onervaren  indien de dienstjaren van GEEN VAN DE wachten
                                     van de Schouwploeg groter zijn dan 5.
Een Schouwploeg is bevoegd    indien ALLE wachten van de Schouwploeg
                                     gediplomeerd zijn.
Een Schouwploeg is sluitend   indien GEEN VAN DE wachten van de Schouwploeg
                                     NIET gediplomeerd is.
Het wachttal van een Schouwploeg moet berekend worden als het aantal
                                     wachten van de Schouwploeg.
```

`IR-SPEC.md` ch. 4.5 (`QuantifierFold`, WF-108); semantics
`RUNTIME-SEMANTICS.md` ch. 3.9a, whose formula is stated over an arbitrary
`test` and whose worked examples until now only ran the temporal one.

## This ir.json was written by hand; its source.rgs arrived later

The document was written against the schema, because at the time no
RegelSpraak source produced these nodes. Measured on the frontend of this
repository, on the sentences at the head of this note, before the cases
were written:

- the comparison form without the §8.2 qualifier — *"indien de
  dienstjaren van alle wachten van de Schouwploeg groter zijn dan 5"* —
  was refused: `setlezing buiten een vouwbron … hier wordt één waarde
  verwacht (WF-096, IR-SPEC ch. 4.3)`, the message until L2. Since L2 the
  same refusal cites WF-053: `… hier wordt één waarde verwacht, en een
  meervoudige stap in een lezing vraagt een aggregatie (WF-053, IR-SPEC
  ch. 4.1a)`, measured 2026-09-24 on this sentence with "ten minste 0 van
  de wachten", a form the lowering still declines. The lowering to
  `Kwantorvouw` opened with "only the §8.2-qualified condition" and
  declined everything else;
- the characteristic form — *"indien alle wachten van de Schouwploeg
  gediplomeerd zijn"* — resolved to `Onopgelost`, and did so silently:
  there is no set reading in that spelling for the set fence (WF-096
  until L2) to name.

Since 2026-09-19 the frontend carries both. Quantification and the
whole-period check are independent productions in v2.3.0, so the
unqualified spelling was never the language's boundary. `source.rgs` now sits
beside this document, holds exactly the five sentences quoted above, and
re-emits this `ir.json` byte for byte — the hand-written IR is unchanged,
and gate 10 sees it. That the hand-computed cells below and the emitted
wire agree is what makes this a witness of the LOWERING and not only of
the node.

## The four folds, and what each of them is for

`ervaren` / `onervaren` are the SAME matrix under the two quantifiers, so
the pair shows on every case that `none` is not `not all`: the two are
computed per member, not over the fold's verdict.

`bevoegd` / `sluitend` are the second matrix SHAPE (a characteristic check
instead of a comparison) and they carry the NIET prefix. `sluitend` is
`none` over a NEGATED matrix — "geen van de wachten is niet gediplomeerd" —
and it must answer exactly what `bevoegd` answers, in every cell. That is
the whole point of the pair: on a non-temporal matrix the negation belongs
to the MATRIX and is evaluated per member (ch. 3.9a: `test` is an ordinary
condition with the element as subject), so nothing about the quantifier
moves. The whole-period arm looks different only because its "niet
gedurende …" sits on the outer comparison and the runtime helper that folds
it is chosen per quantifier; the verdict is the same reading.

`wachttal` is the plain instance fold beside them, so each cell also pins
how many contributors the range actually has — which is what separates the
empty SET from a member that happens to be empty.

## The five cells, computed by hand before any executor ran

The bound is 5 and the comparison is STRICT (`groter dan`).

| case | the ploeg's wachten (dienstjaren, diploma) | wachttal | ervaren (`all` >5) | onervaren (`none` >5) | bevoegd (`all` dipl) | sluitend (`none` ¬dipl) |
|---|---|---|---|---|---|---|
| 1 | S1: 8 ja, 6 ja | 2 | **ja** | nee | **ja** | **ja** |
| 2 | S1: 8 ja, 3 nee | 2 | nee | nee | nee | nee |
| 2 | S2: 9 ja | 1 | **ja** | nee | **ja** | **ja** |
| 3 | S1: 3 ja, 5 ja | 2 | nee | **ja** | **ja** | **ja** |
| 4 | S1: geen wachten | 0 | **ja** | **ja** | **ja** | **ja** |
| 5 | S1: leeg ja, 3 ja | 2 | nee | **ja** | **ja** | **ja** |

**Case 2 is the `none` ≠ `not all` cell.** S1 holds one wacht above the
bound and one below it, so `all` fails on the second and `none` fails on
the first: neither characteristic is attributed. A reader that computed
`onervaren` as the complement of `ervaren` would attribute it here.

**Case 2 also holds a SECOND ploeg**, and it is there to pin the RANGE. S2's
single wacht is at 9, so S2 is `ervaren` while S1 is not — on the same run,
over the same population. An executor that quantified over every
`Strandwacht` instead of over the ploeg's own wachten would drag S1's wacht
at 3 into S2's fold and read S2 as not ervaren.

**Case 3 pins the strictness at the bound** (5 is not greater than 5) and is
the cell where `none` is true with a non-empty set.

**Case 4 is the vacuous cell**: an empty contributor set reads **waar**
under BOTH quantifiers (ch. 3.9a). `wachttal` is 0 in the same case, so the
cell says out loud that the set really is empty and not merely unreachable.

**Case 5 is the leeg cell.** W1 has no `dienstjaren` at all. An ordering
comparison over leeg is FALSE (ch. 1.2d), so W1 fails the matrix: `ervaren`
is onwaar. It does not SATISFY the matrix either, so it is no witness for
`none`, and with W2 at 3 nobody is above the bound — `onervaren` is waar.
Leeg is therefore not a third truth value here and not a vacuous pass: that
is the difference with `jutterswet-ploegpremie` case 7, where an ABSENT
TIMELINE passes the whole-period matrix vacuously and thereby BREAKS
`none`. The two goldens pin opposite answers for superficially similar
inputs, on purpose.

`asOfDate` is null in every case: nothing in this document is
time-dependent, which is exactly what makes the matrix non-temporal.
