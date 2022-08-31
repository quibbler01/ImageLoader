package cn.quibbler.imageloader.core

import cn.quibbler.imageloader.cache.disk.naming.FileNameGenerator
import cn.quibbler.imageloader.cache.disk.naming.HashCodeFileNameGenerator
import cn.quibbler.imageloader.core.assist.QueueProcessingType
import cn.quibbler.imageloader.core.assist.deque.LIFOLinkedBlockingDeque
import cn.quibbler.imageloader.core.assist.deque.LinkedBlockingDeque
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

object DefaultConfigurationFactory {

    fun createExecutor(threadPoolSize: Int, threadPriority: Int, tasksProcessingType: QueueProcessingType): Executor {
        val lifo = tasksProcessingType == QueueProcessingType.LIFO
        val taskQueue = if (lifo) {
            LIFOLinkedBlockingDeque<Runnable>()
        } else {
            LinkedBlockingDeque<Runnable>()
        }
        return ThreadPoolExecutor(
            threadPoolSize,
            threadPoolSize,
            0L,
            TimeUnit.MILLISECONDS,
            taskQueue,
            createThreadFactory(threadPriority, "uil-pool-")
        )
    }

    fun createTaskDistributor(): Executor {
        return Executors.newCachedThreadPool(createThreadFactory(Thread.NORM_PRIORITY, "uil-pool-d-"))
    }

    fun createFileNameGenerator(): FileNameGenerator = HashCodeFileNameGenerator()

    private fun createThreadFactory(threadPriority: Int, threadNamePrefix: String): ThreadFactory {
        return DefaultThreadFactory(threadPriority, threadNamePrefix)
    }

    private class DefaultThreadFactory(private val threadPriority: Int, private val threadNamePrefix: String) : ThreadFactory {

        private val poolNumber: AtomicInteger = AtomicInteger(1)

        private val threadNumber: AtomicInteger = AtomicInteger(1)

        private val group = Thread.currentThread().threadGroup

        private val namePrefix = "$threadNamePrefix${poolNumber.getAndIncrement()}-thread-"

        override fun newThread(r: Runnable?): Thread {
            val thread = Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0)
            if (thread.isDaemon) {
                thread.isDaemon = false
            }
            thread.priority = threadPriority
            return thread
        }

    }

}