Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) through a `ListType`: the
`aanspraken` attribute of `Jutter` is declared `list of beschikking`, and
the `AttribuutRef` that feeds the `MapList` is typed `list of oordeel` —
the element domain drifts, the list wrapper hides it. The lambda parameter
and the binder inside agree with the reference (`oordeel`), and the match
arms cover `oordeel`, so every structural check along the way passes.

    WF-089 FAIL /resolvedRegels/0/waarde/list/list/type: attribute reference
    'aanspraken' is typed with keuzetype domain 'oordeel', but the
    declaration it reads from names domain 'beschikking'

Why it has to exist, and why it is the sharpest of the set: WF-070 crosses
the lambda parameter against the source's SERIALIZED element type, which
is the drifted one, so the whole chain is internally consistent and only
the anchor to the declaration is missing. Before the rule the document was
verifier-clean; ref faulted (`match has no arm for constructor
"Toegekend"`) — but clingo returned a WRONG ANSWER, `som = 0` where 35.00
was expected: no arm matched, the map produced nothing, and the fold
returned its init. Not a fault, not a leeg — the differential split into a
value. Drools auto-excluded the `ListType` datatype.

Falsifiable: make `keuzetype_domain` return `None` for a `List_type`
(drop the element look-through) and this case passes while every other
wf089 case stays red — the look-through is its own falsifiable line.
