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
| leeg / gevuld predicates | absence — including a failed to-one navigation — **is** leeg = waar | "de leeftijd van zijn partner is leeg" is waar for a partnerless person |

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
`elementObjecttype` with the first operand's owner even here, a
vestigial tag — an instance fold always carries exactly one source
argument, a listed fold at least two. For som/totaal/aantal the element
is *load-bearing* (v0's accumulate joins it and reads only the first
operand), so a multi-operand form of those faults loudly — never a
silent list fold v0 would compute differently. **Tracked deviation
(ratification pending):** the numeric half of this fold is a v1
*widening*, not a mirror. v0's static typing (§4.14/§4.15) admits
eerste/laatste over date expressions only — a concrete non-date operand
is a compile-time ERROR ("eerste van vereist datumexpressies, kreeg …",
`Typering.datumAggregatie`) and the model is refused; only
Onbekend-typed operands reach v0's runtime fold, which is itself the
type-generic ordering above. v1 admits any single comparable family
under that same ordering; the delta belongs to the `FunctieKind`
(aggregations) row of `spec/CONSTRUCTS.md` at the serial merge update.
Worked examples (ref tests verbatim; the `jutterswet-peildatum` golden
pins them through both executors — the numeric pair is the widening's
witness): eerste van (5, leeg, 3) = 3 and laatste van
(5, leeg, 3) = 5; eerste van (2026-06-21, 2026-05-01) = 2026-05-01;
laatste van (2026-06-21, leeg) = 2026-06-21; eerste van (leeg, leeg) =
leeg.

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
mixed-decimals arithmetic — and no `Percentage` node is involved (Catala
pre-divides — `20%` arrives as the rational `1/5`). But because the ASP
side scales every value by its declared `decimalen`, an *unbounded*
decimal form would be integer-only there. The pin: a Catala-decimal
declaration carries a **finite scale `decimalen = S`, with S = 4**
(chosen against the tutorial, fixed here) — the frontend maps `TRat` to
`Numeriek{geheel = false, decimalen = 4, teken onbepaald, eenheid
null}`.

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
own `decimalen` — binds only a *declaration* and the *write boundary* it
guards (ch. 2.4); every intermediate value stays the exact rational of
ch. 2.1 — a `Let` binder (ch. 10.6) and a `Match`-arm result
(ch. 9.2) among them, rounded to no scale — and a declared scale is never
itself a collapse point: those stay exactly the three of ch. 2.1 — an
explicit `Afgerond` node (ch. 2.5), the ch. 2.2 abs-division operator, and
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
frontend wraps those products in `Afgerond {REKENKUNDIG, 2}` — the same
wrapper the money *divisions* already carry. (No existing corpus golden
contains a money multiplication, so the fix is byte-neutral; it lands
with the first fixture that exercises it. The ASP encoding must split
the scale-2 × scale-4 product so the intermediate stays inside the
int31 fences — e.g. `M × R = M × R_hi/10² + M × R_lo/10⁴`, each part
within the existing bound, the sum exact.)

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

### 3.4a The die/dat subselection on instance folds

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

**The self-denoting fold source (WF-083).** The written form "het
aantal `<rol>` van de `<onderwerp>`" may name the fold subject by its
own role name ("het aantal vondsten van de aanmelder", evaluated on
the Jutter who *is* the aanmelder). The wire carries that written role
in the fold-source argument's `navigatieRol`; the field is
presentation of the written form, not navigation (`IR-SPEC.md`
ch. 4.3) — no hop happens, and the fold evaluates exactly as the bare
subject-anchored form: the subject's own linked instances of the
element type are the contributors.

Worked example (the `jutterswet-aanmeldertelling` emitter golden,
verbatim in gates 5–6 and as WV-3.4a-zelfbron in `ref/` tests): a
Jutter linked to two Vondsten — one with gewicht 2,50 kg, one whose
gewicht is leeg — counts **2**: a count folds instances, not values,
so a leeg element attribute still counts (the aggregation row of
ch. 1.2: a count, never empty). A Jutter with zero linked Vondsten
counts **0**, never leeg.

