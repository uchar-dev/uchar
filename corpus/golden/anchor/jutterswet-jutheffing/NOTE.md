# jutterswet-jutheffing — runtime version gating, replayed

The replaying witness for **runtime version gating**
(`spec/RUNTIME-SEMANTICS.md` ch. 8.2; validity windows `IR-SPEC.md`
ch. 3.10, WF-021): two dated versions of **one** rule name —
`jutheffing naar jaartarief #1` (window 2025-01-01..2025-12-31,
aangiftewaarde + 30) and `#2` (window 2026-01-01..2026-12-31,
aangiftewaarde + 70) — write the same target `jutheffing`; the
evaluation date selects the applicable version. Before this anchor the
only two-dated-versions document was the verifier-only witness cabinet
(`corpus/golden/jutterswet-vormenkabinet`, no `case*.json` by design),
so this gating was exercised by zero replaying cases.

Hand-computed absolutes (aangiftewaarde 200 in every case; the
unwindowed control rule `registratiegetal bij aangifte` computes
200 + 1 = 201 regardless of the date, which keeps every case a gate-7
no-op refuter):

| Case | asOfDate | applicable version | jutheffing | registratiegetal |
|---|---|---|---|---|
| `case1-tarief-2025` | 2025-07-01 | `#1` | 200 + 30 = **230** | 201 |
| `case2-tarief-2026` | 2026-07-01 | `#2` | 200 + 70 = **270** | 201 |
| `case3-buiten-elk-venster` | 2027-02-01 | none | **leeg** (`attributeEmpty`) | 201 |

Case 3 pins the outside-every-window flank exactly as both executors
behave: a date every version's window excludes is **not a fault** — the
versions simply do not apply (ch. 8.2 gating; `ref/lib/eval.ml`
`applies`, mirrored at translation time in
`backends/clingo/lib/translate.ml`), the target stays empty, and the
control rule still fires. The fault route exists only for a bounded
window in a *time-free* session (`asOfDate` null), which is not this
case's shape.
