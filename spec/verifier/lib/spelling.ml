(* The translation table between the enumeration values the wire writes and
   the in-memory constructors ([Ast]), one family per enumerated field.

   Since landing L5 the wire spells every enumeration value in English
   lowerCamel (IR-SPEC, Conventions), while the in-memory constructors keep
   their v0 names: the wire was renamed, the in-memory model was not. This
   module is the one table of the OCaml side. The loader decodes with it, so
   the verifier, the reference interpreter and the ASP backend read one
   spelling; the Catala and F-logic emitters encode with it. The section
   `waarden` of the name table (spec/proposals/v1-naamtabel.json) lists each
   family with its v0 values; the wire values of a family here are exactly
   the L5 values of the same family there, the refused ones included.

   A family's [values] are in the order of the schema's enum. Its [refused]
   values are values the verifier refuses BY NAME: the verifier loads them
   so that its rule can cite them, and every other reader refuses them at
   load in the rule's own sentence ([Loader.reader]). Two families carry
   such values: the severity of a diagnostic (`error` and `info`, WF-113
   (a)) and the quantifier (`the`, refused on a QuantifierFold by WF-108
   (a) and an ordinary value of a Compound, where the schema admits it).

   A value outside a family is no value of it: the loader refuses it as
   `unknown <field> value "<v>"`, and [encode] refuses a constructor its
   family does not spell, so neither direction falls back to a default. *)

open Ast

