# wf102-bronpad-naast-bronrol

Violates **WF-102 (a)** (`spec/IR-SPEC.md` ch. 4.3): a fold source carries
`bronPad` AND `bronRol`.

    WF-102 FAIL /resolvedRegels/0/waarde/bronPad: bronPad beside bronRol:
    the one-step range IS bronRol, so the two fields are mutually exclusive

The document is `corpus/golden/anchor/jutterswet-bootberging` reduced to its
minimum fold, with the one-step spelling put back beside the path. Both
fields resolve, both name a declared relation, and the LAST step of the path
is exactly the binding in `bronRol` — so nothing here is wrong except that
the range is stated twice. That is the point of the arm: one meaning has one
spelling, and a consumer that reads one field and not the other would fold a
different set from a consumer that reads the other. `ref` and the ASP
backend both fault the shape by name for the same reason.
