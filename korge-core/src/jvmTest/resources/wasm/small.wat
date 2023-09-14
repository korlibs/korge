(module
  (type (;0;) (func (param i32 i32 i32) (result i32)))
  (type (;1;) (func (param i32) (result i32)))
  (type (;2;) (func (param i32)))
  (type (;3;) (func (param i32 i32) (result i32)))
  (type (;4;) (func (param i32 i32 i32 i32) (result i32)))
  (import "env" "memory" (memory (;0;) 256 256))
  (import "env" "table" (table (;0;) 6 6 anyfunc))
  (import "env" "tableBase" (global (;0;) i32))
  (import "env" "STACKTOP" (global (;1;) i32))
  (import "env" "abort" (func (;0;) (type 2)))
  (import "env" "___syscall140" (func (;1;) (type 3)))
  (import "env" "___syscall146" (func (;2;) (type 3)))
  (import "env" "___syscall54" (func (;3;) (type 3)))
  (import "env" "___syscall6" (func (;4;) (type 3)))
  (import "env" "_emscripten_memcpy_big" (func (;5;) (type 0)))
  (func (;6;) (type 0) (param i32 i32 i32) (result i32)
    (local i32 i32 i32 i32)
    block  ;; label = @1
      block  ;; label = @2
        get_local 2
        i32.const 16
        i32.add
        tee_local 4
        i32.load
        tee_local 3
        br_if 0 (;@2;)
        get_local 2
        call 7
        if  ;; label = @3
          i32.const 0
          set_local 2
        else
          get_local 4
          i32.load
          set_local 3
          br 1 (;@2;)
        end
        br 1 (;@1;)
      end
      get_local 3
      get_local 2
      i32.const 20
      i32.add
      tee_local 5
      i32.load
      tee_local 4
      i32.sub
      get_local 1
      i32.lt_u
      if  ;; label = @2
        get_local 2
        get_local 0
        get_local 1
        get_local 2
        i32.load offset=36
        i32.const 3
        i32.and
        i32.const 2
        i32.add
        call_indirect (type 0)
        set_local 2
        br 1 (;@1;)
      end
      block  ;; label = @2
        get_local 2
        i32.load8_s offset=75
        i32.const -1
        i32.gt_s
        if  ;; label = @3
          get_local 1
          set_local 3
          loop  ;; label = @4
            get_local 3
            i32.eqz
            if  ;; label = @5
              i32.const 0
              set_local 3
              br 3 (;@2;)
            end
            get_local 0
            get_local 3
            i32.const -1
            i32.add
            tee_local 6
            i32.add
            i32.load8_s
            i32.const 10
            i32.ne
            if  ;; label = @5
              get_local 6
              set_local 3
              br 1 (;@4;)
            end
          end
          get_local 2
          get_local 0
          get_local 3
          get_local 2
          i32.load offset=36
          i32.const 3
          i32.and
          i32.const 2
          i32.add
          call_indirect (type 0)
          tee_local 2
          get_local 3
          i32.lt_u
          br_if 2 (;@1;)
          get_local 0
          get_local 3
          i32.add
          set_local 0
          get_local 1
          get_local 3
          i32.sub
          set_local 1
          get_local 5
          i32.load
          set_local 4
        else
          i32.const 0
          set_local 3
        end
      end
      get_local 4
      get_local 0
      get_local 1
      call 12
      drop
      get_local 5
      get_local 5
      i32.load
      get_local 1
      i32.add
      i32.store
      get_local 3
      get_local 1
      i32.add
      set_local 2
    end
    get_local 2)
  (func (;7;) (type 1) (param i32) (result i32)
    (local i32 i32)
    get_local 0
    i32.const 74
    i32.add
    tee_local 2
    i32.load8_s
    set_local 1
    get_local 2
    get_local 1
    i32.const 255
    i32.add
    get_local 1
    i32.or
    i32.store8
    get_local 0
    i32.load
    tee_local 1
    i32.const 8
    i32.and
    if (result i32)  ;; label = @1
      get_local 0
      get_local 1
      i32.const 32
      i32.or
      i32.store
      i32.const -1
    else
      get_local 0
      i32.const 0
      i32.store offset=8
      get_local 0
      i32.const 0
      i32.store offset=4
      get_local 0
      get_local 0
      i32.load offset=44
      tee_local 1
      i32.store offset=28
      get_local 0
      get_local 1
      i32.store offset=20
      get_local 0
      get_local 1
      get_local 0
      i32.load offset=48
      i32.add
      i32.store offset=16
      i32.const 0
    end
    tee_local 0)
  (func (;8;) (type 0) (param i32 i32 i32) (result i32)
    (local i32 i32 i32 i32 i32 i32 i32 i32 i32 i32 i32)
    get_global 2
    set_local 6
    get_global 2
    i32.const 48
    i32.add
    set_global 2
    get_local 6
    i32.const 16
    i32.add
    set_local 7
    get_local 6
    i32.const 32
    i32.add
    tee_local 3
    get_local 0
    i32.const 28
    i32.add
    tee_local 9
    i32.load
    tee_local 4
    i32.store
    get_local 3
    get_local 0
    i32.const 20
    i32.add
    tee_local 10
    i32.load
    get_local 4
    i32.sub
    tee_local 4
    i32.store offset=4
    get_local 3
    get_local 1
    i32.store offset=8
    get_local 3
    get_local 2
    i32.store offset=12
    get_local 6
    tee_local 8
    get_local 0
    i32.const 60
    i32.add
    tee_local 12
    i32.load
    i32.store
    get_local 8
    get_local 3
    i32.store offset=4
    get_local 8
    i32.const 2
    i32.store offset=8
    block  ;; label = @1
      block  ;; label = @2
        get_local 4
        get_local 2
        i32.add
        tee_local 6
        i32.const 146
        get_local 8
        call 2
        tee_local 1
        i32.const -4096
        i32.gt_u
        if (result i32)  ;; label = @3
          i32.const 1660
          i32.const 0
          get_local 1
          i32.sub
          i32.store
          i32.const -1
        else
          get_local 1
        end
        tee_local 5
        i32.eq
        br_if 0 (;@2;)
        i32.const 2
        set_local 4
        get_local 3
        set_local 1
        get_local 5
        set_local 3
        loop  ;; label = @3
          get_local 3
          i32.const 0
          i32.ge_s
          if  ;; label = @4
            get_local 6
            get_local 3
            i32.sub
            set_local 6
            get_local 1
            i32.const 8
            i32.add
            set_local 5
            get_local 3
            get_local 1
            i32.load offset=4
            tee_local 13
            i32.gt_u
            tee_local 11
            if  ;; label = @5
              get_local 5
              set_local 1
            end
            get_local 4
            get_local 11
            i32.const 31
            i32.shl
            i32.const 31
            i32.shr_s
            i32.add
            set_local 4
            get_local 1
            get_local 1
            i32.load
            get_local 3
            get_local 11
            if (result i32)  ;; label = @5
              get_local 13
            else
              i32.const 0
            end
            i32.sub
            tee_local 3
            i32.add
            i32.store
            get_local 1
            i32.const 4
            i32.add
            tee_local 5
            get_local 5
            i32.load
            get_local 3
            i32.sub
            i32.store
            get_local 7
            get_local 12
            i32.load
            i32.store
            get_local 7
            get_local 1
            i32.store offset=4
            get_local 7
            get_local 4
            i32.store offset=8
            get_local 6
            i32.const 146
            get_local 7
            call 2
            tee_local 5
            i32.const -4096
            i32.gt_u
            if (result i32)  ;; label = @5
              i32.const 1660
              i32.const 0
              get_local 5
              i32.sub
              i32.store
              i32.const -1
            else
              get_local 5
            end
            tee_local 3
            i32.eq
            br_if 2 (;@2;)
            br 1 (;@3;)
          end
        end
        get_local 0
        i32.const 0
        i32.store offset=16
        get_local 9
        i32.const 0
        i32.store
        get_local 10
        i32.const 0
        i32.store
        get_local 0
        get_local 0
        i32.load
        i32.const 32
        i32.or
        i32.store
        get_local 4
        i32.const 2
        i32.eq
        if (result i32)  ;; label = @3
          i32.const 0
        else
          get_local 2
          get_local 1
          i32.load offset=4
          i32.sub
        end
        set_local 2
        br 1 (;@1;)
      end
      get_local 0
      get_local 0
      i32.load offset=44
      tee_local 1
      get_local 0
      i32.load offset=48
      i32.add
      i32.store offset=16
      get_local 9
      get_local 1
      i32.store
      get_local 10
      get_local 1
      i32.store
    end
    get_local 8
    set_global 2
    get_local 2)
  (func (;9;) (type 3) (param i32 i32) (result i32)
    i32.const 1152
    call 13
    drop
    i32.const 0)
  (func (;10;) (type 0) (param i32 i32 i32) (result i32)
    i32.const 1
    call 0
    i32.const 0)
  (func (;11;) (type 1) (param i32) (result i32)
    i32.const 0
    call 0
    i32.const 0)
  (func (;12;) (type 0) (param i32 i32 i32) (result i32)
    (local i32 i32 i32)
    get_local 2
    i32.const 8192
    i32.ge_s
    if  ;; label = @1
      get_local 0
      get_local 1
      get_local 2
      call 5
      return
    end
    get_local 0
    set_local 4
    get_local 0
    get_local 2
    i32.add
    set_local 3
    get_local 0
    i32.const 3
    i32.and
    get_local 1
    i32.const 3
    i32.and
    i32.eq
    if  ;; label = @1
      loop  ;; label = @2
        get_local 0
        i32.const 3
        i32.and
        if  ;; label = @3
          get_local 2
          i32.eqz
          if  ;; label = @4
            get_local 4
            return
          end
          get_local 0
          get_local 1
          i32.load8_s
          i32.store8
          get_local 0
          i32.const 1
          i32.add
          set_local 0
          get_local 1
          i32.const 1
          i32.add
          set_local 1
          get_local 2
          i32.const 1
          i32.sub
          set_local 2
          br 1 (;@2;)
        end
      end
      get_local 3
      i32.const -4
      i32.and
      tee_local 2
      i32.const -64
      i32.add
      set_local 5
      loop  ;; label = @2
        get_local 0
        get_local 5
        i32.le_s
        if  ;; label = @3
          get_local 0
          get_local 1
          i32.load
          i32.store
          get_local 0
          get_local 1
          i32.load offset=4
          i32.store offset=4
          get_local 0
          get_local 1
          i32.load offset=8
          i32.store offset=8
          get_local 0
          get_local 1
          i32.load offset=12
          i32.store offset=12
          get_local 0
          get_local 1
          i32.load offset=16
          i32.store offset=16
          get_local 0
          get_local 1
          i32.load offset=20
          i32.store offset=20
          get_local 0
          get_local 1
          i32.load offset=24
          i32.store offset=24
          get_local 0
          get_local 1
          i32.load offset=28
          i32.store offset=28
          get_local 0
          get_local 1
          i32.load offset=32
          i32.store offset=32
          get_local 0
          get_local 1
          i32.load offset=36
          i32.store offset=36
          get_local 0
          get_local 1
          i32.load offset=40
          i32.store offset=40
          get_local 0
          get_local 1
          i32.load offset=44
          i32.store offset=44
          get_local 0
          get_local 1
          i32.load offset=48
          i32.store offset=48
          get_local 0
          get_local 1
          i32.load offset=52
          i32.store offset=52
          get_local 0
          get_local 1
          i32.load offset=56
          i32.store offset=56
          get_local 0
          get_local 1
          i32.load offset=60
          i32.store offset=60
          get_local 0
          i32.const -64
          i32.sub
          set_local 0
          get_local 1
          i32.const -64
          i32.sub
          set_local 1
          br 1 (;@2;)
        end
      end
      loop  ;; label = @2
        get_local 0
        get_local 2
        i32.lt_s
        if  ;; label = @3
          get_local 0
          get_local 1
          i32.load
          i32.store
          get_local 0
          i32.const 4
          i32.add
          set_local 0
          get_local 1
          i32.const 4
          i32.add
          set_local 1
          br 1 (;@2;)
        end
      end
    else
      get_local 3
      i32.const 4
      i32.sub
      set_local 2
      loop  ;; label = @2
        get_local 0
        get_local 2
        i32.lt_s
        if  ;; label = @3
          get_local 0
          get_local 1
          i32.load8_s
          i32.store8
          get_local 0
          get_local 1
          i32.load8_s offset=1
          i32.store8 offset=1
          get_local 0
          get_local 1
          i32.load8_s offset=2
          i32.store8 offset=2
          get_local 0
          get_local 1
          i32.load8_s offset=3
          i32.store8 offset=3
          get_local 0
          i32.const 4
          i32.add
          set_local 0
          get_local 1
          i32.const 4
          i32.add
          set_local 1
          br 1 (;@2;)
        end
      end
    end
    loop  ;; label = @1
      get_local 0
      get_local 3
      i32.lt_s
      if  ;; label = @2
        get_local 0
        get_local 1
        i32.load8_s
        i32.store8
        get_local 0
        i32.const 1
        i32.add
        set_local 0
        get_local 1
        i32.const 1
        i32.add
        set_local 1
        br 1 (;@1;)
      end
    end
    get_local 4)
  (func (;13;) (type 1) (param i32) (result i32)
    (local i32 i32 i32)
    i32.const 1024
    i32.load
    tee_local 1
    i32.load offset=76
    i32.const -1
    i32.gt_s
    if (result i32)  ;; label = @1
      i32.const 0
    else
      i32.const 0
    end
    set_local 3
    block (result i32)  ;; label = @1
      get_local 0
      i32.const 1
      get_local 0
      call 16
      tee_local 0
      get_local 1
      tee_local 2
      call 15
      get_local 0
      i32.ne
      i32.const 31
      i32.shl
      i32.const 31
      i32.shr_s
      i32.const 0
      i32.lt_s
      if (result i32)  ;; label = @2
        i32.const -1
      else
        get_local 1
        i32.load8_s offset=75
        i32.const 10
        i32.ne
        if  ;; label = @3
          get_local 1
          i32.const 20
          i32.add
          tee_local 2
          i32.load
          tee_local 0
          get_local 1
          i32.load offset=16
          i32.lt_u
          if  ;; label = @4
            get_local 2
            get_local 0
            i32.const 1
            i32.add
            i32.store
            get_local 0
            i32.const 10
            i32.store8
            i32.const 0
            br 3 (;@1;)
          end
        end
        get_local 1
        i32.const 10
        call 14
        i32.const 31
        i32.shr_s
      end
    end
    set_local 0
    get_local 0)
  (func (;14;) (type 3) (param i32 i32) (result i32)
    (local i32 i32 i32 i32 i32)
    get_global 2
    set_local 2
    get_global 2
    i32.const 16
    i32.add
    set_global 2
    get_local 2
    get_local 1
    i32.const 255
    i32.and
    tee_local 6
    i32.store8
    block  ;; label = @1
      block  ;; label = @2
        get_local 0
        i32.const 16
        i32.add
        tee_local 3
        i32.load
        tee_local 4
        br_if 0 (;@2;)
        get_local 0
        call 7
        if  ;; label = @3
          i32.const -1
          set_local 1
        else
          get_local 3
          i32.load
          set_local 4
          br 1 (;@2;)
        end
        br 1 (;@1;)
      end
      get_local 0
      i32.const 20
      i32.add
      tee_local 3
      i32.load
      tee_local 5
      get_local 4
      i32.lt_u
      if  ;; label = @2
        get_local 1
        i32.const 255
        i32.and
        tee_local 1
        get_local 0
        i32.load8_s offset=75
        i32.ne
        if  ;; label = @3
          get_local 3
          get_local 5
          i32.const 1
          i32.add
          i32.store
          get_local 5
          get_local 6
          i32.store8
          br 2 (;@1;)
        end
      end
      get_local 0
      get_local 2
      i32.const 1
      get_local 0
      i32.load offset=36
      i32.const 3
      i32.and
      i32.const 2
      i32.add
      call_indirect (type 0)
      i32.const 1
      i32.eq
      if (result i32)  ;; label = @2
        get_local 2
        i32.load8_u
      else
        i32.const -1
      end
      set_local 1
    end
    get_local 2
    set_global 2
    get_local 1)
  (func (;15;) (type 4) (param i32 i32 i32 i32) (result i32)
    (local i32)
    get_local 2
    get_local 1
    i32.mul
    set_local 4
    get_local 3
    i32.load offset=76
    i32.const -1
    i32.gt_s
    if  ;; label = @1
      get_local 0
      get_local 4
      get_local 3
      call 6
      set_local 0
    else
      get_local 0
      get_local 4
      get_local 3
      call 6
      set_local 0
    end
    get_local 1
    i32.eqz
    if  ;; label = @1
      i32.const 0
      set_local 2
    end
    get_local 0
    get_local 4
    i32.ne
    if  ;; label = @1
      get_local 0
      get_local 1
      i32.div_u
      set_local 2
    end
    get_local 2)
  (func (;16;) (type 1) (param i32) (result i32)
    (local i32 i32 i32)
    block  ;; label = @1
      get_local 0
      tee_local 2
      i32.const 3
      i32.and
      if  ;; label = @2
        get_local 2
        set_local 1
        loop  ;; label = @3
          get_local 1
          i32.load8_s
          i32.eqz
          br_if 2 (;@1;)
          get_local 1
          i32.const 1
          i32.add
          tee_local 1
          tee_local 0
          i32.const 3
          i32.and
          br_if 0 (;@3;)
          get_local 1
          set_local 0
        end
      end
      loop  ;; label = @2
        get_local 0
        i32.const 4
        i32.add
        set_local 1
        get_local 0
        i32.load
        tee_local 3
        i32.const -2139062144
        i32.and
        i32.const -2139062144
        i32.xor
        get_local 3
        i32.const -16843009
        i32.add
        i32.and
        i32.eqz
        if  ;; label = @3
          get_local 1
          set_local 0
          br 1 (;@2;)
        end
      end
      get_local 3
      i32.const 255
      i32.and
      if  ;; label = @2
        loop  ;; label = @3
          get_local 0
          i32.const 1
          i32.add
          tee_local 0
          i32.load8_s
          br_if 0 (;@3;)
        end
      end
    end
    get_local 0
    get_local 2
    i32.sub)
  (func (;17;) (type 0) (param i32 i32 i32) (result i32)
    (local i32 i32)
    get_global 2
    set_local 3
    get_global 2
    i32.const 32
    i32.add
    set_global 2
    get_local 3
    i32.const 16
    i32.add
    set_local 4
    get_local 0
    i32.const 3
    i32.store offset=36
    get_local 0
    i32.load
    i32.const 64
    i32.and
    i32.eqz
    if  ;; label = @1
      get_local 3
      get_local 0
      i32.load offset=60
      i32.store
      get_local 3
      i32.const 21523
      i32.store offset=4
      get_local 3
      get_local 4
      i32.store offset=8
      i32.const 54
      get_local 3
      call 3
      if  ;; label = @2
        get_local 0
        i32.const -1
        i32.store8 offset=75
      end
    end
    get_local 0
    get_local 1
    get_local 2
    call 8
    set_local 0
    get_local 3
    set_global 2
    get_local 0)
  (func (;18;) (type 0) (param i32 i32 i32) (result i32)
    (local i32)
    get_global 2
    set_local 3
    get_global 2
    i32.const 32
    i32.add
    set_global 2
    get_local 3
    get_local 0
    i32.load offset=60
    i32.store
    get_local 3
    i32.const 0
    i32.store offset=4
    get_local 3
    get_local 1
    i32.store offset=8
    get_local 3
    get_local 3
    i32.const 20
    i32.add
    tee_local 0
    i32.store offset=12
    get_local 3
    get_local 2
    i32.store offset=16
    i32.const 140
    get_local 3
    call 1
    tee_local 1
    i32.const -4096
    i32.gt_u
    if (result i32)  ;; label = @1
      i32.const 1660
      i32.const 0
      get_local 1
      i32.sub
      i32.store
      i32.const -1
    else
      get_local 1
    end
    i32.const 0
    i32.lt_s
    if (result i32)  ;; label = @1
      get_local 0
      i32.const -1
      i32.store
      i32.const -1
    else
      get_local 0
      i32.load
    end
    set_local 0
    get_local 3
    set_global 2
    get_local 0)
  (func (;19;) (type 1) (param i32) (result i32)
    (local i32)
    get_global 2
    set_local 1
    get_global 2
    i32.const 16
    i32.add
    set_global 2
    get_local 1
    get_local 0
    i32.load offset=60
    i32.store
    i32.const 6
    get_local 1
    call 4
    tee_local 0
    i32.const -4096
    i32.gt_u
    if (result i32)  ;; label = @1
      i32.const 1660
      i32.const 0
      get_local 0
      i32.sub
      i32.store
      i32.const -1
    else
      get_local 0
    end
    set_local 0
    get_local 1
    set_global 2
    get_local 0)
  (func (;20;) (type 1) (param i32) (result i32)
    (local i32)
    get_global 2
    set_local 1
    get_global 2
    get_local 0
    i32.add
    set_global 2
    get_global 2
    i32.const 15
    i32.add
    i32.const -16
    i32.and
    set_global 2
    get_local 1)
  (global (;2;) (mut i32) (get_global 1))
  (export "_main" (func 9))
  (export "stackAlloc" (func 20))
  (elem (get_global 0) 11 19 10 17 18 8)
  (data (i32.const 1024) "\04\04\00\00\05")
  (data (i32.const 1040) "\01")
  (data (i32.const 1064) "\01\00\00\00\02\00\00\00\88\06\00\00\00\04")
  (data (i32.const 1088) "\01")
  (data (i32.const 1103) "\0a\ff\ff\ff\ff")
  (data (i32.const 1152) "Hello World"))
