# datumtijd-lezers-versmalling: de snippet-tolerantie achter het hek

Nalatenschap van kaart `20260922-p5-datumtijd-lexeem` (WF-095 versmald
tot canoniek ISO in `waarde`; de emitter schrijft ISO, de snippet
blijft in `bron`). De LEZERS tolereren de dag-eerst-snippet nog: `ref`
(en via zijn casemodule clingo) en de Drools-lezer
`Literalen.parseDatumTijd`. IR-SPEC benoemt die tolerantie expliciet
als gedrag achter het hek (ch. 4.1, de lezerszin) — de vorm bereikt de
schijf niet meer, dus de tolerantie is dood spoor maar geen fout.

Normvraag bij oppakken (afweging in de kaart beslissen, langs de
plan-lijn): versmallen de lezers mee tot canoniek — dan worden de
tolerantie-getuigen weigeringsgetuigen en verdwijnt de
maand-eerst/dag-eerst-vraag ook uit de lezers — of blijft de
tolerantie als gedocumenteerde leesbreedte? Aanbeveling: versmallen;
een lezer die vormen aanvaardt die geen producent meer schrijft en
geen verifier meer toelaat is ongetuigd gedrag.

Los van die keuze, en hoe dan ook te doen:

- `ref/lib/value.ml:110-117` zegt "which a v0 emitter still writes
  into `waarde`" — sinds de P5-kaart onwaar voor deze repo; commentaar
  actualiseren.
- `spec/ASP-TRANSLATION.md` en IR-SPEC houden de lezerszin consistent
  met de gekozen uitkomst.

## Acceptatie (bij oppakken aanscherpen)

- (a) De normkeuze vastgelegd in de kaart met motivering; bij
  versmallen: alle drie de lezers weigeren de snippet benoemd,
  rood-eerst per lezer (de bestaande tolerantie-getuigen — o.a.
  `ref/test/test_ref.ml` ~4082, `DatumTijdLexeemWitnessTest`,
  `CaseWireDatumTijdWitnessTest`, `LiteralenTest` — kantelen
  gemotiveerd mee).
- (b) Het value.ml-commentaar en de spec-lezerszinnen kloppen met de
  werkelijkheid.
- (c) Alle veertien gates groen.

May touch: `ref/**`, `backends/**`, `frontends/regelspraak/**`,
`spec/IR-SPEC.md` (**voorstel**), `spec/ASP-TRANSLATION.md`
(**voorstel**), kaartverplaatsing. NB: serieel met andere
Drools-kaarten.

## Normkeuze (hoofdsessie, 2026-09-23)

VERSMALLEN — een lezer die vormen aanvaardt die geen producent meer schrijft en geen verifier meer toelaat, is ongetuigd gedrag; de tolerantie was dood spoor sinds de P5-kaart en het fail-fast-huisbeginsel wint van leesbreedte.

## Acceptatie, aangescherpt bij de claim (2026-09-23)

Samen opgepakt met `20260922-datum-lexeem-hele-match` (zelfde
lezerszone, besluit hoofdsessie); gedeeld werk op één branch.

