# WF-034, vierde golf: naamresolutie op de verdeling en de foldbron-terugval

Goal: de derde golf sloot de leeg-veld-klasse; de lenzen erop maten de
naamresolutie-klasse ernaast, drie vlakken, alle verifier-schoon en alle
gedocumenteerd in de open-lijsten van de WF-034-indexrij (ch. 4.1,
ch. 4.6 (f)): (1) een verdelings-endpoint dat een attribuut noemt zonder
declaratie — zes naamvelden, gemeten op `bestaatniet`: bron-/doel-/
restantAttribuut falen luid in beide executors, maar `ratoAttribuut` en
`groepeerAttribuut` antwoorden STIL FOUT in beide (leeg-waar-60,
300-waar-1200) en `maximumAttribuut` splijt het vierkant (ref stil 600
waar 300 hoort, clingo weigert) — de plek waar alleen de ankerpinning
vangt; (2) `bronType`/`doelType` null op de verdeling (11/11
verifier-schoon, beide executors luid `missing bronType`/`doelType`);
(3) de ch. 4.3-terugval overschrijdt zijn scoping: de spec beperkt de
uitzondering tot de row-1-ROLnaam van de instance-fold, maar
`aggregation_source_check` past de `connecting_role_candidates`-terugval
op élke foldbron toe — gemeten op een waarde-SOM in een anchor-golden:
een niet-bestaand (of leeg) attribuut is verifier-schoon, ref telt STIL
0 waar 12 hoort, clingo weigert. Route als de eerdere golven:
WF-034-clausules (naamresolutie tegen de declaratietabel; de terugval
terug naar zijn row-1-scoping), elk rood-eerst met eigen negatief,
clausule-exact mutant-bewezen, nul over-afwijzing over de vier zones —
met bijzondere aandacht voor de twee legitieme telfolds die vandaag op
de brede terugval leunen (gemeten: beide executors spelen ze identiek;
de vernauwing mag ze niet raken).

Acceptance:
- Per vlak minstens één negatief, rood-eerst zichtbaar; de stille
  vormen (rato/maximum/groepeer, de waarde-SOM) elk gepind.
- Sweeps vóór/na met nul over-afwijzing; de open-lijsten in IR-SPEC
  worden korter en blijven kloppend.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

Out of scope: executors, frontends, de rij-1-wart zelf (de rolnaam-
terugval binnen zijn scoping), de 9 fold-argumenttypes, WF-007's
payload-blindevlek.

---

Result (2026-09-04): **geland als voorstel op het beschermde pad** — drie
WF-034-clausules, zeven negatiefs, alle dertien gates groen. Geen nieuw
regelnummer, geen gedocumenteerde uitstelpost.

**Rood-eerst, letterlijk gemeten (commit `8cea4c9`, gate 3 rood).**

Vlak 1, `bestaatniet` per veld één voor één over alle golden-verdelingen,
verifier-SCHOON: `bronAttribuut` 11/11, `doelAttribuut` 11/11,
`restantAttribuut` 9/9, `ratoAttribuut` 7/7, `maximumAttribuut` 4/4,
`groepeerAttribuut` 3/3. Uitvoerders:

    rato      @ anchor/jutterswet-bergingstoezicht  BEIDE STIL
              ref    MISMATCH: J1.bergingsaandeel = leeg, expected 60
              clingo MISMATCH: J1.bergingsaandeel is underived (leeg), expected 60
    groepeer  @ jutterswet-rangaflopend             BEIDE STIL EN EENS
              ref    MISMATCH: J1.sluitdeel = 300, expected 1200
              clingo MISMATCH: J1.sluitdeel = [300], expected 1200
    maximum   @ jutterswet-berging                  REF != CLINGO
              ref    MISMATCH: W.bergingsaandeel = 600, expected 300
              clingo TRANSLATION FAULT: unknown attribute "bestaatniet" on "Berger"
    bron      @ jutterswet-berging                  BEIDE LUID
              ref    EVALUATION FAULT: empty source amount in proportional allocation
              clingo TRANSLATION FAULT: unknown attribute "bestaatniet" on "Berging"

Vlak 2, null: `bronType` 11/11 en `doelType` 11/11 verifier-schoon; ref
`EVALUATION FAULT: missing bronType in IR node`, clingo `TRANSLATION FAULT:
missing bronType` (idem doelType). Een *niet-gedeclareerd* type is wél al
gedekt — met binding WF-047, zonder binding WF-048, beide gemeten luid; het
gat is precies de null.

