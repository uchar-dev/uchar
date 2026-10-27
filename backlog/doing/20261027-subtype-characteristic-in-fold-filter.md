# subtype-characteristic-in-fold-filter: een kenmerk van een subtype in het filter van een vouw over een rol van het supertype

*Vastgelegd door de werkbank (2026-10-27), uit de bouw van de regelset-opdracht `wekeneis-nieuw-recht` (rules, tak
`wekeneis-nieuw-recht`, 9590119). Gemeten op toolchain b4f3ab94 (uchar c6a45d89). Niet gecommit tot de bouw.*

## Aanleiding

In v2/herleving-ww vouwt de uitzondering van art. 21 lid 2 onder a over "alle rechten van de samenloop" (rol `rechten`,
gevuld met het supertype `RechtOpUitkering`). Sinds een besluit van de eigenaar (2026-10-27) telt een recht daar alleen
mee als het de wekeneis heeft. Het kenmerk `wekeneis` staat op het subtype `RechtOpUitkeringWwBijOntslag`, waar het CGM
het legt.

In het filter van die vouw bindt `.. hij heeft de wekeneis` niet. `check` geeft, exit 0:

    WARNING (rules/hww-06-samenloop.rgs:42): onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het
    onderwerp (§8.1.7/§8.1.8), dus er is geen lidmaatschap om te toetsen — 'hij heeft de wekeneis' blijft als marker
    op de wire staan

Hetzelfde gebeurt met "het RechtOpUitkering heeft de wekeneis", plat en in een geneste groep. De werkbank maakt van een
WARNING een rode `make check`.

De regelset omzeilt het nu met brug K14: het filter leest de toets van de wekeneis zelf (de kalenderweken van de
referteperiode tegen de drempel). Daardoor staat die toets op twee plekken. Een eerdere brug op hetzelfde patroon is K9:
"de twee maanden" staan op het supertype, omdat de inkomens aan `Recht op uitkering WW` hangen, dat de grond niet ziet.

## Reproductie op neutraal materiaal

```
Objecttype het Strandrecht (mv: Strandrechten)
    het loon Numeriek (getal met 2 decimalen);
Objecttype het Jutrecht is een Strandrecht (mv: Jutrechten)
    de grens Numeriek (getal met 2 decimalen);
    de blokkade kenmerk (bezittelijk);
Objecttype het Wrakrecht is een Jutrecht (mv: Wrakrechten)
Extensie van objecttype het Wrakrecht
    de vaartijd kenmerk (bezittelijk);
Objecttype de Ploeg (mv: Ploegen)
Objecttype het Logboek (mv: Logboeken)
    de zeeweken Numeriek (geheel getal);
Objecttype de Melding (mv: Meldingen)
    de soortcode Numeriek (geheel getal);
Feittype PloegBetreftStrandrecht
    de ploeg Ploeg
    het recht (mv: rechten) Strandrecht
Eén ploeg betreft meerdere rechten
Feittype StrandrechtHeeftLogboek
    het recht Strandrecht
    het logboek Logboek
Eén recht heeft één logboek
Feittype MeldingLeidtTotStrandrecht
    de melding Melding
    het recht Strandrecht
Eén melding leidt tot één recht
Parameter de vaartijddrempel : Numeriek (geheel getal)

Regel vaartijd
    geldig altijd
        Een Wrakrecht heeft de vaartijd
        indien de zeeweken van het logboek van het Wrakrecht groter of gelijk is aan de vaartijddrempel.

Regel blokkade
    geldig altijd
        Een Jutrecht heeft de blokkade
        indien hij aan alle volgende voorwaarden voldoet:
        - zijn grens is gevuld
        - (de maximale waarde van het loon van alle rechten van de ploeg van het Jutrecht die aan alle volgende voorwaarden voldoen:
        .. hij is niet het Jutrecht zelf
        .. de soortcode van de melding van het Strandrecht is gelijk aan 1
        .. hij heeft de vaartijd) is groter dan zijn grens.
```

