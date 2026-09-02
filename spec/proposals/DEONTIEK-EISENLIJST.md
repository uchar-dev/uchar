# DEONTIEK-EISENLIJST — the normative-layer requirements, with provenance

*Status: standing requirements list (proposal), adopted as route R0 of
the FLINT/eFLINT exploration of 2026-09-02 — the survey of the TNO/UvA
Calculemus-FLINT programme and its executable offshoot (maintainer
decision 2026-09-02, numbered question; the exploration report lives
outside the repo, in the exploration folder next to it). Companion to
`LEGALRULEML-AUDIT.md`: where that list asks "can the IR express what
LegalRuleML can?", this one asks the same for the *normative layer* —
duties, powers, acts, violations — ahead of any decision to open
`CONSTRUCTS.md` row 32. Row 32 stays a v1 non-goal; nothing here is
normative. Every requirement carries its SOURCE, and every claim taken
from a source is cross-checked against the two independent frameworks
the maintainer designated (2026-09-02): Hohfeld's analysis and the
Wetsanalyse method's juridisch analyseschema (JAS). Where frameworks
disagree, the tension is recorded — never silently resolved.*

*Idea provenance, by maintainer instruction: this list is deliberately a
separate document. The findings below originate in a measured
exploration of the eFLINT/FLINT ecosystem (2026-09-02: interpreter and
translation code read and executed, not only papers) and must remain
attributable — no anonymous merging into existing spec prose. PLAN §3
carries a one-line pointer here; adoption of any requirement into the
spec happens only through the normal card regime, after the level-0
evidence of `backlog/new/20260902-meldplichtspoor-niveau0.md` exists.*

## 0. The frameworks — and how far each can be trusted

| Framework | What it is | Role here |
|---|---|---|
| **eFLINT (measured)** | Executable normative DSL (UvA line; the Haskell reference implementation, BSD-3-Clause). Findings below come from reading and running the implementation, not from its papers alone. | Source of operational lessons — with the trust caveat below. |
| **Hohfeld (1913/1917)** | The classical analysis: eight atomic positions in two squares (claim–duty, liberty–no-right; power–liability, immunity–disability), strictly two-*person* relations about one act, correlative pairs. | Conceptual cross-check: does an operational lesson respect or contradict the classification? |
| **Wetsanalyse / JAS (Ausems, Bulles & Lokin 2021)** | The analysis method developed by the Belastingdienst and PNA (with IND and academia in the Blauwe Kamer collaboration): a juridisch analyseschema of **sixteen** classes for dissecting legislation; its rechtsbetrekking subclasses are explicitly built on Hohfeld. Referenced in paraphrase only — the book is ©; no excerpts enter this repo. | Practice cross-check: what the method that analyses real Dutch legislation found necessary. |
| **LegalRuleML** | Already the standing test in PLAN §3 / `LEGALRULEML-AUDIT.md`. | Cited in E1 and E4, where its audit overlaps. |

**The eFLINT trust caveat, recorded up front.** The exploration also
measured the source's limits: the reference interpreter's derivation is
an unstratified fixpoint whose outcome can depend on type *names* (on
uchar's own anchor `jutterswet-uitsluiting` it answers 2 of 4 cases
wrongly, and a purely lexical rename of one domain name makes all four
pass); overlapping derivations can hold contradictory conclusions
simultaneously without any report; and the language has no normative
definition, no formal operational semantics and no type system for its
current version. The lessons below are therefore *design-vocabulary*
lessons — how a running system renders duty, act, power, violation —
never an appeal to eFLINT's authority on outcomes. That split is
exactly why each lesson is cross-checked against Hohfeld and the JAS.

**A structural observation** (our reading, not the book's own
structure — the book groups its classes thematically): the sixteen JAS
classes split naturally into a normative side (rechtssubject,
rechtsobject, rechtsbetrekking, rechtsfeit, delegatiebevoegdheid,
delegatie-invulling), a computational side (voorwaarde, afleidingsregel,
variabele, variabelewaarde, parameter, parameterwaarde, operator,
tijdsaanduiding, brondefinitie) and one class uchar deliberately does
not carry (plaatsaanduiding — jurisdiction, CONSTRUCTS row 13, a
non-goal). The computational side maps well onto the IR's existing
core; the normative side is largely row-32 territory — though not
entirely absent today: ObjectType, FactType and roles (IR-SPEC ch. 2.2,
2.6) already carry rechtssubject, rechtsobject and the *relational*
skeleton of a rechtsbetrekking. What uchar genuinely lacks is the
rechtsfeit (a state-changing act) and the deontic subtyping of the
relation — E4 details why that gap is real.

