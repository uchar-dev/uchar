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
