# uchar IR — Runtime semantics

*Status: ch. 1–5 are normative **draft** (v0 mirror with tracked deltas and
OPEN decisions); ch. 6–8 are skeleton. Authority for undocumented behaviour
still lives in `regelspraak-java` (pinned commit `b85dff3`):
`runtime/RegelSpraakRuntime.java` — the single shared implementation both
Drools back-ends call — and the decision records in
`docs/grammar-divergences.md`; that pointer leaves at Phase-1 exit. Every
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
boolean, a date (day or millisecond precision), an enumeration value — or
**empty** (leeg): the distinguished absence of a value, belonging to no
datatype. Empty is not zero, not the empty string, not false; every context
below defines explicitly what it does with an empty operand. (Empty is also
distinct from `Onopgelost`, which marks an *unresolvable construct*, not an
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
| date ± duration | empty *date* propagates; empty *duration* coalesces to 0 | leeg + 5 dagen = leeg; datum + leeg dagen = datum |
| date construction / part extraction | any empty part or date propagates | jaar uit leeg = leeg |
| tijdsduur van A tot B | any empty operand propagates (never 0) | tijdsduur van leeg tot d = leeg |

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
| numeric inequality | true iff **exactly one** empty, else value inequality | leeg ≠ 5 → waar; leeg ≠ leeg → onwaar; 5 ≠ 3 → waar |
| non-numeric equality (text/enum/boolean, §5 Tabel 38) | any empty operand → false | leeg =ₜ leeg → onwaar |
| leeg / gevuld predicates | absence — including a failed to-one navigation — **is** leeg = waar | "de leeftijd van zijn partner is leeg" is waar for a partnerless person |

Note the deliberate asymmetry: numeric equality treats two empties as
equal; non-numeric equality does not. Both are mirrored, and the ch. 1.4
decision keeps them: the equality rules where empty carries meaning are
on its must-preserve list.

**Aggregations**

Empty contributors are skipped, never counted as 0. The result for an
empty or all-empty source differs per aggregation — this is the
§5.8.2/§4.20 split:

| Aggregation | Empty/all-empty source | Worked examples |
|---|---|---|
| som | **empty**, unless the declared fallback clause ("… of 0 als die er niet zijn") substitutes | som over {5, leeg, 3} = 8; som over {leeg, leeg} = leeg; with fallback = 0 |
| totaal | **0**, unconditionally (§4.20) | totaal over {} = 0 |
| aantal | 0 (a count, never empty) | aantal over {} = 0 |
| minimum/maximum, eerste/laatste | **empty**; no fallback form exists | min(5, leeg, 3) = 3; min(leeg, leeg) = leeg |
| dimension folds (§5.8.5) | empty cells skipped; all-empty selection = empty (som may use its fallback) | cells {Q1: 3, Q2: leeg, Q3: 2}: som over Q1–Q3 = 5; som over {Q2} = leeg |

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
computed leeg → no-op. The initialisation form writes only when the target
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

Division by numeric zero: hard fault in v0; decided for v1: it routes
into the §9.5 channel (ch. 1.4 point 4). A rational literal
with denominator zero is rejected at parse time.

### 2.3 Result precision (static typing)

Declared result precision per operator, over bounded operands (`maxDec` =
maximum of the operands' declared decimals):

| Operator | Declared result precision |
|---|---|
| plus, min, verminderd met | maxDec; whole iff both operands whole; left operand's unit |
| maal | **sum** of the operands' decimals (§4.4: 2 dec × 2 dec = 4 dec) |
| gedeeld door | maxDec — a documented **under-report**: an exact quotient has no finite inherent precision (see ch. 2.4) |
| tot de macht | maxDec, not whole |
| percentage van | precision claim dropped (0, not whole); right operand's unit |
| wortel | placeholder claim (0 dec, non-negative, unitless); the effective precision comes from the mandatory rounding |
| absolute waarde | passthrough |
| aantal | whole, non-negative, unitless |
| min/max over listed operands | least operand precision (never over-reports) |

Unbounded targets ("getal", the ch. 2.4 exemption) carry no ceiling.

Examples: 12,34 + 1,2 types at 2 decimals; 1,25 × 1,5 types at 3 decimals
(sum); 10 ÷ 4 types at 0 decimals although its value is 2,5 — the
under-report ch. 2.4 closes by rule.

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
bound. A trailing rounding applies to the **whole** clamped result:
`(X met een minimum van 0) afgerond op 0 naar beneden` rounds the clamped
value. Example: begrensd(150, 0, 100) = 100; begrensd(−5, 0, 100) = 0;
(begrensd(2/3, 0, 1)) afgerond op 2 rekenkundig = 0,67.

### 2.8 Time-proportional part (tijdsevenredig deel)

Per granularity bucket: result = base × days-true/days-in-bucket, as an
exact fraction. Example: base 1200, 90 of 365 days → 1200 × 90/365 =
21600/73 — non-terminating, rounded only by an explicit or target
rounding: afgerond op 2 rekenkundig = **295,89**. Full timeline algebra:
ch. 3.

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
- Segment-wise equality — same `van`, same `tot`, numerically equal or
  both-leeg value — is the change-detection rule for timeline writes (the
  temporal analogue of the scalar rule in ch. 1.2 Assignment; evaluation
  model: ch. 5).

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

### 3.3 Reading a timeline — and the point-read gap

`read(tl, d)` (ch. 3.1) is the primitive every operation below is built
from. But **v0 exposes no point-in-time read to the rule author**: a
time-dependent attribute or parameter in a scalar context is refused (the
"§8.2 list-in-scalar" marker family), and the evaluation date (rekendatum)
only *gates* rules — version validity, the ch. 3.10 period condition — it
never *indexes* a timeline to a value. The only ways a timeline becomes a
scalar are the dedicated collapses: the temporal total (ch. 3.6) and the
whole-period predicate (ch. 3.9).

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
- **Units.** The rate obligation of WF-038 is satisfied *structurally*
  here: the timeline's granularity is the "per time unit", so a plain
  `€`-valued per-month timeline totals to `€` by value × month-count. An
  explicit rate unit (`€/mnd`) is what the *non-temporal* totaal operand
  requires; the temporal collapse must not be misread as requiring it.

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

*v0 mirror note:* the totaal of a multi-period timeline is refused with
the grid (ch. 3.2), never mis-weighted as single-period.

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
  nothing.
- A rate-unit base is restated **first**, by its exact factor
  (`€/jr` base for a per-month target: ÷ 12 exactly), then prorated.
- The fraction is exact and is **never auto-rounded** to the target's
  declared decimals — rounding happens only at an explicit rounding or
  the assignment gate (ch. 2.4). *v0 mirror note (residual, same class as
  ch. 2.1):* v0 stores a non-terminating pro-rata at 16 significant
  digits (DECIMAL64); the exact fraction is normative, the store is the
  v0 approximation; backends must agree on the corpus after rounding.

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
23**, **feb = 13**, two segments.

**tijdsduur van A tot B** counts *whole* declared units between two
dates, signed (negative when B < A); quarters = whole months ÷ 3; any
leeg operand → leeg, never 0 (ch. 1.2). The absolute variant drops the
sign.

**Duration in a day-kind** ("de tijdsduur van A tot B in werkdagen")
counts the days d with `A < d ≤ B` — **start exclusive, end inclusive**
(the "A itself does not count" rule) — that satisfy the day-kind's
defining predicate; signed by operand order; leeg operand → leeg. Worked
examples (absolute; day-kind = 25/26 December): 20-12 tot 31-12 → **2**;
25-12 tot 26-12 → **1** (the 25th is the excluded start); reversed →
**−2**, absolute variant **+2**; equal dates → **0**.

**Date plus N day-kind days** steps one calendar day at a time, counting
only matching days; the result is the Nth matching day. The start date
itself never counts; N = 0 or leeg N returns the start date unchanged
(even when that date is not itself a matching day); negative N walks
backward; a leeg date propagates. An unsatisfiable day-kind definition is
a hard fault (the walk is bounded), never an endless loop. Worked
examples (absolute; day-kind = 25/26 December): 20-12 + 2 → **26-12**;
25-12 + 1 → **26-12**; 26-12-2024 + 1 → **25-12-2025**; 20-12 + 0 →
**20-12**; 27-12 − 1 → **26-12**; 25-12-2024 − 1 → **26-12-2023**.

**Date ± whole calendar units** clamps to the month end: 2024-01-31 + 1
maand = **2024-02-29** (leap), 2025-01-31 + 1 maand = **2025-02-28**; a
quarter is three months: 2024-01-01 + 1 kwartaal = **2024-04-01**. A
fractional count truncates toward zero. (Empty rules: ch. 1.2 — a leeg
date propagates, a leeg duration counts as 0.) One date-builtin pin that
becomes a `ref/` test: eerste paasdag van 2024 = **2024-03-31**.

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
- Over a to-many navigation the fold is conjunctive — every element's
  timeline must pass — and an **empty set passes vacuously**; a failed
  to-one navigation contributes a null timeline, which passes vacuously
  (the ch. 1.2 leeg-navigation discipline, lifted).
- Date-valued whole-period comparisons (eerder/later over a date
  timeline) are refused in v0 — recorded, not specified (ch. 3.12).

Worked examples (absolute; value timeline per year, tested ≥ 10):
value 12 on the whole of 2024 → all 366 days of the touched bucket
satisfy → **waar**. Value 12 on [2024-01-01, 2024-07-01) only → the 2024
bucket is touched for 182 days but has 366 → **onwaar** (a July moment
carries no value); the negated form → waar. Same half-year timeline
under the **leeg** test → touched days must be leeg, but they carry 12 →
**onwaar**.

### 3.10 Period conditions and the §8.4.3 clip

A **period condition** (`TimeWindow`, `IR-SPEC.md` ch. 4.5) restricts a
rule to a date window. Two cases:

**Scalar target — the evaluation-date gate (v0 behaviour, mirrored).**
The rule applies iff the evaluation date lies in the window: `vanaf`
inclusive, `tot` exclusive, "tot en met" inclusive; an absent bound is
open; an unset evaluation date never matches; a leeg bound never matches.
Worked example: window [2024-01-01, 2025-01-01), evaluation date
2024-12-31 → applies; 2025-01-01 → does not; the "tot en met 2024-12-31"
form still applies on 2024-12-31. *v0 mirror note (backend maturity, not
IR scope):* v0's backends lower only literal or own-scalar-attribute
bounds and refuse the rest, and refuse the condition under counting
quantifiers; the IR admits any date expression (WF-042).

**Temporal target — the clip (v1 semantics, delta).** When the rule's
target is time-dependent, gating all-or-nothing on the evaluation date is
the wrong axis: the window speaks about the *timeline*, not about the
evaluation moment. v1 specifies the **clip**: the produced timeline is
intersected with the window,

```
written(tl, window) = tl restricted to [vanaf, tot)
```

and the write proceeds with the clipped timeline. Worked example: the
rule computes {2024: 10, 2025: 12} yearly; window [2025-01-01, open) →
written value {2025: 12}; days in 2024 stay whatever another version or
producer gives them — a gap, absent other writers. **Delta (tracked,
`CONSTRUCTS.md` rows 11/29):** v0 refuses this case outright (the
manifest family "period-comparison clipping, §8.4.3"); v1 supplies the
clip semantics above, and `ref/` implements them first. The analogous
interaction with a rule *version's* validity window is **not** decided
here: a validity window on a temporal write mixes the version-selection
axis with the timeline axis, and belongs to the single-axis decision of
ch. 8 (PLAN §9, decision 2) — OPEN there, recorded in ch. 3.12.

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
  no semantics claimed.

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
| Multi-period grids (`count` ≥ 2): all reads and writes | refused (mirrored); OPEN — specify producers or restrict `count` = 1 |
| Bucketing by day/week/quarter | absent (mirrored); OPEN with the grid decision |
| Point-in-time read (the "op dat tijdstip" node) | **landed (v1 extension)**: `read(tl, d)` is `IR-SPEC.md` ch. 4.3a (`TemporalRead`, WF-049; ch. 3.3, row 11), anchor-witnessed and replayed by both executors |
| Timeline arithmetic beyond plus/min (verminderd-met, maal, division, percentage) | refused (mirrored); OPEN — per-period lifting of ch. 1.2 rules is the candidate |
| Scalar broadcast into timeline arithmetic / unconditional temporal characteristic | refused (mirrored); OPEN — both need a time domain for unbounded segments |
| min/max/eerste/laatste over timelines | refused (mirrored); OPEN — the two readings (per-period vs over-time) must be defined apart |
| §8.4.3 period condition on a temporal target | **specified (delta): the clip, ch. 3.10** |
| Negated temporal-characteristic condition | **specified (delta) within a clipped span, ch. 3.11**; unbounded complement OPEN; the navigated case stays refused/OPEN |
| Version-validity window on a temporal write | OPEN — the ch. 8 axis decision |
| Date-valued whole-period comparison | refused (mirrored) |
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
relation. **Decided (2026-08-16, WF-047/048):** the connecting relation
is named explicitly — the `connectingRole` binding of `IR-SPEC.md`
ch. 3.2 (v0 wire `verbindendeRol`, an extension field), the same
symbolic-role move as `CONSTRUCTS.md` rows 4/22; executors consume the
binding when present. *v0 mirror note:* a bindingless form derives the
relation as the unique fact type relating `targetType` to `sourceType`
— admissible only while that derivation is unique (WF-048; v0 refused
the non-unique case as marker/skip, and the executors fault on it). In
v1 the binding is required and the derivation retires.

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

### 5.5 Creation in the loop

An object creation must be a function of the **fact base**, not of the
fire count: re-evaluating it against the same facts may not mint a
duplicate. The idempotence contract (v0 realises it with two guards —
a linked-instance check where the created object references its
subject, else a per-(rule, subject) creation marker): **one creation per
rule per subject** unless the fact base itself changed the question.
Relation creation (fact creation) is naturally idempotent — creating an
already-present link is a no-op write.

**Recursive creation** (the dialect rule group, `IR-SPEC.md` ch. 3.9)
deliberately breaks the acyclicity of ch. 5.2 in one controlled way: a
creation whose output feeds its own condition advances **exactly one
generation per instance**, and the chain runs to its fixpoint within
the single evaluation. Termination is the author's obligation — every
**object-creation member** of a recursive group MUST be conditional
(WF-033; relation creations need no guard, being idempotent) — and the
runtime's: a bounded fire budget turns a non-terminating set into a
**fault**, never a silent truncation. Worked example (absolute): a
chain "person aged n creates a linked person aged n + 1, while n < 5"
from age 1 terminates with **5 instances**, both v0 backends, no cap
hit.

### 5.6 The error channel, operationally

§9.5 inconsistencies are **facts on a channel**, not exceptions: the
computation degrades locally (ch. 1.2's error-empty rules) and the error
surfaces beside the result. Operational clauses, mirrored:

- Producers: violated consistency assertions, uniqueness violations
  (a duplicate non-empty key), the allocation degrades of ch. 4.3/4.4,
  division by empty (dividend filled), a negative radicand, an empty
  variable-start anchor (ch. 3.2).
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

Onopgelost nodes take no part in evaluation and must survive into the
report (WF-044); an engine that reaches one fails that rule's evaluation
observably rather than guessing.

Worked trace (`ref/` test verbatim, over the penningdrempel golden):
evaluation date absent (the model is time-free); parameters
{vondstdrempel: 12}; inputs {Jutter j1: aantal aangemelde vondsten 14;
Jutter j2: aantal aangemelde vondsten 3}. Fixpoint after one pass: j1
**penninggerechtigd**, j2 not; no channel entries; end state = the two
instances plus nothing else.

---

## 6. Stratification

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

Each rule form reads and writes nodes:

| Form | Writes | Reads |
|---|---|---|
| assignment / initialisation | its target attribute | every attribute and characteristic its value expression and condition reference — a navigated reference reads the referenced type's node; population(element type) per instance fold |
| characteristic attribution | its target characteristic | idem |
| allocation | target attribute; declared remainder attribute | source amount; ratio, maximum and ordering attributes; condition reads; population(target type) |
| consistency / uniqueness | channel(own name) | assertion operands / key attributes |
| object creation | the attributes and characteristics its toekenningen set on the created type; population(created type) | condition and toekenning-value reads on the anchor |
| relation creation | population(target-role filler type) | condition reads on the anchor and the join-chain traversal (role hops, `IR-SPEC.md` ch. 3.4) |

Edges run producer → consumer — from every form that writes a node to
every form that reads it — in three kinds:

- **positive**, the default: reads by attribute-writing forms
  (assignment, initialisation, allocation), and positive characteristic
  reads anywhere. Attribute writes are revocable — idempotent and
  re-evaluated under freshness (ch. 5.1) — so a transient self-corrects
  within a stratum, and a positive characteristic read is monotone-safe
  (it can only still become true).
- **stratum-strict**: any non-characteristic read (attribute values,
  emptiness and gevuld predicates, comparisons) by a form whose write is
  *irrevocable* — a characteristic attribution (monotone, ch. 5.3), a
  consistency or uniqueness (channel facts are never retracted,
  ch. 5.6), or a creation. Such a form must evaluate strictly above the
  producers of what it reads: a transient it acted on would persist.
  Within a WF-045-admissible recursive-creation cycle the strictness is
  discharged by the one-generation-per-instance progression (ch. 5.5)
  rather than a stratum boundary.
- **negative**: a negated characteristic read, a characteristic read
  under a negation-like quantifier (*geen*, *ten hoogste*, *precies* —
  their truth can be lost by a later monotone write), or a channel test
  (regelversie, ch. 5.6). Once negative, conservatively negative — a
  double negation does not cancel. Negative edges are stratum-strict
  *and* forbidden on any cycle (WF-046).

Parameters and input-only nodes (nodes no form writes) contribute no
edges.

*Scope narrowing, recorded:* relation membership read by a condition
(rollidmaatschap) is treated as input — relation creation contributes a
population edge to its target-role filler type, but finer
relation-level nodes are deliberately not modelled until a corpus case
demands them. Day-kind definitions read only input dates and contribute
no edges.

*Recorded residuals (ch. 7 territory, mirrored):* a condition-guarded
attribute write whose condition later turns false keeps its written
value — the mirror has no retraction — so conditions on
attribute-writing forms read positively here; and §9.5 deposits from
arithmetic transients within a stratum persist (idempotent inserts,
never retracted). Both await the default model of ch. 7.

### 6.2 Acyclicity (WF-045)

**WF-045.** The dependency graph MUST be acyclic after removing every
object-creation form that is a member of a recursive rule group
(`IR-SPEC.md` ch. 3.9): a dependency cycle is admissible **only** when
it passes through such a form. That is ch. 5.5's one controlled
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
form of stratum k + 1 fires. A negated characteristic is then read only
after every producer — and the producers' own input chains — have
quiesced, so a monotone write cannot contradict it later; a channel
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
- A four-deep negation chain (the shape of the art. 4 ladder read in
  `ASP-TRANSLATION.md` ch. 6): k1 from an input comparison, k2 indien
  niet k1, k3 indien niet k2, k4 indien niet k3 — strata 0, 1, 2, 3;
  input x = 0 yields {k1 onwaar, k2 waar, k3 onwaar, k4 waar}, x = 1
  the complement. v0's single boundary cannot express it — precisely
  the case WF-046 exists for.
- An emptiness test on a *derived* attribute by a characteristic rule
  ("gemarkeerd indien a leeg is", a computed elsewhere) is a
  stratum-strict edge: the reader sits above the writer, and the
  outcome is independent of rule order — the transient emptiness is
  unobservable.
- Ill-formed: `a ← b + 1` and `b ← a + 1` on one object type is a
  WF-045 cycle through no creation (negatief case
  `wf045-cyclic-attribute-dependency`). A recursive creation whose
  condition negates a characteristic derived from its own output is a
  WF-045-admissible cycle carrying a negative edge — a WF-046
  violation (negatief case `wf046-negated-read-in-recursive-cycle`).

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
  available to expressions solely as the v0 execution-date constant —
  `Constante` named `Rekendatum` (day precision; its sibling
  `Rekenjaar`, a whole number, is not yet exercised by any corpus case
  and executors fault on it, recorded).

### 8.3 The vintage read

Selecting a parameter's vintage **is not a separate mechanism**: it is
the ch. 4.3a temporal read of a granular parameter with the evaluation
date as index — `read(tl_p, Rekendatum)`. The read itself keeps its
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

Point reads at a *non*-evaluation-date index (the art. 1 shape,
ch. 3.3) are deliberately **outside** this check: their index is
data-dependent, so gap-to-leeg is their meaning (the anchor case
`jutterswet-aanmeldquotum` pins it); the session-start check covers
exactly the reads whose index is the session's own date, where a gap
is a configuration error, not a data condition.

### 8.5 Worked example (absolute; the anchor case verbatim)

Document: granular parameter `penningbedrag` (year granularity, whole
number); rule: *het huidig penningbedrag van een Jutter* = the
temporal read of `penningbedrag` at `Rekendatum`. Session: evaluation
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

*Status: normative **design**. This chapter fixes the model, its
outcomes, and its lowering targets; the wire shape, WF artifacts
(numbers WF-050/051 reserved, `IR-SPEC.md` ch. 6), executor
implementations, and corpus witnesses land with the Phase-4
implementation cards, sized per construct. Until they do, every worked
outcome below is pinned prose — binding on the implementation, not yet
machine-checked.*

### 7.1 The model: the ladder, and fall-through as its semantics

Adopted direction (`PLAN.md` Phase 4, `IR-REVIEW.md`): Catala's default
calculus — prioritised defaults with explicit conflict detection —
specialised by what the ASP hand-exercises established
(`ASP-TRANSLATION.md` ch. 5 finding 3, ch. 6; `CONSTRUCTS.md` rows
7/8):

- A **default** is a pair (justification, consequence) aimed at one
  target (an attribute write or a characteristic attribution).
- Defaults on one target organise as a **ladder**: a totally ordered
  priority chain of rungs. The ladder is **one writer** evaluating to
  **one outcome**, top-down: the ladder's outcome is the consequence of
  the **highest rung whose justification holds and whose consequence
  evaluates gevuld**; a rung whose justification fails, or whose
  consequence evaluates leeg, **falls through** to the next lower rung
  (Catala's own fall-through). At most one rung supplies the outcome,
  so a single ladder can never conflict — as a property of the
  semantics, not a claim about authors.
- If at least one justification held but every justified rung fell
  through on leeg, the ladder's outcome is **leeg** and is written as
  such (an ordinary ch. 1.2g write); if no justification held at all,
  the ladder **does not write**. Input facts are **not** a rung: they
  are the initial fact base the ladder's write meets under ch. 1.2g.
- The **base rule** is a rung like any other; the initialisation rung,
  when present, sits below it (ch. 7.3). The textual nesting of
  art. 4 — "geldt niet indien …, dat laatste geldt weer niet indien …"
  — is surface form: the frontend desugars each nested layer to a
  self-contained justification (conjoining its enclosing layer's), so
  rungs carry independent conditions and top-down fall-through decides.
  The ladder stays **first-class IR structure**: compiling it into a
  DNF would erase exactly the layers legal amendments edit (the
  exercise-2 finding).
- **Parallel grounds** (art. 4bis "evenmin") are *separate ladders*,
  each on its own target, composing conjunctively downstream (each
  ground guards its consumer independently). Multiple ladders on **the
  same** target are admissible only as **declared-unordered** parallel
  defaults — the IR makes priority explicit (rungs in one ladder) or
  explicitly absent (separate ladders on one target); there is no
  implicit order between ladders. That declared absence is exactly the
  over-determination surface of ch. 7.2.
- **Single-writer, reconciled (ch. 5.2):** the ladder *is* the
  sanctioned form of "many grounds, one target". WF-026's single-writer
  principle lifts from "one rule per target" to "one ladder — or one
  declared-unordered ladder group — per target"; because a ladder
  evaluates as one writer to one outcome, ch. 5.2's confluence argument
  carries over unchanged, and the declared-unordered group is the one
  place it deliberately does not (ch. 7.2's channel is the price, paid
  loudly).

### 7.2 Outcomes: under- versus over-determination

The two failure directions stay distinct (row 8, the Catala
EmptyError/ConflictError shape), mapped onto this IR's own discipline:

- **Under-determination** — no ladder on the target produces a gevuld
  outcome: the target ends **leeg** (written or untouched per ch. 7.1).
  **Decided here (a tracked deviation from Catala, `CONSTRUCTS.md`
  row 8):** Catala's catchable ∅ (EmptyError) maps to ordinary leeg,
  consistent in spirit with the ch. 1.4 empty-as-value decision but
  decided in this chapter, not by it. *Within* a ladder, Catala's
  ∅-fallback survives structurally as fall-through; *across*
  compositions it does not — a Catala frontend must compile
  enclosing-default fallback into ladder structure, never into an
  error bit. That reconciliation is a recorded Phase-5 obligation.
  Honest inexpressibility stays with `Onopgelost` (WF-044) — a default
  never masks an Unresolved node (row 7, the OpenFisca lesson).
- **Over-determination** — two ladders of one **declared-unordered
  group** on one target both produce gevuld outcomes and the values
  differ: the group **does not write** (the target keeps what the fact
  base holds — its input value, or leeg) and one **§9.5 channel fact**
  records the conflict, keyed per ch. 5.6 as rule × subject with the
  group's target form as the rule name. Equal values are no conflict
  (idempotence, ch. 5.1). A single ladder cannot reach this outcome
  (ch. 7.1); the channel is exercised exactly by declared-unordered
  simultaneity — and a linear ladder never pays it (the exercise-2
  finding, now by construction).
- **Characteristic targets** obey ch. 5.3's monotonicity: a ladder on a
  characteristic computes one attribution *decision* at the fixpoint of
  its justification strata — it never writes per rung, and a higher
  rung whose meaning is "not excluded" simply makes the ladder derive
  nothing (no retraction exists to need). Over-determination cannot
  arise on a characteristic (there is only one non-default value);
  under-determination is the ordinary absent characteristic.

### 7.3 Initialisation is the lowest rung

The §9.6 initialisation (ch. 5.4) *is* a default: justification "the
target is empty in the input facts", consequence its value — the rung
below the base rule. The fall-through semantics reproduces ch. 5.4's
operational account case by case: a computing rung with a holding
condition and a gevuld outcome supplies the value (computed wins — 50
in the ch. 5.1 chain example); a computing rung that evaluates **leeg
falls through**, so the initialisation still fills the empty target
(25 — exactly ch. 5.4's "the initialisation still matches"); an
input-supplied target fails the initialisation's own justification and
is left standing; the initialisation alone fills the empty target
(25). The worked pair of ch. 5.4 is binding unchanged. Phase 4 changes
the *justification* from mechanism (a guard) to declaration (the
lowest rung), so the Drools lowering derives the guard instead of
embodying it.

### 7.4 What defeasibility is *not* — and what it must not break

- **Rule-version selection is temporal, not defeasible** (ch. 8.2
  gating; WF-021 keeps the windows disjoint). A later version does not
  "override" an earlier one at the same date — they never co-apply.
  Keeping these mechanisms apart is normative; conflating them is how
  salience leaked into v0.
- **Stratification extends to displacement, it is not replaced by
  priority.** A rung's fall-through reads the justifications and
  consequences of the rungs *above* it: those reads join the ch. 6
  dependency graph as **negative edges** regardless of whether they
  test characteristics or attributes — an **extension of the ch. 6.1
  edge classes** (which today carry only negated characteristic reads
  and channel tests) that lands together with the ladder node. This is
  precisely the debt ch. 6.1's residual note ("conditions on
  attribute-writing forms read positively here … await the default
  model of ch. 7") assigns to this chapter: the ladder is where a
  condition's failure becomes observable displacement, so the ladder's
  edges carry the negativity. WF-045/046's *principles* — no negative
  edge on any cycle; strata exist — apply to the extended graph;
  WF-051 states exactly that. A ladder whose rungs cyclically depend
  on the ladder's own output is thereby ill-formed — no salience-like
  escape hatch. (Within one session evaluation there is still no
  retraction: the ladder decides once, at the fixpoint of its
  justification strata, per ch. 5.3's staging.)
- **Strict-versus-defeasible tagging and cross-rule override**
  (LegalRuleML) are recorded refinements, not v1-design scope.

### 7.5 The lowering table (each row a derivation, not a mechanism)

| Declared (ch. 7) | ASP (`backends/clingo`) | Drools (`regelspraak-java`) | Replaces |
|---|---|---|---|
| ladder rung *n* | one rule with `not applied_m` for each higher rung *m* (over derived layer predicates; the nested art. 4 chain of `ASP-TRANSLATION.md` ch. 6.1 is its desugared special case) | generated guard from the higher rungs' conditions | the salience −1 trick — becomes derivable output |
| fall-through on leeg | the rung's head is underivable when its consequence body is — absence composes | consequence-null guard | nothing (v0 cannot express it) |
| initialisation rung | NAF over the input fact | the empty-guard of ch. 5.4 | the hard-coded guard |
| declared-unordered group | both heads derivable ∧ values differ → `inconsistent/2`, no `val/3` | §9.5 channel insert, no write | silent last-writer-wins |

### 7.6 Worked examples (absolute; binding on the Phase-4 cards)

1. **The art. 4 ladder** (the Marrit dossier and its four
   single-difference variants, `ASP-TRANSLATION.md` ch. 6.2), after
   desugaring to self-contained justifications: refused → excluded
   (base rung); refused ∧ herroepen → the higher rung derives
   "exception", the ladder's decision is *not excluded*; refused ∧
   herroepen ∧ vervallen → the top rung wins, *excluded* again. Each
   dossier variant's absolute outcome as the hand-runs pin them
   (`ASP-TRANSLATION.md` ch. 6.3 runs match). These become golden
   cases verbatim when the ladder node lands.
2. **Initialisation**: the ch. 5.4 pair (50 with the chain, 25 alone)
   unchanged, **plus** the fall-through case pinned: a computing rung
   whose outcome is leeg over an empty target with initialisation 25 →
   **25**, never leeg.
3. **Conflict**: a declared-unordered group writing 10 and 20 to one
   empty target → the target stays **leeg**, the channel carries one
   fact (target form × subject); the same two defaults ordered as a
   ladder → **20 is the outcome iff it is the higher rung** and no
   channel fact; the same group both writing 10 → **10**, no conflict
   (equal values).

## Chapters (planned)

*(none — chapters 1–8 are drafted; ch. 7 awaits its Phase-4 wire and
artifacts as noted in its status line)*
