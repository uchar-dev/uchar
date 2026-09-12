(* Lexer + recursive-descent parser for the F-logic slice.

   Hand-written on purpose: the slice is small and its edges are the
   product — every form outside it must refuse by name, which a generated
   parser would report as a nameless syntax error. *)

open Syntax

type tok = Atom of string | Var of string | Num of string | Prim of string | P of string | Eof

type lexeme = { t : tok; line : int }

(* --- lexer -------------------------------------------------------------- *)

let is_alpha c = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c = '_'
let is_digit c = c >= '0' && c <= '9'
let is_alnum c = is_alpha c || is_digit c

(* longest first: '[|' must beat '[', ':-' must beat ':' *)
let puncts =
  [ "@!{"; "@{"; "[|"; "|]"; "::"; ":-"; "*->"; "->"; "=>"; ">="; "<="; "!="; "\\="; "..";
    "["; "]"; "{"; "}"; "("; ")"; "|"; ","; ":"; ">"; "<"; "="; "+"; "-"; "*"; "/" ]

let lex (src : string) : lexeme list =
  let n = String.length src in
  let out = ref [] in
  let line = ref 1 in
  let i = ref 0 in
  let emit t = out := { t; line = !line } :: !out in
  let starts_at p s =
    let l = String.length s in
    p + l <= n && String.sub src p l = s
  in
  while !i < n do
    let c = src.[!i] in
    if c = '\n' then (incr line; incr i)
    else if c = ' ' || c = '\t' || c = '\r' then incr i
    else if starts_at !i "//" then
      while !i < n && src.[!i] <> '\n' do incr i done
    else if starts_at !i "/*" then begin
      i := !i + 2;
      let closed = ref false in
      while (not !closed) && !i < n do
        if starts_at !i "*/" then (i := !i + 2; closed := true)
        else (if src.[!i] = '\n' then incr line; incr i)
      done;
      if not !closed then refuse ~line:!line "an unterminated /* comment"
    end
    else if c = '\'' then begin
      let start = !i + 1 in
      let j = ref start in
      while !j < n && src.[!j] <> '\'' do
        if src.[!j] = '\n' then refuse ~line:!line "a quoted atom running past the end of its line";
        incr j
      done;
      if !j >= n then refuse ~line:!line "an unterminated quoted atom";
      emit (Atom (String.sub src start (!j - start)));
      i := !j + 1
    end
    else if c = '?' then begin
      let start = !i + 1 in
      let j = ref start in
      while !j < n && is_alnum src.[!j] do incr j done;
      if !j = start then refuse ~line:!line "a bare '?' is not a variable";
      emit (Var (String.sub src start (!j - start)));
      i := !j
    end
    else if c = '\\' then begin
      let start = !i in
      let j = ref (!i + 1) in
      while !j < n && is_alnum src.[!j] do incr j done;
      emit (Prim (String.sub src start (!j - start)));
      i := !j
    end
    else if is_digit c then begin
      let start = !i in
      let j = ref !i in
      while !j < n && is_digit src.[!j] do incr j done;
      (* a fraction only when a digit follows the point: '0..' in a
         cardinality and the clause terminator both keep their own token *)
      if !j + 1 < n && src.[!j] = '.' && is_digit src.[!j + 1] then begin
        incr j;
        while !j < n && is_digit src.[!j] do incr j done
      end;
      emit (Num (String.sub src start (!j - start)));
      i := !j
    end
    else if is_alpha c then begin
      let start = !i in
      let j = ref !i in
      while !j < n && is_alnum src.[!j] do incr j done;
      emit (Atom (String.sub src start (!j - start)));
      i := !j
    end
    else if c = '.' then begin
      (* Prolog's rule: a point that ends a clause is followed by layout *)
      if starts_at !i ".." then (emit (P ".."); i := !i + 2)
      else if !i + 1 >= n || src.[!i + 1] = ' ' || src.[!i + 1] = '\t'
              || src.[!i + 1] = '\n' || src.[!i + 1] = '\r' || starts_at (!i + 1) "//"
      then (emit (P "END"); incr i)
      else (emit (P "."); incr i)
    end
    else begin
      match List.find_opt (fun s -> starts_at !i s) puncts with
      | Some s -> emit (P s); i := !i + String.length s
      | None -> refuse ~line:!line "the character %C has no reading" c
    end
  done;
  emit Eof;
  List.rev !out

(* --- parser state ------------------------------------------------------- *)

type st = { mutable toks : lexeme array; mutable pos : int }

let peek st = st.toks.(st.pos).t
let peek2 st = if st.pos + 1 < Array.length st.toks then st.toks.(st.pos + 1).t else Eof
let line st = st.toks.(st.pos).line
let advance st = st.pos <- st.pos + 1

let show = function
  | Atom a -> "'" ^ a ^ "'"
  | Var v -> "?" ^ v
  | Num n -> n
  | Prim p -> p
  | P p -> p
  | Eof -> "end of file"

let expect st p =
  match peek st with
  | P q when q = p -> advance st
  | t -> refuse ~line:(line st) "expected %S, found %s" p (show t)

let atom_of st =
  match peek st with
  | Atom a -> advance st; a
  | Var _ -> refuse ~line:(line st) "a variable where a name is expected: HiLog and meta-programming"
  | t -> refuse ~line:(line st) "expected a name, found %s" (show t)

(* --- refused vocabulary -------------------------------------------------
   Named one by one so the message says which refusal list the form is on,
   instead of "syntax error". *)

let check_reserved st =
  match peek st with
  | Prim ("\\naf" | "\\tnot") -> refuse ~line:(line st) "the negation form %s" (show (peek st))
  | Prim "\\neg" ->
      refuse ~line:(line st)
        "the negation form \\neg (also the defeasible-reasoning operator)"
  | Prim "\\overrides" ->
      refuse ~line:(line st)
        "the defeasible-reasoning operator \\overrides: uchar orders defaults by block \
         position, never by an argumentation theory"
  | P "@{" ->
      refuse ~line:(line st)
        "the argumentation-theory rule descriptor @{...}: defeasible reasoning"
  | Atom "setsemantics" ->
      refuse ~line:(line st)
        "setsemantics{...}: uchar's inheritance is non-monotonic by mandate decision 3 and \
         the two modes are not interchangeable"
  | _ -> ()

(* --- types, expressions, goals ------------------------------------------ *)

let parse_card st =
  expect st "{";
  let lo = match peek st with
    | Num v -> advance st; (try int_of_string v with _ -> refuse ~line:(line st) "a non-integer cardinality bound")
    | t -> refuse ~line:(line st) "expected a cardinality bound, found %s" (show t)
  in
  expect st "..";
  let hi =
    match peek st with
    | P "*" -> advance st; None
    | Num v -> advance st; Some (try int_of_string v with _ -> refuse ~line:(line st) "a non-integer cardinality bound")
    | t -> refuse ~line:(line st) "expected a cardinality bound or '*', found %s" (show t)
  in
  expect st "}";
  { c_min = lo; c_max = hi }

let parse_ty st =
  match peek st with
  | Prim p -> advance st; Ty_prim p
  | Atom a -> (
      advance st;
      match peek st with
      | P "(" ->
          advance st;
          let args = ref [] in
          let rec loop () =
            (match peek st with
            | Atom x -> advance st; args := x :: !args
            | Num x -> advance st; args := x :: !args
            | t -> refuse ~line:(line st) "expected a type argument, found %s" (show t));
            match peek st with P "," -> advance st; loop () | _ -> ()
          in
          loop ();
          expect st ")";
          Ty_term (a, List.rev !args)
      | _ -> Ty_name a)
  | t -> refuse ~line:(line st) "expected a type, found %s" (show t)

let aggregates = [ "sum"; "count" ]
let known_other_aggregates = [ "min"; "max"; "avg"; "collectset"; "collectbag" ]

let rec parse_expr st = parse_add st

and parse_add st =
  let l = ref (parse_mul st) in
  let rec loop () =
    match peek st with
    | P (("+" | "-") as op) ->
        advance st;
        let r = parse_mul st in
        l := E_bin (op, !l, r);
        loop ()
    | _ -> ()
  in
  loop (); !l

and parse_mul st =
  let l = ref (parse_atom_expr st) in
  let rec loop () =
    match peek st with
    | P (("*" | "/") as op) ->
        advance st;
        let r = parse_atom_expr st in
        l := E_bin (op, !l, r);
        loop ()
    | _ -> ()
  in
  loop (); !l

and parse_atom_expr st =
  check_reserved st;
  match peek st with
  | Num v -> advance st; E_num v
  | P "(" -> advance st; let e = parse_expr st in expect st ")"; e
  | Var v ->
      advance st;
      let steps = ref [] in
      let rec loop () =
        match peek st with
        | P "." -> (
            advance st;
            match peek st with
            | Atom a -> advance st; steps := a :: !steps; loop ()
            | Var _ ->
                refuse ~line:(line st)
                  "a variable in an attribute position: HiLog and meta-programming"
            | t -> refuse ~line:(line st) "expected an attribute name after '.', found %s" (show t))
        | _ -> ()
      in
      loop ();
      if !steps = [] then E_var v else E_path (v, List.rev !steps)
  | Atom a when List.mem a aggregates && peek2 st = P "{" -> parse_agg st a
  | Atom a when List.mem a known_other_aggregates && peek2 st = P "{" ->
      refuse ~line:(line st)
        "the aggregate %s{...}: the slice carries sum and count only" a
  | t -> refuse ~line:(line st) "expected a value expression, found %s" (show t)

(* [sum{?V | ?V = <path>}] — Flora-2's own aggregate spelling, restricted
   to the one goal shape uchar can lower: a path over a role. *)
and parse_agg st kind =
  advance st;
  expect st "{";
  let v1 = match peek st with
    | Var v -> advance st; v
    | t -> refuse ~line:(line st) "expected the aggregated variable, found %s" (show t)
  in
  expect st "|";
  let v2 = match peek st with
    | Var v -> advance st; v
    | t -> refuse ~line:(line st) "expected ?%s = <path> inside the aggregate, found %s" v1 (show t)
  in
  if v1 <> v2 then
    refuse ~line:(line st)
      "the aggregate binds ?%s but collects ?%s: the slice carries sum{?V | ?V = <path>} only" v2 v1;
  expect st "=";
  let operand = parse_expr st in
  (match operand with
  | E_path _ -> ()
  | _ ->
      refuse ~line:(line st)
        "an aggregate over %s: the slice carries a path expression over a role only"
        (show_expr operand));
  expect st "}";
  E_agg (kind, operand)

let cmp_ops = [ ">"; ">="; "<"; "<="; "="; "!="; "\\=" ]

let parse_goal st =
  check_reserved st;
  match (peek st, peek2 st) with
  | Var v, P ":" ->
      let l = line st in
      advance st; advance st;
      let c = atom_of st in
      G_member (v, c, l)
  | Var v, P "=" ->
      let l = line st in
      advance st; advance st;
      let e = parse_expr st in
      G_bind (v, e, l)
  | _ ->
      let l = line st in
      let lhs = parse_expr st in
      let op =
        match peek st with
        | P o when List.mem o cmp_ops -> advance st; o
        | t ->
            refuse ~line:l "expected a comparison in the rule body, found %s" (show t)
      in
      let rhs = parse_expr st in
      G_cmp (op, lhs, rhs, l)

(* --- statements --------------------------------------------------------- *)

let parse_sigitems st =
  let items = ref [] in
  let rec loop () =
    let l = line st in
    let name = atom_of st in
    let card = match peek st with P "{" -> Some (parse_card st) | _ -> None in
    (match peek st with
    | P "=>" ->
        advance st;
        items := Si_decl (name, card, parse_ty st, l) :: !items
    | P "->" ->
        advance st;
        if card <> None then
          refuse ~line:l "a cardinality on the inheritable value %s: cardinality declares a \
                          role's arity, never a default's" name;
        items := Si_val (name, parse_expr st, l) :: !items
    | P "*->" ->
        refuse ~line:l
          "the Flora-1 inheritable-value arrow '*->': Flora-2 2.2 writes it with the bars, \
           c[|attr -> v|]"
    | t -> refuse ~line:l "expected '=>' or '->' after %s, found %s" name (show t));
    match peek st with P "," -> advance st; loop () | _ -> ()
  in
  loop ();
  List.rev !items

let skip_to_close st ~opening ~closing =
  let depth = ref 0 in
  let stop = ref false in
  while not !stop do
    (match peek st with
    | P p when p = opening -> incr depth
    | P p when p = closing -> if !depth = 0 then stop := true else decr depth
    | Eof -> refuse ~line:(line st) "an unclosed %s" opening
    | _ -> ());
    if not !stop then advance st
  done;
  advance st

let parse_stmt st : stmt =
  check_reserved st;
  let l0 = line st in
  (* the Flora-2 rule id, used here purely as the rule's NAME *)
  let rule_id =
    match peek st with
    | P "@!{" ->
        advance st;
        let id = atom_of st in
        expect st "}";
        Some id
    | _ -> None
  in
  check_reserved st;
  match peek st with
  | P ":-" ->
      advance st;
      check_reserved st;
      refuse ~line:l0 "a directive (:- ...)"
  | Var v ->
      (* the only variable-headed statement in the slice is a Horn rule *)
      advance st;
      (match peek st with
      | P "[|" ->
          refuse ~line:l0
            "a class-level frame on a variable subject: HiLog and meta-programming"
      | _ -> ());
      expect st "[";
      let attr =
        match peek st with
        | Atom a -> advance st; a
        | Var _ ->
            refuse ~line:l0 "a variable in an attribute position: HiLog and meta-programming"
        | t -> refuse ~line:l0 "expected the target attribute, found %s" (show t)
      in
      expect st "->";
      let value = parse_expr st in
      expect st "]";
      expect st ":-";
      let body = ref [ parse_goal st ] in
      let rec loop () =
        match peek st with
        | P "," -> advance st; body := parse_goal st :: !body; loop ()
        | _ -> ()
      in
      loop ();
      expect st "END";
      (* Flora-2 leaves the rule id optional; uchar does not. A nameless
         rule reaches the wire as `naam: null`, which no WF rule crosses
         (WF-020 filters nulls out and never looks at a defaults group's
         name at all) and which one Drools branch cannot render. Naming is
         the author's job, not the frontend's to invent. *)
      if rule_id = None then
        refuse ~line:l0
          "a rule with no id: write @!{'name'} before the head — a rule reaches the wire by \
           name, and a synthesised one would be this frontend's invention";
      S_rule { r_id = rule_id; r_subj = v; r_attr = attr; r_value = value;
               r_body = List.rev !body; r_line = l0 }
  | Atom a -> (
      if rule_id <> None then
        refuse ~line:l0 "a rule id on %s, which is not a rule" a;
      advance st;
      match peek st with
      | P "::" ->
          advance st;
          let parent = atom_of st in
          expect st "END";
          S_sub (a, parent, l0)
      | P ":" ->
          advance st;
          let c = atom_of st in
          expect st "END";
          S_member (a, c, l0)
      | P "[|" ->
          advance st;
          let items = parse_sigitems st in
          expect st "|]";
          expect st "END";
          S_sig (a, items, l0)
      | P "[" ->
          advance st;
          skip_to_close st ~opening:"[" ~closing:"]";
          expect st "END";
          S_unbarred (a, l0)
      | P "(" ->
          refuse ~line:l0
            "the predicate %s(...): the slice is frame-based — uchar's wire format has no \
             predicate channel" a
      | t -> refuse ~line:l0 "expected '::', ':', '[|' or '[' after %s, found %s" a (show t))
  | t -> refuse ~line:l0 "expected a statement, found %s" (show t)

let parse (src : string) : stmt list =
  let st = { toks = Array.of_list (lex src); pos = 0 } in
  let out = ref [] in
  while peek st <> Eof do
    out := parse_stmt st :: !out
  done;
  List.rev !out
