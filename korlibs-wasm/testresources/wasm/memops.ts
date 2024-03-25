// asc memops.ts --outFile memops.wasm --optimize && wasm2wat memops.wasm> /tmp/memops.wat && cat /tmp/memops.wat

@external("log", "integer") declare function logInteger(i: i32): void
@external("log", "long") declare function logLong(i: i64): void
@external("log", "float") declare function logFloat(i: f32): void
@external("log", "double") declare function logDouble(i: f64): void
@external("log", "string") declare function logString(i: string): void

// @ts-ignore: decorator
@inline const AL_BITS: u32 = 4; // 16 bytes to fit up to v128
// @ts-ignore: decorator
@inline const AL_SIZE: usize = 1 << <usize>AL_BITS;
// @ts-ignore: decorator
@inline const AL_MASK: usize = AL_SIZE - 1;
// @ts-ignore: decorator
@inline const BLOCK_OVERHEAD: usize = 4;
// @ts-ignore: decorator
@inline const BLOCK_MAXSIZE: usize = (1 << 30) - BLOCK_OVERHEAD;

var demo1: boolean = false
var demo2: i8 = 1
var demo3: i16 = 2
var demo4: i32 = 3
var demo5: i64 = 4
var demo6: f32 = 5.0
var demo7: f64 = 6.0

var demo8: u8 = 7
var demo9: i16 = 8
var demo10: u32 = 9
var demo11: u64 = 10

function addMemory(root: i32, start: usize, endU64: u64): i64 {
  let end = <usize>endU64;
  if (<u64>start > endU64) {
    return -1
  }
  start = ((start + BLOCK_OVERHEAD + AL_MASK) & ~AL_MASK) - BLOCK_OVERHEAD;
  end &= ~AL_MASK;
  return start
}

export function main(): void {
    logInteger(demo1)
    logInteger(demo2)
    logInteger(demo3)
    logInteger(demo4)
    logLong(demo5)
    logFloat(demo6)
    logDouble(demo7)
    logInteger(demo8)
    logInteger(demo9)
    logInteger(demo10)
    logLong(demo11)
    demo1 = true
    demo2++;
    demo3++;
    demo4++;
    demo5++;
    demo6++;
    demo7++;
    demo8++;
    demo9++;
    demo10++;
    demo11++;
    logInteger(demo1)
    logInteger(demo2)
    logInteger(demo3)
    logInteger(demo4)
    logLong(demo5)
    logFloat(demo6)
    logDouble(demo7)
    logInteger(demo8)
    logInteger(demo9)
    logInteger(demo10)
    logLong(demo11)
    logInteger(load<i8>(10))
    store<i8>(10, -11)
    logInteger(load<i8>(10))
    logInteger(load<u8>(10))
    logString("hello")
    logString("worlds")
    logLong(addMemory(36080, 37652, 4294967296))
}
