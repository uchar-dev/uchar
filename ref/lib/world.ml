(* The fact base: instances with attributes, characteristics and role
   links, plus the §9.5 error channel. Names are matched through the
   verifier's normalisation (Ir_v0.Checks.norm) so ref/ and the verifier
   cannot drift on name identity.

   Writes implement ch. 5.1 clause 2 (idempotence): the computed value is
   collapsed to the target's declared representation first (ch. 2.4 write
   boundary — a value that does not fit the declared decimals is a fault,
   never an implicit rounding), then compared; only a real change counts. *)

module SMap = Map.Make (String)

let norm = Ir_v0.Checks.norm

type instance = {
  label : string;
  otype : string; (* declared object-type name, as in the document *)
  attrs : Value.t SMap.t; (* keyed by norm name; absent = empty *)
  chars : SMap.key list; (* characteristics currently true (norm names) *)
  left_side : string list SMap.t; (* norm role name -> linked labels *)
  tls : Timeline.t SMap.t;
      (* full case-supplied timelines of temporal attributes (norm name);
         disjoint from [attrs] — a temporal attribute carries either the
         asOfDate-bucket scalar there or its full timeline here *)
  ctls : (int * int) list SMap.t;
      (* true-periods of temporal characteristics (norm name): half-open
         day intervals where the characteristic holds; a gap means it
         does not hold there (the gedurende clip cuts it away, ch. 3.6) *)
}

(* a §9.5 fact. Its identity is rule × subject (ch. 5.6); [codes] are the EF
   codes (ch. 5.7b) of the kinds deposited for that pair, ascending and each
   once, beside the identity: the channel read does not look at them *)
type error_fact = { rule : string; subject : string; codes : string list }

(* an evaluation fault as data: the fields of Value.Fault *)
type fault = { fault_code : string option; fault_params : (string * string) list; fault_text : string }

(* What the last evaluation of one form on one subject came to
   (RUNTIME-SEMANTICS ch. 5.7): the §9.5 deposits it made, as (rule, subject,
   code), or the fault that ended it. [at] is the subject's label, "" for a
   form that evaluates over its whole population at once. The next evaluation
   of that form on that subject replaces it, so at a fixpoint the outcomes are
   those of the world that stands, whichever states the session passed. *)
type outcome = {
  form : string; (* the wire pointer of the form *)
  at : string;
  deposits : (string * string * string option) list;
  fault : fault option;
}

module Key = Map.Make (struct
  type t = string * string

  let compare = compare
end)

(* What a target held before any form wrote it: its value in the input world, or
   what the creation that made the object gave it *)
type held = Held_value of Value.t

(* The form whose write a target's current value is (RUNTIME-SEMANTICS ch. 5.2,
   6.1). Where that form does not decide on its next evaluation the target gets
   [base] back ([release]), so at a fixpoint a value stands only where a form
   that decides there wrote it, whichever states the session passed. *)
type owner = { owner_form : string; initialisation : bool; base : held }

(* The stack of a timeline target per day (RUNTIME-SEMANTICS ch. 7.8): what it
   held before any form wrote it, its input; per writer, as (rank, form,
   segments), the segments it decides, [rank] its place in the document; and the
   initialisation's timeline, which decides the days that no writer decides and
   on which the input reads no value *)
type stack = {
  before : Timeline.t option;
  writers : (int * string * Timeline.t) list;
  init : (string * Timeline.t) option;
}

type t = {
  instances : instance SMap.t; (* by label *)
  order : string list; (* case declaration order, for determinism *)
  channel : error_fact list; (* §9.5: the deposits of [outcomes], one fact per rule × subject *)
  outcomes : outcome list; (* only those with a deposit or a fault, in order of first arrival *)
  evaluating : (string * string * string option) list;
      (* the deposits of the evaluation under way, newest first; [close_evaluation] ends it *)
  owners : owner Key.t;
      (* by (label, norm attribute): the form whose write the current value
         or timeline is. An initialisation's own write does not switch its
         guard off (RUNTIME-SEMANTICS ch. 5.4), so its value is re-evaluated
         under freshness like any attribute write, while a value the case
         supplied or another writer placed does *)
  holders : (string list * bool) Key.t;
      (* by (label, norm characteristic): the attributions that grant it, and
         whether it held before any of them did. It holds while one of them
         decides, or where it held before *)
  mints : (int * int) SMap.t;
      (* by label, for every minted instance: the stratum of the creation
         that minted it and its creation depth in that stratum, recorded at
         the making (RUNTIME-SEMANTICS ch. 5.5, the creation budget). An
         input instance has no entry; its depth is 0 in every stratum *)
  char_writes : string Key.t;
      (* by (label, norm characteristic), for a time-dependent characteristic:
         the form whose write its true periods are *)
  parts : stack Key.t;
      (* by (label, norm attribute), for a timeline target that rules or an
         initialisation write. Its timeline is the union of its writers'
         segments; on a day no writer decides, what it held before; and on a day
         on which that is leeg too, the initialisation's value (RUNTIME-SEMANTICS
         ch. 3.11, 5.7 and 7.8 per day) *)
}

