# besluit-registertekst-constructs-rij-6: de Landed-alinea van 2026-10-02 in CONSTRUCTS rij 6 klopt niet meer

*Besluit voor de eigenaar, vastgelegd door de hoofdsessie op 2026-10-06, zodat
het vindbaar blijft. Bron: `backlog/done/20261006-feitcreatie-zonder-populatie.md`,
sectie "Voorgestelde registertekst (CONSTRUCTS rij 6)". Dat voorstel vervangt
het voorstel in `backlog/done/20261006-kringtoets-volgt-wf045.md` (sectie met
dezelfde naam).*

## Aanleiding

`spec/CONSTRUCTS.md` regel 42 (rij 6, "Negation over derived characteristics")
sluit af met de Landed-alinea van ronde klein 2 (2026-10-02). Die zegt dat
`check` een kring weigert die de verifier weigert, met twee uitzonderingen: een
kring op het eigen onderwerp die de eigen WARNING al noemt, en een kring door
een objectcreatie buiten een recursieve groep. Beide uitzonderingen zijn op
2026-10-06 weggehaald (records kringtoets-volgt-wf045 en
feitcreatie-zonder-populatie), en sinds die dag leest een rolcheck de relatie
die hij noemt. Het register beschrijft dus een toestand die niet meer geldt.

Een kaartsessie werkt CONSTRUCTS niet zelf bij: zij stelt registertekst voor in
haar record, en de hoofdsessie plaatst die serieel (`WAY-OF-WORKING.md` §4;
`CLAUDE.md`, "Protected paths"). Beide records van 2026-10-06 hebben nog geen
Keuring.

## Gevraagd van de eigenaar

De voorgestelde tekst overnemen of aanpassen. Zij staat in het record
feitcreatie-zonder-populatie, sectie "Voorgestelde registertekst (CONSTRUCTS
rij 6)", en vervangt een bijzin in de Landed-alinea van 2026-10-02. Het eerdere
voorstel in het record kringtoets-volgt-wf045 vervalt daarmee.

Mee te wegen:

- De slotzin van de rij, "Recorded open: the exec-model branch plays a WF-045
  cycle to a fixpoint without a word (IR-SPEC ch. 5.5)", raakt het voorstel
  niet. De meting voor kaart `20261006-check-wf046-door-recursieve-creatie.md`
  laat zien dat beide Drools-takken ook een ontkende rolcheck op een kring door
  een recursieve creatie zonder melding spelen. Of dat nu in het register komt
  of pas bij die kaart, is een keuze.
- Bij ronde klein 2 werd de registertekst geplaatst in de commit met de Keuring
  van het record ("Registertekst geplaatst in deze commit"); hier ligt hetzelfde
  voor de hand, voor beide records.

## Acceptatie

- `spec/CONSTRUCTS.md` rij 6 draagt de aanvaarde tekst.
- Beide records hebben een Keuring die de plaatsing noemt; deze kaart staat in
  `done/` met een `Result:`.
- Gate 1 groen.

## Omvang

Klein: één bijzin in één tabelcel, en een Keuring per record.

May touch: `spec/CONSTRUCTS.md` (rij 6), de twee done-records (Keuring),
kaartverplaatsing.
Niet in deze kaart: andere rijen van CONSTRUCTS; de spec en DIVERGENCES (die
zijn in de rondes zelf bijgewerkt).
