# jutterswet-meldtermijnen

The **per-instance term witness**: a `Fold` whose `init` is not a constant but
a `Construct` over the SUBJECT'S OWN attribute — `Openstaand(<the termijn of
this Meldplicht>)`. Every earlier fold in the corpus starts from a value that
is the same for every instance (a literal, at most a document parameter). Here
two Meldplichten with the same event register and DIFFERENT terms settle
differently, and nothing but the init distinguishes them.

Beside it stands the SAME fold over the same register with a `Construct` init
whose payload is a document **parameter** — `Openstaand(<de standaardtermijn>)`,
the position the duty would have been in under the statutory standard term. The
two rules differ in one leaf and take two different routes through the ASP
carrier: the own-term init is lowered at rule level (it needs a subject to read
from), the standard-term init inside the closed-body grammar (it does not).
Both routes are pinned here, side by side, on the same register.

That is what a reified duty needs. A reporting duty under the Jutterswet is an
object of its own: it hangs on the find it belongs to, it carries the term
that find's category sets, and it carries the register of what happened. The
term is a property of the DUTY, not of the statute, so the fold that walks the
register must start from the duty's own term. Without that, "each with its own
term" is not expressible as a fold at all.

## What it declares

| Thing | Form |
|---|---|
| domain `meldstatus` | `SumType`: `Openstaand(dagen)`, `Vervuld`, `Geschonden` |
| domain `gebeurtenis` | `SumType`: `Dagverloop(dagen)`, `Melding` |
| objecttype `Meldplicht` | the reified duty (`bezield: false`) |
| attribute `Meldplicht.termijn` | whole number — the term this duty was given |
| attribute `Meldplicht.meldreeks` | `ListType` of `gebeurtenis` — this duty's own register |
| attribute `Meldplicht.meldpositie` | `meldstatus` — the own-term target |
| attribute `Meldplicht.standaardpositie` | `meldstatus` — the standard-term target |
| parameter `standaardtermijn` | whole number — the term the statute sets for everyone |

Both domains satisfy WF-087 (each carries exactly one payload-bearing
constructor, so neither normalises to an enumeration) and WF-060 (whole-number
payloads). WF-065 admits the declared `SumType` as a list element; WF-070
admits the `SumType` accumulator.

```
meldpositie:      init = Openstaand(de termijn van de meldplicht)
standaardpositie: init = Openstaand(de standaardtermijn)

λ (stand, gebeurtenis).
  match stand with
  | Openstaand resterend ->
      (match gebeurtenis with
       | Dagverloop dagen -> if dagen <= resterend
                             then Openstaand (resterend - dagen)
                             else Geschonden
       | Melding          -> Vervuld)
  | Vervuld    -> Vervuld
  | Geschonden -> Geschonden
