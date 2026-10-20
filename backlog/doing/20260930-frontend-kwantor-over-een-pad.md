# frontend-kwantor-over-een-pad: een kwantor of vouw over een pad van twee relaties

*Grens gevonden door een regelsetbouwer, 2026-09-30 (brug K6 in zijn
clusterdocument), gemeten door de hoofdsessie op uchar `c16cb4d6` via de
distributie (`bin/regelspraak check|scenario|cases`) en de eigen
uitvoerders; reproductie op Jutterswet-materiaal in `/tmp/v2/probe/k6/`
(source.rgs, bron-brug.rgs, scenario.rgs, cases/; alleen lezen). Luid in alle
uitvoerders (de frontend lowert de zin niet en laat een marker op de draad
staan; ref en clingo falen op de onopgeloste voorwaarde, de exec-tak
markeert), geen fout antwoord. Op verzoek van de eigenaar geschreven
(2026-09-30), zodat de grens niet vergeten wordt; geen bouwdatum.*

## Aanleiding

Een kwantorvouw noemt precies één relatie vanaf het onderwerp van de regel
(IR-SPEC ch. 4.5): "ten minste één van de vondsten van de Jutter". Zodra
de verzameling twee stappen ver ligt — het onderwerp, een to-one-rol naar
een tussenobject, en de to-many-rol van dat tussenobject — weigert de
frontend: "de kwantorvouw over 'ten minste één van de vondsten van de
melder van de Melding' wordt NIET gelowerd: een meerstapsbereik: de zin
stapelt nog een verzameling onder de gekwantificeerde, en een kwantorvouw
noemt precies één relatie vanaf het regelonderwerp (IR-SPEC ch. 4.5)".
Hetzelfde geldt voor een vouw over twee relaties in een waarde.

De omweg is een hulpkenmerk op het tussenobject, afgeleid met een kwantor
van één stap, dat de regel dan één stap ver leest (bron-brug.rgs, PASS in
de drie uitvoerders). Dat werkt, maar het kost per tussenobject een lid
dat de bron niet kent, en de regel zegt niet meer wat de norm zegt: de
norm spreekt over de leden van de groep van het onderwerp, de regel over
een kenmerk van de groep. Een regelset met een tweelaagse structuur (een
object, zijn groep, de leden van die groep) loopt hier bij elke norm over
de leden tegenaan.

Dit is geen bug: de grens staat in het IR. Het opheffen is een
ontwerpkeuze met twee wegen, hieronder als acceptatie (b).

## Minimale reproductie

