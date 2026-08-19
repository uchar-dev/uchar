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

May touch: `corpus/golden/synth2-*/`, `backlog/`.

Out of scope: new executor or verifier code (synthesis only — any
disagreement routes to triage, never to a fix inside this card); the
TemporalRead-over-derived-timeline chain (clingo's timeline guard
requires case-supplied timelines — a real extension, its own card);
scale beyond three directories.

Result: three generators, thirteen cases, all admitted exclusively
through scripts/admit.sh; the independent recomputation lens verified
every cell and found nothing. Now witnessed: the pro-rata fold across
BOTH mixed-decimal scaling paths (0-dec base → 2-dec target and the
reverse), under a rule condition with both flanks pinned (segments when
true, the empty timeline when false), across three instances with
different true days on the shared bucket facts, leap-aware (feb-2024,
29 days) and with the no-base-coverage edge (synth2-prorata-breed);
the empty algebra under composition as lasting goldens — a coalesced
product (0) computes through verminderd-met while a genuinely empty
left propagates, the division channel folds to 0 inside a coalescing
join, and a false condition suppresses the channel fact per instance
(synth2-leegcomposities); rounding as an operand inside larger
expressions, with REKENKUNDIG uniquely separated from all five modes
across two cases — 17/8 = 2.125 → 2.1 refutes the ceiling modes,
22/8 = 2.75 → 2.8 refutes the floor modes (synth2-afrondingscombinaties).
Remaining unpinned, with why: NAAR_BENEDEN vs RICHTING_NUL stays
unseparated on this material (identical on non-negative operands; the
separating domain is fenced in clingo — the recorded structural
boundary); the TemporalRead-over-derived-timeline chain stays out
(clingo's timeline guard requires case-supplied timelines — a real
extension, its own card). Corpus at 90 running cases, ref ≡ clingo
everywhere; the 77→76 miscount in the tijdsevenredig note corrected.
