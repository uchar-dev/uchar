# jutterswet-kopie-past-niet — a copy that does not fit its whole target

"Het bergloon van een Vondst moet gesteld worden op zijn taxatie", with a `taxatie` of 2
decimals and a whole `bergloon`. V1 with taxatie 1.50: the value 3/2 does not fit 0
decimals (EF-006, RUNTIME-SEMANTICS ch. 2.4 point 1, ch. 5.7b). The value counts, not the
scale it is written at: with taxatie 1.00 the same rule writes 1 and plays. The RegelSpraak
frontend refuses this rule statically (RGS-338), so the document is IR without a source.
