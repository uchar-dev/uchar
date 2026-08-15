# uchar IR — Runtime semantics

*Status: ch. 1–2 are normative **draft** (v0 mirror with tracked deltas and
OPEN decisions); ch. 3–8 are skeleton. Authority for undocumented behaviour
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
equal; non-numeric equality does not. Both are mirrored; whether v1 keeps
the asymmetry is part of the OPEN decision in ch. 1.4.

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
gives leeg (or the fallback). The two rules coexist in v0; naming them
apart is a ch. 3 obligation.

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

### 1.4 OPEN — the "empty = 0" design decision (PLAN §9, decision 7)

Ch. 1.2 is the **mirror**, not the frozen decision. Whether a missing value
is *zero* or *unknown* is a substantive legal choice. The spec keeps it
open until the maintainer freezes it, with these inputs on the table:

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
   parse time). OPEN: whether v1 routes numeric-zero division into the
   same inconsistency channel or keeps it a hard fault.

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

Division by numeric zero: hard fault (ch. 1.4 point 4). A rational literal
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

## Chapters (planned)

3. **Timeline algebra.** Segments, cut-merge, granularity and multi-unit
   periods, variable starting points, pro-rata; the §5.1.4-vs-§5.8.2 empty
   split of ch. 1.2 named and specified.
4. **Allocation (verdeling).** Equal shares, pro-rata, and ordered group
   depletion as recurrence equations over groups — not "one helper call" —
   including maximum, rounding-floor, and the undistributed remainder.
5. **The evaluation model.** Forward chaining to fixpoint; freshness and
   idempotence of writes (the change-detection rule ch. 1.2 relies on);
   what "a berekening is an equation" means operationally.
6. **Stratification.** Negation over derived characteristics requires
   stratification — a well-formedness condition on the IR (with a WF
   rule), not an engine agenda trick.
7. **Defaults.** Initialisation as an explicitly defeasible default (§9.6,
   ch. 1.2 assignment rule); from Phase 4: the adopted default calculus
   (priorities, conflict detection, the inconsistency channel).
8. **Time selection.** The single axis (valuation = calculation = vintage
   selection); fail-fast vintage semantics (gap and overlap are errors,
   never silent defaults); the named extension point for a knowledge-time
   axis. Parameter value data (`Parameterset`) is specified here.