type 'a family = {
  field : string;  (** the wire field the family is written under *)
  values : (string * 'a) list;  (** wire value and constructor, schema order *)
  refused : (string * 'a) list;  (** read, and refused by name by a verifier rule *)
}

let pairs f = f.values @ f.refused

(* The constructor a wire value spells, the refused values included. *)
let decode (f : 'a family) (s : string) : 'a option = List.assoc_opt s (pairs f)

(* Whether a wire value is one the verifier refuses by name. *)
let is_refused (f : 'a family) (s : string) : bool = List.mem_assoc s f.refused

(* The wire value of a constructor. A constructor the family does not spell
   is a caller's defect, never an empty or a guessed value. *)
let encode (f : 'a family) (c : 'a) : string =
  match List.find_opt (fun (_, c') -> c' = c) (pairs f) with
  | Some (s, _) -> s
  | None -> invalid_arg ("Spelling.encode: the " ^ f.field ^ " family spells no such constructor")

let sign : sign_char family =
  {
    field = "sign";
    values =
      [ ("unspecified", Unspecified); ("negative", Negative); ("nonNegative", Non_negative);
        ("positive", Positive) ];
    refused = [];
  }

let characteristic_kind : characteristic_kind_type family =
  {
    field = "kind";
    values = [ ("adjectival", Adjectival); ("possessive", Possessive); ("nominal", Nominal) ];
    refused = [];
  }

(* One family at three positions: a timeline facet, a timeline, and a
   bucketing builtin (IR-SPEC ch. 2.2). *)
let granularity : grain family =
  {
    field = "granularity";
    values =
      [ ("day", Day); ("week", Week); ("month", Month); ("quarter", Quarter); ("year", Year) ];
    refused = [];
  }

(* The presentation tag of a computing rule version (IR-SPEC ch. 3.1). The
   other two in-memory kinds are no tag, and [encode] refuses them. *)
let presentation : rule_presentation family =
  {
    field = "presentation";
    values = [ ("equation", Equation); ("calculation", Calculation) ];
    refused = [];
  }

let method_ : method_name family =
  {
    field = "method";
    values = [ ("equalShares", Equal_shares); ("proportional", Proportional); ("ordered", Ordered) ];
    refused = [];
  }

let comparison_op : compare_op family =
  {
    field = "op";
    values =
      [ ("less", Less); ("lessOrEqual", Less_or_equal); ("greater", Greater);
        ("greaterOrEqual", Greater_or_equal); ("equal", Equal); ("notEqual", Not_equal);
        ("earlier", Earlier); ("earlierOrEqual", Earlier_or_equal); ("later", Later);
        ("laterOrEqual", Later_or_equal) ];
    refused = [];
  }

let binary_op : bin_op family =
  {
    field = "op";
    values =
      [ ("plus", Plus); ("minus", Min); ("reducedBy", Reduced_by); ("times", Times);
        ("dividedBy", Divided_by); ("dividedByAbs", Divided_by_abs); ("power", Power);
        ("percentageOf", Percentage_of) ];
    refused = [];
  }

let rounding_mode : rounding_mode_kind family =
  {
    field = "mode";
    values =
      [ ("floor", Floor); ("ceiling", Ceiling); ("halfAwayFromZero", Half_away_from_zero);
        ("towardZero", Toward_zero); ("awayFromZero", Away_from_zero) ];
    refused = [];
  }

(* The kind decides the node (delta B21): the eight folds are an
   Aggregation's, the seventeen other kinds a BuiltinCall's, so a kind of
   the other node is no value of this one. *)
let aggregation_kind : function_kind family =
  {
    field = "kind";
    values =
      [ ("sum", Sum_function); ("total", Total); ("count", Count); ("minimum", Minimum);
        ("maximum", Maximum); ("first", First); ("last", Last); ("filledCount", Filled_count) ];
    refused = [];
  }

let builtin_kind : function_kind family =
  {
    field = "kind";
    values =
      [ ("squareRoot", Square_root); ("absoluteValue", Absolute_value);
        ("concatenation", Concatenation); ("duration", Duration);
        ("absoluteDuration", Absolute_duration); ("easterSunday", Easter_sunday);
        ("timeProportionalPart", Time_proportional_part); ("dayCount", Day_count);
        ("yearOf", Year_of); ("monthOf", Month_of); ("dayOf", Day_of); ("hourOf", Hour_of);
        ("minuteOf", Minute_of); ("secondOf", Second_of); ("millisecondOf", Millisecond_of);
        ("dateWith", Date_with); ("dateTimeWith", Date_time_with) ];
    refused = [];
  }

let predicate_kind : predicate_kind_type family =
  {
    field = "kind";
    values =
      [ ("empty", Empty_predicate); ("filled", Filled); ("elevenTest", Eleven_test);
        ("exactDigits", Exact_digits); ("dayKind", Day_kind_predicate) ];
    refused = [];
  }

(* One family at two positions. `the` is an ordinary quantifier of a
   Compound and refused by name on a QuantifierFold (WF-108 (a)): the loader
   reads it at both positions, and an executor refuses it on the fold. *)
let quantifier : quantifier_kind family =
  {
    field = "quantifier";
    values =
      [ ("all", All); ("none", None_of); ("atLeast", At_least); ("atMost", At_most);
        ("exactly", Exactly) ];
    refused = [ ("the", The) ];
  }

(* The whole-period qualifier's granularity: the in-memory flag [gp_granularity],
   the v0 boolean `jaar`. *)
let whole_period_granularity : bool family =
  { field = "granularity"; values = [ ("year", true); ("month", false) ]; refused = [] }

(* The one wire severity, and the two the verifier refuses by name (WF-113
   (a)). A diagnostic without a severity is null on the wire and [None] in
   memory; the verifier loads it too, and an executor refuses it. *)
let severity : severity family =
  {
    field = "severity";
    values = [ ("warning", Sev_warning) ];
    refused = [ ("error", Sev_error); ("info", Sev_info) ];
  }

(* The precision of a Date (IR-SPEC ch. 2.4), in this spelling since L3 and
   untouched by L5. *)
let precision : datatype family =
  {
    field = "precision";
    values = [ ("day", Date_in_days); ("millisecond", Date_time_type) ];
    refused = [];
  }

(* The two session values a ContextValue names (IR-SPEC ch. 4.1). The name
   is a free string on the wire and in memory, carried verbatim; WF-098
   closes it over exactly these two, byte for byte, so a v0 spelling or a
   recapitalisation stays a name the rule can cite. *)
let evaluation_date = "evaluationDate"
let evaluation_year = "evaluationYear"
let context_value_names = [ evaluation_date; evaluation_year ]

(* The wire node name of an in-memory expression (IR-SPEC ch. 4), for a
   reader that refuses a node by naming it: the constructors keep their v0
   names, as the enumeration values above do, and [Loader.expr] reads the
   other direction. [Functie] holds both an Aggregation and a BuiltinCall,
   told apart by kind as the wire tells them apart. *)
let expr_node (e : expr) : string =
  match e with
  | Literal _ -> "Literal"
  | Text_composition _ -> "TextComposition"
  | Binary _ -> "Binary"
  | Function_call { kind; _ } ->
      if List.exists (fun (_, k) -> k = kind) (pairs aggregation_kind) then "Aggregation"
      else "BuiltinCall"
  | Instance_set_union _ -> "InstanceSetUnion"
  | Rounding _ -> "Rounding"
  | Clamp _ -> "Clamp"
  | Attribute_reference _ -> "AttributeReference"
  | Dimension_cell_read _ -> "DimensionCellRead"
  | Parameter_ref _ -> "ParameterReference"
  | Variable_reference _ -> "VariableReference"
  | Context_value _ -> "ContextValue"
  | Bound_day _ -> "BoundDay"
  | Expr_unresolved _ -> "Unresolved"
  | Temporal_read _ -> "TemporalRead"
  | Construct _ -> "Construct"
  | Match _ -> "Match"
  | Binder_ref _ -> "BinderRef"
  | Struct_literal _ -> "StructLiteral"
  | Field_access _ -> "FieldAccess"
  | Let _ -> "Let"
  | List_literal _ -> "ListLiteral"
  | Lambda _ -> "Lambda"
  | If_then_else _ -> "IfThenElse"
  | Invoke _ -> "Invoke"
  | Fold _ -> "Fold"
  | Map_list _ -> "MapList"
  | Filter_list _ -> "FilterList"
  | List_length _ -> "ListLength"

(* The node name with its indefinite article, as a refusal sentence names
   the node ("a Literal", "an Unresolved"). The Drools loader spells the
   article by the same rule, on the first letter of the name. *)
let a_node (e : expr) : string =
  let n = expr_node e in
  (match n.[0] with 'A' | 'E' | 'I' | 'O' | 'U' -> "an " | _ -> "a ") ^ n

(* A family is a bijection: two wire values for one constructor, or one wire
   value for two, would make one direction ambiguous. Checked once, when a
   program that reads the wire starts, so a defect in this table stops every
   reader before it reads a document. *)
let bijective (f : 'a family) =
  let rec distinct = function [] -> true | x :: rest -> (not (List.mem x rest)) && distinct rest in
  if not (distinct (List.map fst (pairs f)) && distinct (List.map snd (pairs f))) then
    failwith ("Spelling: the " ^ f.field ^ " family is not a bijection")

let () =
  bijective sign;
  bijective characteristic_kind;
  bijective granularity;
  bijective presentation;
  bijective method_;
  bijective comparison_op;
  bijective binary_op;
  bijective rounding_mode;
  bijective aggregation_kind;
  bijective builtin_kind;
  bijective predicate_kind;
  bijective quantifier;
  bijective whole_period_granularity;
  bijective severity;
  bijective precision
