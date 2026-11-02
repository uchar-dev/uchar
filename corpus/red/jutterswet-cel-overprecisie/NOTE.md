# jutterswet-cel-overprecisie

The witness of EF-004 on a cell (`spec/RUNTIME-SEMANTICS.md` ch. 5.7b,
ch. 5.8c): the value of a cell in the case input has the attribute's
datatype and is held to its declared decimals. Here `vondstgewicht` is a
number with 2 decimals, and the case supplies the cell `voorjaar` as 1.005
beside the cell `najaar` 4.00.

The document is `corpus/golden/jutterswet-seizoenvondst/ir.json`; it is
verifier-clean. Only the case is red.

`ref` and the ASP backend lay the cells down and refuse the value before any
rule fires, with EF-004 and the parameters `value` 1.005 and `decimals` 2;
`ref` names the cell as the place, `j1.vondstgewicht[voorjaar]`, and the
`ref/test` row `WV-5.8c-invoer-ef004` pins it. The Drools backend refuses the
cells input itself (DROOLS-047), before a value is held to its decimals;
gate 8 lists it.
