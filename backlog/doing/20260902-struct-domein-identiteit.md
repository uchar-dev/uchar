# Struct-domein-identiteit: de zustertweeling van WF-089/WF-023

Goal: Fixronde 2 van de use-site-declaratie-identiteit-kaart heeft de
struct-tweeling van het domein-identiteitsgat GEMETEN en bewust buiten
scope gelaten (de maintainerterm "keuzetype-familie" dekt Enumeratie en
SumType, geen structdomeinen): een `StructLiteral` van structdomein B
geschreven in een attribuut van structdomein A is verifier-schoon; ref
fault bij de veldlezing en clingo leidt niets af waar een waarde
verwacht werd — een fout antwoord, dus een verifier-schone
ref≢clingo-divergentie van precies de klasse die WF-089 voor keuzetypes
dichtte. De uitbreiding is al gebouwd en doorgemeten in die fixronde
(0 over-afwijzing over 197 documenten, waarvan 9 met 55
struct-getypeerde referenties; het E35-reproductiedocument vangt hij) en
is dus gemeten gratis; wat ontbreekt is het maintainerbesluit over de
term (de "keuzetype-familie" verbreden, of structdomeinen als eigen
zusterclausule onder WF-023/WF-089 brengen) en het volle regime
(negatief per as, rood-eerst, indexrijen). De meetstand staat in de
WF-023-clausule (ch. 3.1), de WF-023-indexrij en de WF-089-clausule.

Acceptance:
- De termkeuze als genummerde vraag aan de maintainer vóór de bouw.
- Rood-eerst: het struct-reproductiedocument (StructLiteral-drift, en de
  lees-tweeling als die bestaat) verifier-schoon getoond vóór de regel,
  afgewezen erna; per as een negatief met NOTE en mutant-bewijs.
- De "should preclude"-restzinnen in `ref/lib/eval.ml` en de
  spec-clausules die het gat nu documenteren, aangescherpt naar de
  nieuwe stand.
- Nul over-afwijzing over golden + rood + toka (read-only), gemeten;
  alle twaalf gates groen.

May touch: `spec/verifier/**` (proposal), `spec/IR-SPEC.md` (proposal),
`corpus/negatief/**`, `ref/**` (meldteksten), `backlog/`.

Out of scope: keuzetype-armen (geland), de WF-061-expressielaag/
declaratiekant (eigen open punt), nieuwe struct-constructen.

## Bouwnotitie

**Maintainerbesluit (2026-09-03).** De regel landt als eigen nummer
**WF-090 "struct-domein-identiteit"**: eigen clausule, eigen indexrij,
eigen negatiefs; de verifier-machinerie gedeeld met WF-089/WF-023; de
ch.-2.4-term "keuzetype-familie" blijft ONgewijzigd (structdomeinen
vallen er niet onder).

### 1. Reproductie, rood-eerst, alle flanken

Tien documenten in `/tmp/sd-flanken/` (buiten de repo), gebouwd op de
`anchor/jutterswet-vondstdossier`-golden met een tweede, intern
consistent structdomein `logboek` (`bedrag`, `aantal`) naast `dossier`
(`waarde`, `telling`) — andere veldnamen, zodat een veldlezing de drift
zichtbaar maakt. VOOR-meting met de HEAD-verifier, NA-meting met de
gelande verifier.

