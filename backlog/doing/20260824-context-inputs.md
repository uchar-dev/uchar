# Besluit-6-context-inputs: de gegenereerde top-rung

Goal: het geratificeerde-maar-uitgestelde besluit over Catala's
context-inputs uitvoeren, nu de subscope-machinerie er is. Lees
EERST spec/proposals/PHASE5-CATALA.md (het besluit over de
context-override-polariteit als gegenereerde top-rung, Reading B als
rung-volgorde) en de PHASE5C-TUTORIAL §3-randvoorwaarde: een
caller-waarde op een GELADDERD callee-doel gaat als rung, nooit als
MemberInit (de WF-050-randvoorwaarde). De plugin lowert een
context-input op een subscope naar de geratificeerde vorm: de
caller-waarde wordt een gegenereerde top-rung boven de callee-ladder
(de bestaande refuse_context versmalt of klapt om); ladderloze
callee-doelen blijven de MemberInit-route. E2E: fixture(s) plus een
eigen catala-golden door plugin → verifier → ref → clingo. Wijk je
ergens van het geratificeerde ontwerp af of blijkt een verifier-
aanscherping (de randvoorwaarde als check) nodig die het proposal
niet expliciet dekt: BLOKKADE, geen eigen keuze.

Acceptance: rood-eerst (de huidige refusal letterlijk getoond); alle
negen gates groen; de refuse-fixtureset dekt exact wat blijft
weigeren; twee opus-lenzen verwerkt vóór de merge.

May touch: frontends/catala/, corpus/golden/ (de nieuwe
catala-golden), spec/CONSTRUCTS.md (UITSLUITEND de rij die
context-inputs draagt), spec/proposals/PHASE5-CATALA.md (de
Executed-notitie), scripts/check-noop.sh (alleen indien nodig),
backlog/. Out of scope: ref/, backends/, spec/IR-SPEC.md,
spec/RUNTIME-SEMANTICS.md, spec/verifier/ (een noodzakelijk
gebleken verifier-check = blokkade, eerst voorleggen).

Result (2026-08-24): besluit 6 uitgevoerd — de gegenereerde
top-rung. De rood-eerst is vastgelegd: het oude blanket-refuse gaf
letterlijk `context (reentrant) variable "drempel": besluit 6 wants
Reading B ... A single-scope 5a program cannot realise Reading B —
refused.`

Ontwerpkeuze (binnen het geratificeerde besluit, GEEN afwijking): de
caller-arg wordt een gegenereerde top-rung IN de ladder van de
aangeroepen scope (besluit-6-notitie 2026-08-21) waarvan guard én
waarde de caller lezen, GENAVIGEERD over de `verbindendeRol`-teruglus
(callee → caller) — geen standing-machinerie. Dit is de meest letterlijke
lezing van "de caller-waarde wordt een top-rung boven de callee-ladder"
en het §4.1-hardheidspatroon (de rung leest een input-attribuut, hier
navigerend de caller-input). De navigatie-mechaniek is EMPIRISCH
gevalideerd vóór implementatie: een handgebouwde IR met een genavigeerde
top-rung is verifier-schoon en beide executors (ref ≡ clingo) matchen
`catala interpret` (70 override-wint / 90 callee-basis). GEEN
verifier-wijziging nodig — dus geen blokkade.

Plumbing (`frontends/catala/lib/translate.ml`): het blanket
reentrant-refuse is weg; een reentrant doel met eigen default wordt een
ladder-basis geforceerd (nooit een kale Berekening); de caller-override
lowert via een genavigeerde `override_exprs` naar top-rung(s), verzameld
per (callee-objecttype, callee-attribuut) en in `file_to_document`
vóórgeplakt op de ladder van dat doel. Een ladderloos callee-doel (kale
input) houdt de MemberInit-route (WF-050). `attr_ref`/`read_var`/
`check_var` zijn geherfactored tot gedeelde `scalar_read`/`kenmerk_read`
+ genavigeerde varianten (gedragsbehoud; 126 suite-checks groen).

Getuigen: fixture `frontends/catala/test/context.catala_en` (beide
polariteiten door verifier → ref → clingo) en de eigen catala-golden
`corpus/golden/jutterswet-hardheidsaftrek-catala/` (art. 9bis, byte-
identiek onder gate 9). De refuse-fixtureset is versmald: `refuse_context`
weigert nu een context-override op een KENMERK-doel (het narrowed
overblijfsel — attribuut-doel gelandt, karakteristiek/onder-de-wortel-
vertakt/root-ongeordend blijven luide flanken).

Alle negen gates groen in deze worktree (slotregels in het
sessieverslag). `scripts/check-noop.sh` NIET geraakt — de nieuwe
golden-cases berekenen `verschuldigd` (niet in de ruwe invoer) en
weigeren de no-op vanzelf; survivor-lijst ongewijzigd (24).

Lensronde (2026-08-24, 2 MINOR verwerkt):
- Bevinding 1 [CONFIRMED] — de refuse-fixtureset pinde 1 van 3 geclaimde
  context-flanken. Empirisch bevestigd (rood-eerst, letterlijke exit-3-uitvoer)
  dat de andere twee luid weigeren; ze krijgen nu elk een eigen fixture:
  `refuse_context_branching.catala_en` (een onder-de-wortel-vertakte
  caller-override → needle "branching tree", pad translate.ml:1429) en
  `refuse_context_owndefault.catala_en` (een root-ongeordend eigen default van
  de callee → needle "root-level unordered own default", pad translate.ml:1330).
  De suite pint nu alle drie (128 checks, +2). Nuance uit de bevinding
  weerlegd: een échte onder-de-wortel-vertakte (Form_a) override bereikt de
  toegewijde 1429-tak rechtstreeks — een base-loze root-ongeordende override is
  daarvoor NIET vereist; het generieke 1446-None-pad ("outside the fragment")
  hoort bij de gedeelde-basis-vorm (een andere, reeds-elders-gedekte flank),
  niet bij onder-de-wortel-vertakking.
- Bevinding 2 [PLAUSIBLE] — de top-rung leest de caller via back-link-navigatie
  (een elaboratie boven het letterlijke §4.1-zelfde-subject-patroon).
  Geverifieerd: geen defect (de bevinding meldt zelf "geen code-actie"); de
  navigatie-mechaniek is al expliciet benoemd in de Executed-notitie
  (spec/proposals/PHASE5-CATALA.md:246-247), in CONSTRUCTS rij 9 ("navigated
  over the `verbindendeRol` back-link") én in deze kaart (r. 40-48). De
  gevraagde documentatie bestaat dus al — geen wijziging.
Alle negen gates na de fix opnieuw groen in deze worktree.

Open: (afgehandeld) de twee opus-lenzen zijn verwerkt.
