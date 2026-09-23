# wf046-kwantorvouw-bindingloos-bereik

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3), through the range of a quantifier fold
written without a binding: no `bronRol`, no `bronPad`, so the range was the
one connecting relation the type pair derived.

The recursive creation "richt een post in" (group "estafette") mints a Post
for a Wachter while "de rang van de Wachter is kleiner dan 5 en alle posten
van de Wachter hebben een nummer groter dan 5". The fold names no role. Its
range is the `bemanning` relation, the one relation that connects Wachter to
Post, and the creation writes that relation through its back-link and gives
the new Post nummer 1. The Wachter without posten satisfies the ALLE fold
vacuously, and the Post it mints falsifies it.

    WF-046 FAIL /rules/objectCreaties/0: negative dependency edge on a cycle: richt een post in reads what richt een post in produces while richt een post in is reachable from it (no strata exist)

Until 2026-09-23 the dependency walk drew no range edge at all for a
bindingless fold: the arm read only a bound role or path. It now reads the
derived connecting relation and, as a bindingless instance fold does
(`RUNTIME-SEMANTICS.md` ch. 6.1), the element population. Under ALLE the
matrix is monotone and reads positively, so the refusal comes from the range
alone. Measured before (2026-09-23), with one input Wachter W1 of rang 1:
`verifier: PASS`; the reference interpreter minted one Post with nummer 1,
justified by a vacuous ALLE that this Post falsifies; the ASP backend found
no stable model. No corpus document carries a bindingless quantifier fold.

Minimal own-authored document on the shape of
`corpus/golden/anchor/jutterswet-wachtersketen`, with a second type Post.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** Since L2 the range
of a quantifier fold is written (B12, D7): the loader refuses a quantifier
fold without `bronRol` or `bronPad`, and the migration
(`spec/proposals/v1-migratie/m2.py`) derived the one role,
`bemanning`/`post`. The document still fails on WF-046, now through the
bound range under ALLE — the arm `wf046-kwantorvouw-alle-bereik` pins on
another model; the directory name and the paragraphs above record the form
it was written for, which the wire can no longer carry.
