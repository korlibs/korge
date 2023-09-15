// asc console1.ts --outFile console1.wasm --optimize && wasm2wat console1.wasm> /tmp/console1.wat && cat /tmp/console1.wat

export function demo(i: i32): void {
    console.log(`hello ${i}`)
}
