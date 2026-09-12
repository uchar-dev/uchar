(* F-logic slice -> regelspraak-ir-v0 (Ir_v0.Ast).

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
     ResolvedRegel; a target that has any exception lowers to a
     PrioritizedDefaults group, exceptions first in source order, base
     last. That is the whole priority device: F-logic's own defeasible
     operators are refused, so nothing else in the slice can order two
     definitions of one target.
   - [sum{?V | ?V = ?K.role.attr}] and [count{?V | ?V = ?K.role}] are the
     aggregations; the role hop becomes [bronRol]. Flora-2's aggregates
     answer 0 over an empty set (measured), which is what the SOM's
     [leegwaarde] says in uchar's vocabulary, at the target's declared
     scale. *)

open Syntax
module A = Ir_v0.Ast

(* --- collected declarations --------------------------------------------- *)

type otdecl = {
  o_name : string;
  mutable o_super : (string * int) option;
  mutable o_attrs : (string * A.datatype * int) list; (* source order *)
  mutable o_kenmerken : (string * A.kenmerk_soort * int) list;
  mutable o_bases : (string * expr * int) list; (* the barred class-level defaults *)
}

type ftdecl = { f_name : string; mutable f_roles : (string * A.rol * int) list }

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

let teken_of ~line = function
  | "onbepaald" -> A.Onbepaald
  | "niet_negatief" -> A.Niet_negatief
  | "negatief" -> A.Negatief
  | "positief" -> A.Positief
  | other ->
      refuse ~line "the sign %s: numeriek(...) takes onbepaald, niet_negatief, negatief or positief"
        other

let kenmerk_soort_of ~line = function
  | "bijvoeglijk" -> A.Bijvoeglijk
  | "bezittelijk" -> A.Bezittelijk
  | "zelfstandig" -> A.Zelfstandig
  | other -> refuse ~line "the characteristic kind %s: kenmerk(...) takes bijvoeglijk, bezittelijk or zelfstandig" other

let num ?(teken = A.Onbepaald) ~geheel ~decimalen () =
  A.Numeriek { geheel; decimalen; teken = Some teken; eenheid = None }

type slot = Attribute of A.datatype | Characteristic of A.kenmerk_soort | Filler of string

let slot_of_ty ~line (t : ty) : slot =
  match t with
  | Ty_term ("numeriek", [ sign; scale ]) ->
      let teken = teken_of ~line sign in
      if scale = "geheel" then Attribute (num ~teken ~geheel:true ~decimalen:0 ())
      else (
        match int_of_string_opt scale with
        | Some d when d >= 0 -> Attribute (num ~teken ~geheel:false ~decimalen:d ())
        | _ ->
            refuse ~line
              "the scale %s: numeriek(...) takes 'geheel' or a decimal count" scale)
  | Ty_term ("kenmerk", [ soort ]) -> Characteristic (kenmerk_soort_of ~line soort)
  | Ty_term (f, args) ->
      refuse ~line "the type term %s/%d" f (List.length args)
  | Ty_prim "\\integer" -> Attribute (num ~geheel:true ~decimalen:0 ())
  | Ty_prim "\\boolean" -> Attribute A.Booleaan
  | Ty_prim "\\string" -> Attribute A.Tekst
  | Ty_prim "\\date" -> Attribute A.Datum_in_dagen
  | Ty_prim "\\decimal" ->
      refuse ~line
        "the primitive type \\decimal: uchar's Numeriek carries a DECLARED decimal scale and \
         sign, which \\decimal does not state — write numeriek(onbepaald, 2)"
  | Ty_prim p -> refuse ~line "the primitive type %s" p
  | Ty_name c -> Filler c

