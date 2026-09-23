# dialect-defaults-niet-en-nesting: een uitzondering kan geen "niet" zeggen en niet op een uitzondering staan

*Status: onderzoeks- en ontwerpkaart, 23 september 2026, aangemaakt vanuit
de werkbank-sessie over de herbouw van herleving-ww. Levert een voorstel en
maintainersbesluiten, geen code; de bouwkaarten worden na de besluiten
gesneden.*

## Aanleiding: waar we het tegenkwamen

De werkbank (`~/Developer/uwv/regelspraak-werkbank`) gaat de regelset
herleving-ww (WW art. 17–21) opnieuw bouwen: een gegevensmodel gegrond in
het CGM, met `is een` en de prioritized defaults. Art. 21 WW is het
schoolvoorbeeld van een hoofdregel met uitzonderingen, en precies daar
schiet het dialect tekort. De tekst (BWBR0004045, toestand 2026-07-01, uit
`wetsanalyse/herleving-ww/dist/wetkaart.json`):

> **Art. 21 lid 1.** Indien het recht op uitkering op grond van artikel 20,
> eerste lid, onderdeel a of c, is geëindigd en vervolgens de omstandigheid
> die tot dat eindigen heeft geleid heeft opgehouden te bestaan, herleeft
> het recht op uitkering met inachtneming van het tweede lid, de in artikel
> 8 en het derde lid genoemde termijnen, het vierde lid en de op grond van
> het vijfde lid gestelde regels. Indien laatstelijk het recht op uitkering
> op grond van artikel 20, eerste lid, onderdeel d, is geëindigd, herleeft
> het recht op uitkering met ingang van de eerste dag van de kalendermaand
> waarin de werknemer een aanvraag tot herleving van het recht op
> WW-uitkering indient met inachtneming van het tweede lid en de in het
> derde lid genoemde termijnen.
>
> **Lid 2.** Het recht herleeft niet indien: a. een nieuw recht op
> uitkering ingevolge dit hoofdstuk ontstaat, waarvan het maandloon meer
> dan 87,5% bedraagt van het maandloon van de eerdere uitkering; of b. een
> recht op uitkering op grond van de Wet werk en inkomen naar
> arbeidsvermogen ontstaat.
>
> **Lid 3.** Een recht op uitkering dat is geëindigd: a. wegens een
> omstandigheid als bedoeld in artikel 19, eerste lid, onderdeel e, g, h, k
> of n; b. op grond van artikel 20, eerste lid, onderdeel c, als gevolg van
> het minder beschikbaar zijn voor arbeid dan zijn gemiddeld aantal
> arbeidsuren, bedoeld in artikel 16, tweede lid, wegens andere
> omstandigheden dan ziekte of arbeidsongeschiktheid of het volgen van
> scholing of opleiding, terzake waarvan de werknemer een uitkering
> ontvangt als bedoeld in artikel 19, eerste lid, onderdelen a, b, c of d;
> c. op grond van artikel 20, eerste lid, onderdeel d; of d. wegens een
> combinatie van de hier bedoelde omstandigheden; kan, ook indien deze
> omstandigheden zich aaneensluitend voordoen, slechts herleven indien de
> periode tussen de eindiging van het recht en het vervallen van de
> omstandigheid of omstandigheden als hier bedoeld niet langer is dan zes
> maanden.
>
> **Regeling herlevingstermijn WW, art. 1** (BWBR0016001). De termijn van
> zes maanden, bedoeld in artikel 21, derde lid, van de Werkloosheidswet
> blijft buiten toepassing ten aanzien van de werknemer, bedoeld in artikel
> 7 van de Regeling vrijstelling verplichtingen sociale zekerheidswetten.

De wetsanalyse legt de voorrang vast in
`wetsanalyse/herleving-ww/defaults.json`, met een letterlijke citaatcontrole:
art. 15 is de basis, daarboven 21.1, dan 21.2 en 21.3, en RHT art. 1 als
uitzondering op 21.3 (21.4 en de termijnen van art. 8 ontbreken daar nog).
Dat is een boom met "niet" erin, vier niveaus diep. Het is precies wat
`spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` al als WW-eis noteerde
(R1 "met inachtneming van" en R3 "exception-to-the-termination", §3.2
r. 128-145), en precies de vorm van het handanker
`corpus/golden/anchor/ww-herleving-inkomen/NOTE.md:81-93`:

