# jutterswet-verdeling-botsing

A computing rule and an allocation that write one target with different
values (`RUNTIME-SEMANTICS.md` ch. 5.7, ch. 7.8: the two are writers of one
rank): no fact base satisfies both equations, and the session faults with
EF-016.

    Objecttype de Berging (mv: Bergingen)
        de opbrengst Numeriek (geheel getal);
        de onverdeelde rest Numeriek (geheel getal);

    Objecttype de Berger (mv: Bergers) (bezield)
        het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal);
        het vaste aandeel Numeriek (geheel getal);
        is vast kenmerk (bijvoeglijk);

    Feittype bergingsdeelname
        de berging Berging
        de berger (mv: bergers) Berger
    Eén berging heeft meerdere bergers

    Regel verdeel de opbrengst van een berging
        geldig altijd
            De opbrengst van een berging wordt verdeeld over de
            bergingsaandelen van alle bergers van de berging, waarbij wordt
            verdeeld:
            - in gelijke delen,
            - afgerond op 0 decimalen naar beneden.
            Als onverdeelde rest blijft de onverdeelde rest van de berging
            over.

    Regel vast bergingsaandeel
        geldig altijd
            Het bergingsaandeel van een Berger moet gesteld worden op zijn
            vaste aandeel indien hij vast is.

The Berging G1 has the opbrengst 100 and the Bergers B1 and B2. B1 is vast
with the vaste aandeel 30. The allocation gives B1 and B2 50 each, and the
rule gives B1 30. The fault names both writers by their declared names, the
rule first, since the IR document carries its rules before its
allocations, and the attribute, the label and the object type. An
executor's sentence names the rule as a rule and the allocation as an
allocation; `ref` and the Drools backend also name the values 30 and 50,
which the ASP backend's witness does not carry, so the case pins the four
parameters every executor names.
