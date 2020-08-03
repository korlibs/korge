package com.soywiz.korge.intellij.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.lang.xml.*
import com.intellij.openapi.util.*
import com.intellij.patterns.*
import com.intellij.patterns.PlatformPatterns.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.psi.xml.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.util.*

class PexCompletionContributor : CompletionContributor() {
	companion object {
		enum class Type(vararg val attributes: String) {
			NAME("name"), VECTOR("x", "y"), COLOR("red", "green", "blue", "alpha"), VALUE("value")
		}

		enum class PexTags(val type: Type) {
			texture(Type.NAME),

			sourcePosition(Type.VECTOR),
			sourcePositionVariance(Type.VECTOR),
			gravity(Type.VECTOR),

			startColor(Type.COLOR),
			startColorVariance(Type.COLOR),
			finishColor(Type.COLOR),
			finishColorVariance(Type.COLOR),

			speed(Type.VALUE),
			speedVariance(Type.VALUE),
			particleLifeSpan(Type.VALUE),
			particleLifespanVariance(Type.VALUE),
			angle(Type.VALUE),
			angleVariance(Type.VALUE),
			radialAcceleration(Type.VALUE),
			tangentialAcceleration(Type.VALUE),
			radialAccelVariance(Type.VALUE),
			tangentialAccelVariance(Type.VALUE),
			maxParticles(Type.VALUE),
			startParticleSize(Type.VALUE),
			startParticleSizeVariance(Type.VALUE),
			finishParticleSize(Type.VALUE),
			FinishParticleSizeVariance(Type.VALUE),
			duration(Type.VALUE),
			emitterType(Type.VALUE),
			maxRadius(Type.VALUE),
			maxRadiusVariance(Type.VALUE),
			minRadius(Type.VALUE),
			minRadiusVariance(Type.VALUE),
			rotatePerSecond(Type.VALUE),
			rotatePerSecondVariance(Type.VALUE),
			blendFuncSource(Type.VALUE),
			blendFuncDestination(Type.VALUE),
			rotationStart(Type.VALUE),
			rotationStartVariance(Type.VALUE),
			rotationEnd(Type.VALUE),
			rotationEndVariance(Type.VALUE);

			companion object {
				val BY_NAME_LC = values().map { it.name.toLowerCase() to it }.toMap()
			}
		}
	}

	init {
		fun emptyObj() = object {
			override fun toString(): String = ""
		}
		
		val PEX_PATTERN = PlatformPatterns.psiElement()
			.inVirtualFile(virtualFile().withExtension("pex"))
			.withLanguage(XMLLanguage.INSTANCE)

		extend(CompletionType.BASIC, PEX_PATTERN
			.inside(
				XmlPatterns.xmlAttributeValue().inside(
					XmlPatterns.xmlTag().withName("texture")
				)
			)
			,
			CompletionProvider { parameters, context, result ->
				val pos = parameters.position
				val posTextRange = pos.textRange
				val posTextRangeStartOffset = posTextRange.startOffset
				val root = pos.getTextToCaret(parameters.editor)
				val file = pos.containingFile.originalFile.containingDirectory
				val directory = file.getSpecial(root, nearest = true) as? PsiDirectory?
				//println("directory: $directory")
				if (directory != null) {
					val replaceRange = TextRange(posTextRangeStartOffset, posTextRangeStartOffset + root.length)
					val directoryIcon = directory.getIcon(0)
					for (file in directory.files) {
						val fullFileName = (root.trim('/') + "/${file.name}").trim('/')
						result.addElement(
							LookupElementBuilder.create(emptyObj()).withTailText(fullFileName).withIcon(
								file.getIcon(0)
							).withInsertHandler(ReplaceInsertHandler(replaceRange, fullFileName))
						)
					}
					for (file in listOf(".", "..")) {
						val fullFileName = (root.trim('/') + "/${file}").trim('/')
						result.addElement(
							LookupElementBuilder.create(emptyObj()).withTailText(fullFileName).withIcon(
								directoryIcon
							).withInsertHandler(ReplaceInsertHandler(replaceRange, fullFileName))
						)
					}
				}
				result.stopHere()
			}

		)

		extend(CompletionType.BASIC, PEX_PATTERN
			.withParent(XmlPatterns.xmlAttributeValue())
			,
			CompletionProvider { parameters, context, result ->
				result.stopHere()
			}
		)

		extend(CompletionType.BASIC, PEX_PATTERN
			.withParent(
				XmlPatterns.xmlAttribute()
			),
			CompletionProvider { parameters, context, result ->
				val tag = PsiTreeUtil.getParentOfType(parameters.position, XmlTag::class.java)
				if (tag != null) {
					val tagName = tag.name.toLowerCase()
					val tagInfo = PexTags.BY_NAME_LC[tagName]
					if (tagInfo != null) {
						for (attribute in tagInfo.type.attributes) {
							result.addElement(LookupElementBuilder.create(attribute))
						}
						result.stopHere()
					}
				}
			}
		)

		extend(CompletionType.BASIC, PEX_PATTERN
			.withLanguage(XMLLanguage.INSTANCE)
			.withSuperParent(2, XmlPatterns.xmlTag().withName("particleEmitterConfig"))
			,CompletionProvider { parameters, context, result ->
				//println("particleEmitterConfig")
				for (tag in PexTags.values()) {
					result.addElement(LookupElementBuilder.create(tag.name).withTypeText("${tag.type}"))
					//result.addElement(LookupElementBuilder.create(tag.name))
				}
				result.stopHere()
			}
		)
	}
}

class ReplaceInsertHandler(val replaceRange: TextRange, val replaceText: String) : InsertHandler<LookupElement> {
	override fun handleInsert(context: InsertionContext, item: LookupElement) {
		context.commitDocument()

		val editor = context.editor
		val document = editor.document
		val file = context.file
		val element = file.findElementAt(editor.caretModel.offset)

//
		//context.setAddCompletionChar(false)
		//document.insertString(editor.caretModel.offset, "HELLO[A]")
		//context.commitDocument()
		//document.insertString(editor.caretModel.offset, "HELLO[B]")

		document.replaceString(replaceRange.startOffset, replaceRange.endOffset, replaceText)
		editor.caretModel.moveToOffset(replaceRange.startOffset + replaceText.length)

		//element?.replace(replaceText, context)
		//item.
		//item?.psiElement?.replace(file.name)
	}
}