```
Default grant  (besluit waar,  als: werkloos)                                          -- art. 15
  exceptie -> Default einde     (besluit onwaar, als: maandinkomen > inkomensgrens)    -- art. 20-1-c
    exceptie -> Default herleving (besluit waar,  als: omstandigheid vervallen)        -- art. 21-1
      exceptie -> Default blokkade (besluit onwaar, als: nieuw recht ontstaan
                                    EN nieuw maandloon > inkomensgrens)                -- art. 21-2-a
```

De IR, de verifier en ref ≡ clingo ≡ Drools dragen die boom (`besluit:
false`, geneste `Default.excepties`, RUNTIME-SEMANTICS ch. 7.9). Ook de
exec-model-vertaling in regelspraak-java verwerkt `besluit` en geneste
`excepties` al (`ExecModelTranspiler.compileDefault`). Alleen het
RegelSpraak-dialect kan de boom niet schrijven.

## Wat we gemeten hebben (2026-09-22, frontend-build van 22-09 14:53)

1. **"niet" in een uitzondering.** Bron (p2):

   ```
   Objecttype het Recht (mv: Rechten)
       is herleefd kenmerk (bijvoeglijk);
       is omstandigheid vervallen kenmerk (bijvoeglijk);
       is nieuw recht ontstaan kenmerk (bijvoeglijk);

   Regel herleving art 21 lid 1
       geldig altijd
           Een Recht is herleefd
           indien hij omstandigheid vervallen is.

   Uitzondering op herleving art 21 lid 1: geen herleving art 21 lid 2 a
       geldig altijd
           Een Recht is niet herleefd
           indien hij nieuw recht ontstaan is.
   ```

   Melding: `ERROR (line 11): Uitzondering op 'herleving art 21 lid 1':
   uitzondering 'geen herleving art 21 lid 2 a' schrijft een ander doel
   (Recht.niet herleefd (kenmerk)) dan de basisregel (Recht.herleefd
   (kenmerk)) — een uitzondering moet hetzelfde onderwerp en
   kenmerk/attribuut betreffen (§4, WF-050)`. De weigering is luid, maar
   "niet herleefd" wordt gelezen als de naam van een ander kenmerk. De
   melding zet de auteur dus op het verkeerde been.

