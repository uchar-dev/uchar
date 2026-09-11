# wf047-bronpad-stap-onopgeloste-rol

Violates **WF-047** (`spec/IR-SPEC.md` ch. 2.6) at a `bronPad` step: the step
names a role the fact type does not declare.

    WF-047 FAIL /resolvedRegels/0/waarde/bronPad/1: role binding names no
    declared role 'geborgen vondsten' of fact type 'berging' (the plural does
    not bind)

The second step spells the DECLARED PLURAL (`geborgen vondsten`) where the
binding takes the declared NAME (`geborgen vondst`). WF-102 adds no resolution
rule of its own: a step is a `RolBinding` and resolves exactly as one
anywhere else on the wire, which is what makes the path reuse rather than
restate. The witness exists because the POSITION is new — a rule that walked
the path without resolving each step would report the chain break instead of
the unresolvable step, naming the wrong defect.

The walk stops at an unresolved step rather than cascading: without its filler
types the remainder of the chain has nothing to join against.
