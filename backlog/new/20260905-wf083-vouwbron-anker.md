# WF-083 ankert de zelfbenoemende vouwbron op het verkeerde objecttype

Goal: WF-083 controleert de zelfbenoemende rol van een vouwbron tegen
het **operand**-objecttype — `spec/verifier/lib/checks.ml` (~:2497,
`self_denoting_role ctx ~otype:otn nr`) — terwijl beide executors op het
**vouwSUBJECT** ankeren. En geen enkele regel kruist het
vouwbron-objecttype tegen het subject of tegen het
`elementObjecttype`. Daardoor kantelt één wire-veld het verifier-oordeel
zonder dat een executor beweegt, en blijven vormen verifier-clean die
beide executors weigeren.

Drie gemeten getuigen (2026-09-05, eigen bron, jaargranulariteit;
Vondst-subject, `Eén bewarende jutter bewaart meerdere vondsten`):

**1. Het anker.** Dezelfde set-vouw, dezelfde `navigatieRol` (`vondst`),
alleen het operand-`objecttype` gedraaid:

```
objecttype "Vondst" : verifier PASS
objecttype "Jutter" : WF-083 FAIL /resolvedRegels/0/waarde/argumenten/0:
                      navigatieRol 'vondst' on a fold source does not
                      denote the fold subject …
ref     : in BEIDE gevallen EVALUATION FAULT: unsupported instance-fold
          source shape
clingo  : in BEIDE gevallen dezelfde luide §7.1-weigering
```

Eén wire-veld kantelt de verifier; geen executor beweegt. Het veld dat
de executors wél lezen — het vouwsubject — komt in de regel niet voor.

**2. De ontbrekende kruising.** Een temporele `TOTAAL` met een
operand-objecttype dat niet het subjecttype is en zónder navigatie:

```
verifier : PASS   (gemeten op 73b824a, de vierde-golf-verifier)
ref      : EVALUATION FAULT: unsupported instance-fold source shape
clingo   : TRANSLATION FAULT: the temporal total of "bewaartarief" is
           declared on "Jutter", not on the subject type "Vondst" …
           (reden-dragend sinds 20260904-clingo-setfold-armen)
```

Beide executors weigeren, de verifier laat door. Precies de vorm die
vóór die kaart stil 99 antwoordde: de verifier had hem kunnen vangen en
deed dat niet.

**3. Ref ≢ clingo onder een welgevormd document.** Een NIET-temporele
set-`TOTAAL`, verder identiek:

```
verifier : PASS
ref      : V1.bewaarlast = 0
clingo   : TRANSLATION FAULT: expression kind not translated
           (no corpus case exercises it)
```

Een welgevormd document waarop de twee executors het oneens zijn — de
soort divergentie die per PLAN §6 per definitie een spec-bug is.

Acceptance:
- Het anker verplaatst van het operand-objecttype naar het
  **vouwsubject**, plus de ontbrekende kruisregel (vouwbron-objecttype
  tegen subject dan wel `elementObjecttype`).
- Elke nieuwe of gewijzigde regel krijgt een negatief-case én
  arm-exact mutant-bewijs: de mutatie die de regel uitschakelt maakt
  precies dat negatief groen en niets anders.
- De regressiegrens geteld over alle vier de zones vóór het landen
  (verwachting: 0 van de 79 enkelvoudige `AttribuutRef`-vouwoperanden
  beweegt; 74 daarvan bestonden al vóór de vierde WF-034-golf).
- Poort 3 telt minstens twee negatiefs erbij; alle dertien gates groen.
- De kaart wordt op `73b824a`-of-later gewerkt: de wart-versmalling van
  de vierde golf zit tegen deze arm aan. Die versmalling is
  verdict-neutraal gemeten over 64 getuigedocumenten (0
  verdictverschillen), dus de metingen hierboven staan er ook op.

May touch: `spec/verifier/**` (voorstel — beschermd pad, de maintainer
merget), `spec/IR-SPEC.md` (voorstel — idem), `corpus/negatief/**`,
`backlog/`.

Out of scope: de executors — die weigeren de vormen al luid, en de
`clingo`-kant is net gedaan
(`backlog/done/20260904-clingo-setfold-armen.md`); ref's foutmelding
verfijnen (`unsupported instance-fold source shape` noemt de vorm niet,
maar hij is luid en dat is een eigen kaartje).
