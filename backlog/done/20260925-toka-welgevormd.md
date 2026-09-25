# toka-welgevormd: de pariteitszone is ook welgevormd op de v1-wire

Maintainersbesluit 2026-09-25: de zone `toka/` blijft nog een poos, in haar
eigen map, en hoort dan ook welgevormd te zijn op de nieuwe wire — niet
alleen byte-identiek her-emitteerbaar (gate 11). Gemeten op main a7360d1c
met de verifier van vandaag: 28 van de 30 documenten geven exit 1.

- 27 van de 29 scenario-slices falen op precies één regel: WF-016 op
  `/model/timelines` — de casus declareert een tijdlijn met variabel
  startpunt, en het slicen per scenario houdt het hele model maar knipt de
  regellaag, zodat de startpuntbepaling alleen in de twee
  startpunt-slices (toka-43, toka-44) staat; die twee zijn schoon. Een
  artefact van het slicen; de toolchain merkt het ontbrekende anker pas bij
  het transpileren.
- toka-24 heeft daarbovenop WF-099: de ondergrens van een begrenzing met
  een eenheid die niet bij haar basis past. Oorzaak nog niet uitgezocht:
  een emitterkeuze (de literal-grens neemt de eenheid van de basis niet
  over) of een casus-eigenaardigheid zoals toka-45 (dan een holdout met
  reden).
- Het volledige casusdocument (`toka/casus/ir.json`) is overbepaald door
  zijn aard: de scenario's schrijven hetzelfde doel elk anders (zes
  verdelingen van hetzelfde bedrag in de scenario's 32–37): zes keer
  WF-026, acht keer WF-034, en één keer WF-022, WF-045, WF-079 en WF-099.
  Als losse scenario's legitiem, als één document niet; dat document blijft
  het pariteitsobject (byte-identieke her-emissie) en hoeft niet
  welgevormd te zijn.

Rechten: alles blijft in `toka/`, niets uit de zone wordt geciteerd
(paden, pointers en tellingen mogen); de repo blijft privé zolang de zone
bestaat (CLAUDE.md, corpusregels).

## Acceptatie (bij oppakken aanscherpen)

- (a) Elke slice draagt de startpuntbepaling van de casus (de slices zijn
  handgehouden bestanden die per landing mechanisch mee-migreren; leg de
  regel vast in `toka/README.md`: een slice draagt naast zijn scenario
  alles wat zijn model verankert). Meet dat de 29 replays (ref, clingo,
  Drools) daarna dezelfde uitkomst geven; verandert een uitkomst, dan is
  dat een nulmeting die de kaart vastlegt.
- (b) toka-24: de WF-099 uitgezocht; een emitterfout is een frontendkaart
  (of deze, als hij klein is en vóór de bump valt), een casus-eigenaardigheid
  een holdout met reden naast toka-23 en toka-45.
- (c) Gate 11 (**voorstel**, `scripts/check-toka-parity.sh`) krijgt een been
  "elke slice is verifier-schoon", met een reden-dragende holdoutlijst die
  faalt als hij verouderd is (het patroon van de bestaande holdouts); het
  casusdocument blijft buiten dat been, met de reden erbij.
- (d) `toka/README.md` beschrijft de welgevormdheidsstand; alle veertien
  gates groen.

May touch: `toka/**`, `scripts/check-toka-parity.sh` (**voorstel**),
kaartverplaatsing; `frontends/regelspraak/**` alleen als (b) een
emitterfout blijkt.

## U-besluiten (hoofdsessie, 2026-09-25, bij de claim)

