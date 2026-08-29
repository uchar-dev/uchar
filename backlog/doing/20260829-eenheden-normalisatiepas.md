# Eenheden: de gespecificeerde normalisatiepas (fijnste canoniek)

Goal: de taillebeslissing van LADDER-EVOLUTIE §8.2 / Q9 (geratificeerd
2026-08-29): broneenheden blijven op de wire; een gespecificeerde
wire→wire-normalisatiepas canonicaliseert eenheden naar de FIJNSTE
basiseenheid van elk stelsel (uitsluitend vermenigvuldigen — exact en
integer-encodeerbaar) en draait als clingo's voorstap, differentieel
geborgd: ref/Drools-over-bron ≡ clingo-over-canoniek. De pas leeft in de
clingo-pijplijn (geen gate-1-no-share-zorg), wordt gespecificeerd in
ASP-TRANSLATION (nieuwe sectie: de pas als lowering, met de
fijnste-canoniek-regel normatief), en heft de factor≠1-weigering in
`translate.ml` op voor alles wat de pas identiek maakt — de weigering
blijft luid voor wat erbuiten valt. Minstens één nieuwe golden met een
echte factor≠1-omrekening (bv. jaar/maand of km/m in één berekening) die
vóór deze kaart aantoonbaar door clingo geweigerd werd (rood-eerst) en
erna door alle beide executors met handberekende waarden speelt.

Acceptance: gates 5/6 groen met de nieuwe factor≠1-golden(s); de
bestaande corpus ongewijzigd (geen migratie — de pas is gate-tijd, geen
wire-wijziging); rood-eerst getoond en geciteerd; alle overige gates
groen; ASP-TRANSLATION draagt de passpecificatie zonder toka-verwijzingen.

May touch: backends/clingo/ (de pas + aanroep + tests), corpus/golden/
(nieuwe goldens), spec/ASP-TRANSLATION.md, backlog/.

Out of scope: de canonieke-eenheden-wire (optie A — genoteerd als
uiteindelijke v1-vorm); `factorToCanonical` → {num,den} (zelfde v1-epoche);
ref/Drools-wijzigingen (die converteren al); de %-kaart (los gesneden).
