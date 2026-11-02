# jutterswet-precisiepoort — writes the precision gate admits

The passing side of the precision gate of RUNTIME-SEMANTICS ch. 2.4 point 4
(WF-023, `spec/IR-SPEC.md` ch. 3.1), beside the refusals in
`corpus/negative/wf023-precision-*`. Every write has a bound (ch. 2.3) within
its target:

| rule | value | bound | target |
|---|---|---|---|
| ruw aandeel | opbrengst gedeeld door aantal deelnemers | ∞ | the unbounded form |
| aandeel | ruwe aandeel naar beneden afgerond op 0 decimalen | 0 | whole |
| derde deel | opbrengst gedeeld door 3, rekenkundig afgerond op 2 decimalen | 2 | 2 decimals |
| kwart | opbrengst gedeeld door 4 | 2 | 2 decimals |
| heffing | (heffingspercentage, 1 decimal, van taxatie, 2) naar beneden afgerond op 2 decimalen | 2 | 2 decimals |
| taxatiewaarde | taxatie, 2 decimals, maal 100 | 0 | whole |

The first two rules are the form the gate leaves an author who wants no
rounding on the way: the quotient lands in an attribute of the unbounded
form, and the attribute that holds the amount rounds it.

| case | input | ruwe aandeel | aandeel | derde deel | kwart | heffing | taxatiewaarde |
|---|---|---|---|---|---|---|---|
| case1-de-gate-laat-door | V1 opbrengst 100, aantal deelnemers 4, taxatie 10.10, heffingspercentage 12.5 | 25 | 25 | 33.33 | 25.00 | 1.26 | 1010 |
| case2-afronding | V2 opbrengst 70, aantal deelnemers 5, taxatie 99.99, heffingspercentage 12.5 | 14 | 14 | 23.33 | 17.50 | 12.49 | 9999 |

By hand: 100/3 = 33.333… rounds to 33.33; 12.5% of 10.10 is 1.2625 and of
99.99 is 12.49875, both rounded down to 2 decimals. The values of the
unbounded `ruwe aandeel` are whole, since the ASP backend holds the unbounded
form as a whole number (RUNTIME-SEMANTICS ch. 2.4). A listed maximum and a
write that restates its unit are left out: the ASP backend translates
neither; `jutterswet-sleepbaan` carries a unit conversion the gate admits (2,5 km
plus a whole number of metres, bound 0 in metres, into whole metres).
