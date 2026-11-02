# jutterswet-halvering-past-niet — a product that does not fit its whole target

"Het veilingdeel van een Jutter moet berekend worden als zijn opbrengst maal 0,5", with
a whole `veilingdeel`. J1 with opbrengst 1001: the product is 1001/2, which does not fit
0 decimals (EF-006, RUNTIME-SEMANTICS ch. 2.4 point 1, ch. 5.7b). With opbrengst 1000 the
same rule writes 500 and plays. The RegelSpraak frontend refuses this rule statically
(RGS-338: the declared precision of the product, 1 decimal, exceeds the target's 0), so
the document is IR without a source.
