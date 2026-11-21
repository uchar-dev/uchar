# jutterswet-afleiden-uit-leeg — a document that infers nothing from an empty value

The RegelSpraak line `Afleiden uit leeg: nee` (grammar/DIVERGENCES.md, "Afleiden uit leeg") on a small Jutterswet
model: a Vondst with a taxatie, a gewicht and a vindloon, the characteristic kostbaar, one rule with a comparison
(`kostbaarheid`: taxatie groter dan 1000) and one with an addition (`vindloon`: gewicht plus 10). The frontend writes
the choice as `inferFromEmpty: false` on the root of `ir.json`, directly after `format`.

In the reading of RUNTIME-SEMANTICS ch. 1.2–1.4 a Vondst without a taxatie is not kostbaar (an ordering against leeg
is false) and a Vondst without a gewicht has the vindloon 10 (leeg coalesces in plus). Under this document's choice
nothing follows from those empty values.

No case file: no executor of this repository plays a document with the property yet. The Drools backend refuses it
with DROOLS-370 on both branches (`InferFromEmptyRefusalWitnessTest`); gate 12 pins it `NOCASE`, gate 5 lists it as not
replayed. Gate 10 re-emits `ir.json` from `source.rgs` byte for byte.
