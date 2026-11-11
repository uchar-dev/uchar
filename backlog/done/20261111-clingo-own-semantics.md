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

Result, steps 5 and 6 (2026-11-11):
- step 5, tripwire: gate 1 fails where `backends/clingo/lib` names `Uchar_ref.Eval` (or opens `Uchar_ref` whole),
  `Uchar_ref.Arith`, ref's calendar in `Timeline` (`grid_*`, `calendar_*`, `attr_grid`, `param_grid`, `bucket`,
  `whole_units`, `add_months`, `days_in_month`, `is_leap`, `is_open`), `Value.civil_from_days`/`days_from_civil`, or
  aliases `Uchar_ref.Timeline`. Red on the start (56 hits), green at the end. What clingo still reads of
  `Uchar_ref` is the case encoding (`Case`, `World`, `Timeline.t`/segments, `Value`); it shares the verifier's
  declarations (`Ir_v0`), which is no executor semantics.
- step 5, own semantics:
  - validity: every version is translated; the program carries the evaluation date as `as_of/1` and each bounded
    window as `valid/1`, and every rule of the version waits for it; EF-012 (a window without an evaluation date) is
    a witness in the program. Fault in clingo found and fixed: a window on an allocation, a consistency or a
    uniqueness was refused at translation while ref plays the form (suite frame "a consistency the evaluation date
    excludes asserts nothing").
  - fold order from the case's links (RS ch. 5.7a); the die/dat filter of the horizon fence in the program (the two
    translation refusals go; the two suite frames now pin that ref and the program both fault EF-037 {bewaartarief,
    V1}); the parameter-set check of RS ch. 8.4 in clingo's own code; the static questions (role, cell, target,
    host, timeline read, quantifier-fold form, static type, the WF-126 grid, a creation's back link) answered by
    clingo with the verifier's declarations.
  - unit factors (`factors.ml`, RS ch. 2.6) and the calendar and grids (`calendar.ml`, ch. 3.2, 3.6, 3.8) are
    clingo's own; 24 suite checks put the worked examples of ch. 2.6, 3.2 and 3.6 on them directly. A probe of
    `Calendar` against ref's `Timeline` over every day of 1890–2110 on ten grids: 0 differences.
  - fault in ref found and fixed (own commit): the session-start check of RS ch. 8.4 skipped defaults groups; witness
    WV-8.4-sessiestart-defaults and the red case `jutterswet-penningvintage-defaults-zonder-tijdlijn` (EF-014 in ref
    and clingo; Drools refuses the TemporalRead at load, DROOLS-023, listed in gate 8 with its reason).
  - Provisional: refusals that fall before a program exists (a creation on a cycle, CLINGO-038) compare the window
    with the date at translation in clingo's own code; the stack rules of a target do not wait for a window; `as_of/1`
    only in programs where a form carries a bounded window; with several failing parameters the check reports the
    first in document order, ref the last found (no corpus case has two).
- step 6: one record `carrier = { witness; guard_body }` for the guard carrier of rule level (`rule_carrier`) and a
  closed body; one `compared_builtins`/`compared_with_factors` for the IfThenElse condition; one `quotient_terms`,
  `quotient_misfit_guard` and `division_misfit` for the three places a division chooses its guard; `rule_let` and
  `closed_let` for the Let arms; `money_mult_rate`/`money_mult_fences` take the carrier. Oracle 0 differences over
  3,227 files, suite count unchanged.
- The oracle over the whole card: corpus programs changed only where clingo now carries its own semantics (the four
  goldens with a window, jutterswet-keurlast's filter); no golden or red outcome changed.
- fixed (found during step 6): a division in a closed body chose the write bound EF-006 on a remainder also inside a
  value or in a condition, a norm code where the encoding meets its own limit (a fold `acc + x ÷ 3` over [1, 2]: ref
  1.00, clingo EF-006); now CLINGO-031 inside a value and CLINGO-039 in a closed IfThenElse's condition, as at rule
  level. Witnesses in the clingo suite. Provisional: a quotient in a closed body never counts as the written value,
  so EF-006 and CLINGO-033 no longer come from a closed body.
- Not done: the order of created members as a norm (provisional decision of 2026-11-11: instance order) is a spec
  and ref question outside this card.
