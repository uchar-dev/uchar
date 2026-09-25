Violates WF-070 (`spec/IR-SPEC.md` ch. 4.8, the fold family): a `MapList`'s
lambda body has the datatype of the list's element. Broken from
`corpus/golden/jutterswet-aanspraakherziening` by one field: the `Invoke` of
`herziening`, the inner `MapList`'s lambda body, is typed with the domain
`aanspraak` as declared, but its `Erkend` payload carries 3 decimals where
the declaration and the `MapList`'s element carry 2. WF-068 refuses the same
node against the function's declared result.

    WF-070 FAIL /rules/rules/0/versions/0/value/list/list/function: lambda
    body is sumtype 'aanspraak' [Erkend of number(3 decimals, fractional,
    sign nonNegative), Vervallen], but the MapList requires sumtype
    'aanspraak' [Erkend of number(2 decimals, fractional, sign nonNegative),
    Vervallen]
    WF-068 FAIL /rules/rules/0/versions/0/value/list/list/function/body/type:
    invoke result type differs from the declared function result

What it pins is the message: two datatypes that differ only inside their
members render alike under their names, so each side is rendered whole
(`detail_vs` in `spec/verifier/lib/checks.ml`). Until 2026-09-25 the
verifier named one type twice: `lambda body is sumtype 'aanspraak', but the
fold requires sumtype 'aanspraak'`. `expect.txt` pins the arm.
