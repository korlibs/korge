package com.soywiz.korge.intellij.filetype

import com.intellij.lang.*
import com.intellij.lang.xml.*
import com.intellij.lexer.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.psi.tree.*

// @TODO: Can use XMLParserDefinition?
//class KtreeParserDefinition : ParserDefinition {
class KTreeParserDefinition : XMLParserDefinition() {
    override fun getFileNodeType(): IFileElementType = KtreeFile.KTREE_FILE_ELEMENT_TYPE
    //override fun getWhitespaceTokens(): TokenSet = LanguageParserDefinitions.INSTANCE.forLanguage(Language.findInstance(XMLLanguage::class.java)).whitespaceTokens
    //override fun getCommentTokens(): TokenSet = LanguageParserDefinitions.INSTANCE.forLanguage(Language.findInstance(XMLLanguage::class.java)).commentTokens
    //override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    //override fun createLexer(project: Project): Lexer = XmlLexer()
    //override fun createParser(project: Project): PsiParser = LanguageParserDefinitions.INSTANCE.forLanguage(Language.findInstance(XMLLanguage::class.java)).createParser(project)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = KtreeFile(viewProvider)
    //override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements = canStickTokensTogetherByLexerInXml(left, right, createLexer(left.psi.project), 0)
    override fun createElement(node: ASTNode): PsiElement = throw IllegalArgumentException("Unknown element: $node")
}
