# jutterswet-cel-onbekend-label

The witness of EF-034 (`spec/RUNTIME-SEMANTICS.md` ch. 5.7b, ch. 5.8c): the
labels of a cell in the case input name one label of each dimension of the
attribute, label i a label of dimension i. Here `vondstgewicht` carries the
dimensions `seizoen` and `strand`, and the case supplies the cell `zuid`,
`voorjaar` — both labels declared, in the reversed order.

The document is `corpus/golden/jutterswet-seizoenvondst/ir.json` with a
second dimension `strand` (labels `noord`, `zuid`) and the cell read
`voorjaar`, `zuid`; it is verifier-clean. Only the case is red.

`ref` and the ASP backend refuse the case while they read it, before any
rule fires, with EF-034 and the parameters `label`, `attribute` and
`labels`: both read the case with the case reader of `ref`. The Drools
backend refuses it with the same code and parameters in a case reader of
its own.
