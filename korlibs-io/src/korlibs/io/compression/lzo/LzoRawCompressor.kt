package korlibs.io.compression.lzo

import korlibs.memory.*

object LzoRawCompressor {
    const val LAST_LITERAL_SIZE = 5
    const val MIN_MATCH = 4
    private const val MAX_INPUT_SIZE = 0x7E000000 /* 2 113 929 216 bytes */
    private const val HASH_LOG = 12
    private const val MIN_TABLE_SIZE = 16
    const val MAX_TABLE_SIZE = 1 shl HASH_LOG
    private const val COPY_LENGTH = 8
    private const val MATCH_FIND_LIMIT = COPY_LENGTH + MIN_MATCH
    private const val MIN_LENGTH = MATCH_FIND_LIMIT + 1
    private const val ML_BITS = 4
    private const val RUN_BITS = 8 - ML_BITS
    private const val RUN_MASK = (1 shl RUN_BITS) - 1
    private const val MAX_DISTANCE = 49152 - 1
    private const val SKIP_TRIGGER = 6 /* Increase this value ==> compression run slower on incompressible data */
    private fun hash(value: Long, mask: Int): Int {
        // Multiplicative hash. It performs the equivalent to
        // this computation:
        //
        //  value * frac(a)
        //
        // for some real number 'a' with a good & random mix
        // of 1s and 0s in its binary representation
        //
        // For performance, it does it using fixed point math
        return (value * 889523592379L ushr 28 and mask.toLong()).toInt()
    }

    fun maxCompressedLength(sourceLength: Int): Int {
        return sourceLength + sourceLength / 255 + 16
    }

    fun compress(
        inputBase: ByteArray,
        inputAddress: Int,
        inputLength: Int,
        outputBase: ByteArray,
        outputAddress: Int,
        maxOutputLength: Int,
        table: IntArray = IntArray(MAX_TABLE_SIZE)
    ): Int {
        val tableSize = computeTableSize(inputLength)
        table.fill(0)
        val mask = tableSize - 1
        require(inputLength <= MAX_INPUT_SIZE) { "Max input length exceeded" }
        require(maxOutputLength >= maxCompressedLength(inputLength)) {
            "Max output length must be larger than " + maxCompressedLength(inputLength)
        }

        // nothing compresses to nothing
        if (inputLength == 0) {
            return 0
        }
        var input = inputAddress
        var output = outputAddress
        val inputLimit = inputAddress + inputLength
        val matchFindLimit = inputLimit - MATCH_FIND_LIMIT
        val matchLimit = inputLimit - LAST_LITERAL_SIZE
        if (inputLength < MIN_LENGTH) {
            output = emitLastLiteral(true, outputBase, output, inputBase, input, inputLimit - input)
            return (output - outputAddress)
        }
        var anchor = input

        // First Byte
        // put position in hash
        table[hash(inputBase.getS64LE(input), mask)] = (input - inputAddress)
        input++
        var nextHash = hash(inputBase.getS64LE(input), mask)
        var done = false
        var firstLiteral = true
        do {
            var nextInputIndex = input
            var findMatchAttempts = 1 shl SKIP_TRIGGER
            var step = 1

            // find 4-byte match
            var matchIndex: Int
            do {
                val hash = nextHash
                input = nextInputIndex
                nextInputIndex += step
                step = findMatchAttempts++ ushr SKIP_TRIGGER
                if (nextInputIndex > matchFindLimit) {
                    output = emitLastLiteral(firstLiteral, outputBase, output, inputBase, anchor, inputLimit - anchor)
                    return (output - outputAddress)
                }

                // get position on hash
                matchIndex = inputAddress + table[hash]
                nextHash = hash(inputBase.getS64LE(nextInputIndex), mask)

                // put position on hash
                table[hash] = (input - inputAddress)
            } while (inputBase.getS32LE(matchIndex) != inputBase.getS32LE(input) || matchIndex + MAX_DISTANCE < input
            )

            // catch up
            while (input > anchor && matchIndex > inputAddress && inputBase.getU8(input - 1) == inputBase.getU8(
                    matchIndex - 1
                )
            ) {
                --input
                --matchIndex
            }
            val literalLength = (input - anchor)
            output = emitLiteral(firstLiteral, inputBase, anchor, outputBase, output, literalLength)
            firstLiteral = false

            // next match
            while (true) {
                val offset = (input - matchIndex)

                // find match length
                input += MIN_MATCH
                val matchLength = count(inputBase, input, matchIndex + MIN_MATCH, matchLimit)
                input += matchLength

                // write copy command
                output = emitCopy(outputBase, output, offset, matchLength + MIN_MATCH)
                anchor = input

                // are we done?
                if (input > matchFindLimit) {
                    done = true
                    break
                }
                val position = input - 2

                table[hash(inputBase.getS64LE(position), mask)] = (position - inputAddress)

                // Test next position
                val hash = hash(inputBase.getS64LE(input), mask)
                matchIndex = inputAddress + table[hash]
                table[hash] = (input - inputAddress)
                if (matchIndex + MAX_DISTANCE < input || inputBase.getS32LE(matchIndex) != inputBase.getS32LE(input)) {
                    input++
                    nextHash = hash(inputBase.getS64LE(input), mask)
                    break
                }

                // go for another match
            }
        } while (!done)

        // Encode Last Literals
        output = emitLastLiteral(false, outputBase, output, inputBase, anchor, inputLimit - anchor)
        return (output - outputAddress)
    }

