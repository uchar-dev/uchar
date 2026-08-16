# Negatief corpus

Deliberately ill-formed IR documents: the failing half of the spec
discipline (every WF rule of `spec/IR-SPEC.md` carries at least one
passing golden case and one failing case here).

Layout: one directory per case, named `wf###-<slug>` — the prefix
declares the violated rule. Each directory holds `ir.json` (a minimal
hand-broken variant of a golden dump) and `NOTE.md` (which rule, and
what was broken). Gate 3 (`scripts/check-verifier.sh`) requires the
verifier to reject every case here **for exactly the declared rule**; a
case that passes, or fails only for other rules, turns the gate red.

These files are not required to validate against the schema (gate 2
covers `corpus/golden/` only); they must stay *loadable* — a violation
is a semantic defect in a structurally well-formed document, not a
syntax error.
