package cn.quibbler.imageloader.cache.disk.impl.ext

import cn.quibbler.imageloader.utils.closeSilently
import java.io.*
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class DiskLruCache : Closeable {

    companion object {
        const val JOURNAL_FILE = "journal"
        const val JOURNAL_FILE_TEMP = "journal.tmp"
        const val JOURNAL_FILE_BACKUP = "journal.bkp"
        const val MAGIC = "libcore.io.DiskLruCache"

        const val VERSION_1 = "1"
        const val ANY_SEQUENCE_NUMBER = -1L

        val LEGAL_KEY_PATTERN = Pattern.compile("[a-z0-9_-]{1,64}")

        const val CLEAN = "CLEAN"
        const val DIRTY = "DIRTY"
        const val REMOVE = "REMOVE"
        const val READ = "READ"

        fun open(directory: File, appVersion: Int, valueCount: Int, maxSize: Long, maxFileCount: Int): DiskLruCache {
            if (maxSize <= 0) {
                throw IllegalArgumentException("maxSize <= 0")
            }
            if (maxFileCount <= 0) {
                throw IllegalArgumentException("maxFileCount <= 0")
            }
            if (valueCount <= 0) {
                throw IllegalArgumentException("valueCount <= 0")
            }

            // If a bkp file exists, use it instead.
            val backupFile: File = File(directory, JOURNAL_FILE_BACKUP)
            if (backupFile.exists()) {
                val journalFile: File = File(directory, JOURNAL_FILE)
                // If journal file also exists just delete backup file.
                if (journalFile.exists()) {
                    backupFile.delete()
                } else {
                    renameTo(backupFile, journalFile, false)
                }
            }

            // Prefer to pick up where we left off.
            var cache: DiskLruCache = DiskLruCache(directory, appVersion, valueCount, maxSize, maxFileCount)
            if (cache.journalFile.exists()) {
                try {
                    cache.readJournal()
                    cache.processJournal()
                    cache.journalWriter = BufferedWriter(OutputStreamWriter(FileOutputStream(cache.journalFile, true), US_ASCII))
                    return cache
                } catch (journalIsCorrupt: IOException) {
                    System.out.println("DiskLruCache$directory is corrupt ${journalIsCorrupt.message},removing")
                    cache.delete()
                }
            }

            // Create a new empty cache.
            directory.mkdirs()
            cache = DiskLruCache(directory, appVersion, valueCount, maxSize, maxFileCount)
            cache.rebuildJournal()
            return cache
        }


        @Throws(IOException::class)
        private fun deleteIfExists(file: File) {
            if (file.exists() && !file.delete()) {
                throw IOException()
            }
        }

        @Throws(IOException::class)
        private fun renameTo(from: File, to: File, deleteDestination: Boolean) {
            if (deleteDestination) {
                deleteIfExists(to)
            }
            if (!from.renameTo(to)) {
                throw IOException()
            }
        }

        @Throws(IOException::class)
        private fun inputStreamToString(input: InputStream?): String {
            return readFully(InputStreamReader(input, UTF_8))
        }

        private val NULL_OUTPUT_STREAM = object : OutputStream() {
            @Throws(IOException::class)
            override fun write(b: Int) {
                // Eat all writes silently. Nom nom.
            }
        }

    }

    private var directory: File
    private var journalFile: File
    private var journalFileTmp: File
    private var journalFileBackup: File

    private var appVersion: Int = 0
    private var maxSize: Long = 0
    private var maxFileCount: Int = 0
    private var valueCount: Int = 0
    private var size: Long = 0
    private var fileCount: Int = 0
    private var journalWriter: Writer? = null
    private val lruEntries = LinkedHashMap<String, Entry>(0, 0.75f, true)
    private var redundantOpCount: Int = 0

    private var nextSequenceNumber: Long = 0

    private val executorService = ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, LinkedBlockingDeque<Runnable>())

    private val cleanupCallable = object : Callable<Unit?> {
        override fun call(): Unit? {
            synchronized(this) {
                if (journalWriter == null) {
                    return null
                }
                trimToSize()
                trimToFileCount()
                if (journalRebuildRequired()) {
                    rebuildJournal()
                    redundantOpCount = 0
                }
                return null
            }
        }
    }

    private constructor(directory: File, appVersion: Int, valueCount: Int, maxSize: Long, maxFileCount: Int) {
        this.directory = directory
        this.appVersion = appVersion

        this.journalFile = File(directory, JOURNAL_FILE)
        this.journalFileTmp = File(directory, JOURNAL_FILE_TEMP)
        this.journalFileBackup = File(directory, JOURNAL_FILE_BACKUP)

        this.maxSize = maxSize
        this.valueCount = valueCount
        this.maxFileCount = maxFileCount
    }

    @Throws(IOException::class)
    private fun readJournal() {
        val reader = StrictLineReader(FileInputStream(journalFile), US_ASCII)
        try {
            val magic = reader.readLine()
            val version = reader.readLine()
            val appVersionString = reader.readLine()
            val valueCountString = reader.readLine()
            val blank = reader.readLine()
            if (MAGIC != magic
                || VERSION_1 != version
                || appVersion.toString() != appVersionString
                || valueCount.toString() != valueCountString
                || "" != blank
            ) {
                throw IOException("unexpected journal header: [$magic,$version,$valueCountString,$blank]")
            }

            var lineCount = 0
            while (true) {
                try {
                    readJournalLine(reader.readLine())
                    lineCount++
                } catch (endOfJournal: EOFException) {
                    break
                }
                redundantOpCount = lineCount - lruEntries.size
            }
        } finally {
            closeSilently(reader)
        }
    }

    @Throws(IOException::class)
    private fun readJournalLine(line: String) {

    }

    @Throws(IOException::class)
    private fun processJournal() {

    }

    @Throws(IOException::class)
    private fun rebuildJournal() {

    }

    @Throws(IOException::class)
    private fun completeEdit(editor: Editor, success: Boolean) {
        val entry = editor.entry
        if (entry.currentEditor != editor) {
            throw IllegalStateException()
        }

        // If this edit is creating the entry for the first time, every index must have a value.
        if (success && !entry.readable) {
            for (i in 0 until valueCount) {
                if (editor.written?.get(i) != true) {
                    editor.abort()
                    throw java.lang.IllegalStateException("Newly created entry didn't create value for index $i")
                }
                if (!entry.getDirtyFile(i).exists()) {
                    editor.abort()
                    return
                }
            }
        }

        for (i in 0 until valueCount) {
            val dirty = entry.getDirtyFile(i)
            if (success) {
                if (dirty.exists()) {
                    val clean: File = entry.getCleanFile(i)
                    dirty.renameTo(clean)
                    val oldLength = entry.lengths[i]
                    val newLength = clean.length()
                    entry.lengths[i] = newLength
                    size = size - oldLength + newLength
                    fileCount++
                }
            } else {
                deleteIfExists(dirty)
            }
        }

        redundantOpCount++
        entry.currentEditor = null
        if (entry.readable || success) {
            entry.readable = true
            journalWriter?.write("$CLEAN ${entry.key}${entry.getLengths()}\n")
            if (success) {
                entry.sequenceNumber = nextSequenceNumber++
            }
        } else {
            lruEntries.remove(entry.key)
            journalWriter?.write("$REMOVE ${entry.key}\n")
        }
        journalWriter?.flush()

        if (size > maxSize || fileCount > maxFileCount || journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
    }

    private fun journalRebuildRequired(): Boolean {
        val redundantOpCompactThreshold = 2000
        return redundantOpCount >= redundantOpCompactThreshold && redundantOpCount >= lruEntries.size
    }

    @Throws(IOException::class)
    fun remove(key: String): Boolean {
        checkNotClosed()
        validateKey(key)
        val entry = lruEntries[key]
        if (entry == null || entry.currentEditor != null) {
            return false
        }

        for (i in 0 until valueCount) {
            val file = entry.getCleanFile(i)
            if (file.exists() && !file.delete()) {
                throw IOException("failed to delete $file")
            }
            size -= entry.lengths[i]
            fileCount--
            entry.lengths[i] = 0
        }

        redundantOpCount++
        journalWriter?.append("$REMOVE $key\n")
        lruEntries.remove(key)

        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }

        return true
    }

    override fun close() {
        if (journalWriter == null) return
        for (entry in ArrayList<Entry>(lruEntries.values)) {
            if (entry.currentEditor != null) {
                entry.currentEditor?.abort()
            }
        }
        trimToSize()
        trimToFileCount()
        journalWriter?.close()
        journalWriter = null
    }

    fun isClosed(): Boolean = journalWriter == null

    private fun checkNotClosed() {
        if (journalWriter == null) {
            throw IllegalArgumentException("cache is closed")
        }
    }

    @Throws(IOException::class)
    fun flush() {
        checkNotClosed()
        trimToSize()
        trimToFileCount()
        journalWriter?.flush()
    }

    @Throws(IOException::class)
    private fun trimToSize() {
        while (size > maxSize) {
            val toEvict = lruEntries.entries.iterator().next()
            remove(toEvict.key)
        }
    }

    @Throws(IOException::class)
    private fun trimToFileCount() {
        while (fileCount > maxFileCount) {
            val toEvict = lruEntries.entries.iterator().next()
            remove(toEvict.key)
        }
    }

    @Throws(IOException::class)
    fun delete() {
        close()
        deleteIfExists(directory)
    }

    private fun validateKey(key: String) {
        val matcher = LEGAL_KEY_PATTERN.matcher(key)
        if (!matcher.matches()) {
            throw IllegalArgumentException("keys must match regex [a-z0-9_-]{1,64}: \"$key\"")
        }
    }

    @Throws(IOException::class)
    fun get(key: String): Snapshot? {
        checkNotClosed()
        validateKey(key)
        var entry = lruEntries[key]
        if (entry == null) {
            return null
        }

        if (!entry.readable) {
            return null
        }

        // Open all streams eagerly to guarantee that we see a single published
        // snapshot. If we opened streams lazily then the streams could come
        // from different edits.
        val files = arrayOfNulls<File>(valueCount)
        val ins = arrayOfNulls<InputStream>(valueCount)
        try {
            var file: File? = null
            for (i in 0 until fileCount) {
                file = entry.getCleanFile(i)
                files[i] = file
                ins[i] = FileInputStream(file)
            }
        } catch (e: FileNotFoundException) {
            for (i in 0 until valueCount) {
                if (ins[i] != null) {
                    closeSilently(ins[i])
                } else {
                    break
                }
            }
            return null
        }

        redundantOpCount++
        journalWriter?.append("$REMOVE $key\n")
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
        return Snapshot(key, entry.sequenceNumber, files, ins, entry.lengths)
    }

    @Throws(IOException::class)
    fun edit(key: String): Editor? {
        return edit(key, ANY_SEQUENCE_NUMBER)
    }

    @Throws(IOException::class)
    fun edit(key: String, expectedSequenceNumber: Long): Editor? {
        checkNotClosed()
        validateKey(key)
        var entry = lruEntries.get(key)
        if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER && (entry == null || entry.sequenceNumber != expectedSequenceNumber)) {
            return null
        }
        if (entry == null) {
            entry = Entry(key)
            lruEntries[key] = entry
        } else if (entry.currentEditor != null) {
            return null
        }

        val editor: Editor = Editor(entry)
        entry.currentEditor = editor

        // Flush the journal before creating files to prevent file leaks.
        journalWriter?.write("$DIRTY $key\n")
        journalWriter?.flush()
        return editor
    }

    public inner class Snapshot(
        var key: String,
        var sequenceNumber: Long,
        var files: Array<File?>,
        var ins: Array<InputStream?>,
        var lengths: LongArray
    ) :
        Closeable {

        @Throws(IOException::class)
        fun edit(): Editor? {
            return this@DiskLruCache.edit(key, sequenceNumber)
        }

        fun getFile(index: Int): File? = files[index]

        fun getInputStream(index: Int): InputStream? = ins[index]

        @Throws(IOException::class)
        fun getString(index: Int): String {
            return inputStreamToString(getInputStream(index))
        }

        override fun close() {
            for (`in` in ins) {
                closeSilently(`in`)
            }
        }

    }

    inner class Entry(val key: String) {

        val lengths: LongArray = LongArray(valueCount)

        var readable = false

        var currentEditor: Editor? = null

        var sequenceNumber: Long = 0L

        @Throws(IOException::class)
        fun getLengths(): String {
            val result = StringBuilder()
            for (size in lengths) {
                result.append(' ').append(size)
            }
            return result.toString()
        }

        @Throws(IOException::class)
        private fun setLengths(strings: Array<String>) {
            if (strings.size != valueCount) {
                throw invalidLengths(strings)
            }
            try {
                for (i in strings.indices) {
                    lengths[i] = strings[i].toLong()
                }
            } catch (e: NumberFormatException) {
                throw invalidLengths(strings)
            }
        }

        @Throws(IOException::class)
        private fun invalidLengths(strings: Array<String>): IOException {
            throw IOException("unexpected journal line: ${strings.contentToString()}")
        }

        fun getCleanFile(i: Int): File {
            return File(directory, "$key.$i")
        }

        fun getDirtyFile(i: Int): File {
            return File(directory, "$key.$i.tmp")
        }

    }

    inner class Editor(val entry: Entry) {

        var written = if (entry.readable) null else BooleanArray(valueCount)

        private var hasErrors = false

        private var committed = false

        @Throws(IOException::class)
        fun newInputStream(index: Int): InputStream? {
            synchronized(this@DiskLruCache) {
                if (entry.currentEditor != this) {
                    throw IllegalArgumentException()
                }
                if (!entry.readable) {
                    return null
                }
                try {
                    return FileInputStream(entry.getCleanFile(index))
                } catch (e: FileNotFoundException) {
                    return null
                }
            }
        }

        @Throws(IOException::class)
        fun getString(index: Int): String? {
            val input = newInputStream(index)
            return if (input != null) {
                inputStreamToString(input)
            } else {
                null
            }
        }

        @Throws(IOException::class)
        fun newOutputStream(index: Int): OutputStream {
            synchronized(this@DiskLruCache) {
                if (entry.currentEditor != this) {
                    throw IllegalStateException()
                }
                if (!entry.readable) {
                    written?.set(index, true)
                }
                val dirtyFile: File = entry.getDirtyFile(index)
                var outputStream: FileOutputStream? = null
                try {
                    outputStream = FileOutputStream(dirtyFile)
                } catch (e: FileNotFoundException) {
                    directory.mkdirs()
                    try {
                        outputStream = FileOutputStream(dirtyFile)
                    } catch (e2: FileNotFoundException) {
                        return NULL_OUTPUT_STREAM
                    }
                }
                return FaultHidingOutputStream(outputStream!!)
            }
        }

        @Throws(IOException::class)
        fun set(index: Int, value: String) {
            var writer: Writer? = null
            try {
                writer = OutputStreamWriter(newOutputStream(index), UTF_8)
                writer.write(value)
            } finally {
                closeSilently(writer)
            }
        }

        @Throws(IOException::class)
        fun commit() {
            if (hasErrors) {
                completeEdit(this, false)
                remove(entry.key)
            } else {
                completeEdit(this, true)
            }
            committed = true
        }

        @Throws(IOException::class)
        fun abort() {
            completeEdit(this, false)
        }

        fun abortUnlessCommitted() {
            if (!committed) {
                try {
                    abort()
                } catch (ignored: IOException) {
                }
            }
        }

        private inner class FaultHidingOutputStream(out: OutputStream) : FilterOutputStream(out) {

            override fun close() {
                try {
                    out.close()
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            override fun flush() {
                try {
                    out.flush()
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            override fun write(b: Int) {
                try {
                    out.write(b)
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            override fun write(b: ByteArray?, off: Int, len: Int) {
                try {
                    out.write(b, off, len)
                } catch (e: IOException) {
                    hasErrors = true
                }
            }
        }

    }

}