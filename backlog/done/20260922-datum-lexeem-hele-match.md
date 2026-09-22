# datum-lexeem-hele-match: find() leest een misvormde datum stil verkeerd

Gemeten door de datumranden-kaart (2026-09-22, zusterranden-meting, via
de CLI gereproduceerd): een dag of maand van MEER dan twee cijfers
passeert het datumtoken van de grammatica en wordt daarna stil
verkeerd gelezen, op twee plekken verschillend:

- de literal `2024-01-2026` werd `"waarde": "2024-01-20"` en
  `geldig vanaf 2024-01-2026` werd `"vanaf": "2026-01-24"` — BEIDE
  FRONTENDHELFTEN ZIJN GEDICHT in de fixronde van de
  datumranden-kaart zelf (2026-09-22, heel-lexeem-guards in
  `ExpressieBouwer` en `RuleChecker.grens`, rood-eerst getuigen);
- WAT RESTEERT is de gedeelde LEZER: `Literalen.parseDatum` zoekt nog
  met `find()` naar het ISO-patroon. Alle backend-aanroepers lezen
  vandaag de ISO-`waarde` van de wire, dus de tolerantie is dood
  spoor — maar een lezer die een misvormde waarde stil herleest in
  plaats van benoemd te weigeren blijft de verboden klasse.

Nul corpusdragers. `parseDatumTijd` matcht het hele lexeem al; dat is
het huispatroon. `Literalen.parseDatum` is gedeeld met de
Drools-backend, dus de fix raakt de consumenten — meet bij oppakken
of een corpus- of toka-wire de tolerante lezing nodig heeft (er
hoort niets te bewegen: gates 10/11 re-emitten byte-identiek). Deze
kaart sluit inhoudelijk aan bij `20260922-datumtijd-lezers-versmalling`
(zelfde lezerszone); overweeg ze samen op te pakken.

## Acceptatie (bij oppakken aanscherpen)

- (a) De gedeelde lezer matcht het HELE lexeem; een misvormde waarde
  geeft een benoemde weigering, nooit een stil ander antwoord.
  Rood-eerst getuigen aan de lezerkant (beide Drools-takken).
- (b) Corpussweep: geen bestaande emissie of replay beweegt (gates
  10/11 byte-identiek; gate 12 pins onaangeroerd).
- (c) Alle veertien gates groen.

May touch: `frontends/regelspraak/**`, `backends/drools/**` (de
gedeelde lezer), kaartverplaatsing. NB: serieel met andere
Drools-kaarten.

## Acceptatie, aangescherpt bij de claim (2026-09-23)

Samen opgepakt met `20260922-datumtijd-lezers-versmalling` (zelfde
lezerszone, besluit hoofdsessie; normkeuze daar: VERSMALLEN).

- (a) `Literalen.parseDatum` matcht het HELE lexeem (`matches()`, nooit
  `find()`), het huispatroon van `parseDatumTijd`; een misvormde waarde
  (o.a. `2024-01-2026`, `123-01-2026`) geeft null en geen ander antwoord.
- (b) Rood-eerst getuigen aan de lezerkant: de unit-lezer, en BEIDE
  Drools-takken via de replay-API (exec-model en DRL-tekst) op een model
  met een misvormd datumlexeem — vóór de fix gemeten gedrag letterlijk in
  het Result, erna een benoemde weigering (geen stil gespeeld antwoord).
  De frontend-aanroepers voor zover ze nog door de lezer lopen: hun
  bestaande heel-lexeem-getuigen blijven groen.
- (c) Mutatieproef: `find()` terug ⇒ minstens één getuige valt ⇒
  herstel, `cmp` in één keten.
- (d) Corpussweep: geen emissie of replay beweegt (gates 10/11
  byte-identiek; gate-12-pins 180/157 onaangeroerd; beide Drools-takken
  vóór/na identiek).
- (e) Alle veertien gates groen, elk los gedraaid.

## Result:

Alle acceptatiepunten gehaald; alle veertien gates groen, elk los
gedraaid. Samen gebouwd met `20260922-datumtijd-lezers-versmalling`
(één Java-commit voor beide; meting, sweep en gate-uitslagen staan in
het Result van die kaart en gelden hier ongewijzigd).

