# overlappende-voorwaardelijke-schrijvers: twee voorwaardelijke schrijvers van één doel die beide gelden, drie antwoorden

*Vondst van de bouwer van opgave-markering-schrijver-op-subtype (2026-09-30,
open punt 5, gemeten als schrijvers op een supertype en een subtype), door de
hoofdsessie teruggebracht tot één type en gemeten op uchar `6b7ab8a2` via de
distributie (`bin/regelspraak check|scenario|cases`) en de eigen uitvoerders;
reproductie op Jutterswet-materiaal in `/tmp/v2/probe/erflijn-schrijvers/`
(bron-een-type.rgs, scenario-een-type.rgs, cases-een-type/; bron.rgs en
scenario.rgs voor de erflijnvorm; alleen lezen). De verifier zwijgt; ref
en de exec-tak falen luid, de exec-tak met een verkeerde naam; clingo geeft
stil twee waarden; de DRL-teksttak speelt. Op akkoord van de eigenaar
geschreven (2026-09-30).*

## Aanleiding

Twee gewone voorwaardelijke regels die hetzelfde attribuut schrijven en
waarvan de voorwaarden op één instantie beide waar zijn, passeren de
verifier: WF-079 ("A (type, attribute) target that carries no defaults MUST
NOT be written by two unconditional computing versions of different forms")
dekt alleen onvoorwaardelijke schrijvers, en niets anders bewaakt de
overlap. Het document is dus welgevormd en de uitvoerders moeten het
beantwoorden. Zij antwoorden verschillend:

- `ref/` faalt luid: "EVALUATION FAULT: no fixpoint within 100 passes —
  divergence is a fault (ch. 5.7)". De twee schrijvers wisselen elkaar
  per pas af.
- De Drools-exec-tak faalt ook luid, maar noemt een oorzaak die er niet
  is: "geen fixpunt (kringverwijzing?)".
- De ASP-backend antwoordt met twee waarden voor één attribuut en geen
  `viol/2`-getuige: "MISMATCH: V.meldingsdatum = [20513;20516], expected
  20513" komt alleen van de verwachting van de case; zonder verwachting
  is het een stil dubbel antwoord, waar `backends/clingo/README.md` juist
  belooft dat elke divergentie een benoemde `viol/2`-getuige krijgt en
  nooit een tweede model.
- De DRL-teksttak speelt het document en kiest stil één van beide
  (bouwmeting p05u van de kaart opgave-markering-schrijver-op-subtype:
  `drools-drl: exit 0 — PLAYED 2`).

De erflijn is niet de oorzaak: dezelfde twee regels op één type gedragen
zich precies zo. IR-SPEC ch. 6 zegt dat de enkele-schrijverregels op de
effectieve stapel per type worden beoordeeld en "must ask whether two
writers survive into one instance's effective stack"; voor voorwaardelijke
schrijvers is die vraag nog nergens gesteld, noch statisch, noch bij de
uitvoering.

## Minimale reproductie

`6b7ab8a2`, distributie `bin/regelspraak check|scenario|cases`; ref via
`_build/default/ref/bin/runner.exe`, clingo via
`_build/default/backends/clingo/bin/clingo_runner.exe`.

    Objecttype de Vondst (mv: Vondsten)
        de vinddatum Datum in dagen;
        de aanspoeldatum Datum in dagen;
        de meldingsdatum Datum in dagen;

    Regel melding op vinddag
        geldig altijd
            De meldingsdatum van een Vondst moet gesteld worden op zijn vinddatum
            indien zijn vinddatum gevuld is.

    Regel melding op aanspoeldag
        geldig altijd
            De meldingsdatum van een Vondst moet gesteld worden op zijn aanspoeldatum
            indien zijn aanspoeldatum gevuld is.

    Scenario: een Vondst V met vinddatum 04-03-2026 en aanspoeldatum 01-03-2026; de
    verwachting (meldingsdatum 01-03-2026) dient alleen om de uitvoerders te laten spreken.

    R1  bron-een-type.rgs, zoals hierboven.
        nu: check exit 0, 0 ERROR, 0 WARNING (resolved=2); scenario (exec) FAIL "geen fixpunt
        (kringverwijzing?)"; ref "EVALUATION FAULT: no fixpoint within 100 passes — divergence is
        a fault (ch. 5.7)"; clingo "MISMATCH: V.meldingsdatum = [20513;20516], expected 20513".
    R2  bron.rgs: dezelfde twee regels, de tweede op een subtype Strandvondst (aanspoeldatum op het
        subtype), scenario op een Strandvondst met beide datums.
        nu: identiek aan R1 in check, exec, ref en clingo.
    R3  R2 met alleen de vinddatum, of alleen de aanspoeldatum, gevuld.
        nu: PASS in alle drie (bouwmeting p05u case1: ref en clingo PASS, exec speelt).
    R4  dezelfde twee regels als `Regel` en `Afwijking van` erop (één boom).
        nu: exit 0 en één antwoord (de vorm van DIVERGENCES §4; de kaart bouwt hier niets).

## Acceptatie

- (a) Meet de vormruimte in de vier uitvoerders (ref, clingo, Drools-exec,
  DRL-tekst): twee en drie voorwaardelijke schrijvers op één type; op
  supertype en subtype; een gelijkstelling naast een berekening; een
  kenmerktoekenning naast een tweede; met een derde regel die het doel
  leest; met de opgave-markering op het doel; en de disjuncte vorm (de
  voorwaarden sluiten elkaar uit) als de vorm die moet blijven spelen.
- (b) Eén gedrag, vastgelegd in de spec: de standaardkeuze is de luide
  fout van `ref/` (divergentie, ch. 5.7), met een melding die de twee
  schrijvers en het doel noemt en geen kring; de ASP-backend leidt voor
  dezelfde body een benoemde `viol/2`-getuige af en geeft nooit twee
  waarden; de DRL-teksttak volgens haar kanaal. Het andere antwoord, als
  normkeuze van de eigenaar: de later geschreven regel wint, zoals ch. 7.10
  dat voor defaults zegt — dan is het een spec-wijziging in
  `RUNTIME-SEMANTICS.md` ch. 5 en een regel in alle uitvoerders.
- (c) Rode-zone-case in `corpus/rood/` (werknaam `jutterswet-dubbele-melding`)
  die ref én clingo laat falen op de body, plus een groene tweeling met de
  disjuncte vorm als golden (`jutterswet-melding-naar-aard`); rood-eerst
  JUnit-getuige in de exec-tak voor de melding; de pins van gate 8
  (rood) en gate 12 (`EXPECTED_PLAYED` 226, `EXPECTED_DRL_PLAYED` 191) als
  **voorstel**.
- (d) Of, als de statische weg gekozen wordt: een WF-regel die overlap van
  voorwaardelijke schrijvers weigert waar de verifier de overlap kan zien
  (dezelfde voorwaarde, of geen voorwaarde tegenover een voorwaarde), met
  de dynamische fout als vangnet voor de rest; IR-SPEC ch. 6 bijwerken.
  Corpussweep ongewijzigd; alle zestien gates groen.

May touch: `ref/` (de melding), `backends/clingo/**`, `backends/drools/**`,
`corpus/rood/<nieuw>/`, `corpus/golden/<nieuw>/`, `scripts/check-*.sh`
(pins, **voorstel**), `spec/RUNTIME-SEMANTICS.md` en `spec/IR-SPEC.md` alleen
met de registertekst van de eigenaar, kaartverplaatsing. NB: serieel na de
kaarten drools-exec-datumvouw-over-rol en drools-exec-vouwfilter-latere-
afleiding; niet nodig vóór de v2-regelset verder gaat (die kent de vorm als
modelfout, en ref en de exec-tak vangen haar luid in de scenariotoetsen).

## Result (2026-10-01, bouwer)

Tak `card/overlappende-voorwaardelijke-schrijvers` in de worktree van de kaart, afgetakt van main `b784bd98`; niet
gerebased, niet gepusht, niet gemerged. Gemeten met de eigen uitvoerders van deze repo: `_build/default/ref/bin/runner.exe`,
`_build/default/backends/clingo/bin/clingo_runner.exe`, `_build/default/spec/verifier/bin/verifier.exe`, de frontend
(`Resolver --ir … --cases … bron.rgs scenario.rgs`) en de Drools-jar (`java -jar <jar> ir.json case…`, `--drl` voor de
teksttak). "Vóór" is de code van de claim `531cd452` (gelijk aan main): de jar van de claim, en ref en clingo zoals ze
vóór de eerste codecommit gebouwd waren (de drie late flankvormen q1, r1 en r2 met ref en clingo gebouwd uit `git archive`
van de claim); "na" is de code van `1292a0d0`, de laatste commit die een uitvoerder raakt. Elke vorm is een eigen
Jutterswet-document (bron.rgs + scenario.rgs), door een generator geschreven, door de frontend naar ir.json en cases
verlaagd en per case in de vier uitvoerders gemeten. Generator, meet- en sweepscripts staan buiten de repo
(`/tmp/v2/probe/ovs-bouw/maak.py`, `fe.sh`, `meet.sh`, `sweep.sh`, `tabel.py`; per vorm `meting-voor.txt` en
`meting-na.txt` onder `vorm/`). De reproducties zijn uit `/tmp/v2/probe/erflijn-schrijvers/` gekopieerd (alleen gelezen)
naar `repro/r1` en `repro/r2` en met de frontend van deze kop opnieuw verlaagd. De pinnen stonden bij de claim op
`EXPECTED_PLAYED=233` en `EXPECTED_DRL_PLAYED=191` (de kaart noemt 226/191, de stand van toen).

Normkeuze van de eigenaar: acceptatie (b), één gedrag, de luide fout van `ref/` (divergentie, ch. 5.7); niet "de later
geschreven regel wint". Een fout volgt waar twee schrijvers van één doel op één instantie gelden en verschillende waarden
schrijven; waar ze dezelfde waarde schrijven, gelden beide vergelijkingen (ch. 5.1) en speelt het document.

### De reproductie op deze kop

| | ref | clingo | exectak | DRL-tekst |
|---|---|---|---|---|
| R1 vóór | `EVALUATION FAULT: no fixpoint within 100 passes — divergence is a fault (ch. 5.7)` | `MISMATCH: V.meldingsdatum = [20513;20516], expected 20513` | `FAIL case1-…: geen fixpunt (kringverwijzing?)` | `PLAYED 1` (stil: de laatst gedeclareerde schrijver) |
| R1 na | `… (ch. 5.7): two writers of one target disagree: rule "melding op vinddag" writes 2026-03-04 and rule "melding op aanspoeldag" writes 2026-03-01 to the attribute meldingsdatum of V, a Vondst` | `UNSATISFIABLE — a fail-fast guard fired: viol("two writers of one target disagree: rules "melding op vinddag" and "melding op aanspoeldag" write different values to the attribute meldingsdatum of a Vondst — no fixpoint, divergence is a fault (ch. 5.7)","V")` | `FAIL case1-…: geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel "melding op vinddag" schrijft 2026-03-04 en de regel "melding op aanspoeldag" schrijft 2026-03-01 in het attribuut meldingsdatum van V, een Vondst; divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)` | dezelfde zin als de exectak |
| R2 vóór | als R1 | `MISMATCH: S.meldingsdatum = [20513;20516], expected 20513` | als R1 | `PLAYED 1` |
| R2 na | als R1, `… of S, a Strandvondst` | als R1, `… of a Strandvondst …","S")` | als R1, `… van S, een Strandvondst; …` | als de exectak |
| R3 (b1 case2/case3) | `PASS` vóór en na | `PASS` | `PLAYED 1` | `PLAYED 1` |
| R4 (i1) | `PASS` vóór en na | `PASS` | `PLAYED 1` | `EXCLUDE drl: ch. 7 prioritizedDefaults …` |

`check` (de verifier) zegt op R1 en R2 vóór en na `verifier: PASS`.

### Oorzaak

1. `ref/`: een pas draait de regels in documentvolgorde; elk van de twee overschrijft de ander, zodat elke pas een
   verandering meldt en het budget van 100 passen opraakt. De fout noemde alleen het budget, niet wat divergeerde.
2. De ASP-backend: elke berekenende regel is een eigen `val/3`-kop. Twee regels die op één subject gelden, leiden twee
   waarden in één stabiel model af, en niets bewaakt dat. Een schrijver waarvan de voorwaarde geldt en de waarde leeg
   uitrekent, leidt niets af (leeg is afwezigheid), zodat de waarde van de ander stil bleef staan waar ref faalt
   (h2-leeg-tegen-waarde, en f1 case 2, waar de opgave X beslist en `berekende X` twee waarden droeg). In de erflijnvorm
   met invoer op het doel (r1) is elke schrijver de enige van zijn eigen sleutel en dus staand (ch. 7.8); de
   invoerregel van de schrijver die niet vuurde liet de invoer staan naast de berekende waarde van de ander: twee
   waarden waar ref de invoer verdringt (gevonden in de vormruimte, van vóór deze kaart).
3. De exectak: de patronen zijn klasse-reactief, dus elke schrijfactie van de een laat de ander opnieuw vuren tot de
   vuurgrens van 100.000; `CaseRunner` meldde bij elke grens "geen fixpunt (kringverwijzing?)", ook waar geen kring is.
4. De DRL-teksttak: een regel kijkt (`@watch`) naar wat zijn voorwaarde en waarde lezen, niet naar zijn eigen doel. Elke
   schrijver vuurde één keer en de laatst gevuurde hield zijn waarde (gemeten: die van de laatst gedeclareerde regel).
   Dezelfde oorzaak gaf de hoek die `backends/drools/README.md` vastlegde: een voorwaardelijke initialisatie naast een
   berekening die leeg uitrekent (q1), waar de teksttak 25.00 gaf en de andere drie falen.

### Fix en de gekozen weg

- `ref/` (`lib/eval.ml` +110 −48, `lib/world.ml` +16): een pas is een lijst stappen (vorm en de naam van de schrijver),
  in dezelfde volgorde als voorheen. Raakt het budget op, dan speelt `disagreement` twee passen vorm voor vorm na en
  leest de schrijfacties van de wereld af (`World.changed_attrs`, zodat de naspeling de pas zelf is en geen tweede
  evaluator). Een attribuut van één instantie dat twee of meer schrijvers in beide passen naar verschillende waarden
  zetten, komt in de fout: schrijvers in documentvolgorde, hun waarden, het attribuut, de instantie en haar type, achter
  de budgetzin. Een op hol geslagen creatie groeit in plaats daarvan en houdt de budgetzin
  (`rood/runaway-recursion`, `rood/finite-chain-over-budget`, gemeten).
- De ASP-backend (`lib/translate.ml` +119 −12): `co_writer` herkent een berekenende regel met medeschrijvers (de
  co-writers van `init_writers`: dezelfde attribuutnaam, populaties met een gedeeld concreet type, van toepassing op de
  datum van de case). Zo'n schrijver leidt naast `val/3` zijn waarde af onder zijn index in het document (`wval/4`) en
  zijn vuren (`wvuurt/3`), in elke arm die een `val`-kop schrijft (getal en datum, som-, record-, datum-tijd-, tekst- en
  enumeratiedoel). `writer_conflicts` geeft per schrijver `wschrijft(S,a,v(V),i)` en, alleen voor een schrijver die niet
  staand is, `wschrijft(S,a,leeg,i)` waar hij vuurt zonder waarde; per paar met een gedeeld concreet type één `viol/2`
  met de zin hierboven, het lagere van de twee doeltypen genoemd. Op een staand doel staat de invoer alleen waar geen
  schrijver van het subject beslist, een staande medeschrijver op een ander type van de keten inbegrepen. Twee
  berekenende regels op een tijdlijndoel weigeren met naam.
- De Drools-backend (`cases/CaseRunner.java` +21 −9, nieuw `cases/Schrijfconflict.java` +146,
  `transpiler/DrlTranspiler.java` +37): `Schrijfconflict` is een agendaluisteraar die per sessie de laatste 64
  vuringen bijhoudt waarin een regel met een attribuutdoel (gelijkstelling, berekening of initialisatie) dat attribuut
  van zijn subject veranderde, met de waarde erna. `CaseRunner.vuurTotFixpunt` vuurt elke sessie onder de grens
  (`runCase`, `fireRood`, `naFire`) en geeft bij de grens de zin met de schrijvers, of, waar de laatste vuringen er geen
  tonen, de oude zin. In de DRL-teksttak kijkt een berekenende regel naar zijn doel waar een andere regel (berekening
  of initialisatie) hetzelfde doel schrijft, gesleuteld op het declarerende type (`gedeeldDoel`, via
  `ProducentConsument.elementVan`); dan bereiken beide takken de grens met dezelfde zin. Beide takken delen de
  luisteraar, want de grens en de replay zijn gedeeld.

Verworpen: in ref een fout bij de eerste schrijfactie die de waarde van een andere schrijver overschrijft (een
voorwaarde over een waarde die in dezelfde laag nog wordt berekend, kan twee schrijvers na elkaar laten schrijven
terwijl het fixpunt bestaat, het transiënt van ch. 6.1); in ref een detectie van een herhaalde toestand (vraagt een
gelijkheid op de hele wereld; het budget plus de naspeling zegt hetzelfde met de pas zelf); in clingo één
`#count`-bewaking op `val/3` (vangt leeg tegen een waarde niet en noemt geen schrijvers); in de DRL-teksttak een
marker bij het vertalen voor twee voorwaardelijke schrijvers (zou de disjuncte vorm weigeren, die moet blijven spelen).

### (a) Vormruimte, vóór en na

Elke rij is één case. Kolommen: ref, clingo, exectak, DRL-tekst; "a → b" is vóór → na, één waarde is vóór en na gelijk.
"FAULT budget": `no fixpoint within 100 passes — divergence is a fault (ch. 5.7)`; "FAULT benoemd (two/three)": die zin
met `: two/three writers of one target disagree: …`; "twee waarden": `MISMATCH: X = [a;b]`; "viol benoemd": de
`viol/2` van de paren; "viol (init)": de bestaande `viol("no fixpoint: a computing rule evaluating leeg erases what the
initialisation of meldwaarde writes …")`; "TF": `TRANSLATION FAULT: the initialisation of "meldingsdatum" sits beside
2 computing rules on its target …` (bestaand); "FAIL kring": `geen fixpunt (kringverwijzing?)`; "FAIL benoemd (twee)":
de zin van `Schrijfconflict`; "FAIL waarde (verwacht/werkelijk)": de verwachting van de case tegen de stille waarde.
De verwachtingen van een divergente case dienen alleen om de uitvoerders te laten spreken. Alle documenten zijn
verifier-schoon behalve h5 (`WF-079 FAIL … two unconditional computing rules write target (Vondst, meldingsdatum)`),
dat ter vergelijking ook gemeten is.

| vorm en case | omschrijving | ref | clingo | exectak | DRL-tekst |
|---|---|---|---|---|---|
| a1-twee-een-type case1 | twee op één type, beide gelden, 04-03 en 01-03 | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| a1-twee-een-type case2 | alleen de vinddatum | PASS | PASS | PLAYED | PLAYED |
| a1-twee-een-type case3 | alleen de aanspoeldatum | PASS | PASS | PLAYED | PLAYED |
| a1-twee-een-type case4 | beide gelden, dezelfde datum | PASS | PASS | PLAYED | PLAYED |
| a1-twee-een-type case5 | geen van beide | PASS | PASS | PLAYED | PLAYED |
| a2-drie-een-type case1 | drie op één type, drie datums | FAULT budget → FAULT benoemd (three) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | FAIL waarde (2026-03-01/2026-03-06) → FAIL benoemd (twee) |
| a2-drie-een-type case2 | twee van de drie gelden, verschillend | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | FAIL waarde (2026-03-04/2026-03-06) → FAIL benoemd (twee) |
| a2-drie-een-type case3 | drie gelden, twee gelijk | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | FAIL waarde (2026-03-04/2026-03-06) → FAIL benoemd (twee) |
| a2-drie-een-type case4 | drie gelden, alle gelijk | PASS | PASS | PLAYED | PLAYED |
| a2-drie-een-type case5 | één geldt | PASS | PASS | PLAYED | PLAYED |
| b1-super-sub case1 | supertype en subtype (R2), beide gelden | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| b1-super-sub case2 | Strandvondst, alleen aanspoeldatum (R3) | PASS | PASS | PLAYED | PLAYED |
| b1-super-sub case3 | Strandvondst, alleen vinddatum (R3) | PASS | PASS | PLAYED | PLAYED |
| b1-super-sub case4 | Vondst met vinddatum | PASS | PASS | PLAYED | PLAYED |
| b2-zuster-subtypen case1 | schrijvers op twee zuster-subtypen | PASS | PASS | PLAYED | PLAYED |
| b3-drie-niveaus case1 | Vondst en kleinkind Barnsteenvondst, beide gelden | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| b3-drie-niveaus case2 | Strandvondst, alleen de bovenste geldt | PASS | PASS | PLAYED | PLAYED |
| c1-gelijkstelling-berekening case1 | gelijkstelling naast berekening (getal), 20,00 en 45,00 | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | FAIL waarde (20.00/45.00) → FAIL benoemd (twee) |
| c1-gelijkstelling-berekening case2 | alleen de gelijkstelling | PASS | PASS | PLAYED | PLAYED |
| c1-gelijkstelling-berekening case3 | alleen de berekening | PASS | PASS | PLAYED | PLAYED |
| c1-gelijkstelling-berekening case4 | beide, hetzelfde bedrag | PASS | PASS | PLAYED | PLAYED |
| d1-kenmerk-twee case1 | twee kenmerktoekenningen, beide gelden | PASS | PASS | PLAYED | PLAYED |
| d1-kenmerk-twee case2 | één geldt | PASS | PASS | PLAYED | PLAYED |
| d1-kenmerk-twee case3 | geen geldt | PASS | PASS | PLAYED | PLAYED |
| e1-derde-leest-doel case1 | derde regel (datum plus 30 dagen) en een kenmerk lezen het doel, beide gelden | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| e1-derde-leest-doel case2 | alleen de aanspoeldatum | PASS | PASS | PLAYED | PLAYED |
| e1-derde-leest-doel case3 | alleen de vinddatum | PASS | PASS | PLAYED | PLAYED |
| f1-opgave-markering case1 | opgave-markering op het doel, beide gelden, geen opgave | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | EXCLUDE defaults |
| f1-opgave-markering case2 | idem met opgave | FAULT budget → FAULT benoemd (two) | PASS → viol benoemd | FAIL kring → FAIL benoemd (twee) | EXCLUDE defaults |
| f1-opgave-markering case3 | één geldt, geen opgave | PASS | PASS | PLAYED | EXCLUDE defaults |
| g1-disjunct-gevuld-leeg case1 | disjunct (aanspoeldatum gevuld / leeg), beide datums | PASS | PASS | PLAYED | PLAYED |
| g1-disjunct-gevuld-leeg case2 | alleen de vinddatum | PASS | PASS | PLAYED | PLAYED |
| g1-disjunct-gevuld-leeg case3 | alleen de aanspoeldatum | PASS | PASS | PLAYED | PLAYED |
| g1-disjunct-gevuld-leeg case4 | geen datum | PASS | PASS | PLAYED | PLAYED |
| g2-disjunct-drempel case1 | disjunct op een drempel (> 5,00 / <= 5,00), drie gewichten | PASS | PASS | PLAYED | PLAYED |
| h2-leeg-tegen-waarde case1 | beide gelden, de tweede rekent leeg uit | FAULT budget → FAULT benoemd (two) | PASS → viol benoemd | FAIL kring → FAIL benoemd (twee) | FAIL waarde (2026-03-04/leeg) → FAIL benoemd (twee) |
| h2-leeg-tegen-waarde case2 | beide gelden, gelijke waarde | PASS | PASS | PLAYED | PLAYED |
| h3-onvoorwaardelijk-voorwaardelijk case1 | onvoorwaardelijk naast voorwaardelijk, beide gelden | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| h3-onvoorwaardelijk-voorwaardelijk case2 | alleen de onvoorwaardelijke | PASS | PASS | PLAYED | PLAYED |
| h4-zelfde-voorwaarde case1 | dezelfde voorwaarde letterlijk | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| h5-twee-onvoorwaardelijk case1 | twee onvoorwaardelijk (verifier: WF-079) | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| i1-afwijking-van case1 | Regel en Afwijking van (R4), beide gelden | PASS | PASS | PLAYED | EXCLUDE defaults |
| i1-afwijking-van case2 | idem, alleen de regel | PASS | PASS | PLAYED | EXCLUDE defaults |
| j1-enumeratie case1 | enumeratie als doel, beide gelden | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| j1-enumeratie case2 | idem, één geldt | PASS | PASS | PLAYED | PLAYED |
| j2-tekst case1 | tekst als doel, beide gelden | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| j2-tekst case2 | idem, één geldt | PASS | PASS | PLAYED | PLAYED |
| m1-navigatie case1 | voorwaarde over een rol (vinder), beide gelden | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| m1-navigatie case2 | idem, alleen de eigen voorwaarde | PASS | PASS | PLAYED | PLAYED |
| n1-twee-vondsten case1 | twee Vondsten, de tweede dubbel | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| o1-regelgroep case1 | schrijvers in een regelgroep, beide gelden | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| o1-regelgroep case2 | idem, één geldt | PASS | PASS | PLAYED | PLAYED |
| p1-voorwaardelijk-naast-initialisatie case1 | twee voorwaardelijk plus een initialisatie, beide gelden | FAULT budget → FAULT benoemd (two) | TF: the initialisation of "meldingsdatum" sits beside | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |
| p1-voorwaardelijk-naast-initialisatie case2 | idem, geen geldt | PASS | TF: the initialisation of "meldingsdatum" sits beside | PLAYED | PLAYED |
| q1-initialisatie-naast-lege-berekening case1 | voorwaardelijke initialisatie naast een berekening die leeg uitrekent | FAULT budget → FAULT benoemd (two) | viol (init) | FAIL kring → FAIL benoemd (twee) | FAIL waarde (leeg/25.00) → FAIL benoemd (twee) |
| q1-initialisatie-naast-lege-berekening case2 | idem, de berekening heeft een waarde | PASS | PASS | PLAYED | PLAYED |
| r1-super-sub-met-invoer case1 | supertype en subtype, invoer op het doel, de subtypeschrijver beslist | PASS | twee waarden → PASS | PLAYED | PLAYED |
| r1-super-sub-met-invoer case2 | idem, geen schrijver beslist | PASS | PASS | PLAYED | PLAYED |
| r2-super-sub-leeg-staand case1 | supertype en subtype, de supertypeschrijver rekent leeg uit (staand) | PASS | PASS | PLAYED | PLAYED |
| r2-super-sub-leeg-staand case2 | idem, beide schrijven een waarde | FAULT budget → FAULT benoemd (two) | twee waarden → viol benoemd | FAIL kring → FAIL benoemd (twee) | PLAYED → FAIL benoemd (twee) |

Samengevat over 25 vormen met 60 cases. De 22 cases waarin twee schrijvers op één instantie gelden met verschillende
waarden (leeg meegeteld): ref 22 × budgetzin → benoemde zin (21 met twee schrijvers, a2 case 1 met drie); clingo 18 ×
twee waarden → benoemde `viol/2`, 2 × stil `PASS` → benoemde `viol/2` (f1 case 2, h2 case 1), 2 × ongewijzigd luid
(p1 case 1 `TRANSLATION FAULT`, q1 case 1 de bestaande initialisatie-`viol/2`); exectak 22 × "geen fixpunt
(kringverwijzing?)" → benoemde zin; DRL-tekst 14 × stil `PLAYED` en 6 × `FAIL` op een stille waarde → benoemde zin,
2 × `EXCLUDE` (defaults) ongewijzigd. De 38 andere cases (één schrijver geldt, beide schrijven dezelfde waarde, de
voorwaarden sluiten elkaar uit, twee kenmerktoekenningen, zuster-subtypen, R4) spelen vóór en na in alle vier
(`PASS`/`PLAYED`; 3 × `EXCLUDE` defaults in de DRL-tak), op één na: r1 case 1 in clingo, twee waarden → `PASS`. Met
drie schrijvers met drie waarden noemt ref alle drie; in beide Drools-takken laat de engine er twee elkaar afwisselen
terwijl de derde op de agenda wacht, dus de zin noemt er twee, elk met haar eigen waarde (open punt 2).

### (b) Getuige

`OverlappendeSchrijversWitnessTest` (Drools, tien rijen, documenten in de klasse op Jutterswet-materiaal, beide takken
per rij): `r1_twee_voorwaardelijke_schrijvers_op_een_type`, `r2_de_tweede_schrijver_op_een_subtype`, `r1_op_de_rode_route`
(`CaseRunner.runRood`: `FAULT` met de zin), `drie_schrijvers_met_drie_waarden`,
`een_schrijver_die_leeg_uitrekent_tegenover_een_die_een_waarde_schrijft`, `een_gelijkstelling_naast_een_berekening`,
`de_schrijvers_in_een_regelgroep_heten_naar_hun_regel` (de zin noemt de regel, niet `melding: <regel>`), en drie
flanken die vóór en na groen zijn: `de_disjuncte_vorm_speelt`, `twee_schrijvers_met_dezelfde_waarde_spelen`,
`twee_kenmerktoekenningen_spelen`. Elke rij met een divergentie vraagt "geen fixpunt", "divergentie is een fout
(RUNTIME-SEMANTICS ch. 5.7)", de schrijvers met hun waarden en het doel, en geen "kringverwijzing".

Rood op de claim (`49043b93` tegen de code van `531cd452`):

    Tests run: 10, Failures: 7, Errors: 0, Skipped: 0
    r1_twee_voorwaardelijke_schrijvers_op_een_type:50 exectak: case: geen fixpunt (kringverwijzing?)
    r2_de_tweede_schrijver_op_een_subtype:65 exectak: case: geen fixpunt (kringverwijzing?)
    r1_op_de_rode_route:87 rode route: geen fixpunt (kringverwijzing?)
    drie_schrijvers_met_drie_waarden:102, een_gelijkstelling_naast_een_berekening:136,
      een_schrijver_die_leeg_uitrekent_tegenover_een_die_een_waarde_schrijft:119,
      de_schrijvers_in_een_regelgroep_heten_naar_hun_regel:151 exectak: case: geen fixpunt (kringverwijzing?)

De lus over de takken stopt bij de exectak; de DRL-tak op dezelfde vormen staat in tabel (a) (stil `PLAYED` of `FAIL`
op de waarde van de laatste schrijver). Na de fix: de klasse 10 tests, 0 failures; de suite 985 tests, 0 failures
(975 op de claim).

### (c) Rood `jutterswet-dubbele-melding` en golden `jutterswet-melding-naar-aard`

Rood (`958ae03b`, `corpus/rood/jutterswet-dubbele-melding/`): `ir.json` is de emissie van de frontend van R1 (de bron
staat in NOTE.md), twee cases: `case1-beide-schrijvers-gelden.json` (V met beide datums) en
`case2-een-van-twee-vondsten.json` (V1 met alleen de vinddatum naast V2 met beide; de zin moet V2 noemen). Per case
drie fragmenten: `ref` "two writers of one target disagree: rule "melding op vinddag" writes 2026-03-04 and rule
"melding op aanspoeldag" writes 2026-03-01 to the attribute meldingsdatum of V, a Vondst", `clingo` "two writers of
one target disagree: rules "melding op vinddag" and "melding op aanspoeldag" write different values to the attribute
meldingsdatum of a Vondst — no fixpoint, divergence is a fault (ch. 5.7)","V")" en `drools` "twee schrijvers van één
doel geven verschillende waarden — de regel "melding op vinddag" schrijft 2026-03-04 en de regel "melding op
aanspoeldag" schrijft 2026-03-01 in het attribuut meldingsdatum van V, een Vondst" (case 2 met V2). Op de claim
haalt geen van de drie het fragment (tabel (a), a1 case 1). Verifier-schoon, gate 2 en 4 schoon, geen fragment citeert
een bodemzin.

Golden (`96784085`, `corpus/golden/jutterswet-melding-naar-aard/`), uit `bron.rgs` en `scenario.rgs`, handgerekend in
NOTE.md: de meldingsdatum geschreven op de Vondst ("op zijn vinddatum indien zijn aanspoeldatum leeg is") en op het
subtype Strandvondst ("op zijn aanspoeldatum indien zijn aanspoeldatum gevuld is"), het vindersloon op een drempel
(25,00 bij gewicht > 5,00, 10,00 bij <= 5,00), en een regel die de meldingsdatum leest (plus 30 dagen). De IR uit
`--ir bron.rgs` alleen is byte-gelijk aan die met het scenario.

| case | verwachtingen | ref | clingo | exec vóór = na | DRL-tekst vóór = na | no-op |
|---|---|---|---|---|---|---|
| case1-vondsten-naar-aard | 12 | `oracle: PASS (12 expectations)` | `clingo: PASS (12 expectations)` | `PLAYED 2` (golden) | `PLAYED 2` (golden) | 9 MISMATCH |
| case2-grenzen-en-lege-gegevens | 8 | `oracle: PASS (8 expectations)` | `clingo: PASS (8 expectations)` | idem | idem | 4 MISMATCH |

Toegelaten via `scripts/admit.sh` met het gate-12-voorstel en de README's in de werkboom: `admit: ADMITTED
corpus/golden/jutterswet-melding-naar-aard (2 case(s); verifier-clean, ref = clingo, no-op refuted, gates green)`.

Gate 8 (`scripts/check-rood.sh`, beschermd pad): geen pin te verschuiven. De vloer `EXPECTED_MIN_ROOD_CASES=26` is een
ondergrens en de nieuwe case is in de Drools-poot met een fragment gepind, dus de uitsluitlijst blijft gelijk. Tellingen
40 → 42 rode cases, 80 → 84 fragmenten, Drools-poot 12 → 14 fault-gepind, 28 niet-gedragen.

Gate-12-voorstel (`2c83c188`, `scripts/check-drools.sh`, beschermd pad):

- `EXPECTED_PLAYED`: 233 → 234, redenblok "233 -> 234 (2026-10-01, two writers of one target)" na dat van
  jutterswet-afrekening: de golden, de rode tweeling, wat de exectak daar meldde en nu meldt, en "Measured before and
  after over ALL goldens in both branches: no report line moves".
- `EXPECTED_DRL_PLAYED`: 191 → 192, historieregel "191 -> 192 (2026-10-01, two writers of one target)": de teksttak
  speelt de golden, en een berekenende regel op een gedeeld doel kijkt naar dat doel.
- `DRL_CHANNEL` klasse (A): een hertelzin "Recounted again the same day (two writers of one target):
  jutterswet-melding-naar-aard plays in both branches and moves neither the gap nor a class head, so the gap is 42
  (234 - 192) and the heads hold at 97 lines." De lijst zelf, `EXPECTED_CHANNEL`, `EXCLUDE_KNOWN` en
  `EXPECTED_MIN_TESTS` (239) zijn ongewijzigd.

### (d) De statische weg: niet gebouwd, open punt 1

De verifier ziet beide vormen zonder nieuwe analyse: WF-079 paart de berekenende versies van één doel al en filtert op
"geen voorwaarde"; een onvoorwaardelijke tegenover een voorwaardelijke schrijver is dat filter verruimd, en dezelfde
voorwaarde letterlijk is de structurele gelijkheid van de twee conditieknopen. Beide vormen gaan tegen de huidige tekst
van IR-SPEC ch. 3.1 in: "a guarded case split (each writer carrying a condition) carries its own priority and stays
outside the rule; only the bare collision is statically over-determined". De spec is leidend, dus de regel is een
spec-wijziging van de eigenaar en geen bouwstap van deze kaart. Gemeten over het hele corpus (golden, rood, negatief):
geen document draagt een onvoorwaardelijke naast een voorwaardelijke berekenende schrijver van één doel, en geen
document twee voorwaardelijke met dezelfde voorwaarde, zodat zo'n regel geen bestaande golden zou raken.

### Sweep

Over alle 289 goldens (de nieuwe meegeteld), elke golden met al haar cases, statusregel per golden en per tak, jar van de
claim (`531cd452`) tegen de jar van `1292a0d0`: alle 578 regels gelijk (exectak `PLAYED` 234, DRL-tak `PLAYED` 192;
zonder de nieuwe golden 576 regels, 233 en 191, ook gelijk). Gate 6 (ref = clingo over 783 cases) is groen, dus de
`val/3`-uitkomsten van clingo zijn op het corpus ongewijzigd. Geen bestaande golden of rode case draagt twee
schrijvers met verschillende waarden op één instantie; de acht goldens met een paar voorwaardelijke schrijvers van één doel (onder
meer `anchor/jutterswet-rekenkamer-overzee`, `jutterswet-rubriektabel`, `jutterswet-vrijgavetoets`,
`synth3-kamerheffing`) hebben voorwaarden die elkaar uitsluiten en spelen ongewijzigd in alle vier.

### README's (`85982ee1`)

`ref/README.md`: een punt "Divergence is a fault, named where the state shows its cause" onder de vastgelegde lezingen.
`backends/clingo/README.md`: een punt over twee of meer berekenende regels op één doel (`wval/4`, `wvuurt/3`, de
paarsgewijze `viol/2`, de staande schrijver, de invoer op een staand doel, de weigering op een tijdlijndoel).
`backends/drools/README.md`: de sectie "Recorded: the DRL-text branch answers where the others refuse" is vervangen
door "Recorded: two writers of one target" (de zin van de replay, de drie-schrijversgrens, het kijken naar het doel in
de teksttak); de hoek die de oude sectie vastlegde, speelt niet meer anders.

### Gates

Alle zestien in de volgorde van `gates.yml` met `/tmp/v2/uchar-gates15/run.sh` op de kop `85982ee1` (de laatste
codecommit; de commit met dit Result raakt alleen de kaart), met een schone werkboom: `eval $(opam env)`, `dune build`
(exit 0), dan elk script onder zijn shebang. Logs in `/tmp/v2/uchar-gates15/`.

     1. check-self-contained: exit 0 — check-self-contained: PASS
     2. check-ir-schema: exit 0 — PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 329 file(s) validate and round-trip, floor 192; 43383 object(s) in 830 document(s) in the schema's key order, 1 in no subschema)
     3. check-case-schema: exit 0 — PASS (schema valid; 92 pattern self-test row(s); 825 case file(s) validate)
     4. check-verifier: exit 0 — PASS (329 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
     5. check-oracle: exit 0 — PASS (ref suite 1152 worked examples, floor 489; 783 cases replayed exactly, floor 427; …)
     6. check-clingo: exit 0 — PASS (backend suite 1988 checks, floor 83; 783 cases replay exactly through clingo — ref = clingo over the corpus)
     7. check-noop: exit 0 — PASS (739 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
     8. check-rood: exit 0 — PASS (42 red cases fault observably in ref and clingo, floor 26; 84 pinned fragments cite none of the 36 listed bottom sentences); Drools leg PASS (14 fault-pinned, 28 not-carried with reason)
     9. check-catala: exit 0 — PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    10. check-regelspraak: exit 0 — PASS — JUnit suite green (1008 tests, floor 173); 116 rgs golden(s) re-emit byte-identically (floor 80)
    11. check-toka-parity: exit 0 — PASS
    12. check-drools: exit 0 — PASS
    13. check-reftype-derivation: exit 0 — PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    14. check-flogic: exit 0 — PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    15. check-graaf: exit 0 — PASS
    16. check-scenario: exit 0 — PASS — 23 scenario golden(s) (floor 7) regenerate 82 case file(s) (floor 31) and their ir.json byte-identically

Gate 12 verder: `played 234 (… matched the pinned ref≡clingo expectations)`, `auto-excluded 52`, `channel floor 55
non-PLAYED goldens pinned by name; played matches the pinned 234`, `DRL-text branch played 192 (… pin 192)`, `DRL-text
floor 97 non-PLAYED goldens pinned by name`, `JUnit 985 tests, floor 239`. Gate 15 ook `route leg — … all 116 goldens
with a bron.rgs (floor 98); 23 unit tests green (floor 23)` en `spec leg — 252 of 289 goldens compared, 183 dependencies
equal to the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)`. Eerder groen: alle gates in de
toelating van de golden (`scripts/admit.sh`).

### Afwijkingen van de kaart

1. De DRL-teksttak geeft een luide fout en niet alleen een README-regel: haar kanaal voor een fout bij het vuren is de
   vuurgrens van de gedeelde replay, en met het kijken naar het doel bereikt zij die grens op dezelfde body. Hetzelfde
   kijken dekt de hoek van een voorwaardelijke initialisatie naast een berekening die leeg uitrekent (q1), die de
   README als "the DRL-text branch answers where the others refuse" vastlegde; die sectie is vervangen.
2. De ASP-backend kreeg naast de paarsgewijze `viol/2` twee verfijningen op een typeketen, beide gevonden in de
   vormruimte: de invoer op een staand doel staat alleen waar geen schrijver van het subject beslist (r1: twee waarden
   → `PASS`, een stil fout antwoord van vóór deze kaart), en een staande schrijver die leeg uitrekent telt niet als
   schrijver van leeg (r2 case 1: zonder deze verfijning zou de nieuwe `viol/2` vallen waar ref speelt).
3. De getuigenrij met drie schrijvers vraagt na de rode commit ten minste twee van de drie genoemde schrijvers, niet
   alle drie: de engine laat twee schrijvers elkaar afwisselen terwijl de derde op de agenda wacht. De rode commit
   `49043b93` vroeg alle drie; de versoepeling staat in `fde8bfe0`.
4. De kaart noemt de pinnen 226/191; bij de claim stonden ze op 233/191, het voorstel is 234/192. Gate 8 krijgt geen
   pinwijziging (een vloer, en de nieuwe case is in alle drie de poten gepind).
5. De waarden in de zin volgen elk de eigen weergave van de uitvoerder: ref "20" en "25", de exectak "20.00" en "25",
   de DRL-tak "25.00"; clingo noemt geen waarden (de getuige draagt alleen het subject).
6. (d) niet gebouwd: de vorm gaat tegen de huidige tekst van IR-SPEC ch. 3.1 in (open punt 1).
7. Geen bestaand testbestand en geen bestaande golden of rode case gewijzigd; `spec/RUNTIME-SEMANTICS.md` en
   `spec/IR-SPEC.md` niet aangeraakt.

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/RUNTIME-SEMANTICS.md` ch. 5.7, na stap 4 van de sessie, een alinea: "**Divergence by two writers of one
  target.** Two rules that write one attribute and whose conditions both hold on one instance demand, by ch. 5.1,
  that the attribute equal each of their values. Where the values differ — a writer that fires with a leeg value
  writes leeg (ch. 1.2g), except a standing writer (ch. 7.8), which writes nothing on leeg — no fact base satisfies
  both equations: there is no fixpoint, and the session faults. The fault names the writers, the values they write and
  the attribute and instance; it names no cycle, since the dependency graph has none (WF-045). Where at most one of the
  conditions holds, or the writers write the same value, the equations hold together and the document plays. WF-079
  refuses two unconditional writers statically; a pair with conditions is well-formed and is decided here. Worked
  example (`corpus/rood/jutterswet-dubbele-melding`, `corpus/golden/jutterswet-melding-naar-aard`): "De meldingsdatum
  van een Vondst moet gesteld worden op zijn vinddatum indien zijn vinddatum gevuld is" beside the same rule on its
  aanspoeldatum; V with vinddatum 04-03-2026 and aanspoeldatum 01-03-2026 faults, naming both rules and the values
  2026-03-04 and 2026-03-01; V with only one of the two dates gets that date; V with both dates on 02-03-2026 gets
  **02-03-2026**."
