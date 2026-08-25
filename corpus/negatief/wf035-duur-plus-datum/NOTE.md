Violates WF-035 (`spec/IR-SPEC.md`): duration + date — the date stands on the
left of plus and min; v0's typing opens its date branch only for a date-left
operand, so a duration-left plus is ill-formed (no commuting).

A minimal hand-broken variant of `corpus/golden/jutterswet-vervaltermijn/ir.json`.
