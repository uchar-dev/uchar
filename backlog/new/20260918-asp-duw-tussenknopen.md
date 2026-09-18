# asp-duw-tussenknopen: drie knopen die de ch.-10.6-herschrijving niet dekt

Rest van de asp-hek-ch102-tak-kaart (2026-09-18). De til-herschrijving
daalt af door de vier zuivere rekenwikkels en door niets anders. Drie
tussenknopen tussen een `Let` en de takknoop blijven op de oude, eager
plaatsing staan, waardoor clingo daar kan melden of afbreken waar ref
speelt:

1. **Geneste `Let`** — gemeten door de slotlens op twee eigen invoeren
   (regelniveau: `Let k … in Let z = teller/deler in Let w = 1,00 in
   (als k > 10,00 dan w anders z)`; gesloten body: idem met `Let m =
   grondslag/x`). Divergeert op alle gemeten revisies, main inbegrepen
   — pre-existent, latent, nul corpustreffers.
2. **Vouwlichaam** — niet gemeten; de directe vorm (rule-level `Let`
   gelezen ín een gesloten lichaam) is WF-064-ill-formed en weigert
   luid aan beide kanten, maar een `Let` bínnen het lichaam met een
   tussenknoop is dezelfde open vraag.
3. **Aggregatielichaam** — niet gemeten.

Geen terugvalredenering meer; dat was de les van de kaart. Elke vorm
wordt gemeten en dan gedragen of luid geweigerd.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: per knoop een eigen invoer, verifier-clean, ref als
  arbiter (PASS), met de huidige clingo-uitkomst letterlijk.
- (b) Per knoop: dragen (de herschrijving of een gelijkwaardige
  constructie die de barrière-reikwijdte intact laat — de reikwijdte-pin
  van de slotlens is de verplichte getuige) óf een luide, benoemde
  weigering. Nooit een stil fout antwoord.
- (c) Suite-getuigen beide richtingen plus mutatieproef; alle veertien
  gates groen; `EXPECTED_MIN_SUITE_CHECKS` blijft een vloer (groei vergt
  geen scriptwijziging).

May touch: `backends/clingo/**`, deze kaart en haar verplaatsing binnen
`backlog/`.
