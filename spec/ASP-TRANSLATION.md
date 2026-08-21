# ASP translation

*Status: hand exercises (Phase-1 early start of the Phase-3 deliverable,
per `PLAN.md` §6: "start the hand translation of one representative rule
during Phase 1"). Scope: exercise 1 (ch. 1–5) — Jutterswet art. 1
(verkrijging) and the art. 2 default (strandvondeling), in the v1 reading
of `corpus/jutterswet/versiegeschiedenis.md`; exercise 2 (ch. 6) — the
wet-v2 layered exclusion (art. 4 gelaagd + art. 4bis), where
stratification becomes load-bearing. The systematic translation of the
core IR is the Phase-3 deliverable; this document records the first
contacts between the IR's constructs and a semantically alien target, and
feeds its findings into the delta column of `CONSTRUCTS.md`.*

## 1. Source and IR view

The two articles (`corpus/jutterswet/wet-v0.md`) exercise, in IR terms:

| IR construct | Where it appears here | `CONSTRUCTS.md` row |
|---|---|---|
| Characteristic attribution | "erkend jutter" as derived characteristic | 2, 6 |
| Fact type + role navigation | ouderschap: kind ↔ ouder | 4, 5 |
| Existential quantification | "ten minste één ouder die …" | 1, 5 |
| Defeasible default | art. 2 "zolang het tegendeel niet blijkt" | 7, 8 |
| The two negations | NAF vs aangetoond tegendeel | 6, 16 |
| Temporal qualification | art. 1 "op dat tijdstip" | 11 |

## 2. The translation, construct by construct

- **Model layer → predicates.** An object type's characteristic becomes a
  unary predicate over its instances; a fact type becomes a plain relation
  (`ouder(Kind, Ouder)`). Nothing of the v0 reference-field encoding
  (`zetVeld`/`joinVeld`/`backref`) has any counterpart: a role is a
  predicate argument position, navigation is a shared variable in a rule
  body.
- **Characteristic attribution → derivation rule.** One rule per
  acquisition ground; multiple grounds are simply multiple rules with the
  same head (art. 1 and art. 2 both conclude `erkend_jutter/1`).
- **Existential "ten minste één ouder" → body variable.** The existential
  is an unbound body variable — available at any nesting depth. The v0
  top-level-only restriction on far-side role membership has no ASP
  counterpart to translate *to*; there is nothing it could even mean here.
- **Positive recursion is native.** Art. 1 qualifies a parent through the
  parent's own (possibly derived) status — `kwalificeert(O) :-
  erkend_jutter(O)` — the classic British-Nationality-Act recursion. The
  minimal stable model is the forward-chaining fixpoint; no ordering
  mechanism is declared anywhere.
- **The art. 2 default → one NAF over a declared contrary.** The repaired
  reading (versiegeschiedenis, v1) translates as a single
  negation-as-failure over a positive contrary predicate:
  `not aangetoond_geen_ouder_kwalificeert(K)`. The contrary is an *input*
  predicate (closed-world at stratum 0), so the program stays stratified.
  The naive reading — a double NAF over derivability of art. 1 — is also
  expressible (`not not voldoet_aan_art_1(K)`) and reproduces the
  documented inversion: it demands positive derivability instead of
  yielding to a proven contrary (run 3 below).
- **Open vs closed predicates.** The corpus's predicate register (world
  facts open, organisational concepts closed) maps onto which predicates
  are inputs; `#defined` marks the inputs that may legitimately be absent.
  Full open-world treatment of world facts (unknown ≠ false) is *not*
  captured by this closed-world program — that is the row-18
  empty/unknown design question surfacing on the ASP side, noted, not
  solved here.

## 3. The program

`jutterswet.lp` — the rules:

```prolog
% Jutterswet, art. 1 + art. 2 (v1-lezing) — ASP-Core-2 / clingo.

% Art. 1 (verkrijging): erkend jutter is degene die na inwerkingtreding op
% Scholde geboren is uit ten minste een ouder die op dat tijdstip erkend
% jutter of vast ingezetene is.
erkend_jutter(K) :-
    geboren_op_scholde(K),
    na_inwerkingtreding(K),
    ouder(K, O),
    kwalificeert(O).

kwalificeert(O) :- erkend_jutter(O).
kwalificeert(O) :- vast_ingezetene(O).

% Art. 2 (strandvondeling), gerepareerde lezing: het vermoeden wijkt pas
% voor een aangetoond tegendeel (NAF over een contrair predicaat).
erkend_jutter(K) :-
    strandvondeling(K),
    not aangetoond_geen_ouder_kwalificeert(K).

#show erkend_jutter/1.

% Invoerpredicaten (mogen leeg zijn zonder waarschuwing).
#defined geboren_op_scholde/1. #defined na_inwerkingtreding/1.
#defined ouder/2. #defined vast_ingezetene/1.
#defined strandvondeling/1. #defined aangetoond_geen_ouder_kwalificeert/1.
```

`dossiers.lp` — dossier facts (Ade and Norre from
`corpus/jutterswet/dossiers.md`; Douwe is a synthetic second generation
added to exercise the recursion):

```prolog
% Ade: geboren op Scholde na inwerkingtreding; moeder vast ingezetene.
geboren_op_scholde(ade). na_inwerkingtreding(ade).
ouder(ade, tsjerkje). vast_ingezetene(tsjerkje).

% Norre: als pasgeborene aangetroffen op het strand; ouders onbekend.
strandvondeling(norre).

% Douwe: kind van Ade (positieve recursie: kwalificatie via een afgeleide
% status, niet via een vastgesteld feit).
geboren_op_scholde(douwe). na_inwerkingtreding(douwe).
ouder(douwe, ade).
```

`tegendeel.lp` — the proven-contrary variant. Deliberately *not* the
corpus dossier "Norre, twee jaar later": that dossier asks whether an
already acquired status is lost when the presumption falls afterwards;
this variant only puts the contrary on the table at evaluation time, so
the default never fires:

```prolog
% Tegendeel aangetoond op het evaluatiemoment: geen ouder kwalificeert.
aangetoond_geen_ouder_kwalificeert(norre).
```

`art2-naief.lp` — the designed pitfall, as a separate head so both
readings can run side by side:

```prolog
% De ontworpen valkuil: art. 2 als dubbele negation-as-failure.
% "zolang het tegendeel niet blijkt" naief vertaald: niet [niet voldoet-aan-art-1].
erkend_jutter_naief(K) :-
    strandvondeling(K),
    not not voldoet_aan_art_1(K).

voldoet_aan_art_1(K) :-
    geboren_op_scholde(K),
    na_inwerkingtreding(K),
    ouder(K, O),
    kwalificeert(O).

#show erkend_jutter_naief/1.
```

## 4. Runs and expected results

Executed with the clingo 5.8.2 binary (`clingo <files>`; any clingo ≥ 5
works). The program is stratified, so each run has exactly one stable
model (`Models: 1`). Answer sets verbatim; solver statistics and timing
lines trimmed — timings vary run to run and pin nothing.

**Run 1 — base:** `clingo jutterswet.lp dossiers.lp`

```
Answer: 1
erkend_jutter(norre) erkend_jutter(ade) erkend_jutter(douwe)
SATISFIABLE
```

Ade via art. 1 on established facts; Douwe via art. 1 through Ade's
*derived* status (the recursion); Norre via the art. 2 default.

**Run 2 — proven contrary:** `clingo jutterswet.lp dossiers.lp tegendeel.lp`

```
Answer: 1
erkend_jutter(ade) erkend_jutter(douwe)
SATISFIABLE
```

The default yields to the proven contrary: Norre's acquisition ground
falls away. (Whether an *already acquired* status is lost retroactively is
the deliberately open herziening question of the corpus — not asked here.)

**Run 3 — the naive reading alongside the repaired one:**
`clingo jutterswet.lp art2-naief.lp dossiers.lp`

```
Answer: 1
erkend_jutter(norre) erkend_jutter(ade) erkend_jutter(douwe)
SATISFIABLE
```

`erkend_jutter_naief(norre)` is **absent** while `erkend_jutter(norre)` is
present: the double NAF demands positive derivability of art. 1, which a
foundling with unknown parents cannot supply — the documented inversion of
the statute's intent, reproduced under stable-model semantics.

## 5. Findings (fed into `CONSTRUCTS.md`)

1. **A fact type is a predicate** (row 4, confirmation). Role navigation
   is variable sharing; the v0 reference-field vocabulary has no
   translation target. The symbolic `RolBinding` delta is not just
   cleaner — for this backend it is the only thing that *can* exist.
   *Landed (2026-08-16):* the binding rides the wire (`IR-SPEC.md`
   ch. 2.6, WF-047/048) and `backends/clingo` reads the `rol/3` join
   role from it, deriving only for bindingless v0 dumps.
2. **The top-level-only restriction is a Drools artefact** (row 5,
   confirmation). The existential lives in any rule body at any depth.
3. **The single-layer default is one NAF over a declared contrary**
   (rows 7/8). No priority or salience mechanism appears anywhere; the
   only ordering notion is stratification, which here holds because the
   contrary is an input. When contraries become *derived* (the art. 4
   ladder of wet-v2), layering will need the standard
   exception-predicate pattern and stratification becomes the load-bearing
   WF condition (row 6) — that is the natural next hand exercise.
4. **"Op dat tijdstip" did not translate** (row 11 — since decided). The
   parent must qualify *at the child's birth* ; this program collapses
   that to an atemporal `kwalificeert/1`, exactly as the boolean corpus
   dossiers do. The timeline machinery is required to say it honestly —
   the first corpus-driven demand on row 11, decided 2026-08-16:
   `read(tl, d)` becomes a core IR node (`RUNTIME-SEMANTICS.md` ch. 3.3);
   in ASP the node is a plain join against the segment relation.
   *Landed:* `backends/clingo` translates `TemporalRead` exactly so —
   `tl(Inst, A, From, To, V), From <= D, D < To` (and `param_tl/4` for
   the vintage read); a gap or an empty index is an unbound join, leeg
   as absence.
5. **Closed-world inputs vs open world facts** (row 18, boundary noted).
   `#defined` documents which inputs may be absent, but absence still
   means false; the corpus's "world facts are open" register needs the
   row-18 empty/unknown decision before this translation can respect it.

## 6. Hand exercise 2: the art. 4 exception ladder

Source: `corpus/jutterswet/wet-v2.md` — art. 3 (penningrecht), art. 4
(uitsluiting, gelaagd: "dit geldt niet indien …; dat laatste geldt weer
niet indien …"), art. 4bis (schorsing). The test case is the Marrit
dossier (`corpus/jutterswet/dossiers.md`) — the only dossier that walks
the whole ladder — plus four synthetic single-difference variants. In IR
terms this exercises:

| IR construct | Where it appears here | `CONSTRUCTS.md` row |
|---|---|---|
| Negation over a derived characteristic | "recht" negatively depends on the derived "uitgesloten" | 6 |
| Exception layering (exception-on-exception) | the art. 4 ladder itself | 8 |
| Stratification as a WF condition | the four-stratum negative dependency chain below | 6 |
| Parallel exclusion grounds | art. 4bis "evenmin" next to the art. 4 ladder | 8, 19 |
| Numeric comparison in a condition | "ten minste twaalf vondsten" | 1 |

### 6.1 The translation, construct by construct

- **The ladder is layered negation-as-failure over *derived* exception
  predicates** — the standard abnormality pattern. Each textual layer
  becomes one rule; each "geldt niet indien" becomes one `not` over the
  next layer's head:

  - layer 0: excluded on a refused raapvergunning, *unless* the layer-1
    exception holds;
  - layer 1: the exception holds when the refusal is herroepen, *unless*
    the layer-2 exception holds;
  - layer 2: the herroeping is vervallen (an input fact; deeper layers
    would continue the same pattern).

  Contrast exercise 1: there the single-layer art. 2 default negated an
  *input* predicate, and stratification was trivially given. Here the
  negated predicates are **derived**, so stratification does real work.
- **The negative dependency chain is stratified by construction**:
  `recht_op_penning` ← ¬`uitgesloten` ← ¬`uitzondering_art4` ←
  ¬`uitzondering_herroeping`, four strata, no cycle through negation.
  This is exactly the condition `CONSTRUCTS.md` row 6 promotes to a WF
  rule: the text's "geldt niet / geldt weer niet" nesting *is* the
  stratification order, read off the statute.
- **The compiled representation** flattens the ladder into classical
  cases over input facts (refused-and-not-herroepen, or
  refused-and-herroepen-and-vervallen). Over complete inputs the two are
  equivalent by construction: unfolding layer 1 into layer 0 gives
  `uitgesloten ⟺ geweigerd ∧ ¬(herroepen ∧ ¬vervallen)`, which
  distributes to `(geweigerd ∧ ¬herroepen) ∨ (geweigerd ∧ vervallen)` —
  logically equivalent to the two compiled cases (the compiled second
  case adds the `herroepen` conjunct, disjoint-normalising the same
  Boolean function; the extra region is absorbed by the first case).
  The equivalence is additionally *shown* under clingo by a difference
  test (run 3): any subject on which the two disagreed would surface as
  `verschil/1`.
- **Art. 4bis is a parallel ground, not a ladder rung.** "Evenmin"
  attaches a second, independent exclusion next to art. 4; it is
  translated as a separate `not geschorst_raapvergunning` in the art. 3
  rule, deliberately *not* as a case of `uitgesloten` — a suspension is
  not subject to the herroeping ladder.
- **"Ten minste twaalf vondsten"** is a plain arithmetic guard
  (`N >= 12`); the art. 5 hardheid variant (twaalf → zes) is out of
  scope here (ch. 7).

### 6.2 The programs

`ladder.lp` — the layered representation:

```prolog
% Jutterswet wet-v2, art. 3 + art. 4 (gelaagd) + art. 4bis — clingo.

% Art. 3 (penningrecht): recht op de jutterspenning heeft de erkende jutter
% van onbesproken juttersgedrag met ten minste twaalf aangemelde vondsten.
recht_op_penning(P) :-
    erkend_jutter(P),
    onbesproken_juttersgedrag(P),
    aantal_vondsten(P, N), N >= 12,
    not uitgesloten(P),
    not geschorst_raapvergunning(P).

% Art. 4 (uitsluiting, gelaagd) — de tekststructuur een-op-een:
% hoofdregel:            geen recht bij een geweigerde raapvergunning,
% uitzondering:          dit geldt niet indien de weigering is herroepen,
% uitzondering daarop:   dat laatste geldt weer niet indien de herroeping
%                        is vervallen.
uitgesloten(P) :-
    raapvergunning_geweigerd(P),
    not uitzondering_art4(P).

uitzondering_art4(P) :-
    weigering_herroepen(P),
    not uitzondering_herroeping(P).

uitzondering_herroeping(P) :-
    herroeping_vervallen(P).

#show recht_op_penning/1.
#show uitgesloten/1.

% Invoerpredicaten (mogen leeg zijn zonder waarschuwing).
#defined erkend_jutter/1. #defined onbesproken_juttersgedrag/1.
#defined aantal_vondsten/2. #defined raapvergunning_geweigerd/1.
#defined weigering_herroepen/1. #defined herroeping_vervallen/1.
#defined geschorst_raapvergunning/1.
```

`gecompileerd.lp` — the compiled representation plus the difference test:

```prolog
% De gecompileerde lezing van art. 4: de ladder platgerekend tot twee
% klassieke gevallen over invoerfeiten (weigering zonder herroeping, of
% weigering met vervallen herroeping).
uitgesloten_c(P) :-
    raapvergunning_geweigerd(P),
    not weigering_herroepen(P).
uitgesloten_c(P) :-
    raapvergunning_geweigerd(P),
    weigering_herroepen(P),
    herroeping_vervallen(P).

% Divergentietoets: een verschil tussen de gelaagde en de gecompileerde
% representatie zou hier zichtbaar worden.
verschil(P) :- uitgesloten(P), not uitgesloten_c(P).
verschil(P) :- uitgesloten_c(P), not uitgesloten(P).
#show verschil/1.
#defined uitgesloten/1. #defined uitgesloten_c/1.
```

`marrit.lp` — the corpus dossier:

```prolog
% Marrit (corpus/jutterswet/dossiers.md): erkend jutter; veertien vondsten;
% onbesproken juttersgedrag uitdrukkelijk vastgesteld; twee jaar geleden een
% raapvergunning geweigerd; vorig jaar de weigering herroepen; over verval
% van de herroeping is niets vastgesteld.
erkend_jutter(marrit).
onbesproken_juttersgedrag(marrit).
aantal_vondsten(marrit, 14).
raapvergunning_geweigerd(marrit).
weigering_herroepen(marrit).
```

`varianten.lp` — the ladder rung by rung:

```prolog
% De ladder laagsgewijs: vier synthetische varianten van hetzelfde dossier.
% w: alleen weigering.               wh: weigering, herroepen.
% whv: weigering, herroepen, vervallen.   s: geschorst (art. 4bis).
erkend_jutter(w).   onbesproken_juttersgedrag(w).   aantal_vondsten(w, 14).
raapvergunning_geweigerd(w).

erkend_jutter(wh).  onbesproken_juttersgedrag(wh).  aantal_vondsten(wh, 14).
raapvergunning_geweigerd(wh). weigering_herroepen(wh).

erkend_jutter(whv). onbesproken_juttersgedrag(whv). aantal_vondsten(whv, 14).
raapvergunning_geweigerd(whv). weigering_herroepen(whv).
herroeping_vervallen(whv).

erkend_jutter(s).   onbesproken_juttersgedrag(s).   aantal_vondsten(s, 14).
geschorst_raapvergunning(s).
```

### 6.3 Runs and expected results

Executed with the clingo 5.8.2 binary; stratified, so every run has
exactly one stable model. Answer sets verbatim, timing trimmed (ch. 4
convention).

**Run 1 — Marrit:** `clingo ladder.lp marrit.lp`

```
Answer: 1
recht_op_penning(marrit)
SATISFIABLE
```

Recht — the weigering would exclude, the herroeping lifts the exclusion,
and of verval nothing is established: the layer-1 exception stands. The
v0 statute (flat art. 4) would have excluded her; the layered v2 text
exists to decide this dossier correctly, and does.

**Run 2 — the ladder rung by rung:** `clingo ladder.lp varianten.lp`

```
Answer: 1
recht_op_penning(wh) uitgesloten(w) uitgesloten(whv)
SATISFIABLE
```

`w` (weigering alone) → uitgesloten, no recht; `wh` (herroepen) →
recht; `whv` (herroeping vervallen) → uitgesloten again, no recht; `s`
(geschorst, art. 4bis) → no recht *without* being `uitgesloten` — the
parallel ground blocks art. 3 directly and never touches the ladder.

**Run 3 — the difference test:**
`clingo ladder.lp gecompileerd.lp marrit.lp varianten.lp`

```
Answer: 1
recht_op_penning(wh) recht_op_penning(marrit) uitgesloten(w) uitgesloten(whv)
SATISFIABLE
```

`verschil/1` is absent over all five subjects: the layered and compiled
representations agree, as the ch. 6.1 unfolding argument says they must.

### 6.4 Findings (fed into `CONSTRUCTS.md`)

1. **Exception-on-exception is layered NAF over derived exception
   predicates** (rows 6/8). One rule per textual layer, one `not` per
   "geldt niet"; the pattern extends to any depth without touching
   earlier layers. The compiled form needs the full DNF recomputed at
   every amendment — the layered form is the amendable one, which is
   precisely why wet-v2 is written that way.
2. **Stratification is now load-bearing** (row 6). Exercise 1's only
   negation sat on an input predicate; here a four-stratum negative
   chain over *derived* predicates decides every outcome. The WF
   condition row 6 promises ("negation over derived characteristics
   requires stratification") is checkable on the IR as acyclicity of the
   negative dependency graph — the statute's own nesting supplies the
   strata.
3. **What the ladder demands of the Phase-4 default model** (row 8): a
   priority *chain* (rule < exception < exception-on-exception), not
   just pairwise defaults — Catala's exception trees express exactly
   this nesting, and the stable-model reading here gives the same
   answers. No conflict arises anywhere in the ladder (the layers are
   linearly ordered), so the §9.5 inconsistency channel is not
   exercised; over-determination needs *unordered* simultaneous
   exceptions, which this statute deliberately avoids.
4. **Parallel grounds compose by conjunction of NAFs** (rows 8/19).
   "Evenmin" (art. 4bis) is a second independent blocker, not a ladder
   rung: `not uitgesloten, not geschorst`. Keeping it out of
   `uitgesloten` preserves the ladder's meaning (a suspension cannot be
   "herroepen" away).
5. **The two representations diverge in structure, not in models** —
   and only the layered one mirrors the statute's amendment history. For
   the IR this is an argument to make exception layering first-class
   (row 8) rather than compiling it away in the frontend: the compiled
   form erases exactly the structure a later amendment needs.

## 6a. The empty-coalescing encoding (backends/clingo, 2026-08-18)

The ch. 1.2a whitelist — leeg reads as 0 on both sides of plus and min
and on the right of verminderd-met, while verminderd-met's left
propagates — lands in the systematic backend as two dual encodings. A
**propagating** position is the bare unbound join: an underivable
operand body leaves the whole rule underivable, which IS Empty
propagation. A **coalescing** position moves the operand's body atoms
into a helper:

```
coN(S,V)   :- is_a(S,"t"), <operand body binding V>.
coN_def(S) :- is_a(S,"t"), <operand body>.
coN(S,0)   :- is_a(S,"t"), not coN_def(S).
```

and the operator's rule joins `coN(S,V)` instead of the raw body — the
value when the operand derives, 0 by negation-as-failure when it does
not. An atom-free operand (a literal) is never empty and stays inline.
Sound on WF-045/046-clean documents: the negation reads exactly what
the positive operand read, so a cycle it sits on is a creation cycle
whose values are grounded in the minimal model — witnessed by the
recursive anchors (rank+1 on the wachtersketen chain) staying stable
under the encoding, and pinned absolutely by
`corpus/golden/anchor/jutterswet-leegrekening`.

## 6b. The multiplicative encoding (backends/clingo, 2026-08-19)

The integer-scaling convention (a value with d declared decimals is the
integer value × 10^d) extends to the multiplicative family without any
division for two of the three operators. **Maal** coalesces both
operands (ch. 1.2a) and multiplies the scaled integers: the product of
a scale-sl and a scale-sr term is the true product at scale sl·sr —
v0's static decimal count dl+dr, exactly. **Percentage-van** is
(l/100)·r with both sides coalescing; the /100 folds into the scale
(sl·sr·100), so no quotient is ever computed and no truncation can
diverge from ref's exact rationals. The write site fits a coarser scale
to the target's declared scale by exact upscaling; a finer scale
downscales under a runtime divisibility guard — ref writes any exact
rational that fits the target's declared decimals (1.5 × 2.0 = 3.0
lands on a 1-decimal target), and a value that does NOT fit derives a
viol/2 witness, never a silently truncated value (a recorded loud
asymmetry: ref would write the exact value).

Every viol/2 or inconsistent/2 head a value expression emits carries
the enclosing rule's condition atoms: ref evaluates the value only
under a true voorwaarde, so a subject the condition excludes must
never fault or deposit a channel fact — without the guard, a
conditional division would turn a satisfiable program UNSAT on the
excluded subjects (pinned by the vindersloon anchor's case4: a zero
divisor under a false condition stays consistent).

**Gedeeld-door** carries the §6.5 edges (arith.div_gen) beside its
value:

```
qN_ldef(S) :- is_a(S,"t"), <dividend body>.
qN_rdef(S) :- is_a(S,"t"), <divisor body>.
qN(S,0)    :- is_a(S,"t"), not qN_ldef(S).
inconsistent("<rule name>",S)
           :- is_a(S,"t"), qN_ldef(S), not qN_rdef(S).
viol("division by numeric zero",S) :- <full body>, (R) == 0.
```

An empty dividend collapses to 0 before the divisor is read (also under
a zero or empty divisor — ref's match order); an empty divisor under a
filled dividend deposits the §9.5 channel fact — keyed by the ENCLOSING
RULE'S NAME, exactly as ref keys its channel (harmonised 2026-08-20 so
the case format's rule pin reads both executors identically; the reason
text stays executor-internal) — and derives no value
(leeg as absence); a numeric zero divisor derives a `viol/2` witness —
UNSAT with the reason named, exactly where ref hard-faults (pinned by
`corpus/rood/division-by-numeric-zero`). The true quotient
(L/sl)/(R/sr) at out-scale m is L·(sr·m) / (R·sl), where m is
max(sl, sr) widened to the write target's scale — ref writes the exact
rational, so a quotient that fits the target's declared decimals
exactly (10/4 = 2.50 on a 2-decimal target) must translate; the bare
division derives its value only when the integer division at m is
EXACT, and a remainder derives a `viol/2` witness instead of a
truncated value. This is a recorded loud asymmetry: ref computes the
exact rational, so a corpus case whose division result fits no
representable scale *without an explicit rounding* cannot be admitted
(the differential would split). The **Afgerond-over-division**
form computes the mode's integer rounding of the true quotient in one
step — floor T/N, ceiling (T+N−1)/N, half-away-from-zero (2T+N)/(2N) —
exact for non-negative operands; the negative range is fenced by a
`viol/2` witness (richting-nul alone matches gringo's native truncation
on both signs and needs no fence). A standalone `Afgerond` over a
scaled term divides the scale surplus away with the same formulas.
Unit-carrying multiplicative operands are refused at translation time:
ref's unit algebra applies exact conversion factors (rate cancellation
under maal, convertible-unit division) that this encoding does not
carry. Anchored absolutely by
`corpus/golden/anchor/jutterswet-vindersloon` (all three operators,
every empty cell, the channel edge) and differentially by the emitter
golden `corpus/golden/jutterswet-bergloon`.

