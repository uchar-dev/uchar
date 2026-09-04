# De stille factor-één-terugval in de eenheidslogica wordt een fout

Goal: `Eenheid.tellerConvertibel`/`tellerFactorNaar`
(frontends/regelspraak/.../model/Eenheid.java, ~r. 79-128) vallen bij
een naam-mismatch stil terug op factor één — gemeten de motor achter
het kastkanaal (vóór de byte-vergelijking van WF-091: ref 3,75 tegen
3.750,00 in het Drools-pad) en strijdig met de
fail-fast-grondregel. Maak de terugval luid (maintaineropdracht
2026-09-04, tweede golf).

Aanpak: EERST de aanroepersinventaris — wie roept de twee methodes (en
`omrekenfactorNaar`/`vereistOmrekening` als die dezelfde terugval
delen): de frontend-Typering, de DRL- en exec-model-transpilers via het
gedeelde model, en meet per aanroeper of een naam-mismatch daar ná de
EffectieveEenheid-kaart nog kán aankomen (verwachting: niet — alle
argumenten komen uit declaraties of de afleiding; de terugval is dan
dood pad). Dan de vervanging: een mismatch die om een factor wordt
gevraagd faalt hard met een dragende melding, of — als een aanroeper de
terugval legitiem nodig blijkt te hebben — die ene plek expliciet en
reden-dragend, nooit stil. Let op de convertibiliteits-poortwachters
(convertibleNaar) die vóór de factorvraag horen te staan: de fout hoort
onbereikbaar te zijn op elk geldig pad, en dát is precies wat de
fail-fast bewaakt.

Acceptance:
- De inventaris met per aanroeper de meting.
- De terugval vervangen door een luide fout (of de dode-pad-conclusie
  met een fail-fast-klem die hem dood hóúdt).
- Gate 10 én 11 byte-identiek (beweegt één golden-emissie: stoppen en
  de vraag stellen); beide JUnit-suites groen (frontend 159, backend
  198); alle dertien gates groen; herpin + re-vendor (frontends
  geraakt).

May touch: `frontends/regelspraak/src/**`, `backlog/`. Nieuwe
frontend-tests alleen na de testbestanden-vraag aan de maintainer.

Out of scope: backends (consumeren het model; hun JUnit bewaakt de
uitkomst), verifier, spec, corpus.
