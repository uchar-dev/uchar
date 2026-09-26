# frontend-breuk-en-navigatiekeuze: een breukliteral als rationaal op de wire, en een genavigeerde lezing kiest haar relatie of weigert in een zin

Twee producentgaten, gemeten op 2026-09-26 toen de testsuites van de oude
toolchain via uchars gate-pad (emitter → lader) liepen (rapport
`../research/toolchain-verhuizing/onderzoek-2026-09-26.md`, §2 G4).

1. **Breukliteral.** De grammatica leest RegelSpraaks rationeelgetal
   (§13.2.5, `RegelSpraakLexer.g4` r. 247: een gemengde breuk `1_1/2`), en
   de emitter schrijft dat lexeme door. De wire kent alleen een exact
   decimaal getal of een rationaal `a/b` (schema-beschrijving van Literal,
   IR-SPEC ch. 4.1); de verifier (`checks.ml`, `number_lexeme_refusal`) en
   de Drools-lader weigeren `1_1/2` en `-0_1/2` als misvormd document. De
   frontend schrijft dus een document dat haar eigen lezers weigeren.
2. **Genavigeerde lezing zonder unieke binding.** Een lezing "de prijs van
   de passagier" (rolnaam) of "de leeftijd van een Klant" (typenaam) vanaf
   een ander objecttype eindigt in de emitter met een
   `IllegalStateException` ("de lezing … navigeert, en de resolver vond
   daarvoor geen unieke binding") in plaats van een diagnose. De oude
   transpiler koos in die gevallen zelf op naam. (De proefdocumenten van
   het onderzoek dragen ©-domeinwoorden; de bouwer schrijft eigen,
   rechtenvrije Jutterswet-documenten.)

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 Breuk: de emitter normaliseert een gemengde breuk naar het rationaal
  van de wire (`1_1/2` → `3/2`, `-0_1/2` → `-1/2`; gereduceerd, teken op de
  teller; de bouwer meet of `lexeme_bare_number` een teken op een rationaal
  toelaat en volgt de verifier); de auteursspelling blijft in de bron-
  snippet (`source`). Golden `jutterswet-breukliteral` (**voorstel**: een
  regel met een gemengde breuk en een met een zuivere breuk) met cases,
  gespeeld door ref en clingo; de Drools-tak speelt of weigert met reden
  in de lijst van gate 12 (de bouwer meet; een rationale literal die
  Drools niet draagt is een redendragende niet-speler, geen stille
  uitval). Eén IrEmitterTest die de normalisatie pint.
- U2 Navigatiekeuze: de resolver bindt een genavigeerde lezing op de
  geschreven rolnaam wanneer die precies één relatie tussen de twee typen
  aanwijst, ook als er meer relaties tussen die typen bestaan; een lezing
  op typenaam bindt alleen wanneer precies één relatie de typen verbindt.
  De bouwer toetst dit aan de RegelSpraak-specificatie (read-only, niet
  citeren: `/Users/yeb/Developer/uwv/regelspraak-werkbank/toolchain/
  specification/v2.3.0/`, §13.3 over rollen en relaties) en legt de
  gekozen lezing vast in `grammar/DIVERGENCES.md`.
- U3 Blijft de binding dubbelzinnig, dan een ERROR-diagnose in de
  lezerszin van de verifier voor de vorm (de bouwer zoekt de bestaande
  WF-053-/WF-109-zin die past; bestaat er geen, dan een negatief
  `wf053-…` met de nieuwe zin, rood-eerst) — nooit een exception uit de
  emitter. `LezerszinGrensTest` blijft groen (de zin staat letterlijk in
  de verifierliterals).
- U4 Getuigen, rechtenvrij: twee relaties tussen dezelfde typen met
  verschillende rolnamen (lezing op rolnaam bindt; lezing op typenaam
  weigert), één relatie (beide binden), geen relatie (weigert). Alle
  bestaande goldens met `bron.rgs` byte-identiek (gate 10); de
  pariteitszone alleen tellen (gate 11).
- U5 Klaar: alle veertien gates groen; gate 12's pins bijgewerkt voor de
  nieuwe golden; naamtabel-toets rc 0; geen regelnummerstaarten.

## Acceptatie

- (a) U1–U5; (b) een document met `1_1/2` is verifier-schoon en speelt in
  ref en clingo; (c) geen `IllegalStateException` meer voor een
  navigatie zonder binding: elke proef eindigt in een diagnose.

May touch: `frontends/regelspraak/**`, `corpus/golden/jutterswet-breuk*`,
`corpus/negatief/wf053-*` (**voorstel**), `scripts/check-drools.sh` en
`scripts/check-noop.sh` (alleen de pins voor de nieuwe golden, **voorstel**),
`spec/IR-SPEC.md` alleen als U3 een nieuwe arm vraagt (**voorstel**). Niet in
deze kaart: de uitvoerders.
