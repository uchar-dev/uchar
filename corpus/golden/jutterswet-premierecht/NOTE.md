# jutterswet-premierecht — prepositions and verbs in declaration heads

The golden of the names this frontend reads in a declaration head
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "Namen in
declaratiekoppen"). `source.rgs` declares three names the frontend cut short
before, each written as it reads:

- the object type `Recht op premie`, plural `Rechten op premie`: a
  preposition after the first capitalised word, in the head and in the
  plural;
- the role `recht op premie`, plural `rechten op premie`, filled by
  `Recht op premie`: a preposition in the role's plural and in its filler;
- the fact type `jutter heeft premierecht`: a verb in a fact-type name.

The rules read the type name where rule text reads a name: the target
`De premie van een Recht op premie`, the reading `het vondstgewicht van het
Recht op premie`, the conditions `indien het vondstgewicht van het Recht op
premie groter is dan 2` and `indien het Recht op premie toegekend is`, and
the role's plural in a sum and in a count with a filter, `alle rechten op
premie van de Jutter`. `scenario.rgs` writes the instances as `een Recht op
premie R1`; the frontend's `--cases` mode lowers its two scenarios to the two
case files, and gate 16 regenerates them from it byte for byte.

## Before: where the heads were cut

The frontend built at the commit before this work refused the source at its
heads (real output, the three syntax errors of the run):

    ERROR (line 8): syntaxfout: mismatched input 'op' expecting {RPAREN, UPPER_ID, LOWER_ID}
    ERROR (line 13): syntaxfout: extraneous input 'heeft' expecting {BENAMING_START, TOT_EN_MET, NUMBER, …}
    ERROR (line 15): syntaxfout: mismatched input 'op' expecting {RPAREN, UPPER_ID, LOWER_ID}

Line 8 is the object type's head: the name stopped at `op`, the rest was read
as an attribute whose plural could not hold `op`. Line 13 is the fact type:
its name stopped at `heeft`, and the role absorbed `premierecht`, which a
warning of that run guessed at (`de rolnaam 'premierecht de rechthebbende'
bevat een lidwoord (de/het) na een zelfstandig woord`). Line 15 is the
role's plural. Every rule that reads the type then failed to resolve as well.
Now the source emits `ir.json` with an empty diagnostics list and the
verifier answers `verifier: PASS`.

## The rules

    Een Recht op premie is toegekend
    indien het vondstgewicht van het Recht op premie groter is dan 2.

    De premie van een Recht op premie moet berekend worden als het vondstgewicht
    van het Recht op premie maal het premietarief
    indien het Recht op premie toegekend is.

    Het premiebedrag van een Jutter moet berekend worden als de som van de premie
    van alle rechten op premie van de Jutter of 0 als die er niet zijn.

    Het aantal toegekende rechten van een Jutter moet berekend worden als het
    aantal rechten op premie van de Jutter die toegekend zijn.

    Een Jutter is premiegerechtigd indien zijn premiebedrag groter is dan 20.

The sum and the count carry the role binding `jutter heeft premierecht` /
`recht op premie` as their `sourceRole`: the plural `rechten op premie`
binds the role.

## Hand computation

`case1-tarief-vier.json`, premietarief 4:

| recht | vondstgewicht | toegekend (> 2) | premie (vondstgewicht × 4, where toegekend) |
|---|---|---|---|
| R1 | 5 | **yes** | **20** |
| R2 | 2 | **no** (2 is not greater than 2) | **leeg** |
| R3 | 3 | **yes** | **12** |
| R4 | 1 | **no** | **leeg** |

| jutter | rechten op premie | premiebedrag | aantal toegekende rechten | premiegerechtigd (> 20) |
|---|---|---|---|---|
| J1 | R1, R2, R3 | 20 + 12 = **32** (the leeg premie of R2 is skipped) | **2** | **yes** |
| J2 | R4 | **0** (the one premie is leeg, so the fallback) | **0** | **no** |
| J3 | — | **0** (no right at all, the fallback) | **0** | **no** |

`case2-tarief-vijf-op-de-grens.json`, premietarief 5: R5 (vondstgewicht 4),
R6 and R7 (3 each) are all toegekend, with premie **20**, **15** and **15**.
J4 holds R5: premiebedrag **20**, which is not greater than 20, so J4 is
**not** premiegerechtigd, with **1** toegekend recht; J5 holds R6 and R7:
premiebedrag **30**, premiegerechtigd, **2** toegekende rechten. Case 1's R2
sits on the edge of the characteristic, case 2's J4 on the edge of the
threshold: each catches a `groter of gelijk` misreading of its rule.

Both cases also expect `consistent`: the document has no consistency rule.
The input carries no premie, premiebedrag, count or characteristic, so a
no-op run fails both cases.

## The square

`ref` and the ASP backend replay both cases (`oracle: PASS (18
expectations)`, `clingo: PASS (18 expectations)`; 13 for the second case),
and both Drools branches play them (`PLAYED 2`, exec-model and DRL text).
