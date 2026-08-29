# The day-kind validity window becomes WF-checked — a bounded rd_geldigheid is refused

Facts (recon 2026-08-28, verified read-only): rd_geldigheid on a
ResolvedDagsoort is a dead field EVERYWHERE — the v0 day-kind compiler
reads only the condition and never the validity (its own documentation
notes the validity is "typically ALTIJD"), ref touches resolved_dagsoorten
only to collect parameter refs from rd_voorwaarde (ref/lib/eval.ml:~2006)
and refuses the DAGSOORT predicate outright (eval.ml:~1089 "predicate kind
not implemented"), and clingo faults it at the condition catch-all
(translate.ml catch-all "condition kind not translated"). A bounded window
on the wire would therefore be silently dead in every executor. Maintainer
decision (2026-08-28, numbered question): FORBID it — a day-kind
declaration MUST carry the ALTIJD validity (vanaf and tot both null),
machine-checked.

Build, narrowest form:
(1) spec/verifier/lib/checks.ml: extend the EXISTING day-kind rule WF-031
    (one definition per day kind; condition present) with the validity
    clause: rd_geldigheid MUST be ALTIJD; a bounded window (vanaf or tot
    non-null) is rejected citing WF-031. No new WF number. Follow the
    existing WF-031 check's structure and message style.
(2) spec/IR-SPEC.md (protected — the branch is the proposal, merged after
    the lens round under the standing mandate): the WF-031 rule text gains
    the validity clause, and the empty §3.7 dagsoort-table validity row
    ("validity | Validity | —") is filled with the normative statement and
    its ground: no executor consults a bounded day-kind window — the v0
    semantics never reads it — so the IR refuses what nothing can honour.
    SELF-CONTAINED prose: no toolchain paths, no repo names.
(3) corpus/negatief/wf031-dagsoort-venster/: a schema-valid ir.json whose
    ResolvedDagsoort carries a bounded geldigheid (e.g. vanaf non-null),
    rejected by gate 3 citing WF-031 (directory name declares the rule).
    A short NOTE.md.
(4) The passing witness already exists: corpus/golden/jutterswet-vormenkabinet
    carries a ResolvedDagsoort with the ALTIJD validity and MUST stay
    verifier-clean — prove it in the gate-3 run.
(5) spec/ASP-TRANSLATION.md S19: ONE sentence repairing the "complete loud
    inventory" completeness — the day-kind (DAGSOORT) predicate itself is a
    loud non-translation in clingo (the condition catch-all) as it is a
    loud refusal in ref; S19 stays the highest section; nothing else in
    S19 moves. No toka IDs.
NOT in this card: the recursion/stratification question for day-kind
conditions stays OPEN exactly as WF-031's delta note records it (decided
with the stratification chapter — the defaults design round); no executor
changes; no window semantics.

Acceptance:
- Red-first: BEFORE the change, the bounded-window negatief ir.json passes
  the verifier (real output quoted); AFTER, it is rejected citing WF-031;
  gate 3 green including it (negatief count rises by one).
- jutterswet-vormenkabinet still verifies clean; zero regression over the
  corpus (no golden/rood carries a bounded day-kind window — verify and
  state the count).