2. **Uitzondering op een uitzondering.** Bron (p3, met een Boolean-attribuut
   om het "niet" te omzeilen):

   ```
   Regel herleving art 21 lid 1
       geldig altijd
           De herleving van een Recht moet gesteld worden op waar
           indien hij omstandigheid vervallen is.

   Uitzondering op herleving art 21 lid 1: geen herleving art 21 lid 2 a
       geldig altijd
           De herleving van een Recht moet gesteld worden op onwaar
           indien hij nieuw recht ontstaan is.

   Uitzondering op geen herleving art 21 lid 2 a: tegenuitzondering
       geldig altijd
           De herleving van een Recht moet gesteld worden op waar
           indien hij wia ontstaan is.
   ```

   Melding: `ERROR (line 17): Uitzondering op 'geen herleving art 21 lid 2 a':
   geen basisregel met die naam gevonden — de basis moet een gewone platte
   regel (kenmerktoekenning of gelijkstelling) zijn`. Vergelijk
   `frontends/regelspraak/grammar/DIVERGENCES.md:129-130` ("not authored from
   source in this card").

3. **De omweg via een Boolean- of enumeratie-attribuut als defaults-doel**
   haalt frontend, verifier en ref. Clingo weigert echter elk berekend
   booleaans doel ("a boolean computed target … has no encoding here") en
   een enumeratie als defaults-doel ("unscalable datatype"). Alleen doelen
   van het type getal of datum halen ref ≡ clingo.

4. **Wat vandaag wel werkt: het poortkenmerk.** "Met inachtneming van" wordt
   een hulpkenmerk dat de hoofdregel ontkend leest. De uitzonderingen worden
   positieve regels op dat hulpkenmerk, en een uitzondering op een
   uitzondering zakt weg in een voorwaarde:

   ```
   Regel herleving art. 21 lid 1, eerste volzin
       geldig altijd
           Een RechtOpUitkeringWw is herleefd
           indien hij aan alle volgende voorwaarden voldoet:
           - hij is herleefbaar geeindigd
           - zijn datum vervallen omstandigheid is gevuld
           - hij niet uitgesloten van herleving is.
   Regel geen herleving art. 21 lid 2, onder b
       geldig altijd
           Een RechtOpUitkeringWw is uitgesloten van herleving
           indien hij recht op wia ontstaan is.
   Regel geen herleving na de termijn, art. 21 lid 3
       geldig altijd
           Een RechtOpUitkeringWw is uitgesloten van herleving
           indien hij aan alle volgende voorwaarden voldoet:
           - hij is geeindigd op termijngrond
           - zijn datum vervallen omstandigheid is later dan zijn datum einde recht op uitkering plus 6 maanden.
   ```

   Frontend en verifier geven PASS, en ref ≡ clingo op elf verwachtingen.
   Het werkt, maar de wetsstructuur (lid 1 herleeft, lid 2 herleeft niet,
   RHT 1 schakelt lid 3 uit) staat niet meer in de bron. Die zit verstopt
   in een hulpkenmerk en in conjuncten.

## Wat RegelSpraak v2.3.0 zelf zegt

Uit de specificatie (`RegelSpraak-specificatie v2.3.0.md` in de
werkbank-toolchain, `specification/v2.3.0/`):

> **§3.5.** Een kenmerk is een eigenschap die een object wel of niet heeft.
> Default zal een kenmerk er niet zijn (onwaar), en alleen als het kenmerk
> in de invoer wordt gezet of als er een kenmerktoekenningsregel (zie
> paragraaf 9.2) toepasbaar is, kan het betreffende kenmerk waar zijn.

> **§9.2.** Belangrijk om hierbij op te merken is dat kenmerken onwaar zijn
> als er geen kenmerktoekenning is gedaan. Om die reden kan er ook geen
> negatieve kenmerktoekenning worden gedaan. Dus "Een Vlucht is niet
> duurzaam" is in RegelSpraak niet toegestaan.

uchar heeft dat bij de eerste defaults-uitbreiding zo overgenomen:
"Kenmerk semantics forbid negative attribution (§9.2) … an exception can
never *withdraw* a characteristic" (PRIORITIZED-DEFAULTS-EVOLUTIE.md §4.1,
r. 190-204), en de frontend "never emits `besluit: false`"
(DIVERGENCES.md:90-92). Daarnaast geldt RUNTIME-SEMANTICS ch. 5.3
(r. 3035 e.v.): kenmerken zijn monotoon binnen één evaluatie, ze staan
standaard op onwaar en worden alleen ooit waar gezet.

## Onderzoeksvraag van de maintainer: waarom kent RegelSpraak (en RuleSpeak) geen "niet &lt;kenmerk&gt;"?

Eerste lezing, te toetsen. Het argument van §9.2 is een gesloten-wereld-
argument: zonder toekenning is een kenmerk onwaar, dus een negatieve
toekenning is overbodig. In een vlakke taal zonder voorrang klopt dat, en
er is een tweede reden die de spec niet uitspreekt. Een negatieve naast een
positieve toekenning van hetzelfde kenmerk zou een conflict zijn waarvoor
de taal geen beslisregel heeft, en bij monotone evaluatie zou de uitkomst
van de vuurvolgorde afhangen.

Binnen een defaults-groep vallen beide redenen weg. Er is per doel precies
één schrijver en een voorrangsvolgorde, dus een "niet" op een hoger niveau
trekt een lager "waar" in zonder conflict. RUNTIME-SEMANTICS r. 4607 zegt
het al: "ch. 5.3's monotone write regime is untouched — the group decides".
Wat nog uitgezocht moet worden:

