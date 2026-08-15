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
