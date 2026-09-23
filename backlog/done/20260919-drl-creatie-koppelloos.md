# drl-creatie-koppelloos: de objectcreatie zonder koppeling is stil

Gemeten (bereikafleiding-lens, 2026-09-19): bij een §13.4.6-objectcreatie
over een herhaald of zelfgetypeerd paar lost exec de terugkoppeling op
via de wire-binding (`FactModelGenerator.gebondenRolpaar`,
`ExecModelTranspiler.objectCreatie`), maar
`DrlTranspiler.objectCreatie` leidt haar uitsluitend af met
`model.verbindendeRol(nieuw, subject)` en raadpleegt de binding nooit.
Bij een niet-oplosbare `verbindendeRol` zet de DRL-tak géén marker: hij
emitteert een commentaarregel (`// onopgelost terugkoppeling naar …`)
en voert `insert($new)` tóch uit — een aangemaakt object zónder
koppeling, zonder zichtbare weigering. Geen getuige dekt dit
(`ObjectCreatieWitnessTest` draait exec-only, geen `BeideTakken`), en
de DRL-tak is niet het gate-12-pad, dus geen corpusrisico vandaag —
wel precies de stille-flankvorm die `20260919-exec-stille-guardflanken`
aan exec-zijde dichtte.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de stille route vastgelegd (commentaarregel +
  insert) op een verifier-schoon document met herhaald paar.
- (b) De DRL-tak leest de wire-binding zoals exec, óf weigert per
  construct met reden (nooit een koppelloze insert);
  dual-branchgetuige + mutatieproef.
- (c) Corpussweep beide takken: nul kanaalwisselingen (geen golden
  draagt de vorm — hermeten); alle veertien gates groen.

May touch: `backends/drools/**` (DrlTranspiler + tests),
kaartverplaatsing. NB: serieel met andere DrlTranspiler-kaarten
(drl-let-meldplek, drl-geneste-let-duw).

## Hermeetnotitie (2026-09-19, hoofdsessie, na de takteller-lens)

Twee onafhankelijke lensmetingen op de takteller-kaart weerleggen de
premisse "zonder zichtbare weigering" op het REPLAY-pad:
`DrlTranspiler.meet()` scant elke `// onopgelost`-commentaarregel in de
slice tot MARKER en de replay weigert daarop — gemeten tonen precies de
twee goldens die deze tak nemen (`anchor/jutterswet-wachtersketen`,
`synth2-budgetgrens-onder`) een `EXCLUDE drl-transpiler MARKER:
objectcreatie: … onopgelost terugkoppeling naar Wachter`, en sinds de
takteller-kaart staan zij reden-dragend in gate 12's `DRL_CHANNEL`. Ook
regel "de DRL-tak is niet het gate-12-pad" is sindsdien achterhaald: de
tak draagt nu een eigen gelijkheidspin in de gate. Wat wél staat: de
TRANSPILER zelf blijft stil (koppelloze `insert` in de gegenereerde
DRL; alleen de harnasscan redt het) en het GEDRAGSGAT tegenover exec —
exec lost de terugkoppeling via de wire-binding op en speelt — is echt.
Bij oppakken: eerst hertriëren en de kop plus acceptatie (a)
herformuleren op deze stand; (b), de binding lezen zoals exec, blijft
de kern. Los defect, eigen kaart: de vier goldens waar de
terugkoppeling WÉL oplost en het vuren faalt
(`20260919-drl-objectcreatie-defect.md`) — disjunct codepad, niet
samenvoegen.

Erbij belegd (lens op de objectcreatie-defect-kaart, 2026-09-19; zelfde
codegebied, zelfde weigerklasse — hier oppakken):

- **Weigeren bij naam waar exec dat doet.** Bij een gedeclareerd lid
  dat het referentieveld verdringt (naamdeling rol/lid) weigert exec de
  creatie bij naam (`transpiler SKIP: … deelt haar naam met een
  gedeclareerd lid`), maar de DRL-tak speelt stil door met een half
  geschreven koppeling — het voorwaartse schrijven blijft achter de
  `heeftReferentieVeld`-garde uit, zonder melding. Vandaag onschadelijk
  (geen golden draagt de vorm; elke lezer van het veld weigert zelf),
  maar het is de stilteklasse die het gate-12-regime uitsluit.
