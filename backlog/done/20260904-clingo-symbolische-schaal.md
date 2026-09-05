# clingo crasht op een symbolische term in de schaal-deling — nette fault

Goal: de gelede EERSTE/LAATSTE over een SumType-element laat clingo
hard crashen: `Fatal error: exception Division_by_zero`, exitcode 2,
zonder file/line — geen fault, een crash. Gemeten (meetfase
doorgeeffold, 2026-09-04): `backends/clingo/lib/translate.ml` deelt in
de gelede-fold-arm (~r. 1485) `m / s` waar `s = 0`, de schaal die
`expr_term` (~r. 114-120) aan een constructorterm geeft; de
`is_symbolic_operand`-fence op dezelfde arm (~r. 1456) dekt Tekst,
Booleaan en Enumeratie maar niet SumType. Fail-fast betekent een
reden-dragende `TRANSLATION FAULT`, nooit een ongevangen exceptie: de
fence leert SumType kennen (of de schaal-nul wordt zelf de luide
weigering), en de kaart loopt na of méér plekken een symbolische term
door een numerieke schaal-deling laten lopen (grep op de
schaal-delingen in translate.ml; elke vondst zelfde behandeling of een
reden in commentaar).

Onder het orderbaarheidsbesluit van 2026-09-04
(`20260902-doorgeeffold-orderbaarheid.md`) wordt de dragende vorm
ill-formed, maar een crash-pad hoort ook onbereikbaar-net te zijn: de
translator mag op geen enkele invoer een ongevangen exceptie gooien.

Acceptance:
- Rood-eerst: de crash gereproduceerd (letterlijke uitvoer in de
  kaart); ná de fix een reden-dragende TRANSLATION FAULT op dezelfde
  invoer.
- De sweep over de schaal-delingen met per plek fence of reden.
- Alle dertien gates groen; geen bestaande pin beweegt (gemeten
  verwachting: geen corpusdocument raakt de arm).

May touch: `backends/clingo/lib/**`, `backends/clingo/test/**`
(additief), `backlog/`.

Out of scope: de orderbaarheidsclausule zelf (eigen kaart), ref,
frontends, corpus.

---

Result (2026-09-05): **geland.** De crash is een reden-dragende
TRANSLATION FAULT, de snede zit op de schaal in plaats van op de
annotatie, en de sweep dekt elke schaal-deling in `translate.ml`. Alle
dertien poorten groen, geen enkele bestaande telling beweegt.

## De reproductie, letterlijk

Het dragende document staat al in het corpus als negatief:
`corpus/negatief/wf038-eerste-somtype-geleed/ir.json` — de gelede
`EERSTE` over twee operanden van het somtype `stand`. Met een casus
ernaast (buiten de repo gehouden; de negatief-zone draagt er geen):

    $ clingo_runner corpus/negatief/wf038-eerste-somtype-geleed/ir.json case.json
    Fatal error: exception Division_by_zero
    EXITCODE=2

Twee feiten, allebei genoteerd zoals de kaart vroeg:

1. **De translator crashte.** Geen fault, geen regel, geen file/line —
   een ongevangen exceptie en exit 2.
2. **Het document is ill-formed.** Sinds de WF-038-ordeningsclausule
   (geland 2026-09-05) wijst de verifier precies deze vorm af:

       WF-038 FAIL /resolvedRegels/0/waarde/argumenten/0: operand of the
       EERSTE ordering fold is sumtype, must be a number or a date
       WF-038 FAIL /resolvedRegels/0/waarde/argumenten/1: operand of the
       EERSTE ordering fold is sumtype, must be a number or a date
       EXITCODE=1

   De kaart blijft daarmee overeind in plaats van te vervallen: de
   meting gaat over de translator, niet over de verifier. Een
   translator moet op élke invoer een reden geven, ook op invoer die
   buiten de verifier om binnenkomt.

