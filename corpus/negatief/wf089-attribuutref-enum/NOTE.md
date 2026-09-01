Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4): an `AttribuutRef` used as a
`Match` scrutinee carries a `type` naming the enumeration domain `Kust`
(`Noord`, `Zuid`, `West`) while the attribute it reads — `vaarwater` of
`Berging` — is declared with the domain `Vaarwater` (`Wad`, `Zeegat`,
`Ruimzee`). Both domains are declared and both inline copies repeat their
declaration faithfully; only the reference reads from the wrong one.

    WF-089 FAIL /resolvedRegels/0/waarde/scrutinee/type: attribute reference
    'vaarwater' is typed with keuzetype domain 'Kust', but the declaration it
    reads from names domain 'Vaarwater'

The card case, reproduced rood-eerst. Nothing else fires: WF-088 binds the
use site to the domain it NAMES (`Kust`, repeated exactly), WF-063 checks
the arms against that same named domain (all three present, none alien),
WF-003/WF-039 find every domain and label declared. Before the rule the
document was verifier-clean (`verifier: PASS`); ref faulted at evaluation
(`match has no arm for enumeration value "Wad"`), clingo derived nothing
and reported `tarief` underived where 3 was expected — a silent leeg for
the wrong reason — and Drools auto-excluded the `Match` form.

Falsifiable: disable the `wf089_domain_identity` call in the verifier's
`Attribuut_ref` arm and this document passes while every other wf089 case
stays red; restore it and gate 3 is green with this case in the count.
