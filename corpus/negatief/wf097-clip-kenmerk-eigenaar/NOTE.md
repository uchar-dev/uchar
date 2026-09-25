Violates WF-097 (`spec/IR-SPEC.md`): a `gedurende` clip member carries a
`host`. The clip reads the subject's own characteristic timeline, so a host
names a subject no reader consults.

A minimal hand-broken variant of
`corpus/golden/jutterswet-tegenwacht/ir.json`: one field on one node —
`rules[0].versions[0].value.filter.parts[0].host` goes from absent to
`"Jutter"`. The name is a DECLARED object type on purpose, so
WF-034's host arm (which only asks whether the name resolves) stays silent
and this document is rejected by WF-097 alone.

The member sits inside the flat `all` composite, so this witness also pins the
recursive path: the clip walk reaches composite members through the same arm.

Before the rule existed the document was verifier-PASS and both executors
replayed all six of the golden's cases with the untwisted answers — the silence
this negatief closes.

Since L5 the field is `host` (delta B10), absent where the subject is meant;
the report names it, and `expect.txt` pins the arm.
