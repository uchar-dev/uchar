# jutterswet-schouwrooster — a day-kind unit spelling on the wire

The public witness for `dagsoortPerEenheid`, the map that couples a **unit
spelling** to a declared day kind (`spec/IR-SPEC.md` ch. 2.9 DayKind: the
`plural` field is the duration-unit spelling, and spelling lookup is trimmed
and case-insensitive).

## What it declares

Two day kinds, `schouwdag` and `stormvloeddag` (the second is the statute's
own term, Jutterswet art. 3b–3c), each defined by a **compound** condition
over the day-kind reference `de dag`: two date comparisons under `alle`,
bounding the day on both sides. Only dates and literals are read, so WF-031's
purity clause holds; no day kind names another, so the reference graph is
empty and WF-085 holds vacuously. Declaring a day kind registers **both** its
spellings, singular and plural, so the map carries four entries:

    schouwdag   -> schouwdag        stormvloeddag   -> stormvloeddag
    schouwdagen -> schouwdag        stormvloeddagen -> stormvloeddag

The two windows are complementary inside one bounded period: a schouwdag is
later than 28-02-2026 **and** earlier than 01-10-2026; a stormvloeddag is
later than 30-09-2026 **and** earlier than 01-03-2027. Bounding both sides is
deliberate. A half-open condition (`… eerder dan 01-10-2026`, full stop) makes
a claim about every day since the calendar began, which is not what a seasonal
schedule says, and it hides the fact that the two definitions partition a
period rather than split the time axis in two.

## Two shapes, both on purpose

- `schouwdagen` is the declared `eenheid` of the three `Strandmeester`
  attributes and of the `schouwdrempel` parameter, so the spelling reaches
  evaluation. **What reaches evaluation is the UNIT, not the day-kind-ness of
  it.** Measured: replace every `schouwdagen` spelling in this document by
  `gram` — the unit annotations and the registry key alike — and the verifier,
  the reference interpreter, the ASP backend and the Drools backend all stay
  green on the same pinned values. The arithmetic runs on a unit name; nothing
  on that path asks the registry what that name is.
  The unit is load-bearing in its own right, and skewing it is loud: retype
  one attribute's declared `eenheid` to `dagen` and the verifier refuses with
  WF-091 (`unit mismatch: attribute reference 'extra schouwlast' is typed
  'schouwdagen' … but the declaration it reads from is 'dagen'`), the
  reference interpreter faults with `incompatible units: schouwdagen vs
  dagen`, and the ASP backend faults on the additive join. (In the reference
  interpreter that fault is case1's: case2's second operand is leeg, so the
  join never puts two filled units side by side.)
- `stormvloeddagen` is **registered and never mentioned** by any expression.
  That is the ordinary shape of a day-kind registry entry, and a witness that
  means to carry the map has to carry it too.

## What no consumer reads here

No consumer resolves a spelling through `dagsoortPerEenheid` in *this*
document. The verifier consults the map at exactly two positions — the
day-kind-unit admission beside the nine calendar units in date arithmetic
(WF-035) and in a tijdsduur (WF-038) — and neither form occurs here. The
reference interpreter and the ASP backend read the map at zero positions. The
Drools backend does cross it, but only from those same two duration lowerings
(§6.10, §6.11), which are likewise absent.

Four mutants confirm it. Each was replayed through all four consumers against
the pinned expectations of `case1.json` and `case2.json`:

| mutant | verifier | ref | clingo | Drools |
|---|---|---|---|---|
| `dagsoortPerEenheid` emptied | PASS | PASS | PASS | PLAYED |
| every value replaced by a non-name | PASS | PASS | PASS | PLAYED |
| `resolvedDagsoorten` emptied | PASS | PASS | PASS | PLAYED |
| one value pointed at an undeclared day kind | PASS | PASS | PASS | PLAYED |

Carrying without executing is the corpus norm for this form, not a defect of
this golden: over all 156 goldens there is not one **executed** day-kind
position, because the two forms that would execute one are refused (next
section). And a carrying witness is precisely what a resolution arm needs —
an arm that crosses a map value against `dagsoorten` needs a document the map
lives in, not a document that computes with it.

## Firsts this golden puts on the wire

- **A `Constante` named `de dag`.** Before this golden, `Rekendatum` was the
  only `Constante` name in golden, rood and negatief together (2 occurrences
  in 2 documents). `spec/IR-SPEC.md` ch. 4.1 enumerates the ContextValue row
  as `Rekendatum` or `Rekenjaar` and nothing else, so the day-kind reference
  is a third name that row does not name. Nothing checks it either: rename it
  to `de dagX` in all four positions and all four consumers stay green on the
  same values. The verifier and the two OCaml executors never look at the
  field; the Drools day-kind compiler keys on the literal string
  (`"de dag".equalsIgnoreCase(...)` in its day-kind operand compiler), so a
  misspelling there degrades to an unrecognised operand, not to a diagnosis.
  This is also the first day-kind condition in the corpus that reads the day
  at all: the two pre-existing definitions compare two literals to each other
  and degenerate to a constant.
- **A bare `LATER` comparison.** `LATER_OF_GELIJK` already existed (3
  documents); the strict `LATER` existed nowhere in the repository. It arrives
  inside a day-kind condition, which no executor evaluates — so the operator
  is on the wire, schema-valid and verifier-checked, with **zero** executed
  coverage. Worth knowing before anyone treats its presence as evidence that
  strict date ordering is exercised.
- **A `Dagsoort` declaration in a `bron.rgs`**, and with it the first
  frontend-emitted `Dagsoortdefinitie` entries in `regels`. Gate 10 re-emits
  this source byte-identically, so the frontend's day-kind path — declaration,
  plural registration, compound condition — is pinned end to end.

## What is deliberately absent

The day-kind **duration** forms — `tijdsduur van A tot B in hele
<dagsoort>` and `datum ± N <dagsoort>` — are not here, by measurement.
Both are recorded flanks that no executor of gates 5 and 6 runs: the
reference interpreter faults at evaluation ("a day-kind tijdsduur is a
recorded flank, not executed"; likewise for the duration operand of date
arithmetic) and the ASP backend faults at translation with the same
reading. A golden carrying either form could not replay, so the executed
half of this witness is the unit spelling in ordinary arithmetic, and the
duration half stays a flank until the semantics chapter fixes day-by-day
day-kind reckoning for every executor.

## The pins, hand-computed

`schouwplicht = vaste schouwlast + extra schouwlast` (both in
`schouwdagen`); `schouwvrij` iff `schouwplicht < schouwdrempel`, strictly.
With `schouwdrempel = 20 schouwdagen`:

| case1 | vaste | extra | schouwplicht | `< 20` | schouwvrij |
|---|---|---|---|---|---|
| S1 | 12 | 3 | **15** | yes | present |
| S2 | 18 | 6 | **24** | no | absent |
| S3 | 20 | 0 | **20** | no (strict) | absent |

S3 is the boundary: `20 < 20` is false, so the kenmerk stays absent.

`case2` pins the coalescing half of `plus` (`RUNTIME-SEMANTICS.md` ch. 1.2:
"5 + leeg = 5") at a day-kind-unit position: S4 supplies only its vaste
schouwlast of 9, the extra one is leeg, so `schouwplicht = 9 + leeg = 9`
schouwdagen — a filled value, not leeg — and `9 < 20` makes S4 schouwvrij.
Both cases also pin `consistent`: nothing in this model can be violated, and
the vacuous pin is what makes a future consistency form's arrival visible.
