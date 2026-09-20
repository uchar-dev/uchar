# wf037-aantal-opgesomd-drietal

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in the count's ARITY clause,
at THREE operands. Sibling of `wf037-aantal-opgesomd`: same shape, one
post more.

    WF-037 FAIL /resolvedRegels/0/waarde: a count folds INSTANCES and has
    no LISTED form, and this node carries 3 operands: the source language
    spells a count over a subject expression alone (ch. 4.3), so there is
    no articulated operand list to admit — the only reading an operand
    list leaves a count is the number of FILLED operands, and that meaning
    belongs to the filled count over its set source, never to this kind

Why a second witness on one arm. The arm's bound is `>= 2`, not `= 2`, and
the two directions a reader can mistake it in are different: the pair could
be read as the smallest listed form and the bound as an equality. This case
pins the open end, and it pins it where it is observable — the message
interpolates the operand count, so a bound that silently became an equality
would leave this document clean rather than report `3`.

The document. `wf037-aantal-opgesomd` with `gewogen wrakhout` declared and
read as a third operand. Everything else is identical, including the
count-typed target and the absence of every facet.

Executors, measured on this document with a case in which `J1` fills
`gewogen vangst` and `gewogen wrakhout` and leaves `gewogen bijvangst`
empty:

    ref         : 2   (it counts the FILLED operands)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : EXCLUDE transpiler SKIP (aggregatie buiten Phase-C scope);
                  DRL-text branch EXCLUDE drl-transpiler MARKER (TODO
                  aggregatie AANTAL)

The one-answers-two-refuse shape does not change with the operand count,
which is the point: nothing about the pair was special, so nothing about
the refusal is either.

Rood-eerst: pre-refusal verifier (binary md5
`c7356ed0f3002e933a550b899e888a1a`) says `verifier: PASS`; with the arm,
the single rejection above. Falsifiable, arm-exact: the shared mutant of
`wf037-aantal-opgesomd` turns this document clean too, and moves nothing
outside the three siblings over 715 documents.
