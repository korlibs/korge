package com.soywiz.korge.intellij.annotator

import com.intellij.codeInsight.intention.*
import com.intellij.codeInsight.intention.impl.*
import com.intellij.lang.annotation.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.ui.*
import com.intellij.util.ui.*
import com.soywiz.kds.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korim.color.*
import com.soywiz.korim.color.Colors
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

        fun gutter(rgba: RGBA) {
            val color = rgba.toAwt()
            val renderer = GutterColorRenderer(element, color)
            holder.newAnnotation(HighlightSeverity.INFORMATION, "Color")
                .withFix(object : BaseIntentionAction() {
                    override fun getText(): String = "Choose color..."
                    override fun getFamilyName(): String = text
                    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
                    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) = renderer.chooseColor()
                })
                .gutterIconRenderer(renderer)
                .create()
        }

        when (element) {
            is KtDotQualifiedExpression -> {
                val receiverExpression = element.receiverExpression
                val typeReceiver = receiverExpression.getType(context)?.fqName?.asString()
                val selectorExpression = element.selectorExpression
                val typeSelector = selectorExpression?.getType(context)?.fqName?.asString()

                if (typeReceiver == Colors::class.java.name && typeSelector == RGBA::class.java.name) {
                    if (selectorExpression is KtNameReferenceExpression) {
                        gutter(Colors[selectorExpression.getReferencedName()])
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
                                gutter(Colors[indexText])
                            }
                        }
                    }
                }
            }
        }
    }
}

private val COLORS_TO_NAMES = Colors.colorsByName.flip()

data class GutterColorRenderer(val element: PsiElement, val color: Color): GutterIconRenderer() {
    override fun getIcon(): Icon = ColorIcon(if (UIUtil.isRetina()) 24 else 12, color, true)

    fun chooseColor() {
        val editor = PsiUtilBase.findEditor(element) ?: return
        val newColor = ColorChooser.chooseColor(editor.component, "Choose Color", color, true, true)
        if (newColor != null) {
            val rgba = newColor.toRgba()
            val colorName = COLORS_TO_NAMES[rgba]
            val replacement = when {
                colorName != null -> "Colors.${colorName.toUpperCase()}"
                rgba.a == 0xFF -> "Colors[\"${rgba.hexStringNoAlpha}\"]"
                else -> "Colors[\"${rgba.hexString}\"]"
            }
            element.replace(replacement)
        }
    }

    override fun getClickAction(): AnAction? {
        return object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                chooseColor()
            }
        }
    }
}
