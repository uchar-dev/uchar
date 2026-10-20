# beanlidnamen-zonder-java-identifier: een lid waarvan de naam geen Java-identifier of DRL-eigenschap oplevert

*Vastgelegd door de hoofdsessie bij de landing van `20261008-drl-teksttak-op-truth-maintenance` (2026-10-14), uit de
besluiten 24, 27 en 28 van die kaart. Bestaand gedrag van de Drools-backend op beide takken, buiten het corpus.*

## Aanleiding

De beangenerator (`schema/FactModelGenerator`) maakt van de naam van een attribuut of kenmerk een Java-veld met
`Namen.ident`. Sinds de kaart leidt één hulp in `transpiler/core/Namen` (`lidnamen`) veld, getter, setter en
DRL-eigenschap af; de generator gebruikt haar ook. Drie grenzen zijn gemeten, geen ervan raakt een golden:

- **Een naam met een cijfer vooraan of een teken dat geen Java-identifierteken is.** `Namen.ident` houdt alles van
  `\p{N}`, dus ook `²`, `³` en `½`, terwijl `Character.isJavaIdentifierPart` daar `false` geeft. Een lid "m²", "3 M" of
  "3e lid" geeft zo een Java-veld dat niet compileert: de bouw van de beans faalt luid, maar als crash, niet als
  benoemde weigering per vorm.
- **Een DRL-sleutelwoord als lidnaam.** Achttien woorden (`in`, `is`, `with`, `drools`, `nil`, …) werken niet als
  linkerkant van een DRL-constraint. De DRL-weergave weigert zo'n lid luid met het label `drl:`; de exec-model-tak
  speelt het. Onder de strenge gate 12 is dat een verschil tussen de takken, dus rood zodra een golden zo'n lid krijgt.
  De bouwer van stap 2a2 mat dat een getteraanroep in de tekst overal leest.
- **Een lid als "İş".** De generator schrijft veld `iş` met getter `getIş`; sinds de kaart vindt de tak die getter
  (vroeger zocht hij `getİş` en las het lid niet). Dat is opgelost; het staat hier omdat elke nieuwe afleiding het
  opnieuw moet halen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Meet welke van deze namen de IR toelaat (verifier, IR-SPEC §2.1) en wat ref en clingo ermee doen.
- (b) Elke toegelaten naam wordt een geldig, deterministisch en injectief Java-veld, of wordt per vorm geweigerd met
  een benoemde reden via het `Coverage`-manifest, op beide takken in dezelfde zin. Nooit een crash bij de bouw.
- (c) De DRL-weergave leest een lid met een van de achttien woorden via de getter, zodat beide takken het spelen.
- (d) Eigen documenten met zulke namen, rood eerst, door ref, clingo en beide takken; klopt het vierkant, dan een golden
  (de pin van gate 12 als voorstel).

May touch: `backends/drools/**` (`schema/`, `transpiler/core/Namen`, `transpiler/tms/DrlRendering`), `corpus/**`,
`scripts/check-drools.sh` (**voorstel**, pin).

Out of scope: `ref/`, `backends/clingo`, `spec/` (alleen meten; een spec-vraag wordt een eigen voorstel).
