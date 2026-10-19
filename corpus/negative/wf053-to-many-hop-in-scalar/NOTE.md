Violates WF-053 (`spec/IR-SPEC.md` ch. 4.1a) at the position of the whole
rule value: the rule of a `Wachter` reads `hoogte` of the `Post` that
`postbezetting` links to it, one bound hop whose reached role `post` is
to-many. A rule value is one value; a to-many step needs an aggregation,
never a silent first pick.

    WF-053 FAIL /rules/rules/0/versions/0/value: hop 0 reaches the
    to-many role 'post' in a scalar slot — a to-many step needs an aggregation,
    never a silent first pick

**Rewritten at L2 (2026-09-24), stamp: herschreven (m).** (First stamped
(i); the L2 fix round measured the document byte-identical to the
migration's translation of its L1 form.) Until L2 the hop
was spelled by the written plural `posten` and derived by name. Since L2 the
navigation carries its binding (B11); the migration derived
`postbezetting`/`post` from the written name, and the to-many hop is what
remains. `wf053-to-many-rule-value` pins the same position on the vondsten
model. `expect.txt` pins the arm.
