package me.anno.remtext

import me.anno.remtext.gfx.Color

// todo load them from a config .txt file?
object Colors {

    const val DEFAULT = 0.toByte()
    const val COMMENT = 1.toByte()
    const val ML_COMMENT = 2.toByte()
    const val NUMBER = 3.toByte()
    const val SYMBOL = 4.toByte()
    const val KEYWORD = 5.toByte()
    const val BRACKET = 6.toByte()
    const val STRING = 7.toByte()
    const val ML_STRING = 8.toByte()
    const val TODO = 9.toByte()
    const val VARIABLE = 10.toByte()
    const val DOC_COMMENT = 11.toByte()

    const val COLOR_BITS = 4
    const val COLOR_MASK = (1 shl COLOR_BITS) - 1

    val darkStyle = run {

        val textColor = Color(0xffffaa)
        val commentColor = Color(0x8ACF24)
        val numberColor = Color(0x33BCCC)
        val symbolColor = Color(0xE5836A)
        val keywordColor = Color(0xE5A440)
        val bracketColor = Color(0xC0A070)
        val stringColor = Color(0xA0CF7E)
        val todoColor = Color(0xC0ED2A)
        val variableColor = Color(0xDDDDDD)
        val docColor = Color(0x8ACF24)

        arrayOf(
            textColor,
            commentColor,
            commentColor,
            numberColor,
            symbolColor,
            keywordColor,
            bracketColor,
            stringColor,
            stringColor,
            todoColor,
            variableColor,
            docColor,
        )
    }

    val lightStyle = run {

        val textColor = Color(0x223344)
        val commentColor = Color(0x365514)
        val numberColor = Color(0x155E63)
        val symbolColor = Color(0x555555)
        val keywordColor = Color(0x6D3F1B)
        val bracketColor = Color(0x594B33)
        val stringColor = Color(0x4D653F)
        val todoColor = Color(0x5A1F6F)
        val variableColor = Color(0x000000)
        val docColor = Color(0x365514)

        arrayOf(
            textColor,
            commentColor,
            commentColor,
            numberColor,
            symbolColor,
            keywordColor,
            bracketColor,
            stringColor,
            stringColor,
            todoColor,
            variableColor,
            docColor,
        )
    }

    val style get() = if (Window.isDarkTheme) darkStyle else lightStyle
    operator fun get(color: Byte): Color = style[color.toInt() and COLOR_MASK]

    operator fun get(char: Char): Color {
        val type = when (char) {
            in "()[]{}" -> BRACKET
            in "+-/*<>=?:" -> SYMBOL
            in '0'..'9' -> NUMBER
            else -> DEFAULT
        }
        return this[type]
    }
}