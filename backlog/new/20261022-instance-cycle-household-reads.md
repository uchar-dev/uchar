# instance-cycle-household-reads: een lezing binnen het huishouden telt als lokaal of verschoven, en de weigering noemt de lezing

*Uit Fable-advies 2 van 2026-10-22 (`uchar-dev/research/bruggen-2026-10-22/fable-advies-2.md`, vraag 1, stap 1 van het
stappenplan, beslispunten B2.1–B2.3). Voorlopige besluiten van de hoofdsessie naar dat advies; de eigenaar kan elk
besluit omdraaien. Vervolg op `cycle-across-instances` (in `backlog/done/`).*

## Aanleiding

De afbakening van `cycle-across-instances` (IR-SPEC ch. 5.3) telt een lezing achter een interne rand van een
instantiegeordend deel alleen als lokaal of verschoven wanneer elke stap naar een to-one rol gaat. Meting 5 van die kaart
(`werk/bruggen/k12/m5/diag.txt`) laat in de v2-regelset herleving-WW vijf lezingen buiten die afbakening zien. Vier
ervan lezen binnen het eigen huishouden van het recht:

1. `herlevingsdatum van het recht art. 21 lid 1` vouwt over `eindigingsgrond.herlevingsdatum` (to-many satellieten);
2. `herlevingstermijn buiten toepassing art. 21 lid 5` toetst een kenmerk van de eindigingsgronden (een kwantor);
3. `herleving art. 21 lid 1` vouwt over `eindigingsgrond.herlevingsdatum`;
5. `eerste kalendermaand zonder overschrijding vanaf de eindmaand art. 21 lid 1` vouwt over de inkomens van het recht,
   met in het filter de beëindigingsdatum van het recht van het inkomen (de stap naar een satelliet en terug).

De vijfde, `herleving art. 21 lid 1` die de beëindigingsdatum van de rechten in de samenloop leest, is een lezing over
een hub; die blijft buiten de afbakening (B2.2, hieronder).

Verder zegt de verifier bij zo'n weigering alleen `WF-045 … <kring>`: welke lezing buiten de afbakening valt, moet de
lezer zelf zoeken. Dat is precies wat de werkbank nodig heeft.

## Voorlopige besluiten (2026-10-22, naar Fable-advies 2)

- **B2.1 Het huishouden.** Het *huishouden* van een object X van T is X met zijn satellieten over de satellietrelaties
  van T. Elk lid heeft de diepte van X (ch. 6.2). Een *huishoudstap* is een stap over een satellietrelatie van T: naar de
  satelliet (to-one of to-many; een to-many stap alleen als bereik van een vouw of kwantor, WF-053 weigert haar
  scalair) of terug naar de eigenaar (to-one).
  - **Lokaal:** nul of meer huishoudstappen vanaf het onderwerp (T of een satelliet).
  - **Verschoven:** eventueel één eigenaarsstap (als het onderwerp een satelliet is), dan één R-stap naar de
    voorganger, dan nul of meer huishoudstappen in het huishouden van de voorganger.
  - **Onveranderd blijft:** de schakelrelatie, één R en één rol per deel, elke lezing achter een interne rand
    verschoven of lokaal, één satellietrelatie per satelliettype, alleen regels en defaults-groepen, geen creatie van
    een recursieve groep, kringvrij zonder de verschoven randen, de niveaus, en EF-033.
  - **Het negatief `wf045-instance-cycle-to-many-satellite-step` vervalt.** Fable trekt het in: de kardinaliteit aan
    de satellietkant doet voor de dieptevolgorde niet ter zake, want alle satellieten van X hebben diepte(X). Het
    negatief gaat weg met reden, in de bestaande rijvorm van `corpus/negative/README.md`.
- **B2.2 Buiten de afbakening blijven,** met reden in ch. 5.3:
  - een hub, dat wil zeggen een relatie met aan de T-kant een to-many rol (`Samenloop`, `Register`); gemeten geeft die
    twee stabiele modellen (`proef/k12-2/hub/`);
  - een satelliet van een satelliet;
  - twee R-stappen;
  - een verzameling voorgangers.
  
  Er komt geen grondgraaftoets per casus.
