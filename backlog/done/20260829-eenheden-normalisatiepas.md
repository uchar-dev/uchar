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

Result: backends/clingo/lib/normalise.ml — de fijnste-canoniek-lowering
als wire→wire-voorstap in de clingo-pijplijn (Q9). Fijnste eenheid =
kleinste factorNaarCanoniek; herstelling erin bewezen positief-geheel
(vermenigvuldig-alleen); twee luide faultmodes (geen exacte
korte-decimaal-lezing; stelsel zonder geheel-fijnste); identiteitspoort:
de pas leest een stelsel alleen bij een echt af te leiden herstelling —
wat hem byte-identiek houdt op factor-1-wires (door de lens bewezen over
370/370 pre-existente goldens, sterker dan gate 6 alleen) en gate 11
groen (een toka-tijdstelsel met ms-factor zonder korte lezing blijft
onaangeraakt). Twee rood-eerst-goldens (sleepbaan km/m ×1000,
vergunningsduur jaar/maand ×12) met handafleiding — beide falen
aantoonbaar op de pasloze vertaler en spelen nu door het volle vierkant
(gate 12 played 68). De beslissende exactheidsaanval van de lens: de
scale-bump-non-fit fault in BEIDE executors symmetrisch, geen stille
afronding. Verwerkte lens-MINORs: (1) de clingo-unitsuite draait nu ÍN
gate 6 met een nul-checks-is-rood-eis (de faultmode-bewijzen waren
alleen machine-gecheckt in een suite die geen enkele gate draaide);
(2) S6/S19 versmald naar het getuigde: condities/vergelijkingsposities
daalt de pas nooit in (luide weigering blijft), clamp/fold is
code-gedekt maar corpus-ongetuigd — geregistreerd jacht-getuigenpunt;
(3) de hele-stelsel-integraliteitscheck blijft bewust conservatief
(luid, nooit stil). Alle twaalf gates groen. Buiten scope gebleven:
canonieke wire (optie A, v1-epoche), {num,den}-factor, rate-eenheden,
attribuut/parameter-gedragen grovere operanden (architectonisch
v0-onbereikbaar zonder de executors te splijten — gedocumenteerd).
