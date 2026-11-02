# jutterswet-vormenkabinet — the witness cabinet

This golden is the **witness cabinet** (see the "witness cabinet" section
of `corpus/golden/README.md`). It exists to witness, positively and
verifier-clean, the well-formedness rules whose constructs no other golden
carries. After the 2026-09-19 shrink
(the next section) that is WF-004/014/040 (the dimensioned attribute and
its cell read), WF-016/032 (the variable-start timeline with its start
point), WF-042 (the time window — **eligible to leave since 2026-09-20**:
the window executes, and `jutterswet-premievenster`, which carries it in both
wire positions, is its successor), plus WF-017's positive side, which the section near the end
carries (until L3 also WF-034 arm (s)'s identity half, which left the wire
with the day-kind registry). Every construct it holds executes in `ref`
and the ASP backend, so it carries a case, `case1-cellen.json`, which gates
5 and 6 replay (the section "The case" below).

## What left, and to whom (2026-09-19)

The admission rule at the bottom — a construct that becomes executable in
both executors leaves — had not been applied since the cabinet was built.
Measured over the corpus's 200 golden documents (198 carry case files and
therefore replay through gates 5–6; the two that do not are this cabinet
and `jutterswet-seizoenvondst`), **six** of the ten rows had an executing
witness and left; the other four are in the table further down. Two of the
six travelled together, in one pair of rules, so the six rows are five
removals from the document:

| What left | WF rows | The executing successor, measured |
|---|---|---|
| `staat na schouw`, with the `kwaliteit` domain and the `staat` attribute | WF-006, WF-039 (enum-label arm), the enum slice of WF-023 | `jutterswet-keurstaat`: it declares the domain `keurklasse`, and its `stempel voor een veilbare vondst` writes the label `gaaf` into an `Enumeration`-typed target — the same `equation`-with-enum-`Literal` shape this rule had. `jutterswet-vaarwatertoeslag-catala` carries a second one |
| `aanwas na schouw`, with the `aanwas` attribute | WF-039 (sign arm) | `anchor/jutterswet-taxatie` (the literals `2` and `3`, each typed `nonNegative`), `anchor/jutterswet-aanspraaktelling` (`0.00`), `anchor/jutterswet-hardheid`, `anchor/jutterswet-vondstdossier` (`120.50`, `3`), `jutterswet-postenheffing`, `synth3-vondststaat`. Difference recorded rather than glossed: those carry the sign-restricted `Literal` as an **operand**, where this rule carried it at the write position. WF-039 reads the literal's own type wherever `walk_expr` reaches it, so it is the same node under the same clause — no corpus document outside this cabinet had it at a rule's value |
| the counting quantifier of `peiling bij dubbele borging` — `atLeast` with threshold 2, now `all` with 0 | WF-041 | `jutterswet-wachtploeg` carries `atLeast` with threshold 2 on the composite host, the same form this rule had. Eight further composite goldens carry the other quantors and thresholds (`paasvondst`, `rubriektabel`, `seinwacht`, `sluimerwacht`, `tweevoudkeur`, `vacuumwacht`, `vrijgavetoets`, `wachtdeling`), and `jutterswet-reddingsvloot` carries the fold host of WF-108 (e). The rule itself stays: its `TimeWindow` member is a row that did not leave — *eligible to leave since 2026-09-20, when the window became executable (the header above)* |
| the two dated versions of `doorwaadbaar bij lage stand` (`#1`, `#2`) | WF-021 | `anchor/jutterswet-jutheffing` — `jutheffing naar jaartarief #1`/`#2` is the only other corpus document whose rule names group to one name under the version strip WF-021 applies, with non-overlapping windows replayed per window |
| the whole-year qualifier on those same two rules, with the `waterstand` read and the `doorwaadbaar` characteristic | WF-043 | `jutterswet-keurploeg`, `jutterswet-ploegpremie`, `jutterswet-reddingsvloot` and `jutterswet-sleepvloot` each compare a `month`-granular attribute under a whole-year qualifier (the last one negated) — the same wire shape, a `Comparison` whose `wholePeriod` is a `WholePeriod` over a non-set `AttributeReference` — and `synth-gehelejaarkenmerk` carries the characteristic variant |
| the two definitions in `dayKindDefinitions` | WF-031 | `jutterswet-schouwkalender` carries two pure definitions, replayed by gates 5–6 over its ordinary rule, and `jutterswet-schouwrooster` a second pair. The `dayKinds` **declarations** and (until L3) the day-kind registry stay here — WF-017 and WF-034 (s) read the declaration table, not the definitions, and their witnesses did not leave (the section below; gate 3 stays green with the definitions gone) |

Two things the shrink deliberately kept, each because a *staying* row needs
them:

- the `waterstand` attribute, `year`-granular on the `boekjaar` timeline.
  Nothing reads it now that the whole-period rules are gone, but the
  variable-start timeline is a staying witness and a timeline with no
  time-dependent member would make it a declaration about nothing.
