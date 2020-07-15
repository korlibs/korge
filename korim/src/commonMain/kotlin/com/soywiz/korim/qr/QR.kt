//---------------------------------------------------------------------
// QRCode for JavaScript (ported to Kotlin/korim by soywiz)
//
// Copyright (c) 2009 Kazuhiko Arase
//
// URL: http://www.d-project.com/
//
// Licensed under the MIT license:
//   http://www.opensource.org/licenses/mit-license.php
//
// The word "QR Code" is registered trademark of
// DENSO WAVE INCORPORATED
//   http://www.denso-wave.com/qrcode/faqpatent-e.html
//
//---------------------------------------------------------------------

package com.soywiz.korim.qr

/**
 * https://github.com/davidshimjs/qrcodejs
 *
 * @fileoverview
 * - Using the 'QRCode for Javascript library'
 * - Fixed dataset of 'QRCode for Javascript library' for support full-spec.
 * - this library has no dependencies.
 *
 * @author davidshimjs
 * @see <a href="http://www.d-project.com/" target="_blank">http://www.d-project.com/</a>
 * @see <a href="http://jeromeetienne.github.com/jquery-qrcode/" target="_blank">http://jeromeetienne.github.com/jquery-qrcode/</a>
 */

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import kotlin.math.*

@Suppress("unused")
open class QR(
    val colorDark: RGBA = Colors.BLACK,
    val colorLight: RGBA = Colors.WHITE,
    val correctLevel: QRErrorCorrectLevel = QRErrorCorrectLevel.H
) {
    fun msg(msg: ByteArray): Bitmap32 =
        QRCodeModel(getTypeNumber(msg, correctLevel), correctLevel).apply {
            addData(msg)
            make()
        }.toBitmap(colorDark, colorLight)

    fun msg(msg: String): Bitmap32 =
        QRCodeModel(getTypeNumber(msg, correctLevel), correctLevel).apply {
            addData(msg)
            make()
        }.toBitmap(colorDark, colorLight)

    fun vCard(
        name: String, phone: String, email: String, url: String, addr: String, org: String, note: String
    ): Bitmap32 =
        msg("BEGIN:VCARD\nN:$name\nTEL:$phone\nEMAIL:$email\nURL:$url\nADR:$addr\nORG:$org\nNOTE:$note\nVERSION:3.0\nEND:VCARD")

    fun meCard(
        name: String, phone: String, email: String, url: String, addr: String
    ): Bitmap32 = msg("MECARD:N:$name;ADR:$addr;TEL:$phone;EMAIL:$email;URL:$url;;")

    enum class WifiKind { WEP, WAP, nopass }

    fun wifi(ssid: String, password: String, kind: WifiKind = WifiKind.WEP) = msg("WIFI:S:$ssid;T:$kind;P:$password;;")
    fun itunesAppUrl(appId: String) = msg("itms://itunes.apple.com/app/$appId")
    fun itunesAppReviewUrl(appId: String) =
        msg("itms-apps://ax.itunes.apple.com/WebObjects/MZStore.woa/wa/viewContentsUserReviews?type=Purple+Software&id=$appId")

    fun androidMarketAppUrl(packageName: String) = msg("market://details?id=$packageName")
    fun androidMarketSearchUrl(search: String) = msg("market://search?q=$search")
    fun foursquareVenueURL(venueId: String) = msg("https://foursquare.com/venue/$venueId")
    fun youtubeForIOS(videoId: String) = msg("youtube://$videoId")
    fun twitterTweet(tweet: String) = msg("http://twitter.com/home?status=$tweet")
    fun phone(phone: String) = msg("tel:$phone")
    fun email(email: String) = msg("email:$email")
    fun sms(number: String, message: String) = msg("smsto:$number:$message")
    fun geo(latitude: String, longitude: String) = msg("geo:$latitude,$longitude")
    fun geo(latitude: Double, longitude: Double) = msg("geo:$latitude,$longitude")
    fun calendarEvent(
        summary: String,
        start: DateTime,
        end: DateTime,
        location: String,
        description: String
    ): Bitmap32 = msg(
        "BEGIN:VEVENT\nSUMMARY:$summary\nDTSTART:${dateFormat.format(start)}\nDTEND:${dateFormat.format(end)}\nLOCATION:$location\nDESCRIPTION:$description\nEND:VEVENT"
    )

    companion object : QR() {
        private val dateFormat = DateFormat("YYYYMMdd")

        private fun getTypeNumber(sText: String, nCorrectLevel: QRErrorCorrectLevel): Int =
            getTypeNumber(sText.toByteArray(UTF8).size, nCorrectLevel)

        private fun getTypeNumber(msg: ByteArray, nCorrectLevel: QRErrorCorrectLevel): Int =
            getTypeNumber(msg.size, nCorrectLevel)

        private fun getTypeNumber(length: Int, nCorrectLevel: QRErrorCorrectLevel): Int {
            var nType = 1

            for (i in 0 until QRCodeLimitLength.size) {
                val nLimit = when (nCorrectLevel) {
                    QRErrorCorrectLevel.L -> QRCodeLimitLength[i][0]
                    QRErrorCorrectLevel.M -> QRCodeLimitLength[i][1]
                    QRErrorCorrectLevel.Q -> QRCodeLimitLength[i][2]
                    QRErrorCorrectLevel.H -> QRCodeLimitLength[i][3]
                }

                if (length <= nLimit) break
                nType++
            }

            if (nType > QRCodeLimitLength.size) error("Too long data")

            return nType
        }

        private var QRCodeLimitLength = listOf(
            intArrayOf(17, 14, 11, 7),
            intArrayOf(32, 26, 20, 14),
            intArrayOf(53, 42, 32, 24),
            intArrayOf(78, 62, 46, 34),
            intArrayOf(106, 84, 60, 44),
            intArrayOf(134, 106, 74, 58),
            intArrayOf(154, 122, 86, 64),
            intArrayOf(192, 152, 108, 84),
            intArrayOf(230, 180, 130, 98),
            intArrayOf(271, 213, 151, 119),
            intArrayOf(321, 251, 177, 137),
            intArrayOf(367, 287, 203, 155),
            intArrayOf(425, 331, 241, 177),
            intArrayOf(458, 362, 258, 194),
            intArrayOf(520, 412, 292, 220),
            intArrayOf(586, 450, 322, 250),
            intArrayOf(644, 504, 364, 280),
            intArrayOf(718, 560, 394, 310),
            intArrayOf(792, 624, 442, 338),
            intArrayOf(858, 666, 482, 382),
            intArrayOf(929, 711, 509, 403),
            intArrayOf(1003, 779, 565, 439),
            intArrayOf(1091, 857, 611, 461),
            intArrayOf(1171, 911, 661, 511),
            intArrayOf(1273, 997, 715, 535),
            intArrayOf(1367, 1059, 751, 593),
            intArrayOf(1465, 1125, 805, 625),
            intArrayOf(1528, 1190, 868, 658),
            intArrayOf(1628, 1264, 908, 698),
            intArrayOf(1732, 1370, 982, 742),
            intArrayOf(1840, 1452, 1030, 790),
            intArrayOf(1952, 1538, 1112, 842),
            intArrayOf(2068, 1628, 1168, 898),
            intArrayOf(2188, 1722, 1228, 958),
            intArrayOf(2303, 1809, 1283, 983),
            intArrayOf(2431, 1911, 1351, 1051),
            intArrayOf(2563, 1989, 1423, 1093),
            intArrayOf(2699, 2099, 1499, 1139),
            intArrayOf(2809, 2213, 1579, 1219),
            intArrayOf(2953, 2331, 1663, 1273)
        )
    }
}

