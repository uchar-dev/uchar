# jutterswet-cel-dubbel

The witness of EF-035 (`spec/RUNTIME-SEMANTICS.md` ch. 5.7b, ch. 5.8c): the
case input names one cell of an attribute at most once. Here it supplies the
cell `voorjaar`, `zuid` of `vondstgewicht` twice, with 12.50 and with 4.00.

The document is the one of `corpus/red/jutterswet-cel-onbekend-label`. Only
the case is red.

`ref`, the ASP backend and the Drools backend refuse the case while they
read it, with EF-035 and the parameters `label`, `attribute` and `labels`.
