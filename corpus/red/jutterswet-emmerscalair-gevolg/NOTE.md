# jutterswet-emmerscalair-gevolg — the asOfDate-bucket scalar in a consequence that is evaluated

The document of `corpus/golden/jutterswet-gevolg-buiten-de-beurt`: the base
strandloon 5, above it "berekend worden als zijn kustloon indien hij kustwacht
is", and above that 60 "indien hij winterwacht is", on a timeline target that
the group decides per day (`spec/RUNTIME-SEMANTICS.md` ch. 7.11). The case
(asOfDate 2025-07-01) gives the kustloon of both Jutters as the asOfDate-bucket
scalar 40, which carries no period.

J1 is kustwacht from 2025-06-01 up to 2025-08-01 and never winterwacht, so the
kustwacht default is justified on days of its turn and its consequence is
evaluated: the period arithmetic reads the kustloon whole and finds the bucket
scalar (ch. 8.4). The session faults with EF-037, attribute `kustloon`, label
`J1`, in the reference interpreter, the ASP backend and both branches of the
Drools backend (`WV-7.11-gevolg-buiten-de-beurt-fout`). J2, kustwacht and
winterwacht in April 2025 only, gives the same scalar and raises nothing: there
the winter default decides, and the consequence is not evaluated.
