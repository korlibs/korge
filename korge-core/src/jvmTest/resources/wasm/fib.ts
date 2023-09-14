// asc fib.ts --outFile fib.wasm --optimize && wasm2wat fib.wasm > fib.wat

@external("log", "integer")
declare function logInteger(i: i32): void

export function fib(n: i32): i32 {
  var a = 0, b = 1
  if (n > 0) {
    while (--n) {
      let t = a + b
      a = b
      b = t
      logInteger(n)
    }
    return b
  }
  return a
}