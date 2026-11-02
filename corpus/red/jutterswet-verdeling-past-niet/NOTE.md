# jutterswet-verdeling-past-niet — an allocation whose share does not fit its whole target

The document of `corpus/golden/jutterswet-gelijkdeling` without the rounding of its
allocation: the veilingopbrengst of a Strandveiling is divided in equal shares over its
veilingjutters, into a whole `veilingdeel`, with the rest in a whole `veilingrestant`. V1
with veilingopbrengst 1001 over J1 and J2: each share is 1001/2, which does not fit 0
decimals (EF-006, RUNTIME-SEMANTICS ch. 2.4 point 1, ch. 4.2, ch. 5.7b). One fault for the
allocation on V1, and no share is written. With the rounding of the golden (`naar beneden
afgerond op 0 decimalen`) each share is 500 and the rest 1; with a veilingopbrengst of 1000
and no rounding each share is 500 and the rest 0, and the document plays.
