# Krijgt de Begrensd-grens een WF-omheining, zoals WF-035/036/023 de andere posities omheinen?

Goal: beslissen — en de beslissing als spec-delta of als expliciet
niet-doen vastleggen — of een `Begrensd`-grens (de `minimum`/`maximum` van een
begrenzing) een WF-regel krijgt die eist dat haar eenheid met die van de basis
verenigbaar is. Vandaag is dat de enige eenheidspositie in de gemeten set
waar de verifier een document doorlaat dat ref én clingo weigeren.

## De meting die de vraag stelt (2026-09-09, lens 1 op de kaart
`20260906-drools-eenheid-uit-declaratie`)

Mutant **N**: een `Begrensd` over twee `EUR`-attributen, met een `Literal`
ondergrens `10` geannoteerd in `kg` — en een bovengrens `100`, ook in `kg`.
Alleen de grens-annotaties dragen die vreemde eenheid; de basis en het doel zijn
`EUR`.

| consument | uitkomst |
|---|---|
| **verifier** | **PASS** — geen enkele WF-regel omheint deze positie |
| ref | `EVALUATION FAULT: incompatible units: EUR vs kg` |
| clingo | `TRANSLATION FAULT: non-identical units under a Begrensd bound are not translated` |
| Drools (vóór de eenheidsgrens) | **PLAYED** — stil, met de basiswaarde ongewijzigd |
| Drools (ná de eenheidsgrens) | `ERROR Eenheidsconflict: … 'kg' … herschaalt niet naar 'EUR' …` |

De andere drie posities zijn wél omheind, en de mutanten laten dat zien:
`WF-035` op de additieve join, `WF-036` op de vergelijkingsoperanden, `WF-023`
op de toekenning. De grens van een begrenzing valt tussen wal en schip.

Merk op waarom WF-091 hier niet redt: de grens is een `Literal`, en een
`Literal` is juist één van de drie ch. 2.10-posities waar de annotatie de
**bron** is. Er valt dus niets te kruisen tegen een declaratie — de vraag is
niet of de annotatie een declaratie correct herhaalt, maar of twee eenheden op
één plek bij elkaar passen. Dat is precies wat WF-035/036/023 elders doen.

## Wat deze kaart moet opleveren

Eén van twee, expliciet:

- **een nieuwe WF-regel** (voorstel: de begrenzing-grens moet een eenheid
  dragen die verenigbaar is met die van haar basis), met de gebruikelijke
  volledige uitrusting: ID, verifier-check, een geslaagde golden en een
  `corpus/negatief/<wf###-slug>/` die om de juiste reden faalt; of
- **een vastgelegd niet-doen**, met de reden waarom deze positie anders is dan
  de drie die wél omheind zijn, opgeschreven in `spec/IR-SPEC.md` naast de
  bestaande regels zodat de asymmetrie een uitspraak is en geen gat.

Neem in beide gevallen de N-meting hierboven op als getuige.

## Waarom dit een eigen kaart is

De Drools-backend heeft de vorm sinds 2026-09-09 dicht (de eenheidsgrens van
`Eenheidsconflict` faalt op deze positie, in beide takken), dus het
differentiële vierkant is hier al sluitend: alle drie de consumenten weigeren.
Wat open blijft is een **spec**-vraag — hoort de verifier dit document te
weigeren? — en die raakt `spec/IR-SPEC.md` en `spec/verifier/`, allebei
beschermde paden waar alleen de beheerder merget. Ze hoort dus niet in een
backend-kaart thuis.

May touch: `spec/IR-SPEC.md`, `spec/verifier/`, `corpus/golden/`,
`corpus/negatief/` — alle als voorstel, nooit als merge.

## Out of scope

- De Drools-backend: die weigert al, met reden en in beide takken.
- Ref en clingo: die weigeren al.
- De andere drie posities: die zijn omheind en blijven zoals ze zijn.
