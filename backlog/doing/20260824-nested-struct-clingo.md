# Geneste struct-veldtoegang in clingo

Goal: een genest struct-veld (o.binnen.bedrag — struct-in-struct,
veldtoegang op een veldtoegang) is verifier-clean, draait in ref en
door de Catala-keten, maar backends/clingo faalt luid ("struct
field-access base ... no corpus case for more" — de pre-existing
kaart-1-flank). Maak de struct-basis-vertaling recursief zodat
geneste veldprojectie grondt, met de bestaande schaaldiscipline
per einddatatype; een eigen-geschreven Jutterswet-golden met een
genest struct-veld door BEIDE executors met absolute verwachtingen;
de luide weigering verdwijnt uit de vertaling én uit de
S19-inventaris (S19 blijft de hoogste sectie — bij een nieuwe sectie
repo-breed hernummeren); ASP-TRANSLATION S16 uitgebreid.

Acceptance: rood-eerst (de huidige fault op de nieuwe golden
letterlijk getoond); alle negen gates groen; ref ≡ clingo over de
volle corpus; twee opus-lenzen verwerkt vóór de merge.

May touch: backends/clingo/, corpus/golden/ (de nieuwe golden),
spec/ASP-TRANSLATION.md, spec/CONSTRUCTS.md (UITSLUITEND rij 38),
scripts/check-noop.sh (alleen indien nodig), backlog/. Out of scope:
ref/, frontends/, spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md,
spec/verifier/.
