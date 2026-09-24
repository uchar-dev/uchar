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

let sign : teken family =
  {
    field = "sign";
    values =
      [ ("unspecified", Onbepaald); ("negative", Negatief); ("nonNegative", Niet_negatief);
        ("positive", Positief) ];
    refused = [];
  }

let characteristic_kind : kenmerk_soort family =
  {
    field = "kind";
    values = [ ("adjectival", Bijvoeglijk); ("possessive", Bezittelijk); ("nominal", Zelfstandig) ];
    refused = [];
  }

(* One family at three positions: a timeline facet, a timeline, and a
   bucketing builtin (IR-SPEC ch. 2.2). *)
let granularity : granulariteit family =
  {
    field = "granularity";
    values =
      [ ("day", Dag); ("week", Week); ("month", Maand); ("quarter", Kwartaal); ("year", Jaar) ];
    refused = [];
  }

(* The presentation tag of a computing rule version (IR-SPEC ch. 3.1). The
   other two in-memory kinds are no tag, and [encode] refuses them. *)
let presentation : regel_soort family =
  {
    field = "presentation";
    values = [ ("equation", Gelijkstelling); ("calculation", Berekening) ];
    refused = [];
  }

let method_ : methode family =
  {
    field = "method";
    values = [ ("equalShares", Gelijke_delen); ("proportional", Naar_rato); ("ordered", Op_volgorde) ];
    refused = [];
  }

let comparison_op : vergelijk_op family =
  {
    field = "op";
    values =
      [ ("less", Kleiner); ("lessOrEqual", Kleiner_of_gelijk); ("greater", Groter);
        ("greaterOrEqual", Groter_of_gelijk); ("equal", Gelijk); ("notEqual", Ongelijk);
        ("earlier", Eerder); ("earlierOrEqual", Eerder_of_gelijk); ("later", Later);
        ("laterOrEqual", Later_of_gelijk) ];
    refused = [];
  }

let binary_op : bin_op family =
  {
    field = "op";
    values =
      [ ("plus", Plus); ("minus", Min); ("reducedBy", Verminderd_met); ("times", Maal);
        ("dividedBy", Gedeeld_door); ("dividedByAbs", Gedeeld_door_abs); ("power", Tot_de_macht);
        ("percentageOf", Percentage_van) ];
    refused = [];
  }

let rounding_mode : afrond_modus family =
  {
    field = "mode";
    values =
      [ ("floor", Naar_beneden); ("ceiling", Naar_boven); ("halfAwayFromZero", Rekenkundig);
        ("towardZero", Richting_nul); ("awayFromZero", Weg_van_nul) ];
    refused = [];
  }

(* The kind decides the node (delta B21): the eight folds are an
   Aggregation's, the seventeen other kinds a BuiltinCall's, so a kind of
   the other node is no value of this one. *)
let aggregation_kind : functie_kind family =
  {
    field = "kind";
    values =
      [ ("sum", Som); ("total", Totaal); ("count", Aantal); ("minimum", Minimum);
        ("maximum", Maximum); ("first", Eerste); ("last", Laatste); ("filledCount", Aantal_gevulde) ];
    refused = [];
  }

let builtin_kind : functie_kind family =
  {
    field = "kind";
    values =
      [ ("squareRoot", Wortel); ("absoluteValue", Absolute_waarde);
        ("concatenation", Concatenatie); ("duration", Tijdsduur);
        ("absoluteDuration", Absolute_tijdsduur); ("easterSunday", Eerste_paasdag);
        ("timeProportionalPart", Tijdsevenredig_deel); ("dayCount", Aantal_dagen_in);
        ("yearOf", Jaar_uit); ("monthOf", Maand_uit); ("dayOf", Dag_uit); ("hourOf", Uur_uit);
        ("minuteOf", Minuut_uit); ("secondOf", Seconde_uit); ("millisecondOf", Milliseconde_uit);
        ("dateWith", Datum_met); ("dateTimeWith", Datumtijd_met) ];
    refused = [];
  }

let predicate_kind : predicaat_soort family =
  {
    field = "kind";
    values =
      [ ("empty", Leeg); ("filled", Gevuld); ("elevenTest", Elfproef);
        ("exactDigits", Numeriek_met_exact); ("dayKind", Dagsoort) ];
    refused = [];
  }

(* One family at two positions. `the` is an ordinary quantifier of a
   Compound and refused by name on a QuantifierFold (WF-108 (a)): the loader
   reads it at both positions, and an executor refuses it on the fold. *)
let quantifier : kwantor family =
  {
    field = "quantifier";
    values =
      [ ("all", Alle); ("none", Geen); ("atLeast", Ten_minste); ("atMost", Ten_hoogste);
        ("exactly", Precies) ];
    refused = [ ("the", De) ];
  }

(* The whole-period qualifier's granularity: the in-memory flag [gp_jaar],
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
    values = [ ("day", Datum_in_dagen); ("millisecond", Datum_tijd) ];
    refused = [];
  }

(* The two session values a ContextValue names (IR-SPEC ch. 4.1). The name
   is a free string on the wire and in memory, carried verbatim; WF-098
   closes it over exactly these two, byte for byte, so a v0 spelling or a
   recapitalisation stays a name the rule can cite. *)
let evaluation_date = "evaluationDate"
let evaluation_year = "evaluationYear"
let context_value_names = [ evaluation_date; evaluation_year ]

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
