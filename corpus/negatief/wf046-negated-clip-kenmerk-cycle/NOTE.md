Violates WF-046 (`spec/IR-SPEC.md`): the negated characteristic check
inside a `gedurende` clip is a NEGATIVE edge in the dependency walk
(`RUNTIME-SEMANTICS.md` ch. 6.1), and here it sits on a cycle. The
clipped total of `wachtvergoeding per maand` runs over the periods where
the Jutter is NOT `aangesteld`, and the characteristic rule derives
`aangesteld` from that very total, so no evaluation strata exist.

This case pins the ROUTE rather than the rule: it is the witness that a
filter condition on a `Functie` reaches the dependency walk at all, and
that a member with `negatie: true` enters it as a negative edge. The
cycle passes through no recursive creation, so WF-045 is reported as
well; `wf046-negated-read-in-recursive-cycle` is the case that isolates
WF-046 on a WF-045-admissible cycle.

The route matters for the ASP translation: the negated clip is encoded
with negation as failure (`spec/ASP-TRANSLATION.md` S8), and it is this
refusal that keeps a document whose grounded program would have no
stratification off the backend's input.