- **De fail-fast op een niet-booleaanse kenmerkwaarde.** Exec werpt
  luid (WF-027-fail-fast, `IR-SPEC.md` verwijst de vorm expliciet naar
  de runtime); de DRL-tak laat sinds de defect-kaart stil ongezet —
  vóór die kaart blies de klasse luid op bij het vuren. Een werpende
  helper is beschikbaar (de tak importeert de runtime-statics en
  `RegelSpraakRuntime` werpt zelf), dus dit is een keuze, geen
  onmogelijkheid; bouw de fail-fast of leg normatief vast waarom de
  takken hier verschillen.

## Hertriage (2026-09-23, bij oppakken, basis main 8784d5e8)

Uitkomst (i): alle drie de flanken staan nog. P2 dekte ze niet: P2
ging over de §13.4.7-FEITcreatie (`creatievelden`, `scopeVeld`), niet
over de §13.4.6-OBJECTcreatie. Exec leest de `verbindendeRol` van de
objectcreatie al sinds `drools-zelfgetypeerd-creatiepaar` (2026-08-30);
`DrlTranspiler.objectCreatie` leidt de terugkoppeling nog steeds alleen
af met `model.verbindendeRol(nieuw, subject)` en raakt
`rolBindingen.get(oc)` niet aan. Gemeten met de jar van main, per
proef verifier / ref / clingo / exec / DRL-tekst:

- (1) Koppelloze insert. `anchor/jutterswet-wachtersketen`
  (zelfgetypeerd): PASS / PASS 2+3 / PASS 2+3 / PLAYED 2 / EXCLUDE
  "MARKER: objectcreatie: werf de opvolger — onopgelost terugkoppeling
  naar Wachter". Proef herhaald paar (rekenkamer + tweede feittype
  `reservetaxatie` Jutter–Rekenkamer): PASS / PASS 2 / PASS 2 / PLAYED 1
  / EXCLUDE idem "naar Jutter". De gegenereerde DRL draagt in beide
  gevallen `// onopgelost terugkoppeling naar …` gevolgd door
  `insert($new);` en een CreationMarker-garde: de transpiler emitteert
  een koppelloze creatie, alleen de scan in `meet()` weigert haar.
- (2a) Naamdeling rol/lid. Voorwaarts (Numeriek `rekenkamer` op
  Jutter): PASS / PASS / PASS / EXCLUDE "transpiler SKIP: … deelt haar
  naam met een gedeclareerd lid" / PLAYED 1 — de DRL schrijft alleen
  de terugverwijzing en zwijgt. Terugwaarts (Numeriek `opdrachtgever` op
  Rekenkamer, nieuwe meting): PASS / PASS / PASS / EXCLUDE idem / EXCLUDE
  "drl compile: … Unable to Analyse Expression" — een weigering van het
  hele pakket die geen vorm noemt.
- (2b) Niet-booleaanse kenmerkwaarde (literal 5 op overzee): WF-027
  FAIL / EVALUATION FAULT "… is not boolean: 5 (WF-027)" / TRANSLATION
  FAULT / ERROR "… niet booleaans: 5 (WF-027)" / PLAYED 1 — de DRL
  gardeert met `Objects.equals(v, true)` en laat stil ongezet.

Gevolg voor gate 12: flank (1) dichten laat de twee goldens die
DRL_CHANNEL als "MARKER objectcreatie: unresolved back-link to
Wachter" pint in de DRL-tak spelen (verwacht 157 → 159). Dat is de kern
van de kaart zelf, geen bijwerking; het pin-diff op `scripts/` is
beschermd en gaat als voorstel in het Result.

## Acceptatie (herformuleerd op de gemeten stand)

- (a) Rood-eerst, beide takken via de replay-API: het herhaalde paar en
  het zelfgetypeerde paar spelen in de DRL-tak zoals in exec (waarden
  van ref); de naamdeling weigert in beide takken bij naam, aan beide
  zijden; de niet-booleaanse kenmerkwaarde faalt in beide takken luid
  met WF-027. Plus een tekstassertie dat de gegenereerde DRL nergens
  een `insert` zonder koppeling draagt.
- (b) `DrlTranspiler.objectCreatie` volgt exec: binding eerst
  (`FactModelGenerator.gebondenRolpaar`), typenpaar alleen zonder
  binding; beide uiteinden door `heeftReferentieVeld`; een creatie
  zonder schrijfbaar uiteinde weigert per construct met de reden van
  exec, nooit een koppelloze insert; de kenmerkwaarde werpt bij een
  niet-booleaanse waarde. Mutatieproef per flank, uitgevoerd.
