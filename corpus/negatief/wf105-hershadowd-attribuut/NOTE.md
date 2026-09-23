# wf105-hershadowd-attribuut

Violates **WF-105** (`spec/IR-SPEC.md` ch. 2.2): `Wrakrecht` redeclares
`grondslag`, which its chain already declares on `Strandrecht` — and with a
different datatype, which is what makes the shadowing observable.

    WF-105 FAIL /model/objectTypes/1/attributes/0/datatype: attribute
    'grondslag' is already declared on 'Strandrecht' further up the chain: a
    subtype never redeclares an inherited name (one instance would carry two
    declarations of it)

One instance would then carry two declarations of one name, and which one a
reader finds decides that name's datatype, dimensions and timeline facet on
every read. The measured consumer consequence is sharper than "it would not
compile": the Drools bean generator deduplicates member names silently,
first-wins, so a shadowed name yields a bean that generates and compiles
perfectly and is quietly missing one member.

What is deliberately NOT here is the sibling collision — two incomparable
subtypes of one supertype each declaring the same name. That is admitted
without a rule (ch. 2.2): no instance is of two incomparable types, so no
read is ambiguous, and a read of that name anchored at the common supertype
is refused by WF-034 rather than by this rule.
