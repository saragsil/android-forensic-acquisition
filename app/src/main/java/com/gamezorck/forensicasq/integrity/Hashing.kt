package com.gamezorck.forensicasq.integrity

import java.io.File
import java.security.MessageDigest

object Hashing {

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytes = input.read(buffer)
            while (bytes > 0) {
                digest.update(buffer, 0, bytes)
                bytes = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun writeHashes(caseDir: File) {
        val out = File(caseDir, "hashes.sha256")
        caseDir.listFiles()?.forEach { f ->
            if (f.isFile && f.name != out.name) {
                val hash = sha256(f)
                out.appendText("$hash  ${f.name}\n")
            }
        }
    }
}
