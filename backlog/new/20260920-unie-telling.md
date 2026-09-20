# unie-telling: de concatenatie van onderwerpexpressies als instantietelling

Gevonden bij de herpin na de opgesomde-telling-kaart (2026-09-20): drie
toolchain-tests dragen de opgesomde vorm als GETESTE feature —
`telling_concatenatie_met_oplosbare_kenmerkfilters_wordt_een_union_accumulate`,
`telling_concatenatie_met_onoplosbare_operand_blijft_marker` en
`union_over_verschillende_elementtypes_blijft_marker` in
`ExecModelDifferentialTest` — waarin "het aantal ⟨rolselectie⟩ en
⟨rolselectie⟩" een union-accumulate wordt: een INSTANTIEtelling over de
unie van twee selecties. Dat is een andere lezing dan de
gevulde-waarden-telling waarop het weigerbesluit van
`20260920-aantal-opgesomde-telling.md` was gebaseerd. Gemeten: nul
operationele documenten dragen de vorm (alle corpora doorzocht); alleen
de drie inline-testbronnen.

## Maintainersbesluit (2026-09-20, verbatim)

"De eerdere weigering blijft gelden voor multi-operand waarde-AANTAL,
maar raakt deze vorm niet: v2.3.0 §5.7 en typeringen §4.11 definiëren
een concatenatie van onderwerpexpressies die als instantieverzameling
wordt geteld. Modelleer dit als `CountInstances` over een expliciete
`InstanceSetUnion` — of, voor dezelfde bronrange, als één telling met
een OR-filter — met deduplicatie op instantie-identiteit. De bestaande
toolchaintests blijven conformancegetuigen; de herpin wacht op de
vier-uitvoerderkaart."

## Acceptatie (bij oppakken aanscherpen)

- (a) Wire-ontwerp per het besluit: een expliciete unie-bron onder de
  instantietelling (of de OR-filter-vorm waar de bronrange gelijk is),
  met deduplicatie op instantie-identiteit genormeerd; spec-first
  (IR-SPEC + schema + verifier als voorstel). De WF-037-weigering van
  de waarde-vorm blijft byte-exact staan — de twee vormen zijn
  onderscheidbaar op de wire.
- (b) De frontend-route: de `aantalFunc`-operandenlijst (de
  geregistreerde grammatica-verruiming) emitteert de unie-vorm voor
  SETLEZING-operanden; de fail-fast van de opgesomde-kaart blijft voor
  waarde-operanden. Rood-eerst emittergetuigen; de drie
  toolchain-testbronnen als conformance-invoer (©-vrij herformuleren
  in Jutterswet-stijl voor eigen getuigen).
- (c) Alle vier de uitvoerders dragen de vorm (golden met handberekende
  absoluten, gedeeld element telt één keer — de dedup-getuige);
  onoplosbare operand en ongelijke elementtypes krijgen hun benoemde
  weigering (de toolchain markert daar — meet en volg of wijk
  gemotiveerd af).
- (d) DAARNA de herpin: de toolchain staat tot die tijd op pin
  ccf7c793 (commit 34bec57); de pin-bump neemt de drie tests als
  conformancegetuigen mee en de registerrij van de opgesomde-kaart
  wordt op de nieuwe stand gebracht.
- (e) Alle veertien gates groen.

May touch: `spec/**` (voorstel), `frontends/regelspraak/**`, `ref/**`,
`backends/**`, `corpus/**`, `scripts/check-drools.sh` (voorstel),
kaartverplaatsing. NB: serieel met Drools/check-drools-kaarten; raakt
dezelfde frontend-plekken als de opgesomde-kaart.
