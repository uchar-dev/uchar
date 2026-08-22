Violates WF-060 (`spec/IR-SPEC.md` ch. 2.4): the `lijst` sum type is
recursive — the `Punt` constructor's payload names the `lijst` domain
itself. The declaration graph over sum-type payloads must be acyclic
(Catala has no recursive types, and acyclicity is what statically
bounds constructor-term depth for the ASP encoding).

A minimal hand-built document.
