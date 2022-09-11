package cn.quibbler.imageloader.core.assist.deque

import java.io.Serializable

class LIFOLinkedBlockingDeque<T> : LinkedBlockingDeque<T>() {

    override fun offer(e: T): Boolean {
        return super.offerFirst(e)
    }

    override fun remove(): T {
        return super.removeFirst()
    }

}