let empty =
  { instances = SMap.empty; order = []; channel = []; outcomes = []; evaluating = []; owners = Key.empty;
    holders = Key.empty; mints = SMap.empty; char_writes = Key.empty; parts = Key.empty }

let add_instance w inst =
  if SMap.mem inst.label w.instances then
    Value.fault "duplicate instance label %S" inst.label
  else
    { w with instances = SMap.add inst.label inst w.instances; order = w.order @ [ inst.label ] }

let get w label =
  match SMap.find_opt label w.instances with
  | Some i -> i
  | None -> Value.fault "unknown instance label %S" label

(* Extensional type membership (IR-SPEC ch. 2.2): every position that
   ranges over a type ranges over { i : typeOf(i) ⊑ T }, so a supertype
   rule fires for subtype instances and a supertype aggregation counts
   them. The chain lives in the DOCUMENT, which the fact base does not
   hold, so the caller supplies the membership predicate — [isa sub super]
   — rather than this module inventing a second copy of it. *)
let of_type ~isa w otype =
  List.filter_map
    (fun l ->
      let i = get w l in
      if isa i.otype otype then Some i else None)
    w.order

(* A cell of a dimensioned attribute (RUNTIME-SEMANTICS ch. 5.8c) is a member
   of its own, held in [attrs] beside the scalar attributes under this name:
   the attribute's name with the cell's labels in declared order, spelled as
   the dependency graph spells the cell (Ir_v0.Checks.n_cell) and as a fault
   names its place. So every reader and writer of an attribute below serves a
   cell unchanged. *)
let cell name labels = name ^ "[" ^ String.concat ", " labels ^ "]"

(* whether the member [m] is a cell of the attribute [name] *)
let cell_of name m = String.starts_with ~prefix:(norm name ^ "[") (norm m)

let attr i name = Option.value (SMap.find_opt (norm name) i.attrs) ~default:Value.Empty
let has_char i name = List.mem (norm name) i.chars
let left_side i role_key = Option.value (SMap.find_opt (norm role_key) i.left_side) ~default:[]
let timeline i name = SMap.find_opt (norm name) i.tls

(* --- the write boundary (ch. 2.4) -------------------------------------- *)

(* collapse a computed value to the target attribute's declared
   representation: restate into the declared unit; verify the declared
   decimals hold the value exactly (no implicit rounding anywhere — a
   non-representable bounded write faults, surfacing the ch. 2.4 static
   error dynamically) *)
let collapse ~at (decl : Ir_v0.Ast.datatype option) (v : Value.t) =
  match (v, decl) with
  | Empty, _ -> Value.Empty
  | Num (q, u), Some (Number { whole_part; decimal_places; measure_unit; _ } | Percentage { whole_part; decimal_places; measure_unit; _ }) ->
      let target_u = match measure_unit with Some e -> Value.Unit e | None -> Value.Unitless in
      let q = Arith.restate_into target_u q u in
      let scaled = Q.mul q (Q.of_bigint (Z.pow (Z.of_int 10) decimal_places)) in
      (* the unbounded form (not whole, 0 decimals: IR-SPEC ch. 2.4 (c)) has
         no ceiling, so ch. 2.4 point 3 exempts it: 0 decimals is its
         spelling, not a scale *)
      if Z.equal (Q.den scaled) Z.one || ((not whole_part) && decimal_places = 0) then Value.Num (q, target_u)
      else
        Value.faultc ~params:[ ("place", at); ("value", (Q.to_string q)); ("decimals", string_of_int decimal_places) ] "EF-006"
          "%s: value %s does not fit the declared %d decimals — an explicit rounding is \
           mandatory (ch. 2.4)"
          at (Q.to_string q) decimal_places
  | _ -> v

