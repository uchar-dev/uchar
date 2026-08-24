# Corpus-synthese, eerste golf: synth3 over de 5b/5c-constructen

Goal: de PLAN §8-synthesepijplijn in het klein maar echt: een eerste
golf synthetische goldens (synth3-*) gericht op de constructen die
nog dun gedekt zijn — sumtypes/match, folds over lijsten, structs
(incl. geneste), geneste laddergroepen, strikte Let, subscope-creatie
met kenmerk-init — elk geval met 2-3 cases met ABSOLUTE verwachtingen.
Admissie drievoudig per PLAN §8: verifier-clean, ref ≡ clingo exact,
en noop-refuter (of reden-dragende survivor); plus de coverage-eis:
elk geval oefent zijn geclaimde construct aantoonbaar (verwijder het
construct en de verwachting verandert — toon dat steekproefsgewijs).
Kwaliteit boven kwantiteit: 10-20 goldens. Generators zijn
/tmp-throwaway conform het bestaande synth-patroon; de canonieke
serialisatie via de gate-2-slice. KRITIEK: een gegenereerd geval
waarop ref en clingo VERSCHILLEN wordt NOOIT toegelaten — het gaat
als geminimaliseerde divergentievondst in het kaartrapport (de
adversariële route), nooit in het corpus.

Acceptance: alle negen gates groen; de admissie- en coverage-toetsen
per golden gedocumenteerd in de kaart; rood-eerst
(noop/foute-verwachting-proeven getoond); twee opus-lenzen verwerkt.

May touch: corpus/golden/synth3-*/, scripts/check-noop.sh (alleen
survivor-pins), backlog/. Out of scope: al het overige — GEEN
executor-, spec- of frontend-wijziging; een gevonden divergentie is
een rapport, geen fix.
