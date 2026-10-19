(* Typed AST of the uchar-ir-v1 wire format in its L5 shape
   (spec/schema/uchar-ir-v1.schema.json).

   Modelling rules:
   - since L5 the wire spells every node, key and enumeration value with its
     IR-SPEC name, while the constructors and fields here keep their v0
     names: the loader reads the one onto the other, the enumeration values
     through the translation table [Spelling];
   - a record field of a node carried over from v0 is present on the wire
     where the wire requires it, and "optional" there means the value may
     be null, mapped to [option]. The exceptions are the fields the wire
     writes whole or not at all (the role bindings, the source record of
     every node, `Unresolved.type`, the optional fields of the nodes L3 and
     L4 rewrote — a number's unit, a unit's system, canonical unit, factor
     and denominator, a parameter's timeline, a day kind's plural; a
     version's condition, a rule version's presentation, an allocation's
     optional attribute fields and rounding, an aggregation's and a
     builtin's optional fields — and the fields L5 made optional: the
     bounds of a Validity, a TimeWindow and a Clamp, a characteristic
     check's host, every wholePeriod, a role's and a dimension's plural, a
     dimension's preposition, a default's decision and justification, a
     defaults group's characteristic, a predicate's dayKind, a quantifier
     fold's filter): absent is [None] there (a declared unit's absent
     spellings are []), and an explicit null is a load error;
   - since L4 the rule layer and the expression layer keep their in-memory
     constructors while the wire writes their L4 nodes, and the loader reads
     the one onto the other (IR-SPEC ch. 3 and 4.1–4.3): a named form with
     its versions (Rule, Allocation, ObjectCreation, FactCreation,
     Consistency, Uniqueness) becomes one record per version, named
     "<name> #<i>" (i from 1) when the form has two or more versions and
     bare when it has one, and each such record carries the wire pointer of
     its version (`/rules/<list>/<f>/versions/<v>`), the pointer every
     report about it cites; a rule version's kind is read from its
     `initialisation` flag and `presentation` tag; the two uniqueness
     shapes stay two lists, split by the number of sources; a fact
     creation's target type is the declared filler of its target role, and
     its path splits into the intermediate hops and the final one; an
     Aggregation and a BuiltinCall are both a [Function_call]; a Literal keeps its
     v0 lexeme beside the typed value it is read from ([literal_variant]);
     a ContextValue is a [Context_value], and the bound day of a day-kind
     definition has its own constructor, [Bound_day];
   - since L3 the datatype keeps its in-memory constructors while the wire
     writes two nodes for them: the loader reads a `Number` whose unit is
     '%' as [Percentage] and any other `Number` as [Numeriek], and a `Date`
     by its precision as [Date_in_days] or [Date_time_type], so every reader
     keeps the typing families it was written against. On every corpus
     document that is the v0 meaning exactly (no v0 [Numeriek] carried the
     unit '%'), but not at every derived position: the RegelSpraak frontend
     types a sum of two percentages as a number with the unit '%', and L3
     reads what it writes there as a [Percentage] — a refinement the
     verifier's derivation follows ([Checks.derived_number]; IR-SPEC ch. 2.4
     states it). What the in-memory form gained is what the wire now states
     and v0 could not: a unit's factor as an exact rational, a declared
     unit's spellings, a day kind's plural and a parameter's period count.
     The presentation tag of a percentage stays out of the datatype, which
     readers compare structurally between a declaration and its copies: the
     document lists instead where a tag was written inside a type annotation
     ([presentation_annotations]), the one position WF-007 refuses it;
   - since L2 a reference carries its symbolic form and nothing else: the
     five reference classes (AttributeReference, ParameterReference,
     DimensionCellRead, BinderRef, VariableReference) carry no type of their
     own — the declaration
     is the one source, [Checks.expr_type] — and a navigated read, a fold
     range, a creation step and a scope carry their role binding, never a
     written name to derive it from. The loader refuses every field that
     left the wire by name, and reads a required binding as required, so no
     reader here can fall back to a derivation;
   - the model lists become ordered association lists (declaration order
     is normative, WF-019). A model list carries no map key, so the key here
     is the declaration's own name as its node writes it; the loader refuses
     an entry without one. Since L3 the wire carries no JSON map at all: a
     unit system's units and a day kind's spellings are lists and fields;
   - an object type keeps its members in the in-memory shape every OCaml
     reader of this library (this verifier, ref, the ASP backend and the two
     OCaml frontends) was written against: a name-keyed table per facet. The loader builds
     those tables from the M1 member nodes (Attribute, Characteristic,
     TimelineFacet), so a table key is always a member's own name and a
     facet always hangs on a member — the wire can no longer say otherwise;
   - the datatype union carries no unknown variant since L2: an
     unresolvable domain is a declaration naming an undeclared domain
     (WF-003, WF-018), and an unknown expression type is an `Unresolved`
     without a `type`;
   - enums that admit null on the wire are [t option]. *)

