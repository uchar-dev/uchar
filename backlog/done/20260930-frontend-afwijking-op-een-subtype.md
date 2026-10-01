# frontend-afwijking-op-een-subtype: een uitzondering of afwijking op een subtype onder een basisregel op het supertype weigert (WF-050)

*Grens gevonden door een regelsetbouwer, 2026-09-30 (meting P1 in zijn
clusterdocument), gemeten door de hoofdsessie op uchar `b0aab696` via de
distributie (`bin/regelspraak check|scenario`); reproductie op
Jutterswet-materiaal in `/tmp/v2/probe/erflijn-afwijking/` (kop.rgs,
basis-super.rgs, r1–r4, scenario.rgs, scenario-sub.rgs; alleen lezen). Luid
(`check` exit 1 met de WF-050-zin), geen fout antwoord. Op verzoek van de
eigenaar geschreven (2026-09-30); geen bouwdatum.*

## Aanleiding

De dialectvormen `Uitzondering op` en `Afwijking van` (DIVERGENCES §4) eisen
dat de uitzondering hetzelfde onderwerp schrijft als haar basisregel: de
frontend weigert een afwijking op een subtype onder een basisregel op het
supertype met "schrijft een ander doel (Strandjutter.meldtermijn
(attribuut)) dan de basisregel (Jutter.meldtermijn (attribuut)) — een
afwijking moet hetzelfde onderwerp en kenmerk/attribuut betreffen (§4,
WF-050)". De semantiek voor precies deze vorm bestaat al: RUNTIME-SEMANTICS
ch. 7.10 geeft elk concreet type één effectieve stapel per doel, waarin de
eigen uitzonderingen van het subtype bovenaan staan, daaronder de geërfde
uitzonderingen, daaronder de basis — "the subtype's own base if it has one,
which replaces the inherited base, otherwise the inherited base" — en het
anker `corpus/golden/anchor/jutterswet-strandrechtketen` pint die volgorde
door de drie uitvoerders. Alleen de frontend laat de auteur er niet bij: wie
"voor elke Jutter geldt X, behalve voor een Strandjutter Y" wil schrijven,
moet nu óf de hele boom naar het subtype verhuizen (R3: de regel geldt dan
niet meer voor een gewone Jutter) óf de basisregel op het subtype herhalen
(R4: twee bases, de eigen vervangt de geërfde; speelt, maar de norm staat er
tweemaal). Beide vormen zeggen niet meer wat de norm zegt, en een regelset
met een CGM-hiërarchie (het attribuut op het supertype, de leden die de
afwijking leest op het subtype) loopt hier bij elke uitzondering tegenaan.

Dit is geen bug: WF-050 bewaakt dat één doel één groep heeft. Het opheffen
is een lowering in de frontend naar de vorm die ch. 7.10 al draagt.

## Minimale reproductie

`b0aab696`, distributie `bin/regelspraak check|scenario`.

    Eenheidsysteem Tijd
        de dag dg
        de week wk = 7 dg
        de maand mnd
        het jaar jr = 12 mnd

    Objecttype de Jutter (mv: Jutters) (bezield)
        de vinddag Datum in dagen;
        de meldtermijn Datum in dagen;
    Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
        het vergunningseinde Datum in dagen;

    Regel meldtermijn
        geldig altijd
            De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus 6 mnd
            indien zijn vinddag gevuld is.

    Afwijking van meldtermijn: meldtermijn tot het vergunningseinde
        geldig altijd
            De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde
            indien zijn vergunningseinde gevuld is.

    Scenario: Jutter J (vinddag 10-03-2026), Strandjutter S (vinddag 10-03-2026, vergunningseinde
    01-06-2026), Strandjutter T (vinddag 10-03-2026); verwacht: meldtermijn J 10-09-2026, S 01-06-2026,
    T 10-09-2026.

    R1  r1-afwijking-subtype.rgs, zoals hierboven.
        nu: check exit 1, "ERROR (line 18): Afwijking van 'meldtermijn': afwijking 'meldtermijn tot het
        vergunningseinde' schrijft een ander doel (Strandjutter.meldtermijn (attribuut)) dan de basisregel
        (Jutter.meldtermijn (attribuut)) — een afwijking moet hetzelfde onderwerp en kenmerk/attribuut
        betreffen (§4, WF-050)".
    R2  r2-uitzondering-subtype.rgs: dezelfde afwijking als `Uitzondering op meldtermijn`.
        nu: check exit 1 met dezelfde zin ("een uitzondering moet hetzelfde onderwerp …").
    R3  r3-boom-op-subtype.rgs: de hele boom op Strandjutter (de omweg).
        nu: check exit 0, scenario-sub.rgs PASS; een gewone Jutter krijgt geen meldtermijn meer.
    R4  r4-eigen-basis-subtype.rgs: de basisregel herhaald op Strandjutter ("meldtermijn van de
        strandjutter") met de afwijking daaronder, naast de basisregel op Jutter.
        nu: check exit 0, scenario.rgs PASS (J 10-09-2026, S 01-06-2026, T 10-09-2026): ch. 7.10 laat de
        eigen basis de geërfde vervangen; de norm staat tweemaal.

## Acceptatie

- (a) Meet de vormruimte: `Uitzondering op` en `Afwijking van` op een direct
  subtype en op een kleinkind; twee subtypen met elk een eigen afwijking
  onder één basis; een afwijking op een subtype onder een uitzondering van
  het supertype (de geneste vorm van §4); de eigen basis op het subtype (R4)
  als vorm die moet blijven spelen; de omgekeerde richting (basis op het
  subtype, afwijking op het supertype) als benoemde weigering; een kenmerk
  als doel; een supertype zonder eigen instanties.
- (b) De frontend lowert een uitzondering of afwijking waarvan het
  onderwerp een subtype is van het onderwerp van de basisregel en waarvan
  het doel het geërfde attribuut of kenmerk is, als eigen uitzondering van
  het subtype zonder eigen basis (de groep-default van ch. 7.9), zodat de
  blokvolgorde van ch. 7.10 geldt: de eigen uitzondering boven de geërfde,
  de geërfde basis eronder; het rapport zegt bij de groep dat de basis
  geërfd is. Kan de draadvorm van IR-SPEC ch. 2.2 en 4 dat niet dragen, dan
  eerst een spec-voorstel. WF-050 houdt zijn zin voor elk ander doel (een
  ander attribuut, een andere erflijn, de omgekeerde richting). Rood-eerst
  JUnit-getuige met R1 en R2.
- (c) Golden (werknaam `jutterswet-meldtermijn-strandjutter`), handgerekend
  (het scenario hierboven), in ref ≡ clingo ≡ Drools-exec; de pins van gate
  10 en 12 als **voorstel** waar de golden telt; de DRL-teksttak volgens haar
  kanaal (zij weigert defaultsgroepen); een negatief-case voor de omgekeerde
  richting.
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). DIVERGENCES §4: onder de fail-fast
  refusals een alinea over de erflijn (wat de frontend nu aanneemt, wat
  WF-050 blijft weigeren, met verwijzing naar ch. 7.10); CONSTRUCTS rij 39
  noemt de dialectvorm.

