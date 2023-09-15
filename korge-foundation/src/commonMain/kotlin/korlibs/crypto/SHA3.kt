/*
@file:Suppress("NAME_SHADOWING")

package korlibs.crypto

import korlibs.encoding.Hex

@KryptoExperimental
class SHA3_256 : SHA3(16, 256) {
}

@KryptoExperimental
abstract class SHA3 internal constructor(chunkSize: Int, val digestSizeBits: Int) : Hasher(chunkSize, digestSizeBits / 8) {
    private lateinit var keccak: SHA3Impl.Keccak

    init {
        coreReset()
    }

    // @TODO: Actual reset
    override fun coreReset() {
        keccak = SHA3Impl.Keccak(digestSizeBits, SHA3Impl.KECCAK_PADDING, digestSizeBits)
    }

    override fun corePadding(totalWritten: Long): ByteArray {
        //TODO("Not yet implemented")
        //return ByteArray(0)
        keccak.finalize()
        return ByteArray(0)
    }

    override fun coreUpdate(chunk: ByteArray) {
        keccak.update(chunk.asUByteArray())
    }

    override fun coreDigest(out: ByteArray) {
        val data = keccak.digest()
        data.copyInto(out.asUByteArray())
    }
}

// https://github.com/emn178/js-sha3/blob/master/src/sha3.js
/**
 * [js-sha3]{@link https://github.com/emn178/js-sha3}
 *
 * @version 0.8.0
 * @author Chen, Yi-Cyuan [emn178@gmail.com]
 * @copyright Chen, Yi-Cyuan 2015-2018
 * @license MIT
 */

@Suppress("unused")
@OptIn(ExperimentalUnsignedTypes::class)
internal object SHA3Impl {
    val SHAKE_PADDING = intArrayOf(31, 7936, 2031616, 520093696)
    val CSHAKE_PADDING = intArrayOf(4, 1024, 262144, 67108864)
    val KECCAK_PADDING = intArrayOf(1, 256, 65536, 16777216)
    val PADDING = intArrayOf(6, 1536, 393216, 100663296)
    val SHIFT = intArrayOf(0, 8, 16, 24)
    val RC = uintArrayOf(1U, 0U, 32898U, 0U, 32906U, 2147483648U, 2147516416U, 2147483648U, 32907U, 0U, 2147483649U,
        0U, 2147516545U, 2147483648U, 32777U, 2147483648U, 138U, 0U, 136U, 0U, 2147516425U, 0U,
        2147483658U, 0U, 2147516555U, 0U, 139U, 2147483648U, 32905U, 2147483648U, 32771U,
        2147483648U, 32770U, 2147483648U, 128U, 2147483648U, 32778U, 0U, 2147483658U, 2147483648U,
        2147516545U, 2147483648U, 32896U, 2147483648U, 2147483649U, 0U, 2147516424U, 2147483648U
    )
    val OUTPUT_TYPES = listOf("hex", "buffer", "arrayBuffer", "array", "digest")
    val BITS = intArrayOf(224, 256, 384, 512)
    val SHAKE_BITS = intArrayOf(128, 256)
    val CSHAKE_BYTEPAD = mapOf(
        "128" to 168,
        "256" to 136
    )

