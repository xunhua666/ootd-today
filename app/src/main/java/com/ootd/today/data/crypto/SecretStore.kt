package com.ootd.today.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 敏感字符串的加密存储。
 *
 * 用途：用户填写的 API Key 不以明文落盘。
 *
 * 为什么自己写而不用 `androidx.security:security-crypto`：该库已被官方废弃，
 * 直接基于 Android Keystore 加密的代码量很小，也更可控。
 *
 * 加密方案：AES/GCM/NoPadding，密钥由 Android Keystore 保管（不出硬件/系统）。
 * GCM 的 IV 每次随机生成，与密文拼接后一起 Base64 存储。
 *
 * **降级策略**：部分设备（定制 ROM、部分模拟器镜像）的 Keystore 不可用。
 * 这种情况下降级为明文存储，并加 `plain:` 前缀以便正确读回。
 * 这不是理想的，但好过让功能直接不可用 —— 明文也只存在于应用私有目录，
 * 在未 root 的设备上其它应用读不到。
 */
@Singleton
class SecretStore @Inject constructor() {

    /** 加密后返回可直接落盘的字符串；失败返回 null。 */
    fun encrypt(plain: String): String? {
        if (plain.isEmpty()) return ""
        return runCatching { encryptWithKeystore(plain) }
            .getOrElse { PLAIN_PREFIX + Base64.encodeToString(plain.toByteArray(), Base64.NO_WRAP) }
    }

    /** 读取加密存储的值；无法解密时返回空串（等同于未配置）。 */
    fun decrypt(stored: String): String {
        if (stored.isEmpty()) return ""

        if (stored.startsWith(PLAIN_PREFIX)) {
            val payload = stored.removePrefix(PLAIN_PREFIX)
            return runCatching {
                String(Base64.decode(payload, Base64.NO_WRAP))
            }.getOrDefault("")
        }

        return runCatching { decryptWithKeystore(stored) }.getOrDefault("")
    }

    // ------------------------------------------------------------------
    // Keystore 实现
    // ------------------------------------------------------------------

    private fun encryptWithKeystore(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())

        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))

        // IV 长度固定为 12 字节（GCM 推荐值），读取时按同样长度切分。
        val combined = ByteArray(1 + iv.size + encrypted.size)
        combined[0] = iv.size.toByte()
        iv.copyInto(combined, destinationOffset = 1)
        encrypted.copyInto(combined, destinationOffset = 1 + iv.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptWithKeystore(stored: String): String {
        val combined = Base64.decode(stored, Base64.NO_WRAP)
        val ivSize = combined[0].toInt()
        require(ivSize in 1..MAX_IV_SIZE) { "IV 长度异常" }

        val iv = combined.copyOfRange(1, 1 + ivSize)
        val payload = combined.copyOfRange(1 + ivSize, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(payload), Charsets.UTF_8)
    }

    /** 取出 Keystore 里的密钥，没有就生成一个。 */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "ootd_stylist_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val TAG_BITS = 128
        const val MAX_IV_SIZE = 32

        /** 降级为明文时的前缀标记。 */
        const val PLAIN_PREFIX = "plain:"
    }
}