Violates WF-071 (`spec/IR-SPEC.md` ch. 2.4): the `dossier` struct is
recursive — the `zelf` field's datatype names the `dossier` domain
itself. The declaration graph over sum-type payloads and struct fields
must be acyclic (it is the term-depth fence for the ASP encoding).

A minimal hand-built document.
