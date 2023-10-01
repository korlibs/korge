---
permalink: /math/bignum/
group: math
layout: default
title: Big Numbers
title_short: Big Numbers
description: Package for dealing with Big Numbers both integral and decimal
fa-icon: fa-sort-numeric-up
priority: 200
---


<https://github.com/korlibs/korge/tree/main/korge-foundation/src/common/korlibs/bignumber>{:target=_blank}

## General Bits

KBigNum exposes two basic classes: `BigInt` and `BigNum`.
For them, you can construct ranges with: `BigIntRange`+`BigIntProgression` and `ClosedBigNumRange`

Basically you can do the typical operations you would do with `Int` or `Double`,
but the precision is only limited by memory.

Also, they are much slower than their fixed-width counterparts.

In the case of JS, JVM & Android, BigInt wraps each native big integer implementation.
There is also a `CommonBigInt` implementation, that is used in targets not providing a native BigInt implementation.

Have in mind that `CommonBigInt` has not been optimized, so the performance is subpar in some operations where
some algorithms would provide a much better performance.

## `BigInt`

This class allow to represent precise Big Integers that do not overflow.

### Constructing `BigInt` instances

You can construct `BigInt` by using the `.bi` extensions:

```kotlin
val Long.bi: BigInt
val Int.bi: BigInt
val String.bi: BigInt
fun String.bi(radix: Int): BigInt
```

In the case you want to use a different base other than decimal, you can use `String.bi(radix)`,
where radix is the base you want to use: 2 for binary, 10 for decimal, 16 hexadecimal, etc.

### Converting from `BigInt` to other types

You can convert the `BigInt` into an `Int`, a `BigInt` or a `String` by using some of these methods:

```kotlin
fun BigInt.toInt(): Int // Will throw [BigIntOverflowException] if the number cannot be represented as an [Int]
fun BigInt.toBigNum(): BigNum
fun BigInt.toString(): String
fun BigInt.toString(radix: Int): String
```

### Getting sign of the `BigInt`

You can determine the sign of the [BigInt] with these methods:

```kotlin
val BigInt.signum: Int // Returns -1, 0 or +1 depending on the sign of this [BigInt]
val BigInt.isZero: Boolean // Determines if this [BigInt] is 0
val BigInt.isNotZero: Boolean // Determines if this [BigInt] is not 0
val BigInt.isNegative: Boolean // Determines if this [BigInt] is negative
val BigInt.isPositive: Boolean // Determines if this [BigInt] is positive
val BigInt.isNegativeOrZero: Boolean // Determines if this [BigInt] is either negative or zero (non-positive)
val BigInt.isPositiveOrZero: Boolean // Determines if this [BigInt] is either positive or zero (non-negative)
```

### `BigInt` operations

You can perform typical unary arithmetic operations:

```kotlin
-(1.bi) == (-1).bi
+(1.bi) == (+1).bi
(-1).bi.abs() == (+1).bi
7.bi.square() == 49.bi
```

And binary arithmetic operations:

```kotlin
(1.bi + 2.bi) == 3.bi
(1.bi - 2.bi) == (-1).bi
(2.bi * 3.bi) == 6.bi
(6.bi / 3.bi) == 2.bi
(9.bi % 5.bi) == 4.bi
(3.bi pow 100.bi) == "515377520732011331036461129765621272702107522001".bi
val (div, rem) = (97.bi divRem 17.bi)
```

Also, you can do bit-wise operations:

```kotlin
// Bit combination
("0101".bi(2) and "0110".bi(2)).toString(2).padStart(4, '0') == "0100"
("0101".bi(2) or "0110".bi(2)).toString(2).padStart(4, '0') == "0111"
("0101".bi(2) xor "0110".bi(2)).toString(2).padStart(4, '0') == "0011"

// Bit shifting
("0101".bi(2) shl 2).toString(2).padStart(8, '0') == "00010100"
("0101".bi(2) shr 2).toString(2).padStart(8, '0') == "00000001"
```

### Constructing `BigInt` ranges and progressions

```kotlin
(10.bi)..(20.bi)
(10.bi..20.bi) step 2.bi
((10.bi..15.bi) step 2.bi).toList() == listOf(10.bi, 12.bi, 14.bi)
```


## BigNum

### Constructing `BigNum` instances

You can construct `BigInt` instances by using the `.bn` extensions:

```kotlin
val Double.bn: BigNum
val Long.bn: BigNum
val Int.bn: BigNum
val String.bn: BigNum
```

### Adjusting `BigNum` number of decimals

In the case you want to have more (or less) decimals,
you can adjust it with the `BigNum.convertToScale` extension.

```kotlin
fun BigNum.convertToScale(scale: Int): BigNum
```

### `BigNum` operations

The operations available to `BigNum` are much less reduced that the ones available to [BigInt].

You can do binary operations:

```kotlin
10.5.bn + 11.0.bn == "21.5".bn
10.5.bn - 11.0.bn == "0.5".bn
10.5.bn * 11.0.bn == "115.50".bn
10.5.bn / 11.0.bn == "0.9".bn
10.5.bn pow 4 == "12155.0625".bn
```

In the case of the division, we might want to control the number of decimals we want to have: 

```kotlin
10.5.bn.div(11.0.bn, 4) == "0.95454".bn
```

### Rounding `BigNum` to `BigInt`

```kotlin
fun BigNum.toBigInt(): BigInt
fun BigNum.toBigIntFloor(): BigInt
fun BigNum.toBigIntCeil(): BigInt
fun BigNum.toBigIntRound(): BigInt
```

### Getting `BigNum` decimal part:

```kotlin
"20.675".bn.decimalPart == "675".bi
```

### `BigNum` ranges

We can construct closed ranges the range `..` operator:

```kotlin
(10.4.bn in (10.5.bn .. 16.5.bn)) == false
```
