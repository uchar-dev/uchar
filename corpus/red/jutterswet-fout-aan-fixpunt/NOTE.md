# jutterswet-fout-aan-fixpunt — an evaluation fault that stands at the fixpoint

The flank of `corpus/golden/jutterswet-fout-op-tussenstand`: V3 gemeld, taxatie
7, opbrengst 90. The rule writes `bergloon` 7, and 90 gedeeld door 7 does not fit
the whole `aandeel` at the fixpoint (EF-006, RUNTIME-SEMANTICS ch. 2.4, 5.7). The rules stand in the order of the casus; `jutterswet-fout-aan-fixpunt-omgekeerd` has them in the reverse order, with the same expectations.