**Meting.** `Literalen.parseDatum` wordt aangeroepen door
`DrlTranspiler.literal`, `ExecModelTranspiler` (periodegrens,
waardepositie, `datumLiteraal`) en `Dagsoorten` — alle op de
wire-`waarde` — en door de frontend (`ExpressieBouwer`, de kale
datumliteral, op de BRONSNIPPET achter een heel-lexeem-guard). De
corpusscan vond geen `DatumInDagen`-literal buiten ISO behalve de twee
`wf084-*`-negatiefs; nul toka-dragers.

**Wat de lezer deed** (gemeten via de backend-CLI op een model met één
datumliteral in een EERDER-vergelijking, casus gepind op de
mislezing): `2024-01-2026` → `PLAYED 1` op de exec-model- én de
DRL-teksttak, dus stil gelezen als 2024-01-20; ook `31-01-2024` (de
dag-eerst-snippet die WF-084 weigert) en `2024-1-31` (buiten de vaste
veldbreedtes) speelden op beide takken. Oorzaak: `find()` op
`\d{4}-\d{1,2}-\d{1,2}` plus een dag-eerst-terugvalarm.

**Wat hij nu doet.** Hele-lexeem-match op `(\d{4})-(\d{2})-(\d{2})`,
kalender via `LocalDate.of`, anders null — dezelfde grammatica als
`ref`'s `date_of_iso` en WF-084. De dag-eerst-arm verviel mee: hij las
een vorm die WF-084 weigert en die `ref` en clingo al weigerden (zelfde
normkeuze als de zusterkaart). De bronlezing verhuisde naar
`ExpressieBouwer.bronDatum`; de oude guard daar is daarmee opgegaan in
de bronlezer zelf (zelfde patroon, hele match). Benoemde weigering: een
nieuw predicaat `Literalen.onleesbaarDatumLexeem` (zuster van
`onleesbaarDatumTijdLexeem`) voedt de redenen van beide takken — na:
exec `EXCLUDE transpiler SKIP: regel: Kenmerktoekenning verjaard —
regel (voorwaarde met onleesbaar datum-lexeem)`, DRL `EXCLUDE
drl-transpiler MARKER: … TODO voorwaarde met onleesbaar datum-lexeem:
zijn vondstdag eerder is dan 2024-01-2026` (idem voor de andere twee).
Zonder het predicaat had de exec-tak "voorwaarde buiten Phase-B scope"
gezegd.

**Rood-eerst.** Nieuwe `DatumLexeemWitnessTest` (vier tests, replay-API
op beide takken) tegen de oude lezer: `de_lezer_weigert_elke_vorm_
buiten_het_hele_lexeem:66 a multi-digit day is not a prefix to read as
2024-01-20 ==> expected: <null> but was: <2024-01-20>` en `een_misvormd_
datumlexeem_wordt_op_beide_takken_luid_geweigerd:89 the exec-model
branch refuses the rule for '2024-01-2026' instead of playing a
misreading ==> Expected ...CaseRunner.Excluded to be thrown, but
nothing was thrown.` Frontend `LiteralenTest.datum_matcht_het_hele_
lexeem ✘ werd met find() stil 2024-01-20 ==> expected: <null> but was:
<2024-01-20>`. De frontend-aanroeper loopt niet meer door deze lezer;
zijn heel-lexeem-getuigen in `IrEmitterTest` (`2024-01-2026`,
`123-01-2026`, `1-123-2026` → ERROR; `dd. 1-2-2026` gelezen) bleven
groen, en gates 10/11 re-emitteren byte-identiek.

**Mutatieproeven** (één keten elk, `cmp identiek` na herstel):
`matches()` → `find()` ⇒ `DatumLexeemWitnessTest` 2 failures (de twee
hierboven); dag-eerst-arm terug ⇒ `DatumLexeemWitnessTest` 2 failures
(`…:72 the day-first source snippet (WF-084 refuses it) ==> expected:
<null> but was: <2024-01-31>`; `…:89 … for '31-01-2024' …`) en frontend
1 (`het_bronsnippet_is_geen_wire_lexeem ✘ dag-eerst met dd.-voorvoegsel
… but was: <2024-12-25>`).

**Niets bewoog:** sweep beide takken vóór/na byte-identiek; gates 10/11
byte-identiek; gate-12-pins 180/157 onaangeroerd; Drools-JUnit 582.

**Beschermd pad, voorstel:** `spec/IR-SPEC.md` ch. 4.1, WF-084-alinea:
één zin toegevoegd dat de Drools-literallezer sinds 2026-09-23 dezelfde
grammatica heel matcht, met de gemeten mislezing ervoor.

