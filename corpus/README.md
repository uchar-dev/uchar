# The uchar corpus

The executable test material for the spec, in three zones: golden cases
(source → IR → expected outcome), deliberately ill-formed negatief
documents, and rood cases that must fail in every executor. The corpus
ships with the spec and inherits its obligations: **rights-free by
construction**.

## Rights policy

- Casus material is own-authored (the Jutterswet, `jutterswet/`) or built on
  statute text, which is copyright-free under art. 11 Auteurswet.
- No ©-Belastingdienst material enters the corpus: no TOKA casus, no
  RegelSpraak specification excerpts. Programs *written in* RegelSpraak are
  ours; the language specification text is not. The repository's one
  exception is the top-level `toka/` parity zone — temporary, outside
  `corpus/`, and the reason the repository stays private (see the root
  README and `CLAUDE.md`). Gate 1 checks that every corpus zone stays
  TOKA-free.

## Layout

| Path | Contents |
|---|---|
| `jutterswet/` | The casus source material: statute text, dossiers, coverage roadmap |
| `golden/` | One directory per golden: `ir.json`, one or more `case*.json` with expected values, a `NOTE.md` with the hand computation, and — where a frontend carries the form — a source file (`bron.rgs`, `bron.catala_en` or `bron.flogic`) that must re-emit the committed IR byte-identically (gates 9, 10, 14); beside a `bron.rgs` optionally a `scenario.rgs` whose scenarios regenerate the case files byte-identically (gate 16) |
| `golden/anchor/` | Hand-written cases with **absolute** expected values, permanently pinned — protected path |
| `negatief/` | Deliberately ill-formed IR files; each directory name declares the WF rule it violates (`wf###-slug`) and gate 3 demands the verifier cite exactly that rule |
| `rood/` | Verifier-clean documents whose cases declare `expectedFault {ref, clingo}`: **loading or** evaluation must fail observably in both executors, substring-pinned (gate 8), the WF-044 unsolved state among them. Since 2026-09-19 a refusal by the case reader counts — the runners read `expectedFault` before the world, so an ambiguous role key or a link to an undeclared label is pinnable like any fault |

## Admission rules

1. Hand-authored cases: spec-derived; expected values computed by hand
   first — for the anchor subset always, and as working discipline for
   every pinned expectation — then confirmed by the reference
   interpreter.
2. Synthesized cases: admitted **only** when the full differential square
   agrees (reference interpreter ≡ Clingo ≡ Drools; a form the Drools
   backend refuses appears in gate 12's reason-carrying channel, never
   silently). Any disagreement routes to adversarial triage — it is a
   spec bug to resolve, never a case to admit. Mechanized:
   `scripts/admit.sh <candidate-dir> <name>` is the required admission
   route — it checks self-containedness, verifier acceptance, exact
   replay in ref AND clingo, the no-op refutation per case, and the full
   gate set after the copy (rolling back on red). Routing synthesized
   material through it is working discipline (WAY-OF-WORKING), not
   itself a gate: the gate set judges what is in the corpus, not how it
   arrived. Synthesized directories are named `golden/synth-*`; the
   anchor subset stays hand-written and pinned.
3. Every WF rule in the spec has at least one passing golden case and one
   failing negatief case.
4. The held-out acceptance split lives in a **separate private
   repository**, pulled only by CI. It is never vendored, mirrored, or
   read here.
