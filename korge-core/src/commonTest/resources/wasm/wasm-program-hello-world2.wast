(module
 (type $FUNCSIG$iiii (func (param i32 i32 i32) (result i32)))
 (type $FUNCSIG$ii (func (param i32) (result i32)))
 (type $FUNCSIG$vii (func (param i32 i32)))
 (type $FUNCSIG$vi (func (param i32)))
 (type $FUNCSIG$i (func (result i32)))
 (type $FUNCSIG$iii (func (param i32 i32) (result i32)))
 (type $FUNCSIG$v (func))
 (import "env" "DYNAMICTOP_PTR" (global $DYNAMICTOP_PTR$asm2wasm$import i32))
 (import "env" "STACKTOP" (global $STACKTOP$asm2wasm$import i32))
 (import "env" "STACK_MAX" (global $STACK_MAX$asm2wasm$import i32))
 (import "env" "abort" (func $abort (param i32)))
 (import "env" "enlargeMemory" (func $enlargeMemory (result i32)))
 (import "env" "getTotalMemory" (func $getTotalMemory (result i32)))
 (import "env" "abortOnCannotGrowMemory" (func $abortOnCannotGrowMemory (result i32)))
 (import "env" "___lock" (func $___lock (param i32)))
 (import "env" "___syscall6" (func $___syscall6 (param i32 i32) (result i32)))
 (import "env" "___setErrNo" (func $___setErrNo (param i32)))
 (import "env" "_abort" (func $_abort))
 (import "env" "___syscall140" (func $___syscall140 (param i32 i32) (result i32)))
 (import "env" "_emscripten_memcpy_big" (func $_emscripten_memcpy_big (param i32 i32 i32) (result i32)))
 (import "env" "___syscall54" (func $___syscall54 (param i32 i32) (result i32)))
 (import "env" "___unlock" (func $___unlock (param i32)))
 (import "env" "___syscall146" (func $___syscall146 (param i32 i32) (result i32)))
 (import "env" "memory" (memory $0 256 256))
 (import "env" "table" (table 6 6 anyfunc))
 (import "env" "memoryBase" (global $memoryBase i32))
 (import "env" "tableBase" (global $tableBase i32))
 (global $DYNAMICTOP_PTR (mut i32) (get_global $DYNAMICTOP_PTR$asm2wasm$import))
 (global $STACKTOP (mut i32) (get_global $STACKTOP$asm2wasm$import))
 (global $STACK_MAX (mut i32) (get_global $STACK_MAX$asm2wasm$import))
 (global $__THREW__ (mut i32) (i32.const 0))
 (global $threwValue (mut i32) (i32.const 0))
 (global $tempRet0 (mut i32) (i32.const 0))
 (elem (get_global $tableBase) $b0 $___stdio_close $b1 $___stdout_write $___stdio_seek $___stdio_write)
 (data (i32.const 1212) ",\0f")
 (data (i32.const 1268) "\f8\04\00\00\05")
 (data (i32.const 1284) "\01")
 (data (i32.const 1308) "\01\00\00\00\02\00\00\00H\11\00\00\00\04")
 (data (i32.const 1332) "\01")
 (data (i32.const 1347) "\n\ff\ff\ff\ff")
 (data (i32.const 1396) "\f8\04\00\00%d,\00hello world\00T!\"\19\0d\01\02\03\11K\1c\0c\10\04\0b\1d\12\1e\'hnopqb \05\06\0f\13\14\15\1a\08\16\07($\17\18\t\n\0e\1b\1f%#\83\82}&*+<=>?CGJMXYZ[\\]^_`acdefgijklrstyz{|\00Illegal byte sequence\00Domain error\00Result not representable\00Not a tty\00Permission denied\00Operation not permitted\00No such file or directory\00No such process\00File exists\00Value too large for data type\00No space left on device\00Out of memory\00Resource busy\00Interrupted system call\00Resource temporarily unavailable\00Invalid seek\00Cross-device link\00Read-only file system\00Directory not empty\00Connection reset by peer\00Operation timed out\00Connection refused\00Host is down\00Host is unreachable\00Address in use\00Broken pipe\00I/O error\00No such device or address\00Block device required\00No such device\00Not a directory\00Is a directory\00Text file busy\00Exec format error\00Invalid argument\00Argument list too long\00Symbolic link loop\00Filename too long\00Too many open files in system\00No file descriptors available\00Bad file descriptor\00No child process\00Bad address\00File too large\00Too many links\00No locks available\00Resource deadlock would occur\00State not recoverable\00Previous owner died\00Operation canceled\00Function not implemented\00No message of desired type\00Identifier removed\00Device not a stream\00No data available\00Device timeout\00Out of streams resources\00Link has been severed\00Protocol error\00Bad message\00File descriptor in bad state\00Not a socket\00Destination address required\00Message too large\00Protocol wrong type for socket\00Protocol not available\00Protocol not supported\00Socket type not supported\00Not supported\00Protocol family not supported\00Address family not supported by protocol\00Address not available\00Network is down\00Network unreachable\00Connection reset by network\00Connection aborted\00No buffer space available\00Socket is connected\00Socket not connected\00Cannot send after socket shutdown\00Operation already in progress\00Operation in progress\00Stale file handle\00Remote I/O error\00Quota exceeded\00No medium found\00Wrong medium type\00No error information\00\00\11\00\n\00\11\11\11\00\00\00\00\05\00\00\00\00\00\00\t\00\00\00\00\0b")
 (data (i32.const 3340) "\11\00\0f\n\11\11\11\03\n\07\00\01\13\t\0b\0b\00\00\t\06\0b\00\00\0b\00\06\11\00\00\00\11\11\11")
 (data (i32.const 3389) "\0b")
 (data (i32.const 3398) "\11\00\n\n\11\11\11\00\n\00\00\02\00\t\0b\00\00\00\t\00\0b\00\00\0b")
 (data (i32.const 3447) "\0c")
 (data (i32.const 3459) "\0c\00\00\00\00\0c\00\00\00\00\t\0c\00\00\00\00\00\0c\00\00\0c")
 (data (i32.const 3505) "\0e")
 (data (i32.const 3517) "\0d\00\00\00\04\0d\00\00\00\00\t\0e\00\00\00\00\00\0e\00\00\0e")
 (data (i32.const 3563) "\10")
 (data (i32.const 3575) "\0f\00\00\00\00\0f\00\00\00\00\t\10\00\00\00\00\00\10\00\00\10\00\00\12\00\00\00\12\12\12")
 (data (i32.const 3630) "\12\00\00\00\12\12\12\00\00\00\00\00\00\t")
 (data (i32.const 3679) "\0b")
 (data (i32.const 3691) "\n\00\00\00\00\n\00\00\00\00\t\0b\00\00\00\00\00\0b\00\00\0b")
 (data (i32.const 3737) "\0c")
 (data (i32.const 3749) "\0c\00\00\00\00\0c\00\00\00\00\t\0c\00\00\00\00\00\0c\00\00\0c\00\00-+   0X0x\00(null)\00-0X+0X 0X-0x+0x 0x\00inf\00INF\00nan\00NAN\000123456789ABCDEF.")
 (export "_malloc" (func $_malloc))
 (export "getTempRet0" (func $getTempRet0))
 (export "_free" (func $_free))
 (export "_main" (func $_main))
 (export "setTempRet0" (func $setTempRet0))
 (export "establishStackSpace" (func $establishStackSpace))
 (export "stackSave" (func $stackSave))
 (export "_memset" (func $_memset))
 (export "_sbrk" (func $_sbrk))
 (export "_emscripten_get_global_libc" (func $_emscripten_get_global_libc))
 (export "_memcpy" (func $_memcpy))
 (export "___errno_location" (func $___errno_location))
 (export "setThrew" (func $setThrew))
 (export "_fflush" (func $_fflush))
 (export "stackAlloc" (func $stackAlloc))
 (export "stackRestore" (func $stackRestore))
 (export "_llvm_bswap_i32" (func $_llvm_bswap_i32))
 (export "runPostSets" (func $runPostSets))
 (export "dynCall_ii" (func $dynCall_ii))
 (export "dynCall_iiii" (func $dynCall_iiii))
 (func $stackAlloc (param $0 i32) (result i32)
  (local $1 i32)
  (set_local $1
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (get_local $0)
   )
  )
  (set_global $STACKTOP
   (i32.and
    (i32.add
     (get_global $STACKTOP)
     (i32.const 15)
    )
    (i32.const -16)
   )
  )
  (get_local $1)
 )
 (func $stackSave (result i32)
  (get_global $STACKTOP)
 )
 (func $stackRestore (param $0 i32)
  (set_global $STACKTOP
   (get_local $0)
  )
 )
 (func $establishStackSpace (param $0 i32) (param $1 i32)
  (set_global $STACKTOP
   (get_local $0)
  )
  (set_global $STACK_MAX
   (get_local $1)
  )
 )
 (func $setThrew (param $0 i32) (param $1 i32)
  (if
   (i32.eqz
    (get_global $__THREW__)
   )
   (block
    (set_global $__THREW__
     (get_local $0)
    )
    (set_global $threwValue
     (get_local $1)
    )
   )
  )
 )
 (func $setTempRet0 (param $0 i32)
  (set_global $tempRet0
   (get_local $0)
  )
 )
 (func $getTempRet0 (result i32)
  (get_global $tempRet0)
 )
 (func $_main (result i32)
  (local $0 i32)
  (local $1 i32)
  (local $2 i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (local $6 i32)
  (local $7 i32)
  (local $8 i32)
  (local $9 i32)
  (local $10 i32)
  (set_local $0
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 80)
   )
  )
  ;;@ wasm-program.c:4:0
  (set_local $1
   (i32.add
    (get_local $0)
    (i32.const 72)
   )
  )
  (set_local $2
   (i32.add
    (get_local $0)
    (i32.const 64)
   )
  )
  (set_local $3
   (i32.add
    (get_local $0)
    (i32.const 56)
   )
  )
  (set_local $4
   (i32.add
    (get_local $0)
    (i32.const 48)
   )
  )
  (set_local $5
   (i32.add
    (get_local $0)
    (i32.const 40)
   )
  )
  (set_local $6
   (i32.add
    (get_local $0)
    (i32.const 32)
   )
  )
  (set_local $7
   (i32.add
    (get_local $0)
    (i32.const 24)
   )
  )
  (set_local $8
   (i32.add
    (get_local $0)
    (i32.const 16)
   )
  )
  (set_local $9
   (i32.add
    (get_local $0)
    (i32.const 8)
   )
  )
  (set_local $10
   (get_local $0)
  )
  (drop
   (call $_puts
    (i32.const 1404)
   )
  )
  ;;@ wasm-program.c:5:0
  (i32.store
   (get_local $10)
   (i32.const 0)
  )
  (drop
   (call $_printf
    (i32.const 1400)
    (get_local $10)
   )
  )
  (i32.store
   (get_local $9)
   (i32.const 1)
  )
  (drop
   (call $_printf
    (i32.const 1400)
    (get_local $9)
   )
  )
  (i32.store
   (get_local $8)
   (i32.const 2)
  )
  (drop
   (call $_printf
    (i32.const 1400)
    (get_local $8)
   )
  )
  (i32.store
   (get_local $7)
   (i32.const 3)
  )
  (drop
   (call $_printf
    (i32.const 1400)
    (get_local $7)
   )
  )
  (i32.store
   (get_local $6)
   (i32.const 4)
  )
  (drop
   (call $_printf
    (i32.const 1400)
    (get_local $6)
   )
  )
  (i32.store
   (get_local $5)
   (i32.const 5)
  )
  (drop
   (call $_printf
    (i32.const 1400)
    (get_local $5)
   )
  )
  (i32.store
   (get_local $4)
   (i32.const 6)
  )
  (drop
   (call $_printf
    (i32.const 1400)
    (get_local $4)
   )
  )
  (i32.store
   (get_local $3)
   (i32.const 7)
  )
  (drop
   (call $_printf
    (i32.const 1400)
    (get_local $3)
   )
  )
  (i32.store
   (get_local $2)
   (i32.const 8)
  )
  (drop
   (call $_printf
    (i32.const 1400)
    (get_local $2)
   )
  )
  (i32.store
   (get_local $1)
   (i32.const 9)
  )
  (drop
   (call $_printf
    (i32.const 1400)
    (get_local $1)
   )
  )
  ;;@ wasm-program.c:6:0
  (drop
   (call $_putchar
    (i32.const 10)
   )
  )
  (set_global $STACKTOP
   (get_local $0)
  )
  ;;@ wasm-program.c:7:0
  (i32.const 0)
 )
 (func $_emscripten_get_global_libc (result i32)
  (i32.const 3844)
 )
 (func $___stdio_close (param $0 i32) (result i32)
  (local $1 i32)
  (local $2 i32)
  (set_local $1
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 16)
   )
  )
  (i32.store
   (tee_local $2
    (get_local $1)
   )
   (call $_dummy_243
    (i32.load offset=60
     (get_local $0)
    )
   )
  )
  (set_local $0
   (call $___syscall_ret
    (call $___syscall6
     (i32.const 6)
     (get_local $2)
    )
   )
  )
  (set_global $STACKTOP
   (get_local $1)
  )
  (get_local $0)
 )
 (func $___stdio_seek (param $0 i32) (param $1 i32) (param $2 i32) (result i32)
  (local $3 i32)
  (local $4 i32)
  (set_local $4
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 32)
   )
  )
  (i32.store
   (tee_local $3
    (get_local $4)
   )
   (i32.load offset=60
    (get_local $0)
   )
  )
  (i32.store offset=4
   (get_local $3)
   (i32.const 0)
  )
  (i32.store offset=8
   (get_local $3)
   (get_local $1)
  )
  (i32.store offset=12
   (get_local $3)
   (tee_local $0
    (i32.add
     (get_local $4)
     (i32.const 20)
    )
   )
  )
  (i32.store offset=16
   (get_local $3)
   (get_local $2)
  )
  (set_local $0
   (if (result i32)
    (i32.lt_s
     (call $___syscall_ret
      (call $___syscall140
       (i32.const 140)
       (get_local $3)
      )
     )
     (i32.const 0)
    )
    (block (result i32)
     (i32.store
      (get_local $0)
      (i32.const -1)
     )
     (i32.const -1)
    )
    (i32.load
     (get_local $0)
    )
   )
  )
  (set_global $STACKTOP
   (get_local $4)
  )
  (get_local $0)
 )
 (func $___syscall_ret (param $0 i32) (result i32)
  (if (result i32)
   (i32.gt_u
    (get_local $0)
    (i32.const -4096)
   )
   (block (result i32)
    (i32.store
     (call $___errno_location)
     (i32.sub
      (i32.const 0)
      (get_local $0)
     )
    )
    (i32.const -1)
   )
   (get_local $0)
  )
 )
 (func $___errno_location (result i32)
  (i32.add
   (call $___pthread_self_304)
   (i32.const 64)
  )
 )
 (func $___pthread_self_304 (result i32)
  (call $_pthread_self)
 )
 (func $_pthread_self (result i32)
  (i32.const 1024)
 )
 (func $_dummy_243 (param $0 i32) (result i32)
  (get_local $0)
 )
 (func $___stdout_write (param $0 i32) (param $1 i32) (param $2 i32) (result i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (set_local $4
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 32)
   )
  )
  (set_local $3
   (get_local $4)
  )
  (set_local $5
   (i32.add
    (get_local $4)
    (i32.const 16)
   )
  )
  (i32.store offset=36
   (get_local $0)
   (i32.const 3)
  )
  (if
   (i32.eqz
    (i32.and
     (i32.load
      (get_local $0)
     )
     (i32.const 64)
    )
   )
   (block
    (i32.store
     (get_local $3)
     (i32.load offset=60
      (get_local $0)
     )
    )
    (i32.store offset=4
     (get_local $3)
     (i32.const 21523)
    )
    (i32.store offset=8
     (get_local $3)
     (get_local $5)
    )
    (if
     (call $___syscall54
      (i32.const 54)
      (get_local $3)
     )
     (i32.store8 offset=75
      (get_local $0)
      (i32.const -1)
     )
    )
   )
  )
  (set_local $0
   (call $___stdio_write
    (get_local $0)
    (get_local $1)
    (get_local $2)
   )
  )
  (set_global $STACKTOP
   (get_local $4)
  )
  (get_local $0)
 )
 (func $___stdio_write (param $0 i32) (param $1 i32) (param $2 i32) (result i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (local $6 i32)
  (local $7 i32)
  (local $8 i32)
  (local $9 i32)
  (local $10 i32)
  (local $11 i32)
  (local $12 i32)
  (local $13 i32)
  (set_local $6
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 48)
   )
  )
  (set_local $7
   (i32.add
    (get_local $6)
    (i32.const 16)
   )
  )
  (i32.store
   (tee_local $3
    (i32.add
     (get_local $6)
     (i32.const 32)
    )
   )
   (tee_local $4
    (i32.load
     (tee_local $9
      (i32.add
       (get_local $0)
       (i32.const 28)
      )
     )
    )
   )
  )
  (i32.store offset=4
   (get_local $3)
   (tee_local $4
    (i32.sub
     (i32.load
      (tee_local $10
       (i32.add
        (get_local $0)
        (i32.const 20)
       )
      )
     )
     (get_local $4)
    )
   )
  )
  (i32.store offset=8
   (get_local $3)
   (get_local $1)
  )
  (i32.store offset=12
   (get_local $3)
   (get_local $2)
  )
  (set_local $4
   (i32.add
    (get_local $4)
    (get_local $2)
   )
  )
  (i32.store
   (tee_local $1
    (get_local $6)
   )
   (i32.load
    (tee_local $12
     (i32.add
      (get_local $0)
      (i32.const 60)
     )
    )
   )
  )
  (i32.store offset=4
   (get_local $1)
   (get_local $3)
  )
  (i32.store offset=8
   (get_local $1)
   (i32.const 2)
  )
  (set_local $5
   (call $___syscall_ret
    (call $___syscall146
     (i32.const 146)
     (get_local $1)
    )
   )
  )
  (block $__rjto$0
   (block $__rjti$0
    (br_if $__rjti$0
     (i32.eq
      (get_local $4)
      (get_local $5)
     )
    )
    (set_local $8
     (i32.const 2)
    )
    (set_local $1
     (get_local $3)
    )
    (set_local $3
     (get_local $5)
    )
    (loop $while-in
     (if
      (i32.ge_s
       (get_local $3)
       (i32.const 0)
      )
      (block
       (set_local $4
        (i32.sub
         (get_local $4)
         (get_local $3)
        )
       )
       (set_local $5
        (i32.add
         (get_local $1)
         (i32.const 8)
        )
       )
       (if
        (tee_local $11
         (i32.gt_u
          (get_local $3)
          (tee_local $13
           (i32.load offset=4
            (get_local $1)
           )
          )
         )
        )
        (set_local $1
         (get_local $5)
        )
       )
       (set_local $8
        (i32.add
         (i32.shr_s
          (i32.shl
           (get_local $11)
           (i32.const 31)
          )
          (i32.const 31)
         )
         (get_local $8)
        )
       )
       (i32.store
        (get_local $1)
        (i32.add
         (i32.load
          (get_local $1)
         )
         (tee_local $3
          (i32.sub
           (get_local $3)
           (if (result i32)
            (get_local $11)
            (get_local $13)
            (i32.const 0)
           )
          )
         )
        )
       )
       (i32.store
        (tee_local $5
         (i32.add
          (get_local $1)
          (i32.const 4)
         )
        )
        (i32.sub
         (i32.load
          (get_local $5)
         )
         (get_local $3)
        )
       )
       (i32.store
        (get_local $7)
        (i32.load
         (get_local $12)
        )
       )
       (i32.store offset=4
        (get_local $7)
        (get_local $1)
       )
       (i32.store offset=8
        (get_local $7)
        (get_local $8)
       )
       (set_local $3
        (call $___syscall_ret
         (call $___syscall146
          (i32.const 146)
          (get_local $7)
         )
        )
       )
       (br_if $__rjti$0
        (i32.eq
         (get_local $4)
         (get_local $3)
        )
       )
       (br $while-in)
      )
     )
    )
    (i32.store offset=16
     (get_local $0)
     (i32.const 0)
    )
    (i32.store
     (get_local $9)
     (i32.const 0)
    )
    (i32.store
     (get_local $10)
     (i32.const 0)
    )
    (i32.store
     (get_local $0)
     (i32.or
      (i32.load
       (get_local $0)
      )
      (i32.const 32)
     )
    )
    (set_local $2
     (if (result i32)
      (i32.eq
       (get_local $8)
       (i32.const 2)
      )
      (i32.const 0)
      (i32.sub
       (get_local $2)
       (i32.load offset=4
        (get_local $1)
       )
      )
     )
    )
    (br $__rjto$0)
   )
   (i32.store offset=16
    (get_local $0)
    (i32.add
     (tee_local $1
      (i32.load offset=44
       (get_local $0)
      )
     )
     (i32.load offset=48
      (get_local $0)
     )
    )
   )
   (i32.store
    (get_local $9)
    (get_local $1)
   )
   (i32.store
    (get_local $10)
    (get_local $1)
   )
  )
  (set_global $STACKTOP
   (get_local $6)
  )
  (get_local $2)
 )
 (func $_strlen (param $0 i32) (result i32)
  (local $1 i32)
  (local $2 i32)
  (local $3 i32)
  (block $__rjto$0
   (if
    (i32.and
     (tee_local $2
      (get_local $0)
     )
     (i32.const 3)
    )
    (block
     (set_local $1
      (get_local $0)
     )
     (set_local $0
      (get_local $2)
     )
     (loop $while-in
      (br_if $__rjto$0
       (i32.eqz
        (i32.load8_s
         (get_local $1)
        )
       )
      )
      (br_if $while-in
       (i32.and
        (tee_local $0
         (tee_local $1
          (i32.add
           (get_local $1)
           (i32.const 1)
          )
         )
        )
        (i32.const 3)
       )
      )
      (set_local $0
       (get_local $1)
      )
     )
    )
   )
   (loop $while-in1
    (set_local $1
     (i32.add
      (get_local $0)
      (i32.const 4)
     )
    )
    (if
     (i32.eqz
      (i32.and
       (i32.xor
        (i32.and
         (tee_local $3
          (i32.load
           (get_local $0)
          )
         )
         (i32.const -2139062144)
        )
        (i32.const -2139062144)
       )
       (i32.add
        (get_local $3)
        (i32.const -16843009)
       )
      )
     )
     (block
      (set_local $0
       (get_local $1)
      )
      (br $while-in1)
     )
    )
   )
   (if
    (i32.and
     (get_local $3)
     (i32.const 255)
    )
    (loop $while-in3
     (br_if $while-in3
      (i32.load8_s
       (tee_local $0
        (i32.add
         (get_local $0)
         (i32.const 1)
        )
       )
      )
     )
    )
   )
  )
  (i32.sub
   (get_local $0)
   (get_local $2)
  )
 )
 (func $_strerror (param $0 i32) (result i32)
  (call $___strerror_l
   (get_local $0)
   (i32.load offset=188
    (call $___pthread_self_304)
   )
  )
 )
 (func $___strerror_l (param $0 i32) (param $1 i32) (result i32)
  (local $2 i32)
  (local $3 i32)
  (set_local $2
   (i32.const 0)
  )
  (block $__rjto$1
   (block $__rjti$1
    (block $__rjti$0
     (loop $while-in
      (block $while-out
       (br_if $__rjti$0
        (i32.eq
         (i32.load8_u
          (i32.add
           (get_local $2)
           (i32.const 1416)
          )
         )
         (get_local $0)
        )
       )
       (br_if $while-in
        (i32.ne
         (tee_local $2
          (i32.add
           (get_local $2)
           (i32.const 1)
          )
         )
         (i32.const 87)
        )
       )
       (set_local $0
        (i32.const 1504)
       )
       (set_local $2
        (i32.const 87)
       )
       (br $__rjti$1)
      )
     )
    )
    (if
     (get_local $2)
     (block
      (set_local $0
       (i32.const 1504)
      )
      (br $__rjti$1)
     )
     (set_local $0
      (i32.const 1504)
     )
    )
    (br $__rjto$1)
   )
   (loop $while-in1
    (set_local $3
     (get_local $0)
    )
    (loop $while-in3
     (set_local $0
      (i32.add
       (get_local $3)
       (i32.const 1)
      )
     )
     (if
      (i32.load8_s
       (get_local $3)
      )
      (block
       (set_local $3
        (get_local $0)
       )
       (br $while-in3)
      )
     )
    )
    (br_if $while-in1
     (tee_local $2
      (i32.add
       (get_local $2)
       (i32.const -1)
      )
     )
    )
   )
  )
  (call $___lctrans
   (get_local $0)
   (i32.load offset=20
    (get_local $1)
   )
  )
 )
 (func $___lctrans (param $0 i32) (param $1 i32) (result i32)
  (call $___lctrans_impl
   (get_local $0)
   (get_local $1)
  )
 )
 (func $___lctrans_impl (param $0 i32) (param $1 i32) (result i32)
  (local $2 i32)
  (if (result i32)
   (tee_local $2
    (if (result i32)
     (get_local $1)
     (call $___mo_lookup
      (i32.load
       (get_local $1)
      )
      (i32.load offset=4
       (get_local $1)
      )
      (get_local $0)
     )
     (i32.const 0)
    )
   )
   (get_local $2)
   (get_local $0)
  )
 )
 (func $___mo_lookup (param $0 i32) (param $1 i32) (param $2 i32) (result i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (local $6 i32)
  (local $7 i32)
  (local $8 i32)
  (local $9 i32)
  (local $10 i32)
  (local $11 i32)
  (local $12 i32)
  (set_local $4
   (call $_swapc
    (i32.load offset=8
     (get_local $0)
    )
    (tee_local $6
     (i32.add
      (i32.load
       (get_local $0)
      )
      (i32.const 1794895138)
     )
    )
   )
  )
  (set_local $3
   (call $_swapc
    (i32.load offset=12
     (get_local $0)
    )
    (get_local $6)
   )
  )
  (set_local $7
   (call $_swapc
    (i32.load offset=16
     (get_local $0)
    )
    (get_local $6)
   )
  )
  (block $label$break$L1
   (if
    (i32.lt_u
     (get_local $4)
     (i32.shr_u
      (get_local $1)
      (i32.const 2)
     )
    )
    (if
     (i32.and
      (i32.lt_u
       (get_local $3)
       (tee_local $5
        (i32.sub
         (get_local $1)
         (i32.shl
          (get_local $4)
          (i32.const 2)
         )
        )
       )
      )
      (i32.lt_u
       (get_local $7)
       (get_local $5)
      )
     )
     (if
      (i32.and
       (i32.or
        (get_local $7)
        (get_local $3)
       )
       (i32.const 3)
      )
      (set_local $1
       (i32.const 0)
      )
      (block
       (set_local $10
        (i32.shr_u
         (get_local $3)
         (i32.const 2)
        )
       )
       (set_local $11
        (i32.shr_u
         (get_local $7)
         (i32.const 2)
        )
       )
       (set_local $5
        (i32.const 0)
       )
       (loop $while-in
        (block $while-out
         (set_local $9
          (call $_swapc
           (i32.load
            (i32.add
             (get_local $0)
             (i32.shl
              (tee_local $3
               (i32.add
                (tee_local $8
                 (i32.shl
                  (tee_local $12
                   (i32.add
                    (get_local $5)
                    (tee_local $7
                     (i32.shr_u
                      (get_local $4)
                      (i32.const 1)
                     )
                    )
                   )
                  )
                  (i32.const 1)
                 )
                )
                (get_local $10)
               )
              )
              (i32.const 2)
             )
            )
           )
           (get_local $6)
          )
         )
         (if
          (i32.eqz
           (i32.and
            (i32.lt_u
             (tee_local $3
              (call $_swapc
               (i32.load
                (i32.add
                 (get_local $0)
                 (i32.shl
                  (i32.add
                   (get_local $3)
                   (i32.const 1)
                  )
                  (i32.const 2)
                 )
                )
               )
               (get_local $6)
              )
             )
             (get_local $1)
            )
            (i32.lt_u
             (get_local $9)
             (i32.sub
              (get_local $1)
              (get_local $3)
             )
            )
           )
          )
          (block
           (set_local $1
            (i32.const 0)
           )
           (br $label$break$L1)
          )
         )
         (if
          (i32.load8_s
           (i32.add
            (get_local $0)
            (i32.add
             (get_local $3)
             (get_local $9)
            )
           )
          )
          (block
           (set_local $1
            (i32.const 0)
           )
           (br $label$break$L1)
          )
         )
         (br_if $while-out
          (i32.eqz
           (tee_local $3
            (call $_strcmp
             (get_local $2)
             (i32.add
              (get_local $0)
              (get_local $3)
             )
            )
           )
          )
         )
         (set_local $8
          (i32.eq
           (get_local $4)
           (i32.const 1)
          )
         )
         (set_local $4
          (i32.sub
           (get_local $4)
           (get_local $7)
          )
         )
         (if
          (tee_local $3
           (i32.lt_s
            (get_local $3)
            (i32.const 0)
           )
          )
          (set_local $4
           (get_local $7)
          )
         )
         (if
          (i32.eqz
           (get_local $3)
          )
          (set_local $5
           (get_local $12)
          )
         )
         (br_if $while-in
          (i32.eqz
           (get_local $8)
          )
         )
         (set_local $1
          (i32.const 0)
         )
         (br $label$break$L1)
        )
       )
       (set_local $5
        (call $_swapc
         (i32.load
          (i32.add
           (get_local $0)
           (i32.shl
            (tee_local $2
             (i32.add
              (get_local $8)
              (get_local $11)
             )
            )
            (i32.const 2)
           )
          )
         )
         (get_local $6)
        )
       )
       (if
        (i32.and
         (i32.lt_u
          (tee_local $2
           (call $_swapc
            (i32.load
             (i32.add
              (get_local $0)
              (i32.shl
               (i32.add
                (get_local $2)
                (i32.const 1)
               )
               (i32.const 2)
              )
             )
            )
            (get_local $6)
           )
          )
          (get_local $1)
         )
         (i32.lt_u
          (get_local $5)
          (i32.sub
           (get_local $1)
           (get_local $2)
          )
         )
        )
        (block
         (set_local $1
          (i32.add
           (get_local $0)
           (get_local $2)
          )
         )
         (if
          (i32.load8_s
           (i32.add
            (get_local $0)
            (i32.add
             (get_local $2)
             (get_local $5)
            )
           )
          )
          (set_local $1
           (i32.const 0)
          )
         )
        )
        (set_local $1
         (i32.const 0)
        )
       )
      )
     )
     (set_local $1
      (i32.const 0)
     )
    )
    (set_local $1
     (i32.const 0)
    )
   )
  )
  (get_local $1)
 )
 (func $_swapc (param $0 i32) (param $1 i32) (result i32)
  (local $2 i32)
  (set_local $2
   (call $_llvm_bswap_i32
    (get_local $0)
   )
  )
  (if (result i32)
   (get_local $1)
   (get_local $2)
   (get_local $0)
  )
 )
 (func $_strcmp (param $0 i32) (param $1 i32) (result i32)
  (local $2 i32)
  (local $3 i32)
  (if
   (i32.or
    (i32.eqz
     (tee_local $2
      (i32.load8_s
       (get_local $0)
      )
     )
    )
    (i32.ne
     (get_local $2)
     (tee_local $3
      (i32.load8_s
       (get_local $1)
      )
     )
    )
   )
   (block
    (set_local $0
     (get_local $3)
    )
    (set_local $1
     (get_local $2)
    )
   )
   (loop $while-in
    (if
     (i32.or
      (i32.eqz
       (tee_local $2
        (i32.load8_s
         (tee_local $0
          (i32.add
           (get_local $0)
           (i32.const 1)
          )
         )
        )
       )
      )
      (i32.ne
       (get_local $2)
       (tee_local $3
        (i32.load8_s
         (tee_local $1
          (i32.add
           (get_local $1)
           (i32.const 1)
          )
         )
        )
       )
      )
     )
     (block
      (set_local $0
       (get_local $3)
      )
      (set_local $1
       (get_local $2)
      )
     )
     (br $while-in)
    )
   )
  )
  (i32.sub
   (i32.and
    (get_local $1)
    (i32.const 255)
   )
   (i32.and
    (get_local $0)
    (i32.const 255)
   )
  )
 )
 (func $_memchr (param $0 i32) (param $1 i32) (param $2 i32) (result i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (set_local $4
   (i32.and
    (get_local $1)
    (i32.const 255)
   )
  )
  (block $label$break$L8
   (block $__rjti$2
    (if
     (i32.and
      (tee_local $3
       (i32.ne
        (get_local $2)
        (i32.const 0)
       )
      )
      (i32.ne
       (i32.and
        (get_local $0)
        (i32.const 3)
       )
       (i32.const 0)
      )
     )
     (block
      (set_local $5
       (i32.and
        (get_local $1)
        (i32.const 255)
       )
      )
      (loop $while-in
       (br_if $__rjti$2
        (i32.eq
         (i32.load8_u
          (get_local $0)
         )
         (get_local $5)
        )
       )
       (br_if $while-in
        (i32.and
         (tee_local $3
          (i32.ne
           (tee_local $2
            (i32.add
             (get_local $2)
             (i32.const -1)
            )
           )
           (i32.const 0)
          )
         )
         (i32.ne
          (i32.and
           (tee_local $0
            (i32.add
             (get_local $0)
             (i32.const 1)
            )
           )
           (i32.const 3)
          )
          (i32.const 0)
         )
        )
       )
      )
     )
    )
    (br_if $__rjti$2
     (get_local $3)
    )
    (set_local $1
     (i32.const 0)
    )
    (br $label$break$L8)
   )
   (if
    (i32.eq
     (i32.load8_u
      (get_local $0)
     )
     (tee_local $3
      (i32.and
       (get_local $1)
       (i32.const 255)
      )
     )
    )
    (set_local $1
     (get_local $2)
    )
    (block
     (set_local $4
      (i32.mul
       (get_local $4)
       (i32.const 16843009)
      )
     )
     (block $__rjto$0
      (block $__rjti$0
       (if
        (i32.gt_u
         (get_local $2)
         (i32.const 3)
        )
        (block
         (set_local $1
          (get_local $2)
         )
         (loop $while-in3
          (if
           (i32.eqz
            (i32.and
             (i32.xor
              (i32.and
               (tee_local $2
                (i32.xor
                 (i32.load
                  (get_local $0)
                 )
                 (get_local $4)
                )
               )
               (i32.const -2139062144)
              )
              (i32.const -2139062144)
             )
             (i32.add
              (get_local $2)
              (i32.const -16843009)
             )
            )
           )
           (block
            (set_local $0
             (i32.add
              (get_local $0)
              (i32.const 4)
             )
            )
            (br_if $while-in3
             (i32.gt_u
              (tee_local $1
               (i32.add
                (get_local $1)
                (i32.const -4)
               )
              )
              (i32.const 3)
             )
            )
            (br $__rjti$0)
           )
          )
         )
        )
        (block
         (set_local $1
          (get_local $2)
         )
         (br $__rjti$0)
        )
       )
       (br $__rjto$0)
      )
      (if
       (i32.eqz
        (get_local $1)
       )
       (block
        (set_local $1
         (i32.const 0)
        )
        (br $label$break$L8)
       )
      )
     )
     (loop $while-in5
      (br_if $label$break$L8
       (i32.eq
        (i32.load8_u
         (get_local $0)
        )
        (get_local $3)
       )
      )
      (set_local $0
       (i32.add
        (get_local $0)
        (i32.const 1)
       )
      )
      (br_if $while-in5
       (tee_local $1
        (i32.add
         (get_local $1)
         (i32.const -1)
        )
       )
      )
      (set_local $1
       (i32.const 0)
      )
     )
    )
   )
  )
  (if (result i32)
   (get_local $1)
   (get_local $0)
   (i32.const 0)
  )
 )
 (func $_vfprintf (param $0 i32) (param $1 i32) (param $2 i32) (result i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (local $6 i32)
  (local $7 i32)
  (local $8 i32)
  (local $9 i32)
  (local $10 i32)
  (local $11 i32)
  (local $12 i32)
  (local $13 i32)
  (local $14 i32)
  (set_local $4
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 224)
   )
  )
  (set_local $5
   (i32.add
    (get_local $4)
    (i32.const 136)
   )
  )
  (i64.store align=4
   (tee_local $3
    (i32.add
     (get_local $4)
     (i32.const 80)
    )
   )
   (i64.const 0)
  )
  (i64.store offset=8 align=4
   (get_local $3)
   (i64.const 0)
  )
  (i64.store offset=16 align=4
   (get_local $3)
   (i64.const 0)
  )
  (i64.store offset=24 align=4
   (get_local $3)
   (i64.const 0)
  )
  (i64.store offset=32 align=4
   (get_local $3)
   (i64.const 0)
  )
  (i32.store
   (tee_local $6
    (i32.add
     (get_local $4)
     (i32.const 120)
    )
   )
   (i32.load
    (get_local $2)
   )
  )
  (if
   (i32.lt_s
    (call $_printf_core
     (i32.const 0)
     (get_local $1)
     (get_local $6)
     (tee_local $2
      (get_local $4)
     )
     (get_local $3)
    )
    (i32.const 0)
   )
   (set_local $1
    (i32.const -1)
   )
   (block
    (set_local $12
     (if (result i32)
      (i32.gt_s
       (i32.load offset=76
        (get_local $0)
       )
       (i32.const -1)
      )
      (call $___lockfile
       (get_local $0)
      )
      (i32.const 0)
     )
    )
    (set_local $7
     (i32.load
      (get_local $0)
     )
    )
    (if
     (i32.lt_s
      (i32.load8_s offset=74
       (get_local $0)
      )
      (i32.const 1)
     )
     (i32.store
      (get_local $0)
      (i32.and
       (get_local $7)
       (i32.const -33)
      )
     )
    )
    (if
     (i32.load
      (tee_local $8
       (i32.add
        (get_local $0)
        (i32.const 48)
       )
      )
     )
     (set_local $1
      (call $_printf_core
       (get_local $0)
       (get_local $1)
       (get_local $6)
       (get_local $2)
       (get_local $3)
      )
     )
     (block
      (set_local $10
       (i32.load
        (tee_local $9
         (i32.add
          (get_local $0)
          (i32.const 44)
         )
        )
       )
      )
      (i32.store
       (get_local $9)
       (get_local $5)
      )
      (i32.store
       (tee_local $13
        (i32.add
         (get_local $0)
         (i32.const 28)
        )
       )
       (get_local $5)
      )
      (i32.store
       (tee_local $11
        (i32.add
         (get_local $0)
         (i32.const 20)
        )
       )
       (get_local $5)
      )
      (i32.store
       (get_local $8)
       (i32.const 80)
      )
      (i32.store
       (tee_local $14
        (i32.add
         (get_local $0)
         (i32.const 16)
        )
       )
       (i32.add
        (get_local $5)
        (i32.const 80)
       )
      )
      (set_local $1
       (call $_printf_core
        (get_local $0)
        (get_local $1)
        (get_local $6)
        (get_local $2)
        (get_local $3)
       )
      )
      (if
       (get_local $10)
       (block
        (drop
         (call_indirect $FUNCSIG$iiii
          (get_local $0)
          (i32.const 0)
          (i32.const 0)
          (i32.add
           (i32.and
            (i32.load offset=36
             (get_local $0)
            )
            (i32.const 3)
           )
           (i32.const 2)
          )
         )
        )
        (if
         (i32.eqz
          (i32.load
           (get_local $11)
          )
         )
         (set_local $1
          (i32.const -1)
         )
        )
        (i32.store
         (get_local $9)
         (get_local $10)
        )
        (i32.store
         (get_local $8)
         (i32.const 0)
        )
        (i32.store
         (get_local $14)
         (i32.const 0)
        )
        (i32.store
         (get_local $13)
         (i32.const 0)
        )
        (i32.store
         (get_local $11)
         (i32.const 0)
        )
       )
      )
     )
    )
    (i32.store
     (get_local $0)
     (i32.or
      (tee_local $2
       (i32.load
        (get_local $0)
       )
      )
      (i32.and
       (get_local $7)
       (i32.const 32)
      )
     )
    )
    (if
     (get_local $12)
     (call $___unlockfile
      (get_local $0)
     )
    )
    (if
     (i32.and
      (get_local $2)
      (i32.const 32)
     )
     (set_local $1
      (i32.const -1)
     )
    )
   )
  )
  (set_global $STACKTOP
   (get_local $4)
  )
  (get_local $1)
 )
 (func $_printf_core (param $0 i32) (param $1 i32) (param $2 i32) (param $3 i32) (param $4 i32) (result i32)
  (local $5 i32)
  (local $6 i32)
  (local $7 i32)
  (local $8 i32)
  (local $9 i32)
  (local $10 i32)
  (local $11 i32)
  (local $12 i32)
  (local $13 i32)
  (local $14 i32)
  (local $15 i32)
  (local $16 i32)
  (local $17 i64)
  (local $18 i32)
  (local $19 i32)
  (local $20 i32)
  (local $21 i32)
  (local $22 i32)
  (local $23 i32)
  (local $24 i32)
  (local $25 i32)
  (local $26 i32)
  (local $27 i32)
  (set_local $20
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 64)
   )
  )
  (set_local $12
   (get_local $20)
  )
  (set_local $22
   (i32.add
    (get_local $20)
    (i32.const 20)
   )
  )
  (i32.store
   (tee_local $15
    (i32.add
     (get_local $20)
     (i32.const 16)
    )
   )
   (get_local $1)
  )
  (set_local $21
   (i32.ne
    (get_local $0)
    (i32.const 0)
   )
  )
  (set_local $24
   (tee_local $18
    (i32.add
     (tee_local $13
      (i32.add
       (get_local $20)
       (i32.const 24)
      )
     )
     (i32.const 40)
    )
   )
  )
  (set_local $25
   (i32.add
    (get_local $13)
    (i32.const 39)
   )
  )
  (set_local $27
   (i32.add
    (tee_local $23
     (i32.add
      (get_local $20)
      (i32.const 8)
     )
    )
    (i32.const 4)
   )
  )
  (set_local $5
   (i32.const 0)
  )
  (set_local $11
   (i32.const 0)
  )
  (set_local $13
   (i32.const 0)
  )
  (block $label$break$L114
   (block $__rjti$9
    (loop $label$continue$L1
     (block $label$break$L1
      (if
       (i32.gt_s
        (get_local $11)
        (i32.const -1)
       )
       (set_local $11
        (if (result i32)
         (i32.gt_s
          (get_local $5)
          (i32.sub
           (i32.const 2147483647)
           (get_local $11)
          )
         )
         (block (result i32)
          (i32.store
           (call $___errno_location)
           (i32.const 75)
          )
          (i32.const -1)
         )
         (i32.add
          (get_local $5)
          (get_local $11)
         )
        )
       )
      )
      (br_if $__rjti$9
       (i32.eqz
        (tee_local $5
         (i32.load8_s
          (get_local $1)
         )
        )
       )
      )
      (set_local $7
       (get_local $1)
      )
      (block $label$break$L12
       (block $__rjti$1
        (loop $label$continue$L9
         (block $label$break$L9
          (block $switch-default
           (block $switch-case0
            (block $switch-case
             (br_table $switch-case0 $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-case $switch-default
              (i32.shr_s
               (i32.shl
                (get_local $5)
                (i32.const 24)
               )
               (i32.const 24)
              )
             )
            )
            (set_local $5
             (get_local $7)
            )
            (br $__rjti$1)
           )
           (set_local $5
            (get_local $7)
           )
           (br $label$break$L9)
          )
          (i32.store
           (get_local $15)
           (tee_local $7
            (i32.add
             (get_local $7)
             (i32.const 1)
            )
           )
          )
          (set_local $5
           (i32.load8_s
            (get_local $7)
           )
          )
          (br $label$continue$L9)
         )
        )
        (br $label$break$L12)
       )
       (loop $while-in
        (br_if $label$break$L12
         (i32.ne
          (i32.load8_s offset=1
           (get_local $7)
          )
          (i32.const 37)
         )
        )
        (set_local $5
         (i32.add
          (get_local $5)
          (i32.const 1)
         )
        )
        (i32.store
         (get_local $15)
         (tee_local $7
          (i32.add
           (get_local $7)
           (i32.const 2)
          )
         )
        )
        (br_if $while-in
         (i32.eq
          (i32.load8_s
           (get_local $7)
          )
          (i32.const 37)
         )
        )
       )
      )
      (set_local $5
       (i32.sub
        (get_local $5)
        (get_local $1)
       )
      )
      (if
       (get_local $21)
       (call $_out
        (get_local $0)
        (get_local $1)
        (get_local $5)
       )
      )
      (if
       (get_local $5)
       (block
        (set_local $1
         (get_local $7)
        )
        (br $label$continue$L1)
       )
      )
      (i32.store
       (get_local $15)
       (tee_local $13
        (if (result i32)
         (i32.lt_u
          (tee_local $14
           (i32.add
            (i32.load8_s
             (tee_local $5
              (i32.add
               (get_local $7)
               (i32.const 1)
              )
             )
            )
            (i32.const -48)
           )
          )
          (i32.const 10)
         )
         (block (result i32)
          (set_local $8
           (i32.add
            (get_local $7)
            (i32.const 3)
           )
          )
          (set_local $7
           (if (result i32)
            (tee_local $6
             (i32.eq
              (i32.load8_s offset=2
               (get_local $7)
              )
              (i32.const 36)
             )
            )
            (get_local $8)
            (get_local $5)
           )
          )
          (if
           (get_local $6)
           (set_local $13
            (i32.const 1)
           )
          )
          (if
           (i32.eqz
            (get_local $6)
           )
           (set_local $14
            (i32.const -1)
           )
          )
          (set_local $6
           (get_local $13)
          )
          (get_local $7)
         )
         (block (result i32)
          (set_local $14
           (i32.const -1)
          )
          (set_local $6
           (get_local $13)
          )
          (get_local $5)
         )
        )
       )
      )
      (block $label$break$L24
       (if
        (i32.lt_u
         (tee_local $8
          (i32.add
           (tee_local $7
            (i32.load8_s
             (get_local $13)
            )
           )
           (i32.const -32)
          )
         )
         (i32.const 32)
        )
        (block
         (set_local $5
          (i32.const 0)
         )
         (loop $while-in4
          (br_if $label$break$L24
           (i32.eqz
            (i32.and
             (tee_local $8
              (i32.shl
               (i32.const 1)
               (get_local $8)
              )
             )
             (i32.const 75913)
            )
           )
          )
          (set_local $5
           (i32.or
            (get_local $8)
            (get_local $5)
           )
          )
          (i32.store
           (get_local $15)
           (tee_local $13
            (i32.add
             (get_local $13)
             (i32.const 1)
            )
           )
          )
          (br_if $while-in4
           (i32.lt_u
            (tee_local $8
             (i32.add
              (tee_local $7
               (i32.load8_s
                (get_local $13)
               )
              )
              (i32.const -32)
             )
            )
            (i32.const 32)
           )
          )
         )
        )
        (set_local $5
         (i32.const 0)
        )
       )
      )
      (set_local $13
       (if (result i32)
        (i32.eq
         (i32.and
          (get_local $7)
          (i32.const 255)
         )
         (i32.const 42)
        )
        (block (result i32)
         (set_local $9
          (block $__rjto$0 (result i32)
           (block $__rjti$0
            (br_if $__rjti$0
             (i32.ge_u
              (tee_local $8
               (i32.add
                (i32.load8_s
                 (tee_local $7
                  (i32.add
                   (get_local $13)
                   (i32.const 1)
                  )
                 )
                )
                (i32.const -48)
               )
              )
              (i32.const 10)
             )
            )
            (br_if $__rjti$0
             (i32.ne
              (i32.load8_s offset=2
               (get_local $13)
              )
              (i32.const 36)
             )
            )
            (i32.store
             (i32.add
              (get_local $4)
              (i32.shl
               (get_local $8)
               (i32.const 2)
              )
             )
             (i32.const 10)
            )
            (set_local $6
             (i32.wrap/i64
              (i64.load
               (i32.add
                (get_local $3)
                (i32.shl
                 (i32.add
                  (i32.load8_s
                   (get_local $7)
                  )
                  (i32.const -48)
                 )
                 (i32.const 3)
                )
               )
              )
             )
            )
            (set_local $7
             (i32.add
              (get_local $13)
              (i32.const 3)
             )
            )
            (br $__rjto$0
             (i32.const 1)
            )
           )
           (if
            (get_local $6)
            (block
             (set_local $11
              (i32.const -1)
             )
             (br $label$break$L1)
            )
           )
           (if (result i32)
            (get_local $21)
            (block (result i32)
             (set_local $6
              (i32.load
               (tee_local $13
                (i32.and
                 (i32.add
                  (i32.load
                   (get_local $2)
                  )
                  (i32.const 3)
                 )
                 (i32.const -4)
                )
               )
              )
             )
             (i32.store
              (get_local $2)
              (i32.add
               (get_local $13)
               (i32.const 4)
              )
             )
             (i32.const 0)
            )
            (block (result i32)
             (set_local $6
              (i32.const 0)
             )
             (i32.const 0)
            )
           )
          )
         )
         (i32.store
          (get_local $15)
          (get_local $7)
         )
         (set_local $8
          (i32.or
           (get_local $5)
           (i32.const 8192)
          )
         )
         (set_local $16
          (i32.sub
           (i32.const 0)
           (get_local $6)
          )
         )
         (if
          (i32.eqz
           (tee_local $13
            (i32.lt_s
             (get_local $6)
             (i32.const 0)
            )
           )
          )
          (set_local $8
           (get_local $5)
          )
         )
         (if
          (i32.eqz
           (get_local $13)
          )
          (set_local $16
           (get_local $6)
          )
         )
         (get_local $9)
        )
        (block (result i32)
         (if
          (i32.lt_s
           (tee_local $16
            (call $_getint
             (get_local $15)
            )
           )
           (i32.const 0)
          )
          (block
           (set_local $11
            (i32.const -1)
           )
           (br $label$break$L1)
          )
         )
         (set_local $8
          (get_local $5)
         )
         (set_local $7
          (i32.load
           (get_local $15)
          )
         )
         (get_local $6)
        )
       )
      )
      (block $do-once5
       (if
        (i32.eq
         (i32.load8_s
          (get_local $7)
         )
         (i32.const 46)
        )
        (block
         (if
          (i32.ne
           (i32.load8_s offset=1
            (get_local $7)
           )
           (i32.const 42)
          )
          (block
           (i32.store
            (get_local $15)
            (i32.add
             (get_local $7)
             (i32.const 1)
            )
           )
           (set_local $5
            (call $_getint
             (get_local $15)
            )
           )
           (set_local $7
            (i32.load
             (get_local $15)
            )
           )
           (br $do-once5)
          )
         )
         (if
          (i32.lt_u
           (tee_local $5
            (i32.add
             (i32.load8_s
              (tee_local $6
               (i32.add
                (get_local $7)
                (i32.const 2)
               )
              )
             )
             (i32.const -48)
            )
           )
           (i32.const 10)
          )
          (if
           (i32.eq
            (i32.load8_s offset=3
             (get_local $7)
            )
            (i32.const 36)
           )
           (block
            (i32.store
             (i32.add
              (get_local $4)
              (i32.shl
               (get_local $5)
               (i32.const 2)
              )
             )
             (i32.const 10)
            )
            (set_local $5
             (i32.wrap/i64
              (i64.load
               (i32.add
                (get_local $3)
                (i32.shl
                 (i32.add
                  (i32.load8_s
                   (get_local $6)
                  )
                  (i32.const -48)
                 )
                 (i32.const 3)
                )
               )
              )
             )
            )
            (i32.store
             (get_local $15)
             (tee_local $7
              (i32.add
               (get_local $7)
               (i32.const 4)
              )
             )
            )
            (br $do-once5)
           )
          )
         )
         (if
          (get_local $13)
          (block
           (set_local $11
            (i32.const -1)
           )
           (br $label$break$L1)
          )
         )
         (if
          (get_local $21)
          (block
           (set_local $5
            (i32.load
             (tee_local $7
              (i32.and
               (i32.add
                (i32.load
                 (get_local $2)
                )
                (i32.const 3)
               )
               (i32.const -4)
              )
             )
            )
           )
           (i32.store
            (get_local $2)
            (i32.add
             (get_local $7)
             (i32.const 4)
            )
           )
          )
          (set_local $5
           (i32.const 0)
          )
         )
         (i32.store
          (get_local $15)
          (get_local $6)
         )
         (set_local $7
          (get_local $6)
         )
        )
        (set_local $5
         (i32.const -1)
        )
       )
      )
      (set_local $10
       (i32.const 0)
      )
      (set_local $6
       (get_local $7)
      )
      (loop $while-in8
       (if
        (i32.gt_u
         (i32.add
          (i32.load8_s
           (get_local $6)
          )
          (i32.const -65)
         )
         (i32.const 57)
        )
        (block
         (set_local $11
          (i32.const -1)
         )
         (br $label$break$L1)
        )
       )
       (i32.store
        (get_local $15)
        (tee_local $7
         (i32.add
          (get_local $6)
          (i32.const 1)
         )
        )
       )
       (if
        (i32.lt_u
         (i32.add
          (tee_local $9
           (i32.and
            (tee_local $19
             (i32.load8_s
              (i32.add
               (i32.add
                (i32.mul
                 (get_local $10)
                 (i32.const 58)
                )
                (i32.load8_s
                 (get_local $6)
                )
               )
               (i32.const 3243)
              )
             )
            )
            (i32.const 255)
           )
          )
          (i32.const -1)
         )
         (i32.const 8)
        )
        (block
         (set_local $10
          (get_local $9)
         )
         (set_local $6
          (get_local $7)
         )
         (br $while-in8)
        )
       )
      )
      (if
       (i32.eqz
        (get_local $19)
       )
       (block
        (set_local $11
         (i32.const -1)
        )
        (br $label$break$L1)
       )
      )
      (set_local $26
       (i32.gt_s
        (get_local $14)
        (i32.const -1)
       )
      )
      (block $__rjto$2
       (block $__rjti$2
        (if
         (i32.eq
          (get_local $19)
          (i32.const 19)
         )
         (if
          (get_local $26)
          (block
           (set_local $11
            (i32.const -1)
           )
           (br $label$break$L1)
          )
          (br $__rjti$2)
         )
         (block
          (if
           (get_local $26)
           (block
            (i32.store
             (i32.add
              (get_local $4)
              (i32.shl
               (get_local $14)
               (i32.const 2)
              )
             )
             (get_local $9)
            )
            (i64.store
             (get_local $12)
             (i64.load
              (i32.add
               (get_local $3)
               (i32.shl
                (get_local $14)
                (i32.const 3)
               )
              )
             )
            )
            (br $__rjti$2)
           )
          )
          (if
           (i32.eqz
            (get_local $21)
           )
           (block
            (set_local $11
             (i32.const 0)
            )
            (br $label$break$L1)
           )
          )
          (call $_pop_arg
           (get_local $12)
           (get_local $9)
           (get_local $2)
          )
         )
        )
        (br $__rjto$2)
       )
       (if
        (i32.eqz
         (get_local $21)
        )
        (block
         (set_local $5
          (i32.const 0)
         )
         (set_local $1
          (get_local $7)
         )
         (br $label$continue$L1)
        )
       )
      )
      (set_local $6
       (i32.and
        (tee_local $9
         (i32.load8_s
          (get_local $6)
         )
        )
        (i32.const -33)
       )
      )
      (if
       (i32.eqz
        (i32.and
         (i32.ne
          (get_local $10)
          (i32.const 0)
         )
         (i32.eq
          (i32.and
           (get_local $9)
           (i32.const 15)
          )
          (i32.const 3)
         )
        )
       )
       (set_local $6
        (get_local $9)
       )
      )
      (set_local $9
       (i32.and
        (get_local $8)
        (i32.const -65537)
       )
      )
      (if
       (i32.and
        (get_local $8)
        (i32.const 8192)
       )
       (set_local $8
        (get_local $9)
       )
      )
      (block $__rjto$8
       (block $__rjti$8
        (block $__rjti$7
         (block $__rjti$6
          (block $__rjti$5
           (block $__rjti$4
            (block $__rjti$3
             (block $switch-default43
              (block $switch-case35
               (block $switch-case34
                (block $switch-case33
                 (block $switch-case32
                  (block $switch-case31
                   (block $switch-case30
                    (block $switch-case29
                     (block $switch-case27
                      (block $switch-case26
                       (block $switch-case24
                        (block $switch-case23
                         (block $switch-case22
                          (br_table $switch-case35 $switch-default43 $switch-case33 $switch-default43 $switch-case35 $switch-case35 $switch-case35 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-case34 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-case24 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-default43 $switch-case35 $switch-default43 $switch-case30 $switch-case27 $switch-case35 $switch-case35 $switch-case35 $switch-default43 $switch-case27 $switch-default43 $switch-default43 $switch-default43 $switch-case31 $switch-case22 $switch-case26 $switch-case23 $switch-default43 $switch-default43 $switch-case32 $switch-default43 $switch-case29 $switch-default43 $switch-default43 $switch-case24 $switch-default43
                           (i32.sub
                            (get_local $6)
                            (i32.const 65)
                           )
                          )
                         )
                         (block $switch13
                          (block $switch-default21
                           (block $switch-case20
                            (block $switch-case19
                             (block $switch-case18
                              (block $switch-case17
                               (block $switch-case16
                                (block $switch-case15
                                 (block $switch-case14
                                  (br_table $switch-case14 $switch-case15 $switch-case16 $switch-case17 $switch-case18 $switch-default21 $switch-case19 $switch-case20 $switch-default21
                                   (i32.shr_s
                                    (i32.shl
                                     (i32.and
                                      (get_local $10)
                                      (i32.const 255)
                                     )
                                     (i32.const 24)
                                    )
                                    (i32.const 24)
                                   )
                                  )
                                 )
                                 (i32.store
                                  (i32.load
                                   (get_local $12)
                                  )
                                  (get_local $11)
                                 )
                                 (set_local $5
                                  (i32.const 0)
                                 )
                                 (set_local $1
                                  (get_local $7)
                                 )
                                 (br $label$continue$L1)
                                )
                                (i32.store
                                 (i32.load
                                  (get_local $12)
                                 )
                                 (get_local $11)
                                )
                                (set_local $5
                                 (i32.const 0)
                                )
                                (set_local $1
                                 (get_local $7)
                                )
                                (br $label$continue$L1)
                               )
                               (i64.store
                                (i32.load
                                 (get_local $12)
                                )
                                (i64.extend_s/i32
                                 (get_local $11)
                                )
                               )
                               (set_local $5
                                (i32.const 0)
                               )
                               (set_local $1
                                (get_local $7)
                               )
                               (br $label$continue$L1)
                              )
                              (i32.store16
                               (i32.load
                                (get_local $12)
                               )
                               (get_local $11)
                              )
                              (set_local $5
                               (i32.const 0)
                              )
                              (set_local $1
                               (get_local $7)
                              )
                              (br $label$continue$L1)
                             )
                             (i32.store8
                              (i32.load
                               (get_local $12)
                              )
                              (get_local $11)
                             )
                             (set_local $5
                              (i32.const 0)
                             )
                             (set_local $1
                              (get_local $7)
                             )
                             (br $label$continue$L1)
                            )
                            (i32.store
                             (i32.load
                              (get_local $12)
                             )
                             (get_local $11)
                            )
                            (set_local $5
                             (i32.const 0)
                            )
                            (set_local $1
                             (get_local $7)
                            )
                            (br $label$continue$L1)
                           )
                           (i64.store
                            (i32.load
                             (get_local $12)
                            )
                            (i64.extend_s/i32
                             (get_local $11)
                            )
                           )
                           (set_local $5
                            (i32.const 0)
                           )
                           (set_local $1
                            (get_local $7)
                           )
                           (br $label$continue$L1)
                          )
                          (set_local $5
                           (i32.const 0)
                          )
                          (set_local $1
                           (get_local $7)
                          )
                          (br $label$continue$L1)
                         )
                        )
                        (set_local $6
                         (i32.const 120)
                        )
                        (if
                         (i32.le_u
                          (get_local $5)
                          (i32.const 8)
                         )
                         (set_local $5
                          (i32.const 8)
                         )
                        )
                        (set_local $8
                         (i32.or
                          (get_local $8)
                          (i32.const 8)
                         )
                        )
                        (br $__rjti$3)
                       )
                       (br $__rjti$3)
                      )
                      (set_local $14
                       (i32.add
                        (tee_local $10
                         (i32.sub
                          (get_local $24)
                          (tee_local $1
                           (call $_fmt_o
                            (tee_local $17
                             (i64.load
                              (get_local $12)
                             )
                            )
                            (get_local $18)
                           )
                          )
                         )
                        )
                        (i32.const 1)
                       )
                      )
                      (set_local $9
                       (i32.const 0)
                      )
                      (set_local $6
                       (i32.const 3772)
                      )
                      (if
                       (i32.eqz
                        (i32.or
                         (i32.eqz
                          (i32.and
                           (get_local $8)
                           (i32.const 8)
                          )
                         )
                         (i32.gt_s
                          (get_local $5)
                          (get_local $10)
                         )
                        )
                       )
                       (set_local $5
                        (get_local $14)
                       )
                      )
                      (br $__rjti$7)
                     )
                     (if
                      (i64.lt_s
                       (tee_local $17
                        (i64.load
                         (get_local $12)
                        )
                       )
                       (i64.const 0)
                      )
                      (block
                       (i64.store
                        (get_local $12)
                        (tee_local $17
                         (i64.sub
                          (i64.const 0)
                          (get_local $17)
                         )
                        )
                       )
                       (set_local $9
                        (i32.const 1)
                       )
                       (set_local $6
                        (i32.const 3772)
                       )
                       (br $__rjti$4)
                      )
                      (block
                       (set_local $1
                        (i32.eqz
                         (i32.and
                          (get_local $8)
                          (i32.const 2048)
                         )
                        )
                       )
                       (set_local $6
                        (if (result i32)
                         (i32.and
                          (get_local $8)
                          (i32.const 1)
                         )
                         (i32.const 3774)
                         (i32.const 3772)
                        )
                       )
                       (set_local $9
                        (i32.ne
                         (i32.and
                          (get_local $8)
                          (i32.const 2049)
                         )
                         (i32.const 0)
                        )
                       )
                       (if
                        (i32.eqz
                         (get_local $1)
                        )
                        (set_local $6
                         (i32.const 3773)
                        )
                       )
                       (br $__rjti$4)
                      )
                     )
                    )
                    (set_local $9
                     (i32.const 0)
                    )
                    (set_local $6
                     (i32.const 3772)
                    )
                    (set_local $17
                     (i64.load
                      (get_local $12)
                     )
                    )
                    (br $__rjti$4)
                   )
                   (i64.store8
                    (get_local $25)
                    (i64.load
                     (get_local $12)
                    )
                   )
                   (set_local $1
                    (get_local $25)
                   )
                   (set_local $10
                    (i32.const 0)
                   )
                   (set_local $14
                    (i32.const 3772)
                   )
                   (set_local $6
                    (get_local $18)
                   )
                   (set_local $5
                    (i32.const 1)
                   )
                   (set_local $8
                    (get_local $9)
                   )
                   (br $__rjto$8)
                  )
                  (set_local $1
                   (call $_strerror
                    (i32.load
                     (call $___errno_location)
                    )
                   )
                  )
                  (br $__rjti$5)
                 )
                 (if
                  (i32.eqz
                   (tee_local $1
                    (i32.load
                     (get_local $12)
                    )
                   )
                  )
                  (set_local $1
                   (i32.const 3782)
                  )
                 )
                 (br $__rjti$5)
                )
                (i64.store32
                 (get_local $23)
                 (i64.load
                  (get_local $12)
                 )
                )
                (i32.store
                 (get_local $27)
                 (i32.const 0)
                )
                (i32.store
                 (get_local $12)
                 (get_local $23)
                )
                (set_local $10
                 (i32.const -1)
                )
                (set_local $5
                 (get_local $23)
                )
                (br $__rjti$6)
               )
               (set_local $1
                (i32.load
                 (get_local $12)
                )
               )
               (if
                (get_local $5)
                (block
                 (set_local $10
                  (get_local $5)
                 )
                 (set_local $5
                  (get_local $1)
                 )
                 (br $__rjti$6)
                )
                (block
                 (call $_pad
                  (get_local $0)
                  (i32.const 32)
                  (get_local $16)
                  (i32.const 0)
                  (get_local $8)
                 )
                 (set_local $1
                  (i32.const 0)
                 )
                 (br $__rjti$8)
                )
               )
              )
              (set_local $5
               (call $_fmt_fp
                (get_local $0)
                (f64.load
                 (get_local $12)
                )
                (get_local $16)
                (get_local $5)
                (get_local $8)
                (get_local $6)
               )
              )
              (set_local $1
               (get_local $7)
              )
              (br $label$continue$L1)
             )
             (set_local $10
              (i32.const 0)
             )
             (set_local $14
              (i32.const 3772)
             )
             (set_local $6
              (get_local $18)
             )
             (br $__rjto$8)
            )
            (set_local $1
             (call $_fmt_x
              (tee_local $17
               (i64.load
                (get_local $12)
               )
              )
              (get_local $18)
              (i32.and
               (get_local $6)
               (i32.const 32)
              )
             )
            )
            (set_local $6
             (i32.add
              (i32.shr_s
               (get_local $6)
               (i32.const 4)
              )
              (i32.const 3772)
             )
            )
            (if
             (tee_local $9
              (i32.or
               (i32.eqz
                (i32.and
                 (get_local $8)
                 (i32.const 8)
                )
               )
               (i64.eq
                (get_local $17)
                (i64.const 0)
               )
              )
             )
             (set_local $6
              (i32.const 3772)
             )
            )
            (set_local $9
             (if (result i32)
              (get_local $9)
              (i32.const 0)
              (i32.const 2)
             )
            )
            (br $__rjti$7)
           )
           (set_local $1
            (call $_fmt_u
             (get_local $17)
             (get_local $18)
            )
           )
           (br $__rjti$7)
          )
          (set_local $19
           (i32.eqz
            (tee_local $8
             (call $_memchr
              (get_local $1)
              (i32.const 0)
              (get_local $5)
             )
            )
           )
          )
          (set_local $10
           (i32.sub
            (get_local $8)
            (get_local $1)
           )
          )
          (set_local $6
           (i32.add
            (get_local $1)
            (get_local $5)
           )
          )
          (if
           (i32.eqz
            (get_local $19)
           )
           (set_local $5
            (get_local $10)
           )
          )
          (set_local $10
           (i32.const 0)
          )
          (set_local $14
           (i32.const 3772)
          )
          (if
           (i32.eqz
            (get_local $19)
           )
           (set_local $6
            (get_local $8)
           )
          )
          (set_local $8
           (get_local $9)
          )
          (br $__rjto$8)
         )
         (set_local $9
          (get_local $5)
         )
         (set_local $1
          (i32.const 0)
         )
         (set_local $6
          (i32.const 0)
         )
         (loop $while-in46
          (block $while-out45
           (br_if $while-out45
            (i32.eqz
             (tee_local $14
              (i32.load
               (get_local $9)
              )
             )
            )
           )
           (br_if $while-out45
            (i32.or
             (i32.lt_s
              (tee_local $6
               (call $_wctomb
                (get_local $22)
                (get_local $14)
               )
              )
              (i32.const 0)
             )
             (i32.gt_u
              (get_local $6)
              (i32.sub
               (get_local $10)
               (get_local $1)
              )
             )
            )
           )
           (set_local $9
            (i32.add
             (get_local $9)
             (i32.const 4)
            )
           )
           (br_if $while-in46
            (i32.gt_u
             (get_local $10)
             (tee_local $1
              (i32.add
               (get_local $6)
               (get_local $1)
              )
             )
            )
           )
          )
         )
         (if
          (i32.lt_s
           (get_local $6)
           (i32.const 0)
          )
          (block
           (set_local $11
            (i32.const -1)
           )
           (br $label$break$L1)
          )
         )
         (call $_pad
          (get_local $0)
          (i32.const 32)
          (get_local $16)
          (get_local $1)
          (get_local $8)
         )
         (if
          (get_local $1)
          (block
           (set_local $6
            (i32.const 0)
           )
           (loop $while-in48
            (br_if $__rjti$8
             (i32.eqz
              (tee_local $9
               (i32.load
                (get_local $5)
               )
              )
             )
            )
            (br_if $__rjti$8
             (i32.gt_s
              (tee_local $6
               (i32.add
                (tee_local $9
                 (call $_wctomb
                  (get_local $22)
                  (get_local $9)
                 )
                )
                (get_local $6)
               )
              )
              (get_local $1)
             )
            )
            (set_local $5
             (i32.add
              (get_local $5)
              (i32.const 4)
             )
            )
            (call $_out
             (get_local $0)
             (get_local $22)
             (get_local $9)
            )
            (br_if $while-in48
             (i32.lt_u
              (get_local $6)
              (get_local $1)
             )
            )
            (br $__rjti$8)
           )
          )
          (block
           (set_local $1
            (i32.const 0)
           )
           (br $__rjti$8)
          )
         )
        )
        (set_local $10
         (i32.and
          (get_local $8)
          (i32.const -65537)
         )
        )
        (if
         (i32.gt_s
          (get_local $5)
          (i32.const -1)
         )
         (set_local $8
          (get_local $10)
         )
        )
        (set_local $14
         (i32.or
          (i32.ne
           (get_local $5)
           (i32.const 0)
          )
          (tee_local $10
           (i64.ne
            (get_local $17)
            (i64.const 0)
           )
          )
         )
        )
        (if
         (i32.gt_s
          (get_local $5)
          (tee_local $10
           (i32.add
            (i32.and
             (i32.xor
              (get_local $10)
              (i32.const 1)
             )
             (i32.const 1)
            )
            (i32.sub
             (get_local $24)
             (get_local $1)
            )
           )
          )
         )
         (set_local $10
          (get_local $5)
         )
        )
        (if
         (get_local $14)
         (set_local $5
          (get_local $10)
         )
        )
        (if
         (i32.eqz
          (get_local $14)
         )
         (set_local $1
          (get_local $18)
         )
        )
        (set_local $10
         (get_local $9)
        )
        (set_local $14
         (get_local $6)
        )
        (set_local $6
         (get_local $18)
        )
        (br $__rjto$8)
       )
       (call $_pad
        (get_local $0)
        (i32.const 32)
        (get_local $16)
        (get_local $1)
        (i32.xor
         (get_local $8)
         (i32.const 8192)
        )
       )
       (set_local $5
        (if (result i32)
         (i32.gt_s
          (get_local $16)
          (get_local $1)
         )
         (get_local $16)
         (get_local $1)
        )
       )
       (set_local $1
        (get_local $7)
       )
       (br $label$continue$L1)
      )
      (call $_pad
       (get_local $0)
       (i32.const 32)
       (tee_local $5
        (if (result i32)
         (i32.lt_s
          (get_local $16)
          (tee_local $6
           (i32.add
            (tee_local $19
             (if (result i32)
              (i32.lt_s
               (get_local $5)
               (tee_local $9
                (i32.sub
                 (get_local $6)
                 (get_local $1)
                )
               )
              )
              (get_local $9)
              (get_local $5)
             )
            )
            (get_local $10)
           )
          )
         )
         (get_local $6)
         (get_local $16)
        )
       )
       (get_local $6)
       (get_local $8)
      )
      (call $_out
       (get_local $0)
       (get_local $14)
       (get_local $10)
      )
      (call $_pad
       (get_local $0)
       (i32.const 48)
       (get_local $5)
       (get_local $6)
       (i32.xor
        (get_local $8)
        (i32.const 65536)
       )
      )
      (call $_pad
       (get_local $0)
       (i32.const 48)
       (get_local $19)
       (get_local $9)
       (i32.const 0)
      )
      (call $_out
       (get_local $0)
       (get_local $1)
       (get_local $9)
      )
      (call $_pad
       (get_local $0)
       (i32.const 32)
       (get_local $5)
       (get_local $6)
       (i32.xor
        (get_local $8)
        (i32.const 8192)
       )
      )
      (set_local $1
       (get_local $7)
      )
      (br $label$continue$L1)
     )
    )
    (br $label$break$L114)
   )
   (if
    (i32.eqz
     (get_local $0)
    )
    (if
     (get_local $13)
     (block
      (set_local $0
       (i32.const 1)
      )
      (loop $while-in51
       (if
        (tee_local $1
         (i32.load
          (i32.add
           (get_local $4)
           (i32.shl
            (get_local $0)
            (i32.const 2)
           )
          )
         )
        )
        (block
         (call $_pop_arg
          (i32.add
           (get_local $3)
           (i32.shl
            (get_local $0)
            (i32.const 3)
           )
          )
          (get_local $1)
          (get_local $2)
         )
         (br_if $while-in51
          (i32.lt_s
           (tee_local $0
            (i32.add
             (get_local $0)
             (i32.const 1)
            )
           )
           (i32.const 10)
          )
         )
         (set_local $11
          (i32.const 1)
         )
         (br $label$break$L114)
        )
       )
      )
      (loop $while-in53
       (if
        (i32.load
         (i32.add
          (get_local $4)
          (i32.shl
           (get_local $0)
           (i32.const 2)
          )
         )
        )
        (block
         (set_local $11
          (i32.const -1)
         )
         (br $label$break$L114)
        )
       )
       (br_if $while-in53
        (i32.lt_s
         (tee_local $0
          (i32.add
           (get_local $0)
           (i32.const 1)
          )
         )
         (i32.const 10)
        )
       )
       (set_local $11
        (i32.const 1)
       )
      )
     )
     (set_local $11
      (i32.const 0)
     )
    )
   )
  )
  (set_global $STACKTOP
   (get_local $20)
  )
  (get_local $11)
 )
 (func $___lockfile (param $0 i32) (result i32)
  (i32.const 0)
 )
 (func $___unlockfile (param $0 i32)
  (nop)
 )
 (func $_out (param $0 i32) (param $1 i32) (param $2 i32)
  (if
   (i32.eqz
    (i32.and
     (i32.load
      (get_local $0)
     )
     (i32.const 32)
    )
   )
   (drop
    (call $___fwritex
     (get_local $1)
     (get_local $2)
     (get_local $0)
    )
   )
  )
 )
 (func $_getint (param $0 i32) (result i32)
  (local $1 i32)
  (local $2 i32)
  (local $3 i32)
  (local $4 i32)
  (if
   (i32.lt_u
    (tee_local $1
     (i32.add
      (i32.load8_s
       (tee_local $2
        (i32.load
         (get_local $0)
        )
       )
      )
      (i32.const -48)
     )
    )
    (i32.const 10)
   )
   (block
    (set_local $3
     (i32.const 0)
    )
    (loop $while-in
     (set_local $1
      (i32.add
       (get_local $1)
       (i32.mul
        (get_local $3)
        (i32.const 10)
       )
      )
     )
     (i32.store
      (get_local $0)
      (tee_local $2
       (i32.add
        (get_local $2)
        (i32.const 1)
       )
      )
     )
     (if
      (i32.lt_u
       (tee_local $4
        (i32.add
         (i32.load8_s
          (get_local $2)
         )
         (i32.const -48)
        )
       )
       (i32.const 10)
      )
      (block
       (set_local $3
        (get_local $1)
       )
       (set_local $1
        (get_local $4)
       )
       (br $while-in)
      )
     )
    )
   )
   (set_local $1
    (i32.const 0)
   )
  )
  (get_local $1)
 )
 (func $_pop_arg (param $0 i32) (param $1 i32) (param $2 i32)
  (local $3 i32)
  (local $4 f64)
  (local $5 i64)
  (block $label$break$L1
   (if
    (i32.le_u
     (get_local $1)
     (i32.const 20)
    )
    (block $switch-default
     (block $switch-case9
      (block $switch-case8
       (block $switch-case7
        (block $switch-case6
         (block $switch-case5
          (block $switch-case4
           (block $switch-case3
            (block $switch-case2
             (block $switch-case1
              (block $switch-case
               (br_table $switch-case $switch-case1 $switch-case2 $switch-case3 $switch-case4 $switch-case5 $switch-case6 $switch-case7 $switch-case8 $switch-case9 $switch-default
                (i32.sub
                 (get_local $1)
                 (i32.const 9)
                )
               )
              )
              (set_local $3
               (i32.load
                (tee_local $1
                 (i32.and
                  (i32.add
                   (i32.load
                    (get_local $2)
                   )
                   (i32.const 3)
                  )
                  (i32.const -4)
                 )
                )
               )
              )
              (i32.store
               (get_local $2)
               (i32.add
                (get_local $1)
                (i32.const 4)
               )
              )
              (i32.store
               (get_local $0)
               (get_local $3)
              )
              (br $label$break$L1)
             )
             (set_local $3
              (i32.load
               (tee_local $1
                (i32.and
                 (i32.add
                  (i32.load
                   (get_local $2)
                  )
                  (i32.const 3)
                 )
                 (i32.const -4)
                )
               )
              )
             )
             (i32.store
              (get_local $2)
              (i32.add
               (get_local $1)
               (i32.const 4)
              )
             )
             (i64.store
              (get_local $0)
              (i64.extend_s/i32
               (get_local $3)
              )
             )
             (br $label$break$L1)
            )
            (set_local $3
             (i32.load
              (tee_local $1
               (i32.and
                (i32.add
                 (i32.load
                  (get_local $2)
                 )
                 (i32.const 3)
                )
                (i32.const -4)
               )
              )
             )
            )
            (i32.store
             (get_local $2)
             (i32.add
              (get_local $1)
              (i32.const 4)
             )
            )
            (i64.store
             (get_local $0)
             (i64.extend_u/i32
              (get_local $3)
             )
            )
            (br $label$break$L1)
           )
           (set_local $5
            (i64.load
             (tee_local $1
              (i32.and
               (i32.add
                (i32.load
                 (get_local $2)
                )
                (i32.const 7)
               )
               (i32.const -8)
              )
             )
            )
           )
           (i32.store
            (get_local $2)
            (i32.add
             (get_local $1)
             (i32.const 8)
            )
           )
           (i64.store
            (get_local $0)
            (get_local $5)
           )
           (br $label$break$L1)
          )
          (set_local $3
           (i32.load
            (tee_local $1
             (i32.and
              (i32.add
               (i32.load
                (get_local $2)
               )
               (i32.const 3)
              )
              (i32.const -4)
             )
            )
           )
          )
          (i32.store
           (get_local $2)
           (i32.add
            (get_local $1)
            (i32.const 4)
           )
          )
          (i64.store
           (get_local $0)
           (i64.extend_s/i32
            (i32.shr_s
             (i32.shl
              (i32.and
               (get_local $3)
               (i32.const 65535)
              )
              (i32.const 16)
             )
             (i32.const 16)
            )
           )
          )
          (br $label$break$L1)
         )
         (set_local $3
          (i32.load
           (tee_local $1
            (i32.and
             (i32.add
              (i32.load
               (get_local $2)
              )
              (i32.const 3)
             )
             (i32.const -4)
            )
           )
          )
         )
         (i32.store
          (get_local $2)
          (i32.add
           (get_local $1)
           (i32.const 4)
          )
         )
         (i64.store
          (get_local $0)
          (i64.extend_u/i32
           (i32.and
            (get_local $3)
            (i32.const 65535)
           )
          )
         )
         (br $label$break$L1)
        )
        (set_local $3
         (i32.load
          (tee_local $1
           (i32.and
            (i32.add
             (i32.load
              (get_local $2)
             )
             (i32.const 3)
            )
            (i32.const -4)
           )
          )
         )
        )
        (i32.store
         (get_local $2)
         (i32.add
          (get_local $1)
          (i32.const 4)
         )
        )
        (i64.store
         (get_local $0)
         (i64.extend_s/i32
          (i32.shr_s
           (i32.shl
            (i32.and
             (get_local $3)
             (i32.const 255)
            )
            (i32.const 24)
           )
           (i32.const 24)
          )
         )
        )
        (br $label$break$L1)
       )
       (set_local $3
        (i32.load
         (tee_local $1
          (i32.and
           (i32.add
            (i32.load
             (get_local $2)
            )
            (i32.const 3)
           )
           (i32.const -4)
          )
         )
        )
       )
       (i32.store
        (get_local $2)
        (i32.add
         (get_local $1)
         (i32.const 4)
        )
       )
       (i64.store
        (get_local $0)
        (i64.extend_u/i32
         (i32.and
          (get_local $3)
          (i32.const 255)
         )
        )
       )
       (br $label$break$L1)
      )
      (set_local $4
       (f64.load
        (tee_local $1
         (i32.and
          (i32.add
           (i32.load
            (get_local $2)
           )
           (i32.const 7)
          )
          (i32.const -8)
         )
        )
       )
      )
      (i32.store
       (get_local $2)
       (i32.add
        (get_local $1)
        (i32.const 8)
       )
      )
      (f64.store
       (get_local $0)
       (get_local $4)
      )
      (br $label$break$L1)
     )
     (set_local $4
      (f64.load
       (tee_local $1
        (i32.and
         (i32.add
          (i32.load
           (get_local $2)
          )
          (i32.const 7)
         )
         (i32.const -8)
        )
       )
      )
     )
     (i32.store
      (get_local $2)
      (i32.add
       (get_local $1)
       (i32.const 8)
      )
     )
     (f64.store
      (get_local $0)
      (get_local $4)
     )
    )
   )
  )
 )
 (func $_fmt_x (param $0 i64) (param $1 i32) (param $2 i32) (result i32)
  (if
   (i64.ne
    (get_local $0)
    (i64.const 0)
   )
   (loop $while-in
    (i32.store8
     (tee_local $1
      (i32.add
       (get_local $1)
       (i32.const -1)
      )
     )
     (i32.or
      (i32.load8_u
       (i32.add
        (i32.and
         (i32.wrap/i64
          (get_local $0)
         )
         (i32.const 15)
        )
        (i32.const 3824)
       )
      )
      (get_local $2)
     )
    )
    (br_if $while-in
     (i64.ne
      (tee_local $0
       (i64.shr_u
        (get_local $0)
        (i64.const 4)
       )
      )
      (i64.const 0)
     )
    )
   )
  )
  (get_local $1)
 )
 (func $_fmt_o (param $0 i64) (param $1 i32) (result i32)
  (if
   (i64.ne
    (get_local $0)
    (i64.const 0)
   )
   (loop $while-in
    (i32.store8
     (tee_local $1
      (i32.add
       (get_local $1)
       (i32.const -1)
      )
     )
     (i32.or
      (i32.and
       (i32.wrap/i64
        (get_local $0)
       )
       (i32.const 7)
      )
      (i32.const 48)
     )
    )
    (br_if $while-in
     (i64.ne
      (tee_local $0
       (i64.shr_u
        (get_local $0)
        (i64.const 3)
       )
      )
      (i64.const 0)
     )
    )
   )
  )
  (get_local $1)
 )
 (func $_fmt_u (param $0 i64) (param $1 i32) (result i32)
  (local $2 i32)
  (local $3 i64)
  (local $4 i32)
  (set_local $2
   (i32.wrap/i64
    (get_local $0)
   )
  )
  (if
   (i64.gt_u
    (get_local $0)
    (i64.const 4294967295)
   )
   (block
    (loop $while-in
     (i32.store8
      (tee_local $1
       (i32.add
        (get_local $1)
        (i32.const -1)
       )
      )
      (i32.or
       (i32.and
        (i32.wrap/i64
         (i64.rem_u
          (get_local $0)
          (i64.const 10)
         )
        )
        (i32.const 255)
       )
       (i32.const 48)
      )
     )
     (set_local $3
      (i64.div_u
       (get_local $0)
       (i64.const 10)
      )
     )
     (if
      (i64.gt_u
       (get_local $0)
       (i64.const 42949672959)
      )
      (block
       (set_local $0
        (get_local $3)
       )
       (br $while-in)
      )
     )
    )
    (set_local $2
     (i32.wrap/i64
      (get_local $3)
     )
    )
   )
  )
  (if
   (get_local $2)
   (loop $while-in1
    (i32.store8
     (tee_local $1
      (i32.add
       (get_local $1)
       (i32.const -1)
      )
     )
     (i32.or
      (i32.rem_u
       (get_local $2)
       (i32.const 10)
      )
      (i32.const 48)
     )
    )
    (set_local $4
     (i32.div_u
      (get_local $2)
      (i32.const 10)
     )
    )
    (if
     (i32.ge_u
      (get_local $2)
      (i32.const 10)
     )
     (block
      (set_local $2
       (get_local $4)
      )
      (br $while-in1)
     )
    )
   )
  )
  (get_local $1)
 )
 (func $_pad (param $0 i32) (param $1 i32) (param $2 i32) (param $3 i32) (param $4 i32)
  (local $5 i32)
  (local $6 i32)
  (set_local $6
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 256)
   )
  )
  (set_local $5
   (get_local $6)
  )
  (if
   (i32.and
    (i32.gt_s
     (get_local $2)
     (get_local $3)
    )
    (i32.eqz
     (i32.and
      (get_local $4)
      (i32.const 73728)
     )
    )
   )
   (block
    (drop
     (call $_memset
      (get_local $5)
      (get_local $1)
      (if (result i32)
       (i32.lt_u
        (tee_local $1
         (i32.sub
          (get_local $2)
          (get_local $3)
         )
        )
        (i32.const 256)
       )
       (get_local $1)
       (i32.const 256)
      )
     )
    )
    (if
     (i32.gt_u
      (get_local $1)
      (i32.const 255)
     )
     (block
      (set_local $2
       (i32.sub
        (get_local $2)
        (get_local $3)
       )
      )
      (loop $while-in
       (call $_out
        (get_local $0)
        (get_local $5)
        (i32.const 256)
       )
       (br_if $while-in
        (i32.gt_u
         (tee_local $1
          (i32.add
           (get_local $1)
           (i32.const -256)
          )
         )
         (i32.const 255)
        )
       )
      )
      (set_local $1
       (i32.and
        (get_local $2)
        (i32.const 255)
       )
      )
     )
    )
    (call $_out
     (get_local $0)
     (get_local $5)
     (get_local $1)
    )
   )
  )
  (set_global $STACKTOP
   (get_local $6)
  )
 )
 (func $_wctomb (param $0 i32) (param $1 i32) (result i32)
  (if (result i32)
   (get_local $0)
   (call $_wcrtomb
    (get_local $0)
    (get_local $1)
    (i32.const 0)
   )
   (i32.const 0)
  )
 )
 (func $_fmt_fp (param $0 i32) (param $1 f64) (param $2 i32) (param $3 i32) (param $4 i32) (param $5 i32) (result i32)
  (local $6 i32)
  (local $7 i32)
  (local $8 i32)
  (local $9 i32)
  (local $10 i32)
  (local $11 i32)
  (local $12 i32)
  (local $13 i32)
  (local $14 i32)
  (local $15 i32)
  (local $16 f64)
  (local $17 i32)
  (local $18 i32)
  (local $19 i32)
  (local $20 i32)
  (local $21 i32)
  (local $22 i32)
  (local $23 i32)
  (local $24 i32)
  (local $25 f64)
  (local $26 i32)
  (local $27 i64)
  (local $28 i64)
  (local $29 i32)
  (local $30 f64)
  (set_local $22
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 560)
   )
  )
  (i32.store
   (tee_local $12
    (get_local $22)
   )
   (i32.const 0)
  )
  (if
   (i64.lt_s
    (call $___DOUBLE_BITS_536
     (get_local $1)
    )
    (i64.const 0)
   )
   (block
    (set_local $1
     (f64.neg
      (get_local $1)
     )
    )
    (set_local $18
     (i32.const 1)
    )
    (set_local $15
     (i32.const 3789)
    )
   )
   (block
    (set_local $9
     (i32.eqz
      (i32.and
       (get_local $4)
       (i32.const 2048)
      )
     )
    )
    (set_local $15
     (if (result i32)
      (i32.and
       (get_local $4)
       (i32.const 1)
      )
      (i32.const 3795)
      (i32.const 3790)
     )
    )
    (set_local $18
     (i32.ne
      (i32.and
       (get_local $4)
       (i32.const 2049)
      )
      (i32.const 0)
     )
    )
    (if
     (i32.eqz
      (get_local $9)
     )
     (set_local $15
      (i32.const 3792)
     )
    )
   )
  )
  (set_local $9
   (i32.add
    (get_local $22)
    (i32.const 8)
   )
  )
  (set_local $20
   (tee_local $14
    (i32.add
     (get_local $22)
     (i32.const 524)
    )
   )
  )
  (set_local $19
   (i32.add
    (tee_local $8
     (i32.add
      (get_local $22)
      (i32.const 512)
     )
    )
    (i32.const 12)
   )
  )
  (set_local $0
   (block $do-once (result i32)
    (if (result i32)
     (i64.lt_u
      (i64.and
       (call $___DOUBLE_BITS_536
        (get_local $1)
       )
       (i64.const 9218868437227405312)
      )
      (i64.const 9218868437227405312)
     )
     (block (result i32)
      (if
       (tee_local $7
        (f64.ne
         (tee_local $1
          (f64.mul
           (call $_frexpl
            (get_local $1)
            (get_local $12)
           )
           (f64.const 2)
          )
         )
         (f64.const 0)
        )
       )
       (i32.store
        (get_local $12)
        (i32.add
         (i32.load
          (get_local $12)
         )
         (i32.const -1)
        )
       )
      )
      (if
       (i32.eq
        (tee_local $13
         (i32.or
          (get_local $5)
          (i32.const 32)
         )
        )
        (i32.const 97)
       )
       (block
        (set_local $9
         (i32.add
          (get_local $15)
          (i32.const 9)
         )
        )
        (if
         (tee_local $6
          (i32.and
           (get_local $5)
           (i32.const 32)
          )
         )
         (set_local $15
          (get_local $9)
         )
        )
        (if
         (i32.eqz
          (i32.or
           (i32.gt_u
            (get_local $3)
            (i32.const 11)
           )
           (i32.eqz
            (tee_local $9
             (i32.sub
              (i32.const 12)
              (get_local $3)
             )
            )
           )
          )
         )
         (block
          (set_local $16
           (f64.const 8)
          )
          (loop $while-in
           (set_local $16
            (f64.mul
             (get_local $16)
             (f64.const 16)
            )
           )
           (br_if $while-in
            (tee_local $9
             (i32.add
              (get_local $9)
              (i32.const -1)
             )
            )
           )
          )
          (set_local $1
           (if (result f64)
            (i32.eq
             (i32.load8_s
              (get_local $15)
             )
             (i32.const 45)
            )
            (f64.neg
             (f64.add
              (get_local $16)
              (f64.sub
               (f64.neg
                (get_local $1)
               )
               (get_local $16)
              )
             )
            )
            (f64.sub
             (f64.add
              (get_local $1)
              (get_local $16)
             )
             (get_local $16)
            )
           )
          )
         )
        )
        (set_local $9
         (i32.sub
          (i32.const 0)
          (tee_local $7
           (i32.load
            (get_local $12)
           )
          )
         )
        )
        (if
         (i32.eq
          (tee_local $9
           (call $_fmt_u
            (i64.extend_s/i32
             (if (result i32)
              (i32.lt_s
               (get_local $7)
               (i32.const 0)
              )
              (get_local $9)
              (get_local $7)
             )
            )
            (get_local $19)
           )
          )
          (get_local $19)
         )
         (i32.store8
          (tee_local $9
           (i32.add
            (get_local $8)
            (i32.const 11)
           )
          )
          (i32.const 48)
         )
        )
        (set_local $8
         (i32.or
          (get_local $18)
          (i32.const 2)
         )
        )
        (i32.store8
         (i32.add
          (get_local $9)
          (i32.const -1)
         )
         (i32.add
          (i32.and
           (i32.shr_s
            (get_local $7)
            (i32.const 31)
           )
           (i32.const 2)
          )
          (i32.const 43)
         )
        )
        (i32.store8
         (tee_local $7
          (i32.add
           (get_local $9)
           (i32.const -2)
          )
         )
         (i32.add
          (get_local $5)
          (i32.const 15)
         )
        )
        (set_local $10
         (i32.lt_s
          (get_local $3)
          (i32.const 1)
         )
        )
        (set_local $11
         (i32.eqz
          (i32.and
           (get_local $4)
           (i32.const 8)
          )
         )
        )
        (set_local $5
         (get_local $14)
        )
        (loop $while-in3
         (i32.store8
          (get_local $5)
          (i32.or
           (i32.load8_u
            (i32.add
             (tee_local $9
              (i32.trunc_s/f64
               (get_local $1)
              )
             )
             (i32.const 3824)
            )
           )
           (get_local $6)
          )
         )
         (set_local $1
          (f64.mul
           (f64.sub
            (get_local $1)
            (f64.convert_s/i32
             (get_local $9)
            )
           )
           (f64.const 16)
          )
         )
         (set_local $5
          (if (result i32)
           (i32.eq
            (i32.sub
             (tee_local $9
              (i32.add
               (get_local $5)
               (i32.const 1)
              )
             )
             (get_local $20)
            )
            (i32.const 1)
           )
           (if (result i32)
            (i32.and
             (get_local $11)
             (i32.and
              (get_local $10)
              (f64.eq
               (get_local $1)
               (f64.const 0)
              )
             )
            )
            (get_local $9)
            (block (result i32)
             (i32.store8
              (get_local $9)
              (i32.const 46)
             )
             (i32.add
              (get_local $5)
              (i32.const 2)
             )
            )
           )
           (get_local $9)
          )
         )
         (br_if $while-in3
          (f64.ne
           (get_local $1)
           (f64.const 0)
          )
         )
        )
        (set_local $9
         (i32.add
          (get_local $3)
          (i32.const 2)
         )
        )
        (call $_pad
         (get_local $0)
         (i32.const 32)
         (get_local $2)
         (tee_local $3
          (i32.add
           (i32.add
            (tee_local $6
             (i32.sub
              (get_local $19)
              (get_local $7)
             )
            )
            (get_local $8)
           )
           (if (result i32)
            (i32.and
             (i32.ne
              (get_local $3)
              (i32.const 0)
             )
             (i32.lt_s
              (i32.add
               (tee_local $5
                (i32.sub
                 (get_local $5)
                 (get_local $20)
                )
               )
               (i32.const -2)
              )
              (get_local $3)
             )
            )
            (get_local $9)
            (tee_local $9
             (get_local $5)
            )
           )
          )
         )
         (get_local $4)
        )
        (call $_out
         (get_local $0)
         (get_local $15)
         (get_local $8)
        )
        (call $_pad
         (get_local $0)
         (i32.const 48)
         (get_local $2)
         (get_local $3)
         (i32.xor
          (get_local $4)
          (i32.const 65536)
         )
        )
        (call $_out
         (get_local $0)
         (get_local $14)
         (get_local $5)
        )
        (call $_pad
         (get_local $0)
         (i32.const 48)
         (i32.sub
          (get_local $9)
          (get_local $5)
         )
         (i32.const 0)
         (i32.const 0)
        )
        (call $_out
         (get_local $0)
         (get_local $7)
         (get_local $6)
        )
        (call $_pad
         (get_local $0)
         (i32.const 32)
         (get_local $2)
         (get_local $3)
         (i32.xor
          (get_local $4)
          (i32.const 8192)
         )
        )
        (br $do-once
         (get_local $3)
        )
       )
      )
      (if
       (get_local $7)
       (block
        (i32.store
         (get_local $12)
         (tee_local $6
          (i32.add
           (i32.load
            (get_local $12)
           )
           (i32.const -28)
          )
         )
        )
        (set_local $1
         (f64.mul
          (get_local $1)
          (f64.const 268435456)
         )
        )
       )
       (set_local $6
        (i32.load
         (get_local $12)
        )
       )
      )
      (set_local $7
       (i32.add
        (get_local $9)
        (i32.const 288)
       )
      )
      (set_local $8
       (if (result i32)
        (i32.lt_s
         (get_local $6)
         (i32.const 0)
        )
        (get_local $9)
        (tee_local $9
         (get_local $7)
        )
       )
      )
      (loop $while-in5
       (i32.store
        (get_local $8)
        (tee_local $7
         (i32.trunc_u/f64
          (get_local $1)
         )
        )
       )
       (set_local $8
        (i32.add
         (get_local $8)
         (i32.const 4)
        )
       )
       (br_if $while-in5
        (f64.ne
         (tee_local $1
          (f64.mul
           (f64.sub
            (get_local $1)
            (f64.convert_u/i32
             (get_local $7)
            )
           )
           (f64.const 1e9)
          )
         )
         (f64.const 0)
        )
       )
      )
      (if
       (i32.gt_s
        (get_local $6)
        (i32.const 0)
       )
       (block
        (set_local $7
         (get_local $9)
        )
        (loop $while-in7
         (set_local $11
          (if (result i32)
           (i32.lt_s
            (get_local $6)
            (i32.const 29)
           )
           (get_local $6)
           (i32.const 29)
          )
         )
         (if
          (i32.ge_u
           (tee_local $6
            (i32.add
             (get_local $8)
             (i32.const -4)
            )
           )
           (get_local $7)
          )
          (block
           (set_local $27
            (i64.extend_u/i32
             (get_local $11)
            )
           )
           (set_local $10
            (i32.const 0)
           )
           (loop $while-in9
            (i64.store32
             (get_local $6)
             (i64.rem_u
              (tee_local $28
               (i64.add
                (i64.shl
                 (i64.extend_u/i32
                  (i32.load
                   (get_local $6)
                  )
                 )
                 (get_local $27)
                )
                (i64.extend_u/i32
                 (get_local $10)
                )
               )
              )
              (i64.const 1000000000)
             )
            )
            (set_local $10
             (i32.wrap/i64
              (i64.div_u
               (get_local $28)
               (i64.const 1000000000)
              )
             )
            )
            (br_if $while-in9
             (i32.ge_u
              (tee_local $6
               (i32.add
                (get_local $6)
                (i32.const -4)
               )
              )
              (get_local $7)
             )
            )
           )
           (if
            (get_local $10)
            (i32.store
             (tee_local $7
              (i32.add
               (get_local $7)
               (i32.const -4)
              )
             )
             (get_local $10)
            )
           )
          )
         )
         (loop $while-in11
          (if
           (i32.gt_u
            (get_local $8)
            (get_local $7)
           )
           (if
            (i32.eqz
             (i32.load
              (tee_local $6
               (i32.add
                (get_local $8)
                (i32.const -4)
               )
              )
             )
            )
            (block
             (set_local $8
              (get_local $6)
             )
             (br $while-in11)
            )
           )
          )
         )
         (i32.store
          (get_local $12)
          (tee_local $6
           (i32.sub
            (i32.load
             (get_local $12)
            )
            (get_local $11)
           )
          )
         )
         (br_if $while-in7
          (i32.gt_s
           (get_local $6)
           (i32.const 0)
          )
         )
        )
       )
       (set_local $7
        (get_local $9)
       )
      )
      (set_local $11
       (if (result i32)
        (i32.lt_s
         (get_local $3)
         (i32.const 0)
        )
        (i32.const 6)
        (get_local $3)
       )
      )
      (if
       (i32.lt_s
        (get_local $6)
        (i32.const 0)
       )
       (block
        (set_local $17
         (i32.add
          (i32.div_s
           (i32.add
            (get_local $11)
            (i32.const 25)
           )
           (i32.const 9)
          )
          (i32.const 1)
         )
        )
        (set_local $24
         (i32.eq
          (get_local $13)
          (i32.const 102)
         )
        )
        (set_local $3
         (get_local $7)
        )
        (set_local $7
         (get_local $8)
        )
        (loop $while-in13
         (if
          (i32.ge_s
           (tee_local $10
            (i32.sub
             (i32.const 0)
             (get_local $6)
            )
           )
           (i32.const 9)
          )
          (set_local $10
           (i32.const 9)
          )
         )
         (if
          (i32.lt_u
           (get_local $3)
           (get_local $7)
          )
          (block
           (set_local $21
            (i32.add
             (i32.shl
              (i32.const 1)
              (get_local $10)
             )
             (i32.const -1)
            )
           )
           (set_local $23
            (i32.shr_u
             (i32.const 1000000000)
             (get_local $10)
            )
           )
           (set_local $6
            (i32.const 0)
           )
           (set_local $8
            (get_local $3)
           )
           (loop $while-in15
            (i32.store
             (get_local $8)
             (i32.add
              (i32.shr_u
               (tee_local $26
                (i32.load
                 (get_local $8)
                )
               )
               (get_local $10)
              )
              (get_local $6)
             )
            )
            (set_local $6
             (i32.mul
              (i32.and
               (get_local $26)
               (get_local $21)
              )
              (get_local $23)
             )
            )
            (br_if $while-in15
             (i32.lt_u
              (tee_local $8
               (i32.add
                (get_local $8)
                (i32.const 4)
               )
              )
              (get_local $7)
             )
            )
           )
           (set_local $8
            (i32.add
             (get_local $3)
             (i32.const 4)
            )
           )
           (if
            (i32.eqz
             (i32.load
              (get_local $3)
             )
            )
            (set_local $3
             (get_local $8)
            )
           )
           (if
            (get_local $6)
            (block
             (i32.store
              (get_local $7)
              (get_local $6)
             )
             (set_local $7
              (i32.add
               (get_local $7)
               (i32.const 4)
              )
             )
            )
           )
          )
          (block
           (set_local $8
            (i32.add
             (get_local $3)
             (i32.const 4)
            )
           )
           (if
            (i32.eqz
             (i32.load
              (get_local $3)
             )
            )
            (set_local $3
             (get_local $8)
            )
           )
          )
         )
         (set_local $6
          (i32.add
           (tee_local $8
            (if (result i32)
             (get_local $24)
             (get_local $9)
             (get_local $3)
            )
           )
           (i32.shl
            (get_local $17)
            (i32.const 2)
           )
          )
         )
         (if
          (i32.gt_s
           (i32.shr_s
            (i32.sub
             (get_local $7)
             (get_local $8)
            )
            (i32.const 2)
           )
           (get_local $17)
          )
          (set_local $7
           (get_local $6)
          )
         )
         (i32.store
          (get_local $12)
          (tee_local $6
           (i32.add
            (i32.load
             (get_local $12)
            )
            (get_local $10)
           )
          )
         )
         (br_if $while-in13
          (i32.lt_s
           (get_local $6)
           (i32.const 0)
          )
         )
         (set_local $10
          (get_local $7)
         )
        )
       )
       (block
        (set_local $3
         (get_local $7)
        )
        (set_local $10
         (get_local $8)
        )
       )
      )
      (set_local $12
       (get_local $9)
      )
      (if
       (i32.lt_u
        (get_local $3)
        (get_local $10)
       )
       (block
        (set_local $7
         (i32.mul
          (i32.shr_s
           (i32.sub
            (get_local $12)
            (get_local $3)
           )
           (i32.const 2)
          )
          (i32.const 9)
         )
        )
        (if
         (i32.ge_u
          (tee_local $6
           (i32.load
            (get_local $3)
           )
          )
          (i32.const 10)
         )
         (block
          (set_local $8
           (i32.const 10)
          )
          (loop $while-in17
           (set_local $7
            (i32.add
             (get_local $7)
             (i32.const 1)
            )
           )
           (br_if $while-in17
            (i32.ge_u
             (get_local $6)
             (tee_local $8
              (i32.mul
               (get_local $8)
               (i32.const 10)
              )
             )
            )
           )
          )
         )
        )
       )
       (set_local $7
        (i32.const 0)
       )
      )
      (set_local $17
       (i32.eq
        (get_local $13)
        (i32.const 103)
       )
      )
      (set_local $24
       (i32.ne
        (get_local $11)
        (i32.const 0)
       )
      )
      (set_local $8
       (if (result i32)
        (i32.lt_s
         (tee_local $8
          (i32.add
           (i32.sub
            (get_local $11)
            (if (result i32)
             (i32.ne
              (get_local $13)
              (i32.const 102)
             )
             (get_local $7)
             (i32.const 0)
            )
           )
           (i32.shr_s
            (i32.shl
             (i32.and
              (get_local $24)
              (get_local $17)
             )
             (i32.const 31)
            )
            (i32.const 31)
           )
          )
         )
         (i32.add
          (i32.mul
           (i32.shr_s
            (i32.sub
             (get_local $10)
             (get_local $12)
            )
            (i32.const 2)
           )
           (i32.const 9)
          )
          (i32.const -9)
         )
        )
        (block (result i32)
         (set_local $13
          (i32.div_s
           (tee_local $8
            (i32.add
             (get_local $8)
             (i32.const 9216)
            )
           )
           (i32.const 9)
          )
         )
         (if
          (i32.lt_s
           (tee_local $8
            (i32.add
             (i32.rem_s
              (get_local $8)
              (i32.const 9)
             )
             (i32.const 1)
            )
           )
           (i32.const 9)
          )
          (block
           (set_local $6
            (i32.const 10)
           )
           (loop $while-in19
            (set_local $6
             (i32.mul
              (get_local $6)
              (i32.const 10)
             )
            )
            (br_if $while-in19
             (i32.ne
              (tee_local $8
               (i32.add
                (get_local $8)
                (i32.const 1)
               )
              )
              (i32.const 9)
             )
            )
           )
          )
          (set_local $6
           (i32.const 10)
          )
         )
         (set_local $13
          (i32.rem_u
           (tee_local $21
            (i32.load
             (tee_local $8
              (i32.add
               (i32.add
                (get_local $9)
                (i32.shl
                 (get_local $13)
                 (i32.const 2)
                )
               )
               (i32.const -4092)
              )
             )
            )
           )
           (get_local $6)
          )
         )
         (if
          (i32.eqz
           (i32.and
            (tee_local $23
             (i32.eq
              (i32.add
               (get_local $8)
               (i32.const 4)
              )
              (get_local $10)
             )
            )
            (i32.eqz
             (get_local $13)
            )
           )
          )
          (block
           (set_local $25
            (if (result f64)
             (i32.and
              (i32.div_u
               (get_local $21)
               (get_local $6)
              )
              (i32.const 1)
             )
             (f64.const 9007199254740994)
             (f64.const 9007199254740992)
            )
           )
           (set_local $29
            (i32.lt_u
             (get_local $13)
             (tee_local $26
              (i32.div_s
               (get_local $6)
               (i32.const 2)
              )
             )
            )
           )
           (set_local $1
            (if (result f64)
             (i32.and
              (get_local $23)
              (i32.eq
               (get_local $13)
               (get_local $26)
              )
             )
             (f64.const 1)
             (f64.const 1.5)
            )
           )
           (if
            (get_local $29)
            (set_local $1
             (f64.const 0.5)
            )
           )
           (set_local $1
            (if (result f64)
             (get_local $18)
             (block (result f64)
              (set_local $16
               (f64.neg
                (get_local $25)
               )
              )
              (set_local $30
               (f64.neg
                (get_local $1)
               )
              )
              (if
               (tee_local $23
                (i32.eq
                 (i32.load8_s
                  (get_local $15)
                 )
                 (i32.const 45)
                )
               )
               (set_local $25
                (get_local $16)
               )
              )
              (set_local $16
               (if (result f64)
                (get_local $23)
                (get_local $30)
                (get_local $1)
               )
              )
              (get_local $25)
             )
             (block (result f64)
              (set_local $16
               (get_local $1)
              )
              (get_local $25)
             )
            )
           )
           (i32.store
            (get_local $8)
            (tee_local $13
             (i32.sub
              (get_local $21)
              (get_local $13)
             )
            )
           )
           (if
            (f64.ne
             (f64.add
              (get_local $1)
              (get_local $16)
             )
             (get_local $1)
            )
            (block
             (i32.store
              (get_local $8)
              (tee_local $7
               (i32.add
                (get_local $13)
                (get_local $6)
               )
              )
             )
             (if
              (i32.gt_u
               (get_local $7)
               (i32.const 999999999)
              )
              (loop $while-in21
               (i32.store
                (get_local $8)
                (i32.const 0)
               )
               (if
                (i32.lt_u
                 (tee_local $8
                  (i32.add
                   (get_local $8)
                   (i32.const -4)
                  )
                 )
                 (get_local $3)
                )
                (i32.store
                 (tee_local $3
                  (i32.add
                   (get_local $3)
                   (i32.const -4)
                  )
                 )
                 (i32.const 0)
                )
               )
               (i32.store
                (get_local $8)
                (tee_local $7
                 (i32.add
                  (i32.load
                   (get_local $8)
                  )
                  (i32.const 1)
                 )
                )
               )
               (br_if $while-in21
                (i32.gt_u
                 (get_local $7)
                 (i32.const 999999999)
                )
               )
              )
             )
             (set_local $7
              (i32.mul
               (i32.shr_s
                (i32.sub
                 (get_local $12)
                 (get_local $3)
                )
                (i32.const 2)
               )
               (i32.const 9)
              )
             )
             (if
              (i32.ge_u
               (tee_local $13
                (i32.load
                 (get_local $3)
                )
               )
               (i32.const 10)
              )
              (block
               (set_local $6
                (i32.const 10)
               )
               (loop $while-in23
                (set_local $7
                 (i32.add
                  (get_local $7)
                  (i32.const 1)
                 )
                )
                (br_if $while-in23
                 (i32.ge_u
                  (get_local $13)
                  (tee_local $6
                   (i32.mul
                    (get_local $6)
                    (i32.const 10)
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
         )
         (set_local $6
          (get_local $7)
         )
         (if
          (i32.le_u
           (get_local $10)
           (tee_local $7
            (i32.add
             (get_local $8)
             (i32.const 4)
            )
           )
          )
          (set_local $7
           (get_local $10)
          )
         )
         (get_local $3)
        )
        (block (result i32)
         (set_local $6
          (get_local $7)
         )
         (set_local $7
          (get_local $10)
         )
         (get_local $3)
        )
       )
      )
      (loop $while-in25
       (block $while-out24
        (if
         (i32.le_u
          (get_local $7)
          (get_local $8)
         )
         (block
          (set_local $13
           (i32.const 0)
          )
          (br $while-out24)
         )
        )
        (if
         (i32.load
          (tee_local $3
           (i32.add
            (get_local $7)
            (i32.const -4)
           )
          )
         )
         (set_local $13
          (i32.const 1)
         )
         (block
          (set_local $7
           (get_local $3)
          )
          (br $while-in25)
         )
        )
       )
      )
      (set_local $21
       (i32.sub
        (i32.const 0)
        (get_local $6)
       )
      )
      (if
       (get_local $17)
       (block
        (set_local $3
         (if (result i32)
          (i32.and
           (i32.gt_s
            (tee_local $3
             (i32.add
              (i32.xor
               (get_local $24)
               (i32.const 1)
              )
              (get_local $11)
             )
            )
            (get_local $6)
           )
           (i32.gt_s
            (get_local $6)
            (i32.const -5)
           )
          )
          (block (result i32)
           (set_local $5
            (i32.add
             (get_local $5)
             (i32.const -1)
            )
           )
           (i32.sub
            (i32.add
             (get_local $3)
             (i32.const -1)
            )
            (get_local $6)
           )
          )
          (block (result i32)
           (set_local $5
            (i32.add
             (get_local $5)
             (i32.const -2)
            )
           )
           (i32.add
            (get_local $3)
            (i32.const -1)
           )
          )
         )
        )
        (if
         (i32.eqz
          (tee_local $11
           (i32.and
            (get_local $4)
            (i32.const 8)
           )
          )
         )
         (block
          (if
           (get_local $13)
           (if
            (tee_local $17
             (i32.load
              (i32.add
               (get_local $7)
               (i32.const -4)
              )
             )
            )
            (if
             (i32.rem_u
              (get_local $17)
              (i32.const 10)
             )
             (set_local $10
              (i32.const 0)
             )
             (block
              (set_local $10
               (i32.const 0)
              )
              (set_local $11
               (i32.const 10)
              )
              (loop $while-in29
               (set_local $10
                (i32.add
                 (get_local $10)
                 (i32.const 1)
                )
               )
               (br_if $while-in29
                (i32.eqz
                 (i32.rem_u
                  (get_local $17)
                  (tee_local $11
                   (i32.mul
                    (get_local $11)
                    (i32.const 10)
                   )
                  )
                 )
                )
               )
              )
             )
            )
            (set_local $10
             (i32.const 9)
            )
           )
           (set_local $10
            (i32.const 9)
           )
          )
          (set_local $11
           (i32.add
            (i32.mul
             (i32.shr_s
              (i32.sub
               (get_local $7)
               (get_local $12)
              )
              (i32.const 2)
             )
             (i32.const 9)
            )
            (i32.const -9)
           )
          )
          (set_local $11
           (if (result i32)
            (i32.eq
             (i32.or
              (get_local $5)
              (i32.const 32)
             )
             (i32.const 102)
            )
            (block (result i32)
             (if
              (i32.ge_s
               (get_local $3)
               (if (result i32)
                (i32.gt_s
                 (tee_local $10
                  (i32.sub
                   (get_local $11)
                   (get_local $10)
                  )
                 )
                 (i32.const 0)
                )
                (get_local $10)
                (tee_local $10
                 (i32.const 0)
                )
               )
              )
              (set_local $3
               (get_local $10)
              )
             )
             (i32.const 0)
            )
            (block (result i32)
             (if
              (i32.ge_s
               (get_local $3)
               (if (result i32)
                (i32.gt_s
                 (tee_local $10
                  (i32.sub
                   (i32.add
                    (get_local $11)
                    (get_local $6)
                   )
                   (get_local $10)
                  )
                 )
                 (i32.const 0)
                )
                (get_local $10)
                (tee_local $10
                 (i32.const 0)
                )
               )
              )
              (set_local $3
               (get_local $10)
              )
             )
             (i32.const 0)
            )
           )
          )
         )
        )
       )
       (block
        (set_local $3
         (get_local $11)
        )
        (set_local $11
         (i32.and
          (get_local $4)
          (i32.const 8)
         )
        )
       )
      )
      (if
       (tee_local $17
        (i32.eq
         (i32.or
          (get_local $5)
          (i32.const 32)
         )
         (i32.const 102)
        )
       )
       (block
        (set_local $10
         (i32.const 0)
        )
        (if
         (i32.le_s
          (get_local $6)
          (i32.const 0)
         )
         (set_local $6
          (i32.const 0)
         )
        )
       )
       (block
        (if
         (i32.lt_s
          (i32.sub
           (tee_local $12
            (get_local $19)
           )
           (tee_local $10
            (call $_fmt_u
             (i64.extend_s/i32
              (if (result i32)
               (i32.lt_s
                (get_local $6)
                (i32.const 0)
               )
               (get_local $21)
               (get_local $6)
              )
             )
             (get_local $19)
            )
           )
          )
          (i32.const 2)
         )
         (loop $while-in31
          (i32.store8
           (tee_local $10
            (i32.add
             (get_local $10)
             (i32.const -1)
            )
           )
           (i32.const 48)
          )
          (br_if $while-in31
           (i32.lt_s
            (i32.sub
             (get_local $12)
             (get_local $10)
            )
            (i32.const 2)
           )
          )
         )
        )
        (i32.store8
         (i32.add
          (get_local $10)
          (i32.const -1)
         )
         (i32.add
          (i32.and
           (i32.shr_s
            (get_local $6)
            (i32.const 31)
           )
           (i32.const 2)
          )
          (i32.const 43)
         )
        )
        (i32.store8
         (tee_local $6
          (i32.add
           (get_local $10)
           (i32.const -2)
          )
         )
         (get_local $5)
        )
        (set_local $10
         (get_local $6)
        )
        (set_local $6
         (i32.sub
          (get_local $12)
          (get_local $6)
         )
        )
       )
      )
      (call $_pad
       (get_local $0)
       (i32.const 32)
       (get_local $2)
       (tee_local $6
        (i32.add
         (i32.add
          (i32.add
           (i32.add
            (get_local $18)
            (i32.const 1)
           )
           (get_local $3)
          )
          (i32.ne
           (tee_local $12
            (i32.or
             (get_local $3)
             (get_local $11)
            )
           )
           (i32.const 0)
          )
         )
         (get_local $6)
        )
       )
       (get_local $4)
      )
      (call $_out
       (get_local $0)
       (get_local $15)
       (get_local $18)
      )
      (call $_pad
       (get_local $0)
       (i32.const 48)
       (get_local $2)
       (get_local $6)
       (i32.xor
        (get_local $4)
        (i32.const 65536)
       )
      )
      (if
       (get_local $17)
       (block
        (set_local $19
         (tee_local $11
          (i32.add
           (get_local $14)
           (i32.const 9)
          )
         )
        )
        (set_local $10
         (i32.add
          (get_local $14)
          (i32.const 8)
         )
        )
        (set_local $8
         (tee_local $15
          (if (result i32)
           (i32.gt_u
            (get_local $8)
            (get_local $9)
           )
           (get_local $9)
           (get_local $8)
          )
         )
        )
        (loop $while-in33
         (set_local $5
          (call $_fmt_u
           (i64.extend_u/i32
            (i32.load
             (get_local $8)
            )
           )
           (get_local $11)
          )
         )
         (if
          (i32.eq
           (get_local $8)
           (get_local $15)
          )
          (if
           (i32.eq
            (get_local $5)
            (get_local $11)
           )
           (block
            (i32.store8
             (get_local $10)
             (i32.const 48)
            )
            (set_local $5
             (get_local $10)
            )
           )
          )
          (if
           (i32.gt_u
            (get_local $5)
            (get_local $14)
           )
           (block
            (drop
             (call $_memset
              (get_local $14)
              (i32.const 48)
              (i32.sub
               (get_local $5)
               (get_local $20)
              )
             )
            )
            (loop $while-in35
             (br_if $while-in35
              (i32.gt_u
               (tee_local $5
                (i32.add
                 (get_local $5)
                 (i32.const -1)
                )
               )
               (get_local $14)
              )
             )
            )
           )
          )
         )
         (call $_out
          (get_local $0)
          (get_local $5)
          (i32.sub
           (get_local $19)
           (get_local $5)
          )
         )
         (if
          (i32.le_u
           (tee_local $5
            (i32.add
             (get_local $8)
             (i32.const 4)
            )
           )
           (get_local $9)
          )
          (block
           (set_local $8
            (get_local $5)
           )
           (br $while-in33)
          )
         )
        )
        (if
         (get_local $12)
         (call $_out
          (get_local $0)
          (i32.const 3840)
          (i32.const 1)
         )
        )
        (if
         (i32.and
          (i32.lt_u
           (get_local $5)
           (get_local $7)
          )
          (i32.gt_s
           (get_local $3)
           (i32.const 0)
          )
         )
         (loop $while-in37
          (if
           (i32.gt_u
            (tee_local $9
             (call $_fmt_u
              (i64.extend_u/i32
               (i32.load
                (get_local $5)
               )
              )
              (get_local $11)
             )
            )
            (get_local $14)
           )
           (block
            (drop
             (call $_memset
              (get_local $14)
              (i32.const 48)
              (i32.sub
               (get_local $9)
               (get_local $20)
              )
             )
            )
            (loop $while-in39
             (br_if $while-in39
              (i32.gt_u
               (tee_local $9
                (i32.add
                 (get_local $9)
                 (i32.const -1)
                )
               )
               (get_local $14)
              )
             )
            )
           )
          )
          (call $_out
           (get_local $0)
           (get_local $9)
           (if (result i32)
            (i32.lt_s
             (get_local $3)
             (i32.const 9)
            )
            (get_local $3)
            (i32.const 9)
           )
          )
          (set_local $9
           (i32.add
            (get_local $3)
            (i32.const -9)
           )
          )
          (if
           (i32.and
            (i32.lt_u
             (tee_local $5
              (i32.add
               (get_local $5)
               (i32.const 4)
              )
             )
             (get_local $7)
            )
            (i32.gt_s
             (get_local $3)
             (i32.const 9)
            )
           )
           (block
            (set_local $3
             (get_local $9)
            )
            (br $while-in37)
           )
           (set_local $3
            (get_local $9)
           )
          )
         )
        )
        (call $_pad
         (get_local $0)
         (i32.const 48)
         (i32.add
          (get_local $3)
          (i32.const 9)
         )
         (i32.const 9)
         (i32.const 0)
        )
       )
       (block
        (set_local $5
         (i32.add
          (get_local $8)
          (i32.const 4)
         )
        )
        (set_local $12
         (if (result i32)
          (get_local $13)
          (get_local $7)
          (get_local $5)
         )
        )
        (if
         (i32.gt_s
          (get_local $3)
          (i32.const -1)
         )
         (block
          (set_local $18
           (i32.eqz
            (get_local $11)
           )
          )
          (set_local $13
           (tee_local $11
            (i32.add
             (get_local $14)
             (i32.const 9)
            )
           )
          )
          (set_local $20
           (i32.sub
            (i32.const 0)
            (get_local $20)
           )
          )
          (set_local $15
           (i32.add
            (get_local $14)
            (i32.const 8)
           )
          )
          (set_local $5
           (get_local $3)
          )
          (set_local $9
           (get_local $8)
          )
          (loop $while-in41
           (if
            (i32.eq
             (tee_local $3
              (call $_fmt_u
               (i64.extend_u/i32
                (i32.load
                 (get_local $9)
                )
               )
               (get_local $11)
              )
             )
             (get_local $11)
            )
            (block
             (i32.store8
              (get_local $15)
              (i32.const 48)
             )
             (set_local $3
              (get_local $15)
             )
            )
           )
           (block $do-once42
            (if
             (i32.eq
              (get_local $9)
              (get_local $8)
             )
             (block
              (set_local $7
               (i32.add
                (get_local $3)
                (i32.const 1)
               )
              )
              (call $_out
               (get_local $0)
               (get_local $3)
               (i32.const 1)
              )
              (if
               (i32.and
                (get_local $18)
                (i32.lt_s
                 (get_local $5)
                 (i32.const 1)
                )
               )
               (block
                (set_local $3
                 (get_local $7)
                )
                (br $do-once42)
               )
              )
              (call $_out
               (get_local $0)
               (i32.const 3840)
               (i32.const 1)
              )
              (set_local $3
               (get_local $7)
              )
             )
             (block
              (br_if $do-once42
               (i32.le_u
                (get_local $3)
                (get_local $14)
               )
              )
              (drop
               (call $_memset
                (get_local $14)
                (i32.const 48)
                (i32.add
                 (get_local $3)
                 (get_local $20)
                )
               )
              )
              (loop $while-in45
               (br_if $while-in45
                (i32.gt_u
                 (tee_local $3
                  (i32.add
                   (get_local $3)
                   (i32.const -1)
                  )
                 )
                 (get_local $14)
                )
               )
              )
             )
            )
           )
           (call $_out
            (get_local $0)
            (get_local $3)
            (if (result i32)
             (i32.gt_s
              (get_local $5)
              (tee_local $3
               (i32.sub
                (get_local $13)
                (get_local $3)
               )
              )
             )
             (get_local $3)
             (get_local $5)
            )
           )
           (br_if $while-in41
            (i32.and
             (i32.lt_u
              (tee_local $9
               (i32.add
                (get_local $9)
                (i32.const 4)
               )
              )
              (get_local $12)
             )
             (i32.gt_s
              (tee_local $5
               (i32.sub
                (get_local $5)
                (get_local $3)
               )
              )
              (i32.const -1)
             )
            )
           )
           (set_local $3
            (get_local $5)
           )
          )
         )
        )
        (call $_pad
         (get_local $0)
         (i32.const 48)
         (i32.add
          (get_local $3)
          (i32.const 18)
         )
         (i32.const 18)
         (i32.const 0)
        )
        (call $_out
         (get_local $0)
         (get_local $10)
         (i32.sub
          (get_local $19)
          (get_local $10)
         )
        )
       )
      )
      (call $_pad
       (get_local $0)
       (i32.const 32)
       (get_local $2)
       (get_local $6)
       (i32.xor
        (get_local $4)
        (i32.const 8192)
       )
      )
      (get_local $6)
     )
     (block (result i32)
      (set_local $5
       (if (result i32)
        (tee_local $3
         (i32.ne
          (i32.and
           (get_local $5)
           (i32.const 32)
          )
          (i32.const 0)
         )
        )
        (i32.const 3808)
        (i32.const 3812)
       )
      )
      (set_local $9
       (i32.or
        (f64.ne
         (get_local $1)
         (get_local $1)
        )
        (i32.const 0)
       )
      )
      (set_local $7
       (if (result i32)
        (get_local $3)
        (i32.const 3816)
        (i32.const 3820)
       )
      )
      (call $_pad
       (get_local $0)
       (i32.const 32)
       (get_local $2)
       (tee_local $3
        (i32.add
         (get_local $18)
         (i32.const 3)
        )
       )
       (i32.and
        (get_local $4)
        (i32.const -65537)
       )
      )
      (call $_out
       (get_local $0)
       (get_local $15)
       (get_local $18)
      )
      (call $_out
       (get_local $0)
       (if (result i32)
        (get_local $9)
        (get_local $7)
        (get_local $5)
       )
       (i32.const 3)
      )
      (call $_pad
       (get_local $0)
       (i32.const 32)
       (get_local $2)
       (get_local $3)
       (i32.xor
        (get_local $4)
        (i32.const 8192)
       )
      )
      (get_local $3)
     )
    )
   )
  )
  (set_global $STACKTOP
   (get_local $22)
  )
  (if (result i32)
   (i32.lt_s
    (get_local $0)
    (get_local $2)
   )
   (get_local $2)
   (get_local $0)
  )
 )
 (func $___DOUBLE_BITS_536 (param $0 f64) (result i64)
  (i64.reinterpret/f64
   (get_local $0)
  )
 )
 (func $_frexpl (param $0 f64) (param $1 i32) (result f64)
  (call $_frexp
   (get_local $0)
   (get_local $1)
  )
 )
 (func $_frexp (param $0 f64) (param $1 i32) (result f64)
  (local $2 i64)
  (local $3 i64)
  (block $switch
   (block $switch-default
    (block $switch-case0
     (block $switch-case
      (br_table $switch-case $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-default $switch-case0 $switch-default
       (i32.and
        (i32.wrap/i64
         (tee_local $3
          (i64.shr_u
           (tee_local $2
            (i64.reinterpret/f64
             (get_local $0)
            )
           )
           (i64.const 52)
          )
         )
        )
        (i32.const 2047)
       )
      )
     )
     (i32.store
      (get_local $1)
      (if (result i32)
       (f64.ne
        (get_local $0)
        (f64.const 0)
       )
       (block (result i32)
        (set_local $0
         (call $_frexp
          (f64.mul
           (get_local $0)
           (f64.const 18446744073709551615)
          )
          (get_local $1)
         )
        )
        (i32.add
         (i32.load
          (get_local $1)
         )
         (i32.const -64)
        )
       )
       (i32.const 0)
      )
     )
     (br $switch)
    )
    (br $switch)
   )
   (i32.store
    (get_local $1)
    (i32.add
     (i32.and
      (i32.wrap/i64
       (get_local $3)
      )
      (i32.const 2047)
     )
     (i32.const -1022)
    )
   )
   (set_local $0
    (f64.reinterpret/i64
     (i64.or
      (i64.and
       (get_local $2)
       (i64.const -9218868437227405313)
      )
      (i64.const 4602678819172646912)
     )
    )
   )
  )
  (get_local $0)
 )
 (func $_wcrtomb (param $0 i32) (param $1 i32) (param $2 i32) (result i32)
  (block $do-once (result i32)
   (if (result i32)
    (get_local $0)
    (block (result i32)
     (if
      (i32.lt_u
       (get_local $1)
       (i32.const 128)
      )
      (block
       (i32.store8
        (get_local $0)
        (get_local $1)
       )
       (br $do-once
        (i32.const 1)
       )
      )
     )
     (if
      (i32.eqz
       (i32.load
        (i32.load offset=188
         (call $___pthread_self_304)
        )
       )
      )
      (if
       (i32.eq
        (i32.and
         (get_local $1)
         (i32.const -128)
        )
        (i32.const 57216)
       )
       (block
        (i32.store8
         (get_local $0)
         (get_local $1)
        )
        (br $do-once
         (i32.const 1)
        )
       )
       (block
        (i32.store
         (call $___errno_location)
         (i32.const 84)
        )
        (br $do-once
         (i32.const -1)
        )
       )
      )
     )
     (if
      (i32.lt_u
       (get_local $1)
       (i32.const 2048)
      )
      (block
       (i32.store8
        (get_local $0)
        (i32.or
         (i32.shr_u
          (get_local $1)
          (i32.const 6)
         )
         (i32.const 192)
        )
       )
       (i32.store8 offset=1
        (get_local $0)
        (i32.or
         (i32.and
          (get_local $1)
          (i32.const 63)
         )
         (i32.const 128)
        )
       )
       (br $do-once
        (i32.const 2)
       )
      )
     )
     (if
      (i32.or
       (i32.lt_u
        (get_local $1)
        (i32.const 55296)
       )
       (i32.eq
        (i32.and
         (get_local $1)
         (i32.const -8192)
        )
        (i32.const 57344)
       )
      )
      (block
       (i32.store8
        (get_local $0)
        (i32.or
         (i32.shr_u
          (get_local $1)
          (i32.const 12)
         )
         (i32.const 224)
        )
       )
       (i32.store8 offset=1
        (get_local $0)
        (i32.or
         (i32.and
          (i32.shr_u
           (get_local $1)
           (i32.const 6)
          )
          (i32.const 63)
         )
         (i32.const 128)
        )
       )
       (i32.store8 offset=2
        (get_local $0)
        (i32.or
         (i32.and
          (get_local $1)
          (i32.const 63)
         )
         (i32.const 128)
        )
       )
       (br $do-once
        (i32.const 3)
       )
      )
     )
     (if (result i32)
      (i32.lt_u
       (i32.add
        (get_local $1)
        (i32.const -65536)
       )
       (i32.const 1048576)
      )
      (block (result i32)
       (i32.store8
        (get_local $0)
        (i32.or
         (i32.shr_u
          (get_local $1)
          (i32.const 18)
         )
         (i32.const 240)
        )
       )
       (i32.store8 offset=1
        (get_local $0)
        (i32.or
         (i32.and
          (i32.shr_u
           (get_local $1)
           (i32.const 12)
          )
          (i32.const 63)
         )
         (i32.const 128)
        )
       )
       (i32.store8 offset=2
        (get_local $0)
        (i32.or
         (i32.and
          (i32.shr_u
           (get_local $1)
           (i32.const 6)
          )
          (i32.const 63)
         )
         (i32.const 128)
        )
       )
       (i32.store8 offset=3
        (get_local $0)
        (i32.or
         (i32.and
          (get_local $1)
          (i32.const 63)
         )
         (i32.const 128)
        )
       )
       (i32.const 4)
      )
      (block (result i32)
       (i32.store
        (call $___errno_location)
        (i32.const 84)
       )
       (i32.const -1)
      )
     )
    )
    (i32.const 1)
   )
  )
 )
 (func $___fwritex (param $0 i32) (param $1 i32) (param $2 i32) (result i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (local $6 i32)
  (block $label$break$L5
   (block $__rjti$0
    (br_if $__rjti$0
     (tee_local $3
      (i32.load
       (tee_local $4
        (i32.add
         (get_local $2)
         (i32.const 16)
        )
       )
      )
     )
    )
    (if
     (call $___towrite
      (get_local $2)
     )
     (set_local $2
      (i32.const 0)
     )
     (block
      (set_local $3
       (i32.load
        (get_local $4)
       )
      )
      (br $__rjti$0)
     )
    )
    (br $label$break$L5)
   )
   (if
    (i32.lt_u
     (i32.sub
      (get_local $3)
      (tee_local $4
       (i32.load
        (tee_local $5
         (i32.add
          (get_local $2)
          (i32.const 20)
         )
        )
       )
      )
     )
     (get_local $1)
    )
    (block
     (set_local $2
      (call_indirect $FUNCSIG$iiii
       (get_local $2)
       (get_local $0)
       (get_local $1)
       (i32.add
        (i32.and
         (i32.load offset=36
          (get_local $2)
         )
         (i32.const 3)
        )
        (i32.const 2)
       )
      )
     )
     (br $label$break$L5)
    )
   )
   (set_local $2
    (block $label$break$L10 (result i32)
     (if (result i32)
      (i32.gt_s
       (i32.load8_s offset=75
        (get_local $2)
       )
       (i32.const -1)
      )
      (block (result i32)
       (set_local $3
        (get_local $1)
       )
       (loop $while-in
        (drop
         (br_if $label$break$L10
          (i32.const 0)
          (i32.eqz
           (get_local $3)
          )
         )
        )
        (if
         (i32.ne
          (i32.load8_s
           (i32.add
            (get_local $0)
            (tee_local $6
             (i32.add
              (get_local $3)
              (i32.const -1)
             )
            )
           )
          )
          (i32.const 10)
         )
         (block
          (set_local $3
           (get_local $6)
          )
          (br $while-in)
         )
        )
       )
       (br_if $label$break$L5
        (i32.lt_u
         (tee_local $2
          (call_indirect $FUNCSIG$iiii
           (get_local $2)
           (get_local $0)
           (get_local $3)
           (i32.add
            (i32.and
             (i32.load offset=36
              (get_local $2)
             )
             (i32.const 3)
            )
            (i32.const 2)
           )
          )
         )
         (get_local $3)
        )
       )
       (set_local $0
        (i32.add
         (get_local $0)
         (get_local $3)
        )
       )
       (set_local $1
        (i32.sub
         (get_local $1)
         (get_local $3)
        )
       )
       (set_local $4
        (i32.load
         (get_local $5)
        )
       )
       (get_local $3)
      )
      (i32.const 0)
     )
    )
   )
   (drop
    (call $_memcpy
     (get_local $4)
     (get_local $0)
     (get_local $1)
    )
   )
   (i32.store
    (get_local $5)
    (i32.add
     (i32.load
      (get_local $5)
     )
     (get_local $1)
    )
   )
   (set_local $2
    (i32.add
     (get_local $2)
     (get_local $1)
    )
   )
  )
  (get_local $2)
 )
 (func $___towrite (param $0 i32) (result i32)
  (local $1 i32)
  (local $2 i32)
  (set_local $1
   (i32.load8_s
    (tee_local $2
     (i32.add
      (get_local $0)
      (i32.const 74)
     )
    )
   )
  )
  (i32.store8
   (get_local $2)
   (i32.or
    (i32.add
     (get_local $1)
     (i32.const 255)
    )
    (get_local $1)
   )
  )
  (tee_local $0
   (if (result i32)
    (i32.and
     (tee_local $1
      (i32.load
       (get_local $0)
      )
     )
     (i32.const 8)
    )
    (block (result i32)
     (i32.store
      (get_local $0)
      (i32.or
       (get_local $1)
       (i32.const 32)
      )
     )
     (i32.const -1)
    )
    (block (result i32)
     (i32.store offset=8
      (get_local $0)
      (i32.const 0)
     )
     (i32.store offset=4
      (get_local $0)
      (i32.const 0)
     )
     (i32.store offset=28
      (get_local $0)
      (tee_local $1
       (i32.load offset=44
        (get_local $0)
       )
      )
     )
     (i32.store offset=20
      (get_local $0)
      (get_local $1)
     )
     (i32.store offset=16
      (get_local $0)
      (i32.add
       (get_local $1)
       (i32.load offset=48
        (get_local $0)
       )
      )
     )
     (i32.const 0)
    )
   )
  )
 )
 (func $___ofl_lock (result i32)
  (call $___lock
   (i32.const 3908)
  )
  (i32.const 3916)
 )
 (func $___ofl_unlock
  (call $___unlock
   (i32.const 3908)
  )
 )
 (func $_fflush (param $0 i32) (result i32)
  (local $1 i32)
  (local $2 i32)
  (block $do-once
   (if
    (get_local $0)
    (block
     (if
      (i32.le_s
       (i32.load offset=76
        (get_local $0)
       )
       (i32.const -1)
      )
      (block
       (set_local $0
        (call $___fflush_unlocked
         (get_local $0)
        )
       )
       (br $do-once)
      )
     )
     (set_local $2
      (i32.eqz
       (call $___lockfile
        (get_local $0)
       )
      )
     )
     (set_local $1
      (call $___fflush_unlocked
       (get_local $0)
      )
     )
     (set_local $0
      (if (result i32)
       (get_local $2)
       (get_local $1)
       (block (result i32)
        (call $___unlockfile
         (get_local $0)
        )
        (get_local $1)
       )
      )
     )
    )
    (block
     (set_local $0
      (if (result i32)
       (i32.load
        (i32.const 1396)
       )
       (call $_fflush
        (i32.load
         (i32.const 1396)
        )
       )
       (i32.const 0)
      )
     )
     (if
      (tee_local $1
       (i32.load
        (call $___ofl_lock)
       )
      )
      (loop $while-in
       (set_local $2
        (if (result i32)
         (i32.gt_s
          (i32.load offset=76
           (get_local $1)
          )
          (i32.const -1)
         )
         (call $___lockfile
          (get_local $1)
         )
         (i32.const 0)
        )
       )
       (if
        (i32.gt_u
         (i32.load offset=20
          (get_local $1)
         )
         (i32.load offset=28
          (get_local $1)
         )
        )
        (set_local $0
         (i32.or
          (call $___fflush_unlocked
           (get_local $1)
          )
          (get_local $0)
         )
        )
       )
       (if
        (get_local $2)
        (call $___unlockfile
         (get_local $1)
        )
       )
       (br_if $while-in
        (tee_local $1
         (i32.load offset=56
          (get_local $1)
         )
        )
       )
      )
     )
     (call $___ofl_unlock)
    )
   )
  )
  (get_local $0)
 )
 (func $___fflush_unlocked (param $0 i32) (result i32)
  (local $1 i32)
  (local $2 i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (local $6 i32)
  (tee_local $0
   (block $__rjto$0 (result i32)
    (block $__rjti$0
     (br_if $__rjti$0
      (i32.le_u
       (i32.load
        (tee_local $1
         (i32.add
          (get_local $0)
          (i32.const 20)
         )
        )
       )
       (i32.load
        (tee_local $2
         (i32.add
          (get_local $0)
          (i32.const 28)
         )
        )
       )
      )
     )
     (drop
      (call_indirect $FUNCSIG$iiii
       (get_local $0)
       (i32.const 0)
       (i32.const 0)
       (i32.add
        (i32.and
         (i32.load offset=36
          (get_local $0)
         )
         (i32.const 3)
        )
        (i32.const 2)
       )
      )
     )
     (br_if $__rjti$0
      (i32.load
       (get_local $1)
      )
     )
     (br $__rjto$0
      (i32.const -1)
     )
    )
    (if
     (i32.lt_u
      (tee_local $4
       (i32.load
        (tee_local $3
         (i32.add
          (get_local $0)
          (i32.const 4)
         )
        )
       )
      )
      (tee_local $6
       (i32.load
        (tee_local $5
         (i32.add
          (get_local $0)
          (i32.const 8)
         )
        )
       )
      )
     )
     (drop
      (call_indirect $FUNCSIG$iiii
       (get_local $0)
       (i32.sub
        (get_local $4)
        (get_local $6)
       )
       (i32.const 1)
       (i32.add
        (i32.and
         (i32.load offset=40
          (get_local $0)
         )
         (i32.const 3)
        )
        (i32.const 2)
       )
      )
     )
    )
    (i32.store offset=16
     (get_local $0)
     (i32.const 0)
    )
    (i32.store
     (get_local $2)
     (i32.const 0)
    )
    (i32.store
     (get_local $1)
     (i32.const 0)
    )
    (i32.store
     (get_local $5)
     (i32.const 0)
    )
    (i32.store
     (get_local $3)
     (i32.const 0)
    )
    (i32.const 0)
   )
  )
 )
 (func $___overflow (param $0 i32) (param $1 i32) (result i32)
  (local $2 i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (local $6 i32)
  (local $7 i32)
  (set_local $3
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 16)
   )
  )
  (i32.store8
   (tee_local $4
    (get_local $3)
   )
   (tee_local $7
    (i32.and
     (get_local $1)
     (i32.const 255)
    )
   )
  )
  (block $do-once
   (block $__rjti$0
    (br_if $__rjti$0
     (tee_local $5
      (i32.load
       (tee_local $2
        (i32.add
         (get_local $0)
         (i32.const 16)
        )
       )
      )
     )
    )
    (if
     (call $___towrite
      (get_local $0)
     )
     (set_local $1
      (i32.const -1)
     )
     (block
      (set_local $5
       (i32.load
        (get_local $2)
       )
      )
      (br $__rjti$0)
     )
    )
    (br $do-once)
   )
   (if
    (i32.lt_u
     (tee_local $6
      (i32.load
       (tee_local $2
        (i32.add
         (get_local $0)
         (i32.const 20)
        )
       )
      )
     )
     (get_local $5)
    )
    (if
     (i32.ne
      (tee_local $1
       (i32.and
        (get_local $1)
        (i32.const 255)
       )
      )
      (i32.load8_s offset=75
       (get_local $0)
      )
     )
     (block
      (i32.store
       (get_local $2)
       (i32.add
        (get_local $6)
        (i32.const 1)
       )
      )
      (i32.store8
       (get_local $6)
       (get_local $7)
      )
      (br $do-once)
     )
    )
   )
   (set_local $1
    (if (result i32)
     (i32.eq
      (call_indirect $FUNCSIG$iiii
       (get_local $0)
       (get_local $4)
       (i32.const 1)
       (i32.add
        (i32.and
         (i32.load offset=36
          (get_local $0)
         )
         (i32.const 3)
        )
        (i32.const 2)
       )
      )
      (i32.const 1)
     )
     (i32.load8_u
      (get_local $4)
     )
     (i32.const -1)
    )
   )
  )
  (set_global $STACKTOP
   (get_local $3)
  )
  (get_local $1)
 )
 (func $_fputc (param $0 i32) (param $1 i32) (result i32)
  (local $2 i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (set_local $5
   (i32.and
    (get_local $0)
    (i32.const 255)
   )
  )
  (set_local $2
   (i32.and
    (get_local $0)
    (i32.const 255)
   )
  )
  (block $do-once
   (block $__rjti$1
    (br_if $__rjti$1
     (i32.lt_s
      (i32.load offset=76
       (get_local $1)
      )
      (i32.const 0)
     )
    )
    (br_if $__rjti$1
     (i32.eqz
      (call $___lockfile
       (get_local $1)
      )
     )
    )
    (block $__rjto$0
     (block $__rjti$0
      (br_if $__rjti$0
       (i32.eq
        (get_local $2)
        (i32.load8_s offset=75
         (get_local $1)
        )
       )
      )
      (br_if $__rjti$0
       (i32.ge_u
        (tee_local $3
         (i32.load
          (tee_local $4
           (i32.add
            (get_local $1)
            (i32.const 20)
           )
          )
         )
        )
        (i32.load offset=16
         (get_local $1)
        )
       )
      )
      (i32.store
       (get_local $4)
       (i32.add
        (get_local $3)
        (i32.const 1)
       )
      )
      (i32.store8
       (get_local $3)
       (get_local $5)
      )
      (br $__rjto$0)
     )
     (set_local $2
      (call $___overflow
       (get_local $1)
       (get_local $0)
      )
     )
    )
    (call $___unlockfile
     (get_local $1)
    )
    (br $do-once)
   )
   (if
    (i32.ne
     (get_local $2)
     (i32.load8_s offset=75
      (get_local $1)
     )
    )
    (if
     (i32.lt_u
      (tee_local $3
       (i32.load
        (tee_local $4
         (i32.add
          (get_local $1)
          (i32.const 20)
         )
        )
       )
      )
      (i32.load offset=16
       (get_local $1)
      )
     )
     (block
      (i32.store
       (get_local $4)
       (i32.add
        (get_local $3)
        (i32.const 1)
       )
      )
      (i32.store8
       (get_local $3)
       (get_local $5)
      )
      (br $do-once)
     )
    )
   )
   (set_local $2
    (call $___overflow
     (get_local $1)
     (get_local $0)
    )
   )
  )
  (get_local $2)
 )
 (func $_printf (param $0 i32) (param $1 i32) (result i32)
  (local $2 i32)
  (local $3 i32)
  (set_local $2
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 16)
   )
  )
  (i32.store
   (tee_local $3
    (get_local $2)
   )
   (get_local $1)
  )
  (set_local $0
   (call $_vfprintf
    (i32.load
     (i32.const 1268)
    )
    (get_local $0)
    (get_local $3)
   )
  )
  (set_global $STACKTOP
   (get_local $2)
  )
  (get_local $0)
 )
 (func $_fputs (param $0 i32) (param $1 i32) (result i32)
  (local $2 i32)
  (i32.shr_s
   (i32.shl
    (i32.ne
     (call $_fwrite
      (get_local $0)
      (i32.const 1)
      (tee_local $2
       (call $_strlen
        (get_local $0)
       )
      )
      (get_local $1)
     )
     (get_local $2)
    )
    (i32.const 31)
   )
   (i32.const 31)
  )
 )
 (func $_fwrite (param $0 i32) (param $1 i32) (param $2 i32) (param $3 i32) (result i32)
  (local $4 i32)
  (local $5 i32)
  (set_local $4
   (i32.mul
    (get_local $2)
    (get_local $1)
   )
  )
  (if
   (i32.gt_s
    (i32.load offset=76
     (get_local $3)
    )
    (i32.const -1)
   )
   (block
    (set_local $5
     (i32.eqz
      (call $___lockfile
       (get_local $3)
      )
     )
    )
    (set_local $0
     (call $___fwritex
      (get_local $0)
      (get_local $4)
      (get_local $3)
     )
    )
    (if
     (i32.eqz
      (get_local $5)
     )
     (call $___unlockfile
      (get_local $3)
     )
    )
   )
   (set_local $0
    (call $___fwritex
     (get_local $0)
     (get_local $4)
     (get_local $3)
    )
   )
  )
  (if
   (i32.eqz
    (get_local $1)
   )
   (set_local $2
    (i32.const 0)
   )
  )
  (if
   (i32.ne
    (get_local $0)
    (get_local $4)
   )
   (set_local $2
    (i32.div_u
     (get_local $0)
     (get_local $1)
    )
   )
  )
  (get_local $2)
 )
 (func $_puts (param $0 i32) (result i32)
  (local $1 i32)
  (local $2 i32)
  (local $3 i32)
  (set_local $2
   (if (result i32)
    (i32.gt_s
     (i32.load offset=76
      (tee_local $1
       (i32.load
        (i32.const 1268)
       )
      )
     )
     (i32.const -1)
    )
    (call $___lockfile
     (get_local $1)
    )
    (i32.const 0)
   )
  )
  (set_local $0
   (block $do-once (result i32)
    (if (result i32)
     (i32.lt_s
      (call $_fputs
       (get_local $0)
       (get_local $1)
      )
      (i32.const 0)
     )
     (i32.const 1)
     (block (result i32)
      (if
       (i32.ne
        (i32.load8_s offset=75
         (get_local $1)
        )
        (i32.const 10)
       )
       (if
        (i32.lt_u
         (tee_local $0
          (i32.load
           (tee_local $3
            (i32.add
             (get_local $1)
             (i32.const 20)
            )
           )
          )
         )
         (i32.load offset=16
          (get_local $1)
         )
        )
        (block
         (i32.store
          (get_local $3)
          (i32.add
           (get_local $0)
           (i32.const 1)
          )
         )
         (i32.store8
          (get_local $0)
          (i32.const 10)
         )
         (br $do-once
          (i32.const 0)
         )
        )
       )
      )
      (i32.lt_s
       (call $___overflow
        (get_local $1)
        (i32.const 10)
       )
       (i32.const 0)
      )
     )
    )
   )
  )
  (if
   (get_local $2)
   (call $___unlockfile
    (get_local $1)
   )
  )
  (i32.shr_s
   (i32.shl
    (get_local $0)
    (i32.const 31)
   )
   (i32.const 31)
  )
 )
 (func $_putchar (param $0 i32) (result i32)
  (call $_fputc
   (get_local $0)
   (i32.load
    (i32.const 1268)
   )
  )
 )
 (func $_malloc (param $0 i32) (result i32)
  (local $1 i32)
  (local $2 i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (local $6 i32)
  (local $7 i32)
  (local $8 i32)
  (local $9 i32)
  (local $10 i32)
  (local $11 i32)
  (local $12 i32)
  (local $13 i32)
  (local $14 i32)
  (local $15 i32)
  (local $16 i32)
  (local $17 i32)
  (local $18 i32)
  (local $19 i32)
  (local $20 i32)
  (set_local $13
   (get_global $STACKTOP)
  )
  (set_global $STACKTOP
   (i32.add
    (get_global $STACKTOP)
    (i32.const 16)
   )
  )
  (set_local $15
   (get_local $13)
  )
  (block $do-once
   (if
    (i32.lt_u
     (get_local $0)
     (i32.const 245)
    )
    (block
     (set_local $3
      (i32.and
       (i32.add
        (get_local $0)
        (i32.const 11)
       )
       (i32.const -8)
      )
     )
     (if
      (i32.and
       (tee_local $2
        (i32.shr_u
         (tee_local $7
          (i32.load
           (i32.const 3920)
          )
         )
         (tee_local $0
          (i32.shr_u
           (if (result i32)
            (i32.lt_u
             (get_local $0)
             (i32.const 11)
            )
            (tee_local $3
             (i32.const 16)
            )
            (get_local $3)
           )
           (i32.const 3)
          )
         )
        )
       )
       (i32.const 3)
      )
      (block
       (set_local $0
        (i32.load
         (tee_local $6
          (i32.add
           (tee_local $2
            (i32.load
             (tee_local $4
              (i32.add
               (tee_local $3
                (i32.add
                 (i32.shl
                  (tee_local $1
                   (i32.add
                    (i32.xor
                     (i32.and
                      (get_local $2)
                      (i32.const 1)
                     )
                     (i32.const 1)
                    )
                    (get_local $0)
                   )
                  )
                  (i32.const 3)
                 )
                 (i32.const 3960)
                )
               )
               (i32.const 8)
              )
             )
            )
           )
           (i32.const 8)
          )
         )
        )
       )
       (if
        (i32.eq
         (get_local $3)
         (get_local $0)
        )
        (i32.store
         (i32.const 3920)
         (i32.and
          (get_local $7)
          (i32.xor
           (i32.shl
            (i32.const 1)
            (get_local $1)
           )
           (i32.const -1)
          )
         )
        )
        (block
         (if
          (i32.lt_u
           (get_local $0)
           (i32.load
            (i32.const 3936)
           )
          )
          (call $_abort)
         )
         (if
          (i32.eq
           (i32.load
            (tee_local $5
             (i32.add
              (get_local $0)
              (i32.const 12)
             )
            )
           )
           (get_local $2)
          )
          (block
           (i32.store
            (get_local $5)
            (get_local $3)
           )
           (i32.store
            (get_local $4)
            (get_local $0)
           )
          )
          (call $_abort)
         )
        )
       )
       (i32.store offset=4
        (get_local $2)
        (i32.or
         (tee_local $0
          (i32.shl
           (get_local $1)
           (i32.const 3)
          )
         )
         (i32.const 3)
        )
       )
       (i32.store
        (tee_local $0
         (i32.add
          (i32.add
           (get_local $2)
           (get_local $0)
          )
          (i32.const 4)
         )
        )
        (i32.or
         (i32.load
          (get_local $0)
         )
         (i32.const 1)
        )
       )
       (set_global $STACKTOP
        (get_local $13)
       )
       (return
        (get_local $6)
       )
      )
     )
     (if
      (i32.gt_u
       (get_local $3)
       (tee_local $16
        (i32.load
         (i32.const 3928)
        )
       )
      )
      (block
       (if
        (get_local $2)
        (block
         (set_local $0
          (i32.and
           (i32.shr_u
            (tee_local $2
             (i32.add
              (i32.and
               (tee_local $0
                (i32.and
                 (i32.shl
                  (get_local $2)
                  (get_local $0)
                 )
                 (i32.or
                  (tee_local $0
                   (i32.shl
                    (i32.const 2)
                    (get_local $0)
                   )
                  )
                  (i32.sub
                   (i32.const 0)
                   (get_local $0)
                  )
                 )
                )
               )
               (i32.sub
                (i32.const 0)
                (get_local $0)
               )
              )
              (i32.const -1)
             )
            )
            (i32.const 12)
           )
           (i32.const 16)
          )
         )
         (set_local $0
          (i32.load
           (tee_local $11
            (i32.add
             (tee_local $2
              (i32.load
               (tee_local $8
                (i32.add
                 (tee_local $5
                  (i32.add
                   (i32.shl
                    (tee_local $4
                     (i32.add
                      (i32.or
                       (i32.or
                        (i32.or
                         (i32.or
                          (tee_local $4
                           (i32.and
                            (i32.shr_u
                             (tee_local $2
                              (i32.shr_u
                               (get_local $2)
                               (get_local $0)
                              )
                             )
                             (i32.const 5)
                            )
                            (i32.const 8)
                           )
                          )
                          (get_local $0)
                         )
                         (tee_local $2
                          (i32.and
                           (i32.shr_u
                            (tee_local $0
                             (i32.shr_u
                              (get_local $2)
                              (get_local $4)
                             )
                            )
                            (i32.const 2)
                           )
                           (i32.const 4)
                          )
                         )
                        )
                        (tee_local $2
                         (i32.and
                          (i32.shr_u
                           (tee_local $0
                            (i32.shr_u
                             (get_local $0)
                             (get_local $2)
                            )
                           )
                           (i32.const 1)
                          )
                          (i32.const 2)
                         )
                        )
                       )
                       (tee_local $2
                        (i32.and
                         (i32.shr_u
                          (tee_local $0
                           (i32.shr_u
                            (get_local $0)
                            (get_local $2)
                           )
                          )
                          (i32.const 1)
                         )
                         (i32.const 1)
                        )
                       )
                      )
                      (i32.shr_u
                       (get_local $0)
                       (get_local $2)
                      )
                     )
                    )
                    (i32.const 3)
                   )
                   (i32.const 3960)
                  )
                 )
                 (i32.const 8)
                )
               )
              )
             )
             (i32.const 8)
            )
           )
          )
         )
         (if
          (i32.eq
           (get_local $5)
           (get_local $0)
          )
          (i32.store
           (i32.const 3920)
           (tee_local $1
            (i32.and
             (get_local $7)
             (i32.xor
              (i32.shl
               (i32.const 1)
               (get_local $4)
              )
              (i32.const -1)
             )
            )
           )
          )
          (block
           (if
            (i32.lt_u
             (get_local $0)
             (i32.load
              (i32.const 3936)
             )
            )
            (call $_abort)
           )
           (if
            (i32.eq
             (i32.load
              (tee_local $12
               (i32.add
                (get_local $0)
                (i32.const 12)
               )
              )
             )
             (get_local $2)
            )
            (block
             (i32.store
              (get_local $12)
              (get_local $5)
             )
             (i32.store
              (get_local $8)
              (get_local $0)
             )
             (set_local $1
              (get_local $7)
             )
            )
            (call $_abort)
           )
          )
         )
         (i32.store offset=4
          (get_local $2)
          (i32.or
           (get_local $3)
           (i32.const 3)
          )
         )
         (i32.store offset=4
          (tee_local $5
           (i32.add
            (get_local $2)
            (get_local $3)
           )
          )
          (i32.or
           (tee_local $4
            (i32.sub
             (i32.shl
              (get_local $4)
              (i32.const 3)
             )
             (get_local $3)
            )
           )
           (i32.const 1)
          )
         )
         (i32.store
          (i32.add
           (get_local $5)
           (get_local $4)
          )
          (get_local $4)
         )
         (if
          (get_local $16)
          (block
           (set_local $3
            (i32.load
             (i32.const 3940)
            )
           )
           (set_local $0
            (i32.add
             (i32.shl
              (tee_local $2
               (i32.shr_u
                (get_local $16)
                (i32.const 3)
               )
              )
              (i32.const 3)
             )
             (i32.const 3960)
            )
           )
           (if
            (i32.and
             (get_local $1)
             (tee_local $2
              (i32.shl
               (i32.const 1)
               (get_local $2)
              )
             )
            )
            (if
             (i32.lt_u
              (tee_local $1
               (i32.load
                (tee_local $2
                 (i32.add
                  (get_local $0)
                  (i32.const 8)
                 )
                )
               )
              )
              (i32.load
               (i32.const 3936)
              )
             )
             (call $_abort)
             (block
              (set_local $6
               (get_local $1)
              )
              (set_local $10
               (get_local $2)
              )
             )
            )
            (block
             (i32.store
              (i32.const 3920)
              (i32.or
               (get_local $1)
               (get_local $2)
              )
             )
             (set_local $6
              (get_local $0)
             )
             (set_local $10
              (i32.add
               (get_local $0)
               (i32.const 8)
              )
             )
            )
           )
           (i32.store
            (get_local $10)
            (get_local $3)
           )
           (i32.store offset=12
            (get_local $6)
            (get_local $3)
           )
           (i32.store offset=8
            (get_local $3)
            (get_local $6)
           )
           (i32.store offset=12
            (get_local $3)
            (get_local $0)
           )
          )
         )
         (i32.store
          (i32.const 3928)
          (get_local $4)
         )
         (i32.store
          (i32.const 3940)
          (get_local $5)
         )
         (set_global $STACKTOP
          (get_local $13)
         )
         (return
          (get_local $11)
         )
        )
       )
       (if
        (tee_local $10
         (i32.load
          (i32.const 3924)
         )
        )
        (block
         (set_local $0
          (i32.and
           (i32.shr_u
            (tee_local $2
             (i32.add
              (i32.and
               (get_local $10)
               (i32.sub
                (i32.const 0)
                (get_local $10)
               )
              )
              (i32.const -1)
             )
            )
            (i32.const 12)
           )
           (i32.const 16)
          )
         )
         (set_local $2
          (i32.sub
           (i32.and
            (i32.load offset=4
             (tee_local $1
              (i32.load
               (i32.add
                (i32.shl
                 (i32.add
                  (i32.or
                   (i32.or
                    (i32.or
                     (i32.or
                      (tee_local $1
                       (i32.and
                        (i32.shr_u
                         (tee_local $2
                          (i32.shr_u
                           (get_local $2)
                           (get_local $0)
                          )
                         )
                         (i32.const 5)
                        )
                        (i32.const 8)
                       )
                      )
                      (get_local $0)
                     )
                     (tee_local $2
                      (i32.and
                       (i32.shr_u
                        (tee_local $0
                         (i32.shr_u
                          (get_local $2)
                          (get_local $1)
                         )
                        )
                        (i32.const 2)
                       )
                       (i32.const 4)
                      )
                     )
                    )
                    (tee_local $2
                     (i32.and
                      (i32.shr_u
                       (tee_local $0
                        (i32.shr_u
                         (get_local $0)
                         (get_local $2)
                        )
                       )
                       (i32.const 1)
                      )
                      (i32.const 2)
                     )
                    )
                   )
                   (tee_local $2
                    (i32.and
                     (i32.shr_u
                      (tee_local $0
                       (i32.shr_u
                        (get_local $0)
                        (get_local $2)
                       )
                      )
                      (i32.const 1)
                     )
                     (i32.const 1)
                    )
                   )
                  )
                  (i32.shr_u
                   (get_local $0)
                   (get_local $2)
                  )
                 )
                 (i32.const 2)
                )
                (i32.const 4224)
               )
              )
             )
            )
            (i32.const -8)
           )
           (get_local $3)
          )
         )
         (if
          (tee_local $0
           (i32.load
            (i32.add
             (i32.add
              (get_local $1)
              (i32.const 16)
             )
             (i32.shl
              (i32.eqz
               (i32.load offset=16
                (get_local $1)
               )
              )
              (i32.const 2)
             )
            )
           )
          )
          (loop $while-in
           (if
            (tee_local $8
             (i32.lt_u
              (tee_local $6
               (i32.sub
                (i32.and
                 (i32.load offset=4
                  (get_local $0)
                 )
                 (i32.const -8)
                )
                (get_local $3)
               )
              )
              (get_local $2)
             )
            )
            (set_local $2
             (get_local $6)
            )
           )
           (if
            (get_local $8)
            (set_local $1
             (get_local $0)
            )
           )
           (br_if $while-in
            (tee_local $0
             (i32.load
              (i32.add
               (i32.add
                (get_local $0)
                (i32.const 16)
               )
               (i32.shl
                (i32.eqz
                 (i32.load offset=16
                  (get_local $0)
                 )
                )
                (i32.const 2)
               )
              )
             )
            )
           )
           (set_local $6
            (get_local $2)
           )
          )
          (set_local $6
           (get_local $2)
          )
         )
         (if
          (i32.lt_u
           (get_local $1)
           (tee_local $15
            (i32.load
             (i32.const 3936)
            )
           )
          )
          (call $_abort)
         )
         (if
          (i32.ge_u
           (get_local $1)
           (tee_local $9
            (i32.add
             (get_local $1)
             (get_local $3)
            )
           )
          )
          (call $_abort)
         )
         (set_local $12
          (i32.load offset=24
           (get_local $1)
          )
         )
         (block $do-once4
          (if
           (i32.eq
            (tee_local $0
             (i32.load offset=12
              (get_local $1)
             )
            )
            (get_local $1)
           )
           (block
            (if
             (i32.eqz
              (tee_local $0
               (i32.load
                (tee_local $2
                 (i32.add
                  (get_local $1)
                  (i32.const 20)
                 )
                )
               )
              )
             )
             (if
              (i32.eqz
               (tee_local $0
                (i32.load
                 (tee_local $2
                  (i32.add
                   (get_local $1)
                   (i32.const 16)
                  )
                 )
                )
               )
              )
              (block
               (set_local $4
                (i32.const 0)
               )
               (br $do-once4)
              )
             )
            )
            (loop $while-in7
             (if
              (tee_local $11
               (i32.load
                (tee_local $8
                 (i32.add
                  (get_local $0)
                  (i32.const 20)
                 )
                )
               )
              )
              (block
               (set_local $0
                (get_local $11)
               )
               (set_local $2
                (get_local $8)
               )
               (br $while-in7)
              )
             )
             (if
              (tee_local $11
               (i32.load
                (tee_local $8
                 (i32.add
                  (get_local $0)
                  (i32.const 16)
                 )
                )
               )
              )
              (block
               (set_local $0
                (get_local $11)
               )
               (set_local $2
                (get_local $8)
               )
               (br $while-in7)
              )
             )
            )
            (if
             (i32.lt_u
              (get_local $2)
              (get_local $15)
             )
             (call $_abort)
             (block
              (i32.store
               (get_local $2)
               (i32.const 0)
              )
              (set_local $4
               (get_local $0)
              )
             )
            )
           )
           (block
            (if
             (i32.lt_u
              (tee_local $2
               (i32.load offset=8
                (get_local $1)
               )
              )
              (get_local $15)
             )
             (call $_abort)
            )
            (if
             (i32.ne
              (i32.load
               (tee_local $8
                (i32.add
                 (get_local $2)
                 (i32.const 12)
                )
               )
              )
              (get_local $1)
             )
             (call $_abort)
            )
            (if
             (i32.eq
              (i32.load
               (tee_local $11
                (i32.add
                 (get_local $0)
                 (i32.const 8)
                )
               )
              )
              (get_local $1)
             )
             (block
              (i32.store
               (get_local $8)
               (get_local $0)
              )
              (i32.store
               (get_local $11)
               (get_local $2)
              )
              (set_local $4
               (get_local $0)
              )
             )
             (call $_abort)
            )
           )
          )
         )
         (block $label$break$L73
          (if
           (get_local $12)
           (block
            (if
             (i32.eq
              (get_local $1)
              (i32.load
               (tee_local $2
                (i32.add
                 (i32.shl
                  (tee_local $0
                   (i32.load offset=28
                    (get_local $1)
                   )
                  )
                  (i32.const 2)
                 )
                 (i32.const 4224)
                )
               )
              )
             )
             (block
              (i32.store
               (get_local $2)
               (get_local $4)
              )
              (if
               (i32.eqz
                (get_local $4)
               )
               (block
                (i32.store
                 (i32.const 3924)
                 (i32.and
                  (get_local $10)
                  (i32.xor
                   (i32.shl
                    (i32.const 1)
                    (get_local $0)
                   )
                   (i32.const -1)
                  )
                 )
                )
                (br $label$break$L73)
               )
              )
             )
             (if
              (i32.lt_u
               (get_local $12)
               (i32.load
                (i32.const 3936)
               )
              )
              (call $_abort)
              (block
               (i32.store
                (i32.add
                 (i32.add
                  (get_local $12)
                  (i32.const 16)
                 )
                 (i32.shl
                  (i32.ne
                   (i32.load offset=16
                    (get_local $12)
                   )
                   (get_local $1)
                  )
                  (i32.const 2)
                 )
                )
                (get_local $4)
               )
               (br_if $label$break$L73
                (i32.eqz
                 (get_local $4)
                )
               )
              )
             )
            )
            (if
             (i32.lt_u
              (get_local $4)
              (tee_local $2
               (i32.load
                (i32.const 3936)
               )
              )
             )
             (call $_abort)
            )
            (i32.store offset=24
             (get_local $4)
             (get_local $12)
            )
            (if
             (tee_local $0
              (i32.load offset=16
               (get_local $1)
              )
             )
             (if
              (i32.lt_u
               (get_local $0)
               (get_local $2)
              )
              (call $_abort)
              (block
               (i32.store offset=16
                (get_local $4)
                (get_local $0)
               )
               (i32.store offset=24
                (get_local $0)
                (get_local $4)
               )
              )
             )
            )
            (if
             (tee_local $0
              (i32.load offset=20
               (get_local $1)
              )
             )
             (if
              (i32.lt_u
               (get_local $0)
               (i32.load
                (i32.const 3936)
               )
              )
              (call $_abort)
              (block
               (i32.store offset=20
                (get_local $4)
                (get_local $0)
               )
               (i32.store offset=24
                (get_local $0)
                (get_local $4)
               )
              )
             )
            )
           )
          )
         )
         (if
          (i32.lt_u
           (get_local $6)
           (i32.const 16)
          )
          (block
           (i32.store offset=4
            (get_local $1)
            (i32.or
             (tee_local $0
              (i32.add
               (get_local $6)
               (get_local $3)
              )
             )
             (i32.const 3)
            )
           )
           (i32.store
            (tee_local $0
             (i32.add
              (i32.add
               (get_local $1)
               (get_local $0)
              )
              (i32.const 4)
             )
            )
            (i32.or
             (i32.load
              (get_local $0)
             )
             (i32.const 1)
            )
           )
          )
          (block
           (i32.store offset=4
            (get_local $1)
            (i32.or
             (get_local $3)
             (i32.const 3)
            )
           )
           (i32.store offset=4
            (get_local $9)
            (i32.or
             (get_local $6)
             (i32.const 1)
            )
           )
           (i32.store
            (i32.add
             (get_local $9)
             (get_local $6)
            )
            (get_local $6)
           )
           (if
            (get_local $16)
            (block
             (set_local $4
              (i32.load
               (i32.const 3940)
              )
             )
             (set_local $0
              (i32.add
               (i32.shl
                (tee_local $2
                 (i32.shr_u
                  (get_local $16)
                  (i32.const 3)
                 )
                )
                (i32.const 3)
               )
               (i32.const 3960)
              )
             )
             (if
              (i32.and
               (get_local $7)
               (tee_local $2
                (i32.shl
                 (i32.const 1)
                 (get_local $2)
                )
               )
              )
              (if
               (i32.lt_u
                (tee_local $3
                 (i32.load
                  (tee_local $2
                   (i32.add
                    (get_local $0)
                    (i32.const 8)
                   )
                  )
                 )
                )
                (i32.load
                 (i32.const 3936)
                )
               )
               (call $_abort)
               (block
                (set_local $5
                 (get_local $3)
                )
                (set_local $14
                 (get_local $2)
                )
               )
              )
              (block
               (i32.store
                (i32.const 3920)
                (i32.or
                 (get_local $7)
                 (get_local $2)
                )
               )
               (set_local $5
                (get_local $0)
               )
               (set_local $14
                (i32.add
                 (get_local $0)
                 (i32.const 8)
                )
               )
              )
             )
             (i32.store
              (get_local $14)
              (get_local $4)
             )
             (i32.store offset=12
              (get_local $5)
              (get_local $4)
             )
             (i32.store offset=8
              (get_local $4)
              (get_local $5)
             )
             (i32.store offset=12
              (get_local $4)
              (get_local $0)
             )
            )
           )
           (i32.store
            (i32.const 3928)
            (get_local $6)
           )
           (i32.store
            (i32.const 3940)
            (get_local $9)
           )
          )
         )
         (set_global $STACKTOP
          (get_local $13)
         )
         (return
          (i32.add
           (get_local $1)
           (i32.const 8)
          )
         )
        )
        (set_local $2
         (get_local $3)
        )
       )
      )
      (set_local $2
       (get_local $3)
      )
     )
    )
    (if
     (i32.gt_u
      (get_local $0)
      (i32.const -65)
     )
     (set_local $2
      (i32.const -1)
     )
     (block
      (set_local $4
       (i32.and
        (tee_local $0
         (i32.add
          (get_local $0)
          (i32.const 11)
         )
        )
        (i32.const -8)
       )
      )
      (if
       (tee_local $6
        (i32.load
         (i32.const 3924)
        )
       )
       (block
        (set_local $17
         (if (result i32)
          (tee_local $0
           (i32.shr_u
            (get_local $0)
            (i32.const 8)
           )
          )
          (if (result i32)
           (i32.gt_u
            (get_local $4)
            (i32.const 16777215)
           )
           (i32.const 31)
           (i32.or
            (i32.and
             (i32.shr_u
              (get_local $4)
              (i32.add
               (tee_local $0
                (i32.add
                 (i32.sub
                  (i32.const 14)
                  (i32.or
                   (i32.or
                    (tee_local $3
                     (i32.and
                      (i32.shr_u
                       (i32.add
                        (tee_local $1
                         (i32.shl
                          (get_local $0)
                          (tee_local $0
                           (i32.and
                            (i32.shr_u
                             (i32.add
                              (get_local $0)
                              (i32.const 1048320)
                             )
                             (i32.const 16)
                            )
                            (i32.const 8)
                           )
                          )
                         )
                        )
                        (i32.const 520192)
                       )
                       (i32.const 16)
                      )
                      (i32.const 4)
                     )
                    )
                    (get_local $0)
                   )
                   (tee_local $1
                    (i32.and
                     (i32.shr_u
                      (i32.add
                       (tee_local $0
                        (i32.shl
                         (get_local $1)
                         (get_local $3)
                        )
                       )
                       (i32.const 245760)
                      )
                      (i32.const 16)
                     )
                     (i32.const 2)
                    )
                   )
                  )
                 )
                 (i32.shr_u
                  (i32.shl
                   (get_local $0)
                   (get_local $1)
                  )
                  (i32.const 15)
                 )
                )
               )
               (i32.const 7)
              )
             )
             (i32.const 1)
            )
            (i32.shl
             (get_local $0)
             (i32.const 1)
            )
           )
          )
          (i32.const 0)
         )
        )
        (set_local $3
         (i32.sub
          (i32.const 0)
          (get_local $4)
         )
        )
        (block $__rjto$3
         (block $__rjti$3
          (if
           (tee_local $0
            (i32.load
             (i32.add
              (i32.shl
               (get_local $17)
               (i32.const 2)
              )
              (i32.const 4224)
             )
            )
           )
           (block
            (set_local $5
             (i32.sub
              (i32.const 25)
              (i32.shr_u
               (get_local $17)
               (i32.const 1)
              )
             )
            )
            (set_local $1
             (i32.const 0)
            )
            (set_local $10
             (i32.shl
              (get_local $4)
              (if (result i32)
               (i32.eq
                (get_local $17)
                (i32.const 31)
               )
               (i32.const 0)
               (get_local $5)
              )
             )
            )
            (set_local $5
             (i32.const 0)
            )
            (loop $while-in15
             (if
              (i32.lt_u
               (tee_local $14
                (i32.sub
                 (i32.and
                  (i32.load offset=4
                   (get_local $0)
                  )
                  (i32.const -8)
                 )
                 (get_local $4)
                )
               )
               (get_local $3)
              )
              (if
               (get_local $14)
               (block
                (set_local $1
                 (get_local $0)
                )
                (set_local $3
                 (get_local $14)
                )
               )
               (block
                (set_local $1
                 (get_local $0)
                )
                (set_local $3
                 (i32.const 0)
                )
                (br $__rjti$3)
               )
              )
             )
             (if
              (i32.eqz
               (i32.or
                (i32.eqz
                 (tee_local $14
                  (i32.load offset=20
                   (get_local $0)
                  )
                 )
                )
                (i32.eq
                 (get_local $14)
                 (tee_local $0
                  (i32.load
                   (i32.add
                    (i32.add
                     (get_local $0)
                     (i32.const 16)
                    )
                    (i32.shl
                     (i32.shr_u
                      (get_local $10)
                      (i32.const 31)
                     )
                     (i32.const 2)
                    )
                   )
                  )
                 )
                )
               )
              )
              (set_local $5
               (get_local $14)
              )
             )
             (set_local $10
              (i32.shl
               (get_local $10)
               (i32.xor
                (tee_local $14
                 (i32.eqz
                  (get_local $0)
                 )
                )
                (i32.const 1)
               )
              )
             )
             (br_if $while-in15
              (i32.eqz
               (get_local $14)
              )
             )
            )
           )
           (block
            (set_local $5
             (i32.const 0)
            )
            (set_local $1
             (i32.const 0)
            )
           )
          )
          (br_if $__rjti$3
           (tee_local $0
            (if (result i32)
             (i32.and
              (i32.eqz
               (get_local $5)
              )
              (i32.eqz
               (get_local $1)
              )
             )
             (block (result i32)
              (if
               (i32.eqz
                (tee_local $0
                 (i32.and
                  (get_local $6)
                  (i32.or
                   (tee_local $0
                    (i32.shl
                     (i32.const 2)
                     (get_local $17)
                    )
                   )
                   (i32.sub
                    (i32.const 0)
                    (get_local $0)
                   )
                  )
                 )
                )
               )
               (block
                (set_local $2
                 (get_local $4)
                )
                (br $do-once)
               )
              )
              (set_local $0
               (i32.and
                (i32.shr_u
                 (tee_local $5
                  (i32.add
                   (i32.and
                    (get_local $0)
                    (i32.sub
                     (i32.const 0)
                     (get_local $0)
                    )
                   )
                   (i32.const -1)
                  )
                 )
                 (i32.const 12)
                )
                (i32.const 16)
               )
              )
              (set_local $1
               (i32.const 0)
              )
              (i32.load
               (i32.add
                (i32.shl
                 (i32.add
                  (i32.or
                   (i32.or
                    (i32.or
                     (i32.or
                      (tee_local $10
                       (i32.and
                        (i32.shr_u
                         (tee_local $5
                          (i32.shr_u
                           (get_local $5)
                           (get_local $0)
                          )
                         )
                         (i32.const 5)
                        )
                        (i32.const 8)
                       )
                      )
                      (get_local $0)
                     )
                     (tee_local $5
                      (i32.and
                       (i32.shr_u
                        (tee_local $0
                         (i32.shr_u
                          (get_local $5)
                          (get_local $10)
                         )
                        )
                        (i32.const 2)
                       )
                       (i32.const 4)
                      )
                     )
                    )
                    (tee_local $5
                     (i32.and
                      (i32.shr_u
                       (tee_local $0
                        (i32.shr_u
                         (get_local $0)
                         (get_local $5)
                        )
                       )
                       (i32.const 1)
                      )
                      (i32.const 2)
                     )
                    )
                   )
                   (tee_local $5
                    (i32.and
                     (i32.shr_u
                      (tee_local $0
                       (i32.shr_u
                        (get_local $0)
                        (get_local $5)
                       )
                      )
                      (i32.const 1)
                     )
                     (i32.const 1)
                    )
                   )
                  )
                  (i32.shr_u
                   (get_local $0)
                   (get_local $5)
                  )
                 )
                 (i32.const 2)
                )
                (i32.const 4224)
               )
              )
             )
             (get_local $5)
            )
           )
          )
          (set_local $5
           (get_local $1)
          )
          (br $__rjto$3)
         )
         (loop $while-in17
          (if
           (tee_local $10
            (i32.lt_u
             (tee_local $5
              (i32.sub
               (i32.and
                (i32.load offset=4
                 (get_local $0)
                )
                (i32.const -8)
               )
               (get_local $4)
              )
             )
             (get_local $3)
            )
           )
           (set_local $3
            (get_local $5)
           )
          )
          (if
           (get_local $10)
           (set_local $1
            (get_local $0)
           )
          )
          (br_if $while-in17
           (tee_local $0
            (i32.load
             (i32.add
              (i32.add
               (get_local $0)
               (i32.const 16)
              )
              (i32.shl
               (i32.eqz
                (i32.load offset=16
                 (get_local $0)
                )
               )
               (i32.const 2)
              )
             )
            )
           )
          )
          (set_local $5
           (get_local $1)
          )
         )
        )
        (if
         (get_local $5)
         (if
          (i32.lt_u
           (get_local $3)
           (i32.sub
            (i32.load
             (i32.const 3928)
            )
            (get_local $4)
           )
          )
          (block
           (if
            (i32.lt_u
             (get_local $5)
             (tee_local $15
              (i32.load
               (i32.const 3936)
              )
             )
            )
            (call $_abort)
           )
           (if
            (i32.ge_u
             (get_local $5)
             (tee_local $9
              (i32.add
               (get_local $5)
               (get_local $4)
              )
             )
            )
            (call $_abort)
           )
           (set_local $10
            (i32.load offset=24
             (get_local $5)
            )
           )
           (block $do-once18
            (if
             (i32.eq
              (tee_local $0
               (i32.load offset=12
                (get_local $5)
               )
              )
              (get_local $5)
             )
             (block
              (if
               (i32.eqz
                (tee_local $0
                 (i32.load
                  (tee_local $1
                   (i32.add
                    (get_local $5)
                    (i32.const 20)
                   )
                  )
                 )
                )
               )
               (if
                (i32.eqz
                 (tee_local $0
                  (i32.load
                   (tee_local $1
                    (i32.add
                     (get_local $5)
                     (i32.const 16)
                    )
                   )
                  )
                 )
                )
                (block
                 (set_local $8
                  (i32.const 0)
                 )
                 (br $do-once18)
                )
               )
              )
              (loop $while-in21
               (if
                (tee_local $12
                 (i32.load
                  (tee_local $11
                   (i32.add
                    (get_local $0)
                    (i32.const 20)
                   )
                  )
                 )
                )
                (block
                 (set_local $0
                  (get_local $12)
                 )
                 (set_local $1
                  (get_local $11)
                 )
                 (br $while-in21)
                )
               )
               (if
                (tee_local $12
                 (i32.load
                  (tee_local $11
                   (i32.add
                    (get_local $0)
                    (i32.const 16)
                   )
                  )
                 )
                )
                (block
                 (set_local $0
                  (get_local $12)
                 )
                 (set_local $1
                  (get_local $11)
                 )
                 (br $while-in21)
                )
               )
              )
              (if
               (i32.lt_u
                (get_local $1)
                (get_local $15)
               )
               (call $_abort)
               (block
                (i32.store
                 (get_local $1)
                 (i32.const 0)
                )
                (set_local $8
                 (get_local $0)
                )
               )
              )
             )
             (block
              (if
               (i32.lt_u
                (tee_local $1
                 (i32.load offset=8
                  (get_local $5)
                 )
                )
                (get_local $15)
               )
               (call $_abort)
              )
              (if
               (i32.ne
                (i32.load
                 (tee_local $11
                  (i32.add
                   (get_local $1)
                   (i32.const 12)
                  )
                 )
                )
                (get_local $5)
               )
               (call $_abort)
              )
              (if
               (i32.eq
                (i32.load
                 (tee_local $12
                  (i32.add
                   (get_local $0)
                   (i32.const 8)
                  )
                 )
                )
                (get_local $5)
               )
               (block
                (i32.store
                 (get_local $11)
                 (get_local $0)
                )
                (i32.store
                 (get_local $12)
                 (get_local $1)
                )
                (set_local $8
                 (get_local $0)
                )
               )
               (call $_abort)
              )
             )
            )
           )
           (block $label$break$L164
            (if
             (get_local $10)
             (block
              (if
               (i32.eq
                (get_local $5)
                (i32.load
                 (tee_local $1
                  (i32.add
                   (i32.shl
                    (tee_local $0
                     (i32.load offset=28
                      (get_local $5)
                     )
                    )
                    (i32.const 2)
                   )
                   (i32.const 4224)
                  )
                 )
                )
               )
               (block
                (i32.store
                 (get_local $1)
                 (get_local $8)
                )
                (if
                 (i32.eqz
                  (get_local $8)
                 )
                 (block
                  (i32.store
                   (i32.const 3924)
                   (tee_local $2
                    (i32.and
                     (get_local $6)
                     (i32.xor
                      (i32.shl
                       (i32.const 1)
                       (get_local $0)
                      )
                      (i32.const -1)
                     )
                    )
                   )
                  )
                  (br $label$break$L164)
                 )
                )
               )
               (if
                (i32.lt_u
                 (get_local $10)
                 (i32.load
                  (i32.const 3936)
                 )
                )
                (call $_abort)
                (block
                 (i32.store
                  (i32.add
                   (i32.add
                    (get_local $10)
                    (i32.const 16)
                   )
                   (i32.shl
                    (i32.ne
                     (i32.load offset=16
                      (get_local $10)
                     )
                     (get_local $5)
                    )
                    (i32.const 2)
                   )
                  )
                  (get_local $8)
                 )
                 (if
                  (i32.eqz
                   (get_local $8)
                  )
                  (block
                   (set_local $2
                    (get_local $6)
                   )
                   (br $label$break$L164)
                  )
                 )
                )
               )
              )
              (if
               (i32.lt_u
                (get_local $8)
                (tee_local $1
                 (i32.load
                  (i32.const 3936)
                 )
                )
               )
               (call $_abort)
              )
              (i32.store offset=24
               (get_local $8)
               (get_local $10)
              )
              (if
               (tee_local $0
                (i32.load offset=16
                 (get_local $5)
                )
               )
               (if
                (i32.lt_u
                 (get_local $0)
                 (get_local $1)
                )
                (call $_abort)
                (block
                 (i32.store offset=16
                  (get_local $8)
                  (get_local $0)
                 )
                 (i32.store offset=24
                  (get_local $0)
                  (get_local $8)
                 )
                )
               )
              )
              (if
               (tee_local $0
                (i32.load offset=20
                 (get_local $5)
                )
               )
               (if
                (i32.lt_u
                 (get_local $0)
                 (i32.load
                  (i32.const 3936)
                 )
                )
                (call $_abort)
                (block
                 (i32.store offset=20
                  (get_local $8)
                  (get_local $0)
                 )
                 (i32.store offset=24
                  (get_local $0)
                  (get_local $8)
                 )
                 (set_local $2
                  (get_local $6)
                 )
                )
               )
               (set_local $2
                (get_local $6)
               )
              )
             )
             (set_local $2
              (get_local $6)
             )
            )
           )
           (block $do-once27
            (if
             (i32.lt_u
              (get_local $3)
              (i32.const 16)
             )
             (block
              (i32.store offset=4
               (get_local $5)
               (i32.or
                (tee_local $0
                 (i32.add
                  (get_local $3)
                  (get_local $4)
                 )
                )
                (i32.const 3)
               )
              )
              (i32.store
               (tee_local $0
                (i32.add
                 (i32.add
                  (get_local $5)
                  (get_local $0)
                 )
                 (i32.const 4)
                )
               )
               (i32.or
                (i32.load
                 (get_local $0)
                )
                (i32.const 1)
               )
              )
             )
             (block
              (i32.store offset=4
               (get_local $5)
               (i32.or
                (get_local $4)
                (i32.const 3)
               )
              )
              (i32.store offset=4
               (get_local $9)
               (i32.or
                (get_local $3)
                (i32.const 1)
               )
              )
              (i32.store
               (i32.add
                (get_local $9)
                (get_local $3)
               )
               (get_local $3)
              )
              (set_local $1
               (i32.shr_u
                (get_local $3)
                (i32.const 3)
               )
              )
              (if
               (i32.lt_u
                (get_local $3)
                (i32.const 256)
               )
               (block
                (set_local $0
                 (i32.add
                  (i32.shl
                   (get_local $1)
                   (i32.const 3)
                  )
                  (i32.const 3960)
                 )
                )
                (if
                 (i32.and
                  (tee_local $2
                   (i32.load
                    (i32.const 3920)
                   )
                  )
                  (tee_local $1
                   (i32.shl
                    (i32.const 1)
                    (get_local $1)
                   )
                  )
                 )
                 (if
                  (i32.lt_u
                   (tee_local $1
                    (i32.load
                     (tee_local $2
                      (i32.add
                       (get_local $0)
                       (i32.const 8)
                      )
                     )
                    )
                   )
                   (i32.load
                    (i32.const 3936)
                   )
                  )
                  (call $_abort)
                  (block
                   (set_local $7
                    (get_local $1)
                   )
                   (set_local $16
                    (get_local $2)
                   )
                  )
                 )
                 (block
                  (i32.store
                   (i32.const 3920)
                   (i32.or
                    (get_local $2)
                    (get_local $1)
                   )
                  )
                  (set_local $7
                   (get_local $0)
                  )
                  (set_local $16
                   (i32.add
                    (get_local $0)
                    (i32.const 8)
                   )
                  )
                 )
                )
                (i32.store
                 (get_local $16)
                 (get_local $9)
                )
                (i32.store offset=12
                 (get_local $7)
                 (get_local $9)
                )
                (i32.store offset=8
                 (get_local $9)
                 (get_local $7)
                )
                (i32.store offset=12
                 (get_local $9)
                 (get_local $0)
                )
                (br $do-once27)
               )
              )
              (set_local $0
               (i32.add
                (i32.shl
                 (tee_local $1
                  (if (result i32)
                   (tee_local $0
                    (i32.shr_u
                     (get_local $3)
                     (i32.const 8)
                    )
                   )
                   (if (result i32)
                    (i32.gt_u
                     (get_local $3)
                     (i32.const 16777215)
                    )
                    (i32.const 31)
                    (i32.or
                     (i32.and
                      (i32.shr_u
                       (get_local $3)
                       (i32.add
                        (tee_local $0
                         (i32.add
                          (i32.sub
                           (i32.const 14)
                           (i32.or
                            (i32.or
                             (tee_local $4
                              (i32.and
                               (i32.shr_u
                                (i32.add
                                 (tee_local $1
                                  (i32.shl
                                   (get_local $0)
                                   (tee_local $0
                                    (i32.and
                                     (i32.shr_u
                                      (i32.add
                                       (get_local $0)
                                       (i32.const 1048320)
                                      )
                                      (i32.const 16)
                                     )
                                     (i32.const 8)
                                    )
                                   )
                                  )
                                 )
                                 (i32.const 520192)
                                )
                                (i32.const 16)
                               )
                               (i32.const 4)
                              )
                             )
                             (get_local $0)
                            )
                            (tee_local $1
                             (i32.and
                              (i32.shr_u
                               (i32.add
                                (tee_local $0
                                 (i32.shl
                                  (get_local $1)
                                  (get_local $4)
                                 )
                                )
                                (i32.const 245760)
                               )
                               (i32.const 16)
                              )
                              (i32.const 2)
                             )
                            )
                           )
                          )
                          (i32.shr_u
                           (i32.shl
                            (get_local $0)
                            (get_local $1)
                           )
                           (i32.const 15)
                          )
                         )
                        )
                        (i32.const 7)
                       )
                      )
                      (i32.const 1)
                     )
                     (i32.shl
                      (get_local $0)
                      (i32.const 1)
                     )
                    )
                   )
                   (i32.const 0)
                  )
                 )
                 (i32.const 2)
                )
                (i32.const 4224)
               )
              )
              (i32.store offset=28
               (get_local $9)
               (get_local $1)
              )
              (i32.store offset=4
               (tee_local $4
                (i32.add
                 (get_local $9)
                 (i32.const 16)
                )
               )
               (i32.const 0)
              )
              (i32.store
               (get_local $4)
               (i32.const 0)
              )
              (if
               (i32.eqz
                (i32.and
                 (get_local $2)
                 (tee_local $4
                  (i32.shl
                   (i32.const 1)
                   (get_local $1)
                  )
                 )
                )
               )
               (block
                (i32.store
                 (i32.const 3924)
                 (i32.or
                  (get_local $2)
                  (get_local $4)
                 )
                )
                (i32.store
                 (get_local $0)
                 (get_local $9)
                )
                (i32.store offset=24
                 (get_local $9)
                 (get_local $0)
                )
                (i32.store offset=12
                 (get_local $9)
                 (get_local $9)
                )
                (i32.store offset=8
                 (get_local $9)
                 (get_local $9)
                )
                (br $do-once27)
               )
              )
              (set_local $0
               (i32.load
                (get_local $0)
               )
              )
              (set_local $2
               (i32.sub
                (i32.const 25)
                (i32.shr_u
                 (get_local $1)
                 (i32.const 1)
                )
               )
              )
              (set_local $2
               (i32.shl
                (get_local $3)
                (if (result i32)
                 (i32.eq
                  (get_local $1)
                  (i32.const 31)
                 )
                 (i32.const 0)
                 (get_local $2)
                )
               )
              )
              (block $__rjto$1
               (block $__rjti$1
                (loop $while-in30
                 (block $while-out29
                  (br_if $__rjti$1
                   (i32.eq
                    (i32.and
                     (i32.load offset=4
                      (get_local $0)
                     )
                     (i32.const -8)
                    )
                    (get_local $3)
                   )
                  )
                  (set_local $1
                   (i32.shl
                    (get_local $2)
                    (i32.const 1)
                   )
                  )
                  (if
                   (tee_local $4
                    (i32.load
                     (tee_local $2
                      (i32.add
                       (i32.add
                        (get_local $0)
                        (i32.const 16)
                       )
                       (i32.shl
                        (i32.shr_u
                         (get_local $2)
                         (i32.const 31)
                        )
                        (i32.const 2)
                       )
                      )
                     )
                    )
                   )
                   (block
                    (set_local $2
                     (get_local $1)
                    )
                    (set_local $0
                     (get_local $4)
                    )
                    (br $while-in30)
                   )
                  )
                 )
                )
                (if
                 (i32.lt_u
                  (get_local $2)
                  (i32.load
                   (i32.const 3936)
                  )
                 )
                 (call $_abort)
                 (block
                  (i32.store
                   (get_local $2)
                   (get_local $9)
                  )
                  (i32.store offset=24
                   (get_local $9)
                   (get_local $0)
                  )
                  (i32.store offset=12
                   (get_local $9)
                   (get_local $9)
                  )
                  (i32.store offset=8
                   (get_local $9)
                   (get_local $9)
                  )
                  (br $do-once27)
                 )
                )
                (br $__rjto$1)
               )
               (if
                (i32.and
                 (i32.ge_u
                  (tee_local $2
                   (i32.load
                    (tee_local $1
                     (i32.add
                      (get_local $0)
                      (i32.const 8)
                     )
                    )
                   )
                  )
                  (tee_local $3
                   (i32.load
                    (i32.const 3936)
                   )
                  )
                 )
                 (i32.ge_u
                  (get_local $0)
                  (get_local $3)
                 )
                )
                (block
                 (i32.store offset=12
                  (get_local $2)
                  (get_local $9)
                 )
                 (i32.store
                  (get_local $1)
                  (get_local $9)
                 )
                 (i32.store offset=8
                  (get_local $9)
                  (get_local $2)
                 )
                 (i32.store offset=12
                  (get_local $9)
                  (get_local $0)
                 )
                 (i32.store offset=24
                  (get_local $9)
                  (i32.const 0)
                 )
                )
                (call $_abort)
               )
              )
             )
            )
           )
           (set_global $STACKTOP
            (get_local $13)
           )
           (return
            (i32.add
             (get_local $5)
             (i32.const 8)
            )
           )
          )
          (set_local $2
           (get_local $4)
          )
         )
         (set_local $2
          (get_local $4)
         )
        )
       )
       (set_local $2
        (get_local $4)
       )
      )
     )
    )
   )
  )
  (if
   (i32.ge_u
    (tee_local $3
     (i32.load
      (i32.const 3928)
     )
    )
    (get_local $2)
   )
   (block
    (set_local $0
     (i32.load
      (i32.const 3940)
     )
    )
    (if
     (i32.gt_u
      (tee_local $1
       (i32.sub
        (get_local $3)
        (get_local $2)
       )
      )
      (i32.const 15)
     )
     (block
      (i32.store
       (i32.const 3940)
       (tee_local $3
        (i32.add
         (get_local $0)
         (get_local $2)
        )
       )
      )
      (i32.store
       (i32.const 3928)
       (get_local $1)
      )
      (i32.store offset=4
       (get_local $3)
       (i32.or
        (get_local $1)
        (i32.const 1)
       )
      )
      (i32.store
       (i32.add
        (get_local $3)
        (get_local $1)
       )
       (get_local $1)
      )
      (i32.store offset=4
       (get_local $0)
       (i32.or
        (get_local $2)
        (i32.const 3)
       )
      )
     )
     (block
      (i32.store
       (i32.const 3928)
       (i32.const 0)
      )
      (i32.store
       (i32.const 3940)
       (i32.const 0)
      )
      (i32.store offset=4
       (get_local $0)
       (i32.or
        (get_local $3)
        (i32.const 3)
       )
      )
      (i32.store
       (tee_local $2
        (i32.add
         (i32.add
          (get_local $0)
          (get_local $3)
         )
         (i32.const 4)
        )
       )
       (i32.or
        (i32.load
         (get_local $2)
        )
        (i32.const 1)
       )
      )
     )
    )
    (set_global $STACKTOP
     (get_local $13)
    )
    (return
     (i32.add
      (get_local $0)
      (i32.const 8)
     )
    )
   )
  )
  (if
   (i32.gt_u
    (tee_local $1
     (i32.load
      (i32.const 3932)
     )
    )
    (get_local $2)
   )
   (block
    (i32.store
     (i32.const 3932)
     (tee_local $1
      (i32.sub
       (get_local $1)
       (get_local $2)
      )
     )
    )
    (i32.store
     (i32.const 3944)
     (tee_local $3
      (i32.add
       (tee_local $0
        (i32.load
         (i32.const 3944)
        )
       )
       (get_local $2)
      )
     )
    )
    (i32.store offset=4
     (get_local $3)
     (i32.or
      (get_local $1)
      (i32.const 1)
     )
    )
    (i32.store offset=4
     (get_local $0)
     (i32.or
      (get_local $2)
      (i32.const 3)
     )
    )
    (set_global $STACKTOP
     (get_local $13)
    )
    (return
     (i32.add
      (get_local $0)
      (i32.const 8)
     )
    )
   )
  )
  (if
   (i32.le_u
    (tee_local $4
     (i32.and
      (tee_local $5
       (i32.add
        (tee_local $0
         (if (result i32)
          (i32.load
           (i32.const 4392)
          )
          (i32.load
           (i32.const 4400)
          )
          (block (result i32)
           (i32.store
            (i32.const 4400)
            (i32.const 4096)
           )
           (i32.store
            (i32.const 4396)
            (i32.const 4096)
           )
           (i32.store
            (i32.const 4404)
            (i32.const -1)
           )
           (i32.store
            (i32.const 4408)
            (i32.const -1)
           )
           (i32.store
            (i32.const 4412)
            (i32.const 0)
           )
           (i32.store
            (i32.const 4364)
            (i32.const 0)
           )
           (i32.store
            (get_local $15)
            (tee_local $0
             (i32.xor
              (i32.and
               (get_local $15)
               (i32.const -16)
              )
              (i32.const 1431655768)
             )
            )
           )
           (i32.store
            (i32.const 4392)
            (get_local $0)
           )
           (i32.const 4096)
          )
         )
        )
        (tee_local $6
         (i32.add
          (get_local $2)
          (i32.const 47)
         )
        )
       )
      )
      (tee_local $8
       (i32.sub
        (i32.const 0)
        (get_local $0)
       )
      )
     )
    )
    (get_local $2)
   )
   (block
    (set_global $STACKTOP
     (get_local $13)
    )
    (return
     (i32.const 0)
    )
   )
  )
  (if
   (tee_local $0
    (i32.load
     (i32.const 4360)
    )
   )
   (if
    (i32.or
     (i32.le_u
      (tee_local $7
       (i32.add
        (tee_local $3
         (i32.load
          (i32.const 4352)
         )
        )
        (get_local $4)
       )
      )
      (get_local $3)
     )
     (i32.gt_u
      (get_local $7)
      (get_local $0)
     )
    )
    (block
     (set_global $STACKTOP
      (get_local $13)
     )
     (return
      (i32.const 0)
     )
    )
   )
  )
  (set_local $7
   (i32.add
    (get_local $2)
    (i32.const 48)
   )
  )
  (block $__rjto$13
   (block $__rjti$13
    (if
     (i32.and
      (i32.load
       (i32.const 4364)
      )
      (i32.const 4)
     )
     (set_local $1
      (i32.const 0)
     )
     (block
      (block $do-once37
       (block $__rjti$5
        (block $__rjti$4
         (br_if $__rjti$4
          (i32.eqz
           (tee_local $0
            (i32.load
             (i32.const 3944)
            )
           )
          )
         )
         (set_local $3
          (i32.const 4368)
         )
         (loop $while-in34
          (block $while-out33
           (if
            (i32.le_u
             (tee_local $10
              (i32.load
               (get_local $3)
              )
             )
             (get_local $0)
            )
            (br_if $while-out33
             (i32.gt_u
              (i32.add
               (get_local $10)
               (i32.load
                (tee_local $10
                 (i32.add
                  (get_local $3)
                  (i32.const 4)
                 )
                )
               )
              )
              (get_local $0)
             )
            )
           )
           (br_if $while-in34
            (tee_local $3
             (i32.load offset=8
              (get_local $3)
             )
            )
           )
           (br $__rjti$4)
          )
         )
         (if
          (i32.lt_u
           (tee_local $1
            (i32.and
             (i32.sub
              (get_local $5)
              (get_local $1)
             )
             (get_local $8)
            )
           )
           (i32.const 2147483647)
          )
          (if
           (i32.eq
            (tee_local $0
             (call $_sbrk
              (get_local $1)
             )
            )
            (i32.add
             (i32.load
              (get_local $3)
             )
             (i32.load
              (get_local $10)
             )
            )
           )
           (br_if $__rjti$13
            (i32.ne
             (get_local $0)
             (i32.const -1)
            )
           )
           (br $__rjti$5)
          )
          (set_local $1
           (i32.const 0)
          )
         )
         (br $do-once37)
        )
        (if
         (i32.eq
          (tee_local $0
           (call $_sbrk
            (i32.const 0)
           )
          )
          (i32.const -1)
         )
         (set_local $1
          (i32.const 0)
         )
         (block
          (set_local $3
           (i32.sub
            (i32.and
             (i32.add
              (tee_local $5
               (i32.add
                (tee_local $3
                 (i32.load
                  (i32.const 4396)
                 )
                )
                (i32.const -1)
               )
              )
              (tee_local $1
               (get_local $0)
              )
             )
             (i32.sub
              (i32.const 0)
              (get_local $3)
             )
            )
            (get_local $1)
           )
          )
          (set_local $3
           (i32.add
            (tee_local $1
             (i32.add
              (if (result i32)
               (i32.and
                (get_local $5)
                (get_local $1)
               )
               (get_local $3)
               (i32.const 0)
              )
              (get_local $4)
             )
            )
            (tee_local $5
             (i32.load
              (i32.const 4352)
             )
            )
           )
          )
          (if
           (i32.and
            (i32.gt_u
             (get_local $1)
             (get_local $2)
            )
            (i32.lt_u
             (get_local $1)
             (i32.const 2147483647)
            )
           )
           (block
            (if
             (tee_local $8
              (i32.load
               (i32.const 4360)
              )
             )
             (if
              (i32.or
               (i32.le_u
                (get_local $3)
                (get_local $5)
               )
               (i32.gt_u
                (get_local $3)
                (get_local $8)
               )
              )
              (block
               (set_local $1
                (i32.const 0)
               )
               (br $do-once37)
              )
             )
            )
            (br_if $__rjti$13
             (i32.eq
              (tee_local $3
               (call $_sbrk
                (get_local $1)
               )
              )
              (get_local $0)
             )
            )
            (set_local $0
             (get_local $3)
            )
            (br $__rjti$5)
           )
           (set_local $1
            (i32.const 0)
           )
          )
         )
        )
        (br $do-once37)
       )
       (if
        (i32.eqz
         (i32.and
          (i32.gt_u
           (get_local $7)
           (get_local $1)
          )
          (i32.and
           (i32.lt_u
            (get_local $1)
            (i32.const 2147483647)
           )
           (i32.ne
            (get_local $0)
            (i32.const -1)
           )
          )
         )
        )
        (if
         (i32.eq
          (get_local $0)
          (i32.const -1)
         )
         (block
          (set_local $1
           (i32.const 0)
          )
          (br $do-once37)
         )
         (br $__rjti$13)
        )
       )
       (br_if $__rjti$13
        (i32.ge_u
         (tee_local $3
          (i32.and
           (i32.add
            (i32.sub
             (get_local $6)
             (get_local $1)
            )
            (tee_local $3
             (i32.load
              (i32.const 4400)
             )
            )
           )
           (i32.sub
            (i32.const 0)
            (get_local $3)
           )
          )
         )
         (i32.const 2147483647)
        )
       )
       (set_local $6
        (i32.sub
         (i32.const 0)
         (get_local $1)
        )
       )
       (if
        (i32.eq
         (call $_sbrk
          (get_local $3)
         )
         (i32.const -1)
        )
        (block
         (drop
          (call $_sbrk
           (get_local $6)
          )
         )
         (set_local $1
          (i32.const 0)
         )
        )
        (block
         (set_local $1
          (i32.add
           (get_local $3)
           (get_local $1)
          )
         )
         (br $__rjti$13)
        )
       )
      )
      (i32.store
       (i32.const 4364)
       (i32.or
        (i32.load
         (i32.const 4364)
        )
        (i32.const 4)
       )
      )
     )
    )
    (if
     (i32.lt_u
      (get_local $4)
      (i32.const 2147483647)
     )
     (block
      (set_local $4
       (i32.and
        (i32.lt_u
         (tee_local $0
          (call $_sbrk
           (get_local $4)
          )
         )
         (tee_local $3
          (call $_sbrk
           (i32.const 0)
          )
         )
        )
        (i32.and
         (i32.ne
          (get_local $0)
          (i32.const -1)
         )
         (i32.ne
          (get_local $3)
          (i32.const -1)
         )
        )
       )
      )
      (if
       (tee_local $6
        (i32.gt_u
         (tee_local $3
          (i32.sub
           (get_local $3)
           (get_local $0)
          )
         )
         (i32.add
          (get_local $2)
          (i32.const 40)
         )
        )
       )
       (set_local $1
        (get_local $3)
       )
      )
      (br_if $__rjti$13
       (i32.eqz
        (i32.or
         (i32.or
          (i32.eq
           (get_local $0)
           (i32.const -1)
          )
          (i32.xor
           (get_local $6)
           (i32.const 1)
          )
         )
         (i32.xor
          (get_local $4)
          (i32.const 1)
         )
        )
       )
      )
     )
    )
    (br $__rjto$13)
   )
   (i32.store
    (i32.const 4352)
    (tee_local $3
     (i32.add
      (i32.load
       (i32.const 4352)
      )
      (get_local $1)
     )
    )
   )
   (if
    (i32.gt_u
     (get_local $3)
     (i32.load
      (i32.const 4356)
     )
    )
    (i32.store
     (i32.const 4356)
     (get_local $3)
    )
   )
   (block $do-once39
    (if
     (tee_local $6
      (i32.load
       (i32.const 3944)
      )
     )
     (block
      (set_local $3
       (i32.const 4368)
      )
      (block $__rjto$10
       (block $__rjti$10
        (loop $while-in44
         (block $while-out43
          (br_if $__rjti$10
           (i32.eq
            (get_local $0)
            (i32.add
             (tee_local $4
              (i32.load
               (get_local $3)
              )
             )
             (tee_local $8
              (i32.load
               (tee_local $5
                (i32.add
                 (get_local $3)
                 (i32.const 4)
                )
               )
              )
             )
            )
           )
          )
          (br_if $while-in44
           (tee_local $3
            (i32.load offset=8
             (get_local $3)
            )
           )
          )
         )
        )
        (br $__rjto$10)
       )
       (if
        (i32.eqz
         (i32.and
          (i32.load offset=12
           (get_local $3)
          )
          (i32.const 8)
         )
        )
        (if
         (i32.and
          (i32.lt_u
           (get_local $6)
           (get_local $0)
          )
          (i32.ge_u
           (get_local $6)
           (get_local $4)
          )
         )
         (block
          (i32.store
           (get_local $5)
           (i32.add
            (get_local $8)
            (get_local $1)
           )
          )
          (set_local $4
           (i32.load
            (i32.const 3932)
           )
          )
          (set_local $0
           (i32.and
            (i32.sub
             (i32.const 0)
             (tee_local $3
              (i32.add
               (get_local $6)
               (i32.const 8)
              )
             )
            )
            (i32.const 7)
           )
          )
          (i32.store
           (i32.const 3944)
           (tee_local $3
            (i32.add
             (get_local $6)
             (if (result i32)
              (i32.and
               (get_local $3)
               (i32.const 7)
              )
              (get_local $0)
              (tee_local $0
               (i32.const 0)
              )
             )
            )
           )
          )
          (i32.store
           (i32.const 3932)
           (tee_local $0
            (i32.add
             (get_local $4)
             (i32.sub
              (get_local $1)
              (get_local $0)
             )
            )
           )
          )
          (i32.store offset=4
           (get_local $3)
           (i32.or
            (get_local $0)
            (i32.const 1)
           )
          )
          (i32.store offset=4
           (i32.add
            (get_local $3)
            (get_local $0)
           )
           (i32.const 40)
          )
          (i32.store
           (i32.const 3948)
           (i32.load
            (i32.const 4408)
           )
          )
          (br $do-once39)
         )
        )
       )
      )
      (if
       (i32.lt_u
        (get_local $0)
        (tee_local $3
         (i32.load
          (i32.const 3936)
         )
        )
       )
       (block
        (i32.store
         (i32.const 3936)
         (get_local $0)
        )
        (set_local $3
         (get_local $0)
        )
       )
      )
      (set_local $5
       (i32.add
        (get_local $0)
        (get_local $1)
       )
      )
      (set_local $4
       (i32.const 4368)
      )
      (block $__rjto$11
       (block $__rjti$11
        (loop $while-in46
         (block $while-out45
          (br_if $__rjti$11
           (i32.eq
            (i32.load
             (get_local $4)
            )
            (get_local $5)
           )
          )
          (br_if $while-in46
           (tee_local $4
            (i32.load offset=8
             (get_local $4)
            )
           )
          )
         )
        )
        (br $__rjto$11)
       )
       (if
        (i32.eqz
         (i32.and
          (i32.load offset=12
           (get_local $4)
          )
          (i32.const 8)
         )
        )
        (block
         (i32.store
          (get_local $4)
          (get_local $0)
         )
         (i32.store
          (tee_local $4
           (i32.add
            (get_local $4)
            (i32.const 4)
           )
          )
          (i32.add
           (i32.load
            (get_local $4)
           )
           (get_local $1)
          )
         )
         (set_local $4
          (i32.and
           (i32.sub
            (i32.const 0)
            (tee_local $1
             (i32.add
              (get_local $0)
              (i32.const 8)
             )
            )
           )
           (i32.const 7)
          )
         )
         (set_local $10
          (i32.and
           (i32.sub
            (i32.const 0)
            (tee_local $8
             (i32.add
              (get_local $5)
              (i32.const 8)
             )
            )
           )
           (i32.const 7)
          )
         )
         (set_local $7
          (i32.add
           (tee_local $9
            (i32.add
             (get_local $0)
             (if (result i32)
              (i32.and
               (get_local $1)
               (i32.const 7)
              )
              (get_local $4)
              (i32.const 0)
             )
            )
           )
           (get_local $2)
          )
         )
         (set_local $8
          (i32.sub
           (i32.sub
            (tee_local $5
             (i32.add
              (get_local $5)
              (if (result i32)
               (i32.and
                (get_local $8)
                (i32.const 7)
               )
               (get_local $10)
               (i32.const 0)
              )
             )
            )
            (get_local $9)
           )
           (get_local $2)
          )
         )
         (i32.store offset=4
          (get_local $9)
          (i32.or
           (get_local $2)
           (i32.const 3)
          )
         )
         (block $do-once47
          (if
           (i32.eq
            (get_local $5)
            (get_local $6)
           )
           (block
            (i32.store
             (i32.const 3932)
             (tee_local $0
              (i32.add
               (i32.load
                (i32.const 3932)
               )
               (get_local $8)
              )
             )
            )
            (i32.store
             (i32.const 3944)
             (get_local $7)
            )
            (i32.store offset=4
             (get_local $7)
             (i32.or
              (get_local $0)
              (i32.const 1)
             )
            )
           )
           (block
            (if
             (i32.eq
              (get_local $5)
              (i32.load
               (i32.const 3940)
              )
             )
             (block
              (i32.store
               (i32.const 3928)
               (tee_local $0
                (i32.add
                 (i32.load
                  (i32.const 3928)
                 )
                 (get_local $8)
                )
               )
              )
              (i32.store
               (i32.const 3940)
               (get_local $7)
              )
              (i32.store offset=4
               (get_local $7)
               (i32.or
                (get_local $0)
                (i32.const 1)
               )
              )
              (i32.store
               (i32.add
                (get_local $7)
                (get_local $0)
               )
               (get_local $0)
              )
              (br $do-once47)
             )
            )
            (set_local $4
             (if (result i32)
              (i32.eq
               (i32.and
                (tee_local $0
                 (i32.load offset=4
                  (get_local $5)
                 )
                )
                (i32.const 3)
               )
               (i32.const 1)
              )
              (block (result i32)
               (set_local $10
                (i32.and
                 (get_local $0)
                 (i32.const -8)
                )
               )
               (set_local $4
                (i32.shr_u
                 (get_local $0)
                 (i32.const 3)
                )
               )
               (block $label$break$L314
                (if
                 (i32.lt_u
                  (get_local $0)
                  (i32.const 256)
                 )
                 (block
                  (set_local $2
                   (i32.load offset=12
                    (get_local $5)
                   )
                  )
                  (block $do-once50
                   (if
                    (i32.ne
                     (tee_local $1
                      (i32.load offset=8
                       (get_local $5)
                      )
                     )
                     (tee_local $0
                      (i32.add
                       (i32.shl
                        (get_local $4)
                        (i32.const 3)
                       )
                       (i32.const 3960)
                      )
                     )
                    )
                    (block
                     (if
                      (i32.lt_u
                       (get_local $1)
                       (get_local $3)
                      )
                      (call $_abort)
                     )
                     (br_if $do-once50
                      (i32.eq
                       (i32.load offset=12
                        (get_local $1)
                       )
                       (get_local $5)
                      )
                     )
                     (call $_abort)
                    )
                   )
                  )
                  (if
                   (i32.eq
                    (get_local $2)
                    (get_local $1)
                   )
                   (block
                    (i32.store
                     (i32.const 3920)
                     (i32.and
                      (i32.load
                       (i32.const 3920)
                      )
                      (i32.xor
                       (i32.shl
                        (i32.const 1)
                        (get_local $4)
                       )
                       (i32.const -1)
                      )
                     )
                    )
                    (br $label$break$L314)
                   )
                  )
                  (block $do-once52
                   (if
                    (i32.eq
                     (get_local $2)
                     (get_local $0)
                    )
                    (set_local $18
                     (i32.add
                      (get_local $2)
                      (i32.const 8)
                     )
                    )
                    (block
                     (if
                      (i32.lt_u
                       (get_local $2)
                       (get_local $3)
                      )
                      (call $_abort)
                     )
                     (if
                      (i32.eq
                       (i32.load
                        (tee_local $0
                         (i32.add
                          (get_local $2)
                          (i32.const 8)
                         )
                        )
                       )
                       (get_local $5)
                      )
                      (block
                       (set_local $18
                        (get_local $0)
                       )
                       (br $do-once52)
                      )
                     )
                     (call $_abort)
                    )
                   )
                  )
                  (i32.store offset=12
                   (get_local $1)
                   (get_local $2)
                  )
                  (i32.store
                   (get_local $18)
                   (get_local $1)
                  )
                 )
                 (block
                  (set_local $6
                   (i32.load offset=24
                    (get_local $5)
                   )
                  )
                  (block $do-once54
                   (if
                    (i32.eq
                     (tee_local $0
                      (i32.load offset=12
                       (get_local $5)
                      )
                     )
                     (get_local $5)
                    )
                    (block
                     (if
                      (tee_local $0
                       (i32.load
                        (tee_local $1
                         (i32.add
                          (tee_local $2
                           (i32.add
                            (get_local $5)
                            (i32.const 16)
                           )
                          )
                          (i32.const 4)
                         )
                        )
                       )
                      )
                      (set_local $2
                       (get_local $1)
                      )
                      (if
                       (i32.eqz
                        (tee_local $0
                         (i32.load
                          (get_local $2)
                         )
                        )
                       )
                       (block
                        (set_local $11
                         (i32.const 0)
                        )
                        (br $do-once54)
                       )
                      )
                     )
                     (loop $while-in57
                      (if
                       (tee_local $4
                        (i32.load
                         (tee_local $1
                          (i32.add
                           (get_local $0)
                           (i32.const 20)
                          )
                         )
                        )
                       )
                       (block
                        (set_local $0
                         (get_local $4)
                        )
                        (set_local $2
                         (get_local $1)
                        )
                        (br $while-in57)
                       )
                      )
                      (if
                       (tee_local $4
                        (i32.load
                         (tee_local $1
                          (i32.add
                           (get_local $0)
                           (i32.const 16)
                          )
                         )
                        )
                       )
                       (block
                        (set_local $0
                         (get_local $4)
                        )
                        (set_local $2
                         (get_local $1)
                        )
                        (br $while-in57)
                       )
                      )
                     )
                     (if
                      (i32.lt_u
                       (get_local $2)
                       (get_local $3)
                      )
                      (call $_abort)
                      (block
                       (i32.store
                        (get_local $2)
                        (i32.const 0)
                       )
                       (set_local $11
                        (get_local $0)
                       )
                      )
                     )
                    )
                    (block
                     (if
                      (i32.lt_u
                       (tee_local $2
                        (i32.load offset=8
                         (get_local $5)
                        )
                       )
                       (get_local $3)
                      )
                      (call $_abort)
                     )
                     (if
                      (i32.ne
                       (i32.load
                        (tee_local $1
                         (i32.add
                          (get_local $2)
                          (i32.const 12)
                         )
                        )
                       )
                       (get_local $5)
                      )
                      (call $_abort)
                     )
                     (if
                      (i32.eq
                       (i32.load
                        (tee_local $3
                         (i32.add
                          (get_local $0)
                          (i32.const 8)
                         )
                        )
                       )
                       (get_local $5)
                      )
                      (block
                       (i32.store
                        (get_local $1)
                        (get_local $0)
                       )
                       (i32.store
                        (get_local $3)
                        (get_local $2)
                       )
                       (set_local $11
                        (get_local $0)
                       )
                      )
                      (call $_abort)
                     )
                    )
                   )
                  )
                  (br_if $label$break$L314
                   (i32.eqz
                    (get_local $6)
                   )
                  )
                  (block $do-once58
                   (if
                    (i32.eq
                     (get_local $5)
                     (i32.load
                      (tee_local $2
                       (i32.add
                        (i32.shl
                         (tee_local $0
                          (i32.load offset=28
                           (get_local $5)
                          )
                         )
                         (i32.const 2)
                        )
                        (i32.const 4224)
                       )
                      )
                     )
                    )
                    (block
                     (i32.store
                      (get_local $2)
                      (get_local $11)
                     )
                     (br_if $do-once58
                      (get_local $11)
                     )
                     (i32.store
                      (i32.const 3924)
                      (i32.and
                       (i32.load
                        (i32.const 3924)
                       )
                       (i32.xor
                        (i32.shl
                         (i32.const 1)
                         (get_local $0)
                        )
                        (i32.const -1)
                       )
                      )
                     )
                     (br $label$break$L314)
                    )
                    (if
                     (i32.lt_u
                      (get_local $6)
                      (i32.load
                       (i32.const 3936)
                      )
                     )
                     (call $_abort)
                     (block
                      (i32.store
                       (i32.add
                        (i32.add
                         (get_local $6)
                         (i32.const 16)
                        )
                        (i32.shl
                         (i32.ne
                          (i32.load offset=16
                           (get_local $6)
                          )
                          (get_local $5)
                         )
                         (i32.const 2)
                        )
                       )
                       (get_local $11)
                      )
                      (br_if $label$break$L314
                       (i32.eqz
                        (get_local $11)
                       )
                      )
                     )
                    )
                   )
                  )
                  (if
                   (i32.lt_u
                    (get_local $11)
                    (tee_local $2
                     (i32.load
                      (i32.const 3936)
                     )
                    )
                   )
                   (call $_abort)
                  )
                  (i32.store offset=24
                   (get_local $11)
                   (get_local $6)
                  )
                  (if
                   (tee_local $0
                    (i32.load
                     (tee_local $1
                      (i32.add
                       (get_local $5)
                       (i32.const 16)
                      )
                     )
                    )
                   )
                   (if
                    (i32.lt_u
                     (get_local $0)
                     (get_local $2)
                    )
                    (call $_abort)
                    (block
                     (i32.store offset=16
                      (get_local $11)
                      (get_local $0)
                     )
                     (i32.store offset=24
                      (get_local $0)
                      (get_local $11)
                     )
                    )
                   )
                  )
                  (br_if $label$break$L314
                   (i32.eqz
                    (tee_local $0
                     (i32.load offset=4
                      (get_local $1)
                     )
                    )
                   )
                  )
                  (if
                   (i32.lt_u
                    (get_local $0)
                    (i32.load
                     (i32.const 3936)
                    )
                   )
                   (call $_abort)
                   (block
                    (i32.store offset=20
                     (get_local $11)
                     (get_local $0)
                    )
                    (i32.store offset=24
                     (get_local $0)
                     (get_local $11)
                    )
                   )
                  )
                 )
                )
               )
               (set_local $5
                (i32.add
                 (get_local $5)
                 (get_local $10)
                )
               )
               (i32.add
                (get_local $10)
                (get_local $8)
               )
              )
              (get_local $8)
             )
            )
            (i32.store
             (tee_local $0
              (i32.add
               (get_local $5)
               (i32.const 4)
              )
             )
             (i32.and
              (i32.load
               (get_local $0)
              )
              (i32.const -2)
             )
            )
            (i32.store offset=4
             (get_local $7)
             (i32.or
              (get_local $4)
              (i32.const 1)
             )
            )
            (i32.store
             (i32.add
              (get_local $7)
              (get_local $4)
             )
             (get_local $4)
            )
            (set_local $2
             (i32.shr_u
              (get_local $4)
              (i32.const 3)
             )
            )
            (if
             (i32.lt_u
              (get_local $4)
              (i32.const 256)
             )
             (block
              (set_local $0
               (i32.add
                (i32.shl
                 (get_local $2)
                 (i32.const 3)
                )
                (i32.const 3960)
               )
              )
              (block $do-once62
               (if
                (i32.and
                 (tee_local $1
                  (i32.load
                   (i32.const 3920)
                  )
                 )
                 (tee_local $2
                  (i32.shl
                   (i32.const 1)
                   (get_local $2)
                  )
                 )
                )
                (block
                 (if
                  (i32.ge_u
                   (tee_local $1
                    (i32.load
                     (tee_local $2
                      (i32.add
                       (get_local $0)
                       (i32.const 8)
                      )
                     )
                    )
                   )
                   (i32.load
                    (i32.const 3936)
                   )
                  )
                  (block
                   (set_local $12
                    (get_local $1)
                   )
                   (set_local $19
                    (get_local $2)
                   )
                   (br $do-once62)
                  )
                 )
                 (call $_abort)
                )
                (block
                 (i32.store
                  (i32.const 3920)
                  (i32.or
                   (get_local $1)
                   (get_local $2)
                  )
                 )
                 (set_local $12
                  (get_local $0)
                 )
                 (set_local $19
                  (i32.add
                   (get_local $0)
                   (i32.const 8)
                  )
                 )
                )
               )
              )
              (i32.store
               (get_local $19)
               (get_local $7)
              )
              (i32.store offset=12
               (get_local $12)
               (get_local $7)
              )
              (i32.store offset=8
               (get_local $7)
               (get_local $12)
              )
              (i32.store offset=12
               (get_local $7)
               (get_local $0)
              )
              (br $do-once47)
             )
            )
            (set_local $0
             (i32.add
              (i32.shl
               (tee_local $2
                (block $do-once64 (result i32)
                 (if (result i32)
                  (tee_local $0
                   (i32.shr_u
                    (get_local $4)
                    (i32.const 8)
                   )
                  )
                  (block (result i32)
                   (drop
                    (br_if $do-once64
                     (i32.const 31)
                     (i32.gt_u
                      (get_local $4)
                      (i32.const 16777215)
                     )
                    )
                   )
                   (i32.or
                    (i32.and
                     (i32.shr_u
                      (get_local $4)
                      (i32.add
                       (tee_local $0
                        (i32.add
                         (i32.sub
                          (i32.const 14)
                          (i32.or
                           (i32.or
                            (tee_local $1
                             (i32.and
                              (i32.shr_u
                               (i32.add
                                (tee_local $2
                                 (i32.shl
                                  (get_local $0)
                                  (tee_local $0
                                   (i32.and
                                    (i32.shr_u
                                     (i32.add
                                      (get_local $0)
                                      (i32.const 1048320)
                                     )
                                     (i32.const 16)
                                    )
                                    (i32.const 8)
                                   )
                                  )
                                 )
                                )
                                (i32.const 520192)
                               )
                               (i32.const 16)
                              )
                              (i32.const 4)
                             )
                            )
                            (get_local $0)
                           )
                           (tee_local $2
                            (i32.and
                             (i32.shr_u
                              (i32.add
                               (tee_local $0
                                (i32.shl
                                 (get_local $2)
                                 (get_local $1)
                                )
                               )
                               (i32.const 245760)
                              )
                              (i32.const 16)
                             )
                             (i32.const 2)
                            )
                           )
                          )
                         )
                         (i32.shr_u
                          (i32.shl
                           (get_local $0)
                           (get_local $2)
                          )
                          (i32.const 15)
                         )
                        )
                       )
                       (i32.const 7)
                      )
                     )
                     (i32.const 1)
                    )
                    (i32.shl
                     (get_local $0)
                     (i32.const 1)
                    )
                   )
                  )
                  (i32.const 0)
                 )
                )
               )
               (i32.const 2)
              )
              (i32.const 4224)
             )
            )
            (i32.store offset=28
             (get_local $7)
             (get_local $2)
            )
            (i32.store offset=4
             (tee_local $1
              (i32.add
               (get_local $7)
               (i32.const 16)
              )
             )
             (i32.const 0)
            )
            (i32.store
             (get_local $1)
             (i32.const 0)
            )
            (if
             (i32.eqz
              (i32.and
               (tee_local $1
                (i32.load
                 (i32.const 3924)
                )
               )
               (tee_local $3
                (i32.shl
                 (i32.const 1)
                 (get_local $2)
                )
               )
              )
             )
             (block
              (i32.store
               (i32.const 3924)
               (i32.or
                (get_local $1)
                (get_local $3)
               )
              )
              (i32.store
               (get_local $0)
               (get_local $7)
              )
              (i32.store offset=24
               (get_local $7)
               (get_local $0)
              )
              (i32.store offset=12
               (get_local $7)
               (get_local $7)
              )
              (i32.store offset=8
               (get_local $7)
               (get_local $7)
              )
              (br $do-once47)
             )
            )
            (set_local $0
             (i32.load
              (get_local $0)
             )
            )
            (set_local $1
             (i32.sub
              (i32.const 25)
              (i32.shr_u
               (get_local $2)
               (i32.const 1)
              )
             )
            )
            (set_local $2
             (i32.shl
              (get_local $4)
              (if (result i32)
               (i32.eq
                (get_local $2)
                (i32.const 31)
               )
               (i32.const 0)
               (get_local $1)
              )
             )
            )
            (block $__rjto$7
             (block $__rjti$7
              (loop $while-in67
               (block $while-out66
                (br_if $__rjti$7
                 (i32.eq
                  (i32.and
                   (i32.load offset=4
                    (get_local $0)
                   )
                   (i32.const -8)
                  )
                  (get_local $4)
                 )
                )
                (set_local $1
                 (i32.shl
                  (get_local $2)
                  (i32.const 1)
                 )
                )
                (if
                 (tee_local $3
                  (i32.load
                   (tee_local $2
                    (i32.add
                     (i32.add
                      (get_local $0)
                      (i32.const 16)
                     )
                     (i32.shl
                      (i32.shr_u
                       (get_local $2)
                       (i32.const 31)
                      )
                      (i32.const 2)
                     )
                    )
                   )
                  )
                 )
                 (block
                  (set_local $2
                   (get_local $1)
                  )
                  (set_local $0
                   (get_local $3)
                  )
                  (br $while-in67)
                 )
                )
               )
              )
              (if
               (i32.lt_u
                (get_local $2)
                (i32.load
                 (i32.const 3936)
                )
               )
               (call $_abort)
               (block
                (i32.store
                 (get_local $2)
                 (get_local $7)
                )
                (i32.store offset=24
                 (get_local $7)
                 (get_local $0)
                )
                (i32.store offset=12
                 (get_local $7)
                 (get_local $7)
                )
                (i32.store offset=8
                 (get_local $7)
                 (get_local $7)
                )
                (br $do-once47)
               )
              )
              (br $__rjto$7)
             )
             (if
              (i32.and
               (i32.ge_u
                (tee_local $2
                 (i32.load
                  (tee_local $1
                   (i32.add
                    (get_local $0)
                    (i32.const 8)
                   )
                  )
                 )
                )
                (tee_local $3
                 (i32.load
                  (i32.const 3936)
                 )
                )
               )
               (i32.ge_u
                (get_local $0)
                (get_local $3)
               )
              )
              (block
               (i32.store offset=12
                (get_local $2)
                (get_local $7)
               )
               (i32.store
                (get_local $1)
                (get_local $7)
               )
               (i32.store offset=8
                (get_local $7)
                (get_local $2)
               )
               (i32.store offset=12
                (get_local $7)
                (get_local $0)
               )
               (i32.store offset=24
                (get_local $7)
                (i32.const 0)
               )
              )
              (call $_abort)
             )
            )
           )
          )
         )
         (set_global $STACKTOP
          (get_local $13)
         )
         (return
          (i32.add
           (get_local $9)
           (i32.const 8)
          )
         )
        )
       )
      )
      (set_local $3
       (i32.const 4368)
      )
      (loop $while-in69
       (block $while-out68
        (if
         (i32.le_u
          (tee_local $4
           (i32.load
            (get_local $3)
           )
          )
          (get_local $6)
         )
         (br_if $while-out68
          (i32.gt_u
           (tee_local $11
            (i32.add
             (get_local $4)
             (i32.load offset=4
              (get_local $3)
             )
            )
           )
           (get_local $6)
          )
         )
        )
        (set_local $3
         (i32.load offset=8
          (get_local $3)
         )
        )
        (br $while-in69)
       )
      )
      (set_local $5
       (i32.and
        (i32.sub
         (i32.const 0)
         (tee_local $4
          (i32.add
           (tee_local $3
            (i32.add
             (get_local $11)
             (i32.const -47)
            )
           )
           (i32.const 8)
          )
         )
        )
        (i32.const 7)
       )
      )
      (set_local $8
       (i32.add
        (if (result i32)
         (i32.lt_u
          (tee_local $3
           (i32.add
            (get_local $3)
            (if (result i32)
             (i32.and
              (get_local $4)
              (i32.const 7)
             )
             (get_local $5)
             (i32.const 0)
            )
           )
          )
          (tee_local $12
           (i32.add
            (get_local $6)
            (i32.const 16)
           )
          )
         )
         (tee_local $3
          (get_local $6)
         )
         (get_local $3)
        )
        (i32.const 8)
       )
      )
      (set_local $4
       (i32.add
        (get_local $3)
        (i32.const 24)
       )
      )
      (set_local $10
       (i32.add
        (get_local $1)
        (i32.const -40)
       )
      )
      (set_local $5
       (i32.and
        (i32.sub
         (i32.const 0)
         (tee_local $7
          (i32.add
           (get_local $0)
           (i32.const 8)
          )
         )
        )
        (i32.const 7)
       )
      )
      (i32.store
       (i32.const 3944)
       (tee_local $7
        (i32.add
         (get_local $0)
         (if (result i32)
          (i32.and
           (get_local $7)
           (i32.const 7)
          )
          (get_local $5)
          (tee_local $5
           (i32.const 0)
          )
         )
        )
       )
      )
      (i32.store
       (i32.const 3932)
       (tee_local $5
        (i32.sub
         (get_local $10)
         (get_local $5)
        )
       )
      )
      (i32.store offset=4
       (get_local $7)
       (i32.or
        (get_local $5)
        (i32.const 1)
       )
      )
      (i32.store offset=4
       (i32.add
        (get_local $7)
        (get_local $5)
       )
       (i32.const 40)
      )
      (i32.store
       (i32.const 3948)
       (i32.load
        (i32.const 4408)
       )
      )
      (i32.store
       (tee_local $5
        (i32.add
         (get_local $3)
         (i32.const 4)
        )
       )
       (i32.const 27)
      )
      (i64.store align=4
       (get_local $8)
       (i64.load align=4
        (i32.const 4368)
       )
      )
      (i64.store offset=8 align=4
       (get_local $8)
       (i64.load align=4
        (i32.const 4376)
       )
      )
      (i32.store
       (i32.const 4368)
       (get_local $0)
      )
      (i32.store
       (i32.const 4372)
       (get_local $1)
      )
      (i32.store
       (i32.const 4380)
       (i32.const 0)
      )
      (i32.store
       (i32.const 4376)
       (get_local $8)
      )
      (set_local $0
       (get_local $4)
      )
      (loop $while-in71
       (i32.store
        (tee_local $1
         (i32.add
          (get_local $0)
          (i32.const 4)
         )
        )
        (i32.const 7)
       )
       (if
        (i32.lt_u
         (i32.add
          (get_local $0)
          (i32.const 8)
         )
         (get_local $11)
        )
        (block
         (set_local $0
          (get_local $1)
         )
         (br $while-in71)
        )
       )
      )
      (if
       (i32.ne
        (get_local $3)
        (get_local $6)
       )
       (block
        (i32.store
         (get_local $5)
         (i32.and
          (i32.load
           (get_local $5)
          )
          (i32.const -2)
         )
        )
        (i32.store offset=4
         (get_local $6)
         (i32.or
          (tee_local $5
           (i32.sub
            (get_local $3)
            (get_local $6)
           )
          )
          (i32.const 1)
         )
        )
        (i32.store
         (get_local $3)
         (get_local $5)
        )
        (set_local $1
         (i32.shr_u
          (get_local $5)
          (i32.const 3)
         )
        )
        (if
         (i32.lt_u
          (get_local $5)
          (i32.const 256)
         )
         (block
          (set_local $0
           (i32.add
            (i32.shl
             (get_local $1)
             (i32.const 3)
            )
            (i32.const 3960)
           )
          )
          (if
           (i32.and
            (tee_local $3
             (i32.load
              (i32.const 3920)
             )
            )
            (tee_local $1
             (i32.shl
              (i32.const 1)
              (get_local $1)
             )
            )
           )
           (if
            (i32.lt_u
             (tee_local $3
              (i32.load
               (tee_local $1
                (i32.add
                 (get_local $0)
                 (i32.const 8)
                )
               )
              )
             )
             (i32.load
              (i32.const 3936)
             )
            )
            (call $_abort)
            (block
             (set_local $9
              (get_local $3)
             )
             (set_local $20
              (get_local $1)
             )
            )
           )
           (block
            (i32.store
             (i32.const 3920)
             (i32.or
              (get_local $3)
              (get_local $1)
             )
            )
            (set_local $9
             (get_local $0)
            )
            (set_local $20
             (i32.add
              (get_local $0)
              (i32.const 8)
             )
            )
           )
          )
          (i32.store
           (get_local $20)
           (get_local $6)
          )
          (i32.store offset=12
           (get_local $9)
           (get_local $6)
          )
          (i32.store offset=8
           (get_local $6)
           (get_local $9)
          )
          (i32.store offset=12
           (get_local $6)
           (get_local $0)
          )
          (br $do-once39)
         )
        )
        (set_local $0
         (i32.add
          (i32.shl
           (tee_local $1
            (if (result i32)
             (tee_local $0
              (i32.shr_u
               (get_local $5)
               (i32.const 8)
              )
             )
             (if (result i32)
              (i32.gt_u
               (get_local $5)
               (i32.const 16777215)
              )
              (i32.const 31)
              (i32.or
               (i32.and
                (i32.shr_u
                 (get_local $5)
                 (i32.add
                  (tee_local $0
                   (i32.add
                    (i32.sub
                     (i32.const 14)
                     (i32.or
                      (i32.or
                       (tee_local $3
                        (i32.and
                         (i32.shr_u
                          (i32.add
                           (tee_local $1
                            (i32.shl
                             (get_local $0)
                             (tee_local $0
                              (i32.and
                               (i32.shr_u
                                (i32.add
                                 (get_local $0)
                                 (i32.const 1048320)
                                )
                                (i32.const 16)
                               )
                               (i32.const 8)
                              )
                             )
                            )
                           )
                           (i32.const 520192)
                          )
                          (i32.const 16)
                         )
                         (i32.const 4)
                        )
                       )
                       (get_local $0)
                      )
                      (tee_local $1
                       (i32.and
                        (i32.shr_u
                         (i32.add
                          (tee_local $0
                           (i32.shl
                            (get_local $1)
                            (get_local $3)
                           )
                          )
                          (i32.const 245760)
                         )
                         (i32.const 16)
                        )
                        (i32.const 2)
                       )
                      )
                     )
                    )
                    (i32.shr_u
                     (i32.shl
                      (get_local $0)
                      (get_local $1)
                     )
                     (i32.const 15)
                    )
                   )
                  )
                  (i32.const 7)
                 )
                )
                (i32.const 1)
               )
               (i32.shl
                (get_local $0)
                (i32.const 1)
               )
              )
             )
             (i32.const 0)
            )
           )
           (i32.const 2)
          )
          (i32.const 4224)
         )
        )
        (i32.store offset=28
         (get_local $6)
         (get_local $1)
        )
        (i32.store offset=20
         (get_local $6)
         (i32.const 0)
        )
        (i32.store
         (get_local $12)
         (i32.const 0)
        )
        (if
         (i32.eqz
          (i32.and
           (tee_local $3
            (i32.load
             (i32.const 3924)
            )
           )
           (tee_local $4
            (i32.shl
             (i32.const 1)
             (get_local $1)
            )
           )
          )
         )
         (block
          (i32.store
           (i32.const 3924)
           (i32.or
            (get_local $3)
            (get_local $4)
           )
          )
          (i32.store
           (get_local $0)
           (get_local $6)
          )
          (i32.store offset=24
           (get_local $6)
           (get_local $0)
          )
          (i32.store offset=12
           (get_local $6)
           (get_local $6)
          )
          (i32.store offset=8
           (get_local $6)
           (get_local $6)
          )
          (br $do-once39)
         )
        )
        (set_local $0
         (i32.load
          (get_local $0)
         )
        )
        (set_local $3
         (i32.sub
          (i32.const 25)
          (i32.shr_u
           (get_local $1)
           (i32.const 1)
          )
         )
        )
        (set_local $1
         (i32.shl
          (get_local $5)
          (if (result i32)
           (i32.eq
            (get_local $1)
            (i32.const 31)
           )
           (i32.const 0)
           (get_local $3)
          )
         )
        )
        (block $__rjto$9
         (block $__rjti$9
          (loop $while-in73
           (block $while-out72
            (br_if $__rjti$9
             (i32.eq
              (i32.and
               (i32.load offset=4
                (get_local $0)
               )
               (i32.const -8)
              )
              (get_local $5)
             )
            )
            (set_local $3
             (i32.shl
              (get_local $1)
              (i32.const 1)
             )
            )
            (if
             (tee_local $4
              (i32.load
               (tee_local $1
                (i32.add
                 (i32.add
                  (get_local $0)
                  (i32.const 16)
                 )
                 (i32.shl
                  (i32.shr_u
                   (get_local $1)
                   (i32.const 31)
                  )
                  (i32.const 2)
                 )
                )
               )
              )
             )
             (block
              (set_local $1
               (get_local $3)
              )
              (set_local $0
               (get_local $4)
              )
              (br $while-in73)
             )
            )
           )
          )
          (if
           (i32.lt_u
            (get_local $1)
            (i32.load
             (i32.const 3936)
            )
           )
           (call $_abort)
           (block
            (i32.store
             (get_local $1)
             (get_local $6)
            )
            (i32.store offset=24
             (get_local $6)
             (get_local $0)
            )
            (i32.store offset=12
             (get_local $6)
             (get_local $6)
            )
            (i32.store offset=8
             (get_local $6)
             (get_local $6)
            )
            (br $do-once39)
           )
          )
          (br $__rjto$9)
         )
         (if
          (i32.and
           (i32.ge_u
            (tee_local $1
             (i32.load
              (tee_local $3
               (i32.add
                (get_local $0)
                (i32.const 8)
               )
              )
             )
            )
            (tee_local $4
             (i32.load
              (i32.const 3936)
             )
            )
           )
           (i32.ge_u
            (get_local $0)
            (get_local $4)
           )
          )
          (block
           (i32.store offset=12
            (get_local $1)
            (get_local $6)
           )
           (i32.store
            (get_local $3)
            (get_local $6)
           )
           (i32.store offset=8
            (get_local $6)
            (get_local $1)
           )
           (i32.store offset=12
            (get_local $6)
            (get_local $0)
           )
           (i32.store offset=24
            (get_local $6)
            (i32.const 0)
           )
          )
          (call $_abort)
         )
        )
       )
      )
     )
     (block
      (if
       (i32.or
        (i32.eqz
         (tee_local $3
          (i32.load
           (i32.const 3936)
          )
         )
        )
        (i32.lt_u
         (get_local $0)
         (get_local $3)
        )
       )
       (i32.store
        (i32.const 3936)
        (get_local $0)
       )
      )
      (i32.store
       (i32.const 4368)
       (get_local $0)
      )
      (i32.store
       (i32.const 4372)
       (get_local $1)
      )
      (i32.store
       (i32.const 4380)
       (i32.const 0)
      )
      (i32.store
       (i32.const 3956)
       (i32.load
        (i32.const 4392)
       )
      )
      (i32.store
       (i32.const 3952)
       (i32.const -1)
      )
      (set_local $3
       (i32.const 0)
      )
      (loop $while-in42
       (i32.store offset=12
        (tee_local $4
         (i32.add
          (i32.shl
           (get_local $3)
           (i32.const 3)
          )
          (i32.const 3960)
         )
        )
        (get_local $4)
       )
       (i32.store offset=8
        (get_local $4)
        (get_local $4)
       )
       (br_if $while-in42
        (i32.ne
         (tee_local $3
          (i32.add
           (get_local $3)
           (i32.const 1)
          )
         )
         (i32.const 32)
        )
       )
      )
      (set_local $3
       (i32.add
        (get_local $1)
        (i32.const -40)
       )
      )
      (set_local $1
       (i32.and
        (i32.sub
         (i32.const 0)
         (tee_local $4
          (i32.add
           (get_local $0)
           (i32.const 8)
          )
         )
        )
        (i32.const 7)
       )
      )
      (i32.store
       (i32.const 3944)
       (tee_local $0
        (i32.add
         (get_local $0)
         (if (result i32)
          (i32.and
           (get_local $4)
           (i32.const 7)
          )
          (get_local $1)
          (tee_local $1
           (i32.const 0)
          )
         )
        )
       )
      )
      (i32.store
       (i32.const 3932)
       (tee_local $1
        (i32.sub
         (get_local $3)
         (get_local $1)
        )
       )
      )
      (i32.store offset=4
       (get_local $0)
       (i32.or
        (get_local $1)
        (i32.const 1)
       )
      )
      (i32.store offset=4
       (i32.add
        (get_local $0)
        (get_local $1)
       )
       (i32.const 40)
      )
      (i32.store
       (i32.const 3948)
       (i32.load
        (i32.const 4408)
       )
      )
     )
    )
   )
   (if
    (i32.gt_u
     (tee_local $0
      (i32.load
       (i32.const 3932)
      )
     )
     (get_local $2)
    )
    (block
     (i32.store
      (i32.const 3932)
      (tee_local $1
       (i32.sub
        (get_local $0)
        (get_local $2)
       )
      )
     )
     (i32.store
      (i32.const 3944)
      (tee_local $3
       (i32.add
        (tee_local $0
         (i32.load
          (i32.const 3944)
         )
        )
        (get_local $2)
       )
      )
     )
     (i32.store offset=4
      (get_local $3)
      (i32.or
       (get_local $1)
       (i32.const 1)
      )
     )
     (i32.store offset=4
      (get_local $0)
      (i32.or
       (get_local $2)
       (i32.const 3)
      )
     )
     (set_global $STACKTOP
      (get_local $13)
     )
     (return
      (i32.add
       (get_local $0)
       (i32.const 8)
      )
     )
    )
   )
  )
  (i32.store
   (call $___errno_location)
   (i32.const 12)
  )
  (set_global $STACKTOP
   (get_local $13)
  )
  (i32.const 0)
 )
 (func $_free (param $0 i32)
  (local $1 i32)
  (local $2 i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (local $6 i32)
  (local $7 i32)
  (local $8 i32)
  (local $9 i32)
  (local $10 i32)
  (local $11 i32)
  (local $12 i32)
  (local $13 i32)
  (local $14 i32)
  (local $15 i32)
  (local $16 i32)
  (if
   (i32.eqz
    (get_local $0)
   )
   (return)
  )
  (if
   (i32.lt_u
    (tee_local $2
     (i32.add
      (get_local $0)
      (i32.const -8)
     )
    )
    (tee_local $12
     (i32.load
      (i32.const 3936)
     )
    )
   )
   (call $_abort)
  )
  (if
   (i32.eq
    (tee_local $11
     (i32.and
      (tee_local $0
       (i32.load
        (i32.add
         (get_local $0)
         (i32.const -4)
        )
       )
      )
      (i32.const 3)
     )
    )
    (i32.const 1)
   )
   (call $_abort)
  )
  (set_local $7
   (i32.add
    (get_local $2)
    (tee_local $5
     (i32.and
      (get_local $0)
      (i32.const -8)
     )
    )
   )
  )
  (block $label$break$L10
   (if
    (i32.and
     (get_local $0)
     (i32.const 1)
    )
    (block
     (set_local $3
      (get_local $2)
     )
     (set_local $1
      (get_local $5)
     )
     (set_local $4
      (get_local $2)
     )
    )
    (block
     (set_local $9
      (i32.load
       (get_local $2)
      )
     )
     (if
      (i32.eqz
       (get_local $11)
      )
      (return)
     )
     (if
      (i32.lt_u
       (tee_local $0
        (i32.add
         (get_local $2)
         (i32.sub
          (i32.const 0)
          (get_local $9)
         )
        )
       )
       (get_local $12)
      )
      (call $_abort)
     )
     (set_local $2
      (i32.add
       (get_local $9)
       (get_local $5)
      )
     )
     (if
      (i32.eq
       (get_local $0)
       (i32.load
        (i32.const 3940)
       )
      )
      (block
       (if
        (i32.ne
         (i32.and
          (tee_local $3
           (i32.load
            (tee_local $1
             (i32.add
              (get_local $7)
              (i32.const 4)
             )
            )
           )
          )
          (i32.const 3)
         )
         (i32.const 3)
        )
        (block
         (set_local $3
          (get_local $0)
         )
         (set_local $1
          (get_local $2)
         )
         (set_local $4
          (get_local $0)
         )
         (br $label$break$L10)
        )
       )
       (i32.store
        (i32.const 3928)
        (get_local $2)
       )
       (i32.store
        (get_local $1)
        (i32.and
         (get_local $3)
         (i32.const -2)
        )
       )
       (i32.store offset=4
        (get_local $0)
        (i32.or
         (get_local $2)
         (i32.const 1)
        )
       )
       (i32.store
        (i32.add
         (get_local $0)
         (get_local $2)
        )
        (get_local $2)
       )
       (return)
      )
     )
     (set_local $5
      (i32.shr_u
       (get_local $9)
       (i32.const 3)
      )
     )
     (if
      (i32.lt_u
       (get_local $9)
       (i32.const 256)
      )
      (block
       (set_local $3
        (i32.load offset=12
         (get_local $0)
        )
       )
       (if
        (i32.ne
         (tee_local $4
          (i32.load offset=8
           (get_local $0)
          )
         )
         (tee_local $1
          (i32.add
           (i32.shl
            (get_local $5)
            (i32.const 3)
           )
           (i32.const 3960)
          )
         )
        )
        (block
         (if
          (i32.lt_u
           (get_local $4)
           (get_local $12)
          )
          (call $_abort)
         )
         (if
          (i32.ne
           (i32.load offset=12
            (get_local $4)
           )
           (get_local $0)
          )
          (call $_abort)
         )
        )
       )
       (if
        (i32.eq
         (get_local $3)
         (get_local $4)
        )
        (block
         (i32.store
          (i32.const 3920)
          (i32.and
           (i32.load
            (i32.const 3920)
           )
           (i32.xor
            (i32.shl
             (i32.const 1)
             (get_local $5)
            )
            (i32.const -1)
           )
          )
         )
         (set_local $3
          (get_local $0)
         )
         (set_local $1
          (get_local $2)
         )
         (set_local $4
          (get_local $0)
         )
         (br $label$break$L10)
        )
       )
       (if
        (i32.eq
         (get_local $3)
         (get_local $1)
        )
        (set_local $6
         (i32.add
          (get_local $3)
          (i32.const 8)
         )
        )
        (block
         (if
          (i32.lt_u
           (get_local $3)
           (get_local $12)
          )
          (call $_abort)
         )
         (if
          (i32.eq
           (i32.load
            (tee_local $1
             (i32.add
              (get_local $3)
              (i32.const 8)
             )
            )
           )
           (get_local $0)
          )
          (set_local $6
           (get_local $1)
          )
          (call $_abort)
         )
        )
       )
       (i32.store offset=12
        (get_local $4)
        (get_local $3)
       )
       (i32.store
        (get_local $6)
        (get_local $4)
       )
       (set_local $3
        (get_local $0)
       )
       (set_local $1
        (get_local $2)
       )
       (set_local $4
        (get_local $0)
       )
       (br $label$break$L10)
      )
     )
     (set_local $13
      (i32.load offset=24
       (get_local $0)
      )
     )
     (block $do-once
      (if
       (i32.eq
        (tee_local $5
         (i32.load offset=12
          (get_local $0)
         )
        )
        (get_local $0)
       )
       (block
        (if
         (tee_local $5
          (i32.load
           (tee_local $9
            (i32.add
             (tee_local $6
              (i32.add
               (get_local $0)
               (i32.const 16)
              )
             )
             (i32.const 4)
            )
           )
          )
         )
         (set_local $6
          (get_local $9)
         )
         (if
          (i32.eqz
           (tee_local $5
            (i32.load
             (get_local $6)
            )
           )
          )
          (block
           (set_local $8
            (i32.const 0)
           )
           (br $do-once)
          )
         )
        )
        (loop $while-in
         (if
          (tee_local $11
           (i32.load
            (tee_local $9
             (i32.add
              (get_local $5)
              (i32.const 20)
             )
            )
           )
          )
          (block
           (set_local $5
            (get_local $11)
           )
           (set_local $6
            (get_local $9)
           )
           (br $while-in)
          )
         )
         (if
          (tee_local $11
           (i32.load
            (tee_local $9
             (i32.add
              (get_local $5)
              (i32.const 16)
             )
            )
           )
          )
          (block
           (set_local $5
            (get_local $11)
           )
           (set_local $6
            (get_local $9)
           )
           (br $while-in)
          )
         )
        )
        (if
         (i32.lt_u
          (get_local $6)
          (get_local $12)
         )
         (call $_abort)
         (block
          (i32.store
           (get_local $6)
           (i32.const 0)
          )
          (set_local $8
           (get_local $5)
          )
         )
        )
       )
       (block
        (if
         (i32.lt_u
          (tee_local $6
           (i32.load offset=8
            (get_local $0)
           )
          )
          (get_local $12)
         )
         (call $_abort)
        )
        (if
         (i32.ne
          (i32.load
           (tee_local $9
            (i32.add
             (get_local $6)
             (i32.const 12)
            )
           )
          )
          (get_local $0)
         )
         (call $_abort)
        )
        (if
         (i32.eq
          (i32.load
           (tee_local $11
            (i32.add
             (get_local $5)
             (i32.const 8)
            )
           )
          )
          (get_local $0)
         )
         (block
          (i32.store
           (get_local $9)
           (get_local $5)
          )
          (i32.store
           (get_local $11)
           (get_local $6)
          )
          (set_local $8
           (get_local $5)
          )
         )
         (call $_abort)
        )
       )
      )
     )
     (if
      (get_local $13)
      (block
       (if
        (i32.eq
         (get_local $0)
         (i32.load
          (tee_local $6
           (i32.add
            (i32.shl
             (tee_local $5
              (i32.load offset=28
               (get_local $0)
              )
             )
             (i32.const 2)
            )
            (i32.const 4224)
           )
          )
         )
        )
        (block
         (i32.store
          (get_local $6)
          (get_local $8)
         )
         (if
          (i32.eqz
           (get_local $8)
          )
          (block
           (i32.store
            (i32.const 3924)
            (i32.and
             (i32.load
              (i32.const 3924)
             )
             (i32.xor
              (i32.shl
               (i32.const 1)
               (get_local $5)
              )
              (i32.const -1)
             )
            )
           )
           (set_local $3
            (get_local $0)
           )
           (set_local $1
            (get_local $2)
           )
           (set_local $4
            (get_local $0)
           )
           (br $label$break$L10)
          )
         )
        )
        (if
         (i32.lt_u
          (get_local $13)
          (i32.load
           (i32.const 3936)
          )
         )
         (call $_abort)
         (block
          (i32.store
           (i32.add
            (i32.add
             (get_local $13)
             (i32.const 16)
            )
            (i32.shl
             (i32.ne
              (i32.load offset=16
               (get_local $13)
              )
              (get_local $0)
             )
             (i32.const 2)
            )
           )
           (get_local $8)
          )
          (if
           (i32.eqz
            (get_local $8)
           )
           (block
            (set_local $3
             (get_local $0)
            )
            (set_local $1
             (get_local $2)
            )
            (set_local $4
             (get_local $0)
            )
            (br $label$break$L10)
           )
          )
         )
        )
       )
       (if
        (i32.lt_u
         (get_local $8)
         (tee_local $6
          (i32.load
           (i32.const 3936)
          )
         )
        )
        (call $_abort)
       )
       (i32.store offset=24
        (get_local $8)
        (get_local $13)
       )
       (if
        (tee_local $5
         (i32.load
          (tee_local $9
           (i32.add
            (get_local $0)
            (i32.const 16)
           )
          )
         )
        )
        (if
         (i32.lt_u
          (get_local $5)
          (get_local $6)
         )
         (call $_abort)
         (block
          (i32.store offset=16
           (get_local $8)
           (get_local $5)
          )
          (i32.store offset=24
           (get_local $5)
           (get_local $8)
          )
         )
        )
       )
       (if
        (tee_local $5
         (i32.load offset=4
          (get_local $9)
         )
        )
        (if
         (i32.lt_u
          (get_local $5)
          (i32.load
           (i32.const 3936)
          )
         )
         (call $_abort)
         (block
          (i32.store offset=20
           (get_local $8)
           (get_local $5)
          )
          (i32.store offset=24
           (get_local $5)
           (get_local $8)
          )
          (set_local $3
           (get_local $0)
          )
          (set_local $1
           (get_local $2)
          )
          (set_local $4
           (get_local $0)
          )
         )
        )
        (block
         (set_local $3
          (get_local $0)
         )
         (set_local $1
          (get_local $2)
         )
         (set_local $4
          (get_local $0)
         )
        )
       )
      )
      (block
       (set_local $3
        (get_local $0)
       )
       (set_local $1
        (get_local $2)
       )
       (set_local $4
        (get_local $0)
       )
      )
     )
    )
   )
  )
  (if
   (i32.ge_u
    (get_local $4)
    (get_local $7)
   )
   (call $_abort)
  )
  (if
   (i32.eqz
    (i32.and
     (tee_local $0
      (i32.load
       (tee_local $2
        (i32.add
         (get_local $7)
         (i32.const 4)
        )
       )
      )
     )
     (i32.const 1)
    )
   )
   (call $_abort)
  )
  (if
   (i32.and
    (get_local $0)
    (i32.const 2)
   )
   (block
    (i32.store
     (get_local $2)
     (i32.and
      (get_local $0)
      (i32.const -2)
     )
    )
    (i32.store offset=4
     (get_local $3)
     (i32.or
      (get_local $1)
      (i32.const 1)
     )
    )
    (i32.store
     (i32.add
      (get_local $4)
      (get_local $1)
     )
     (get_local $1)
    )
   )
   (block
    (set_local $2
     (i32.load
      (i32.const 3940)
     )
    )
    (if
     (i32.eq
      (get_local $7)
      (i32.load
       (i32.const 3944)
      )
     )
     (block
      (i32.store
       (i32.const 3932)
       (tee_local $0
        (i32.add
         (i32.load
          (i32.const 3932)
         )
         (get_local $1)
        )
       )
      )
      (i32.store
       (i32.const 3944)
       (get_local $3)
      )
      (i32.store offset=4
       (get_local $3)
       (i32.or
        (get_local $0)
        (i32.const 1)
       )
      )
      (if
       (i32.ne
        (get_local $3)
        (get_local $2)
       )
       (return)
      )
      (i32.store
       (i32.const 3940)
       (i32.const 0)
      )
      (i32.store
       (i32.const 3928)
       (i32.const 0)
      )
      (return)
     )
    )
    (if
     (i32.eq
      (get_local $7)
      (get_local $2)
     )
     (block
      (i32.store
       (i32.const 3928)
       (tee_local $0
        (i32.add
         (i32.load
          (i32.const 3928)
         )
         (get_local $1)
        )
       )
      )
      (i32.store
       (i32.const 3940)
       (get_local $4)
      )
      (i32.store offset=4
       (get_local $3)
       (i32.or
        (get_local $0)
        (i32.const 1)
       )
      )
      (i32.store
       (i32.add
        (get_local $4)
        (get_local $0)
       )
       (get_local $0)
      )
      (return)
     )
    )
    (set_local $6
     (i32.add
      (i32.and
       (get_local $0)
       (i32.const -8)
      )
      (get_local $1)
     )
    )
    (set_local $5
     (i32.shr_u
      (get_local $0)
      (i32.const 3)
     )
    )
    (block $label$break$L108
     (if
      (i32.lt_u
       (get_local $0)
       (i32.const 256)
      )
      (block
       (set_local $1
        (i32.load offset=12
         (get_local $7)
        )
       )
       (if
        (i32.ne
         (tee_local $2
          (i32.load offset=8
           (get_local $7)
          )
         )
         (tee_local $0
          (i32.add
           (i32.shl
            (get_local $5)
            (i32.const 3)
           )
           (i32.const 3960)
          )
         )
        )
        (block
         (if
          (i32.lt_u
           (get_local $2)
           (i32.load
            (i32.const 3936)
           )
          )
          (call $_abort)
         )
         (if
          (i32.ne
           (i32.load offset=12
            (get_local $2)
           )
           (get_local $7)
          )
          (call $_abort)
         )
        )
       )
       (if
        (i32.eq
         (get_local $1)
         (get_local $2)
        )
        (block
         (i32.store
          (i32.const 3920)
          (i32.and
           (i32.load
            (i32.const 3920)
           )
           (i32.xor
            (i32.shl
             (i32.const 1)
             (get_local $5)
            )
            (i32.const -1)
           )
          )
         )
         (br $label$break$L108)
        )
       )
       (if
        (i32.eq
         (get_local $1)
         (get_local $0)
        )
        (set_local $15
         (i32.add
          (get_local $1)
          (i32.const 8)
         )
        )
        (block
         (if
          (i32.lt_u
           (get_local $1)
           (i32.load
            (i32.const 3936)
           )
          )
          (call $_abort)
         )
         (if
          (i32.eq
           (i32.load
            (tee_local $0
             (i32.add
              (get_local $1)
              (i32.const 8)
             )
            )
           )
           (get_local $7)
          )
          (set_local $15
           (get_local $0)
          )
          (call $_abort)
         )
        )
       )
       (i32.store offset=12
        (get_local $2)
        (get_local $1)
       )
       (i32.store
        (get_local $15)
        (get_local $2)
       )
      )
      (block
       (set_local $8
        (i32.load offset=24
         (get_local $7)
        )
       )
       (block $do-once6
        (if
         (i32.eq
          (tee_local $0
           (i32.load offset=12
            (get_local $7)
           )
          )
          (get_local $7)
         )
         (block
          (if
           (tee_local $0
            (i32.load
             (tee_local $2
              (i32.add
               (tee_local $1
                (i32.add
                 (get_local $7)
                 (i32.const 16)
                )
               )
               (i32.const 4)
              )
             )
            )
           )
           (set_local $1
            (get_local $2)
           )
           (if
            (i32.eqz
             (tee_local $0
              (i32.load
               (get_local $1)
              )
             )
            )
            (block
             (set_local $10
              (i32.const 0)
             )
             (br $do-once6)
            )
           )
          )
          (loop $while-in9
           (if
            (tee_local $5
             (i32.load
              (tee_local $2
               (i32.add
                (get_local $0)
                (i32.const 20)
               )
              )
             )
            )
            (block
             (set_local $0
              (get_local $5)
             )
             (set_local $1
              (get_local $2)
             )
             (br $while-in9)
            )
           )
           (if
            (tee_local $5
             (i32.load
              (tee_local $2
               (i32.add
                (get_local $0)
                (i32.const 16)
               )
              )
             )
            )
            (block
             (set_local $0
              (get_local $5)
             )
             (set_local $1
              (get_local $2)
             )
             (br $while-in9)
            )
           )
          )
          (if
           (i32.lt_u
            (get_local $1)
            (i32.load
             (i32.const 3936)
            )
           )
           (call $_abort)
           (block
            (i32.store
             (get_local $1)
             (i32.const 0)
            )
            (set_local $10
             (get_local $0)
            )
           )
          )
         )
         (block
          (if
           (i32.lt_u
            (tee_local $1
             (i32.load offset=8
              (get_local $7)
             )
            )
            (i32.load
             (i32.const 3936)
            )
           )
           (call $_abort)
          )
          (if
           (i32.ne
            (i32.load
             (tee_local $2
              (i32.add
               (get_local $1)
               (i32.const 12)
              )
             )
            )
            (get_local $7)
           )
           (call $_abort)
          )
          (if
           (i32.eq
            (i32.load
             (tee_local $5
              (i32.add
               (get_local $0)
               (i32.const 8)
              )
             )
            )
            (get_local $7)
           )
           (block
            (i32.store
             (get_local $2)
             (get_local $0)
            )
            (i32.store
             (get_local $5)
             (get_local $1)
            )
            (set_local $10
             (get_local $0)
            )
           )
           (call $_abort)
          )
         )
        )
       )
       (if
        (get_local $8)
        (block
         (if
          (i32.eq
           (get_local $7)
           (i32.load
            (tee_local $1
             (i32.add
              (i32.shl
               (tee_local $0
                (i32.load offset=28
                 (get_local $7)
                )
               )
               (i32.const 2)
              )
              (i32.const 4224)
             )
            )
           )
          )
          (block
           (i32.store
            (get_local $1)
            (get_local $10)
           )
           (if
            (i32.eqz
             (get_local $10)
            )
            (block
             (i32.store
              (i32.const 3924)
              (i32.and
               (i32.load
                (i32.const 3924)
               )
               (i32.xor
                (i32.shl
                 (i32.const 1)
                 (get_local $0)
                )
                (i32.const -1)
               )
              )
             )
             (br $label$break$L108)
            )
           )
          )
          (if
           (i32.lt_u
            (get_local $8)
            (i32.load
             (i32.const 3936)
            )
           )
           (call $_abort)
           (block
            (i32.store
             (i32.add
              (i32.add
               (get_local $8)
               (i32.const 16)
              )
              (i32.shl
               (i32.ne
                (i32.load offset=16
                 (get_local $8)
                )
                (get_local $7)
               )
               (i32.const 2)
              )
             )
             (get_local $10)
            )
            (br_if $label$break$L108
             (i32.eqz
              (get_local $10)
             )
            )
           )
          )
         )
         (if
          (i32.lt_u
           (get_local $10)
           (tee_local $1
            (i32.load
             (i32.const 3936)
            )
           )
          )
          (call $_abort)
         )
         (i32.store offset=24
          (get_local $10)
          (get_local $8)
         )
         (if
          (tee_local $0
           (i32.load
            (tee_local $2
             (i32.add
              (get_local $7)
              (i32.const 16)
             )
            )
           )
          )
          (if
           (i32.lt_u
            (get_local $0)
            (get_local $1)
           )
           (call $_abort)
           (block
            (i32.store offset=16
             (get_local $10)
             (get_local $0)
            )
            (i32.store offset=24
             (get_local $0)
             (get_local $10)
            )
           )
          )
         )
         (if
          (tee_local $0
           (i32.load offset=4
            (get_local $2)
           )
          )
          (if
           (i32.lt_u
            (get_local $0)
            (i32.load
             (i32.const 3936)
            )
           )
           (call $_abort)
           (block
            (i32.store offset=20
             (get_local $10)
             (get_local $0)
            )
            (i32.store offset=24
             (get_local $0)
             (get_local $10)
            )
           )
          )
         )
        )
       )
      )
     )
    )
    (i32.store offset=4
     (get_local $3)
     (i32.or
      (get_local $6)
      (i32.const 1)
     )
    )
    (i32.store
     (i32.add
      (get_local $4)
      (get_local $6)
     )
     (get_local $6)
    )
    (if
     (i32.eq
      (get_local $3)
      (i32.load
       (i32.const 3940)
      )
     )
     (block
      (i32.store
       (i32.const 3928)
       (get_local $6)
      )
      (return)
     )
     (set_local $1
      (get_local $6)
     )
    )
   )
  )
  (set_local $4
   (i32.shr_u
    (get_local $1)
    (i32.const 3)
   )
  )
  (if
   (i32.lt_u
    (get_local $1)
    (i32.const 256)
   )
   (block
    (set_local $0
     (i32.add
      (i32.shl
       (get_local $4)
       (i32.const 3)
      )
      (i32.const 3960)
     )
    )
    (if
     (i32.and
      (tee_local $1
       (i32.load
        (i32.const 3920)
       )
      )
      (tee_local $4
       (i32.shl
        (i32.const 1)
        (get_local $4)
       )
      )
     )
     (if
      (i32.lt_u
       (tee_local $4
        (i32.load
         (tee_local $1
          (i32.add
           (get_local $0)
           (i32.const 8)
          )
         )
        )
       )
       (i32.load
        (i32.const 3936)
       )
      )
      (call $_abort)
      (block
       (set_local $14
        (get_local $4)
       )
       (set_local $16
        (get_local $1)
       )
      )
     )
     (block
      (i32.store
       (i32.const 3920)
       (i32.or
        (get_local $1)
        (get_local $4)
       )
      )
      (set_local $14
       (get_local $0)
      )
      (set_local $16
       (i32.add
        (get_local $0)
        (i32.const 8)
       )
      )
     )
    )
    (i32.store
     (get_local $16)
     (get_local $3)
    )
    (i32.store offset=12
     (get_local $14)
     (get_local $3)
    )
    (i32.store offset=8
     (get_local $3)
     (get_local $14)
    )
    (i32.store offset=12
     (get_local $3)
     (get_local $0)
    )
    (return)
   )
  )
  (set_local $0
   (i32.add
    (i32.shl
     (tee_local $4
      (if (result i32)
       (tee_local $0
        (i32.shr_u
         (get_local $1)
         (i32.const 8)
        )
       )
       (if (result i32)
        (i32.gt_u
         (get_local $1)
         (i32.const 16777215)
        )
        (i32.const 31)
        (i32.or
         (i32.and
          (i32.shr_u
           (get_local $1)
           (i32.add
            (tee_local $0
             (i32.add
              (i32.sub
               (i32.const 14)
               (i32.or
                (i32.or
                 (tee_local $2
                  (i32.and
                   (i32.shr_u
                    (i32.add
                     (tee_local $4
                      (i32.shl
                       (get_local $0)
                       (tee_local $0
                        (i32.and
                         (i32.shr_u
                          (i32.add
                           (get_local $0)
                           (i32.const 1048320)
                          )
                          (i32.const 16)
                         )
                         (i32.const 8)
                        )
                       )
                      )
                     )
                     (i32.const 520192)
                    )
                    (i32.const 16)
                   )
                   (i32.const 4)
                  )
                 )
                 (get_local $0)
                )
                (tee_local $4
                 (i32.and
                  (i32.shr_u
                   (i32.add
                    (tee_local $0
                     (i32.shl
                      (get_local $4)
                      (get_local $2)
                     )
                    )
                    (i32.const 245760)
                   )
                   (i32.const 16)
                  )
                  (i32.const 2)
                 )
                )
               )
              )
              (i32.shr_u
               (i32.shl
                (get_local $0)
                (get_local $4)
               )
               (i32.const 15)
              )
             )
            )
            (i32.const 7)
           )
          )
          (i32.const 1)
         )
         (i32.shl
          (get_local $0)
          (i32.const 1)
         )
        )
       )
       (i32.const 0)
      )
     )
     (i32.const 2)
    )
    (i32.const 4224)
   )
  )
  (i32.store offset=28
   (get_local $3)
   (get_local $4)
  )
  (i32.store offset=20
   (get_local $3)
   (i32.const 0)
  )
  (i32.store offset=16
   (get_local $3)
   (i32.const 0)
  )
  (block $do-once14
   (if
    (i32.and
     (tee_local $2
      (i32.load
       (i32.const 3924)
      )
     )
     (tee_local $5
      (i32.shl
       (i32.const 1)
       (get_local $4)
      )
     )
    )
    (block
     (set_local $0
      (i32.load
       (get_local $0)
      )
     )
     (set_local $2
      (i32.sub
       (i32.const 25)
       (i32.shr_u
        (get_local $4)
        (i32.const 1)
       )
      )
     )
     (set_local $4
      (i32.shl
       (get_local $1)
       (if (result i32)
        (i32.eq
         (get_local $4)
         (i32.const 31)
        )
        (i32.const 0)
        (get_local $2)
       )
      )
     )
     (block $__rjto$1
      (block $__rjti$1
       (loop $while-in17
        (block $while-out16
         (br_if $__rjti$1
          (i32.eq
           (i32.and
            (i32.load offset=4
             (get_local $0)
            )
            (i32.const -8)
           )
           (get_local $1)
          )
         )
         (set_local $2
          (i32.shl
           (get_local $4)
           (i32.const 1)
          )
         )
         (if
          (tee_local $5
           (i32.load
            (tee_local $4
             (i32.add
              (i32.add
               (get_local $0)
               (i32.const 16)
              )
              (i32.shl
               (i32.shr_u
                (get_local $4)
                (i32.const 31)
               )
               (i32.const 2)
              )
             )
            )
           )
          )
          (block
           (set_local $4
            (get_local $2)
           )
           (set_local $0
            (get_local $5)
           )
           (br $while-in17)
          )
         )
        )
       )
       (if
        (i32.lt_u
         (get_local $4)
         (i32.load
          (i32.const 3936)
         )
        )
        (call $_abort)
        (block
         (i32.store
          (get_local $4)
          (get_local $3)
         )
         (i32.store offset=24
          (get_local $3)
          (get_local $0)
         )
         (i32.store offset=12
          (get_local $3)
          (get_local $3)
         )
         (i32.store offset=8
          (get_local $3)
          (get_local $3)
         )
         (br $do-once14)
        )
       )
       (br $__rjto$1)
      )
      (if
       (i32.and
        (i32.ge_u
         (tee_local $1
          (i32.load
           (tee_local $4
            (i32.add
             (get_local $0)
             (i32.const 8)
            )
           )
          )
         )
         (tee_local $2
          (i32.load
           (i32.const 3936)
          )
         )
        )
        (i32.ge_u
         (get_local $0)
         (get_local $2)
        )
       )
       (block
        (i32.store offset=12
         (get_local $1)
         (get_local $3)
        )
        (i32.store
         (get_local $4)
         (get_local $3)
        )
        (i32.store offset=8
         (get_local $3)
         (get_local $1)
        )
        (i32.store offset=12
         (get_local $3)
         (get_local $0)
        )
        (i32.store offset=24
         (get_local $3)
         (i32.const 0)
        )
       )
       (call $_abort)
      )
     )
    )
    (block
     (i32.store
      (i32.const 3924)
      (i32.or
       (get_local $2)
       (get_local $5)
      )
     )
     (i32.store
      (get_local $0)
      (get_local $3)
     )
     (i32.store offset=24
      (get_local $3)
      (get_local $0)
     )
     (i32.store offset=12
      (get_local $3)
      (get_local $3)
     )
     (i32.store offset=8
      (get_local $3)
      (get_local $3)
     )
    )
   )
  )
  (i32.store
   (i32.const 3952)
   (tee_local $0
    (i32.add
     (i32.load
      (i32.const 3952)
     )
     (i32.const -1)
    )
   )
  )
  (if
   (get_local $0)
   (return)
   (set_local $0
    (i32.const 4376)
   )
  )
  (loop $while-in19
   (set_local $0
    (i32.add
     (tee_local $1
      (i32.load
       (get_local $0)
      )
     )
     (i32.const 8)
    )
   )
   (br_if $while-in19
    (get_local $1)
   )
  )
  (i32.store
   (i32.const 3952)
   (i32.const -1)
  )
 )
 (func $runPostSets
  (nop)
 )
 (func $_sbrk (param $0 i32) (result i32)
  (local $1 i32)
  (local $2 i32)
  (set_local $1
   (i32.add
    (tee_local $2
     (i32.load
      (get_global $DYNAMICTOP_PTR)
     )
    )
    (tee_local $0
     (i32.and
      (i32.add
       (get_local $0)
       (i32.const 15)
      )
      (i32.const -16)
     )
    )
   )
  )
  (if
   (i32.or
    (i32.and
     (i32.gt_s
      (get_local $0)
      (i32.const 0)
     )
     (i32.lt_s
      (get_local $1)
      (get_local $2)
     )
    )
    (i32.lt_s
     (get_local $1)
     (i32.const 0)
    )
   )
   (block
    (drop
     (call $abortOnCannotGrowMemory)
    )
    (call $___setErrNo
     (i32.const 12)
    )
    (return
     (i32.const -1)
    )
   )
  )
  (i32.store
   (get_global $DYNAMICTOP_PTR)
   (get_local $1)
  )
  (set_local $0
   (call $getTotalMemory)
  )
  (if
   (i32.gt_s
    (get_local $1)
    (get_local $0)
   )
   (if
    (i32.eqz
     (call $enlargeMemory)
    )
    (block
     (i32.store
      (get_global $DYNAMICTOP_PTR)
      (get_local $2)
     )
     (call $___setErrNo
      (i32.const 12)
     )
     (return
      (i32.const -1)
     )
    )
   )
  )
  (get_local $2)
 )
 (func $_memset (param $0 i32) (param $1 i32) (param $2 i32) (result i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (local $6 i32)
  (set_local $4
   (i32.add
    (get_local $0)
    (get_local $2)
   )
  )
  (set_local $1
   (i32.and
    (get_local $1)
    (i32.const 255)
   )
  )
  (if
   (i32.ge_s
    (get_local $2)
    (i32.const 67)
   )
   (block
    (loop $while-in
     (if
      (i32.and
       (get_local $0)
       (i32.const 3)
      )
      (block
       (i32.store8
        (get_local $0)
        (get_local $1)
       )
       (set_local $0
        (i32.add
         (get_local $0)
         (i32.const 1)
        )
       )
       (br $while-in)
      )
     )
    )
    (set_local $6
     (i32.sub
      (tee_local $5
       (i32.and
        (get_local $4)
        (i32.const -4)
       )
      )
      (i32.const 64)
     )
    )
    (set_local $3
     (i32.or
      (i32.or
       (i32.or
        (get_local $1)
        (i32.shl
         (get_local $1)
         (i32.const 8)
        )
       )
       (i32.shl
        (get_local $1)
        (i32.const 16)
       )
      )
      (i32.shl
       (get_local $1)
       (i32.const 24)
      )
     )
    )
    (loop $while-in1
     (if
      (i32.le_s
       (get_local $0)
       (get_local $6)
      )
      (block
       (i32.store
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=4
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=8
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=12
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=16
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=20
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=24
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=28
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=32
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=36
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=40
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=44
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=48
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=52
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=56
        (get_local $0)
        (get_local $3)
       )
       (i32.store offset=60
        (get_local $0)
        (get_local $3)
       )
       (set_local $0
        (i32.add
         (get_local $0)
         (i32.const 64)
        )
       )
       (br $while-in1)
      )
     )
    )
    (loop $while-in3
     (if
      (i32.lt_s
       (get_local $0)
       (get_local $5)
      )
      (block
       (i32.store
        (get_local $0)
        (get_local $3)
       )
       (set_local $0
        (i32.add
         (get_local $0)
         (i32.const 4)
        )
       )
       (br $while-in3)
      )
     )
    )
   )
  )
  (loop $while-in5
   (if
    (i32.lt_s
     (get_local $0)
     (get_local $4)
    )
    (block
     (i32.store8
      (get_local $0)
      (get_local $1)
     )
     (set_local $0
      (i32.add
       (get_local $0)
       (i32.const 1)
      )
     )
     (br $while-in5)
    )
   )
  )
  (i32.sub
   (get_local $4)
   (get_local $2)
  )
 )
 (func $_memcpy (param $0 i32) (param $1 i32) (param $2 i32) (result i32)
  (local $3 i32)
  (local $4 i32)
  (local $5 i32)
  (if
   (i32.ge_s
    (get_local $2)
    (i32.const 8192)
   )
   (return
    (call $_emscripten_memcpy_big
     (get_local $0)
     (get_local $1)
     (get_local $2)
    )
   )
  )
  (set_local $4
   (get_local $0)
  )
  (set_local $3
   (i32.add
    (get_local $0)
    (get_local $2)
   )
  )
  (if
   (i32.eq
    (i32.and
     (get_local $0)
     (i32.const 3)
    )
    (i32.and
     (get_local $1)
     (i32.const 3)
    )
   )
   (block
    (loop $while-in
     (if
      (i32.and
       (get_local $0)
       (i32.const 3)
      )
      (block
       (if
        (i32.eqz
         (get_local $2)
        )
        (return
         (get_local $4)
        )
       )
       (i32.store8
        (get_local $0)
        (i32.load8_s
         (get_local $1)
        )
       )
       (set_local $0
        (i32.add
         (get_local $0)
         (i32.const 1)
        )
       )
       (set_local $1
        (i32.add
         (get_local $1)
         (i32.const 1)
        )
       )
       (set_local $2
        (i32.sub
         (get_local $2)
         (i32.const 1)
        )
       )
       (br $while-in)
      )
     )
    )
    (set_local $5
     (i32.sub
      (tee_local $2
       (i32.and
        (get_local $3)
        (i32.const -4)
       )
      )
      (i32.const 64)
     )
    )
    (loop $while-in1
     (if
      (i32.le_s
       (get_local $0)
       (get_local $5)
      )
      (block
       (i32.store
        (get_local $0)
        (i32.load
         (get_local $1)
        )
       )
       (i32.store offset=4
        (get_local $0)
        (i32.load offset=4
         (get_local $1)
        )
       )
       (i32.store offset=8
        (get_local $0)
        (i32.load offset=8
         (get_local $1)
        )
       )
       (i32.store offset=12
        (get_local $0)
        (i32.load offset=12
         (get_local $1)
        )
       )
       (i32.store offset=16
        (get_local $0)
        (i32.load offset=16
         (get_local $1)
        )
       )
       (i32.store offset=20
        (get_local $0)
        (i32.load offset=20
         (get_local $1)
        )
       )
       (i32.store offset=24
        (get_local $0)
        (i32.load offset=24
         (get_local $1)
        )
       )
       (i32.store offset=28
        (get_local $0)
        (i32.load offset=28
         (get_local $1)
        )
       )
       (i32.store offset=32
        (get_local $0)
        (i32.load offset=32
         (get_local $1)
        )
       )
       (i32.store offset=36
        (get_local $0)
        (i32.load offset=36
         (get_local $1)
        )
       )
       (i32.store offset=40
        (get_local $0)
        (i32.load offset=40
         (get_local $1)
        )
       )
       (i32.store offset=44
        (get_local $0)
        (i32.load offset=44
         (get_local $1)
        )
       )
       (i32.store offset=48
        (get_local $0)
        (i32.load offset=48
         (get_local $1)
        )
       )
       (i32.store offset=52
        (get_local $0)
        (i32.load offset=52
         (get_local $1)
        )
       )
       (i32.store offset=56
        (get_local $0)
        (i32.load offset=56
         (get_local $1)
        )
       )
       (i32.store offset=60
        (get_local $0)
        (i32.load offset=60
         (get_local $1)
        )
       )
       (set_local $0
        (i32.add
         (get_local $0)
         (i32.const 64)
        )
       )
       (set_local $1
        (i32.add
         (get_local $1)
         (i32.const 64)
        )
       )
       (br $while-in1)
      )
     )
    )
    (loop $while-in3
     (if
      (i32.lt_s
       (get_local $0)
       (get_local $2)
      )
      (block
       (i32.store
        (get_local $0)
        (i32.load
         (get_local $1)
        )
       )
       (set_local $0
        (i32.add
         (get_local $0)
         (i32.const 4)
        )
       )
       (set_local $1
        (i32.add
         (get_local $1)
         (i32.const 4)
        )
       )
       (br $while-in3)
      )
     )
    )
   )
   (block
    (set_local $2
     (i32.sub
      (get_local $3)
      (i32.const 4)
     )
    )
    (loop $while-in5
     (if
      (i32.lt_s
       (get_local $0)
       (get_local $2)
      )
      (block
       (i32.store8
        (get_local $0)
        (i32.load8_s
         (get_local $1)
        )
       )
       (i32.store8 offset=1
        (get_local $0)
        (i32.load8_s offset=1
         (get_local $1)
        )
       )
       (i32.store8 offset=2
        (get_local $0)
        (i32.load8_s offset=2
         (get_local $1)
        )
       )
       (i32.store8 offset=3
        (get_local $0)
        (i32.load8_s offset=3
         (get_local $1)
        )
       )
       (set_local $0
        (i32.add
         (get_local $0)
         (i32.const 4)
        )
       )
       (set_local $1
        (i32.add
         (get_local $1)
         (i32.const 4)
        )
       )
       (br $while-in5)
      )
     )
    )
   )
  )
  (loop $while-in7
   (if
    (i32.lt_s
     (get_local $0)
     (get_local $3)
    )
    (block
     (i32.store8
      (get_local $0)
      (i32.load8_s
       (get_local $1)
      )
     )
     (set_local $0
      (i32.add
       (get_local $0)
       (i32.const 1)
      )
     )
     (set_local $1
      (i32.add
       (get_local $1)
       (i32.const 1)
      )
     )
     (br $while-in7)
    )
   )
  )
  (get_local $4)
 )
 (func $_llvm_bswap_i32 (param $0 i32) (result i32)
  (i32.or
   (i32.or
    (i32.or
     (i32.shl
      (i32.and
       (get_local $0)
       (i32.const 255)
      )
      (i32.const 24)
     )
     (i32.shl
      (i32.and
       (i32.shr_s
        (get_local $0)
        (i32.const 8)
       )
       (i32.const 255)
      )
      (i32.const 16)
     )
    )
    (i32.shl
     (i32.and
      (i32.shr_s
       (get_local $0)
       (i32.const 16)
      )
      (i32.const 255)
     )
     (i32.const 8)
    )
   )
   (i32.shr_u
    (get_local $0)
    (i32.const 24)
   )
  )
 )
 (func $dynCall_ii (param $0 i32) (param $1 i32) (result i32)
  (call_indirect $FUNCSIG$ii
   (get_local $1)
   (i32.and
    (get_local $0)
    (i32.const 1)
   )
  )
 )
 (func $dynCall_iiii (param $0 i32) (param $1 i32) (param $2 i32) (param $3 i32) (result i32)
  (call_indirect $FUNCSIG$iiii
   (get_local $1)
   (get_local $2)
   (get_local $3)
   (i32.add
    (i32.and
     (get_local $0)
     (i32.const 3)
    )
    (i32.const 2)
   )
  )
 )
 (func $b0 (param $0 i32) (result i32)
  (call $abort
   (i32.const 0)
  )
  (i32.const 0)
 )
 (func $b1 (param $0 i32) (param $1 i32) (param $2 i32) (result i32)
  (call $abort
   (i32.const 1)
  )
  (i32.const 0)
 )
)