- `spec/IR-SPEC.md` ch. 6.1, aan het eind van de alinea over de verplichting (na "inside it the fold makes them one."):
  "For two writers that carry a condition the question is asked at evaluation, not statically: two conditional
  computing writers of one target that survive into one instance's effective stack — on one type, or on a type and
  its ancestor outside the gate of `RUNTIME-SEMANTICS.md` ch. 7.10 — are well-formed, and where both conditions hold on
  that instance with different values the session faults as a divergence (`RUNTIME-SEMANTICS.md` ch. 5.7). Every
  executor names the two writers and the target: `ref/` when its pass budget runs out, the ASP backend in a `viol/2`
  witness per pair of writers that share a concrete type, the Drools replay at its fire cap."
- `spec/IR-SPEC.md` ch. 3.1, WF-079, de gemeten motivering: "(ref oscillates and faults the fixpoint budget, clingo is
  silently multi-valued, …)" en "ref and the Drools exec-model branch find no fixpoint, clingo derives two values, and
  the DRL-text branch answers by document order" beschrijven de stand vóór 2026-10-01. Voorstel: na die zinnen "Since
  2026-10-01 all four executors fault such a pair by name where both writers apply to one instance
  (`RUNTIME-SEMANTICS.md` ch. 5.7); WF-079 keeps refusing the unconditional pair ahead of evaluation."

