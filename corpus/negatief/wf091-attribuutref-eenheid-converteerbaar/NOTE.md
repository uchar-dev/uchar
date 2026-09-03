Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `AttributeReference`
arm, unit facet: the reference reads `Jutter.gewicht`, declared with the
unit `kg` (canonical `kg`, factor 1), but its serialized `type` carries
the unit `t` (the same canonical `kg`, factor 1000). Every other facet
agrees — same soort, no domain, two decimals, not whole, `NIET_NEGATIEF`
— so only the unit differs, and the two units are **convertible**, which
is the whole point of the case.

    WF-091 FAIL /resolvedRegels/0/waarde/type: unit mismatch: attribute
    reference 'gewicht' is typed 't' (factor 1000 to 'kg'), but the
    declaration it reads from is 'kg' (factor 1 to 'kg')

Why it has to exist: this is the class no existing rule can reach.
WF-035 and WF-023 cross units for **compatibility** (`units_convertible`
— same canonical name, same shape), so a lie inside the convertible class
is compatible by construction and passes both. WF-078 crosses the
**soort** only (its own comment and the ch. 4.7 prose said so in as many
words: decimals and units uncrossed), and number equals number here.
WF-089/WF-090 cross the domain, and a numeric type names none. The
declaration is the source of truth for the value, so `ref` and the ASP
backend read `550.00`; a consumer that honours the reference's own
serialized unit rescales by the factor and answers something else. The
card measured exactly that split on this shape before the rule existed:
verifier-clean, `ref` ≡ clingo, third executor divergent — a wrong answer
nothing refused.

Falsifiable, facet-exact: drop the unit facet from the identity engine
(`use_site_identity`) and this case alone turns clean while every other
`wf091-*`, `wf089-*`, `wf090-*` and `wf078-*` case stays red; disable the
`Attribuut_ref` arm instead and this case turns clean together with the
other `wf091-attribuutref-*` cases while the binder, parameter,
fold-source and write-target cases stay red.
