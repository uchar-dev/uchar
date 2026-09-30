# drools-exec-defaults-justificatie-vouw: de rechtvaardiging van een default telt niet en leest niet verder dan één rol

*Vondst van een regelsetbouwer, 2026-09-28 (brug K3 in zijn README), bevestigd
door de hoofdsessie op uchar `056df376` via de distributie; reproducties op
Jutterswet-materiaal in `/tmp/v2/probe/k1k3/` (k3a.rgs, k3b.rgs; alleen
lezen). Luid (exit 3), geen fout antwoord.*

## Aanleiding

De exec-tak vertaalt een defaultsgroep (DIVERGENCES §4) alleen als elke
rechtvaardiging reduceerbaar is: lokaal of hooguit één rol ver, zonder
aggregatie. Een `Uitzondering op` met een voorwaarde die telt ("het aantal
vondsten van de Jutter groter is dan 2") of twee rollen ver leest ("de premie
van de jutter van de vondst van de Melding") markeert de hele groep:
"defaults (…): niet-reduceerbare justificatie (far-side/aggregatie/global)".
Dezelfde voorwaarde in een gewone regel speelt (gemeten: exit 0), en één rol
ver in een default speelt ook (exit 0). Een regelset die uitzonderingen op
een recht schrijft vanuit wat zijn gronden of zijn aanvraag zeggen, moet
daardoor hulpkenmerken en dagtellingen op het recht zetten die een gewone
regel afleidt.

## Minimale reproductie

`056df376`, distributie `bin/regelspraak check`; basis B:

    Objecttype de Jutter (mv: Jutters) (bezield)
        de premie Numeriek (geheel getal);
    Objecttype de Vondst (mv: Vondsten)
        het gewicht Numeriek (geheel getal);
        de bonus Numeriek (geheel getal);
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten

    R1  B + Regel premie / geldig altijd / De premie van een Jutter moet berekend worden als 10.
        Uitzondering op premie: premie bij veel vondsten / geldig altijd /
        De premie van een Jutter moet berekend worden als 20
        indien het aantal vondsten van de Jutter groter is dan 2.
        nu: exit 3, "transpiler SKIP: defaults: defaults A:Jutter.premie — defaults (Jutter.premie):
        niet-reduceerbare justificatie (far-side/aggregatie/global): <default>".
    R2  B + een derde objecttype Melding (de gemelde premie) met Feittype VondstHeeftMelding
        (de vondst Vondst; de melding (mv: meldingen) Melding; Eén vondst heeft meerdere meldingen);
        Regel gemelde premie / … als 10; Uitzondering op gemelde premie: … als 20
        indien de premie van de jutter van de vondst van de Melding groter is dan 5.
        nu: exit 3, dezelfde zin voor Melding.gemelde premie.
    R3  B + Regel bonus / … als 1; Uitzondering op bonus: bonus bij rijke jutter / … als 2
        indien de premie van de jutter van de Vondst groter is dan 5.
        nu: exit 0 (één rol ver speelt).
    R4  R1 als gewone regel (zonder Uitzondering op, met de voorwaarde op de regel zelf).
        nu: exit 0.

## Acceptatie

- (a) Meet de vormruimte: aggregatie in de rechtvaardiging (aantal, som,
  minimum, maximum, met en zonder filter), twee en drie rollen ver, een
  parameter (global) in de rechtvaardiging, en combinaties; per vorm ref,
  clingo, exec, DRL-tekst.
- (b) De exec-tak speelt een rechtvaardiging met een aggregatie en met een
  lezing twee rollen ver, met dezelfde uitkomst als ref en clingo (de
  rechtvaardiging beslist per ch. 7 op het moment dat haar operanden er zijn;
  let op de herevaluatie als een geaggregeerde waarde later verandert, dezelfde
  familie als de kaart drools-exec-voorwaarde-genavigeerde-afleiding). Wat na
  de kaart niet meegaat, blijft een benoemde SKIP die de vorm noemt. Rood-eerst
  JUnit-getuigen.
- (c) Golden (werknaam `jutterswet-premiestaffel`) met R1 en R2 en de
  gemeten vormen, handgerekend, ref ≡ clingo ≡ Drools-exec; gate-12-voorstel;
  DRL-teksttak volgens haar kanaal (zij heeft geen defaults-lowering).
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: serieel na de kaart
drools-exec-voorwaarde-genavigeerde-afleiding (zelfde regelevaluatie).

## Result (2026-09-29, bouwer)

Branch `card/drools-exec-defaults-justificatie-vouw`, gebouwd op 29 en 30 september: op `26ca6b0b` en daarna
gerebased op main `1f601d19` (daarin landde `434d478f`, de kaart frontend-ontkende-kwantor); zie "Rebase".
Gemeten met de eigen uitvoerders van deze repo: `_build/default/ref/bin/runner.exe`,
`_build/default/backends/clingo/bin/clingo_runner.exe`, `_build/default/spec/verifier/bin/verifier.exe`, de
Drools-jar (`java -jar <jar> ir.json case…`, `--drl` voor de teksttak) en de frontend (`Resolver --ir … --cases …`).
"Vóór" is de jar van de claim (backend gelijk aan `26ca6b0b` en aan main: main raakte `backends/drools` niet),
"na" de jar van de kop.

### Oorzaak

1. **De vertaling van een rechtvaardiging.** `compileDefault` maakte van elke rechtvaardiging een predicaat op het
   onderwerp (`subjectConditie` → `leafPredicate`). Drie vormen hebben daar geen arm: een aggregatie (die vraagt een
   accumulate op de linkerkant van de regel), een lezing over twee of meer rollen (`navComparisonRole` geeft voor een
   keten null) en een parameter (een voorwaarde bindt die als patroonvariabele in `voorwaarde`, niet in een
   predicaat). Eén zo'n blad weigerde de hele groep, in één verzamelzin die vorm noch voorwaarde noemt
   (`niet-reduceerbare justificatie (far-side/aggregatie/global): <default>`; `<default>` omdat `Default.bron` op de
   draad leeg is).
2. **Het moment waarop een groep beslist** (RUNTIME-SEMANTICS ch. 7.4: bij het fixpunt van alle producenten van wat
   haar rechtvaardigingen lezen). `defaultsSalience` zette een groep alleen onder de gewone regels die wat zij leest op
   haar eigen type schrijven, en de groepen kwamen in gedeclareerde volgorde na alle regels in het model. Een groep
   die las wat een later gebouwde groep schrijft, of wat een verre regel schrijft die zelf op een negatielezer wacht,
   besliste daardoor op een lege waarde; een toegekend kenmerk blijft staan (ch. 5.3). En een regel die het
   kenmerkdoel van een groep ontkend leest, stond in de stratificatie een band onder de regels, want die ziet een groep
   niet als producent; stond de groep zelf in die band, dan vuurde de lezer eerst. Vijf van de gemeten vormen gaven zo
   al vóór de kaart een stil fout antwoord, in vormen die de exectak al speelde (tabel: de FAIL-rijen onder "vóór").

### Fix

`ExecModelTranspiler` (+540, −58 regels, commentaar inbegrepen) en `Stratificatie` (+14):

- **`rechtvaardiging`**: waar `subjectConditie` een rechtvaardiging vertaalt, blijft dat de rechtvaardiging, zoals
  voorheen. Anders vouwt een samengestelde voorwaarde haar leden hier, met de stoptriggers van ch. 5.10
  (`vastgesteld`, `telVerdict`), en vertaalt elk lid apart: een blad op het onderwerp; een vergelijking met een
  aggregatie (`aggregatieRechtvaardiging`): de aggregatie wordt een accumulate op de reduce-regel
  (`aggregatie(f, subject, $subject, true)`, met de telling gebonden zodat een lege minimum of maximum leeg is en geen
  ontbrekende rij), waarvan de consequentie het resultaat bindt (`RechtvaardigingBouw.gebonden`); of een vergelijking
  of leeg/gevuld-toets waarvan een operand een keten van rollen of een numerieke parameter leest
  (`scalaireRechtvaardiging`, `rechtvaardigingZijde`): die operand leest zoals een waarde (`scalarWaarde`, met de
  ketenpoort `ketenWaardePad` open), per datatype vergeleken zoals de bladen op het onderwerp dat doen (getallen via
  `rsAggVergelijk` met de herleiding van `restatement`, datums via `datumVergelijkTemporal`, tekst, enumeratie en
  booleaan via `tekstAttrGelijk`). De rechtvaardiging wordt geëvalueerd bij het vuren, niet als toets op het
  onderwerppatroon, dus zo'n lid mag onder elke kwantor staan. Wat niet vertaalt, weigert de groep met de vorm en de
  voorwaarde: `rechtvaardiging niet vertaald, <vorm>: <voorwaarde>`.
- **`groepVolgorde`**, vóór de regels gebouwd worden: een graaf over gewone regels en groepen (wat elk leest en
  schrijft, op naam en op verwante typen: eigen type, een rol verder, de elementen van een vouw en zijn filter). Een
  groep neemt de laagste band waarop wat zij leest wacht, transitief door regels en groepen (niet strikt eronder), en
  komt in het model na de groepen waarop zij wacht; Drools vuurt binnen een salience in toevoegvolgorde, en gewone
  regels komen vóór de groepen. Een regel die het kenmerkdoel van een groep ontkend leest (`Stratificatie.leestOntkend`,
  dezelfde lezing van de voorwaarde als de stratificatie), komt strikt onder die groep, en een regel die het kenmerk van
  zo'n verlaagde regel ontkend leest weer daaronder; de andere regels houden hun salience. Een kring met een ontkende
  lezing door een groep (WF-046, WF-051) weigert met naam.
- **Geen far-side-joins.** De eerste fix (`2b3f1ae2`) gaf elke navigatie van een groep de join van `navJoins`. Met de
  band speelt elke gemeten vorm gelijk met en zonder de joins (proefjar met een schakelaar, alle vormen en de golden);
  zij zijn eruit (`418ad9a4`). De herevaluatie van een aggregatie is die van de accumulate; een verre lezing wacht op
  haar producenten.

### (a) Vormruimte, vóór en na

Elke vorm is een eigen Jutterswet-document (bron.rgs + scenario.rgs, door de frontend naar ir.json en cases), met een
generator; 76 documenten, in `/tmp/v2/dj/vorm/` (`maak.py` schrijft ze, `meet.sh` meet ze). "vóór" en "na" zijn de exectak. DRL-tekst is voor en na gelijk: elke vorm met een
defaultsgroep `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering`.
"SKIP verzamelzin" is `niet-reduceerbare justificatie (far-side/aggregatie/global): <default>`.

| vorm | rechtvaardiging | ref | clingo | exec vóór | exec na | DRL-tekst |
|---|---|---|---|---|---|---|
| r1-aantal | R1: aantal vondsten > 2 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| r2-tweerollen | R2: premie van de jutter van de vondst van de Melding > 5 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| r3-eenrol | R3: premie van de jutter van de Vondst > 5 | PASS | PASS | PLAYED 1 | PLAYED 1 | EXCLUDE defaultszin |
| r4-gewone-regel | R4: R1 als gewone regel | PASS | PASS | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| ag-aantal | aantal > 2 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-aantal-filter | aantal die zwaar zijn > 1 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-som | som gewicht > 16 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-som-filter | som gewicht die zwaar zijn > 14 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-min | minimale waarde gewicht > 5 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-max | maximale waarde gewicht > 8 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-min-filter | minimum die zwaar zijn > 5 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-max-filter | maximum die zwaar zijn > 8 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-aantal-param | aantal > parameter | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-aantal-eigen | aantal > eigen drempel | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-som-vs-som | som gewicht > som bonus | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-geen | geen van: aantal > 2, som > 14 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-tenminste | ten minste één van: aantal > 2, maximum > 10 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-alle | alle: aantal > 1, minimum > 5 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-afwezigheidsfilter-a | aantal met subselectie "zijn gewicht is leeg", gewicht afgeleid, afleiding eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-afwezigheidsfilter-l | idem, lezer eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-afwezigheidsfilter-groep | idem, gewicht uit een defaultsgroep | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| ag-kwantorvouw | kwantorvouw: gewicht van ten minste 2 van de vondsten > 5 | PASS | PASS | SKIP verzamelzin | SKIP een kwantorvouw | EXCLUDE defaultszin |
| ag-in-rekenkunde | aantal plus 1 > 3 | PASS | PASS | SKIP verzamelzin | SKIP een aggregatie binnen | EXCLUDE defaultszin |
| hr-som-afgeleid-a | som over afgeleid gewicht, afleiding eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| hr-som-afgeleid-l | idem, lezer eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| hr-filter-afgeleid-a | aantal met afgeleid filterkenmerk, afleiding eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| hr-filter-afgeleid-l | idem, lezer eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| hr-kenmerk-afgeleid-a | kenmerkdoel, aantal zware > 1, afleiding eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| hr-kenmerk-afgeleid-l | idem, lezer eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| hr-accumulate-transitief | som over gewicht dat wacht op een negatielezer | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| hr-join-tweerollen | twee rollen, premie wacht op een negatielezer | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| hr-join-kenmerk-tweerollen | idem, kenmerkdoel | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| hr-join-kenmerk-eenrol | één rol, kenmerkdoel, premie wacht op een negatielezer | PASS | PASS | FAIL kenmerk zwaar: verwacht afwezig, werkelijk aanwezig | PLAYED 1 | EXCLUDE defaultszin |
| hr-join-verdeling | één rol, aandeel uit een verdeling van een groepsdoel | FAULT lege bron | PASS | ERROR (NPE in de verdeling) | ERROR (NPE in de verdeling) | EXCLUDE defaultszin |
| hr-join-verdeling-regel | idem, bedrag uit een negatielezer | PASS | PASS | ERROR (NPE in de verdeling) | ERROR (NPE in de verdeling) | EXCLUDE defaultszin |
| zz-verdeling-zonder-groep | flank: die verdeling zonder defaultsgroep | PASS | PASS | ERROR (NPE in de verdeling) | ERROR (NPE in de verdeling) | ERROR (NPE in de verdeling) |
| nv-tweerollen | twee rollen > 5 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| nv-tweerollen-gevuld | twee rollen gevuld | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| nv-tweerollen-leeg | twee rollen leeg | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| nv-tweerollen-eigen | twee rollen > eigen attribuut | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| nv-tweerollen-expr | twee rollen plus 1 > 5 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| nv-tweerollen-param | twee rollen > parameter | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| nv-tweerollen-kenmerk | kenmerkcheck twee rollen ver | FAULT onopgelost | FAULT onopgelost | SKIP verzamelzin | SKIP een onopgeloste voorwaarde | EXCLUDE defaultszin |
| nv-tweerollen-afgeleid-a | twee rollen, afgeleide premie, afleiding eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| nv-tweerollen-afgeleid-l | idem, lezer eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| nv-drierollen | drie rollen > 5 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| nv-eenrol-afgeleid-a | één rol, afgeleide premie, afleiding eerst | PASS | PASS | PLAYED 1 | PLAYED 1 | EXCLUDE defaultszin |
| nv-eenrol-afgeleid-l | idem, lezer eerst | PASS | PASS | PLAYED 1 | PLAYED 1 | EXCLUDE defaultszin |
| nv-eenrol-afgeleid-omgekeerd-a | idem, Vondsten eerst in de case, afleiding eerst | PASS | PASS | PLAYED 1 | PLAYED 1 | EXCLUDE defaultszin |
| nv-eenrol-afgeleid-omgekeerd-l | idem, lezer eerst | PASS | PASS | PLAYED 1 | PLAYED 1 | EXCLUDE defaultszin |
| nv-eenrol-kenmerk-afgeleid-a | één rol, kenmerkdoel, afgeleide premie, afleiding eerst | PASS | PASS | PLAYED 1 | PLAYED 1 | EXCLUDE defaultszin |
| nv-eenrol-kenmerk-afgeleid-l | idem, lezer eerst | PASS | PASS | PLAYED 1 | PLAYED 1 | EXCLUDE defaultszin |
| nv-kenmerk-afgeleid-a | twee rollen, kenmerkdoel, afgeleide premie, afleiding eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| nv-kenmerk-afgeleid-l | idem, lezer eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| gl-eigen-param | eigen gewicht > parameter | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| gl-eenrol-param | één rol > parameter | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| gl-datum-param | eigen vinddag later dan een datumparameter | PASS | PASS | SKIP verzamelzin | SKIP de parameter 'meldgrens' | EXCLUDE defaultszin |
| gl-param-in-consequentie | parameter in de consequentie (controle) | PASS | PASS | PLAYED 1 | PLAYED 1 | EXCLUDE defaultszin |
| gg-eenrol-attr-omgekeerd | leest één rol ver het doel van een latere groep, attribuutdoel | PASS | PASS | FAIL attribuut bonus: verwacht 2, werkelijk 1 | PLAYED 1 | EXCLUDE defaultszin |
| gg-eenrol-kenmerk-omgekeerd | idem, kenmerkdoel | PASS | PASS | FAIL kenmerk zwaar: verwacht afwezig, werkelijk aanwezig | PLAYED 1 | EXCLUDE defaultszin |
| gg-eenrol-kenmerk-recht | idem, de schrijvende groep eerst | PASS | PASS | PLAYED 1 | PLAYED 1 | EXCLUDE defaultszin |
| gg-eigen-kenmerk-omgekeerd | leest het eigen doel van een latere groep, kenmerkdoel | PASS | PASS | FAIL kenmerk ijverig: verwacht afwezig, werkelijk aanwezig | PLAYED 1 | EXCLUDE defaultszin |
| gg-eigen-kenmerk-recht | idem, de schrijvende groep eerst | PASS | PASS | PLAYED 1 | PLAYED 1 | EXCLUDE defaultszin |
| gg-tweerollen-kenmerk-omgekeerd | leest twee rollen ver het doel van een latere groep | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| gg-negatielezer-a | negatielezer van het kenmerkdoel van een groep in band 0, afleiding eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| gg-negatielezer-l | idem, lezer eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| gg-negatielezer-eigen | negatielezer van het kenmerkdoel van een groep in band -1 (eigen producent) | PASS | PASS | FAIL kenmerk rustig: verwacht afwezig, werkelijk aanwezig | PLAYED 1 | EXCLUDE defaultszin |
| gg-negatielezer-band | idem, groep in band -1 via een verre producent | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| cb-alle-agg-nav | alle: aantal controles > 1, twee rollen > 5 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| cb-r1r2-a | R1 en R2 samen, regeltekst Jutter eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| cb-r1r2-l | idem, regeltekst Melding eerst | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| cb-ongeordend | ongeordend: aantal > 2 (20), som > 14 (30) | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| cb-ongeordend-afgeleid-a | ongeordend, afgeleid filterkenmerk, afleiding eerst | PASS | PASS | SKIP verzamelzin | PLAYED 2 | EXCLUDE defaultszin |
| cb-ongeordend-afgeleid-l | idem, lezer eerst | PASS | PASS | SKIP verzamelzin | PLAYED 2 | EXCLUDE defaultszin |
| cb-genest | Uitzondering op een uitzondering, som > 16 | PASS | PASS | SKIP verzamelzin | PLAYED 1 | EXCLUDE defaultszin |
| cb-consequentie-agg | aggregatie in de consequentie | PASS | PASS | SKIP consequentie | SKIP consequentie | EXCLUDE defaultszin |

Samengevat, exectak over 76 vormen: vóór 11 PLAYED, 57 SKIP (56 verzamelzin, 1 consequentie), 5 FAIL, 3 ERROR; na
68 PLAYED, 5 SKIP met naam, 3 ERROR. De drie ERROR-rijen zijn een verdeling waarvan het bedrag eerst leeg is; zij
crasht ook zonder defaultsgroep en in beide Drools-takken (open punt 6). Letterlijke weigerzinnen na de fix:

    ag-kwantorvouw: rechtvaardiging niet vertaald, een kwantorvouw (een telling over de gekoppelde instanties, die deze tak alleen in de voorwaarde van een regel vouwt): het gewicht van ten minste 2 van de vondsten van de Jutter groter is dan 5
    ag-in-rekenkunde: rechtvaardiging niet vertaald, een aggregatie binnen een rekenkundige uitdrukking: het aantal vondsten van de Jutter plus 1
    gl-datum-param: rechtvaardiging niet vertaald, de parameter 'meldgrens', die deze tak in een rechtvaardiging alleen als getal leest: de meldgrens
    nv-tweerollen-kenmerk: rechtvaardiging niet vertaald, een onopgeloste voorwaarde: de jutter van de vondst van de Melding ijverig is
    cb-consequentie-agg: niet-reduceerbare consequentie (far-side/aggregatie): <default>   (ongewijzigd, buiten de kaart)

Proeven op de fix, elk een tijdelijke schakelaar in een jar buiten de tak: zonder de groepsvolgorde en -band (op
`418ad9a4`) falen gg-eenrol-attr-omgekeerd, gg-eenrol-kenmerk-omgekeerd, gg-eigen-kenmerk-omgekeerd,
gg-tweerollen-kenmerk-omgekeerd, hr-join-kenmerk-eenrol, hr-join-kenmerk-tweerollen, hr-join-tweerollen en de golden
(case 1 gemelde premie van M1 en M2 10 waar 20, dringend van M1 aanwezig, controlepremie van C1 5 waar 15; case 2
idem); met elke groep strikt onder haar producenten (op `cc1e9869`) falen gg-negatielezer-a en -l (`kenmerk rustig:
verwacht afwezig, werkelijk aanwezig`); zonder de joins (op `cc1e9869`) speelt alles. gg-negatielezer-eigen en
-band faalden op `418ad9a4` (`kenmerk rustig: verwacht afwezig, werkelijk aanwezig`; de eerste ook al op de claim) en
spelen sinds `b31260cd`. De vorm van de kaart drools-exec-vouwfilter-latere-afleiding (een subselectie die op afwezigheid
toetst, over een later afgeleide waarde) speelt in een rechtvaardiging in alle drie de varianten
(ag-afwezigheidsfilter-*): de groep wacht op de schrijver.

### (b) Getuigen

`DefaultsRechtvaardigingWitnessTest` (exectak, 16 rijen, documenten in de klasse op Jutterswet-materiaal), en één
rij van `RekenjaarWitnessTest`.

- Rood op de claim (`20a76d08`, vóór de rebase `d40e495e`, twaalf rijen): `Tests run: 12, Failures: 2, Errors: 10`;
  tien rijen `Excluded transpiler SKIP: defaults: … niet-reduceerbare justificatie (far-side/aggregatie/global):
  <default>`, de groep-leest-latere-groep-rij `een rol ver, de lezende groep eerst ==> … <case: kenmerk zwaar: verwacht
  afwezig, werkelijk aanwezig>`, de kwantorvouwrij `… niet-reduceerbare justificatie … <default> ==> expected: <true>
  but was: <false>` (de weigering noemde vorm noch voorwaarde).
- `24e0b7ac`: de tweede case van de kenmerkrij las drempel 5 waar de gemeten vorm 15 heeft; na de correctie 12/12
  groen op de eerste fix.
- Herevaluatie (`7a5fc535`): rood op de claim (verzamelzin), op de eerste fix zonder joins `attribuut gemelde premie:
  verwacht 20, werkelijk 10`.
- De band (`a05fd45c`): de kenmerkrij rood op de eerste fix (`kenmerk dringend: verwacht afwezig, werkelijk
  aanwezig`); de flank met een negatielezer in band 0 groen voor en na, rood met de groep strikt onder haar producenten.
- De negatielezer onder de groep (`759c148b`): rood op de kop ervoor en al op de claim, `rustig na dringend, op het
  eigen type ==> … <case: kenmerk rustig: verwacht afwezig, werkelijk aanwezig>`.
- `RekenjaarWitnessTest`, rij "the justification of a prioritized default (jutterswet-wrakrecht…)": die rij pinde de
  weigering `niet-reduceerbare justificatie` en schrijft zelf voor dat zij het antwoord van ref gaat verwachten zodra
  de tak de knoop verlaagt; na de eerste fix faalde zij met `Expected … Excluded to be thrown, but nothing was thrown`
  en verwacht zij nu ref's waarden (exectak), de weigering van de DRL-tak ongewijzigd.
- Na de fix: de klasse 16 tests, 0 failures; de suite 897 tests, 0 failures, 0 skipped (881 op de claim; vloer 239).

### (c) Golden `jutterswet-premiestaffel`

Zes defaultsgroepen over Jutter, Melding en Controle: de premie (aantal > 2 → 20; som van het gewicht van de zware
vondsten > parameter zwaartegrens → 30, hoger gestapeld), ijverig (kenmerkdoel; niet ijverig bij minimum < 2 of
maximum < 5), de gemelde premie en dringend (twee rollen ver), de controlepremie (drie rollen ver). Melding en Controle
staan in `bron.rgs` vóór de Jutter, zodat hun groepen lezen wat een later gebouwde groep schrijft. Handgerekend in
NOTE.md; toegelaten via `scripts/admit.sh` met het gate-12-voorstel in de werkboom:
`admit: ADMITTED corpus/golden/jutterswet-premiestaffel (2 case(s); verifier-clean, ref = clingo, no-op refuted, gates green)`.

| case | verwachtingen | ref | clingo | exec vóór | exec na | DRL-tekst | no-op |
|---|---|---|---|---|---|---|---|
| case1-staffel | 26 | PASS | PASS | EXCLUDE verzamelzin (gemelde premie) | PASS | EXCLUDE defaultszin | 22 MISMATCH (o.a. `J1.premie = leeg, expected 30`) |
| case2-hogere-zwaartegrens | 5 | PASS | PASS | idem | PASS | idem | 5 MISMATCH |

Replay per uitvoerder: `oracle: PASS (26 expectations)`, `oracle: PASS (5 expectations)`; `clingo: PASS (26
expectations)`, `clingo: PASS (5 expectations)`; exectak `PLAYED 2`; DRL-tak `EXCLUDE drl: ch. 7 prioritizedDefaults —
the DRL-text branch has no defaults lowering`; exectak op de jar van de claim `EXCLUDE transpiler SKIP: defaults:
defaults A:Melding.gemelde premie — defaults (Melding.gemelde premie): niet-reduceerbare justificatie
(far-side/aggregatie/global): <default>`. De IR uit `--ir bron.rgs` alleen is byte-gelijk aan die met het scenario.

### Gate-12-voorstel (`5e49cd30`, `scripts/check-drools.sh`, beschermd pad)

- `EXPECTED_PLAYED`: 223 → 224, redenblok "223 -> 224 (2026-09-30, the justification that counts or reads further
  than one role)" na dat van jutterswet-zinkende-jutter. Op de oorspronkelijke basis was dit 222 → 223.
