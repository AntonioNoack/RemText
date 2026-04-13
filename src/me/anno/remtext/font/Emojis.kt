package me.anno.remtext.font

import me.anno.remtext.gfx.PixelData
import me.anno.remtext.gfx.Texture
import javax.imageio.ImageIO

object Emojis {

    private const val TILE_SIZE = 36
    private const val TILES_PER_ROW = 64

    val images: PixelData
    private val names: List<String> // name, desc, name, desc, ...
    private val emojiIndicesByFirstChar: Map<Char, List<Int>>
    private val maxEmojiLength: Int

    init {
        val self = Emojis::class.java
        images = self.getResourceAsStream("/Emojis.png")!!.use { stream ->
            ImageIO.read(stream)
        }.run { PixelData(this) }
        names = self.getResourceAsStream("/Emojis.txt")!!.use { stream ->
            stream.readBytes().decodeToString().split('\n')
        }
        val byFirstChar = HashMap<Char, MutableList<Int>>()
        var longestName = 0
        for (emojiIndex in 0 until numEmojis) {
            val emoji = getName(emojiIndex)
            if (emoji.isEmpty()) continue
            byFirstChar.getOrPut(emoji[0], ::ArrayList).add(emojiIndex)
            longestName = maxOf(longestName, emoji.length)
        }
        emojiIndicesByFirstChar = byFirstChar.mapValues { (_, value) ->
            value.sortedByDescending { getName(it).length }
        }
        maxEmojiLength = longestName
    }

    fun getName(i: Int) = names[i * 2]
    fun getDesc(i: Int) = names[i * 2 + 1]

    val numEmojis get() = names.size shr 1

    data class Match(val emojiIndex: Int, val length: Int)

    fun findMatch(text: String, i0: Int, i1: Int = text.length): Match? {
        if (i0 !in 0 until i1) return null
        val matchingIndices = emojiIndicesByFirstChar[text[i0]] ?: return null
        for (emojiIndex in matchingIndices) {
            val emoji = getName(emojiIndex)
            val length = emoji.length
            if (i0 + length <= i1 && text.startsWith(emoji, i0)) {
                return Match(emojiIndex, length)
            }
        }
        return null
    }

    fun findPreviousStart(text: String, end: Int, i0: Int = 0): Int {
        if (end <= i0) return end - 1
        val minStart = maxOf(i0, end - maxEmojiLength)
        for (start in minStart until end) {
            val match = findMatch(text, start, end) ?: continue
            if (start + match.length == end) return start
        }
        return end - 1
    }

    fun tileX(index: Int) = index % TILES_PER_ROW * TILE_SIZE
    fun tileY(index: Int) = index / TILES_PER_ROW * TILE_SIZE
    fun tileSize() = TILE_SIZE

    val texture = lazy {
        Texture(images)
    }

}
