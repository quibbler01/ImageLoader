package cn.quibbler.imageloader.core.assist.deque

import java.util.Deque
import java.util.concurrent.BlockingDeque
import java.util.concurrent.TimeUnit

interface BlockingDeque<E> : BlockingDeque<E>, Deque<E> {

    override fun addFirst(e: E)

    override fun addLast(e: E)

    override fun offerFirst(e: E): Boolean

    override fun offerLast(e: E): Boolean

    @Throws(InterruptedException::class)
    override fun putFirst(e: E)

    @Throws(InterruptedException::class)
    override fun putLast(e: E)

    @Throws(InterruptedException::class)
    override fun offerFirst(e: E, timeout: Long, unit: TimeUnit): Boolean

    @Throws(InterruptedException::class)
    override fun offerLast(e: E, timeout: Long, unit: TimeUnit): Boolean

    @Throws(InterruptedException::class)
    override fun takeFirst(): E

    @Throws(InterruptedException::class)
    override fun pollFirst(timeout: Long, unit: TimeUnit): E

    @Throws(InterruptedException::class)
    override fun pollLast(timeout: Long, unit: TimeUnit): E

    override fun removeFirstOccurrence(o: Any?): Boolean

    override fun removeLastOccurrence(a: Any?): Boolean

    override fun add(e: E): Boolean

    override fun offer(e: E): Boolean

    @Throws(InterruptedException::class)
    override fun put(e: E)

    override fun offer(e: E, timeout: Long, unit: TimeUnit): Boolean

    override fun remove(): E

    override fun poll(): E

    @Throws(InterruptedException::class)
    override fun take(): E

    @Throws(InterruptedException::class)
    override fun poll(timeout: Long, unit: TimeUnit): E

    override fun element(): E

    override fun peek(): E

    override fun remove(element: E?): Boolean

    override fun contains(element: E?): Boolean

    override val size: Int

    override fun iterator(): MutableIterator<E>

    override fun push(e: E)

}