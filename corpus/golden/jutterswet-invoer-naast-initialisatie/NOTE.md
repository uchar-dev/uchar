# jutterswet-invoer-naast-initialisatie — the input of a target between its writers and its initialisation

Three forms write the `taxatie` of a Vondst: "op zijn opgave indien hij gemeld is", "op zijn schatting indien hij
erkend is" and the initialisation "op 25 indien hij gemeld is". With the case input they are one stack
(RUNTIME-SEMANTICS ch. 7.8): the two computing rules decide where their value is filled, a rule that fires with a
leeg value decides nothing (ch. 3.11); below them the input stands; below the input the initialisation writes, where
the input leaves the target empty and no rule decides (ch. 5.4).

| case | instance | input | `taxatie` |
|---|---|---|---|
| case1-invoer | V1 gemeld, erkend, opgave 50 | taxatie 40 | **50** (the first rule decides, the second fires with leeg) |
| | V2 gemeld, erkend, schatting 60 | taxatie 40 | **60** (the second rule decides) |
| | V3 gemeld, erkend | taxatie 40 | **40** (both fire with leeg; the input stands, the initialisation is blocked) |
| | V4 | taxatie 40 | **40** (nothing fires) |
| case2-zonder-invoer | V1 gemeld, erkend, opgave 50 | — | **50** |
| | V2 gemeld, erkend, schatting 60 | — | **60** |
| | V3 gemeld, erkend | — | **25** (the initialisation) |
| | V4 erkend | — | **leeg** (the second rule fires with leeg, the initialisation's condition fails) |

Both cases refute the no-op guard: V1's 50 is not its input.
