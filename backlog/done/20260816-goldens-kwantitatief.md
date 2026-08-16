# Golden corpus: temporal and quantitative Jutterswet cases

Goal: extend `corpus/golden/` with cases exercising the machinery the
spec now covers but no golden witnesses: timeline attributes and
parameters (penningjaar; RUNTIME-SEMANTICS ch. 3), aggregation ("het
aantal vondsten … in het penningjaar"), units (€, kg), allocation in its
modalities, uniqueness/consistency, day kinds — each case as `bron.rgs`
(RegelSpraak source over the quantitative extension articles) plus
`ir.json` emitted by the pinned toolchain, admitted only when gates 2
and 3 pass. The WF-index golden cells in `spec/IR-SPEC.md` move from
OPEN wherever a new case witnesses a rule non-vacuously.

Depends on: the quantitative extension articles
(`backlog/new/20260815-jutterswet-kwantitatief.md`) for statute text;
the toolchain pin for emission.

Acceptance: new `corpus/golden/<case>/` directories pass gates 1–3; the
WF-index golden column updated for every witnessed rule; each case's
`bron.rgs` traces to a Jutterswet article.

May touch: `corpus/golden/` (protected for `anchor/` only — no anchor
cases here), `spec/IR-SPEC.md` (protected; WF-index golden cells only),
`backlog/`.

Out of scope: statute-text authoring (the kwantitatief card); negatief
cases; anchor cases with absolute expected values; schema changes;
source anchoring.

Result: four goldens landed (vondsttelling: roles + count/sum + kg + sum
fallback; berging: proportional allocation with maximum, floor, rest +
a real unit-conversion chain; vondstnummer: uniqueness + date-ordering
consistency; penningjaar: named timeline + facet adoption + yearly
parameters), each byte-identical re-emitted from the pinned toolchain,
gates 1-3 green; corpus/golden/README.md traces every case to its
wet-v4 article with three honest coverage notes; 20 WF-index golden
cells filled (WF-013 deliberately left OPEN - needs a reciprocal fact
type; WF-019 now witnessed by multi-element lists). Side effect: the
vondsttelling case exposed a verifier false positive, fixed by the
separate verifier-rolebron card. Deliberately left: stormvloed double
count and penningjaar-scoped telling (need timeline/day-kind machinery
in rules), anchor cases, negatief additions.
