# jutterswet-vondstvenster

The **EERSTE/LAATSTE over a set source** witness (ASP-TRANSLATION S8). A
`Jutter`'s `eerste vondstdag`/`laatste vondstdag` fold the `vondstdatum` of its
aanmeldings-`vondsten` — the set-typed `Functie EERSTE`/`LAATSTE` arm, which
rides the same contributor gathering and the same `#min`/`#max` aggregation as
`jutterswet-vondstpiek`'s MINIMUM/MAXIMUM, because it is the same fold.

**What this golden exists to falsify.** `RUNTIME-SEMANTICS.md` ch. 1.2e states
that eerste/laatste are ORDERING folds — a `<`/`>` fold over the contributed
VALUES, never a positional pick of the first or last filled member. That claim
is only worth something if a document can tell the two readings apart, so
`case2-volgorde` is built so that the member order and the value order point in
opposite directions, in BOTH directions at once. Every expectation below is
hand-computed; `ref` and clingo replay them exactly (gate 5 ≡ gate 6). The
Drools backend carries neither of its transpiler branches for this fold and
refuses the golden per form with a named reason (gate 12's channel list), so
the square is a two-executor agreement here and says so.

## case1-venster — the four source shapes (9 expectations)

Members are listed in the order the case writes them.

| Jutter | vondsten (vondstdatum) | eerste vondstdag | laatste vondstdag |
|---|---|---|---|
| J1 | V1=2024-01-15, V2=2024-03-01, V3=leeg | **2024-01-15** | **2024-03-01** |
| J2 | (geen) | **leeg** | **leeg** |
| J3 | V4 (vondstdatum leeg) | **leeg** | **leeg** |
| J4 | V5=2023-12-31 | **2023-12-31** | **2023-12-31** |

Eight attribute expectations plus `consistent`.

- **J1 — the empty contributor.** The filled contributors are
  {2024-01-15, 2024-03-01}; V3 brings no value and is skipped, never counted
  as a zero or a sentinel date (ch. 1.2e: empty contributors are skipped).
  Earliest = 2024-01-15, latest = 2024-03-01. Falsifies a reading that lets an
  absent member participate in the ordering: were leeg ordered as anything at
  all it would win one of the two ends.
- **J2 — the empty source.** No linked Vondst, so no contributor. Neither fold
  has a fallback form (ch. 1.2e — only the som has one), so both targets stay
  leeg. Falsifies both a zero-like default and, on the ASP side, a `#sup`/`#inf`
  escaping into a value: the presence guard is what keeps the term underivable.
- **J3 — the all-empty source.** One member, and its `vondstdatum` is leeg, so
  the skip of J1 collapses the whole contributor set. Both targets leeg. This
  is the case that distinguishes "no members" from "no values": J2 and J3 must
  answer the same, and a reading that counted members rather than values would
  make them differ.
- **J4 — the singleton.** One filled contributor, so earliest = latest =
  2023-12-31. Falsifies an implementation that assumes at least two
  contributors before an ordering fold produces anything.

## case2-volgorde — the ordering itself (3 expectations)

J5's vondsten, in the order the case writes them:

| # | lid | vondstdatum |
|---|---|---|
| 1 | V6 | 2025-06-30 |
| 2 | V7 | 2025-02-14 |
| 3 | V8 | 2025-11-01 |
| 4 | V9 | 2025-02-14 |

Contributors sorted by value: 2025-02-14 (V7), 2025-02-14 (V9), 2025-06-30
(V6), 2025-11-01 (V8).

| target | hand-computed | a POSITIONAL reading would give |
|---|---|---|
| eerste vondstdag | **2025-02-14** | 2025-06-30 (the first member) |
| laatste vondstdag | **2025-11-01** | 2025-02-14 (the last member) |

Two attribute expectations plus `consistent`.

- **Both directions are discriminating.** The member order was chosen so that
  neither end coincides with its positional twin: the first member (2025-06-30)
  is neither the earliest nor the latest date, and the last member
  (2025-02-14) is the earliest date rather than the latest. A positional
  implementation therefore fails BOTH expectations, and it fails them with
  values that are themselves in the case — so the failure cannot be mistaken
  for a lookup error.
- **The tie.** V9 repeats V7's 2025-02-14, so the earliest value is reached by
  two different members. The fold yields a VALUE, not a member, so which of the
  two "wins" is unobservable — and that is the point of including it: `ref`'s
  `agg_eerste` keeps the later equal contributor where `agg_min` keeps the
  first-seen, and this case pins that the difference cannot show. A reading
  that returned a member instead of a value would have to choose, and would
  have nothing to choose by.

NOOP-REFUTING: every computed value on both cases is a derived attribute of a
Jutter, absent from the raw input — no Jutter carries a `vondstdag` of its own.
