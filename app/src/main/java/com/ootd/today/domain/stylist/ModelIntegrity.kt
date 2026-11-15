package com.ootd.today.domain.stylist

import java.io.File
import java.security.MessageDigest

object ModelIntegrity {
    fun verify(file: File, model: LocalModel, checkpoint: () -> Unit = {}): Boolean {
        if (file.length() != model.bytes) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                checkpoint()
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) } == model.sha256
    }
}
