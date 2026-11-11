# clingo computes its own semantics, and translates more simply

Goal: the clingo backend no longer takes ref's answer where the differential needs two, and its translation loses
the machinery no corpus case uses. Source: the Fable design review of 2026-11-11
(`werk/review-fable/clingo-architecture/` beside the repository: `notes.md`, `sweep.py`, the 1,071 dumped programs).

Measured: `translate.ml` opens `Uchar_ref.Eval`. Validity (`Eval.window_admits`), fold membership and order
(`Eval.fold_members`), a die/dat filter (`Eval.eval_cond`), the parameter-set check of ch. 8.4
(`Eval.parameter_set_check`), the period walls (`Timeline.grid_*`, `calendar_*`) and the unit factors
(`Arith.conv_factor`, `total_factor`, `restatement`) come from ref. For those, gate 6 proves ref ≡ ref; Drools is
the only independent leg (`jutterswet-vensterheffing`: the clingo program carries no window bound, only the version
ref admitted).

Steps, each a commit or a small series, in this order:
1. The byte oracle: the 1,071 programs of every golden and red case, compared after each step (method in the
   review folder).
2. `doc_env` once (13 literal copies); `emit_rule ~head ~body` in place of re-parsing rule text (`split_rule`,
   `body_literals`, the guard read in `unkeyed_form_read`). Byte-equal.
3. A register filled where a guard is written (reason → code and parameters) in place of `viol_code`'s prefix match
   over sentences. Touches `scripts/check-exec-codes.sh` (protected).
4. Refuse, with CLINGO-035 at translation, a guard whose body carries an order-dependent atom in a document with a
   creation; the twins, the `_c`/`_o`/`_p` families, `pre_*`, `map_predicates`, `pre_clone` and the `_ph` chain go.
   Measured corpus loss: 0 goldens, 0 red cases; the suite tests that pin the twins change — the maintainer decides
   that before this step.
5. A gate-1 tripwire: `backends/clingo/lib` names no `Uchar_ref.Eval`, `Arith` or `Timeline.grid_*` (protected);
   then clingo's own validity (an ASP comparison on an `as_of` fact), fold order from the case links, the filter in
   ASP or refused, its own parameter-set check; then its own calendar grid and factors.
6. One record for the guard carrier of rule level and closed body (`translate_division`/`closed_division`, the
   comparison pairs, the Let arms).

Acceptance:
- Steps 1–3 and 6: the oracle byte-equal, or each difference named and explained.
- Step 5: the tripwire red on the start, green at the end; every difference with ref the change exposes is triaged
  (fault in clingo, fault in ref, or norm question), never hidden.
- `make gates` green after every step that lands.

May touch: `backends/clingo/`, its tests, corpus witnesses outside `corpus/golden/anchor`, this card. Protected (the
main session lands them): `scripts/`.

Out of scope: ref and Drools, except a fault in ref that step 5 exposes (its own commit, named in the Result).