    /*
    fun createOutputMethod(bits: Int, padding: Int, outputType: String) {
        return { message ->
            return Keccak(bits, padding, bits).update(message)[outputType]();
        }
    }
    fun createShakeOutputMethod(bits: Int, padding: Int, outputType: String) {
        return { message, outputBits ->
            return Keccak(bits, padding, outputBits).update(message)[outputType]();
        }
    }
    fun createCshakeOutputMethod(bits: Int, padding: Int, outputType: String) {
        return { message, outputBits, n, s ->
            return methods['cshake' + bits].update(message, outputBits, n, s)[outputType]();
        }
    }
    fun createKmacOutputMethod(bits: Int, padding: Int, outputType: String) {
        return { key, message, outputBits, s ->
            return methods['kmac' + bits].update(key, message, outputBits, s)[outputType]();
        }
    }
    fun createOutputMethods(method, createMethod, bits, padding) {
        for (i in 0 until OUTPUT_TYPES.length) {
            var type = OUTPUT_TYPES[i];
            method[type] = createMethod(bits, padding, type);
        }
        return method;
    };
    interface Sha3Method {
        fun create()
        fun update(message: UByteArray)
    }
    fun createMethod (bits: Int, padding: Int) {
        var method = createOutputMethod(bits, padding, "hex");
        method.create = function () {
            return new Keccak(bits, padding, bits);
        };
        method.update = function (message) {
            return method.create().update(message);
        };
        return createOutputMethods(method, createOutputMethod, bits, padding);
    };
    fun createShakeMethod(bits: Int, padding: Int) {
        var method = createShakeOutputMethod(bits, padding, "hex");
        method.create = function (outputBits) {
            return new Keccak(bits, padding, outputBits);
        };
        method.update = function (message, outputBits) {
            return method.create(outputBits).update(message);
        };
        return createOutputMethods(method, createShakeOutputMethod, bits, padding);
    };
    fun createCshakeMethod(bits: Int, padding: Int) {
        val w = CSHAKE_BYTEPAD[bits];
        val method = createCshakeOutputMethod(bits, padding, "hex");
        method.create = { outputBits, n, s ->
            if (!n && !s) {
                return methods['shake' + bits].create(outputBits);
            } else {
                return Keccak(bits, padding, outputBits).bytepad([n, s], w);
            }
        };
        method.update = { message, outputBits, n, s ->
            return method.create(outputBits, n, s).update(message);
        };
        return createOutputMethods(method, createCshakeOutputMethod, bits, padding);
    };
    fun createKmacMethod(bits: Int, padding: Int) {
        val w = CSHAKE_BYTEPAD[bits];
        val method = createKmacOutputMethod(bits, padding, "hex");
        method.create = { key, outputBits, s,
            return Kmac(bits, padding, outputBits).bytepad(['KMAC', s], w).bytepad([key], w);
        };
        method.update = { key, message, outputBits, s ->
            return method.create(key, outputBits, s).update(message);
        };
        return createOutputMethods(method, createKmacOutputMethod, bits, padding);
    };
    class SHA3(val padding: Int, val bits: Int) {
        init {
            assert(padding in PADDING)
            assert(padding in PADDING)
        }
    }
    data class Algo(val name: String, val padding: IntArray, val bits: IntArray, val createMethod: Any)
    val algorithms = listOf(
        Algo(name = "keccak", padding = KECCAK_PADDING, bits = BITS, createMethod = createMethod),
        Algo(name = "sha3", padding = PADDING, bits = BITS, createMethod = createMethod),
        Algo(name = "shake", padding = SHAKE_PADDING, bits = SHAKE_BITS, createMethod = createShakeMethod),
        Algo(name = "cshake", padding = CSHAKE_PADDING, bits = SHAKE_BITS, createMethod = createCshakeMethod),
        Algo(name = "kmac", padding = CSHAKE_PADDING, bits = SHAKE_BITS, createMethod = createKmacMethod)
    )
    var methods = LinkedHashMap<String, Algo>()
    var methodNames = arrayListOf<String>()
    init {
        for (i in 0 until algorithms.size) {
            val algorithm = algorithms[i];
            val bits = algorithm.bits;
            for (j in 0 until bits.size) {
                val methodName = algorithm.name + '_' + bits[j];
                methodNames.add(methodName);
                methods[methodName] = algorithm.createMethod(bits[j], algorithm.padding);
                if (algorithm.name != "sha3") {
                    val newMethodName = algorithm.name + bits[j];
                    methodNames.add(newMethodName);
                    methods[newMethodName] = methods[methodName];
                }
            }
        }
    }
    */

