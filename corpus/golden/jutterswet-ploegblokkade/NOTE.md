# jutterswet-ploegblokkade

The witness of the **narrowing host** (`spec/IR-SPEC.md` ch. 4.6 (q2), WF-109;
`spec/RUNTIME-SEMANTICS.md` ch. 5.8b rule 4 and worked example C; card
`subtype-characteristic-in-fold-filter`, 2026-10-27). A characteristic that
only a subtype declares is read on an object of its supertype: in the filter of
a fold over a role the supertype fills, and as the condition of a rule on the
supertype. The check names the subtype as its `host`; it holds for an instance
of the host that has the characteristic, and for no other object, and its
negation is the complement.

The source is authored, not patched: `source.rgs` goes through the frontend and
`ir.json` is its byte-identical emission (gate 10); `scenario.rgs` lowers to
the three case files (gate 16), whose expectations are written by hand below.

## The model

`Wrakrecht` ⊏ `Jutrecht` ⊏ `Strandrecht`. Only `Wrakrecht` declares the
possessive characteristic `vaartijd`, which the rule `vaartijd` derives where
the zeeweken of the Wrakrecht's logboek reach the parameter `vaartijddrempel`.
A Ploeg holds rechten over the role `recht` of `PloegBetreftStrandrecht`, whose
declared filler is `Strandrecht`, so a fold over "de rechten van de ploeg"
ranges over Strandrechten of every type.

## What it pins

| rule | form | position of the check |
|---|---|---|
| `vaartijd` | `Een Wrakrecht heeft de vaartijd indien de zeeweken van het logboek van het Wrakrecht groter of gelijk is aan de vaartijddrempel` | — (the writer: the check reads a derived characteristic) |
| `hoogste vaarloon` | the largest loon of the other rechten of the Jutrecht's ploeg that `hij heeft de vaartijd` | filter of a fold in a value, a member of an `all` composite |
| `hoogste rustloon` | idem with `hij heeft geen vaartijd` | idem, negated |
| `blokkade` | `zijn grens is gevuld` and (the fold of `hoogste vaarloon`, with `het Strandrecht heeft de vaartijd`) `is groter dan zijn grens` | filter of a fold in a condition, nested in a composite, written with the type name |
| `zeewaardig` | `Een Strandrecht is zeewaardig indien hij de vaartijd heeft` | a rule's condition on the supertype |
| `landvast` | `Een Strandrecht is landvast indien hij geen vaartijd heeft` | idem, negated |
| `ploegvarend` | `Een Jutrecht is ploegvarend indien ten minste één van de rechten van de ploeg van het Jutrecht de vaartijd heeft` | the test of a quantifier fold |

Every check of `vaartijd` outside its own rule is `CharacteristicCheck
{characteristic: vaartijd, host: Wrakrecht}` on the wire, at a position whose
one bound object is a `Strandrecht` (the member of the fold, or the rule's
subject).

## Hand-computed absolutes

Drempel 5 in every case. "—" is an object that is no Wrakrecht.

**Case 1**, ploeg P1 with J1 (Jutrecht, loon 5, grens 10), W1 (Wrakrecht, loon
20, logboek L1 with zeeweken 6), W2 (Wrakrecht, loon 30, grens 25, logboek L2
with zeeweken 2), S1 (Strandrecht, loon 40):

| object | vaartijd | hoogste vaarloon | hoogste rustloon | blokkade | zeewaardig | landvast | ploegvarend |
|---|---|---|---|---|---|---|---|
| J1 | — | **20** (W1) | **40** (W2, S1) | **yes** (20 > 10) | no | **yes** | yes |
| W1 | yes | **leeg** (no other member holds) | **40** (J1, W2, S1) | no (no grens) | **yes** | no | yes |
| W2 | no | **20** (W1) | **40** (J1, S1) | no (20 is not > 25) | no | yes | yes |
| S1 | — | | | | no | **yes** | |

The numbers separate the readings. Hoogste vaarloon of J1 is 20 and not 30 (W2
lacks the vaartijd) and not 40 (S1 is no Wrakrecht). Hoogste rustloon of J1 is
40: under a reading in which an object outside the host holds in neither
polarity (the navigated check's, `RUNTIME-SEMANTICS.md` ch. 5.8 rule 3), or one
that leaves it out of the fold, it is 30, of W1 30 and of W2 leeg. Landvast of
J1 and S1 is the same distinction on a rule's own condition. Blokkade of W2
would hold if S1's loon counted (40 > 25).

**Case 2**, ploeg P1 with J1 (Jutrecht, loon 5, grens 10) and S1 (Strandrecht,
loon 40): hoogste vaarloon of J1 **leeg**, hoogste rustloon **40**, no
blokkade (a leeg maximum is not greater than 10), J1 and S1 not zeewaardig and
both landvast, J1 not ploegvarend.

**Case 3**, ploeg P1 with J1 (Jutrecht, loon 5, grens 10) and W1 (Wrakrecht,
loon 20, no logboek): W1 has no vaartijd (the zeeweken of an absent logboek are
leeg, and a leeg operand makes the comparison onwaar), so hoogste vaarloon of
J1 and of W1 is **leeg**, hoogste rustloon of J1 **20** and of W1 **5**, no
blokkade, neither zeewaardig, both landvast, neither ploegvarend.

All three cases are consistent.

## Red before the card

Measured on uchar `af5fd045` with the files of this directory:

- the frontend wrote six `RGS-092` WARNINGs (`onopgelost: de geschreven naam
  bindt geen kenmerk en geen rol van het onderwerp (§8.1.7/§8.1.8), dus er is
  geen lidmaatschap om te toetsen`), one per check of `vaartijd` outside its own
  rule, and an `RGS-213` WARNING on the quantifier fold (`'de vaartijd' is geen
  kenmerk van Strandrecht`), and left each check as an `Unresolved` marker on
  the wire;
- with the hosts the frontend now writes, the verifier of `af5fd045` refused
  the document with `WF-109.only-some-objects` at each of the six checks; `ref`
  faulted (`characteristic check "vaartijd" is hosted on "Wrakrecht" while the
  subject is "Strandrecht"`, and in case 3 "while the subject is "Jutrecht"" —
  the concrete type of the object read, so the fault depended on the case),
  the ASP backend faulted at translation in the same sentence, and both Drools
  branches excluded it (`EXCLUDE DROOLS-240: … a CharacteristicCheck with a
  host that is not the subject ('hij heeft de vaartijd')`).

The Drools backend with the check lowered to a plain read of the standing — the
standing of `vaartijd` is kept on Wrakrechten only — played the cases with
hoogste rustloon 30, 30 and leeg in case 1 and landvast missing on J1 and S1:
an object outside the host left the rule instead of reading onwaar. The branch
therefore keeps an onwaar standing on every object outside the host
(`TmsTranspiler.outsideStateRule`).
