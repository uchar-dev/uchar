# Jutterswet statute text and dossiers as corpus source material

Goal: Move the Jutterswet from README summary to real corpus source material
in `corpus/jutterswet/`: the statute text of v0 (five articles) and the
amendment versions v1–v3 as separate, dated texts, plus the dossiers (Ade,
Brecht, Norre) as scenario seeds — own-authored, following the calculemus
course material (the maintainer supplies its location at session launch).

Acceptance: `corpus/jutterswet/` contains per-version statute text and the
dossiers; `corpus/jutterswet/README.md` links them;
`scripts/check-self-contained.sh` passes. `corpus/jutterswet/` is not a
listed artifact zone, but the texts must be publishable as-is: no
machine-local paths, no private-repo references in the material itself.

May touch: `corpus/jutterswet/`, `backlog/`

Out of scope: RegelSpraak sources for the cases (golden-corpus work,
Phase 1); the zorgtoeslag realism case (own card when Phase-1 corpus work
starts).
