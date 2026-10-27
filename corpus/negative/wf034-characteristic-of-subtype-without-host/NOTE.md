Violates WF-034 (`spec/IR-SPEC.md` ch. 2.2 and ch. 4.6 (q)): a characteristic
check without a `host` reads a characteristic that only a strict SUBTYPE of
the subject declares.

    WF-034.characteristic-undeclared FAIL /rules/rules/4/versions/0/condition:
    'vaartijd' is not a declared characteristic of 'Strandrecht'

Broken from `corpus/golden/jutterswet-ploegblokkade` by deleting one field: the
`host: "Wrakrecht"` of the condition of the rule `zeewaardig` ("Een Strandrecht
is zeewaardig indien hij de vaartijd heeft"). `vaartijd` is declared on
`Wrakrecht`, two steps below the rule's subject `Strandrecht`.

Why the field is required. Member resolution walks the chain UPWARD only
(ch. 2.2): a check without a host reads a characteristic of the subject's own
type or of an ancestor. The one read of a member of a subtype is the narrowing
host (ch. 4.6 (q2); `RUNTIME-SEMANTICS.md` ch. 5.8b rule 4), and the wire names
that subtype in `host`, so no reader searches below the subject and every reader
reads the same declaration — with two sibling subtypes each declaring
`vaartijd`, a reader that searched would have two to choose from. The frontend
writes the host where exactly one type below the subject declares the written
name (`frontends/regelspraak/grammar/DIVERGENCES.md`, "A characteristic of a
subtype — the narrowing host").

Falsifiable in two directions, each verifier-checked on 2026-10-27:

- put `host: "Wrakrecht"` back — clean, the golden;
- write `host: "Jutrecht"` — NOT clean, and another rule: `Jutrecht` is a
  strict subtype of the subject but does not declare `vaartijd`, so the check
  is `WF-100.characteristic-undeclared` (the host must declare what it reads).
