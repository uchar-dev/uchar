Violates WF-087 (`spec/IR-SPEC.md` ch. 2.4): the `keurstatus` sum type
declares `Gaaf` and `Beschadigd`, both with a null payload. A keuzetype
whose alternatives are all nullary has exactly one wire form — the v0
`Enumeratie` domain — so this declaration must be normalised to
`Enumeratie { domein: "keurstatus", waarden: ["Gaaf", "Beschadigd"] }`
(and the attribute use site with it). Nullair-als-sum is sugar; the
enumeration is the kernel.

A minimal hand-built document — before the rule, the verifier passed it
(`verifier: PASS`), so both frontends could have expressed one meaning
in two wire forms.
