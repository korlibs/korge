package com.soywiz.korge.intellij.debug

import com.intellij.debugger.engine.*
import com.intellij.debugger.engine.evaluation.*
import com.intellij.debugger.ui.impl.watch.*
import com.intellij.debugger.ui.tree.*
import com.intellij.debugger.ui.tree.render.*
import com.intellij.util.ui.*
import com.intellij.xdebugger.frame.*
import com.soywiz.korim.awt.*
import com.sun.jdi.*
import org.intellij.images.editor.impl.*
import java.awt.*
import javax.swing.*

class KorimBitmapDebugNodeRenderer
	: CompoundReferenceRenderer("Bitmap", null, null), FullValueEvaluatorProvider {
	companion object {
		const val NAME = "KorimBitmapDebugNodeRenderer"
	}

	override fun isApplicable(type: Type?): Boolean = type?.isKorimBitmapOrDrawable() ?: false
	override fun isEnabled(): Boolean = true
	override fun getUniqueId(): String = NAME

	/*
	override fun calcLabel(descriptor: ValueDescriptor, evaluationContext: EvaluationContext, listener: DescriptorLabelListener): String {
		val value = descriptor.value
		val thread = evaluationContext.suspendContext.thread?.threadReference
		/*
		try {
			if (value is ObjectReference) {
				val width = value.invoke("getWidth", listOf(), thread = thread).int()
				val height = value.invoke("getHeight", listOf(), thread = thread).int()
				return "${width}x${height}"
			}
		} catch (e: Throwable) {
			//e.printStackTrace()
		}
		*/
		try {
			if (value is ObjectReference) {
				return value.invoke("toString", listOf(), thread = thread).toString()
			}
		} catch (e: Throwable) {
			//e.printStackTrace()
		}
		return value.toString()
	}
	 */

	override fun hasOverhead(): Boolean = true

	override fun calcValueIcon(descriptor: ValueDescriptor, evaluationContext: EvaluationContext, listener: DescriptorLabelListener): Icon? {
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

	internal abstract class IconPopupEvaluator(linkText: String, evaluationContext: EvaluationContextImpl) :
		CustomPopupFullValueEvaluator<Icon?>(linkText, evaluationContext) {
		override fun createComponent(icon: Icon?): JComponent {
			if (icon == null) return JLabel("No data", SwingConstants.CENTER)
			val w = icon.iconWidth
			val h = icon.iconHeight
			val image = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.defaultScreenDevice.defaultConfiguration.createCompatibleImage(w, h, Transparency.TRANSLUCENT)
			val g = image.createGraphics()
			icon.paintIcon(null, g, 0, 0)
			g.dispose()
			return ImageEditorManagerImpl.createImageEditorUI(image)
		}
	}
}