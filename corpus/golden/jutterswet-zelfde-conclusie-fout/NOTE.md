# jutterswet-zelfde-conclusie-fout — a fault below an open default with a constant leaves the stack open

A stack in a document with `Afleiden uit leeg: nee` (`inferFromEmpty: false`), read as RUNTIME-SEMANTICS ch. 1.5 lays
down ("An open default with a constant"). Own Jutterswet-flavoured casus, rights-free.

- `rekenloon`: the base `rekenloon`, "op 2 indien het Rekenjaar groter is dan 2025", with above it the deviation
  `betwist rekenloon`, "op 2 indien hij betwist is" — a fault in the justification of the lower default;
- `werkloon`: the base `werkloon`, "het Rekenjaar min 2023", with above it the deviation `betwist werkloon`, "op 2
  indien hij betwist is" — a fault in the value of the lower default.

`betwist` is an input characteristic, not declared complete. The cases carry no evaluation date (`asOfDate: null`), so
every read of the Rekenjaar faults with EF-011 (ch. 8.2).

The reading. Where betwist is open, the deviation is open with 2, and the base is read provisionally: a fault of it,
in its justification (rekenloon) or in its value (werkloon), does not end the evaluation but leaves it open, with any
value, so the stack is open and nothing faults. Where betwist holds the deviation decides 2 and the base is not read. Where betwist stands not to hold the base is read as in the
ordinary reading and the session faults: the red case `corpus/red/jutterswet-zelfde-conclusie-fout`, on this document.
Giving betwist a value thus adds an outcome or a fault, and changes none (ch. 1.5, "The two requirements").

## Cases (hand-computed, absolute)

| case | betwist | rekenloon | werkloon | §9.5 |
|---|---|---|---|---|
| case1-betwist-open | open | open | open | none |
| case2-betwist-waar | holds | 2 | 2 | none |

`ir.json` is the frontend's emission of `source.rgs` (gate 10). The case files are written by hand, without
`scenario.rgs`: the scenario layer demands an evaluation date for a rule that reads one, and these cases have none.
