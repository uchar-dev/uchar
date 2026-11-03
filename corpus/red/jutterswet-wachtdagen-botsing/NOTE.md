# jutterswet-wachtdagen-botsing

Two writers of one timeline target that decide one day with different
values (`RUNTIME-SEMANTICS.md` ch. 5.7 per day): no fact base satisfies
both equations on that day, and the session faults with EF-016.

    Objecttype de Jutter (mv: Jutters) (bezield)
        is wachtplichtig kenmerk (bijvoeglijk) voor elke dag;
        de wachtdagen Numeriek (niet-negatief geheel getal) voor elke maand;

    Regel wachtdagen in een volle maand
        geldig altijd
            De wachtdagen van een Jutter moet gesteld worden op 30 gedurende
            de tijd dat hij gedurende de gehele maand wachtplichtig is.

    Regel wachtdagen in elke maand
        geldig altijd
            De wachtdagen van een Jutter moet berekend worden als het aantal
            dagen in de maand dat hij wachtplichtig is.

J1 is wachtplichtig 2025-11-16 up to 2026-03-13. The second writer has no
condition and decides every month it counts; in December 2025, a whole
month, the first decides 30 and the second 31. The fault names both rules,
the attribute, the label and the object type; `ref` also names the values
of the first such day, 30 and 31, which the ASP backend's witness does not
carry, so the case pins the four parameters both name. Beside its own
condition the second writer gives the golden
`jutterswet-wachtdagen-twee-schrijvers`.