- the `bewaakt` and `omheind` members of the composite. Under `all` they
  are no longer the "at least two of three" the old name counted, but the
  name still reads true — the peiling holds when the field is guarded *and*
  fenced *and* the window is open. The swap does narrow one thing: an
  executor reaches the `TimeWindow` only on an instance carrying both
  characteristics (S2 of the case below stops before it).

As of this shrink the WF index in `spec/IR-SPEC.md` still names this
cabinet in the Golden column of the rules that left. That file is a
protected path; the replacement cells are proposed in the shrinking card's
`Result:` note, for the maintainer to land.

## The case

`case1-cellen.json`, hand-computed, with `asOfDate` 2026-03-01:

| instance | input | `vakcijfer` | `zeecijfer` | `peiling` |
|---|---|---|---|---|
| S1 | `opbrengst` `voorjaar` 7, `najaar` 2; `aanspoeling` (`voorjaar`, `zee`) 4, (`voorjaar`, `voorjaar`) 9, (`najaar`, `voorjaar`) 1; `bewaakt`, `omheind` | 7 | 4 | 1 |
| S2 | `opbrengst` `najaar` 2; `aanspoeling` (`najaar`, `zee`) 3; `bewaakt` | leeg | leeg | leeg |

`zeecijfer` reads the cell (`voorjaar`, `zee`) and not (`voorjaar`,
`voorjaar`): label 1 is a label of `seizoen`, label 2 one of `herkomst`, so
the shared label `voorjaar` names a different cell at each position (the
section on WF-040's positional reading below). The `peiling` of S1 is the
window's verdict on 2026-03-01; S2 lacks `omheind`, so its composite stops
before the window. The case pins no input cell, since an expectation on one
holds on the input as well and the no-op guard (gate 7) would let it pass.

| executor | outcome (2026-11-02) |
|---|---|
| `ref` (gate 5) | `oracle: PASS (7 expectations)` |
| `clingo` (gate 6) | `clingo: PASS (7 expectations)` |
| Drools (gate 12) | `PLAYED 1`, both branches |

## What stays, and the measurement that keeps it here

| Stays | WF rows | Measured 2026-09-19 |
|---|---|---|
| `vakcijfer uit voorjaarsopbrengst`, the dimensioned `opbrengst`, the `seizoen` dimension; `zeecijfer uit voorjaarsaanspoeling`, the `aanspoeling` dimensioned by `seizoen` and `herkomst`, the `herkomst` dimension | WF-004, WF-014, WF-040 | Measured 2026-11-02: `jutterswet-seizoenvondst` executes the cell read over one dimension and `jutterswet-seizoenvouw` the fold over one; a cell over two dimensions, and one whose dimensions share a label, stands in no other golden, so the row stays |
| the variable-start `boekjaar` timeline, its `StartPointDetermination`, the `meetdatum` anchor and the `waterstand` member | WF-016, WF-032 | `jutterswet-penningjaar` is the corpus's other timeline golden, but its `het penningjaar` declares `variableStart: false`; this is the only GOLDEN with a non-empty `startPointDeterminations`, so both halves of the row are uncovered. (Two negative cases carried the node — `wf032-fixed-start-anchor` and `wf034-wees-attribuutdimensies`, the second removed at M1 — but a refusal pins no positive form) |
| the `TimeWindow` member of `peiling bij dubbele borging` | WF-042 | `"node": "TimeWindow"` occurred in no other GOLDEN at the shrink; the window executes since 2026-09-20 and `jutterswet-premievenster` carries it. Its host composite therefore stays, with the counting quantifier that *did* leave replaced by `all`. (`wf042-empty-window` carries the node on the refusing side, as `wf034-wees-attribuutdimensies` did until M1 removed it) |
| the two day kinds, each with its plural | WF-017 (positive side) | the section below. Until L3 the row also carried the four registry entries and WF-034 arm (s)'s identity half; both left the wire with the registry (same section) |

## A cell over two dimensions that share a label: WF-040's positional reading

The dimensions `seizoen` (`voorjaar`, `najaar`) and `herkomst` (`voorjaar`,
`zee`) share the label `voorjaar`, and the rule `zeecijfer uit
voorjaarsaanspoeling` reads the cell `voorjaar`, `zee` of `aanspoeling`,
dimensioned by `seizoen` and `herkomst` in that order. WF-040 reads a cell
positionally (`IR-SPEC.md` ch. 4.3): label 1 is a label of `seizoen`, label
2 one of `herkomst`, so the shared label is no ambiguity and the document is
verifier-clean. The verifier of main `b906f78ba` counted per dimension how
many labels of the cell belong to it and refused this document,
`WF-040.dimension-pin-count FAIL /rules/rules/1/versions/0/value: cell read
pins dimension 'herkomst' 2 times, must be exactly once` (measured
2026-11-02; mutant h of the dimension research). The refusing side is
`corpus/negative/wf040-cell-labels-reversed`.

## The day kinds and their plurals: WF-017's positive witness (since 2026-09-10)

WF-017 (`spec/IR-SPEC.md` ch. 2.9) holds a day kind's spellings — its `name`
and its `plural` — against the built-in calendar spellings and against every
other day kind's spellings. Its arms are refusals, and a refusal pins a
rejection and never a non-rejection, so what the rule ADMITS needs a positive
witness, and this is it. The cabinet declares two day kinds, each with its
plural:

    "dayKinds": [
      { "node": "DayKind", "name": "schouwdag", "plural": "schouwdagen" },
      { "node": "DayKind", "name": "stormvloeddag", "plural": "stormvloeddagen" }
    ]

No spelling is a built-in calendar unit and none collides with another day
kind's, so the document stays verifier-clean, which is the point. The two day
kinds are declared here but **defined** elsewhere since 2026-09-19: the
definitions left for `jutterswet-schouwkalender` in the shrink. WF-017 reads
the declarations, not the definitions, and gate 3 confirms it: the document is
verifier-clean with `dayKindDefinitions` empty.

**Why the day kinds are here — index hygiene, and that is the whole claim.**
The WF index names this cabinet in WF-017's Golden column, so this is the
document a reader of the index opens to see what the rule ADMITS. It is
deliberately not claimed that the cabinet is the only pin.

## Until L3: the registry and the two witnesses it carried

Until L3 of the v1 wire migration (the L3 card,
`backlog/done/20260924-l3-typesysteem.md`) the day kinds' spellings stood in
a separate registry beside the list of names, and this cabinet carried two
witnesses over it. Both left with the registry: since L3 a spelling hangs on
its own `DayKind` node, so it can name no day kind but its own. The
migration resolved every registry value to its day kind under the ch. 2.1
identity; a key equal to the name was the name itself, and the other key
became the plural. The registry as it stood:

    "dagsoorten": ["schouwdag", "stormvloeddag"],
    "dagsoortPerEenheid": {
      "schouwdag":       "SchouwDag",
      "schouwdagen":     "  schouwdag  ",
      "stormvloeddag":   "stormvloeddag",
      "stormvloeddagen": "stormvloeddag"
    }

**WF-034 arm (s)'s IDENTITY witness (2026-09-09).** The arm required every
registry value to name a declared day kind, compared under the ch. 2.1
identity — collapse whitespace, lower case. The first two values were not
byte-identical to the declaration (`SchouwDag`, `  schouwdag  `): one per half
of the identity, both resolving, so the document stayed verifier-clean.
Measured 2026-09-09, before these two entries: every registry value in all
four zones was byte-identical to its declaration, so replacing the arm's
case-insensitive comparison with a byte-exact one moved **0 of 555** documents
and gate 3 stayed green; with the two entries the same mutant took gate 3
down. With the registry the arm lost its object; since L3 no spelling in this
document differs from another only in case or whitespace.

**WF-017's key-space witness (2026-09-10).** Two arms ran over the registry's
KEYS: a key must not be a built-in calendar unit (arm (a)), and a key that IS
a declared day-kind name must bind to that same day kind (arm (b)). The four
entries showed a declared name bound to itself byte for byte
(`stormvloeddag`), one bound to itself only through the normalised identity
(`schouwdag` → `SchouwDag`), and a plural key binding to its own day kind
(`stormvloeddagen`, `schouwdagen`). Measured 2026-09-10 over 585 documents:
arm (b) compared byte-exactly reported `/model/dagsoortPerEenheid/schouwdag`
here and took gate 3 down; arm (b) comparing against the FIRST declared day
kind instead of the key's OWN reported `/model/dagsoortPerEenheid/stormvloeddag`
here and was caught by `corpus/golden/jutterswet-schouwrooster` as well. Since
L3 neither arm has a key to read; the refusing side of WF-017 is pinned by the
`wf017-*` cases of `corpus/negative/`.

Cost, then and now: the day kinds move no verdict and no report line
anywhere else.

## Reproduction

From the workspace root, with the ref runner built (`dune build ref`):

    _build/default/ref/bin/runner.exe corpus/golden/jutterswet-vormenkabinet/ir.json \
      corpus/golden/jutterswet-vormenkabinet/case1-cellen.json

gives `oracle: PASS (7 expectations)`, and the clingo runner on the same pair
`clingo: PASS (7 expectations)` (2026-11-02).

## When this cabinet shrinks

When one of the refused constructs becomes executable in **both**
executors, its witness moves into an executing golden with a replay case
and its rows leave this cabinet — the README's rule: the cabinet is
"scaffolding for the WF index, never a place for semantics". Applied for
the first time on 2026-09-19 (the table at the top of this NOTE). The rows
that stay execute in this document itself, which carries a case for that
reason; a row leaves when another golden carries its construct with a
case.
