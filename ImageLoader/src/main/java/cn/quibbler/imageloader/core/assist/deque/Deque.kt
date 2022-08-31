package cn.quibbler.imageloader.core.assist.deque

import java.util.*

interface Deque<E> : Queue<E> {

    fun addFirst(e: E)

    fun addLast(e: E)

    fun offerFirst(e: E): Boolean

    fun offerLast(e: E): Boolean

    fun removeFirst(): E

    fun removeLast(): E

    fun pollFirst(): E

    fun pollLast(): E

    fun getFirst(): E

    fun getLast(): E

    fun peekFirst(): E

    fun peekLast(): E

    fun removeFirstOccurrence(a: Any): Boolean

    fun removeLastOccurrence(a: Any): Boolean

    override fun add(e: E): Boolean

    override fun offer(e: E): Boolean

    override fun remove(): E

    override fun poll(): E

    override fun element(): E

    override fun peek(): E

    fun push(e: E)

    fun pop(): E

    override fun remove(element: E): Boolean

    override fun contains(element: E): Boolean

    override val size: Int

    override fun iterator(): MutableIterator<E>

    fun descendingIterator(): Iterator<E>

}