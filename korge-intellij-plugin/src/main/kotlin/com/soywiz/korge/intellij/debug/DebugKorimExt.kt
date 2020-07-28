package com.soywiz.korge.intellij.debug

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.sun.jdi.*

fun Type.isKorimBitmapOrDrawable() = this.instanceOf<Bitmap>() || this.instanceOf<BmpSlice>() || this.instanceOf<Drawable>() || this.instanceOf<com.soywiz.korge.view.Image>()

fun ObjectReference.readKorimBitmap32(hintWidth: Int, hintHeight: Int, thread: ThreadReference?): Bitmap32 {
	val value = this
	val type = value.type()
	return when {
		type.instanceOf<Bitmap>() -> readKorimBitmap32Internal(thread)
		type.instanceOf<BmpSlice>() -> readKorimBmpSliceInternal(thread)
		type.instanceOf<com.soywiz.korge.view.Image>() -> readKorgeImageInternal(thread)
		type.instanceOf<Drawable>() -> {
			val isSizedDrawable = type.instanceOf<SizedDrawable>()
			val width = if (isSizedDrawable) value.invoke("getWidth", listOf(), thread = thread).int(hintWidth) else hintWidth
			val height = if (isSizedDrawable) value.invoke("getHeight", listOf(), thread = thread).int(hintHeight) else hintHeight
			readKorimDrawableInternal(width, height, thread)
		}
		else -> error("Can't interpret $this object as Bitmap or Context2d.Drawable")
	}
}

fun ObjectReference.readKorgeImageInternal(thread: ThreadReference?): Bitmap32 {
	val value = this
	if (!value.type().instanceOf<com.soywiz.korge.view.Image>()) error("Not a com.soywiz.korge.view.Image")
	return (value.invoke("getBitmap", thread = thread) as ObjectReference).readKorimBmpSliceInternal(thread)
}

fun ObjectReference.readKorimBmpSliceInternal(thread: ThreadReference?): Bitmap32 {
	val value = this
	if (!value.type().instanceOf<BmpSlice>()) error("Not a korim BmpSlice")
	val left = value.invoke("getLeft", thread = thread).int(0)
	val top = value.invoke("getTop", thread = thread).int(0)
	val width = value.invoke("getWidth", thread = thread).int(0)
	val height = value.invoke("getHeight", thread = thread).int(0)
	return (value.invoke("getBmp", listOf(), thread = thread) as ObjectReference).readKorimBitmap32Internal(thread).sliceWithSize(left, top, width, height).extract()
}

fun ObjectReference.readKorimBitmap32Internal(thread: ThreadReference?): Bitmap32 {
	val value = this
	if (!value.type().instanceOf<Bitmap>()) error("Not a korim Bitmap")
	val width = value.invoke("getWidth", listOf(), thread = thread).int(0)
	val height = value.invoke("getHeight", listOf(), thread = thread).int(0)
	val premultiplied = value.invoke("getPremultiplied", listOf(), thread = thread).bool(false)
	val bmp32Mirror = value.invoke("toBMP32", listOf(), thread = thread) as ObjectReference
	val dataInstance = (bmp32Mirror.invoke("getData", listOf(), thread = thread) as ObjectReference).debugToLocalInstanceViaSerialization(thread = thread) as IntArray
	return Bitmap32(width, height, RgbaArray(dataInstance.copyOf()), premultiplied)
}

fun ObjectReference.readKorimDrawableInternal(requestedWidth: Int, requestedHeight: Int, thread: ThreadReference?): Bitmap32 {
	val value = this
	val type = value.type()
	val vm = virtualMachine()
	if (!type.instanceOf<Drawable>()) error("Not a korim Context2d.Drawable")
	val isSizedDrawable = type.instanceOf<SizedDrawable>()
	val isBoundsDrawable = type.instanceOf<BoundsDrawable>()

	val width = if (isSizedDrawable) value.invoke("getWidth", thread = thread).int(requestedWidth) else requestedWidth
	val height = if (isSizedDrawable) value.invoke("getHeight", thread = thread).int(requestedHeight) else requestedHeight
	val (top, left) = when {
		isBoundsDrawable -> {
			val rectangle = value.invoke("getBounds", thread = thread) as ObjectReference
			listOf(rectangle.invoke("getTop", thread = thread).int(0), rectangle.invoke("getLeft", thread = thread).int(0))
		}
		else -> {
			listOf(0, 0)
		}
	}

	virtualMachine().process()
	val clazz = virtualMachine().getRemoteClass("com.soywiz.korim.format.NativeImageFormatProviderJvmKt", thread = thread) ?: error("Can't find NativeImageFormatProviderJvmKt")
	val nativeImageFormatProvider = clazz.invoke("getNativeImageFormatProvider", listOf(), thread = thread) as? ObjectReference? ?: error("Error calling getNativeImageFormatProvider")
	val image = nativeImageFormatProvider.invoke("create", listOf(vm.mirrorOf(width), vm.mirrorOf(height)), thread = thread) as? ObjectReference? ?: error("Error calling create")
	val ctx2d = image.invoke("getContext2d", listOf(vm.mirrorOf(false)), thread = thread) as? ObjectReference? ?: error("Error calling getContext2d")
	ctx2d.invoke("translate", listOf(vm.mirrorOf(-left.toDouble()), vm.mirrorOf(-top.toDouble())), thread = thread, signature = "(DD)Lcom/soywiz/korma/geom/Matrix;")
	ctx2d.invoke("draw", listOf(value), thread = thread)
	//NativeImage(100, 100).getContext2d().translate()
	return image.readKorimBitmap32Internal(thread)
}