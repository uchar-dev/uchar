# De TOKA-stresstest: een productiecasus door de uchar-keten

Goal: de TOKA-casus van de regelspraak-java-toolchain (dáár, en
alleen daar) door de uchar-keten: de resolver dumpt de IR, de
uchar-verifier keurt, ref en clingo rekenen, en de uitkomsten worden
naast de toolchain-eigen (Drools-)verwachtingen gelegd — de eerste
directe toets van het indirecte Drools-been (de bewust open flank
uit Phase 2) én een echte-wereld-stresstest van de corpus-subset.
RECHTENREGEL, absoluut: al het TOKA-materiaal (bronnen, dumps,
cases, waarden) blijft in de toolchain en /tmp; in dit repo landt
uitsluitend deze rapportkaart met geaggregeerde technische
bevindingen — constructsoorten, aantallen, WF-/S-verwijzingen,
divergentieklassen — zonder één geciteerde bronregel of
dossierwaarde. De toolchain is strikt read-only (geen commits daar).

Acceptance: het rapport in deze kaart beantwoordt: (a) hoeveel van
de casus de verifier schoon passeert en waar hij weigert (per
WF/constructsoort); (b) waar ref en clingo dragen, weigeren of
faulten; (c) voor het uitvoerbare deel: stemmen ref- en
clingo-uitkomsten overeen met de Drools-verwachtingen (aantallen,
divergentieklassen); (d) de eerlijke conclusie over het directe
Drools-been; (e) de gaten-inventaris als kaartkandidaten. Gates
blijven groen (dit spoor wijzigt niets aan artefactzones); twee
opus-lenzen (reproductie + rapport-eerlijkheid) verwerkt.

May touch: backlog/ (deze kaart). Out of scope: elke wijziging aan
spec/, corpus/, ref/, backends/, frontends/, scripts/; elke
toolchain-commit.
