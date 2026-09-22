# drools-enumnaam-identiteit: condities vergelijken enumlabels nog byte-exact

Gemeten bij kaart `20260920-drools-enumlabel-lezer` (dossier §"Niet
gebouwd, wel gemeten"): in CONDITIES vergelijken beide Drools-takken
enumlabels byte-exact waar `ref` (`Checks.norm`) en de ASP-backend
(genormaliseerde opslag) naamidentiteit hanteren — een enumlabel is een
gedeclareerde naam. De plekken: de twee literal-conditie-arms en de
assertie-arm van `ExecModelTranspiler` (`rhs.equals(...)`) en de
literal-verlaging van `DrlTranspiler` die naar een mvel-`==` rendert.
De casusvergelijker is bij die kaart al op naamidentiteit gezet; de
conditiekant bleef bewust staan.

Geen corpusdrager: precies één enumwaarde in de hele zone wijkt van
haar declaratie af (`jutterswet-keurstaat`, casusinvoer) en haar
vergelijkingspartner verschilt onder beide lezingen, dus geen document
kan het verschil vandaag tonen. Optrekken raakt vier arms plus de
geëmitteerde mvel en de uniciteitssleutels, en wijkt af van de
productiesemantiek — dat is een NORMBESLUIT (volgt de conditielezing de
naamidentiteit van ref/clingo, of blijft byte-exact de Drools-lezing
met een genormeerde divergentie?), geen techniekkeuze.

## Acceptatie (bij oppakken aanscherpen)

- (a) De normvraag als genummerde maintainersvraag met de meting erbij
  (een verifier-schoon document met een norm-afwijkend gespelde
  enumvergelijking splijt de uitvoerders — construeer hem als bewijs).
- (b) Bij naamidentiteit: de vier arms + mvel-emissie + getuigen in
  beide takken, mutatieproef, en de productiesemantiek-divergentie in
  de eerstvolgende herpin-conformance-notitie.
- (c) Bij byte-exact-als-norm: de spec-zin die de divergentie draagt en
  een negatief/rood-getuige die haar pint.
- (d) Alle veertien gates groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh`
(**voorstel**), `spec/**` (**voorstel**), `corpus/**`,
kaartverplaatsing. NB: serieel met andere Drools-kaarten.

## Maintainersbesluit (2026-09-20): naamidentiteit

De Drools-conditielezing volgt ref en clingo: één identiteitsbegrip
voor het hele vierkant (trim + lowercase + witruimtevouw), consistent
met het casusvergelijker-besluit van de enumlezer-kaart. De
productiesemantiek-divergentie van regelspraak-java gaat als
conformance-notitie mee in de eerstvolgende herpin, zoals eerdere
divergenties. Uitvoering: acceptatiepunt (b) van deze kaart — de vier
arms plus de geëmitteerde mvel, getuigen in beide takken met
mutatieproef, en het bewijs-document uit (a) wordt de golden of
suite-getuige die de splijting vóór de fix toont.

## Result (2026-09-22)

Geland op naamidentiteit, beide takken, met één uitbreiding buiten de
may-touch-lijst die de meting afdwong (ref, zie onder).

(a) Bewijsdocument. `corpus/golden/jutterswet-keurspelling` (bron.rgs,
frontend-geëmitteerd, verifier-schoon): drie condities (eigen attribuut
GELIJK en ONGELIJK tegen `'gaaf'`, attribuut tegen attribuut), twee
asserties (tegen de literal `'wrakhout'` en tegen een attribuut) en één
uniciteit over de enumsleutel `staat`. De casussen spellen de invoer
anders dan de declaratie ("Gaaf", "Wrakhout", "Beschadigd"); case 3 is
de controle de andere kant op (twee verschillende labels blijven er
twee). Rood-eerst gemeten op de pre-fix-jar en de pre-fix-ref:

| uitvoerder | c1 | c2 | c3 | c4 |
|---|---|---|---|---|
| ref (vóór) | PASS | PASS | PASS | MISMATCH (uniciteit) |
| clingo | PASS | PASS | PASS | PASS |
| exec-model (vóór) | FAIL | FAIL | PASS | FAIL |
| DRL-tekst (vóór) | FAIL | FAIL | PASS | FAIL |
| alle vier (na) | PASS | PASS | PASS | PASS |

De Drools-faalregels, in beide takken identiek: c1 `kenmerk veilbaar:
verwacht aanwezig, werkelijk afwezig; kenmerk afgekeurd …; kenmerk
bevestigd …; verwacht consistent, werkelijk inconsistent (herkeuring
volgt de staat @ V1)`, c2 `kenmerk bevestigd …; verwacht inconsistent
(geen wrakhout ter keuring @ V1), niet gevonden`, c4 tweemaal `kenmerk
bevestigd` plus het ontbrekende `eenmalige staat`-paar.

Keuze golden, niet alleen suitegetuige: de golden draagt vijf van de
armen in het VOLLE vierkant, zodat gates 5/6/12 hem blijvend bewaken.
De armen die clingo bij vertaling weigert (genavigeerde enumoperand,
enumparameter als operand: `a text/enum/boolean operand here is an
own-subject attribute read or a literal`; enumsleutel in een unie:
`unscalable datatype`) kunnen geen golden zijn en staan als
suitegetuige in beide takken.

De uniciteitsmeting liet een tweede splijting zien die de kaart niet
noemde: ref's sleuteltupel stringificeerde een enumlabel byte-exact
(`Value.to_string (Enum s) = s`), dus op c4 stonden ref ≡ Drools tegen
clingo, dat genormaliseerd opslaat. Onder het besluit (één identiteit
voor het hele vierkant, ref's eigen `Arith.equal` normaliseert al) is
dat een ref-defect; `ref/lib/eval.ml` (`ever_tuples`) laat een
enumcomponent nu door `norm` gaan. **Buiten de may-touch-lijst van de
kaart (die noemt `ref/` niet); acht regels, beschermd pad nee — ter
beoordeling in de lens.**

(b) De fix. Eén definitie: `RegelSpraakRuntime.rsEnumNaam` delegeert
naar `Namen.norm` (dezelfde als `CaseRunner.gelijkeWaarde`) en laat
null null, zodat elke arm zijn leegwaardetabel houdt. Exec-model: de
eigen en de genavigeerde literal-conditiearm, de parameterarm, de
attribuut-tegen-attribuutconditie, de assertie tegen een literal en
tegen een attribuut, en de sleuteljoin van de enkele uniciteit en de
unie, via één helper `gelijkheidsLezing` (enum aan een van beide
kanten ⇒ norm; Tekst/Booleaan ongemoeid). DRL-tekst:
`vergelijkingsOperanden` wikkelt beide operanden in `rsEnumNaam(…)` —
alle condities, de genavigeerde schending/voorwaarde en de statische
guard lopen erdoorheen — plus de niet-numerieke GELIJK-assertie en de
sleutelmatchers van uniciteit en unie. `literal()` bleef verbatim: hij
bedient ook de schrijfpositie. "Vier arms" uit de meting werden er in
de exec-tak zeven (plus twee sleuteljoins); de extra drie (parameter,
twee attribuut-tegen-attribuutvormen) splijten aantoonbaar op dezelfde
manier en vallen onder hetzelfde besluit.

Getuigen: `EnumNaamIdentiteitWitnessTest` (4 rijen, `BeideTakken`):
de golden per casus; genavigeerd (met `navigatieRol`, de vorm die de
exec-arm leest — de bindingenvorm draagt die arm niet) plus parameter
op een in-memory document; de unie; en de tekstflank (elke enum
vervangen door Tekst: "Gaaf" ≠ "gaaf" bij ref, clingo en beide takken).
Alle vier in-memory documenten verifier-schoon en door ref gespeeld
met exact de verwachtingen van de rij (gemeten 2026-09-22); vóór de
fix FAILden beide takken op genavigeerd/parameter en unie.

Mutatieproeven (build-rc in de keten, telkens teruggedraaid, bestanden
met `cmp` tegen het origineel bevestigd):
1. `rsEnumNaam` geeft een String ongewijzigd terug (jar-md5
   9c83a980…): golden in beide takken FAIL c1/c2/c4; suite 3 van 4
   rood (golden, genavigeerd+parameter, unie), tekstflank groen.
2. De normalisatie lekt naar Tekst (`|| isTekst()` in de
   enumbeslissing van beide takken): tekstflank rood in exec, DRL-replay FAIL op het
   tekstdocument; `EnumLabelWireWitnessTest` blijft groen.
3. ref's `key_lexeme` terug naar `Value.to_string` (runner-md5
   7e05a8f7…): c4 MISMATCH op `eenmalige staat`, c1-c3 PASS.

Corpussweep pre-jar tegen post-jar over alle 228 goldens met casussen,
beide takken: precies één regel beweegt per tak,
`jutterswet-keurspelling` FAIL → PLAYED 4. Geen bestaande golden wisselt
van kanaal of waarde.

(d) Gates, elk los na `eval $(opam env)`, alle veertien PASS:
1 self-contained PASS; 2 ir-schema 271 bestanden; 3 verifier 271 golden
schoon, 430 negatief; 4 case-schema 623; 5 oracle 773 voorbeelden, 582
casussen; 6 clingo 479 checks, 582 casussen; 7 noop 546 + 36
overlevers; 8 rood 41 (Drools-been 9 + 32); 9 catala 201 checks, 12
goldens; 10 regelspraak 372 tests, 69 rgs-goldens; 11 toka-parity 29
ref/clingo/Drools; 12 drools played 178 = pin, DRL 155 = pin, JUnit
539; 13 reftype 131 twists, 128 replays; 14 flogic 130 checks.

Beschermd-pad-voorstellen:
- `scripts/check-drools.sh`: `EXPECTED_PLAYED` 177 → 178 en
  `EXPECTED_DRL_PLAYED` 154 → 155, elk met redenblok. Pindelta van
  deze kaart: **+1 / +1** (alleen `jutterswet-keurspelling`; handtelling
  177 + 1 = 178, 154 + 1 = 155, gelijk aan de gate-meting).
- Voorgesteld, NIET aangeraakt — `spec/RUNTIME-SEMANTICS.md` ch. 5.6,
  achter "(a duplicate non-empty key)": "; two keys are the same key
  under the ch. 1.2d value equality, component by component — an
  enumeration component therefore up to the ci label identity of
  WF-008, so two spellings of one declared label collide". Worked
  example: `jutterswet-keurspelling` case 4.

Registerrij 27 (`spec/CONSTRUCTS.md`), voorgestelde vervanging van de
zin "Wat NIET meekwam, en gemeten is: … (`backlog/new/20260920-drools-
enumnaam-identiteit.md`)." door: "De regelkant van dezelfde identiteit
is gedicht (2026-09-22, kaart `20260920-drools-enumnaam-identiteit`,
maintainersbesluit 2026-09-20): beide Drools-takken lezen elke
enumeratie-operand van een gelijkheid en elke enumeratiesleutel van
een uniciteit (enkel en unie) als gedeclareerde naam via één runtime-
helper op `Namen.norm`, en ref's uniciteitssleutel — die byte-exact
stringificeerde en daarmee met Drools tegen clingo stond — gaat door
dezelfde norm. Golden `jutterswet-keurspelling` toont de splijting
vóór en het volle vierkant na (`EXPECTED_PLAYED` 177 → 178,
`EXPECTED_DRL_PLAYED` 154 → 155); de genavigeerde, parameter- en
unievorm, die clingo bij vertaling weigert, en de tekstflank staan als
suitegetuige in beide takken. Geboekte toolchain-divergentie: de
productietoolchain vergelijkt enumlabels byte-exact."

Conformance-notitie voor de eerstvolgende herpin van regelspraak-java
(tekstvoorstel; de toolchain-repo is niet aangeraakt): "Sinds
<SPEC_REV> vergelijkt uchar een enumeratielabel in elke regel-
vergelijking (condities, consistentie-asserties, uniciteitssleutels)
als gedeclareerde naam: trim, witruimtevouw over spatie en tab, ASCII-
lowercase (ref's `Checks.norm`). `RegelSpraakRuntime` en de twee
transpilers van regelspraak-java vergelijken byte-exact; een casus
die een label anders spelt dan zijn declaratie (bijv. "Gaaf" tegen
`'gaaf'`) vuurt daar andere regels. Dit is een bewuste divergentie
(maintainersbesluit 2026-09-20), geen regressie; getuige
`jutterswet-keurspelling` (cases 1, 2 en 4 splijten op de
productiesemantiek, case 3 niet)."

Niet gedaan: `spec/CONSTRUCTS.md`, `spec/PLAN.md` en
`RUNTIME-SEMANTICS.md` (voorstellen hierboven); geen ref-testvoorbeeld
toegevoegd (de golden bewaakt de ref-wijziging via gates 5/6).
Temporele enumsleutels bleven buiten beeld: de ever-uniciteit
(`rsOoitBotst`/`rsOoitBevat`) leest alleen getallen, dus een temporele
enumsleutel is daar al geen gedragen vorm.