- `DRL_CHANNEL` klasse (A): `"jutterswet-premiestaffel|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this
  branch"` op naamvolgorde; kopregels: "36 lines", "sitting 34 below", "35 of the 36", "the 36th", "All 35", "The 34 is
  38 exec-only players — those 35, …", een hertelzin "(224 - 190, jutterswet-zinkende-jutter of the same day playing in
  both); the six class heads (A 36, B 37, C 10, D 2, E 3, F 3 = 91 lines)", en in de lijstkop "eighty-six of the
  ninety-one".
- `EXPECTED_DRL_PLAYED` blijft 190, `EXPECTED_MIN_TESTS` blijft 239, `EXPECTED_CHANNEL` en `EXCLUDE_KNOWN`
  ongewijzigd.

### (d) Sweep

Over alle goldens, elke golden met al zijn cases, statusregel per golden vergeleken tussen de jar van de claim en de
jar van de kop:
- vóór de rebase, 279 goldens: exectak en DRL-tak alle regels byte-gelijk (PLAYED 222 en 189);
- na de rebase, 281 goldens (met jutterswet-zinkende-jutter en de nieuwe golden): exectak precies één regel anders,
  `jutterswet-premiestaffel` van `EXCLUDE … niet-reduceerbare justificatie …` naar `PLAYED 2`, PLAYED 223 → 224; DRL-tak
  alle 281 regels gelijk, PLAYED 190.