Vlak 3, de ch. 4.3-terugval buiten zijn scoping, twee takken:

    typenpaar-tak @ anchor/jutterswet-maatschap (SOM, argument met ECHT datatype)
      attribuut "bestaatniet" -> verifier PASS
      ref    MISMATCH: J1.maatwaarde = 0, expected 12 / J2 = 0, expected 30
      clingo TRANSLATION FAULT: unknown attribute "bestaatniet" on "Jutter"
    rolnaam-tak   @ jutterswet-vondsttelling (SOM, argument met ECHT datatype)
      attribuut "vondsten" (gedeclareerd meervoud) -> verifier PASS
      ref    MISMATCH: J1.aangemeld gewicht = 0, expected 3.75
      clingo TRANSLATION FAULT: unknown attribute "vondsten" on "Vondst"

**Wat er landde.** `wf034_verdeling_naamresolutie` (elke gevulde naam
resolveert tegen zijn eigen kant: bron/restant tegen `bronType`,
doel/rato/maximum/groepeer tegen `doelType`), `wf034_verdeling_typen`
(beide typen genoemd), en de scoping-vernauwing in
`aggregation_source_check`: de rij-1-wrat is het foldbron-argument dat de
onbekend-sentinel op zijn eigen type draagt — de discriminator waarop
beide uitvoerders hun rolnaam-tak al sleutelen — en buiten die wrat is een
foldbron een gewone attribuutreferentie.

**Census-lezing:** "benoemde foldbron" = het argument spelt zowel zijn
`attribuut` als zijn `objecttype` — het paar waarop deze arm resolveert.
Tel de ene bron mee die wél een attribuut en géén objecttype spelt (een
golf-1-vorm, daar al afgewezen) en de telling leest 90/58 in plaats van
89/57.

**Regressiegrens gemeten (de twee telfolds en hun buren), ná de
vernauwing:** `jutterswet-kaveltelling` verifier PASS, ref 5 / clingo 5
verwachtingen; `jutterswet-aanmeldertelling` PASS, 3 / 3;
`jutterswet-vondsttelling` PASS, 7 / 7; `anchor/jutterswet-maatschap`
PASS, 7 / 7. Exacte census over de vier zones: 89 benoemde foldbronnen —
32 wrat (24 via rolnaam, 5 via typenpaar, 3 die deze regel al afwees) en
57 getypeerd (54 gedeclareerd attribuut, 1 kenmerk via WF-080, 2 nieuwe
getuigen). `jutterswet-kaveltelling` is de énige golden die op de
typenpaar-terugval leunt en is daarmee de staande regressiegetuige.

**Nul over-afwijzing, byte-sweep over alle vier zones (486 documenten:
153 golden + 22 rood + 281 negatief + 30 parity — de tellingen ná de
fixronde):** 15 rapporten veranderen, 10 verdicts — de tien nieuwe
getuigen (zeven van de golf, drie van de fixronde). Geen enkel golden- of
rood-document beweegt. Vijf documenten houden hun verdict en krijgen
waarachtige incidentele regels: `wf020-crossform-collision`,
`wf024-complex`, `wf025-max-without-ratio`,
`wf026-two-allocations-one-target` (minimale fragmenten zonder typen, elk
nog steeds afgewezen voor de regel die zijn map declareert) en de
parity-casus, waarvan het eigen `doelAttribuut` een attribuut noemt dat
het doeltype niet declareert — daar **zes** regels, één per verdeling,
niet één.

**Mutantbewijs, elk over de volle 486-document-sweep (hermeten in de
fixronde):**

    wf034_verdeling_naamresolutie geschrapt   7 rapporten, 6 verdicts
                                              (de zes -onbekend- getuigen)
    wf034_verdeling_typen geschrapt           6 rapporten, 2 verdicts
                                              (de twee -zonder-type-getuigen)
    wrat-guard van de rolnaam-tak weg         1 rapport, 1 verdict
                                              (wf034-foldbron-rolnaam-waardevouw)
    terugval weer buiten de wrat bereikbaar   1 rapport, 1 verdict
                                              (wf034-foldbron-onbekend-attribuut)

