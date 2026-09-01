Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) on its one-sided arm: an
`AttribuutRef` reading the enumeration attribute `vaarwater` (domain
`Vaarwater`) carries a plain `Numeriek` `type` — no keuzetype domain at
all — and its value is assigned to the numeric attribute `tarief`. The
rule fires whenever EITHER side names a keuzetype domain; here only the
declaration does.

    WF-089 FAIL /resolvedRegels/0/waarde/type: attribute reference
    'vaarwater' is typed number, naming no keuzetype domain, but the
    declaration it reads from names domain 'Vaarwater'

Why it has to exist: this is the soort-lie of an `AttribuutRef` in an
ordinary value position, and no rule crossed it before — WF-023 compares
the rule target's type with the value's SERIALIZED type (number against
number, clean), WF-027's cross lives only in the object-creation
initialisation, WF-078 covers binders. The measured split is the widest of
the set: ref wrote the label into the numeric target (`tarief = Wad`),
clingo faulted the translation (`unscalable datatype`), and Drools crashed
at fire (`ClassCastException: Cannot cast java.lang.String to
java.math.BigDecimal`) — three executors, three behaviours, one
verifier-clean document.

Falsifiable: make the two one-sided branches of `wf089_domain_identity`
(`Some _, None` and `None, Some _`) return `[]` and this case passes while
the two-sided cases stay red.
