(* F-logic slice -> uchar-ir-v1 (Ir_v0.Ast).

   THE MAPPING, in one place.

   - [c::d] is the single direct supertype. A second [::] fact for one
     class is the refusal WF-106 exercises for real: F-logic can write two
     parents where the wire cannot represent them.
   - [c[|attr => numeriek(sign, scale)|]] is an attribute declaration,
     [c[|attr => kenmerk(soort)|]] a characteristic. The BARS matter: the
     unbarred frame is the class object's own, and uchar has no
     class-level attribute value for a type object, so it refuses.
   - [c[|attr -> v|]] is the inheritable class-level default and lowers to
     a defaults BASE on that target.
   - a class marked [x : feittype] is a fact type; its signatures are its
     roles, and Flora-2's cardinality braces say which role is to-many.
   - a Horn rule [?S[attr -> Expr] :- ?S : C, Cond...] defines [C.attr].
     WITHOUT further conditions it is the target's base; WITH them it is
     an exception above the base. A target that has only an unconditional
     rule is a bare rule (the standing norm, ch. 7.8) and lowers to a
     Rule; a target that has any exception lowers to a
     PrioritizedDefaults group, exceptions first in source order, base
     last. That is the whole priority device: F-logic's own defeasible
     operators are refused, so nothing else in the slice can order two
     definitions of one target.
   - [sum{?V | ?V = ?K.role.attr}] and [count{?V | ?V = ?K.role}] are the
     aggregations; the role hop becomes the fold's sourceRole. Flora-2's
     aggregates answer 0 over an empty set (measured), which is what the
     sum's [leegwaarde] says in uchar's vocabulary, at the target's
     declared scale. *)

open Syntax
module A = Ir_v0.Ast

(* --- collected declarations --------------------------------------------- *)

type otdecl = {
  o_name : string;
  mutable o_super : (string * int) option;
  mutable o_attrs : (string * A.datatype * int) list; (* source order *)
  mutable o_characteristics : (string * A.characteristic_kind_type * int) list;
  mutable o_bases : (string * expr * string * int) list;
      (* the barred class-level defaults: attribute, value, declared id, line *)
}

type ftdecl = { f_name : string; mutable f_roles : (string * A.role_ref * int) list }

type model = {
  mutable otypes : otdecl list; (* source order *)
  mutable ftypes : ftdecl list;
  mutable rules : rule list;
}

let find_ot m n = List.find_opt (fun o -> o.o_name = n) m.otypes
let find_ft m n = List.find_opt (fun f -> f.f_name = n) m.ftypes

(* --- the reserved vocabulary --------------------------------------------
   Where Flora-2 has a spelling, the slice uses it; where it has none for
   something the wire carries, the slice uses uchar's own metamodel word.
   This is that second list, and it is closed. *)

let reserved_marker = "feittype"

let sign_of ~line = function
  | "onbepaald" -> A.Unspecified
  | "niet_negatief" -> A.Non_negative
  | "negatief" -> A.Negative
  | "positief" -> A.Positive
  | other ->
      refuse ~line "the sign %s: numeriek(...) takes onbepaald, niet_negatief, negatief or positief"
        other

let characteristic_kind_of ~line = function
  | "bijvoeglijk" -> A.Adjectival
  | "bezittelijk" -> A.Possessive
  | "zelfstandig" -> A.Nominal
  | other -> refuse ~line "the characteristic kind %s: kenmerk(...) takes bijvoeglijk, bezittelijk or zelfstandig" other

let num ?(sign_char = A.Unspecified) ~whole_part ~decimal_places () =
  A.Number { whole_part; decimal_places; sign_char = Some sign_char; measure_unit = None }

type slot = Attribute of A.datatype | Characteristic of A.characteristic_kind_type | Filler of string

let slot_of_ty ~line (t : ty) : slot =
  match t with
  | Ty_term ("numeriek", [ sign; scale ]) ->
      let sign_char = sign_of ~line sign in
      if scale = "geheel" then Attribute (num ~sign_char ~whole_part:true ~decimal_places:0 ())
      else (
        match int_of_string_opt scale with
        | Some d when d >= 0 -> Attribute (num ~sign_char ~whole_part:false ~decimal_places:d ())
        | _ ->
            refuse ~line
              "the scale %s: numeriek(...) takes 'geheel' or a decimal count" scale)
  | Ty_term ("kenmerk", [ variety ]) -> Characteristic (characteristic_kind_of ~line variety)
  | Ty_term (f, args) ->
      refuse ~line "the type term %s/%d" f (List.length args)
  | Ty_prim "\\integer" -> Attribute (num ~whole_part:true ~decimal_places:0 ())
  | Ty_prim "\\boolean" -> Attribute A.Boolean
  | Ty_prim "\\string" -> Attribute A.Text_type
  | Ty_prim "\\date" -> Attribute A.Date_in_days
  | Ty_prim "\\decimal" ->
      refuse ~line
        "the primitive type \\decimal: uchar's Number carries a DECLARED decimal scale and \
         sign, which \\decimal does not state — write numeriek(onbepaald, 2)"
  | Ty_prim p -> refuse ~line "the primitive type %s" p
  | Ty_name c -> Filler c

(* --- pass 1: collect ----------------------------------------------------- *)