- (a) Is het gesloten-wereld-argument de hele reden in RegelSpraak, of
  speelt er meer? Denk aan de productieregel-herkomst (Drools, monotone
  inserts), stratificatie van negatie, en de vraag wat "niet" betekent
  tegenover een kenmerk dat in de *invoer* waar is gezet (§3.5).
- (b) RuleSpeak en SBVR: hoe behandelen die negatieve afleidingen en
  ontkende definities, en met welke motivering? Bronnen: de RuleSpeak
  Sentence Forms (Ross), SBVR (OMG) met modaliteiten als "it is prohibited
  that" en logische negatie, en de Nederlandse RuleSpeak-variant. Uit te
  zoeken, niets aangenomen.
- (c) Catala: `consequence equals false` in een uitzonderingsboom
  (`corpus/golden/jutterswet-penningrecht-catala/bron.catala_en:25`). Hoe
  voorkomt Catala het conflict: via voorrang, en een botsing is daar fataal.
- (d) Vormt "niet" binnen één defaults-groep een schending van ch. 5.3,
  of juist niet?

## De ontbrekende dialectvormen (de IR heeft ze al)

1. **Een negatieve kenmerkbeslissing in een uitzondering:** `Een Recht is
   niet herleefd indien …` → `besluit: false`.
2. **Een uitzondering op een uitzondering:** `Uitzondering op <naam van een
   uitzondering>: <naam>` → geneste `Default.excepties`.
