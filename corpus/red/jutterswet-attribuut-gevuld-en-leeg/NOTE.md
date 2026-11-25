# jutterswet-attribuut-gevuld-en-leeg

The case input gives the attribute `gewenste afhaal-dag` of the Vondst `V1` a value in `attributes` (`14`) and names it
in `attributesEmpty`, which states that it has no value (RUNTIME-SEMANTICS ch. 1.5). The case contradicts itself, so it
is refused while it is read, before any rule fires, with the fault kind EF-048 of the case input (RUNTIME-SEMANTICS
ch. 5.7 step 3, ch. 5.7b), with the label of the instance and the attribute as written in `attributesEmpty` as its
parameters.

The document is the one of the golden `jutterswet-invoer-leeg-strikt`, which carries `inferFromEmpty: false`: the field
`attributesEmpty` decides an outcome only there.
