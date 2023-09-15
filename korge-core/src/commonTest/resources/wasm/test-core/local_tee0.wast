;; Test `local.tee` operator

(module
  (func $f (param i32 i32 i32) (result i32) (i32.const -1))
  (type $sig (func (param i32 i32 i32) (result i32)))
  (table funcref (elem $f))
  (func (export "as-call_indirect-first") (param i32) (result i32)
    (call_indirect (type $sig)
      (i32.const 1)
      (local.tee 0 (i32.const 2))
      (i32.const 3)
      (i32.const 0)
    )
  )
)

(assert_return (invoke "as-call_indirect-first" (i32.const 0)) (i32.const -1))