**Registerrijtekst `spec/CONSTRUCTS.md`, `Geldigheid`-rij (§2.3)** —
vervangt "de gedeelde lezer `Literalen.parseDatum` en het
tijdlijn-startpunt zijn eigen kaarten (`20260922-datum-lexeem-hele-match`,
`20260922-tijdlijn-startpunt-emissie`, the latter landed 2026-09-22:
refusal at the emission boundary, see the `Tijdlijn` row)" door: "het
tijdlijn-startpunt was een eigen kaart (`20260922-tijdlijn-startpunt-emissie`,
landed 2026-09-22: refusal at the emission boundary, see the `Tijdlijn`
row); **the shared reader closed 2026-09-23 (card
`datum-lexeem-hele-match`)**: `Literalen.parseDatum` matches WF-084's
lexeme whole (fixed widths, calendar) where it searched with `find()` —
`2024-01-2026` played as 2024-01-20 on both Drools branches, and the
day-first snippet was read too — and an unreadable date lexeme is now
refused on both branches: named at a comparison (`voorwaarde met
onleesbaar datum-lexeem`), elsewhere under the position's own reason
(value position, period bound, used day kind — each witnessed in
`DatumLexeemWitnessTest`), never answered; the frontend's source reading
moved to its own reader; no document moved".

**Fixronde (lens 2026-09-23).** Het open punt van de eerste ronde
(andere posities dan de vergelijking) had geen getuige: lens-mutant MC
(`ExecModelTranspiler`, waardepositie `return d == null ? null :
WaardeFn.constant(d)` → `return WaardeFn.constant(d)`) liet de exec-tak
stil "verwacht 2024-01-20, werkelijk leeg" antwoorden met alle 582 tests
groen. Drie getuigen erbij in `DatumLexeemWitnessTest`, elk met het ISO-
lexeem als speelcontrole en de drie misvormde lexemen als weigering, de
reden gepind die er werkelijk uit komt:

- waardepositie (GELIJKSTELLING `peildag` := literal): exec `regel
  (waarde buiten scope): Peildag`, DRL `TODO datumliteral: <lexeem>`;
- periodegrens (golden `jutterswet-premievenster`, eerste venster `tot`
  → `2025-01-2026`): exec `voorwaarde met onleesbaar datum-lexeem`, DRL
  `TODO periodevergelijking-grens (§8.4.3)`; de oude jar speelde dit
  (`PLAYED 1`, dus als 2025-01-20);
- gebruikte dagsoort (`zijn vondstdag is een peildag`, dagsoort
  vergelijkt `de dag` met de literal): exec `voorwaarde buiten Phase-B
  scope`, DRL `TODO dagsoortcontrole 'peildag'`; de oude jar speelde
  `2024-01-2026` en `20-01-2024` als 2024-01-20 (`PLAYED 1` op een
  casus gepind op die dag).

Rood-eerst onder MC zelf, uitgevoerd: `DatumLexeemWitnessTest.
een_misvormd_datumlexeem_op_een_waardepositie_wordt_op_beide_takken_
geweigerd:118 the exec-model branch refuses the value '2024-01-2026'
instead of writing leeg ==> Expected ...CaseRunner.Excluded to be
thrown, but nothing was thrown.` (7 tests, 1 failure); herstel `cmp
identiek`. Lens-mutant MA (`ISO_DATUM` met `(?:dd\. +)?`): nieuwe pins
rood — `de_lezer_weigert_elke_vorm_buiten_het_hele_lexeem:80 the
de-dato prefix before an ISO lexeme is not the lexeme ==> expected:
<null> but was: <2024-01-31>` en frontend `het_bronsnippet_is_geen_
wire_lexeem ✘ het dd.-voorvoegsel voor een ISO-datum is geen lexeem ==>
expected: <null> but was: <2024-12-25>`; herstel `cmp identiek`.

De weigerzin in IR-SPEC (WF-084-alinea, voorstel) is beperkt tot "at a
comparison each branch names the unreadable date lexeme; elsewhere the
rule is refused under its position's own reason, never answered".

**Open:** geen. Een lexeemreden op de andere posities is niet
toegevoegd; de getuigen pinnen de positiereden die er vandaag uitkomt,
zodat een latere benoeming die pin bewust moet bijstellen.
