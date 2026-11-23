# jutterswet-afhaalbaar — a status written as a characteristic

A value that does not come for a Vondst is written as a characteristic in the rules: the rule that computes the value
is conditioned on a characteristic, and a second rule says when that characteristic holds. This is the ordinary
document; its twin `corpus/golden/jutterswet-afhaalbaar-strikt` carries the same rules with `Afleiden uit leeg: nee`.
Own Jutterswet-flavoured casus, own dates, rights-free.

## Casus (own text)

> **Art. 3i (afhaling).** 1. Een vondst is afhaalbaar, tenzij zij drijfhout is. 2. De afhaaldatum van een afhaalbare
> vondst valt zeven dagen na de claimdatum.

## Source (`source.rgs`)

A Vondst with a claimdatum (`Datum in dagen`, input), an afhaaldatum (`Datum in dagen`), the characteristic drijfhout
(input) and the characteristic afhaalbaar, and two rules:

- **afhaalbare vondst** (lid 1): "Een Vondst is afhaalbaar indien hij niet drijfhout is", a negated characteristic
  check;
- **afhaaldatum** (lid 2): "De afhaaldatum van een Vondst moet berekend worden als zijn claimdatum plus 7 dagen indien
  hij afhaalbaar is", a date plus a duration under a characteristic check.

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand.

## Cases (hand-computed, absolute)

The document reads a missing value as RUNTIME-SEMANTICS ch. 1.2 lays down: a characteristic the case does not name
does not hold, and a date plus a duration is leeg where the date is leeg (ch. 1.2a).

| case | input | afhaalbaar | afhaaldatum |
|---|---|---|---|
| case1-afhaalbare-vondst | claimdatum 10-03-2026 | holds | 17-03-2026 |
| case2-drijfhout | claimdatum 10-03-2026, drijfhout | does not hold | leeg: the rule does not fire |
| case3-zonder-claimdatum | — | holds | leeg: the rule fires on a leeg claimdatum |

In this document a drijfhout Vondst and a Vondst without a claimdatum both end with a leeg afhaaldatum; the strict twin
tells them apart. ref, clingo and both branches of the Drools backend play the three cases.