- **B2.3 De weigering noemt de lezing.** Valt een sterk samenhangend deel met een schakelrelatie op een clausule van
  de afbakening, dan geeft de verifier naast de kring een eigen arm `WF-045.instance-cycle-read-outside`. Die heeft de
  parameters `form`, `node`, `path` en `clause`. Drools geeft dezelfde weigering in dezelfde zin.

## Doel

- Verifier `instance_ordering`:
  - `satellite_hop` laat de eis "to-one" vallen voor de stap naar de satelliet, en houdt hem voor de eigenaarsrol;
  - `classify` loopt over het pad, houdt het huidige type bij (T of een satelliettype) en aanvaardt de drie rijen van
    B2.1;
  - elke gebruikte satellietrelatie gaat in `sats`.
  
  De paden zijn er al (`df_paths`).
- Drools `Cycles.satelliteHop` en `classify` spiegelen de verifier. ref verandert niet, want `instance_orders` is
  gedeeld en de diepte van een satelliet loopt via `io_owners`; zie het of dat klopt. clingo verandert niet.
- Spec als voorstel:
  - IR-SPEC ch. 5.3 krijgt de drie definities en de redenen van B2.2. De zin "Without the to-one hops of a shifted read
    the read lands on a set" vervalt.
  - RUNTIME-SEMANTICS ch. 6.4 krijgt een voorbeeld met een vouw over de eigen satellieten.
  - De index krijgt de nieuwe arm.

## Acceptatie

- (a) **Rood eerst: een golden.** `jutterswet-vergunningtermijnen` is handgeschreven IR, met een `NOTE.md` met de
  handberekening. Het gaat om een `Vergunning` met to-many `Termijnen` en de volgende vormen:
  - `restant` als som over de eigen termijnen;
  - `verlengde einddag` als basiseinddag plus de som over de termijnen van de vorige vergunning;
  - een voorwaarde met alle termijnen;
  - een vouw met in het filter een lezing van de einddag van de houder.
  
  De casussen hebben ketens van één tot drie vergunningen en nul tot drie termijnen. De golden faalt gate 3 (WF-045)
  vóór de wijziging. Erna speelt hij in gates 3, 5, 6, 12 (beide takken) en 17. De volgordes worden gemeten met
  `werk/bruggen/k12/scripts/order_run.sh`. `EXPECTED_PLAYED` gaat plus één.
- (b) **Negatieven.**
  - `wf045-instance-cycle-to-many-satellite-step` gaat weg met reden.
  - Rood erbij komen `wf045-instance-cycle-satellite-of-satellite` en `wf045-instance-cycle-hub-negation`. Het laatste
    is de miniatuur van `proef/k12-2/hub/`, met de twee modellen in haar `NOTE.md`.
  - De nieuwe arm krijgt een `expect.txt` op elk bestaand negatief waar hij past.
  - Gate 3 blijft groen. De vloeren zijn ondergrenzen.
- (c) **Meting 6, zonder bestand in deze repository.** Het m5-document gaat door de verifier van de tak. Verwacht wordt
  dat precies één lezing buiten de afbakening overblijft, de samenlooplezing, en dat de nieuwe arm haar noemt.
- (d) De zeventien gates groen. De Result noemt de CONSTRUCTS-rij (rij 45 bijgewerkt) als voorgestelde tekst.

May touch: `spec/verifier/**`, `backends/drools/**` buiten de productieruntime, `ref/` (alleen als de gedeelde module het
vraagt), `corpus/golden/jutterswet-vergunningtermijnen/`, `corpus/negative/` (de genoemde gevallen en `README.md`),
`scripts/check-drools.sh` (alleen `EXPECTED_PLAYED`), `spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md` als voorstel,
deze kaart. Niet: `frontends/` (dat is `check-admits-instance-cycle`, na deze kaart), `backends/clingo/`.

## Open voor de werkbank (B2.5, normvraag)

De samenlooplezing verdwijnt alleen als de werkbank de uitzondering van art. 21 lid 2 onder a herschrijft, zodat het
nieuwe recht niet wordt herkend aan een afgeleide beëindigingsdatum. Er zijn twee wegen:

- (c2) een invoerkenmerk op het recht in de samenloop (aanbevolen);
- (c1) de opvolger over `eerdere recht`.

Daarna vervallen K12, N13 en afwijking 21 in v2.
