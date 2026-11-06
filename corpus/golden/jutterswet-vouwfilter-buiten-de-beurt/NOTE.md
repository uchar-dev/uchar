# jutterswet-vouwfilter-buiten-de-beurt — a dividing fold filter where no form reaches the fold

A filled dividend divided by a numeric zero is leeg with a §9.5 fact of kind
EF-005 (`RUNTIME-SEMANTICS.md` ch. 1.4 point 4), and the division happens only
where a form evaluates the fold that holds it: a default below the one that
decides is not evaluated (ch. 7.1), a composite condition stops at the member
after which its verdict stands (ch. 5.10), a rule whose condition does not
hold does not evaluate its value, and a fold reads only the members of its own
subject's range (ch. 3.4a). Each of the three forms below divides in the
filter of a fold; case 1 keeps every division out of reach, case 2 reaches
each one. Own Jutterswet casus, rights-free.

## Forms

- **zware vondsten** (a defaults group, `Jutter.zware vondsten`): the base
  default counts the vondsten of the Jutter whose gewicht divided by their
  deelnemers is greater than 2; the exception "actieve jutter" sets 0 for an
  actief Jutter and stands above it.
- **rust**: a Jutter is rustig if none of "hij is actief" and "the first
  vinddag of his vondsten that pass the same filter is later than
  2026-01-01" holds; "geen" settles at the first member that holds.
- **meldgewicht**: for a dringend Melding, the sum of the gewicht of the
  vangsten of its melder (a Schipper, a path of two roles) that pass the
  same filter.

## Hand-computed absolutes

Case 1 (`case1-buiten-de-beurt`), consistent:

| Object | Input | Result | Why no division by 0 |
|---|---|---|---|
| J1 | actief; V1 (10, 2), V2 (9, 0) | zware vondsten **0**, not rustig | the exception decides, the base is not evaluated; "hij is actief" settles "geen" |
| J2 | V3 (10, 2, vinddag 2025-12-01), V4 (3, 3) | zware vondsten **1**, rustig | no zero divisor in his range; V3 passes the filter and its vinddag is not later than 2026-01-01 |
| M1 | dringend; melder S1 with V5 (10, 2) | meldgewicht **10** | |
| M2 | not dringend; melder S8 with V8 (9, 0) | meldgewicht **leeg** | the rule does not fire |
| — | S9 with V9 (9, 0), no Melding | | V9 is in no range |

Case 2 (`case2-bereikt`): J3, not actief, with V6 (9, 0) and V7 (10, 2); M3,
dringend, melder S3 with V10 (9, 0) and V11 (10, 2). Zware vondsten of J3 is
**1**, J3 is **not** rustig (V7's vinddag 2026-03-07 is later), meldgewicht of
M3 is **10**, and the channel carries exactly three EF-005 facts:
`Jutter.zware vondsten × J3`, `rust × J3` and `meldgewicht × M3`.

## The differential square

ref, clingo and both Drools branches play both cases (gates 5, 6 and 12);
gate 10 re-emits `source.rgs` to a byte-identical `ir.json`.
