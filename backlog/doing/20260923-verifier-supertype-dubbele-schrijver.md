# verifier-supertype-dubbele-schrijver: gewone regels op super- én subtype voor één doel, zonder defaults-groep

Bijvangst van de onderzoekskaart dialect-defaults-niet-en-nesting (gemeten,
oorzaak toen niet uitgezocht); het voorstel-concept
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` (§9, kaartbranch) geeft
de wortel: de verifier neemt in WF-079 aan dat een kale regel van het
subtype die van het supertype vervangt (`spec/verifier/lib/checks.ml:8899–8903`),
terwijl RUNTIME-SEMANTICS ch. 7.10 een kale regel alleen in een stapel
vouwt wanneer de keten een defaults-groep declareert
(`RUNTIME-SEMANTICS.md:5179–5184`; `spec/verifier/lib/desugar.ml:286–290`).
Gemeten: de verifier zegt niets; ref en de Drools-exec-tak vinden geen
fixpunt; clingo geeft twee waarden; de DRL-teksttak antwoordt naar
bronvolgorde. Vier uitvoerders, drie antwoorden, en de spec beslist het
geval niet.

## Te beslissen bij oppakken (spec-vraag, beschermd pad)

- (a) De stapelvervanging van ch. 7.10 geldt ook voor kale regels: de kale
  regel van het subtype vervangt die van het supertype op instanties van
  het subtype (het Flora-beginsel dat SUBTYPING §4.4 al voor blokken
  volgt). Dan: ref, clingo en beide Drools-takken implementeren het, en
  een ankercase pint het.
- (b) Twee kale schrijvers op één doel in één keten zijn ill-formed: WF-079
  (of WF-026 op de effectieve stapel, IR-SPEC ch. 6.1) weigert, met een
  negatief; de uitdrukbare vorm is een defaults-groep (basis op het
  supertype, eigen basis op het subtype), die ch. 7.10 al draagt.

Aanbeveling: (b) — het is de zin die IR-SPEC ch. 6.1 al uitspreekt ("the
single-writer rules … are evaluated on the per-type EFFECTIVE stack"), en
(a) voegt een tweede vervangingsmechanisme naast de defaults-groep toe.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproductie op alle vier uitvoerders in de kaart vastgelegd (het
  M7-document van het voorstel of een eigen rechtenvrije variant).
- (b) De gekozen weg: bij (b) een negatief `wf079-…` of `wf026-…`
  (**voorstel**), de verifier rood-eerst, en de spec-zin die het geval
  benoemt (**voorstel**); bij (a) de spec-zin, drie uitvoerders en een
  ankercase, allen exact.
- (c) Alle veertien gates groen.

May touch: `spec/verifier/**`, `spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md`, `corpus/negatief/**` (alle **voorstel**); bij
(a) `ref/**`, `backends/**`, `corpus/golden/anchor/**` (**voorstel**);
kaartverplaatsing.

Niet in deze kaart: de dialectvormen.

## Correcties bij de claim (2026-09-25, hoofdsessie)

De kaart is op 2026-09-23 op de L1-wire geschreven; de repo staat op de
L5-wire (`uchar-ir-v1` compleet, main 2f47d80e).

- Regelnummers in de kaart zijn van 2026-09-23 en gelden niet meer. Vandaag:
  de WF-079-arm en zijn comment in `spec/verifier/lib/checks.ml` r. 7611–7672
  (`let wf079`; de onware comment "A bare rule on the supertype and a bare
  rule on the subtype are not two writers" r. 7624–7628); de poortalinea van
  RUNTIME-SEMANTICS ch. 7.10 ("The fold is gated…", "Outside the gate nothing
  changes … WF-079 is what speaks about them") r. 5487–5501;
  `spec/verifier/lib/desugar.ml` r. 278–300 (de poort) en `absorbed_uncached`
  r. 526 e.v.; IR-SPEC ch. 6.1 "the per-type EFFECTIVE stack" r. 8383–8397;
  de WF-079-tekst in IR-SPEC r. 2258–2284.
- Het voorstel `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` is geland
  op main; §9.4 (r. 1425–1466) draagt het meetdocument en de wortel.
- Wire-namen van het meetdocument: `model.objectTypes[]` met `supertype`;
  `rules.rules[]` `{node: Rule, name, versions[]}` met per versie `target`
  een `AttributeReference {attribute, subjectType}`, `presentation:
  "equation"`, geen `condition`, `validity` afwezig; de uitdrukbare vorm is
  een `PrioritizedDefaults {subject, attribute, defaults[]}` per type.
- De verifier meldt met JSON-pointers in L5-spelling
  (`/rules/rules/<f>/versions/<v>/…`).

## U-besluiten (hoofdsessie, 2026-09-25)

- U1 **Norm: optie (b).** Twee kale schrijvers (onvoorwaardelijke,
  versievrije rekenende versies van verschillende vormen met overlappende
  geldigheid) op één doel in één typeketen zonder defaults-groep zijn
  ill-formed. De poort van ch. 7.10 blijft staan; WF-079 keurt voortaan per
  CONCREET type de effectieve stapel: elke onvoorwaardelijke rekenende versie
  op (T of een voorouder van T, attribuut) die niet in een stapel is
  opgenomen telt als schrijver op T (buiten de poort is dat elke zulke
  versie; binnen de poort is de opname `Desugar`'s zaak en spreekt WF-050).
  De bestaande vlakke arm en haar zin blijven; de ketenarm krijgt een eigen
  zin die beide declarerende typen noemt en de uitdrukbare vorm (een
  defaults-groep: basis op het supertype, eigen basis op het subtype — ch.
  7.10 regel 1). Geen nieuw WF-nummer. Optie (a) is verworpen: zij zou de
  poort voor deze paren opheffen en een tweede vervangingsmechanisme naast de
  defaults-groep leggen; wie kind-wint wil, schrijft de groep.
- U2 **Zin** (voorstel, de bouwer stelt de definitieve zin vast en meldt
  haar letterlijk in het Result): `two unconditional computing rules write
  target (Wrakrecht, uitkering) through its type chain — one declared on
  Strandrecht, one on Wrakrecht — and no defaults group folds them into one
  stack, so both fire on a Wrakrecht instance; priority is explicit (a
  defaults group, whose subtype base replaces the inherited base, ch. 7.10)
  or explicitly absent, never implicit agenda order (ch. 7.7)`. Pointer: de
  versie op het laagste type van het paar. Geen regelnummerstaart.
- U3 **Getuigen.** Negatief `wf079-keten-kale-schrijvers` (supertype-regel
  eerst in het document) met `expect.txt` op de ketenarm en NOTE met de
  echte verifieruitvoer; rood-eerst (verifier PASS vóór de arm, gemeten en
  geciteerd); mutatieproef. Positief: elke golden blijft schoon (gate 3),
  in het bijzonder `anchor/jutterswet-strandrechtketen` (keten MET groep),
  `jutterswet-wrakrecht`/`jutterswet-wrakconflict` en elke golden met een
  supertype waar maar één niveau het doel schrijft — geen nieuwe golden
  nodig; ontbreekt een schone drager van "één niveau schrijft, het andere
  niet", dan meld je dat als open punt (geen golden aanmaken zonder
  noodzaak).
- U4 **Reproductie (acceptatie (a)).** Het §9.4-document in L5-vorm, beide
  bronvolgordes, op verifier, `ref`, clingo, Drools exec en DRL-tak; de
  tabel in het Result. De uitvoerders veranderen NIET (optie (b)).
- U5 **Spec.** IR-SPEC WF-079 krijgt de ketenclausule (per concreet type,
  door de keten, buiten de poort van ch. 7.10); ch. 6.1's zin blijft; RS ch.
  7.10 "Outside the gate nothing changes …" krijgt de zin dat WF-079 daar
  per concreet type door de keten spreekt. Geen D-/U-nummers en geen
  procestaal in spec, comment of NOTE. De naamtabel-toets
  (`spec/proposals/v1-naamtabel-toets.py`) blijft rc 0.
- U6 De verifier-comment r. 7624–7628 wordt naar de waarheid herschreven.
- U7 **Zusterregels meten, niet bouwen:** WF-026 (toewijzingen) en WF-050
  keyen ook op het gedeclareerde type; meet met een proefdocument of een
  toewijzing op super- én subtype dezelfde kloof heeft en meld het als
  genummerd open punt. Kenmerktoekenningen blijven vrijgesteld.
- U8 **Toka:** de zone is niet verifier-schoon (28/30 documenten); tel of de
  ketenarm daar extra meldingen geeft (alleen tellingen en paden, geen
  citaten) en meld het.
- U9 **Gates** voor deze bouwer: 1, 2, 3; de unie draait alle veertien.
