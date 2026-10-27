Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)): a NAVIGATED characteristic
check names a host strictly below its subject.

    WF-109.only-some-objects FAIL /rules/rules/4/versions/0/condition/host: the
    characteristic check is hosted on 'Wrakrecht', which no object bound here
    IS: only SOME objects of the subject 'Strandrecht' are one, so the host
    would hold for some of them and fault for the rest

Broken from `corpus/golden/jutterswet-ploegblokkade` by adding one declaration
and replacing one condition: `Logboek` declares the characteristic `vol`, and
the condition of the rule `zeewaardig` (subject `Strandrecht`) becomes "zijn
logboek vol is" — `vol` read over the role `logboek` of
`StrandrechtHeeftLogboek` — with the host `Wrakrecht` the golden's own check
carried.

Why the narrowing host stops at the navigated check. On a check WITHOUT a
navigation a host strictly below the one bound object is the narrowing host
(ch. 4.6 (q2); `RUNTIME-SEMANTICS.md` ch. 5.8b rule 4): the characteristic is
the subject's own, declared on the host, and an object outside the host has
none, so the check is false there. On a navigated check the characteristic
lives on the object the hop reaches (ch. 5.8b rule 1), and a host below the
subject would say something about the subject's type that the reading of the
reached object has no place for; no executor carries the shape and no producer
writes it. So the flank stays `only-some-objects` here, the one-object position
notwithstanding.

Falsifiable in one direction, verifier-checked on 2026-10-27: delete the
`host` — clean, an ordinary navigated check.