    open class Keccak(bits: Int, var padding: IntArray, var outputBits: Int) {
        private var blocks = IntArray(50)
        private var buffer = UByteArray(bits / 8)
        private var s = IntArray(50)
        private var reset = true
        private var finalized = false
        private var block = 0
        private var start = 0
        private var blockCount = (1600 - (bits shl 1)) shr 5
        private var byteCount = this.blockCount shl 2
        private var outputBlocks = outputBits shr 5
        private var extraBytes = (outputBits and 31) shr 3
        private var lastByteIndex: Int = 0

        fun update(message: UByteArray): Keccak {
            if (this.finalized) {
                error("finalize already called")
            }
            val blocks = this.blocks
            val byteCount = this.byteCount
            val length: Int = message.size
            val blockCount = this.blockCount
            var index = 0
            val s = this.s
            var i: Int

            while (index < length) {
                if (this.reset) {
                    this.reset = false
                    blocks[0] = this.block
                    i = 1
                    while (i < blockCount + 1) {
                        blocks[i] = 0
                        ++i
                    }
                }

                i = this.start
                while (index < length && i < byteCount) {
                    blocks[i shr 2] = blocks[i shr 2] or (message[index].toInt() shl SHIFT[i++ and 3])
                    ++index
                }

                this.lastByteIndex = i
                if (i >= byteCount) {
                    this.start = i - byteCount
                    this.block = blocks[blockCount]
                    i = 0
                    while (i < blockCount) {
                        s[i] = s[i] xor blocks[i]
                        ++i
                    }
                    f(s)
                    this.reset = true
                } else {
                    this.start = i
                }
            }
            return this
        }

        fun encode(x: Int, right: Boolean = false): Int {
            var o = x and 255
            var n = 1
            // @TODO: Optimize
            val bytes = arrayListOf<Int>(o)
            var x = x shr 8
            o = x and 255
            while (o > 0) {
                bytes.add(0, o)
                x = x shr 8
                o = x and 255
                ++n
            }
            if (right) {
                bytes.add(n)
            } else {
                bytes.add(0, n)
            }
            // @TODO: Optimize
            this.update(bytes.map { it.toByte() }.toByteArray().asUByteArray())
            return bytes.size
        }

        fun encodeString(str: UByteArray): Int {
            var bytes: Int = str.size
            bytes += this.encode(bytes * 8, false)
            this.update(str)
            return bytes
        }

        fun bytepad(strs: List<UByteArray>, w: Int): Keccak {
            var bytes = this.encode(w, false)
            for (element in strs) bytes += this.encodeString(element)
            this.update(UByteArray(w - bytes % w))
            return this
        }

        open fun finalize() {
            if (this.finalized) return
            this.finalized = true
            val blocks = this.blocks
            val i = this.lastByteIndex
            val blockCount = this.blockCount
            val s = this.s
            blocks[i shr 2] = blocks[i shr 2] or this.padding[i and 3]
            if (this.lastByteIndex == this.byteCount) {
                blocks[0] = blocks[blockCount]
                for (i in 1 until blockCount + 1) blocks[i] = 0
            }
            blocks[blockCount - 1] = blocks[blockCount - 1] or 0x80000000.toInt()
            for (i in 0 until blockCount) s[i] = s[i] xor blocks[i]
            f(s)
        }

