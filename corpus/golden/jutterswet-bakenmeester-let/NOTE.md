# jutterswet-bakenmeester-let — a pro-rata write through a Let

The document of `jutterswet-bakenmeester` with its value bound to a variable. A Bakenmeester has an onderhoudstarief
per year, an onderhoudsbijdrage per month (2 decimals) and the characteristic dienstdoend per month. The rule writes
the onderhoudsbijdrage as a `Let` that binds `het deel` to the time-proportional part per month of the
onderhoudstarief during the time he is dienstdoend, and whose body reads `het deel` and nothing else ("Daarbij geldt:
het deel is …"; RUNTIME-SEMANTICS ch. 3.7, 10.6).

Such a `Let` stands for its bound value, so the write is a pro-rata write as the bare form of
`jutterswet-bakenmeester` is, and the precision gate of WF-023 recognises it as one through the `Let`: the document
is well-formed.

## The hand calculation

Case 1: B1 has onderhoudstarief 24 over 2025 and is dienstdoend from 2025-03-01 to 2025-04-16 (exclusive).

| month | days dienstdoend | onderhoudsbijdrage |
|---|---|---|
| March 2025 | 31 of 31 | 24 × 31/31 = **24** |
| April 2025 | 15 of 30 | 24 × 15/30 = **12** |

The other months carry no value; the case is consistent. The outcome equals `jutterswet-bakenmeester/case1.json`.

## Provenance

Own work, 2026-11-11. Before the verifier looked through the `Let`, it refused the document with
`WF-023.precision-unbounded` at `/rules/rules/0/versions/0/value`, while ref, the ASP backend and both branches of the
Drools backend played it. Measured at admission: verifier PASS; ref, the ASP backend and both branches of the Drools
backend play the case.