let update w inst = { w with instances = SMap.add inst.label inst w.instances }

(* returns (world, changed); empty-over-empty is a no-op,
   empty-over-value is a change (ch. 1.2g) *)
let store_attr w ~label ~attr:name ~decl v =
  let i = get w label in
  let v = collapse ~at:(label ^ "." ^ name) decl v in
  if Arith.equal (attr i name) v then (w, false)
  else (update w { i with attrs = SMap.add (norm name) v i.attrs }, true)

let held_by_initialisation w key =
  match Key.find_opt key w.owners with Some o -> o.initialisation | None -> false

(* A write that no form holds, a creation's at the making: the target is
   nobody's to give back. A write by any writer but an initialisation takes the
   target from the initialisation, also where it writes the value that stands:
   the value is that writer's then, whichever fired first, and the
   initialisation does not decide (ch. 5.4). That counts as a change: the guard
   of the initialisation reads it, so its evaluation is not current until it
   has run again. *)
let write_attr w ~label ~attr:name ~decl v =
  let key = (label, norm name) in
  let was_initialisation = held_by_initialisation w key in
  let w = { w with owners = Key.remove key w.owners } in
  let w, changed = store_attr w ~label ~attr:name ~decl v in
  (w, changed || was_initialisation)

(* what the target held before the form that holds it now, or now *)
let base_of w key ~now = match Key.find_opt key w.owners with Some o -> o.base | None -> now

(* The write of [form], a rule or a defaults group that decides: as [write_attr],
   and the value is [form]'s until another writer changes it or [form] no longer
   decides ([release]) *)
let write_attr_by w ~form ~label ~attr:name ~decl v =
  let key = (label, norm name) in
  let was_initialisation = held_by_initialisation w key in
  let base = base_of w key ~now:(Held_value (attr (get w label) name)) in
  let w, changed = store_attr w ~label ~attr:name ~decl v in
  ({ w with owners = Key.add key { owner_form = form; initialisation = false; base } w.owners }, changed || was_initialisation)

(* The attribute values [after] holds and [before] did not, as (label, norm
   attribute, value) in case declaration order; an instance [before] lacks
   counts with every attribute it carries. The divergence diagnosis of
   Eval.evaluate reads one form's writes off this, so it observes the writes
   [write_attr] made instead of evaluating the forms a second way. *)
let changed_attrs ~before ~after =
  List.concat_map
    (fun label ->
      let i = get after label in
      let old a =
        match SMap.find_opt label before.instances with Some o -> attr o a | None -> Value.Empty
      in
      List.rev
        (SMap.fold (fun a v acc -> if Arith.equal (old a) v then acc else (label, a, v) :: acc) i.attrs []))
    after.order

(* An initialisation's guard (ch. 5.4): the target holds nothing, or nothing
   but what an initialisation wrote to it. *)
let init_may_write w ~label ~attr:name =
  attr (get w label) name = Value.Empty || held_by_initialisation w (label, norm name)

