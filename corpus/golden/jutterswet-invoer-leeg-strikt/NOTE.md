# jutterswet-invoer-leeg-strikt — scalar attributes stated to have no value (`attributesEmpty`)

A Vondst under `Afleiden uit leeg: nee` (`inferFromEmpty: false`, RUNTIME-SEMANTICS ch. 1.5, "The case"), with an
optional input attribute written with a space and a hyphen (`gewenste afhaal-dag`), an initialisation-only attribute
(`strandloon`), a derived attribute without an initialisation (`vindloon`) and a derived attribute with an
initialisation below its rule (`bergpremie`). Its twin `corpus/golden/jutterswet-invoer-leeg` carries the same rules in
an ordinary document. Own Jutterswet-flavoured casus, rights-free.

## Casus (own text)

> **Art. 3k (opslagtermijn en vrijgave na berging).** De opslagtermijn van een vondst bedraagt standaard 30 dagen, met
> dien verstande dat zij op de gewenste afhaal-dag wordt gesteld indien een gewenste afhaal-dag is opgegeven. Indien
> geen gewenste afhaal-dag is opgegeven, is de vondst direct vrijgegeven. Bij een taxatie boven 1000 bedraagt het
> vindloon 10 en de bergpremie 20; zonder die verhoging wordt de bergpremie op 4 gesteld. Het strandloon bedraagt bij
> aanvang 5.

## Source (`source.rgs`) and scenarios (`scenario.rgs`)

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the five case files are the lowering of
`scenario.rgs` (gate 16), whose expectations are computed by hand. A bullet `- de|het <attr> is leeg` in a situation
writes `<attr>` in `attributesEmpty` directly after `attributes`, and in a `gebaseerd op` chain replaces an inherited
value in place (and is replaced in turn by a value in a later block).

## Cases (hand-computed, absolute)

| case | input (`V1`, and `V2`/`V3` in case 5) | gewenste afhaal-dag | opslagtermijn | direct vrijgegeven | vindloon | bergpremie | strandloon |
|---|---|---|---|---|---|---|---|
| case1-invoer-weggelaten | `taxatie = 50`; `gewenste afhaal-dag` and `strandloon` omitted | open | open (`gevuld` exception undetermined, base 30 displaced) | open (`leeg` condition undetermined) | settled `leeg` | 4 | open (initialisation waits on open input) |
| case2-invoer-vaststaand-leeg | `taxatie = 50`; `gewenste afhaal-dag`, `strandloon` in `attributesEmpty` | settled `leeg` | 30 (`gevuld` exception false, base 30 decides) | holds (`leeg` condition true) | settled `leeg` | 4 | 5 (initialisation at its turn) |
| case3-leeg-vervangen-door-waarde | based on `leeg gelaten`, overridden with `gewenste afhaal-dag = 14`, `strandloon = 8` | 14 | 14 (exception decides) | does not hold | settled `leeg` | 4 | 8 (filled input stands above initialisation) |
| case4-waarde-vervangen-door-leeg | based on `met opgave`, overridden with `gewenste afhaal-dag is leeg`, `strandloon is leeg` | settled `leeg` | 30 | holds | settled `leeg` | 4 | 5 |
| case5-afgeleid-met-is-leeg | `vindloon`, `bergpremie` in `attributesEmpty` on `V1` (`taxatie` open), `V2` (`taxatie = 50`), `V3` (`taxatie = 2000`) | settled `leeg` | 30 | holds | `V1` open, `V2` settled `leeg`, `V3` 10 | `V1` open, `V2` 4 (initialisation below writer not blocked), `V3` 20 | `V1` 5 |

`ref`, `clingo` and both branches of the Drools backend (`exec-model` and `DRL-text`) play all five cases and agree on
their readings and end states.
