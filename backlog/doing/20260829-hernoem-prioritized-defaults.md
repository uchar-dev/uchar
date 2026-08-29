# Hernoeming: ladder → prioritized defaults (wire-epoch v0.10)

Goal: de volledige, in één beweging uitgevoerde hernoeming van het
gemunte "ladder/rung/sub-ladder"-vocabulaire naar prioritized
defaults / default / exception group, conform
HERNOEM_LADDER_20260829.md (geratificeerd 2026-08-29, V1–V8; archief
inbegrepen per V4-B): spec (IR-SPEC, RUNTIME-SEMANTICS, CONSTRUCTS,
ASP-TRANSLATION, PLAN integraal), schema + wire (vijf spellingen,
epoch v0.10, geen alias), verifier, ref, clingo, drools, beide
frontends incl. tests (V7), corpus (golden-regeneratie byte-identiek;
twaalf negatief-dirs via git mv; NOTE's incl. de NL-metafoor "sport"),
scripts, CLAUDE.md, de vier open kaarten, én het archief —
spec/proposals/ en backlog/done/ inhoudelijk plus de zes git mv's van
§2 — met de formerly-noot en de naamsverantwoordingsalinea (bijlage A)
als brug naar de git-geschiedenis.

Acceptance: alle twaalf gates groen met output; de §6-grep levert
exact de reason-carrying restlijst (formerly-noten + eventuele
vals-positieven; rood getoond bij aanvang); geen verwachtingswaarde
gewijzigd.

May touch: spec/ integraal (IR-REVIEW.md: nulmeting), corpus/golden,
corpus/negatief, corpus/jutterswet (NL-casusprose — valt onder de
§6-grep), ref/, backends/, frontends/, scripts/, CLAUDE.md,
backlog/new/*.md, backlog/done/**, spec/proposals/**, backlog/
(kaartverplaatsing), HERNOEM_LADDER_20260829.md.

Out of scope: toka/, corpus/rood (nulmeting), git-geschiedenis en
commitboodschappen, de bestandsnaam HERNOEM_LADDER_20260829.md,
regelspraak-java (volgt als pin-bump-kaart), calculemus-cursus.

Claim-notitie (2026-08-29, nulmeting op main = 8c6a992): de §6-grep
telt 2525 treffers — rood getoond vóór aanvang, conform fase A. Twee
afwijkingen van de §3-inventaris (gemeten op 55a7e1b, vóór de
typerfix-ronde): (1) de negatief-mv-lijst van fase E telt VEERTIEN
dirs, niet twaalf — `wf086-temporele-laddertarget` en
`wf086-temporele-ladderkenmerk` (typerfix-ronde) komen erbij als
`wf086-temporele-defaultstarget` / `wf086-temporele-defaultskenmerk`
(stamregel §1.2); (2) backlog/new/ is leeg — de "vier open kaarten"
van fase J zijn geland en vallen als done-kaarten onder de
V4-B-archiefbehandeling (V8's letter ingehaald, zoals bij ratificatie
voorzien). Extra binnen het contract: corpus/jutterswet/-prose (4
NL-treffers) valt onder de §6-grep en gaat mee.