- U1 Slices: elke slice krijgt het ene element van `rules.startPointDeterminations` van `toka/casus/ir.json` (toka-43 en toka-44 dragen het al) — mechanisch, met een script buiten de repo (niets citeren), in de sleutelvolgorde van het schema; daarna verifier over alle 29 slices; gate 11 (metaschema, her-emissie en de 29 replays in ref, clingo en Drools) onbewogen — een veranderde replay-uitkomst is een nulmeting die je vastlegt en niet wegwerkt.
- U2 toka-24 (WF-099 op een begrenzingsgrens): zoek uit of de emitter de eenheid van de basis op een literal-grens hoort te zetten (WF-099: een eenheidloze literal neemt de eenheid van de basis over — dus vuurt de regel alleen bij een échte eenheidsbotsing; vergelijk met een golden met een `Clamp`), en of de casus daar een eigenaardigheid heeft zoals toka-45. Emitterfout: fix in `frontends/regelspraak/` met een rood-eerst JUnit-getuige en meld het (vóór de pin-bump); casus-eigen: holdout met reden.
- U3 Gate 11 (**voorstel**, `scripts/check-toka-parity.sh`): een been "elke slice is verifier-schoon" met een reden-dragende holdoutlijst die faalt als hij verouderd is (het patroon van `HOLDOUTS`), en het casusdocument buiten dat been met de reden (overbepaald door zijn aard: de scenario's schrijven hetzelfde doel elk anders).
- U4 `toka/README.md`: de slice-regel (een slice draagt naast zijn scenario wat zijn model verankert) en de welgevormdheidsstand; geen inhoud uit de zone in README, Result of commits.
- U5 Gates 1, 2, 3, 4, 11 (de replays van de zone zitten in 11); als U2 de emitter raakt ook 10 en 12.

## Result (2026-09-25, bouwer)

## Result toka-welgevormd

### Ronde 2 (lensfix)

Alle vondsten van de toka-lens zijn gefixt: B1–B4, A1–A12 en het open punt over de negatief-README. A13 was een commit van de hoofdsessie en is niet herschreven. De gevraagde gates 1, 2, 3, 5, 6, 8 en 11 zijn groen, en gate 12 ook: die draaide ik omdat A10 vier zinnen van de Drools-lader raakt. De naamtabeltoets geeft OK.

#### Stand
- Branch `fix/lens-toka`, vanaf `card/k7-unie` (36750511), in worktree (worktree toka).
- Kop **f647a6dd**: 19 commits, één per vondst, niet gepusht en niet gemerged.
- Beschermde paden zijn **voorstellen**: IR-SPEC, RUNTIME-SEMANTICS, `spec/verifier/`, `scripts/`, ASP-TRANSLATION.

#### Per vondst: meting vóór → na (commit)
- **B1** (a98e9b94). De casusdump geeft op de unie 15 meldingen, niet 18.
  - WF-026 geeft 3 meldingen (één per pointer `/rules/allocations/{0,1,2}/versions/0`), was 6.
  - Oorzaak: `dedup_reports` in `wf026` uit commit 3ddbf701, kaart `verifier-allocatie-keten`.
  - Aangepast in `toka/README.md` (twee plekken plus de regel van de frontend-landing) en in het commentaar van gate 11. De reden staat erbij.
- **B4** (e8d3ce3c). De eenheidsafkorting van de casus-literal is weg uit de README en uit het commentaar van gate 11; de pointer volstaat.
- **A6** (c973f181).
  - Vóór: onder `/bin/bash` 3.2.57 met een lege `VERIFIER_HOLDOUTS` (en toka-24 tijdelijk schoon) gaf de gate `VERIFIER_HOLDOUTS[@]: unbound variable`, rc 1.
  - Na: `${a[@]+...}` en in de PASS-regel `verifier holdouts: none`; zelfde proef rc 0 en PASS.
- **A11** (9420531e). De kopregel van gate 11 telt de divergenties niet meer. Hij verwijst naar de README (8 gedateerde re-pins) en noemt alleen de eerste.
- **A7** (f55c4f52). In de README:
  - "every replay slice but one named holdout";
  - "26 on it alone, toka-24 on WF-099 as well";
  - de voorwaarde voor de schrijfregel van het anker. Gemeten: 0 tijdlijnfacetten in de 30 documenten.
- **B2** (e4126b0c). De WF-099-rij zegt nu "the one replay slice that carries the one-sided pair, parity slice 24, is its named holdout on this report; the casus dump, which carries the same rule, stays outside that leg", en de punt vóór "**Since" staat er.
- **A1** (c1a0e492). "the slice parity slice 24" is op twee plekken gecorrigeerd, de losse komma in r. 3313 is weg, en de regels zijn opnieuw gewrapt.
- **A2** (c56247d9). r. 6105–6109 heeft nu de vorm van r. 4712: "*Since 2026-09-25 … the sentence records the state on its date.*"
- **A3** (ee3e8efe). De WF-092-rij zegt nu: "resolves every cited pointer in its document" (gate 3, check 4).
- **B3** (ddc43caf). Zelf nagemeten over 301 golden- en rooddocumenten, op kopieën van de verifier:
  - identiteitsmutant (`keuzetype_same` byte-exact): vaarwaterspelling én meldtermijnen worden rood, beide met WF-023 op `/rules/rules/1/versions/0`;
  - opzoekmutant (`enum_decl` byte-exact): alleen vaarwaterspelling wordt rood (WF-063).
  - Meldtermijnen staat nu genoemd in de WF-089-rij en in de NOTE van vaarwaterspelling.
- **Open punt negatief-README r. 565** (1a68374b). De rij citeert nu "(Jutter, uitkomst)", het fragment dat de getuige pint.
- **A4** (55baa929). De pointer loopt tot de eerste ": ", spaties inbegrepen; een FAIL-regel zonder dat scheidingsteken laat de gate vallen.
  - Proef met een kopie van gate 3 en een verifier-wrapper. Vóór: beide synthetische regels rc 0, PASS met 564 pointers. Na: rc 1 met "does not resolve" respectievelijk "reports a line with no readable pointer".
  - De echte gate blijft PASS; nu 567 pointers door de nieuwe negatiefs.
- **A5** (7b5f8869). In `admit.sh` telt alleen grep-exit 1 als "geen treffer".
  - Proef met een onleesbaar bestand. Vóór: "Permission denied", en pas de verifier (check 2) weigerde. Na: check 1 weigert met "(grep exit 2)". Er wordt niets gekopieerd.
- **A8a** (d7776c79, voorstel). WF-091 claimt de heelheid bij `times` naast de eenheid, plus een nieuwe arm voor `dividedBy` en `power` die alleen de heelheid claimt. Eenheid, soort en decimalen blijven ongeclaimd, dus de uitzonderingsset van gate 13 verandert niet.
  - Vóór: 0 van 131 zulke knopen was whole (57 `times`, 74 `dividedBy`/`power`, over golden, rood, negatief en de zone). De drie nieuwe getuigen waren verifier-schoon.
  - Na: elk van de drie faalt op zijn arm. Sweep over 793 documenten: 0 rapporten veranderd.
  - Drie mutanten (heelheid weer overgeslagen bij `times`, arm zonder `dividedBy`, arm zonder `power`) maken elk alleen hun eigen getuige schoon.
  - Nieuwe negatiefs: `wf091-maal-geheel`, `wf091-deling-geheel` en `wf091-macht-geheel`, elk met `expect.txt` en `NOTE.md`.
  - IR-SPEC is bijgewerkt in ch. 4.1 (de times-bullet en de vierde klasse) en in de WF-091-rij.
- **A8b** (9d81d387). Gate 2 heeft een check 4: elk object van golden, rood en negatief (in expliciete modus: van de opgegeven bestanden) staat in de sleutelvolgorde van zijn subschema.
  - Gemeten: 36497 objecten in 766 documenten, 0 afwijkend, gelijk aan de meting van de lens.
  - 1 object valt onder geen subschema: `wf113-marker-bron-null`, dat het schema bewust breekt. Het wordt apart geteld.
  - Proef: een verwisseling in een golden en in een negatief laat de gate elk vallen, met pad en beide volgordes.
- **A9** (d052e01b). De WF-109-zinnen staan nu één keer in `checks.ml` (`wf109_host_sentence`). De verifierarm gebruikt die functie, en ref (`guard_on_doel`, dat clingo aanroept) ook.
  - De verifierrapporten over alle 793 documenten zijn byte-gelijk gebleven.
  - Twee suitepins zijn aangepast (ref en clingo).
  - De NOTE van `wf109-subtype-overlap` citeert nu de gedeelde zin, zoals gemeten in de ref-suite.
- **A10** (a2ca4718, f647a6dd). Gevonden: 119 staarten, 50 in ref en 69 in clingo; 94 bestaan alleen uit regelnummers, 25 bevatten ook uitleg.
  - 115 zijn weg. Volgens welke regels: zie "Technische keuzes".
  - 4 blijven bewust staan. Zie open punt 2.
  - Vier Drools-kopieën zijn mee aangepast (IrLoader r. 435, 513, 1922 en 1928) om letterlijk gelijk te blijven, samen met hun JUnit-pins.
  - Verder gevolgd: de suitepins in ref en clingo (onder meer twee tests die "WF-013" en "WF-043" als naald hadden), de bodemzin `value_condition` in gate 8, twee Drools-testcommentaren, de NOTE van wf064 (de nieuwe ref-uitvoer zelf gemeten), het citaat in de WF-092-rij, twee citaten in ASP-TRANSLATION (S15/S17) en een hermeting in de NOTE van `wf034-rollidmaatschap-vullersklasse-onbekend`.
  - De telling van de lens (31/50) kon ik niet exact reproduceren. "Exacte staart op één regel" geeft 32/54. Ik heb de volledige verzameling genomen.
- **A12** (971542a8). RS ch. 2.3 heeft er een voorbeeld bij: "3 × 4, two whole operands, types at 0 decimals and fractional although its value is 12".
  - De getuige in de ref-suite is WV-2.3-maal-niet-heel. Hij toetst de waarde (ref rekent 12) en het statische type via de checks-bibliotheek die ref linkt: whole geeft precies de WF-091-bevinding, fractioneel geeft niets.
  - Suite: 1019 → 1022 voorbeelden. Een mutant die de claim schrapt laat alleen die check vallen.
  - De kop van de suite (WV-2.3 buiten scope) noemt dit ene voorbeeld.

#### Technische keuzes
- **Regels voor A10:**
  - Een staart met alleen regelnummers valt weg.
  - "(WF-x should preclude this)" wordt "(the verifier should preclude this)".
  - "(unreachable on a verifier-clean document: …)" wordt "(unreachable on a verifier-clean document)". De uitleg staat al in het commentaar erboven.
  - "(WF-x; tekst)" en "(WF-x: tekst)" worden "(tekst)".
  - Testnamen houden hun WF-nummer.
- **Plaats van de heelheidsclaim:** in WF-091, de regel van de afgeleide typen, niet in een nieuwe regel.

#### Definitieve weigerzinnen (letterlijk, alleen in de verifier)
- `wholeness mismatch: the times composite is typed whole, but the type derived from its operands is fractional`
- `wholeness mismatch: the dividedBy composite is typed whole, but the type a quotient derives is fractional`
- `wholeness mismatch: the power composite is typed whole, but the type a power derives is fractional`

WF-109: de vier zinnen van de verifier zijn ongewijzigd; ref en clingo gebruiken ze nu.

#### Gate-uitvoerregels (alle op f647a6dd, elk los na `eval $(opam env)`)
- 1: `check-self-contained: PASS`
- 2: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 301 file(s) validate and round-trip, floor 192; 36497 object(s) in 766 document(s) in the schema's key order, 1 in no subschema)`
- 3: `check-verifier: PASS (301 golden clean; 465 negatief rejected for their declared rule, floor 350; 307 on their declared arm (expect.txt), floor 260; 567 cited pointers resolve in their documents, floor 547)`
- 5: `check-oracle: PASS (ref suite 1022 worked examples, floor 489; 677 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)`
- 6: `check-clingo: PASS (backend suite 1782 checks, floor 83; 677 cases replay exactly through clingo — ref = clingo over the corpus)`
- 8: `check-rood: Drools leg PASS (11 fault-pinned, 32 not-carried with reason)` / `check-rood: PASS (43 red cases fault observably in ref and clingo, floor 26; 86 pinned fragments cite none of the 36 listed bottom sentences)`
- 11: PASS. Her-emissie 45/45 (holdouts toka-23-….rgs en toka-45-….rgs), suite 27, schema 29, `well-formedness: 28 replay slice(s) verifier-clean, floor 28 (verifier holdouts: toka-24 WF-099 /rules/rules/0/versions/0/value/base/minimum)`, replay 29/29/29.
- 12: `check-drools: PASS`; played 204 (pin 204), DRL-text 177 (pin 177), JUnit 800 tests, floor 239.
- Naamtabel: `v1-naamtabel-toets: OK`.

#### Gewijzigde scripts (beschermd pad, dus voorstel)
- `scripts/check-toka-parity.sh`:
  - reden en telling van de casusdump (15, met reden);
  - de afkorting weg;
  - `${VERIFIER_HOLDOUTS[@]+…}` op twee plekken, en `${vh_list:-none}` in de PASS-regel;
  - kopregel 27 herschreven.
- `scripts/check-verifier.sh`:
  - awk-parse met `index(rest, ": ")`;
  - een nieuw tijdelijk bestand `noptr`, en de weigering `FAIL: negatief case %s reports a line with no readable pointer (no ": " after "FAIL "): %s`;
  - de kopbeschrijving van check 4.
- `scripts/check-ir-schema.sh`:
  - een nieuwe check 4 (sleutelvolgorde) ná de bestandslus, dus buiten het stuk dat gates 9/14 en m1–m5 uitvoeren;
  - weigering `FAIL: %s at %s: the keys %s are not in the schema's order %s`;
  - de PASS-regel uitgebreid, en de kop "Six checks … runs 1-4".
- `scripts/check-rood.sh`: de bodemzin `value_condition` zonder "WF-077; ".
- `scripts/admit.sh`: de functie `selfcheck` (exit 0 is een treffer, 1 is geen treffer, anders weigering "(grep exit N)").

#### Wat aan de lensteksten niet klopte (gemeten)
1. De telling van A10 (31/50) heb ik niet kunnen reproduceren; de volledige verzameling telt 119.
2. A10 is niet alleen iets van ref en clingo: vier zinnen hebben een letterlijke kopie in IrLoader. Daardoor moest gate 12 wél draaien.
3. A12 "als ref-suitegetuige": de ref-suite sloot WV-2.3 bewust uit (statisch). De getuige loopt nu via de checks-bibliotheek die ref linkt, en de kop van de suite zegt dat.

#### Open punten
1. **ASP-TRANSLATION S18** (het gebied van fix/lens-clingo) citeert op r. 2829–2830 en r. 2837 twee WF-077-zinnen van clingo nog mét hun oude staart. Die citaten moeten mee aangepast worden bij de merge van S18.
2. **Vier staarten blijven** in ref (eval.ml r. 1634 en 1648) en clingo (translate.ml r. 1725 en 1735): WF-080 bij een aggregatiebron en WF-034 bij de bron van een waardevouw. Hun letterlijke kopie staat in de bronarm van IrLoader (r. 1747 en 1768, het gebied van fix/lens-bronarm). Ze moeten samen weg. De WF-093-zin was al ongelijk: ref en clingo hadden "(WF-093 (a))", Drools "(WF-093)". Hij staat nu zonder staart in ref en clingo en met staart in Drools.
3. **De Drools-lader heeft nog eigen staarten** die geen ref/clingo-kopie hebben: IrLoader r. 707, 759, 766, 985, 1146, 1912 en 1916, plus de Nederlandse redenen van de transpilers. Dat valt buiten A10.
4. **CLAUDE.md** (alleen de hoofdsessie) beschrijft gate 2 zonder de nieuwe check 4 en gate 3 zonder de regel "geen leesbare pointer".
5. **HeelperiodeDoelpogingWitnessTest** citeert in een gedateerd commentaar ("Measured RED FIRST") nog de ref-zin met "(WF-043)". Ik heb dat als historische meting laten staan.
6. **Bekend en niet in deze ronde gesloten:**
   - het eenzijdige eenheidspaar is één besluit;
   - `detail_vs` toont typen die alleen in teken verschillen nog gelijk (verifier-fixer, A6);
   - de lijsten van WF-091 en gate 13 worden met de hand gelijk gehouden.

#### Buiten de repo
Scripts en proeven in (buiten de repo): `a4-proef.sh`, `a5-proef.sh`, `a6-proef.sh`, `a8-getuigen.py`, `a8b-proef.sh`, `a10-sweep.py`, `verifier-sweep.sh`, `wf-staarten.py`. De verifierkopieën voor de mutanten staan in (buiten de repo).

---

### Ronde 1 (bouw)

### Stand
Branch `card/toka-welgevormd`, worktree (worktree toka), kop **cb1da0e9**
(basis 20e508d5, claim 1a7f8c2b). Vier commits, niet gepusht of gemerged:
- 422b8c47 — U1: de 27 slices zonder anker krijgen de startpuntbepaling plus haar ankerlid
- e470686a — U3: gate 11 (voorstel), het been "elke replayslice is verifier-schoon"
- 0a3c1d1d — U4: `toka/README.md`, de slice-regel en de welgevormdheidsstand
- cb1da0e9 — correctie: de casusmeldingen in README en scriptcommentaar naar hun gemeten oorzaak
Geen wijziging in `frontends/regelspraak/`, dus gates 10 en 12 zijn niet nodig.

### Tellingen (verifier over de 30 documenten; alleen WF-nummers en pointers)
- **Vóór** (20e508d5 + claim): 28 van de 30 geven exit 1. 27 slices: 1 × WF-016 op
  `/model/timelines`. toka-24: WF-016 op `/model/timelines` plus WF-099 op
  `/rules/rules/0/versions/0/value/base/minimum`. toka-43 en toka-44: exit 0. `toka/casus/ir.json`:
  18 meldingen (6 × WF-026, 8 × WF-034, 1 × WF-022, WF-045, WF-079, WF-099). De meting van de kaart klopt.
- **Proef met alleen de bepaling** (volgens de letter van U1, teruggedraaid): 27 × WF-032 op
  `/rules/startPointDeterminations/0`, arm "anchor member '…' is not a declared attribute of '…'".
- **Na** (cb1da0e9): 2 van de 30 geven exit 1: toka-24 (1 × WF-099, zelfde pointer) en het
  casusdocument (18, ongewijzigd). De 28 andere slices geven exit 0.
- Slices: 27 bestanden, elk +16/−1: de bepaling op `/rules/startPointDeterminations/0` en het lid op
  `/model/objectTypes/1/attributes/21`. toka-43 en toka-44 zijn byte voor byte onaangeroerd. De 29
  slices dragen nu één identiek model, gelijk aan dat van toka-43/44.
- Replays: 116 uitvoerbestanden (29 × ref, clingo, Drools-stdout en Drools-stderr) zijn byte-identiek
  vóór en na U1. Manifestklassen onveranderd: 29 PASS / 29 PASS / 29 PLAYED. Geen nulmeting.

### Oordeel toka-24: eigenschap van de casus, geen emitterfout → holdout met reden
- Op de pointer: value = Rounding. De base daarvan is een Clamp met alleen een minimum. De basis van
  de Clamp is een Binary `minus` van een AttributeReference en een ParameterReference, beide gedeclareerd
  als Number zonder eenheid (2 decimalen, via een eenheidloos numeriek domein). Het minimum is een
  Literal 0 met eenheid EUR; het bronfragment is 3 tekens en bevat het euroteken. Het domeinfragment
  van de casus heeft geen eenheid. Arm: "clamp minimum and base have incompatible units".
- Dit is het eenzijdige paar: de basis heeft geen eenheid en de grens wel. Adoptie geldt alleen voor
  een literal **zonder** eenheid; hier is de literal juist de kant met een eenheid. IR-SPEC ch. 4.1
  laat deze klasse open als één besluit voor WF-023, WF-035, WF-036 en WF-099 samen. De casus-regel
  is dezelfde regel als casus `/rules/rules/6` (plain-gelijk).
- Eigen, rechtenvrij proefdocument ((buiten de repo), frontend via `make ir`, daarna de verifier):
  - eenheidloze basis met grens "0 €" → WF-099 op `/rules/rules/0/versions/0/value/minimum`;
  - eenheidloze basis met grens "0" → PASS;
  - €-basis met grens "0" → PASS: de literal wordt zonder eenheid geschreven en de verifier adopteert.
  De frontend geeft in alle drie gevallen 0 diagnostics. Hij schrijft dus de eenheid die de bron
  schrijft. De hypothese van U2 (de grens neemt de eenheid van de basis niet over) is gemeten precies
  andersom.

### Gate-uitvoerregels
Gates 1 en 11 zijn gedraaid op cb1da0e9. Gates 2, 3 en 4 zijn gedraaid op 0a3c1d1d; cb1da0e9 raakt
alleen README-proza en scriptcommentaar, en die lezen gates 2–4 niet.
- gate 1: `check-self-contained: PASS`
- gate 2: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 293 file(s) validate and round-trip, floor 192)`
- gate 3: `check-verifier: PASS (293 golden clean; 433 negatief rejected for their declared rule, floor 350; 275 on their declared arm (expect.txt), floor 260)`
- gate 4: `check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 686 case file(s) validate)`
- gate 11 (de bestandsnamen van de her-emissie-holdouts ingekort):
  ```
  check-toka-parity: PASS
    re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: toka-23-….rgs toka-45-….rgs); TOKA suite 27 tests, floor 27
    schema: 29 replay slice(s) metaschema-valid + byte-round-trip (gate-2 discipline, explicit mode)
    well-formedness: 28 replay slice(s) verifier-clean, floor 28 (verifier holdouts: toka-24 WF-099 /rules/rules/0/versions/0/value/base/minimum); the casus dump is outside this leg (the parity object, not a well-formed program)
    differential replay: 29 toka cases (floor 29) through ref, clingo, Drools (slices under toka/cases/), ZERO silent divergences
      ref    : 29 PASS / 0 diverge  (the 100% oracle carries every toka case)
      clingo : 29 PASS / 0 loud translation-fault / 0 silent diverge
      Drools : 29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR
  ```
- Zeven proeven op het nieuwe been, elk hersteld uit HEAD en elk rc=1 met een eigen melding:
  1. schone slice zonder anker ("not verifier-clean");
  2. holdout schoon gemaakt ("verifier-clean now — … stale");
  3. holdout met een tweede melding ("its report differs");
  4. sleutel zonder slice ("resolves to 0");
  5. sleutel met drie slices ("resolves to 3");
  6. pin op een andere pointer ("its report differs");
  7. vloer 29 ("EXPECTED_MIN_CLEAN pins at least 29").

### Gewijzigde scriptregels — `scripts/check-toka-parity.sh`, **voorstel** (beschermd pad), +100/−12
- Kopcommentaar: "Five" → "Six obligations". Nieuwe verplichting 4 (welgevormdheid); de replay wordt
  5 en de laatste 6. De toolchainzin noemt nu ook de verifier van gate 3, en verplichting 6 noemt
  dune ook voor de verifier.
- `EXPECTED_MIN_CLEAN=28       # 2026-09-25: measured 28 (verifier-clean replay slices; the verifier holdouts not counted)`
- `VERIFIER_HOLDOUTS=( 'toka-24 WF-099 /rules/rules/0/versions/0/value/base/minimum' )`, met de reden
  van toka-24 en die van het casusdocument in commentaar. De sleutel is het scenarionummer en wordt
  via `scenario-<key>-*` opgelost naar precies één map. Zo staat er geen scenarionaam in het script.
