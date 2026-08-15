# ASP translation

*Status: hand exercise (Phase-1 early start of the Phase-3 deliverable, per
`PLAN.md` §6: "start the hand translation of one representative rule during
Phase 1"). Scope: Jutterswet art. 1 (verkrijging) and the art. 2 default
(strandvondeling), in the v1 reading of
`corpus/jutterswet/versiegeschiedenis.md`. The systematic translation of
the core IR is the Phase-3 deliverable; this chapter records the first
contact between the IR's constructs and a semantically alien target, and
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

`tegendeel.lp` — the "Norre, twee jaar later" variant:

```prolog
% Norre, twee jaar later: de ouders zijn gevonden en kwalificeren aantoonbaar niet.
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
model (`Models: 1`). Answer sets verbatim; solver statistics trimmed.

**Run 1 — base:** `clingo jutterswet.lp dossiers.lp`

```
Answer: 1 (Time: 0.001s)
erkend_jutter(norre) erkend_jutter(ade) erkend_jutter(douwe)
SATISFIABLE
```

Ade via art. 1 on established facts; Douwe via art. 1 through Ade's
*derived* status (the recursion); Norre via the art. 2 default.

**Run 2 — proven contrary:** `clingo jutterswet.lp dossiers.lp tegendeel.lp`

```
Answer: 1 (Time: 0.001s)
erkend_jutter(ade) erkend_jutter(douwe)
SATISFIABLE
```

The default yields to the proven contrary: Norre's acquisition ground
falls away. (Whether an *already acquired* status is lost retroactively is
the deliberately open herziening question of the corpus — not asked here.)

**Run 3 — the naive reading alongside the repaired one:**
`clingo jutterswet.lp art2-naief.lp dossiers.lp`

```
Answer: 1 (Time: 0.001s)
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
2. **The top-level-only restriction is a Drools artefact** (row 5,
   confirmation). The existential lives in any rule body at any depth.
3. **The single-layer default is one NAF over a declared contrary**
   (rows 7/8). No priority or salience mechanism appears anywhere; the
   only ordering notion is stratification, which here holds because the
   contrary is an input. When contraries become *derived* (the art. 4
   ladder of wet-v2), layering will need the standard
   exception-predicate pattern and stratification becomes the load-bearing
   WF condition (row 6) — that is the natural next hand exercise.
4. **"Op dat tijdstip" did not translate** (row 11, recorded gap). The
   parent must qualify *at the child's birth* ; this program collapses
   that to an atemporal `kwalificeert/1`, exactly as the boolean corpus
   dossiers do. The timeline machinery is required to say it honestly —
   the first corpus-driven demand on row 11, recorded rather than silently
   dropped.
5. **Closed-world inputs vs open world facts** (row 18, boundary noted).
   `#defined` documents which inputs may be absent, but absence still
   means false; the corpus's "world facts are open" register needs the
   row-18 empty/unknown decision before this translation can respect it.

## 6. Deliberately out of scope

Art. 3–5 (the penning side, the exception ladder, hardheid), art. 6 and
the v3 temporalisation, quantitative machinery, and any systematic
core-IR-to-ASP mapping — all Phase-3 work. This chapter grows with each
hand exercise until that systematic mapping replaces it.
