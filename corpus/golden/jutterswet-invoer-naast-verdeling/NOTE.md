# jutterswet-invoer-naast-verdeling — the input of a target beside an allocation and a computing rule

An equal-shares allocation (RUNTIME-SEMANTICS ch. 4) divides the `opbrengst` of a Berging over the
`bergingsaandeel` of its bergers and writes the undivided rest into its `onverdeelde rest`; the rule "Het
bergingsaandeel van een Berger moet gesteld worden op zijn vaste aandeel indien hij vast is" writes the same target.
Allocation and rule are writers of one rank of the target's stack (ch. 7.8): each decides where it writes a value, two
that write one object with different values are the fault of ch. 5.7, and the input stands where neither decides.

The opbrengst 101 over the two linked bergers B1 and B2 gives each **50** (⌊101 / 2⌋) and a rest of **1**.

| case | instance | input | outcome |
|---|---|---|---|
| case1-invoer | G1, opbrengst 101 | onverdeelde rest 9 | rest **1** (the allocation writes the rest over its input) |
| | G2, opbrengst 50, no bergers | onverdeelde rest 9 | rest **9** (without recipients the allocation writes no rest; the input stands) |
| | B1 linked, vast | bergingsaandeel 7 | **50** (the share; the rule fires with leeg) |
| | B2 linked, vast, vaste aandeel 50 | bergingsaandeel 7 | **50** (share and rule agree) |
| | B3 unlinked, vast, vaste aandeel 30 | bergingsaandeel 7 | **30** (the rule decides) |
| | B4 unlinked, vast | bergingsaandeel 7 | **7** (the rule fires with leeg; the input stands) |
| | B5 unlinked, vaste aandeel 30 | bergingsaandeel 7 | **7** (nothing writes) |
| case2-zonder-invoer | as case 1 without input and without B5 | — | rest **1**, B1 **50**, B2 **50**, B3 **30**, B4 **leeg** |
