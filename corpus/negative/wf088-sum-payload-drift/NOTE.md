Violates WF-088 (`spec/IR-SPEC.md` ch. 2.4) at the payload of a sum-type declaration. The domain
`keuring` is a sum type `Gekeurd(keurklasse) | Afgekeurd`, whose `Gekeurd` constructor carries an `Enumeration` of the
domain `keurklasse`. The declaration's payload repeats a third value `wrakhout` that `keurklasse` does not declare. No attribute uses the sum type, so the
declaration is the one use site of `keurklasse` in the document: a payload of a sum-type declaration is a use site of
the domain it names, and WF-088 walks it, so the verifier reports that payload and nothing else.
