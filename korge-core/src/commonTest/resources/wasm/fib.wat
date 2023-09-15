(module
  (type (;0;) (func (param i32)))
  (type (;1;) (func (param i32) (result i32)))
  (import "log" "integer" (func (;0;) (type 0)))
  (func (;1;) (type 1) (param i32) (result i32)
    (local i32 i32 i32)
    i32.const 1
    local.set 1
    local.get 0
    i32.const 0
    i32.gt_s
    if  ;; label = @1
      loop  ;; label = @2
        local.get 0
        i32.const 1
        i32.sub
        local.tee 0
        if  ;; label = @3
          local.get 1
          local.get 2
          i32.add
          local.set 3
          local.get 1
          local.set 2
          local.get 3
          local.set 1
          local.get 0
          call 0
          br 1 (;@2;)
        end
      end
      local.get 1
      return
    end
    i32.const 0)
  (memory (;0;) 0)
  (export "fib" (func 1))
  (export "memory" (memory 0)))
