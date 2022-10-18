package cn.quibbler.imageloader.cache.disk.naming

import cn.quibbler.imageloader.utils.L
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Names image file as MD5 hash of image URI
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.4.0
 */
class Md5FileNameGenerator : FileNameGenerator {

    companion object {
        const val HASH_ALGORITHM = "MD5"
        const val RADIX = 10 + 26 // 10 digits + 26 letters
    }

    override fun generate(imageUrl: String): String {
        val md5 = getMD5(imageUrl.toByteArray())
        val bi: BigInteger = BigInteger(md5).abs()
        return bi.toString(RADIX)
    }

    private fun getMD5(data: ByteArray): ByteArray? {
        var hash: ByteArray? = null
        try {
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            digest.update(data)
            hash = digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            L.e(e)
        }
        return hash
    }

}