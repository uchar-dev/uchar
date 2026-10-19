Violates WF-088 (`spec/IR-SPEC.md` ch. 2.4): the `keurstatus` attribute
use site repeats a value list that disagrees with the declaring domain —
it carries a third value `Verweerd` the declaration does not have. An
`Enumeration` use site repeats the declared values inline exactly as a
`SumType` use site repeats its constructors (WF-061); the inline copy is
a repetition of the declaration, never a second opinion.

The drift matters because the two rules that read enumeration values read
DIFFERENT lists: WF-039 checks a literal's label against the LOCAL list,
WF-063 checks a match's arms against the DECLARATION. A drifted use site
therefore let an alien label through both — verifier-clean, yet ref
faulted at evaluation ("match has no arm for enumeration value …") and
clingo silently derived nothing.

A minimal hand-built document — before the rule the verifier passed it
(`verifier: PASS`).