    private fun count(inputBase: ByteArray, start: Int, matchStart: Int, matchLimit: Int): Int {
        var matchStart = matchStart
        var current = start

        // first, compare long at a time
        while (current < matchLimit - (LzoConstants.SIZE_OF_LONG - 1)) {
            val diff: Long = inputBase.getS64LE(matchStart) xor inputBase.getS64LE(current)
            if (diff != 0L) {
                current += (diff.countTrailingZeroBits() shr 3)
                return (current - start)
            }
            current += LzoConstants.SIZE_OF_LONG
            matchStart += LzoConstants.SIZE_OF_LONG
        }
        if (current < matchLimit - (LzoConstants.SIZE_OF_INT - 1) && inputBase.getU32LE(matchStart) == inputBase.getU32LE(
                current
            )
        ) {
            current += LzoConstants.SIZE_OF_INT
            matchStart += LzoConstants.SIZE_OF_INT
        }
        if (current < matchLimit - (LzoConstants.SIZE_OF_SHORT - 1) && inputBase.getU16LE(matchStart) == inputBase.getU16LE(
                current
            )
        ) {
            current += LzoConstants.SIZE_OF_SHORT
            matchStart += LzoConstants.SIZE_OF_SHORT
        }
        if (current < matchLimit && inputBase.getU8(matchStart) == inputBase.getU8(current)
        ) {
            ++current
        }
        return (current - start)
    }

    private fun emitLastLiteral(
        firstLiteral: Boolean,
        outputBase: ByteArray,
        output: Int,
        inputBase: ByteArray,
        inputAddress: Int,
        literalLength: Int
    ): Int {
        var output = output
        output = encodeLiteralLength(firstLiteral, outputBase, output, literalLength)
        arraycopy(inputBase, inputAddress, outputBase, output, literalLength)
        output += literalLength

        // write stop command
        // this is a 0b0001_HMMM command with a zero match offset
        outputBase.set8(output++, 17)
        outputBase.set16LE(output, 0)
        output += LzoConstants.SIZE_OF_SHORT
        return output
    }

    private fun emitLiteral(
        firstLiteral: Boolean,
        inputBase: ByteArray,
        input: Int,
        outputBase: ByteArray,
        output: Int,
        literalLength: Int
    ): Int {
        var input = input
        var output = output
        output = encodeLiteralLength(firstLiteral, outputBase, output, literalLength)
        val outputLimit = output + literalLength
        do {
            outputBase.set64LE(output, inputBase.getS64LE(input))
            input += LzoConstants.SIZE_OF_LONG
            output += LzoConstants.SIZE_OF_LONG
        } while (output < outputLimit)
        return outputLimit
    }