May touch: `frontends/regelspraak/**`, `corpus/golden/<nieuwe golden>/`,
`corpus/negatief/<nieuw>/`, `scripts/check-*.sh` (pins, **voorstel**),
kaartverplaatsing. Niet: `ref/`, `backends/**`, `spec/` (ch. 7.10 draagt de
semantiek al; bij een nieuwe draadvorm anders, dan eerst een spec-voorstel).
NB: geen bouwdatum; te wegen bij de kaartenreeks aan de frontend. Na de
landing kan een regelset een basisregel terugzetten op het type waar het
attribuut gedeclareerd is en alleen de afwijking op het subtype houden.

## Result (2026-10-01, bouwer)

Tak `card/frontend-afwijking-op-een-subtype` in de worktree van de kaart, afgetakt van main `094e5fad`; niet gerebased,
niet gepusht, niet gemerged. Gemeten met de eigen uitvoerders van deze repo: de frontend (`Resolver --ir out.json
bron.rgs` als `check`, en `Resolver --ir … --cases <map> bron.rgs scenario.rgs`), `_build/default/spec/verifier/bin/
verifier.exe`, `_build/default/ref/bin/runner.exe`, `_build/default/backends/clingo/bin/clingo_runner.exe` (gebouwd met
`dune build` op deze tak) en de Drools-jar uit `mvn -o -f backends/drools/pom.xml package` (`java -jar <jar> ir.json
case…`, `--drl` voor de teksttak; de backend is op de tak niet veranderd). "Vóór" is de frontend van de claim `61883079`
(code gelijk aan main), "na" die van `9c32a503`, de laatste commit die frontendcode raakt. Generator, meetscripts,
tabellen en sweep staan buiten de repo onder `/tmp/v2/probe/eas-bouw/`: `tools/` (`maak.py`, `meet.sh`, `alles.sh`,
`fe.sh`, `junit.sh`, `sweep.sh`, `tabel.py`), per vorm `vorm/<naam>/{bron.rgs,scenario.rgs,voor/,na/}` met
`meting.txt`, de tabel `tabel.md`, de handgeschreven draad-IR in `draad/`, de tegencases in `tegencases/`, de sweep in
`sweep/` en de kandidaat van de golden in `golden-kandidaat/`. De reproducties zijn uit
`/tmp/v2/probe/erflijn-afwijking/` gekopieerd (alleen gelezen) naar `/tmp/v2/probe/eas-bouw/repro/`.

### De reproductie op deze kop

| | `check` vóór | `check` na | verifier (na) | ref (na) | clingo (na) | exectak (na) | DRL-tekst (na) |
|---|---|---|---|---|---|---|---|
| R1 r1-afwijking-subtype, scenario.rgs | exit 1, de WF-050-zin van de kaart (`ERROR (line 18): Afwijking van 'meldtermijn': afwijking 'meldtermijn tot het vergunningseinde' schrijft een ander doel (Strandjutter.meldtermijn (attribuut)) dan de basisregel (Jutter.meldtermijn (attribuut)) — …`) | exit 0, `resolvedRegels=0`, `diagnostics: []` | `PASS` | `PASS (3 expectations)` | `PASS (3 expectations)` | `PLAYED 1` | `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering` |
| R2 r2-uitzondering-subtype, scenario.rgs | exit 1, dezelfde zin met `Uitzondering op` | exit 0 | `PASS` | `PASS (3 expectations)` | `PASS (3 expectations)` | `PLAYED 1` | idem |
| R3 r3-boom-op-subtype, scenario-sub.rgs | exit 0 | exit 0, IR byte-gelijk aan vóór (`cmp`) | `PASS` | `PASS (2 expectations)` | `PASS (2 expectations)` | `PLAYED 1` | idem |
| R4 r4-eigen-basis-subtype, scenario.rgs | exit 0 | exit 0, IR byte-gelijk aan vóór | `PASS` | `PASS (3 expectations)` | `PASS (3 expectations)` | `PLAYED 1` | idem |

R1 na de fix, op de draad: de groep `meldtermijn` op Jutter met alleen de basis, en de groep `meldtermijn voor
Strandjutter` op Strandjutter met één default zonder rechtvaardiging en zonder consequentie (een groep-default), die
één geordende uitzonderingsgroep draagt met de afwijking; de bron van die groep is `eigen uitzonderingen van
Strandjutter; de basis 'meldtermijn' is geërfd van Jutter`, die van de groep-default en de uitzonderingsgroep `Afwijking
van meldtermijn`. Byte voor byte gelijk aan de draad-IR die ik vóór de bouw met de hand schreef en door de vier lezers
haalde (`draad/ir.json`: verifier `PASS`, ref en clingo `PASS (3 expectations)`, exectak `PLAYED 1`). Dezelfde draad
met de afwijking als gewone default op Strandjutter (`draad/ir-vlak.json`) is ook verifier-schoon, maar daar is zij de
eigen basis van Strandjutter en vervangt zij de geërfde: ref `MISMATCH: T.meldtermijn = leeg, expected 2026-09-10`,
clingo `MISMATCH: T.meldtermijn is underived (leeg), expected 20706`, exectak `FAIL …: attribuut meldtermijn: verwacht
2026-09-10, werkelijk leeg`. De draadvorm van IR-SPEC ch. 2.2 en 4 (en 6.1) draagt de lowering dus zonder
spec-voorstel; de groep-default is wat het verschil maakt.

### Oorzaak

1. `RuleChecker.keurKnoop` vergeleek het doel van elk blok met het doel van de basisregel via `DefaultsDoel.gelijk`,
   dat het onderwerp op identiteit vergelijkt. Een blok voor Strandjutter op het geërfde `meldtermijn` was daarmee "een
   ander doel", en de boom bleef ongelift met de WF-050-zin.
2. De lowering kende per boom één groep, op het onderwerp van de basis. Een groep op het subtype bestond alleen als het
   subtype een eigen basisregel had (R4); de vorm die ch. 7.9 en 7.10 voor "eigen uitzondering, geërfde basis" al
   dragen (een groep-default op het subtype), schreef de frontend niet.
3. Daarbij hoorde de uitweg in de geneste weigering (een blok voor een subtype onder een uitzondering van het
   supertype): die vroeg een subtype zonder eigen regel er één te schrijven met de uitkomst en de voorwaarde van de
   geërfde basis, dus precies de omweg van R4.