type grain = Day | Week | Month | Quarter | Year
type sign_char = Unspecified | Negative | Non_negative | Positive
type characteristic_kind_type = Adjectival | Possessive | Nominal
type rounding_mode_kind = Floor | Ceiling | Half_away_from_zero | Toward_zero | Away_from_zero
type severity = Sev_error | Sev_warning | Sev_info

type bin_op =
  | Plus | Min | Reduced_by | Times
  | Divided_by | Divided_by_abs | Power | Percentage_of

type function_kind =
  | Sum_function | Total | Count | Minimum | Maximum | First | Last
  | Filled_count
      (* the filled-value count (IR-SPEC ch. 4.3, RUNTIME-SEMANTICS
         ch. 3.4c): an instance fold that reads a VALUE per contributor
         and answers how many of those values are not leeg. A uchar
         dialect kind — the source language spells it nowhere. *)
  | Square_root | Absolute_value | Concatenation
  | Duration | Absolute_duration | Easter_sunday
  | Time_proportional_part | Day_count
  | Year_of | Month_of | Day_of | Hour_of | Minute_of
  | Second_of | Millisecond_of
  | Date_with | Date_time_with

type compare_op =
  | Less | Less_or_equal | Greater | Greater_or_equal
  | Equal | Not_equal
  | Earlier | Earlier_or_equal | Later | Later_or_equal

type predicate_kind_type = Empty_predicate | Filled | Eleven_test | Exact_digits | Day_kind_predicate
type quantifier_kind = The | All | None_of | At_least | At_most | Exactly
type rule_presentation = Equation | Calculation | Initialisation | Characteristic_attribution
type method_name = Equal_shares | Proportional | Ordered

