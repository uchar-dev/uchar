# Use-site ≡ declaratie: het keuzetype-gat dat WF-088 niet dicht

Goal: WF-088 (en WF-061 voor de somvariant) binden een use-site aan het
domein dat hij **noemt** — dezelfde waarden, dezelfde volgorde. Geen
regel bindt een use-site aan de declaratie waaruit hij **leest**. Een
`AttribuutRef` mag dus een `type` dragen dat een ánder, intern consistent
keuzetype-domein noemt dan het attribuut waarvan hij leest; het document
is verifier-schoon, WF-063 controleert de armen tegen het genoemde
domein, en ref fault bij evaluatie op een label dat geen arm heeft
(`ref/lib/eval.ml`, beide Match-armen dragen sinds fixronde 2 de eerlijke
melding in plaats van "unreachable on a verifier-clean document").

De som-kant is pre-existent en identiek: WF-061 loopt attribuut- en
parameterdatatypes plus de nominale vergelijkingscheck in `walk_cond` —
geen expressie-`type`, geen `FunctionDef`. Het enige bestaande
kruisverband is WF-027's leaf-ref-cross-check, en dat leeft alleen in de
objectcreatie-initialisatie (negatief `wf027-lying-attribuutref-type`).

Twee flanken, één vraag:
1. **De use-site-kant.** Moet een expressie-`type` (en een
   `FunctionDef`-parameter/resultaat) tegen de declaratie gekruist worden
   die hij leest, zoals WF-023/WF-027 dat voor de soort doen? Dan is de
   ref-fault echt onbereikbaar en mag de belofte terug in de melding.
2. **De declaratie-kant.** WF-088 slaat `domeinen` bewust over (de
   declaratie ís de waarheid), maar een geneste `Enumeratie` ín een
   SumType-payloaddeclaratie is óók een use-site van dat enum-domein en
   wordt nergens gecontroleerd. Genoteerd in
   `corpus/negatief/wf088-functiondef-drift/NOTE.md`.

Acceptance:
- Beide flanken als flankenpaar gereproduceerd (rood-eerst), met de
  gemeten uitkomst per executor (ref, clingo, Drools).
- Een besluit: regel erbij (proposal-diff op het beschermde pad, met
  negatief-geval per as) óf een gemotiveerde vastlegging dat de melding
  de waarheid is en het gat blijft.
- Als de regel landt: de melding in `ref/lib/eval.ml` (beide Match-armen)
  en de S14-prosa terug aanscherpen.
- Alle twaalf gates groen.

May touch: `spec/verifier/**` (proposal), `spec/IR-SPEC.md` (proposal),
`corpus/negatief/**`, `ref/**`, `backlog/`.

Out of scope: de v0-wire-vorm van `Enumeratie`/`SumType`, WF-087.
