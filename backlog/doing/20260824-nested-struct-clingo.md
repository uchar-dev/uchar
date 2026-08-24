# Geneste struct-veldtoegang in clingo

Goal: een genest struct-veld (o.binnen.bedrag — struct-in-struct,
veldtoegang op een veldtoegang) is verifier-clean, draait in ref en
door de Catala-keten, maar backends/clingo faalt luid ("struct
field-access base ... no corpus case for more" — de pre-existing
kaart-1-flank). Maak de struct-basis-vertaling recursief zodat
geneste veldprojectie grondt, met de bestaande schaaldiscipline
per einddatatype; een eigen-geschreven Jutterswet-golden met een
genest struct-veld door BEIDE executors met absolute verwachtingen;
de luide weigering verdwijnt uit de vertaling én uit de
S19-inventaris (S19 blijft de hoogste sectie — bij een nieuwe sectie
repo-breed hernummeren); ASP-TRANSLATION S16 uitgebreid.

Acceptance: rood-eerst (de huidige fault op de nieuwe golden
letterlijk getoond); alle negen gates groen; ref ≡ clingo over de
volle corpus; twee opus-lenzen verwerkt vóór de merge.

May touch: backends/clingo/, corpus/golden/ (de nieuwe golden),
spec/ASP-TRANSLATION.md, spec/CONSTRUCTS.md (UITSLUITEND rij 38),
scripts/check-noop.sh (alleen indien nodig), backlog/. Out of scope:
ref/, frontends/, spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md,
spec/verifier/.

## Voortgang (2026-08-24)

Gedaan:
- Golden `corpus/golden/jutterswet-vangstwaardering/` (eigen Jutterswet-
  materiaal): een `Vangst` berekent de geneste record
  `waardering{toeslag, kern{eenheden, grondslag}}` uit zijn scalars en
  leest het diepste geldveld terug via een veldtoegang op een
  veldtoegang. `heffing := waardering.kern.grondslag + waardering.toeslag`
  (125.75) en `grondslagkopie := waardering.kern.grondslag` (100.25).
  Ontwerpkeuze: de buitenste struct is COMPUTED (geneste StructLiteral,
  grondt al via `struct_literal_term`), zodat alleen `struct_base` de
  blokkade is — de geneste-struct-VELDWAARDE-fault (`struct_value_term`,
  input/expectation) blijft een aparte, nog niet gedichte S19-gap.
- Rood-eerst getoond (letterlijk): beide cases faalden met
  `TRANSLATION FAULT: a struct field-access base here is an own-subject
  attribute read or a bound binder (no corpus case for more)`; ref was al
  groen (case1 heffing 125.75 / grondslagkopie 100.25; case2 heffing 0.00
  door de ch.1.3 additieve coalesce, grondslagkopie leeg).
- `struct_base` recursief gemaakt op een `Field_access`-basis (één
  projectie-unificatie per niveau; tussenstruct op scale 0, blad op eigen
  scale). Weigering weg uit de vertaling én uit S19; S16 uitgebreid met de
  recursieve derde basisvorm + de nieuwe golden als witness; CONSTRUCTS
  rij 38 bijgewerkt.
- Leeg-flank: case2 pint beide hoeken — `heffing` 0.00 (additieve
  coalesce, ref-gedrag) en `grondslagkopie` leeg (bare-read propagatie);
  ref ≡ clingo. Case2 faalt onder --noop (concrete 0.00-verwachting), dus
  GEEN survivor-regel nodig: `scripts/check-noop.sh` ongewijzigd.

Alle negen gates groen in de worktree (zie commit-log). Open: twee
opus-lenzen vóór merge (buiten deze subagent).
