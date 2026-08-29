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

Result: `corpus/jutterswet/` now carries the real corpus source material:
three consolidated statute texts (`wet-v0.md`, `wet-v2.md`, `wet-v3.md` —
article texts verified verbatim against the canonical source, 18/18 exact),
`versiegeschiedenis.md` (v0–v3 with corpus-fixed fictional dates
2020/2022/2024 and the reading notes: v1 is a *reading* correction with no
text change, so no `wet-v1.md` exists — deliberate deviation from the
card's literal wording), and `dossiers.md` with nine dossiers (the card
expected three; the source canon has eight plus Marrit, the only dossier
exercising the art. 4 exception defaults). All files are self-contained: no
course/lesson references, no rule-ids, no machine-local paths; only public
literature (Sergot et al. 1986) is cited. One verifier finding fixed:
Marrit's established "onbesproken juttersgedrag" fact restored so her
"recht" outcome is grounded. Deliberately left: the quantitative extension
articles (README roadmap, Phase-1), the post-v3 retroactive raadsbesluit,
and the example revision article (the wet's silence on herziening is itself
corpus material).
