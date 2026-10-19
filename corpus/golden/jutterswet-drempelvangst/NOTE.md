# jutterswet-drempelvangst

The **lowering-boundary witness** of the let-lowering card
(`IR-SPEC.md` ch. 4.10, boundary (c)): one golden that pins **both**
sides of the third lowering boundary, so neither can move unnoticed.

## What it pins

```
Het drempelgewicht van een Jutter moet berekend worden als
    de som van het gewicht van alle vondsten van de Jutter
    die groter zijn dan de drempel of 0 als die er niet zijn.
Daarbij geldt:
    de drempel is 5.

Het drempelaandeel van een Jutter moet berekend worden als
    de vangst plus de vangst.
Daarbij geldt:
    de vangst is het drempelgewicht van de Jutter.
```

* **`drempelgewicht` — the STRUCTURE-SENSITIVE read.** `de drempel` is read as
  the right-hand side of a §5.6 subselectie comparison. The resolver decides
  there on the *shape* of the resolved subtree (only a literal or a parameter
  may be a subselectie RHS — an attribute would mis-bind against the element),
  so the variable is **substituted** there and no `Let` is emitted: the wire
  carries `filter = Comparison(gewicht > Literal 5)`. Had it been lowered to a
  `BinderRef`, the shape test would have failed on the opaque leaf, the filter
  would have degraded to an `Unresolved` marker (`Conditie.Onopgelost` in the
  frontend) and the whole aggregation would have become a marker — `19` would
  have turned into a WF-044 unsolved-state fault. That is a **wrong answer**,
  not a lost optimisation, which is why the boundary is normative.
* **`drempelaandeel` — the ORDINARY read.** `de vangst` is read twice in
  plain arithmetic, so it lowers exactly as ch. 4.10 says: **one** `Let`
  with **two** `BinderRef`s. Sharing is visible on the wire.

Together they fence the boundary from both sides: a resolver that
lowered too little loses the `Let` in `drempelaandeel`; one that lowered
too much loses the `19` in `drempelgewicht`. Gate 10 re-emits `source.rgs`
byte-identically, so either drift is caught at the source level too.

## Hand-computed absolutes

`J1` has four vondsten with gewicht 3, 7, 12 and *absent*; `J2` has none.

| Jutter | vondsten > 5 | `drempelgewicht`        | `drempelaandeel`  |
|--------|--------------|-------------------------|-------------------|
| J1     | 7, 12        | 7 + 12 = **19**         | 19 + 19 = **38**  |
| J2     | —            | **0** (the §5.8.2 "of 0 als die er niet zijn" fallback) | 0 + 0 = **0** |

The absent-gewicht vondst contributes nothing: it fails the `> 5`
comparison under the §5 leeg-operand discipline.

## The differential square

`ref` ≡ `clingo` over these expectations (gate 5 ≡ gate 6), anchored to
the hand-written absolutes above; the Drools backend replays them
(gate 12), so ref ≡ clingo ≡ Drools. NOOP-REFUTING: every one of the
four attribute values is derived and absent from the raw input, so a
do-nothing run mismatches all four (gate 7).
