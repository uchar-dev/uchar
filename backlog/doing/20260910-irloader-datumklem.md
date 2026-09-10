# De IR-wire-kant van de Drools-datumlezer: twee kale parses, en het schema dekt ze niet

Goal: beslissen of de twee kale `LocalDate.parse` op de **IR**-wire dezelfde
patroonklem plus getuige krijgen die de case-wire op 2026-09-10 kreeg, of dat
de kale lezing met een reden blijft staan. De kaart levert het besluit plus,
bij "klem", de klem en zijn getuige. Het gaat om twee aanroepen op één plek.

## De stand (gemeten 2026-09-10)

`loader/IrLoader.java:545-546`, in `geldigheid(JsonNode)`:

```java
return new Geldigheid(vanaf == null ? null : java.time.LocalDate.parse(vanaf),
        tot == null ? null : java.time.LocalDate.parse(tot));
```

Na de case-wire-kaart (`backlog/done/20260909-date-caselezer-klem.md`) zijn
dit de **enige** kale date-parses die nog in `backends/drools/src/main`
staan, naast de twee parses ín de kalendertakken van de klemmen zelf
(`CaseRunner` r. 948 en r. 977, waar het patroon al is afgedwongen).

**Het schema dekt deze twee niet.** Dat is het verschil met de case-wire en
de reden dat dit een eigen kaart is. In
`spec/schema/uchar-case-v0.schema.json` draagt elke datumpositie
`^[0-9]{4}-[0-9]{2}-[0-9]{2}$`; in `spec/schema/regelspraak-ir-v0.schema.json`
staat er bij `$defs/Geldigheid`:

```json
"vanaf": { "type": ["string", "null"] },
"tot":   { "type": ["string", "null"] }
```

Geen patroon. Gate 2 laat dus elke string door op deze twee posities, en de
lezer erachter is `LocalDate.parse`, die — gemeten in de case-wire-kaart —
de tekenjaarvormen `+012026-03-15` (jaar 12026) en `-0500-01-01` (jaar −500)
aanvaardt. De aanname "de IR-wire is schema-gedekt, dus de klem is daar
overbodig" is dus onjuist en moet niet als grond gebruikt worden.

**Wat het corpus draagt.** Over `corpus/golden/**` plus `corpus/rood/**`:
846 `vanaf`/`tot`-posities, waarvan 836 `null` en 10 met een waarde; de
distinct waarden zijn `2025-01-01`, `2025-12-31`, `2026-01-01`,
`2026-12-31`. Alle vier binnen het patroon. De klasse is dus smal — maar dat
was hij op de case-wire ook, en daar bleek hij niet leeg.

## De vraag die beslist moet worden

Drie dingen zijn ongemeten en de kaart moet ze meten voordat ze beslist:

1. **De vormklasse.** Wat aanvaardt `LocalDate.parse` hier dat de andere
   lezers van hetzelfde IR-document weigeren? Vermoedelijk dezelfde twee
   tekenjaarvormen, maar dat is een vermoeden tot het gedraaid is — en de
   tegenlezer is hier een andere (`ref`'s IR-loader, `spec/verifier/`), niet
   `ref`'s case-loader.
2. **De producent.** Wie schrijft `Geldigheid.vanaf`/`tot` op de wire? Als
   dat uitsluitend de eigen emitter is, is de producentroute smaller dan die
   van de case-wire (waar een generator of een handmatige run legitiem is) en
   weegt het consistentieargument zwaarder dan het lekargument. Meet het,
   beredeneer het niet.
3. **Wat de klem zou moeten volgen.** Op de case-wire volgde de klem `ref`,
   en dat viel samen met het schemapatroon. Hier is er geen schemapatroon om
   mee samen te vallen, dus de kaart moet expliciet kiezen: `ref`'s
   IR-lezer, de verifier, of een patroon dat als schemavoorstel meegaat.

Een vierde uitkomst is toegestaan en misschien de beste: **het schema
repareren** in plaats van (of naast) de lezer, want een IR-wire zonder
patroon op een datumveld is zelf de afwijking. `spec/schema/` is protected —
dat wordt dan een voorstel, geen merge.

## Acceptance

- Het besluit staat in het Result, met de drie metingen hierboven erbij.
- Bij "klem": de klem staat er, weigert de gemeten vormen hard (nooit een
  `Excluded`), noemt waarde + veld + verwachte vorm, en krijgt een getuige
  die rood-eerst is getoond, met de spiegelassertie en de randbreedtes van
  beide kanten gepind — het patroon van `CaseWireDatumWitnessTest`.
- Bij "geen klem": de reden staat als commentaar op de regel, en de kaart
  legt vast dat de vormen bekend en aanvaard zijn.
- Bij een schemavoorstel: de diff staat op de tak als voorstel, niet gemerged.
- Gate 12 groen met stijgende JUnit-teller bij "klem"; gate 2 groen; geen
  andere telling beweegt. Gate 1 groen.

May touch: `backends/drools/**`, `backlog/`, en — als voorstel, niet
mergen — `spec/schema/regelspraak-ir-v0.schema.json`.

## Out of scope

- De case-wire-routes en hun klem: die landden 2026-09-10 en zijn klaar.
- De datum-LITERAL-lezer (WF-084) — andere lezer, eigen regel, eigen getuige.
- Andere lezers van hetzelfde IR-document (`ref`, `spec/verifier/`,
  `frontends/`): die mogen als TEGENMETING gedraaid worden, maar deze kaart
  wijzigt ze niet.