### Fix en de gekozen weg

`RuleChecker` (+330, −31 regels, commentaar inbegrepen); de draad, de emitter, de verifier en de uitvoerders zijn niet
aangeraakt.

- `keurKnoop` neemt een blok direct onder de basisregel aan waarvan het onderwerp een strikt subtype is van het
  onderwerp van de basis en waarvan het doel hetzelfde attribuut of kenmerk is (WF-105 houdt een lidnaam bij één type
  van de erflijn, dus dat is het geërfde lid). `keurKinderen` geeft onder zo'n blok het doel van het subtype door, zodat
  een genest blok op hetzelfde subtype erin nest en een blok voor een kleinkind de geneste weigering krijgt.
- `boomDefaults` splitst de kinderen van de basis naar type: die van het eigen type gaan zoals voorheen in de groep van
  de basis (zonder eigen kinderen is dat de basis alleen); per subtype maakt `subtypeGroep` één groep op het subtype,
  naam `<basisregel> voor <subtype>`, met één groep-default die één geordende uitzonderingsgroep draagt met de blokken
  van dat subtype, het later geschreven blok hoger. De groep krijgt het venster van de basis. De bron van de groep zegt
  waar de basis vandaan komt; de groep-default en de uitzonderingsgroep dragen de kop of koppen die ze maakten (`Afwijking
  van meldtermijn`, of beide koppen met `en`).
- Een `Uitzondering op` neemt de voorwaarde van de basis mee, gelezen op het subtype: `opSubtype` (uitputtend over de
  verzegelde unies van `Conditie` en `Expr`) laat elke eigen lezing van het supertype het subtype noemen, omdat IR-SPEC
  ch. 4.1a (WF-053) een eigen lezing het onderwerpstype laat noemen; een genavigeerde lezing, een kenmerkcheck en een
  rolcheck houden hun knoop, en wat een vouw of kwantor per element leest blijft het element. Een conjunct zonder eigen
  lezing blijft dezelfde knoop. Een `Afwijking van` draagt alleen haar eigen voorwaarde.
- Benoemde weigeringen, elk één ERROR bij het blok, met de vorm die wel kan in de zin: een eigen basis op de erflijn
  (`subtypeBlokFout`, met `eigenBasisOpLijn`), een lid van een ongeordend blok in de eigen uitzonderingen van een subtype
  (`subtypeLidFout`), een subtypeblok dat vóór een blok van zijn supertype staat (`volgordeFout`) en een bezette
  groepsnaam (`subtypeNaamFout`, WF-020). De gemengde-kinderen-controle kijkt alleen naar kinderen van het eigen type van
  de drager. De uitweg van de geneste weigering noemt nu de eigen uitzondering onder de basis die het subtype erft.

Verworpen:

1. Het blok als gewone default in een groep op het subtype: dat is de eigen basis van het subtype en vervangt de
   geërfde (gemeten hierboven, verifier-schoon en in alle drie fout voor T).
2. De basis een gewone regel laten als zij alleen subtypeblokken heeft, en ch. 7.10 haar als geërfde basis laten vouwen
   (regel 4): speelt ook (`draad/ir-basis-als-regel.json`: verifier `PASS`, ref en clingo `PASS (3 expectations)`,
   exectak `PLAYED 1`), maar dan schrijft de frontend de basis in twee vormen naargelang er subtypeblokken zijn; de
   boom wordt nu altijd één groep genoemd naar zijn basis, zoals vorm 2 doet.
3. Het subtypeblok in de groep van de basis zetten: die groep heeft het supertype als onderwerp, zodat het blok voor
   elke Jutter zou gelden, en een lezing van een lid van het subtype daar weigert de verifier (WF-034, IR-SPEC ch. 2.2).
4. De basis op het subtype herhalen in de lowering (R4 door de frontend): de norm staat dan tweemaal op de draad, en de
   kaart vraagt de groep-default.
5. De voorwaarde van de basis opnieuw resolven met het subtype als onderwerp: dat herhaalt diagnostiek, en een tekst
   die het supertype noemt leest dan als navigatie vanaf de Strandjutter. Gemeten in een regel op Strandjutter met
   "indien de vinddag van de Jutter gevuld is": `ERROR (line 14): genavigeerde lezing 'de vinddag van de Jutter' zonder
   binding (geen relatie verbindt Strandjutter met Jutter): …`. Het omtypen van de opgeloste eigen lezingen is exact:
   dezelfde instantie, hetzelfde lid.
6. Ook de leden van `Uitzonderingen ongeordend op` voor een subtype lowerden: speelt in de drie (handgeschreven
   `draad/ir-lid.json`: de kanalen dragen `Jutter.meldtermijn/0 × S` in ref, clingo en de exectak), maar de caselader van
   ref en clingo neemt die conflictsleutel niet als verwachting aan (`CASE ERROR: inconsistent expectation references
   unknown rule "Jutter.meldtermijn/0": … present, non-exhaustive: "Jutter.meldtermijn", "Strandjutter.meldtermijn",
   "Strandjutter.meldtermijn/0"`), zodat geen case zo'n conflict kan pinnen. Benoemd geweigerd; open punt 1.

### (a) Vormruimte, vóór en na

Eén document per vorm met een scenario met handgerekende verwachtingen (`maak.py`); vinddag 10-03-2026 overal, plus 6
mnd 10-09-2026, plus 3 mnd 10-06-2026, plus 1 mnd 10-04-2026; vergunningseinde 01-06-2026 (plus 1 mnd 01-07-2026);
keurdag 20-03-2026; bergdag 15-04-2026 (plus 1 mnd 15-05-2026). Model: Jutter (vinddag, meldtermijn, meldplichtig,
gewaarschuwd), Strandjutter is een Jutter (vergunningseinde, vergunninghouder), Barnsteenjutter is een Strandjutter
(keurdag), Wrakjutter is een Jutter (bergdag); basis op Jutter `meldtermijn = vinddag plus 6 mnd indien zijn vinddag
gevuld is`. "PASS n" is per casebestand de letterlijke laatste regel van ref en clingo; de DRL-teksttak gaf bij elke
gelowerde vorm `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering`.

