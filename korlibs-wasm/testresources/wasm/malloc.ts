// asc malloc.ts --outFile malloc.wasm --optimize && wasm2wat malloc.wasm> /tmp/malloc.wat && cat /tmp/malloc.wat

export function malloc(size: usize): usize {
    //console.log(`alloc ${size}`)
    return heap.alloc(size)
}
export function free(ptr: usize): void {
    //console.log(`free ${ptr}`)
    heap.free(ptr)
}
export function realloc(ptr: usize, size: usize): usize {
    //console.log(`realloc ${ptr}, ${size}`)
    return heap.realloc(ptr, size)
}