- (a) De normkeuze hierboven verbatim. Alle drie de lezers weigeren de
  dag-eerst-snippet in `waarde` BENOEMD: `ref`
  (`Value.datetime_of_lexeme`), clingo (meten of hij een eigen lezer
  heeft of `ref`'s `Value` deelt) en de Drools-lezer
  `Literalen.parseDatumTijd` (beide takken via de replay-API).
  Rood-eerst PER LEZER, met de letterlijke faaluitvoer in het Result.
  Elke gekantelde tolerantie-getuige (`ref/test/test_ref.ml` WF-095-blok,
  `DatumTijdLexeemWitnessTest`, `CaseWireDatumTijdWitnessTest`,
  `LiteralenTest`) gemotiveerd: oude lezing, nieuwe lezing.
- (b) De frontend blijft de bronsnippet lezen (zijn bron IS de snippet);
  meet waar hij dat vandaag via de gedeelde lezer doet en scheid bron-
  van wire-lezen zonder dat een emissie beweegt.
- (c) Het `ref/lib/value.ml`-commentaar ("which a v0 emitter still
  writes") klopt; IR-SPEC ch. 4.1-lezerszin en `spec/ASP-TRANSLATION.md`
  kloppen met de uitkomst — beide **beschermd-pad-voorstel**.
- (d) Meting vooraf: aanroepers per lezer; corpus- en toka-wire-scan op
  de snippet in `waarde`. Er beweegt niets: gates 10/11 byte-identiek,
  gate-12-pins 180/157 onaangeroerd, corpussweep beide Drools-takken
  vóór/na identiek. Een bewegend document is een stopteken.
- (e) Mutatieproef per versmalde lezer (tolerantie terug ⇒ minstens één
  weigeringsgetuige valt ⇒ herstel, `cmp` in één keten), uitgevoerd en
  vastgelegd.
- (f) Alle veertien gates groen, elk los gedraaid.

## Result:

Alle acceptatiepunten gehaald; alle veertien gates groen, elk los
gedraaid. Gedeeld werk met `20260922-datum-lexeem-hele-match` (één
branch; de Java-commit dekt beide kaarten).

**Meting vooraf.** (1) Aanroepers. `ref`: `Value.datetime_of_lexeme`
wordt aangeroepen door `Eval.literal_value` (de literal) en door
clingo's `translate.ml` (twee plekken); de case-lader roept
`read_iso_datetime` al direct aan en las de snippet nooit. Clingo heeft
GEEN eigen lezer: hij leest via `ref`'s `Value`, dus versmallen in
`value.ml` versmalt beide. Drools: `Literalen` leeft in
`frontends/regelspraak/.../kern/` en wordt door de backend-pom
meegecompileerd (met uitsluiting van `resolver/**` en `emit/**`).
`parseDatumTijd` wordt aangeroepen door `DrlTranspiler.literal`,
`ExecModelTranspiler` (waardepositie en `datumLiteraal`),
`onleesbaarDatumTijdLexeem` — en door de FRONTEND:
`ExpressieBouwer` las de bronsnippet via dezelfde methode. Versmallen
zonder scheiden had de emitter gebroken; de bronlezer is daarom naar
`ExpressieBouwer` verhuisd (`bronDatumTijd`, `bronDatum`), een pakket
dat de backend niet meebouwt. (2) Corpusscan (JSON-walk over
`corpus/`, `toka/`, `backends/`, `frontends/`, 1454 documenten, 78
`DatumInDagen`- en 11 `DatumTijd`-literals): acht niet-ISO-lexemen,
alle acht in `corpus/negatief/wf084-*`/`wf095-*`, die de verifier
weigert en geen uitvoerder leest. Nul wire-dragers van de snippet
buiten de testsuites. (3) Het `value.ml`-commentaar is herschreven.

**Normkeuze** (verbatim, hoofdsessie 2026-09-23; staat ook boven):
VERSMALLEN — een lezer die vormen aanvaardt die geen producent meer
schrijft en geen verifier meer toelaat, is ongetuigd gedrag; de
tolerantie was dood spoor sinds de P5-kaart en het
fail-fast-huisbeginsel wint van leesbreedte.

**Rood-eerst per lezer** (getuigen eerst gekanteld, tegen de oude lezer
gedraaid):

- `ref` (`test_ref.exe`): `FAIL WF-095-weigert-snippet (no fault at
  all)`, idem `-zonder-fractie`, `-dedato-prefix`,
  `-smalle-velden`; `4 failure(s)`.
- clingo (`test_clingo.exe`): `FAIL dag-eerst-bronsnippet als
  datum-tijd-literal in de vertaling: expected a loud fault, got a
  model` — `test_clingo: 480 checks, 1 FAILURES`.
- Drools (mvn, drie klassen): `DatumTijdLexeemWitnessTest.
  het_bronsnippet_wordt_op_beide_takken_luid_geweigerd:106 the
  exec-model branch refuses the rule for snippet 31-01-2024 12:00:00
  ==> Expected ...CaseRunner.Excluded to be thrown, but nothing was
  thrown.`; `de_lezer_weigert_het_bronsnippet:178 ... expected: <null>
  but was: <2024-01-31T12:00>`; `CaseWireDatumTijdWitnessTest.
  het_bronsnippet_is_geen_literal_lexeem_en_geen_casewaarde:166 ...
  expected: <null> but was: <2026-03-15T12:00>`. Via de CLI op een
  model met snippetliteral: vóór `PLAYED 1` op beide takken.
- Frontend `LiteralenTest`: `het_bronsnippet_is_geen_wire_lexeem() ✘
  dag-eerst met dd.-voorvoegsel ==> expected: <null> but was:
  <2024-12-25>`.

**Na:** `ref` `EVALUATION FAULT: malformed datum-tijd lexeme
"31-01-2024 12:00:00": not the canonical ISO form
yyyy-mm-ddThh:mm:ss[.fff] (at offset 0: expected four year digits)`;
clingo idem als `TRANSLATION FAULT`; Drools exec `EXCLUDE transpiler
SKIP: regel: Kenmerktoekenning verjaard — regel (voorwaarde met
onleesbaar datum-tijd-lexeem)`, DRL `EXCLUDE drl-transpiler MARKER: …
TODO voorwaarde met onleesbaar datum-tijd-lexeem: …`.

**Gekantelde getuigen** (oud → nieuw):

- `test_ref.ml` WF-095-blok: vier `dt_ms`-pins lazen de snippet als
  12:00 op 15-03-2026 (resp. 5-3) → vier `expect_fault_msg`-pins die
  `malformed datum-tijd lexeme "<lexeem>": not the canonical ISO form`
  eisen; het casewire-commentaar ("the snippet is a LITERAL form")
  zegt nu dat beide wires één spelling dragen.
- `test_clingo.ml`: nieuwe pin 6 (geen bestaande tolerantiegetuige in
  clingo; zijn tolerantie was die van `ref`).
- `DatumTijdLexeemWitnessTest`: `het_bronsnippet_speelt_identiek_op_
  de_exec_model_tak` en de snippethelft van de DRL-pin en van de
  milliseconde-pin ("snippet speelt identiek") →
  `het_bronsnippet_wordt_op_beide_takken_luid_geweigerd` (EXCLUDE met
  `datum-tijd-lexeem` op beide takken); `de_lezer_leest_beide_lexemen`
  ("de lezer mag niet strenger zijn dan zijn producent") →
  `de_lezer_leest_het_canonieke_lexeem…` + `de_lezer_weigert_het_
  bronsnippet` (vier vormen null). Klasse-javadoc bijgewerkt.
- `CaseWireDatumTijdWitnessTest.het_bronsnippet_is_wel_een_literal_
  lexeem_en_geen_casewaarde` pinde een ASYMMETRIE (literal leest,
  case weigert) → `…_is_geen_literal_lexeem_en_geen_casewaarde`: beide
  weigeren, zodat het terugverbreden van één van beide lezers hier
  valt.
- `LiteralenTest`: de drie snippetlezingen (`dd. 25-12-2024`, `dd.
  8-9-2024`, `dd. 25-12-2024 10:30:00.500`) en "beide lexemen
  benoemen hetzelfde moment" → ISO-lezingen plus `het_bronsnippet_is_
  geen_wire_lexeem`. De bronlezing zelf blijft gepind door de
  bestaande emittergetuigen in `IrEmitterTest` (dd.-voorvoegsel,
  enkelcijferige velden, milliseconden, kalenderfout).

**Mutatieproeven** (elk in één keten, `cmp` identiek na herstel):
`value.ml` terug naar HEAD ⇒ `ref` 4 failures, clingo 1 FAILURE ⇒
herstel, herbouw rc 0, `cmp identiek`, 779/480 groen.
Snippetarm terug in `Literalen.parseDatumTijd` ⇒ Drools 3 failures
(de drie rood-eerst-pins hierboven), frontend 1 (`het_bronsnippet_is_
geen_wire_lexeem ✘ datumtijd-snippet met voorvoegsel … but was:
<2024-12-25T10:30:00.500>`) ⇒ herstel, `cmp identiek`.

**Niets bewoog.** Corpussweep beide Drools-takken (230 goldens met
case, volledige stdout+stderr per golden, 2420 regels) vóór en na:
`cmp` byte-identiek. Gate 10: 70 rgs-goldens byte-identiek; gate 11:
45 .rgs byte-identiek; gate-12-pins 180/157 onaangeroerd.

**Gates** (elk los, na `eval $(opam env)`): 1 PASS; 2 PASS (273
bestanden); 3 PASS (273 golden / 431 negatief); 4 PASS (629 cases); 5
PASS (ref-suite 779, 588 cases); 6 PASS (480 checks, 588 cases); 7
PASS (552 + 36 overlevers); 8 PASS (41 rood; Drools-been 9/32); 9
PASS (201 checks, 12 goldens); 10 PASS (393 tests, 70 rgs-goldens); 11
PASS (45 re-emissies, 27 tests, 29/29/29); 12 PASS (played 180, DRL
157, JUnit 582); 13 PASS (131 twists, 25 DRL-played, 0 overlevers); 14
PASS (130 checks, 2 goldens).

**Beschermd pad, voorstel:** `spec/IR-SPEC.md` ch. 4.1 — (i) de
WF-095-alinea "The executors' readers still READ the snippet … behind
the fence" herschreven: sinds 2026-09-23 weigeren ze hem benoemd, en de
frontend leest zijn bron met een eigen lezer; (ii) de WF-095-registerrij
"the readers keep reading the snippet behind the fence" idem.
`spec/ASP-TRANSLATION.md` (datum-tijd-literalalinea): "the shared reader
still also reads the day-first source snippet is tolerance behind the
verifier's fence" → de snippet faalt bij vertaling met het geciteerde
lexeem. Het `value.ml`-commentaar is geen beschermd pad en is
gewoon bijgewerkt.

**Registerrijtekst `spec/CONSTRUCTS.md` rij 27** — vervangt de slotzin
"; the readers still read the snippet behind the fence" door:
"; **the readers narrowed with the wire 2026-09-23 (card
`datumtijd-lezers-versmalling`, maintainer norm: a reader admitting a
form no producer writes and no verifier admits is unwitnessed
behaviour)**: ref and clingo (which reads the literal through ref's
reader) fault on the snippet quoting the lexeme, the shared Drools
reader `Literalen.parseDatumTijd` yields no value and both transpiler
branches refuse the rule as an unreadable datum-tijd lexeme; the
frontend reads its own source snippet with a reader in `ExpressieBouwer`
that the backend does not build — no document moved (both-branch sweep
byte-identical, gates 10/11 byte-identical, gate-12 pins 180/157)".

**Open:** niets voor deze kaart. `spec/RUNTIME-SEMANTICS.md` noemt de
snippet niet (gecontroleerd).
