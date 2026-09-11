# jutterswet-vormenkabinet — no replay case, by design

This golden is the **verifier-only witness cabinet** (see the "witness
cabinet" section of `corpus/golden/README.md`). It exists to witness,
positively and verifier-clean, the well-formedness rules whose constructs
no *executing* corpus document carries yet — closing the Golden column of
the WF index for WF-004/006/014/016/017/021/031/032/039/040/041/042/043
(and the enum-assignment slice of WF-023, and since 2026-09-09 the name
identity of WF-034 arm (s) — the section below). It deliberately carries no
`case*.json`, so gate 5 (the oracle) lists it as "NOT replayed, no case
file yet". This NOTE records *why* that is a true report and not a gap to
be filled with a manufactured case, and the exact condition under which it
becomes one.

## Why no replay case

The oracle (`ref/`) and the differential backend (`backends/clingo/`)
replay a case by evaluating the **whole** document to its fixpoint and then
checking the pinned expectations — there is no per-rule replay. Three of
this cabinet's rules exercise constructs both executors deliberately refuse
(they fault with context rather than guess — the fail-fast discipline of
`ref/lib/eval.ml` and `backends/clingo/lib/translate.ml`), so any case over
this IR faults before a single expectation can be checked:

| Rule | Construct (WF row) | Fault the reference interpreter raises |
|---|---|---|
| `vakcijfer uit voorjaarsopbrengst` | dimension-cell read of the dimensioned `opbrengst` (WF-004/014/040) | `expression kind not implemented in this card` |
| `peiling bij dubbele borging` | counting quantifier with a time-window (`Periode`) member (WF-041/042) | `condition kind not implemented in this card` |
| `doorwaadbaar bij lage stand #1`/`#2` | whole-period comparison over the temporal `waterstand` (WF-043) | `whole-period comparison is ch. 3.9 scope, not executed here` |

The ASP backend mirrors these same refusals by construction
(`backends/clingo/lib/translate.ml`: everything outside the
corpus-exercised subset faults with context), so neither executor can
replay this document.

The two rules that *are* individually executable — `staat na schouw`
(enum-label assignment) and `aanwas na schouw` (sign-restricted literal) —
cannot be replayed on their own: the evaluator runs every `resolvedRegel`,
so the three refusing rules bring the whole replay down first.

A case with zero `Schouwveld` instances does not even evade the faults
cleanly. The reference interpreter is lazy per-instance: with no subject no
rule body runs, so `ref` reports `oracle: PASS`. But the ASP backend
translates every rule body **statically**, independent of instance count, so
`clingo` still faults —
`TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)`
— and gate 6 would turn **red**, not merely pin nothing. Either way the case
exercises and pins nothing real: it is exactly the manufactured "make the
report go away" case this cabinet must not carry. Executable expectations
belong to executing goldens, not to a scaffold.

## The day-kind unit registry: WF-034 (s)'s IDENTITY witness (2026-09-09)

Beside the rules above, this cabinet now carries the **name identity** half of
WF-034 arm (s) (`spec/IR-SPEC.md` ch. 4.6): *every value of
`dagsoortPerEenheid` must name a day kind `dagsoorten` declares*, compared
under the ch. 2.1 identity — collapse whitespace, lower case.

The document already declared the day kind `schouwdag` and registered no
spelling for it. It now registers two for that day kind, and **neither value is
byte-identical to the declaration** — they are the first two entries of the map
as it stands today (the last two, and the second day kind they belong to,
arrived with the WF-017 section below):

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
second, with its own definition and both its spellings registered:

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
dimension-cell read of the first rule. Filtering `resolvedRegels` down to a
single blocking rule in turn (for example with `jq`) reproduces the other
faults in the table above — with one wrinkle for the temporal row:

- `peiling bij dubbele borging` alone faults with
  `condition kind not implemented in this card`, at any `asOfDate`
  (including a null one).
- The two `doorwaadbaar bij lage stand` rules carry validity bounds
  (`#1 tot 2025-12-31`, `#2 vanaf 2026-01-01`), so filtering to them with a
  null `asOfDate` faults *first* with `a validity-bounded form needs an
  evaluation date; this session is time-free` — not the whole-period fault.
  The whole-period-comparison refusal (`whole-period comparison is
  ch. 3.9 scope, not executed here`) surfaces only once the case also
  carries an `asOfDate` inside a rule's window — for example `#2` with
  `asOfDate 2026-06-01`.

## When this cabinet shrinks

When one of the refused constructs becomes executable in **both**
executors, its witness moves into an executing golden with a replay case
and its rows leave this cabinet — the README's rule: the cabinet is
"scaffolding for the WF index, never a place for semantics". Until then the
case-less state is the correct one, and gate 5's "NOT replayed" line for
this directory is a true, motivated report — not a coverage hole.
