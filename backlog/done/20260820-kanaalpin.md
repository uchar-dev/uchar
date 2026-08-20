# The inconsistency expectation learns rule and label — after the channel keys harmonise

Goal: the coarse `{"kind":"inconsistent"}` (≥1 channel fact, whichever
rule produced it) gains optional precision: `rule` pins the producing
rule's name, `label` the flagged instance — closing the false-green
window where a case stays green on the WRONG fact (the recorded
anchor-strength limitation). Prerequisite found by design comparison:
ref keys its channel by RULE NAME everywhere, but clingo's §9.5
division channel keys by the reason text ("division by empty
divisor") — rule matching would split the executors, so the division
channel fact first moves to the rule name (a ctx.rule_name beside the
existing rule_guard, set for rules and creation members alike),
matching ref. Then: the case schema gains the optional fields
(additive — every existing case stays valid), both checkers match
them (name-normalised; a clingo creation-term subject never matches a
label), and the load-bearing cases sharpen: strandregister case3
(scope × ever must come from "kavel per kust"), nummerregister
case1/case3, leegcomposities case2/case4, vindersloon case3.

Acceptance: all eight gates green; a deliberately wrong `rule` on a
sharpened case shown red in BOTH executors before landing the right
one; ASP-TRANSLATION ch. 6b's channel-key sentence matches the code.

May touch: `spec/schema/uchar-case-v0.schema.json`, `ref/lib/case.ml`,
`backends/clingo/lib/translate.ml`, `backends/clingo/lib/model.ml`,
`spec/ASP-TRANSLATION.md`, the sharpened `corpus/golden/**/case*.json`
listed above, `backlog/`.

Out of scope: an exclusivity variant ("this fact and no other");
per-reason matching (the reason text stays executor-internal); rood
and negatief corpora (fault fragments, not channel facts); the
toolchain.

Result: the inconsistency expectation carries optional rule and label
pins, and the channel keys agree everywhere they can be pinned. The
design comparison first exposed the split the pin would have created:
ref keys its channel by rule name, clingo's division fact keyed by the
reason text — harmonised via ctx.rule_name (set for rules, creation
members and, after the verify lens caught the stale-name leak,
consistency assertions), the reason text staying executor-internal.
The schema gains the additive fields (rule: name-normalised match;
label: exact, requires rule via dependentRequired — the validator
enforces it, lens-verified); both checkers match them and, on a
mismatch, ENUMERATE what the channel/model actually carries (the
clingo side gained that in the verify round — it said "carries none"
while facts from other rules were present). Red first: a deliberately
wrong rule pin on the strandregister scope-collision MISMATCHed in
both executors with the channel contents named
("kavel per kust × S1; × S2" refusing the "eenmalig nummer"
attribution), then the six load-bearing cases sharpened:
strandregister case3 (scope × ever from "kavel per kust", both
instances), nummerregister case1/case3 (per-rule and per-instance),
leegcomposities case2/case4 (the division channel from "uitkomst van
een meting" on M1), vindersloon case3 ("deel van een vondst" on V1) —
all replaying exactly in both executors; anchor edits are additive
sharpenings only, no pinned absolute value changed. Recorded,
deliberate residue: seventeen coarse cases stay coarse (the field is
optional — sharpening them is routine follow-up work, not this card);
a rule pin naming a non-existent rule is fail-red, never false-green,
but not load-refused like an unknown label (asymmetry recorded, a
maintainer call); unnamed constructs keep divergent synthetic default
keys ("?" vs "<naamloos>") — unpinnable by construction, no
false-green path. ASP-TRANSLATION ch. 6b names the rule-name channel
key. Corpus: 100 running cases, 79 negatiefs, 12 red; ref ≡ clingo
everywhere.