## 1. The four requirements

Verdict table (per requirement × framework):

| | Hohfeld | Wetsanalyse/JAS | LegalRuleML |
|---|---|---|---|
| E1 violation as derived observable | silent (static raster) | **refines** (enforceability variants) | confirms (Violation→Penalty→Reparation) |
| E2 power as derived view | **contradicts** (own position kind) | **contradicts** (two power classes) | silent here (audited separately) |
| E3 no declarative precedence in eFLINT | silent | silent in consulted chapters | has precedence itself (Override) |
| E4 two parties + content | confirms (two persons, one act) | confirms (rechtsobject a separate class) | confirms (Bearer/AuxiliaryParty) |

### E1 — Violation is a derived, observable outcome — never a modality, never fatal

**Source (eFLINT, measured):** since its v3 the interpreter *always
manifests* the effects of an act, permitted or not, and reports the
violation separately (the `Force` keyword — an override that used to
force effects through — fell into disuse as a consequence and survives
only as a reserved word). The violation is *derived and reported*, not
stored as a fact in the state — a distinction that matters for row 32:
the design question is precisely whether a schending is a deposited
fact or a computed view. eFLINT distinguishes three violation kinds:
duty violation, act violation (acting without power), and invariant
violation. Nearly every executable deontic neighbour surveyed
operationalises violation the same way — as a predicate over a state —
the two exceptions being the modal-logic line (NAI) and defeasible
deontic logic (SPINdle/DDL), which stay in the logic itself.

**Hohfeld:** silent — the squares classify static positions; violation
is dynamics, outside the framework. No tension.

**Wetsanalyse:** confirms and *refines*. The JAS subdivides
aanspraak–verplichting into three enforceability variants — in our
words: after non-performance the claimant can enforce directly, can
enforce only after an ingebrekestelling, or cannot enforce at all. The
variant thus encodes what a violation *opens up*. A violation outcome
is therefore not one thing: the derived predicate must carry (or the
duty declaration must determine) which enforcement route follows.

**LegalRuleML** (from the existing audit): the
Violation→Penalty→Reparation chain is the same shape — violation as a
node in a derivation, with consequences attached.

**Consequence for uchar:** violation belongs in the ordinary value
world — a derived domain fact, like the `Geschonden` constructor the
meldplichtspoor card models — NOT in the §9.5 inconsistency channel,
which stays reserved for assertion failure and overdetermination
(CONSTRUCTS rows 19/30). The channel is the right *shape analogy*
(observable, never fatal), not the right *home*; eFLINT's invariant
violation is the one kind that does map to §9.5. A future row-32 duty
node needs an enforceability discriminant, not just a violated bit.

### E2 — Power is act-enabledness, not a stored position kind — a three-against-one tension, recorded

**Source (eFLINT, measured):** the reference implementation derives
"power exists" from an act's enabledness (the act instance holds and
its preconditions are true); nothing stores a power. The Go
re-implementation independently derives rather than stores it too —
though the two *diverge* on exactly this question's edge (measured: an
act that holds but whose conditions fail yields no violation report in
the Go server), so the precise conjunct is the reference's reading, not
an ecosystem invariant.

**Hohfeld:** *contradicts at the classification level.* Power–liability
is a first-class position in the second square, not an abbreviation.

**Wetsanalyse:** contradicts likewise, twice over — bevoegdheid–
gehoudenheid is its own rechtsbetrekking subclass, and the JAS
additionally carries delegatiebevoegdheid (with delegatie-invulling) as
a separate top-level class pair. The method that analyses real Dutch
legislation found *two* power notions worth classifying.

**DPCL** (same UvA orbit, shared authorship with eFLINT — not an
independent line): also a counterexample, not support — its schema
reifies a power frame with explicit position values and has no
enabledness notion. Its design principle (conditional rules strictly
separated from Hohfeld frames) stands on its own, but on storage-vs-
derivation it sides with Hohfeld.

