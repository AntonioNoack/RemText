package me.anno.remtext.formatters

import me.anno.remtext.language.Language
import me.anno.remtext.language.CLikeLanguage
import me.anno.remtext.language.CLikeLanguageType
import me.anno.remtext.language.XMLLanguage

class AutoFormatOptions(val indentation: String, val lineBreakLength: Int) {
    companion object {
        var index = 0

        val xmlOptions = listOf(
            AutoFormatOptions("", Int.MAX_VALUE),
            AutoFormatOptions("  ", 0),
        )

        val jsonOptions = listOf(
            AutoFormatOptions("", Int.MAX_VALUE),
            AutoFormatOptions("  ", 0),
            AutoFormatOptions("  ", 200),
        )

        val options = listOf(
            AutoFormatOptions("", 200),
            AutoFormatOptions("  ", 200),
            AutoFormatOptions("    ", 200),
        )

        fun nextOption(language: Language?): AutoFormatOptions {
            val options = when {
                language is XMLLanguage -> xmlOptions
                language is CLikeLanguage && language.type == CLikeLanguageType.JSON -> jsonOptions
                else -> options
            }
            return options[(index++) % options.size]
        }
    }

    override fun toString(): String {
        return "{indentation: \"$indentation\", lineBreakLength: $lineBreakLength}"
    }
}