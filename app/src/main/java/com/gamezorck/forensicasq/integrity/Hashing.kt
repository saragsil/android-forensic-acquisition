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

        val lines = StringBuilder()
        caseDir.listFiles()
            ?.filter { it.isFile && it.name != out.name }
            ?.sortedBy { it.name }
            ?.forEach { f ->
                val hash = sha256(f)
                lines.append("$hash  ${f.name}\n")
            }

        out.writeText(lines.toString())
    }

    /** Writes "<sha256>  <filename>" to the given output file (overwrites). */
    fun writeSingleHash(file: File, outFileName: String) {
        val out = File(file.parentFile, outFileName)
        val hash = sha256(file)
        out.writeText("$hash  ${file.name}\n")
    }

    /** Appends "<sha256>  <filename>" to hashes.sha256 (optional helper). */
    fun appendHashLine(caseDir: File, file: File, outFileName: String = "hashes.sha256") {
        val out = File(caseDir, outFileName)
        val hash = sha256(file)
        out.appendText("$hash  ${file.name}\n")
    }
}
