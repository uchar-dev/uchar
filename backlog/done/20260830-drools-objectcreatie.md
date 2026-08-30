# Drools: de objectcreatie-jacht (§13.4.6 — de laatste grote exclusieklasse)

Goal: De Drools-backend voert de ch.-3.4-objectcreatie werkelijk uit.
`CaseRunner.build` weigert nu elk model met `objectCreaties` up front,
omdat de creation-consequence niet uitvoert: hij lowert zonder marker en
bereikt fire, maar een genavigeerde lezing van het gecreëerde object leest
leeg, of de creatieregel werpt een `intValue()`-NPE op een leeg lid
(kaart-1-triage 2026-08-30). Getroffen: NEGEN goldens (anchor:
penninguitgifte — met een toekenningloze tweede creatie — rekenkamer,
rekenkamer-overzee, wachtersketen; catala: hardheidsaftrek, overzeelezing,
rekenkamerheffing; synth: budgetgrens-onder, kamerheffing), alle in de
vorm subject + voorwaarde + 0–2 `toekenningen` + meestal `verbindendeRol`;
de tiende creatiedrager (kamerhuishouden-catala) weigert eerder op een
v1-loadervorm en blijft daar. Verder hangt eraan: de laatste
gate-11-slice (het Drools-been van 28/1/0 naar potentieel 29/29/0) en
drie rood-cases die nu EXCLUDE zijn omdat de creatie vóór fire geweigerd
wordt (de ambigue back-link, de generatieve keten over budget, de
weggelopen recursie) — die horen dan hun échte uitkomst te tonen
(FAULT met gepind `expectedFault.drools`-fragment, of eerlijk hergelijst).
Referentiemateriaal: de productietoolchain draait objectcreatie wél (de
TOKA-casus), dus de port draagt vermoedelijk het meeste al — dit is
triage-plus-dichting, geen nieuwbouw; ref (`Eval.run_object_creatie`,
de "::"-gemunte labels) is de semantische scheidsrechter.

UITSPLITSINGSCLAUSULE: wat na eerlijke triage niet haalbaar blijkt binnen
deze kaart (bv. een creatievorm die een eigen ontwerpbesluit vergt) blijft
reden-dragend uitgesloten — per vorm, met de reden in de weigering of in
`EXCLUDE_KNOWN` — en krijgt een eigen vervolgkaart; de kaart faalt niet op
gedeeltelijke opbrengst zolang elk niet-geland deel luid en gelijst is.

Acceptance:
- Rood-eerst: de negen EXCLUDE-regels, de gate-11-telling (28/1/0) en de
  drie rood-EXCLUDEs vóór de fix getoond; per werkstuk een falende
  toestand vóór en de groene erna.
- `bash scripts/check-drools.sh` → PASS; elk van de negen speelt tegen
  zijn ongewijzigde pins óf staat reden-dragend gelijst (doel: played
  86 → 95, auto-excluded 50 → 41; value-divergence alleen groeiend met
  échte, gelijste divergenties). Verwachtingen, pins en WF-nummers
  onaantastbaar.
- `bash scripts/check-toka-parity.sh` → PASS; de laatste slice herpind
  naar PLAYED als hij tegen zijn pins speelt (manifest), anders blijft
  de luide EXCLUDE met waar fragment.
- `sh scripts/check-rood.sh` → PASS; de drie objectcreatie-EXCLUDEs
  hertriagerd: een échte FAULT pint zijn `expectedFault.drools`-fragment
  (corpus/rood: uitsluitend dat veld toevoegen), een NOFAULT of blijvende
  EXCLUDE wordt eerlijk hergelijst met de nieuwe reden.
- Nieuwe witnesses in `backends/drools/src/test` (nieuwe bestanden;
  bestaande testbestanden niet wijzigen) pinnen: de creatie zelf (het
  object bestaat als fact na fire), de member-init (`toekenningen`), de
  rolkoppeling (`verbindendeRol`), de genavigeerde lezing, de
  toekenningloze creatie, en de voorwaarde-arm (geen voorwaarde → geen
  creatie); elke verwachting eerst door ref.
- Alle twaalf gates groen; gates 5/6 met ongewijzigde getallen.

May touch: `backends/drools/src/main/java/**`,
`backends/drools/src/test/**` (nieuwe testbestanden),
`scripts/check-drools.sh` en `scripts/check-rood.sh` (uitsluitend
entries/lijsten), `toka/cases/parity.manifest`, `corpus/rood/*/case*.json`
uitsluitend het `expectedFault.drools`-veld, `backlog/`.

