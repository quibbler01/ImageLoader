package cn.quibbler.imageloader.core.assist.deque

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.lang.Integer.min
import java.util.*
import java.util.concurrent.BlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.NoSuchElementException

open class LinkedBlockingDeque<E> : AbstractQueue<E>, BlockingDeque<E>, Serializable {

    constructor() : this(Int.MAX_VALUE)

    constructor(capacity: Int) {
        if (capacity <= 0) {
            throw IllegalArgumentException()
        }
        this.capacity = capacity
    }

    constructor(c: Collection<E>) : this(Int.MAX_VALUE) {
        val lock = lock
        lock.lock() // Never contended, but necessary for visibility
        try {
            for (e in c) {
                if (e == null) throw NullPointerException()
                check(linkLast(Node<E>(e))) { "Deque full" }
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Doubly-linked list node class
     */
    class Node<E>(/*The item, or null if this node has been removed.*/var item: E?) {

        /**
         * One of:
         * - the real predecessor Node
         * - this Node, meaning the predecessor is tail
         * - null, meaning there is no predecessor
         */
        var prev: Node<E>? = null

        /**
         * One of:
         * - the real successor Node
         * - this Node, meaning the successor is head
         * - null, meaning there is no successor
         */
        var next: Node<E>? = null
    }

    @Transient
    var first: Node<E>? = null

    @Transient
    var last: Node<E>? = null

    @Transient
    private var count: Int = 0

    @Transient
    private val capacity: Int

    val lock: ReentrantLock = ReentrantLock()

    private val notEmpty: Condition = lock.newCondition()

    private val notFull: Condition = lock.newCondition()

    private fun linkFirst(node: Node<E>): Boolean {
        if (count >= capacity) {
            return false
        }
        val f = first
        node.next = first
        first = node
        if (last == null) {
            last = node
        } else {
            f?.prev = node
        }
        ++count
        notEmpty.signal()
        return true
    }

    private fun linkLast(node: Node<E>): Boolean {
        if (count >= capacity) {
            return false
        }
        val l = last
        node.prev = l
        last = node
        if (first == null) {
            first = node
        } else {
            l?.next = node
        }
        ++count
        notEmpty.signal()
        return true
    }

    private fun unlinkFirst(): E? {
        val f = first
        if (f == null) {
            return null
        }
        val n = f.next
        val item = f.item
        f.item = null
        f.next = f
        first = n
        if (n == null) {
            last = null
        } else {
            n.prev = null
        }
        --count
        notFull.signal()
        return item
    }

    private fun unlinkLast(): E? {
        val l = last
        if (l == null) {
            return null
        }
        val p = l.prev
        val item = l.item
        l.item = null
        l.prev = l
        last = p
        if (p == null) {
            first = null
        } else {
            p.next = null
        }
        --count
        notFull.signal()
        return item
    }

    fun unlink(x: Node<E>) {
        // assert lock.isHeldByCurrentThread();
        val p = x.prev
        val n = x.next
        if (p == null) {
            unlinkFirst()
        } else if (n == null) {
            unlinkLast()
        } else {
            p.next = n
            n.prev = p
            x.item = null
            // Don't mess with x's links.  They may still be in use by
            // an iterator.
            --count
            notFull.signal()
        }
    }

    override fun iterator(): MutableIterator<E> {
        return Itr()
    }

    override fun offer(e: E): Boolean {
        return offerLast(e)
    }

    override fun offer(e: E, timeout: Long, unit: TimeUnit): Boolean {
        return offerLast(e, timeout, unit)
    }

    override fun element(): E {
        return getFirst()
    }

    override fun poll(): E? {
        return pollFirst()
    }

    override fun poll(timeout: Long, unit: TimeUnit): E? {
        return pollFirst(timeout, unit)
    }

    override fun peek(): E? {
        return peekFirst()
    }

    override val size: Int
        get() {
            val lock = this.lock
            lock.lock()
            try {
                return count
            } finally {
                lock.unlock()
            }
        }

    override fun contains(element: E?): Boolean {
        if (element == null) return false
        val lock = this.lock
        lock.lock()
        try {
            var p: Node<E>? = first
            while (p != null) {
                if (element == p.item)
                    return true
                p = p.next
            }
            return false
        } finally {
            lock.unlock()
        }
    }

    override fun toArray(): Array<Any?>? {
        val lock = this.lock
        lock.lock()
        try {
            val a = arrayOfNulls<Any>(count)
            var k = 0
            var p: Node<E>? = first
            while (p != null) {
                a[k++] = p.item
                p = p.next
            }
            return a
        } finally {
            lock.unlock()
        }
    }

    override fun <T : Any?> toArray(a: Array<out T>): Array<T> {
        return super.toArray(a)
    }

    override fun clear() {
        val lock = this.lock
        lock.lock()
        try {
            var f = first
            while (f != null) {
                f.item = null
                val n = f.next
                f.prev = null
                f.next = null
                f = n
            }
            first = null
            last = null
            count = 0
            notFull.signal()
        } finally {
            lock.unlock()
        }
    }

    override fun toString(): String {
        val lock = this.lock
        lock.lock()
        try {
            var p: Node<E>? = first ?: return "[]"

            val sb = StringBuilder()
            sb.append('[')
            while (true) {
                val e = p?.item
                sb.append(if (e === this) "(this Collection)" else e)
                p = p?.next
                if (p == null) return sb.append(']').toString()
                sb.append(',').append(' ')
            }
        } finally {
            lock.unlock()
        }
    }

    override fun put(e: E) {
        putLast(e)
    }

    override fun take(): E? {
        return takeFirst()
    }

    override fun remainingCapacity(): Int {
        val lock = this.lock
        lock.lock()
        try {
            return capacity - count
        } finally {
            lock.unlock()
        }
    }

    override fun drainTo(c: MutableCollection<in E?>?): Int {
        return drainTo(c, Int.MAX_VALUE)
    }

    override fun drainTo(c: MutableCollection<in E?>?, maxElements: Int): Int {
        if (c == null) throw NullPointerException()
        if (c == this) throw IllegalArgumentException()
        val lock = this.lock
        lock.lock()
        try {
            val n = min(maxElements, count)
            for (i in 0 until n) {
                c.add(first?.item)
                unlinkFirst()
            }
            return n
        } finally {
            lock.unlock()
        }
    }

    override fun addFirst(e: E) {
        if (!offerFirst(e)) {
            throw IllegalArgumentException("Deque full")
        }
    }

    override fun addLast(e: E) {
        if (!offerLast(e)) {
            throw IllegalArgumentException("Deque full")
        }
    }

    override fun offerFirst(e: E?): Boolean {
        if (e == null) throw NullPointerException()
        val node = Node<E>(e)
        val lock = this.lock
        lock.lock()
        try {
            return linkFirst(node)
        } finally {
            lock.unlock()
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun offerFirst(e: E?, timeout: Long, unit: TimeUnit): Boolean {
        if (e == null) throw NullPointerException()
        val node = Node<E>(e)
        var nanos: Long = unit.toNanos(timeout)
        val lock = this.lock
        lock.lock()
        try {
            while (!linkFirst(node)) {
                if (nanos <= 0)
                    return false
                nanos = notFull.awaitNanos(nanos)
            }
            return true
        } finally {
            lock.unlock()
        }
    }

    override fun offerLast(e: E?): Boolean {
        if (e == null) throw NullPointerException()
        val node = Node<E>(e)
        val lock = this.lock
        lock.lockInterruptibly()
        try {
            return linkLast(node)
        } finally {
            lock.unlock()
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun offerLast(e: E?, timeout: Long, unit: TimeUnit): Boolean {
        if (e == null) throw NullPointerException()
        val node = Node<E>(e)
        var nanos: Long = unit.toNanos(timeout)
        val lock = this.lock
        lock.lockInterruptibly()
        try {
            while (!linkLast(node)) {
                if (nanos <= 0)
                    return false
                nanos = notFull.awaitNanos(nanos)
            }
            return true
        } finally {
            lock.unlock()
        }
    }

    override fun removeFirst(): E {
        val x: E = pollFirst() ?: throw NoSuchElementException()
        return x
    }

    override fun removeLast(): E {
        TODO("Not yet implemented")
    }

    @Throws(InterruptedException::class)
    override fun pollFirst(timeout: Long, unit: TimeUnit): E? {
        var nanos: Long = unit.toNanos(timeout)
        val lock = this.lock
        lock.lockInterruptibly()
        try {
            var x: E? = null
            while (unlinkFirst().also { x = it } == null) {
                if (nanos <= 0)
                    return null
                nanos = notFull.awaitNanos(nanos)
            }
            return x
        } finally {
            lock.unlock()
        }
    }

    override fun pollFirst(): E? {
        val lock = this.lock
        lock.lock()
        try {
            return unlinkFirst()
        } finally {
            lock.unlock()
        }
    }

    @Throws(InterruptedException::class)
    override fun pollLast(timeout: Long, unit: TimeUnit): E? {
        var nanos: Long = unit.toNanos(timeout)
        val lock = this.lock
        lock.lockInterruptibly()
        try {
            var x: E? = null
            while (unlinkLast().also { x = it } == null) {
                if (nanos <= 0)
                    return null
                nanos = notEmpty.awaitNanos(nanos)
            }
            return x
        } finally {
            lock.unlock()
        }
    }

    override fun pollLast(): E? {
        val lock = this.lock
        lock.lock()
        try {
            return unlinkLast()
        } finally {
            lock.unlock()
        }
    }

    override fun getFirst(): E {
        return peekFirst() ?: throw NoSuchElementException()
    }

    override fun getLast(): E {
        return peekLast() ?: throw NoSuchElementException()
    }

    override fun peekFirst(): E? {
        val lock = this.lock
        lock.lock()
        try {
            return first?.item
        } finally {
            lock.unlock()
        }
    }

    override fun peekLast(): E? {
        val lock = this.lock
        lock.lock()
        try {
            return last?.item
        } finally {
            lock.unlock()
        }
    }

    override fun removeFirstOccurrence(o: Any?): Boolean {
        if (o == null) return false
        val lock = this.lock
        lock.lock()
        try {
            var p: Node<E>? = first
            while (p != null) {
                if (o == p.item) {
                    unlink(p)
                    return true
                }
                p = p.next
            }
            return false
        } finally {
            lock.unlock()
        }
    }

    override fun removeLastOccurrence(o: Any?): Boolean {
        if (o == null) return false
        val lock = this.lock
        lock.lock()
        try {
            var p: Node<E>? = last
            while (p != null) {
                if (o == p) {
                    unlink(p)
                    return true
                }
                p = p.prev
            }
            return false
        } finally {
            lock.unlock()
        }
    }

    override fun add(element: E): Boolean {
        addLast(element)
        return true
    }

    override fun remove(): E {
        return removeFirst()
    }

    override fun remove(element: E?): Boolean {
        return removeFirstOccurrence(element)
    }

    override fun push(e: E) {
        addFirst(e)
    }

    override fun pop(): E {
        return removeFirst()
    }

    override fun descendingIterator(): MutableIterator<E> {
        return DescendingItr()
    }

    override fun putFirst(e: E?) {
        if (e == null) throw NullPointerException()
        val node = Node<E>(e)
        val lock = this.lock
        lock.lock()
        try {
            while (!linkFirst(node))
                notFull.await()
        } finally {
            lock.unlock()
        }
    }

    override fun putLast(e: E?) {
        if (e == null) throw NullPointerException()
        val node = Node<E>(e)
        val lock = this.lock
        lock.lock()
        try {
            while (!linkLast(node))
                notFull.await()
        } finally {
            lock.unlock()
        }
    }

    override fun takeFirst(): E? {
        val lock = this.lock
        lock.lock()
        try {
            var x: E? = null
            while (unlinkFirst().also { x = it } == null)
                notEmpty.await()
            return x
        } finally {
            lock.unlock()
        }
    }

    override fun takeLast(): E? {
        val lock = this.lock
        lock.lock()
        try {
            var x: E? = null
            while (unlinkLast().also { x = it } == null)
                notEmpty.await()
            return x
        } finally {
            lock.unlock()
        }
    }

    private inner abstract class AbstractItr : MutableIterator<E> {

        var next: Node<E>? = null

        var nextItem: E? = null

        private var lastRet: Node<E>? = null

        abstract fun firstNode(): Node<E>?

        abstract fun nextNode(n: Node<E>?): Node<E>?

        constructor() {
            val lock = this@LinkedBlockingDeque.lock
            lock.lock()
            try {
                next = firstNode()
                nextItem = next?.item
            } finally {
                lock.unlock()
            }
        }

        private fun succ(n: Node<E>?): Node<E>? {
            while (true) {
                val s = nextNode(n)
                if (s == null) {
                    return null
                } else if (s.item != null) {
                    return s
                } else if (s == n) {
                    return firstNode()
                } else {
                    //n = s
                }
            }
        }

        fun advance() {
            val lock = this@LinkedBlockingDeque.lock
            lock.lock()
            try {
                next = succ(next)
                nextItem = next?.item
            } finally {
                lock.unlock()
            }
        }

        override fun hasNext(): Boolean {
            return next != null
        }

        override fun next(): E {
            if (next == null) throw NoSuchElementException()
            lastRet = next
            val x = nextItem
            advance()
            return x!!
        }

        override fun remove() {
            var n = lastRet
            if (n == null) throw IllegalArgumentException()
            lastRet = null
            val lock = this@LinkedBlockingDeque.lock
            lock.lock()
            try {
                if (n.item != null)
                    unlink(n)
            } finally {
                lock.unlock()
            }
        }

    }

    private inner class Itr : AbstractItr() {
        override fun firstNode(): Node<E>? {
            return first
        }

        override fun nextNode(n: Node<E>?): Node<E>? {
            return n?.next
        }
    }

    private inner class DescendingItr : AbstractItr() {
        override fun firstNode(): Node<E>? {
            return last
        }

        override fun nextNode(n: Node<E>?): Node<E>? {
            return n?.prev
        }

    }

    @Throws(IOException::class)
    private fun writeObject(s: ObjectOutputStream) {
        val lock = this.lock
        lock.lock()
        try {
            s.defaultWriteObject()

            var p = first
            while (p != null) {
                s.writeObject(p)
                p = p.next
            }

            s.writeObject(null)
        } finally {
            lock.unlock()
        }
    }

    @Throws(IOException::class)
    private fun readObject(s: ObjectInputStream) {
        s.defaultReadObject()
        count = 0
        first = null
        last = null
        while (true) {
            @SuppressWarnings("unchecked")
            val item = s.readObject() as E? ?: break
            add(item)
        }

    }

}