enum class QRErrorCorrectLevel(val id: Int) {
    L(1), M(0), Q(3), H(2)
}

private fun QRCodeModel.toBitmap(dark: RGBA, light: RGBA): Bitmap32 =
    Bitmap32(moduleCount, moduleCount, premultiplied = false) { col, row -> if (isDark(row, col)) dark else light }

private class QR8bitByte(parsedDataS: ByteArray) {
    var mode = QRMode.MODE_8BIT_BYTE
    val parsedData = UByteArrayInt(parsedDataS)

    companion object {
        operator fun invoke(data: String): QR8bitByte {
            val datab = data.toByteArray(UTF8)
            return QR8bitByte(
                if (datab.size != data.length) datab + byteArrayOf(191.toByte(), 187.toByte(), 239.toByte()) else datab
            )
        }
    }

    fun getLength(): Int = parsedData.size
    fun write(buffer: QRBitBuffer) = run { for (i in 0 until this.parsedData.size) buffer.put(this.parsedData[i], 8) }
}

private class QRCodeModel(val typeNumber: Int, val errorCorrectLevel: QRErrorCorrectLevel) {
    lateinit var modules: Array<Array<Boolean?>>
    var moduleCount: Int = 0; private set
    var dataCache: IntArray? = null
    var dataList = arrayListOf<QR8bitByte>()

    fun addData(data: String) {
        dataList.add(QR8bitByte(data))
        dataCache = null
    }

    fun addData(data: ByteArray) {
        dataList.add(QR8bitByte(data))
        dataCache = null
    }

    fun isDark(row: Int, col: Int): Boolean {
        if (row < 0 || moduleCount <= row || col < 0 || moduleCount <= col) error("""$row,$col""")
        return modules[row][col] ?: false
    }

    fun make() {
        makeImpl(false, getBestMaskPattern())
    }

    fun makeImpl(test: Boolean, maskPattern: Int) {
        moduleCount = typeNumber * 4 + 17
        if (moduleCount <= 0) error("Invalid moduleCount")
        @Suppress("RemoveExplicitTypeArguments")
        modules = Array<Array<Boolean?>>(moduleCount) { arrayOf() }
        for (row in 0 until moduleCount) {
            modules[row] = Array(moduleCount) { null }
            for (col in 0 until moduleCount) {
                modules[row][col] = null
            }
        }
        setupPositionProbePattern(0, 0)
        setupPositionProbePattern(moduleCount - 7, 0)
        setupPositionProbePattern(0, moduleCount - 7)
        setupPositionAdjustPattern()
        setupTimingPattern()
        setupTypeInfo(test, maskPattern)
        if (typeNumber >= 7) setupTypeNumber(test)
        if (dataCache == null) {
            dataCache = QRCodeModel.createData(typeNumber, errorCorrectLevel, dataList)
        }
        mapData(dataCache!!, maskPattern)
    }

