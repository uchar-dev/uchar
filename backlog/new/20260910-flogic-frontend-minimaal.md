# Minimale F-logic-frontend: subtypen uit een tweede taal, met Flora-2 als externe maat

Goal: `frontends/flogic` met de minimale F-logic-snede uit `spec/proposals/SUBTYPING.md` vraag 6: subklasse-declaratie (`c::d`), lidmaatschap (`o:c`), signatures (`c[attr=>type]`), attribuutwaarden (`o[attr->v]`) en Horn-regels, gelowered naar dezelfde kernel-subtypering als het RegelSpraak-dialect; alles buiten de snede (HiLog, meta-programmeren, defeasible inheritance, negatievormen, meervoudige ouders) weigert luid met verwijzing naar `OPDRACHT_OBJECTTYPE_HIERARCHIE_20260910.md`. De use case van opdrachtdeel 1.5 wordt in F-logic-bron toegevoegd en per frontend-diff (de penningrecht-methode van Phase-5d) naast de dialectvorm gelegd: elk verschil wordt gecategoriseerd en alleen administratieve of getrackte verschillen zijn toelaatbaar. Als de onderzoekskaart Flora-2 bruikbaar verklaarde: draai de use case ook door Flora-2 en leg de uitkomstvergelijking vast als externe-executor-notitie (geen gate, wel bewijsstuk dat onze inheritance-semantiek de JACM-lijn niet stilzwijgend tegenspreekt).

Acceptance: de F-logic-golden speelt groen door frontend → verifier → executorreeks en is via de frontend-diff tegen de dialect-golden verantwoord (geen categorie-(iii)-verschillen); de weigeringslijst heeft per punt een negatieve fixture; de volledige gate-reeks is groen; de eventuele Flora-2-vergelijking staat als notitie in het proposal of de kaartafronding.

May touch: `frontends/flogic/` (nieuw), `corpus/` (de F-logic-golden + fixtures), `spec/proposals/SUBTYPING.md` (alleen de Flora-2-notitie), en de eigen kaart.

Out of scope: kernel- en executor-wijzigingen; ErgoAI-integratie of licentieonderzoek daarvan; Flora-2's overriding-syntax in de frontend-snede (de sémantische vergelijking met F-logic's niet-monotone overerving hoort bij de onderzoekskaart, want besluit 3 maakt onze groepserving zelf een vorm daarvan); volledige F-logic-dekking — dit is nadrukkelijk een minimale snede met luide randen.

## Aangescherpt door de onderzoekskaart (2026-09-11)

`spec/proposals/SUBTYPING.md` §8 is bindend; hieronder de uitvoerbare
neerslag.

**Gemeten bevinding: de snede zoals de opdracht haar vastlegt kan haar
eigen use case niet uitdrukken.** In Flora-2 2.2 is de érfbare
klasseniveau-waarde `c[|attr -> v|]` — mét de balken — en niet
`c[attr -> v]`; de JACM-vorm `*->` is Flora-1-syntax en wordt door de
2.2-parser geweigerd (gemeten). De opdrachtsnede noemt alleen
`c[attr=>type]` en `o[attr->v]` en kan daarmee geen enkele érfbare
default schrijven, dus ook de pinguïn-toets niet.

*Aanbevolen*, niet beslist: verbreed met **`c[|attr -> v|]`** (lowert
naar een uchar defaults-**basis** op het doel van de klasse — precies
de vorm van besluit 3) en **`c[|attr => type|]`** (de
instantiesignatuur), en maak de ongebalkte klasseniveau-waarde
`c[attr -> v]` een **weigering**, want uchar kent geen
klasseniveau-attribuutwaarde en stille lowering naar een
instantiedefault zou de betekenis veranderen. De opdracht heeft de
snede vastgelegd, dus verbreden is een **opdrachtamendement en een
maintainerbesluit** — SUBTYPING §11.4. Begin deze kaart niet voordat
dat punt beantwoord is; als de maintainer de verbreding afwijst, moet
de use case van deze kaart een andere zijn dan die van §7.

**Weigeringslijst.** Meervoudige ouders (twee `::`-feiten met
onvergelijkbare ouders, met `RechtOpUitkeringWazo` en
`RechtUitkeringZw` bij naam); HiLog en meta-programmeren; defeasible
reasoning (argumentatietheorieën, `\overrides`, `\neg`); negatievormen
(`\naf`, `\neg`); **`setsemantics{inheritance=monotonic}`** — uchar's
overerving is niet-monotoon per besluit 3 en de twee modi zijn niet
uitwisselbaar; de ongebalkte `c[attr->v]`; en al het overige buiten de
verbrede snede. Elke weigering verwijst naar
`OPDRACHT_OBJECTTYPE_HIERARCHIE_20260910.md`.

**Golden.** `corpus/golden/jutterswet-wrakrecht-flogic` (het
`-flogic`-achtervoegsel volgt de bestaande `-catala`-conventie), naast
de dialectvorm `corpus/golden/jutterswet-wrakrecht` in de
frontend-diff; beide drukken de kernel-anchor
`anchor/jutterswet-strandrechtketen` uit.

**Flora-2: bruikbaar verklaard, maar begrensd.** Installeerbaar en
geïnstalleerd (buiten de repo): XSB 5.0 uit broncode, daarna de
Makeself-`.run` van Flora-2 2.2 beta uitgepakt met
`--noexec --keep --nox11 --target` en het gedocumenteerde
`floraconfig`-pad gedraaid; de installer ondersteunt Darwin expliciet.
Wat zij kan: de pinguïn-toets en de vierceltabel exact reproduceren —
gemeten, en cel voor cel gelijk aan besluit 3. Wat zij niet kan: het
§9.5-conflictgeval überhaupt uitdrukken (geen conflictkanaal, en
kardinaliteitsbeperkingen worden nooit afgedwongen — zij geeft beide
botsende waarden stil terug), en geen eenheden, decimalen, tijdlijnen
of verdeling dragen. **Dus: leg de vergelijking vast als notitie over
het overervingsskelet alleen — nooit een gate, nooit een vierde been
van de differential.** De letterlijke uitvoer van de vier proeven staat
in SUBTYPING §8.3 en hoeft niet opnieuw gedraaid te worden.
