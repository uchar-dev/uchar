# wf079-two-writers-one-cell

Violates WF-079 (`spec/IR-SPEC.md` ch. 3.1) for a cell: two unconditional
computing rules of different forms write one target, and the target of a
cell write is the cell. Here both rules of
`corpus/golden/jutterswet-seizoenkopie` write the cell `najaar` of
`vondstgewicht`. In the golden itself they write the cells `najaar` and
`zomer`, two targets, and WF-079 is silent there.

Red first: the verifier of main `b906f78ba` cannot load this document (`unknown field 'labels'` at the
creation's member; its rules write cells). `expect.txt` pins the arm
`WF-079.computing-same-target` with the cell as the attribute,
`vondstgewicht[najaar]`.
