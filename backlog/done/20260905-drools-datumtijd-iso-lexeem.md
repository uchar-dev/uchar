# De Drools-backend leest het canonieke DatumTijd-lexeem niet (wire-gat)

Goal: `Literalen.parseDatumTijd` — de gedeelde literal-lezer die beide
backends compileren — accepteert alléén het RegelSpraak-bronsnippet
`dd-mm-yyyy hh:mm:ss[.SSS]` en geeft op het canonieke ISO-lexeem
`null`. `parseDatum` doet het naast haar wél goed: die leest sinds
WF-084 zowel `yyyy-mm-dd` als het dag-eerst-snippet, en de eigen
frontend schrijft voor een DAG-datum ook het ISO-lexeem in `waarde`
(snippet blijft in `bron`). Voor een `DatumTijd`-literal schrijft die
frontend nog het snippet (`ExpressieBouwer` ~r. 379), dus vandaag is er
geen levend corpusdefect — maar de IR is een wire, geen frontend-uitvoer.
Een conforme producent die het canonieke ISO-datetime-lexeem schrijft,
wordt door de Drools-backend **stil geskipt**: de vergelijking valt terug
op `null` en de hele regel verdwijnt uit de KieBase.

Gemeten (fixronde 2026-09-05, probe op een handgebroken
`corpus/golden/jutterswet-vondstverjaring`: `vondstdag` als `DatumTijd`,
regelvoorwaarde `EERDER` tegen een `DatumTijd`-literal, verder identiek —
alleen het lexeem verschilt):

    waarde "2025-03-01T00:00:00"  -> EXCLUDE transpiler SKIP: regel
                                     (voorwaarde buiten Phase-B scope)
    waarde "01-03-2025 00:00:00"  -> PLAYED 1

Dit is de eerste uitvoerdergetuige voor de discipline die WF-084
expliciet uitscopet ("datetime lexemes carry their own ISO discipline,
tracked separately — they are not exercised by any corpus literal yet",
IR-SPEC ch. 4.1). De kaart maakt van dat uitgescopete zinnetje een
gemeten verplichting: het lexeem vastleggen én de lezer eraan houden.

Naast het gat zelf zit er een tweede les in: de SKIP-reden loog niet,
maar hij was niet-onderscheidend. "voorwaarde buiten Phase-B scope"
dekte hier een parseerfout, en die verwarring heeft in de WF-036-kaart
één beurt lang voor "de backend kan geen milliseconden" doorgegaan
(zie `backlog/done/20260905-wf036-datumordening-precisie.md`, fixronde).

Acceptance:
- Meet eerst waar de fix hoort. `Literalen` staat in de frontend-kern
  (`frontends/regelspraak/.../kern/Literalen.java`) maar wordt door
  `backends/drools/pom.xml` als bron mee-gecompileerd, dus één plek
  bedient beide — de vermoedelijke landingsplaats is `parseDatumTijd`,
  symmetrisch met wat `parseDatum` al doet. Bevestig of weerleg dat met
  een meting vóór de wijziging.
- Rood-eerst: een getuige die vandaag stil skipt en na de fix speelt.
  Bij voorkeur een golden met een `DatumTijd`-literal in het canonieke
  lexeem (die het corpus nu helemaal niet heeft), zodat gate 12 hem
  daarna afdwingt; anders een gepinde probe in de backend-suite.
- De wire-kant: leg het canonieke DatumTijd-lexeem vast — de
  WF-084-passage scopet het expliciet uit, dus dit is spec-voorstel-werk
  (grammatica + de vraag of de milliseconden verplicht, optioneel of
  genormaliseerd zijn) met de bijbehorende negatief-cases wanneer het
  een WF-regel wordt.
- Neutraliteitsbewijs: dezelfde betekenis in beide lexemen levert
  hetzelfde speelresultaat — geen SKIP-asymmetrie meer tussen het
  snippet en het canonieke lexeem.