    fun setupPositionProbePattern(row: Int, col: Int) {
        for (r in -1..7) {
            if (row + r <= -1 || moduleCount <= row + r) continue
            for (c in -1..7) {
                if (col + c <= -1 || moduleCount <= col + c) continue
                modules[row + r][col + c] = (r in 0..6 && (c == 0 || c == 6)) ||
                    (c in 0..6 && (r == 0 || r == 6)) || (r in 2..4 && 2 <= c && c <= 4)
            }
        }
    }

    fun getBestMaskPattern(): Int {
        var minLostPoint = 0
        var pattern = 0
        for (i in 0 until 8) {
            makeImpl(true, i)
            val lostPoint = QRUtil.getLostPoint(this)
            if (i == 0 || minLostPoint > lostPoint) {
                minLostPoint = lostPoint
                pattern = i
            }
        }
        return pattern
    }

    fun setupTimingPattern() {
        for (r in 8 until moduleCount - 8) {
            if (modules[r][6] == null) {
                modules[r][6] = (r % 2 == 0)
            }
        }

        for (c in 8 until moduleCount - 8) {
            if (modules[6][c] == null) {
                modules[6][c] = (c % 2 == 0)
            }
        }
    }

    fun setupPositionAdjustPattern() {
        val pos = QRUtil.getPatternPosition(typeNumber)
        for (i in 0 until pos.size) for (j in 0 until pos.size) {
            val row = pos[i]
            val col = pos[j]
            if (modules[row][col] == null) {
                for (r in -2..+2) for (c in -2..+2) {
                    modules[row + r][col + c] = (r == -2 || r == 2 || c == -2 || c == 2 || (r == 0 && c == 0))
                }
            }
        }
    }

    fun setupTypeNumber(test: Boolean) {
        val bits = QRUtil.getBCHTypeNumber(typeNumber)
        for (i in 0 until 18) {
            val mod = (!test && ((bits shr i) and 1) == 1)
            modules[(i / 3)][i % 3 + moduleCount - 8 - 3] = mod
        }
        for (i in 0 until 18) {
            val mod = (!test && ((bits shr i) and 1) == 1)
            modules[i % 3 + moduleCount - 8 - 3][(i / 3)] = mod
        }
    }

    fun setupTypeInfo(test: Boolean, maskPattern: Int) {
        val data = (errorCorrectLevel.id shl 3) or maskPattern
        val bits = QRUtil.getBCHTypeInfo(data)
        for (i in 0 until 15) {
            val mod = (!test && ((bits shr i) and 1) == 1)
            when {
                i < 6 -> modules[i][8] = mod
                i < 8 -> modules[i + 1][8] = mod
                else -> modules[moduleCount - 15 + i][8] = mod
            }
        }
        for (i in 0 until 15) {
            val mod = (!test && ((bits shr i) and 1) == 1)
            when {
                i < 8 -> modules[8][moduleCount - i - 1] = mod
                i < 9 -> modules[8][15 - i - 1 + 1] = mod
                else -> modules[8][15 - i - 1] = mod
            }
        }
        modules[moduleCount - 8][8] = (!test)
    }

    fun mapData(data: IntArray, maskPattern: Int) {
        var inc = -1
        var row = moduleCount - 1
        var bitIndex = 7
        var byteIndex = 0
        var col = moduleCount - 1
        while (col > 0) {
            if (col == 6) col--
            while (true) {
                for (c in 0 until 2) {
                    if (modules[row][col - c] == null) {
                        var dark = false
                        if (byteIndex < data.size) {
                            dark = (((data[byteIndex] ushr bitIndex) and 1) == 1)
                        }
                        val mask = QRUtil.getMask(maskPattern, row, col - c)
                        if (mask) dark = !dark
                        modules[row][col - c] = dark
                        bitIndex--
                        if (bitIndex == -1) {
                            byteIndex++
                            bitIndex = 7
                        }
                    }
                }
                row += inc
                if (row < 0 || moduleCount <= row) {
                    row -= inc
                    inc = -inc
                    break
                }
            }
            col -= 2
        }
    }