- Nieuwe sectie 3c. De verifier wordt gebouwd zoals gate 3 dat doet (`dune build --root spec/verifier`).
  Elke slice moet exit 0 geven. Een holdout moet exit 1 geven met als hele rapport precies de gepinde
  regel en pointer. De gate faalt als een holdout verouderd is (in beide richtingen), op de vloer en
  bij een lege set. Het verifierrapport wordt alleen bij falen geprint, omdat de meldingen namen uit de
  zone bevatten. De dune-detectie is van stap 4 naar 3c verplaatst; stap 4 gebruikt haar nog.
- PASS-regel: `well-formedness: $clean_n replay slice(s) verifier-clean, floor $EXPECTED_MIN_CLEAN (verifier holdouts: …); the casus dump is outside this leg (the parity object, not a well-formed program)`
- Er is geen nieuwe weigerzin in de verifier of de laders.

### Technische keuzes (zelf genomen)
- U1 is verbreed tot bepaling **plus** ankerlid, omdat alleen de bepaling 27 × WF-032 geeft. Het lid
  staat als laatste attribuut van zijn objecttype; daar schrijft de frontend het, zoals in de casus en
  in toka-43/44. De schrijfregel van het anker blijft bij zijn eigen scenario.
- De pin omvat het hele rapport (regel + pointer) en niet alleen de regel. Een holdout kan daardoor
  geen tweede of verschoven defect verbergen.
