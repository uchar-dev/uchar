# De afleidingswet voor composiet-resultaattypen

Goal: het resultaattype van een composiet heeft geen declaratie
tegenover zich, en WF-091's eerlijkheidsnotitie 4 documenteert dat de
numerieke facetten daar door geen enkele regel worden gekruist; de
eenheidshelft is alleen aan de EXECUTORkant gedicht (EffectieveEenheid
+ gate 13), zodat de onware annotatie zelf verifier-schoon blijft — de
gemeten 550.000-Binair-leugen en de 3.750.000-foldwortel zijn nog
steeds toelaatbare documenten. Sluit de klasse verifier-zijdig met een
AFLEIDINGSWET (maintaineropdracht 2026-09-04, "precisie/soort op
composieten — de WF-091-notitie-4-deferral"): leid per composietknoop
het verwachte resultaattype af uit operand- en declaratietypen — de
emitter-Typering exact spiegelend — en kruis de annotatie ertegen.

Aanpak: MEETFASE EERST. (1) Inventariseer de afleidingsformules: de
frontend-Typering (PLUS/MIN linker-eenheid + max-decimalen +
AND-geheel; MAAL samengesteld/cancel; MINIMUM/MAXIMUM min-decimalen;
TOTAAL-noemerstrip op de temporele route; PERCENTAGE_VAN; Afgerond
versmald; Begrensd) naast de spec (ch. 2.6, §4.6) en naast de
ASP-eff_unit en de Java-EffectieveEenheid — waar die vier bronnen
verschillen is dat een bevinding, geen keuze. (2) Leg over alle vier
zones per composietknoop de afgeleide verwachting naast de annotatie:
nul afwijkingen verwacht (de emitter berekent ze); elke afwijking is
een routebeslissing die gemeld wordt, nooit een stille versoepeling
van de wet. (3) Bepaal de vorm: de voor de hand liggende is de
WF-091-facetmotor uitgebreid met een AFGELEIDE verwachting op
composietposities (zelfde facetvolgorde, zelfde meldstijl; het "no
single declaration to repeat"-gat wordt "derived expectation to
match"); een echt alternatief met eigen nummer alleen als de meting
daar aanleiding toe geeft — dan eerst de genummerde vraag. (4) Bouw
rood-eerst: de 550.000-Binair-vorm en de foldwortel-vorm als eerste
negatiefs, daarna per facet en per knoopsoort arm-exact,
mutant-bewezen. Uitzonderingen expliciet en reden-dragend (de
vierde-klasse-knopen waarvan v0 de resultaateenheid niet afleidt —
GEDEELD_DOOR, TOT_DE_MACHT, PERCENTAGE_VAN-eenheid, absolute
waarde/wortel — volgen de gate-13-lijn: niet afgeleid betekent niet
gekruist, wél genoemd).

Acceptance:
- De vierbronnen-meettabel en de zone-vergelijking (afgeleid vs
  annotatie) gedocumenteerd; afwijkingen gemeld vóór er iets versoepelt.
- De wet rood-eerst; negatiefs per facet×knoopsoort; nul over-afwijzing
  over golden+rood+toka+negatief.
- IR-SPEC (voorstel): de clausule, de indexrij, en eerlijkheidsnotitie 4
  wordt korter en blijft waar.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

Out of scope: executors (gate 13 bewaakt die kant), frontends,
bestaande artefacten, de Let-drager (WF-076 dekt hem al) en
referentieposities (WF-091's bestaande armen).

## Fase 1 — de vierbronnen-meettabel

Bron (a) = de RegelSpraak-frontend (`resolver/Typering.java`,
`ExpressieBouwer.functieType`, en de `type()`-methoden van
`ir/Expr.Afgerond`/`Begrensd`); (b) = de spec (`RUNTIME-SEMANTICS.md`
ch. 2.2/2.3/2.6/2.7/2.10 en `IR-SPEC.md` ch. 4.1/4.3); (c) =
`backends/clingo/lib/translate.ml`, `eff_unit`; (d) =
`backends/drools/.../transpiler/kern/EffectieveEenheid.java`.

**Eerlijk gesteld: (c) en (d) leiden UITSLUITEND eenheden af.** Ze lezen
`Datatype.decimalen`/`geheel`/`teken` niet — (d) zegt dat zelf in zijn
kop, en (c) heeft geen precisie-afleiding, alleen een schaal-encoding.
De facethelft van de tabel komt dus uit (a) en (b) alleen; waar de
kolommen (c)/(d) leeg staan is dat geen stilzwijgende instemming.

| Knoop | (a) frontend | (b) spec | (c) ASP eff_unit | (d) Java |
|---|---|---|---|---|
| PLUS/MIN/VERM. numeriek | maxDec; geheel = l∧r; teken ONBEPAALD; eenheid = LINKS | ch. 2.3 idem; ch. 2.6 "result carries the left unit" | links, **anders rechts** | links, **anders rechts** |
| PLUS/MIN datum ± duur | resultaat = het datumtype links | ch. 2.2 kalenderrekenen | — | — |
| MAAL | dec = som; geheel=false; cancel / opaak product / luide fout | ch. 2.3 "sum"; ch. 2.6 cancel + product + "unitless *literal*" | links, anders rechts (**geen** cancel/product; twee-eenheden faalt bij de vertaling) | cancel / product; onspelbaar → noemerloos product |
| GEDEELD_DOOR(_ABS) | maxDec; geheel=false; €/€→getal, €/jr; eenheidloos/eenheid = luide fout | ch. 2.3 maxDec ("documented under-report"); ch. 2.6 idem | **None** (vierde klasse) | **annotatie** (vierde klasse) |
| TOT_DE_MACHT | maxDec; geheel=false; operanden eenheidloos | ch. 2.3 "maxDec, not whole" | None | annotatie |
| PERCENTAGE_VAN | 0 dec; geheel=false; eenheid van RECHTS | ch. 2.3 idem; ch. 2.6 "right operand's unit" | None | annotatie |
| Afgerond | dec = eigen veld; geheel = (dec=0); teken+eenheid van basis — **maar bij een Percentage-basis valt de eenheid weg** | ch. 2.4 "facing precision"; ch. 2.10 "carries its basis's" | basis | basis |
| Begrensd | basis, geheel | ch. 2.7 / ch. 2.10 | basis | basis |
| SOM / EERSTE / LAATSTE | operand 0, geheel | ch. 2.10 "first operand's" (§4.6 *Eenheid1*) | operand 0 | operand 0 |
| TOTAAL | operand 0, **noemer gestript** (§4.20) | ch. 2.10 zegt alleen "first operand's" — **geen strip** | operand 0, geen strip | operand 0, geen strip |
| MINIMUM / MAXIMUM | ≤1 operand: operand 0; anders minste dec, conjunctie geheel, teken+eenheid van operand 0 | ch. 2.3 "least operand precision"; ch. 4.1 idem | operand 0 | operand 0 |
| AANTAL | heel, 0, NIET_NEGATIEF, eenheidloos | ch. 2.3 idem | None | annotatie |
| WORTEL | 0 dec, NIET_NEGATIEF, eenheidloos | ch. 2.3 "placeholder claim" | None (vierde klasse) | annotatie |
| ABSOLUTE_WAARDE | operand 0 (doorgeef) | ch. 2.2/2.3 "passthrough" | None (**vierde klasse**) | annotatie (**vierde klasse**) |
| extractors JAAR..MS_UIT | heel, 0, NIET_NEGATIEF | (geen rij in ch. 2.3) | None (vierde klasse) | annotatie |
| TIJDSEVENREDIG_DEEL | operand 0 (doorgeef) | ch. 2.8 zwijgt over het type | None | annotatie |
| TIJDSDUUR / ABS_TIJDSDUUR | eigen route (operatorparameter) | ch. 2.10: de annotatie **IS** de bron | annotatie | annotatie |
| CONCATENATIE / AANTAL_DAGEN_IN | Onbekend | — | None | annotatie |

**Vijf bevindingen — gemeld, niet gekozen.**

1. **Het eenzijdige eenheidspaar bij +/−.** (a) en (b) geven het
   resultaat de LINKER eenheid, ook als die er geen heeft; (c) en (d)
   zeggen "links, anders rechts". De frontend kan de vorm niet
   uitgeven (zijn `eenhedenCompatibel` maakt er een fout van), maar
   WF-035's literal-adoptieclausule laat hem toe. **De wet claimt daar
   niets.**
2. **De TOTAAL-noemerstrip.** (a) en §4.20 strippen de `/tijdseenheid`;
   (c) en (d) geven de eenheid van operand 0 heel door. Op dit corpus
   onobserveerbaar (elke TOTAAL-annotatie volgt de emitter), maar het is
   een echte splitsing tussen de spec en beide executor-afleidingen. De
   verifier volgt (a)+(b); de executorkant is een consumentenvraag.
3. **De absolute waarde.** ch. 2.2/2.3 noemen hem een doorgeef; ch. 2.10
   zet hem in de VIERDE klasse ("v0 derives nothing"). Dat is een
   spec-interne spanning tussen twee hoofdstukken. Deze kaart volgt
   ch. 2.10 (en gate 13), zodat verifier en harnas dezelfde
   uitzonderingsset noemen — nul voorkomens in vier zones.
4. **GEDEELD_DOOR en PERCENTAGE_VAN leiden bij (a) en (b) wél een
   eenheid af** (€/€ → getal, €/jr; "X% van Y draagt Y's eenheid"),
   terwijl ch. 2.10's vierde klasse zegt dat v0 daar niets afleidt. De
   vierde klasse gaat over de CONSUMENTkant; de regels bestaan wel. Ook
   hier volgt de kaart de gate-13-lijn en laat de posities ongekruist,
   met naam en reden.
5. **`Afgerond` over een Percentage-basis** laat bij (a) de eenheid
   vallen, terwijl (b), (c) en (d) hem houden. Nul voorkomens in vier
   zones; de verifier volgt de meerderheid en zegt dat het onbeproefd is.

## Fase 2 — de zonevergelijking

Alle vier zones (golden 152, rood 22, negatief 244, toka 30), per
composietknoop de afgeleide verwachting naast de geserialiseerde
annotatie, facet voor facet in de WF-091-volgorde.

Eerste ronde, met de VOLLE formule van bron (a) op elk facet: 62
afwijkingen op golden+rood. Beslissend was de herkomstsplitsing:

| herkomst | goldens | afwijkingen |
|---|---|---|
| `bron.rgs` (rgs-emitter) | 28 | **0** |
| `bron.catala_en` (catala-plugin) | 12 | 9 |
| handgeschreven | 112 | 44 |

**Geen enkele afwijking zit in een rgs-geëmitteerde golden** — de
(a)-spiegel is exact tegenover de emitter die hij spiegelt. Elke
afwijking zit in een handgeschreven of Catala-geëmitteerd document, en
elke afwijking valt in precies twee klassen: het TEKEN (12 documenten
versmallen het naar waarheid) en de PRECISIE van de multiplicatieve
familie (MAAL 9+9, GEDEELD_DOOR 5+18, TOT_DE_MACHT 2+5,
PERCENTAGE_VAN 4). Plus twee losse: `synth2-prorata-breed` annoteert
zijn tijdsevenredig deel met het DOEL-type (2 knopen).

Tweede ronde, met de voorgenomen kruisingstabel (teken en
maal-precisie uitgesloten, vierde klasse en prorata ongekruist):
**nul afwijkingen op golden, rood en toka.** De drie enige afwijkingen
zitten in de negatief-zone en zijn de drie BESTAANDE foldbron-getuigen
(`wf091-foldbron-eenheid`, `wf034-foldbron-zonder-attribuut`,
`wf034-foldbron-zonder-objecttype`), die hun eigen leugen nu ook op de
SOM-wortel gemeld krijgen.

Geen versperring: de wet is gebouwd op de formules waarover alle
bronnen het eens zijn, en elke uitsluiting draagt haar meting.

## Fase 3 — de wet, rood-eerst

`475a937` zet dertien negatiefs neer vóór één regel motorcode; alle
dertien zijn dan verifier-SCHOON (het gat). `f79e5da` voegt
`composite_expectation` + `composite_identity` toe en een `?skip` op
`use_site_identity`; alle dertien melden dan precies één regel,
arm- en facet-exact.

Nul over-afwijzing, direct gemeten: het verifierrapport over alle 447
documenten van golden+rood+negatief plus de 30 pariteitsdocumenten is
byte-identiek vóór en na, op drie toegevoegde regels na (de drie
bestaande foldbron-getuigen hierboven).

### Mutanttabel (schoon / naar `residual` / rood, van dertien)

| mutant | schoon | residual | rood | golden+rood vuil |
|---|---|---|---|---|
| 0. ongemuteerd | 0 | 0 | 13 | 0 |
| facet `unit` uit | 3 | 4 | 6 | 0 |
| facet `decimals` uit | 2 | 0 | 11 | 0 |
| facet `wholeness` uit | 1 | 0 | 12 | 0 |
| facet `soort` uit | 1 | 0 | 12 | 0 |
| arm additief uit | 4 | 0 | 9 | 0 |
| arm MAAL uit | 1 | 0 | 12 | 0 |
| arm afronding uit | 2 | 0 | 11 | 0 |
| arm begrenzing uit | 1 | 0 | 12 | 0 |
| arm AANTAL uit | 1 | 0 | 12 | 0 |
| arm aggregatie uit | 4 | 0 | 9 | 0 |
| TOTAAL-noemerstrip uit | 1 | 0 | 12 | 0 |
| MIN/MAX: operand 0 i.p.v. minste precisie | 1 | 0 | 12 | 0 |
| hele afleidingskruising uit | **13** | 0 | 0 | 0 |
| *over-afwijzing:* teken WEL gekruist (additief) | 0 | 0 | 13 | **5** |
| *over-afwijzing:* MAAL-precisie WEL gekruist | 0 | 0 | 13 | **20** |
| *over-afwijzing:* alle skiplijsten leeg | — | — | — | **27** |

De `unit`-mutant splitst precies langs de "claimt elk facet"-kolom van
de ch. 4.1-tabel: waar de formule het hele type claimt degradeert het
geval naar `residual structure mismatch` (afronding, begrenzing, SOM,
TOTAAL), waar zij een facet niet claimt wordt het SCHOON (additief,
MAAL, MAXIMUM). Geen enkele mutant raakt een bestaande referentie-arm:
de zes `wf091-*ref*`-gevallen en `wf078-binderref-type-lie` blijven in
elke run rood.

### Dekking, eerlijk

228 uitgevoerde composietvergelijkingen over **102 van de 174**
golden+rood-documenten (gemeten met een tijdelijke probe IN
`composite_identity`, niet met het meetscript): additief 114, maal 38,
afronding 28, aantal 18, som 12, totaal 7, begrenzing 3, eerste 3,
laatste 3, minimum 1, maximum 1. Daarvan zijn er **14 eenheid-dragend**,
over 10 documenten. Dus:

- het eenheidsfacet heeft een golden-getuige op **drie** armen
  (additief 8 documenten, maal 3, som 1) en op de **acht** andere
  alleen een negatief — die pint een weigering, nooit een niet-weigering;
- de MAAL-**cancel** heeft geen enkele corpusgetuige: geen maal-knoop in
  vier zones draagt twee eenheid-dragende operanden;
- `Afgerond` over een Percentage-basis: nul voorkomens, dus die tak is
  gesteld en onbeproefd;
- de negatief-zone voert 63 vergelijkingen uit BUITEN de dertien eigen
  getuigen (die 13 tellen niet mee als dracht), de pariteitszone 21.

## Fase 4 — spec (voorstel)

- **ch. 4.1** — nieuwe clausule "Derived-type identity on composite
  positions": de wet, de kruisingstabel per positie met de geclaimde
  facetten, de twee geprijsde uitsluitingen, de vierde klasse
  ongekruist-en-benoemd (identiek aan gate 13's uitzonderingsset), en
  een "Measured, and open"-alinea (prorata, het eenzijdige
  eenheidspaar, het opake product, de Percentage-afronding). De
  voorgaande alinea over de zesde use site zegt niet langer "no further
  facet does" maar wijst door naar de afleiding.
- **indexrij WF-091** — de DERIVED ARMS met de elf posities, het tweede
  ingangspunt in de gatekolom, de dertien negatiefs met hun
  mutantbewijs in de negatiefkolom, en de 929/46-telling die nu
  expliciet zegt dat zij de REFERENTIEhelft telt.
- **eerlijkheidsnotitie 4** is geen deferral meer maar een
  DEKKINGSnotitie: wat de afgeleide helft doet, hoe smal haar
  eenheidsgetuigen zijn, en welke drie formules erbuiten blijven met
  hun prijs.

**Niet aangeraakt, en het moet wél:** `RUNTIME-SEMANTICS.md` ch. 2.10
sluit af met "Soort and precision at a *derived* type are crossed by
nothing … closing the other two needs the derivation law for result
types that note calls for". Die zin is sinds deze kaart onwaar. Het
bestand staat op de verbodslijst van deze kaart, dus het is hier
gemeld in plaats van gewijzigd — één alinea voor de maintainer of een
vervolgkaart.

## Poorten, alle dertien

     1 check-self-contained: PASS
     2 check-ir-schema: PASS (schema valid; 174 file(s) validate and round-trip)
     3 check-verifier: PASS (174 golden clean; 256 negatief rejected for their declared rule)
     4 check-case-schema: PASS (schema valid; 425 case file(s) validate)
     5 check-oracle: PASS (403 cases replayed exactly; 2 no-case goldens)
     6 check-clingo: PASS (403 cases replay exactly through clingo)
     7 check-noop: PASS (367 cases refute a do-nothing executor; 36 pinned survivors)
     8 check-rood: PASS (22 red cases fault observably in ref en clingo; Drools-leg 5 fault-pinned, 17 not-carried)
     9 check-catala: PASS (frontend suite 201 checks; 12 catala golden(s) byte-identiek)
    10 check-regelspraak: PASS (JUnit 159 tests; 28 rgs golden(s) byte-identiek)
    11 check-toka-parity: PASS (ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED, nul stille divergenties)
    12 check-drools: PASS (JUnit 198 tests; value-divergence 1, no-case 2)
    13 check-reftype-derivation: PASS (52 unit-annotaties gedraaid over 11 goldens; 39 executor-replays; 0 gepinde overlevenden)

Eén teller beweegt: gate 3 gaat van 243 naar 256 negatiefs. Alle overige
tellingen zijn ongewijzigd; geen enkele pin is geraakt.

## Result (2026-09-04)

De klasse die WF-091's eerlijkheidsnotitie 4 open liet is verifier-zijdig
gesloten: op elf composietposities wordt de annotatie gekruist tegen de
verwachting die haar operanden AFLEIDEN — zelfde motor, zelfde
facetvolgorde, zelfde regelnummer, want het is dezelfde wet met een
formule op de plek van de declaratie. Dertien negatiefs, rood-eerst en
mutant-exact, één per (positie, facet); over-afwijzing nul, direct
gemeten als een byte-identiek verifierrapport over 477 documenten in
vier zones.

De meetfase leverde vijf bevindingen op waar de vier bronnen uiteenlopen
(het eenzijdige eenheidspaar, de TOTAAL-noemerstrip, de absolute waarde
tussen ch. 2.2 en ch. 2.10, de eenheidsafleiding bij deling/percentage
tegenover de vierde klasse, de Percentage-afronding). Geen daarvan is
stil beslecht: waar de bronnen het eens zijn kruist de wet, waar zij
uiteenlopen claimt zij niets en staat de splitsing in de spec.

Twee uitsluitingen zijn gemeten en geprijsd in plaats van gekozen: het
teken (5 tot 27 golden+rood-documenten) en de precisie van maal (20) —
dit corpus draagt drie producenten met drie precisiepolitieken. De
vierde klasse van ch. 2.10 blijft ongekruist met een uitzonderingsset
die letterlijk gelijk is aan die van gate 13, zodat verifier en
executor-harnas dezelfde posities benoemen.

Wat open blijft staat er met zoveel woorden: acht van de elf armen
hebben voor het eenheidsfacet alleen een negatief; de MAAL-cancel heeft
geen enkele corpusgetuige; het tijdsevenredig deel en het eenzijdige
eenheidspaar zijn eenbron-formules; en ch. 2.10's slotalinea in
`RUNTIME-SEMANTICS.md` is nu achterhaald en moet na — dat bestand stond
op de verbodslijst van deze kaart. Alle dertien poorten groen. Voorstel
op beschermde paden (`spec/verifier/`, `spec/IR-SPEC.md`): de tak blijft
staan tot de maintainer hem fast-forward merget.
