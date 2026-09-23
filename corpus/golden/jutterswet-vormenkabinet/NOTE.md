# jutterswet-vormenkabinet — no replay case, by design

This golden is the **verifier-only witness cabinet** (see the "witness
cabinet" section of `corpus/golden/README.md`). It exists to witness,
positively and verifier-clean, the well-formedness rules whose constructs
no *executing* corpus document carries yet. After the 2026-09-19 shrink
(the next section) that is WF-004/014/040 (the dimensioned attribute and
its cell read), WF-016/032 (the variable-start timeline with its start
point), WF-042 (the time window — **eligible to leave since 2026-09-20**,
see below: the window now executes, and `jutterswet-premievenster` is its
successor), plus the two registry rules the sections
near the end carry: WF-017's positive binding and WF-034 arm (s)'s identity
half. It deliberately carries no `case*.json`, so gate 5 (the oracle) lists
it as "NOT replayed, no case file yet". This NOTE records *why* that is a
true report and not a gap to be filled with a manufactured case, and the
exact condition under which it becomes one.

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
| `staat na schouw`, with the `kwaliteit` domain and the `staat` attribute | WF-006, WF-039 (enum-label arm), the enum slice of WF-023 | `jutterswet-keurstaat`: it declares the domain `keurklasse`, and its `stempel voor een veilbare vondst` writes the label `gaaf` into an `Enumeratie`-typed target — the same `GELIJKSTELLING`-with-enum-`Literal` shape this rule had. `jutterswet-vaarwatertoeslag-catala` carries a second one |
| `aanwas na schouw`, with the `aanwas` attribute | WF-039 (sign arm) | `anchor/jutterswet-taxatie` (the literals `2` and `3`, each typed `NIET_NEGATIEF`), `anchor/jutterswet-aanspraaktelling` (`0.00`), `anchor/jutterswet-hardheid`, `anchor/jutterswet-vondstdossier` (`120.50`, `3`), `jutterswet-postenheffing`, `synth3-vondststaat`. Difference recorded rather than glossed: those carry the sign-restricted `Literal` as an **operand**, where this rule carried it at the write position. WF-039 reads the literal's own type wherever `walk_expr` reaches it, so it is the same node under the same clause — no corpus document outside this cabinet had it at a rule's `waarde` |
| the counting quantifier of `peiling bij dubbele borging` — `TEN_MINSTE` with threshold 2, now `ALLE` with 0 | WF-041 | `jutterswet-wachtploeg` carries `TEN_MINSTE` with threshold 2 on the composite host, the same form this rule had. Eight further composite goldens carry the other quantors and thresholds (`paasvondst`, `rubriektabel`, `seinwacht`, `sluimerwacht`, `tweevoudkeur`, `vacuumwacht`, `vrijgavetoets`, `wachtdeling`), and `jutterswet-reddingsvloot` carries the fold host of WF-108 (e). The rule itself stays: its `Periode` member is a row that did not leave — *and that is no longer true as of 2026-09-20, see "The second rule no longer refuses anything" below* |
| the two dated versions of `doorwaadbaar bij lage stand` (`#1`, `#2`) | WF-021 | `anchor/jutterswet-jutheffing` — `jutheffing naar jaartarief #1`/`#2` is the only other corpus document whose rule names group to one name under the version strip WF-021 applies, with non-overlapping windows replayed per window |
| the whole-year qualifier on those same two rules, with the `waterstand` read and the `doorwaadbaar` characteristic | WF-043 | `jutterswet-keurploeg`, `jutterswet-ploegpremie`, `jutterswet-reddingsvloot` and `jutterswet-sleepvloot` each compare a `MAAND`-granular attribute under a whole-year qualifier (the last one negated) — the same wire shape, a `Vergelijking` whose `periode` is a `GeheelPeriode` over a non-set `AttribuutRef` — and `synth-gehelejaarkenmerk` carries the characteristic variant |
| the two `resolvedDagsoorten` definitions | WF-031 | `jutterswet-schouwkalender` carries two pure definitions, replayed by gates 5–6 over its ordinary rule, and `jutterswet-schouwrooster` a second pair. The `dagsoorten` **declarations** and the `dagsoortPerEenheid` registry stay here — WF-017 and WF-034 (s) read the declaration table, not the definitions, and their witnesses did not leave (the two sections below; gate 3 stays green with the definitions gone) |

Two things the shrink deliberately kept, each because a *staying* row needs
them:

- the `waterstand` attribute, `JAAR`-granular on the `boekjaar` timeline.
  Nothing reads it now that the whole-period rules are gone, but the
  variable-start timeline is a staying witness and a timeline with no
  time-dependent member would make it a declaration about nothing.
- the `bewaakt` and `omheind` members of the composite. Under `ALLE` they
  are no longer the "at least two of three" the old name counted, but the
  name still reads true — the peiling holds when the field is guarded *and*
  fenced *and* the window is open. The swap does narrow one thing, and it
  is recorded under "Why no replay case" below: `ref` now reaches the
  `Periode` only on an instance carrying both characteristics.

As of this shrink the WF index in `spec/IR-SPEC.md` still names this
cabinet in the Golden column of the rules that left. That file is a
protected path; the replacement cells are proposed in the shrinking card's
`Result:` note, for the maintainer to land.

## Why no replay case

The oracle (`ref/`) and the differential backend (`backends/clingo/`)
replay a case by evaluating the **whole** document to its fixpoint and then
checking the pinned expectations — there is no per-rule replay. Both rules
this cabinet still carries exercise constructs the two executors
deliberately refuse (they fault with context rather than guess — the
fail-fast discipline of `ref/lib/eval.ml` and
`backends/clingo/lib/translate.ml`), so any case over this IR faults before
a single expectation can be checked:

| Rule | Construct (WF row) | Fault the reference interpreter raises |
|---|---|---|
| `vakcijfer uit voorjaarsopbrengst` | dimension-cell read of the dimensioned `opbrengst` (WF-004/014/040) | `expression kind not implemented in this card` |
| `peiling bij dubbele borging` | composite condition with a time-window (`Periode`) member (WF-042) | **none any more** — the window became executable on 2026-09-20, see the section below |

The ASP backend mirrors these same refusals by construction
(`backends/clingo/lib/translate.ml`: everything outside the
corpus-exercised subset faults with context), so neither executor can
replay this document. Since 2026-09-19 the document holds no rule that
runs to a value in `ref`: the two that did — `staat na schouw` and
`aanwas na schouw` — are exactly the two that left for their executing
successors.

**The second rule no longer refuses anything (2026-09-20).** Its `Periode`
member was the cabinet's WF-042 witness precisely because no executor
carried the construct. That changed with the card
`ref-periodeconditie-arm`: `RUNTIME-SEMANTICS.md` ch. 3.10 specifies the
scalar target's evaluation-date gate in six clauses with a worked example,
and `ref` and the ASP backend now both implement it. On this document the
rule simply evaluates — `ref` reads the composite member by member and
stops at the first false one, so a `Schouwveld` without both
characteristics never reaches the window at all, and one carrying both
gets the window's own verdict against the case's `asOfDate`.

**So this row is eligible to leave, under the cabinet's own admission rule
at the bottom of this NOTE**, and its executing successor exists:
`corpus/golden/jutterswet-premievenster` carries the window in both wire
positions — as a rule's whole condition (five rules, one per spelling) and
as a member of an `ALLE` composite beside a characteristic check, which is
exactly this rule's shape. Six cases, all four executors, the six ch. 3.10
clauses pinned one by one. Moving the row is a shrink, and a shrink is its
own card: the 2026-09-19 one above shows what it costs to do carefully
(which declarations a staying row still needs, which WF index cells move).
Recorded here rather than done halfway.

Until that shrink the document still cannot be replayed, and the reason is
now the FIRST rule alone: its dimension-cell read faults on any instance at
all, in `ref` and in the ASP backend alike. `clingo` translates every rule
body statically, so it reaches that refusal without needing a subject.

A case with zero `Schouwveld` instances does not even evade the faults
cleanly. The reference interpreter is lazy per-instance: with no subject no
rule body runs, so `ref` reports `oracle: PASS`. But the ASP backend
translates every rule body **statically**, independent of instance count, so
`clingo` still faults —
`TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)`
— and gate 6 would turn **red**, not merely pin nothing. (Re-measured
2026-09-19 on the shrunken document: unchanged.) Either way the case
exercises and pins nothing real: it is exactly the manufactured "make the
report go away" case this cabinet must not carry. Executable expectations
belong to executing goldens, not to a scaffold.

## What stays, and the measurement that keeps it here

| Stays | WF rows | Measured 2026-09-19 |
|---|---|---|
| `vakcijfer uit voorjaarsopbrengst`, the dimensioned `opbrengst`, the `seizoen` dimension | WF-004, WF-014, WF-040 | The only other corpus document carrying a `Dimensie` or a `DimensieCel` is `jutterswet-seizoenvondst`, and that golden carries **no case file either** — it is the second verifier-only golden (`corpus/golden/README.md`). No *executing* document carries the form, so the row stays. The shrinking card's 2026-09-17 reading that `jutterswet-seizoenvondst` covers it had measured the construct without measuring the execution |
| the variable-start `boekjaar` timeline, its `ResolvedStartpunt`, the `meetdatum` anchor and the `waterstand` member | WF-016, WF-032 | `jutterswet-penningjaar` is the corpus's other timeline golden, but its `het penningjaar` declares `variabelStartpunt: false`; this is the only GOLDEN with a non-empty `resolvedStartpunten`, so both halves of the row are uncovered. (Two negatief cases carried the node — `wf032-fixed-start-anchor` and `wf034-wees-attribuutdimensies`, the second removed at M1 — but a refusal pins no positive form) |
| the `Periode` member of `peiling bij dubbele borging` | WF-042 | `"node": "Periode"` occurs in no other GOLDEN, and both executors still refuse it (the table above). Its host composite therefore stays, with the counting quantifier that *did* leave replaced by `ALLE`. (`wf042-empty-window` carries the node on the refusing side, as `wf034-wees-attribuutdimensies` did until M1 removed it) |
| the `dagsoorten` declarations and the four `dagsoortPerEenheid` entries | WF-017 (positive side), WF-034 arm (s) (identity half) | the two sections below; measured across all corpus zones, the non-byte-identical registry values live here and in one negatief case only |

## The day-kind unit registry: WF-034 (s)'s IDENTITY witness (2026-09-09)

Beside the rules above, this cabinet carries the **name identity** half of
WF-034 arm (s) (`spec/IR-SPEC.md` ch. 4.6): *every value of
`dagsoortPerEenheid` must name a day kind `dagsoorten` declares*, compared
under the ch. 2.1 identity — collapse whitespace, lower case.

The document declares the day kind `schouwdag` and registers two spellings
for it, and **neither value is byte-identical to the declaration** — they
are the first two entries of the map (the last two, and the second day kind
they belong to, arrived with the WF-017 section below):

    "dagsoortPerEenheid": {
      "schouwdag":       "SchouwDag",
      "schouwdagen":     "  schouwdag  ",
      "stormvloeddag":   "stormvloeddag",
      "stormvloeddagen": "stormvloeddag"
    }

One entry per half of the identity: capitalisation, and collapsible whitespace.
Both resolve, so the document stays verifier-clean, and that is the point —
this is a POSITIVE witness, which is why it belongs in the cabinet and not in
`corpus/negatief/`.

**Why it had to exist.** Measured 2026-09-09, before these two entries: every
`dagsoortPerEenheid` value in all four zones was byte-identical to its
declaration, so replacing the arm's case-insensitive comparison with a
byte-exact one moved **0 of 555** documents and gate 3 stayed green. The words
"trimmed and case-insensitive" in the rule were unfalsifiable. With these two
entries the same mutant reports both of them here and takes **gate 3 down**:

    WF-034 FAIL /dagsoortPerEenheid/schouwdag: day-kind unit 'schouwdag' names
    day kind 'SchouwDag', which no declaration carries: …
    WF-034 FAIL /dagsoortPerEenheid/schouwdagen: day-kind unit 'schouwdagen'
    names day kind '  schouwdag  ', which no declaration carries: …

Re-measured 2026-09-19 over the golden, negatief and rood zones: the only
registry values that are not byte-identical to a declaration are these two
and the one in `corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort`,
which is the refusing side. The witness is still this document's alone.

The RESOLUTION half of the same arm is pinned from the other side by
`corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort`.

**Why here and not in a golden of its own.** The witness needs no executor: it
pins a verifier comparison. A new golden would have arrived in the
differential gate's channel floor and would have had to be pinned there by
name; this cabinet is already pinned `NOCASE` and already exists for exactly
this purpose, so the witness costs no executor channel and no pin. It also
changes no rule of this document — the registry is read by no rule here, and
the four consumers' behaviour on this IR is unchanged.

## The registry KEY space: WF-017's positive witness (2026-09-10)

WF-017 (ch. 2.9) gained two arms over the same map's KEYS: a key must not be a
built-in calendar unit (arm (a)), and a key that IS a declared day-kind name
must bind to that same day kind (arm (b)). Both are refusals; a refusal pins a
rejection and never a non-rejection, so the good binding needs a positive
witness, and this is it.

The cabinet declared exactly ONE day kind, so the only positive fact it could
show was that one declaration's key resolving to itself. It now declares a
second, and registers both spellings of each:

    "dagsoorten": ["schouwdag", "stormvloeddag"],
    "dagsoortPerEenheid": {
      "schouwdag":       "SchouwDag",
      "schouwdagen":     "  schouwdag  ",
      "stormvloeddag":   "stormvloeddag",
      "stormvloeddagen": "stormvloeddag"
    }

Three positive facts in four entries: a declared name bound to itself byte for
byte (`stormvloeddag`), a declared name bound to itself only through the
normalised identity (`schouwdag` → `SchouwDag`), and a PLURAL key that binds to
its own day kind and collides with no other declaration (`stormvloeddagen`,
`schouwdagen`). No key is a built-in calendar unit, so arm (a) is silent too.
The document stays verifier-clean, which is the point.

Since 2026-09-19 these two day kinds are declared and registered here but
**defined** elsewhere: the definitions left for `jutterswet-schouwkalender`
in the shrink. Both rules read the declaration table — WF-017 compares a key
against declared names, WF-034 (s) resolves a value against them — so
neither witness depends on a definition standing beside it, and gate 3
confirms it: the document is verifier-clean with `resolvedDagsoorten` empty.

**Why the entries are here — index hygiene, and that is the whole claim.** The
WF index names this cabinet in WF-017's Golden column, so this is the document a
reader of the index opens to see what the rule ADMITS. Before these entries it
could not show the arm's positive form at all. It is deliberately not claimed
that the cabinet is the only pin: the two mutants below are each caught
elsewhere as well.

Measured 2026-09-10 over 585 documents (four zones plus the read-only parity
zone):

- arm (b) compared byte-exactly instead of under the ch. 2.1 identity reports
  `/dagsoortPerEenheid/schouwdag` here and takes gate 3 down — but on the
  document's PRE-CARD state too, since that entry came in with the WF-034
  section above;
- arm (b) comparing against the FIRST declared day kind instead of the key's OWN
  reports `/dagsoortPerEenheid/stormvloeddag` here, and is caught by
  `corpus/golden/jutterswet-schouwrooster` as well.

The refusing side is pinned from the other end by
`corpus/negatief/wf017-eenheidsleutel-schaduwt-kalendereenheid` (arm (a)) and
`corpus/negatief/wf017-dagsoortnaam-bindt-vreemde-dagsoort` (arm (b)).

Cost, same sweep: adding the second day kind moves no verdict and no report line
anywhere — the four consumers read this document exactly as before, and the
cabinet still carries no `case*.json`, so gate 5 keeps listing it as "NOT
replayed" and gate 12 keeps it `NOCASE`.

## Reproduction

From the workspace root, with the ref runner built (`dune build ref`), the
runner on this `ir.json` and any case carrying one `Schouwveld` instance
faults with `expression kind not implemented in this card` — the
dimension-cell read of the first rule, which needs nothing of the instance.

The second rule no longer contributes a refusal at all. Filtering
`resolvedRegels` down to it (for example with `jq`) and replaying a
`Schouwveld` gives `oracle: PASS` on every instance and every `asOfDate`:
the window is evaluated, not refused. Before 2026-09-20 an instance
carrying **both** `bewaakt` and `omheind` faulted here with the residual
`condition kind not implemented in this card`, and that is the measurement
the section above replaces.

All of the above re-measured 2026-09-19 on the shrunken document. The
validity-window wrinkle this section used to record went out with the two
`doorwaadbaar bij lage stand` rules; it now reproduces on
`anchor/jutterswet-jutheffing`, where those windows execute.

## When this cabinet shrinks

When one of the refused constructs becomes executable in **both**
executors, its witness moves into an executing golden with a replay case
and its rows leave this cabinet — the README's rule: the cabinet is
"scaffolding for the WF index, never a place for semantics". Applied for
the first time on 2026-09-19 (the table at the top of this NOTE); until the
next construct becomes executable the case-less state is the correct one,
and gate 5's "NOT replayed" line for this directory is a true, motivated
report — not a coverage hole.
