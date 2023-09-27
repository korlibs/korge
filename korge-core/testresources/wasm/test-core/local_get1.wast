;; wat2wasm local_get0.wast
;; Test `local.get` operator

(module
  ;; Typing
  (func (export "as-br_table-value") (param i32) (result i32)
    (block
      (block
        (block
          (br_table 0 1 2 (local.get 0))
          (return (i32.const 0))
        )
        (return (i32.const 1))
      )
      (return (i32.const 2))
    )
    (i32.const 3)
  )

)

(assert_return (invoke "as-br_table-value" (i32.const 1)) (i32.const 2))
