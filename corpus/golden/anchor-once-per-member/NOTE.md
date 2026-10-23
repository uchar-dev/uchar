# anchor-once-per-member — an anchored divisor deposits only where a member reaches it

The worked example of RUNTIME-SEMANTICS ch. 3.4a, *Once or per member*
(`WV-3.4a-anchor-once-*`, also ref's own tests), written in fix round A of the
card `subselection-reads-subject` (2026-10-23) after review A, point 3.

The model of `jutterswet-registervoorrang` with a `tarief` and a `teller` on
the Vergunning and one rule:

    De hoogste tarief boven eigen teller van een Vergunning moet berekend
    worden als de maximale waarde van het tarief van alle vergunningen van het
    register van de Vergunning die aan alle volgende voorwaarden voldoen:
    - hij is niet de Vergunning zelf
    - zijn tarief gedeeld door de teller van de Vergunning zelf is groter
      dan 1.

The divisor is an anchored read (IR-SPEC ch. 4.1a): the Vergunning's own
teller, the same at every member. A teller of 0 makes the division a §9.5
deposit of kind EF-005 on the rule × the form subject, and RUNTIME-SEMANTICS
ch. 3.4a says it arises iff a member reaches the division.

The source is `source.rgs`, and the frontend writes this `ir.json` from it
byte-identically (gate 10; fix round B of the card, after review B). The
document fix round A wrote by hand differed from it in four source snippets
and in the type annotation of the division (`decimals` 2, where the frontend
writes 0): a composite position, whose annotation no consumer reads
(RUNTIME-SEMANTICS ch. 2.10). The cases did not change; ref, clingo and both
Drools branches play them on the frontend's document.

## Hand computation

V1 (tarief 10, teller 2) and V2 (30, 1) in R1 give the first two cases a
value the input does not carry (gate 7): V1 folds V2 (30 / 2 = 15 > 1),
**30**; V2 folds V1 (10 / 1 = 10 > 1), **10**.

`case1-alone-in-register.json`: V9 (10, teller 0) is alone in R9. Its one
member is V9 itself, which the identity check stops before the division
(ch. 5.10): V9 **leeg**, and the channel is **empty**.

`case2-no-register.json`: V8 (10, teller 0) is in no register: its range is
empty, no member reaches the division: V8 **leeg**, channel **empty**.

`case3-member-reaches-division.json`: R8 holds V7 (10, 2) and V8 (10, 0). For
V8 the member V7 passes the identity check and reaches 10 / 0: the deposit
**EF-005 on the rule × V8**; the comparison over the leeg quotient fails, so
V8 has no contributor: **leeg**. For V7 the divisor is 2: V8 passes (10 / 2 =
5 > 1): **10**.

Before fix round A the ASP backend refused all three case files by name (the
helper of the division did not carry the form subject in its head).
