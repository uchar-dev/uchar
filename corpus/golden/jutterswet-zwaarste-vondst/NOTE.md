# jutterswet-zwaarste-vondst — folds whose subselection tests a value derived later

A Berging holds vondsten. The afgeleide dag of a Vondst comes from a
defaults group: its opgegeven dag when it has one ("Afwijking van afgeleid:
opgave gaat voor"), else its vinddag ("Regel afgeleid"). A Vondst is
gedateerd once its afgeleide dag is filled. Its registratiedag is
initialised on its vinddag and set to its opgegeven dag when it has one
(RUNTIME-SEMANTICS ch. 5.4: the berekening wins over the initialisatie).
Four rules fold over the vondsten of a Berging with a subselection ("die aan
de volgende voorwaarde voldoen: ...", IR-SPEC ch. 4.3 and 4.5, the `filter`
field) that tests one of those derived values:

| rule | fold | subselection | the tested value comes from |
|---|---|---|---|
| zwaarste | de maximale waarde van het gewicht van alle vondsten van de berging van de Vondst (two roles away) | zijn afgeleide dag is leeg | the defaults group |
| zwaarste late | the same fold | zijn registratiedag is later dan 01-02-2026 | the initialisatie, then the berekening |
| losse gewicht | de som van het gewicht van alle vondsten van de Berging | hij is niet gedateerd | a rule on the afgeleide dag |
| meldklasse | in the condition: het gewicht van ten minste één van de vondsten van de Berging ... groter is dan 15 | zijn afgeleide dag is leeg | the defaults group |

`source.rgs` declares the four readers before the writers. `ref/` and the ASP
backend evaluate each fold after the writers of what its subselection reads
negatively (the leeg test and the negated characteristic are negative edges,
IR-SPEC ch. 5.5 and WF-046), and in their fixpoint a comparison on a value
that changes is evaluated again, so the order does not matter to the
answer. `zwaarste` is the reproduction of the card
drools-exec-vouwfilter-latere-afleiding.

## Hand computation

`case1-een-gedateerde-en-een-ongedateerde-vondst.json`: B with V1 (gewicht
10, no dates) and V2 (vinddag 01-03-2026, gewicht 20). Afgeleide dag V1
**leeg** (neither default decides), V2 **01-03-2026**; registratiedag V2
**01-03-2026** (the initialisatie; no opgegeven dag); V2 **gedateerd**, V1
**not**. Only V1 has an empty afgeleide dag, so the zwaarste gewicht of V1
and of V2 is max(10) = **10**. Only V2 has a registratiedag later than
01-02-2026, so the zwaarste late gewicht of both is **20**. Only V1 is not
gedateerd: losse gewicht 10 = **10**. The one vondst with an empty
afgeleide dag weighs 10, not more than 15: meldklasse **leeg**.

`case2-de-opgave-gaat-voor.json`: B with V1 (opgegeven dag 15-02-2026,
gewicht 30), V2 (no dates, gewicht 20) and V3 (vinddag 01-03-2026,
opgegeven dag 15-01-2026, gewicht 25). Afgeleide dag V1 **15-02-2026** (the
exception), V2 **leeg**, V3 **15-01-2026** (the exception goes before the
vinddag). Registratiedag V1 **15-02-2026** (the initialisatie finds no
vinddag, the berekening sets the opgegeven dag), V3 **15-01-2026** (the
initialisatie writes 01-03-2026, the berekening then 15-01-2026). V1 and V3
**gedateerd**, V2 **not**. The one vondst with an empty afgeleide dag is V2:
zwaarste gewicht of V1, V2 and V3 max(20) = **20**. Only V1 has a
registratiedag later than 01-02-2026 (V3's is 15-01-2026, V2 has none):
zwaarste late gewicht of all three max(30) = **30**. Losse gewicht 20 =
**20**. V2 weighs 20, more than 15: meldklasse **1**.

`case3-alle-vondsten-gedateerd.json`: B with V1 (vinddag 01-02-2026, gewicht
10) and V2 (vinddag 01-03-2026, gewicht 20). Afgeleide dag and registratiedag
V1 **01-02-2026**, V2 **01-03-2026**; both **gedateerd**. No vondst has an
empty afgeleide dag, and the maximum over no contributor is leeg (ch. 1.2e):
zwaarste gewicht of V1 and V2 **leeg**. V1's registratiedag is 01-02-2026,
not later than itself; V2's is later: zwaarste late gewicht of both **20**.
No vondst is not gedateerd, and the sum over no contributor without a
fallback is leeg (§5.8.2): losse gewicht **leeg**. Meldklasse **leeg**.

A no-op run fails all three cases: it leaves every derived value leeg and
no characteristic granted (case 1: 8 mismatches, case 2: 14, case 3: 8).

## The square

`ref` and the ASP backend replay the three cases (`oracle: PASS (11
expectations)`, `(16 expectations)`, `(12 expectations)`; clingo the same).
The exec-model branch of the Drools backend plays them (`PLAYED 3`); the
DRL-text branch refuses the document for its defaults group (`EXCLUDE drl:
ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering`).

Measured on the exec-model branch before the card (2026-09-30):

    FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1 | case2-de-opgave-gaat-voor.json: attribuut zwaarste gewicht: verwacht 20, werkelijk 25 | case3-alle-vondsten-gedateerd.json: verwacht leeg, werkelijk 20; verwacht leeg, werkelijk 20; verwacht leeg, werkelijk 20; verwacht leeg, werkelijk 1

Each rule computed over the vondsten as they stood before their afgeleide
dag was written. A fold in a value is an accumulate and saw the element
leave its subselection, but Drools passed that revision on with the
subject's last propagation context, and when the rule's own write was that
last change NO_LOOP dropped it; a maximum that emptied read leeg, and leeg
decides nothing on a standing target (ch. 7.8); the meldklasse had fired on
the transient and a rule does not write back. The branch now places a rule
below the writers of what a fold in it reads negatively, and a rule whose
value folds with a subselection carries no NO_LOOP. Measured with each half
alone: with the revision alone case 1 and case 3 still fail, with the order
alone the three cases play.

Not in this golden, with its reason: a revision that only the revision
gives, a comparison whose element leaves the subselection after the rule
fired (the zwaarste late gewicht is such a comparison, but in this document
the order alone already plays it, measured); a case input on the folded
target; a characteristic granted on the fold's result; the quantifier alle
over a subselection that fills later; an aggregation comparison over a
subselection. Each is a row of `FoldFilterLaterDerivationWitnessTest` in the
Drools backend.
