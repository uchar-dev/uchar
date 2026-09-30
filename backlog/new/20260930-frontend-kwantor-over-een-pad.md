# frontend-kwantor-over-een-pad: een kwantor of vouw over een pad van twee relaties

*Grens gevonden door een regelsetbouwer, 2026-09-30 (brug K6 in zijn
clusterdocument), gemeten door de hoofdsessie op uchar `c16cb4d6` via de
distributie (`bin/regelspraak check|scenario|cases`) en de eigen
uitvoerders; reproductie op Jutterswet-materiaal in `/tmp/v2/probe/k6/`
(bron.rgs, bron-brug.rgs, scenario.rgs, cases/; alleen lezen). Luid in alle
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

    R1  bron.rgs zoals hierboven.
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
