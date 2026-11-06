# uchar IR — Runtime semantics

*Status: ch. 1–11 are normative **draft** (ch. 1–5 the v0 mirror with
tracked deltas and OPEN decisions; ch. 6–8 drafted; ch. 9–10 v1 kernel
extensions with no v0 counterpart — `spec/proposals/PHASE5B-KERNEL.md`;
ch. 11 the Phase-5c record extension —
`spec/proposals/PHASE5C-TUTORIAL.md`). This document reads stand-alone
(graduated into the self-containedness gate at the Phase-1 exit,
2026-08-19): behaviour it does not document is undocumented — the
mirror's provenance (which toolchain, and the repo relation) lives in
`spec/PLAN.md` §5 and the git history, never here. Every
`§…` reference is a section number of the RegelSpraak language
specification — numbers only, no text from it is reproduced (construct
keywords, such as the fallback clause quoted in ch. 1.2, excepted). This
document's own chapters are cited as `ch. N`.*

Every chapter states its semantics as equations and tables plus worked
examples with concrete expected values; the examples become `ref/` unit
tests verbatim.

---

## 1. Values and emptiness

### 1.1 The value domain

A computed value is one of: an exact rational number (ch. 2.1), a text, a
boolean, a date (day or millisecond precision), an enumeration value, a
constructor value (ch. 9 — a declared constructor tag with, for a unary
constructor, one payload value), a list value (ch. 10 — an ordered,
possibly empty sequence of element values; `[]` is a value, leeg is the
absence of one), a record value (ch. 11 — the declared fields of a struct
type mapped to values, each of the field's datatype; filled by
construction, never carrying a leeg field) — or
**empty** (leeg): the distinguished absence of a value, belonging to no
datatype. Empty is not zero, not the empty string, not false; every context
below defines explicitly what it does with an empty operand. (Empty is also
distinct from `Unresolved`, which marks an *unresolvable construct*, not an
absent value — `CONSTRUCTS.md` row 16.)

### 1.2 Empty in each context (v0 mirror)

The v0 discipline, mirrored faithfully. Three behaviours occur: **coalesce**
(empty counts as 0), **propagate** (empty in → empty out), and **dedicated
rule**. Worked examples are exact expected values.

**Arithmetic operators**

| Operator | Empty behaviour | Worked examples |
|---|---|---|
| plus | both operands coalesce | 5 + leeg = 5; leeg + leeg = 0 |
| min | both operands coalesce | leeg − 5 = −5; 5 − leeg = 5; leeg − leeg = 0 |
| verminderd met | **left propagates; right coalesces** | leeg vm 5 = leeg; 5 vm leeg = 5; leeg vm leeg = leeg |
| maal | both coalesce | 5 × leeg = 0; leeg × leeg = 0 |
| percentage van | both coalesce | leeg% van 600 = 0; 10% van leeg = 0 |
| gedeeld door (both variants) | **left coalesces; empty divisor propagates as error-empty** | leeg ÷ 5 = 0; leeg ÷ leeg = 0 (left tested first, §6.5); 5 ÷ leeg = leeg **+ §9.5 inconsistency** |
| tot de macht | empty base propagates; the runtime-exponent variant propagates on either operand | leeg² = leeg; 4^leeg = leeg |
| wortel | propagates (a negative radicand also yields empty **+ §9.5 inconsistency**) | √leeg = leeg; √−4 = leeg + inconsistency |
| absolute waarde | propagates | \|leeg\| = leeg |
| afronding | propagates | afgerond(leeg, 2) = leeg |
| begrensd | empty *value* propagates; an empty *bound* means "no bound" | begrensd(leeg, 0, 100) = leeg; begrensd(150, leeg, 100) = 100 |

The subtraction pair is the heart of the discipline: *min* and *verminderd
met* denote the same subtraction and differ **only** in empty-handling
(§6.3): `leeg min 4 = −4` but `leeg verminderd met 4 = leeg`. The
propagation of empty through power, root, absolute value, rounding and
clamping exists so that a mandated empty (division by empty, negative
radicand) survives to the output instead of crashing or becoming a silent
number; the rounding case is not §-stated and is an adopted reading in v0 —
recorded as such.

**Dates and durations**

| Context | Empty behaviour | Worked examples |
|---|---|---|
| date ± duration | empty *date* propagates; empty *duration* coalesces to 0 — **Executed** (2026-08-25, both executors; ch. 3.8) | leeg + 5 dagen = leeg; datum + leeg dagen = datum |
| date construction / part extraction | any empty part or date propagates — day-precision extraction and the Easter builtin **Executed** (2026-08-25, both executors; ch. 3.8); date construction **Executed** (2026-10-22, both executors; ch. 3.8) | jaar uit leeg = leeg; eerste paasdag van leeg = leeg; de datum met jaar: leeg, maand: 3 en dag: 1 = leeg |
| tijdsduur van A tot B | any empty operand propagates (never 0) — **Executed** (2026-08-25, both executors; ch. 3.8) | tijdsduur van leeg tot d = leeg |

**Text, booleans, enumerations**

- Text coercion of empty yields the empty string `""` — never `"0"`, never
  a spelled-out null: `"a" ++ leeg ++ "b"` = `"ab"`.
- There is no boolean or enumeration coalescing; absence is handled by the
  comparison rules and the leeg/gevuld predicates below.

**Comparisons (three-valued at the edges)**

| Comparison | Rule | Worked examples |
|---|---|---|
| ordering (<, ≤, >, ≥, eerder/later) | any empty operand → false | leeg < 5 = onwaar; leeg < leeg = onwaar |
| numeric equality | true iff **both** empty, else value equality | leeg = leeg → waar; leeg = 5 → onwaar |
| non-numeric equality (text/enum/boolean, §5 Tabel 38) | any empty operand → false | leeg =ₜ leeg → onwaar |
| date equality (v0's Tabel-38 guard on the date shape) | any empty operand → false — in particular **both** empty, never the numeric two-empties-equal cell | leeg =ᴰ leeg → onwaar; leeg =ᴰ 2026-06-21 → onwaar |
| **inequality — every datatype (§5.6 Tabel 44)** | true iff **exactly one** empty, else value inequality — uniform over numeric, percentage, text, enum, boolean, date, and the ch. 9/11 constructor and record values | leeg ≠ 5 → waar; 5 ≠ 3 → waar; leeg ≠ leeg → onwaar; leeg ≠ₜ "a" → waar; leeg ≠ᴰ 2026-06-21 → waar; leeg ≠ᴰ leeg → onwaar |
| leeg / gevuld predicates | absence — including a failed to-one navigation — **is** leeg = waar; the `negated` field is the inner, per-value negation and complements the test — negated leeg is gevuld, negated gevuld is leeg (the qualifier's own negation is `WholePeriod.negated`, ch. 3.9. Under that qualifier the inner negation is per day as well, so there too the negated leeg test is the gevuld test and the negated gevuld test is the leeg test — ch. 3.9, decided 2026-09-26; until then the threshold a negated leeg test takes under the qualifier was an OPEN norm point and all three executors refused the form by name) | "de leeftijd van zijn partner is leeg" is waar for a partnerless person; `leeg(5)` negated = waar, `leeg(leeg)` negated = onwaar (WV-1.2-neg) |

Note the deliberate asymmetry, now confined to **equality**: numeric
equality treats two empties as equal; non-numeric equality (text, enum,
boolean, and — by v0's Tabel-38 guard on the date shape — date) does not.
**Inequality carries no such asymmetry.** After the Tabel-44 ratification
(2026-08-28, source-verified against RegelSpraak typeringen §5.6) it is
uniform over *every* datatype: exactly one empty operand is waar, both
empty is onwaar, else value inequality. So `≠` is no longer the exact
complement of `=` over a single leeg operand: where exactly one operand is
empty `=` is onwaar (numerically because two empties, not one, are the
equal cell; non-numerically because any empty is false) yet `≠` is waar.
Both equality rules are mirrored, and the ch. 1.4 decision keeps
them: the equality rules where empty carries meaning are on its
must-preserve list. Dates straddle the two only for equality: ordering
rides the numeric rules on the civil day number and inequality rides the
uniform Tabel-44 rule, but date equality follows the any-empty-is-false
rule — the mirror of v0's explicit guard `cmp && !(l == null && r ==
null)` on the date-vs-date comparison shape (its Tabel 38 classifies Datum
non-numeric). Pinned by `jutterswet-paasvondst` case5 (both-empty
gelijk-aan → onwaar, a dateless subject gains no kenmerk) beside
`jutterswet-jutdatum` case3 (one-empty: gelijk-aan onwaar, ongelijk-aan
waar).

**Aggregations**

Empty contributors are skipped, never counted as 0. The result for an
empty or all-empty source differs per aggregation — this is the
§5.8.2/§4.20 split:

| Aggregation | Empty/all-empty source | Worked examples |
|---|---|---|
| som | **empty**, unless the declared fallback clause ("… of 0 als die er niet zijn") substitutes | som over {5, leeg, 3} = 8; som over {leeg, leeg} = leeg; with fallback = 0 |
| totaal | **0**, unconditionally (§4.20) | totaal over {} = 0 |
| aantal | 0 (a count, never empty) | aantal over {} = 0 |
| aantal gevulde | 0 (a count, never empty) | aantal gevulde over {leeg} = 0; over {} = 0 (ch. 3.4c) |
| minimum/maximum, eerste/laatste | **empty**; no fallback form exists | min(5, leeg, 3) = 3; min(leeg, leeg) = leeg |
| dimension folds (§5.8.5; ch. 5.8c) | empty cells skipped; all-empty selection = empty (som may use its fallback), but the filled count, which is 0; over the product of one selection per dimension; eerste/laatste by position | cells {Q1: 3, Q2: leeg, Q3: 2}: som over Q1–Q3 = 5; som over {Q2} = leeg; eerste over Q1–Q3 = 3, laatste = 2; aantal gevulde = 2; over two dimensions, ch. 5.8c |

**Eerste/laatste are ordering folds** over instances and over listed
operands; over the cells of a dimensioned attribute they pick by position
(ch. 5.8c, 3). The articulated value fold
"de eerste van A en B [en C …]" (§5.8.3/§4.14–.15, ≥ 2 listed operands)
keeps the least/earliest operand and "de laatste van" the
greatest/latest — a "<"/">" fold over the operand *values*, never a
positional pick of the first/last filled operand: de eerste van 5 en 3
is 3, not 5. Empty operands are skipped and all-empty yields empty (the
table row above); a tie takes the *later* operand (v0's listed fold,
`acc < x ? acc : x`) — value-equal, so unobservable today. The listed
fold is dispatched by argument *shape*, for v0's listed family
minimum/maximum/eerste/laatste ONLY: v0's resolver stamps
the element type (`elementType` on the wire since L4) with the first operand's owner even here, a
vestigial tag — an instance fold always carries exactly one source
argument, a listed fold at least two. For som/totaal/aantal the element
is *load-bearing* (v0's accumulate joins it and reads only the first
operand), so a multi-operand form of those faults loudly — never a
silent list fold v0 would compute differently. **For `aantal` that
promise is kept at BOTH spellings from 2026-09-20, and until then at
one** (maintainer decision of that date). Where the element type is
stamped — the producer's spelling — a multi-operand count does fault
loudly. Loudly, but not by NAME: the reference interpreter's refusal
there is its residual arm over fold-source combinations, which states
the position and never the combination rejected, so it is one sentence
for every shape that reaches it and it is listed among the bottom
sentences no red case may pin. "Faults loudly" is therefore all this
paragraph promises, and all it is measured to keep; a named arm per
kind is the cure the bottom-sentence fence always names, and this kind
does not have one.
Where the element type is absent nothing in the runtime distinguished the shape at
all: the fold took the operand list and the interpreter counted the
FILLED operands, silently, which is exactly the case this sentence
forbids, and no other executor answered it. That spelling is refused on
the WIRE now, by WF-037's count-arity arm (`IR-SPEC.md` ch. 4.3), so the
promise holds at both — one at evaluation, one before anything runs.
Below two operands the residue closed on 2026-09-23 (maintainer
decision): a count of ONE operand or none without an element type has no
source at all, and WF-037's second count arm refuses it on the wire —
the interpreter had answered it with the same filled count, and with
**1** on a single list literal whether the list held two elements or
none. The interpreter's arm for a count handed values, out of reach
through any well-formed document since that date, now faults by name
instead of counting: aantal over the operands (5 kg, leeg), over (5 kg),
over no operand, and over the list literals [1, 2] and [] each fault
with "a count folds INSTANCES and this node hands it …" (worked examples
`WV-3.4a-opgesomd-*`, `-eenoperand-*`, `-geen-operand`,
`-lijstliteraal-*`). **Ratified (maintainer
decision 2026-09-04, landed 2026-09-05):** the numeric half of this fold
is a v1 *widening*, not a mirror. v0's static typing (§4.14/§4.15)
admits eerste/laatste over date expressions only — a concrete non-date
operand is a compile-time ERROR ("eerste van vereist datumexpressies,
kreeg …", `Typing.dateAggregation`) and the model is refused; only
Onbekend-typed operands reach v0's runtime fold, which is itself the
type-generic ordering above. v1 admits **any single comparable family**
under that same ordering, and the fence is now written where the wire
can be checked: WF-038's ordering-fold clause requires every operand of
`first`/`last` (eerste/laatste) and `minimum`/`maximum` — read off the operand list, so
in either wire form, the instance fold's one set-valued argument and the
articulated fold's listed ones alike — to come from one comparable
family: numbers, or dates of ONE precision. An equality-only operand
(`Enumeration`, `SumType`, `StructType`, `Text`, `Boolean`,
`ListType`: the ch. 2.4 orderability paragraph of `IR-SPEC.md`) is
ill-formed, and so is a fold that mixes families — a number against a
date, and (since 2026-09-05) a day-precision `Date` against a
millisecond-precision one, which are two representations with no order
between them. That last
half is where v1 and v0 agree rather than diverge: v0's static typing
demands one precision too ("alle datumexpressies moeten dezelfde
tijd-precisie hebben", the same `Typing.dateAggregation`), so the
tightening puts the wire fence and the frontend that writes to it in
agreement. An operand without a static type defers: since L2 that is a
reference whose declaration does not resolve (WF-034 reports it) or an
`Unresolved` without a `type` (an executor faults on reaching it,
WF-044). Until L2 it was the unknown sentinel, the one operand shape v0
never typed and did hand to this very fold at runtime; the sentinel left
the datatype union at L2 (B16). The
delta belongs to the `FunctionKind` (aggregations) row of
`spec/CONSTRUCTS.md` at the serial merge update.
Worked examples (ref tests verbatim; the `jutterswet-peildatum` golden
pins them through both executors — the numeric pair is the widening's
witness): eerste van (5, leeg, 3) = 3 and laatste van
(5, leeg, 3) = 5; eerste van (2026-06-21, 2026-05-01) = 2026-05-01;
laatste van (2026-06-21, leeg) = 2026-06-21; eerste van (leeg, leeg) =
leeg. Worked example of the fence, the same fold one operand family
further: `eerste van (2026-05-01, 3)` has no answer — 3 is not earlier
or later than a date and 2026-05-01 is not smaller or greater than a
number — and is refused at the document layer, not at evaluation. So is
`laatste van (Zuid, Noord)` over an enumeration domain, where a value
*was* produced before the fence: the ordering the production runtime
falls back on there is `Comparable` on the label spelling — alphabetical,
case-sensitive ASCII, nobody's design — which is why admitting it would
have meant ratifying an accident. And so is `eerste van (2026-06-21,
2026-06-21T06:10:00)`: one operand names a whole day, the other a point
inside a day, and neither is earlier than the other (ref, before the
fence: `unordered comparison: 2026-06-21 vs @…ms`).
*What the widening rests on:* `jutterswet-peildatum` is the only
document in the corpus whose verdict tells the family reading from the
v0 letter — measured, restoring that letter refuses its four numeric
`eerste`/`laatste` operand positions and moves no other document's
verdict. It carries no `source.rgs`, and it could not: the frontend still
refuses a non-date operand at typing, so the widening is **wire-first**,
and the frontend's own doc comment still states the v0 letter it
enforces. That gap is a recorded flank, not a claim of agreement — the
precision half above is where the two DO agree.

**Timelines** carry *two different* empty rules, mirrored as found: the
per-period arithmetic of §5.1.4 applies the rule of each operator in the
table above per day — plus and min coalesce an empty period to 0 (ch. 3.4) —
while the temporal sum of §5.8.2 skips empty operands and yields an empty
period only when *all* operands are empty there (fallback clause applicable). Worked
example — in a period where A = leeg and B = 4: §5.1.4 A + B = 4; §5.8.2
som(A, B) = 4; in a period where both are leeg: §5.1.4 gives 0, §5.8.2
gives leeg (or the fallback). The two rules coexist in v0; ch. 3 names
them apart as the **period-coalescing rule** (ch. 3.4) and the
**contributor-skipping rule** (ch. 3.5).

**Assignment.** A writer that fires with a leeg value writes nothing and
decides nothing (ch. 3.11, 7.8): the target keeps what the fact base
holds — another writer's value, its input or leeg. A write of a value is
a real write when it changes state (the fixpoint machinery of ch. 5
compares old and new value). Worked example: target holds 5 from the
input, computed value leeg → no write, the target stays 5; computed value
7 → the target becomes 7. The v0 mirror wrote the computed leeg over the
input (5 became leeg); that erase is a tracked delta (`CONSTRUCTS.md` rows
8/18). The initialisation form writes only when the target
is empty (§9.6) — specified as a defeasible default in ch. 7
(`CONSTRUCTS.md` row 7).

### 1.3 The distilled invariant (v0)

> Empty coalesces to 0 **only**: in the additive and multiplicative
> value-combining operators (plus, min, maal, percentage-van), as the
> *right* operand of verminderd-met, as the *left* operand (dividend) of
> division, as a *duration* in date arithmetic, and per period in §5.1.4
> timeline arithmetic. Everywhere else empty propagates as empty or has a
> dedicated three-valued rule — never a silent 0. (The 0 of totaal and
> aantal over an empty *collection* is a different thing: the identity
> element of a dedicated rule over sets, not empty-*value* coalescing.)

### 1.4 Decided — the "empty = 0" design decision (PLAN §9, decision 7)

**Decided (maintainer, 2026-08-16):** the ch. 1.2 mirror, with the
ch. 1.3 coalesce whitelist, is frozen as the v1 semantics.
Empty-as-unknown (FEEL-style null propagation) is a non-goal. The
open/closed-world split is not expressed in operator behaviour:
legislation fills missing values explicitly through the Phase-4 default
layer (ch. 7, `CONSTRUCTS.md` row 7). And v1 routes division by a
genuine numeric zero into the §9.5 channel, uniform with division by
empty — a tracked delta against v0's hard fault. The inputs that were
on the table, kept as the decision's rationale:

1. **The must-not-be-zero list** (from the mirror itself): verminderd-met's
   left operand; an empty *divisor* under a non-empty dividend (leeg ÷
   leeg is 0 by the dividend rule, tested first); power, root, absolute value,
   rounding, clamping over empty; som/min/max/eerste/laatste over an
   all-empty source; every date-valued and duration-producing operand
   (except a duration operand itself); text coercion; the equality rules
   where empty carries meaning (leeg = leeg, leeg ≠ gevuld, the leeg
   predicate). Any v1 semantics must preserve these or track a delta.
2. **Empty-as-unknown alternative.** FEEL's null propagation is the
   reference three-valued model (`CONSTRUCTS.md` row 18); under it, most
   coalescing rows of ch. 1.2 would propagate instead. That changes
   observable outcomes (5 + leeg: 5 vs leeg) — a corpus-visible fork.
3. **The open/closed-world boundary** (concrete since the ASP
   hand-translation, `ASP-TRANSLATION.md` ch. 2/5): world facts want
   open-world treatment (unknown stays unknown — a foundling's parents),
   while organisational registers are closed-world (not asserted = not the
   case — a strandvondeling record, a proven contrary: the translation's
   input predicates). A single global "empty = 0" cannot
   express that split; a per-declaration open/closed marking on the model
   layer is a candidate design that would let ch. 1.2 rules vary by the
   declaration's register.
4. **Error-channel cases.** Division by empty and a negative radicand
   yield empty *plus* an observable §9.5 inconsistency (the Fout pattern —
   the value degrades, the error surfaces). Division by a genuine numeric
   zero is a hard runtime error in v0 (and a literal `1/0` is rejected at
   parse time). Decided: v1 routes numeric-zero division into the same
   inconsistency channel; the v0 hard fault and the parse-time rejection
   remain the mirror. **Executed 2026-10-19** (card
   `zero-division-channel`): a filled dividend divided by a numeric zero —
   `gedeeld door` and its abs variant alike — is leeg and deposits a §9.5
   fact of kind **EF-005** (ch. 5.7b) on the rule × subject that evaluates
   it, as a leeg divisor does with EF-031; an empty dividend still yields 0
   before the divisor is read (ch. 1.2). The kind kept its code; its
   witnesses moved from the red corpus to golden `inconsistent`
   expectations. Worked example: ch. 2.2.

---

## 2. Arithmetic and rounding

### 2.1 The exact-rational model

Numeric computation is exact rational arithmetic. A number is a fraction
`t/n` in lowest terms with `n > 0` (sign on the numerator; zero is `0/1`).
Equations:

```
a/b + c/d = (ad + cb) / bd        a/b − c/d = (ad − cb) / bd
a/b × c/d = ac / bd               (a/b) ÷ (c/d) = ad / bc   (c ≠ 0)
a/b = c/d  ⟺  ad = cb            a/b < c/d  ⟺  ad < cb
```

every result renormalised (gcd-reduced, sign-normalised). Comparisons are
exact cross-multiplications — so `1/3 × 3 = 1` is *waar*, never a
floating-point near-miss. A rational is *terminating* iff its reduced
denominator contains no prime factors other than 2 and 5; only then does it
have an exact decimal form.

**Collapse points** — the only places an exact rational becomes a decimal:

1. an explicit rounding (ch. 2.5), which rounds the *true* rational once;
2. the abs-division variant (ch. 2.2), defined to truncate toward zero at
   5 decimals.

The write boundary (ch. 2.4) is no collapse point: it tests that the exact
value fits the declared decimals of its target, faults where it does not
(EF-006), and writes a value that fits as the exact value it is. Worked
example (`ref/` test `WV-2.1-schrijfgrens`): 3/2 written into an attribute
of 1 decimal is **3/2**; the same 3/2 into a whole-number attribute is
EF-006; 2,00 into a whole-number attribute is **2**.

*v0 mirror notes:* v0 computes rational-free chains of +, −, × and the
square root at 34 significant decimal digits (IEEE 754-2008 decimal128);
the fractional-exponent power is computed at binary double precision.
These are approximations of the exact semantics above, which is normative:
+, − and × are exact on every number, however many digits it has, and a
square root and a power with a fractional exponent exist only under their
rounding, which rounds the true value once (ch. 2.2). An executor that
cannot compute such a value exactly refuses its form by name, as a limit of
its own; it does not approximate it. Worked example (`ref/` tests
`WV-2.1-exact-a` and `-b`, and the Drools backend's
`ExactArithmeticWitnessTest`): (10^31 + 0,001) − 10^31 is **0,001**, and
100000000000000001 × 100000000000000001 is
**10000000000000000200000000000000001**.

### 2.2 Operators

| Operator | Semantics | Worked examples (exact) |
|---|---|---|
| plus, min, maal | rational field operations | 9 × 1/3 = 3; 0,1 + 0,2 = 0,3 (exact decimals, no binary float) |
| verminderd met | subtraction; differs from min only in ch. 1.2 empty-handling | 5 vm 3 = 2 |
| gedeeld door | exact quotient; result may be non-terminating and stays exact until a collapse point | 10 ÷ 4 = 5/2 = 2,5; 2 ÷ 3 = 2/3 (exact) |
| gedeeld door (abs-variant) | quotient truncated **toward zero at 5 decimals** — a defined collapse point, dialect operator | 10 ÷abs 3 = 3,33333; −10 ÷abs 3 = −3,33333 |
| tot de macht (integer exponent) | exact integer power; negative exponent inverts first | 7¹⁹ = 11398895185373143 (exact); (1/3)⁻¹ = 3 |
| tot de macht (fractional exponent) | the exact root where the power is rational; else the real power exists only under its mandatory rounding (§6.7), which rounds the true power once; without a rounding it is a fault, and so is a negative base | 4^0,5 = 2; 8^(2/3) = 4; 10^(1/3) afgerond op 10 naar beneden = 2,1544346900, naar boven = 2,1544346901; 10^(1/3) without a rounding: fault |
| percentage van | `l/100 × r`, exactly | (1/3)% van 600 = 2 (exact, not 1,999…); 50% van 4,00 = 2 |
| wortel | principal square root; mandatory explicit rounding follows (§13.4.17.13), which rounds the true root once | √9 = 3; √2 afgerond op 2 (rekenkundig) = 1,41; √(1/9) afgerond op 20 naar beneden = 0,33333333333333333333 |
| absolute waarde | magnitude; type (unit, precision, sign) passes through | \|−3/7\| = 3/7 |
| datum ± duur (plus/min over a date and a duration) | **calendar arithmetic, not field arithmetic** — ch. 3.8: the count truncates toward zero, months/quarters/years clamp to the month end; the date stands on the left of plus and min (duration ± date is ill-formed) | 2024-01-31 + 2 weken = 2024-02-14; 2024-01-31 + 1 maand = 2024-02-29 |
| tijdsduur van A tot B (in hele *eenheid*) | whole declared units between two dates, signed; the packed-month rule of ch. 3.8; the absolute variant drops the sign | tijdsduur van 29-02-2004 tot 28-02-2026 in jaren = 21 |

A root and a fractional power under their rounding are rounded once, on the
true value, with whole numbers: with s = 10^d for d decimals and q the
denominator of the exponent, the rounded value is k/s for the whole k next
to the q-th root of x^p·s^q, decided by comparing k^q and, for the half-way
point of *rekenkundig*, (2k + 1)^q with that number. Worked examples (`ref/`
tests `WV-2.2-p` to `WV-2.2-v`): √2 afgerond op 40 naar beneden is
**1,4142135623730950488016887242096980785696**, and 2 tot de macht 0,5
afgerond op 20 richting nul is **1,41421356237309504880**.

Division by numeric zero: hard fault in v0; in v1 it routes into the
§9.5 channel (ch. 1.4 point 4). Worked example (`ref/` test
`ch2.2-div-zero`): 5 ÷ 0 is **leeg**, with exactly one §9.5 deposit, of
kind EF-005; 0 ÷ 0 and leeg ÷ 0 follow the dividend rule of ch. 1.2
instead — 0 ÷ 0 has a filled dividend and is leeg with the deposit, leeg ÷ 0
is 0 without one. A rational literal with denominator zero is rejected at
parse time.

A negative exponent inverts the power of its base, so a numeric zero to a
negative power is a division by a numeric zero (ch. 1.4 point 4): **leeg**,
with one §9.5 deposit of kind EF-005, for a whole and a fractional exponent
alike and under the rounding of a fractional one; a leeg base stays leeg
without a deposit (ch. 1.2). 0 to the power 0 is 1, and to a positive power
0, without a deposit. Worked example (`ref/` tests `ch2.2-pow-zero-negative`,
`-fraction`, `-rounded`, `ch2.2-pow-zero-zero` and `ch2.2-pow-zero-positive`):
0 tot de macht −1 is **leeg** with one deposit of kind EF-005, and so are
0 tot de macht −1/2 and that power afgerond op 2 decimalen naar beneden;
0 tot de macht 0 is **1**; 0 tot de macht 3 is **0**.

### 2.3 Result precision (static typing)

**The precision bound.** Every numeric expression has a bound: the most
decimals its value can carry, read from the declarations and the shape of the
expression, never from a `type` annotation (ch. 2.10). It is the typing ALEF,
the RegelSpraak toolchain of the Belastingdienst, applies, and it is a bound,
not a value: a quotient by an attribute has no finite bound although 100/4 is
25. "No finite bound" is written ∞; ∞ exceeds every number of decimals. The
bound of a value is a bound in a unit, the unit the value is expressed in, and
restating it into another unit multiplies by the exact factor of ch. 2.6.

**A factor.** A value of d decimals times an exact factor f has at most
`max(0, d + dec(f) − tens(f))` decimals, where dec(f) is the number of decimals
of f (∞ where its reduced denominator has a prime other than 2 and 5) and
tens(f) the number of factors 10 in its numerator: times 100 takes two decimals
off, times 1/1000 (m into km) adds three, times 1/3 gives ∞. A factor 0 gives 0.
This is the rule for a literal operand and for a unit conversion.

| Expression | Bound |
|---|---|
| attribute, parameter, cell (ch. 5.8c) | its declaration: 0 for a whole number, its decimals, ∞ for the unbounded form (IR-SPEC ch. 2.4) |
| numeric literal | the decimals of its exact value (∞ where it does not terminate) |
| plus, min, verminderd met | the higher of the operands, both restated into one unit: the unit of one of them, the one that gives the smaller bound (ALEF restates both sides into their common unit) |
| maal | the sum of the operands; with a literal operand, the factor rule over the other operand; with a unit restatement (ch. 2.6, rule 2), its factor as well |
| gedeeld door | ∞; by a literal other than 0, the factor rule with its reciprocal (÷ 2 adds one decimal, ÷ 4 two, ÷ 3 gives ∞) |
| gedeeld door (abs-variant) | 5 |
| percentage van | left + 2 + right (the left operand is divided by 100); with a literal left operand L, the factor rule over the right operand with L/100; with a literal right operand R, the factor rule over left + 2 with R |
| tot de macht, wortel, tijdsevenredig deel | ∞ (the language demands a rounding after a power and a root) |
| minimum, maximum, eerste, laatste, som, een begrenzing, the branches of a conditional | the highest of the operands (a fallback and a bound included), restated into one unit as for plus |
| totaal over a timeline | its operand, restated where the period of the rate is not the grid's own unit (ch. 2.6, rule 8); ∞ where the calendar integrates (ch. 3.6) — a rate per month, quarter or year over a grid of days or weeks, or per week over a grid of months, quarters or years — and the operand's own where a rate per day lies over such a grid, which weighs it by whole days; ∞ as well where the total can count a part of a period of its grid (below) |
| aantal, tijdsduur, a date part | 0 |
| absolute waarde | its operand |
| afgerond op n | n |
| a binder (Let, match arm, function parameter) | the bound of the value it holds; a match payload, a struct field: its declaration |

Examples (the verifier's negatives `corpus/negative/wf023-precision-*`, with the
frontend's refusals of the same sources, `PrecisionGateTest`): a whole opbrengst
gedeeld door 4 has bound 2, gedeeld door 3 ∞; 21% van a whole opbrengst has bound
2, 20% van it 1 (20/100 = 1/5); a percentage of 1 decimal van a taxatie of 2 has
bound 5; the maximale waarde of a taxatie of 2 decimals and a whole opbrengst has
bound 2; a taxatie of 2 decimals maal 100 has bound 0; "2 jr plus zijn basisduur"
over a whole basisduur in mnd, with `jr = 12 mnd`, has bound 0 in mnd (in jr it
would be ∞, b/12); the anchor's total of a dagrente in `€/jr` over a day grid
during the time it is rentedragend has bound ∞, the calendar weighing a day by
1/365 or 1/366, and is admitted into its target of 2 decimals through its
rounding (`corpus/negative/wf023-precision-calendar-total` is the same total
without it).

**A part of a period.** Outside the calendar, the temporal total measures a
segment in periods of its grid, a day counting 1/len of the period that holds
it (ch. 3.6), so a segment that holds a part of a period gives a share no
number of decimals bounds: 100 over 3 days of a storm year of 366 days and 2 of
one of 365 is 6090/4453. A total that can count such a part has bound ∞. It
cannot where every segment it totals lies on walls of its grid, and that is
read from the document in three cases:

1. the grid is the day grid, one day per period: every bound of a segment is
   a day;
2. the total has no clip and totals a timeline no rule and no defaults group
   writes: the case puts every bound of a segment of such a timeline on its
   declared grid (`uchar-case-v0`), and the asOfDate bucket is one period of
   it; the filter of the per-element fold (ch. 3.6, §7.1) selects members and
   is no clip;
3. as in case 2, but with a clip by a comparison (no whole-period qualifier)
   of an own time-dependent attribute that no rule and no defaults group
   writes, both timelines lying on the calendar grid of one period (no start
   point, no named timeline), the compared one's on a grid that fits the
   grid of the total (the fit of WF-126: every wall of the compared grid a
   wall of the grid of the total): the clip cuts where the compared timeline
   or the totalled one changes, on walls only.

Every other total counts as one that can split a period: a clip by a
characteristic, which holds on periods of days whatever its granularity, or by
a composite of characteristics; a clip by a comparison of a timeline on a grid
that does not fit, on a grid of its own start point, or that a rule writes; and
the total of a timeline a rule writes, which the rule cuts where its condition
and its operands cut it. A writer or a clip that cuts on walls in some other way
is not looked for. A clip that is an `Unresolved` makes no claim, as a value
whose bound nothing declares (ch. 2.4, point 4). Examples (the verifier's negatives
`corpus/negative/wf023-precision-partial-unit-*`, with the frontend's refusals
of the same sources, `PrecisionGateTest`): a whole stormtoelage per storm year
totalled over the weeks a Jutter is wachtplichtig, into a whole target, is
refused (∞ > 0) and admitted through "rekenkundig afgerond op 0 decimalen"
(`corpus/golden/jutterswet-vast-startpunt` rounds it to 2 decimals); without
the clip it keeps bound 0; the total of a monthly wachttoelage a rule writes as
30 on the days a Jutter is wachtplichtig is refused, whichever of the two rules
stands first; a monthly wachtvergoeding totalled while a monthly or a yearly
peilstand of the case exceeds 10 keeps bound 0 (as in
`corpus/golden/jutterswet-peilwacht`, which compares a monthly peilstand with a
drempelstand), and the same clip by a daily peilstand or by a peilstand a rule
writes has bound ∞, as has a yearly jaarpremie clipped by a peilstand per storm
year from 1 October. A whole dagtarief in €/dg over a monthly grid, clipped by a
characteristic, keeps bound 0: the calendar counts it by whole days
(`ref/test/fixtures/partial-unit/whole-days`).

**The annotation.** The decimals a producer writes in the `type` of a composite
node are the v0 claim, not the bound: maxDec for plus, min and verminderd met,
the sum for maal, maxDec for gedeeld door and tot de macht, 0 for percentage van
and wortel, the least operand precision for a listed minimum or maximum.
WF-091 crosses that claim where it makes one (IR-SPEC ch. 4.1); no reader
computes with it (ch. 2.10), and the gate of ch. 2.4 reads the bound.

### 2.4 The assignment gate (§6.1.2)

**There is no implicit rounding anywhere.** Precision changes only through
an explicit rounding. At assignment:

1. Writing a value that does not fit the target's declared decimals —
   whose exact value times 10^decimals is no whole number — is an
   **evaluation fault**, EF-006 (ch. 5.7b), which stands in place of the
   write (ch. 5.7): the author must insert an explicit rounding. The value
   counts, not the precision its expression declares (ch. 2.3) and not the
   scale a decimal is written at. Examples: `1,5` into a whole-number
   attribute: EF-006; `1,5 afgerond op 0 decimalen` (rekenkundig) into the
   same attribute: passes, writes 2; `1,00` into it writes 1; "zijn
   opbrengst gedeeld door 2" writes 500 for an opbrengst of 1000 and faults
   for 1001, with the parameters place, value 1001/2 and decimals 0 (`ref/`
   tests `WV-2.4-a`, `WV-2.4-b` and `WV-2.4-i` to `WV-2.4-l`). The fault
   is a property of the evaluation: 1001/2 and 1000/2 have one type, so no
   rule on types decides it. It stands behind the static gate of point 4,
   which refuses the `1,5` and the quotient of these examples before they
   run; on a document the gate admits, the share of an allocation and a
   segment of a pro-rata write into a timeline reach it
   (`corpus/red/jutterswet-verdeling-past-niet`,
   `corpus/red/jutterswet-tijdsevenredig-past-niet`), and an executor holds it
   on every document it runs, the verifier being no part of a run.
2. A rounding changes the *facing* precision: `afgerond op n` makes the
   expression n-decimal (whole when n = 0).
3. An unbounded target (the "getal" precision form, `IR-SPEC.md` ch. 2.4)
   is exempt from the check.
4. **The precision gate.** A write whose value's bound (ch. 2.3), restated
   into the unit of the target, exceeds the target's declared decimals is
   ill-formed (WF-023, `IR-SPEC.md` ch. 3.1): the author writes it through an
   explicit rounding, or into the unbounded form. The bound guarantees the
   fit, so a value that would fit but whose bound does not — a quotient by
   an attribute that happens to divide — needs a rounding that changes
   nothing; an author who wants no rounding on the way writes the
   intermediate result into the unbounded form and rounds where it lands.
   The gate stands at a rule's target (a cell reads the declaration of its
   attribute), an object creation's attribute member, a defaults
   consequence and an allocation, whose share it bounds by the rounding of
   the allocation or else by the declaration of its pot: the division by
   the recipients is not typed (as in ALEF), so a share that does not fit
   stays the evaluation fault of point 1. A rule on a target with a timeline
   stands in the gate: the value of each segment is the value of the
   expression on its period (ch. 3.4), so the bound holds per segment (a
   quotient by a timeline into a whole timeline is refused,
   `corpus/negative/wf023-precision-timeline-target`). The pro-rata write of
   ch. 3.7 stands outside it: no executor carries a rounding around that
   write (ch. 2.8). A value whose bound nothing declares (an `Unresolved`, an
   unbound variable) defers, as WF-023 does. Examples (the frontend gives the same verdicts on
   the same sources, RGS-372 and RGS-370, and RGS-340 and RGS-371 for an
   allocation): "zijn opbrengst gedeeld door zijn aantal deelnemers" into a
   whole aandeel is refused (∞ > 0) and admitted with "rekenkundig afgerond
   op 0 decimalen"; "zijn opbrengst gedeeld door 2" into a whole target is
   refused (1 > 0) and admitted into one of 1 decimal; `1,5` into a whole
   attribute is refused and `1,5 afgerond op 0 decimalen` admitted; a sleep-
   lengte in whole metres into a whole sleepafstand in km is refused (3 > 0),
   a gemeten afstand of 1 decimal in km into whole metres admitted; an
   allocation of a pot of 2 decimals in equal shares over whole recipients
   is refused (2 > 0) and admitted with "afgerond op 0 decimalen naar
   beneden".

Worked example — the unbounded form (`whole: false`, `decimals: 0`, a
"getal" declared without decimals) has no ceiling, so the exact value lands:
3/2 written into it is **3/2**, and 1/3 is **1/3**, where the same 3/2 into
a whole-number attribute is the error of point 1. A case input on it is
read the same way: with `sleepafstand` unbounded and the input **1/6**, the
rules "de dubbele sleepafstand = zijn sleepafstand maal 2" and "de gedeelde
sleepafstand = zijn sleepafstand gedeeld door 4", both on unbounded
attributes, write **1/3** and **1/24**; with the input 1,5 they write **3**
and **3/8**. (Measured 2026-09-26: until then the reference interpreter
refused both, the write as a value that "does not fit the declared 0
decimals" and the input as one that "does not fit 0 decimals", against
point 3.) The ASP backend holds a number at the scale its declaration fixes,
and the unbounded form fixes none: it gives an attribute or parameter of
that form the scale of its precision bound (ch. 2.3), the widest bound of
the values written into it and of the values the case gives it, where that
bound is finite — with the input 1,5 the sleepafstand and the dubbele
sleepafstand are bound at 1 decimal and the gedeelde at 3, so both rules
play there — and holds the form as a whole number where it is not (the
input 1/6, a quotient by an attribute): there a whole value plays, and a
value that is not whole is refused by name, at translation for a case input
and by a named `viol/2` for a written result (`ASP-TRANSLATION.md` S2,
S19). The Drools backend holds the exact value of the unbounded form in
its working memory, in both branches, so that a reader and a condition
compute with the value the form wrote, and writes it onto the bean of the
attribute as a decimal: exact where it terminates, and at 16 significant
digits rounded half to even where it does not (the 1/3 above is
0,3333333333333333 on the bean). Its folds add and compare the exact
values, and a value of that form that an object creation assigns
stands exactly in its working memory too. It refuses by name a case value
written as a rational (`1/6`), and a time-proportional part into a timeline
of that form (ch. 3.7), since a segment of its timelines holds a decimal; a segment of the period arithmetic of ch. 3.4 into such a timeline whose value
does not terminate is a limit of that backend, which refuses the session by
name where it stands at the fixpoint (DROOLS-312).

Worked example — the exact value at rest (`ref/` test `WV-2.4-p`; the Drools
backend's `RationalAtRestWitnessTest`): a Jutter with `begrensde opbrengst`,
`volle opbrengst`, `strandinkomen`, `vrijgelaten inkomen` and
`vrijlatingsgrens` of 2 decimals and a `factor` of the unbounded form;
"De factor van een Jutter moet berekend worden als zijn begrensde opbrengst
gedeeld door zijn volle opbrengst", "Het vrijgelaten inkomen van een Jutter
moet berekend worden als zijn strandinkomen maal zijn factor" and "Een
Jutter is boven de grens indien zijn strandinkomen maal zijn factor groter
is dan zijn vrijlatingsgrens". With 240,00, 360,00, 6851,25 and 4567,50 the
factor is **2/3**, the vrijgelaten inkomen **4567,50**, and the Jutter is
**not** boven de grens: 6851,25 × 2/3 = 4567,50 exactly. On a factor held
at 16 digits, 0,6666666666666667, the product is 4567,500000000000228,
which faults at the gate of the vrijgelaten inkomen and is greater than the
grens.

On a timeline target the write boundary of point 1 holds per segment
(ch. 3.11).

Worked example — the exact value 2/3 into a 2-decimal attribute: with
`afgerond op 2 decimalen` rekenkundig → **0,67**; with `naar beneden` →
**0,66**. The rounding applies to the true rational, never to a
pre-truncated approximation.

*v0 mirror note (a delta):* v0 types a bare division at maxDec (ch. 2.3),
so an unrounded non-terminating quotient statically passes its type check,
and v0 then collapses it to 16 significant digits at the write. The
candidate rule that this note carried — a value that can be non-terminating
passes an explicit rounding before a bounded write — is the gate of point 4,
in the form ALEF types it: that form also refuses a quotient that
terminates and does not fit (÷ 2 into a whole target), and bounds the share
of an allocation by its pot rather than by its division.

### 2.5 Rounding

Five modes. A rounding always writes its mode: the RegelSpraak source
names it (§6.1.3), and the wire's `Rounding.mode` is required (IR-SPEC
ch. 4), so there is no default. No banker's rounding exists.

| Mode (RegelSpraak) | Meaning |
|---|---|
| naar beneden | toward −∞ (floor) |
| naar boven | toward +∞ (ceiling) |
| richting nul | toward zero (truncate) |
| weg van nul | away from zero |
| rekenkundig | half away from zero |

Worked tables — these are `ref/` tests verbatim. To **2 decimals**:

| value | naar beneden | naar boven | richting nul | weg van nul | rekenkundig |
|---|---|---|---|---|---|
| 2,345 | 2,34 | 2,35 | 2,34 | 2,35 | 2,35 |
| −2,345 | −2,35 | −2,34 | −2,34 | −2,35 | −2,35 |
| 2,355 | 2,35 | 2,36 | 2,35 | 2,36 | 2,36 |
| −2,355 | −2,36 | −2,35 | −2,35 | −2,36 | −2,36 |

To **0 decimals**:

| value | naar beneden | naar boven | richting nul | weg van nul | rekenkundig |
|---|---|---|---|---|---|
| 2,5 | 2 | 3 | 2 | 3 | 3 |
| −2,5 | −3 | −2 | −2 | −3 | −3 |
| 2,345 | 2 | 3 | 2 | 3 | 2 |
| −2,345 | −3 | −2 | −2 | −3 | −2 |

Note: *rekenkundig* breaks the half-tie away from zero on **both** signs
(−2,5 → −3); *naar beneden/boven* are sign-asymmetric; *richting nul/weg
van nul* are sign-symmetric.

### 2.6 Units in arithmetic

- **plus/min/verminderd met** require compatible units (`IR-SPEC.md`
  ch. 2.5); the right operand is restated into the left operand's unit
  with the exact num/den factors, which needs no rounding at all until a
  collapse point. Result carries the left unit.
- **Comparison across units** restates *both* sides by their exact
  canonical factors: `24 mnd = 2 jr` compares 24 = 2·12 — exact, never a
  truncated ratio.
- **maal, gedeeld door and the rest** follow from the factor list of a unit
  (`IR-SPEC.md` ch. 2.5, point 5), below.
- **percentage van** carries the right operand's unit (a percentage of a
  `€` value is a `€` value).

**The algebra.** A unit U is a finite, non-empty mapping from atoms to whole
exponents other than 0, written as the sorted list of ch. 2.5. An atom is the
record (`abbreviation`, `system`, `canonical`, `factorToCanonical`). The **class**
of an atom is its `canonical` under the ch. 2.1 identity, or its `abbreviation`
where it is bare (ch. 2.5, a bare unit is the canonical target of every unit that
names it). The **factor** f(a) is its `factorToCanonical` as a rational, 1 where it
is bare; half a conversion pair is WF-092 and a fault in an executor. The
**dimension** D(U) maps each class to the sum of its exponents, without zeros. The
**total factor** is F(U) = ∏ f(a)^e(a).

1. **Convertible.** U ~ V iff D(U) = D(V). The factor from U to V is F(U)/F(V),
   exact. Two units that are not convertible are a fault (WF-010, `incompatible
   units`).
2. **Times.** U × V, value v_U·v_V: begin with U, and for each factor (b, e) of V:
   where b is an atom of U, add e to its exponent; else, where an atom a of U has
   the class of b, restate into a, value × (f(b)/f(a))^e, and add e to the exponent
   of a; else add (b, e). An exponent 0 leaves the list, and a list that ends up
   empty is no unit. A unitless operand times U is U, value unchanged.
3. **Divided by.** U ÷ V is U × V⁻¹, all exponents of V negated. A unitless operand
   ÷ U is U⁻¹, and U ÷ a unitless operand is U.
4. **Plus, min, verminderd met.** U ~ V is required; the right operand is restated
   into the left unit, and the result carries the left unit.
5. **Comparison.** U ~ V is required; both sides are restated, exactly.
6. **Writing.** The unit of the value ~ the declared unit, restated into it; else a
   fault.
7. **Rounding, clamping.** The unit of the base. **Percentage van**: the right unit.
   **Power**: both operands unitless (WF-035). **Decimals**: Tabel 13.
8. **Fold over a timeline** (§4.20, ch. 3.6). The integration over the grid
   multiplies by the grid unit, and the negative factor of the class of the grid unit
   rises one power: the grid's own atom where the value's unit has it, else the first
   of its class, into which the grid unit restates. `€/mnd` over months is `€`,
   `€/dg^2` over days is `€/dg`, `€/m/mnd` over months is `€/m`, `€/mnd/jr` over months
   is `€/jr`, `€/jr/m` over months with `jr = 12 mnd` is `€/m` at 1/12 of the value per
   month. Between the day family and the month family of the calendar the factor of
   the denominator rises too, and the value takes the measure the calendar gives the
   period in that unit (ch. 3.6, the calendar): `€/jr` over days is `€`, a day weighing
   1/365 or 1/366. A value with neither is a fault that names both units, in every
   executor. A reader that claims the unit of a fold without a grid in
   hand takes the one negative factor, and a unit with more than one stands unchanged.
   Worked examples, absolute, `jutterswet-kadetarief`: 12 `€/m/mnd` over three months
   is 36 `€/m`.

The order of the list (WF-124 (d)) makes `m·s` and `s·m` one list, and the rule
that the right operand restates into the left makes the spelling of a result depend
only on what "left" already prescribes for an additive result. The value of a
product with a unit of the same class does not depend on the order of the operands;
its unit does (`2 cm × 3 m` is `600 cm^2`, `3 m × 2 cm` is `3/50 m^2`), and the two
are one quantity.

Worked examples, with `wk = 7 dg`, `cm = 1/100 m` and `jr = 12 mnd` (`ref/` tests
`WV-2.6-*`, exact):

| Expression | Value | Unit |
|---|---|---|
| 2 m × 3 m | 6 | `m^2` |
| 6 `m^2` in an `m^2` attribute | 6 | `m^2` |
| 6 `m^2` in an `m^2/s` attribute | fault | `incompatible units: m^2 vs m^2/s` |
| 2 cm × 3 m | 600 | `cm^2` (3 m = 300 cm, rule 2) |
| 600 `cm^2` in an `m^2` attribute | 3/50 | `m^2` (factor 1/10000) |
| 6 `m^2` ÷ 2 m | 3 | `m` |
| 6 `m^2` ÷ 2 cm | 300 | `m` (1/cm = 100/m) |
| 1 ÷ 4 `m^2` | 1/4 | `1/m^2` |
| 100 `€/jr` × 6 mnd | 50 | `€` (6 mnd = 1/2 jr) |
| 30 `€/dg` × 1 wk | 210 | `€` |
| 2 `1/dg` × 3 wk | 42 | none |
| 7 `1/wk` × 3 dg | 3 | none |
| 3 dg ÷ 1 wk | 3/7 | none |
| 5 `€/dg/m` × 2 dg × 3 m | 30 | `€` |
| 1 `€/jr` × 1 `kg/jr` | 1 | `kg·€/jr^2` |
| 1 `€/jr` × 1 m | 1 | `m·€/jr` |
| 6 € ÷ 2 `1/n` | 3 | `n·€` |
| 1 ÷ 2 `€/dg` | 1/2 | `dg/€` |
| 2 `m^2` = 20000 `cm^2` | true | — |
| 14 ÷ 7 dg | 2 | `1/dg` |
| 2 `1/dg` × 7 dg | 14 | none |
| 3 wk × 2 `1/dg` | 42 | none |
| 2 `1/dg` plus 7 `1/wk` | 3 | `1/dg` |
| 1 `1/dg` × 3 € | 3 | `€/dg` |
| 3 € × 1 `1/dg` | 3 | `€/dg` |
| 1 ÷ 2 `1/dg` | 1/2 | `dg` |
| 10 `€/mnd^2` totalled over twelve months | 120 | `€/mnd` |

A write crosses the unit of the value into the declared one, so `ref` names them in
that order: the third row, written by a rule, reports `m^2/s vs m^2`.

2 `1/dg` = 14 `1/wk` holds; 5/2 `1/dg` > 14 `1/wk` holds, compared as
17,5 against 14 — where the bare numbers would answer the other way.
`1/dg` against `dg` and against `€/dg` is a unit mismatch, and so is
`m^2` against `m` or `m^2/s`.

### 2.7 Clamping (begrensd)

`begrensd(x, min, max)`: if x < min then min, else if x > max then max,
else x — minimum applied first, comparisons exact, an absent bound is no
bound. The three operands are **evaluated** in the same direction the
bounds are applied in — `base`, `minimum`, `maximum` — which is
ch. 5.11's wire-order rule and is observable whenever a bound faults or
deposits. A trailing rounding applies to the **whole** clamped result:
`(X met een minimum van 0) afgerond op 0 naar beneden` rounds the clamped
value. Example: begrensd(150, 0, 100) = 100; begrensd(−5, 0, 100) = 0;
(begrensd(2/3, 0, 1)) afgerond op 2 rekenkundig = 0,67.

### 2.8 Time-proportional part (tijdsevenredig deel)

Per granularity bucket: result = base × days-true/days-in-bucket, as an
exact fraction. Example: base 1200, 90 of 365 days → 1200 × 90/365 =
21600/73 — non-terminating, and rounded only by an explicit rounding:
afgerond op 2 rekenkundig = **295,89**. The assignment gate rounds nothing
and holds per segment of a timeline target (ch. 3.11): 21600/73 written
unrounded into a target of 2 decimals is EF-006. The static gate of ch. 2.4
point 4 leaves the pro-rata write into a timeline target alone, and no
executor carries a rounding around it (ref
faults on it, the ASP backend refuses it with CLINGO-024 and the Drools
backend with DROOLS-105, measured 2026-11-02), so an unrounded part into a
timeline of 2 decimals is well-formed (`anchor/jutterswet-borgdeel`) and the
fault per segment stands behind it (`corpus/red/jutterswet-tijdsevenredig-past-niet`);
into a scalar target of 2 decimals the part is refused (∞ > 2,
`corpus/negative/wf023-precision-time-proportional-part`). Full timeline
algebra: ch. 3.

### 2.9 Catala decimals — the unbounded form (Phase-5c)

*A v1 kernel decision (owner's decision 2026-11-02; the design of the
Catala route: `spec/proposals/PHASE5C-TUTORIAL.md` §5). Frontend mapping:
the Catala plugin.*

A Catala decimal (`TRat`) is an **exact rational** — precisely the
ch. 2.1 model — so its resolved operators
(`Add/Sub/Mult/Div_rat_rat`, `Minus_rat`) map onto the existing numeric
operators and its literals (`LRat`) carry the exact value in its shortest
decimal lexeme (`0.2`, `0.12345`, `3`); the casts
`ToRat_int`/`ToRat_mon` are a **pass-through** — the value is already
exact-rational, so the plugin emits the operand unchanged, with no cast
node of its own, and the result type is borne by the surrounding
mixed-decimals arithmetic — and no percentage (a `Number` whose unit is `%`) is involved (Catala
pre-divides — `20%` arrives as the rational `1/5`). A decimal fixes no
number of decimals, so the frontend maps `TRat` to the **unbounded form**
(IR-SPEC ch. 2.4 (c)): `Number{whole = false, decimals = 0, sign
unspecified}` without a unit.

`ref` stays exact-rational throughout. The precision gate of ch. 2.4
point 4 sets no ceiling on the unbounded form, so a Catala quotient into a
decimal — by a literal or by a runtime value — is well-formed without the
rounding a Catala source has no means to write: the gate holds for the
Catala route as for every frontend. The ASP backend holds a decimal at the
scale of its precision bound (ch. 2.3) where the bound of every value
written into it and of every value the case gives it is finite, and as a
whole number where it is not (`ASP-TRANSLATION.md` S2): a quotient by a
runtime value has no finite bound, so there a value that is not whole is
refused by name, a limit of that backend.

Worked example (`ref/` tests `WV-2.9-tarief-*`, `WV-2.9-aanslag-*` and
`WV-2.9-quotient-by-input*`; golden `jutterswet-tariefrecht-catala`): the
tarief is a decimal whose defaults write 20%, 15%, 10% and 0%. With an
inkomen of 20000,00 and a kindertal of 1 the tarief is **1/5** and the
aanslag — 20000,00 × 1/5 rounded to the cent, less a kinderaftrek of
250,00 — is **3750,00**; with a kindertal of 4 the tarief is **1/10** and
the aanslag **1000,00**. The four consequences are bound at 1, 2, 1 and 0
decimals, so the ASP backend holds the tarief at 2 decimals (1/5 as 20). A
decimal aandeel defined as `vangst / jutters` over two integer inputs is
verifier-clean without a rounding; with 7 and 2 it is **7/2**, which the
ASP backend refuses by name
(`frontends/catala/test/division_runtime.catala_en`).

**The scale is a boundary, not a step (maintainer decision 2026-08-24,
vraag 3-A).** A declared scale — an attribute's own `decimals` — binds
only a *declaration* and the *write boundary* it
guards (ch. 2.4); every intermediate value stays the exact rational of
ch. 2.1 — a `Let` binder (ch. 10.6) and a `Match`-arm result
(ch. 9.2) among them, rounded to no scale — and a declared scale is never
itself a collapse point: those stay exactly the two of ch. 2.1 — an
explicit `Rounding` node (ch. 2.5) and the ch. 2.2 abs-division operator;
the ch. 2.4 write boundary tests the value and rounds nothing. Worked example: `100 ÷
3` (non-terminating) bound through a `Let` at a 2-decimal
declaration and rounded once at the endpoint — `afgerond op 2
rekenkundig` → **33,33**; the same binder `afgerond op 4` → **33,3333**,
never the 33,3300 a step-binding scale would have pre-collapsed; while
writing that binder unrounded into its bounded target faults (ch. 2.4).
`ref` realises this exactly; clingo materialises each step at its
declared scale and so must refuse this form loudly — a documented backend
limit conform to the norm, not a value split (`ASP-TRANSLATION.md` S19).

**Money × decimal rounds to the cent.** Catala's runtime rounds money
multiplication (`Mult_mon_rat`/`Mult_mon_int`) to the cent, so the
frontend wraps those products in `Rounding {halfAwayFromZero, 2}` — the same
wrapper the money *divisions* already carry (golden
`jutterswet-tariefrecht-catala`). The ASP encoding splits the product of a
2-decimal money and a rate of up to four decimals so the intermediate stays
inside the int31 fences — `M × R = M × R_hi/10² + M × R_lo/10⁴` at the
rate's scale 10⁴, each part within the existing bound, the sum exact
(`ASP-TRANSLATION.md` S18(c)).

### 2.10 The consumer clause — where a type annotation is not the source

A v0 node carried its result type on the wire at every position. At a
**reference** position that type was a *copy* of a declaration; at a
**composite** position it is a *derivation* over the node's operands.
Neither is a source. Since L2 (2026-09-24, B13) the copy is gone: the five
reference classes carry no type of their own, so at a reference position
there is no annotation to read and the declaration is the one source. At a
composite position the annotation stays on the wire, and the consumer must
not read it. The clause is scoped, not absolute: there are **named positions** — three where
the annotation genuinely *is* the source, and a fourth where v0 states no
rule to put in its place — and they are listed below rather than left to a
consumer's discretion. Everywhere else a consumer of the IR derives the
unit, the soort and the precision it computes with:

1. **Reference positions** — an `AttributeReference` (bare, navigated, or
   the one a rule names as its `target`), a `ParameterReference`, a
   `DimensionCellRead`, a `BinderRef` — take unit, soort and precision from the
   **declaration** the reference names: the object type's declared
   attribute datatype, the parameter's declared datatype, the attribute's
   element datatype, the binder's declaration (a `Let`'s value, a match
   arm's payload, a lambda or function parameter). Since L2 that is the
   only place they can take it from: the wire carries no type on these
   nodes, nor on a `VariableReference` (`IR-SPEC.md` ch. 4.1), and a loader
   refuses one by name. A `VariableReference` names no declaration: it is the
   marker for a variable the resolver could not bind (`IR-SPEC.md`
   ch. 4.1), so there is no type to take — the verifier assigns it none,
   and `ref` faults on reaching it. Until
   L2 each carried a copy, which WF-091's reference arms held faithful,
   so on a verifier-clean document the two agreed — which is exactly why
   reading the copy was *invisibly* wrong when they did not, and why the
   copy left the wire rather than stay crossed.
2. **Composite positions** — an arithmetic node, a `Let`, a value
   aggregation, a rounding, a clamp — take the **effective** type computed
   from those declarations through the ch. 2.3 precision table and the
   ch. 2.6 unit rules, never the node's own annotation. An additive result
   carries the left operand's unit and a unitless side adopts the other; a
   product and a quotient of two units carry what rules 2 and 3 of ch. 2.6 derive
   ("€/jr maal jr = €", "€ maal kg = kg·€", "1 ÷ dg" the reciprocal, "€ ÷ €" no
   unit); a `Let` carries its body's; and a binder read carries
   what its binding derived, unit-bearing **or unitless** — bound to a
   unitless value is not the same fact as unbound (until L2 only the
   second fell back to the binder reference's own annotation; since L2 a
   `BinderRef` carries none, and an unbound one is ill-formed,
   `IR-SPEC.md` WF-064); a value aggregation carries its first
   operand's (§4.6 *Eenheid1*), also where no member contributes: the 0 of a
   total and a unitless fallback of a som carry it, and so does the 0 of a
   temporal total without a contributing segment; a percentage-of carries its
   right operand's (ch. 2.6, rule 7); a rounding and a clamp carry their basis's.
   Where v0 states no join rule at all, the **fourth class** below applies.

**Three positions where the annotation IS the source**, and a consumer
reads it there:

- a **`Literal`**: its type is its own value's carrier (`{"number": "3.5"}`
  in EUR), a copy of no declaration;
- the **duration operand of a date ± duration**: the calendar unit that
  routes the arithmetic is that operand's static type (`IR-SPEC.md`
  WF-035);
- a **`Tijdsduur` / `Absolute tijdsduur` node's own type**: its unit is the
  operator's parameter — "de tijdsduur van A tot B **in jaren**"
  (WF-038) — not a derived result.

Since 2026-09-19 the second and third of those carry **more** than a
calendar unit: where the spelling names a declared day kind rather than a
unit on the ch. 3.8 table, it selects the PREDICATE the duration walks
(ch. 3.8, the day-kind forms). The exemption is therefore stronger at a
day-kind position than at a calendar one — twisting the annotation there
would not merely restate a number, it would ask a different question — and
that is why gate 13 leaves both positions untwisted for a reason it already
carried.

**A fourth class, and it says something weaker.** A *position* whose result
unit v0 does not *derive* at all — a power
(`power`), an absolute value, a
root, the date/time extractors — keeps its own annotation, because clause 2
has nothing to put in its place. That is not the annotation being the
source; it is v0 having no rule, and the two consumers answer it
differently while both stay conform: the ASP translator's `eff_unit`
returns `None` at those positions and its fences (`unit_free_ok`,
`units_identical_ok`) then *fault* rather than compute with a unit it could
not derive, whereas the Drools backend has no such fence and computes with
the annotation — the behaviour it had before this clause, deliberately
unchanged. **Zero** unit-bearing annotations in the
corpus sit at such a position, so the class is empty today and the
difference between the two answers is unobservable. The clamp that reports
the first witness is step 6 of `scripts/check-reftype-derivation.sh`. Note
that this class is a set of *positions*, not of node kinds: `power`
and `plus` are the same node kind (`Binary`), and only one of them derives.
The clamp therefore keys on **(node, op)** for a `Binary` and on
**(node, kind)** for an `Aggregation` or a `BuiltinCall` (one `Functie` until L4), and it separates the two arrivals it can
see — a fourth-class *branch* of a node kind the twist table otherwise
carries, and a node kind outside the table altogether — each failing the
gate with the position class and the golden named. Keying on the node alone
would twist a unit-bearing `dividedBy` as though it sat at a derived
position and report the result as a consumer defect, which is a wrong
diagnosis of a real gap. What the witness must then buy is a derivation
rule (or a fence) — never a wider licence to read the wire.

Two further readings are recorded rather than blessed, because they are a
backend's encoding rather than a semantic claim: the ASP translator sizes
its integer scale from a `Fold`'s and a `Match`'s own annotated `decimals`
(`value_scale`, the scaled-integer encoding of `ASP-TRANSLATION.md` S2 and
the `expr_term` triple of S4 — *not* S19, which is the not-translated
inventory). They stay legitimate only while that scale is
an encoding detail; the moment a unit-bearing witness reaches those nodes
they fall under clause 2.

**The enforcing artifact** is `scripts/check-reftype-derivation.sh`: it
grafts a systematic unit lie onto every twistable annotation of a corpus
golden — leaving every declaration byte-identical — and demands that `ref`,
clingo and the Drools backend each reproduce, on the lying copy, exactly
the outcome they produce on the untouched one. Its reason-carrying survivor
list is empty: measured 2026-09-04, all three are indifferent over 46 unit
annotations in ten goldens. The harness twists the **unit** facet only, and
says at its head why the other two facets are not machine-checked this way:
a soort lie is a different document (WF-091 crosses it in the verifier at
the derived positions, and `ref` legitimately routes ch. 1.2d and date
arithmetic on the static soort), and a precision lie collides with the ASP
integer encoding above. Since L2 the reference positions carry no
annotation to twist, so the harness twists the derived positions alone.

Soort and precision are no less normative here, but the two clauses hold
them in different ways, and the difference is not a detail. **Clause 1**
(reference positions) cannot be lied about since L2: there is no copy on
the wire. Until L2 it was crossed in full — WF-091's identity engine
compared soort, domain, unit, decimals, wholeness and sign against the
declaration, facet by facet, so a lie there was a *rejected document*
rather than a divergent answer. **Clause 2** (composite positions) is, since the
derivation law landed (2026-09-04), derived and machine-checked for
soort, domain, unit, decimals and wholeness at eleven composite
positions — `IR-SPEC.md` WF-091's DERIVED ARMS, each crossing the
annotation against the type derived from the operands per that
clause's table. Two priced exclusions stay open there, measured and
recorded in the clause itself: the SIGN is crossed at no derived
position (documents may narrow it truthfully; crossing it turns seven
golden+red documents red), and the multiplicative precision follows
the stated ch. 2.3 formula only in prose (crossing it costs eleven
documents across three producers with three precision policies). The
fourth class — **positions** whose result unit v0 does not derive —
stays uncrossed and named; positions, not node kinds, for the reason
that class already gives above (`power` and `plus` are one node
kind and only one of them derives, which is why the enforcing clamp
keys on **(node, op)**), and WF-091's honesty note 4 is now a coverage
note rather than a deferral. The element-passing aggregation keeps its
domain-only cross under WF-089/WF-090 — once per operand, the domain
being the one facet a pass-through fold hands on unchanged — and since
the derivation law landed the same position also carries **soort, unit,
decimals and wholeness**, crossed once against the derivation by
WF-091's `first`/`last` (eerste/laatste) and `minimum`/`maximum` arms rather than
once per operand.

*History:* this clause is the consumer half of the WF-091 work. Before it,
the Drools backend read the wire annotation at its write boundary, its
additive operand boundary, its `Let` cluster and its aggregation result
type, and answered **3.750,00** where `ref` and clingo answered **3,75** on
a unit-lying aggregation result — a divergence the verifier could not see,
because a derived type has no single declaration to be crossed against.
It could not until the derivation law landed (2026-09-04): that shape is
`wf091-fold-maximum-unit`'s negative today, refused at the fold root on
the unit facet.

---

## 3. Timeline algebra

A time-dependent attribute, characteristic or parameter does not hold one
value; it holds a **timeline**: a function from days to values that is
constant on each of finitely many periods. This chapter defines the
timeline value and its grids, states the one principle by which an operator
or a condition without a time semantics of its own reads timelines — the
scalar rule, applied on each day — and defines the operations that have a
time semantics of their own. It is organised by the role of each section:

| Part | Sections |
|---|---|
| the value | ch. 3.1 the timeline value, `read`, open bounds, the canonical form |
| grids and the calendar | ch. 3.2 grids, buckets, start points, the weighting and the calendar |
| the principle | ch. 3.3 the reading per day, the periods of a condition, the clip, the scalar contexts |
| producers of a timeline | ch. 3.4 arithmetic per day; ch. 3.5 the sum per period; ch. 3.7 the time-proportional part; ch. 3.8 the day count, with the calendar arithmetic of dates it rests on; ch. 3.10 a condition per period, the period window, and the characteristic a condition writes |
| collapses to a scalar | ch. 3.6 the temporal total; ch. 3.9 the whole-period qualifier |
| writing | ch. 3.11 the write of a timeline: a writer's own days beside the input, the write boundary per segment, leeg, two writers, what 0 means |
| folds over linked instances | ch. 3.4a–3.4d and ch. 3.9a: the scalar folds over a set of instances, which ch. 3.5, 3.6 and 3.9 apply per member |
| open decisions | ch. 3.12 |

Two empty rules run through the algebra, and ch. 1.2 records both: the
**period-coalescing rule** of the arithmetic operators, read per day
(§5.1.4, ch. 3.4), and the **contributor-skipping rule** of the sum (§5.8.2,
ch. 3.5). Where the RegelSpraak specification is silent about time, the
reading is ALEF's, the RegelSpraak toolchain of the Belastingdienst; the
grounds of each reading are in `spec/proposals/IMPLICIT-TIME.md`, and every
reading that departs from the mirrored v0 runtime is a tracked delta in
`spec/CONSTRUCTS.md` row 11 (row 29 for the whole-period qualifier).

### 3.1 The timeline value

A timeline divides **all time**: it is a finite set of **segments**, and
every day reads one value. A segment is a half-open interval of days
`[van, tot)` — `van` inclusive, `tot` exclusive — that carries one value of
the member's element type, or carries **leeg**. A segment may have an
**open bound**: an open `van`, written −∞, lies before every day, and an
open `tot`, written +∞, lies after every day. Only the first segment can
have an open `van` and only the last an open `tot`; one segment may have
both. A day that no segment covers (a **gap**, the days before the first
segment and after the last included) reads leeg: a gap is a leeg segment.
The reading function:

```
read(tl, d) = the value of the segment with van ≤ d < tot,  if one exists
            = leeg,                                          otherwise
```

where −∞ ≤ d and d < +∞ hold for every day d. A **scalar** read as a
timeline is the one segment (−∞, +∞) that carries it, and a leeg scalar is
the empty timeline: a scalar holds on every day (§5.1.1, §5.1.4.1).

- A **boolean timeline** — a time-dependent characteristic — is its true
  periods: the segments are the periods in which the characteristic holds,
  and a day outside them reads false. Its segments carry no value. The true
  periods of a characteristic carry no open bound: a case supplies none
  (`characteristicTimelines`), and a rule whose condition would give it one
  is refused with EF-023, a form this chapter leaves without a meaning
  (ch. 3.10, 3.12).
- Granularity and period count are properties of the **declaration** (the
  `TimelineFacet`, `IR-SPEC.md` ch. 2.2), never of the value: a segment
  does not know its grid.
- A well-formed timeline value is sorted ascending by `van` and its
  segments do not overlap. Overlap and disorder in timeline data are
  ill-formed, a rule of the data plane, and `read` never depends on the
  order of the segments. A zero-length or reversed interval (`tot` ≤ `van`)
  contributes nothing anywhere in this chapter. *v0 mirror note:* the v0
  producers emit sorted segments without overlap, while v0's reads accept
  unsorted and overlapping input (the first covering segment wins); the
  rule above is a tracked delta.
- A gap and an explicit leeg segment are two representations of "leeg
  here", and they are **observationally equivalent** under `read`. The
  semantics is defined over `read`, so no value-producing operation in this
  chapter distinguishes the two, and whether a producer writes an explicit
  leeg segment is representation. The one comparison that sees the
  representation is the change-detection rule below, which compares segment
  lists; that makes it a rule of the data plane, not of the semantics. The
  case format (`uchar-case-v0`) writes filled segments only, and leaves out
  `from` or `to` for an open bound.
- The **canonical form** of a timeline is its maximal runs of equal values
  without the gaps: every leeg segment and every zero-length or reversed one
  is dropped, then two adjacent segments `[a, b)` and `[b, c)` with equal
  values merge into `[a, c)` until no two do; an open bound stays open.
  Values are equal as in the change-detection rule (numeric equality for a
  number, so 12 and 12,00 are one run). Two well-formed timelines read alike
  on every day exactly when their canonical forms are equal, so how a
  producer divides a run of equal values into segments is representation,
  as the choice between a gap and an explicit leeg segment is. An expected
  timeline of a case (`uchar-case-v0`, `attributeTimeline`) is compared with
  the timeline the evaluation gives in this form: both sides canonical, then
  segment by segment, the bounds exactly and the values by that equality.
- An open bound is a bound of the value and lies on no wall of any grid. A
  fold that walks the buckets or the whole units of a period (ch. 3.7, 3.8)
  has no end to reach in an open segment, and such a walk is refused by
  name; the temporal total has a rule of its own (ch. 3.6). Every bucket of
  the whole-period qualifier (ch. 3.9) that lies inside an open segment reads
  the segment's one value on each of its days, so all those buckets give one
  verdict.
- **Segment-wise equality** — the same `van`, the same `tot`, and values
  that are equal or both leeg (numeric equality for a number; the structural
  equality of ch. 9.3 for a constructor value) — is the change-detection
  rule of a timeline write, the counterpart of the scalar rule of ch. 1.2,
  Assignment (the evaluation model: ch. 5).

Worked example (`WV-3.1-canonical`; `ref/` tests verbatim): 0 on
(−∞, 2024-01-01) and on [2024-01-01, 2024-02-01), 12 on
[2024-02-01, 2024-03-01), 12,00 on [2024-03-01, 2024-04-01), an explicit
leeg segment [2024-04-01, 2024-05-01) and 12 on [2024-05-01, +∞) have the
canonical form (−∞, 2024-02-01) = 0, [2024-02-01, 2024-04-01) = 12,
[2024-05-01, +∞) = 12: April reads leeg, so the two runs of 12 stay apart.
The suite cases `ref/test/fixtures/canonical-timeline` expect runs divided
otherwise than their producer writes them: the twelve months of a
time-proportional part as one segment (ch. 3.7), two counted buckets of a
day count as one and an open run of 0 as two (ch. 3.8), and a segment and
both open runs of period arithmetic as two each (ch. 3.4).

### 3.2 Period grids, buckets, and start points

A member's timeline lies on a **grid**: a sequence of periods of one
granularity — `day`, `week`, `month`, `quarter` or `year` — and `count`
units each. The grid is the **calendar grid** unless the timeline fixes a
start point (below, fixed starting points) or takes one per object (below,
variable starting points). The grid serves two jobs.

1. **Weighting.** The temporal total (ch. 3.6) measures a span in units of
   the grid: a day counts 1/len of the period of the grid that holds it, len
   the length of that period in days, so a period of the grid counts one
   unit and a part of one counts its share of the days. On the calendar the
   period is the calendar day, month, quarter or year, and a week counts
   seven days wherever it starts; a fixed start point lays the periods of
   its own grid. All five granularities weight. A period between clamped
   walls is therefore one unit. Counted from the start of a span that starts
   on a wall, the whole units are the same periods: [van, tot) holds k whole
   months for the greatest k with van + k months ≤ tot, the month step
   clamping to the last valid day of the target month (ch. 3.8), k ÷ 3 whole
   quarters and k ÷ 12 whole years; a week is seven days. This is the
   date-plus-unit step, not the packed-month rule by which ch. 3.8 counts a
   duration, and the two differ exactly where the end is a clamped day.
2. **Bucketing.** A producer that writes one value per calendar period
   (ch. 3.7, 3.8, 3.9) uses walls aligned to the calendar: a month bucket
   runs from the first of a month to the first of the next, a year bucket
   from 1 January to 1 January. Only **month and year buckets** exist on the
   calendar: the source language's bucketing operators are restricted to
   "de maand" and "het jaar". A calendar bucket of a day, a week or a
   quarter is not defined (ch. 3.12): a member declared on the calendar's
   day, week or quarter grid has no bucket, and every reader of such a
   bucket refuses it. A case timeline on the day grid needs none: every day
   lies on that grid, so its segment bounds are checked against nothing,
   and its total weighs its days (ch. 3.6). A fixed start point lays periods
   of its own on a week, month, quarter or year grid, and they are read
   where a bucket is read.

A bucket has its actual calendar length, leap-aware: February 2024 has 29
days, 2025 has 365. There is no 360-day convention anywhere.

Worked examples of the weighting (absolute; `ref/` tests verbatim):
[2024-01-31, 2024-02-29) holds **1** whole month, where the duration from
31-01-2024 to 29-02-2024 is **0** whole months (ch. 3.8, "Clamp, not
inverse"); [2024-11-30, 2025-02-28) holds **1** whole quarter;
[2024-02-29, 2025-02-28) holds **1** whole year; [2024-01-31, 2024-02-28)
holds **0** whole months. Measured on the grid, the same
[2024-01-31, 2024-02-29) is **1** period of the `--01-31` month grid, and on
the calendar's month grid the last day of January and 28 days of February,
**1/31 + 28/29** of a month.

**The calendar.** The time units fall into two families by the spelling
table of ch. 3.8: the **day family**, dag and week, and the **month
family**, maand, kwartaal and jaar. Within a family a factor is exact, and
the factor the document declares governs (`jr = 12 mnd`, `wk = 7 dg`).
Between the two families no factor is exact, because a month and a year
have no fixed number of days (§7.3.1). A span measured in a unit of the
other family than its grid's is measured by the **actual length of the
calendar unit**: a day counts 1 dg and 1/7 wk, and 1/len of the calendar
month, quarter or year that holds it, len its length in days (28 to 31, 90
to 92, 365 or 366). A factor that a document declares between the two
families (`jr = 365 dg`, `mnd = 30 dg`) would stand in for the calendar and
is ill-formed (WF-009, `IR-SPEC.md` ch. 2.5; negative
`wf009-calendar-family-factor`); an executor that meets one refuses it by
name. A unit outside the spelling
table, such as a `bm` declared canonically as a month, belongs to neither
family and keeps the factor it declares (ch. 3.12). The temporal total
integrates by this measure (ch. 3.6).

**Multi-period grids** (`count` ≥ 2, "voor elke 3 maanden") are declared and
carried in the model layer (WF-005), and **no operation over them is
defined**: every read of and write to a multi-period timeline is refused,
because the source language leaves open how a value spreads over a period
of N units (ch. 3.12). A parameter's facet has `count` 1 (WF-005,
`IR-SPEC.md` ch. 2.10).

**Variable starting points** (§9.9). A timeline with a variable start lays
its periods per object from its **anchor**: the value of the synthetic
day-precision date attribute that the start-point determination names
(`IR-SPEC.md` ch. 3.8) and that an ordinary rule writes. The anchored
periods are

```
[ anchor + k·unit,  anchor + (k+1)·unit )      for every integer k
```

Periods exist before the anchor as well as after it, and **every wall is
computed from the anchor directly**, never by stepping from the previous
wall, so a month-end anchor clamps the same way at every wall. Worked
example: anchor 2024-01-31, month grid → walls 2024-01-31, 2024-02-29 (the
leap clamp), 2024-03-31, 2024-04-30, …; the third wall is the 31st again,
which a step from the clamped 29th would have lost. The date-plus-unit step
clamps to the last valid day of the target month (ch. 3.8).

A missing start-point determination is ill-formed (WF-016/WF-032). An anchor
that is leeg at evaluation gives the empty timeline for every anchored
producer, and the anchor rule's own §6.8 error reaches the §9.5 channel.

**Fixed starting points** (§13.3.6.3–.7). A timeline may fix the point its
periods start from (`IR-SPEC.md` ch. 2.8, `fixedStart`, WF-115), on its
declaration or on a member's timeline facet. Its grid is the sequence of
periods of `count` units from that point and the same sequence back in time,
with every wall computed from the start point directly, as from a variable
anchor:

```
date start A (yyyy-mm-dd):     [ A + k·count·unit,  A + (k+1)·count·unit )   for every integer k
day-month start --mm-dd:       [ D + k·unit,        D + (k+1)·unit )         for every integer k
```

`unit` is one day, seven days, or one, three or twelve months, the month
step clamping as in ch. 3.8; `D` is day `dd` of month `mm` in any year that
has that day (a leap year for `--02-29`), and every such year lays the same
walls. On a month grid the walls fall on day `dd` of every month, on a
quarter grid on day `dd` of month `mm` and of every third month from it, on
a year grid on day `dd` of month `mm`; a month shorter than `dd` takes its
last day. A timeline without a `fixedStart` and without a variable start
lies on the calendar grid.

The grid is read where the calendar grid is read, from the member's facet:
the bounds of a case-supplied timeline lie on its walls; a scalar case value
is the value of the period that holds the evaluation date (ch. 3.6), on a
week or quarter grid as on a month or year grid, since a fixed start point
lays a period there where the calendar lays none; and a day count into a
month or year target of `count` 1 buckets on the target's periods (ch. 3.8).
The whole-period qualifier (ch. 3.9) does not read it: its buckets are the
calendar month or year the qualifier names, whatever grid the tested member
declares. The time-proportional part (ch. 3.7) needs a base that is
constant across a bucket, which holds because the base's grid nests in the
target's; a start point off the calendar breaks that nesting, so the part
onto a target or from a base on a fixed or variable start point is not
defined, and is refused (ch. 3.12).

Worked examples (absolute; `ref/` tests verbatim):

- year grid `--10-01`: the period that holds 2025-02-01 is
  [2024-10-01, 2025-10-01), the one that holds 2024-09-30 is
  [2023-10-01, 2024-10-01);
- month grid `--01-31`: walls 2024-01-31, 2024-02-29, 2024-03-31,
  2024-04-30 — computed from the start point, the 31st returns in March;
- quarter grid `--05-31`: walls 2024-02-29, 2024-05-31, 2024-08-31,
  2024-11-30, 2025-02-28;
- year grid `--02-29`: walls 2024-02-29, 2025-02-28, 2026-02-28,
  2027-02-28, 2028-02-29;
- week grid `2026-01-05`: the period that holds 2026-01-04 is
  [2025-12-29, 2026-01-05), the one that holds 2026-01-05 is
  [2026-01-05, 2026-01-12);
- the day count "het aantal dagen in het jaar dat hij wachtplichtig is"
  into a target on the `--10-01` year grid, the characteristic true on
  [2024-09-28, 2024-10-03): **3** on [2023-10-01, 2024-10-01) and **2** on
  [2024-10-01, 2025-10-01); on the calendar grid the same count is 5 on
  [2024-01-01, 2025-01-01) (`corpus/golden/jutterswet-vast-startpunt`,
  case 1);
- "voor elk jaar startend op dag 15 van maand 10" declares the periods
  15 October – 15 October, written `--10-15`: a case input over
  [2024-10-15, 2025-10-15) lies on the grid, one over
  [2025-01-01, 2026-01-01) is refused as off it;
- the subject's own total over the asOfDate bucket, evaluation date
  2025-02-15: a member on the week grid `2024-09-30` with the scalar case
  value 7 reads the period [2025-02-10, 2025-02-17), one whole week →
  **7**; a member on the quarter grid `--05-31` with 30 reads
  [2024-11-30, 2025-02-28), one whole quarter → **30**; the calendar's week
  grid has no such period, and the total is refused.

*v0 mirror note:* v0 resolves the fixed start point in its model and has no
wire field for it; the wire field is a tracked addition.

### 3.3 The principle: the reading per day

**The rule.** For every operator and every condition without a time
semantics of its own, the meaning over timelines is the scalar meaning on
each day: the empty rules of ch. 1.2 per day, and the equation that a rule
is (ch. 5.1) per day (§5.1.1, §5.1.4, §8.4.1, §10.3). For a scalar
operation f over operands x₁ … xₙ,

```
read(f(x₁, …, xₙ), d)  =  f(read(x₁, d), …, read(xₙ, d))      for every day d
```

where an operand is a timeline, a scalar (the timeline (−∞, +∞) of
ch. 3.1), or a time-dependent parameter (its supplied timeline). The result
is constant between two consecutive bounds of the operands' segments, so it
is computed once per period of their **cut set** (ch. 3.4). The operations
with a time semantics of their own keep their own definition: the temporal
total (ch. 3.6), the time-proportional part (ch. 3.7), the day count
(ch. 3.8), the whole-period qualifier (ch. 3.9) and the period window
(ch. 3.10). Of the arithmetic operators of ch. 1.2 the power alone is not
read per day: a power over a timeline is refused (ch. 3.4).

**The periods of a condition.** A condition c read per day has a truth on
every day, and its **periods** are the days on which it holds, gathered into
maximal periods:

```
periods(c) = the maximal periods of { d : c holds on d }
```

Two adjacent periods in which c holds are one period, so periods(c) changes
only where c changes. Ch. 3.10 gives periods(c) for each form of condition
that reads a timeline.

**The clip.** A producer of a timeline is restricted to the periods of the
condition that governs it:

```
written(tl, c) = tl restricted to periods(c)
```

The restriction cuts segments and keeps their values: it is the restriction
of a function to a smaller domain, so a segment that straddles a bound of
periods(c) keeps the producer's value on the part inside, and that value is
not recomputed for the shorter span (ch. 3.10). The condition of a rule
with a timeline target restricts the rule's producer to periods(c)
(ch. 3.10); the "gedurende" filter of the temporal total restricts the
totalled timeline to them (ch. 3.6); and the time-proportional part and the
day count count the days of periods(c) of their selecting condition
(ch. 3.7, 3.8).

Worked examples of the principle stand in the sections that apply it: an
operator per day with a scalar operand (`WV-3.4-domein`, ch. 3.4), every
operator per day (`WV-3.4-per-dag`, ch. 3.4), and a condition per period
(`WV-3.10-voorwaarde-per-periode`, ch. 3.10).

**Scalar contexts.** The reading per day applies where a timeline is read.
A time-dependent attribute or parameter in a **scalar context** is
refused (the "§8.2 list-in-scalar" marker family): the evaluation date only
gates — a rule version's validity, the period window on a scalar target
(ch. 3.10) — and never indexes a timeline to a value. The only ways from a
timeline to a scalar are the two collapses, the temporal total (ch. 3.6) and
the whole-period qualifier (ch. 3.9), and the point read below. A condition
that carries the whole-period qualifier is not a scalar context: it is the
second collapse (ch. 3.9, "This qualifier is not a scalar context"). The
condition of a rule follows its target: on a rule with a timeline target a
condition that reads a timeline is read per period, and the rule writes in
the periods in which it holds (ch. 3.10); on a scalar target a
time-dependent characteristic or attribute in the condition is refused, and
the whole-period qualifier folds (ch. 3.9). The justification and the
consequence of a default follow the target of their group in the same way
(ch. 7.11).

**The point-in-time read.** `read(tl, d)` is a core IR node, `TemporalRead`
(`IR-SPEC.md` ch. 4.3a, WF-049): a timeline operand and a date operand, the
date any date expression. The IR carries every primitive of the source
language, and the source needs this one: Jutterswet art. 1 needs the value
of "erkend jutter of vast ingezetene" **at the child's birth**, a read at an
instant that is not the evaluation date (`ASP-TRANSLATION.md` ch. 5,
finding 4). The read keeps the totality of ch. 3.1 — a gap reads leeg — and
a leeg date index reads leeg (ch. 1.2, propagation). The vintage read of
ch. 8.3 is this node over a parameter's timeline with the evaluation date as
its index. Its fail-fast discipline lives in the data plane: the
Parameterset must cover the evaluation date, and an overlap is a load error
and a coverage gap a session-start error, both raised before any read
(ch. 8.4). The anchor witness is `corpus/golden/anchor/jutterswet-aanmeldquotum`.
A time-dependent characteristic as the operand stays open with the case
format (ch. 3.12).

### 3.4 Arithmetic per day — the period-coalescing rule (§5.1.4)

A binary arithmetic operator ⊕ over two timelines is the principle of
ch. 3.3 for that operator. ⊕ is an operator of the arithmetic table of
ch. 1.2 other than the power — plus, min, verminderd met, maal, gedeeld door
in both variants, percentage van — and it is read **per day** with its own
empty rule of that table (§5.1.1, §5.1.4). The result is computed per period
of **all time** over the **cut set** (knip), the sorted union of both
operands' segment bounds and the two open bounds:

```
cuts(a, b)  =  sort ( { −∞, +∞ } ∪ { van, tot of every segment of a and of b } )
(a ⊕ b) on [c_i, c_i+1)  =  read(a, c_i) ⊕ read(b, c_i)      ⊕ with its ch. 1.2 empty rule
```

The empty rules: plus and min coalesce both operands, `coalesce(leeg) = 0`;
verminderd met propagates a leeg left operand and coalesces its right; maal
and percentage van coalesce both; a division coalesces a leeg dividend to 0
before it reads the divisor, and a leeg or numeric-zero divisor under a
filled dividend makes the period leeg and deposits on the §9.5 channel
(EF-031, EF-005; ch. 1.2, 2.2, 5.6).

- **All time.** The result divides all time (ch. 3.1; §5.1.4.1). Under plus
  and min a period in which both operands are leeg gives **0**, not leeg —
  between two segments, before the first cut and after the last alike — so
  the result's first and last segments are open. That is the
  **period-coalescing rule**: the §5.1.4 row of ch. 1.2 per day, and the
  counterpart of the contributor-skipping rule of ch. 3.5. Under the other
  operators such a period reads what their rule gives two leeg operands: 0
  for maal, percentage van and a division (a leeg dividend), leeg for
  verminderd met.
- **A scalar operand** holds on every day: it is the timeline (−∞, +∞) of
  ch. 3.1 and adds no cut (§5.1.1). A leeg scalar is the empty timeline,
  which reads leeg on every day.
- **A time-dependent parameter** is a timeline operand like a time-dependent
  attribute (§3.8, §5.1.1): its timeline, the one the Parameterset supplies,
  is read whole, and its bounds are cuts. Its timeline is its binding, so a
  session that supplies none faults as for an unbound parameter (EF-014,
  ch. 5.7). The vintage read of ch. 8.3 is another reading of the same
  parameter and keeps its own rules.
- **A deposit counts once.** However many periods divide by a leeg or
  numeric-zero divisor, the rule × subject that evaluates the value carries
  one §9.5 fact, with each code once among its codes (ch. 5.6). The period
  is no parameter of the fact.
- **Representation.** How the result divides a run of equal values into
  segments is representation (ch. 3.1, the canonical form).
- **Units** follow ch. 2.6 per period: an additive operator computes in the
  left operand's unit (each right operand restated by its exact factor); a
  product and a quotient carry the unit their rule gives, in a period where
  an operand is leeg as well — the empty rule decides that operand's value
  there, its declarations its unit (ch. 2.10) — and the result is restated
  **once**, to the target's unit. The precision bound of ch. 2.3 holds for
  the value of every segment, and the precision gate of ch. 2.4 point 4
  holds per segment (ch. 3.11).
- **Granularity.** Every operand's grid must fit the target's: every wall of
  the operand's grid is a wall of the target's (`IR-SPEC.md` WF-126;
  negatives `wf126-value-grid`, `wf126-condition-grid` and
  `wf126-whole-period-grid`, the last two for the condition, ch. 3.9, 3.10). The
  result's segment bounds are the cut set itself; they follow the operands'
  bounds and are not laid on the target's calendar grid.

Worked examples (absolute; `ref/` tests verbatim) — `a` = 100 on
[2024-01-01, 2024-07-01), `b` = 30 on [2024-04-01, 2024-10-01), `a + b`
(`WV-3.4-a`) and `a − b` (`WV-3.4-a-min`):

| period | a | b | a + b | a − b |
|---|---|---|---|---|
| (−∞, 2024-01-01) | leeg→0 | leeg→0 | 0 | 0 |
| [2024-01-01, 2024-04-01) | 100 | leeg→0 | 100 | 100 |
| [2024-04-01, 2024-07-01) | 100 | 30 | 130 | 70 |
| [2024-07-01, 2024-10-01) | leeg→0 | 30 | 30 | −30 |
| [2024-10-01, +∞) | leeg→0 | leeg→0 | 0 | 0 |

Worked example (an interior gap coalesces to 0, `WV-3.4-b`) — `a` = 1 on
[jan, feb), `b` = 2 on [mrt, apr), `a + b`: (−∞, jan) = 0, [jan, feb) = 1,
**[feb, mrt) = 0** (both leeg, coalesced to 0 + 0), [mrt, apr) = 2,
[apr, +∞) = 0.

Worked example (a scalar beside a timeline, `WV-3.4-domein`) — `a` as
above, `5 + a`: (−∞, 2024-01-01) = 5, [2024-01-01, 2024-07-01) = 105,
[2024-07-01, +∞) = 5.

The corpus witness of these three, written by rules through the RegelSpraak
frontend, is `corpus/golden/jutterswet-maandloon`.

Worked example (every operator per day, `WV-3.4-per-dag`) — `a` and `b` of
the first example, both without a unit:

| period | a maal b | a verminderd met b | a gedeeld door b | 10% van a |
|---|---|---|---|---|
| (−∞, 2024-01-01) | 0 | leeg | 0 | 0 |
| [2024-01-01, 2024-04-01) | 0 | 100 | leeg, and one §9.5 fact EF-031 | 10 |
| [2024-04-01, 2024-07-01) | 3000 | 70 | 10/3 | 10 |
| [2024-07-01, 2024-10-01) | 0 | leeg | 0 | 0 |
| [2024-10-01, +∞) | 0 | leeg | 0 | 0 |

In its canonical form (ch. 3.1) `a maal b` is 0 on (−∞, 2024-04-01), 3000
on [2024-04-01, 2024-07-01) and 0 on [2024-07-01, +∞); `a gedeeld door
(ABS) b` is `a gedeeld door b` with 3,33333 on the second quarter. The
corpus witness of the five operators, written by rules on a day grid, is
`corpus/golden/jutterswet-rekenen-per-dag`, with b = 25 so that every
quotient is whole; its case 4 gives four operands with a unit a gap each,
and its cases 5 to 7 fold over a set as an operand, the 0 of a fold without
a contributor in the unit of the fold.

Worked example (a time-dependent parameter as an operand,
`corpus/golden/anchor/jutterswet-verzuimrente-dagen`): the
verzuimrentevoet, 0,04 /jr until 2026-02-01 and 0,05 /jr from then, both
bounds open, maal an afdrachtbedrag of 10 000 € is 400 €/jr on
(−∞, 2026-02-01) and 500 €/jr on [2026-02-01, +∞).

Worked example (a finer operand, `WV-3.4-fijner-geweigerd`): a dagrentevoet
for every day in a maandrente for every month is ill-formed (WF-126): the
walls of the day grid are not all walls of the month grid.

*v0 mirror note (scope):* v0 has only **plus and min** as timeline
arithmetic, over the span of its operands, and refuses verminderd met, maal,
division and percentage van over two timelines. Every operator of ch. 1.2 but
the power read per day over all time, with a scalar and a time-dependent
parameter among its operands, is a tracked delta (`CONSTRUCTS.md` row 11).

### 3.4a The die/dat subselection on instance folds

**The contributors before any filter: reach ∩ population.** An instance
fold names two declarations, and they answer two questions. The source
relation — `sourceRole`, or the multi-step `sourcePath` of ch. 3.4b; every
instance fold carries one of the two — says which instances the fold
**reaches** from its subject. `elementType` says which instances the fold
**ranges over**, extensionally (`IR-SPEC.md` ch. 2.2): every instance whose
type is ⊑ it. The contributors are the intersection. An instance the
relation reaches and whose type the element type does not cover is no
contributor, and contributes to neither a count nor a value fold. A role's
declared filler is an upper bound (`IR-SPEC.md` ch. 2.6), so the filler may
be a strict supertype of the element type and the two sets differ; where the
filler is the element type or a type below it, the intersection is the
reach.

This narrowing is **not** the die/dat filter below, and the difference is
observable: the type narrowing comes first, and no condition can express it,
since no condition can ask an instance for its type.

Worked example (golden `jutterswet-munttelling`). `vondstaanmelding` relates
Jutter to **Vondst**; both folds declare `elementType` **Munt**, and
`Munt ⊑ Vondst` strictly. `J1` is linked to `V1` (a plain Vondst, gewicht
3,00), `M1` (a Munt, 7,00) and `M2` (a Munt, 5,00): the reach is all three,
the population is {M1, M2}, so the count is **2** and the sum **12,00** —
unfiltered they would read 3 and 15,00. `J2` is linked to exactly one
instance, `V2` (a plain Vondst, 4,00): the reach is not empty and the
contributors are, so the count is **0** and the sum falls to its `of 0`
fallback — the empty-source rules of ch. 1.2 read the contributors, never the
links. `J3` is linked to `M3`, a Munt with no gewicht: it is a contributor, so
the count is **1**, while the sum has a member and no value and reaches **0**
by the same fallback. Both folds are pinned per jutter because only the
count tells J2's empty population from J3's empty value.

The `filter` condition of an instance fold is evaluated **per
contributor**: the element instance is the condition's subject, never the
outer fold subject. A contributor whose filter operand is leeg fails the
condition (the comparison discipline of ch. 1.2) and drops out. A filter
over a derived characteristic reads it on the element type, so the fold
stands above that characteristic's writer in the strata (ch. 6.1). A filter
that excludes every contributor leaves an empty source: the sum's declared
fallback applies and a count reads 0, as for an empty role. The same wire
field carries the §7.1 "gedurende" clip of the temporal total, whose element
type is the subject itself (ch. 3.6).

Worked example (golden `jutterswet-strandgoed`): vondsten of 3, 7, 12 and one
with an empty gewicht; "die groter zijn dan 5" sums 7+12=19 (the empty one
drops out); "die waardevol zijn" — waardevol derived as gewicht > 10 — counts
1; a jutter whose only vondst fails the filter reads the fallback 0 and
count 0.

**The count source: the range selects, the written name does not.** Under
`count` the fold's one argument is the **count source**. A count over one
source argument carries its range (`sourceRole`, one step; a `sourcePath`
belongs to a value fold's set source alone, `IR-SPEC.md` WF-102 (c)), and the
count folds the instances that range reaches from the subject, narrowed by
`elementType` as above. The argument's `attribute` is the **written** name of
the bound role and selects nothing: it denotes the bound role by its
declared name or its declared plural (`IR-SPEC.md` WF-047). A written name
that matches roles on several fact types the subject reaches therefore
decides nothing: `corpus/golden/ambiguous-fold-role-name` writes `vriend`,
declared as a role on two fact types the Jutter reaches, and its count reads
the one relation its `sourceRole` names.

**The count source is recognised at the source itself.** Its `attribute`
names a role of the fold subject and no member of the element type
(`IR-SPEC.md` WF-093, in both directions: such a source is the source of
`count` and of nothing else, and `count` reads nothing but such a source). No
field on the node records which reading applies, so the declarations decide a
collision of names: a count whose written name is also a member of the
element type (an attribute or a characteristic) is no count source — it reads
as that member, and a count over a member is refused (WF-093 (b)). A role
whose declared name and declared plural both name members of the element
type, or whose name does while it declares no plural, therefore cannot be
counted; where only one of the two spellings collides, the count is written
with the other.

**The self-denoting written form is a plain count source.** The written form
"het aantal `<rol>` van de `<onderwerp>`" may name the fold subject by its own
role name ("het aantal vondsten van de aanmelder", evaluated on the Jutter
who *is* the aanmelder). The wire carries nothing of that written role: the
count carries its range and reads the subject's own linked instances of the
element type, anchored on the subject like every count source (`IR-SPEC.md`
WF-083 (c)); no hop happens.

Worked example (golden `jutterswet-aanmeldertelling`; `ref/` tests
`WV-3.4a-telbron`; on the wire the count binds `vondstaanmelding`/`vondst`
and its argument names `vondsten`, the role's declared plural): a Jutter
linked to two Vondsten — one with gewicht 2,50 kg, one whose gewicht is leeg
— counts **2**: a count folds instances, not values, so a leeg element
attribute still counts (the aggregation row of ch. 1.2: a count, never
empty). A Jutter with no linked Vondst counts **0**, never leeg. A count
without its range does not load, and handed to the reference interpreter
directly it faults (`WV-3.4a-telbron-zonder-bereik`).

Worked example (golden `jutterswet-kaveltelling`; `ref/` tests
`WV-3.4a-typenpaar`). The role `duinkavel` (kaveltoewijzing) declares no
plural, so the written source "het aantal duinkavels van de Jutter" denotes
no role by its written form; the count binds `kaveltoewijzing`/`duinkavel`
and its argument names `duinkavel`, the declared role name: a Jutter linked
to three Duinkavels counts **3**. The written singular "het aantal vat van de
Jutter" names the role `vat` (vatberging, declared plural `vaten`) by its
declared name and binds `vatberging`/`vat`: that Jutter's two Vaten count
**2**. A Jutter with no links counts **0** and **0**. With a second fact type
that connects Jutter to Duinkavel the count still reads the relation its
binding names and counts **3** (`WV-3.4a-typenpaar-tweede-feittype-bindt`).

**The form anchor: the form subject in a filter** (`IR-SPEC.md` ch. 4.1a,
WF-122). In the `filter` of an instance fold with a range, and in the
`filter` and the `test` of a quantifier fold (ch. 3.9a), the subject is the
member. A node there that carries `anchor: "form"` is evaluated with the
**form subject** as its subject, as if it stood in the form's own condition
or value: the object the form runs on (ch. 5.12, *Which forms, which
objects*), whichever fold and filter it stands in, at any depth. Everything
about such a node is what it is in the form's own condition: an anchored own
read reads the form subject's attribute (ch. 5.8 rule 4), an anchored
navigated read walks from the form subject (ch. 5.8 rules 1–3), an anchored
characteristic check reads the form subject's characteristic, an anchored
fold ranges from the form subject, and the fold's own filter has the fold's
member as its subject again. A node without the anchor reads the member.

The **identity check** (`IdentityCheck`, `IR-SPEC.md` ch. 4.5) is **waar**
iff the member is the form subject, the same object (ch. 5.8, *Identity*),
and **onwaar** otherwise; `negated: true` turns that around. It reads no
value, so it is never leeg and never faults. "die niet het Recht zelf zijn"
is `negated: true`: it takes the form subject out of a range that reaches it.
Where the member's type and the form subject's type share no instance
(`IR-SPEC.md` ch. 2.2), no member is the form subject: the check is onwaar at
every member, and with `negated: true` waar at every member (`IR-SPEC.md`
ch. 4.6, WF-122 (c), says why it is admitted; the golden `anchor-forms`, a
Register whose Vergunningen are the members, carries both).

**A leeg value of the form subject needs no rule of its own.** An ordering
comparison (`<`, `≤`, `>`, `≥`, `eerder`, `later`) between a member's value
and a leeg value of the form subject is onwaar for every member (ch. 1.2, the
comparison table; an equality or an inequality there follows its own row of
that table, as anywhere), so under such a filter every member drops out and
the fold runs over no contributor: a maximum reads leeg, a sum its fallback,
a count 0, a quantifier `all` waar (ch. 3.9a, the vacuous case), `atLeast` 1
onwaar. In a quantifier's test the same comparison fails the matrix for every
contributor: `all` over at least one contributor is onwaar. This is the one
reading a filter operand has (ch. 1.2, and above). A `Let` that bound the form
subject's value before the fold would make the whole `Let` leeg instead
(ch. 10.6, clause 2): one sentence, two answers, which is why the IR carries
the read as an anchor and not as a binding (`IR-SPEC.md` ch. 4.1a).

**Once or per member.** The value of an anchored node does not depend on the
member, so an executor may compute it once per form subject and use that
value at each member. What it may not change is where the node is evaluated:
the node is evaluated at the members whose evaluation of the filter or the
test reaches it (ch. 5.10, 5.11), as every node there is, so a §9.5 deposit
(ch. 5.6) or a fault of it arises iff at least one member reaches it. A
deposit is identified by rule × subject (ch. 5.6), so a deposit at every
member that reaches it is one fact; a fault ends the evaluation at the first
member that reaches it, with the fault the one computation gives. An executor
that computes the node where no member reaches it — over an empty range, or
at a member whose filter stopped before the node (ch. 5.10) — must not raise
its fault or make its deposit there.

Worked example (golden `anchor-once-per-member`; `ref/` tests
`WV-3.4a-anchor-once-*`). On the model of the worked example below, with a
`teller` (a whole number) and the derived `hoogste tarief boven eigen teller`
on the Vergunning: "de maximale waarde van het tarief van alle vergunningen
van het register van de Vergunning die aan alle volgende voorwaarden voldoen:
hij is niet de Vergunning zelf; zijn tarief gedeeld door de teller van de
Vergunning zelf is groter dan 1". The anchored teller is the divisor, and a
teller of 0 makes the division a §9.5 deposit of kind EF-005 (ch. 1.4 point 4)
on the rule × the form subject.

| case | registers, Vergunningen (tarief, teller) | hoogste tarief boven eigen teller | channel |
|---|---|---|---|
| `WV-3.4a-anchor-once-alone` | R9: V9 (10, 0); R1: V1 (10, 2), V2 (30, 1) | V9 **leeg**, V1 **30**, V2 **10** | **empty** |
| `WV-3.4a-anchor-once-no-register` | V8 (10, 0) in no register; R1: V1 (10, 2), V2 (30, 1) | V8 **leeg**, V1 30, V2 10 | **empty** |
| `WV-3.4a-anchor-once-reached` | R8: V7 (10, 2), V8 (10, 0) | V7 **10**, V8 **leeg** | EF-005 on the rule × **V8** |

Read per case. **alone**: V9's range reaches V9 alone, and the identity check
stops the filter before the division (ch. 5.10): no member reaches the
anchored divisor, so V9's teller of 0 deposits nothing. **no register**: V8's
range is empty: no member, no deposit. **reached**: V7 is a member of V8's
range that passes the identity check and reaches 10 / 0: one deposit on the
rule × V8; the comparison over the leeg quotient fails (ch. 1.2), so V8's
maximum has no contributor (leeg). V7's divisor is its own teller 2: V8
passes (10 / 2 = 5 > 1), 10. In the first two cases an executor that computed
the anchored divisor once per form subject without asking whether a member
reaches it would deposit on V9 and on V8. V1 and V2 give each case a value the
input does not carry (gate 7): V1 folds V2 (30 / 2 = 15 > 1), V2 folds V1
(10 / 1 = 10 > 1).

**Two spellings, one answer.** Where the member reaches the form subject over
a hop whose reached role is to-one ("de schuit van de Lading", from a lading of
that schuit), a navigation from the member and an anchored read give the same
value at every member (`IR-SPEC.md` ch. 4.1a): every member the range reaches
from the form subject S over a role links back to S over the opposite role.
Their dependency edges differ (ch. 6.1); their answers do not.

Worked example (golden `jutterswet-registervoorrang`; `ref/` tests
`WV-3.4a-anchor-*`). A `Register` holds `Vergunningen` through the fact type
`registerinschrijving` (roles `register`, filled by Register, to-one, and
`vergunning`, plural `vergunningen`, filled by Vergunning, to-many). A
Vergunning has an `ingangsdag` (a date), a `tarief` (a whole number) and the
derived `hoogste latere tarief`, and the characteristic `voorrang`. Both rules
range over `sourcePath [registerinschrijving/register,
registerinschrijving/vergunning]` from the Vergunning, a range that reaches
the Vergunning itself:

- **hoogste latere tarief** — "de maximale waarde van het tarief van alle
  vergunningen van het register van de Vergunning die aan alle volgende
  voorwaarden voldoen: hij is niet de Vergunning zelf; zijn ingangsdag is
  later dan de ingangsdag van de Vergunning zelf": a maximum whose filter is
  `all` over an `IdentityCheck` with `negated: true` and a comparison `later`
  of the member's `ingangsdag` with the anchored `ingangsdag`;
- **voorrang** — "een Vergunning heeft de voorrang indien de ingangsdag van
  alle vergunningen van het register van de Vergunning die niet de Vergunning
  zelf zijn later is dan de ingangsdag van de Vergunning zelf": a quantifier
  `all` with the identity check as its filter and the same comparison as its
  test.

The identity check is redundant beside a strict `later` in the value fold (no
member starts later than itself); it stands there so that both parts of the
sentence, the exclusion and the comparison, stand in one filter. In the
quantifier it is not redundant: without it the Vergunning would be its own
contributor, and its test, `later` than itself, would fail.

| case | register, Vergunningen (ingangsdag, tarief) | hoogste latere tarief | voorrang |
|---|---|---|---|
| `WV-3.4a-anchor-three` | R1: V1 (2026-01-01, 10), V2 (2026-03-01, 20), V3 (2026-06-01, 15) | V1 **20**, V2 **15**, V3 **leeg** | V1 **waar**, V2 onwaar, V3 onwaar |
| `WV-3.4a-anchor-alone` | R1: V4 (2026-01-01, 10) | V4 **leeg** | V4 **waar** |
| `WV-3.4a-anchor-two-registers` | R1: V1 (2026-01-01, 10), V2 (2026-03-01, 20); R2: V3 (2026-02-01, 30) | V1 **20**, V2 leeg, V3 **leeg** | V1 waar, V2 onwaar, V3 **waar** |
| `WV-3.4a-anchor-empty-subject` | R1: V1 (leeg, 10), V2 (2026-03-01, 20), V3 (2026-06-01, 15) | V1 **leeg**, V2 **15**, V3 leeg | V1 **onwaar**, V2 onwaar, V3 onwaar |
| `WV-3.4a-anchor-empty-member` | R1: V1 (2026-01-01, 10), V2 (leeg, 20), V3 (2026-03-01, 15) | V1 **15**, V2 leeg, V3 leeg | V1 **onwaar**, V2 onwaar, V3 onwaar |
| `WV-3.4a-anchor-same-day` | R1: V1 (2026-01-01, 10), V2 (2026-01-01, 20), V3 (2026-02-01, 5) | V1 **5**, V2 **5**, V3 leeg | V1 **onwaar**, V2 **onwaar**, V3 onwaar |
| `WV-3.4a-anchor-earlier-elsewhere` | R1: V1 (2026-03-01, 10), V2 (2026-02-01, 20); R2: V3 (2025-12-01, 30) | V1 **leeg**, V2 **10**, V3 leeg | V1 **onwaar**, V2 **waar**, V3 waar |

Read per case. **three**: each Vergunning gets a different answer from the
same rules; V3, the latest, has no later member (leeg), and V2 and V3 have V1,
which starts earlier, among their contributors (onwaar). **alone**: the range
reaches V4 alone, the identity check drops it, so the maximum has no
contributor (leeg) and `all` is waar over none. **two registers**: the range
is the register's, so V3 (2026-02-01, in R2) does not count for V1
(2026-01-01, in R1) although it starts later; V3 is alone in R2. **empty
subject**: V1's ingangsdag is leeg, so the anchored operand is leeg at every
member of V1's folds: the comparison fails for V2 and V3, V1's maximum has no
contributor (leeg), and V2 and V3 are contributors of V1's quantifier whose
test fails (onwaar); for V2, V1 is a member with a leeg ingangsdag, which
drops out of V2's maximum, so V2 folds V3 alone (15), and fails V2's test
(onwaar). **empty member**: V2's ingangsdag is leeg, so V2 drops out of V1's
value fold, which folds V3 alone (15), while in V1's quantifier V2 passes the
filter and fails the test (onwaar); V3 has no later member. **same day**: a
strict `later` counts no member of the same day, in either direction: V1 and
V2 each fold V3 alone (5, where a comparison that admitted the same day would
give 20 and 10), and V2 fails V1's test (onwaar, where such a comparison would
hold). **earlier elsewhere**: V1's one member V2 starts earlier than V1 (leeg,
onwaar), V2's one member V1 starts later than V2 (10, waar), and V3 is alone
in R2. The test compares each member with the form subject, not with any
Vergunning: V2 starts later than V3, so a reading that compared it with V3
would give V1 the voorrang. The third Vergunning of the empty-subject and the
same-day cases gives each a value the input does not carry, so a run that
evaluates nothing does not pass them (gate 7).

### 3.4b The multi-step fold source — the flat union (WF-102)

A set fold source may describe its range with a **path** of relations rather
than one (`IR-SPEC.md` ch. 4.3, `sourcePath`; two or more steps, the one-step
range stays `sourceRole`). The folded set is the **flat union**: start at the
fold subject, take step 1's linked instances, take step 2's linked instances
from each of those, and so on; the instances the last step reaches are the
fold's reach, which `elementType` narrows to the contributors exactly as in
the one-step form (ch. 3.4a — the intermediate steps are not narrowed, only
the landing). It is a **set** — an element reachable through more than one
intermediate instance contributes **once** — and everything downstream is
unchanged: the element's attribute is read at the `elementType` as in the
one-step form, a leeg element attribute drops out of a value fold (ch. 1.2),
the die/dat subselection (ch. 3.4a) is evaluated per contributor with the
element as its subject, and the empty-source rules (ch. 1.2) apply to the
union, not to any intermediate step.

Three consequences of "the union, not any intermediate step", each stated
because the nested reading answers differently:

1. An intermediate instance that reaches nothing contributes nothing. It
   never produces an empty inner result for the outer fold to absorb, so
   `minimum`, `maximum`, `first` and `last`, which have no fallback, never
   decide what to do with one.
2. A count over the union counts **elements**, never intermediate
   instances. A counting fold under `count` cannot range over a union: a
   `sourcePath` is admissible only as the range of a value fold's set source,
   and a count source's range is one step (`IR-SPEC.md` WF-102 (c)); every
   executor refuses a count that carries a `sourcePath`, by name. The
   operation that counts over a union is the filled count (ch. 3.4c), which
   reads a value per contributor and therefore takes the set source a path
   needs; ch. 3.4c's last worked example pins that it counts elements.
3. A sum's `emptyFallback` applies at most **once**, for a subject whose
   whole range is empty — never once per empty intermediate instance. With a
   zero fallback the two readings agree in value; with any other they do not.

A step through a to-one role is an ordinary step: it contributes its zero or
one linked instance to the union. Multiplicity is a fact of the declaration
and does not enter this reading.

Worked example (golden `anchor/jutterswet-bootberging`; `ref/` tests
`WV-3.4b-vlakke-unie` and `WV-3.4b-gedeeld`). The model: a Jutter sails
`gevoerde boten`, a Boot salvages `geborgen vondsten`, a Vondst fills
`ingepakte kisten`. Three rules on one Jutter — the minimum `gewicht` over the
two-step range, the sum of the same with an `of 0` fallback, and the minimum
`inhoudsgewicht` over the three-step range.

*Case 1 — the range spread over two intermediate instances.* `J1` sails `B1`
and `B2`; `B1` salvages `V1` (3,00) and `V2` (7,00); `B2` salvages `V3`
(5,00). The union is {V1, V2, V3}, so the minimum is **3,00** — it lies on
`B1` while `B2` contributes 5,00, which makes the answer a proof that both
intermediate instances are walked — and the sum is **15,00**. `V1` holds `K1`
(2,50), `V2` holds `K2` (1,25) and `K3` (4,00), `V3` holds none: the
three-step union is {K1, K2, K3} and the minimum is **1,25**. `J2` sails one
boat salvaging `V4` (9,00) and `V5` (no gewicht): the leeg element drops out
of both value folds, so the minimum is **9,00** and the sum **9,00**, while
the three-step range reaches only `K4` (6,00) → **6,00**. `J3` sails a boat
that salvages nothing and `J4` sails no boat at all: both ranges are empty,
so both minima are **leeg** (no fallback exists) and both sums are **0** (the
fallback, applied once) — the empty range at the last step and at the first
step answer alike.

*Case 2 — the shared element.* `J1` sails `B1` and `B2`; `B1` salvages `V1`
(3,00) and `V2` (7,00); `B2` salvages `V1` again and `V3` (5,00). `V1` is
reachable along two intermediate instances and contributes once, so the sum
is **15,00** and not 18,00; the minimum is **3,00** either way, which is why
the sum is the witness. The same holds one step deeper: `V1` and `V3` both
hold `K1` (2,50) while `V2` holds `K2` (1,25), so the three-step union is
{K1, K2} and the minimum is **1,25**.

### 3.4c The filled-value count

(Wire form and well-formedness: `IR-SPEC.md` ch. 4.3, "The filled count".)

One aggregation kind folds the contributors' values by **presence**:
`filledCount` counts the contributors whose value is not leeg. It is an
instance fold and it reads a value per contributor, so it takes a **set**
source — the single argument of a fold that carries a range, `sourceRole` or
`sourcePath` (`IR-SPEC.md` ch. 4.3) — and that source is required. It has no
listed form: the source language has no spelling for the operation, so there
is no articulated operand list to admit.

The contributor set is ch. 3.4a's: reach ∩ population, narrowed further by
the `filter` per contributor. **Filled** is the ordinary emptiness test of
ch. 1.2 — the one the source language's `is gevuld` predicate applies — so a
contributor whose value is leeg is not filled and every other value is,
**whatever its type**. The test asks for presence, not for a number: a text, a
date, an enumeration value and a boolean all count as filled, and so does a
boolean whose value is false. False is a value; leeg is the absence of one. An
instance that is not a contributor never reaches the test.

`filledCount` over a source with no filled values is **0**, never leeg — a
count is a cardinality and the empty collection has one, as the count row of
ch. 1.2 says, and ch. 1.3 keeps that 0 apart from coalescing: it is the
identity element of a rule over sets, not an empty value read as a number.
That ground belongs to every count, not to the kind `count`. It carries no
`emptyFallback`: there is no empty to fall back from, and WF-037's "sum only"
clause excludes it.

The result is a whole, unitless number whatever unit the folded attribute
carries, so a filled count has no rounding question and no unit derivation.
It has the result type of the instance count, reached by another fold. That
type is derived and not declared, so a wire annotation that says anything else
is ill-formed rather than believed (`IR-SPEC.md` WF-091; negative
`wf091-filled-count-unit`).

**One exclusion: the folded attribute is not time-dependent.** "Whatever its
type" is about the value domain — number, text, date, enumeration, boolean —
and not about the time axis. A set read of a time-dependent attribute folds
twice (ch. 3.6, §7.1): each member's own timeline collapses over time first,
and only those scalars fold over the set. That first collapse is defined for
the som and the totaal and for nothing else, because only they integrate; a
count has no defined moment at which to ask a timeline whether it is filled,
and this chapter states no such moment. A filled count over a time-dependent
attribute therefore fails observably with EF-021, a form without a meaning
(ch. 3.12), naming the attribute and the fold, and never reads a value the
timeline does not have at an instant (worked example
`WV-3.4c-temporeel-faalt`, red case `filled-count-temporal-set-source`).
Naming the fold is part of the clause: the ranking folds are refused at the
same position on the same ground, so a refusal that named only the position
would be one sentence for five constructs and would identify none of them.

**The instance count and the filled count answer different questions, and it
takes both to tell the two kinds of empty apart.** Over a source whose
contributors are all leeg the filled count is 0 while the instance count is
not; over a source with no contributors both are 0. The instance count
separates an empty population from an empty value (ch. 3.4a), the filled count
does not, and a document that needs the distinction writes both.

**What this does NOT give.** There is no average. `sum ÷ filledCount` is the
form an author writes out; over a wholly empty source it divides by zero: the
quotient is leeg and the division deposits a §9.5 fact of kind EF-005 on the
rule × subject (ch. 1.4 point 4). Worked example (golden
`filled-count-unguarded-division`): a Jutter without vondsten has `aantal
gevulde` 0 and a som of 0 by its fallback, so `gemiddelde vondstwaarde` =
0 ÷ 0 is **leeg**, with one §9.5 fact of kind EF-005 from the rule
`gemiddelde vondstwaarde` on that Jutter. What the kind does give the author
is the guard that avoids it: a filled count is an ordinary expression, so
"indien het aantal gevulde … groter is dan 0" can stand in a rule's condition,
where `count` could not serve — an instance count is 1 for a member without a
value and would let the division through.

**Worked examples (absolute; `ref/` tests verbatim, `WV-3.4c-*`).** The
Jutterswet model: a Jutter aanmeldt Vondsten; a Vondst declares `gewicht` (kg,
two decimals), `vindplaats` (tekst), `vondstdag` (datum), `kustvak`
(enumeratie noord/zuid) and `geborgen` (booleaan).

*The source shapes.* The `gewicht` fold over "alle vondsten van de Jutter",
per jutter, beside the instance count over the same population:

| contributors' values | aantal | aantal gevulde |
|---|---|---|
| 2,50; 1,25; leeg | 3 | **2** |
| 5,00; 7,00 | 2 | 2 |
| leeg; leeg | 2 | **0** |
| leeg | 1 | **0** |
| (no contributors) | 0 | **0** |

The third and fourth rows are where the two counts part: a member with no
value is a member. The fifth pins the empty source at 0 rather than leeg.

*The test does not depend on the type.* The same three contributors — two
carrying values, one carrying none — folded once per declared type:

| folded attribute | the three values | aantal gevulde |
|---|---|---|
| `gewicht` (numeriek, kg) | 2,50; 1,25; leeg | **2** |
| `vindplaats` (tekst) | "Noorderstrand"; "Zuiderhoofd"; leeg | **2** |
| `vondstdag` (datum) | 2026-05-01; 2026-06-21; leeg | **2** |
| `kustvak` (enumeratie) | noord; zuid; leeg | **2** |
| `geborgen` (booleaan) | waar; onwaar; leeg | **2** |

In the last row `onwaar` is filled. The fold neither compares nor adds, so the
operand family the ordering folds demand (`IR-SPEC.md` ch. 2.4) has no bearing
here.

*The filter narrows the contributors first.* Over the jutter whose vondsten
weigh 2,50, 1,25 and leeg:

| filter | aantal | aantal gevulde |
|---|---|---|
| none | 3 | **2** |
| die zwaarder zijn dan 2,00 kg | 1 | **1** |
| die zwaarder zijn dan 10,00 kg | 0 | **0** |

The leeg member fails the comparison (ch. 1.2) and drops out before the
presence test, which is why the middle row's two counts agree: one
contributor, and it has a value.

*The multi-step range counts elements (ch. 3.4b).* A Jutter sails boten, a
Boot salvages vondsten. `J1` sails `B1` and `B2`; `B1` salvages `V1` (3,00)
and `V2` (no gewicht); `B2` salvages `V3` (5,00). The union is {V1, V2, V3}
and the filled count is **2**. With `B2` salvaging `V1` as well, the union is
unchanged and the filled count is still **2**: an element reachable along two
intermediate instances contributes once. Under `count` this shape has no wire
form (`IR-SPEC.md` WF-102 (c): a count source's range is one step), and the
filled count is the operation that carries it.

### 3.4d The instance-set union — the §5.7 concatenation

(Wire form and well-formedness: `IR-SPEC.md` ch. 4.3, "The instance-set
union", WF-110.)

A count may range over a **concatenation** of subject expressions — "het
aantal aangemelde vondsten van de Jutter en geborgen vondsten van de Jutter" —
and the source language defines that as joining the instance sets the
selections yield into one new set (§5.7); its typing document adds that the
count operator admits such a concatenation, unbounded in number, and takes
subject expressions only (§4.11). On the wire the selections are an
`InstanceSetUnion` under the count, one `InstanceSetBranch` each.

**The members.** Each selection has its own contributors, computed as
ch. 3.4a computes a fold's: the reach — the instances its `sourceRole` links
the count's subject to — intersected with the population its `elementType`
names extensionally, narrowed further by its own `filter`, which is evaluated
per contributor with the element as the condition's subject. The union's
members are the union of those sets.

**Deduplication is on instance identity, and it is the whole operation.** An
instance that satisfies two selections is one instance and contributes once,
so the count is |A ∪ B| and never |A| + |B|. This is ch. 3.4b's rule for the
flat union of a multi-step range, stated across selections instead of across
intermediate steps, and for the same reason: a set has no multiplicities. The
three consequences carry over unchanged — a selection that reaches nothing
contributes nothing (it never produces an empty inner result for the count to
absorb), the count counts elements, and the empty-source rules of ch. 1.2
apply to the union and never to a selection.

**Empty.** A union none of whose selections contributes anything counts
**0**, never leeg — the count row of ch. 1.2, which belongs to every count and
not to any particular source shape. A count carries no `emptyFallback` and
has no empty to fall back from.

**Two selections describing the same thing.** A union whose selections name
the same relation and the same filter is well-formed and counts that selection
once: the deduplication rule doing its job, not a defect. The verifier does not
refuse it, because refusing it would mean comparing conditions structurally to
forbid a document that already answers correctly.

**Where a union count stands.** WF-110 (a) restricts the union node to one
position — the count's single argument. The count over it is an ordinary
whole, unitless number and stands wherever any other count stands: as a rule
value, in a `condition`, on either side of a comparison. Worked example below
(`jutterswet-unievoorwaarde`).

**One population.** Every selection of one union names the same element type
(WF-110 (d)), so "instance identity" needs no reading across a subtype lattice
here: the members are instances of one declared population. That is a wire
restriction and not a semantic simplification — see `IR-SPEC.md` for why the
union over several populations is a separate question.

**Worked examples (absolute; `ref/` tests verbatim, `WV-3.4d-*`; goldens
`jutterswet-vondstunie` and `jutterswet-keurunie`).** The Jutterswet model: a
Jutter `meldt` aangemelde vondsten (`vondstaanmelding`) and `bergt` geborgen
vondsten (`vondstberging`); a Vondst carries the characteristics `gemerkt` and
`gekeurd`.

*Two selections over different relations — the deduplication witness.* `J1`
meldt `V1`, `V2` and `V3` aan, and bergt `V3` and `V4`. The selections are
{V1, V2, V3} and {V3, V4}; `V3` lies in both and counts once, so the union is
{V1, V2, V3, V4} and the count is **4** — the sum of the parts is 5, so the
answer discriminates. `J2` meldt `V5` aan and bergt `V5` as well: both
selections are {V5}, the union is {V5} and the count is **1**, not 2. `J3`
meldt nothing and bergt `V6`: one empty selection, so the count is **1** — an
empty selection contributes nothing rather than emptying the union. `J4` meldt
nothing and bergt nothing: **0**, never leeg.

*Two selections over one relation with overlapping filters.* Over `J1`'s
aangemelde vondsten, `V1` is gemerkt, `V2` is gekeurd and `V3` is both: the
selections are {V1, V3} and {V2, V3}, the union is {V1, V2, V3} and the count
is **3** where the sum of the parts is 4. `J2` meldt one vondst that is
neither: both selections are empty and the count is **0**.

*The union count in a `condition`.* "Een Jutter is ruim voorzien indien het
aantal aangemelde vondsten van de Jutter en geborgen vondsten van de Jutter
groter is dan 2." `J1` meldt `V1`, `V2`, `V3` aan en bergt `V3` en `V4`: the
union is 4, so the characteristic holds. `J2` meldt `V5` en `V6` aan en bergt
dezelfde twee: the union is **2** and the characteristic does not hold — where
the sum of the parts is 4 and would have made it hold, which makes this row a
deduplication witness and not only a witness of the position. `J3` reaches one
element (1) and `J4` none (0); neither holds.

*A filtered selection beside an unfiltered one.* Over `J1`'s three aangemelde
vondsten, "die gemerkt zijn" selects {V1, V3} and the bare selection selects
all three: the union is the bare selection's set and the count is **3**. The
form is carried rather than refused — see `IR-SPEC.md`, where the v0
toolchain's marker for it is shown to be a property of its lowering mechanism
and not of the operation.

### 3.5 The sum per period — the contributor-skipping rule (§5.8.2)

The temporal sum over N timeline contributors — the operands of a temporal
som, or the element timelines of a to-many navigation — is the som rule of
ch. 1.2 read per day (ch. 3.3). It is computed per period over the cut set of
**all** contributors:

```
som(t_1 … t_N) on [c_i, c_i+1)
  = Σ { read(t_k, c_i) | read(t_k, c_i) ≠ leeg }   if ≥ 1 contributor is not leeg there
  = leeg                                            if all N are leeg there — unless
    the declared fallback clause ("… of 0 als die er niet zijn")
    substitutes 0
```

This is the **contributor-skipping rule**: a leeg contributor is skipped,
never counted as 0 — the aggregation discipline of ch. 1.2 per day, and the
opposite of the period-coalescing rule of ch. 3.4.

- **All time** (ch. 3.1). Before the first and after the last bound of the
  contributors every contributor is leeg, so the sum is leeg there, and the
  fallback clause fills it with 0: the result's first and last segments are
  then open. With **zero contributors** (an empty to-many set) every day is
  all-leeg too: the result is the **empty timeline** without the fallback
  clause, and the one segment (−∞, +∞) = 0 with it.
- An all-leeg period that the fallback does not fill reads leeg. Whether it
  is a gap or an explicit leeg segment, and how the sum divides a run of
  equal values into segments, is representation (ch. 3.1).
- Each contributor is restated **before** the fold, by its exact factor
  (ch. 2.6): into the target's unit where the sum is the value of the rule,
  into its attribute's unit where it is an operand of an operator, since only
  the value of the rule is restated to the target's (ch. 3.4). A unitless
  fallback adopts that unit.
- The operand is a Number (WF-091, `IR-SPEC.md` ch. 4.1): a contributor
  declared as any other type makes the document ill-formed.

Worked examples (absolute; `ref/` tests verbatim): contributor
p1 = {jan: 50, feb: 60}, p2 = {feb: 10, mrt: 10}, per month:

| month | p1 | p2 | som |
|---|---|---|---|
| jan | 50 | leeg (skipped) | 50 |
| feb | 60 | 10 | 70 |
| mrt | leeg (skipped) | 10 | 10 |

With p1 = {jan: 40}, p2 = {mrt: 20}: jan = 40, **feb = leeg**, mrt = 20, and
leeg before jan and from apr on (`WV-3.5-b`); the same with the fallback
clause: (−∞, jan) = 0, jan = 40, **feb = 0**, mrt = 20, [apr, +∞) = 0
(`WV-3.5-b-fallback`). With no contributors at all: the empty timeline
(`WV-3.5-c-empty-set`), and with the fallback clause (−∞, +∞) = 0
(`WV-3.5-c-empty-set-fallback`). The sum over the members of a set, written
by a rule, is witnessed by `corpus/golden/jutterswet-maandloon` (the first two
examples, with and without the fallback clause, and the empty set).
Cross-unit: contributors in €/mnd summed into a €/jr target, jan p1 = 50,
p2 = 10 → each contributor restated before the fold: 50×12 + 10×12 =
**720**.

*v0 mirror note (scope):* of the aggregations, v0 has only **som** and the
totaal (ch. 3.6) over timelines; minimum, maximum, eerste and laatste over a
timeline are refused, because "the minimum of a timeline" is ambiguous
between a fold per period over contributors and a fold over the periods of
one timeline (ch. 3.12).

### 3.6 The temporal total — collapse to a scalar

"Het totaal van" a timeline over time collapses it to one scalar:

```
totaal(tl) = Σ over segments s of tl:  value(s) × length(van_s, tot_s)
```

where `length` measures the segment in units of the timeline's grid, by the
weighting of ch. 3.2: a day counts 1/len of the period of the grid that holds
it, len that period's length in days, so a whole period counts 1 and a part
of one its share of the days (ALEF's `partialLength`). On the calendar the
period is the calendar day, month, quarter or year, and a week counts seven
days wherever it starts; a fixed start point lays the periods of its own
grid, so a part of a storm year from 1 October counts its days over that
storm year's 365 or 366. The length is additive: two fragments that split a
period count the period. *v0 mirror note:* v0 counts whole units per clipped
fragment, so a part of a unit counts 0 and two fragments that split a unit
count 0 together; counting a part by its days is a tracked delta.

- A **leeg** segment contributes 0 — a rule of this collapse, in the family
  of the totaal row of ch. 1.2: the collapse is a total, not a som. A segment
  shorter than one unit contributes its share.
- A segment with an **open bound** (ch. 3.1) holds unboundedly many units.
  With the value 0 it contributes 0; with any other value the total has no
  value and the session faults with **EF-036** (ch. 5.7b; the parameters
  are the totalled `attribute` and the `label` of the object whose timeline
  it is), whichever route reaches it: the subject's own total, the
  "gedurende" clip (whose fragment of an open segment keeps its open bound)
  and the per-element fold of §7.1. This is the warning of §7.1 — a total
  without a bounding condition can have unboundedly many values — made a
  fault.
- A **segmentless** timeline totals **0**, the identity of a sum over
  nothing, in the unit the total carries by its declarations (ch. 2.10), and
  so does an **absent** timeline (the member itself leeg): it reads leeg on
  every day (ch. 3.1), the total counts a leeg value as 0, and it is 0 where
  every value is leeg (typ. §4.20). This holds on every route: the clip, the
  timeline a rule writes, the asOfDate bucket, and each member of the
  per-element fold. *v0 mirror note:* v0 totals an absent timeline leeg and a
  segmentless one 0; 0 for both is a tracked delta.
- **Which timeline the subject's own total reads.** A timeline that a rule
  writes by ch. 3.4 or 3.5 divides all time, and the subject's own total
  totals it whole. For the own total without a clip, a case supplies the
  subject's timeline as the asOfDate-bucket scalar, the value of the period
  of its grid that holds the evaluation date, and the total counts that one
  period; a full timeline there is refused, and a session without an
  evaluation date has no such period, so the total faults (the two input
  conventions, below and ch. 8.4; ch. 3.12).

**The "gedurende" clip (§7.1, WF-055).** A filter on the temporal total
restricts the totalled timeline to the periods of its condition before the
total (ch. 3.3):

```
totaal(tl gedurende c) = totaal(tl restricted to periods(c))
```

Each raw clipped fragment is totalled over its length, never coalesced, so a
part of a unit counts by its days: a per-year value clipped to five days of
2024 contributes 5/366 of it, and two adjacent condition periods that split
one unit count its two parts, together the whole unit (golden
`jutterswet-getijdenwacht`, J4: **10**). The condition's periods (ch. 3.10)
come from:

- a check of an own time-dependent characteristic — its true periods; a gap
  means the characteristic does not hold and clips away; a negated check
  takes the complement; the condition's own values are ignored;
- a flat `all` composite of such checks — the **intersection** of their
  periods, so the order of the members never matters; a negated member clips
  through its complement; a member without any true period empties the
  intersection, and the total is 0;
- a comparison of an own time-dependent attribute against a scalar bound —
  the days on which the comparison of ch. 1.2 holds, read per day: a gap in
  the compared timeline and a leeg bound are leeg sides, so "ongelijk aan"
  holds on a gap day against a filled bound.

The clip is about the **subject** and about the **whole span** of the
clipped timeline: a clip member that carries an owner or a whole-period
qualifier — the negated qualifier included — is ill-formed (WF-097,
`IR-SPEC.md` ch. 4.5), and an executor that meets one faults by name. The
same rule covers the three clips: this total's, the time-proportional part's
(ch. 3.7) and the day count's (ch. 3.8). WF-055 admits no other condition:
v0 reads a static (non-temporal) comparison member as a guard on the value
and clips an at-least-one composite to the union of its members' periods,
and v1 admits neither, nor a nested composite (a tracked narrowing). A
fully clipped-away or absent value timeline totals 0, never leeg, as the
unfiltered total does. The operand is a Number (WF-091, `IR-SPEC.md`
ch. 4.1), so this 0 always lands in a numeric slot.

**Units, the plain operand.** The rate obligation of WF-038 is met by the
structure of this collapse: a plain value reads per unit of the grid, so a
plain `€`-valued timeline per month totals to `€` by value × its length in
months, a part of a month by its days. The specification and ALEF ask the
operand of the total for a unit per time unit (§7.1, typ. §4.20); keeping the
plain operand is a tracked delta, and with the length above it gives the
number ALEF gives for a denominator equal to the grid unit. An explicit rate
unit (`€/mnd`) is what the non-temporal totaal operand requires; the
temporal collapse does not require it.

**Units, the explicit rate operand.** When the timeline's value carries a
rate unit, the collapse is an **integration**, and the length is a
**duration in the grid's own unit**: `value × duration` cancels the negative
factor of the grid's class by one power (ch. 2.6, rule 8) — `€/mnd` over a
timeline per month totals to `€`, `€/m/mnd` to `€/m`. This is the one
collapse that changes the unit, and it is the runtime half of the §4.20
Tabel-30 result type (row 2, `Eenheid1`), so a `€`-declared target accepts the
total without a restatement (`ref/` tests `WV-3.6-g*`, `WV-3.6-h*`).

- The grid's unit is the one the document's declared unit systems spell for
  this granularity (the spelling table of ch. 3.8), resolved **once per
  fold**. The integration is **exact over every denominator the declarations
  connect to it**, through those declared factors and nothing else: `€/mnd`
  on a grid per month cancels one for one; `€/jr` on a grid per month cancels
  too where the document declares `jr = 12 mnd` — twelve months are one year,
  so `10 €/jr` over 2024 is **10 €**, not 120; and a month-sized unit that is
  not a calendar spelling (`€/bm` with `bm` canonically a month) cancels by
  its declared factor, the spelling table not entering.
- **The calendar** (ch. 3.2). Where the value's denominator is a unit of the
  spelling table of the other family than the grid's, the collapse
  integrates by the actual length of the calendar unit: the segment is
  measured in the denominator's unit as ch. 3.2 measures a span, and the
  negative factor rises as above, so `€/jr` over a day grid totals to `€`, a
  day weighing 1/365 or 1/366. This is ALEF's integration, a part of a grid
  unit included.
- Three things are refused **by name**, never guessed: a unit none of whose
  negative factors the declarations connect to a grid-sized unit and none of
  which the calendar integrates (`€/kg`, `€/kg/m`; the refusal names both the
  value's unit and the grid's), a document that declares **no** unit for the
  grid, and a document that declares **several** grid units with different
  canonical chains. A value unit **without** a denominator keeps the plain
  rule above, and a unitless value keeps a unitless total — those two need no
  grid unit, so they total in a document that declares none.
- Within one collapse the products per segment are summed by the ordinary
  addition of ch. 2.6: a segment in a **convertible** sister unit is restated
  exactly (120 € + 500 ct = 125 €), a **foreign** one is refused by name, and
  only a **unitless** side adopts the other unit. The case format cannot
  reach a timeline of mixed units — the loader gives every segment the
  declaration's one unit — so this rule governs computed timelines alone.

**The per-element fold over a set (§7.1).** `het totaal van <temporal
attribute> van alle <set>` folds **twice**: each member's own timeline
collapses over time first — by the rule above, at that attribute's own
granularity — and only the resulting scalars fold over the set. The outer
fold is the ordinary aggregation of ch. 1.2: a member without a timeline
contributes **0**, the total of its absent timeline, and for `het totaal van`
an empty set totals **0** in the unit the total carries by its declarations
(ch. 2.10); `de som van` over the same shape folds alike inside and keeps the
som's leeg rule outside, for the empty set. Every other aggregation over a
time-dependent member of a set — minimum, maximum, eerste, laatste, aantal,
aantal gevulde — is **refused**: there is no defined moment of the timeline
for it to rank or count (ch. 3.4c, 3.12), and reading the segment list
instead of a value is the error this rule prevents.

On the wire the two routes differ by their range. The per-element fold
carries one (`sourceRole` or `sourcePath`, the relation of its set source).
The subject's own collapse carries none: of the single-argument aggregations
that carry an `elementType`, `total` alone may omit its range (`IR-SPEC.md`
ch. 4.3), and it then reads the subject itself, so its `elementType` and its
argument's `subjectType` both name the subject type (WF-083 (c)).

The two routes read **opposite** input conventions, and the asymmetry is
deliberate (ch. 8.4 keeps the two case conventions apart):

- the **subject's own** collapse of a case-supplied timeline takes the
  asOfDate-bucket scalar: the session date is the horizon, so one period of
  the subject's grid carries the total;
- the **per-element** fold requires every member to bring its **own**
  horizon, because the integration runs once per member and there is no
  single bucket to borrow. The full-timeline variant of the case format is
  the one form that states its own bounds; a member that carries the bucket
  scalar is refused by name, naming that member — the "needs the one, finds
  the other" case of ch. 8.4. An **absent** member contributes 0, and members
  that carry their horizon mix freely.

Worked examples (absolute, `ref/` tests verbatim; a timeline per month):

- segments 2023-01-01..2024-01-01 = 10,00 and 2024-01-01..2024-07-01 = 11,00
  → totaal = 12×10 + 6×11 = **186,00**.
- degenerate segments (zero-length or reversed) are skipped: only a 6-month
  segment of 10 remains → **60,00**, never a negative count.
- open segments (`WV-3.6-open-zero`, `WV-3.6-open-nonzero`): the total of
  `a + b` of ch. 3.4's first example — 0 on (−∞, 2024-01-01), 100, 130, 30,
  0 on [2024-10-01, +∞) — is 3×100 + 3×130 + 3×30 = **780**; the total of
  `5 + a` (5 on both open segments) faults with EF-036. In the corpus:
  `jutterswet-maandloon` totals its `maandloon` (780 in case 1, 3 in case 2,
  0 in case 3, whose one segment (−∞, +∞) carries 0), and
  `corpus/red/jutterswet-totaal-open-segment` totals `5 + a`.
- the clip (golden `jutterswet-getijdenwacht`): 10/month through 2023 and
  11/month January–June 2024, clipped to [2023-07-01, 2024-02-01) →
  6×10 + 1×11 = **71**; the comparison clip "> 10" keeps the 11-segment →
  6×11 = **66**; a carrier without periods, and one without a timeline, both
  total **0**.
- gedurende "> 1000" over the value timeline {3 mnd: 1200, 2 mnd: 800}:
  clipped to the 1200-months → 1200×3 = **3600,00** (not 5200).
- value timeline 10 on [2023-01-01, 2024-01-01) and 11 on
  [2024-01-01, 2024-07-01); characteristic true on [2023-07-01, 2024-02-01).
  Gedurende the characteristic → 6×10 (jul–dec 2023) + 1×11 (jan 2024) =
  **71,00**; the negated form (the complement, within the value timeline's
  span) → 6×10 (jan–jun 2023) + 5×11 (feb–jun 2024) = **115,00**.
- the composite clip (anchor `jutterswet-dubbelwacht`): 10/month through
  2024; borgplichtig January–July and aangesteld April–January intersect to
  April–July → 3×10 = **30**; disjoint members → **0**; one member without
  periods → **0**; split periods (January–March + June–September against
  February–August) → (February–March) + (June–August) = 3 months → **30**.
  The golden `jutterswet-tegenwacht` pins the negated member and the negated
  single clip.
- a part of a unit (`WV-3.6-naar-rato`; counted in whole units each gave
  **0**): ALEF's own case, `400 €/jr` until 2021-05-15 and `500 €/jr` after,
  on a year grid, clipped to [2021-04-01, 2021-07-21) → 44 × 400/365 +
  67 × 500/365 = **140 €**; `120 €/jr` on 2024 clipped to
  [2024-09-28, 2024-10-03) → 120 × 5/366 = **100/61 €**; `100 €/mnd` in
  January 2024 clipped in [2024-01-01, 2024-01-15) and
  [2024-01-15, 2024-02-01) → 100 × 14/31 + 100 × 17/31 = **100 €**, and the
  same with a plain 100 → **100**. On the storm-year grid from 1 October,
  100 on [2023-10-01, 2025-10-01) clipped to [2024-09-28, 2024-10-03) →
  100 × 3/366 + 100 × 2/365 = **6090/4453** (the storm year before
  2024-10-01 holds 29 February; `corpus/golden/jutterswet-vast-startpunt`
  case 1 rounds it to 1,37). End to end (suite fixtures `partial-unit`): 30
  per month over April to June clipped to [2024-04-16, 2024-06-01) →
  30 × (15/30 + 1) = **45**, and the total of a timeline a rule writes, 30
  indien hij wachtplichtig is, over the same period → **45**.
- gedurende "≠ 1000" where the tested timeline has leeg periods: leeg
  **satisfies** ≠ against a filled bound (the inequality row of ch. 1.2, per
  day): value 100 on [2024-01-01, 2024-07-01); tested timeline 1000 on
  [2024-01-01, 2024-02-01), leeg elsewhere → ≠ 1000 holds feb–jun → 5×100 =
  **500,00**.
- an explicit rate unit: `10 €/mnd` on [2024-01-01, 2025-01-01) → 12 months
  × 10 €/mnd = **120 €** — the denominator cancels, so a `€`-declared target
  takes it unchanged.
- the same timeline declared `€/jr`, in a document that declares
  `jr = 12 mnd` → the 12 months are one year → **10 €**. Declared `€/bm` with
  `bm` canonically a month (factor 1) → **120 €**, and over one month bucket
  → **10 €**.
- the same `10 €/mnd` value on a grid per **quarter** across 2024 → 4
  quarters × 3 months × 10 = **120 €**: a coarser grid gives the same
  number, because the conversion is exact.
- `10 €/dg` on a grid per month across 2024 → the calendar: the twelve
  months hold 366 days, so **3660 €** (`WV-3.6-g-vreemde-noemer`). A document
  that declares `mnd = 30 dg` is ill-formed (WF-009); with that factor the
  same timeline would have integrated to 12 × 30 × 10 = 3600 €.
- `7 €/wk` over February 2024 on a grid per month → 29 days, 29/7 weeks →
  **29 €** (`WV-3.6-kalender-week-over-maanden`); `31 €/mnd` over the 31
  days of January on a day grid → **31 €**, `91 €/kw` over the 91 days of the
  first quarter of 2024 → **91 €**.
- the calendar across a turn of the year: `366 €/jr` on a day grid over
  [2027-12-31, 2028-01-02) → 366/365 + 366/366 = **731/365 €**
  (`WV-3.6-kalender-jaargrens`).
- formulation B of the anchor casus (`WV-3.6-kalender`, the anchor
  `corpus/golden/anchor/jutterswet-verzuimrente-dagen`): `500 €/jr` on the 54
  days [2028-02-11, 2028-04-05) of the leap year 2028 → 500 × 54/366 =
  **73 47/61 €** (with `jr = 365 dg` it would be 73 71/73 €); `400 €/jr` on
  77 days and `500 €/jr` on 40 days of 2025 and 2026 → 400 × 77/365 +
  500 × 40/365 = **139 13/73 €** (`WV-3.6-kalender-geval-1`). The anchor pins
  both exactly, in targets of the unbounded form; the golden
  `corpus/golden/jutterswet-verzuimrente-dagen-afgerond` rounds them to the
  cent, 139,18 € and 73,77 €.
- the per-element fold of §7.1, three members of a set with `€/mnd`
  **timelines** per month: `10` over [2024-01-01, 2025-01-01) → 120 €, `5`
  over [2024-01-01, 2024-07-01) → 30 €, and a member without a timeline →
  0 €. Total **150 €**. Replace any one member's timeline by the
  asOfDate-bucket scalar and the fold is **refused**, naming that member; all
  members absent → **0**, under `de som van` too; an empty set → **0** (and
  `de som van` → leeg).

*v0 mirror note:* the totaal of a multi-period timeline is refused with the
grid (ch. 3.2), never weighted as though its periods were single units.

### 3.7 The time-proportional part (tijdsevenredig deel)

Ch. 2.8 gives the equation; per bucket of the target's grid (month or year,
ch. 3.2):

```
deel on bucket B = read(base, start(B)) × trueDays(B) / days(B)
```

- `days(B)` = the bucket's actual calendar day count (leap-aware: 28, 29,
  365, 366 — never 360).
- `trueDays(B)` = the number of days of `B` in the periods of the selecting
  condition (ch. 3.3), half-open, summed over every true interval that
  touches `B`. The condition is a check of an own time-dependent
  characteristic (WF-056), fenced as the clip of ch. 3.6 is (WF-097).
- `read(base, start(B))` — the base must be constant across the bucket,
  which the rule that the base's grid is coarser than or equal to the
  target's guarantees. A base that is **leeg at the start of the bucket
  writes no segment** for that bucket (a gap, not a 0), and a bucket with
  `trueDays` = 0 writes nothing. The base is a Number (WF-091, `IR-SPEC.md`
  ch. 4.1).
- A base with a rate unit is restated **first**, by its exact factor (a
  `€/jr` base for a target per month: ÷ 12 exactly), then prorated.
- A base or a target on a fixed or variable start point is not defined
  (ch. 3.2, 3.12).
- The fraction is exact and is **never rounded** to the target's declared
  decimals on its own: rounding happens only at an explicit rounding. The
  part into a timeline target stands outside the precision gate of ch. 2.4
  point 4, and the write boundary holds per segment (ch. 3.11, with the
  worked example `WV-3.7-schrijfgrens-*`). *v0 mirror note:* v0 stores a
  part that does not terminate at 16 significant digits (DECIMAL64); the
  exact fraction is normative.

Worked examples (absolute): base per year €12 (2024) / €18 (2025),
condition true [2024-01-01, 2025-02-08), monthly target: twelve months of
€12, jan 2025 = €18, **feb 2025 = 18 × 7/28 = 4,50** (7 true days of 28; 2025
is no leap year), nothing from March on; in the canonical form of ch. 3.1 the
twelve months of 2024 are the one segment [2024-01-01, 2025-01-01) = 12, since
whether the part writes them as one segment or as twelve is representation.
Yearly target, base €18, 38 true days in 2025 → 18 × 38/365 = **684/365**
exact (≈ 1,873972…, only an explicit rounding collapses it). Rate base
120 €/jr into a monthly target → 10 €/mnd; 5 €/jr → **5/12 €/mnd** exact (v0:
0,4166666666666667 at its 16-digit store — the exact division by 12, never a
pre-rounded ×0,0833 reciprocal).

### 3.8 Day counting and the calendar arithmetic of dates

**The day count: "aantal dagen in (de maand / het jaar)".** The day count
buckets the periods of its condition (ch. 3.3) on the target's grid — the
calendar grid, or the grid a fixed start point lays (ch. 3.2) — and gives
every day of a bucket the number of days of that bucket on which the
condition holds:

```
read(dagentelling(c), d) = | { e ∈ B : c holds on e } |      B the bucket that holds d
```

Several true intervals in one bucket add up, and a bucket without a true day
reads **0**, over all time (typ. §4.21): the 0 reaches before the first
bucket with a true day and past the last, and where no bucket has one the
count is 0 on (−∞, +∞). How the count divides a run of equal values into
segments is representation (ch. 3.1). The operator's stated bucket ("de
maand"/"het jaar") must equal the target's granularity. Worked example
(absolute): true on [2024-01-01, 2024-01-02) and [2024-01-10, 2024-02-14) →
**0** until 2024-01-01, **jan = 1 + 22 = 23**, **feb = 13**, **0** from
2024-03-01, four segments in the canonical form.

*The condition* is a check of an own time-dependent characteristic, or an
`all` composite of one such check and day-kind tests of the counted day
(`IR-SPEC.md` WF-058). In the condition of a day count the bound day
(`BoundDay`, the day-kind forms below) is the counted day: a day the tests
reject is not counted. A day-kind test names a declared day kind, which is a
function of the day alone (WF-031); ALEF admits only its own standard kinds
here.

*The unit* (§7.2, typ. §4.21): the count is days per unit of its bucket,
dg/mnd or dg/jr, where its target declares a unit, which MUST be a day unit
over the unit of the bucket (WF-058); in a target without a unit it is the
bare number.

Worked example `WV-3.8-nul-en-dagsoort` (absolute; `ref/` tests verbatim,
the fixture `ref/test/fixtures/conditions-per-period/nul-en-dagsoort`).
`rentedragend` holds on [2028-02-11, 2028-04-05); the day kind `schrikkeldag`
is day 29 of month 2; "het aantal dagen in de maand dat er aan alle volgende
voorwaarden wordt voldaan: hij is rentedragend, de dag is geen schrikkeldag"
in a target with the unit dg/mnd: **0** until 2028-02-01, **18** on
[2028-02-01, 2028-03-01), **31** on [2028-03-01, 2028-04-01), **4** on
[2028-04-01, 2028-05-01), **0** from 2028-05-01, each in dg/mnd. Without the
day-kind test February is **19**. The anchor `jutterswet-dagentelling`
carries the first worked example verbatim plus the split at a year
boundary; the golden `jutterswet-verzuimrente-30-360-afgerond` carries the
day-kind test and the unit, and its anchor
`anchor/jutterswet-verzuimrente-30-360` the exact maandrente. A count over
all time decides on every day; what that means for a write is ch. 3.11.

*v0 mirror note:* v0's day count writes no segment in a bucket without a
true day; the 0 over all time, the day-kind test and the unit are tracked
deltas.

**tijdsduur van A tot B** counts *whole* declared units between two dates,
signed (negative when B < A); any leeg operand → leeg, never 0 (ch. 1.2). A
is **evaluated** before B, in the order the phrase names them (ch. 5.11). The
absolute variant drops the sign. "hele" in the surface phrase only types the
*result* as a whole number — the count is always whole units, written "hele"
or not. The calendar is the **proleptic Gregorian calendar in zoneless civil
time**: no time zones, no DST, no leap seconds — a date is a civil (y, m, d)
triple, bijective with its epoch day number (days since 1970-01-01). The
whole-unit definition is the **packed-month rule** (v0 mirror, exact):

```
epoch(D)  = civil days since 1970-01-01
packed(D) = (12·year(D) + month(D) − 1)·32 + day(D)

dagen     = epoch(B) − epoch(A)
weken     = dagen quot 7
maanden   = (packed(B) − packed(A)) quot 32
kwartalen = maanden quot 3
jaren     = maanden quot 12
```

`quot` is integer division truncating **toward zero**, so a negative span
truncates toward zero, never toward −∞. Worked examples (absolute, pinned):

- **Leap child:** 29-02-2004 tot 28-02-2026 = **21** whole years (packed
  difference 8447, quot 32 = 263 months, quot 12 = 21); 29-02-2004 tot
  01-03-2026 = **22** (8452 quot 32 = 264 months).
- **Clamp, not inverse:** 31-01-2024 + 1 maand = 29-02-2024 (the clamp
  below), yet tijdsduur van 31-01-2024 tot 29-02-2024 = **0** whole months
  (packed difference 30 < 32): counting whole units is *not* the inverse of
  date-plus-unit arithmetic.
- **Signed truncation:** 10-03-2025 tot 10-08-2024 = −7 whole months; in
  whole quarters −7 quot 3 = **−2** (toward zero); in whole years **0**; the
  absolute variant reads 7 months.

The anchor `jutterswet-jutleeftijd` carries these three examples verbatim,
with the leeg corner.

**The time-unit spelling table (normative).** A duration's unit is the
declared unit of its static type — for date ± duration the duration
operand's type, for tijdsduur the node's own result type — recognised by
**spelling**, case-insensitive, keyed on the unit's written abbreviation.
Declared conversion factors and the declaring system play no part in
calendar arithmetic (a unit may name no system): a year is a calendar year,
never a count of seconds. Nine units:

| Unit | Recognised spellings |
|---|---|
| jaar | jaar, jr, jaren |
| maand | maand, mnd, maanden |
| kwartaal | kwartaal, kw, kwartalen |
| week | week, wk, weken |
| dag | dag, dg, dagen |
| uur | uur, u, uren |
| minuut | minuut, min, minuten |
| seconde | seconde, sec, s, seconden, secondes |
| milliseconde | milliseconde, ms, milliseconden, millisecondes |

The first five are day-precision units; the last four are sub-day units and
demand millisecond-precision (datum-tijd) operands. A spelling outside the
table is a declared day kind (the day-kind forms below) or ill-formed
(WF-035/WF-038). The day family and the month family of the calendar
(ch. 3.2) are the first five, by this table.

**Thirty spellings, and every one of them is a calendar unit.** The table is
read as a whole: nine units, thirty spellings, and no spelling in it is
available for anything else. Three of the thirty are easy to read past. `s` is
a spelling of the second, one character long. `secondes` and `millisecondes`
are second plural forms, beside `seconden` and `milliseconden`; they are not
abbreviations, and a reader who sorts the thirty into "singulars, plurals and
abbreviations" loses exactly those two. The ten abbreviations (`jr`, `mnd`,
`kw`, `wk`, `dg`, `u`, `min`, `sec`, `s`, `ms`) plus those two are the twelve
that are neither the singular nor the `-en` plural, and **WF-017 refuses all
thirty alike** as a day-kind spelling — as a day kind's `name` and as its
`plural` (`IR-SPEC.md` ch. 2.9). The twelve stay ordinary calendar units
wherever a unit is written; golden `jutterswet-vervaltermijn` carries `wk`,
`mnd` and `kw` that way.

**The lookup order is normative: this table first.** "Outside the table" is a
test on the table, so a consumer that resolves a duration's unit spelling
consults this table first, and the spellings of the declared day kinds (their
`name` and `plural`, `IR-SPEC.md` ch. 2.9) only where the table has nothing.
A consumer that asked the day kinds first would, on a document that
registered a calendar spelling as a day-kind unit, walk a day kind where the
table steps in calendar units; WF-017 refuses such a document, and the order
keeps a consumer from acting on one either way.

**"Case-insensitive" is read as written.** A spelling is trimmed and
lower-cased before the lookup, in every reader — the identity of ch. 2.1
under which this table is compared. `WK` is the week.

**Operand order and precision.** The date stands on the **left** of both plus
and min — a duration-left operand (duration + date, duration − date) is
ill-formed, as is date ± date (WF-035): v0's typing opens its date branch
only for a date-left operand, so plus does *not* commute. tijdsduur's two
operands must be dates of one precision (WF-038); mixed precision is
ill-formed. A fractional duration count **truncates toward zero** before the
calendar step (1,9 maanden steps 1 month; −1,9 truncates to −1); the wire
keeps the truncation, and golden `jutterswet-vervaltermijn` pins it, with the
month-end clamp, the standstill on a leeg duration and the quarter step. Empty
rules: ch. 1.2 — a leeg date propagates; a leeg duration counts as 0 (the date
passes unchanged); a leeg tijdsduur operand propagates, never 0.

#### The day-kind duration forms (§6.10, §6.11)

A **day kind** is not a unit on the spelling table: it is a declared
predicate over a calendar day (`IR-SPEC.md` ch. 2.9, ch. 3.7). Written where a
duration unit is written, it names *which days count*, and both duration forms
then walk the calendar — there is no closed formula, because no arithmetic
knows which days are salvage days.

**Resolution — spelling to predicate.** The duration's unit spelling is looked
up among the spellings of the declared day kinds — each day kind's `name` and
`plural` — **trimmed and case-insensitive**; one day kind carries it (WF-001
and WF-017 keep the spellings of two day kinds apart), and its name selects its
single `DayKindDefinition` (case-insensitive again; WF-031 keeps the
definition unique). A spelling that the calendar table does not know and no
day kind carries is ill-formed (WF-035/WF-038), and so is a unit of more than
one factor or with an exponent: only a declared unit has a spelling to look
up. The definition's `validity` is **not consulted**: it MUST be always,
neither bound written (WF-031).

**Evaluation — the predicate of one day.** `P_k(d)` is the definition's
`condition`, evaluated with **the bound day set to d** and nothing else in
scope. That binding is the runtime meaning of the `BoundDay` node
(`IR-SPEC.md` ch. 4.1): it is no session value — ch. 8.2's evaluation date is
— but the arbitrary day the definition is about and, in the condition of a
day count, the counted day (above); it denotes nothing anywhere else. An
executor that meets the bound day outside those two **faults**, naming the
form; it never reads it as leeg and never falls back on the evaluation date
(red case `context-value-de-dag-outside-day-kind`).

`P_k` is a function of **the day alone**. WF-031's purity clause makes that
true: the condition reaches no fact base and, by its position arm, no session
value, so the same document classifies the same calendar day alike in every
session. A definition may reference other day kinds (`P_k` calls `P_j` on the
same bound day, the §9.8 composition); the reference graph is acyclic
(WF-085), and an executor that meets a cycle faults rather than loop. The
composition is defined **on the bound day only**, the narrow route: a nested
test on another date ("… indien 15-03-2026 een ankerdag is") is a form without a meaning,
EF-022 (red case `day-kind-reference-other-day`; ch. 3.12). Its reading needs
a source for what such a test means inside a definition about an arbitrary
day. The day-kind predicate is evaluated inside a day-kind definition, on the
bound day, and in the condition of a day count, on the counted day; as the
condition of another rule ("… is een werkdag", §8.1.5) it has no meaning here
(ch. 3.12): a day kind is consulted as a classifier of stratum 0, never as a
derived read inside the fixpoint of the rules (ch. 5.4).

**§6.10 — the count.** "de tijdsduur van A tot B in hele werkdagen" counts the
days d with `A < d ≤ B` — **start exclusive, end inclusive** (the "A itself
does not count" rule, taken from §6.11's explicit reading, since §6.10 says
nothing about a day kind) — that satisfy `P_k`; signed by operand order, the
absolute variant dropping the sign; any leeg operand → leeg, never 0. The
interval bounds the work, so this form has **no fence**, and a day kind no
day satisfies counts **0** here: a count over an empty set is an answer,
where a step to a day that does not exist is not.

Worked examples (absolute; day kind = 25/26 December; each a `ref/` test
verbatim): 20-12-2024 tot 31-12-2024 → **2**; 25-12-2024 tot 26-12-2024 →
**1** (the 25th is the excluded start); reversed, 31-12-2024 tot 20-12-2024 →
**−2**, absolute variant **+2**; equal dates → **0**; leeg tot 31-12-2024 →
**leeg**; with a day kind no day satisfies, 20-12-2024 tot 31-12-2024 → **0**.

**§6.11 — the step.** "datum plus N werkdagen" steps one calendar day at a
time and counts only the days that satisfy `P_k`; the result is the Nth such
day, and is therefore itself a day of the kind. The **start date never
counts**, in either direction, even when it is itself a day of the kind.
`N = 0` — and a leeg N, which coalesces to 0 (ch. 1.2) — returns the start
date **unchanged**, even when that date is not a day of the kind: the source's
general zero rule wins over its "the result is always such a day" sentence,
which is about the stepping case. A fractional N truncates toward zero before
the walk. A negative N (or the `min` operator) walks backward — the mirror
image, which the source leaves unwritten. A leeg date propagates. The reach
fence below and the backward step are v1 additions: the source gives neither.

**The reach fence (normative).** A step that passes **40 000 consecutive
calendar days** — a little over 109 years — without finding a day of the kind
is a **fault** (EF-009), never an endless walk. It is a fence on the gap, not
on the total: "datum plus 5 000 werkdagen" walks some nineteen years and is
answerable, while a day kind no calendar day satisfies faults after 40 000
days (red case `day-kind-unsatisfiable-walk`).

**What the fence claims, and what it does not.** It is a limit of reach: the
walk looks a bounded distance ahead and reports that it did not get there. It
is **not a satisfiability test**, and neither the rule nor the fault says the
definition is unsatisfiable: the two are not the same thing, and a reader who
takes the fault as a diagnosis will look for a defect that may not exist. Two
bounds set the number. The longest recurrence gap a Gregorian day kind can
have is the leap day a century skips (2096-02-29 to 2104-02-29 is 2 921 days);
but a day kind may also be bound to a year or to a parameter — "the 1st or the
15th of 2040" is a well-formed, pure, satisfiable definition — and such a kind
lies whole decades past any date a case is likely to carry. A reach of ten
years refused those.

**The accepted residual cost.** A satisfiable day kind whose next occurrence
lies beyond the reach is refused although it exists: "the 1st of January 2200"
stepped from 2026 faults, however correct the definition. Three properties
make the trade acceptable. **The refusal of a step beyond the reach is a fault
and never a value**: no arm of this rule answers leeg, and red case
`day-kind-scope-outside-table` holds every executor to that. **The fence is
the same distance in every executor**, so how far a model may reach is a
property of the language and not of an engine. And **the distance is written
here**, so a model that needs more knows what it asks for; raising the number
is a change of this specification, not a setting.

Those three properties are about the fence; they do not claim that every
executor evaluates every well-formed day-kind condition. An executor that
does not carry a condition WF-031 admits refuses it by name, per form; it
never answers it otherwise.

Worked examples (absolute; day kind = 25/26 December; each a `ref/` test
verbatim): 20-12-2024 + 2 → **26-12-2024**; 25-12-2024 + 1 → **26-12-2024**
(the start is of the kind and still does not count); 26-12-2024 + 1 →
**25-12-2025**; 20-12-2024 + 0 → **20-12-2024** (not itself of the kind);
20-12-2024 + leeg → **20-12-2024**; leeg + 2 → **leeg**; 27-12-2024 − 1 →
**26-12-2024**; 25-12-2024 − 1 → **26-12-2023**; with a day kind no day
satisfies, 20-12-2024 + 1 → **fault**. Golden `jutterswet-bergingsklok`
carries the step forward and backward, the zero and leeg counts, and the
signed and absolute counts.

**The datum-tijd layer.** The two forms are not in the same position over
millisecond-precision operands.

- **`datum-tijd ± N <dagsoort>` is defined.** The day kind is judged on the
  **civil day part alone**, and after the Nth matching day is found the
  **original local clock time rides through unchanged**. Zero, leeg,
  negative and the reach fence follow the day-precision rules without
  exception — including the leeg count, which returns the datetime with its
  clock time, as it returns the date. Worked examples (day kind = 25/26
  December; each a `ref/` test verbatim): 2024-12-20T09:15:30 + 2 →
  **2024-12-26T09:15:30**; 2024-12-25T23:59:59 + 1 → **2024-12-26T23:59:59**
  (the start is of the kind and still does not count, and 23:59:59 is kept
  across the day step); 2024-12-26T09:15:30 − 1 → **2024-12-25T09:15:30**;
  2024-12-20T09:15:30 + 0 → **unchanged**; + leeg → **unchanged**; leeg + 2 →
  **leeg**. Golden `jutterswet-getijklok` carries the step, the clock time
  kept.
- **`tijdsduur van A tot B in hele <dagsoort>` over two datum-tijden has no
  meaning here** (ch. 3.12): counting whole days of a kind between two
  instants needs a rule for the part of a day at each end, and this chapter
  fixes none. An executor refuses it by name, and the **precision test runs
  before the empty rules** — a leeg operand gets the refusal that names the
  form, not the leeg rule of a form without a meaning, so the same document
  cannot answer leeg or fault depending on which operand a case fills.

**Date ± whole calendar units** clamps to the month end: date + n maanden
advances the proleptic month (12·year + month − 1) by n and clamps the day to
the target month's length — 2024-01-31 + 1 maand = **2024-02-29** (leap),
2025-01-31 + 1 maand = **2025-02-28**; a quarter is three months, a year
twelve (so a leap birthday clamps the same way); a day or week count is linear
(+n / +7·n epoch days). A fractional count truncates toward zero. (Empty rules:
ch. 1.2 — a leeg date propagates, a leeg duration counts as 0.) A step whose
day lies outside the representable range of a date (below) is a fault
(EF-007), never a silent wrap-around: 2024-01-01 + 60 000 000 000 dagen =
**fault**, + 2 000 000 000 maanden = **fault** (corpus/red
`date-step-outside-range`).

**The datum-tijd millisecond layer.** A millisecond-precision date is its
millisecond count since 1970-01-01T00:00:00 in the same zoneless civil time —
no time zones, no DST, no leap seconds, so every civil day is exactly
86 400 000 ms. Its civil day and its time of day split by floor division: the
time of day is always 0 ≤ msod < 86 400 000, also left of the epoch. Datetime ±
duration follows the date rules above, per unit family:

- **Sub-day units are linear on the ms value**: datetime ± n
  uur/minuut/seconde/milliseconde moves n·3 600 000 / n·60 000 / n·1 000 /
  n·1 ms, across day boundaries freely.
- **Calendar units act on the civil day part, keeping the time of day**:
  datetime ± n dagen/weken/maanden/kwartalen/jaren steps the day part exactly
  as a day-precision date (linear days and weeks; the month-end clamp) and
  attaches the time of day again unchanged.

tijdsduur van A tot B over two datetimes (one precision, WF-038) counts whole
units:

- in a **sub-day unit**: the ms difference quot the unit's ms size — linear,
  signed, truncating toward zero;
- in a **day-based unit**: the **borrow rule**: when B's civil day lies ahead
  of A's but B's time of day lies *strictly* before A's, the last day is not
  complete — one day is borrowed back off B's day part before the
  day-precision whole-unit count above (epoch days, packed months, quot);
  mirrored on the negative side (B's day behind A's, B's time of day strictly
  after A's: one day forward). Equal times of day borrow nothing.

Empty rules are those of ch. 1.2: a leeg datetime propagates; a leeg duration
counts as 0 (the datetime unchanged); a leeg tijdsduur operand propagates,
never 0. A fractional duration count truncates toward zero before the step, as
at day precision. A step that leaves the representable millisecond range is a
fault, never a silent wrap-around (the range and the order of the checks
below). Worked examples (absolute, pinned; each a `ref/` test verbatim):

- **Sub-day linearity:** 2024-02-28T23:30:00 + 26 uur =
  **2024-03-01T01:30:00** (across the leap day); 2024-03-01T00:15:00 −
  30 minuten = **2024-02-29T23:45:00**; 2024-01-01T00:00:00 + 1500
  milliseconden = **2024-01-01T00:00:01.500**.
- **The time of day kept with the clamp:** 2024-01-31T09:30:00 + 1 maand =
  **2024-02-29T09:30:00**; 2024-02-28T23:30:00 + 1 maand =
  **2024-03-28T23:30:00**, and + 2 weken = **2024-03-13T23:30:00**.
- **The borrow corner:** tijdsduur van 2024-02-28T23:30:00 tot
  2024-03-01T10:15:00 in hele uren = **34** (48 h − 13 h 15 min = 34 h 45 min,
  whole 34); in hele dagen = **1**, not 2 — B's 10:15 lies before A's 23:30, so
  the second day is incomplete. Counting the bare day parts would read 2: the
  silent error the borrow rule pins.
- **Borrow across a month count:** tijdsduur van 2024-01-31T10:00:00 tot
  2024-03-31T09:59:00 in hele maanden = **1** (the borrow steps B back to
  2024-03-30; packed difference 63, quot 32 = 1); one minute later, tot
  2024-03-31T10:00:00, = **2** (equal times of day, no borrow; packed
  difference 64).
- **A negative span:** tijdsduur van 2024-03-01T10:15:00 tot
  2024-02-28T23:30:00 in hele uren = **−34**; in hele dagen = **−1** (the
  mirrored borrow); the absolute variant reads 34 and 1.
- **Empty rules:** leeg + 26 uur = leeg; 2024-02-28T23:30:00 + leeg uur =
  2024-02-28T23:30:00; tijdsduur van leeg tot 2024-03-01T10:15:00 in hele uren
  = leeg.
- **Out of range:** 2024-01-01T00:00:00 ± 2 000 000 000 000 uur = **fault**,
  and + 60 000 000 000 dagen = **fault** — the step leaves the ms range; a
  wrap-around never stands in for a datetime.

Golden `jutterswet-getijdenpaar` carries the borrow corner.

**The representable millisecond range (normative).** A datum-tijd value is a
whole number of milliseconds since 1970-01-01T00:00:00 in the closed interval
[−2⁶², 2⁶² − 1], that is −4 611 686 018 427 387 904 to
4 611 686 018 427 387 903 ms: from −146136543-09-08T08:23:32.096 to
146140482-04-24T15:36:27.903 in the proleptic Gregorian calendar, about 146
million years either side of the epoch. The bound belongs to the language,
not to an engine: an executor with wider integers still faults outside it, and
one with narrower integers refuses by name where it cannot follow. A datum-tijd
step is checked before it is taken and faults (EF-007), never wraps around,
when the duration's whole count (after truncation toward zero) lies outside
the interval; in a sub-day unit, when the step itself (the count times the
unit's length in milliseconds) or the instant it reaches lies outside it; in a
calendar unit, when the instant it reaches lies above the interval or the
midnight of the civil day it lands on lies below it. A date construction faults
the same way when the instant its parts name lies outside the interval ("Date
construction" below).

Worked example (absolute; `ref/` tests `WV-3.8-dt-bereik-grens`,
`WV-3.8-dt-bereik-grens-plus-een` and `WV-3.8-dt-bereik-telling`):
2024-01-01T00:00:00 + 1 281 023 420 655 uur = **146140482-04-24T15:00:00**,
and one hour more = **fault**; the same instant + 18 446 744 073 709 551 617
dagen (2⁶⁴ + 1) = **fault**: the count itself is out of range and never wraps
to 1. No intermediate of a step wraps either (`ref/` tests
`WV-3.8-dt-bereik-kwartalen`, `WV-3.8-dt-bereik-min-min-int`,
`WV-3.8-dt-bereik-bodem-min-dag`; corpus/red `date-time-step-wrap`):
2024-01-01T00:00:00 + 3 074 457 345 618 258 603 kwartalen = **fault** (three
times the count lies outside the interval), − −4 611 686 018 427 387 904
milliseconden = **fault** (a step of 2⁶² ms forward), and from
1970-01-01T00:00:00 − 1 281 023 894 007 uur = −146136543-09-08T09:00:00, one
dag back from there = **fault** (its civil day lies below the interval).

**The representable range of a date (normative).** A date without a time
(day precision) is a civil day whose midnight lies in the millisecond range
above: the epoch days −53 375 995 583 to 53 375 995 583, that is
−146136543-09-09 to 146140482-04-24 (decided 2026-10-09). A date step, a date
construction and an Easter Sunday whose day lies outside are a fault (EF-007),
never a wrap-around and never a date; an executor whose integers cannot reach
that far refuses by name where it cannot follow, as for a datum-tijd. Worked
example (absolute; `ref/` tests `WV-3.8-d-bereik-*`): 2024-01-01 +
53 375 975 860 dagen = **146140482-04-24**, one day more = **fault**;
2024-01-01 − 53 376 015 306 dagen = **−146136543-09-09**, one day more =
**fault**; 2024-01-24 + 1 753 661 499 maanden = **146140482-04-24** and from
2024-01-25 = **fault**; datum met jaar 146140482, maand 4, dag 24 =
**146140482-04-24**, dag 25 = **fault**, jaar 200000000 = **fault**; eerste
paasdag van 146140482 = **146140482-03-29**, van 4 294 969 320 = **fault**
(corpus/red `date-step-outside-range`, `date-with-outside-date-range`).

**Date-part extraction and the Easter builtin.** Year, month and day
extraction ("het jaar / de maand / de dag uit") read the **civil components
of the proleptic Gregorian calendar in zoneless civil time** — the same
(y, m, d) triple the whole-unit definition above is built on — from a
day-precision date, each as a **unitless whole number**: the year as written,
the month **1..12**, the day of month 1..the month's length. A leeg date
propagates (ch. 1.2: jaar/maand/dag uit leeg = leeg). "De eerste paasdag van"
a **whole, unitless** year (WF-038) is Easter Sunday of that year by the
Gauss/Anonymous algorithm, normative verbatim (`quot` and `mod` are integer
division truncating toward zero and its remainder, as in the packed-month rule
above):

```
a = j mod 19,   b = j quot 100,  c = j mod 100
d = b quot 4,   e = b mod 4
f = (b + 8) quot 25,   g = (b − f + 1) quot 3
h = (19·a + b − d − g + 15) mod 30
i = c quot 4,   k = c mod 4
l = (32 + 2·e + 2·i − h − k) mod 7
m = (a + 11·h + 22·l) quot 451
maand = (h + l − 7·m + 114) quot 31
dag   = ((h + l − 7·m + 114) mod 31) + 1
```

The result is the day-precision civil date (j, maand, dag). A leeg year
propagates; a non-whole or unit-carrying year value is a fault, never a
silent truncation to whole, and a year whose Easter Sunday lies outside the
representable range of a date is EF-007 (above), never a silent wrap-around.
*v0 mirror note:* v0 narrows the year to a machine int silently; the verifier
makes the non-whole or unit-carrying static shape ill-formed, and the fault out
of range is a runtime fault only, since no static check can bound a computed
year (a tracked delta).
Worked examples (absolute, pinned; each a `ref/` test verbatim): jaar uit
2026-11-03 = **2026**; maand uit 2026-11-03 = **11**; dag uit 2026-11-03 =
**3**; eerste paasdag van 2024 = **2024-03-31**, van 2026 = **2026-04-05**, van
2027 = **2027-03-28**, van 2038 = **2038-04-25**. Golden `jutterswet-paasvondst`
carries the class: a maand-uit disjunction, an eerste-paasdag∘jaar-uit date
equality, a jaar-uit/dag-uit Gelijkstelling pair, and the leeg corner.
Datum-tijd operands of the extractors and the sub-day extractors are not
defined.

**Date construction.** "De datum met jaar: …, maand: … en dag: …" (`dateWith`)
takes three parts, year, month and day, in that order; "de datumtijd met jaar:
…, maand: …, dag: …, uur: …, minuut: …, seconde: … en milliseconde: …"
(`dateTimeWith`) takes seven, the hour, minute, second and millisecond after
them. Every part is evaluated, in argument order (ch. 5.11), before a leeg part
makes the result **leeg** (ch. 1.2). Each part is a **whole, unitless** number:
a non-whole or unit-carrying part is a fault, never a truncation, as for the
Easter year. The result is the civil date (year, month, day) of the proleptic
Gregorian calendar in zoneless civil time — the triple the part extractors
above read — when the parts name one: the month 1..12 and the day 1..the
month's length (28, 29 in a leap year, 30 or 31); for `dateTimeWith` also the
hour 0..23, the minute 0..59, the second 0..59 (no leap second) and the
millisecond 0..999, and the instant, that day's midnight plus the clock time,
lies in the representable millisecond range above. **Parts that name no
calendar day or no clock time are a fault**: never a roll-over into the next
month (31 April is not 1 May), never a clamp to the month's end (31 April is
not 30 April either — the month-end clamp belongs to date ± duration, a step,
not to the construction), and never leeg. A year outside the years of the
representable range is tested first, a date's day after the calendar, and a
datum-tijd's instant last: parts that name no day or no clock time are the
fault kind EF-032, with the parts as its parameters; a day outside the
representable range of a date, and an instant outside the representable
millisecond range, are the range's own kind, EF-007 (ch. 5.7b). The rule for parts that name
no day is a provisional decision. Worked examples (absolute, pinned; each a
`ref/` test verbatim): datum met jaar 2026, maand 3, dag 1 = **2026-03-01**;
2024, 2, 29 = **2024-02-29**; 2000, 2, 29 = **2000-02-29**; 2026, 2, 29 =
**fault** (2026 is no leap year); 1900, 2, 29 = **fault** (1900 is divisible
by 100 and not by 400); 2026, 4, 31 = **fault**; 2026, 13, 1 = **fault**;
2026, 0, 1 = **fault**; 2026, 3, 0 = **fault**; maand 2,5 = **fault**; jaar
2026 jr = **fault**; jaar leeg = **leeg**; 2026, 13, leeg = **leeg** (the leeg
test comes before the calendar); datumtijd met 2026, 3, 1, 6, 30, 0, 0 =
**2026-03-01T06:30:00**; 2026, 12, 31, 23, 59, 59, 999 =
**2026-12-31T23:59:59.999**; uur 24, seconde 60 or milliseconde 1000 =
**fault**; jaar 146140482, maand 1, dag 1, 0:00 = **146140482-01-01T00:00:00**
and jaar 200000000 = **fault** (EF-007); the two ends of the range to the
millisecond, 146140482-04-24T15:36:27.903 and −146136543-09-08T08:23:32.096,
are instants, and one millisecond past either is **fault** (EF-007). Golden
`date-with-parts` (the witness 15-03-2026 → 01-03-2026, a datum-tijd, two leap
years and the leeg corner), red cases `date-with-no-calendar-day` (EF-032) and
`date-time-with-outside-range` (EF-007).

### 3.9 The whole-period qualifier (§8.2)

"gedurende de gehele maand / het gehele jaar" turns a condition per day into
a condition per bucket: a bucket satisfies it when the condition holds on
every day of the bucket. The qualifier has two readings, and the target of the
rule decides between them. In the condition of a rule with a timeline target,
which is read per period (ch. 3.10), it is a truth **per bucket** (below,
"Per bucket on a timeline target"). Everywhere else it **folds** to one
boolean:

```
geheel(tl, grid, toets) = for EVERY bucket B touched by tl:
                            satisfied-days(B, toets) ≥ days(B)
```

**The test per day.** `satisfied-days(B, toets)` is the number of days of B on
which the test holds, the test read per day by ch. 1.2. A day on which the
tested timeline reads leeg — a gap day, or a day under an explicit leeg
segment (ch. 3.1) — has a leeg side, and so has every day where the bound of a
comparison is leeg; a leeg bound is never a fault (§8.2 with typ. §5.3 and
§5.6):

- an **ordering** (`<`, `≤`, `>`, `≥`, eerder, later) is onwaar on a leeg
  side; "gelijk aan" between numbers holds on two leeg sides and is onwaar on
  one; "ongelijk aan" holds on exactly one; a date takes the date row of
  ch. 1.2, where two leeg dates are not equal;
- the **gevuld** test holds on the days the timeline reads a value, the
  **leeg** test on the days it reads leeg;
- the **characteristic-holds** test holds on the days of the characteristic's
  true periods;
- the **inner negation** (`negated`) is per day, as everywhere (ch. 1.2): it
  complements the satisfied days and leaves the touched buckets as they are.
  Under the qualifier the negated leeg test is therefore the gevuld test and
  the negated gevuld test is the leeg test: the same touched buckets, the same
  threshold, the complementary days.

The threshold is days(B), the bucket's calendar length, for every test.
Where the temporal side of a comparison stands on the right ("40 is kleiner
dan zijn dagvergoeding gedurende het gehele jaar") the operator is
**reversed**, never negated. On totally ordered filled values the two
coincide; on a leeg operand they do not, because ch. 1.2 makes both orderings
false there, so a reader that negated instead of reversing would turn every
leeg day into a satisfying one.

**The buckets.** The grid is the **qualifier's own** — "de gehele maand" or
"het gehele jaar" — never the tested member's declared granularity: a
characteristic per month tested "gedurende het gehele jaar" folds over year
buckets, and its true periods stay precise to the day either way (ch. 3.1).
A bucket is **touched** when the tested timeline reads a value — for a
characteristic, holds — on at least one of its days. The touched buckets are
the timeline's, not those of the satisfying days: a value of 30 across the
whole of 2026, tested `> 40`, has no satisfying day, and a fold that took its
buckets from the satisfying days would touch no bucket and pass — the opposite
of the answer. An explicit leeg segment reads as a gap for every test, the
comparisons included: it touches no bucket, and its days read leeg as gap days
do; whether a producer writes such a segment is representation (ch. 3.1), and
a case input carries none.

**The fold** (a scalar target, and every position other than the condition of
a rule with a timeline target):

- A timeline that reads a value on no day — no segments at all, an absent
  member, a characteristic without true periods — **touches no bucket and
  passes every test vacuously**, and the qualifier's negation fails on it. A
  characteristic with no periods at all passes; that is not the same thing as
  the characteristic being false.
- A failed **to-one** navigation contributes the null timeline, which passes
  vacuously: the leeg-navigation discipline of ch. 1.2, lifted. This
  **departs deliberately** from the unqualified navigated characteristic
  check, whose mirrored v0 asymmetry answers false for the positive and the
  negated form alike (ch. 5.8): there the absent link decides the check, here
  the qualifier lifts it to the leeg-navigation discipline.
- Over a **to-many** range the condition is a quantifier fold (ch. 3.9a),
  whose matrix is this qualifier applied per member: conjunctive under `all`,
  and an empty set passes.
- The qualifier's negation (`WholePeriod.negated`, "niet gedurende …")
  complements the folded result, for every test.
- **The operands.** A comparison tests a time-dependent attribute of the
  subject against a scalar bound, the temporal side on the left or on the
  right (reversed, above). The operand of the leeg and the gevuld test is a
  time-dependent attribute of the subject, one reached over to-one hops
  (ch. 5.8), or a time-dependent parameter (WF-043). The characteristic-holds
  test reads a characteristic of the subject or one reached over to-one hops
  (the navigated worked examples below). A hop that reaches nothing
  contributes the null timeline, which passes.
- A comparison over a timeline reached over a path, between two timelines or
  without a timeline, and a **date-valued** whole-period comparison
  (eerder/later over a timeline of dates), have no reading here (ch. 3.12).
- **Source.** RegelSpraak writes the leeg and the gevuld test under the
  qualifier, with and without the qualifier's "niet". It has no spelling for
  the inner negation of either test, so a document that carries
  `negated: true` on them under the qualifier is written on the wire only,
  and its reading is the one above.

**This qualifier is not a scalar context, and the refusal of ch. 3.3 does not
reach it.** Ch. 3.3 refuses a time-dependent member in a scalar context and
names the exits in the same place: the temporal total (ch. 3.6) and this
qualifier. Ch. 8.2 states it as the rule of indexing ("the only bridges from a
timeline to a scalar are the dedicated collapses") and ch. 3.11 as the
boundary of a write. A condition that carries the qualifier **is** the second
collapse; refusing it as a scalar context would refuse the exit for being one.
The reading is normative for every executor: a qualified condition is answered
by the fold above, and dropping the qualifier to answer the unqualified
condition instead is a defect, not a choice of scope. For the
characteristic-holds test the fold reads the characteristic's true-period
timeline, since a time-dependent characteristic is delivered as periods and
nothing else (ch. 8.4).

Worked examples (absolute; a value timeline per year, tested ≥ 10): value 12
on the whole of 2024 → all 366 days of the touched bucket satisfy → **waar**.
Value 12 on [2024-01-01, 2024-07-01) only → the 2024 bucket is touched for 182
days but has 366 → **onwaar** (a July day carries no value); the negated form
→ waar. The same half-year timeline under the **leeg** test → every day of the
touched bucket must read leeg, but 182 of them carry 12 → **onwaar**.

Worked examples — the **ordering** test over a value timeline (absolute;
`ref/` tests verbatim; attribute `dagvergoeding` per month, qualifier
"gedurende het gehele jaar", tested `> 40`):

| # | value timeline | fold | verdict |
|---|---|---|---|
| a | no segments | no bucket touched | **waar** |
| b | 50 on [2026-01-01, 2027-01-01) | bucket 2026: 365 of 365 satisfy | **waar** |
| c | 30 on [2026-01-01, 2027-01-01) | bucket 2026: 0 of 365 | **onwaar** |
| d | 50 on [2026-01-01, 2026-07-01) | bucket 2026: 181 of 365 | **onwaar** |
| e | 40 on [2026-01-01, 2027-01-01) | strictly greater: 0 of 365 | **onwaar** |
| f | as e, tested `≥ 40` | 365 of 365 | **waar** |

Row e is the operator, not the fold: `>` is strict, and the whole-period
reading changes *when* the test is asked, never *what* it asks. The
qualifier's own negation complements each verdict — a → onwaar, b → onwaar,
d → **waar** — and under the **month** grid row d passes (January through June
are each touched in full) while a value on [2026-01-15, 2026-03-01) fails it
(January is touched for 17 of 31 days). A **leeg** segment inside the period
gives days that read leeg, and no ordering holds on a leeg side: 50 on the
first half of 2026 and explicit leeg on the second reads **onwaar** under
`> 40`, never a fault (ch. 1.2 — an ordering against leeg is false).

Worked examples of the explicit leeg segment (absolute; `ref/` tests
verbatim, under the year grid): 50 on 2025, explicit leeg on 2026 and 50 on
2027 → `> 40` **waar** and `≠ 40` **waar**, for 2026 is not touched; 50 on the
first half of 2026 and explicit leeg on the second → `≠ 40` **waar** (181 days
50 and 184 leeg days, 365 of 365) and `> 40` **onwaar** (181 of 365); explicit
leeg on 2026 alone → `> 40` **waar**, no bucket touched.

Worked examples of the leeg day in a comparison (absolute; `ref/` tests
verbatim, year grid; 2026 has 365 days): 50 on [2026-01-01, 2026-07-01) tested
`≠ 40` → 181 days 50 and 184 gap days, 365 of 365 → **waar**, and under "niet
gedurende …" **onwaar**; tested `≠ 50` → 184 of 365 → **onwaar**; tested
`= 50` → 181 of 365 → **onwaar**. Against a leeg bound: 50 on the whole of
2026 → `≠` 365 of 365 **waar**, `=` 0 of 365 **onwaar**; 50 on
[2026-01-01, 2026-07-01) → `≠` 181 of 365 **onwaar** (a gap day and the bound
are both leeg), `=` 184 of 365 **onwaar**. Every touched bucket holds a filled
day, which no leeg bound equals, so "gelijk aan" against a leeg bound fails
every touched bucket, and the date row and the numeric row give the same
verdicts here. The clip of ch. 3.6 reads a leeg day of the same comparison in
the same way, so "gedurende de tijd dat" and "gedurende het gehele" read a
leeg day alike. `corpus/golden/jutterswet-gehele-jaar-lege-grens` carries both
bounds, both gaps and the qualifier's negation.

Worked example `WV-3.9-lege-dag` (absolute; the `ref/` tests
`WV-3.9-lege-dag-*` verbatim). An attribute `peilstand` per month, a scalar
`drempelstand`, "gedurende het gehele jaar" over 2024 (366 days):

| # | `peilstand` | `drempelstand` | "ongelijk aan" | "gelijk aan" |
|---|---|---|---|---|
| K1 | 7 on [2024-01-01, 2024-02-01) | 5 | **waar**: 31 days 7 ≠ 5 and 335 gap days leeg ≠ 5, 366 of 366 | onwaar: 0 of 366 |
| K2 | 7 on [2024-01-01, 2025-01-01) | leeg | **waar**: 366 days 7 ≠ leeg | onwaar: 0 of 366 |

Worked examples — the **characteristic-holds** test over a *navigated*
characteristic (absolute; `ref/` tests verbatim). Document: a Schuit reaches
one Werf over the to-one role `werf`; `bedrijvig` is a characteristic of Werf
per month; the rule is *de toeslag van een Schuit is de lengte van de Schuit
indien de werf van de Schuit gedurende het gehele jaar bedrijvig is*.
Session: evaluation date 2026-03-15, S1.lengte 8.

| # | link | `bedrijvig` true periods | fold | toeslag |
|---|---|---|---|---|
| a | none written | — | null timeline, no bucket touched → **waar** | 8 |
| b | W1 | none | no segments, no bucket touched → **waar** | 8 |
| c | W1 | [2026-01-01, 2027-01-01) | bucket 2026: 365 of 365 → **waar** | 8 |
| d | W1 | [2026-01-01, 2026-07-01) | bucket 2026: 181 of 365 → **onwaar** | leeg |

The same document under the qualifier's own negation ("**niet** gedurende het
gehele jaar bedrijvig") complements each verdict: a → onwaar, b → onwaar, c →
onwaar, d → **waar** (8). Under the inner negation ("gedurende het gehele jaar
**niet** bedrijvig") the touched buckets are unchanged and the satisfying days
are their complement: a → waar, b → waar, c → onwaar (bucket 2026 satisfies 0
of 365), d → onwaar (184 of 365). Under the **month** grid ("gedurende de
gehele maand bedrijvig") row d passes — January through June are each touched
in full, 181 of 181 days — while a true period of [2026-01-15, 2026-03-01)
fails it (January is touched for 17 of 31 days).

Worked examples — the **leeg** and **gevuld** tests ("De bemanning van de
Schuit is gedurende het gehele jaar leeg / gevuld"; absolute; `ref/` tests
verbatim, and the rows of `corpus/golden/jutterswet-gehele-periode-leeg`;
attribute `bemanning` per month, qualifier "gedurende het gehele jaar"; 2025
and 2026 have 365 days each). In the formula above:

```
satisfied-days(B, gevuld) = the days of B on which tl reads a value
satisfied-days(B, leeg)   = the days of B on which tl reads leeg
                          = days(B) − satisfied-days(B, gevuld)
```

so the leeg test comes down to "the timeline reads a value on no day at all":
every day that reads a value lies in a touched bucket, which then holds a day
that is not leeg, and the grid does not change the verdict. An absent member
is "gedurende het gehele jaar leeg" and "gedurende het gehele jaar gevuld" at
once, and the qualifier's negation fails on it both ways.

| # | `bemanning` timeline | leeg | gevuld | niet leeg | niet gevuld | niet gedurende … leeg |
|---|---|---|---|---|---|---|
| a | no segments | **waar** | **waar** | **waar** | **waar** | onwaar |
| b | 4 on [2025-01-01, 2026-01-01) | onwaar | **waar** (365 of 365) | **waar** | onwaar | **waar** |
| c | 4 on [2025-01-01, 2025-07-01) | onwaar | onwaar (181 of 365) | onwaar | onwaar | **waar** |
| d | 4 on [2025-01-01, 2025-07-01), 5 on [2025-07-01, 2027-01-01) | onwaar | **waar** (365 of 365 in 2025 and in 2026) | **waar** | onwaar | **waar** |

Row c is where the inner negation and the qualifier's negation give different
answers: "niet leeg" asks for a value on every day of 2025 and fails on the
second half, while "niet gedurende het gehele jaar leeg" asks only that some
day holds one. Under the **month** grid the gevuld test passes on row c —
January through June 2025 each read a value on all their days — and on 4 on
[2025-07-01, 2026-07-01), which fails it under the year grid (184 of 365 in
2025, 181 of 365 in 2026), while 4 on [2025-01-15, 2025-03-01) fails it under
both grids (January holds 17 of 31) and fails the leeg test under both; that
timeline lies off the attribute's declared month grid, where no case input may
lie, so it is pinned in `ref/`'s suite alone. An **explicit leeg segment**
changes no verdict: leeg on [2025-01-01, 2026-01-01) alone touches no bucket
and reads as row a, and 4 on [2025-01-01, 2025-07-01) followed by leeg on
[2025-07-01, 2026-01-01) reads as row c (`ref/` tests).

**Per bucket on a timeline target.** In the condition of a rule with a
timeline target, which is read per period (ch. 3.10), the qualifier is a truth
per bucket of its own grid, the calendar month or year it names: it holds in
every bucket B every day of which satisfies the test, the leeg days of ch. 1.2
above included, and its negation ("niet gedurende …") holds in every other
bucket of all time — also in a bucket in which no day satisfies the test. The
notion of a touched bucket does not enter: each bucket of all time is judged,
and outside the periods of the tested timeline every bucket reads the same.
The inner negation is per day, as above. A whole-period qualifier MUST lie on
a grid that fits the target's (`IR-SPEC.md` WF-126): "gedurende de gehele
maand" on a target for every year is ill-formed. On a scalar target the fold
above stays (goldens `synth-gehelejaarkenmerk`, `jutterswet-ploegpremie`).

Worked example `WV-3.9-per-emmer` (absolute; `ref/` tests verbatim, the
fixture `ref/test/fixtures/conditions-per-period/afdracht`). `rentedragend`
holds on [2025-11-16, 2026-03-13). "Gedurende de gehele maand rentedragend"
per bucket holds on [2025-12-01, 2026-03-01) — December 2025, January and
February 2026 — and its negation on (−∞, 2025-12-01) and [2026-03-01, +∞). The
rule "de toetsdagen moet berekend worden als (het aantal dagen in de maand dat
hij rentedragend is) gedurende de tijd dat hij niet gedurende de gehele maand
rentedragend is" writes **0** until 2025-11-01, **15** on
[2025-11-01, 2025-12-01), **12** on [2026-03-01, 2026-04-01) and **0** from
2026-04-01: the day count is 0 in a month without a true day (ch. 3.8), and the
negation of the qualifier holds there. The fold would have read the qualifier
once, false for this timeline, and written all five months 15, 31, 31, 28 and
12. The corpus witness, written by rules through the RegelSpraak frontend, is
`corpus/golden/jutterswet-wachtmaanden` (a month qualifier on a characteristic
for every day, both polarities, and a year qualifier on a comparison).

### 3.9a The quantifier fold — a condition over a linked set

A **quantifier fold** (`IR-SPEC.md` ch. 4.5, wire `QuantifierFold`, v0
`Kwantorvouw`; WF-108) asks one condition of every member of a linked
instance set:

```
contributors = { m : m ∈ reach ∩ population,  filter(m) }
all   = for every m in contributors:       test(m)
none  = for every m in contributors:  not  test(m)
contributors = ∅  →  waar, for BOTH quantifiers

count          = | { m ∈ contributors : test(m) } |
atLeast n      =  count ≥ n
atMost n       =  count ≤ n
exactly n      =  count = n
```

`reach ∩ population` and `filter` are **ch. 3.4a's**, unchanged and shared:
the range (`sourceRole`, or the multi-step `sourcePath` of ch. 3.4b, exactly
one of the two) says which instances the quantifier reaches; `elementType`
says which it ranges over, extensionally (`IR-SPEC.md` ch. 2.2) — every
instance whose type is ⊑ it; and the die/dat `filter` drops contributors,
evaluated per contributor with the element as subject. There is one
definition of "which members", not two.

`test` is evaluated the same way — **per contributor, the element as the
condition's subject**. That is the whole difference between the two
condition fields: `filter` says which members the sentence is about, `test`
says what it claims about them. In both, a node with the form anchor reads the
form subject and an identity check compares the member with it (ch. 3.4a,
*The form anchor*); the worked example there carries a quantifier `all` whose
filter excludes the form subject and whose test compares each member's
ingangsdag with the form subject's (`voorrang`).

**`none` is the complement per member, never the fold's.** `not (all)` and
`none` are different conditions, and the difference shows on exactly the
members whose test is false for a reason of time: a member whose timeline
does not cover the period fails the matrix, so it does not block `all`'s
failure and it does not block `none` either.

**Order and short circuit** — two disciplines meet, each taken from the node
it belongs to. The **contributors are settled in full first**, filter
included, as an instance fold settles them (ch. 3.4a): the subselection
decides what the sentence is about, and that question is not short-circuited
by its answer, so a filter that faults on a later member faults whether or not
an earlier member has already decided the verdict. The **matrix then short
circuits**, as a composite condition does (ch. 5.10): evaluation stops at the
first contributor whose verdict settles the fold, in the contributors' own
order (ch. 5.11), and a member never reached produces no fault and no §9.5
channel fact.

**The vacuous case.** An empty contributor set reads **waar** under both
quantifiers. It is the empty conjunction, and it is the answer the empty set
gets everywhere else a universal appears in this document (ch. 3.9's empty
to-many range, ch. 5.10's `all` composite over no parts); it is written down
here because it is the rule of this node a reader is most likely to supply
from intuition and get backwards, and because the corpus pins it.

**The counting quantifiers count the contributors the matrix holds of**
(`IR-SPEC.md` ch. 4.5, WF-108 (e); the threshold is `Compound`'s field under
`Compound`'s rule, WF-041: at least 1). They are not a second kind of fold:
`all` is `count = |contributors|` and `none` is `count = 0`, so **one** rule
of evaluation covers all five, and it is ch. 5.10's — the contributors are
settled in full first, the matrix is then read in the contributors' own order
(ch. 5.11) and stops at the first one after which the outcome stands, with
ch. 5.10's trigger table applied verbatim, the contributors as its members.
Two of its rows fire on the **empty prefix** here as they do there:
`atLeast` *n* and `exactly` *n* over fewer than *n* contributors are
**onwaar** having read nothing, and `atMost` *n* over at most *n*
contributors is **waar** having read nothing.

**The empty set needs no separate principle under these three.** A count of 0
answers each of them by itself — `atLeast` *n* (n ≥ 1) onwaar, `atMost` *n*
waar, `exactly` *n* onwaar — so the vacuous case above is a rule about `all`
and `none` and is not extended to them.

**A NIET prefix on the matrix lands on the count, because a threshold cannot
be flipped.** Ch. 3.9's "niet gedurende …" qualifies the outer comparison, so
under `all` and `none` an executor may fold it by flipping the quantifier
(∀c.¬P = ¬∃c.P). No such move exists for `atLeast` 2: there is no quantifier
it turns into. The negation is therefore read where it is written, per
contributor, and what it changes is the count: `count(¬P) = |contributors| −
count(P)`, an equality and not an approximation, because the whole-period
predicate is total — every contributor satisfies it or does not, the member
with no timeline at all included (it passes vacuously, so it counts on the
positive side; ch. 3.9). The empty set stays 0 − 0 = 0. Witness
`corpus/golden/jutterswet-sleepvloot`, whose cells are chosen so that counting
P instead of ¬P gives the other verdict in four of them.

**Worked examples — the §8.2 form (absolute; `ref/` tests and
`corpus/golden/jutterswet-ploegpremie` verbatim).** Document: a
`Bergingsploeg` reaches its `bergers` over the to-many role of
`ploegindeling`; `dagvergoeding` is an attribute of `Berger` per month; the
rule is *een Bergingsploeg is premiegerechtigd indien de dagvergoeding van
alle bergers van de Bergingsploeg gedurende het gehele jaar groter is dan 40*.
The matrix is ch. 3.9's ordering test, asked of each berger's own timeline.

| # | the ploeg's bergers | verdict per member | fold |
|---|---|---|---|
| 1 | 50 and 60, each the whole of 2026 | waar, waar | **waar** |
| 2 | 50 and 30, each the whole of 2026 | waar, onwaar | **onwaar** |
| 3 | 50 the whole year; 60 on [2026-01-01, 2026-07-01) only | waar, onwaar (181 of 365) | **onwaar** |
| 4 | none | — | **waar** (vacuous) |
| 5 | 40, the whole of 2026 | onwaar (strictly greater) | **onwaar** |
| 6 | 50 the whole year; one berger with no timeline at all | waar, waar (no bucket touched) | **waar** |
| 7 | one berger, no timeline at all | waar (no bucket touched) | **waar** |

Row 3 is the gap and row 4 the empty set: a member the relation reaches but
whose timeline does not cover the period **fails**, while a set the relation
does not reach at all **passes**. A member with **no timeline at all** is the
third case and sides with row 4, not row 3: it touches no bucket, so ch. 3.9
passes it vacuously. That is ch. 3.9's rule applied unchanged, and it is
pinned so that an executor which collapses "absent" into "gap" fails by name.

**Under `none` that same member changes the sign, and the two vacuous
readings part.** An absent timeline makes the member satisfy its matrix
vacuously, so it satisfies what `none` says nobody satisfies: a ploeg whose
single berger has no `dagvergoeding` at all reads premiegerechtigd **waar**
and wisselvallig **onwaar**, while a ploeg with no bergers reads **waar** on
both. Vacuous at the member and vacuous at the set are different, and only the
second is the empty conjunction of this section; nothing about an empty set
follows from a member that happens to be empty. The corpus pins the pair side
by side.

Under **`none`**, on the same document: bergers at 30 and 20 the whole year →
**waar** (no member is above 40); 30 and 50 → **onwaar**; no bergers →
**waar**; a single berger at 50 on the first half of 2026 only → **waar**,
because he does not have more than 40 *gedurende het gehele jaar* either.
Rows 6 and 7 are where `none` turns over: both read **onwaar**, because a
member with no timeline satisfies the matrix vacuously and `none` denies
exactly that. Row 7 is pinned on its own — in row 6 the berger at 50 already
decides `none`, so that row cannot tell the empty member from the full one.

With a die/dat subselection — *alle bergers die een dagvergoeding van meer dan
10 hebben* — a berger at 5 the whole year is no contributor, so a ploeg of one
berger at 50 and one at 5 reads **waar**, and a ploeg whose only berger is at 5
reads **waar** vacuously.

**Worked examples — a non-temporal matrix** (absolute;
`corpus/golden/jutterswet-schouwploeg` verbatim). `test` is an ordinary
condition, so it need not read a timeline at all. Document: a `Schouwploeg`
reaches its `wachten` over the to-many role of `schouwindeling`; `dienstjaren`
is a plain attribute of `Strandwacht` and `gediplomeerd` a characteristic of
it. Four folds in one document: *ervaren* = `all` over `dienstjaren > 5`,
*onervaren* = `none` over the same, *bevoegd* = `all` over `gediplomeerd`,
*sluitend* = `none` over `niet gediplomeerd`.

| # | ploeg | the ploeg's wachten (dienstjaren, diploma) | ervaren | onervaren | bevoegd | sluitend |
|---|---|---|---|---|---|---|
| 1 | S1 | 8 ja, 6 ja | **waar** | onwaar | **waar** | **waar** |
| 2 | S1 | 8 ja, 3 nee | onwaar | onwaar | onwaar | onwaar |
| 2 | S2 | 9 ja | **waar** | onwaar | **waar** | **waar** |
| 3 | S1 | 3 ja, 5 ja | onwaar | **waar** | **waar** | **waar** |
| 4 | S1 | none | **waar** | **waar** | **waar** | **waar** |
| 5 | S1 | leeg ja, 3 ja | onwaar | **waar** | **waar** | **waar** |

Row 2 is `none` ≠ `not (all)` again, with no reason of time in the matrix: one
member above the bound and one below leaves both folds false. It also holds a
second ploeg in the same case, and that row is the **range**: S2's single
wacht is above the bound, so S2 reads *ervaren* while S1 does not, in one run
over one population. An executor that quantified over every `Strandwacht`
rather than over the ploeg's own `wachten` would draw S1's wacht at 3 into
S2's fold and read S2 the other way. Row 3 pins the strict bound, row 4 the
vacuous set under both quantifiers. Row 5 is the **leeg** member, the
counterpart of row 7 above: an ordering comparison over leeg is onwaar
(ch. 1.2), so the member **fails** its matrix — it breaks `all` and is no
witness for `none` — whereas an absent **timeline** passes its whole-period
matrix vacuously and therefore breaks `none`. Nothing but the matrix decides
which of the two readings applies, which is why both are pinned.

*sluitend* is the **negated matrix**, and it answers what *bevoegd* answers in
every row. On a non-temporal matrix the "niet" is part of the condition and is
read per member, so the quantifier is untouched — unlike ch. 3.9's "niet
gedurende …", which qualifies the outer comparison and which an executor may
therefore fold by flipping `all` into `none` (∀c.¬P = ¬∃c.P). Both give the
same verdict; stating where the negation stands keeps an executor from
applying that flip twice.

**Worked examples — the counting quantifiers** (absolute;
`corpus/golden/jutterswet-reddingsvloot` verbatim). Document: a
`Reddingsvloot` reaches its `sloepen` over the to-many role of
`vlootindeling`; `vaartijd` is an attribute of `Reddingssloep` per month and
`gekeurd` a characteristic of it. One matrix — the ch. 3.9 whole-period test
*vaartijd > 20 gedurende het gehele jaar* — under three quantifiers, plus a
fourth fold that adds a die/dat subselection to the same matrix: *inzetbaar* =
`atLeast` 2, *krap* = `atMost` 1, *paraat* = `exactly` 1, *geschikt* =
`atLeast` 2 over the sloepen *die gekeurd zijn*. `sloeptal` is the plain
instance fold beside them, so every row also states how large the unfiltered
range is.

| # | vloot | the vloot's sloepen (vaartijd, keuring) | sloeptal | count | inzetbaar (≥2) | krap (≤1) | paraat (=1) | geschikt (≥2, gekeurd) |
|---|---|---|---|---|---|---|---|---|
| 1 | V1 | 25 gekeurd, 30 gekeurd | 2 | 2 | **waar** | onwaar | onwaar | **waar** |
| 2 | V1 | 25 gekeurd, 10 gekeurd | 2 | 1 | onwaar | **waar** | **waar** | onwaar |
| 3 | V1 | none | 0 | 0 | onwaar | **waar** | onwaar | onwaar |
| 4 | V1 | 25 gekeurd, 30 gekeurd on Jan–Jun only | 2 | 1 | onwaar | **waar** | **waar** | onwaar |
| 5 | V1 | 25 gekeurd, 30 NIET gekeurd, 10 gekeurd | 3 | 2 | **waar** | onwaar | onwaar | onwaar |
| 5 | V2 | 30 gekeurd | 1 | 1 | onwaar | **waar** | **waar** | onwaar |
| 6 | V1 | 25 NIET gekeurd, 30 NIET gekeurd | 2 | 2 | **waar** | onwaar | onwaar | onwaar |

The `count` column is the count of the formula — over the unfiltered
contributors, which the first three folds range over; *geschikt* counts over
the `gekeurd` survivors, and its own counts are 2, 1, 0, 1, 1, 1 and 0 in the
seven rows.

Rows 1 and 2 pin each threshold **from both sides at once**, which one shared
matrix gives: a count of 2 satisfies `atLeast` 2 exactly at its threshold and
breaks `atMost` 1 by exactly one, and a count of 1 turns both of those over
while it satisfies `exactly` 1. Row 3 is the empty set under all three, and it
makes the paragraph above observable: a count of 0 decides each quantifier
without any vacuum rule, so *krap* is **waar** where *inzetbaar* and *paraat*
are **onwaar**, which no single "empty means true" principle would give. Row 4
is ch. 3.9's gap, reused unchanged: the member with the half-year timeline
fails the matrix, so it does not count.

Row 5 carries two vloten. V1 is the cell **the filter alone decides**:
*inzetbaar* and *geschikt* ask the same question of the same matrix and part
here, because the sloep at 30 is not `gekeurd` and the surviving pair holds
only one satisfier. Removing the `filter` from the wire makes *geschikt* read
**waar** and moves nothing else in the document. V2 pins the **range** as
`jutterswet-schouwploeg` row 2 does — it reads *krap* and *paraat* in the same
run in which V1 reads neither, so an executor that quantified over every
`Reddingssloep` rather than over the vloot's own sloepen answers it the other
way — and it is also where ch. 5.10's **empty-prefix** triggers fire: over one
contributor, `atLeast` 2 is out of reach before the matrix is read once, and
`atMost` 1 can no longer be exceeded, so both answer having read nothing. Row
6 is the empty **selection** beside a filled range: two satisfying sloepen,
neither `gekeurd`, so *inzetbaar* is **waar** on a count of 2 while *geschikt*
counts over ∅ and is **onwaar** — the counting mirror of the vacuous cell, and
the one place where an executor that dropped the filter and one that dropped
the reading of the empty set would answer differently.

### 3.10 Period conditions and the §8.4.3 clip

A **period window** (`TimeWindow`, `IR-SPEC.md` ch. 4.5) holds on the days
from its `from` up to its `to`: `from` inclusive, `to` exclusive, and
inclusive where the source wrote "tot en met" (`toInclusive`), so that the
window then ends the day after `to`. An absent bound is open. `from` is
evaluated before `to` (ch. 5.11), and a bound that evaluates to leeg puts no
day in the window. *Well-formedness* (§8.4.3): a bound is a day-precise date
expression over the subject, the parameters and literals — a literal, an own
scalar date attribute, a scalar date parameter, and the operators and builtins
over those, such as "zijn vervaldatum plus 1 dg" (`IR-SPEC.md` WF-111); a bound
that reads a timeline, navigates a role, folds a set or is a datum-tijd is
ill-formed (negatives `wf111-computed-bound-navigated-read`,
`wf111-period-boundary-time-dependent-parameter`, `wf111-date-time-bound-*`).
The window never stands under `none` or a counting quantifier (WF-112;
negative `wf112-time-window-counting-quantifier`): the window gates the rule,
and no reading is defined for its complemented or counted verdict; the rule
covers a quantifier fold's `filter` and `test` too. The window has two
readings, and the target decides between them.

**Scalar target — the evaluation-date gate.** The rule applies iff the
evaluation date lies in the window; an unset evaluation date never matches,
and a leeg bound never matches. Worked example: window
[2024-01-01, 2025-01-01), evaluation date 2024-12-31 → applies; 2025-01-01 →
does not; the "tot en met 2024-12-31" form still applies on 2024-12-31. The
window stands as a rule's whole condition, as a member of an `all` or `the`
composite, in a consistency assertion as its whole criterion or as an `all`
member, and in the `test` or the `filter` of an `all` quantifier fold or the
die/dat `filter` of an aggregation; in the last two positions the member is
the window's subject, so a bound that is an attribute is read on each member.
`corpus/golden/jutterswet-premievenster` pins the six clauses and this
example case by case, and `corpus/golden/jutterswet-wachtvenster` the fold
and aggregation positions, each ploeg carrying a date of the same name that a
reading on the fold's subject would take. *v0 mirror note:* v0's IR admits any
date expression as a bound (WF-042), and v0's backends refuse the condition
under counting quantifiers; WF-111 and WF-112 are the v1 rules.

**Timeline target — the window clips.** When the rule's target is
time-dependent, the window speaks about the timeline, not about the moment of
evaluation, and its days are the periods of the clip of ch. 3.3:

```
written(tl, window) = tl restricted to [from, to)
```

Worked example: the rule computes {2024: 10, 2025: 12} per year; window
[2025-01-01, open) → written value {2025: 12}; the days in 2024 keep what
another version or producer gives them — a gap, absent other writers.

- **The window is the rule's whole condition, and the evaluation date plays
  no part.** The window says which days of the timeline are written, so the
  write happens with an unset evaluation date and with one outside the
  window alike. The gate's clause "an unset evaluation date never matches" is
  about the moment of evaluation and has no counterpart here.
- **The bounds read as the gate's do**: `from` inclusive, `to` exclusive,
  "tot en met" inclusive, an absent bound open, the shapes of WF-111. A bound
  that is the subject's own attribute is read per subject.
- **The restriction cuts segments, never values.** A segment that straddles
  a bound keeps its value on the part inside the window: the producer's value
  is not recomputed for the shorter span. A monthly day count of 31 cut at the
  fifth of the month still reads 31 on the days that remain
  (`WV-3.10-clip-knipt-segment-houdt-waarde`; the case format pins every
  expected timeline to the grid, so this clause is pinned in the executors'
  suites, ch. 3.12).
- **An empty window writes an empty timeline.** A leeg bound puts no day in
  the window, and a window whose `from` lies after its `to` has no days
  either. A window that misses the produced timeline leaves nothing of it.
  The clipped result is then the empty timeline, which decides no day of the
  target (ch. 3.11, ch. 7.8), as for any producer.

`corpus/golden/jutterswet-pachtvenster` pins every clause whose case lies on
the declared grid. *v0 mirror note:* v0 refuses the window on a temporal
target (the manifest family "period-comparison clipping, §8.4.3"); the clip is
a tracked delta.

**The periods of a condition — the clip generalised.** On a rule with a
timeline target, a condition
that reads a timeline or holds a window restricts the produced timeline to its
periods, by the clip of ch. 3.3 (§10.3, §8.4.1):

```
written(tl, c) = tl restricted to periods(c)
```

The window is the case of a condition whose periods are [from, to). The
periods of a condition, read per day:

- a check of a time-dependent characteristic of the subject holds in its
  true periods, a negated check outside them, over all time (ch. 3.1);
- a comparison of a time-dependent attribute of the subject with a scalar
  bound holds on every day on which the comparison of ch. 1.2 holds; a gap day
  reads leeg, so "ongelijk aan" holds on it against a filled bound;
- a whole-period qualifier holds in its buckets (ch. 3.9, per bucket);
- a period window holds on its days, from `from` up to `to`, or to the day
  after `to` where the source wrote "tot en met", its bounds read per subject;
  a leeg bound puts no day in it;
- a composite holds on every day on which its quantifier holds of the members
  that hold that day. Every member is read: the short circuit of ch. 5.10
  orders the members of one verdict, and here each day has its own;
- a node that reads no timeline holds on every day or on none.

The periods are maximal (ch. 3.3), so the restriction cuts a segment only
where the condition changes. A condition that reads no timeline holds on every
day or on none, and it gates the rule as on a scalar target: where it fails,
the rule does not decide. Every timeline the condition reads lies on a grid
that fits the target's (`IR-SPEC.md` WF-126). A form not in this list that
reads a timeline — a navigated or hosted check, a comparison of two timelines,
a leeg or gevuld test, a quantifier fold — has no periods defined here
(ch. 3.12). RegelSpraak writes the condition as "gedurende de tijd dat …" or
as "indien …"; the reading follows the target, not the word, and the wire
carries no field for the word.

Worked example `WV-3.10-voorwaarde-per-periode` (absolute; `ref/` tests
verbatim, the fixture `ref/test/fixtures/conditions-per-period/afdracht`). An
Afdracht of 10 000 € is `rentedragend` (a characteristic for every day) on
[2025-11-16, 2026-03-13) and `verzuimd` (for every month) on
[2025-12-01, 2026-02-01); the verzuimrentevoet is 0,04 /jr on
(−∞, 2026-02-01) and 0,05 /jr on [2026-02-01, +∞).

- "De minimumdagen (voor elke maand) moet berekend worden als (het aantal
  dagen in de maand dat hij rentedragend is) gedurende de tijd dat hij
  verzuimd is": **31** on [2025-12-01, 2026-01-01) and **31** on
  [2026-01-01, 2026-02-01); in every other month the rule writes nothing.
- "De dagrente (€/jr, voor elke dag) moet berekend worden als de
  verzuimrentevoet maal zijn afdrachtbedrag gedurende de tijd dat hij
  rentedragend is": **400** on [2025-11-16, 2026-02-01) and **500** on
  [2026-02-01, 2026-03-13), nothing outside them.

The corpus witness, written by rules through the RegelSpraak frontend, is
`corpus/golden/jutterswet-loon-per-periode`: a characteristic, its negation, a
comparison per day, a composite, a scalar as the whole value (ch. 3.11), a day
count, and the gate of a condition without a timeline. The window as a member
of such a condition and as the condition of the period arithmetic are the
`ref/` tests `WV-3.10-venster-als-lid` and `WV-3.10-venster-rekenen` (the
fixtures `venster-als-lid` and `venster-rekenen`).

**A time-dependent characteristic within a window** (§10.3, §8.4.3, §9.2). The
target of a rule may be a time-dependent characteristic, whose timeline the
condition is (ch. 3.11): it holds in the periods of the window, cut by the
periods of the rest of the condition read per period. "Time-dependent" is
asked of both declaration tables, the attribute's granularity and the
characteristic's: a target whose characteristic is time-dependent is a
timeline target, and the scalar gate does not apply to it
(`corpus/golden/time-window-timeline-characteristic`). Where the periods of
the condition have an open bound, the characteristic has no true periods to
hold (ch. 3.1): EF-023, a form without a meaning (ch. 3.12; red case
`time-window-timeline-characteristic-open`).

Worked example `WV-3.10-kenmerk-venster` (absolute; `ref/` tests verbatim, the
fixture `ref/test/fixtures/conditions-per-period/kenmerk-venster`). "Een
Afdracht is rentedragend van zijn vervaldatum plus 1 dg tot en met zijn
betaaldatum." Geval 1 (verval 2025-11-15, betaald 2026-03-12): rentedragend on
**[2025-11-16, 2026-03-13)**. Geval 2 (verval 2028-02-10, betaald 2028-04-04):
on **[2028-02-11, 2028-04-05)**. A leeg betaaldatum: rentedragend
**nowhere**. Formulation A of `spec/proposals/IMPLICIT-TIME.md` §4 stands on
this characteristic: the golden `jutterswet-verzuimrente-30-360-afgerond`, and
its anchor `corpus/golden/anchor/jutterswet-verzuimrente-30-360` with the
exact values.

The interaction of the window with a rule **version's** validity window on a
temporal write is not defined here (ch. 3.12).

### 3.11 Temporal writes

A rule with a time-dependent target writes a **whole timeline at once**, per
subject. Its value is the timeline of the producer that its value expression
is, restricted to the periods of its condition where the condition reads a
timeline and gated by the condition where it reads none (ch. 3.3, 3.10); the
value is never computed by evaluating a scalar rule once per period. The
producers:

- the **day count** (ch. 3.8) and the **time-proportional part** (ch. 3.7);
- **period arithmetic** (ch. 3.4, 3.5): an operator of ch. 1.2 other than the
  power (plus, min, verminderd met, maal, gedeeld door in both variants,
  percentage van) whose operands are such an operator again or a rounding of
  one, the sum of a time-dependent attribute over the members of a set, an
  own time-dependent attribute, a time-dependent parameter, or a scalar
  operand (a literal, an own scalar attribute, a scalar parameter), or a
  rounding of such a value, which rounds it per day (ch. 2.5). It writes the
  timeline of the value over all time, open segments included (ch. 3.1). A
  die/dat subselection on the sum is refused by name. Witnesses
  `corpus/golden/jutterswet-maandloon` (plus, min, the sum),
  `corpus/golden/jutterswet-rekenen-per-dag` (the other five operators) and
  `corpus/golden/jutterswet-verzuimrente-dagen-afgerond` (a time-dependent
  parameter; the anchor `anchor/jutterswet-verzuimrente-dagen` pins its exact
  totals);
- an **operand alone** as the whole value: a scalar holds on every day, the
  one segment (−∞, +∞) of ch. 3.1, an own time-dependent attribute or a
  time-dependent parameter is read whole, and the rule's condition cuts it.
  "De aanstellingspremie moet gesteld worden op 25 gedurende de tijd dat hij
  aangesteld is" writes 25 on the periods in which he is aangesteld
  (`corpus/golden/jutterswet-loon-per-periode`);
- for a time-dependent **characteristic**, the condition itself, read per
  period with a window among its members (ch. 3.10): the target holds exactly
  where the condition holds and is false elsewhere — the monotone reading; no
  complement is seeded;
- a **defaults group** on a timeline attribute target, which decides per day
  (ch. 7.11); beside it no rule of the target's level writes (`IR-SPEC.md`
  WF-050);
- an **initialisation**, the lowest layer of its target's stack, which
  decides the days on which no writer decides and the input reads no value,
  over all time (ch. 7.8). Its value is period arithmetic, a scalar on every
  day, cut to the periods of its condition.

A **time-dependent value in a scalar slot** is refused except through the
collapses (ch. 3.3); that boundary is never crossed by silent coercion.

**A writer's own days and change.** A write replaces what its rule wrote
before. The target is the union of the segments its writers write, and on a
day none of them decides it holds what it held before them, its input or leeg
("Leeg in a write", below). Whether a write is a change is decided by
segment-wise equality of the target's timeline (ch. 3.1), which makes
rewriting to a fixpoint idempotent (ch. 5).

**The write boundary per segment.** The write boundary of ch. 2.4 point 1
holds per segment of a timeline target: a segment whose value does not fit
the target's declared decimals is EF-006, with the place of the attribute,
the exact value and the decimals as for a scalar write, and the rule writes no
timeline; a segment that fits is written as the exact value it is, and the
unbounded form has no ceiling. The static precision gate (WF-023) bounds every
segment of a rule's timeline target before the rule runs, and leaves the
time-proportional part alone, behind which this boundary stands (ch. 2.4
point 4, ch. 2.8).

Worked example — the write boundary per segment (absolute; `ref/` tests
`WV-3.7-schrijfgrens-maand`, `-jaar` and `-onbegrensd`): a Jutter with a
`borgtarief` of 18 per year and `borgplichtig` from 2025-01-01 up to
2025-02-08, and "het borgdeel moet berekend worden als het tijdsevenredig deel
van zijn borgtarief gedurende de tijd dat hij borgplichtig is". Into a monthly
`borgdeel` of 2 decimals the segments are **18** in January and 18 × 7/28 =
**4,50** in February, which fit. Into a yearly `borgdeel` of 2 decimals the
one segment is 18 × 38/365 = 684/365, which does not fit: **EF-006**, place
`J1.borgdeel`, value 684/365, decimals 2. Into a yearly `borgdeel` of the
unbounded form it is **684/365**. The red case
`corpus/red/jutterswet-tijdsevenredig-past-niet` pins the fault.

**Leeg in a write.** A writer that fires with a leeg value decides nothing,
on every target (decided by the maintainer 2026-11-04: the specification gives
an equation with a leeg value a leeg result, §9.1, and says nothing of the
input of a target, §9.3; the reading follows the default evaluation of ALEF).
On a **timeline** target a writer, also its only one, decides only the days on
which its timeline reads a value; on a day no writer decides, the target holds
what it held before any of them wrote, its input, where that is leeg the value
of its initialisation, or leeg (ch. 5.7, "Two writers per day"; ch. 7.8;
`spec/proposals/IMPLICIT-TIME.md` point 5). An empty result
decides no day, so the input stands whole; a result with a value on some days
leaves the input standing on the others, so input and computed segments
interleave per day. A scalar is the timeline (−∞, +∞) (ch. 3.1), so on a
**scalar** target the same rule reads: a writer that fires with a leeg value
decides nothing, and the target holds another writer's value, its input, its
initialisation's value or leeg — for the target's only writer, beside a second
writer and with a validity window alike (ch. 1.2, Assignment; ch. 5.7; the
stack of ch. 7.8).

Worked example (`WV-3.11-leeg-beslist-niets`, `-versie` and `-naast-waarde`;
`ref/` tests verbatim, on the documents of `ref/test/fixtures/empty-write`,
which the suites of the ASP and the Drools backend play as well): a Vondst
with whole numbers `taxatie`, `opgave` and `schatting` and the rules "De
taxatie van een Vondst moet gesteld worden op zijn opgave indien hij gemeld
is" and "… op zijn schatting indien hij niet gemeld is". V1, gemeld, with
taxatie 40 in the input and no opgave: the first rule fires with leeg and the
second does not fire, so the taxatie is **40**; V2 with opgave 50 as well:
**50**; V3 without input: **leeg**. The one rule "De taxatie van een Vondst
moet gesteld worden op zijn opgave" with "geldig vanaf 2020", on 2025-07-01,
gives the same: **40**, **50**, **leeg**. Beside "… op zijn schatting indien
hij erkend is", a Vondst gemeld and erkend with taxatie 40, schatting 50 and
no opgave: **50**, without a fault; without the taxatie: **50**; with taxatie
40 and neither opgave nor schatting: **40**. Two writers with different values
stay EF-016 (ch. 5.7).

Worked example (`WV-3.11-per-dag-invoer`; `ref/` tests verbatim, on the
documents of `ref/test/fixtures/writers-per-day`, which the suites of the ASP
and the Drools backend play as well): a Jutter whose `strandloon` is a whole
number for each month, the input 10 on [2025-01-01, 2026-01-01), and the one
rule "Het strandloon van een Jutter moet gesteld worden op 20 van dd.
01-03-2025 tot dd. 01-06-2025". The strandloon is **10** on [2025-01-01,
2025-03-01), **20** on [2025-03-01, 2025-06-01) and **10** on [2025-06-01,
2026-01-01); without the input it is 20 on [2025-03-01, 2025-06-01) and leeg
on every other day. With 10 in place of 20 it is **10** on [2025-01-01,
2026-01-01). With the one rule "Het strandloon van een Jutter moet berekend
worden als zijn basisloon" and the basisloon 20 on [2025-03-01, 2025-06-01)
and a gap elsewhere, it is again 10, 20 and 10: the rule reads leeg outside
the spring and decides none of those days. Beside a second rule "… gesteld
worden op 30 van dd. 01-09-2025 tot dd. 01-11-2025" (and 20 in spring) the
input stands on the days neither decides: 10, 20, 10, 30 on [2025-09-01,
2025-11-01) and 10 (`WV-5.7-per-dag-twee-schrijvers-invoer`).

**Two writers per day.** Two or more writers of one timeline target each
decide the days of their own segments, and the target is their union per day;
on a day two decide with different values the session faults with EF-016
(ch. 5.7, "Two writers per day", with the worked example `WV-5.7-per-dag`).

**What 0 means.** 0 is a value, and a producer that writes 0 on a day decides
that day. The day count writes 0 in every bucket without a true day (ch. 3.8);
plus and min write 0 on a day no operand fills, as maal, percentage van and a
division with a leeg dividend do (ch. 3.4); and the sum with its fallback
clause writes 0 on a day every contributor is leeg (ch. 3.5). The
time-proportional part writes nothing in a bucket without a true day or with a
leeg base (ch. 3.7), the sum without the fallback writes leeg on an all-leeg
day, and verminderd met writes leeg where its left operand is leeg. A producer
that writes a value on every day of all time has three consequences:

1. **No vacuous truth.** A whole-period test of such a timeline always finds
   a touched bucket, so it never passes vacuously (ch. 3.9): the gevuld test
   holds in every bucket, and every other test is judged on the values,
   0 included. Worked example (`corpus/golden/jutterswet-vast-startpunt`,
   case 4): a Jutter without a wachtplichtig period has both day counts 0 on
   (−∞, +∞); `stormpremie`, 50 "indien zijn stormwachtdagen gedurende het
   gehele jaar gevuld is", is **50**, and `maandpremie`, 20 "indien zijn
   maandwachtdagen gedurende de gehele maand groter is dan 0", is **leeg**: 0
   is not greater than 0 in any month. A count without segments would touch no
   bucket, pass both tests vacuously and give `maandpremie` 20.
2. **Beside the input.** A target that such a producer writes is decided on
   every day of the periods of its rule's condition, and its input timeline
   stands only on the days outside them (ch. 3.10); without a condition, on no
   day.
3. **Beside a second writer.** Such a producer decides every day of the
   periods of its rule's condition, so it stands beside another writer of the
   same target without EF-016 only where the two have disjoint periods or
   write the same value on the days they share (ch. 5.7). In `WV-5.7-per-dag`
   the day count stands beside the writer of 30 because their conditions are
   each other's complement per bucket; without the day count's condition both
   decide December 2025, with 30 and 31, and the session faults.

**Anchored targets.** A producer on a target with a variable start point
buckets on the anchored grid (ch. 3.2). The temporal sum over a set onto such
a target is not defined: it would fold timelines whose grids are laid per
object (ch. 3.12).

**What a characteristic write does not cover.** The write of a time-dependent
characteristic is defined within a window (ch. 3.10). A negated check in its
condition holds on the complement of its true periods **within the periods of
the window**, which bound it. An unconditional characteristic would hold on
(−∞, +∞), and a negated check outside a window on the unbounded complement of
finite periods: both are open periods, which a characteristic's true periods
do not carry (ch. 3.1). Those forms, a write without a window, two rules that
write one characteristic, a case-supplied timeline of a characteristic that a
rule writes, and a navigated characteristic in the condition of such a write
are not defined here (ch. 3.12). This is about a condition whose periods
become a **written** timeline; reading a navigated time-dependent
characteristic under the whole-period qualifier is defined (ch. 3.9), since
the qualifier folds the reached timeline to a boolean and writes no timeline.
*v0 mirror note:* v0 refuses a time-dependent characteristic target whose
write producer it cannot determine (the manifest family "temporal
kenmerktoekenning with indeterminate write-producer"), and lowers a
conjunctive or at-least-one compound of own characteristics to the
intersection or the union of their true periods; the write within a window and
its negated check are tracked deltas.

Worked examples (`ref/` tests verbatim):

- *Monotone characteristic write.* Condition: the subject's characteristic
  `recht` holds on [2024-03-01, 2024-09-01). Written target characteristic:
  holds on exactly [2024-03-01, 2024-09-01), false everywhere else — no
  complement is seeded.
- *A negated characteristic under a clip.* Window [2024-01-01, 2025-01-01),
  `recht` as above, condition "niet recht" → written true periods
  [2024-01-01, 2024-03-01) ∪ [2024-09-01, 2025-01-01).
- *Idempotence.* The target already holds {[2024-03-01, 2024-09-01)} and the
  rule computes the same segment list → segment-wise equal → the write is a
  no-op; computing {[2024-03-01, 2024-10-01)} instead → not equal → a real
  write (the fixpoint of ch. 5 sees a change).

### 3.12 Open decisions

The decisions this chapter leaves open. A form listed here as without a
meaning is refused by name by every executor; where it has a fault kind, the
kind is `declined` (ch. 5.7b): EF-021, EF-022, EF-023. What each executor
carries of this chapter, and what it refuses by name beyond this list, is
recorded in `spec/CONSTRUCTS.md` rows 11 and 29, in `spec/ASP-TRANSLATION.md`
and in the READMEs of the backends.

| # | Open decision | Where | What holds now |
|---|---|---|---|
| 1 | **Multi-period grids** (`count` ≥ 2) | ch. 3.2 | Declared and carried in the model layer (WF-005); no operation is defined, a case input included, and every read and write is refused. A parameter's facet has `count` 1 (`IR-SPEC.md` ch. 2.10). Open: specify how a value spreads over a period of N units, or restrict `count` to 1 at the type level. |
| 2 | **The calendar beyond month and year** | ch. 3.2 | The calendar lays month and year buckets only; a member on the calendar's day, week or quarter grid has no bucket (a fixed start point lays week and quarter periods of its own, and a case timeline on the day grid needs none). A unit outside the spelling table, such as `bm`, belongs to neither family and keeps its declared factor. Open: calendar buckets of a day, a week and a quarter; whether a unit outside the table that a document declares as a fixed number of days and uses as a month reopens the fixed factor between the families that WF-009 refuses on `mnd = 30 dg`, and if so whether it is intended (a bank month) or refused. |
| 3 | **Producers across start points and grids** | ch. 3.2, 3.7, 3.11 | The time-proportional part onto a target or from a base on a fixed or variable start point is not defined; its base must be as coarse as the bucket or coarser; the temporal sum over a set onto a target with a variable start is not defined. Open: the part across start points; a base finer than the bucket, which ALEF reads as the average of the base weighted by days (`spec/proposals/IMPLICIT-TIME.md` point 8b); the sum across grids laid per object. |
| 4 | **Operators and folds over timelines without a reading** | ch. 3.4, 3.4c, 3.5, 3.6 | The power over a timeline is refused; minimum, maximum, eerste and laatste over timelines are refused (v0: "the minimum of a timeline" is ambiguous between a fold per period over contributors and a fold over the periods of one timeline); a ranking or counting fold over a time-dependent member of a set — minimum, maximum, eerste, laatste, aantal, aantal gevulde (EF-021) — is refused, for want of a moment. Open: these readings. `spec/proposals/IMPLICIT-TIME.md` point 9 reads minimum, maximum, eerste and laatste per period over the operands, with no form over the time of one timeline, which also gives a fold over the members of a set a moment per period; this chapter does not state that rule. |
| 5 | **Conditions over timelines without a reading** | ch. 3.9, 3.10 | Under the whole-period qualifier: a comparison over a timeline reached over a path, between two timelines or without a timeline, and a date-valued comparison. On a rule with a timeline target: a condition form outside the list of ch. 3.10 — a navigated or hosted check, a comparison of two timelines, a leeg or gevuld test, a quantifier fold. Open: their readings; the reading per day of ch. 3.3 is the candidate for the second group. |
| 6 | **Day kinds outside the bound day** | ch. 3.8 | No meaning: a §9.8 test on a date other than the bound day (EF-022); the day-kind predicate as the condition of a rule (§8.1.5); the duration in whole days of a kind between two datum-tijden. Open: a source for what a test on another date means inside a definition about an arbitrary day; a classifier read inside the fixpoint of the rules; a rule for the part of a day at each end of an interval between instants. |
| 7 | **A time-dependent characteristic outside a window** | ch. 3.3, 3.10, 3.11 | The write of a characteristic is defined within a window. No meaning: periods with an open bound (EF-023), so an unconditional characteristic and a negated check outside a window. Not defined: a write without a window, two rules on one characteristic, a case-supplied timeline of a characteristic a rule writes, a navigated characteristic in the condition of the write, and a time-dependent characteristic as the operand of the point read (the case format). A defaults group on a time-dependent characteristic is ill-formed (`IR-SPEC.md` WF-086). Open: each of these. |
| 8 | **The period window and the axis of versions** | ch. 3.10 | (a) A rule version's validity window on a temporal write mixes the selection of versions with the timeline axis; it belongs to the single-axis decision of ch. 8 (PLAN §9, decision 2), and no golden carries a validity window on a temporal rule; a defaults group's validity on a timeline target gates by the evaluation date in the same way (ch. 7.11, `WV-7.11-versie`). (b) A window in the `filter` of an `all` quantifier fold in a session without an evaluation date never matches, so the member is filtered out and the fold is vacuously waar; WF-112 is not widened to it (`corpus/golden/jutterswet-wachtvenster`, `bevoegd` in `case3-tijdvrij`). (c) A clip that cuts a bucket has no corpus carrier, because the case format pins every expected timeline to the declared grid; the alternative, a WF rule that demands bounds on the grid for a clip, is not taken. Open: (a), the reading of (b), and a case format that releases expected timelines from the grid. |
| 9 | **The subject's own total of a case-supplied timeline** | ch. 3.6, 8.4 | It reads the asOfDate bucket, one period of the grid that holds the evaluation date, while a timeline a rule writes is totalled whole, so the meaning of "het totaal van zijn X" depends on who wrote X. Open: whether the own total of a case timeline reads the whole timeline, as every other timeline is read over all time, which changes the case convention of ch. 8.4. |
| 10 | **The producers of an initialisation and of a member value on a timeline target** | ch. 3.11, 7.8 | An initialisation and the member value of a creation on a timeline target are read per day in the stack of ch. 7.8, their value period arithmetic over all time; an allocation that names a time-dependent attribute is not well-formed (`IR-SPEC.md` WF-128). The time-proportional part and the day count as the value of an initialisation or of a member value are refused by name. Open: their reading there. |

---

## 4. Allocation (verdeling)

An allocation distributes one source amount over related recipients. Its
semantics are stated here as **recurrence equations over recipient
groups** — one closed definition all three methods instantiate — never as
"one helper call". Everything is v0 mirror with tracked deltas; the
chapter's edge-rule table and refusals close the row-17 obligations of
`CONSTRUCTS.md`.

### 4.1 The form and its recipients

An allocation names a source `(sourceType, sourceAttribute)` — the pot —
and a target `(targetType, targetAttribute)` — each recipient's share
(`IR-SPEC.md` ch. 3.2). The **recipient set** of one source instance is:
every `targetType` instance related to it through the connecting
relation. **The role supplies the relation, `targetType` supplies the
population, and the intersection is what receives** — read extensionally,
so an instance of any S ⊑ `targetType` is in and everything else the
relation reaches is out (`IR-SPEC.md` ch. 2.2). The two coincided until
the supertype relation landed, because WF-047 then demanded one and the
same name on the bound role's filler and on `targetType`; with "agree"
widened to ⊑-compatibility the filler may be a **strict supertype**, and a
related instance that is not a `targetType` receives nothing and stays
**leeg** — never 0. It contributes nothing to Σ either, so the excluded
instances raise the remainder. Worked witness:
`golden/jutterswet-hoofdberging`, 1001 over two `Hoofdberger`s beside a
plain `Berger` on one `Berger`-typed role → **500, 500, leeg**, rest
**1** (unfiltered it would read 333/333/333, rest 2).
**Decided (2026-08-16, WF-047/048):** the connecting relation
is named explicitly — the `connectingRole` binding of `IR-SPEC.md`
ch. 3.2 (wire `connectingRole`; `verbindendeRol` until L4), the same symbolic-role move as
`CONSTRUCTS.md` rows 4/22. **Landed L2 (2026-09-24, B12):** the binding is
required, and executors consume it and nothing else: there is no
derivation to fall back on, so an allocation without it does not load.
*History until L2:* a bindingless form derived the relation as the unique
fact type relating `targetType` to `sourceType` — admissible only while
that derivation was unique (WF-048; v0 refused the non-unique case as
marker/skip, and the executors faulted on it). The reciprocal shape (one
role relating a type to itself, WF-011/013) is bound like any other: its
single role is bound and opposite at once. That a
reciprocal relation entry holds in **both directions** — the pinned
runtime writes both to-one legs, so navigation never reads a spuriously
empty side — is no peculiarity of allocation: it is the reciprocal case
of the general case-input law, which **ch. 5.7a** states for every fact
type and which the shared case loader implements at load.

### 4.2 The distribution equations

All three methods are instances of one recurrence over an ordered
partition `G_1 … G_m` of the recipient set:

```
pot_1 = A                                     (the source amount)
for each group G_k in order:
    raw_i     = pot_k × weight_i / Σ_{j∈G_k} weight_j        (weighted)
              | pot_k / |G_k|                                (unweighted)
    share_i   = max(0, floor_d(min(raw_i, max_i)))           (cap, then floor,
                                                              then clamp)
    pot_{k+1} = max(0, pot_k − Σ_{i∈G_k} share_i)
rest = A − Σ_all share_i
```

with `min(x, leeg) = x` (no cap) and `floor_d` = round toward −∞ at the
declared decimals (identity when no rounding is declared), applied once to
the exact `min(raw_i, max_i)`. Each share and the rest are written through
the assignment gate (ch. 2.4 point 1): a share or a rest that does not fit
its target is EF-006, one fault for the allocation on its source, and the
allocation writes none of its shares and no rest there (ch. 4.4). *v0
residual (same class as ch. 2.1/3.7):* v0 computes the share divisions at
16 significant digits; the exact fractions above are normative, and the
executors compute them.

- **Equal shares**: one group, all recipients, unweighted; no cap
  (ill-formed with one, ch. 4.5). `share_i = A / n`.
- **Proportional (naar rato)**: one group, the recipients whose weight is
  **filled** — an empty-weight recipient is excluded and receives no
  share (it stays leeg, never 0), and the exclusion surfaces as a §9.5
  error (EF-028, ch. 5.6); a **negative weight counts as 0**: its
  recipient stays in the group and is assigned a real 0, and the weight
  surfaces as a §9.5 error (EF-038); when the eligible weights sum to 0
  there is no distribution at all (nothing written, no division by
  zero).
- **Ordered (op volgorde)**: groups are the equal-key classes of the
  ordering attribute, ascending or descending as declared, an **empty key
  sorting last** (no error — mirrored); each group divides the pot *as it
  stands at that group's entry* (ties are simultaneous, not sequentially
  depleted), unweighted or — with the declared tie-break weight —
  weighted. In the weighted tie-break, an **empty weight counts as 0**
  and surfaces as a §9.5 error (EF-029), and a **negative weight counts
  as 0** without an error; either way its member's raw share is that of
  weight 0; a tie group whose weights sum to 0
  assigns **0 to every member** (the recurrence's 0/0 case, defined, not
  crashed). The `max(0, …)` clauses make an exhausted pot give later
  groups **0** (a real 0, never leeg). These clamps are
  **ordered-method-only** — the proportional method has its own, harder
  rules above and in ch. 4.4.

### 4.3 The modalities

- **Maximum** (per-recipient cap): `min(raw_i, max_i)`, before rounding.
  The capped surplus is **not redistributed and not iterated** — it falls
  into the rest. An **empty or negative** cap degrades to *uncapped* and
  surfaces a §9.5 error; a **zero** cap is valid (the recipient gets 0).
  A cap requires a weighted split (ch. 4.5).
- **Rounding**: floor at the declared decimals, applied per share
  **after** the cap; in the ordered method it applies inside the
  recurrence, so depletion carries capped-and-floored shares forward.
- **Remainder**: `rest = A − Σ share_i`, written to the declared source
  attribute. It is a derived value, recomputed whenever the share set
  changes (a late-created recipient corrects it — ch. 5's freshness);
  excluded recipients contribute nothing to Σ. Absent a declared
  remainder target, an under-distributed rest is silently gone —
  producers SHOULD declare it (the remainder advisory of `IR-SPEC.md`
  ch. 3.2 — deliberately a SHOULD, no WF number: the legal meaning may
  genuinely be "the rest lapses").

### 4.4 Empty and edge rules

| Case | Behaviour |
|---|---|
| zero recipients | nothing written, no error |
| empty source amount | no distribution, in every method: no share and no rest is written, and no error is raised, also not for an empty weight or cap beside it. *v0 mirror note:* v0 guards the ordered method only (the §9.7.6 discipline) and faults in the other two; v1 makes the rule uniform (decided) |
| weight empty — proportional | excluded from the split (share stays leeg) + §9.5 error; the others still receive |
| weight empty — ordered tie-break | assigned a real **0** + §9.5 error (ch. 4.2) |
| all eligible weights 0 — proportional | no distribution (nothing written) |
| all weights 0 in an ordered tie group | every member assigned **0** (ch. 4.2) |
| a negative weight — ordered tie-break | counts as 0, no error: claims nothing, cannot inflate others beyond the pot |
| a negative weight — proportional | counts as 0 + §9.5 error (EF-038): the recipient is assigned a real 0, and the weight neither shrinks the weight sum nor pushes the others past the pot. *v0 mirror note:* v0 leaves the weight unclamped, so a negative share and over-distribution past the pot are possible; v1 adopts the ordered method's clamp and adds the error (decided) |
| cap empty / negative | uncapped + §9.5 error (never a negative share) |
| empty ordering key | sorts last, no error |
| pot exhausted before a group | that group's shares are 0 (real 0, not leeg) |
| a share or the rest does not fit its target (no rounding, or a rounding at more decimals than the target holds) | EF-006 (ch. 2.4 point 1): one fault for the allocation on its source, its place the first share or rest that does not fit in the order of the case; no share and no rest is written there |

### 4.5 Well-formedness and refusals

WF-024 (no `COMPLEX` — the v0 placeholder for unrecognised forms is not
admissible IR), WF-025 (field validity per method: ratio iff weighted
split; cap only with a ratio — v0 demotes a capped equal-shares to
COMPLEX, strict reading of §9.7.3; ordering key orderable), WF-026 (at
most one allocation per written target), WF-128 (no time-dependent attribute
among the amount, the share, the rest, the weight, the cap and the ordering
key: the equations below hold at one moment, and no reading of them per day
is defined). *v0 mirror note on WF-026:* the
two backends genuinely **diverge** on a duplicate-target model — one
lowers both allocations (last agenda order wins), the other refuses the
pair — which is precisely why v1 promotes the case to ill-formed rather
than picking a winner.

### 4.6 Worked examples (absolute; `ref/` tests verbatim)

- Equal shares: 90 over 3 → **30, 30, 30**; 1000 over 2 → **500, 500**.
- Proportional: 120 over weights 1:2:3 → **20, 40, 60**; a fourth,
  weightless recipient stays **leeg**.
- Proportional + floor + rest: 1001 over 3:2, whole euros → raw 600,6 /
  400,4 → **600, 400**, rest **1**.
- Proportional + cap + rest: 1200 over 3:1:2, cap 300 each → raw
  600/200/400 → **300, 200, 300**, rest **400** (surplus not
  redistributed).
- Cap degrade: 1200 over 3:2:1, caps {leeg, −5, 300} → the leeg and
  negative caps degrade to uncapped → **600, 400, 200** + two §9.5
  errors (one per degraded cap — the rule × subject discipline of
  ch. 5.6).
- Ordered depletion: 1200; keys (age) 20, 20, 22 ascending; tie-break
  weights 3:1 in the age-20 group → group {20}: 900 and 300 (both from
  the full 1200); pot exhausted → age 22 gets **0**; rest **0**.
  Descending: age 22 alone takes **1200**, the others **0**.
- The full recurrence (cap + floor + depletion): 1800; groups by
  ascending age {22}, {39, 39}, {66, 66}; tie weights 3/2/1/3/1; caps
  500/500/500/250/250; floor at 0 decimals →
  22: raw 1800×3/3, cap **500** (pot → 1300);
  39s: raw 2/3×1300 = 866,67 → cap **500**, and 1/3×1300 = 433,33 →
  floor **433** (pot → 367);
  66s: raw 3/4×367 = 275,25 → cap **250**, and 1/4×367 = 91,75 → floor
  **91**; rest = 1800 − 1774 = **26**.
- Exact before the floor: 2999999999999999,97 in equal shares over 3,
  floor at 2 decimals → **999999999999999,99** three times, rest **0**
  (the quotient at 16 significant digits would be 1000000000000000,00 and
  the rest −0,03).
- A share that does not fit: 1001 in equal shares over 2 into whole-number
  shares without a rounding → EF-006, place the first recipient's share,
  value 1001/2, decimals 0, and nothing written
  (`corpus/red/jutterswet-verdeling-past-niet`).
- Edge rows of ch. 4.4, as observable outputs: a source with **no
  recipients** writes nothing (also no rest); an **empty pot** writes
  nothing in every method and raises no error, also beside an empty
  weight (`corpus/golden/jutterswet-lege-pot-en-negatief-gewicht`, case
  1); ordered with tie weights
  {3, −1} over 1200 in one group → the negative weight counts as 0 →
  **1200, 0**, no error; proportional with weights {3, −1} over 100 →
  **100, 0**, rest **0**, and one §9.5 error (EF-038) on the second
  recipient (the golden's case 2 adds a weightless third recipient:
  leeg, EF-028); an ordered tie group with weights {0, 0} over 1200 →
  **0, 0** and the pot carries to the next group; ordered over keys
  {2020, leeg, 2021} ascending → evaluation order 2020, 2021, leeg (the
  empty key last).

---

## 5. The evaluation model

A rule set does not run; it **holds**. The meaning of a model over a fact
base is the least fixpoint of its equations: every rule's target equals
its computed value, given every other rule's targets. This chapter states
the operational reading — forward chaining to that fixpoint — and the two
clauses that make it work, plus what is and is not order-independent.
Everything is v0 mirror with tracked deltas.

### 5.1 A berekening is an equation

Two clauses turn one-shot rule firing into an equation that holds at the
fixpoint:

1. **Freshness.** A rule is re-evaluated whenever any value it *reads*
   changes — its condition's operands and its value expression's inputs
   alike. A chained computation (`prijs = toeslag − korting`) therefore
   recomputes when a sibling rule later delivers `toeslag`; without this
   clause it would have computed once against leeg and gone stale.
2. **Idempotence.** A write happens only when it *changes* the target:
   scalar change per the ch. 1.2 assignment rule (empty-over-empty is a
   no-op, value comparison numerical), timeline change per segment-wise
   equality (ch. 3.1). The computed value is collapsed to the target's
   representation (ch. 2.4 write boundary) *before* the comparison —
   comparing across representations would never find equality and loop
   forever.

Together: re-evaluate on every input change, write only real changes, and
the system quiesces exactly when every equation holds. *v0 mirror note:*
the two backends realise freshness differently (declared read-sets on
patterns vs class-level reactivity); the semantics above are the
engine-neutral contract both satisfy, and the reference interpreter may
simply iterate to the fixpoint.

Worked example (absolute): `basistarief ← leeftijd`, `prijs ←
basistarief + 5`, input `leeftijd = 42` → **prijs = 47** at the fixpoint
regardless of which rule the engine tries first (a stale evaluation
order would have left 5). Chain with three rules and an initialisation:
inputs make rule B compute `100 − 0,20 × 200 = 60` and rule C
`max(0, 60 − 10) = 50` → **50**, while the initialisation's default 25
loses (ch. 5.4).

### 5.2 Confluence — what is order-independent, and what is not

The generated system is **confluent by construction**: every attribute
has a single writer (WF-026 generalises this discipline for allocations),
and the read-dependency graph between attributes is acyclic, so the
fixpoint is unique — rule order, activation order, and fact-insertion
order do not matter. *v0 mirror note:* v0 warns on a circular reference
at build time and bounds execution with a fire cap (ch. 5.5); **delta
(promoted):** WF-045 (ch. 6.2) makes acyclicity of the dependency graph
well-formedness — with two deliberate exceptions, each with an order of its
own: creation recursion (ch. 5.5), settled generation by generation, and
since 2026-10-22 the instance-ordered cycle (ch. 6.2), a value of an object
that reads the same value of its predecessor, settled object by object
along the chain the case input lays. The promise holds per case the loading
admits: a cycle in that chain is a fault of the case input (EF-033), never
an evaluation with two outcomes. The mirrored **exceptions** to
order-independence, each recorded:

- the ~1-ULP order dependence in mixed-unit chained arithmetic (a
  restatement-before-exact-add double rounding at v0's 16-digit store) —
  accepted and documented in v0, subsumed in v1 by the exact model of
  ch. 2 (the reference interpreter has no such dependence; backends must
  agree on the corpus after rounding);
- two allocations on one target (ill-formed, WF-026; the v0 backends
  diverge — ch. 4.5);
- the ordered allocation's internal depletion order — order-dependent by
  *definition*, and therefore fully specified by ch. 4.2's recurrence
  rather than left to the engine.

Worked example (order-independence made concrete): the ch. 5.1 chain
(`basistarief ← leeftijd`, `prijs ← basistarief + 5`, `leeftijd` = 42)
evaluated computation-first and reader-first: computation-first sets
`basistarief` = 42, then `prijs` = 47; reader-first computes `prijs` =
leeg-coalesced 5, then `basistarief` = 42 arrives, freshness re-fires the
reader, `prijs` = 47 — **the same fixpoint {42, 47}** either way; the
transient 5 is unobservable in the end state.

### 5.3 Negation at the fixpoint

Characteristics are default false. **A characteristic holds at the
fixpoint where an attribution that grants it decides there, or where the
case input sets it** (ch. 7.8; decided by the maintainer 2026-11-02, as
for a written value, ch. 6.1): an attribution that does not decide on a
later evaluation withdraws what it set, unless another attribution that
grants the characteristic decides. The temporal analogue is ch. 3.11's
no-complement rule. A condition that reads a **negated** characteristic
is evaluated *after* every producer of that characteristic — and the
producers' own input chains — have quiesced, so a negation consumer never
acts on the "not yet true" of a transient window. The evaluation model
requires: **negated reads of derived characteristics evaluate at the
fixpoint of the positive part.** *v0 mirror note:* v0 realises exactly one such boundary (it
sinks negation consumers below the entire positive computation — the
"salience −1" of the founding review); that is sound precisely when one
level suffices, i.e. when the negation dependencies are stratifiable
with a single negative stratum. The general condition — stratification
as IR well-formedness, covering nested and chained negation — is ch. 6
(`CONSTRUCTS.md` row 6; the ASP hand exercises show the strata being
read off the statute text).

Worked example (`ref/` test verbatim): producer "een Jutter is
vrijgesteld indien zijn inkomen kleiner is dan 100"; consumer "een
Jutter is bijdrageplichtig indien hij niet vrijgesteld is". Input:
inkomen = 50. Required fixpoint: **vrijgesteld waar, bijdrageplichtig
onwaar**. The transient-window failure this clause excludes: the
consumer evaluated before the producer sees "not yet vrijgesteld", sets
bijdrageplichtig true, and an executor that keeps that write answers
{waar, waar}, wrong. With inkomen = 200: **vrijgesteld onwaar, bijdrageplichtig
waar** (the negation legitimately holds at the fixpoint).

Worked example of the withdrawal (absolute; the golden
`jutterswet-kenmerk-aan-fixpunt` with the rules in the order below and in
the reverse order, replayed by gates 5, 6 and 12, and
`WV-6.1-written-characteristic` in `ref/test/`, which runs every order):
"Een Vondst is spoedeisend indien zijn meldingsgetal kleiner is dan 5",
"Een Vondst is spoedeisend indien zijn boete groter is dan 10", "De
spoedpremie van een Vondst moet gesteld worden op 1 indien hij spoedeisend
is" and "Het meldingsgetal van een Vondst moet berekend worden als zijn
aantal getuigen". V with aantal getuigen 10 and input meldingsgetal 3:
**meldingsgetal 10, not spoedeisend, spoedpremie leeg**; with boete 20 as
well: **spoedeisend, spoedpremie 1**; with spoedeisend in the input:
**spoedeisend, spoedpremie 1**. Both attributions read the meldingsgetal
and the boete stratum-strictly (ch. 6.1), so neither decides on a state
between in this document: measured 2026-11-02, the reference interpreter,
the ASP backend and both Drools branches answered it in both orders before
the decision as after it. Where an attribution has set a characteristic on
a state between, the reference interpreter withdraws it once it does not
decide (`WV-6.1-written-between`, on an input world no case can write).

### 5.4 Initialisation

An initialisation (§9.6) writes **only when the target is empty** — a
guard on the fact base, not an ordering device. Consequences, all
order-independent: a computed value from an ordinary rule **wins** over
an initialisation whichever fires first (computed-first: the
initialisation no longer matches; initialisation-first: the unguarded
computation overwrites); a target supplied with the input facts is left
standing; an initialisation alone fills the empty target. Worked example
(absolute): initialisation 25 beside the ch. 5.1 chain → **50**; the
initialisation alone → **25**. Phase 4 restates this guard as the
defeasible default it is (`CONSTRUCTS.md` row 7); the operational
account above is what it must reproduce.

**The guard reads the input, not the initialisation's own write.** Ch. 7.3
states the justification as "the target is empty in the input facts": a
case input switches the guard off, and so does a value another writer
places — the computed value above; the initialisation's own earlier write
does not. Its value is therefore an equation like any attribute write
(ch. 5.1, and ch. 6.1: attribute writes are revocable): it is re-evaluated
when a value it reads changes, and the fixpoint does not depend on the
order in which the rules are written. Another writer that writes the value
the initialisation holds has placed that value all the same: the
initialisation does not decide (`corpus/golden/jutterswet-lezer-van-initialisatie`,
case 3, in either order). Read off the running fact base alone,
the guard kept the value the initialisation had computed against a read
that another rule had not yet written. Measured on verifier-clean
documents: the reference interpreter and both Drools branches answered 0
where the ASP backend answered 26, and with the two rules swapped in the
document all four answered 26. Worked example (absolute; the golden
`jutterswet-initialisatievolgorde`): `bergloon` initialised to 2 ×
`berekend gewicht` where `gewicht` > 10, `vindloon` initialised to
`berekend gewicht` + 5, and `berekend gewicht` = `gewicht` + 1 written
after both. `gewicht` 12,00 gives **bergloon 26,00 and vindloon 18,00** in
either document order; `gewicht` 8,00 gives bergloon **leeg** and vindloon
**14,00**; a case input for either target stands.

On a timeline target the guard is read per day: the initialisation decides
the days on which the input reads no value and no writer decides (ch. 7.8).

Two initialisations on one target are ill-formed (`IR-SPEC.md` WF-079, its
initialisation arm), conditional or not: where both apply nothing ranks
them, and the fixpoint does not define which value the target takes — the
v0 guard let the first to fire decide, and the guard above lets the two
write in turn without end.

### 5.5 Creation in the loop

An object creation must be a function of the **fact base**, not of the
fire count: re-evaluating it against the same facts may not mint a
duplicate. The idempotence contract (v0 realises it with two guards —
a linked-instance check where the created object references its
subject, else a per-(rule, subject) creation marker): **one creation per
rule per subject** unless the fact base itself changed the question.
Executed: for each subject whose condition holds, a
deterministic instance of the created type is minted, the member
assignments apply (values evaluated on the subject, the input of the
object made, on a time-dependent attribute per day (ch. 7.8): a writer of the member displaces such a value where it
decides, and an initialisation of the member finds it filled, the stack of
ch. 7.8, `corpus/golden/jutterswet-invoer-naast-objectcreatie`; a characteristic
assignment's boolean-typed value sets the mark when it evaluates
true and, evaluating false **or empty**, leaves it unset — equivalent to
an absent initialisation, the closed-world reading characteristic checks
already apply; the empty flank follows the attribute-initialisation
parallel — an empty value writes silently there — and the closed-world
reading makes leeg and false indistinguishable to a characteristic check,
as an absent atom is both to the ASP backend; the boolean constant is its
v0 form), and new object and subject
are linked over the back-link relation — the explicit `connectingRole`
binding (`IR-SPEC.md` ch. 3.3) when present, else the
**uniquely-derivable** connecting relation — both ends.
The 1:1 shape (a to-one created-side role) realises the
linked-instance guard: an already linked instance — a case-supplied
one included — blocks the creation; the executed guard reads the
**input world** (a snapshot: v0's live fact-base negation is narrowed,
recorded — cross-form blocking through links laid during evaluation is
not mirrored), and asks whether the created-side role is already
**filled toward the subject** in it — the subject fills the *other*
role, the one this chapter's back-link writes on the mint (ch. 5.8a's
sense of "fills"). It does not ask which **leg** the case spelled: by ch. 5.7a
a supplied link establishes the relation itself, so a delivery solely on
the created side blocks exactly as a subject-side delivery does — the
reverse leg the loader materialises is a link like any other, and the
guard reads it. *(Until 2026-09-10 this clause read the other way: only
subject-side stored links blocked, and a created-side delivery minted
anyway. That was recorded here as the storage-direction narrowing, and
the ch. 5.7a decision **withdraws** it — the narrowing described a
question the case input no longer poses. The anchor
`jutterswet-rekenkamer` case 4 pins the symmetric reading with absolute
values: the subject's own attribute reads through the SUPPLIED
counterpart, not through a mint, in ref, clingo and the Drools backend
alike.)* Minted instances live in a
reserved label namespace (`::`), refused for case labels at load, and
are keyed by the creation's index — never its nullable name — so two
name-less creations can never collide. The to-many and linkless shapes realise the marker as
the deterministic mint itself. More than one connecting relation is a
**fault** — v0 silently drops the back-link there ("onopgelost
terugkoppeling"); execution refuses to guess (recorded delta, the red
witness `corpus/red/ambiguous-backlink`). And a to-one connecting
relation admits at most **one** creating writer (WF-052, `IR-SPEC.md`
ch. 3.4): a second writer's inevitable second filler is excluded
statically, never left to diverge between a faulting interpreter and
a guardless model.

Worked example (absolute; the anchor `jutterswet-penninguitgifte`
verbatim): every penninggerechtigde jutter gets a created Penning
(bedrag = the jutter's vondstwaarde; the characteristic `uitgereikt`
set) and, over a 1:1 relation, a created Dossier. Folds from the
subject read the mint: **1** penning totalling **12**, **1** dossier;
an ineligible jutter reads **0/0/0**; a consistency over every Penning
witnesses the characteristic on the created instance. The guards,
observable: a jutter who already holds an input-penning (bedrag 5,
not uitgereikt) gets a second one — **2** penningen totalling **12**,
input and mint mixing in one fold, and the un-marked input penning
turns the case **inconsistent** — while the input-dossier on the 1:1
relation blocks: **1** dossier, the linked-instance guard pinned.
Relation creation (fact creation) is naturally idempotent — creating an
already-present link is a no-op write. For each anchor whose condition
holds, the join chain (anchor → path steps → join hop, `IR-SPEC.md`
ch. 3.4) is navigated over the existing links, and anchor and target
are linked in the created relation — **both ends** are written, so
navigation from either side sees the link. A second distinct filler on
a to-one role is a fault, never a silent overwrite.

**The condition is about the anchor, or about the target, and the wire
says which.** This is the one construct whose guard has two objects in
scope, and a characteristic check's owner (`host`, ch. 5.8b) is what
names the host: absent, or naming a type the anchor satisfies, the guard
GATES the anchor exactly as written above — an anchor that fails it
creates nothing at all. Naming a type the TARGET satisfies, it FILTERS
the targets instead: the chain is navigated first and the condition is
asked of each target reached, so the anchor still creates, but only
toward the targets that pass. Nothing else in the fact creation changes,
and no other condition form has a second object to be about (WF-109
refuses an owner that names neither, and one that names both). The
filtering reading is admitted only where the check IS the whole
condition, does not navigate and carries no whole-period qualifier
(`IR-SPEC.md` ch. 4.6 (q2)); outside those three the anchor is the only
bound object and an owner naming anything else is refused.

Worked example (absolute; the anchor `jutterswet-gemeenteberging`
verbatim): a kustgemeente creates `gemeentelijke berging` links to
every vondst its ingeschreven jutters have aangemeld (one path step,
then the join hop); som and aantal folds *over the created relation*
yield **51** and **4** — the vondst reached along two jutters is
linked once, the idempotence observable in the count — while a
gemeente that is not a kustgemeente, or whose chain runs dry, creates
nothing and the folds read the empty set (**0** and **0**). The target
side reads back: an aantal fold on the Vondst over the `bergende
gemeente` end counts **1** for every geborgen vondst and **0** for the
vondst of the non-coastal gemeente — the both-ends write, witnessed
from the far end.

**Recursive creation** (the dialect rule group, `IR-SPEC.md` ch. 3.9)
deliberately breaks the acyclicity of ch. 5.2 in one controlled way: a
creation whose output feeds its own condition advances **exactly one
generation per instance**, and the chain runs to its fixpoint within
the single evaluation. A self-typed chain names its back-link role
explicitly (`connectingRole`, `IR-SPEC.md` ch. 3.3 — the type-pair
derivation is role-ambiguous there and mirrors v0's null). Termination
is the author's obligation — every
**object-creation member** of a recursive group MUST be conditional
(WF-033; relation creations need no guard, being idempotent) — and the
runtime's: the **creation budget**. Every minted instance has a
creation depth in the stratum of the creation that minted it: one more
than its subject's depth in that stratum, where an input instance, and
an instance a lower stratum minted, has depth 0 (a lower stratum is
complete before a higher one starts, ch. 6.3; a subject is never a mint
of a higher stratum, since a creation reads its subject's population
strictly, ch. 6.1, and an executor may hold that as an invariant and
fail loudly where it breaks). A mint at depth 101 is a **fault**, never
a silent truncation: the reference interpreter and the Drools backend's
truth-maintenance branch fault at the creation, and the ASP backend
derives a named `viol/2` witness → UNSAT, each with the words "creation
budget exhausted". The depth is a function of the chain of creations, not
of document order nor of the generation in which a creation fires: a
creation whose condition waits for a link or a value keeps its
subject's depth. Fact creations carry no budget — they mint nothing,
and over the finite universe a stratum's mints leave, the links are
finite — nor do the forms that make nothing (their fixpoint exists,
WF-045; an executor may bound their rounds only to name two writers of
one target that keep overwriting each other, ch. 5.7). The budget
bounds the height of each stratum's tree of mints; each (creation,
subject) mints at most once, so every stratum ends after finitely many
generations. Its breadth stays the author's obligation (WF-033). Until
2026-10-09 the reference interpreter bounded 100 passes of a stratum
and the ASP backend the depth of a chain of mints over all strata, and
the two parted near the bound: on a chain of exactly 100 mints, on a
chain whose mints wait for links, and on a closure of more than 100
hops.

**Resource bounds** (card `creatiebudget-ref-en-clingo`, 2026-10-09).
The creation budget is the norm's only bound on recursive creation: fact
creations and the forms that make nothing have none, and a closure of
any length is a result. An executor may carry a named resource bound
of its own beside it. Such a bound is no norm: a run that meets it ends
loudly in that executor, never with a truncated result, where an
executor without that bound completes the same run. Two are recorded.
The Drools backend fires a case, in both its branches, under a cap of
max(10 000, 4 × R × N0) firings, R the rules of its KieBase and N0 the
facts just before the fire, computed again at the start of every
creation phase (`backends/drools/README.md`); both branches carry the
creation budget of this chapter. A run that needs more firings ends at
the cap without a fixpoint and is reported as such. Measured 2026-10-09
on the anchor `jutterswet-erflijn` over a chain of 450 input Wachters,
whose closure has 101 025 links: the reference interpreter and the ASP
backend give erfvolgeraantal(W1) = **449**; the truth-maintenance branch
of that day ended at a fixed cap of 100 000 firings, and the two
destructive branches did not carry the document. Measured again
2026-11-06, with the cap that grows with the facts: both Drools branches
give **449** (`PLAYED`, in 719 s on the exec-model branch and 213 s on
the DRL-text branch).
The ASP backend refuses by name (`viol("term nesting bound reached …")`,
the second posture of `IR-SPEC.md` ch. 5.5) a chain of 190 nested
creations over all strata in a document with a recursive object
creation, which the reference interpreter plays
(`backends/clingo/README.md`, Recorded limitations).

Worked example (absolute; `ref/` tests `WV-5.5-budget-wacht-op-link`
and `WV-5.5-budget-erflijn`): "werf de opvolger" gives a Wachter whose
rang is below N and who has a maatgever an opvolger with rang plus 1,
and "maak maten" links every Wachter as maatgever to its opvolger, both
members of the recursive group "estafette"; W1 (rang 1) has the
maatgever W0 (rang 0) in the input. W1 mints W2 in generation 1, W2
gets its maatgever in generation 2 and mints W3 in generation 3, so
W(k + 1) comes in generation 2k − 1 at depth k. With N = 101 the chain
plays: **102** Wachters, W101 minted at depth 100 in generation 199
with rang **101**, and the consistency "rangdetector" (a rang below
101) deposits **one** fact, on W101. With N = 102 the mint of W102 at
depth 101 is the fault. Over a chain of 103 input Wachters the anchor
`jutterswet-erflijn`, whose recursive fact creation walks an erflijn
link and then an opvolger, lays the 102-hop link W1 → W103 in the 101st
generation of its stratum and plays: erfvolgeraantal(W1) = **102**, the
channel empty.

**Generations** (card `creatie-initialisatie-leest-groeiende-waarde`,
2026-10-09). Within a stratum, evaluation proceeds in generations.
Generation 0 is the fixpoint of the stratum's forms that make nothing —
rules, allocations, defaults groups — over the world the lower strata
left (for stratum 0, the input). Generation G + 1 is what every creation
of the stratum, object creation and fact creation alike, derives from
generation G's settled world: every creation reads that same world, so
none reads what another creation of its own generation mints or links,
whatever their order in the document. After it the forms that make
nothing settle again, stratum by stratum (ch. 6.3), those of the lower
strata included, since they compute on the minted instances as well.
The stratum is done when a generation mints and links nothing. Its
consistency and uniqueness forms fire then, on the world it ends with.
Those of every lower stratum fire again in each of its generations,
generation 0 included, once their own stratum has come to rest, so they
also check the instances a higher creation mints: what they read is
written by their own stratum or a lower one, which no longer changes it
on an instance that exists and has computed it on a minted one by then.
In the `ref/` tests `WV-6.3-consistentie-na-een-creatie` and
`WV-6.3-uniciteit-na-een-creatie` (absolute, each in two rule orders) a
creation of stratum 2 gives every open Haven a Post, and a rule of
stratum 0 sets the a of every Post to 5. With one open Haven the
consistency "controle post" of stratum 1 (the a of a Post is below 5)
deposits **one** fact, on that Post; with two open Havens the
uniqueness "eenmalige a" of stratum 1 (the a of every Post is unique)
deposits a fact on **both** Posts. Until 2026-10-09 these forms fired
once, at the fixpoint of their own stratum, and both channels stayed
**empty**. Within their own stratum they wait for its last generation:
a consistency reads a positive characteristic non-strictly (ch. 6.1),
so it can share a stratum with a cycle that still makes that
characteristic grow, and a channel fact is never withdrawn (ch. 5.6). In
the `ref/` test `WV-5.5-consistentie-op-een-groeiend-kenmerk`
(absolute), "ervaren wachter" marks a Wachter that has a maatgever, the
fact creation "geef door", the member of the recursive group
"doorgeven", makes an ervaren Wachter the maatgever of its opvolger, and
the consistency "elke wachter ervaren" asserts that a Wachter is
ervaren; all three are in stratum 0. With W1 → W2 → W3 over opvolging
and W1 ervaren in the input, W2 is ervaren after generation 1 and W3
after generation 2, and the channel stays **empty**. Fired on generation
0's world, where only W1 is ervaren, the consistency would deposit a
fact on W2 and one on W3. A uniqueness reads its keys strictly and sits
a stratum above every writer of a key, so it never sees such a world:
in `WV-5.5-uniciteit-op-een-groeiende-sleutel` (absolute, in both orders
of its two rules) the key volgnummer, the number of maatgevers plus the
basis, is 5 for both W1 and W2 on generation 0's world and 5, 6 and 1
at the end, and the uniqueness "uniek volgnummer" deposits **nothing**.
A creation's condition and member assignments are
evaluated on the settled world it reads, which is the moment of
creation; this is the
reading of "values evaluated on the subject" above and of "exactly one
generation per instance" in the paragraph before. A later generation
may change the value read; the minted instance's member assignments are
written once and do not follow it.

Worked example (absolute; `ref/` tests `WV-5.5-generaties-keten` and
`WV-5.5-generaties-kopie`): a Wachter carries the whole-number
attributes rang, stand, beginstand and opvolgerbegin, and the fact type
opvolging gives one voorganger one opvolger, both Wachters. "stand van
een wachter" counts the Wachter's opvolgers; "opvolgerbegin" reads the
beginstand of its opvolger; the object creation "werf de opvolger", the
member of the recursive group "estafette", gives a Wachter whose rang is
below 4 an opvolger over `connectingRole {opvolging, opvolger}`, with
rang its rang plus 1 and beginstand its stand. W1 with rang 1 mints W2
in generation 1, W2 mints W3 in generation 2, W3 mints W4 in generation
3, and W4 (rang 4) mints nothing. W1 mints on generation 0's settled
world, where it has no opvolger and its stand is 0; afterwards its stand
is 1. In either document order of the two rules: four Wachters,
**stand(W1) = 1**, **opvolgerbegin(W1) = 0**, and **beginstand 0** on
every minted Wachter. The copy variant adds "De standkopie van een
Wachter moet gesteld worden op zijn stand" and initialises the
beginstand from the standkopie: the same values in every order of its
three rules. Until 2026-10-09 the reference interpreter ran the
creations inside a pass over the running world, so the moment of creation
was a position in the pass; with the copy rule written before the
count, the initialisation read a copy the pass had not computed yet and
opvolgerbegin(W1) was **leeg**. A fact creation belongs to the
generation in the same way (`ref/` test `WV-5.5-generaties-feitcreatie`):
with "maak maten" in the group, linking a Wachter as maatgever to its
opvolger, and the beginstand initialised to the number of maatgevers,
every minted Wachter starts at **beginstand 0** in either rule order,
where a pass that ran the fact creations before the object creations
gave 1 from W3 on.

The ASP backend has no moment of creation: a member assignment is a head on
the minted term, read in the stable model, so it reads the end value; on
the worked example it answered opvolgerbegin(W1) = 1. Carrying the
generation reading would take a copy of the rule layer per generation,
so the backend takes the second posture of `IR-SPEC.md` ch. 5.5. It
refuses by name every member assignment of an object creation that
reads a node a later generation of the creation's own stratum can still
change: a node with a writer that a creation on a dependency cycle in
that stratum reaches in the dependency graph, the creation itself
included where it lies on such a cycle (WF-045 admits the cycle through
a recursive group). Only such a creation changes, after the creation, a
value the member assignment reads; a later stratum changes none (ch. 6.3).
Refused are what a rule, an allocation or a defaults group writes there,
also where a creation writes the same member, and a population or
relation that a creation on the cycle grows (the number of the
subject's opvolgers over the creation's own back-link, the number of its
maatgevers over a relation a fact creation of the group lays). A
creation off the cycle can share the cycle's stratum, since a positive
characteristic read is not stratum-strict (ch. 6.1): beside "geef door"
and "ervaren wachter" of `WV-5.5-consistentie-op-een-groeiend-kenmerk`
above, the object creation "rapport van een haven" gives a Haven a
Rapport whose aantal is the number of the Haven's wachters that are
ervaren. It makes the Rapport in generation 1; over W1 → W2 → W3, all
three wachters of H1 and W1 ervaren in the input, the reference
interpreter answers rapportaantal(H1) = **1**, where the backend
answered 3 until 2026-10-09 and now refuses the document. A creation a
stratum higher than the cycle reads the end
value, as the reference interpreter does, and stays carried: with the
aantal read from an attribute of the Haven that counts its ervaren
wachters, both answer **3**. An attribute or characteristic that only
creations write is set once per object, at its creation, so its end value
is the value at the creation, and a read of it stays carried: the anchor
below reads its rang so. A read of a member that a creation writes is
refused as well when a rule on the cycle writes that member too: with
the rang assigned the rang plus 1 and the beginstand the rang,
and a rule that sets the rang of a Wachter with an opvolger to 2, W1's
opvolger starts at **beginstand 1** while W1 ends at **rang 2**, and
the backend refuses the document. The refusal reads the static
dependency graph and its strata, not the values. So a rule on the cycle
whose value on an object is fixed at the object's creation (a basis
computed as the rang plus 10) is refused as well, although the end
value would be right, and so is a writer on the cycle whose validity the
case's date excludes; only a creation version the date excludes is not
asked, since the backend does not translate it, and such a version does
not count as a creation that reaches a writer either: it mints and links
nothing, in any executor, so no later generation brings a change from
it. With "geef door" valid from 2027-01-01 and a case on 2026-01-01 no
maatschap is laid, W3 stays without ervaren, and both the reference
interpreter and the backend answer rapportaantal(H1) = **1**; valid from
2025-01-01, the backend refuses the document as above. The backend's
suite pins the refusals, each beside the reference interpreter's
absolute answer, and the carried forms (`ch5.5 generations`, `ch5.5
generations off the cycle`); no corpus document has a refused form.

The condition of a creation, an object creation or a fact creation, is
read in the same way and refused by name on the same nodes and writers
(CLINGO-038), except where the read stands on a position on which a
condition that held cannot be lost while the stratum grows; there the
condition holds in the stable model exactly where it held on some
generation, and the backend carries it. Ch. 6.1 names the positions and
gives the forms with their absolute answers (the closed residual; the
suite's `ch5.5 creation condition on the cycle`).

Worked example (absolute; the anchor `jutterswet-wachtersketen`
verbatim): the chain "a wachter of rank n recruits an opvolger of rank
n + 1, while n < 5", linked over `opvolging` via the explicit
`connectingRole {opvolging, opvolger}`. From rank 1 the chain
terminates at rank 5 — measured: the direct-successor fold on the
labelled start reads **2** (from rank 4: **5**), the rank-detector
consistency (rank < 5) turns the case **inconsistent** exactly because
generation five exists, and an ineligible start (rank 7) recruits
nothing (fold **0**). The runaway variant (condition rank ≥ 1) is the
red witness `corpus/red/runaway-recursion`: both executors fault
their budget, loudly.

### 5.6 The error channel, operationally

§9.5 inconsistencies are **facts on a channel**, not exceptions: the
computation degrades locally (ch. 1.2's error-empty rules) and the error
surfaces beside the result. Operational clauses, mirrored:

- Producers: violated consistency assertions, uniqueness violations
  (a duplicate non-empty key; two keys are the same key under the
  ch. 1.2d value equality, component by component — an enumeration
  component therefore up to the ci label identity of WF-008, so two
  spellings of one declared label collide), the allocation degrades of
  ch. 4.3/4.4, division by empty (dividend filled), division by a
  numeric zero (dividend filled; ch. 1.4 point 4), a negative
  radicand, an empty variable-start anchor (ch. 3.2).
  Worked example (golden `jutterswet-keurspelling` case 4, verbatim in
  gates 5–6, and `WV-5.6-enum-sleutel` in `ref/test/`): under
  "De staten van alle Vondsten moeten uniek zijn." over the domain
  {`gaaf`, `beschadigd`, `wrakhout`}, V1 with staat `Beschadigd` and V2
  with staat `beschadigd` are **both marked** under "eenmalige staat";
  `Beschadigd` beside `gaaf` marks neither.
- Inserts are **idempotent** (one error fact per rule × subject, however
  often re-evaluated) and **validity-gated**: a version whose window
  excludes the evaluation date raises nothing (the version simply does
  not apply — ch. 3.10's gate).
- A deposit is an outcome of one evaluation (ch. 5.7): the fact stands
  while an evaluation of its form at the fixpoint deposits it.
- Every producer deposits with the **code** of its kind, a row of ch. 5.7b
  on the channel `§9.5`. The identity of a fact stays rule × subject; the
  codes deposited for that pair form a set beside it, each code once. A
  deposit of another kind for the same pair adds its code to the one
  fact, so the idempotency above is unchanged. A kind that has no code
  yet (ch. 5.7b, "Kinds without a code") deposits the fact and adds no
  code. The channel read below does not look at the set. A case's
  `inconsistent` expectation may pin a code
  (`spec/schema/uchar-case-v0.schema.json`): a fact that matches the
  expectation's rule and label must carry that code among its codes.
  Worked example (golden `jutterswet-rangmaximering` case 3, replayed by
  gates 5, 6 and 12): an ordered allocation of a pot of 1000 over three
  Wachters by wachtjaar, weighted by voorranggewicht, capped by
  penningmaximum and floored to whole units. W1 (2010, weight 1, cap 300)
  receives **300**. In the group 2020, W2 has neither a weight nor a cap
  and receives a real **0**, and W3 (weight 1, cap 800) receives **700**;
  the rest is **0**. One fact stands, (verdeel het penningbudget van een
  penningkas × W2), and it carries **both** EF-029 (the empty tie-break
  weight) and EF-030 (the empty cap); W1 and W3 have no fact.
- The channel is **readable**: "regelversie X is inconsistent" is an
  existential test over the channel for X — it has no subject of its
  own, so one flag makes the reading rule applicable to every instance
  of *its* subject. Reading makes the channel part of the dependency
  graph: a channel-reading rule is downstream of every producer for X
  (ch. 5.2/5.3 apply). An unknown rule name in the test is ill-formed
  (`CONSTRUCTS.md` row 30's split verdict; v0 declines it).
- "regelversie X **is gevuurd**": *mirror note, corrected against the
  pinned code* — the pin **does** lower it, as an existential over an
  opt-in firing marker inserted unconditionally at the top of X's
  consequence (a no-change write still counts as "fired"; only a
  reference to a non-instrumentable target declines). The pinned
  *decision records* still describe it as declined — they are stale, and
  `CONSTRUCTS.md` row 30's non-goal verdict rests on them. **Decided (row
  30):** v1 admits it only as a declarative model predicate — "the
  condition of X holds at the fixpoint" — in the RegelSpraak dialect,
  never the core; no execution record exists in the model. Edge cases
  where the pin's firing-marker lowering diverges from the fixpoint
  reading route to differential triage.

Worked example (channel read, `ref/` test verbatim): consistency rule
"controle saldo" asserts saldo ≥ 0 over Rekeninghouder; a second rule
sets the characteristic "onder verscherpt toezicht" on every Dossier
"indien regelversie controle saldo inconsistent is". Input: houders
{h1: saldo −5, h2: saldo 10}, dossiers {d1, d2}. Fixpoint: **one**
error fact (controle saldo, h1) — re-evaluation inserts no duplicate —
and, the flag existing, **both** d1 and d2 carry the characteristic:
the reading rule has no subject of its own and applies to every
instance of *its* subject type.

### 5.7 The evaluation session

The operational happy path both backends share, and the shape `ref/`
implements:

1. fix the **evaluation date** (one date; the single axis of PLAN §9,
   decision 2 — absent when the model is time-free);
2. bind **parameter values** for that date (vintage selection is
   fail-fast — an overlap is a load error, a coverage gap a
   session-start error, never a default; the phasing is ch. 8.4);
3. insert the **input facts**;
4. evaluate to the **fixpoint** (ch. 5.1–5.5; a bounded budget turns
   divergence into a fault);
5. read the **end state**: all instances with their attributes,
   characteristics, created objects and relations, and the error
   channel. Engine-internal control facts (creation markers and kin) are
   not part of the observable outcome.

**Refusals of the case input (step 3).** The executors read the case
before any rule fires and refuse it there, by name and never by a guess,
in three ways beside the input-precision fence of ch. 2.4 and ch. 11.3
(fault kind EF-004, ch. 5.7b). A role key that resolves to two roles of
the instance's object type is refused, since a link cannot be filed under
two relations (EF-001; IR-SPEC ch. 2.6). A role link that names a label
the case does not declare is refused, since it names no instance
(EF-002). A date lexeme that names no calendar day — `2026-02-30`, which
the case schema's pattern admits because a pattern checks field widths and
not a calendar — is refused, naming its position and the lexeme (EF-003).
The cells of a dimensioned attribute are refused where their labels name no
cell of the attribute (EF-034) and where the input names one cell twice
(EF-035), and a scalar or a timeline on a dimensioned attribute is a case
error (ch. 5.8c).

**Divergence by two writers of one target.** Two rules that write one
attribute and whose conditions both hold on one instance demand, by
ch. 5.1, that the attribute equal each of their values; a rule whose value
is leeg demands nothing (ch. 3.11). Where the values differ, no fact base
satisfies both equations: there is no fixpoint, and the session
faults. The fault names the writers, the values they write and the
attribute and instance; it names no cycle, since the dependency graph has
none (WF-045). Where at most one of the conditions holds, or the writers
write the same value, the equations hold together and the document plays.
WF-079 refuses two unconditional writers statically; a pair with
conditions is well-formed and is decided here. Worked example
(`corpus/red/jutterswet-dubbele-melding`,
`corpus/golden/jutterswet-melding-naar-aard`): "De meldingsdatum van een
Vondst moet gesteld worden op zijn vinddatum indien zijn vinddatum gevuld
is" beside the same rule on its aanspoeldatum; V with vinddatum 04-03-2026
and aanspoeldatum 01-03-2026 faults, naming both rules and the values
2026-03-04 and 2026-03-01; V with only one of the two dates gets that
date; V with both dates on 02-03-2026 gets **02-03-2026**.

**The writers that decide at the fixpoint** (decided by the maintainer
2026-11-02). The value of a target with two or more writers is that of the
writers that decide at the fixpoint, in every order of the rules: where
none decides, the target keeps its input value, takes its initialisation's
value or stays leeg; where one decides, it takes that writer's value, and a
writer that fires with a leeg value does not decide (ch. 3.11); where two
decide with different values, the session faults as above. This is the stack
of ch. 7.8, in which an allocation is a writer of the same rank as the
computing rules. A value a writer wrote on a state
between, where its condition held on a value another rule replaces, does
not stand (ch. 6.1, "A write is read at the fixpoint"). Worked example
(absolute; the golden `jutterswet-twee-schrijvers-aan-fixpunt` with the
rules in the order below and in the reverse order, replayed by gates 5, 6
and 12, and `WV-6.1-written-two` in `ref/test/`, which runs every order):
"De spoedpremie van een Vondst moet gesteld worden op 1 indien zijn
meldingsgetal kleiner is dan 5", "De spoedpremie van een Vondst moet
berekend worden als zijn boete indien zijn meldingsgetal groter is dan 20"
and "Het meldingsgetal van een Vondst moet berekend worden als zijn aantal
getuigen". V with aantal getuigen 10 and input meldingsgetal 3 gives
**meldingsgetal 10, spoedpremie leeg**, and with input spoedpremie 7
**spoedpremie 7**; aantal getuigen 30, meldingsgetal 3 and boete 4 give
**4**; aantal getuigen 2, meldingsgetal 30 and boete 4 give **1**; aantal
getuigen 10, meldingsgetal 30, boete 4 and input spoedpremie 7 give **7**;
aantal getuigen 30, meldingsgetal 3, no boete and input spoedpremie 7 give
**7**: the second rule fires without a boete and decides nothing. Measured 2026-11-02 before the decision: with the rules in this
order the reference interpreter answered the first, second and fifth case
with the value a writer had written on the input meldingsgetal (1, 1, 4),
the ASP backend refused every case with an input spoedpremie, and both
Drools branches answered as above in both orders.

**Two writers per day** (`spec/proposals/IMPLICIT-TIME.md` point 5; the
specification is silent, and the reading follows ALEF). On a timeline
target the paragraphs above hold per day, for one writer as for two or more:
a writer decides the days of the segments it writes — its value restricted
to the periods of its condition (ch. 3.10) — and a day on which its timeline
reads leeg it does not decide (IMPLICIT-TIME VB 5.1). The target is the
union of the writers' segments: on a day one writer decides, its value; on a
day none decides, what the target held before any of them wrote, its input
or leeg (ch. 3.11, "Leeg in a write", with the worked example
`WV-3.11-per-dag-invoer` of one writer beside the input); on a day two
decide with different values, no fact base satisfies both equations and the
session faults with EF-016, once per attribute and object, naming the
writers in the order of the document and their values on the first such
day (VB 5.2). Two that decide a day with one value hold together; each
writer's segments stand as it wrote them, and a shared day lies in the
segment of the writer the document names first. Worked example
`WV-5.7-per-dag` (absolute; `ref/` tests verbatim,
the fixtures `ref/test/fixtures/conditions-per-period/afdracht` and
`afdracht-botsing`, shared by the ASP backend's suite): `rentedragend` on
[2025-11-16, 2026-03-13); writer 1 sets the rentedagen to 30 gedurende de
tijd dat hij gedurende de gehele maand rentedragend is, writer 2 counts the
days gedurende de tijd dat hij niet gedurende de gehele maand rentedragend
is. Together: **0** on (−∞, 2025-11-01), **15** on [2025-11-01, 2025-12-01),
**30** on [2025-12-01, 2026-03-01) (writer 1's one segment), **12** on
[2026-03-01, 2026-04-01) and **0** on [2026-04-01, +∞) (writer 2's day count
over all time, ch. 3.8). Without writer 2's condition both decide December
2025, with 30 and 31: EF-016 on A1.rentedagen, the rules "rentedagen in een
gehele maand, rentedagen in elke maand", the values 30 and 31. The corpus
witnesses are `corpus/golden/jutterswet-wachtdagen-twee-schrijvers` and
`corpus/red/jutterswet-wachtdagen-botsing`.

**Faults and deposits are read at the fixpoint.** An evaluation fault
(ch. 1.4) and a §9.5 deposit are outcomes of one form's evaluation on one
subject. Re-evaluation under freshness (ch. 5.1) replaces the earlier
outcome, and a default a higher default displaces (ch. 7.1), or an
initialisation whose guard blocks it (ch. 5.4, 7.3), is not evaluated;
the session reports the outcomes standing at the fixpoint of the form's
stratum. A fault stands in place of what the form would write, so a form
that faults at the fixpoint leaves the session without an answer whichever
states came before it, and where several stand the report names them all.
What such a session derives beside its faults is not normed: the reference
interpreter sets the world back to the state before the evaluation that
faulted, so its target keeps the value it had there, and the Drools backend
withdraws the value of the form, so its target is leeg or holds its input;
a reader of the target, and the deposits and faults that follow from what
it reads, can differ between executors. The outcome of the session is the
faults that stand; gate 8 holds the executors to their codes and
parameters (ch. 5.7b). Worked example (`corpus/red/jutterswet-verdeling-past-niet`,
`ref/` test `WV-5.7-schrijfgrens`): the reference interpreter and the Drools
backend each report the one fault EF-006 of the allocation on V1, at
J1.veilingdeel, value 1001/2, decimals 0.
An initialisation tests its guard (ch. 5.4) before its condition and its
value, so a target the input fills or another writer's value displaces
evaluates neither. Worked examples (absolute; goldens with the rules in the
order below and in the reverse order, replayed by gates 5, 6 and 12, and
`WV-5.7-fixpoint` in `ref/test/`, which runs every order of the rules). A
Vondst with whole numbers `taxatie`, `opbrengst`, `aantal deelnemers`,
`bergloon`, `aandeel` and `toeslag`, and characteristics `gemeld` and
`erkend`, where every document opens with "Een Vondst is erkend indien hij
gemeld is":

- **A** (`jutterswet-initialisatie-naast-regel`): "Het bergloon moet gesteld
  worden op zijn taxatie indien hij erkend is" and "Het bergloon moet
  geïnitialiseerd worden op zijn opbrengst gedeeld door zijn aantal
  deelnemers rekenkundig afgerond op 0 decimalen". V gemeld, taxatie 30,
  opbrengst 100, deelnemers 0 gives **bergloon 30, consistent**, in either
  order: the computation decides, so the initialisation does not, and its
  division is not a deposit of the session; with deelnemers 3 as well. V not
  gemeld, otherwise the same: the initialisation is evaluated, **bergloon
  leeg** (the rounding of a leeg quotient is leeg), and the fact
  (standaardbergloon × V) with EF-005 stands. An initialisation that
  stores nothing holds no target (`jutterswet-lege-initialisatie-naast-regel`):
  beside "Het bergloon moet berekend worden als zijn opbrengst", "Het bergloon
  moet geïnitialiseerd worden op zijn taxatie indien hij gemeld is" gives V
  gemeld with taxatie and opbrengst leeg **bergloon leeg, consistent**, in
  either order, and with taxatie 5 and opbrengst 40 **bergloon 40**.
- **B** (`jutterswet-lezer-van-initialisatie`): "Het bergloon moet gesteld
  worden op zijn taxatie indien hij erkend is", "Het bergloon moet
  geïnitialiseerd worden op 0" and "Het aandeel moet berekend worden als zijn
  opbrengst gedeeld door zijn bergloon rekenkundig afgerond op 0 decimalen".
  V gemeld, taxatie 30, opbrengst 90 gives **bergloon 30, aandeel 3,
  consistent**, in either order: the 0 of the initialisation is a state
  between, in which an aandeel written first divides by zero, and that
  outcome is replaced. V not gemeld: the 0 stands, **bergloon 0, aandeel
  leeg**, and (aandeel × V) with EF-005 stands.
- **E** (`jutterswet-fout-op-tussenstand`), a Vondst with whole `opgegeven
  termijn`, `vervalmaand` and `bergtermijn` and a date `vervaldag`, the hard
  fault on a state between: "De bergtermijn moet gesteld worden op zijn
  opgegeven termijn indien hij erkend is", "De bergtermijn moet
  geïnitialiseerd worden op 30" and "De vervaldag moet gesteld worden op de
  datum met jaar: 2025, maand: zijn vervalmaand en dag: zijn bergtermijn". V
  gemeld, opgegeven termijn 28, vervalmaand 2 gives **bergtermijn 28,
  vervaldag 2025-02-28, consistent**, in every order: on the state between,
  where the bergtermijn is 30, a vervaldag evaluated first names no calendar
  day (EF-032, ch. 3.8), and that outcome is replaced. With opgegeven termijn
  31 and vervalmaand 4 the day 31 of April stands at the fixpoint and faults
  the session (`corpus/red/jutterswet-fout-aan-fixpunt`).
- **C** (`jutterswet-voorwaarde-onder-invoer`): "De toeslag moet
  geïnitialiseerd worden op 5 indien zijn opbrengst gedeeld door zijn aantal
  deelnemers groter is dan 10". Opbrengst 100, deelnemers 0, toeslag 7 in the
  input gives **toeslag 7, consistent**. Without that input the condition is
  evaluated: **toeslag leeg**, and (standaardtoeslag × V) with EF-005 stands.
- **D** (`jutterswet-deponering-onder-invoer`), a Jutter with `b` and
  `heffing`: "De heffing moet geïnitialiseerd worden op 7 indien 5 gedeeld
  door zijn b leeg is". Heffing 9,00 in the input and b leeg gives **heffing
  9,00, consistent**. Without that input **heffing 7,00**, and (heffing van een
  jutter × J) with EF-031 stands.

`Unresolved` nodes take no part in evaluation and must survive into the
report (WF-044); an engine that reaches one fails that rule's evaluation
observably rather than guessing.

Worked trace (`ref/` test verbatim, over the penningdrempel golden):
evaluation date absent (the model is time-free); parameters
{vondstdrempel: 12}; inputs {Jutter j1: aantal aangemelde vondsten 14;
Jutter j2: aantal aangemelde vondsten 3}. Fixpoint after one pass: j1
**penninggerechtigd**, j2 not; no channel entries; end state = the two
instances plus nothing else.

### 5.7a The case input: what a supplied role link establishes

Step 3 of the session inserts the input facts. Attribute and
characteristic input is per instance and needs no further law. A **role
link** does, because it names one end of a relation that has two, and
the reader that arrives from the other end must be told what it sees.

**A supplied role link establishes the relation instance itself, and
both roles read it.** A relation is symmetric: the case names one leg,
and the reader navigating from the side the case did NOT name reaches
the same relation instance the naming side reaches — never a spuriously
empty far side. This holds for **every** fact type; the reciprocal shape
(one role that is its own opposite, WF-011/013, ch. 4.1) is the special
case in which the two legs are one and the same role. *(Decided by the
maintainer 2026-09-10, on the numbered question the measurement round
required: the alternative reading — a link is directed, and the
unsupplied direction reads leeg — makes the same relation compute
different answers depending on which end the case happened to spell,
and no engine can hold that reading on a 1:N fact model, where one
stored reference necessarily serves both directions.)*

Two consequences, both mechanical:

1. **The loader materialises the reverse leg.** For every supplied link
   from instance `a` under a key naming role `far`, the loader adds `a`
   to each target's links under the **sister role** — the role `a`
   itself fills. It runs as a separate pass over the finished input, so
   the outcome cannot depend on the order instances appear in, and it is
   idempotent: a case that supplies both legs describes one relation,
   not two. A conflicting reverse leg on a **to-one** sister role is a
   load error (WF-013), never a silent overwrite.

   *Recorded residue — member ORDER.* A supplied to-many list carries
   the order the case wrote it in; a materialised one accumulates its
   members in **ascending label order** — plain lexicographic order over
   the raw labels, that being the order the pass walks the input in, not
   the order the case file lists them in. The two need not coincide, so
   a construct that exposes member order — an ordered allocation's tie
   group, a first/last fold — can see the difference between the two
   spellings. Nothing this chapter decides is affected (membership and
   cardinality are order-free, and ch. 4.2 makes a tie group
   simultaneous), so this is a residue rather than a divergence; it is
   written down instead of left to be discovered. The two orders never
   meet in one list: measured over the whole corpus, the pass extends an
   already non-empty list in six goldens, and in every one of them that
   list is one the pass itself built earlier in the same walk — no
   case-written list is ever appended to.
2. **A reference-field fact model writes what it can hold.** Where an
   executor stores one scalar reference per to-one role, a **1:N**
   relation has exactly ONE field and that field already serves both
   directions; a **1:1** relation has TWO, and the case-input layer must
   fill the second one itself. An executor whose model cannot hold a
   direction refuses the form with its reason — it never reads a
   spuriously empty side and calls that an answer.

Worked example (`ref/` test verbatim; the golden `jutterswet-ketenband`,
cases 1–3, replayed by gates 5 and 6). The fact type `ketenband` relates
`Wachter` to `Wachter` through two distinct roles: `voorligger` (to-one)
and `opvolger` (plural `opvolgers`, to-many). Two rules: `voorliggerrang`
is the `rangnummer` of the `voorligger`; `opvolgeraantal` is the number
of `opvolgers`. Three Wachters W1, W2, W3 with `rangnummer` 10, 20, 30,
and ONE relation — W1 is the voorligger of both W2 and W3 — spelled
three different ways:

| the case supplies | `voorliggerrang` W1/W2/W3 | `opvolgeraantal` W1/W2/W3 |
|---|---|---|
| both legs: `W1{opvolgers:[W2,W3]}`, `W2{voorligger:[W1]}`, `W3{voorligger:[W1]}` | leeg / 10 / 10 | 2 / 0 / 0 |
| the to-many leg only: `W1{opvolgers:[W2,W3]}` | leeg / 10 / 10 | 2 / 0 / 0 |
| the to-one legs only: `W2{voorligger:[W1]}`, `W3{voorligger:[W1]}` | leeg / 10 / 10 | 2 / 0 / 0 |

The three rows are identical, and that identity is the law: how a case
spells a relation does not change what the model computes from it. The
values naming the *unsupplied* direction carry the weight — row 2 reads
`W2.voorliggerrang = 10` although no case ROLE line names W2 at all,
and row 3 reads `W1.opvolgeraantal = 2` although no case role line names
W1. (Each instance still carries its own `rangnummer`; it is the
relation, not the instance, that is spelled from one side only.) W1's own `voorliggerrang` is **leeg** in every
row, and that is the flank: nothing fills `voorligger` toward W1, so the
absent-link rule of ch. 5.8 reads leeg — establishing a relation in both
directions is not the same as inventing one.

### 5.7b Fault kinds and their codes

A **fault kind** is a property of the semantics, not of an executor: a
context value that needs an evaluation date the case does not carry is the
same fault whether an executor meets it while it reads the case, translates
the document or evaluates it. Every
kind has a code, `EF-###` (the `E` of evaluation, beside the `WF-###` rules
of `IR-SPEC.md`), listed in the table below with its channel, its
parameters, the clause that defines it and its witnesses.

- **Codes.** Numbered from EF-001 in order of arrival. A code is never
  reused and never renumbered; a retired code stays in the table, marked
  retired, as a WF rule does. One namespace serves every channel, so a
  kind that moves to another channel keeps its code.
- **Channels.** `case input`: the case is refused while it is read, before
  any rule fires (ch. 5.7 step 3, ch. 11.3). `fault`: the session aborts on
  a well-formed input (ch. 2.4, ch. 3.8, ch. 5.5, ch. 5.7, ch. 8). `declined`: a form this specification leaves without a meaning
  and every executor refuses by name (ch. 3.12). `§9.5`: a fact on the error
  channel, without an abort (ch. 5.6); the fact keeps rule × subject as its
  identity and carries the codes of its kinds beside it. A channel cell may
  add, after a semicolon, a decision that moves the kind to another
  channel.
- **The report.** An executor carries the code beside the text of its
  message, never inside it, and renders it at the head of its report:
  `EF-011: the evaluationYear context value needs an evaluation date; …`. A
  report of several faults lists their codes in the order of the report:
  `EF-011, EF-016: …`. The text of
  a message is the executor's own and differs between executors; the code
  does not. A kind on the `§9.5` channel has no report: its code travels on
  the channel fact (ch. 5.6).
- **Parameters.** The column names the parameters that identify the object of a
  fault; their values are declared names, labels and lexemes as they stand in
  the document and the case, untranslated, and the writers or values of a list
  (EF-016) are its elements joined by `, `, each writer by its declared name
  alone, a rule's or an allocation's (`corpus/red/jutterswet-verdeling-botsing`).
  An executor emits them as data
  beside its report and never inside the text: on the machine line of its
  runner, `CODE EF-014 {"parameter": "…"}`, one JSON object of strings per code
  (the red run of the Drools backend puts the object after each code of its
  status line, `FAULT EF-016 {"rules": "…", …}: …`; `{}` where the executor
  names none). A
  red case pins them (`expectedFault.params`, an object of strings) and gate 8
  holds every executor that checks the case to them as a subset: it emits at least
  the pinned ones. An executor emits what it has in hand at the place that
  raises the fault, so the column is what the table names, not what each
  executor emits; a parameter that not every executor emits is not pinned by
  any case (an executor that does not name it would fail the pin). Measured
  2026-10-19, of the column of each row: `ref` emits all of it except
  `contextValue` (EF-011), `position` (EF-019) and `key` (EF-024), which no
  executor emits yet. The ASP backend lacks, beside those, `place` (EF-004),
  `value` and `decimals` (EF-006), `place` (EF-006) in all but one of its
  reasons, `from` (EF-009), `creation`, `stratum` and `depth` (EF-015),
  `values` (EF-016) and `filler` (EF-017). The Drools backend emits only `field` and
  `lexeme` (EF-003), `place`, `value` and `decimals` (EF-004 and EF-006, since
  2026-11-02), `count` (EF-008), `from` and `reach` (EF-009), `creation`,
  `stratum` and `depth` (EF-015), `rules`, `values`, `attribute`, `label`
  and `objectType` (EF-016), the parts of EF-032 (since 2026-10-22, as ref
  and the ASP backend), `factType` and `cycle` (EF-033), and `attribute`
  and `label` (EF-036, and EF-037, where it names `label`
  only for the object whose input it is: an attribute read over a path names
  `attribute` alone), and no other.
  Gate 8 holds each executor's emitted keys to this column: a key outside the
  column of its code fails with the code, the executor and the key.
- **Witnesses.** A code enters the table together with its witness: a red
  case (`corpus/red/<dir>`) whose `expectedFault.code` names it, or, for a
  kind on the §9.5 channel, a golden case (`corpus/golden/<dir>`) whose
  `inconsistent` expectation names it. Gate 8 (`scripts/check-red.sh`)
  reads this table: every row parses, every witness exists and pins the
  code of its row, every code a red case pins is a row, every parameter an
  executor emits is a key of the column of its code, and every code a golden
  `inconsistent` expectation pins is a row on the `§9.5` channel. An executor
  that fails on a witness for a reason of its own — a form it does not
  carry, a limit of its representation — stands in that gate's
  reason-carrying divergence list, with the code of its own refusal.
- **Kinds without a code.** A normative fault without a witness keeps its
  text and gets a code when a red case for it lands: today, among others,
  the pass budget of ch. 5.7 step 4 where no divergence by two writers is
  named. A safeguard of one executor (a fire cap) is no fault kind. On the
  `§9.5` channel two kinds have no witness and so no code; each deposits
  its fact without a code (ch. 5.6): a negative radicand (ch. 1.2), which no
  golden can witness while the ASP backend has no square root and gate 6
  replays every golden through it; and an anchor of a variable start that
  is leeg at evaluation time (ch. 3.2), since no executor lays a grid per
  object.

| Code | Kind | Channel | Parameters | Spec home | Witnesses |
|---|---|---|---|---|---|
| EF-001 | a role key of the case input resolves to two roles | case input | `roleKey`, `objectType` | ch. 5.7 step 3; IR-SPEC ch. 2.6 | `corpus/red/ambiguous-case-role-key` |
| EF-002 | a role link of the case input names a label the case does not declare | case input | `role`, `label`, `target` | ch. 5.7 step 3 | `corpus/red/unknown-role-target-label` |
| EF-003 | a date lexeme of the case input names no calendar day | case input | `field`, `lexeme` | ch. 5.7 step 3 | `corpus/red/as-of-date-calendar`, `corpus/red/input-date-calendar` |
| EF-004 | an input value does not fit the declared decimals of its place | case input | `place`, `value`, `decimals` | ch. 2.4; ch. 5.8c; ch. 11.3 | `corpus/red/input-overprecision`, `corpus/red/input-overprecision-param`, `corpus/red/list-element-overprecision`, `corpus/red/struct-field-overprecision`, `corpus/red/jutterswet-cel-overprecisie` |
| EF-005 | a filled dividend is divided by a numeric zero, or a numeric zero is raised to a negative power | §9.5 | `rule`, `label` | ch. 1.4 point 4; ch. 2.2 | `corpus/golden/division-by-numeric-zero`, `corpus/golden/division-abs-by-numeric-zero`, `corpus/golden/filled-count-unguarded-division`, `corpus/golden/at-most-reads-until-settled`, `corpus/golden/closed-runtime-division-by-zero`, `corpus/golden/fold-filter-divides-by-zero`, `corpus/golden/jutterswet-initialisatie-naast-regel`, `corpus/golden/jutterswet-lezer-van-initialisatie`, `corpus/golden/jutterswet-macht-van-nul` |
| EF-006 | a written value does not fit the declared decimals of its target | fault | `place`, `value`, `decimals` | ch. 2.4; ch. 5.8c | `corpus/red/jutterswet-verdeling-past-niet`, `corpus/red/jutterswet-tijdsevenredig-past-niet` |
| EF-007 | a date or datum-tijd step, a date construction or an Easter Sunday leaves the representable range of a date or the representable millisecond range | fault | — | ch. 3.8 | `corpus/red/day-kind-step-outside-range`, `corpus/red/date-time-step-outside-range`, `corpus/red/date-time-with-outside-range`, `corpus/red/date-time-step-wrap`, `corpus/red/date-step-outside-range`, `corpus/red/date-with-outside-date-range` |
| EF-008 | a duration count is out of range | fault | `count` | ch. 3.8 | `corpus/red/date-time-step-outside-range` |
| EF-009 | a day-kind walk passes the reach fence without a day of the kind | fault | `from`, `reach` | ch. 3.8 | `corpus/red/day-kind-unsatisfiable-walk`, `corpus/red/day-kind-scope-outside-table` |
| EF-010 | the bound day is read outside a day-kind evaluation | fault | — | ch. 3.8 | `corpus/red/context-value-de-dag-outside-day-kind`, `corpus/red/bound-day-behind-a-holding-condition` |
| EF-011 | a context value needs an evaluation date the case does not carry | fault | `contextValue` | ch. 8.2 | `corpus/red/evaluation-year-without-evaluation-date`, `corpus/red/duration-reads-van-before-tot`, `corpus/red/context-value-behind-a-holding-condition` |
| EF-012 | a validity-bounded form in a session without an evaluation date | fault | — | ch. 8.1 | `corpus/red/jutterswet-uitsluiting-zonder-rekendatum` |
| EF-013 | the Parameterset timeline of a parameter does not cover the evaluation date | fault | `parameter`, `date` | ch. 8.4 | `corpus/red/vintage-uncovered-date` |
| EF-014 | a parameter the case does not bind is read | fault | `parameter` | ch. 5.7 step 2 | `corpus/red/aanvoergeld-parameter-unbound`, `corpus/red/day-kind-parameter-unbound` |
| EF-015 | the creation budget is exhausted | fault | `creation`, `stratum`, `depth` | ch. 5.5 | `corpus/red/finite-chain-over-budget`, `corpus/red/jutterswet-budgetgrens-over`, `corpus/red/runaway-recursion` |
| EF-016 | two writers of one target disagree | fault | `rules`, `values`, `attribute`, `label`, `objectType` | ch. 5.7 | `corpus/red/jutterswet-dubbele-melding` |
| EF-017 | a to-one role gets a second filler | fault | `role`, `label`, `filler` | ch. 5.5 | `corpus/red/to-one-second-filler` |
| EF-018 | the back-link of a created object is ambiguous | fault | `objectType`, `createdType` | ch. 5.5 | `corpus/red/ambiguous-backlink` |
| EF-019 | an `Unresolved` node is reached, at an expression or a condition position | fault | `position` | ch. 5.7; WF-044 | `corpus/red/wf044-unresolved-guard`, `corpus/red/wf044-unresolved-reached`, `corpus/red/unresolved-behind-a-holding-condition` |
| EF-020 | a temporal key component is supplied as a scalar | fault | `key`, `label` | ch. 5.9 | `corpus/red/uniqueness-temporal-key-scalar`, `corpus/red/ever-unique-temporal-key-scalar`, `corpus/red/ever-unique-temporal-key-bucket-scalar` |
| EF-021 | a filled count over a temporal attribute of a set | declined | `attribute` | ch. 3.4c | `corpus/red/filled-count-temporal-set-source` |
| EF-022 | a nested day-kind test on a date other than the bound day | declined | — | ch. 3.8; ch. 3.12 | `corpus/red/day-kind-reference-other-day` |
| EF-023 | a period condition on a temporal characteristic target leaves its true periods with an open bound | declined | `characteristic` | ch. 3.10; ch. 3.11 | `corpus/red/time-window-timeline-characteristic-open` |
| EF-024 | a uniqueness key with more than one temporal component | declined | `key` | ch. 5.9 | `corpus/red/uniqueness-two-temporal-keys`, `corpus/red/ever-unique-two-temporal-keys` |
| EF-025 | a consistency assertion does not hold for its subject | §9.5 | `rule`, `label` | ch. 5.6 | `corpus/golden/jutterswet-vondstnummer`, `corpus/golden/jutterswet-keurcriteria` |
| EF-026 | a key of a uniqueness occurs more than once, within one source or across the sources of a union | §9.5 | `rule`, `label` | ch. 5.6; ch. 5.9 | `corpus/golden/jutterswet-vondstnummer`, `corpus/golden/jutterswet-uniciteit-drie-bronnen` |
| EF-027 | the defaults groups of a declared-unordered group decide differently, at the root or in a nested exception group | §9.5 | `rule`, `label` | ch. 7.2; ch. 7.9 | `corpus/golden/jutterswet-wachtgeld`, `corpus/golden/synth3-nestkorting` |
| EF-028 | a recipient of a proportional allocation has an empty weight | §9.5 | `rule`, `label` | ch. 4.4 | `corpus/golden/jutterswet-verdeling-dubbelbezet` |
| EF-029 | a recipient of an ordered allocation has an empty tie-break weight | §9.5 | `rule`, `label` | ch. 4.2; ch. 4.4 | `corpus/golden/jutterswet-rangmaximering`, `corpus/golden/jutterswet-verdeling-dubbelbezet` |
| EF-030 | a recipient of an allocation has an empty or negative cap | §9.5 | `rule`, `label` | ch. 4.3; ch. 4.4 | `corpus/golden/jutterswet-berging`, `corpus/golden/jutterswet-rangmaximering` |
| EF-031 | a filled dividend is divided by an empty divisor | §9.5 | `rule`, `label` | ch. 1.2; ch. 1.4 point 4 | `corpus/golden/jutterswet-bergloon`, `corpus/golden/synth3-takhek`, `corpus/golden/synth3-operandvolgorde` |
| EF-032 | the parts of a date construction name no calendar day or no clock time | fault | `year`, `month`, `day`, `hour`, `minute`, `second`, `millisecond` | ch. 3.8 | `corpus/red/date-with-no-calendar-day`, `corpus/red/jutterswet-fout-aan-fixpunt`, `corpus/red/jutterswet-fout-aan-fixpunt-omgekeerd` |
| EF-033 | the links of the switch relation of an instance-ordered cycle form a cycle | case input | `factType`, `cycle` | ch. 6.2; IR-SPEC ch. 5.3 | `corpus/red/instance-order-link-cycle` |
| EF-034 | the labels of a cell of the case input name no cell of its attribute | case input | `label`, `attribute`, `labels` | ch. 5.8c | `corpus/red/jutterswet-cel-onbekend-label` |
| EF-035 | the case input names one cell of an attribute twice | case input | `label`, `attribute`, `labels` | ch. 5.8c | `corpus/red/jutterswet-cel-dubbel` |
| EF-036 | a temporal total meets a segment with an open bound whose value is not 0 | fault | `attribute`, `label` | ch. 3.6 | `corpus/red/jutterswet-totaal-open-segment` |
| EF-037 | a form that reads the full timeline of a time-dependent attribute finds the asOfDate-bucket scalar | fault | `attribute`, `label` | ch. 8.4; ch. 3.3; ch. 3.4; ch. 3.5; ch. 3.6; ch. 3.7; ch. 3.9; ch. 3.10 | `corpus/red/jutterswet-emmerscalair-per-periode`, `corpus/red/jutterswet-emmerscalair-knip`, `corpus/red/jutterswet-emmerscalair-gehele-jaar`, `corpus/red/jutterswet-emmerscalair-gevolg` |
| EF-038 | a recipient of a proportional allocation has a negative weight | §9.5 | `rule`, `label` | ch. 4.2; ch. 4.4 | `corpus/golden/jutterswet-lege-pot-en-negatief-gewicht` |

The four declined kinds are static properties of the document. Each ends
as a WF rule (its witness moves to `corpus/negative`) or as a specified
form (its witness moves to the golden corpus), and its code then retires.

Worked example (`corpus/red/clamp-reads-bounds-in-order`, replayed by gate
8): the case pins `"code": "EF-011"` and `"params": {}` (the parameter
`contextValue` that the row names is emitted by no executor yet, so the case
pins none). The reference interpreter reports the code beside its message
where it evaluates the minimum of the clamp, "the evaluationYear context
value needs an evaluation date; the case carries asOfDate null"; the ASP
backend reports it beside the same sentence when it solves, from the witness
of that minimum, which fires where the evaluation reaches it; the Drools
backend's red run reports `FAULT EF-011 {}:
ComputeFault: error in the rule "begrenswaarde van een jutter": the
evaluationYear context value needs an evaluation date; …`. Two moments,
three texts, one code. (Until 2026-10-19 this example was
`division-by-numeric-zero`, whose kind EF-005 moved to the §9.5 channel
then, ch. 1.4 point 4.)

### 5.8 The navigated read

An attribute reference whose endpoint is off-subject (IR-SPEC ch. 4.1a,
WF-053) reads through the role links, one to-one hop per step, each hop
named by its role binding (`path`; since L2 the only
spelling of a hop, B11). The rules, mirrored from the pinned runtime's
null-safe navigation readers:

1. **Absent is leeg, at every hop.** A subject without a link over the
   hop's role reads the whole operand as **leeg** — never a fault, never
   a skipped rule. A multi-hop walk short-circuits: any intervening
   absent link makes the operand leeg (§5.5.5). The leeg value then
   flows through the ordinary ch. 1.2 discipline (a leeg operand makes
   an ordering comparison onwaar; leeg/gevuld see it as leeg; a bare
   assignment of leeg writes nothing).
2. **To-many never reaches a scalar slot.** WF-053 rejects the shape
   statically; the executors additionally refuse it at the hop, so a
   document that evades the verifier still cannot silently pick a first
   filler. A stored second filler on a to-one role is the ch. 5.5
   write-boundary fault and cannot arise from a well-formed case.
3. **The navigated characteristic check is asymmetric — deliberately.**
   On an absent link the check is **onwaar for the positive AND the
   negated form**: "zijn post is geen risicopost" does NOT hold for a
   wachter without a post. This mirrors the pinned runtime exactly, and
   differs from comparison and leeg/gevuld, which treat the absent
   relation as a leegwaarde. In the ASP reading the hop is a positive
   body literal and only the characteristic sits under negation-as-
   failure — the same asymmetry, for free.
4. **A read without a path is the subject's own read —
   unconditionally.** An attribute reference that carries no
   `path` reads the subject's OWN attribute, and its
   `subjectType` is the subject type itself — equality, not ⊑ (IR-SPEC
   ch. 4.1a, WF-053): the verifier refuses any other type there, a
   supertype included, and so do `ref` and the ASP backend, by name. No
   link is consulted, so the read succeeds on an instance with no
   relation instances at all: there is no leeg-on-absent-link here,
   because there is no hop to be absent. A written form that names the
   subject by a role the subject itself fills ("het strandtegoed van de
   aanmelder", said of the aanmelder) is such an own read on the wire; a
   reciprocal partner ("zijn strandmaat") is a real one-hop read with a
   binding and rule 1's leeg-on-absent behaviour. *History until L2:*
   the self-reference was a single bindingless written step that the
   readers resolved to zero hops (the far-role resolution first, then a
   self-denotation test on the written name); the written step and the
   test left the wire with B11, and the frontend writes the own read. The
   navigated characteristic check was outside that rule, and a
   self-denoting name there was refused by both executors.
5. **A read with the form anchor starts at the form subject.** In a
   filter or a quantifier's test, where the subject is the member, a read
   that carries `anchor: "form"` (`IR-SPEC.md` ch. 4.1a) is read on the
   form subject: rules 1–4 apply with the form subject in place of the
   member. Without a `path` it is the form subject's own read, unconditional
   as rule 4 says; with one, its hops start at the form subject and rule 1
   reads it leeg on an absent link. The navigated characteristic check with
   the anchor makes its hop from the form subject and keeps rule 3.

**Identity.** Two objects are the same object iff they are one instance:
the instance a label of the case input names (ch. 5.7a), or the instance a
creation made (ch. 5.5). An object of one type is the object of another type
only where it is an instance of both (`IR-SPEC.md` ch. 2.2: an instance of a
subtype is an instance of its supertypes). The identity check
(`IdentityCheck`, `IR-SPEC.md` ch. 4.5) compares the member of its filter or
test with the form subject by this identity and by nothing else; it reads no
attribute and no link, so it is never leeg and never faults (ch. 3.4a, *The
form anchor*).

Worked example (the `jutterswet-kustwacht` anchor, case 2, verbatim in
gates 5–6): W1 mans post P1 in kustvak V1 (verraderlijk, gevarenklasse
5); W4 mans post P3, which lies in no kustvak; W3 mans nothing. Then
W1.uitkijkbonus = 12 (single hop), W1.risicotoeslag = 5 (two hops);
W4.uitkijkbonus = 9 but W4.risicotoeslag is **leeg** (hop two absent —
the short-circuit); W3 reads leeg on both (hop one absent). The negated
check separates W3 from W4: P3 is not a risicopost, so W4 (linked) IS a
rustwacht, while W3 (link absent) is NOT — negation does not resurrect
an absent relation.

Worked example (rule 4; the `jutterswet-aanmelderkrediet` golden,
case 1, verbatim in gates 5–6 and in `ref/test/test_ref.ml`): Jutters
fill the role aanmelder in feittype vondstaanmelding, and the role
strandmaat in the reciprocal feittype strandmaatschap. J1
(strandtegoed 10) is strandmaat of J2 (25); J3 (7) carries no links at
all. "het strandtegoed van de aanmelder plus 2" is on the wire the
subject's own read, a reference without a path: meldkrediet
J1 = 12, J2 = 27, **J3 = 9** — J3 has no links and still reads its own
attribute (the unconditional own read). "het strandtegoed van zijn
strandmaat" is the one hop its binding names over the reciprocal role
(`strandmaatschap`/`strandmaat`): maatdeling J1 = 25 and J2 = 10, never a
self-read; J3's maatdeling is **leeg** (rule 1, absent link) — the
contrast that separates the real hop from the empty walk. A binding over
the to-many role `vondst` of vondstaanmelding, read from a Jutter, faults
in this scalar slot (rule 2; WV-5.8-4-vreemde-rol) and is never read as an
own read. *History until L2:* that last form was the written "het
strandtegoed van de vondst", a same-typed reference naming a role the
subject does not fill, refused by WF-053's self-denotation narrowing (the
negative `wf053-zelfverwijzing-vreemde-rol`, removed at L2 with the
written step).

### 5.8a The role check

A **role check** ("hij is een wachtjutter", "hij heeft een wachtjutter",
§8.1.7) is the IR's `RoleMembership {factType, role, counterpartType?,
negated}` (`IR-SPEC.md` ch. 4.5). It holds iff a link of the fact type
`factType` exists in which the **subject** fills the role `role` and, where
`counterpartType` is written, the filler of the fact type's OTHER role in
that link is an instance of `counterpartType`. With `negated` it holds iff
no such link exists. A reciprocal fact type has one role, which is its own
other end. The rules:

1. **Existence, never a count and never a pick.** One link and ten links
   give the same verdict, and no reader selects among them. The
   multiplicity of either role is no condition of the reading: a check
   whose other role is to-many answers exactly as one whose other role is
   to-one. An executor whose fact model cannot store a direction refuses
   the form with its reason (ch. 5.5); it never reads a spuriously empty
   side and calls that an answer.
2. **The counterpart is a class test, and it is extensional.** A linked
   instance counts only where its own type is ⊑ `counterpartType`
   (ch. 2.2), so an instance of a subtype counts and an instance of the
   supertype alone does not. Where the field is absent the counterpart is
   the other role's declared filler, which every linked instance already
   is, and the test is empty. The field is written only where it narrows,
   to a strict subtype of that filler; written equal to the filler it is
   well-formed and changes nothing. A reader that resolves the pair and
   skips the class test answers a **different sentence** — the role's
   whole population instead of the named class — and the two coincide
   exactly where the class IS the declared filler, which is why a witness
   needs a strict subtype to tell them apart.
3. **It reads the RELATION, not a direction.** By ch. 5.7a a supplied
   link establishes the relation itself and both roles read it, so a case
   may spell a link from either end and the verdict is the same. The
   reference interpreter reads the subject's links over the other role
   (the leg the loader materialises from either spelling), and the ASP
   backend joins `role/3` from the subject over that role; a
   reference-field executor asks the mirror-image question — does some
   instance of the class hold a stored reference back at this subject —
   because that is the side its fact model stores. It is the same
   relation and the same class test.
4. **The negation is the plain complement.** Nothing sits behind a hop
   here, so an absent link makes the negated form **waar**: "hij is geen
   wachtjutter" holds exactly for a Jutter in no link of the fact type,
   and "hij heeft geen strandjutter" for a post with no linked instance of
   that class. That differs from the navigated characteristic check of
   ch. 5.8 rule 3, where the negation sits behind a hop and an absent
   link fails the positive and the negated form alike. Both forms read
   the same narrowed set, so they cannot disagree about which instances
   count. In the ASP reading the positive form is a `role/3` join over the
   other role (an absent link never binds, so the gated rule does not
   fire), and the negation a per-subject helper deriving the link's
   presence, read under negation-as-failure with the subject kept bound by
   the helper's `isa` base (`ASP-TRANSLATION.md` S7).
5. **What is refused.** The node's declarations are crossed before the
   check is evaluated (`IR-SPEC.md` ch. 4.6 (o), the role arm of WF-034):
   `factType` is a declared fact type; `role` is one of its roles by
   DECLARED NAME — a plural is presentation and names no role here; the
   subject fills `role`, its type ⊑ the role's declared filler — a role of
   the right fact type on the wrong side names a link the subject can
   never be in, so the positive check could never hold and its negation
   always would; and a written `counterpartType` is a declared object type
   ⊑ the other role's declared filler. The verifier refuses a document
   that breaks any of these. The reference interpreter and the ASP backend
   refuse the same shapes by name when they evaluate the check, so such a
   document, evaluated without the verifier, gives a refusal and never a
   silent false.

*History until L2.* v0 carried the sentence as two nodes. The **near-side**
`Rollidmaatschap` ("hij is een wachtjutter") was lowered to a **null-test**
on the subject's reference to the other end: the connecting role from the
subject's type to the other-end type the node carried (`anderTeFiller`),
derived from the type pair as for the allocation recipients (ch. 4.1). A
present reference filled the role, an absent one did not, and the "is
geen" negation was the inverse null-test. Where that connecting reference
was **to-many**, v0 could not lower it to a scalar null-test and made a
marker, and both executors refused the check. The **far-side**
`VerreRollidmaatschap` ("hij heeft een wachtjutter") was already the
existence question of rules 1–4, with the class test on its `filler` and
the subject's role in its `backref`, and it was admitted at top level
only. L2 (B9 of `spec/proposals/V1-WIREMIGRATIE.md`; `spec/CONSTRUCTS.md`
row 5) merged the two into `RoleMembership`, which names the relation and
the subject's role instead of deriving them and carries the far side's
narrowing as `counterpartType`. Two meanings changed with it: the to-many
near-side check answers with existence where it faulted
(`corpus/golden/near-side-role-check-to-many`, a red case until L2), and the
top-level restriction is gone.

Worked example — the near side (the `jutterswet-wachtindeling` golden,
verbatim in gates 5–6 and as WV-5.8a in `ref/test/test_ref.ml`): feittype
wachtindeling relates the to-many role wachtjutter (Jutter) to the to-one
role toegewezen wachtpost (Wachtpost). "Een Jutter is wachtdoend indien hij
een wachtjutter is" and "Een Jutter is vrijgesteld indien hij geen
wachtjutter is" are `RoleMembership {factType: wachtindeling, role:
wachtjutter}`, positive and negated. With J1 assigned to post W1 and J2
assigned to no post: a link of wachtindeling holds J1 in wachtjutter, so J1
**is wachtdoend** and **is not vrijgesteld**; no link holds J2, so J2 **is
not wachtdoend** and **is vrijgesteld**. The reference interpreter and the
ASP backend agree on all four (link × {positive, negation}). The `ref/`
suite also declares the other role to-many: J1's check still holds and
J2's does not (WV-5.8a-naar-veel-*).

Worked example — a to-many other role (the `near-side-role-check-to-many`
golden, verbatim in gates 5, 6 and 12): "Een Wachtpost is bemand indien hij
een toegewezen wachtpost is" makes the Wachtpost the subject and names the
role the post fills, `RoleMembership {factType: wachtindeling, role:
toegewezen wachtpost}`; the other role, wachtjutter, is to-many. J1 and J2
are both assigned post W1, and W2 is assigned to no one. Two links hold W1
in toegewezen wachtpost, so W1 **is bemand**; no link holds W2, so W2 **is
not bemand**. Two links read exactly as one does (rule 1). Until L2 this
was the shape v0 could not lower — the post's reference to the other end
is the to-many wachtjutter — and it is the same node as the far-side check
"hij heeft een wachtjutter" below: the two sentences name one link.

Worked example — the far side and the class test (the
`jutterswet-postbezetting` golden, verbatim in gates 5–6 and as WV-5.8a-ver
in `ref/test/test_ref.ml`): the same feittype wachtindeling, with the
Wachtpost as subject, and a **Strandjutter** declared under the Jutter.
Four rules, two checks each with its negation, all naming `role`
toegewezen wachtpost, the role the post fills: "Een Wachtpost is bemand
indien hij een wachtjutter heeft" / "is onbemand indien hij geen
wachtjutter heeft" carry no `counterpartType` (the other role's declared
filler, Jutter), and "is strandbemand indien hij een strandjutter heeft" /
"is strandvrij indien hij geen strandjutter heeft" carry `counterpartType:
Strandjutter`.

| case | input | W1 | W2 |
|---|---|---|---|
| 1 | J1, a plain Jutter, assigned to W1; W2 empty | bemand, **not** strandbemand (strandvrij) | onbemand, strandvrij |
| 2 | W1 holds wachtjutters J1 (Jutter) and J2 (Strandjutter); J3 assigned to no post; W2 empty | bemand, strandbemand | onbemand, strandvrij |
| 3 | J1 (Jutter) assigned to W1, S1 (Strandjutter) assigned to W2 | bemand, strandvrij | bemand, **strandbemand** |

Each row carries one of rules 1–4. Case 1's W1 verdict depends on a link no
case line names for W1 at all — the case spells only J1's leg (rule 3) —
and its *strandvrij* is rule 2: the relation is there and the class is
not. Case 2's two fillers give exactly one filler's verdict (rule 1), J3
in no relation leaves W2 alone, and the Strandjutter satisfies the Jutter
filler as well as its own (rule 2, extensionally). Case 3 is where the two
readings come apart inside one world: both posts are **bemand** and only
W2 is **strandbemand**, so a reader that skipped the class test answers
case 3 wrong on W1 while answering `bemand` right everywhere. Rule 5 in
the `ref/` suite: a check naming the role of the other side (wachtjutter,
said of a Wachtpost) and a counterpart outside the other role's filler
(Wachtpost) both fault (WV-5.8a-ver-verkeerde-zijde,
WV-5.8a-ver-vreemde-tegenpartij).

### 5.8b The characteristic check's owner: the host

A characteristic check may carry an **owner** (the IR's `host`). It is
**authoritative** (maintainer decision 2026-09-10; `IR-SPEC.md` ch. 4.6 (q1),
WF-100): where the field is present it, and not the enclosing form's subject,
decides what the check resolves against. And it is the **host** (maintainer
decision 2026-09-19; ch. 4.6 (q2), WF-109): it names the object type the check
lives on, which is the object read. The rules:

1. **The owner is the declaration key.** Every declaration a reader consults
   for the characteristic is the one the NAMED type carries: whether the type
   declares the characteristic at all (WF-100 crosses it before execution),
   whether it is time-dependent — which is what decides between the scalar
   read of this chapter and the timeline read of ch. 3.3 — and the node key
   `char:<owner>:<name>` of the dependency edge the check contributes to
   ch. 6.1's graph. Where the field is **absent** the subject's own type is
   that key, exactly as before. An absent owner and an owner naming the
   subject's own type are the **same reading**; a difference in spelling is
   not a difference (the name resolves case-insensitively, `IR-SPEC.md`
   ch. 4.6 (q)). The key is the owner's on a **non-navigated** check only. On a
   NAVIGATED one the characteristic lives on the type the hop REACHES and is
   crossed there (WF-053), and the owner names neither that type nor a second
   declaration to consult; it stays what rule 2 makes of it, a statement about
   the host pattern, and nothing about the reached type's declarations moves.
2. **The owner is the OBJECT read, and the position decides which objects
   there are to read.** The field names the type whose PATTERN the constraint
   is attached to — the v0 producer emits it on exactly one shape, the guard of
   a fact creation that names a characteristic of the DOEL, so that the
   constraint lands on the doel pattern and never on the anker, even when the
   anker declares a characteristic of the same name. Every condition position
   in this language binds exactly ONE object, the enclosing form's subject,
   with a single exception: the **guard of a fact creation** (ch. 5.5) binds
   TWO — the anker it iterates and the doel its join chain reaches. The owner
   designates one of the bound objects, and that object is read.

   **Designation is extensional, and it asks two questions.** A binding whose
   declared type is `T` SATISFIES owner `O` iff `T ⊑ O` — every object it can
   hold is an `O`, so a supertype owner is satisfied and an inherited
   characteristic is read on the object that hosts it. It OVERLAPS `O` iff some
   object could be both, which the declared type graph answers. The owner
   designates a binding when exactly one binding satisfies it and no second
   binding overlaps it. Where the owner is absent, or names a type the subject
   satisfies, the subject is read — every reading that predates the rule. Where
   it names a type the DOEL satisfies, the doel is read, and the guard filters
   the doelen instead of gating the anker (ch. 5.5).

   **An owner that designates nothing, or two things, is not a reading.**
   WF-109 refuses the document before any executor sees it, and an executor
   that meets one anyway **faults** rather than falling back to the subject —
   that is the reading the 2026-09-10 decision rejected, and it is the reading
   that answered before it, when both OCaml executors returned the subject's
   own value under the other type's name while the third carried no rule at
   all. The two faults are distinct and both are named. An owner no bound
   object can be is refused where the check is read: "the host names the
   object the check is about, and this reader has no other object to read".
   An owner that reaches BOTH objects of a fact-creation guard is refused where
   the guard's host is decided, naming the two it reached — the arm that was
   missing until 2026-09-19, when the helper answered "not the doel" and let
   the anker read, which is the silent form of the rejected reading. The
   executors still fault at different moments, and that is inherited rather
   than new: the reference interpreter is lazy and faults when the condition is
   evaluated, the ASP backend is eager and faults at translation, exactly as
   they split on WF-055.
3. **All four executors read the same object.** This is the point of rule 2 and
   the reason it replaced a refusal on 2026-09-19. A bean-model executor reads
   the owner's instances by construction — it attaches the constraint to that
   type's pattern — and it did so a rule earlier than the others; the reference
   interpreter and the ASP backend now reach the same object by the route their
   own shapes allow, the interpreter by evaluating the guard once per doel
   after the chain is walked, the ASP backend by placing the literal on the
   doel variable the chain binds. A form that is verifier-clean under WF-100
   and WF-109 is therefore carried by all four, with the same answer. The
   scope fences that keep this true are in `IR-SPEC.md` ch. 4.6 (q2): the doel
   is a bound object only where the check IS the whole guard, does not navigate
   and carries no whole-period qualifier, because each of those three is a shape
   one of the four cannot carry — and the verifier refuses what the four cannot
   agree on. The third fence is the youngest and the most clearly temporary:
   under `gedurende het gehele <periode>` both bean-model branches run the
   ch. 3.9 fold arm BEFORE they look at the owner, so the field decides nothing
   there; it falls the day the §8.2 family norms the position. The EXTENSIONAL
   reading had to be made true of the bean model as well — both its branches
   compared owner and pattern by NAME until 2026-09-19, which refused a legal
   supertype owner in both while the other three answered.
4. **The narrowing host** (card `subtype-characteristic-in-fold-filter`,
   2026-10-27; `IR-SPEC.md` ch. 4.6 (q2)). At a position that binds ONE object
   (rule 2) — a rule's subject, the subject of a consistency, an allocation, a
   creation and a defaults group, the member of a fold's filter, the member of
   a quantifier fold's filter and test — a check that neither navigates nor
   carries a whole-period qualifier may name as its owner a type `S` strictly
   below the type `T` the position binds. No binding satisfies such an owner,
   and it is a reading all the same, the one exception to rule 2's "designates
   nothing": the check holds iff the object is an instance of `S` (ch. 2.2,
   extensionally: an instance of a subtype of `S` is one) and has the
   characteristic `K` as `S` declares it, and `negated` holds exactly where the
   positive form does not. An object outside `S` has no `K`, and that is a fact
   about the object and not an absent link: the positive form is **onwaar**
   there and the negated form **waar**, the plain complement of the role check
   (ch. 5.8a rule 4) and not the navigated check's asymmetry (ch. 5.8 rule 3).
   Rule 1 holds unchanged, with `S` as the key: WF-100 requires `S` to declare
   `K`, its time-dependence decides between this chapter's scalar read and
   ch. 3.3's refusal of a timeline in a scalar condition, and the dependency
   edge is `char:S:K`. Where no type the position binds lies above `S` — at the
   guard of a fact creation, which binds two objects, on a navigated check,
   whose `K` lives on the reached object, and under a whole-period qualifier,
   which the bean model folds before it reads the owner — the owner is no
   reading and WF-109 refuses it; an executor that meets one anyway faults, as
   rule 2 requires. The population a fold ranges over is not narrowed by the
   check: an object outside `S` is a member of the fold and fails or passes the
   filter like any other, which is the difference between this rule and an
   `elementType` below the role's filler (ch. 3.4a), which leaves that object
   out of the fold.

Worked example A, the single-object position (`ref/test/test_ref.ml` ch. 5.8b
verbatim; its ASP mirror in `backends/clingo/test/test_clingo.ml`). Two object
types, `Vondst` and `Jutter`, each declaring a characteristic named `gekeurd`,
neither time-dependent. The world holds V1, a Vondst carrying `gekeurd`, and
J1, a Jutter that does not. Over the subject V1:

| check | owner | outcome |
|---|---|---|
| `de Vondst is gekeurd` | absent | **waar** — V1 carries it |
| `de Vondst is gekeurd` | `Vondst` | **waar** — the same reading |
| `de Vondst is gekeurd` | `vondst` | **waar** — resolution is case-insensitive |
| `de Vondst is gekeurd` | `Jutter` | **fault** — never J1's value, and never V1's |

and over the subject J1 the mirror holds: owner absent gives **onwaar** (J1
does not carry it), owner `Vondst` faults. A navigated check with the same
foreign owner faults too, and faults BEFORE the hop is resolved (rule 1's
closing sentence), which is why both suites assert the sentence and not merely
the fault. The last row is a document WF-109 refuses; the fault is the
executors' fail-fast on a non-conforming input, and it is asserted in the two
suites rather than in a corpus zone because a golden must replay and a red case
must fault in every executor that carries its form — which the bean-model
executor does not do on a plain rule, where it drops the rule as a transpiler
skip. The golden `jutterswet-keurhouder` carries row 2, the owner naming the
subject's own type.

Worked example B, the two-object position (`ref/test/test_ref.ml` ch. 5.8b
verbatim; the goldens `jutterswet-seinhijsing` and `jutterswet-mastsein` replay
it in all four executors). A `Kustvak` anker, a `Sein` doel, and one
intermediate `Post`; `gehesen` is declared on all three types. The creation
"een vaksein van een Kustvak is een sein van de post van het kustvak indien het
sein gehesen is" carries the guard `gehesen` with owner `Sein`, and a fold
counts the created relation. The world holds V1 (a Kustvak that IS gehesen)
with one post P1 (NOT gehesen) carrying S1 (gehesen) and S2 (not):

| reading | which object the guard is asked of | `vakseinaantal` of V1 |
|---|---|---|
| anker | V1, which carries `gehesen` | 2 — every sein reached |
| intermediate step | P1, which does not | 0 — the creation never fires |
| **doel (normative)** | S1 and S2, each in turn | **1** — S1 only |
| doel, owner a SUPERTYPE of `Sein` | the same objects | **1** — the test is `T ⊑ O` |

The three readings give three different numbers, so the answer identifies the
reading rather than merely agreeing with it. Over TWO intermediate steps
(`jutterswet-mastsein`: V1 gehesen → P1 not → M1 gehesen → S1, S2, S3 with S1
and S3 gehesen) the guard still reaches past both, and the four numbers are
anker 3, first step 0, second step 3, **doel 2**. Negation rides along
unchanged: "indien het sein niet gehesen is" filters the complement. A
SUPERTYPE owner reads the same doel — the golden `jutterswet-bakensein` puts
`gehesen` on a `Baken` the doel inherits from, with the anker and the step
each declaring a `gehesen` of their own, and all four executors answer 1.

Worked example C, the narrowing host (rule 4; the `jutterswet-ploegblokkade`
golden, case 1, verbatim in gates 5, 6 and 12 and as WV-5.8b-C in
`ref/test/test_ref.ml` and `backends/clingo/test/test_clingo.ml`). `Wrakrecht`
⊏ `Jutrecht` ⊏ `Strandrecht`; only `Wrakrecht` declares `vaartijd`, which a
rule derives where the zeeweken of its logboek reach the drempel (5). Ploeg P1
holds, over the role `rechten` filled by `Strandrecht`, the Jutrecht J1 (loon 5,
grens 10), the Wrakrechten W1 (loon 20, zeeweken 6) and W2 (loon 30, zeeweken
2, grens 25), and the plain Strandrecht S1 (loon 40). "Het hoogste vaarloon"
of a Jutrecht is the largest loon of the other rechten of its ploeg that have
the vaartijd, "het hoogste rustloon" the same over those that do not; the
filters carry the check with owner `Wrakrecht` on the member, a `Strandrecht`.

| member | instance of `Wrakrecht` | `vaartijd` | "hij heeft de vaartijd" | "hij heeft geen vaartijd" |
|---|---|---|---|---|
| J1 | no | — | onwaar | waar |
| W1 | yes | yes | waar | onwaar |
| W2 | yes | no | onwaar | waar |
| S1 | no | — | onwaar | waar |

So hoogste vaarloon of J1 is **20** (W1 only: not 30, W2 lacks the vaartijd,
and not 40, S1 is no Wrakrecht), and hoogste rustloon of J1 is **40** (W2 and
S1) — a reading in which an object outside the owner holds in neither
polarity, the navigated check's, gives **30**, and one that leaves it out of
the fold gives 30 as well. Of W1 the vaarloon is **leeg** (no other member
holds) and the rustloon **40**; of W2 the vaarloon is **20** and the rustloon
**40**. As a rule's condition on the supertype, "Een Strandrecht is zeewaardig
indien hij de vaartijd heeft" holds for W1 alone, and its negated twin
"landvast" for J1, W2 and S1. In the test of a quantifier fold, "Een
Jutrecht is ploegvarend indien ten minste één van de rechten van de ploeg van
het Jutrecht de vaartijd heeft" holds for J1, W1 and W2, each of whose ranges
holds W1; with only S1 beside it (case 2), J1 is not ploegvarend.

### 5.8c The dimensioned attribute: its cells

A dimensioned attribute (IR-SPEC ch. 2.7) has no value of its own. On each
object it holds a partial map from **label tuples** to values. A tuple names
one label of each dimension of the attribute, label i a label of dimension i
of the attribute's `dimensions`; a tuple the map does not hold is an **empty
cell**. Labels compare as names do (IR-SPEC ch. 2.1). A cell behaves in every
respect as a scalar attribute whose member is the pair (attribute, tuple),
and every clause of this document about an attribute holds for a cell in the
same way. Where RegelSpraak v2.3.0 is silent — writing a cell, a fold over
more than one dimension, a cell of another type than a number — this
chapter follows ALEF (IR-SPEC ch. 2.7, the norm source).

1. **Reading a cell.** A `DimensionCellRead` evaluates to the value of the
   cell its `labels` name, on the object its `attribute` reaches: the
   subject itself, or the object a path of roles reaches, by the rules of
   ch. 5.8 unchanged — an absent link reads leeg (rule 1), and a to-many
   hop never reaches a cell (rule 2). The reference under the cell names
   the attribute and the object and is not evaluated as a value of its
   own. An empty cell reads leeg (ch. 1.2).
2. **Writing a cell.** A rule version whose `target` is a cell writes that
   cell of its subject and no other cell. The written value is collapsed to
   the attribute's declared representation at the write boundary (ch. 2.4);
   a value that does not fit is EF-006, its `place` the cell,
   `J1.vondstgewicht[najaar]`. A write that changes nothing is no change
   (ch. 5.1). An initialisation writes its cell only where that cell is
   empty (ch. 5.4): another cell of the attribute that is filled does not
   keep it from its turn. Two writers of one cell whose values differ fault
   the session (EF-016, ch. 5.7); two writers of two cells do not meet. An
   object creation's member with `labels` writes that cell of the object it
   makes (ch. 5.5). A cell is a target of its own in the standing norm
   (ch. 7.8) and in the domain of the v0 desugaring (ch. 7.7): a computing
   rule on a cell displaces the input of that cell when its value is gevuld
   and leaves it standing when it is leeg, and a
   computing rule beside an initialisation on one cell decides like the
   default above the initialisation default. On the wire the two forms stay
   as written, since a defaults group names its attribute whole (WF-125); an
   executor that lowers them into a defaults chain does so on the cell.
3. **Folding cells.** An aggregation with `dimensionSelections` — a `sum`,
   a `minimum`, a `maximum`, a `first`, a `last` or a `filledCount`
   (WF-037) — folds the cells of the product of its selections on the
   object its argument reaches. The tuples are formed with the first
   selection outermost, each selection in its own label order, and the
   cells are read in that order (ch. 5.11). Empty cells are skipped
   (ch. 1.2e); where every selected cell is empty the fold is empty, and a
   sum then takes its `emptyFallback`, which is evaluated after the cells
   (ch. 1.2, the table of the aggregations). A dimension the fold does not
   range over is selected with one label, which stands in every tuple. The
   sum, the minimum and the maximum read the values; a `first` and a `last`
   pick by position, the value of the first and of the last filled cell in
   that order, over cells of any type, so a set selection written
   `{najaar, voorjaar}` reads `najaar` first; a `filledCount` is the number
   of filled cells, a whole number, 0 where none is filled. This is ALEF's
   eerste, laatste and tel (IR-SPEC ch. 2.7, the norm source). ALEF lets a
   first and a last range over one dimension with more than one label only;
   the dialect admits more, read in the order of the product as every
   dimension fold reads them (the worked example below). No cell read
   faults, so no fault witnesses the order of the cells; the first and the
   last do.
4. **The dependency graph** has a node per cell (IR-SPEC ch. 5.1): a cell
   read reads its cell, a cell target and a member with `labels` write
   theirs, and a dimension fold reads every cell of the product of its
   selections. A rule that computes one cell from another cell of the same
   attribute is therefore no cycle.

**The case.** An input of a dimensioned attribute is its cells,
`{"cells": [{"labels": [<label>, …], "value": <value>}, …]}`, the labels
positional as on the wire; a cell the list does not name is empty. A cell's
value has the attribute's datatype and is held to its declared decimals
(EF-004, the `place` the cell, `j1.vondstgewicht[voorjaar]`). Two
expectations name a cell, `cellValue` (the cell holds the value) and
`cellEmpty` (the cell is empty), with the labels spelled as in the input. The
case reader refuses, while it reads the case (ch. 5.7 step 3):

- labels that name no cell of the attribute — a number of labels other than
  the number of its dimensions, or a label at position i that is no label of
  dimension i — with EF-034;
- a cell the input names twice, with EF-035;
- a scalar or a timeline input on a dimensioned attribute, a `cells` input
  on an attribute without dimensions, and an `attributeValue`,
  `attributeEmpty` or `attributeTimeline` expectation on a dimensioned
  attribute: a case error without a code, since the case does not fit the
  document's declarations, as a timeline on an attribute without one does
  not. A scalar on a dimensioned attribute would otherwise load as a value
  the document cannot read; measured 2026-11-02 before the reader held this,
  `ref` and the ASP backend each returned the scalar 3.00 to an
  `attributeValue` expectation on the attribute, `PASS`.

**Worked examples** (absolute). A dimension `seizoen` with the labels
`voorjaar`, `zomer`, `najaar` and, where named, a dimension `strand` with
`noord`, `zuid`; on the `Jutter` a `vondstgewicht` in kg with 2 decimals.

- **Reading** (`jutterswet-seizoenvondst`, the dimension `seizoen` with
  `voorjaar` and `najaar` there): "Het voorjaarsgewicht van een Jutter moet
  berekend worden als zijn vondstgewicht in voorjaar". J1 with the cells
  `voorjaar` 12.50 and `najaar` 4.00 gives **voorjaarsgewicht 12.50**; J2
  with the cell `najaar` 4.00 alone gives **voorjaarsgewicht leeg**.
- **Reading over a path**: a `Ploeg` whose to-one role `leider` reaches a
  `Jutter`; its `leidersvoorjaar` is the cell `voorjaar` of the leader's
  `vondstgewicht`, its `leidersjaargewicht` the sum of the leader's
  `vondstgewicht` over all seasons. P1 whose leader J1 has the cells
  `voorjaar` 12.50 and `najaar` 4.00 gives **leidersvoorjaar 12.50,
  leidersjaargewicht 16.50**; P2 without a leader gives **leeg** for both
  (rule 1 of ch. 5.8: the absent link reads every cell leeg).
- **A cell of each member** (ALEF's fold of one cell over a role): the
  source of a value fold or of a filled count over a set may be a cell of
  each member, a `DimensionCellRead` of the element type (IR-SPEC ch. 4.3);
  each member gives the value of its cell, and a member whose cell is empty
  gives none (ch. 1.2e), as a member whose attribute is empty. A `Ploeg`
  whose to-many role `lid` reaches J1 (`voorjaar` 2.50), J2 without a cell
  and J3 (`voorjaar` 3.00, `najaar` 1.00) gives the sum of the cells
  `voorjaar` **5.50**, the maximum of the cells `najaar` **1.00** and the
  filled count of the cells `voorjaar` **2**; a `Ploeg` without members
  gives **leeg**, **leeg** and **0** (`WV-5.8c-cel-van-elk-lid`). In
  `jutterswet-ploegseizoen` the leader's cells, the fold over them and the
  folds of one cell of each member are written in RegelSpraak.
- **Folding over one dimension** (`jutterswet-seizoenvouw`): `jaargewicht`
  is the sum over all seasons, `groeigewicht` the sum from `voorjaar` up to
  and including `zomer`, `piekgewicht` the maximum over `voorjaar`, `zomer`
  and `najaar`, `dalgewicht` the minimum from `zomer` up to and including
  `najaar`. J1 with `voorjaar` 3.00 and `najaar` 2.00, `zomer` empty, gives
  **5.00, 3.00, 3.00 and 2.00**; J2 without a cell gives **leeg** for all
  four; J3 with `zomer` 1.25 alone gives **1.25** for all four.
- **Folding over two dimensions**: `vondstgewicht` dimensioned by `seizoen`
  and `strand`, with the cells (`voorjaar`, `noord`) 2.00, (`najaar`,
  `noord`) 1.00 and (`voorjaar`, `zuid`) 5.00. The cell read `voorjaar`,
  `zuid` gives **5.00**. A sum with the selections `seizoen`: `voorjaar`,
  `zomer`, `najaar` and `strand`: `noord` reads (`voorjaar`, `noord`),
  (`zomer`, `noord`), (`najaar`, `noord`) and gives **3.00**; with
  `strand`: `zuid` it gives **5.00**. A sum with the selections `seizoen`:
  all three and `strand`: `noord`, `zuid` reads (`voorjaar`, `noord`),
  (`voorjaar`, `zuid`), (`zomer`, `noord`), (`zomer`, `zuid`), (`najaar`,
  `noord`), (`najaar`, `zuid`) and gives **8.00**; the minimum over the same
  selections gives **1.00**. Two cells of one value both count: with the
  cells (`voorjaar`, `noord`) 2.00 and (`najaar`, `noord`) 2.00 the sum over
  `strand`: `noord` gives **4.00**, the minimum over all cells **2.00**, and
  the cell read `voorjaar`, `zuid` **leeg**.
- **First, last and the filled count** (ALEF's two tests of the fold): a
  dimension `kustvak` with the labels `texel`, `vlieland`, `terschelling`,
  `ameland`, `schiermonnikoog`, `rottumeroog` and a whole `vondstaantal`
  dimensioned by it; `eerste vondstaantal` and `laatste vondstaantal` the
  first and the last over all kustvakken, `vondstvakken` the filled count
  over them, and `oosteerste` and `oostlaatste` the first and the last over
  the set `schiermonnikoog`, `ameland`, `terschelling` in that order. J1 with
  the cells 1, leeg, 0, 0, 6, 7 in label order gives **eerste vondstaantal 1,
  laatste vondstaantal 7, vondstvakken 5, oosteerste 6, oostlaatste 0**; J2
  with the cells leeg, leeg, 0, leeg, 6, leeg gives **0, 6, 2, 6, 0**; J3
  without a cell gives **leeg, leeg, 0, leeg, leeg**. A fold by value would
  give J1 a first of 0, and the declared order of the set an oosteerste of
  0. Over two dimensions, on the cells (`voorjaar`, `noord`) 2.00,
  (`najaar`, `noord`) 1.00 and (`voorjaar`, `zuid`) 5.00, the first over all
  seasons and both stranden reads (`voorjaar`, `noord`), (`voorjaar`,
  `zuid`), (`zomer`, `noord`), (`zomer`, `zuid`), (`najaar`, `noord`),
  (`najaar`, `zuid`) and gives **2.00**, the last **1.00** and the filled
  count **3**; with `strand` outermost the last would be 5.00. In
  `jutterswet-seizoenvouw` J1 (`voorjaar` 3.00, `najaar` 2.00) has the
  first over all seasons **3.00**, the last **2.00**, the first over
  `{najaar en voorjaar}` **2.00** and the filled count **2**; J4 (`zomer`
  0.00, `najaar` 1.50) the first **0.00**, a filled cell of 0, and the
  filled count **2**; and the sum "of 0,50 kg als die er niet zijn" is
  **0.50** for J2 without a cell.
- **Cells of a text, a date and a boolean** (`jutterswet-seizoenlogboek`):
  J1 with `vondstsoort` `voorjaar` "barnsteen" and `zomer` "wrakhout",
  `meldingsdatum` `voorjaar` 2026-04-12 and `najaar` 2026-10-03, and
  `vergunning` `zomer` waar: the rule "De vondstsoort in najaar van een Jutter
  moet gesteld worden op zijn vondstsoort in zomer" gives the cell `najaar`
  **"wrakhout"**, the first of `vondstsoort` over all seasons **"barnsteen"**,
  its filled count **3**, the last of `meldingsdatum` **2026-10-03**, and
  "Het zomertarief van een Jutter moet berekend worden als 2 indien zijn
  vergunning in zomer gelijk is aan waar" gives **zomertarief 2**.
- **Writing**: "Het vondstgewicht in najaar van een Jutter moet berekend
  worden als zijn vondstgewicht in voorjaar maal 2", "Het vondstgewicht in
  zomer van een Jutter moet geïnitialiseerd worden op 0 kg" and the sum
  `jaargewicht` over all seasons. J1 with `voorjaar` 1.50 gives **najaar
  3.00, zomer 0.00, jaargewicht 4.50**. J2 with `voorjaar` 1.50 and `zomer`
  2.00 gives **zomer 2.00** — the input fills the cell, so the
  initialisation is not at its turn — **najaar 3.00, jaargewicht 6.50**. J3
  without input gives **najaar 0.00** (the product coalesces a leeg operand
  to 0, ch. 1.2), **zomer 0.00, jaargewicht 0.00**. J4 with `najaar` 5.00
  alone gives **najaar 0.00**: the computed value displaces the input of the
  cell (ch. 7.8); J5 with `voorjaar` 1.00 and `najaar` 5.00 gives **najaar
  2.00, jaargewicht 3.00**. An object creation "Een Jutter heeft een
  geboekte vangst met vangstgewicht in najaar gelijk aan zijn vondstgewicht
  in najaar" makes for J1 a `Vangst` whose cell `najaar` is **3.00** and whose
  other cells are **empty** (`jutterswet-seizoenboeking`). With "maal 1,5"
  in place of "maal 2" the rule is refused by the precision gate of ch. 2.4
  point 4, a cell of 2 decimals times 1,5 carrying 3
  (`corpus/negative/wf023-cel-schrijfgrens`); behind the gate, J1 with
  `voorjaar` 1.25 faults: 1.875 does not fit 2 decimals, **EF-006** at
  `J1.vondstgewicht[najaar]`, value 15/8, decimals 2 (`ref/test` row
  `WV-5.8c-schrijfgrens-ef006`).
- **The standing norm and the pair on a cell**: "Het vondstgewicht in najaar
  van een Jutter moet berekend worden als zijn vondstgewicht in voorjaar" and
  the same rule on `zomer`, each its cell's only writer
  (`jutterswet-seizoenkopie`). J1 with `voorjaar` 12.50 and `najaar` 4.00
  gives **najaar 12.50, zomer 12.50**; J2 with `zomer` 7.00 alone gives
  **zomer 7.00** — the copy of an empty cell is leeg and writes nothing — and
  **najaar leeg**. With an initialisation "Het vondstgewicht in najaar van
  een Jutter moet geïnitialiseerd worden op 0 kg" beside the copy into
  `najaar`, J1 with `voorjaar` 2.00 gives **najaar 2.00**, J2 without input
  **najaar 0.00** (the copy evaluates leeg and does not decide, so the
  initialisation does), and J3 with `najaar` 5.00 alone **najaar 5.00**.
  `jutterswet-seizoenaanvang` writes the pair with an initialisation on 1 kg
  and the sum over all seasons: J1 with `voorjaar` 2.00 gives **najaar 2.00,
  jaargewicht 4.00**, J2 without input **1.00, 1.00**, J3 with `najaar` 5.00
  alone **5.00, 5.00**, and J4 with `voorjaar` 3.00 and `najaar` 5.00 **3.00,
  6.00**.
- **The case**: on `vondstgewicht` dimensioned by `seizoen` and `strand`, the
  cell `zuid`, `voorjaar` — two declared labels in the reversed order — is
  refused with **EF-034** {`label`: j1, `attribute`: vondstgewicht,
  `labels`: zuid, voorjaar} (`corpus/red/jutterswet-cel-onbekend-label`), and
  the cell `voorjaar`, `zuid` named twice with **EF-035** {`label`: j1,
  `attribute`: vondstgewicht, `labels`: voorjaar, zuid}
  (`corpus/red/jutterswet-cel-dubbel`); the input 1.005 in a cell of 2
  decimals is refused with **EF-004** {`place`: j1.vondstgewicht[voorjaar],
  `value`: 1.005, `decimals`: 2}.

**Where the executors stand** (measured 2026-11-02). The reference
interpreter and the ASP backend share the case reader, which holds the
refusals of the case above and lays a valid cell input down cell by cell.
Both read a cell, of the subject and over a path, fold over one dimension
and over more with each of the six kinds, and write a cell by a rule, an
initialisation and an object creation's member, and both refuse a read of a
dimensioned attribute outside a cell in WF-125's sentence. A cell is a member of its own in either
executor: `ref` holds it in its fact base under the attribute's name with
the cell's labels, `vondstgewicht[najaar]`, and the ASP backend reads and
writes it with the `val/3` of an attribute under the same name, its
declaration the attribute's (`spec/ASP-TRANSLATION.md`); a cell of a text,
an enumeration or a boolean of the subject is read there as such an
attribute is, on the subject and over a path. Both fold a cell of each
member of a set as a set fold folds an attribute of each member. The pair of a computing rule and an initialisation on one cell
reads in the defaults reading in both: `ref` natively, the ASP backend
through the ch. 7.7 lowering on the cell. The worked examples replay in both:
the goldens `jutterswet-seizoenvondst`, `jutterswet-seizoenvouw` (the six
kinds and a sum with a fallback), `jutterswet-vormenkabinet` (a cell over two
dimensions that share a label), `jutterswet-seizoenboeking`,
`jutterswet-seizoenkopie`, `jutterswet-strandseizoen` (cells over two
dimensions, read, written and folded, written in RegelSpraak),
`jutterswet-seizoenaanvang` (the pair on a cell),
`jutterswet-seizoenlogboek` (cells of a text, a date and a boolean) and
`jutterswet-ploegseizoen` (cells over a path and of each member), the
`ref/test` rows `WV-5.8c-*` and the frames of
`backends/clingo/test/test_clingo.ml` for two dimensions, a path, first,
last and the filled count, the pair on a cell and the write boundary of a
cell; the red witness `corpus/red/jutterswet-cel-overprecisie` carries the
input precision of a cell, and the case beside
`corpus/negative/wf023-cel-schrijfgrens` the write boundary of a cell, which
the executors' suites replay behind the precision gate. The Drools
backend holds the same refusals of the case in a case reader of its own and
keeps a dimensioned attribute as a map of its cells; it reads a cell and
folds over one dimension and over more with each of the six kinds, of an
attribute of the subject and of the object a path of to-one roles reaches,
the input off the map of the reached bean and a written cell through the
standing of the path; a fold over a set reads a cell of each member. A cell a form writes is a member of
its own there as well, under the key of its node (IR-SPEC ch. 5.1): the
rule, the initialisation and the object creation's member write it, a read
and a fold take it through its standing, and the write-back puts it into the
map. The nine goldens replay in all three executors (gate 12), the red
witnesses of the input precision and of the write boundary fault in all
three with their pinned code and parameters (gate 8), the Drools backend
naming the place of the write boundary as `ref` does,
`j1.vondstgewicht[najaar]`, and the backend's `DimensionCellWitnessTest`
replays the examples over two dimensions on its cells 2.00, 1.00 and 5.00,
the first, the last and the filled count among them.
The worked example of reading over a path replays in
`jutterswet-ploegseizoen` and in its rows of the backend's witness.

### 5.9 Uniqueness: the union and the scope

Both uniqueness forms fire at the fixpoint of their stratum and deposit
idempotent §9.5 channel facts per (form name × instance), like the
single form of ch. 5.6. Mirrored from the pinned runtime:

1. **The union is one shared key domain, counted per (instance ×
   source).** The key tuples of ALL sources — compared positionally,
   after per-position scale harmonisation — form a single uniqueness
   domain: a duplicate may arise across two different source types.
   The count is the v0 SUM of the per-source counts: an instance
   contributing one tuple through two sources counts twice and
   duplicates against itself (two same-type sources over one row whose
   two keys are equal mark that row). Every instance whose tuple's
   summed count exceeds one is marked, whichever source it came from,
   under the shared union name. Rows with an empty key component do not
   participate (an empty key never collides, the ch. 5.6 rule). Source
   arity, per-position datatype kind and unit agree, and a union
   carries at least two sources (WF-030). A **temporal key** reads
   **ever-uniqueness**, in the SINGLE form and the union alike — one
   shared tuple function carries both, and the single form composes it
   with its scope machinery (scope parent × ever-tuples), so the same
   key reads the same way wherever it appears (compositionality,
   decided 2026-08-20; v0's own backends disagreed —
   one skipped the union, the other checked it — so this is a v1 norm,
   not a mirror): a key is an identity, not a measurement, so every
   value the timeline EVER carries is claimed, and two instances
   collide even when their carrying periods do not overlap (a number
   once issued stays issued; date-bound numbering is modelled
   explicitly, as an object type with a validity period). The tuple set
   is deduplicated per instance × source: same-instance redeclaration
   is no collision. With a mixed compound key the tuples pair the
   scalars with every value the temporal component ever carries; at
   most ONE temporal component per source is executed (a second needs
   the same-moment tuple join — loud until a corpus case lands). A
   temporal key supplied as a bucket scalar faults: ever-uniqueness
   needs the full timeline, a scalar would silently miss values — and
   this form error precedes the scope narrowing: a scope-less instance
   whose key has the wrong shape still faults (data integrity screams
   before participation is decided, in both executors).
   Worked examples (the `jutterswet-nummerregister` union anchor and
   the `jutterswet-strandregister` single-form anchor, verbatim in
   gates 5–6): Strandvondst S1 carries vondstnummer 7 over
   [2024, 2025) and Wrakvondst W1 carries 7 over [2026, 2027) —
   non-overlapping, yet **inconsistent** (the ever collision); S1
   carrying 7 over both [2024, 2025) and [2026, 2027) beside W1's 8 is
   **consistent** (same-instance redeclaration deduplicates); and the
   compound (reekscode, volgnummer) = (4, 3) carried by S1 in 2024 and
   by W1 in 2026 collides across those different moments while
   (9, 3) does not. On the single form: two Strandvondsten carrying 7
   over 2024 and 2026 respectively are **inconsistent** (the same ever
   collision, one type); and under the parent scope the same kavel
   number ever-carried by two vondsten of ONE kust collides while the
   same pair under two different kusten does not (scope × ever).
2. **The scope partitions, and scope-less rows are OUT.** A
   parent-scoped uniqueness (WF-054) tests its keys within each
   sub-population sharing one scope parent — the same key under two
   different parents never collides. An instance without a scope link
   does not participate AT ALL: duplicates among scope-less instances
   go undetected. That is the pinned runtime's silent narrowing,
   mirrored deliberately and recorded here — a document that wants
   global uniqueness declares it unscoped.
3. **A scoped union does not exist.** v0 cannot represent it (the union
   node carries no scope field and its resolver never produces one);
   the wire keeps that shape unrepresentable until a v1 decision.

Worked example (the `jutterswet-kustregister` anchor, verbatim in gates
5–6): vondstnummer 7 on a Strandvondst AND on a Wrakvondst → both
marked under "eenmalig vondstnummer" (case 2, the cross-type
duplicate). Two kavels with etiket 5 in ONE gemeente → both marked
(case 3); the same etiket in two different gemeenten, a scope-less
kavel duplicating a scoped one, and an empty vondstnummer all stay
consistent (case 1 — the three exclusions).

### 5.10 The composite condition short-circuits

A composite condition (`Compound`) applies a quantifier to an **ordered**
list of members. Reading it is not a set operation. The members are
evaluated **left to right in wire order**, and evaluation **stops at the
first member after which the outcome stands**. A member that is never
reached raises no fault and deposits no §9.5 channel fact.

One rule covers all five quantifiers. Write *h* for the hits among the
members already read and *r* for the members still unread. Evaluation
stops as soon as one of these holds:

| quantifier | stop trigger | outcome |
|------------|--------------|---------|
| `all` / `the` | a member read does not hold | **false** |
| `none` | a member read holds (*h* > 0) | **false** |
| `atLeast` *d* | *h* ≥ *d* | **true** |
| `atLeast` *d* | *h* + *r* < *d* (the threshold is out of reach) | **false** |
| `atMost` *d* | *h* > *d* | **false** |
| `atMost` *d* | *h* + *r* ≤ *d* (the threshold can no longer be exceeded) | **true** |
| `exactly` *d* | *h* > *d* | **false** |
| `exactly` *d* | *h* + *r* < *d* | **false** |

If no trigger fires, every member is read and the verdict is the plain
comparison over the total hits *h* against the member count *n*: `all`
and `the` need *h* = *n*, `none` needs *h* = 0, `atLeast` *h* ≥ *d*,
`atMost` *h* ≤ *d*, `exactly` *h* = *d*.

The table is **one rule with no exception**: each row marks a moment at
which the outcome can no longer change, and the rows together are the
exhaustive list of those moments per quantifier. There is no further
criterion — nothing is weighed about how a threshold was *meant*; a
trigger fires exactly when the arithmetic says the verdict is fixed
(maintainer decision, 2026-09-07).

**The triggers are tested before each member, the empty prefix
included.** With *h* = 0 and *r* = *n* three rows can already fire on the
empty prefix, and then the composite reads **no member at all**:

- `atLeast` and `exactly` with ***d* > *n***: the threshold is out of
  reach from the start, so the composite is **false**;
- `atMost` with ***d* ≥ *n***: the threshold can never be exceeded, so
  the composite is **vacuously true**.

That is not an edge case bolted on — it is the same rule applied to the
prefix of length zero. It is observable, because the members it does not
read are exactly the members whose faults and §9.5 facts do not happen: a
vacuously true `atMost` over a member that would divide by zero
**answers**, and deposits nothing. The wire admits all three shapes (WF-041 constrains only
*d* ≥ 1) and this clause fixes what they mean.

**The short circuit never changes a truth value.** Every trigger is
stable under the members it skips: as reading proceeds *h* only grows and
*h* + *r* only shrinks, so a trigger that has fired already decides the
final comparison whatever the unread members would have done. What the
short circuit changes is exactly one thing: *which faults and which §9.5
channel facts come into being.* That is why it is semantics and not an
optimisation — a member that would fault decides, by being reached or
not, whether the evaluation has an answer at all.

**Nested composites recurse under the same rule.** A member that is
itself a composite counts as one member of its parent — it holds or it
does not — and its own members are read left to right under its own
quantifier's triggers.

*Mirror note.* The source language states the ordering as a generic
drafting rule (§11.1: conditions are checked left to right, and variables
are not computed unnecessarily) and §10.2/§11.1 give the composite its
form. The two executors realised it unevenly before this clause: a
conjunction lowered to a chain of pattern constraints short-circuits by
construction, while a counting quantifier folded into one counting pass
does not, and a translation whose fault fences are order-free fires them
all. **Delta (promoted):** v1 makes the rule uniform, explicit, and
identical across quantifiers and executors.

**One boundary, recorded rather than left implicit.**

The ch. 3.6 composite **gedurende clip** is a different construct —
but only on its **inside**. Its own members yield period sets that are
intersected, and an intersection has no moment at which "the outcome
stands" in the sense above, so **the clip's own members stay fully
evaluated** and their WF-055 shape faults stay data-independent.

That exemption reaches no further. A totalled clip is an
**expression**, so it can appear as an operand of a comparison that is
itself a member of an ordinary composite — and *there* it is read like
any other operand: if the prefix has settled the outcome, the clip is
not evaluated and none of its fences or channel facts arise. The two
readings are easy to conflate and were: `jutterswet-klipwacht` is the
witness that separates them.

Worked example (absolute; the `jutterswet-ploegaandeel` corpus golden,
verbatim in gates 5–6). Member 1 tests the divisor; member 2 divides by
it:

```
bergingsaandeel ← 1  indien  ALLE [
    ploegomvang ongelijk aan 0 ;
    ⌊vangstwaarde ÷ ploegomvang⌋ groter dan 0 ]
```

| ploegomvang | vangstwaarde | member 1 | member 2 | `bergingsaandeel` |
|-------------|--------------|----------|----------|-------------------|
| 0 | 100 | **false** | *never read* | **leeg** |
| 4 | 100 | true | ⌊100/4⌋ = 25 > 0 → true | **1** |
| 7 | 3 | true | ⌊3/7⌋ = 0 > 0 → false | **leeg** |

Row 1 is the clause: without it, member 2 divides by zero and deposits a
§9.5 fact of kind EF-005 (ch. 1.4 point 4); with it the channel stays
empty, and the golden pins `consistent`. Rows 2 and 3 are the control —
member 2 *is* read whenever member 1 holds, and decides both ways.

Worked example (absolute; the `jutterswet-wachtdeling` corpus golden,
verbatim in gates 5–6) — the same two members, in the same order, under
two counting quantifiers, showing a stop that fixes the outcome **false**
and a stop that fixes it **true**:

```
geenteken   ← 1  indien  GEEN        [ ploegomvang gelijk aan 0 ; ⌊vangstwaarde ÷ ploegomvang⌋ groter dan 0 ]
minsteteken ← 1  indien  TEN MINSTE 1 [ ploegomvang gelijk aan 0 ; ⌊vangstwaarde ÷ ploegomvang⌋ groter dan 0 ]
```

| ploegomvang | vangstwaarde | member 1 | member 2 | `geenteken` | `minsteteken` |
|-------------|--------------|----------|----------|-------------|---------------|
| 0 | 100 | **true** | *never read* | **leeg** (*h* = 1 > 0) | **1** (*h* = 1 ≥ 1) |
| 4 | 100 | false | ⌊100/4⌋ = 25 > 0 → true | **leeg** | **1** |
| 7 | 3 | false | ⌊3/7⌋ = 0 > 0 → false | **1** | **leeg** |

Row 1 stops at the same member for both rules and yields opposite
verdicts: the stop follows from the quantifier's own trigger, never from
a special case for the conjunction.

**Every row of the trigger table carries its own artifact.** A stop is
observable only as a fault or a §9.5 fact that does *not* happen, so a
witness that pins verdicts alone leaves the row free to move. Each row
below is therefore pinned by a case in which the member after the stop
*would* divide by zero, and which pins the channel `consistent` — measured
against a mutant that changes exactly that row (until the numeric zero went
to the channel, 2026-10-19, the member would fault, and the case pinned the
answer). The other direction — a member the outcome has not settled away
*is* read — is the golden `at-most-reads-until-settled`, whose member 2
deposits EF-005:

| row | witness |
|-----|---------|
| `all` / `the`, first non-holding member | `jutterswet-ploegaandeel` (row 1) |
| `none`, first holding member | `jutterswet-wachtdeling` (`geenteken`, row 1) |
| `atLeast`, threshold reached | `jutterswet-wachtdeling` (`minsteteken`, row 1) |
| `atLeast`, out of reach | `jutterswet-seinwacht` (`minsteteken`, *d* = 3 > *n* = 2) |
| `atMost`, threshold exceeded | `jutterswet-seinwacht` (`hoogsteteken`, `S1`) |
| `atMost`, can no longer be exceeded (*d* < *n*) | `jutterswet-vacuumwacht` (`stopteken`, `B1`) |
| `atMost`, vacuously true (*d* ≥ *n*) | `jutterswet-vacuumwacht` (`vacuumteken`, every row) |
| `exactly`, threshold exceeded | `jutterswet-seinwacht` (`preciesteken`, `S1`) |
| `exactly`, out of reach | `jutterswet-seinwacht` (`randteken`, *d* = 3 > *n* = 2) |
| triggers tested *before* the member | `jutterswet-seinwacht` (`minsteteken`): a check made afterwards reads the dividing member 1 |

### 5.11 Operand order

A node's operands are evaluated **left to right in wire order**. One
rule, for the whole expression language; ch. 5.10's composite is its
best-known instance, not an exception to it.

The order is semantics for exactly the reason ch. 5.10 gives for the
short circuit: it never changes a value, and it decides **which faults
and which §9.5 channel facts come into being**. Four things end an
evaluation part-way, and each leaves every operand after it in the order
unevaluated:

- a **fault** (ch. 5.7) — the rule's evaluation fails observably;
- the **ch. 10.6 barrier** — a read of a binder whose need is leeg ends
  the `Let` that bound it;
- the **ch. 5.10 short circuit** — a composite stops at the member after
  which its outcome stands;
- **leeg propagation — at exactly one node.** `Fold`: a leeg `list` is
  the whole fold's answer, so its `init` is never evaluated (ch. 10.2).
  That is the *only* place where leegte ends the walk early. The other
  leeg-propagating forms — `ListLiteral`, `StructLiteral`, `Invoke`,
  `MapList` — evaluate **every** operand first and test for leegte
  afterwards, so a leeg operand costs its successors nothing: they still
  run, still fault, still deposit. The distinction is easy to smooth
  over and only the second half is what the reference interpreter does,
  so it is written out: leeg propagates in the **result**, not through
  the **walk**.

An operand that is never reached raises no fault and deposits no channel
fact. A deposit already made **stands**: channel facts are keyed by
(rule × subject) and inserted whatever the rule goes on to do — a leeg
outcome writes nothing and retracts nothing (ch. 5.6).

**Wire order** is the order the node declares its operands in: for a
list-valued slot the list's own order, for named slots the order the
**schema** gives them — `spec/schema/uchar-ir-v1.schema.json`,
`$defs/Clamp` (`node`, `base`, `minimum`, `maximum`), `$defs/Aggregation`
(`arguments` before `emptyFallback`), `$defs/Fold` (`list` before `init`).
For the tijdsduur family the slot is a list: argument 0 is `van`,
argument 1 is `tot`.

The schema is named here rather than `IR-SPEC.md` because a clause about
*slot* order has to name the slots as a document carries them. Until L5
the two spelled the metamodel apart — `IR-SPEC.md` wrote `Clamp`,
`Rounding`, `TextComposition` where the wire wrote `Begrensd`, `Afgerond`,
`TekstReeks`; since L5 the wire carries the `IR-SPEC.md` names.

**How far that is checked, stated exactly.** JSON Schema has no ordering
vocabulary, and gate 2's round-trip re-emits each document in *its own*
key order — so a document that writes `maximum` before `minimum`
validates and round-trips green. Measured: swapping the two keys in a
golden leaves gates 2, 3 and 5 passing. What the schema gives is the
**convention** that spells the order, and what pins it mechanically is
narrower: gates 9 and 10 re-emit 50 of the corpus's 192 documents (38
`source.rgs`, 12 `source.catala_en`) byte-identically from source, so for
those the emitter's slot order is fixed. For the remaining 142
hand-written documents nothing checks it. This does not weaken the
clause — the order that matters here is the order a consumer **reads
operands in**, and that is pinned by the artifact table below, in every
executor, for every visible row. It does mean the wire convention itself
rests on the emitters and on review.

Nothing here is left to the executor. A consumer that evaluates
`Clamp`'s `maximum` before its `minimum` is **wrong**, not merely
different — and this clause is written because that is precisely what an
executor does when its host language leaves the argument order of an
application unspecified and nobody chose.

Two consequences the rule is chosen to give, rather than accidents it
inherits: within `Clamp` the operands are now evaluated in the same
direction ch. 2.7 **applies** the bounds in (minimum first), and
"tijdsduur van A tot B" evaluates A before B, in the order the phrase
names them.

#### The sweep

Every expression and condition node of the wire, with its verdict.
**Visible** means two or more operand positions are each capable, on some
input, of faulting, depositing a §9.5 fact or forcing a ch. 10.6 need —
so which runs first is observable. **Forced** means the data dependency
already fixes the order; there is no freedom to spend. **Invisible**
means at most one such position exists, so there is no order to see. The
verdict is a property of the **form**, not of any one executor: where the
reference interpreter declines a form today that is noted, and the
normative order still stands, because the spec is engine-neutral and the
next consumer may carry it.

Node names below are the **wire's**, as the schema spells them. The one
family that is not a node per name is the aggregation and builtin family:
two wire nodes, `Aggregation` and `BuiltinCall` (one `Functie` until L4),
each discriminated by its `kind`, so its rows are named by kind
rather than by a node name it does not have: the wire's value, with the
source language's word in parentheses (`sum` (som), `duration`
(tijdsduur), `concatenation` (concatenatie), …). The rows of `Predicate`
name its kinds the same way.

| node | evaluated operands | verdict | normative order |
|------|--------------------|---------|-----------------|
| `Binary` (all operators) | `left`, `right` | **visible** | `left`, then `right` |
| `Comparison` | `left`, `right` | **visible** | `left`, then `right` |
| `TimeWindow` (the period condition) | `from`, `to` | **visible** | `from`, then `to`; an absent bound evaluates nothing, and BOTH bounds are read before the evaluation date decides. *Carried by both OCaml executors since 2026-09-20 (ch. 3.10); the order is stated and not yet witnessed by a case, because the two bound shapes WF-111 admits — a literal and an own attribute — have no fault position to order* |
| `Clamp` | `base`, `minimum`, `maximum` | **visible** | `base`, then `minimum`, then `maximum`; an absent bound evaluates nothing |
| `duration` / `absoluteDuration` (tijdsduur / absolute tijdsduur) | argument 0 (`van`), argument 1 (`tot`) | **visible** | `van`, then `tot` |
| `dateWith` / `dateTimeWith` (datum met / datum-tijd met) | the `arguments` list: year, month, day, and for `dateTimeWith` hour, minute, second, millisecond (WF-037 leaves its arity open; ch. 3.8 fixes it) | **visible** | argument order; every part runs before the leeg test, so a leeg part costs its successors nothing |
| aggregation (`sum` (som), `total` (totaal), `count` (aantal), `minimum`, `maximum`, `first` (eerste), `last` (laatste), `filledCount`) | the contributors — the listed `arguments`, or the instance fold's members each gated by the `filter` — then the `emptyFallback` | **visible** | the contributors in wire order (an instance fold: the filter per member, members in fold order), then the `emptyFallback` (WF-037 confines it to `sum`) |
| aggregation over `dimensionSelections` (the third contributor source; WF-037 admits it on `sum`, `minimum`, `maximum`, `first`, `last`, `filledCount`) | the selected cells, then the `emptyFallback` | **visible** | the cells of the product of the selections, the first selection outermost and each in its own label order (ch. 5.8c), then the `emptyFallback`. *All three executors evaluate the form; no cell read faults, and the first and the last over cells witness the order, which they pick by* |
| `concatenation` (concatenatie), `TextComposition` | the inserted parts | **visible** | part order |
| `ListLiteral` | `elements` | **visible** | element order |
| `StructLiteral` | `fields` | **visible** | declared field order (WF-073) |
| `Invoke` | `arguments`, then the body | **visible** | argument order, then the body (a leeg argument propagates without entering it) |
| `Fold` | `list`, `init`, the body per element | **visible** | `list`, then `init`, then the elements in list order (a leeg `list` answers before `init` is read) |
| `MapList` | `list`, the body per element | **visible** | `list`, then element order |
| `FilterList` | `list`, the condition per element | **visible** | `list`, then element order |
| `Compound` | the members | **visible** | ch. 5.10: wire order, with the short circuit |
| `Let` | `value`, `body` | forced | ch. 10.6: the `body` drives; the `value` is evaluated at the **first read**, so it takes that read's position in the enclosing order |
| `Match` | `scrutinee`, one arm body | forced | the scrutinee selects the arm |
| `IfThenElse` | `condition`, one branch | forced | the condition selects the branch; the other branch evaluates nothing |
| `Rounding`, `squareRoot` (wortel), `absoluteValue` (absolute waarde), `yearOf` (jaar uit), `monthOf` (maand uit), `dayOf` (dag uit), `easterSunday` (eerste paasdag), `ListLength`, `FieldAccess`, `Construct`, `DimensionCellRead` | one | invisible | — |
| `hourOf` (uur uit), `minuteOf` (minuut uit), `secondOf` (seconde uit), `millisecondOf` (milliseconde uit) | one | invisible | — *(ref declines them; a single operand carries no order either way)* |
| `timeProportionalPart` (tijdsevenredig deel) | the base, and the `filter` | invisible | WF-056 restricts the base to a **bare** temporal attribute reference, so only the filter is data-dependent — one fault-capable position, no order |
| `dayCount` (aantal dagen in) | none in `arguments`; the `filter` carries the condition | invisible | — |
| `TemporalRead` | `date` only — `base` **names** the timeline and is never evaluated (ch. 3.3) | invisible | — |
| `Predicate` — `empty` / `filled` (leeg / gevuld) | `operand` | invisible | — |
| `Predicate` — `elevenTest` / `exactDigits` / `dayKind` (elfproef / numeriek met exact / dagsoort) | `operand` | invisible | — *(ref declines them)* |
| `CharacteristicCheck`, `RoleMembership` (until L2 `Rollidmaatschap` and `VerreRollidmaatschap`), `Regelversie` | none | invisible | — |
| `Literal`, `AttributeReference`, `ParameterReference`, `VariableReference`, `ContextValue`, `BoundDay`, `BinderRef`, `Unresolved` (the expression and the condition variant) | none | invisible | — (a `BinderRef` forces its ch. 10.6 need **at its own position** in the parent's order) |
| `Lambda` | — | not an operand position | never a value (WF-069): it is only the `function` slot of a `Fold`/`MapList`, applied once per element under that node's row |

**The som's `emptyFallback` is evaluated unconditionally.** It is an
operand of the node, not a lazy alternative: it runs every time the node
runs, whether or not the contributors leave its value to be used. Its
faults and its §9.5 deposits therefore happen on documents where the
value is discarded — the row above orders it, and this sentence says that
ordering it is not the same as skipping it. Witness:
`sum-fallback-always-evaluated` (gate 8), which stops failing the moment
an executor makes the stand-in lazy.

Rule- and form-level order — which rule fires first, which member of a
defaults group is tried — is **not** this clause's subject: ch. 5.2 makes
the fixpoint independent of it, and ch. 7.1 fixes the defaults chain by
priority rather than by evaluation.

**Worked example — `Clamp` (absolute; the `clamp-reads-bounds-in-order`
red case verbatim, gate 8).** A `Jutter` with `grens` **absent**, in a
session with no evaluation date:

```
begrenswaarde := Let grenswaarde = zijn grens
                 in (100 met een minimum van het Rekenjaar
                         en een maximum van de grenswaarde)
```

`base` = 100 evaluates first. `minimum` = the `evaluationYear` context
value evaluates second and **faults** — there is no evaluation date to read
(ch. 8.2). `maximum` is never reached. Pinned outcome: the rule **fails
observably**, fault kind EF-011. (Until 2026-10-19 the minimum was `1 ÷ 0`,
whose fault went to the §9.5 channel with ch. 1.4 point 4; a deposit would
not end the evaluation, so it could not show the order.)

Under the reverse order the `maximum` would run first, its read of
`grenswaarde` would raise the ch. 10.6 barrier, the whole `Let` would be
leeg, the evaluation year would never be read, and the same document would
quietly write nothing. The two orders do not differ in a value; one has
an answer and the other does not.

**Worked example — tijdsduur (absolute; the
`duration-reads-van-before-tot` red case verbatim, gate 8).** A `Vaart`
with `vertrek` **absent**, in a session with no evaluation date:

```
vaartduur := Let vertrekdatum = zijn vertrek
             in de tijdsduur van de rekendatum tot de vertrekdatum in hele dagen
```

`van` = the `evaluationDate` context value evaluates first and **faults** — there
is no evaluation date to read (ch. 8.2). `tot` is never reached. Pinned
outcome: the rule **fails observably**. Under the reverse order the read
of `vertrekdatum` would raise the barrier first and there would be no
fault at all.

**Worked example — the channel count (absolute; the
`synth3-operandvolgorde` golden verbatim, gates 5–6).** A `Vangst` with

```
vangstsom := Let deel = zijn deelvangst
             in (het deel  plus  zijn teller gedeeld door zijn noemer)
```

`left` is a read of `deel` — the barrier, when `deelvangst` is absent.
`right` is a division with a filled dividend and an absent divisor — a
§9.5 producer (ch. 5.6). The value cannot separate the two orders,
because `leeg plus …` coalesces either way; the channel can:

| case | `deelvangst` | `teller` | `noemer` | `vangstsom` | channel facts |
|------|--------------|----------|----------|-------------|---------------|
| barrier | *absent* | 4 | *absent* | **leeg** | **0** — `left` barriers, `right` is never reached |
| companion | 5 | 4 | *absent* | 5 + leeg = **5** | **1** (`vangstsom van een vangst`, that instance) |
| filled | 5 | 4 | 2 | 5 + 2 = **7** | 0 |

The first row is the order pin and the second is its control: without a
case in which the division *is* reached, a zero count would also be
satisfied by a division that never deposits under any circumstance.

**What the executors can and cannot witness — recorded, not glossed.**

The ASP encoding has **no** operand evaluation order: a body is a
conjunctive set of atoms, and the one order-derived construct it carries
is ch. 5.10's prefix guard, which exists only because that short
circuit's triggers are monotone in the prefix. Nothing analogous is
available for a clamp's two bounds or a duration's two dates. Two
consequences, both measured:

- Its `Clamp` order *is* pinned, but on the **translation** side: a
  bound that is not a ground literal is refused (an empty bound is ref's
  *no* bound, which the bare join cannot express), and the red case
  above pins which bound is refused first. That is the translator's
  visiting order, not an evaluation order — and it is enough to catch a
  reversal.
- Its `Let` guards the **whole body** with the need's atoms, where ref
  stops at the read **positionally**. The two coincide on every shape in
  which the barrier position precedes the depositing position — which is
  every shape the corpus carries. They part company on the mirror image,
  where ref's already-made deposit stands (the paragraph above: deposits
  are not retracted) and the ASP side's never arises. That boundary is
  recorded here rather than papered over; it is the reason no golden in
  this corpus places a deposit before a barrier in wire order.

The third executor is a **known divergence**, not a gap: it evaluates a
`Clamp`'s maximum **before** its minimum, measured in both
directions on the red case above (move the division to the maximum and
it faults on the document where ref does not). It is listed with that
reason in the red gate's exclusion list, and correcting it is a card of
its own.

**The v0 pin, measured row by row — two of the three already agree.**
The pin's *static trace walk* (the bookkeeping that decides which
companion preconditions and which hoists the emitted rules carry) runs
`Clamp` maximum-first, the tijdsduur operands in reverse, and the
aggregation's `emptyFallback` before the contributors, and an arbiter test
pins that with absolute channel-fact counts, citing the *reference
interpreter's host-language argument order* as its evidence — the
inversion this clause exists to end. But what the pin **emits** is a
different question from what it walks, and it splits the three rows:

- **clamp** — the emitted term applies the clamp to its base, minimum
  and maximum in that order, and the host evaluates a call's arguments
  left to right. At fire time the pin is **already** ch. 5.11.
- **tijdsduur** — likewise: the emitted term takes `van` then `tot`.
  Already ch. 5.11 at fire time.
- **the aggregation's `emptyFallback`** — **not** already, and this is the
  row that matters. The pin emits a *conditional*: the stand-in is
  evaluated only when the contributors turn out empty (in one lowering a
  ternary, in another a fold that reaches its stand-in only at a zero
  count; a third flattens the test to a compile-time constant). That is
  exactly the laziness the **unconditional** clause above forbids, and
  it is a runtime divergence, not bookkeeping: a stand-in that faults or
  deposits does so in ref and not in the pin, on any document whose
  contributors are filled. `sum-fallback-always-evaluated` is the case
  that separates them.

So the correction owed there is of two kinds, and the second is the
substantive one: turn the static walk around on three rows, and make the
emitted stand-in **unconditional**. A **second, live walk in the same
pinned component** already uses this clause's orders, so the first kind
also removes a contradiction the pin carries internally. Until both
land, the pin is a measured deviation — on the channel-fact order for
two rows, and on channel facts and faults alike for the third — not a
second reading of the norm.

**Each visible row carries an artifact.** A row without one is spec text
an executor may quietly walk the other way, so the table below is the
clause's completeness check, not a courtesy. The corpus witnesses use the
§9.5 **channel count** — a barrier in the earlier position makes the
later position's deposit not happen — and the `ref/` examples use two
**distinguishable faults**, one per position, swapped between a pair: the
surviving message names the position that ran first. The fault device is
what reaches the rows a barrier cannot, because a closed fold body
(WF-069) has no enclosing binder in scope.

| what is pinned | witness |
|----------------|---------|
| `Binary` `left` before `right`, by channel count | `synth3-operandvolgorde` (gates 5–6, and Drools: the full square) |
| `Clamp` `minimum` before `maximum` | `clamp-reads-bounds-in-order` (gate 8; ref by which fault, clingo by which bound is refused first) |
| `duration` (tijdsduur) `van` before `tot` | `duration-reads-van-before-tot` (gate 8; the ref leg — clingo carries the form and fails, but does not order it) |
| composite members, wire order + short circuit | ch. 5.10's own witness table |
| `Comparison` `left` before `right` | `ref/` `WV-5.11-vergelijking-…` (pair) |
| aggregation contributors, in wire order | `ref/` `WV-5.11-som-bijdrager…` (pair) |
| the **instance** fold: the filter per member, members in fold order | `ref/` `WV-5.11-instantiefold-…` and `WV-5.11-instantiesom-…` (two pairs — the `Instances` walk a `count` (aantal) takes and the `Values` walk a set-read `sum` (som) takes) |
| aggregation contributors **before** the `emptyFallback` | `ref/` `WV-5.11-som-…-leegwaarde` (pair) |
| the `emptyFallback` evaluates unconditionally | `sum-fallback-always-evaluated` (gate 8) + `ref/` `WV-5.11-som-leegwaarde-onvoorwaardelijk` |
| `concatenation` (concatenatie) and `TextComposition` part order | `ref/` `WV-5.11-concat-…`, `WV-5.11-tekstreeks-…` (pairs) |
| `ListLiteral` element order | `ref/` `WV-5.11-listliteral-…` (pair) |
| `StructLiteral` field order | `ref/` `WV-5.11-structliteral-…` (pair) |
| `Invoke` argument order | `ref/` `WV-5.11-invoke-…` (pair) |
| `Fold` `list` before `init` | `ref/` `WV-5.11-fold-…` (pair) |
| `MapList` element order | `ref/` `WV-5.11-maplist-…` (pair) |
| `FilterList` element order | `ref/` `WV-5.11-filterlist-…` (pair) |
| `dateWith` / `dateTimeWith` (datum met / datum-tijd met) part order | `ref/` `WV-5.11-datummet-…` (pair) |
| `TimeWindow` `from` before `to` | `ref/` `WV-5.11-venster-…` (pair): since WF-111 admits a date expression over the parameters and literals (2026-11-03), a bound can fault — a date construction whose parts name no day (EF-032, ch. 3.8) |

### 5.12 The reading of a condition at the fixpoint

The **reading** of a form's condition on an object is the evaluation of
that condition on the fixpoint world, recorded node by node. It is no rule
of evaluation and it changes nothing the session computes: it records an
evaluation that ch. 1–5 and 9–11 already define, once, at the end, and, for
a quantifier fold, the table its definition in ch. 3.9a fixes. A
consumer that says why a characteristic holds or why a value stayed leeg —
an explanation — reads it, instead of evaluating the condition again in a
semantics of its own.

**Which forms, which objects.** A form has a reading where it has a
condition: a rule version's `condition`, a consistency's `assertion`, an
allocation's, an object creation's and a fact creation's `condition`, and a
default's `justification` (ch. 7). The form is named by the JSON pointer
(RFC 6901) of its version or of its default:
`/rules/rules/k/versions/v`, `/rules/consistencies/k/versions/v`,
`/rules/allocations/k/versions/v`, `/rules/objectCreations/k/versions/v`,
`/rules/factCreations/k/versions/v`, and
`/rules/prioritizedDefaults/i/defaults/j`, with
`/exceptions/members/m/defaults/l` for each level of an exception group
(ch. 7.9). An ordinary rule that ch. 7.10 folds into a stack is read as a
default of that stack, under its own version's pointer and with its own
condition. The objects of a form are the objects it runs on at the
fixpoint, the objects a creation made included: the instances of its
subject type (ch. 2.2), for a rule on an attribute without the concrete
types on which ch. 7.10 takes it into a stack; for a stack the instances
of the concrete type it belongs to (ch. 7.10); for a fact creation its
anchors. A fact creation whose condition is a check on its target has no
reading: that condition filters the targets (ch. 5.5), so its verdict
belongs to an (anchor, target) pair and not to one object. A form without a
condition has a reading where it computes a value (card
`readings-of-unconditional-forms`, 2026-10-22): a rule version without a
`condition` that computes, equates or initialises an attribute, an
attribute default without a `justification` — the initialisation default
(ch. 7.3) — and an ordinary rule without a condition that ch. 7.10 folds
into a stack, whose justification there is the fold's vacuous one, which
stands nowhere in the document. It is the reading of its value (**A form
without a condition**, below). A form without a condition that computes no
value here — a characteristic attribution, a characteristic default, an
allocation, a creation — has none: there is nothing to read.

**The status.** Each (form, object) has one status:

| status | when | nodes |
|--------|------|-------|
| `HOLDS` | the condition holds | the nodes of the condition, and of the value |
| `HOLDS` | the form has no condition and is at its turn | the nodes of the value |
| `HOLDS` | the form is a default of a group on a timeline target that is at its turn on at least one day (ch. 7.11; **A default on a timeline target**, below) | the nodes of its justification, and of its consequence where the justification holds on some day |
| `FAILS` | the condition does not hold | the nodes of the condition |
| `FAULTS` | the evaluation of the condition faults (ch. 5.7) | the nodes, the fault at those it reached |
| `FAULTS` | the form has a dated validity and the session no evaluation date (ch. 8.1) | none |
| `DISPLACED` | the form is a default that is not at its turn because a higher default of its chain decided or settled (ch. 7.1, 7.9): a default below the one that decides, at the root or in an exception group, and every default inside its exception groups; the content of a default whose exception groups settled; an ordinary rule that ch. 7.10 folds into a stack, below the default that decides; the initialisation default below the default that decides, where the input leaves its target empty; on a timeline target, a default at its turn on no day (ch. 7.11) | the nodes of the condition, and of the value where the condition holds, as at its turn (**A displaced default is read**, below) |
| `DISPLACED` | the target of an initialisation — a rule's, a folded one's in a stack, or the initialisation default's — holds a value of the input or of another writer (ch. 5.4, 7.3), on a timeline target on every day; the subject of an object creation of the 1:1 shape is linked in the input (ch. 5.5) | none |
| `OUTSIDE_VALIDITY` | the validity window of the form does not admit the evaluation date (ch. 8.2) | none |

The session does not evaluate a form that is not at its turn or outside its
validity. Every default below the one that decides is displaced, and with
it every default inside its exception groups. A displaced default is read
all the same (below); an initialisation whose target the input or another
writer fills, a creation whose subject the input links and a form outside
its validity have no nodes. The emptiness of the target in the input is the
justification of an initialisation (ch. 7.3) and the link in the input the
guard of a 1:1 creation (ch. 5.5), and neither is a node of the form: where
they keep the form from its turn, there is no node whose evaluation the
reading could record. The two rows of `DISPLACED` are told apart by their
nodes: a default that a higher one displaced has the nodes of its
condition, or of its value where it has no condition, and a form of the
second row has none.

**The evaluation.** The condition is evaluated with the object as its
subject, on the fixpoint world, in the order of ch. 5.10 and 5.11. Where it
holds and the form computes a value — a rule on an attribute, an attribute
default's consequence — the value is evaluated after it. A characteristic
attribution, a consistency, an allocation and a creation compute no value
here: the member assignments of a creation are evaluated at its creation,
not at the fixpoint. The status is the condition's; a value that faults
leaves the reading `HOLDS` and carries its fault at its nodes.

**A form without a condition** (card `readings-of-unconditional-forms`,
2026-10-22) has no verdict to record. Where it is at its turn on the
object, its value is evaluated with the object as its subject, on the
fixpoint world, in the order of ch. 5.11, and its status is `HOLDS`, also
where the value is leeg or faults; where it is not at its turn, or the
window of its validity does not admit the evaluation date, its status is
`DISPLACED`, `OUTSIDE_VALIDITY` or `FAULTS` as a form with a condition has
it: an attribute default without a justification, and an ordinary rule
without a condition that ch. 7.10 folds into a stack, that a default above
it displaced has the nodes of its value (**A displaced default is read**,
below), and every other one has none. An initialisation without a
condition — a rule's, a folded one's in a stack, or the initialisation
default (ch. 7.3) — is not
at its turn where its target holds a value of the input or of another
writer (ch. 5.4, 7.3), on a timeline target where they leave it no day
(ch. 7.8). An attribute default without a justification, and an
ordinary rule without a condition that ch. 7.10 folds into a stack, is not
at its turn where a default above it decided or an exception group of it
settled (ch. 7.1, 7.9). Elsewhere the form is at its turn; a computation or
an equation without a condition outside a stack is at its turn on every
object it runs on.

**A displaced default is read** (card `readings-of-displaced-defaults`,
2026-10-27). A default that is not at its turn because a higher default of
its chain decided or settled is read as a default at its turn is: its
justification, with the object as its subject, on the fixpoint world, and
its consequence where the justification holds, each on the level that
declared it (ch. 7.10); an attribute default without a justification has
the reading of its consequence. Its status stays `DISPLACED` — it says
that the default was not at its turn — and its nodes say what it found
there: on an attribute whether its justification held and the value it
would have given, leeg included; on a characteristic whether its
justification held. Its decision, to attribute or not, stands on the wire
(`Default.decision`, IR-SPEC ch. 6.1) and is no node. This reading is the
speculative
evaluation of ch. 7.1, made once, on the fixpoint world. WF-051 folds every
read of every default of a group — its justification and its consequence,
recursively through its exception groups (ch. 7.9), and the reads of the
rules a stack folds in (ch. 7.10) — into the group's negative edges toward
its target, and a group whose defaults depend on its own output is
ill-formed (ch. 7.4), so the group decides above everything its defaults
read. What a displaced default reads at the fixpoint is therefore what it
would have read at its turn: the fixpoint world gives the reading the
speculation of ch. 7.1 would have made. A fault of its justification or its consequence — a date step out
of range, a context value without an evaluation date, an `Unresolved`
node — stands at its nodes and leaves the status `DISPLACED`: the session
never evaluated the default and has no such fault, as a contributor after
the stop of a quantifier fold carries a fault the session did not raise.
A §9.5 deposit is dropped, as in every reading.

Which defaults are read so: every default below the one that decides in
its chain, at the root or in an exception group, with every default inside
its exception groups; every default below one that settled by the conflict
of its exception groups (ch. 7.9: settling displaces); the content of a
default whose exception groups decided or conflicted — the fall-back that
did not come to its turn; an ordinary rule that ch. 7.10 folds into a
stack, below the default that decides; and the initialisation default
below the default that decides, where the input leaves its target empty.
Not so: an initialisation whose target the input or another writer fills
(the second row), a group-default and a characteristic default without a
justification (nothing to read, displaced or not), and a member of a
declared-unordered group of exception groups, which decides on its own
(ch. 7.2, 7.9) and is at its turn wherever the default that carries it is.

**A default on a timeline target** (card `defaults-per-period-on-timeline`,
2026-11-04). A default of a group on a timeline target decides per day
(ch. 7.11), and its reading is the reading per day. Its justification is read
per period, as the condition of a rule on a timeline target (ch. 3.10), each
of its nodes the part of all time in which it holds. Its consequence, where
the justification holds on at least one day, is read as a producer of
ch. 3.11: each node below its root the timeline it reads or computes, or its
value where it is a scalar, and the root the filled segments of the
consequence's timeline within the periods of the justification, the timeline
the default would write; where the justification holds on no day, the
consequence is not reached. A default without a justification, the
initialisation default among them, reads its consequence alone, its root the
whole filled timeline. Both are read over all time, whatever the days of the
default's turn. A day of its turn is a day on which no strictly higher default
of its chain settled (ch. 7.1, 7.9), within the days of the turn of the
default whose exception group its chain is; the days of the turn of the
content of a default with exception groups are those days of its turn on
which none of its exception groups settled; and those of the initialisation
default the days of its turn on which the input leaves its target empty. Its
status is `HOLDS` where it is at its turn on at least one day and `DISPLACED`
where it is on none, with the same nodes, read as at its turn (**A displaced
default is read**, above); the initialisation default whose input fills its
target on every day is `DISPLACED` without nodes, as on a scalar target. A
justification that faults leaves the status `FAULTS` where the default is at
its turn on some day and `DISPLACED` where it is on none, the fault at its
nodes. The status line names no days: the days of the turn follow from the
readings of the defaults above, the periods of each justification and the
filled segments of each consequence, by the rule of ch. 7.11.

Worked example `WV-5.12-default-per-dag` (absolute; `ref/test/` verbatim, the
golden `jutterswet-opgave-per-periode` of ch. 7.11). The defaults
`/rules/prioritizedDefaults/0/defaults/0`, the opgave, and `/1`, the
computation, each number node 0 `Compound` (the justification "altijd", a
composite without members, which reads no timeline and holds on every day)
and node 1 `AttributeReference` (the consequence); the initialisation default
of the stormtoelage, `/rules/prioritizedDefaults/1/defaults/2`, numbers node 0
`Literal`. Their readings on case 2 (J2, the opgave 10 on January and February
2025) and on a case J6 of the same document with the opgave 10 and the opgave
12 on all time and the input 7 on the stormtoelage on all time:

| form | J2 | J6 |
|------|----|----|
| `…/0/defaults/0` | `HOLDS`: `[[- - empty]]`, `[[2025-01-01 2025-03-01 10]]` | `HOLDS`: `[[- - empty]]`, `[[- - 10]]` |
| `…/0/defaults/1` | `HOLDS`: `[[- - empty]]`, `[[2025-03-01 2025-06-01 20]]` | `DISPLACED`: `[[- - empty]]`, `[[2025-03-01 2025-06-01 20]]` |
| `…/1/defaults/0` | `HOLDS`: `[[- - empty]]`, `[]` | `HOLDS`: `[[- - empty]]`, `[[- - 12]]` |
| `…/1/defaults/1` | `HOLDS`: `[[- - empty]]`, `[]` | `DISPLACED`: `[[- - empty]]`, `[[2025-10-01 2026-01-01 30]]` |
| `…/1/defaults/2` | `HOLDS`: `[[- - 0]]` | `DISPLACED`, no nodes |

On J2 the computation is at its turn on every day outside January and February
and reads the 20 it writes in spring; on J6 the opgave decides every day, so
the computation is at its turn on none and is read all the same. The
initialisation default of J6 has no nodes: the input fills the stormtoelage on
every day. In the canonical text J6 reads, for the strandloon:

```
/rules/prioritizedDefaults/0/defaults/0 "J6" 0 Compound [[- - empty]]
/rules/prioritizedDefaults/0/defaults/0 "J6" 1 AttributeReference [[- - 10]]
/rules/prioritizedDefaults/0/defaults/0 "J6" HOLDS
/rules/prioritizedDefaults/0/defaults/1 "J6" 0 Compound [[- - empty]]
/rules/prioritizedDefaults/0/defaults/1 "J6" 1 AttributeReference [[2025-03-01 2025-06-01 20]]
/rules/prioritizedDefaults/0/defaults/1 "J6" DISPLACED
```

On the document of `WV-7.11-uitzondering` with a Jutter J3 without kustwacht
and the basisloon 10 on 2025, the exception reads `0 CharacteristicCheck []`
and `1 Literal unreached`, `HOLDS`: its justification holds on no day, so its
consequence is not reached, and it is at its turn on every day
(`WV-5.12-default-per-dag-niet-gerechtvaardigd`). On the document of
`WV-7.11-verdrongen-conflict` the default of 30 and the members of its
exception groups are at their turn on every day outside April, which the
default of 60 settles, and each reads `HOLDS` with its periods
(`WV-5.12-default-per-dag-uitzonderingsgroepen`). On the golden
`jutterswet-gevolg-buiten-de-beurt` of ch. 7.11 (`WV-7.11-gevolg-buiten-de-beurt`),
J2 with kustwacht and winterwacht in April 2025: the kustwacht default is at
its turn outside April and justified in April alone, so its consequence is
read, and its read of the kustloon, which the case gives as the
asOfDate-bucket scalar, faults: `0 CharacteristicCheck [[2025-04-01
2025-05-01 empty]]`, `1 AttributeReference fault EF-037`, `HOLDS`, where the
evaluation raises nothing (`WV-5.12-default-per-dag-gevolg-faalt`). With the
consequence "zijn kustloon plus 1" the sum passes the fault on and its right
operand is not reached: `1 Binary fault EF-037`, `2 AttributeReference fault
EF-037`, `3 Literal unreached` (`-som`; the golden
`jutterswet-gevolg-buiten-de-beurt-som`, whose readings gate 17 holds on both
branches of the Drools backend). On the golden
`jutterswet-voorwaarde-buiten-de-beurt` a default "indien hij vrijgesteld is"
decides every day of a vrijgesteld J1, and the default below it, justified
by "zijn kustloon is groter dan 30" and "zijn kustloon is kleiner dan 100",
is at its turn on no day; the case gives J1's kustloon as the
asOfDate-bucket scalar. Its justification is read all the same, and faults
at the read: `0 Compound fault EF-037`, `1 Comparison fault EF-037`, `2
AttributeReference fault EF-037`, `3 Literal unreached` and every later node
`unreached`, `DISPLACED`, where the session raises nothing
(`WV-5.12-default-per-dag-voorwaarde-faalt`; gate 17 holds the readings on
both branches).

**A reading deposits nothing and writes nothing.** A §9.5 deposit the
evaluation makes is dropped, a value it computes is written nowhere, and no
outcome of the session reads a reading. Two consequences follow from the
definition and are not rules of their own: a reading of a condition that
the session evaluated has the session's verdict, since it is the same
evaluation on the same world; and a member that the session's short circuit
skipped is not reached in the reading either.

**The nodes.** The nodes of a form are numbered in the pre-order of the
document over the nodes of the condition union (IR-SPEC ch. 4.5) and of the
expression union (IR-SPEC ch. 4): first those of the condition, its root 0,
then those of the value; a node before its fields, the fields in the order
the document writes them, a list in its order. A form without a condition
has the nodes of its value only, its root 0. A text part, a whole-period
qualifier, a dimension selection, a type and a role binding are no nodes; a
fold's inline function (`Lambda`) is one. Each node that has a reading of
its own records one of:

- **not reached** — after the stop of a composite or a quantifier fold
  (ch. 5.10, 3.9a), in the branch of a choice that is not taken (ch. 10.2),
  in the value of a `Let` whose binder no read forces (ch. 10.6), in the
  value of a form whose condition does not hold, after a fault;
- a condition's **verdict**, true or false;
- an expression's **value**, a number with its unit (ch. 2.6); leeg is a
  value. A read of a leeg binder that ends the evaluation at the node
  (ch. 10.6, clause 2) reaches it and leaves it leeg;
- a **fault**: the evaluation fault the node raised or passed on, by the
  EF code of its kind (ch. 5.7b) and never by its sentence.

A node that the evaluation reads as a timeline, instead of evaluating it to
a value, has that timeline as its value, leeg where it is absent: the
operand of a whole-period test (ch. 3.9) and the base of a temporal total
(ch. 3.6) or of a time-proportional part (ch. 3.7); an asOfDate-bucket
scalar (ch. 8.4), which only the subject's own total without a clip reads,
is the timeline of its one period, and every other such node faults on it
(EF-037). A condition that the
evaluation reads over a timeline has the part of that timeline in which it
holds: the clip of a temporal total (ch. 3.6) and the filter of a
time-proportional part or a day count (ch. 3.7, 3.8) — the periods of a
characteristic, the segments of a compared timeline — and a period window
that cuts a timeline target (ch. 3.10), the window itself. A condition read
per period on a rule of a timeline target (ch. 3.10, the clip generalised)
holds where it does not fault, so the form holds, and each of its nodes has
the part of all time in which it holds: a check, a qualifier per bucket, a composite and a node
that reads no timeline its periods, each a period without a value, a
comparison the segments of its compared timeline there, with their values,
and the gaps where a leeg side satisfies it; its operands have the
timelines and values they read (`ref/` test
`WV-5.12-voorwaarde-per-periode`). A node of such a condition that faults,
or passes a fault on, carries the fault, as a node of a scalar condition
does: a read of a timeline that the case gives as the asOfDate-bucket
scalar faults (EF-037), the comparison and the composite above it pass the
fault on, and nothing after it is reached; the condition then faults, and
so does the form (`FAULTS`; for a default on a timeline target, **A default
on a timeline target**, below). The value of a
rule on a timeline target is the timeline it writes; under period
arithmetic (ch. 3.4, 3.5), with a condition or without one, each node
below the value's root has the timeline it reads or computes — an operand
read whole its timeline, `[]` where it is absent — or its value where it is
a scalar (`ref/` test `WV-5.12-maandloon`). Such a rule reads its producer
through every `Let` whose body reads its binder and nothing else ("Daarbij
geldt: de getelde dagen is …"). That `Let` stands for its bound value,
which it evaluates once, at that read (ch. 10.6), so the `Let`, the read of
its binder and the producer have one reading, the timeline the rule
writes: rule 2 of the golden `jutterswet-verzuimrente-30-360-afgerond`,
"de rentedagen … moet berekend worden als de getelde dagen gedurende de
tijd dat hij niet gedurende de gehele maand rentedragend is", reads on
case 1 node 1 `Let`, node 2 `BuiltinCall` (the day count) and node 7
`BinderRef` each `[[- 2025-11-01 0], [2025-11-01 2025-12-01 15],
[2026-03-01 2026-04-01 12], [2026-04-01 - 0]]` (`ref/` test
`WV-5.12-let-op-tijdlijndoel`). Of the clip of a
temporal total, a check of a characteristic has its true periods, a negated
check the periods outside them within each segment of the totalled
timeline, and a composite the intersection of its members' periods, each
member read on its own — each a period without a value; a comparison has
the segments of the compared timeline where it holds, with their values,
and, where a leeg side satisfies it, the gaps of the compared timeline
within the segments of the totalled timeline, each a period without a
value: a gap reads leeg (ch. 3.1), so "gelijk aan" holds on it against a
leeg bound and "ongelijk aan" against a filled one (ch. 1.2), and an
ordering never does; all of it cut where the segments of the totalled
timeline are cut, in the order of their days (the `ref/` tests
`WV-5.12-gedurende-clip` and `WV-5.12-peilwacht`, and the worked example
of a gap in a comparison clip, below).

**The object of a read** (card `reading-read-objects`, 2026-10-27). A
node that reads a member of one object names the object it read the member
on, where the evaluation reached it and it did not fault. Such a node is an
`AttributeReference` (IR-SPEC ch. 4.1a) — of the subject's own member
(ch. 5.8 rule 4) or over a path of roles, also where the evaluation reads it
as a timeline (the operand of a whole-period test, the base of a temporal
total or of a time-proportional part) —, a `DimensionCellRead`, which reads
one cell (ch. 5.8c), and
a `CharacteristicCheck` — of the subject's own characteristic, which a
`host` names in a rule's condition as well (ch. 5.8b), or over a role
(`navigation`), also under the whole-period qualifier. A check with a
narrowing host (ch. 5.8b rule 4) is a read of the subject's own
characteristic, on an object outside the host as well, and names no object. Of a member of its
own the object is the object of the reading; over a path, the object the
hops reached. Where a link on the path is absent there is no object: the
read is leeg (ch. 5.8 rule 1), the check holds in neither polarity
(rule 3), and the node names none. A node that was not reached, or that
faults (a to-many hop, rule 2), names no object. A `RoleMembership` (it
reads a relation, and its subject is the object of the reading), an
`IdentityCheck`, a `ParameterReference`, a `BinderRef`, a `ContextValue`
and the node of a fold over a set read no member of one object and name
none; a node without a reading of its own (above) has none to name, and the
contributors of a fold name their members already. The object is what the
evaluation read, not a second walk: an executor names the object its own
evaluation of the node reached.

**The cell.** A `DimensionCellRead` is the node of a cell read: it has the
value of the cell, leeg for an empty cell, and over a path of roles it
names the object the path reached, `on "<object>"` or `on none`, as an
`AttributeReference` over a path does. The `AttributeReference` under it has
no reading of its own and keeps its number: it names the attribute and the
object, and a dimensioned attribute has no value of its own (ch. 5.8c). The
node of a dimension fold has the value of the fold, and its argument has no
reading of its own either; the fold carries no contributors, since the
cells it reads are named by the labels of its selections and are no
members of a set (*provisional decision of the builder of card
`dimensions-executed`, 2026-11-02*). Worked example: on the case J1 of the
reading example of ch. 5.8c, the condition "indien zijn vondstgewicht in
voorjaar groter is dan 10 kg" of a rule on the `Jutter` reads `HOLDS`, its
node 1, the `DimensionCellRead`, the value `12.5 "kg"`, its node 2, the
`AttributeReference` under the cell, no line, and its node 3, the literal,
`10 "kg"`; on J2, with the cell `najaar` 4.00 alone, it reads `FAILS`, node 1
`empty "kg"`. The sum of `vondstgewicht` over all seasons, the value of a
rule without a condition, reads on J1 its node 0, the `Aggregation`,
`16.5 "kg"`, and its node 1, the argument, no line. The `ref/test` row
`WV-5.12-cel` pins both. Over a path, on the example "Reading over a path"
of ch. 5.8c, the cell read of P1 reads `12.5 "kg" on "J1"` and that of P2
`empty "kg" on none`, while the fold over the path names no object
(`WV-5.12-cel-over-een-pad`). A rule whose target is a cell is read as a
rule on an attribute, and its initialisation is `DISPLACED` where its cell
holds a value of the input or of another writer: on the case J1, J2 of the
writing example of ch. 5.8c, the rule on `najaar` reads `HOLDS` on both, its
node 0, the product, `3 "kg"`, node 1, the cell `voorjaar`, `1.5 "kg"`, and
node 3, the literal, `2`; the initialisation of `zomer` reads `HOLDS` on J1
with its node 0, the literal, `0 "kg"`, and `DISPLACED` on J2, whose cell
`zomer` the input fills (`WV-5.12-celdoel`).

**Folds over a set.** A node that the evaluation reads once per member of
a fold over a set — the source, the filter and the matrix of an
aggregation with a range or over an instance-set union (ch. 3.4, 3.4a,
3.4d) and of a quantifier fold (ch. 3.9a) — and a node read once per
element of a list (ch. 10.2) has no reading of its own; it keeps its
number. A node with the form anchor there (ch. 3.4a) is read per member too
and has none either, although its value does not depend on the member (card
`subselection-reads-subject`, 2026-10-23, B7). The node of a fold over a set
carries its **contributors** instead,
the members of its range that pass its filter: for a count each instance it
counts, for the filled count (ch. 3.4c) each member whose value is filled;
for a value fold each member with the value it contributes, where a
member whose value is leeg contributes nothing (ch. 1.2e); for the sum per
period over a set (ch. 3.5) each member with the timeline it contributes,
where a member without a timeline contributes nothing; for a quantifier
fold each member with the verdict of the matrix on it, or its fault — the
table of ch. 3.9a's `test(m)` over the contributors, evaluated on the
fixpoint world as a reading evaluates, depositing nothing — and whether the
evaluation reached it: the contributors after the stop (ch. 5.10 over the
contributors) and after a fault are not reached. Their verdict or fault is
the reading's, not the session's: the session raised that fault and made
that deposit no more than it read the contributor, and the status of the
reading does not change by it. Where the evaluation of a fold faults before
its contributors are settled — its filter faults, or the value of one of
its members — the node carries no contributors. An executor whose
evaluation keeps no order among the contributors of a quantifier fold whose
matrix can neither fault nor deposit (ch. 5.11: an invisible order) writes
`unordered` in place of the position; it reports no position for that fold.
A quantifier fold read per member or per element (above) has no reading of
its own: its table is written nowhere, and an executor need not evaluate
its contributors after the stop. It is the one place where a quantifier
fold reads a binder of an enclosing `Let`, since WF-077 keeps a quantifier
fold out of the condition of an `IfThenElse`; the reading then reads the
binder only where the evaluation read it, and the nodes of the `Let`'s
value are reached exactly where the evaluation forced the binder
(ch. 10.6). The golden `jutterswet-grens-per-jutter` pins it: its second
case stops the folds of two vondsten after a member that forced the
binder. In `ref/` test `WV-5.12-binder-na-de-stop` a fold that stops on
the empty prefix forces nothing, and the node of the binder's value reads
`unreached`, with a filled and with a leeg value.

**The canonical text.** The readings of a case are written one line per
reading, `<form> <object> <status>`, and one line per node with a reading
of its own, `<form> <object> <number> <kind> <found>`, all lines sorted in
code-unit order; the kind is the node's discriminator as the wire spells
it. An object of the input is written as its case label, quoted; an object
a creation made as `«"<creation>"»(<subject>)#<k>`, the name of its
creation, the object it was made for and its number among the objects that
creation made for it, which is 1, since a creation makes one object per
subject (ch. 5.5). `<found>` is `unreached`; `true` or `false`; `fault`
with the EF code, or `fault -` for a fault without one; or the value: a
number as its exact decimal without trailing zeros where it terminates, and
else as its numerator and denominator in lowest terms, `t/n`, the sign on the
numerator (`-1/3`), so that a rational and a decimal of many digits never
write alike; a date and a date-time as the case format writes them,
and a year the case format cannot write — below 0000 or above 9999, which
the date construction reaches (ch. 3.8) — with its sign and at least four
digits, `+12026-03-01`, `-0005-03-01` (ISO 8601's expanded form;
*provisional decision of the builder of card date-with-in-ref-and-clingo,
2026-10-22*: the Drools backend writes it so, `ref` wrote `12026-03-01`
and `-005-03-01` until then, and the golden `date-with-parts` reads one); a
text and an enumeration value quoted, with `\`, `"` and the line breaks
escaped; a constructor value (ch. 9.1) as its tag quoted and, for a unary
constructor, its payload written as a value is, in parentheses:
`"onbekend"`, `"waarde"(300)`; leeg as `empty`; a timeline as `[[<from> <to> <value>], …]`, with
`empty` as the value of a period without one. After the value of an
expression stands its unit, quoted, where it has one: of a number the unit
of the node's effective type (ch. 2.10), the number restated into it where
the evaluation computed it in another unit or in none — a leeg coalesced to
0, a fold's 0 over no contributor, a sum whose left operand was leeg
(ch. 2.6) —, of a leeg value or a fault the unit of the node's declaration
or annotation (*provisional decision of the builder of card
readings-of-unconditional-forms, 2026-10-22*: until then the text said
"the unit it carries", and `ref` wrote the unit a value carried in its own
evaluation, where the Drools backend, which computes in the effective unit,
wrote that one; the two first met on the values of forms without a
condition). After the value of a node that reads a member over a path of
roles, and its unit, stands `on` and the object the path reached, named as
the object of a reading is, or `on none` where a link on the path is
absent:
`<form> <object> <number> <kind> <found> ["<unit>"] on <object>|none`. A
node that reads a member of its own writes no object, since the object of
the reading stands at the head of its line (ch. 5.8 rule 4), and neither
does a node that was not reached or that faulted (card
`reading-read-objects`, 2026-10-27). After the node of a fold over a set
stand its contributors,
`contributors [<object> <value|verdict|fault> reached|unreached; …]`,
sorted, each found written as at a node, and `unordered` in place of the
position where the executor keeps no order. `runner --trace` prints this
text for a golden case, each line after `reading `.

**Worked example** (absolute; the `jutterswet-ploegaandeel` golden of
ch. 5.10, verbatim in the `ref/` test `WV-5.12-ploegaandeel`). The rule

```
bergingsaandeel ← 1  indien  ALLE [
    ploegomvang ongelijk aan 0 ;
    ⌊vangstwaarde ÷ ploegomvang⌋ groter dan 0 ]
```

numbers its nodes 0 `Compound`, 1 `Comparison` (member 1), 2
`AttributeReference` (ploegomvang), 3 `Literal` (0), 4 `Comparison`
(member 2), 5 `Rounding`, 6 `Binary` (÷), 7 `AttributeReference`
(vangstwaarde), 8 `AttributeReference` (ploegomvang), 9 `Literal` (0),
and, after the condition, 10 `Literal` (the value 1). The version is
`/rules/rules/0/versions/0`; its readings on the three Jutters of the case:

| node | J1 (ploegomvang 0, vangstwaarde 100) | J2 (4, 100) | J3 (7, 3) |
|------|----|----|----|
| status | `FAILS` | `HOLDS` | `FAILS` |
| 0 `Compound` | false | true | false |
| 1 `Comparison` | false | true | true |
| 2 `AttributeReference` | 0 | 4 | 7 |
| 3 `Literal` | 0 | 0 | 0 |
| 4 `Comparison` | *unreached* | true | false |
| 5 `Rounding` | *unreached* | 25 | 0 |
| 6 `Binary` | *unreached* | 25 | 3/7 |
| 7 `AttributeReference` | *unreached* | 100 | 3 |
| 8 `AttributeReference` | *unreached* | 4 | 7 |
| 9 `Literal` | *unreached* | 0 | 0 |
| 10 `Literal` | *unreached* | 1 | *unreached* |

On J1 the composite stops after member 1 (ch. 5.10, `all`, the first member
that does not hold), so member 2 and the division in it are not reached,
and neither is the value, since the condition fails. On J3 both members are
read and member 2 decides; the value is not reached. In the canonical text
J1 reads:

```
/rules/rules/0/versions/0 "J1" 0 Compound false
/rules/rules/0/versions/0 "J1" 1 Comparison false
/rules/rules/0/versions/0 "J1" 10 Literal unreached
/rules/rules/0/versions/0 "J1" 2 AttributeReference 0
/rules/rules/0/versions/0 "J1" 3 Literal 0
/rules/rules/0/versions/0 "J1" 4 Comparison unreached
/rules/rules/0/versions/0 "J1" 5 Rounding unreached
/rules/rules/0/versions/0 "J1" 6 Binary unreached
/rules/rules/0/versions/0 "J1" 7 AttributeReference unreached
/rules/rules/0/versions/0 "J1" 8 AttributeReference unreached
/rules/rules/0/versions/0 "J1" 9 Literal unreached
/rules/rules/0/versions/0 "J1" FAILS
```

**Worked example — a quantifier fold** (absolute; the
`jutterswet-keurgemiddelde` golden, verbatim in the `ref/` test
`WV-5.12-keurgemiddelde`). The rule

```
inzetbaar  indien  ALLE wachten van de Ploeg voldoen aan ALLE [
    hij is gekeurd ;
    ⌊keurpunten ÷ keuringen⌋ groter dan 6 ]
```

numbers its nodes 0 `QuantifierFold`, 1 `Compound`, 2
`CharacteristicCheck`, 3 `Comparison`, 4 `Rounding`, 5 `Binary`, 6 and 7
`AttributeReference`, 8 `Literal`; all but node 0 are the matrix, read per
member, with no reading of their own. A member with 0 keuringen divides by
a numeric zero, which deposits a fact of kind EF-005 where the evaluation
reaches it (ch. 1.4 point 4). The readings of `/rules/rules/0/versions/0`
on the Ploeg P1 of the four cases:

| case | the wachten in order (gekeurd, keurpunten / keuringen) | status | contributors | channel |
|------|----|----|----|----|
| 1 | W1 (no, 70 / 10), W2 (yes, 30 / 0) | `FAILS` | `"W1" false reached; "W2" false unreached` | consistent |
| 2 | W1 (yes, 70 / 10), W2 (yes, 80 / 10) | `HOLDS` | `"W1" true reached; "W2" true reached` | consistent |
| 3 | W1 (yes, 70 / 10), W2 (yes, 50 / 10), W3 (yes, 30 / 0) | `FAILS` | `"W1" true reached; "W2" false reached; "W3" false unreached` | consistent |
| 4 | W1 (yes, 70 / 10), W2 (yes, 30 / 0) | `FAILS` | `"W1" true reached; "W2" false reached` | EF-005 on P1 |

In case 1 the fold stops after W1 (`all`, the first contributor that does
not hold) and in case 3 after W2, so the session never divides by W2's and
W3's zero and its channel stays consistent; the reading evaluates the
matrix on them all the same, drops the deposit, and records the verdict
false, not reached. In case 4 the session reaches W2 and deposits; the
reading drops that deposit too and records W2 reached. In the canonical
text case 3 reads:

```
/rules/rules/0/versions/0 "P1" 0 QuantifierFold false contributors ["W1" true reached; "W2" false reached; "W3" false unreached]
/rules/rules/0/versions/0 "P1" FAILS
/rules/rules/1/versions/0 "P1" 0 Aggregation 3 contributors ["W1" empty reached; "W2" empty reached; "W3" empty reached]
/rules/rules/1/versions/0 "P1" HOLDS
```

The golden's second rule, `wachttal van een ploeg`, the count of the
wachten, has no condition: it holds, and the count carries each wacht it
counts (**A form without a condition**, above).

**Worked example — a form without a condition** (absolute; the
`jutterswet-opgave-bedrag` golden, verbatim in the `ref/` test
`WV-5.12-opgave-bedrag`). The rule

```
verzekerde waarde ← waarde plus 2,50 €
```

has no condition and numbers the nodes of its value from 0: 0 `Binary`
(plus), 1 `AttributeReference` (waarde), 2 `Literal` (2,50 €). Its version
is `/rules/rules/2/versions/0`. The waarde carries the opgave marker, so one
defaults group decides it: from the top the opgave, the computation of the
waarde from the taxatie and the bergingskosten, and the initialisation
default 25,00 € without a justification,
`/rules/prioritizedDefaults/0/defaults/2`, whose one node, 0, is its
`Literal`. The readings of the two forms on the Vondst V of three cases:

| case | input | waarde | `…/defaults/2` | `/rules/rules/2/versions/0`: nodes 0, 1, 2 |
|------|----|----|----|----|
| `case1-geen-opgave` | taxatie 100,00 €, bergingskosten 10,00 € | 110,00 € (the computation) | `DISPLACED`: 25,00 € | `HOLDS`: 112,50 €, 110,00 €, 2,50 € |
| `case5-niets` | — | 25,00 € (the initialisation default) | `HOLDS`: 25,00 € | `HOLDS`: 27,50 €, 25,00 €, 2,50 € |
| `case7-doel-zelf-zonder-feiten` | waarde 80,00 € | 80,00 € (the input) | `DISPLACED` | `HOLDS`: 82,50 €, 80,00 €, 2,50 € |

In case 1 the computation above the initialisation default decided
(ch. 7.1), and the default is read all the same (**A displaced default is
read**, above: it would have given 25,00 €); in case 7 the input fills the
target (ch. 7.3), and it has nothing to read. In the canonical
text the two forms read in case 5:

```
/rules/prioritizedDefaults/0/defaults/2 "V" 0 Literal 25 "EUR"
/rules/prioritizedDefaults/0/defaults/2 "V" HOLDS
/rules/rules/2/versions/0 "V" 0 Binary 27.5 "EUR"
/rules/rules/2/versions/0 "V" 1 AttributeReference 25 "EUR"
/rules/rules/2/versions/0 "V" 2 Literal 2.5 "EUR"
/rules/rules/2/versions/0 "V" HOLDS
```

**Worked example — the object of a read** (absolute; the
`jutterswet-kustwacht` anchor of ch. 5.8, case 2, verbatim in the `ref/`
test `WV-5.12-kustwacht`). V1 (gevarenklasse 5, verraderlijk) is the
kustvak of post P1 (hoogte 12); post P3 (hoogte 9) lies in no kustvak; W1
mans P1, W4 mans P3, W3 mans nothing. Every node of these six forms reads
over a path, and has number 0: the three rules on an attribute have no
condition and read their value, the three on a characteristic their
condition. Per object, what the node found and the object it read on:

| form | W1 | W3 | W4 | P1 | P3 |
|------|----|----|----|----|----|
| `/rules/rules/0/versions/0` uitkijkbonus ← de hoogte van zijn post | 12 on P1 | leeg, none | 9 on P3 | | |
| `/rules/rules/1/versions/0` risicotoeslag ← de gevarenklasse van het kustvak van zijn post | 5 on V1 | leeg, none | leeg, none | | |
| `/rules/rules/2/versions/0` vakklasse ← de gevarenklasse van het kustvak | | | | 5 on V1 | leeg, none |
| `/rules/rules/3/versions/0` risicopost indien zijn kustvak verraderlijk is | | | | true on V1 | false, none |
| `/rules/rules/4/versions/0` gevarenwacht indien zijn post risicopost is | true on P1 | false, none | false on P3 | | |
| `/rules/rules/5/versions/0` rustwacht indien zijn post geen risicopost is | false on P1 | false, none | true on P3 | | |

W3's paths meet the absent link at their first step, W4's path of two
steps at its second. Rule 5 makes rule 3 of ch. 5.8 readable in the reading
itself: W3 and W4 both fail rule 4, but in rule 5 they differ, and the
object says why — W4 read P3, which is no risicopost, while W3 read
nothing, and the negation does not resurrect an absent link. In the
canonical text W3 and W4 read, on rules 1 and 5:

```
/rules/rules/1/versions/0 "W3" 0 AttributeReference empty on none
/rules/rules/1/versions/0 "W3" HOLDS
/rules/rules/1/versions/0 "W4" 0 AttributeReference empty on none
/rules/rules/1/versions/0 "W4" HOLDS
/rules/rules/5/versions/0 "W3" 0 CharacteristicCheck false on none
/rules/rules/5/versions/0 "W3" FAILS
/rules/rules/5/versions/0 "W4" 0 CharacteristicCheck true on "P3"
/rules/rules/5/versions/0 "W4" HOLDS
```

The three worked examples above read only members of their own subject:
their lines carry no object.

**Worked example — a displaced default** (absolute; the
`jutterswet-nestuitzondering` anchor of ch. 7.9, cases 1 and 9, verbatim
in the `ref/` test `WV-5.12-nestuitzondering-verdrongen`). One defaults
group on `Jutter.heffing`: default 0 is a form-a default `grondX → 40.00`
with the unordered exception groups `grondY → 10.00`, `grondZ → 20.00` and
`grondW → 10.00`; default 1 is a group-default, which has no reading of its
own, with the members `grondB1 → 15.00` and `grondB2 → 25.00`. Each
default reads its justification, node 0 `CharacteristicCheck`, and its
consequence, node 1 `Literal`. With P for
`/rules/prioritizedDefaults/0/defaults`, the readings on the Jutter M:

| form | case 1 (grondX, grondY): heffing 10.00 | case 9 (grondX, grondB1, grondB2): heffing 40.00 |
|------|----|----|
| `P/0`, grondX → 40.00 | `DISPLACED`: true, 40 | `HOLDS`: true, 40 |
| `P/0/exceptions/members/0/defaults/0`, grondY → 10.00 | `HOLDS`: true, 10 | `FAILS`: false, *unreached* |
| `P/0/exceptions/members/1/defaults/0`, grondZ → 20.00 | `FAILS`: false, *unreached* | `FAILS`: false, *unreached* |
| `P/0/exceptions/members/2/defaults/0`, grondW → 10.00 | `FAILS`: false, *unreached* | `FAILS`: false, *unreached* |
| `P/1/exceptions/members/0/defaults/0`, grondB1 → 15.00 | `DISPLACED`: false, *unreached* | `DISPLACED`: true, 15 |
| `P/1/exceptions/members/1/defaults/0`, grondB2 → 25.00 | `DISPLACED`: false, *unreached* | `DISPLACED`: true, 25 |

In case 1 the exception group of grondY decides 10.00 for default 0, so
the content of default 0, its fall-back, was not at its turn: it held and
would have given 40. grondZ and grondW are members beside the group that
decided, at their turn, and fail. Default 1 is below the default that
decides, and its members are displaced; neither held. In case 9 the
exception groups decide nothing and default 0 falls back to 40.00; the two
members of default 1 are displaced, and both held — the 15 and the 25
whose conflict at `Jutter.heffing/1` the session never reached (ch. 7.9).
In the canonical text case 1 reads, for default 0 and the first member of
default 1:

```
/rules/prioritizedDefaults/0/defaults/0 "M" 0 CharacteristicCheck true
/rules/prioritizedDefaults/0/defaults/0 "M" 1 Literal 40
/rules/prioritizedDefaults/0/defaults/0 "M" DISPLACED
/rules/prioritizedDefaults/0/defaults/1/exceptions/members/0/defaults/0 "M" 0 CharacteristicCheck false
/rules/prioritizedDefaults/0/defaults/1/exceptions/members/0/defaults/0 "M" 1 Literal unreached
/rules/prioritizedDefaults/0/defaults/1/exceptions/members/0/defaults/0 "M" DISPLACED
```

**Worked example — the initialisation default** (absolute; the
`jutterswet-vondstwaardering` anchor of ch. 7.6, all six cases, verbatim in
the `ref/` test `WV-5.12-vondstwaardering-verdrongen`). The chain on
`Jutter.waardering`: default 0 `erkend → zijn basisbedrag`, default 1
`kandidaat → zijn kandidaatbedrag`, default 2 the initialisation default
25. Nodes 0 and 1 of defaults 0 and 1 are the `CharacteristicCheck` and the
`AttributeReference`; node 0 of default 2 is its `Literal`.

| case | input | waardering | default 0 | default 1 | default 2 |
|------|----|----|----|----|----|
| `case1-erkend` | erkend, basisbedrag 50 | 50 | `HOLDS`: true, 50 | `DISPLACED`: false, *unreached* | `DISPLACED`: 25 |
| `case2-doorval` | erkend, kandidaat, kandidaatbedrag 30 | 30 | `HOLDS`: true, leeg | `HOLDS`: true, 30 | `DISPLACED`: 25 |
| `case3-doorval-init` | erkend | 25 | `HOLDS`: true, leeg | `FAILS`: false, *unreached* | `HOLDS`: 25 |
| `case4-init` | — | 25 | `FAILS`: false, *unreached* | `FAILS`: false, *unreached* | `HOLDS`: 25 |
| `case5-input-blijft` | waardering 40 | 40 | `FAILS`: false, *unreached* | `FAILS`: false, *unreached* | `DISPLACED`, no nodes |
| `case6-default-overschrijft` | erkend, basisbedrag 50, waardering 40 | 50 | `HOLDS`: true, 50 | `DISPLACED`: false, *unreached* | `DISPLACED`, no nodes |

In cases 1 and 2 a higher default decided and the input leaves the
waardering empty: the initialisation default is read, and would have given
25. In case 6 a higher default decided as well, but the input fills the
target: the emptiness of the target in the input is the justification of
the initialisation default (ch. 7.3), not the turn of its chain, so it has
no nodes, as in case 5.

**Worked example — a gap in a comparison clip** (absolute; the
`jutterswet-peilwacht` golden of ch. 3.6, cases 1, 2 and 4, verbatim in
the `ref/` test `WV-5.12-peilwacht`). Two rules without a condition on the
`Jutter`, `/rules/rules/0/versions/0` the peilsom, "het totaal van zijn
wachtvergoeding per maand gedurende de tijd dat zijn peilstand gelijk is
aan zijn drempelstand", and `/rules/rules/1/versions/0` the afwijksom, the
same with "ongelijk aan". Each numbers its nodes 0 `Aggregation`, 1
`AttributeReference` (the wachtvergoeding per maand, the totalled
timeline), 2 `Comparison`, 3 `AttributeReference` (the peilstand, the
compared timeline) and 4 `AttributeReference` (the drempelstand, the
bound). Every Jutter has a wachtvergoeding per maand of 10 from 2024-01-01
to 2024-07-01, one segment. Node 2 and the total, node 0, per Jutter:

| case, Jutter | peilstand | drempelstand | peilsom: node 2 | node 0 | afwijksom: node 2 | node 0 |
|----|----|----|----|----|----|----|
| 1, J1 | 5 in January | leeg | `[[2024-02-01 2024-07-01 empty]]` | 50 | `[[2024-01-01 2024-02-01 5]]` | 10 |
| 1, J2 | — | leeg | `[[2024-01-01 2024-07-01 empty]]` | 60 | `[]` | 0 |
| 2, J1 | 5 in January | 5 | `[[2024-01-01 2024-02-01 5]]` | 10 | `[[2024-02-01 2024-07-01 empty]]` | 50 |
| 2, J2 | — | 5 | `[]` | 0 | `[[2024-01-01 2024-07-01 empty]]` | 60 |
| 2, J3 | 5 in January, 7 in February and March | 5 | `[[2024-01-01 2024-02-01 5]]` | 10 | `[[2024-02-01 2024-04-01 7], [2024-04-01 2024-07-01 empty]]` | 50 |
| 4, J1 | 7 from 2024-03-01 to 2024-09-01 | 5 | `[]` | 0 | `[[2024-01-01 2024-03-01 empty], [2024-03-01 2024-07-01 7], [2024-07-01 2024-09-01 7]]` | 60 |
| 4, J2 | 7 from 2024-03-01 to 2024-09-01 | leeg | `[[2024-01-01 2024-03-01 empty]]` | 20 | `[[2024-03-01 2024-07-01 7], [2024-07-01 2024-09-01 7]]` | 40 |

In case 1 the peilstand of J1 has a gap from February to June, where it and
the leeg drempelstand are both leeg: "gelijk aan" holds there, and the
peilsom totals those five months to 50; the comparison reads that gap,
where the clip holds, and not only the segments of the peilstand, none of
which is equal to a leeg bound. Against the drempelstand 5 of case 2 the
same gap satisfies "ongelijk aan" instead. A gap is read only within the
segments of the wachtvergoeding: J2 of case 1 has no peilstand at all, and
its gap is the segment of the wachtvergoeding. A segment of the peilstand
is read where it satisfies the comparison, also beyond the wachtvergoeding,
cut where the wachtvergoeding ends: J1 and J2 of case 4 read the peilstand
from July to September, which the clip does not total. In the canonical
text J1 of case 4 reads, for the afwijksom, the gap before the segments:

```
/rules/rules/1/versions/0 "J1" 0 Aggregation 60
/rules/rules/1/versions/0 "J1" 1 AttributeReference [[2024-01-01 2024-07-01 10]]
/rules/rules/1/versions/0 "J1" 2 Comparison [[2024-01-01 2024-03-01 empty], [2024-03-01 2024-07-01 7], [2024-07-01 2024-09-01 7]]
/rules/rules/1/versions/0 "J1" 3 AttributeReference [[2024-03-01 2024-09-01 7]]
/rules/rules/1/versions/0 "J1" 4 AttributeReference 5
/rules/rules/1/versions/0 "J1" HOLDS
```

and J1 of case 1, for the peilsom, the gap alone:

```
/rules/rules/0/versions/0 "J1" 2 Comparison [[2024-02-01 2024-07-01 empty]]
```

**The artifacts.** The `ref/` tests `WV-5.12-ploegaandeel`,
`WV-5.12-keurgemiddelde` and `WV-5.12-opgave-bedrag` (the three examples
above),
`WV-5.12-fout-in-een-lezing` (a member that faults: the fault's EF code at
the member and at the composite, the status `FAULTS`, and the composite on
the other object stopping before it) and `WV-5.12-fout-in-een-kwantorvouw`
(the fault arms of the table: a contributor after the stop whose matrix
faults carries the fault, not reached, and the fold keeps its verdict; a
contributor after a reached one that faults carries its verdict, not
reached; a filter that faults leaves the node without contributors) and
`WV-5.12-gedurende-clip` (the clip of a temporal total: a check, a negated
check, a composite and a comparison of another timeline), the last two
also the Drools backend's `ReadingsWitnessTest`, beside its record of forms
without a condition (a sum over a role with its contributors and an empty
one, a `gesteld worden op` one role away, an initialisation at its turn and
displaced, an initialisation default at its turn and displaced by a higher
default and by the input). `WV-5.12-peilwacht` (the example of a gap in a
comparison clip above) pins the gaps of a comparison clip in `ref/`, and
the backend's `ReadingsWitnessTest` the same readings of the comparison on
both its branches. `WV-5.12-maandloon` (the golden `jutterswet-maandloon`,
period arithmetic without a condition: the timeline of each node, and the
contributors of the sum per period, a member without a timeline among
them) pins the reading of period arithmetic in `ref/`, and the backend's
`TmsPeriodArithmeticWitnessTest` the same lines on both its branches. Gate 17
(`scripts/check-readings.sh`) holds the readings of `runner --trace`, per
golden case file, to those of the Drools backend on both its branches: per
contributor the member and its verdict, value or fault verbatim, the
position where both write one; and where the backend writes `unordered`, no
contributor of the trace may carry a fault. The object of a read stands in
the line of its node, so the gate holds it verbatim. `WV-5.12-kustwacht`
(the example above) and `WV-5.12-gelezen-objecten` (a member of the
subject's own; a path of one step in a condition and of two as the value of
a computation; an absent link at the first step and at the second; a check
over a role of an input characteristic and of a derived one; a read in the
value of a computation with a condition, not reached where the condition
fails; and a path over the relation a fact creation lays) pin it in `ref/`,
the latter also in the Drools backend's `ReadingsWitnessTest`, whose
records name the object of a member of the subject's own as well; and the
backend's `ReadingsCorpusWitnessTest` holds the object of every node of
every case the backend plays to the object the node's walk reaches on the
written-back beans. `WV-5.12-nestuitzondering-verdrongen` and
`WV-5.12-vondstwaardering-verdrongen` (the two examples above) and
`WV-5.12-fout-in-een-verdrongen-default` (a justification of a displaced
default that faults: the fault at its nodes, the status `DISPLACED`) pin
the reading of a displaced default in `ref/`, and
`WV-5.12-default-per-dag` with its three rows the reading of a default on a
timeline target, which the Drools backend's `TmsDefaultsPerDayWitnessTest`
holds on both its branches as well; the last also in the Drools
backend's `ReadingsWitnessTest`, beside its records of displaced defaults:
an attribute default that held and gave a value, one that held and gave
none, one that did not hold, a characteristic default, a default in an
exception group below a displaced default, the content of a default whose
exception group decided, a subtype's rule that ch. 7.10 folds into a stack
below the inherited exception that decides, and the initialisation default
below the default that decides, read where the input leaves its target
empty and without nodes where the input fills it. The backend's
`ReadingsCorpusWitnessTest` holds every displaced default of every case it
plays to the nodes of a reading at its turn, and every operand in them to
the value of the fixpoint; gate 17 compares their node lines as it
compares every other, and counts them.

A cell is inside that differential: gate 17 compares the readings of the
goldens that read or write one (ch. 5.8c) on both branches, the cell read
and the dimension fold as the nodes, a rule whose target is a cell as a rule
on an attribute, the object a path reaches for a cell read over it
(`jutterswet-ploegseizoen`; `WV-5.12-cel-over-een-pad` in `ref`) and none
for the subject's own cell or for a dimension fold, and the contributors of
a fold of one cell of each member.

Three boundaries, recorded rather than left implicit. The status `FAULTS`
and a fault at a node stay outside that differential: a golden does not
fault at its fixpoint, and `runner --trace` refuses a case that pins one.
A fault at the nodes of a default stands in a golden only where the session
did not evaluate the node: the consequence of a default on a timeline target
outside the days of its turn (`jutterswet-gevolg-buiten-de-beurt`, `-som`)
and the justification of a default at its turn on no day
(`jutterswet-voorwaarde-buiten-de-beurt`), each inside the differential; the
fault at the nodes of a displaced default on a scalar target is pinned by
`WV-5.12-fout-in-een-verdrongen-default` and its twin in
`ReadingsWitnessTest` only.
A fault of a contributor after the stop is inside it for one kind: the
golden `jutterswet-keurtermijn` (card
`clingo-day-kind-walk-after-fold-stop`, 2026-10-21) reads `"W2" fault
EF-009 unreached` and `"W3" fault EF-009 unreached` on both sides, the
day-kind walk of a member after the stop. The other kinds stay outside
it: no golden carries such a fault after the stop. The ASP backend raises
the faults of an evaluation date the case does not carry (EF-011) and of an
`Unresolved` node (EF-019) where the evaluation reaches their node, as ref
does (card `early-faults-and-sum-reading`, 2026-11-04), and the Drools
backend reads a parameter the case does not bind (EF-014) as leeg, so its
reading would carry a verdict where ref's carries the fault. Those arms are
pinned by the executors' own tests above. And a verdict of a
contributor the session did not reach may rest on a deposit the reading
dropped, as W2 in case 1 and W3 in case 3 of the second example do: a
consumer that reads the table reads a verdict the session would have
accompanied by a channel fact, had it read the contributor.

---

## 6. Stratification

> **Normative home:** the dependency graph, its three edge kinds, the
> illegal-cycle rules (WF-045/046) and the WF-051 defaults fold are defined
> normatively in `IR-SPEC.md` ch. 5 (with the day-kind stratum-0 framing,
> `IR-SPEC.md` ch. 5.4, and the executor-neutrality clause, ch. 5.5). This
> chapter is the **operational companion**: the per-form read/write table
> (ch. 6.1) and the worked evaluations (ch. 6.4) that realise those
> definitions.

A rule set holds as one system of equations (ch. 5); this chapter
states the well-formedness that makes its fixpoint well-defined in the
presence of negation and creation: the dependency graph, acyclicity
(WF-045), and stratified negation (WF-046). This is the declared
replacement for v0's single agenda boundary — the "salience −1" of the
founding review (ch. 5.3): stratification is a property of the IR,
checked by the verifier, never an engine agenda trick. The ASP hand
exercises (`ASP-TRANSLATION.md` ch. 6) read the same strata off the
statute text.

### 6.1 The dependency graph

Value nodes, identified up to name normalisation:

- **attribute** — object type × attribute;
- **characteristic** — object type × characteristic;
- **channel** — rule name (version-stripped): the §9.5 error entries the
  named rule can deposit (ch. 5.6);
- **population** — object type: the instance set an instance fold
  (`IR-SPEC.md` ch. 4.3) or an allocation's recipient derivation reads,
  and object creation grows.
- **relation** — declared fact type: its link set — written by the fact
  creations that create it (ch. 5.5), read by every symbolic hop that
  traverses it, by every fold or allocation that names it in a role
  binding (from either end; the link is written on both) and by every
  role check that names it (ch. 5.8a).

Each rule form reads and writes nodes:

| Form | Writes | Reads |
|---|---|---|
| assignment / initialisation | its target attribute | every attribute and characteristic its value expression and condition reference — a navigated reference reads the referenced type's node; population(element type) per instance fold |
| characteristic attribution | its target characteristic | idem |
| allocation | target attribute; declared remainder attribute | source amount; ratio, maximum and ordering attributes; condition reads; population(target type) |
| consistency / uniqueness | channel(own name) | assertion operands / key attributes; a union reads every source's key attributes; a scoped uniqueness also reads relation(scope hop), named by its `scope` binding (`scopeRol` until L4; since L2 the only spelling of the scope, B8; until L2 a scope without a binding was derived from the written scope field) |
| object creation | the attributes and characteristics its `assignments` set on the created type; population(created type); relation(the uniquely-derivable connecting fact type — the back-link, ch. 5.5) | condition and toekenning-value reads on the anchor; population(subject type) — iterating the subjects is a read: it orders creation-after-creation, and a self-typed creation becomes a WF-045 cycle instead of a diverging fixpoint or an unbounded grounding |
| relation creation | relation(created fact type) — no population: it mints no instance, it links instances that exist (since 2026-10-06; below) | condition reads on the anchor; relation(each hop of the join-chain traversal, `IR-SPEC.md` ch. 3.4) |

Edges run producer → consumer — from every form that writes a node to
every form that reads it — in three kinds:

- **positive**, the default: reads by attribute-writing forms
  (assignment, initialisation, allocation), and positive characteristic
  reads anywhere. Attribute writes are revocable — idempotent and
  re-evaluated under freshness (ch. 5.1) — so a transient self-corrects
  within a stratum, and a positive characteristic read is safe there: a
  characteristic holds at the fixpoint only where an attribution that
  grants it decides (ch. 5.3), and an attribution reads every attribute
  stratum-strictly (below).
- **stratum-strict**: any non-characteristic read (attribute values,
  comparisons; the leeg/gevuld predicates read negatively, below) by a
  characteristic attribution, a consistency or uniqueness, or a creation.
  Such a form must evaluate strictly above the producers of what it
  reads. A consistency or uniqueness (channel facts are never retracted,
  ch. 5.6) and a creation are *irrevocable*, so a transient they acted on
  would persist; a characteristic attribution withdraws what it set where
  it no longer decides (ch. 5.3), and its strict reads order it after its
  producers, so it does not decide on a transient.
  Within a WF-045-admissible recursive-creation cycle the strictness is
  discharged by the one-generation-per-instance progression (ch. 5.5)
  rather than a stratum boundary: the creation reads the settled world
  of the previous generation (ch. 5.5, Generations).
- **negative**: a negated characteristic read, a characteristic read
  under a negation-like quantifier (*geen*, *ten hoogste*, *precies* —
  their truth can be lost by a later write), every read under a
  leeg/gevuld predicate in either polarity, every read in the range of a
  quantifier fold (ch. 3.9a) under *alle*, *geen*, *ten hoogste* or
  *precies* and in its matrix under *geen*, *ten hoogste* or *precies*
  (both since 2026-09-23; the maintainer decisions are quoted in
  `IR-SPEC.md` ch. 5.2), a negated role check (since 2026-10-06, below),
  or a channel test (regelversie, ch. 5.6). Once
  negative, conservatively negative — a double negation does not cancel.
  Negative edges are stratum-strict *and* forbidden on any cycle (WF-046).

Parameters and input-only nodes (nodes no form writes) contribute no
edges.

*Relation reads.* A read that names a relation carries a relation-node
edge: a role binding of a fold or an allocation, a creation's own hops
(the creation-after-creation ordering demanded this), a navigation's hops
(below) and, since 2026-10-06, the role check (`RoleMembership`, ch.
5.8a), which reads relation(its `factType`). The role check reads it
positively, and negatively where it is negated or sits under a
negation-like quantifier: "hij is geen …" holds on absence, and a link a
later creation lays makes it false. Since L2 every allocation, instance
fold and quantifier fold names its relation so (B12), and an allocation
and an instance fold read the population proxy beside it. Day-kind
definitions read only input dates and contribute no edges. *History until
2026-10-06:* relation membership read by a condition was treated as input,
a recorded scope narrowing; and a relation creation wrote
population(target-role filler type) beside its relation, a proxy from
before every fold and allocation named its relation. With the binding
there the proxy ordered nothing the relation edge did not order, and it
drew a cycle that does not exist: an object creation that counts the
members of a role over that type reads the population, and a relation
creation that links those members to the created object reads the
creation's back-link. WF-045 refused such a document and the reference
interpreter with it, while the ASP backend and both Drools branches
answered it (golden `jutterswet-premiepot`; WV-6.4-h). Measured that day
over 1171 documents (the corpus zones, the parity zone, and the examples,
the rule sets and the demonstrations the RegelSpraak frontend's consumers
carry): the proxy edges left 24 of them, only the parity zone's casus lost
a WF-045 report, no role check read a relation that a form writes, and
1186 replays through the reference interpreter and the ASP backend were
unchanged. *History until L2:* a bindingless allocation
kept the population proxy alone; a bindingless fold read the population
proxy and, beside it, the relation derived best-effort — for an instance
fold the fact type of the role its source named, for a quantifier fold
(from 2026-09-23) the one connecting relation WF-048 demanded — and a
relation that could not be derived contributed no edge while the
verifier reported the document; a bean-field-only v0 creation record
carried no symbolic hops and stayed on the bare population write,
recorded with WF-028's uncheckable clause (since L2 the symbolic block
is required, B7).

*The path of a read* (since 2026-10-22). Beside its node and its kind, the
walk records for an attribute or characteristic read the role bindings from
the form's subject to the object it lands on: the ranges of the folds it sits
in, then its own hops. The path decides nothing about the edge kind; it
decides whether the read is shifted, local or neither within an
instance-ordered cycle (IR-SPEC ch. 5.3, ch. 6.2 below).

*A read with the form anchor* (since 2026-10-23, card
`subselection-reads-subject`; ch. 3.4a, `IR-SPEC.md` ch. 4.1a and 5.1). A node
with `anchor: "form"` in a filter or a test draws the edges the same node
draws in the form's own condition or value: a read of the form subject's type
× its member (a navigated one with the link set of each hop), an anchored
fold the population of its element type and the link sets of its own range.
Its kind is the kind of its position, as for every read there: negative in the
range or the matrix of a quantifier fold where those are negative, and under
a leeg/gevuld test. Its path is its own hops alone, without the ranges of the
folds around it, so an anchored own read is local. An identity check draws no
edge. The navigation from the member and the anchored read of the same
object (ch. 3.4a, *Two spellings*) differ in one edge: the navigation reads
the link set of its relation, the anchored read does not, and both orders are
sound.

*Navigated reads (ch. 5.8)* consume relation(each hop), named by the
hop's binding — since L2 every hop carries one (B11); until L2 a
bindingless hop was resolved by the WF-053 unique derivation, and an
unresolvable walk contributed no relation edges (WF-053 rejected the
document, so nothing was hidden on clean input). A navigated
characteristic check additionally reads characteristic(reached type) —
on the type the hop lands on, never the subject — negatively when
negated; the hop relation itself is always a positive read (the
asymmetry of ch. 5.8: an absent link fails the check, so no negative
edge runs through the relation).

*A write is read at the fixpoint* (2026-11-02, card
`written-value-at-fixpoint`; the maintainer's decisions of that day on a
characteristic, ch. 5.3, and on two writers, ch. 5.7). A value, a
timeline or a characteristic that a form wrote stands at the fixpoint
only where that form decides there. A rule whose condition is false on a
later evaluation, or whose value is leeg on a standing or desugared
target (ch. 7.7, 7.8), a defaults group that decides nothing or
conflicts (ch. 7.1, 7.2), and an allocation whose condition is false or
that leaves a recipient out (ch. 4.1), give the target back what it held
before the form wrote it: its input value, or what the creation that made
the object gave it; a characteristic goes where no other attribution that grants it
decides and the input did not set it. Conditions on attribute-writing
forms therefore read positively here: a value they acted on in a
transient window is replaced at the fixpoint. A leeg/gevuld predicate
reads negatively in every position since 2026-09-23. Worked example
(absolute; the golden `jutterswet-geschreven-waarde-aan-fixpunt` with the
rules in the order below and in the reverse order, replayed by gates 5, 6
and 12, and `WV-6.1-written` in `ref/test/`, which runs every order): "De
spoedpremie van een Vondst moet gesteld worden op 1 indien zijn
meldingsgetal kleiner is dan 5" and "Het meldingsgetal van een Vondst moet
berekend worden als zijn aantal getuigen". V with aantal getuigen 10 and
input meldingsgetal 3 gives **meldingsgetal 10, spoedpremie leeg**, and
with input spoedpremie 7 as well **spoedpremie 7**; aantal getuigen 2 and
meldingsgetal 8 give **spoedpremie 1**; meldingsgetal 3 without an aantal
getuigen gives **meldingsgetal 3, spoedpremie 1** (the leeg evaluation
writes nothing, ch. 7.8). Measured 2026-11-02 before the change: with the
rule spoedpremie first the reference interpreter answered spoedpremie 1 in
the first two cases, the value it had written on the input 3; the ASP
backend and both Drools branches answered as above in both orders. A
timeline reads the same: the jaardeel of `TmsTimelineWitnessTest` B2,
written where the teller is below 5 on its input, gives back the input
timeline once the teller is 10 (`WV-6.1-written-timeline`), and so does
an allocation: in the model of the golden `jutterswet-voorwaardedeling`,
with its vrijgavescore computed from a tussenscore that a second rule
replaces by a bergscore 5, the bergopbrengst 100 is distributed 25 and 75
over berggewichten 1 and 3 on the input tussenscore 12, and the shares go
back to **leeg** at the fixpoint, in
either order of the two rules (`WV-6.1-written-allocation`; measured
2026-11-02, the reference interpreter answered 25 and 75 with the
vrijgavescore first, the ASP backend and both Drools branches leeg).

*Closed residual — conditions that hold on absence, and a value that grows
(measured 2026-09-23; decided 2026-11-06, a provisional decision on Fable
advice, card `clingo-creatievoorwaarde-op-de-kring`; the owner can reverse
it).* The leeg/gevuld predicate and the negative parts of a quantifier
fold are not the only conditions that are true while an operand is
absent, or that a growing value makes false: an inequality with one leeg
operand, a numeric equality of two leeg operands, a count or a
filled-value count tested against zero, a propositional *geen* over a
comparison, arithmetic that coalesces leeg to 0, and a comparison that a
growing value makes false, inside the monotone part of a fold ("merk
kleiner dan 2" in an `all` or `atLeast` matrix, merk the number of a
Wachter's opvolgers) or outside any fold ("het merk van de ploegleider is
ongelijk aan 2"). They carry no `negated` field and keep their positive
or stratum-strict reading, so on a WF-045-admissible creation cycle they
stay verifier-clean. The Generations paragraph of ch. 5.5 gives each of
them one reading: a creation's condition is read on the settled world of
the generation before its own, a creation that fired stands, and a later
generation may make the condition that justified it false.

Worked example (absolute; the reference interpreter and both Drools
branches answer so, measured 2026-11-06; the ASP backend's suite pins
each beside its refusal, `ch5.5 creation condition on the cycle`). In the
model of the golden `jutterswet-opvolgerquorum` with its case 4, the
`all` matrix "merk kleiner dan 2" or "merk ongelijk aan 2" gives
merk(W1) = **2** and merk(W3) = **2**, and so does "het merk van de
ploegleider van de Wachter is ongelijk aan 2" in place of the fold. A
recursive creation "werf de opvolger" over a to-one opvolger, under "de
rang van de Wachter is kleiner dan 5 en X", with one input Wachter W1 of
rang 1, mints W2 to W5 for each X below, every creation justified by an X
that the next generation makes false:

| X | why it holds on absence |
|---|---|
| de rang van de opvolger is ongelijk aan 2 | exactly one leeg operand is waar (ch. 1.2) |
| de rang van de opvolger is gelijk aan het peil (peil leeg) | two leeg numeric operands are equal (ch. 1.2) |
| het aantal opvolgers is gelijk aan 0 | a count over nothing is 0 |
| het aantal gevulde rangen van alle opvolgers is gelijk aan 0 | idem (ch. 3.4c) |
| geen van: de rang van de opvolger is groter dan 0 (composite) | a leeg operand makes the member false |
| de rang van de opvolger plus 0 is kleiner dan 1 | plus coalesces leeg to 0 (ch. 1.2) |

The two-form shape reads the same way: with "het merk is ongelijk aan 1"
in the creation and a rule that writes merk = 1 when "de rang van de
opvolger is ongelijk aan 2", generation 0 writes merk(W1) = 1 on the
absent opvolger, the creation does not fire, and the answer is
merk(W1) = **1**. The controls are the same shape with X = "de rang van
de opvolger is leeg" and with X a quantifier fold over the opvolgers
under *alle* or *geen*: the reference interpreter mints the same chain,
and WF-046 refuses those documents (`IR-SPEC.md` ch. 5.2).

The ASP backend has no moment of creation and reads the condition on the
stable model; it answered 1 on the first three forms, found no stable
model on the six of the table and two on the two-form shape. It refuses
by name, before it solves (CLINGO-038, the second posture of `IR-SPEC.md`
ch. 5.5), every condition of an object creation or a fact creation that
reads a node a later generation of the creation's stratum can still
change, the nodes and writers of a member assignment in ch. 5.5, except
a read on a monotone position, where a condition that held cannot be
lost while the stratum grows:

- (M1) the reach of an `atLeast` fold, and a positive role check: a link
  set only grows;
- (M2) the left side of `greater` or `greaterOrEqual`, or the right side
  of `less` or `lessOrEqual`, where the other side reads nothing that
  changes and this side only grows: a count over its reach, without a
  filter or with a monotone one; an attribute that every writer that can
  change it computes as such a count, without a condition; and `plus`
  over such operands and fixed ones;
- (M3) a positive characteristic check where every writer that can change
  the characteristic is an attribution of true whose condition is
  monotone itself (a characteristic whose question meets itself again is
  taken as monotone there, and refuted by any other read).

A composite under `all` or `atLeast` and the matrix of an `all` or
`atLeast` fold are monotone where their parts are; the reach and the
filter of an `all` fold read nothing that changes. The list is closed:
every other read of such a node is refused, the six of the table, the
first three forms and the two-form shape among them, and so is "alle
ploegleden zijn gemerkt" where gemerkt is attributed under "zijn merk is
kleiner dan 2". On a monotone position "held on some generation" and
"holds in the stable model" are one, and the backend carries the
condition: the golden `jutterswet-opvolgerquorum` (M1 and M2), "het
aantal voorgangers is groter dan 0" (M2), "alle ploegleden zijn gemerkt"
with gemerkt under "zijn merk is groter dan 1" (M3), and the fact
creation "geef door" under "hij is ervaren", with ervaren attributed
where the number of maatgevers is groter dan 0 (M3 over M2). Where a
count grows by the creation's own product for its own subject ("het
aantal opvolgers is groter dan 0" in "werf een opvolger", merk(W1) =
**2** in the reference interpreter), the backend's encoding of the count
has no stable model, and its runner refuses the program (CLINGO-030).
None of these forms occurs in the corpus.

Off a cycle the same classes leave no executor order-dependent: a write
that acted on a transient leeg goes back once the condition is false ("A
write is read at the fixpoint", above). Measured 2026-11-02 on a Houder
with one Ding whose a (5) another rule derives, with the reader listed
first: "b = 1 indien de a van het ding ongelijk is aan 5" and "b = 1
indien het aantal dingen waarvan de a groter is dan 0 gelijk is aan 0"
give b leeg in the reference interpreter, the ASP backend and both Drools
branches, in both orders; before that day the reference interpreter kept
the b = 1 it had written on the transient leeg.

A related gap sat in the role-check narrowing above, with no recursion
needed: a creation of a Post under "hij is geen bemanner" reads the very
link it writes. Until 2026-10-06 the verifier saw no edge, the reference
interpreter minted once, and the ASP backend found no stable model.
Since the role check reads its relation, negatively under the negation
(that day, card `feitcreatie-zonder-populatie`), the verifier refuses
the document under WF-045 (a dependency cycle through no recursive
creation: bemannen -> bemannen) and WF-046 (the negative edge lies on
that cycle), and the reference interpreter refuses it under WF-045
(measured 2026-10-09).

### 6.2 Acyclicity (WF-045)

**WF-045.** The dependency graph MUST be acyclic after removing every
creation form — object creation, or relation creation (ch. 5.5's
idempotent, guard-free member) — that is a member of a recursive rule
group (`IR-SPEC.md` ch. 3.9), and every shifted edge of an
instance-ordered part (`IR-SPEC.md` ch. 5.3): a dependency cycle is
admissible **only** when it passes through such a form or is closed by
such edges. The first is ch. 5.5's controlled exception — recursive
creation advances exactly one generation per instance; WF-033 guards the
author's termination obligation and the runtime's bounded budget makes
divergence a fault — promoted from the ch. 5.2 delta to well-formedness.
The second, since 2026-10-22, is the instance-ordered cycle below. With
WF-045 (and the single-writer allocation discipline of WF-026) the
fixpoint of the positive part is unique for every case the loading
admits: rule order, activation order and insertion order do not matter
(ch. 5.2).

**The instance-ordered cycle** *(voorlopig besluit 2026-10-22, card
`cycle-across-instances`; the definitions are `IR-SPEC.md` ch. 5.3)*. A
value of an object that reads the same value of its predecessor over a
switch relation closes a cycle of the graph, which has a node per type and
member, while per object the reads form a chain. The members of such a part
— rules and defaults groups — share a stratum, since every edge of a cycle
counts positive in the assignment of ch. 6.3, and within it they evaluate
per object:

- **Depth.** An object without a link of the switch relation to a
  predecessor stands at depth 0, an object whose predecessor stands at depth
  d at d + 1, and a satellite at the depth of its owner (over its satellite
  relation) where the owner is an object of the chain — of a type that fills
  the role a hop to the predecessor leaves — and at 0 where it is not (an
  owner outside the chain, a satellite among them); an object with neither
  stands at 0. No form writes these links, so the depths are a property of
  the case input; an instance a creation makes has no link and stands at 0.
  *(The satellite outside the chain: voorlopig besluit 2026-11-02, card
  `instance-depth-owner-hop`.)* An owner outside the chain that is no
  satellite has no predecessor and no owner, so it stands at 0 itself. Where
  the satellite type lies below the filler of the owner's role, a satellite
  can own another, and the owner links can close a cycle, which EF-033
  below, a test of the switch relation, does not refuse. A satellite owned by
  a satellite lies in no household (`IR-SPEC.md` ch. 5.3). No read of the
  part reaches its owner from it: the hop falls on the clause `one-walk`,
  since the satellite relation then has ⊑-comparable fillers and a to-one
  owner's role, the shape of a switch relation (measured 2026-11-02 on a
  Bijlage that reads the grensdag of its eigenaar). So it reads in the part
  only itself and the satellites below it, which stand at 0 as well, and at
  depth 0 they settle, ordered by the levels, before an object of a higher
  depth reads them. The depth recurses only over the switch relation, which
  EF-033 keeps free of cycles.
- **The chain ends.** The links of the switch relation, over the role the
  hop reaches, MUST form no cycle in the case input. A cycle is refused
  while the case is read, before any rule fires: EF-033 (ch. 5.7b), with
  the declared name of the fact type and the labels of the cycle, listed from
  its smallest label (labels compared byte by byte) along the role the hop
  reaches, joined by `, `; where the links hold several cycles, the one with
  the smallest label is reported. It is never an evaluation with two
  outcomes and never a budget: without the test the ASP backend grounds
  without bound (measured 2026-10-22: on two Vergunningen that are each
  other's vorige vergunning, still grounding after 30 s at 2.2 GB, stopped by
  hand), and the reference interpreter has no depth to order by.
- **The order.** For d = 0, 1, … up to the deepest object, and per depth for
  each level l of the part (`IR-SPEC.md` ch. 5.3: the longest path over the
  part's edges that are not shifted, a strict or negative edge counting one),
  the members of level ≤ l evaluate on the objects of depth d, and every
  member on the objects of a lower depth, to their fixpoint, beside the
  other forms of the stratum; after the deepest depth the stratum settles
  once more as a whole. A shifted read on an object of depth d lands in the
  household of its predecessor (`IR-SPEC.md` ch. 5.3: the predecessor and its
  satellites), of depth d − 1, which is settled; a local read lands in the
  household of the object, of depth d, whose members of a lower level are
  settled. So a negative shifted edge — a leeg/gevuld test on the
  predecessor's value — reads the predecessor at rest, as a negative edge
  between strata reads its producer at rest.

The order is an order of evaluation, not of meaning: the fixpoint does not
depend on it where the part is evaluated by a model rather than by passes.
The ASP backend translates the part unchanged; its program, grounded on a
case whose chain ends, is stratified per object and has the one stable
model (measured 2026-10-22: the three cases of
`corpus/golden/jutterswet-vergunningketen` and the case of WV-6.4-j PASS
without a change to the translation). The Drools backend fires the members
under truth maintenance, which withdraws a value justified by a leeg
predecessor once the predecessor's value arrives (ch. 6.4, WV-6.4-j). The
reading of a condition at the fixpoint (ch. 5.12) does not change: the
order of the objects is not visible at the fixpoint.

*v0 mirror note:* v0 warns on a circular reference at build time and
bounds execution with a fire cap; WF-045 makes the refusal structural.

### 6.3 Stratified negation (WF-046)

**WF-046.** No dependency cycle — including a WF-045-admissible one —
may carry a negative edge. Equivalently: **strata exist**. Assign every
form the smallest stratum such that along every positive edge the
consumer's stratum is at least the producer's, and along every
stratum-strict or negative edge strictly greater (strictness on a
WF-045-admissible creation cycle is discharged by the generation
progression, ch. 6.1, not by a boundary). WF-046 alone guarantees the
assignment exists — no negative edge on a cycle is exactly the
stratifiability condition; WF-045 separately makes the within-stratum
fixpoint unique. The verifier rejects a program where the assignment
does not exist.

Evaluation by strata generalises ch. 5.3's single boundary: for k = 0,
1, …, the forms of stratum ≤ k evaluate to their fixpoint, by
generations within a stratum (ch. 5.5), before any form of stratum
k + 1 fires. A negated characteristic, and since
2026-09-23 a leeg/gevuld predicate and the negative parts of a quantifier
fold (its range under every quantifier but *ten minste*, its matrix under
*geen*, *ten hoogste* and *precies*), is then read only after every
producer — and the producers' own input chains — have quiesced, so a
later write cannot contradict it; a channel
test reads a complete channel for the named rule. v0's single boundary
is the two-stratum special case; nested and chained negation get as
many strata as the chain is deep. Within the stratum of an
instance-ordered part the order gains two keys, the depth of the object and
the level of the member (ch. 6.2): stratum k, then depth d, then level l.

### 6.4 Worked examples (ref/ tests verbatim)

- The ch. 5.3 pair — producer "een Jutter is vrijgesteld indien zijn
  inkomen kleiner is dan 100", consumer "een Jutter is
  bijdrageplichtig indien hij niet vrijgesteld is" — takes two strata:
  producer 0, consumer 1 (one negative edge). Input inkomen = 50: at
  stratum 0's fixpoint vrijgesteld is waar; stratum 1 reads the settled
  negation → bijdrageplichtig onwaar. Input inkomen = 200: {onwaar,
  waar}. Never the transient {waar, waar}.
- The ch. 5.6 channel read — consistency "controle saldo" and the
  Dossier rule reading "regelversie controle saldo is inconsistent" —
  puts the consistency at its reads' stratum and the reading rule
  strictly higher (channel edge); ch. 5.6's fixpoint (one error fact,
  both dossiers flagged) follows by construction.
- A four-deep negation chain (the shape of the art. 4 defaults read in
  `ASP-TRANSLATION.md` ch. 6): k1 from an input comparison, k2 indien
  niet k1, k3 indien niet k2, k4 indien niet k3 — strata 0, 1, 2, 3;
  input x = 0 yields {k1 onwaar, k2 waar, k3 onwaar, k4 waar}, x = 1
  the complement. v0's single boundary cannot express it — precisely
  the case WF-046 exists for.
- An emptiness test on a *derived* attribute by a characteristic rule
  ("gemarkeerd indien a leeg is", a computed elsewhere) is a negative
  edge (stratum-strict before 2026-09-23): the reader sits above the
  writer, and the outcome is independent of rule order — the transient
  emptiness is unobservable (WV-6.4-e).
- The same test by an ATTRIBUTE rule ("b = 1 indien a leeg is", with
  a = 5 written by another rule) is a negative edge too, and for this
  reader that is new. Until 2026-09-23 the edge was positive, the two
  rules shared a stratum, and a reader listed before its writer acted on
  the transient leeg and kept its write: b = 1 with the reader first and
  b leeg with the writer first, on one verifier-clean document, while
  the ASP backend answered leeg in both orders. Now the reader sits above
  the writer and b is leeg in both orders (WV-6.4-f). The move reaches
  every position in the condition of an attribute-writing form: measured
  on the same reader-first shape, the own operand, a navigated operand,
  a fold filter, a quantifier filter, a Let as the operand and a gevuld
  test in the filter of a count compared with zero each moved from b = 1
  to leeg, which is the ASP backend's answer in every case. The
  quantifier-fold decision moves the same reader-first shape in its
  negative parts: "b = 1 indien geen ding heeft een a groter dan 3"
  (a = 5 derived by another rule) and "b = 1 indien alle dingen hebben een
  rang groter dan 0", whose range a creation fills, each went from b = 1
  to leeg, again the ASP backend's answer; that is the matrix under
  *geen*, *ten hoogste* and *precies* and the range of every fold but
  *ten minste*. A creation condition and a characteristic rule did not
  move, because they already read stratum-strict.
- A relation creation that links instances which exist to the object a
  creation made — "een premiejutter van een verdeelde premiepot is een
  havenjutter van de premiehaven van de verdeelde premiepot", beside "een
  Haven heeft de premiepot met pot gelijk aan het aantal havenjutters van
  de Haven" — writes the relation it creates and no population, so the
  creation, which counts the havenjutters, does not read what the
  relation creation writes, and the two make no cycle. A role check on
  the created relation, "een Jutter is premiegerechtigd indien hij een
  premiejutter is", reads it stratum-strict. Strata: the creation 0, the
  relation creation 1 (it reads the creation's back-link), the role check
  2. Input H1 with havenjutters J1 and J2, and J3 without a haven: J1 and
  J2 premiegerechtigd, J3 not (WV-6.4-h). Until 2026-10-06 this was a
  WF-045 cycle through the relation creation's population proxy (ch. 6.1).
- Ill-formed: `a ← b + 1` and `b ← a + 1` on one object type is a
  WF-045 cycle through no creation (negative case
  `wf045-cyclic-attribute-dependency`). A recursive creation whose
  condition negates a characteristic derived from its own output is a
  WF-045-admissible cycle carrying a negative edge — a WF-046
  violation (negative case `wf046-negated-read-in-recursive-cycle`).
  So is a recursive creation guarded by a leeg/gevuld test on what its
  own cycle produces, in either polarity (`IR-SPEC.md` ch. 5.2): the R1
  shape (the creation mints a successor while "het merk is leeg", and
  the rule that writes merk = 1 reads the successor's rang under
  "gevuld"), the R3 shape spelled with `negated: true` (both tests "niet
  gevuld"), a presence-true test alone ("het merk is niet leeg") and
  the test as the matrix of an `all` fold over the minted successors —
  negative cases `wf046-empty-test-recursive-creation`,
  `wf046-empty-test-negated-recursive-creation`,
  `wf046-empty-test-presence-recursive-creation` and
  `wf046-empty-test-quantifier-fold-recursive-creation`. `ref/` re-runs the
  check and refuses R1 and R3 with the WF-046 report (WV-6.4-g). Before
  the rule it answered them: R1 with a chain of five whose creations
  are each justified by a leeg merk the final world fills (the ASP
  reading has no stable model), R3 with merk(W1) = 1 (the ASP reading
  has five stable models).
  So, too, is a recursive creation guarded by a quantifier fold whose
  range grows under the creation, under *alle*, *geen*, *ten hoogste*
  or *precies*, or whose matrix the cycle derives, under *geen*, *ten
  hoogste* or *precies*, or which sits under an enclosing propositional
  *geen* (`IR-SPEC.md` ch. 5.2): "alle opvolgers hebben een rang groter
  dan 5" and "geen van de opvolgers heeft een rang groter dan 0" over the
  successors it mints, "geen van de ploegleden heeft een merk groter dan
  0" with merk derived from the minted successors, "alle posten van de
  Wachter hebben een nummer groter dan 5" over the Posts a creation of
  another type mints for the Wachter, "ten hoogste 1" and "precies 1
  opvolger heeft een kleur ongelijk aan 7" with kleur input only, "ten
  hoogste 1" and "precies 1 ploeglid heeft een merk groter dan 0", and
  "geen van: ten minste 1 opvolger heeft een rang groter dan 0" —
  negative cases
  `wf046-quantifier-fold-all-range`, `wf046-quantifier-fold-none-comparison`,
  `wf046-quantifier-fold-none-matrix`, `wf046-quantifier-fold-range-other-type`,
  `wf046-quantifier-fold-at-most-range`, `wf046-quantifier-fold-exactly-range`,
  `wf046-quantifier-fold-at-most-matrix`, `wf046-quantifier-fold-exactly-matrix`
  and `wf046-quantifier-fold-under-none`. Before the rule, where the range
  or an enclosing negation closed the cycle, the reference interpreter
  minted on a condition that the mint itself falsified (a chain, one
  successor, or — where the creation mints a Post for a Wachter — one
  Post) and the ASP reading has no stable model; where only the matrix
  closed it, the two executors split without either failing (the
  reference interpreter minted for W1, the ASP backend did not).
  So, too, since 2026-10-06, is a recursive creation guarded by a negated
  role check on the relation it creates: "een Wachter heeft een opvolger
  … indien hij geen voorganger is" reads `opvolging` negatively and lays
  the link that makes it false (negative case
  `wf046-negated-role-check-recursive-creation`). Before the role check
  read its relation the reference interpreter answered it in pass order
  and the ASP backend found no stable model.
- Well-formed, the monotone parts of the same fold on the same kind of
  cycle (golden `jutterswet-opvolgerquorum`): the creation mints while
  "ten minste 1 opvolger heeft een rang groter dan 3" and "alle
  ploegleden hebben een merk groter dan 1", with merk the number of a
  Wachter's successors. W1 (rang 1) with an input opvolger of rang 4
  mints one successor and keeps its verdict as the range grows: merk(W1)
  = 2. Add a ploeglid W3 without successors and W1's `all` fails: merk(W1)
  = 1. Give W3 an input opvolger of rang 4 and W3 mints first, its merk
  becomes 2, and then W1's `all` holds and W1 mints: merk(W1) = 2,
  merk(W3) = 2. With no links at all (W1 of rang 1, W2 of rang 4)
  nothing is minted: merk 0 and 0, the counter-witness to reading the
  matrix negatively under every quantifier.
- The instance-ordered cycle (ch. 6.2; `ref/` tests WV-6.4-i, absolute, in
  four orders of the rules and of the objects): the einddag of a Vergunning
  is its basiseinddag plus the restant of its vorige vergunning when that
  is gevuld, and its basiseinddag when that is leeg; its restant is the
  number of days from its stopdag to its einddag when both are gevuld and
  the stopdag is earlier. The restant reads the einddag of its own object
  (a local edge, negative under the gevuld test, so the restant has level 1),
  and the einddag reads the restant of the predecessor (shifted edges, one
  of them negative). V1 (stopdag 01-03-2026, basiseinddag 01-06-2026, no
  predecessor) stands at depth 0 and V2 (basiseinddag 01-12-2026, vorige
  vergunning V1) at depth 1. At depth 0, level 0: the restant of V1's
  predecessor is leeg (there is none, ch. 5.8), so **einddag V1 =
  01-06-2026**, the basiseinddag; level 1: **restant V1 = 92** dagen. At
  depth 1, level 0: the restant of V1 is 92, so **einddag V2 =
  01-12-2026 + 92 dagen = 03-03-2027**; level 1: V2 has no stopdag, its
  restant stays leeg. A Vergunning alone gets its basiseinddag. The golden
  `jutterswet-vergunningketen` carries this case as its case 2, a chain of
  three as its case 3 (einddag of the third 01-06-2027 + 183 dagen =
  01-12-2027), and the same chain in the positive form over a satellite
  Termijn.
- What the order buys (`ref/` test WV-6.4-j, absolute, in two orders of the
  rules): add "een Vergunning is zonder verlenging indien het restant van
  de vorige vergunning leeg is" and let "einddag zonder restant" write the
  basiseinddag on a Vergunning zonder verlenging. The characteristic is a
  member of the cycle, and it is never withdrawn (ch. 5.3). Evaluated on V2
  before the restant of V1 exists, it would hold for V2 for good, and the
  two writers of the einddag of V2 would disagree: measured 2026-10-22 with
  the depth order switched off, the reference interpreter faulted EF-016
  ("einddag zonder restant" writes 01-12-2026, "einddag" 03-03-2027). In
  depth order: **V1 zonder verlenging, V2 not, einddag V2 03-03-2027**,
  which is the ASP backend's stable model as well.
- A defaults group on the cycle (`ref/` test WV-6.4-l, absolute, in two orders
  of the rules): the einddag "mag opgegeven worden", so the group "opgave boven
  berekening einddag" puts a given einddag above the berekende einddag, which
  is the basiseinddag plus the restant of the vorige vergunning. The group's
  displacement reads are negative edges (WF-051), local ones on this cycle. V1
  without a predecessor, V2 with an opgegeven einddag 01-01-2027, V3 after V2:
  **berekende einddag V2 03-03-2027, einddag V2 01-01-2027, restant V2 122**
  (from 01-09-2026), **einddag V3 01-06-2027 + 122 dagen = 01-10-2027**, and the
  consistency of a given value that differs from the computed one holds for
  V2. The ASP backend and both Drools branches answer the same (measured
  2026-10-22).
- A fold over the own satellites (`ref/` test WV-6.4-m, absolute, in four
  orders of the rules and of the objects; `IR-SPEC.md` ch. 5.3, the household
  of an object): a Vergunning has several termijnen; the overschot of a
  Termijn is the number of days from the verlengde einddag of its houder to
  its einddag when that is later, the restant of a Vergunning is the sum of
  the overschotten of its termijnen, and the verlengde einddag is the
  basiseinddag plus the restant of the vorige vergunning. The restant reads
  the overschotten over a to-many hop to the satellites, a local read: every
  termijn stands at the depth of its houder. V1 (basiseinddag 01-06-2026, no
  predecessor) with T1 (einddag 11-06-2026) and T2 (einddag 21-06-2026), V2
  (basiseinddag 01-12-2026, vorige vergunning V1) with T3 (einddag
  10-01-2027). At depth 0: the restant of V1's predecessor is leeg, so
  **verlengde einddag V1 = 01-06-2026**; **overschot T1 = 10** and **T2 =
  20** dagen; **restant V1 = 30**. At depth 1: **verlengde einddag V2 =
  01-12-2026 + 30 dagen = 31-12-2026**; **overschot T3 = 10**, **restant V2
  = 10**. The ASP backend and both Drools branches answer the same (measured
  2026-10-22). Until 2026-10-22 the verifier refused the document, since the
  fold reached a to-many role. The golden `jutterswet-vergunningtermijnen`
  carries the household further: a filter and a quantifier `all` over the
  own termijnen that read the verlengde einddag of the houder of the termijn,
  and a sum over the termijnen of the vorige vergunning.
- A satellite outside the chain (ch. 6.2, the depth; `ref/` test
  `instance-depth-owner-cycle`, absolute): Vergunning and Bijlage are each a
  Ding, the switch relation is vorige vergunning, and the owner's role of the
  satellite relation bijlagerelatie, eigenaar, is filled by Ding, its other
  role by Bijlage, to-many. The einddag of a Vergunning is its basiseinddag
  plus the restant of its vorige vergunning, its restant the sum of the
  overschotten of its bijlagen, the overschot of a Bijlage the number of days
  from its stopdag to its grensdag when the stopdag is earlier, and the
  grensdag of a Ding its einddag; the verifier admits the document. The
  Bijlagen b1 and b2 are each other's eigenaar, a cycle EF-033 does not
  test, and V1 (basiseinddag 01-06-2026) has no predecessor: **depth V1 0,
  b1 0, b2 0**, and the case is **consistent**, the ASP backend's answer as
  well. Measured 2026-11-02 with the owner's depth followed onto a satellite:
  the depth of b1 asked for that of b2 and then for its own, and the
  reference interpreter failed. The Bijlage of `ref/` test WV-6.4-o, whose
  eigenaar is a Ding that is no Vergunning, stands at 0 by the same clause.
- A cycle in the links (`ref/` test WV-6.4-k, red case
  `corpus/red/instance-order-link-cycle`): V1's vorige vergunning is V2 and
  V2's is V1. The case is refused while it is read, **EF-033** with
  `factType` "vorige vergunning" and `cycle` "V1, V2"; three Vergunningen
  eerste → tweede → derde → eerste give `cycle` "derde, eerste, tweede", one
  that is its own predecessor "V1".
- Ill-formed, each a WF-045 cycle that is no instance-ordered part
  (`IR-SPEC.md` ch. 5.3): the einddag summed over a set of vorige
  vergunningen (`wf045-instance-cycle-to-many-predecessor`); a fact
  creation that links the switch relation
  (`wf045-instance-cycle-created-relation`); a Termijn shared by several
  houders (`wf045-instance-cycle-shared-satellite`); the einddag reading the
  restant of the predecessor and its own restant
  (`wf045-instance-cycle-direct-and-shifted-read`); a sum over every
  Vergunning of the same register (`wf045-instance-cycle-non-local-read`); a
  read of the predecessor and one of the successor
  (`wf045-instance-cycle-predecessor-and-successor`); a Termijn reached
  through two satellite relations
  (`wf045-instance-cycle-two-satellite-relations`); a blokkade that reads the
  openstand of the Vergunningen of the same register, over a hub, with two
  stable models (`wf045-instance-cycle-hub-negation`); a read of a satellite
  of a satellite (`wf045-instance-cycle-satellite-of-satellite`). All but
  the first, the second and the fourth are named by the arm
  `WF-045.instance-read-outside` with the read and its clause (`IR-SPEC.md`
  ch. 5.3). A sum over the termijnen of the
  predecessor was ill-formed until 2026-10-22 and is a shifted read since
  (`wf045-instance-cycle-to-many-satellite-step`, removed with reason).

---

## 8. Time selection

### 8.1 The single axis

One evaluation session carries **one date** (ch. 5.7 step 1): the
**evaluation date** — v0 surface name *Rekendatum*, wire name `asOfDate`
in `uchar-case-v0`. It is simultaneously the valuation date, the
calculation date, and the vintage selector: the single time axis of
`PLAN.md` §9, decision 2, accepted v1 scope. A time-free session
carries none (`asOfDate` null), and every construct that needs the date
faults when it is absent — never a default "today".

**Extension point (named, not designed):** a second, knowledge-time
axis — "what did we believe the rules/data were on date k" (retroactive
legislation) — would enter as a *second* session parameter next to the
evaluation date, selecting among rule versions and parameter timelines
*as known at k*, while the evaluation date keeps indexing the object
timelines. Nothing in this chapter may assume the two coincide
structurally; that is the whole extension point. Adding the axis is a
session-shape change (ch. 5.7 step 1), not a re-foundation of the
timeline algebra.

### 8.2 Gating versus indexing

The evaluation date acts in exactly two ways, and the distinction is
normative:

- **It gates.** A versioned form applies only when the date falls in
  its validity window (ch. 5.7; `IR-SPEC.md` ch. 3.10), and the
  ch. 3.10 period condition tests the date against a window. Gating
  decides *whether* a rule participates.
- **It indexes only through the read.** No construct implicitly turns a
  timeline into "its value today": the only bridges from a timeline to
  a scalar are the dedicated collapses (ch. 3.6, 3.9) and the explicit
  point read (ch. 3.3, `IR-SPEC.md` ch. 4.3a). The evaluation date is
  available to expressions solely through the two context values: a
  `ContextValue` named `evaluationDate` (`Rekendatum` until L5, the v0
  `Constante` until L4; day precision), and its sibling
  `evaluationYear`, the calendar year of that date (below).

**The execution year.** A `ContextValue` named `evaluationYear` denotes
the calendar year of the evaluation date: a whole number without a unit,
the value `yearOf(evaluationDate)` has (ch. 3.8). It is derived from the
session's one date and is never supplied beside it. A session carries
one date (ch. 8.1); a year supplied as a second value could contradict
that date, and a rule that reads both would then read a date from one
year and a year number that names another. It needs the date exactly as
`evaluationDate` does: where the session carries none (`asOfDate` null)
the read faults — "the evaluationYear context value needs an evaluation
date; the case carries asOfDate null" — and never falls back to a year
of its own. Its type is fixed by `IR-SPEC.md` ch. 4.1 and not declared
in the document: `Number`, `whole: true`, `decimals: 0`, no `unit`. All
three executors carry it: the reference interpreter at evaluation, the
ASP backend as a program constant written at translation time, and both
Drools branches from the session's evaluation date. The golden
`jutterswet-rekenjaar` pins the same outcomes in all three. Both Drools
branches play it, with the reference interpreter's answer, in every
position the backend's `EvaluationYearWitnessTest` twists: the assertion of a
consistency, a member assignment of an object creation, the guards of an
allocation and of a fact creation, the filter of a fold, and the
consequence and the justification of a prioritized default (measured
2026-10-14; since the card `drl-teksttak-op-truth-maintenance` the two
branches render one analysis). No golden carries the node in those
positions. On 2026-09-26 the exec-model branch still refused the assertion
of a consistency, the filter of a fold and the justification of a
prioritized default by name, and the DRL-text branch, then a lowering of
its own, had no defaults lowering. *History:*
from L4 until 2026-09-26 (card `rekenjaar-als-waarde`) the name was
admitted but no document carried it, and every executor refused it by
name in one sentence.

**Worked example (absolute; `ref/` tests verbatim, WV-8.2-rekenjaar-\*).**
Document: object type `Vondst` with three whole-number attributes,
`vondstjaar`, `meldjaar` and `aantal verstreken penningjaren`; two
rules, *het meldjaar van een Vondst* = `evaluationYear`, and *het
aantal verstreken penningjaren van een Vondst* = `evaluationYear` min
`vondstjaar`. One Vondst V1 with `vondstjaar` 2025.

| session `asOfDate` | `meldjaar` of V1 | `aantal verstreken penningjaren` of V1 |
|---|---|---|
| 2026-12-31 | **2026** | **1** |
| 2027-01-01 | **2027** | **2** |
| null | the evaluation faults: "the evaluationYear context value needs an evaluation date; the case carries asOfDate null" | — |

The first two sessions are one day apart and one year apart: the year
follows the calendar, not a count of days. At 2026-12-31 the value is
the number 2026 without a unit, equal to `yearOf(evaluationDate)` in
the same session.

**The bound day is not a session value.** Until L4 `IR-SPEC.md` ch. 4.1
admitted a third context-value name, `de dag`; since L4 it is the node
`BoundDay`, and this chapter does **not** own it: it denotes the BOUND DAY
of a day-kind definition, and its runtime meaning is stated where that
binding is made — ch. 3.8, the day-kind duration forms. Two consequences
follow, and both are checked. Outside a day-kind evaluation the bound day
denotes nothing and every executor
faults by name (red case `context-value-de-dag-outside-day-kind`); and the
converse, a SESSION value inside a day-kind condition, is refused
statically (WF-031's position arm, `IR-SPEC.md` ch. 3.7), because a
stratum-0 classification that moved with the session would classify the
same calendar day differently in two runs of the same document.

### 8.3 The vintage read

Selecting a parameter's vintage **is not a separate mechanism**: it is
the ch. 4.3a temporal read of a granular parameter with the evaluation
date as index — `read(tl_p, evaluationDate)`. The read itself keeps its
ch. 3.1 totality (a gap reads to leeg; an empty index reads to leeg);
everything fail-fast about vintages lives one layer down, in the data
plane (ch. 8.4), where it is checked **before any rule fires**. This is
the decided division of labour of ch. 3.3.

A time-dependent parameter is also a **timeline operand** of the period
arithmetic of ch. 3.4, read whole like a temporal attribute
(`spec/proposals/IMPLICIT-TIME.md` VB 2.3). That reading is no vintage
read: it indexes no date, so the session-start coverage check of ch. 8.4
covers the vintage reads alone, and a session that supplies no timeline
for such an operand faults as for an unbound parameter (EF-014). Worked
example (the anchor `corpus/golden/anchor/jutterswet-verzuimrente-dagen`):
the verzuimrentevoet, 0,04 /jr on (−∞, 2026-02-01) and 0,05 /jr on
[2026-02-01, +∞), maal an afdrachtbedrag of 10 000 € is 400 €/jr and
500 €/jr on the same two periods; the evaluation date (2026-03-12) selects
nothing.

### 8.4 The Parameterset and the session-start coverage check

The **Parameterset** is the data plane of granular parameters: per
parameter its full timeline, supplied by the session (in the corpus:
the `uchar-case-v0` timeline value variant on a parameter). Its
well-formedness is data-plane, not IR well-formedness — no WF number;
the checks live in the loaders and at session start, like the case
loader's cross-checks:

- **Load-time (per timeline, ch. 3.1 shape):** day-precision segments,
  ordered, non-overlapping, aligned to the declared granularity grid —
  an overlap is a load error, never a later surprise.
- **Session-start (coverage):** for every granular parameter the
  document reads at the evaluation date (a ch. 8.3 vintage read), the
  session MUST carry an evaluation date, the Parameterset MUST carry
  that parameter's timeline, and the timeline MUST cover the
  evaluation date (the single-axis evaluation window is that one day).
  A violation is a **session-start error**, raised before any rule
  fires — a missing vintage never surfaces as a mid-evaluation leeg.
  The check is **static and conservative** by design: it covers every
  vintage read the document carries, whether or not ch. 8.2's gating
  would activate the carrying rule version at this date, and whether
  or not any instance exists for it to fire over — an inactive
  version's missing vintage is still a configuration error. The check
  can therefore only be too strict, never silently too lenient.

The two conventions of the case format stay strictly apart (ch. 3.6,
`spec/schema/README.md`): the asOfDate-bucket scalar serves the
temporal total; the full timeline serves reads. An executor that needs
the one and finds the other faults — never guesses (the ch. 4.3a
rule) — and both executors implement the session-start check
identically (`ref/` at evaluation entry; the ASP backend at
translation time, before emitting a program). Every form that reads the
timeline of an attribute whole is such a reader, and the
asOfDate-bucket scalar is the fault EF-037 (ch. 5.7b), naming the
attribute and the object whose input it is: a point read (ch. 3.3), the
period arithmetic over an own attribute and over the members of a sum
(ch. 3.4, 3.5), the value and the compared timeline of a gedurende clip
and a member of a per-element total (ch. 3.6), the base of a pro-rata
part (ch. 3.7), the whole-period qualifier, also over a path (ch. 3.9),
and a condition per period (ch. 3.10). An absent attribute is no
such input: those forms read it as the empty timeline, and the point
read faults on it without the code.

A **characteristic** has the same two conventions and the same
apartness (maintainer decision 2026-09-09): the flat `characteristics`
list asserts a scalar truth, `characteristicTimelines` supplies
periods, and every temporal reader — this chapter's clip, ch. 3.7's
pro-rata fold, the day count — consults the timelines and nothing
else. A characteristic the document declares TIME-DEPENDENT therefore
has exactly one admissible delivery, and asserting it in the static
list is refused **at load**, by name and with the reason, in all three
case readers. It is not a form an executor may decline to carry: it is
a defective case, and until that decision it was accepted and answered
from an empty period set — 0 where the golden pinned 30, in both
executors, without a word. The case schema cannot enforce this (it
validates the case document alone and never sees the declarations), so
each reader holds it itself.

Point reads at a *non*-evaluation-date index (the art. 1 shape,
ch. 3.3) are deliberately **outside** this check: their index is
data-dependent, so gap-to-leeg is their meaning (the anchor case
`jutterswet-aanmeldquotum` pins it); the session-start check covers
exactly the reads whose index is the session's own date, where a gap
is a configuration error, not a data condition.

### 8.5 Worked example (absolute; the anchor case verbatim)

Document: granular parameter `penningbedrag` (year granularity, whole
number); rule: *het huidig penningbedrag van een Jutter* = the
temporal read of `penningbedrag` at `evaluationDate`. Session: evaluation
date 2026-03-15; Parameterset `penningbedrag` = [2025: 4, 2026: 5]
(year segments).

- Coverage check: the document vintage-reads `penningbedrag`; the date
  is present; the timeline covers 2026-03-15 → the session starts.
- The read: the 2026 segment → **5**; every Jutter's `huidig
  penningbedrag` is 5 (`corpus/golden/anchor/jutterswet-penningvintage`,
  replayed by gates 5 and 6).
- The red variants (same document): evaluation date 2028-03-15 (gap),
  the timeline absent, or `asOfDate` null — each fails the
  session-start check before any rule fires, and both executors report
  the same missing coverage; nothing is evaluated. These variants
  cannot sit in the golden corpus (a golden must replay green); they
  are pinned here and exercised in the card's verification record. A
  standing regression guard for the red path needs an expected-fault
  case form the harness does not have yet — open debt of the same
  class as the standing harness guards (no-op, unsolved-state) still
  to land, recorded here rather than silently absent.

## 7. Defaults and defeasibility (design — the Phase-4 gate)

*Status: normative design, with **prioritized defaults landed in both
target kinds** (`IR-SPEC.md` ch. 6.1: the `prioritizedDefaults` extension
form, WF-050/051 with verifier checks and negative cases). The art. 4
characteristic defaults are the anchor
`corpus/golden/anchor/jutterswet-uitsluiting` (worked example 7.6.1
machine-checked); the attribute defaults with **leeg fall-through**, the
**initialisation default**, and the **speculative-deposit rule** are the
anchor `corpus/golden/anchor/jutterswet-vondstwaardering` (worked
example 7.6.2's outcomes — computed wins, fall-through to
initialisation on a computed leeg, input left standing, a deciding
default overwriting input — machine-checked through both executors).
The declared-unordered group and the §9.5 conflict path are the anchor
`corpus/golden/anchor/jutterswet-toelagegronden` (worked example
7.6.3's outcomes — differing grounds → no write + one channel fact,
equal grounds → one write and no conflict, input kept standing under a
conflict, the characteristic conflict — machine-checked through both
executors; the wire marking is `unordered`, IR-SPEC ch. 6.1). The v0
desugaring is ch. 7.7, machine-checked by the
`jutterswet-waardering-v0vorm` anchor (ref replays the v0 forms
natively, the ASP backend the desugared defaults — the gate-6
differential witnesses the lowering). Rule-readability of the conflict
fact is **closed** for v1 (ch. 7.2, decided 2026-08-20); the ch. 7.8
standing norm holds on a scalar target and per day on a timeline target,
and a defaults group on a timeline attribute target decides per day
(ch. 7.11). Below-root
exception branching is ch. 7.9 (Phase 5c): the `Default.exceptions` wire
form and its well-formedness (WF-050 recursion, WF-051 unchanged,
WF-075) land with this chapter; the anchors `jutterswet-nestuitzondering`
and `jutterswet-nestkenmerk` prove both target kinds through the executor
card.*

### 7.1 The model: prioritized defaults, and fall-through as its semantics

*Renamed 2026-08-29 from "ladder"/"rung"/"sub-ladder" (coined here
2026-08-15/16; no term of art). Git history — commit messages and
pre-rename revisions — uses the old names throughout.*

*Naming.* "Prioritized defaults" replaces "ladder", coined in this
project (2026-08-15/16) and no term of art. The family is prioritized
default logic (Reiter; Brewka; Horty, *Reasons as Defaults*; Lawsky,
*A Logic for Statutes*, 21 Fla. Tax Rev. 60 (2017)). A default here is
a Horty-style pair — justification plus consequence — with priority
doing the work of Reiter's consistency justifications: the lowering
generates `not decides_m` for every higher default. The nearest named
structure is Catala's default tree (Merigoux, Chataing & Protzenko,
ICFP 2021), from which this deviates exactly on the tracked
`CONSTRUCTS.md` row-8 deltas (observable non-fatal conflicts,
equal-decision reconciliation, leeg fall-through across compositions).
It is deliberately *not* a defeasible theory in the
Antoniou–Billington–Governatori–Maher sense: priority is a total,
structural, per-target order — never a free `r1 > r2` relation
(`spec/proposals/PRIORITY-RELATION.md`, C2) — there is no
strict/defeasible/defeater trichotomy, and a pure blocker on an
attribute target is inexpressible by design (a defaults group never
writes leeg). Operational kin, for the integrator: DMN hit policies
FIRST (the ordered group) and ANY (the declared-unordered group), and
XACML's `first-applicable` / `only-one-applicable` with
`Indeterminate`-as-value. Unrelated to ladder logic (IEC 61131-3).

Adopted direction (`PLAN.md` Phase 4, `IR-REVIEW.md`): Catala's default
calculus — prioritised defaults with explicit conflict detection —
specialised by what the ASP hand-exercises established
(`ASP-TRANSLATION.md` ch. 5 finding 3, ch. 6; `CONSTRUCTS.md` rows
7/8):

- A **default** is a pair (justification, consequence) aimed at one
  target (an attribute write or a characteristic attribution).
- Defaults on one target organise as a group of **prioritized defaults**:
  a totally ordered priority chain. The group is **one writer** computing
  **one decision**, top-down: the **highest default that decides** supplies
  it, and every lower default is displaced. What "decides" means differs by
  target kind, and the distinction is normative:
  - **attribute target:** a default decides when its justification holds
    *and* its consequence evaluates **gevuld**; a justified default whose
    consequence evaluates leeg **falls through** (Catala's fall-through
    — this is what makes ch. 5.4 reproducible, ch. 7.3);
  - **characteristic target:** a default's consequence is the boolean
    **decision itself** — attribute or do-not-attribute — and a
    justified default therefore *always* decides (there is no leeg
    boolean); a do-not-attribute decision displaces lower defaults and
    makes the group derive nothing, which is not a retraction
    (the group decides once, at the fixpoint of its justification
    strata, then at most one attribution happens; ch. 5.3).

  At most one default decides, so a single group can never conflict — a
  property of the semantics, not a claim about authors.
- A decision that does not fit the declared decimals of its target
  **decides** all the same: every lower default is displaced, a member of a
  declared-unordered group reconciles on its exact value (ch. 7.2), and
  the fault EF-006 of the assignment gate (ch. 2.4 point 1) stands in place
  of the group's write. Worked example (the document of
  `corpus/negative/wf023-afwijking-past-niet`, which the precision gate of
  ch. 2.4 point 4 refuses before it runs, and the case beside it, `ref/`
  test `WV-7.1-past-niet`): the deviation "(zijn toeslaggrond gedeeld
  door zijn geschatte gewicht) plus 5,00" of the toeslag, of 2 decimals,
  with toeslaggrond 100,00 and geschatte gewicht 30,00, decides 25/3; the
  rule "op 1,00" beneath it does not decide, and the session faults with
  the one fault EF-006, place V6.toeslag, value 25/3, decimals 2.
- On a **timeline** attribute target every clause of this section holds
  per day (ch. 3.3): on each day the highest default that decides that day
  supplies the decision of that day, and a default whose consequence reads
  leeg on a day falls through on that day (ch. 7.11). A time-dependent
  characteristic is no target of a defaults group (`IR-SPEC.md` WF-086;
  ch. 3.12 row 7).
- If no default decides, the group **writes nothing**: the target keeps
  what the fact base holds — its input value, or leeg. A defaults group
  never writes leeg; consequently "deliberately assign leeg" is not
  expressible as a default (a recorded design limit: it needs an
  ordinary rule, outside the defaults), and the fall-through deliberately
  does **not** distinguish "default inapplicable" from "default
  applicable with an empty value" — both fall through. *v0 mirror note
  (tracked with the Phase-4 desugaring):* an unguarded v0 computation writes a
  computed leeg over an input value (ch. 1.2g); once desugared into a
  defaults group that case shifts to "input left standing" — a deliberate,
  pinned delta, not drift. Input facts are **not** a default: they are
  the fact base the group's write meets under ch. 1.2g.
- Default evaluation is **speculative** until a default decides: a
  fallen-through default's arithmetic channel deposits (a division by
  leeg inside a consequence that then falls through, ch. 1.2) do
  **not** persist — only the deciding default's evaluation deposits on
  the §9.5 channel. Outside a defaults group a form that is evaluated at
  the fixpoint deposits whether it writes or not (ch. 7.8), so prioritized
  defaults are the one sanctioned speculative context; a deposit on a
  state before the fixpoint is replaced in either (ch. 5.7).
- The **base rule** is a default like any other; the initialisation
  default, when present, sits below it (ch. 7.3). The textual nesting of
  art. 4 — "geldt niet indien …, dat laatste geldt weer niet indien …"
  — is surface form: the frontend desugars each nested layer to a
  self-contained justification (conjoining its enclosing layer's), so
  defaults carry independent conditions and top-down fall-through decides.
  The group stays **first-class IR structure**: compiling it into a
  DNF would erase exactly the layers legal amendments edit (the
  exercise-2 finding).
- **Parallel grounds** (art. 4bis "evenmin") are *separate defaults
  groups*, each on its own target, composing conjunctively downstream (each
  ground guards its consumer independently). Multiple defaults groups on
  **the same** target are admissible only as **declared-unordered**
  parallel defaults — the IR makes priority explicit (ordered defaults in
  one group) or explicitly absent (separate groups on one target); there is
  no implicit order between groups. That declared absence is exactly the
  over-determination surface of ch. 7.2.
- **Single-writer, reconciled (ch. 5.2):** the defaults group *is* the
  sanctioned form of "many grounds, one target". WF-026's single-writer
  principle lifts from "one rule per target" to "one defaults group — or
  one declared-unordered default group — per target"; because a defaults
  group evaluates as one writer to one outcome, ch. 5.2's confluence argument
  carries over unchanged, and the declared-unordered group is the one
  place it deliberately does not (ch. 7.2's channel is the price, paid
  loudly).

### 7.2 Outcomes: under- versus over-determination

The two failure directions stay distinct (row 8, the Catala
EmptyError/ConflictError shape), mapped onto this IR's own discipline:

- **Under-determination** — no default of any group on the target
  decides: nothing is written and the target ends as the fact base
  holds it — leeg absent an input value (ch. 7.1).
  **Decided here (a tracked deviation from Catala, `CONSTRUCTS.md`
  row 8):** Catala's catchable ∅ (EmptyError) maps to ordinary leeg,
  consistent in spirit with the ch. 1.4 empty-as-value decision but
  decided in this chapter, not by it. *Within* a defaults group, Catala's
  ∅-fallback survives structurally as fall-through; *across*
  compositions it does not — a Catala frontend must compile
  enclosing-default fallback into defaults structure, never into an
  error bit. That reconciliation is a recorded Phase-5 obligation.
  Honest inexpressibility stays with `Unresolved` (WF-044) — a default
  never masks an Unresolved node (row 7, the OpenFisca lesson).
- **Over-determination** — two defaults groups of one **declared-unordered
  group** on one target both decide and the decisions differ: the
  group **does not write** (the target keeps what the fact base
  holds — its input value, or leeg) and one **§9.5 channel fact**
  records the conflict, keyed per ch. 5.6 as rule × subject with the
  group's target form as the rule name. That fact is an *end-state
  observation* (ch. 5.7 step 5): the ch. 5.6 regelversie read tests
  rule names, not target forms, so no existing condition reads a
  conflict fact. **Decided (2026-08-20): it stays that way in v1** —
  the conflict fact is not rule-readable. Catala's conflict is
  deliberately fatal and uncatchable; a readable conflict fact would
  exceed even that, toward meta-defeasible repair rules, and the
  statute pattern that seems to want it (samenloop) is a priority
  declaration — an ordered defaults group — not a conflict reader. Reopening
  later is additive (`spec/proposals/PHASE4-DEFAULTS.md` §6). Equal
  decisions are no conflict (idempotence, ch. 5.1) — a tracked
  deviation from Catala, which conflicts on any two simultaneous
  non-empty exceptions regardless of value equality (`CONSTRUCTS.md`
  row 8). A
  single defaults group cannot reach this outcome (ch. 7.1); the channel is
  exercised exactly by declared-unordered simultaneity — and a linear
  chain never pays it (the exercise-2 finding, now by construction).
  The exception groups nested in a default (ch. 7.9) reconcile by this same
  rule at their own level; differing exception groups propagate the conflict
  to the root — no write, one **path-keyed** §9.5 fact
  (`Onderwerp.doel/<default index path>`) distinguishing a nested conflict
  from a root one.
- **Characteristic targets**: the defaults group computes its one boolean
  decision at the fixpoint of its justification strata (ch. 7.1);
  do-not-attribute means the group derives nothing. On a
  declared-unordered characteristic group, differing decisions
  (attribute vs do-not-attribute) are over-determination like any
  other; under-determination is the ordinary absent characteristic.

### 7.3 Initialisation is the lowest default

The §9.6 initialisation (ch. 5.4) *is* a default: justification "the
target is empty in the input facts", consequence its value — the default
below the base rule. The fall-through semantics reproduces ch. 5.4's
operational account case by case: a computing default with a holding
condition and a gevuld outcome supplies the value (computed wins — 50
in the ch. 5.1 chain example); a computing default that evaluates **leeg
falls through**, so the initialisation still fills the empty target
(25 — exactly ch. 5.4's "the initialisation still matches"); an
input-supplied target fails the initialisation's own justification and
is left standing; the initialisation alone fills the empty target
(25). The worked pair of ch. 5.4 is binding unchanged. Phase 4 changes
the *justification* from mechanism (a guard) to declaration (the
lowest default), so the Drools lowering derives the guard instead of
embodying it.

### 7.4 What defeasibility is *not* — and what it must not break

- **Rule-version selection is temporal, not defeasible** (ch. 8.2
  gating; WF-021 keeps the windows disjoint). A later version does not
  "override" an earlier one at the same date — they never co-apply.
  Keeping these mechanisms apart is normative; conflating them is how
  salience leaked into v0.
- **Stratification extends to displacement, it is not replaced by
  priority.** Which default decides depends on *every* default's
  justification — a read serves its own default and the displacement of
  every default below it alike — so **all default-justification reads
  join the ch. 6 dependency graph as negative edges** toward the group's
  write, regardless of whether they test characteristics or attributes
  (the conservative reading: the group decides once, at the fixpoint
  of all its justification producers). This is an **extension of the
  ch. 6.1 edge classes** (which today carry only negated
  characteristic reads and channel tests) that lands together with the
  prioritized-defaults node. The defaults group is where a
  condition's failure becomes observable displacement, so the group's
  edges carry the negativity; outside a group a write whose condition
  fails is given back (ch. 6.1, "A write is read at the fixpoint"). WF-045/046's *principles* — no negative
  edge on any cycle; strata exist — apply to the extended graph;
  WF-051 states exactly that. A defaults group whose defaults cyclically
  depend on the group's own output is thereby ill-formed — no salience-like
  escape hatch. (Within one session evaluation there is still no
  retraction: the group decides once, at the fixpoint of its
  justification strata, per ch. 5.3's staging.)
- **Strict-versus-defeasible tagging and cross-rule override**
  (LegalRuleML) are recorded refinements, not v1-design scope.

### 7.5 The lowering table (each row a derivation, not a mechanism)

One derived predicate carries the whole table: `decides_m` — default *m*'s
justification holds ∧ (attribute target: its consequence body derives a
gevuld value; characteristic target: always — the boolean decision).

| Declared (ch. 7) | ASP (`backends/clingo`) | Drools (the first-frontend toolchain) | Replaces |
|---|---|---|---|
| default *n* | its outcome rule carries `not decides_m` for each higher default *m* (the nested art. 4 chain of `ASP-TRANSLATION.md` ch. 6.1 is the desugared characteristic special case) | generated guard from the higher defaults' `decides` conditions | the salience −1 trick — becomes derivable output |
| fall-through on leeg (attribute) | `decides_n` is underivable when the consequence body is — absence composes | consequence-null guard | nothing (v0 cannot express it) |
| speculative default evaluation (ch. 7.1) | channel deposits of a default's arithmetic are **guarded derivations** — the `inconsistent/2` rule for a default's transient carries `decides_n` in its body (ASP has no retraction; the guard is the speculation) | evaluate the deciding default only | v0's persist-always transients (ch. 5.7) |
| initialisation default | NAF over the input fact | the empty-guard of ch. 5.4 | the hard-coded guard |
| declared-unordered group | both `decides` ∧ decisions differ → `inconsistent/2`, no `val/3` | §9.5 channel insert, no write | silent last-writer-wins |

### 7.6 Worked examples (absolute; binding on the Phase-4 cards)

1. **The art. 4 defaults group** (the Marrit dossier and its four
   single-difference variants, `ASP-TRANSLATION.md` ch. 6.2), after
   desugaring to self-contained justifications — characteristic
   defaults, so every justified default decides (ch. 7.1): refused → the
   base default decides *excluded*; refused ∧ herroepen → the middle
   default decides *do-not-attribute* and displaces the base (Marrit: not
   excluded, recht); refused ∧ herroepen ∧ vervallen → the highest
   default decides *excluded* again. Each dossier variant's absolute
   outcome as the hand-runs pin them (`ASP-TRANSLATION.md` ch. 6.3 runs match).
   These become golden cases verbatim when the prioritized-defaults node
   lands.
2. **Initialisation**: the ch. 5.4 pair (50 with the chain, 25 alone)
   unchanged, **plus** the fall-through case pinned: a computing default
   whose outcome is leeg over an empty target with initialisation 25 →
   **25**, never leeg.
3. **Conflict**: a declared-unordered group writing 10 and 20 to one
   empty target → the target stays **leeg**, the channel carries one
   fact (target form × subject); the same two defaults ordered as one
   group → **20 is the outcome iff it is the higher default** and no
   channel fact; the same group both writing 10 → **10**, no conflict
   (equal values).
4. **Per day** on a timeline target: an opgave above a computation of 20 in
   March to May 2025, the opgave 10 on January and February 2025 →
   **10** on January and February, **20** in March to May, leeg elsewhere
   (ch. 7.11, `WV-7.11-opgave-per-dag-januari-februari`).

### 7.7 The v0 desugaring

How the existing v0 rule forms read as prioritized defaults — the lowering
that retires ch. 5.4's guard as mechanism (ch. 7.3) and lets a backend
serve the old forms through the new machinery:

| v0 form(s) on one (type, attribute) target | Defaults reading |
|---|---|
| **exactly one** unconditional, version-free Initialisatie | the ch. 7.3 initialisation default (lowest) |
| plus at most one version-free computing rule (Gelijkstelling/Berekening) | plus one default above it: justification = its condition (an unconditional rule gets the vacuously true empty `all`), consequence = its value |

A cell of a dimensioned attribute is a target of its own here (ch. 5.8c,
2), and the pair on a cell reads as the table says. On the wire it keeps its
two forms, since a defaults group names its attribute whole (WF-125); a
backend that lowers the pair into a defaults group names the cell in it.

The initialisation is what the desugaring exists for — and what keeps
the two readings extensionally equal on the confluent domain. A
**computation without one stays outside the desugaring** — but no longer
native-v0: the ch. 7.8 standing norm gives it the defaults reading
directly — a computed leeg leaves the input standing — with the v0
erase-on-leeg retired as a tracked delta. What this
chapter could not do silently, ch. 7.8 does openly. (The v0 erase
itself had no absence encoding on the ASP native path — the recorded
backend limitation that, with the ch. 6d fence, kept the corner free
of cases and the change cheap; see `backends/clingo/README.md`.)

Outside the desugaring's domain the v0 forms are left as they are, a
computing rule among them deciding like a default (ch. 7.8); a
consumer that cannot carry a form faults on it, and where a form's v0
meaning is order-dependent the reference interpreter faults on the
fixpoint budget — no gate ever admits a case on which the executors
disagree. Excluded: **conditional initialisations** (their justification would need the input-emptiness
guard conjoined, which is not a writable condition — WF-051 forbids
reading the own target), **versioned forms** (version selection is
temporal, ch. 7.4 — folding windows into justifications would conflate
the two mechanisms), and **multiple computing rules on one target**
(their v0 meaning is agenda order — exactly what prioritized defaults
exist to replace; ordering them needs authorial intent, not a
transformation). A single characteristic attribution is trivially its own
one-default group — recorded, not transformed.

**The pinned delta, and the mirror hole it repairs.** In the confluent
domain — the computing rule evaluates gevuld or does not fire — the
two readings agree, and that agreement is machine-checked: the anchor
`corpus/golden/anchor/jutterswet-waardering-v0vorm` carries the v0
forms, `ref/` replays them **natively** (ch. 5.4 semantics) while the
ASP backend translates the **desugared** document (`Ir_v0.Desugar`,
the table above, through the ch. 6.1 defaults machinery) — gate 6
demands both readings coincide. Outside it, a computing rule that
evaluates **leeg** beside an initialisation was *divergent* in the v0
reading: the empty write (ch. 1.2g) and the refill alternate and no
fixpoint exists, so `ref/`'s native replay faulted on the pass budget
while the desugared defaults answered cleanly — a corner only prose
could pin (a fault is not an expectation). **Closed with ch. 7.8:**
`ref/` carries the defaults reading natively — the leeg-evaluating rule
writes nothing (ch. 3.11), also beside a conditional initialisation,
the init or input decides, and
the corner is machine-pinned by the anchor's
`case5-leeg-doorval-init` (erkend, no basisbedrag → **25**) and
`case6-leeg-doorval-input` (idem over input 40 → **40**, a
reason-carrying gate-7 survivor). The desugaring therefore repairs a
hole rather than preserving it — and the repair is now witnessed
differentially instead of recorded as prose.

### 7.8 The input default and the standing norm (decided 2026-08-20)

The case input on a defaults-carrying target is itself a default —
restated as the **virtual input default**: between the lowest ordinary
default and the initialisation default sits an implicit default whose
justification is "the target is **gevuld** in the input facts" and
whose consequence is that input value — the exact dual of the
initialisation default (ch. 7.3). Like the initialisation default's, its
justification reads the input world and contributes no dependency
edges. On a characteristic target no decision removes what the virtual
input default holds: a do-not-attribute decision displaces the lower
defaults and derives nothing, which is not a retraction (ch. 7.1), so a
characteristic the case input sets stays set — the input is a source of
the characteristic beside the attribution (§3.5) — whereas on an
attribute target a deciding default replaces the input value
(ch. 7.6.2). Worked, absolute, through all three executors (gates 5, 6
and 12): the anchor `ww-herleving-inkomen/case7-invoer-staat-tegen-blokkade`,
whose blocking default decides do-not-attribute over an input
`recht op uitkering`, leaves that characteristic **present**. The virtual
input default is a re-description of ch. 7.1's "the target keeps what
the fact base holds" — every outcome of ch. 7.3 and 7.6.2 derives from
"the highest default that decides wins" alone — and it generalises beyond
declared defaults. On a timeline target it is read per day: it is
justified on a day on which the input timeline reads a value, and its
consequence is that day's value (ch. 7.11).

**The stack of a target** (decided by the maintainer 2026-11-04). Every
writer of an attribute target stands in one implicit stack of defaults,
highest first:

1. the **writers**: the computing rules (Gelijkstelling/Berekening) and the
   allocation (ch. 4) that write the target, of one rank. A writer decides
   where its condition holds and its value is gevuld; one that fires with a
   leeg value decides nothing (ch. 3.11). Where one decides, the target takes
   its value; where two decide with different values, no fact base satisfies
   both and the session faults with EF-016 (ch. 5.7);
2. the **input of the object**, the virtual input default: the value the case
   supplies, or, on an object a creation made, the value its creation assigned
   (ch. 5.5) — a creation gives the object it makes its member values as the
   case gives an input object its attributes;
3. the **initialisation** (§9.6, ch. 5.4, 7.3), justified where the input
   leaves the target empty, which decides where no writer does.

Where none of them decides, the target is leeg. A defaults group is a stack of
its own (ch. 7.1), whose virtual input default is rank 2 of this one; beside it
no writer of its level stands (`IR-SPEC.md` WF-050). On a timeline target the
stack is read per day (ch. 3.3): a writer decides the days on which its timeline
reads a value, the input stands on every other day, and two writers that decide
one day with different values are EF-016 (ch. 3.11, 5.7). The input of an object
a creation made is the value its creation assigned per day: a scalar on every day, a
timeline on its periods, the member value read on the subject as period
arithmetic (ch. 3.4, 3.5). The initialisation decides every day on which no
writer decides and the input reads no value, over all time, open segments
included (ch. 3.1); its value is period arithmetic, a scalar on every day, and
its condition is read as the condition of a rule on a timeline target (ch. 3.10).
It reads the input and the writers, never its own write, so the order of the
forms in the document does not move it (ch. 5.4). Where it decides no day its
value is not evaluated, and what that value deposits on the §9.5 channel stands
where it decides some day, as a default's does (ch. 7.11). The asOfDate-bucket
scalar of its target carries no day, so the initialisation cannot read which
days the input leaves empty: **EF-037**, attribute and label. A total of a
timeline an initialisation writes meets its open segments, and one that carries
a value other than 0 faults with EF-036 (ch. 3.6). An allocation that names a
time-dependent attribute is not well-formed (`IR-SPEC.md` WF-128): it
distributes one amount at one moment (ch. 4). The initialisation default of a
defaults group is read per day as well (ch. 7.11).

The reading follows ALEF, measured: ALEF's lazy routes, generated and
interpreted, and its generated eager route fill every day that no rule decides
and the input leaves empty with the initialisation's value, over open bounds,
also the days on which a rule reads leeg. ALEF's eager interpreter gives leeg
in spring where an initialisation stands before the rule in an imperative rule
group, an artefact of the order of its writers that this stack, which orders no
writers, does not have. ALEF admits the member value of a creation on a
time-dependent attribute and does not execute it; the reading per day above is
uchar's own (decided by the owner 2026-11-05). ALEF refuses an allocation with
a time-dependent attribute statically, as WF-128 does.

Worked example `WV-7.8-stapel-per-dag-initialisatie` (absolute; `ref/test/`
verbatim, the golden `jutterswet-initialisatie-per-dag`, each target a whole
number for every month): the initialisation "Het strandloon van een Jutter moet
geïnitialiseerd worden op 5" alone gives **5** on (−∞, +∞). Beside "Het
voorjaarsloon van een Jutter moet gesteld worden op 20 van dd. 01-03-2025 tot
dd. 01-06-2025", in either order of the two in the document, the voorjaarsloon
is **5** on (−∞, 2025-03-01), **20** on [2025-03-01, 2025-06-01) and **5** on
[2025-06-01, +∞). Beside "Het kustloon van een Jutter moet berekend worden als
zijn basisloon van dd. 01-03-2025 tot dd. 01-09-2025" with the basisloon 20 on
[2025-03-01, 2025-06-01), the kustloon is the same, also on
[2025-06-01, 2025-09-01), where the rule reads leeg. Beside "… op 20 van dd.
01-03-2025 tot dd. 01-06-2025" and "… op 30 van dd. 01-09-2025 tot dd.
01-11-2025", the initialisation "… op 5 gedurende de tijd dat hij stormwacht is"
with stormwacht on 2025 gives the stormloon **5**, **20**, **5**, **30** and
**5** within 2025 and leeg outside it; without stormwacht 20 and 30 alone
(`-zonder-stormwacht`). With the input 10 on [2025-03-01, 2025-06-01) the
initialisation "… op 5" alone gives **5**, **10**, **5**; with the input 10 on
[2025-01-01, 2025-04-01) beside the rule of 20 in spring the voorjaarsloon is
**5** on (−∞, 2025-01-01), **10** on [2025-01-01, 2025-03-01), **20** on
[2025-03-01, 2025-06-01) and **5** on [2025-06-01, +∞) (`-initialisatie-invoer`).

Worked example `WV-7.8-stapel-per-dag-creatie` (the golden
`jutterswet-creatie-per-dag`): "Een Jutter heeft een melding met meldwaarde
gelijk aan 7 en meldloon gelijk aan zijn strandloon en …", every member a whole
number for every month, read by a sum of each member over the meldingen of the
Jutter. With the strandloon 10 on [2025-01-01, 2025-07-01): the meldwaarde is
**7** on (−∞, +∞) and the meldloon **10** on [2025-01-01, 2025-07-01); beside
"… moet gesteld worden op 20 van dd. 01-03-2025 tot dd. 01-09-2025" the value 7
gives **7**, **20** on [2025-03-01, 2025-09-01), **7**, and the strandloon gives
**10** on [2025-01-01, 2025-03-01) and **20** on [2025-03-01, 2025-09-01); beside
"… moet geïnitialiseerd worden op 5" the strandloon gives **5**, **10**, **5**.
Without the strandloon (`-zonder-strandloon`) the meldloon is leeg on every day
and the initialised member **5** on (−∞, +∞).

Worked examples on the documents of `ref/test/fixtures/writers-stack`, which
the suite of the ASP backend plays as well: "Het strandloon van een Jutter moet
geïnitialiseerd worden op 5 indien hij kustwacht is" beside the rule of 20 in
spring and the input 10 in January 2025 gives **5**, **10**, **5**, **20**,
**5** (`WV-7.8-stapel-per-dag-voorwaarde`), and over the asOfDate-bucket scalar
10 of the strandloon **EF-037**, attribute `strandloon`, label `J1`
(`-emmerscalair`). The initialisation "… op (100 gedeeld door zijn deler) naar
beneden afgerond op 0 decimalen", the deler 0 on [2025-01-01, 2025-07-01) and 2
elsewhere: over the input 10 on (−∞, +∞) it decides no day, the strandloon is
**10** and the channel empty (`-niet-geevalueerd`); over the input 10 on
[2025-01-01, 2025-07-01) it is **50**, **10**, **50**, and the channel carries
the EF-005 of the initialisation (`-deponering`); over the input 10 on
(−∞, 2025-01-01) and [2025-07-01, +∞) it could decide only days on which its
value is leeg, decides none, and the channel stays empty (`-leeg`). At the
fixpoint the initialisation reads `DISPLACED` over the input on all time and
`HOLDS` in the other two cases, its value the timeline it writes
(ch. 5.12; `-lezing-verdrongen`, `-lezing`). Beside "Het jaarloon van een
Jutter moet berekend worden als het totaal van zijn strandloon", the
initialisation "… op 5" leaves the total an open segment of 5: **EF-036**,
attribute `strandloon`, label `J1` (`-totaal`); "… op 5 van dd. 01-01-2025 tot
dd. 01-01-2026" beside the rule of 20 in spring gives **5**, **20**, **5** on
2025, leeg elsewhere, and the jaarloon **105** (`-venster`). The creation of a melding with
the waarde of the scalar vangst 7 gives a terug of **7** on (−∞, +∞)
(`-creatie-scalair`); a creation that gives a sum-typed timeline the marker
`onbekend`, beside 10 in spring, gives the object it makes `onbekend`,
`waarde(10)` on [2025-03-01, 2025-06-01) and `onbekend` (`-creatie-marker`,
the golden `jutterswet-afwezigheidsmarkers-initialisatie` for an
initialisation on such a timeline); an allocation over a share for every month
is WF-128 (`-verdeling`).

Worked example `WV-7.8-stapel` (absolute; `ref/test/` verbatim, the golden
`jutterswet-invoer-naast-initialisatie` and the documents of
`ref/test/fixtures/writers-stack`, which the suite of the ASP backend plays as
well): a Vondst whose `taxatie` three forms write, "De taxatie van een Vondst
moet gesteld worden op zijn opgave indien hij gemeld is", "… op zijn schatting
indien hij erkend is" and "De taxatie van een Vondst moet geïnitialiseerd worden
op 25 indien hij gemeld is". Gemeld and erkend with taxatie 40 in the input:
with opgave 50 the taxatie is **50** (the second rule fires with leeg), with
schatting 60 **60**, with neither **40** — the input stands and the
initialisation does not write; without the input, with neither, **25**; erkend
alone and without the input, **leeg**. Beside an equal-shares allocation of 100
over B1 and B2, the initialisation "Het bergingsaandeel van een Berger moet
geïnitialiseerd worden op 5 indien hij vast is" gives B1 and B2, both vast, the
share **50**, also over B2's input 7, an unlinked vast B3 **5**, an unlinked
vast B4 with input 7 **7**, and B5, not vast, **leeg**; with the rule "Het
bergingsaandeel van een Berger moet gesteld worden op zijn vaste aandeel indien
hij vast is" in place of the initialisation and a vaste aandeel 30 on B1, the
session faults with EF-016, B1, the values 30 and 50
(`WV-7.8-stapel-verdeling-botsing`). Beside the creation "Een Jutter heeft een
melding met waarde gelijk aan zijn vangst", the initialisation "De waarde van een
Melding moet geïnitialiseerd worden op 5" finds filled the waarde 7 that the
creation assigned, and fills it where the creation assigns none: a Jutter with vangst 7 has a
terug, the sum of the waarde of its meldingen, of **7**, one without a vangst
**5**; the same with the initialisation "… op 5 indien zijn toeslag groter is
dan 0" and a creation that assigns the toeslag 1 (`WV-7.8-stapel-creatie-voorwaardelijk`).
The goldens `jutterswet-invoer-naast-verdeling`,
`jutterswet-invoer-naast-objectcreatie` and `jutterswet-invoer-op-keurklasse`
pin the stack with case input beside an allocation, beside a creation and on an
enumeration and a text target, through all three executors.

**The standing norm** is rank 1 of the stack read for a computing rule. A
**scalar computing rule (Gelijkstelling/Berekening)** — its target's only
writer or one of several, with or without a validity window — decides
**like a default**: its justification is its condition, and it decides only
when its value evaluates **gevuld**. A **leeg evaluation writes nothing**:
the target keeps what the rest of its stack gives it — another writer's
value, the input of the object, the initialisation's value, or leeg
(ch. 3.11). The maintainer decided the
norm on 2026-08-20 for a version-free rule that is its target's only
writer and widened it on 2026-11-04 to every computing rule. The v0
mirror wrote the computed leeg over the input (ch. 1.2); that erase is
retired as a **tracked delta** (`CONSTRUCTS.md` rows 8/18) — decided
openly where ch. 7.7 records it as the reason the bare form could not be
desugared silently. Channel deposits of the evaluation at the fixpoint
stand as in any ordinary evaluation (ch. 5.7) — prioritized defaults
remain the one sanctioned speculative context (ch. 7.1). Together with
ch. 7.7, every scalar form of a computation carries the defaults reading:
initialisation alone and initialisation-beside-computation through the
desugaring, every other computation through this norm.

Worked examples (absolute; the anchor `jutterswet-waardestelling`
verbatim, all four through both executors): one conditional rule "de
geschatte waarde = de taxatiewaarde, indien de Vondst gemeld is" over
a case input of 40 — deciding rule computing 50 → **50** (computation
displaces input); condition false → **40** (the flank: input stands);
condition holds, taxatiewaarde leeg → **40** (the fall-through: a
computed leeg never displaces an input value — the delta pin); no
input, computing 50 → **50**. The two standing outcomes satisfy a
do-nothing executor by construction — the standing *is* what they
test — so they are reason-carrying gate-7 survivors. In the ASP backend
the input of a target that a writer of its stack writes enters as
`inputval/3` and stands where no writer's value atom holds, an atom that
carries condition **and** value bindings (a condition-only atom would
encode the v0 erase instead — the atom's body is the semantic choice;
`spec/ASP-TRANSLATION.md` S10).
**The timeline arm:** on a timeline target the norm holds per day, for
every writer of the target, versioned or not, one or more: a writer decides
only the days on which its result timeline reads a value, and on every other
day the target keeps what the fact base holds, the case-supplied input
timeline or leeg (ch. 3.11, "Leeg in a write"; ch. 5.7, "Two writers per
day"); the maintainer decided the arm for the whole timeline on 2026-08-21
and revised it to per day on 2026-11-04 (`spec/proposals/IMPLICIT-TIME.md`
point 5). An empty result is the temporal leeg (ch. 3.1) and decides no day,
so the input stands whole; a result with a value on some days leaves the
input standing on the others. Worked, absolute — the anchor
`jutterswet-borgdeel` verbatim: a fold without a true period over the input
[2024-03-01, 2024-05-01) = 7 leaves exactly that timeline, a reason-carrying
gate-7 survivor; a fold that decides January and February 2024 with 12 over
the input [2024-03-01, 2024-04-01) = 99 yields **12** on
[2024-01-01, 2024-03-01) and **99** on [2024-03-01, 2024-04-01).

### 7.9 Nested exception groups (design — the Phase-5c gate)

*Status: normative design; the wire form is `Default.exceptions` (`IR-SPEC.md`
ch. 6.1, WF-075), an extension field, never emitter-produced. The worked
examples below are the anchors `jutterswet-nestuitzondering` (attribute
target) and `jutterswet-nestkenmerk` (characteristic target).*

Ch. 7.1's defaults group is a **chain**; ch. 7.2's declared-unordered group
is **root-level branching**. What neither expresses is **branching below the
root** — a chain whose exceptions themselves branch. Phase 5c adds it as
one concept applied recursively (`PHASE4-DEFAULTS.md` §7 option i,
ratified 2026-08-22): a default's exception slot may hold **exception
groups** — `exceptions = { unordered, members }`
(v0 `excepties = { ongeordend, leden }`), each member an `ExceptionGroup`
(a default chain, descending, highest first, inheriting the root target
and itself nesting via `Default.exceptions`). An exception group carries **no
target of its own**. Two default shapes result:

- a **form-a default** — own justification/consequence (or decision) **and**
  `exceptions`: the exception groups go first (below), and its own
  justification/consequence is the **fall-back** when they
  decide nothing;
- a **group-default** — `exceptions` with no own justification/consequence/
  decision: a chain position carrying a *multi-member base group* (or a
  further exception group), whose members reconcile as an unordered
  group.

**The recursive reading.** A default carrying `exceptions` first **reconciles
its exception groups exactly as the root group reconciles** (ch. 7.2): each
member decides independently (its own highest deciding default,
recursively), and

- if the deciding members **agree** — one member decides, or several
  decide **equal** values/besluiten — that is the exception groups' decision,
  and it **is the default's decision** (the equal-decisions rule of ch. 7.2
  holds at every level: agreeing grounds are no conflict);
- if **no member decides**, the exception groups decide nothing and the
  default **falls back**: a form-a default to its own
  justification/consequence (deciding,
  or falling through, like a plain default); a group-default decides
  nothing and the chain falls through to the next lower default;
- if two members **decide differently**, that is the branch-below-the-root
  conflict, and it **propagates to the root** (vraag 5, decided
  2026-08-22, option A): the group **does not write** (the target keeps
  what the fact base holds — its input value, or leeg) and one **§9.5
  channel fact** records it, keyed by the target form **plus the default
  index path** — `Onderwerp.doel/<defaultindexpad>` (e.g.
  `Jutter.heffing/0` for the exception groups of default 0; a root-level conflict
  keeps its bare `Jutter.heffing`). The path suffix keeps a nested conflict
  distinguishable from a root one. This is **uniform with the root
  reading** (a differing group does not write) and **Catala-faithful**:
  Catala's `⊛` propagates up the default tree (D-ContextConflictError),
  and this reading follows that propagation **minus the fatality** we
  already made observable (ch. 7.2; the deviation extends the tracked
  row-8 record in `CONSTRUCTS.md`). The initialisation default (ch. 7.3)
  and the input default (ch. 7.8) stay **root-only** — a sub-default is
  never an initialisation or input default (WF-075).

**Displacement of a lower default's exception groups.** Ch. 7.1's rule — the
highest deciding default of a chain wins, lower defaults never reached —
extends to settling **by conflict**: a default **settles** when its
exception groups decide *or* conflict. A strictly-higher settling default
(deciding or conflicting) therefore **displaces the exception groups of
every lower default entirely, including their path-keyed §9.5
fact** — displaced exception groups are never reached, so neither their
decision nor their conflict surfaces. This is the chain reading applied to the
nested form: the conflict lower exception groups *would* deposit is
speculative until their default is reached, and a higher settling default keeps
it from ever being reached. (The corollary the executors must honour:
the path-keyed `subject.target/<path>` fact of displaced exception groups
must not be emitted — a bare differing-pair signal without the
displacement wait is an incoherent model. Witnessed by
`jutterswet-nestuitzondering/case9-verdrongen-conflict`: input
`grondX + grondB1 + grondB2` settles default 0 at **40.00** on exception
groups that decide nothing, so the base group-default's
`grondB1`-vs-`grondB2` conflict at
`Jutter.heffing/1` is displaced — the group writes 40.00, consistent,
**no** `/1` fact.)

The **binding acceptance test** is `PHASE4-DEFAULTS.md` §7's spurious-
conflict example (`b ← x`, `x ← {y, z}` with only `y` applicable and `x`
justified): flattening the tree to a root group `{[y, x], [z, x]}`
manufactures a conflict — member 1 decides `y`, member 2 decides `x` —
where the paper's calculus yields **`y`** (the `z` slot is ∅, the sole
non-empty exception wins). The nested reading yields `y`, consistent: the
exception groups of `x` reconcile first, only `y` decides, and that is `x`'s
decision — no spurious conflict. Ch. 7.2's reference here: a declared-
unordered *root* group reconciles by the same rule the exception groups now
use recursively.

**Worked examples (absolute; the anchors verbatim, both target kinds).**

*Attribute target* — `jutterswet-nestuitzondering`, `Jutter.heffing`
(2 decimals). One defaults group: default 0 = form-a `grondX → 40.00` with
unordered exception groups `{ grondY → 10.00, grondZ → 20.00, grondW → 10.00 }`;
default 1 = a group-default carrying the unordered base group
`{ grondB1 → 15.00, grondB2 → 25.00 }`.

| Case | Input characteristics | `heffing` | Channel |
|---|---|---|---|
| `case1-alleen-y` | grondX, grondY | **10.00** | consistent — the spurious-conflict test: only `y` decides among the exception groups |
| `case2-alleen-z` | grondX, grondZ | **20.00** | consistent |
| `case3-verschillend` | grondX, grondY, grondZ | **leeg** | inconsistent `Jutter.heffing/0` — the exception groups of default 0 decide 10 and 20 |
| `case4-gelijk` | grondX, grondY, grondW | **10.00** | consistent — equal sub-decisions (10 and 10) are no conflict |
| `case5-terugval` | grondX | **40.00** | consistent — the exception groups decide nothing, so default 0 falls back to its own consequence |
| `case6-basis` | grondB1 | **15.00** | consistent — default 0 undecided, the base group-default decides |
| `case7-basisconflict` | grondB1, grondB2 | **leeg** | inconsistent `Jutter.heffing/1` — the base group decides 15 and 25 |
| `case8-niets` | (none) | **leeg** | consistent — nothing decides; a no-op-survivor (gate 7) |

*Characteristic target* — `jutterswet-nestkenmerk`, `Jutter.vrijgesteld`.
One defaults group: default 0 = form-a `nx → attribute (decision true)` with
unordered exception groups `{ nx ∧ ny → do-not-attribute (false), nx ∧ nz →
attribute (true) }` — each justification conjoins its carrier's `nx`
(ch. 7.1).

| Case | Input characteristics | `vrijgesteld` | Channel |
|---|---|---|---|
| `case1-y-wint` | nx, ny | **absent** | consistent — the sub-decision `false` displaces `nx`'s `true` |
| `case2-conflict` | nx, ny, nz | **absent** | inconsistent `Jutter.vrijgesteld/0` — the exception groups decide `false` and `true` |
| `case3-terugval` | nx | **present** | consistent — the exception groups decide nothing, so default 0 falls back to its own besluit `true` |
| `case4-nz-zonder-nx` | M: nz; N: nx, nz; O: ny, nz | M **absent**, N **present**, O **absent** | consistent — the justification of each exception group's default conjoins its carrier's `nx` (ch. 7.1), so without `nx` nothing decides: M and O stay absent and O's `ny` and `nz` raise no conflict; in N the `nz` group decides `true` |

*Nest depth* — `jutterswet-nestdiepte`, `Jutter.heffing` (2 decimals),
three levels deep. One defaults group: default 0 = form-a `grondX → 40.00`
with an unordered pair of sibling exception groups `{ s0 = grondY → 10.00; s1 =
grondP → 30.00 with its own unordered exception groups { grondQ → 5.00,
grondR → 25.00 } }`. `s0` and `s1` are unordered siblings — they
reconcile as a group, they do not displace each other. The path suffix
is the **default-index path**, so the innermost conflict keys at
`Jutter.heffing/0/0` (default 0 of the root chain, then default 0 of `s1`).

| Case | Input characteristics | `heffing` | Channel |
|---|---|---|---|
| `case1-diep-pad` | grondX, grondP, grondQ | **5.00** | consistent — the clean deep path: only `grondQ` decides among the innermost exception groups, and the single decision bubbles up unchanged (5.00 → s1 → default 0) |
| `case2-diep-conflict` | grondX, grondP, grondQ, grondR | **leeg** | inconsistent `Jutter.heffing/0/0` — the innermost exception groups decide 5.00 and 25.00; the deep conflict propagates through the sibling group to the root |
| `case3-terugval` | grondX | **40.00** | consistent — nothing below decides; default 0 falls back to its own consequence |

**Well-formedness and strata.** WF-050 recurses its default-shape checks
into sub-defaults (content per target kind; a group-default carries
`exceptions` and no own content; a contentless default is WF-075's
concern). WF-051 needs
**no change**: `dep_forms` folds **every descendant read** (justifications
and consequences, recursively through sub-defaults) into the root group's
negative edges, so a cycle through a sub-default read is caught unchanged
(a sub-default reading its own root target is ill-formed). WF-075 governs
the nested form itself (members ≥ 1, the unordered-multiplicity rule at
every level, no initialisation in sub-defaults, the contentless default).
The ASP lowering of the nested form — a per-path predicate family, with
value priority between the exception groups' outcome and a form-a default's own
consequence — is real design work carried by the executor card, proven by these
anchors.

### 7.10 Inheritance: the effective stack per type

*Status: normative. `IR-SPEC.md` ch. 2.2 carries the wire form
(`supertype`) and the well-formedness rules; this section carries what an
executor does with it. The anchor
`corpus/golden/anchor/jutterswet-strandrechtketen` machine-checks every
outcome below through all three executors (gates 5, 6 and 12), and the
worked examples are `ref/test/`'s verbatim.*

A subtype inherits, per target, the **whole defaults stack** of its
supertype. Its own declarations fold over the parent's **effective** stack
— not over the parent's declarations — so a three-level chain needs no rule
of its own: the grandchild's own exceptions end up above the child's, above
the grandparent's, with the base contributed by the lowest declarant that
has one.

The composition has **one fixed block order**, highest first, and **nothing
on the wire declares it** (there is no `inheritedExceptions` field, no enum
and no well-formedness rule demanding a boundary declaration):

1. the subtype's own exceptions, in their declared order;
2. the inherited exceptions, in their inherited order — an inherited
   **group-default** (ch. 7.9: `exceptions`, no own consequence) is not a
   base and rides at the bottom of this block;
3. the **base** — the subtype's own base if it has one, which *replaces*
   the inherited base, otherwise the inherited base;
4. the **initialisation** default (ch. 7.3) and the **virtual input**
   default (ch. 7.8), which are never replaced and are not the base.

The base is *the lowest ordinary default*, and only when it decides
something itself: a group-default delegates to its exception groups and is
therefore not a base, so a subtype base is inserted *below* it rather than
replacing it; and a declared-unordered member has no "lowest" at all, so
its whole block travels as one.

**Where a subtype-own and an inherited exception are both applicable and
decide differently, the subtype's own decides** — deterministically, as an
ordinary write, with **no §9.5 conflict fact**. That is override *per
target and per block*, not per attribute: an inherited exception with no
applicable subtype-own counterpart is untouched and **still outranks the
subtype's own base**. A subtype never edits an inherited exception; where
an author needs one not to apply, the expressible form is a subtype-own
exception carrying the same justification, which the block order then lets
win exactly there and nowhere else.

**The result is one effective stack per (concrete type, target), and the
group runs on instances of exactly that type.** On a timeline target the
effective stack decides per day (ch. 7.11), in the same block order. The extensional
quantification of the defaults subject (ch. 2.2) is spent in the
composition; reading the subject extensionally as well would put the
supertype's stack and the subtype's on one instance, and they would write
in turn without reaching a fixpoint. The single-writer fences read that
effective stack and never a chain-closed target key — the obligation
stated in `IR-SPEC.md` ch. 6.1.

**The ch. 7.2 conflict key names the type that DECLARES the target**, not
the instance's concrete type and not the type whose defaults group the
conflicting members sit in. A target is declared once; keying on the
concrete type would make one target report under as many names as it has
subtypes, and the ch. 5.6 channel already keys rule × subject, so the
subject distinguishes the instances.

One conflict shape survives inheritance, and it is not a boundary conflict:
a clash *inside* an inherited block that is internally unordered (a
declared-unordered set on the ancestor, inherited whole because the
descendant declares nothing on that target). It is keyed on the declaring
type and at the root. Witness `corpus/golden/jutterswet-wrakconflict`,
which puts both instance kinds on one case: the subtype instance separates
the declarant from the concrete type, and the plain supertype instance
beside it shows the key does not depend on the instance kind at all.

*Stated so it is not looked for:* a conflict of that block cannot be one
the **subtype makes applicable**. The conditions of an ancestor-declared
group are resolved at the ancestor, and member resolution walks the chain
upward only, so nothing a strict subtype declares is in scope there: an
attribute or characteristic of the subtype is refused by **WF-034**, and a
hop toward a role only the subtype fills is refused by **WF-053**, whose
by-name step resolves from the group's own declared subject. The
applicability of the block is therefore decided entirely by members the
ancestor itself declares — **and the case input sets those freely on a
plain ancestor instance**, so every combination a subtype instance can
reach, a flat one can reach too. A subtype rule constrains the subtype; it
does not free it. What inheritance changes here is the KEY, not the
reachability.

#### An ORDINARY rule is the base of its level

`ch. 7.8` already reads a bare computing rule as a default: it decides, a
leeg evaluation writes nothing, and the case input stands. **Inheritance
takes that reading at its word — an ordinary rule IS the one-member base of
the level that declares it**, and the composition above then treats it like
any other base. Two shapes, both `spec/proposals/SUBTYPING.md` §4.1:

- the rule sits on the **owner itself** while the stack is inherited —
  **rule 1**, its bare-rule branch ("the subtype declares its own ordinary
  rule *or* defaults group … the subtype's own base replaces the inherited
  base"). Inherited exceptions stay above it, exactly as they stay above a
  base a defaults group contributes;
- the rule sits on an **ancestor** of the owner — **rule 4**, "its rule is
  the base; case 1 applies": it becomes the owner's inherited base, which
  the owner's own base then replaces.

The rule's own condition becomes the default's justification, and an
unconditional rule gets the vacuously true empty `all` — the same shape the
ch. 7.7 desugaring uses, so the two readings are one form rather than two.

**An initialisation the fold takes keeps its condition.** It becomes the
initialisation default (ch. 7.3), justified where the target is empty in
the input facts **and** its condition holds. The emptiness stays the
default's kind and is written into no condition, so nothing reads the own
target: the reason ch. 7.7 leaves a conditional initialisation out of the
desugaring does not reach the fold. This justification exists inside the
composed stack only; on the wire an initialisation default carries none
(WF-050). Folded without its condition, the initialisation wrote its value
where the condition failed — measured on a verifier-clean chain, in the
reference interpreter and the Drools exec-model branch.

**Each default is read on the level that declared it.** The composition
moves an ancestor's defaults into a descendant's stack, but an own read in
a default names the type it was declared on (WF-053: without a path the
read is the subject's own, and a supertype is another type). An executor
therefore evaluates every default of a composed stack on its declaring
level, and an exception group on the level of the default that carries it;
the instance it reads is the owner's, which is an instance of that level
(ch. 2.2). Read on the owner's level instead, an inherited default that
reads an attribute of its own level faulted on the own-read check in the
reference interpreter and the ASP backend, and the Drools exec-model branch
refused such a consequence as a far-side read. Witness, absolute:
`corpus/golden/jutterswet-wrakinitialisatie` — an initialisation "2 maal
zijn gewicht indien zijn gewicht groter is dan 10" on `Vondst` and a
defaults group on `Wrakvondst` (5,00 above 50, 9,00 above 100): a
`Wrakvondst` of 8,00 stays **leeg**, one of 12,00 gets **24,00**, one of
60,00 **5,00**, one of 120,00 **9,00**, one of 12,00 with input 40,00
keeps **40,00**; a `Vondst` of 12,00 gets **24,00** from the rule itself.

**A composed stack evaluates above everything it reads.** A rule the fold
takes into a stack reads there as the default it becomes: its condition and
value reads are WF-051's negative edges toward its target (`IR-SPEC.md`
ch. 5.2), like any default's. The stack is stratified by every form it was
composed from — its declared groups and its folded rules — so it decides at
the fixpoint of what its inherited defaults and its folded rules read.
Measured before, on verifier-clean chains where a later rule computed the
read (`taxatie` = 100 − `lengte`, `lengte` = `gewicht` + 1): an inherited
exception on `taxatie` > 90 decided 9,00 in a pass that read `taxatie` as
100, and a folded base on the same read decided 7,00; with nothing in the
stack deciding at the fixpoint, the reference interpreter kept those values
where the ASP backend and the Drools exec-model branch answer leeg. The
stack had run below its own inherited group, because a folded rule
contributed its index in the rule list and that index was read as a group's,
and its folded rule's reads had been positive. Both shapes are pinned in the
ASP backend's suite, which replays them through the reference interpreter
as well.

A rule folded into a stack **must not also fire as a rule**: it is the base,
and the group writes it. The absorption is per CONCRETE type and all three
executors read it from one shared predicate, so no two of them can absorb on
different instances.

> **The fold is gated, and the gate is what keeps every flat model where it
> was.** A rule is absorbed only on a target whose CHAIN declares a defaults
> group somewhere. A bare rule with no group above or below it is the
> ordinary ch. 7.8 standing norm and enters no stack at all — which is every
> ordinary rule in a document without a supertype edge, and every rule in
> the corpus that predates this chapter.

**On an absorbed target**, two writers on one level are still not a stack:
two computing rules on one target mean agenda order, which is exactly what
prioritized defaults exist to replace (ch. 7.7), so a level carrying two
refuses by name rather than picking one. Outside the gate nothing changes —
in a flat model two computing rules on one target keep the reading they
always had, and WF-079 is what speaks about them. It speaks there per
concrete type, through the chain: outside the gate an unconditional
computing rule on a type and one on an ancestor both write the type's
instances, with nothing to rank them, so the pair is refused as two such
rules on one type are (`IR-SPEC.md` WF-079; witness
`corpus/negative/wf079-chain-unconditional-writers`). A rule beside a group the
level declares **itself** is likewise not this chapter's business: that is
WF-050's single-writer lift, refused at the document. An allocation is never
folded — this section folds ordinary rules only — so WF-026 speaks through
the chain without a gate: an allocation over a type and one over an ancestor
both write the type's instances whatever defaults groups the chain declares,
and so do an allocation and a defaults group on two levels of one chain,
which WF-050's lift refuses the same way (`IR-SPEC.md` WF-026 and WF-050;
witnesses `corpus/negative/wf026-chain-two-allocations` and
`corpus/negative/wf050-chain-assignment-beside-group`).

**A rule carrying a VALIDITY WINDOW is not folded**, and is refused by name
instead. A windowed base would put version selection inside the stack, and
ch. 7.4 keeps the two apart on purpose — "rule-version selection is
temporal, not defeasible". The refusal has to be loud rather than a silent
skip, because absorption takes the rule out of the rule circuit: a fold that
merely passed over the window would silence the writer without ever making
it a base. Measured on verifier-clean documents — the target came out
silently leeg in all three executors, and a windowed rule that was ACTIVE on
the evaluation date answered from the inherited base instead of its own
value.

#### An inherited declared-unordered set

A target carried by a **declared-unordered set** on one level and a stack on
another splits in two, and the two are not the same kind of gap:

- **one own group over an inherited declared-unordered set** is **defined**
  — §4.1 rule 3: the inherited group stays internally unordered and moves
  as one block, above the subtype's base and below its own exceptions. It
  is **not lowered in this release**, and the reason is a property of the
  wire rather than a gap in the design. That reading is a **three-layer**
  priority — own exceptions above the unordered inherited block above the
  own base — while a defaults group's members reconcile as **equals**, with
  no order between them (ch. 7.2). The own base's justification is
  unconditional, so wherever it sat it would decide whenever an inherited
  member decides, and "an inherited exception outranks the subtype's base"
  (§4.4) could not be told apart from "the base decides when nothing else
  does". The one mechanism that does express *reconcile a set, else fall
  back* is ch. 7.9's group-default, and wrapping the block in one would
  move the conflict key from the ROOT form §4.4 pins to the path-keyed
  form. It needs a layered stack this wire does not have;
- **a declared-unordered set on the OWN level**, over anything inherited,
  is **undefined**: rule 3 speaks of an unordered *parent* group and no rule
  places a subtype's own set against an inherited stack. Inventing a reading
  silently is the single thing a lowering must not do.

Both refuse loudly, in all three executors, with one named message each,
before any value exists. They are witnessed in `ref/test/` (WV-7.10-c) and
in the Drools backend's own suite (`InheritanceRefusalWitnessTest`), each
pinning a fragment of its message — **no corpus case can hold either**,
because a golden must replay and `ref` faults on both.

#### 7.10.1 Worked examples (absolute; `ref/` tests verbatim)

The model — `Strandrecht` with `grondslag`, `uitkering`,
`registratiekosten` and the characteristic `betwist`; `Wrakrecht is een
Strandrecht` adding `bergingsdiepte`; a `Kustgemeente` registering
`Strandrechten`. A bare rule `registratiekosten van een Strandrecht =
5,00`; a defaults group on `Strandrecht.uitkering` (base `= de grondslag`,
exception `= 99,00 indien betwist`); a defaults group on
`Wrakrecht.uitkering` (base `= de grondslag × 2`, own exception `= 55,00
indien de bergingsdiepte groter is dan 20`); `uitkeringstotaal = de som van
de uitkeringen van alle geregistreerde strandrechten` and `registertal =
het aantal geregistreerde strandrechten`. Every instance has
`grondslag = 10,00`.

The effective `Wrakrecht` stack for `uitkering` is, highest first: the own
55-exception, the inherited 99-exception, the own base 20 — and below it,
unreplaced, the initialisation and virtual input defaults.

| # | instance | betwist | bergingsdiepte | `uitkering` | what it proves |
|---|---|---|---|---|---|
| 1 | Strandrecht | no | — | **10,00** | the supertype's own base |
| 2 | Strandrecht | yes | — | **99,00** | the supertype's own exception |
| 3 | Wrakrecht | no | 5 | **20,00** | base override: the subtype's own base replaces the inherited one |
| 4 | Wrakrecht | yes | 5 | **99,00** | the inherited exception still outranks the subtype base — override is per block, not per attribute |
| 5 | Wrakrecht | no | 25 | **55,00** | the subtype's own exception over its own base |
| 6 | Wrakrecht | yes | 25 | **55,00**, an ordinary write, `consistent` | both exceptions applicable: the subtype's own decides, deterministically and with no conflict fact |

Rows 3 and 4 together are what a flat lowering cannot produce
simultaneously: flattening gives 20,00 for row 4 or 99,00 for row 3, never
both. Row 6 separates all three readings — child-wins writes 55,00,
parent-wins would write 99,00, and an unordered reading would write nothing
and deposit a §9.5 fact.

**The three-level composition**, on a second model, because the table above
has only two levels and "the grandchild's own exceptions above the child's,
above the grandparent's" is a claim about three. `Aa <- Bb <- Cc`, each
level declaring one exception and one base (11,00/30,00 at Aa,
22,00/60,00 at Bb, 99,00/90,00 at Cc), read on a `Cc` instance:

| characteristics held | `uitkering` | what it proves |
|---|---|---|
| none | **90,00** | the grandchild's own base replaces BOTH inherited ones |
| k1 (Aa's) | **11,00** | the grandparent's exception reaches the grandchild THROUGH the child — a fold over the parent's DECLARATIONS instead of its EFFECTIVE stack loses it and answers 90,00 |
| k2 (Bb's) | **22,00** | the child's exception, inherited one level |
| k3 (Cc's) | **99,00** | the grandchild's own exception |
| k1 + k2 | **22,00** | inside the inherited block the child's sits above the grandparent's |
| k1 + k2 + k3 | **99,00** | the grandchild's own above both — never the grandparent's base 30,00 |

**An ordinary rule in the base slot**, on a third model, because rules 1
and 4 place a rule where the tables above place a defaults group. Same
`Strandrecht` group (base `= de grondslag`, exception 99,00 on `betwist`),
with the subtype's writer a bare rule `de uitkering van een Wrakrecht = de
grondslag × 2`, and `grondslag` 10,00 throughout:

| instance | betwist | `uitkering` | what it proves |
|---|---|---|---|
| Strandrecht | no | **10,00** | the supertype's own base, untouched |
| Wrakrecht | no | **20,00** | the subtype's RULE is its own base and replaces the inherited one (rule 1) |
| Wrakrecht | yes | **99,00** | the inherited exception still outranks it — a rule in the base slot is a base like any other |

Swap the two levels — the rule on the ancestor, the group on the subtype —
and rule 4 reads the mirror: a `Wrakrecht` answers from **its own** base
while a `Strandrecht` answers **from the rule**, through the ordinary
ch. 7.8 path, because nothing on *its* chain declares a group.

Two further outcomes, on the first model, pin the extensional half:

- **the supertype rule fires for a subtype instance** —
  `registratiekosten` is **5,00** on the `Wrakrecht` as well as on the
  `Strandrecht`;
- **a supertype aggregation counts subtype instances** — with one
  `Strandrecht` (row 1) and one `Wrakrecht` (row 3) registered by one
  `Kustgemeente`, `uitkeringstotaal` is **30,00** and `registertal` is
  **2**. A non-extensional reading gives 10,00 and 1, so the case fails
  loudly against a flat implementation.

### 7.11 A timeline target: the decision per day

*Status: normative (maintainer decision 2026-11-04: Reading B per period).
The anchor of the reading is the golden
`corpus/golden/jutterswet-opgave-per-periode`, and the worked examples below
are `ref/test/`'s verbatim.*

**The rule.** On a timeline attribute target a defaults group is read per
day (ch. 3.3): on every day its decision is the decision of ch. 7.1, 7.2,
7.3 and 7.9 with every justification and every consequence read on that
day. On every day the highest default that decides that day wins.

- A default is **justified** on the periods of its justification, read per
  period as the condition of a rule on a timeline target is (ch. 3.10); a
  justification that reads no timeline holds on every day or on none. A
  form that ch. 3.10 does not read per period has no reading here either
  (ch. 3.12 row 5).
- Its **consequence** is a producer of ch. 3.11, its timeline over all
  time: a scalar holds on every day.
- It **decides** a day on which it is reached — no strictly higher default
  of its chain settled that day —, its justification holds and its
  consequence reads a value. A consequence that reads leeg on a day falls
  through on that day.
- The **initialisation default** (ch. 7.3) is justified on the days on
  which the input facts leave the target empty, the days its input
  timeline reads no value, and, where ch. 7.10 folds a condition into it,
  on those of them on which that condition holds.
- The **virtual input default** (ch. 7.8) is justified on the days on which
  the input timeline reads a value, and decides that value.
- **Members reconcile per day** (ch. 7.2, 7.9): on a day on which the
  deciding members of a declared-unordered group or of a default's
  exception groups agree, that is the decision of the day; on a day on
  which they differ, the group decides nothing that day and the conflict is
  recorded under the key of ch. 7.2 or 7.9. A conflict of exception groups
  on a day settles their default on that day and propagates to the root,
  and a strictly higher default that settles on a day displaces the lower
  defaults, their exception groups and their conflicts on that day. The
  §9.5 channel carries one fact per key × subject (ch. 5.6), whatever the
  number of days.

**The write.** The group is one writer of the target (ch. 3.11): it decides
the days on which it decides, and on every other day the target holds what
the fact base holds, its input or leeg — the virtual input default read per
day. The write boundary holds per segment (ch. 3.11): a segment that does
not fit the target's decimals faults with EF-006, as a decision that does
not fit does on a scalar target (ch. 7.1).

**The evaluation.** A default's evaluation is the evaluation of its
justification and its consequence over all time (ch. 3.11, "a whole timeline
at once"); what it deposits on the §9.5 channel stands where it decides on at
least one day (ch. 7.1, the speculative deposit). A default is evaluated where
it is at its turn on some day, and its consequence where its justification
holds on a day of its turn, so a consequence that is not evaluated raises no
fault. Its reading at the fixpoint is the reading per day of ch. 5.12 (**A
default on a timeline target**).

**The scalar target is the case of one segment.** A scalar is the timeline
(−∞, +∞) (ch. 3.1), so on a scalar target the reading per day is the reading
of ch. 7.1 to 7.10, unchanged. A group's validity window gates by the
evaluation date on a timeline target as on a scalar one (ch. 3.12 row 8).

*Well-formedness:* the timelines that a justification and a consequence read
lie on a grid that fits the target's (`IR-SPEC.md` WF-126, the arms
`justification-grid` and `consequence-grid`), and the precision gate of
WF-023 bounds a consequence per segment. A time-dependent characteristic is
no target of a defaults group (WF-086; ch. 3.12 row 7).

Worked example `WV-7.11-opgave-per-dag` (absolute; `ref/test/` verbatim, the
golden `jutterswet-opgave-per-periode`, the opgave marker of the RegelSpraak
dialect, `frontends/regelspraak/grammar/DIVERGENCES.md` §7): a Jutter's
`strandloon`, a whole number for every month, carries the marker
"(mag opgegeven worden)", so the group on it puts the input attribute
`opgegeven strandloon` above `berekende strandloon`, which the one rule "Het
strandloon van een Jutter moet gesteld worden op 20 van dd. 01-03-2025 tot dd.
01-06-2025" computes.

- The opgave 10 on every month of 2025: the strandloon is **10** on
  [2025-01-01, 2026-01-01). The opgave decides every day of 2025, and the
  computation's 20 in spring is displaced.
- The opgave 10 on January and February 2025 alone
  (`WV-7.11-opgave-per-dag-januari-februari`): **10** on
  [2025-01-01, 2025-03-01), **20** on [2025-03-01, 2025-06-01), leeg on every
  other day.
- No opgave (`-zonder-opgave`): **20** on [2025-03-01, 2025-06-01).
- The opgave of January and February beside the input 99 on the strandloon
  itself for 2025 (`-invoer-op-het-doel`): **10**, **20**, and **99** on
  [2025-06-01, 2026-01-01), where neither the opgave nor the computation
  decides and the virtual input default does.

Worked example `WV-7.11-initialisatie-per-dag` (the same golden): the
`stormtoelage`, a whole number for every month with the marker, computed as
30 "gedurende de tijd dat hij stormwacht is" and initialised on 0. With
stormwacht on [2025-10-01, 2026-01-01), the opgave 12 in November 2025 and
the input 7 on January and February 2025, the stormtoelage is **0** on
(−∞, 2025-01-01), **7** on [2025-01-01, 2025-03-01), **0** on
[2025-03-01, 2025-10-01), **30** in October, **12** in November, **30** in
December and **0** on [2026-01-01, +∞): the initialisation default is
justified on the days the input leaves empty, and in January and February the
input stands.

Worked examples on the forms of the dialect (`ref/test/` verbatim, the
documents of `ref/test/fixtures/defaults-per-day`, which the suites of the ASP
and the Drools backend play as well), each a whole number for every month:

- `WV-7.11-uitzondering`: "Het strandloon moet berekend worden als zijn
  basisloon" with the exception "… gesteld worden op 30 indien hij kustwacht
  is", kustwacht on [2025-03-01, 2025-06-01). The basisloon 10 on 2025:
  **10**, **30** in spring, **10**. No basisloon and the input 7 on
  [2024-01-01, 2026-01-01): **7**, **30**, **7** — the base reads leeg and
  falls through outside spring.
- `WV-7.11-ongeordend`: the unordered members 10 "indien hij kustwacht is"
  (kustwacht on [2025-01-01, 2025-05-01)) and 20 "indien hij stormwacht is"
  (stormwacht on [2025-03-01, 2025-07-01)), the input 5 on 2025: **10** on
  [2025-01-01, 2025-03-01), the input **5** on [2025-03-01, 2025-05-01), where
  the members differ, **20** on [2025-05-01, 2025-07-01) and **5** on
  [2025-07-01, 2026-01-01); the channel carries one fact `Jutter.toelage`.
- `WV-7.11-afwijking`: the base 5, above it 30 "indien hij kustwacht is" with
  the unordered exception groups 40 "indien … stormwacht" and 50 "indien …
  havenwacht"; kustwacht on [2025-01-01, 2025-07-01), stormwacht on
  [2025-02-01, 2025-05-01), havenwacht on [2025-04-01, 2025-06-01): **5** on
  (−∞, 2025-01-01), **30** in January, **40** in February and March, leeg in
  April, where the exception groups differ and settle their default, **50**
  in May, **30** in June and **5** on [2025-07-01, +∞); the channel carries
  one fact `Jutter.strandloon/0`.
- `WV-7.11-verdrongen-conflict`: the same, with 60 "indien hij winterwacht
  is" above it; kustwacht on [2025-01-01, 2025-07-01), and stormwacht,
  havenwacht and winterwacht in April 2025: **5**, **30**, **60** in April,
  **30** in May and June, **5**, and no fact — the higher default settles
  April, so the conflict of the exception groups below it is displaced there
  and stands on no day.
- `WV-7.11-versie`: the base 5 and the exception 30 "indien hij kustwacht is",
  both "geldig vanaf 2025", on 2025-07-01, kustwacht in spring: **5**, **30**,
  **5**.
- `WV-7.11-gevolg-buiten-de-beurt`: the base 5, above it "… berekend worden
  als zijn kustloon indien hij kustwacht is", and above that 60 "indien hij
  winterwacht is"; the case gives the kustloon as the asOfDate-bucket scalar 40
  on 2025-07-01. Without kustwacht: **5** on all time; with kustwacht and
  winterwacht in April 2025: **5**, **60** in April, **5**, and no fault — the
  consequence is not evaluated, since its justification holds on no day of its
  turn (the golden `corpus/golden/jutterswet-gevolg-buiten-de-beurt`). With
  kustwacht from June to August 2025 it is, and faults: **EF-037**, attribute
  `kustloon`, label `J1` (`WV-7.11-gevolg-buiten-de-beurt-fout`,
  `corpus/red/jutterswet-emmerscalair-gevolg`).
- `WV-7.11-deponering`: the base 5 and above it "… berekend worden als (zijn
  basisloon gedeeld door zijn deler) naar beneden afgerond op 0 decimalen
  indien hij kustwacht is", the basisloon 20 on 2025 and the deler 0 up to
  2025-04-01 and 2 from then to 2026-01-01. Kustwacht from March to May: **5**
  on (−∞, 2025-04-01), **10** on [2025-04-01, 2025-06-01), **5** on
  [2025-06-01, +∞), and one fact `Jutter.strandloon` of EF-005 — the default
  decides April and May, and its division by 0 deposits; kustwacht in March
  alone: **5** on all time and no fact — it decides no day
  (`WV-7.11-deponering-vervalt`).

Worked example `WV-7.11-initialisatie-emmerscalair` (the golden
`jutterswet-opgave-per-periode`): the case gives the stormtoelage as the
asOfDate-bucket scalar 7 on 2025-07-01, which carries no period, so the
initialisation default cannot read on which days the input leaves its target
empty: **EF-037**, attribute `stormtoelage`, label `J7`.

## 9. Sum types and match

*A v1 kernel extension (CONSTRUCTS row 36; design and ratified decisions:
`spec/proposals/PHASE5B-KERNEL.md` §2). No v0 counterpart exists, so this
chapter mirrors nothing; it is normative from first writing. The
RegelSpraak dialect lowers its absence markers to these forms (ch. 9.5).
Node forms and well-formedness: IR-SPEC ch. 2.4 (`SumType`, the keuzetype
family and its WF-087 normalisation) and ch. 4.7 (`Construct`, `Match`,
`BinderRef`, `PayloadAccess`), WF-060..064 and WF-127. `Match` is the one
node of this chapter that also serves the family's other wire variant,
the enumeration — its evaluation is ch. 9.2; everything else here is
sum-only.*

### 9.1 Constructor values

A **constructor value** is a declared constructor tag of a declared sum
type with, for a unary constructor, exactly one payload value of the
declared payload datatype. It is *gevuld* by construction, and it
**never carries a leeg payload**: a `Construct` whose argument evaluates
leeg propagates — the whole `Construct` evaluates leeg (the ch. 1.3
strict default; constructing "a grant of nothing" is not expressible,
exactly as prioritized defaults never write leeg, ch. 7.1).

Worked (the `jutterswet-beschikkingsom` anchor's declarations —
`beschikking` = `Toegekend(bedrag: 2 decimals)` | `Afgewezen` |
`Aangehouden`):

- `Construct Toegekend(1250.50)` → the value `Toegekend(1250.50)`.
- `Construct Toegekend(x)` with `x` leeg → leeg.
- `Construct Afgewezen` → the value `Afgewezen` (nullary; gevuld).

The anchor's fourth rule pins both forms absolutely through gates 5–6:
`herbeschikking` := `Construct Toegekend(borg)` (the argument a bound
attribute read). Case 1 supplies `borg` = 300.00 →
`herbeschikking` = **Toegekend(300.00)** (the unary build); cases 2–4
supply no `borg`, so the argument is leeg and the whole `Construct`
propagates → `herbeschikking` **leeg** (the leeg-argument corner, pinned
threefold).

### 9.2 Match

`Match(scrutinee, arms)` — the arms cover the declared constructors
exactly (WF-063: compile-time totality over the domain the scrutinee's type
names; WF-088/WF-089 pin that type to the declaration it reads from, and
WF-023/WF-067 pin every value written into it — a runtime "no arm" state
is unreachable on well-formed input):

1. Evaluate the scrutinee. **If it is leeg, the match evaluates leeg**
   — propagation, the ch. 1.3 default. There is no implicit leeg-arm
   and no coalesce to a default constructor (an explicit leeg-arm is a
   recorded non-goal; it would be a new three-valued form).
2. Otherwise the scrutinee is a constructor value; evaluate the unique
   arm for its tag, with the arm's binder (unary arms) bound to the
   payload value. The match's value is that body's value. A `BinderRef`
   reads the nearest enclosing binder of its name (WF-064); the binding
   is lexical and per-evaluation, never stored.

Worked (the anchor's rule — `uitkering` := match `beschikking`:
`Toegekend(bedrag)` → `bedrag` + parameter `vaste voet` (25.00);
`Afgewezen` → 0.00; `Aangehouden` → 12.00):

- `beschikking` = `Toegekend(1250.50)` → `uitkering` = **1275.50**.
- `beschikking` = `Afgewezen` → `uitkering` = **0.00**.
- `beschikking` = `Aangehouden` → `uitkering` = **12.00**.
- `beschikking` leeg → `uitkering` **leeg** (nothing is written; on a
  bare single-writer target the ch. 7.8 standing norm applies
  unchanged).

The body's own operators keep their ordinary ch. 1–2 discipline; the
match adds no empty policy of its own beyond step 1.

**The enumeration scrutinee (2026-09-01).** `Match` is the keuzetype
family's node (IR-SPEC ch. 2.4): the scrutinee may equally be
*enumeration*-typed, and WF-063 then demands the arms cover the declared
values exactly, each once, every arm nullary. The evaluation is
**literally steps 1 and 2 above**, with the label in place of the tag:

1. Evaluate the scrutinee. **If it is leeg, the match evaluates leeg** —
   the same propagation, no implicit leeg-arm, no coalesce to a default
   label.
2. Otherwise the scrutinee is an enumeration value; evaluate the unique
   arm whose label equals it (compared ci, the WF-001 identity rule
   under which the arms were checked). No binder is bound — an
   enumeration alternative carries nothing to bind.

**This step 1 is where the family's leeg difference becomes visible, and
it is deliberately not a difference in the rule.** The two variants
reach leeg by different routes: an enumeration-typed slot is an ordinary
value slot and *can hold* leeg (ch. 1.2 — the §5.6 Tabel-44 row lists
enum among every datatype, and a case may simply not supply the
attribute), whereas a sum-typed slot holds a constructor value that is
gevuld by construction and only becomes leeg by propagation (ch. 9.1).
So a leeg enumeration scrutinee is the *common* case and a leeg sum
scrutinee the *derived* one — and the match treats both identically:
leeg in, leeg out.

**The lowering.** For a *gevulde* scrutinee the meaning of an
enumeration match is the comparison chain over the labels: for arms
`v₁ → e₁, …, vₙ → eₙ` it is "if scrutinee = `v₁` then `e₁` else if
scrutinee = `v₂` then `e₂` … else `eₙ`", with `=` the ch. 1.2d
enumeration equality (ci label equality within one declaring domain,
WF-008). The chain is total by WF-063 — the final `else` is reached only
by the last declared value — and it is order-independent, the labels
being mutually exclusive. This is a *definition of meaning*, not a
required implementation: an executor may dispatch on the label directly
(both `ref` and the ASP backend do), and must agree with the chain.

Worked (`waarde` := match `staat`: `gaaf` → 10.00; `beschadigd` →
4.00; `wrakhout` → 1.00, over a `keurklasse` enumeration domain
declaring exactly those three values):

- `staat` = `gaaf` → `waarde` = **10.00**.
- `staat` = `beschadigd` → `waarde` = **4.00**.
- `staat` leeg → `waarde` **leeg** — nothing is written (the ch. 7.8
  standing norm applies unchanged), *not* the last arm and *not* 0.00.

### 9.3 Equality — structural and nominal

Two constructor values are compared only when their static types name
the same declaring sum-type domain (WF-061 — the nominal rule;
cross-sum-type comparison is ill-formed, never false). Then:

- **equal** iff the tags are equal and, for unary tags, the payloads
  are equal under the ordinary value equality of their payload
  datatype;
- constructor values are **non-numeric** for the ch. 1.2 comparison
  discipline. Equality: **any leeg operand makes `=` false** (the Tabel 38
  rule — the numeric both-empty asymmetry does *not* apply, so two empties
  are unequal). Inequality: **the uniform Tabel-44 rule** (ch. 1.2d) —
  exactly one empty operand makes `≠` waar, both empty makes it onwaar. So
  `=` and `≠` no longer both fall to false over a single leeg operand: `=`
  is onwaar there and `≠` is waar (the ratified 2026-08-28 change; the
  equality cell is untouched);
- constructor values are not orderable: `<`/`>` over a sum type is a
  typing fault, as for text and enumeration values.

Structural equality is the equality used by the three consumers that
need one: idempotent writes (ch. 5.1), timeline segment
change-detection (ch. 3.1, as amended), and the defaults'
equal-decisions-are-no-conflict rule (ch. 7.5) — two defaults both
deciding `Toegekend(1250.50)` are equal decisions, not a conflict.

Worked (the anchor's condition rules — `bevoorschot` iff `beschikking`
= `Aangehouden`; `afgehandeld` iff `beschikking` ≠ `Aangehouden`):

- `beschikking` = `Aangehouden` → `bevoorschot` waar, `afgehandeld`
  onwaar.
- `beschikking` = `Toegekend(1250.50)` → `bevoorschot` onwaar,
  `afgehandeld` waar (different tag).
- `beschikking` leeg → `bevoorschot` **onwaar** (equality: any leeg
  operand → false, Tabel 38) but `afgehandeld` **waar** (inequality:
  exactly one empty → waar, Tabel 44); pinned absolutely in the anchor's
  case 4.

### 9.4 Carriage, defaults, timelines

- **Defaults and standing.** A sum-typed attribute target changes no
  defaults semantics: the decide-test stays gevuld/leeg and never
  inspects the tag (ch. 7.1); a default wanting "decide only for
  constructor X" puts a match or a ch. 9.3 equality in its
  justification. The ch. 7.8 standing norm carries over unchanged.
- **Case carriage.** Constructor values ride the case format's value
  union (input attributes, parameters, expectations); the loaders
  fail fast on an undeclared constructor or a payload against the
  wrong datatype (the WF-059/WF-018 backstop, widened).
- **Timelines.** A sum-typed temporal attribute holds a constructor
  value per segment, in the case format as in the end state. A
  constructor of a literal is a scalar operand of ch. 3.11, which holds
  on every day, so a rule writes it in the periods of its condition; an
  own timeline of the same sum type is an operand read whole (ch. 3.4),
  so a rule that writes it copies it segment by segment in the periods
  of its condition; input, writers and EF-016 are read per day as for
  every timeline target (ch. 3.11, 5.7, 7.8), and a comparison of such a
  timeline with a constructor is a condition per period (ch. 3.10).
  Timeline *arithmetic* and temporal totals stay numeric-only
  (ch. 3.4–3.6), as for text and enumeration members. Corpus witnesses:
  the goldens `jutterswet-afwezigheidsmarkers` (ch. 9.5) and
  `jutterswet-afwezigheidsmarkers-kopie` (the copy).

### 9.5 Absence markers and the payload access

*Status: proposal of the card `absence-markers` (2026-11-04), after the
decisions of the maintainer of that day recorded in
`spec/proposals/FLAVOURS-OF-EMPTY.md`.* The IR has one absence, leeg
(ch. 1.1): not yet derived, or not supplied. Beside it a document may
give an attribute **absence markers**: values without a number that a
rule writes and that decide like a number does. **onbekend** says that
the attribute has a value that cannot be established; **niet van
toepassing** that it has none. A marker is a filled value: it is no
second absence, and ch. 1.2 does not change.

**The lowering.** An attribute of datatype T with markers is an
attribute of a sum type of its own, over a domain the producer names,
with the constructors `waarde` (payload T) and then the markers it
admits, `onbekend` and `niet van toepassing`, as nullary constructors.
The RegelSpraak dialect writes them on the declaration
(`frontends/regelspraak/grammar/DIVERGENCES.md`,
"Afwezigheidsmarkeringen"). Every rule of ch. 9.1–9.4 applies:

- **Writing.** A value v is written as `Construct waarde(v)`; a leeg v
  leaves the Construct leeg (ch. 9.1), which decides nothing (ch. 3.11).
  A marker is written as the Construct of the marker, and decides: it
  displaces the input (ch. 7.8, rank 1 over rank 2), it yields where a
  defaults group puts an opgave above it (ch. 7.1), and against a writer
  of another value on the same object, or the same day of a timeline, the
  session faults with EF-016 (ch. 5.7, ch. 3.11).
- **Testing a marker.** An equality with the Construct of the marker
  (ch. 9.3): waar where the attribute holds that marker, onwaar on leeg
  and on every other value.
- **Reading the value.** The node `PayloadAccess(base, c)` (`IR-SPEC.md`
  ch. 4.7) evaluates `base`, a sum-typed expression, and is the payload
  where the value carries the unary constructor `c`, and leeg where it
  carries another constructor or is leeg — a failed access, as a failed
  to-one navigation is leeg (ch. 1.2). On a timeline the access is read
  per day.
- **Testing for a value.** A gevuld test of `PayloadAccess(X, waarde)`
  holds where X carries a value; leeg and the markers fail it.
- **A cell.** On a dimensioned attribute with markers each cell is such
  a value (ch. 5.8c): a marker is written into a cell, a cell is tested
  for a marker and for a value, and its value is read as the
  `PayloadAccess` over the `DimensionCellRead` of the cell.

**The reading rule.** A failed access is leeg, and plus, min and maal
count leeg as 0 (ch. 1.2): an unknown amount would count on as nothing.
So every `PayloadAccess` stands under a test of its own constructor —
`IR-SPEC.md` WF-127: as the operand of a leeg or gevuld test, or after a
gevuld test of the same access, the same attribute or the same cell of
it, that holds as a conjunct before it (in the
condition of its form for the form's value; earlier in the same `all` for
a condition, whose members are read in order, ch. 5.10; in the filter of
a fold over members for the fold's per-member positions; in the condition
of an IfThenElse for its then branch). Under such a test the access never
fails, and its leeg reaches no operator.

**An aggregation over members with markers** reads each member's
payload, so its filter tests that each member holds a value ("de som van
de taxatie van alle vondsten die een waarde hebben"); the members with a
marker are no contributors, and where no member holds a value the
aggregation reads as over an empty source (ch. 1.2). *Normvraag, open:*
the alternative skips the markers as leeg contributors without a filter
in the source; this chapter takes the filter, the reading of the decision
"wie leest, toetst eerst".

**Executed.** In ref, the ASP backend and both branches of the Drools
backend: writing, testing, reading the value in arithmetic, comparisons and
a fold over members, scalar, per cell and in an object creation, the write
and the test per period and the copy of a timeline, with the readings of
ch. 5.12. Not executed, refused by name in all three: a `PayloadAccess` of a
timeline, whose test per period is a gevuld test per period without a
reading (ch. 3.12 row 5). The ASP backend and the Drools backend write a
sum-typed timeline from a constructor of a literal and from a copy of an
own timeline of the same sum type, and from nothing else.

Worked examples (absolute; `ref/test/` verbatim, the golden
`jutterswet-afwezigheidsmarkers` and its red twin
`jutterswet-afwezigheidsmarkers-botsing`). A Vondst whose `taxatie`
(2 decimals) and `opslagkosten` (whole, for every month) carry both
markers, with the rules "taxatie := zijn schatting", "taxatie :=
onbekend indien hij onleesbaar is", "taxatie := niet van toepassing
indien hij drijfhout is", "vindloon := zijn taxatie gedeeld door 10
rekenkundig afgerond op 2 decimalen indien zijn taxatie een waarde
heeft", "vindloon := 25,00 indien zijn taxatie onbekend is", "vindloon :=
0,00 indien zijn taxatie niet van toepassing is", "kostbaar indien zijn
taxatie een waarde heeft en zijn taxatie groter is dan 1000,00", for a
Jutter "getaxeerd totaal := de som van de taxatie van alle vondsten die
een waarde hebben", and "opslagkosten := onbekend van 01-01-2025 tot
01-03-2025", "opslagkosten := niet van toepassing gedurende de tijd dat
hij verkocht is", "opslagtoeslag := 7 gedurende de tijd dat zijn
opslagkosten onbekend zijn".

- `WV-9.5-payload`: the payload access to `waarde` of `waarde(300)` is
  **300**, of `onbekend` **leeg**, of leeg **leeg**.
- `WV-9.5-markeringen-schatting`: schatting 300,00 → taxatie
  **waarde(300.00)**, vindloon **30,00**, not kostbaar.
- `WV-9.5-markeringen-onbekend-verdringt-invoer`: onleesbaar, input
  taxatie waarde(40.00) → taxatie **onbekend**, vindloon **25,00**.
- `WV-9.5-markeringen-niet-van-toepassing`: drijfhout → **niet van
  toepassing**, vindloon **0,00**.
- `WV-9.5-markeringen-niets`: no input → taxatie and vindloon **leeg**.
- `WV-9.5-markeringen-onbekend-in-de-invoer`: input taxatie onbekend →
  **onbekend** (the rule from the schatting fires with leeg and decides
  nothing), vindloon **25,00**.
- `WV-9.5-markeringen-kostbaar`: schatting 2000,00 → **kostbaar**,
  vindloon **200,00**.
- `WV-9.5-markeringen-som-van-de-waarden`: vondsten with schatting
  300,00, onleesbaar and schatting 100,00 → getaxeerd totaal **400,00**;
  `-geen-waarde-in-de-som`: only the onleesbare → **leeg**.
- `WV-9.5-markeringen-per-maand`: opslagkosten waarde(10) on 2025 in the
  input → **onbekend** on [2025-01-01, 2025-03-01) and **waarde(10)** on
  [2025-03-01, 2026-01-01); opslagtoeslag **7** on [2025-01-01,
  2025-03-01). `-na-verkoop`: verkocht from 2025-06-01 as well →
  **onbekend**, **waarde(10)** on [2025-03-01, 2025-06-01) and **niet van
  toepassing** on [2025-06-01, 2026-01-01).
- `WV-9.5-botsing-scalair`: onleesbaar with schatting 300,00 → **EF-016**,
  attribute `taxatie`, label V1, the values waarde(300) and onbekend;
  `WV-9.5-botsing-per-dag`: verkocht from 2025-02-01 → **EF-016** in
  February 2025, attribute `opslagkosten`, the values onbekend and niet
  van toepassing.
- `WV-9.5-per-dag-*`: a payload access to the opslagkosten in a scalar
  slot, and a sum over the payloads of their timelines, are refused by
  name; `WV-9.5-kopie-van-een-tijdlijn`: a timeline of the same sum type
  copied whole reads **onbekend**, **waarde(10)** as its source.

On a cell (the golden `jutterswet-afwezigheidsmarkers-cel`): a Vondst whose
`gewicht` (2 decimals) is dimensioned by `seizoen` (voorjaar, najaar) and
carries the marker onbekend, with the rules "gewicht in najaar := zijn
schatting", "gewicht in voorjaar := onbekend indien hij onleesbaar is",
"najaarsloon := zijn gewicht in najaar maal 2 indien zijn gewicht in najaar
een waarde heeft", "voorjaarstoeslag := 7,00 indien zijn gewicht in voorjaar
onbekend is" and "zwaar indien zijn gewicht in najaar een waarde heeft en
zijn gewicht in najaar groter is dan 100,00".

- `WV-9.5-cel-schatting`: schatting 60,00 → gewicht in najaar
  **waarde(60.00)**, in voorjaar **leeg**, najaarsloon **120,00**, not zwaar.
- `WV-9.5-cel-onbekend-verdringt-invoer`: onleesbaar, input gewicht in
  voorjaar waarde(10.00) → **onbekend**, voorjaarstoeslag **7,00**.
- `WV-9.5-cel-zwaar`: schatting 150,00 → najaarsloon **300,00**, **zwaar**.
- `WV-9.5-cel-onbekend-in-de-invoer`: input onbekend in both cells → gewicht
  in najaar **onbekend**, najaarsloon **leeg**, voorjaarstoeslag **7,00**.

In an object creation (the golden `jutterswet-afwezigheidsmarkers-creatie`):
a Jutter that is actief makes a vondst with the taxatie onbekend, one that is
ervaren a vondst with the taxatie 12,50, a drijfhout vondst has the taxatie
5,00; the Jutter counts its vondsten whose taxatie is onbekend and sums the
taxatie of those that hold a value.

- `WV-9.5-creatie-actief`: **1** onbekend, the sum **leeg**;
  `-ervaren`: **0**, **12,50**; `-beide-met-drijfhout`: actief, ervaren and a
  drijfhout vondst → taxatie **waarde(5.00)**, **1**, **17,50**;
  `-drijfhout-verdringt-onbekend`: two vondsten with the input onbekend, one
  drijfhout → **waarde(5.00)** and **onbekend**, **1**, **5,00**.

The copy (the golden `jutterswet-afwezigheidsmarkers-kopie`): "opslagkopie :=
zijn opslagkosten" and "verkoopkopie := zijn opslagkosten gedurende de tijd
dat hij verkocht is", beside the writers of the opslagkosten above.

- `WV-9.5-kopie`: opslagkosten waarde(10) on 2025 → opslagkopie
  **onbekend** on [2025-01-01, 2025-03-01), **waarde(10)** on [2025-03-01,
  2026-01-01); `-na-verkoop`: verkocht from 2025-06-01 → opslagkopie as the
  opslagkosten, verkoopkopie **niet van toepassing** on [2025-06-01,
  2026-01-01); `-invoer-naast-de-kopie`: opslagkosten waarde(10) on
  [2025-04-01, 2025-08-01) and the input opslagkopie waarde(4) on 2025 →
  opslagkopie **onbekend**, **waarde(4)** on [2025-03-01, 2025-04-01),
  **waarde(10)** on [2025-04-01, 2025-08-01), **waarde(4)** on [2025-08-01,
  2026-01-01).

## 10. Folds and function values

*A v1 kernel extension (CONSTRUCTS row 10; design and ratified
decisions: `spec/proposals/PHASE5B-KERNEL.md` §3). No v0 counterpart
exists; this chapter is normative from first writing. Node forms and
well-formedness: IR-SPEC ch. 2.4 (`ListType`), ch. 2.11
(`functionDefs`) and ch. 4.8, WF-065..070. DMN's Business Knowledge
Model + invocation is exactly the ch. 2.11 + `Invoke` pair.*

### 10.1 List values

A **list value** is an ordered, possibly empty sequence of element
values of one declared element datatype. A list value is *gevuld* —
also when empty: `[]` is a value, leeg is the absence of one (the
distinction the fold identity and the case format both need). Element
positions hold values, never leeg — "a list with holes" is not a v1
notion: a `ListLiteral` whose element evaluates leeg **propagates**
(the whole literal evaluates leeg, the ch. 1.3 strict default), and
the case loader rejects a leeg element. This is also why binders never
see leeg inside fold bodies. List equality is elementwise and in
order; lists are not orderable.

**The closed-body leeg-free invariant.** Every operand a closed lambda
or function body reads is *known non-leeg* at the point the body runs,
so the body's ch. 1–2 operator discipline never has to coalesce a
missing operand mid-fold. The justification is exhaustive on the
operand kinds a closed body admits: *binders* are never leeg (a list
element carries no hole — the propagation above — and a leeg `init` or
a leeg `list` **source** propagates *before* any body evaluation, so no
`f(acc, x)` step is ever entered with a leeg `acc` or `x`); *literals*
are never leeg; and a *parameter* read is the one place an absence
could enter — a declared-but-absent **non-list** parameter FAULTS
(fail-fast, ch. 5.7, unchanged), so it never reaches a body as leeg,
while a declared-but-absent **list** parameter reads leeg and that
leegte propagates on **source position** (the `list` operand) *before*
any body runs, never as a body operand. Emptiness therefore lives only
at the fold's boundary (init, source), never in an operand the interior
reads. An operation in the body can still yield leeg where it deposits a
§9.5 fact — a division by a numeric zero (ch. 1.4 point 4), a negative
radicand — and the body's ch. 1–2 discipline then governs that leeg as
it does any other: `acc + grondslag ÷ x` over [2, 0] with grondslag 60
folds to 30, since `plus` reads the leeg quotient as 0, with one fact of
kind EF-005 (the golden `closed-runtime-division-by-zero`).

### 10.2 Fold, MapList, FilterList, ListLength

- **Fold**: `fold(f, init, [x1..xn]) = f(…f(f(init, x1), x2)…, xn)` —
  a **left** fold, order normative. `fold(f, init, []) = init`. A leeg
  `list` operand propagates leeg; so does a leeg `init`. Inside the
  body the ordinary ch. 1–2 operator discipline governs — the fold
  node adds no empty policy of its own (compositionality; the
  aggregation table of ch. 1.2 stays the instance world's own).
- **MapList**: elementwise, order-preserving; a leeg source propagates.
  A body evaluation that yields leeg propagates — the whole map
  evaluates leeg (no holes, ch. 10.1).
- **FilterList**: keeps, in order, the elements for which the
  condition holds with the binder bound to the element — the
  ch. 3.4a aggregation-filter discipline per element; a leeg source
  propagates. A filter that excludes everything yields `[]`, a value.
- **ListLength**: the element count; `[]` counts 0; a leeg source
  propagates leeg.
- **IfThenElse** (inside lambda, function or — the Phase-5c widening —
  `Let` bodies, ch. 10.6): evaluate the condition under the
  ordinary ch. 1.2 comparison discipline, its operands disciplined to
  bound-non-leeg reads by WF-077; evaluate exactly the chosen branch —
  strict, no coalesce, the default-justification mirror.
- **Invoke**: evaluate the arguments strictly (a leeg argument
  propagates leeg without entering the body), bind, evaluate the
  closed body. Recursion does not exist (WF-067), so termination is
  structural.

### 10.3 Worked examples — the `jutterswet-staffelvergoeding` anchor

Parameter `staffelbedragen` : list of Number(2 dec) ; parameter
`plafond` = 40.00. Rule: `vergoeding` := Fold over `staffelbedragen`,
init 0.00, body `(acc, x) ↦ if acc + x ≤ plafond then acc + x else
acc` (the capped sum — the `IfThenElse` body).

- `staffelbedragen` = [12.50, 30.00, 7.25] → 0+12.50 = 12.50 (≤ 40);
  12.50+30.00 = 42.50 (> 40, keep 12.50); 12.50+7.25 = 19.75 (≤ 40) →
  `vergoeding` = **19.75**. The left-to-right order is load-bearing:
  the reordered list [30.00, 12.50, 7.25] would yield 37.25 — the
  anchor pins the order.
- `staffelbedragen` = [] → the fold yields its init: `vergoeding` =
  **0.00** (the empty list is a value).
- `staffelbedragen` absent (leeg) → `vergoeding` **leeg** (propagation
  — nothing is written; a reason-carrying no-op survivor). This reads
  leeg because `staffelbedragen` is **list-typed**: an absent list
  parameter reading leeg is the **only** widening of the ch. 5.7
  fail-fast binding, and it yields **source-position** emptiness alone
  — it propagates on the `list` operand before the body runs (the
  ch. 10.1 invariant), never as a body operand. Every other
  declared-but-absent parameter still FAULTS (ch. 5.7). The anchor pins
  exactly this: case3 omits only `staffelbedragen`; the scalar
  `plafond` remains supplied, so its own absence would fault rather than
  read leeg.

### 10.4 Worked examples — the `jutterswet-aanspraaktelling` anchor (composing ch. 9)

Sum type `aanspraak` = `Erkend(bedrag: 2 dec)` | `Vervallen`; input
attribute `aanspraken` : list of `aanspraak`. FunctionDef
`aanspraakwaarde(a: aanspraak) : Number(2 dec)` = match `a`:
`Erkend(b)` → `b`; `Vervallen` → 0.00. Rules: `erkende som` :=
Fold(+, 0.00) over MapList(`aanspraken`, `Invoke aanspraakwaarde`);
`aantal erkend` := ListLength(FilterList(`aanspraken`, `a` ≠
`Construct Vervallen`)).

- `aanspraken` = [`Erkend(10.00)`, `Vervallen`, `Erkend(2.50)`] →
  MapList yields [10.00, 0.00, 2.50]; `erkende som` = **12.50**;
  FilterList keeps [`Erkend(10.00)`, `Erkend(2.50)`] (the ch. 9.3
  inequality per element); `aantal erkend` = **2**.
- `aanspraken` = [] → `erkende som` = 0.00; `aantal erkend` = 0 (both
  written — `[]` is a value).
- `aanspraken` absent → both **leeg** (propagation; no-op survivors).

### 10.5 Carriage and boundaries

Case carriage: list values ride the case value union as
`{ "list": [ …values… ] }` in input attributes, parameters and
expectations; the loaders fail fast on an element against the wrong
element datatype and on a leeg element. Storable list-typed
*attributes* exist (scalar-carried; no timelines, WF-065). An
absent declared parameter binds fail-fast (ch. 5.7) with the **one**
exception this chapter needs: an absent **list-typed** parameter reads
leeg, and that leegte is confined to **source position** — it
propagates on the `list` operand before any body evaluation (ch. 10.1),
never as a fold-body operand, and no scalar absence is ever read leeg
(which keeps ref and Clingo in step: a scalar absence would fire ref's
else-branch where Clingo derives nothing). The ASP
side reifies a list value as indexed facts and runs the fold as index
recursion with the S6/S8 range fences; every fold-family form of this
chapter translates — the differential (gates 5 ≡ 6) covers the whole
kernel, and whatever a future corner cannot carry lands as a loud
fault in the ASP inventory, never a silent skip.

### 10.6 Let — the value-level binding, call-by-need

*A v1 kernel extension (Phase-5c; design and ratified decision:
`spec/proposals/PHASE5C-TUTORIAL.md` §6, question 2 = A). The semantics
unified to **call-by-need with the leeg-barrier at the read** by
maintainer decision O3 (2026-09-01), replacing the strict binding this
chapter stated before; the change is measured **pin-neutral** — 399 of
399 corpus cases evaluate identically. Node form and well-formedness:
IR-SPEC ch. 4.10, WF-076/WF-077. Normative from first writing.*

A `Let` binds its `binder` to its `value` as a **need**: one evaluation,
performed at the **first read** of the binder and reused by every further
read. Three clauses, all normative.

1. **Lazy, and once.** The `value` is evaluated in the enclosing scope —
   the binder is not visible in its own `value` (WF-076) — at the moment
   the `body` first **reads** the binder, and that single result serves
   every further read. Evaluating it exactly once is observable: a value
   whose evaluation deposits a §9.5 channel reason (ch. 5.6) deposits it
   **once**, however many times the binder is read.
2. **The leeg-barrier, at the read.** A read of a binder whose `value`
   evaluates **leeg** makes the **whole `Let`** evaluate leeg. The read
   never yields leeg — it ends the `Let`. The barrier belongs to the
   `Let` that bound the binder that was read: a nested `Let` empties
   *itself*, never an enclosing one.
3. **An unread binder evaluates nothing.** A `body` that completes
   without reading the binder never evaluates the `value`: no leeg, no
   fault, no §9.5 channel reason — and the `Let`'s outcome **is** the
   body's.

Clause 2 is what keeps a `Let`-body binder **never leeg**: every read
either yields a gevuld value or ends the `Let`. That is what makes such a
binder a safe operand for an `IfThenElse` condition (WF-077), and it
keeps ref's leeg propagation coinciding with clingo's underivability —
an underivable value atom simply never binds *at the read site*, so
exactly the reading positions die (`ASP-TRANSLATION.md` S18(a)). The
NAF-free exact-complement branch encoding is untouched: a condition's
operands are still total. What moved is only **where** the barrier sits
— the read place, not the bind place — and with it clause 3, which is
the "niet onnodig berekenen" of the source language's §11.1.

The binding is **not** a coalesce, and that is the point. Compare,
over an absent `inkomen` (leeg):

- `inkomen maal 0,21` — a bare `maal`: empty **coalesces** to 0
  (the ch. 1.2 coalesce table's `maal` row), so the product is **0**.
- `Let ink = inkomen in ink maal 0,21` — the body **reads** `ink`, so
  the barrier fires: the whole `Let` is **leeg**.

The two disagree on exactly the empty input. Routing a read through a
`Let` whose binder the expression then reads is how an author (or the
Catala frontend) chooses propagation over coalescing; WF-077 makes that
routing mandatory for `IfThenElse` condition operands, so the branch
guard is always a definite waar/onwaar over non-leeg operands (the
NAF-free exact-complement encoding).

**Worked example — call-by-need against strict (absolute; `ref/` tests
verbatim).** One instance of an object type carrying whole-number inputs
`a`, `d` (both **absent**) and `g` = 100.

| # | expression | value | why — and what strict gave |
|---|---|---|---|
| 2a | `Let x = a in x` | **leeg** | clause 2: the read fires the barrier (strict: leeg too) |
| 2b | `Let x = a in 42` | **42** | clause 3: `x` is never read, so `a` is never evaluated (strict: **leeg**) |
| i | `Let q = 100 ÷ 0 in 42` | **42**, no §9.5 reason | clause 3 over a *depositing* value (strict: one reason, EF-005) |
| ii | `Let q = 100 ÷ 0 in q` | **leeg**, exactly **one** §9.5 reason, EF-005 | the read forces the value, which deposits and is leeg (ch. 1.4 point 4), so the read ends the `Let` (clause 2) |
| iii | `Let k = 2 in Let x = a in if k > 1 then 7 else x maal 2` | **7** | the condition reads `k` on *every* branch; `x` is read only on the branch not chosen (strict: **leeg**) |
| iv | `Let q = (g ÷ d) plus 0 in q plus q` | **0**, exactly **one** §9.5 reason | clause 1, and only clause 1: `g ÷ d` deposits a reason and yields leeg, which the `plus 0` coalesces to a filled **0** — so the barrier stays silent, the second read really happens, and the single reason is the memo's doing. Without the memo: **two** reasons |

Row iv is stated with the coalesce for a reason. Over the bare
`Let q = g ÷ d in q plus q` the need is **leeg**, so the first read ends
the `Let` (clause 2) and the second read never happens: that expression
also deposits exactly one reason, but by the *barrier*, not by the
*memo*. It witnesses clause 2 twice over and clause 1 not at all. Only a
**gevuld** need can be read more than once, which is what makes the
coalesced form the once-ness witness.

Rows i, iii and iv are the differential witnesses of the change: ref and
clingo agree on each, where the strict lowering faulted on i and derived
nothing on iii. (Rows i and ii read a fault until the numeric zero went to
the §9.5 channel, 2026-10-19: i answered 42 without the fault, ii faulted.)

**Worked example — the `jutterswet-tariefstelling` anchor.** Object
type `Jutter` with `inkomen` (2 dec, input), `kindertal` (whole, input),
and derived `aanslag` (2 dec), `factor` (4 dec), `gewogen` (2 dec).

- `aanslag` := `Let ink = inkomen in Let aftrek = kindertal × 250,00 in
  Let belasting = (ink × 0,2100) afgerond op 2 (rekenkundig) in
  if aftrek > belasting then 0,00 else belasting − aftrek` — a `Let`
  chain whose innermost body is the `IfThenElse`, its condition reading
  only the binders `aftrek`, `belasting` (WF-077).
- `factor` := `1,0000 + kindertal ÷ 2,0000` (pure decimal arithmetic —
  whole `kindertal` ÷ a 4-dec literal, the mixed-decimals family).
- `gewogen` := `(Let ink2 = inkomen in ink2 × factor) afgerond op 2`.

| case | inputs | `aftrek` | `belasting` | `aanslag` | `factor` | `gewogen` |
|---|---|---|---|---|---|---|
| 1 | inkomen 20000,00; kindertal 2 | 500,00 | 4200,00 | **3700,00** (else: 4200 − 500) | 2,0000 | **40000,00** |
| 2 | inkomen 1000,00; kindertal 3 | 750,00 | 210,00 | **0,00** (then: aftrek > belasting) | 2,5000 | 2500,00 |
| 3 | inkomen **absent**; kindertal 2 | — | — | **leeg** | 2,0000 | **leeg** |

Case 3 is the barrier pinned: `ink` is read (inside `belasting`'s value,
which the `IfThenElse` condition reads on both branches), so the barrier
fires and the whole `aanslag` chain is leeg — and likewise reading
`ink2` makes `gewogen` leeg. Every binder in this chain is read, which
is why call-by-need reproduces the strict outcome here exactly.
**Without** the `Let`, the bare `inkomen × 0,2100` would *coalesce* to 0
and `belasting`/`gewogen` would compute a spurious `0,00` — this is why
the discipline exists. `factor` still writes `2,0000` (it reads
`kindertal`, which is present), so case 3 **decides** and is **not** a
defeasible no-op survivor.

**Tracked delta 1 — the RegelSpraak variable part inherits the barrier
(a v1 divergence, LANDED 2026-09-01).** The v0 toolchain has no wire
`Let`: its frontend *inlines* the variable part (the source language's
`Daarbij geldt:`, §11/§13.4.2) by substituting the resolved subtree at
every use site, so a variable whose expression reads an absent attribute
**coalesces** per use site exactly as a bare read would (the `maal` row
above), and sharing is lost. uchar's own RegelSpraak frontend now lowers
that variable part to a `Let` chain instead (CONSTRUCTS rows 24 and 10,
one construct under two names; `IR-SPEC.md` ch. 4.10), which gives it
the leeg-barrier of clause 2: a use site that *reads* a variable whose
expression evaluates leeg empties the whole rule value instead of
coalescing to 0. That is a **deliberate v1 divergence** from the v0
inlining-coalesce, tracked in `spec/CONSTRUCTS.md`, never a silent
rewrite — the divergence is *witnessed* by
`corpus/golden/jutterswet-vondstrecht` (a `Daarbij geldt:` variable
reading an absent attribute: the target stays leeg where the inlined
form would have computed the bare `grondrecht`), and the same discipline
is pinned by `corpus/golden/anchor/jutterswet-tariefstelling` case3,
`corpus/golden/synth3-drempelheffing` case4 and
`corpus/golden/synth3-letbeslag` case4. Clause 3 is the *other* half of
the divergence and the friendlier one: the inlined form recomputes a
variable at every use site and computes it even where no use site is
reached, while a `Let` computes it once and only if some read demands it
— the source language's own "niet onnodig berekenen" (§11.1);
`corpus/golden/jutterswet-lichterloon` pins the once (one `Let`, two
reads) and `corpus/golden/synth3-takbarriere` pins the only-if (an
unread binder in the branch ch. 10.2 does not take raises no barrier,
so the target is written).

The lowering is not total, and the wire is why: a rule's condition and
its value are separate fields, so no `Let` can span them. A variable
read **only** in the voorwaarde is therefore still substituted there,
which costs sharing but no value — a variable read twice in one
condition is evaluated twice, where one `Let` would evaluate it once
(clause 1); no case observes it, though a definition that deposited a
§9.5 channel reason would deposit one per read. A variable **no** read
reaches gets no `Let` at all (clause 3 makes it a no-op). And a read
anywhere inside a **shape-reading construct** — an aggregation or a §7
temporal operator — is substituted *there*, because the frontend
resolves such a construct against the syntactic shape of its operand and
an opaque `BinderRef` would degrade it to an unresolved marker. All
three are stated normatively for the frontend in `IR-SPEC.md` ch. 4.10
and witnessed together by `corpus/golden/jutterswet-drempelvangst`.

The third is a **construct** boundary, not a position one, and that is
what bounds this delta. Substitution is switched on for the *whole*
aggregation sub-tree, so **inside** it the v0 inlining-coalesce still
governs and the barrier of clause 2 does not reach — including in
positions that are pure *value* positions and decide nothing about
shape. This delta is therefore a statement about reads **outside** an
aggregation sub-tree.

**Worked counter-example — the boundary as a value (absolute; measured
through `ref/`).** Object type `Jutter` with `bonusgrond` (whole number,
input) and derived `totaal`; one `Jutter` with **no** vondsten and an
**absent** `bonusgrond`.

  `totaal` := *de som van het gewicht van alle vondsten van de Jutter of
  de bonus plus 1 als die er niet zijn*, with
  *Daarbij geldt: de bonus is de bonusgrond van de Jutter*.

| wire | `totaal` |
|---|---|
| what the frontend emits — **no** `Let` at all: the only read of `de bonus` sits in the §5.8.2 empty-value fallback (the aggregation's `emptyFallback`), a pure value position under the aggregation, so it is substituted | **1** — the absent read coalesces to 0 inside the `plus` (the ch. 1.2 `plus` row), exactly as v0 |
| the same IR with that `emptyFallback` wrapped as `Let bonus = bonusgrond in bonus plus 1` | **leeg** — the read fires clause 2 and empties the rule value |

The barrier would have promised the second row; the frontend emits the
first. Both rows are `ref/`-measured on the identical case, and the
frontend's own suite pins the emitted wire
(`VariablePartLoweringTest.the_boundary_is_the_whole_aggregation_construct_also_its_pure_value_positions`).

**Tracked delta 2 — Catala's `let` is call-by-value (a fidelity delta).**
Catala evaluates a `let … in …` eagerly: the bound expression is computed
before the body runs, whatever the body reads. The `frontends/catala`
plugin lowers it to this chapter's call-by-need `Let`, so the two differ
exactly where a bound expression is **unread** and its evaluation would
have been observable — an empty result, a fault, or a §9.5 channel
reason. Over the corpus the difference is **unobservable**: 0 of 399
cases move (every Catala-golden binder is read). The delta is recorded
here rather than hidden; a fixture that made it observable would be a
frontend question, not a semantics one.

## 11. Record values

*A v1 kernel extension (Phase-5c core; design and ratified decision:
`spec/proposals/PHASE5C-TUTORIAL.md` §2, question 1 = A). No v0
counterpart exists — the toolchain has no source construct that produces
these forms — so this chapter mirrors nothing; it is normative from first
writing. Node forms and well-formedness: IR-SPEC ch. 2.4 (`StructType`)
and ch. 4.9 (`StructLiteral`, `FieldAccess`), WF-071..074. The worked
examples are the `jutterswet-vondstdossier` anchor's, absolute.*

The anchor's declarations, used throughout: domain `dossier` is a struct
with fields `waarde` (2-decimal number) and `telling` (whole number).
Object type `Jutter` carries `dossier` (a `dossier`), `dossiers` (a list
of `dossier`), `premie`, `totaalwaarde`, `samenvatting` (a `dossier`), and
the characteristics `dubbel`/`afwijkend`. Parameter `opslag` = 5.00.

### 11.1 Record values and the no-hole rule

A **record value** is a declared struct type's declared fields mapped to
values, each of the field's declared datatype. It is *gevuld* by
construction and **never carries a leeg field**: a `StructLiteral` whose
field value evaluates leeg propagates — the whole literal evaluates leeg
(the ch. 1.3 strict default, exactly the `Construct` (ch. 9.1) and
`ListLiteral` (ch. 10.1) no-hole rule; "a record of a missing field" is
not expressible). A `FieldAccess` on a leeg base likewise propagates
(reading a field of an absent record is leeg).

Worked (the anchor's `premie` := `dossier.waarde + opslag` **guarded by
`filled(dossier)`**, `waardekopie` := `dossier.waarde` — a bare
`FieldAccess`, **unguarded** — and `samenvatting` := `{ waarde: premie,
telling: |dossiers| }`). The guard on `premie` is load-bearing: `+`
coalesces a leeg operand to 0 (ch. 1.3), so an *unguarded* `premie` would
write **5.00** at an absent `dossier` (0 + `opslag`), not leeg — the
`filled(dossier)` condition makes `premie` leeg exactly when the record
is absent. `waardekopie` needs no such guard: a bare `FieldAccess` on a
leeg base propagates leeg (below), with no arithmetic to coalesce.

- **case1-gevuld** — `dossier` = `{waarde: 120.50, telling: 3}`,
  `dossiers` = `[{10.00, 1}, {2.50, 2}]`. `dossier.waarde` reads
  **120.50** (the `FieldAccess`), so `premie` (its guard true) = 120.50 +
  5.00 = **125.50** and `waardekopie` = **120.50**; `totaalwaarde` maps
  `d ↦ d.waarde` over `dossiers` → `[10.00, 2.50]` and folds → **12.50**
  (the `FieldAccess` inside the map's closed lambda body — the WF-069
  widening); `samenvatting` = **`{waarde: 125.50, telling: 2}`** (`premie`
  and `|dossiers|` = 2).
- **case2-leeg-lijst** — `dossiers` = `[]`, `dossier` unchanged. `premie`
  = **125.50** and `waardekopie` = **120.50** as case1; `totaalwaarde`
  folds the empty map → **0.00**; `samenvatting` = **`{waarde: 125.50,
  telling: 0}`** (`|[]|` = 0 — a value, not leeg).
- **case3-afwezig** — `dossier` and `dossiers` both absent. `premie` is
  **leeg**: its `filled(dossier)` guard is false, so the rule does not
  fire — never the 5.00 an unguarded coalescing `+` would write.
  `waardekopie` reads **leeg** directly (`FieldAccess` on a leeg base
  propagates, no guard needed); `totaalwaarde` reads **leeg** (a leeg
  list source propagates through map and fold, ch. 10.2); and
  `samenvatting` is **leeg** — the build-side no-hole witness: its
  `telling` field, `|dossiers|` over the absent list, evaluates leeg, so
  the whole `StructLiteral` is leeg (never `{125.50, leeg}`). Nothing is
  written; on bare single-writer targets the ch. 7.8 standing norm
  applies unchanged — the reason-carrying no-op survivor.
- **case4-dossier-zonder-lijst** — `dossier` = `{120.50, 3}` present,
  `dossiers` absent. `premie` fires (guard true) = **125.50** and
  `waardekopie` = **120.50**; `totaalwaarde` is **leeg** (the absent list
  propagates through map and fold). `samenvatting` is **leeg** even
  though its `waarde` field (`premie`) *is* gevuld at 125.50: its
  `telling` field `|dossiers|` over the absent list is leeg, so the whole
  `StructLiteral` is leeg — the pure `{gevuld, leeg}` no-hole witness,
  **never** `{125.50, leeg}`. This case decides (`premie`/`waardekopie`
  written), so it refutes a do-nothing executor.

### 11.2 Equality — structural and nominal

Two record values are compared only when their static types name the
**same declaring struct domain** (WF-072 — the nominal rule, the struct
sibling of ch. 9.3; cross-domain comparison is ill-formed, never false).
Then:

- **equal** iff every declared field is equal under the ordinary value
  equality of that field's datatype (fieldwise structural equality; field
  names match ci by declaration, so the comparison is positional over the
  shared declaration order);
- record values are **non-numeric** for the ch. 1.2 comparison
  discipline. Equality: **any leeg operand makes `=` false** (the Tabel 38
  rule — the numeric both-empty asymmetry does *not* apply). Inequality:
  **the uniform Tabel-44 rule** (ch. 1.2d) — exactly one empty operand
  makes `≠` waar, both empty makes it onwaar. So `=` and `≠` no longer both
  fall to false over a single leeg operand: `=` is onwaar and `≠` is waar
  there (the ratified 2026-08-28 change; the equality cell is untouched);
- record values are not orderable: `<`/`>` over a struct is a typing
  fault, as for text, enumeration and sum-type values.

Structural equality is the equality used by the three consumers that need
one: idempotent writes (ch. 5.1), timeline segment change-detection
(ch. 3.1, as amended), and the defaults' equal-decisions-are-no-conflict
rule (ch. 7.5) — two defaults both deciding `{waarde: 125.50, telling: 2}`
are equal decisions, not a conflict.

Worked (the anchor's `dubbel` iff `dossier` = `{waarde: 120.50, telling:
3}`; `afwijkend` iff `dossier` ≠ `{waarde: 120.50, telling: 3}`):

- **case1/case2/case4** — `dossier` = `{120.50, 3}` → `dubbel`
  **aanwezig** (fieldwise equal within one domain), `afwijkend`
  **afwezig**.
- **case3** — `dossier` **leeg** → `dubbel` **afwezig** (equality: leeg
  makes `=` onwaar, Tabel 38) but `afwijkend` **aanwezig** (inequality:
  exactly one empty makes `≠` waar, Tabel 44 — pinned absolutely).

### 11.3 Carriage, defaults, timelines

- **Case carriage.** Record values ride the case format's value union as
  `{ "struct": { field: value, … } }`; `list` already recurses, so a list
  of records parses with no further surface. Input **attributes** and
  **expectations** are witnessed by the anchor
  (`corpus/golden/anchor/jutterswet-vondstdossier`); the value union also
  admits a **struct-typed parameter**, which ref carries but clingo
  refuses loudly — the ASP case emission scales every scalar parameter and
  a `StructType` has no integer encoding (`unscalable datatype in the ASP
  translation`), and no corpus case exercises it — in ANY zone, scalar or
  list-typed (measured 2026-09-05 over every corpus case that supplies a
  parameter: the declared shapes are scalar `Numeriek`, list-of-`Numeriek`
  and list-of-`SumType`, and no struct one). The sibling is the SCALAR
  sum-typed parameter, and only that one (ASP-TRANSLATION S19): the LIST
  form of a sum-typed parameter is not equally uncarried — it takes the
  case emission's `ListType` branch ahead of the scalar scaling and is
  witnessed by a golden (`corpus/golden/jutterswet-meldplichtspoor`, eight
  cases replaying exactly through clingo). The loaders fail fast on an
  undeclared
  field, a field against the wrong datatype, or a leeg field (the no-hole
  rule at the boundary — the WF-059/WF-018 backstop, widened); and the
  input-precision fence recurses into record fields and list elements,
  refusing a field or element whose exact rational does not fit its
  declared decimals in **both** executors
  (`corpus/red/struct-field-overprecision`,
  `corpus/red/list-element-overprecision` — ref now matches the ASP
  backend's per-field/per-element scaling, which faulted there all along).
  The anchor's `dossier` rides as `{"struct": {"waarde": {"number":
  "120.50"}, "telling": {"number": "3"}}}`.
- **Defaults and standing.** A struct-typed attribute target changes no
  defaults semantics: the decide-test stays gevuld/leeg and never inspects a
  field (ch. 7.1); the ch. 7.8 standing norm carries over unchanged.
- **Timelines — recorded flank, not yet carried.** The value model admits
  a struct-typed temporal attribute and the case format can write one; no
  anchor exercises it, and the ASP case emission does not yet carry
  record-valued segments (it faults loudly). Until a case wants one,
  temporal record carriage is a recorded OPEN in the ASP inventory —
  never a silent divergence, the ch. 9.4 flank pattern. Timeline
  *arithmetic* and temporal totals stay numeric-only regardless
  (ch. 3.4–3.6), as for the sum-type, text and enumeration members.

## Chapters (planned)

*(none — chapters 1–11 are drafted; ch. 7 is complete, ch. 9 carries
the row-36 sum-type kernel, ch. 10 the row-10 fold kernel
(`spec/proposals/PHASE5B-KERNEL.md`) and ch. 11 the Phase-5c record
kernel (`spec/proposals/PHASE5C-TUTORIAL.md`).)*
