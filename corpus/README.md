# The uchar corpus

The executable test material for the spec: golden cases (source → IR →
expected outcome) and deliberately ill-formed negatief cases. The corpus
ships with the spec and inherits its obligations: **rights-free by
construction**.

## Rights policy

- Casus material is own-authored (the Jutterswet, `jutterswet/`) or built on
  statute text, which is copyright-free under art. 11 Auteurswet.
- No ©-Belastingdienst material enters this repo: no TOKA casus, no
  RegelSpraak specification excerpts. Programs *written in* RegelSpraak are
  ours; the language specification text is not.

## Layout (Phase 1 fills this in)

| Path | Contents |
|---|---|
| `jutterswet/` | The casus source material: statute text, dossiers, coverage roadmap |
| `golden/` | One directory per case: source, its IR-JSON, expected outcomes |
| `golden/anchor/` | Hand-written cases with **absolute** expected values, permanently pinned — protected path |
| `negatief/` | Deliberately ill-formed IR files + the verifier verdict each must produce (at least one per WF rule) |
| `rood/` | Verifier-clean documents whose cases declare `expectedFault {ref, clingo}`: evaluation must fail observably in both executors, substring-pinned (gate 8) — the standing fence for red paths, the WF-044 unsolved state among them |

## Admission rules

1. Hand-authored cases: spec-derived; expected values computed by hand
   (anchor) or by the reference interpreter once it exists.
2. Synthesized cases (from Phase 2): admitted **only** when the full
   differential square agrees (Drools ≡ reference interpreter ≡ Clingo, to
   the extent those exist). Any disagreement routes to adversarial triage —
   it is a spec bug to resolve, never a case to admit.
3. Every WF rule in the spec has at least one passing golden case and one
   failing negatief case.
4. The held-out acceptance split lives in a **separate private repository**,
   pulled only by the CI acceptance job. It is never vendored, mirrored, or
   read here.