(* One member name is declared once per class. Two declarations would put
   two members of one name in the object type's `attributes` list, which
   every reader would resolve by its own rule — so the frontend refuses it,
   exactly as it refuses two unconditional definitions of one target. (The
   verifier catches it too, but only after a reader has already had to
   pick.) *)
let declare_once (o : otdecl) name line =
  let hit =
    List.find_map (fun (n, _, l) -> if n = name then Some l else None) o.o_attrs
    |> function
    | Some l -> Some l
    | None -> List.find_map (fun (n, _, l) -> if n = name then Some l else None) o.o_characteristics
  in
  match hit with
  | Some first ->
      refuse ~line "%s declares the member %s twice (line %d and line %d)" o.o_name name first line
  | None -> ()

let collect (stmts : stmt list) : model =
  let m = { otypes = []; ftypes = []; rules = [] } in
  (* the fact-type markers first: a signature's reading depends on which
     kind of class carries it, and the slice is order-free *)
  List.iter
    (function
      | S_member (x, c, line) when c = reserved_marker ->
          if find_ft m x <> None then
            refuse ~line "%s is marked a fact type twice" x;
          m.ftypes <- m.ftypes @ [ { f_name = x; f_roles = [] } ]
      | _ -> ())
    stmts;
  let ot n =
    match find_ot m n with
    | Some o -> o
    | None ->
        let o = { o_name = n; o_super = None; o_attrs = []; o_characteristics = []; o_bases = [] } in
        m.otypes <- m.otypes @ [ o ];
        o
  in
  List.iter
    (function
      | S_member (x, c, line) when c <> reserved_marker ->
          refuse_rule ~line
            ~rule:"uchar separates program from data: instances, their attribute values and their \
                   characteristics live in a uchar-case-v0 case document, never in the program"
            "the ground membership fact %s : %s" x c
      | S_member _ -> ()
      | S_unbarred (c, line) ->
          refuse ~line
            "the unbarred class-level frame %s[...]: that is the class OBJECT's own value, and \
             uchar has no class-level attribute value for a type object — lowering it to an \
             instance default would change its meaning. The inheritable form carries the bars: \
             %s[|attr -> v|]"
            c c
      | S_sub (c, p, line) ->
          if find_ft m c <> None then refuse ~line "the fact type %s in a subclass fact" c;
          let o = ot c in
          (match o.o_super with
          | Some (first, l1) when first <> p ->
              refuse_rule ~line
                ~rule:"WF-106: an object type declares at most one direct supertype. On the wire \
                       that refusal is structural (`supertype` is one string), so an F-logic \
                       source is where it is actually exercised. The two known cases in the \
                       source data — RechtOpUitkeringWazo and RechtUitkeringZw, each with the \
                       parents RechtOpUitkering and RechtUitkeringZwWazo — are refused here and \
                       must be remodelled, never flattened in silence"
                "%s declares a second direct supertype: %s (line %d) and %s" c first l1 p
          | Some _ -> refuse ~line "%s :: %s is declared twice" c p
          | None -> o.o_super <- Some (p, line))
      | S_sig (c, sig_id, items, _) -> (
          match find_ft m c with
          | Some f ->
              List.iter
                (function
                  | Si_val (n, _, line) ->
                      refuse ~line
                        "an inheritable default %s on the fact type %s: a fact type carries \
                         roles, never values" n c
                  | Si_decl (n, card, t, line) -> (
                      match slot_of_ty ~line t with
                      | Filler filler ->
                          let many =
                            match card with
                            | None -> false
                            | Some { c_max = None; _ } -> true
                            | Some { c_max = Some hi; _ } -> hi > 1
                          in
                          f.f_roles <-
                            f.f_roles
                            @ [ ( n,
                                  { A.role_identifier = Some n; role_object_type_name = Some filler;
                                    role_plural = None; role_many = many },
                                  line ) ]
                      | _ ->
                          refuse ~line
                            "the role %s of the fact type %s must name an object type" n c))
                items
          | None ->
              let o = ot c in
              List.iter
                (function
                  | Si_decl (n, card, t, line) -> (
                      if card <> None then
                        refuse ~line
                          "a cardinality on %s.%s: cardinality declares a ROLE's arity, and a \
                           role lives on a fact type" c n;
                      match slot_of_ty ~line t with
                      | Attribute dt ->
                          declare_once o n line;
                          o.o_attrs <- o.o_attrs @ [ (n, dt, line) ]
                      | Characteristic variety ->
                          declare_once o n line;
                          o.o_characteristics <- o.o_characteristics @ [ (n, variety, line) ]
                      | Filler f ->
                          refuse ~line
                            "the signature %s.%s names the object type %s: a reference between \
                             object types is a ROLE — declare a fact type (x : feittype) and put \
                             the two role signatures on it" c n f)
                  | Si_val (n, e, line) ->
                      (* the parser has already required the id on exactly this form *)
                      let id =
                        match sig_id with
                        | Some id -> id
                        | None -> refuse ~line "an inheritable class-level default with no id"
                      in
                      o.o_bases <- o.o_bases @ [ (n, e, id, line) ])
                items)
      | S_rule r -> m.rules <- m.rules @ [ r ])
    stmts;
  m

(* --- the chain ----------------------------------------------------------- *)