    companion object {
        const val PAD0 = 0xEC
        const val PAD1 = 0x11
        fun createData(typeNumber: Int, errorCorrectLevel: QRErrorCorrectLevel, dataList: List<QR8bitByte>): IntArray {
            val rsBlocks = QRRSBlock.getRSBlocks(typeNumber, errorCorrectLevel)
            val buffer = QRBitBuffer()
            for (i in 0 until dataList.size) {
                val data = dataList[i]
                buffer.put(data.mode, 4)
                buffer.put(data.getLength(), QRUtil.getLengthInBits(data.mode, typeNumber))
                data.write(buffer)
            }
            var totalDataCount = 0
            for (i in 0 until rsBlocks.size) totalDataCount += rsBlocks[i].dataCount
            if (buffer.getLengthInBits() > totalDataCount * 8) {
                error("code length overflow. (${buffer.getLengthInBits()}>${totalDataCount * 8})")
            }
            if (buffer.getLengthInBits() + 4 <= totalDataCount * 8) {
                buffer.put(0, 4)
            }
            while (buffer.getLengthInBits() % 8 != 0) buffer.putBit(false)
            while (true) {
                if (buffer.getLengthInBits() >= totalDataCount * 8) break
                buffer.put(QRCodeModel.PAD0, 8)
                if (buffer.getLengthInBits() >= totalDataCount * 8) break
                buffer.put(QRCodeModel.PAD1, 8)
            }
            return QRCodeModel.createBytes(buffer, rsBlocks)
        }

        fun createBytes(buffer: QRBitBuffer, rsBlocks: List<QRRSBlock>): IntArray {
            var offset = 0
            var maxDcCount = 0
            var maxEcCount = 0
            val dcdata = Array(rsBlocks.size) { intArrayOf() }
            val ecdata = Array(rsBlocks.size) { intArrayOf() }
            for (r in 0 until rsBlocks.size) {
                val dcCount = rsBlocks[r].dataCount
                val ecCount = rsBlocks[r].totalCount - dcCount
                maxDcCount = max(maxDcCount, dcCount)
                maxEcCount = max(maxEcCount, ecCount)
                dcdata[r] = IntArray(dcCount)
                for (i in 0 until dcdata[r].size) {
                    dcdata[r][i] = 0xff and buffer.buffer.getAt(i + offset)
                }
                offset += dcCount
                val rsPoly = QRUtil.getErrorCorrectPolynomial(ecCount)
                val rawPoly = QRPolynomial(dcdata[r], rsPoly.getLength() - 1)
                val modPoly = rawPoly.mod(rsPoly)
                ecdata[r] = IntArray(rsPoly.getLength() - 1)
                for (i in 0 until ecdata[r].size) {
                    val modIndex = i + modPoly.getLength() - ecdata[r].size
                    ecdata[r][i] = if (modIndex >= 0) modPoly.get(modIndex) else 0
                }
            }
            var totalCodeCount = 0
            for (i in 0 until rsBlocks.size) totalCodeCount += rsBlocks[i].totalCount
            val data = IntArray(totalCodeCount)
            var index = 0
            for (i in 0 until maxDcCount) for (r in 0 until rsBlocks.size) {
                if (i < dcdata[r].size) data[index++] = dcdata[r][i]
            }
            for (i in 0 until maxEcCount) for (r in 0 until rsBlocks.size) {
                if (i < ecdata[r].size) data[index++] = ecdata[r][i]
            }
            return data
        }
    }
}

private object QRMode {
    const val MODE_NUMBER = 1 shl 0
    const val MODE_ALPHA_NUM = 1 shl 1
    const val MODE_8BIT_BYTE = 1 shl 2
    const val MODE_KANJI = 1 shl 3
}

private object QRMaskPattern {
    const val PATTERN000 = 0
    const val PATTERN001 = 1
    const val PATTERN010 = 2
    const val PATTERN011 = 3
    const val PATTERN100 = 4
    const val PATTERN101 = 5
    const val PATTERN110 = 6
    const val PATTERN111 = 7
}

private object QRUtil {
    val PATTERN_POSITION_TABLE = listOf(
        intArrayOf(),
        intArrayOf(6, 18),
        intArrayOf(6, 22),
        intArrayOf(6, 26),
        intArrayOf(6, 30),
        intArrayOf(6, 34),
        intArrayOf(6, 22, 38),
        intArrayOf(6, 24, 42),
        intArrayOf(6, 26, 46),
        intArrayOf(6, 28, 50),
        intArrayOf(6, 30, 54),
        intArrayOf(6, 32, 58),
        intArrayOf(6, 34, 62),
        intArrayOf(6, 26, 46, 66),
        intArrayOf(6, 26, 48, 70),
        intArrayOf(6, 26, 50, 74),
        intArrayOf(6, 30, 54, 78),
        intArrayOf(6, 30, 56, 82),
        intArrayOf(6, 30, 58, 86),
        intArrayOf(6, 34, 62, 90),
        intArrayOf(6, 28, 50, 72, 94),
        intArrayOf(6, 26, 50, 74, 98),
        intArrayOf(6, 30, 54, 78, 102),
        intArrayOf(6, 28, 54, 80, 106),
        intArrayOf(6, 32, 58, 84, 110),
        intArrayOf(6, 30, 58, 86, 114),
        intArrayOf(6, 34, 62, 90, 118),
        intArrayOf(6, 26, 50, 74, 98, 122),
        intArrayOf(6, 30, 54, 78, 102, 126),
        intArrayOf(6, 26, 52, 78, 104, 130),
        intArrayOf(6, 30, 56, 82, 108, 134),
        intArrayOf(6, 34, 60, 86, 112, 138),
        intArrayOf(6, 30, 58, 86, 114, 142),
        intArrayOf(6, 34, 62, 90, 118, 146),
        intArrayOf(6, 30, 54, 78, 102, 126, 150),
        intArrayOf(6, 24, 50, 76, 102, 128, 154),
        intArrayOf(6, 28, 54, 80, 106, 132, 158),
        intArrayOf(6, 32, 58, 84, 110, 136, 162),
        intArrayOf(6, 26, 54, 82, 110, 138, 166),
        intArrayOf(6, 30, 58, 86, 114, 142, 170)
    )

