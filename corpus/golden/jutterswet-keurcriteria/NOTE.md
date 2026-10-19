# jutterswet-keurcriteria

Five **consistencies whose assertion is a composite under a counting or a
negating quantifier** (IR-SPEC ch. 4.5, the `Compound` row; RUNTIME-SEMANTICS
ch. 3.9a for the counting reading, ch. 5.10 for the reading order): a `Vondst`
must meet at least two, at most one, exactly one and none of a list of
criteria, and exactly one of a list whose second criterion is itself a
composite, each consistency over criteria of its own, so that one find can
meet all five. The assertion holds where the composite holds, and the
consistency is violated where it does not (the card
aantal-bronarm-en-drools-lader).

The model. A `Vondst` with `gewicht` (two decimals), `lengte` and `jaartal`
(whole numbers), and the characteristics `oud` and `keurig`. The rule "Een
Vondst is oud indien zijn jaartal kleiner is dan 1900" is here so that every
case carries a derived expectation and fails under the no-op guard (gate 7);
the consistencies alone would leave a case that expects `consistent` passing
on the raw input. The rule "Een Vondst is keurig indien hij aan precies één
van de volgende voorwaarden voldoet" carries K5's composite as its condition,
word for word, so that the assertion and the condition read one composite
side by side: `keurig` is present on exactly the finds where K5 holds.

    K1  ten minste twee van: gewicht > 0, lengte > 10, jaartal < 1900
    K2  ten hoogste één van: gewicht > 50, lengte > 50, jaartal < 1500
    K3  precies één van:     gewicht > 1, lengte > 20
    K4  geen van:            gewicht < 0, lengte < 0
    K5  precies één van:     lengte > 20,
                             ten minste één van: gewicht > 50, jaartal < 1860

Hand-computed, the number of criteria that hold per find (a leeg operand
makes its criterion onwaar, ch. 1.2d); under K5 the nested composite is one
criterion, and it holds where one of its own two does (the column in
brackets counts those):

    find  gewicht lengte jaartal   K1  K2  K3  K4  K5      oud  keurig
    V1       2.50     12    1850    3   0   1   0   1 (1)   yes  yes
    V2       0.00     25    1899    2   0   1   0   1 (0)   yes  yes
    V3      60.00     12    1850    3   1   1   0   1 (2)   yes  yes
    V4       0.00      5    1950    0   0   0   0   0 (0)   no   no
    V5       leeg     12    leeg    1   0   0   0   0 (0)   no   no
    V6      60.00     60    1850    3   2   2   0   2 (2)   yes  no
    V7      -1.00     12    1850    2   0   0   1   1 (1)   yes  yes

`case1-binnen-de-drempels.json` holds V1, V2 and V3 and expects the channel
empty: every consistency holds for all three, K1 exactly at its threshold on
V2 and K2 exactly at its on V3, and K5 on V1 and V3 through its nested
criterion alone — on V3 with both of that criterion's own two holding, which
counts once. `case2-over-de-drempels.json` holds V4 to V7 and pins every
violation: K1 below its threshold on V4 (0) and V5 (1, the leeg operands
counting as criteria that do not hold), K2 above it on V6 (2), K3 below it on
V4, V5 and V7 (0) and above it on V6 (2), K4 on V7 (1), and K5 below it on V4
and V5 (0) and above it on V6 (2). An
inconsistency pin is a pin of presence, so the non-violations are pinned by
case 1 and nowhere else.

Measured before the card: `ref` and the ASP backend answered every cell as
above; the Drools exec-model branch excluded the four consistencies
(transpiler SKIP, `schending buiten Phase-D scope`) and the DRL-text branch
left a marker for each (`onopgelost consistentieregel`), while both played
the same assertion under `all`. Since the card both branches lower the
violation as the negation of the fold their rule conditions already carry
for these quantifiers, and play both cases with the values above. Swapping
that negation for the fold itself, in either branch, fails case 1 in that
branch (measured).

K5 came with the fix round of the card. Before it the exec-model branch
excluded that consistency (transpiler SKIP, `schending buiten Phase-D
scope`): its fold lowered one level of criteria and no composite among them,
while the DRL-text branch, `ref` and the ASP backend answered it as above.
Since then it folds a nested composite as one criterion, at any depth, as the
DRL-text branch lowers it. Making `ten minste` strict in that fold fails case
1 in the exec-model branch, on K5 at V1 (measured). Case 2 gained the three K5
pins; case 1 did not change.

The rule `keurigheid van een vondst` came with the same round, for the same
gap in the exec-model branch's rule conditions: it excluded the rule
(transpiler SKIP, `voorwaarde buiten Phase-B scope`), as its counting fold
read leaf criteria only, while the DRL-text branch, `ref` and the ASP
backend derived `keurig` as above. That fold now reads a nested composite as
the assertion does. The same strict `ten minste` fails `keurig` on V1 in
case 1 and on V7 in case 2 (measured). Each case gained the `keurig` pins of
its finds.

The document is emitted from `source.rgs` by the RegelSpraak frontend. Until
the card it was written by hand: the frontend parsed the subject-led form
("Een Vondst moet voldoen aan ten minste twee van de volgende criteria: …")
and lifted a composite consistency only under `alle` with criteria that
named the subject in full, leaving every other one off the wire with a
warning (`consistentieregel wordt nog niet (volledig) naar IR/DRL
omgezet`); since the card it lifts the composite under every quantifier and
reads "zijn …" in the criteria against the onderwerp. Re-emitted from the
source, the document differs from the hand-written one in its source
snippets alone; the two case files did not change.