Out of scope: ref/clingo/verifier/spec/corpusverwachtingen en -pins,
toka-ir's (byte-identiek), de v1-loadervormen (ListType/StructType/…),
de sleutelconventies-kaart, gate-scriptgedrag buiten de lijsten.

Result (2026-08-30): Geland op main na één bouwronde en twee seriële
adversariële lensrondes met elk een fixronde. De §13.4.6-objectcreatie
voert uit in het exec-model. De triage wees drie oorzaken aan: (B) alleen
de terugverwijzing werd geschreven — het voorwaartse subjectveld nooit,
dus elke genavigeerde lezing las leeg; (D) de kenmerk-toekenningsvlag werd
genegeerd en een nullable Booleaan unboxte in een primitief veld (de
intValue()-NPE); plus een verkeerd gerichte 1:1-wacht. De fixes: beide
uiteinden geschreven (RUNTIME-SEMANTICS ch. 5.5 "both ends" — de
productietoolchain schrijft alleen de terugzijde; de backend implementeert
hier de spec, het prioritizedDefaults-precedent) met drools.update(s); de
1:1-wacht subjectzijdig conform de opslagrichtings-versmalling van de spec
(r1673-1680) — de snapshot-helft (ref leest world0, dit leest de live
factbase) is bewust NIET overgenomen en staat als comment bij de wacht;
de kenmerk-arm (waar→zet, onwaar/leeg→ongezet, niet-booleaans→
WF-027-fault); en na lens 2 een symmetrische uiteinden-afweging: een
meervoudig uiteinde degradeert (het andere draagt de koppeling — ref-
geverifieerd op 150/6/leeg), geweigerd wordt alleen wat werkelijk
onbereikbaar is (zelfgetypeerd paar / geen uniek verbindend feittype /
beide rollen meervoudig — die laatste vorm faalt bij ref op WF-053 zodra
iets erover navigeert). De invariant die de wacht draagt is als
fail-fast-check vastgelegd.

Gate 12: 86 → 92 played / 50 → 44 auto-excluded (aritmetiek: 9
creatie-dragers verlieten de blanket-weigering, 6 spelen tegen
ongewijzigde pins, 3 vallen op ándere, geprinte redenen uit: 2×
"aggregatie buiten Phase-C scope" + 1× role-refusal voor de
zelfgetypeerde ketens, 1× "consistentie buiten Phase-D scope" voor
penninguitgifte — de per-creatie-weigeringen zelf print de gate NIET;
hun dekking leunt op de witnesses); value-divergence blijft 1
(kustwacht); de 86 eerdere spelers onbewogen. Gate 11: de laatste slice
speelt — het Drools-been VOL op 29 PLAYED / 0 EXCLUDE / 0 FAIL. Gate 8:
de drie objectcreatie-rood-cases blijven EXCLUDE, herlijst met de wáre
redenen (geen fault bereikbaar zolang de vormen geweigerd worden);
corpus/rood byte-onaangeroerd. Backend-JUnit 59 → 82 (23 witnesses in
ObjectCreatieWitnessTest incl. de naFire-seam — een gemunte instantie
heeft geen case-label, dus geen andere assertieroute; de seam werpt op de
fire-cap), elke verwachting vóór het pinnen door ref; beide
wachtrichting-armen, de vier kenmerkflanken, het geldigheidsvenster
(beide armen), de degradatievorm en de marker-telling zijn per
lens-boobytrap rood/groen bewezen.

Uitsplitsingsclausule nagekomen:
`backlog/new/20260830-drools-zelfgetypeerd-creatiepaar.md` draagt de twee
resterende vormen — het zelfgetypeerde creatiepaar (IrLoader gooit de
symbolische verbindendeRol-binding weg die ref als enige route gebruikt;
een FactModelGenerator-ontwerpbesluit; ontgrendelt 2 goldens en 2
rood-fault-pins, elk met óók nog een Phase-C-aggregatie-SKIP) en de
Phase-D-consistentie (kenmerk-negatie + aggregatie-assertie;
penninguitgifte + de ambigue-back-link-rood-case).

Incident, eerlijk gemeld en hersteld: een /tmp-kopie van de worktree
erfde het .git-BESTAND (cp -R; --exclude '.git/' matcht alleen mappen)
waardoor een agent-commit kort op de kaartbranch belandde — hersteld met
reset --soft vóór de echte commits; lens 2 verifieerde de branchhistorie
schoon. Nieuwe regel: /tmp-kopieën met git archive maken.

Alle twaalf gates integraal groen op de eindstand (160/168, 396, 374,
374, 338+36, 22+3/19, 169+11, 144+23, toka 29/0/0, drools 92/44/1/1,
JUnit 82).
