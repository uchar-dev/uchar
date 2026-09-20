# drools-enumlabel-lezer: de consumer eist delimiters die de wire nooit draagt

Uit kaart `20260919-v8-enum-emitter` (2026-09-20, lens + mini-fixronde):
`Literalen.tekstOfEnumLabel` leest een enumeratiewaarde door `enumLabel`,
dat delimiters verlangt, terwijl de wire per WF-039 het kale label
draagt. Vijf plekken lezen door die lezer heen, en het zijn niet alleen
vergelijkingen: `ExecModelTranspiler` 3935 (eigen attribuut), 3950
(genavigeerd) en 5445 (assertie), `DrlTranspiler` 5126 (de
literal-verlaging van die tak, die ook de waardepositie bedient), en de
WAARDE-arm `ExecModelTranspiler:4207`, die een enumliteral als
geschreven waarde omzet. (Het `new/`-kaartje noteerde 3913/3928/5422,
5114 en 4185; die stonden zo'n 22 regels eerder in de boom van vóór dit
kaartje en zijn hier bijgetrokken naar de basis van deze branch,
c4f12743, zodat het dossier als vindplaats bruikbaar blijft.) Beide Drools-takken weigeren daardoor zowel elke enumgelijkheid
als elke enumtoekenning — apart gemeten op een voorwaardeloze variant:
exec-model `waarde buiten scope`, DRL-tekst `TODO literal: wrakgoed`. De
twee geraakte goldens zijn `jutterswet-keurstaat` en
`jutterswet-vondstklasse`, beide per naam gepind in beide kanaallijsten
van gate 12.

De reparatie is klein en zit op ÉÉN plek: de lezer geeft het label van
een `Enumeratie`-getypeerde literal verbatim terug, precies zoals de
Tekst-arm doet, waarmee alle vijf plekken gratis meekomen — geen van de
call sites hoeft te wijzigen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de twee EXCLUDEs op de twee goldens vastgelegd, dan de
  lezer-fix; dual-branchgetuige met mutatieproef (delimiters-eis terug →
  getuige faalt bij naam in beide takken).
- (b) Kanaalgevolgen: beide goldens worden spelers — de
  gelijkheidspins elk +2 op de dan geldende stand (bij het schrijven van
  dit kaartje: `EXPECTED_PLAYED` 165 → 167, `EXPECTED_DRL_PLAYED` 142 →
  144) en twee regels uit élke kanaallijst; SERIEEL plannen tegen andere
  kaarten op `scripts/check-drools.sh`.
- (c) Corpussweep beide takken; alle veertien gates groen. Het levert
  twee goldens erbij aan het volledige vierkant.

May touch: `backends/drools/**`, `scripts/check-drools.sh` (pins +
kanaallijsten, **voorstel**), kaartverplaatsing. NB: serieel met andere
DrlTranspiler/check-drools-kaarten.

## Rood-eerst, gemeten vóór elke wijziging (2026-09-20, HEAD c4f12743)

De twee goldens door de replay-API, beide takken, met de jar van deze
HEAD:

    jutterswet-keurstaat    exec: EXCLUDE transpiler SKIP: regel:
                                  veilbaarheid naar staat — regel
                                  (voorwaarde buiten Phase-B scope)
                            drl : EXCLUDE drl-transpiler MARKER: regel:
                                  veilbaarheid naar staat — TODO
                                  voorwaarde: haar staat gelijk is aan 'gaaf'
    jutterswet-vondstklasse exec: EXCLUDE transpiler SKIP: regel:
                                  bestemming van drijfgoed — regel
                                  (voorwaarde buiten Phase-B scope)
                            drl : EXCLUDE drl-transpiler MARKER: regel:
                                  bestemming van drijfgoed — TODO
                                  voorwaarde: zijn klasse gelijk is aan
                                  'drijfgoed'

