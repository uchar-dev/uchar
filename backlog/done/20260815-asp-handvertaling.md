# Hand-translate one Jutterswet rule to ASP

Goal: `spec/PLAN.md` §6 (Phase 3) instructs starting the hand translation
*during* Phase 1: translate Jutterswet art. 1 (verkrijging) including the
art. 2 default ("zolang het tegendeel niet blijkt") into ASP-Core-2/clingo on
paper, as the first content of `spec/ASP-TRANSLATION.md`. Every construct
that does not translate cleanly is recorded as a candidate Drools assumption
and fed back into the delta column of `spec/CONSTRUCTS.md`.

Acceptance: `spec/ASP-TRANSLATION.md` exists with the worked translation and
a program that runs under clingo (output shown); findings recorded as delta
notes in `spec/CONSTRUCTS.md`; `scripts/check-self-contained.sh` passes.

May touch: `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md` (delta notes
only), `backlog/`

Out of scope: `backends/clingo/` code; corpus admission; the full core
translation (Phase 3 proper).

Result: `spec/ASP-TRANSLATION.md` created: the hand translation of
Jutterswet art. 1 (incl. positive recursion through a derived parent
status, exercised by a synthetic second-generation dossier Douwe) and the
art. 2 default in its repaired v1 reading (one NAF over a declared
contrary), plus the naive double-NAF as an executable counter-example that
reproduces the documented inversion for Norre. Three clingo 5.8.2 runs
shown with verbatim answer sets; verified by re-extracting the document's
own code blocks and re-running them (identical results; the program is
stratified, exactly one stable model per run). Findings fed into
`CONSTRUCTS.md` notes on rows 4, 5, 8, 11 and 18: fact type = predicate
(confirmation), top-level-only restriction has no translation target
(confirmation), single-layer default needs no priority mechanism, art. 1's
"op dat tijdstip" is the first corpus-driven demand on the timeline row
(recorded gap — collapsed to atemporal qualification here, as the boolean
dossiers do), and the open/closed-world boundary surfaced concretely.
Deliberately left: the art. 4 exception ladder (the natural next hand
exercise, where stratification becomes load-bearing), art. 6/v3
temporalisation, and the systematic core-IR mapping (Phase 3 proper).
