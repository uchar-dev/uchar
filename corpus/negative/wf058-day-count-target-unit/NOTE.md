# wf058-day-count-target-unit

Violates WF-058 (`spec/IR-SPEC.md` ch. 4.3): a day count writes days per unit
of its bucket (`RUNTIME-SEMANTICS.md` ch. 3.8; IMPLICIT-TIME VB 7.2), so a
target that declares a unit declares a day unit over the unit its bucket
counts in. Here the month count `kale dagen` declares the unit `dg` alone.

Expected verifier report (`expect.txt` pins key and parameters):

    WF-058.target-unit FAIL /rules/rules/1/versions/0/value: the day count
    writes days per month, and its target 'kale dagen' declares the unit 'dg'
