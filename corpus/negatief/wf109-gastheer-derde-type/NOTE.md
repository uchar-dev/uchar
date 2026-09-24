Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)) at the **characteristic
check's host**, in the one position that binds TWO objects: the guard of a
§13.4.7 fact creation. The creation runs `Kustvak` → `Post` → `Sein`, so its
guard has the anker and the doel in scope and nothing else; this guard is
hosted on `Post`, the intermediate step's type. Two patterns, a name for
neither.

    WF-109 FAIL /rules/feitCreaties/0/versions/0/condition/eigenaar: the
    characteristic check is hosted on 'Post', which no object bound here can be:
    this position binds the anchor 'Kustvak' and the target 'Sein'

Broken from `corpus/golden/jutterswet-seinhijsing` by moving one field: the
first creation's `eigenaar` from `Sein` to `Post`. `Post` declares `gehesen` in
that model, so WF-100 is satisfied and this rule is the only one speaking. The
SECOND creation is left untouched on purpose: it carries the legal doel host
(`eigenaar: "Sein"`, negated) in the same document, so the file holds the
contrast rather than only the defect.

Why it has to exist. An intermediate step is an object the chain walks
THROUGH, not an object the guard is about: the creation asks its question once
per anker and once per doel, and there is no third pattern to hang a constraint
on. Measured 2026-09-19 before this rule, on this exact shape: verifier PASS,
`ref` and the ASP backend faulting on the host, and both Drools branches
refusing the guard by name (`voorwaarde niet naar anker of doel te lowereren`)
— a verifier-clean document that every executor declined, each in its own
words. WF-109 gives it one verdict, before execution.

Falsifiable in two directions, each turning this case clean:

- put `Sein` back in the field — the golden's own reading, where the guard
  filters the doelen;
- put `Kustvak` in it — the anker reading, where the guard gates the anker and
  a `Kustvak` that is not `gehesen` creates nothing.

Both directions change the ANSWER, not only the verdict, which is why the
golden pins three different numbers for the three readings.
