package me.anno.remtext.language

class KeywordMap(val flatList: List<String>, ignoreCase: Boolean) {

    private val char0 = flatList.minOf {
        if (ignoreCase) it[0].uppercaseChar()
        else it[0]
    }.code

    private val entries = arrayOfNulls<ArrayList<String>>(
        flatList.maxOf { it[0] }.code + 1 - char0
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
        for (keyword in flatList) {
            put(keyword[0].code - char0, keyword)
        }
        if (ignoreCase) for (keyword in flatList) {
            put(keyword[0].uppercaseChar().code - char0, keyword)
        }
    }

    operator fun get(char: Char): List<String>? {
        val index = char.code - char0
        return entries.getOrNull(index)
    }

    fun flatten() = flatList

}