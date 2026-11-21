# jutterswet-kenmerk-wel-en-niet

The case input names the characteristic `gemeld` of the Vondst `V1` in `characteristics`, which states that it holds,
and in `characteristicsAbsent`, which states that it does not (RUNTIME-SEMANTICS ch. 1.5). The case contradicts
itself, so it is refused while it is read, before any rule fires, with the fault kind EF-047 of the case input
(RUNTIME-SEMANTICS ch. 5.7 step 3, ch. 5.7b), with the label of the instance and the characteristic as written as its
parameters.

The document is the one of the golden `jutterswet-afleiden-uit-leeg`, which carries `inferFromEmpty: false`: the
field `characteristicsAbsent` decides an outcome only there. The Drools backend refuses that document before it reads
the case (DROOLS-370), so gate 8 lists the case in its Drools leg with that code.
