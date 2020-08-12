package com.soywiz.korge.intellij.util

import com.intellij.codeInsight.completion.*
import com.intellij.ide.*
import com.intellij.openapi.application.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.soywiz.korge.intellij.completion.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.PathInfo
import kotlin.math.*

fun PsiElement.replace(text: String, context: InsertionContext? = null) {
	//context?.commitDocument()
	//document?.replaceString(start, end - 1, text)
    WriteAction.run<Throwable> {
        val range = this.textRange
        val start = range.startOffset
        val end = range.endOffset
        document?.replaceString(start, end, text)
    }
	//context?.commitDocument()
	//currentCaret?.moveToOffset(start + text.length)
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
	for (component in PathInfo(path).getPathComponents()) {
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
	for (component in PathInfo(path).getPathComponents()) {
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
val currentEditor get() = DataManager.getInstance().dataContextFromFocusAsync.blockingGet(10_000)!!.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.EDITOR)
val currentCaret get() = currentEditor?.caretModel
val currentCursor get() = currentCaret?.offset
