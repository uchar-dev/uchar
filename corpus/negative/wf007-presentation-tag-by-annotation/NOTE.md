# wf007-presentation-tag-by-annotation

Violates WF-007 (`spec/IR-SPEC.md` ch. 2.4), the presentation arm: the tag
`presentation: "percentage"` on a type annotation. Rule 1 of the host golden
compares the attribute `vondsttarief` with the literal `21`, whose `type` is a
percentage — a `Number` with the unit `%`. Here that `Literal.type` carries
the tag.

    WF-007 FAIL /rules/rules/1/versions/0/condition/right/type/presentation:
    the presentation tag "percentage" on a type annotation: the tag is written
    only where a datatype is declared

Why the arm exists. Since L3 a percentage is a `Number` whose unit is `%`
(delta B14 of `spec/proposals/V1-WIREMIGRATIE.md`, CONSTRUCTS row 27), and the
tag is the dialect's presentation of such a number. It is written at the
declaration positions — the datatype of an attribute, a parameter or a domain
and every datatype nested in one, a function or lambda parameter, a function
result — and never on the `type` a value node carries: that annotation is a
copy of what the declarations decide, and a tag there would be a second
presentation of one number. The schema admits the field on every `Number`;
this arm holds it to the declarations. The tag carries no meaning for
evaluation: every reader reads a `Number` whose unit is `%` as a percentage,
with or without it.

The document is `corpus/golden/jutterswet-vondsttarief` with the tag added to
that one literal type. `expect.txt` pins the arm.