- All twelve gates green in the worktree.
May touch: spec/verifier/**, spec/IR-SPEC.md (WF-031 text + §3.7 validity
row only), corpus/negatief/** (the new case only), spec/ASP-TRANSLATION.md
(the one S19 sentence), backlog/.
Out of scope: ref/, backends/**, frontends/**, corpus/golden/**,
corpus/rood/**, spec/schema/ (the wire stays v0: geldigheid stays on the
node), spec/RUNTIME-SEMANTICS.md, toka/**, scripts/**, spec/CONSTRUCTS.md,
spec/PLAN.md, the recursion decision, any toolchain commit.

Werkregels (bindend):
- Werk UITSLUITEND in de worktree /Users/yeb/Developer/uchar-dev/wt/wf031-dagsoort-venster (branch card/wf031-dagsoort-venster, basis main 63f2271 — de WF-034-kaart is zojuist geland; checks.ml draagt de verse typ:null-arm). Root-checkout /Users/yeb/Developer/uchar-dev/uchar NIET aanraken (behalve jars kopiëren voor gates). Geen merges, geen push.
- Eerste commit: maak backlog/doing/20260828-wf031-dagsoort-venster.md met de kaarttekst hierboven (verbatim). Na een heredoc-append altijd expliciet git add in dezelfde &&-keten.
- RECHTENREGEL: geen ©-TOKA-inhoud; de negatief-case is eigen minimaal materiaal; spec-tekst zelfdragend zonder toolchain-verwijzingen.
- Rood-eerst is de kern: demonstreer met echte verifier-output dat het venster vóór de fix stil passeert.
- OCaml: eval $(opam env); dune build vanaf de workspace-root. Gates: ALLE twaalf; voor 10/11 kopieer frontends/regelspraak/lib/*.jar uit /Users/yeb/Developer/uchar-dev/uchar/frontends/regelspraak/lib/; voor 12: mvn -q -o -f backends/drools/pom.xml package.
- Smalste wijziging: alleen de venster-clausule; de recursie-OPEN-notitie in IR-SPEC blijft letterlijk staan.
- Rapporteer compact: rood-eerst-bewijs, gate-uitkomsten, diff-stat, wat bewust bleef liggen.

Lees eerst de bestaande WF-031-check in spec/verifier/lib/checks.ml en de WF-031/§3.7-tekst in spec/IR-SPEC.md, corpus/golden/jutterswet-vormenkabinet/ir.json (de dagsoort-dragende golden), een bestaande negatief-case als vormvoorbeeld, en scripts/check-verifier.sh. Bouw daarna de kaart volledig.

## Result

Closed 2026-08-28. The verifier fence landed; all twelve gates are green in
the worktree. NO merge, NO push — the maintainer lands the protected-path
proposal (`spec/verifier/**`, `spec/IR-SPEC.md`) after the lens round.

### The change (narrowest)
`spec/verifier/lib/checks.ml`, `wf031`, the per-entry arm only: a
ResolvedDagsoort whose `rd_geldigheid` bounds the window (`geld_vanaf` or
`geld_tot` non-null) is now refused as WF-031 ("day-kind definition with a
bounded validity window; must be ALTIJD (vanaf and tot both null)"),
alongside the existing missing-condition, duplicate and undeclared clauses.
No new WF number. `spec/IR-SPEC.md`: WF-031 prose gains the validity
clause, the §3.7 dagsoort-table `validity` row is filled with the normative
statement and its ground, and the WF-index row for WF-031 is kept coherent
(new witness `wf031-dagsoort-venster`). The recursion OPEN note is left
verbatim. `spec/ASP-TRANSLATION.md` S19: one sentence records the day-kind
(DAGSOORT) predicate itself as a loud non-translation in clingo (the
condition catch-all) and a loud refusal in ref. The v0 schema is untouched:
`geldigheid` stays on the ResolvedDagsoort node (mirror-then-diverge; the
verifier is the fence).

### Red-first proof (verifier output, reproduced first-hand this round)
New negatief case `corpus/negatief/wf031-dagsoort-venster/` — a
ResolvedDagsoort (`schouwdag`) with a present condition but
`geldigheid.vanaf = "2026-01-01"`, so the bounded window is the sole
defect. Schema-valid and round-trips (gate 2 explicit: PASS, 1 file).
- BEFORE (base `checks.ml`, rebuilt): `verifier: PASS` (rc=0) — the gap: a
  bounded day-kind window passes silently.
- AFTER (this branch): `WF-031 FAIL /resolvedDagsoorten/0: day-kind
  definition with a bounded validity window; must be ALTIJD (vanaf and tot
  both null)` (rc=1).
Gate 3 green including it: 152 golden clean, 163 negatief rejected for their
declared rule (count rose by one, 162 → 163).

### Passing witness + zero regression
`corpus/golden/jutterswet-vormenkabinet` (its `schouwdag` ResolvedDagsoort
carries the ALTIJD validity, vanaf/tot both null) stays verifier-clean —
proven in the gate-3 golden sweep. Corpus scan over golden+rood: 1
ResolvedDagsoort total, 0 bounded windows — no golden/rood carries a
bounded day-kind window, so the fence adds no regression.

### Twelve gates — all green in the worktree
1 self-contained PASS · 2 ir-schema PASS (152) · 3 verifier PASS (152 golden
/ 163 negatief) · 4 case-schema PASS (379) · 5 oracle/ref PASS (357) · 6
clingo PASS (357) · 7 noop PASS (324/33) · 8 rood PASS (22) · 9 catala PASS
(169 checks; 11 goldens) · 10 regelspraak PASS (130 tests; 19 rgs) · 11
toka-parity PASS (30 replay) · 12 drools PASS (53 played; 72 auto-excluded;
4 value-divergence; 1 no-case).

### Review round (fix-and-close, 2026-08-28)
Two adversarial lenses ran the diff and reproduced red-first; both found **no
CRITICAL and no MAJOR** and reported merge-ready. Two minors, handled:
- *Scope — the WF-index row (`spec/IR-SPEC.md`) is edited alongside the two
  enumerated spots.* **Motivated keep.** The WF-index table is the spec's
  WF → witness map; this card adds the negatief witness
  `wf031-dagsoort-venster`, and leaving the index without it would itself be
  a stale-index defect. The edit stays wholly inside the IR-SPEC.md zone and
  is coherence-preserving.
- *NOTE.md did not record that `geldigheid` is a required v0 field, so ALTIJD
  is the explicit `vanaf: null, tot: null` object (an omitted field is a load
  error, not ALTIJD).* **Processed.** One clarifying sentence added to
  `corpus/negatief/wf031-dagsoort-venster/NOTE.md`; schema confirms
  `ResolvedDagsoort.geldigheid` is required. No gate consumes NOTE.md content,
  so no gate is affected.

All twelve gates were re-run this round (opam env; frontend jars in place;
Drools uber-jar rebuilt via `mvn -o … package`) and are green with the counts
above. Red-first was reproduced first-hand this round: base `checks.ml`
`verifier: PASS` (rc=0) on the bounded-window case, branch `checks.ml`
`WF-031 FAIL … must be ALTIJD (vanaf and tot both null)` (rc=1); the tree was
restored byte-identical after the base-build probe.

### Deliberately left open (per card)
The recursion/stratification question for day-kind conditions stays **OPEN**
exactly as WF-031's delta note records it — decided with the stratification
chapter (the defaults design round). No executor, schema, or window-semantics
change: the wire stays v0 (`geldigheid` stays on the node; the verifier is
the fence).

### Diff (5 artifact files + this card)
`backlog/` (this card), `corpus/negatief/wf031-dagsoort-venster/` (NOTE.md +
ir.json), `spec/IR-SPEC.md` (WF-031 prose + §3.7 validity row + WF-index
row), `spec/verifier/lib/checks.ml` (the validity arm), `spec/ASP-TRANSLATION.md`
(one S19 sentence). No out-of-scope path touched.
