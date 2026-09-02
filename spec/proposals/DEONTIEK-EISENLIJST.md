# DEONTIEK-EISENLIJST — the normative-layer requirements, with provenance

*Status: standing requirements list (proposal), adopted route R0 of the
calculemus/FLINT exploration (maintainer decision 2026-09-02, numbered
question; exploration report outside the repo). Companion to
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
spec happens only through the normal card regime, after the
meldplichtspoor level-0 evidence (`backlog/` card 20260902) exists.*

## 0. The frameworks

| Framework | What it is | Role here |
|---|---|---|
| **eFLINT (measured)** | Executable normative DSL (UvA line; BSD-3-Clause interpreter). Findings below come from reading and running the implementation, not from its papers alone. | Source of operational lessons: how a running system actually renders duty, power, act, violation. |
| **Hohfeld (1913/1917)** | The classical analysis: eight atomic positions in two squares (claim–duty, liberty–no-right; power–liability, immunity–disability), strictly two-party, correlative. | Conceptual cross-check: does an operational lesson respect or contradict the classification? |
| **Wetsanalyse / JAS (Ausems, Bulles & Lokin 2021)** | The Dutch government analysis method: a juridisch analyseschema whose classes cover both worlds — rechtssubject, rechtsobject, rechtsbetrekking, rechtsfeit on the normative side; voorwaarde, afleidingsregel, variabele, parameter, operator, tijdsaanduiding on the computational side. Its rechtsbetrekking subclasses are explicitly built on Hohfeld (via the Blauwe Kamer collaboration). Referenced in paraphrase only — the book is ©; no excerpts enter this repo. | Practice cross-check: what the method that analyses real Dutch legislation found necessary. |
| **LegalRuleML** | Already the standing test in PLAN §3 / `LEGALRULEML-AUDIT.md`. | Mentioned where it overlaps; not re-audited here. |

A structural observation before the requirements: the JAS class list is
itself the clearest available argument for uchar's position. Its
computational classes (voorwaarde, afleidingsregel, variabele,
parameter, operator, tijdsaanduiding) map near one-to-one onto the IR's
existing core; its normative classes (rechtssubject, rechtsobject,
rechtsbetrekking, rechtsfeit) are precisely the row-32 territory. One
analysis raster, two halves — and the half uchar carries today is the
half eFLINT lacks, and vice versa.

## 1. The four requirements

### E1 — Violation is a derived, observable predicate — never a modality, never fatal

**Source (eFLINT, measured):** since its v3 the interpreter executes a
non-permitted act *with full effect* and records the violation as a
separate observable fact (the earlier blocking mode was deliberately
removed); every executable deontic neighbour surveyed operationalises
violation the same way — a predicate over a state, no modal logic
anywhere.

**Hohfeld:** silent — the squares classify static positions; violation
is dynamics, outside the framework. No tension.

**Wetsanalyse:** confirms and *refines*. The JAS subclasses
aanspraak–verplichting into three enforceability variants (immediately
enforceable; enforceable after ingebrekestelling; weak/unenforceable),
distinguished exactly by the legal consequence of non-performance. So a
violation fact is not one thing: the derived predicate must carry (or
the duty declaration must determine) *which* enforcement route opens.

**Consequence for uchar:** the §9.5 channel discipline (observable,
never fatal) is the right shape and needs no change. A future row-32
duty node needs an enforceability discriminant, not just a violated
bit. The meldplichtspoor's Geschonden constructor is the level-0
version of this predicate.

### E2 — Power is act-enabledness, not a stored position kind — a recorded tension

**Source (eFLINT, measured):** two independent implementations
(Haskell reference and the Go server) derive "power exists" from an
act's enabledness (the act instance holds and its preconditions are
true); nothing stores a power. The DPCL design line states the same
separation as a principle.

**Hohfeld:** *contradicts at the classification level.* Power–liability
is a first-class position in the second square, not an abbreviation of
something else.

**Wetsanalyse:** sides with Hohfeld — bevoegdheid–gehoudenheid is its
own rechtsbetrekking subclass in the JAS.

**Resolution recorded, not silently chosen:** classification and
storage answer different questions. A source language or analysis
raster may classify power as a position (Hohfeld, JAS — useful to
juristen, and any frontend consuming analysis data must accept it); an
executable core need not *store* it, because it is derivable from the
act's declaration and the current state. The earlier in-repo Hohfeld
sketch (which proposed a stored `kind: power`) is corrected by this
requirement: the stored kinds a row-32 design needs are duty and act;
power is a view. If the meldplichtspoor evidence ever shows a power
that is *not* derivable from an act declaration, this requirement
falls — that is its falsification condition.

### E3 — The leading deontic peers have no rule precedence; frontends must treat overlap as a loud question

**Source (eFLINT, measured):** the language has no precedence
mechanism at all — multiple derivation clauses for one fact form a
disjunction, and its conditional construct is a conjunctive filter, not
an exception. The most ladder-like idiom (several Derived-from clauses)
keeps *both* values on overlap. uchar's prioritizedDefaults (adopted
from Catala's proven calculus) is strictly stronger: ordered defeat
plus conflict detection.

