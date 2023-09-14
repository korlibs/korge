;; Test `br` operator

(module
  ;; Auxiliary definition
  (func $dummy)

  (func (export "nested-block-value") (result i32)
    (i32.add
      (i32.const 1)
      (block (result i32)
        (call $dummy)
        (i32.add (i32.const 4) (br 0 (i32.const 8)))
      )
    )
  )
)

(assert_return (invoke "nested-block-value") (i32.const 9))
