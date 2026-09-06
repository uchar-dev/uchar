Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (p)): a **side-table key** that
names a declared member but does not SPELL it — `Borgplichtig` where `Jutter`
declares `borgplichtig`. A one-character diff.

    WF-034 FAIL /objecttypen/Jutter/kenmerkGranulariteit/Borgplichtig:
    kenmerkGranulariteit key 'Borgplichtig' names no declared characteristic of
    'Jutter': the side table resolves against no declaration

**Why this is not a liberty.** A side-table key is not a site where one name is
matched against another: it is the v0 **denormalisation** of a facet v1 carries
on the member node itself (`spec/CONSTRUCTS.md` row 11 — "each v0 map entry
folds into the corresponding member node"), so the key IS that member's
identity, written out beside the declaration instead of on it. Ch. 2.1 has two
rules and this is the first: declared names are **carried verbatim**
(whitespace-collapsed, case preserved), and separately, name *matching* is
case-insensitive. The sixth wave landed (p) under the matching rule; this
witness is why it is the carriage rule.

**The consumers disagree, and that is the whole point** (measured 2026-09-06,
per consumer, on this exact document's golden). The verifier, `ref` and the ASP
backend resolve a side-table key case-insensitively. The bean-model executor
does not: it reaches these maps through raw case-sensitive map lookups at
**108** sites across four of the eight tables (`attribuutGranulariteit` 64,
`kenmerkGranulariteit` 20 — this one — `attribuutTijdlijnAantal` 17,
`kenmerkTijdlijnAantal` 7), against five case-insensitive lookups through its
own name helper. So under a case-insensitive rule this document is **not** an
orphan and stays clean everywhere, while one executor quietly stops seeing the
characteristic as temporal.

**Measured, one consumer at a time**, with `corpus/golden/synth2-clip-over-prorata`
carrying the same one-character change:

| consumer | before | after |
| --- | --- | --- |
| verifier (before this fix round) | PASS | **PASS** |
| `ref` (`case1`) | PASS, 3 expectations | **PASS, 3 expectations** |
| ASP backend (`case1`) | PASS, 3 expectations | **PASS, 3 expectations** |
| bean-model executor (`case1`) | PLAYED | **EXCLUDE** — a transpiler skip, the assignment no longer temporal |

The consequence at the gate: the golden leaves the differential and gate 12
reports **PASS** anyway. Its played count falls from 109 to 108, and its only
floor is a **non-empty** played set (`scripts/check-drools.sh:197`) — no
counter, no expected-play list, nothing that notices one golden fewer. **The
wire has to catch what no gate catches**, which is what this document is for.
(The floor itself is a separate proposal,
`backlog/new/20260906-gate-12-speelvloer.md`.)

*Measured breadth:* zero documents lose anything. Of the **943** side-table
keys the four zones held before this fix round, **zero** differ from their
declared member in byte while resolving case-insensitively — and zero of the
**924** they held before the sixth wave. The tightening rejects nothing that
was there; this is its only witness.

*Why this table and this document:* `kenmerkGranulariteit` is the most heavily
case-sensitively read of the three characteristic tables, and the capitalisation
flank had no corpus witness at all before this one. The eight
`wf034-wees-<table>` cases cover the orphan that names **nothing**; this covers
the key that names the right member in the wrong spelling.

A hand-broken variant of `corpus/golden/synth2-clip-over-prorata/ir.json` — one
key recapitalised, a one-line diff; schema-valid, since the eight tables are
`additionalProperties`-typed maps on the v0 wire.

Falsifiable, arm-exact: give the `kenmerkGranulariteit` call site in
`wf034_zijtabelsleutels` (`spec/verifier/lib/checks.ml`) its case-insensitive
predicate back — `has_kenmerk ot` for the byte-exact `k` — and this case, alone
in all four zones, turns clean: 523 documents compared, one verdict moved, no
report elsewhere.
