# C2 — design proposal: the free priority relation (read-only exploration)

Goal: LRML-audit candidate C2 (`spec/proposals/LEGALRULEML-AUDIT.md` §7):
uchar expresses override as *structure* (intra-target ladder priority,
WF-050/051/075) and carries no free `r1 > r2` superiority relation across
targets/rules and no strict/defeasible/defeater strength tags
(`CONSTRUCTS.md` row 8 records the shortfall). This card is a **design
exploration and proposal only** — the 5b/5c pattern: read the existing
machinery (ladders ch. 6.1/RS ch. 7, the §9.5 conflict channel,
stratification WF-045/046/051, the Catala default calculus the ladder
adopted, Antoniou's derivable-override literature the audit cites), map
the design space (free relation object vs structural encodings vs
strength tags; interaction with conflict detection and stratification;
what LegalRuleML actually requires vs its refinements), and write
`spec/proposals/PRIORITY-RELATION.md`: analysis, a recommended design (or
a reasoned "do not build"), migration/verifier implications sketched, and
**numbered ratification questions for the maintainer** — no normative
change, no build.

Acceptance:
- `spec/proposals/PRIORITY-RELATION.md` exists: honest coverage of the
  design space, every claim about existing uchar machinery carrying its
  witness (WF id, chapter, corpus case — verified, not pattern-matched),
  and a closing numbered-questions section the maintainer can answer
  one by one.
- Nothing else changed; all gates green (they were green at start).

May touch: `spec/proposals/PRIORITY-RELATION.md` only.
Out of scope: any edit to `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
schema, verifier, corpus, backends, frontends, `spec/CONSTRUCTS.md`.

## Result

`spec/proposals/PRIORITY-RELATION.md` landed (415 lines): the C2 design
exploration recommends NOT building the free `r1 > r2` relation or
strength tags, with five numbered ratification questions (§9) awaiting
the maintainer. The opus lens caught one MAJOR — §2 overclaimed that
over-determination outside a ladder always reports loudly; two guarded
writers with overlapping guards are verifier-clean yet split ref
(divergence fault) from clingo (silent multi-value) — fixed with the
mechanical witness written into the proposal itself. Deliberately left:
the five questions are unanswered (a proposal is a proposal); the
CONSTRUCTS row-8 note is ratification question 5; the samenloop witness
of question 4 would be its own small card if ratified.
