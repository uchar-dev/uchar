(* The slice's serialiser is the shared one of the [uchar-ir-v0] library
   ([Ir_v0.Emit]); its refusals name the frontend "the F-logic slice". The
   slice builds no domain, function definition, object creation or
   consistency, so a document that fills one of those lists comes from a
   lowering the slice does not have, and refuses before anything is
   written. *)
module Shared = Ir_v0.Emit.Make (struct
  let producer = "F-logic slice"
end)

let slice_lists (d : Ir_v0.Ast.document) =
  let unfilled list = function
    | [] -> ()
    | _ -> failwith (Printf.sprintf "Emit: the document fills %s, which the slice never builds" list)
  in
  unfilled "domains" d.domain_specs;
  unfilled "functionDefs" d.function_defs;
  unfilled "objectCreations" d.object_creations;
  unfilled "consistencies" d.consistencies

let to_string ?default_names (d : Ir_v0.Ast.document) : string =
  slice_lists d;
  Shared.to_string ?default_names d

let to_file ?default_names (path : string) (d : Ir_v0.Ast.document) : unit =
  slice_lists d;
  Shared.to_file ?default_names path d
