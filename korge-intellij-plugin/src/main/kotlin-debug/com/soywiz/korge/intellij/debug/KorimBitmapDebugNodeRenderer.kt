package com.soywiz.korge.intellij.debug

import com.intellij.debugger.engine.*
import com.intellij.debugger.engine.evaluation.*
import com.intellij.debugger.ui.impl.watch.*
import com.intellij.debugger.ui.tree.*
import com.intellij.debugger.ui.tree.render.*
import com.intellij.util.ui.*
import com.intellij.xdebugger.frame.*
import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.vector.*
import com.sun.jdi.*
import java.awt.*
import javax.swing.*
import kotlin.reflect.*

class KorimBitmapDebugNodeRenderer : BaseKorimBitmapDebugNodeRenderer(Bitmap::class)
class KorimBmpSliceDebugNodeRenderer : BaseKorimBitmapDebugNodeRenderer(BmpSlice::class)
class KorimDrawableDebugNodeRenderer : BaseKorimBitmapDebugNodeRenderer(Drawable::class)
class KorimKorgeViewImageDebugNodeRenderer : BaseKorimBitmapDebugNodeRenderer(com.soywiz.korge.view.Image::class)

abstract class BaseKorimBitmapDebugNodeRenderer(val applicableClass: KClass<*>) : CompoundReferenceRenderer(applicableClass.simpleName!!, null, null), FullValueEvaluatorProvider {
    //val uniqueIdName = this::class.simpleName!!
    val uniqueIdName = "Korim${applicableClass.simpleName}DebugNodeRenderer"
	//companion object { const val NAME = "KorimBitmapDebugNodeRenderer" }

    //override fun getClassName(): String = applicableClass.qualifiedName!!.replace("/", ".")
    override fun getClassName(): String = applicableClass.qualifiedName!!
    //override fun isApplicable(type: Type?): Boolean {
    //    val applicable = type?.isKorimBitmapOrDrawable() ?: false
    //    println("BaseKorimBitmapDebugNodeRenderer.isApplicable: $type, applicable=$applicable")
    //    return applicable
    //}
	override fun isEnabled(): Boolean = true
	override fun getUniqueId(): String = uniqueIdName
    override fun hasOverhead(): Boolean = true


    //override fun hasOverhead(): Boolean = true

	override fun calcValueIcon(descriptor: ValueDescriptor, evaluationContext: EvaluationContext, listener: DescriptorLabelListener): Icon? {
        //println("calcValueIcon: $descriptor, $evaluationContext, $listener")
		try {
			val value = descriptor.value
			val type = value.type()
			val thread = evaluationContext.suspendContext.thread?.threadReference
			if (value is ObjectReference) {
				if (type.isKorimBitmapOrDrawable()) {
					val bmp32 = value.readKorimBitmap32(16, 16, thread)
					return ImageIcon(bmp32.toAwt().getScaledInstance(16, 16, Image.SCALE_SMOOTH))
				}
			}
		} catch (e: Throwable) {
			e.printStackTrace()
		}
		return null
	}

	override fun getFullValueEvaluator(evaluationContext: EvaluationContextImpl, valueDescriptor: ValueDescriptorImpl): XFullValueEvaluator? {
        //println("getFullValueEvaluator: $evaluationContext, $valueDescriptor")
		return object : IconPopupEvaluator("\u2026 Show bitmap", evaluationContext) {
			override fun getData(): Icon? {
				val value = valueDescriptor.value
				val type = value.type()
				val thread = evaluationContext.suspendContext.thread?.threadReference
				if (value is ObjectReference && type.isKorimBitmapOrDrawable()) {
					val bmp32 = value.readKorimBitmap32(512, 512, thread)
					return ImageIcon(bmp32.toAwt())
				}
				return EmptyIcon.create(16, 16)
			}
		}
	}

	internal abstract class IconPopupEvaluator(linkText: String, evaluationContext: EvaluationContextImpl) : CustomPopupFullValueEvaluator<Icon?>(linkText, evaluationContext) {
		override fun createComponent(icon: Icon?): JComponent {
			if (icon == null) return JLabel("No data", SwingConstants.CENTER)
			val w = icon.iconWidth
			val h = icon.iconHeight
			val image = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.defaultScreenDevice.defaultConfiguration.createCompatibleImage(w, h, Transparency.TRANSLUCENT)
			val g = image.createGraphics()
			icon.paintIcon(null, g, 0, 0)
			g.dispose()
            @Suppress("INACCESSIBLE_TYPE")
            return org.intellij.images.editor.impl.ImageEditorManagerImpl.createImageEditorUI(image)
		}
	}
}
