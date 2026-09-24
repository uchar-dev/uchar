Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) on its domain clause THROUGH
a `ListType`: the attribute `aanspraken` of `Jutter` is declared `list of
beschikking`, and the rule writes a `ListLiteral` typed `list of oordeel`
whose elements are `Construct`s of `oordeel` (`Goedgekeurd(10.00)`,
`Verworpen`) — every structural check along the value (WF-062, WF-066)
passes because the value is internally consistent. The second rule is the
faithful `MapList`/`Fold` reader of `wf089-attribuutref-lijst-sum`.

    WF-023 FAIL /rules/rules/0/versions/0: a value of the sum-type
    domain 'oordeel' is assigned to a target of the sum-type domain
    'beschikking'

Why it has to exist: the family clause sees list = list and the domain
lives one level down; `keuzetype_domain` looks through the element (the
same look-through WF-089 uses on the read side). Verifier-clean before;
ref wrote the alien list and faulted `match has no arm for constructor
"Goedgekeurd"` in the reader's lambda; clingo faulted the translation
(`unscalable datatype` — the pre-existing fence on a list write, which the
faithful twin with `beschikking` elements hits identically while ref
passes it).

Falsifiable: make `keuzetype_domain` return `None` for a `List_type`
(drop the element look-through) and this case passes together with
`wf089-attribuutref-lijst-sum` — the shared line, pinned from both sides.