De WAARDE-arm apart, want de conditie-arm ketst als eerste af: hetzelfde
document met de voorwaarde van R1 weggenomen geeft exec `regel (waarde
buiten scope)` en DRL-tekst `TODO literal: wrakgoed`. Beide helften van
de kaartzin kloppen dus letterlijk.

Actuele stand van de pins op deze HEAD: `EXPECTED_PLAYED` 169,
`EXPECTED_DRL_PLAYED` 146 (het kaartje schreef 165/142; er zijn
sindsdien kaarten geland).

## Toolchain-consequentie (herpin 2026-09-20, regelspraak-java 1e53206)

De pin-bump naar 3e97f181 legde dezelfde lezer in de toolchain bloot:
de twee beslistabel-enum-fixtures van `ExecModelDifferentialTest`
(OR-lijst én waarde-kant) loweren daar niet meer — drie
reden-dragende allowlist-entries en de diffvloer 6 → 4, elk met een
terugzet-verwijzing naar déze kaart. De herpin ná deze kaart draait
die drie entries en de vloer terug; een achtergebleven entry faalt de
stale-check daar vanzelf.

Result: geland. De reparatie zat inderdaad op ÉÉN plek —
`Literalen.tekstOfEnumLabel` geeft het label van een
`Enumeratie`-getypeerde literal verbatim terug, zoals de Tekst-arm doet
— en alle vijf de leesplekken kwamen gratis mee: geen enkele call site
is aangeraakt. Beide goldens spelen sindsdien in BEIDE takken. Eén
afwijking van de May touch, en zij was onvermijdelijk: het gerepareerde
bestand ligt in `frontends/regelspraak/.../kern/`, dat via de
vendored-slice-route in de Drools-backend meecompileert. De frontend
zelf leest er niet doorheen (machinaal gecontroleerd: alleen de twee
transpilers en de tests roepen hem aan), dus de EMISSIE beweegt geen
byte — gate 10 her-emitteert alle 64 `bron.rgs`-goldens byte-identiek.

ROOD-EERST, feitelijk gemeten vóór elke wijziging en in commit 8ce52db1
vastgelegd. Beide goldens, beide takken, door de replay-API:
keurstaat exec `EXCLUDE transpiler SKIP: regel: veilbaarheid naar staat
— regel (voorwaarde buiten Phase-B scope)`, drl `EXCLUDE drl-transpiler
MARKER: regel: veilbaarheid naar staat — TODO voorwaarde: haar staat
gelijk is aan 'gaaf'`; vondstklasse exec `… bestemming van drijfgoed —
regel (voorwaarde buiten Phase-B scope)`, drl `… TODO voorwaarde: zijn
klasse gelijk is aan 'drijfgoed'`. De WAARDE-arm apart, omdat de
conditie-arm als eerste afketst en hem anders verbergt: hetzelfde
document met de voorwaarde van R1 weggenomen geeft exec `regel (waarde
buiten scope)` en DRL-tekst `TODO literal: wrakgoed`.

