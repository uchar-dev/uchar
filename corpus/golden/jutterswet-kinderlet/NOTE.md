# jutterswet-kinderlet

The closed-body **`Let`** witness (ASP-TRANSLATION S15/S18/S19, form 3).
`lettotaal` folds `acc + (let bijdrage = grondslag maal x in bijdrage)` over the
list parameter `kindertallen`. The Let lives INSIDE the fold body: its value
`grondslag maal x` is a closed-body term, so it is **inlined** — the binder maps
to the value's term with empty atoms (the recurrence-bound convention), and the
value's `param(…)` binding atom threads into every read site. This is the
`let_binder` discipline carried into the body context; a closed-body binder is
never leeg, so there is no subject-keyed helper and no leeg barrier (the rule
level's shape).

The value carries an atom that would ground **unsafe** if dropped
(`param("grondslag",P)` binding `P` in the term `(P*V)`), so the witness pins the
threading, not merely the binding.

## Hand-computed absolutes (case1, `kindertallen = [2, 3]`, `grondslag = 5.00`)

| x | bijdrage = 5.00 · x |
|---|---------------------|
| 2 | **10.00**           |
| 3 | **15.00**           |

`lettotaal = 0.00 + 10.00 + 15.00 = 25.00`.

case2 (`kindertallen = []`): the fold base fires on `lpres`, so
`lettotaal = 0.00`.

Both cases match exactly in ref and clingo (gate 5 ≡ gate 6). ROOD-EERST: before
the broadening clingo faulted the closed_term catch-all (`… a Construct or a Let
(no corpus case for more)`).

## The sharp afbakening — the OUTER-binder reading stays refused

A binder bound by a Let OUTSIDE the closed body (a rule-level `let_binder`
helper) is a different, ill-formed shape: `Let(f = <param>) in Fold(+, 0, λacc
x. acc + x·f)`. ref faults cleanly (`unbound binder "f" (the verifier should
preclude this)`); clingo previously ground-crashed (unsafe `LB` variable, exit
65). The `closed_term` `Binder_ref` fence now refuses it with a clean
TRANSLATION FAULT (`the closed body reads binder "f" bound by a Let OUTSIDE it
…`) — the parallel verifier card
(`backlog/new/20260825-closed-body-enclosing-binder.md`) will make it
WF-064-ill-formed; until it merges this fence catches the form. This witness
adds only the INSIDE-body Let.