- `toka-24` staat in een eigen lijst `VERIFIER_HOLDOUTS`, niet in `HOLDOUTS`: zijn fragment
  her-emitteert byte-identiek.
- Geen `declare -A`, zodat het script ook onder bash 3.2 werkt; geen ander gatescript gebruikt het.
- De CI-job `toka-parity` levert al dune, yojson en zarith (ref linkt de verifierbibliotheek al).
  `.github` hoeft niet te veranderen.

### Wat aan de kaart niet klopte (gemeten)
1. "het slicen … houdt het hele model maar knipt de regellaag". Twee afwijkingen:
   - De 27 slices misten ook het ankerlid. U1 zoals geschreven had WF-016 ingeruild voor WF-032.
   - Het slicemodel is niet het casusmodel. De slices declareren 4 attributen en 2 kenmerken meer
     (objecttypen 0 en 1), in een andere attribuutvolgorde. Ze zijn gesneden in de scoping van de
     eigen scenariotoetsen van de toolchain (`backlog/done/20260825-toka-stresstest.md`, Methode).
2. De 18 casusmeldingen zijn niet allemaal overbepaaldheid.
   - Concurrerende schrijvers: WF-026 × 6 en WF-079 × 1. Die laatste gaat over de twee
     startpunt-schrijfregels, `/rules/rules/15` en `/rules/rules/16`.
   - WF-045 × 1 sluit alleen over scenario's heen: de slice met de objectcreatie (toka-28) is schoon.
   - Onderdeclaratie: WF-034 × 8 en WF-022 × 1. Elke melding met een naam wijst naar een attribuut of
     kenmerk dat de slices wel declareren en de casus niet. De achtste WF-034 is de daardoor ongetypeerde
     afgeleide positie `/rules/rules/16/versions/0/value/type`.
   - WF-099 × 1 is de regel van toka-24.
   README en scriptcommentaar zeggen dit nu (cb1da0e9).
