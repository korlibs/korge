package com.soywiz.korge.intellij

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ide.DataManager
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.virtualFile
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import com.soywiz.korio.vfs.PathInfo
import kotlin.math.min

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

		//val filePathContributor = FilePathCompletionContributor()

		extend(CompletionType.BASIC, PlatformPatterns.psiElement()
			.inVirtualFile(virtualFile().withExtension("pex"))
			.withLanguage(XMLLanguage.INSTANCE)
			.inside(
				XmlPatterns.xmlAttributeValue().inside(
					XmlPatterns.xmlTag().withName("texture")
				)
			)
			,
			object : CompletionProvider<CompletionParameters>() {
				override fun addCompletions(
					parameters: CompletionParameters,
					context: ProcessingContext,
					result: CompletionResultSet
				) {
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
			}
		)

		extend(CompletionType.BASIC, PlatformPatterns.psiElement()
			.inVirtualFile(virtualFile().withExtension("pex"))
			.withLanguage(XMLLanguage.INSTANCE)
			.withParent(XmlPatterns.xmlAttributeValue())
			,
			object : CompletionProvider<CompletionParameters>() {
				override fun addCompletions(
					parameters: CompletionParameters,
					context: ProcessingContext,
					result: CompletionResultSet
				) {
					result.stopHere()
				}
			}
		)

		extend(CompletionType.BASIC, PlatformPatterns.psiElement()
			.inVirtualFile(virtualFile().withExtension("pex"))
			.withLanguage(XMLLanguage.INSTANCE)
			.withParent(
				XmlPatterns.xmlAttribute()
			),
			object : CompletionProvider<CompletionParameters>() {
				override fun addCompletions(
					parameters: CompletionParameters,
					context: ProcessingContext,
					result: CompletionResultSet
				) {
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
			}
		)

		extend(CompletionType.BASIC, PlatformPatterns.psiElement()
			.inVirtualFile(virtualFile().withExtension("pex"))
			.withLanguage(XMLLanguage.INSTANCE)
			.withParent(
				XmlPatterns.xmlTag().withName("particleEmitterConfig")
			),
			object : CompletionProvider<CompletionParameters>() {
				override fun addCompletions(
					parameters: CompletionParameters,
					context: ProcessingContext,
					result: CompletionResultSet
				) {
					for (tag in PexTags.values()) {
						result.addElement(LookupElementBuilder.create(tag.name).withTailText("${tag.type}"))
					}
					result.stopHere()
				}
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

fun PsiElement.getTextToCaret(editor: Editor): String {
	val textRange = this.textRange
	return editor.document.getText(TextRange(textRange.startOffset, min(textRange.endOffset, editor.caretModel.offset)))
}

val PsiElement.textWithoutDummy: String
	get() {
		val out = this.text
		if (out.endsWith(CompletionInitializationContext.DUMMY_IDENTIFIER)) {
			return out.substring(0, out.length - CompletionInitializationContext.DUMMY_IDENTIFIER.length)
		}
		//else if (out.endsWith(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED)) {
		//	return out.substring(0, out.length - CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED.length)
		//}
		else {
			return out
		}
	}

fun PsiFileSystemItem.getSpecial(path: String, nearest: Boolean = false): PsiFileSystemItem? {
	var current: PsiFileSystemItem? = this
	var lastValid: PsiFileSystemItem = this
	for (component in PathInfo(path).getComponents()) {
		when (component) {
			".", "" -> {
			}
			".." -> {
				current = current?.parent
			}
			else -> {
				if (current is PsiDirectory) {
					current = current.findSubdirectory(component) ?: current.findFile(component)
				} else {
					current = null
				}
			}
		}
		if (current != null) {
			lastValid = current
		}
	}
	return if (nearest) lastValid else current
}

operator fun PsiFileSystemItem.get(path: String): PsiFileSystemItem? {
	var current: PsiFileSystemItem? = this
	for (component in PathInfo(path).getComponents()) {
		when (component) {
			".", "" -> {
			}
			".." -> {
				current = current?.parent
			}
			else -> {
				if (current is PsiDirectory) {
					current = current.findSubdirectory(component) ?: current.findFile(component)
				} else {
					current = null
				}
			}
		}
	}
	return current
}

val PsiElement.document get() = PsiDocumentManager.getInstance(this.project).getDocument(this.containingFile)
val currentEditor get() = DataManager.getInstance().dataContextFromFocus.result.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.EDITOR)
val currentCaret get() = currentEditor?.caretModel
val currentCursor get() = currentCaret?.offset

fun PsiElement.replace(text: String, context: InsertionContext? = null) {
	val range = this.textRange
	val start = range.startOffset
	val end = range.endOffset
	//context?.commitDocument()
	document?.replaceString(start, end - 1, text)
	//context?.commitDocument()
	//currentCaret?.moveToOffset(start + text.length)
}