    const val G15 = (1 shl 10) or (1 shl 8) or (1 shl 5) or (1 shl 4) or (1 shl 2) or (1 shl 1) or (1 shl 0)
    const val G18 =
        (1 shl 12) or (1 shl 11) or (1 shl 10) or (1 shl 9) or (1 shl 8) or (1 shl 5) or (1 shl 2) or (1 shl 0)
    const val G15_MASK = (1 shl 14) or (1 shl 12) or (1 shl 10) or (1 shl 4) or (1 shl 1)

    fun getBCHTypeInfo(data: Int): Int {
        var d = data shl 10
        while (QRUtil.getBCHDigit(d) - QRUtil.getBCHDigit(QRUtil.G15) >= 0) {
            d = d xor (QRUtil.G15 shl (QRUtil.getBCHDigit(d) - QRUtil.getBCHDigit(QRUtil.G15)))
        }
        return ((data shl 10) or d) xor QRUtil.G15_MASK
    }

    fun getBCHTypeNumber(data: Int): Int {
        var d = data shl 12
        while (QRUtil.getBCHDigit(d) - QRUtil.getBCHDigit(QRUtil.G18) >= 0) {
            d = d xor (QRUtil.G18 shl (QRUtil.getBCHDigit(d) - QRUtil.getBCHDigit(QRUtil.G18)))
        }
        return (data shl 12) or d
    }

    fun getBCHDigit(data: Int): Int {
        var dd = data
        var digit = 0
        while (dd != 0) {
            digit++
            dd = dd ushr 1
        }
        return digit
    }

    fun getPatternPosition(typeNumber: Int): IntArray = QRUtil.PATTERN_POSITION_TABLE[typeNumber - 1]

    fun getMask(maskPattern: Int, i: Int, j: Int): Boolean = when (maskPattern) {
        QRMaskPattern.PATTERN000 -> (i + j) % 2 == 0
        QRMaskPattern.PATTERN001 -> i % 2 == 0
        QRMaskPattern.PATTERN010 -> j % 3 == 0
        QRMaskPattern.PATTERN011 -> (i + j) % 3 == 0
        QRMaskPattern.PATTERN100 -> ((i / 2) + (j / 3)) % 2 == 0
        QRMaskPattern.PATTERN101 -> (i * j) % 2 + (i * j) % 3 == 0
        QRMaskPattern.PATTERN110 -> ((i * j) % 2 + (i * j) % 3) % 2 == 0
        QRMaskPattern.PATTERN111 -> ((i * j) % 3 + (i + j) % 2) % 2 == 0
        else -> error("bad maskPattern:$maskPattern")
    }

    fun getErrorCorrectPolynomial(errorCorrectLength: Int): QRPolynomial {
        var a = QRPolynomial(intArrayOf(1), 0)
        for (i in 0 until errorCorrectLength) a = a.multiply(QRPolynomial(intArrayOf(1, QRMath.gexp(i)), 0))
        return a
    }

    fun getLengthInBits(mode: Int, type: Int): Int = when (type) {
        in 1..9 -> when (mode) {
            QRMode.MODE_NUMBER -> 10; QRMode.MODE_ALPHA_NUM -> 9; QRMode.MODE_8BIT_BYTE -> 8; QRMode.MODE_KANJI -> 8
            else -> error("mode:$mode")
        }
        in 10..27 -> when (mode) {
            QRMode.MODE_NUMBER -> 12; QRMode.MODE_ALPHA_NUM -> 11; QRMode.MODE_8BIT_BYTE -> 16; QRMode.MODE_KANJI -> 10
            else -> error("mode:$mode")
        }
        in 28..41 -> when (mode) {
            QRMode.MODE_NUMBER -> 14; QRMode.MODE_ALPHA_NUM -> 13; QRMode.MODE_8BIT_BYTE -> 16; QRMode.MODE_KANJI -> 12
            else -> error("mode:$mode")
        }
        else -> error("type:$type")
    }

