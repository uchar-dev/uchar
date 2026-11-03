Violates WF-126 (`spec/IR-SPEC.md` ch. 3.1): the whole-period qualifier in the condition of a rule on a timeline target lies on a grid that does not fit the target's.

On a timeline target "gedurende de gehele maand" is a truth per month (RUNTIME-SEMANTICS ch. 3.9, per bucket); the rule writes `jaarpremie`, declared for every year, so a month in which the qualifier changes its truth would cut a year. The operand of the qualifier (`wachtplichtig`, for every day) is read by the qualifier per bucket and is no operand of this rule itself.