De laatste regel citeert de formulering van de NOTE, want het rapportgetal
is lezingsafhankelijk: precies zoals de NOTE de mutant voorschrijft (de
"gewone attribuutreferentie"-arm óók bewaken met "geen unieke verbindende
rol") verandert er 1 rapport; schrap je in plaats daarvan de hele
buiten-de-wrat-arm, dan wisselt de zustergetuige van boodschap en worden
het 2 rapporten. Het verdict is in beide lezingen hetzelfde ene document.
De arm-exacte mutanten per deelarm staan in de zes NOTE's: één [resolve]-
regel weg vrijwaart precies één document (op `doelAttribuut` plus de zes
incidentele parity-regels), één type-arm weg idem.

De twee foldbron-takken zijn tak-exact doordat hun getuigen op
verschillende goldens staan: op `vondsttelling` levert het paar
`Vondst`/`Vondst` NUL kandidaten, dus alleen de rolnaam-route kan daar iets
toelaten; op `maatschap` levert de wederkerige zelfrelatie er één voor
elke naam. De eerste plaatsing van de rolnaam-getuige (op `maatschap`) was
rood maar niet tak-exact en is daarom in commit `b3591c1` verhuisd — met
de meting erbij.

**Afwijkingen van de kaart, alle gerapporteerd:**
- Zeven negatiefs in plaats van de geschatte ~5: de foldbron-vernauwing
  heeft twee apart falsifieerbare takken en die verdienen elk een getuige;
  negatiefzone 271 → **278**, niet 276 (en 278 → **281** in de fixronde,
  die de drie deelarmen zonder getuige er alsnog een gaf).
- De kaart noemde `jutterswet-aanmeldertelling` en `jutterswet-kaveltelling`
  als de twee telfolds "die vandaag op de brede terugval leunen". Gemeten:
  alleen `kaveltelling` gebruikt de typenpaar-terugval (het gesynthetiseerde
  meervoud `duinkavels`); `aanmeldertelling` resolveert via het
  *gedeclareerde* meervoud `vondsten` en zit dus op de rolnaam-route. Beide
  zijn wratten en beide houden hun route; de meting staat hierboven.
- De kaartformulering "de terugval mag alleen een naam accepteren die een
  DECLARED ROLE resolveert" is niet letterlijk uitvoerbaar: dan zou
  `kaveltelling`'s gesynthetiseerde meervoud — juist de reden dat de
  terugval bestaat — afgewezen worden. Uitgevoerd is de scoping die de
  spec noemt: de uitzondering geldt alleen in de rij-1-wrat, en de wrat is
  op de wire de onbekend-sentinel. Beide routes (rolnaam én typenpaar)
  blijven binnen de wrat volledig intact.
- De verifier-commit-boodschap `6052fb1` noemt een census van 29/22/7/53;
  dat was een ruwe eerste telling. De exacte is 89/32(24+5+3)/57(54+1+2) en
  staat in spec, NOTE's en verifier-commentaar; de correctie is opgeschreven
  in commit `bd2a684`.

**Open gebleven, ongewijzigd en gemeten in de open-lijst:** de sentinel op
de 9 fold-/aggregatie-argumenttypes; de rij-1-wrat zelf (nu binnen zijn
vernauwde scoping) met zijn 15 no-op-posities; WF-007's
payload-blindevlek. Restwijdte van de wrat, in de fixronde geteld over de
vier zones: van de 32 wratbronnen staan er **26** op een typenpaar met
precies één verbindende rol — daar aanvaardt de terugval élke naam — en op
**18** daarvan kruist WF-047 de geschreven naam tegen de gebonden rol van
een expliciete binding. Netto blijven **acht** posities over waar een
willekeurige naam gewoon blijft staan, waarvan **twee** golden:
`jutterswet-aanmeldertelling` en `jutterswet-kaveltelling`, de twee
bindingloze telfolds. Dat is precies wat de wrat een document nog oplevert.

**Fixronde (2026-09-04, na twee lenzen).** Beide lenzen bevestigden de
landing — elk cijfer in dit Result gereproduceerd, nul over-afwijzing
opnieuw gemeten — en leverden één inhoudelijke vangst plus een lijst stale
zinnen. Alles hieronder is opnieuw gemeten, niet overgeschreven.

*Gefixt, met getuige.* Drie deelarmen droegen een clausule zonder document:
`restantAttribuut` en `doelAttribuut` binnen `wf034_verdeling_naamresolutie`,
en de `doelType`-arm van `wf034_verdeling_typen`. Voor `restantAttribuut` was
dat letterlijk zichtbaar: die ene `resolve`-regel weghalen veranderde géén
enkel document in de vier zones. Elk kreeg een eenregelige getuige vanaf
`corpus/golden/jutterswet-berging`, elk rood-eerst aangetoond — PASS onder de
verifier van vóór deze branch (`ec7d188`), één regel eronder — en elk
arm-exact: alleen die arm weg → alleen dat document schoon (1, 2 en 4
rapporten, telkens 1 verdict).

*Gefixt, tekstueel.* Zes reparaties, elk gemeten gefalsifieerd: het
commentaarblok van `wf034_verdelingen` noemde de naamresolutie en de
typevelden "OPEN" terwijl beide zeventig regels lager gesloten zijn; "IN
THESE THREE WAVES" telde de eigen golf niet mee; "one witness carries both
halves of the pair" werd door de derde getuige onwaar; de kale getallenreeks
`11/11/9/7/4/3` botste met de veldopsomming ervoor (doel en restant wisselen
positioneel) en draagt nu namen; "both measured loud" gold voor de regels,
niet voor de uitvoerders (een ongedeclareerd `bronType` laat `ref` stil met
alle aandelen leeg waar 300/200/300 + 400 gepind staat, clingo weigert); en
"every executor keys on that sentinel" is onwaar voor de derde uitvoerder —
de Drools-backend sleutelt op `kind = AANTAL` plus eigen rolafleiding en
speelt een echt-datatype-bron die `ref` en clingo weigeren. Daarnaast vijf
kleine bijzinnen: de census-lezing (89/57 geldt onder "spelt attribuut ÉN
objecttype"; anders 90/58), de restwijdte van de wrat (hierboven), de
zesvoudige parity-regel, de lezingsafhankelijkheid van de terugval-mutant, en
de KENMERKnaam op een verdelingsveld (valt onder deze WF-034-arm waar de
zusterposities WF-080 melden — gemeten onschadelijk, 5/5 afgewezen, maar
afwijkend en nu opgeschreven).

*Nieuwe tellingen.* Negatiefzone 278 → **281**, vier zones 483 → **486**; de
sweep tegen `ec7d188` leest daarmee 15 rapporten en 10 verdicts in plaats van
12 en 7. De clausule-mutanten hermeten: naamresolutie geschrapt 7 rapporten /
6 verdicts, typen geschrapt 6 / 2. Alle vermeldingen van 483 en 278 in
IR-SPEC, kaart en NOTE's zijn meegetrokken.

*Open gebleven.* De open-lijst van de vierde golf staat ongewijzigd
hierboven. Wat de lenzen daarnaast maten hoort niet bij deze kaart en staat
in `backlog/new/20260904-wf034-vijfde-golf-v0-naamvelden.md`: de vier
naamvelden van een `ResolvedFeitCreatie` (14 golden-posities, nul
verifier-dekking, beide OCaml-uitvoerders blind, de Drools-backend crasht op
alle vier — verifier-schone invoer die een uitvoerder laat crashen);
`objectCreaties[].subject` op de toekenningsloze creatie (1 van de 11, een
anker-golden, `ref` en clingo eens over hetzelfde foute antwoord);
`Rollidmaatschap.anderTeFiller` (4/4 schoon, beide uitvoerders luid); en het
WF-028-gat — een feitCreatie in kale v0-beanvorm is verifier-PASS terwijl
`ref` en clingo hem weigeren en de Drools-backend hem speelt.

**Gates** (alle dertien, na de fixronde):

    check-self-contained: PASS
    check-ir-schema: PASS (schema valid; 175 file(s) validate and round-trip)
    check-verifier: PASS (175 golden clean; 278 negatief rejected for their declared rule)
    check-case-schema: PASS (schema valid; 425 case file(s) validate)
    check-oracle: PASS (403 cases replayed exactly; 3 goldens without a case file)
    check-clingo: PASS (403 cases replay exactly through clingo)
    check-noop: PASS (367 cases refute a do-nothing executor; 36 pinned survivors)
    check-rood: PASS (22 red cases fault observably in ref and clingo)
    check-catala: PASS — frontend suite green (201 checks); 12 catala goldens byte-identical
    check-regelspraak: PASS — JUnit suite green (171 tests); 29 rgs goldens byte-identical
    check-toka-parity: PASS (29 ref / 29 clingo / 29 Drools, 0 divergenties)
    check-drools: PASS (106 played, 43 auto-excluded with reason, 1 value-divergence, JUnit 199)
    check-reftype-derivation: PASS (52 unit annotations twisted over 11 goldens; 0 pinned survivors)
