# infer-from-empty-property: een document draagt de keuze dat uit leeg niets volgt, en ref en clingo lezen haar niet stil anders

*Opgesteld door de hoofdsessie op 2026-11-21. Eerste kaart van het landen van "Afleiden uit leeg" langs de route die de
eigenaar koos: de wire draagt het document van de auteur met de keuze, en elke executor draagt de lezing zelf. Het
voorstel met de tabellen staat in `spec/proposals/STRICT-EMPTY.md`.*

## Aanleiding

De eigenaar besloot: uit leeg mag niets afgeleid worden, als algemene eis aan het dialect. Een regelset kiest dat met
één regel, `Afleiden uit leeg: nee`; zonder die regel, of met `ja`, geldt de lezing van nu (RUNTIME-SEMANTICS ch.
1.2–1.4). Een voorbeeld uit de Jutterswet: "Een vondst is kostbaar indien zijn taxatie groter is dan 1.000 €". Is de
taxatie leeg, dan leest de lezing van nu "niet kostbaar"; onder `Afleiden uit leeg: nee` staat niet vast of de vondst
kostbaar is.

De proef (`card/strict-empty-lowering`, niet geland) zette een document om naar een document zonder de keuze. De
eigenaar koos daarna de andere route: het document van de auteur gaat ongewijzigd over de wire, met de keuze als
eigenschap, en ref, clingo en Drools bouwen de lezing elk in hun eigen verlaging in. Die route begint met de eigenschap
zelf, en met de eis dat geen afnemer een document dat haar draagt met de lezing van nu rekent: dat zou een fout antwoord
zonder melding zijn.

## Goal

De wire kent de eigenschap `inferFromEmpty` op de wortel van een document. Het schema, de OCaml-lader en de
OCaml-schrijver lezen en schrijven haar; de verifier keurt een document dat haar draagt gewoon. ref en clingo weigeren
zo'n document bij naam, met een code uit hun register, vóór zij iets rekenen. Geen lader laat een onbekende eigenschap
op de wortel door, en een gate houdt dat vast.

De lezing zelf (de tabellen, de sluitingen, de drie standen van een kenmerk) is niet van deze kaart: zij komt met de
kaart waarin ref haar rekent.

## Besluiten vóór de bouw

Van de eigenaar (2026-11-21): de keuze staat in de regelset, als ja/nee-regel "Afleiden uit leeg"; de wire draagt het
document van de auteur; de backends dragen de lezing.

Van de hoofdsessie (voorlopig, ter bevestiging door de eigenaar):

1. **De naam** van de eigenschap is `inferFromEmpty`. Gebruik die ene spelling overal (in OCaml `infer_from_empty`),
   zodat een andere naam later één mechanische vervanging is.
2. **De vorm**: een boolean op de wortel, waarvan alleen `false` geschreven wordt. Afwezig betekent: de lezing van
   RUNTIME-SEMANTICS ch. 1.2–1.4. `true`, `null` of een andere waarde is een laadfout, zoals `EmbeddedExpression.unit`
   alleen `true` kent (één schrijfwijze per betekenis; gate 2 en gate 10 vergelijken byte voor byte). In het schema:
   `{"const": false}` met een beschrijving. De laadfout van de OCaml-lader luidt, op het pad `/inferFromEmpty`:
   `expected false, the one spelling of a document that infers nothing from an empty value, got <json>`. De Java-lader
   krijgt in een parallelle kaart dezelfde zin; wijzig hem niet zonder het in het Result te zeggen.
3. **De plaats** in de canonieke sleutelvolgorde van de wortel: direct na `format`, vóór `vocabularies`.
4. **Geen presentatieveld**: de eigenschap verandert uitkomsten. Gate 13 stript of wisselt haar niet, en IR-SPEC ch. 10
   noemt haar niet.
5. **Geen WF-regel**: de vorm is een laadfout en een schemaregel, zoals bij `EmbeddedExpression.unit`. Schrijf in
   IR-SPEC geen zin met MUST over een document die geen controle in de verifier heeft.
6. **De weigering**: ref en clingo krijgen elk één code van soort `refusal`, met de sleutel `infer-from-empty-false`
   en de parameter `form`: `REF-014` in `ref/codes.tsv`, het eerstvolgende vrije nummer in
   `backends/clingo/codes.tsv`. De betekenis: het document draagt `inferFromEmpty: false` en deze executor draagt die
   lezing niet voor `form`. In deze kaart is `form` altijd `document`. De code blijft bestaan als de executor de lezing
   later draagt: dan weigert hij er de vormen mee die hij strikt nog niet draagt. Maak dus geen tweede code.
