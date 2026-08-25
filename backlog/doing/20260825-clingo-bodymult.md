# Multiplicatieve gesloten-body-termen in clingo (2-4-muur D)

Goal: de gesloten-body-termvertaling (de S15-fence "a closed-body
term here is a bound binder, a literal, a parameter, a +/-
combination or a Construct") verbreedt met de multiplicatieve vormen
die tutorialpagina 2-4 vergt: Maal — inclusief geld×rate via de
bestaande gesplitste Mult_mon_rat-encodering — plus de
decimal-of-conversie en de constante-deler-deling (de
gcd-gereduceerde vorm), alles door HERGEBRUIK van de bestaande
regel-niveau-encoderingen in de body-term-context: één codering,
dezelfde schaaldiscipline, dezelfde range- en afrondingshekken (de
lens-les van de vorige golf: een gevallen hek is een stille
overloop). Rood-eerst op de huidige fence-tekst; eigen
corpus-getuige(n) met een multiplicatieve fold-/functie-body met
absolute verwachtingen door beide executors; ASP-TRANSLATION S15
bijgewerkt (S19 blijft de hoogste sectie; schrap wat gedicht is).

Acceptance: alle negen gates groen; rood-eerst per vorm; ref ≡
clingo over de volle corpus; twee opus-lenzen verwerkt vóór de
merge.

May touch: backends/clingo/, corpus/golden/ (nieuwe getuigen),
spec/ASP-TRANSLATION.md, scripts/check-noop.sh (alleen indien
nodig), backlog/. GEEN spec/CONSTRUCTS.md; de frontend-fence blijft
in deze kaart ONaangeraakt (kaart 2 versmalt hem). Out of scope:
ref/, frontends/, normatieve spec-documenten, spec/verifier/.
