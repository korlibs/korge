package com.soywiz.korim.font

actual val nativeSystemFontProvider: NativeSystemFontProvider = FallbackNativeSystemFontProvider(DefaultTtfFont)

//override fun renderText(
//    state: Context2d.State,
//    font: Font,
//    fontSize: Double,
//    text: String,
//    x: Double,
//    y: Double,
//    fill: Boolean
//) {
//    val metrics = TextMetrics()
//    val bounds = metrics.bounds
//    paint.typeface = Typeface.create(font.name, Typeface.NORMAL)
//    paint.textSize = fontSize.toFloat()
//    val fm = paint.fontMetrics
//    getBounds(font, fontSize, text, metrics)
//
//    val baseline = fm.ascent + fm.descent
//
//    val ox = state.horizontalAlign.getOffsetX(bounds.width)
//    val oy = state.verticalAlign.getOffsetY(bounds.height, baseline.toDouble())
//
//    //val tp = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
//
//    canvas.drawText(text, 0, text.length, (x - ox).toFloat(), (y + baseline - oy).toFloat(), paint)
//}

//override fun getBounds(font: Font, size: Double, text: String, out: TextMetrics) {
//    val rect = Rect()
//    paint.getTextBounds(text, 0, text.length, rect)
//    out.bounds.setTo(rect.left.toDouble(), rect.top.toDouble(), rect.width().toDouble(), rect.height().toDouble())
//}
