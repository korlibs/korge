// asc sample1.ts --outFile sample1.wasm && wasm2wat sample1.wasm > /tmp/sample1.wat && cat /tmp/sample1.wat

@external("log", "integer")
declare function logInteger(i: i32): void
@external("log", "boolean")
declare function logBoolean(i: boolean): void

export function max(a: i32, b: i32): i32 {
    return (a > b) ? a : b;
}

export function min(a: i32, b: i32): i32 {
    return (a < b) ? a : b;
}

@unmanaged class BLOCK {
  mmInfo: usize;
}
// @ts-ignore: decorator
@inline const AL_BITS: u32 = 4; // 16 bytes to fit up to v128
// @ts-ignore: decorator
@inline const AL_SIZE: usize = 1 << <usize>AL_BITS;
// @ts-ignore: decorator
@inline const AL_MASK: usize = AL_SIZE - 1;
// @ts-ignore: decorator
@inline const BLOCK_OVERHEAD: usize = offsetof<BLOCK>();
// @ts-ignore: decorator
@inline const BLOCK_MAXSIZE: usize = (1 << 30) - BLOCK_OVERHEAD;

// @ts-ignore: decorator
@inline function computeSize(size: i32): i32 {
  return ((size + BLOCK_OVERHEAD + AL_MASK) & ~AL_MASK) - BLOCK_OVERHEAD;
}

export function sample1(): void {
    logInteger(max(-1, 2))
    logInteger(max(2, -1))
    logInteger(min(-1, 2))
    logInteger(min(2, -1))
    logBoolean((-1 <= 2))
    logBoolean((2 <= -1))
    logBoolean((3 <= 3))
    logBoolean((-1 >= 2))
    logBoolean((2 >= -1))
    logBoolean((3 >= 3))
    logBoolean((-1 == 2))
    logBoolean((2 == -1))
    logBoolean((3 == 3))
    logBoolean((-1 != 2))
    logBoolean((2 != -1))
    logBoolean((3 != 3))
    logInteger(-777)
    logInteger(AL_BITS)
    logInteger(AL_SIZE)
    logInteger(AL_MASK)
    logInteger(BLOCK_OVERHEAD)
    logInteger(BLOCK_MAXSIZE)
    logInteger(computeSize(16))
    logInteger(computeSize(1000))
}
