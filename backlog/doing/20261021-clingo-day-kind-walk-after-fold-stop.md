# clingo-day-kind-walk-after-fold-stop: clingo meldt de dagsoortwandeling van een kwantorlid na de stop

*Vondst van de kaart `fold-contributor-verdicts` (2026-10-21), gemeten met een proefdocument op de kaarttak. Luid: clingo
geeft UNSATISFIABLE met een fout waar ref een antwoord geeft; geen stil fout antwoord. Geen golden draagt de vorm.*

## Aanleiding

RUNTIME-SEMANTICS ch. 3.9a: "a member never reached produces no fault". In een samenstelling (ch. 5.10) bewaakt de
prefix-wacht van de clingo-backend een lid na de stop, ook bij een dagsoortwandeling (EF-009); in de matrix van een
kwantorvouw niet: het hulpatoom van de wandeling wordt voor elk lid van het bereik afgeleid. Proef (bewaard in
`uchar-dev/werk/defaults-decider/u5/probes/kwantor-dagsoortwandeling/`, `ir.json` en `case1.json`): een vouw
`alle [gekeurd ; <dagsoortwandeling zonder dag binnen de grens>]` waarvan lid 1 niet gekeurd is; ref geeft PASS, clingo
`UNSATISFIABLE — … viol("day-kind walk: no day of the kind within 40000 calendar days …","W2")`. De tegenproef in een
samenstelling (`u5/probes/samenstelling-dagsoortwandeling`) is in clingo groen.

Daarnaast, als ontwerp en buiten deze kaart: clingo geeft EF-011 (geen rekendatum) en EF-019 (een onopgeloste
voorwaarde) als vertaalfout, ook voor een lid dat nooit gelezen wordt; samen met Drools' lezing van een ongebonden
parameter als leeg (gate 8) is dat de reden dat geen golden een fout na de stop draagt (ch. 5.12, "The artifacts").

## Doel

De clingo-vertaling van de matrix van een kwantorvouw bewaakt de dagsoortwandeling van een lid met de stop van de vouw,
zoals de prefix-wacht dat in een samenstelling doet, zodat een lid na de stop geen EF-009 geeft.

## Acceptatie

- (a) Rood eerst: de proef als golden (kanaal `consistent`, met een controlecase waarin het lid wel bereikt wordt en
  EF-009 geeft, als rode case), rood in clingo vóór de fix, groen in ref en Drools.
- (b) Gates 5, 6, 8, 12 en 17 groen; de eigen suite van de clingo-backend groen.

May touch: `backends/clingo/`, één golden en één rode case, deze kaart.

Out of scope: EF-011 en EF-019 als vertaalfout (ontwerp van de clingo-backend).

## Result (2026-10-21)

Tak `card/clingo-day-kind-walk-after-fold-stop`, vanaf main `4f283d9d`. Geraakt: `backends/clingo` (`lib/translate.ml`,
`test/test_clingo.ml`, `README.md`), één golden (`corpus/golden/jutterswet-keurtermijn`), één rode case
(`corpus/red/jutterswet-keurtermijn-bereikt`), `scripts/check-drools.sh` (`EXPECTED_PLAYED`), en als voorstel
`spec/RUNTIME-SEMANTICS.md` ch. 5.12 en `spec/ASP-TRANSLATION.md` (zie "Buiten May touch"). Niet geraakt: `ref/`,
`backends/drools`, `spec/verifier/`, `CLAUDE.md`, `spec/PLAN.md`, `spec/CONSTRUCTS.md`.

- `8b9d1637` de kaart naar `doing/`.
- `c3a300aa` de golden `jutterswet-keurtermijn` (vier cases, `NOTE.md`) en de rode case `jutterswet-keurtermijn-bereikt`;
  gate 12 `EXPECTED_PLAYED` 262 → 263.
- `e6f72b30` clingo: de hekken van een dagsoortstap dragen het bereik van de plaats; twee frames in de suite; README [V5].
- `82d77bca` voorstel: RUNTIME-SEMANTICS 5.12 (de grens over een fout na de stop) en ASP-TRANSLATION (de dagsoortduur).

### De golden en de rode case

