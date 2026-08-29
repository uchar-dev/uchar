# De systematische ASP-vertaling, deel 2: S8–S12

Vervolg op 20260821-asp-systematisch (Part I-kern geland).

Goal: de aangekondigde secties van Part I schrijven — S8 aggregaties
en folds (aantal/som/totaal-clip/asOfDate-bucket), S9 tijdlijnen
(temporal read, pro-rata, dagentelling, de ch. 6d-machinerie
systematisch), S10 defeasibility en standing (defaults, decides,
conflict, inputval/inputtl), S11 creatie (object/feit, budget,
backlinks), S12 allocatie en uniciteit — zelfde regime als deel 1:
gedistilleerd uit backends/clingo, gate-getuigen per construct, geen
normatieve drift. Daarna de aankondigingstekst in de Part I-kop en de
S2/ch. 7-verwijzingen bijwerken (S8–S12 geleverd).

Acceptance: elk behandeld construct citeert zijn gate-getuigen; geen
normatieve drift t.o.v. RUNTIME-SEMANTICS (lenzen checken tekst tegen
code); alle acht gates groen.

May touch: `spec/ASP-TRANSLATION.md`, `backlog/`;
`backends/clingo/README.md` voor verwijzingen.

Out of scope: gedragswijzigingen in de backend; nieuwe constructen.

Result (2026-08-21): Part I is compleet — S8 aggregaties en folds
(count/som/clip/asOfDate-bucket), S9 tijdlijnen (point reads,
pro-rata, dagentelling, de heel-tijdlijn-standing-arm), S10
defeasibility (defaults, decides/conflict, de
eff-body-is-de-semantiek-regel), S11 creatie (deterministische
termen, budget, backlinks, feitcreatie-joins), S12 allocatie en
uniciteit (de aggregaat-cascade, paarsgewijze botsingsjoins, de
unie met bronindex). Alle aankondigingsteksten bijgewerkt; de
groeibelofte van ch. 7 is volledig ingelost. Twee opus-lenzen
vonden negen echte onnauwkeurigheden (geen BLOCKING), alle vóór de
merge gefixt — o.a.: de count draagt géén hek (alleen de som is
dubbel gehekt; ch. 6c-notitie hersteld), de bucketkalender prN_b
enumereert de volledige endpoint-span (de TD>0-head beperkt, niet
de kalender), de exactheids-guard zit alleen in prN_eff (dagentelling
heeft geen quotiënt), het vintage-faalpad is fail-fast en niet
leeg-for-free, de share-fence hangt aan het restant-attribuut,
clN_cond bestaat alleen voor de vergelijkings-clip, en de
initialisatie-citatie wees naar ch. 7.2 i.p.v. 7.3/7.8. Bewust
gelaten: getuigenlijsten per sectie zijn representatief, niet
uitputtend (de gates draaien het volledige corpus); de
ch. 1.2a/1.2d-subletterconventie volgt deel 1.
