(* Typed AST of the uchar-ir-v1 wire format in its M1 shape
   (spec/schema/uchar-ir-v1.schema.json).

   Modelling rules:
   - every record field of a node carried over from v0 is always present on
     the wire; "optional" means the value may be null, mapped to [option] —
     never a missing key;
   - JSON maps and the model lists become ordered association lists
     (declaration order is normative, WF-019). A model list carries no map
     key, so the key here is the declaration's own name as its node writes
     it; the loader refuses an entry without one;
   - an object type keeps its members in the in-memory shape every OCaml
     reader of this library (this verifier, ref, the ASP backend and the two
     OCaml frontends) was written against: a name-keyed table per facet. The loader builds
     those tables from the M1 member nodes (Attribute, Characteristic,
     TimelineFacet), so a table key is always a member's own name and a
     facet always hangs on a member — the wire can no longer say otherwise;
   - the three unions (datatype / expr / conditie) each carry their own
     "Onbekend" variant — the discriminator value is reused across unions
     and must be dispatched per union, never globally;
   - enums that admit null on the wire are [t option]. *)

type granulariteit = Dag | Week | Maand | Kwartaal | Jaar
type teken = Onbepaald | Negatief | Niet_negatief | Positief
type kenmerk_soort = Bijvoeglijk | Bezittelijk | Zelfstandig
type afrond_modus = Naar_beneden | Naar_boven | Rekenkundig | Richting_nul | Weg_van_nul
type severity = Sev_error | Sev_warning | Sev_info

type bin_op =
  | Plus | Min | Verminderd_met | Maal
  | Gedeeld_door | Gedeeld_door_abs | Tot_de_macht | Percentage_van

type functie_kind =
  | Som | Totaal | Aantal | Minimum | Maximum | Eerste | Laatste
  | Aantal_gevulde
      (* the filled-value count (IR-SPEC ch. 4.3, RUNTIME-SEMANTICS
         ch. 3.4c): an instance fold that reads a VALUE per contributor
         and answers how many of those values are not leeg. A uchar
         dialect kind — the source language spells it nowhere. *)
  | Wortel | Absolute_waarde | Concatenatie
  | Tijdsduur | Absolute_tijdsduur | Eerste_paasdag
  | Tijdsevenredig_deel | Aantal_dagen_in
  | Jaar_uit | Maand_uit | Dag_uit | Uur_uit | Minuut_uit
  | Seconde_uit | Milliseconde_uit
  | Datum_met | Datumtijd_met

type vergelijk_op =
  | Kleiner | Kleiner_of_gelijk | Groter | Groter_of_gelijk
  | Gelijk | Ongelijk
  | Eerder | Eerder_of_gelijk | Later | Later_of_gelijk

type predicaat_soort = Leeg | Gevuld | Elfproef | Numeriek_met_exact | Dagsoort
type kwantor = De | Alle | Geen | Ten_minste | Ten_hoogste | Precies
type regel_soort = Gelijkstelling | Berekening | Initialisatie | Kenmerktoekenning
type methode = Gelijke_delen | Naar_rato | Op_volgorde | Complex

type eenheid = {
  afkorting : string option;
  stelsel : string option;
  canoniek : string option;
  factor_naar_canoniek : float option;
  noemer : eenheid option;
}

type datatype =
  | Numeriek of {
      geheel : bool;
      decimalen : int;
      teken : teken option;
      eenheid : eenheid option;
    }
  | Percentage of { geheel : bool; decimalen : int; eenheid : eenheid option }
  | Tekst
  | Booleaan
  | Datum_in_dagen
  | Datum_tijd
  | Enumeratie of { domein : string option; waarden : string option list }
  (* Extension node (v1, row 36): the payload-carrying variant family
     (IR-SPEC ch. 2.4). [domein] names the declaring domain; each
     constructor carries a name and either one payload datatype (unary) or
     None (nullary). Never emitter-produced. *)
  | Sum_type of { domein : string; constructoren : (string * datatype option) list }
  (* Extension node (v1, row 10): an ordered, possibly empty sequence type
     (IR-SPEC ch. 2.4). [element] is the single element datatype (first-order
     and non-list — WF-065). Never emitter-produced. *)
  | List_type of { element : datatype }
  (* Extension node (v1, Phase 5c): the record datatype
     (IR-SPEC ch. 2.4; design spec/proposals/PHASE5C-TUTORIAL.md §2).
     [domein] names the declaring domain; each field carries a name and a
     non-null datatype (WF-071). Never emitter-produced. *)
  | Struct_type of { domein : string; velden : (string * datatype) list }
  | Datatype_onbekend of { reden : string option }