HET TWEEDE GAT, dat de weigering verborgen hield — niet gezocht, wel
gemeten. Met de lezer gerepareerd lowert keurstaat, vuurt en antwoordt
`FAIL case2-beschadigd.json: attribuut oordeel: verwacht beschadigd,
werkelijk Beschadigd`, in beide takken. Die golden spelt haar
enuminvoer MET OPZET anders dan haar declaratie ("de norm-insensitieve
spelling", commit 5161bf74 die haar aanmaakte): een enumlabel is een
GEDECLAREERDE NAAM, die ref tot op `Checks.norm` vergelijkt
(`arith.ml`: `Enum a, Enum b -> norm a = norm b`) en die de ASP-backend
genormaliseerd opslaat. De bean houdt de spelling van de casus — ref
houdt die ook — maar de leesterug van de casusvergelijker vergeleek
bytes en meldde een divergentie die geen van beide andere uitvoerders
heeft. `CaseRunner.gelijkeWaarde` vergelijkt een enumverwachting nu op
naamidentiteit (via `Namen.norm`, dat hier al ref's `Checks.norm` is) en
laat elke andere waardevorm bij `gelijk`; de VERWACHTINGSKNOOP beslist,
niet het Javatype, zodat een Tekst zijn exacte §13.3.3.4-gelijkheid
houdt.

MUTATIEPROEVEN, alle drie uitgevoerd en in dezelfde keten teruggedraaid,
met de build-rc erbij. (1) Delimiters-eis terug: 4 van de 5 getuigenrijen
rood, de exec-tak bij naam in de melding (`hoort door de exec-model-tak
gespeeld te worden: transpiler SKIP … ==> expected: <PLAYED> but was:
<EXCLUDE>`), en met dezelfde mutatie gepakt in een jar geeft de
replay-API de DRL-tak er even luid naast (`EXCLUDE drl-transpiler
MARKER: … TODO voorwaarde`) — de getuige faalt dus in beide takken bij
naam. (2) De naamidentiteit terug naar bytegelijkheid: 2 rijen rood,
waaronder `beide_takken_spelen_de_twee_enumgoldens` met
`expected: <PLAYED> but was: <FAIL>`. (3) De normalisatie laten lekken
naar Tekst: `een_tekstwaarde_houdt_haar_exacte_gelijkheid` rood. Na elke
proef teruggedraaid en groen hermeten (5/5).

GETUIGEN. Vijf rijen in `EnumLabelWireWitnessTest`
(`backends/drools/src/test/.../cases/`), alle dual-branch: beide goldens
door beide takken met elke casus gespeeld; een gemuteerde GESCHREVEN
enumwaarde per golden die beide takken waarneembaar verwerpen; de
WAARDE-arm los, op het voorwaardeloze document waarop zijn weigering
gemeten is; de naamidentiteit van de leesterug van BEIDE kanten (de
derde spelling HAALT, een andere waarde van hetzelfde domein FAALT); en
de tekstzuster, die pint dat de normalisatie niet naar `Tekst` lekt. In
de frontend één unitgetuige erbij
(`LiteralenTest.tekstOfEnumLabel_leest_een_enumeratiegetypeerde_literal_verbatim`),
die ook vastlegt dat verbatim verbatim betekent: een gedelimiteerde
waarde op een Enumeratie-type wordt NIET alsnog gestript, want WF-039
keurt haar af en een lezer die haar repareert verzint een label dat de
verifier nooit gezien heeft. Geen bestaande check gewijzigd; de suites
groeien 509 → 514 (Drools) en 337 → 338 (frontend — één @Test erbij).

DE SWEEP (kaartpunt c). Alle 222 goldens, beide takken, met de jar vóór
en de jar na, regel voor regel gedift: precies VIER rapportregels
bewegen — de twee goldens maal de twee takken — en geen andere. Er is
dus geen andere vorm die deze lezer ontsluit; de gelijkheidspins
kloppen exact op de meting (exec 169 → 171, DRL-tekst 146 → 148).

CHECK-DROOLS, exact wat ik wijzigde. Twee namen uit `EXPECTED_CHANNEL`,
twee uit `DRL_CHANNEL`, `EXPECTED_PLAYED` 169 → 171 en
`EXPECTED_DRL_PLAYED` 146 → 148, elk met een redenblok. De
lijstlengtevloeren zijn geen pin maar `${#LIJST[@]}` en bewegen mee (51
en 74 in het rapport, was 53 en 76). Eén telling in de PROZA gecorrigeerd
omdat zij al scheef stond: de klassekop van de DrlTranspiler-markeringen
zei "12" over een lijst van 13 — de v8-toevoeging was niet meegeteld —
en staat nu op 11, met acht gedeelde en drie tak-eigen, herteld tegen de
exec-sweep. In de mini-fixronde één woord in datzelfde redenblok
gecorrigeerd: de DRL-kant heette daar "one condition arm", maar
`DrlTranspiler:5126` zit in `literal()` — de algemene literal-verlaging
van die tak, die ook de waardepositie bedient (vandaar de gemeten
weigering `TODO literal: wrakgoed`), dus er staat nu "the literal
lowering of the DRL-text branch". Beide wijzigingen zijn TEKST in een
beschermd script; geen enkele pin, lijst of controle beweegt erdoor.

NIET GEBOUWD, en gemeten in plaats van verzwegen. De RULE-kant van
diezelfde naamidentiteit: beide Drools-takken vergelijken enumlabels in
CONDITIES byte-exact (`rhs.equals(...)`, en in de DRL-tekst een mvel
`==`), waar ref en clingo tot op de normalisatie vergelijken. Geen
corpusdocument raakt dat — machinaal over alle 222 goldens: keurstaats
invoer is de ENIGE enumwaarde in de zone die niet byte-identiek aan haar
declaratie is, en de literal waartegen zij het opneemt verschilt van
haar onder beide lezingen — dus er is geen stil fout antwoord in het
corpus. Het repareren raakt vier arms in de exec-tak, de geëmitteerde
mvel in de DRL-tak en de uniciteitssleutels, en het is bovendien een
divergentie van de PRODUCTIEsemantiek (de toolchain vergelijkt net zo
byte-exact), dus het is een besluit en geen lensfix. KAARTVOORSTEL —
`drools-enumnaam-identiteit`: de twee takken lezen een enumlabel als een
gedeclareerde naam in plaats van als een string, met rood-eerst op een
handgebouwd document (twee spellingen van één label aan weerszijden van
een gelijkheid) en, als de maintainer de v1-lezing bevestigt, een
corpusgetuige die hem pint in alle drie de uitvoerders.

TOOLCHAIN-HERPIN (het kaartpunt uit de opdracht). `Literalen.java` is
een frontend-bestand, dus `regelspraak-java` herpint na de landing op de
nieuwe `SPEC_REV`. VERWACHTING, met de reden erbij, want ik kan het niet
zelf meten (andere repo): de drie reden-dragende allowlist-entries van
`ExecModelDifferentialTest` (Beslistabel: Indeling 1, Indeling 2,
Naamzetting 1) en de diffvloer 4 → terug naar 6 kunnen worden
teruggedraaid. De grond is dezelfde lezer: die fixtures loweren daar
sinds de pin-bump naar 3e97f181 niet meer omdat de toolchain-frontend
sindsdien het KALE label emitteert en de gedeelde lezer er delimiters op
eiste. Met deze arm geeft hij het label verbatim terug, dus de
beslistabel-enumcellen loweren weer — zowel de OR-lijst (conditie) als
de waarde-kant. De ene voorwaarde waaronder dat NIET geldt, en zij is
controleerbaar bij de herpin: de literal moet daar het
`Enumeratie`-datatype dragen. Waar de v8-promotie geen domein vindt
blijft een `Onopgelost`-marker staan en weigert de lezer terecht nog
steeds; beide beslistabelposities zijn juist de twee promotieplaatsen
(schrijfdoel en andere vergelijkingszijde), dus dat zou een verrassing
zijn en geen verwachting. Een achtergebleven entry faalt daar vanzelf op
de stale-check.

REGISTERVOORSTEL — `spec/CONSTRUCTS.md`, rij 27, ter VERVANGING van de
slotzin van "Enumeratiewaarde geëmitteerd (2026-09-20, V8 (a))", die nu
luidt: "De Drools-backend draagt het construct NIET — zijn gedeelde
lezer `Literalen.tekstOfEnumLabel` verlangt de gedelimiteerde vorm die
de wire nooit draagt — dus de golden is per naam gepind in beide
kanaallijsten van gate 12; dat is de consumentenkant en heeft een eigen
kaart (`backlog/new/20260920-drools-enumlabel-lezer.md`)."

> De consumentenkant is gedicht (2026-09-20, kaart
> `20260920-drools-enumlabel-lezer`): de gedeelde lezer geeft het label
> van een `Enumeratie`-getypeerde literal verbatim terug, precies zoals
> de Tekst-arm, waarmee alle vijf de leesplekken van de twee takken
> meekomen zonder dat één call site wijzigt. `jutterswet-vondstklasse`
> en `jutterswet-keurstaat` verlaten daarmee beide kanaallijsten van
> gate 12 en dragen het construct in het VOLLEDIGE vierkant (ref ≡
> clingo ≡ Drools, beide takken; `EXPECTED_PLAYED` 169 → 171,
> `EXPECTED_DRL_PLAYED` 146 → 148, gemeten met een sweep over alle 222
> goldens waarin precies vier rapportregels bewegen). De reparatie legde
> één gat bloot dat de weigering verborg en dat in dezelfde kaart dicht
> is: een enumlabel is een GEDECLAREERDE NAAM, tot op `Checks.norm`
> vergeleken door ref en genormaliseerd opgeslagen door de ASP-backend,
> terwijl de leesterug van de Drools-casusvergelijker bytes vergeleek —
> waardoor de norm-insensitieve spelling die `jutterswet-keurstaat` met
> opzet draagt een divergentie meldde die geen andere uitvoerder heeft.
> Wat NIET meekwam, en gemeten is: in CONDITIES vergelijken beide
> Drools-takken enumlabels nog byte-exact; geen corpusdocument raakt dat
> verschil (één enumwaarde in de hele zone wijkt van haar declaratie af,
> en haar vergelijkingspartner verschilt onder beide lezingen), en het
> optrekken is een divergentie van de productiesemantiek en dus een
> eigen besluit.

ALLE VEERTIEN GATES, los uitgevoerd na `eval $(opam env)`, elk rc 0:

    1  self-contained          PASS
    2  ir-schema               PASS  schema valide; 258 bestanden (vloer 192)
    3  verifier                PASS  258 golden schoon; 420 negatief (vloer 350)
    4  case-schema             PASS  600 casusbestanden
    5  oracle                  PASS  726 worked examples (vloer 489); 564 casussen (vloer 427)
    6  clingo                  PASS  457 checks (vloer 83); 564 casussen
    7  noop                    PASS  528 casussen weerleggen; 36 survivors
    8  rood                    PASS  36 rode casussen (vloer 26); 72 fragmenten; 37 bodemzinnen
    9  catala                  PASS  201 checks (vloer 201); 12 goldens (vloer 12)
    10 regelspraak             PASS  338 tests (vloer 173); 64 rgs-goldens (vloer 38)
    11 toka-parity             PASS  45 fragmenten; 29 casussen; ref/clingo/Drools 29-29-29
    12 drools                  PASS  played 171; kanaalvloer 51; DRL played 148, vloer 74; JUnit 514
    13 reftype-derivation      PASS  131 twists over 27 goldens (vloer 74); 128 replays, 25 DRL
    14 flogic                  PASS  130 checks (vloer 81); 2 goldens (vloer 1)

BUITEN SCOPE GEHOUDEN, met reden. De rule-kant van de naamidentiteit
(kaartvoorstel hierboven). De `enumLabel`-terugval voor NIET-enumeratie
datatypen in dezelfde lezerregel: geen emitter schrijft die vorm en geen
corpusdocument draagt haar, dus hem versmallen zou een tweede wijziging
in één regel zijn zonder eigen getuige — de bestaande unitcheck
`tekstOfEnumLabel_stript_alleen_de_enumdelimiters` pint hem ongewijzigd.
De wire-vorm zelf (WF-039 en WF-034 zijn niet aangeraakt), B22/V9, en de
registerbestanden `spec/CONSTRUCTS.md` en `spec/PLAN.md`, waarvoor de
voorgestelde tekst hierboven staat.