- (c) Corpussweep beide takken vóór/na: exec byte-identiek; DRL-tekst
  alleen de twee genoemde goldens EXCLUDE → PLAYED. Pin-diff voor
  `scripts/check-drools.sh` als voorstel. Overige dertien gates groen.
- De bestaande rij `DrlCreatieTakWitnessTest.een_gelijknamig_lid_…`
  pint de divergentie die (2a) opheft en verwijst zelf naar deze kaart;
  zij wordt omgezet naar een tweetakkenweigering (gemeld mandaat).

Result: (a) en (b) gehaald voor alle drie de flanken; (c) gelijkheid
gehaald, maar de DRL-pin beweegt zoals de hertriage voorspelde: 157 →
159. Gate 12 staat daardoor rood met het script van main zolang het
pin-diff hieronder niet landt (`scripts/` is beschermd pad). De andere
dertien gates zijn groen. Volgens de opdracht is dit het stop-en-meldpunt.
De kaart gaat naar `done/`; de hoofdsessie landt het pin-diff direct
erna (zie onder).

Per flank:

- (1) Koppelloze insert: in deze kaart gedicht.
  `DrlTranspiler.objectCreatie` lost de terugkoppeling nu op zoals
  `ExecModelTranspiler.objectCreatie`: eerst de binding
  (`rolBindingen.get(oc)` → `FactModelGenerator.gebondenRolpaar`), alleen
  zonder binding het typenpaar. Een creatie zonder schrijfbaar uiteinde
  weigert per construct met de zin van exec (ambigu paar, zelfgetypeerd
  zonder binding, geen uniek feittype, beide rollen meervoudig). Er
  wordt geen insert meer geëmitteerd. Een binding die geen rolpaar
  aanwijst, weigert als "wijst geen rolpaar aan" (vóór de fix speelde
  de DRL-tak zo'n document op het typenpaar en negeerde hij de binding).
  De 1:1-garde leest nu het voorwaartse veld op het subjectpatroon
  (`<fwdRef> == null`), net als exec. Alleen daarmee speelt ook de vorm
  met een meervoudige subjectzijdige rol: daar draagt de munt geen
  terugverwijzing, en de DRL-tak compileerde niet ("Unable to Analyse
  Expression opdrachtgever == $subject"). Het ambigue paar zonder
  binding (`corpus/rood/ambiguous-backlink`) faalt in exec bij het vuren.
  De DRL-tak kent geen foutregel-idioom en weigert het bij naam met
  dezelfde zin. Dat is een versmalling, geen stilte, en gate 8 draait
  alleen exec. Op een LEGE wereld (geen subject dat vuurt; gevonden in
  de lensronde) faalt ref ook, maar speelt exec (PLAYED 1, de eigen
  vastgelegde versmalling van exec, inhoudelijk fout tegenover ref). De
  DRL-tak weigert daar, en is dus behoudender dan exec.
- (2a) Naamdeling rol/lid: in deze kaart gedicht, aan beide zijden. Het
  voorwaartse geval speelde stil half, het terugwaartse (nieuw gemeten)
  sloeg het hele pakket neer zonder vorm te noemen. Beide weigeren nu
  bij naam met `FactModelGenerator.geenReferentieveldReden`, woord voor
  woord de exec-zin.
- (2b) WF-027: in deze kaart gedicht. Nieuwe runtime-static
  `RegelSpraakRuntime.rsKenmerkWaarde(v, attr)`: true zet de mark,
  false/leeg laat haar ongezet, elke andere waarde werpt met de exec-zin
  "kenmerkinitialisatie van '…' is niet booleaans: … (WF-027)". De
  DRL-tak roept die static aan in plaats van `Objects.equals(v, true)`.
  Exec is ongewijzigd (werpt al zelf).

Na de fix, zelfde proeven, exec / DRL-tekst: herhaald paar PLAYED 1 /
PLAYED 1; wachtersketen PLAYED 2 / PLAYED 2; budgetgrens-onder PLAYED 1 /
PLAYED 1; naamdeling voorwaarts en terugwaarts beide EXCLUDE met
dezelfde zin; beide rollen meervoudig beide EXCLUDE "beide rollen zijn
meervoudig"; subjectzijde meervoudig PLAYED 1 / PLAYED 1; kenmerk 5
beide ERROR "… niet booleaans: 5 (WF-027)"; ambigu DRL EXCLUDE "… the
created object's back-link is ambiguous (ch. 5.5)".

Getuigen, rood eerst. Nieuwe klasse `DrlCreatieKoppelingWitnessTest`,
11 rijen via de replay-API. Tien rijen gaan door beide takken
(`BeideTakken`); de ambigu-rij is alleen DRL, omdat exec daar
bewust foutvuurt en `ObjectCreatieWitnessTest` die kant al pint. Er is
ook een tekstassertie op de gegenereerde DRL van wachtersketen: geen
`// onopgelost`, beide uiteinden geschreven, garde op `opvolger ==
null`. Keten stash/test/pop/test: zonder fix test-rc=1 ("Tests run: 11,
Failures: 7, Errors: 4"), met fix test-rc=0 (11/11). Mutatieproeven,
elk met kopie, mutant, test en terugzetten met cmp in één keten:
- M1 (binding genegeerd, `vr = null`): test-rc=1, 5 rijen rood
  (herhaald paar ×2, zelfgetypeerd, DRL-tekst, binding zonder rolpaar).
- M2 (beide `heeftReferentieVeld`-gardes uit): test-rc=1, precies de 2
  naamdelingsrijen.
- M3 (`rsKenmerkWaarde` werpt niet): test-rc=1, precies de WF-027-rij.
- Hersteld: test-rc=0.

Bestaande test gewijzigd, gemeld mandaat:
`DrlCreatieTakWitnessTest.een_gelijknamig_lid_verdringt_het_voorwaartse_veld`
pinde de divergentie die (2a) opheft en zei zelf dat het gelijktrekken
bij deze kaart hoorde. De rij is omgezet naar
`BeideTakken.geweigerd(…, "deelt haar naam met een gedeclareerd lid")`,
in een eigen commit. De hulpfunctie die daarna ongebruikt was
(`jutterMetOnwareVerwachting`) en drie imports zijn weg. Andere
bestaande tests zijn niet aangeraakt. JUnit 600/600 groen.

(c) Sweep over alle goldens met casus en alle rood-documenten (503
volledige stdout-bestanden), jar van main 8784d5e8 tegen de jar met
fix. Uitkomst van `diff -r`: precies twee bestanden, beide DRL-tekst:
`anchor/jutterswet-wachtersketen` EXCLUDE → PLAYED 2 en
`synth2-budgetgrens-onder` EXCLUDE → PLAYED 1. Exec en rood zijn
byte-identiek. Exec 180 → 180, DRL-tekst 157 → 159. De kanaallijst van
exec en `EXPECTED_PLAYED` bewegen niet. Er draagt geen golden een
herhaald niet-zelfgetypeerd paar, naamdeling of niet-booleaanse
kenmerkwaarde.

Gates, elk los gedraaid op de takbroncode:

| gate | stand |
|---|---|
| 1 self-contained | PASS |
| 2 ir-schema | PASS (274 bestanden, vloer 192) |
| 3 verifier | PASS (274 golden schoon, 431 negatief verworpen, vloer 350) |
| 4 case-schema | PASS (632 cases) |
| 5 oracle | PASS (780 voorbeelden, 591 cases) |
| 6 clingo | PASS (480 checks, 591 cases) |
| 7 noop | PASS (555 cases, 36 overlevers) |
| 8 rood | PASS (41 rood; Drools-been 9 met pin, 32 met reden) |
| 9 catala | PASS (201 checks, 12 goldens) |
| 10 regelspraak | PASS (393 tests, 70 goldens) |
| 11 toka-parity | PASS (29 cases, Drools 29 PLAYED) |
| 12 drools | FAIL met het script van main: "2 golden(s) changed channel in the DRL-TEXT branch: anchor/jutterswet-wachtersketen :: pinned EXCLUDE, measured PLAYED — PLAYED 2; synth2-budgetgrens-onder :: pinned EXCLUDE, measured PLAYED — PLAYED 1". PASS met het voorgestelde diff (tijdelijk toegepast, gedraaid, teruggezet, `git diff --quiet scripts/`): played 180, DRL-tekst 159, vloeren 53/74, JUnit 600 |
| 13 reftype-derivation | PASS (131 twists, 128 replays, 25 DRL) |
| 14 flogic | PASS (130 checks, 2 goldens) |

Voorgesteld diff voor `scripts/check-drools.sh` (beschermd pad,
maintainer). Zo gedraaid; het past op main 8784d5e8:

```diff
@@ -884,8 +884,9 @@ DRL_CHANNEL=(
   "synth3-standtarief|EXCLUDE|loader Refusal: Datatype:SumType"
   "synth3-vangstsom|EXCLUDE|loader Refusal: Datatype:ListType"
   "synth3-vondststaat|EXCLUDE|loader Refusal: Datatype:StructType"
-  # (C) DrlTranspiler MARKERs — 12, this branch's own per-form refusals, each naming the rule it
-  # could not lower. Recounted 2026-09-23 (card drl-periode-zink): the list held 11 under a header of
+  # (C) DrlTranspiler MARKERs — 10, this branch's own per-form refusals, each naming the rule it
+  # could not lower. 12 -> 10 (2026-09-23, card drl-creatie-koppelloos): the two objectcreatie
+  # markers below the THREE are gone, see EXPECTED_DRL_PLAYED. Recounted 2026-09-23 (card drl-periode-zink): the list held 11 under a header of
   # 12, because anchor/jutterswet-bergingstoezicht (the unresolved verdeling) left it at 156 -> 157
   # and the header did not follow; jutterswet-seinjaar, a NEW golden refused in both branches by one
   # sentence, makes it 12 again and moves neither played count. Recounted 2026-09-20 (card
@@ -898,27 +899,21 @@ DRL_CHANNEL=(
   # same limit (the many-to-many bean field, the qualified creation guard's doel attempt, the
   # consistency rule, the EERSTE fold over a set, the date-valued value fold, and the out-of-scope
   # condition — jutterswet-klipwacht, shared, not split).
-  # THREE are this branch alone, and the exec branch PLAYS all three:
-  #   anchor/jutterswet-wachtersketen (exec PLAYED 2), synth2-budgetgrens-onder (exec PLAYED 1) —
-  #     both an OBJECTCREATIE marker on the same unresolved back-link to Wachter. That is what is
-  #     LEFT of this branch's §13.4.6 gap: the SILENT half — four goldens that fired and answered
-  #     leeg, or faulted at fire — was a class of its own here until the creation lowering learned
-  #     ch. 5.5's two clauses (card drl-objectcreatie-defect, 2026-09-19), and those four now play.
-  #     What remains is the LOUD half, refused by name before anything fires: a different code path
-  #     (the rolNaarSubject == null route), and its own card.
+  # ONE is this branch alone, and the exec branch PLAYS it:
   #   jutterswet-vouwrand (exec PLAYED 1) — a TODO voorwaarde on a conjunction of comparisons.
+  # Until 2026-09-23 two more stood here, anchor/jutterswet-wachtersketen and
+  # synth2-budgetgrens-onder, an OBJECTCREATIE marker on the unresolved back-link to Wachter; they
+  # left with card drl-creatie-koppelloos (reason block at EXPECTED_DRL_PLAYED).
   "anchor/jutterswet-bootberging|EXCLUDE|MARKER regel: bronPad step Boot -> Vondst has no bean field — the middle relation is many-to-many (§13.3.9)"
   "anchor/jutterswet-erflijn|EXCLUDE|MARKER regel: aggregation join with both sides plural — neither end stores a bean field (§13.3.9)"
   "anchor/jutterswet-gemeenteberging|EXCLUDE|MARKER regel: aggregation join whose storage direction is the far side (§13.3.9)"
   "anchor/jutterswet-penninguitgifte|EXCLUDE|MARKER consistentie: unresolved consistency rule over every Penning"
-  "anchor/jutterswet-wachtersketen|EXCLUDE|MARKER objectcreatie: unresolved back-link to Wachter"
   "jutterswet-klipwacht|EXCLUDE|MARKER regel: TODO voorwaarde — the gedurende clip as a composite member"
   "jutterswet-muntketen|EXCLUDE|MARKER regel: bronPad step Boot -> Munt has no bean field — the same many-to-many middle relation"
   "jutterswet-seinjaar|EXCLUDE|MARKER feitcreatie: voorwaarde niet naar anker of doel te lowereren — gehele-periodekwalificatie: alleen de anker is gebonden (WF-109). Shared with the exec branch, one sentence (kern.Gastheer). Until card drl-periode-zink (2026-09-23) this branch PLAYED the golden by accident: its whole-period lowering reads the subject variable, which inside the doel pattern is the anker, and the anker carries a same-named timeline of the same type — on any other anker the same lowering failed the whole package at compile"
   "jutterswet-vondstbereik|EXCLUDE|MARKER regel: the MINIMUM/MAXIMUM fold over a DATE set source — the shared member-type reason, word for word the exec branch's (card datum-setbron-breuk, 2026-09-20). Its EERSTE/LAATSTE twin sits one line below and is refused one arm earlier, on the fold KIND rather than on the member type"
   "jutterswet-vondstvenster|EXCLUDE|MARKER regel: TODO aggregatie EERSTE over a set source"
   "jutterswet-vouwrand|EXCLUDE|MARKER regel: TODO voorwaarde — a conjunction of comparisons"
-  "synth2-budgetgrens-onder|EXCLUDE|MARKER objectcreatie: unresolved back-link to Wachter"
   # (D) The one value-divergence this branch SHARES with the exec branch — negation-as-failure is not
   # stratified, so a geen-<kenmerk> rule fires before the positive kenmerk is derived. The exec
   # branch's full reason is in EXCLUDE_KNOWN above; both branches inherit it from the ported runtime.
@@ -996,7 +991,14 @@ DRL_CHANNEL=(
 # EXPECTED_PLAYED above.
 # 156 -> 157 (2026-09-22, card p3-drools-verbindenderol-verdeling): anchor/jutterswet-bergingstoezicht.
 # The recipient join is the same shared derivation in this branch. Reason block at EXPECTED_PLAYED above.
-EXPECTED_DRL_PLAYED=157
+# 157 -> 159 (2026-09-23, card drl-creatie-koppelloos): anchor/jutterswet-wachtersketen and
+# synth2-budgetgrens-onder. Their creation runs over a SELF-TYPED pair (Wachter creates Wachter over
+# `opvolging`), where the type-pair derivation this branch used for the back-link answers nothing; the
+# lowering then emitted an insert with no link, and only the marker scan refused it. It now reads the
+# creation's verbindendeRol binding, as the exec branch has since 2026-08-30 and as ref does, and plays
+# both. Measured over every golden and rood document in both branches: the exec output and every
+# other document are byte-identical.
+EXPECTED_DRL_PLAYED=159
 
 # --- each list's own two guards (fixronde 2026-09-06; generalised over both lists 2026-09-19) ---
 # The floors below compare each MEASURED golden against ITS pin, so a malformed list can weaken a
```

Het pin-diff landt de hoofdsessie als eigen commit op de branch, direct
na de done-commit van deze kaart. In hetzelfde scriptblok hertelt zij de
commentaren: "seventy-three of the seventy-six" wordt "seventy-one of
the seventy-four", "NINE of the twelve" wordt "of the ten", in de
klasse-(A)-kop 23 → 21 en 26 → 24, de verwijzing "below the THREE"
vervalt, en de overlange nieuwe kopregel wordt gewrapt.

Lensronde (slotronde, oordeel MERGEN): het verouderde commentaar in het
kenmerkblok van `DrlTranspiler.objectCreatie` ("evaluates the value
twice") beschrijft nu de `rsKenmerkWaarde`-aanroep. Het lege-wereldgeval
van het ambigue paar staat hierboven bij de versmalling en in de
registerrijtekst.

Voorgestelde registerbijwerking (`spec/CONSTRUCTS.md`, hoofdsessie).
Rij 21 ("Object creation") krijgt achteraan: "**Landed (2026-09-23,
card `drl-creatie-koppelloos`):** the DRL-text Drools branch resolves
the back-link as the exec branch does — the creation's `verbindendeRol`
binding first (`FactModelGenerator.gebondenRolpaar`), the type pair
only without one; a creation with no writable end, a displaced
reference field on either end, or a binding that names no role pair is
refused by name in the exec branch's words, never inserted unlinked;
the 1:1 guard reads the subject's forward field; a non-boolean
characteristic initialisation faults (WF-027,
`RegelSpraakRuntime.rsKenmerkWaarde`). The self-typed goldens
`anchor/jutterswet-wachtersketen` and `synth2-budgetgrens-onder` play
in both branches (180/159); the sweep is otherwise byte-identical.
Narrowing, recorded: without a binding, an ambiguous pair is a fault at
fire in the exec branch — which passes where no subject fires, where ref
faults even on an empty world — and a refusal by name in the DRL-text
branch, in either world. Witness `DrlCreatieKoppelingWitnessTest`." De knooprij
`ResolvedObjectCreatie` krijgt: "back-link read off `verbindendeRol`
by both Drools branches since 2026-09-23 (DRL-text; exec since
2026-08-30)". PLAN.md: geen scopewijziging.

Niet aangeraakt: corpus, spec, scripts, frontends, toka, wire en schema.
`ExecModelTranspiler` is ongewijzigd, want de metingen wezen daar geen
werk aan.
