package com.soywiz.korge.intellij.editor

import com.intellij.ide.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ide.*
import com.soywiz.korio.async.*
import com.soywiz.korio.serialization.xml.*
import java.awt.*
import javax.swing.*

class RootComponent(val korgeBaseKorgeFileEditor: KorgeBaseKorgeFileEditor) : JPanel(BorderLayout()), DataProvider, CopyProvider, PasteProvider, DeleteProvider, CutProvider {
    val project get() = korgeBaseKorgeFileEditor.project
    val fileToEdit get() = korgeBaseKorgeFileEditor.fileToEdit
    val viewsDebuggerActions get() = korgeBaseKorgeFileEditor.viewsDebuggerActions

    override fun getData(dataId: String): Any? {
        //println("FakeCopyAndPasteProvider.getData('$dataId')")
        //CopyPasteDelegator(project, this)
        when {
            CommonDataKeys.PROJECT.`is`(dataId) -> return project
            PlatformDataKeys.COPY_PROVIDER.`is`(dataId) -> return this
            PlatformDataKeys.PASTE_PROVIDER.`is`(dataId) -> return this
            PlatformDataKeys.DELETE_ELEMENT_PROVIDER.`is`(dataId) -> return this
            PlatformDataKeys.CUT_PROVIDER.`is`(dataId) -> return this
            CommonDataKeys.VIRTUAL_FILE.`is`(dataId) -> return fileToEdit.originalFile

            /*
            CommonDataKeys.VIRTUAL_FILE_ARRAY.`is`(dataId) -> return if (editor != null) arrayOf(editor!!.getFile()) else VirtualFile.EMPTY_ARRAY
            CommonDataKeys.PSI_FILE.`is`(dataId) -> return findPsiFile()
            CommonDataKeys.PSI_ELEMENT.`is`(dataId) -> return findPsiFile()
            LangDataKeys.PSI_ELEMENT_ARRAY.`is`(dataId) -> {
                val psi = findPsiFile()
                return if (psi != null) arrayOf<PsiElement>(psi) else PsiElement.EMPTY_ARRAY
            }
            ImageComponentDecorator.DATA_KEY.`is`(dataId) -> return if (editor != null) editor else this
            */
            else -> return null
        }
    }

    override fun deleteElement(dataContext: DataContext) {
        viewsDebuggerActions?.removeCurrentNode()
    }

    override fun canDeleteElement(dataContext: DataContext): Boolean {
        return viewsDebuggerActions?.canDeleteCopyCut() == true
    }

    override fun performCut(dataContext: DataContext) {
        performCopy(dataContext)
        deleteElement(dataContext)
    }

    override fun isCutEnabled(dataContext: DataContext): Boolean {
        return viewsDebuggerActions?.canDeleteCopyCut() == true
    }

    override fun isCutVisible(dataContext: DataContext): Boolean {
        return true
    }

    override fun performPaste(dataContext: DataContext) {
        val actions = viewsDebuggerActions ?: return
        val xml = CopyPasteManager.getInstance().getContents<Xml>(KTreeTransferable.FLAVOR) ?: return
        launchImmediately(actions.views.coroutineContext) {
            println("PASTE: $xml")
            actions.pasteFromXml(xml)
        }
    }

    override fun isPastePossible(dataContext: DataContext): Boolean {
        return viewsDebuggerActions?.canPaste() == true
    }

    override fun isPasteEnabled(dataContext: DataContext): Boolean {
        return viewsDebuggerActions?.canPaste() == true
    }

    override fun performCopy(dataContext: DataContext) {
        val actions = viewsDebuggerActions ?: return
        val xml = actions.copyToXml() ?: return
        println("COPY: $xml")
        CopyPasteManager.getInstance().setContents(KTreeTransferable(xml))
    }

    override fun isCopyEnabled(dataContext: DataContext): Boolean {
        return viewsDebuggerActions?.canDeleteCopyCut() == true
    }

    override fun isCopyVisible(dataContext: DataContext): Boolean {
        return true
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        korgeBaseKorgeFileEditor.onRepaint()
    }
}
