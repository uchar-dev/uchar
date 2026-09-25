Violates WF-035 (`spec/IR-SPEC.md`): date + date — date arithmetic takes a date
and a duration; two dates under plus are ill-formed.

A minimal hand-broken variant of `corpus/golden/jutterswet-vervaltermijn/ir.json`.

    WF-035 FAIL /rules/rules/0/versions/0/value: date ± date is ill-formed
    (date arithmetic takes a date and a duration)

`expect.txt` pins the arm.
