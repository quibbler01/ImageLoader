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

/**
 * An optionally-bounded {@linkplain BlockingDeque blocking deque} based on
 * linked nodes.
 * <p/>
 * <p> The optional capacity bound constructor argument serves as a
 * way to prevent excessive expansion. The capacity, if unspecified,
 * is equal to {@link Integer#MAX_VALUE}.  Linked nodes are
 * dynamically created upon each insertion unless this would bring the
 * deque above capacity.
 * <p/>
 * <p>Most operations run in constant time (ignoring time spent
 * blocking).  Exceptions include {@link #remove(Object) remove},
 * {@link #removeFirstOccurrence removeFirstOccurrence}, {@link
 * #removeLastOccurrence removeLastOccurrence}, {@link #contains
 * contains}, {@link #iterator iterator.remove()}, and the bulk
 * operations, all of which run in linear time.
 * <p/>
 * <p>This class and its iterator implement all of the
 * <em>optional</em> methods of the {@link Collection} and {@link
 * Iterator} interfaces.
 * <p/>
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <E> the type of elements held in this collection
 * @author Doug Lea
 * @since 1.6
 */
open class LinkedBlockingDeque<E> : AbstractQueue<E>, BlockingDeque<E>, Serializable {

    /*
     * Implemented as a simple doubly-linked list protected by a
     * single lock and using conditions to manage blocking.
     *
     * To implement weakly consistent iterators, it appears we need to
     * keep all Nodes GC-reachable from a predecessor dequeued Node.
     * That would cause two problems:
     * - allow a rogue Iterator to cause unbounded memory retention
     * - cause cross-generational linking of old Nodes to new Nodes if
     *   a Node was tenured while live, which generational GCs have a
     *   hard time dealing with, causing repeated major collections.
     * However, only non-deleted Nodes need to be reachable from
     * dequeued Nodes, and reachability does not necessarily have to
     * be of the kind understood by the GC.  We use the trick of
     * linking a Node that has just been dequeued to itself.  Such a
     * self-link implicitly means to jump to "first" (for next links)
     * or "last" (for prev links).
     */

    /*
     * We have "diamond" multiple interface/abstract class inheritance
     * here, and that introduces ambiguities. Often we want the
     * BlockingDeque javadoc combined with the AbstractQueue
     * implementation, so a lot of method specs are duplicated here.
     */

    /**
     * Creates a {@code LinkedBlockingDeque} with a capacity of
     * {@link Integer#MAX_VALUE}.
     */
    constructor() : this(Int.MAX_VALUE)

    /**
     * Creates a {@code LinkedBlockingDeque} with the given (fixed) capacity.
     *
     * @param capacity the capacity of this deque
     * @throws IllegalArgumentException if {@code capacity} is less than 1
     */
    constructor(capacity: Int) {
        if (capacity <= 0) {
            throw IllegalArgumentException()
        }
        this.capacity = capacity
    }

    /**
     * Creates a {@code LinkedBlockingDeque} with a capacity of
     * {@link Integer#MAX_VALUE}, initially containing the elements of
     * the given collection, added in traversal order of the
     * collection's iterator.
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *                              of its elements are null
     */
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

    /**
     * Pointer to first node.
     * Invariant: (first == null && last == null) ||
     * (first.prev == null && first.item != null)
     */
    @Transient
    var first: Node<E>? = null

    /**
     * Pointer to last node.
     * Invariant: (first == null && last == null) ||
     * (last.next == null && last.item != null)
     */
    @Transient
    var last: Node<E>? = null

    /**
     * Number of items in the deque
     */
    @Transient
    private var count: Int = 0

    /**
     * Maximum number of items in the deque
     */
    @Transient
    private val capacity: Int

    /**
     * Main lock guarding all access
     */
    val lock: ReentrantLock = ReentrantLock()

    /**
     * Condition for waiting takes
     */
    private val notEmpty: Condition = lock.newCondition()

    /**
     * Condition for waiting puts
     */
    private val notFull: Condition = lock.newCondition()


    // Basic linking and unlinking operations, called only while holding lock

