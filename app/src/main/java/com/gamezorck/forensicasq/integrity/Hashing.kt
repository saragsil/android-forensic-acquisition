package com.gamezorck.forensicasq.integrity

import java.io.File
import java.security.MessageDigest

object Hashing {

    private const val ALGO_SHA256 = "SHA-256"
    private const val BUFFER_SIZE = 8 * 1024

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance(ALGO_SHA256)
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHexLower()
    }

    /**
     * Writes hashes for ALL files under caseDir recursively into [outFileName].
     * Uses relative paths for stability in exports.
     * Excludes the output file itself to avoid self-referential hashing.
     */
    fun writeHashes(caseDir: File, outFileName: String = "hashes.sha256") {
        val out = File(caseDir, outFileName)

        val files = caseDir.walkTopDown()
            .filter { it.isFile }
            .filterNot { it.name == out.name }
            .sortedBy { it.relativeTo(caseDir).invariantSeparatorsPath }
            .toList()

        val text = buildString(capacity = files.size * 80) {
            for (f in files) {
                val hash = sha256(f)
                val relPath = f.relativeTo(caseDir).invariantSeparatorsPath
                append(hash).append("  ").append(relPath).append('\n')
            }
        }

        out.writeText(text)
    }

    /** Writes "<sha256>  <filename>" to the given output file (overwrites). */
    fun writeSingleHash(file: File, outFileName: String) {
        val out = File(file.parentFile, outFileName)
        val hash = sha256(file)
        out.writeText("$hash  ${file.name}\n")
    }

    /**
     * Appends "<sha256>  <filename>" to [outFileName].
     * Note: keeps "filename only" behavior for backward compatibility.
     */
    fun appendHashLine(caseDir: File, file: File, outFileName: String = "hashes.sha256") {
        val out = File(caseDir, outFileName)
        val hash = sha256(file)
        out.appendText("$hash  ${file.name}\n")
    }

    private fun ByteArray.toHexLower(): String = joinToString(separator = "") { b ->
        ((b.toInt() and 0xff) + 0x100).toString(16).substring(1)
    }
}