**The type-pair fallback for the fold-source role name (WF-034).** A
written fold-source name that resolves to no declared role (ci,
declared name or declared plural) folds over the **unique connecting
role** derived from the type pair — exactly one fact type connects the
fold subject type to the element type, and it is not a self-typed
two-role fact type, whose two roles are both candidates and so block the
derivation (the ch. 4.1 type-pair block); zero or multiple connecting
roles, or a name matching several reachable roles, keep the loud fault —
never a spelling guess (`IR-SPEC.md` ch. 4.3; uniqueness is counted in
candidate roles, not fact types; the `+s`/`+en` plural synthesis is an
unmirrored delta).

Worked example (the `jutterswet-kaveltelling` golden, verbatim in
gates 5–6 and as WV-3.4a-typenpaar in `ref/` tests): the role
`duinkavel` (kaveltoewijzing) declares no plural, and the written
source "het aantal duinkavels van de Jutter" resolves nowhere — the
unique Jutter/Duinkavel fact type is the source, so a Jutter linked
to three Duinkavels counts **3**; the written singular "het aantal
vat van de Jutter" resolves by declared role name against `vat`
(vatberging, declared plural `vaten`) and counts that Jutter's two
Vaten as **2** — in the ASP backend both counts key the connecting
role's canonical name (`duinkavel`, `vaten`), never the written
lexeme, which as a raw `rol/3` key silently counted 0. A Jutter with
no links counts **0** and **0**; with a second fact type connecting
Jutter to Duinkavel the unresolved written form faults loudly
(the `wf034-ambigu-typenpaar` rejection at the well-formedness
layer).

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
(piecewise-constant, never per day). A fully clipped-away — or
absent — value timeline totals 0, never leeg: the clip's presence
turns absence into 0 (the mirrored v0 nuance; the unfiltered total of
an absent timeline stays leeg). Worked example (the
`jutterswet-getijdenwacht` emitter golden, verbatim in gates 5–6):
10/month through 2023 and 11/month January–June 2024, clipped to
[2023-07-01, 2024-02-01) → 6×10 + 1×11 = 71; the comparison clip
"> 10" keeps the 11-segment → 6×11 = 66; a carrier without periods, and
one without a timeline, both total 0.