| Vorm | vóór | na |
|---|---|---|
| v01 `Afwijking van` op een direct subtype (R1, plus S2 alleen vergunningseinde: 01-06-2026, J2 leeg) | exit 1, WF-050-zin | exit 0; verifier `PASS`; ref en clingo `PASS (5)`; exec `PLAYED 1` |
| v02 `Uitzondering op` op een direct subtype (R2, S2 leeg: restrictief) | exit 1, WF-050 | exit 0; `PASS`; `PASS (5)` / `PASS (5)`; `PLAYED 1` |
| v03 `Afwijking van` op een kleinkind (Barnsteenjutter) | exit 1, WF-050 | exit 0; `PASS`; `PASS (5)` / `PASS (5)`; `PLAYED 1` |
| v04 `Uitzondering op` op een kleinkind | exit 1, WF-050 | exit 0; `PASS`; `PASS (5)` / `PASS (5)`; `PLAYED 1` |
| v05 twee subtypen (Strandjutter, Wrakjutter), elk een eigen afwijking onder één basis | exit 1, 2× WF-050 | exit 0; `PASS`; `PASS (5)` / `PASS (5)`; `PLAYED 1` |
| v06a uitzondering van het supertype (gewaarschuwd: plus 1 mnd) en afwijking van het subtype onder één basis; S met beide: 01-06 (eigen boven geërfd) | exit 1, WF-050 | exit 0; `PASS`; `PASS (6)` / `PASS (6)`; `PLAYED 1` |
| v06b de geneste vorm: `Uitzondering op <uitzondering van Jutter>` voor Strandjutter | exit 1, geneste weigering (uitweg: eigen basis op het subtype schrijven) | exit 1, geneste weigering (uitweg: `Afwijking van meldtermijn: waarschuwing met vergunning` met de volledige voorwaarde) |
| v06c die uitweg | exit 1, WF-050 | exit 0; `PASS`; `PASS (3)` / `PASS (3)`; `PLAYED 1` |
| v06d het subtypeblok vóór de uitzondering van het supertype geschreven | exit 1, WF-050 | exit 1, `… staat vóór uitzondering 'meldtermijn bij waarschuwing' voor Jutter …` |
| r4 eigen basis op het subtype (R4) | exit 0; `PASS (3)`; `PLAYED 1` | gelijk, IR byte-gelijk |
| v07b eigen basis op het subtype en een blok onder de basis van Jutter | exit 1, WF-050 | exit 1, `… Strandjutter heeft een eigen basisregel 'meldtermijn van de strandjutter' …` |
| v08 omgekeerd: basis op Strandjutter, afwijking voor Jutter | exit 1, WF-050 | exit 1, dezelfde WF-050-zin |
| v09a kenmerk als doel, `Uitzondering op` met ontkenning (`is niet meldplichtig`) | exit 1, WF-050 | exit 0; `PASS`; `PASS (5)` / `PASS (5)`; `PLAYED 1` |
| v09b kenmerk als doel, `Afwijking van` positief | exit 1, WF-050 | exit 0; `PASS`; `PASS (6)` / `PASS (6)`; `PLAYED 1` |
| v10 supertype zonder eigen instanties (alleen Strandjutters en een Barnsteenjutter) | exit 1, WF-050 | exit 0; `PASS`; `PASS (3)` / `PASS (3)`; `PLAYED 1` |
| v11 leden van `Uitzonderingen ongeordend op meldtermijn:` voor Strandjutter | exit 1, WF-050 (groepslid) | exit 1, `… lowert deze frontend niet …` |
| v12 opgave-markering op `meldtermijn` | exit 1, WF-050 | exit 0; `PASS`; `PASS (10)` / `PASS (10)`; `PLAYED 1` (de groep van het subtype schrijft `berekende meldtermijn`) |
| v13 een initialisatie van `meldtermijn` op Strandjutter naast het blok | exit 1, WF-050 | exit 1, de enige-schrijverzin: `… (defaults-groep 'meldtermijn voor Strandjutter', doel ook geschreven door regel 'meldtermijn bij onbekende vinddag'): a rule also writes defaults target 'Strandjutter.meldtermijn' — …` |
| v14 navigatie (`de sluitdag van zijn thuishaven`) en kenmerkcheck in de voorwaarde van de basis, `Uitzondering op` | exit 1, WF-050 | exit 0; `PASS`; `PASS (5)` / `PASS (5)`; `PLAYED 1` |
| v15 onvoorwaardelijke basis, `Uitzondering op` | exit 1, WF-050 | exit 0; `PASS`; `PASS (4)` / `PASS (4)`; `PLAYED 1` |
| v16 genest blok onder het eigen blok van het subtype | exit 1, WF-050 | exit 0; `PASS`; `PASS (4)` / `PASS (4)`; `PLAYED 1` |
| v17 blok voor het kleinkind onder het eigen blok van Strandjutter | exit 1, WF-050 | exit 1, geneste weigering (`… is een afwijking van Strandjutter, en …`) |
| v17c die uitweg (`Afwijking van meldtermijn: keuring` met de volledige voorwaarde) | exit 1, 2× WF-050 | exit 0; `PASS`; `PASS (6)` / `PASS (6)`; `PLAYED 1` |
| v18 blok voor het kleinkind onder de basis van Jutter, Strandjutter heeft een eigen basis | exit 1, WF-050 | exit 1, `… Barnsteenjutter erft van Strandjutter de basisregel …` |
| v18c die uitweg (onder de basis van Strandjutter) | exit 1, WF-050 | exit 0; `PASS`; `PASS (4)` / `PASS (4)`; `PLAYED 1` |
| v20a andere erflijn (Wrakjutter onder een basis van Strandjutter) | exit 1, WF-050 | exit 1, dezelfde WF-050-zin |
| v20b ander attribuut (`vinddag` voor Strandjutter onder `meldtermijn`) | exit 1, WF-050 | exit 1, dezelfde WF-050-zin |
| v22 twee blokken voor één subtype (later geschreven hoger) | exit 1, 2× WF-050 | exit 0; `PASS`; `PASS (3)` / `PASS (3)`; `PLAYED 1` |
| v23 beide koppen voor één subtype | exit 1, 2× WF-050 | exit 0; `PASS`; `PASS (4)` / `PASS (4)`; `PLAYED 1` |
| v24 venster `vanaf 01-01-2026` op basis en blok; peildatum 01-06-2026 en 01-06-2025 | exit 1, WF-050 | exit 0; `PASS`; `PASS (3)`, `PASS (3)` / idem; exec `EXCLUDE transpiler SKIP: … defaults-vensters niet gedragen` |
| v25 de basis ná het blok geschreven | exit 1, WF-050 | exit 0; `PASS`; `PASS (3)` / `PASS (3)`; `PLAYED 1` |
| v27 de basis noemt het supertype ("indien de vinddag van de Jutter gevuld is"), `Uitzondering op` voor Strandjutter | exit 1, WF-050 | exit 0; `PASS`; `PASS (5)` / `PASS (5)`; `PLAYED 1` |
| v26 ongeordend blok van Jutter onder de basis en een afwijking van Strandjutter | exit 1, gemengde-kinderen-weigering en WF-050 | exit 0; `PASS`; waarden en het conflict op J `PASS`; het conflict op een Strandjutter: ref en clingo `CASE ERROR: … unknown rule "Jutter.meldtermijn/1"`, exec `PLAYED 3` |

