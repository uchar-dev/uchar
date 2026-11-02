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

type t = {
  instances : instance SMap.t; (* by label *)
  order : string list; (* case declaration order, for determinism *)
  channel : error_fact list; (* §9.5: the deposits of [outcomes], one fact per rule × subject *)
  outcomes : outcome list; (* only those with a deposit or a fault, in order of first arrival *)
  evaluating : (string * string * string option) list;
      (* the deposits of the evaluation under way, newest first; [close_evaluation] ends it *)
  init_written : (string * string) list;
      (* (label, norm attribute) whose current value an initialisation wrote
         and no other writer has changed since (RUNTIME-SEMANTICS ch. 5.4):
         the initialisation's own write does not switch its guard off, so
         its value is re-evaluated under freshness like any attribute write,
         while a value the case supplied or another writer placed does *)
  mints : (int * int) SMap.t;
      (* by label, for every minted instance: the stratum of the creation
         that minted it and its creation depth in that stratum, recorded at
         the making (RUNTIME-SEMANTICS ch. 5.5, the creation budget). An
         input instance has no entry; its depth is 0 in every stratum *)
}

let empty =
  { instances = SMap.empty; order = []; channel = []; outcomes = []; evaluating = []; init_written = []; mints = SMap.empty }

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

(* A write by any writer but an initialisation takes the target out of
   [init_written], also where it writes the value that stands: the value is that
   writer's then, whichever fired first, and the initialisation does not decide
   (ch. 5.4). That counts as a change: the guard of the initialisation reads it,
   so its evaluation is not current until it has run again. *)
let write_attr w ~label ~attr:name ~decl v =
  let key = (label, norm name) in
  let was_initialisation = List.mem key w.init_written in
  let w = { w with init_written = List.filter (( <> ) key) w.init_written } in
  let w, changed = store_attr w ~label ~attr:name ~decl v in
  (w, changed || was_initialisation)

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
  attr (get w label) name = Value.Empty || List.mem (label, norm name) w.init_written

(* The write of an initialisation: as [write_attr], and a value it stores stays
   its own until another writer changes it. Storing nothing holds no target: the
   guard admits an empty target anyway, and a held empty target would make every
   other writer's empty write a change, so the session would never settle. *)
let write_attr_init w ~label ~attr:name ~decl v =
  let w, changed = store_attr w ~label ~attr:name ~decl v in
  let key = (label, norm name) in
  let others = List.filter (( <> ) key) w.init_written in
  let holds = attr (get w label) name <> Value.Empty in
  ({ w with init_written = (if holds then key :: others else others) }, changed)

(* a timeline write (ch. 3.11, the pro-rata target): idempotence
   compares segment lists exactly — same bounds, value-equal payloads.
   Values land exactly as computed (v0 writes the fold's fraction
   without a target rounding; the §6.1.2 discipline is downstream). *)
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

(* characteristics are monotone within one evaluation (ch. 5.3):
   default false, only ever set true *)
let set_char w ~label ~char =
  let i = get w label in
  if has_char i char then (w, false)
  else (update w { i with chars = norm char :: i.chars }, true)

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