Dezelfde aanroep ná de fix:

    TRANSLATION FAULT: a symbolic operand reaches an eerste/laatste fold:
    a sum-type or record value carries no numeric scale, so it cannot be
    harmonised against one (ref faults the operation; this arm refuses it
    rather than divide by the absent scale)
    EXITCODE=1

## De snede: de schaal, niet de annotatie

`numeric_scale ~what s` (`translate.ml`, naast `value_scale`) faalt luid
op `s < 1`. Schaal 0 markeert een SYMBOLISCHE term — een
constructorterm, een recordterm, en elk toekomstig datatype waaraan
`value_scale` geen numerieke schaal geeft. Zo'n term *dragen* is
legitiem (een vouw-accumulator, een sum-getypeerd doel, een
gelijkheidsjoin); hem *harmoniseren* is het niet, en juist daar wordt
door de schaal gedeeld.

Waarom niet de fence uitbreiden met SumType, de andere optie uit de
kaart: `is_symbolic_operand` en `numeric_ok` lezen `typ_of_expr`, dus de
wire-annotatie, terwijl de schaal uit de DECLARATIE komt
(`RUNTIME-SEMANTICS` §2.10 — een consument leidt af uit declaraties, wat
poort 13 mechanisch afdwingt). Een ontbrekende annotatie (`None` glipt
door `numeric_ok`) of een liegende annotatie loopt de annotatie-hekken
straal voorbij en tot in de deling. Een hek op de schaal kan dat niet,
en het dekt bovendien het symbolische type dat er nog niet is: dat faalt
op de dag dat het wordt toegevoegd, niet op de dag dat het crasht.

## De sweep — plek → verdict

Alle OCaml-delingen en -moduli in `translate.ml` nagelopen met commentaar
en strings weggestreept; 24 rekenplekken, plus de plekken die alleen ASP
uitschrijven (die deelt clingo zelf, met een `viol/2`-hek ervoor).

**Gehekt** (`numeric_scale`, op de plek waar de schaal binnenkomt):

| plek | arm |
| --- | --- |
| `harmonised` | zelf-hekkend, voor een aanroeper die het vergeet |
| gelede `eerste`/`laatste` | **de gemeten crash** |
| gedurende-clip-vergelijking | de `grens`-operand |
| `plus`/`min`/`verminderd met` | beide operanden |
| `Afgerond` | de basis (schaal is er óók modulus) |
| `Begrensd` | basis + beide grenzen |
| `translate_division` | deeltal + deler (worden gcd-operanden) |
| `translate_power` | de basis |
| numerieke gelijkheid | beide operanden |
| ordeningsvergelijking | beide operanden |
| `body_condition` | beide operanden (closed body) |
| closed-body `plus`/`min` | beide operanden |
| closed-body macht | de basis |
| `closed_division` | deeltal + deler |
| `value_condition` | beide operanden |
| `fitted` | de waardeschaal, ná de identiteitstak |

**Reden in commentaar** (een symbolische term kan er aantoonbaar niet
binnenlopen):

| plek | reden |
| --- | --- |
| `const_div_reduce` | beide aanroepers hekken `sl`/`sr`; `const_div` laat alleen `c > 0` toe, dus nooit `gcd 0 0` |
| `pow_le_max` | deelt door de basis-WAARDE, geen schaal; `k = 0` apart afgevangen |
| pro-rata `kn`/`kd` | `pow10 (attr_decimals …)`, en `attr_decimals` fault al op een sum-attribuut |
| verdeling-opschaling, uniciteitssleutel-opschaling | idem `pow10`, geen expressieschaal |
| sub-dag-factor `u`, `ms_per_day` | compile-time constanten |

Twee van de gehekte plekken deelden helemaal niet — ze antwoordden
**stil**. De ordeningsvergelijking en de closed-body-vergelijking met
twee symbolische operanden krijgen `m = max 0 0 = 0`, dus `s = m` en er
wordt niets gedeeld: clingo vergeleek constructortermen in zijn eigen
termorde waar `ref` de ongeordende vergelijking fault. Daar wint het hek
een weigering in plaats van een crash — een gevonden stille divergentie,
niet in de kaart voorzien.