(* --- pass 1: collect ----------------------------------------------------- *)

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
        let o = { o_name = n; o_super = None; o_attrs = []; o_kenmerken = []; o_bases = [] } in
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
      | S_sig (c, items, _) -> (
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
                          let meervoudig =
                            match card with
                            | None -> false
                            | Some { c_max = None; _ } -> true
                            | Some { c_max = Some hi; _ } -> hi > 1
                          in
                          f.f_roles <-
                            f.f_roles
                            @ [ ( n,
                                  { A.rol_naam = Some n; rol_objecttype_naam = Some filler;
                                    rol_meervoud = None; rol_meervoudig = meervoudig },
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
                      | Attribute dt -> o.o_attrs <- o.o_attrs @ [ (n, dt, line) ]
                      | Characteristic soort -> o.o_kenmerken <- o.o_kenmerken @ [ (n, soort, line) ]
                      | Filler f ->
                          refuse ~line
                            "the signature %s.%s names the object type %s: a reference between \
                             object types is a ROLE — declare a fact type (x : feittype) and put \
                             the two role signatures on it" c n f)
                  | Si_val (n, e, line) -> o.o_bases <- o.o_bases @ [ (n, e, line) ])
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

let find_kenmerk m cls name =
  List.exists (fun o -> List.exists (fun (n, _, _) -> n = name) o.o_kenmerken) (chain m cls)

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
  | None -> num ~geheel:true ~decimalen:0 ()
  | Some i -> num ~geheel:false ~decimalen:(String.length lex - i - 1) ()

let join_num ~line a b =
  match (a, b) with
  | A.Numeriek { geheel = g1; decimalen = d1; _ }, A.Numeriek { geheel = g2; decimalen = d2; _ } ->
      num ~geheel:(g1 && g2) ~decimalen:(max d1 d2) ()
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
        | Some (_, r, _) ->
            let anchored =
              List.exists
                (fun (n, (other : A.rol), _) ->
                  n <> role
                  && (match other.rol_objecttype_naam with
                     | Some t -> List.mem t up
                     | None -> false))
                f.f_roles
            in
            if anchored then Some (f, r) else None)
      m.ftypes
  in
  match candidates with
  | [ (f, r) ] -> (f, r)
  | [] ->
      refuse ~line
        "no fact type connects %s to a role named %s" subject role
  | _ -> refuse ~line "the role %s reaches %s over more than one fact type" role subject

(* --- expressions ---------------------------------------------------------- *)

type ctx = { c_subject : string; c_var : string; c_binds : (string * expr) list }

let rec low_expr m (ctx : ctx) ~line (e : expr) : A.expr =
  match e with
  | E_num lex ->
      A.Literal { waarde = Some lex; typ = Some (literal_type lex); bron = Some lex }
  | E_var v -> (
      match List.assoc_opt v ctx.c_binds with
      | Some e' -> low_expr m ctx ~line e'
      | None -> refuse ~line "the unbound variable ?%s" v)
  | E_path (v, [ attr ]) when v = ctx.c_var ->
      let dt = attr_type m ~line ctx.c_subject attr in
      A.Attribuut_ref
        { attribuut = Some attr; objecttype = Some ctx.c_subject;
          onderwerp_bron = Some ("?" ^ v); navigatie_rol = None; typ = Some dt;
          bron = Some (show_expr e); set = false; navigatie_pad = [];
          navigatie_bindingen = None }
  | E_path (v, _) when v <> ctx.c_var ->
      refuse ~line "the path %s does not start at the rule's subject ?%s" (show_expr e) ctx.c_var
  | E_path (_, steps) ->
      refuse ~line "the %d-step path %s outside an aggregate" (List.length steps) (show_expr e)
  | E_bin (op, l, r) ->
      let ll = low_expr m ctx ~line l and rr = low_expr m ctx ~line r in
      let op' =
        match op with
        | "+" -> A.Plus | "-" -> A.Min | "*" -> A.Maal | "/" -> A.Gedeeld_door
        | _ -> refuse ~line "the operator %s" op
      in
      let typ = join_num ~line (type_of ~line ll) (type_of ~line rr) in
      A.Binair { op = op'; links = Some ll; rechts = Some rr; typ = Some typ;
                 bron = Some (show_expr e) }
  | E_agg (kind, operand) -> low_agg m ctx ~line ~kind ~operand ~whole:e

