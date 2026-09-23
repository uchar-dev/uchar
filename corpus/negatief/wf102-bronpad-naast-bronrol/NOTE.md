# wf102-bronpad-naast-bronrol

Violates **WF-102 (a)** (`spec/IR-SPEC.md` ch. 4.3): a fold source carries
`bronPad` AND `bronRol`.

    WF-102 FAIL /rules/resolvedRegels/0/waarde/bronPad: bronPad beside bronRol:
    the one-step range IS bronRol, so the two fields are mutually exclusive

The document is `corpus/golden/anchor/jutterswet-bootberging` reduced to its
minimum fold, with the one-step spelling put back beside the path. Both
fields resolve, both name a declared relation, and the LAST step of the path
is exactly the binding in `bronRol` — so nothing here is wrong except that
the range is stated twice. That is the point of the arm: one meaning has one
spelling, and a consumer that reads one field and not the other would fold a
different set from a consumer that reads the other. `ref` and the ASP
backend both fault the shape by name for the same reason.

That last sentence was FALSE for one round, and the round is recorded because
the defect it names is exactly the one this witness is about. `ref` first
landed with only two of the three shape arms — the arity and the position —
so with both fields present its path arm simply won, the binding was never
read, and this document replayed **clean**: `oracle: PASS (2 expectations)`
against the ASP backend's `TRANSLATION FAULT: a bronPad beside a bronRol`.
A two-executor disagreement under a document neither rule in that file
refused, found by review and not by a gate — no gate can see it, because a
negatief is never replayed. The arm now stands in `ref` too, ahead of the
other two so a document carrying both spellings is named for carrying both,
and the ref suite pins it (`WV-3.4b-pad-naast-bronrol-faalt`).
