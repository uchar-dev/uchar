# jutterswet-vast-bergloon-past-niet — a literal that does not fit its whole target

"Het bergloon van een Vondst moet gesteld worden op 1,5", with a whole `bergloon`. The value
3/2 does not fit 0 decimals (EF-006, RUNTIME-SEMANTICS ch. 2.4 point 1, ch. 5.7b); the
literal 1,50 afgerond op 0 decimalen rekenkundig would write 2. The RegelSpraak frontend
refuses this rule statically (RGS-338), so the document is IR without a source.
