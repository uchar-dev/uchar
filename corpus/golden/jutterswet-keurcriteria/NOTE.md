# jutterswet-keurcriteria

Four **consistencies whose assertion is a composite under a counting or a
negating quantifier** (IR-SPEC ch. 4.5, the `Compound` row; RUNTIME-SEMANTICS
ch. 3.9a for the counting reading, ch. 5.10 for the reading order): a `Vondst`
must meet at least two, at most one, exactly one and none of a list of
criteria, each consistency over criteria of its own, so that one find can
meet all four. The assertion holds where the composite holds, and the
consistency is violated where it does not (the card
aantal-bronarm-en-drools-lader).

The model. A `Vondst` with `gewicht` (two decimals), `lengte` and `jaartal`
(whole numbers), and the characteristic `oud`. The one rule, "Een Vondst is
oud indien zijn jaartal kleiner is dan 1900", is here so that every case
carries a derived expectation and fails under the no-op guard (gate 7); the
consistencies alone would leave a case that expects `consistent` passing on
the raw input.

    K1  ten minste twee van: gewicht > 0, lengte > 10, jaartal < 1900
    K2  ten hoogste één van: gewicht > 50, lengte > 50, jaartal < 1500
    K3  precies één van:     gewicht > 1, lengte > 20
    K4  geen van:            gewicht < 0, lengte < 0

Hand-computed, the number of criteria that hold per find (a leeg operand
makes its criterion onwaar, ch. 1.2d):

    find  gewicht lengte jaartal   K1  K2  K3  K4   oud
    V1       2.50     12    1850    3   0   1   0   yes
    V2       0.00     25    1899    2   0   1   0   yes
    V3      60.00     12    1850    3   1   1   0   yes
    V4       0.00      5    1950    0   0   0   0   no
    V5       leeg     12    leeg    1   0   0   0   no
    V6      60.00     60    1850    3   2   2   0   yes
    V7      -1.00     12    1850    2   0   0   1   yes

`case1-binnen-de-drempels.json` holds V1, V2 and V3 and expects the channel
empty: every consistency holds for all three, K1 exactly at its threshold on
V2 and K2 exactly at its on V3. `case2-over-de-drempels.json` holds V4 to V7
and pins every violation: K1 below its threshold on V4 (0) and V5 (1, the
leeg operands counting as criteria that do not hold), K2 above it on V6 (2),
K3 below it on V4, V5 and V7 (0) and above it on V6 (2), and K4 on V7 (1). An
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
branch (measured). No `bron.rgs`: the RegelSpraak frontend parses the
subject-led form ("Een Vondst moet voldoen aan ten minste twee van de
volgende criteria: …") and does not lift a composite consistency to the
wire under any quantifier, `alle` included (a warning, `consistentieregel
wordt nog niet (volledig) naar IR/DRL omgezet`); the document is written by
hand.
