Violates WF-063 (`spec/IR-SPEC.md` ch. 4.7), the enumeration arm of the
rule: the `Match` over the `keurklasse`-typed `staat` omits the declared
value `wrakhout`. On an enumeration scrutinee the arms must cover the
declared value list exactly — each once, none missing — the same
compile-time totality the sum variant carries over its constructors
(the keuzetype family, ch. 2.4).

A minimal hand-built document.
