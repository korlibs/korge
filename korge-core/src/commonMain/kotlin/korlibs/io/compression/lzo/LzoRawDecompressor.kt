package korlibs.io.compression.lzo

import korlibs.io.lang.MalformedInputException
import korlibs.memory.*

object LzoRawDecompressor {
    private val DEC_32_TABLE = intArrayOf(4, 1, 2, 1, 4, 4, 4, 4)
    private val DEC_64_TABLE = intArrayOf(0, 0, 0, -1, 0, 1, 2, 3)

    @Throws(MalformedInputException::class)
    fun decompress(
        inputBase: ByteArray,
        inputAddress: Int,
        inputLimit: Int,
        outputBase: ByteArray,
        outputAddress: Int,
        outputLimit: Int
    ): Int {
        // nothing compresses to nothing
        if (inputAddress == inputLimit) {
            return 0
        }

        // maximum offset in buffers to which it's safe to write long-at-a-time
        val fastOutputLimit: Int = outputLimit - LzoConstants.SIZE_OF_LONG

        // LZO can concat multiple blocks together so, decode until all input data is consumed
        var input = inputAddress
        var output = outputAddress
        while (input < inputLimit) {
            var firstCommand = true
            var lastLiteralLength = 0
            while (true) {
                if (input >= inputLimit) {
                    throw MalformedInputException(input - inputAddress)
                }

                val command: Int = inputBase.getU8(input++)
                // Commands are described using a bit pattern notation:
                // 0: bit is not set
                // 1: bit is set
                // L: part of literal length
                // H: high bits of match offset position
                // D: low bits of match offset position
                // M: part of match length
                // ?: see documentation in command decoder
                var matchLength: Int
                var matchOffset: Int
                var literalLength: Int
                if (command and 240 == 0) {
                    if (lastLiteralLength == 0) {
                        // 0b0000_LLLL (0bLLLL_LLLL)*
                        // copy 4 or more literals only

                        // copy length :: fixed
                        //   0
                        matchOffset = 0

                        // copy offset :: fixed
                        //   0
                        matchLength = 0

                        // literal length - 3 :: variable bits :: valid range [4..]
                        //   3 + variableLength(command bits [0..3], 4)
                        literalLength = command and 15
                        if (literalLength == 0) {
                            literalLength = 15
                            var nextByte = 0
                            while (input < inputLimit && inputBase.getU8(input++).also {
                                    nextByte = it
                                } == 0
                            ) {
                                literalLength += 255
                            }
                            literalLength += nextByte
                        }
                        literalLength += 3
                    } else if (lastLiteralLength <= 3) {
                        // 0b0000_DDLL 0bHHHH_HHHH
                        // copy of a 2-byte block from the dictionary within a 1kB distance

                        // copy length: fixed
                        //   2
                        matchLength = 2

                        // copy offset :: valid range [1..1024]
                        //   DD from command [2..3]
                        //   HH from trailer [0..7]
                        // offset = (HH << 2) + DD + 1
                        if (input >= inputLimit) {
                            throw MalformedInputException(input - inputAddress)
                        }
                        matchOffset = command and 12 ushr 2
                        matchOffset = matchOffset or (inputBase.getU8(input++) shl 2)

                        // literal length :: 2 bits :: valid range [0..3]
                        //   [0..1] from command [0..1]
                        literalLength = command and 3
                    } else {
                        // 0b0000_DDLL 0bHHHH_HHHH

                        // copy length :: fixed
                        //   3
                        matchLength = 3

                        // copy offset :: 10 bits :: valid range [2049..3072]
                        //   DD from command [2..3]
                        //   HH from trailer [0..7]
                        // offset = (H << 2) + D + 2049
                        if (input >= inputLimit) {
                            throw MalformedInputException(input - inputAddress)
                        }
                        matchOffset = command and 12 ushr 2
                        matchOffset = matchOffset or (inputBase.getU8(input++) shl 2)
                        matchOffset = matchOffset or 2048

                        // literal length :: 2 bits :: valid range [0..3]
                        //   [0..1] from command [0..1]
                        literalLength = command and 3
                    }
                } else if (firstCommand) {
                    // first command has special handling when high nibble is set
                    matchLength = 0
                    matchOffset = 0
                    literalLength = command - 17
                } else if (command and 240 == 16) {
                    // 0b0001_HMMM (0bMMMM_MMMM)* 0bDDDD_DDDD_DDDD_DDLL

                    // copy length - 2 :: variable bits :: valid range [3..]
                    //   2 + variableLength(command bits [0..2], 3)
                    matchLength = command and 7
                    if (matchLength == 0) {
                        matchLength = 7
                        var nextByte = 0
                        while (input < inputLimit && inputBase.getU8(input++).also {
                                nextByte = it
                            } == 0) {
                            matchLength += 255
                        }
                        matchLength += nextByte
                    }
                    matchLength += 2

                    // read trailer
                    if (input + LzoConstants.SIZE_OF_SHORT > inputLimit) {
                        throw MalformedInputException(input - inputAddress)
                    }

                    val trailer: Int = inputBase.getU16LE(input)
                    input += LzoConstants.SIZE_OF_SHORT

                    // copy offset :: 16 bits :: valid range [16383..49151]
                    //   [0..13] from trailer [2..15]
                    //   [14] if command bit [3] set
                    //   plus fixed offset 0b11_1111_1111_1111
                    matchOffset = command and 8 shl 11
                    matchOffset += trailer shr 2
                    if (matchOffset == 0) {
                        // match offset of zero, means that this is the last command in the sequence
                        break
                    }
                    matchOffset += 16383

                    // literal length :: 2 bits :: valid range [0..3]
                    //   [0..1] from trailer [0..1]
                    literalLength = trailer and 3
                } else if (command and 224 == 32) {
                    // command in [32, 63]
                    // 0b001M_MMMM (0bMMMM_MMMM)* 0bDDDD_DDDD_DDDD_DDLL

                    // copy length - 2 :: variable bits :: valid range [3..]
                    //   2 + variableLength(command bits [0..4], 5)
                    matchLength = command and 31
                    if (matchLength == 0) {
                        matchLength = 31
                        var nextByte = 0
                        while (input < inputLimit && inputBase.getU8(input++).also {
                                nextByte = it
                            } == 0) {
                            matchLength += 255
                        }
                        matchLength += nextByte
                    }
                    matchLength += 2

                    // read trailer
                    if (input + LzoConstants.SIZE_OF_SHORT > inputLimit) {
                        throw MalformedInputException(input - inputAddress)
                    }

                    val trailer: Int = inputBase.getU16LE(input)
                    input += LzoConstants.SIZE_OF_SHORT

                    // copy offset :: 14 bits :: valid range [0..16383]
                    //  [0..13] from trailer [2..15]
                    matchOffset = trailer ushr 2

                    // literal length :: 2 bits :: valid range [0..3]
                    //   [0..1] from trailer [0..1]
                    literalLength = trailer and 3
                } else if (command and 192 != 0) {
                    // 0bMMMD_DDLL 0bHHHH_HHHH

                    // copy length - 1 :: 3 bits :: valid range [1..8]
                    //   [0..2] from command [5..7]
                    //   add 1
                    matchLength = command and 224 ushr 5
                    matchLength += 1

                    // copy offset :: 11 bits :: valid range [0..4095]
                    //   [0..2] from command [2..4]
                    //   [3..10] from trailer [0..7]
                    if (input >= inputLimit) {
                        throw MalformedInputException(input - inputAddress)
                    }
                    matchOffset = command and 28 ushr 2
                    matchOffset =
                        matchOffset or (inputBase.getU8(input++) shl 3)

                    // literal length :: 2 bits :: valid range [0..3]
                    //   [0..1] from command [0..1]
                    literalLength = command and 3
                } else {
                    throw MalformedInputException(input - 1, "Invalid LZO command $command")
                }
                firstCommand = false

                // copy match
                if (matchLength != 0) {
                    // lzo encodes match offset minus one
                    matchOffset++
                    var matchAddress = output - matchOffset
                    if (matchAddress < outputAddress || output + matchLength > outputLimit) {
                        throw MalformedInputException(input - inputAddress)
                    }
                    val matchOutputLimit = output + matchLength
                    if (output > fastOutputLimit) {
                        // slow match copy
                        while (output < matchOutputLimit) {
                            outputBase.set8(output++, outputBase.getU8(matchAddress++))
                        }
                    } else {
                        // copy repeated sequence
                        if (matchOffset < LzoConstants.SIZE_OF_LONG) {
                            // 8 bytes apart so that we can copy long-at-a-time below
                            val increment32 = DEC_32_TABLE[matchOffset]
                            val decrement64 = DEC_64_TABLE[matchOffset]
                            outputBase.set8(output + 0, outputBase.getU8(matchAddress + 0))
                            outputBase.set8(output + 1, outputBase.getU8(matchAddress + 1))
                            outputBase.set8(output + 2, outputBase.getU8(matchAddress + 2))
                            outputBase.set8(output + 3, outputBase.getU8(matchAddress + 3))
                            output += LzoConstants.SIZE_OF_INT
                            matchAddress += increment32
                            outputBase.set32LE(output, outputBase.getU32LE(matchAddress))
                            output += LzoConstants.SIZE_OF_INT
                            matchAddress -= decrement64
                        } else {
                            outputBase.set64LE(output, outputBase.getS64LE(matchAddress))
                            matchAddress += LzoConstants.SIZE_OF_LONG
                            output += LzoConstants.SIZE_OF_LONG
                        }
                        if (matchOutputLimit >= fastOutputLimit) {
                            if (matchOutputLimit > outputLimit) {
                                throw MalformedInputException(input - inputAddress)
                            }
                            while (output < fastOutputLimit) {
                                outputBase.set64LE(output, outputBase.getS64LE(matchAddress))
                                matchAddress += LzoConstants.SIZE_OF_LONG
                                output += LzoConstants.SIZE_OF_LONG
                            }
                            while (output < matchOutputLimit) {
                                outputBase.set8(output++, outputBase.getU8(matchAddress++))
                            }
                        } else {
                            while (output < matchOutputLimit) {
                                outputBase.set64LE(output, outputBase.getS64LE(matchAddress))
                                matchAddress += LzoConstants.SIZE_OF_LONG
                                output += LzoConstants.SIZE_OF_LONG
                            }
                        }
                    }
                    output = matchOutputLimit // correction in case we over-copied
                }

                // copy literal
                val literalOutputLimit = output + literalLength
                if (literalOutputLimit > fastOutputLimit || input + literalLength > inputLimit - LzoConstants.SIZE_OF_LONG) {
                    if (literalOutputLimit > outputLimit) {
                        throw MalformedInputException(input - inputAddress)
                    }

                    // slow, precise copy
                    arraycopy(inputBase, input, outputBase, output, literalLength)
                    input += literalLength
                    output += literalLength
                } else {
                    // fast copy. We may over-copy but there's enough room in input and output to not overrun them
                    do {
                        outputBase.set64LE(output, inputBase.getS64LE(input))
                        input += LzoConstants.SIZE_OF_LONG
                        output += LzoConstants.SIZE_OF_LONG
                    } while (output < literalOutputLimit)
                    input -= output - literalOutputLimit // adjust index if we over-copied
                    output = literalOutputLimit
                }
                lastLiteralLength = literalLength
            }
        }
        return (output - outputAddress)
    }
}