Handgeschreven IR uit de proef van `fold-contributor-verdicts` (`u5/probes/kwantor-dagsoortwandeling`), met één
correctie: de vergelijking `ligt na` is `later`, niet `greater`. Met `greater` weigert Drools de regel op beide takken
(`EXCLUDE DROOLS-261`, "a Comparison greater between DateInDays and DateInDays …: the types of the two sides do not
allow this comparison"); ref en clingo lazen `greater` over twee datums wel. Met `later` speelt Drools.

```
Een keurdag is een dag waarvoor geldt dat het jaar uit de dag gelijk is aan 2026.
Een Ploeg is inzetbaar indien alle wachten van de Ploeg aan alle volgende voorwaarden voldoen:
  - hij is gekeurd;
  - zijn keurdatum plus zijn keurtermijn ligt na de rekendatum.      (keurtermijn in keurdagen)
Het wachttal van een Ploeg moet berekend worden als het aantal wachten van de Ploeg.
```

Rekendatum 2026-06-01. Een stap vanaf een datum in 2027 vindt geen keurdag binnen 40 000 dagen (EF-009).

| case | de wachten (gekeurd, keurdatum + keurtermijn) | vouw | kanaal |
|---|---|---|---|
| 1 | W1 (nee, 2026-03-01 + 100); W2 (ja, 2027-03-01 + 1) | onwaar, stop bij W1 | consistent |
| 2 | W1 (ja, 2026-03-01 + 100 = 06-09); W2 (ja, 2026-05-01 + 60 = 06-30) | waar | consistent |
| 3 | W1 (ja, 06-09); W2 (ja, 2026-01-10 + 10 = 01-20); W3 (ja, 2027-03-01 + 1) | onwaar, stop bij W2 | consistent |
| 4 | W1 (ja, 06-09); W2 (ja, 2027-03-01 + 1) in geen ploeg | waar | consistent |

Case 4 (niet in de proef) is het bereik: W2 is lid van geen ploeg; ook die viel in clingo. De rode case: W1 en W2
gekeurd, W1 waar, dus W2 bereikt → `expectedFault {code: EF-009, params: {reach: 40000}}` (`from` noemt clingo niet,
ch. 5.7b, dus niet gepind). ref, clingo en Drools (`DroolsRedMain`) geven EF-009; ref en Drools ook `from` 2027-03-01.

De lezing (gate 17), ref en Drools op beide takken gelijk:

```
case1  … QuantifierFold false contributors ["W1" false reached; "W2" fault EF-009 unreached]
case2  … QuantifierFold true contributors ["W1" true reached; "W2" true reached]
case3  … QuantifierFold false contributors ["W1" true reached; "W2" false reached; "W3" fault EF-009 unreached]
case4  … QuantifierFold true contributors ["W1" true reached]
```

De eerste golden met een fout van een bijdrager na de stop in de vergelijking van gate 17 (wat
`fold-contributor-verdicts` voor EF-011 vroeg en niet haalde).

### (a) Rood eerst

clingo op de vertaling van vóór de fix (`u6/logs/red-first.log`, `u6/logs/red-before-all.log`); ref en Drools groen:

```
case1-stop-voor-de-wandeling.json
EF-009: clingo: UNSATISFIABLE — a fail-fast guard fired: viol("day-kind walk: no day of the kind within 40000 calendar
days (the walk reaches no further; a day kind whose next occurrence lies beyond the reach is refused, not diagnosed)","W2")
case2-beide-leden-gelezen.json        clingo: PASS (3 expectations)
case3-het-tweede-lid-beslist.json     EF-009: clingo: UNSATISFIABLE — … viol("day-kind walk: …","W3")
case4-een-wacht-buiten-de-ploeg.json  EF-009: clingo: UNSATISFIABLE — … viol("day-kind walk: …","W2")
red case1.json                        CODE EF-009 {"reach":"40000"} … clingo: expected fault observed (EF-009)
```

ref: `oracle: PASS (3 expectations)` op alle vier; Drools `PLAYED 4` op beide takken; de tegenproef in een samenstelling
(`u5/probes/samenstelling-dagsoortwandeling`) blijft PASS. De suite met de nieuwe frames op de oude vertaling
(`u6/logs/suite-red-1.log`): `FAIL a quantifier fold stops before a member whose day-kind walk faults (control): clingo
pins the values (UNSATISFIABLE — a fail-fast guard fired)`, `2202 checks, 1 FAILURES`.

### De fix

`dk_date_plus` (de §6.11-stap) krijgt de knoop van de stap (`~node`) en neemt het bereik van de plaats,
`ctx.param_reach node`, op in elk hek van de stap: de zes regels van het bereikhek (EF-009), de twee vensterlimieten en
de vensterwacht. Het bereik rijdt mee op de precondities `dkiN(X)` en de body van de vensterwacht. Dat is hetzelfde
bereik waarmee de deponeringen op het §9.5-kanaal (EF-005, EF-031) en de fail-fast van een ongebonden parameter al
werken: in de matrix van een kwantorvouw `isa(S,ot), <bereik>, kvrN(S,X)` (de stop, `emit_kv_reach`), in een vouwlid
het onderwerp en zijn bereik, in een default de hogere defaults die niets besloten; op regelniveau `[]`. De waarderegels
van de stap (`dkw`, `dkb`, `dkz`, `dkp`) wachten niet op het bereik: `kvr` wordt uit de atomen van de matrix gelezen,
en een waarde achter `kvr` sloot een lus door de telling van de vouw (onder `none` niet-monotoon). De `viol/2`-atomen van de
stap leest niemand behalve de integriteitsregel, dus het bereik in die hekken opent geen lus. In een document met een
object- of feitcreatie (`doc_dynamic`) zegt `kvr` "in elke toegestane volgorde"; daar blijven de hekken zoals ze waren,
voor elk lid, bereikt of niet (luid, nooit stil): README [V5], met een frame in de suite.

Gemeten: de vertaling van alle 884 bestaande case-bestanden van `corpus/golden` en `corpus/red` is bytegelijk aan die van
vóór de fix (een dump van `Translate.translate` per case, vergeleken met `diff -rq`); alleen de vier cases van de nieuwe
golden en de rode case veranderen. In case 1 krijgen de acht hekken en de twee vensterwachten
`isa(S,"ploeg"), role(S,"wachten",X1), isa(X1,"strandwacht"), kvr7(S,X1)`, en de helpers `kvr7_*` worden geschreven.

Suite: twee frames, 2198 → 2202 checks. `both_answer` op het document van de golden met case 1 (de controle, rood vóór
de fix) en `pinned_loud ~entry:"V5"` op hetzelfde document met een creatie van een Keurrapport per Ploeg die niets aan
het bereik toevoegt: ref speelt, clingo weigert met het bereikhek.

### Andere foutsoorten in een kwantormatrix

clingo meldt tijdens het oplossen met een EF-code: EF-006 (twee redenen), EF-009, EF-014, EF-015, EF-016, EF-017
(`Translate.viol_code`), en deponeert EF-005 en EF-031. In een matrix kunnen EF-005, EF-006, EF-009, EF-014 en EF-031
voorkomen; EF-015, EF-016 en EF-017 horen bij creaties en schrijvers.

- EF-005, EF-031 (deponeringen) en EF-014 (ongebonden parameter) dragen het bereik al (`param_reach`); gemeten in
  `fold-contributor-verdicts` (golden `jutterswet-keurgemiddelde`, de proef met EF-014).
- **EF-006, "non-exact division", is een andere fout, niet dezelfde fix.** Proef (scratchpad, de matrix van
  `jutterswet-keurgemiddelde` zonder de afronding, W2 met 7 / 2): clingo geeft `EF-006: clingo: UNSATISFIABLE — …
  viol("non-exact division (an explicit rounding is mandated before precision is lost)","W2")` zowel na de stop als
  wanneer W2 bereikt wordt; ref speelt beide (`oracle: PASS`), want ref vergelijkt het exacte rationale getal en schrijft
  niets. Het bereik zou alleen de helft na de stop dichten. Daarbij draagt die weigering de code van EF-006 ("a written
  value does not fit the declared decimals of its target") terwijl er niets geschreven wordt. Voorstel: een kaart voor
  `backends/clingo` (een kale deling in een voorwaarde: exact vergelijken, of weigeren met een eigen `CLINGO-###`-code
  in plaats van EF-006).
- Grenzen van clingo zonder EF-code in een matrix (`arithmetic range exceeded`, de pre-epoch-hekken, `negative operand
  under rounding/rounded division/money multiplication is not translated`, `easterSunday of a year before 1`, de
  vensterwacht van `dk_duration`): hetzelfde patroon (luid voor een lid na de stop), niet meegenomen. Het
  hek `arithmetic range exceeded` wordt negatief gelezen door `sign_conds` (`not viol("arithmetic range exceeded
  …",S)`); het bereik daarin zou een lus door de matrix sluiten, dus daar is het niet dezelfde fix. De andere zijn
  limieten, geen foutsoort, zonder getuige.
- EF-011 en EF-019 als vertaalfout: buiten de kaart.

### Restanten, vastgelegd

- README [V5] (boven): in een document met een creatie blijft het hek luid voor elk lid.
- De stop wordt op het fixpunt gelezen, net als de prefix-wacht van een samenstelling: een wandeling die ref alleen in
  een tussenpas van een stratum bereikt (zoals README [S3] voor de parameter) vuurt niets. Dezelfde rest als de
  prefix-wacht voor alle hekken van een samenstellingslid; geen nieuwe soort, niet apart gepind.

### Buiten May touch, met reden

1. `spec/RUNTIME-SEMANTICS.md` ch. 5.12, "Three boundaries": de zin "So does a fault of a contributor after the stop:
   … no such golden passes gate 6, since … that of a day-kind walk (EF-009) for every member of the range, read or not
   …" is na de fix onwaar. Voorstel (`82d77bca`): "A fault of a contributor after the stop is inside it for one kind:
   the golden `jutterswet-keurtermijn` … reads `"W2" fault EF-009 unreached` and `"W3" fault EF-009 unreached` on both
   sides … The other kinds stay outside it: … no golden with such a fault passes gates 6 and 17, since … (EF-011) …
   (EF-019) … and the Drools backend reads … (EF-014) as leeg, so its reading would carry a verdict where ref's carries
   the fault."
2. `spec/ASP-TRANSLATION.md`, de dagsoortduur: één alinea over het bereik op de hekken van een stap en de luide rest
   bij een creatie (`82d77bca`).
3. `scripts/check-drools.sh`: `EXPECTED_PLAYED` 262 → 263 met een redenregel.
4. **Niet gedaan, voorstel**: `corpus/golden/jutterswet-keurgemiddelde/NOTE.md`, "Why a deposit and not a fault", zegt
   dat de ASP-backend EF-009 "for every member of the range, read or not" geeft. Dat is na deze kaart niet meer waar.
   Voorgestelde vervanging van "and EF-009 (the day-kind walk) for every member of the range, read or not, so the case
   fails gate 6;" door "so the case fails gate 6 (the day-kind walk, EF-009, fired for every member of the range until
   the card `clingo-day-kind-walk-after-fold-stop`, whose golden `jutterswet-keurtermijn` carries that fault after the
   stop);".

### Gates, zelf gedraaid

Op de tak (code op `e6f72b30`; `82d77bca` raakt alleen spec-proza):

- clingo-suite: `test_clingo: 2202 checks, 0 failures` (was 2198).
- gate 1 PASS (op `82d77bca`, na het verwijderen van het tijdelijke dumpprogramma).
- gates 2, 3, 4, 7 PASS met de golden en de rode case, vóór de commit (`u6/logs/gates-2347-1.log`): 353 documenten
  valideren en gaan bytegelijk heen en terug; 353 golden verifier-schoon, 511 negatieven verworpen; 889 case-bestanden;
  798 cases weerleggen de no-op (was 794).
- gate 6 PASS, 222 s: 843 cases, suite 2202 (`u6/logs/check-clingo-1.log`).
- gate 5 PASS: ref-suite 1311, 843 cases (`u6/logs/check-oracle-1.log`).
- gate 8 PASS: 46 rode cases in ref en clingo, 86 codecontroles, 124 parametercontroles; Drools-been 23 op de fout gepind,
  30 parametercontroles (`u6/logs/check-red-1.log`).
- gate 12 `--fast` PASS, 163 s: played 263, 49 niet-spelende goldens bij naam (`u6/logs/check-drools-fast-1.log`).
  Niet gedraaid onder `--fast`: de JUnit-suite en de DRL-tekstsweep. De golden speelt apart gemeten op beide takken
  (`java -jar … [--drl] ir.json case*.json`: `PLAYED 4` en `PLAYED 4`).
- gate 17 PASS, 403 s: 843 case-bestanden getraceerd; per tak 263 goldens, 714 case-bestanden, 3834 lezingen, 13979
  knoopregels waarvan 3118 `unreached`, 322 met bijdragers, 76 met een tijdlijn, 108 bijdragers van ref niet bereikt
  (was 106: W2 in case 1 en W3 in case 3); afwijkingen 0 (`u6/logs/check-readings-1.log`).
- gate 13 PASS (246 s) en gate 15 PASS (18 s; 275 van 312 goldens vergeleken) (`u6/logs/gates-13-15-1.log`).
- Niet gedraaid: gates 9, 10, 11, 14, 16 en de volledige gate 12; de hoofdsessie draait de set.

### Voorgestelde tekst voor `CLAUDE.md`

Geen. Gate 12 noemt het getal van `EXPECTED_PLAYED` niet in `CLAUDE.md`; geen gate eist iets nieuws en geen vloer
verschuift (de vloeren van gates 2–8 en 17 zijn ondergrenzen; de nieuwe golden telt erbij). De suitevloer van gate 6
(`EXPECTED_MIN_SUITE_CHECKS=83`) blijft.

Voor `spec/CONSTRUCTS.md`: geen rij (een fix van een uitvoerder, geen vorm en geen afwijking van v0). Voor
`spec/PLAN.md`: geen scopewijziging; de EF-006-vondst hierboven is een kaart voor `backlog/new/`.

### Volledige set, door de hoofdsessie

Op `02c2120f` (`werk/defaults-decider/u6/gates-1`): alle zeventien `rc=0`, `EXIT 0`. Gate 12 842 s, played 263 op
beide takken, JUnit 1649; gate 17 388 s: 843 case-bestanden getraceerd, 263 goldens en 714 case-bestanden vergeleken,
108 niet-bereikte bijdragers van ref, afwijkingen 0. De hoofdsessie werkte de `NOTE.md` van `jutterswet-keurgemiddelde`
bij (`02c2120f`, de voorgestelde tekst), landde de spec-voorstellen en zette de EF-006-vondst als kaart in
`backlog/new/20261021-clingo-nonexact-division-in-condition.md`.
