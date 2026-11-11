Violates WF-031 (`spec/IR-SPEC.md` ch. 3.7): a day-kind condition reads the
state of a rule version — "regelversie controle saldo is inconsistent", the
`Regelversie` check of the inconsistency channel. The channel is derived
fact-base state and falls outside WF-031's closed admission list (dates,
date components, eerste paasdag, literals, parameters and other day kinds).

    WF-031.condition-reads-state FAIL /rules/dayKindDefinitions/0/condition:
    day-kind condition reads rule-version state 'controle saldo' — a day-kind
    condition references only dates, date-components, easterSunday,
    literals, parameters and other day kinds, never derived object state
    (an attribute or characteristic read)

Derived by hand from golden `jutterswet-saldotoezicht`, 2026-11-11: the day
kind `toezichtsdag` and its definition added. The rule-status check has no
verifier arm of its own; this is the arm that names it. `expect.txt` pins
the arm with its parameters.
