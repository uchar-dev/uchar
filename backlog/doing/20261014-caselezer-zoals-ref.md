# caselezer-zoals-ref: de Drools-caselezer leest sleutels en labels anders dan ref

*Vastgelegd door de hoofdsessie bij de landing van `20261008-drl-teksttak-op-truth-maintenance` (2026-10-14), uit het
verslag van stap 2b en de lens op ronde 4 van die kaart. Bestaand gedrag van de caselezer in `cases/CaseRunner` (nu `cases/CaseInput`), op
beide takken; geen golden raakt het.*

## Aanleiding

Gemeten in stap 2b van die kaart, met een aangepaste case:

- **Een sleutel met een dubbele spatie.** "geschatte  waarde" (twee spaties) als attribuut in case1 van
  `anchor/jutterswet-waardestelling`: ref en clingo geven PASS; de Drools-takken weigeren met `EXCLUDE case sets
  undeclared attribute 'geschatte  waarde' on Vondst`. `ObjectType.attribute` vergelijkt met `equalsIgnoreCase` en
  vouwt geen witruimte.
- **Een sleutel die alleen in een niet-ASCII-hoofdletter verschilt.** "İŞ" voor het attribuut "İş" (eigen document
  `eigenschap/turks-lid`, case-invoer): ref en clingo geven `CASE ERROR: attribute … is not declared on "Jutter"`; de
  Drools-takken spelen (`PLAYED 1`). De caselezer aanvaardt hier ruimer dan ref.

Gemeld door de lens op stap 6c (2026-10-14): de caselezer zoekt een lid via de afgeleide Java-naam en toetst niet of
de gespelde naam gedeclareerd is. Een verwachting op "premie-som" voor het lid "premie som" geeft dezelfde getter
(`getPremieSom`), dus replay en scenariorapport slagen, waar ref de verwachting als niet-gedeclareerd weigert
(`ref/lib/case.ml`, r. 695–701 op 2026-10-14). Een tikfout in een scenario blijft zo onopgemerkt.

Gemeld door de lens op ronde 4: `CaseRunner` (r. 620–634 op `605fc574`; nu `CaseInput.beanFor`) geeft bij een tweede case-instantie met
hetzelfde label de eerste bean terug. Twee instanties met één label worden zo stil samengevoegd in plaats van als
ongeldige invoer geweigerd. Wat ref en clingo daar doen, is niet gemeten.

## Acceptatie (bij oppakken aanscherpen)

- (a) Leg vast welke regel de spec of het caseschema geeft voor het vinden van een sleutel (witruimte, hoofdletters) en
  voor een label dat twee keer voorkomt, en meet ref en clingo op elk van de drie gevallen.
- (b) De Drools-caselezer volgt die regel op beide takken; waar ref weigert, weigert hij met een zin die gate 8 kan
  pinnen.
- (c) Rode cases onder `corpus/red/` waar de invoer ongeldig is (`expectedFault` voor ref, clingo en drools), en een
  golden waar ref de invoer aanvaardt; rood eerst.

May touch: `backends/drools/**` (`cases/`, `schema/`), `corpus/red/**`, `corpus/golden/**`, `scripts/check-red.sh`
(**voorstel**, als de Drools-poot een regel nodig heeft).

Out of scope: `ref/`, `backends/clingo` (tenzij (a) daar een afwijking van de regel vindt; dan een eigen kaart).

## Result

Bouwer drools-residue, 2026-11-06, branch `card/drools-residue` (afgetakt van main `575de08e5`). Logs en
proefbestanden in `werk/drools-residue/` (`logs/`, `probe/caselezer/`).

**(a) De regel en de meting.** Het caseschema zegt "Attribute name (as declared)", "Role name (as declared)" en
"Case-local instance name; unique within the case"; `spec/schema/README.md` legt het label en het oplossen van namen
tegen de `ir.json` bij de caselezer van `ref/`. Die vergelijkt elke naam met `Checks.norm` (`collapse_ws` op spatie en
tab, `lowercase_ascii`: alleen A–Z gevouwen) en weigert bij het laden, zonder EF-code (`Case.fail`, "CASE ERROR"):
"duplicate instance label", "attribute … is not declared on …", "characteristic … is not declared", "instance objectType
… is not declared", "role key … does not resolve to a declared role". Clingo leest de case met dezelfde lezer en gaf op
elk geval dezelfde regel als ref. Gemeten op 19 proefcases (`probe/caselezer/meting-oud.log`, `meting-oud-2.log`), de
Drools-takken vóór de reparatie:
- twee instanties met label `v1`: ref en clingo CASE ERROR, beide Drools-takken voegden ze stil samen (FAIL op de
  waarde, met gelijke waarden zou het PLAYED zijn);