Flanken buiten de kaart: x24 (hetzelfde venster met een uitzondering van het eigen type, zonder subtype) geeft in de
exectak dezelfde `EXCLUDE … defaults-vensters niet gedragen`: de exectak sluit elk defaults-venster uit, ook zonder deze
lowering. x26 (een eigen boom op het subtype boven het ongeordende blok van Jutter, zonder deze lowering) geeft op de
claim al dezelfde `CASE ERROR` op `Jutter.meldtermijn/1`, terwijl alle drie dat kanaal dragen (ref `the channel carries:
Jutter.meldtermijn/1 × T`, clingo `jutter.meldtermijn/1 × T`, exec `Jutter.meldtermijn/1 @ T`): de sleutel van een
samengestelde stapel bestond al vóór deze kaart, en de caselader kent hem niet (open punt 1).

Telling: vóór weigerde `check` 34 van de 36 documenten van de kaartvormen (R3 en R4 speelden). Na: 24 nieuwe
documenten met exit 0 spelen met de handgerekende waarden in ref, clingo en de exectak (v24: de exectak sluit het
venster uit; v26: het verwachte conflict op een Strandjutter neemt de caselader van ref en clingo niet aan, open punt 1),
R3 en R4 houden byte-gelijke IR, en 10 weigeren met één ERROR: v06b en v17 (geneste weigering, nieuwe uitweg), v06d,
v07b, v11 en v18 (nieuwe zinnen), v13 (enige-schrijverzin), v08, v20a en v20b (WF-050 ongewijzigd).

### (b) Getuige

`AfwijkingOpSubtypeTest` (frontend, 19 tests): `r1_een_afwijking_op_een_subtype` en `r2_een_uitzondering_op_een_subtype`
(de groepen, de groep-default zonder rechtvaardiging en consequentie, de bronnen, het venster, voor R2 de conjunctie met
`vinddag` op Strandjutter en geen `"subjectType": "Jutter"` in de groep van het subtype), en zeventien rijen voor de
vormruimte: `een_kleinkind_onder_de_basis_van_het_supertype`, `twee_subtypen_elk_een_eigen_groep`,
`een_uitzondering_van_het_supertype_blijft_in_zijn_groep`, `twee_blokken_voor_een_subtype_het_later_geschrevene_hoger`,
`beide_koppen_voor_een_subtype`, `een_kenmerk_als_doel` (geen WARNING "kent het nooit toe"),
`de_voorwaarde_van_de_basis_wordt_op_het_subtype_gelezen` (eigen lezing omgetypt, navigatie en kenmerkcheck dezelfde
knoop), `de_opgave_markering_verlegt_ook_de_groep_van_het_subtype`, `een_eigen_basis_op_het_subtype_blijft_spelen` (R4),
en de weigeringen letterlijk: `de_geneste_vorm_weigert_en_noemt_de_basis`,
`een_subtypeblok_voor_een_blok_van_het_supertype_weigert`,
`een_eigen_basis_van_het_subtype_weigert_het_blok_onder_de_basis_van_het_supertype`,
`een_eigen_basis_op_een_tussentype_weigert`, `een_ongeordend_blok_voor_een_subtype_weigert`,
`een_bezette_groepsnaam_weigert`, `de_omgekeerde_richting_houdt_de_doelweigering`,
`een_andere_erflijn_en_een_ander_attribuut_houden_de_doelweigering`.

Rood eerst: op `68fc0f10` (R1 en R2, tegen de klassen van de claim) `[ 2 tests failed ]`, beide op `onverwachte
diagnostiek: [ERROR (line 18): … schrijft een ander doel (Strandjutter.meldtermijn (attribuut)) dan de basisregel
(Jutter.meldtermijn (attribuut)) …]`. De klasse van `7f681bd3` tegen de klassen van de claim: 19 tests, 16 rood, de drie
flankrijen groen (`een_eigen_basis_op_het_subtype_blijft_spelen`, `de_omgekeerde_richting_houdt_de_doelweigering`,
`een_andere_erflijn_en_een_ander_attribuut_houden_de_doelweigering`). Na de fix: 19 groen; de suite 1063 tests, 0
failures (1044 op de claim).

Drie rijen van een bestaand testbestand zijn aangepast, omdat hun gepinde zin terecht verandert (de uitweg van de geneste
weigering vroeg een herhaalde basis op het subtype, de vorm die deze kaart overbodig maakt), in
`UitzonderingOpUitzonderingTest`:

- `een_geerfde_uitzondering_als_drager_weigert`: oud `contains("Schrijf een eigen uitzondering van Wrakrecht op een
  basisregel van Wrakrecht, met de volledige voorwaarde van 'uitkering van een betwist strandrecht'")`; nieuw
  `contains("Schrijf haar als eigen uitzondering van Wrakrecht onder de basisregel 'uitkering van een strandrecht':
  'Afwijking van uitkering van een strandrecht: uitkering van een diep wrakrecht', met de volledige voorwaarde van
  'uitkering van een betwist strandrecht'")`.
- `een_geerfde_drager_zonder_eigen_basis_noemt_de_basisregel_die_erbij_hoort`: oud `endsWith("gaan vóór de geërfde, dus
  zij beslist precies waar beide gelden. Wrakrecht heeft nog geen eigen basisregel voor 'uitkering': schrijf die met de
  uitkomst en de voorwaarde van 'uitkering van een strandrecht', want een eigen basisregel vervangt de geërfde (dialect
  §4)")`; nieuw `contains("onder de basisregel 'uitkering van een strandrecht': 'Afwijking van uitkering van een
  strandrecht: uitkering van een diep wrakrecht'")`, `endsWith("gaan vóór de geërfde, dus zij beslist precies waar beide
  gelden (dialect §4)")` en `!contains("nog geen eigen basisregel")`.
- `een_geerfde_drager_met_eigen_basis_noemt_geen_nieuwe_basisregel`: oud `contains("Schrijf een eigen uitzondering van
  Wrakrecht op een basisregel van Wrakrecht")`; nieuw `contains("Schrijf haar als eigen uitzondering van Wrakrecht onder
  zijn eigen basisregel 'uitkering van een wrakrecht': 'Afwijking van uitkering van een wrakrecht: uitkering van een diep
  wrakrecht'")`; de `endsWith` en de `!contains` bleven.

### (c) Golden en negatief-case

`corpus/golden/jutterswet-meldtermijn-strandjutter/` (`bron.rgs`, `scenario.rgs`, `ir.json`, twee cases, `NOTE.md` met
de casustekst "Art. 3h (meldtermijn van een vondst)", de lowering, de stapels per concreet type en de handberekening),
toegelaten met `scripts/admit.sh`: `admit: ADMITTED corpus/golden/jutterswet-meldtermijn-strandjutter (2 case(s);
verifier-clean, ref = clingo, no-op refuted, gates green)`, gedraaid met het voorstel voor gate 12 in de werkboom. De
bron is R1 met een `// Bron:`-annotatie op beide regels. De IR uit `bron.rgs` alleen is byte-gelijk aan die uit `bron.rgs`
met `scenario.rgs`.

| Case | Instanties en invoer | `meldtermijn` verwacht | ref | clingo |
|---|---|---|---|---|
| case1-meldtermijn-over-de-erflijn (het scenario van de kaart) | J vinddag 10-03-2026; S vinddag 10-03-2026, vergunningseinde 01-06-2026; T vinddag 10-03-2026 | J 10-09-2026, S 01-06-2026, T 10-09-2026; consistent | `PASS (4 expectations)` | `PASS (4 expectations)` |
| case2-vergunningseinde-zonder-vinddag | S2 vergunningseinde 01-06-2026; J2 niets | S2 01-06-2026 (een afwijking draagt alleen haar eigen voorwaarde), J2 leeg; consistent | `PASS (3 expectations)` | `PASS (3 expectations)` |

Exectak `PLAYED 2`; DRL-teksttak `EXCLUDE drl: ch. 7 prioritizedDefaults — …`. Onder `--noop` geven beide exit 1. Vier
tegencases (buiten de repo, `tegencases/`, elk één verwachting omgedraaid) falen in alle drie: T leeg (de afwijking als
eigen basis) ref `MISMATCH: T.meldtermijn = 2026-09-10, expected leeg`, clingo `MISMATCH: T.meldtermijn = [20706],
expected leeg`, exec `FAIL …: verwacht leeg, werkelijk 2026-09-10`; S 10-09-2026 (de basis boven de afwijking); J leeg
(de boom naar het subtype verhuisd, R3); S2 leeg (de restrictieve lezing).

Negatief-case: niet gemaakt. Beide draadvormen die de omgekeerde richting zou kunnen krijgen zijn verifier-schoon
(`draad/ir-omgekeerd.json`: de afwijking als groep-default op Jutter naast de basisgroep op Strandjutter;
`draad/ir-omgekeerd-een-groep.json`: één groep op Jutter met beide): `verifier: PASS`. Ze zijn welgevormd en zeggen iets
anders dan de bron (de afwijking geldt dan voor elke Jutter), dus de verifier heeft geen regel om ze onder te weigeren
en een map onder `corpus/negatief/` zou gate 4 rood maken. De weigering is gepind in gate 10
(`de_omgekeerde_richting_houdt_de_doelweigering`, de zin letterlijk).

### Pinvoorstellen

- Gate 12 (`scripts/check-drools.sh`, beschermd pad, eigen commit `ddb6b5f2`): `EXPECTED_PLAYED=234` → `235` met
  redenblok `# 234 -> 235 (2026-10-01, an exception of a subtype under its supertype's base):
  jutterswet-meldtermijn-strandjutter, new, from bron.rgs and scenario.rgs and hand-computed: … No source file of this
  backend changed: the exec branch plays the two cases on the first run, PLAYED 2, with ref's and clingo's values. The
  DRL-text branch refuses the new golden on its defaults sentence (class (A) of DRL_CHANNEL), so EXPECTED_DRL_PLAYED does
  not move.` `EXPECTED_DRL_PLAYED` blijft 192. DRL_CHANNEL klasse (A), in naamvolgorde tussen `jutterswet-meldingsketen`
  en `jutterswet-nestkenmerk-rgs`: `"jutterswet-meldtermijn-strandjutter|EXCLUDE|ch. 7 prioritizedDefaults: no defaults
  lowering in this branch"`. Kop (A): `39 lines` → `40 lines`, `sitting 42 below` → `sitting 43 below`, `38 of the 39
  lines` → `39 of the 40 lines`, `the 39th, jutterswet-havenrooster` → `the 40th, …`, `All 38 ARE` → `All 39 ARE`, `The
  42 is 46 exec-only players — those 38,` → `The 43 is 47 exec-only players — those 39,`, en een hertelzin (A 40, B 37,
  C 13, D 2, E 3, F 3 = 98 regels, tegen de lijst geteld; kloof 43 = 235 - 192). Kop DRL_CHANNEL: `ninety-one of the
  ninety-seven` → `ninety-two of the ninety-eight`. Gemeten: `played 235`, `played matches the pinned 235`, `DRL-text
  branch played 192 (… pin 192)`, `DRL-text floor 98 non-PLAYED goldens pinned by name`.
- Gate 10: `EXPECTED_MIN_GOLDENS=80` en `EXPECTED_MIN_TESTS=173` zijn vloeren; gemeten 117 rgs-goldens en 1063 tests.
  Geen pin te verzetten; redenregel als de eigenaar de vloer ophoogt: `117 (2026-10-01, +1
  jutterswet-meldtermijn-strandjutter)`.
- Gate 16: vloeren 7 en 31; gemeten 24 scenario-goldens en 84 casebestanden. Redenregel bij ophogen: `24 / 84 (2026-10-01,
  +1 golden en +2 cases van jutterswet-meldtermijn-strandjutter)`.
- Gate 4: geen negatief-case (zie (c)); de vloeren 350/260/547 gemeten op 503/346/607, ongewijzigd.

### (d) Sweep

- `sweep.sh` over alle 116 `bron.rgs` van het corpus vóór de golden: de frontend van de claim en die van `9c32a503`
  geven op alle 116 dezelfde IR, dezelfde uitvoer en dezelfde exitcode, en de IR van na is byte-gelijk aan de
  ingecheckte `ir.json` (116 van 116). Geen bron schrijft een blok voor een subtype onder de basis van een supertype.
- Gate 10: 117 rgs-goldens byte-gelijk opnieuw uitgegeven (116 en de nieuwe). Gate 11: 45 `.rgs` van de pariteitszone
  byte-gelijk. Gate 16: 24 scenario-goldens regenereren hun 84 casebestanden en `ir.json` byte voor byte. Gate 15:
  de graaf van de draad is gelijk aan die van de bron op alle 117 goldens met een `bron.rgs`.
- Geen bestaande golden gewijzigd.

### DIVERGENCES (`038a5f38`) en CONSTRUCTS

