# jutterswet-ketenband

The **case-input law witness** (`RUNTIME-SEMANTICS.md` ch. 5.7a,
maintainer decision 2026-09-10): a supplied role link establishes the
relation instance itself, and BOTH roles read it. Before this golden no
corpus case pinned self-referential navigation in two directions at all,
and no case anywhere supplied a relation from a side other than the one
whose executor agreement was already known.

## The shape

One self-referential fact type with two distinctly named roles on one
object type, a to-one side and a to-many side, and two rules that each
navigate through the OTHER role — so a swapped resolution shows up in
the outcome rather than cancelling out:

```
Feittype ketenband
    de voorligger (mv: voorliggers) Wachter      to-one
    de opvolger   (mv: opvolgers)   Wachter      to-many
Eén voorligger heeft meerdere opvolgers

voorliggerrang  := het rangnummer van de voorligger van de Wachter
opvolgeraantal  := het aantal opvolgers van de Wachter
```

The emitter binds both by NAME: the navigated read carries
`navigatieBindingen [{ketenband, voorligger}]`, the count carries
`sourceRole {ketenband, opvolger}`. Neither reaches the by-pair derivation,
which is ambiguous on a self-typed pair by construction.

## The three cases: one relation, three spellings

W1, W2, W3 with `rangnummer` 10, 20, 30. One relation throughout: **W1 is
the voorligger of both W2 and W3.** Each case spells it differently.

| case | the case supplies | supplies the reverse of |
|---|---|---|
| 1 | `W1{opvolgers:[W2,W3]}`, `W2{voorligger:[W1]}`, `W3{voorligger:[W1]}` | both legs written out |
| 2 | `W1{opvolgers:[W2,W3]}` | the to-many leg only |
| 3 | `W2{voorligger:[W1]}`, `W3{voorligger:[W1]}` | the to-one legs only |

## Hand-computed absolutes (identical in all three cases)

| Wachter | `voorliggerrang` | `opvolgeraantal` |
|---|---|---|
| W1 | **leeg** | **2** |
| W2 | **10** | **0** |
| W3 | **10** | **0** |

The identity of the three rows IS the law: how a case spells a relation
does not change what the model computes from it. The load-bearing
entries are the ones naming the direction the case did not supply — case
2 reads `W2.voorliggerrang = 10` although no case ROLE line names W2 at
all, and case 3 reads `W1.opvolgeraantal = 2` although no case role line
names W1. (Every instance still carries its own `rangnummer`; it is the
relation, not the instance, that is spelled from one side only.)

`W1.voorliggerrang` is **leeg** in every case, and that is the flank:
nothing fills `voorligger` toward W1, so ch. 5.8 rule 1 reads leeg.
Establishing a relation in both directions is not the same as inventing
one — a mirror that over-fired would read a rangnummer here.

## Red-first

Measured on the directed reading this golden replaces (the loader
mirroring only `wederkerig` fact types), same wire, same cases:

```
case1  oracle: PASS (6 expectations)            decision-independent
case2  MISMATCH: W2.voorliggerrang = leeg, expected 10
       MISMATCH: W3.voorliggerrang = leeg, expected 10
case3  MISMATCH: W1.opvolgeraantal = 0, expected 2
```

## The differential square

`ref` ≡ `clingo` over all 18 expectations (gate 5 ≡ gate 6), anchored to
the hand-written absolutes above; the Drools backend PLAYS all three
cases (gate 12), so ref ≡ clingo ≡ Drools — the first corpus witness for
which that holds on a self-typed 1:N relation supplied from either end.
NOOP-REFUTING: every case pins computed values absent from the raw
input, so a do-nothing run mismatches them (gate 7).

Gate 13: no `Number` here carries a `unit`, so the golden
falls outside the twist set and moves no gate-13 count. Giving
`rangnummer` a unit would pull it in; that is extra work without
demonstrated risk, so the deliberate default is no unit.