3. **Verwant: invoer boven berekening.** De werkbank wil afgeleide waarden
   ook als invoer kunnen aannemen en een afleiding kunnen laten overrulen.
   - In Catala heet dat een `context`-variabele: de aanroeper overschrijft
     de eigen definitie van de scope. In uchar is dat besluit 6, "Reading B"
     (`spec/proposals/PHASE5-CATALA.md:72`, 225-250; C-Def als "single
     highest exception", PHASE4-DEFAULTS.md:92). Dat is geland als
     gegenereerde topdefault, maar alleen via een subscope-aanroep.
   - Voor casusinvoer geldt Reading A: ch. 7.8 zet de virtuele
     invoer-default *onder* de gewone defaults. In het dialect geeft alleen
     `moet geïnitialiseerd worden op` voorrang aan de invoer, en een
     initialisatie mag geen defaults-basis zijn.
   - Een oppervlak voor "deze afleiding mag door invoer worden
     overschreven" ontbreekt dus. Misschien een eigen kaart.

## Te beslissen door de maintainer

1. De uitkomst van de onderzoeksvraag: is "niet" in een uitzondering een
   legitieme uitbreiding, of botst het met een ontwerpprincipe van
   RegelSpraak dat we willen houden?
2. De reikwijdte van "niet": alleen binnen `Uitzondering op` (één schrijver,
   geen conflict), of ook in een gewone regel (dan is conflictsemantiek
   nodig)?
3. De spelling per kenmerksoort: bijvoeglijk "is niet X", zelfstandig "is
   geen X", bezittelijk "heeft geen X". Aansluiten op de ontkenningsvormen
   die voorwaarden al kennen, en de misleidende melding uit meting 1
   vervangen door een gerichte.
4. Nesting:
   - de vorm (`Uitzondering op <uitzonderingsnaam>`);
   - de volgorde tussen broers (later geschreven = hoger, zoals nu);
   - de maximale diepte;
   - neemt een geneste uitzondering de rechtvaardiging van haar drager
     impliciet mee? Nu schrijft de auteur de basisvoorwaarde zelf uit, en
     RUNTIME-SEMANTICS r. 4544-4549 wil zelfstandige rechtvaardigingen.
5. Samenhang met overerving. Het kind wint per blok, ook voor
   uitzonderingen (`spec/proposals/SUBTYPING.md:1446-1468`). Mag een subtype
   een geneste uitzondering onder een geërfde uitzondering hangen?
6. Samenhang met `Uitzonderingen ongeordend` onder een uitzondering. De
   Catala-frontend kent "below-root branching" al
   (`frontends/catala/lib/translate.ml:28-31`).
7. Vorm 3, invoer boven berekening:
   - in deze kaart of in een eigen kaart;
   - per attribuut of per groep;
   - hoe de uitleg "opgegeven, niet afgeleid" toont;
   - wat er gebeurt als feiten en opgave elkaar tegenspreken (een melding
     via het §9.5-kanaal?).
8. Het volgwerk in regelspraak-java: in de bouwkaarten, of eigen kaarten
   daar. Zie de bijvangst hieronder.

## Bijvangst (elk een eigen kaart waard)

- Een `Regelgroep` sluit stil af bij de eerste `Uitzondering op`; die
  uitzondering hoort er dan niet bij.
- `// Bron:`-annotaties verdwijnen als een regel in een defaults-groep
  wordt opgenomen (RuleChecker: "no per-group bron", "No per-default bron").
  De werkbank-regelsets hangen hun herleidbaarheid per wetslid aan die
  annotaties.
- regelspraak-java: `testDocument` (de scenariotoetsen) kent de
  uitzonderingsvormen niet (`RegelSpraak.g4:88-90`), dus een regelset met
  defaults is niet met scenario's te toetsen. De playground-graaf, de
  uitleg en de dekking lezen alleen `resolvedRegels`, dus defaults-families
  zijn onzichtbaar in de werkbank-UI.
- Gemeten, oorzaak niet uitgezocht: gewone regels op super- én subtype
  voor hetzelfde doel, zonder defaults-groep in de keten, geven "no
  fixpoint" in ref en twee waarden in clingo, terwijl de verifier niets
  meldt.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een voorstel in `spec/proposals/` (naam bij oppakken) met:
  - het onderzoek naar de "niet"-vraag: RegelSpraak §3.5/§9.2,
    RuleSpeak/SBVR, Catala;
  - de dialectvormen 1 en 2, en vorm 3 of een verwijzing naar een eigen
    kaart;
  - de WF-regels met negatieve getuigen;
  - een goldenplan, met ten minste de RegelSpraak-tweeling van
    `anchor/ww-herleving-inkomen` (frontend-diff tegen het handanker, de
    penningrecht-methode), groen in ref ≡ clingo ≡ Drools.
- (b) De besluiten 1–8 vastgelegd in het voorstel.
- (c) De bouwkaarten gesneden: frontend-dialect (grammatica, RuleChecker,
  DIVERGENCES.md), eventueel verifier, en het volgwerk in regelspraak-java.
- (d) Geen code; alle poorten ongewijzigd groen.

May touch: `spec/proposals/**` (het nieuwe voorstel), `backlog/**` (deze
kaart verplaatsen en de vervolgkaarten). Niet in deze kaart: grammatica,
frontend, verifier, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`.

## Result (2026-09-23)

Geleverd: `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` (Engels,
met markering citaat/parafrase/gevolgtrekking/meting), tien conceptkaarten
in `backlog/new/`, geen code. Alle besluiten 1–8 staan er als voorstel in;
ze wachten op de maintainer.

**Wat het onderzoek vond.**

- (a) RegelSpraak noemt één reden (§9.2: gesloten wereld). Twee andere zijn
  even structureel maar staan nergens: zonder voorrang is een negatieve
  naast een positieve toekenning een onbeslisbaar conflict (de
  SBVR/RuleSpeak-afkomst van §2.1), en de uitvoering is monotoon zonder
  terugtrekking (de Drools-backend gebruikt geen truth maintenance; de
  stratificatie leunt op "a missed positive is recoverable; a consumed
  negation is not"). Binnen een defaults-groep geldt geen van de drie.
  Tegenover een invoerkenmerk betekent "niet": niet toekennen, niet
  intrekken — gemeten in ref, clingo en Drools-exec.
- (b) RuleSpeak kent "must not" en (Ross 2010) "is never considered",
  SBVR kent logische negatie en "it is prohibited/impossible that", maar
  geen van beide kent voorrang: uitzonderingen worden weggeherformuleerd
  (SBVR 1.5 §16.3.2–16.3.3, Ross 2009 wijst een prioriteitsschema expliciet
  af). De Nederlandse RuleSpeak (2009) heeft geen negatieve afleidingsvorm.
- (c) Catala heeft precies de gezochte combinatie: `condition` met default
  false plus `not fulfilled`, structurele voorrang, fatale botsing —
  gemeten met catala 1.2.1 (ook: geneste uitzonderingen dragen de
  voorwaarde van hun drager níet mee).
- (d) Ch. 5.3 blijft heel: besluit false schrijft niets (ref eval.ml:3165,
  clingo translate.ml:8063, Drools exec :7649), de groep beslist na al haar
  lezingen (WF-051).
- Draad, verifier en drie uitvoerders dragen beide vormen al; het werk is
  frontendwerk; geen nieuwe WF-regel. Wel een getuigegat: de waardeclausule
  van WF-022 heeft een verifiercheck maar geen eigen negatief.
- Bevinding in het handanker `ww-herleving-inkomen`: de geneste
  rechtvaardigingen missen de voorwaarde van hun drager, waardoor het anker
  op twee niet geraakte invoeren tegen art. 21 in beslist (in alle drie de
  uitvoerders gelijk). Met de dragervoorwaarden erin slagen alle zeven
  proefcases (4 bestaande + 2 hoeken + de invoercase).
- Bijvangst 4 heeft een oorzaak: het WF-079-commentaar (checks.ml:8899)
  veronderstelt dat de kale subtype-regel de supertype-regel vervangt, maar
  ch. 7.10 vouwt alleen als de keten een defaults-groep heeft. Gemeten:
  verifier PASS, ref en Drools-exec geen fixpunt, clingo twee waarden,
  DRL-tekst volgt de bronvolgorde.

**Wat niet gevonden of niet gedaan is.** De pagina van het Regelregister
over RuleSpeak was onbereikbaar (DNS). Geen Nederlandse tegenhanger van
"is never considered", geen bespreking van ontkende definities bij Ross
(2006), geen voorrangsmechanisme in SBVR. De tweede en derde reden van (a)
zijn gevolgtrekkingen. De scenarioharnas en graaf van regelspraak-java zijn
gelezen, niet gedraaid. Alleen gate 1 is gedraaid (PASS); de overige
poorten niet — er is geen code, corpus of script gewijzigd. De metingen
draaiden op kopieën buiten de repo; in de worktree staat alleen de
genegeerde `_build/` van de OCaml-bouw.

**Conceptkaarten** (`backlog/new/`): bouwkaarten
`20260923-dialect-niet-nesting-frontend`, `-dialect-niet-nesting-goldens`,
`-anker-ww-herleving-hoeken` (beschermd pad), `-wf022-waardeclausule-getuige`,
`-regelspraak-java-dialect-pinbump` (hoort in regelspraak-java),
`-invoer-boven-berekening` (vorm 3); bijvangstkaarten
`-regelgroep-uitzondering-afsluiting`, `-bron-annotaties-defaults`,
`-testdocument-uitzonderingsvormen`, `-keten-kale-regels-divergentie`.

**Registerzin voor `spec/CONSTRUCTS.md` rij 8** (de hoofdsessie landt hem):

> **Dialect negation and nesting (proposed 2026-09-23,
> `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`; awaits decisions
> 1–8):** an `Uitzondering op` block may decide `niet`/`geen` — lowered to
> `besluit: false`, a displacement that writes nothing, so ch. 5.3's
> monotone regime and an input-set characteristic stay untouched — and may
> name another exception, lowered into that exception's
> `Default.excepties` while first-level exceptions stay chain positions so
> that ch. 7.10's base override keeps them; `Uitzonderingen ongeordend op
> <naam>:` adds unordered exceptions under a rule; a plain `Regel` still
> cannot negate (§9.2, refused by the frontend, WF-022 on the wire); no
> wire change and no new WF rule, the witnesses being RegelSpraak twins of
> `anchor/ww-herleving-inkomen`, `anchor/jutterswet-uitsluiting` and
> `anchor/jutterswet-nestkenmerk`.
