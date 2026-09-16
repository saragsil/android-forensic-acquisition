package com.gamezorck.forensicasq.logging

import com.gamezorck.forensicasq.integrity.Provenance
import com.gamezorck.forensicasq.util.TimeUtil
import java.io.File

object ChainOfCustody {

    private const val FILE_NAME = "chain_of_custody.txt"

    private const val HEADER_PREFIX = "#"
    private const val FORMAT_VERSION = "1"

    /**
     * Appends a custody event line to chain_of_custody.txt inside caseDir.
     *
     * Header (written once, if file does not exist):
     *   # chain_of_custody format=1 schema=... signature=...
     *
     * Event format:
     *   [timestamp] EVENT | details
     */
    fun record(caseDir: File, event: String, details: String = "") {
        try {
            val file = File(caseDir, FILE_NAME)

            if (!file.exists()) {
                writeHeader(file)
            }

            val ts = TimeUtil.nowReadable()
            val line = buildString {
                append('[').append(ts).append("] ")
                append(event)
                if (details.isNotBlank()) {
                    append(" | ").append(details)
                }
                append('\n')
            }

            file.appendText(line)
        } catch (_: Exception) {
            // Best-effort logging: custody must not crash the app
        }
    }

    private fun writeHeader(file: File) {
        val header = buildString {
            append(HEADER_PREFIX).append(' ')
            append("chain_of_custody ")
            append("format=").append(FORMAT_VERSION).append(' ')
            append("schema=").append(Provenance.schemaHint()).append(' ')
            append("signature=").append(Provenance.token()).append('\n')
        }
        file.writeText(header)
    }
}