### Open punten

1. De statische weg (acceptatie (d)) is een normvraag voor de eigenaar: een WF-regel die een onvoorwaardelijke naast
   een voorwaardelijke berekenende schrijver van één doel weigert, of twee voorwaardelijke met letterlijk dezelfde
   voorwaarde, gaat tegen IR-SPEC ch. 3.1 in ("only the bare collision is statically over-determined"). De verifier
   ziet beide zonder nieuwe analyse (het WF-079-paar met een verruimd filter; structurele gelijkheid van de
   conditieknoop), en geen document in het corpus draagt een van beide vormen.
2. Met drie of meer schrijvers met verschillende waarden noemen de Drools-takken de schrijvers die de engine in de
   laatste vuringen laat afwisselen, gemeten twee van de drie; ref noemt alle drie. Luid in alle vier, maar niet met
   dezelfde opsomming.
3. ref noemt alleen schrijvers van een scalair attribuut: `World.changed_attrs` leest geen tijdlijnen. Twee schrijvers
   van een tijdlijndoel geven in ref alleen de budgetzin; clingo weigert ze met naam; de Drools-replay vergelijkt de
   segmentlijst. Niet gemeten (geen vorm in de vormruimte), geen corpusdocument draagt de vorm.
4. Op een typeketen is elke schrijver van een doel de enige van zijn eigen sleutel en dus staand (ch. 7.8 sleutelt op
   het gedeclareerde type, in alle uitvoerders gedeeld). Of het staande domein per concreet type moet worden gelezen,
   zoals IR-SPEC ch. 6.1 dat van de enkele-schrijverregels vraagt, is een specvraag; deze kaart maakte clingo alleen
   gelijk aan ref (r1, r2).
