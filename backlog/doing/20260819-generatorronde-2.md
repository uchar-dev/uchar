# Generator round 2: pin the verified-but-unpinned cells

Goal: the second PLAN §8 generator round pins the cells the recent
verification rounds exercised in /tmp but the corpus does not witness:
(a) the pro-rata fold across multiple instances sharing the global
bucket facts, under a rule voorwaarde, and through both mixed-decimal
scaling paths (base coarser and finer than the target); (b) the
empty-value compositions the round-1 empty-edge hunter probed and found
agreeing (a coalesced product under verminderd-met's propagating left,
a division channel nested in a coalescing join under a condition);
(c) rounding-mode combinations inside larger expressions (a rounded
division as an additive operand, a rounded percentage). Three
synthesized golden directories, hand-computed absolute expectations,
admitted exclusively through `scripts/admit.sh`.

Acceptance: all eight gates green; the new goldens replay
differentially (gates 5–6) and each refutes the no-op; the Result
records which previously-unpinned cells are now witnessed and which
remain (with why).

May touch: `corpus/golden/synth-*/`, `backlog/`.

Out of scope: new executor or verifier code (synthesis only — any
disagreement routes to triage, never to a fix inside this card); the
TemporalRead-over-derived-timeline chain (clingo's timeline guard
requires case-supplied timelines — a real extension, its own card);
scale beyond three directories.
