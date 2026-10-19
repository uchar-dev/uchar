# fold-filter-divides-by-zero

The twin of `corpus/golden/jutterswet-filterwacht`: the same document,
byte for byte, replayed on `J1` when `J1` IS actief. Both rules then reach
their fold ("het aantal vondsten van de Jutter die aan de volgende
voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan
2"), the filter is evaluated for every member of `J1`'s range, and for `V2`
(gewicht 9, deelnemers 0) it divides by a numeric zero. Since 2026-10-19
(card `zero-division-channel`, `RUNTIME-SEMANTICS.md` ch. 1.4 point 4) the
quotient is leeg, so `V2` does not pass the filter, and the division
deposits a §9.5 fact of kind EF-005 under the rule that reads the fold, on
`J1` (ch. 5.6): one for `zware vondsten` and one for `drukte`. The case pins
`vondstental` 2, `zware vondsten` 1, `J1` druk and both facts.

The golden twin pins that the same division does not happen where the
reading form does not reach the fold (ch. 5.10, ch. 3.4a); this case pins
that where it does, it deposits, under the reading form and not under the
member. Measured at that card: the ASP backend keyed a filter's deposit on
the member (`drukte × V2`) until the card keyed it on the reading form's
subject, as ref does.

Until 2026-10-19 this was a red case: all three executors faulted on the
division.
