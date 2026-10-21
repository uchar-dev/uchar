# backends/ — the engine backends

`clingo/` translates a `uchar-ir-v1` document to ASP-Core-2, and `drools/` loads it into a Drools KieBase. Both are
held to the hand-written expectations of the corpus (gates 6 and 12).

The reference interpreter is not here: it lives one level up, in `../ref/`, on purpose. It is the executable
definition of `spec/RUNTIME-SEMANTICS.md`, not a third engine backend (`spec/PLAN.md`, Phase 2 and §5), and it shares no
code with the production runtime under `drools/`. When a backend and `ref` disagree, the spec decides, never a
majority of executors.
