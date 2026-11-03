Violates WF-126 (`spec/IR-SPEC.md` ch. 3.1): a timeline that the condition of a rule on a timeline target reads per period lies on a grid that does not fit the target's.

The characteristic `wachtplichtig` is declared for every day; the rule writes `wachtloon`, declared for every month, gedurende de tijd dat hij wachtplichtig is. On a timeline target the condition is read per period (RUNTIME-SEMANTICS ch. 3.10), so its true periods cut the written timeline, and a cut on a day inside a month cannot be carried by a timeline for every month.
