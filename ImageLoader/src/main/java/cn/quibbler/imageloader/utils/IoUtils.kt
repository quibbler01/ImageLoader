package cn.quibbler.imageloader.utils

interface CopyListener {
    fun onBytesCopied(current: Int, total: Int): Boolean
}