    fun getLostPoint(qrCode: QRCodeModel): Int {
        val moduleCount = qrCode.moduleCount
        var lostPoint = 0
        for (row in 0 until moduleCount) for (col in 0 until moduleCount) {
            var sameCount = 0
            val dark = qrCode.isDark(row, col)
            for (r in -1..1) {
                if (row + r < 0 || moduleCount <= row + r) continue
                for (c in -1..+1) {
                    if (col + c < 0 || moduleCount <= col + c) continue
                    if (r == 0 && c == 0) continue
                    if (dark == qrCode.isDark(row + r, col + c)) sameCount++
                }
            }
            if (sameCount > 5) lostPoint += (3 + sameCount - 5)
        }
        for (row in 0 until moduleCount - 1) for (col in 0 until moduleCount - 1) {
            var count = 0
            if (qrCode.isDark(row, col)) count++
            if (qrCode.isDark(row + 1, col)) count++
            if (qrCode.isDark(row, col + 1)) count++
            if (qrCode.isDark(row + 1, col + 1)) count++
            if (count == 0 || count == 4) lostPoint += 3
        }
        for (row in 0 until moduleCount) for (col in 0 until moduleCount - 6) {
            if (
                qrCode.isDark(row, col) &&
                !qrCode.isDark(row, col + 1) &&
                qrCode.isDark(row, col + 2) &&
                qrCode.isDark(row, col + 3) &&
                qrCode.isDark(row, col + 4) &&
                !qrCode.isDark(row, col + 5) &&
                qrCode.isDark(row, col + 6)
            ) {
                lostPoint += 40
            }
        }
        for (col in 0 until moduleCount) for (row in 0 until moduleCount - 6) {
            if (
                qrCode.isDark(row, col) && !qrCode.isDark(row + 1, col) &&
                qrCode.isDark(row + 2, col) &&
                qrCode.isDark(row + 3, col) &&
                qrCode.isDark(row + 4, col) &&
                !qrCode.isDark(row + 5, col) &&
                qrCode.isDark(row + 6, col)
            ) {
                lostPoint += 40
            }
        }
        var darkCount = 0
        for (col in 0 until moduleCount) for (row in 0 until moduleCount) {
            if (qrCode.isDark(row, col)) darkCount++
        }
        val ratio = abs(100 * darkCount / moduleCount / moduleCount - 50) / 5
        lostPoint += ratio * 10
        return lostPoint
    }
}


private object QRMath {
    fun glog(n: Int): Int = LOG[if (n >= 1) n else error("glog($n)")]
    fun gexp(n: Int): Int {
        var nn = n
        while (nn < 0) nn += 255
        while (nn >= 256) nn -= 255
        return EXP[nn]
    }

    private val EXP = IntArray(256).apply {
        for (i in 0 until 8) this[i] = 1 shl i
        for (i in 8 until 256) this[i] = this[i - 4] xor this[i - 5] xor this[i - 6] xor this[i - 8]
    }
    private val LOG = IntArray(256).apply {
        for (i in 0 until 255) this[EXP[i]] = i
    }
}

private class QRPolynomial(cnum: IntArray, shift: Int) {
    var num: IntArray = run {
        var offset = 0
        while (offset < cnum.size && cnum[offset] == 0) offset++
        val num = IntArray(cnum.size - offset + shift)
        for (i in 0 until cnum.size - offset) num[i] = cnum[i + offset]
        num
    }

    fun get(index: Int): Int = num[index]
    fun getLength(): Int = num.size

    fun multiply(e: QRPolynomial): QRPolynomial {
        val num = IntArray(getLength() + e.getLength() - 1)
        for (i in 0 until getLength()) {
            for (j in 0 until e.getLength()) {
                num[i + j] = num[i + j] xor (QRMath.gexp(QRMath.glog(this.get(i)) + QRMath.glog(e.get(j))))
            }
        }
        return QRPolynomial(num, 0)
    }

    fun mod(e: QRPolynomial): QRPolynomial {
        if (getLength() - e.getLength() < 0) return this
        val ratio = QRMath.glog(this.get(0)) - QRMath.glog(e.get(0))
        val num = IntArray(getLength())
        for (i in 0 until getLength()) num[i] = this.get(i)
        for (i in 0 until e.getLength()) num[i] = num[i] xor (QRMath.gexp(QRMath.glog(e.get(i)) + ratio))
        return QRPolynomial(num, 0).mod(e)
    }
}

