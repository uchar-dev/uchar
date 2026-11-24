# jutterswet-zelfde-conclusie-fout

`ir.json` is `golden/jutterswet-zelfde-conclusie-fout` byte for byte: in a document with `Afleiden uit leeg: nee`, the
rekenloon "op 2 indien het Rekenjaar groter is dan 2025" with above it "op 2 indien hij betwist is". The case names
betwist in `characteristicsAbsent` and carries no evaluation date (`asOfDate: null`).

The deviation gives way, so the base is at its turn and is read as in the ordinary reading (RUNTIME-SEMANTICS ch. 1.5,
"An open default with a constant": a default is read provisionally only below a default that is open): its read of the
Rekenjaar faults with EF-011 (ch. 8.2), without parameters, and the session ends there. The golden of the same name holds
the two other states of betwist: open, where the base is read provisionally and its fault leaves the rekenloon open, and
holds, where the base is not read.
