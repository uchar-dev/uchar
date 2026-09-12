# typerange-sluiting — de twee open punten van de subtypering-reeks

Goal: de laatste reparatiekaart van de subtypering-reeks. Twee gemeten gaten
blijven over uit de twee lensrondes ervoor, en de maintainer vraagt "geen open
punten". Beide gaan over hetzelfde: een positie die een type NOEMT terwijl de
verzameling die haar bereikt BREDER is dan dat type.

**Gat 1 — de uitsluitingsfilter van een type-rangende positie**
(`backlog/done/20260912-subtype-verdeling-keten.md`, vervolgwerk B; daar
"bevinding 4" van de lens). `IR-SPEC` ch. 2.2 belooft extensionele
kwantificatie op elke typerange-positie. De INsluitingsrichting (een
subtype-instantie telt mee op een supertype-positie) is overal gewitnesst; de
UITsluitingsrichting niet. Gemeten op `p3-wf102-landing.json`: een
`Munt ⊑ Vondst`-vouw met `elementObjecttype: "Munt"` over een pad dat op een
`Vondst`-rol landt telt de mee-gelinkte gewone `Vondst` gewoon mee, en die
bepaalt zelfs het minimum — in ref én clingo dezelfde uitkomst, dus
spec-versus-uitvoerders en geen differentiële divergentie. De verdelings-tak
van dezelfde familie is een ronde eerder al gesloten
(`ref/lib/eval.ml`, ontvangersfilter: identiteit zodra de vuller
`doelType`-of-lager is, filter anders); dat is het model.

**Gat 2 — de frontend emitteert geen doelType op een vooroudersrelatie**
(`backlog/done/20260912-subtype-frontend-keten.md`, vervolgwerk C). Een
verdeling "over alle hoofdbergers" waarvan het feittype op `Berger` hangt
(`Hoofdberger ⊑ Berger`) emitteert een verdeling ZONDER `doelType` →
`WF-034 FAIL "allocation without a target object type"`. Daardoor is precies
het arrangement dat gat 1's zusterreparatie pint — ontvangersverzameling
smaller dan het rolbereik — niet uit een `bron.rgs` schrijfbaar, en is
`golden/jutterswet-hoofdberging` handgeschreven zonder brontweeling.

Acceptance:
- gat 1: de uitsluitingsfilter bestaat generiek op de vouw-elementverzameling
  in ref én clingo; Drools is gemeten en het antwoord (gefilterd of niet)
  gedocumenteerd; de norm is handmatig gerekend (de vouw telt precies de
  gelinkte instanties waarvan het concrete type ⊑ `elementObjecttype`);
- gat 1: een getuige-golden met handberekende pins die de vorm in alle drie
  speelt, met een mee-gelinkte niet-passende instantie die NIET meetelt;
- gat 1: de ch. 2.2-positielijst is systematisch doorzocht op posities met
  hetzelfde gat, per positie gerapporteerd als gedekt / gerepareerd / n.v.t.;
- gat 1: de spec-zin die de lacune nu eerlijk benoemt belooft weer vol, met
  de getuige erbij;
- gat 2: de onderwerp-/doelresolutie van de verdeling bindt een frase die een
  SUBTYPE benoemt op een rol waarvan de vuller een VOOROUDER is
  (⊑-verenigbaar) en emitteert dat subtype als `doelType` plus de rolbinding;
  een rol wiens vuller niet ⊑-verenigbaar is bindt nooit, en meerduidigheid
  weigert;
- gat 2: een `bron.rgs`-getuige die het hoofdberging-arrangement uit bron laat
  komen, byte-identiek re-emitterend (gate 10) en spelend in alle drie;
- alle veertien gates groen; de bestaande 45 rgs-goldens re-emitteren
  byte-identiek; `EXPECTED_PLAYED` per stap apart benoemd, met reden en datum.

May touch: `ref/`, `backends/clingo`, `backends/drools` (alleen indien de
meting het vergt), `frontends/regelspraak` (resolver/emitter voor gat 2),
`corpus/golden/` (de nieuwe getuigen), `scripts/check-drools.sh` (alleen de
`EXPECTED_PLAYED`-stappen, met reden), `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md` (alleen de gerichte zinnen), dit kaartbestand.

Out of scope: `spec/CONSTRUCTS.md`, `spec/PLAN.md`, `CLAUDE.md`, `.github/`,
`spec/verifier/` (tenzij een WF-zin mee moet — dan gemeld), bestaande
corpusbestanden, andere frontends.

Result:
