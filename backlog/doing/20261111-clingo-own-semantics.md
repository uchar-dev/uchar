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
4. The twins, in two halves (maintainer decision 2026-11-11, after a Fable advice in `werk/review-fable/clingo-twins/`):
   - the pass before a creation (`pre_*`, `_ph`, `_p`, `pre_clone`, `map_predicates`) goes without a refusal in its
     place: since RS ch. 5.7 reads faults at the fixpoint, ref plays those documents, and clingo must agree with ref
     on the 14 suite frames it refused (measured with the machinery switched off: they answer ref's rows);
   - a fault-where-reached in the matrix or Let barrier of a fold over members a creation adds is refused with
     CLINGO-035 at translation, as Drools refuses the form (DROOLS-259); the order twins (`_c`/`_o`) go. The suite
     tests that pinned the twins change accordingly, named in the Result.
   The order of created members itself is a norm question outside this card (provisional decision: instance order).
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

Result, steps 1–4 (2026-11-11, landed while steps 5 and 6 continue):
- step 1: the byte oracle dumps every runner program of every golden and red case file (1,110 on the start) and
  every suite program (1,060); it is a tool of this card and lives beside the repository
  (`werk/clingo-own-semantics/oracle/oracle.py`), not a gate.
- step 2: ref's document environment is built once instead of in 13 literal copies; rules go through
  `emit_rule ~head ~body`, and `split_rule`, `body_literals` and the guard read from rule text are gone (403
  places). Oracle: byte-equal but for one space less in the fallback rule of a sum (15 programs; the same rule).
- step 3: a guard's code comes from a register filled where the guard is written (`emit_viol`, `guardc`), no longer
  from `viol_code`'s prefix match over sentences; a probe beside both readings found 0 differences; oracle
  byte-equal. `scripts/check-exec-codes.sh` checks the parameters of a `guardc` site too. Seven suite checks read the
  register of a real translation instead of a hand-written witness.
- step 4, first half: the pass before a creation (`pre_*`, `_ph`, `_p`, `pre_clone`, `map_predicates`) is gone, with
  no refusal in its place. The suite frames tr-cr-1, tr-w-1, tr-pm-1, tr-pm2-1, tr-pm2-ctl, tr-kv-1, tr-kv-listed,
  r6-ebar, r6-alle-fold, r6-pres, tr-of-1, r6-of-alle, r6-agg-comp, r6-nav and the six former [L2] frames
  (tr-strat-1, r6-strat-sc, r6-rapport, P6a, P11, P13) went from a refusal to clingo ≡ ref; r6-dmd-1 now pins the
  existing limit [V3] (clingo leeg where ref gives 4), which the refusal hid.
- step 4, second half: a guard whose reach can hang on the order of created members is refused at translation with
  CLINGO-035 (`refuse_order_dependent`); the order twins `_c`, `_o`, `_q`, `lbr_o` are gone. The frames cr-*, mx-*,
  ms-2, ms-4, the eight stratum frames, [N1]–[N4] and the three EF-037 frames now pin that refusal. Provisional: a
  range counts as grown only where a step runs through a fact type a creation links (`created_fact_types`); a Let
  barrier counts as order-dependent only where it compares members of such a range — a barrier raised by a read in
  the matrix stays, a loud residual recorded as README [L3] with frame l3-3-8.
- Corpus: 0 of 1,110 programs changed across step 4; gates 6, 8 and 17 green. ASP-TRANSLATION and the CONSTRUCTS row
  `ParameterRef` follow.