## 6c. The three fences of the first adversarial round (2026-08-19)

The first hacker–fixer round (PLAN §8) found no crack in the empty-value
algebra — every composition probed agreed between the executors — but
exposed three ways the integer encoding could diverge SILENTLY or crash
unfenced. Each is now a loud fence:

**The machine-integer fence.** clingo grounds arithmetic in machine
integers and wraps silently on overflow: 50000×50000 grounded negative,
and a comparison threshold upscaled by the scale harmoniser wrapped
negative and flipped a rule's condition — the one divergence class the
differential gate cannot see coming, witnessed live by the round. Every
multiplication emitted by the expression translation now carries a
`viol/2` range guard on its operands: product factors ≤ 46340 (note the
bound is on the SCALED integer, so a 2-decimal multiplication operand
is refused above 463.40 — conservative, and loud); additive, rounding
and division operands bounded with headroom for their formulas; the
clip's comparison harmonisation and its W×N segment products included
(the wu factor is a compile-time constant, bounded at emission). Input
values beyond 2^31−1 fault in `scaled_of_value`, expectation values in
`Model.scaled_expect` (both also fencing the formerly raw `Z.Overflow`
crash on 10^20), and scales beyond 10^9 fault in `pow10`. A comparison
guard deliberately omits the enclosing rule's condition: ref evaluates
a voorwaarde on every subject, so fencing it for every subject mirrors
that reach. The safe side is pinned by `corpus/golden/synth-grensrand`:
the largest translatable product (46340² = 2147395600) replays exactly,
so the guard provably does not fire early. The remainder recorded by
round 1 is closed (2026-08-19): the allocation recurrence bounds pot
and weights at 46340 with at most 46340 recipients (the weight sum,
the pot×weight product and the share sum then provably stay inside
2^31 — shares never exceed the pot under the non-negative-weight
guard), the uniqueness-union harmonisation fences each upscaled key at
max_int31 over its compile-time factor, and every `#sum` accumulation
is bounded — the Som aggregation, the weight sum and the share sum by
contributor ≤ 46340 plus contributor count ≤ 46340 (46340² < 2^31);
the clip total, whose contributors are W×N products, by the |W| ≤
46340 segment guard plus a compile-time bound of 46340 whole units on
the FULL clipped endpoint span (segments are disjoint slices of one
timeline, so ΣN never exceeds the span and |ΣW×N| ≤ 46340² holds).
`#count` aggregates carry no fence: a count cannot approach 2^31
before grounding itself becomes infeasible — recorded, not guarded.

