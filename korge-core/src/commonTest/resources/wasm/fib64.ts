// asc fib64.ts --outFile fib64.wasm --optimize && wasm2wat fib64.wasm
export function fib(n: i64): i64 {
  var a: i64 = 0, b: i64 = 1
  if (n > 0) {
    while (--n) {
      let t = a + b
      a = b
      b = t
    }
    return b
  }
  return a
}