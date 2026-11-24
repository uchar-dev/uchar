# jutterswet-kenmerknaam-wel-en-niet

The case input names the characteristic `op zee geborgen` of the Vondst `V1` in `characteristics`, which states that it
holds, and in `characteristicsAbsent`, which states that it does not (RUNTIME-SEMANTICS ch. 1.5). The case contradicts
itself, so it is refused while it is read, before any rule fires, with the fault kind EF-047 of the case input
(RUNTIME-SEMANTICS ch. 5.7 step 3, ch. 5.7b), with the label of the instance and the characteristic as written as its
parameters.

This is `corpus/red/jutterswet-kenmerk-wel-en-niet` with a characteristic whose name holds spaces: an executor that
reads the input under a name of its own must reach the same characteristic for both statements. The document is the
one of the golden `jutterswet-kenmerknamen-strikt`, which carries `inferFromEmpty: false`.
