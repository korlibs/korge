// asc sample2.ts --outFile sample2.wasm && wasm2wat sample2.wasm > /tmp/sample2.wat && cat /tmp/sample2.wat

@external("log", "integer") declare function logInteger(i: i32): void

function demo(a: i32, b: i32): i32 {
    const c = 3 + ((a < b + 10) ? ((b > 1 + 2) ? (1) : 2) : (7))
    return c + 4
}

@unmanaged class Root {
  /** First level bitmap. */
  flMap: usize;
}

@unmanaged class BLOCK {
  /** Memory manager info. */
  mmInfo: usize;
}
// F: FREE, L: LEFTFREE
@unmanaged class Block extends BLOCK {

  /** Previous free block, if any. Only valid if free, otherwise part of payload. */
  prev: Block | null;
  /** Next free block, if any. Only valid if free, otherwise part of payload. */
  next: Block | null;

  // If the block is free, there is a 'back'reference at its end pointing at its start.
}

/** Overhead of a memory manager block. */
// @ts-ignore: decorator
@inline const BLOCK_OVERHEAD: usize = offsetof<BLOCK>();

/** Maximum size of a memory manager block's payload. */
// @ts-ignore: decorator
@inline const BLOCK_MAXSIZE: usize = (1 << 30) - BLOCK_OVERHEAD;

// @ts-ignore: decorator
@inline const AL_BITS: u32 = 4; // 16 bytes to fit up to v128
// @ts-ignore: decorator
@inline const AL_SIZE: usize = 1 << <usize>AL_BITS;
// @ts-ignore: decorator
@inline const AL_MASK: usize = AL_SIZE - 1;


// @ts-ignore: decorator
@inline const SL_BITS: u32 = 4;
// @ts-ignore: decorator
@inline const SL_SIZE: u32 = 1 << SL_BITS; // 16

// @ts-ignore: decorator
@inline const SB_BITS: u32 = SL_BITS + AL_BITS; // 8
// @ts-ignore: decorator
@inline const SB_SIZE: u32 = 1 << SB_BITS; // 256

// @ts-ignore: decorator
@inline const FL_BITS: u32 = 31 - SB_BITS; // 23
// @ts-ignore: decorator
@inline const SL_START: usize = sizeof<usize>();
// @ts-ignore: decorator
@inline const SL_END: usize = SL_START + (FL_BITS << alignof<u32>());
// @ts-ignore: decorator
@inline const HL_START: usize = (SL_END + AL_MASK) & ~AL_MASK;
// @ts-ignore: decorator
@inline const HL_END: usize = HL_START + FL_BITS * SL_SIZE * sizeof<usize>();
// @ts-ignore: decorator
@inline const ROOT_SIZE: usize = HL_END + sizeof<usize>();

@inline function GETTAIL(root: Root): Block {
  return load<Block>(
    changetype<usize>(root),
    HL_END
  );
}

function roundSize(size: usize): usize {
  const halfMaxSize = BLOCK_MAXSIZE >> 1; // don't round last fl
  const inv: usize = sizeof<usize>() * 8 - 1;
  const invRound = inv - SL_BITS;
  return size < halfMaxSize
    ? size + (1 << (invRound - clz<usize>(size))) - 1
    : size;
}

function addMemory(root: Root, start: usize, endU64: u64): bool {
    return false;
}

/** Grows memory to fit at least another block of the specified size. */
export function growMemory(root: Root, size: usize): void {
  if (ASC_LOW_MEMORY_LIMIT) {
    unreachable();
    return;
  }
  // Here, both rounding performed in searchBlock ...
  if (size >= SB_SIZE) {
    size = roundSize(size);
  }
  // and additional BLOCK_OVERHEAD must be taken into account. If we are going
  // to merge with the tail block, that's one time, otherwise it's two times.
  let pagesBefore = memory.size();
  size += BLOCK_OVERHEAD << usize((<usize>pagesBefore << 16) - BLOCK_OVERHEAD != changetype<usize>(GETTAIL(root)));
  let pagesNeeded = <i32>(((size + 0xffff) & ~0xffff) >>> 16);
  let pagesWanted = max(pagesBefore, pagesNeeded); // double memory
  if (memory.grow(pagesWanted) < 0) {
    if (memory.grow(pagesNeeded) < 0) unreachable();
  }
  let pagesAfter = memory.size();
  addMemory(root, <usize>pagesBefore << 16, <u64>pagesAfter << 16);
}

export function sample2(): void {
    //logInteger(demo(7, 3))
}
