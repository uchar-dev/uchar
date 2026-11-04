Violates WF-126 (`spec/IR-SPEC.md` ch. 3.1): a timeline that the justification of a default on a timeline target reads per day lies on a grid that does not fit the target's.

The characteristic `kustwacht` is declared for every day; the defaults group `wachtgeld voor kustwachters` writes `wachtgeld`, declared for every month, with the default 30 justified where he is kustwacht. A defaults group on a timeline target is read per day (RUNTIME-SEMANTICS ch. 7.11), so the periods of the justification cut the written timeline, and a cut on a day inside a month cannot be carried by a timeline for every month.

The same group with a justification that reads no timeline is well-formed: the justification then holds on every day or on none, and the group writes `wachtgeld` per day.
