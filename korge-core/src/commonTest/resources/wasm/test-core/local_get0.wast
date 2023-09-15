;; wat2wasm local_get0.wast
;; Test `local.get` operator

(module
  ;; Typing

  (func (export "read") (param i64 f32 f64 i32 i32) (result f64)
    (local f32 i64 i64 f64)
    (local.set 5 (f32.const 5.5))
    (local.set 6 (i64.const 6))
    (local.set 8 (f64.const 8))
    (f64.add
      (f64.convert_i64_u (local.get 0))
      (f64.add
        (f64.promote_f32 (local.get 1))
        (f64.add
          (local.get 2)
          (f64.add
            (f64.convert_i32_u (local.get 3))
            (f64.add
              (f64.convert_i32_s (local.get 4))
              (f64.add
                (f64.promote_f32 (local.get 5))
                (f64.add
                  (f64.convert_i64_u (local.get 6))
                  (f64.add
                    (f64.convert_i64_u (local.get 7))
                    (local.get 8)
                  )
                )
              )
            )
          )
        )
      )
    )
  )
)

(assert_return
  (invoke "read"
    (i64.const 1) (f32.const 2) (f64.const 3.3) (i32.const 4) (i32.const 5)
  )
  (f64.const 34.8)
)