        fun hex(): String {
            this.finalize()
            val HEX_CHARS = Hex.DIGITS_LOWER

            val blockCount = this.blockCount
            val s = this.s
            val outputBlocks = this.outputBlocks
            val extraBytes = this.extraBytes
            var i = 0
            var j = 0
            val hex = StringBuilder()
            while (j < outputBlocks) {
                i = 0
                while (i < blockCount && j < outputBlocks) {
                    val block = s[i]
                    hex.append(HEX_CHARS[(block shr 4) and 0x0F])
                    hex.append(HEX_CHARS[block and 0x0F])
                    hex.append(HEX_CHARS[(block shr 12) and 0x0F])
                    hex.append(HEX_CHARS[(block shr 8) and 0x0F])
                    hex.append(HEX_CHARS[(block shr 20) and 0x0F])
                    hex.append(HEX_CHARS[(block shr 16) and 0x0F])
                    hex.append(HEX_CHARS[(block shr 28) and 0x0F])
                    hex.append(HEX_CHARS[(block shr 24) and 0x0F])
                    ++i
                    ++j
                }
                if (j % blockCount == 0) {
                    f(s)
                    i = 0
                }
            }
            if (extraBytes != 0) {
                val block = s[i]
                hex.append(HEX_CHARS[(block shr 4) and 0x0F])
                hex.append(HEX_CHARS[block and 0x0F])
                if (extraBytes > 1) {
                    hex.append(HEX_CHARS[(block shr 12) and 0x0F])
                    hex.append(HEX_CHARS[(block shr 8) and 0x0F])
                }
                if (extraBytes > 2) {
                    hex.append(HEX_CHARS[(block shr 20) and 0x0F])
                    hex.append(HEX_CHARS[(block shr 16) and 0x0F])
                }
            }
            return hex.toString()
        }

        fun arrayBuffer(): UByteArray {
            this.finalize()

            val blockCount = this.blockCount
            val s = this.s
            val outputBlocks = this.outputBlocks
            val extraBytes = this.extraBytes
            var i = 0
            var j = 0
            val bytes = this.outputBits shr 3
            val array: UIntArray = when {
                extraBytes != 0 -> UIntArray((outputBlocks + 1))
                else -> UIntArray(bytes / 4)
            }
            while (j < outputBlocks) {
                i = 0
                while (i < blockCount && j < outputBlocks) {
                    array[j] = s[i].toUInt()
                    ++i
                    ++j
                }
                if (j % blockCount == 0) {
                    f(s)
                }
            }
            if (extraBytes != 0) {
                array[i] = s[i].toUInt()
                buffer = buffer.copyOfRange(0, bytes)
            }
            return buffer
        }

        fun digest(): UByteArray {
            this.finalize()

            val blockCount = this.blockCount
            val s = this.s
            val outputBlocks = this.outputBlocks
            val extraBytes = this.extraBytes
            var i = 0
            var j = 0
            val array = UByteArray(outputBlocks * 4)
            var offset: Int
            var block: Int
            while (j < outputBlocks) {
                i = 0
                while (i < blockCount && j < outputBlocks) {
                    offset = j shl 2
                    block = s[i]
                    array[offset + 0] = ((block shr 0) and 0xFF).toUByte()
                    array[offset + 1] = ((block shr 8) and 0xFF).toUByte()
                    array[offset + 2] = ((block shr 16) and 0xFF).toUByte()
                    array[offset + 3] = ((block shr 24) and 0xFF).toUByte()
                    ++i
                    ++j
                }
                if (j % blockCount == 0) {
                    f(s)
                }
            }
            if (extraBytes != 0) {
                offset = j shl 2
                block = s[i]
                array[offset] = (block and 0xFF).toUByte()
                if (extraBytes > 1) {
                    array[offset + 1] = ((block shr 8) and 0xFF).toUByte()
                }
                if (extraBytes > 2) {
                    array[offset + 2] = ((block shr 16) and 0xFF).toUByte()
                }
            }
            return array
        }

    }

    class Kmac(bits: Int, padding: IntArray, outputBits: Int) : Keccak(bits, padding, outputBits) {
        override fun finalize() {
            this.encode(this.outputBits, true)
            return super.finalize()
        }
    }

