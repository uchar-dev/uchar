# The samenloop witness — an ordered statute-shaped competition (low priority)

Goal: C2 ratification question 4, answered A (2026-08-25,
`spec/proposals/PRIORITY-RELATION.md` §10): the load-bearing claim behind
declining the free superiority relation — "samenloop is a priority
declaration, hence ordered defaults" — gains a machine-checked witness.
Write one own-authored Jutterswet article ordering two competing grounds
("bij samenloop geldt de hoogste"), compile it as an ordered defaults group (or a
comparison-justified default) per the `PHASE4-DEFAULTS.md` §6 samenloop
analysis, and admit it as a golden with absolute expected values through
both executors. This is the corpus's first statute-shaped ordered
*competition* — the existing defaults anchors order exceptions, not
competing entitlements.

Acceptance:
- A new own-authored golden (anchor or plain) whose defaults order two
  competing grounds, with cases pinning: the higher ground winning when
  both apply, the lower applying alone, and neither (leeg or the
  documented base). Both executors replay it exactly (gates 5–6); the
  cases refute noop (gate 7).
- All nine gates green.

May touch: `corpus/golden/` (the new golden only), `corpus/jutterswet/`
(the article text, if the wet grows a numbered article for it),
`scripts/check-noop.sh` (survivor list only, with reasons).
Out of scope: any spec or executor change (the machinery exists —
WF-050/051, RS ch. 7); `spec/CONSTRUCTS.md` (registry note serial at
merge if needed); the free superiority relation and strength tags
(decided non-goals, §10 of the proposal).

Result: landed as the plain golden `corpus/golden/jutterswet-samenloop/`
(hand-authored ir.json, extension form `prioritizedDefaults` — the plain zone
already carries hand-authored defaults, e.g. `synth3-nestkorting`; anchor
was not needed and stays untouched). The own-authored samenloopbepaling
("berggeld voor de berger, wachtgeld voor de wachter; bij samenloop van
beide aanspraken geldt de hoogste") compiles as a three-default **ordered
competition** on `Jutter.vergoeding`: a comparison-justified samenloop
default (both grounds AND wachtgeld > berggeld → wachtgeld) above the two
grounds in article order (berger → berggeld, wachter → wachtgeld) — the
ordering itself carries the samenloop meaning, no exception defaults. Six
hand-calculated cases pin all flanks: both grounds with the wachter
claim highest (45), both with the berger claim highest (60), the tie at
the strict-comparison boundary (40), each ground alone (a higher amount
without its ground never wins: 30 beats a groundless 45, mirrored 45
beats a groundless 60), and no ground → leeg. Both executors replay all
six exactly (gates 5–6: 321 cases); five refute --noop, the geen-grond
flank was shown red unpinned and then pinned as a reason-carrying
survivor (gate 7: 287 refute / 34 survivors). Deliberately left: the
wet itself did not grow a numbered article — the bepaling lives in the
golden's bron prose like the other hand-authored defaults goldens; a
future wet-v5 can absorb it verbatim.