**The unit fence.** ref restates operands across units with exact
conversion factors (1 m plus 50 cm = 150 cm); the integer encoding
carries no factor and grounded 1+50 = 51 — a silent split the round
caught in the additive join, the comparison, and the write boundary
(kg-target written from a t-typed expression). All positions where ref
would restate now refuse NON-IDENTICAL units; identical units (factor
1) and a unitless side (which adopts, value unchanged) translate,
pinned from the safe side by the identical-unit join and comparison in
`synth-grensrand`. The unit read is the EFFECTIVE unit — derived
recursively from attribute and parameter declarations with the ch. 2.6
join rules — never the node's own static annotation: a composite
annotated `eenheid:null` still carries its operands' units at runtime,
and the annotation-based first cut let a comparison of two plus
composites slip through (caught by the verification round of this very
fix). The companion verifier gap is closed: WF-035's additive-family
unit-compatibility clause skipped Verminderd_met entirely (negatief
`wf035-verminderd-met-incompatible-units` pins the rejection; v0's
resolver enforces the same, so the widening is mirror-founded).

**The name-quoting fence.** A declared name or case label containing a
double quote, backslash or control character passed the verifier and
broke the generated program's syntax — an unfenced parser crash.
Emission now faults with the name and reason. Whether the *spec* should
constrain declared names (a WF rule, which would also protect other
backends) is an open recommendation for the maintainer; until then this
is a recorded loud asymmetry (ref executes such names as data).