- **Zo geeft het de WARNING.**
- **Wat wel zonder melding speelt** (gemeten door de bouwer, met vier scenario's: boven, onder, zonder logboek, zonder
  aantal):
  - de toets zelf in het filter ("… de zeeweken van het logboek van het Strandrecht is leeg" of "… groter of gelijk aan
    de vaartijddrempel");
  - het kenmerk op het supertype gedeclareerd, met de regel op het subtype.

## Doel

`hij heeft <kenmerk>` (en `het <Supertype> heeft <kenmerk>`) bindt in het filter van een vouw over een rol die een
supertype vult, ook als alleen een subtype het kenmerk declareert.

Wat zo'n kenmerk betekent voor een lid dat niet van dat subtype is, is een ontwerpvraag. Het standaardantwoord van de
werkbank: het kenmerk geldt voor een lid van het subtype zoals daar afgeleid, en geldt niet voor een lid van een ander
type. Dat is gelijk aan "hij is een <Subtype> en hij heeft <kenmerk>".

Twee dingen beslist de bouwer, met één Fable-advies als het een echte ontwerpvraag is:
- geldt hetzelfde voor een attribuut van een subtype in zo'n filter;
- wat zegt de verifier tegen een naam die alleen een subtype van een ander type declareert.

De keuze komt in de spec (RUNTIME-SEMANTICS en de paragraaf van de binding) en in het Result.

## Acceptatie

- (a) Rood eerst: een golden in de vorm van de reproductie, met een lid van het subtype dat het kenmerk heeft, een dat
  het niet heeft, en een lid van een ander type in de vouw. Ref, clingo en beide Drools-takken spelen hem met dezelfde
  uitkomst.
- (b) De uitleg van de toolchain noemt het kenmerk in het filter (`NAME` met `declaration: KENMERK`).
- (c) Goldens zonder deze vorm blijven bytegelijk; `make gates` groen.
- (d) v2 bouwt nog. In het Result staat hww-06 met "... hij heeft de wekeneis" in plaats van brug K14, gemeten met
  `check` en `scenario-test` van v2/herleving-ww: geen WARNING, en dezelfde uitkomsten.

## Niet

De regelset zelf: het weghalen van brug K14 doet de werkbank na de pin-bump.

## Voorlopige besluiten (Fable-advies, 2026-10-27)

*Uit `research/subtype-characteristic-in-fold-filter-2026-10-27/fable-advies.md` (gelezen op `088659f0`), door de
hoofdsessie als voorlopig besluit overgenomen. Het advies noemt per uitvoerder de plaatsen in de code, de golden
met haar waarden en de bouwvolgorde. Acceptatie (b) (de uitleg van de toolchain) en (d) (hww-06 met `check` en
`scenario-test`) doet de hoofdsessie bij de pin-bump van de toolchain.*

- **D1 Weergave.** `CharacteristicCheck {characteristic, host: S}` met S een strikt subtype van het type van het
  ene gebonden object van de positie: de versmallende host. Geen nieuw knooppunt, geen nieuw veld, geen
  schemawijziging. De frontend emitteert hem alleen waar de keten van het onderwerp K niet draagt.
- **D2 Lezing** (RUNTIME-SEMANTICS ch. 5.8b regel 4). Waar iff het object een instantie van S is (extensioneel,
  ch. 2.2) en K heeft zoals S het declareert; `negated` is het complement. Declaratiesleutel en graafknoop: S.
- **D3 Ontkenning.** Een lid buiten S: positief onwaar, ontkend waar — het gewone complement (ch. 5.8a regel 4,
  WF-122 (c)); niet de hop-lezing van ch. 5.8 regel 3. Gepind door `hoogste rustloon` (40, niet 30).
- **D4 WF-109.** De flank "nul voldoend, één overlappend" is op een positie met één gebonden object een lezing;
  op de feitcreatie-guard blijft hij `only-some-objects`. Getuige herschreven naar de guard.
- **D5 Positie.** Elke positie met één gebonden object (regel, consistentie, verdeling, creatie, default,
  vouwfilter, kwantorfilter en -test, unieselectie). Niet: de genavigeerde check, de verankerde check, de guard.
- **D6 Siblings.** Twee onvergelijkbare declaranten: frontend weigert met een benoemde diagnose die ze noemt;
  geen vereniging geëmitteerd (de wire kan haar wel dragen; verwijding later). Eén declarant op welke diepte ook
  onder het onderwerp: die host.
- **D7 Attribuut.** Niet in deze kaart. Richting voor de volgende: versmallende eigen lezing (`subjectType` S ⊏
  onderwerp) leest leeg buiten S, ch. 1.2-discipline, geen typetoets als conjunct.
- **D8 Verifier.** WF-100 en WF-034 ongewijzigd: de host moet K declareren; zonder host moet het onderwerp K
  declareren (nieuwe negatieve getuige).
- **D9 Frontend-diagnose.** Geen declarant onder het onderwerp: RGS-092 `no-characteristic-no-role` als nu.
- **D10 ref.** `owner_hosts` kent drie uitkomsten; het statische type is `env.subj`; de foutzin voor een vreemde
  host blijft (suite r. 6126).
- **D11 clingo.** `isa(X,S), has(X,K)`; ontkend via helper onder NAF.
- **D12 Drools.** Klassetoets + lezing op beide takken; DROOLS-240 alleen nog voor een onvergelijkbare host; een
  ontbrekende stand leest als "buiten S", nooit als een patroon dat het lid uit de regel laat vallen.
- **D13 Lezing ch. 5.12.** Knoop `CharacteristicCheck <oordeel>`, geen `on`-deel; geen nieuwe regelvorm.
- **D14 Uitleg.** Niets extra op de wire; de distributie leest `host` als holder (`Verbaliser` r. 1815), eigen
  kaart bij de pin-bump.
- **D15 Getuigen.** Golden `jutterswet-ploegblokkade` (bron + scenario, 3 cases, 6 regels); negatief herschreven +
  nieuw; werkvoorbeeld C in ref- en clingo-suite; frontend-JUnit; `EXPECTED_PLAYED` 281.
