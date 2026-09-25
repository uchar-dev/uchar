# clingo-tarief-decimalen: geld × tarief met minder dan vier decimalen, en de WW-tweeling

Nulmeting van de kaart dialect-uitzondering-op-uitzondering (2026-09-25):
de RegelSpraak-tweeling van `anchor/ww-herleving-inkomen` — boom en
rechtvaardigingen gelijk aan het anker, `ref` 10/10 en de Drools-exec-tak
PLAYED 10 — is niet toegelaten, omdat clingo haar frontend-emissie op alle
tien cases weigert: `UNSATISFIABLE — viol("arithmetic range exceeded
(clingo grounds in machine integers)")`. Oorzaak, gemeten: de frontend
typeert de literal `0,8750` op haar kleinste schaal (drie decimalen; het
product op vijf), zoals `ExpressieBouwer.decimalenVanLiteral` gedocumenteerd
doet en zoals de toka-pariteit vereist; het anker schreef vier en zes. In
`backends/clingo/lib/translate.ml` eist `money_mult_shape` een tarief met
precies vier decimalen (`dec m = Some 2 && dec r = Some 4`), en het gewone
productpad hekt elke operand op 46340 — een maandloon van 2400,00 (240000
op schaal 2) valt daarbuiten. Met alleen die literal op vier decimalen
slaagt clingo 10/10. Het is een beperking van de ASP-backend, geen
frontendfout: RUNTIME-SEMANTICS §2.10 maakt het literaltype de bron, `ref`
en de exec-tak rekenen gelijk, en clingo weigert luid zonder fout antwoord.
De bron van de tweeling staat reproduceerbaar in
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §8.2.

## Acceptatie (bij oppakken aanscherpen)

- (a) De gesplitste geld-×-tariefcodering van clingo draagt een tarief met
  ten hoogste vier decimalen (het tarief exact naar schaal vier gebracht),
  met een check in de backendsuite (`backends/clingo/test/test_clingo.ml`)
  en een rood-eerst-meting op de WW-emissie.
- (b) De tweeling `corpus/golden/ww-herleving-inkomen-rgs/` toegelaten:
  `bron.rgs`, de emissie als `ir.json` (gate 10 byte-identiek), de tien
  ankercases byte-gelijk, NOTE met de structurele diff en triage — daarin
  ook het naamverschil van de groep (`toekenning art 15` tegen `recht op
  uitkering bij herleving`; `name` is geen `source`); ref ≡ clingo ≡ exec.
- (c) Pins (**voorstel**): gate 7 de overlever `case7-invoer-staat-tegen-
  blokkade` met de reden van het anker; gate 12 `EXPECTED_PLAYED` +1,
  `DRL_CHANNEL` klasse (A) +1 en de koptelling; CONSTRUCTS rij 8 (de
  hoofdsessie) meldt de toelating.
- (d) Alle veertien gates groen.

May touch: `backends/clingo/**`, `corpus/golden/ww-herleving-inkomen-rgs/**`
(nieuw), `scripts/check-noop.sh` en `scripts/check-drools.sh`
(**voorstel**), kaartverplaatsing. Niet in deze kaart: de literaltypering
van de frontend (die draagt de toka-pariteit) en de ankers.

## U-besluiten (hoofdsessie, 2026-09-25, bij de claim)

- U1 De gesplitste geld-×-tariefcodering in `backends/clingo/lib/translate.ml` (`money_mult_shape`, r. ~8130–8150) draagt een tarief met ten hoogste vier decimalen door het tarief exact naar schaal vier te brengen (0,875 op schaal 3 = 8750 op schaal 4); geen verandering voor tarieven die al vier decimalen dragen; een check in `backends/clingo/test/test_clingo.ml` (mandaat) met rood-eerst op de WW-emissie (de literal op drie decimalen).
- U2 De tweeling `corpus/golden/ww-herleving-inkomen-rgs/`: de bron uit `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §8.2 (sinds de fixronde van dialect-uitzondering-op-uitzondering draagt de emissie twee `source`-records meer op de uitzonderingsgroepen), emissie als `ir.json` (gate 10 byte-identiek), de tien ankercases byte-gelijk, NOTE met de structurele diff en triage (ook het naamverschil van de groep: `name` is geen `source`), ref ≡ clingo ≡ Drools-exec; gate 7: `case7-invoer-staat-tegen-blokkade` als overlever met de reden van het anker (**voorstel**); gate 12: `EXPECTED_PLAYED` +1 en klasse (A) van `DRL_CHANNEL` +1 met de koptelling (**voorstel**; de hoofdsessie telt de pins van parallelle kaarten op). De registerzin voor CONSTRUCTS rij 8 (de toelating) in het Result.
- U3 Gates 1–8, 10, 12, 13.