`frontends/regelspraak/grammar/DIVERGENCES.md` §4: onder "Fail-fast refusals" de alinea "The inheritance line" (wat de
frontend nu aanneemt en hoe het lowert, met de verwijzing naar ch. 7.9 en 7.10 en de meting van de gewone default; de
vier benoemde weigeringen; dat WF-050 zijn zin houdt voor een ander attribuut of kenmerk, een andere erflijn en de
omgekeerde richting, die de verifier niet weigert en gate 10 pint; de initialisatie op het subtype; de gemeten vormen;
de golden). Verder een rij in de loweringtabel van vorm 2, de uitweg in de bullet van de geneste weigering, de golden in
"Scope / limitations of the first card", en in §7 de zin over een `Afwijking van` op de regel van T voor het subtype (was:
"refused by the target refusal of §4"). `spec/CONSTRUCTS.md` rij 39 niet aangeraakt; tekst hieronder.

### Gates

Alle zestien in de volgorde van `gates.yml` met `/tmp/v2/uchar-gates18/run.sh` op de kop `038a5f38` (de laatste commit
vóór dit Result; de commit met dit Result raakt alleen de kaart), met een schone werkboom: `eval $(opam env)`, `dune
build` (exit 0), dan elk script onder zijn shebang. Logs in `/tmp/v2/uchar-gates18/`. Per gate de exitcode en de
PASS-regel:

     1. check-self-contained: exit 0 — check-self-contained: PASS
     2. check-ir-schema: exit 0 — check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 330 file(s) validate and round-trip, floor 192; …)
     3. check-case-schema: exit 0 — check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 827 case file(s) validate)
     4. check-verifier: exit 0 — check-verifier: PASS (330 golden clean; 503 negatief rejected for their declared rule, floor 350; 346 on their declared arm (expect.txt), floor 260; 607 cited pointers resolve in their documents, floor 547)
     5. check-oracle: exit 0 — check-oracle: PASS (ref suite 1152 worked examples, floor 489; 785 cases replayed exactly, floor 427; NOT replayed, no case file yet: …)
     6. check-clingo: exit 0 — check-clingo: PASS (backend suite 1988 checks, floor 83; 785 cases replay exactly through clingo — ref = clingo over the corpus)
     7. check-noop: exit 0 — check-noop: PASS (741 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
     8. check-rood: exit 0 — check-rood: PASS (42 red cases fault observably in ref and clingo, floor 26; 84 pinned fragments cite none of the 36 listed bottom sentences)
     9. check-catala: exit 0 — check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    10. check-regelspraak: exit 0 — check-regelspraak: PASS — JUnit suite green (1063 tests, floor 173); 117 rgs golden(s) re-emit byte-identically (floor 80)
    11. check-toka-parity: exit 0 — check-toka-parity: PASS
    12. check-drools: exit 0 — check-drools: PASS
    13. check-reftype-derivation: exit 0 — check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    14. check-flogic: exit 0 — check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    15. check-graaf: exit 0 — check-graaf: PASS
    16. check-scenario: exit 0 — check-scenario: PASS — 24 scenario golden(s) (floor 7) regenerate 84 case file(s) (floor 31) and their ir.json byte-identically

Gate 12 verder: `played 235`, `channel floor 55 non-PLAYED goldens pinned by name; played matches the pinned 235`,
`DRL-text branch played 192 (… pin 192)`, `DRL-text floor 98 non-PLAYED goldens pinned by name`, `JUnit 1020 tests,
floor 239`. Gate 11: `re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (…); TOKA suite 27
tests, floor 27`. Gate 15: `route leg — the graph of the wire equals the graph of the source on all 117 goldens with a
bron.rgs (floor 98); 23 unit tests green (floor 23)` en `spec leg — 253 of 290 goldens compared, 183 dependencies equal to
the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)`.

### Afwijkingen van de kaart

1. (c) Geen negatief-case: de verifier neemt elke draadvorm van de omgekeerde richting aan (zie (c)); de weigering is
   gepind in gate 10.
2. `Uitzonderingen ongeordend op` met leden voor een subtype is niet gelowerd maar benoemd geweigerd (de kaart noemt
   `Uitzondering op` en `Afwijking van`; de conflictsleutel van zo'n groep kan geen case verwachten, open punt 1). Ook een
   ongeordend blok onder het eigen blok van een subtype weigert, zodat de groep van een subtype nooit een conflict draagt.
3. Drie benoemde weigeringen erbij in dezelfde laag, elk als vorm gemeten (tabel (a)): een eigen basis op de erflijn
   (het blok hoort daaronder; zonder de weigering stonden de groep van het subtype en zijn eigen basis op één niveau, wat
   WF-050 als tweede schrijver weigert, RUNTIME-SEMANTICS ch. 7.10), een subtypeblok dat vóór een blok van zijn
   supertype staat (de geschreven volgorde zou het omgekeerde zeggen van ch. 7.10; open punt 3), en een bezette
   groepsnaam (WF-020).
4. "Het rapport zegt bij de groep dat de basis geërfd is" is uitgevoerd als de bron van de groep op de draad (`eigen
   uitzonderingen van Strandjutter; de basis 'meldtermijn' is geërfd van Jutter`): een uitleg die de groep toont, leest
   haar `source`. Een INFO-diagnose kan niet: WF-113 (a) laat alleen `warning` op de draad, en een WARNING meldt geen
   probleem hier. Het `check`-rapport van de distributie staat buiten deze repo.
5. De uitweg van de geneste weigering is herschreven (drie rijen van `UitzonderingOpUitzonderingTest`, oud en nieuw in
   (b)): zij vroeg een herhaalde basis op het subtype, en die omweg is met deze lowering niet meer nodig.
6. De golden heeft naast het scenario van de kaart een tweede case (S2 en J2), die de verruimende lezing van `Afwijking
   van` op het subtype pint.

### Voorgestelde registertekst (voor de hoofdsessie)

`spec/CONSTRUCTS.md` rij 39, een alinea achter de laatste Landed-alinea:

> **Landed (2026-10-01): the dialect's exception of a subtype under its supertype's base.** The RegelSpraak dialect's
> `Uitzondering op` and `Afwijking van` (`frontends/regelspraak/grammar/DIVERGENCES.md` §4, "The inheritance line") may
> be written for a strict subtype of the base rule's subject, on the target the subtype inherits. The frontend lowers the
> subtype's blocks to one more defaults group, on the subtype, named `<base> voor <subtype>`, whose one default is a
> group-default (`RUNTIME-SEMANTICS.md` ch. 7.9) holding them as one ordered exception group. A group-default is no
> base, so the ch. 7.10 composition keeps the inherited base below the subtype's own exceptions; as an ordinary default
> the block would be the subtype's own base and replace the inherited one (measured on that IR, verifier-clean: a subtype
> instance for which the block decides nothing stays empty in all three executors). A restrictive block conjoins the
> base's condition with its own reads naming the subtype (ch. 4.1a). Refused by name: an own base on the subtype's line
> (the block belongs under it), an unordered block among a subtype's own exceptions (the case loader of ref and clingo
> does not take the composed conflict key), a subtype's block written before a block of its supertype under the same
> base, and a group name another form carries; WF-050's target sentence stays for every other target, the reverse
> direction included, which the verifier does not refuse and gate 10 pins. Witness `golden/jutterswet-meldtermijn-strandjutter`
> (two cases from a scenario document; ref ≡ clingo ≡ Drools exec, the DRL-text branch refuses it on its defaults
> sentence). No wire, spec, verifier or executor change.

