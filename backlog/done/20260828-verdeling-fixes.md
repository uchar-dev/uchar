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

---

## Result (2026-08-28, card verdeling-fixes)

Both triaged toolchain allocation bugs FIXED at the regelspraak-java source and
synced byte-identically into `backends/drools`. Gate 12 plays two more.

### Finding 2 verdict (the official-spec check, done FIRST)
RegelSpraak-specificatie v2.3.0 §9.7.5 "Gebruik van onverdeelde rest" frames the
rest as *"In het geval van verdelen met een aanspraak of een afronding kan het
voorkomen dat na het verdelen een restant waarde overblijft"* — the restant is
"het restant na verdeling", i.e. what remains AFTER a distribution occurs. §9.7.6
Tabel 19 enumerates only EMPTY/exceptional ATTRIBUTE values (verdeelplafond leeg →
"Verdeling wordt niet toegepast"; naar-rato/maximum/criterium leeg → Fout) and
says nothing about a ZERO-RECIPIENT set. The spec is therefore SILENT on the
no-recipient case — it never prescribes rest = totaal. Per the card, silent →
FIX (require ≥1 recipient), NOT a documented delta. leeg matches ref/clingo
(golden `jutterswet-gelijkdeling`).

### The two fixes (both in DrlTranspiler AND ExecModelTranspiler)
1. **Voorwaarde guard on the whole lowered family.** `verdeling` now lowers
   `v.voorwaarde()` as a guard on the share rule, the §9.7.5 onverdeelde-rest
   rule AND the §9.7.6 Fout companions — a false gate writes nothing. DRL splices
   the lowered voorwaarde into every bron pattern; a hoisting/aggregation
   voorwaarde -> honest no-op marker. Exec-model probes scope once and applies
   `gateVoorwaarde(...)` per sub-rule; out-of-scope/CE-hoisting -> honest skip.
2. **No-recipient rest.** The rest rule requires ≥1 recipient (`exists
   <Doel>(<filter>)` in DRL, `DSL.exists(recipientFilterPattern)` in the
   exec-model), so a zero-recipient verdeling leaves the rest leeg.

### Red-first (pre-fix wrong output, uchar Drools runner)
- `jutterswet-voorwaardedeling`: `FAIL case.json: verwacht leeg, werkelijk 40;
  verwacht leeg, werkelijk 50` (J3/J4 distributed though the vrijgavescore gate is
  false/leeg).
- `jutterswet-gelijkdeling`: `FAIL case2-restant.json: verwacht leeg, werkelijk
  40` (V2 has no recipients, yet restant = totaal).
Toolchain red-first (pre-fix DRL emission): the voorwaarde-gate constraint count
in the lowered family was 0 (now 4); the rest rule had no distribution-set
`exists` (now present).

### The square (post-fix; ref ≡ clingo ≡ Drools, per case)
- voorwaardedeling / case.json      — ref PASS(5) · clingo PASS(5) · drools PLAYED
- gelijkdeling / case.json          — ref PASS(5) · clingo PASS(5) · drools PLAYED
- gelijkdeling / case2-restant.json — ref PASS(5) · clingo PASS(5) · drools PLAYED

### Copy sync (heilig)
The uchar copy's transpiler diffs are CONTENT-IDENTICAL to the toolchain diffs:
the same patch (`git diff` of the two source files) applied cleanly to
`backends/drools/...` via `git apply --directory=backends/drools`, and a
diff-of-diffs of the added/removed lines is byte-identical (only the path prefix
differs). No deviation.
- regelspraak-java commit: `685e35d` (main; NOT pushed, NO SPEC_REV re-pin — the
  maintainer lands it).

### Gates (all twelve green)
1 self-contained PASS · 2 ir-schema PASS (143) · 3 verifier PASS (143 golden /
161 negatief) · 4 case-schema PASS (365) · 5 oracle PASS (343) · 6 clingo PASS
(343) · 7 noop PASS (310 / 33 survivors) · 8 rood PASS (22) · 9 catala PASS
(169 checks / 11 goldens) · 10 regelspraak PASS (130 tests / 19 goldens) ·
11 toka-parity PASS (ref 30 / clingo 15+15 / Drools 30 excluded) ·
12 drools PASS — **played 45 (was 43), auto-excluded 72, value-divergence 3
(was 5), no-case 1**. The two goldens left EXCLUDE_KNOWN; the staleness check
enforced the removal.

Toolchain suites: `make test` 557 green, Drools IT (`mvn -B test`) 966 green
(0 failures) — 6 new tests (DrlTest +2 structural, DroolsExecutionTest +2 DRL
firing, ExecModelDifferentialTest +2 exec-model firing). No existing pin weakened.

### Leftovers / out of scope
- The three remaining gate-12 value-divergences (`jutterswet-waardestelling`,
  `jutterswet-waardering-v0vorm`, `jutterswet-kustwacht`) are genuine v0 leeg /
  stratification semantics divergences — untouched, still named v1-delta
  candidates.
- The bug card's "Related observation" (exec-model non-termination on a leeg-source
  gelijkstelling oscillating against an INITIALISATIE) is NOT addressed — its own
  candidate.
- `scripts/check-drools.sh` is a PROTECTED path: this branch is the proposal for
  the maintainer to merge (fast-forward), together with the SPEC_REV re-pin.
