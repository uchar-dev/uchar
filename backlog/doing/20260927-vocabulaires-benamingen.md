# vocabulaires-benamingen: benoemde vocabulaires met een benaming per declaratie, als zijtafel zonder semantiek

De begrippenkaart van een uitvoeringsorganisatie kent per begrip een naam
per laag (wet, handboek, bedrijfsregels, canoniek gegevensmodel). Een
regelset spreekt één taal — de gedeclareerde namen — maar de dienst, de
uitleg en de koppelingen willen de andere lagen kunnen spreken, en de
vertaling daartussen moet ondubbelzinnig zijn. Maintainersbesluit
(2026-09-27): dat wordt een taalkenmerk van uchar, geen commentaarconventie
en geen mappingbestand. Prior art (research/vocabulaires-2026-09-27/
prior-art.md, 2026-09-27): het patroon bestaat — ISO 1087 (designations),
SBVR (designations per speech community), OMG MVF 1.1 (vocabulaire per
gemeenschap, actief vocabulaire, vertaling), XBRL met de EFM-regels (één
naam per element per vocabulaire, verplicht standaardvocabulaire,
uniciteit), Gellish, SNOMED (taal- en dialectreferentiesets, keuze per
verzoek) — en wat uchar toevoegt is de combinatie: benamingen als zuivere
metadata op alle declaratiesoorten van een normatieve IR, uniciteit als
verifierregel, en dezelfde vocabulaires voor een dienstcontract en voor
uitleg. Woorden: **vocabulaire** voor de verzameling, **benaming** voor de
afzonderlijke naam (Engels: vocabulary, designation); niet "dialect"
(uchars laag boven de kern), "label" (dimensielabels) of "alias"
(resolverend in SQL, Ada, L4).

## U-besluiten (hoofdsessie, 2026-09-27)

- U1 Syntaxis (RegelSpraak-frontend, uchars dialect, additief en
  strippbaar): een declaratieblok naast de eenhedenstelsels en dagsoorten,
  `Vocabulaire <naam>` met optioneel `(standaard)` en optioneel een
  taalcode en een soort (`code` of `term`; standaard `term`); op een
  declaratie een annotatie `(<vocabulaire>: <benaming>)` na de naam en na
  `(mv: …)`, voor objecttype, attribuut, kenmerk, feittype, rol,
  enumeratiewaarde, parameter, regel en regelgroep; hooguit één benaming
  per declaratie per vocabulaire; een benaming van een code-vocabulaire
  voldoet aan een tekenpatroon dat in OpenAPI en JSON geldig is (de bouwer
  kiest en documenteert het); een document zonder vocabulaires parseert
  en emitteert byte-gelijk als nu. Regels blijven in de canonieke naam
  geschreven: een benaming resolveert NOOIT in regeltekst, ook niet als
  ze toevallig een canonieke naam spelt.
- U2 IR (`uchar-ir-v1`, L5-spelling): op het document `vocabularies`
  (naam, `default`, `language`, `kind`) en per declaratie `designations`
  (vocabulaire → benaming), als zijtafel zonder semantiek; sleutelvolgorde
  canoniek in het schema; ref en clingo lezen en negeren (nooit een
  weigering); de Drools-lader draagt de tafel in het model met een
  publieke leesingang (vocabulaire → benaming van een element, en de
  omkering), zonder wijziging aan beans, DRL of exec-model.
- U3 Verifier, nieuwe WF-regels met arm per oorzaak (nummers na WF-115):
  een benaming in een niet-gedeclareerd vocabulaire; twee benamingen voor
  hetzelfde vocabulaire op één declaratie; uniciteit van de EFFECTIEVE
  naam per vocabulaire (benaming als die er is, anders de canonieke naam)
  met dezelfde reikwijdte als de canonieke namen: attributen en kenmerken
  per objecttype, rollen per feittype, enumeratiewaarden per domein, de
  overige soorten globaal — zodat de omkering per vocabulaire
  deterministisch is, ook voor een onvolledig vocabulaire; een vocabulaire
  met `standaard` is volledig (elke declaratie heeft er een benaming;
  ERROR), twee standaardvocabulaires is een fout; een benaming die de
  canonieke naam van hetzelfde element spelt is toegestaan maar overbodig
  (WARNING).
- U4 Neutraliteit: een been in gate 13 (annotatie-onverschilligheid) naar
  het bestaande model: elke golden met vocabulaires wordt gespeeld met de
  benamingen gestript en met de benamingen verwisseld, en ref, clingo en
  beide Drools-takken geven de gepinde uitkomsten; een lege set is een
  fout; vloer in het script. Voorstel van de bouwer, landing door de
  hoofdsessie (beschermd pad).
- U5 Getuigen: een Jutterswet-golden met de vocabulaires `handboek`
  (term) en `cgm` (code, standaard) op alle declaratiesoorten inclusief
  enumeratiewaarden, rollen en regels; negatieve getuigen per arm van U3
  (`corpus/negatief/wf###-…`, met `expect.txt`); frontendtests voor de
  syntaxis en de weigeringen; `bron.rgs` re-emitteert byte-gelijk (gate
  10); de pariteitszone en de bestaande goldens onveranderd.
- U6 Register: DIVERGENCES.md sectie "Vocabulaires en benamingen"; IR-SPEC
  (hoofdstuk 2: de zijtafel; de WF-rijen) en het schema als diff van de
  bouwer, geland door de hoofdsessie; CONSTRUCTS-rij en PLAN §9-zin in
  het Result (de hoofdsessie landt ze).
- U7 Niet in deze kaart, alleen benoemen: de dienst per vocabulaire (kaart
  in de distributie), de lint tegen de begrippenkaart (werkbank), een
  deprecatiestatus per benaming en meertaligheid (later), een
  strip-uitvoer naar kale RegelSpraak (later), benamingen in het
  caseformaat (cases blijven canoniek).

## Acceptatie

U1–U5 gebouwd met de getuigen; alle zestien gates groen; het Result noemt
de gekozen tekenpatronen, de weigeringszinnen uit echte uitvoer, de
WF-nummers, en de tellingen van gates 3, 10 en 13.

Mag raken: `frontends/regelspraak/` (grammatica, resolver, emitter,
DIVERGENCES, tests), `spec/IR-SPEC.md`, `spec/schema/`, `spec/verifier/`
(als diff, hoofdsessie landt), `backends/drools/` (lader en model, geen
beans/DRL), `ref/` en `backends/clingo/` alleen om de zijtafel te
accepteren, `corpus/golden/` en `corpus/negatief/` (nieuwe getuigen),
`scripts/check-reftype-derivation.sh` (voorstel voor het been). Niet:
`toka/`, de uitvoerende semantiek, het caseformaat.
