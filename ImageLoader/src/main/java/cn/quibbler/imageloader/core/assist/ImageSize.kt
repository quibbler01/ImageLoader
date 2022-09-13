package cn.quibbler.imageloader.core.assist

class ImageSize(val width: Int, val height: Int) {

    companion object {
        const val TO_STRING_MAX_LENGHT = 9
        const val SEPARATOR = "x"
    }

    constructor(width: Int, height: Int, rotation: Int) : this(
        if (rotation % 180 == 0) width else height,
        if (rotation % 180 == 0) height else width
    )

    fun scaleDown(sampleSize: Int): ImageSize {
        return ImageSize(width / sampleSize, height / sampleSize)
    }

    fun scale(scale: Float): ImageSize {
        return ImageSize((width * scale).toInt(), (height * scale).toInt())
    }

    override fun toString(): String {
        return "$TO_STRING_MAX_LENGHT$width$SEPARATOR$height"
    }

}