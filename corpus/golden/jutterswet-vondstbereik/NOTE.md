# jutterswet-vondstbereik

The **MINIMUM/MAXIMUM over a DATE set source** witness. A `Jutter`'s
`vroegste vondstdag`/`jongste vondstdag` fold the `vondstdatum` of its
aanmeldings-`vondsten` through the set-typed `Aggregation MINIMUM`/`MAXIMUM`
arm — the same contributor gathering and the same `#min`/`#max` the
numeric `jutterswet-muntpiek` rides, over members whose values are
calendar positions instead of amounts.

**What this golden exists to falsify**, and why it is a golden where its
additive siblings are red cases. The ordering folds RETURN one
contributed value: a date goes in and the same date comes out, so the
reading is well defined over a date source exactly as ch. 1.2e states it
for any comparable family. The additive folds ADD their contributors, and
a sum of two calendar positions is not a position — `corpus/rood/
datumsetbron-somvouw` and its `TOTAAL` twin pin that refusal in both
OCaml executors. One document shape, two verdicts, and the line between
them is whether the fold combines its members or selects among them. This
golden is the carrying half of that line.

**It is deliberately the kind-twin of `jutterswet-vondstvenster`**: the
same types, the same relation, the same two cases with the same members
and the same hand-computed dates, with `MINIMUM`/`MAXIMUM` where that
golden has `EERSTE`/`LAATSTE`. Reading the two side by side IS the claim
of ch. 1.2e that eerste/laatste are min/max under another name — if they
ever stopped agreeing, one of the two goldens would move and the pair
would say which.

Hand-written IR with no `bron.rgs`: the RegelSpraak frontend types the
sentence out (`minimale waarde van vereist numerieke operanden, kreeg
Datum in dagen`), so there is no producer route to re-emit and gate 10
has nothing to check. Measured 2026-09-20; the eerste/laatste spelling of
the same fold DOES have one, which is why its twin carries a source.

Every expectation below is hand-computed; `ref` and clingo replay them
exactly (gate 5 = gate 6). The Drools backend carries neither of its
transpiler branches for this fold — both accumulate in `BigDecimal` and a
date-valued member has no place there — and refuses the golden per form
with a named reason in gate 12's two channel lists. The square is a
two-executor agreement here and says so.

## case1-bereik — the four source shapes (9 expectations)

Members are listed in the order the case writes them.

| Jutter | vondsten (vondstdatum) | vroegste vondstdag | jongste vondstdag |
|---|---|---|---|
| J1 | V1=2024-01-15, V2=2024-03-01, V3=leeg | **2024-01-15** | **2024-03-01** |
| J2 | (geen) | **leeg** | **leeg** |
| J3 | V4 (vondstdatum leeg) | **leeg** | **leeg** |
| J4 | V5=2023-12-31 | **2023-12-31** | **2023-12-31** |

Eight attribute expectations plus `consistent`.

- **J1 — the empty contributor.** Filled contributors {2024-01-15,
  2024-03-01}; V3 brings no value and is skipped, never ordered as a
  sentinel date. Were leeg ordered at all it would win one of the two
  ends, so this case falsifies that reading outright.
- **J2 — the empty source.** No linked Vondst, no contributor. Neither
  fold has a fallback form (ch. 1.2e — only the som has one), so both
  targets stay leeg. On the ASP side this is also the guard against
  `#sup`/`#inf` escaping into a value.
- **J3 — the all-empty source.** One member whose `vondstdatum` is leeg,
  so the skip collapses the whole contributor set. J2 and J3 must answer
  the same; a reading that counted MEMBERS rather than VALUES would make
  them differ.
- **J4 — the singleton.** One filled contributor, so minimum = maximum =
  2023-12-31.

## case2-volgorde — the ordering is on the VALUES (3 expectations)

J5's vondsten, in the order the case writes them:

| # | lid | vondstdatum |
|---|---|---|
| 1 | V6 | 2025-06-30 |
| 2 | V7 | 2025-02-14 |
| 3 | V8 | 2025-11-01 |
| 4 | V9 | 2025-02-14 |

Contributors sorted by value: 2025-02-14 (V7), 2025-02-14 (V9),
2025-06-30 (V6), 2025-11-01 (V8).

| target | hand-computed | a POSITIONAL reading would give |
|---|---|---|
| vroegste vondstdag | **2025-02-14** | 2025-06-30 (the first member) |
| jongste vondstdag | **2025-11-01** | 2025-02-14 (the last member) |

Two attribute expectations plus `consistent`.

- **Both ends are discriminating.** The first member (2025-06-30) is
  neither the earliest nor the latest date, and the last member
  (2025-02-14) is the earliest rather than the latest, so a positional
  implementation fails BOTH expectations with values that are themselves
  in the case — the failure cannot be mistaken for a lookup error.
- **The tie.** V9 repeats V7's 2025-02-14, so the earliest value is
  reached by two members. The fold yields a VALUE and not a member, so
  which one "wins" is unobservable; that is why the duplicate is here.
- **A day-number ordering is the same ordering.** The ASP side folds the
  encodings (20133, 20133, 20269, 20393) and the dates order the same way
  their day numbers do, which is what makes `#min`/`#max` faithful here
  while `#sum` over the same encodings is not.

NOOP-REFUTING: every computed value on both cases is a derived attribute
of a Jutter, absent from the raw input — no Jutter carries a
`vondstdag` of its own. Measured: both cases fail under `--noop`.
