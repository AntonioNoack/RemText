package me.anno.remtext.colors.impl

class KeywordMap(keywords: List<String>, ignoreCase: Boolean) {

    private val char0 = keywords.minOf {
        if (ignoreCase) it[0].uppercaseChar()
        else it[0]
    }.code

    private val entries = arrayOfNulls<ArrayList<String>>(
        keywords.maxOf { it[0] }.code + 1 - char0
    )

    private fun put(char: Int, keyword: String) {
        var entry = entries[char]
        if (entry == null) {
            entry = ArrayList()
            entries[char] = entry
        }
        entry.add(keyword)
    }

    init {
        for (keyword in keywords) {
            put(keyword[0].code - char0, keyword)
        }
        if (ignoreCase) for (keyword in keywords) {
            put(keyword[0].uppercaseChar().code - char0, keyword)
        }
    }

    operator fun get(char: Char): List<String>? {
        val index = char.code - char0
        return entries.getOrNull(index)
    }

}