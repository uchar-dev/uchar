# toka-vrijgave: de besluiten van 2026-10-13 over TOKA en de publieke repositories

*Kaart van de hoofdsessie, 2026-10-13. Grondslag: het besluit van de eigenaar van 2026-10-13 ("Herkomst en status")
en de technische gevolgen die de hoofdsessie daaruit trok. De inventaris en het bronnenonderzoek van dezelfde dag staan
buiten de repository; de labels hieronder (C1-b, C2-a, C7-a enz.) komen uit die inventaris en staan hier met hun
inhoud.*

## Besluiten

Van de eigenaar (2026-10-13):

1. **Herkomst en status.** Weg gaan de zinnen over privé blijven zolang de zone bestaat, over nooit citeren, over
   "not redistributed", en over de publieke flip die zoneverwijdering en een history rewrite vereist. Voor TOKA komt
   overal één vaste tekst (bron, rechtenvermelding van de bron, status van de vrijgave, licenties), inhoudelijk gelijk
   in `toka/README.md`, `README.md` en `CLAUDE.md`, met per repository de lijst van letterlijk overgenomen en van
   afgeleide of eigen bestanden.
2. **Publiek.** De repositories uchar en regelspraak-toolchain zijn publiek sinds 2026-10-12. Er komt geen history
   rewrite: de zone en haar geschiedenis blijven.

Van de hoofdsessie (technische gevolgen):

3. **C1-b.** De TOKA-tripwire van gate 1 houdt hetzelfde gedrag. De reden wordt de herkomst: het corpus is eigen werk,
   en TOKA is materiaal van derden waarvan de vrijgave nog niet schriftelijk bevestigd is.
4. **C2-a.** Gate 11 wordt een gewone, blijvende gate. "TEMPORARY" en de uitzondering op monotone groei verdwijnen.
5. **C7-a.** `uchar-dev/regelspraak-toolchain` gaat uit het PRIVATE-patroon van gate 1; de uitslag van gate 1
   verandert daardoor niet.
6. De regel "geen RegelSpraak-specificatiecitaten" blijft. De reden mag zeggen dat de specificatie "Alle rechten
   voorbehouden" draagt; de ©-taal over TOKA verdwijnt.
7. Het gedrag van geen enkele gate verandert, behalve C7-a: alleen commentaar, meldingen en tekst.

Goal: de levende teksten, scripts en commentaren van uchar volgen besluiten 1 tot en met 7. De vaste TOKA-tekst staat
gelijk op de drie plekken; de beschermde paden dragen de nieuwe redenen (C1-b), gate 11 als gewone gate (C2-a) en het
PRIVATE-patroon zonder de toolchain (C7-a); de voorgestelde tekst voor `spec/PLAN.md` en `spec/CONSTRUCTS.md` staat in
het Result, voor de hoofdsessie.

Acceptance:

- alle 16 gates van `CLAUDE.md` groen, lokaal gedraaid op de laatste inhoudelijke commit van de branch;
- de vaste TOKA-tekst is gelijk op de drie plekken: mechanisch vergeleken na het weghalen van het citaatteken (`> `)
  en de inspringing;
- een grep op de levende bestanden (zonder `backlog/done/`, `spec/proposals/` en de originele tests in `toka/src/`) naar
  `stay private|must stay private|public flip|history rewrite|filter-repo|not redistributed|TEMPORARY` geeft alleen
  bedoelde resten, elk met reden in het Result;
- C7-a: het oude en het nieuwe PRIVATE-patroon geven in de artefactzones van gate 1 hetzelfde aantal treffers, en gate 1
  geeft vóór en na de wijziging PASS.

May touch: `CLAUDE.md` (corpusregels, gate 1, gate 11, het mechanisme bij Protected paths, de repo-relaties),
`WAY-OF-WORKING.md` (§5–6), `scripts/check-self-contained.sh` (commentaar, meldingen, het PRIVATE-patroon),
`scripts/check-toka-parity.sh` (kop en commentaar), `scripts/admit.sh` (de melding), `.github/CODEOWNERS` (de kop),
`.github/workflows/gates.yml` (de job van gate 11), `README.md`, `corpus/README.md`, `toka/README.md`,
`toka/cases/parity.manifest` (alleen commentaar) en deze kaart. De eerste zeven zijn beschermde paden: de branch blijft
een voorstel en de hoofdsessie merget.

Out of scope: `spec/PLAN.md` en `spec/CONSTRUCTS.md` (voorgestelde tekst in het Result; de hoofdsessie landt die);
branch protection (C4); de licentie van `backlog/` en van de documenten in de wortel (L2/L3); het schema-`$id` onder
uchar.dev (D); privéverwijzingen in gearchiveerd proza (P); de website; de toolchain; elke gedragswijziging van een gate
buiten C7-a.
