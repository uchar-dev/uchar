# aantal-bronargument-welgevormd: mét elementtype, zonder bron, niemand voert uit

Gemeten door de aantal-zonder-bron-kaart (2026-09-23, open punt 1):
een `AANTAL` MÉT elementtype maar ZONDER bron-argument (ariteit 0, of
één `ListLiteral` als argument) is verifier-schoon, en alle vier de
uitvoerders weigeren hem — ref met een bodemzin. Er is geen kloof
tussen de uitvoerders, maar wel een welgevormd document dat niemand
kan uitvoeren: een groene deelverzameling die geen groen is. De
WF-037-arm van die kaart dekt alleen de vorm ZONDER elementtype; deze
tweeling (elementtype gezet, bron ontbreekt) viel buiten het besluit.

## Acceptatie (bij oppakken aanscherpen)

- (a) Meet de vormen (ariteit 0 met elementtype; ListLiteral met
  elementtype) op alle vier; leg de bodemzin-route vast.
- (b) Normkeuze: de verifier eist een bron-argument bij een
  elementtype (WF-uitbreiding, negatief per spelling) — de lijn van
  de zusterbesluiten — of de vorm krijgt een lezing. Weigering ligt
  in lijn; motiveer verbatim.
- (c) Rood-eerst per negatief; mutatieproef; refs bodemzin wordt een
  benoemde arm of een invariantnet (fail-fast).
- (d) Alle veertien gates groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` (**voorstel**),
`corpus/negatief/**`, `ref/**`, kaartverplaatsing.

## Correcties bij de claim (2026-09-25, hoofdsessie)

De kaart is op 2026-09-23 op de L1-wire geschreven; de repo staat op de
L5-wire (`uchar-ir-v1` compleet, main 2f47d80e).

- Wire-namen: `AANTAL` → `Aggregation {kind: "count" | "filledCount",
  arguments, elementType?, filter?, sourceRole?, sourcePath?}`;
  `elementObjecttype` → `elementType`; `ListLiteral` en `InstanceSetUnion`
  heten zo. WF-037 staat in IR-SPEC ch. 4.3 (de Aggregation-tabel, cel
  `arguments`, r. 4169, en de regeltekst); de zusterbesluiten: opgesomd ≥ 2
  is de waardevouw (2026-09-20), ariteit 0/1 zonder elementtype (kaart
  aantal-zonder-bron, 2026-09-23; Result §11 punt 1 is deze kaart).
- De bodemzin van `ref`: `unsupported instance-fold source shape`; de
  Drools-lader weigert `ListLiteral` als niet-gedragen knoop (`Refusal`),
  ariteit 0 als SKIP/MARKER; clingo: TRANSLATION FAULT (meet of dat een
  benoemde zin of een bodemzin uit de lijst van 36 is).
- De frontend emitteert de vorm sinds aantal-zonder-bron niet meer;
  DIVERGENCES.md §13.4.17.47 "A type as the operand: no count over all
  instances (2026-09-23)".

## U-besluiten (hoofdsessie, 2026-09-25)

- U1 **Norm: weigering.** Geen lezing "alle instanties van het elementtype"
  (die normvraag is open punt 2 van aantal-zonder-bron en blijft open). Nieuwe
  arm van WF-037, geen nieuw nummer: een telling (`count` of `filledCount`)
  die `elementType` draagt, draagt PRECIES ÉÉN argument en dat argument is
  een instantiebron (de vormen die WF-096/WF-102 als setbron kennen);
  ariteit 0 en een `ListLiteral` als argument zijn ill-formed. Sluit de arm
  precies op de gemeten tweeling (elementtype met ariteit 0; elementtype met
  één `ListLiteral`); blijkt bij de meting een derde niet-bronvorm
  verifier-schoon, dan neem je haar mee en meld je het.
- U2 **Zinnen** (voorstel; de bouwer stelt vast en meldt letterlijk): `a
  count carries elementType "Vondst" but no source operand: an instance fold
  ranges over a written source (WF-037)` en `a count carries elementType
  "Vondst" and a ListLiteral operand: a listed value is no source of
  instances (WF-037)`; voor `filledCount` dezelfde zinnen met "a filled-value
  count". Zonder regelnummerstaart. `ref` krijgt voor beide vormen een
  benoemde arm met dezelfde zin (de bodemzin vervalt voor deze vormen);
  clingo idem als zijn zin een bodemzin is; Drools blijft zoals hij is
  (benoemde `Refusal`/SKIP), gemeld in het Result.
- U3 **Getuigen.** Negatiefs `wf037-aantal-elementtype-zonder-operand`,
  `wf037-aantal-elementtype-lijstliteraal`,
  `wf037-gevuldetelling-elementtype-zonder-operand` met `expect.txt` en
  NOTE (echte verifieruitvoer); rood-eerst (verifier PASS vóór de arm,
  geciteerd); mutatieproef; een ref-suitegetuige per benoemde arm.
- U4 **Meting (acceptatie (a)):** de tabel vóór en na op verifier, `ref`,
  clingo, Drools exec en DRL-tak.
- U5 **Spec.** IR-SPEC: de WF-037-regeltekst en de cel `arguments`; geen
  RS-wijziging. Geen D-/U-nummers, geen procestaal. Naamtabel-toets rc 0.
- U6 Frontend ongemoeid; gate 10 blijft groen (meten).
- U7 **Gates** voor deze bouwer: 1, 2, 3, 5, 6 (als clingo wijzigt), 8; de
  unie draait alle veertien.
