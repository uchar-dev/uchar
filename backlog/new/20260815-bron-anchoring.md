# Source anchoring (cross-repo: lands in regelspraak-java)

Goal: the Phase-0 job from `spec/PLAN.md` §6 that lands in
`regelspraak-java`: replace the bare `String bron` with
`Bron(file, line, column, snippet)` on `Expr`, `Conditie`, and every
`Resolved*` form, and pass `// Bron:` statute annotations through the
lexer on a hidden channel so explanations can cite articles of law. Small,
mechanical, touches no semantics.

Acceptance: every IR node carries file/line/column; a statute annotation
written above a rule is retrievable from the resolved form;
`regelspraak-java`'s own gate set stays green.

May touch (in regelspraak-java, on a branch there): `ir/`, lexer/parser
plumbing, resolver construction sites; nothing here except `backlog/`.

Out of scope: the v1 `Bron` structure in IR-SPEC (row 3 delta — specified
here, separately); verbalization/explanation output changes.