**Resolution recorded, not silently chosen:** classification and
storage answer different questions. A source language or analysis
raster classifies power as a position (Hohfeld, JAS, DPCL — useful to
juristen, and any frontend consuming analysis data must accept that
class); an executable core need not *store* it, because it is derivable
from the act's declaration and the current state. The local Hohfeld
exploration note (repo root, deliberately untracked) was *internally
inconsistent* on this: its design section already modelled a power as
an act with effects plus a scope condition, yet its node sketch still
stored `kind: power` — E2 resolves that inconsistency in the direction
the note itself argued. Falsification condition: if the meldplichtspoor
evidence (or any later measurement) shows a power that is *not*
derivable from an act declaration, this requirement falls.

### E3 — eFLINT has no declarative rule precedence; frontends must treat overlap as a loud question

**Source (eFLINT, measured):** the language has no *declarative*
rule-precedence mechanism — multiple derivation clauses for one fact
form a disjunction, its conditional construct is a conjunctive filter,
and the most defaults-like idiom (several derivation clauses over one
fact) keeps *both* values on overlap, without any report. What does
exist is fixed instance-level precedence (postulated valuations beat
derived ones; and the papers name an implicit ordering of prohibitions
over permissions, powers over prohibitions) — built-in, not
authorable. uchar's prioritizedDefaults (adopted from Catala's proven
calculus) is strictly stronger *on the defeasibility axis*: ordered
defeat plus conflict detection. (On other axes — state change,
three-valued openness — eFLINT expresses things uchar deliberately
does not; this claim is about precedence only.)

**Hohfeld:** silent (no theory of norm conflict).

**Wetsanalyse:** no precedence mechanism appears as a JAS class in the
consulted chapters; exception structures ("in afwijking van…") are
handled interpretively during analysis. Recorded as a consulted-scope
observation, not as a claim about the whole method.

**The peers are not uniform:** L4 has authorable precedence
(subject-to/despite constructs) and LegalRuleML has an explicit
Override relation — the finding is eFLINT-specific, which sharpens
rather than weakens the warning below.

**Consequence for uchar:** two-sided. Outward: prioritizedDefaults is a
genuine differentiator against this particular peer. Inward, the
warning (measured): any future frontend that maps a source language's
overlapping derivation rules onto prioritizedDefaults *changes
meaning* — keep-both becomes ordered-defeat-or-conflict. Such a
mapping must be a loud, per-case decision (refusal or explicit priority
annotation), never a silent default.

### E4 — Two parties and a content slot: the arity lesson

**Source (eFLINT, measured):** duties in real eFLINT models are
routinely ternary *or higher* — holder, claimant, and a related-to
component that is a *list* (measured over the corpora: 37 of 64 duty
declarations carry it, several with two or more entries) — while
uchar's fact-type contract (WF-047) resolves role bindings only
against exactly-two-role (or one, reciprocal) fact types.

**Hohfeld:** clarifies rather than contradicts. His atoms are strictly
two-*person* relations about one act; the extra components in practice
are the *content* (what the relation is about), not extra parties.

**Wetsanalyse:** confirms the same decomposition structurally — the
JAS separates rechtssubject (parties) from rechtsobject (what the
relation concerns) as distinct classes, and holds that a
rechtsbetrekking always involves a rechtsobject: two subjects, content
attached, never a third participant.

**LegalRuleML** (from the existing audit): its directed deontic
operators name exactly two parties (bearer and auxiliary party) —
independent support for the two-party reading.

**Consequence for uchar:** the row-32 design should model a position as
two party roles plus a content slot, NOT widen the WF-047 navigation
contract to n-ary fact types on this evidence alone. But honesty about
the cost, from the exploration's own correction of the local note: a
uchar FactType carries roles only, no attributes and no state — so
"two roles plus content" is *not expressible as one fact type* today; a
position becomes an object type with fact types beside it, which is an
argument that a dedicated position node is *more* necessary, not less.
And the content slot meets a second wall: the value world carries no
identity (no party references inside values), which is exactly the
boundary the meldplichtspoor card expects to hit. The remaining arity
pressure (the list-valued related-to; party diversity/subtyping) is
real and must be settled by measurement when row 32 opens.

## 2. Adjacent observations (outside the four, recorded for later cards)