- Een proefjar die per document meldt of `groepVolgorde` iets verandert: over alle goldens met een case krijgt geen
  bestaande golden een andere groepssalience, een andere groepsvolgorde of een andere regelsalience; alleen
  jutterswet-premiestaffel een andere volgorde. De beangeneratie is niet aangeraakt.

### Gates

Alle zestien in de volgorde van `gates.yml`, op de kop `b31260cd` (de laatste codecommit; het Result raakt
alleen de kaart): bash voor de zeven scripts met `#!/usr/bin/env bash` (catala, regelspraak, toka-parity, drools,
flogic, graaf, scenario; graaf staat niet in de opdrachtlijst maar heeft die shebang), sh voor de rest,
`eval $(opam env)` vooraf, catala aanwezig, plus `dune build @ref/runtest`. Ook alle zestien groen op de claim
(gate 12: played 222, DRL-text 189, JUnit 881), in de toelating van de golden (`scripts/admit.sh`) en op de
gerebasede kop `5e49cd30` vóór de laatste drie commits (played 224, DRL-text 190, JUnit 896).

    check-self-contained: PASS
    check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 320 file(s) validate and round-trip, floor 192; 42061 object(s) in 821 document(s) in the schema's key order, 1 in no subschema)
    check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 795 case file(s) validate)
    check-verifier: PASS (320 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
    check-oracle: PASS (ref suite 1152 worked examples, floor 489; 755 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
    check-clingo: PASS (backend suite 1988 checks, floor 83; 755 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop: PASS (711 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
    check-rood: Drools leg PASS (12 fault-pinned, 28 not-carried with reason)
    check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
    check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    check-regelspraak: PASS — JUnit suite green (999 tests, floor 173); 108 rgs golden(s) re-emit byte-identically (floor 80)
    check-toka-parity: PASS
      re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: toka-23-regel-bevestigingstijdstip-vlucht.rgs toka-45-regel-belasting-op-basis-van-afstand.rgs); TOKA suite 27 tests, floor 27
    check-drools: PASS
      played 224  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
      auto-excluded 54  (per-form refusals: loader / transpiler marker / contract / unsupported input)
      value-divergence 0  (listed with reason — the exec-model differs from ref/clingo)
      channel floor 57 non-PLAYED goldens pinned by name; played matches the pinned 224
      DRL-text branch played 190  (its OWN count — the narrower branch, not the square; pin 190)
      DRL-text floor 91 non-PLAYED goldens pinned by name; sweep cost 135s wall clock
      JUnit 897 tests, floor 239  (the backend's own suite, green with a non-zero count)
    check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    check-graaf: spec leg — 244 of 281 goldens compared, 141 dependencies equal to the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)
    check-graaf: route leg — the graph of the wire equals the graph of the source on all 108 goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)
    check-graaf: PASS
    check-scenario: PASS — 15 scenario golden(s) (floor 7) regenerate 54 case file(s) (floor 31) and their ir.json byte-identically
    (dune build @ref/runtest: exit 0)

### README

`backends/drools/README.md`, sectie "Recorded: a defaults justification that counts or reads further than one role":
wat de exectak vertaalt, de volgorde en band van de groepen en de plaats van een negatielezer, dat een groep geen
far-side-join draagt, en welke vormen met naam weigeren.

### Afwijkingen van de kaart

1. De fix gaat verder dan een aggregatie en twee rollen ver: ook een numerieke parameter in een rechtvaardiging (hetzelfde
   pad, gemeten en getuigd), en de volgorde van de groepen met de plaats van een negatielezer. Reden: zonder de volgorde
   gaf een vorm die nu gaat spelen een stil fout antwoord waar zij eerst luid weigerde (hr-join-kenmerk-tweerollen,
   gg-negatielezer-band), en vijf vormen die de tak al speelde antwoordden fout.
2. Geen far-side-joins, waar de kaart "dezelfde far-side-join-aanpak als regelVoorwaarde" noemt: gebouwd, gemeten zonder
   werking zodra de band er is, en verwijderd (Fix, derde punt). De herevaluatie van een aggregatie doet de accumulate.
3. Een groep staat niet strikt onder haar producenten, zoals het commentaar bij `defaultsSalience` het ontwerp noemde,
   maar in hun band en erna; gemeten reden: gg-negatielezer-a en -l.
4. Een bestaand testbestand gewijzigd: één rij van `RekenjaarWitnessTest` verwacht nu het antwoord van ref in de
   exectak, zoals die rij zelf voorschrijft voor een tak die de knoop gaat verlagen.
5. `Stratificatie` (gedeeld met de DRL-tak) kreeg een leesmethode, `leestOntkend`; de salience die de DRL-tak leest is
   ongewijzigd.

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md`, bij de hekken "non-reducible justifications" en de salience
  "strictly below every producer of every default justification": aanvullen met "2026-09-30: in the exec-model branch a
  justification that counts, reads a chain of roles or a numeric parameter lowers member by member (an aggregation as an
  accumulate on the reduce-rule); the refusal names the form and the condition. A group takes the band of the producers
  of what it reads, transitively through rules and groups, and is added after the groups it waits on; a rule that reads
  a group's characteristic under negation is placed strictly below the group. Golden `jutterswet-premiestaffel`."
- `spec/CONSTRUCTS.md` noemt het hek niet; geen rij.

### Open punten

1. Een kwantorvouw in een rechtvaardiging weigert met naam (ag-kwantorvouw); `kwantorvouwVoorwaarde` bouwt een
   accumulate met een eval, die voor een rechtvaardiging haar verzamelde lijst aan de consequentie zou moeten binden.
2. Een aggregatie binnen een rekenkundige uitdrukking in een rechtvaardiging weigert met naam (ag-in-rekenkunde);
   `lowerVal` vouwt een minimum of maximum zonder telling, zodat een lege vouw geen rij geeft.
3. Een niet-numerieke parameter (datum, tekst) in een rechtvaardiging weigert met naam (gl-datum-param); de
   consequentie van een groep bindt alleen numerieke globals en de rekendatum.
4. Een kenmerkcheck twee rollen ver: de frontend maakt er een onopgeloste voorwaarde van; ref en clingo weigeren hem ook.
5. Producenten buiten regels en groepen (verdeling, object- en feitcreatie) staan niet in de graaf van `groepVolgorde`;
   een groep die leest wat alleen zo'n producent schrijft, wacht er niet op. Niet gemeten tot een antwoord: zie 6.
6. Buiten de kaart gevonden: een verdeling waarvan het bedrag eerst leeg is en later wordt afgeleid, crasht in beide
   Drools-takken (`NullPointerException … BigDecimal.divide`), ook zonder defaultsgroep (zz-verdeling-zonder-groep); ref
   en clingo geven `PASS`. Komt het bedrag uit een defaultsgroep, dan faalt ref (`empty source amount in equal-shares
   allocation (v0 mirror hole, ch. 4.4)`) en geeft clingo `PASS` (hr-join-verdeling): ref en clingo verschillen daar.
7. De stratificatie van de regels kent de groepen niet. `groepVolgorde` zet een negatielezer van een groepskenmerk
   onder de groep; een regel die een door een groep geschreven attribuut op leegte toetst, zet het niet om (niet gemeten
   in deze kaart; de README noemt de volgordegrens van de leeg-toets, en de kaart drools-exec-vouwfilter-latere-afleiding
   meet die vorm in een vouwfilter met een groep als schrijver).
8. De DRL-teksttak heeft geen defaults-lowering (ongewijzigd).
9. De distributie-CLI draait op een oudere pin en kent deze wijziging niet; de pin moet na het landen mee.

### Rebase

`git rebase main` naar `1f601d19`. Eén conflict, in `scripts/check-drools.sh` bij het redenblok van `EXPECTED_PLAYED`:
opgelost door beide blokken te houden, dat van jutterswet-zinkende-jutter eerst, en de pin op te tellen (223 van main
+ 1 = 224); de hertelzin van klasse (A) telt nu tegen 224 − 190. De commitboodschap van het voorstel is met
`git commit --amend` bijgewerkt naar 223 → 224 (de kop van de tak op dat moment). Na de rebase opnieuw gemeten: de
sweep (d), de vormen (a) (gelijk aan de meting ervoor), de suite en alle zestien gates.

### Commits

Na de rebase op `1f601d19` (tussen haken de hash vóór de rebase):

    630ba82b (2c25dad1) claim: drools-exec-defaults-justificatie-vouw
    20a76d08 (d40e495e) DefaultsRechtvaardigingWitnessTest: telling en keten in een rechtvaardiging, rood
    24e0b7ac (6c7b27ce) DefaultsRechtvaardigingWitnessTest: drempel 15 in de tweede kenmerkcase
    2b3f1ae2 (10a0918d) Exectak: rechtvaardiging die telt of verder leest, groep na producent
    7a5fc535 (a795553a) DefaultsRechtvaardigingWitnessTest: herevaluatie na de beslissing
    cee849b1 (f16635ee) backends/drools/README: een rechtvaardiging die telt of verder leest
    a05fd45c (898fea6e) DefaultsRechtvaardigingWitnessTest: de band van een verre producent
    cc1e9869 (0a559a4e) Exectak: een groep neemt de band van haar producenten, transitief
    418ad9a4 (ec707d87) Exectak: geen far-side-joins op een defaultsgroep, de band volstaat
    ee410f13 (d36d35d8) golden jutterswet-premiestaffel: rechtvaardigingen die tellen en ver lezen
    5e49cd30 (c98e9274) voorstel gate 12: jutterswet-premiestaffel speelt in de exectak
    52b2c8bc            DefaultsRechtvaardigingWitnessTest: geen commithash in de klassetekst
    759c148b            DefaultsRechtvaardigingWitnessTest: een negatielezer onder de groep
    b31260cd            Exectak: een negatielezer van een groepskenmerk zakt onder de groep

en de commit die dit Result in de kaart zet. Niet gepusht, niet gemerged; de kaart blijft in `backlog/doing/`.

## Keuring (hoofdsessie, 2026-09-30)

Geland op main na een eigen run van alle zestien gates op de kop van de
branch, `420435f1`, door de bouwer gerebased op main `1f601d19` met de
opgetelde pinnen (logs `/tmp/v2/uchar-gates7/`): groen. Gemeten daar:
gate 3 320 goldens schoon; gate 5 en 6 755 cases exact; gate 7 711
weerleggingen, 44 overlevers (ongewijzigd); gate 10 999 toetsen, 108
rgs-goldens byte-gelijk; gate 12 speelt 224 in de exec-tak en 190 in de
DRL-tak (de exec-pin één omhoog, voorstel van de bouwer, hier geland),
JUnit 897; gate 16 15 scenario-goldens, 54 casebestanden.

Gelezen: de oorzaak (elke rechtvaardiging werd een toets op het onderwerp,
waarin een aggregatie, een keten van rollen en een parameter niet passen;
de groepen beslisten vóór de groepen en verre regels die schrijven wat zij
lezen, zodat vijf al spelende vormen stil fout antwoordden), de fix in
`ExecModelTranspiler` en `Stratificatie` (een aggregatie als accumulate op
de reduce-regel, een keten of numerieke parameter gelezen als regelwaarde,
`groepVolgorde` met de band van de producenten en de negatielezer onder de
groep, weigering met vorm en voorwaarde), de vormruimte van 76 vormen (11
spelend vóór, 68 na, 5 met naam geweigerd, 3 crashes buiten de kaart), de
getuigen (16 rijen rood-eerst plus één rij van `RekenjaarWitnessTest`), de
golden `jutterswet-premiestaffel` met handberekening in alle uitvoerders,
de sweep (281 goldens, alleen de nieuwe golden verschuift, DRL-tak gelijk)
en de README-sectie. De vijf afwijkingen van de kaart aanvaard met hun
meting (bredere fix met de groepsvolgorde, geen far-side-joins, groep in de
band van haar producenten en erna, één rij van een bestaand testbestand,
`Stratificatie.leestOntkend`). Registertekst in
`spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` bij deze landing gezet.
Open, vastgelegd in het Result en niet als kaart: een kwantorvouw, een
aggregatie binnen een rekensom en een datum- of tekstparameter in een
rechtvaardiging weigeren met naam; een verdeling met een eerst leeg bedrag
crasht in beide Drools-takken, en ref en clingo verschillen daar;
verdelingen en creaties tellen niet mee in de groepsvolgorde. Pin-bump
naar de distributie en de werkbank hierna, samen met de kaart
frontend-ontkende-kwantor.
