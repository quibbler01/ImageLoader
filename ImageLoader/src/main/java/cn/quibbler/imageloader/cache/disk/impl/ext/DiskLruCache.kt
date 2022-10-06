package cn.quibbler.imageloader.cache.disk.impl.ext

import cn.quibbler.imageloader.utils.closeSilently
import java.io.*
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * A cache that uses a bounded amount of space on a filesystem. Each cache
 * entry has a string key and a fixed number of values. Each key must match
 * the regex <strong>[a-z0-9_-]{1,64}</strong>. Values are byte sequences,
 * accessible as streams or files. Each value must be between {@code 0} and
 * {@code Integer.MAX_VALUE} bytes in length.
 *
 * <p>The cache stores its data in a directory on the filesystem. This
 * directory must be exclusive to the cache; the cache may delete or overwrite
 * files from its directory. It is an error for multiple processes to use the
 * same cache directory at the same time.
 *
 * <p>This cache limits the number of bytes that it will store on the
 * filesystem. When the number of stored bytes exceeds the limit, the cache will
 * remove entries in the background until the limit is satisfied. The limit is
 * not strict: the cache may temporarily exceed it while waiting for files to be
 * deleted. The limit does not include filesystem overhead or the cache
 * journal so space-sensitive applications should set a conservative limit.
 *
 * <p>Clients call {@link #edit} to create or update the values of an entry. An
 * entry may have only one editor at one time; if a value is not available to be
 * edited then {@link #edit} will return null.
 * <ul>
 * <li>When an entry is being <strong>created</strong> it is necessary to
 * supply a full set of values; the empty value should be used as a
 * placeholder if necessary.
 * <li>When an entry is being <strong>edited</strong>, it is not necessary
 * to supply data for every value; values default to their previous
 * value.
 * </ul>
 * Every {@link #edit} call must be matched by a call to {@link Editor#commit}
 * or {@link Editor#abort}. Committing is atomic: a read observes the full set
 * of values as they were before or after the commit, but never a mix of values.
 *
 * <p>Clients call {@link #get} to read a snapshot of an entry. The read will
 * observe the value at the time that {@link #get} was called. Updates and
 * removals after the call do not impact ongoing reads.
 *
 * <p>This class is tolerant of some I/O errors. If files are missing from the
 * filesystem, the corresponding entries will be dropped from the cache. If
 * an error occurs while writing a cache value, the edit will fail silently.
 * Callers should handle other problems by catching {@code IOException} and
 * responding appropriately.
 */
class DiskLruCache : Closeable {

    /*
     * This cache uses a journal file named "journal". A typical journal file
     * looks like this:
     *     libcore.io.DiskLruCache
     *     1
     *     100
     *     2
     *
     *     CLEAN 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054
     *     DIRTY 335c4c6028171cfddfbaae1a9c313c52
     *     CLEAN 335c4c6028171cfddfbaae1a9c313c52 3934 2342
     *     REMOVE 335c4c6028171cfddfbaae1a9c313c52
     *     DIRTY 1ab96a171faeeee38496d8b330771a7a
     *     CLEAN 1ab96a171faeeee38496d8b330771a7a 1600 234
     *     READ 335c4c6028171cfddfbaae1a9c313c52
     *     READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
     *
     * The first five lines of the journal form its header. They are the
     * constant string "libcore.io.DiskLruCache", the disk cache's version,
     * the application's version, the value count, and a blank line.
     *
     * Each of the subsequent lines in the file is a record of the state of a
     * cache entry. Each line contains space-separated values: a state, a key,
     * and optional state-specific values.
     *   o DIRTY lines track that an entry is actively being created or updated.
     *     Every successful DIRTY action should be followed by a CLEAN or REMOVE
     *     action. DIRTY lines without a matching CLEAN or REMOVE indicate that
     *     temporary files may need to be deleted.
     *   o CLEAN lines track a cache entry that has been successfully published
     *     and may be read. A publish line is followed by the lengths of each of
     *     its values.
     *   o READ lines track accesses for LRU.
     *   o REMOVE lines track entries that have been deleted.
     *
     * The journal file is appended to as cache operations occur. The journal may
     * occasionally be compacted by dropping redundant lines. A temporary file named
     * "journal.tmp" will be used during compaction; that file should be deleted if
     * it exists when the cache is opened.
     */
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

        /**
         * Opens the cache in {@code directory}, creating a cache if none exists
         * there.
         *
         * @param directory a writable directory
         * @param valueCount the number of values per cache entry. Must be positive.
         * @param maxSize the maximum number of bytes this cache should use to store
         * @param maxFileCount the maximum file count this cache should store
         * @throws IOException if reading or writing the cache directory fails
         */
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

    /*
     * the directory where this cache stores its data.
     */
    var directory: File
    private var journalFile: File
    private var journalFileTmp: File
    private var journalFileBackup: File

    private var appVersion: Int = 0

    /**
     * the maximum number of bytes that this cache should use to store its data.
     */
    var maxSize: Long = 0
        set(value) {
            field = value
            executorService.submit(cleanupCallable)
        }

    /**
     * the maximum number of files that this cache should store
     */
    var maxFileCount: Int = 0
    private var valueCount: Int = 0

    /**
     * the number of bytes currently being used to store the values in
     * this cache. This may be greater than the max size if a background
     * deletion is pending.
     */
    private var size: Long = 0

    /**
     * the number of files currently being used to store the values in
     * this cache. This may be greater than the max file count if a background
     * deletion is pending.
     */
    private var fileCount: Int = 0
    private var journalWriter: Writer? = null
    private val lruEntries = LinkedHashMap<String, Entry>(0, 0.75f, true)
    private var redundantOpCount: Int = 0

    /**
     * To differentiate between old and current snapshots, each entry is given
     * a sequence number each time an edit is committed. A snapshot is stale if
     * its sequence number is not equal to its entry's sequence number.
     */
    private var nextSequenceNumber: Long = 0

    /** This cache uses a single background thread to evict entries. */
    private val executorService = ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, LinkedBlockingDeque<Runnable>())

    private val cleanupCallable = object : Callable<Unit?> {
        override fun call(): Unit? {
            synchronized(this) {
                if (journalWriter == null) {
                    return null // Closed.
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
        val firstSpace = line.indexOf(' ')
        if (firstSpace == -1) {
            throw IOException("unexpected journal line: $line")
        }

        val keyBegin = firstSpace + 1
        val secondSpace = line.indexOf(' ', keyBegin)
        val key = if (secondSpace == -1) {
            line.substring(keyBegin).apply {
                if (firstSpace == REMOVE.length && line.startsWith(REMOVE)) {
                    lruEntries.remove(this)
                    return
                }
            }
        } else {
            line.substring(keyBegin, secondSpace)
        }

        val entry: Entry = lruEntries.get(key) ?: Entry(key).apply { lruEntries.put(key, this) }

        if (secondSpace != -1 && firstSpace == CLEAN.length && line.startsWith(CLEAN)) {
            val parts = line.substring(secondSpace + 1).split(" ").toTypedArray()
            entry.readable = true
            entry.currentEditor = null
            entry.setLengths(parts)
        } else if (secondSpace == -1 && firstSpace == DIRTY.length && line.startsWith(DIRTY)) {
            entry.currentEditor = Editor(entry)
        } else if (secondSpace == -1 && firstSpace == READ.length && line.startsWith(READ)) {
            // This work was already done by calling lruEntries.get().
        } else {
            throw IOException("unexpected journal line: $line")
        }
    }

    /**
     * Computes the initial size and collects garbage as a part of opening the
     * cache. Dirty entries are assumed to be inconsistent and will be deleted.
     */
    @Throws(IOException::class)
    private fun processJournal() {
        deleteIfExists(journalFileTmp)
        val i = lruEntries.values.iterator()
        while (i.hasNext()) {
            val entry = i.next()
            if (entry.currentEditor == null) {
                for (t in 0 until valueCount) {
                    size += entry.lengths[t]
                    fileCount++
                }
            } else {
                entry.currentEditor = null
                for (t in 0 until valueCount) {
                    deleteIfExists(entry.getCleanFile(t))
                    deleteIfExists(entry.getDirtyFile(t))
                }
                i.remove()
            }
        }
    }

    /**
     * Creates a new journal that omits redundant information. This replaces the
     * current journal if it exists.
     */
    @Throws(IOException::class)
    private fun rebuildJournal() {
        journalWriter?.close()

        val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(journalFileTmp), US_ASCII))
        try {
            writer.write(MAGIC)
            writer.write("\n")
            writer.write(VERSION_1)
            writer.write("\n")
            writer.write(Integer.toString(appVersion))
            writer.write("\n")
            writer.write(Integer.toString(valueCount))
            writer.write("\n")
            writer.write("\n")

            for (entry in lruEntries.values) {
                if (entry.currentEditor != null) {
                    writer.write("$DIRTY ${entry.key}")
                } else {
                    writer.write("$CLEAN ${entry.key}${entry.getLengths()}")
                }
            }
        } finally {
            writer.close()
        }

        if (journalFile.exists()) {
            renameTo(journalFile, journalFileBackup, true)
        }
        renameTo(journalFileTmp, journalFile, false)
        journalFileBackup.delete()

        journalWriter = BufferedWriter(OutputStreamWriter(FileOutputStream(journalFile, true), US_ASCII))
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

    /**
     * We only rebuild the journal when it will halve the size of the journal
     * and eliminate at least 2000 ops.
     */
    private fun journalRebuildRequired(): Boolean {
        val redundantOpCompactThreshold = 2000
        return redundantOpCount >= redundantOpCompactThreshold && redundantOpCount >= lruEntries.size
    }

    /**
     * Drops the entry for {@code key} if it exists and can be removed. Entries
     * actively being edited cannot be removed.
     *
     * @return true if an entry was removed.
     */
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

    /** Closes this cache. Stored values will remain on the filesystem. */
    override fun close() {
        if (journalWriter == null) return // Already closed.
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

    /** Returns true if this cache has been closed. */
    fun isClosed(): Boolean = journalWriter == null

    private fun checkNotClosed() {
        if (journalWriter == null) {
            throw IllegalArgumentException("cache is closed")
        }
    }

    /** Force buffered operations to the filesystem. */
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

    /**
     * Closes the cache and deletes all of its stored values. This will delete
     * all files in the cache directory including files that weren't created by
     * the cache.
     */
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

    /**
     * Returns a snapshot of the entry named {@code key}, or null if it doesn't
     * exist is not currently readable. If a value is returned, it is moved to
     * the head of the LRU queue.
     */
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
            // A file must have been deleted manually!
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

    /**
     * Returns an editor for the entry named {@code key}, or null if another
     * edit is in progress.
     */
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
            return null // Snapshot is stale.
        }
        if (entry == null) {
            entry = Entry(key)
            lruEntries[key] = entry
        } else if (entry.currentEditor != null) {
            return null // Another edit is in progress.
        }

        val editor: Editor = Editor(entry)
        entry.currentEditor = editor

        // Flush the journal before creating files to prevent file leaks.
        journalWriter?.write("$DIRTY $key\n")
        journalWriter?.flush()
        return editor
    }

    /** A snapshot of the values for an entry. */
    public inner class Snapshot(
        var key: String,
        var sequenceNumber: Long,
        var files: Array<File?>,
        var ins: Array<InputStream?>,
        var lengths: LongArray
    ) : Closeable {

        /**
         * Returns an editor for this snapshot's entry, or null if either the
         * entry has changed since this snapshot was created or if another edit
         * is in progress.
         */
        @Throws(IOException::class)
        fun edit(): Editor? {
            return this@DiskLruCache.edit(key, sequenceNumber)
        }

        /** Returns file with the value for {@code index}. */
        fun getFile(index: Int): File? = files[index]

        /** Returns the unbuffered stream with the value for {@code index}. */
        fun getInputStream(index: Int): InputStream? = ins[index]

        /** Returns the string value for {@code index}. */
        @Throws(IOException::class)
        fun getString(index: Int): String {
            return inputStreamToString(getInputStream(index))
        }

        /** Returns the byte length of the value for {@code index}. */
        fun getLength(index: Int) = lengths[index]

        override fun close() {
            for (`in` in ins) {
                closeSilently(`in`)
            }
        }

    }

    inner class Entry(val key: String) {
        /** Lengths of this entry's files. */
        val lengths: LongArray = LongArray(valueCount)

        /** True if this entry has ever been published. */
        var readable = false

        /** The ongoing edit or null if this entry is not being edited. */
        var currentEditor: Editor? = null

        /** The sequence number of the most recently committed edit to this entry. */
        var sequenceNumber: Long = 0L

        @Throws(IOException::class)
        fun getLengths(): String {
            val result = StringBuilder()
            for (size in lengths) {
                result.append(' ').append(size)
            }
            return result.toString()
        }

        /** Set lengths using decimal numbers like "10123". */
        @Throws(IOException::class)
        fun setLengths(strings: Array<String>) {
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

    /** Edits the values for an entry. */
    inner class Editor(val entry: Entry) {

        var written = if (entry.readable) null else BooleanArray(valueCount)

        private var hasErrors = false

        private var committed = false

        /**
         * Returns an unbuffered input stream to read the last committed value,
         * or null if no value has been committed.
         */
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

        /**
         * Returns the last committed value as a string, or null if no value
         * has been committed.
         */
        @Throws(IOException::class)
        fun getString(index: Int): String? {
            val input = newInputStream(index)
            return if (input != null) {
                inputStreamToString(input)
            } else {
                null
            }
        }

        /**
         * Returns a new unbuffered output stream to write the value at
         * {@code index}. If the underlying output stream encounters errors
         * when writing to the filesystem, this edit will be aborted when
         * {@link #commit} is called. The returned output stream does not throw
         * IOExceptions.
         */
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
                    // Attempt to recreate the cache directory.
                    directory.mkdirs()
                    try {
                        outputStream = FileOutputStream(dirtyFile)
                    } catch (e2: FileNotFoundException) {
                        // We are unable to recover. Silently eat the writes.
                        return NULL_OUTPUT_STREAM
                    }
                }
                return FaultHidingOutputStream(outputStream!!)
            }
        }

        /** Sets the value at {@code index} to {@code value}. */
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

        /**
         * Commits this edit so it is visible to readers.  This releases the
         * edit lock so another edit may be started on the same key.
         */
        @Throws(IOException::class)
        fun commit() {
            if (hasErrors) {
                completeEdit(this, false)
                remove(entry.key) // The previous entry is stale.
            } else {
                completeEdit(this, true)
            }
            committed = true
        }

        /**
         * Aborts this edit. This releases the edit lock so another edit may be
         * started on the same key.
         */
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