3. U2: de hypothese dat de literal-grens de eenheid van de basis niet overneemt, is gemeten precies
   andersom (zie het oordeel).
4. Acceptatie (b) vraagt "een holdout met reden naast toka-23 en toka-45". toka-24 is echter geen
   her-emissie-holdout; het is een holdout van het nieuwe verifierbeen, met hetzelfde
   verouderingspatroon.

### Open punten
1. **IR-SPEC (beschermd, voorstel voor de maintainer).** Drie zinnen zeggen dat geen gate de
   pariteitszone door de verifier haalt en dat beide WF-099-dragers "already non-conforming for
   unrelated reasons" waren: ch. 4.1 r. 3280–3282, de WF-099-bullet r. 7328–7331 en de WF-099-rij
   r. 8704. Na deze kaart haalt gate 11 de 29 slices door de verifier, en toka-24 is alleen om WF-099
   niet conform. Voorstel: "the parity zone carries two: the casus dump, which gate 11 keeps out of
   its well-formedness leg, and the slice toka-24, which that leg holds out on this one report (since
   2026-09-25, the card toka-welgevormd)". Twee gedateerde meetverhalen (r. 4673–4677, r. 6061) zeggen
   "no gate runs the verifier over the parity zone". Dat was waar op hun datum; of ze een noot krijgen,
   beslist de maintainer.