(* The whole-period qualifier tag riding Vergelijking/Predicaat/Kenmerk. *)
type geheel_periode = { gp_jaar : bool; gp_niet : bool }

(* A symbolic role binding — the v1 extension of IR-SPEC ch. 2.6: one
   declared role of one declared fact type, named instead of derived.
   Extension fields carrying it are absent in every v0 emitter dump. *)
type rol_binding = { rb_feittype : string; rb_rol : string }

type expr =
  | Literal of { waarde : string option; typ : datatype option; bron : string option }
  | Tekst_reeks of { delen : tekst_deel list; typ : datatype option; bron : string option }
  | Binair of {
      op : bin_op;
      links : expr option;
      rechts : expr option;
      typ : datatype option;
      bron : string option;
    }
  | Functie of {
      kind : functie_kind;
      argumenten : expr list;
      element_objecttype : string option;
      leegwaarde : expr option;
      filter : conditie option;
      dimensie_selectie : dimensie_selectie option;
      typ : datatype option;
      bron : string option;
      granulariteit : granulariteit option;
      bron_rol : rol_binding option; (* extension: the fold-source role *)
      bron_pad : rol_binding list option;
          (* extension: the fold source's multi-step range (WF-102) — the
             list is carried AS LOADED, empty included, so the arity is a
             violation the rule reports and never a load failure *)
    }
  | Instance_set_union of {
      (* extension: the §5.7 concatenation of subject expressions, the
         source of an instance count (WF-110). It carries no datatype: it
         denotes instances and not a value. Like bron_pad above, the
         branches are loaded AS WRITTEN — a one-branch or field-less union
         is a violation the rule names, never a load failure. *)
      branches : instance_set_branch list;
      bron : string option;
    }
  | Afgerond of {
      basis : expr option;
      modus : afrond_modus;
      decimalen : int;
      typ : datatype option;
      bron : string option;
    }
  | Begrensd of {
      basis : expr option;
      minimum : expr option;
      maximum : expr option;
      typ : datatype option;
      bron : string option;
    }
  | Attribuut_ref of {
      attribuut : string option;
      objecttype : string option;
      onderwerp_bron : string option;
      navigatie_rol : string option;
      typ : datatype option;
      bron : string option;
      set : bool;
      navigatie_pad : string option list;
      navigatie_bindingen : rol_binding list option;
          (* extension: the navigation hops, subject-outward, one binding
             per hop — the bound role is the role the hop reaches *)
    }
  | Dimensie_cel of {
      attribuut : expr option;
      labels : string option list;
      typ : datatype option;
      bron : string option;
    }
  | Parameter_ref of { parameter : string option; typ : datatype option; bron : string option }
  | Variabele_ref of { naam : string option; typ : datatype option; bron : string option }
  | Constante of { naam : string option; typ : datatype option; bron : string option }
  | Expr_onopgelost of { typ : datatype option; bron : string option }
  (* Extension node (v1, row 11): the ch. 3.3 point-in-time read —
     read(tl, d). [basis] references the timeline (an own temporal
     attribute or a granular parameter); [datum] is a day-precision date
     expression. Never emitter-produced; strict, no nullable children. *)
  | Temporal_read of { basis : expr; datum : expr; typ : datatype option; bron : string option }
  (* Extension nodes (v1, row 36): the sum-type expression forms
     (IR-SPEC ch. 4.7; semantics RUNTIME-SEMANTICS ch. 9). [typ] is
     required on the wire (non-null, the TemporalRead pattern); [argument]
     and the arm [binder] are the only nullable children — an explicit
     null there is nullary. Never emitter-produced. *)
  | Construct of {
      typ : datatype option;
      constructor_ : string;
      argument : expr option;
      bron : string option;
    }
  | Match of {
      typ : datatype option;
      scrutinee : expr;
      arms : (string * string option * expr) list; (* constructor, binder, body *)
      bron : string option;
    }
  | Binder_ref of { naam : string; typ : datatype option; bron : string option }
  (* Extension nodes (v1, Phase 5c): the record expression forms
     (IR-SPEC ch. 4.9; semantics RUNTIME-SEMANTICS ch. 11). [typ] is required
     on the wire (non-null, the TemporalRead pattern). [Struct_literal]'s
     [velden] map field name to value in declaration order; [Field_access]
     reads one field [veld] of a struct-typed [base]. Never emitter-produced. *)
  | Struct_literal of { typ : datatype option; velden : (string * expr) list; bron : string option }
  | Field_access of { typ : datatype option; base : expr; veld : string; bron : string option }
  (* Extension node (v1, Phase 5c): the Let binding (IR-SPEC ch. 4.10;
     semantics RUNTIME-SEMANTICS ch. 10.6; design
     spec/proposals/PHASE5C-TUTORIAL.md §6, question 2 = A). [typ] is required
     on the wire (non-null, the TemporalRead pattern) and equals the [body]
     type (WF-076). [binder] is visible only in [body] — never in its own
     [value] (WF-076); it joins the WF-064 binder regime. Call-by-need
     (decision O3, 2026-09-01): the [value] is evaluated at the first READ of
     the binder and a read of a leeg [value] empties the whole Let there, so an
     unread binder evaluates nothing. Emitter-produced: by the Catala plugin
     since Phase-5c card 3, and by the RegelSpraak emitter since the
     let-lowering card (the variabelendeel). *)
  | Let of {
      typ : datatype option;
      binder : string;
      value : expr;
      body : expr;
      bron : string option;
    }
  (* Extension nodes (v1, row 10): the fold family (IR-SPEC ch. 4.8;
     semantics RUNTIME-SEMANTICS ch. 10). [typ] rides the TemporalRead
     pattern — present on the wire (non-null) for every node except [Lambda],
     which is never a value. A [Lambda] as an ordinary expr operand is
     ill-formed (WF-069); it is legal only as the [functie] of a Fold/MapList,
     where it is a [fold_fn], not this variant. Never emitter-produced. *)
  | List_literal of { typ : datatype option; elements : expr list; bron : string option }
  | Lambda of { parameters : (string * datatype) list; body : expr; bron : string option }
  | If_then_else of {
      typ : datatype option;
      condition : conditie;
      then_ : expr;
      else_ : expr;
      bron : string option;
    }
  | Invoke of { typ : datatype option; functie : string; arguments : expr list; bron : string option }
  | Fold of {
      typ : datatype option;
      list : expr;
      functie : fold_fn;
      init : expr;
      bron : string option;
    }
  | Map_list of { typ : datatype option; list : expr; functie : fold_fn; bron : string option }
  | Filter_list of {
      typ : datatype option;
      list : expr;
      binder : string;
      condition : conditie;
      bron : string option;
    }
  | List_length of { typ : datatype option; list : expr; bron : string option }

