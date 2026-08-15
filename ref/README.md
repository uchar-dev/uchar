# ref — the reference interpreter (Phase 2)

Not started; begins as soon as the IR JSON form exists (`../spec/PLAN.md`
§6, Phase 2).

A small, slow, readable tree-walker over the IR: not a production backend,
but the **executable definition of the semantics**, on the WebAssembly model.
The worked examples in `../spec/RUNTIME-SEMANTICS.md` become its unit tests
verbatim; the golden corpus is its acceptance suite. Once it passes, the
existing differential becomes a triangle: both `regelspraak-java` Drools
back-ends must reproduce it.

Constraints (from the plan, enforced structurally):

- **No shared code with any production runtime** — in particular nothing
  from `regelspraak-java`'s `RegelSpraakRuntime`. Living in a different
  repository makes this structural; no copy-paste imports either: an
  interpreter that shares production code defines nothing.
- No Drools, no engine dependencies. Language free of choice; clarity beats
  speed.
- It implements the default model (Phase 4) first, before any engine does.