(* An exact rational, in the form the wire writes it: a unit's conversion
   factor is [{num; den}] with both at least 1 (the loader refuses any other
   value), and whether it is in lowest terms is WF-092's question, not the
   loader's. *)
type rational = { num : int; den : int }

type measure_unit = {
  abbr_text : string option;
  system : string option;
  canonical_form : string option;
  factor_to_canonical : rational option;
  denominator : measure_unit option;
}

type datatype =
  | Number of {
      whole_part : bool;
      decimal_places : int;
      sign_char : sign_char option;
      measure_unit : measure_unit option;
    }
  | Percentage of { whole_part : bool; decimal_places : int; measure_unit : measure_unit option }
  | Text_type
  | Boolean
  | Date_in_days
  | Date_time_type
  | Enumeration of { domain_spec : string option; label_list : string option list }
  (* Extension node (v1, row 36): the payload-carrying variant family
     (IR-SPEC ch. 2.4). [domain_spec] names the declaring domain; each
     constructor carries a name and either one payload datatype (unary) or
     None (nullary). Never emitter-produced. *)
  | Sum_type of { domain_spec : string; constructor_list : (string * datatype option) list }
  (* Extension node (v1, row 10): an ordered, possibly empty sequence type
     (IR-SPEC ch. 2.4). [element] is the single element datatype (first-order
     and non-list — WF-065). Never emitter-produced. *)
  | List_type of { element : datatype }
  (* Extension node (v1, Phase 5c): the record datatype
     (IR-SPEC ch. 2.4; design spec/proposals/PHASE5C-TUTORIAL.md §2).
     [domain_spec] names the declaring domain; each field carries a name and a
     non-null datatype (WF-071). Never emitter-produced. *)
  | Struct_type of { domain_spec : string; field_list : (string * datatype) list }

(* The whole-period qualifier, the wire's WholePeriod, riding a Comparison, a
   Predicate or a CharacteristicCheck: [gp_granularity] is its granularity (year
   true, month false), [gp_negated] its negation. *)
type entire_period = { gp_granularity : bool; gp_negated : bool }

(* A symbolic role binding — the v1 extension of IR-SPEC ch. 2.6: one
   declared role of one declared fact type, named instead of derived.
   Extension fields carrying it are absent in every v0 emitter dump. *)
type role_binding = { rb_fact_type : string; rb_role : string }

(* The typed value of a Literal (IR-SPEC ch. 4.1, delta B22): the value
   variants of uchar-case-v0, one constructor per variant key, without the
   list and struct variants (no literal carries one). A number is its exact
   decimal or rational string with a point, a constructor carries no
   payload. The string is loaded as written: its spelling is the business
   of WF-081, WF-084 and WF-095, and whether the variant is the one the
   literal's type names is WF-114's. *)
type literal_variant =
  | Lv_number of string
  | Lv_boolean of bool
  | Lv_text of string
  | Lv_date of string
  | Lv_date_time of string
  | Lv_enumeration of string
  | Lv_constructor of string

type expr =
  | Literal of { variant : literal_variant; typ : datatype option; source_name : string option }
      (* the variant is the one value a literal carries in memory: a reader
         that needs the v0 lexeme derives it ([Loader.literal_lexeme]) *)
  | Text_composition of { pieces : text_part list; typ : datatype option; source_name : string option }
  | Binary of {
      op : bin_op;
      left_side : expr option;
      right_side : expr option;
      typ : datatype option;
      source_name : string option;
    }
  | Function_call of {
      kind : function_kind;
      call_arguments : expr list;
      element_object_type : string option;
      empty_fallback : expr option;
      filter : condition_expr option;
      dimension_selection : dimension_selection option;
      typ : datatype option;
      source_name : string option;
      grain : grain option;
      source_role : role_binding option; (* the range: one step *)
      source_path : role_binding list option;
          (* the range: a multi-step path (WF-102) — the list is carried AS
             LOADED, empty included, so the arity is a violation the rule
             reports and never a load failure. The loader admits at most one
             of the two, and demands one on a single-argument fold with an
             elementType under any kind but total (B12): a range here
             makes the argument the set source, its absence makes it the
             subject's own read *)
    }
  | Instance_set_union of {
      (* extension: the §5.7 concatenation of subject expressions, the
         source of an instance count (WF-110). It carries no datatype: it
         denotes instances and not a value. Like source_path above, the
         branches are loaded AS WRITTEN — a one-branch or field-less union
         is a violation the rule names, never a load failure. *)
      branches : instance_set_branch list;
      source_name : string option;
    }
  | Rounding of {
      base_expr : expr option;
      mode_kind : rounding_mode_kind;
      decimal_places : int;
      typ : datatype option;
      source_name : string option;
    }
  | Clamp of {
      base_expr : expr option;
      minimum : expr option;
      maximum : expr option;
      typ : datatype option;
      source_name : string option;
    }
  | Attribute_reference of {
      attribute_ref : string option;
      object_type : string option;
      source_name : string option;
      navigation_bindings : role_binding list option;
          (* the navigation hops, subject-outward, one binding per hop — the
             bound role is the role the hop reaches. Absent: the subject's
             own read, whose [object_type] (the wire's subjectType) is the
             subject type (WF-053); never empty *)
    }
  | Dimension_cell_read of { attribute_ref : expr option; labels : string option list; source_name : string option }
  | Parameter_ref of { parameter : string option; source_name : string option }
  | Variable_reference of { ident : string option; source_name : string option }
  (* The wire's ContextValue (IR-SPEC ch. 4.1, delta B23): a session value,
     evaluationDate or evaluationYear, and nothing else — the bound day of a
     day-kind definition is [Bound_day]. [ident] is the wire name verbatim.
     [ident] and [typ] are required on the wire, so both are always [Some];
     which names are admissible is WF-098's. *)
  | Context_value of { ident : string option; typ : datatype option; source_name : string option }
  (* The wire's BoundDay (IR-SPEC ch. 3.7 and 4.1, delta B23): the day a
     day-kind definition is about. It carries no name; [typ] is required on
     the wire, so it is always [Some]. *)
  | Bound_day of { typ : datatype option; source_name : string option }
  | Expr_unresolved of { typ : datatype option; source_name : string option }
      (* [typ] absent = the type is unknown; present, a real datatype *)
  (* Extension node (v1, row 11): the ch. 3.3 point-in-time read —
     read(tl, d). [base_expr] references the timeline (an own temporal
     attribute or a granular parameter); [date_value] is a day-precision date
     expression. Never emitter-produced; strict, no nullable children. *)
  | Temporal_read of { base_expr : expr; date_value : expr; typ : datatype option; source_name : string option }
  (* Extension nodes (v1, row 36): the sum-type expression forms
     (IR-SPEC ch. 4.7; semantics RUNTIME-SEMANTICS ch. 9). [typ] is
     required on the wire (non-null, the TemporalRead pattern); [argument]
     and the arm [binder] are the only nullable children — an explicit
     null there is nullary. Never emitter-produced. *)
  | Construct of {
      typ : datatype option;
      constructor_ : string;
      argument : expr option;
      source_name : string option;
    }
  | Match of {
      typ : datatype option;
      scrutinee : expr;
      arms : (string * string option * expr) list; (* constructor, binder, body *)
      source_name : string option;
    }
  | Binder_ref of { ident : string; source_name : string option }
  (* Extension nodes (v1, Phase 5c): the record expression forms
     (IR-SPEC ch. 4.9; semantics RUNTIME-SEMANTICS ch. 11). [typ] is required
     on the wire (non-null, the TemporalRead pattern). [Struct_literal]'s
     [field_list] map field name to value in declaration order; [Field_access]
     reads one field [field_name] of a struct-typed [base]. Never emitter-produced. *)
  | Struct_literal of { typ : datatype option; field_list : (string * expr) list; source_name : string option }
  | Field_access of { typ : datatype option; base : expr; field_name : string; source_name : string option }
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
      source_name : string option;
    }
  (* Extension nodes (v1, row 10): the fold family (IR-SPEC ch. 4.8;
     semantics RUNTIME-SEMANTICS ch. 10). [typ] rides the TemporalRead
     pattern — present on the wire (non-null) for every node except [Lambda],
     which is never a value. A [Lambda] as an ordinary expr operand is
     ill-formed (WF-069); it is legal only as the [function_name] of a Fold/MapList,
     where it is a [fold_fn], not this variant. Never emitter-produced. *)
  | List_literal of { typ : datatype option; elements : expr list; source_name : string option }
  | Lambda of { parameters : (string * datatype) list; body : expr; source_name : string option }
  | If_then_else of {
      typ : datatype option;
      condition : condition_expr;
      then_ : expr;
      else_ : expr;
      source_name : string option;
    }
  | Invoke of { typ : datatype option; function_name : string; arguments : expr list; source_name : string option }
  | Fold of {
      typ : datatype option;
      list : expr;
      function_name : fold_fn;
      init : expr;
      source_name : string option;
    }
  | Map_list of { typ : datatype option; list : expr; function_name : fold_fn; source_name : string option }
  | Filter_list of {
      typ : datatype option;
      list : expr;
      binder : string;
      condition : condition_expr;
      source_name : string option;
    }
  | List_length of { typ : datatype option; list : expr; source_name : string option }

(* The [function] operand of a Fold/MapList: an inline lambda or a named
   FunctionDef (the two wire forms of IR-SPEC ch. 4.8). A lambda body is
   closed (WF-069); a name resolves against [functionDefs] (WF-070). *)
and fold_fn =
  | Fn_lambda of { parameters : (string * datatype) list; body : expr; source_name : string option }
  | Fn_name of string

(* One selection of an InstanceSetUnion (WF-110): a population, the
   one-step relation it ranges over, and the die/dat subselection that
   narrows it PER CONTRIBUTOR. The population is carried as option so a
   missing one is a citable violation, not a load failure; the relation is
   schema-required since L2 (B12), and nothing derives it. *)
and instance_set_branch = {
  isb_element_type : string option;
  isb_source_role : role_binding;
  isb_filter : condition_expr option;
  isb_source : string option;
}

and text_part =
  | Literal_chunk of { body_text : string option }
  | Embedded_expression of { expr : expr option }

and dimension_selection = {
  ds_dimension : string option;
  ds_labels : string option list;
  ds_source : string option;
}

and condition_expr =
  | Comparison of {
      op : compare_op;
      left_side : expr option;
      right_side : expr option;
      time_period : entire_period option;
      source_name : string option;
    }
  | Predicate of {
      variety : predicate_kind_type;
      operand : expr option;
      day_kind : string option;
      negation : bool;
      digit_chars : int;
      time_period : entire_period option;
      source_name : string option;
    }
  | Characteristic_check of {
      characteristic : string option;
      negation : bool;
      holder : string option;
      time_period : entire_period option;
      source_name : string option;
      navigation_binding : role_binding option;
          (* the one hop of a navigated check — the bound role is the role
             the hop reaches. Absent: the check reads the subject *)
    }
  (* The role check (IR-SPEC ch. 4.5, B9): true iff a link of the declared
     fact type [fact_type] exists in which the subject fills [role], named
     by its declared name, and whose other end is an instance of
     [counterpart_type] where that is written; [negated]: no such link
     exists. *)
  | Role_membership of {
      fact_type : string;
      role : string;
      counterpart_type : string option;
          (* the object type the filler of the OTHER role of [fact_type]
             must belong to, ⊑ that role's declared filler; absent = the
             declared filler itself *)
      negated : bool;
      source_name : string option;
    }
  | Time_window of {
      starting_at : expr option;
      up_to : expr option;
      to_inclusive : bool;
      source_name : string option;
    }
  | Compound of {
      quantifier_kind : quantifier_kind;
      cutoff : int;
      components : condition_expr list;
      source_name : string option;
    }
  | Quantifier_fold of {
      kv_quantifier : quantifier_kind;
      kv_threshold : int option;
          (* extension: the counting quantifier's threshold, Samengesteld's
             field. An OPTION and not an int, because on this node the field
             is present exactly where it is read (WF-108 (e)) rather than
             written under every quantifier and read only under the three
             counting ones, as on the sibling — so its absence is a citable
             violation, and its VALUE is WF-041's, as on the sibling *)
      kv_element_object_type : string option;
      kv_filter : condition_expr option;
      kv_test : condition_expr option;
      kv_source : string option;
      kv_source_role : role_binding option; (* the one-step range *)
      kv_source_path : role_binding list option;
          (* the multi-step range, carried AS LOADED (empty included) so the
             arity is a violation WF-108 reports and never a load failure —
             the WF-102 discipline, under this node's own rule. The loader
             demands exactly one of the two (B12): the range is written,
             never derived *)
    }
  | Rule_status_check of { rule_def : string option; inconsistent : bool; source_name : string option }
  | Condition_unresolved of { source_name : string option }

(* --- model layer ------------------------------------------------------- *)

(* Built by the loader from an M1 ObjectType node. Since M1 the wire writes
   each member once, as a node carrying its own facets, so several values
   this record can hold are no longer produced: [ot_ident] is always [Some],
   every granularity and period count is [Some], and a facet table holds a
   key only for a member that carries that facet. *)
(* The period of a member's timeline facet beyond its unit: the number of
   units it spans ([tv_count], the facet's count) and the point the periods
   run from when the facet fixes one ([tv_fixed_start_point], its fixedStart,
   IR-SPEC ch. 2.8). *)
type period = { tv_count : int option; tv_fixed_start_point : string option }

type object_type = {
  ot_ident : string option;
  ot_supertype : string option;
      (* the single direct parent, by declared name; None = a root type.
         Single parenthood is true by SHAPE — one string, never a list —
         so WF-106 has nothing on the wire to check (IR-SPEC ch. 2.2). *)
  attributes : (string * datatype option) list;
  attribute_plural : (string * string option) list;
  attribute_dimensions : (string * string option list) list;
  attribute_granularity : (string * grain option) list;
  attribute_timeline_count : (string * period) list;
      (* named for the v0 side-map it mirrors, the count; an entry carries the
         facet's fixed start point beside it because the Catala and F-logic
         frontends build this record whole, so a field of its own would reach
         into both *)
  attribute_timeline_names : (string * string option) list;
  characteristics : string option list;
  characteristic_kinds : (string * characteristic_kind_type option) list;
  characteristic_granularity : (string * grain option) list;
  characteristic_timeline_count : (string * period) list; (* as [attribute_timeline_count] *)
  animate : bool;
  plural : string option;
}

(* Built by the loader from a Parameter node: its timeline facet is split
   into the fields an attribute's facet tables hold — granularity and count
   both [Some] iff the facet is present, the fixed start point [Some] iff the
   facet carries one. [par_datatype] is always [Some]: the wire requires it. *)
type parameter = {
  par_name : string option;
  par_datatype : datatype option;
  par_granularity : grain option;
  par_timeline_count : int option;
  par_fixed_start_point : string option;
}

(* A unit as its unit system declares it: the unit itself and the further
   spellings that resolve to it. Only the declaration carries spellings; an
   occurrence of the unit elsewhere is a bare [measure_unit]. *)
type declared_unit = { ge_unit : measure_unit; ge_spellings : string list }

type unit_system = {
  es_name : string option;
  unit_list : declared_unit list; (* declaration order *)
}

(* A day kind: its name and its plural, the two spellings it has on the wire
   (IR-SPEC ch. 2.9). Both name it as a duration unit. *)
type day_kind = { dk_name : string; dk_plural : string option }

type role_ref = {
  role_identifier : string option;
  role_object_type_name : string option;
  role_plural : string option;
  role_many : bool;
}

type fact_type_name = {
  ft_name : string option;
  reciprocal : bool;
  role_names : role_ref list;
}

type dimension = {
  dim_name : string option;
  dim_name_plural : string option;
  preposition : string option;
  dim_values : string option list;
}

type timeline_state = {
  tl_name : string option;
  variable_start : bool;
  tl_granularity : grain option;
  tl_count : int;
  tl_fixed_start_point : string option; (* fixedStart, IR-SPEC ch. 2.8; None = absent *)
}

(* --- rule layer -------------------------------------------------------- *)

type validity_window = { valid_from : string option; valid_to : string option }

(* The records of the six named forms (IR-SPEC ch. 3, Versioning) are one
   per VERSION, as v0 flattened them (Ast header): [*_naam] is the form's
   name with the suffix " #<i>" when the form has two or more versions, and
   [*_pointer] is the wire pointer of the version itself,
   `/rules/<list>/<f>/versions/<v>`, set by the loader. Every field the
   wire holds on the version is always present there, so a [geldigheid
   option] or a kind is always [Some] after a load; the options are the v0
   types every reader was written against. *)
type resolved_rule = {
  rr_name : string option;
  rr_presentation : rule_presentation option;
      (* read from the version's `initialisation` and `presentation`: a
         presentation tag is Gelijkstelling or Berekening,
         `initialisation: true` is Initialisatie, and neither is
         Kenmerktoekenning *)
  objective : expr option; (* always an [Attribute_reference] *)
  value_node : expr option;
  rr_condition : condition_expr option;
  rr_validity : validity_window option;
  rr_source_annotations : string list option;
      (* extension: `// Bron:` statute annotations, source order (WF-082).
         The wire writes them once, on the form (`statuteAnnotations`), and
         every version's record carries that one list *)
  rr_pointer : string;
}

type resolved_day_kind = {
  rd_name : string option;
  rd_condition : condition_expr option;
  rd_validity : validity_window option;
}

type assignment = {
  to_attribute : string option;
  to_value : expr option;
  to_characteristic : bool;
}

type resolved_object_creation = {
  oc_name : string option;
  created_type : string option;
  oc_subject : string option;
  assignments : assignment list;
  oc_condition : condition_expr option;
  oc_validity : validity_window option;
  oc_connecting_role : role_binding option;
      (* extension: the created-side role of the back-link — required
         where the derivation is role-ambiguous (a self-typed pair) *)
  oc_pointer : string;
}

type resolved_allocation = {
  vd_name : string option;
  source_type : string option;
  source_attribute : string option;
  objective_type_name : string option;
  target_attribute : string option;
  vd_method : method_name option;
  ratio_attribute : string option;
  maximum_attribute : string option;
  rounding_decimals : int option;
  remainder_attribute : string option;
  ordering_attribute : string option;
  descending : bool;
  vd_condition : condition_expr option;
  vd_validity : validity_window option;
  connecting_role : role_binding; (* the recipient role, required (B12) *)
  vd_pointer : string;
}

type resolved_consistency = {
  cons_name : string option;
  cons_subject : string option;
  assertion : condition_expr option;
  cons_source : string option;
  cons_line : int;
  cons_validity : validity_window option;
  cons_pointer : string;
}

(* A uniqueness version with ONE source (IR-SPEC ch. 3.6): the source's
   object type and keys. A version with two or more is a
   [resolved_uniqueness_union]; the wire writes both shapes in one list
   (delta B27), and the loader splits them by the number of sources. *)
type resolved_uniqueness = {
  uni_name : string option;
  uni_subject : string option;
  key_list : string option list;
  uni_source : string option;
  uni_validity : validity_window option;
  scope_role : role_binding option;
      (* the scope-parent role of a parent-scoped uniqueness — the bound
         role's filler is the scope parent. Its presence IS the scoping
         (B8): absent, the uniqueness is unscoped *)
  uni_pointer : string;
}

type uniqueness_union_source = {
  uub_type : string option;
  uub_keys : string option list;
}

type resolved_uniqueness_union = {
  uu_name : string option;
  source_list : uniqueness_union_source list; (* two or more; a union carries no scope *)
  uu_source : string option;
  uu_validity : validity_window option;
  uu_pointer : string;
}

(* One intermediate hop of a fact creation's join chain: its binding and
   nothing else (B7). Since L4 the wire writes the chain as one [path]; the
   loader makes every hop but the last a [path_step] and the last the
   [join_role]. *)
type path_step = { ts_role : role_binding }

type resolved_fact_creation = {
  fc_name : string option;
  anchor_point : string option;
  fc_target : string option;
      (* not on the wire since L4 (IR-SPEC ch. 3.4): the declared filler of
         [objective_role] in [fc_fact_type], resolved by the loader with the
         verifier's name identity; [None] where that does not resolve (an
         undeclared fact type or role, which WF-028 reports) *)
  intermediates : path_step list;
  fc_condition : condition_expr option;
  fc_validity : validity_window option;
  (* the symbolic block, required since L2 (B7): the created relation, the
     roles its two ends fill, and the join hop reaching the target *)
  fc_fact_type : string;
  anchor_role : string;
  objective_role : string;
  join_role : role_binding;
  fc_pointer : string;
}

type resolved_start_point_determination = {
  sp_timeline : string option;
  sp_subject_type : string option;
  one_member : string option;
}

type resolved_rule_group = {
  rgr_name : string option;
  recursive : bool;
  member_list : string option list;
  rgr_source : string option;
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
   characteristic target the defaults carry a boolean [default_decision]
   (attribute / do-not-attribute); on an attribute target they carry a
   value [default_consequence] (a leeg-evaluating consequence falls
   through). The initialisation default ([default_initialisation], lowest
   only) has no written justification: it is justified iff the target
   is empty in the input facts (ch. 7.3). Never emitter-produced. *)
type default = {
  default_decision : bool option;
  default_justification : condition_expr option;
  default_consequence : expr option;
  default_initialisation : bool;
  default_exceptions : exception_groups option;
      (* extension (v1, Phase 5c): the nested exception groups
         (RUNTIME-SEMANTICS ch. 7.9). A default carrying them is a group-default
         (no own consequence/decision — its exception groups are its decision) or
         a form-a default (own consequence/decision too — the exception groups
         decide first, the own consequence is the fall-back when they decide
         nothing). A differing-deciding exception group propagates the conflict
         to the root (WF-075). Absent = a flat default. *)
  default_source : string option;
}

(* Extension records (v1, Phase 5c): a Default's `exceptions` field and its
   members (RUNTIME-SEMANTICS ch. 7.9). [egs_unordered] carries the
   declared-unordered flag itself (the root regime, recursed); each
   [exception_group] is a default chain (descending, highest first)
   inheriting the root group's target — it carries no target of its own.
   Never emitter-produced. *)
and exception_groups = { egs_unordered : bool; egs_members : exception_group list }
and exception_group = { eg_defaults : default list; eg_source : string option }

type prioritized_defaults = {
  pd_name : string option;
  pd_subject : string; (* subject object type *)
  pd_characteristic : string option; (* target characteristic — exactly one of the two *)
  pd_attribute : string option; (* target attribute *)
  defaults : default list; (* descending priority: highest first *)
  pd_unordered : bool;
      (* declared-unordered (ch. 7.2): multiple defaults groups on one target
         are admissible iff every one carries this marking *)
  pd_source : string option;
  pd_validity : validity_window option;
}

(* Extension form (v1, row 10): a named reusable function — DMN's Business
   Knowledge Model (IR-SPEC ch. 2.11). Typed, uniquely named parameters; a
   resolvable result; a closed body; an acyclic invocation graph (WF-067).
   Carried by the optional [function_defs] (the [functionDefs] list under
   [model] on the wire). Never emitter-produced. *)
type function_def = {
  fd_name : string;
  fd_parameters : (string * datatype) list;
  fd_result : datatype;
  fd_body : expr;
  fd_source : string option;
}

(* --- the vocabulary side table (IR-SPEC ch. 2.12) ---------------------- *)

(* The named vocabularies and the designations of a document, and the
   origin of every declaration a producer made itself (IR-SPEC ch. 2.13): a
   side table without semantics. It is not a field of [document], and on
   purpose: the document record is built whole by every producer of this
   library (the Catala and F-logic frontends among them), and no reader but
   the verifier keeps the table. [Loader.vocabularies] reads it from the
   JSON the document is read from, structurally for every reader
   ([Loader.document] calls it) and for its content in the verifier alone
   (WF-116 to WF-121, [Checks.vocabularies]); an executor refuses at load
   only an origin whose form or kind is outside its closed set (WF-121 (a),
   [Checks.origin_refusal]). *)
type vocabulary = {
  voc_name : string;
  voc_default : bool;
  voc_language : string option;
  voc_at : string; (* the pointer of the Vocabulary node *)
}

(* The kinds of declaration a vocabulary can name. [Bn_form] is a named form:
   the six forms of ch. 3 and a defaults group, WF-020's one namespace. *)
type designatable_kind =
  | Bn_object_type
  | Bn_attribute
  | Bn_characteristic
  | Bn_fact_type
  | Bn_role
  | Bn_enumeration_value
  | Bn_parameter
  | Bn_form
  | Bn_rule_group

(* One designation: its vocabulary and its text as written, and the pointer
   of its record (an entry of `designations`, or of `valueDesignations` for
   an enumeration value). *)
type designation = { ben_vocabulary : string; ben_text : string; ben_at : string }

(* The origin of a declaration a producer made itself (IR-SPEC ch. 2.13),
   as written: the form, and the kind, owner and name of the declaration it
   comes from, with the pointer of the origin record. The form and the kind
   are kept as strings, so that WF-121 (a), not this load, names a value
   outside their sets to the verifier. *)
type origin = {
  hk_form : string;
  hk_kind : string;
  hk_owner : string option;
  hk_name : string;
  hk_at : string;
}

(* A designatable declaration: its kind, the declaration that scopes it (the
   object type of an attribute or a characteristic, the fact type of a role,
   the domain of an enumeration value; [None] for the other kinds), its
   canonical name, the pointer of its node (of the value in its list, for an
   enumeration value), its designations in document order, and its origin
   where a producer made it (never on an enumeration value, which has no
   node to carry one). *)
type designatable = {
  bn_kind : designatable_kind;
  bn_owner : string option;
  bn_name : string;
  bn_at : string;
  bn_designations : designation list;
  bn_origin : origin option;
}

type vocabulary_table = {
  vt_vocabularies : vocabulary list;
  vt_designatable : designatable list; (* every designatable declaration, document order *)
  vt_separate_values : (string * string * string) list;
      (* an entry of `valueDesignations` whose value is none of its
         enumeration's values: the domain, the value as written, and the
         pointer of the entry's `value` *)
}

(* The M1 root nests [model] (the declaration set) and [rules]; this record
   keeps them flat, and [format] is checked by the loader and not kept. *)
type document = {
  object_types : (string * object_type) list;
  domain_specs : (string * datatype option) list;
      (* the Domain wrapper's name and datatype; always [Some] since M1. At
         this declaration position the wire writes no self-name, and the
         loader sets the datatype's own domain field to the wrapper's name *)
  parameters : (string * parameter) list;
  unit_systems : (string * unit_system) list;
  fact_types : (string * fact_type_name) list;
  dimensions : (string * dimension) list;
  timelines : timeline_state list;
  day_kinds : day_kind list;
  resolved_rules : resolved_rule list;
  resolved_day_kind_definitions : resolved_day_kind list;
  object_creations : resolved_object_creation list;
  allocations : resolved_allocation list;
  consistencies : resolved_consistency list;
  uniquenesses : resolved_uniqueness list; (* the wire's single-source versions *)
  uniqueness_unions : resolved_uniqueness_union list; (* the wire's versions with two or more *)
  fact_creations : resolved_fact_creation list;
  resolved_start_point_determinations : resolved_start_point_determination list;
  rule_groups : resolved_rule_group list;
  diagnostics : diagnostic list;
  prioritized_defaults : prioritized_defaults list; (* extension: absent = [] *)
  function_defs : function_def list; (* extension: absent = [] *)
  presentation_annotations : string list;
      (* the JSON pointer of every presentation tag written inside a type
         annotation, in document order; [] on a well-formed document *)
}
