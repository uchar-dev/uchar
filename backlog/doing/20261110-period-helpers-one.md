# One set of period helpers in the Drools backend

Goal: reading a timeline at a day, testing that a period covers or cuts another, and clipping a period to a window
are each written once in `transpiler/tms`. No behaviour changes.

Measured on `ccf6de21c` (a read of the three classes, line numbers of that commit): the same four operations stand in
three classes.

| Operation | `PeriodArithmetic` | `PeriodCondition` | `PeriodDefaults` |
|---|---|---|---|
| read at a day | `read` `:395` | `read` `:358` | `read` `:281` |
| covers | — | `covers` `:364` | `covers` `:287` |
| cuts | `cuts` `:384` | `cuts` `:345` | `cut` `:267` |
| clip / intersection | `clipped` `:141` | `clip` `:284`, `intersection` `:334` | `filled` `:154` |

Beside them: `PeriodCondition.comparisonReading` writes the read inline (`:97`, `:102`); the loop of
`PeriodDefaults` at `:107` is `filled`; `PeriodArithmetic.written` (`:208`) joins as `coalesced` and `writtenValues`
do; and there are two `sameValue`, `PeriodArithmetic.sameValue` (`:416`, by `instanceof Number`) and
`Period.sameValue` (`:93`).

Before building: lay the copies side by side and record where they differ (a bound closed in one and open in the
other, an open bound as `LocalDate.MIN`/`MAX`, null). A difference is either a reason to keep two, named in the
Result, or a fault, which this card fixes with a witness. Whether a `Breuk` is a `Number`, and so which `sameValue`
is right for it, is part of that.

Acceptance:
- Each operation has one implementation, on `Period` or in one helper class; the three classes call it.
- What the generated rules call at run time keeps its name and signature, so the generated DRL text and PatternDSL
  are equal to those of `ccf6de21c` (`BranchDumpsCorpusWitnessTest` and the DRL pins hold unchanged).
- The backend suite passes with the same test count; `make gates` green.

May touch: `PeriodArithmetic.java`, `PeriodCondition.java`, `PeriodDefaults.java`, `Period.java` in
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/`, a unit test for a difference found, this card.

Out of scope: `core/PeriodCoverage`, `core/DayKinds`, `core/WindowPosition`; the period arithmetic of `ref` and
clingo (separate implementations on purpose); the production runtime.
