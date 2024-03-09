// asc func_call.ts --outFile func_call.wasm && wasm2wat func_call.wasm
export function add(l: i32, r: i32): i32 {
    return l + r
}

export function sub(l: i32, r: i32): i32 {
    return l - r
}

export function myfunc(a: i32, b: i32, c: i32): i32 {
  return add(a, b) * sub(b, c)
}