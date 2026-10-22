# instance-order-link-cycle

A cycle in the links of the switch relation of an instance-ordered cycle is
refused while the case is read, before any rule fires: EF-033, with the
fact type and the labels of the cycle (`RUNTIME-SEMANTICS.md` ch. 6.2,
ch. 5.7b).

## The shape

`ir.json` is the document of `corpus/golden/jutterswet-vergunningketen`,
verifier-clean: the einddag of a Vergunning reads the restant of its vorige
vergunning, and the restant reads the einddag. The forms evaluate per
object in the order of the chain `vorige vergunning` lays, and that order
exists only where the chain ends.

| case | links of `vorige vergunning` | `cycle` |
|---|---|---|
| `case1-twee-vergunningen` | V1 → V2, V2 → V1 | `V1, V2` |
| `case2-drie-vergunningen` | eerste → tweede, tweede → derde, derde → eerste | `derde, eerste, tweede` |
| `case3-eigen-voorganger` | V1 → V1 | `V1` |

The cycle is listed from its smallest label, labels compared byte by byte,
along the role the hop reaches (the predecessor's): in case 2 the vorige
vergunning of `derde` is `eerste`, whose vorige vergunning is `tweede`.

## Why a fault at load

Without the test the ASP backend grounds the program without bound: the
einddag of V1 and V2 each grow from the other's restant. Measured
2026-10-22 on case 1 before the test: clingo still grounding after 30 s,
2.2 GB resident, stopped by hand. The reference interpreter has no depth
to order the objects by. The links are input (no form writes the switch
relation, `IR-SPEC.md` ch. 5.3), so the cycle is a property of the case and
every executor refuses it before it evaluates: `ref` and the ASP backend in
the case reader they share, the Drools backend when it reads the case into
its session.

## Herkomst

Eigen werk (kaart `cycle-across-instances`, 2026-10-22).
