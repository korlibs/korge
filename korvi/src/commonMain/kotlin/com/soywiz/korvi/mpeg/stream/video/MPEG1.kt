@file:Suppress(
    "unused", "NAME_SHADOWING", "MemberVisibilityCanBePrivate", "ClassName", "FunctionName", "UNUSED_VARIABLE", "UNUSED_PARAMETER",
    "LocalVariableName", "DuplicatedCode", "SpellCheckingInspection"
)

package com.soywiz.korvi.mpeg.stream.video

import com.soywiz.kds.FastArrayList
import com.soywiz.kmem.BaseIntBuffer
import com.soywiz.kmem.Int32Buffer
import com.soywiz.kmem.Uint32Buffer
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.Uint8ClampedBuffer
import com.soywiz.korvi.mpeg.JSMpeg
import com.soywiz.korvi.mpeg.hashArray
import com.soywiz.korvi.mpeg.stream.DecoderBase
import com.soywiz.korvi.mpeg.stream.VideoDestination
import com.soywiz.korvi.mpeg.util.BitBuffer
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

// Inspired by Java MPEG-1 Video Decoder and Player by Zoltan Korandi
// https://sourceforge.net/projects/javampeg1video/
class MPEG1(
    streaming: Boolean,
    val decodeFirstFrame: Boolean = true,
    var bufferSize: Int = 512 * 1024,
    val onDecodeCallback: ((mpeg1: MPEG1, elapsedTime: Double) -> Unit)? = null,
) : DecoderBase<VideoDestination>(streaming) {
    var bufferMode = if (streaming) BitBuffer.MODE.EVICT else BitBuffer.MODE.EXPAND

    override val bits: BitBuffer = BitBuffer(bufferSize, bufferMode)
    val customIntraQuantMatrix = IntArray(64)
    val customNonIntraQuantMatrix = IntArray(64)
    var intraQuantMatrix: IntArray = IntArray(64)
    var nonIntraQuantMatrix: IntArray = IntArray(64)
    val blockData: IntArray = IntArray(64)
    var currentFrame = 0

    var width: Int = 0
    var height: Int = 0

    var mbWidth: Int = 0
    var mbHeight: Int = 0
    var mbSize: Int = 0

    var codedWidth: Int = 0
    var codedHeight: Int = 0
    var codedSize: Int = 0

    var halfWidth: Int = 0
    var halfHeight: Int = 0
    var hasSequenceHeader: Boolean = false

    override fun write(pts: Double, buffers: FastArrayList<Uint8Buffer>) {
        super.write(pts, buffers)

        if (!this.hasSequenceHeader) {
            if (this.bits.findStartCode(START.SEQUENCE) == -1) {
                return
            }
            this.decodeSequenceHeader()

            if (this.decodeFirstFrame) {
                this.decode()
            }
        }
    }

    override fun decode(): Boolean {
        val startTime = JSMpeg.Now()

        if (!this.hasSequenceHeader) {
            return false
        }

        if (this.bits.findStartCode(START.PICTURE) == -1) {
            var bufferedBytes = this.bits.byteLength - (this.bits.index shr 3)
            return false
        }

        this.decodePicture()
        //debug("decodePicture")
        this.advanceDecodedTime(1 / this.frameRate)

        val elapsedTime = JSMpeg.Now() - startTime
        this.onDecodeCallback?.invoke(this, elapsedTime)
        return true
    }

    fun readHuffman(codeTable: IntArray): Int {
        var state = 0
        do {
            state = codeTable[state + this.bits.read(1)]
        } while (state >= 0 && codeTable[state] != 0)
        return codeTable[state + 2]
    }

// Sequence Layer

    fun decodeSequenceHeader() {
        val newWidth = this.bits.read(12)
        val newHeight = this.bits.read(12)

        // skip pixel aspect ratio
        this.bits.skip(4)

        this.frameRate = PICTURE_RATE[this.bits.read(4)]

        // skip bitRate, marker, bufferSize and constrained bit
        this.bits.skip(18 + 1 + 10 + 1)

        if (newWidth != this.width || newHeight != this.height) {
            this.width = newWidth
            this.height = newHeight

            this.initBuffers()

            this.destination?.resize(newWidth, newHeight)
        }

        if (this.bits.readBool()) { // load custom intra quant matrix?
            for (i in 0 until 64) {
                this.customIntraQuantMatrix[ZIG_ZAG[i]] = this.bits.read(8)
            }
            this.intraQuantMatrix = this.customIntraQuantMatrix
        }

        if (this.bits.readBool()) { // load custom non intra quant matrix?
            for (i in 0 until 64) {
                val idx = ZIG_ZAG[i]
                this.customNonIntraQuantMatrix[idx] = this.bits.read(8)
            }
            this.nonIntraQuantMatrix = this.customNonIntraQuantMatrix
        }

        this.hasSequenceHeader = true
    }

    fun initBuffers() {
        this.intraQuantMatrix = DEFAULT_INTRA_QUANT_MATRIX
        this.nonIntraQuantMatrix = DEFAULT_NON_INTRA_QUANT_MATRIX

        this.mbWidth = (this.width + 15) shr 4
        this.mbHeight = (this.height + 15) shr 4
        this.mbSize = this.mbWidth * this.mbHeight

        this.codedWidth = this.mbWidth shl 4
        this.codedHeight = this.mbHeight shl 4
        this.codedSize = this.codedWidth * this.codedHeight

        this.halfWidth = this.mbWidth shl 3
        this.halfHeight = this.mbHeight shl 3

        // Allocated buffers and resize the canvas
        this.currentY = Uint8ClampedBuffer(this.codedSize)
        this.currentY32 = Uint32Buffer(this.currentY!!.buffer)

        this.currentCr = Uint8ClampedBuffer(this.codedSize shr 2)
        this.currentCr32 = Uint32Buffer(this.currentCr!!.buffer)

        this.currentCb = Uint8ClampedBuffer(this.codedSize shr 2)
        this.currentCb32 = Uint32Buffer(this.currentCb!!.buffer)


        this.forwardY = Uint8ClampedBuffer(this.codedSize)
        this.forwardY32 = Uint32Buffer(this.forwardY!!.buffer)

        this.forwardCr = Uint8ClampedBuffer(this.codedSize shr 2)
        this.forwardCr32 = Uint32Buffer(this.forwardCr!!.buffer)

        this.forwardCb = Uint8ClampedBuffer(this.codedSize shr 2)
        this.forwardCb32 = Uint32Buffer(this.forwardCb!!.buffer)
    }

    // Picture Layer
    var frameRate = 30.0

    var currentY: Uint8ClampedBuffer? = null
    var currentCr: Uint8ClampedBuffer? = null
    var currentCb: Uint8ClampedBuffer? = null
    var currentY32: Uint32Buffer? = null
    var currentCr32: Uint32Buffer? = null
    var currentCb32: Uint32Buffer? = null

    var pictureType = 0

    // Buffers for motion compensation
    var forwardY: Uint8ClampedBuffer? = null
    var forwardCr: Uint8ClampedBuffer? = null
    var forwardCb: Uint8ClampedBuffer? = null
    var forwardY32: Uint32Buffer? = null
    var forwardCr32: Uint32Buffer? = null
    var forwardCb32: Uint32Buffer? = null

    var fullPelForward = false
    var forwardFCode = 0
    var forwardRSize = 0
    var forwardF = 0

    fun debug(pass: String, vararg values: Any?) {
        println(buildString {
            append("$pass ")
            for (value in values) {
                //append("\n  ")
                var rvalue = value
                if (rvalue is KProperty<*>) append(rvalue.name + "=")
                if (rvalue is KProperty0<*>) rvalue = rvalue.get()

                //if (rvalue is BaseIntBuffer) rvalue = "Array[${rvalue.size}]=hash:${hashArray(rvalue)}"
                //if (rvalue is IntArray) rvalue = "Array[${rvalue.size}]=hash:${hashArray(rvalue)}"
                //if (rvalue is Int32Buffer) rvalue = "Array[${rvalue.size}]=hash:${hashArray(rvalue)}"
                //if (rvalue is Uint32Buffer) rvalue = "Array[${rvalue.size}]=hash:${hashArray(rvalue)}"

                if (rvalue is BaseIntBuffer) rvalue = "${hashArray(rvalue)}"
                if (rvalue is IntArray) rvalue = "${hashArray(rvalue)}"
                if (rvalue is Int32Buffer) rvalue = "${hashArray(rvalue)}"
                if (rvalue is Uint32Buffer) rvalue = "${hashArray(rvalue)}"


                append(rvalue)
                append(", ")
            }
        })
    }

    fun debug(pass: String) {
        debug(
            "MPEG1.debug[$pass]", ::currentY, customIntraQuantMatrix, customNonIntraQuantMatrix,
            intraQuantMatrix, nonIntraQuantMatrix, blockData,

        )
    }

    fun decodePicture(skipOutput: Boolean = false) {
        this.currentFrame++

        this.bits.skip(10) // skip temporalReference
        this.pictureType = this.bits.read(3)
        this.bits.skip(16) // skip vbv_delay

        // Skip B and D frames or unknown coding type
        if (this.pictureType <= 0 || this.pictureType >= PICTURE_TYPE.B) {
            return
        }

        // full_pel_forward, forward_f_code
        if (this.pictureType == PICTURE_TYPE.PREDICTIVE) {
            this.fullPelForward = this.bits.readBool()
            this.forwardFCode = this.bits.read(3)
            if (this.forwardFCode == 0) {
                // Ignore picture with zero forward_f_code
                return
            }
            this.forwardRSize = this.forwardFCode - 1
            this.forwardF = 1 shl this.forwardRSize
        }

        var code: Int
        do {
            code = this.bits.findNextStartCode()
        } while (code == START.EXTENSION || code == START.USER_DATA)


        while (code >= START.SLICE_FIRST && code <= START.SLICE_LAST) {
            this.decodeSlice(code and 0x000000FF)
            code = this.bits.findNextStartCode()
        }

        if (code != -1) {
            // We found the next start code; rewind 32bits and let the main loop
            // handle it.
            this.bits.rewind(32)
        }

        // Invoke decode callbacks
        this.destination?.render(this.currentY!!, this.currentCr!!, this.currentCb!!, true)

        // If this is a reference picutre then rotate the prediction pointers
        if (
            this.pictureType == PICTURE_TYPE.INTRA ||
            this.pictureType == PICTURE_TYPE.PREDICTIVE
        ) {
            val tmpY = this.forwardY
            val tmpY32 = this.forwardY32
            val tmpCr = this.forwardCr
            val tmpCr32 = this.forwardCr32
            val tmpCb = this.forwardCb
            val tmpCb32 = this.forwardCb32

            this.forwardY = this.currentY
            this.forwardY32 = this.currentY32
            this.forwardCr = this.currentCr
            this.forwardCr32 = this.currentCr32
            this.forwardCb = this.currentCb
            this.forwardCb32 = this.currentCb32

            this.currentY = tmpY
            this.currentY32 = tmpY32
            this.currentCr = tmpCr
            this.currentCr32 = tmpCr32
            this.currentCb = tmpCb
            this.currentCb32 = tmpCb32
        }
    }

// Slice Layer

    var quantizerScale = 0
    var sliceBegin = false

    fun decodeSlice(slice: Int) {
        this.sliceBegin = true
        this.macroblockAddress = (slice - 1) * this.mbWidth - 1

        // Reset motion vectors and DC predictors
        this.motionFwH = 0
        this.motionFwHPrev = 0
        this.motionFwV = 0
        this.motionFwVPrev = 0
        this.dcPredictorY = 128
        this.dcPredictorCr = 128
        this.dcPredictorCb = 128

        this.quantizerScale = this.bits.read(5)

        // skip extra bits
        while (this.bits.readBool()) {
            this.bits.skip(8)
        }

        //var macroBlockCount = 0
        //var line = ""
        do {
            this.decodeMacroblock()
            //line += "${hashArray(currentY!!)}, "

            //macroBlockCount++
        } while (!this.bits.nextBytesAreStartCode())
        if (this.decodeSliceCount == 63) {
            //println("decodeSlice[${decodeSliceCount}]($macroBlockCount) : $line")
        }

        //debug("decodeSlice[${decodeSliceCount}]: macroBlockCount=$macroBlockCount")
        decodeSliceCount++
    }

    var decodeSliceCount = 0

// Macroblock Layer

    var macroblockAddress = 0
    var mbRow = 0
    var mbCol = 0

    var macroblockType = 0
    var macroblockIntra = false
    var macroblockMotFw = false

    var motionFwH = 0
    var motionFwV = 0
    var motionFwHPrev = 0
    var motionFwVPrev = 0

    fun decodeMacroblock() {
        if (this.decodeSliceCount == 63) {
            //println("------------")
        }
        // Decode macroblock_address_increment
        var increment = 0
        var t = this.readHuffman(MACROBLOCK_ADDRESS_INCREMENT)

        while (t == 34) {
            // macroblock_stuffing
            t = this.readHuffman(MACROBLOCK_ADDRESS_INCREMENT)
        }
        while (t == 35) {
            // macroblock_escape
            increment += 33
            t = this.readHuffman(MACROBLOCK_ADDRESS_INCREMENT)
        }
        increment += t

        // Process any skipped macroblocks
        if (this.sliceBegin) {
            // The first macroblock_address_increment of each slice is relative
            // to beginning of the preverious row, not the preverious macroblock
            this.sliceBegin = false
            this.macroblockAddress += increment
        } else {
            if (this.macroblockAddress + increment >= this.mbSize) {
                // Illegal (too large) macroblock_address_increment
                return
            }
            if (increment > 1) {
                // Skipped macroblocks reset DC predictors
                this.dcPredictorY = 128
                this.dcPredictorCr = 128
                this.dcPredictorCb = 128

                // Skipped macroblocks in P-pictures reset motion vectors
                if (this.pictureType == PICTURE_TYPE.PREDICTIVE) {
                    this.motionFwH = 0
                    this.motionFwHPrev = 0
                    this.motionFwV = 0
                    this.motionFwVPrev = 0
                }
            }

            // Predict skipped macroblocks
            while (increment > 1) {
                this.macroblockAddress++
                this.mbRow = (this.macroblockAddress / this.mbWidth)
                this.mbCol = this.macroblockAddress % this.mbWidth
                this.copyMacroblock(
                    this.motionFwH, this.motionFwV,
                    this.forwardY!!, this.forwardCr!!, this.forwardCb!!
                )
                increment--
            }
            this.macroblockAddress++
        }
        this.mbRow = (this.macroblockAddress / this.mbWidth)
        this.mbCol = this.macroblockAddress % this.mbWidth

        // Process the current macroblock
        val mbTable = MACROBLOCK_TYPE[this.pictureType]
        this.macroblockType = this.readHuffman(mbTable!!)
        this.macroblockIntra = (this.macroblockType and 0x01) != 0
        this.macroblockMotFw = (this.macroblockType and 0x08) != 0

        // Quantizer scale
        if ((this.macroblockType and 0x10) != 0) {
            this.quantizerScale = this.bits.read(5)
        }

        if (this.macroblockIntra) {
            // Intra-coded macroblocks reset motion vectors
            this.motionFwH = 0
            this.motionFwHPrev = 0
            this.motionFwV = 0
            this.motionFwVPrev = 0
        } else {
            // Non-intra macroblocks reset DC predictors
            this.dcPredictorY = 128
            this.dcPredictorCr = 128
            this.dcPredictorCb = 128

            this.decodeMotionVectors()
            this.copyMacroblock(
                this.motionFwH, this.motionFwV,
                this.forwardY!!, this.forwardCr!!, this.forwardCb!!
            )
        }

        // Decode blocks
        val cbp: Int = when {
            (this.macroblockType and 0x02) != 0 -> this.readHuffman(CODE_BLOCK_PATTERN)
            this.macroblockIntra -> 0x3f
            else -> 0
        }

        var mask = 0x20
        for (block in 0 until 6) {
            if ((cbp and mask) != 0) {
                this.decodeBlock(block)
            }
            mask = mask shr 1
        }
    }

    fun decodeMotionVectors() {
        var d: Int
        var r: Int

        // Forward
        if (this.macroblockMotFw) {
            // Horizontal forward
            var code = this.readHuffman(MOTION)
            if ((code != 0) && (this.forwardF != 1)) {
                r = this.bits.read(this.forwardRSize)
                d = ((kotlin.math.abs(code) - 1) shl this.forwardRSize) + r + 1
                if (code < 0) {
                    d = -d
                }
            } else {
                d = code
            }

            this.motionFwHPrev += d
            if (this.motionFwHPrev > (this.forwardF shl 4) - 1) {
                this.motionFwHPrev -= this.forwardF shl 5
            } else if (this.motionFwHPrev < ((-this.forwardF) shl 4)) {
                this.motionFwHPrev += this.forwardF shl 5
            }

            this.motionFwH = this.motionFwHPrev
            if (this.fullPelForward) {
                this.motionFwH = this.motionFwH shl 1
            }

            // Vertical forward
            code = this.readHuffman(MOTION)
            if ((code != 0) && (this.forwardF != 1)) {
                r = this.bits.read(this.forwardRSize)
                d = ((kotlin.math.abs(code) - 1) shl this.forwardRSize) + r + 1
                if (code < 0) {
                    d = -d
                }
            } else {
                d = code
            }

            this.motionFwVPrev += d
            if (this.motionFwVPrev > (this.forwardF shl 4) - 1) {
                this.motionFwVPrev -= this.forwardF shl 5
            } else if (this.motionFwVPrev < ((-this.forwardF) shl 4)) {
                this.motionFwVPrev += this.forwardF shl 5
            }

            this.motionFwV = this.motionFwVPrev
            if (this.fullPelForward) {
                this.motionFwV = this.motionFwV shl 1
            }
        } else if (this.pictureType == PICTURE_TYPE.PREDICTIVE) {
            // No motion information in P-picture, reset vectors
            this.motionFwH = 0
            this.motionFwHPrev = 0
            this.motionFwV = 0
            this.motionFwVPrev = 0
        }
    }

    fun copyMacroblock(motionH: Int, motionV: Int, sY: Uint8ClampedBuffer, sCr: Uint8ClampedBuffer, sCb: Uint8ClampedBuffer) {
        // We use 32bit writes here
        val dY = this.currentY32!!
        val dCb = this.currentCb32!!
        val dCr = this.currentCr32!!

        // Luminance
        run {
            val width = this.codedWidth
            val scan = width - 16

            val H = motionH shr 1
            val V = motionV shr 1
            val oddH = (motionH and 1) == 1
            val oddV = (motionV and 1) == 1

            var src = ((this.mbRow shl 4) + V) * width + (this.mbCol shl 4) + H
            var dest = (this.mbRow * width + this.mbCol) shl 2
            val last = dest + (width shl 2)

            if (oddH) {
                if (oddV) {
                    while (dest < last) {
                        var y1 = sY[src] + sY[src + width]; src++
                        for (x in 0 until 4) {
                            var y2 = sY[src] + sY[src + width]; src++
                            var y = (((y1 + y2 + 2) shr 2) and 0xff)

                            y1 = sY[src] + sY[src + width]; src++
                            y = y or (((y1 + y2 + 2) shl 6) and 0xff00)

                            y2 = sY[src] + sY[src + width]; src++
                            y = y or (((y1 + y2 + 2) shl 14) and 0xff0000)

                            y1 = sY[src] + sY[src + width]; src++
                            y = y or (((y1 + y2 + 2) shl 22) and 0xff000000.toInt())

                            dY[dest++] = y
                        }
                        dest += scan shr 2
                        src += scan - 1
                    }
                } else {
                    while (dest < last) {
                        var y1 = sY[src++]
                        for (x in 0 until 4) {
                            var y2 = sY[src++]
                            var y = (((y1 + y2 + 1) shr 1) and 0xff)

                            y1 = sY[src++]
                            y = y or (((y1 + y2 + 1) shl 7) and 0xff00)

                            y2 = sY[src++]
                            y = y or (((y1 + y2 + 1) shl 15) and 0xff0000)

                            y1 = sY[src++]
                            y = y or (((y1 + y2 + 1) shl 23) and 0xff000000.toInt())

                            dY[dest++] = y
                        }
                        dest += scan shr 2
                        src += scan - 1
                    }
                }
            } else {
                if (oddV) {
                    while (dest < last) {
                        for (x in 0 until 4) {
                            var y = (((sY[src] + sY[src + width] + 1) shr 1) and 0xff); src++
                            y = y or (((sY[src] + sY[src + width] + 1) shl 7) and 0xff00); src++
                            y = y or (((sY[src] + sY[src + width] + 1) shl 15) and 0xff0000); src++
                            y = y or (((sY[src] + sY[src + width] + 1) shl 23) and 0xff000000.toInt()); src++

                            dY[dest++] = y
                        }
                        dest += scan shr 2
                        src += scan
                    }
                } else {
                    while (dest < last) {
                        for (x in 0 until 4) {
                            var y = sY[src]; src++
                            y = y or (sY[src] shl 8); src++
                            y = y or (sY[src] shl 16); src++
                            y = y or (sY[src] shl 24); src++

                            dY[dest++] = y
                        }
                        dest += scan shr 2
                        src += scan
                    }
                }
            }
        }

        // Chrominance
        run {
            val width = this.halfWidth
            val scan = width - 8

            val H = (motionH / 2) shr 1
            val V = (motionV / 2) shr 1
            val oddH = ((motionH / 2) and 1) == 1
            val oddV = ((motionV / 2) and 1) == 1

            var src = ((this.mbRow shl 3) + V) * width + (this.mbCol shl 3) + H
            var dest = (this.mbRow * width + this.mbCol) shl 1
            val last = dest + (width shl 1)

            if (oddH) {
                if (oddV) {
                    while (dest < last) {
                        var cr1 = sCr[src] + sCr[src + width]
                        var cb1 = sCb[src] + sCb[src + width]
                        src++
                        for (x in 0 until 2) {
                            var cr2 = sCr[src] + sCr[src + width]
                            var cb2 = sCb[src] + sCb[src + width]; src++
                            var cr = (((cr1 + cr2 + 2) shr 2) and 0xff)
                            var cb = (((cb1 + cb2 + 2) shr 2) and 0xff)

                            cr1 = sCr[src] + sCr[src + width]
                            cb1 = sCb[src] + sCb[src + width]; src++
                            cr = cr or (((cr1 + cr2 + 2) shl 6) and 0xff00)
                            cb = cb or (((cb1 + cb2 + 2) shl 6) and 0xff00)

                            cr2 = sCr[src] + sCr[src + width]
                            cb2 = sCb[src] + sCb[src + width]; src++
                            cr = cr or (((cr1 + cr2 + 2) shl 14) and 0xff0000)
                            cb = cb or (((cb1 + cb2 + 2) shl 14) and 0xff0000)

                            cr1 = sCr[src] + sCr[src + width]
                            cb1 = sCb[src] + sCb[src + width]; src++
                            cr = cr or (((cr1 + cr2 + 2) shl 22) and 0xff000000.toInt())
                            cb = cb or (((cb1 + cb2 + 2) shl 22) and 0xff000000.toInt())

                            dCr[dest] = cr
                            dCb[dest] = cb
                            dest++
                        }
                        dest += scan shr 2
                        src += scan - 1
                    }
                } else {
                    while (dest < last) {
                        var cr1 = sCr[src]
                        var cb1 = sCb[src]
                        src++
                        for (x in 0 until 2) {
                            var cr2 = sCr[src]
                            var cb2 = sCb[src++]
                            var cr = (((cr1 + cr2 + 1) shr 1) and 0xff)
                            var cb = (((cb1 + cb2 + 1) shr 1) and 0xff)

                            cr1 = sCr[src]
                            cb1 = sCb[src++]
                            cr = cr or (((cr1 + cr2 + 1) shl 7) and 0xff00)
                            cb = cb or (((cb1 + cb2 + 1) shl 7) and 0xff00)

                            cr2 = sCr[src]
                            cb2 = sCb[src++]
                            cr = cr or (((cr1 + cr2 + 1) shl 15) and 0xff0000)
                            cb = cb or (((cb1 + cb2 + 1) shl 15) and 0xff0000)

                            cr1 = sCr[src]
                            cb1 = sCb[src++]
                            cr = cr or (((cr1 + cr2 + 1) shl 23) and 0xff000000.toInt())
                            cb = cb or (((cb1 + cb2 + 1) shl 23) and 0xff000000.toInt())

                            dCr[dest] = cr
                            dCb[dest] = cb
                            dest++
                        }
                        dest += scan shr 2
                        src += scan - 1
                    }
                }
            } else {
                if (oddV) {
                    while (dest < last) {
                        for (x in 0 until 2) {
                            var cr = (((sCr[src] + sCr[src + width] + 1) shr 1) and 0xff)
                            var cb = (((sCb[src] + sCb[src + width] + 1) shr 1) and 0xff); src++

                            cr = cr or (((sCr[src] + sCr[src + width] + 1) shl 7) and 0xff00)
                            cb = cb or (((sCb[src] + sCb[src + width] + 1) shl 7) and 0xff00); src++

                            cr = cr or (((sCr[src] + sCr[src + width] + 1) shl 15) and 0xff0000)
                            cb = cb or (((sCb[src] + sCb[src + width] + 1) shl 15) and 0xff0000); src++

                            cr = cr or (((sCr[src] + sCr[src + width] + 1) shl 23) and 0xff000000.toInt())
                            cb = cb or (((sCb[src] + sCb[src + width] + 1) shl 23) and 0xff000000.toInt()); src++

                            dCr[dest] = cr
                            dCb[dest] = cb
                            dest++
                        }
                        dest += scan shr 2
                        src += scan
                    }
                } else {
                    while (dest < last) {
                        for (x in 0 until 2) {
                            var cr = sCr[src]
                            var cb = sCb[src]; src++

                            cr = cr or (sCr[src] shl 8)
                            cb = cb or (sCb[src] shl 8); src++
                            cr = cr or (sCr[src] shl 16)
                            cb = cb or (sCb[src] shl 16); src++
                            cr = cr or (sCr[src] shl 24)
                            cb = cb or (sCb[src] shl 24); src++

                            dCr[dest] = cr
                            dCb[dest] = cb
                            dest++
                        }
                        dest += scan shr 2
                        src += scan
                    }
                }
            }
        }
        //debug("copyMacroblock")
    }

    // Block layer

    var dcPredictorY = 0
    var dcPredictorCr = 0
    var dcPredictorCb = 0

    fun decodeBlock(block: Int) {
        if (this.decodeSliceCount == 63) {
            //println("------------")
        }

        var n = 0

        // Decode DC coefficient of intra-coded blocks
        val quantMatrix = if (this.macroblockIntra) {
            val predictor: Int
            val dctSize: Int

            // DC prediction

            if (block < 4) {
                predictor = this.dcPredictorY
                dctSize = this.readHuffman(DCT_DC_SIZE_LUMINANCE)
            } else {
                predictor = if (block == 4) this.dcPredictorCr else this.dcPredictorCb
                dctSize = this.readHuffman(DCT_DC_SIZE_CHROMINANCE)
            }

            // Read DC coeff
            if (dctSize > 0) {
                val differential = this.bits.read(dctSize)
                if ((differential and (1 shl (dctSize - 1))) != 0) {
                    this.blockData[0] = predictor + differential
                } else {
                    this.blockData[0] = predictor + (((-1) shl dctSize) or (differential + 1))
                }
            } else {
                this.blockData[0] = predictor
            }

            // Save predictor value
            if (block < 4) {
                this.dcPredictorY = this.blockData[0]
            } else if (block == 4) {
                this.dcPredictorCr = this.blockData[0]
            } else {
                this.dcPredictorCb = this.blockData[0]
            }

            // Dequantize + premultiply
            this.blockData[0] = this.blockData[0] shl (3 + 5)

            n = 1
            this.intraQuantMatrix
        } else {
            this.nonIntraQuantMatrix
        }

        // Decode AC coefficients (+DC for non-intra)
        var level: Int
        while (true) {
            var run: Int
            val coeff = this.readHuffman(DCT_COEFF)

            if ((coeff == 0x0001) && (n > 0) && (this.bits.read(1) == 0)) {
                // end_of_block
                break
            }
            if (coeff == 0xffff) {
                // escape
                run = this.bits.read(6)
                level = this.bits.read(8)
                level = when {
                    level == 0 -> this.bits.read(8)
                    level == 128 -> this.bits.read(8) - 256
                    level > 128 -> level - 256
                    else -> level
                }
            } else {
                run = coeff shr 8
                level = coeff and 0xff
                if (this.bits.readBool()) {
                    level = -level
                }
            }

            n += run
            val dezigZagged = ZIG_ZAG[n]
            n++

            // Dequantize, oddify, clip
            level = level shl 1
            if (!this.macroblockIntra) {
                level += if (level < 0) -1 else 1
            }
            if (this.decodeSliceCount >= 63) {
                //println("---------")
            }
            level = (level * this.quantizerScale * quantMatrix[dezigZagged]) shr 4
            if ((level and 1) == 0) {
                level -= if (level > 0) 1 else -1
            }
            if (level > 2047) {
                level = 2047
            } else if (level < -2048) {
                level = -2048
            }

            // Save premultiplied coefficient
            this.blockData[dezigZagged] = level * PREMULTIPLIER_MATRIX[dezigZagged]
        }

        // Move block to its place
        val destArray: Uint8ClampedBuffer
        var destIndex: Int
        val scan: Int

        if (block < 4) {
            destArray = this.currentY!!
            scan = this.codedWidth - 8
            destIndex = (this.mbRow * this.codedWidth + this.mbCol) shl 4
            if ((block and 1) != 0) {
                destIndex += 8
            }
            if ((block and 2) != 0) {
                destIndex += this.codedWidth shl 3
            }
        } else {
            destArray = if (block == 4) this.currentCb!! else this.currentCr!!
            scan = (this.codedWidth shr 1) - 8
            destIndex = ((this.mbRow * this.codedWidth) shl 2) + (this.mbCol shl 3)
        }

        if (this.macroblockIntra) {
            // Overwrite (no prediction)
            if (n == 1) {
                CopyValueToDestination((this.blockData[0] + 128) shr 8, destArray, destIndex, scan)
                this.blockData[0] = 0
            } else {
                IDCT(this.blockData)
                CopyBlockToDestination(this.blockData, destArray, destIndex, scan)
                this.blockData.fill(0)
            }
        } else {
            // Add data to the predicted macroblock
            if (n == 1) {
                AddValueToDestination((this.blockData[0] + 128) shr 8, destArray, destIndex, scan)
                this.blockData[0] = 0
            } else {
                IDCT(this.blockData)
                AddBlockToDestination(this.blockData, destArray, destIndex, scan)
                this.blockData.fill(0)
            }
        }
    }

    companion object {
        fun CopyBlockToDestination(block: IntArray, dest: Uint8ClampedBuffer, index: Int, scan: Int) {
            var index = index
            var n = 0
            while (n < 64) {
                dest[index + 0] = block[n + 0]
                dest[index + 1] = block[n + 1]
                dest[index + 2] = block[n + 2]
                dest[index + 3] = block[n + 3]
                dest[index + 4] = block[n + 4]
                dest[index + 5] = block[n + 5]
                dest[index + 6] = block[n + 6]
                dest[index + 7] = block[n + 7]
                index += scan + 8
                n += 8
            }
        }

        fun AddBlockToDestination(block: IntArray, dest: Uint8ClampedBuffer, index: Int, scan: Int) {
            var index = index
            var n = 0
            while (n < 64) {
                dest[index + 0] += block[n + 0]
                dest[index + 1] += block[n + 1]
                dest[index + 2] += block[n + 2]
                dest[index + 3] += block[n + 3]
                dest[index + 4] += block[n + 4]
                dest[index + 5] += block[n + 5]
                dest[index + 6] += block[n + 6]
                dest[index + 7] += block[n + 7]
                index += scan + 8
                n += 8
            }
        }

        fun CopyValueToDestination(value: Int, dest: Uint8ClampedBuffer, index: Int, scan: Int) {
            var index = index
            var n = 0
            while (n < 64) {
                dest[index + 0] = value
                dest[index + 1] = value
                dest[index + 2] = value
                dest[index + 3] = value
                dest[index + 4] = value
                dest[index + 5] = value
                dest[index + 6] = value
                dest[index + 7] = value
                index += scan + 8
                n += 8
            }
        }

        fun AddValueToDestination(value: Int, dest: Uint8ClampedBuffer, index: Int, scan: Int) {
            var index = index
            var n = 0
            while (n < 64) {
                dest[index + 0] += value
                dest[index + 1] += value
                dest[index + 2] += value
                dest[index + 3] += value
                dest[index + 4] += value
                dest[index + 5] += value
                dest[index + 6] += value
                dest[index + 7] += value
                index += scan + 8
                n += 8
            }
        }

        fun IDCT(block: IntArray) {
            // See http://vsr.informatik.tu-chemnitz.de/~jan/MPEG/HTML/IDCT.html
            // for more info.

            // Transform columns
            for (i in 0 until 8) {
                val b1 = block[4 * 8 + i]
                val b3 = block[2 * 8 + i] + block[6 * 8 + i]
                val b4 = block[5 * 8 + i] - block[3 * 8 + i]
                val tmp1 = block[1 * 8 + i] + block[7 * 8 + i]
                val tmp2 = block[3 * 8 + i] + block[5 * 8 + i]
                val b6 = block[1 * 8 + i] - block[7 * 8 + i]
                val b7 = tmp1 + tmp2
                val m0 = block[0 * 8 + i]
                val x4 = ((b6 * 473 - b4 * 196 + 128) shr 8) - b7
                val x0 = x4 - (((tmp1 - tmp2) * 362 + 128) shr 8)
                val x1 = m0 - b1
                val x2 = (((block[2 * 8 + i] - block[6 * 8 + i]) * 362 + 128) shr 8) - b3
                val x3 = m0 + b1
                val y3 = x1 + x2
                val y4 = x3 + b3
                val y5 = x1 - x2
                val y6 = x3 - b3
                val y7 = -x0 - ((b4 * 473 + b6 * 196 + 128) shr 8)
                block[0 * 8 + i] = b7 + y4
                block[1 * 8 + i] = x4 + y3
                block[2 * 8 + i] = y5 - x0
                block[3 * 8 + i] = y6 - y7
                block[4 * 8 + i] = y6 + y7
                block[5 * 8 + i] = x0 + y5
                block[6 * 8 + i] = y3 - x4
                block[7 * 8 + i] = y4 - b7
            }

            // Transform rows
            var i = 0
            while (i < 64) {
                val b1 = block[4 + i]
                val b3 = block[2 + i] + block[6 + i]
                val b4 = block[5 + i] - block[3 + i]
                val tmp1 = block[1 + i] + block[7 + i]
                val tmp2 = block[3 + i] + block[5 + i]
                val b6 = block[1 + i] - block[7 + i]
                val b7 = tmp1 + tmp2
                val m0 = block[0 + i]
                val x4 = ((b6 * 473 - b4 * 196 + 128) shr 8) - b7
                val x0 = x4 - (((tmp1 - tmp2) * 362 + 128) shr 8)
                val x1 = m0 - b1
                val x2 = (((block[2 + i] - block[6 + i]) * 362 + 128) shr 8) - b3
                val x3 = m0 + b1
                val y3 = x1 + x2
                val y4 = x3 + b3
                val y5 = x1 - x2
                val y6 = x3 - b3
                val y7 = -x0 - ((b4 * 473 + b6 * 196 + 128) shr 8)
                block[0 + i] = (b7 + y4 + 128) shr 8
                block[1 + i] = (x4 + y3 + 128) shr 8
                block[2 + i] = (y5 - x0 + 128) shr 8
                block[3 + i] = (y6 - y7 + 128) shr 8
                block[4 + i] = (y6 + y7 + 128) shr 8
                block[5 + i] = (x0 + y5 + 128) shr 8
                block[6 + i] = (y3 - x4 + 128) shr 8
                block[7 + i] = (y4 - b7 + 128) shr 8
                i += 8
            }
        }

        // VLC Tables and Constants

        val PICTURE_RATE = doubleArrayOf(
            0.000, 23.976, 24.000, 25.000, 29.970, 30.000, 50.000, 59.940,
            60.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000
        )

        val ZIG_ZAG = intArrayOf(
            0, 1, 8, 16, 9, 2, 3, 10,
            17, 24, 32, 25, 18, 11, 4, 5,
            12, 19, 26, 33, 40, 48, 41, 34,
            27, 20, 13, 6, 7, 14, 21, 28,
            35, 42, 49, 56, 57, 50, 43, 36,
            29, 22, 15, 23, 30, 37, 44, 51,
            58, 59, 52, 45, 38, 31, 39, 46,
            53, 60, 61, 54, 47, 55, 62, 63
        )

        val DEFAULT_INTRA_QUANT_MATRIX = intArrayOf(
            8, 16, 19, 22, 26, 27, 29, 34,
            16, 16, 22, 24, 27, 29, 34, 37,
            19, 22, 26, 27, 29, 34, 34, 38,
            22, 22, 26, 27, 29, 34, 37, 40,
            22, 26, 27, 29, 32, 35, 40, 48,
            26, 27, 29, 32, 35, 40, 48, 58,
            26, 27, 29, 34, 38, 46, 56, 69,
            27, 29, 35, 38, 46, 56, 69, 83
        )

        val DEFAULT_NON_INTRA_QUANT_MATRIX = intArrayOf(
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16
        )

        val PREMULTIPLIER_MATRIX = intArrayOf(
            32, 44, 42, 38, 32, 25, 17, 9,
            44, 62, 58, 52, 44, 35, 24, 12,
            42, 58, 55, 49, 42, 33, 23, 12,
            38, 52, 49, 44, 38, 30, 20, 10,
            32, 44, 42, 38, 32, 25, 17, 9,
            25, 35, 33, 30, 25, 20, 14, 7,
            17, 24, 23, 20, 17, 14, 9, 5,
            9, 12, 12, 10, 9, 7, 5, 2
        )

        // MPEG-1 VLC

        //  macroblock_stuffing decodes as 34.
        //  macroblock_escape decodes as 35.

        val MACROBLOCK_ADDRESS_INCREMENT = intArrayOf(
            1 * 3, 2 * 3, 0, //   0
            3 * 3, 4 * 3, 0, //   1  0
            0, 0, 1, //   2  1.
            5 * 3, 6 * 3, 0, //   3  00
            7 * 3, 8 * 3, 0, //   4  01
            9 * 3, 10 * 3, 0, //   5  000
            11 * 3, 12 * 3, 0, //   6  001
            0, 0, 3, //   7  010.
            0, 0, 2, //   8  011.
            13 * 3, 14 * 3, 0, //   9  0000
            15 * 3, 16 * 3, 0, //  10  0001
            0, 0, 5, //  11  0010.
            0, 0, 4, //  12  0011.
            17 * 3, 18 * 3, 0, //  13  0000 0
            19 * 3, 20 * 3, 0, //  14  0000 1
            0, 0, 7, //  15  0001 0.
            0, 0, 6, //  16  0001 1.
            21 * 3, 22 * 3, 0, //  17  0000 00
            23 * 3, 24 * 3, 0, //  18  0000 01
            25 * 3, 26 * 3, 0, //  19  0000 10
            27 * 3, 28 * 3, 0, //  20  0000 11
            -1, 29 * 3, 0, //  21  0000 000
            -1, 30 * 3, 0, //  22  0000 001
            31 * 3, 32 * 3, 0, //  23  0000 010
            33 * 3, 34 * 3, 0, //  24  0000 011
            35 * 3, 36 * 3, 0, //  25  0000 100
            37 * 3, 38 * 3, 0, //  26  0000 101
            0, 0, 9, //  27  0000 110.
            0, 0, 8, //  28  0000 111.
            39 * 3, 40 * 3, 0, //  29  0000 0001
            41 * 3, 42 * 3, 0, //  30  0000 0011
            43 * 3, 44 * 3, 0, //  31  0000 0100
            45 * 3, 46 * 3, 0, //  32  0000 0101
            0, 0, 15, //  33  0000 0110.
            0, 0, 14, //  34  0000 0111.
            0, 0, 13, //  35  0000 1000.
            0, 0, 12, //  36  0000 1001.
            0, 0, 11, //  37  0000 1010.
            0, 0, 10, //  38  0000 1011.
            47 * 3, -1, 0, //  39  0000 0001 0
            -1, 48 * 3, 0, //  40  0000 0001 1
            49 * 3, 50 * 3, 0, //  41  0000 0011 0
            51 * 3, 52 * 3, 0, //  42  0000 0011 1
            53 * 3, 54 * 3, 0, //  43  0000 0100 0
            55 * 3, 56 * 3, 0, //  44  0000 0100 1
            57 * 3, 58 * 3, 0, //  45  0000 0101 0
            59 * 3, 60 * 3, 0, //  46  0000 0101 1
            61 * 3, -1, 0, //  47  0000 0001 00
            -1, 62 * 3, 0, //  48  0000 0001 11
            63 * 3, 64 * 3, 0, //  49  0000 0011 00
            65 * 3, 66 * 3, 0, //  50  0000 0011 01
            67 * 3, 68 * 3, 0, //  51  0000 0011 10
            69 * 3, 70 * 3, 0, //  52  0000 0011 11
            71 * 3, 72 * 3, 0, //  53  0000 0100 00
            73 * 3, 74 * 3, 0, //  54  0000 0100 01
            0, 0, 21, //  55  0000 0100 10.
            0, 0, 20, //  56  0000 0100 11.
            0, 0, 19, //  57  0000 0101 00.
            0, 0, 18, //  58  0000 0101 01.
            0, 0, 17, //  59  0000 0101 10.
            0, 0, 16, //  60  0000 0101 11.
            0, 0, 35, //  61  0000 0001 000. -- macroblock_escape
            0, 0, 34, //  62  0000 0001 111. -- macroblock_stuffing
            0, 0, 33, //  63  0000 0011 000.
            0, 0, 32, //  64  0000 0011 001.
            0, 0, 31, //  65  0000 0011 010.
            0, 0, 30, //  66  0000 0011 011.
            0, 0, 29, //  67  0000 0011 100.
            0, 0, 28, //  68  0000 0011 101.
            0, 0, 27, //  69  0000 0011 110.
            0, 0, 26, //  70  0000 0011 111.
            0, 0, 25, //  71  0000 0100 000.
            0, 0, 24, //  72  0000 0100 001.
            0, 0, 23, //  73  0000 0100 010.
            0, 0, 22  //  74  0000 0100 011.
        )

        //  macroblock_type bitmap:
        //    0x10  macroblock_quant
        //    0x08  macroblock_motion_forward
        //    0x04  macroblock_motion_backward
        //    0x02  macrobkock_pattern
        //    0x01  macroblock_intra
        //

        val MACROBLOCK_TYPE_INTRA = intArrayOf(
            1 * 3, 2 * 3, 0, //   0
            -1, 3 * 3, 0, //   1  0
            0, 0, 0x01, //   2  1.
            0, 0, 0x11  //   3  01.
        )

        val MACROBLOCK_TYPE_PREDICTIVE = intArrayOf(
            1 * 3, 2 * 3, 0, //  0
            3 * 3, 4 * 3, 0, //  1  0
            0, 0, 0x0a, //  2  1.
            5 * 3, 6 * 3, 0, //  3  00
            0, 0, 0x02, //  4  01.
            7 * 3, 8 * 3, 0, //  5  000
            0, 0, 0x08, //  6  001.
            9 * 3, 10 * 3, 0, //  7  0000
            11 * 3, 12 * 3, 0, //  8  0001
            -1, 13 * 3, 0, //  9  00000
            0, 0, 0x12, // 10  00001.
            0, 0, 0x1a, // 11  00010.
            0, 0, 0x01, // 12  00011.
            0, 0, 0x11  // 13  000001.
        )

        val MACROBLOCK_TYPE_B = intArrayOf(
            1 * 3, 2 * 3, 0,  //  0
            3 * 3, 5 * 3, 0,  //  1  0
            4 * 3, 6 * 3, 0,  //  2  1
            8 * 3, 7 * 3, 0,  //  3  00
            0, 0, 0x0c,  //  4  10.
            9 * 3, 10 * 3, 0,  //  5  01
            0, 0, 0x0e,  //  6  11.
            13 * 3, 14 * 3, 0,  //  7  001
            12 * 3, 11 * 3, 0,  //  8  000
            0, 0, 0x04,  //  9  010.
            0, 0, 0x06,  // 10  011.
            18 * 3, 16 * 3, 0,  // 11  0001
            15 * 3, 17 * 3, 0,  // 12  0000
            0, 0, 0x08,  // 13  0010.
            0, 0, 0x0a,  // 14  0011.
            -1, 19 * 3, 0,  // 15  00000
            0, 0, 0x01,  // 16  00011.
            20 * 3, 21 * 3, 0,  // 17  00001
            0, 0, 0x1e,  // 18  00010.
            0, 0, 0x11,  // 19  000001.
            0, 0, 0x16,  // 20  000010.
            0, 0, 0x1a   // 21  000011.
        )

        val MACROBLOCK_TYPE = listOf(
            null,
            MACROBLOCK_TYPE_INTRA,
            MACROBLOCK_TYPE_PREDICTIVE,
            MACROBLOCK_TYPE_B
        )

        val CODE_BLOCK_PATTERN = intArrayOf(
            2 * 3, 1 * 3, 0,  //   0
            3 * 3, 6 * 3, 0,  //   1  1
            4 * 3, 5 * 3, 0,  //   2  0
            8 * 3, 11 * 3, 0,  //   3  10
            12 * 3, 13 * 3, 0,  //   4  00
            9 * 3, 7 * 3, 0,  //   5  01
            10 * 3, 14 * 3, 0,  //   6  11
            20 * 3, 19 * 3, 0,  //   7  011
            18 * 3, 16 * 3, 0,  //   8  100
            23 * 3, 17 * 3, 0,  //   9  010
            27 * 3, 25 * 3, 0,  //  10  110
            21 * 3, 28 * 3, 0,  //  11  101
            15 * 3, 22 * 3, 0,  //  12  000
            24 * 3, 26 * 3, 0,  //  13  001
            0, 0, 60,  //  14  111.
            35 * 3, 40 * 3, 0,  //  15  0000
            44 * 3, 48 * 3, 0,  //  16  1001
            38 * 3, 36 * 3, 0,  //  17  0101
            42 * 3, 47 * 3, 0,  //  18  1000
            29 * 3, 31 * 3, 0,  //  19  0111
            39 * 3, 32 * 3, 0,  //  20  0110
            0, 0, 32,  //  21  1010.
            45 * 3, 46 * 3, 0,  //  22  0001
            33 * 3, 41 * 3, 0,  //  23  0100
            43 * 3, 34 * 3, 0,  //  24  0010
            0, 0, 4,  //  25  1101.
            30 * 3, 37 * 3, 0,  //  26  0011
            0, 0, 8,  //  27  1100.
            0, 0, 16,  //  28  1011.
            0, 0, 44,  //  29  0111 0.
            50 * 3, 56 * 3, 0,  //  30  0011 0
            0, 0, 28,  //  31  0111 1.
            0, 0, 52,  //  32  0110 1.
            0, 0, 62,  //  33  0100 0.
            61 * 3, 59 * 3, 0,  //  34  0010 1
            52 * 3, 60 * 3, 0,  //  35  0000 0
            0, 0, 1,  //  36  0101 1.
            55 * 3, 54 * 3, 0,  //  37  0011 1
            0, 0, 61,  //  38  0101 0.
            0, 0, 56,  //  39  0110 0.
            57 * 3, 58 * 3, 0,  //  40  0000 1
            0, 0, 2,  //  41  0100 1.
            0, 0, 40,  //  42  1000 0.
            51 * 3, 62 * 3, 0,  //  43  0010 0
            0, 0, 48,  //  44  1001 0.
            64 * 3, 63 * 3, 0,  //  45  0001 0
            49 * 3, 53 * 3, 0,  //  46  0001 1
            0, 0, 20,  //  47  1000 1.
            0, 0, 12,  //  48  1001 1.
            80 * 3, 83 * 3, 0,  //  49  0001 10
            0, 0, 63,  //  50  0011 00.
            77 * 3, 75 * 3, 0,  //  51  0010 00
            65 * 3, 73 * 3, 0,  //  52  0000 00
            84 * 3, 66 * 3, 0,  //  53  0001 11
            0, 0, 24,  //  54  0011 11.
            0, 0, 36,  //  55  0011 10.
            0, 0, 3,  //  56  0011 01.
            69 * 3, 87 * 3, 0,  //  57  0000 10
            81 * 3, 79 * 3, 0,  //  58  0000 11
            68 * 3, 71 * 3, 0,  //  59  0010 11
            70 * 3, 78 * 3, 0,  //  60  0000 01
            67 * 3, 76 * 3, 0,  //  61  0010 10
            72 * 3, 74 * 3, 0,  //  62  0010 01
            86 * 3, 85 * 3, 0,  //  63  0001 01
            88 * 3, 82 * 3, 0,  //  64  0001 00
            -1, 94 * 3, 0,  //  65  0000 000
            95 * 3, 97 * 3, 0,  //  66  0001 111
            0, 0, 33,  //  67  0010 100.
            0, 0, 9,  //  68  0010 110.
            106 * 3, 110 * 3, 0,  //  69  0000 100
            102 * 3, 116 * 3, 0,  //  70  0000 010
            0, 0, 5,  //  71  0010 111.
            0, 0, 10,  //  72  0010 010.
            93 * 3, 89 * 3, 0,  //  73  0000 001
            0, 0, 6,  //  74  0010 011.
            0, 0, 18,  //  75  0010 001.
            0, 0, 17,  //  76  0010 101.
            0, 0, 34,  //  77  0010 000.
            113 * 3, 119 * 3, 0,  //  78  0000 011
            103 * 3, 104 * 3, 0,  //  79  0000 111
            90 * 3, 92 * 3, 0,  //  80  0001 100
            109 * 3, 107 * 3, 0,  //  81  0000 110
            117 * 3, 118 * 3, 0,  //  82  0001 001
            101 * 3, 99 * 3, 0,  //  83  0001 101
            98 * 3, 96 * 3, 0,  //  84  0001 110
            100 * 3, 91 * 3, 0,  //  85  0001 011
            114 * 3, 115 * 3, 0,  //  86  0001 010
            105 * 3, 108 * 3, 0,  //  87  0000 101
            112 * 3, 111 * 3, 0,  //  88  0001 000
            121 * 3, 125 * 3, 0,  //  89  0000 0011
            0, 0, 41,  //  90  0001 1000.
            0, 0, 14,  //  91  0001 0111.
            0, 0, 21,  //  92  0001 1001.
            124 * 3, 122 * 3, 0,  //  93  0000 0010
            120 * 3, 123 * 3, 0,  //  94  0000 0001
            0, 0, 11,  //  95  0001 1110.
            0, 0, 19,  //  96  0001 1101.
            0, 0, 7,  //  97  0001 1111.
            0, 0, 35,  //  98  0001 1100.
            0, 0, 13,  //  99  0001 1011.
            0, 0, 50,  // 100  0001 0110.
            0, 0, 49,  // 101  0001 1010.
            0, 0, 58,  // 102  0000 0100.
            0, 0, 37,  // 103  0000 1110.
            0, 0, 25,  // 104  0000 1111.
            0, 0, 45,  // 105  0000 1010.
            0, 0, 57,  // 106  0000 1000.
            0, 0, 26,  // 107  0000 1101.
            0, 0, 29,  // 108  0000 1011.
            0, 0, 38,  // 109  0000 1100.
            0, 0, 53,  // 110  0000 1001.
            0, 0, 23,  // 111  0001 0001.
            0, 0, 43,  // 112  0001 0000.
            0, 0, 46,  // 113  0000 0110.
            0, 0, 42,  // 114  0001 0100.
            0, 0, 22,  // 115  0001 0101.
            0, 0, 54,  // 116  0000 0101.
            0, 0, 51,  // 117  0001 0010.
            0, 0, 15,  // 118  0001 0011.
            0, 0, 30,  // 119  0000 0111.
            0, 0, 39,  // 120  0000 0001 0.
            0, 0, 47,  // 121  0000 0011 0.
            0, 0, 55,  // 122  0000 0010 1.
            0, 0, 27,  // 123  0000 0001 1.
            0, 0, 59,  // 124  0000 0010 0.
            0, 0, 31   // 125  0000 0011 1.
        )

        val MOTION = intArrayOf(
            1 * 3, 2 * 3, 0,  //   0
            4 * 3, 3 * 3, 0,  //   1  0
            0, 0, 0,  //   2  1.
            6 * 3, 5 * 3, 0,  //   3  01
            8 * 3, 7 * 3, 0,  //   4  00
            0, 0, -1,  //   5  011.
            0, 0, 1,  //   6  010.
            9 * 3, 10 * 3, 0,  //   7  001
            12 * 3, 11 * 3, 0,  //   8  000
            0, 0, 2,  //   9  0010.
            0, 0, -2,  //  10  0011.
            14 * 3, 15 * 3, 0,  //  11  0001
            16 * 3, 13 * 3, 0,  //  12  0000
            20 * 3, 18 * 3, 0,  //  13  0000 1
            0, 0, 3,  //  14  0001 0.
            0, 0, -3,  //  15  0001 1.
            17 * 3, 19 * 3, 0,  //  16  0000 0
            -1, 23 * 3, 0,  //  17  0000 00
            27 * 3, 25 * 3, 0,  //  18  0000 11
            26 * 3, 21 * 3, 0,  //  19  0000 01
            24 * 3, 22 * 3, 0,  //  20  0000 10
            32 * 3, 28 * 3, 0,  //  21  0000 011
            29 * 3, 31 * 3, 0,  //  22  0000 101
            -1, 33 * 3, 0,  //  23  0000 001
            36 * 3, 35 * 3, 0,  //  24  0000 100
            0, 0, -4,  //  25  0000 111.
            30 * 3, 34 * 3, 0,  //  26  0000 010
            0, 0, 4,  //  27  0000 110.
            0, 0, -7,  //  28  0000 0111.
            0, 0, 5,  //  29  0000 1010.
            37 * 3, 41 * 3, 0,  //  30  0000 0100
            0, 0, -5,  //  31  0000 1011.
            0, 0, 7,  //  32  0000 0110.
            38 * 3, 40 * 3, 0,  //  33  0000 0011
            42 * 3, 39 * 3, 0,  //  34  0000 0101
            0, 0, -6,  //  35  0000 1001.
            0, 0, 6,  //  36  0000 1000.
            51 * 3, 54 * 3, 0,  //  37  0000 0100 0
            50 * 3, 49 * 3, 0,  //  38  0000 0011 0
            45 * 3, 46 * 3, 0,  //  39  0000 0101 1
            52 * 3, 47 * 3, 0,  //  40  0000 0011 1
            43 * 3, 53 * 3, 0,  //  41  0000 0100 1
            44 * 3, 48 * 3, 0,  //  42  0000 0101 0
            0, 0, 10,  //  43  0000 0100 10.
            0, 0, 9,  //  44  0000 0101 00.
            0, 0, 8,  //  45  0000 0101 10.
            0, 0, -8,  //  46  0000 0101 11.
            57 * 3, 66 * 3, 0,  //  47  0000 0011 11
            0, 0, -9,  //  48  0000 0101 01.
            60 * 3, 64 * 3, 0,  //  49  0000 0011 01
            56 * 3, 61 * 3, 0,  //  50  0000 0011 00
            55 * 3, 62 * 3, 0,  //  51  0000 0100 00
            58 * 3, 63 * 3, 0,  //  52  0000 0011 10
            0, 0, -10,  //  53  0000 0100 11.
            59 * 3, 65 * 3, 0,  //  54  0000 0100 01
            0, 0, 12,  //  55  0000 0100 000.
            0, 0, 16,  //  56  0000 0011 000.
            0, 0, 13,  //  57  0000 0011 110.
            0, 0, 14,  //  58  0000 0011 100.
            0, 0, 11,  //  59  0000 0100 010.
            0, 0, 15,  //  60  0000 0011 010.
            0, 0, -16,  //  61  0000 0011 001.
            0, 0, -12,  //  62  0000 0100 001.
            0, 0, -14,  //  63  0000 0011 101.
            0, 0, -15,  //  64  0000 0011 011.
            0, 0, -11,  //  65  0000 0100 011.
            0, 0, -13   //  66  0000 0011 111.
        )

        val DCT_DC_SIZE_LUMINANCE = intArrayOf(
            2 * 3, 1 * 3, 0,  //   0
            6 * 3, 5 * 3, 0,  //   1  1
            3 * 3, 4 * 3, 0,  //   2  0
            0, 0, 1,  //   3  00.
            0, 0, 2,  //   4  01.
            9 * 3, 8 * 3, 0,  //   5  11
            7 * 3, 10 * 3, 0,  //   6  10
            0, 0, 0,  //   7  100.
            12 * 3, 11 * 3, 0,  //   8  111
            0, 0, 4,  //   9  110.
            0, 0, 3,  //  10  101.
            13 * 3, 14 * 3, 0,  //  11  1111
            0, 0, 5,  //  12  1110.
            0, 0, 6,  //  13  1111 0.
            16 * 3, 15 * 3, 0,  //  14  1111 1
            17 * 3, -1, 0,  //  15  1111 11
            0, 0, 7,  //  16  1111 10.
            0, 0, 8   //  17  1111 110.
        )

        val DCT_DC_SIZE_CHROMINANCE = intArrayOf(
            2 * 3, 1 * 3, 0,  //   0
            4 * 3, 3 * 3, 0,  //   1  1
            6 * 3, 5 * 3, 0,  //   2  0
            8 * 3, 7 * 3, 0,  //   3  11
            0, 0, 2,  //   4  10.
            0, 0, 1,  //   5  01.
            0, 0, 0,  //   6  00.
            10 * 3, 9 * 3, 0,  //   7  111
            0, 0, 3,  //   8  110.
            12 * 3, 11 * 3, 0,  //   9  1111
            0, 0, 4,  //  10  1110.
            14 * 3, 13 * 3, 0,  //  11  1111 1
            0, 0, 5,  //  12  1111 0.
            16 * 3, 15 * 3, 0,  //  13  1111 11
            0, 0, 6,  //  14  1111 10.
            17 * 3, -1, 0,  //  15  1111 111
            0, 0, 7,  //  16  1111 110.
            0, 0, 8   //  17  1111 1110.
        )

        //  dct_coeff bitmap:
        //    0xff00  run
        //    0x00ff  level

        //  Decoded values are unsigned. Sign bit follows in the stream.

        //  Interpretation of the value 0x0001
        //    for dc_coeff_first:  run=0, level=1
        //    for dc_coeff_next:   If the next bit is 1: run=0, level=1
        //                         If the next bit is 0: end_of_block

        //  escape decodes as 0xffff.

        val DCT_COEFF = intArrayOf(
            1 * 3, 2 * 3, 0,  //   0
            4 * 3, 3 * 3, 0,  //   1  0
            0, 0, 0x0001,  //   2  1.
            7 * 3, 8 * 3, 0,  //   3  01
            6 * 3, 5 * 3, 0,  //   4  00
            13 * 3, 9 * 3, 0,  //   5  001
            11 * 3, 10 * 3, 0,  //   6  000
            14 * 3, 12 * 3, 0,  //   7  010
            0, 0, 0x0101,  //   8  011.
            20 * 3, 22 * 3, 0,  //   9  0011
            18 * 3, 21 * 3, 0,  //  10  0001
            16 * 3, 19 * 3, 0,  //  11  0000
            0, 0, 0x0201,  //  12  0101.
            17 * 3, 15 * 3, 0,  //  13  0010
            0, 0, 0x0002,  //  14  0100.
            0, 0, 0x0003,  //  15  0010 1.
            27 * 3, 25 * 3, 0,  //  16  0000 0
            29 * 3, 31 * 3, 0,  //  17  0010 0
            24 * 3, 26 * 3, 0,  //  18  0001 0
            32 * 3, 30 * 3, 0,  //  19  0000 1
            0, 0, 0x0401,  //  20  0011 0.
            23 * 3, 28 * 3, 0,  //  21  0001 1
            0, 0, 0x0301,  //  22  0011 1.
            0, 0, 0x0102,  //  23  0001 10.
            0, 0, 0x0701,  //  24  0001 00.
            0, 0, 0xffff,  //  25  0000 01. -- escape
            0, 0, 0x0601,  //  26  0001 01.
            37 * 3, 36 * 3, 0,  //  27  0000 00
            0, 0, 0x0501,  //  28  0001 11.
            35 * 3, 34 * 3, 0,  //  29  0010 00
            39 * 3, 38 * 3, 0,  //  30  0000 11
            33 * 3, 42 * 3, 0,  //  31  0010 01
            40 * 3, 41 * 3, 0,  //  32  0000 10
            52 * 3, 50 * 3, 0,  //  33  0010 010
            54 * 3, 53 * 3, 0,  //  34  0010 001
            48 * 3, 49 * 3, 0,  //  35  0010 000
            43 * 3, 45 * 3, 0,  //  36  0000 001
            46 * 3, 44 * 3, 0,  //  37  0000 000
            0, 0, 0x0801,  //  38  0000 111.
            0, 0, 0x0004,  //  39  0000 110.
            0, 0, 0x0202,  //  40  0000 100.
            0, 0, 0x0901,  //  41  0000 101.
            51 * 3, 47 * 3, 0,  //  42  0010 011
            55 * 3, 57 * 3, 0,  //  43  0000 0010
            60 * 3, 56 * 3, 0,  //  44  0000 0001
            59 * 3, 58 * 3, 0,  //  45  0000 0011
            61 * 3, 62 * 3, 0,  //  46  0000 0000
            0, 0, 0x0a01,  //  47  0010 0111.
            0, 0, 0x0d01,  //  48  0010 0000.
            0, 0, 0x0006,  //  49  0010 0001.
            0, 0, 0x0103,  //  50  0010 0101.
            0, 0, 0x0005,  //  51  0010 0110.
            0, 0, 0x0302,  //  52  0010 0100.
            0, 0, 0x0b01,  //  53  0010 0011.
            0, 0, 0x0c01,  //  54  0010 0010.
            76 * 3, 75 * 3, 0,  //  55  0000 0010 0
            67 * 3, 70 * 3, 0,  //  56  0000 0001 1
            73 * 3, 71 * 3, 0,  //  57  0000 0010 1
            78 * 3, 74 * 3, 0,  //  58  0000 0011 1
            72 * 3, 77 * 3, 0,  //  59  0000 0011 0
            69 * 3, 64 * 3, 0,  //  60  0000 0001 0
            68 * 3, 63 * 3, 0,  //  61  0000 0000 0
            66 * 3, 65 * 3, 0,  //  62  0000 0000 1
            81 * 3, 87 * 3, 0,  //  63  0000 0000 01
            91 * 3, 80 * 3, 0,  //  64  0000 0001 01
            82 * 3, 79 * 3, 0,  //  65  0000 0000 11
            83 * 3, 86 * 3, 0,  //  66  0000 0000 10
            93 * 3, 92 * 3, 0,  //  67  0000 0001 10
            84 * 3, 85 * 3, 0,  //  68  0000 0000 00
            90 * 3, 94 * 3, 0,  //  69  0000 0001 00
            88 * 3, 89 * 3, 0,  //  70  0000 0001 11
            0, 0, 0x0203,  //  71  0000 0010 11.
            0, 0, 0x0104,  //  72  0000 0011 00.
            0, 0, 0x0007,  //  73  0000 0010 10.
            0, 0, 0x0402,  //  74  0000 0011 11.
            0, 0, 0x0502,  //  75  0000 0010 01.
            0, 0, 0x1001,  //  76  0000 0010 00.
            0, 0, 0x0f01,  //  77  0000 0011 01.
            0, 0, 0x0e01,  //  78  0000 0011 10.
            105 * 3, 107 * 3, 0,  //  79  0000 0000 111
            111 * 3, 114 * 3, 0,  //  80  0000 0001 011
            104 * 3, 97 * 3, 0,  //  81  0000 0000 010
            125 * 3, 119 * 3, 0,  //  82  0000 0000 110
            96 * 3, 98 * 3, 0,  //  83  0000 0000 100
            -1, 123 * 3, 0,  //  84  0000 0000 000
            95 * 3, 101 * 3, 0,  //  85  0000 0000 001
            106 * 3, 121 * 3, 0,  //  86  0000 0000 101
            99 * 3, 102 * 3, 0,  //  87  0000 0000 011
            113 * 3, 103 * 3, 0,  //  88  0000 0001 110
            112 * 3, 116 * 3, 0,  //  89  0000 0001 111
            110 * 3, 100 * 3, 0,  //  90  0000 0001 000
            124 * 3, 115 * 3, 0,  //  91  0000 0001 010
            117 * 3, 122 * 3, 0,  //  92  0000 0001 101
            109 * 3, 118 * 3, 0,  //  93  0000 0001 100
            120 * 3, 108 * 3, 0,  //  94  0000 0001 001
            127 * 3, 136 * 3, 0,  //  95  0000 0000 0010
            139 * 3, 140 * 3, 0,  //  96  0000 0000 1000
            130 * 3, 126 * 3, 0,  //  97  0000 0000 0101
            145 * 3, 146 * 3, 0,  //  98  0000 0000 1001
            128 * 3, 129 * 3, 0,  //  99  0000 0000 0110
            0, 0, 0x0802,  // 100  0000 0001 0001.
            132 * 3, 134 * 3, 0,  // 101  0000 0000 0011
            155 * 3, 154 * 3, 0,  // 102  0000 0000 0111
            0, 0, 0x0008,  // 103  0000 0001 1101.
            137 * 3, 133 * 3, 0,  // 104  0000 0000 0100
            143 * 3, 144 * 3, 0,  // 105  0000 0000 1110
            151 * 3, 138 * 3, 0,  // 106  0000 0000 1010
            142 * 3, 141 * 3, 0,  // 107  0000 0000 1111
            0, 0, 0x000a,  // 108  0000 0001 0011.
            0, 0, 0x0009,  // 109  0000 0001 1000.
            0, 0, 0x000b,  // 110  0000 0001 0000.
            0, 0, 0x1501,  // 111  0000 0001 0110.
            0, 0, 0x0602,  // 112  0000 0001 1110.
            0, 0, 0x0303,  // 113  0000 0001 1100.
            0, 0, 0x1401,  // 114  0000 0001 0111.
            0, 0, 0x0702,  // 115  0000 0001 0101.
            0, 0, 0x1101,  // 116  0000 0001 1111.
            0, 0, 0x1201,  // 117  0000 0001 1010.
            0, 0, 0x1301,  // 118  0000 0001 1001.
            148 * 3, 152 * 3, 0,  // 119  0000 0000 1101
            0, 0, 0x0403,  // 120  0000 0001 0010.
            153 * 3, 150 * 3, 0,  // 121  0000 0000 1011
            0, 0, 0x0105,  // 122  0000 0001 1011.
            131 * 3, 135 * 3, 0,  // 123  0000 0000 0001
            0, 0, 0x0204,  // 124  0000 0001 0100.
            149 * 3, 147 * 3, 0,  // 125  0000 0000 1100
            172 * 3, 173 * 3, 0,  // 126  0000 0000 0101 1
            162 * 3, 158 * 3, 0,  // 127  0000 0000 0010 0
            170 * 3, 161 * 3, 0,  // 128  0000 0000 0110 0
            168 * 3, 166 * 3, 0,  // 129  0000 0000 0110 1
            157 * 3, 179 * 3, 0,  // 130  0000 0000 0101 0
            169 * 3, 167 * 3, 0,  // 131  0000 0000 0001 0
            174 * 3, 171 * 3, 0,  // 132  0000 0000 0011 0
            178 * 3, 177 * 3, 0,  // 133  0000 0000 0100 1
            156 * 3, 159 * 3, 0,  // 134  0000 0000 0011 1
            164 * 3, 165 * 3, 0,  // 135  0000 0000 0001 1
            183 * 3, 182 * 3, 0,  // 136  0000 0000 0010 1
            175 * 3, 176 * 3, 0,  // 137  0000 0000 0100 0
            0, 0, 0x0107,  // 138  0000 0000 1010 1.
            0, 0, 0x0a02,  // 139  0000 0000 1000 0.
            0, 0, 0x0902,  // 140  0000 0000 1000 1.
            0, 0, 0x1601,  // 141  0000 0000 1111 1.
            0, 0, 0x1701,  // 142  0000 0000 1111 0.
            0, 0, 0x1901,  // 143  0000 0000 1110 0.
            0, 0, 0x1801,  // 144  0000 0000 1110 1.
            0, 0, 0x0503,  // 145  0000 0000 1001 0.
            0, 0, 0x0304,  // 146  0000 0000 1001 1.
            0, 0, 0x000d,  // 147  0000 0000 1100 1.
            0, 0, 0x000c,  // 148  0000 0000 1101 0.
            0, 0, 0x000e,  // 149  0000 0000 1100 0.
            0, 0, 0x000f,  // 150  0000 0000 1011 1.
            0, 0, 0x0205,  // 151  0000 0000 1010 0.
            0, 0, 0x1a01,  // 152  0000 0000 1101 1.
            0, 0, 0x0106,  // 153  0000 0000 1011 0.
            180 * 3, 181 * 3, 0,  // 154  0000 0000 0111 1
            160 * 3, 163 * 3, 0,  // 155  0000 0000 0111 0
            196 * 3, 199 * 3, 0,  // 156  0000 0000 0011 10
            0, 0, 0x001b,  // 157  0000 0000 0101 00.
            203 * 3, 185 * 3, 0,  // 158  0000 0000 0010 01
            202 * 3, 201 * 3, 0,  // 159  0000 0000 0011 11
            0, 0, 0x0013,  // 160  0000 0000 0111 00.
            0, 0, 0x0016,  // 161  0000 0000 0110 01.
            197 * 3, 207 * 3, 0,  // 162  0000 0000 0010 00
            0, 0, 0x0012,  // 163  0000 0000 0111 01.
            191 * 3, 192 * 3, 0,  // 164  0000 0000 0001 10
            188 * 3, 190 * 3, 0,  // 165  0000 0000 0001 11
            0, 0, 0x0014,  // 166  0000 0000 0110 11.
            184 * 3, 194 * 3, 0,  // 167  0000 0000 0001 01
            0, 0, 0x0015,  // 168  0000 0000 0110 10.
            186 * 3, 193 * 3, 0,  // 169  0000 0000 0001 00
            0, 0, 0x0017,  // 170  0000 0000 0110 00.
            204 * 3, 198 * 3, 0,  // 171  0000 0000 0011 01
            0, 0, 0x0019,  // 172  0000 0000 0101 10.
            0, 0, 0x0018,  // 173  0000 0000 0101 11.
            200 * 3, 205 * 3, 0,  // 174  0000 0000 0011 00
            0, 0, 0x001f,  // 175  0000 0000 0100 00.
            0, 0, 0x001e,  // 176  0000 0000 0100 01.
            0, 0, 0x001c,  // 177  0000 0000 0100 11.
            0, 0, 0x001d,  // 178  0000 0000 0100 10.
            0, 0, 0x001a,  // 179  0000 0000 0101 01.
            0, 0, 0x0011,  // 180  0000 0000 0111 10.
            0, 0, 0x0010,  // 181  0000 0000 0111 11.
            189 * 3, 206 * 3, 0,  // 182  0000 0000 0010 11
            187 * 3, 195 * 3, 0,  // 183  0000 0000 0010 10
            218 * 3, 211 * 3, 0,  // 184  0000 0000 0001 010
            0, 0, 0x0025,  // 185  0000 0000 0010 011.
            215 * 3, 216 * 3, 0,  // 186  0000 0000 0001 000
            0, 0, 0x0024,  // 187  0000 0000 0010 100.
            210 * 3, 212 * 3, 0,  // 188  0000 0000 0001 110
            0, 0, 0x0022,  // 189  0000 0000 0010 110.
            213 * 3, 209 * 3, 0,  // 190  0000 0000 0001 111
            221 * 3, 222 * 3, 0,  // 191  0000 0000 0001 100
            219 * 3, 208 * 3, 0,  // 192  0000 0000 0001 101
            217 * 3, 214 * 3, 0,  // 193  0000 0000 0001 001
            223 * 3, 220 * 3, 0,  // 194  0000 0000 0001 011
            0, 0, 0x0023,  // 195  0000 0000 0010 101.
            0, 0, 0x010b,  // 196  0000 0000 0011 100.
            0, 0, 0x0028,  // 197  0000 0000 0010 000.
            0, 0, 0x010c,  // 198  0000 0000 0011 011.
            0, 0, 0x010a,  // 199  0000 0000 0011 101.
            0, 0, 0x0020,  // 200  0000 0000 0011 000.
            0, 0, 0x0108,  // 201  0000 0000 0011 111.
            0, 0, 0x0109,  // 202  0000 0000 0011 110.
            0, 0, 0x0026,  // 203  0000 0000 0010 010.
            0, 0, 0x010d,  // 204  0000 0000 0011 010.
            0, 0, 0x010e,  // 205  0000 0000 0011 001.
            0, 0, 0x0021,  // 206  0000 0000 0010 111.
            0, 0, 0x0027,  // 207  0000 0000 0010 001.
            0, 0, 0x1f01,  // 208  0000 0000 0001 1011.
            0, 0, 0x1b01,  // 209  0000 0000 0001 1111.
            0, 0, 0x1e01,  // 210  0000 0000 0001 1100.
            0, 0, 0x1002,  // 211  0000 0000 0001 0101.
            0, 0, 0x1d01,  // 212  0000 0000 0001 1101.
            0, 0, 0x1c01,  // 213  0000 0000 0001 1110.
            0, 0, 0x010f,  // 214  0000 0000 0001 0011.
            0, 0, 0x0112,  // 215  0000 0000 0001 0000.
            0, 0, 0x0111,  // 216  0000 0000 0001 0001.
            0, 0, 0x0110,  // 217  0000 0000 0001 0010.
            0, 0, 0x0603,  // 218  0000 0000 0001 0100.
            0, 0, 0x0b02,  // 219  0000 0000 0001 1010.
            0, 0, 0x0e02,  // 220  0000 0000 0001 0111.
            0, 0, 0x0d02,  // 221  0000 0000 0001 1000.
            0, 0, 0x0c02,  // 222  0000 0000 0001 1001.
            0, 0, 0x0f02   // 223  0000 0000 0001 0110.
        )

        object PICTURE_TYPE {
            const val INTRA = 1
            const val PREDICTIVE = 2
            const val B = 3
        }

        object START {
            const val SEQUENCE = 0xB3
            const val SLICE_FIRST = 0x01
            const val SLICE_LAST = 0xAF
            const val PICTURE = 0x00
            const val EXTENSION = 0xB5
            const val USER_DATA = 0xB2
        }
    }
}