- Alle dertien gates groen; geen bestaande pin beweegt. Let op gate 10
  (de frontend-suite compileert dezelfde klasse) en gate 13 (de
  DRL-teksttak leest via `DrlTranspiler` ~r. 3716 dezelfde lezer).

May touch: `frontends/regelspraak/**` (de kern-lezer) of
`backends/drools/**` — welke van de twee, beslist de meting hierboven;
`spec/IR-SPEC.md` (voorstel, de WF-084-passage en zo nodig een eigen
regel + indexrij), `corpus/golden/**` (nieuwe getuige),
`corpus/negatief/**` (indien het een WF-regel wordt), `backlog/`.

Out of scope: de datum-ordeningsclausule van WF-036 zelf (geland,
2026-09-05); de dag-precisie-kant van WF-084 (dicht); het bijtrekken van
`Typering.vergelijk` op de precisienaad (eigen restpunt op die kaart);
`toka/`.

---

Result (2026-09-06, branch `card/drools-datumtijd-iso`): **geland** — de
lezer, het neutraliteitsbewijs op beide Drools-takken, een gepinde
getuige in de backend-suite en een BESCHRIJVENDE lexeempassage in
IR-SPEC ch. 4.1. **Niet geland: de nieuwe golden** — die route bleek
door een meting geblokkeerd (hieronder). Alle dertien gates groen.

**1. De landingsplaats, gemeten en bevestigd.** `backends/drools/pom.xml`
r. 35 zet `<frontend.src>` op de frontend-kern en r. 112–121 voegt die
via build-helper `add-source` aan de backendbron toe, dus de backend
compileert `Literalen` mee. Beide takken lezen via diezelfde methode:
`DrlTranspiler.java:3716` roept `Literalen.parseDatumTijd` rechtstreeks
aan, en `ExecModelTranspiler.java:2227` delegeert er zijn eigen
`parseDatumTijd` naartoe (gebruikt op r. 3600 en 4602). Eén plek bedient
dus beide takken — de kaart-hypothese klopt.

**2. Rood-eerst, met een tweede vondst.** Getuige: één objecttype, één
`DatumTijd`-attribuut, één kenmerktoekenning met een `EERDER`-voorwaarde
tegen een `DatumTijd`-literal; alleen het lexeem verschilt. VÓÓR de fix,
letterlijk uit de backend-CLI:

    exec-model, canoniek : EXCLUDE transpiler SKIP: regel:
        Kenmerktoekenning verjaard — regel (voorwaarde buiten Phase-B
        scope): Kenmerktoekenning verjaard
    exec-model, snippet  : PLAYED 1
    DRL-tekst,  canoniek : FAIL: kenmerk verjaard: verwacht aanwezig,
        werkelijk afwezig
    DRL-tekst,  snippet  : PLAYED 1

De DRL-teksttak was dus ERGER dan een skip: `DrlTranspiler.literal` viel
bij een null uit `parseDatumTijd` door op de DATUMlezer, die het
datumprefix van het ISO-lexeem matcht, en vergeleek de datum-tijd tegen
MIDDERNACHT — een stil fout antwoord zodra het tijdstip beslist. Met een
middernacht-drempel (de vorm uit de kaart) speelde die tak toevallig
goed; pas een subdag-drempel legt het bloot. NA de fix spelen alle acht
combinaties (twee lexemen x twee takken x twee modellen) `PLAYED 1`.

**3. De fix (smallest change).** `Literalen.parseDatumTijd` laat zijn
DATUMdeel nu door `parseDatum` lezen, dat sinds WF-084 al ISO-eerst,
dan-dag-eerst leest; de tijdlezer en de milli->nano-schaal blijven
ongewijzigd. De EMITTER is onaangeraakt: `ExpressieBouwer:379` schrijft
voor `DatumTijd` nog steeds het snippet (r. 385 schrijft voor een
dagdatum wél ISO) — gemeten status quo, genoteerd als open flank in de
spec-passage.

