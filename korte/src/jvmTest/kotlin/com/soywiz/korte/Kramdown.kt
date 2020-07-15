package com.soywiz.korte

import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension
import com.vladsch.flexmark.ext.definition.DefinitionExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.typographic.TypographicExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.ParserEmulationProfile
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.misc.Extension

private val options = MutableDataSet().also { options ->
    options.setFrom(ParserEmulationProfile.KRAMDOWN)
    options.set(
        Parser.EXTENSIONS, mutableListOf<Extension>(
        AbbreviationExtension.create(),
        DefinitionExtension.create(),
        FootnoteExtension.create(),
        TablesExtension.create(),
        TypographicExtension.create()
    ))
}
private val parser = Parser.builder(options).build()
private val renderer = HtmlRenderer.builder(options).build()

fun String.kramdownToHtml() = renderer.render(parser.parse(this))