2. **Het eenzijdige eenheidspaar op een begrenzing.** Op het eigen proefdocument geeft de frontend 0
   diagnostics waar de verifier WF-099 geeft; alle drie uitvoerders spelen toka-24. Valt het ene besluit
   voor WF-023/035/036/099 op "toelaten", dan meldt gate 11 de holdout als verouderd. Of de frontend
   daarna moet waarschuwen, hoort bij hetzelfde besluit.
3. **De schrijfregel van het anker.** De 27 slices dragen het anker maar niet de regel die het ankerlid
   schrijft. Dat heeft geen gevolg zolang geen lid een tijdlijnfacet op de tijdlijn met variabel
   startpunt draagt (gemeten: 0 in casus en slices). Komt zo'n lid er, dan moet de schrijfregel mee in
   de slice-regel.
4. **Verouderd kopcommentaar van gate 11 (beschermd).** Verplichting 2 zegt nog "Today that list holds
   one entry", maar `toka/README.md` telt zeven gedateerde divergenties. Dit is bestaande tekst, niet
   door deze kaart veroorzaakt. Eén zin corrigeren bij de volgende aanraking.
5. **Acceptatie (d), "alle veertien gates groen".** Volgens U5 zijn gates 1, 2, 3, 4 en 11 gedraaid,
   alle vijf PASS. Gates 5–10 en 12–14 zijn niet gedraaid. Geen daarvan leest een gewijzigd pad: alleen
   gates 1 en 11 lezen `toka/`. Draai bij de landing de volledige set als je dat wilt.