**Hohfeld:** silent (no theory of norm conflict).

**Wetsanalyse:** no precedence mechanism appears as a JAS class in the
consulted chapters; exception structures ("in afwijking van…") are
handled interpretively during analysis, not by a declared defeat
relation. Recorded as *consulted-scope* observation, not as a claim
about the whole method.

**Consequence for uchar:** two-sided. Outward: prioritizedDefaults is a
genuine differentiator, worth stating when the spec is compared to
deontic peers. Inward, the warning (measured): any future frontend that
maps a source language's overlapping derivation rules onto
prioritizedDefaults *changes meaning* — keep-both becomes
ordered-defeat-or-conflict. Such a mapping must be a loud, per-case
decision (refusal or explicit priority annotation), never a silent
default.

### E4 — Two parties and a content slot: the arity lesson

**Source (eFLINT, measured):** duties in real eFLINT models are
routinely ternary — holder, claimant, and a "related to" third
component — while uchar's fact-type contract (WF-047) resolves role
bindings only against exactly-two-role (or one, reciprocal) fact
types.

**Hohfeld:** clarifies rather than contradicts. His atoms are strictly
two-*person* relations about one act; the third component in practice
is the *content* (the act or object the relation is about), not a
third party.

**Wetsanalyse:** confirms the same decomposition structurally — the
JAS separates rechtssubject (parties) from rechtsobject (what the
relation concerns) as distinct classes; a rechtsbetrekking relates two
subjects, and its object is attached, not a third participant.

**Consequence for uchar:** the row-32 design should model a position as
two party roles plus a content slot (attribute-like), NOT widen the
WF-047 navigation contract to n-ary fact types on this evidence alone.
The arity pressure that remains real is subtyping/party-diversity, a
separate question to be carried by measurement when row 32 opens.

## 2. Adjacent observations (outside the four, recorded for later cards)

- **Rechtsfeit typology as a trace checklist (row 37).** Wetsanalyse
  distinguishes four kinds of juridical fact that change legal
  positions: rechtshandeling, feitelijke handeling met rechtsgevolg,
  gebeurtenis met rechtsgevolg, and *tijdsverloop met rechtsgevolg*.
  Any future trace/case-format design (the exploration's costliest
  post) should be checked against all four — note that lapse-of-time
  is a first-class fact kind there, which matches the meldplichtspoor's
  termijn handling and warns against event-only trace formats.
- **Juridische scenario's confirm the validation form.** The method
  validates an analysis by playing scenarios: a legal state plus a
  sequence of juridical facts, checked against expected outcomes. That
  is independently the shape of uchar's case format and of the
  meldplichtspoor card — practice arrived at the same validation form
  the gates enforce.
- **State/transition split is universal.** JAS: rechtsbetrekkingen are
  the state, rechtsfeiten the transitions (the method even derives BPMN
  process models from them); eFLINT: a transition system over fact
  sets. The in-repo Hohfeld sketch's state-machine operationalisation
  is thereby confirmed from both sides.
- **Open/closed per type as a third way for absence (row 18/16).**
  eFLINT marks each type open or closed: absence of a closed fact is
  falsity, absence of an open fact is a *question to the environment*.
  That is a measured, running alternative next to uchar's empty-value
  discipline and `Onopgelost` — worth a look whenever the input-default
  discussion reopens, not before.

## 3. The standing test

Alongside PLAN §3's LegalRuleML test, the normative-layer test reads:

> **When row 32 opens, can the IR express what the JAS classifies and
> what eFLINT executes — duty with an enforceability discriminant, act
> with effects, violation as a derived observable fact, power as a
> derived view — while keeping the computational half (the JAS's own
> voorwaarde/afleidingsregel/variabele/parameter classes) exactly where
> it already lives?**

Until the meldplichtspoor level-0 card delivers its measurement, this
list is the requirement side only; the evidence side is that card's
job.

## 4. Sources

- eFLINT: the Haskell reference implementation and its Clingo
  translation (both BSD-3-Clause; gitlab.com/eflint), read and executed
  during the exploration of 2026-09-02; language papers GPCE 2020,
  GPCE 2025, and the 2026 reflections article.
- W.N. Hohfeld, *Some Fundamental Legal Conceptions as Applied in
  Judicial Reasoning*, Yale Law Journal 1913 (and 1917).
- A. Ausems, J. Bulles & M. Lokin, *Wetsanalyse — voor een werkbare
  uitvoering van wetgeving met ICT*, 2021 (©; paraphrased only,
  chapters 3–4: the method, the JAS classes, the Hohfeld basis, the
  enforceability variants, the rechtsfeit typology, scenario
  validation).
- DPCL (design principle: conditional rules separated from Hohfeld
  frames), UvA line.
- The in-repo Hohfeld sketch (repo-root exploration note, 2026-08-22),
  which this list corrects on E2 and refines on E1/E4.