## 6d. The second adversarial round: the timeline machinery (2026-08-20)

Round 2 hunted the youngest code — the pro-rata fold, the timeline
checkers, the day-precise characteristic loader and the clip — and
found two real classes, each independently confirmed by two hunters:

**The derived-wall fix.** A gedurende clip over a DERIVED value
timeline (a pro-rata target) inherits the fold's interior bucket
walls, which the case's endpoint set does not carry: the clipped
segments found no `wu` fact, the `#sum` ranged over nothing, and the
total silently grounded 0 where ref computed the exact value. The
clip's cutset now enriches with the granularity walls of every derived
attribute the clip touches — the clipped source AND a
condition-comparison attribute alike (this fix's own verify round
caught the condition side reintroducing the zero) — compile-time, and
sufficient because a derived segment's bounds are bucket walls by
construction. Pinned by `corpus/golden/synth2-clip-over-prorata` (the
clipped side: fold Feb+Mar → clip totals 24) and
`corpus/golden/synth2-clip-cond-over-prorata` (the condition side: a
derived timeline under the comparison, total 20), both the rounds'
minimized witnesses admitted through the protocol.

**The computed-target input fence.** ref — like v0 — lets a computed
target OVERWRITE a case-supplied input value or timeline
(`write_attr`/`write_timeline` replace); a monotone ASP program cannot
retract, so the case fact and the rule head would union into a
superset model — a silent split both hunters reproduced symmetrically.
The translator now refuses loudly when the case supplies input for a
Berekening/Gelijkstelling target (scalar or temporal): overwrite
semantics have no monotone encoding without the defeasibility
machinery (Phase 4's default calculus is the principled home; ladder
targets already carry it via `inputval/3`). A recorded loud asymmetry:
ref executes such cases, this backend refuses them, so they cannot
enter the differential corpus.

