# frontend-ongebonden-lezing-check: een lezing die niet bindt passeert `check` als ongedeclareerd attribuut

*Vondst van een regelsetbouwer, 2026-09-30 (skelet-vervolg, waarneming G2
in zijn Result), bevestigd door de hoofdsessie op uchar `b0aab696` via de
distributie (`bin/regelspraak check|scenario|cases`) en de eigen
uitvoerders; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/check-ongebonden/` (a-subtype-lid.rgs, b-ongedeclareerd.rgs,
scenario.rgs, cases-*/; alleen lezen). Stil in de frontend (`check` exit 0,
IR met lege diagnostics), luid pas in de uitvoerders: de exec-tak crasht
met "geen getter", ref en clingo falen benoemd. Op verzoek van de eigenaar
geschreven (2026-09-30).*

## Aanleiding

Een genavigeerde lezing waarvan het lid niet bestaat op de rolvuller — "de
toeslag van de jutter van de Vondst", waar `toeslag` alleen op het subtype
Strandjutter staat, of "de rang van de jutter van de Vondst", waar `rang`
nergens staat — wordt door de resolver niet als fout gezien maar herlezen
als een attribuut met de naam `toeslag van de jutter` van de Vondst zelf.
Dat attribuut is nergens gedeclareerd; de frontend schrijft het toch in het
IR, zonder WARNING of ERROR. `check` zegt "resolved=1" en zwijgt. Ref en
clingo weigeren het document dan benoemd ("'toeslag van de jutter' is not a
declared attribute of 'Vondst': a value position reads an attribute …"); de
Drools-exec-tak crasht ("java.lang.IllegalStateException: geen getter voor
'toeslag van de jutter' op … Vondst"). DIVERGENCES ("An undeclared
attribute read — refused for one shape") laat zo'n lezing bewust aan de
verifier; maar de poort `check` van de distributie draait de verifier niet,
en een regelsetbouwer die op `check` vertrouwt, ziet de fout pas bij het
scenario, met een crash in plaats van een zin. Namen met spaties en
koppelwoorden ("maxdatumverschuiving van de lopende periode") zijn in het
dialect gewoon, dus de herlezing "toeslag van de jutter" is een echte
dubbelzinnigheid van de grammatica, niet een tikfout die de lexer al vangt.

## Minimale reproductie

`b0aab696`, distributie `bin/regelspraak check|scenario|cases`; ref via
`_build/default/ref/bin/runner.exe`, clingo via
`_build/default/backends/clingo/bin/clingo_runner.exe`.

    Objecttype de Jutter (mv: Jutters) (bezield)
        de premie Numeriek (geheel getal);
    Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
        de toeslag Numeriek (geheel getal);
    Objecttype de Vondst (mv: Vondsten)
        de bonus Numeriek (geheel getal);
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten

    Regel bonus
        geldig altijd
            De bonus van een Vondst moet berekend worden als de toeslag van de jutter van de Vondst plus 1.

    Scenario: Jutter J (premie 5) met Vondst V; verwacht: de bonus van V is leeg (de verwachting dient
    alleen om de uitvoerders te laten spreken).

    R1  a-subtype-lid.rgs, zoals hierboven (het lid bestaat op een subtype van de rolvuller).
        nu: check exit 0, geen ERROR of WARNING, "regels=1 resolved=1"; IR: `"attribute": "toeslag van
        de jutter"` op Vondst, diagnostics leeg; scenario FAIL met "java.lang.IllegalStateException:
        geen getter voor 'toeslag van de jutter' op dev.uchar.regelspraak.facts.Vondst"; ref
        "EVALUATION FAULT: \\"toeslag van de jutter\\" is not a declared attribute of \\"Vondst\\": a
        value position reads an attribute …"; clingo "TRANSLATION FAULT: …" met dezelfde zin.
    R2  b-ongedeclareerd.rgs: `de rang van de jutter van de Vondst`, `rang` bestaat nergens.
        nu: identiek: check exit 0, IR met `"attribute": "rang van de jutter"`, scenario "geen getter",
        ref en clingo benoemd.

## Acceptatie

- (a) Meet de vormruimte: het lid op een subtype van de rolvuller, nergens,
  en op een ander objecttype dan de rolvuller; een verkeerde rolnaam; de
  lezing in een waarde, in een voorwaarde, in een rechtvaardiging van een
  default en in de subselectie van een vouw; één en twee rollen ver; en de
  bindende lezing als de vorm die moet blijven spelen.
- (b) De resolver weigert bij `check` benoemd, in de woorden van de auteur:
  welk lid niet bestaat op welke rolvuller, en waar het wel bestaat (het
  subtype, WF-053) als dat zo is; geen herlezing als attribuut van het
  onderwerp tenzij dat attribuut gedeclareerd is. Rood-eerst JUnit-getuige
  met R1 en R2. Blijft de herlezing ergens nodig (een gedeclareerd attribuut
  met een koppelwoord in zijn naam), dan wint de declaratie en zegt het
  rapport dat.
- (c) Negatief-cases in het corpus (werknaam `jutterswet-ongebonden-lezing`)
  voor R1 en R2 met `expect.txt`, zodat gate 4 de weigering pint; de pins
  van gate 4 en 10 als **voorstel** waar zij verschuiven.