private class QRRSBlock(val totalCount: Int, val dataCount: Int) {
    companion object {
        val RS_BLOCK_TABLE = listOf(
            intArrayOf(1, 26, 19),
            intArrayOf(1, 26, 16),
            intArrayOf(1, 26, 13),
            intArrayOf(1, 26, 9),
            intArrayOf(1, 44, 34),
            intArrayOf(1, 44, 28),
            intArrayOf(1, 44, 22),
            intArrayOf(1, 44, 16),
            intArrayOf(1, 70, 55),
            intArrayOf(1, 70, 44),
            intArrayOf(2, 35, 17),
            intArrayOf(2, 35, 13),
            intArrayOf(1, 100, 80),
            intArrayOf(2, 50, 32),
            intArrayOf(2, 50, 24),
            intArrayOf(4, 25, 9),
            intArrayOf(1, 134, 108),
            intArrayOf(2, 67, 43),
            intArrayOf(2, 33, 15, 2, 34, 16),
            intArrayOf(2, 33, 11, 2, 34, 12),
            intArrayOf(2, 86, 68),
            intArrayOf(4, 43, 27),
            intArrayOf(4, 43, 19),
            intArrayOf(4, 43, 15),
            intArrayOf(2, 98, 78),
            intArrayOf(4, 49, 31),
            intArrayOf(2, 32, 14, 4, 33, 15),
            intArrayOf(4, 39, 13, 1, 40, 14),
            intArrayOf(2, 121, 97),
            intArrayOf(2, 60, 38, 2, 61, 39),
            intArrayOf(4, 40, 18, 2, 41, 19),
            intArrayOf(4, 40, 14, 2, 41, 15),
            intArrayOf(2, 146, 116),
            intArrayOf(3, 58, 36, 2, 59, 37),
            intArrayOf(4, 36, 16, 4, 37, 17),
            intArrayOf(4, 36, 12, 4, 37, 13),
            intArrayOf(2, 86, 68, 2, 87, 69),
            intArrayOf(4, 69, 43, 1, 70, 44),
            intArrayOf(6, 43, 19, 2, 44, 20),
            intArrayOf(6, 43, 15, 2, 44, 16),
            intArrayOf(4, 101, 81),
            intArrayOf(1, 80, 50, 4, 81, 51),
            intArrayOf(4, 50, 22, 4, 51, 23),
            intArrayOf(3, 36, 12, 8, 37, 13),
            intArrayOf(2, 116, 92, 2, 117, 93),
            intArrayOf(6, 58, 36, 2, 59, 37),
            intArrayOf(4, 46, 20, 6, 47, 21),
            intArrayOf(7, 42, 14, 4, 43, 15),
            intArrayOf(4, 133, 107),
            intArrayOf(8, 59, 37, 1, 60, 38),
            intArrayOf(8, 44, 20, 4, 45, 21),
            intArrayOf(12, 33, 11, 4, 34, 12),
            intArrayOf(3, 145, 115, 1, 146, 116),
            intArrayOf(4, 64, 40, 5, 65, 41),
            intArrayOf(11, 36, 16, 5, 37, 17),
            intArrayOf(11, 36, 12, 5, 37, 13),
            intArrayOf(5, 109, 87, 1, 110, 88),
            intArrayOf(5, 65, 41, 5, 66, 42),
            intArrayOf(5, 54, 24, 7, 55, 25),
            intArrayOf(11, 36, 12),
            intArrayOf(5, 122, 98, 1, 123, 99),
            intArrayOf(7, 73, 45, 3, 74, 46),
            intArrayOf(15, 43, 19, 2, 44, 20),
            intArrayOf(3, 45, 15, 13, 46, 16),
            intArrayOf(1, 135, 107, 5, 136, 108),
            intArrayOf(10, 74, 46, 1, 75, 47),
            intArrayOf(1, 50, 22, 15, 51, 23),
            intArrayOf(2, 42, 14, 17, 43, 15),
            intArrayOf(5, 150, 120, 1, 151, 121),
            intArrayOf(9, 69, 43, 4, 70, 44),
            intArrayOf(17, 50, 22, 1, 51, 23),
            intArrayOf(2, 42, 14, 19, 43, 15),
            intArrayOf(3, 141, 113, 4, 142, 114),
            intArrayOf(3, 70, 44, 11, 71, 45),
            intArrayOf(17, 47, 21, 4, 48, 22),
            intArrayOf(9, 39, 13, 16, 40, 14),
            intArrayOf(3, 135, 107, 5, 136, 108),
            intArrayOf(3, 67, 41, 13, 68, 42),
            intArrayOf(15, 54, 24, 5, 55, 25),
            intArrayOf(15, 43, 15, 10, 44, 16),
            intArrayOf(4, 144, 116, 4, 145, 117),
            intArrayOf(17, 68, 42),
            intArrayOf(17, 50, 22, 6, 51, 23),
            intArrayOf(19, 46, 16, 6, 47, 17),
            intArrayOf(2, 139, 111, 7, 140, 112),
            intArrayOf(17, 74, 46),
            intArrayOf(7, 54, 24, 16, 55, 25),
            intArrayOf(34, 37, 13),
            intArrayOf(4, 151, 121, 5, 152, 122),
            intArrayOf(4, 75, 47, 14, 76, 48),
            intArrayOf(11, 54, 24, 14, 55, 25),
            intArrayOf(16, 45, 15, 14, 46, 16),
            intArrayOf(6, 147, 117, 4, 148, 118),
            intArrayOf(6, 73, 45, 14, 74, 46),
            intArrayOf(11, 54, 24, 16, 55, 25),
            intArrayOf(30, 46, 16, 2, 47, 17),
            intArrayOf(8, 132, 106, 4, 133, 107),
            intArrayOf(8, 75, 47, 13, 76, 48),
            intArrayOf(7, 54, 24, 22, 55, 25),
            intArrayOf(22, 45, 15, 13, 46, 16),
            intArrayOf(10, 142, 114, 2, 143, 115),
            intArrayOf(19, 74, 46, 4, 75, 47),
            intArrayOf(28, 50, 22, 6, 51, 23),
            intArrayOf(33, 46, 16, 4, 47, 17),
            intArrayOf(8, 152, 122, 4, 153, 123),
            intArrayOf(22, 73, 45, 3, 74, 46),
            intArrayOf(8, 53, 23, 26, 54, 24),
            intArrayOf(12, 45, 15, 28, 46, 16),
            intArrayOf(3, 147, 117, 10, 148, 118),
            intArrayOf(3, 73, 45, 23, 74, 46),
            intArrayOf(4, 54, 24, 31, 55, 25),
            intArrayOf(11, 45, 15, 31, 46, 16),
            intArrayOf(7, 146, 116, 7, 147, 117),
            intArrayOf(21, 73, 45, 7, 74, 46),
            intArrayOf(1, 53, 23, 37, 54, 24),
            intArrayOf(19, 45, 15, 26, 46, 16),
            intArrayOf(5, 145, 115, 10, 146, 116),
            intArrayOf(19, 75, 47, 10, 76, 48),
            intArrayOf(15, 54, 24, 25, 55, 25),
            intArrayOf(23, 45, 15, 25, 46, 16),
            intArrayOf(13, 145, 115, 3, 146, 116),
            intArrayOf(2, 74, 46, 29, 75, 47),
            intArrayOf(42, 54, 24, 1, 55, 25),
            intArrayOf(23, 45, 15, 28, 46, 16),
            intArrayOf(17, 145, 115),
            intArrayOf(10, 74, 46, 23, 75, 47),
            intArrayOf(10, 54, 24, 35, 55, 25),
            intArrayOf(19, 45, 15, 35, 46, 16),
            intArrayOf(17, 145, 115, 1, 146, 116),
            intArrayOf(14, 74, 46, 21, 75, 47),
            intArrayOf(29, 54, 24, 19, 55, 25),
            intArrayOf(11, 45, 15, 46, 46, 16),
            intArrayOf(13, 145, 115, 6, 146, 116),
            intArrayOf(14, 74, 46, 23, 75, 47),
            intArrayOf(44, 54, 24, 7, 55, 25),
            intArrayOf(59, 46, 16, 1, 47, 17),
            intArrayOf(12, 151, 121, 7, 152, 122),
            intArrayOf(12, 75, 47, 26, 76, 48),
            intArrayOf(39, 54, 24, 14, 55, 25),
            intArrayOf(22, 45, 15, 41, 46, 16),
            intArrayOf(6, 151, 121, 14, 152, 122),
            intArrayOf(6, 75, 47, 34, 76, 48),
            intArrayOf(46, 54, 24, 10, 55, 25),
            intArrayOf(2, 45, 15, 64, 46, 16),
            intArrayOf(17, 152, 122, 4, 153, 123),
            intArrayOf(29, 74, 46, 14, 75, 47),
            intArrayOf(49, 54, 24, 10, 55, 25),
            intArrayOf(24, 45, 15, 46, 46, 16),
            intArrayOf(4, 152, 122, 18, 153, 123),
            intArrayOf(13, 74, 46, 32, 75, 47),
            intArrayOf(48, 54, 24, 14, 55, 25),
            intArrayOf(42, 45, 15, 32, 46, 16),
            intArrayOf(20, 147, 117, 4, 148, 118),
            intArrayOf(40, 75, 47, 7, 76, 48),
            intArrayOf(43, 54, 24, 22, 55, 25),
            intArrayOf(10, 45, 15, 67, 46, 16),
            intArrayOf(19, 148, 118, 6, 149, 119),
            intArrayOf(18, 75, 47, 31, 76, 48),
            intArrayOf(34, 54, 24, 34, 55, 25),
            intArrayOf(20, 45, 15, 61, 46, 16)
        )

        fun getRSBlocks(typeNumber: Int, errorCorrectLevel: QRErrorCorrectLevel): List<QRRSBlock> {
            val rsBlock = QRRSBlock.getRsBlockTable(typeNumber, errorCorrectLevel)
            val length = rsBlock.size / 3
            val list = arrayListOf<QRRSBlock>()
            for (i in 0 until length) {
                val count = rsBlock[i * 3 + 0]
                val totalCount = rsBlock[i * 3 + 1]
                val dataCount = rsBlock[i * 3 + 2]
                for (j in 0 until count) list.add(QRRSBlock(totalCount, dataCount))
            }
            return list
        }

        fun getRsBlockTable(typeNumber: Int, errorCorrectLevel: QRErrorCorrectLevel): IntArray {
            return when (errorCorrectLevel) {
                QRErrorCorrectLevel.L -> QRRSBlock.RS_BLOCK_TABLE[(typeNumber - 1) * 4 + 0]
                QRErrorCorrectLevel.M -> QRRSBlock.RS_BLOCK_TABLE[(typeNumber - 1) * 4 + 1]
                QRErrorCorrectLevel.Q -> QRRSBlock.RS_BLOCK_TABLE[(typeNumber - 1) * 4 + 2]
                QRErrorCorrectLevel.H -> QRRSBlock.RS_BLOCK_TABLE[(typeNumber - 1) * 4 + 3]
            }
        }
    }
}

private class QRBitBuffer {
    var buffer = IntArrayList()
    var length = 0

    fun get(index: Int): Boolean = ((buffer[(index / 8)] ushr (7 - index % 8)) and 1) == 1

    fun put(num: Int, length: Int) =
        run { for (i in 0 until length) this.putBit(((num ushr (length - i - 1)) and 1) == 1) }

    fun getLengthInBits(): Int = length

    fun putBit(bit: Boolean) {
        val bufIndex = (length / 8)
        if (buffer.size <= bufIndex) buffer.add(0)
        if (bit) buffer[bufIndex] = buffer[bufIndex] or (0x80 ushr (length % 8))
        length++
    }
}
