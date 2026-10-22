# jutterswet-vroege-vondsten-zonder-schorsing

A fold whose subselection compares two dates, read under a condition that
denies a characteristic of the fold's subject. The comparison in the filter
reads per member of the subject's range (`RUNTIME-SEMANTICS.md` ch. 3.4a),
and only where the reading form reaches the fold: under a true rule
condition (ch. 5.7), after the members of a composite that leave its verdict
open (ch. 5.10).

## What it pins

```
Het vroege vondstgewicht van een Jutter moet berekend worden als de som van
het gewicht van alle vondsten van de Jutter die aan de volgende voorwaarde
voldoen: zijn vinddag is eerder dan de peildag van de jutter van de Vondst
indien hij niet geschorst is.

Een Jutter is vroeg actief
indien hij aan alle volgende voorwaarden voldoet:
- hij is niet geschorst
- het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen:
  zijn vinddag is eerder dan de peildag van de jutter van de Vondst is groter
  dan 0.

Een Jutter is vroeg klaar
indien hij aan alle volgende voorwaarden voldoet:
- hij is niet geschorst
- de vinddag van alle vondsten van de Jutter is eerder dan de peildag van de
  jutter van de Vondst.
```

The three rules reach the same date comparison by three routes: the value
of a rule with a denied characteristic as its condition (a sum with a
filter), the second member of a composite after that denial (a count with a
filter), and the same position with a quantifier fold whose test is the
comparison. `geschorst` is case input; `peildag` is read through the
member's own role back to its Jutter.

## Hand-computed absolutes

`peildag` is 2026-03-01 for every Jutter; "eerder dan" is strict.

Case 1, `J1` not geschorst, with `V1` (2026-02-10, gewicht 4), `V2`
(2026-03-01, gewicht 7) and `V3` (2026-01-20, gewicht 2):

| Jutter | early vondsten | `vroege vondstgewicht` | `vroeg actief` | `vroeg klaar` |
|--------|----------------|------------------------|----------------|---------------|
| J1 | V1, V3 (V2 falls on the peildag) | **6** (4 + 2) | **yes** (2 > 0) | **no** (V2 is not early) |

Case 2, `J1` geschorst with `V1` (2026-02-10, 4) and `V2` (2026-01-05, 9);
`J2` not geschorst with `V3` (2026-02-27, 5):

| Jutter | `vroege vondstgewicht` | `vroeg actief` | `vroeg klaar` |
|--------|------------------------|----------------|---------------|
| J1 | **leeg** — the rule does not fire | **no** — the composite stops after "hij is niet geschorst" | **no** — likewise, though every vondst of J1 is early |
| J2 | **5** | **yes** (1 > 0) | **yes** |

Case 3, `J1` not geschorst with `V1` (2026-03-01, 7) and `V2` (2026-04-15,
3); `J2` not geschorst without vondsten:

| Jutter | `vroege vondstgewicht` | `vroeg actief` | `vroeg klaar` |
|--------|------------------------|----------------|---------------|
| J1 | **leeg** — the sum over no member, without a fallback | **no** (0 > 0 fails) | **no** |
| J2 | **leeg** — no member | **no** | **yes** — vacuously, over no vondst |

Every case is consistent. `J2`'s `vroeg klaar` in case 3 is also the
derived value the no-op guard (gate 7) refutes.

## Why it is here

The ASP backend's range fence on each operand of the comparison in the
filter is keyed on the member and carried the reading form's guard — the
rule's condition, or the composite's prefix — which speaks of the subject,
`not has(S,"geschorst")`, without the subject and its range join beside it.
clingo stopped grounding (measured 2026-10-22, before the fix, on all three
cases):

```
error: unsafe variables in:
  viol("arithmetic range exceeded (clingo grounds in machine integers)",X3):-[#inc_base];
    val(N5,"peildag",A6);role(X3,"jutter",N5);val(X3,"vinddag",A4);isa(X3,"vondst");
    |A4|>2147483647;not has(S,"geschorst").
note: 'S' is unsafe
```

six times: two operands for each of the three rules. Inside a fold member or
a quantifier element the guard now carries the subject and the range join,
as the rule's own body binds the subject.

## The differential square

`ref` ≡ clingo over all eighteen expectations (gates 5 and 6), anchored to
the hand-written absolutes above. Gate 12 replays the three cases in the
exec-model branch and in the DRL-text branch. Gate 10 re-emits `source.rgs`
to a byte-identical `ir.json`.
