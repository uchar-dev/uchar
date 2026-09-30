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
