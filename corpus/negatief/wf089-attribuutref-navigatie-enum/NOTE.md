Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) on a NAVIGATED read — the
clause "at the endpoint type (ch. 4.1a: a navigated read crosses against
the attribute it lands on)": the feittype `ligplaats` relates one `Werf`
to many `Schuit`en; `Werf.vaarwater` is declared `Enumeratie` `Vaarwater`;
the rule on `Schuit` computes `premie` by matching `vaarwater` reached
through the `werf` role (`objecttype Werf`, `navigatieRol Schuit`,
`navigatiePad [werf]`, a bound hop on `ligplaats/werf`), and the reference
is typed `Enumeratie` `Kust`, with arms `Noord`/`Zuid`/`West`.

    WF-089 FAIL /rules/resolvedRegels/0/waarde/scrutinee/type: attribute
    reference 'vaarwater' is typed with keuzetype domain 'Kust', but the
    declaration it reads from names domain 'Vaarwater'

Why it has to exist: the declaration source for a navigated reference is
the attribute at the reference's endpoint type — the `objecttype` field,
which WF-053's endpoint check binds to the type the path actually reaches,
so crossing there is safe. Skipping navigated references left gate 3 green
before this case existed. Measured: ref faulted `match has no arm for
enumeration value "Wad"` (W1 carries `Wad`, S1 sits at W1); clingo faulted
the translation for this document AND for its faithful twin typed
`Vaarwater` (the pre-existing fence on a navigated enumeration read — ref
passes the twin with `premie = 3`).

Falsifiable: make the `Attribuut_ref` arm pass `~decl:None` whenever
`navigatie_rol <> None || navigatie_pad <> []` and this case passes while
every un-navigated `wf089-*` case stays red.
