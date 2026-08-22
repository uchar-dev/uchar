Violates WF-072 (`spec/IR-SPEC.md` ch. 2.4): the `dossier` attribute's
struct use-site declares `telling` as a 2-decimal number, but the
declaration of domain `dossier` declares it as a whole number. A use
site must repeat the declaration whole — same fields, datatypes and
order.

A minimal hand-built document.
