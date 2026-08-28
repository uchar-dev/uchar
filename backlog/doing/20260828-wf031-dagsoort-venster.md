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
with the stratification chapter — the ladder design round); no executor
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
