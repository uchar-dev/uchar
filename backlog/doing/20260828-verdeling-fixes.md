# Fix the two allocation bugs in the toolchain and sync the copy — gate 12 plays two more

Goal: maintainer decision 2026-08-28 ("doe 3", the explicit new
toolchain write mandate). Two of the five triaged gate-12 divergences
are toolchain-side bugs; fix them at the source and in the copy:

1. **The voorwaarde guard** (the bug card
   regelspraak-java `backlog/new/20260828-verdeling-voorwaarde-ignored.md`
   carries the full analysis): neither `DrlTranspiler.verdeling`
   (L673-768) nor `ExecModelTranspiler.verdeling` (L4420 ff.) reads
   `v.voorwaarde()` — a conditional allocation distributes
   unconditionally. Fix: emit the voorwaarde as a guard on EVERY rule
   the verdeling lowers to (shares, maximum/rounding steps AND the
   onverdeelde-rest rule — the family gates together), in both
   backends. Add a toolchain test pinning gate-false → no writes at
   all, gate-true → identical to today. The uchar golden
   `jutterswet-voorwaardedeling` is the reference shape.
2. **The no-recipient rest** (triage finding 6): a verdeling with zero
   recipients still fires the onverdeelde-rest rule, writing
   restant = totaal; ref/uchar pin restant leeg
   (`jutterswet-gelijkdeling`). FIRST verify what the official
   RegelSpraak spec says about the zero-recipient case (§9.7 /
   typeringen — the spec files live at
   /Users/yeb/Developer/uwv/regelspraak-werkbank/toolchain/specification/v2.3.0/,
   read-only): if it is silent or agrees with leeg, fix the rest rule
   to require ≥1 recipient (both backends + test); if it explicitly
   prescribes rest = totaal, do NOT fix — report, and the uchar side
   becomes a documented delta instead (say so loudly in the Result).

3. **Sync the copy**: apply the identical diffs to
   `backends/drools/src/main/java/.../transpiler/` (the copy must not
   drift from the source — cite both commit shas in the Result).
4. **Gate 12**: after the sync, `jutterswet-voorwaardedeling` (and
   `jutterswet-gelijkdeling`, if fixed) stop diverging — remove their
   value-divergence entries (the staleness check enforces it); played
   rises accordingly. Show the square on each: same case, same answer
   in ref, clingo AND Drools.
5. Move the regelspraak-java bug card to its backlog/done with a
   Result note.

Toolchain regime: commits on regelspraak-java main allowed for exactly
these fixes + their tests (this card is the mandate); run `make test`
AND the Drools integration suite there (existing tests may pin the OLD
behaviour — investigate and adjust those specific pins with
justification, never blanket-weaken); NO SPEC_REV commit (the
maintainer session re-pins at landing); no push (same).

Acceptance:
- Red-first per fix: quote the pre-fix wrong output (toolchain replay
  or uchar Drools runner), then the post-fix agreement.
- Toolchain: make test green, Drools IT suite green (non-zero counts).
- uchar: all twelve gates green; gate 12 played 44 or 45 /
  value-divergence 4 or 3 (depending on finding 2's verdict).

May touch: regelspraak-java `src/main/**` (the two verdeling paths) +
`src/test/**` + its `backlog/`; uchar `backends/drools/**` (the same
transpiler diffs), `scripts/check-drools.sh` (the entries), `backlog/`.
Out of scope: `spec/**` (if finding 2 turns into a delta, that is a
report line + follow-up card, not a spec edit now), `corpus/**` (the
pinned expectations are the norm), the other three divergences
(leeg-overwrite, oscillation, stratification — bigger, later),
`spec/CONSTRUCTS.md` (serial at merge).