let rec chain m ~seen n =
  if List.mem n seen then
    refuse_rule ~rule:"WF-104: the supertype graph is acyclic"
      "the supertype chain of %s closes on itself" n;
  match find_ot m n with
  | None -> refuse_rule ~rule:"WF-103: a supertype names a declared object type" "the unknown object type %s" n
  | Some o -> (
      match o.o_super with
      | None -> [ o ]
      | Some (p, line) ->
          if find_ot m p = None then
            refuse_rule ~line ~rule:"WF-103: a supertype names a declared object type"
              "%s :: %s, and %s is not a declared object type" n p p;
          o :: chain m ~seen:(n :: seen) p)

let chain m n = chain m ~seen:[] n

let find_attr m cls name =
  List.filter_map
    (fun o -> match List.find_opt (fun (n, _, _) -> n = name) o.o_attrs with
      | Some (_, dt, _) -> Some (o.o_name, dt)
      | None -> None)
    (chain m cls)

let find_characteristic m cls name =
  List.exists (fun o -> List.exists (fun (n, _, _) -> n = name) o.o_characteristics) (chain m cls)

let attr_type m ~line cls name =
  match find_attr m cls name with
  | [ (_, dt) ] -> dt
  | [] -> refuse ~line "%s has no attribute %s (declared on it or on a supertype)" cls name
  | (a, _) :: (b, _) :: _ ->
      refuse_rule ~line
        ~rule:"WF-105: no member name is declared twice along one chain"
        "the attribute %s is declared on both %s and %s" name a b

(* --- literals and their derived types ------------------------------------ *)

let literal_type (lex : string) =
  match String.index_opt lex '.' with
  | None -> num ~whole_part:true ~decimal_places:0 ()
  | Some i -> num ~whole_part:false ~decimal_places:(String.length lex - i - 1) ()

let join_num ~line a b =
  match (a, b) with
  | A.Number { whole_part = g1; decimal_places = d1; _ }, A.Number { whole_part = g2; decimal_places = d2; _ } ->
      num ~whole_part:(g1 && g2) ~decimal_places:(max d1 d2) ()
  | _ -> refuse ~line "arithmetic over a non-numeric operand"

(* --- role resolution ------------------------------------------------------ *)

