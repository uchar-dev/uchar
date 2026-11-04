# jutterswet-invoer-naast-objectcreatie — the values of a making as the input of the object made

"Een Jutter heeft een melding met waarde gelijk aan zijn vangst en toeslag gelijk aan zijn bonus" makes a Melding for
every Jutter; "De waarde van een Melding moet gesteld worden op zijn toeslag" writes the waarde of every Melding; "De
terug van een Jutter moet berekend worden als de som van de waarde van alle meldingen van de Jutter" reads the made
object back. The member values of a creation are the input of the object it makes (RUNTIME-SEMANTICS ch. 5.5, 7.8):
on a made Melding the rule displaces the value of the making where it decides, as it displaces case input on an input
Melding, and the value of the making stands where the rule fires with a leeg toeslag.

| instance | input | outcome |
|---|---|---|
| J1, vangst 7 | — | terug **7**: its Melding has waarde 7 from the making and no toeslag, so the rule decides nothing |
| J2, vangst 7, bonus 99 | — | terug **99**: its Melding has toeslag 99, and the rule displaces the waarde 7 of the making |
| M1, toeslag 30 | waarde 40 | waarde **30**: the rule displaces the input |
| M2 | waarde 40 | waarde **40**: the rule fires with leeg, the input stands |
