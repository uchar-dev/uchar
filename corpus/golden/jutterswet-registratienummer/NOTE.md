# jutterswet-registratienummer — the exact-digit test

A Jutter carries a registratienummer, a text. The rule `volledig registratienummer` makes a Jutter geregistreerd
"indien zijn registratienummer numeriek is met exact 9 cijfers"; the rule `afwijkend registratienummer` makes it
afwijkend under the written negation, "indien zijn registratienummer niet numeriek is met exact 9 cijfers". On the
wire both conditions are a `Predicate` of kind `exactDigits` with `digits` 9, the second `negated`.

The test is true iff the operand is a text of exactly 9 characters, each a decimal digit 0–9. A leading zero is a
digit; a sign, a space or a letter fails the test. A leeg operand fails it rather than propagating, and the negation
complements the verdict, leeg included, so a Jutter without a registratienummer is afwijkend.

## The hand calculation

| case | registratienummer | length | all digits | geregistreerd | afwijkend |
|---|---|---|---|---|---|
| case1-negen-cijfers | `123456789` | 9 | yes | **present** | absent |
| case2-voorloopnullen | `000012345` | 9 | yes | **present** | absent |
| case3-te-kort | `12345678` | 8 | yes | absent | **present** |
| case4-te-lang | `1234567890` | 10 | yes | absent | **present** |
| case5-letter | `12345678a` | 9 | no | absent | **present** |
| case6-minteken | `-12345678` | 9 | no | absent | **present** |
| case7-spatie | `1234 5678` | 9 | no | absent | **present** |
| case8-leeg | leeg | — | — | absent | **present** |

Every case is consistent.

## Provenance

Own work, 2026-11-11, written as `source.rgs` and emitted by the RegelSpraak frontend. The test's meaning is the
Drools runtime's `rsNumeriekMetExact`, which the distribution's scenario test of a nine-digit bsn asks for. Measured
at admission: verifier PASS; ref and both Drools branches play all eight cases. The ASP backend refuses the golden
(`CLINGO-046`: the test reads the characters of a text, and the encoding has no string functions), listed in gate 6's
`EXPECTED_CHANNEL`.