```

`Vervuld` and `Geschonden` are absorbing: once the position has settled, no
later event moves it. The step reads only its two binders, so WF-077 holds and
the branch complement stays exact. The step is a closed lambda body (WF-069);
the `meldpositie` init is NOT, and that asymmetry is the whole point of this
golden. The `standaardpositie` init IS closed — same shape, same step, one leaf
different — which is what makes the pair a boundary and not just an example.

## Hand-computed absolutes

`standaardtermijn` is 21 in every case but case4, where it is 30 — a value that
differs from the terms the instances carry, so a `standaardpositie` that
silently read a constant or the subject's own attribute could not go unnoticed.

**case1 — two duties, one register, different terms.** Both carry
`[Dagverloop(20), Melding]`.

| | M1 (`termijn` 30) | M2 (`termijn` 10) |
|---|---|---|
| 0 | `Openstaand(30)` | `Openstaand(10)` |
| 1 `Dagverloop(20)` | 20 ≤ 30 → `Openstaand(10)` | 20 ≤ 10 false → `Geschonden` |
| 2 `Melding` | `Openstaand` arm → **`Vervuld`** | `Geschonden` arm → **`Geschonden`** |

Same events, opposite outcomes. Nothing in the rule differs between them; only
the term the init reads does.

`standaardpositie` (init `Openstaand(21)` for both): 20 ≤ 21 → `Openstaand(1)`,
then `Melding` → **`Vervuld`** for M1 and M2 alike. The standard term is the
same for everyone, so this half is deliberately blind to the difference case1
exists to show — and M2 breaching its own term while meeting the standard one is
the sharpest reading of what a per-instance term means.

**case2 — an absent term, beside a present one.** Both carry `[Dagverloop(5)]`.

- M3 has NO `termijn`. The init reads empty, so the fold's base case never
  fires, so nothing downstream of it is derivable: **`meldpositie` is empty**.
  An empty init empties the whole fold — the value/absence distinction of the
  runtime, propagating through the recursion without any special machinery.
- M4 has `termijn` 20: 5 ≤ 20 → **`Openstaand(15)`**. This is the only pinned
  value that reveals the subtraction's DIRECTION (with the operands swapped it
  becomes `Openstaand(-15)`), and it pins that the `20` came from M4's own
  attribute and not from anywhere else.

- M10 has `termijn` 7 and NO `meldreeks` at all: **both positions are empty**.
  This is the OTHER empty flank — not an empty init but an absent source. The
  list derives no presence, so neither fold has a base case to fire, and the
  reason differs from M3's even though the observation looks the same.

The instances sit in ONE case on purpose: the empty half is what makes the
empty-propagation absolute, and the filled half beside it proves the emptiness
is M3's and not a rule that stopped firing. `standaardpositie` sharpens that
further: 5 ≤ 21 → **`Openstaand(16)`** for M3 AND M4. M3's own-term position is
empty while its standard-term position is filled, on the same instance, in the
same case — so the emptiness is provably the init's own read and nothing else.
M10 keeps both empty, because there the missing thing is the register.

**case3 — a present but empty register.** M5 has `termijn` 7 and
`meldreeks: []`. The list is present with no elements, so the base case fires
and the result IS the init: **`Openstaand(7)`**, and **`Openstaand(21)`** for
`standaardpositie`. Both inits read undisturbed by any step, side by side —
this is the case that pins the two source values apart absolutely.

**case4 — the last day of one's own term.** Both carry
`[Dagverloop(30), Melding]`.

- M6 (`termijn` 30): 30 ≤ 30 → `Openstaand(0)` → `Melding` → **`Vervuld`**.
- M7 (`termijn` 29): 30 ≤ 29 false → `Geschonden` → **`Geschonden`**.

The boundary day still counts as inside the term, and the boundary is each
duty's OWN — one day of difference in the attribute flips the outcome.

Here `standaardtermijn` is **30**, not 21: 30 ≤ 30 → `Openstaand(0)` → `Melding`
→ **`Vervuld`** for M6 and M7 both. The same boundary reading, once through the
attribute and once through the parameter; and because this case's parameter
value differs from every other case's, a `standaardpositie` init that had
quietly become a constant would be caught right here.

**case5 — a settled position stays settled.**

- M8 (`termijn` 30), `[Melding, Dagverloop(31)]`: `Openstaand(30)` →
  `Vervuld` → the outer `Vervuld` arm → **`Vervuld`**. The term running out
  afterwards does not re-arm a duty already met.
- M9 (`termijn` 3), `[Dagverloop(4), Melding]`: 4 ≤ 3 false → `Geschonden` →
  the outer `Geschonden` arm → **`Geschonden`**. A late report does not cure a
  breach.

`standaardpositie` (21): M8 `Melding` → `Vervuld` → `Dagverloop(31)` hits the
absorbing `Vervuld` arm → **`Vervuld`**; M9 4 ≤ 21 → `Openstaand(17)` →
`Melding` → **`Vervuld`**. M9 breached its own three days and met the statutory
twenty-one — the same contrast as case1, now on the settling side.

Every expectation is absolute — an `attributeValue` with a
`constructor`/`payload`, or an `attributeEmpty` — plus `consistent`.

## Why five cases

Each was measured against a mutant of this document, in `ref`:

| mutation | cases that still pass | the case that refutes it |
|---|---|---|
| `meldpositie`'s `init` becomes the constant `Openstaand(30)` | none | all five |
| `<=` weakened to `<` | 1, 2, 3, 5 | case4 (M6 → `Geschonden`) |
| the subtraction's operands swapped | 1, 3, 4, 5 | case2 (M4 → `Openstaand(-15)`) |
| the outer `Vervuld` arm rewritten to `Geschonden` | 1, 2, 3, 4 | case5 (M8) |
| the outer `Geschonden` arm rewritten to `Vervuld` | 2, 3 | case1 (M2), case4 (M7), case5 (M9) |
| `standaardpositie`'s init reads `de termijn` (the two rules become one) | none | all five |
| `meldpositie`'s init reads `de standaardtermijn` (the routes swap) | none | all five |
| `standaardpositie`'s init becomes the literal `21` | 1, 2, 3, 5 | case4 (the case whose parameter is 30) |

The first row is the golden's reason for existing: the constant-init mutant is
exactly the document this corpus could already express, and every one of the
five cases sees the difference. The last three are the second rule's: the two
inits must stay told apart, in both directions, and the parameter must stay a
parameter.

## Gates

Gate 2 validates and round-trips the dump; gate 3 passes it verifier-clean;
gate 4 validates the five case files; gate 5 (`ref`) and gate 6 (clingo) both
replay all five (5, 7, 3, 5 and 5 expectations) against exactly these
hand-written values — ref ≡ clingo, never parity alone. Gate 7 (the no-op
guard) fails on all five, including case2: the three empty expectations would
survive a do-nothing executor, but `M4.meldpositie` is absent on the raw input,
so the case as a whole refutes one and nothing is added to the survivor list.

Gate 6 is also the only gate that sees the closed-body route the
`standaardpositie` init takes. Removing the carrier's recursive lowering of a
non-literal `Construct` payload in a closed body — measured, as a fault
substituted for that one arm — leaves the whole corpus green WITHOUT this
rule, and fails all five cases WITH it. That is the reason the second rule
exists: an executor path nothing exercises is a path that can change while
every gate stays green.

## The carrier boundary this golden moves, and where it now stands

On the ASP side the fold is index recursion over the reified register. The
accumulator and result facts are keyed by the LIST ID, and for an own
attribute that id carries the subject:

```
fa1(alist(S,"meldreeks"),0,openstaand(A2)) :- lpres(alist(S,"meldreeks")), val(S,"termijn",A2).
```

The same `S` keys the head and grounds the init read, so M1's `30` and M2's
`10` can never meet. That coupling is exactly the condition under which a
subject-dependent init is safe, and the carrier now checks it: an init that
reads the subject rides the rule-level lowering only when the fold's source is
subject-keyed.

Over a list PARAMETER the id is `plist(p)` and the subject drops out of the
head. Two duties' init facts would then land on ONE key and the recursion would
hand every subject every subject's outcome — measured, before the fence
existed, as both instances carrying both `geschonden` and `vervuld`. That
shape is refused loudly rather than answered wrongly. It is the one refusal of
the four that is not about a constructor payload at all — it fires on the
init's SHAPE, so a BARE attribute read as init trips it with no constructor in
sight; the other three are payload positions. All four are inventoried in
`spec/ASP-TRANSLATION.md` S19 and pinned by the fault fixtures in
`backends/clingo/test/test_clingo.ml`, since no corpus document can witness a
refusal.

`standaardpositie` sits on the other side of that same fence. Its init reads no
subject, so it never needs the coupling; it lowers inside the closed-body
grammar and its parameter read becomes a body atom of the base rule:

```
fa6(alist(S,"meldreeks"),0,openstaand(P7)) :- lpres(alist(S,"meldreeks")), param("standaardtermijn",P7).
```

Same list id, same head shape, a different body atom — the payload's binding
atom travelled out of the closed-body lowering with the term. The two
routes are told apart by the init's SHAPE — an attribute read, bare or directly
under a constructor, takes the rule-level route; everything else takes the
closed one. That is a route choice, not a semantic test: an init that reads the
subject from DEEPER down (under an addition, under a `Let`) takes the closed
route and is refused there, loudly, naming this same shape rule. `ref` plays
such a form; the ASP carrier does not, and says why.

`corpus/golden/jutterswet-meldplichtspoor` records the observation this golden
answers: that a position with a per-instance term stayed out of reach of the
ASP carrier while `ref` played it. It no longer does. What is still out of
reach there is the other half — a duty that names its parties as a VALUE —
and that needs a new kind of datatype, not a wider fence.
