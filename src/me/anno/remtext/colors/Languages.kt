package me.anno.remtext.colors

import me.anno.remtext.colors.impl.JavaScriptLanguage
import me.anno.remtext.colors.impl.XMLLanguage

object Languages {

    val highlighters = HashMap<String, Language>()
    val jsonColors = JavaScriptLanguage(false)

    init {
        highlighters["js"] = JavaScriptLanguage(true)
        highlighters["json"] = jsonColors
        highlighters["xml"] = XMLLanguage(false)
        highlighters["html"] = XMLLanguage(true)
    }
}