    private fun encodeLiteralLength(
        firstLiteral: Boolean,
        outBase: ByteArray,
        output: Int,
        length: Int
    ): Int {
        var output = output
        var length = length
        if (firstLiteral && length < 0xFF - 17) {
            outBase.set8(output++, (length + 17))
        } else if (length < 4) {
            // Small literals are encoded in the low two bits trailer of the previous command.  The
            // trailer is a little endian short, so we need to adjust the byte 2 back in the output.
            outBase.set8(output - 2, outBase.getU8(output - 2) or length)
        } else {
            length -= 3
            if (length > RUN_MASK) {
                outBase.set8(output++, 0)
                var remaining = length - RUN_MASK
                while (remaining > 255) {
                    outBase.set8(output++, 0)
                    remaining -= 255
                }
                outBase.set8(output++, remaining)
            } else {
                outBase.set8(output++, length)
            }
        }
        return output
    }

    private fun emitCopy(outputBase: ByteArray, output: Int, matchOffset: Int, matchLength: Int): Int {
        var output = output
        var matchOffset = matchOffset
        var matchLength = matchLength
        require(!(matchOffset > MAX_DISTANCE || matchOffset < 1)) { "Unsupported copy offset: $matchOffset" }

        // use short command for small copy with small offset
        if (matchLength <= 8 && matchOffset <= 2048) {
            // 0bMMMP_PPLL 0bPPPP_PPPP

            // encodes matchLength and matchOffset - 1
            matchLength--
            matchOffset--
            outputBase.set8(output++, (matchLength shl 5 or (matchOffset and 7 shl 2)))
            outputBase.set8(output++, (matchOffset ushr 3))
            return output
        }

        // lzo encodes matchLength - 2
        matchLength -= 2
        if (matchOffset >= 1 shl 15) {
            // 0b0001_1MMM (0bMMMM_MMMM)* 0bPPPP_PPPP_PPPP_PPLL
            output = encodeMatchLength(outputBase, output, matchLength, 7, 24)
        } else if (matchOffset > 1 shl 14) {
            // 0b0001_0MMM (0bMMMM_MMMM)* 0bPPPP_PPPP_PPPP_PPLL
            output = encodeMatchLength(outputBase, output, matchLength, 7, 16)
        } else {
            // 0b001M_MMMM (0bMMMM_MMMM)* 0bPPPP_PPPP_PPPP_PPLL
            output = encodeMatchLength(outputBase, output, matchLength, 31, 32)

            // this command encodes matchOffset - 1
            matchOffset--
        }
        output = encodeOffset(outputBase, output, matchOffset)
        return output
    }

    private fun encodeOffset(outputBase: ByteArray, outputAddress: Int, offset: Int): Int {
        outputBase.set16LE(outputAddress, (offset shl 2))
        return outputAddress + 2
    }

    private fun encodeMatchLength(
        outputBase: ByteArray,
        output: Int,
        matchLength: Int,
        baseMatchLength: Int,
        command: Int
    ): Int {
        var output = output
        if (matchLength <= baseMatchLength) {
            outputBase.set8(output++, command or matchLength)
        } else {
            outputBase.set8(output++, command)
            var remaining = (matchLength - baseMatchLength).toLong()
            while (remaining > 510) {
                outputBase.set16LE(output, 0)
                output += LzoConstants.SIZE_OF_SHORT
                remaining -= 510
            }
            if (remaining > 255) {
                outputBase.set8(output++, 0)
                remaining -= 255
            }
            outputBase.set8(output++, remaining)
        }
        return output
    }

    private fun computeTableSize(inputSize: Int): Int {
        // smallest power of 2 larger than inputSize
        val target: Int = (inputSize - 1).takeHighestOneBit() shl 1

        // keep it between MIN_TABLE_SIZE and MAX_TABLE_SIZE
        return kotlin.math.max(kotlin.math.min(target, MAX_TABLE_SIZE), MIN_TABLE_SIZE)
    }
}
