---
permalink: /crypto/
group: reference
layout: default
title: "Crypto"
fa-icon: fa-lock
priority: 950
---

Krypto is a cryptography library for Multiplatform Kotlin.


## SecureRandom

Krypto provides a `SecureRandom` object that extends the `kotlin.random.Random` class,
but it generates cryptographic secure values. It is a singleton, and you cannot provide an initial seed.

Instead of a pseudo-random and reproducible, its result values are fully random,
making it suitable for cryptographic purposes, and not suitable for reproductible results.

It uses `SecureRandom` on the JVM + [`PRNGFixes`](https://android-developers.googleblog.com/2013/08/some-securerandom-thoughts.html){:target="_blank",:rel="noopener"} on Android.
On Native POSIX (including Linux, macOS and iOS), it uses `/dev/urandom`, on Windows
[`BCryptGenRandom`](https://docs.microsoft.com/en-us/windows/desktop/api/bcrypt/nf-bcrypt-bcryptgenrandom){:target="_blank",:rel="noopener"}

### Using the SecureRandom instance

Since it is an object, you can use it directly as a `Random` instance:

```kotlin
val random: Random = SecureRandom
```

### Seeding extra bytes

In addition to the standard kotlin `Random` interface, SecureRandom provides a method for seeding extra random bytes.

```kotlin
val bytes = byteArrayOf(1, 2, 3)
SecureRandom.addSeed(bytes)
```

## Hash (MD4/MD5/SHA1/SHA256/SHA512)

```kotlin
fun ByteArray.hash(algo: HashFactory): ByteArray
fun ByteArray.md4()
fun ByteArray.md5()
fun ByteArray.sha1()
fun ByteArray.sha256()
fun ByteArray.sha512()

object MD4 : HashFactory
object MD5 : HashFactory
object SHA1 : HashFactory
object SHA256 : HashFactory
object SHA512 : HashFactory

open class HashFactory() {
    fun create(): Hash
}

fun HashFactory.digest(data: ByteArray): ByteArray

abstract class Hash {
    val chunkSize: Int
    val digestSize: Int
    
    fun reset(): Hash
    fun update(data: ByteArray, offset: Int, count: Int): Hash
    fun digest(): ByteArray
    fun digestOut(out: ByteArray)
}
```

## HMAC

## PBKDF2

## AES

```kotlin
object AES {
    fun decryptAes128Cbc(encryptedMessage: ByteArray, cipherKey: ByteArray): ByteArray
    fun encryptEes128Cbc(plainMessage: ByteArray, cipherKey: ByteArray): ByteArray
}
```
