# WF-034, vijfde golf: de v0-naamvelden buiten de expressiewandeling

Goal: de vierde golf sloot de naamresolutie op de verdeling en bracht de
ch. 4.3-terugval terug binnen zijn scoping; de lenzen erop maten drie
naamvlakken die de wandeling nog steeds niet raakt, plus één gat in WF-028.
Alle vier zijn verifier-schoon gemeten en alle vier hebben een uitvoerder die
er anders mee omgaat dan de andere twee. Route als de vorige golven: een
WF-034-clausule per vlak (voor het WF-028-gat een arm op WF-028 zelf), elk
rood-eerst met eigen negatief, arm-exact mutant-bewezen, nul over-afwijzing
over de vier zones.

**R1 (zwaarst, voorrang) — de vier naamvelden van een `ResolvedFeitCreatie`.**
`zetVeld`, `joinVeld`, `tussenstappen[].type` en `tussenstappen[].veld` zijn
namen die nergens tegen een declaratie worden gehouden: **14 golden-posities**
(4 + 4 + 3 + 3, over `anchor/jutterswet-erflijn`,
`anchor/jutterswet-gemeenteberging` en `jutterswet-postwacht`). Gemeten, elk
veld één voor één op `bestaatniet`:

    verifier   14/14 PASS
    ref        14/14 CLEAN — replay tot de gepinde verwachtingen
    clingo     14/14 CLEAN — idem

Beide OCaml-uitvoerders zijn hier *blind*: zij lezen de symbolische velden
(`feittype`, `ankerRol`, `doelRol`, `joinRol`, `tussenstappen[].rolBinding`)
en raken de beanvelden niet aan, dus een onzinnige naam verandert hun antwoord
niet en er is geen verschil om te meten. De Drools-backend leest ze wél, en
reflectief; op `jutterswet-postwacht` (baseline: PLAYED) crasht hij op elk van
de vier:

    zetVeld              IllegalStateException: geen getter voor 'bestaatniet'
                         op ...facts.Sein   (via ConsequenceException)
    joinVeld             ConstraintEvaluationException: Error evaluating
                         constraint '' in  and in more rules
    tussenstappen[].veld ConstraintEvaluationException, idem
    tussenstappen[].type NullPointerException: Cannot read field "naam"
                         because "ot" is null

Dat is exact de vorm die deze regelfamilie het scherpst motiveert en die
`wf034-regeldoel-zonder-type` al eens droeg: **verifier-schone invoer die een
uitvoerder laat crashen**. Nu vier keer, op één knooppunt, en met een
golden die de gate speelt.

**Het WF-028-gat (voorrang, naast R1).** Een `ResolvedFeitCreatie` waarvan
*alle* extensiesleutels weg zijn — `feittype`, `ankerRol`, `doelRol`,
`joinRol` en `tussenstappen[].rolBinding` — is een kale v0-beanvorm. Gemeten
op `jutterswet-postwacht`:

    verifier   PASS
    ref        EVALUATION FAULT: fact creation "..." carries only bean fields;
               the symbolic form (IR-SPEC ch. 3.4) is what executes
    clingo     TRANSLATION FAULT: dezelfde zin
    Drools     PLAYED 1  — hij speelt hem gewoon, want hij leest juist die
               beanvelden

Twee uitvoerders weigeren het document, de derde speelt het, en de verifier
laat het door: WF-028 mist een geen-extensievelden-arm. Dit is bovendien de
enige plek in dit blok waar de drie uitvoerders het over het *bestaan* van het
document oneens zijn.

**R2 — `objectCreaties[].subject` op een toekenningsloze creatie.** Van de 11
golden-objectcreaties heeft er één geen `toekenningen`
(`anchor/jutterswet-penninguitgifte`, creatie 1). Daar is `subject` het enige
wat de creatie nog aan een objecttype bindt, en niemand kruist het. Gemeten
met `subject: "Bestaatniet"`:

    verifier   PASS
    ref        MISMATCH: J1.aantal dossiers = 0, expected 1
    clingo     MISMATCH: J1.aantal dossiers = [0], expected 1

Geen van beide faalt; ze zijn het eens over hetzelfde **foute** antwoord, op
een **anker**-golden. Dat is de rij waar het differentiële vierkant blind is
en alleen de handgepinde verwachting nog vangt. Gemeten over alle elf:
**1 van de 11** blijft verifier-schoon, precies de toekenningsloze. De tien
andere worden alsnog afgewezen — maar door **WF-053**, die de navigatie in de
toekenningswaarde niet meer kan binden zodra het subject een onbekend type
noemt. Zij zijn dus incidenteel gedekt, niet door een regel die over dit veld
gaat; die dekking valt weg zodra een creatie geen navigerende toekenning
draagt.

**R3 — `Rollidmaatschap.anderTeFiller`.** Vier golden-posities
(`jutterswet-wachtindeling` 2, `jutterswet-wachtploeg` 2). Gemeten met
`Bestaatniet`: **4/4 verifier-schoon**, en beide uitvoerders luid —
`ref` "EVALUATION FAULT: no fact type relates \"Jutter\" to \"Bestaatniet\"",
clingo dezelfde zin als TRANSLATION FAULT. De goedkoopste van de drie: één
kruising tegen de rolvullers, en de vorm is dicht.

Acceptance:
- Per vlak minstens één negatief, rood-eerst zichtbaar (PASS onder de
  verifier van vóór de kaart, één regel erna), met de arm-exacte mutant
  erbij: alleen die arm weg → alleen het eigen document schoon, over de
  volle vier-zone-sweep.
- R1 krijgt vier getuigen (één per veld) of een gemeten argument waarom
  minder volstaat; de crashende vorm wordt met de exception-naam vastgelegd,
  zoals `wf034-regeldoel-zonder-type` dat doet.
- Het WF-028-gat krijgt zijn eigen negatief onder `wf028-`, want de regel die
  hem afwijst is WF-028 en niet WF-034.
- Sweeps vóór/na met nul over-afwijzing; de tellingen in IR-SPEC (vier zones,
  negatiefzone) en de open-lijst van de WF-034-indexrij worden in dezelfde
  ronde bijgewerkt — een golf die zijn eigen tellingen laat verlopen is de
  fout die de fixronde van de vierde golf moest herstellen.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

Out of scope: uitvoerders, frontends, goldens; de rij-1-wrat en zijn
restwijdte (acht posities, twee golden — geteld in de vierde golf); de
sentinel op de 9 fold-/aggregatie-argumenttypes; WF-007's
payload-blindevlek; het herstellen van de Drools-crashes zelf (de kaart maakt
de invoer ill-formed, zij repareert geen backend).
