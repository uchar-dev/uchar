# wf009-dubbele-afkorting

Violates WF-009 (`spec/IR-SPEC.md` ch. 2.5): a unit system that declares one
abbreviation twice. A unit system carries one `Unit` per abbreviation, and the
abbreviations are compared under the ch. 2.1 identity, the one a `canonical`
resolves by.

    WF-009 FAIL /model/unitSystems/0/units/2/abbreviation: the unit 't' is
    declared twice in the unit system 'Gewicht': one Unit per abbreviation

Until L3 a unit system was a map keyed by the unit, so a second declaration of
one abbreviation could not be written. Since L3 it is a list (card
`20260924-l3-typesysteem`), which can write one, and then the readers part:
the verifier, `ref` and the ASP backend passed this document, and the Drools
loader refused it (measured 2026-09-24). Since this arm the verifier refuses
the document, in the sentence the Drools loader writes as well.

The document is `corpus/golden/jutterswet-vangstklem` with a second `t` in
`Gewicht`, which converts to `kg` by 100 where the first converts by 1000.
`expect.txt` pins the arm.
