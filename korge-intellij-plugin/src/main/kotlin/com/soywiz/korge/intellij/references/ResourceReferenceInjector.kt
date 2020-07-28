package com.soywiz.korge.intellij.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.SoftFileReferenceSet
import com.intellij.psi.injection.ReferenceInjector
import com.intellij.util.ProcessingContext

class ResourceReferenceInjector : ReferenceInjector() {
    override fun getId(): String = "resource"

    override fun getDisplayName(): String = "Resource"

    override fun getReferences(element: PsiElement, context: ProcessingContext, range: TextRange): Array<out PsiReference> {
        val text: String = range.substring(element.text)
        return SoftFileReferenceSet(text, element, range.startOffset, null, true).allReferences
    }
}