5. `frontends/regelspraak/grammar/DIVERGENCES.md` §7 zegt nog "where both apply to one instance, ref and the Drools exec
   branch report no fixpoint and clingo gives the target two values"; buiten May touch, niet bijgewerkt.
6. De distributie-CLI draait op een oudere pin en kent deze wijziging niet; `bin/regelspraak scenario` toont de
   benoemde zin pas als de pin meegaat.

### Commits

    531cd452 claim: overlappende-voorwaardelijke-schrijvers
    49043b93 OverlappendeSchrijversWitnessTest: twee voorwaardelijke schrijvers, rood
    5e0fa01d ref: een divergentie door twee schrijvers van één doel krijgt een naam
    d5dc2c9a clingo: twee schrijvers van één doel met verschillende waarden, viol/2
    fde8bfe0 Drools: twee schrijvers van één doel krijgen een naam in beide takken
    958ae03b rood jutterswet-dubbele-melding: twee voorwaardelijke schrijvers
    e2450303 clingo: een staande medeschrijver op de typeketen verdringt de invoer
    1292a0d0 DRL-teksttak: een berekening naast een initialisatie kijkt naar haar doel
    96784085 golden jutterswet-melding-naar-aard: schrijvers die elkaar uitsluiten
    2c83c188 voorstel gate 12: jutterswet-melding-naar-aard speelt in beide takken
    85982ee1 README's: twee schrijvers van één doel in ref, clingo en Drools

en de commit die de kaart met dit Result naar `backlog/done/` verplaatst. Niet gepusht, niet gemerged.