and type_of ~line (e : A.expr) =
  match e with
  | A.Literal { typ = Some t; _ } | A.Attribuut_ref { typ = Some t; _ }
  | A.Binair { typ = Some t; _ } | A.Functie { typ = Some t; _ } -> t
  | _ -> refuse ~line "an operand without a derivable type"

and low_agg m ctx ~line ~kind ~operand ~whole =
  let v, steps =
    match operand with
    | E_path (v, steps) -> (v, steps)
    | _ -> refuse ~line "an aggregate over %s" (show_expr operand)
  in
  if v <> ctx.c_var then
    refuse ~line "the aggregate path %s does not start at the rule's subject ?%s"
      (show_expr operand) ctx.c_var;
  match (kind, steps) with
  | "count", [ role ] ->
      let f, r = find_hop m ~line ~subject:ctx.c_subject role in
      let el = Option.get r.A.rol_objecttype_naam in
      A.Functie
        { kind = A.Aantal;
          argumenten =
            [ A.Attribuut_ref
                { attribuut = Some role; objecttype = Some ctx.c_subject;
                  onderwerp_bron = Some ("?" ^ v); navigatie_rol = None;
                  typ = Some (A.Datatype_onbekend { reden = Some "niet opgelost" });
                  bron = Some (show_expr operand); set = false; navigatie_pad = [];
                  navigatie_bindingen = None } ];
          element_objecttype = Some el; leegwaarde = None; filter = None;
          dimensie_selectie = None;
          typ = Some (num ~teken:A.Niet_negatief ~geheel:true ~decimalen:0 ());
          bron = Some (show_expr whole); granulariteit = None;
          bron_rol = Some { rb_feittype = f.f_name; rb_rol = role }; bron_pad = None }
  | "sum", [ role; attr ] ->
      let f, r = find_hop m ~line ~subject:ctx.c_subject role in
      let el = Option.get r.A.rol_objecttype_naam in
      let dt = attr_type m ~line el attr in
      (* Flora-2 answers 0 for an aggregate over an empty set (measured on
         2.2 beta); [leegwaarde] is that answer in uchar's vocabulary,
         written at the summed attribute's declared scale. *)
      let zero =
        match dt with
        | A.Numeriek { decimalen = 0; _ } -> "0"
        | A.Numeriek { decimalen = d; _ } -> "0." ^ String.make d '0'
        | _ -> refuse ~line "sum over the non-numeric attribute %s" attr
      in
      A.Functie
        { kind = A.Som;
          argumenten =
            [ A.Attribuut_ref
                { attribuut = Some attr; objecttype = Some el;
                  onderwerp_bron = Some (show_expr (E_path (v, [ role ])));
                  navigatie_rol = None; typ = Some dt; bron = Some (show_expr operand);
                  set = true; navigatie_pad = []; navigatie_bindingen = None } ];
          element_objecttype = Some el;
          leegwaarde =
            Some (A.Literal { waarde = Some zero; typ = Some (literal_type zero);
                              bron = Some zero });
          filter = None; dimensie_selectie = None; typ = Some dt;
          bron = Some (show_expr whole); granulariteit = None;
          bron_rol = Some { rb_feittype = f.f_name; rb_rol = role }; bron_pad = None }
  | "count", _ ->
      refuse ~line "count{...} counts the members of ONE role hop, ?%s.role" v
  | "sum", _ ->
      refuse ~line "sum{...} sums ONE attribute over one role hop, ?%s.role.attribute" v
  | _ -> refuse ~line "the aggregate %s{...}" kind

(* --- conditions ------------------------------------------------------------ *)

let low_cond m ctx (g : goal) : A.conditie =
  match g with
  | G_member (_, k, line) ->
      if not (find_kenmerk m ctx.c_subject k) then
        refuse ~line "%s is neither a declared object type nor a characteristic of %s" k
          ctx.c_subject;
      A.Kenmerk_check
        { kenmerk = Some k; navigatie = None; negatie = false; eigenaar = None;
          periode = None; bron = Some (show_goal g); navigatie_binding = None }
  | G_cmp (op, l, r, line) ->
      let op' =
        match op with
        | ">" -> A.Groter | ">=" -> A.Groter_of_gelijk
        | "<" -> A.Kleiner | "<=" -> A.Kleiner_of_gelijk
        | "=" -> A.Gelijk | "!=" | "\\=" -> A.Ongelijk
        | _ -> refuse ~line "the comparison %s" op
      in
      A.Vergelijking
        { op = op'; links = Some (low_expr m ctx ~line l);
          rechts = Some (low_expr m ctx ~line r); periode = None;
          bron = Some (show_goal g) }
  | G_bind (v, _, line) -> refuse ~line "the binding of ?%s is not a condition" v

(* the vacuously true justification WF-050 asks of a base: ALLE over no
   parts. F-logic writes no justification for an unconditional rule, so
   the [bron] stays null rather than borrowing a phrase. *)
let always = A.Samengesteld { kwantor = A.Alle; drempel = 0; onderdelen = []; bron = None }

let conjoin ~bron = function
  | [] -> always
  | [ c ] -> c
  | cs -> A.Samengesteld { kwantor = A.Alle; drempel = 0; onderdelen = cs; bron = Some bron }

(* --- rules ----------------------------------------------------------------- *)

type definition = {
  d_name : string option;
  d_target : string * string; (* subject class, attribute *)
  d_var : string; (* the head variable, for the [bron] renderings *)
  d_barred : bool; (* from c[|attr -> v|], which is a base by decision *)
  d_cond : A.conditie option; (* None = unconditional: the base *)
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
  if find_kenmerk m subject r.r_attr then
    refuse ~line:r.r_line
      "the rule head writes the characteristic %s: the slice defines attribute values only"
      r.r_attr;
  let (_ : A.datatype) = attr_type m ~line:r.r_line subject r.r_attr in
  let value = low_expr m ctx ~line:r.r_line r.r_value in
  let bron =
    String.concat ", " (List.filter_map (function
      | G_member (v, c, _) when v = r.r_subj && c = subject -> None
      | G_bind _ -> None
      | g -> Some (show_goal g)) r.r_body)
  in
  { d_name = r.r_id; d_target = (subject, r.r_attr); d_var = r.r_subj; d_barred = false;
    d_cond = (if conds = [] then None else Some (conjoin ~bron conds));
    d_value = value; d_line = r.r_line }

(* --- the document ----------------------------------------------------------- *)

let no_geldigheid = Some { A.geld_vanaf = None; geld_tot = None }

let doel m ~subject ~attr ~var ~line =
  let dt = attr_type m ~line subject attr in
  A.Attribuut_ref
    { attribuut = Some attr; objecttype = Some subject;
      onderwerp_bron = Some ("?" ^ var); navigatie_rol = None; typ = Some dt;
      bron = Some (show_expr (E_path (var, [ attr ]))); set = false; navigatie_pad = [];
      navigatie_bindingen = None }

let document (stmts : stmt list) : A.document =
  let m = collect stmts in
  (* every class mentioned as a supertype, a role filler or a rule subject
     must be declared; the chain walk asserts it *)
  List.iter (fun o -> ignore (chain m o.o_name)) m.otypes;
  let defs = List.map (low_rule m) m.rules in
  (* the barred class-level defaults are bases too, and they arrive before
     the rules in the group order *)
  let barred =
    List.concat_map
      (fun o ->
        List.map
          (fun (attr, e, line) ->
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
            { d_name = None; d_target = (o.o_name, attr); d_var = "self"; d_barred = true;
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
  let regels =
    List.map
      (fun (r : rule) -> { A.rg_naam = r.r_id; rg_soort = Some "regel" })
      m.rules
  in
  let resolved = ref [] and groups = ref [] in
  List.iter
    (fun ((subject, attr) as target) ->
      let here = List.filter (fun d -> d.d_target = target) (barred @ defs) in
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
            @ [ { A.rr_naam = b.d_name; rr_soort = Some A.Berekening;
                  doel = Some (doel m ~subject ~attr ~var:b.d_var ~line:b.d_line);
                  waarde = Some b.d_value; rr_voorwaarde = None;
                  rr_geldigheid = no_geldigheid; rr_bron_annotaties = None } ]
      | [], None -> ()
      | _ ->
          let default_of d =
            { A.default_besluit = None;
              default_voorwaarde = Some (Option.value d.d_cond ~default:always);
              default_consequentie = Some d.d_value; default_initialisatie = false;
              default_excepties = None; default_bron = None }
          in
          let members = List.map default_of excs @ (match base with None -> [] | Some b -> [ default_of b ]) in
          let naam =
            match base with
            | Some b when b.d_name <> None -> b.d_name
            | _ -> (match excs with e :: _ -> e.d_name | [] -> None)
          in
          groups :=
            !groups
            @ [ { A.pd_naam = naam; pd_onderwerp = subject; pd_kenmerk = None;
                  pd_attribuut = Some attr; defaults = members; pd_ongeordend = false;
                  pd_bron = None; pd_geldigheid = no_geldigheid } ])
    !targets;
  let objecttypen =
    List.map
      (fun o ->
        ( o.o_name,
          { A.ot_naam = Some o.o_name;
            ot_supertype = (match o.o_super with Some (p, _) -> Some p | None -> None);
            attributen = List.map (fun (n, dt, _) -> (n, Some dt)) o.o_attrs;
            attribuut_meervoud = []; attribuut_dimensies = []; attribuut_granulariteit = [];
            attribuut_tijdlijn_aantal = []; attribuut_tijdlijn_naam = [];
            kenmerken = List.map (fun (n, _, _) -> Some n) o.o_kenmerken;
            kenmerk_soorten = List.map (fun (n, s, _) -> (n, Some s)) o.o_kenmerken;
            kenmerk_granulariteit = []; kenmerk_tijdlijn_aantal = [];
            (* F-logic declares no grammatical number and no animacy: the
               two RegelSpraak surface affordances stay unset rather than
               invented *)
            bezield = false; meervoud = None } ))
      m.otypes
  in
  let feittypen =
    List.map
      (fun f ->
        if List.length f.f_roles < 2 then
          refuse "the fact type %s declares %d role(s); a fact type connects at least two"
            f.f_name (List.length f.f_roles);
        (f.f_name, { A.ft_naam = Some f.f_name; wederkerig = false;
                     rollen = List.map (fun (_, r, _) -> r) f.f_roles }))
      m.ftypes
  in
  { A.objecttypen; domeinen = []; parameters = []; eenheidstelsels = []; feittypen;
    dimensies = []; tijdlijnen = []; dagsoorten = []; dagsoort_per_eenheid = [];
    regels; resolved_regels = !resolved; resolved_dagsoorten = []; object_creaties = [];
    verdelingen = []; consistenties = []; uniciteiten = []; uniciteit_unies = [];
    feit_creaties = []; resolved_startpunten = []; regel_groepen = []; diagnostics = [];
    prioritized_defaults = !groups; function_defs = [] }

let of_string (src : string) : A.document = document (Parse.parse src)

let of_file (path : string) : A.document =
  let ic = open_in_bin path in
  let n = in_channel_length ic in
  let src = really_input_string ic n in
  close_in ic;
  of_string src
