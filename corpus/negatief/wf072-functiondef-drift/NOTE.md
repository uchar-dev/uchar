Violates WF-072 (`spec/IR-SPEC.md` ch. 2.4): the `dossierweging` function
declares a parameter `d` of struct domain `dossier` whose inline copy declares
`telling` as a 2-decimal number where the declaration declares a whole number.

The arm this case pins: the `functionDefs` position — the same position WF-088
walks for enumerations (`wf088-functiondef-drift`), which `wf072` did not
reach. The corpus carries a struct-typed function parameter for real
(`jutterswet-kamerhuishouden-catala`, `JutterAandeel aftrek`), so this is a
live position, not a hypothetical one. The body is a bare literal, so no
other rule crosses the parameter.

A minimal hand-built document.