Geen tekst voor `spec/IR-SPEC.md` of `spec/RUNTIME-SEMANTICS.md`: de draad en de semantiek veranderen niet.

### Open punten

1. De caselader van ref en clingo controleert een verwacht conflict tegen de paden van de gedeclareerde groepen
   (onderwerp van de groep, eigen index), terwijl alle drie de uitvoerders de sleutel van de samengestelde stapel
   schrijven (het declarerende type, de index in de stapel van het concrete type): `Jutter.meldtermijn/1 × T` voor het
   ongeordende blok van Jutter op een Strandjutter met een eigen groep. Op de claim al bereikbaar zonder deze lowering
   (x26: een eigen boom op het subtype boven het ongeordende blok van Jutter; ref en clingo `CASE ERROR … unknown rule
   "Jutter.meldtermijn/1"`, de exectak `PLAYED 1`). Laag `ref/` (en de spec, of ch. 7.9/7.10 de padsleutel op de
   samengestelde stapel moet noemen). Tot dan weigert de frontend leden van een ongeordend blok in de eigen
   uitzonderingen van een subtype.
2. Een initialisatie van het doel op het subtype naast de groep van het subtype krijgt de enige-schrijverzin van WF-050
   (v13). Zonder het blok neemt de vouw van ch. 7.10 haar op als initialisatiedefault van de stapel van het subtype. De
   lowering zou een onvoorwaardelijke initialisatie in de groep van het subtype kunnen opnemen (blok 4 van ch. 7.10); een
   voorwaardelijke past op de draad niet in een groep (WF-050). Ontwerpkeuze voor de eigenaar.
3. Normkeuze: een subtypeblok dat vóór een blok van zijn supertype onder dezelfde basis staat, weigert nu. Het
   alternatief is het aannemen met de blokvolgorde van ch. 7.10 en vastleggen dat de geschreven volgorde tussen niveaus
   niet telt; de weigering kan later vervallen zonder dat een aangenomen document van betekenis verandert.
4. De exectak sluit elk defaults-venster uit (`defaults-vensters niet gedragen`, v24 en x24); ref en clingo spelen het.
   Bestaand, buiten deze laag.
5. Het voorstel voor gate 12 (`ddb6b5f2`) staat op een beschermd pad en landt met de golden (zonder het is gate 12 rood:
   235 spelers tegen pin 234).
6. De registertekst voor `spec/CONSTRUCTS.md` rij 39 hierboven: hoofdsessie.
7. De distributie (`bin/regelspraak`) draait op haar eigen submodulepin en weigert R1 en R2 tot die pin deze kop bevat.

### Commits

    61883079 claim: frontend-afwijking-op-een-subtype
    68fc0f10 AfwijkingOpSubtypeTest: een afwijking op een subtype, rood
    9c32a503 Resolver: een afwijking op een subtype als eigen uitzondering
    7f681bd3 AfwijkingOpSubtypeTest: de vormruimte van de kaart
    845107b2 golden jutterswet-meldtermijn-strandjutter: afwijking op een subtype
    ddb6b5f2 voorstel gate 12: jutterswet-meldtermijn-strandjutter speelt in exec
    038a5f38 DIVERGENCES §4: de erflijn onder de fail-fast refusals

en de commit die de kaart met dit Result naar `backlog/done/` verplaatst. Niet gepusht, niet gemerged.

## Keuring (hoofdsessie, 2026-10-01)

Eigen run van de zestien gates op `556e9f52` in de volgorde van `gates.yml`
(bash waar de shebang dat zegt), `dune build` vooraf; logs in
`/tmp/v2/uchar-gates18/`. Alle zestien exit 0: gate 4 330 goldens schoon,
503 negatief-cases, 346 op hun arm; gate 10 JUnit 1063, 117 rgs-goldens;
gate 12 `check-drools: PASS`, played 235 (pin 235), DRL 192 (pin 192),
JUnit 1020; gate 16 24 scenario-goldens, 84 casebestanden. De fix gelezen:
`keurKnoop` neemt een blok direct onder de basis aan voor een strikt
subtype op het geërfde lid; `boomDefaults` splitst de kinderen per type en
`subtypeGroep` maakt per subtype één groep `<basis> voor <subtype>` met
één groep-default (ch. 7.9) die de blokken als geordende
uitzonderingsgroep draagt, zodat ch. 7.10 de geërfde basis eronder houdt;
`opSubtype` laat voor een `Uitzondering op` de eigen lezingen van de
basisvoorwaarde het subtype noemen (WF-053); vier benoemde weigeringen
(eigen basis op de erflijn, lid van een ongeordend blok, subtypeblok vóór
een supertypeblok, bezette groepsnaam). Afwijking 1 aanvaard (geen
negatief-case: de verifier neemt de omgekeerde richting aan, gate 10 pint
de weigering); 2 aanvaard (ongeordende leden benoemd geweigerd zolang de
caselader van ref en clingo de samengestelde conflictsleutel niet aanneemt,
open punt 1); 3 aanvaard; 4 aanvaard (de bron van de groep zegt dat de
basis geërfd is; WF-113 laat geen INFO toe); 5 aanvaard (de uitweg van de
geneste weigering noemt nu de eigen uitzondering onder de geërfde basis;
drie gepinde zinnen in `UitzonderingOpUitzonderingTest` volgen, oud en
nieuw in het Result); 6 aanvaard. De voorgestelde registertekst is in dit
landingscommit gezet in `spec/CONSTRUCTS.md` rij 39 (Landed-alinea). Voor
de eigenaar: open punt 1 (de caselader van ref en clingo neemt de
padsleutel van de samengestelde stapel niet aan; laag `ref/` en de spec)
en open punt 3 (normkeuze: telt de geschreven volgorde tussen een
subtypeblok en een supertypeblok, nu een weigering); 2 is een
ontwerpkeuze (initialisatie op het subtype naast de groep); 4 bestaand
(de exec-tak sluit defaults-vensters uit). Commitkoppen tot 68 tekens
aanvaard.
