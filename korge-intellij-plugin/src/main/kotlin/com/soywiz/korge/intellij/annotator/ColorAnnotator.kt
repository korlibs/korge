package com.soywiz.korge.intellij.annotator

import com.intellij.lang.annotation.*
import com.intellij.openapi.editor.markup.*
import com.intellij.psi.*
import com.intellij.util.ui.*
import com.soywiz.korim.color.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.refactoring.fqName.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.lazy.*
import java.awt.*
import javax.swing.*

class ColorAnnotator: Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtElement) return

        val context = element.analyze(BodyResolveMode.PARTIAL)

        when (element) {
            is KtDotQualifiedExpression -> {
                val receiverExpression = element.receiverExpression
                val typeReceiver = receiverExpression.getType(context)?.fqName?.asString()
                val selectorExpression = element.selectorExpression
                val typeSelector = selectorExpression?.getType(context)?.fqName?.asString()

                if (typeReceiver == Colors::class.java.name && typeSelector == RGBA::class.java.name) {
                    if (selectorExpression is KtNameReferenceExpression) {
                        val name = selectorExpression.getReferencedName()
                        val rgba = Colors[name]
                        holder.newAnnotation(HighlightSeverity.INFORMATION, "Color")
                            .gutterIconRenderer(GutterColorRenderer(rgba.toAwt()))
                            .create()
                    }
                }
            }
            is KtArrayAccessExpression -> {
                if (element.indexExpressions.size == 1) {
                    val indexExpression = element.indexExpressions.firstOrNull()
                    if (indexExpression is KtStringTemplateExpression && !indexExpression.hasInterpolation()) {
                        val indexEntries = indexExpression.entries
                        if (indexEntries.size == 1 && indexEntries[0] is KtLiteralStringTemplateEntry) {
                            val indexEntry = indexEntries[0] as KtLiteralStringTemplateEntry
                            val indexText = indexEntry.text
                            val arrayExpression = element.arrayExpression
                            val typeArray = arrayExpression?.getType(context)?.fqName?.asString()
                            if (typeArray == Colors::class.java.name) {
                                val rgba = Colors[indexText]
                                holder.newAnnotation(HighlightSeverity.INFORMATION, "Color")
                                    .gutterIconRenderer(GutterColorRenderer(rgba.toAwt()))
                                    .create()
                            }
                        }

                    }
                }
            }
        }
    }
}


data class GutterColorRenderer(val color: Color): GutterIconRenderer() {
    override fun getIcon(): Icon = ColorIcon(if (UIUtil.isRetina()) 24 else 12, color, true)
}
