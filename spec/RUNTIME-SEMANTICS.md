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
| date construction / part extraction | any empty part or date propagates — day-precision extraction and the Easter builtin **Executed** (2026-08-25, both executors; ch. 3.8) | jaar uit leeg = leeg; eerste paasdag van leeg = leeg |
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
| leeg / gevuld predicates | absence — including a failed to-one navigation — **is** leeg = waar; the `negated` field is the inner, per-value negation and complements the test — negated leeg is gevuld, negated gevuld is leeg (the qualifier's own negation is `WholePeriod.negated`, ch. 3.9; which threshold a negated leeg test takes under that qualifier — touched-days or days(B) — is an OPEN norm point, and all three executors refuse the form by name: ref and the ASP backend in the same words, because neither executes any whole-period leeg/gevuld test, and both Drools branches in the loader as `Predicaat.negatie`, on that open threshold. Red case `corpus/rood/leegnegatie-heel-tijdvak` holds the refusal, and `corpus/rood/gevuldnegatie-heel-tijdvak` the same refusal of a negated gevuld test (the Drools refusal names both); their twins `corpus/rood/leegtest-heel-tijdvak` and `corpus/rood/gevuldtest-heel-tijdvak` hold the refusal of the unnegated leeg and gevuld tests, which ch. 3.9 norms and no executor runs, ch. 3.12) | "de leeftijd van zijn partner is leeg" is waar for a partnerless person; `leeg(5)` negated = waar, `leeg(leeg)` negated = onwaar (WV-1.2-neg) |

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
| dimension folds (§5.8.5) | empty cells skipped; all-empty selection = empty (som may use its fallback) | cells {Q1: 3, Q2: leeg, Q3: 2}: som over Q1–Q3 = 5; som over {Q2} = leeg |

**Eerste/laatste are ordering folds.** The articulated value fold
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
kreeg …", `Typering.datumAggregatie`) and the model is refused; only
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
tijd-precisie hebben", the same `Typering.datumAggregatie`), so the
tightening puts the wire fence and the frontend that writes to it in
agreement. An operand without a static type defers: since L2 that is a
reference whose declaration does not resolve (WF-034 reports it) or an
`Unresolved` without a `type` (an executor faults on reaching it,
WF-044). Until L2 it was the unknown sentinel, the one operand shape v0
never typed and did hand to this very fold at runtime; the sentinel left
the datatype union at L2 (B16). The
delta belongs to the `FunctieKind` (aggregations) row of
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
verdict. It carries no `bron.rgs`, and it could not: the frontend still
refuses a non-date operand at typing, so the widening is **wire-first**,
and the frontend's own doc comment still states the v0 letter it
enforces. That gap is a recorded flank, not a claim of agreement — the
precision half above is where the two DO agree.

**Timelines** carry *two different* empty rules, mirrored as found: the
per-period arithmetic of §5.1.4 coalesces an empty period to 0, while the
temporal sum of §5.8.2 skips empty operands and yields an empty period only
when *all* operands are empty there (fallback clause applicable). Worked
example — in a period where A = leeg and B = 4: §5.1.4 A + B = 4; §5.8.2
som(A, B) = 4; in a period where both are leeg: §5.1.4 gives 0, §5.8.2
gives leeg (or the fallback). The two rules coexist in v0; ch. 3 names
them apart as the **period-coalescing rule** (ch. 3.4) and the
**contributor-skipping rule** (ch. 3.5).

**Assignment.** Writing empty is a real write when it changes state (the
fixpoint machinery of ch. 5 compares old and new value; empty-over-empty
is a no-op, empty-over-value is a change). Worked example: target holds 5,
computed value leeg → the write happens, target becomes leeg; target leeg,
computed leeg → no-op. **Exception (ch. 7.8, decided 2026-08-20):** on a
standing target — a bare scalar computing rule as its target's only
writer — a computed leeg does not write and the input value stands (a
tracked delta from the v0 mirror, `CONSTRUCTS.md` rows 8/18). The
initialisation form writes only when the target
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
   remain the mirror.

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
   5 decimals;
3. the write boundary (ch. 2.4).

*v0 mirror notes:* v0 computes rational-free chains of +, −, × and the
square root at 34 significant decimal digits (IEEE 754-2008 decimal128);
the fractional-exponent power is computed at binary double precision.
These are implementation approximations of the exact semantics above; the
reference interpreter implements the exact semantics, and backends must
agree on the corpus (the differential gate is the arbiter). The
double-precision power is the weakest spot — **OPEN:** whether v1 narrows
the fractional-exponent domain or mandates a decimal-precision minimum.

### 2.2 Operators

| Operator | Semantics | Worked examples (exact) |
|---|---|---|
| plus, min, maal | rational field operations | 9 × 1/3 = 3; 0,1 + 0,2 = 0,3 (exact decimals, no binary float) |
| verminderd met | subtraction; differs from min only in ch. 1.2 empty-handling | 5 vm 3 = 2 |
| gedeeld door | exact quotient; result may be non-terminating and stays exact until a collapse point | 10 ÷ 4 = 5/2 = 2,5; 2 ÷ 3 = 2/3 (exact) |
| gedeeld door (abs-variant) | quotient truncated **toward zero at 5 decimals** — a defined collapse point, dialect operator | 10 ÷abs 3 = 3,33333; −10 ÷abs 3 = −3,33333 |
| tot de macht (integer exponent) | exact integer power; negative exponent inverts first | 7¹⁹ = 11398895185373143 (exact); (1/3)⁻¹ = 3 |
| tot de macht (fractional/derived exponent) | real power — see the ch. 2.1 OPEN | 4^0,5 = 2 |
| percentage van | `l/100 × r`, exactly | (1/3)% van 600 = 2 (exact, not 1,999…); 50% van 4,00 = 2 |
| wortel | principal square root; mandatory explicit rounding follows (§13.4.17.13) | √9 = 3; √2 afgerond op 2 (rekenkundig) = 1,41 |
| absolute waarde | magnitude; type (unit, precision, sign) passes through | \|−3/7\| = 3/7 |
| datum ± duur (plus/min over a date and a duration) | **calendar arithmetic, not field arithmetic** — ch. 3.8: the count truncates toward zero, months/quarters/years clamp to the month end; the date stands on the left of plus and min (duration ± date is ill-formed) | 2024-01-31 + 2 weken = 2024-02-14; 2024-01-31 + 1 maand = 2024-02-29 |
| tijdsduur van A tot B (in hele *eenheid*) | whole declared units between two dates, signed; the packed-month rule of ch. 3.8; the absolute variant drops the sign | tijdsduur van 29-02-2004 tot 28-02-2026 in jaren = 21 |

Division by numeric zero: hard fault in v0; decided for v1: it routes
into the §9.5 channel (ch. 1.4 point 4). A rational literal
with denominator zero is rejected at parse time.

### 2.3 Result precision (static typing)

Declared result precision per operator, over bounded operands (`maxDec` =
maximum of the operands' declared decimals):

| Operator | Declared result precision |
|---|---|
| plus, min, verminderd met | maxDec; whole iff both operands whole; left operand's unit |
| maal | **sum** of the operands' decimals (§4.4: 2 dec × 2 dec = 4 dec); not whole, as for gedeeld door and tot de macht |
| gedeeld door | maxDec, not whole, as for tot de macht — a documented **under-report**: an exact quotient has no finite inherent precision (see ch. 2.4) |
| tot de macht | maxDec, not whole |
| percentage van | precision claim dropped (0, not whole); right operand's unit |
| wortel | placeholder claim (0 dec, non-negative, unitless); the effective precision comes from the mandatory rounding |
| absolute waarde | passthrough |
| aantal | whole, non-negative, unitless |
| min/max over listed operands | least operand precision (never over-reports) |

Unbounded targets ("getal", the ch. 2.4 exemption) carry no ceiling.

Examples: 12,34 + 1,2 types at 2 decimals; 1,25 × 1,5 types at 3 decimals
(sum); 3 × 4, two whole operands, types at 0 decimals and fractional although
its value is 12, whole (a product is never whole); 10 ÷ 4 types at 0 decimals
although its value is 2,5 — the under-report ch. 2.4 closes by rule.

### 2.4 The assignment gate (§6.1.2)

**There is no implicit rounding anywhere.** Precision changes only through
an explicit rounding. At assignment:

1. Writing a value whose declared precision exceeds the target's declared
   decimals is a **static error** — the author must insert an explicit
   rounding. Example: `1,5` into a whole-number attribute: error; `1,5
   afgerond op 0 decimalen` (rekenkundig) into the same attribute: passes,
   writes 2.
2. A rounding changes the *facing* precision: `afgerond op n` makes the
   expression n-decimal (whole when n = 0).
3. An unbounded target (the "getal" precision form, `IR-SPEC.md` ch. 2.4)
   is exempt from the check.

Worked example — the exact value 2/3 into a 2-decimal attribute: with
`afgerond op 2 decimalen` rekenkundig → **0,67**; with `naar beneden` →
**0,66**. The rounding applies to the true rational, never to a
pre-truncated approximation.

*v0 mirror note (known hole, becomes a delta):* because v0 types a bare
division at maxDec (ch. 2.3), an unrounded non-terminating quotient can
statically pass the gate and then collapses defensively to 16 significant
digits at the write boundary. The spec does not bless this: **delta
(candidate WF rule, rule layer):** a value that can be non-terminating
MUST pass through an explicit rounding before a bounded write — the static
under-report is closed by rule rather than by a defensive collapse.
Verifier/corpus artifacts: OPEN.

### 2.5 Rounding

Five modes; the default when no mode is written is *rekenkundig*. No
banker's rounding exists.

| Mode (RegelSpraak) | Meaning |
|---|---|
| naar beneden | toward −∞ (floor) |
| naar boven | toward +∞ (ceiling) |
| richting nul | toward zero (truncate) |
| weg van nul | away from zero |
| rekenkundig | half away from zero (default) |

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
  with the exact num/den factors — under the exact model this needs no
  rounding at all until a collapse point (v0 carries the division leg at
  16 significant digits as a documented residual). Result carries the
  left unit.
- **Comparison across units** restates *both* sides by their exact
  canonical factors: `24 mnd = 2 jr` compares 24 = 2·12 — exact, never a
  truncated ratio.
- **maal**: a rate times its denominator unit cancels (`€/jr × jr = €`);
  two plain units form an opaque product unit (`€ × m = €·m`); a unitless
  *literal* times a unit-bearing value carries the unit through.
- **gedeeld door**: convertible units cancel to dimensionless (`€ ÷ € =
  getal`); two plain units form a rate (`€ ÷ jr = €/jr`).
- **percentage van** carries the right operand's unit (a percentage of a
  `€` value is a `€` value).
- Deliberately not modelled in v0, mirrored as scope limits: multi-level
  composite units, unit exponents (`m²`), re-decomposition of product
  units. OPEN as v1 scope questions on `CONSTRUCTS.md` row 26.

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
21600/73 — non-terminating, rounded only by an explicit or target
rounding: afgerond op 2 rekenkundig = **295,89**. Full timeline algebra:
ch. 3.

### 2.9 The pinned decimal scale — Catala decimals (Phase-5c, vraag 4-A)

*A v1 kernel decision (design and ratified decision:
`spec/proposals/PHASE5C-TUTORIAL.md` §5, question 4 = A). Frontend
mapping: the Catala plugin.*

A Catala decimal (`TRat`) is an **exact rational** — precisely the
ch. 2.1 model — so its resolved operators
(`Add/Sub/Mult/Div_rat_rat`, `Minus_rat`) map onto the existing numeric
operators and its literals (`LRat`) carry the exact lexeme; the casts
`ToRat_int`/`ToRat_mon` are a **pass-through** — the value is already
exact-rational, so the plugin emits the operand unchanged, with no cast
node of its own, and the result type is borne by the surrounding
mixed-decimals arithmetic — and no percentage (a `Number` whose unit is `%`) is involved (Catala
pre-divides — `20%` arrives as the rational `1/5`). But because the ASP
side scales every value by its declared `decimals`, an *unbounded*
decimal form would be integer-only there. The pin: a Catala-decimal
declaration carries a **finite scale `decimals = S`, with S = 4**
(chosen against the tutorial, fixed here) — the frontend maps `TRat` to
`Number{whole = false, decimals = 4, sign unspecified}` without a unit.

`ref` stays exact-rational throughout; both executors then **fault
loudly and consistently on a value that does not terminate at 4
decimals** — ref at its bounded-write gate (ch. 2.4), clingo at its
existing does-not-fit fence. This is the ch. 2.4 delta (a
possibly-non-terminating value MUST pass an explicit rounding before a
bounded write) realised for Catala decimals; a 4-dec target fed `1,0000
÷ 3,0000` faults in **both** executors (the differential holds where it
runs — the red witness `corpus/rood/decimal-nonterminating`, landing
with the executor card).

**The scale is a boundary, not a step (maintainer decision 2026-08-24,
vraag 3-A).** A declared scale `S` — the pin above, or any attribute's
own `decimals` — binds only a *declaration* and the *write boundary* it
guards (ch. 2.4); every intermediate value stays the exact rational of
ch. 2.1 — a `Let` binder (ch. 10.6) and a `Match`-arm result
(ch. 9.2) among them, rounded to no scale — and a declared scale is never
itself a collapse point: those stay exactly the three of ch. 2.1 — an
explicit `Rounding` node (ch. 2.5), the ch. 2.2 abs-division operator, and
the ch. 2.4 write boundary already named. Worked example: `100 ÷
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
wrapper the money *divisions* already carry. (No existing corpus golden
contains a money multiplication, so the fix is byte-neutral; it lands
with the first fixture that exercises it. The ASP encoding must split
the scale-2 × scale-4 product so the intermediate stays inside the
int31 fences — e.g. `M × R = M × R_hi/10² + M × R_lo/10⁴`, each part
within the existing bound, the sum exact.)

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
   product of two unit-bearing operands *cancels* against a rate's
   denominator ("€/jr maal jr = €") or is the **opaque product** ("€ maal
   kg = €·kg", ch. 2.5: no canonical, convertible only to an identical
   product — what stops a restatement instead of scaling by a wrong
   factor), and in a **fourth** shape it is neither: an *unspellable rate
   factor* ("€/jr maal kg/jr" — a product whose honest unit would need a
   denominator). The producing frontend and two of the three consumers
   refuse that shape outright, each loudly (all three measured
   2026-09-04): the RegelSpraak frontend's `Typering` makes it a **type
   error** rather than a type ("een producteenheid met een noemer kan het
   Eenheid-model niet uitdrukken"), so no frontend ever writes the
   document; `ref` faults
   (`unit product not modelled: EUR * kg`); and clingo's unit fence
   refuses ahead of translation. The Drools backend, having no such fence,
   answers a **denominator-less opaque product**: its `Eenheid.product`
   keeps the numerator factors and drops the noemer, so "€/jr maal kg" and
   "€/jr maal kg/jr" both yield €·kg. That is the *conservative*
   outcome, not a claim that the product is spellable — €·kg carries no
   canonical, so no restatement site will scale it by an invented factor,
   and the value simply stands in the unit it was computed in. It is
   therefore a silent divergence from the other three on a shape the
   corpus does not carry and no frontend can emit; a `Let` carries its
   body's, and a binder read carries
   what its binding derived, unit-bearing **or unitless** — bound to a
   unitless value is not the same fact as unbound (until L2 only the
   second fell back to the binder reference's own annotation; since L2 a
   `BinderRef` carries none, and an unbound one is ill-formed,
   `IR-SPEC.md` WF-064); a value aggregation carries its first
   operand's (§4.6 *Eenheid1*); a rounding and a clamp carry their basis's.
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
unit v0 does not *derive* at all — a division (`dividedBy`), a power
(`power`), a percentage-of (`percentageOf`), an absolute value, a
root, the date/time extractors — keeps its own annotation, because clause 2
has nothing to put in its place. That is not the annotation being the
source; it is v0 having no rule, and the two consumers answer it
differently while both stay conform: the ASP translator's `eff_unit`
returns `None` at those positions and its fences (`unit_free_ok`,
`units_identical_ok`) then *fault* rather than compute with a unit it could
not derive, whereas the Drools backend has no such fence and computes with
the annotation — the behaviour it had before this clause, deliberately
unchanged. Measured 2026-09-04: **zero** unit-bearing annotations in the
corpus sit at such a position, so the class is empty today and the
difference between the two answers is unobservable. The clamp that reports
the first witness is step 6 of `scripts/check-reftype-derivation.sh`. Note
that this class is a set of *positions*, not of node kinds: `dividedBy`
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
golden+rood documents red), and the multiplicative precision follows
the stated ch. 2.3 formula only in prose (crossing it costs eleven
documents across three producers with three precision policies). The
fourth class — **positions** whose result unit v0 does not derive —
stays uncrossed and named; positions, not node kinds, for the reason
that class already gives above (`dividedBy` and `plus` are one node
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
`wf091-foldwortel-eenheid`'s negatief today, refused at the fold root on
the unit facet.

---

## 3. Timeline algebra

A time-dependent member or parameter does not hold one value; it holds a
**timeline**: a piecewise-constant partial function from days to values.
This chapter specifies the timeline value itself, its period grids, the
operations over it, and how rules produce it. Two different empty rules
run through the algebra; ch. 1.2 recorded them, this chapter names them:
the **period-coalescing rule** (§5.1.4 arithmetic, empty period = 0,
ch. 3.4) and the **contributor-skipping rule** (§5.8.2 summation, empty
contributors skipped, ch. 3.5). Everything here is v0 mirror with tracked
deltas; the v0 refusals and the chapter's OPEN decisions are collected in
ch. 3.12.

### 3.1 The timeline value

A timeline value is a finite set of **segments**. Each segment is a
half-open day interval `[van, tot)` — `van` inclusive, `tot` exclusive —
carrying one constant value of the member's element type, or carrying
**leeg**. Days covered by no segment (**gaps**, including all days before
the first and after the last segment) also read as leeg. The normative
reading function:

```
read(tl, d) = the value of the segment with van ≤ d < tot,  if one exists
            = leeg,                                          otherwise
```

- A **boolean timeline** (a time-dependent characteristic) is represented
  by its true-periods only: the segments are the periods where the
  characteristic holds, days outside them read false. Its segments carry
  no value payload.
- Granularity and period count are properties of the *declaration* (the
  `TimelineFacet`, `IR-SPEC.md` ch. 2.2), never of the value: a segment
  does not know its grid.
- Well-formed timeline values are sorted ascending by `van` and
  non-overlapping. *v0 mirror note:* every v0 producer emits sorted,
  non-overlapping segments, but v0 *reads* tolerate unsorted and
  overlapping input (first covering segment wins) — the input invariant
  is unstated there. **Delta (candidate WF rule, data plane):** overlap
  and disorder in timeline data are ill-formed; `read` never depends on
  segment order. Zero-length and reversed intervals (`tot` ≤ `van`)
  contribute nothing anywhere in this chapter.
- Two representations of "empty here" exist and are **observationally
  equivalent** under `read`: a gap, and an explicit leeg-valued segment
  (only the contributor-skipping rule of ch. 3.5 emits the latter).
  Semantics are defined over `read`; no *value-producing* operation in
  this chapter may distinguish the two representations. (The
  change-detection rule below is the one representation-sensitive
  comparison — it compares segment lists, not read-functions; that is
  what makes it a data-plane rule, not a semantics.) *v0 mirror note
  (residual):* v0's
  temporal total distinguishes an absent timeline (leeg result) from a
  present-but-segmentless one (0) — see ch. 3.6.
- Segment-wise equality — same `van`, same `tot`, equal-or-both-leeg
  value (numeric equality for numeric members; ch. 9.3 structural
  equality for constructor-valued members) — is the change-detection
  rule for timeline writes (the temporal analogue of the scalar rule in
  ch. 1.2 Assignment; evaluation model: ch. 5).

### 3.2 Period grids, buckets, and variable starting points

The five granularities (`day`, `week`, `month`, `quarter`, `year`) serve
two distinct jobs, with different v0 support:

1. **Weighting** (the temporal total, ch. 3.6) counts *whole* granularity
   units between two dates: days, weeks, months, quarters = whole months
   ÷ 3, years. All five granularities weight.
2. **Bucketing** (producers that emit one segment per calendar period:
   ch. 3.7, 3.8, 3.9) uses calendar-aligned walls: a month bucket runs
   from the first of the month to the first of the next; a year bucket
   from 1 January to 1 January. Only **month and year buckets exist** —
   the source language's bucketing operators are grammatically restricted
   to "de maand"/"het jaar", and v0 implements exactly that. Bucketing by
   day, week, or quarter is not defined (ch. 3.12).

Bucket lengths are actual calendar lengths, leap-aware: February 2024 has
29 days, 2025 has 365. There is no 360-day convention anywhere.

**Multi-period grids** (`count` ≥ 2, "voor elke 3 maanden"): declared and
carried in the model layer (WF-005), but **no operation over them is
defined** — v0 refuses every read of and write to a multi-period timeline
(the coverage-manifest family "multi-period temporal writes"; the refusal
reason is that the producer semantics — how a value spreads over an
N-unit period — are genuinely under-specified in the source language).
**OPEN** (ch. 3.12): v1 either specifies the multi-period producers or
restricts `count` to 1 at the type level.

**Variable starting points** (§9.9). A variable-start timeline buckets
per object at its **anchor** — the value of the synthetic day-precision
date attribute that the start-point determination names (`IR-SPEC.md`
ch. 3.8) and that an ordinary rule writes. Anchored buckets are

```
[ anchor + k·unit,  anchor + (k+1)·unit )      for every integer k
```

— periods exist before as well as after the anchor, and **every wall is
computed directly from the anchor** (never by incrementing the previous
wall), so a month-end anchor clamps consistently. Worked example: anchor
2024-01-31, month grid → walls 2024-01-31, 2024-02-29 (leap clamp),
2024-03-31, 2024-04-30, … — the third wall is the 31st again, which
cursor-incrementing from the clamped 29th would have lost. Date-plus-unit
arithmetic clamps to the last valid day of the target month (ch. 3.8).

A missing start-point determination is ill-formed (WF-016/WF-032); an
anchor that is *leeg at evaluation time* yields the empty timeline for
every anchored producer, and the anchor rule's own §6.8 error surfaces on
the §9.5 channel (v0 mirror: a companion rule raises it).

**Fixed starting points** (§13.3.6.3–.7). The source language lets a
timeline declare a fixed start point: a date for a week or multi-period
grid, or a day (and months) for a month, quarter or year grid. The wire
has **no field for it**: `Timeline` carries name, `variableStart`,
granularity and count, and the start-point determination (`IR-SPEC.md`
ch. 3.8) is the variable anchor only. Three start points add nothing to
the timeline written without one, so they are omitted without loss: day 1
of the month and day 1 of month 1 coincide with the month and year
buckets above; day 1 of months 1, 4, 7 and 10 is the calendar quarter,
which is what the anchorless quarter timeline already means — no quarter
bucket is defined (above), so there is no grid for it to disagree with.
Every other start point would change the grid. No wire document can
express such a source, so the producer refuses it rather than dropping
the start point: the RegelSpraak frontend refuses it at emission, by
name, and writes nothing (witness: its suite's `VastStartpuntTest`,
which also pins the lossless omission byte for byte). Worked example:
"voor elk jaar startend op dag 15 van maand 10" declares the periods
15 October – 15 October; emitted without the
start point, the reference interpreter refuses a case input over that
period as off-grid and accepts one over 1 January – 1 January, so the
omission is not neutral — the source is refused instead. **OPEN:** a v1
wire card either carries the fixed start point (the anchored bucket
formula above serves it, with the anchor a constant) or keeps the
refusal.

### 3.3 Reading a timeline — and the point-read gap

`read(tl, d)` (ch. 3.1) is the primitive every operation below is built
from. But **v0 exposes no point-in-time read to the rule author**: a
time-dependent attribute or parameter in a scalar context is refused (the
"§8.2 list-in-scalar" marker family), and the evaluation date (rekendatum)
only *gates* rules — version validity, the ch. 3.10 period condition — it
never *indexes* a timeline to a value. The only ways a timeline becomes a
scalar are the dedicated collapses: the temporal total (ch. 3.6) and the
whole-period predicate (ch. 3.9). **The refusal ends exactly where those
collapses begin:** a condition carrying the §8.2 whole-period qualifier is
not a scalar context at all, and refusing it under this clause refuses the
exit it names (ch. 3.9, "This qualifier is not a scalar context").

This is the recorded "op dat tijdstip" gap (`ASP-TRANSLATION.md` ch. 5,
finding 4; `CONSTRUCTS.md` row 11): Jutterswet art. 1 needs the value of
"erkend jutter of vast ingezetene" **at the child's birth** — a point
read at an instant that is not the evaluation date. That is doubly
inexpressible in v0: there is no point read at all, and no way to name a
non-rekendatum instant. The algebra above already defines the needed
primitive (`read(tl, d)` with `d` any date expression). **Decided
(maintainer, 2026-08-16, row 11):** v1 exposes it as a core IR node — a
temporal-read expression carrying a timeline operand and a date
operand — on the principle that the IR must carry every source-language
primitive; art. 1 and the ch. 8 vintage read (a parameter timeline read
at the evaluation date) become expressible. The reconciliation with
ch. 5.7's fail-fast vintage discipline is a division of labour: the
read keeps its ch. 3.1 leeg-on-gap totality, and ch. 8's fail-fast
lives in the data plane — the Parameterset must cover the evaluation
window (an overlap is a load error, a coverage gap a session-start
error — the ch. 8.4 phasing — both raised before any read). **Landed (2026-08-16):** the node is `IR-SPEC.md` ch. 4.3a
(`TemporalRead`, WF-049), with the full-timeline case value variant in
`uchar-case-v0`, the ref/ implementation on this chapter's `read`, and
the Clingo translation as a segment-containment join; an empty date
index reads to leeg (ch. 1.2 propagation). The anchor witness is
`corpus/golden/anchor/jutterswet-aanmeldquotum`. The RegelSpraak
surface follows spec-first via a pin bump; a temporal *characteristic*
operand stays OPEN with the case format.

### 3.4 Cut-merge arithmetic — the period-coalescing rule (§5.1.4)

Binary arithmetic over two timelines `a ⊕ b` is defined per period over
the **cut set** (knip): the sorted union of both operands' segment
boundaries.

```
cuts(a, b)  =  sort ( { van, tot of every segment of a and of b } )
(a ⊕ b) on [c_i, c_i+1)  =  coalesce(read(a, c_i)) ⊕ coalesce(read(b, c_i))
                            where coalesce(leeg) = 0
```

- The result exists only between the first and last cut — outside the
  union's span it is a gap. *Inside* the span, a period where both
  operands are leeg yields **0**, not leeg: that is the
  **period-coalescing rule**, ch. 1.2's §5.1.4 row, and the exact
  counterpart of the ch. 3.5 rule.
- Adjacent result segments with equal values are coalesced into one.
- Units follow ch. 2.6: the computation runs in the left operand's unit
  (each right operand restated by its exact factor), and the result is
  restated **once** to the target's unit.
- Granularity: every operand's declared granularity must be
  coarser-or-equal to the target's; the result's segment boundaries are
  the cut set itself — event-driven, not re-bucketed onto the target's
  calendar grid.

Worked example — `a` = 100 on [2024-01-01, 2024-07-01), `b` = 30 on
[2024-04-01, 2024-10-01), `a + b`:

| period | a | b | result |
|---|---|---|---|
| [2024-01-01, 2024-04-01) | 100 | leeg→0 | 100 |
| [2024-04-01, 2024-07-01) | 100 | 30 | 130 |
| [2024-07-01, 2024-10-01) | leeg→0 | 30 | 30 |

Worked example (interior gap coalesces to 0) — `a` = 1 on [jan, feb),
`b` = 2 on [mrt, apr), `a + b`: [jan, feb) = 1, **[feb, mrt) = 0**
(both leeg, coalesced to 0 + 0), [mrt, apr) = 2. Before jan and from apr
on: gap (leeg).

*v0 mirror note (scope):* only **plus and min** exist as timeline
arithmetic in v0. Verminderd-met, maal, division, and percentage-van over
two timelines are refused (marker/skip) — so verminderd-met's
left-propagation rule (ch. 1.2) has **no per-period counterpart** in v0;
the period-coalescing rule knows only the plus/min coalescing behaviour.
A scalar operand mixed into timeline arithmetic (broadcast) is also
refused. Both are OPEN in ch. 3.12; the natural liftings (per-period
application of the ch. 1.2 scalar rules; broadcast as an
everywhere-constant timeline) are candidates, not specified behaviour —
broadcast in particular needs a time domain for the constant (an
unbounded segment), the same obstacle as the unconditional temporal
characteristic (ch. 3.11).

### 3.4a The die/dat subselection on instance folds

**The contributors, before any filter: reach ∩ population.** An instance
fold names two declarations, and they answer two different questions. The
source relation (`sourceRole`, or the multi-step `sourcePath` of ch. 3.4b —
since L2 every instance fold carries one of the two, B12; until L2 WF-048
derived the role when neither was written) says which instances the
fold REACHES from its subject. `elementType` says which instances the
fold RANGES OVER, extensionally (`IR-SPEC.md` ch. 2.2): every instance
whose type is ⊑ it. The contributors are the intersection — an instance the
relation reaches whose type the element type does not cover is **not** a
contributor, and contributes to neither a count nor a value fold.

The two used to be the same set. WF-047 demanded the bound role's filler
and `elementType` be the same name, so the relation could not reach
anything the element type failed to cover; since ch. 2.6 made a role's
declared filler an UPPER bound and widened "agree" to ⊑-compatibility, the
filler may be a strict supertype and the two come apart. Where the filler
is the element type or below — every document written before the chain
existed — the intersection is the reach and nothing changes.

This narrowing is **not** the die/dat filter below, and the difference is
observable: the type narrowing happens first and cannot be written as a
condition, since no condition can ask an instance for its type.

Worked example (the `golden/jutterswet-munttelling` golden, verbatim in
gates 5, 6 and 12). `vondstaanmelding` relates Jutter to **Vondst**; both
folds declare `elementType` **Munt**, and `Munt ⊑ Vondst` strictly.
`J1` is linked to `V1` (a plain Vondst, gewicht 3,00), `M1` (a Munt, 7,00)
and `M2` (a Munt, 5,00): the reach is all three, the population is {M1,
M2}, so the count is **2** and the sum **12,00** — unfiltered they would
read 3 and 15,00. `J2` is linked to exactly one instance, `V2` (a plain
Vondst, 4,00): the reach is NOT empty and the contributors are, so the
count is **0** and the sum falls to its `of 0` fallback — the empty-source
rules of ch. 1.2 read the contributors, never the links. `J3` is linked to
`M3`, a Munt with no gewicht: it IS a contributor, so the count is **1**,
while the sum has a member and no value and reaches **0** by the same
fallback — which is why both folds are pinned per jutter, since only the
count tells J2's empty population from J3's empty value.

The `filter` condition of an instance fold evaluates **per
contributor**: the element instance is the condition's subject, never
the outer fold subject. A contributor whose filter operand is leeg
fails the condition (the ch. 1.2 comparison discipline) and drops out;
a filter over a derived characteristic reads it on the element type,
so the fold sits above that characteristic's writer in the strata
(ch. 6.1). A filter that excludes every contributor leaves an empty
source: the sum's declared fallback fires, a count reads 0 — exactly
the empty-role outcome. The same wire field also carries the §7.1
gedurende timeline clip (on the temporal total, whose element type is
the subject itself); that form is a timeline operation and both
executors refuse it until the clip lands — recorded, unwitnessed by a
red case until that card.

Worked example (the `jutterswet-strandgoed` emitter golden, verbatim
in gates 5–6): vondsten of 3, 7, 12 and one with an empty gewicht;
"die groter zijn dan 5" sums 7+12=19 (the empty one drops out); "die
waardevol zijn" — waardevol derived as gewicht > 10 — counts 1; a
jutter whose only vondst fails the filter reads the fallback 0 and
count 0.

**The count source: the range selects, the written name does not.** Under
`count` the fold's one argument is the COUNT SOURCE. Since L2 a count over
one source argument carries its range (`sourceRole`, one step — a
`sourcePath` belongs to a value fold's set source alone, `IR-SPEC.md`
WF-102 (c)), and the count folds the instances that range reaches from the
subject, narrowed by `elementType` as above. The argument's
`attribute` is the WRITTEN name of the bound role and selects nothing: it
MUST denote the bound role by its declared name or its declared plural
(`IR-SPEC.md` WF-047), and where the written form did not, the migration to
the L2 wire and the emitter write the declared role name in its place (the
L2 card, `backlog/done/20260923-l2-referentielaag.md`). A written name that
matches roles on several fact types the subject reaches therefore decides
nothing: `corpus/golden/ambiguous-fold-role-name` writes `vriend`, declared
as a role on two fact types the Jutter reaches, and its count reads the one
relation its `sourceRole` names (gates 5, 6 and 12). Until L2 the range of
a count without a binding was derived from the written name, a name
matching several reachable roles faulted in both executors, and that
document was a red case.

**The count source is recognised at the source itself.** Its `attribute`
names a role of the fold subject and no member of the element type
(`IR-SPEC.md` WF-093, in both directions: such a source is the source of
`count` and of nothing else, and `count` reads nothing but such a
source). Since L2 no field on the node records which reading applies, so a
collision of names is decided by the declarations: a count whose written
name is also a member of the element type (an attribute or a
characteristic) is no count source — it reads as that member, and a count
over a member is refused (WF-093 (b); `ref` and the ASP backend fault
with the verifier's sentence, `the source of this count aggregation reads
'…', a member of the element type '…': a count folds INSTANCES, …`). A role
whose declared name and declared plural both name members of the element
type, or whose name does while it declares no plural, therefore cannot be
counted; where only one of the two spellings collides, the count is written
with the other. Until L2 the recognition
keyed on the unknown-type sentinel on the source's own `type` (B13, B16).

**The self-denoting written form is a plain count source.** The written
form "het aantal `<rol>` van de `<onderwerp>`" may name the fold subject by
its own role name ("het aantal vondsten van de aanmelder", evaluated on the
Jutter who *is* the aanmelder). The wire carries nothing of that written
role: the count carries its range and reads the subject's own linked
instances of the element type, anchored on the subject like every count
source (`IR-SPEC.md` WF-083 (c)); no hop happens. Until L2 the written role
stood in the source argument's `navigatieRol`, as presentation and not
navigation, and the executors tested that it denoted the subject.

Worked example (the `jutterswet-aanmeldertelling` emitter golden, verbatim
in gates 5–6 and as WV-3.4a-telbron in `ref/` tests; on the wire the count
binds `vondstaanmelding`/`vondst` and its argument names `vondsten`, the
role's declared plural): a Jutter linked to two Vondsten — one with
gewicht 2,50 kg, one whose gewicht is leeg — counts **2**: a count folds
instances, not values, so a leeg element attribute still counts (the
aggregation row of ch. 1.2: a count, never empty). A Jutter with zero
linked Vondsten counts **0**, never leeg. A count without its range does
not load, and `ref` faults on one handed to it directly
(WV-3.4a-telbron-zonder-bereik).

*History until L2 — the type-pair fallback for the fold-source role name
(WF-034).* A written fold-source name that resolved to no declared role
(ci, declared name or declared plural) folded over the **unique connecting
role** derived from the type pair: exactly one fact type connecting the
fold subject type to the element type, and not a self-typed two-role fact
type, whose two roles were both candidates and so blocked the derivation
(the ch. 4.1 type-pair block); zero or multiple connecting roles, or a
name matching several reachable roles, kept the loud fault, and uniqueness
was counted in candidate roles, not fact types. Since L2 (B12) a count
carries its range, so nothing is derived and nothing falls back. The
migration derived the missing ranges once, with this derivation, and
refused the documents where it found zero or several candidates
(`wf034-ambigu-typenpaar`, removed at L2 on that ground,
`corpus/negatief/README.md`).

Worked example (the `jutterswet-kaveltelling` golden, verbatim in gates
5–6 and as WV-3.4a-typenpaar in `ref/` tests), in its L2 form. The role
`duinkavel` (kaveltoewijzing) declares no plural, so the written source
"het aantal duinkavels van de Jutter" denotes no role; until L2 this was
the type-pair fallback's example. Since L2 the count binds
`kaveltoewijzing`/`duinkavel` and its argument names `duinkavel`, the
declared role name the migration wrote in place of the written plural (see
above): a Jutter linked to three Duinkavels counts **3**. The written
singular "het aantal vat van de Jutter" names the role `vat` (vatberging,
declared plural `vaten`) by its declared name and binds `vatberging`/`vat`:
that Jutter's two Vaten count **2**. A Jutter with no links counts **0**
and **0**. In the ASP backend both counts key the bound role's canonical
name (`duinkavel`, `vaten`), never the written lexeme, which as a raw
`rol/3` key silently counted 0. With a second fact type connecting Jutter
to Duinkavel the count still reads the relation its binding names and
counts **3** (WV-3.4a-typenpaar-tweede-feittype-bindt); until L2 the
bindingless written form faulted loudly there.

### 3.4b The multi-step fold source — the flat union (WF-102)

A set fold source may describe its range with a PATH of relations rather
than one (`IR-SPEC.md` ch. 4.3, `sourcePath`; two or more steps, the
one-step range stays `sourceRole`). The folded set is the **flat union**:
start at the fold subject, take step 1's linked instances, take step 2's
linked instances from each of those, and so on; the instances the last step
reaches are the fold's REACH, which `elementType` narrows to the
contributors exactly as in the one-step form (ch. 3.4a — the intermediate
steps are not narrowed, only the landing). It is a SET — an element reachable through
more than one intermediate instance contributes **once** — and everything
downstream is unchanged: the element's attribute is read at the
`elementType` exactly as in the one-step form, a leeg element
attribute drops out of a value fold (ch. 1.2), the die/dat subselection
(ch. 3.4a) still evaluates per contributor with the element as its subject,
and the empty-source rules (ch. 1.2) apply to the union, not to any
intermediate step.

Three consequences of "the union, not any intermediate step", each worth
stating because the nested reading answers differently:

1. An intermediate instance that reaches nothing simply contributes
   nothing. It never produces an empty inner result for the outer fold to
   absorb, so `minimum`/`maximum`/`first`/`last` — which have no
   fallback — never have to decide what to do with one.
2. A count over the union counts ELEMENTS, never intermediate instances.
   (Named here and pinned nowhere, and since 2026-09-19 for a reason
   stronger than "no carrier": under `count` a multi-step range is not
   EXPRESSIBLE. Since L2 one clause says so directly: a `sourcePath` is
   admissible only as the range of a VALUE fold's set source, and a count
   source's range is one step (`IR-SPEC.md` WF-102 (c)); every executor
   refuses a count that carries a `sourcePath`, by name. Until L2 two
   fences met at this NODE — a `sourcePath` only on the SET source
   (WF-102 (c)), and no set source under a count (WF-096, maintainer
   decision 2026-09-19) — so neither spelling of a counting FOLD over a
   union survived the wire. The claim is the fold's and not the IR's: another node that
   counts over a range answers to its own rule, and nothing here is evidence
   about it. What prompted the
   decision was this clause read the other way round: measured 2026-09-19,
   `ref` counted the FILLED VALUES of a set source where the Drools backend
   counted the MEMBERS — 2 against 3 on one document as soon as a member's
   attribute was leeg — while the ASP backend refused the kind. Since ch. 1.2e
   and ch. 3.4a already say a count folds the CONTRIBUTORS, one of those two
   readings was not a variant but an error, and the form had no producer
   route at all; the maintainer took option (ii) of
   `backlog/done/20260909-eerste-laatste-setvouw-kloof.md` §6 and refused the
   wire form rather than amend a clause to fit an executor. So this clause's
   own word, ELEMENTS, stands unchanged, and since 2026-09-20 it HAS its
   carrier: the separately defined operation arrived as the filled count
   (ch. 3.4c), which reads a value per contributor and therefore takes the
   set source a path needs. Counted over a union it counts ELEMENTS — an
   element reachable along two intermediate instances contributes once —
   which is this clause's claim, pinned in ch. 3.4c's last worked example
   rather than asserted here. What stays unexpressible is a counting FOLD
   over a union under `count`, and that was never this clause's subject.
   Its two siblings left the asymmetry
   the other way on the same day: `first`/`last` over a set source are
   carried by both OCaml executors, witness `jutterswet-vondstvenster`.)
3. A sum's `emptyFallback` fires at most ONCE, for a subject whose whole
   range is empty — never once per empty intermediate instance. (With a
   zero fallback the two readings agree numerically; with any other they do
   not, and the executors carry only the zero fallback today, so the
   divergence is named here and pinned nowhere.)

A step through a to-ONE role is an ordinary step: it contributes its
zero-or-one linked instance to the union. Multiplicity is a declaration
fact and does not enter this reading.

Worked example (the `anchor/jutterswet-bootberging` golden, verbatim in
gates 5–6 and as WV-3.4b-vlakke-unie / WV-3.4b-gedeeld in `ref/` tests).
The model: a Jutter sails `gevoerde boten`, a Boot salvages `geborgen
vondsten`, a Vondst fills `ingepakte kisten`. Three rules on one Jutter —
the minimum `gewicht` over the two-step range, the sum of the same with an
`of 0` fallback, and the minimum `inhoudsgewicht` over the THREE-step range.

*Case 1 — the range spread over two intermediate instances.* `J1` sails
`B1` and `B2`; `B1` salvages `V1` (3,00) and `V2` (7,00); `B2` salvages
`V3` (5,00). The union is {V1, V2, V3}, so the minimum is **3,00** — it
sits on `B1` while `B2` contributes 5,00, which is what makes the answer a
proof that both intermediate instances are walked — and the sum is
**15,00**. `V1` holds `K1` (2,50), `V2` holds `K2` (1,25) and `K3` (4,00),
`V3` holds none: the three-step union is {K1, K2, K3} and the minimum is
**1,25**. `J2` sails one boat salvaging `V4` (9,00) and `V5` (no gewicht):
the leeg element drops out of both value folds, so the minimum is **9,00**
and the sum **9,00**, while the three-step range reaches only `K4` (6,00)
→ **6,00**. `J3` sails a boat that salvages nothing and `J4` sails no boat
at all: both ranges are empty, so both minima are **leeg** (no fallback
exists) and both sums are **0** (the fallback, fired once) — the empty
range at the LAST step and at the FIRST step answer alike.

*Case 2 — the shared element.* `J1` sails `B1` and `B2`; `B1` salvages
`V1` (3,00) and `V2` (7,00); `B2` salvages `V1` again and `V3` (5,00).
`V1` is reachable along two intermediate instances and contributes once,
so the sum is **15,00** and not 18,00; the minimum is **3,00** either way,
which is why the sum is the witness. The same holds one step deeper: `V1`
and `V3` both hold `K1` (2,50) while `V2` holds `K2` (1,25), so the
three-step union is {K1, K2} and the minimum is **1,25**.

### 3.4c The filled-value count

*(Maintainer decisions G1–G8, 2026-09-19; landed 2026-09-20. Wire form and
well-formedness: `IR-SPEC.md` ch. 4.3, "The filled count".)*

One aggregation kind folds the contributors' VALUES by PRESENCE:
`filledCount` counts the contributors whose value is not leeg. It is an
instance fold and it reads a value per contributor, so it takes a SET source
(`IR-SPEC.md` ch. 4.3: since L2 the single argument of a fold that carries a
range, `sourceRole` or `sourcePath`; until L2 the argument marked
`set: true`), and that is required rather than optional. It has no listed form: the source language has no spelling for the
operation at all, so there is no articulated operand list to admit.

The contributor set is ch. 3.4a's, unchanged: reach ∩ population, narrowed
further by the `filter` per contributor. FILLED is the ordinary ch. 1.2
emptiness test — the one the source language's `is gevuld` predicate applies
— so a contributor whose value is leeg is not filled and every other value
is, **whatever its type**. The test asks for presence, not for a number: a
text, a date, an enumeration value and a boolean all count as filled, and
so does a boolean whose value is FALSE. False is a value; leeg is the
absence of one. An instance that is not a contributor never reaches the
test at all.

`filledCount` over a source with no filled values is **0**, never leeg —
a count is a cardinality and the empty collection has one, exactly as
ch. 1.2e's count row says, and ch. 1.3 keeps that 0 apart from coalescing:
it is the identity element of a rule over SETS, not an empty value dressed
as a number. That ground belongs to every count and not to the kind
`count`. It carries no `emptyFallback`: there is no empty to fall back
from, and WF-037's "sum only" clause already excludes it.

The result is a whole, unitless number whatever unit the folded attribute
carries, so a filled count has no rounding question and no unit derivation.
It is the result type the instance count has, reached by a different fold.
That type is DERIVED and not declared, so a wire annotation saying anything
else is ill-formed rather than believed (`IR-SPEC.md` WF-091; negatief
`wf091-aantalgevulde-eenheid`).

**One exclusion, and it is normative rather than a note: the folded
attribute is NOT time-dependent.** "Whatever its type" is about the value
domain — number, text, date, enumeration, boolean — and not about the
temporal axis. A set read of a TEMPORAL attribute folds twice (ch. 3.6,
§7.1): each member's own timeline collapses over time first, and only those
scalars fold over the set. That first collapse is defined for the som and
the totaal and for nothing else, because only they integrate; a count has no
defined MOMENT at which to ask a timeline whether it is filled, and picking
one would be this chapter inventing a reading rather than stating one. A
filled count over a time-dependent attribute therefore fails observably,
naming the attribute AND the fold, and never reads a value the timeline does
not have at an instant (`ref` and the ASP backend, word for word: *the
temporal attribute … of a set has no moment at which a filled count can ask
a timeline whether it is filled*; worked example WV-3.4c-temporeel-faalt,
red case `aantalgevulde-temporele-setbron`). Naming the FOLD is part of the
clause and not presentation: the ranking folds are refused at the same
position on the same ground, so a refusal that named only the position would
be one sentence for five constructs and would identify none of them.
The door is closed and not locked: a later card with a real casus may give
the filled count its own moment rule, and it would be a clause here rather
than an executor's choice.

**The instance count and the filled count answer different questions, and it
takes both to tell the two kinds of empty apart.** Over a source whose
contributors are all leeg the filled count is 0 while the instance count is
not; over a source with no contributors at all both are 0. ch. 3.4a's
observation — that only a count separates an empty population from an empty
value — is now sharper: the INSTANCE count separates them, the FILLED count
does not, and a document that needs the distinction writes both.

**What this does NOT give.** There is no average. `sum ÷ filledCount` is
the hand-spelled form and it is the author's to write; over a wholly empty
source it divides by zero and faults, and that fault is deliberate
(maintainer decision G1). What the kind does give the author is the GUARD
that makes it avoidable: a filled count is an ordinary expression, so
"indien het aantal gevulde … groter is dan 0" is writable in a rule's
condition, where `count` could not serve — an instance count is 1 for a
member without a value and would let the division through.

**Worked examples (absolute; `ref/` tests verbatim, WV-3.4c-*).** The
Jutterswet model: a Jutter aanmeldt Vondsten; a Vondst declares `gewicht`
(kg, two decimals), `vindplaats` (tekst), `vondstdag` (datum), `kustvak`
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

*The typelessness of the test.* The same three contributors — two carrying
values, one carrying none — folded once per declared type:

| folded attribute | the three values | aantal gevulde |
|---|---|---|
| `gewicht` (numeriek, kg) | 2,50; 1,25; leeg | **2** |
| `vindplaats` (tekst) | "Noorderstrand"; "Zuiderhoofd"; leeg | **2** |
| `vondstdag` (datum) | 2026-05-01; 2026-06-21; leeg | **2** |
| `kustvak` (enumeratie) | noord; zuid; leeg | **2** |
| `geborgen` (booleaan) | waar; onwaar; leeg | **2** |

The last row carries the sharpest half of it: `onwaar` is filled. The fold
neither compares nor adds, so the operand family the ordering folds demand
(`IR-SPEC.md` ch. 2.4) has no bearing here.

*The filter narrows the contributors first.* Over the jutter whose vondsten
weigh 2,50, 1,25 and leeg:

| filter | aantal | aantal gevulde |
|---|---|---|
| none | 3 | **2** |
| die zwaarder zijn dan 2,00 kg | 1 | **1** |
| die zwaarder zijn dan 10,00 kg | 0 | **0** |

The leeg member fails the comparison (ch. 1.2d) and drops out before the
presence test, which is why the middle row's two counts agree: one
contributor, and it has a value.

*The multi-step range counts ELEMENTS (ch. 3.4b).* A Jutter sails boten, a
Boot salvages vondsten. `J1` sails `B1` and `B2`; `B1` salvages `V1` (3,00)
and `V2` (no gewicht); `B2` salvages `V3` (5,00). The union is {V1, V2, V3}
and the filled count is **2**. With `B2` salvaging `V1` as well, the union
is unchanged and the filled count is still **2**: an element reachable along
two intermediate instances contributes once. This is the example ch. 3.4b's
word ELEMENTS waited for; under `count` the shape has no wire form at all
(`IR-SPEC.md` WF-102 (c): a count source's range is one step; until L2
WF-096 and WF-102 met at that node), and the filled count is the operation
that can carry it.

### 3.4d The instance-set union — the §5.7 concatenation

*(Maintainer decision 2026-09-20. Wire form and well-formedness:
`IR-SPEC.md` ch. 4.3, "The instance-set union", WF-110.)*

A count may range over a CONCATENATION of subject expressions — "het aantal
aangemelde vondsten van de Jutter en geborgen vondsten van de Jutter" — and
the source language defines that as joining the instance sets the selections
yield into ONE NEW SET (§5.7); its typing document
adds that the count operator admits such a concatenation, unbounded in
number, and takes subject expressions only (§4.11). On the wire the
selections are an `InstanceSetUnion` under the count, one
`InstanceSetBranch` each.

**The members.** Each selection has its own contributors, computed exactly
as ch. 3.4a computes a fold's: the reach — the instances its `sourceRole`
links the count's SUBJECT to — intersected with the population its
`elementType` names extensionally, narrowed further by its own `filter`,
which evaluates PER CONTRIBUTOR with the element as the condition's subject.
The union's members are the union of those sets.

**Deduplication is on INSTANCE IDENTITY, and it is the whole operation.** An
instance that satisfies two selections is one instance and contributes ONCE,
so the count is |A ∪ B| and never |A| + |B|. This is ch. 3.4b's rule for the
flat union of a multi-step range, stated across selections instead of across
intermediate steps, and for the same reason: a set has no multiplicities.
The three consequences carry over unchanged — a selection that reaches
nothing simply contributes nothing (it never produces an empty inner result
for the count to absorb), the count counts ELEMENTS, and the empty-source
rules of ch. 1.2 apply to the UNION and never to a selection.

**Empty.** A union none of whose selections contributes anything counts
**0**, never leeg — the ch. 1.2e count row, which belongs to every count and
not to any particular source shape. A count carries no `emptyFallback` and
has no empty to fall back from.

**Two selections describing the same thing.** A union whose selections name
the same relation and the same filter is well-formed and counts that
selection once: the dedup rule doing its job, not a defect. The verifier
does not refuse it, because refusing it would mean comparing conditions
structurally to forbid a document that already answers correctly.

**Where a union count stands.** WF-110 (a) restricts the UNION node to one
position — the count's single argument. The COUNT over it is an ordinary whole,
unitless number and stands wherever any other count stands: as a rule value, in
a `condition`, on either side of a comparison. Worked example below
(`jutterswet-unievoorwaarde`).

**One population.** Every selection of one union names the same element type
(WF-110 (d)), so "instance identity" needs no reading across a subtype
lattice here: the members are instances of one declared population. That is
a wire restriction and not a semantic simplification — see `IR-SPEC.md` for
why the multi-population union is a separate question.

**Worked examples (absolute; `ref/` tests verbatim, WV-3.4d-\*; golden
`jutterswet-vondstunie` and `jutterswet-keurunie`, replayed by gates 5, 6
and 12).** The Jutterswet model: a Jutter `meldt` aangemelde vondsten
(`vondstaanmelding`) and `bergt` geborgen vondsten (`vondstberging`); a
Vondst carries the characteristics `gemerkt` and `gekeurd`.

*Two selections over DIFFERENT relations — the dedup witness.* `J1` meldt
`V1`, `V2` and `V3` aan, and bergt `V3` and `V4`. The selections are
{V1, V2, V3} and {V3, V4}; `V3` sits in both and counts once, so the union
is {V1, V2, V3, V4} and the count is **4** — the sum of the parts is 5, so
the answer discriminates. `J2` meldt `V5` aan and bergt `V5` as well: both
selections are {V5}, the union is {V5} and the count is **1**, not 2.
`J3` meldt nothing and bergt `V6`: one empty selection, so the count is
**1** — an empty selection contributes nothing rather than emptying the
union. `J4` meldt nothing and bergt nothing: **0**, never leeg.

*Two selections over ONE relation with overlapping filters.* Over `J1`'s
aangemelde vondsten, `V1` is gemerkt, `V2` is gekeurd and `V3` is BOTH: the
selections are {V1, V3} and {V2, V3}, the union is {V1, V2, V3} and the
count is **3** where the sum of the parts is 4. `J2` meldt one vondst that
is neither: both selections are empty and the count is **0**.

*The union count in a `condition`.* "Een Jutter is ruim voorzien indien het
aantal aangemelde vondsten van de Jutter en geborgen vondsten van de Jutter
groter is dan 2." `J1` meldt `V1`, `V2`, `V3` aan en bergt `V3` en `V4`: the
union is 4, so the characteristic HOLDS. `J2` meldt `V5` en `V6` aan en bergt
dezelfde twee: the union is **2** and the characteristic does NOT hold — where
the sum of the parts is 4 and would have made it hold, which is what makes this
row a dedup witness and not only a position witness. `J3` reaches one element
(1) and `J4` none (0); neither holds.

*A filtered selection beside an UNFILTERED one.* Over `J1`'s three
aangemelde vondsten, "die gemerkt zijn" selects {V1, V3} and the bare
selection selects all three: the union is the bare selection's set and the
count is **3**. The form is carried rather than refused, deliberately — see
`IR-SPEC.md`, where the v0 toolchain's marker for it is shown to be a
property of its lowering mechanism and not of the operation.

### 3.5 Temporal summation — the contributor-skipping rule (§5.8.2)

The temporal sum over N timeline contributors (the operands of a temporal
som, or the element timelines of a to-many navigation) folds per period
over the cut set of **all** contributors:

```
som(t_1 … t_N) on [c_i, c_i+1)
  = Σ { read(t_k, c_i) | read(t_k, c_i) ≠ leeg }   if ≥ 1 contributor non-leeg
  = leeg                                            if all N are leeg — unless
    the declared fallback clause ("… of 0 als die er niet zijn")
    substitutes 0
```

This is the **contributor-skipping rule**: a leeg contributor is
skipped, never counted as 0 — the timeline instance of the ch. 1.2
aggregation discipline, and the named opposite of ch. 3.4's
period-coalescing rule. Further clauses:

- **Zero contributors** (an empty to-many set) yield the **empty
  timeline** — even under the fallback clause: with no contributor there
  is no cut set to bound any period, so there is nothing to substitute 0
  on. (Empty *set* ≠ all-empty *values*; the fallback speaks only about
  the latter.)
- All-leeg periods that the fallback does not fill are emitted as
  explicit leeg segments (the one producer that does, ch. 3.1); adjacent
  equal segments — equal-leeg included — coalesce.
- Each contributor is restated to the target's unit **before** the fold
  (exact factors, ch. 2.6).
- The operand is a Number — WF-091 (`IR-SPEC.md` ch. 4.1): a contributor
  declared as any other type makes the document ill-formed.

Worked examples (absolute; the staggered-contributor set): contributor
p1 = {jan: 50, feb: 60}, p2 = {feb: 10, mrt: 10}, per month:

| month | p1 | p2 | som |
|---|---|---|---|
| jan | 50 | leeg (skipped) | 50 |
| feb | 60 | 10 | 70 |
| mrt | leeg (skipped) | 10 | 10 |

With p1 = {jan: 40}, p2 = {mrt: 20}: jan = 40, **feb = leeg**, mrt = 20;
the same with the fallback clause: jan = 40, **feb = 0**, mrt = 20. With
no contributors at all: the empty timeline, fallback or not. Cross-unit:
contributors in €/mnd summed into a €/jr target, jan p1 = 50, p2 = 10 →
each contributor restated before the fold: 50×12 + 10×12 = **720**.

*v0 mirror note (scope):* only **som** and the scalar-collapsing totaal
(ch. 3.6) exist over timelines. Minimum, maximum, eerste, and laatste
over a timeline are refused in v0 — the recorded refusal reason is that
"the minimum of a timeline" is ambiguous between a per-period fold over
contributors and an over-time fold over one timeline's periods. **OPEN**
(ch. 3.12): v1 must either define both readings apart or keep the
refusal; it may not inherit the ambiguity.

### 3.6 Collapse to a scalar — the temporal total

**The gedurende clip (landed 2026-08-19, WF-055).** A filter on the
temporal total clips the value timeline BEFORE the total: intersect
each value segment with the condition's true-periods (half-open
interval intersection; the condition's own values are ignored), then
total whole units of the value's granularity per RAW clipped
fragment — floor, never pro-rata, never coalesced: a per-year value
clipped to a partial year contributes 0, and two adjacent condition
periods that split one whole unit lose BOTH halves (the pinned
runtime's per-fragment floor, witnessed by the split-floor case). The condition's true-periods come from an own temporal
characteristic (its case-supplied periods; a gap means the
characteristic does not hold and clips away; negation takes the
complement within the value segments) or from a per-segment comparison
of an own temporal attribute against a scalar bound
(piecewise-constant, never per day). Both readings are about **the
subject** and about the **whole span** of the timeline being clipped,
and WF-097 (`IR-SPEC.md` ch. 4.5) holds the wire to it: a clip member
carrying an owner or a whole-period qualifier — the negated qualifier
included — is ill-formed, and both executors fault on it by name rather
than answer the unqualified question the fields were silently reduced
to before 2026-09-09. The rule covers all THREE clips — this chapter's
temporal total, ch. 3.7's pro-rata fold and ch. 3.8's day count — and
each executor implements the fence once, shared between them. The two
fault with the same words but not at the same moment: this interpreter
is lazy and the ASP translation eager, so a violation behind a false
rule condition is refused there and never reached here. That split is
inherited from WF-055, and the verifier bounds it by rejecting the
document either way. A fully clipped-away — or
absent — value timeline totals 0, never leeg: the clip's presence
turns absence into 0 (the mirrored v0 nuance; the unfiltered total of
an absent timeline stays leeg). The operand is a Number — WF-091
(`IR-SPEC.md` ch. 4.1) — so this 0 always lands in a numeric slot. Worked
example (the `jutterswet-getijdenwacht` emitter golden, verbatim in gates
5–6):
10/month through 2023 and 11/month January–June 2024, clipped to
[2023-07-01, 2024-02-01) → 6×10 + 1×11 = 71; the comparison clip
"> 10" keeps the 11-segment → 6×11 = 66; a carrier without periods, and
one without a timeline, both total 0.

**The composite clip (landed 2026-08-21).** A **flat** composite `all`
condition under gedurende clips to the **intersection** of its
members' true-periods: the pinned runtime nests the per-member clips
over exactly such flat characteristic lists, and interval
intersection is associative, so member order never matters. Members
are own-temporal-characteristic checks. The v0 boundary, stated
precisely: v0 partitions a **static** (non-temporal) comparison
member out as a value-side *guard* before its clip ever runs (an AND
gate under `all`), and markers a temporal-attribute comparison member
and a **nested** composite bullet as Onopgelost — v1 narrows
composite members to the characteristic checks both executors carry;
the static-guard form and the nested bullet are recorded loud flanks
(WF-055 admits neither), not silent rewrites. A member
without any true period empties the intersection: the total is 0 by
the clip-presence rule above. Worked example (the
`jutterswet-dubbelwacht` anchor, absolute, gates 5–6): 10/month
through 2024; borgplichtig January–July and aangesteld April–January
intersect to April–July → 3×10 = 30; disjoint members → 0; one member
without periods → 0; split periods (January–March + June–September
against February–August) → (February–March) + (June–August) = 3
months → 30. *Executed-note:* a **negated member** clips through its
complement, exactly like the single negated clip. Both executors carry
the shape since 2026-09-09; `corpus/golden/jutterswet-tegenwacht` pins
both forms differentially (gates 5–6), including the ch. 3.6 negated
worked example below. The **ten-minste-1 union** (v0 clips to the union
of member regions) stays a loud, recorded flank in both executors, and
WF-055 does not admit it until an executor carries it with an anchor.

"Het totaal van" a timeline over time collapses it to one scalar:

```
totaal(tl) = Σ over segments s of tl:  value(s) × whole-units(van_s, tot_s)
```

where `whole-units` counts whole declared-granularity units between the
segment's bounds (ch. 3.2 job 1: all five granularities). Clauses:

- A leeg-valued segment contributes 0 (a dedicated rule of this collapse,
  in the family of ch. 1.2's totaal row — the collapse is a total, not a
  som). A segment shorter than one whole unit contributes 0 by the
  whole-units count.
- An existing but **segmentless** timeline totals to **0** (the identity
  of a sum over nothing); an **absent** timeline (the member itself leeg)
  totals to **leeg**. *v0 mirror note:* this gap-vs-absent distinction is
  the ch. 3.1 residual, mirrored as found; whether v1 keeps it stays open
  with the ch. 3.12 timeline decisions — the ch. 1.4 freeze covers the
  scalar discipline, not this temporal residual.
- **Period selection ("gedurende …")** is clipping, not re-bucketing: the
  value timeline is intersected with the selecting condition's
  true-periods (a boolean timeline: a characteristic's true-periods, its
  complement under negation, or the true-periods of a comparison over a
  timeline), and the clipped timeline is then totalled.
- **Units, the plain operand.** The rate obligation of WF-038 is satisfied
  *structurally* here: the timeline's granularity is the "per time unit",
  so a plain `€`-valued per-month timeline totals to `€` by value ×
  month-count. An explicit rate unit (`€/mnd`) is what the *non-temporal*
  totaal operand requires; the temporal collapse must not be misread as
  requiring it.
- **Units, the explicit rate operand.** When the timeline's value *does*
  carry a rate unit, the collapse is an **integration**, and the
  whole-unit count is not a bare number but a **duration stated in the
  grid's own unit**: `value × duration` cancels the denominator and the
  total carries the **numerator alone** — `€/mnd` over a per-month
  timeline totals to `€`. This is the one collapse that changes the unit,
  and it is the runtime half of the §4.20 Tabel-30 result type (row 2,
  `Eenheid1`), so a `€`-declared target accepts the total without any
  restatement.
  The grid's unit is the one the document's declared unit systems spell
  for this granularity (ch. 3.8 spelling table), resolved **once per
  fold**. The integration is then **exact over every denominator the
  declarations connect to it**, through those declared factors and
  nothing else: `€/mnd` on a per-month grid cancels one-for-one; `€/jr`
  on a per-month grid cancels too where the document declares
  `jr = 12 mnd` — twelve months *are* one year, so `10 €/jr` over 2024 is
  **10 €**, not 120; and a month-sized unit that is not a calendar
  spelling at all (`€/bm` with `bm` canonically a month) cancels by its
  declared factor, the spelling table never entering. Three things are
  refused **loudly**, never guessed: a denominator the declarations
  connect to no grid-sized unit (the refusal NAMES both the value's unit
  and the grid's), a document that declares **no** unit for this grid,
  and a document that declares **several** grid units with different
  canonical chains. A value unit **without** a denominator keeps the
  plain rule above, and a unitless value keeps a unitless total — those
  two need no grid unit at all, so they total in a document that declares
  none.
  Within one collapse, per-segment products are summed by the ordinary
  ch. 2.6 addition: a segment in a **convertible** sister unit is
  restated exactly (120 € + 500 ct = 125 €), a **foreign** one is refused
  loudly, and only a **unitless** side adopts silently. The case format
  cannot reach a mixed-unit timeline — the loader attaches the
  declaration's one unit to every segment — so this rule governs
  computed timelines alone.
  *Executor note (2026-09-04):* the number the collapse produces is not
  always the bare whole-unit count. Where the denominator is the grid
  unit itself the two coincide, but a convertible foreign denominator
  scales by the declared factor. The v0 Drools backend weights
  unit-blind, so on a `€/jr` per-month timeline it answers 120 where this
  rule gives 10 — a **semantic** divergence, not a refusal. No corpus
  form carries it (every corpus temporal total is unitless), so gate 12
  cannot see it; it is backend-card material, recorded here rather than
  silently absent.
- **The per-element fold over a set (§7.1).** `het totaal van <temporal
  attribute> van alle <set>` folds **twice**: each set member's own
  timeline collapses over time first — by the rule above, at that
  attribute's own granularity — and only the resulting scalars fold over
  the set. The outer fold is the ordinary ch. 1.2e aggregation: for
  `het totaal van` a member without a timeline contributes nothing and an
  empty set totals **0**; `de som van` over the same shape folds
  identically inside and keeps the som's leeg rule outside. Every other
  aggregation over a temporal set member — min, max, eerste, laatste,
  aantal — is **refused**: there is no defined moment of the timeline for
  it to rank or count, and silently reading the segment list instead of a
  value is the error this rule exists to prevent.
  On the wire the two routes differ by their range. The per-element fold
  carries one (`sourceRole` or `sourcePath`, the relation of its set
  source). The subject's own collapse carries none: of the
  single-argument aggregations that carry an `elementType`,
  `total` alone may omit its range (since L2, `IR-SPEC.md` ch. 4.3), and
  it then reads the subject itself, so its `elementType` and its
  argument's `subjectType` both name the subject type (WF-083 (c)).
  The two temporal-total routes carry **opposite** input conventions, and
  the asymmetry is deliberate (ch. 8.4 keeps the two case conventions
  strictly apart):
  - the **subject's own** collapse takes the asOfDate-bucket scalar: the
    session date *is* the horizon, so one bucket of the subject's grid
    carries the total;
  - the **per-element** fold requires every member to bring its **own**
    horizon, because the integration runs once per member and there is no
    single bucket to borrow. The case format's full-timeline variant is
    the one self-describing form (it states each segment's bounds); a
    member carrying the horizonless bucket scalar is refused loudly,
    naming that member — the exact "needs the one, finds the other" case
    of ch. 8.4. An **absent** member still contributes nothing (leeg),
    and horizon-carrying members mix freely.

Worked examples (absolute, `ref/` tests verbatim; per-month timeline):

- segments 2023-01-01..2024-01-01 = 10,00 and 2024-01-01..2024-07-01 =
  11,00 → totaal = 12×10 + 6×11 = **186,00**.
- degenerate segments (zero-length or reversed) are skipped: only a
  6-month segment of 10 remains → **60,00**, never a negative count.
- gedurende "> 1000" over value timeline {3 mnd: 1200, 2 mnd: 800}:
  clipped to the 1200-months → 1200×3 = **3600,00** (not 5200).
- value timeline 10 on [2023-01-01, 2024-01-01) and 11 on
  [2024-01-01, 2024-07-01); characteristic true on
  [2023-07-01, 2024-02-01). gedurende the characteristic → 6×10 (jul–dec
  2023) + 1×11 (jan 2024) = **71,00**; the negated form (complement,
  within the value timeline's span) → 6×10 (jan–jun 2023) + 5×11
  (feb–jun 2024) = **115,00**.
- gedurende "≠ 1000" where the tested timeline has leeg periods: leeg
  **satisfies** ≠ against a filled bound (the ch. 1.2 inequality
  asymmetry, applied per period): value 100 on [2024-01-01, 2024-07-01);
  tested timeline 1000 on [2024-01-01, 2024-02-01), leeg elsewhere → ≠
  1000 holds feb–jun → 5×100 = **500,00**.
- explicit rate unit: `10 €/mnd` on [2024-01-01, 2025-01-01) → 12 months
  × 10 €/mnd = **120 €** — the denominator cancels, so a `€`-declared
  target takes it unchanged.
- the same timeline declared `€/jr`, in a document declaring
  `jr = 12 mnd` → the 12 months are one year → **10 €**. Declared
  `€/bm` with `bm` canonically a month (factor 1) → **120 €**, and over
  one month bucket → **10 €**.
- the same `10 €/mnd` value on a per-**quarter** grid across 2024 → 4
  quarters × 3 months × 10 = **120 €**: a coarser grid gives the same
  number, because the conversion is exact.
- `10 €/dg` on a per-month grid, in a document that declares no
  conversion between `dg` and `mnd` → **refused**, naming both units
  (`€/dg` against the `mnd` grid). Where the document *does* declare
  `mnd = 30 dg`, the same timeline integrates to 12 × 30 × 10 =
  **3600 €** — the declared factor governs, not the calendar.
- the §7.1 per-element fold, three set members with per-month `€/mnd`
  **timelines**: `10` over [2024-01-01, 2025-01-01) → 120 €, `5` over
  [2024-01-01, 2024-07-01) → 30 €, and a member without a timeline →
  contributes nothing. Total **150 €**. Replace any one member's timeline
  by the asOfDate-bucket scalar and the fold is **refused**, naming that
  member; all members absent → **0** (and `de som van` → leeg).

*v0 mirror note:* the totaal of a multi-period timeline is refused with
the grid (ch. 3.2), never mis-weighted as single-period.

*Coverage note (2026-09-04):* the §4.20 Tabel-30 row-2 typing and this
integration are meant to be the same statement — the frontend types the
result `Eenheid1`, this rule produces it — but that identity is **not**
covered end to end. No `uchar-case-v0` case can carry it today: the ASP
backend translates neither the §7.1 set fold nor a write whose target
unit differs from the value unit, so no golden of this shape can replay
in both executors (gate 6 has no refusal channel). The rule is pinned at
`ref/` unit level (`WV-3.6-g*`, `WV-3.6-h*`); the end-to-end claim stays
prose until a backend carries the form.

### 3.7 Pro-rata — the time-proportional part

Ch. 2.8 gave the equation; the full algebra, per bucket of the target's
grid (month or year, ch. 3.2 — anchored buckets for a variable-start
target, ch. 3.2):

```
deel on bucket B = read(base, start(B)) × trueDays(B) / days(B)
```

- `days(B)` = the bucket's actual calendar day count (leap-aware: 28, 29,
  365, 366 — never 360).
- `trueDays(B)` = the day count of the intersection of the selecting
  condition's true-periods with `B`, half-open, summed over all true
  intervals touching `B`.
- `read(base, start(B))` — the base must be constant across the bucket,
  guaranteed by requiring the base's granularity coarser-or-equal to the
  target's. A base that is **leeg at the bucket start emits no segment**
  for that bucket (a gap, not a 0); a bucket with `trueDays` = 0 emits
  nothing. The base is a Number — WF-091 (`IR-SPEC.md` ch. 4.1).
- A rate-unit base is restated **first**, by its exact factor
  (`€/jr` base for a per-month target: ÷ 12 exactly), then prorated.
  *Delta (tracked, `CONSTRUCTS.md` row on the date builtins):* the rate
  restatement and the anchored-bucket variant are FENCED in both
  executors until a corpus case lands — a unit-carrying base or target
  faults loudly, never prorates factor-free.
- The fraction is exact and is **never auto-rounded** to the target's
  declared decimals — rounding happens only at an explicit rounding or
  the assignment gate (ch. 2.4). *v0 mirror note (residual, same class as
  ch. 2.1):* v0 stores a non-terminating pro-rata at 16 significant
  digits (DECIMAL64); the exact fraction is normative, the store is the
  v0 approximation; backends must agree on the corpus after rounding.
  The reference interpreter stores the exact fraction; the ASP
  translation derives only outcomes exact at the target scale and
  fences a remainder with a loud witness — a non-terminating pro-rata
  therefore cannot enter the differential corpus un-rounded.

Worked examples (absolute): base per year €12 (2024) / €18 (2025),
condition true [2024-01-01, 2025-02-08), monthly target: twelve months of
€12, jan 2025 = €18, **feb 2025 = 18 × 7/28 = 4,50** (7 true days of 28;
2025 is no leap year), nothing from March on — and the twelve 2024
buckets stay twelve separate segments: bucketing producers do **not**
coalesce adjacent equal buckets (ch. 3.1 normalisation asymmetry,
mirrored). Yearly target, base €18, 38 true days in 2025 → 18 × 38/365 =
**684/365** exact (≈ 1,873972…, only an explicit rounding collapses it).
Rate base 120 €/jr into a monthly target → 10 €/mnd; 5 €/jr → **5/12
€/mnd** exact (v0: 0,4166666666666667 at its 16-digit store — the exact
division by 12, never a pre-rounded ×0,0833 reciprocal).

### 3.8 Day counting and day-kind durations

**aantal dagen in (de maand / het jaar).** Buckets a condition's
true-periods on the calendar grid and emits, per touched bucket, one
segment `[start(B), next(B))` whose value is the true-day count in `B`
(multiple true intervals in one bucket sum). Empty buckets emit nothing.
The operator's stated bucket ("de maand"/"het jaar") must equal the
target's granularity. Worked example (absolute): true on
[2024-01-01, 2024-01-02) and [2024-01-10, 2024-02-14) → **jan = 1 + 22 =
23**, **feb = 13**, two segments. **Executed (2026-08-21, both
executors; WF-058; anchor `jutterswet-dagentelling` carries this
example verbatim plus the year-boundary split):** the gedurende
condition is an own temporal characteristic (richer shapes refuse
loudly, as at ch. 3.7); the ch. 7.8 timeline arm applies — a
non-deciding count leaves a case-supplied input timeline standing
whole; the anchored (variable-startpunt) form stays unexecuted, loud.

**tijdsduur van A tot B** counts *whole* declared units between two
dates, signed (negative when B < A); any leeg operand → leeg, never 0
(ch. 1.2). A is **evaluated** before B, in the order the phrase names
them (ch. 5.11). The absolute variant drops the sign. "hele" in the surface
phrase only types the *result* as a whole number — the count is always
whole units, written "hele" or not. The calendar is the **proleptic
Gregorian calendar in zoneless civil time**: no time zones, no DST, no
leap seconds — a date is a civil (y, m, d) triple, bijective with its
epoch day number (days since 1970-01-01). The whole-unit definition is
the **packed-month rule** (v0 mirror, exact):

```
epoch(D)  = civil days since 1970-01-01
packed(D) = (12·year(D) + month(D) − 1)·32 + day(D)

dagen     = epoch(B) − epoch(A)
weken     = dagen quot 7
maanden   = (packed(B) − packed(A)) quot 32
kwartalen = maanden quot 3
jaren     = maanden quot 12
```

`quot` is integer division truncating **toward zero**, so a negative
span truncates toward zero, never toward −∞. Worked examples
(absolute, pinned):

- **Leap child:** 29-02-2004 tot 28-02-2026 = **21** whole years
  (packed difference 8447, quot 32 = 263 months, quot 12 = 21);
  29-02-2004 tot 01-03-2026 = **22** (8452 quot 32 = 264 months).
- **Clamp, not inverse:** 31-01-2024 + 1 maand = 29-02-2024 (the clamp
  below), yet tijdsduur van 31-01-2024 tot 29-02-2024 = **0** whole
  months (packed difference 30 < 32): whole-unit counting is *not* the
  inverse of date-plus-unit arithmetic.
- **Signed truncation:** 10-03-2025 tot 10-08-2024 = −7 whole months;
  in whole quarters −7 quot 3 = **−2** (toward zero); in whole years
  **0**; the absolute variant reads 7 months.

**The time-unit spelling table (normative).** A duration's unit is the
declared unit of its static type — for date ± duration the duration
operand's type, for tijdsduur the node's own result type — recognised
by SPELLING, case-insensitive, keyed on the unit's written
abbreviation. Declared conversion factors and the declaring system are
irrelevant to calendar arithmetic (a unit may name no system): a year is
a calendar year, never a second count. Nine units:

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

The first five are day-precision units; the last four are sub-day units
and demand millisecond-precision (datum-tijd) operands. A spelling
outside the table is a declared day kind (the day-kind forms above) or
ill-formed (WF-035/WF-038).

**Thirty spellings, and every one of them is a calendar unit.** The
table is read as a whole: nine units, thirty spellings, and no spelling
in it is available for anything else. Three of the thirty are worth
naming because they are easy to read past. `s` is a spelling of the
second, one character long. `secondes` and `millisecondes` are SECOND
PLURAL FORMS, standing beside `seconden` and `milliseconden` rather
than replacing them — they are not abbreviations, and a reader who
sorts the thirty into "singulars, plurals and abbreviations" will lose
exactly those two. The ten abbreviations (`jr`, `mnd`, `kw`, `wk`,
`dg`, `u`, `min`, `sec`, `s`, `ms`) plus those two are the twelve that
are neither the singular nor the `-en` plural, and **WF-017 refuses all
thirty equally** as a day-kind spelling — as a day kind's `name` and as
its `plural` alike (`IR-SPEC.md` ch. 2.9;
maintainer decision 2026-09-19). The twelve stay ordinary calendar
units wherever a unit is written; golden `jutterswet-vervaltermijn`
carries `wk`, `mnd` and `kw` that way.

**The lookup ORDER is normative, and it is this table first.** "Outside
the table" is a test on the table, so a consumer resolving a duration's
unit spelling consults THIS table first and the declared day kinds'
spellings (their `name` and `plural`, `IR-SPEC.md` ch. 2.9) only where the
table has nothing. Every executor
reads it in that order — the verifier's WF-035/WF-038 clauses, the
reference interpreter, the ASP backend and both Drools branches. The
two Drools branches asked the registry first until 2026-09-20, at all
four of their day-kind positions, and on a document that registered a
calendar spelling as a day-kind unit the executors therefore answered
differently: measured on `jutterswet-bergingsklok` with its unit
spelled `wk` and `wk` registered beside it, the reference interpreter
stepped ± 3 weeks and counted whole weeks (2026-03-31, 2026-02-17, 5,
5) while both Drools branches walked the day kind (2026-04-15,
2026-02-01, 3, 3). WF-017 refuses that document since the same day, so
the wire no longer carries the contradiction; the order is what keeps
a consumer from acting on it either way.

*And "case-insensitive" is read as written.* The same day closed a
second way the executors could part on this table, with no day kind
involved at all: both Drools branches lower-cased only the nine `-en`
plurals, because those are the spellings their plural reducer replaced
while every other one fell through it in its original case. Measured
2026-09-20 on `jutterswet-bergingsklok` with its unit respelled `WK`
and nothing registered — a verifier-clean document — the verifier,
`ref` and the ASP backend reckoned in weeks while both Drools branches
refused the rule as an unknown time unit. That split was LOUD, never
silent, and it cost no corpus verdict; it was still two executors
answering a question the other three answered. A spelling is now
trimmed and lower-cased before the lookup in every reader, which is
the ch. 2.1 identity this table has always been compared under.

**Operand order and precision.** The date stands on the **left** of
both plus and min — a duration-left operand (duration + date, duration
− date) is ill-formed, as is date ± date (WF-035): v0's typing opens
its date branch only for a date-left operand, so plus does *not*
commute. tijdsduur's two operands must be dates of one
precision (WF-038); mixed precision is ill-formed. A fractional
duration count **truncates toward zero** before the calendar step (1,9
maanden steps 1 month; −1,9 truncates to −1). Empty rules: ch. 1.2 —
a leeg date propagates; a leeg duration counts as 0 (the date passes
unchanged); a leeg tijdsduur operand propagates, never 0.

**Executed (2026-08-25, both executors; WF-035/WF-038):** the
day-precision family — tijdsduur and absolute tijdsduur in dagen,
weken, maanden, kwartalen and jaren, and date ± duration in those
units — runs in ref and clingo. Anchor `jutterswet-jutleeftijd`
carries the three worked examples above verbatim (plus the leeg
corner); golden `jutterswet-vervaltermijn` pins the month-end clamp,
the empty-duration standstill, the quarter step and the
fractional truncation. The day-kind (dagsoort-as-unit) forms are their
own section below and run since 2026-09-19. The datum-tijd
millisecond layer is executed in ref per the paragraph below and,
since the pair-term encoding landed (2026-08-28, lifting the
2026-08-25 embargo), in the ASP backend as well —
`spec/ASP-TRANSLATION.md` S6a; corpus golden
`jutterswet-getijdenpaar` pins it through both executors, the borrow
corner included.

#### The day-kind duration forms (§6.10, §6.11)

A **day kind** is not a unit on the spelling table above: it is a
declared predicate over a calendar day (`IR-SPEC.md` ch. 2.9, ch. 3.7).
Written where a duration unit is written, it names *which days count*,
and both duration forms then have to WALK the calendar — there is no
closed formula, because there is no arithmetic that knows which days are
salvage days.

**Resolution — spelling to predicate.** The duration's unit spelling is
looked up among the declared day kinds' spellings — each day kind's `name`
and `plural` — **trimmed and case-insensitive**; one day kind carries it
(WF-001 and WF-017 keep the spellings of two day kinds apart), and its name selects
its single `DayKindDefinition` (case-insensitive again; WF-031 keeps the
definition unique, so there is nothing to disambiguate). A spelling the
calendar table does not know and no day kind carries is ill-formed
(WF-035/WF-038). The definition's `validity` is **not consulted**: it
MUST be always, neither bound written (WF-031), so there is no window to read.

**Evaluation — the predicate of one day.** `P_k(d)` is the definition's
`condition`, evaluated with **the bound day set to d** and nothing else
in scope. That binding IS the runtime meaning of the `BoundDay` node
(`IR-SPEC.md` ch. 4.1; until L4 the context value `de dag`): it is not a
session value — ch. 8.2's evaluation date is — but the arbitrary day the
definition is about, and it denotes nothing anywhere else. An executor that
meets the bound day outside a day-kind evaluation **faults**, naming the form; it never reads as leeg and never
falls back on the evaluation date (red case
`contextwaarde-de-dag-buiten-dagsoort` pins both executors).

`P_k` is a function of **the day alone**. WF-031's purity clause is what
makes that true: the condition reaches no fact base, and — the position
arm, landed with this chapter — no session value either, so the same
document classifies the same calendar day identically in every session.
A definition may reference other day kinds (`P_k` calls `P_j` on the same
bound day, the §9.8 composition); the reference graph is acyclic
(WF-085), and an executor that meets a cycle anyway faults rather than
looping. The composition is carried **on the bound day only**: a nested
test on some OTHER date ("… indien 15-03-2026 een ankerdag is") is a
recognised, unexecuted form, refused by name in all three executors
(red case `dagsoortverwijzing-andere-dag`). The narrow route is
deliberate and was measured: before 2026-09-19 `ref` and the Drools
day-kind compiler answered that shape while the ASP backend refused it,
and one executor computing what another refuses is not a semantics. The
carrying reading is open, not decided against — it needs a source for
what such a test MEANS inside a definition about an arbitrary day.

**§6.10 — the count.** "de tijdsduur van A tot B in hele werkdagen"
counts the days d with `A < d ≤ B` — **start exclusive, end inclusive**
(the "A itself does not count" rule, taken from §6.11's explicit reading,
since §6.10 says nothing about a day kind at all) — that satisfy `P_k`;
signed by operand order, the absolute variant dropping the sign; any leeg
operand → leeg, never 0. The interval bounds the work, so this form
carries **no fence**, and an unsatisfiable day kind counts **0** here: a
count over an empty set is an answer, where a step to a day that does not
exist is not.

Worked examples (absolute; day kind = 25/26 December; each a `ref/` test
verbatim): 20-12-2024 tot 31-12-2024 → **2**; 25-12-2024 tot 26-12-2024 →
**1** (the 25th is the excluded start); reversed, 31-12-2024 tot
20-12-2024 → **−2**, absolute variant **+2**; equal dates → **0**; leeg
tot 31-12-2024 → **leeg**; with an unsatisfiable day kind, 20-12-2024 tot
31-12-2024 → **0**.

**§6.11 — the step.** "datum plus N werkdagen" steps one calendar day at
a time and counts only the days satisfying `P_k`; the result is the Nth
such day, and is therefore always itself a day of the kind. The **start
date never counts**, in either direction, even when it is itself a day of
the kind. `N = 0` — and a leeg N, which coalesces to 0 (ch. 1.2) —
returns the start date **unchanged**, even when that date is not a day of
the kind: the source's general zero rule wins over its "the result is
always such a day" sentence, which is about the stepping case. A
fractional N truncates toward zero before the walk. A negative N (or the
`min` operator) walks backward — the mirror image, which the source leaves
unwritten. A leeg date propagates.

**The reach fence (normative).** A step that passes **40 000 consecutive
calendar days** — a little over 109 years — without finding a day of the
kind is a **fault**, never an endless walk. It is a fence on the GAP, not
on the total: "datum plus 5 000 werkdagen" walks some nineteen years and
is answerable, while a day kind no calendar day satisfies faults after
40 000 days. Red case `dagsoort-onvervulbare-wandeling` pins the fault in
all three executors.

**What the fence claims, and what it does not.** It is a limit of REACH:
the walk looks a bounded distance ahead and reports that it did not get
there. It is **not a satisfiability test**, and neither the rule nor the
fault text says the definition is unsatisfiable — a wording chosen
deliberately, because the two are not the same thing and a reader who
takes the fault as a diagnosis will hunt for a defect that may not exist.
Two bars set the number. The longest recurrence gap a Gregorian day kind
can have is the century-skipped leap day (2096-02-29 to 2104-02-29 is
2 921 days); but a day kind may also be bound to a YEAR or to a
PARAMETER — "the 1st or the 15th of 2040" is a well-formed, pure,
perfectly satisfiable definition — and such a kind lies whole decades
past any date a case is likely to carry. A ten-year reach refused those
and was raised for exactly that reason (maintainer decision 2026-09-19).

**The accepted residual cost, stated rather than hidden.** A satisfiable
day kind whose next occurrence lies beyond the reach is refused although
it exists: "the 1st of January 2200" stepped from 2026 faults, and no
amount of correctness in the definition changes that. Three properties
are what make the trade acceptable. **The refusal of a step beyond the
reach is a FAULT and never a value** — this rule admits no arm that
answers leeg, and each executor is held to that by red case
`dagsoort-reikwijdte-buiten-tabel`, which exists because an early ASP
lowering did answer leeg there in silence. **The fence itself is the same
distance in every executor**, so how far a model may reach is a property
of the language and not of an engine. And **the distance is written
here**, so a model that needs more knows what it is asking for; raising
the number is a spec change, not a configuration knob.

What those three properties are about is the FENCE, and they do not claim
that every executor evaluates every well-formed day-kind condition. Each executor
states its own carried set and refuses the rest **by name** — the ASP
backend, which evaluates the predicate rather than walking it, carries a
named subset of WF-031's admission list (`spec/ASP-TRANSLATION.md`), and
a condition outside it is refused there and computed in `ref`. That is a
loud, per-form boundary and an extensible one; it is not a second fence
and not a silent one.

Worked examples (absolute; day kind = 25/26 December; each a `ref/` test
verbatim): 20-12-2024 + 2 → **26-12-2024**; 25-12-2024 + 1 →
**26-12-2024** (the start is of the kind and still does not count);
26-12-2024 + 1 → **25-12-2025**; 20-12-2024 + 0 → **20-12-2024** (not
itself of the kind); 20-12-2024 + leeg → **20-12-2024**; leeg + 2 →
**leeg**; 27-12-2024 − 1 → **26-12-2024**; 25-12-2024 − 1 →
**26-12-2023**; with an unsatisfiable day kind, 20-12-2024 + 1 →
**fault**.

**The datum-tijd layer (maintainer decision 2026-09-19), and it SPLITS.**
The two forms are not in the same position over millisecond-precision
operands, so they are not decided together.

- **`datum-tijd ± N <dagsoort>` is carried.** The day kind is judged on
  the **civil day part alone**, and after the Nth matching day is found
  the **original local clock time rides through unchanged**. Zero, leeg,
  negative and the reach fence follow the day-precision rules without
  exception — including the leeg-count arm, which returns the datetime
  with its clock time, exactly as it returns the date. Worked examples
  (day kind = 25/26 December; each a `ref/` test verbatim):
  2024-12-20T09:15:30 + 2 → **2024-12-26T09:15:30**;
  2024-12-25T23:59:59 + 1 → **2024-12-26T23:59:59** (the start is of the
  kind and still does not count, and 23:59:59 is preserved across the day
  step); 2024-12-26T09:15:30 − 1 → **2024-12-25T09:15:30**;
  2024-12-20T09:15:30 + 0 → **unchanged**; + leeg → **unchanged**;
  leeg + 2 → **leeg**.
- **`tijdsduur van A tot B in hele <dagsoort>` over two datum-tijden is
  REFUSED**, and stays refused until partial days and boundary instants
  are settled in their own right: counting whole days of a kind between
  two INSTANTS needs a rule for the part-day at each end, and this
  chapter fixes none. The refusal is by name in both OCaml executors, and
  its **precision test runs before the empty rules** — a leeg operand
  gets the refusal that names the form, not the leeg rule of a form that
  is not carried, so the same document cannot answer leeg or fault
  depending on which operand a case happens to fill.

**Executed (2026-09-19, ref and clingo; the Drools backend plays the same
values).** Golden `jutterswet-bergingsklok` closes the square on
hand-computed day-precision values — the step forward and backward, the
zero and leeg counts, the signed and absolute counts — and golden
`jutterswet-getijklok` closes it on the datum-tijd step, clock-time
preservation and all. The three red cases pin the three refusals
(`dagsoort-onvervulbare-wandeling`, `contextwaarde-de-dag-buiten-dagsoort`,
`dagsoortverwijzing-andere-dag`).

**How the ASP backend answers without walking.** A 40 000-day reach makes
a ground per-day walk impossible, so the backend does not build one: it
uses the same purity clause this section opens with. `P_k` is a function
of the day, of literals and of parameters, and the ASP translation is per
case, so the backend EVALUATES the predicate at translation time over a
window and ships the day kind's **extension** — its matching days, each
with its rank. The count is then one aggregate over that table, and the
step is a rank lookup, because "the Nth day of the kind after A" IS "the
day whose rank is rank(A) + N"; the reach fence is read off the same
table. One limit is the backend's own and loud, the S6a pattern: the
window covers the case's dates extended by the reach on both sides, and a
query that leaves it is refused there where ref computes it — never
answered from a table that does not reach.

**Date ± whole calendar units** clamps to the month end: date + n
maanden advances the proleptic month (12·year + month − 1) by n and
clamps the day to the target month's length — 2024-01-31 + 1 maand =
**2024-02-29** (leap), 2025-01-31 + 1 maand = **2025-02-28**; a quarter
is three months, a year twelve (so a leap birthday clamps the same
way); a day or week count is linear (+n / +7·n epoch days). A
fractional count truncates toward zero. (Empty rules: ch. 1.2 — a leeg
date propagates, a leeg duration counts as 0.)

**The datum-tijd millisecond layer.** A millisecond-precision date is
its millisecond count since 1970-01-01T00:00:00 in the same zoneless
civil time — no time zones, no DST, no leap seconds, so every civil
day is exactly 86 400 000 ms. Its civil day and its time-of-day split
by floor division: the time-of-day is always 0 ≤ msod < 86 400 000,
also left of the epoch. Datetime ± duration follows the date rules
above, refined per unit family:

- **Sub-day units are linear on the ms value**: datetime ± n
  uur/minuut/seconde/milliseconde moves n·3 600 000 / n·60 000 /
  n·1 000 / n·1 ms, rolling freely across day boundaries.
- **Calendar units act on the civil day part, preserving the
  time-of-day**: datetime ± n dagen/weken/maanden/kwartalen/jaren
  steps the day part exactly as a day-precision date (linear days and
  weeks; the month-end clamp) and reattaches the time-of-day
  unchanged.

tijdsduur van A tot B over two datetimes (one precision, WF-038)
counts whole units:

- in a **sub-day unit**: the ms difference quot the unit's ms size —
  linear, signed, truncating toward zero;
- in a **day-based unit**: the **borrow rule**, the classic
  silent-error corner: when B's civil day lies ahead of A's but B's
  time-of-day lies *strictly* before A's, the last day is not yet
  complete — one day is borrowed back off B's day part before the
  day-precision whole-unit count above (epoch days, packed months,
  quot); mirrored on the negative side (B's day behind A's, B's
  time-of-day strictly after A's: one day forward). Equal
  times-of-day borrow nothing.

Empty rules are ch. 1.2 unchanged: a leeg datetime propagates; a leeg
duration counts as 0 (the datetime unchanged); a leeg tijdsduur
operand propagates, never 0. A fractional duration count truncates
toward zero before the step, as at day precision. A step whose result
leaves the representable millisecond range is a fault, never a silent
wrap-around (v0's `LocalDateTime.plus` raises `DateTimeException` past
its range). Worked examples (absolute, pinned; each becomes a `ref/`
test verbatim):

- **Sub-day linearity:** 2024-02-28T23:30:00 + 26 uur =
  **2024-03-01T01:30:00** (across the leap day); 2024-03-01T00:15:00
  − 30 minuten = **2024-02-29T23:45:00**; 2024-01-01T00:00:00 +
  1500 milliseconden = **2024-01-01T00:00:01.500**.
- **Time-of-day preservation with the clamp:** 2024-01-31T09:30:00 +
  1 maand = **2024-02-29T09:30:00**; 2024-02-28T23:30:00 + 1 maand =
  **2024-03-28T23:30:00**, and + 2 weken = **2024-03-13T23:30:00**.
- **The borrow corner:** tijdsduur van 2024-02-28T23:30:00 tot
  2024-03-01T10:15:00 in hele uren = **34** (48 h − 13 h 15 min =
  34 h 45 min, whole 34); in hele dagen = **1**, not 2 — B's 10:15
  lies before A's 23:30, so the second day is incomplete. Counting
  the bare day parts would read 2: the silent error the borrow rule
  pins.
- **Borrow across a month count:** tijdsduur van 2024-01-31T10:00:00
  tot 2024-03-31T09:59:00 in hele maanden = **1** (the borrow steps B
  back to 2024-03-30; packed difference 63, quot 32 = 1); one minute
  later, tot 2024-03-31T10:00:00, = **2** (equal times-of-day, no
  borrow; packed difference 64).
- **A negative span:** tijdsduur van 2024-03-01T10:15:00 tot
  2024-02-28T23:30:00 in hele uren = **−34**; in hele dagen = **−1**
  (the mirrored borrow); the absolute variant reads 34 and 1.
- **Empty rules:** leeg + 26 uur = leeg; 2024-02-28T23:30:00 + leeg
  uur = 2024-02-28T23:30:00; tijdsduur van leeg tot
  2024-03-01T10:15:00 in hele uren = leeg.
- **Out of range:** 2024-01-01T00:00:00 ± 2 000 000 000 000 uur =
  **fault**, and + 60 000 000 000 dagen = **fault** — the step leaves
  the ms range; a wrap-around never masquerades as a datetime.

**Executed in both executors (ref 2026-08-25, WF-035/WF-038; ASP
2026-08-28):** the datum-tijd layer runs in `ref/`, pinned by the
worked examples above as unit tests, and in the ASP backend through
the pair-term encoding `dt(civil day, ms-of-day)` — a datetime's ms
value never enters clingo's integer range whole
(`spec/ASP-TRANSLATION.md` S6a; the 2026-08-25 embargo is lifted, its
record kept in S19). Corpus golden `jutterswet-getijdenpaar` replays
through both. One loud asymmetry stands, documented in S6a: a sub-day
span beyond the int31 fence (~23 civil days in uren-counts) faults in
clingo where ref computes it — fail-fast, a corpus constraint, never
a silent divergence.

**Date-part extraction and the Easter builtin.** Year, month and day
extraction ("het jaar / de maand / de dag uit") read the **civil
components of the proleptic Gregorian calendar in zoneless civil
time** — the same (y, m, d) triple the whole-unit definition above is
built on; no time zones, no DST — from a day-precision date, each as a
**unitless whole number**: the year as written, the month **1..12**,
the day of month 1..the month's length. A leeg date propagates
(ch. 1.2b: jaar/maand/dag uit leeg = leeg). "De eerste paasdag van" a
**whole, unitless** year (WF-038) is Easter Sunday of that year per
the Gauss/Anonymous algorithm, normative verbatim (`quot` and `mod`
are integer division truncating toward zero and its remainder, as in
the packed-month rule above):

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

The result is the day-precision civil date (j, maand, dag). A leeg
year propagates; a non-whole or unit-carrying year value is a fault,
and so is a whole year outside the executor's machine-integer range —
never a silent truncation to whole, never a silent wrap-around (one
tracked delta, both cells: v0 narrows the year to a machine int
silently; the verifier makes the non-whole/unit-carrying static shape
ill-formed, while the out-of-range fault is runtime-only — no static
check can bound a computed year). Worked examples (absolute, pinned; each
becomes a `ref/` test verbatim): jaar uit 2026-11-03 = **2026**; maand
uit 2026-11-03 = **11**; dag uit 2026-11-03 = **3**; eerste paasdag
van 2024 = **2024-03-31**, van 2026 = **2026-04-05**, van 2027 =
**2027-03-28**, van 2038 = **2038-04-25**. **Executed (2026-08-25,
both executors; WF-038):** the day-precision extractors and the Easter
builtin run in ref and clingo — golden `jutterswet-paasvondst`
(a maand-uit disjunction, an eerste-paasdag∘jaar-uit date equality, a
jaar-uit/dag-uit Gelijkstelling pair, the leeg corner) carries the
class differentially. Datum-tijd operands, the sub-day extractors and
date construction (datum-met/datumtijd-met) stay unexecuted, loud.

### 3.9 The whole-period qualifier (§8.2)

"gedurende de gehele maand / het gehele jaar" turns a per-instant
condition into a per-bucket one and folds it to a single boolean:

```
geheel(tl, grid, toets) = for EVERY bucket B touched by tl:
                            satisfied-days(B, toets) ≥ threshold(B)
threshold(B) = days(B)          for every toets except the leeg test
             = touched-days(B)  for the leeg test
```

- A value test (ordering, equality, filled, characteristic-holds) demands
  satisfaction on **every day of each touched bucket**: a gap day, a leeg
  day, or a partial-coverage bucket fails it — "at every moment of the
  whole period" is literal.
- The **leeg test** is the one exception: a gap day is itself
  leeg-satisfying, so only the *touched* days need be leeg — a timeline
  leeg throughout its touched buckets passes.
- A timeline with **no segments at all touches no bucket and passes
  vacuously** (the qualifier's negation then fails). A leeg comparison
  bound makes the test false, never a fault. Negation complements the
  folded result.
- A failed **to-one** navigation contributes a null timeline, which passes
  vacuously (the ch. 1.2 leeg-navigation discipline, lifted).
- Over a **to-many** range the fold is conjunctive — every member's
  timeline must pass — and an **empty set passes vacuously**. *(Re-scoped
  2026-09-16.)* That reading is right and it is **not** an implicit
  reading of a set-valued operand: the wire shape it used to describe —
  an `AttributeReference` with `set: true` standing in a condition — was
  ill-formed under WF-096 until L2 and cannot be written since (the flag
  left the wire with B11, and a to-many hop in a condition is WF-053's),
  and it could not distinguish "alle" from "geen van de" even where it
  was written, since the set flag recorded that a quantifier was written
  and never which one. The conjunctive lift is
  therefore stated where the quantifier is: **ch. 3.9a**, the quantifier
  fold, whose matrix is this qualifier applied per member. Counted when
  the clause was re-scoped: **zero** documents in the four zones carried
  the shape it described, so the bullet had never had a witness.
- The **grid is the qualifier's own** — "de gehele maand" or "het gehele
  jaar" — never the tested member's declared granularity. The two are
  independent: a month-granular characteristic tested "gedurende het
  gehele jaar" folds over year buckets, and its true-periods stay
  day-precise either way (ch. 3.1; the pro-rata's whole point, ch. 3.7).
- Date-valued whole-period comparisons (eerder/later over a date
  timeline) are refused in v0 — recorded, not specified (ch. 3.12).

**This qualifier is not a scalar context, and ch. 3.3's refusal does not
reach it** (clarified 2026-09-11). ch. 3.3 refuses a time-dependent
member "in a scalar context" and names its two exits in the same
sentence: "the only ways a timeline becomes a scalar are the dedicated
collapses: the temporal total (ch. 3.6) and the whole-period predicate
(ch. 3.9)". ch. 8.2 states it as the indexing rule ("the only bridges
from a timeline to a scalar are the dedicated collapses") and ch. 3.11 as
the write boundary ("a temporal value in a scalar slot is refused
**except** through the specified collapses"). A condition carrying the
qualifier **is** the second collapse; refusing it as scalar refuses the
exit for being the door. The reading is normative for every executor: a
qualified condition is answered by the fold above, and dropping the
qualifier to answer the unqualified condition instead is a defect, not an
executor's choice of scope.

Two consequences follow for the **characteristic-holds** test, and both
were executor gaps until 2026-09-11:

- The characteristic's truth is its true-period timeline (the ch. 8.2
  apartness: a time-dependent characteristic is delivered as periods and
  nothing else), so the fold reads that timeline. A characteristic with
  no periods at all is the "no segments" case above and passes
  vacuously — it is not the same thing as the characteristic being false.
- Under the qualifier a failed to-one navigation passes vacuously, as the
  fourth bullet says. This **diverges deliberately** from the unqualified
  navigated characteristic check, whose mirrored v0 asymmetry answers
  false for the positive and the negated form alike (ch. 5.8): there the
  absent link decides the check, here the qualifier lifts it to the
  ch. 1.2 leeg-navigation discipline. An executor that answers the
  unqualified asymmetry under a qualifier answers the opposite of this
  clause.

Worked examples (absolute; value timeline per year, tested ≥ 10):
value 12 on the whole of 2024 → all 366 days of the touched bucket
satisfy → **waar**. Value 12 on [2024-01-01, 2024-07-01) only → the 2024
bucket is touched for 182 days but has 366 → **onwaar** (a July moment
carries no value); the negated form → waar. Same half-year timeline
under the **leeg** test → touched days must be leeg, but they carry 12 →
**onwaar**.

**Where the buckets come from, on a VALUE timeline** (stated apart
because it is the one place the formula can be implemented backwards and
still look right). The touched buckets are the ones the **value**
timeline touches — not the ones the satisfying days touch. On a
characteristic the two coincide, because a characteristic's timeline IS
its true-periods, which is why the distinction never had to be drawn
before the value test was built. On a value timeline they come apart at
exactly the case that matters: a value of 30 across the whole of 2026,
tested `> 40`, has **no** satisfying day, so a fold that took its buckets
from the satisfying days would touch no bucket and pass vacuously — the
opposite of the answer. The buckets are the timeline's; the satisfied
days are the predicate's; the threshold is the bucket's calendar length.

Worked examples — the **ordering** test over a value timeline (absolute;
`ref/` tests and the ASP backend's suite verbatim; month-granular
attribute `dagvergoeding`, qualifier "gedurende het gehele jaar",
tested `> 40`):

| # | value timeline | fold | verdict |
|---|---|---|---|
| a | no segments | no bucket touched | **waar** |
| b | 50 on [2026-01-01, 2027-01-01) | bucket 2026: 365 of 365 satisfy | **waar** |
| c | 30 on [2026-01-01, 2027-01-01) | bucket 2026: 0 of 365 | **onwaar** |
| d | 50 on [2026-01-01, 2026-07-01) | bucket 2026: 181 of 365 | **onwaar** |
| e | 40 on [2026-01-01, 2027-01-01) | strictly greater: 0 of 365 | **onwaar** |
| f | as e, tested `≥ 40` | 365 of 365 | **waar** |

Row e is the operator, not the fold: `>` is strict, and the clause says
so here because the whole point of the whole-period reading is that it
changes WHEN the test is asked, never WHAT it asks. The qualifier's own
negation complements each verdict — a → onwaar, b → onwaar, d → **waar**
— and under the **month** grid row d passes (January through June are
each touched in full) while a value on [2026-01-15, 2026-03-01) fails it
(January is touched for 17 of 31 days). A **leeg** segment inside the
period is a day that does not satisfy the test: 50 on the first half of
2026 and explicit leeg on the second reads **onwaar**, never a fault
(ch. 1.2d — a comparison against leeg is false).

When the temporal side stands on the RIGHT ("40 is kleiner dan zijn
dagvergoeding gedurende het gehele jaar") the operator is **reversed**,
never negated. On totally ordered filled values the two coincide; on a
leeg operand they do not, because ch. 1.2d makes BOTH orderings false
there, so a reader that negated instead of reversing would turn every
leeg day into a satisfying one.

Worked examples — the **characteristic-holds** test over a *navigated*
characteristic (absolute; `ref/` tests and the ASP backend's suite
verbatim). Document: a Schuit reaches one Werf over the to-one role
`werf`; `bedrijvig` is a month-granular characteristic of Werf; the rule
is *de toeslag van een Schuit is de lengte van de Schuit indien de werf
van de Schuit gedurende het gehele jaar bedrijvig is*. Session:
evaluation date 2026-03-15, S1.lengte 8.

| # | link | `bedrijvig` true-periods | fold | toeslag |
|---|---|---|---|---|
| a | none written | — | null timeline, no bucket touched → **waar** | 8 |
| b | W1 | none | no segments, no bucket touched → **waar** | 8 |
| c | W1 | [2026-01-01, 2027-01-01) | bucket 2026: 365 of 365 → **waar** | 8 |
| d | W1 | [2026-01-01, 2026-07-01) | bucket 2026: 181 of 365 → **onwaar** | leeg |

The same document under the qualifier's own negation ("**niet** gedurende
het gehele jaar bedrijvig") complements each verdict: a → onwaar, b →
onwaar, c → onwaar, d → **waar** (8). Under the inner negation
("gedurende het gehele jaar **niet** bedrijvig") the touched buckets are
unchanged and the satisfying days are their complement: a → waar, b →
waar, c → onwaar (bucket 2026 satisfies 0 of 365), d → onwaar (184 of
365). Under the **month** grid ("gedurende de gehele maand bedrijvig")
row d passes — January through June are each touched in full, 181 of 181
days — while a true-period of [2026-01-15, 2026-03-01) fails it (January
is touched for 17 of 31 days).

### 3.9a The quantifier fold — a condition over a linked set

A **quantifier fold** (`IR-SPEC.md` ch. 4.5, wire `QuantifierFold`, v0
`Kwantorvouw`; WF-108)
asks one condition of every member of a linked instance set:

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

`reach ∩ population` and `filter` are **ch. 3.4a's**, unchanged and
shared: the range (`sourceRole`, or the multi-step `sourcePath` of
ch. 3.4b — since L2 exactly one of the two, B12; until L2 the derived
connecting role when neither was written) says which instances the
quantifier reaches; `elementType` says which it
ranges over, extensionally (`IR-SPEC.md` ch. 2.2) — every instance whose
type is ⊑ it; and the die/dat `filter` drops contributors, evaluated per
contributor with the element as subject. There is one definition of
"which members", not two, and one implementation of it per artifact.

`test` is evaluated the same way — **per contributor, the element as the
condition's subject**. That is the whole difference between the two
condition fields: `filter` says which members the sentence is about,
`test` says what it claims about them.

**`none` is the per-MEMBER complement, never the fold's.** `not (all)` and
`none` are different conditions and the difference shows on exactly the
members whose test is false for a *temporal* reason: a member whose
timeline does not cover the period fails the matrix, so it does not block
`all`'s failure and it does not block `none` either.

**Order and short circuit** — two disciplines meet, each taken from the
node it belongs to, and neither is invented here. The **contributors are
settled in full first**, filter included, exactly as an instance fold
settles them (ch. 3.4a): the subselection decides what the sentence is
about, and that question is not short-circuited by its answer, so a
filter that faults on a later member faults whether or not an earlier
member has already decided the verdict. The **matrix then short
circuits**, exactly as a composite condition does (ch. 5.10): evaluation
stops at the first contributor whose verdict settles the fold, in the
contributors' own order (ch. 5.11), and a member never reached produces
no fault and no §9.5 channel fact.

**The vacuous case is stated, not inherited.** An empty contributor set
reads **waar** under both quantifiers. It is the empty conjunction, and
it is the same answer the empty set gets everywhere else a universal
appears in this document (ch. 3.9's empty to-many range, ch. 5.10's
`all` composite over no parts) — but it is written down here because it
is the one rule of this node that a reader is most likely to supply from
intuition and get backwards, and because the corpus pins it.

**The counting quantifiers count the CONTRIBUTORS the matrix holds of**
(`IR-SPEC.md` ch. 4.5, WF-108 (e); the threshold is `Compound`'s field
under `Compound`'s rule, WF-041: at least 1). They are not a second kind
of fold: `all` is `count = |contributors|` and `none` is `count = 0`
written the other way round, so **one** rule of evaluation covers all
five, and it is ch. 5.10's — the contributors are settled in full first,
the matrix is then read in the contributors' own order (ch. 5.11) and
stops at the first one after which the outcome stands, with ch. 5.10's
trigger table applied verbatim, the contributors as its members. Two of
its rows fire on the **empty prefix** here as they do there and are the
sharper for ranging over a set rather than a written list: `atLeast` *n*
and `exactly` *n* over fewer than *n* contributors are **onwaar** having
read nothing, and `atMost` *n* over at most *n* contributors is **waar**
having read nothing.

**The empty set needs no separate principle under these three.** A count
of 0 answers each of them by itself — `atLeast` *n* (n ≥ 1) onwaar,
`atMost` *n* waar, `exactly` *n* onwaar — so the paragraph above is a rule
about `all` and `none` and is not extended to them. Those two would come
out the same way from the count; they are stated separately because that
is where the intuition slips, and the counting arms are stated here
because a reader might expect a vacuum rule and there is none to apply.

**A NIET prefix on the matrix lands on the COUNT, because a threshold
cannot be flipped.** ch. 3.9's "niet gedurende …" qualifies the outer
comparison, so under `all` and `none` an executor may fold it by
flipping the quantifier (∀c.¬P = ¬∃c.P) — and two of the three do. No
such move exists for `atLeast` 2: there is no quantifier it turns into.
The negation is therefore read where it is written, per contributor, and
what it changes is the count: `count(¬P) = |contributors| − count(P)`,
an equality and not an approximation, because the whole-period predicate
is total — every contributor either satisfies it or does not, the member
with no timeline at all included (it passes vacuously, so it counts on
the POSITIVE side; ch. 3.9's rule, unchanged). The empty set stays
0 − 0 = 0 and does not become "everything denied". Witness
`corpus/golden/jutterswet-sleepvloot`, whose cells are chosen so that
counting P instead of ¬P gives the other verdict in four of them.

**Worked examples — the §8.2 form (absolute; `ref/` tests, the ASP
backend's suite and `corpus/golden/jutterswet-ploegpremie` verbatim).**
Document: a `Bergingsploeg` reaches its `bergers` over the to-many role
of `ploegindeling`; `dagvergoeding` is a month-granular attribute of
`Berger`; the rule is *een Bergingsploeg is premiegerechtigd indien de
dagvergoeding van alle bergers van de Bergingsploeg gedurende het gehele
jaar groter is dan 40*. The matrix is therefore ch. 3.9's ordering test,
asked of each berger's own timeline.

| # | the ploeg's bergers | per-member verdict | fold |
|---|---|---|---|
| 1 | 50 and 60, each the whole of 2026 | waar, waar | **waar** |
| 2 | 50 and 30, each the whole of 2026 | waar, onwaar | **onwaar** |
| 3 | 50 the whole year; 60 on [2026-01-01, 2026-07-01) only | waar, onwaar (181 of 365) | **onwaar** |
| 4 | none | — | **waar** (vacuous) |
| 5 | 40, the whole of 2026 | onwaar (strictly greater) | **onwaar** |
| 6 | 50 the whole year; one berger with no timeline at all | waar, waar (no bucket touched) | **waar** |
| 7 | one berger, no timeline at all | waar (no bucket touched) | **waar** |

Row 3 is the gap and row 4 the empty set; together they are the two ends
the fold is most often got wrong at, and they disagree on purpose — a
member the relation reaches but whose timeline does not cover the period
**fails**, while a set the relation does not reach at all **passes**.
A member with **no timeline at all** is the third case and sides with
row 4, not row 3: it touches no bucket, so ch. 3.9 passes it vacuously
and the fold does not stumble over it. That is ch. 3.9's rule applied
unchanged rather than a rule of this chapter, and it is pinned so that an
executor which collapses "absent" into "gap" fails by name.

**Under `none` that same member changes the sign, and the two vacuous
readings part company.** An absent timeline makes the member SATISFY its
matrix vacuously, so it satisfies what `none` says nobody satisfies: a
ploeg whose single berger has no `dagvergoeding` at all reads
premiegerechtigd **waar** and wisselvallig **onwaar**, while a ploeg with
no bergers reads **waar** on both. Vacuous-at-the-member and
vacuous-at-the-set are different vacua and only one of them is the empty
conjunction of this chapter; row 4's rule is about the SET, and nothing
about an empty set follows from a member that happens to be empty. The
corpus pins the pair side by side for that reason.

Under **`none`**, on the same document: bergers at 30 and 20 the whole year
→ **waar** (no member is above 40); 30 and 50 → **onwaar**; no bergers →
**waar**; a single berger at 50 on the first half of 2026 only →
**waar**, because he does not have more than 40 *gedurende het gehele
jaar* either. Rows 6 and 7 are where `none` turns over: both read
**onwaar**, because a member with no timeline satisfies the matrix
vacuously and `none` denies exactly that. Row 7 is the one that has to be
pinned on its own — in row 6 the berger at 50 already decides `none`, so
that row cannot tell the empty member from the full one.

With a die/dat subselection — *alle bergers die een dagvergoeding van
meer dan 10 hebben* — a berger at 5 the whole year is not a contributor,
so a ploeg of one berger at 50 and one at 5 reads **waar**, and a ploeg
whose only berger is at 5 reads **waar** vacuously.

**Worked examples — a NON-temporal matrix** (absolute;
`corpus/golden/jutterswet-schouwploeg` verbatim). `test` is an ordinary
condition, so it need not be temporal at all, and until 2026-09-16 the
three executors disagreed about exactly that. They agree now, each by
executing the sentence above rather than by recognising one matrix shape.
Document: a `Schouwploeg` reaches its `wachten` over the to-many role of
`schouwindeling`; `dienstjaren` is a plain attribute of `Strandwacht` and
`gediplomeerd` a characteristic of it. Four folds in one document:
*ervaren* = `all` over `dienstjaren > 5`, *onervaren* = `none` over the
same, *bevoegd* = `all` over `gediplomeerd`, *sluitend* = `none` over
`niet gediplomeerd`.

| # | ploeg | the ploeg's wachten (dienstjaren, diploma) | ervaren | onervaren | bevoegd | sluitend |
|---|---|---|---|---|---|---|
| 1 | S1 | 8 ja, 6 ja | **waar** | onwaar | **waar** | **waar** |
| 2 | S1 | 8 ja, 3 nee | onwaar | onwaar | onwaar | onwaar |
| 2 | S2 | 9 ja | **waar** | onwaar | **waar** | **waar** |
| 3 | S1 | 3 ja, 5 ja | onwaar | **waar** | **waar** | **waar** |
| 4 | S1 | none | **waar** | **waar** | **waar** | **waar** |
| 5 | S1 | leeg ja, 3 ja | onwaar | **waar** | **waar** | **waar** |

Row 2 is `none` ≠ `not (all)` again, now with no temporal reason in the
matrix at all: one member above the bound and one below leaves both folds
false. It also holds a SECOND ploeg in the same case, and that row is the
RANGE: S2's single wacht is above the bound, so S2 reads *ervaren* while
S1 does not, on one run over one population. An executor that quantified
over every `Strandwacht` rather than over the ploeg's own `wachten` would
drag S1's wacht at 3 into S2's fold and read S2 the other way — which is
the failure mode any implementation that reifies the matrix into a helper
defined for EVERY element has to avoid. Row 3 pins the strict bound, row 4
the vacuous set under both quantifiers. Row 5 is the **leeg** member, and
it is the counterpart of
row 7 above: an ordering comparison over leeg is onwaar (ch. 1.2d), so the
member FAILS its matrix — it breaks `all` and is no witness for `none` —
whereas an absent TIMELINE PASSES its whole-period matrix vacuously and
therefore breaks `none`. Nothing but the matrix decides which of the two
readings applies, which is why both are pinned.

*sluitend* is the **negated matrix**, and it answers what *bevoegd*
answers in every row. On a non-temporal matrix the "niet" is part of the
condition and is read per member, so the quantifier is untouched — unlike
ch. 3.9's "niet gedurende …", which qualifies the outer comparison and
which an executor may therefore fold by flipping `all` into `none`
(∀c.¬P = ¬∃c.P). Both are the same verdict; stating where the negation
lives is what keeps an executor from applying that flip twice.

**Worked examples — the counting quantifiers** (absolute;
`corpus/golden/jutterswet-reddingsvloot` verbatim). Document: a
`Reddingsvloot` reaches its `sloepen` over the to-many role of
`vlootindeling`; `vaartijd` is a month-granular attribute of
`Reddingssloep` and `gekeurd` a characteristic of it. One matrix — the
ch. 3.9 whole-period test *vaartijd > 20 gedurende het gehele jaar* —
under three quantifiers, plus a fourth fold that adds a die/dat
subselection to the same matrix: *inzetbaar* = `atLeast` 2,
*krap* = `atMost` 1, *paraat* = `exactly` 1, *geschikt* =
`atLeast` 2 over the sloepen *die gekeurd zijn*. `sloeptal` is the
plain instance fold beside them, so every row also states how large the
unfiltered range is.

| # | vloot | the vloot's sloepen (vaartijd, keuring) | sloeptal | count | inzetbaar (≥2) | krap (≤1) | paraat (=1) | geschikt (≥2, gekeurd) |
|---|---|---|---|---|---|---|---|---|
| 1 | V1 | 25 gekeurd, 30 gekeurd | 2 | 2 | **waar** | onwaar | onwaar | **waar** |
| 2 | V1 | 25 gekeurd, 10 gekeurd | 2 | 1 | onwaar | **waar** | **waar** | onwaar |
| 3 | V1 | none | 0 | 0 | onwaar | **waar** | onwaar | onwaar |
| 4 | V1 | 25 gekeurd, 30 gekeurd on Jan–Jun only | 2 | 1 | onwaar | **waar** | **waar** | onwaar |
| 5 | V1 | 25 gekeurd, 30 NIET gekeurd, 10 gekeurd | 3 | 2 | **waar** | onwaar | onwaar | onwaar |
| 5 | V2 | 30 gekeurd | 1 | 1 | onwaar | **waar** | **waar** | onwaar |
| 6 | V1 | 25 NIET gekeurd, 30 NIET gekeurd | 2 | 2 | **waar** | onwaar | onwaar | onwaar |

The `count` column is the count of the formula — over the UNFILTERED
contributors, which is what the first three folds range over; *geschikt*
counts over the `gekeurd` survivors and its own counts are 2, 1, 0, 1, 1,
1 and 0 in the seven rows.

Rows 1 and 2 pin each threshold **from both sides at once**, which is
what one shared matrix buys: a count of 2 satisfies `atLeast` 2 exactly
at its threshold and breaks `atMost` 1 by exactly one, and a count of 1
turns both of those over while satisfying `exactly` 1. Row 3 is the empty
set under all three — and it is the paragraph above made observable: a
count of 0 decides each quantifier without any vacuum rule, so *krap* is
**waar** where *inzetbaar* and *paraat* are **onwaar**, which no single
"empty means true" principle would give. Row 4 is ch. 3.9's gap, reused
unchanged: the member with the half-year timeline fails the matrix, so it
simply does not count.

Row 5 carries TWO vloten and both of them earn their place. V1 is the
cell **the filter alone decides**: *inzetbaar* and *geschikt* ask the same
question of the same matrix and part company here, because the sloep at
30 is not `gekeurd` and the surviving pair holds only one satisfier.
Removing the `filter` from the wire makes *geschikt* read **waar** and
nothing else in the document moves. V2 pins the **range** the way
`jutterswet-schouwploeg` row 2 does — it reads *krap* and *paraat* in the
same run in which V1 reads neither, so an executor quantifying over every
`Reddingssloep` rather than over the vloot's own sloepen answers it the
other way — and it is also where ch. 5.10's **empty-prefix** triggers
fire: over one contributor, `atLeast` 2 is out of reach before the
matrix is read once, and `atMost` 1 can no longer be exceeded, so both
answer having read nothing. Row 6 is the empty SELECTION beside a filled
range: two satisfying sloepen, neither `gekeurd`, so *inzetbaar* is
**waar** on a count of 2 while *geschikt* counts over ∅ and is **onwaar**
— the counting mirror of the vacuous cell, and the one place where an
executor that dropped the filter and one that dropped the empty-set
reading would answer differently.

### 3.10 Period conditions and the §8.4.3 clip

A **period condition** (`TimeWindow`, `IR-SPEC.md` ch. 4.5) restricts a
rule to a date window. Two cases:

**Scalar target — the evaluation-date gate (v0 behaviour, mirrored).**
The rule applies iff the evaluation date lies in the window: `from`
inclusive, `to` exclusive, inclusive where the source wrote "tot en met"
(`toInclusive`); an absent bound is
open; an unset evaluation date never matches; a leeg bound never matches.
Worked example: window [2024-01-01, 2025-01-01), evaluation date
2024-12-31 → applies; 2025-01-01 → does not; the "tot en met 2024-12-31"
form still applies on 2024-12-31. *Well-formedness (maintainer decision
2026-09-23):* a bound is a date literal or the subject's own scalar date
attribute, of day precision (`IR-SPEC.md` WF-111 — the precision clause
closes a measured split: a LEEG datum-tijd bound played in `ref`, which
refused only a filled instant, while the ASP backend refused the bound
statically), and the window never
sits under `none` or a counting quantifier (WF-112). Both rules promote
what used to be a v0 mirror note about backend maturity: v0's backends
lower only those two bound shapes and refuse the condition under counting
quantifiers, while v0's IR admits any date expression (WF-042). The
general date expression is an OPEN wire decision (ch. 3.12).
**Executed since 2026-09-20** by all four executors — the six clauses and
this worked example are pinned case by case in
`corpus/golden/jutterswet-premievenster`, and the example runs verbatim in
each OCaml executor's own suite. Per WIRE POSITION, because the mirror
note's two bounds land on different ones and a single "executed" would
hide that:

| the window sits | status |
|---|---|
| as a rule's whole condition | executed by all four (five rules in the golden, one per spelling) |
| as a member of an `all` or `the` composite | executed by all four (the golden's `borgpremie`). In a consistency assertion, as its whole criterion or an `all` member, executed by all four since 2026-09-23: both Drools branches refused it until card `drools-venster-vouwtoets` (witness `VensterAssertieWitnessTest` in the Drools backend; no golden) |
| in the `test` or the `filter` of an `all` quantifier fold, or in the die/dat `filter` of an aggregation | executed by all four since 2026-09-23, the member as the window's subject: a bound that is an attribute is read on each member. The Drools exec-model branch refused all three positions until card `drools-venster-vouwtoets`; golden `corpus/golden/jutterswet-wachtvenster` pins them, each ploeg carrying a same-named date of its own that a reading on the fold's subject would take |
| under `none` or a counting quantifier | ill-formed since 2026-09-23 (WF-112; negatief `wf112-periodevenster-tellende-kwantor`). Before that all four executors refused it, and their refusals remain as fail-fast checks — both Drools branches' by name since card `drools-venster-vouwtoets`, at every position that holds a condition: a rule condition, a quantifier fold's `filter` and `test`, an aggregation filter, a consistency assertion and a fact-creation guard. The rule reaches into a quantifier fold's `filter` and `test` too: measured that day, the Drools DRL-text branch answered the fold under all five quantifiers, the complement included |

Both bound shapes WF-111 admits are executed (a date literal and the
subject's own scalar date attribute). Any other bound is ill-formed:
`corpus/negatief/wf111-periodegrens-parameterdatum`, a date parameter, was
until 2026-09-23 the red witness that all four executors refused. The two
OCaml executors keep their refusal by name, with the phase in the sentence,
as a fail-fast check on input that has not been verified.

**Temporal target — the clip (v1 semantics, delta).** When the rule's
target is time-dependent, gating all-or-nothing on the evaluation date is
the wrong axis: the window speaks about the *timeline*, not about the
evaluation moment. v1 specifies the **clip**: the produced timeline is
intersected with the window,

```
written(tl, window) = tl restricted to [from, to)
```

and the write proceeds with the clipped timeline. Worked example: the
rule computes {2024: 10, 2025: 12} yearly; window [2025-01-01, open) →
written value {2025: 12}; days in 2024 stay whatever another version or
producer gives them — a gap, absent other writers. **Delta (tracked,
`CONSTRUCTS.md` rows 11/29):** v0 refuses this case outright (the
manifest family "period-comparison clipping, §8.4.3"); v1 supplies the
clip semantics above, and `ref/` implemented them first.

The clip's clauses. `corpus/golden/jutterswet-pachtvenster` pins every one
whose case lies on the declared grid. The one it cannot hold, a bound
inside a bucket, is pinned in the two OCaml executors' own suites (`ref`
`WV-3.10-clip-knipt-segment-houdt-waarde` and the day-count example below
verbatim; the ASP backend reading its `tl/5` atoms), because the case
format pins every expected timeline to the grid (ch. 3.12, OPEN):

- **The window is the rule's whole condition, and the evaluation date plays
  no part.** The window says which days of the timeline are written, so the
  write happens with an unset evaluation date and with one outside the
  window alike. The scalar gate's clause "an unset evaluation date never
  matches" is about the evaluation moment and has no counterpart here.
- **The bounds read as the gate's do:** `from` inclusive, `to` exclusive,
  "tot en met" (`toInclusive`) inclusive (the window then ends the day
  after), an absent
  bound open, bound shapes per `IR-SPEC.md` WF-111. A bound that is the
  subject's own attribute is read per subject.
- **The restriction cuts SEGMENTS, never values.** A segment that straddles
  a bound keeps its value on the part inside the window: `tl restricted to`
  is the restriction of a function to a smaller domain, so the producer's
  value is not recomputed for the shorter span. A monthly day count of 31
  cut at the fifth of the month still reads 31 on the days that remain.
  *Suite-pinned only* (see above).
- **An empty window writes an empty timeline.** A leeg bound puts no day in
  the window (the gate's "a leeg bound never matches", read per day), and a
  window whose `from` lies after its `to` has no days either. A window
  that misses the produced timeline leaves nothing of it. The clipped result
  is then the temporal leeg, and on a standing temporal target it writes
  nothing (ch. 7.8), as for any producer.

**Executed since 2026-09-23** by `ref` and the ASP backend on the attribute
targets whose producers they carry (the ch. 3.7 pro-rata part and the
ch. 3.8 day count). Both Drools branches refuse the clip by name, so the
golden sits in their channel lists. Two positions stay refused by name in
both OCaml executors, each at the rule and before the window is read,
because the case distinction is on the target and a condition cannot see
one:

- the window as a MEMBER of a composite on a temporal target. This chapter
  says what the window does to a timeline. It does not say what the
  composite's other members then gate, clip or leave alone. OPEN.
- the clip on a temporal CHARACTERISTIC target (red case
  `corpus/rood/periodevenster-tijdlijnkenmerk`). Neither OCaml executor
  writes a temporal characteristic (ch. 3.11), so the timeline the window
  would cut is never produced. "Time-dependent" is asked of BOTH declaration
  tables, the attribute's granularity and the characteristic's. An
  executor that read only the first applied the scalar gate to a timeline
  (measured 2026-09-20, before the fix, in the ASP backend). Each
  executor's own suite pins the refusal.

The analogous interaction with a rule *version's* validity window is **not**
decided here: a validity window on a temporal write mixes the
version-selection axis with the timeline axis, and belongs to the
single-axis decision of ch. 8 (PLAN §9, decision 2), which is OPEN there
and recorded in ch. 3.12. The golden carries no validity window on its
temporal rules for that reason.

### 3.11 Temporal writes

A rule with a time-dependent target writes a **whole timeline at once**,
per subject: the periodization comes from the value expression's
producer, never from evaluating a scalar rule "once per period". The
producers, each specified above: the per-bucket counters (ch. 3.8), the
pro-rata (ch. 3.7), cut-merge arithmetic (ch. 3.4), the temporal sum
(ch. 3.5) — and, for a time-dependent **characteristic**, the condition
itself: the written value is the condition's true-period timeline (the
target holds exactly where the condition holds, false elsewhere — the
monotone reading; no complement is seeded). A write replaces the target's
previous timeline; whether that is a change is decided by segment-wise
equality (ch. 3.1), which is what makes rewriting to a fixpoint
idempotent (ch. 5).

- A **scalar value into a temporal target** is refused in v0 (no
  broadcast — ch. 3.4's OPEN); a **temporal value in a scalar slot** is
  refused except through the specified collapses (ch. 3.3). Neither
  boundary is ever crossed by silent coercion.
- **Temporal characteristic, refused cases (v0):** the manifest family
  "temporal kenmerktoekenning with indeterminate write-producer" —
  (a) an *unconditional* temporal characteristic: "always true" has no
  finite segment list (the time-domain obstacle, as in ch. 3.4's
  broadcast — genuinely under-determined, **OPEN**); (b) a non-`altijd`
  validity window: the ch. 3.10 axis question, OPEN with it; (c) a
  condition whose true-periods v0 cannot bound — a negated or navigated
  characteristic, or a compound containing one; the v0-lowered set is
  the conjunctive and at-least-one compounds of *own single-period*
  characteristics (intersection and union of true-period sets — a pure
  at-least-one disjunction lowers in v0 already; no delta there). Of
  the refused cases, v1 supplies semantics for exactly one — **delta
  (promotion of a refusal into semantics)**: the true-periods of a
  **negated** characteristic are the complement of its true-periods
  **within the span the write is clipped to** (ch. 3.10's clip); an
  unbounded complement hits the same time-domain obstacle as (a) and
  stays OPEN outside a clip, and the *navigated* case stays OPEN with
  no semantics claimed. *That last clause is about this section's
  subject — a condition whose true-periods become a WRITTEN timeline —
  and says nothing about READING a navigated temporal characteristic,
  which ch. 3.9's whole-period qualifier specifies and both OCaml
  executors carry: the qualifier folds the reached timeline to a
  boolean and produces no timeline at all, so it needs no time domain
  and meets no part of this obstacle.*

Worked examples (`ref/` tests verbatim):

- *Monotone characteristic write.* Condition: the subject's
  characteristic `recht` holds on [2024-03-01, 2024-09-01). Written
  target characteristic: holds on exactly [2024-03-01, 2024-09-01),
  false everywhere else — no complement is seeded.
- *Negated characteristic under a clip (the delta).* Window
  [2024-01-01, 2025-01-01), `recht` as above, condition "niet recht" →
  written true-periods [2024-01-01, 2024-03-01) ∪
  [2024-09-01, 2025-01-01).
- *Idempotence.* The target already holds {[2024-03-01, 2024-09-01)}
  and the rule computes the same segment list → segment-wise equal → the
  write is a no-op; computing {[2024-03-01, 2024-10-01)} instead → not
  equal → a real write (the fixpoint machinery of ch. 5 sees a change).
- **Anchored targets** compose with the producers via the anchored grid
  (ch. 3.2); v0 additionally refuses the temporal-sum-over-a-set producer
  on an anchored target (a set fold across per-object grids) — recorded,
  OPEN.

### 3.12 v0 refusals and open decisions (summary)

The chapter's tracked debt, in one place. "Refused (mirrored)" = v0
markers/skips it and this spec records the refusal; "OPEN" = v1 must
decide; "specified (delta)" = this chapter supplies semantics v0 lacks.

| Subject | Status |
|---|---|
| Multi-period grids (`count` ≥ 2): all reads and writes | refused (mirrored); OPEN — specify producers or restrict `count` = 1. A parameter's facet is restricted to `count` = 1 since L3 (`IR-SPEC.md` ch. 2.10, WF-005), because no executor evaluates a parameter's count |
| Bucketing by day/week/quarter | absent (mirrored); OPEN with the grid decision |
| Fixed start point off the calendar grid (§13.3.6.3–.7) | no wire field; the RegelSpraak frontend refuses the source at emission by name (ch. 3.2); the calendar start point is omitted without loss; OPEN — a v1 wire card carries it or keeps the refusal |
| Point-in-time read (the "op dat tijdstip" node) | **landed (v1 extension)**: `read(tl, d)` is `IR-SPEC.md` ch. 4.3a (`TemporalRead`, WF-049; ch. 3.3, row 11), anchor-witnessed and replayed by both executors |
| Timeline arithmetic beyond `plus`/`minus` (plus/min): `reducedBy` (verminderd-met), `times` (maal), `dividedBy` and `dividedByAbs` (division), `percentageOf` (percentage) | refused (mirrored); OPEN — per-period lifting of ch. 1.2 rules is the candidate |
| Scalar broadcast into timeline arithmetic / unconditional temporal characteristic | refused (mirrored); OPEN — both need a time domain for unbounded segments |
| `minimum`/`maximum`/`first`/`last` (min/max/eerste/laatste) over timelines | refused (mirrored); OPEN — the two readings (per-period vs over-time) must be defined apart |
| §8.4.3 period condition on a **scalar** target | **specified and executed, ch. 3.10** — all four executors since 2026-09-20, witness `corpus/golden/jutterswet-premievenster` (six clauses; as a rule's whole condition and as an `all`/`the` composite member) |
| §8.4.3 period condition under `none` or a counting quantifier | **ill-formed (WF-112, maintainer decision 2026-09-23)**: the window gates the rule, and no reading is specified for its complemented or counted verdict (the time-free session is where the two readings part). Covers a quantifier fold's `filter` and `test` too. Negatief `wf112-periodevenster-tellende-kwantor`; the executors' refusals remain as fail-fast checks, both Drools branches' by name since card `drools-venster-vouwtoets` |
| §8.4.3 period condition on a temporal target | **specified (delta) and executed: the clip, ch. 3.10** — `ref` and the ASP backend since 2026-09-23, on attribute targets with the ch. 3.7/3.8 producers. Corpus-pinned by `corpus/golden/jutterswet-pachtvenster` wherever the case lies on the grid (the evaluation date ignored — unset and outside every window alike —, a leeg or empty window writing the empty timeline); the SEGMENT-CUTTING clause is suite-pinned only, in both OCaml executors. Both Drools branches refuse it by name. Still refused by name in both OCaml executors: the window as a composite MEMBER on a temporal target (OPEN), and a temporal CHARACTERISTIC target (red case `periodevenster-tijdlijnkenmerk`), because no temporal characteristic is written (ch. 3.11) |
| A corpus carrier for a clipped (off-grid) bucket | **OPEN** — a corpus carrier for a cut bucket needs the case schema (`uchar-case-v0`, `timelineValue`) to release EXPECTED timelines from the declared grid; until then the clause lives in the executors' suites. The alternative, a WF rule demanding on-grid bounds for a clip, is not taken |
| A time window inside the `filter` of an `all` quantifier fold, in a time-free session | **OPEN, no WF-112 widening** (maintainer decision 2026-09-23): `ref`, the ASP backend and the Drools DRL-text branch answer consistently — the window never matches, the member is filtered out, the fold is vacuously true — so there is no unnamed complement reading and no ground for refusal. Since 2026-09-23 (card `drools-venster-vouwtoets`) the Drools exec-model branch carries it too, so all four executors answer it alike; `corpus/golden/jutterswet-wachtvenster` pins the vacuous reading in all four (`bevoegd` in `case3-tijdvrij`) |
| A period-condition bound other than a literal or an own scalar attribute | **ill-formed (WF-111, maintainer decision 2026-09-23)**; negatief `wf111-periodegrens-parameterdatum`. The general date expression (a parameter, a navigated read, arithmetic) is an **OPEN wire decision**: no corpus document asks for it, and a bound not tied to the subject gives the ASP encoding no fault atom to attach to. A v1 card that takes it on moves WF-111 and gives ch. 5.11's `from`-before-`to` order its first witness |
| Negated temporal-characteristic condition | **specified (delta) within a clipped span, ch. 3.11**; unbounded complement OPEN; the navigated case stays refused/OPEN — as a WRITE producer only; the navigated whole-period READ is the row below |
| Version-validity window on a temporal write | OPEN — the ch. 8 axis decision |
| Date-valued whole-period comparison | refused (mirrored) |
| Whole-period qualifier, the **characteristic-holds** test (own and navigated, both grids, the qualifier's negation) | **specified and executed, ch. 3.9** — all three executors, witnessed by `corpus/golden/synth-gehelejaarkenmerk`; the two OCaml ones carry it since 2026-09-11, the Drools backend carried it already |
| Whole-period qualifier over a characteristic, the **inner** negation ("gedurende … NIET") | specified (ch. 3.9's formula), executed by the two OCaml executors; the Drools backend refuses it by name in both branches, so it stays suite-pinned and out of the corpus |
| Whole-period qualifier, the **ordering/equality** test over a value timeline | **specified and executed, ch. 3.9** — the reference interpreter and the ASP backend built it 2026-09-16 (the heelperiode-forall card), the Drools backend carried it already; witnessed by `corpus/golden/jutterswet-ploegpremie` and by each executor's own suite. The **date-valued** form stays refused (its own row above), and the navigated form is untouched |
| Whole-period qualifier, the **leeg/gevuld** tests | specified (ch. 3.9), **executed by no executor** — all three refuse them by name. The two OCaml executors refuse every whole-period leeg/gevuld test in the same words. The Drools loader refuses the NEGATED test as `Predicaat.negatie`, because the threshold it takes is an open norm point (ch. 1.2), and since 2026-09-23 the UNNEGATED test as `Predicaat.periode`, because `ref` executes neither fold, so no second executor could check its answer. Four red cases hold the refusals and close the square `kind` × `negated`: `corpus/rood/leegnegatie-heel-tijdvak` and `corpus/rood/gevuldnegatie-heel-tijdvak` (negated), `corpus/rood/leegtest-heel-tijdvak` and `corpus/rood/gevuldtest-heel-tijdvak` (unnegated). Gate 8 pins the OCaml sentence in both OCaml executors and lists each case as a Drools loader EXCLUDE; the backend's own witness test, run in gate 8's build step, asserts key and ground for three of them, and for the negated gevuld test the listed category is the only guard. The threshold clause the leeg test needs (touched-days rather than calendar days) is stated in ch. 3.9 and implemented in the Drools runtime, which no document reaches while the loader refuses both forms; the sibling that landed above deliberately did not take it along, because a clause with no witness on this side would be a green subset |
| **Quantifier fold** over a linked instance set (ch. 3.9a) | **specified and executed** — `all` and `none` in all three executors, over a WHOLE-PERIOD matrix (witness `corpus/golden/jutterswet-ploegpremie`) and, since 2026-09-16, over a NON-temporal one (witness `corpus/golden/jutterswet-schouwploeg`: five cases carrying six verdict rows — two ploegen share one of them, which is what pins the RANGE — both quantifiers, two matrix shapes and a negated matrix). The executor asymmetry this row used to record is **gone**: the ASP backend REIFIES the matrix into a helper predicate and negates that one atom (negation-as-failure over a reified conjunction is an ordinary ASP body, where the complement of an arbitrary conjunction of atoms is not), and both Drools branches ask the matrix through their ordinary per-subject condition lowering with the element as subject — the exec branch collecting the contributors, the DRL-text branch denying the failure witness with a `not` pattern. What stays bounded is which matrix FORMS each executor's condition translation carries: a form outside it is refused by name, per form, never silently. **All five quantifiers are executed since 2026-09-16**: the counting three — the named extension point this row used to record — are built in all three executors over BOTH matrix routes, each of them reading the SIZE of the satisfier set where `all` and `none` read a witness (the ASP backend a `#count` with the range join INSIDE the aggregate, the Drools exec branch a counting twin of the ch. 3.9 fold helpers and a count over the collected contributors, the DRL-text branch a count accumulate over the element pattern). Witnesses `corpus/golden/jutterswet-reddingsvloot` (the §8.2 matrix, seven verdict rows), `corpus/golden/jutterswet-vuurtoren` (a non-temporal one, six) and `corpus/golden/jutterswet-sleepvloot` (the NIET prefix, which under a threshold lands on the COUNT because there is no quantifier to flip into — three rows, four of whose cells invert if `count(P)` is read for `count(¬P)`). What is left on the quantifier field is `the`, refused by name by WF-108 (a); all matrix routes of this node are executed by all four executors since 2026-09-18; what stays bounded is which matrix FORMS each executor's condition translation carries, per form and by name. Since 2026-09-19 the node also has a SOURCE language: `corpus/golden/jutterswet-vuurtoren-flogic` is the F-logic-emitted twin of the hand-written `jutterswet-vuurtoren`, over the same five case files byte for byte, played by all four executors |
| **Day-kind duration forms** (§6.10 count, §6.11 step) over day-precision operands | **specified and executed, ch. 3.8** — ref and clingo since 2026-09-19, the Drools backend playing the same values (golden `jutterswet-bergingsklok`; red case `dagsoort-onvervulbare-wandeling`). The reach fence and the backward step are DELTAS: the source gives neither, and the source's N = 0 sentence contradicts its own "always a day of the kind" sentence, which ch. 3.8 resolves in favour of the zero rule |
| **`datum-tijd ± N <dagsoort>`** | **specified and executed, ch. 3.8** (maintainer decision 2026-09-19): the day kind is judged on the civil day part and the original clock time rides through unchanged; golden `jutterswet-getijklok` closes the square. The DELTA is the rule itself — the source admits datum-tijd operands and settles nothing about the time of day |
| **`duration` (tijdsduur) in a day kind over datum-tijd operands** | **refused by name** in both OCaml executors, no corpus case (maintainer decision 2026-09-19, split from the row above): counting whole days of a kind between two INSTANTS needs a rule for the part-day at each end, and none is fixed. OPEN — it is the partial-day/boundary-instant question, not the day-kind question. The Drools backend counts over the bare date parts, a reading this spec has not adopted |
| The day-kind **predicate as a rule condition** (§8.1.5, "… is een werkdag") | refused by name in both OCaml executors (ch. 5.4's framing: a day kind is consulted as a stratum-0 classifier, never as a derived read inside the rule fixpoint). It IS evaluated inside a day-kind condition, where the nested reference is the §9.8 composition — that arm is executed, **on the bound day only** |
| The §9.8 composition **on a date other than the bound day** | **refused by name in all three executors** since 2026-09-19 (red case `dagsoortverwijzing-andere-dag`); the narrow route, taken because ref and the Drools compiler answered it while the ASP backend refused. OPEN — the carrying reading needs a source for what the test MEANS inside a definition about an arbitrary day |
| Gap-vs-absent timeline observability (temporal total: 0 vs leeg) | mirrored residual; stays OPEN here — the ch. 1.4 freeze covers the scalar discipline, not this temporal residual |
| Sorted/non-overlapping input invariant | delta candidate (WF rule, data plane), ch. 3.1 |
| Non-terminating pro-rata stored at 16 digits | v0 residual (normative: exact fraction), ch. 3.7 |

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
declared decimals (identity when no rounding is declared). *v0 residual
(same class as ch. 2.1/3.7):* v0 computes the share divisions at 16
significant digits; the exact fractions above are normative, and
backends must agree on the corpus after the floor and the write
boundary.

- **Equal shares**: one group, all recipients, unweighted; no cap
  (ill-formed with one, ch. 4.5). `share_i = A / n`.
- **Proportional (naar rato)**: one group, the recipients whose weight is
  **filled** — an empty-weight recipient is excluded and receives no
  share (it stays leeg, never 0), and the exclusion surfaces as a §9.5
  error (ch. 5.6); when the eligible weights sum to 0 there is no
  distribution at all (nothing written, no division by zero).
- **Ordered (op volgorde)**: groups are the equal-key classes of the
  ordering attribute, ascending or descending as declared, an **empty key
  sorting last** (no error — mirrored); each group divides the pot *as it
  stands at that group's entry* (ties are simultaneous, not sequentially
  depleted), unweighted or — with the declared tie-break weight —
  weighted. In the weighted tie-break, a **negative weight counts as 0**
  and an **empty weight counts as 0** (the member is assigned a real 0,
  plus the §9.5 error of ch. 4.3); a tie group whose weights sum to 0
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
| empty source amount | **ordered:** no distribution (mirrors the §9.7.6 discipline). **equal/proportional:** v0 has no guard here — a mirror **hole** (the implementation faults). **Delta (decided, v1):** "empty pot ⇒ no distribution" is uniform across methods; the rule-layer artifacts land with the v1 schema |
| weight empty — proportional | excluded from the split (share stays leeg) + §9.5 error; the others still receive |
| weight empty — ordered tie-break | assigned a real **0** + §9.5 error (ch. 4.2) |
| all eligible weights 0 — proportional | no distribution (nothing written) |
| all weights 0 in an ordered tie group | every member assigned **0** (ch. 4.2) |
| a negative weight — ordered tie-break | counts as 0: claims nothing, cannot inflate others beyond the pot |
| a negative weight — proportional | **v0 hole**: unclamped — it enters the weight sum and the product, so a negative share and over-distribution past the pot are possible. **Decided (v1):** the ordered method's clamp is adopted uniformly — a negative weight counts as 0 + §9.5 error; the ch. 4.2 recurrence deliberately does not bless the v0 behaviour |
| cap empty / negative | uncapped + §9.5 error (never a negative share) |
| empty ordering key | sorts last, no error |
| pot exhausted before a group | that group's shares are 0 (real 0, not leeg) |

### 4.5 Well-formedness and refusals

WF-024 (no `COMPLEX` — the v0 placeholder for unrecognised forms is not
admissible IR), WF-025 (field validity per method: ratio iff weighted
split; cap only with a ratio — v0 demotes a capped equal-shares to
COMPLEX, strict reading of §9.7.3; ordering key orderable), WF-026 (at
most one allocation per written target). *v0 mirror note on WF-026:* the
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
- Edge rows of ch. 4.4, as observable outputs: a source with **no
  recipients** writes nothing (also no rest); ordered with tie weights
  {3, −1} over 1200 in one group → the negative weight counts as 0 →
  **1200, 0**; an ordered tie group with weights {0, 0} over 1200 →
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
well-formedness — with the deliberate exception of creation recursion
(ch. 5.5). The mirrored **exceptions** to
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

Characteristics are **monotone** within one evaluation: default false,
only ever set true (the temporal analogue: ch. 3.11's no-complement
rule). A condition that reads a **negated** characteristic is therefore
sound only when evaluated *after* every producer of that characteristic
— and the producers' own input chains — have quiesced: a negation
consumer that fires in the transient window derives from "not yet true",
and its write persists. The evaluation model requires: **negated reads
of derived characteristics evaluate at the fixpoint of the positive
part.** *v0 mirror note:* v0 realises exactly one such boundary (it
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
bijdrageplichtig true, and the monotone write persists — {waar, waar},
wrong. With inkomen = 200: **vrijgesteld onwaar, bijdrageplichtig
waar** (the negation legitimately holds at the fixpoint).

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
order in which the rules are written. Read off the running fact base alone,
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

Two initialisations on one target are ill-formed (`IR-SPEC.md` WF-079, its
initialisation arm), conditional or not: where both apply nothing ranks
them, and the guard would let the first to fire decide.

### 5.5 Creation in the loop

An object creation must be a function of the **fact base**, not of the
fire count: re-evaluating it against the same facts may not mint a
duplicate. The idempotence contract (v0 realises it with two guards —
a linked-instance check where the created object references its
subject, else a per-(rule, subject) creation marker): **one creation per
rule per subject** unless the fact base itself changed the question.
Executed: for each subject whose condition holds, a
deterministic instance of the created type is minted, the member
initialisations apply (values evaluated on the subject; a characteristic
initialisation's boolean-typed value sets the mark when it evaluates
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
witness `corpus/rood/ambiguous-backlink`). And a to-one connecting
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
runtime's: a bounded fire budget turns a non-terminating set into a
**fault**, never a silent truncation (the reference interpreter's pass
budget; the ASP backend's generation-depth bound, whose exhaustion is
a named `viol/2` witness → UNSAT).

Worked example (absolute; the anchor `jutterswet-wachtersketen`
verbatim): the chain "a wachter of rank n recruits an opvolger of rank
n + 1, while n < 5", linked over `opvolging` via the explicit
`connectingRole {opvolging, opvolger}`. From rank 1 the chain
terminates at rank 5 — measured: the direct-successor fold on the
labelled start reads **2** (from rank 4: **5**), the rank-detector
consistency (rank < 5) turns the case **inconsistent** exactly because
generation five exists, and an ineligible start (rank 7) recruits
nothing (fold **0**). The runaway variant (condition rank ≥ 1) is the
red witness `corpus/rood/runaway-recursion`: both executors fault
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
  ch. 4.3/4.4, division by empty (dividend filled), a negative
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
negatief `wf053-zelfverwijzing-vreemde-rol`, removed at L2 with the
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
   backend joins `rol/3` from the subject over that role; a
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
   count. In the ASP reading the positive form is a `rol/3` join over the
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
(`corpus/golden/near-side-rolcheck-to-many`, a red case until L2), and the
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

Worked example — a to-many other role (the `near-side-rolcheck-to-many`
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
read are exactly the members whose faults do not happen: a vacuously true
`atMost` over a member that would divide by zero **answers**, and does
not fault. The wire admits all three shapes (WF-041 constrains only
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

Row 1 is the clause: without it, member 2 divides by zero and the whole
evaluation faults instead of answering. Rows 2 and 3 are the control —
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
observable only as a fault that does *not* happen, so a witness that
pins verdicts alone leaves the row free to move. Each row below is
therefore pinned by a case in which the member after the stop *would*
fault — measured against a mutant that changes exactly that row:

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
| triggers tested *before* the member | `jutterswet-seinwacht` (`minsteteken`): a check made afterwards reads the faulting member 1 |

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
`bron.rgs`, 12 `bron.catala_en`) byte-identically from source, so for
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
| `dateWith` / `dateTimeWith` (datum met / datum-tijd met) | the `arguments` list (WF-037 leaves its arity open) | **visible** | argument order. *ref declines the form; the order is stated, not yet exercised* |
| aggregation (`sum` (som), `total` (totaal), `count` (aantal), `minimum`, `maximum`, `first` (eerste), `last` (laatste), `filledCount`) | the contributors — the listed `arguments`, or the instance fold's members each gated by the `filter` — then the `emptyFallback` | **visible** | the contributors in wire order (an instance fold: the filter per member, members in fold order), then the `emptyFallback` (WF-037 confines it to `sum`) |
| aggregation over a `dimensionSelection` (the third contributor source; WF-037 admits it on `sum`, `minimum`, `maximum`) | the selected cells, then the `emptyFallback` | **visible** | the cells in the selection's own label order, then the `emptyFallback`. *ref declines the form; the order is stated, not yet exercised* |
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
`som-fallback-always-evaluated` (gate 8), which stops failing the moment
an executor makes the stand-in lazy.

Rule- and form-level order — which rule fires first, which member of a
defaults group is tried — is **not** this clause's subject: ch. 5.2 makes
the fixpoint independent of it, and ch. 7.1 fixes the defaults chain by
priority rather than by evaluation.

**Worked example — `Clamp` (absolute; the `begrensd-reads-bounds-in-order`
red case verbatim, gate 8).** A `Jutter` with `teller` = 1, `noemer` = 0
and `grens` **absent**:

```
begrenswaarde := Let grenswaarde = zijn grens
                 in (100 met een minimum van zijn teller gedeeld door zijn noemer
                         en een maximum van de grenswaarde)
```

`base` = 100 evaluates first. `minimum` = 1 ÷ 0 evaluates second and
**faults** — division by a numeric zero (ch. 1.2). `maximum` is never
reached. Pinned outcome: the rule **fails observably** with *"division
by numeric zero"*.

Under the reverse order the `maximum` would run first, its read of
`grenswaarde` would raise the ch. 10.6 barrier, the whole `Let` would be
leeg, the division would never happen, and the same document would
quietly write nothing. The two orders do not differ in a value; one has
an answer and the other does not.

**Worked example — tijdsduur (absolute; the
`tijdsduur-reads-van-before-tot` red case verbatim, gate 8).** A `Vaart`
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
  contributors are filled. `som-fallback-always-evaluated` is the case
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
| `Clamp` `minimum` before `maximum` | `begrensd-reads-bounds-in-order` (gate 8; ref by which fault, clingo by which bound is refused first) |
| `duration` (tijdsduur) `van` before `tot` | `tijdsduur-reads-van-before-tot` (gate 8; the ref leg — clingo carries the form and fails, but does not order it) |
| composite members, wire order + short circuit | ch. 5.10's own witness table |
| `Comparison` `left` before `right` | `ref/` `WV-5.11-vergelijking-…` (pair) |
| aggregation contributors, in wire order | `ref/` `WV-5.11-som-bijdrager…` (pair) |
| the **instance** fold: the filter per member, members in fold order | `ref/` `WV-5.11-instantiefold-…` and `WV-5.11-instantiesom-…` (two pairs — the `Instances` walk a `count` (aantal) takes and the `Values` walk a set-read `sum` (som) takes) |
| aggregation contributors **before** the `emptyFallback` | `ref/` `WV-5.11-som-…-leegwaarde` (pair) |
| the `emptyFallback` evaluates unconditionally | `som-fallback-always-evaluated` (gate 8) + `ref/` `WV-5.11-som-leegwaarde-onvoorwaardelijk` |
| `concatenation` (concatenatie) and `TextComposition` part order | `ref/` `WV-5.11-concat-…`, `WV-5.11-tekstreeks-…` (pairs) |
| `ListLiteral` element order | `ref/` `WV-5.11-listliteral-…` (pair) |
| `StructLiteral` field order | `ref/` `WV-5.11-structliteral-…` (pair) |
| `Invoke` argument order | `ref/` `WV-5.11-invoke-…` (pair) |
| `Fold` `list` before `init` | `ref/` `WV-5.11-fold-…` (pair) |
| `MapList` element order | `ref/` `WV-5.11-maplist-…` (pair) |
| `FilterList` element order | `ref/` `WV-5.11-filterlist-…` (pair) |
| `dateWith` / `dateTimeWith` (datum met / datum-tijd met) | **none** — the reference interpreter declines the form, so there is nothing to run. The order is stated for the consumer that carries it first, and lands with it |
| `TimeWindow` `from` before `to` | **none yet** — the form is carried since 2026-09-20 (ch. 3.10, golden `jutterswet-premievenster`), but the two bound shapes WF-111 admits are a literal and an own scalar attribute, and neither can fault. A witness lands with the general date expression, which is the first bound that can and an OPEN wire decision (ch. 3.12) |

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
  traverses it and by every fold or allocation that names it in a role
  binding (from either end; the link is written on both).

Each rule form reads and writes nodes:

| Form | Writes | Reads |
|---|---|---|
| assignment / initialisation | its target attribute | every attribute and characteristic its value expression and condition reference — a navigated reference reads the referenced type's node; population(element type) per instance fold |
| characteristic attribution | its target characteristic | idem |
| allocation | target attribute; declared remainder attribute | source amount; ratio, maximum and ordering attributes; condition reads; population(target type) |
| consistency / uniqueness | channel(own name) | assertion operands / key attributes; a union reads every source's key attributes; a scoped uniqueness also reads relation(scope hop), named by its `scope` binding (`scopeRol` until L4; since L2 the only spelling of the scope, B8; until L2 a scope without a binding was derived from the written scope field) |
| object creation | the attributes and characteristics its `initialisations` set on the created type; population(created type); relation(the uniquely-derivable connecting fact type — the back-link, ch. 5.5) | condition and toekenning-value reads on the anchor; population(subject type) — iterating the subjects is a read: it orders creation-after-creation, and a self-typed creation becomes a WF-045 cycle instead of a diverging fixpoint or an unbounded grounding |
| relation creation | population(target-role filler type); relation(created fact type) | condition reads on the anchor; relation(each hop of the join-chain traversal, `IR-SPEC.md` ch. 3.4) |

Edges run producer → consumer — from every form that writes a node to
every form that reads it — in three kinds:

- **positive**, the default: reads by attribute-writing forms
  (assignment, initialisation, allocation), and positive characteristic
  reads anywhere. Attribute writes are revocable — idempotent and
  re-evaluated under freshness (ch. 5.1) — so a transient self-corrects
  within a stratum, and a positive characteristic read is monotone-safe
  (it can only still become true).
- **stratum-strict**: any non-characteristic read (attribute values,
  comparisons; the leeg/gevuld predicates read negatively, below) by a
  form whose write is *irrevocable* — a characteristic attribution
  (monotone, ch. 5.3), a consistency or uniqueness (channel facts are
  never retracted, ch. 5.6), or a creation. Such a form must evaluate
  strictly above the producers of what it reads: a transient it acted
  on would persist.
  Within a WF-045-admissible recursive-creation cycle the strictness is
  discharged by the one-generation-per-instance progression (ch. 5.5)
  rather than a stratum boundary.
- **negative**: a negated characteristic read, a characteristic read
  under a negation-like quantifier (*geen*, *ten hoogste*, *precies* —
  their truth can be lost by a later monotone write), every read under a
  leeg/gevuld predicate in either polarity, every read in the range of a
  quantifier fold (ch. 3.9a) under *alle*, *geen*, *ten hoogste* or
  *precies* and in its matrix under *geen*, *ten hoogste* or *precies*
  (both since 2026-09-23; the maintainer decisions are quoted in
  `IR-SPEC.md` ch. 5.2), or a channel test (regelversie, ch. 5.6). Once
  negative, conservatively negative — a double negation does not cancel.
  Negative edges are stratum-strict *and* forbidden on any cycle (WF-046).

Parameters and input-only nodes (nodes no form writes) contribute no
edges.

*Scope narrowing, recorded:* relation membership read by a *condition*
(the role check, `RoleMembership`) is treated as input. Only reads that
name the relation in a role binding, and the creation's own hops, carry
relation-node edges (the creation-after-creation ordering demanded
this); since L2 every allocation, instance fold and quantifier fold names
its relation so (B12), and an allocation and an instance fold read the
population proxy beside it. Day-kind definitions read only input dates
and contribute no edges. *History until L2:* a bindingless allocation
kept the population proxy alone; a bindingless fold read the population
proxy and, beside it, the relation derived best-effort — for an instance
fold the fact type of the role its source named, for a quantifier fold
(from 2026-09-23) the one connecting relation WF-048 demanded — and a
relation that could not be derived contributed no edge while the
verifier reported the document; a bean-field-only v0 creation record
carried no symbolic hops and stayed on the bare population write,
recorded with WF-028's uncheckable clause (since L2 the symbolic block
is required, B7).

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

*Recorded residuals (ch. 7 territory, mirrored):* a condition-guarded
attribute write whose condition later turns false keeps its written
value — the mirror has no retraction — so conditions on
attribute-writing forms read positively here, except a leeg/gevuld
predicate, which reads negatively in every position since 2026-09-23;
and §9.5 deposits from arithmetic transients within a stratum persist
(idempotent inserts, never retracted). Both await the default model of
ch. 7.

*Recorded residual — conditions that hold on absence (measured
2026-09-23, open).* The leeg/gevuld predicate, the range of a quantifier
fold under *alle*, *geen*, *ten hoogste* and *precies*, and its matrix
under *geen*, *ten hoogste* and *precies* are not the only conditions that
are true while an operand is absent and can turn false once it arrives.
The others carry no `negated` field and keep their positive or
stratum-strict reading, and on a WF-045-admissible
creation cycle that reading is unsound in the way the leeg/gevuld decision
records. Measured on one shape: a recursive creation "werf de opvolger"
whose condition is "de rang van de Wachter is kleiner dan 5 en X", with X
reading the successor that the creation itself mints, and one input
Wachter W1 of rang 1. Every document in the table is verifier-clean. In
every row the reference interpreter mints W1 to W5, each creation justified
by an X that the next generation falsifies, and the ASP backend finds no
stable model:

| X | why it holds on absence |
|---|---|
| de rang van de opvolger is ongelijk aan 2 | exactly one leeg operand is waar (ch. 1.2) |
| de rang van de opvolger is gelijk aan het merk (merk leeg) | two leeg numeric operands are equal (ch. 1.2) |
| het aantal opvolgers is gelijk aan 0 | a count over nothing is 0 |
| het aantal gevulde rangen van alle opvolgers is gelijk aan 0 | idem (ch. 3.4c) |
| geen van: de rang van de opvolger is groter dan 0 (composite) | a leeg operand makes the member false |
| de rang van de opvolger plus 0 is kleiner dan 1 | plus coalesces leeg to 0 (ch. 1.2) |

The controls are the same shape with X = "de rang van de opvolger is leeg"
and with X a quantifier fold over the successors under *alle* or *geen*
("alle opvolgers hebben een rang groter dan 5", "geen van de opvolgers heeft
een rang groter dan 0"): the reference interpreter mints the same chain,
and WF-046 refuses those documents since 2026-09-23 (`IR-SPEC.md` ch. 5.2).

The same classes also leave the reference interpreter order-dependent OFF
a cycle, and that is the older residual above ("conditions on
attribute-writing forms read positively here"). Measured on a Houder with
one Ding whose a (5) another rule derives: with the reader listed first,
"b = 1 indien de a van het ding ongelijk is aan 5" and "b = 1 indien het
aantal dingen waarvan de a groter is dan 0 gelijk is aan 0" each give b = 1
in the reference interpreter, which acted on the transient leeg and keeps
its write, while the ASP backend answers leeg; with the writer first both
answer leeg. The leeg/gevuld test is out of that residual because its reads
are negative edges, which order the reader above the writer.

*Recorded residual — a value that grows (measured 2026-09-23, open).* The
discharge of strictness on an admissible creation cycle fails for any read
that can lose its truth, and absence is only one way to lose it. A
comparison that a growing value falsifies is another, inside a fold's
monotone part and outside any fold. In the model of the golden
`jutterswet-opvolgerquorum` (merk the number of a Wachter's successors),
with its case 4 as input, the document stays verifier-clean when the `all`
matrix reads "merk kleiner dan 2" or "merk ongelijk aan 2", and the four
executors split: the reference interpreter and the Drools exec branch
answer merk(W1) = 2, the ASP backend and the DRL-text branch 1. An `atLeast`
fold with "merk kleiner dan 2", given a ploeglid under W3 so that W3 mints,
splits the reference interpreter and both Drools branches (2) from the ASP
backend (1). Without any fold, "het merk van de ploegleider van de Wachter
is ongelijk aan 2" was already verifier-clean on the main line before this
card, with merk(W3) = 2 in the reference interpreter and both Drools
branches and 1 in the ASP backend. Whether "monotone in the matrix" should
demand a matrix that is monotone itself is a norm question the
quantifier-fold decision leaves open.

The two-form shape splits silently rather than loudly. With "het merk is
ongelijk aan 1" in the creation and a rule writing merk = 1 when "de rang
van de opvolger is ongelijk aan 2", the reference interpreter answers
merk(W1) = 1, the ASP backend has two stable models, and its runner picks
one with merk(W1) leeg. A related gap sits in the role-check narrowing
above, with no recursion needed: a creation of a Post under "hij is geen
bemanner" reads, as input, the very link it writes. The verifier sees no
edge, the reference interpreter mints once, and the ASP backend finds no
stable model. None of these forms occurs in the corpus. Whether each of
them becomes a negative edge is a norm question that the leeg/gevuld
decision leaves open.

### 6.2 Acyclicity (WF-045)

**WF-045.** The dependency graph MUST be acyclic after removing every
creation form — object creation, or relation creation (ch. 5.5's
idempotent, guard-free member) — that is a member of a recursive rule
group (`IR-SPEC.md` ch. 3.9): a dependency cycle is admissible **only**
when it passes through such a form. That is ch. 5.5's one controlled
exception — recursive creation advances exactly one generation per
instance; WF-033 guards the author's termination obligation and the
runtime's bounded budget makes divergence a fault — promoted from the
ch. 5.2 delta to well-formedness. With WF-045 (and the single-writer
allocation discipline of WF-026) the fixpoint of the positive part is
unique: rule order, activation order and insertion order do not matter
(ch. 5.2).

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
1, …, the forms of stratum ≤ k evaluate to their fixpoint before any
form of stratum k + 1 fires. A negated characteristic, and since
2026-09-23 a leeg/gevuld predicate and the negative parts of a quantifier
fold (its range under every quantifier but *ten minste*, its matrix under
*geen*, *ten hoogste* and *precies*), is then read only after every
producer — and the producers' own input chains — have quiesced, so a
monotone write cannot contradict it later; a channel
test reads a complete channel for the named rule. v0's single boundary
is the two-stratum special case; nested and chained negation get as
many strata as the chain is deep.

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
- Ill-formed: `a ← b + 1` and `b ← a + 1` on one object type is a
  WF-045 cycle through no creation (negatief case
  `wf045-cyclic-attribute-dependency`). A recursive creation whose
  condition negates a characteristic derived from its own output is a
  WF-045-admissible cycle carrying a negative edge — a WF-046
  violation (negatief case `wf046-negated-read-in-recursive-cycle`).
  So is a recursive creation guarded by a leeg/gevuld test on what its
  own cycle produces, in either polarity (`IR-SPEC.md` ch. 5.2): the R1
  shape (the creation mints a successor while "het merk is leeg", and
  the rule that writes merk = 1 reads the successor's rang under
  "gevuld"), the R3 shape spelled with `negated: true` (both tests "niet
  gevuld"), a presence-true test alone ("het merk is niet leeg") and
  the test as the matrix of an `all` fold over the minted successors —
  negatief cases `wf046-leegtest-recursieve-creatie`,
  `wf046-leegtest-negatie-recursieve-creatie`,
  `wf046-leegtest-aanwezigheid-recursieve-creatie` and
  `wf046-leegtest-kwantorvouw-recursieve-creatie`. `ref/` re-runs the
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
  negatief cases
  `wf046-kwantorvouw-alle-bereik`, `wf046-kwantorvouw-geen-vergelijking`,
  `wf046-kwantorvouw-geen-matrix`, `wf046-kwantorvouw-bereik-ander-type`,
  `wf046-kwantorvouw-tenhoogste-bereik`, `wf046-kwantorvouw-precies-bereik`,
  `wf046-kwantorvouw-tenhoogste-matrix`, `wf046-kwantorvouw-precies-matrix`
  and `wf046-kwantorvouw-onder-geen`. Before the rule, where the range
  or an enclosing negation closed the cycle, the reference interpreter
  minted on a condition that the mint itself falsified (a chain, one
  successor, or — where the creation mints a Post for a Wachter — one
  Post) and the ASP reading has no stable model; where only the matrix
  closed it, the two executors split without either failing (the
  reference interpreter minted for W1, the ASP backend did not).
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
  available to expressions solely as the execution-date context value —
  a `ContextValue` named `evaluationDate` (`Rekendatum` until L5, the v0
  `Constante` until L4; day precision; its sibling
  `evaluationYear`, a whole number, is not yet exercised by any corpus case
  and every executor refuses it by name with one sentence — the reference
  interpreter and the ASP backend at evaluation, the Drools backend before
  lowering — recorded in `IR-SPEC.md` ch. 4.1).

**The bound day is not a session value.** Until L4 `IR-SPEC.md` ch. 4.1
admitted a third context-value name, `de dag`; since L4 it is the node
`BoundDay`, and this chapter does **not** own it: it denotes the BOUND DAY
of a day-kind definition, and its runtime meaning is stated where that
binding is made — ch. 3.8, the day-kind duration forms. Two consequences
follow, and both are checked. Outside a day-kind evaluation the bound day
denotes nothing and every executor
faults by name (red case `contextwaarde-de-dag-buiten-dagsoort`); and the
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
translation time, before emitting a program).

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
form, WF-050/051 with verifier checks and negatief cases). The art. 4
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
standing norm executes on scalar targets through both executors
(temporal targets stay fenced — recorded, scalar-first). Below-root
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
    (ch. 5.3's monotone write regime is untouched — the group decides
    once, then at most one attribution happens).

  At most one default decides, so a single group can never conflict — a
  property of the semantics, not a claim about authors.
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
  the §9.5 channel. This scopes the ch. 6.1 residual (b): transient
  deposits keep persisting in ordinary evaluation (the v0 mirror);
  prioritized defaults are the one sanctioned speculative context.
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
  prioritized-defaults node. This is precisely the debt ch. 6.1's residual note ("conditions on
  attribute-writing forms read positively here … await the default
  model of ch. 7") assigns to this chapter: the defaults group is where a
  condition's failure becomes observable displacement, so the group's
  edges carry the negativity. WF-045/046's *principles* — no negative
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
| speculative default evaluation (ch. 7.1) | channel deposits of a default's arithmetic are **guarded derivations** — the `inconsistent/2` rule for a default's transient carries `decides_n` in its body (ASP has no retraction; the guard is the speculation) | evaluate the deciding default only | v0's persist-always transients (the ch. 6.1 residual (b), here scoped) |
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

### 7.7 The v0 desugaring

How the existing v0 rule forms read as prioritized defaults — the lowering
that retires ch. 5.4's guard as mechanism (ch. 7.3) and lets a backend
serve the old forms through the new machinery:

| v0 form(s) on one (type, attribute) target | Defaults reading |
|---|---|
| **exactly one** unconditional, version-free Initialisatie | the ch. 7.3 initialisation default (lowest) |
| plus at most one version-free computing rule (Gelijkstelling/Berekening) | plus one default above it: justification = its condition (an unconditional rule gets the vacuously true empty `all`), consequence = its value |

The initialisation is what the desugaring exists for — and what keeps
the two readings extensionally equal on the confluent domain. A **bare
computation without one stays outside the desugaring** — but no longer
native-v0: the ch. 7.8 standing norm (decided 2026-08-20) gives it the
defaults reading directly — a computed leeg leaves the input standing —
with the v0 erase-on-leeg retired as a tracked delta. What this
chapter could not do silently, ch. 7.8 does openly. (The v0 erase
itself had no absence encoding on the ASP native path — the recorded
backend limitation that, with the ch. 6d fence, kept the corner free
of cases and the change cheap; see `backends/clingo/README.md`.)

Outside the desugaring's domain the v0 forms are left as they are; a
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
could pin (a fault is not an expectation). **Closed with ch. 7.8
(2026-08-20):** `ref/` now carries the defaults reading natively on the
desugaring domain too — the leeg-evaluating rule writes nothing (the
shared `desugared_targets` predicate), the init or input decides, and
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
declared defaults:

**The standing norm.** A **version-free scalar computing rule
(Gelijkstelling/Berekening) that is its target's only writer** — no
second rule, no initialisation beside it (that pair is the ch. 7.7
desugaring's domain), no defaults group, no allocation, no creation-time
write — decides **like a default**: its justification is its condition,
and it decides only when its value evaluates **gevuld**. A **leeg
evaluation writes nothing**: the target keeps what the fact base
holds — the case-supplied input value, or leeg. The v0 mirror wrote
the computed leeg over the input (ch. 1.2g); that erase is retired on
this domain as a **tracked delta** (`CONSTRUCTS.md` rows 8/18) —
decided openly where ch. 7.7 records it as the reason the bare form
could not be desugared silently. Channel deposits of the evaluation
persist as in any ordinary evaluation — prioritized defaults remain the
one sanctioned speculative context (ch. 7.1). Together with ch. 7.7, every
scalar single-writer form now carries the defaults reading:
initialisation alone and initialisation-beside-computation through the
desugaring, the bare computation through this norm.

Worked examples (absolute; the anchor `jutterswet-waardestelling`
verbatim, all four through both executors): one conditional rule "de
geschatte waarde = de taxatiewaarde, indien de Vondst gemeld is" over
a case input of 40 — deciding rule computing 50 → **50** (computation
displaces input); condition false → **40** (the flank: input stands);
condition holds, taxatiewaarde leeg → **40** (the fall-through: a
computed leeg never displaces an input value — the delta pin); no
input, computing 50 → **50**. The two standing outcomes satisfy a
do-nothing executor by construction — the standing *is* what they
test — so they are reason-carrying gate-7 survivors. The domain
predicate is shared between the executors (one definition), the ASP
entry is the defaults group's `inputval/3` with an effectiveness atom
carrying condition **and** value bindings (a condition-only atom would encode
the v0 erase instead — the atom's body is the semantic choice).
**The timeline arm (decided by the maintainer 2026-08-21, the
temporal-standing card's numbered question — whole-timeline over
per-bucket):** the norm extends to a **standing temporal target** — a
temporal attribute carried by exactly one version-free computing rule
(in practice the ch. 3.7 pro-rata fold or the ch. 3.8 day
count) as its only writer. The rule
decides only when its result timeline is **gevuld** (at least one
segment); an empty result is the temporal leeg (empty == never-written,
ch. 3.1) and **writes nothing**, so a case-supplied input timeline
stands — **whole**: the defaults reading is per target, never per bucket
(a default decides once, and ch. 3.11's timeline write replaces the whole
attribute timeline), so input and computed segments never interleave. A
deciding fold displaces the input timeline entirely (worked, absolute —
the anchor `jutterswet-borgdeel` verbatim: a non-deciding fold over
input [2024-03-01, 2024-05-01) = 7 leaves exactly that timeline, a
reason-carrying gate-7 survivor; a fold deciding Jan–Feb 12 over an
input March segment yields Jan–Feb 12 and nothing else). **Multi-writer
targets stay fenced** on the ASP side (v0 meaning: agenda order), as do
versioned targets (selection is temporal, ch. 7.4 — never defeasible).

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
group runs on instances of exactly that type.** The extensional
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
`corpus/negatief/wf079-keten-kale-schrijvers`). A rule beside a group the
level declares **itself** is likewise not this chapter's business: that is
WF-050's single-writer lift, refused at the document. An allocation is never
folded — this section folds ordinary rules only — so WF-026 speaks through
the chain without a gate: an allocation over a type and one over an ancestor
both write the type's instances whatever defaults groups the chain declares,
and so do an allocation and a defaults group on two levels of one chain,
which WF-050's lift refuses the same way (`IR-SPEC.md` WF-026 and WF-050;
witnesses `corpus/negatief/wf026-keten-twee-toewijzingen` and
`corpus/negatief/wf050-keten-toewijzing-naast-groep`).

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
in the Drools backend's own suite (`OverervingWeigeringWitnessTest`), each
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

## 9. Sum types and match

*A v1 kernel extension (CONSTRUCTS row 36; design and ratified decisions:
`spec/proposals/PHASE5B-KERNEL.md` §2). No v0 counterpart exists — the
toolchain has no source construct that produces these forms — so this
chapter mirrors nothing; it is normative from first writing. Node forms
and well-formedness: IR-SPEC ch. 2.4 (`SumType`, the keuzetype family
and its WF-087 normalisation) and ch. 4.7 (`Construct`, `Match`,
`BinderRef`), WF-060..064. `Match` is the one node of this chapter that
also serves the family's other wire variant, the enumeration — its
evaluation is ch. 9.2; everything else here is sum-only.*

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
- **Timelines — recorded flank, not yet carried.** The value model
  admits a sum-typed temporal attribute and the case format can write
  one; no anchor exercises it, and the ASP case emission does not yet
  carry constructor-valued segments (it faults loudly). Until a case
  wants one, temporal sum carriage is a recorded OPEN in the ASP
  inventory — never a silent divergence. Timeline *arithmetic* and
  temporal totals stay numeric-only regardless (ch. 3.4–3.6), as for
  text and enumeration members.

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
at the fold's boundary (init, source), never in its interior.

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
  ch. 3.5a aggregation-filter discipline per element; a leeg source
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
| i | `Let q = 100 ÷ 0 in 42` | **42**, no fault | clause 3 over a *faulting* value (strict: **faults**) |
| ii | `Let q = 100 ÷ 0 in q` | **fault** *"division by numeric zero"* | the read forces the value; fail-fast, unchanged |
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
nothing on iii.

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
(`VariabeledeelLoweringTest.de_grens_is_het_hele_aggregatieconstruct_ook_zijn_zuivere_waardeposities`).

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
  (`corpus/rood/struct-field-overprecision`,
  `corpus/rood/list-element-overprecision` — ref now matches the ASP
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
