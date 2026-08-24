# WF-voorstel: leaf-ref-type-leugen in additieve operand-posities (herkomst: BinderRef, spoor 7b)

Herkomst: de eerste adversariële jachtsessie
(`backlog/doing/20260824-adversariele-jacht.md`, spoor 7b), gecorrigeerd in
de lensronde van dezelfde sessie (twee opus-lenzen, beide MINOR/CONFIRMED —
zie de lensronde-notitie in het jachtrapport). Dit is een KAARTVOORSTEL, geen
ingecheckte wijziging — `spec/verifier/` is een protected path, dus de jacht
raakt de check niet zelf aan.

## De vondst (gecorrigeerd t.o.v. spoor 7b)

De oorspronkelijke spoor-7b-formulering ("`AttribuutRef.type` wordt
kruisgecontroleerd (WF-023), `BinderRef.type` niet") was onnauwkeurig op het
WF-id én op de as van de asymmetrie. Empirisch (verifier/ref/clingo, herhaald
in de lensronde):

- Wat een soort-leugen (`Numeriek` → `Tekst`/`Booleaan`) op een reken-operand
  afkeurt is **WF-035** ("operand of a numeric operator is text/boolean"),
  niet WF-023 en geen blad-kruiscontrole. WF-035 leest de geserialiseerde
  `typ_of_expr` van de operand (`spec/verifier/lib/checks.ml:1663`,`:1675`) en
  vuurt **gelijk voor BinderRef én AttribuutRef** — `typ_of_expr` leest het
  `type`-veld van beide blad-knopen (`checks.ml:719`, AttribuutRef op `:722`,
  BinderRef op `:725`).
- WF-035 vuurt echter **alleen onder numeriek-vereiste operatoren** (`MAAL`,
  `GEDEELD_DOOR`, `GEDEELD_DOOR_ABS`, `PERCENTAGE_VAN`, `VERMINDERD_MET`,
  `TOT_DE_MACHT`). Voor de additieve operatoren `PLUS`/`MIN` is de
  numericiteits-eis expliciet uitgezet
  (`checks.ml:1668`: `Plus | Min -> false (* may be date +/- duration *)`)
  zodat datum±duur kan blijven werken. Gevolg: **geen** soort-leugen op een
  `PLUS`/`MIN`-operand wordt gevangen, ongeacht of het blad een BinderRef of
  een AttribuutRef is.
- De escape in spoor 7b (`acc + x`, `PLUS`) kwam dus door de **additieve
  positie**, niet door BinderRef-specificiteit. Bewijs (lensronde): dezelfde
  BinderRef-`x`-soort-leugen wordt onder `MAAL` wél afgekeurd
  (`WF-035 FAIL … operand of a numeric operator is text`), en een
  AttribuutRef-operand onder `PLUS` glipt er even hard doorheen (verifier
  PASS; ref rekent door; clingo TRANSLATION FAULT).

Wat wél een blad-`type`-kruiscontrole doet — `resolved_attr_datatype`
(`checks.ml:741`) — is UITSLUITEND in **WF-027** bedraad (kenmerk-init,
`checks.ml:791`, binnen `wf027` vanaf `:748`); daarom citeert het negatief
`corpus/negatief/wf027-lying-attribuutref-type/` **WF-027**, niet WF-023.
WF-023 zelf (`checks.ml:3942`) is de toewijzings-doelregel ("a text value is
assigned to a number target") en leest `typ_of_expr` — het geserialiseerde,
mogelijk liegende type — niet een resolved-blad-kruiscontrole. Er bestaat dus
**voor geen van beide blad-soorten** een decimalen/eenheid- of
soort-kruiscontrole van het blad-`type` in een reken-positie.

De decimalen-leugen is bovendien **inert en ongecontroleerd voor beide
blad-soorten**: `waardering`-`AttribuutRef` met `decimalen 0 → 5` in
`loon = waardering * factor` (vindersloon) geeft verifier PASS + ref PASS +
clingo PASS, dezelfde waarde (`loon = 42`) — de executors negeren de
annotatie. Of decimalen-consistentie überhaupt afgedwongen moet worden is een
spec-keuze zonder gedrag; de soort-leugen is de gedrag-rakende splitsing.

## De echte lacune

Een leaf-ref (BinderRef óf AttribuutRef) met een soort-leugen in een
**additieve (`PLUS`/`MIN`) operand-positie** is verifier-schoon maar splitst
het differentieel-vierkant LUID: ref rekent door (annotatie genegeerd),
clingo `TRANSLATION FAULT: non-numeric arithmetic operand is not translated`
(`backends/clingo/lib/translate.ml:383`, uit `numeric_ok`). Nooit stil, dus
geen corpus-case en geen zwijgende divergentie — maar precies de klasse die de
poortwachter hoort te vangen: een zelf-tegensprekende IR die geen frontend
hoort te emitteren en die WF-035 onder elk niet-additief operator al zou
afkeuren.

## Minimale reproductie

Basis: `corpus/golden/anchor/jutterswet-vindersloon/` (`loon = waardering *
factor`) en `.../jutterswet-aanspraaktelling/` (fold-lichaam `acc + x`).

- AttribuutRef onder additief: zet in de vindersloon-`loon`-regel `waarde.op`
  op `PLUS` en `waarde.links` (`waardering`).`type` op `{Tekst}`. → verifier
  PASS; ref rekent (`loon = 13`, annotatie genegeerd); clingo `TRANSLATION
  FAULT: non-numeric arithmetic operand`.
- BinderRef onder additief (spoor 7b): zet in aanspraaktelling op de
  `rechts`-BinderRef `x` `type = {Tekst}` (lichaam blijft `acc + x`, `PLUS`).
  → verifier PASS; ref rekent; clingo TRANSLATION FAULT.
- Controle dat het de additieve positie is, niet het blad: dezelfde
  BinderRef-`x`-soort-leugen onder `MAAL` → `WF-035 FAIL … operand of a
  numeric operator is text` (afgekeurd). Idem AttribuutRef onder `MAAL`.
- Decimalen-leugen (op AttribuutRef of BinderRef): driemaal PASS, inert.

Werkbestanden (buiten de repo): `/tmp/hunt/vl_attref_plus_text.json`,
`/tmp/hunt/asp_binderref_x_text_PLUS.json`,
`/tmp/hunt/asp_binderref_x_text_MAAL.json`, `/tmp/hunt/vl_attref_numlie.json`.

## Voorstel

Twee sporen, maintainer-keuze (beide raken `spec/verifier/` +
`spec/IR-SPEC.md`, protected):

1. **Smalst, gedrag-gericht (aanbevolen):** scherp WF-035's `PLUS`/`MIN`-
   uitzondering aan zodat een operand die *nooit* een datum of duur kan zijn —
   een `Tekst`- of `Booleaan`-getypte operand — óók onder `PLUS`/`MIN` wordt
   afgekeurd, terwijl echte datum±duur-optellingen toegelaten blijven. Dit
   sluit de lacune voor béide blad-soorten in één keer, want WF-035 leest
   `typ_of_expr` uniform. Bij landing een negatief
   `corpus/negatief/wf035-additive-text-operand/` (soort-leugen op een
   `PLUS`-operand).
2. **Breder, structureel:** een blad-`type`-kruiscontrole (`BinderRef` én
   `AttribuutRef`) tegen de resolved/gedeclareerde datatype in álle
   reken-posities, model `resolved_attr_datatype` (`checks.ml:741`). Dit vangt
   óók de (inerte) decimalen-leugen; of decimalen-consistentie afgedwongen
   moet worden is de spec-keuze hierboven.

Let op: een **BinderRef-only** fix (de eerste, onnauwkeurige spoor-7b-
formulering) zou de parallelle AttribuutRef-onder-additief-lacune open laten —
daarom is de lacune hier herkaderd als leaf-ref-breed en
operator-positie-gedreven, niet BinderRef-specifiek. Bij landing hoort de
rij-notitie in `spec/CONSTRUCTS.md` (registry-conflict: door de maintainer,
niet in de jacht).

## Reikwijdte / risico

Optie 1 is klein en mechanisch (één extra tak in WF-035's `PLUS`/`MIN`-arm).
Raakt `spec/verifier/` + `spec/IR-SPEC.md` (WF-index) + een negatief-case —
alle protected, dus maintainer-werk. Geen executor-wijziging nodig: ref en
clingo gedragen zich al correct (ref rekent door, clingo weigert luid); de
check haalt de inconsistente IR weg vóór de executors.

Verbreding (maintainersbesluit 2026-08-24, vraag 2 — alle drie
gekozen): deze kaart draagt naast de BinderRef-kruiscontrole ook de
twee zusterhekken: (a) het dubbelschrijvers-hek — twee of meer kale
BEREKENING/GELIJKSTELLING-schrijvers op één attribuut-doel zonder
ladder zijn onwelgevormd (de bewezen un-pinnable vorm uit
PHASE5D-DIFF §8.2; gelijk-besluitende kenmerktoekenningen blijven
legaal — het penningrecht-paar en de volle corpus moeten clean
blijven, corpus-breed te valideren vóór de aanscherping); (b) de
waarde-positie-AttribuutRef-aanscherping — in waarde-positie moet de
verwijzing op een attribuut resolveren, een kenmerk-endpoint is
onwelgevormd (het getolereerde has_attr||has_kenmerk-lek uit de
kenmerk-verkenning; eveneens corpus-breed te valideren). Elk hek:
WF-tekst in IR-SPEC, verifier-check, rood-eerst, en minstens één
negatief-case per hek. May touch verbreedt met: spec/IR-SPEC.md,
spec/verifier/, corpus/negatief/. GEEN spec/CONSTRUCTS.md.
