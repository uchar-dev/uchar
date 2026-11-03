# jutterswet-gehele-jaar-gemaakte-koppeling — "gedurende het gehele jaar" over a link a fact creation makes

A Jutter reaches its Strandpost over the to-one role `standpost` of the fact type `standplaats`, and no case writes
that link: the fact creation "standplaats van een jutter" makes it from the input relation `aanmelding` (every
`aangemelde jutter` of a Strandpost becomes a `standjutter` of it). Three rules read the Strandpost over the made link:

- `wachtvergoeding`: the `dienstjaren` where "de standpost van de Jutter gedurende het gehele jaar bemand is"
  (`bemand` is a month-granular characteristic of Strandpost; the qualifier's grid is the year);
- `wachtkorting`: the same under the qualifier's negation;
- `dieptegeld`: the `dienstjaren` where "de diepte van de standpost van de Jutter ongelijk is aan 5", the same link
  read without the qualifier.

Before the creation the Jutter has no `standpost`. The qualified check then passes vacuously (RUNTIME-SEMANTICS
ch. 3.9: a failed to-one navigation contributes a null timeline), and the `diepte` read leeg is unequal to 5 (ch. 1.2),
so `wachtvergoeding` and `dieptegeld` decide on that state. The creation is in the stratum of the three rules (its
link is a positive edge to each of them), and after it the rules read the world with the link (ch. 5.5). What stands
is what the rules decide at the fixpoint (ch. 6.1, "A write is read at the fixpoint"): in case 4 the half-year
`bemand` fails the year bucket and `wachtvergoeding` is leeg; in cases 3 and 4 the `diepte` 5 makes `dieptegeld` leeg.

Evaluation date 2026-03-15, J1's `dienstjaren` 8; 2026 has 365 days.

| case | `aanmelding` | `bemand` | `diepte` | year 2026 | `wachtvergoeding` | `wachtkorting` | `dieptegeld` |
|---|---|---|---|---|---|---|---|
| case1-geen-aanmelding | none | [2026-01-01, 2027-01-01) | 5 | no link, null timeline | **8** | leeg | **8** |
| case2-geen-tijdlijn | J1 at P1 | none | 7 | no segments | **8** | leeg | **8** |
| case3-heel-jaar | J1 at P1 | [2026-01-01, 2027-01-01) | 5 | 365 of 365 | **8** | leeg | leeg |
| case4-half-jaar | J1 at P1 | [2026-01-01, 2026-07-01) | 5 | 181 of 365 | leeg | **8** | leeg |

Every case is consistent.