- invoersleutel met twee spaties en een objecttype met een spatie vooraan, een rolsleutel met een spatie achteraan: ref
  PASS, Drools `EXCLUDE DROOLS-046`, `DROOLS-045`, `DROOLS-049`;
- invoersleutel `geschatte-waarde`, objecttype `Vondsten`, rolsleutel `op-volgers`: ref CASE ERROR, Drools `EXCLUDE`
  (een weigering als "niet gedragen vorm");
- verwachtingssleutel `geschatte-waarde` en `İŞ` voor een gedeclareerd `İş` (invoer en verwachting): ref CASE ERROR,
  Drools `PLAYED 1` (de Java-naam van de spelling of `equalsIgnoreCase`);
- een onbekend kenmerk, in invoer of verwachting: ref CASE ERROR, Drools `ERROR` op een ontbrekende setter of getter.

**(b) De Drools-caselezer volgt ref, op beide takken.** `CaseRunner.attributeName`, `characteristicName` en het nieuwe
`objectType` zoeken de gedeclareerde naam met `Namen.norm` (dat al ref's `norm` is) en weigeren een naam die geen
declaratie is met een `IllegalArgumentException` in ref's woorden; `CaseInput` leest elke invoersleutel, elk
objecttype en elke rolsleutel (`declaredRoleKey`) daardoor, en elke verwachting (`attribute`, `cell`, `timeline`,
`characteristic`) ook. Een tweede instantie met een label is een `IllegalArgumentException` ("duplicate instance label").
De `RoleLink` houdt de geschreven rolsleutel, zodat een consument nog het veld van zijn verzoek kan noemen. Gemeten na
de reparatie (`probe/caselezer/meting-nieuw.log`): op alle 19 proefcases weigeren of spelen beide takken waar ref
weigert of speelt. DROOLS-045 en DROOLS-046 zijn `RETIRED` (register, `CLAUDE.md` gate 12). Gate 8 kan deze weigeringen
niet pinnen: ref geeft ze geen EF-code, en een `expectedFault` pint alleen een EF-code (open punt 1).

**(c) Getuigen, rood eerst.** Geen rode case (zie (b)). In de suite `CaseNameRuleWitnessTest` (vier rijen, beide takken):
rood op de oude code (`logs/card1-rood-bewaard.log`: 3 failures, 1 error), groen erna (`logs/card1-groen.log`). Golden
`jutterswet-keurunie/case2-spelling.json`: dezelfde invoer als `case.json` met elke naam anders gespeld (objecttypen
`JUTTER` en ` vondst`, rolsleutel `Aangemelde  Vondsten`, kenmerken `GEMERKT`/`Gekeurd`, verwachte attributen met tab,
hoofdletters en twee spaties), uitleg in `NOTE.md`. Rood op de oude jar: beide takken `EXCLUDE DROOLS-045`
(`logs/card1-golden-rood.log`); nu ref en clingo PASS (8 verwachtingen), beide Drools-takken `PLAYED 2`; `--noop`
faalt (gate 7). Gate 12 groen (`logs/card1-gate12.log`: 352 gespeeld op beide takken, JUnit 1877), gate 4 groen.

**Gewijzigde test.** `CaseInputWitnessTest.a_role_without_to_a_reference_field_is_refused_with_the_link` gebruikte de
niet-gedeclareerde sleutel `buur` als getuige van DROOLS-049. Zo'n sleutel is nu een casusfout (nieuwe rij
`a_key_that_names_no_declared_role_is_a_defect_of_the_case`); de rij van DROOLS-049 krijgt een gedeclareerde rol `buur`
van een tweede relatie (Post–Wachter) in de IR, met dezelfde verwachte zin.

**Normvragen, afgehandeld na de landingsronde van de hoofdsessie.**
1. Een EF-code voor de twee weigeringen van de caselezer (dubbel label, een naam die geen declaratie is), zodat gate 8
   ze kan pinnen: de hoofdsessie gaf deze vraag door aan een volgende bouwer.
2. Besluit van de hoofdsessie: IR-SPEC ch. 2.1 zegt nu in één zin dat "case-insensitive" alleen A–Z vouwt, zoals ref,
   de verifier, clingo en de caselezer vergelijken; de frontend mag in de bron ruimer vouwen, omdat hij de gedeclareerde
   spelling op de draad schrijft (eigen commit, voorstel; gate 1 groen, `logs/punt2-gate1.log`).