| flank | verifier vóór | ref | clingo | verifier ná |
|---|---|---|---|---|
| W1 regeldoel (StructLiteral B → attribuut A) | PASS | `EVALUATION FAULT: field "waarde" is not present on the record value` | `MISMATCH: J1.waardekopie is underived (leeg), expected 12550` | WF-090 FAIL |
| W2 objectcreatie-toekenning | PASS | zelfde fout | **`clingo: PASS (8 expectations)`** — groen op een document dat ref niet kan evalueren | WF-090 FAIL |
| W3 defaults-consequentie | PASS | zelfde fout | `TRANSLATION FAULT: unscalable datatype` (ook op de TROUWE tweeling — het dragerhek, geen bevinding) | WF-090 FAIL |
| R1 AttribuutRef (scalair) | PASS | `field "bedrag" is not present…` | `MISMATCH: J1.waardekopie is underived (leeg), expected 12050` | WF-090 FAIL |
| R4 AttribuutRef door `ListType` | PASS | zelfde fout | **`MISMATCH: J1.totaalwaarde = [0], expected 1250`** — fout antwoord | WF-090 FAIL |
| R2 BinderRef (lambdaparameter) | PASS | zelfde fout | **`MISMATCH: J1.totaalwaarde = [0], expected 1250`** — fout antwoord | WF-090 FAIL |
| R3 ParameterRef | PASS | zelfde fout | `TRANSLATION FAULT: a struct field-access base here is …` | WF-090 FAIL |
| R7 DimensieCel | PASS | geen route (het caseformaat draagt geen celinvoer) | idem | WF-090 FAIL |
| R5 foldbron (`elementObjecttype`) | PASS | zelfde fout | `TRANSLATION FAULT: expression kind not translated` (ook op de trouwe tweeling) | WF-090 FAIL |
| R6 aggregatie-doorgeefluik (`EERSTE`) | PASS | zelfde fout | idem | WF-090 FAIL |

Trouwe tweelingen blijven schoon: W1t `oracle: PASS` + `clingo: PASS`,
W2t `oracle: PASS (8)` + `clingo: PASS (8)`, R5t `verifier: PASS` +
`oracle: PASS`.

**Al gedekt, geen dubbele regel** (gemeten, niet aangenomen):

- struct-in-struct — een veldwaarde van domein B waar het veld A
  declareert: `WF-073 FAIL … field 'kern' value is struct 'logboek', but
  the declared field datatype is struct 'dossier'` (volle
  datatype-gelijkheid);
- het `FieldAccess`-resultaattype: `WF-074 FAIL … field access type
  struct 'logboek' differs from the declared field datatype struct
  'dossier'`;
- een functielichaam tegen zijn gedeclareerde resultaat: WF-067 kruist
  het volle datatype;
- `TemporalRead`/`Invoke`/`Lambda`-parameters: WF-049/068/070,
  structureel.

Bijvangst: die twee meldingen luidden vóór deze kaart "struct differs
from struct" — `datatype_detail` had geen `Struct_type`-arm. Toegevoegd
(de L2-F8-fix, nu voor structs).

### 2. Wat er landde

`spec/verifier/lib/checks.ml`: `struct_domain` (spiegelt
`keuzetype_domain`, mét de `ListType`-doorkijk), `wf090_struct_identity`
(spiegelt `wf089_domain_identity`, met de twee eenzijdige armen), en één
gedeeld ingangspunt `use_site_identity` waar de zes leesposities doorheen
lopen — de twee regels blijven mechanisch in de pas, maar zijn apart
falsifieerbaar. De schrijfhelft rijdt mee op de drie posities van
`wf023_assignment` onder eigen regelnummer; eenzijdig alleen binnen één
familie, net als de keuzetype-clausule. `Datatype_onbekend`, een
onoplosbare bron en een ontbrekend `type` defer-en zoals bij WF-023/089.

### 3. Negatiefs en mutantbewijs (gate 3: 206 → 216)

Elf mutanten in de git-archive-kopie `/tmp/K`, elk arm-exact:

| mutant | schoon geworden negatiefs |
|---|---|
| M1 WF-090 onderdrukt op schrijfpositie 1 | alleen `wf090-schrijf-regeldoel` |
| M2 idem positie 2 | alleen `wf090-schrijf-objectcreatie` |
| M3 idem positie 3 | alleen `wf090-schrijf-defaults` |
| M11 de hele structclausule uit `wf023_assignment` | precies de drie schrijfgevallen |
| M4 `Attribuut_ref`-arm terug naar WF-089-only | `wf090-attribuutref` + `wf090-attribuutref-lijst` |
| M5 `List_type`-recursie uit `struct_domain` | alleen `wf090-attribuutref-lijst` |
| M6 `Binder_ref`-arm | alleen `wf090-binderref` |
| M7 `Parameter_ref`-arm | alleen `wf090-parameterref` |
| M8 `Dimensie_cel`-arm | alleen `wf090-dimensiecel` |
| M9 foldbron-tak | alleen `wf090-foldbron` |
| M10 aggregatietak | alleen `wf090-doorgeefluik` |

