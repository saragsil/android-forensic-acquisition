package com.gamezorck.forensicasq.logging

import com.gamezorck.forensicasq.integrity.Provenance
import com.gamezorck.forensicasq.util.TimeUtil
import java.io.File

class ForensicLogger(caseDir: File) {

    private val logFile = File(caseDir, FILE_NAME)

    init {
        if (!logFile.exists()) {
            writeHeader()
        }
    }

    fun log(message: String) {
        try {
            val line = buildString {
                append('[').append(TimeUtil.nowReadable()).append("] ")
                append(message)
                append('\n')
            }
            logFile.appendText(line)
        } catch (_: Exception) {
            // best-effort logging
        }
    }

    private fun writeHeader() {
        val header = buildString {
            append(HEADER_PREFIX).append(' ')
            append("forensic_log ")
            append("format=").append(FORMAT_VERSION).append(' ')
            append("schema=").append(Provenance.schemaHint()).append(' ')
            append("signature=").append(Provenance.token())
            append('\n')
        }
        logFile.writeText(header)
    }

    private companion object {
        const val FILE_NAME = "logs.txt"
        const val HEADER_PREFIX = "#"
        const val FORMAT_VERSION = "1"
    }
}
