# jutterswet-vrijstelling-drie-niveaus — a third level of nested conditions under a counting quantifier

A Jutter has a leeftijd and the soort of his find. He has the vrijstelling when
at least one of two conditions holds: the find is Blik, or he is older than 65
and the find is Touw, Glas or Kurk. The second condition is an 'alle' group
inside a 'ten minste één' list, and holds an 'ten minste één' group of its own:
three levels, written `-`, `..`, `....`. Until the card third-condition-level
this sentence had no spelling under a counting quantifier: the frontend turned
it into a marker, and every executor stopped on it. The depth of a bullet now
follows from its sign (`frontends/regelspraak/grammar/DIVERGENCES.md`,
"§13.4.14.6–7 The depth of a bullet"); "deeper" is strictly greater, so the
`....` of the third level reads as well as `...`. Own Jutterswet-flavoured
casus, own values, rights-free.

## Casus (own text)

> **Art. 9b (vrijstelling).** Een jutter is vrijgesteld als zijn vondst blik
> is, of als hij ouder is dan 65 jaar en zijn vondst touw, glas of kurk is.

## Source (`source.rgs`)

- **Vondstsoort**: an enumeration of five values.
- **vrijstelling** (art. 9b): a possessive characteristic of the Jutter, set
  when "hij aan ten minste één van de volgende voorwaarden voldoet": `-` the
  soort is Blik; `-` "hij voldoet aan alle volgende voorwaarden": `..` the
  leeftijd is greater than 65, `..` "hij voldoet aan ten minste één van de
  volgende voorwaarden": `....` Touw, `....` Glas, `....` Kurk.

The IR carries the tree as written: `Compound atLeast 1 [soort = Blik,
Compound all [leeftijd > 65, Compound atLeast 1 [Touw, Glas, Kurk]]]`.

## Hand computation

`case1-blik-op-dertig.json`: J1, 30 jaar, Blik. The first bullet holds:
**vrijstelling**.

`case2-touw-op-zeventig.json`: J1, 70 jaar, Touw: not Blik; 70 > 65 and Touw
is one of the third level, so the second bullet holds: **vrijstelling**. J2,
70 jaar, Kurk, the last bullet of the third level: **vrijstelling**.

`case3-touw-op-dertig.json`: J1, 30 jaar, Touw: not Blik, and 30 is not
greater than 65, so the 'alle' group fails: **geen vrijstelling**. J2, 70
jaar, Touw, the same find above 65: **vrijstelling**.

`case4-hout-op-zeventig.json`: J1, 70 jaar, Hout: not Blik; 70 > 65, but Hout
is none of Touw, Glas and Kurk: **geen vrijstelling**. J2, 70 jaar, Glas:
**vrijstelling**.

A flat reading of all bullets as one 'ten minste één' list gives J1 of case 3
and J1 of case 4 the vrijstelling; the third level read as part of the 'alle'
group (every one of Touw, Glas and Kurk) takes it from J1 and J2 of case 2.
Each case carries a vrijstelling, so none survives a run that evaluates
nothing (gate 7).

## Executors

ref, clingo and both Drools branches replay every case (PLAYED 4).