### 4. Twee eerlijkheidsnotities (gemeten)

- **De ci-clausule is ONgepind.** Geen enkel golden-, rood- of
  pariteitsdocument spelt een struct-use-site anders dan zijn declaratie;
  de mutant die de vergelijking hoofdlettergevoelig maakt laat gate 3
  groen (`check-verifier: PASS (170 golden clean; 216 negatief …)`).
  Anders dan bij WF-089, waar `jutterswet-vaarwaterspelling` die clausule
  pint.
- **Vier van de zes leesarmen hebben geen positieve getuige.** Gemeten
  telling over golden + rood + toka: `AttribuutRef` 46 struct-getypeerde
  referenties, `BinderRef` 9, `ParameterRef` 0, `DimensieCel` 0,
  foldbron 0, doorgeeffold 0. Mutanten die op elke struct-getypeerde use
  site vuren maken `anchor/jutterswet-vondstdossier` rood bij de eerste
  twee armen en laten gate 3 groen bij de vier andere. Een golden is daar
  vandaag niet toe te voegen: het ASP-dragerhek weigert de vormen vóór de
  vertaling (gemeten op de trouwe tweelingen).

### 5. Over-afwijzing

Nul, met de gelande verifier:

    golden+rood: 170 documenten, WF-090-treffers=0
    toka:         30 documenten, WF-090-treffers=0

De struct-lading die daar doorheen ging: 9 documenten, 55
struct-getypeerde referenties, 15 `StructLiteral`s
(`anchor/jutterswet-vondstdossier`, `jutterswet-vangstwaardering`,
`jutterswet-waarderingsinvoer`, `jutterswet-dossierrecht-catala`,
`jutterswet-kamerhuishouden-catala`, `synth3-nestwaarde`,
`synth3-vondststaat`, `rood/list-element-overprecision`,
`rood/struct-field-overprecision`) — exact het getal dat de vorige ronde
al mat.

### 6. Gates

    check-self-contained: PASS
    check-ir-schema:      PASS (schema valid; 170 file(s) validate and round-trip)
    check-verifier:       PASS (170 golden clean; 216 negatief rejected for their declared rule)
    check-case-schema:    PASS (schema valid; 418 case file(s) validate)
    check-oracle:         PASS (396 cases replayed exactly; NOT replayed, no case file yet: corpus/golden/jutterswet-vormenkabinet)
    check-clingo:         PASS (396 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop:           PASS (360 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)
    check-rood:           PASS (22 red cases fault observably in ref and clingo); Drools leg PASS (5 fault-pinned, 17 not-carried with reason)

`dune runtest`: 201 checks, 0 failures.

### 7. Open gebleven

1. **De ci-clausule van WF-090 is ongepind** — er is geen golden met een
   struct-use-site die alleen in hoofdlettergebruik van zijn declaratie
   verschilt. Op te lossen zoals `jutterswet-vaarwaterspelling` dat voor
   WF-089 deed: één golden erbij.
2. **Vier leesarmen zonder positieve getuige** (ParameterRef,
   DimensieCel, foldbron, doorgeeffold) — geblokkeerd door het
   ASP-dragerhek, dat drie van die vormen vóór de vertaling weigert.
   Dezelfde blokkade als bij WF-089; hoort bij een dragerkaart, niet bij
   deze.
3. **Orderbaarheid van de doorgeeffolds voor structs.** `EERSTE` is in v0
   een ORDENENDE fold en `StructType` is equality-only (ch. 2.4), dus een
   volstrekt trouwe struct-`EERSTE` met twee bijdragers is een
   WF-038-operandvraag — kaart `20260902-doorgeeffold-orderbaarheid`,
   niet deze regel.
4. **Het clingo-dragerhek op struct-defaults, struct-parameters en
   struct-folds** — `TRANSLATION FAULT` op de trouwe tweelingen, dus geen
   divergentie maar wel een lege plek in het differentiaalvierkant.