(* The [function] operand of a Fold/MapList: an inline lambda or a named
   FunctionDef (the two wire forms of IR-SPEC ch. 4.8). A lambda body is
   closed (WF-069); a name resolves against [functionDefs] (WF-070). *)
and fold_fn =
  | Fn_lambda of { parameters : (string * datatype) list; body : expr; bron : string option }
  | Fn_name of string

(* One selection of an InstanceSetUnion (WF-110): a population, the
   one-step relation it ranges over, and the die/dat subselection that
   narrows it PER CONTRIBUTOR. The two required fields are carried as
   option so a missing one is a citable violation, not a load failure. *)
and instance_set_branch = {
  isb_element_type : string option;
  isb_source_role : rol_binding option;
  isb_filter : conditie option;
  isb_bron : string option;
}

and tekst_deel =
  | Letterlijk of { tekst : string option }
  | Ingevoegd of { expr : expr option }

and dimensie_selectie = {
  ds_dimensie : string option;
  ds_labels : string option list;
  ds_bron : string option;
}

and conditie =
  | Vergelijking of {
      op : vergelijk_op;
      links : expr option;
      rechts : expr option;
      periode : geheel_periode option;
      bron : string option;
    }
  | Predicaat of {
      soort : predicaat_soort;
      operand : expr option;
      dagsoort : string option;
      negatie : bool;
      cijfers : int;
      periode : geheel_periode option;
      bron : string option;
    }
  | Kenmerk_check of {
      kenmerk : string option;
      navigatie : string option;
      negatie : bool;
      eigenaar : string option;
      periode : geheel_periode option;
      bron : string option;
      navigatie_binding : rol_binding option;
          (* extension: the one hop of a navigated check — the bound role
             is the role the hop reaches *)
    }
  | Rollidmaatschap of {
      rol : string option;
      ander_te_filler : string option;
      negatie : bool;
      bron : string option;
    }
  | Verre_rollidmaatschap of {
      filler : string option;
      backref : string option;
      negatie : bool;
      bron : string option;
    }
  | Periode_conditie of {
      vanaf : expr option;
      tot : expr option;
      tot_inclusief : bool;
      bron : string option;
    }
  | Samengesteld of {
      kwantor : kwantor;
      drempel : int;
      onderdelen : conditie list;
      bron : string option;
    }
  | Kwantorvouw of {
      kv_kwantor : kwantor;
      kv_drempel : int option;
          (* extension: the counting quantifier's threshold, Samengesteld's
             field. An OPTION and not an int, because on this node the field
             is present exactly where it is read (WF-108 (e)) rather than
             always present with a zero nobody consults — so its absence is a
             citable violation, and its VALUE is WF-041's, as on the sibling *)
      kv_element_objecttype : string option;
      kv_filter : conditie option;
      kv_toets : conditie option;
      kv_bron : string option;
      kv_bron_rol : rol_binding option; (* extension: the one-step range *)
      kv_bron_pad : rol_binding list option;
          (* extension: the multi-step range, carried AS LOADED (empty
             included) so the arity is a violation WF-108 reports and never
             a load failure — the WF-102 discipline, under this node's own
             rule *)
    }
  | Regelversie of { regel : string option; inconsistent : bool; bron : string option }
  | Conditie_onopgelost of { bron : string option }

(* --- model layer ------------------------------------------------------- *)

(* Built by the loader from an M1 ObjectType node. Since M1 the wire writes
   each member once, as a node carrying its own facets, so several values
   this record can hold are no longer produced: [ot_naam] is always [Some],
   every granularity and period count is [Some], and a facet table holds a
   key only for a member that carries that facet. *)
type objecttype = {
  ot_naam : string option;
  ot_supertype : string option;
      (* the single direct parent, by declared name; None = a root type.
         Single parenthood is true by SHAPE — one string, never a list —
         so WF-106 has nothing on the wire to check (IR-SPEC ch. 2.2). *)
  attributen : (string * datatype option) list;
  attribuut_meervoud : (string * string option) list;
  attribuut_dimensies : (string * string option list) list;
  attribuut_granulariteit : (string * granulariteit option) list;
  attribuut_tijdlijn_aantal : (string * int option) list;
  attribuut_tijdlijn_naam : (string * string option) list;
  kenmerken : string option list;
  kenmerk_soorten : (string * kenmerk_soort option) list;
  kenmerk_granulariteit : (string * granulariteit option) list;
  kenmerk_tijdlijn_aantal : (string * int option) list;
  bezield : bool;
  meervoud : string option;
}

type parameter = {
  par_naam : string option;
  par_datatype : datatype option;
  par_granulariteit : granulariteit option;
}

type eenheidstelsel = {
  es_naam : string option;
  eenheden : (string * eenheid option) list;
  aliassen : (string * eenheid option) list;
}

type rol = {
  rol_naam : string option;
  rol_objecttype_naam : string option;
  rol_meervoud : string option;
  rol_meervoudig : bool;
}

type feittype = {
  ft_naam : string option;
  wederkerig : bool;
  rollen : rol list;
}

type dimensie = {
  dim_naam : string option;
  dim_naam_meervoud : string option;
  voorzetsel : string option;
  dim_waarden : string option list;
}

type tijdlijn = {
  tl_naam : string option;
  variabel_startpunt : bool;
  tl_granulariteit : granulariteit option;
  tl_aantal : int;
}

(* --- rule layer -------------------------------------------------------- *)

type geldigheid = { geld_vanaf : string option; geld_tot : string option }

type resolved_regel = {
  rr_naam : string option;
  rr_soort : regel_soort option;
  doel : expr option;
  waarde : expr option;
  rr_voorwaarde : conditie option;
  rr_geldigheid : geldigheid option;
  rr_bron_annotaties : string list option;
      (* extension: `// Bron:` statute annotations, source order (WF-082) *)
}

type resolved_dagsoort = {
  rd_naam : string option;
  rd_voorwaarde : conditie option;
  rd_geldigheid : geldigheid option;
}

type toekenning = {
  toe_attribuut : string option;
  toe_waarde : expr option;
  toe_kenmerk : bool;
}

type resolved_object_creatie = {
  oc_naam : string option;
  nieuw_type : string option;
  oc_subject : string option;
  toekenningen : toekenning list;
  oc_voorwaarde : conditie option;
  oc_geldigheid : geldigheid option;
  oc_verbindende_rol : rol_binding option;
      (* extension: the created-side role of the back-link — required
         where the derivation is role-ambiguous (a self-typed pair) *)
}

type resolved_verdeling = {
  vd_naam : string option;
  bron_type : string option;
  bron_attribuut : string option;
  doel_type : string option;
  doel_attribuut : string option;
  vd_methode : methode option;
  rato_attribuut : string option;
  maximum_attribuut : string option;
  afronding_decimalen : int option;
  restant_attribuut : string option;
  groepeer_attribuut : string option;
  afnemend : bool;
  vd_voorwaarde : conditie option;
  vd_geldigheid : geldigheid option;
  verbindende_rol : rol_binding option; (* extension: the recipient role *)
}

type resolved_consistentie = {
  cons_naam : string option;
  cons_subject : string option;
  assertie : conditie option;
  cons_bron : string option;
  cons_line : int;
  cons_geldigheid : geldigheid option;
}

type resolved_uniciteit = {
  uni_naam : string option;
  uni_subject : string option;
  sleutels : string option list;
  scope_veld : string option;
  uni_bron : string option;
  uni_geldigheid : geldigheid option;
  scope_rol : rol_binding option;
      (* extension: the scope-parent role of a parent-scoped uniqueness,
         named symbolically — the bound role's filler is the scope parent *)
}

type uniciteit_unie_bron = {
  uub_type : string option;
  uub_sleutels : string option list;
}

type resolved_uniciteit_unie = {
  uu_naam : string option;
  bronnen : uniciteit_unie_bron list;
  uu_bron : string option;
  uu_geldigheid : geldigheid option;
}

type tussenstap = {
  ts_type : string option;
  veld : string option;
  via_ouder_veld : bool;
  ts_rol : rol_binding option; (* extension: the hop, named symbolically *)
}

type resolved_feit_creatie = {
  fc_naam : string option;
  anker : string option;
  fc_doel : string option;
  zet_veld : string option;
  join_veld : string option;
  tussenstappen : tussenstap list;
  fc_voorwaarde : conditie option;
  fc_geldigheid : geldigheid option;
  (* extension (all-or-nothing, WF-028): the created relation and the
     roles its two ends fill, plus the join hop reaching the target *)
  fc_feittype : string option;
  anker_rol : string option;
  doel_rol : string option;
  join_rol : rol_binding option;
}

type resolved_startpunt = {
  sp_tijdlijn : string option;
  sp_objecttype : string option;
  lid : string option;
}

type resolved_regel_groep = {
  rgr_naam : string option;
  recursief : bool;
  leden : string option list;
  rgr_bron : string option;
  rgr_line : int;
}

type diagnostic = {
  diag_severity : severity option;
  message : string option;
  diag_line : int;
}

(* --- the document ------------------------------------------------------ *)

(* Extension form (v1, Phase 4): the prioritized defaults of
   RUNTIME-SEMANTICS ch. 7 — defaults in descending priority. On a
   characteristic target the defaults carry a boolean [default_besluit]
   (attribute / do-not-attribute); on an attribute target they carry a
   value [default_consequentie] (a leeg-evaluating consequence falls
   through). The initialisation default ([default_initialisatie], lowest
   only) has no written justification: it is justified iff the target
   is empty in the input facts (ch. 7.3). Never emitter-produced. *)
type default = {
  default_besluit : bool option;
  default_voorwaarde : conditie option;
  default_consequentie : expr option;
  default_initialisatie : bool;
  default_excepties : exception_groups option;
      (* extension (v1, Phase 5c): the nested exception groups
         (RUNTIME-SEMANTICS ch. 7.9). A default carrying them is a group-default
         (no own consequence/besluit — its exception groups are its decision) or
         a form-a default (own consequence/besluit too — the exception groups
         decide first, the own consequence is the fall-back when they decide
         nothing). A differing-deciding exception group propagates the conflict
         to the root (WF-075). Absent = a flat default. *)
  default_bron : string option;
}

(* Extension records (v1, Phase 5c): a Default's [excepties] field and its
   members (RUNTIME-SEMANTICS ch. 7.9). [egs_ongeordend] carries the
   declared-unordered flag itself (the root regime, recursed); each
   [exception_group] is a default chain (descending, highest first)
   inheriting the root group's target — it carries no target of its own.
   Never emitter-produced. *)
and exception_groups = { egs_ongeordend : bool; egs_leden : exception_group list }
and exception_group = { eg_defaults : default list; eg_bron : string option }

type prioritized_defaults = {
  pd_naam : string option;
  pd_onderwerp : string; (* subject object type *)
  pd_kenmerk : string option; (* target characteristic — exactly one of the two *)
  pd_attribuut : string option; (* target attribute *)
  defaults : default list; (* descending priority: highest first *)
  pd_ongeordend : bool;
      (* declared-unordered (ch. 7.2): multiple defaults groups on one target
         are admissible iff every one carries this marking *)
  pd_bron : string option;
  pd_geldigheid : geldigheid option;
}

(* Extension form (v1, row 10): a named reusable function — DMN's Business
   Knowledge Model (IR-SPEC ch. 2.11). Typed, uniquely named parameters; a
   resolvable result; a closed body; an acyclic invocation graph (WF-067).
   Carried by the optional [function_defs] (the [functionDefs] list under
   [model] on the wire). Never emitter-produced. *)
type function_def = {
  fd_naam : string;
  fd_parameters : (string * datatype) list;
  fd_result : datatype;
  fd_body : expr;
  fd_bron : string option;
}

(* The M1 root nests [model] (the declaration set) and [rules]; this record
   keeps them flat, and [format] is checked by the loader and not kept. *)
type document = {
  objecttypen : (string * objecttype) list;
  domeinen : (string * datatype option) list;
      (* the Domain wrapper's name and datatype; always [Some] since M1. At
         this declaration position the wire writes no self-name, and the
         loader sets the datatype's own domain field to the wrapper's name *)
  parameters : (string * parameter) list;
  eenheidstelsels : (string * eenheidstelsel) list;
  feittypen : (string * feittype) list;
  dimensies : (string * dimensie) list;
  tijdlijnen : tijdlijn list;
  dagsoorten : string option list;
  dagsoort_per_eenheid : (string * string option) list;
  resolved_regels : resolved_regel list;
  resolved_dagsoorten : resolved_dagsoort list;
  object_creaties : resolved_object_creatie list;
  verdelingen : resolved_verdeling list;
  consistenties : resolved_consistentie list;
  uniciteiten : resolved_uniciteit list;
  uniciteit_unies : resolved_uniciteit_unie list;
  feit_creaties : resolved_feit_creatie list;
  resolved_startpunten : resolved_startpunt list;
  regel_groepen : resolved_regel_groep list;
  diagnostics : diagnostic list;
  prioritized_defaults : prioritized_defaults list; (* extension: absent = [] *)
  function_defs : function_def list; (* extension: absent = [] *)
}