- **Position-as-value is the exploration's most consequential find.**
  L4/jl4 (Apache-2.0, the nearest neighbour carrying both worlds as
  first-class constructs) renders a deontic position as a *value*
  folded over an event list — no state, no frame rule, no inertia
  problem; the residual obligation *is* the state. That form is likely
  expressible with uchar's landed kernel without one new IR node, and
  `backlog/new/20260902-meldplichtspoor-niveau0.md` (Openstaand /
  Vervuld / Geschonden as a Fold accumulator) is precisely its level-0
  test. It also refutes any "state/transition split is universal"
  claim: eFLINT and the JAS process view are state-machine shaped
  (positions as state, juridical facts as transitions), L4 is not —
  both operationalisations exist, and uchar's monotone, confluent
  fixpoint sits naturally with the second. Related correction from the
  exploration: a terminate-style effect is a *non-monotonic write*,
  which collides with uchar's monotone-characteristic and confluence
  discipline (RS ch. 5.2/5.3) — a real cost of the state-machine road
  that the fold road avoids.
- **Rechtsfeit typology as a trace checklist (row 37).** The JAS
  subdivides its rechtsfeit class into four subclasses (per the book,
  ch. 4, cited here as fixed vaktermen): rechtshandeling, feitelijke
  handeling met rechtsgevolg, gebeurtenis met rechtsgevolg, and
  tijdsverloop met rechtsgevolg. Lapse-of-time is a *sibling* fact
  kind, not a special case of event — any future trace/case-format
  design should be checked against all four, which matches the
  meldplichtspoor's termijn handling and warns against event-only
  trace formats.
- **Validation forms: the method uses two, and together they are our
  case format.** Wetsanalyse validates with *juridische scenario's* (a
  legal state plus the effect a sequence of juridical facts has on
  it — the trace shape) and, as a sister technique, with *concrete
  voorbeelden* that carry a predicted outcome checked for correctness
  (the pinned-expectation shape). uchar's case format with absolute
  expectations, and the meldplichtspoor card, combine exactly these
  two — practice arrived at the same validation forms the gates
  enforce, but the pinned-expectation half belongs to the second
  technique, not to the scenario notion itself. (The method draws its
  process view — positions as state, facts-with-consequences as
  transitions, subjects as actors — in standard process notation;
  that is presentation, not semantics.)
- **Open/closed per type as a third way for absence (rows 18/16).**
  eFLINT marks each type open or closed: absence of a closed fact is
  falsity, absence of an open fact is a *question to the environment*.
  Running in the Haskell reference (not carried by the ASP
  translation — relevant because uchar's differential is exactly an
  ASP leg); a measured alternative next to uchar's empty-value
  discipline and `Onopgelost` — worth a look whenever the
  input-default discussion reopens, not before.

## 3. The standing test

Alongside PLAN §3's LegalRuleML test, the normative-layer test reads:

> **When row 32 opens, can the IR express what the JAS classifies and
> what eFLINT executes — duty with an enforceability discriminant, act
> with effects, violation as a derived observable domain fact, power as
> a derived view — while keeping the computational half (the JAS's own
> voorwaarde/afleidingsregel/variabele/parameter classes) exactly where
> it already lives?**

Until the meldplichtspoor level-0 card delivers its measurement, this
list is the requirement side only; the evidence side is that card's
job.

## 4. Sources

- eFLINT: the Haskell reference implementation
  (gitlab.com/eflint/haskell-implementation, BSD-3-Clause), read and
  executed during the exploration of 2026-09-02; the Clingo
  translation lives in a fork namespace (tmuller99, license inherited
  BSD-3-Clause upstream; the production Rust variant is not public);
  language papers GPCE 2020, GPCE 2025, and the 2026 reflections
  article.
- W.N. Hohfeld, *Some Fundamental Legal Conceptions as Applied in
  Judicial Reasoning*, Yale Law Journal 1913 (and 1917).
- A. Ausems, J. Bulles & M. Lokin, *Wetsanalyse — voor een werkbare
  uitvoering van wetgeving met ICT*, 2021 (©; paraphrased only,
  chapters 3–4: the method, the sixteen JAS classes, the Hohfeld
  basis, the enforceability variants, the rechtsfeit subclasses, the
  two validation techniques).
- DPCL (UvA orbit, shared authorship with eFLINT): the design
  principle of separating conditional rules from Hohfeld frames; used
  here as a *counterexample* on E2 (it reifies power frames).
- L4/jl4 (smucclaw, Apache-2.0): the position-as-value
  operationalisation, §2.
- The local Hohfeld exploration note (repo root, deliberately
  untracked and excluded from the tree): the sketch this list corrects
  on E2 (its stored `kind: power` — resolved against its own design
  section) and refines on E1/E4. Because the note is not in the tree,
  every correction taken from it is restated self-containedly above.