Hunted and held (no finds): bucket-wall edges of the fold itself, base
gaps at bucket starts, out-of-range condition periods, multi-rule
sharing of condition facts, the overlapping/unsorted ctl loader
rejections, reverse-ordered timeline expectations, and the pro-rata in
operand/creation positions (loud faults on both sides).

*Postscript (2026-08-20, the Phase-4 adoption).* The computed-target
input fence retires for the RS ch. 7.8 **standing targets** — the case
value enters as `inputval/3`, standing by NAF against an effectiveness
atom that carries condition **and** value bindings (the defeasibility
machinery this section named as the principled home; a condition-only
atom would encode v0's erase instead — the atom's body is the semantic
choice). The fence remains on non-standing computing targets
(multi-writer, versioned) — the temporal arm followed with the
temporal-standing card (2026-08-21): a standing temporal target's
input timeline enters as `inputtl/5`, standing **whole** by NAF
against the fold's per-bucket effectiveness atom (`prN_eff` carries
the full bucket body incl. the exactness guard, so an empty fold
derives no eff and the input stands — ref's no-write on an empty
result, whole-timeline reading). The adoption round also found and
closed the neighbouring gap this section missed: an allocation
`doelAttribuut` supplied by the case was **unfenced** — the case
`val/3` fact and the allocation's own head unioned into a superset
model (observed: `[999;60]` on one attribute, ref replaying green over
it), silent wherever no expectation pinned the attribute. It now
refuses loudly, exactly like this fence.

## 7. Deliberately out of scope

Art. 5 (hardheid), art. 6, the v3 temporalisation, quantitative
machinery beyond the art. 3 threshold, and any systematic core-IR-to-ASP
mapping — all Phase-3 work. This document grows with each hand exercise
until that systematic mapping replaces it.