### Bestanden
- Repo (worktree): `scripts/check-toka-parity.sh` (voorstel), `toka/README.md` en 27 × `toka/cases/*/ir.json`.
- Buiten de repo:
  - (buiten de repo): `u1-anker.py` (de U1-bewerking, met proefmodus `--alleen-bepaling`),
    `meet-verifier.sh`, `replay-vastleggen.sh`, `u3-proeven.sh`, `u3-gate11.py` en de ontleedscripts;
    geen van deze bestanden bevat zone-inhoud;
  - (buiten de repo),eenheidloos,adoptie}.rgs`: het eigen proefdocument, rechtenvrij;
  - de zone-afgeleide meetuitvoer in (buiten de repo) is na afloop verwijderd.

## Lens en fixronde (2026-09-25, hoofdsessie)

Lens (toka + restpunten rondes 1–2, unie 36750511, eigen meting in kopieën, uitvoer van de zone na afloop verwijderd): EERST FIXEN — vier blokkerende TEKSTfouten; code, gates en getuigen in orde. Goedgekeurd op eigen meting: de 27 slices dragen precies de startpuntbepaling en het ankerlid (structuurdiff op pointers: +16/−1 per slice, slice 28 daarnaast de connectingRole van de emitterkaart, toka-43/44 ongewijzigd; 29/29 modellen gelijk aan dat van toka-43; alleen de bepaling toevoegen geeft 27 × WF-032); de verifier over de 30 documenten (28 exit 0; toka-24 precies WF-099 op zijn pin; de casusdump 15 meldingen — niet 18: de ontdubbeling per (pointer, zin) van de allocatiekaart halveerde de WF-026-regels, de unieke regels zijn gelijk); gate 11's zeven proeven herhaald, elk rc 1 met de verwachte melding; de replays byte-identiek (116 bestanden), ref/clingo 29 rc 0, Drools 29 PLAYED; de IR-SPEC-passages zonder de zonenaam (gate 1 groen); het oordeel over toka-24 als casus-eigen bevestigd met een eigen rechtenvrij proefdocument. De restpunten van de rondes 1 en 2 elk gereproduceerd op de basis en gezien op de unie (gate 3, WF-079-spelling, RS 2.3, de whole-annotaties met replays 200/200 gelijk, admit.sh, de ref-zinnen, kustwacht, de WF-092-getuigen, de NOTE's, vaarwaterspelling, catala met de flankmeting 40 tegen 43, de sleutelvolgorde 0 afwijkers waar de basis 44 had, WF-070/076, VariableReference).

Blokkerend: B1 (README en gate 11 noemden 18 meldingen op de casusdump, het zijn er 15), B2 (de WF-099-rij noemde slice 24 de enige drager, maar de casusdump draagt dezelfde regel), B3 (de WF-089-rij en de NOTE van vaarwaterspelling zeiden "no other golden", maar de identiteitsmutant maakt sinds ronde 3 ook meldtermijnen rood), B4 (de eenheidsafkorting van een casus-literal stond tussen haakjes in README en script — inhoud uit de zone buiten de zone). Advies A1–A13, waaronder: stijlfouten in IR-SPEC, de WF-092-rij over de pointerresolutie, gate 3 dat een pointer met spatie stil oversloeg, admit.sh en grep-exit 2, gate 11 onder bash 3.2 met een lege holdoutlijst, README-zinnen die met de holdout botsten, geen gate die `whole` op times/dividedBy/power of de sleutelvolgorde bewaakt (nu een verifierarm en een gate-2-toets), de WF-109-zin en 81 regelnummerstaarten in ref en clingo, de gate-11-kopregel, een ontbrekend uitgewerkt voorbeeld voor de heelheid van `maal`, en een te lang commitonderwerp van de hoofdsessie (niet herschreven). Open en bewust zo gelaten: de ankerschrijfregel niet in de slices (0 tijdlijnfacetten, geen gevolg), het eenzijdige eenheidspaar als één besluit voor WF-023/035/036/099.

Fixronde (branch fix/lens-toka, de toka-bouwer): negentien commits op de unie, één per vondst. B1: de vier plaatsen noemen 15 meldingen (WF-026 ×3) met de ontdubbeling als reden; B4: de eenheidsafkorting is weg uit README en gatecommentaar; B2, A1, A2, A3: de WF-099-rij, "the slice parity slice 24", de losse komma, de pariteitszin en de WF-092-rij gecorrigeerd; B3: nagemeten over 301 documenten, meldtermijnen staat in de WF-089-rij en de NOTE; de negatief-README citeert "(Jutter, uitkomst)". A4: gate 3 leest de pointer tot de eerste ": " en een FAIL-regel zonder dat scheidingsteken laat de gate vallen (vóór ging zo'n regel stil door). A5: admit.sh telt alleen grep-exit 1 als "geen treffer". A6: gate 11 met een lege holdoutlijst geeft onder bash 3.2 rc 0 en "verifier holdouts: none". A7, A11: README-zinnen en de kopregel van gate 11. A8: gate 2 heeft een check 4 op de sleutelvolgorde (36497 objecten in 766 documenten, 0 afwijkend; één object onder geen subschema apart geteld; een verwisseling valt met pad en beide volgordes) en WF-091 claimt dat `times`, `dividedBy` en `power` nooit heel zijn (0 van 131 knopen was zo geannoteerd; drie negatiefs `wf091-maal-geheel`, `wf091-deling-geheel`, `wf091-macht-geheel`, elk alleen door zijn eigen arm rood). A9: de WF-109-zinnen één keer in checks.ml, gelezen door verifier, ref en clingo. A10: 115 van 119 regelnummerstaarten weg uit ref en clingo (vier kopieën in de Drools-lader mee; suitepins, een bodemzin van gate 8 en de citaten in NOTE's, IR-SPEC en ASP-TRANSLATION bijgewerkt); de vier die bewust bleven omdat hun kopie in de bronarm van de Drools-lader staat, gingen in een aparte slotronde samen weg. A12: RS ch. 2.3 heeft het voorbeeld "3 × 4, twee hele operanden, typeert fractioneel terwijl de waarde 12 is" met de ref-getuige WV-2.3-maal-niet-heel (suite 1019 → 1022). Gates 1, 2, 3, 5, 6, 8, 11, 12 groen; naamtabel OK. Bij de landing noemt CLAUDE.md de nieuwe check 4 van gate 2 en de pointerregels van gate 3.
