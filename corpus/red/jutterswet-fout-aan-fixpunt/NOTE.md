# jutterswet-fout-aan-fixpunt — an evaluation fault that stands at the fixpoint

The flank of `corpus/golden/jutterswet-fout-op-tussenstand`: V3 gemeld, opgegeven termijn
31, vervalmaand 4. The rule writes `bergtermijn` 31, and the date with year 2025,
month 4 and day 31 names no calendar day at the fixpoint (EF-032, RUNTIME-SEMANTICS
ch. 3.8, 5.7). The rules stand in the order of the casus; `jutterswet-fout-aan-fixpunt-omgekeerd` has them in the reverse order, with the same expectations.
