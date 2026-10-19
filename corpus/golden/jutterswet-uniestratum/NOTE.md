# jutterswet-uniestratum — een NEGATIEF takfilter is een stratificatierand

De ordegetuige. Een unie-tak mag een deelselectie dragen die een AFGELEID
kenmerk NEGATIEF leest, en dan moet de regel die de unie telt ONDER de regel
staan die dat kenmerk schrijft (`RUNTIME-SEMANTICS.md` ch. 6.1, ch. 3.4a voor
de gewone vouw en ch. 3.4d voor de unie). Deze golden is hand geschreven: de
RegelSpraak-frontend lost "die niet gemerkt zijn" niet op naar een kenmerk van
het element en weigert de zin bij naam, dus er is geen `source.rgs` en geen
producentroute — de vorm bestaat alleen op de wire.

**De regelvolgorde is opzettelijk verkeerd om.** De consument (`ongemerkt
aantal`, en het kenmerk `alarmerend` dat erop leunt) staat BOVEN de producent
(`gemerkt bij gewicht`). In producentvolgorde antwoordden alle vier de
uitvoerders ook vóór de reparatie goed; wat de analyse draagt, draagt zij pas
als de volgorde haar nodig heeft.

## Wat er gemeten fout ging (2026-09-22, vóór de reparatie)

`Stratificatie.verzamelReads` in de gedeelde Drools-analyse daalde alleen af
in geneste CONDITIES — de bullets van een samengestelde, het filter en de
toets van een kwantorvouw — en nooit in de EXPRESSIE-operanden van een
conditie. Een regel wier `condition` een aggregatie tegen een grens
vergelijkt, en wier aggregatiefilter een afgeleid kenmerk negatief leest, zag
er voor die analyse uit als een regel die niets leest. Gemeten:

    ref     kenmerk afwezig   (de wandeling ziet het filter, negatie erbij)
    clingo  kenmerk afwezig
    Drools exec-model   AANWEZIG   ← vuurt in het overgangsvenster
    Drools DRL-tekst    AANWEZIG   ← idem; de analyse is gedeeld

Twee antwoorden, twee uitvoerders elk, op een verifier-schoon document. Een
kenmerktoekenning is monoton: eenmaal toegekend wordt zij niet teruggenomen,
dus het overgangsvenster is het eindantwoord.

**Het gat was niet dat van de unie.** Dezelfde meting met een GEWONE
eenbronsvouw met hetzelfde filter divergeert identiek, en deed dat al sinds
dat filter landde. Eén wandeling sluit beide; de Drools-suite pint beide
helften (`FoldFilterStratumWitnessTest`), deze golden de unie-helft onder de
poorten 5, 6 en 12.

## De handberekening

`V1` weegt 10 en `V5` 10 en `V7` 9 — alle drie boven de drempel 5, dus
gemerkt. `V4` weegt 3 en `V6` weegt 2 — ongemerkt.

| jutter | aangemeld | geborgen | ongemerkt per tak | unie | alarmerend |
|---|---|---|---|---|---|
| `J1` | {V1} | {} | {} en {} | **0** | nee |
| `J2` | {V4} | {} | {V4} en {} | **1** | ja |
| `J3` | {V5, V6} | {V6, V7} | {V6} en {V6} | **1** | ja |

`J1` is de ordegetuige: nul ongemerkte vondsten, dus het kenmerk hoort
AFWEZIG te zijn — en dat is precies wat de mutant omzet. `J3` is er
bovendien een dedupgetuige bij: `V6` is zowel aangemeld als geborgen en
ongemerkt, dus zij zit in beide takken en telt één keer — de som der delen is
2, de unie 1.

## Falsifieerbaar

Zet de expressiewandeling in `Stratificatie` uit (één guard, verder niets):
beide Drools-takken melden `kenmerk alarmerend: verwacht afwezig, werkelijk
aanwezig` op `J1`. Terug aan: beide PLAYED.
