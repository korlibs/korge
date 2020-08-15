package com.soywiz.korge.intellij.debug

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.sun.jdi.*

fun Type.isKorimBitmapOrDrawable() = this.instanceOf<Bitmap>() || this.instanceOf<BmpSlice>() || this.instanceOf<Drawable>() || this.instanceOf<com.soywiz.korge.view.Image>()

fun ObjectReference.readKorimBitmap32(hintWidth: Int, hintHeight: Int, thread: ThreadReference?): Bitmap32 {
	return when {
		instanceOf<Bitmap>() -> readKorimBitmap32Internal(thread)
		instanceOf<BmpSlice>() -> readKorimBmpSliceInternal(thread)
		instanceOf<com.soywiz.korge.view.Image>() -> readKorgeImageInternal(thread)
		instanceOf<Drawable>() -> {
			val isSizedDrawable = instanceOf<SizedDrawable>()
			val width = if (isSizedDrawable) this.invoke("getWidth", listOf(), thread = thread).int(hintWidth) else hintWidth
			val height = if (isSizedDrawable) this.invoke("getHeight", listOf(), thread = thread).int(hintHeight) else hintHeight
			readKorimDrawableInternal(width, height, thread)
		}
		else -> error("Can't interpret $this object as Bitmap or Context2d.Drawable")
	}
}

fun ObjectReference.readKorgeImageInternal(thread: ThreadReference?): Bitmap32 {
	val value = this
	if (!value.instanceOf<com.soywiz.korge.view.Image>()) error("Not a com.soywiz.korge.view.Image")
	return (value.invoke("getBitmap", thread = thread) as ObjectReference).readKorimBmpSliceInternal(thread)
}

fun ObjectReference.readKorimBmpSliceInternal(thread: ThreadReference?): Bitmap32 {
	val value = this
	if (!value.instanceOf<BmpSlice>()) error("Not a korim BmpSlice")
	val left = value.invoke("getLeft", thread = thread).int(0)
	val top = value.invoke("getTop", thread = thread).int(0)
	val width = value.invoke("getWidth", thread = thread).int(0)
	val height = value.invoke("getHeight", thread = thread).int(0)
	return (value.invoke("getBmp", listOf(), thread = thread) as ObjectReference).readKorimBitmap32Internal(thread).sliceWithSize(left, top, width, height).extract()
}

fun ObjectReference.readKorimBitmap32Internal(thread: ThreadReference?): Bitmap32 {
	val value = this
	if (!value.instanceOf<Bitmap>()) error("Not a korim Bitmap")
	val width = value.invoke("getWidth", listOf(), thread = thread).int(0)
	val height = value.invoke("getHeight", listOf(), thread = thread).int(0)
	val premultiplied = value.invoke("getPremultiplied", listOf(), thread = thread).bool(false)
	val bmp32Mirror = value.invoke("toBMP32", listOf(), thread = thread) as ObjectReference
    val getDataMethod = bmp32Mirror.getMethod("getIntData")
        ?: bmp32Mirror.getMethod("getData")
        ?: bmp32Mirror.referenceType().allMethods().firstOrNull { it.name().startsWith("getData") }
        ?: error("Can't find suitable Bitmap32.getData method")
	val dataInstance = (bmp32Mirror.invoke(getDataMethod.name(), listOf(), thread = thread) as ObjectReference).debugToLocalInstanceViaSerialization(thread = thread) as IntArray
	return Bitmap32(width, height, RgbaArray(dataInstance.copyOf()), premultiplied)
}

fun ObjectReference.readKorimDrawableInternal(requestedWidth: Int, requestedHeight: Int, thread: ThreadReference?): Bitmap32 {
	val value = this
	val vm = virtualMachine()
	if (!value.instanceOf<Drawable>()) error("Not a korim Context2d.Drawable")
	val isSizedDrawable = value.instanceOf<SizedDrawable>()
	val isBoundsDrawable = value.instanceOf<BoundsDrawable>()

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