    fun f(s: IntArray) {
        for (n in 0 until 48 step 2) {
            val c0 = s[0] xor s[10] xor s[20] xor s[30] xor s[40]
            val c1 = s[1] xor s[11] xor s[21] xor s[31] xor s[41]
            val c2 = s[2] xor s[12] xor s[22] xor s[32] xor s[42]
            val c3 = s[3] xor s[13] xor s[23] xor s[33] xor s[43]
            val c4 = s[4] xor s[14] xor s[24] xor s[34] xor s[44]
            val c5 = s[5] xor s[15] xor s[25] xor s[35] xor s[45]
            val c6 = s[6] xor s[16] xor s[26] xor s[36] xor s[46]
            val c7 = s[7] xor s[17] xor s[27] xor s[37] xor s[47]
            val c8 = s[8] xor s[18] xor s[28] xor s[38] xor s[48]
            val c9 = s[9] xor s[19] xor s[29] xor s[39] xor s[49]

            var h = c8 xor ((c2 shl 1) or (c3 ushr 31))
            var l = c9 xor ((c3 shl 1) or (c2 ushr 31))
            s[0] = s[0] xor h
            s[1] = s[1] xor l
            s[10] = s[10] xor h
            s[11] = s[11] xor l
            s[20] = s[20] xor h
            s[21] = s[21] xor l
            s[30] = s[30] xor h
            s[31] = s[31] xor l
            s[40] = s[40] xor h
            s[41] = s[41] xor l
            h = c0 xor ((c4 shl 1) or (c5 ushr 31))
            l = c1 xor ((c5 shl 1) or (c4 ushr 31))
            s[2] = s[2] xor h
            s[3] = s[3] xor l
            s[12] = s[12] xor h
            s[13] = s[13] xor l
            s[22] = s[22] xor h
            s[23] = s[23] xor l
            s[32] = s[32] xor h
            s[33] = s[33] xor l
            s[42] = s[42] xor h
            s[43] = s[43] xor l
            h = c2 xor ((c6 shl 1) or (c7 ushr 31))
            l = c3 xor ((c7 shl 1) or (c6 ushr 31))
            s[4] = s[4] xor h
            s[5] = s[5] xor l
            s[14] = s[14] xor h
            s[15] = s[15] xor l
            s[24] = s[24] xor h
            s[25] = s[25] xor l
            s[34] = s[34] xor h
            s[35] = s[35] xor l
            s[44] = s[44] xor h
            s[45] = s[45] xor l
            h = c4 xor ((c8 shl 1) or (c9 ushr 31))
            l = c5 xor ((c9 shl 1) or (c8 ushr 31))
            s[6] = s[6] xor h
            s[7] = s[7] xor l
            s[16] = s[16] xor h
            s[17] = s[17] xor l
            s[26] = s[26] xor h
            s[27] = s[27] xor l
            s[36] = s[36] xor h
            s[37] = s[37] xor l
            s[46] = s[46] xor h
            s[47] = s[47] xor l
            h = c6 xor ((c0 shl 1) or (c1 ushr 31))
            l = c7 xor ((c1 shl 1) or (c0 ushr 31))
            s[8] = s[8] xor h
            s[9] = s[9] xor l
            s[18] = s[18] xor h
            s[19] = s[19] xor l
            s[28] = s[28] xor h
            s[29] = s[29] xor l
            s[38] = s[38] xor h
            s[39] = s[39] xor l
            s[48] = s[48] xor h
            s[49] = s[49] xor l

            val b00 = s[0]
            val b01 = s[1]
            val b32 = (s[11] shl 4) or (s[10] ushr 28)
            val b33 = (s[10] shl 4) or (s[11] ushr 28)
            val b14 = (s[20] shl 3) or (s[21] ushr 29)
            val b15 = (s[21] shl 3) or (s[20] ushr 29)
            val b46 = (s[31] shl 9) or (s[30] ushr 23)
            val b47 = (s[30] shl 9) or (s[31] ushr 23)
            val b28 = (s[40] shl 18) or (s[41] ushr 14)
            val b29 = (s[41] shl 18) or (s[40] ushr 14)
            val b20 = (s[2] shl 1) or (s[3] ushr 31)
            val b21 = (s[3] shl 1) or (s[2] ushr 31)
            val b02 = (s[13] shl 12) or (s[12] ushr 20)
            val b03 = (s[12] shl 12) or (s[13] ushr 20)
            val b34 = (s[22] shl 10) or (s[23] ushr 22)
            val b35 = (s[23] shl 10) or (s[22] ushr 22)
            val b16 = (s[33] shl 13) or (s[32] ushr 19)
            val b17 = (s[32] shl 13) or (s[33] ushr 19)
            val b48 = (s[42] shl 2) or (s[43] ushr 30)
            val b49 = (s[43] shl 2) or (s[42] ushr 30)
            val b40 = (s[5] shl 30) or (s[4] ushr 2)
            val b41 = (s[4] shl 30) or (s[5] ushr 2)
            val b22 = (s[14] shl 6) or (s[15] ushr 26)
            val b23 = (s[15] shl 6) or (s[14] ushr 26)
            val b04 = (s[25] shl 11) or (s[24] ushr 21)
            val b05 = (s[24] shl 11) or (s[25] ushr 21)
            val b36 = (s[34] shl 15) or (s[35] ushr 17)
            val b37 = (s[35] shl 15) or (s[34] ushr 17)
            val b18 = (s[45] shl 29) or (s[44] ushr 3)
            val b19 = (s[44] shl 29) or (s[45] ushr 3)
            val b10 = (s[6] shl 28) or (s[7] ushr 4)
            val b11 = (s[7] shl 28) or (s[6] ushr 4)
            val b42 = (s[17] shl 23) or (s[16] ushr 9)
            val b43 = (s[16] shl 23) or (s[17] ushr 9)
            val b24 = (s[26] shl 25) or (s[27] ushr 7)
            val b25 = (s[27] shl 25) or (s[26] ushr 7)
            val b06 = (s[36] shl 21) or (s[37] ushr 11)
            val b07 = (s[37] shl 21) or (s[36] ushr 11)
            val b38 = (s[47] shl 24) or (s[46] ushr 8)
            val b39 = (s[46] shl 24) or (s[47] ushr 8)
            val b30 = (s[8] shl 27) or (s[9] ushr 5)
            val b31 = (s[9] shl 27) or (s[8] ushr 5)
            val b12 = (s[18] shl 20) or (s[19] ushr 12)
            val b13 = (s[19] shl 20) or (s[18] ushr 12)
            val b44 = (s[29] shl 7) or (s[28] ushr 25)
            val b45 = (s[28] shl 7) or (s[29] ushr 25)
            val b26 = (s[38] shl 8) or (s[39] ushr 24)
            val b27 = (s[39] shl 8) or (s[38] ushr 24)
            val b08 = (s[48] shl 14) or (s[49] ushr 18)
            val b09 = (s[49] shl 14) or (s[48] ushr 18)

            s[0] = b00 xor (b02.inv() and b04)
            s[1] = b01 xor (b03.inv() and b05)
            s[10] = b10 xor (b12.inv() and b14)
            s[11] = b11 xor (b13.inv() and b15)
            s[20] = b20 xor (b22.inv() and b24)
            s[21] = b21 xor (b23.inv() and b25)
            s[30] = b30 xor (b32.inv() and b34)
            s[31] = b31 xor (b33.inv() and b35)
            s[40] = b40 xor (b42.inv() and b44)
            s[41] = b41 xor (b43.inv() and b45)
            s[2] = b02 xor (b04.inv() and b06)
            s[3] = b03 xor (b05.inv() and b07)
            s[12] = b12 xor (b14.inv() and b16)
            s[13] = b13 xor (b15.inv() and b17)
            s[22] = b22 xor (b24.inv() and b26)
            s[23] = b23 xor (b25.inv() and b27)
            s[32] = b32 xor (b34.inv() and b36)
            s[33] = b33 xor (b35.inv() and b37)
            s[42] = b42 xor (b44.inv() and b46)
            s[43] = b43 xor (b45.inv() and b47)
            s[4] = b04 xor (b06.inv() and b08)
            s[5] = b05 xor (b07.inv() and b09)
            s[14] = b14 xor (b16.inv() and b18)
            s[15] = b15 xor (b17.inv() and b19)
            s[24] = b24 xor (b26.inv() and b28)
            s[25] = b25 xor (b27.inv() and b29)
            s[34] = b34 xor (b36.inv() and b38)
            s[35] = b35 xor (b37.inv() and b39)
            s[44] = b44 xor (b46.inv() and b48)
            s[45] = b45 xor (b47.inv() and b49)
            s[6] = b06 xor (b08.inv() and b00)
            s[7] = b07 xor (b09.inv() and b01)
            s[16] = b16 xor (b18.inv() and b10)
            s[17] = b17 xor (b19.inv() and b11)
            s[26] = b26 xor (b28.inv() and b20)
            s[27] = b27 xor (b29.inv() and b21)
            s[36] = b36 xor (b38.inv() and b30)
            s[37] = b37 xor (b39.inv() and b31)
            s[46] = b46 xor (b48.inv() and b40)
            s[47] = b47 xor (b49.inv() and b41)
            s[8] = b08 xor (b00.inv() and b02)
            s[9] = b09 xor (b01.inv() and b03)
            s[18] = b18 xor (b10.inv() and b12)
            s[19] = b19 xor (b11.inv() and b13)
            s[28] = b28 xor (b20.inv() and b22)
            s[29] = b29 xor (b21.inv() and b23)
            s[38] = b38 xor (b30.inv() and b32)
            s[39] = b39 xor (b31.inv() and b33)
            s[48] = b48 xor (b40.inv() and b42)
            s[49] = b49 xor (b41.inv() and b43)

            s[0] = s[0] xor RC[n].toInt()
            s[1] = s[1] xor RC[n + 1].toInt()
        }
    }
}
4  krypto/src/commonMain/kotlin/korlibs/crypto/annotations/KryptoExperimental.kt
Viewed
@@ -0,0 +1,4 @@
package korlibs.crypto.annotations

