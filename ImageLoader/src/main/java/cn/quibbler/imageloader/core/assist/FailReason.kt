package cn.quibbler.imageloader.core.assist

class FailReason(public val type: FailType, public val cause: Throwable) {

    enum class FailType {
        IO_ERROR,
        DECODING_ERROR,
        NETWORK_DENIED,
        OUT_OF_MEMORY,
        UNKNOWN
    }

}