7. **Waar de weigering staat**: op één plaats per executor waar elke ingang langs komt (voor ref de replay, de trace en
   de no-op-modus; voor clingo de vertaling), vóór enige evaluatie of vertaling. De gedeelde lader weigert niet: hij
   leest de eigenschap in het record, en elke executor beslist zelf.
8. **De verifier** doet verder niets met de eigenschap: een document dat haar draagt is welgevormd, en de modi
   (`--empty-closures`, `--dependency-edges`, `--arms`) lezen het zoals nu.

## Acceptance

Rood eerst: toon per getuige de rode stand op de code van vóór je wijziging.

- **Schema** (gate 2): een document met `inferFromEmpty: false` valideert en gaat byte voor byte rond; zelftestrijen in
  `scripts/check-ir-schema.sh` houden vast dat `true`, `null`, een tekst en een getal geweigerd worden, en dat een
  onbekende eigenschap op de wortel (bijvoorbeeld `inferFromNothing`) geweigerd wordt.
- **OCaml-lader en -schrijver**: `false` wordt gelezen en op de canonieke plaats teruggeschreven; afwezig blijft
  afwezig (elke bestaande golden en elk bestaand rood document gaat byte-identiek rond: gate 2, 3, 9, 14); `true` en
  een niet-boolean geven de laadfout van besluit 2; een onbekende eigenschap op de wortel geeft `unknown field`. De
  getuigen staan waar een gate ze draait (zeg in het Result welke gate elk van hen draait).
- **ref**: elk van zijn ingangen weigert een document met de eigenschap met `REF-014` en de parameter `form=document`,
  zonder een uitkomst te schrijven; de getuige draait in gate 5 (`ref/test/`) of in een gate die je noemt.
- **clingo**: de runner weigert zo'n document met zijn code en dezelfde parameter; de getuige draait in de suite van de
  backend (gate 6).
- `scripts/check-exec-codes.sh` (gate 8) groen: beide registers kloppen met de bronnen, parameters inbegrepen.
- De frontends in OCaml (`frontends/catala`, `frontends/flogic`) bouwen het record met de standaard (geen eigenschap) en
  hun goldens herschrijven byte-identiek (gate 9 en 14).
- Los groen in jouw worktree: gate 1, 2, 3, 4, 5, 6, 7, 8, 9, 14. De volledige set (`make gates`) draait de hoofdsessie
  vóór de landing; jij draait haar niet.

## May touch

- `spec/schema/uchar-ir-v1.schema.json`; `spec/IR-SPEC.md` (een korte paragraaf over de eigenschap: wat zij is, haar
  vorm, dat afwezig de lezing van RUNTIME-SEMANTICS ch. 1 is, dat een afnemer die de lezing van `false` niet draagt het
  document weigert; als voorstel, de hoofdsessie redigeert haar bij de landing); `scripts/check-ir-schema.sh` (de
  zelftestrijen), in een eigen commit;
- `spec/verifier/lib/` (`ast.ml`, `loader.ml`, `emit.ml` en wat het documentrecord bouwt of kopieert), en een getuige
  onder `spec/verifier/test/` als je daar een poot voor nodig hebt; dan ook `scripts/check-verifier.sh`, in een eigen
  commit;
- `ref/` (de weigering, `codes.tsv`, `ref/test/`); `backends/clingo/` (de weigering, `codes.tsv`, de suite, de README
  waar zij de weigeringen opsomt);
- `frontends/catala/lib/`, `frontends/flogic/lib/` alleen voor het veld van het record;
- deze kaart.

Mandaat voor de beschermde paden hierboven: de opdracht van de eigenaar van 2026-11-21 om voor deze route te bouwen. De
hoofdsessie leest na en landt.

## Out of scope

- De lezing zelf en `spec/RUNTIME-SEMANTICS.md`; het case-schema en de casebestanden; `spec/CONSTRUCTS.md` en
  `spec/PLAN.md` (stel de rij voor in het Result).
- `frontends/regelspraak/` en `backends/drools/` (een parallelle kaart, `regelspraak-infer-from-empty`, bouwt daar de
  regel in de bron, de Java-lader en de weigering van Drools). Raak die mappen niet aan.
- Geen golden in deze kaart: de golden met de eigenschap komt met de frontendkaart (zij draagt een `source.rgs`).
- De toolchain, de werkbank, elke regelset buiten deze repository.

## Voorbeelden en namen

Voorbeelden komen uit de Jutterswet (vondst, jutter, taxatie, gewicht, vindloon). In niets dat in de repository komt
staat een naam, regel of casus van een regelset buiten deze repository.