@RequiresOptIn
annotation class KryptoExperimental
34  krypto/src/commonTest/kotlin/korlibs/crypto/SHA3Test.kt
Viewed
@@ -0,0 +1,34 @@
package korlibs.crypto

import korlibs.crypto.annotations.KryptoExperimental
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(KryptoExperimental::class)
class SHA3Test {
    @Test
    fun test() {
        val sha3 = SHA3_256()
        assertEquals("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a", sha3.update(byteArrayOf()).digest().hexLower)
    }
}

/*
SHA3-224("")
6b4e03423667dbb73b6e15454f0eb1abd4597f9a1b078e3f5b5a6bc7
SHA3-256("")
a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a
SHA3-384("")
0c63a75b845e4f7d01107d852e4c2485c51a50aaaa94fc61995e71bbee983a2ac3713831264adb47fb6bd1e058d5f004
SHA3-512("")
a69f73cca23a9ac5c8b567dc185a756e97c982164fe25859e0d1dcc1475c80a615b2123af1f5f94c11e3e9402c3ac558f500199d95b6d3e301758586281dcd26
SHAKE128("", 256)
7f9c2ba4e88f827d616045507605853ed73b8093f6efbc88eb1a6eacfa66ef26
SHAKE256("", 512)
46b9dd2b0ba88d13233b3feb743eeb243fcd52ea62b81b82b50c27646ed5762fd75dc4ddd8c0f200cb05019d67b592f6fc821c49479ab48640292eacb3b7c4be
SHAKE128("The quick brown fox jumps over the lazy dog", 256)
f4202e3c5852f9182a0430fd8144f0a74b95e7417ecae17db0f8cfeed0e3e66e
SHAKE128("The quick brown fox jumps over the lazy dof", 256)
853f4538be0db9621a6cea659a06c1107b1f83f02b13d18297bd39d7411cf10c
 */

 */