**BIJVANGST, expliciet:** de doorval in `DrlTranspiler.literal` is
terminaal gemaakt — een onleesbaar `DatumTijd`-lexeem markert nu
(`Frag.todo("datumtijdliteral", …)`) in plaats van naar de kale datum te
degraderen. Dit gaat één stap verder dan "de lezer leert het lexeem",
maar het sluit precies het stille-fout-antwoordpad dat de meting boven
aantoonde, en het valt binnen "May touch". Gemeten na de fix op een
kapot lexeem: exec-model EXCLUDE (SKIP), DRL-tekst `EXCLUDE
drl-transpiler MARKER: … TODO voorwaarde: …` — beide takken weigeren
luid, geen enkele degradeert.

**4. Waarom er GEEN nieuwe golden is.** Gemeten, vóór iets te bouwen:
het corpus (en de parityzone) bevat NUL `DatumTijd`-literals, en dat is
geen toeval. `ref` heeft in `literal_value` helemaal geen
`Datum_tijd`-arm — een `DatumTijd`-literal geeft `EVALUATION FAULT:
literal of this datatype not implemented`, in BEIDE lexemen. De
ASP-backend faalt er nog vóór: `decimals_of_datatype` faalt op
`Datum_tijd` (`TRANSLATION FAULT: unscalable datatype in the ASP
translation`) en zijn `dt_term` draagt een eigen-subject-lezing en
datum-tijd ± duur, maar geen literal. Een golden met zo'n literal zou
gate 5 én gate 6 rood maken; hem repareren betekent twee executors
uitbreiden met nieuwe semantiek (de waarde van een datum-tijd-literal in
de ms-laag) — een eigen kaart, geen "smallest change" en buiten deze
kaarts May-touch. De kaart voorzag dit alternatief expliciet ("anders
een gepinde probe in de backend-suite"); die route is genomen.

**5. De getuige die er wél is.**
`backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/DatumTijdLexeemWitnessTest.java`
(NIEUW, additief, 5 tests): beide lexemen x beide takken op een
subdag-drempel, plus een millisecondenmodel en de weigering van een
onleesbaar lexeem. Rood-eerst bewezen tegen de ONgefixte bron in een
/tmp-kopie: `Tests run: 5, Failures: 2, Errors: 2` — de vijfde
(snippet-lexeem) blijft groen, dus de getuige is niet om een triviale
reden rood. Pins handberekend (`EERDER` is strikt `<`) en bevestigd door
`ref` op de LITERAALVRIJE tweeling van elk model (de drempel als tweede
`DatumTijd`-attribuut via de case-wire, wat ref wél draagt): `oracle:
PASS (3 expectations)` op beide; één pin omgedraaid maakt ref rood.
Gate 12 draait deze suite mee, dus de getuige is blijvend afgedwongen.

**6. De wire-kant: beschrijvende passage, GEEN nieuwe MUST.** IR-SPEC
ch. 4.1 krijgt na de WF-084-alinea "The datetime-literal lexeme
(descriptive; not a WF rule yet)": `date 'T' hh ':' mm ':' ss ( '.' fff
)?`, vaste veldbreedtes, `T` (nooit een spatie), seconden altijd
geschreven, geen zone-achtervoegsel, milliseconden optioneel maar exact
drie cijfers. Elk veld op meting:
`T` -> spatie geeft `malformed dateTime`; `…T09:00` (geen seconden) ->
`malformed dateTime`; `…T09:00:00Z` -> `malformed dateTime`; maar een
afwijkende FRACTIEBREEDTE wordt door niemand geweigerd en dáár lopen de
lezers stil uiteen: `.1` is bij BEIDE lezers één milliseconde (niet een
tiende seconde), en `.100000` is 100 000 ms voor de case-lezer
(gemeten: MISMATCH) tegen 100 ms voor de Java-literal-lezer. Drie
cijfers is de enige breedte waarover elke lezer het eens is — en precies
wat de case-wire (`uchar-case-v0`, `dateTime`-patroon) al pinde. De
smalste vastlegging is dus: het lexeem van de case-wire, ook op de
IR-wire.
GEEN WF-regel, en dat is een besluit op twee gronden, niet op smaak:
(a) een MUST vereist hier een DOORLOPEN golden, en die kan niet bestaan
zolang ref en de ASP-backend de literal niet dragen — alle 92
WF-indexrijen dragen een golden, er is geen precedent voor een lege
kolom; (b) een MUST zou de eigen emitter per constructie
non-conform maken (die schrijft nog het snippet) terwijl deze kaart de
emitter bewust niet aanraakt. De passage benoemt beide drempels als wat
er moet landen vóór de regel komt. Er zijn dus ook geen
negatief-cases — die horen bij de MUST, niet bij de beschrijving.

**7. Bewust open gelaten (kandidaat-kaarten).**
- `ref` + de ASP-backend een `DatumTijd`-literal geven; dan pas kan de
  golden bestaan en kan de beschrijvende passage een genummerde MUST met
  verifier-check, golden en negatief-cases worden.
- De emitter voor `DatumTijd` het canonieke lexeem laten schrijven
  (`ExpressieBouwer:379`), symmetrisch met wat r. 385 voor een dagdatum
  al doet — dat vergt een emitter-golden-herpin, dus een eigen kaart.
- Voorstel, NIET uitgevoerd (frontend-testbestanden zijn buiten mijn
  mandaat): `frontends/regelspraak/src/test/.../kern/LiteralenTest.java`
  r. 20–27 pint `parseDatumTijd` alleen op het snippet. Daar hoort de
  eenheidsgetuige voor het canonieke lexeem: `parseDatumTijd(
  "2024-12-25T10:30:00.500")` gelijk aan de snippetvorm, plus een null
  op `"2024-12-25T10:30"`. De hoofdsessie stelt de maintainervraag.
- De niet-onderscheidende SKIP-reden van de exec-model-tak ("voorwaarde
  buiten Phase-B scope") staat er nog; dat was de tweede les van deze
  kaart maar niet haar doel.

**8. Gates (eindstand, alle dertien groen, vanuit de worktree).**
1 self-contained PASS; 2 ir-schema PASS (177 valideren + round-trippen);
3 verifier PASS (177 golden clean, 303 negatief afgewezen); 4
case-schema PASS (429 casebestanden); 5 oracle PASS (407 cases exact, 2
zonder case); 6 clingo PASS (407 cases); 7 noop PASS (371 weerleggen, 36
gepinde overlevers); 8 rood PASS (22 rode cases; Droolsbeen 5
fault-pinned, 17 niet-gedragen); 9 catala PASS (201 checks, 12 goldens);
10 regelspraak PASS (171 tests, 30 rgs-goldens byte-identiek —
ONBEWOGEN, de fix raakt de lezer niet de emitter); 11 toka-pariteit PASS
(45 rgs byte-identiek, 27 tests, 29 replays door drie executors —
onbewogen); 12 drools PASS — **played 109, auto-excluded 43,
value-divergence 1, no-case 2** exact als de basislijn (gemeten in een
/tmp-kopie van de claim-commit), **JUnit 199 -> 204**: zuiver additief;
13 reftype-derivation PASS (68 twists over 14 goldens, 64 replays, 12
door de DRL-teksttak gespeeld — de vloer houdt —, 0 overlevers), cijfer
voor cijfer gelijk aan de basislijn.

**9. Voor de hoofdsessie.** De landing raakt `frontends/regelspraak/`,
dus na de merge is een herpin MET re-vendor nodig richting de
toolchain-repo. Niet zelf gemerged; de branch blijft staan voor de
lensronde. `spec/IR-SPEC.md` is een beschermd pad: de passage is een
VOORSTEL.