`c16cb4d6`, distributie `bin/regelspraak check|scenario|cases`; ref via
`_build/default/ref/bin/runner.exe`, clingo via
`_build/default/backends/clingo/bin/clingo_runner.exe`.

    Objecttype de Jutter (mv: Jutters) (bezield)
    Objecttype de Vondst (mv: Vondsten)
        het gewicht Numeriek (geheel getal);
        is zwaar kenmerk (bijvoeglijk);
    Objecttype de Melding (mv: Meldingen)
        is dringend kenmerk (bijvoeglijk);
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten
    Feittype MeldingVanJutter
        de melder Jutter
        de melding (mv: meldingen) Melding
    Eén melder heeft meerdere meldingen

    Regel zwaar
        geldig altijd
            Een Vondst is zwaar
            indien zijn gewicht groter is dan 10.

    Regel dringend
        geldig altijd
            Een Melding is dringend
            indien ten minste één van de vondsten van de melder van de Melding zwaar is.

    Scenario: Jutter J met Vondst V (gewicht 12) en Melding M (melder J); verwacht: V is zwaar, M is
    dringend.

    R1  source.rgs zoals hierboven.
        nu: check exit 0 met twee WARNINGs ("… wordt NIET gelowerd: een meerstapsbereik …" en
        "onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het onderwerp (§8.1.7/§8.1.8)
        … blijft als marker op de wire staan"); scenario MARKER "transpiler SKIP: regel: dringend —
        regel (voorwaarde buiten Phase-B scope): dringend"; ref "EVALUATION FAULT: Unresolved
        condition reached in evaluation"; clingo "TRANSLATION FAULT: Unresolved condition reached in
        translation: this rule fails observably".
    R2  bron-brug.rgs: het hulpkenmerk `de zware vondst` op de Jutter ("Een Jutter heeft de zware
        vondst indien ten minste één van de vondsten van de Jutter zwaar is") en de regel leest
        "indien de melder van de Melding de zware vondst heeft".
        nu: check exit 0 zonder meldingen; scenario PASS.

## Acceptatie

- (a) Meet de vormruimte in de frontend en de drie uitvoerders: de kwantor
  "alle", "ten minste één" en "geen" over to-one → to-many en over to-many →
  to-many; een vouw (aantal, som, minimum, maximum, eerste, laatste) over
  twee relaties in een waarde; drie stappen; de toets op de elementen als
  kenmerk, als vergelijking en als leeg-toets; en de omweg van R2 als de
  vorm die moet blijven spelen.
- (b) Eén van twee wegen, door de eigenaar te kiezen. *Standaard:* een
  lowering in de frontend die het hulpkenmerk (of, voor een vouw, het
  hulpattribuut) op het tussenobject zelf genereert, zoals de
  opgave-markering haar leden genereert (DIVERGENCES §7): de auteur
  schrijft de zin van de norm, het IR, de verifier en de uitvoerders blijven
  ongewijzigd, de gegenereerde namen zijn vast en staan in het rapport; de
  regel die het pad leest wordt de lezing van één stap. *Alternatief:* een
  pad in de kwantorvouw van het IR (IR-SPEC ch. 4.5 en de verifier), met de
  lowering in ref, clingo en beide Drools-takken; groter, maar zonder
  gegenereerde leden. Rood-eerst JUnit-getuige met R1.
- (c) Golden (werknaam `jutterswet-dringende-melding`), handgerekend, in
  ref ≡ clingo ≡ Drools-exec; de pins van gate 10 en 12 als **voorstel**
  waar de golden telt; de DRL-teksttak volgens haar kanaal.
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). DIVERGENCES: een eigen sectie voor
  de lowering, of IR-SPEC ch. 4.5 bij het alternatief.

May touch (standaardweg): `frontends/regelspraak/**`, `corpus/golden/<nieuwe
golden>/`, `scripts/check-*.sh` (pins, **voorstel**), kaartverplaatsing.
Niet: `ref/`, `backends/clingo/`, `backends/drools/`, `spec/` (bij het
alternatief anders, dan eerst een spec-voorstel). NB: geen bouwdatum; te
wegen zodra een regelset meer dan één zo'n brug draagt.

## Aanvulling (2026-10-01, uit de landing van drools-exec-voorwaarde-twee-rollen-ver)

Een telling over een pad van meer dan één stap kapt de frontend stil af:
"het aantal havenjutters van de haven van de jutter van de Vondst" komt op
de draad als een telling met `sourceRole` `JutterHeeftVondst.jutter` en
argument `jutter`, dus over de jutter van de Vondst; `check` meldt niets
(geen WARNING meerstapsbereik), de verifier laat het document door, en ref
en clingo antwoorden 1 waar 2 hoort. Een telling over een bereik van meer
dan één stap is volgens WF-102 (c) ongeldig. Dat is ernstiger dan de
weigering hierboven: een stil fout antwoord in drie uitvoerders.
Reproductie: `/tmp/v2/probe/vtr-bouw/`, vorm x-veel-aantal (gemaakt door
maak.py; alleen lezen). De acceptatie van deze kaart omvat de vorm: de
lowering (standaardweg) of, tot die er is, een benoemde weigering in de
frontend moet ook de telling dekken.

## Aanvulling (2026-10-02, uit de ronde frontend G4+G5)

De ronde nam de standaardweg van deze kaart (een gegenereerd lid, geen pad
op de draad) voor één vorm: een WAARDEVOUW over één verzameling gevolgd
door to-one stappen ("de maximale waarde van het gewicht van de weging van
de vondst van alle jutters van de Ploeg"). Het elementtype krijgt een lid
"<lid> via <rol 1>, … en <rol n>", een gegenereerde regel zet het uit de
keten, en de vouw loopt erover (DIVERGENCES, "A fold over a set and to-one
steps after it"; golden `jutterswet-zwaarste-weging`). Een som of telling
daarover telt per element.

Open blijven, gemeten op de kop van de ronde (`126c71cd`):
- R1 van deze kaart, de kwantor over een pad (to-one → to-many: "ten
  minste één van de vondsten van de melder van de Melding"): ongewijzigd,
  `check exit=3` met de WARNING "… wordt NIET gelowerd: een
  meerstapsbereik …", ref `EVALUATION FAULT: Unresolved condition …`,
  clingo `TRANSLATION FAULT …`, exectak `EXCLUDE …`. Luid. De standaardweg
  hier is een gegenereerd KENMERK op het tussenobject (de melder), van
  dezelfde vorm als het lid van de ronde.
- De telling over een pad van meer dan één stap (de vorm van de eerste
  Aanvulling, "het aantal havenjutters van de haven van de jutter van de
  Vondst"): op de kop van de ronde nog STIL afgekapt tot een telling over
  `JutterHeeftVondst.jutter` (gemeten op de reproductie
  `/tmp/v2/probe/vtr-bouw/proef/veel-aantal/`: `check exit=3` alleen om de
  exectak, de telling op de draad met `sourceRole`
  `JutterHeeftVondst.jutter`). Een telling telt exemplaren, dus het lid
  van de ronde past er niet op. **Luid sinds de naronde van 2026-10-02**
  (`backlog/done/20261002-naronde-klein-g4-g5.md`): waar de emitter nog
  een bereik zou schrijven (de kop op naam, of het typepaar van onderwerp
  en element) en de geschreven bron verder loopt, weigert de frontend met
  een ERROR die het pad noemt ("… is een telling over een pad van 3
  stappen (jutter, haven, havenjutters): deze frontend telt alleen over
  één relatie vanaf het onderwerp (IR-SPEC ch. 4.3, WF-102 (c)); schrijf
  de telling op het bereikte object of noem één relatie"), `check` exit 1;
  ook een anker van een ander type ("het aantal havenjutters van de
  Haven" op een Vondst) en een selectie van een unie met een stap na haar
  kop, die eveneens stil afkapten. Waar geen bereik te schrijven is,
  houden de vormen hun eerdere luide weigering (een vouw zonder bereik,
  een geschreven stap, de marker met WARNING bij "van alle …"). De
  lowering van een telling over een pad (een lid per element telt hier
  niet; een vlakke vereniging onder `count` is op de draad niet
  uitdrukbaar, WF-102 (c)) blijft open met deze kaart.
- Een vouw over een to-many stap NA de verzameling ("de inhoud van de
  kisten van de vondst van alle jutters"): de oude WARNING en marker
  (luid).

## Besluit en acceptatie (2026-10-20, eigenaar; vervangt (b)–(d) hierboven)

Gemeten op main `4f3d2a8e` (proeven en tabel: `uchar-dev/werk/kwantor-pad/meting.md`, buiten de repository): het IR
draagt een kwantor over een pad al. `QuantifierFold` heeft sinds 2026-09-16 een `sourcePath` (IR-SPEC ch. 4.5,
WF-108 (c)), de verifier toetst hem, en ref en clingo voeren hem uit als platte vereniging (RUNTIME-SEMANTICS ch.
3.4b; golden `jutterswet-ketenkwantor`, handgeschreven). De frontend schrijft hem niet: een kenmerktoets over een pad
geeft de WARNING meerstapsbereik plus een marker (EF-019 in alle uitvoerders), een vergelijking of leeg/gevuld-toets
over een pad de ERROR "setlezing buiten een vouwbron". De Drools-loader weigert `sourcePath` op een `QuantifierFold`
(DROOLS-026). Een waardevouw over een pad speelt al overal (`sourcePath`, in Drools via
`transpiler/core/SourcePath.java`), ook to-many → to-many en over drie stappen. Het aantal over een pad weigert de
frontend; het IR laat die grens bewust open (IR-SPEC ch. 4.3, "No `sourcePath` at a selection").

De eigenaar koos 2026-10-20 de weg **pad op de draad**: de frontend schrijft `QuantifierFold.sourcePath`, de
Drools-backend leert hem. Geen spec- of IR-wijziging, geen gegenereerde leden. Aanleiding nu: de v2-regelset van de
werkbank omzeilt de grens met brug K6 (een kwantor to-one → to-many in de voorwaarde van een `Uitzondering op`) en
met een regelset-intern feittype voor een gevuld-toets die in het CGM twee verzamelingen ver ligt.

Acceptatie:
- (a) Rood eerst: een JUnit-getuige met R1 die faalt op main.
- (b) De frontend lowert een kwantor over een pad van twee of meer stappen naar `QuantifierFold` met `sourcePath`:
  alle kwantoren (alle, geen, ten minste/ten hoogste/precies n, ook ten minste één), elke toets (kenmerk, ook een
  kenmerk dat als "is een <naam>" gelezen wordt; vergelijking; leeg/gevuld), to-one en to-many stappen in elke
  volgorde, en in de voorwaarde van een gewone regel en van een defaults-groep. Wat niet bindt, blijft luid.
- (c) Drools: de loader laat `sourcePath` op een `QuantifierFold` toe en beide takken voeren hem uit via de
  bestaande `SourcePath`-planning (één analyse, twee weergaven). Wat het beanmodel niet draagt, weigert luid met een
  code uit het register.
- (d) Golden `jutterswet-dringende-melding` met `source.rgs` en `scenario.rgs`, handgerekend in `NOTE.md` vóór een
  uitvoerder draaide, met ten minste R1, de vorm van K6 (in een `Uitzondering op`), een gevuld-toets over drie stappen
  (to-one, to-many, to-many), een kwantor over to-many → to-many en een tellende kwantor; ref ≡ clingo ≡ Drools (beide
  takken).
- (e) Het aantal over een pad blijft een benoemde weigering, ongewijzigd.
- (f) DIVERGENCES: de sectie over het meerstapsbereik zegt wat nu gelowerd wordt. Alle zestien gates groen op de
  eindstand. Pins en vloeren in beschermde scripts (onder meer de pin van `jutterswet-ketenkwantor` en
  `EXPECTED_PLAYED` in `scripts/check-drools.sh`) als voorstel, met reden; de tekst voor `CLAUDE.md` en
  `spec/CONSTRUCTS.md` in de Result.

May touch: `frontends/regelspraak/**`, `backends/drools/**` (niet de productieruntime onder `runtime/`),
`corpus/golden/jutterswet-dringende-melding/`, `scripts/check-*.sh` (pins en vloeren, voorstel), de kaart.
Niet: `ref/`, `backends/clingo/`, `spec/`, andere goldens (behalve een pin die verandert omdat een golden gaat spelen).
Landen na akkoord van de eigenaar.
