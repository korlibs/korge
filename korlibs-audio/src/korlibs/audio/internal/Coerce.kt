package korlibs.audio.internal

internal fun Float.toSampleShort(): Short = (this * Short.MAX_VALUE).coerceToShort()

internal fun Float.coerceToShort(): Short = this.toInt().coerceToShort()

internal fun Int.coerceToShort(): Short = this.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
internal fun Int.coerceToByte(): Byte = this.coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte()

@OptIn(ExperimentalUnsignedTypes::class)
internal fun Int.coerceToUShort(): UShort = this.coerceIn(UShort.MIN_VALUE.toInt(), UShort.MAX_VALUE.toInt()).toUShort()
@OptIn(ExperimentalUnsignedTypes::class)
internal fun Int.coerceToUByte(): UByte = this.coerceIn(UByte.MIN_VALUE.toInt(), UByte.MAX_VALUE.toInt()).toUByte()

@OptIn(ExperimentalUnsignedTypes::class)
internal fun Int.coerceToUShortAsInt(): Int = this.coerceIn(UShort.MIN_VALUE.toInt(), UShort.MAX_VALUE.toInt())
@OptIn(ExperimentalUnsignedTypes::class)
internal fun Int.coerceToUByteAsInt(): Int = this.coerceIn(UByte.MIN_VALUE.toInt(), UByte.MAX_VALUE.toInt())
