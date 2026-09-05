# De Drools-backend leest het canonieke DatumTijd-lexeem niet (wire-gat)

Goal: `Literalen.parseDatumTijd` — de gedeelde literal-lezer die beide
backends compileren — accepteert alléén het RegelSpraak-bronsnippet
`dd-mm-yyyy hh:mm:ss[.SSS]` en geeft op het canonieke ISO-lexeem
`null`. `parseDatum` doet het naast haar wél goed: die leest sinds
WF-084 zowel `yyyy-mm-dd` als het dag-eerst-snippet, en de eigen
frontend schrijft voor een DAG-datum ook het ISO-lexeem in `waarde`
(snippet blijft in `bron`). Voor een `DatumTijd`-literal schrijft die
frontend nog het snippet (`ExpressieBouwer` ~r. 379), dus vandaag is er
geen levend corpusdefect — maar de IR is een wire, geen frontend-uitvoer.
Een conforme producent die het canonieke ISO-datetime-lexeem schrijft,
wordt door de Drools-backend **stil geskipt**: de vergelijking valt terug
op `null` en de hele regel verdwijnt uit de KieBase.

Gemeten (fixronde 2026-09-05, probe op een handgebroken
`corpus/golden/jutterswet-vondstverjaring`: `vondstdag` als `DatumTijd`,
regelvoorwaarde `EERDER` tegen een `DatumTijd`-literal, verder identiek —
alleen het lexeem verschilt):

    waarde "2025-03-01T00:00:00"  -> EXCLUDE transpiler SKIP: regel
                                     (voorwaarde buiten Phase-B scope)
    waarde "01-03-2025 00:00:00"  -> PLAYED 1

Dit is de eerste uitvoerdergetuige voor de discipline die WF-084
expliciet uitscopet ("datetime lexemes carry their own ISO discipline,
tracked separately — they are not exercised by any corpus literal yet",
IR-SPEC ch. 4.1). De kaart maakt van dat uitgescopete zinnetje een
gemeten verplichting: het lexeem vastleggen én de lezer eraan houden.

Naast het gat zelf zit er een tweede les in: de SKIP-reden loog niet,
maar hij was niet-onderscheidend. "voorwaarde buiten Phase-B scope"
dekte hier een parseerfout, en die verwarring heeft in de WF-036-kaart
één beurt lang voor "de backend kan geen milliseconden" doorgegaan
(zie `backlog/done/20260905-wf036-datumordening-precisie.md`, fixronde).

Acceptance:
- Meet eerst waar de fix hoort. `Literalen` staat in de frontend-kern
  (`frontends/regelspraak/.../kern/Literalen.java`) maar wordt door
  `backends/drools/pom.xml` als bron mee-gecompileerd, dus één plek
  bedient beide — de vermoedelijke landingsplaats is `parseDatumTijd`,
  symmetrisch met wat `parseDatum` al doet. Bevestig of weerleg dat met
  een meting vóór de wijziging.
- Rood-eerst: een getuige die vandaag stil skipt en na de fix speelt.
  Bij voorkeur een golden met een `DatumTijd`-literal in het canonieke
  lexeem (die het corpus nu helemaal niet heeft), zodat gate 12 hem
  daarna afdwingt; anders een gepinde probe in de backend-suite.
- De wire-kant: leg het canonieke DatumTijd-lexeem vast — de
  WF-084-passage scopet het expliciet uit, dus dit is spec-voorstel-werk
  (grammatica + de vraag of de milliseconden verplicht, optioneel of
  genormaliseerd zijn) met de bijbehorende negatief-cases wanneer het
  een WF-regel wordt.
- Neutraliteitsbewijs: dezelfde betekenis in beide lexemen levert
  hetzelfde speelresultaat — geen SKIP-asymmetrie meer tussen het
  snippet en het canonieke lexeem.
- Alle dertien gates groen; geen bestaande pin beweegt. Let op gate 10
  (de frontend-suite compileert dezelfde klasse) en gate 13 (de
  DRL-teksttak leest via `DrlTranspiler` ~r. 3716 dezelfde lezer).

May touch: `frontends/regelspraak/**` (de kern-lezer) of
`backends/drools/**` — welke van de twee, beslist de meting hierboven;
`spec/IR-SPEC.md` (voorstel, de WF-084-passage en zo nodig een eigen
regel + indexrij), `corpus/golden/**` (nieuwe getuige),
`corpus/negatief/**` (indien het een WF-regel wordt), `backlog/`.

Out of scope: de datum-ordeningsclausule van WF-036 zelf (geland,
2026-09-05); de dag-precisie-kant van WF-084 (dicht); het bijtrekken van
`Typering.vergelijk` op de precisienaad (eigen restpunt op die kaart);
`toka/`.