    /**
     * Links node as first element, or returns false if full.
     */
    private fun linkFirst(node: Node<E>): Boolean {
        // assert lock.isHeldByCurrentThread();
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

    /**
     * Links node as last element, or returns false if full.
     */
    private fun linkLast(node: Node<E>): Boolean {
        // assert lock.isHeldByCurrentThread();
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

    /**
     * Removes and returns first element, or null if empty.
     */
    private fun unlinkFirst(): E? {
        // assert lock.isHeldByCurrentThread();
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

    /**
     * Removes and returns last element, or null if empty.
     */
    private fun unlinkLast(): E? {
        // assert lock.isHeldByCurrentThread();
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

    /**
     * Unlinks x.
     */
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

    /**
     * Returns an iterator over the elements in this deque in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     * <p/>
     * <p>The returned iterator is a "weakly consistent" iterator that
     * will never throw {@link java.util.ConcurrentModificationException
     * ConcurrentModificationException}, and guarantees to traverse
     * elements as they existed upon construction of the iterator, and
     * may (but is not guaranteed to) reflect any modifications
     * subsequent to construction.
     *
     * @return an iterator over the elements in this deque in proper sequence
     */
    override fun iterator(): MutableIterator<E> {
        return Itr()
    }

    /**
     * @throws NullPointerException if the specified element is null
     */
    override fun offer(e: E): Boolean {
        return offerLast(e)
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    override fun offer(e: E, timeout: Long, unit: TimeUnit): Boolean {
        return offerLast(e, timeout, unit)
    }

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque.  This method differs from {@link #peek peek} only in that
     * it throws an exception if this deque is empty.
     * <p/>
     * <p>This method is equivalent to {@link #getFirst() getFirst}.
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException if this deque is empty
     */
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

    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
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

    /**
     * Returns {@code true} if this deque contains the specified element.
     * More formally, returns {@code true} if and only if this deque contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this deque
     * @return {@code true} if this deque contains the specified element
     */
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

    /**
     * Returns an array containing all of the elements in this deque, in
     * proper sequence (from first to last element).
     * <p/>
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this deque.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     * <p/>
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this deque
     */
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

    /**
     * Returns an array containing all of the elements in this deque, in
     * proper sequence; the runtime type of the returned array is that of
     * the specified array.  If the deque fits in the specified array, it
     * is returned therein.  Otherwise, a new array is allocated with the
     * runtime type of the specified array and the size of this deque.
     * <p/>
     * <p>If this deque fits in the specified array with room to spare
     * (i.e., the array has more elements than this deque), the element in
     * the array immediately following the end of the deque is set to
     * {@code null}.
     * <p/>
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     * <p/>
     * <p>Suppose {@code x} is a deque known to contain only strings.
     * The following code can be used to dump the deque into a newly
     * allocated array of {@code String}:
     * <p/>
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the deque are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this deque
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this deque
     * @throws NullPointerException if the specified array is null
     */
    override fun <T : Any?> toArray(a: Array<out T>): Array<T> {
        return super.toArray(a)
    }

    /**
     * Atomically removes all of the elements from this deque.
     * The deque will be empty after this call returns.
     */
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

    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    override fun put(e: E) {
        putLast(e)
    }

    override fun take(): E? {
        return takeFirst()
    }

    /**
     * Returns the number of additional elements that this deque can ideally
     * (in the absence of memory or resource constraints) accept without
     * blocking. This is always equal to the initial capacity of this deque
     * less the current {@code size} of this deque.
     * <p/>
     * <p>Note that you <em>cannot</em> always tell if an attempt to insert
     * an element will succeed by inspecting {@code remainingCapacity}
     * because it may be the case that another thread is about to
     * insert or remove an element.
     */
    override fun remainingCapacity(): Int {
        val lock = this.lock
        lock.lock()
        try {
            return capacity - count
        } finally {
            lock.unlock()
        }
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    override fun drainTo(c: MutableCollection<in E?>?): Int {
        return drainTo(c, Int.MAX_VALUE)
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
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

    /**
     * @throws IllegalStateException {@inheritDoc}
     * @throws NullPointerException  {@inheritDoc}
     */
    override fun addFirst(e: E) {
        if (!offerFirst(e)) {
            throw IllegalArgumentException("Deque full")
        }
    }

    /**
     * @throws IllegalStateException {@inheritDoc}
     * @throws NullPointerException  {@inheritDoc}
     */
    override fun addLast(e: E) {
        if (!offerLast(e)) {
            throw IllegalArgumentException("Deque full")
        }
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
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

    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
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

    /**
     * @throws NullPointerException {@inheritDoc}
     */
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

    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
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

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    override fun removeFirst(): E {
        val x: E = pollFirst() ?: throw NoSuchElementException()
        return x
    }

    override fun removeLast(): E {
        TODO("Not yet implemented")
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
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

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    override fun getFirst(): E {
        return peekFirst() ?: throw NoSuchElementException()
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
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

    /**
     * Inserts the specified element at the end of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * it is generally preferable to use method {@link #offer offer}.
     * <p/>
     * <p>This method is equivalent to {@link #addLast}.
     *
     * @throws IllegalStateException if the element cannot be added at this
     *                               time due to capacity restrictions
     * @throws NullPointerException  if the specified element is null
     */
    override fun add(element: E): Boolean {
        addLast(element)
        return true
    }

    /**
     * Retrieves and removes the head of the queue represented by this deque.
     * This method differs from {@link #poll poll} only in that it throws an
     * exception if this deque is empty.
     * <p/>
     * <p>This method is equivalent to {@link #removeFirst() removeFirst}.
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException if this deque is empty
     */
    override fun remove(): E {
        return removeFirst()
    }

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     * <p/>
     * <p>This method is equivalent to
     * {@link #removeFirstOccurrence(Object) removeFirstOccurrence}.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if this deque changed as a result of the call
     */
    override fun remove(element: E?): Boolean {
        return removeFirstOccurrence(element)
    }

    /**
     * @throws IllegalStateException {@inheritDoc}
     * @throws NullPointerException  {@inheritDoc}
     */
    override fun push(e: E) {
        addFirst(e)
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    override fun pop(): E {
        return removeFirst()
    }

    /**
     * Returns an iterator over the elements in this deque in reverse
     * sequential order.  The elements will be returned in order from
     * last (tail) to first (head).
     * <p/>
     * <p>The returned iterator is a "weakly consistent" iterator that
     * will never throw {@link java.util.ConcurrentModificationException
     * ConcurrentModificationException}, and guarantees to traverse
     * elements as they existed upon construction of the iterator, and
     * may (but is not guaranteed to) reflect any modifications
     * subsequent to construction.
     *
     * @return an iterator over the elements in this deque in reverse order
     */
    override fun descendingIterator(): MutableIterator<E> {
        return DescendingItr()
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
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

    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
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

    /**
     * Base class for Iterators for LinkedBlockingDeque
     */
    private inner abstract class AbstractItr : MutableIterator<E> {

        /**
         * The next node to return in next()
         */
        var next: Node<E>? = null

        /**
         * nextItem holds on to item fields because once we claim that
         * an element exists in hasNext(), we must return item read
         * under lock (in advance()) even if it was in the process of
         * being removed when hasNext() was called.
         */
        var nextItem: E? = null

        /**
         * Node returned by most recent call to next. Needed by remove.
         * Reset to null if this element is deleted by a call to remove.
         */
        private var lastRet: Node<E>? = null

        abstract fun firstNode(): Node<E>?

        abstract fun nextNode(n: Node<E>?): Node<E>?

        constructor() {
            // set to initial position
            val lock = this@LinkedBlockingDeque.lock
            lock.lock()
            try {
                next = firstNode()
                nextItem = next?.item
            } finally {
                lock.unlock()
            }
        }

        /**
         * Returns the successor node of the given non-null, but
         * possibly previously deleted, node.
         */
        private fun succ(n: Node<E>?): Node<E>? {
            // Chains of deleted nodes ending in null or self-links
            // are possible if multiple interior nodes are removed.
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

        /**
         * Advances next.
         */
        fun advance() {
            val lock = this@LinkedBlockingDeque.lock
            lock.lock()
            try {
                // assert next != null;
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

    /**
     * Forward iterator
     */
    private inner class Itr : AbstractItr() {
        override fun firstNode(): Node<E>? {
            return first
        }

        override fun nextNode(n: Node<E>?): Node<E>? {
            return n?.next
        }
    }

    /**
     * Descending iterator
     */
    private inner class DescendingItr : AbstractItr() {
        override fun firstNode(): Node<E>? {
            return last
        }

        override fun nextNode(n: Node<E>?): Node<E>? {
            return n?.prev
        }

    }

    /**
     * Save the state of this deque to a stream (that is, serialize it).
     *
     * @param s the stream
     * @serialData The capacity (int), followed by elements (each an
     * {@code Object}) in the proper order, followed by a null
     */
    @Throws(IOException::class)
    private fun writeObject(s: ObjectOutputStream) {
        val lock = this.lock
        lock.lock()
        try {
            // Write out capacity and any hidden stuff
            s.defaultWriteObject()

            // Write out all elements in the proper order.
            var p = first
            while (p != null) {
                s.writeObject(p)
                p = p.next
            }

            // Use trailing null as sentinel
            s.writeObject(null)
        } finally {
            lock.unlock()
        }
    }

    /**
     * Reconstitute this deque from a stream (that is,
     * deserialize it).
     *
     * @param s the stream
     */
    @Throws(IOException::class)
    private fun readObject(s: ObjectInputStream) {
        s.defaultReadObject()
        count = 0
        first = null
        last = null
        // Read in all elements and place in queue
        while (true) {
            @SuppressWarnings("unchecked")
            val item = s.readObject() as E? ?: break
            add(item)
        }

    }

}