- (d) Corpussweep ongewijzigd; alle zestien gates groen (bash waar de
  shebang dat zegt). DIVERGENCES: de sectie over de ongedeclareerde lezing
  zegt wat de frontend nu weigert en wat aan de verifier blijft. Noot voor de
  distributie (buiten deze kaart): `bin/regelspraak check` zou als tweede
  lijn de verifier kunnen draaien.

May touch: `frontends/regelspraak/**`, `corpus/negatief/<nieuw>/`,
`scripts/check-*.sh` (pins, **voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/**`, `spec/`. NB: niet nodig vóór de regelset verder gaat (alle
regels van de regelset binden); wel vóór een volgende bouwer erop vertrouwt
dat `check` een lezing zonder lid vangt.

## Result (2026-10-01, bouwer)

Tak `card/frontend-ongebonden-lezing-check` in de worktree van de kaart, afgetakt van main `08d70b7e`; niet gerebased,
niet gepusht, niet gemerged. Gemeten met de eigen uitvoerders van deze repo: de frontend (`Resolver --ir out.json
bron.rgs` als `check`, en `Resolver --ir … --cases … bron.rgs scenario.rgs`), `_build/default/spec/verifier/bin/verifier.exe`,
`_build/default/ref/bin/runner.exe`, `_build/default/backends/clingo/bin/clingo_runner.exe` en, voor de reproductie, de
Drools-jar van gate 12 (`java -jar <jar> ir.json case…`, `--drl` voor de teksttak). "Vóór" is de frontend van de claim
`8d0de735` (code gelijk aan main), "na" die van `7b8f0f23`, de enige commit die frontendcode raakt. Elke vorm is een
eigen Jutterswet-document (bron.rgs + scenario.rgs), door een generator geschreven en per vorm gemeten. Generator,
meetscripts en tabellen staan buiten de repo: `/tmp/v2/probe/olc-bouw/tools/` (`maak.py`, `maak-flank.py`, `meet.sh`,
`meet1.sh`, `tabel.py`, `ongedeclareerd2.py`), per vorm `vorm/<naam>/{voor,na}/meting.txt` en
`flank/<naam>/{voor,na}/meting.txt`, de tabellen `tabel-voor.txt`, `tabel-na.txt`, `tabel-flank-voor.txt`,
`tabel-flank-na.txt`. De reproducties zijn uit `/tmp/v2/probe/check-ongebonden/` gekopieerd (alleen gelezen) naar
`/tmp/v2/probe/olc-bouw/repro/`.

### De reproductie op deze kop

| | `check` vóór | `check` na | verifier (IR van vóór) | ref (vóór) | clingo (vóór) | exectak (vóór) | DRL-tekst (vóór) |
|---|---|---|---|---|---|---|---|
| R1 a-subtype-lid | exit 0, `ir: objecttypen=3 feittypen=1 resolvedRegels=1 geschreven naar …`, `"diagnostics": []` | exit 1: `ERROR (line 14): genavigeerde lezing 'de toeslag van de jutter van de Vondst': het lid 'toeslag' bestaat niet op 'Jutter', de vuller van de rol 'jutter', wel op het subtype 'Strandjutter': the navigation reaches 'Jutter', not the endpoint 'Strandjutter'` | `WF-034 FAIL …/value/left: 'toeslag van de jutter' is neither an attribute nor a characteristic of 'Vondst'` | `EVALUATION FAULT: "toeslag van de jutter" is not a declared attribute of "Vondst": a value position reads an attribute` | `TRANSLATION FAULT:` dezelfde zin | `ERROR ConsequenceException: … java.lang.IllegalStateException: geen getter voor 'toeslag van de jutter' op dev.uchar.regelspraak.facts.Vondst` | `EXCLUDE drl-transpiler MARKER: regel: bonus — TODO navigatie: de toeslag van de jutter van de Vondst` |
| R2 b-ongedeclareerd | idem (`objecttypen=2`) | exit 1: `ERROR (line 12): genavigeerde lezing 'de rang van de jutter van de Vondst': het lid 'rang' bestaat niet op 'Jutter', de vuller van de rol 'jutter', en op geen ander objecttype: 'rang' is neither an attribute nor a characteristic of 'Jutter'` | `… 'rang van de jutter' is neither an attribute nor a characteristic of 'Vondst'` | idem met `"rang van de jutter"` | idem | `… geen getter voor 'rang van de jutter' op …Vondst` | `… TODO navigatie: de rang van de jutter van de Vondst` |

Met het scenario (`--cases`) schrijft de frontend na de fix voor beide niets en eindigt met exit 1 op dezelfde ERROR.

### Oorzaak

1. De naam van een lezing loopt over elke binnenste 'van' (de gulzige `kaleNaam`), dus "de toeslag van de jutter van de
   Vondst" komt in `ExpressieBouwer.attribuutRef` aan als attribuut `toeslag van de jutter` met onderwerp `de Vondst`.
   De ketenwandeling daar legt een navigatie alleen waar het deel vóór een 'van' een gedeclareerd attribuut is van de
   vuller die de rollen erachter bereiken. Staat het lid niet op die vuller, dan blijft de lezing een attribuut met de
   lange naam op het type van het onderwerp, getypeerd als onbekend.
2. De controles na de lift weigerden zo'n naam alleen in één vorm: waar het deel achter een 'van' een gedeclareerd
   attribuut van datzelfde type is (`keurOpgeslokteNamen`, "het maximum van de romplengte van de Schuit").
   `keurNavigatiebindingen` keek naar de binding (geen relatie, meer relaties, een meervoudige stap), niet naar het lid.
   Daardoor ging ook een genavigeerde lezing met pad stil naar schijf: "de toeslag van zijn jutter" schrijft `path`
   over `jutter`, `subjectType: "Jutter"`, `attribute: "toeslag"`, en de verifier weigert dat met WF-034.
3. Gevonden in de vormruimte, dezelfde stilte in andere vormen: een lezing die het subtype bij zijn typenaam noemt over
   een rol die het supertype vult ("de toeslag van de Strandjutter van de Vondst": pad over `jutter`, eindpunt
   `Strandjutter`, WF-053); een kenmerk als waarde over een rol (WF-080); een meervoudige stap in de opgenomen keten
   ("de bonus van de vondst van de Jutter"); een rol- of typenaam in de naam die vanaf de stand geen enkele relatie
   bindt (een rol van een ander feittype, of een rol die zo heet als haar vuller terwijl twee relaties de typen
   verbinden: de ketenwandeling leest de naam als het type); een consistentie vanaf het onbepaalde onderwerp ("De premie
   van de jutter van een Vondst moet groter zijn dan 2": de ketenwandeling vraagt een bepaald zelfstandig naamwoord en
   loopt niet); en een onderwerp dat geen rol en geen type noemt ("de premie van zijn visser": een lezing zonder
   `subjectType`). Elk ging met `diagnostics: []` naar schijf en de verifier weigerde het.

### Fix en de gekozen weg

`RuleChecker` (+179, −10 regels, commentaar inbegrepen) en `RegelSpraakModel` (+9, −2); de bouwer, de emitter en de
binding van wat bindt zijn niet aangeraakt.

- `RuleChecker.lidFout`, aangeroepen in de wandeling van `keurNavigatiebindingen` na de twee bindingscontroles, met het
  onderwerp waarvan de emitter elke positie schrijft (regelwaarde en voorwaarde, dagsoort, creatie, verdeling,
  consistentie, feitcreatie, de defaults na de lift, het filter van een vouw en van een kwantorvouw met het element als
  onderwerp). Een venstergrens krijgt alleen het eigen-lezingdeel (`venstergrensLid`): een grens die navigeert is de
  weigering van de venstercontrole (WF-111), één defect houdt één weigering. De controle begint waar de lezing staat (haar
  type, of het type dat haar pad bereikt) en loopt de rollen die haar naam opnam naar buiten af, zoals de ketenwandeling
  van de bouwer, met `RegelSpraakModel.geschrevenHop` (de hop van `ketenHop` zonder het filter op meervoud; `ketenHop`
  roept haar nu aan). Zij weigert alleen waar minstens één rol bereikt is:
  - het lid staat niet op de vuller van de laatste rol: de Nederlandse zin noemt lid, vuller en rol en waar het lid wel
    staat; op precies één subtype van de vuller volgt de zin van de verifier voor de navigatie naar dat subtype (WF-053,
    `the navigation reaches 'Jutter', not the endpoint 'Strandjutter'`), anders (meer subtypen, een ander objecttype,
    nergens) die voor de lezing op de vuller (WF-034, `'rang' is neither an attribute nor a characteristic of 'Jutter'`);
  - de lezing noemt een strikt subtype van de vuller als eindpunt (`… noemt het subtype 'Strandjutter', maar de rol
    'jutter' komt uit op 'Jutter': the navigation reaches …`, WF-053), ook als het lid op de vuller staat;
  - een kenmerk als waarde (WF-080); een meervoudige stap (`meervoudigeStapZin`, WF-053, nu gedeeld met
    `meervoudigeStapFout`);
  - een rol- of typenaam die vanaf de stand geen enkele relatie bindt (`stapReden`: de typelezing met het aantal
    relaties, of de rol die niet precies één relatie aanwijst) en een keten waarvan het lid aan het eind wel staat maar
    die de bouwer niet legde: WF-034 voor het document dat de frontend zou schrijven.
  - Daarnaast: een onderwerp dat geen rol en geen type noemt, in WF-034's zin voor de verwijzing zonder `subjectType`.
- Een gedeclareerd attribuut met 'van' in de naam wint: `t.attribuut(naam) != null` laat de lezing staan, of de navigatie
  ook zou binden of niet; geen diagnose (zie open punt 2).
- Een eigen lezing zonder rol- of typenaam in de naam blijft aan de verifier: de casus van de pariteitszone leest zulke
  namen, die alleen haar replay-slices declareren.
- Elke Engelse zin staat letterlijk in de verifier (`LezerszinGrensTest` groen); elke Nederlandse zin noemt de lezing
  zoals de auteur haar schreef.

Verworpen: de herlezing laten binden waar de keten wel kan (de consistentie vanaf "een Vondst", de rol die zo heet als
haar vuller over twee relaties): dat is een wijziging van de bouwer en een eigen kaart (open punt 3), de kaart vraagt
een weigering; elke ongedeclareerde eigen lezing weigeren: de pariteitszone schrijft dan niets meer (gate 11); een
WARNING in plaats van een ERROR: die laat het document op schijf, en elke uitvoerder weigert het daarna toch.

### (a) Vormruimte, vóór en na

168 gegenereerde documenten: zeven plaatsen van het lid × twee afstanden × drie spellingen × vier posities. Model:
Jutter (premie), Strandjutter is een Jutter (toeslag), Haven (havengeld), Berging (bergloon), Vondst (bonus, gewicht);
rollen `jutter` (Jutter) en `melder` (Strandjutter) vanaf de Vondst, `berging` vanaf de Vondst, `beheerder` (Jutter)
en `wachter` (Strandjutter) vanaf de Berging. Situatie: J (premie 5), S een Strandjutter (premie 4, toeslag 3), B met
beheerder J en wachter S, V (gewicht 4) en W (gewicht 3), beide met jutter J, melder S, in B. Posities: de waarde ("De
bonus van een Vondst moet berekend worden als <lezing> plus 1."), de voorwaarde ("… gesteld worden op 1 indien <lezing>
groter is dan 2."), de rechtvaardiging van een default (de voorwaarde van "Uitzondering op bonus: hoge bonus"), de
subselectie van een vouw ("Het bergloon van een Berging moet berekend worden als de som van het gewicht van alle
geborgen vondsten van de Berging die aan de volgende voorwaarde voldoen: <lezing> is groter dan 2."). Spellingen: lang
"de X van de jutter van de Vondst" / "de X van de beheerder van de berging van de Vondst", bezit "de X van zijn jutter" /
"de X van de beheerder van zijn berging", bepaald "de X van de jutter" / "de X van de beheerder van de berging". In
elke groep van (plaats, afstand, spelling) is de uitkomst in alle vier posities gelijk (alleen het regelnummer van de
ERROR verschilt), dus de tabel heeft één rij per groep. SPEELT = check exit 0, verifier `PASS`, ref en clingo `PASS`
op alle cases.

| plaats van het lid | afstand | spelling | vóór (4 posities) | na (4 posities) |
|---|---|---|---|---|
| op de rolvuller (`premie`) | 1 en 2 | lang, bezit, bepaald | SPEELT | SPEELT (24 van 24) |
| gedeclareerd attribuut met 'van' (`de premie van de jutter` op de Vondst; bij 2 `de premie van de beheerder` op de Berging) | 1 en 2 | lang leest de declaratie, bezit en bepaald bij 1 lezen de premie van de Jutter, bij 2 lezen alle drie de declaratie | SPEELT | SPEELT (24 van 24) |
| op een supertype van de rolvuller (`premie` over `melder`/`wachter`, gevuld door Strandjutter) | 1 en 2 | lang, bezit, bepaald | SPEELT | SPEELT (24 van 24) |
| op een subtype (`toeslag`) | 1 | lang | STIL: `'toeslag van de jutter' is neither an attribute nor a characteristic of 'Vondst'` | `het lid 'toeslag' bestaat niet op 'Jutter', de vuller van de rol 'jutter', wel op het subtype 'Strandjutter': the navigation reaches 'Jutter', not the endpoint 'Strandjutter'` |
| idem | 1 | bezit, bepaald | STIL: `'toeslag' is neither … of 'Jutter'` | idem |
| idem | 2 | lang | STIL: `'toeslag van de beheerder van de berging' is neither … of 'Vondst'` | idem met `de vuller van de rol 'beheerder'` |
| idem | 2 | bezit, bepaald | STIL: `'toeslag van de beheerder' is neither … of 'Berging'` | idem met `'beheerder'` |
| nergens (`rang`) | 1 en 2 | de drie | STIL (dezelfde vier zinnen met `rang`) | `het lid 'rang' bestaat niet op 'Jutter', de vuller van de rol 'jutter'` (bij 2 `'beheerder'`) `, en op geen ander objecttype: 'rang' is neither an attribute nor a characteristic of 'Jutter'` |
| op een ander objecttype (`havengeld`, op Haven) | 1 en 2 | de drie | STIL (idem met `havengeld`) | `… het lid 'havengeld' bestaat niet op 'Jutter', de vuller van de rol …, wel op 'Haven', dat geen subtype van 'Jutter' is: 'havengeld' is neither an attribute nor a characteristic of 'Jutter'` |
| een rolnaam die geen rol is (`visser`) | 1 | lang | STIL: `'premie van de visser' is neither … of 'Vondst'` | STIL, ongewijzigd (blijft aan de verifier) |
| idem | 1 | bezit, bepaald | STIL: `attribute reference 'premie' without a subjectType: it resolves against no declaration` | `lezing 'de premie van zijn visser': 'zijn visser' noemt vanaf 'Vondst' geen rol en geen objecttype: attribute reference 'premie' without a subjectType: it resolves against no declaration` |
| idem, de tweede rol fout | 2 | lang, bezit, bepaald | STIL: `'premie van de visser van de berging' … 'Vondst'` (lang), `'premie van de visser' … 'Berging'` | `het lid 'premie van de visser' bestaat niet op 'Berging', de vuller van de rol 'berging', en op geen ander objecttype: 'premie van de visser' is neither … of 'Berging'` |

Telling: vóór 72 SPEELT en 96 STIL; na 72 SPEELT (dezelfde regels, `diff` leeg), 92 geweigerd en 4 STIL (de verkeerde
rolnaam één rol ver, voluit geschreven). Elk STIL-document werd door de verifier geweigerd en door ref en clingo met een
fout beantwoord.

Flanken, één document per vorm, één positie (de waarde, tenzij anders genoemd):

| vorm | vóór | na |
|---|---|---|
| f01 navigatie naar een kenmerk dat op het subtype staat ("indien de jutter van de Vondst geschorst is") | marker met WARNING, ref/clingo `Unresolved condition reached …` | idem (geen lezing) |
| f01b idem, kenmerk op de rolvuller | SPEELT | SPEELT |
| f02/f02b vouwbron die een rol opnam ("de som van de premie/toeslag van de jutter van alle geborgen vondsten van de Berging") | marker met WARNING, ref/clingo `Unresolved reached …` | idem |
| f03 vouwbron, element zonder lid ("de som van de rang van alle geborgen vondsten …") | idem | idem |
| f04 lid op twee subtypen (Strandjutter en Wadjutter) | STIL | `… wel op de subtypen 'Strandjutter' en 'Wadjutter': 'toeslag' is neither … of 'Jutter'` |
| f05 lid op een subtype van een subtype (Kustjutter is een Strandjutter) | STIL | `… wel op het subtype 'Kustjutter': the navigation reaches 'Jutter', not the endpoint 'Kustjutter'` |
| f06 bij typenaam, lid op het subtype ("de toeslag van de Jutter") | STIL | `… wel op het subtype 'Strandjutter': the navigation reaches …` |
| f06b bij typenaam, bindend | SPEELT | SPEELT |
| f07 consistentie vanaf "een Vondst", bindend lid | STIL: `'premie van de jutter' … 'Vondst'` | `genavigeerde lezing 'De premie van de jutter van een Vondst' is niet als navigatie gelegd: 'premie' staat op 'Jutter', de vuller van de rol 'jutter', maar zij is gelezen als het attribuut 'premie van de jutter' van 'Vondst': 'premie van de jutter' is neither … of 'Vondst'` |
| f07b idem, lid op het subtype | STIL | `… wel op het subtype 'Strandjutter': the navigation reaches …` |
| f07c consistentie "De toeslag van zijn jutter moet …" (onderwerp wordt de Jutter, eigen lezing) | STIL: `'toeslag' … 'Jutter'` | STIL, ongewijzigd (eigen lezing, open punt 1) |
| f08 toekenning van een objectcreatie, lid op het subtype | STIL | `… wel op het subtype 'Strandjutter': …` |
| f08b idem, bindend | SPEELT | SPEELT |
| f09 eigen lezing via een rol die het onderwerp zelf speelt ("de rang van de aanmelder") | STIL | STIL, ongewijzigd |
| f10 meervoudige stap ("de bonus van de vondst van de Jutter") | STIL: `'bonus van de vondst' … 'Jutter'` | `genavigeerde lezing 'de bonus van de vondst van de Jutter' over een meervoudige rol: hop 0 reaches the to-many role 'vondst' in a scalar slot — …` |
| f11 meerwoordig lid met 'van' op de vuller ("de datum van keuring van de jutter van de Vondst") | SPEELT | SPEELT |
| f11b alleen het eerste woord ("de datum van de jutter van de Vondst") | STIL | `het lid 'datum' bestaat niet op 'Jutter', …, en op geen ander objecttype: …` |
| f12/f12b eigen lezing zonder rol in de naam ("de datum van aanmelding van de Vondst", "de rang van de Vondst") | STIL | STIL, ongewijzigd |
| f13 eigen lezing van een lid van een subtype van het onderwerp | STIL | STIL, ongewijzigd (open punt 1) |
| f14/f14b kenmerk als waarde over een rol (lang, bezit) | STIL | `… het lid 'geschorst' van 'Jutter', de vuller van de rol 'jutter', is een kenmerk en geen attribuut: 'geschorst' is a characteristic of 'Jutter', not an attribute — …` |
| f14c kenmerk als waarde, eigen lezing | STIL (WF-080) | STIL, ongewijzigd (open punt 1) |
| f15 venstergrens, eigen lezing die een rol opnam, lid nergens | STIL | `… het lid 'vinddatum' bestaat niet op 'Jutter', …: 'vinddatum' is neither … of 'Jutter'` |
| f15b/f15c venstergrens die navigeert | ERROR van de venstercontrole (WF-111) | idem, geen tweede weigering |
| f16 lid op een zustertype (rol `melder` gevuld door Strandjutter, lid op Wadjutter) | STIL | `… wel op 'Wadjutter', dat geen subtype van 'Strandjutter' is: …` |
| f17 subtype bij typenaam over een rol van het supertype (lang en bepaald, één relatie) | STIL: `the navigation reaches 'Jutter', not the endpoint 'Strandjutter'` | `genavigeerde lezing 'de toeslag van de Strandjutter van de Vondst' noemt het subtype 'Strandjutter', maar de rol 'jutter' komt uit op 'Jutter': the navigation reaches …` |
| f17 idem met twee relaties naar een Strandjutter (`jutter` en `melder`), lid op het subtype of op de Jutter | STIL: `'toeslag van de Strandjutter' … 'Vondst'` | `… bindt niet ('Strandjutter' leest als het objecttype 'Strandjutter', en 2 relaties verbinden 'Vondst' met 'Strandjutter'), dus zij is gelezen als het attribuut …: … is neither … of 'Vondst'` |
| f18 twee relaties Vondst–Jutter (`jutter`, `vinder`), lang | STIL: `'premie van de jutter' … 'Vondst'` | `… bindt niet ('jutter' leest als het objecttype 'Jutter', en 2 relaties verbinden 'Vondst' met 'Jutter'), …` (open punt 3) |
| f18 idem, bezit en bepaald, en "de premie van de vinder van de Vondst" | SPEELT | SPEELT |

### (b) Getuige

`OngebondenLezingTest` (frontend, 17 tests, Jutterswet-model in de klasse): `r1_het_lid_staat_op_een_subtype_van_de_rolvuller`,
`r2_het_lid_bestaat_nergens`, `elke_positie_eindigt_in_een_diagnose` (voorwaarde, rechtvaardiging van een default,
subselectie, in twee spellingen), `een_rol_ver_in_drie_spellingen` (en bij typenaam), `twee_rollen_ver_in_drie_spellingen`,
`het_lid_staat_op_een_ander_objecttype`, `het_lid_staat_op_twee_subtypen_of_op_een_subtype_verder` (en het zustertype),
`het_subtype_bij_naam_over_een_rol_van_het_supertype`, `een_verkeerde_rolnaam`, `een_meervoudige_rol_in_de_naam`,
`een_keten_vanaf_een_onbepaald_onderwerp_wordt_niet_gelegd`, `een_stap_die_geen_relatie_bindt`,
`een_kenmerk_als_waarde_over_een_rol`, `een_venstergrens_die_een_rol_in_haar_naam_draagt`, en drie flankrijen die op de
claim al groen waren en het moeten blijven: `de_bindende_lezing_blijft_schoon`,
`een_gedeclareerd_attribuut_met_van_in_de_naam_wint_van_de_navigatie` (de IR leest `premie van de jutter` op de Vondst,
zonder navigatie) en `een_eigen_lezing_zonder_rol_in_de_naam_blijft_aan_de_verifier`.

Rood op de claim (`2e6c75a3`, de eerste vijftien rijen, tegen de code van `8d0de735`): `1023 tests found`,
`1011 tests successful`, `12 tests failed`, elk met `but was: <[]>`, bijvoorbeeld

    r1_het_lid_staat_op_een_subtype_van_de_rolvuller() ✘ expected: <[genavigeerde lezing 'de toeslag van de jutter
    van de Vondst': het lid 'toeslag' bestaat niet op 'Jutter', de vuller van de rol 'jutter', wel op het subtype
    'Strandjutter': the navigation reaches 'Jutter', not the endpoint 'Strandjutter']> but was: <[]>
    r2_het_lid_bestaat_nergens() ✘ expected: <[genavigeerde lezing 'de rang van de jutter van de Vondst': het lid
    'rang' bestaat niet op 'Jutter', de vuller van de rol 'jutter', en op geen ander objecttype: 'rang' is neither an
    attribute nor a characteristic of 'Jutter']> but was: <[]>

De twee rijen die in `7b8f0f23` bijkwamen (`het_subtype_bij_naam_over_een_rol_van_het_supertype`,
`een_stap_die_geen_relatie_bindt`) zijn met de klasse van de kop tegen de klassen van de claim gedraaid: 17 tests, 14
rood (`but was: <[]>`), de drie flankrijen groen. Na de fix: de klasse 17 tests, 0 failures; de suite 1025 tests, 0
failures (1008 op de claim).

### (c) Negatief-cases `jutterswet-ongebonden-lezing` (`024871c2`)

| map | document | verifier | `expect.txt` |
|---|---|---|---|
| `corpus/negatief/wf053-jutterswet-ongebonden-lezing-subtype` | R1 zoals de auteur haar schreef: `attribute: "toeslag"`, `subjectType: "Strandjutter"`, één binding over `JutterHeeftVondst`/`jutter` | `WF-053 FAIL /rules/rules/0/versions/0/value/left: the navigation reaches 'Jutter', not the endpoint 'Strandjutter'` | `the navigation reaches 'Jutter', not the endpoint 'Strandjutter'` |
| `corpus/negatief/wf034-jutterswet-ongebonden-lezing-nergens` | R2: `attribute: "rang"`, `subjectType: "Jutter"`, dezelfde binding | `WF-034 FAIL /rules/rules/0/versions/0/value/left: 'rang' is neither an attribute nor a characteristic of 'Jutter'` | `'rang' is neither an attribute nor a characteristic of 'Jutter'` |

Elk document is de emissie van de frontend van hetzelfde model met de bindende lezing "de premie van de jutter van de
Vondst" (verifier `PASS`), waarin alleen `attribute`, `subjectType` en de snippets van de lezing zijn veranderd; elk
weigert op één regel, de arm waarvan de frontend de Engelse zin spreekt. NOTE.md per map volgens de conventie van
`corpus/negatief/`. Gate 4 telt 503 negatief (501 bij de claim), 346 op hun arm (344), 607 aangehaalde pointers; gate 2
houdt beide documenten in de sleutelvolgorde van het schema (57 objecten meer, de ene "in no subschema" stond er al).

### Pinvoorstellen

Geen. Gate 4 (`scripts/check-verifier.sh`): `EXPECTED_MIN_NEGATIEF=350`, `EXPECTED_MIN_EXPECT=260`,
`EXPECTED_MIN_POINTERS=547` zijn ondergrenzen; gemeten 503, 346, 607, groei vraagt geen ophoging. Gate 10
(`scripts/check-regelspraak.sh`): `EXPECTED_MIN_TESTS=173`, `EXPECTED_MIN_GOLDENS=80`; gemeten 1025 tests en 116
rgs-goldens. Geen beschermd script is aangeraakt.

### (d) Sweep

- Alle 116 rgs-goldens worden byte-gelijk opnieuw uitgegeven (gate 10, en vooraf met de klassen van vóór en na
  afzonderlijk gemeten); de 23 scenario-goldens leveren hun 82 casebestanden en hun ir.json byte-gelijk (gate 16); de
  casus van de pariteitszone (45 fragmenten) wordt byte-gelijk uitgegeven en de TOKA-suite draait 27 tests groen
  (gate 11); de diagnose zit in de IR, dus ook geen golden kreeg een nieuwe WARNING.
- Alle 501 bestaande negatief-cases weigeren op hun regel en, waar `expect.txt` staat, op hun arm (gate 4); de verifier
  is niet veranderd.
- Geen golden en geen rood-document draagt zo'n lezing: over de 329 ir.json van `corpus/golden` en `corpus/rood` leest
  geen enkele `AttributeReference` een lid dat haar type (met supertypen) niet declareert, de 63 telbronnen met een
  rolnaam (WF-093) niet meegeteld (`ongedeclareerd2.py`). Een golden die er een droeg, faalde nu al: ref en clingo
  weigeren het document van vóór van R1 en R2 (`EVALUATION FAULT` / `TRANSLATION FAULT`, tabel boven), dus gate 5 en 6
  zouden rood zijn.
- De binding verschuift niet: in de vormruimte en de flanken speelt elke vorm die vóór speelde ook na, met dezelfde
  statusregel.

### DIVERGENCES (`8a90d457`)

`frontends/regelspraak/grammar/DIVERGENCES.md`, de sectie heet nu "An undeclared attribute read — refused where the read
names a role or a type". Zij zegt wat de resolver weigert, in drie vormen met de zin van de verifier na een Nederlands
deel: (1) de naam nam een woord vóór 'van' op (zoals vóór deze kaart); (2) de lezing bereikt een rol, via haar pad of
via een rol- of typenaam achter een 'van', en het lid staat niet op de vuller van de laatste rol (WF-053 bij één
subtype, anders WF-034), met in dezelfde wandeling het subtype bij typenaam, het kenmerk als waarde, de meervoudige
stap, de stap die geen relatie bindt en de keten die niet gelegd is; (3) een onderwerp dat geen rol en geen type
noemt. Verder: een gedeclareerd attribuut met 'van' in de naam wint zonder diagnose; wat aan de verifier blijft (de
eigen lezing zonder rol- of typenaam, de rol die het onderwerp zelf speelt, het lid van een subtype van het onderwerp,
het kenmerk als eigen waarde) en waarom (de pariteitszone); dat een vouwbron een marker met WARNING blijft; en de meting.
De lijst in "Producer refusals without a reader sentence" noemt de nieuwe weigeringen en de getuige
`OngebondenLezingTest`.

### Gates

Alle zestien in de volgorde van `gates.yml` met `/tmp/v2/uchar-gates16/run.sh` op de kop `8a90d457` (de laatste commit
vóór dit Result; de commit met dit Result raakt alleen de kaart), met een schone werkboom: `eval $(opam env)`,
`dune build` (exit 0), dan elk script onder zijn shebang. Logs in `/tmp/v2/uchar-gates16/`. Per gate de exitcode en de
PASS-regel:

     1. check-self-contained: exit 0 — check-self-contained: PASS
     2. check-ir-schema: exit 0 — check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 329 file(s) validate and round-trip, floor 192; 43440 object(s) in 832 document(s) in the schema's key order, 1 in no subschema)
     3. check-case-schema: exit 0 — check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 825 case file(s) validate)
     4. check-verifier: exit 0 — check-verifier: PASS (329 golden clean; 503 negatief rejected for their declared rule, floor 350; 346 on their declared arm (expect.txt), floor 260; 607 cited pointers resolve in their documents, floor 547)
     5. check-oracle: exit 0 — check-oracle: PASS (ref suite 1152 worked examples, floor 489; 783 cases replayed exactly, floor 427; NOT replayed, no case file yet: …)
     6. check-clingo: exit 0 — check-clingo: PASS (backend suite 1988 checks, floor 83; 783 cases replay exactly through clingo — ref = clingo over the corpus)
     7. check-noop: exit 0 — check-noop: PASS (739 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
     8. check-rood: exit 0 — check-rood: PASS (42 red cases fault observably in ref and clingo, floor 26; 84 pinned fragments cite none of the 36 listed bottom sentences)
     9. check-catala: exit 0 — check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    10. check-regelspraak: exit 0 — check-regelspraak: PASS — JUnit suite green (1025 tests, floor 173); 116 rgs golden(s) re-emit byte-identically (floor 80)
    11. check-toka-parity: exit 0 — check-toka-parity: PASS
    12. check-drools: exit 0 — check-drools: PASS
    13. check-reftype-derivation: exit 0 — check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    14. check-flogic: exit 0 — check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    15. check-graaf: exit 0 — check-graaf: PASS
    16. check-scenario: exit 0 — check-scenario: PASS — 23 scenario golden(s) (floor 7) regenerate 82 case file(s) (floor 31) and their ir.json byte-identically

Gate 12 verder: `played 234`, `auto-excluded 52`, `channel floor 55 non-PLAYED goldens pinned by name; played matches the
pinned 234`, `DRL-text branch played 192 (… pin 192)`, `JUnit 985 tests, floor 239`. Gate 11:
`re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (…); TOKA suite 27 tests, floor 27`. Gate 15:
`route leg — the graph of the wire equals the graph of the source on all 116 goldens with a bron.rgs (floor 98); 23 unit
tests green (floor 23)` en `spec leg — 252 of 289 goldens compared, 183 dependencies equal to the verifier's ch. 5 edges
(floors 234 / 116), 3 listed difference(s)`.

### Afwijkingen van de kaart

1. De weigering dekt meer dan de spelling van R1 en R2. Ook de bezittelijke en de bepaalde spelling ("de toeslag van
   zijn jutter", "de toeslag van de jutter") en de lezing bij typenaam waren stil; zij gingen als genavigeerde lezing met
   pad naar schijf, niet als ongedeclareerd attribuut van het onderwerp. Dezelfde controle weigert ze, in dezelfde zin.
2. Zes vormen buiten "het lid staat niet op de rolvuller" weigeren nu ook, omdat zij in dezelfde wandeling vallen en
   allemaal met `diagnostics: []` naar schijf gingen terwijl elke lezer ze weigerde: het subtype bij typenaam over een
   rol van het supertype (ook waar het lid op het supertype staat), een kenmerk als waarde over een rol, een meervoudige
   stap in de opgenomen keten, een rol- of typenaam die geen enkele relatie bindt, een keten die de bouwer niet legde
   (consistentie vanaf een onbepaald onderwerp, en de rol die zo heet als haar vuller over twee relaties), en een
   onderwerp dat geen rol en geen type noemt. Geen golden en geen bron van de pariteitszone draagt een van deze vormen
   (sweep).
3. Een venstergrens die een eigen lezing is, wordt ook gekeurd; een grens die navigeert blijft de weigering van de
   venstercontrole.
4. "Dan wint de declaratie en zegt het rapport dat" is gelezen als: dit Result en DIVERGENCES zeggen het, en de getuige
   pint het; de frontend geeft er geen diagnose bij (open punt 2).
5. Geen pin verschuift; er is geen voorstel voor gate 4 of 10.
6. De getuige heeft meer rijen dan R1 en R2: de vormen van (a) en drie flankrijen.
7. `RegelSpraakModel.ketenHop` is opgesplitst in `geschrevenHop` en het meervoudfilter; haar gedrag is gelijk (gate 10,
   11, 16 byte-gelijk). Geen bestaand testbestand en geen bestaande golden gewijzigd.

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/IR-SPEC.md` ch. 4.1a, de bullet **WF-053**, na "(the endpoint; one-sided, since the executors refuse the other
  direction per instance)": "— a role whose declared filler is a strict supertype of the endpoint is refused
  (`wf053-jutterswet-ongebonden-lezing-subtype`: the role `jutter` filled by `Jutter`, the endpoint its subtype
  `Strandjutter`, which alone declares the member read)".
- Geen tekst voor `spec/CONSTRUCTS.md`: de wire verandert niet; dit is een weigering aan de kant van de producent.

### Open punten

1. Wat aan de verifier blijft, zolang de pariteitszone bestaat: de eigen lezing van een ongedeclareerde naam zonder rol-
   of typenaam ("de rang van de Vondst", "de premie van de visser van de Vondst" waar 'visser' geen rol is), de eigen
   lezing via een rol die het onderwerp zelf speelt, de eigen lezing van een lid dat alleen een subtype van het onderwerp
   declareert (f13, f07c) en het kenmerk als eigen waarde (f14c). Gemeten: de vier ongedeclareerde eigen lezingen in de
   casus van de pariteitszone worden in die casus door geen enkel objecttype gedeclareerd, dus een arm "eigen lezing van
   een lid dat een ander type declareert" (f13, f07c, f14c) zou de zone niet raken. Kandidaat voor een kaart, als de
   eigenaar dat wil vóór de zone vertrekt.
2. Een gedeclareerd attribuut met 'van' in de naam wint zonder melding, ook waar de navigatie ook zou binden. Gemeten:
   van de 13 gedeclareerde attributen met ' van ' in 289 goldens en de casus van de pariteitszone is er geen dat ook als
   navigatie leesbaar is, dus een WARNING bij die dubbele lezing verschuift geen gepind document. Beslissing bij de
   eigenaar (vraag).
3. Binden waar de frontend nu weigert: de consistentie vanaf het onbepaalde onderwerp (f07) en de lange spelling van een
   rol die zo heet als haar vuller terwijl twee relaties de typen verbinden (f18; de bezittelijke spelling bindt wel, via
   §13.4.1 geval 2). Beide zitten in de ketenwandeling van `ExpressieBouwer.attribuutRef` (zij vraagt een bepaald
   naamwoord en leest een typenaam als type); een eigen kaart.
4. Een vouwbron met een ongedeclareerd lid of een opgenomen rol blijft een marker met WARNING (f02, f02b, f03); ref en
   clingo weigeren hem luid (`Unresolved …`). Niet stil, maar `check` weigert hem niet.
5. Niet gemeten: een tussenstap bij typenaam naar een subtype ("… van de Strandberging van de Vondst"); de controle kijkt
   naar het eindpunt, de verifier zou de nabije zijde van de volgende hop weigeren.
6. Voor de distributie (buiten deze kaart, uit de kaart): `bin/regelspraak check` zou als tweede lijn de verifier kunnen
   draaien; de submodulepin moet na het landen mee om deze weigering te zien.

### Commits

    8d0de735 claim: frontend-ongebonden-lezing-check
    2e6c75a3 OngebondenLezingTest: lezing zonder lid op de rolvuller, rood
    7b8f0f23 Resolver: een lezing waarvan het lid niet op de rolvuller staat weigert
    024871c2 negatief jutterswet-ongebonden-lezing: R1 op WF-053, R2 op WF-034
    8a90d457 DIVERGENCES: wat de frontend aan een ongedeclareerde lezing weigert

en de commit die de kaart met dit Result naar `backlog/done/` verplaatst. Niet gepusht, niet gemerged.
