// asc memops32.ts --outFile memops32.wasm && wasm2wat memops32.wasm> /tmp/memops32.wat && cat /tmp/memops32.wat

@external("log", "integer") declare function logInteger(v: i32): void
@external("log", "float") declare function logFloat(v: f32): void
@external("log", "string") declare function logString(v: string): void

var demo1: boolean = false
var demo2: i8 = 1
var demo3: i16 = 2
var demo4: i32 = 3
var demo6: f32 = 5.0
var demo8: u8 = 7
var demo9: i16 = 8
var demo10: u32 = 9

//function add(l: i32, r: i32): i32 {
//    return l + r;
//}

export function main(): void {
    logInteger(add(1, 2))
    logInteger(demo1)
    logInteger(demo2)
    logInteger(demo3)
    logInteger(demo4)
    logFloat(demo6)
    logInteger(demo8)
    logInteger(demo9)
    logInteger(demo10)
    demo1 = true
    demo2++;
    demo3++;
    demo4++;
    demo6++;
    demo8++;
    demo9++;
    demo10++;
    logInteger(demo1)
    logInteger(demo2)
    logInteger(demo3)
    logInteger(demo4)
    logFloat(demo6)
    logInteger(demo8)
    logInteger(demo9)
    logInteger(demo10)
    logInteger(load<i8>(10))
    store<i8>(10, -11)
    logInteger(load<i8>(10))
    logInteger(load<u8>(10))
    logString("hello")
    logString("worlds")
}
