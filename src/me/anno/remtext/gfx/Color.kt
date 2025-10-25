package me.anno.remtext.gfx

class Color(val r: Float, val g: Float, val b: Float) {
    constructor(rgb: Int) : this(
        rgb.shr(16).and(0xff) / 255f,
        rgb.shr(8).and(0xff) / 255f,
        rgb.and(0xff) / 255f,
    )
}