(* The write of the initialisation [form]: as [write_attr_by], and the value it
   stores is held as an initialisation's. Storing nothing holds no target: the
   guard admits an empty target anyway, and a held empty target would make every
   other writer's empty write a change, so the session would never settle. *)
let write_attr_init w ~form ~label ~attr:name ~decl v =
  let key = (label, norm name) in
  let base = base_of w key ~now:(Held_value (attr (get w label) name)) in
  let w, changed = store_attr w ~label ~attr:name ~decl v in
  let owners =
    if attr (get w label) name <> Value.Empty then
      Key.add key { owner_form = form; initialisation = true; base } w.owners
    else if held_by_initialisation w key then Key.remove key w.owners
    else w.owners
  in
  ({ w with owners }, changed)

(* a timeline store (ch. 3.11): the segments land exactly as computed;
   idempotence compares segment lists exactly — same bounds, value-equal
   payloads. *)
let write_timeline w ~label ~attr:name (tl : Timeline.t) =
  let i = get w label in
  let same (a : Timeline.t) (b : Timeline.t) =
    List.length a = List.length b
    && List.for_all2
         (fun (x : Timeline.segment) (y : Timeline.segment) ->
           x.s_from = y.s_from && x.s_to = y.s_to && Arith.equal x.s_value y.s_value)
         a b
  in
  match SMap.find_opt (norm name) i.tls with
  | Some old when same old tl -> (w, false)
  | _ -> (update w { i with tls = SMap.add (norm name) tl i.tls }, true)

(* [form] does not decide on [label]: where the target carries its write, the
   target gets back what it held before any form wrote it (RUNTIME-SEMANTICS
   ch. 5.2, 6.1). A value the form wrote on a state the session passes would
   otherwise stand while its condition is false or its value leeg, and the end
   state would depend on the order of the rules. *)
let release w ~form ~label ~attr:name =
  let key = (label, norm name) in
  match Key.find_opt key w.owners with
  | Some { owner_form; base; _ } when String.equal owner_form form -> (
      let w = { w with owners = Key.remove key w.owners } in
      let i = get w label in
      match base with
      | Held_value v ->
          if Arith.equal (attr i name) v then (w, false)
          else (update w { i with attrs = SMap.add (norm name) v i.attrs }, true))
  | _ -> (w, false)

(* --- a timeline target that rules write (RUNTIME-SEMANTICS ch. 3.11, 5.7 per day) --- *)

(* the segments each writer of (label, attribute) decides, but [form]'s, as
   (rank, form, segments): [rank] is the writer's place in the document *)
let timeline_parts_but w ~form ~label ~attr:name =
  match Key.find_opt (label, norm name) w.parts with
  | Some st -> List.filter (fun (_, f, _) -> not (String.equal f form)) st.writers
  | None -> []

let spans tl = List.map (fun (s : Timeline.segment) -> (s.s_from, s.s_to)) tl

(* the stack of (label, attribute), or the empty one over what it holds now *)
let stack_of w ~label ~attr:name =
  match Key.find_opt (label, norm name) w.parts with
  | Some st -> st
  | None -> { before = timeline (get w label) name; writers = []; init = None }

(* The days on which the stack [st] leaves its target to the initialisation: no
   writer decides them and the input reads no value on them (ch. 7.8 per day) *)
let init_days (st : stack) =
  let input = List.filter (fun (s : Timeline.segment) -> s.s_value <> Value.Empty) (Option.value st.before ~default:[]) in
  Timeline.complement_all (List.concat_map (fun (_, _, tl) -> spans tl) st.writers @ spans input)

(* The timeline of the target from its stack: every writer's segments as it
   wrote them, in the order of the document, a day that an earlier writer
   decides left out of a later one's; on the days none of them covers, what the
   target held before any of them wrote; and on the days that leaves leeg, the
   initialisation's segments. The caller has checked that no two writers decide
   a day with different values, so the order of the document only says whose
   segment a shared day lies in. An empty stack gives the target back what it
   held before. *)
let store_stack w ~label ~attr:name (st : stack) =
  let key = (label, norm name) in
  let writers = List.sort (fun (a, _, _) (b, _, _) -> compare a b) st.writers in
  let _, own =
    List.fold_left
      (fun (covered, acc) (_, _, tl) ->
        let free = Timeline.complement_all covered in
        (covered @ spans tl, acc @ Timeline.clip tl free))
      ([], []) writers
  in
  let covered = List.concat_map (fun (_, _, tl) -> spans tl) writers in
  match (writers, st.init) with
  | [], None -> (
      let w = { w with parts = Key.remove key w.parts } in
      match st.before with
      | Some tl -> write_timeline w ~label ~attr:name tl
      | None ->
          let i = get w label in
          if timeline i name = None then (w, false)
          else (update w { i with tls = SMap.remove (norm name) i.tls }, true))
  | _ ->
      let gaps = Timeline.complement_all covered in
      let kept = Timeline.clip (Option.value st.before ~default:[]) gaps in
      let filled = match st.init with Some (_, tl) -> Timeline.clip tl (init_days st) | None -> [] in
      let tl =
        List.sort (fun (a : Timeline.segment) (b : Timeline.segment) -> compare a.s_from b.s_from) (own @ kept @ filled)
      in
      write_timeline { w with parts = Key.add key { st with writers } w.parts } ~label ~attr:name tl

(* the write boundary per segment (ch. 2.4, 3.11): a segment whose value does
   not fit the declared decimals faults as a scalar write does, before anything
   is stored *)
let fits ~label ~attr:name ~decl (tl : Timeline.t) =
  List.iter (fun (sg : Timeline.segment) -> ignore (collapse ~at:(label ^ "." ^ name) decl sg.s_value)) tl

(* [form], the writer at [rank] in the document, decides the segments [tl] of
   the target (none: it decides no day) *)
let write_timeline_part w ~form ~rank ~label ~attr:name ~decl (tl : Timeline.t) =
  fits ~label ~attr:name ~decl tl;
  let st = stack_of w ~label ~attr:name in
  let others = timeline_parts_but w ~form ~label ~attr:name in
  store_stack w ~label ~attr:name { st with writers = (if tl = [] then others else (rank, form, tl) :: others) }

(* [form] does not decide on [label]: its segments leave the target *)
let release_timeline_part w ~form ~label ~attr:name =
  match Key.find_opt (label, norm name) w.parts with
  | Some st when List.exists (fun (_, f, _) -> String.equal f form) st.writers ->
      store_stack w ~label ~attr:name
        { st with writers = List.filter (fun (_, f, _) -> not (String.equal f form)) st.writers }
  | _ -> (w, false)

(* The initialisation [form] of the target: [tl] is its value on the days of
   its condition, and it decides those of them that the stack leaves to it
   ([init_days]). It reads the input and the writers, never its own write, so
   its value is an equation like any other (ch. 5.4). The caller holds the
   segments it decides to the write boundary ([fits]). *)
let write_timeline_init w ~form ~label ~attr:name (tl : Timeline.t) =
  let st = stack_of w ~label ~attr:name in
  store_stack w ~label ~attr:name { st with init = (if tl = [] then None else Some (form, tl)) }

(* the initialisation [form] does not decide on [label] *)
let release_timeline_init w ~form ~label ~attr:name =
  match Key.find_opt (label, norm name) w.parts with
  | Some ({ init = Some (f, _); _ } as st) when String.equal f form -> store_stack w ~label ~attr:name { st with init = None }
  | _ -> (w, false)

(* the characteristic set, held by no attribution: the case input's or a
   creation's; [set_char_by] holds it for an attribution *)
let set_char w ~label ~char =
  let i = get w label in
  if has_char i char then (w, false)
  else (update w { i with chars = norm char :: i.chars }, true)

(* The attribution [form] decides on [label]: the characteristic holds, and
   holds while one of the attributions that grant it decides (RUNTIME-SEMANTICS
   ch. 5.3) *)
let set_char_by w ~form ~label ~char =
  let key = (label, norm char) in
  let forms, before =
    match Key.find_opt key w.holders with Some h -> h | None -> ([], has_char (get w label) char)
  in
  let w, changed = set_char w ~label ~char in
  let forms = if List.mem form forms then forms else form :: forms in
  ({ w with holders = Key.add key (forms, before) w.holders }, changed)

(* The attribution [form] does not decide on [label]: the characteristic is
   withdrawn where no other attribution grants it and it did not hold before *)
let release_char w ~form ~label ~char =
  let key = (label, norm char) in
  match Key.find_opt key w.holders with
  | Some (forms, before) when List.mem form forms -> (
      match List.filter (fun f -> not (String.equal f form)) forms with
      | _ :: _ as rest -> ({ w with holders = Key.add key (rest, before) w.holders }, false)
      | [] ->
          let w = { w with holders = Key.remove key w.holders } in
          if before then (w, false)
          else
            let i = get w label in
            (update w { i with chars = List.filter (fun c -> c <> norm char) i.chars }, true))
  | _ -> (w, false)

(* The write of [form] on a time-dependent characteristic: it holds in
   [periods] and nowhere else (ch. 3.11). Its first write finds no true periods,
   since the case supplies none for a characteristic a rule writes (Eval refuses
   that before). *)
let write_char_timeline_by w ~form ~label ~char periods =
  let key = (label, norm char) in
  let i = get w label in
  let before = SMap.find_opt (norm char) i.ctls in
  if (not (Key.mem key w.char_writes)) && Option.value before ~default:[] <> [] then
    Value.fault
      "the case supplies true periods of the time-dependent characteristic %S of %S, which a rule writes: not \
       executed (no corpus case)"
      char label;
  let w = { w with char_writes = Key.add key form w.char_writes } in
  if before = Some periods then (w, false) else (update w { i with ctls = SMap.add (norm char) periods i.ctls }, true)

(* [form] does not decide on [label]: the characteristic it wrote holds nowhere *)
let release_char_timeline w ~form ~label ~char =
  let key = (label, norm char) in
  match Key.find_opt key w.char_writes with
  | Some f when String.equal f form ->
      let w = { w with char_writes = Key.remove key w.char_writes } in
      let i = get w label in
      if SMap.mem (norm char) i.ctls then (update w { i with ctls = SMap.remove (norm char) i.ctls }, true) else (w, false)
  | _ -> (w, false)

(* role links are naturally idempotent (ch. 5.5): linking an
   already-present target is a no-op. [keys] is the read-preference
   order (the role's plural, then its name — Eval.role_keys): the write
   lands where a reader will look — an already-populated key first,
   else the first key. A second distinct filler on a to-one role is a
   fault, never a silent overwrite. *)
let add_link w ~label ~keys ~many ~target =
  let i = get w label in
  let key =
    match List.find_opt (fun k -> left_side i k <> []) keys with
    | Some k -> k
    | None -> ( match keys with k :: _ -> k | [] -> Value.fault "role without a name")
  in
  let existing = left_side i key in
  if List.mem target existing then (w, false)
  else if (not many) && existing <> [] then
    Value.faultc ~params:[ ("role", key); ("label", label); ("filler", (String.concat ", " existing)) ] "EF-017" "to-one role %S on %S already links %s; a second filler is ill-typed (ch. 5.5)"
      key label
      (String.concat ", " existing)
  else (update w { i with left_side = SMap.add (norm key) (existing @ [ target ]) i.left_side }, true)

(* a §9.5 deposit of the evaluation under way *)
let insert_error w ~rule ~subject ~code = { w with evaluating = (rule, subject, code) :: w.evaluating }

(* §9.5: one error fact per rule × subject, however often re-evaluated; a
   deposit of another kind for the same pair adds its code to that fact *)
let channel_of outcomes =
  List.fold_left
    (fun channel (rule, subject, code) ->
      let same f = String.equal f.rule rule && String.equal f.subject subject in
      let with_code codes =
        match code with
        | Some c when not (List.mem c codes) -> List.sort compare (c :: codes)
        | _ -> codes
      in
      if List.exists same channel then
        List.map (fun f -> if same f then { f with codes = with_code f.codes } else f) channel
      else channel @ [ { rule; subject; codes = with_code [] } ])
    []
    (List.concat_map (fun o -> o.deposits) outcomes)

(* the evaluation of [form] on [at] came to [deposits] or [fault]: this replaces
   its earlier outcome *)
let set_outcome w ~form ~at ~deposits ~fault =
  let mine o = String.equal o.form form && String.equal o.at at in
  let standing = List.find_opt mine w.outcomes in
  let unchanged =
    match standing with
    | None -> deposits = [] && fault = None
    | Some o -> o.deposits = deposits && o.fault = fault
  in
  if unchanged then w
  else
    let entry = { form; at; deposits; fault } in
    let outcomes =
      match (standing, deposits, fault) with
      | Some _, [], None -> List.filter (fun o -> not (mine o)) w.outcomes
      | Some _, _, _ -> List.map (fun o -> if mine o then entry else o) w.outcomes
      | None, _, _ -> w.outcomes @ [ entry ]
    in
    { w with outcomes; channel = channel_of outcomes }

(* the evaluation under way ended without a fault *)
let close_evaluation w ~form ~at =
  set_outcome { w with evaluating = [] } ~form ~at ~deposits:(List.rev w.evaluating) ~fault:None

(* the evaluation ended in [fault]; [w] is the world before it, so what it wrote
   and deposited is gone *)
let fault_evaluation w ~form ~at fault = set_outcome w ~form ~at ~deposits:[] ~fault:(Some fault)

(* the faults that stand, in an order the order of the rules does not move *)
let standing_faults w =
  List.sort_uniq compare (List.filter_map (fun o -> o.fault) w.outcomes)

let channel_has w ~rule =
  let r = norm rule in
  List.exists (fun f -> norm f.rule = r || norm (Ir_v0.Checks.strip_version f.rule) = r) w.channel
