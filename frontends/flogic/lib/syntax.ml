(* The parsed shape of the F-logic slice, and its refusal channel.

   The slice is a syntactic SUBSET of Flora-2 2.2: every form below parses
   in that reasoner. Where Flora-2 has a spelling, the slice uses it —
   [c::d], [o:c], the barred class-level frame [c[|...|]], the cardinality
   braces [{0..*}], the rule id [@!{...}], path expressions [?X.a.b], the
   aggregates [sum{?V | ?V = Path}] and [count{...}]. Where Flora-2 has no
   spelling for something uchar's wire format carries — a datatype with a
   declared decimal scale and sign, a characteristic, a fact type — the
   slice uses uchar's own (Dutch) metamodel vocabulary as an ordinary
   term. Everything outside refuses, loudly and by name. *)

let mandate = "OPDRACHT_OBJECTTYPE_HIERARCHIE_20260910.md"

(* Fail fast with file/line context. Every refusal names the mandate
   document that fixed the slice, so a reader of the message can find the
   list the refusal belongs to. *)
exception Refusal of string

let refuse ?line fmt =
  Printf.ksprintf
    (fun m ->
      let where = match line with None -> "" | Some l -> Printf.sprintf " (line %d)" l in
      raise (Refusal (Printf.sprintf "%s%s — outside the F-logic slice fixed by %s" m where mandate)))
    fmt

(* A refusal that is not about the slice's edges but about a rule of the
   target language; it still names the mandate, because the mandate is
   where the refusal list lives. *)
let refuse_rule ?line ~rule fmt =
  Printf.ksprintf
    (fun m ->
      let where = match line with None -> "" | Some l -> Printf.sprintf " (line %d)" l in
      raise (Refusal (Printf.sprintf "%s%s — %s (%s)" m where rule mandate)))
    fmt

type card = { c_min : int; c_max : int option (* None = the Flora-2 star *) }

(* A signature's right-hand side. [Ty_term] is a compound term
   ([numeriek(niet_negatief, 2)], [kenmerk(bijvoeglijk)]); [Ty_prim] a
   Flora-2 primitive type ([\integer]); [Ty_name] a bare atom, which in a
   fact type names the role's filler object type. *)
type ty =
  | Ty_term of string * string list
  | Ty_prim of string
  | Ty_name of string

type expr =
  | E_num of string
  | E_var of string
  | E_path of string * string list (* ?K . 'rol' . attr *)
  | E_bin of string * expr * expr
  | E_agg of string * expr (* sum{?V | ?V = <path>} — the path is the operand *)

type goal =
  | G_member of string * string * int (* ?V : C *)
  | G_bind of string * expr * int (* ?V = Expr *)
  | G_cmp of string * expr * expr * int

type sigitem =
  | Si_decl of string * card option * ty * int (* name {card} => ty *)
  | Si_val of string * expr * int (* name -> expr : the inheritable default *)

let sigitem_line = function Si_decl (_, _, _, l) | Si_val (_, _, l) -> l

type rule = {
  r_id : string option;
  r_subj : string; (* the head variable, without the '?' *)
  r_attr : string;
  r_value : expr;
  r_body : goal list;
  r_line : int;
}

type stmt =
  | S_sub of string * string * int (* c :: d *)
  | S_member of string * string * int (* t : c *)
  (* [@!{name}] c[| ... |] — the id is carried here because Flora-2 has no
     per-item place for one, and a barred VALUE is a definition that puts a
     name on the wire exactly as a Horn rule does *)
  | S_sig of string * string option * sigitem list * int
  | S_unbarred of string * int (* c[ ... ] — the class object's own frame *)
  | S_rule of rule

(* --- rendering back to the source spelling ------------------------------
   Every [bron] field the emitter writes is produced here, so the wire
   carries the F-logic reading of the node and never a RegelSpraak phrase
   borrowed from the twin golden. *)

let is_bare (s : string) =
  s <> ""
  &&
  let ok = ref true in
  String.iteri
    (fun i c ->
      let head = i = 0 in
      let alpha = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c = '_' in
      let digit = c >= '0' && c <= '9' in
      if not (alpha || ((not head) && digit)) then ok := false)
    s;
  !ok

let atom (s : string) = if is_bare s then s else "'" ^ s ^ "'"

let rec show_expr = function
  | E_num n -> n
  | E_var v -> "?" ^ v
  | E_path (v, steps) -> String.concat "." (("?" ^ v) :: List.map atom steps)
  | E_bin (op, l, r) -> Printf.sprintf "%s %s %s" (show_expr l) op (show_expr r)
  | E_agg (k, operand) ->
      Printf.sprintf "%s{?V | ?V = %s}" k (show_expr operand)

let show_goal = function
  | G_member (v, c, _) -> Printf.sprintf "?%s : %s" v (atom c)
  | G_bind (v, e, _) -> Printf.sprintf "?%s = %s" v (show_expr e)
  | G_cmp (op, l, r, _) -> Printf.sprintf "%s %s %s" (show_expr l) op (show_expr r)