**The composite clip (landed 2026-08-21).** A **flat** composite ALLE
condition under gedurende clips to the **intersection** of its
members' true-periods: the pinned runtime nests the per-member clips
over exactly such flat characteristic lists, and interval
intersection is associative, so member order never matters. Members
are own-temporal-characteristic checks. The v0 boundary, stated
precisely: v0 partitions a **static** (non-temporal) comparison
member out as a value-side *guard* before its clip ever runs (an AND
gate under ALLE), and markers a temporal-attribute comparison member
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
complement in the reference interpreter, exactly like the single
negated clip, and shares that shape's standing executor asymmetry
(the ASP side faults loudly; no anchor can pin it differentially
yet). The **ten-minste-1 union** (v0 clips to the union of member
regions) stays a loud, recorded flank in both executors, and WF-055
does not admit it until an executor carries it with an anchor.

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
(ch. 1.2). The absolute variant drops the sign. "hele" in the surface
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
abbreviation. Declared conversion factors and the declaring stelsel are
irrelevant to calendar arithmetic (the stelsel may be null): a year is
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
fractional truncation. The day-kind (dagsoort-as-unit) forms stay
unexecuted, loud — tracked debt, never a silent skip. The datum-tijd
millisecond layer is executed in ref per the paragraph below and,
since the pair-term encoding landed (2026-08-28, lifting the
2026-08-25 embargo), in the ASP backend as well —
`spec/ASP-TRANSLATION.md` S6a; corpus golden
`jutterswet-getijdenpaar` pins it through both executors, the borrow
corner included.

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
v1 the binding is required and the derivation retires. The reciprocal
shape (one role relating a type to itself, WF-011/013) participates in
both routes: its single role is bound and opposite at once, and it
counts as the connecting candidate for a self-typed pair. A reciprocal
relation entry holds in **both directions** — the pinned runtime
writes both to-one legs so navigation never reads a spuriously empty
side; the shared case loader materialises the reverse leg of every
reciprocal link at load, and a conflicting reverse leg on the to-one
role (WF-013) is a load error.

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
are linked over the back-link relation — the explicit `verbindendeRol`
binding (`IR-SPEC.md` ch. 3.3) when present, else the
**uniquely-derivable** connecting relation — both ends.
The 1:1 shape (a to-one created-side role) realises the
linked-instance guard: an already linked instance — a case-supplied
one included — blocks the creation; the executed guard reads the
**input world** (a snapshot: v0's live fact-base negation is narrowed,
recorded — cross-form blocking through links laid during evaluation is
not mirrored), and only the **subject-side stored links** (a case that
delivers the link solely on the created side does not block — the
storage-direction narrowing, recorded). Minted instances live in a
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
explicitly (`verbindendeRol`, `IR-SPEC.md` ch. 3.3 — the type-pair
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
`verbindendeRol {opvolging, opvolger}`. From rank 1 the chain
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

### 5.8 The navigated read

An attribute reference whose endpoint is off-subject (IR-SPEC ch. 4.1a,
WF-053) reads through the role links, one to-one hop per step. The
rules, mirrored from the pinned runtime's null-safe navigation readers:

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
4. **The self-reference navigates zero hops — unconditionally.** A
   single bindingless written step that denotes the subject by a role
   the subject itself fills (IR-SPEC ch. 4.1a: zero far hits AND the
   self-denotation test, far-role resolution always first) reads the
   subject's OWN attribute — the walk is empty. No link is consulted,
   so the read succeeds on an instance with no relation instances at
   all: there is no leeg-on-absent-link here, because there is no hop
   to be absent. A name that does resolve as a far role — the
   reciprocal partner case — stays a real one-hop read with rule 1's
   leeg-on-absent behaviour. The navigated characteristic check is NOT
   in this rule's scope: a self-denoting name there is refused loudly
   by both executors (ch. 4.1a scopes the zero-hop form to the
   attribute read).

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
all. "het strandtegoed van de aanmelder plus 2" is the zero-hop
self-reference: meldkrediet J1 = 12, J2 = 27, **J3 = 9** — J3 has no
links and still reads its own attribute (the unconditional own-read).
"het strandtegoed van zijn strandmaat" resolves as a far role
(reciprocal — order first): maatdeling J1 = 25 and J2 = 10, never a
self-read; J3's maatdeling is **leeg** (rule 1, absent link) — the
contrast that separates the real hop from the empty walk. A same-typed
reference naming a role the subject does NOT fill ("het strandtegoed
van de vondst" on a Jutter) stays a loud fault, never an own-read
(the WF-053 narrowing; negatief `wf053-zelfverwijzing-vreemde-rol`).

### 5.8a The near-side rolcheck

A **rolcheck** ("hij is een wachtjutter", §8.1.7 — the IR's `Rollidmaatschap`)
tests whether the subject **fills** a role. In the reference-field fact
model the subject fills the role iff its reference to the **other end** of
that fact type is set, so v0 lowers the check to a **null-test on that
reference** (DrlTranspiler §8.1.7). The written role name is presentation;
the reference tested is the **connecting role** from the subject's type to
the other-end type the IR carries (`anderTeFiller`) — the same by-pair
derivation `ref/` shares with the ASP backend and the allocation recipients
(ch. 4.1, `Eval.connecting_role`). The rules:

1. **Present reference fills; absent does not.** A subject holding a link
   over the connecting role fills the role; a subject with no such link does
   not. The empty operand is the absent reference — there is no fault here,
   only false.
2. **The "is geen" negation is the inverse null-test — it flips.** Unlike
   the navigated *characteristic* check of ch. 5.8 rule 3 (asymmetric:
   onwaar for the positive and the negated form on an absent link, because
   the negation sits behind a hop), the rolcheck's negation tests the
   reference **itself**, so an absent reference makes "is geen &lt;rol&gt;"
   **waar**. "hij is geen wachtjutter" holds exactly for a jutter with no
   post. In the ASP reading the positive form is a bare `rol/3` join (an
   absent link never binds, so the gated rule does not fire); the negation
   is a helper deriving the link's presence, negated-as-failure, with the
   subject kept bound by the rule's `is_a` base — the inverse null-test, for
   free.
3. **To-many connecting reference is refused, loudly.** The near-side
   null-test needs a **to-one** reference. When the connecting role is
   to-many, v0 cannot lower it to a scalar null-test and **markers** it (the
   far-side "heeft een &lt;rol&gt;" is the distinct `VerreRollidmaatschap`
   exists-join, not this near-side form); both executors refuse it (ch. 5.5)
   rather than silently deciding a first-filler existence test.

Worked example (the `jutterswet-wachtindeling` golden, verbatim in gates
5–6 and in `ref/test/test_ref.ml`): feittype wachtindeling relates the
to-many role wachtjutter (Jutter) to the to-one role toegewezen wachtpost
(Wachtpost). "Een Jutter is wachtdoend indien hij een wachtjutter is"
(positive) and "Een Jutter is vrijgesteld indien hij geen wachtjutter is"
(negation) pin every corner. With J1 assigned to post W1 and J2 assigned to
no post: J1 fills the role (its toegewezen-wachtpost reference is set), so
J1 **is wachtdoend** and **is not vrijgesteld**; J2's reference is absent,
so J2 **is not wachtdoend** and **is vrijgesteld**. The reference and the ASP
run agree on all four (link × {positive, negation}).

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
| consistency / uniqueness | channel(own name) | assertion operands / key attributes; a union reads every source's key attributes; a scoped uniqueness also reads relation(scope hop) — bound by its named fact type, else derived from the written scope field |
| object creation | the attributes and characteristics its toekenningen set on the created type; population(created type); relation(the uniquely-derivable connecting fact type — the back-link, ch. 5.5) | condition and toekenning-value reads on the anchor; population(subject type) — iterating the subjects is a read: it orders creation-after-creation, and a self-typed creation becomes a WF-045 cycle instead of a diverging fixpoint or an unbounded grounding |
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

*Scope narrowing, recorded:* relation membership read by a *condition*
(rollidmaatschap) is treated as input, and a **bindingless** fold or
allocation keeps the population proxy — only reads that name the
relation in a role binding, and the creation's own hops, carry
relation-node edges (the creation-after-creation ordering demanded
this; a bean-field-only v0 creation record carries no symbolic hops
and stays on the bare population write, recorded with WF-028's
uncheckable clause). Day-kind definitions read only input dates and
contribute no edges.

*Navigated reads (ch. 5.8)* consume relation(each hop) — bound hops by
their named fact types, bindingless hops by the WF-053 unique
derivation (an unresolvable walk contributes no relation edges; WF-053
rejects the document, so nothing is hidden on clean input). A navigated
characteristic check additionally reads characteristic(reached type) —
on the type the hop lands on, never the subject — negatively when
negated; the hop relation itself is always a positive read (the
asymmetry of ch. 5.8: an absent link fails the check, so no negative
edge runs through the relation).

*Recorded residuals (ch. 7 territory, mirrored):* a condition-guarded
attribute write whose condition later turns false keeps its written
value — the mirror has no retraction — so conditions on
attribute-writing forms read positively here; and §9.5 deposits from
arithmetic transients within a stratum persist (idempotent inserts,
never retracted). Both await the default model of ch. 7.

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
- A four-deep negation chain (the shape of the art. 4 defaults read in
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
executors; the wire marking is `ongeordend`, IR-SPEC ch. 6.1). The v0
desugaring is ch. 7.7, machine-checked by the
`jutterswet-waardering-v0vorm` anchor (ref replays the v0 forms
natively, the ASP backend the desugared defaults — the gate-6
differential witnesses the lowering). Rule-readability of the conflict
fact is **closed** for v1 (ch. 7.2, decided 2026-08-20); the ch. 7.8
standing norm executes on scalar targets through both executors
(temporal targets stay fenced — recorded, scalar-first). Below-root
exception branching is ch. 7.9 (Phase 5c): the `Default.excepties` wire
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
  Honest inexpressibility stays with `Onopgelost` (WF-044) — a default
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
| plus at most one version-free computing rule (Gelijkstelling/Berekening) | plus one default above it: justification = its condition (an unconditional rule gets the vacuously true empty-ALLE), consequence = its value |

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
edges. This is a re-description of ch. 7.1's "the target keeps what
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

*Status: normative design; the wire form is `Default.excepties` (`IR-SPEC.md`
ch. 6.1, WF-075), an extension field, never emitter-produced. The worked
examples below are the anchors `jutterswet-nestuitzondering` (attribute
target) and `jutterswet-nestkenmerk` (characteristic target).*

Ch. 7.1's defaults group is a **chain**; ch. 7.2's declared-unordered group
is **root-level branching**. What neither expresses is **branching below the
root** — a chain whose exceptions themselves branch. Phase 5c adds it as
one concept applied recursively (`PHASE4-DEFAULTS.md` §7 option i,
ratified 2026-08-22): a default's exception slot may hold **exception
groups** — `excepties = { ongeordend, leden }`, each member an `ExceptionGroup` (a
default chain, descending, highest first, inheriting the root target and
itself nesting via `Default.excepties`). An exception group carries **no
target of its own**. Two default shapes result:

- a **form-a default** — own justification/consequence (or besluit) **and**
  `excepties`: the exception groups go first (below), and its own
  justification/consequence is the **fall-back** when they
  decide nothing;
- a **group-default** — `excepties` with no own justification/consequence/
  besluit: a chain position carrying a *multi-member base group* (or a
  further exception group), whose members reconcile as an unordered
  group.

**The recursive reading.** A default carrying `excepties` first **reconciles
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
One defaults group: default 0 = form-a `nx → attribute (besluit true)` with
unordered exception groups `{ ny → do-not-attribute (false), nz → attribute
(true) }`.

| Case | Input characteristics | `vrijgesteld` | Channel |
|---|---|---|---|
| `case1-y-wint` | nx, ny | **absent** | consistent — the sub-decision `false` displaces `nx`'s `true` |
| `case2-conflict` | nx, ny, nz | **absent** | inconsistent `Jutter.vrijgesteld/0` — the exception groups decide `false` and `true` |
| `case3-terugval` | nx | **present** | consistent — the exception groups decide nothing, so default 0 falls back to its own besluit `true` |

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
`excepties` and no own content; a contentless default is WF-075's
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
| what the frontend emits — **no** `Let` at all: the only read of `de bonus` sits in the §5.8.2 leegwaarde, a pure value position under the aggregation, so it is substituted | **1** — the absent read coalesces to 0 inside the `plus` (the ch. 1.2 `plus` row), exactly as v0 |
| the same IR with that leegwaarde wrapped as `Let bonus = bonusgrond in bonus plus 1` | **leeg** — the read fires clause 2 and empties the rule value |

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
`GEVULD(dossier)`**, `waardekopie` := `dossier.waarde` — a bare
`FieldAccess`, **unguarded** — and `samenvatting` := `{ waarde: premie,
telling: |dossiers| }`). The guard on `premie` is load-bearing: `+`
coalesces a leeg operand to 0 (ch. 1.3), so an *unguarded* `premie` would
write **5.00** at an absent `dossier` (0 + `opslag`), not leeg — the
`GEVULD(dossier)` condition makes `premie` leeg exactly when the record
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
  **leeg**: its `GEVULD(dossier)` guard is false, so the rule does not
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
  translation`), and no corpus case exercises it (the sum-typed-parameter
  sibling, ASP-TRANSLATION S19). The loaders fail fast on an undeclared
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