(* A hop [?K.role]: the fact type carrying a role of that name whose OTHER
   role is filled by the subject's type (or one of its supertypes). *)
let find_hop m ~line ~subject role =
  let up = List.map (fun o -> o.o_name) (chain m subject) in
  let candidates =
    List.filter_map
      (fun f ->
        match List.find_opt (fun (n, _, _) -> n = role) f.f_roles with
        | None -> None
        | Some (_, r, rline) ->
            let anchored =
              List.exists
                (fun (n, (other : A.role_ref), _) ->
                  n <> role
                  && (match other.role_object_type_name with
                     | Some t -> List.mem t up
                     | None -> false))
                f.f_roles
            in
            if anchored then Some (f, r, rline) else None)
      m.ftypes
  in
  match candidates with
  | [ (f, r, rline) ] -> (
      match r.A.role_object_type_name with
      | Some filler -> (f, filler)
      | None -> refuse ~line:rline "the role %s declares no filler object type" role)
  | [] ->
      refuse ~line
        "no fact type connects %s to a role named %s" subject role
  | _ -> refuse ~line "the role %s reaches %s over more than one fact type" role subject

(* --- expressions ---------------------------------------------------------- *)

type ctx = { c_subject : string; c_var : string; c_binds : (string * expr) list }

let rec low_expr m (ctx : ctx) ~line (e : expr) : A.expr =
  match e with
  | E_num lex ->
      A.Literal { variant = A.Lv_number lex; typ = Some (literal_type lex); source_name = Some lex }
  | E_var v -> (
      match List.assoc_opt v ctx.c_binds with
      | Some e' -> low_expr m ctx ~line e'
      | None -> refuse ~line "the unbound variable ?%s" v)
  | E_path (v, [ attr ]) when v = ctx.c_var ->
      (* the read writes no type (B13); it must still resolve on the subject *)
      ignore (attr_type m ~line ctx.c_subject attr);
      A.Attribute_reference
        { attribute_ref = Some attr; object_type = Some ctx.c_subject; source_name = Some (show_expr e);
          navigation_bindings = None }
  | E_path (v, _) when v <> ctx.c_var ->
      refuse ~line "the path %s does not start at the rule's subject ?%s" (show_expr e) ctx.c_var
  | E_path (_, steps) ->
      refuse ~line "the %d-step path %s outside an aggregate" (List.length steps) (show_expr e)
  | E_bin (op, l, r) ->
      let ll = low_expr m ctx ~line l and rr = low_expr m ctx ~line r in
      let op' =
        match op with
        | "+" -> A.Plus | "-" -> A.Min | "*" -> A.Times | "/" -> A.Divided_by
        | _ -> refuse ~line "the operator %s" op
      in
      let typ = join_num ~line (type_of m ~line ll) (type_of m ~line rr) in
      A.Binary { op = op'; left_side = Some ll; right_side = Some rr; typ = Some typ;
                 source_name = Some (show_expr e) }
  | E_agg (kind, _, operand, goals) -> low_agg m ctx ~line ~kind ~operand ~goals ~whole:e

(* an attribute read takes its declaration's type — the wire writes none on
   a reference (B13) — and every other operand carries its own *)
and type_of m ~line (e : A.expr) =
  match e with
  | A.Attribute_reference { attribute_ref = Some a; object_type = Some ot; _ } -> attr_type m ~line ot a
  | A.Literal { typ = Some t; _ } | A.Binary { typ = Some t; _ } | A.Function_call { typ = Some t; _ } -> t
  | _ -> refuse ~line "an operand without a derivable type"

and low_agg m ctx ~line ~kind ~operand ~goals ~whole =
  let v, steps =
    match operand with
    | E_path (v, steps) -> (v, steps)
    | _ -> refuse ~line "an aggregate over %s" (show_expr operand)
  in
  if v <> ctx.c_var then
    refuse ~line "the aggregate path %s does not start at the rule's subject ?%s"
      (show_expr operand) ctx.c_var;
  (* A comprehension reaches this point as a VALUE. A conditioned one is a
     quantification and belongs in a rule body, compared with its threshold
     — the F-logic counterpart of the set reading that has no fold source to
     live in; see [low_quantifier_fold]. *)
  (match (kind, goals) with
  | _, [] -> ()
  | "count", _ ->
      refuse ~line
        "count{...} with a condition on its members in a VALUE position: a comprehension that \
         states a condition of the members it counts is a QUANTIFICATION, and a quantification \
         needs a threshold to compare its count with — write it as a goal of the rule body, \
         count{...} >= n"
  | _ ->
      refuse ~line
        "sum{...} carries no subselection: the slice's sum adds ONE attribute over one role hop, \
         and a summed set states no condition of its members");
  match (kind, steps) with
  | "count", [ role ] ->
      let f, el = find_hop m ~line ~subject:ctx.c_subject role in
      A.Function_call
        { kind = A.Count;
          call_arguments =
            [ A.Attribute_reference
                { attribute_ref = Some role; object_type = Some ctx.c_subject;
                  source_name = Some (show_expr operand); navigation_bindings = None } ];
          element_object_type = Some el; empty_fallback = None; filter = None;
          dimension_selection = None;
          typ = Some (num ~sign_char:A.Non_negative ~whole_part:true ~decimal_places:0 ());
          source_name = Some (show_expr whole); grain = None;
          source_role = Some { rb_fact_type = f.f_name; rb_role = role }; source_path = None }
  | "sum", [ role; attr ] ->
      let f, el = find_hop m ~line ~subject:ctx.c_subject role in
      let dt = attr_type m ~line el attr in
      (* Flora-2 answers 0 for an aggregate over an empty set (measured on
         2.2 beta); [leegwaarde] is that answer in uchar's vocabulary,
         written at the summed attribute's declared scale. *)
      let zero =
        match dt with
        | A.Number { decimal_places = 0; _ } -> "0"
        | A.Number { decimal_places = d; _ } -> "0." ^ String.make d '0'
        | _ -> refuse ~line "sum over the non-numeric attribute %s" attr
      in
      A.Function_call
        { kind = A.Sum_function;
          call_arguments =
            [ A.Attribute_reference
                { attribute_ref = Some attr; object_type = Some el; source_name = Some (show_expr operand);
                  navigation_bindings = None } ];
          element_object_type = Some el;
          empty_fallback =
            Some
              (A.Literal
                 { variant = A.Lv_number zero; typ = Some (literal_type zero); source_name = Some zero });
          filter = None; dimension_selection = None; typ = Some dt;
          source_name = Some (show_expr whole); grain = None;
          source_role = Some { rb_fact_type = f.f_name; rb_role = role }; source_path = None }
  | "count", _ ->
      refuse ~line "count{...} counts the members of ONE role hop, ?%s.role" v
  | "sum", _ ->
      refuse ~line "sum{...} sums ONE attribute over one role hop, ?%s.role.attribute" v
  | _ -> refuse ~line "the aggregate %s{...}" kind

(* --- conditions ------------------------------------------------------------ *)

(* the vacuously true justification WF-050 asks of a base: the quantifier
   all over no parts. F-logic writes no justification for an unconditional rule, so
   the [bron] stays null rather than borrowing a phrase. *)
let always = A.Compound { quantifier_kind = A.All; cutoff = 0; components = []; source_name = None }

let conjoin ~source_name = function
  | [] -> always
  | [ c ] -> c
  | cs -> A.Compound { quantifier_kind = A.All; cutoff = 0; components = cs; source_name = Some source_name }

(* A [count] comprehension that states a condition of its members is a
   QUANTIFICATION over them, and the node for that is the fold. The hop is
   the first conjunct, the LAST goal is the matrix, and everything between
   narrows the range — Flora-2 has no relative clause to hang a
   subselection on, so the position carries the distinction. *)
type quantification = {
  q_var : string; (* the counted member's variable *)
  q_subj : string; (* the variable the hop starts at — the rule's subject *)
  q_role : string;
  q_hop : expr;
  q_filter : goal list;
  q_matrix : goal;
}

let rec goal_vars = function
  | G_member (v, _, _) -> [ v ]
  | G_bind (v, e, _) -> v :: expr_vars e
  | G_cmp (_, l, r, _) -> expr_vars l @ expr_vars r

and expr_vars = function
  | E_num _ -> []
  | E_var v -> [ v ]
  | E_path (v, _) -> [ v ]
  | E_bin (_, l, r) -> expr_vars l @ expr_vars r
  | E_agg (_, _, o, gs) -> expr_vars o @ List.concat_map goal_vars gs

let is_counting_comprehension = function
  | E_agg ("count", _, _, _ :: _) -> true
  | _ -> false

let as_quantification ~line = function
  | E_agg ("count", av, hop, (_ :: _ as goals)) ->
      let subj, role =
        match hop with
        | E_path (s, [ r ]) -> (s, r)
        | E_path (_, steps) ->
            refuse ~line
              "count{?V | ?V = %s, …} states a condition over a %d-step path: a quantifier \
               ranges over INSTANCES, so its comprehension walks ONE role hop, ?X.rol"
              (show_expr hop) (List.length steps)
        | _ -> refuse ~line "count{...} over %s, which is not a role hop" (show_expr hop)
      in
      let rec split acc = function
        | [ last ] -> (List.rev acc, last)
        | g :: rest -> split (g :: acc) rest
        | [] -> assert false
      in
      let q_filter, q_matrix = split [] goals in
      Some { q_var = av; q_subj = subj; q_role = role; q_hop = hop; q_filter; q_matrix }
  | _ -> None

let rec low_cond m ctx (g : goal) : A.condition_expr =
  match g with
  | G_member (_, k, line) ->
      if not (find_characteristic m ctx.c_subject k) then
        refuse ~line "%s is neither a declared object type nor a characteristic of %s" k
          ctx.c_subject;
      A.Characteristic_check
        { characteristic = Some k; negation = false; holder = None;
          time_period = None; source_name = Some (show_goal g); navigation_binding = None }
  | G_cmp (op, l, r, line) when as_quantification ~line l <> None ->
      low_quantifier_fold m ctx ~line ~op ~rhs:r
        ~q:(Option.get (as_quantification ~line l))
        ~source_name:(show_goal g)
  | G_cmp (_, _, r, line) when is_counting_comprehension r ->
      refuse ~line
        "the counting comprehension %s stands on the RIGHT of the comparison: the slice reads \
         the count on the LEFT and its threshold on the right, so one quantification has one \
         spelling"
        (show_expr r)
  | G_cmp (op, l, r, line) ->
      let op' =
        match op with
        | ">" -> A.Greater | ">=" -> A.Greater_or_equal
        | "<" -> A.Less | "<=" -> A.Less_or_equal
        | "=" -> A.Equal | "!=" | "\\=" -> A.Not_equal
        | _ -> refuse ~line "the comparison %s" op
      in
      A.Comparison
        { op = op'; left_side = Some (low_expr m ctx ~line l);
          right_side = Some (low_expr m ctx ~line r); time_period = None;
          source_name = Some (show_goal g) }
  | G_bind (v, _, line) -> refuse ~line "the binding of ?%s is not a condition" v

(* The fold (IR-SPEC ch. 4.5, RUNTIME-SEMANTICS ch. 3.9a). All five
   quantifiers are one comparison of a COUNT with a number, which is why no
   negation form is needed and the slice's \naf / \neg refusal does not
   move: none is count = 0 and all is count = |contributors|, written as
   the same comprehension without its matrix. *)
and low_quantifier_fold m ctx ~line ~op ~q ~rhs ~source_name : A.condition_expr =
  (* The hop starts at the rule's subject, exactly as it must in a VALUE
     position ([low_agg], same message). Without this the range would be
     read off a variable the rule never bound, and the wire would carry a
     sourceRole whose provenance quotes a name with no reading. *)
  if q.q_subj <> ctx.c_var then
    refuse ~line "the aggregate path %s does not start at the rule's subject ?%s"
      (show_expr q.q_hop) ctx.c_var;
  let f, el = find_hop m ~line ~subject:ctx.c_subject q.q_role in
  (* Every goal speaks of the counted member and of nothing else: the
     filter and the matrix are read PER CONTRIBUTOR with the element as
     subject, so a condition of the outer subject has no reading inside.
     That check is also what makes the rename below capture-free. *)
  List.iter
    (fun g ->
      List.iter
        (fun v ->
          if v <> q.q_var then
            refuse ~line
              "the goal %s inside count{...} speaks of ?%s, not of the counted member ?%s: the \
               filter and the matrix are read per contributor, with the member as subject"
              (show_goal g) v q.q_var)
        (goal_vars g))
    (q.q_matrix :: q.q_filter);
  (* the member reaches the wire as ?V whatever letter the author chose,
     exactly as the aggregate's own rendering does *)
  let ectx = { c_subject = el; c_var = "V"; c_binds = [] } in
  let low g = low_cond m ectx (rename_goal q.q_var g) in
  let filter =
    match q.q_filter with
    | [] -> None
    | gs ->
        Some
          (conjoin
             ~source_name:(String.concat ", " (List.map (fun g -> show_goal (rename_goal q.q_var g)) gs))
             (List.map low gs))
  in
  let test = low q.q_matrix in
  let threshold lex =
    match int_of_string_opt lex with
    | Some n when n >= 0 -> n
    | _ ->
        refuse ~line
          "the threshold %s of a counting quantifier is not a whole, non-negative number: the \
           wire carries an integer threshold (WF-108 (e), WF-041)"
          lex
  in
  let quantifier_kind, cutoff =
    match (op, rhs) with
    | "=", E_agg ("count", av2, hop2, goals2) ->
        let twin = E_agg ("count", q.q_var, q.q_hop, q.q_filter) in
        (* compared on the NORMALISED rendering (alpha-equivalence), quoted
           in the letters the author wrote *)
        if show_expr rhs <> show_expr twin then
          refuse ~line
            "the spelling of the quantifier all repeats the comprehension without its last \
             goal, and this one does not — expected %s, found %s: the quantifier all is \
             count(satisfiers) = count(members), and the members are the ones the filter \
             leaves"
            (show_agg_source "count" q.q_var q.q_hop q.q_filter)
            (show_agg_source "count" av2 hop2 goals2);
        (A.All, None)
    | (">=" | "<=" | "="), E_num lex -> (
        match (op, threshold lex) with
        | "=", 0 -> (A.None_of, None)
        | ">=", 0 ->
            (* not a threshold question at all: a count is never negative *)
            refuse ~line
              "a counting quantifier with threshold 0 under '>=': a count is never negative, so \
               this holds of every subject and states no quantification — WF-041 pins a counting \
               threshold at 1"
        | "<=", 0 ->
            refuse ~line
              "a counting quantifier with threshold 0 under '<=': WF-041 pins the bound at 1, and \
               \"no member satisfies the matrix\" has its own spelling — count{...} = 0, which is \
               the quantifier none"
        | ">=", n -> (A.At_least, Some n)
        | "<=", n -> (A.At_most, Some n)
        | _, n -> (A.Exactly, Some n))
    | ((">" | "<") as o), E_num lex when int_of_string_opt lex <> None ->
        (* every advice below is a form the slice ACCEPTS: '<= 0' and
           '<= -1' would refuse in turn, so a reader following the message
           must not arrive at one *)
        let n = Option.get (int_of_string_opt lex) in
        if o = "<" && n = 0 then
          refuse ~line
            "the comparison '< 0' against a counting comprehension: a count is never negative, so \
             this holds of no subject at all and states no quantification"
        else
          let direct =
            match (o, n) with
            | ">", _ -> Printf.sprintf "'>= %d'" (n + 1)
            | "<", 1 -> "'= 0', which is the spelling of the quantifier none"
            | _ -> Printf.sprintf "'<= %d'" (n - 1)
          in
          refuse ~line
            "the strict comparison '%s %s' against a counting comprehension: the threshold on the \
             wire is the number the source wrote, so the slice reads the direct spelling — write \
             %s"
            o lex direct
    | ("!=" | "\\="), _ ->
        (* no rewrite exists: the five quantifiers are >=, <=, = and the
           matrix-free twin, and "a count OTHER than n" is none of them *)
        refuse ~line
          "the inequality '%s' against a counting comprehension: the five quantifiers of the fold \
           are the comparisons >=, <=, = and the matrix-free twin, and a count DIFFERENT from a \
           number is none of them — there is no direct spelling to rewrite this into, so the \
           sentence has to be stated as a quantification the node carries"
          op
    | _ ->
        refuse ~line
          "the counting comprehension is compared with %s: the right side of a count comparison \
           is a whole, non-negative number (the threshold) or the same comprehension WITHOUT its \
           last goal (the spelling of the quantifier all)"
          (show_expr rhs)
  in
  A.Quantifier_fold
    { kv_quantifier = quantifier_kind; kv_threshold = cutoff; kv_element_object_type = Some el;
      kv_filter = filter; kv_test = Some test; kv_source = Some source_name;
      kv_source_role = Some { rb_fact_type = f.f_name; rb_role = q.q_role }; kv_source_path = None }

(* --- rules ----------------------------------------------------------------- *)

type definition = {
  d_name : string option;
  d_target : string * string; (* subject class, attribute or characteristic *)
  d_var : string; (* the head variable, for the [bron] renderings *)
  d_characteristic : bool; (* from the isa head ?S : k *)
  d_target_source : string; (* the head in its own spelling *)
  d_barred : bool; (* from c[|attr -> v|], which is a base by decision *)
  d_cond : A.condition_expr option; (* None = unconditional: the base *)
  d_value : A.expr;
  d_line : int;
}

let low_rule m (r : rule) : definition =
  let subject_bindings =
    List.filter_map
      (function
        | G_member (v, c, line) when v = r.r_subj && find_ot m c <> None -> Some (c, line)
        | _ -> None)
      r.r_body
  in
  let subject =
    match subject_bindings with
    | [ (c, _) ] -> c
    | [] ->
        refuse ~line:r.r_line
          "the rule binds no subject: a rule body names its subject's object type, ?%s : C"
          r.r_subj
    | (a, _) :: (b, l) :: _ ->
        refuse ~line:l "the rule binds ?%s to two object types, %s and %s" r.r_subj a b
  in
  List.iter
    (function
      | G_member (v, c, line) when v <> r.r_subj ->
          refuse ~line "the rule binds a second subject ?%s : %s; the slice defines one target \
                        per rule" v c
      | _ -> ())
    r.r_body;
  let binds =
    List.filter_map (function G_bind (v, e, _) -> Some (v, e) | _ -> None) r.r_body
  in
  let ctx = { c_subject = subject; c_var = r.r_subj; c_binds = binds } in
  let conds =
    List.filter_map
      (function
        | G_member (v, c, _) when v = r.r_subj && c = subject -> None
        | G_bind _ -> None
        | g -> Some (low_cond m ctx g))
      r.r_body
  in
  let target, value, characteristic =
    match r.r_head with
    | H_characteristic k ->
        if not (find_characteristic m subject k) then
          refuse ~line:r.r_line
            "the head ?%s : %s assigns a characteristic, but %s declares no characteristic %s \
             (on it or on a supertype)"
            r.r_subj k subject k;
        (* a characteristic has ONE value it can be given, and the head does
           not write it — the slice does not invent a lexeme for it either,
           so the literal's [bron] stays null *)
        ( k,
          A.Literal { variant = A.Lv_boolean true; typ = Some A.Boolean; source_name = None },
          true )
    | H_attr (attr, e) ->
        if find_characteristic m subject attr then
          refuse ~line:r.r_line
            "the rule head writes the characteristic %s as an attribute value: a characteristic \
             is assigned with the isa head, ?%s : %s"
            attr r.r_subj attr;
        let (_ : A.datatype) = attr_type m ~line:r.r_line subject attr in
        (attr, low_expr m ctx ~line:r.r_line e, false)
  in
  let source_name =
    String.concat ", " (List.filter_map (function
      | G_member (v, c, _) when v = r.r_subj && c = subject -> None
      | G_bind _ -> None
      | g -> Some (show_goal g)) r.r_body)
  in
  { d_name = r.r_id; d_target = (subject, target); d_var = r.r_subj; d_characteristic = characteristic;
    d_target_source = show_head ~var:r.r_subj r.r_head; d_barred = false;
    d_cond = (if conds = [] then None else Some (conjoin ~source_name conds));
    d_value = value; d_line = r.r_line }

(* --- the document ----------------------------------------------------------- *)

let no_validity = Some { A.valid_from = None; valid_to = None }

let objective m ~subject ~attr ~characteristic ~source_name ~line =
  (* the target writes no type of its own (B13); an attribute target still
     resolves on its declaration, which refuses an undeclared one here *)
  if not characteristic then ignore (attr_type m ~line subject attr);
  A.Attribute_reference
    { attribute_ref = Some attr; object_type = Some subject; source_name = Some source_name;
      navigation_bindings = None }

let document (stmts : stmt list) : A.document =
  let m = collect stmts in
  (* every class mentioned as a supertype, a role filler or a rule subject
     must be declared; the chain walk asserts it *)
  List.iter (fun o -> ignore (chain m o.o_name)) m.otypes;
  (* A role's filler is a declared object type. WF-003 catches it later,
     but the lowering reads the filler to type an aggregation, so refusing
     here keeps the failure at the source position that caused it. *)
  List.iter
    (fun f ->
      List.iter
        (fun (_, (r : A.role_ref), line) ->
          match r.role_object_type_name with
          | Some filler when find_ot m filler = None ->
              refuse ~line "the role %s of %s is filled by %s, which is not a declared object type"
                (Option.value r.role_identifier ~default:"?") f.f_name filler
          | _ -> ())
        f.f_roles)
    m.ftypes;
  (* Every name this document puts on the wire, in source order: a Horn
     rule's id and a barred class-level default's id alike. WF-020 holds
     rule names unique across the rule-form arrays, but it filters nulls
     out and never reaches a defaults group's name — so a duplicate landing
     on two GROUPS passes the verifier and leaves two definitions sharing
     one identity downstream. The frontend closes it here, where both kinds
     are still one list. *)
  let named =
    List.map (fun (r : rule) -> (r.r_line, Option.value r.r_id ~default:"")) m.rules
    @ List.concat_map
        (fun o -> List.map (fun (_, _, id, line) -> (line, id)) o.o_bases)
        m.otypes
    |> List.sort (fun (a, _) (b, _) -> compare a b)
  in
  List.iteri
    (fun i (line, id) ->
      match List.find_opt (fun (j, (_, other)) -> j < i && other = id)
              (List.mapi (fun j x -> (j, x)) named)
      with
      | Some (_, (first, _)) ->
          refuse ~line "the id %S is already used at line %d" id first
      | None -> ())
    named;
  let defs = List.map (low_rule m) m.rules in
  (* the barred class-level defaults are bases too, and they arrive before
     the rules in the group order *)
  let barred =
    List.concat_map
      (fun o ->
        List.map
          (fun (attr, e, id, line) ->
            (* Flora-2's inheritable value is a class-level constant: the
               frame carries no instance to read from, so a path in it has
               no reading *)
            (match e with
            | E_num _ -> ()
            | _ ->
                refuse ~line
                  "the inheritable class-level default %s[|%s -> %s|] is not a constant: the \
                   class frame has no instance to read from — write an unconditional rule, \
                   ?X[%s -> ...] :- ?X : %s"
                  o.o_name attr (show_expr e) attr o.o_name);
            let ctx = { c_subject = o.o_name; c_var = "self"; c_binds = [] } in
            let (_ : A.datatype) = attr_type m ~line o.o_name attr in
            { d_name = Some id; d_target = (o.o_name, attr); d_var = "self"; d_characteristic = false;
              d_target_source = show_head ~var:"self" (H_attr (attr, e)); d_barred = true;
              d_cond = None; d_value = low_expr m ctx ~line e; d_line = line })
          o.o_bases)
      m.otypes
  in
  (* a target's identity is (subject type, attribute); groups keep the
     order of their first definition in the source *)
  let targets = ref [] in
  List.iter
    (fun d -> if not (List.mem d.d_target !targets) then targets := !targets @ [ d.d_target ])
    (barred @ defs);
  let resolved = ref [] and groups = ref [] in
  List.iter
    (fun ((subject, attr) as target) ->
      let here = List.filter (fun d -> d.d_target = target) (barred @ defs) in
      (* A CHARACTERISTIC is attributed, not valued: there is one value it
         can be given, so two definitions of one characteristic target
         cannot conflict and the priority device has nothing to order. It
         therefore stays a standing norm (ch. 7.8) with its condition as the
         rule's own condition, and never becomes a defaults stack. A second
         definition refuses for the same reason a second unconditional
         attribute definition does: nothing in the slice orders them. *)
      match here with
      | ({ d_characteristic = true; _ } as d) :: rest ->
          (match rest with
          | d2 :: _ ->
              refuse ~line:d2.d_line
                "?X : %s is defined twice on %s (line %d and line %d): a characteristic target \
                 carries one definition — there is one value to give it, so nothing orders two"
                attr subject d.d_line d2.d_line
          | [] -> ());
          resolved :=
            !resolved
            @ [ { A.rr_name = d.d_name; rr_presentation = Some A.Characteristic_attribution;
                  objective =
                    Some
                      (objective m ~subject ~attr ~characteristic:true ~source_name:d.d_target_source ~line:d.d_line);
                  value_node = Some d.d_value; rr_condition = d.d_cond;
                  rr_validity = no_validity; rr_source_annotations = None;
                  (* the wire pointer is the loader's to set on a record it
                     reads; this one is only ever serialised *)
                  rr_pointer = "" } ]
      | _ -> (
      let bases = List.filter (fun d -> d.d_cond = None) here in
      let excs = List.filter (fun d -> d.d_cond <> None) here in
      let base =
        match bases with
        | [] -> None
        | [ b ] -> Some b
        | b1 :: b2 :: _ ->
            refuse ~line:b2.d_line
              "%s.%s has two unconditional definitions (line %d and line %d): a target carries \
               one base" subject attr b1.d_line b2.d_line
      in
      match (excs, base) with
      | [], Some b when not b.d_barred ->
          (* the bare rule: the standing norm of ch. 7.8, not a default *)
          resolved :=
            !resolved
            @ [ { A.rr_name = b.d_name; rr_presentation = Some A.Calculation;
                  objective =
                    Some
                      (objective m ~subject ~attr ~characteristic:false ~source_name:b.d_target_source ~line:b.d_line);
                  value_node = Some b.d_value; rr_condition = None;
                  rr_validity = no_validity; rr_source_annotations = None;
                  rr_pointer = "" } ]
      | [], None -> ()
      | _ ->
          let default_of d =
            { A.default_decision = None;
              default_justification = Some (Option.value d.d_cond ~default:always);
              default_consequence = Some d.d_value; default_initialisation = false;
              default_exceptions = None; default_source = None }
          in
          let members = List.map default_of excs @ (match base with None -> [] | Some b -> [ default_of b ]) in
          let ident =
            match base with
            | Some b when b.d_name <> None -> b.d_name
            | _ -> (match excs with e :: _ -> e.d_name | [] -> None)
          in
          groups :=
            !groups
            @ [ { A.pd_name = ident; pd_subject = subject; pd_characteristic = None;
                  pd_attribute = Some attr; defaults = members; pd_unordered = false;
                  pd_source = None; pd_validity = no_validity } ]))
    !targets;
  let object_types =
    List.map
      (fun o ->
        ( o.o_name,
          { A.ot_ident = Some o.o_name;
            ot_supertype = (match o.o_super with Some (p, _) -> Some p | None -> None);
            attributes = List.map (fun (n, dt, _) -> (n, Some dt)) o.o_attrs;
            attribute_plural = []; attribute_dimensions = []; attribute_granularity = [];
            attribute_timeline_count = []; attribute_timeline_names = [];
            characteristics = List.map (fun (n, _, _) -> Some n) o.o_characteristics;
            characteristic_kinds = List.map (fun (n, s, _) -> (n, Some s)) o.o_characteristics;
            characteristic_granularity = []; characteristic_timeline_count = [];
            (* F-logic declares no grammatical number and no animacy: the
               two RegelSpraak surface affordances stay unset rather than
               invented *)
            animate = false; plural = None } ))
      m.otypes
  in
  let fact_types =
    List.map
      (fun f ->
        if List.length f.f_roles < 2 then
          refuse "the fact type %s declares %d role(s); a fact type connects at least two"
            f.f_name (List.length f.f_roles);
        (f.f_name, { A.ft_name = Some f.f_name; reciprocal = false;
                     role_names = List.map (fun (_, r, _) -> r) f.f_roles }))
      m.ftypes
  in
  { A.object_types; domain_specs = []; parameters = []; unit_systems = []; fact_types;
    dimensions = []; timelines = []; day_kinds = [];
    resolved_rules = !resolved; resolved_day_kind_definitions = []; object_creations = [];
    allocations = []; consistencies = []; uniquenesses = []; uniqueness_unions = [];
    fact_creations = []; resolved_start_point_determinations = []; rule_groups = []; diagnostics = [];
    prioritized_defaults = !groups; function_defs = []; presentation_annotations = [] }

let of_string (src : string) : A.document = document (Parse.parse src)

let of_file (path : string) : A.document =
  let ic = open_in_bin path in
  let n = in_channel_length ic in
  let src = really_input_string ic n in
  close_in ic;
  of_string src