`fitted` houdt zijn identiteitstak vóór het hek: schaal = doel = 0 is de
legitieme route van een sum-/structwaarde naar een even ongeschaald
doel, en pas daarna wordt een schaal een modulus.

## De getuige, rood eerst

De kaart vroeg te kijken of `faults_with` een crash überhaupt van een
fault onderscheidt. Gemeten: **hij telde een crash nooit als fault** (hij
ving alleen `Uchar_ref.Value.Fault`), maar hij nam wel de hele suite mee
— `Fatal error: exception Division_by_zero`, exit 2, en de telregel
`test_clingo: N checks, M FAILURES` verscheen helemaal niet. Een crash
was dus niet te onderscheiden van een suite die nooit gedraaid had.
`faults_with` heeft er nu een expliciete arm bij, zodat een crash één
genoemde failure is en de suite doortelt:

    rood:  FAIL a sum-typed operand under the articulated ordering fold:
           expected a loud fault, got a CRASH (Division_by_zero)
           test_clingo: 45 checks, 1 FAILURES
    groen: test_clingo: 45 checks, 0 failures

De getuige staat in de suite en niet in `corpus/rood`: het dragende
document is WF-038-ill-formed, dus geen enkele geldige route komt bij de
arm — en dat is precies waarom het hek gemeten moet worden op de plek
waar de verifier er níet voor staat.

## De metingen

Suite 44 → 45 checks (additief; het maintainer-akkoord op additieve
suitegroei staat). Alle dertien poorten, gedraaid ná commit:

| poort | uitslag |
| --- | --- |
| 1 self-contained | PASS |
| 2 ir-schema | PASS (176 bestanden valideren en round-trippen) |
| 3 verifier | PASS (176 golden clean; 287 negatief afgewezen op hun eigen regel) |
| 4 case-schema | PASS (427 casusbestanden) |
| 5 oracle | PASS (405 casussen exact herspeeld) |
| 6 clingo | PASS (405 casussen — ref ≡ clingo over het corpus) |
| 7 noop | PASS (369 weerleggen een niets-doener; 36 gepinde overlevers) |
| 8 rood | PASS (22 rode casussen; Drools-been 5 fault-gepind, 17 met reden niet gedragen) |
| 9 catala | PASS (201 checks; 12 goldens byte-identiek) |
| 10 regelspraak | PASS (171 tests; 30 rgs-goldens byte-identiek) |
| 11 toka-pariteit | PASS (29/29 ref, 29/29 clingo, 29 PLAYED Drools) |
| 12 drools | PASS (108 played) |
| 13 reftype-afleiding | PASS (64 annotaties getwist over 13 goldens; 0 overlevers) |

Alle tellingen gelijk aan `main`. Gericht nagemeten dat de gelede vouwen
die WÉL spelen niet bewegen — de twee goldens die de arm raken, alle zes
hun casussen:

    corpus/golden/jutterswet-hoogwaterpaar  case1/2/3  clingo: PASS
    corpus/golden/jutterswet-peildatum      case1/2/3  clingo: PASS

Die tweede draagt datums naast tweedecimalen, dus de echte harmonisatie
(`m / s` met s ∈ {1, 100}) loopt er nog steeds doorheen.

## Afwijkingen van de kaart

- **May touch uitgebreid** met `backends/clingo/test/**`: de kaart
  droeg dat pad niet in zijn eigen regel, terwijl acceptatiepunt 1 en de
  opdracht om de getuige te pinnen er niet zonder kunnen. Additief plus
  één extra arm in `faults_with`, die geen bestaande check van uitkomst
  verandert.
- **Gevonden buiten de opdracht:** twee stille divergenties (de
  ordenings- en de closed-body-vergelijking over twee symbolische
  operanden). Meegenomen in dezelfde snede.
